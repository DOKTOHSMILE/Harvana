package com.harvanna.app.utils;

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

    private static final String PREF = "harvanna_theme_prefs";
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

    /**
     * Why: application-context resources follow the PHONE's dark mode, while screens follow the app's own
     * choice. Anything inflated or coloured through the application context (list rows, UiColors) then came
     * out in the other theme, which is why some cards looked dark on a light screen. This keeps the
     * application context's night flag equal to the screen the user is looking at.
     */
    public static void keepAppContextInSync(final android.app.Application app) {
        app.registerActivityLifecycleCallbacks(new android.app.Application.ActivityLifecycleCallbacks() {
            private void sync(android.app.Activity a) {
                int mask = android.content.res.Configuration.UI_MODE_NIGHT_MASK;
                int night = a.getResources().getConfiguration().uiMode & mask;
                android.content.res.Resources r = app.getResources();
                android.content.res.Configuration c = new android.content.res.Configuration(r.getConfiguration());
                if ((c.uiMode & mask) != night) {
                    c.uiMode = (c.uiMode & ~mask) | night;
                    r.updateConfiguration(c, r.getDisplayMetrics());
                }
            }
            @Override public void onActivityCreated(android.app.Activity a, android.os.Bundle b) { sync(a); }
            @Override public void onActivityStarted(android.app.Activity a) { sync(a); }
            @Override public void onActivityResumed(android.app.Activity a) { sync(a); }
            @Override public void onActivityPaused(android.app.Activity a) { }
            @Override public void onActivityStopped(android.app.Activity a) { }
            @Override public void onActivitySaveInstanceState(android.app.Activity a, android.os.Bundle b) { }
            @Override public void onActivityDestroyed(android.app.Activity a) { }
        });
    }

    public static String getMode(Context context) {
        return context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY_MODE, "system");
    }

    /** "Follow system", "Light" or "Dark", for showing in settings. */
    public static String label(Context context) {
        String m = getMode(context);
        return "light".equals(m) ? "Light" : "dark".equals(m) ? "Dark" : "Follow system";
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
