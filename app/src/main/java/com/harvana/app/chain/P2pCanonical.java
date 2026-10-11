package com.harvana.app.chain;

/** Canonical message for a P2P listing, byte-for-byte the node's P2PListingCreateRequest.getCanonicalMessage(). */
public final class P2pCanonical {

    private P2pCanonical() {}

    public static String createListing(String posterAddress, String side, long totalAmount, long minOrderAmount,
                                       long maxOrderAmount, long durationHours, long paymentWindowMinutes,
                                       long unitPricePerHarvRaw, long nonce) {
        return "P2P_CREATE_LISTING:" + posterAddress + ":" + side + ":" + totalAmount + ":" + minOrderAmount
                + ":" + maxOrderAmount + ":" + durationHours + ":" + paymentWindowMinutes + ":" + unitPricePerHarvRaw + ":" + nonce;
    }

    /** Same, for a listing of any asset: a stable asset's code is appended (the node signs it); HARV is unchanged. */
    public static String createListing(String posterAddress, String side, long totalAmount, long minOrderAmount,
                                       long maxOrderAmount, long durationHours, long paymentWindowMinutes,
                                       long unitPricePerHarvRaw, long nonce, String asset) {
        String base = createListing(posterAddress, side, totalAmount, minOrderAmount, maxOrderAmount, durationHours,
                paymentWindowMinutes, unitPricePerHarvRaw, nonce);
        return com.harvana.app.p2p.P2pAsset.isStable(asset) ? base + ":" + com.harvana.app.p2p.P2pAsset.normalize(asset) : base;
    }
}
