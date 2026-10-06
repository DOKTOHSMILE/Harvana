package com.harvana.app.utils;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.Window;

import androidx.appcompat.view.WindowCallbackWrapper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Counts how this person uses the app, on this phone only: screens opened, taps, time in the app, by day, weekday and hour.
 * Nothing is sent anywhere and no content is recorded, only counts. It feeds the My Activity screen.
 */
public final class ActivityTracker {
    private static final String PREFS = "app_activity";
    private static final int KEEP_DAYS = 60;
    private static SharedPreferences prefs;
    private static int started;
    private static long foregroundSince;
    private static long lastTapSave;
    private static int pendingTaps;

    private ActivityTracker() {}

    public static void install(Application app) {
        prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override public void onActivityStarted(Activity a) {
                if (started++ == 0) foregroundSince = System.currentTimeMillis();
                bump("screen_" + a.getClass().getSimpleName(), 1);
                interaction(1);
            }
            @Override public void onActivityResumed(Activity a) {
                Window w = a.getWindow();
                Window.Callback cb = w == null ? null : w.getCallback();
                if (cb != null && !(cb instanceof TapCounter)) w.setCallback(new TapCounter(cb));
            }
            @Override public void onActivityStopped(Activity a) {
                if (started > 0 && --started == 0) closeSession();
            }
            @Override public void onActivityCreated(Activity a, Bundle b) {}
            @Override public void onActivityPaused(Activity a) { flushTaps(); }
            @Override public void onActivitySaveInstanceState(Activity a, Bundle b) {}
            @Override public void onActivityDestroyed(Activity a) {}
        });
    }

    private static final class TapCounter extends WindowCallbackWrapper {
        TapCounter(Window.Callback d) { super(d); }
        @Override public boolean dispatchTouchEvent(MotionEvent e) {
            if (e.getActionMasked() == MotionEvent.ACTION_UP) {
                pendingTaps++;
                long now = System.currentTimeMillis();
                if (now - lastTapSave > 5000) flushTaps();
            }
            return super.dispatchTouchEvent(e);
        }
    }

    private static void flushTaps() {
        if (pendingTaps > 0) { int n = pendingTaps; pendingTaps = 0; lastTapSave = System.currentTimeMillis(); interaction(n); bump("taps", n); }
    }

    private static void closeSession() {
        flushTaps();
        if (foregroundSince <= 0) return;
        long ms = Math.min(System.currentTimeMillis() - foregroundSince, 4L * 3_600_000L);
        foregroundSince = 0;
        bump("ms_" + dayKey(System.currentTimeMillis()), (int) Math.min(ms / 1000, Integer.MAX_VALUE));   // seconds
        bump("sessions", 1);
    }

    private static String dayKey(long t) { return new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date(t)); }

    private static void interaction(int n) {
        if (prefs == null) return;
        Calendar c = Calendar.getInstance();
        SharedPreferences.Editor e = prefs.edit();
        e.putInt("day_" + dayKey(c.getTimeInMillis()), prefs.getInt("day_" + dayKey(c.getTimeInMillis()), 0) + n);
        e.putInt("hour_" + c.get(Calendar.HOUR_OF_DAY), prefs.getInt("hour_" + c.get(Calendar.HOUR_OF_DAY), 0) + n);
        e.putInt("wd_" + (c.get(Calendar.DAY_OF_WEEK) - 1), prefs.getInt("wd_" + (c.get(Calendar.DAY_OF_WEEK) - 1), 0) + n);
        if (prefs.getLong("since", 0) == 0) e.putLong("since", c.getTimeInMillis());
        e.apply();
    }

    private static void bump(String key, int n) {
        if (prefs != null) prefs.edit().putInt(key, prefs.getInt(key, 0) + n).apply();
    }

    // ── reading ──

    public static final class Summary {
        public int[] daily30 = new int[30];       // oldest first, last = today
        public int[] weekdays = new int[7];       // Sunday first
        public int[] hours = new int[24];         // local hours
        public int activeDays30, interactions30, sessions, taps;
        public long secondsToday, seconds30;
        public Map<String, Integer> screens = new TreeMap<>();
    }

    public static Summary summary(Context c) {
        Summary s = new Summary();
        SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long now = System.currentTimeMillis();
        for (int back = 29; back >= 0; back--) {
            String k = dayKey(now - back * 86_400_000L);
            int v = p.getInt("day_" + k, 0);
            s.daily30[29 - back] = v;
            s.interactions30 += v;
            if (v > 0) s.activeDays30++;
            long sec = p.getInt("ms_" + k, 0);
            s.seconds30 += sec;
            if (back == 0) s.secondsToday = sec + (foregroundSince > 0 ? (now - foregroundSince) / 1000 : 0);
        }
        for (int i = 0; i < 7; i++) s.weekdays[i] = p.getInt("wd_" + i, 0);
        for (int i = 0; i < 24; i++) s.hours[i] = p.getInt("hour_" + i, 0);
        s.sessions = p.getInt("sessions", 0);
        s.taps = p.getInt("taps", 0);
        for (Map.Entry<String, ?> en : p.getAll().entrySet())
            if (en.getKey().startsWith("screen_") && en.getValue() instanceof Integer)
                s.screens.put(en.getKey().substring(7), (Integer) en.getValue());
        prune(p, now);
        return s;
    }

    private static void prune(SharedPreferences p, long now) {
        String oldest = dayKey(now - KEEP_DAYS * 86_400_000L);
        SharedPreferences.Editor e = null;
        for (String k : p.getAll().keySet()) {
            String d = k.startsWith("day_") ? k.substring(4) : k.startsWith("ms_") ? k.substring(3) : null;
            if (d != null && d.compareTo(oldest) < 0) { if (e == null) e = p.edit(); e.remove(k); }
        }
        if (e != null) e.apply();
    }
}
