package com.ajoo.app.group;

import android.content.Context;
import android.util.Log;

import com.ajoo.app.firestore.GroupFirestoreManager;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Payout request lifecycle — fully aligned with the Group Engine Data Contract v1.0.
 *
 * Collection: GROUPS/{groupId}/payoutRequests/{requestId}
 *
 * Status machine:
 *   PENDING → APPROVED  (after successful group-wallet → member transfer)
 *          → REJECTED
 *
 * Rules:
 * - Only an authenticated member may create a request.
 * - Only the group admin (device holding group wallet keys) may approve/reject.
 * - On APPROVED: update request status + write a CONFIRMED payments ledger entry + system notice.
 * - No reflection, no notices-as-fallback. Dedicated subcollection only.
 */
public final class GroupPayoutManager {

    private static final String TAG = "GroupPayoutManager";

    public interface Callback {
        void onSuccess(String message);
        void onError(String error);
    }

    public interface ListCallback {
        void onSuccess(List<Map<String, Object>> items);
        void onError(String error);
    }

    /**
     * Implemented by ActivityGroupDetails — wraps the existing admin transfer
     * that signs with group wallet keys from SecureWalletStorage.
     */
    public interface GroupWalletTransfer {
        void transfer(String fromGroupWallet, String toMemberWallet, double amountAjc, Callback callback);
    }

    private GroupPayoutManager() {}

    // ── Member: create payout request ────────────────────────────────────

    /**
     * Member creates a payout request for the admin to review.
     * amountAjc should typically equal one full cycle pot (contributionAmount × totalSlots)
     * for rotating groups, or the fixed-month return for Fixed groups.
     */
    public static void requestPayout(Context context,
                                     String groupId,
                                     String requesterWallet,
                                     double amountAjc,
                                     int slot,
                                     Callback callback) {
        if (callback == null) return;

        String err = GroupValidation.firstError(
                GroupValidation.requireUid(currentUid(), "Not signed in."),
                GroupValidation.requireGroupId(groupId),
                GroupValidation.requireValidWallet(requesterWallet, "Member wallet"),
                GroupValidation.requirePositiveAmount(amountAjc),
                GroupValidation.requireSlot(slot)
        );
        if (err != null) {
            callback.onError(err);
            return;
        }
        String uid = currentUid();

        Map<String, Object> fields = new HashMap<>();
        fields.put("type", "PAYOUT");
        fields.put("status", "PENDING");
        fields.put("requesterUid", uid);
        fields.put("requesterWallet", requesterWallet.trim());
        fields.put("amountAjc", amountAjc);
        fields.put("slot", slot);
        fields.put("createdAt", System.currentTimeMillis());
        fields.put("message", "Payout request: "
                + com.ajoo.app.wallet.BalanceFormatter.smart(amountAjc)
                + " · slot " + slot);

        GroupFirestoreManager.getInstance(context)
                .addPayoutRequest(groupId, fields, new GroupFirestoreManager.IdCallback() {
                    @Override
                    public void onSuccess(String id) {
                        // Best-effort activity notice — never blocks the real request.
                        GroupFirestoreManager.getInstance(context)
                                .postSystemNotice(groupId,
                                        "Payout requested",
                                        "A member requested a payout of "
                                                + com.ajoo.app.wallet.BalanceFormatter.smart(amountAjc));
                        callback.onSuccess("Payout request submitted.");
                    }

                    @Override
                    public void onError(String error) {
                        callback.onError("Could not submit payout request: " + error);
                    }
                });
    }

    // ── Admin: list pending requests ─────────────────────────────────────

    /**
     * Returns only requests whose status is still PENDING.
     * Uses the dedicated payoutRequests subcollection.
     */
    public static void listPendingPayoutRequests(Context context,
                                                 String groupId,
                                                 ListCallback callback) {
        if (callback == null) return;
        if (groupId == null || groupId.isEmpty()) {
            callback.onError("Missing group id.");
            return;
        }

        GroupFirestoreManager.getInstance(context)
                .getPayoutRequests(groupId, new GroupFirestoreManager.ListCallback() {
                    @Override
                    public void onSuccess(List<Map<String, Object>> items) {
                        List<Map<String, Object>> pending = new ArrayList<>();
                        if (items != null) {
                            for (Map<String, Object> item : items) {
                                String status = String.valueOf(item.get("status"));
                                if ("PENDING".equalsIgnoreCase(status)) {
                                    pending.add(item);
                                }
                            }
                        }
                        callback.onSuccess(pending);
                    }

                    @Override
                    public void onError(String error) {
                        callback.onError(error);
                    }
                });
    }

    // ── Admin: approve + transfer ─────────────────────────────────────────

