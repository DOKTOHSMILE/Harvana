package com.harvana.app.web3;

import android.app.Activity;
import android.app.Application;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.harvana.app.R;
import com.harvana.app.utils.UiColors;

/**
 * A small floating Web3 button on every app screen while the browser is minimized. Tap: back to the browser and its
 * tabs. ×: hides the button until the browser is minimized again. Drag: moves it; it snaps to the nearest side.
 */
public final class Web3Bubble {
    private static final String TAG = "web3_bubble";
    private static boolean minimized, dismissed;
    private static float savedY = -1;
    private static boolean savedLeft;

    private Web3Bubble() {}

    /** The browser went behind the app with its tabs open. */
    public static void onMinimized() { minimized = true; dismissed = false; }

    /** The browser is in front again, or closed. */
    public static void onBrowserGone() { minimized = false; }

    public static void install(Application app) {
        app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override public void onActivityResumed(Activity a) {
                if (a instanceof DappBrowserActivity) { minimized = false; return; }
                paint(a);
            }
            @Override public void onActivityCreated(Activity a, Bundle b) { }
            @Override public void onActivityStarted(Activity a) { }
            @Override public void onActivityPaused(Activity a) { }
            @Override public void onActivityStopped(Activity a) { }
            @Override public void onActivitySaveInstanceState(Activity a, Bundle b) { }
            @Override public void onActivityDestroyed(Activity a) { }
        });
    }

    private static void paint(Activity a) {
        View content = a.findViewById(android.R.id.content);
        if (!(content instanceof FrameLayout)) return;
        FrameLayout root = (FrameLayout) content;
        View old = root.findViewWithTag(TAG);
        boolean show = minimized && !dismissed && BrowserTabs.count(a) > 0;
        if (!show) { if (old != null) root.removeView(old); return; }
        if (old != null) { old.bringToFront(); return; }
        root.addView(build(a, root));
    }

    private static View build(Activity a, FrameLayout root) {
        float d = a.getResources().getDisplayMetrics().density;
        int size = Math.round(44 * d), close = Math.round(18 * d), box = size + close / 2;

        FrameLayout wrap = new FrameLayout(a);
        wrap.setTag(TAG);
        wrap.setElevation(8 * d);

        ImageView btn = new ImageView(a);
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(UiColors.accent());
        circle.setStroke(Math.max(2, Math.round(d)), 0x33FFFFFF);
        btn.setBackground(circle);
        btn.setImageResource(R.drawable.ic_web3);
        int p = Math.round(11 * d);
        btn.setPadding(p, p, p, p);
        btn.setContentDescription("Web3");
        FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(size, size, Gravity.BOTTOM | Gravity.START);
        wrap.addView(btn, bp);

        ImageView x = new ImageView(a);
        GradientDrawable xc = new GradientDrawable();
        xc.setShape(GradientDrawable.OVAL);
        xc.setColor(0xE6202020);
        xc.setStroke(Math.max(2, Math.round(d)), 0xFFFFFFFF);
        x.setBackground(xc);
        x.setImageResource(R.drawable.ic_close);
        x.setColorFilter(0xFFFFFFFF);
        int xp = Math.round(4 * d);
        x.setPadding(xp, xp, xp, xp);
        x.setContentDescription("Dismiss");
        wrap.addView(x, new FrameLayout.LayoutParams(close, close, Gravity.TOP | Gravity.END));
        x.setOnClickListener(v -> { dismissed = true; root.removeView(wrap); });

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(box, box);
        wrap.setLayoutParams(lp);
        int margin = Math.round(12 * d);
        // Bottom end, above the bottom navigation, until the person moves it.
        root.post(() -> {
            int w = root.getWidth(), h = root.getHeight();
            if (w == 0 || h == 0) return;
            float y = savedY >= 0 ? Math.min(savedY, h - box - margin) : h - box - Math.round(96 * d);
            wrap.setX(savedLeft ? margin : w - box - margin);
            wrap.setY(Math.max(margin, y));
        });

        btn.setOnTouchListener(new View.OnTouchListener() {
            float downX, downY, startX, startY;
            boolean moved;
            final int slop = Math.round(8 * d);
            @Override public boolean onTouch(View v, MotionEvent e) {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = e.getRawX(); downY = e.getRawY(); startX = wrap.getX(); startY = wrap.getY(); moved = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float dx = e.getRawX() - downX, dy = e.getRawY() - downY;
                        if (!moved && Math.abs(dx) < slop && Math.abs(dy) < slop) return true;
                        moved = true;
                        wrap.setX(clamp(startX + dx, 0, root.getWidth() - box));
                        wrap.setY(clamp(startY + dy, margin, root.getHeight() - box - margin));
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!moved) { v.performClick(); Web3Home.openTabs(a); return true; }
                        savedLeft = wrap.getX() + box / 2f < root.getWidth() / 2f;
                        savedY = wrap.getY();
                        wrap.animate().x(savedLeft ? margin : root.getWidth() - box - margin).setDuration(150).start();
                        return true;
                    case MotionEvent.ACTION_CANCEL:
                        return true;
                }
                return false;
            }
        });
        return wrap;
    }

    private static float clamp(float v, float lo, float hi) { return Math.max(lo, Math.min(hi, v)); }
}
