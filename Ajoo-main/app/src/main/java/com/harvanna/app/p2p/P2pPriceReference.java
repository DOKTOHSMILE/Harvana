package com.harvanna.app.p2p;

import com.harvanna.app.sync.HarvannaNodeClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Price reference for the seller screen, in kobo per 1 HARV (1 Naira = 100 kobo, the node's unit).
 *
 * The node's accepted "mark" (MarkEngine) is the ideal reference, but no REST endpoint exposes it yet
 * (HarvannaProtocolManager.getMarkStatus() has no controller). Until one exists this uses the MEDIAN of the
 * unit prices on currently open listings (GET /api/v1/p2p/listings/open). It is an approximation and is
 * labelled as such in the UI; it is NOT the mark the node enforces.
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

    private P2pPriceReference() {}

    public static void fetch(Callback cb) {
        HarvannaNodeClient.nodeGet("/api/v1/p2p/listings/open?page=0&size=100", new HarvannaNodeClient.JsonCallback() {
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
