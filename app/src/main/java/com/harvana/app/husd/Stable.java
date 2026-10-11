package com.harvana.app.husd;

/**
 * Harvana's stable assets (node: docs/STABLE_ASSETS.md): hUSD, hEUR, hGBP, hNGN, hGHS, hKES, hZAR. Each is its own
 * token, backed one for one by a token of the same currency in its own vault. All use 6 decimals and the same rules;
 * only the currency, the fees and the reserve token differ. The node says which ones are switched on.
 */
public final class Stable {
    private Stable() {}

    public static final String[] CODES = {"HUSD", "HEUR", "HGBP", "HNGN", "HGHS", "HKES", "HZAR"};

    public static boolean isStable(String code) {
        if (code == null) return false;
        for (String c : CODES) if (c.equals(code)) return true;
        return false;
    }

    /** "HNGN" → "NGN". */
    public static String currency(String code) { return code != null && code.length() == 4 ? code.substring(1) : "USD"; }

    /** "HNGN" → "hNGN". */
    public static String symbol(String code) { return "h" + currency(code); }

    public static String name(String code) {
        switch (currency(code)) {
            case "USD": return "US dollar";
            case "EUR": return "Euro";
            case "GBP": return "British pound";
            case "NGN": return "Nigerian naira";
            case "GHS": return "Ghanaian cedi";
            case "KES": return "Kenyan shilling";
            case "ZAR": return "South African rand";
            default: return currency(code);
        }
    }

    /** What a deposit or withdrawal is made in on the other chain: the node's "reserveToken" if it says, else ours. */
    public static String reserve(String code, org.json.JSONObject info) {
        String fromNode = info == null ? "" : info.optString("reserveToken", "");
        if (!fromNode.isEmpty()) return fromNode;
        switch (currency(code)) {
            case "USD": return "USDC";
            case "EUR": return "EURC";
            default: return currency(code) + " stablecoin";
        }
    }

    /** The node's API path for one asset: hUSD keeps /api/v1/husd (older nodes have only that). */
    static String base(String code) { return "HUSD".equals(code) ? "/api/v1/husd" : "/api/v1/stable/" + code; }
}
