package com.harvana.app.wallet;

import android.content.Context;

/** Dollar value of an amount of any coin in the wallet, from prices already on this phone. NaN when unknown. */
public final class UsdValue {
    private UsdValue() {}

    /** Dollars per one unit of the coin with this asset key (HARV, a Harvana coin such as HNGN, or a catalog coin). */
    public static double per(Context c, String key) {
        if (key == null) return Double.NaN;
        if ("HARV".equalsIgnoreCase(key)) return HarvPrice.usd(c, HarvPrice.peek());
        if (com.harvana.app.husd.Stable.isStable(key)) {
            double rate = FxRates.perUsd(c, com.harvana.app.husd.Stable.currency(key));
            return Double.isNaN(rate) || rate <= 0 ? Double.NaN : 1d / rate;
        }
        AssetPrices.Quote q = AssetPrices.peek(c, key);
        return q == null || q.usd <= 0 ? Double.NaN : q.usd;
    }

    public static double of(Context c, String key, double amount) {
        double p = per(c, key);
        return Double.isNaN(p) ? Double.NaN : p * amount;
    }

    /** "$1.24", or "" when the price is not known. */
    public static String text(Context c, String key, double amount) {
        double v = of(c, key, amount);
        return Double.isNaN(v) ? "" : AssetPrices.usd(v);
    }

    /** Loads the HARV price and today's rates (prices of other coins are loaded by the wallet screens), then runs on the main thread. */
    public static void load(Context c, Runnable onMain) {
        HarvPrice.get(q -> FxRates.load(c, onMain));
    }
}
