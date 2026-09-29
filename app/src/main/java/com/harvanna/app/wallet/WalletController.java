package com.harvanna.app.wallet;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.harvanna.app.sync.HarvannaNodeClient;
import com.harvanna.app.tx.TransactionManager;

import org.json.JSONObject;

import java.util.List;
import java.util.Map;

/**
 * Controller for the wallet home screen.
 *
 * USAGE in Activity:
 *
 *   // onCreate / onResume:
 *   controller = new WalletController(this, walletAddress);
 *   controller.setListener(this); // implement WalletController.Listener
 *   controller.loadAll();         // triggers instant cache + live node fetch
 *
 *   // onPause:
 *   controller.destroy();
 *
 * BALANCE STRATEGY (two-phase):
 *   Phase 1 — instant: loads WalletCache (0ms, no network)
 *             → UI renders immediately with last-known balance
 *   Phase 2 — live: queries node REST /api/v1/wallet/balance/{address} (~200ms)
 *             → UI updates with confirmed on-chain balance
 *             → WalletCache is updated for next open
 *
 * The node is the source of truth for HARV balance.
 * Transaction history: tries the node's real ADDR_TX-indexed endpoint
 * first, falls back to Firestore only if the node genuinely returns none
 * (e.g. history that confirmed before that index existed).
 *
 * INCOMING TRANSACTION NOTIFICATIONS:
 *   A TX_CONFIRMED push from the wallet socket only carries a txHash — no
 *   sender or amount. Firestore's TRANSACTIONS record (written by the
 *   node's FirestoreBridgeService once a block seals) is the only place
 *   both live, so onTxConfirmed() does one small lookup there before
 *   deciding whether to alert — and only alerts for money actually
 *   received, not our own outgoing sends confirming, which also fire
 *   TX_CONFIRMED.
 */
public class WalletController {

    private static final String TAG = "WalletController";

    private final Context context;
    private final String  walletAddress;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private Listener listener;
    private boolean  destroyed = false;

    private com.harvanna.app.ws.WalletSocketClient socketClient;

    // ── Listener ──────────────────────────────────────────────────────────────

    public interface Listener {

        /**
         * Called immediately with last-known balance from cache (may be stale).
         * Show this instantly while the live fetch is in flight.
         *
         * @param balanceHarv     last cached HARV balance
         * @param cacheAgeMs     how old the cached data is (-1 = never cached)
         */
        void onCachedBalance(double balanceHarv, long cacheAgeMs);

        /**
         * Called when the live node confirms the balance.
         * Update the UI with this — it's always authoritative.
         *
         * @param balanceHarv  confirmed HARV balance from node
         * @param nonce       confirmed nonce (use nonce+1 when building a send tx)
         * @param blockHeight chain height at which this balance was confirmed
         */
        void onLiveBalance(double balanceHarv, long nonce, long blockHeight);

        /** Called if the node is unreachable. Show cached balance with staleness label. */
        void onBalanceError(String error);

        /**
         * Called with the merged transaction history.
         * HARV txs come from the node; token txs come from Firestore.
         */
        void onTransactionHistory(List<Map<String, Object>> transactions);

        /** Called if transaction history fails to load. */
        void onHistoryError(String error);
    }

    // ── Constructor ───────────────────────────────────────────────────────────

