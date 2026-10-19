package com.harvana.app.tx;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * The addresses this phone has sent to, newest first (owner, 19 Oct 2026, from the HOT Wallet "Enter receiver" screen).
 * Saved when the network accepts a send (DuplicateSendGuard.record); kept exactly as typed, since some chains'
 * addresses are case-sensitive.
 */
public final class RecentRecipients {
    private RecentRecipients() {}

    static final int MAX = 12;
    private static final String PREFS = "recent_recipients", KEY = "list";

    public static void remember(Context c, String to, String coin) {
        if (c == null || to == null || to.trim().isEmpty()) return;
        String t = to.trim();
        try {
            JSONArray next = new JSONArray().put(new JSONObject().put("to", t).put("coin", coin == null ? "" : coin).put("at", System.currentTimeMillis()));
            JSONArray old = read(c);
            for (int i = 0; i < old.length() && next.length() < MAX; i++) {
                JSONObject o = old.optJSONObject(i);
                if (o != null && !o.optString("to").equals(t)) next.put(o);
            }
            prefs(c).edit().putString(KEY, next.toString()).apply();
        } catch (Exception ignored) { }
    }

    /** Recent addresses that fit this coin: a Harvana address for Harvana coins, otherwise ones used with the same coin. */
    public static List<String> forCoin(Context c, String coin, boolean harvanaAddress) {
        List<String> out = new ArrayList<>();
        JSONArray all = read(c);
        for (int i = 0; i < all.length(); i++) {
            JSONObject o = all.optJSONObject(i);
            if (o == null) continue;
            String to = o.optString("to");
            boolean fits = harvanaAddress ? to.startsWith("HV0x") : coin != null && coin.equalsIgnoreCase(o.optString("coin"));
            if (fits && !out.contains(to)) out.add(to);
        }
        return out;
    }

    private static JSONArray read(Context c) {
        try { return new JSONArray(prefs(c).getString(KEY, "[]")); } catch (Exception e) { return new JSONArray(); }
    }

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
