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

    private static void start(final String uid) {
        stop();
        watchedUid = uid;
        SharedPreferences p = prefs();
        String mine = p.getString("id_" + uid, "");
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

    private static void kick(String device) {
        stop();
        prefs().edit().clear().apply();
        try { FirebaseAuth.getInstance().signOut(); } catch (Exception ignored) {}
        com.harvana.app.sync.AuthTokenProvider.clear();
        app.getSharedPreferences("authsp", Context.MODE_PRIVATE).edit().clear().apply();
        new Handler(Looper.getMainLooper()).post(() -> {
            Toast.makeText(app, "Signed out: this account was signed in on " + (device == null || device.isEmpty() ? "another phone" : device)
                    + ". An account can be signed in on one phone at a time.", Toast.LENGTH_LONG).show();
            Intent i = new Intent(app, com.harvana.app.SplashScreenActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            app.startActivity(i);
        });
    }
}
