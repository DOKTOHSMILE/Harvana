package com.harvana.app.learn;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * What this reader has engaged with, kept on the phone only (nothing about it is sent anywhere). It feeds {@link LearnRanker}.
 * Each interaction adds weight to the post's category and author; weights fade with a half-life of 10 days, so the feed follows
 * what the reader cares about now. It also remembers which posts were shown and which were opened, so the feed stops repeating.
 */
public final class LearnInterest implements LearnRanker.Profile {

    public static final double OPEN = 2, READ = 2, FIRE = 4, COMMENT = 4, SHARE = 3, GIFT = 5, UNFIRE = -4;

    private static final String PREFS = "learn_interest";
    private static final double HALF_LIFE_DAYS = 10.0;
    private static final int MAX_POSTS = 400;

    private static LearnInterest instance;

    public static synchronized LearnInterest get(Context ctx) {
        if (instance == null) instance = new LearnInterest(ctx.getApplicationContext());
        return instance;
    }

    private final SharedPreferences prefs;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Map<String, double[]> cats = new HashMap<>();      // name -> {weight, updatedAt}
    private final Map<String, double[]> authors = new HashMap<>();
    private final Map<String, double[]> posts = new HashMap<>();     // id -> {impressions, opened (0/1), lastTouched}
    private final Set<String> shownThisSession = new HashSet<>();
    private boolean dirty;

    private LearnInterest(Context ctx) {
        prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        try {
            JSONObject o = new JSONObject(prefs.getString("data", "{}"));
            read(o.optJSONObject("c"), cats, 2);
            read(o.optJSONObject("a"), authors, 2);
            read(o.optJSONObject("p"), posts, 3);
        } catch (Exception ignored) { }
    }

    private static void read(JSONObject o, Map<String, double[]> into, int n) {
        if (o == null) return;
        for (Iterator<String> it = o.keys(); it.hasNext(); ) {
            String k = it.next();
            org.json.JSONArray a = o.optJSONArray(k);
            if (a == null || a.length() < n) continue;
            double[] v = new double[n];
            for (int i = 0; i < n; i++) v[i] = a.optDouble(i, 0);
            into.put(k, v);
        }
    }

    // ── Reading the profile ───────────────────────────────────────

    @Override public synchronized double category(String c) { return decayed(cats.get(c)); }
    @Override public synchronized double author(String a) { return decayed(authors.get(a)); }
    @Override public synchronized int impressions(String id) { double[] p = posts.get(id); return p == null ? 0 : (int) p[0]; }
    @Override public synchronized boolean opened(String id) { double[] p = posts.get(id); return p != null && p[1] > 0; }

    private static double decayed(double[] v) {
        if (v == null) return 0;
        double days = Math.max(0, System.currentTimeMillis() - v[1]) / 86_400_000.0;
        return Math.max(0, v[0] * Math.pow(0.5, days / HALF_LIFE_DAYS));
    }

    // ── Recording ─────────────────────────────────────────────────

    /** Adds weight for something the reader did with a post. */
    public synchronized void record(LearnPostBinder.Post post, double weight) {
        if (post == null) return;
        bump(cats, post.category, weight);
        bump(authors, post.authorUid, weight * 0.7);
        if (weight >= OPEN) {
            double[] p = posts.get(post.id);
            if (p == null) { p = new double[]{0, 0, 0}; posts.put(post.id, p); }
            p[1] = 1; p[2] = System.currentTimeMillis();
        }
        changed();
    }

    /** The post came on screen. Counted once per session, so scrolling back up does not count it again. */
    public synchronized void shown(String id) {
        if (id == null || id.isEmpty() || !shownThisSession.add(id)) return;
        double[] p = posts.get(id);
        if (p == null) { p = new double[]{0, 0, 0}; posts.put(id, p); }
        p[0] += 1; p[2] = System.currentTimeMillis();
        changed();
    }

    private static void bump(Map<String, double[]> m, String key, double w) {
        if (key == null || key.isEmpty()) return;
        double[] v = m.get(key);
        double now = System.currentTimeMillis();
        if (v == null) { v = new double[]{0, now}; m.put(key, v); }
        v[0] = Math.max(0, decayed(v) + w);
        v[1] = now;
    }

    private void changed() {
        dirty = true;
        main.removeCallbacks(saver);
        main.postDelayed(saver, 3000);
    }

    private final Runnable saver = this::flush;

    public synchronized void flush() {
        if (!dirty) return;
        dirty = false;
        try {
            if (posts.size() > MAX_POSTS) {            // forget the least recently touched
                java.util.List<Map.Entry<String, double[]>> all = new java.util.ArrayList<>(posts.entrySet());
                java.util.Collections.sort(all, (x, y) -> Double.compare(y.getValue()[2], x.getValue()[2]));
                posts.clear();
                for (int i = 0; i < MAX_POSTS; i++) posts.put(all.get(i).getKey(), all.get(i).getValue());
            }
            JSONObject o = new JSONObject();
            o.put("c", write(cats)); o.put("a", write(authors)); o.put("p", write(posts));
            prefs.edit().putString("data", o.toString()).apply();
        } catch (Exception ignored) { }
    }

    private static JSONObject write(Map<String, double[]> m) throws Exception {
        JSONObject o = new JSONObject();
        for (Map.Entry<String, double[]> e : m.entrySet()) {
            org.json.JSONArray a = new org.json.JSONArray();
            for (double d : e.getValue()) a.put(d);
            o.put(e.getKey(), a);
        }
        return o;
    }
}
