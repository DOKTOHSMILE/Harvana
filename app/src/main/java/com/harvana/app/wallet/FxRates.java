package com.harvana.app.wallet;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * What one US dollar is worth in the currencies the app can show. One free call, kept for twelve hours,
 * so every coin can be added up in naira, pounds, cedi and the rest without asking again for each coin.
 */
public final class FxRates {
    private FxRates() {}

    private static final long FRESH_MS = 12L * 60L * 60L * 1000L;
    private static final String PREFS = "fx_rates";
    private static final String[] CODES = {"USD", "NGN", "GBP", "EUR", "GHS", "KES", "ZAR"};
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static volatile boolean inFlight;

    public static String symbol(Context c) {
        if (c == null) return "₦";
        return c.getSharedPreferences("currencysp", Context.MODE_PRIVATE).getString("currency", "₦");
    }

    public static void save(Context c, String symbol) {
        if (c == null || symbol == null) return;
        c.getSharedPreferences("currencysp", Context.MODE_PRIVATE).edit().putString("currency", symbol).apply();
    }

    /** Units of {@code code} for one US dollar. One for USD. NaN when this phone has no rate yet. */
    public static double perUsd(Context c, String code) {
        if (code == null || "USD".equals(code)) return 1d;
        if (c != null) read(c);
        SharedPreferences p = c == null ? null : c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (p == null || !p.contains(code)) return Double.NaN;
        return Double.longBitsToDouble(p.getLong(code, 0));
    }

    public static void load(Context c, Runnable onMain) {
        if (c == null) { if (onMain != null) MAIN.post(onMain); return; }
        Context app = c.getApplicationContext();
        read(app);
        long at = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong("at", 0);
        if (at > 0 && System.currentTimeMillis() - at < FRESH_MS && !Double.isNaN(perUsd(app, "NGN"))) {
            if (onMain != null) MAIN.post(onMain);
            return;
        }
        if (inFlight) { if (onMain != null) MAIN.postDelayed(onMain, 400); return; }
        inFlight = true;
        new Thread(() -> {
            boolean ok = fetch(app);
            inFlight = false;
            if (onMain != null) MAIN.post(onMain);
            if (!ok) { /* the last saved rates, if any, stay */ }
        }, "fx-rates").start();
    }

    /** The holding, in the currency this person chose. "…" until a rate exists for anything other than dollars. */
    public static String money(Context c, double usd) {
        String symbol = symbol(c);
        String code = FiatValueHelper.symbolToCurrencyCode(symbol);
        double rate = perUsd(c, code);
        if (Double.isNaN(rate)) return "…";
        return format(symbol, usd * rate);
    }

    /** Same number {@link #money} shows, or NaN when the rate is not here yet. */
    public static double fiat(Context c, double usd) {
        String code = FiatValueHelper.symbolToCurrencyCode(symbol(c));
        double rate = perUsd(c, code);
        if (Double.isNaN(rate)) return Double.NaN;
        return usd * rate;
    }

    public static String format(String symbol, double v) {
        DecimalFormatSymbols us = DecimalFormatSymbols.getInstance(Locale.US);
        DecimalFormat fmt = Math.abs(v) > 0 && Math.abs(v) < 0.01
                ? new DecimalFormat("0.0000", us)
                : new DecimalFormat("#,##0.00", us);
        return (symbol == null ? "" : symbol) + fmt.format(v);
    }

    /** "1 USD = ₦1,600.00", or an empty string when that rate is not on the phone yet. */
    public static String rateLine(Context c, String code) {
        if ("USD".equals(code)) return "1 USD";
        double rate = perUsd(c, code);
        if (Double.isNaN(rate) || rate <= 0) return "";
        String symbol = symbolFor(code);
        return "1 USD = " + format(symbol, rate);
    }

    public static String symbolFor(String code) {
        switch (code == null ? "" : code) {
            case "USD": return "$";
            case "GBP": return "£";
            case "EUR": return "€";
            case "GHS": return "GH₵";
            case "KES": return "KSh";
            case "ZAR": return "R";
            default: return "₦";
        }
    }

    private static void read(Context c) { /* rates live in prefs; perUsd reads them */ }

    private static boolean fetch(Context app) {
        String body = get("https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/usd.min.json");
        if (body != null && store(app, body, "usd")) return true;
        body = get("https://open.er-api.com/v6/latest/USD");
        return body != null && store(app, body, "rates");
    }

    private static boolean store(Context app, String body, String objectKey) {
        try {
            JSONObject root = new JSONObject(body);
            JSONObject rates = root.optJSONObject(objectKey);
            if (rates == null) return false;
            SharedPreferences.Editor ed = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
            int n = 0;
            for (String code : CODES) {
                if ("USD".equals(code)) continue;
                double v = rates.optDouble(code.toLowerCase(Locale.US), rates.optDouble(code, Double.NaN));
                if (Double.isNaN(v) || v <= 0) continue;
                ed.putLong(code, Double.doubleToRawLongBits(v));
                n++;
            }
            if (n == 0) return false;
            ed.putLong("at", System.currentTimeMillis()).apply();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static String get(String url) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(12000);
            conn.setReadTimeout(12000);
            conn.setRequestProperty("User-Agent", "Harvana");
            if (conn.getResponseCode() != 200) return null;
            InputStream in = conn.getInputStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) >= 0) out.write(buf, 0, n);
            in.close();
            return out.toString("UTF-8");
        } catch (Exception e) {
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}
