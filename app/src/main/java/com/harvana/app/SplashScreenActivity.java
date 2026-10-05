package com.harvana.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary));
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimary));
        float d = getResources().getDisplayMetrics().density;

        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.CENTER);
        root.addView(column, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        ImageView logo = new ImageView(this);
        logo.setId(View.generateViewId());
        logo.setImageResource(R.mipmap.ic_launcher_foreground);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        logo.setContentDescription("Harvana");
        logo.setTag("logo");
        LinearLayout.LayoutParams logoLp = new LinearLayout.LayoutParams((int) (80 * d), (int) (80 * d));
        logoLp.gravity = Gravity.CENTER_HORIZONTAL;
        column.addView(logo, logoLp);

        TextView name = new TextView(this);
        name.setText("Harvana");
        name.setTextSize(28);
        name.setTypeface(null, Typeface.BOLD);
        name.setTextColor(ContextCompat.getColor(this, R.color.colorTextPrimary));
        name.setTag("name");
        name.setGravity(Gravity.CENTER);
        name.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        nameLp.topMargin = (int) (18 * d);
        column.addView(name, nameLp);

        TextView tagline = new TextView(this);
        tagline.setText("Save together. Build trust.");
        tagline.setTextSize(14);
        tagline.setTextColor(ContextCompat.getColor(this, R.color.colorTextSecondary));
        tagline.setTag("tagline");
        tagline.setGravity(Gravity.CENTER);
        tagline.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        LinearLayout.LayoutParams tagLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tagLp.topMargin = (int) (6 * d);
        column.addView(tagline, tagLp);

        status = new TextView(this);
        status.setTextSize(13);
        status.setTextColor(ContextCompat.getColor(this, R.color.colorTextSecondary));
        status.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
        lp.bottomMargin = (int) (48 * d);
        root.addView(status, lp);

        setContentView(root);
    }

    /** Logo pops in with a slight overshoot and then breathes once; the name and tagline rise into place. */
    private void animate() {
        View root = findViewById(android.R.id.content);
        View logo = root.findViewWithTag("logo"), name = root.findViewWithTag("name"), tagline = root.findViewWithTag("tagline");
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
