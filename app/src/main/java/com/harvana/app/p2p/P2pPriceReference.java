package com.harvana.app.p2p;

import com.harvana.app.sync.HarvanaNodeClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Price reference for a HARV listing, in kobo per 1 HARV (1 Naira = 100 kobo).
 *
 * First choice is the open hNGN swap pool. A listing may sit 10% under or over that price and no further
 * ({@link #BAND_BPS}). The mark endpoint reports the band once the node that enforces it is deployed; until then
 * the same number is read from {@code GET /api/v1/swap/price}. With no pool, this falls back to a live mark, then
 * to the median of open listings, and that fallback does not block a price.
 */
public final class P2pPriceReference {

    /** How far a HARV price may sit from the hNGN pool. Same 1,000 bps the node uses. */
    public static final long BAND_BPS = 1_000L;

    /** Older advisory flag, used only when there is no swap pool. */
    public static final double DEVIATION_FLAG = 0.15;

    public interface Callback {
        /** midKobo is 0 when no usable reference exists. */
        void onResult(long midKobo, int sampleSize);
    }

    public interface QuoteCallback {
        void onQuote(Quote quote);
    }

    /** sampleSize value meaning "this is the network mark, not a median of listings". */
    public static final int NETWORK = -1;
    /** sampleSize value meaning "this is the hNGN swap pool". */
    public static final int SWAP = -2;

    /** What the screen and the publisher should price against. */
    public static final class Quote {
        public long midKobo;
        public long floorKobo;
        public long ceilKobo;
        public int sampleSize;
        /** True only when midKobo is the open hNGN pool. That is the only case that blocks a price. */
        public boolean pool;
    }

    private P2pPriceReference() {}

    public static void fetch(Callback cb) {
        fetchQuote(q -> cb.onResult(q.midKobo, q.sampleSize));
    }

    public static void fetchQuote(QuoteCallback cb) {
        HarvanaNodeClient.nodeGet("/api/v1/p2p/mark", new HarvanaNodeClient.JsonCallback() {
            @Override public void onSuccess(JSONObject j) {
                long swap = j.optLong("swapKoboPerHarv", 0L);
                if (swap > 0 && "SWAP_POOL".equals(j.optString("p2pPriceSource", ""))) {
                    cb.onQuote(fromPool(swap));
                    return;
                }
                long mark = j.optLong("koboPerHarv", 0L);
                String status = j.optString("status", "");
                long live = "LIVE".equals(j.optString("phase")) && mark > 0 && ("OK".equals(status) || "STALE".equals(status)) ? mark : 0L;
                fetchPoolOr(live, cb);
            }
            @Override public void onError(String e) { fetchPoolOr(0L, cb); }
        });
    }

    /** One line for the screen saying what the reference is. */
    public static String describe(int sampleSize) {
        if (sampleSize == SWAP) return "the hNGN swap pool";
        if (sampleSize == NETWORK) return "the network price";
        return "median of " + sampleSize + " open listing" + (sampleSize == 1 ? "" : "s");
    }

    /** Lowest allowed kobo-per-HARV. Integer division toward zero, then at least 1. Matches the node. */
    public static long floorKobo(long poolKobo) {
        if (poolKobo <= 0) return 0L;
        long lo = (poolKobo * (10_000L - BAND_BPS)) / 10_000L;
        return Math.max(1L, lo);
    }

    /** Highest allowed kobo-per-HARV. Integer division toward zero. Matches the node. */
    public static long ceilKobo(long poolKobo) {
        if (poolKobo <= 0) return 0L;
        return (poolKobo * (10_000L + BAND_BPS)) / 10_000L;
    }

    /** No open pool: any otherwise valid price. Open pool: price must sit inside the 10% band. */
    public static boolean allowed(long priceKobo, long poolKobo) {
        if (poolKobo <= 0) return true;
        return priceKobo >= floorKobo(poolKobo) && priceKobo <= ceilKobo(poolKobo);
    }

    private static Quote fromPool(long mid) {
        Quote q = new Quote();
        q.midKobo = mid;
        q.floorKobo = floorKobo(mid);
        q.ceilKobo = ceilKobo(mid);
        q.sampleSize = SWAP;
        q.pool = true;
        return q;
    }

    private static Quote fromMark(long mid) {
        Quote q = new Quote();
        q.midKobo = mid;
        q.sampleSize = NETWORK;
        return q;
    }

    /** The public swap price, so the band works before the mark endpoint grows the extra fields. */
    private static void fetchPoolOr(long liveMark, QuoteCallback cb) {
        HarvanaNodeClient.nodeGet("/api/v1/swap/price", new HarvanaNodeClient.JsonCallback() {
            @Override public void onSuccess(JSONObject j) {
                long kobo = j.optLong("koboPerHarv", 0L);
                if (kobo > 0 && "POOL".equals(j.optString("source", ""))) { cb.onQuote(fromPool(kobo)); return; }
                if (liveMark > 0) cb.onQuote(fromMark(liveMark));
                else fetchMedian(cb);
            }
            @Override public void onError(String e) {
                if (liveMark > 0) cb.onQuote(fromMark(liveMark));
                else fetchMedian(cb);
            }
        });
    }

    private static void fetchMedian(QuoteCallback cb) {
        HarvanaNodeClient.nodeGet("/api/v1/p2p/listings/open?page=0&size=100", new HarvanaNodeClient.JsonCallback() {
            @Override
            public void onSuccess(JSONObject json) {
                List<Long> prices = new ArrayList<>();
                JSONArray arr = json.optJSONArray("listings");
                if (arr != null) {
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject l = arr.optJSONObject(i);
                        if (l == null) continue;
                        long p = l.optLong("unitPricePerHarvRaw", 0L);
                        if (p > 0) prices.add(p);
                    }
                }
                Quote q = new Quote();
                if (prices.isEmpty()) { cb.onQuote(q); return; }
                Collections.sort(prices);
                int n = prices.size();
                q.midKobo = (n % 2 == 1) ? prices.get(n / 2) : (prices.get(n / 2 - 1) + prices.get(n / 2)) / 2;
                q.sampleSize = n;
                cb.onQuote(q);
            }

            @Override
            public void onError(String error) { cb.onQuote(new Quote()); }
        });
    }

    /** Signed fractional deviation of price from reference, e.g. 0.2 = 20% above. 0 if no reference. */
    public static double deviation(long priceKobo, long referenceKobo) {
        if (referenceKobo <= 0) return 0d;
        return (priceKobo - referenceKobo) / (double) referenceKobo;
    }

    /** Naira per HARV (double) to the node's kobo-per-HARV integer. */
    public static long nairaToKobo(double nairaPerHarv) {
        return Math.round(nairaPerHarv * 100d);
    }
}
