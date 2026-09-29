package com.harvanna.wallet.group;

import android.content.Context;
import android.util.Log;

import com.harvanna.wallet.firestore.GroupFirestoreManager;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Group loan lifecycle — aligned with Group Engine Data Contract v1.0.
 *
 * Collection: GROUPS/{groupId}/loans/{loanId}
 *
 * Status machine:
 *   PENDING ──→ ACTIVE ──→ REPAID
 *          │           └──→ DEFAULTED
 *          └──→ REJECTED
 */
public final class GroupLoanManager {

    private static final String TAG = "GroupLoanManager";

    public interface Callback {
        void onSuccess(String message);
        void onError(String error);
    }

    public interface ListCallback {
        void onSuccess(List<Map<String, Object>> items);
        void onError(String error);
    }

    private GroupLoanManager() {}

    // ── Interest accrual ─────────────────────────────────────────────────

    private static long periodMillis(String period) {
        String p = period != null ? period.toUpperCase() : "MONTHLY";
        switch (p) {
            case "DAILY":    return 24L * 60 * 60 * 1000;
            case "ANNUALLY": return 365L * 24 * 60 * 60 * 1000;
            case "MONTHLY":
            default:         return 30L * 24 * 60 * 60 * 1000;
        }
    }

    public static double computeAccruedTotalDue(double principal, double interestRatePercent,
                                                 String period, long disbursedAtMillis, long nowMillis) {
        if (disbursedAtMillis <= 0) return principal;
        long elapsed = nowMillis - disbursedAtMillis;
        if (elapsed <= 0) return principal;

        long periodMs = periodMillis(period);
        double periodsElapsed = elapsed / (double) periodMs;
        return principal + (principal * (interestRatePercent / 100.0) * periodsElapsed);
    }

    public static double computeAccruedTotalDue(double principal, double interestRatePercent,
                                                 String period, long disbursedAtMillis) {
        return computeAccruedTotalDue(principal, interestRatePercent, period,
                disbursedAtMillis, System.currentTimeMillis());
    }

    // ── Member: apply ────────────────────────────────────────────────────

    public static void applyForLoan(Context context,
                                    String groupId,
                                    String borrowerWallet,
                                    double principalAjc,
                                    double interestRatePercent,
                                    String interestPeriod,
                                    String note,
                                    Callback callback) {
        if (callback == null) return;

        String err = GroupValidation.firstError(
                GroupValidation.requireUid(currentUid(), "Not signed in."),
                GroupValidation.requireGroupId(groupId),
                GroupValidation.requirePositiveAmount(principalAjc),
                GroupValidation.requireValidWallet(borrowerWallet, "Borrower wallet")
        );
        if (err != null) {
            callback.onError(err);
            return;
        }

        String uid = currentUid();
        String period = (interestPeriod != null && !interestPeriod.trim().isEmpty())
                ? interestPeriod.trim().toUpperCase() : "MONTHLY";

        Map<String, Object> loan = new HashMap<>();
        loan.put("status", "PENDING");
        loan.put("borrowerUid", uid);
        loan.put("borrowerWallet", borrowerWallet.trim());
        loan.put("principalAjc", principalAjc);
        loan.put("interestRate", interestRatePercent);
        loan.put("interestPeriod", period);
        loan.put("note", note != null ? note : "");
        loan.put("createdAt", System.currentTimeMillis());

        MemberNameResolver.resolve(context, uid, "A member", memberName -> {
            GroupFirestoreManager.getInstance(context)
                    .addLoan(groupId, loan, new GroupFirestoreManager.IdCallback() {
                        @Override
                        public void onSuccess(String loanId) {
                            GroupFirestoreManager.getInstance(context)
                                    .postSystemNotice(groupId, "Loan applied",
                                            memberName + " applied for a loan of "
                                                    + com.harvanna.wallet.wallet.BalanceFormatter.smart(principalAjc));
                            callback.onSuccess("Loan application submitted.");
                        }

                        @Override
                        public void onError(String error) {
                            if (com.harvanna.wallet.offline.NetworkStatus.isConnectivityError(error)) {
                                Map<String, Object> payload = new HashMap<>();
                                payload.put("groupId", groupId);
                                payload.put("borrowerWallet", borrowerWallet);
                                payload.put("principalAjc", principalAjc);
                                payload.put("interestRatePercent", interestRatePercent);
                                payload.put("interestPeriod", period);
                                payload.put("note", note);
                                com.harvanna.wallet.offline.OfflineActionQueue.enqueue(context, "LOAN_APPLY", payload);

                                loan.put("_docId", "temp_" + UUID.randomUUID().toString());
                                List<HashMap<String, Object>> cachedLoans = com.harvanna.wallet.offline.OfflineCacheManager.loadList(context, "loans_" + groupId);
                                if (cachedLoans == null) cachedLoans = new ArrayList<>();
                                cachedLoans.add(0, new HashMap<>(loan));
                                com.harvanna.wallet.offline.OfflineCacheManager.saveList(context, "loans_" + groupId, cachedLoans);

                                callback.onSuccess("You're offline — application queued and will submit when online.");
                            } else {
                                callback.onError(error);
                            }
                        }
                    });
        });
    }

