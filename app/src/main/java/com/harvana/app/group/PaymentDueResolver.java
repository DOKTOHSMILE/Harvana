package com.harvana.app.group;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * Decides what the Home "Upcoming payments" row should offer for ONE group, from data already in Firestore.
 * Pure Java (no Android) so it is unit tested.
 *
 * A member has met the current obligation when there is a CONTRIBUTION payment of theirs with status
 * PENDING (submitted, awaiting confirmation), CONFIRMED or PAID that belongs to the current cycle:
 *   - rotating groups with a numeric currentSlot: the payment's "slot" equals currentSlot;
 *   - otherwise: the payment's timestamp is inside the current calendar period for the group's frequency.
 * Accumulative savings groups have no scheduled obligation.
 *
 * Outcome for the signed-in member:
 *   PAY     - I owe the current cycle and it is not paid or in flight.
 *   REMIND  - I am settled, but another ACTIVE member still owes it (first one is returned).
 *   NONE    - nothing to do (everyone paid, my payment is confirming, group not active, or savings group).
 */
public final class PaymentDueResolver {

    public enum Action { PAY, REMIND, NONE }

    public static final class Result {
        public final Action action;
        public final String otherMemberUid;   // set for REMIND
        public final String statusLine;       // short human sentence for the card
        public final String badge;            // "Due", "Waiting", "Paid", "Confirming", "Idle"
        public final int unpaidCount;

        Result(Action action, String otherMemberUid, String statusLine, String badge, int unpaidCount) {
            this.action = action;
            this.otherMemberUid = otherMemberUid;
            this.statusLine = statusLine;
            this.badge = badge;
            this.unpaidCount = unpaidCount;
        }
    }

    private PaymentDueResolver() {}

    public static Result resolve(Map<String, Object> group, List<Map<String, Object>> members,
                                 List<Map<String, Object>> payments, String myUid, long nowMs, TimeZone tz) {
        if (group == null || myUid == null || myUid.isEmpty()) return none("Nothing due", "Idle");

        String type = str(group.get("groupType"));
        String status = str(group.get("status"));
        if (!status.isEmpty() && !"ACTIVE".equalsIgnoreCase(status) && !"OPEN".equalsIgnoreCase(status)) {
            return none("Group is " + status.toLowerCase(Locale.US), "Idle");
        }
        if (type.contains("Accumulative")) return none("Save any time - nothing scheduled", "Idle");
        String goalState = str(group.get("state"));
        if (type.contains("Fixed") && ("COMPLETED".equalsIgnoreCase(goalState) || "CLOSED".equalsIgnoreCase(goalState))) {
            return none("Goal reached - deposits are closed", "Closed");
        }

        boolean bySlot = type.contains("Rotating") && group.get("currentSlot") instanceof Number
                && ((Number) group.get("currentSlot")).intValue() > 0;
        int slot = bySlot ? ((Number) group.get("currentSlot")).intValue() : 0;
        long periodStart = periodStartMs(str(group.get("frequency")), nowMs, tz);

        // who is in the group and active
        List<String> active = new ArrayList<>();
        if (members != null) {
            for (Map<String, Object> m : members) {
                if (m == null) continue;
                String uid = firstNonEmpty(str(m.get("memberUid")), str(m.get("uid")));
                String st = str(m.get("status"));
                if (uid.isEmpty()) continue;
                if (st.isEmpty() || "ACTIVE".equalsIgnoreCase(st)) active.add(uid);
            }
        }
        if (!active.contains(myUid)) return none("You are not an active member", "Idle");

        String myState = memberState(payments, myUid, bySlot, slot, periodStart);   // NONE, PENDING, PAID
        if ("NONE".equals(myState)) {
            int unpaid = 1;
            for (String uid : active) if (!uid.equals(myUid) && "NONE".equals(memberState(payments, uid, bySlot, slot, periodStart))) unpaid++;
            return new Result(Action.PAY, null, "Your contribution is due", "Due", unpaid);
        }

        // I am settled (paid or confirming). Is someone else still owing?
        String firstOther = null;
        int others = 0;
        for (String uid : active) {
            if (uid.equals(myUid)) continue;
            if ("NONE".equals(memberState(payments, uid, bySlot, slot, periodStart))) {
                others++;
                if (firstOther == null) firstOther = uid;
            }
        }
        if (firstOther != null) {
            return new Result(Action.REMIND, firstOther, others == 1 ? "Waiting on 1 member" : "Waiting on " + others + " members", "Waiting", others);
        }
        if ("PENDING".equals(myState)) return none("Your payment is confirming", "Confirming");
        return none("Everyone has paid this cycle", "Paid");
    }

