package com.harvana.app.utils;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ImageSpan;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

/** Puts the app's own icons where text used to carry an emoji or a symbol (a check, a calendar, a warning). */
public final class Icons {
    private Icons() {}

    /** The icon before the text, sized to the text. */
    public static void start(TextView t, int res, int sizeDp) {
        Drawable d = ContextCompat.getDrawable(t.getContext(), res);
        if (d == null) return;
        d = d.mutate();
        int px = dp(t.getContext(), sizeDp);
        d.setBounds(0, 0, px, px);
        t.setCompoundDrawablesRelative(d, null, null, null);
        t.setCompoundDrawablePadding(dp(t.getContext(), 6));
    }

    /**
     * A progress step: its label with a real check icon in front (filled in success colour when done, an open circle
     * otherwise). Replaces the old text ticks, which also wiped the step's label.
     */
    public static void step(TextView t, String label, boolean done) {
        t.setText(label);
        start(t, done ? com.harvana.app.R.drawable.ic_check_circle_on : com.harvana.app.R.drawable.ic_step_open_line, 20);
        android.graphics.drawable.Drawable d = t.getCompoundDrawablesRelative()[0];
        if (d != null) d.setTint(done ? UiColors.success() : UiColors.textTertiary());
        t.setTextColor(done ? UiColors.textPrimary() : UiColors.textTertiary());
        t.setCompoundDrawablePadding(Math.round(10 * t.getResources().getDisplayMetrics().density));
    }

    /** A check (or cross) icon in place of a text tick: the view shows the icon only. */
    public static void check(TextView t, boolean ok) {
        t.setText("");
        start(t, ok ? com.harvana.app.R.drawable.ic_check_circle_on : com.harvana.app.R.drawable.ic_dismiss_circle_line, 18);
        android.graphics.drawable.Drawable d = t.getCompoundDrawablesRelative()[0];
        if (d != null) d.setTint(ok ? UiColors.success() : UiColors.error());
        t.setCompoundDrawablePadding(0);
    }

    /** A label with a check icon in front (a ticked item in a list). */
    public static void checked(TextView t, CharSequence label) {
        t.setText(label);
        start(t, com.harvana.app.R.drawable.ic_check_circle_on, 16);
        android.graphics.drawable.Drawable d = t.getCompoundDrawablesRelative()[0];
        if (d != null) d.setTint(UiColors.success());
    }

    /** Text with each marker character drawn as an icon instead, e.g. a check and a cross in a list of rights. */
    public static CharSequence inline(Context c, String text, int sizeDp, String[] markers, int[] icons) {
        SpannableString s = new SpannableString(text);
        int px = dp(c, sizeDp);
        for (int m = 0; m < markers.length; m++) {
            for (int i = text.indexOf(markers[m]); i >= 0; i = text.indexOf(markers[m], i + markers[m].length())) {
                Drawable d = ContextCompat.getDrawable(c, icons[m]);
                if (d == null) continue;
                d = d.mutate();
                d.setBounds(0, 0, px, px);
                s.setSpan(new ImageSpan(d, ImageSpan.ALIGN_BOTTOM), i, i + markers[m].length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        return s;
    }

    private static int dp(Context c, int v) { return Math.round(v * c.getResources().getDisplayMetrics().density); }
}
