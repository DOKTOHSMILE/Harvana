package com.harvana.app.group;

/** What this family can do, from the protocol, not from a display name. */
public final class GroupCapability {
    private GroupCapability() {}

    public static boolean canContribute(GroupIdentity id) { return id != null && id.onChain(); }
    public static boolean canRequestDisbursement(GroupIdentity id) {
        return id != null && id.kind == GroupIdentity.Kind.ON_CHAIN_FIXED;
    }
    public static boolean payoutFollowsLockedOrder(GroupIdentity id) {
        return id != null && id.kind == GroupIdentity.Kind.ON_CHAIN_CIRCLE;
    }
    public static boolean canClaimLine(GroupIdentity id) {
        return id != null && id.kind == GroupIdentity.Kind.ON_CHAIN_ACCUM;
    }
    public static boolean canRequestLoan(GroupIdentity id) { return id != null && id.onChain(); }
}
