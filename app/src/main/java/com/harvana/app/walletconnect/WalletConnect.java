package com.harvana.app.walletconnect;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import com.harvana.app.walletconnect.WcRelay.JSONObjectBuilder;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The wallet side of WalletConnect v2 (Sign API): pairs from a "wc:" link, answers session proposals and requests,
 * keeps sessions across restarts, and disconnects. Nothing is approved or signed here: proposals and requests go to
 * {@link WcPrompts}, which asks the person (and the PIN or fingerprint for every signature).
 */
public final class WalletConnect implements WcRelay.Listener {

    /** The Reown (WalletConnect) Cloud project id. Public, not a secret: set it here or in WalletConnect → Project ID. */
    public static final String PROJECT_ID = "";

    static final String PREFS = "walletconnect";
    static final long SESSION_SECONDS = 7L * 24 * 60 * 60;

    // tags of the Sign API
    static final int PROPOSE_RES = 1101, PROPOSE_REJECT = 1120, SETTLE_REQ = 1102, REQUEST_RES = 1109,
            DELETE_REQ = 1112, DELETE_RES = 1113, PING_RES = 1115, EXTEND_RES = 1107, UPDATE_RES = 1105,
            EVENT_RES = 1111, PAIR_DELETE_RES = 1001, PAIR_PING_RES = 1003;

    /** One proposal waiting for the person. */
    public static final class Proposal {
        public long id;
        public String pairingTopic;
        public JSONObject params;
        public JSONObject metadata() { JSONObject p = params.optJSONObject("proposer"); return p == null ? new JSONObject() : p.optJSONObject("metadata") == null ? new JSONObject() : p.optJSONObject("metadata"); }
    }

    /** One request waiting for the person. */
    public static final class Request {
        public long id;
        public String topic, method, chainId;
        public JSONArray arrayParams;
        public JSONObject objectParams;
        public Session session;
    }

    /** One connected dApp. */
    public static final class Session {
        public String topic, pairingTopic;
        public byte[] symKey;
        public JSONObject peer = new JSONObject(), namespaces = new JSONObject();
        public long expiry;
        public String name() { return peer.optString("name", "dApp"); }
        public String url() { return peer.optString("url", ""); }
        public String icon() { JSONArray i = peer.optJSONArray("icons"); return i == null || i.length() == 0 ? "" : i.optString(0); }
        JSONObject json() {
            return new JSONObjectBuilder().put("topic", topic).put("pairingTopic", pairingTopic).put("symKey", WcCrypto.hex(symKey))
                    .put("peer", peer).put("namespaces", namespaces).put("expiry", expiry).get();
        }
        static Session of(JSONObject o) {
            Session s = new Session();
            s.topic = o.optString("topic"); s.pairingTopic = o.optString("pairingTopic");
            s.symKey = WcCrypto.unhex(o.optString("symKey"));
            s.peer = o.optJSONObject("peer") == null ? new JSONObject() : o.optJSONObject("peer");
            s.namespaces = o.optJSONObject("namespaces") == null ? new JSONObject() : o.optJSONObject("namespaces");
            s.expiry = o.optLong("expiry");
            return s;
        }
    }

    private static WalletConnect instance;
    private final Context app;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Map<String, byte[]> pairings = new LinkedHashMap<>();
    private final Map<String, Session> sessions = new LinkedHashMap<>();
    private final List<Runnable> watchers = new ArrayList<>();
    private WcRelay relay;
    private boolean connected;

    private WalletConnect(Context c) {
        app = c.getApplicationContext();
        load();
    }

    public static synchronized WalletConnect get(Context c) {
        if (instance == null) instance = new WalletConnect(c);
        return instance;
    }

