package com.harvana.app.utils;

import android.content.Context;

import androidx.core.content.ContextCompat;

import com.harvana.app.R;
import com.harvana.app.SketchApplication;

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
    public static int surfaceAlt()    { return get(R.color.colorSurfaceAlt); }
    public static int accent()        { return get(R.color.colorAccent); }
    public static int onAccent()      { return get(R.color.colorTextOnAccent); }
    public static int textPrimary()   { return get(R.color.colorTextPrimary); }
    public static int textSecondary() { return get(R.color.colorTextSecondary); }
    public static int textTertiary()  { return get(R.color.colorTextTertiary); }
    /** Ink or white, whichever reads on this fill. */
    public static int onFill(int fill) {
        double r = ((fill >> 16) & 0xFF) / 255.0, g = ((fill >> 8) & 0xFF) / 255.0, b = (fill & 0xFF) / 255.0;
        double lum = 0.2126 * r + 0.7152 * g + 0.0722 * b;
        return lum > 0.45 ? 0xFF053D3A : 0xFFFFFFFF;
    }
    public static int success()       { return get(R.color.colorSuccess); }
    public static int warning()       { return get(R.color.colorWarning); }
    public static int error()         { return get(R.color.colorError); }
}
