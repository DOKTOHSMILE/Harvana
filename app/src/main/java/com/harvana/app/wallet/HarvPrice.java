package com.harvana.app.wallet;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * HARV's price for every screen: the swap pools' price (GET /api/v1/swap/price: kobo per HARV from the hNGN pool,
 * micro-dollars from the hUSD pool, and the 24-hour change). Until a pool is open, the network's P2P mark or the
 * median of open listings. One shared result, refreshed at most once a minute.
 */
public final class HarvPrice {
    private HarvPrice() {}

    /** A price: kobo per HARV (0 = none), dollars per HARV when a dollar pool is open (NaN otherwise), 24h change in %. */
    public static final class Quote {
        public final long koboPerHarv;
        public final double usdFromPool;
        public final double changePct;
        Quote(long kobo, double usd, double change) { koboPerHarv = kobo; usdFromPool = usd; changePct = change; }
        public boolean known() { return koboPerHarv > 0 || !Double.isNaN(usdFromPool); }
    }

    public interface Callback { void onPrice(Quote q); }

    private static final long FRESH_MS = 60_000L;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static Quote last;
    private static long at;
    private static boolean inFlight;
    private static final List<Callback> waiting = new ArrayList<>();

    /** The last price already loaded, or null. */
    public static Quote peek() { return last; }

    /** Delivers the price on the main thread. */
    public static void get(Callback cb) {
        synchronized (waiting) {
            if (last != null && System.currentTimeMillis() - at < FRESH_MS) { Quote q = last; MAIN.post(() -> cb.onPrice(q)); return; }
            waiting.add(cb);
            if (inFlight) return;
            inFlight = true;
        }
        com.harvana.app.sync.HarvanaNodeClient.nodeGet("/api/v1/swap/price", new com.harvana.app.sync.HarvanaNodeClient.JsonCallback() {
            @Override public void onSuccess(JSONObject j) {
                long kobo = j.optLong("koboPerHarv", 0L), usdMicro = j.optLong("usdMicroPerHarv", 0L);
                double change = j.isNull("change24hBps") || !j.has("change24hBps") ? Double.NaN : j.optLong("change24hBps") / 100d;
                if (kobo > 0 || usdMicro > 0) done(new Quote(kobo, usdMicro > 0 ? usdMicro / 1e6 : Double.NaN, change));
                else fallback();
            }
            @Override public void onError(String e) { fallback(); }               // an older node without the pools
        });
    }

    private static void fallback() {
        com.harvana.app.p2p.P2pPriceReference.fetch((kobo, n) -> done(new Quote(Math.max(0, kobo), Double.NaN, Double.NaN)));
    }

    private static void done(Quote q) {
        List<Callback> run;
        synchronized (waiting) {
            if (q.known() || last == null) { last = q; at = System.currentTimeMillis(); }
            inFlight = false;
            run = new ArrayList<>(waiting);
            waiting.clear();
        }
        Quote give = last;
        MAIN.post(() -> { for (Callback c : run) c.onPrice(give); });
    }

    /** Dollars per HARV: the dollar pool when open, otherwise the naira price through today's rate. NaN when unknown. */
    public static double usd(Context c, Quote q) {
        if (q == null) return Double.NaN;
        if (!Double.isNaN(q.usdFromPool)) return q.usdFromPool;
        if (q.koboPerHarv <= 0) return Double.NaN;
        double ngnPerUsd = FxRates.perUsd(c, "NGN");
        return Double.isNaN(ngnPerUsd) || ngnPerUsd <= 0 ? Double.NaN : (q.koboPerHarv / 100d) / ngnPerUsd;
    }

    /** Value of {@code harv} in the currency with ISO {@code code}; calls back on the main thread with NaN when unknown. */
    public interface Value { void accept(double v); }

    public static void value(Context c, double harv, String code, Value cb) {
        get(q -> {
            if (q.koboPerHarv > 0 && "NGN".equals(code)) { cb.accept(harv * q.koboPerHarv / 100d); return; }
            FxRates.load(c, () -> {
                double usd = usd(c, q), per = FxRates.perUsd(c, code);
                cb.accept(Double.isNaN(usd) || Double.isNaN(per) || per <= 0 ? Double.NaN : harv * usd * per);
            });
        });
    }
}