    public WalletController(Context context, String walletAddress) {
        this.context       = context.getApplicationContext();
        this.walletAddress = walletAddress;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Loads balance + history in one call.
     * Triggers Phase 1 (cache) then Phase 2 (live node) for balance.
     */
    public void loadAll() {
        loadBalance();
        loadTransactionHistory();
        connectLive();
    }

    /**
     * Connects the wallet WebSocket so balance/history update the instant
     * the node pushes a change — no waiting for the next manual refresh.
     * Safe to call multiple times; a live connection is reused.
     */
    private void connectLive() {
        if (destroyed) return;
        if (socketClient != null && socketClient.isConnected()) return;

        socketClient = new com.harvanna.app.ws.WalletSocketClient(walletAddress,
                new com.harvanna.app.ws.WalletSocketClient.WalletSocketListener() {
                    @Override
                    public void onBalanceUpdate(long rawBalance, long nonce, long blockHeight) {
                        if (destroyed) return;
                        double balanceHarv = HarvannaNodeClient.toHarvDisplay(rawBalance);

                        Map<String, Object> toCache = new java.util.HashMap<>();
                        toCache.put("balance_harv", balanceHarv);
                        toCache.put("nonce",       nonce);
                        toCache.put("blockHeight", blockHeight);
                        WalletCache.save(context, walletAddress, toCache);

                        if (listener != null) listener.onLiveBalance(balanceHarv, nonce, blockHeight);
                    }

                    @Override
                    public void onTxConfirmed(String txHash, long blockHeight, int confirmations) {
                        if (destroyed) return;
                        Log.i(TAG, "Live tx confirmed: " + txHash + " — refreshing history");
                        loadTransactionHistory();
                        checkIfIncomingAndNotify(txHash);
                    }

                    @Override
                    public void onConnected() {
                        Log.i(TAG, "Wallet live updates connected");
                    }

                    @Override
                    public void onDisconnected(String reason) {
                        Log.w(TAG, "Wallet live updates disconnected: " + reason + " — reconnecting…");
                    }

                    @Override
                    public void onError(String error) {
                        Log.w(TAG, "Wallet socket error: " + error);
                    }
                });
        socketClient.connect();
    }

    /**
     * Fetches the full confirmed transaction record from Firestore (the
     * socket push alone doesn't carry enough to build a real message), and
     * — only if it's genuinely incoming, not our own send confirming —
     * shows an in-app notification and broadcasts ACTION_WALLET_RECEIVED
     * for any other open screen already listening for it.
     */
    private void checkIfIncomingAndNotify(String txHash) {
        TransactionManager.getTransaction(txHash,
                new com.harvanna.app.sync.FirestoreSyncManager.SyncCallback() {
                    @Override
                    public void onSuccess(String json) {
                        if (destroyed) return;

                        Map<String, Object> tx =
                                com.harvanna.app.sync.FirestoreSyncManager.parseFirestoreDocument(json);
                        if (tx == null) return;

                        String from = tx.get("from") != null ? tx.get("from").toString() : "";
                        String to   = tx.get("to")   != null ? tx.get("to").toString()   : "";

                        boolean isIncoming = to.equalsIgnoreCase(walletAddress)
                                && !from.equalsIgnoreCase(walletAddress);
                        if (!isIncoming) return;

                        long   rawAmount = tx.get("amount") instanceof Number
                                ? ((Number) tx.get("amount")).longValue() : 0L;
                        double amountHarv = HarvannaNodeClient.toHarvDisplay(rawAmount);
                        String symbol    = tx.get("tokenSymbol") != null
                                ? tx.get("tokenSymbol").toString() : com.harvanna.app.CoinInfo.ticker();

                        // UI work (notification banner) must happen on the main
                        // thread — this callback's thread isn't guaranteed, so
                        // marshal explicitly rather than assume.
                        mainHandler.post(() -> {
                            if (destroyed) return;
                            if (!com.harvanna.app.utils.IncomingTxWatcher.markNotifiedIfNew(txHash)) return;

                            Listener currentListener = listener; // avoid a null race
                            if (currentListener instanceof android.app.Activity) {
                                String title   = (com.harvanna.app.CoinInfo.ticker() + " Received");
                                String message = com.harvanna.app.wallet.BalanceFormatter
                                        .short4(amountHarv) + " received";
                                com.harvanna.app.utils.InAppNotificationHelper.showTransactionNotification(
                                        (android.app.Activity) currentListener, title, message, from);
                            }

                            // Match the existing broadcast pattern other screens
                            // already listen for (CryptoWalletActivity, CoinDetailActivity
                            // all have a globalTxReceiver for this action).
                            android.content.Intent received =
                                    new android.content.Intent("com.harvanna.app.ACTION_WALLET_RECEIVED");
                            received.putExtra("EXTRA_AMOUNT", amountHarv);
                            received.putExtra("EXTRA_SENDER", from);
                            received.putExtra("EXTRA_SYMBOL", symbol);
                            androidx.localbroadcastmanager.content.LocalBroadcastManager
                                    .getInstance(context).sendBroadcast(received);
                        });
                    }

                    @Override
                    public void onError(String error) {
                        Log.w(TAG, "Could not fetch confirmed tx details for notification: " + error);
                    }
                });
    }

    /**
     * Two-phase balance load:
     *   1. Emit cached balance instantly (Phase 1)
     *   2. Fetch live from node and emit again (Phase 2)
     */
    public void loadBalance() {
        if (destroyed || listener == null) return;

        // Phase 1 — instant cache
        Map<String, Object> cached = WalletCache.load(context, walletAddress);
        if (cached != null) {
            double cachedBalance = toDouble(cached.get("balance_harv"));
            long   cacheAgeMs   = WalletCache.ageMs(context, walletAddress);
            listener.onCachedBalance(cachedBalance, cacheAgeMs);
        } else {
            // No cache — show 0 while loading
            listener.onCachedBalance(0.0, -1L);
        }

        // Phase 2 — live node fetch
        WalletNodeBridge.fetchLiveBalance(context, walletAddress,
                new WalletNodeBridge.BalanceCallback() {
                    @Override
                    public void onSuccess(double balanceHarv, long nonce, long blockHeight) {
                        if (destroyed) return;

                        // Persist to WalletCache so next open is instant
                        java.util.Map<String, Object> toCache = new java.util.HashMap<>();
                        toCache.put("balance_harv", balanceHarv);
                        toCache.put("nonce",       nonce);
                        toCache.put("blockHeight", blockHeight);
                        WalletCache.save(context, walletAddress, toCache);

                        if (listener != null) {
                            listener.onLiveBalance(balanceHarv, nonce, blockHeight);
                        }
                    }

                    @Override
                    public void onError(String error) {
                        if (destroyed) return;
                        Log.w(TAG, "Node balance fetch failed: " + error);
                        // Don't wipe the cached display — just notify the error
                        if (listener != null) listener.onBalanceError(error);
                    }
                });
    }

    /**
     * Loads HARV transaction history — tries the node's real, ADDR_TX-indexed
     * endpoint first. If it returns actual records, uses them directly. Only
     * falls back to Firestore when the node genuinely has nothing (e.g. the
     * wallet's history entirely predates the ADDR_TX index existing).
     */
    public void loadTransactionHistory() {
        if (destroyed || listener == null) return;

        HarvannaNodeClient.getHistory(walletAddress, new HarvannaNodeClient.JsonCallback() {
            @Override
            public void onSuccess(JSONObject json) {
                if (destroyed || listener == null) return;

                List<Map<String, Object>> txList = new java.util.ArrayList<>();
                try {
                    org.json.JSONArray transactions = json.optJSONArray("transactions");
                    if (transactions != null) {
                        for (int i = 0; i < transactions.length(); i++) {
                            org.json.JSONObject tx = transactions.getJSONObject(i);
                            Map<String, Object> item = new java.util.HashMap<>();

                            item.put("senderAddress", tx.optString("from", ""));
                            item.put("recipientAddress", tx.optString("to", ""));
                            item.put("value", HarvannaNodeClient.toHarvDisplay(tx.optLong("amount", 0L)));
                            item.put("harvFee", HarvannaNodeClient.toHarvDisplay(tx.optLong("gas", 0L)));
                            item.put("tokenSymbol", com.harvanna.app.CoinInfo.ticker());
                            item.put("txHash", tx.optString("txHash", ""));
                            item.put("status", tx.optString("status", "confirmed"));
                            item.put("blockNumber", tx.optLong("blockHeight", 0L));
                            item.put("nonce", tx.optLong("nonce", 0L));

                            long ts = tx.optLong("timestamp", System.currentTimeMillis());
                            if (ts > 0 && ts < 1_000_000_000_000L) ts *= 1000L;
                            item.put("timestamp", ts);

                            txList.add(item);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to parse node history: " + e.getMessage());
                }

                if (!txList.isEmpty()) {
                    listener.onTransactionHistory(txList);
                } else {
                    // Node genuinely has nothing for this address yet —
                    // fall back to Firestore (covers history from before
                    // the ADDR_TX index existed, and token transfers,
                    // which the node has no concept of at all).
                    loadHistoryFromFirestore();
                }
            }

            @Override
            public void onError(String error) {
                if (destroyed) return;
                Log.w(TAG, "Node history failed, falling back to Firestore: " + error);
                loadHistoryFromFirestore();
            }
        });
    }

    /** Forces a fresh balance fetch from the node (call after a tx is submitted). */
    public void refresh() {
        loadBalance();
    }

    /** Release all references. Call from Activity.onPause() or onDestroy(). */
    public void destroy() {
        destroyed = true;
        listener  = null;
        if (socketClient != null) {
            socketClient.shutdown();
            socketClient = null;
        }
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private void loadHistoryFromFirestore() {
        TransactionManager.getTransactions(walletAddress,
                new TransactionManager.TransactionListCallback() {
                    @Override
                    public void onSuccess(List<Map<String, Object>> txs) {
                        if (destroyed || listener == null) return;
                        listener.onTransactionHistory(txs);
                    }

                    @Override
                    public void onError(String error) {
                        if (destroyed || listener == null) return;
                        listener.onHistoryError(error);
                    }
                });
    }

    private static double toDouble(Object v) {
        if (v instanceof Number) return ((Number) v).doubleValue();
        if (v instanceof String) {
            try { return Double.parseDouble((String) v); } catch (NumberFormatException ignored) {}
        }
        return 0.0;
    }
}