package com.harvanna.wallet.utils;

import android.content.Context;
import androidx.appcompat.app.AppCompatDelegate;

/**
 * FIXED: the previous version only ever offered "light" or "dark" as
 * hardcoded AppCompatDelegate modes, defaulting to forced dark — meaning
 * the app could never actually follow the phone's system theme setting,
 * regardless of what colors.xml/values-night said. MODE_NIGHT_FOLLOW_SYSTEM
 * is now the default, and is what "system" means below.
 */
public final class ThemeManager {

    private static final String PREF = "ajoo_theme_prefs";
    private static final String KEY_MODE = "theme_mode"; // "light" | "dark" | "system"

    private ThemeManager() {}

    /** Call once, as early as possible — ideally in a custom Application
     *  class's onCreate(), before any Activity's onCreate(). */
    public static void applySavedTheme(Context context) {
        String mode = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .getString(KEY_MODE, "system"); // defaults to following the phone's setting
        AppCompatDelegate.setDefaultNightMode(modeStringToDelegateMode(mode));
    }

    public static void setLightMode(Context context) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString(KEY_MODE, "light").apply();
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    }

    public static void setDarkMode(Context context) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString(KEY_MODE, "dark").apply();
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
    }

    public static void setSystemMode(Context context) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString(KEY_MODE, "system").apply();
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    public static String getMode(Context context) {
        return context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY_MODE, "system");
    }

    public static boolean isLightMode(Context context) {
        return "light".equals(getMode(context));
    }

    private static int modeStringToDelegateMode(String mode) {
        if ("light".equals(mode)) return AppCompatDelegate.MODE_NIGHT_NO;
        if ("dark".equals(mode)) return AppCompatDelegate.MODE_NIGHT_YES;
        return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
    }
}
