package com.harvana.app.group;

import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.core.content.ContextCompat;

/** A coin's logo for group pages: a stable coin's own coin (hNGN, hUSD…), or HARV's. */
public final class CoinLogo {
    private CoinLogo() {}

    public static Drawable of(Context c, String asset) {
        if (com.harvana.app.husd.Stable.isStable(asset)) return com.harvana.app.husd.StableIcon.drawable(c, asset);
        return ContextCompat.getDrawable(c, com.harvana.app.R.drawable.ic_coin_harv);
    }

    public static String symbol(String asset) {
        return com.harvana.app.husd.Stable.isStable(asset) ? com.harvana.app.husd.Stable.symbol(asset) : com.harvana.app.CoinInfo.ticker();
    }

    public static String name(String asset) {
        return com.harvana.app.husd.Stable.isStable(asset) ? com.harvana.app.husd.Stable.name(asset) : "Harvana";
    }

    /** Shows a logo on an image view (tint cleared: a logo keeps its own colours). */
    public static void show(android.widget.ImageView iv, String asset) {
        if (iv == null) return;
        iv.setImageTintList(null);
        iv.clearColorFilter();
        iv.setImageDrawable(of(iv.getContext(), asset));
        iv.setVisibility(android.view.View.VISIBLE);
    }
}
