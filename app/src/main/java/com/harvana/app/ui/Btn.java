package com.harvana.app.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.harvana.app.R;

/**
 * Buttons built in code. Every one is a MaterialButton in one of four styles, so they match the buttons in the layouts:
 * filled (the main action), tonal (a softer second action), outlined and text.
 */
public final class Btn {
    private Btn() {}

    private static int dp(Context c, int v) { return Math.round(v * c.getResources().getDisplayMetrics().density); }

    private static MaterialButton make(Context c, int styleAttr, String label, View.OnClickListener l) {
        MaterialButton b = new MaterialButton(c, null, styleAttr);
        b.setText(label);
        b.setAllCaps(false);
        b.setCornerRadius(dp(c, 14));
        b.setMinimumHeight(dp(c, 48));
        if (l != null) b.setOnClickListener(l);
        return b;
    }

    public static MaterialButton filled(Context c, String label, View.OnClickListener l) {
        MaterialButton b = make(c, com.google.android.material.R.attr.materialButtonStyle, label, l);
        b.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(c, R.color.colorAccent)));
        b.setTextColor(ContextCompat.getColor(c, R.color.colorTextOnAccent));
        return b;
    }

    public static MaterialButton tonal(Context c, String label, View.OnClickListener l) {
        MaterialButton b = make(c, com.google.android.material.R.attr.materialButtonStyle, label, l);
        b.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(c, R.color.colorAccentSoft)));
        b.setTextColor(ContextCompat.getColor(c, R.color.colorAccentStrong));
        return b;
    }

    public static MaterialButton outlined(Context c, String label, View.OnClickListener l) {
        MaterialButton b = make(c, com.google.android.material.R.attr.materialButtonOutlinedStyle, label, l);
        b.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(c, R.color.colorAccent)));
        b.setStrokeWidth(dp(c, 1));
        b.setTextColor(ContextCompat.getColor(c, R.color.colorAccentStrong));
        return b;
    }

    public static MaterialButton text(Context c, String label, View.OnClickListener l) {
        MaterialButton b = make(c, com.google.android.material.R.attr.materialButtonOutlinedStyle, label, l);
        b.setStrokeWidth(0);
        b.setBackgroundTintList(ColorStateList.valueOf(android.graphics.Color.TRANSPARENT));
        b.setTextColor(ContextCompat.getColor(c, R.color.colorAccentStrong));
        return b;
    }

    /** primary: filled; otherwise outlined. */
    public static MaterialButton of(Context c, String label, boolean primary, View.OnClickListener l) {
        return primary ? filled(c, label, l) : outlined(c, label, l);
    }

    public static MaterialButton fill(MaterialButton b, int fillColor, int textColor) {
        b.setBackgroundTintList(ColorStateList.valueOf(fillColor));
        b.setTextColor(textColor);
        return b;
    }

    public static ViewGroup.LayoutParams matchWidth(Context c, int heightDp) {
        return new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(c, heightDp));
    }
}