    /** At app start: reconnects when there are sessions, so dApps reach the phone while it is open. */
    public static void install(android.app.Application app) {
        WalletConnect wc = get(app);
        if (!wc.sessions.isEmpty() && wc.ready()) wc.start();
        app.registerActivityLifecycleCallbacks(new android.app.Application.ActivityLifecycleCallbacks() {
            @Override public void onActivityResumed(android.app.Activity a) {
                WcPrompts.flush();                                             // a request that arrived with no screen open
                if (!wc.sessions.isEmpty() && wc.ready()) wc.start();
            }
            @Override public void onActivityCreated(android.app.Activity a, android.os.Bundle b) { }
            @Override public void onActivityStarted(android.app.Activity a) { }
            @Override public void onActivityPaused(android.app.Activity a) { }
            @Override public void onActivityStopped(android.app.Activity a) { }
            @Override public void onActivitySaveInstanceState(android.app.Activity a, android.os.Bundle b) { }
            @Override public void onActivityDestroyed(android.app.Activity a) { }
        });
    }

    // ── settings ────────────────────────────────────────────────────────

    private SharedPreferences prefs() { return app.getSharedPreferences(PREFS, Context.MODE_PRIVATE); }

    public String projectId() {
        String own = prefs().getString("projectId", "");
        return own.isEmpty() ? PROJECT_ID : own;
    }

    public void setProjectId(String id) {
        prefs().edit().putString("projectId", id == null ? "" : id.trim()).apply();
        stop();
        if (ready()) start();
    }

    public boolean ready() { return projectId().matches("[0-9a-fA-F]{32}"); }

    public boolean connected() { return connected; }

    private byte[] clientSeed() {
        String hex = prefs().getString("clientSeed", "");
        if (hex.length() == 64) return WcCrypto.unhex(hex);
        byte[] s = WcCrypto.random(32);
        prefs().edit().putString("clientSeed", WcCrypto.hex(s)).apply();
        return s;
    }

    // ── state ───────────────────────────────────────────────────────────

    private synchronized void load() {
        try {
            JSONObject o = new JSONObject(prefs().getString("state", "{}"));
            JSONObject p = o.optJSONObject("pairings");
            for (Iterator<String> it = p == null ? null : p.keys(); it != null && it.hasNext(); ) {
                String t = it.next();
                pairings.put(t, WcCrypto.unhex(p.getString(t)));
            }
            JSONArray s = o.optJSONArray("sessions");
            long now = System.currentTimeMillis() / 1000;
            for (int i = 0; s != null && i < s.length(); i++) {
                Session x = Session.of(s.getJSONObject(i));
                if (x.expiry > now) sessions.put(x.topic, x);
            }
        } catch (Exception ignored) { }
    }

    private synchronized void save() {
        JSONObject p = new JSONObject();
        for (Map.Entry<String, byte[]> e : pairings.entrySet()) try { p.put(e.getKey(), WcCrypto.hex(e.getValue())); } catch (Exception ignored) { }
        JSONArray s = new JSONArray();
        for (Session x : sessions.values()) s.put(x.json());
        prefs().edit().putString("state", new JSONObjectBuilder().put("pairings", p).put("sessions", s).get().toString()).apply();
        main.post(() -> { for (Runnable r : new ArrayList<>(watchers)) r.run(); });
    }

    public synchronized List<Session> sessions() { return new ArrayList<>(sessions.values()); }

    public void watch(Runnable r) { watchers.add(r); }

    public void unwatch(Runnable r) { watchers.remove(r); }

    // ── connection ──────────────────────────────────────────────────────

    public synchronized void start() {
        if (!ready()) return;
        if (relay == null) {
            relay = new WcRelay(clientSeed(), projectId(), Uri.encode("wc-2/java-2.0/android/com.harvana.app"), this);
            for (String t : pairings.keySet()) relay.subscribe(t);
            for (String t : sessions.keySet()) relay.subscribe(t);
        }
        relay.connect();
    }

    public synchronized void stop() {
        if (relay != null) relay.close();
        relay = null;
        connected = false;
    }

    @Override public void onState(boolean on) {
        connected = on;
        main.post(() -> { for (Runnable r : new ArrayList<>(watchers)) r.run(); });
    }

