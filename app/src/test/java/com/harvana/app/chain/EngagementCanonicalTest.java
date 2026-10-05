package com.harvana.app.chain;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** The strings the node verifies. A change here without the same change on the node makes every receipt fail. */
public class EngagementCanonicalTest {
    @Test public void readMessageMatchesTheNode() {
        assertEquals("HARVANA_ENGAGEMENT_READ_V1:HV0xabc:article-0001:30000:1800000000000",
                EngagementCanonical.read("HV0xabc", "article-0001", 30_000, 1_800_000_000_000L));
    }

    @Test public void statsMessageMatchesTheNode() {
        assertEquals("HARVANA_ENGAGEMENT_STATS_V1:HV0xabc:1800000000000", EngagementCanonical.stats("HV0xabc", 1_800_000_000_000L));
    }

    @Test public void giftMemoMatchesTheNodePrefix() {
        assertEquals("LEARN_GIFT:article-0001", EngagementCanonical.giftMemo("article-0001"));
    }
}
