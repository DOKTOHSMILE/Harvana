package com.harvanna.app.group;

import java.util.Locale;
import java.util.Map;

/** Small classification helpers for group records. Pure Java. */
public final class GroupKinds {
    private GroupKinds() {}

    /** CIRCLE, ACCUM, FIXED, or "" when the id is not a protocol id. */
    public static String familyOf(String groupId) {
        if (groupId == null) return "";
        String id = groupId.trim();
        if (id.startsWith("CIRC-")) return "CIRCLE";
        if (id.startsWith("ACCUM-")) return "ACCUM";
        if (id.startsWith("FIXED-")) return "FIXED";
        return "";
    }

    /** The group's id on the node (CIRC-, ACCUM- or FIXED-), or "" for a Firestore-only group. */
    public static String nodeId(Map<String, Object> g) {
        if (g == null) return "";
        for (String key : new String[]{"nodeGroupId", "circleId", "nodeId", "groupId"}) {
            Object v = g.get(key);
            if (v == null) continue;
            String s = String.valueOf(v).trim();
            if (!familyOf(s).isEmpty()) return s;
        }
        return "";
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
