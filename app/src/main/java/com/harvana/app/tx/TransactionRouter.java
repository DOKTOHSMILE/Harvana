package com.harvana.app.tx;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import com.harvana.app.CoinInfo;
import com.harvana.app.crypto.Ed25519;
import com.harvana.app.ui.PaymentSuccessSheet;
import com.harvana.app.wallet.BalanceFormatter;
import com.harvana.app.sync.HarvanaNodeClient;
import com.harvana.app.wallet.WalletManager;
import com.harvana.app.wallet.WalletNodeBridge;

import org.json.JSONObject;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

/**
 * Main transaction router for Harvana Chain.
 *
 * ROUTING RULES:
 *   Native HARV transfer  → signed locally → POST to node /api/v1/tx/send
 *                          Node handles: validation, mempool, balance in RocksDB, Firestore bridge
 *
 *   Listed/unlisted token transfer → NOT IMPLEMENTED. This app has never
 *   actually offered a non-native asset to send (every send screen's asset
 *   picker only ever lists HARV), so the old Firestore-simulated token path
 *   (gas.GasCalculator, gas.GasFee, tx.CompleteFeeEngine, model.Transaction,
 *   network.BlockProducer, treasury.TreasuryManager.distributeFees) was
 *   dead code and has been removed rather than left half-wired. A
 *   non-empty contractAddress here now fails fast with a clear error
 *   instead of silently going nowhere.
 *
 * CANONICAL MESSAGE (native HARV):
 *   The wallet signs exactly:  from + to + rawAmount + nonce
 *   This matches server TransactionPayload.getCanonicalMessage() — no separators.
 */
public class TransactionRouter {

    private static final String TAG = "TxRouter";

    // ── UI bridge interface ───────────────────────────────────────────────────

    public interface RouterCallback {
        void onSuccess(String txHash);
        void onError(String errorCode, String errorMessage);
    }

    public static void routeTransaction(android.content.Context context, String sender,
                                         String recipient, BigDecimal amount,
                                         String contractAddress, final RouterCallback callback) {
        routeTransaction(context, sender, recipient, amount, contractAddress, null, callback);
    }

    /** Memo-aware version - the group ecosystem uses this so contributions,
     *  loan disbursements, and admin transfers all carry a real on-chain
     *  reason, not just a Firestore-side note that vanishes if you're
     *  looking at the chain directly. Existing callers passing no memo are
     *  completely unaffected - this is additive, not a signature change. */
    public static void routeTransaction(android.content.Context context, String sender,
                                         String recipient, BigDecimal amount,
                                         String contractAddress, String memo, final RouterCallback callback) {
        TransactionRequest req = new TransactionRequest();
        req.context = context;
        req.setSender(sender);
        req.setRecipient(recipient);
        req.setAmount(amount);
        req.setContractAddress(contractAddress);
        req.setMemo(memo);

        // Native HARV — route through node
        if (contractAddress == null || contractAddress.trim().isEmpty()) {
            // Payment buttons stay on "Pending…" from now until the chain confirms (or this fails), and the
            // full-screen success sheet follows on whichever screen is in front.
            final PaymentTracker.Handle inFlight = PaymentTracker.begin();
            routeNativeHarvThroughNode(req, new ExecutionCallback() {
                @Override public void onSuccess(ExecutionResult result) {
                    final String hash = result != null && result.txHash != null ? result.txHash : "";
                    try {
                        if (memo != null && memo.startsWith("ACTION_FEE:")) throw new IllegalStateException("fee only");
                        PaymentSuccessSheet.Info info = new PaymentSuccessSheet.Info();
                        TxKind kind = TxKind.of(memo, false);
                        info.title = kind != null ? kind.title : "Payment sent";
                        info.amount = BalanceFormatter.smartDecimal(amount.doubleValue()) + " " + CoinInfo.ticker();
                        info.rows = new String[][] {{"To", recipient}, {"From", sender},
                                {"For", kind != null ? kind.detail : (memo == null ? "" : memo)}, {"Transaction", hash}};
                        info.txHash = hash;
                        info.note = "The network has accepted it. It shows as confirmed once the next block has it.";
                        PaymentTracker.success(info);
                    } catch (Throwable ignored) {}
                    PaymentTracker.endWhenConfirmed(inFlight, hash, () -> PaymentTracker.markConfirmed(hash));
                    callback.onSuccess(hash);
                }
                @Override public void onError(String error) {
                    inFlight.end();
                    callback.onError("ERR_NODE_TX", error);
                }
            });
            return;
        }

        // Token transfers — not implemented (see class doc above).
        callback.onError("ERR_TOKEN_UNSUPPORTED",
                ("Sending tokens other than " + com.harvana.app.CoinInfo.ticker() + " isn't supported yet."));
    }

