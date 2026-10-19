package com.harvana.app.walletconnect;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

/**
 * The WalletConnect relay (wss://relay.walletconnect.org): one WebSocket, JSON-RPC irn_subscribe / irn_publish, and
 * irn_subscription deliveries acknowledged as they arrive. Topics are subscribed again after every reconnect; messages
 * sent while the socket is down wait in a queue.
 */
final class WcRelay extends WebSocketListener {

    interface Listener {
        void onMessage(String topic, String message);
        void onState(boolean connected);
    }

    static final String URL = "wss://relay.walletconnect.org";

    private final OkHttpClient http = new OkHttpClient.Builder().pingInterval(25, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS).build();
    private final byte[] clientSeed;
    private final String projectId, ua;
    private final Listener listener;
    private final Set<String> topics = new LinkedHashSet<>();
    private final List<String> queue = new ArrayList<>();
    private WebSocket socket;
    private boolean open, wanted;
    private int retries;

    WcRelay(byte[] clientSeed, String projectId, String ua, Listener listener) {
        this.clientSeed = clientSeed;
        this.projectId = projectId;
        this.ua = ua;
        this.listener = listener;
    }

    synchronized void connect() {
        wanted = true;
        if (socket != null) return;
        String url = URL + "/?auth=" + WcCrypto.relayJwt(clientSeed, URL) + "&projectId=" + projectId + "&ua=" + ua;
        socket = http.newWebSocket(new Request.Builder().url(url).build(), this);
    }

    synchronized void close() {
        wanted = false;
        if (socket != null) socket.close(1000, null);
        socket = null;
        open = false;
    }

    synchronized boolean isOpen() { return open; }

    synchronized void subscribe(String topic) {
        topics.add(topic);
        send(rpc("irn_subscribe", new JSONObjectBuilder().put("topic", topic).get()));
    }

    synchronized void unsubscribe(String topic) { topics.remove(topic); }

    synchronized void publish(String topic, String message, int ttl, int tag) {
        send(rpc("irn_publish", new JSONObjectBuilder().put("topic", topic).put("message", message)
                .put("ttl", ttl).put("tag", tag).put("prompt", false).get()));
    }

    static long nextId() { return System.currentTimeMillis() * 1000 + (long) (Math.random() * 1000); }

    private static String rpc(String method, JSONObject params) {
        return new JSONObjectBuilder().put("id", nextId()).put("jsonrpc", "2.0").put("method", method).put("params", params).get().toString();
    }

    private void send(String text) {
        if (open && socket != null) socket.send(text);
        else { queue.add(text); connect(); }
    }

    // ── socket ──────────────────────────────────────────────────────────

    @Override public void onOpen(WebSocket ws, Response response) {
        List<String> pending;
        synchronized (this) {
            if (ws != socket) return;
            open = true;
            retries = 0;
            for (String t : topics) ws.send(rpc("irn_subscribe", new JSONObjectBuilder().put("topic", t).get()));
            pending = new ArrayList<>(queue);
            queue.clear();
        }
        for (String p : pending) if (!p.contains("\"irn_subscribe\"")) ws.send(p);
        listener.onState(true);
    }

    @Override public void onMessage(WebSocket ws, String text) {
        try {
            JSONObject j = new JSONObject(text);
            if (!"irn_subscription".equals(j.optString("method"))) return;      // a result of our own call
            ws.send(new JSONObjectBuilder().put("id", j.opt("id")).put("jsonrpc", "2.0").put("result", true).get().toString());
            JSONObject data = j.getJSONObject("params").getJSONObject("data");
            listener.onMessage(data.getString("topic"), data.getString("message"));
        } catch (Exception ignored) { }
    }

    @Override public void onClosed(WebSocket ws, int code, String reason) { dropped(ws); }

    @Override public void onFailure(WebSocket ws, Throwable t, Response response) { dropped(ws); }

    private void dropped(WebSocket ws) {
        boolean again;
        long wait;
        synchronized (this) {
            if (ws != socket) return;
            socket = null;
            open = false;
            again = wanted;
            wait = Math.min(60_000L, 2_000L << Math.min(retries++, 5));
        }
        listener.onState(false);
        if (again) new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(this::connect, wait);
    }

    /** JSONObject.put without the checked exception. */
    static final class JSONObjectBuilder {
        private final JSONObject o = new JSONObject();
        JSONObjectBuilder put(String k, Object v) { try { o.put(k, v); } catch (Exception ignored) { } return this; }
        JSONObject get() { return o; }
    }
}
