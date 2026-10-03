package com.harvana.app.group;

import java.util.Locale;
import java.util.Map;

/** Small classification helpers for group records. Pure Java. */
public final class GroupKinds {
    private GroupKinds() {}

    /** The group's id on the node (CIRC-, ACCUM- or FIXED-), or "" for a Firestore-only group. */
    public static String nodeId(Map<String, Object> g) {
        if (g == null) return "";
        Object n = g.get("nodeGroupId");
        if (n == null || String.valueOf(n).trim().isEmpty()) n = g.get("circleId");
        return n == null ? "" : String.valueOf(n).trim();
    }

    public static boolean isOnChain(Map<String, Object> g) {
        return !nodeId(g).isEmpty();
    }

    /** A rotating group created before rotating groups ran on the network: test data, hidden from lists. */
    public static boolean isLegacyRotating(Map<String, Object> g) {
        if (g == null) return false;
        Object t = g.get("groupType");
        boolean rotating = t != null && String.valueOf(t).toLowerCase(Locale.US).contains("rotating");
        return rotating && !isOnChain(g);
    }
}
