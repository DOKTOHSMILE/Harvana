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
        Context c = palette();
        return ContextCompat.getColor(c, res);
    }

    /**
     * Colors follow the theme chosen in Settings, not whatever night flag the application context
     * happens to be holding. A sheet built from the application context was painting night text
     * on a day surface, so rows looked blank.
     */
    private static Context palette() {
        Context app = SketchApplication.getContext();
        if (app == null) return app;
        android.content.res.Configuration cfg = new android.content.res.Configuration(app.getResources().getConfiguration());
        int night = ThemeManager.isNightNow(app)
                ? android.content.res.Configuration.UI_MODE_NIGHT_YES
                : android.content.res.Configuration.UI_MODE_NIGHT_NO;
        int mask = android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        if ((cfg.uiMode & mask) == night) return app;
        cfg.uiMode = (cfg.uiMode & ~mask) | night;
        return app.createConfigurationContext(cfg);
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
