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
        return last(c, address, symbol, Long.MAX_VALUE);
    }

    /**
     * The last total only if it was worked out within {@code maxAgeMs}; NaN otherwise. A saved total is a bridge for the
     * seconds before the live one arrives, never a figure to show after the money in the wallet may have moved.
     */
    public static double last(Context c, String address, String symbol, long maxAgeMs) {
        if (c == null || address == null) return Double.NaN;
        SharedPreferences p = prefs(c);
        if (!String.valueOf(symbol).equals(p.getString(address + ":symbol", null))) return Double.NaN;
        long at = p.getLong(address + ":at", 0);
        if (at <= 0 || System.currentTimeMillis() - at > maxAgeMs) return Double.NaN;
        try { return Double.parseDouble(p.getString(address + ":fiat", "NaN")); } catch (Exception e) { return Double.NaN; }
    }

    /** How long Home may show the saved total while it works the live one out (seconds, as a rule). */
    public static final long HOME_MAX_AGE_MS = 24L * 3_600_000L;
    /** How long Assets may hold the saved total up while its rows are still reporting. */
    public static final long ASSETS_MAX_AGE_MS = 2L * 60_000L;

    public static void listen(Listener l) { if (l != null && !listeners.contains(l)) listeners.add(l); }
    public static void unlisten(Listener l) { listeners.remove(l); }

    private static final java.util.Set<String> refreshing = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());

    /**
     * Works the total out without the Assets tab: HARV at its price, every stable coin that is on, and every coin
     * shown on Assets, each in the chosen currency, then publishes it. Home calls this when the app opens, so the
     * card is right at once instead of waiting until Assets has been opened.
     */
    public static void refresh(Context ctx, String address) {
        refresh(ctx, address, 0);
    }

    private static void refresh(Context ctx, String address, int attempt) {
        if (ctx == null || address == null || address.isEmpty() || !refreshing.add(address)) return;
        final Context c = ctx.getApplicationContext();
        final String symbol = FxRates.symbol(c);
        final double[] parts = new double[3];             // HARV, stable coins, other coins
        final java.util.concurrent.atomic.AtomicInteger left = new java.util.concurrent.atomic.AtomicInteger(3);
        final boolean[] harvFailed = {false};
        final boolean[] stablesFailed = {false};
        final boolean[] pricesMissing = {false};
        final boolean[] othersDone = {false};
        final Runnable partDone = () -> {
            int remaining = left.decrementAndGet();
            // HARV and the Harvana coins are in (they are most of a wallet), but the other chains are still answering:
            // show that much now rather than nothing, and the full total when the rest arrives.
            if (remaining == 1 && !othersDone[0] && !harvFailed[0] && !stablesFailed[0]
                    && Double.isNaN(last(c, address, symbol, HOME_MAX_AGE_MS))) {   // only when nothing is shown yet: no jump from part to whole
                publish(c, address, symbol, parts[0] + parts[1]);
            }
            if (remaining > 0) return;
            refreshing.remove(address);
            if (pricesMissing[0] && attempt < 3) {
                main.postDelayed(() -> refresh(c, address, attempt + 1), 2000);
                return;
            }
            // A failed piece would publish a total that is too low and overwrite the real one.
            if (harvFailed[0] || stablesFailed[0]) return;
            publish(c, address, symbol, parts[0] + parts[1] + parts[2]);
        };
        FxRates.load(c, () -> {
            // 1. HARV
            WalletNodeBalanceHelper.load(address, (bal, avail, pin, pout, pending, err) -> {
                double harv = avail >= 0 ? avail : bal;
                if (err != null && !err.isEmpty()) { harvFailed[0] = true; partDone.run(); return; }
                if (harv <= 0) { partDone.run(); return; }
                FiatValueHelper.valueOfHarv(c, harv, symbol, (fiat, ok) -> {
                    if (ok && !Double.isNaN(fiat)) parts[0] = fiat;    // no HARV price yet: the other coins still add up
                    partDone.run();
                });
            });
            // 2. stable coins that are on
            com.harvana.app.husd.HusdClient.list(new com.harvana.app.husd.HusdClient.Cb() {
                @Override public void ok(org.json.JSONObject r) {
                    org.json.JSONArray arr = r.optJSONArray("assets");
                    java.util.List<String> on = new java.util.ArrayList<>();
                    if (arr != null) for (int i = 0; i < arr.length(); i++) {
                        org.json.JSONObject a = arr.optJSONObject(i);
                        if (a != null && a.optBoolean("active") && com.harvana.app.husd.Stable.isStable(a.optString("asset"))) on.add(a.optString("asset"));
                    }
                    if (on.isEmpty()) { partDone.run(); return; }
                    java.util.concurrent.atomic.AtomicInteger n = new java.util.concurrent.atomic.AtomicInteger(on.size());
                    final double[] sum = {0};
                    for (String code : on) {
                        com.harvana.app.husd.HusdClient.balance(code, address, new com.harvana.app.husd.HusdClient.Cb() {
                            @Override public void ok(org.json.JSONObject b) {
                                double amount = b.optLong("balanceRaw") / (double) com.harvana.app.husd.HusdClient.UNIT;
                                double perUsd = FxRates.perUsd(c, com.harvana.app.husd.Stable.currency(code));
                                double fiat = Double.isNaN(perUsd) || perUsd <= 0 ? Double.NaN : FxRates.fiat(c, amount / perUsd);
                                synchronized (sum) { if (!Double.isNaN(fiat)) sum[0] += fiat; }
                                if (n.decrementAndGet() == 0) { parts[1] = sum[0]; partDone.run(); }
                            }
                            @Override public void fail(String e) { if (n.decrementAndGet() == 0) { parts[1] = sum[0]; partDone.run(); } }
                        });
                    }
                }
                @Override public void fail(String e) { stablesFailed[0] = true; partDone.run(); }
            });
            // 3. other coins shown on Assets (network calls: off the main thread)
            AssetPrices.load(c, () -> new Thread(() -> {
                double sum = 0;
                try {
                    java.util.Map<String, String> addrs = CoinCatalog.addresses(c, address);
                    for (CoinCatalog.Coin coin : CoinCatalog.others()) {
                        if (!ChainAssetsSection.shown(c, coin.key)) continue;
                        String a = addrs.get(coin.key);
                        if (a == null || a.isEmpty()) continue;
                        AssetPrices.Quote q = AssetPrices.peek(c, coin.key);
                        Double held = AssetPrices.amount(ChainBalances.assetBalance(coin.key, a));
                        if (held == null || held <= 0) continue;
                        if (q == null || q.usd <= 0) { pricesMissing[0] = true; continue; }
                        double fiat = FxRates.fiat(c, held * q.usd);
                        if (!Double.isNaN(fiat)) sum += fiat;
                    }
                    for (CoinCatalog.Coin coin : CoinCatalog.addedCoins()) {           // chains and tokens the person added
                        String a = CustomAssets.address(c, address, coin.added);
                        if (a == null || a.isEmpty()) continue;
                        Double held = AssetPrices.amount(ChainBalances.assetBalance(coin.key, a));
                        if (held == null || held <= 0) continue;
                        AssetPrices.Quote q = AssetPrices.peek(c, coin.key);
                        if (q == null || q.usd <= 0) { pricesMissing[0] = true; continue; }
                        double fiat = FxRates.fiat(c, held * q.usd);
                        if (!Double.isNaN(fiat)) sum += fiat;
                    }
                } catch (Exception ignored) { }
                parts[2] = sum;
                main.post(() -> { othersDone[0] = true; partDone.run(); });
            }, "wallet-total").start());
        });
    }
}
