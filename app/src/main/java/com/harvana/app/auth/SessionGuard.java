package com.harvana.app.auth;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * One phone per account. When someone signs in, this phone writes its own session id to USERS/{uid}/private/session. Every phone signed
 * in to that account watches the document; one whose id is no longer the stored one signs out. The newest sign-in wins. The wallet and its
 * keys stay on the old phone (only the Firebase sign-in ends), so nothing is lost. Uses the existing owner-only rule for USERS/{uid}/private.
 */
public final class SessionGuard {
    private static final String PREFS = "session_guard";
    private static Application app;
    private static ListenerRegistration watch;
    private static String watchedUid = "";

    private SessionGuard() {}

    public static void install(Application a) {
        app = a;
        FirebaseAuth.getInstance().addAuthStateListener(auth -> {
            FirebaseUser u = auth.getCurrentUser();
            if (u == null) { stop(); prefs().edit().clear().apply(); return; }
            if (u.getUid().equals(watchedUid)) return;
            start(u.getUid());
        });
    }

    private static SharedPreferences prefs() { return app.getSharedPreferences(PREFS, Context.MODE_PRIVATE); }

    private static void stop() {
        if (watch != null) { watch.remove(); watch = null; }
        watchedUid = "";
    }

    private static DocumentReference doc(String uid) {
        return FirebaseFirestore.getInstance().collection("USERS").document(uid).collection("private").document("session");
    }

    private static DocumentReference devicesDoc(String uid) {
        return FirebaseFirestore.getInstance().collection("USERS").document(uid).collection("private").document("devices");
    }

    /** A stable id for this phone (survives sign-out), so the Devices list shows each phone once. */
    public static String deviceId() {
        SharedPreferences d = app.getSharedPreferences("device_identity", Context.MODE_PRIVATE);
        String id = d.getString("id", "");
        if (id.isEmpty()) { id = UUID.randomUUID().toString().substring(0, 12); d.edit().putString("id", id).apply(); }
        return id;
    }

    public static String deviceName() { return (Build.MANUFACTURER + " " + Build.MODEL).trim(); }

    /**
     * Keeps the account's phone list (USERS/{uid}/private/devices, one entry per phone) up to date: this phone is marked signed in and
     * last seen now; on a fresh sign-in every other phone still marked signed in is marked signed out now.
     */
    private static void registerDevice(final String uid, final boolean fresh) {
        final String me = deviceId();
        final long now = System.currentTimeMillis();
        devicesDoc(uid).get().addOnCompleteListener(t -> {
            Map<String, Object> devices = new HashMap<>();
            if (t.isSuccessful() && t.getResult() != null && t.getResult().get("devices") instanceof Map) {
                for (Map.Entry<?, ?> en : ((Map<?, ?>) t.getResult().get("devices")).entrySet()) {
                    if (en.getValue() instanceof Map) {
                        Map<String, Object> copy = new HashMap<>();
                        for (Map.Entry<?, ?> f : ((Map<?, ?>) en.getValue()).entrySet()) copy.put(String.valueOf(f.getKey()), f.getValue());
                        devices.put(String.valueOf(en.getKey()), copy);
                    }
                }
            } else if (!t.isSuccessful()) return;
            if (fresh) for (Map.Entry<String, Object> en : devices.entrySet()) {
                @SuppressWarnings("unchecked") Map<String, Object> d = (Map<String, Object>) en.getValue();
                if (!en.getKey().equals(me) && d.get("signedOutAt") == null) d.put("signedOutAt", now);
            }
            @SuppressWarnings("unchecked") Map<String, Object> mine = devices.get(me) instanceof Map ? (Map<String, Object>) devices.get(me) : new HashMap<String, Object>();
            mine.put("name", deviceName());
            mine.put("lastAt", now);
            if (fresh || mine.get("signedInAt") == null) mine.put("signedInAt", now);
            if (fresh) mine.remove("signedOutAt");
            devices.put(me, mine);
            while (devices.size() > 12) {                          // keep the newest twelve
                String oldest = null; long old = Long.MAX_VALUE;
                for (Map.Entry<String, Object> en : devices.entrySet()) {
                    Object l = ((Map<?, ?>) en.getValue()).get("lastAt");
                    long v = l instanceof Number ? ((Number) l).longValue() : 0L;
                    if (!en.getKey().equals(me) && v < old) { old = v; oldest = en.getKey(); }
                }
                if (oldest == null) break;
                devices.remove(oldest);
            }
            Map<String, Object> doc = new HashMap<>();
            doc.put("devices", devices);
            devicesDoc(uid).set(doc);
        });
    }

    private static void start(final String uid) {
        stop();
        watchedUid = uid;
        SharedPreferences p = prefs();
        String mine = p.getString("id_" + uid, "");
        registerDevice(uid, mine.isEmpty());
        if (mine.isEmpty()) {                                        // a fresh sign-in on this phone: take the session
            mine = UUID.randomUUID().toString();
            p.edit().putString("id_" + uid, mine).apply();
            Map<String, Object> m = new HashMap<>();
            m.put("id", mine);
            m.put("device", (Build.MANUFACTURER + " " + Build.MODEL).trim());
            m.put("at", System.currentTimeMillis());
            doc(uid).set(m).addOnFailureListener(e -> p.edit().remove("id_" + uid).apply());   // try again next start
        }
        final String myId = mine;
        watch = doc(uid).addSnapshotListener((snap, err) -> {
            if (err != null || snap == null || !snap.exists() || snap.getMetadata().hasPendingWrites()) return;
            String stored = snap.getString("id");
            if (stored == null || stored.isEmpty() || stored.equals(myId)) return;
            kick(snap.getString("device"));
        });
    }

    private static void kick(final String device) {
        stop();
        final FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        final Runnable end = () -> endSession(device);
        if (u == null) { end.run(); return; }
        Map<String, Object> out = new HashMap<>();                  // note when this phone was signed out, then end the session
        Map<String, Object> mine = new HashMap<>();
        mine.put("signedOutAt", System.currentTimeMillis());
        Map<String, Object> devs = new HashMap<>();
        devs.put(deviceId(), mine);
        out.put("devices", devs);
        devicesDoc(u.getUid()).set(out, com.google.firebase.firestore.SetOptions.merge()).addOnCompleteListener(t -> end.run());
        new Handler(Looper.getMainLooper()).postDelayed(end, 4000);   // offline: do not hold the sign-out
    }

    private static boolean ended;

    private static void endSession(String device) {
        if (ended) return;
        ended = true;
        new Handler(Looper.getMainLooper()).postDelayed(() -> ended = false, 10000);
        stop();
        prefs().edit().clear().apply();
        // The full sign-out: keys, every saved setting, caches.
        com.harvana.app.auth.LogoutManager.logout(app);
        new Handler(Looper.getMainLooper()).post(() -> {
            Toast.makeText(app, "Signed out: this account was signed in on " + (device == null || device.isEmpty() ? "another phone" : device)
                    + ". An account can be signed in on one phone at a time.", Toast.LENGTH_LONG).show();
            Intent i = new Intent(app, com.harvana.app.SplashScreenActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            app.startActivity(i);
        });
    }
}
