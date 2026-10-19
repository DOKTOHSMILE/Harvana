package com.harvana.app.tx;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.harvana.app.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * Before a send: the same amount of the same coin to the same wallet within the last 30 minutes asks once more
 * (owner, 19 Oct 2026). Sends are remembered on this phone only after the network accepted them.
 */
public final class DuplicateSendGuard {

    private DuplicateSendGuard() {}

    public static final long WINDOW_MS = 30L * 60_000L;
    private static final String PREFS = "duplicate_send", KEY = "recent";

    /** Runs {@code proceed} at once, or after the person confirms a likely duplicate. */
    public static void check(Activity a, String to, String coin, String amountText, Runnable proceed) {
        long last = lastSent(a, to, coin, amountText, System.currentTimeMillis());
        if (last <= 0 || a.isFinishing()) { proceed.run(); return; }
        com.google.android.material.bottomsheet.BottomSheetDialog d = new com.harvana.app.ui.AppSheet(a);
        View v = a.getLayoutInflater().inflate(R.layout.sheet_duplicate_send, null);
        d.setContentView(v);
        long mins = Math.max(1, (System.currentTimeMillis() - last + 59_999L) / 60_000L);
        ((TextView) v.findViewById(R.id.dup_line)).setText("Same amount to the same wallet " + mins + (mins == 1 ? " minute" : " minutes") + " ago.");
        LinearLayout rows = v.findViewById(R.id.dup_rows);
        rows.addView(new com.harvana.app.utils.InfoTable(a)
                .row("To", to)
                .row("Amount", amountText + " " + coin)
                .row("Last sent", java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(new java.util.Date(last)))
                .view());
        v.findViewById(R.id.dup_back).setOnClickListener(x -> d.dismiss());
        v.findViewById(R.id.dup_confirm).setOnClickListener(x -> { d.dismiss(); proceed.run(); });
        d.show();
    }

    /** Remembers an accepted send. */
    public static void record(Context c, String to, String coin, String amountText) {
        RecentRecipients.remember(c, to, coin);
        try {
            long now = System.currentTimeMillis();
            JSONArray kept = new JSONArray();
            for (JSONObject o : recent(c, now)) kept.put(o);
            kept.put(new JSONObject().put("to", norm(to)).put("coin", norm(coin)).put("amount", amount(amountText)).put("at", now));
            prefs(c).edit().putString(KEY, kept.toString()).apply();
        } catch (Exception ignored) { }
    }

    /** When the same send was last made within the window; 0 when not. */
    static long lastSent(Context c, String to, String coin, String amountText, long now) {
        String t = norm(to), k = norm(coin), m = amount(amountText);
        long last = 0;
        for (JSONObject o : recent(c, now)) {
            if (t.equals(o.optString("to")) && k.equals(o.optString("coin")) && m.equals(o.optString("amount"))) last = Math.max(last, o.optLong("at"));
        }
        return last;
    }

    private static java.util.List<JSONObject> recent(Context c, long now) {
        java.util.List<JSONObject> out = new java.util.ArrayList<>();
        try {
            JSONArray a = new JSONArray(prefs(c).getString(KEY, "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.optJSONObject(i);
                if (o != null && now - o.optLong("at") < WINDOW_MS) out.add(o);
            }
        } catch (Exception ignored) { }
        return out;
    }

    static String norm(String s) { return s == null ? "" : s.trim().toLowerCase(Locale.ROOT); }

    /** "1.50" and "1.5" are the same amount. */
    static String amount(String text) {
        try { return new BigDecimal(text.trim().replace(",", "")).stripTrailingZeros().toPlainString(); }
        catch (Exception e) { return norm(text); }
    }

    private static SharedPreferences prefs(Context c) { return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE); }
}