    public static void routeTransaction(String sender, String recipient, BigDecimal amount,
                                         String contractAddress, final RouterCallback callback) {
        routeTransaction(null, sender, recipient, amount, contractAddress, callback);
    }

    // ── Native HARV through node ───────────────────────────────────────────────

    /**
     * Node-integrated HARV transfer pipeline:
     *   1. Fetch current nonce from node
     *   2. Fetch fee estimate from node
     *   3. Sign canonically: from + to + rawAmount + nonce
     *   4. POST signed payload to /api/v1/tx/send
     *   5. Return txHash on success
     */
    private static void routeNativeHarvThroughNode(TransactionRequest req,
                                                    ExecutionCallback callback) {
        prepareNativeTransfer(req, new PrepareCallback() {
            @Override
            public void onPrepared(PreparedTransfer prepared) {
                executeNativeTransfer(prepared, callback);
            }
            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    // ── Split for confirmation-before-send flows ────────────────────────────
    //
    // Fetches nonce (with the pending-count fix) and the node's own real
    // fee estimate, then PAUSES - hands both back to the caller instead of
    // immediately signing. This is what lets a confirmation sheet show the
    // real, node-computed fee and nonce before the user commits, rather
    // than the app silently deciding and submitting on their behalf.
    //
    // executeNativeTransfer() below picks up from exactly where this
    // leaves off - same canonical message, same signing, same submission
    // as the original single-method flow. Splitting the method didn't
    // change what gets signed or submitted, only when.

    public interface PrepareCallback {
        void onPrepared(PreparedTransfer prepared);
        void onError(String error);
    }

    /** Everything needed to review and then sign a native HARV transfer -
     *  the exact nonce and fee the confirmation sheet shows are the exact
     *  values later used to sign, not re-fetched and potentially
     *  different by the time the user taps Accept. */
    public static class PreparedTransfer {
        public final TransactionRequest req;
        public final long nonce;
        public final long estimatedGasRaw;
        public final double feeHarv;
        public final long blockHeight;

        public PreparedTransfer(TransactionRequest req, long nonce, long estimatedGasRaw, double feeHarv, long blockHeight) {
            this.req = req;
            this.nonce = nonce;
            this.estimatedGasRaw = estimatedGasRaw;
            this.feeHarv = feeHarv;
            this.blockHeight = blockHeight;
        }
    }

    public static void prepareNativeTransfer(TransactionRequest req, PrepareCallback callback) {
        if (req.context == null) {
            callback.onError("Context required for local signing");
            return;
        }

        String senderAddress = req.from;
        long   rawAmount     = HarvanaNodeClient.toRawUnits(req.amount);

        // Step 1: current confirmed nonce + blockHeight, in one call
        WalletNodeBridge.fetchLiveBalance(req.context, senderAddress, new WalletNodeBridge.BalanceCallback() {
            @Override
            public void onSuccess(double balanceHarv, long confirmedNonce, long blockHeight) {
                // Same pending-count fix as the original single-method flow:
                // the node's real validation is confirmedNonce + 1 +
                // pendingCount, not just confirmedNonce + 1.
                HarvanaNodeClient.getPending(senderAddress, new HarvanaNodeClient.JsonCallback() {
                    @Override
                    public void onSuccess(JSONObject pendingJson) {
                        long pendingOutCount = 0;
                        org.json.JSONArray pending = pendingJson.optJSONArray("pending");
                        if (pending != null) {
                            for (int i = 0; i < pending.length(); i++) {
                                org.json.JSONObject tx = pending.optJSONObject(i);
                                if (tx == null) continue;
                                String direction = tx.optString("direction", "");
                                if ("OUT".equals(direction) || "SELF".equals(direction)) {
                                    pendingOutCount++;
                                }
                            }
                        }
                        _continueFetchingFee(req, callback, senderAddress, rawAmount,
                            confirmedNonce + 1 + pendingOutCount, blockHeight);
                    }

                    @Override
                    public void onError(String error) {
                        Log.w(TAG, "Pending lookup failed, nonce may collide: " + error);
                        _continueFetchingFee(req, callback, senderAddress, rawAmount,
                            confirmedNonce + 1, blockHeight);
                    }
                });
            }

            @Override
            public void onError(String error) {
                callback.onError("Couldn't reach the network: " + error);
            }
        });
    }

    private static void _continueFetchingFee(TransactionRequest req, PrepareCallback callback,
                                              String senderAddress, long rawAmount, long nonce, long blockHeight) {
        // The real, authoritative fee estimate - from the node, not
        // computed client-side. The node is the one that actually
        // validates and processes the transaction, so it owns the real
        // fee number; the app displays what the node says, it doesn't
        // decide independently.
        HarvanaNodeClient.getFee(senderAddress, req.to, rawAmount, new HarvanaNodeClient.JsonCallback() {
            @Override
            public void onSuccess(JSONObject feeJson) {
                long estimatedGas = feeJson.optLong("estimatedGas", 1000L);
                double feeHarv = HarvanaNodeClient.toHarvDisplay(estimatedGas);
                callback.onPrepared(new PreparedTransfer(req, nonce, estimatedGas, feeHarv, blockHeight));
            }

            @Override
            public void onError(String error) {
                callback.onError("Fee estimate failed: " + error);
            }
        });
    }

    /** Signs and submits using the EXACT nonce and gas from prepareNativeTransfer()
     *  - not re-fetched here. Same canonical message construction and
     *  signing steps as the original single-method flow, unchanged. */
    public static void executeNativeTransfer(PreparedTransfer prepared, ExecutionCallback callback) {
        TransactionRequest req = prepared.req;
        String senderAddress = req.from;
        long rawAmount = HarvanaNodeClient.toRawUnits(req.amount);
        long nonce = prepared.nonce;
        long estimatedGas = prepared.estimatedGasRaw;

        try {
            String seedB64 = WalletManager.loadPrivateKey(req.context, senderAddress);
            if (seedB64 == null) {
                callback.onError("No local private key for " + senderAddress);
                return;
            }
            byte[] seed = Base64.decode(seedB64, Base64.NO_WRAP);

            // Canonical: from + to + rawAmount + nonce + memo (matches the
            // server's TransactionPayload.getCanonicalMessage() exactly)
            String canonical = HarvanaNodeClient.buildCanonicalMessage(
                    senderAddress, req.to, rawAmount, nonce, req.memo);
            byte[] sig = Ed25519.sign(seed, canonical.getBytes(StandardCharsets.UTF_8));
            String signatureB64 = Base64.encodeToString(sig, Base64.NO_WRAP);

            String publicKeyB64 = WalletManager.getPublicKey(req.context, senderAddress);
            if (publicKeyB64 == null) {
                callback.onError("No local public key for " + senderAddress);
                return;
            }

            HarvanaNodeClient.submitSignedTx(
                    senderAddress, req.to, rawAmount, estimatedGas,
                    nonce, signatureB64, publicKeyB64, req.memo,
                    new HarvanaNodeClient.JsonCallback() {
                        @Override
                        public void onSuccess(JSONObject result) {
                            try {
                                String txHash = result.optString("txHash", "");
                                String status = result.optString("status", "PENDING");
                                Log.i(TAG, "Tx accepted by node: " + txHash + " status=" + status);
                                callback.onSuccess(new ExecutionResult(txHash, status));
                            } catch (Exception e) {
                                callback.onError("Failed to parse node response: " + e.getMessage());
                            }
                        }

                        @Override
                        public void onError(String error) {
                            Log.e(TAG, "Node rejected tx: " + error);
                            callback.onError("Node error: " + error);
                        }
                    });

        } catch (Exception e) {
            Log.e(TAG, "Signing failed: " + e.getMessage(), e);
            callback.onError("Signing failed: " + e.getMessage());
        }
    }

    // ── Token transfer ───────────────────────────────────────────────────────
    // executeTransaction()/routeSignedToken() (Firestore-simulated token
    // path) removed - see class doc above.

    public interface ExecutionCallback {
        void onSuccess(ExecutionResult result);
        void onError(String error);
    }
}
