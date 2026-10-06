package com.harvana.app.utils;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputLayout;
import com.harvana.app.R;

/**
 * Safety net for spacing. Buttons and fields that sit right against a neighbour in a LinearLayout (no margin on either side)
 * get a small gap: 8dp, or 12dp between a field and the next control. Layouts that already space their children are untouched,
 * as are toggle groups and anything tagged "keep_tight". Runs on every window, throttled, and marks each view once it is handled.
 */
public final class SmartSpacing {
    private SmartSpacing() {}

    public static void install(Application app) {
        app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override public void onActivityResumed(Activity a) { attach(a.getWindow()); }
            @Override public void onActivityCreated(Activity a, Bundle b) {}
            @Override public void onActivityStarted(Activity a) {}
            @Override public void onActivityPaused(Activity a) {}
            @Override public void onActivityStopped(Activity a) {}
            @Override public void onActivitySaveInstanceState(Activity a, Bundle b) {}
            @Override public void onActivityDestroyed(Activity a) {}
        });
    }

    public static void attach(Window w) {
        if (w == null) return;
        final View decor = w.getDecorView();
        if (decor.getTag(R.id.smart_spacing_attached) != null) { sweep(decor); return; }
        decor.setTag(R.id.smart_spacing_attached, Boolean.TRUE);
        final long[] last = {0};
        decor.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override public void onGlobalLayout() {
                long now = android.os.SystemClock.uptimeMillis();
                if (now - last[0] < 300) return;
                last[0] = now;
                sweep(decor);
            }
        });
        sweep(decor);
    }

    private static boolean control(View v) {
        return v instanceof Button || v instanceof EditText || v instanceof TextInputLayout || v instanceof Spinner;
    }

    private static boolean field(View v) {
        return v instanceof EditText || v instanceof TextInputLayout || v instanceof Spinner;
    }

    private static void sweep(View v) {
        if (!(v instanceof ViewGroup)) return;
        ViewGroup g = (ViewGroup) v;
        if (g instanceof LinearLayout && !(g instanceof MaterialButtonToggleGroup)) space((LinearLayout) g);
        for (int i = 0; i < g.getChildCount(); i++) sweep(g.getChildAt(i));
    }

    private static void space(LinearLayout row) {
        if (row.getShowDividers() != LinearLayout.SHOW_DIVIDER_NONE) return;
        boolean vertical = row.getOrientation() == LinearLayout.VERTICAL;
        float d = row.getResources().getDisplayMetrics().density;
        View prev = null;
        for (int i = 0; i < row.getChildCount(); i++) {
            View c = row.getChildAt(i);
            if (c.getVisibility() == View.GONE) continue;
            if (prev != null && c.getTag(R.id.smart_spacing_done) == null
                    && !"keep_tight".equals(c.getTag()) && !"keep_tight".equals(prev.getTag())
                    && c.getLayoutParams() instanceof LinearLayout.LayoutParams
                    && prev.getLayoutParams() instanceof LinearLayout.LayoutParams) {
                LinearLayout.LayoutParams cp = (LinearLayout.LayoutParams) c.getLayoutParams();
                LinearLayout.LayoutParams pp = (LinearLayout.LayoutParams) prev.getLayoutParams();
                if (vertical) {
                    boolean thin = c.getHeight() > 0 && c.getHeight() < (int) (3 * d) || prev.getHeight() > 0 && prev.getHeight() < (int) (3 * d);
                    if ((control(c) || control(prev)) && !thin && cp.topMargin + pp.bottomMargin == 0) {
                        cp.topMargin = (int) ((field(prev) ? 12 : 8) * d);
                        c.setLayoutParams(cp);
                    }
                } else if (control(c) && control(prev) && cp.getMarginStart() + pp.getMarginEnd() == 0) {
                    cp.setMarginStart((int) (8 * d));
                    c.setLayoutParams(cp);
                }
                c.setTag(R.id.smart_spacing_done, Boolean.TRUE);
            }
            prev = c;
        }
    }
}
