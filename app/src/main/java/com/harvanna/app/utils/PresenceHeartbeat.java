package com.harvanna.app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import com.google.firebase.auth.FirebaseAuth;
import com.harvanna.app.firestore.UserProfileManager;

/**
 * While the app is on screen, tells Firestore the user is active (lastActiveAt) every 2 minutes so other
 * members' avatars can show a real online ring. Respects the privacy switch "show activity status".
 * Nothing is written while the app is in the background, so people naturally go from online to away to offline.
 */
public final class PresenceHeartbeat {
    private static final long PERIOD_MS = 2L * 60 * 1000;
    private static final Handler H = new Handler(Looper.getMainLooper());
    private static Runnable tick;

    private PresenceHeartbeat() {}

    public static synchronized void start(Context ctx) {
        stop();
        final Context app = ctx.getApplicationContext();
        tick = new Runnable() {
            @Override public void run() {
                beat(app);
                H.postDelayed(this, PERIOD_MS);
            }
        };
        H.post(tick);
    }

    public static synchronized void stop() {
        if (tick != null) { H.removeCallbacks(tick); tick = null; }
    }

    private static void beat(Context app) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        SharedPreferences p = app.getSharedPreferences("privacy_prefs", Context.MODE_PRIVATE);
        boolean show = p.getBoolean("show_activity_status", true);
        java.util.Map<String, Object> f = new java.util.HashMap<>();
        f.put("showActivityStatus", show);
        if (show) f.put("lastActiveAt", System.currentTimeMillis());
        UserProfileManager.getInstance(app).updateProfileFields(FirebaseAuth.getInstance().getCurrentUser().getUid(), f,
            new UserProfileManager.SimpleCallback() {
                @Override public void onSuccess() { }
                @Override public void onError(String error) { } // best-effort
            });
    }
}
