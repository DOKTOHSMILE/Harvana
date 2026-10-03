package com.harvana.app.ws;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

/**
 * WebSocket client for the HARV node wallet channel — live push updates so
 * balance/history don't wait for the next manual refresh.
 *
 * Connect: wss://harvannaprotocol.com/ws/wallet?address=HV0x<40hex>  +  Authorization: Bearer <node auth token>
 * (the node closes the socket if a token appears in the query string)
 * Requires a valid NodeAuthManager token — same one used by every REST
 * call in HarvanaNodeClient, fetched fresh via getValidToken() before the
 * socket opens (and again on every reconnect attempt).
 *
 * Message flow:
 *   Server → App: BALANCE_UPDATE  (balance/nonce changed for this address)
 *   Server → App: TX_CONFIRMED    (a transaction involving this address confirmed)
 */
public class WalletSocketClient {

    private static final String WS_BASE = "wss://harvannaprotocol.com";
    private static final String TAG     = "WalletSocket";

    // Reconnect: exponential backoff 1s → 2s → 4s → … → 60s max
    private static final int RECONNECT_BASE_MS = 1_000;
    private static final int RECONNECT_MAX_MS  = 60_000;

    // ── Listener interface ────────────────────────────────────────────────

    public interface WalletSocketListener {
        /**
         * @param rawBalance  raw units (long) — same format as the REST endpoint,
         *                    convert with HarvanaNodeClient.toHarvDisplay()
         */
        void onBalanceUpdate(long rawBalance, long nonce, long blockHeight);

        void onTxConfirmed(String txHash, long blockHeight, int confirmations);

        void onConnected();
        void onDisconnected(String reason);
        void onError(String error);
    }

    // ── State ─────────────────────────────────────────────────────────────

    private final String                 walletAddress;
    private final WalletSocketListener   listener;
    private final Handler                mainHandler = new Handler(Looper.getMainLooper());

    private OkHttpClient     httpClient;
    private WebSocket        webSocket;
    private volatile boolean connected             = false;
    private volatile boolean intentionallyStopped  = false;
    private int              reconnectDelay        = RECONNECT_BASE_MS;

    public WalletSocketClient(String walletAddress, WalletSocketListener listener) {
        this.walletAddress = walletAddress;
        this.listener       = listener;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────

    public void connect() {
        intentionallyStopped = false;

        com.harvana.app.sync.NodeAuthManager.getValidToken(new com.harvana.app.sync.NodeAuthManager.TokenCallback() {
            @Override
            public void onToken(String token) {
                _openSocket(token);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Cannot open wallet socket — node auth failed: " + error);
                mainHandler.post(() -> {
                    if (listener != null) listener.onError("Node auth failed: " + error);
                });
                if (!intentionallyStopped) scheduleReconnect();
            }
        });
    }

    private void _openSocket(String token) {
        if (httpClient == null) {
            httpClient = new OkHttpClient.Builder()
                    .readTimeout(0, TimeUnit.MILLISECONDS)   // required for WebSocket
                    .pingInterval(20, TimeUnit.SECONDS)      // protocol-level ping every 20s
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .writeTimeout(15, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true)
                    .build();
        }

        String url = WS_BASE + "/ws/wallet?address=" + walletAddress;
        Log.i(TAG, "Connecting to: " + WS_BASE + "/ws/wallet?address=" + walletAddress);

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + token)
                .build();

        webSocket = httpClient.newWebSocket(request,
                new WebSocketListener() {

            @Override
            public void onOpen(WebSocket ws, Response response) {
                connected = true;
                reconnectDelay = RECONNECT_BASE_MS;
                Log.i(TAG, "Wallet WS connected | address=" + walletAddress);
                mainHandler.post(() -> {
                    if (listener != null) listener.onConnected();
                });
            }

            @Override
            public void onMessage(WebSocket ws, String text) {
                handleMessage(text);
            }

            @Override
            public void onClosing(WebSocket ws, int code, String reason) {
                connected = false;
                ws.close(1000, null);
            }

            @Override
            public void onClosed(WebSocket ws, int code, String reason) {
                connected = false;
                Log.w(TAG, "WS closed: code=" + code + " reason=" + reason);
                if (code == 401 || code == 403 || code == 1008 || code == 4401 || code == 4001) {
                    com.harvana.app.sync.NodeAuthManager.invalidateCachedToken();
                }
                mainHandler.post(() -> {
                    if (listener != null) listener.onDisconnected(reason);
                });
                if (!intentionallyStopped) scheduleReconnect();
            }

            @Override
            public void onFailure(WebSocket ws, Throwable t, Response response) {
                connected = false;
                Log.e(TAG, "WS failure: " + t.getMessage());
                mainHandler.post(() -> {
                    if (listener != null) {
                        listener.onError(t.getMessage() != null ? t.getMessage() : "WS failure");
                    }
                });
                if (!intentionallyStopped) scheduleReconnect();
            }
        });
    }

    /** Closes the socket only — httpClient is kept alive for reconnects. */
    public void disconnect() {
        intentionallyStopped = true;
        if (webSocket != null) {
            webSocket.close(1000, "Client disconnect");
            webSocket = null;
        }
        connected = false;
    }

    /** Full teardown, including the OkHttpClient's thread/connection pool. */
    public void shutdown() {
        disconnect();
        if (httpClient != null) {
            httpClient.dispatcher().executorService().shutdown();
            httpClient.connectionPool().evictAll();
            httpClient = null;
        }
    }

    // ── Incoming messages ─────────────────────────────────────────────────

    private void handleMessage(String text) {
        try {
            JSONObject root    = new JSONObject(text);
            String     type    = root.optString("type");
            JSONObject payload = root.optJSONObject("payload");
            if (payload == null) return;

            switch (type) {

                case "BALANCE_UPDATE": {
                    long rawBalance  = payload.optLong("balance", 0L);
                    long nonce       = payload.optLong("nonce", 0L);
                    long blockHeight = payload.optLong("blockHeight", -1L);
                    Log.i(TAG, "BALANCE_UPDATE: raw=" + rawBalance + " nonce=" + nonce);
                    mainHandler.post(() -> {
                        if (listener != null) listener.onBalanceUpdate(rawBalance, nonce, blockHeight);
                    });
                    break;
                }

                case "TX_CONFIRMED": {
                    String txHash        = payload.optString("txHash", "");
                    long   blockHeight   = payload.optLong("blockHeight", -1L);
                    int    confirmations = payload.optInt("confirmations", 1);
                    Log.i(TAG, "TX_CONFIRMED: " + txHash + " conf=" + confirmations);
                    mainHandler.post(() -> {
                        if (listener != null) listener.onTxConfirmed(txHash, blockHeight, confirmations);
                    });
                    break;
                }

                default:
                    Log.w(TAG, "Unknown WS message type: " + type);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse message: " + e.getMessage());
        }
    }

    // ── Reconnect ─────────────────────────────────────────────────────────

    private void scheduleReconnect() {
        int delay = reconnectDelay;
        reconnectDelay = Math.min(reconnectDelay * 2, RECONNECT_MAX_MS);
        Log.i(TAG, "Reconnecting in " + delay + "ms...");
        mainHandler.postDelayed(this::connect, delay);
    }

    // ── Accessors ─────────────────────────────────────────────────────────

    public boolean isConnected() {
        return connected;
    }
}