    // ── Admin: approve + disburse ────────────────────────────────────────

    public static void approveAndDisburse(Context context,
                                          String groupId,
                                          String loanId,
                                          String groupWalletAddress,
                                          String borrowerWallet,
                                          double principalAjc,
                                          GroupPayoutManager.GroupWalletTransfer transferExecutor,
                                          Callback callback) {
        if (callback == null) return;

        String adminUid = currentUid();
        String err = GroupValidation.firstError(
                GroupValidation.requireUid(adminUid, "Not signed in."),
                GroupValidation.requireGroupId(groupId),
                GroupValidation.isBlank(loanId) ? "Missing loan id." : null,
                GroupValidation.requireValidWallet(groupWalletAddress, "Group wallet"),
                GroupValidation.requireValidWallet(borrowerWallet, "Borrower wallet"),
                GroupValidation.requirePositiveAmount(principalAjc)
        );
        if (err != null) {
            callback.onError(err);
            return;
        }
        if (transferExecutor == null) {
            callback.onError("Transfer handler missing.");
            return;
        }

        GroupFirestoreManager.getInstance(context).getLoan(groupId, loanId, new GroupFirestoreManager.DocCallback() {
            @Override
            public void onSuccess(Map<String, Object> loanData) {
                String borrowerUid = loanData != null && loanData.containsKey("borrowerUid") ? loanData.get("borrowerUid").toString() : "";

                transferExecutor.transfer(groupWalletAddress, borrowerWallet, principalAjc,
                        new GroupPayoutManager.Callback() {
                            @Override
                            public void onSuccess(String txRef) {
                                final String ref = txRef != null ? txRef : "";
                                final long now = System.currentTimeMillis();

                                Map<String, Object> payment = new HashMap<>();
                                payment.put("type", "LOAN_DISBURSE");
                                payment.put("status", "CONFIRMED");
                                payment.put("loanId", loanId);
                                payment.put("from", groupWalletAddress);
                                payment.put("to", borrowerWallet);
                                payment.put("amountAjc", principalAjc);
                                payment.put("adminUid", adminUid);
                                payment.put("txRef", ref);
                                payment.put("timestamp", now);
                                payment.put("confirmedAt", now);

                                GroupFirestoreManager.getInstance(context)
                                        .addPayment(groupId, payment, new GroupFirestoreManager.IdCallback() {
                                            @Override
                                            public void onSuccess(String id) {
                                                updateLoanToActive(context, groupId, loanId, adminUid, borrowerUid, ref, now, callback);
                                            }
                                            @Override
                                            public void onError(String error) {
                                                Log.w(TAG, "LOAN_DISBURSE ledger write failed: " + error);
                                                updateLoanToActive(context, groupId, loanId, adminUid, borrowerUid, ref, now, callback);
                                            }
                                        });
                            }
                            @Override
                            public void onError(String error) {
                                callback.onError("Disburse failed: " + error);
                            }
                        });
            }
            @Override
            public void onError(String error) {
                callback.onError("Could not fetch loan details for approval: " + error);
            }
        });
    }

