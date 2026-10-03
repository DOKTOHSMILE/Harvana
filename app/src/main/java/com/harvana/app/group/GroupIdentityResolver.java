package com.harvana.app.group;

import java.util.Map;

/**
 * Turns a Firestore group record into one identity. A random document id is never treated as a node id.
 */
public final class GroupIdentityResolver {
    private GroupIdentityResolver() {}

    public static GroupIdentity resolve(String firestoreDocId, Map<String, Object> data) {
        String docId = firestoreDocId == null ? "" : firestoreDocId.trim();
        String explicit = first(data, "nodeGroupId", "circleId", "nodeId");
        if (!explicit.isEmpty() && GroupKinds.familyOf(explicit).isEmpty()) {
            return new GroupIdentity(docId, "", "", GroupIdentity.Kind.BROKEN_MAPPING);
        }
        String node = !explicit.isEmpty() ? explicit : protocol(data == null ? null : data.get("groupId"));
        if (node.isEmpty()) node = protocol(docId);
        String family = GroupKinds.familyOf(node);
        if (family.isEmpty()) return new GroupIdentity(docId, "", "", GroupIdentity.Kind.LEGACY_OFFCHAIN);
        GroupIdentity.Kind kind = "CIRCLE".equals(family) ? GroupIdentity.Kind.ON_CHAIN_CIRCLE
                : "ACCUM".equals(family) ? GroupIdentity.Kind.ON_CHAIN_ACCUM
                : GroupIdentity.Kind.ON_CHAIN_FIXED;
        return new GroupIdentity(docId, node, family, kind);
    }

    private static String first(Map<String, Object> data, String... keys) {
        if (data == null) return "";
        for (String key : keys) {
            Object v = data.get(key);
            if (v == null) continue;
            String s = String.valueOf(v).trim();
            if (!s.isEmpty()) return s;
        }
        return "";
    }

    private static String protocol(Object v) {
        if (v == null) return "";
        String s = String.valueOf(v).trim();
        return GroupKinds.familyOf(s).isEmpty() ? "" : s;
    }
}