    /**
     * Admin approves a payout request and executes the on-chain transfer
     * from the group wallet to the member wallet.
     *
     * Sequence (contract-compliant):
     * 1. Validate inputs
     * 2. Execute transfer via GroupWalletTransfer
     * 3. On success → update payoutRequest status to APPROVED
     * 4. Write CONFIRMED payments ledger entry
     * 5. Post system notice
     */
    public static void approvePayoutAndTransfer(Context context,
                                                String groupId,
                                                String requestId,
                                                String groupWalletAddress,
                                                String memberWalletAddress,
                                                double amountAjc,
                                                GroupWalletTransfer transferExecutor,
                                                Callback callback) {
        if (callback == null) return;

        String adminUid = currentUid();
        if (adminUid.isEmpty()) {
            callback.onError("Not signed in.");
            return;
        }
        if (groupId == null || groupId.isEmpty()) {
            callback.onError("Missing group id.");
            return;
        }
        if (requestId == null || requestId.isEmpty()) {
            callback.onError("Missing payout request id.");
            return;
        }
        if (!isValidAjcWallet(groupWalletAddress) || !isValidAjcWallet(memberWalletAddress)) {
            callback.onError("Invalid wallet addresses.");
            return;
        }
        if (amountAjc <= 0) {
            callback.onError("Invalid amount.");
            return;
        }
        if (transferExecutor == null) {
            callback.onError("Transfer handler missing.");
            return;
        }

        // THE FIX: re-fetch the request and confirm it's actually still
        // PENDING before touching real money — previously this executed
        // the on-chain transfer immediately with no check at all, so a
        // double-tap or a stale reopened sheet (still showing a request
        // that was already approved a moment ago) could trigger two real
        // transfers of group funds for the same request. Same principle
        // GroupLoanManager.recordRepayment() already uses: never trust
        // whatever the UI last rendered, re-check the real current state
        // right before the money moves.
        GroupFirestoreManager.getInstance(context)
                .getPayoutRequest(groupId, requestId, new GroupFirestoreManager.DocCallback() {
                    @Override
                    public void onSuccess(Map<String, Object> request) {
                        if (request == null || request.isEmpty()) {
                            callback.onError("Payout request not found.");
                            return;
                        }
                        String status = String.valueOf(request.get("status"));
                        if (!"PENDING".equalsIgnoreCase(status)) {
                            callback.onError("This request was already " + status.toLowerCase()
                                    + " — nothing to approve.");
                            return;
                        }
                        _executeTransferAndFinalize(context, groupId, requestId, groupWalletAddress,
                                memberWalletAddress, amountAjc, adminUid, transferExecutor, callback);
                    }

                    @Override
                    public void onError(String error) {
                        callback.onError("Could not verify request status: " + error);
                    }
                });
    }

    private static void _executeTransferAndFinalize(Context context,
                                                     String groupId,
                                                     String requestId,
                                                     String groupWalletAddress,
                                                     String memberWalletAddress,
                                                     double amountAjc,
                                                     String adminUid,
                                                     GroupWalletTransfer transferExecutor,
                                                     Callback callback) {
        transferExecutor.transfer(groupWalletAddress, memberWalletAddress, amountAjc,
                new Callback() {
                    @Override
                    public void onSuccess(String txHashOrMsg) {
                        final String txRef = txHashOrMsg != null ? txHashOrMsg : "";

                        // 1. Update the payout request document
                        Map<String, Object> requestUpdate = new HashMap<>();
                        requestUpdate.put("status", "APPROVED");
                        requestUpdate.put("decidedAt", System.currentTimeMillis());
                        requestUpdate.put("decidedBy", adminUid);
                        requestUpdate.put("txRef", txRef);

                        GroupFirestoreManager.getInstance(context)
                                .updatePayoutRequest(groupId, requestId, requestUpdate,
                                        new GroupFirestoreManager.SimpleCallback() {
                                            @Override
                                            public void onSuccess() {
                                                writePayoutLedgerAndNotify(context, groupId,
                                                        requestId, groupWalletAddress,
                                                        memberWalletAddress, amountAjc,
                                                        adminUid, txRef, callback);
                                            }

                                            @Override
                                            public void onError(String error) {
                                                // Chain transfer already succeeded — still write ledger
                                                // so the money movement is recorded even if status patch fails.
                                                Log.w(TAG, "Payout request status update failed: " + error);
                                                writePayoutLedgerAndNotify(context, groupId,
                                                        requestId, groupWalletAddress,
                                                        memberWalletAddress, amountAjc,
                                                        adminUid, txRef, callback);
                                            }
                                        });
                    }

                    @Override
                    public void onError(String error) {
                        callback.onError("Transfer failed: " + error);
                    }
                });
    }

