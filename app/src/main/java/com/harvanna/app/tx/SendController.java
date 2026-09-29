package com.harvanna.app.tx;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.harvanna.app.sync.AjooNodeClient;
import com.harvanna.app.wallet.BalanceFormatter;
import com.harvanna.app.wallet.WalletNodeBridge;

import org.json.JSONObject;

/**
 * Controller for the Send / Transfer screen.
 *
 * TWO-STEP FLOW:
 *
 *   Step 1 — Preview (user fills form and taps "Preview"):
 *     sendController.previewSend(recipient, amountInput, null, "AJC");
 *     → validates inputs
 *     → fetches confirmed balance + fee estimate from node
 *     → calls onPreviewReady(preview) if valid
 *     → calls onValidationError(message) if invalid
 *
 *   Step 2 — Confirm (user reviews and taps "Confirm Send"):
 *     sendController.confirmSend();
 *     → signs transaction locally with Ed25519
 *     → POSTs to node /api/v1/tx/send via TransactionRouter
 *     → calls onSendSuccess(txHash, status) if accepted
 *     → calls onSendError(message) if rejected
 *
 * USAGE in Activity:
 *
 *   sendController = new SendController(this, senderAddress);
 *   sendController.setListener(this); // implement SendController.Listener
 *
 *   // When user taps "Preview":
 *   sendController.previewSend(recipientField.getText().toString(),
 *                               amountField.getText().toString(), null, "AJC");
 *
 *   // When user taps "Confirm":
 *   sendController.confirmSend();
 *
 *   // In onPause():
 *   sendController.destroy();
 */
public class SendController {

    private static final String TAG = "SendController";

    private final Context context;
    private final String  senderAddress;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private Listener listener;
    private boolean  destroyed = false;

    // Saved between previewSend() and confirmSend()
    private SendPreview pendingPreview = null;

    // ── Data classes ──────────────────────────────────────────────────────────

    /** Everything the UI needs to show the confirmation screen. */
    public static class SendPreview {
        public final String recipient;
        public final double amountAjc;
        public final long   rawAmount;
        public final double feeAjc;
        public final long   rawFee;
        public final double totalAjc;
        public final double confirmedBalance;
        public final double remainingBalance;
        public final long   nonce;
        public final String contractAddress;  // null or empty = native AJC
        public final String tokenSymbol;

        SendPreview(String recipient, double amountAjc, long rawAmount,
                    double feeAjc, long rawFee, double confirmedBalance,
                    long nonce, String contractAddress, String tokenSymbol) {
            this.recipient        = recipient;
            this.amountAjc        = amountAjc;
            this.rawAmount        = rawAmount;
            this.feeAjc           = feeAjc;
            this.rawFee           = rawFee;
            this.totalAjc         = amountAjc + feeAjc;
            this.confirmedBalance = confirmedBalance;
            this.remainingBalance = confirmedBalance - amountAjc - feeAjc;
            this.nonce            = nonce;
            this.contractAddress  = contractAddress;
            this.tokenSymbol      = tokenSymbol;
        }

        public boolean isNativeAjc() {
            return contractAddress == null || contractAddress.trim().isEmpty();
        }

        /** Human-readable fee string for the confirmation screen. */
        public String formattedFee()       { return BalanceFormatter.fee(feeAjc); }
        public String formattedAmount()    { return BalanceFormatter.short4(amountAjc); }
        public String formattedTotal()     { return BalanceFormatter.short4(totalAjc); }
        public String formattedRemaining() { return BalanceFormatter.short4(remainingBalance); }
    }

    // ── Listener ──────────────────────────────────────────────────────────────

    public interface Listener {

        /** Called while fetching fee estimate — show a loading indicator. */
        void onPreviewLoading();

        /**
         * Preview is ready — show confirmation screen.
         * @param preview all data needed for the confirmation UI
         */
        void onPreviewReady(SendPreview preview);

        /**
         * Validation or balance check failed — show error inline.
         * @param message user-facing error message
         */
        void onValidationError(String message);

        /** Called while submitting the transaction — show loading indicator. */
        void onSendLoading();

        /**
         * Transaction accepted by the node and added to mempool.
         * @param txHash    unique transaction hash
         * @param status    "PENDING" — will be confirmed after mining
         */
        void onSendSuccess(String txHash, String status);

        /**
         * Transaction was rejected by the node or a network error occurred.
         * @param message user-facing error message
         */
        void onSendError(String message);
    }

    // ── Constructor ───────────────────────────────────────────────────────────

