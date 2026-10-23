package com.harvana.app.engine;

import java.util.Locale;
import java.util.Map;

/** Formatting and safe reads shared by the engines. */
public final class Amounts {
    private Amounts() {}

    public static String harv(long raw) {
        double v = raw / 100_000_000.0;
        String s = v == Math.rint(v) ? String.valueOf((long) v) : String.format(Locale.US, "%.4f", v).replaceAll("0+$", "").replaceAll("\\.$", "");
        return s + " HARV";
    }

    /**
     * Raw units per one whole coin. HARV is 1e8. Every stable coin (hNGN, hUSD, …) is 1e6.
     * Dividing a stable raw amount by 1e8 shows 1/100 of the real figure (500,000 hNGN becomes 5,000).
     */
    public static double scale(String asset) {
        String code = stableCode(asset);
        return code.isEmpty() ? 100_000_000.0 : 1_000_000.0;
    }

    /** Whole coins the person typed, in the raw units that coin uses on the network. */
    public static long toRaw(double human, String asset) {
        if (human <= 0) return 0L;
        double unit = scale(asset);
        return Math.round(human * unit);
    }

    /** Human amount from a raw on-chain figure, using the coin that group or line is kept in. */
    public static double human(double raw, String asset) {
        double unit = scale(asset);
        return unit <= 0 ? raw : raw / unit;
    }

    /** "HARV", or "hNGN" / "hUSD" for a stable coin. */
    public static String unitLabel(String asset) {
        String code = stableCode(asset);
        if (!code.isEmpty()) return com.harvana.app.husd.Stable.symbol(code);
        if (asset == null || asset.trim().isEmpty() || "null".equalsIgnoreCase(asset.trim())) return "HARV";
        return "HARV".equalsIgnoreCase(asset.trim()) ? "HARV" : asset.trim();
    }

    /** "HNGN" when {@code asset} is a stable coin ("hNGN", "HNGN", "NGN"); empty for HARV. */
    public static String stableCode(String asset) {
        if (asset == null) return "";
        String up = asset.trim().toUpperCase(Locale.US);
        if (up.isEmpty() || "NULL".equals(up) || "HARV".equals(up)) return "";
        if (up.length() == 3) up = "H" + up;
        return com.harvana.app.husd.Stable.isStable(up) ? up : "";
    }

    /**
     * An amount of a savings line: HARV (8 decimals), or the stable coin the line is kept in (6 decimals, shown as
     * hNGN, hUSD, …). {@code asset} is the line's "asset" field, empty for HARV.
     */
    public static String money(long raw, String asset) {
        String code = stableCode(asset);
        if (code.isEmpty()) return harv(raw);
        double v = raw / 1_000_000.0;
        String s = v == Math.rint(v) ? String.format(Locale.US, "%,d", (long) v) : String.format(Locale.US, "%,.2f", v);
        return s + " " + com.harvana.app.husd.Stable.symbol(code);
    }

    /** The line's coin: its "asset" field, or empty for HARV. */
    public static String assetOf(Map<String, Object> line) {
        Object a = line == null ? null : line.get("asset");
        return a == null ? "" : String.valueOf(a);
    }

    static long lng(Map<String, Object> m, String k) {
        Object o = m == null ? null : m.get(k);
        if (o instanceof Number) return ((Number) o).longValue();
        try { return o == null ? 0 : Long.parseLong(String.valueOf(o)); } catch (Exception e) { return 0; }
    }

    static String str(Map<String, Object> m, String k) {
        Object o = m == null ? null : m.get(k);
        return o == null ? "" : String.valueOf(o);
    }

    static String cycle(long hours) {
        if (hours > 0 && hours % 24 == 0) {
            long d = hours / 24;
            return d == 1 ? "Daily" : d == 7 ? "Weekly" : "Every " + d + " days";
        }
        return "Every " + hours + " hours";
    }
}