    /** Pairs with a dApp from its "wc:topic@2?relay-protocol=irn&symKey=…" link. Returns an error, or null. */
    public String pair(String uri) {
        if (!ready()) return "Set the WalletConnect project ID first.";
        String u = uri == null ? "" : uri.trim();
        if (u.startsWith("wc://")) u = "wc:" + u.substring(5);
        if (!u.startsWith("wc:") || !u.contains("@2")) return "Not a WalletConnect v2 link.";
        String topic = u.substring(3, u.indexOf('@'));
        Uri q = Uri.parse("wc://x?" + (u.contains("?") ? u.substring(u.indexOf('?') + 1) : ""));
        String key = q.getQueryParameter("symKey");
        if (topic.isEmpty() || key == null || !key.matches("[0-9a-fA-F]{64}")) return "This WalletConnect link is incomplete.";
        synchronized (this) { pairings.put(topic, WcCrypto.unhex(key)); }
        save();
        start();
        synchronized (this) { relay.subscribe(topic); }
        return null;
    }

    // ── incoming ────────────────────────────────────────────────────────

    @Override public void onMessage(String topic, String message) {
        byte[] key;
        Session session;
        synchronized (this) {
            session = sessions.get(topic);
            key = session != null ? session.symKey : pairings.get(topic);
        }
        if (key == null) return;
        JSONObject j;
        try { j = new JSONObject(WcCrypto.open(key, message)); } catch (Exception e) { return; }
        if (!j.has("method")) return;                                          // an answer to our settle / delete
        long id = j.optLong("id");
        JSONObject params = j.optJSONObject("params");
        switch (j.optString("method")) {
            case "wc_sessionPropose": {
                Proposal p = new Proposal();
                p.id = id; p.pairingTopic = topic; p.params = params == null ? new JSONObject() : params;
                main.post(() -> WcPrompts.proposal(app, p));
                return;
            }
            case "wc_sessionRequest": {
                if (session == null || params == null) return;
                Request r = new Request();
                r.id = id; r.topic = topic; r.session = session;
                r.chainId = params.optString("chainId");
                JSONObject req = params.optJSONObject("request");
                if (req == null) return;
                r.method = req.optString("method");
                Object ps = req.opt("params");
                if (ps instanceof JSONArray) r.arrayParams = (JSONArray) ps;
                else if (ps instanceof JSONObject) r.objectParams = (JSONObject) ps;
                main.post(() -> WcPrompts.request(app, r));
                return;
            }
            case "wc_sessionPing": reply(topic, key, id, true, PING_RES); return;
            case "wc_sessionExtend":
                if (session != null) { session.expiry = System.currentTimeMillis() / 1000 + SESSION_SECONDS; save(); }
                reply(topic, key, id, true, EXTEND_RES); return;
            case "wc_sessionUpdate": reply(topic, key, id, true, UPDATE_RES); return;
            case "wc_sessionEvent": reply(topic, key, id, true, EVENT_RES); return;
            case "wc_sessionDelete":
                reply(topic, key, id, true, DELETE_RES);
                drop(topic);
                return;
            case "wc_pairingPing": reply(topic, key, id, true, PAIR_PING_RES); return;
            case "wc_pairingDelete":
                reply(topic, key, id, true, PAIR_DELETE_RES);
                synchronized (this) { pairings.remove(topic); if (relay != null) relay.unsubscribe(topic); }
                save();
                return;
            default:
                // wc_sessionAuthenticate and other optional methods: the dApp's proposal follows on the same pairing
        }
    }

    private void reply(String topic, byte[] key, long id, Object result, int tag) {
        send(topic, key, new JSONObjectBuilder().put("id", id).put("jsonrpc", "2.0").put("result", result).get(), 300, tag);
    }

    private void send(String topic, byte[] key, JSONObject body, int ttl, int tag) {
        try {
            String msg = WcCrypto.seal(key, body.toString());
            synchronized (this) { if (relay != null) relay.publish(topic, msg, ttl, tag); }
        } catch (Exception ignored) { }
    }

    // ── answers the person gave ─────────────────────────────────────────

