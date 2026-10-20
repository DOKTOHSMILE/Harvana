package com.harvana.app.utils;

import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.widget.TextView;

import androidx.appcompat.content.res.AppCompatResources;

/**
 * Puts one of the app's empty-state illustrations (res/drawable/illus_*) above an empty-list line, centred. The
 * pictures use the theme's colours, so they follow light and dark. Calling it again swaps the picture; 0 removes it.
 */
public final class EmptyState {
    private EmptyState() {}

    public static TextView illustrate(TextView line, int illustrationRes) {
        if (line == null) return null;
        Drawable d = illustrationRes == 0 ? null : AppCompatResources.getDrawable(line.getContext(), illustrationRes);
        line.setCompoundDrawablesRelativeWithIntrinsicBounds(null, d, null, null);
        line.setCompoundDrawablePadding(Math.round(12 * line.getResources().getDisplayMetrics().density));
        line.setGravity(Gravity.CENTER);
        if (d != null && line.getPaddingTop() < dp(line, 16)) {
            line.setPadding(line.getPaddingLeft(), dp(line, 16), line.getPaddingRight(), Math.max(line.getPaddingBottom(), dp(line, 16)));
        }
        return line;
    }

    private static int dp(TextView v, int x) { return Math.round(x * v.getResources().getDisplayMetrics().density); }
}
