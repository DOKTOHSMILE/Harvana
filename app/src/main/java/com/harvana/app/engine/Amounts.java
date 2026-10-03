package com.harvana.app.engine;

import java.util.Locale;
import java.util.Map;

/** Formatting and safe reads shared by the engines. */
final class Amounts {
    private Amounts() {}

    static String harv(long raw) {
        double v = raw / 100_000_000.0;
        String s = v == Math.rint(v) ? String.valueOf((long) v) : String.format(Locale.US, "%.4f", v).replaceAll("0+$", "").replaceAll("\\.$", "");
        return s + " HARV";
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
