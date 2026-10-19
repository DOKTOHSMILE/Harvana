package com.harvana.app.swap;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.TextView;

import com.harvana.app.R;
import com.harvana.app.husd.HusdClient;
import com.harvana.app.husd.Stable;
import com.harvana.app.wallet.BalanceFormatter;

import org.json.JSONArray;
import org.json.JSONObject;

/** Words and numbers for the swap screens. */
final class SwapText {

    private SwapText() {}

    static String sym(String code) {
        return code == null || code.isEmpty() || SwapMath.HARV.equals(code) ? com.harvana.app.CoinInfo.ticker() : Stable.symbol(code);
    }

    static String name(String code) {
        return SwapMath.HARV.equals(code) ? "Harvana" : Stable.name(code);
    }

    static String number(String code, long raw) {
        return SwapMath.HARV.equals(code) ? BalanceFormatter.smartDecimal(BalanceFormatter.rawToHarv(raw)) : HusdClient.format(raw);
    }

    static String amount(String code, long raw) { return number(code, raw) + " " + sym(code); }

    static String pct(long bps) { return BalanceFormatter.smartDecimal(bps / 100.0) + "%"; }

    /** "1 HARV ≈ 13,437.31 hNGN" from what the quote pays for what goes in. */
    static String rate(String from, String to, long inRaw, long outRaw) {
        if (inRaw <= 0 || outRaw <= 0) return "";
        double in = inRaw / Math.pow(10, SwapMath.decimals(from)), out = outRaw / Math.pow(10, SwapMath.decimals(to));
        return "1 " + sym(from) + " ≈ " + BalanceFormatter.smartDecimal(out / in) + " " + sym(to);
    }

    /** Before an amount is typed: the pools' spot rate (no fee, no price impact). Empty when a pool is closed. */
    static String spotRate(String from, String to, JSONArray pools) {
        double f = perHarv(from, pools), t = perHarv(to, pools);
        if (f <= 0 || t <= 0) return "";
        return "1 " + sym(from) + " ≈ " + BalanceFormatter.smartDecimal(t / f) + " " + sym(to);
    }

    private static double perHarv(String code, JSONArray pools) {
        if (SwapMath.HARV.equals(code)) return 1;
        for (int i = 0; pools != null && i < pools.length(); i++) {
            JSONObject p = pools.optJSONObject(i);
            if (p != null && code.equals(p.optString("asset"))) {
                return p.optBoolean("open") ? p.optLong("coinRawPerHarv") / (double) HusdClient.UNIT : 0;
            }
        }
        return 0;
    }

    static String shortAddr(String a) {
        return a == null || a.length() < 14 ? String.valueOf(a) : a.substring(0, 8) + "…" + a.substring(a.length() - 4);
    }

    /** The node's error code as something a person can act on. */
    static String problem(String e) {
        if (e == null) return "Couldn't swap.";
        if (com.harvana.app.utils.ErrorText.is(e, "NO_LIQUIDITY")) return "This pair has no liquidity right now.";
        if (com.harvana.app.utils.ErrorText.is(e, "PRICE_MOVED")) return "The price moved past your slippage tolerance. Review the new quote.";
        if (com.harvana.app.utils.ErrorText.is(e, "AMOUNT_TOO_SMALL")) return "That amount is too small to swap.";
        if (com.harvana.app.utils.ErrorText.is(e, "INSUFFICIENT")) return "Not enough balance for this swap and its tFare.";
        if (com.harvana.app.utils.ErrorText.is(e, "_NOT_ACTIVE")) return "That coin isn't switched on yet.";
        if (com.harvana.app.utils.ErrorText.is(e, "SAME_ASSET")) return "Pick two different assets.";
        if (com.harvana.app.utils.ErrorText.is(e, "UNKNOWN_ASSET")) return "That asset can't be swapped.";
        return com.harvana.app.utils.ErrorText.plain(e);
    }

    /** A short line for an empty or error state (res/layout/item_list_empty.xml). */
    static TextView line(Context c, String s) {
        TextView t = (TextView) LayoutInflater.from(c).inflate(R.layout.item_list_empty, null, false);
        t.setText(s);
        t.setTextColor(androidx.core.content.ContextCompat.getColor(c, R.color.swapTextSecondary));
        int pad = Math.round(16 * c.getResources().getDisplayMetrics().density);
        t.setPadding(0, pad, 0, pad);
        t.setTextSize(13);
        return t;
    }
}
