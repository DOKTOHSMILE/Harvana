package com.harvanna.app.group;

/** One group. The Firestore document is the profile. The node id is the protocol identity. */
public final class GroupIdentity {
    public enum Kind {
        ON_CHAIN_CIRCLE,
        ON_CHAIN_ACCUM,
        ON_CHAIN_FIXED,
        LEGACY_OFFCHAIN,
        BROKEN_MAPPING
    }

    public final String firestoreId;
    public final String nodeGroupId;
    public final String family;
    public final Kind kind;

    public GroupIdentity(String firestoreId, String nodeGroupId, String family, Kind kind) {
        this.firestoreId = firestoreId == null ? "" : firestoreId;
        this.nodeGroupId = nodeGroupId == null ? "" : nodeGroupId;
        this.family = family == null ? "" : family;
        this.kind = kind == null ? Kind.LEGACY_OFFCHAIN : kind;
    }

    public boolean onChain() {
        return kind == Kind.ON_CHAIN_CIRCLE || kind == Kind.ON_CHAIN_ACCUM || kind == Kind.ON_CHAIN_FIXED;
    }

    /** The only id that may be sent to a node group endpoint. Empty when this is not a protocol group. */
    public String protocolId() {
        return onChain() ? nodeGroupId : "";
    }
}
