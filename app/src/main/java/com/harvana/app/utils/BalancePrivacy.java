package com.harvana.app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ImageView;
import android.widget.TextView;

import com.harvana.app.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.regex.Pattern;

/**
 * One show/hide switch for every balance in the app (the eye icon). The choice is saved, so it holds after the app closes, and it is
 * shared: tapping the eye on any screen hides or shows every balance on every screen that is open, at once.
 *
 * How balances are covered without touching each screen's code: a TextView that shows a balance is "guarded" once. From then on, whatever
 * text any code puts into it is remembered as the real text and, while balances are hidden, shown with its numbers replaced by dots
 * ("600.90755" becomes "••••", "≈ ₦1,200.50" becomes "≈ ₦••••", "20 HARV" becomes "•••• HARV"). Showing again puts the real text back.
 * Only balances are guarded (wallet totals, fiat values, coin rows, "total saved", loans), not prices, dates or counts.
 */
public final class BalancePrivacy {
    private BalancePrivacy() {}

    private static final String PREFS = "balance_privacy";
    private static final String KEY = "hidden";
    private static final Pattern NUMBER = Pattern.compile("[0-9][0-9.,]*");
    private static final String DOTS = "••••";

    private static final Map<TextView, Guard> GUARDS = new WeakHashMap<>();
    private static final Map<ImageView, Boolean> EYES = new WeakHashMap<>();
    private static Boolean cached;

    public static boolean isHidden(Context c) {
        if (cached == null) cached = c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY, false);
        return cached;
    }

    public static void setHidden(Context c, boolean hidden) {
        cached = hidden;
        SharedPreferences p = c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        p.edit().putBoolean(KEY, hidden).apply();
        repaintAll(hidden);
    }

    public static void toggle(Context c) { setHidden(c, !isHidden(c)); }

    /** The text as it should be shown right now. */
    public static String mask(String raw) {
        return raw == null ? "" : NUMBER.matcher(raw).replaceAll(DOTS);
    }

    // ── balances ──────────────────────────────────────────────────

    /** Guards a TextView that shows a balance. Safe to call again for the same view (recycled rows do). */
    public static void guard(TextView tv) {
        if (tv == null) return;
        synchronized (GUARDS) {
            if (GUARDS.containsKey(tv)) return;
            Guard g = new Guard(tv);
            GUARDS.put(tv, g);
            tv.addTextChangedListener(g);
            g.remember(tv.getText());
            if (isHidden(tv.getContext())) g.show(true);
        }
    }

    private static final class Guard implements TextWatcher {
        final TextView tv;
        String raw = "";
        boolean busy;
        Guard(TextView tv) { this.tv = tv; }

        void remember(CharSequence s) { raw = s == null ? "" : s.toString(); }

        void show(boolean hidden) {
            busy = true;
            try { tv.setText(hidden ? mask(raw) : raw); } finally { busy = false; }
        }

        @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
        @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
        @Override public void afterTextChanged(Editable e) {
            if (busy) return;
            raw = e.toString();                                   // new real text from the screen
            if (isHidden(tv.getContext())) show(true);
        }
    }

    // ── the eye ───────────────────────────────────────────────────

    /** Makes an ImageView the eye: shows the right icon for the saved state and toggles every balance when tapped. */
    public static void bindEye(ImageView eye) {
        if (eye == null) return;
        synchronized (EYES) { EYES.put(eye, Boolean.TRUE); }
        paintEye(eye, isHidden(eye.getContext()));
        eye.setClickable(true);
        eye.setContentDescription(isHidden(eye.getContext()) ? "Show balances" : "Hide balances");
        eye.setOnClickListener(v -> {
            toggle(v.getContext());
            paintEye((ImageView) v, isHidden(v.getContext()));     // this eye at once, whatever the others do
        });
    }

    /**
     * Plain icons, never the pressed/selected selectors: those fade between states, and swapping one in during the tap
     * left its fade stuck at fully transparent, so the eye vanished when clicked. A plain vector has no fade to stall.
     */
    private static void paintEye(ImageView eye, boolean hidden) {
        eye.setImageResource(hidden ? R.drawable.icon_visibility_off_twotone_line : R.drawable.icon_visibility_twotone_line);
        eye.setImageAlpha(255);
        eye.setAlpha(1f);
        eye.setVisibility(android.view.View.VISIBLE);
        eye.setContentDescription(hidden ? "Show balances" : "Hide balances");
        eye.invalidate();
    }

    private static void repaintAll(boolean hidden) {
        List<Guard> gs;
        List<ImageView> es;
        synchronized (GUARDS) { gs = new ArrayList<>(GUARDS.values()); }
        synchronized (EYES) { es = new ArrayList<>(EYES.keySet()); }
        for (Guard g : gs) g.show(hidden);
        for (ImageView e : es) paintEye(e, hidden);
    }
}
