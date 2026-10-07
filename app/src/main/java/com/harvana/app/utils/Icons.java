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
