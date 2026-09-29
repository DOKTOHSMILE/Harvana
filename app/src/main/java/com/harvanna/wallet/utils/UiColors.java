package com.harvanna.wallet.utils;

import android.content.Context;

import androidx.core.content.ContextCompat;

import com.harvanna.wallet.R;
import com.harvanna.wallet.SketchApplication;

/**
 * Theme colors for code that builds UI programmatically. Resolves the palette tokens
 * (values/colors.xml and values-night/colors.xml) so day/night follow the system setting
 * instead of hardcoded hex values.
 */
public final class UiColors {

    private UiColors() {}

    private static int get(int res) {
        Context c = SketchApplication.getContext();
        return ContextCompat.getColor(c, res);
    }

    public static int surface()       { return get(R.color.colorSurface); }
    public static int background()    { return get(R.color.colorBackground); }
    public static int accent()        { return get(R.color.colorAccent); }
    public static int onAccent()      { return get(R.color.colorTextOnAccent); }
    public static int textPrimary()   { return get(R.color.colorTextPrimary); }
    public static int textSecondary() { return get(R.color.colorTextSecondary); }
    public static int textTertiary()  { return get(R.color.colorTextTertiary); }
    public static int success()       { return get(R.color.colorSuccess); }
    public static int warning()       { return get(R.color.colorWarning); }
    public static int error()         { return get(R.color.colorError); }
}
