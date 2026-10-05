package com.harvana.app.learn;

import java.util.Locale;

/**
 * Small pure helpers for the Learn Hub screens: word count, the least time a read must take, counts and "time ago".
 * The dwell rule mirrors the node's EngagementStats so the app does not send a receipt the node would refuse for being
 * too quick. The node still decides; this only avoids pointless requests.
 */
public final class LearnRules {
    private LearnRules() {}

    public static final long MIN_DWELL_MS = 8_000L;
    public static final int READ_WORDS_PER_MINUTE = 400;
    public static final double MIN_DWELL_FRACTION = 0.4;
    public static final int MAX_WORDS = 3_000;

    public static int wordCount(String... parts) {
        int n = 0;
        for (String p : parts) {
            if (p == null) continue;
            String t = p.trim();
            if (t.isEmpty()) continue;
            n += t.split("\\s+").length;
        }
        return Math.min(n, MAX_WORDS);
    }

    /** The least time a reader must stay on a piece this long, same rule as the node. */
    public static long requiredDwellMs(int words) {
        int w = Math.max(0, Math.min(words, MAX_WORDS));
        long reading = Math.round(w / (double) READ_WORDS_PER_MINUTE * 60_000.0 * MIN_DWELL_FRACTION);
        return Math.max(MIN_DWELL_MS, reading);
    }

    /** 0, 999, 1.2K, 15K, 1.1M */
    public static String compactCount(long n) {
        if (n < 1_000) return String.valueOf(Math.max(0, n));
        if (n < 10_000) return trim(n / 1_000.0) + "K";
        if (n < 1_000_000) return (n / 1_000) + "K";
        return trim(n / 1_000_000.0) + "M";
    }

    private static String trim(double v) {
        String s = String.format(Locale.US, "%.1f", v);
        return s.endsWith(".0") ? s.substring(0, s.length() - 2) : s;
    }

    /** "now", "5m ago", "3h ago", "1d ago", "2w ago", "4mo ago", "2y ago" */
    public static String timeAgo(long thenMs, long nowMs) {
        if (thenMs <= 0) return "";
        long s = Math.max(0, (nowMs - thenMs) / 1000);
        if (s < 60) return "now";
        long m = s / 60;
        if (m < 60) return m + "m ago";
        long h = m / 60;
        if (h < 24) return h + "h ago";
        long d = h / 24;
        if (d < 7) return d + "d ago";
        if (d < 30) return (d / 7) + "w ago";
        if (d < 365) return (d / 30) + "mo ago";
        return (d / 365) + "y ago";
    }
}
