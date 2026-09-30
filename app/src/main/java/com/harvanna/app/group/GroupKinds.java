package com.harvanna.app.group;

import java.util.Locale;
import java.util.Map;

/** Small classification helpers for group records. Pure Java. */
public final class GroupKinds {
    private GroupKinds() {}

    public static boolean isOnChain(Map<String, Object> g) {
        Object c = g == null ? null : g.get("circleId");
        return c != null && !String.valueOf(c).trim().isEmpty();
    }

    /** A rotating group created before rotating groups ran on the network: test data, hidden from lists. */
    public static boolean isLegacyRotating(Map<String, Object> g) {
        if (g == null) return false;
        Object t = g.get("groupType");
        boolean rotating = t != null && String.valueOf(t).toLowerCase(Locale.US).contains("rotating");
        return rotating && !isOnChain(g);
    }
}
