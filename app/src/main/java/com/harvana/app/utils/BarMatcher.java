package com.harvana.app.utils;

import android.app.Activity;
import android.app.Application;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.WindowCompat;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Makes the app bar, the status bar (and a bottom bar tagged "bar_bottom") the same colour as the body of the page being shown, on every
 * screen, by reading the page itself instead of trusting one theme colour. Pages differ (a white page, a cream page, a dark page), and
 * the same page can change as tabs change, so this looks again whenever the screen is laid out.
 *
 * How the body colour is found: among the views that start right under the bar, are nearly full width and tall, and have a solid colour
 * behind them, the one nearest the bar (the deepest when tied) is the body. With no bar, the screen's main background is used. If no
 * view has a solid colour, the window's own background is used.
 *
 * Bars it recolours: every {@code Toolbar} and its {@code AppBarLayout}, a {@code CollapsingToolbarLayout}'s scrim, any view tagged
 * "bar_top", the support action bar, and the window's status bar (icons dark on a light colour, light on a dark one).
 * Splash, welcome, the crash screen and the full-screen picture screens keep their own look.
 */
public final class BarMatcher {
    private BarMatcher() {}

    private static final Set<String> SKIP = new HashSet<>(Arrays.asList(
            "SplashScreenActivity", "WelcomeActivity", "DebugActivity", "ImageViewerActivity", "CropImageActivity", "GroupPhotoPickerActivity"));
    private static final WeakHashMap<Activity, State> STATES = new WeakHashMap<>();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    /** Registers on every screen once. Call from Application.onCreate. */
    public static void install(Application app) {
        app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override public void onActivityStarted(Activity a) { attach(a); }
            @Override public void onActivityCreated(Activity a, Bundle b) { }
            @Override public void onActivityResumed(Activity a) { State s = STATES.get(a); if (s != null) s.schedule(); }
            @Override public void onActivityPaused(Activity a) { }
            @Override public void onActivityStopped(Activity a) { }
            @Override public void onActivitySaveInstanceState(Activity a, Bundle b) { }
            @Override public void onActivityDestroyed(Activity a) { STATES.remove(a); }
        });
    }

    private static void attach(Activity a) {
        if (STATES.containsKey(a) || SKIP.contains(a.getClass().getSimpleName())) return;
        if (!(a instanceof AppCompatActivity)) return;
        State s = new State(a);
        STATES.put(a, s);
        a.getWindow().getDecorView().getViewTreeObserver().addOnGlobalLayoutListener(s);
        s.schedule();
    }

    private static final class State implements ViewTreeObserver.OnGlobalLayoutListener {
        final Activity a;
        int last = 0;
        boolean pending;
        State(Activity a) { this.a = a; }

        @Override public void onGlobalLayout() { schedule(); }

        void schedule() {
            if (pending) return;
            pending = true;
            MAIN.postDelayed(() -> { pending = false; if (!a.isFinishing() && !a.isDestroyed()) run(); }, 60);
        }

        void run() {
            try {
                ViewGroup content = a.findViewById(android.R.id.content);
                if (content == null || content.getChildCount() == 0) return;
                java.util.List<View> bars = new java.util.ArrayList<>();
                collectBars(content, bars);
                float d = a.getResources().getDisplayMetrics().density;
                int barBottom = 0;
                for (View b : bars) barBottom = Math.max(barBottom, bottomOf(b));
                Integer color = bodyColor(a, content, barBottom, d);
                if (color == null) return;
                if (color == last) return;
                last = color;
                apply(color, bars, content);
            } catch (Exception ignored) { }
        }

        void apply(int color, java.util.List<View> bars, ViewGroup content) {
            for (View b : bars) {
                if (b instanceof CollapsingToolbarLayout) ((CollapsingToolbarLayout) b).setContentScrimColor(color);
                else b.setBackgroundColor(color);
                if (b instanceof AppBarLayout || b instanceof Toolbar) b.setElevation(0f);
            }
            if (a instanceof AppCompatActivity && ((AppCompatActivity) a).getSupportActionBar() != null) {
                ((AppCompatActivity) a).getSupportActionBar().setBackgroundDrawable(new ColorDrawable(color));
            }
            View bottom = content.findViewWithTag("bar_bottom");
            if (bottom != null) {
                if (bottom instanceof com.google.android.material.card.MaterialCardView) ((com.google.android.material.card.MaterialCardView) bottom).setCardBackgroundColor(color);
                else bottom.setBackgroundColor(color);
            }
            Window w = a.getWindow();
            w.addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            w.setStatusBarColor(color);
            boolean light = ColorUtils.calculateLuminance(color) > 0.5;
            WindowCompat.getInsetsController(w, w.getDecorView()).setAppearanceLightStatusBars(light);
        }
    }

    // ── finding the bars ──────────────────────────────────────────

    private static void collectBars(View v, java.util.List<View> out) {
        if (v instanceof Toolbar || v instanceof AppBarLayout || v instanceof CollapsingToolbarLayout || "bar_top".equals(v.getTag())) {
            out.add(v);
            if (!(v instanceof AppBarLayout)) return;       // an AppBarLayout's toolbar is collected on its own below
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) collectBars(g.getChildAt(i), out);
        }
    }

    private static int bottomOf(View v) {
        int[] loc = new int[2];
        v.getLocationInWindow(loc);
        return v.getHeight() > 0 ? loc[1] + v.getHeight() : 0;
    }

    // ── finding the body colour ───────────────────────────────────

    private static Integer bodyColor(Activity a, ViewGroup content, int barBottom, float d) {
        int screenW = a.getResources().getDisplayMetrics().widthPixels;
        int screenH = a.getResources().getDisplayMetrics().heightPixels;
        final int[] bestTop = {Integer.MAX_VALUE};
        final Integer[] best = {null};
        final int[] bestDepth = {-1};
        find(content, barBottom, screenW, screenH, d, 0, bestTop, best, bestDepth, false);
        if (best[0] != null) return best[0];
        // No bar, or nothing starts right under it: the screen's main background.
        Integer root = solid(content.getChildAt(0));
        if (root != null) return root;
        Drawable window = a.getWindow().getDecorView().getBackground();
        if (window instanceof ColorDrawable) return ((ColorDrawable) window).getColor() | 0xFF000000;
        return null;
    }

    private static void find(View v, int barBottom, int screenW, int screenH, float d, int depth, int[] bestTop, Integer[] best, int[] bestDepth, boolean insideBar) {
        boolean bar = insideBar || v instanceof Toolbar || v instanceof AppBarLayout || v instanceof CollapsingToolbarLayout || "bar_top".equals(v.getTag());
        if (v.getVisibility() != View.VISIBLE) return;
        if (!bar && v.getWidth() >= screenW * 0.9f && v.getHeight() >= screenH * 0.3f) {
            Integer c = solid(v);
            if (c != null) {
                int[] loc = new int[2];
                v.getLocationInWindow(loc);
                int top = loc[1];
                boolean underBar = barBottom == 0 ? top <= Math.round(8 * d) : (top >= barBottom - Math.round(2 * d) && top <= barBottom + Math.round(16 * d));
                if (underBar) {
                    int diff = Math.abs(top - barBottom);
                    if (diff < bestTop[0] || (diff == bestTop[0] && depth > bestDepth[0])) { bestTop[0] = diff; best[0] = c; bestDepth[0] = depth; }
                }
            }
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) find(g.getChildAt(i), barBottom, screenW, screenH, d, depth + 1, bestTop, best, bestDepth, bar);
        }
    }

    /** The view's own solid, fully opaque colour, or null. */
    private static Integer solid(View v) {
        if (v == null) return null;
        if (v instanceof com.google.android.material.card.MaterialCardView) return null;     // cards sit on the page, they are not the page
        Drawable bg = v.getBackground();
        if (bg instanceof ColorDrawable) {
            int c = ((ColorDrawable) bg).getColor();
            if (Color.alpha(c) == 255) return c;
        }
        return null;
    }
}