    private static Result none(String line, String badge) {
        return new Result(Action.NONE, null, line, badge, 0);
    }

    /** NONE = nothing recorded this cycle; PENDING = submitted, not yet confirmed; PAID = confirmed/paid. */
    static String memberState(List<Map<String, Object>> payments, String uid, boolean bySlot, int slot, long periodStart) {
        String best = "NONE";
        if (payments == null) return best;
        for (Map<String, Object> p : payments) {
            if (p == null) continue;
            if (!uid.equals(str(p.get("memberUid")))) continue;
            String type = str(p.get("type"));
            if (!type.isEmpty() && !"CONTRIBUTION".equalsIgnoreCase(type)) continue;
            boolean inCycle;
            if (bySlot && p.get("slot") instanceof Number) {
                inCycle = ((Number) p.get("slot")).intValue() == slot;
            } else {
                long ts = p.get("timestamp") instanceof Number ? ((Number) p.get("timestamp")).longValue() : 0L;
                inCycle = ts >= periodStart;
            }
            if (!inCycle) continue;
            String st = str(p.get("status")).toUpperCase(Locale.US);
            if ("CONFIRMED".equals(st) || "PAID".equals(st)) return "PAID";
            if ("PENDING".equals(st)) best = "PENDING";
        }
        return best;
    }

    /** Start (ms) of the current calendar period for a frequency label such as Daily/Weekly/Monthly/Annually. */
    public static long periodStartMs(String frequency, long nowMs, TimeZone tz) {
        Calendar c = Calendar.getInstance(tz != null ? tz : TimeZone.getDefault(), Locale.US);
        c.setTimeInMillis(nowMs);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        String f = frequency == null ? "" : frequency.trim().toUpperCase(Locale.US);
        switch (f) {
            case "DAILY":
                break;
            case "MONTHLY":
                c.set(Calendar.DAY_OF_MONTH, 1);
                break;
            case "QUARTERLY":
                c.set(Calendar.DAY_OF_MONTH, 1);
                c.set(Calendar.MONTH, (c.get(Calendar.MONTH) / 3) * 3);
                break;
            case "ANNUALLY":
            case "YEARLY":
                c.set(Calendar.DAY_OF_YEAR, 1);
                break;
            case "WEEKLY":
            default: {
                // week starts Monday
                int dow = c.get(Calendar.DAY_OF_WEEK);
                int back = (dow + 5) % 7;
                c.add(Calendar.DAY_OF_YEAR, -back);
            }
        }
        return c.getTimeInMillis();
    }

    /**
     * The next time a contribution may be made after one recorded at {@code paidAtMs}.
     * Daily opens at the next local midnight, not 24 hours after the payment.
     */
    public static long nextOpenMs(String frequency, long paidAtMs, TimeZone tz) {
        long start = periodStartMs(frequency, paidAtMs, tz);
        Calendar c = Calendar.getInstance(tz != null ? tz : TimeZone.getDefault(), Locale.US);
        c.setTimeInMillis(start);
        String f = frequency == null ? "" : frequency.trim().toUpperCase(Locale.US);
        if (f.contains("DAY") && !f.contains("WEEK") && !f.contains("MONTH") && !f.contains("YEAR")) {
            c.add(Calendar.DAY_OF_MONTH, 1);
        } else if (f.contains("QUARTER")) {
            c.add(Calendar.MONTH, 3);
        } else if (f.contains("MONTH")) {
            c.add(Calendar.MONTH, 1);
        } else if (f.contains("YEAR") || f.contains("ANNUAL")) {
            c.add(Calendar.YEAR, 1);
        } else if (f.contains("FORTNIGHT") || f.contains("BI")) {
            c.add(Calendar.DAY_OF_YEAR, 14);
        } else {
            c.add(Calendar.DAY_OF_YEAR, 7);
        }
        return c.getTimeInMillis();
    }

    private static String str(Object o) { return o == null ? "" : String.valueOf(o).trim(); }

    private static String firstNonEmpty(String a, String b) { return !a.isEmpty() ? a : b; }
}
