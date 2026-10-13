package com.harvana.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The first screen: the app logo with a short animation, nothing else. Then it decides where to go:
 * a signed-in member goes to the app (or to finish their profile), everyone else to the sign-in choices (WelcomeActivity).
 * Other screens that need "start over from the right place" (after logging out or signing in) also return here.
 */
public class SplashScreenActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 101;
    private static final long MIN_SHOW_MS = 1600;
    private static final int MAX_PERMISSION_PROMPTS = 2;

    private final Handler main = new Handler(Looper.getMainLooper());
    private long startedAt;
    private boolean routed;
    private TextView status;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        FirebaseApp.initializeApp(this);
        startedAt = System.currentTimeMillis();
        buildUi();
        animate();
        main.postDelayed(this::afterAnimation, MIN_SHOW_MS);
    }

    private void buildUi() {
        setContentView(R.layout.activity_splash);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimary));
        status = findViewById(R.id.splash_status);
    }

    /** Logo pops in with a slight overshoot and then breathes once; the name and tagline rise into place. */
    private void animate() {
        View logo = findViewById(R.id.splash_logo), name = findViewById(R.id.splash_name), tagline = findViewById(R.id.splash_tagline);
        float d = getResources().getDisplayMetrics().density;
        logo.setAlpha(0f); logo.setScaleX(0.5f); logo.setScaleY(0.5f);
        logo.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(650).setInterpolator(new OvershootInterpolator(1.6f))
            .withEndAction(() -> logo.animate().scaleX(1.06f).scaleY(1.06f).setDuration(380)
                .withEndAction(() -> logo.animate().scaleX(1f).scaleY(1f).setDuration(380).start()).start()).start();
        for (View v : new View[]{name, tagline}) { v.setAlpha(0f); v.setTranslationY(14 * d); }
        name.animate().alpha(1f).translationY(0f).setStartDelay(450).setDuration(450).start();
        tagline.animate().alpha(1f).translationY(0f).setStartDelay(650).setDuration(450).start();
    }

    private void afterAnimation() {
        if (isFinishing() || isDestroyed()) return;
        SharedPreferences sp = getSharedPreferences("splash_prefs", Activity.MODE_PRIVATE);
        if (!hasAllPermissions() && sp.getInt("permission_prompts", 0) < MAX_PERMISSION_PROMPTS) {
            sp.edit().putInt("permission_prompts", sp.getInt("permission_prompts", 0) + 1).apply();
            List<String> list = new ArrayList<>();
            list.add(Manifest.permission.ACCESS_FINE_LOCATION);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) list.add(Manifest.permission.POST_NOTIFICATIONS);
            ActivityCompat.requestPermissions(this, list.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        } else {
            route();
        }
    }

    private boolean hasAllPermissions() {
        boolean loc = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean notif = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
            || ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        return loc && notif;
    }

    @Override
    public void onRequestPermissionsResult(int code, @NonNull String[] perms, @NonNull int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        if (code == PERMISSION_REQUEST_CODE) {
            boolean all = results.length > 0;
            for (int r : results) if (r != PackageManager.PERMISSION_GRANTED) all = false;
            if (!all) Toast.makeText(this, "Some features need these permissions. You can allow them later in Settings.", Toast.LENGTH_LONG).show();
            route();
        }
    }

    /** Signed in: to the app, or to finish the profile. Not signed in: to the sign-in choices. */
    private void route() {
        if (routed || isFinishing()) return;
        final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { go(new Intent(this, WelcomeActivity.class)); return; }
        if (com.harvana.app.auth.EmailVerification.needsVerification(user)) {
            // Confirmed in the browser since last time? Reload first so they are not sent back needlessly.
            user.reload().addOnCompleteListener(t -> {
                FirebaseUser fresh = FirebaseAuth.getInstance().getCurrentUser();
                if (fresh != null && com.harvana.app.auth.EmailVerification.needsVerification(fresh)) {
                    go(new Intent(this, VerifyEmailActivity.class));
                } else {
                    route();
                }
            });
            return;
        }
        final String uid = user.getUid();
        status.setText("");
        // Like WhatsApp: a person who has used the app on this phone goes straight in, from what is saved, and the
        // profile is refreshed in the background. Only a first start waits for the network.
        Map<String, Object> saved = null;
        try { saved = com.harvana.app.offline.OfflineCacheManager.load(getApplicationContext(), "profile_" + uid); } catch (Exception ignored) { }
        if (saved != null && !saved.isEmpty()) {
            final android.content.Context app = getApplicationContext();
            com.harvana.app.firestore.UserProfileManager.getInstance(app).getProfile(uid, new com.harvana.app.firestore.UserProfileManager.DocCallback() {
                @Override public void onSuccess(Map<String, Object> data) {
                    if (data != null && !data.isEmpty()) {
                        try { com.harvana.app.offline.OfflineCacheManager.save(app, "profile_" + uid, data); } catch (Exception ignored) { }
                    }
                }
                @Override public void onError(String error) { }
            });
            go(new Intent(this, MainActivity.class));
            return;
        }
        com.harvana.app.firestore.UserProfileManager.getInstance(getApplicationContext()).getProfile(uid,
            new com.harvana.app.firestore.UserProfileManager.DocCallback() {
                @Override public void onSuccess(Map<String, Object> data) {
                    if (data != null && !data.isEmpty()) {
                        try { com.harvana.app.offline.OfflineCacheManager.save(getApplicationContext(), "profile_" + uid, data); }
                        catch (Exception e) { android.util.Log.e("Splash", "cache save failed", e); }
                        runOnUiThread(() -> go(new Intent(SplashScreenActivity.this, MainActivity.class)));
                    } else {
                        runOnUiThread(() -> completeProfile(user));
                    }
                }
                @Override public void onError(String error) {
                    if (com.harvana.app.offline.NetworkStatus.isConnectivityError(error)) {
                        try {
                            Map<String, Object> cached = com.harvana.app.offline.OfflineCacheManager.load(getApplicationContext(), "profile_" + uid);
                            if (cached != null && !cached.isEmpty()) { runOnUiThread(() -> go(new Intent(SplashScreenActivity.this, MainActivity.class))); return; }
                        } catch (Exception e) { android.util.Log.e("Splash", "cache load failed", e); }
                        runOnUiThread(() -> offline());
                    } else {
                        runOnUiThread(() -> completeProfile(user));
                    }
                }
            });
    }

    private void completeProfile(FirebaseUser user) {
        String photo = user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "";
        String email = user.getEmail() != null ? user.getEmail() : "";
        getSharedPreferences("authsp", Activity.MODE_PRIVATE).edit().putString("photoUrl", photo).putString("gmail", email).commit();
        go(new Intent(this, LoginActivity.class).putExtra("page", "complete"));
    }

    private void offline() {
        status.setText("You're offline. Tap to retry");
        status.setOnClickListener(v -> { status.setText(""); route(); });
    }

    private void go(Intent i) {
        if (routed) return;
        routed = true;
        startActivity(i);
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