    /** Approves with these namespaces ({"eip155": {chains, accounts, methods, events}, …}). */
    public void approve(Proposal p, JSONObject namespaces) throws Exception {
        byte[] pairKey;
        synchronized (this) { pairKey = pairings.get(p.pairingTopic); }
        if (pairKey == null) throw new IllegalStateException("The pairing expired. Scan a new code.");
        JSONObject proposer = p.params.getJSONObject("proposer");
        byte[][] self = WcCrypto.x25519Pair();
        byte[] sym = WcCrypto.sharedKey(self[0], WcCrypto.unhex(proposer.getString("publicKey")));
        java.util.Arrays.fill(self[0], (byte) 0);
        Session s = new Session();
        s.topic = WcCrypto.topicOf(sym);
        s.pairingTopic = p.pairingTopic;
        s.symKey = sym;
        s.peer = p.metadata();
        s.namespaces = namespaces;
        s.expiry = System.currentTimeMillis() / 1000 + SESSION_SECONDS;
        synchronized (this) { sessions.put(s.topic, s); if (relay != null) relay.subscribe(s.topic); }
        save();
        JSONObject relayProto = new JSONObjectBuilder().put("protocol", "irn").get();
        JSONObject settle = new JSONObjectBuilder()
                .put("relay", relayProto)
                .put("namespaces", namespaces)
                .put("requiredNamespaces", p.params.optJSONObject("requiredNamespaces") == null ? new JSONObject() : p.params.optJSONObject("requiredNamespaces"))
                .put("optionalNamespaces", p.params.optJSONObject("optionalNamespaces") == null ? new JSONObject() : p.params.optJSONObject("optionalNamespaces"))
                .put("pairingTopic", p.pairingTopic)
                .put("controller", new JSONObjectBuilder().put("publicKey", WcCrypto.hex(self[1])).put("metadata", metadata()).get())
                .put("expiry", s.expiry).get();
        send(s.topic, sym, new JSONObjectBuilder().put("id", WcRelay.nextId()).put("jsonrpc", "2.0").put("method", "wc_sessionSettle").put("params", settle).get(), 300, SETTLE_REQ);
        send(p.pairingTopic, pairKey, new JSONObjectBuilder().put("id", p.id).put("jsonrpc", "2.0")
                .put("result", new JSONObjectBuilder().put("relay", relayProto).put("responderPublicKey", WcCrypto.hex(self[1])).get()).get(), 300, PROPOSE_RES);
    }

    public void reject(Proposal p, int code, String message) {
        byte[] pairKey;
        synchronized (this) { pairKey = pairings.get(p.pairingTopic); }
        if (pairKey == null) return;
        send(p.pairingTopic, pairKey, new JSONObjectBuilder().put("id", p.id).put("jsonrpc", "2.0")
                .put("error", new JSONObjectBuilder().put("code", code).put("message", message).get()).get(), 300, PROPOSE_REJECT);
    }

    public void result(Request r, Object result) {
        send(r.topic, r.session.symKey, new JSONObjectBuilder().put("id", r.id).put("jsonrpc", "2.0").put("result", result == null ? JSONObject.NULL : result).get(), 300, REQUEST_RES);
    }

    public void error(Request r, int code, String message) {
        send(r.topic, r.session.symKey, new JSONObjectBuilder().put("id", r.id).put("jsonrpc", "2.0")
                .put("error", new JSONObjectBuilder().put("code", code).put("message", message).get()).get(), 300, REQUEST_RES);
    }

    /** Ends a session on both sides. */
    public void disconnect(Session s) {
        send(s.topic, s.symKey, new JSONObjectBuilder().put("id", WcRelay.nextId()).put("jsonrpc", "2.0").put("method", "wc_sessionDelete")
                .put("params", new JSONObjectBuilder().put("code", 6000).put("message", "User disconnected.").get()).get(), 86_400, DELETE_REQ);
        drop(s.topic);
    }

    private void drop(String topic) {
        synchronized (this) { sessions.remove(topic); if (relay != null) relay.unsubscribe(topic); }
        save();
    }

    static JSONObject metadata() {
        return new JSONObjectBuilder().put("name", "Harvana").put("description", "Harvana wallet")
                .put("url", "https://harvannaprotocol.com").put("icons", new JSONArray().put("https://harvannaprotocol.com/favicon.png")).get();
    }
}
