package com.harvana.app.chain;

/** Signed messages for the node's engagement endpoints. They must match EngagementReadRequest / EngagementStatsRequest on the node. */
public final class EngagementCanonical {
    private EngagementCanonical() {}

    /** HARVANA_ENGAGEMENT_READ_V1:{address}:{contentId}:{dwellMs}:{timestamp} */
    public static String read(String address, String contentId, long dwellMs, long timestamp) {
        return "HARVANA_ENGAGEMENT_READ_V1:" + address + ":" + contentId + ":" + dwellMs + ":" + timestamp;
    }

    /** HARVANA_ENGAGEMENT_STATS_V1:{address}:{timestamp} */
    public static String stats(String address, long timestamp) {
        return "HARVANA_ENGAGEMENT_STATS_V1:" + address + ":" + timestamp;
    }

    /** The memo on a Learn Hub gift. The node counts a confirmed transfer with this memo once per recipient. */
    public static String giftMemo(String contentId) {
        return "LEARN_GIFT:" + contentId;
    }
}
