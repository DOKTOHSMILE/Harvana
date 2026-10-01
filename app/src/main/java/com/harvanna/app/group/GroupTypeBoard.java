package com.harvanna.app.group;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * Turns a group's real records into the small boxes each type is supposed to show.
 *
 * Rotating: one box per slot. The name sits in the box; a paid slot carries the date it was paid.
 * Fixed: one box per day of the custody month, with that day's date written inside.
 * Accumulative: one box per contribution, with the payment date written inside.
 *
 * Pure Java so the readings can be checked without Android.
 */
public final class GroupTypeBoard {

    public static final int OPEN = 0;
    public static final int PAID = 1;
    public static final int PENDING = 2;
    public static final int MISSED = 3;
    public static final int TODAY = 4;
    public static final int TURN = 5;

    public static final String ROTATING = "ROTATING";
    public static final String FIXED = "FIXED";
    public static final String ACCUMULATIVE = "ACCUMULATIVE";

    private static final String[] MONTHS = {
            "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    };
    private static final long DAY_MS = 24L * 60L * 60L * 1000L;
    private static final double RAW = 100_000_000.0;

    private GroupTypeBoard() {}

    public static final class Cell {
        public final String top;
        public final String bottom;
        public final String title;
        public final String body;
        public final int state;
        /** Fixed custody: this box is today and still unpaid, so a tap should start payment. */
        public final boolean payToday;

        public Cell(String top, String bottom, String title, String body, int state, boolean payToday) {
            this.top = top == null ? "" : top;
            this.bottom = bottom == null ? "" : bottom;
            this.title = title == null ? "" : title;
            this.body = body == null ? "" : body;
            this.state = state;
            this.payToday = payToday;
        }
    }

    public static final class Board {
        public final String kind;
        public final String sectionTitle;
        public final String title;
        public final String subtitle;
        public final String legend;
        public final int columns;
        public final String nextRecipient;
        public final List<Cell> cells;

        Board(String kind, String sectionTitle, String title, String subtitle, String legend,
              int columns, String nextRecipient, List<Cell> cells) {
            this.kind = kind;
            this.sectionTitle = sectionTitle;
            this.title = title;
            this.subtitle = subtitle;
            this.legend = legend;
            this.columns = columns;
            this.nextRecipient = nextRecipient == null ? "" : nextRecipient;
            this.cells = cells == null ? Collections.<Cell>emptyList() : cells;
        }

        public int paidCount() {
            int n = 0;
            for (Cell c : cells) if (c.state == PAID) n++;
            return n;
        }
    }

    public static String kindOf(String groupType) {
        String t = groupType == null ? "" : groupType.toLowerCase(Locale.US);
        if (t.contains("rotat") || t.contains("circle")) return ROTATING;
        if (t.contains("fixed") || t.contains("goal")) return FIXED;
        if (t.contains("accum") || t.contains("saving")) return ACCUMULATIVE;
        return "";
    }

    /**
     * @param personalBook when true, an accumulative book shows only {@code focusUid}'s payments.
     *                     Overview passes false so the group sees every stamp.
     */
    public static Board fromFirestore(Map<String, Object> group,
                                      List<? extends Map<String, Object>> members,
                                      List<? extends Map<String, Object>> payments,
                                      String focusUid,
                                      boolean personalBook,
                                      long nowMs,
                                      TimeZone zone) {
        if (zone == null) zone = TimeZone.getDefault();
        String kind = kindOf(str(group, "groupType", ""));
        if (ROTATING.equals(kind)) return rotating(group, members, payments, nowMs, zone);
        if (FIXED.equals(kind)) return fixed(group, members, payments, focusUid, nowMs, zone);
        if (ACCUMULATIVE.equals(kind)) return accumulative(payments, focusUid, personalBook, zone);
        return new Board("", "PAYMENTS", "Payments", "This group has no type yet.", "", 4, "", new ArrayList<Cell>());
    }

    /** Node families are CIRCLE, ACCUM and FIXED. {@code me} is the signed-in wallet address. */
    public static Board fromNode(String family, Map<String, Object> group, Map<String, Object> member,
                                 Map<String, Object> line, String me, long nowMs, TimeZone zone) {
        if (zone == null) zone = TimeZone.getDefault();
        String f = family == null ? "" : family.toUpperCase(Locale.US);
        if ("CIRCLE".equals(f)) return circle(group, member, me, nowMs, zone);
        if ("ACCUM".equals(f)) return accumLine(line, nowMs, zone);
        if ("FIXED".equals(f)) return fixedGoal(group, member, nowMs, zone);
        return new Board("", "GROUP", "Group", "", "", 4, "", new ArrayList<Cell>());
    }

    // ── rotating (Firestore) ──────────────────────────────────────────────

    private static Board rotating(Map<String, Object> group, List<? extends Map<String, Object>> members,
                                  List<? extends Map<String, Object>> payments, long nowMs, TimeZone zone) {
        int slots = Math.max(1, (int) num(group, "totalSlots", 1));
        if (slots > 24) slots = 24;
        long round = (long) num(group, "currentRound", 1);
        if (round <= 0) round = 1;
        int explicit = (int) num(group, "currentSlot", 0);
        int turn = explicit > 0 ? explicit : (int) (((round - 1) % slots) + 1);

        Map<Integer, String> slotName = new HashMap<>();
        Map<String, Integer> uidSlot = new HashMap<>();
        if (members != null) {
            for (Map<String, Object> m : members) {
                if (m == null) continue;
                int slot = (int) num(m, "slot", 0);
                String uid = str(m, "memberUid", str(m, "uid", ""));
                String name = memberName(m);
                if (slot > 0) {
                    if (!name.isEmpty()) slotName.put(slot, name);
                    if (!uid.isEmpty()) uidSlot.put(uid, slot);
                }
            }
        }

        Map<Integer, Long> paidAt = new HashMap<>();
        if (payments != null) {
            for (Map<String, Object> p : payments) {
                if (p == null || !confirmed(p)) continue;
                long pRound = (long) num(p, "round", 0);
                if (pRound <= 0) pRound = round;
                if (pRound != round) continue;
                int slot = (int) num(p, "slot", 0);
                if (slot <= 0) {
                    String uid = str(p, "memberUid", str(p, "uid", ""));
                    Integer mapped = uidSlot.get(uid);
                    if (mapped != null) slot = mapped;
                }
                if (slot <= 0) continue;
                long ts = timeOf(p);
                Long prev = paidAt.get(slot);
                if (prev == null || ts > prev) paidAt.put(slot, ts);
            }
        }

        String recipient = slotName.containsKey(turn) ? slotName.get(turn) : "Open slot";
        List<Cell> cells = new ArrayList<>();
        for (int s = 1; s <= slots; s++) {
            String name = slotName.containsKey(s) ? slotName.get(s) : "Open";
            boolean paid = paidAt.containsKey(s);
            boolean isTurn = s == turn && !paid;
            int state = paid ? PAID : isTurn ? TURN : OPEN;
            String top = String.valueOf(s);
            String bottom;
            if (paid && paidAt.get(s) != null && paidAt.get(s) > 0) bottom = shortDate(paidAt.get(s), zone);
            else bottom = clip(name, 8);
            String title = "Slot " + s + (name.equals("Open") ? "" : " · " + name);
            String body = title + "\n"
                    + (paid ? "Paid " + fullDate(paidAt.get(s), zone) : isTurn ? "This slot's turn to pay." : "Not paid this round.")
                    + "\nRound " + round;
            cells.add(new Cell(top, bottom, title, body, state, false));
        }
        String subtitle = "Round " + round + " · " + paidAt.size() + " of " + slots + " paid"
                + " · next pot: " + recipient;
        return new Board(ROTATING, "ROTATION BOARD", "Who pays this round", subtitle,
                "Orange is the slot whose turn it is. Green means that slot has paid, and the date is written in the box.",
                4, recipient, cells);
    }

    // ── fixed custody month ───────────────────────────────────────────────

    private static Board fixed(Map<String, Object> group, List<? extends Map<String, Object>> members,
                               List<? extends Map<String, Object>> payments, String focusUid,
                               long nowMs, TimeZone zone) {
        Calendar now = cal(nowMs, zone);
        int today = now.get(Calendar.DAY_OF_MONTH);
        int month = now.get(Calendar.MONTH);
        int year = now.get(Calendar.YEAR);
        int dim = now.getActualMaximum(Calendar.DAY_OF_MONTH);
        String monthKey = String.format(Locale.US, "%04d-%02d", year, month + 1);
        String monthName = MONTHS[month];

        boolean[] paid = new boolean[32];
        if (members != null) {
            for (Map<String, Object> m : members) {
                if (m == null) continue;
                String uid = str(m, "memberUid", str(m, "uid", ""));
                if (focusUid != null && !focusUid.isEmpty() && !focusUid.equals(uid)) continue;
                for (int d : FixedPaymentEngine.daysPaidFromMember(m, monthKey)) {
                    if (d >= 1 && d <= 31) paid[d] = true;
                }
            }
        }
        if (payments != null) {
            for (Map<String, Object> p : payments) {
                if (p == null || !confirmed(p)) continue;
                String uid = str(p, "memberUid", str(p, "uid", ""));
                if (focusUid != null && !focusUid.isEmpty() && !uid.isEmpty() && !focusUid.equals(uid)) continue;
                int day = (int) num(p, "day", 0);
                String mk = str(p, "monthKey", "");
                if (day >= 1 && day <= 31 && (mk.isEmpty() || mk.equals(monthKey))) {
                    String type = str(p, "type", "").toUpperCase(Locale.US);
                    if (type.isEmpty() || type.contains("FIXED") || type.contains("DAILY") || type.contains("CONTRIB")) {
                        paid[day] = true;
                    }
                }
                long ts = timeOf(p);
                if (ts > 0) {
                    Calendar c = cal(ts, zone);
                    if (c.get(Calendar.YEAR) == year && c.get(Calendar.MONTH) == month) {
                        paid[c.get(Calendar.DAY_OF_MONTH)] = true;
                    }
                }
            }
        }

        int paidN = 0;
        List<Cell> cells = new ArrayList<>();
        for (int day = 1; day <= FixedPaymentEngine.DAYS_IN_GRID; day++) {
            boolean isPaid = paid[day];
            if (isPaid) paidN++;
            boolean inMonth = day <= dim;
            int state;
            boolean payToday = false;
            if (!inMonth) state = OPEN;
            else if (isPaid) state = PAID;
            else if (day == today) { state = TODAY; payToday = true; }
            else if (day < today) state = MISSED;
            else state = OPEN;
            String title = day + " " + monthName + " " + year;
            String body = title + "\n"
                    + (isPaid ? "Paid." : !inMonth ? "This month does not have this day."
                    : day == today ? "Today. Tap Pay to record it."
                    : day < today ? "Missed." : "Not due yet.");
            cells.add(new Cell(String.valueOf(day), inMonth ? monthName : "—", title, body, state, payToday));
        }
        String subtitle = paidN + " of " + dim + " days paid · " + monthName + " " + year
                + " · 1 day is the admin fee at month close";
        return new Board(FIXED, "THIS MONTH", "Custody calendar", subtitle,
                "Each box is a day, with the date written inside. Green is paid, red is missed, blue is today.",
                7, "", cells);
    }

    // ── accumulative stamps ───────────────────────────────────────────────

    private static Board accumulative(List<? extends Map<String, Object>> payments, String focusUid,
                                      boolean personalBook, TimeZone zone) {
        List<Map<String, Object>> stamps = new ArrayList<>();
        if (payments != null) {
            for (Map<String, Object> p : payments) {
                if (p == null || !savingsStamp(p)) continue;
                String uid = str(p, "memberUid", str(p, "uid", ""));
                if (personalBook && focusUid != null && !focusUid.isEmpty()
                        && !uid.isEmpty() && !focusUid.equals(uid)) continue;
                stamps.add(p);
            }
        }
        Collections.sort(stamps, (a, b) -> Long.compare(timeOf(a), timeOf(b)));

        double total = 0;
        int confirmedN = 0;
        List<Cell> cells = new ArrayList<>();
        for (Map<String, Object> p : stamps) {
            long ts = timeOf(p);
            boolean ok = confirmed(p);
            if (ok) {
                confirmedN++;
                total += amountOf(p);
            }
            String who = str(p, "memberName", str(p, "name", ""));
            if (who.isEmpty()) who = personalBook ? "You" : "Member";
            String top = ts > 0 ? String.valueOf(cal(ts, zone).get(Calendar.DAY_OF_MONTH)) : "•";
            String bottom = ts > 0 ? MONTHS[cal(ts, zone).get(Calendar.MONTH)] : (ok ? "Paid" : "Wait");
            String title = ts > 0 ? fullDate(ts, zone) : "Payment";
            String body = title + "\n" + who + "\n" + money(amountOf(p))
                    + "\n" + str(p, "status", ok ? "PAID" : "PENDING");
            cells.add(new Cell(top, bottom, title, body, ok ? PAID : PENDING, false));
        }
        String subtitle = cells.isEmpty()
                ? "No savings recorded yet. Each payment you make appears here as its own box, with the date written inside."
                : confirmedN + " recorded · " + money(total) + " saved";
        return new Board(ACCUMULATIVE, "SAVINGS STAMPS", "Payment book", subtitle,
                "Every contribution is a small box. The payment date is written inside. Green is confirmed, amber is still pending.",
                4, "", cells);
    }

    // ── node circle ───────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private static Board circle(Map<String, Object> group, Map<String, Object> member, String me,
                                long nowMs, TimeZone zone) {
        List<String> order = new ArrayList<>();
        Object raw = group == null ? null : group.get("payoutOrder");
        if (raw instanceof List) {
            for (Object o : (List<Object>) raw) if (o != null) order.add(String.valueOf(o));
        }
        int limit = (int) num(group, "memberLimit", order.size());
        if (limit <= 0) limit = Math.max(order.size(), 1);
        if (limit > 12) limit = 12;
        int round = (int) num(group, "currentRound", 1);
        if (round <= 0) round = 1;
        String state = str(group, "state", "");
        boolean forming = "FORMING".equals(state);
        boolean completed = "COMPLETED".equals(state);
        long cycleH = (long) num(group, "cycleDurationHours", 0);
        long start = firstTime(group, "activatedTimestamp", "startTimestamp", "createdTimestamp", "createdAt");

        String meNorm = me == null ? "" : me.trim();
        String recipient = "";
        List<Cell> cells = new ArrayList<>();
        for (int i = 1; i <= limit; i++) {
            String addr = i <= order.size() ? order.get(i - 1) : "";
            boolean mine = !meNorm.isEmpty() && meNorm.equalsIgnoreCase(addr);
            String label = addr.isEmpty() ? "Open" : mine ? "You" : tail(addr);
            int cellState;
            if (completed || (!forming && i < round)) cellState = PAID;
            else if (!forming && i == round) cellState = TURN;
            else cellState = OPEN;
            if (cellState == TURN) recipient = mine ? "You" : (addr.isEmpty() ? "Open slot" : label);
            long due = (start > 0 && cycleH > 0) ? start + (i - 1L) * cycleH * 60L * 60L * 1000L : 0;
            String bottom = due > 0 && cellState == PAID ? shortDate(due, zone) : label;
            String title = "Round " + i + (label.equals("Open") ? "" : " · " + label);
            String body = title
                    + (addr.isEmpty() ? "" : "\n" + addr)
                    + (due > 0 ? "\nDue " + fullDate(due, zone) : "")
                    + "\n" + (cellState == PAID ? "This round has been paid." : cellState == TURN ? "This is the current round." : "Not yet.");
            cells.add(new Cell(String.valueOf(i), bottom, title, body, cellState, false));
        }
        if (recipient.isEmpty() && member != null) {
            long payoutRound = (long) num(member, "payoutRound", 0);
            if (payoutRound > 0) recipient = "You, round " + payoutRound;
        }
        String subtitle = (forming ? "Still forming · " : "Round " + round + " of " + limit + " · ")
                + (recipient.isEmpty() ? "payout order fills as members join" : "this round: " + recipient);
        return new Board(ROTATING, "ROTATION BOARD", "Payout order", subtitle,
                "Orange is the current round. Green rounds are already paid. Tap a box to see who and when.",
                4, recipient, cells);
    }

