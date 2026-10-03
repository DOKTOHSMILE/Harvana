package com.harvana.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.harvana.app.sync.HarvanaNodeClient;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * The native coin's ticker and name, taken from the node instead of being hardcoded.
 *
 * Source: GET /api/v1/chain/info -> "ticker" (the node publishes "HARV"). The node does not publish a coin
 * NAME or decimals today; if it adds "coinName" (or "name") this class picks it up, otherwise the name defaults to
 * "Harvana". The last good values are cached, and the defaults are HARV / Harvana, so the UI is never blank or
 * stale-branded when offline.
 *
 * Use {@link #ticker()} / {@link #name()} wherever the coin is shown. Amounts are still 1e8 raw units.
 */
public final class CoinInfo {

    private static final String TAG = "CoinInfo";
    private static final String PREFS = "coin_info";
    private static final String DEFAULT_TICKER = "HARV";
    private static final String DEFAULT_NAME = "Harvana";
    private static final Pattern TICKER_OK = Pattern.compile("^[A-Z0-9]{2,8}$");

    private static volatile String ticker = DEFAULT_TICKER;
    private static volatile String name = DEFAULT_NAME;

    private CoinInfo() {}

    public static String ticker() { return ticker; }

    public static String name() { return name; }

    /** Load the cached values. Call once at startup, before any screen. */
    public static void init(Context context) {
        try {
            SharedPreferences p = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            String t = p.getString("ticker", DEFAULT_TICKER);
            String n = p.getString("name", DEFAULT_NAME);
            if (t != null && TICKER_OK.matcher(t).matches()) ticker = t;
            if (n != null && !n.trim().isEmpty()) name = n.trim();
        } catch (Exception e) {
            Log.w(TAG, "init failed; using defaults", e);
        }
    }

    /** Ask the node for the current values (background thread; failures keep the cached values). */
    public static void refresh(final Context context) {
        new Thread(() -> {
            HttpURLConnection c = null;
            try {
                c = (HttpURLConnection) new URL(HarvanaNodeClient.BASE + "/api/v1/chain/info").openConnection();
                c.setConnectTimeout(10_000);
                c.setReadTimeout(10_000);
                int code = c.getResponseCode();
                if (code < 200 || code >= 300) return;
                StringBuilder sb = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                }
                JSONObject json = new JSONObject(sb.toString());
                String t = json.optString("ticker", "").trim().toUpperCase();
                String n = json.optString("coinName", json.optString("name", "")).trim();
                SharedPreferences.Editor ed = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
                if (TICKER_OK.matcher(t).matches()) { ticker = t; ed.putString("ticker", t); }
                if (!n.isEmpty()) { name = n; ed.putString("name", n); }
                ed.apply();
            } catch (Exception e) {
                Log.w(TAG, "refresh failed; keeping cached values: " + e.getMessage());
            } finally {
                if (c != null) c.disconnect();
            }
        }, "coin-info").start();
    }
}