    public SendController(Context context, String senderAddress) {
        this.context       = context.getApplicationContext();
        this.senderAddress = senderAddress;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    // ── Step 1: Preview ───────────────────────────────────────────────────────

    /**
     * Validates inputs, fetches live balance and fee from node, builds preview.
     *
     * @param recipient       recipient HV0x address
     * @param amountInput     user-typed amount string (e.g. "1.5" or "1,500")
     * @param contractAddress null or "" for native AJC; contract address for tokens
     * @param tokenSymbol     "AJC" for native; token symbol for tokens
     */
    public void previewSend(String recipient, String amountInput,
                             String contractAddress, String tokenSymbol) {
        if (destroyed || listener == null) return;

        pendingPreview = null;

        // 1. Instant input validation (no network)
        NodeSendValidator.Result inputCheck =
                NodeSendValidator.validateInputs(recipient, amountInput);
        if (!inputCheck.valid) {
            listener.onValidationError(inputCheck.errorMessage);
            return;
        }

        double amountAjc       = BalanceFormatter.parseUserInput(amountInput);
        boolean isNative       = (contractAddress == null || contractAddress.trim().isEmpty());
        final String finalContract = isNative ? "" : contractAddress.trim();
        final String finalSymbol   = (tokenSymbol == null || tokenSymbol.trim().isEmpty())
                                     ? "AJC" : tokenSymbol.trim();

        listener.onPreviewLoading();

        if (isNative) {
            previewNativeAjc(recipient, amountAjc, finalSymbol);
        } else {
            previewTokenSend(recipient, amountAjc, finalContract, finalSymbol);
        }
    }

    private void previewNativeAjc(String recipient, double amountAjc, String symbol) {
        long rawAmount = AjooNodeClient.toRawUnits(amountAjc);

        // Fetch confirmed balance + nonce from node
        WalletNodeBridge.fetchLiveBalance(context, senderAddress,
                new WalletNodeBridge.BalanceCallback() {
                    @Override
                    public void onSuccess(double balanceAjc, long nonce, long blockHeight) {
                        if (destroyed || listener == null) return;

                        // Fetch fee estimate from node
                        AjooNodeClient.getFee(senderAddress, recipient, rawAmount,
                                new AjooNodeClient.JsonCallback() {
                                    @Override
                                    public void onSuccess(JSONObject feeJson) {
                                        if (destroyed || listener == null) return;

                                        long   rawFee  = feeJson.optLong("estimatedGas", 1000L);
                                        double feeAjc  = AjooNodeClient.toAjcDisplay(rawFee);

                                        // Balance validation
                                        NodeSendValidator.Result balCheck =
                                                NodeSendValidator.validateBalance(
                                                        senderAddress, recipient,
                                                        amountAjc, balanceAjc, feeAjc);
                                        if (!balCheck.valid) {
                                            listener.onValidationError(balCheck.errorMessage);
                                            return;
                                        }

                                        // Build preview
                                        pendingPreview = new SendPreview(
                                                recipient, amountAjc, rawAmount,
                                                feeAjc, rawFee, balanceAjc,
                                                nonce + 1, // nonce to sign with
                                                "", symbol);

                                        listener.onPreviewReady(pendingPreview);
                                    }

                                    @Override
                                    public void onError(String error) {
                                        if (destroyed || listener == null) return;
                                        Log.w(TAG, "Fee estimate failed: " + error);
                                        // Use a safe default fee if estimate fails
                                        long   rawFee = 5_000L;
                                        double feeAjc = AjooNodeClient.toAjcDisplay(rawFee);
                                        NodeSendValidator.Result balCheck =
                                                NodeSendValidator.validateBalance(
                                                        senderAddress, recipient,
                                                        amountAjc, balanceAjc, feeAjc);
                                        if (!balCheck.valid) {
                                            listener.onValidationError(balCheck.errorMessage);
                                            return;
                                        }
                                        pendingPreview = new SendPreview(
                                                recipient, amountAjc, rawAmount,
                                                feeAjc, rawFee, balanceAjc,
                                                nonce + 1, "", symbol);
                                        listener.onPreviewReady(pendingPreview);
                                    }
                                });
                    }

                    @Override
                    public void onError(String error) {
                        if (destroyed || listener == null) return;
                        listener.onValidationError(
                                "Could not fetch confirmed balance. Check your connection.");
                    }
                });
    }

    /**
     * Token send preview — uses Firestore balance (node manages AJC only).
     * Fee is still fetched from node since it's paid in AJC.
     */
    private void previewTokenSend(String recipient, double amount,
                                   String contractAddress, String symbol) {
        // For token sends, pull AJC balance from node for gas check,
        // token balance from Firestore wallet doc
        WalletNodeBridge.fetchLiveBalance(context, senderAddress,
                new WalletNodeBridge.BalanceCallback() {
                    @Override
                    public void onSuccess(double ajcBalanceForGas, long nonce, long blockHeight) {
                        if (destroyed || listener == null) return;

                        // Estimate fee with a zero-amount native call as proxy
                        AjooNodeClient.getFee(senderAddress, recipient, 0L,
                                new AjooNodeClient.JsonCallback() {
                                    @Override
                                    public void onSuccess(JSONObject feeJson) {
                                        if (destroyed || listener == null) return;
                                        long   rawFee = feeJson.optLong("estimatedGas", 1000L);
                                        double feeAjc = AjooNodeClient.toAjcDisplay(rawFee);

                                        if (ajcBalanceForGas < feeAjc) {
                                            listener.onValidationError(
                                                "Insufficient AJC for gas fee ("
                                                + BalanceFormatter.fee(feeAjc) + " needed)");
                                            return;
                                        }

                                        // Token send: amount is in token units, not raw AJC
                                        pendingPreview = new SendPreview(
                                                recipient, amount, 0L,
                                                feeAjc, rawFee, ajcBalanceForGas,
                                                nonce + 1, contractAddress, symbol);
                                        listener.onPreviewReady(pendingPreview);
                                    }

                                    @Override
                                    public void onError(String e) {
                                        if (destroyed || listener == null) return;
                                        // Default fee
                                        double feeAjc = AjooNodeClient.toAjcDisplay(5_000L);
                                        pendingPreview = new SendPreview(
                                                recipient, amount, 0L,
                                                feeAjc, 5_000L, ajcBalanceForGas,
                                                nonce + 1, contractAddress, symbol);
                                        listener.onPreviewReady(pendingPreview);
                                    }
                                });
                    }

                    @Override
                    public void onError(String error) {
                        if (destroyed || listener == null) return;
                        listener.onValidationError("Could not fetch AJC balance for gas check.");
                    }
                });
    }

    // ── Step 2: Confirm ───────────────────────────────────────────────────────

    /**
     * Submits the previewed transaction.
     * Must be called after previewSend() completes with onPreviewReady().
     * Signs locally and posts to the node — no Firestore write for native AJC.
     */
    public void confirmSend() {
        if (destroyed || listener == null) return;

        if (pendingPreview == null) {
            listener.onSendError("No transaction to confirm. Call previewSend() first.");
            return;
        }

        listener.onSendLoading();

        final SendPreview preview = pendingPreview;
        pendingPreview = null;

        // Delegate to TransactionRouter — native AJC goes through node,
        // token sends go through Firestore (handled inside TransactionRouter)
        TransactionRouter.routeTransaction(
                context,
                senderAddress,
                preview.recipient,
                java.math.BigDecimal.valueOf(preview.amountAjc),
                preview.contractAddress,
                new TransactionRouter.RouterCallback() {
                    @Override
                    public void onSuccess(String txHash) {
                        if (destroyed || listener == null) return;
                        Log.i(TAG, "Tx submitted | hash=" + txHash);
                        listener.onSendSuccess(txHash, "PENDING");
                    }

                    @Override
                    public void onError(String errorCode, String errorMessage) {
                        if (destroyed || listener == null) return;
                        Log.e(TAG, "Tx failed: " + errorCode + " — " + errorMessage);
                        String userMessage = friendlyError(errorCode, errorMessage);
                        listener.onSendError(userMessage);
                    }
                });
    }

    /** Cancel the current preview without submitting. */
    public void cancelPreview() {
        pendingPreview = null;
    }

    public boolean hasPreview() {
        return pendingPreview != null;
    }

    public SendPreview getPreview() {
        return pendingPreview;
    }

    /** Release all references. Call from Activity.onPause() or onDestroy(). */
    public void destroy() {
        destroyed      = true;
        listener       = null;
        pendingPreview = null;
    }

    // ── Private ───────────────────────────────────────────────────────────────

    /** Maps internal error codes to user-friendly messages. */
    private static String friendlyError(String code, String message) {
        if (code == null) return message != null ? message : "Transaction failed";
        switch (code) {
            case "ERR_NODE_TX":
                if (message != null && message.contains("INSUFFICIENT_BALANCE"))
                    return "Insufficient balance on the blockchain. Your balance may have changed.";
                if (message != null && message.contains("INVALID_NONCE"))
                    return "Nonce mismatch. Please wait a moment and try again.";
                if (message != null && message.contains("INVALID_SIGNATURE"))
                    return "Signature verification failed. Check that your wallet keys are correct.";
                return "Node rejected transaction: " + message;
            case "ERR_SIGNING":
                return "Failed to sign transaction. Your wallet may not be set up correctly.";
            case "ERR_WALLET_NOT_FOUND":
                return "Wallet keys not found on this device. Please restore your wallet.";
            case "ERR_RECIPIENT":
                return "Recipient wallet does not exist yet. Ask them to create an Ajoo wallet first.";
            default:
                return message != null ? message : "Transaction failed (" + code + ")";
        }
    }
}