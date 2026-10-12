package com.harvana.app.web3;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

/** Sites the person connected (per EVM account) and recently opened sites. Stored on this phone only. */
public final class Web3Sites {
    private Web3Sites() {}

    private static final String PREFS = "web3_sites";

    private static SharedPreferences sp(Context c) { return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE); }

    public static boolean connected(Context c, String account, String origin) {
        if (account == null || origin == null) return false;
        return sp(c).getStringSet("conn_" + account.toLowerCase(), new HashSet<>()).contains(origin);
    }

    public static void connect(Context c, String account, String origin) {
        Set<String> s = new HashSet<>(sp(c).getStringSet("conn_" + account.toLowerCase(), new HashSet<>()));
        s.add(origin);
        sp(c).edit().putStringSet("conn_" + account.toLowerCase(), s).apply();
    }

    public static void disconnect(Context c, String account, String origin) {
        Set<String> s = new HashSet<>(sp(c).getStringSet("conn_" + account.toLowerCase(), new HashSet<>()));
        s.remove(origin);
        sp(c).edit().putStringSet("conn_" + account.toLowerCase(), s).apply();
    }

    public static Set<String> connectedSites(Context c, String account) {
        return new HashSet<>(sp(c).getStringSet("conn_" + account.toLowerCase(), new HashSet<>()));
    }

    /** The chain the browser last used ("ETH", "BNB", "POL", "BASE"). */
    public static String chain(Context c) { return sp(c).getString("chain", "ETH"); }
    public static void setChain(Context c, String key) { sp(c).edit().putString("chain", key).apply(); }

    /** Whether the browser's network chip shows Harvana (the Harvana wallet) instead of an EVM chain. */
    public static boolean showHarvana(Context c) { return sp(c).getBoolean("show_harvana", true); }
    public static void setShowHarvana(Context c, boolean on) { sp(c).edit().putBoolean("show_harvana", on).apply(); }

    public static void remember(Context c, String url, String title) {
        try {
            JSONArray old = recent(c), out = new JSONArray();
            out.put(new JSONObject().put("url", url).put("title", title == null ? "" : title).put("at", System.currentTimeMillis()));
            for (int i = 0; i < old.length() && out.length() < 20; i++) {
                JSONObject o = old.optJSONObject(i);
                if (o != null && !url.equals(o.optString("url"))) out.put(o);
            }
            sp(c).edit().putString("recent", out.toString()).apply();
        } catch (Exception ignored) { }
    }

    public static JSONArray recent(Context c) {
        try { return new JSONArray(sp(c).getString("recent", "[]")); } catch (Exception e) { return new JSONArray(); }
    }

    public static void clearRecent(Context c) { sp(c).edit().remove("recent").apply(); }
}
