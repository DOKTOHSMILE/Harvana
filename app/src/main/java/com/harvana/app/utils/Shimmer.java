package com.harvana.app.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.harvana.app.R;

/**
 * Shimmer loading placeholders for screens that wait on the node. Grey rounded bars with a light band sweeping across them,
 * in the theme's own colours (day and night), so a screen shows its shape straight away instead of a blank page or a spinner.
 *
 * Usage: put {@link #rows}, {@link #card}, {@link #profile} or {@link #block} where the data will go, and replace it
 * (removeAllViews / addView) when the data arrives. Nothing needs stopping: a bar only animates while it is on screen.
 * All bars sweep in step because the sweep is driven by the clock, not by each bar.
 */
public final class Shimmer {
    private Shimmer() {}

    private static final long PERIOD_MS = 1300L;

    private static int dp(Context c, float v) { return Math.round(v * c.getResources().getDisplayMetrics().density); }

    /** One placeholder bar. {@code widthDp} of -1 fills the width. */
    public static View block(Context c, int widthDp, int heightDp, int radiusDp) {
        Bar b = new Bar(c, dp(c, radiusDp));
        b.setLayoutParams(new LinearLayout.LayoutParams(widthDp < 0 ? ViewGroup.LayoutParams.MATCH_PARENT : dp(c, widthDp), dp(c, heightDp)));
        return b;
    }

    /** A round placeholder, for avatars. */
    public static View circle(Context c, int sizeDp) {
        Bar b = new Bar(c, dp(c, sizeDp) / 2);
        b.setLayoutParams(new LinearLayout.LayoutParams(dp(c, sizeDp), dp(c, sizeDp)));
        return b;
    }

    /** {@code n} lines of text, the last one shorter. */
    public static View lines(Context c, int n) {
        LinearLayout box = vertical(c);
        for (int i = 0; i < n; i++) {
            View line = block(c, i == n - 1 && n > 1 ? 180 : -1, 12, 6);
            ((LinearLayout.LayoutParams) line.getLayoutParams()).topMargin = i == 0 ? 0 : dp(c, 8);
            box.addView(line);
        }
        return box;
    }

    /** A list of {@code n} rows, each an avatar with two lines: friends, members, trades, invitations. */
    public static View rows(Context c, int n) {
        LinearLayout box = vertical(c);
        for (int i = 0; i < n; i++) {
            LinearLayout row = new LinearLayout(c);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(c, 8), 0, dp(c, 8));
            View av = circle(c, 44);
            row.addView(av);
            LinearLayout text = vertical(c);
            LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            tp.setMarginStart(dp(c, 12));
            View a = block(c, 150 + (i % 3) * 30, 14, 7);
            View b = block(c, 100 + (i % 2) * 40, 11, 6);
            ((LinearLayout.LayoutParams) b.getLayoutParams()).topMargin = dp(c, 8);
            text.addView(a);
            text.addView(b);
            row.addView(text, tp);
            box.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        return box;
    }

    /** A card-shaped placeholder: a heading bar and a few lines, on the theme's surface colour. */
    public static View card(Context c, int lines) {
        LinearLayout card = vertical(c);
        int p = dp(c, 16);
        card.setPadding(p, p, p, p);
        android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
        g.setColor(UiColors.surface());
        g.setCornerRadius(dp(c, 14));
        g.setStroke(dp(c, 1), (UiColors.textTertiary() & 0x00FFFFFF) | 0x33000000);
        card.setBackground(g);
        card.addView(block(c, 130, 16, 8));
        View body = lines(c, Math.max(1, lines));
        ((LinearLayout.LayoutParams) body.getLayoutParams()).topMargin = dp(c, 14);
        card.addView(body);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(c, 14);
        card.setLayoutParams(lp);
        return card;
    }

