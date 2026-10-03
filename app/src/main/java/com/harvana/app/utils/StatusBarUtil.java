package com.harvana.app.utils;

import android.app.Activity;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.Window;

import androidx.core.graphics.ColorUtils;
import androidx.core.view.WindowCompat;

/**
 * Makes the system status bar the same color as the action bar / toolbar.
 * Toolbars are filled with ?attr/appBarColor, so that attribute is what is resolved here - it follows the
 * active theme (light, dark, P2P) automatically. Icon contrast is chosen from the color's luminance.
 */
public final class StatusBarUtil {

    private StatusBarUtil() {}

    public static void matchToolbar(Activity activity) {
        if (activity == null) return;
        TypedValue tv = new TypedValue();
        if (!activity.getTheme().resolveAttribute(com.harvana.app.R.attr.appBarColor, tv, true)) return;
        int color = tv.data;
        Window w = activity.getWindow();
        w.setStatusBarColor(color);
        boolean lightBar = ColorUtils.calculateLuminance(color) > 0.4;
        WindowCompat.getInsetsController(w, w.getDecorView()).setAppearanceLightStatusBars(lightBar);
    }
}
