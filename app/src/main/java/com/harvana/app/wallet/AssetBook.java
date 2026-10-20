package com.harvana.app.wallet;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * The balances of every coin outside Harvana (the built-in chains and the coins the person added), read together
 * (owner, 20 Oct 2026: rows must not fill in one by one). All balances and the prices are asked at once; when all have
 * answered, or the time limit passed, the whole book is published in one go and kept on the phone, so the Assets tab
 * opens on the last book at once. A balance a network failed to give keeps its last known value: it never drops to 0.
 */
public final class AssetBook {

    public interface Listener { void onBook(Map<String, String> balances, boolean fresh); }

    private static final long WAIT_MS = 20_000L;
    private static final Map<String, AssetBook> BY_WALLET = new HashMap<>();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final ExecutorService POOL = Executors.newFixedThreadPool(6);

    private final String wallet;
    private final Map<String, Listener> listeners = new HashMap<>();
    private final Map<String, String> balances = new HashMap<>();
    private boolean running, loaded;

    private AssetBook(String wallet) { this.wallet = wallet; }

    public static synchronized AssetBook of(String wallet) {
        AssetBook b = BY_WALLET.get(wallet);
        if (b == null) { b = new AssetBook(wallet); BY_WALLET.put(wallet, b); }
        return b;
    }

    /** One listener per name (each Assets section); a new one replaces the old. */
    public synchronized void listen(String name, Listener l) { listeners.put(name, l); }

    /** The last book, from memory or the phone: coin key → balance text ("0.054021459 SOL"). */
    public synchronized Map<String, String> cached(Context c) {
        if (!loaded) {
            SharedPreferences p = c.getSharedPreferences("asset_book", Context.MODE_PRIVATE);
            try {
                JSONObject j = new JSONObject(p.getString(wallet, "{}"));
                for (Iterator<String> it = j.keys(); it.hasNext(); ) { String k = it.next(); balances.putIfAbsent(k, j.optString(k)); }
            } catch (Exception ignored) { }
            loaded = true;
        }
        return new HashMap<>(balances);
    }

    /** Reads every balance and the prices together, then publishes once. */
    public synchronized void refresh(Context context) {
        if (running) return;
        running = true;
        final Context app = context.getApplicationContext();
        cached(app);
        POOL.execute(() -> {
            Map<String, String> fresh = new HashMap<>();
            List<Callable<String[]>> jobs = new ArrayList<>();
            Map<String, String> addrs = CoinCatalog.addresses(app, wallet);
            for (CoinCatalog.Coin coin : CoinCatalog.others()) {
                if (!ChainAssetsSection.shown(app, coin.key)) continue;
                String a = addrs.get(coin.key);
                if (a == null || a.isEmpty()) continue;
                jobs.add(() -> new String[]{coin.key, ChainBalances.assetBalance(coin.key, a)});
            }
            if (CustomAssets.cached().isEmpty()) CustomAssets.all(app);
            for (CustomAssets.Asset a : CustomAssets.cached()) {
                if (a.hidden) continue;
                jobs.add(() -> {
                    String owner = CustomAssets.address(app, wallet, a);
                    return new String[]{a.key(), owner.isEmpty() ? "" : CustomAssets.balanceOf(a, owner)};
                });
            }
            // The prices are asked at the same time, so the values are painted with the balances.
            final java.util.concurrent.CountDownLatch prices = new java.util.concurrent.CountDownLatch(1);
            MAIN.post(() -> AssetPrices.load(app, () -> FxRates.load(app, prices::countDown)));
            try {
                for (Future<String[]> f : POOL_INNER.invokeAll(jobs, WAIT_MS, TimeUnit.MILLISECONDS)) {
                    if (f.isCancelled()) continue;
                    try {
                        String[] r = f.get();
                        if (usable(r[1])) fresh.put(r[0], r[1].trim());
                    } catch (Exception ignored) { }
                }
                prices.await(WAIT_MS, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            final Map<String, String> out;
            synchronized (AssetBook.this) {
                running = false;
                balances.putAll(fresh);                                  // a failed read keeps the last known balance
                out = new HashMap<>(balances);
                try {
                    app.getSharedPreferences("asset_book", Context.MODE_PRIVATE).edit()
                            .putString(wallet, new JSONObject(out).toString()).apply();
                } catch (Exception ignored) { }
            }
            MAIN.post(() -> {
                List<Listener> ls;
                synchronized (AssetBook.this) { ls = new ArrayList<>(listeners.values()); }
                for (Listener l : ls) l.onBook(out, true);
            });
        });
    }

    private static final ExecutorService POOL_INNER = Executors.newFixedThreadPool(6);

    /** A real balance, not a network's error text. */
    static boolean usable(String line) {
        if (line == null) return false;
        String t = line.trim().toLowerCase(java.util.Locale.US);
        if (t.isEmpty() || t.contains("unavailable") || t.contains("error") || t.contains("fail") || t.contains("timeout") || t.contains("loading")) return false;
        return Character.isDigit(t.charAt(0)) || t.startsWith(".");
    }
}
