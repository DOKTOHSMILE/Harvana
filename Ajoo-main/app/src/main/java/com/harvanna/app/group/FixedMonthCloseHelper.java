package com.harvanna.app.group;

import android.content.Context;
import android.util.Log;

import com.harvanna.app.firestore.GroupFirestoreManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin action at month end for Fixed groups.
 *
 * Contract:
 * - Member due: 30 × dailyAmount (returned via on-chain transfer from group wallet)
 * - Admin fee: 1 × dailyAmount (already sitting in group wallet — just recorded)
 *
 * After the member transfer succeeds, two CONFIRMED payments ledger entries
 * are written (member payout + admin fee) and a system notice is posted.
 */
public final class FixedMonthCloseHelper {

    private static final String TAG = "FixedMonthCloseHelper";

    public interface Callback {
        void onReady(double memberPayoutHarv, double adminFeeHarv, String message);
        void onError(String error);
    }

    public interface CloseCallback {
        void onSuccess(String message);
        void onError(String error);
    }

    private FixedMonthCloseHelper() {}

    /**
     * Validates that the member has paid all 31 days and returns the two amounts.
     * Does not move money — call executeClose after the admin confirms the transfer.
     */
    public static void evaluate(Context ctx, String groupId, String memberUid,
                                double dailyAmount, List<Integer> daysPaid, Callback cb) {
        if (cb == null) return;
        if (dailyAmount <= 0) {
            cb.onError("Invalid daily amount.");
            return;
        }
        if (!FixedPaymentEngine.monthComplete(daysPaid)) {
            cb.onError("Member has only " + FixedPaymentEngine.paidCount(daysPaid)
                    + "/31 days. Need full month.");
            return;
        }
        double member = FixedPaymentEngine.memberPayoutAmount(dailyAmount);
        double fee = FixedPaymentEngine.adminCompensation(dailyAmount);
        cb.onReady(member, fee,
                "Return " + com.harvanna.app.wallet.BalanceFormatter.smart(member)
                        + " to member; retain "
                        + com.harvanna.app.wallet.BalanceFormatter.smart(fee)
                        + " as admin compensation.");
    }

    /**
     * After the admin has successfully transferred the member's 30-day return,
     * record both ledger entries and post a system notice.
     */
    public static void recordClose(Context context,
                                   String groupId,
                                   String memberUid,
                                   double memberPayoutHarv,
                                   double adminFeeHarv,
                                   String groupWallet,
                                   String memberWallet,
                                   String txRef,
                                   CloseCallback callback) {
        if (callback == null) return;
        if (GroupValidation.isBlank(groupId) || GroupValidation.isBlank(memberUid)) {
            callback.onError("Missing group or member id.");
            return;
        }

        String monthKey = FixedPaymentEngine.currentMonthKey();

        // 1. Member payout ledger entry
        Map<String, Object> memberPayment = FixedPaymentEngine.monthCloseMemberPayout(
                memberUid, memberPayoutHarv, groupWallet, memberWallet, txRef, monthKey);

        GroupFirestoreManager.getInstance(context)
                .addPayment(groupId, memberPayment, new GroupFirestoreManager.IdCallback() {
                    @Override
                    public void onSuccess(String id) {
                        // 2. Admin fee ledger entry (no extra transfer needed)
                        Map<String, Object> feePayment = FixedPaymentEngine.monthCloseAdminFee(
                                memberUid, adminFeeHarv, groupWallet, monthKey);

                        GroupFirestoreManager.getInstance(context)
                                .addPayment(groupId, feePayment, new GroupFirestoreManager.IdCallback() {
                                    @Override
                                    public void onSuccess(String feeId) {
                                        GroupFirestoreManager.getInstance(context)
                                                .postSystemNotice(groupId, "Month closed",
                                                        "Fixed month closed for a member. "
                                                                + com.harvanna.app.wallet.BalanceFormatter.smart(memberPayoutHarv)
                                                                + " returned; admin fee retained.");
                                        callback.onSuccess("Month closed and recorded.");
                                    }

                                    @Override
                                    public void onError(String error) {
                                        // Member payout already recorded — soft success
                                        Log.w(TAG, "Admin fee ledger write failed: " + error);
                                        callback.onSuccess("Month closed (admin fee note failed: " + error + ")");
                                    }
                                });
                    }

                    @Override
                    public void onError(String error) {
                        callback.onError("Could not record month-close payout: " + error);
                    }
                });
    }

    /**
     * Legacy helper kept for existing call sites that only need a summary record.
     * Prefer recordClose() after the real transfer.
     */
    public static Map<String, Object> closeRecord(String memberUid, double memberPayout, double adminFee) {
        Map<String, Object> m = new HashMap<>();
        m.put("type", "FIXED_MONTH_CLOSE");
        m.put("memberUid", memberUid);
        m.put("memberPayoutHarv", memberPayout);
        m.put("adminFeeHarv", adminFee);
        m.put("monthKey", FixedPaymentEngine.currentMonthKey());
        m.put("timestamp", System.currentTimeMillis());
        m.put("status", "PENDING_ADMIN_TRANSFER");
        return m;
    }
}