    private static Board accumLine(Map<String, Object> line, long nowMs, TimeZone zone) {
        List<Cell> cells = new ArrayList<>();
        long start = line == null ? 0 : (long) num(line, "startTimestamp", 0);
        int daysPaid = line == null ? 0 : (int) num(line, "daysPaid", 0);
        if (daysPaid < 0) daysPaid = 0;
        if (daysPaid > 30) daysPaid = 30;
        if (start <= 0) {
            return new Board(ACCUMULATIVE, "SAVINGS STAMPS", "30-day savings line",
                    "Start a savings line. Each day you save becomes a box with that date written inside.",
                    "Green boxes are days already saved.",
                    6, "", cells);
        }
        Calendar today = cal(nowMs, zone);
        int saved = 0;
        for (int i = 0; i < 30; i++) {
            long ts = start + i * DAY_MS;
            Calendar c = cal(ts, zone);
            boolean paid = i < daysPaid;
            boolean sameDay = c.get(Calendar.YEAR) == today.get(Calendar.YEAR)
                    && c.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR);
            int state;
            if (paid) { state = PAID; saved++; }
            else if (sameDay) state = TODAY;
            else if (ts < startOfDay(nowMs, zone)) state = MISSED;
            else state = OPEN;
            String title = fullDate(ts, zone);
            String body = title + "\n" + (paid ? "Saved." : state == TODAY ? "Due today." : state == MISSED ? "Not saved." : "Coming up.");
            cells.add(new Cell(String.valueOf(c.get(Calendar.DAY_OF_MONTH)), MONTHS[c.get(Calendar.MONTH)], title, body, state, false));
        }
        double savedHarv = line == null ? 0 : num(line, "amountPaidRaw", 0) / RAW;
        return new Board(ACCUMULATIVE, "SAVINGS STAMPS", "30-day savings line",
                saved + " of 30 days saved · " + money(savedHarv),
                "Each box is one day of the line. The date is written inside. Green means that day was paid.",
                6, "", cells);
    }

    private static Board fixedGoal(Map<String, Object> group, Map<String, Object> member, long nowMs, TimeZone zone) {
        double target = num(group, "targetGoalAmountRaw", 0) / RAW;
        double raised = num(group, "raisedAmountRaw", 0) / RAW;
        double mine = member == null ? 0 : num(member, "contributedRaw", 0) / RAW;
        List<Cell> cells = new ArrayList<>();
        if (mine > 0) {
            long ts = member == null ? 0 : (long) num(member, "joinedTimestamp", 0);
            if (ts <= 0) ts = nowMs;
            cells.add(new Cell(String.valueOf(cal(ts, zone).get(Calendar.DAY_OF_MONTH)),
                    MONTHS[cal(ts, zone).get(Calendar.MONTH)],
                    "Your deposit",
                    "You have put in " + money(mine) + ".\nRecorded " + fullDate(ts, zone) + ".",
                    PAID, false));
        }
        String rule = str(group, "disbursementRule", "");
        return new Board(FIXED, "GOAL", "Shared goal",
                money(raised) + " raised of " + money(target)
                        + (rule.isEmpty() ? "" : " · " + rule.replace('_', ' ').toLowerCase(Locale.US)),
                "Deposits show as a box with the date. Money leaves only by the rule the group set.",
                4, "", cells);
    }

    // ── helpers ───────────────────────────────────────────────────────────

    private static boolean savingsStamp(Map<String, Object> p) {
        String st = str(p, "status", "");
        if (!confirmed(p) && !"PENDING".equalsIgnoreCase(st)) return false;
        String type = str(p, "type", "").toUpperCase(Locale.US);
        if (type.contains("LOAN") || type.contains("PAYOUT") || type.contains("FEE")
                || type.contains("DISBURSE") || type.contains("WITHDRAW") || type.contains("BOND")) return false;
        return true;
    }

    private static boolean confirmed(Map<String, Object> p) {
        String st = str(p, "status", "");
        return "CONFIRMED".equalsIgnoreCase(st) || "PAID".equalsIgnoreCase(st) || "SUCCESS".equalsIgnoreCase(st);
    }

    static long timeOf(Map<String, Object> p) {
        if (p == null) return 0;
        for (String k : new String[]{"timestamp", "createdAt", "paidAt", "confirmedAt", "date"}) {
            long t = asMillis(p.get(k));
            if (t > 0) return t;
        }
        return 0;
    }

    private static long asMillis(Object v) {
        if (v == null) return 0;
        if (v instanceof Number) {
            long n = ((Number) v).longValue();
            if (n > 0 && n < 10_000_000_000L) return n * 1000L;
            return n;
        }
        if (v instanceof java.util.Date) return ((java.util.Date) v).getTime();
        String cn = v.getClass().getName();
        if (cn.endsWith("Timestamp")) {
            try {
                Object sec = v.getClass().getMethod("getSeconds").invoke(v);
                if (sec instanceof Number) return ((Number) sec).longValue() * 1000L;
            } catch (Exception ignored) {}
        }
        try {
            long n = Long.parseLong(String.valueOf(v).trim());
            if (n > 0 && n < 10_000_000_000L) return n * 1000L;
            return n;
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static long firstTime(Map<String, Object> m, String... keys) {
        if (m == null) return 0;
        for (String k : keys) {
            long t = asMillis(m.get(k));
            if (t > 0) return t;
        }
        return 0;
    }

    private static double amountOf(Map<String, Object> p) {
        double a = num(p, "amountHarv", 0);
        if (a <= 0) a = num(p, "amount", 0);
        if (a <= 0) a = num(p, "amountRaw", 0) / RAW;
        return a;
    }

    private static String memberName(Map<String, Object> m) {
        String n = str(m, "displayName", str(m, "memberName", str(m, "name", str(m, "fullName", ""))));
        if (!n.isEmpty()) return n;
        return "";
    }

    private static String money(double v) {
        if (Math.abs(v - Math.rint(v)) < 0.001) return String.format(Locale.US, "%.0f HARV", v);
        return String.format(Locale.US, "%.2f HARV", v);
    }

    private static String shortDate(long ms, TimeZone zone) {
        Calendar c = cal(ms, zone);
        return c.get(Calendar.DAY_OF_MONTH) + " " + MONTHS[c.get(Calendar.MONTH)];
    }

    private static String fullDate(long ms, TimeZone zone) {
        Calendar c = cal(ms, zone);
        return c.get(Calendar.DAY_OF_MONTH) + " " + MONTHS[c.get(Calendar.MONTH)] + " " + c.get(Calendar.YEAR);
    }

    private static long startOfDay(long ms, TimeZone zone) {
        Calendar c = cal(ms, zone);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private static Calendar cal(long ms, TimeZone zone) {
        Calendar c = Calendar.getInstance(zone);
        c.setTimeInMillis(ms);
        return c;
    }

    private static String clip(String s, int n) {
        if (s == null) return "";
        String t = s.trim();
        int sp = t.indexOf(' ');
        if (sp > 0) t = t.substring(0, sp);
        return t.length() <= n ? t : t.substring(0, n);
    }

    private static String tail(String addr) {
        if (addr == null) return "";
        String a = addr.trim();
        return a.length() <= 4 ? a : a.substring(a.length() - 4);
    }

    private static String str(Map<String, Object> m, String k, String d) {
        if (m == null) return d;
        Object v = m.get(k);
        if (v == null) return d;
        String s = String.valueOf(v).trim();
        return s.isEmpty() || "null".equalsIgnoreCase(s) ? d : s;
    }

    private static double num(Map<String, Object> m, String k, double d) {
        if (m == null) return d;
        Object v = m.get(k);
        if (v instanceof Number) return ((Number) v).doubleValue();
        if (v == null) return d;
        try { return Double.parseDouble(String.valueOf(v).trim()); } catch (Exception e) { return d; }
    }
}