    /** A profile page: big avatar, name, a row of action tiles and two cards. */
    public static View profile(Context c) {
        LinearLayout page = vertical(c);
        page.setGravity(Gravity.CENTER_HORIZONTAL);
        View av = circle(c, 96);
        ((LinearLayout.LayoutParams) av.getLayoutParams()).topMargin = dp(c, 16);
        page.addView(av);
        View name = block(c, 180, 20, 10);
        ((LinearLayout.LayoutParams) name.getLayoutParams()).topMargin = dp(c, 16);
        page.addView(name);
        View sub = block(c, 120, 12, 6);
        ((LinearLayout.LayoutParams) sub.getLayoutParams()).topMargin = dp(c, 10);
        page.addView(sub);
        LinearLayout tiles = new LinearLayout(c);
        tiles.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < 4; i++) {
            View t = new Bar(c, dp(c, 16));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(c, 64), 1f);
            lp.setMargins(dp(c, 4), 0, dp(c, 4), 0);
            tiles.addView(t, lp);
        }
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tl.topMargin = dp(c, 22);
        tl.bottomMargin = dp(c, 16);
        page.addView(tiles, tl);
        page.addView(card(c, 3));
        page.addView(card(c, 2));
        return page;
    }

    /** A screen of cards, for dashboards and detail pages. */
    public static View cards(Context c, int n) {
        LinearLayout box = vertical(c);
        for (int i = 0; i < n; i++) {
            box.addView(card(c, 2 + (i % 2)));
        }
        return box;
    }

    /** Puts {@code placeholder} into an empty-able host, replacing what is there. Call {@code host.removeAllViews()} when the data arrives. */
    public static void show(ViewGroup host, View placeholder) {
        host.removeAllViews();
        host.addView(placeholder, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    /**
     * A full-size shimmer laid over an existing screen until the data is in (for screens built in XML, where the real views are
     * already there). Returns the overlay; call {@link #hide(View)} to remove it.
     */
    public static View overlay(FrameLayout host, View placeholder) {
        FrameLayout shield = new FrameLayout(host.getContext());
        shield.setBackgroundColor(UiColors.background());
        shield.setClickable(true);
        shield.addView(placeholder, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        int pad = dp(host.getContext(), 16);
        shield.setPadding(pad, pad, pad, pad);
        host.addView(shield, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return shield;
    }

    public static void hide(View overlay) {
        if (overlay != null && overlay.getParent() instanceof ViewGroup) ((ViewGroup) overlay.getParent()).removeView(overlay);
    }

    // ── Holding a list or section until its data is in ─────────────────────────────

    private static final java.util.Map<View, Hold> HOLDS = new java.util.WeakHashMap<>();

    /** A placeholder standing in for a view whose data is still on its way. */
    public static final class Hold {
        private final View target;
        private final View skeleton;
        private boolean released;
        private Hold(View target, View skeleton) { this.target = target; this.skeleton = skeleton; }

        /** Shows the real view and removes the placeholder. Safe to call again. */
        public void release() {
            if (released) return;
            released = true;
            target.setVisibility(View.VISIBLE);
            if (skeleton.getParent() instanceof ViewGroup) ((ViewGroup) skeleton.getParent()).removeView(skeleton);
            HOLDS.remove(target);
        }
    }

    /**
     * Shows {@code skeleton} in place of {@code target} (a list or a section built in XML) until the target has content, or
     * {@code maxMs} has passed (so an empty or failed load still ends in the screen's own empty state). The target keeps its id
     * and position: it is moved into a frame that takes its place. Lists whose parent is a ConstraintLayout or RelativeLayout are
     * left alone, because other views point at them by id.
     */
    public static Hold hold(View target, View skeleton, long maxMs) {
        if (target == null || !(target.getParent() instanceof ViewGroup)) return null;
        if (HOLDS.containsKey(target)) return HOLDS.get(target);
        ViewGroup parent = (ViewGroup) target.getParent();
        String pc = parent.getClass().getName();
        if (pc.contains("ConstraintLayout") || parent instanceof android.widget.RelativeLayout) return null;
        if (hasContent(target)) return null;

        int index = parent.indexOfChild(target);
        ViewGroup.LayoutParams lp = target.getLayoutParams();
        FrameLayout frame = new FrameLayout(target.getContext());
        parent.removeViewAt(index);
        parent.addView(frame, index, lp);
        frame.addView(target, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            lp.height == ViewGroup.LayoutParams.MATCH_PARENT ? ViewGroup.LayoutParams.MATCH_PARENT : ViewGroup.LayoutParams.WRAP_CONTENT));
        target.setVisibility(View.INVISIBLE);
        frame.addView(skeleton, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final Hold h = new Hold(target, skeleton);
        HOLDS.put(target, h);
        target.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override public void onLayoutChange(View v, int l, int t, int r, int b, int ol, int ot, int or, int ob) {
                if (hasContent(v)) {
                    v.removeOnLayoutChangeListener(this);
                    // Never change the view tree from inside a layout pass: the frame is still walking its children, and a
                    // child removed now is a null the walk then trips over (NullPointerException in FrameLayout.layoutChildren).
                    v.post(h::release);
                }
            }
        });
        target.postDelayed(h::release, maxMs);
        return h;
    }

    /** Ends the hold on {@code target} now, for loads that finish with nothing to show. */
    public static void release(View target) {
        Hold h = HOLDS.get(target);
        if (h != null) h.release();
    }

    private static boolean hasContent(View v) {
        if (v instanceof androidx.recyclerview.widget.RecyclerView) {
            androidx.recyclerview.widget.RecyclerView.Adapter<?> a = ((androidx.recyclerview.widget.RecyclerView) v).getAdapter();
            return a != null && a.getItemCount() > 0;
        }
        if (v instanceof android.widget.AdapterView) {
            android.widget.Adapter a = ((android.widget.AdapterView<?>) v).getAdapter();
            return a != null && a.getCount() > 0;
        }
        return false;     // a plain section has no way to say it is loaded: it is released by release() or the timeout
    }

    // ── A single piece of text that is still loading ──────────────────────────────

    private static final int TAG_WATCHING = R.id.shimmer_watch;

    /**
     * Puts {@code tv} into the loading state: its text becomes a shimmering bar until anything other than a "Loading…" text is set
     * on it. Use it in place of {@code setText("Loading…")} for a number, a name or a line that comes from the node.
     */
    public static void loading(TextView tv) {
        if (tv == null) return;
        watch(tv);
        tv.setText("Loading\u2026");
    }

    /** Makes {@code tv} shimmer whenever its text starts with "Loading", and stop when the text changes. Safe to call again. */
    public static void watch(final TextView tv) {
        if (tv == null || tv.getTag(TAG_WATCHING) != null) { if (tv != null) refresh(tv); return; }
        tv.setTag(TAG_WATCHING, new Object[] {tv.getCurrentTextColor(), null});
        tv.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(android.text.Editable e) { refresh(tv); }
        });
        refresh(tv);
    }

    private static void refresh(TextView tv) {
        Object tag = tv.getTag(TAG_WATCHING);
        if (!(tag instanceof Object[])) return;
        Object[] saved = (Object[]) tag;
        boolean waiting = tv.getText() != null && tv.getText().toString().startsWith("Loading");
        if (waiting) {
            if (!(tv.getBackground() instanceof BarDrawable)) {
                if (tv.getCurrentTextColor() != android.graphics.Color.TRANSPARENT) saved[0] = tv.getCurrentTextColor();
                saved[1] = tv.getBackground();
                tv.setTextColor(android.graphics.Color.TRANSPARENT);
                tv.setBackground(new BarDrawable(Math.round(6 * tv.getResources().getDisplayMetrics().density)));
            }
        } else if (tv.getBackground() instanceof BarDrawable) {
            tv.setBackground((android.graphics.drawable.Drawable) saved[1]);
            tv.setTextColor((Integer) saved[0]);
        }
    }

    /** The same bar as {@link Bar}, as a background, so any text view can shimmer without being replaced. */
    private static final class BarDrawable extends android.graphics.drawable.Drawable implements Runnable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private final int radius;

        BarDrawable(int radius) { this.radius = radius; }

        @Override public void draw(Canvas canvas) {
            android.graphics.Rect b = getBounds();
            if (b.isEmpty()) return;
            rect.set(b);
            paint.setShader(null);
            paint.setColor((UiColors.textTertiary() & 0x00FFFFFF) | 0x33000000);
            canvas.drawRoundRect(rect, radius, radius, paint);
            float phase = (SystemClock.uptimeMillis() % PERIOD_MS) / (float) PERIOD_MS;
            float w = Math.max(b.width(), 1);
            int light = (UiColors.textPrimary() & 0x00FFFFFF) | 0x26000000;
            LinearGradient g = new LinearGradient(b.left + (phase * 2 - 0.5f) * w, 0, b.left + (phase * 2 + 0.5f) * w, 0,
                new int[] {0x00FFFFFF & light, light, 0x00FFFFFF & light}, new float[] {0f, 0.5f, 1f}, Shader.TileMode.CLAMP);
            paint.setShader(g);
            canvas.drawRoundRect(rect, radius, radius, paint);
            paint.setShader(null);
            scheduleSelf(this, SystemClock.uptimeMillis() + 16);
        }

        @Override public void run() { invalidateSelf(); }
        @Override public void setAlpha(int alpha) { }
        @Override public void setColorFilter(android.graphics.ColorFilter cf) { }
        @Override public int getOpacity() { return android.graphics.PixelFormat.TRANSLUCENT; }
    }

    private static LinearLayout vertical(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return l;
    }

    /** The bar itself: a rounded grey block with a light band that sweeps left to right, in step with every other bar. */
    private static final class Bar extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private final Matrix matrix = new Matrix();
        private final int radius;
        private final int[] where = new int[2];
        private LinearGradient band;
        private int bandFor = -1;

        Bar(Context c, int radius) {
            super(c);
            this.radius = radius;
        }

        private static int base() { return (UiColors.textTertiary() & 0x00FFFFFF) | 0x33000000; }
        private static int light() { return (UiColors.textPrimary() & 0x00FFFFFF) | 0x26000000; }

        @Override
        protected void onDraw(Canvas canvas) {
            int w = getWidth(), h = getHeight();
            if (w == 0 || h == 0) return;
            rect.set(0, 0, w, h);
            paint.setShader(null);
            paint.setColor(base());
            canvas.drawRoundRect(rect, radius, radius, paint);

            int screenW = getResources().getDisplayMetrics().widthPixels;
            int bandW = Math.max(screenW / 2, 1);
            if (band == null || bandFor != bandW) {
                band = new LinearGradient(0, 0, bandW, 0, new int[] {0x00FFFFFF & light(), light(), 0x00FFFFFF & light()},
                    new float[] {0f, 0.5f, 1f}, Shader.TileMode.CLAMP);
                bandFor = bandW;
            }
            float phase = (SystemClock.uptimeMillis() % PERIOD_MS) / (float) PERIOD_MS;
            getLocationInWindow(where);
            float sweep = -bandW + phase * (screenW + bandW);        // position of the band in window coordinates
            matrix.setTranslate(sweep - where[0], 0);
            band.setLocalMatrix(matrix);
            paint.setShader(band);
            canvas.drawRoundRect(rect, radius, radius, paint);
            paint.setShader(null);
            postInvalidateOnAnimation();
        }
    }
}