    private static void writePayoutLedgerAndNotify(Context context,
                                                   String groupId,
                                                   String requestId,
                                                   String groupWallet,
                                                   String memberWallet,
                                                   double amountAjc,
                                                   String adminUid,
                                                   String txRef,
                                                   Callback callback) {
        if (txRef == null || txRef.trim().isEmpty()) {
            callback.onError("Payout has no on-chain tx hash — ledger not marked CONFIRMED");
            return;
        }
        GroupOnChainGate.requireConfirmedTx(txRef, new GroupOnChainGate.Callback() {
            @Override
            public void onConfirmed(String hash, long blockHeight) {
                writeConfirmedPayoutRow(context, groupId, requestId, groupWallet,
                        memberWallet, amountAjc, adminUid, hash, callback);
            }
            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    private static void writeConfirmedPayoutRow(Context context,
                                                   String groupId,
                                                   String requestId,
                                                   String groupWallet,
                                                   String memberWallet,
                                                   double amountAjc,
                                                   String adminUid,
                                                   String txRef,
                                                   Callback callback) {
        Map<String, Object> payment = new HashMap<>();
        payment.put("type", "PAYOUT");
        payment.put("status", "CONFIRMED");
        payment.put("from", groupWallet);
        payment.put("to", memberWallet);
        payment.put("amountAjc", amountAjc);
        payment.put("requestId", requestId != null ? requestId : "");
        payment.put("adminUid", adminUid);
        payment.put("timestamp", System.currentTimeMillis());
        payment.put("txRef", txRef != null ? txRef : "");

        GroupFirestoreManager.getInstance(context)
                .addPayment(groupId, payment, new GroupFirestoreManager.IdCallback() {
                    @Override
                    public void onSuccess(String id) {
                        GroupFirestoreManager.getInstance(context)
                                .postSystemNotice(groupId, "Payout sent",
                                        "Admin paid out "
                                                + com.ajoo.app.wallet.BalanceFormatter.smart(amountAjc)
                                                + " to " + shortAddr(memberWallet));
                        callback.onSuccess("Payout approved and sent.");
                    }

                    @Override
                    public void onError(String error) {
                        // Money already moved on-chain. Surface success with a note.
                        Log.w(TAG, "Payout ledger write failed after successful transfer: " + error);
                        callback.onSuccess("Payout sent (ledger note failed: " + error + ")");
                    }
                });
    }

    // ── Admin: reject ────────────────────────────────────────────────────

    /**
     * Admin rejects a pending payout request.
     * Updates the request document and writes an audit entry in payments.
     */
    public static void rejectPayout(Context context,
                                    String groupId,
                                    String requestId,
                                    Callback callback) {
        if (callback == null) return;

        String adminUid = currentUid();
        if (adminUid.isEmpty()) {
            callback.onError("Not signed in.");
            return;
        }
        if (groupId == null || groupId.isEmpty() || requestId == null || requestId.isEmpty()) {
            callback.onError("Missing group or request id.");
            return;
        }

        Map<String, Object> requestUpdate = new HashMap<>();
        requestUpdate.put("status", "REJECTED");
        requestUpdate.put("decidedAt", System.currentTimeMillis());
        requestUpdate.put("decidedBy", adminUid);

        GroupFirestoreManager.getInstance(context)
                .updatePayoutRequest(groupId, requestId, requestUpdate,
                        new GroupFirestoreManager.SimpleCallback() {
                            @Override
                            public void onSuccess() {
                                // Audit entry in the payments ledger
                                Map<String, Object> payment = new HashMap<>();
                                payment.put("type", "PAYOUT_REJECTED");
                                payment.put("status", "REJECTED");
                                payment.put("requestId", requestId);
                                payment.put("decidedBy", adminUid);
                                payment.put("timestamp", System.currentTimeMillis());

                                GroupFirestoreManager.getInstance(context)
                                        .addPayment(groupId, payment, new GroupFirestoreManager.IdCallback() {
                                            @Override
                                            public void onSuccess(String id) {
                                                callback.onSuccess("Payout request rejected.");
                                            }

                                            @Override
                                            public void onError(String error) {
                                                // Status already updated — treat as success
                                                callback.onSuccess("Payout request rejected.");
                                            }
                                        });
                            }

                            @Override
                            public void onError(String error) {
                                callback.onError("Could not reject request: " + error);
                            }
                        });
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private static String currentUid() {
        return FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
    }

    private static boolean isValidAjcWallet(String address) {
        return address != null && address.trim().startsWith("HV0x") && address.trim().length() >= 10;
    }

    private static String shortAddr(String a) {
        if (a == null || a.length() < 12) return a != null ? a : "";
        return a.substring(0, 8) + "…" + a.substring(a.length() - 4);
    }
}