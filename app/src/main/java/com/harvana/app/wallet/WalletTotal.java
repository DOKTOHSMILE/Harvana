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

    private static final java.util.Set<String> refreshing = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());

    /**
     * Works the total out without the Assets tab: HARV at its price, every stable coin that is on, and every coin
     * shown on Assets, each in the chosen currency, then publishes it. Home calls this when the app opens, so the
     * card is right at once instead of waiting until Assets has been opened.
     */
    public static void refresh(Context ctx, String address) {
        if (ctx == null || address == null || address.isEmpty() || !refreshing.add(address)) return;
        final Context c = ctx.getApplicationContext();
        final String symbol = FxRates.symbol(c);
        final double[] parts = new double[3];             // HARV, stable coins, other coins
        final java.util.concurrent.atomic.AtomicInteger left = new java.util.concurrent.atomic.AtomicInteger(3);
        final boolean[] harvFailed = {false};
        final Runnable partDone = () -> {
            if (left.decrementAndGet() > 0) return;
            refreshing.remove(address);
            // Without the HARV balance the sum would be too low; keep the last total rather than show a wrong one.
            if (!harvFailed[0]) publish(c, address, symbol, parts[0] + parts[1] + parts[2]);
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
                @Override public void fail(String e) { partDone.run(); }
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
                        if (q == null) continue;
                        Double held = AssetPrices.amount(ChainBalances.assetBalance(coin.key, a));
                        if (held == null || held <= 0) continue;
                        double fiat = FxRates.fiat(c, held * q.usd);
                        if (!Double.isNaN(fiat)) sum += fiat;
                    }
                    for (CoinCatalog.Coin coin : CoinCatalog.addedCoins()) {           // chains and tokens the person added
                        AssetPrices.Quote q = AssetPrices.peek(c, coin.key);
                        if (q == null) continue;
                        String a = CustomAssets.address(c, address, coin.added);
                        if (a == null || a.isEmpty()) continue;
                        Double held = AssetPrices.amount(ChainBalances.assetBalance(coin.key, a));
                        if (held == null || held <= 0) continue;
                        double fiat = FxRates.fiat(c, held * q.usd);
                        if (!Double.isNaN(fiat)) sum += fiat;
                    }
                } catch (Exception ignored) { }
                parts[2] = sum;
                main.post(partDone);
            }, "wallet-total").start());
        });
    }
}