    private static void updateLoanToActive(Context context, String groupId, String loanId,
                                           String adminUid, String borrowerUid, String txRef, long now,
                                           Callback callback) {
        Map<String, Object> loanUpdate = new HashMap<>();
        loanUpdate.put("status", "ACTIVE");
        loanUpdate.put("approvedAt", now);
        loanUpdate.put("approvedBy", adminUid);
        loanUpdate.put("disbursedAt", now);
        loanUpdate.put("txDisburseRef", txRef);

        GroupFirestoreManager.getInstance(context)
                .updateLoan(groupId, loanId, loanUpdate, new GroupFirestoreManager.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        MemberNameResolver.resolve(context, borrowerUid, "a member", borrowerName -> {
                            GroupFirestoreManager.getInstance(context)
                                    .postSystemNotice(groupId, "Loan disbursed",
                                            "Loan was approved and disbursed to " + borrowerName + ".");
                            callback.onSuccess("Loan approved and disbursed.");
                        });
                    }

                    @Override
                    public void onError(String error) {
                        callback.onSuccess("Loan disbursed, but status update failed: " + error);
                    }
                });
    }

    // ── Admin: reject ────────────────────────────────────────────────────

    public static void rejectLoan(Context context, String groupId, String loanId, Callback callback) {
        if (callback == null) return;

        String adminUid = currentUid();
        String err = GroupValidation.firstError(
                GroupValidation.requireUid(adminUid, "Not signed in."),
                GroupValidation.requireGroupId(groupId),
                GroupValidation.isBlank(loanId) ? "Missing loan id." : null
        );
        if (err != null) {
            callback.onError(err);
            return;
        }

        Map<String, Object> loanUpdate = new HashMap<>();
        loanUpdate.put("status", "REJECTED");
        loanUpdate.put("decidedAt", System.currentTimeMillis());
        loanUpdate.put("decidedBy", adminUid);

        GroupFirestoreManager.getInstance(context)
                .updateLoan(groupId, loanId, loanUpdate, new GroupFirestoreManager.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        Map<String, Object> payment = new HashMap<>();
                        payment.put("type", "LOAN_REJECTED");
                        payment.put("status", "REJECTED");
                        payment.put("loanId", loanId);
                        payment.put("decidedBy", adminUid);
                        payment.put("timestamp", System.currentTimeMillis());

                        GroupFirestoreManager.getInstance(context)
                                .addPayment(groupId, payment, new GroupFirestoreManager.IdCallback() {
                                    @Override
                                    public void onSuccess(String id) { callback.onSuccess("Loan rejected."); }
                                    @Override
                                    public void onError(String error) { callback.onSuccess("Loan rejected."); }
                                });
                    }

                    @Override
                    public void onError(String error) {
                        if (com.harvanna.wallet.offline.NetworkStatus.isConnectivityError(error)) {
                            Map<String, Object> payload = new HashMap<>();
                            payload.put("groupId", groupId);
                            payload.put("loanId", loanId);
                            com.harvanna.wallet.offline.OfflineActionQueue.enqueue(context, "LOAN_REJECT", payload);

                            List<HashMap<String, Object>> cachedLoans = com.harvanna.wallet.offline.OfflineCacheManager.loadList(context, "loans_" + groupId);
                            if (cachedLoans != null) {
                                for (HashMap<String, Object> cachedLoan : cachedLoans) {
                                    if (loanId.equals(cachedLoan.get("_docId"))) {
                                        cachedLoan.put("status", "REJECTED");
                                        break;
                                    }
                                }
                                com.harvanna.wallet.offline.OfflineCacheManager.saveList(context, "loans_" + groupId, cachedLoans);
                            }
                            callback.onSuccess("You're offline — rejection queued and will sync when online.");
                        } else {
                            callback.onError("Could not reject loan: " + error);
                        }
                    }
                });
    }

    // ── Member: repay ────────────────────────────────────────────────────

    public static void recordRepayment(Context context,
                                       String groupId,
                                       String loanId,
                                       String fromMemberWallet,
                                       String groupWalletAddress,
                                       double amountAjc,
                                       String txRef,
                                       boolean markFullyRepaid,
                                       Callback callback) {
        if (callback == null) return;

        String err = GroupValidation.firstError(
                GroupValidation.requireGroupId(groupId),
                GroupValidation.isBlank(loanId) ? "Missing loan id." : null,
                GroupValidation.requireValidWallet(fromMemberWallet, "Member wallet"),
                GroupValidation.requireValidWallet(groupWalletAddress, "Group wallet"),
                GroupValidation.requirePositiveAmount(amountAjc)
        );
        if (err != null) {
            callback.onError(err);
            return;
        }

        final Context appContext = context.getApplicationContext();
        final GroupFirestoreManager fs = GroupFirestoreManager.getInstance(appContext);

        fs.getLoan(groupId, loanId, new GroupFirestoreManager.DocCallback() {
            @Override
            public void onSuccess(Map<String, Object> loan) {
                if (loan == null || loan.isEmpty()) {
                    callback.onError("Loan not found.");
                    return;
                }
                String status = String.valueOf(loan.get("status"));
                if (!"ACTIVE".equalsIgnoreCase(status)) {
                    callback.onError("This loan is not active (" + status + ") — nothing to repay.");
                    return;
                }

                double principal = _asDouble(loan.get("principalAjc"));
                double interestRate = _asDouble(loan.get("interestRate"));
                String interestPeriod = loan.get("interestPeriod") != null
                        ? String.valueOf(loan.get("interestPeriod")) : "MONTHLY";
                long disbursedAt = (long) _asDouble(loan.get("disbursedAt"));

                fs.getPayments(groupId, new GroupFirestoreManager.ListCallback() {
                    @Override
                    public void onSuccess(List<Map<String, Object>> payments) {
                        double alreadyRepaid = 0;
                        if (payments != null) {
                            for (Map<String, Object> p : payments) {
                                if (!"LOAN_REPAY".equalsIgnoreCase(String.valueOf(p.get("type")))) continue;
                                if (!loanId.equals(String.valueOf(p.get("loanId")))) continue;
                                Object stObj = p.get("status");
                                String st = stObj != null ? String.valueOf(stObj) : "";
                                if (!st.isEmpty() && !"CONFIRMED".equalsIgnoreCase(st)) continue;
                                alreadyRepaid += _asDouble(p.get("amountAjc"));
                            }
                        }

                        double accruedDue = computeAccruedTotalDue(principal, interestRate, interestPeriod, disbursedAt);
                        double remainingDue = Math.max(0, accruedDue - alreadyRepaid);

                        if (remainingDue <= 0.0001) {
                            callback.onError("This loan is already fully repaid.");
                            return;
                        }
                        
                        if (amountAjc > remainingDue + 0.0001) {
                            callback.onError("Amount exceeds what's owed ("
                                    + com.harvanna.wallet.wallet.BalanceFormatter.smart(remainingDue) + " AJC remaining).");
                            return;
                        }

                        boolean fullyRepaid = amountAjc >= remainingDue - 0.0001;
                        _writeRepayment(context, fs, groupId, loanId, fromMemberWallet, groupWalletAddress,
                                amountAjc, txRef, fullyRepaid, callback);
                    }

                    @Override
                    public void onError(String error) {
                        callback.onError("Could not verify loan balance: " + error);
                    }
                });
            }

            @Override
            public void onError(String error) {
                callback.onError("Could not load loan: " + error);
            }
        });
    }

    private static double _asDouble(Object v) {
        if (v instanceof Number) return ((Number) v).doubleValue();
        try { return Double.parseDouble(String.valueOf(v)); } catch (Exception e) { return 0; }
    }

    private static void _writeRepayment(Context context,
                                        GroupFirestoreManager fs,
                                        String groupId,
                                        String loanId,
                                        String fromMemberWallet,
                                        String groupWalletAddress,
                                        double amountAjc,
                                        String txRef,
                                        boolean markFullyRepaid,
                                        Callback callback) {
        final long now = System.currentTimeMillis();
        final String ref = txRef != null ? txRef : "";

        Map<String, Object> payment = new HashMap<>();
        payment.put("type", "LOAN_REPAY");
        payment.put("status", "CONFIRMED");
        payment.put("loanId", loanId);
        payment.put("from", fromMemberWallet.trim());
        payment.put("to", groupWalletAddress.trim());
        payment.put("amountAjc", amountAjc);
        payment.put("txRef", ref);
        payment.put("timestamp", now);
        payment.put("confirmedAt", now);
        payment.put("borrowerUid", currentUid());

        fs.addPayment(groupId, payment, new GroupFirestoreManager.IdCallback() {
            @Override
            public void onSuccess(String id) {
                MemberNameResolver.resolve(context, currentUid(), "A member", memberName -> {
                    if (markFullyRepaid) {
                        Map<String, Object> loanUpdate = new HashMap<>();
                        loanUpdate.put("status", "REPAID");
                        loanUpdate.put("repaidAt", now);
                        loanUpdate.put("txRepayRef", ref);

                        fs.updateLoan(groupId, loanId, loanUpdate,
                                new GroupFirestoreManager.SimpleCallback() {
                                    @Override
                                    public void onSuccess() {
                                        fs.postSystemNotice(groupId, "Loan repaid",
                                                memberName + " fully repaid their loan.");
                                        callback.onSuccess("Repayment recorded — loan closed.");
                                    }

                                    @Override
                                    public void onError(String error) {
                                        callback.onSuccess("Repayment recorded (loan status update failed: "
                                                + error + ")");
                                    }
                                });
                    } else {
                        fs.postSystemNotice(groupId, "Loan repayment",
                                memberName + " made a partial loan repayment of "
                                        + com.harvanna.wallet.wallet.BalanceFormatter.smart(amountAjc) + ".");
                        callback.onSuccess("Repayment recorded.");
                    }
                });
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public static void recordRepayment(Context context,
                                       String groupId,
                                       String loanId,
                                       String fromMemberWallet,
                                       String groupWalletAddress,
                                       double amountAjc,
                                       String txRef,
                                       Callback callback) {
        recordRepayment(context, groupId, loanId, fromMemberWallet, groupWalletAddress,
                amountAjc, txRef, false, callback);
    }

    // ── List ─────────────────────────────────────────────────────────────

    public static void listLoans(Context context, String groupId, ListCallback callback) {
        if (callback == null) return;
        if (GroupValidation.isBlank(groupId)) {
            callback.onError("Missing group id.");
            return;
        }
        GroupFirestoreManager.getInstance(context).getLoans(groupId, new GroupFirestoreManager.ListCallback() {
            @Override
            public void onSuccess(List<Map<String, Object>> items) {
                callback.onSuccess(items != null ? items : new ArrayList<>());
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    private static String currentUid() {
        return FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
    }

    // ── Offline replay ──────────────────────────────────────────────────

    public static void replayRepaymentFromQueue(Context context, Map<String, Object> payload,
                                                 com.harvanna.wallet.offline.OfflineActionQueue.FlushCallback callback) {
        String groupId = _s(payload, "groupId");
        String loanId = _s(payload, "loanId");
        String fromWallet = _s(payload, "fromWallet");
        String groupWallet = _s(payload, "groupWallet");
        double amountAjc = payload.get("amountAjc") instanceof Number ? ((Number) payload.get("amountAjc")).doubleValue() : 0;
        String txRef = _s(payload, "txRef");
        boolean markFullyRepaid = Boolean.TRUE.equals(payload.get("markFullyRepaid"));

        recordRepayment(context, groupId, loanId, fromWallet, groupWallet, amountAjc, txRef, markFullyRepaid,
                new Callback() {
                    @Override public void onSuccess(String message) { if (callback != null) callback.onSuccess(); }
                    @Override public void onError(String error) { if (callback != null) callback.onError(error); }
                });
    }

    public static void replayLoanApplyFromQueue(Context context, Map<String, Object> payload,
                                                com.harvanna.wallet.offline.OfflineActionQueue.FlushCallback callback) {
        String groupId = _s(payload, "groupId");
        String borrowerWallet = _s(payload, "borrowerWallet");
        double principalAjc = payload.get("principalAjc") instanceof Number ? ((Number) payload.get("principalAjc")).doubleValue() : 0;
        double interestRatePercent = payload.get("interestRatePercent") instanceof Number ? ((Number) payload.get("interestRatePercent")).doubleValue() : 0;
        String interestPeriod = _s(payload, "interestPeriod");
        String note = _s(payload, "note");

        applyForLoan(context, groupId, borrowerWallet, principalAjc, interestRatePercent, interestPeriod, note, new Callback() {
            @Override public void onSuccess(String message) { if (callback != null) callback.onSuccess(); }
            @Override public void onError(String error) { if (callback != null) callback.onError(error); }
        });
    }

    public static void replayLoanRejectFromQueue(Context context, Map<String, Object> payload,
                                                 com.harvanna.wallet.offline.OfflineActionQueue.FlushCallback callback) {
        String groupId = _s(payload, "groupId");
        String loanId = _s(payload, "loanId");

        rejectLoan(context, groupId, loanId, new Callback() {
            @Override public void onSuccess(String message) { if (callback != null) callback.onSuccess(); }
            @Override public void onError(String error) { if (callback != null) callback.onError(error); }
        });
    }

    private static String _s(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v != null ? String.valueOf(v) : "";
    }
}