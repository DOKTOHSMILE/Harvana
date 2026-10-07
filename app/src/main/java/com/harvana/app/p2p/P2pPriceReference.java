package com.harvana.app.p2p;

import com.harvana.app.sync.HarvanaNodeClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Price reference for the seller screen, in kobo per 1 HARV (1 Naira = 100 kobo, the node's unit).
 *
 * First choice: the network's own price, the "mark" (GET /api/v1/p2p/mark), once it is live. Until enough trades have
 * completed there is none, and this falls back to the MEDIAN of the unit prices on open listings
 * (GET /api/v1/p2p/listings/open). The callback says which one it is: {@code sampleSize} is {@link #NETWORK} for the mark,
 * otherwise the number of listings in the median.
 *
 * Deviation is advisory only: the UI flags but never blocks (the node's own mark logic decides what it accepts).
 */
public final class P2pPriceReference {

    /** Flag when the seller's price is this far from the reference (matches the mark's 15% base move). */
    public static final double DEVIATION_FLAG = 0.15;

    public interface Callback {
        /** medianKobo is 0 when no usable reference exists. */
        void onResult(long medianKobo, int sampleSize);
    }

    /** sampleSize value meaning "this is the network's price, not a median of listings". */
    public static final int NETWORK = -1;

    private P2pPriceReference() {}

    public static void fetch(Callback cb) {
        HarvanaNodeClient.nodeGet("/api/v1/p2p/mark", new HarvanaNodeClient.JsonCallback() {
            @Override public void onSuccess(JSONObject j) {
                long kobo = j.optLong("koboPerHarv", 0L);
                String status = j.optString("status", "");
                if ("LIVE".equals(j.optString("phase")) && kobo > 0 && ("OK".equals(status) || "STALE".equals(status))) cb.onResult(kobo, NETWORK);
                else fetchMedian(cb);
            }
            @Override public void onError(String e) { fetchMedian(cb); }      // older node without the endpoint
        });
    }

    /** One line for the screen saying what the reference is. */
    public static String describe(int sampleSize) {
        return sampleSize == NETWORK ? "the network price" : "median of " + sampleSize + " open listing" + (sampleSize == 1 ? "" : "s");
    }

    private static void fetchMedian(Callback cb) {
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
                if (prices.isEmpty()) {
                    cb.onResult(0L, 0);
                    return;
                }
                Collections.sort(prices);
                int n = prices.size();
                long median = (n % 2 == 1) ? prices.get(n / 2) : (prices.get(n / 2 - 1) + prices.get(n / 2)) / 2;
                cb.onResult(median, n);
            }

            @Override
            public void onError(String error) {
                cb.onResult(0L, 0);
            }
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
