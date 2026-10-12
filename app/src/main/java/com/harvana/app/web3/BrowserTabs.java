package com.harvana.app.web3;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * The browser's open tabs, kept on this phone: each tab's address and title, which one was on screen, and a small
 * snapshot of its page for the tab grid. The browser stays open behind the app when minimized; this brings the tabs
 * back if Android closed it meanwhile.
 */
public final class BrowserTabs {
    private BrowserTabs() {}

    private static final String PREFS = "browser_tabs";

    public static final class Saved {
        public final String id, url, title;
        public Saved(String id, String url, String title) { this.id = id; this.url = url == null ? "" : url; this.title = title == null ? "" : title; }
    }

    private static SharedPreferences sp(Context c) { return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE); }

    public static List<Saved> load(Context c) {
        List<Saved> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(sp(c).getString("tabs", "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.optJSONObject(i);
                if (o != null && !o.optString("id").isEmpty()) out.add(new Saved(o.optString("id"), o.optString("url"), o.optString("title")));
            }
        } catch (Exception ignored) { }
        return out;
    }

    public static int current(Context c) { return sp(c).getInt("current", 0); }

    /** How many tabs are open (for a "Tabs" button elsewhere in the app). */
    public static int count(Context c) { return load(c).size(); }

    public static void save(Context c, List<Saved> tabs, int current) {
        JSONArray a = new JSONArray();
        try {
            for (Saved t : tabs) a.put(new JSONObject().put("id", t.id).put("url", t.url).put("title", t.title));
        } catch (Exception ignored) { }
        sp(c).edit().putString("tabs", a.toString()).putInt("current", current).apply();
    }

    private static File dir(Context c) {
        File d = new File(c.getCacheDir(), "browser_tabs");
        if (!d.exists()) d.mkdirs();
        return d;
    }

    public static Bitmap shot(Context c, String id) {
        File f = new File(dir(c), id + ".jpg");
        return f.exists() ? BitmapFactory.decodeFile(f.getAbsolutePath()) : null;
    }

    public static void keepShot(Context c, String id, Bitmap b) {
        if (b == null) return;
        try (FileOutputStream out = new FileOutputStream(new File(dir(c), id + ".jpg"))) {
            b.compress(Bitmap.CompressFormat.JPEG, 70, out);
        } catch (Exception ignored) { }
    }

    public static void dropShot(Context c, String id) {
        File f = new File(dir(c), id + ".jpg");
        if (f.exists()) f.delete();
    }
}
