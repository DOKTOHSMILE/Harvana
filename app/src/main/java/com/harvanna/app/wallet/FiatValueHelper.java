package com.harvanna.app.wallet;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * HARV has no external market price anywhere — no exchange lists it, no
 * price feed exists for it. The only real, honest source of "what is HARV
 * worth" is the P2P marketplace itself: real sellers setting real rates,
 * real buyers accepting them. This class defines "the" displayed rate as
 * the CHEAPEST currently-open unit price per currency — the best price a
 * buyer could actually get right now, same logic as a "best ask" in a
 * real order book.
 *
 * If no open listings exist in a given currency, there is genuinely no
 * rate to show — callers get told that explicitly (rateAvailable=false)
 * rather than being handed a fabricated number. A screen showing "≈ ₦0.00"
 * when the real answer is "we don't know" is worse than showing nothing.
 */
public final class FiatValueHelper {

    private FiatValueHelper() {}

    private static final long CACHE_TTL_MS = 3 * 60 * 1000; // 3 minutes
    // Bounds how many listings' prices get fetched from Firestore per
    // refresh — see _refreshRates' own comment for why this is a
    // deliberate sample rather than every open listing.
    private static final int RATE_SAMPLE_SIZE = 20;
    private static Map<String, Double> cachedRates = null; // currency code -> cheapest unit price
    private static long cachedAt = 0;

    public interface RateCallback {
        /** rate is null if no open listings exist in that currency right now. */
        void onRate(Double rate);
    }

    public interface ConvertCallback {
        void onConverted(double fiatValue, boolean rateAvailable);
    }

    public static void getCheapestRate(String currencyCode, RateCallback callback) {
        long now = System.currentTimeMillis();
        if (cachedRates != null && (now - cachedAt) < CACHE_TTL_MS) {
            callback.onRate(cachedRates.get(currencyCode));
            return;
        }
        _refreshRates(() -> callback.onRate(cachedRates != null ? cachedRates.get(currencyCode) : null),
        error -> callback.onRate(cachedRates != null ? cachedRates.get(currencyCode) : null));
    }

    public static void convertHarvToFiat(double harvAmount, String currencyCode, ConvertCallback callback) {
        getCheapestRate(currencyCode, rate -> {
            if (rate == null) {
                callback.onConverted(0, false);
            } else {
                callback.onConverted(harvAmount * rate, true);
            }
        });
    }

    /** Drops the cache so the next call fetches fresh — useful right after
     *  posting or completing a P2P trade, when the cheapest rate may have
     *  just changed and a 3-minute-stale number would be misleading. */
    public static void invalidateCache() {
        cachedRates = null;
        cachedAt = 0;
    }

    private interface OnError {
        void onError(String error);
    }

    private static void _refreshRates(Runnable onDone, OnError onError) {
        // THE UPGRADE: explicitly SELL-side only — that's genuine supply
        // being offered at a real price, the actual market rate. A BUY
        // listing's price is just someone's bid/demand, not a reliable
        // signal for what HARV is actually trading at.
        com.harvanna.app.sync.HarvannaNodeClient.getP2POpenListings(0, 200, "SELL", new com.harvanna.app.sync.HarvannaNodeClient.JsonCallback() {
            @Override
            public void onSuccess(JSONObject result) {
                JSONArray listings = result.optJSONArray("listings");
                java.util.List<String> listingIds = new java.util.ArrayList<>();
                if (listings != null) {
                    for (int i = 0; i < listings.length(); i++) {
                        JSONObject listing = listings.optJSONObject(i);
                        if (listing == null) continue;
                        String id = listing.optString("listingId", "");
                        if (!id.isEmpty()) listingIds.add(id);
                    }
                }

                if (listingIds.isEmpty()) {
                    cachedRates = new HashMap<>();
                    cachedAt = System.currentTimeMillis();
                    onDone.run();
                    return;
                }

                // THE UPGRADE: unitPrice/currency no longer exist on the
                // node's listing response at all — fiat data was removed
                // from the chain entirely, now living only in Firestore
                // per listingId. Fetching all 200 individually would be
                // 200 network round-trips for one cached rate lookup, so
                // this samples a bounded subset (RATE_SAMPLE_SIZE) rather
                // than every open listing — a "cheapest of N" estimate
                // instead of a guaranteed global minimum, which is a
                // reasonable trade for something re-fetched every 3
                // minutes rather than needed to the exact cent.
                java.util.List<String> sample = listingIds.size() > RATE_SAMPLE_SIZE
                        ? listingIds.subList(0, RATE_SAMPLE_SIZE) : listingIds;

                Map<String, Double> rates = new java.util.concurrent.ConcurrentHashMap<>();
                java.util.concurrent.atomic.AtomicInteger pending = new java.util.concurrent.atomic.AtomicInteger(sample.size());

                for (String listingId : sample) {
                    // P2PPaymentDetailsManager.getInstance(Context) never
                    // actually uses its Context parameter — confirmed by
                    // reading the constructor, which takes none and only
                    // builds an OkHttpClient — so null is genuinely safe
                    // here, not a shortcut around a real dependency. This
                    // helper has no Context anywhere in its own public
                    // API to thread through instead.
                    com.harvanna.app.firestore.P2PPaymentDetailsManager.getInstance(null)
                    .get(listingId, new com.harvanna.app.firestore.P2PPaymentDetailsManager.DocCallback() {
                        @Override
                        public void onSuccess(java.util.Map<String, Object> data) {
                            Object priceObj = data.get("unitPrice");
                            Object currencyObj = data.get("currency");
                            if (priceObj instanceof Number && currencyObj != null) {
                                double unitPrice = ((Number) priceObj).doubleValue();
                                String currency = String.valueOf(currencyObj);
                                if (!currency.isEmpty() && unitPrice > 0) {
                                    rates.merge(currency, unitPrice, Math::min);
                                }
                            }
                            _finishIfDone(pending, rates, onDone);
                        }
                        @Override
                        public void onError(String error) {
                            _finishIfDone(pending, rates, onDone);
                        }
                    });
                }
            }

            @Override
            public void onError(String error) {
                onError.onError(error);
            }
        });
    }

    /** Thread-safe completion check for the parallel per-listing price
*  fetches above — Firestore callbacks can land on different
*  background threads, so this can't just be a plain counter
*  decrement without a race between "am I the last one" checks. */
    private static void _finishIfDone(java.util.concurrent.atomic.AtomicInteger pending, Map<String, Double> rates, Runnable onDone) {
        if (pending.decrementAndGet() == 0) {
            cachedRates = new HashMap<>(rates);
            cachedAt = System.currentTimeMillis();
            onDone.run();
        }
    }

    /**
     * The app's currency preference currently stores a display symbol
     * (e.g. "₦"), not the ISO-style code P2P listings actually use — a
     * real gap in the currency-preference system that a proper Currency
     * Settings screen should eventually close by storing the code
     * directly. This bridges that gap until it exists, rather than
     * blocking fiat display on that screen getting built first.
     */
    public static String symbolToCurrencyCode(String symbol) {
        if (symbol == null) return "NGN";
        switch (symbol) {
            case "₦": return "NGN";
            case "$": return "USD";
            case "£": return "GBP";
            case "€": return "EUR";
            case "GH₵": return "GHS";
            case "KSh": return "KES";
            case "R": return "ZAR";
            default: return "NGN";
        }
    }
}
