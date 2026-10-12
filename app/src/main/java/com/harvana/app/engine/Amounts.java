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
     * An amount of a savings line: HARV (8 decimals), or the stable coin the line is kept in (6 decimals, shown as
     * hNGN, hUSD, …). {@code asset} is the line's "asset" field, empty for HARV.
     */
    public static String money(long raw, String asset) {
        if (asset == null || asset.isEmpty() || "null".equals(asset)) return harv(raw);
        double v = raw / 1_000_000.0;
        String s = v == Math.rint(v) ? String.format(Locale.US, "%,d", (long) v) : String.format(Locale.US, "%,.2f", v);
        String sym = asset.length() == 4 ? "h" + asset.substring(1) : asset;
        return s + " " + sym;
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
