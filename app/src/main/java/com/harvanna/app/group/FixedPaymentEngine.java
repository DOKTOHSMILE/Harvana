package com.harvanna.app.group;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Fixed Contribution (daily custody) model — pure logic + ledger builders.
 *
 * Rules (contract):
 * - 31 day-slots in a month calendar grid
 * - Member pays uniform dailyAmount for each day
 * - Checked boxes = days paid this month (stored on member doc)
 * - Admin keeps 1 day as compensation at month close
 * - Member is due 30 × dailyAmount when all 31 days are complete
 *
 * State on member doc:
 *   fixedDaysPaid : List of day numbers 1..31
 *   fixedMonthKey : "yyyy-MM"
 *   fixedPaidCount: int
 *
 * Every money movement must also appear in GROUPS/{groupId}/payments
 * with the correct type and status.
 */
public final class FixedPaymentEngine {

    public static final int DAYS_IN_GRID = 31;
    public static final int ADMIN_FEE_DAYS = 1;
    public static final int MEMBER_RETURN_DAYS = DAYS_IN_GRID - ADMIN_FEE_DAYS; // 30

    private FixedPaymentEngine() {}

    // ── Calendar helpers ─────────────────────────────────────────────────

    public static String currentMonthKey() {
        Calendar c = Calendar.getInstance();
        return String.format(Locale.US, "%04d-%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1);
    }

    public static int todayDayOfMonth() {
        return Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
    }

    // ── Member day state ─────────────────────────────────────────────────

    /** Days already paid this month from member map. Resets if monthKey differs. */
    @SuppressWarnings("unchecked")
    public static List<Integer> daysPaidFromMember(Map<String, Object> member, String monthKey) {
        List<Integer> out = new ArrayList<>();
        if (member == null) return out;
        String mk = member.get("fixedMonthKey") != null ? String.valueOf(member.get("fixedMonthKey")) : "";
        if (!monthKey.equals(mk)) return out; // new month → empty
        Object raw = member.get("fixedDaysPaid");
        if (raw instanceof List) {
            for (Object o : (List<?>) raw) {
                try {
                    int d = o instanceof Number ? ((Number) o).intValue() : Integer.parseInt(String.valueOf(o));
                    if (d >= 1 && d <= DAYS_IN_GRID && !out.contains(d)) out.add(d);
                } catch (Exception ignored) {}
            }
        }
        return out;
    }

    public static boolean isDayPaid(List<Integer> daysPaid, int day) {
        return daysPaid != null && daysPaid.contains(day);
    }

    public static boolean canPayToday(List<Integer> daysPaid) {
        int today = todayDayOfMonth();
        if (today > DAYS_IN_GRID) return false;
        return !isDayPaid(daysPaid, today);
    }

    public static int paidCount(List<Integer> daysPaid) {
        return daysPaid == null ? 0 : daysPaid.size();
    }

    public static boolean monthComplete(List<Integer> daysPaid) {
        return paidCount(daysPaid) >= DAYS_IN_GRID;
    }

    /** Amount admin keeps (1 day). */
    public static double adminCompensation(double dailyAmount) {
        return dailyAmount * ADMIN_FEE_DAYS;
    }

    /** Amount returned to member after full month (30 days). */
    public static double memberPayoutAmount(double dailyAmount) {
        return dailyAmount * MEMBER_RETURN_DAYS;
    }

    // ── Member doc update builders ───────────────────────────────────────

    public static Map<String, Object> memberDayUpdate(List<Integer> existing, int newDay, String monthKey) {
        List<Integer> next = new ArrayList<>();
        if (existing != null) next.addAll(existing);
        if (!next.contains(newDay)) next.add(newDay);
        Map<String, Object> fields = new HashMap<>();
        fields.put("fixedDaysPaid", next);
        fields.put("fixedMonthKey", monthKey);
        fields.put("fixedPaidCount", next.size());
        fields.put("updatedAt", System.currentTimeMillis());
        return fields;
    }

    // ── Payments ledger builders (contract-compliant) ────────────────────

    /**
     * Create a PENDING fixed-daily contribution record.
     * Call this right after the member broadcasts the on-chain payment.
     * Later call markConfirmed() (or write a new CONFIRMED record) once
     * confirmation is observed.
     */
    public static Map<String, Object> pendingDailyPayment(String memberUid, String memberName,
                                                          double dailyAmount, int day, String monthKey,
                                                          String txHash, String fromWallet, String groupWallet) {
        Map<String, Object> m = new HashMap<>();
        m.put("type", "FIXED_DAILY");
        m.put("status", "PENDING");
        m.put("memberUid", memberUid != null ? memberUid : "");
        m.put("memberName", memberName != null ? memberName : "Member");
        m.put("amountAjc", dailyAmount);
        m.put("day", day);
        m.put("monthKey", monthKey != null ? monthKey : currentMonthKey());
        m.put("txRef", txHash != null ? txHash : "");
        m.put("from", fromWallet != null ? fromWallet : "");
        m.put("to", groupWallet != null ? groupWallet : "");
        m.put("timestamp", System.currentTimeMillis());
        return m;
    }

    /**
     * Create a CONFIRMED fixed-daily contribution record.
     * Prefer this when the on-chain confirmation is already known at write time.
     */
    public static Map<String, Object> confirmedDailyPayment(String memberUid, String memberName,
                                                            double dailyAmount, int day, String monthKey,
                                                            String txHash, String fromWallet, String groupWallet) {
        Map<String, Object> m = pendingDailyPayment(memberUid, memberName, dailyAmount, day,
                monthKey, txHash, fromWallet, groupWallet);
        m.put("status", "CONFIRMED");
        m.put("confirmedAt", System.currentTimeMillis());
        return m;
    }

    /**
     * Fields to PATCH an existing payment document from PENDING → CONFIRMED.
     * Use with GroupFirestoreManager.updatePaymentStatus(...).
     */
    public static Map<String, Object> confirmPaymentFields(String txRef) {
        Map<String, Object> m = new HashMap<>();
        m.put("status", "CONFIRMED");
        m.put("confirmedAt", System.currentTimeMillis());
        if (txRef != null && !txRef.isEmpty()) {
            m.put("txRef", txRef);
        }
        return m;
    }

    /**
     * Month-close ledger entry: records the return of 30 days to the member.
     * Written when admin executes the actual transfer.
     */
    public static Map<String, Object> monthCloseMemberPayout(String memberUid, double amountAjc,
                                                             String fromGroupWallet, String toMemberWallet,
                                                             String txRef, String monthKey) {
        Map<String, Object> m = new HashMap<>();
        m.put("type", "FIXED_MONTH_CLOSE");
        m.put("status", "CONFIRMED");
        m.put("memberUid", memberUid != null ? memberUid : "");
        m.put("amountAjc", amountAjc);
        m.put("from", fromGroupWallet != null ? fromGroupWallet : "");
        m.put("to", toMemberWallet != null ? toMemberWallet : "");
        m.put("txRef", txRef != null ? txRef : "");
        m.put("monthKey", monthKey != null ? monthKey : currentMonthKey());
        m.put("timestamp", System.currentTimeMillis());
        m.put("confirmedAt", System.currentTimeMillis());
        return m;
    }

    /**
     * Month-close ledger entry: records the 1-day admin fee retained in the group wallet.
     * No on-chain transfer is required (funds already sit in the group wallet).
     */
    public static Map<String, Object> monthCloseAdminFee(String memberUid, double feeAjc,
                                                         String groupWallet, String monthKey) {
        Map<String, Object> m = new HashMap<>();
        m.put("type", "ADMIN_FEE");
        m.put("status", "CONFIRMED");
        m.put("memberUid", memberUid != null ? memberUid : "");
        m.put("amountAjc", feeAjc);
        m.put("from", groupWallet != null ? groupWallet : "");
        m.put("to", groupWallet != null ? groupWallet : ""); // retained
        m.put("monthKey", monthKey != null ? monthKey : currentMonthKey());
        m.put("timestamp", System.currentTimeMillis());
        m.put("confirmedAt", System.currentTimeMillis());
        return m;
    }

    // ── Legacy alias kept for existing call sites ────────────────────────

    /** @deprecated Prefer pendingDailyPayment or confirmedDailyPayment */
    public static Map<String, Object> paymentRecord(String memberUid, String memberName,
                                                    double dailyAmount, int day, String monthKey,
                                                    String txHash, String fromWallet, String groupWallet) {
        return pendingDailyPayment(memberUid, memberName, dailyAmount, day, monthKey,
                txHash, fromWallet, groupWallet);
    }
}
