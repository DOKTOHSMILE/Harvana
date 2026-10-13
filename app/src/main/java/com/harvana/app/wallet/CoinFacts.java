package com.harvana.app.wallet;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;

/**
 * A coin's description, read from CoinGecko by its id and kept on the phone for a week, so no coin's facts have to be
 * written into the app. Delivers on the main thread; null when there is none.
 */
public final class CoinFacts {
    private CoinFacts() {}

    public interface Got { void got(String about); }

    private static final String PREFS = "coin_facts";
    private static final long KEEP_MS = 7L * 24 * 60 * 60 * 1000;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public static void about(Context c, String geckoId, Got cb) {
        if (c == null || geckoId == null || geckoId.isEmpty()) { MAIN.post(() -> cb.got(null)); return; }
        Context app = c.getApplicationContext();
        android.content.SharedPreferences p = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String saved = p.getString("about:" + geckoId, null);
        if (saved != null && System.currentTimeMillis() - p.getLong("at:" + geckoId, 0) < KEEP_MS) { MAIN.post(() -> cb.got(saved)); return; }
        new Thread(() -> {
            String text = null;
            HttpURLConnection h = null;
            try {
                h = (HttpURLConnection) new URL("https://api.coingecko.com/api/v3/coins/" + java.net.URLEncoder.encode(geckoId, "UTF-8")
                        + "?localization=false&tickers=false&market_data=false&community_data=false&developer_data=false").openConnection();
                h.setConnectTimeout(8000);
                h.setReadTimeout(10000);
                h.setRequestProperty("Accept", "application/json");
                if (h.getResponseCode() == 200) {
                    java.io.InputStream in = h.getInputStream();
                    java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream();
                    byte[] b = new byte[8192];
                    for (int n; (n = in.read(b)) > 0; ) buf.write(b, 0, n);
                    JSONObject j = new JSONObject(buf.toString("UTF-8"));
                    JSONObject d = j.optJSONObject("description");
                    String en = d == null ? "" : d.optString("en", "");
                    text = firstParagraph(android.text.Html.fromHtml(en, android.text.Html.FROM_HTML_MODE_LEGACY).toString());
                    if (text != null && !text.isEmpty()) p.edit().putString("about:" + geckoId, text).putLong("at:" + geckoId, System.currentTimeMillis()).apply();
                }
            } catch (Exception ignored) {
            } finally {
                if (h != null) h.disconnect();
            }
            final String out = text == null || text.isEmpty() ? saved : text;
            MAIN.post(() -> cb.got(out));
        }, "coin-facts").start();
    }

    private static String firstParagraph(String s) {
        if (s == null) return null;
        String t = s.trim();
        int cut = t.indexOf("\n\n");
        if (cut > 0) t = t.substring(0, cut);
        if (t.length() > 900) {
            int dot = t.lastIndexOf(". ", 900);
            t = dot > 200 ? t.substring(0, dot + 1) : t.substring(0, 900) + "…";
        }
        return t.trim();
    }
}
