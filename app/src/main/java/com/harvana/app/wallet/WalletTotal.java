package com.harvana.app.wallet;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The wallet's total in the chosen currency, across every coin: the figure the Assets tab adds up. Assets publishes
 * it as its rows load; Home shows the same number, so the two screens never disagree. The last total is kept per
 * wallet so Home can show it at once on the next start.
 */
public final class WalletTotal {
    private WalletTotal() {}

    public interface Listener { void onTotal(String address, String symbol, double fiat); }

    private static final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();
    private static final Handler main = new Handler(Looper.getMainLooper());

    private static SharedPreferences prefs(Context c) { return c.getSharedPreferences("wallet_total", Context.MODE_PRIVATE); }

    public static void publish(Context c, String address, String symbol, double fiat) {
        if (c == null || address == null || address.isEmpty() || Double.isNaN(fiat)) return;
        prefs(c).edit().putString(address + ":symbol", symbol).putString(address + ":fiat", String.valueOf(fiat))
                .putLong(address + ":at", System.currentTimeMillis()).apply();
        main.post(() -> { for (Listener l : listeners) l.onTotal(address, symbol, fiat); });
    }

    /** The last total for this wallet in this currency, or NaN when there is none yet. */
    public static double last(Context c, String address, String symbol) {
        if (c == null || address == null) return Double.NaN;
        SharedPreferences p = prefs(c);
        if (!String.valueOf(symbol).equals(p.getString(address + ":symbol", null))) return Double.NaN;
        try { return Double.parseDouble(p.getString(address + ":fiat", "NaN")); } catch (Exception e) { return Double.NaN; }
    }

    public static void listen(Listener l) { if (l != null && !listeners.contains(l)) listeners.add(l); }
    public static void unlisten(Listener l) { listeners.remove(l); }
}
