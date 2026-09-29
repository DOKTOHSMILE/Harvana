package com.harvanna.app.chain;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** Golden values produced by the node's own P2PListingCreateRequest.getCanonicalMessage(). */
public class P2pCanonicalTest {

    @Test public void createListing_sell() {
        assertEquals("P2P_CREATE_LISTING:HV0xabc0:SELL:1000:200:900:24:45:150000:7",
            P2pCanonical.createListing("HV0xabc0", "SELL", 1000L, 200L, 900L, 24L, 45L, 150000L, 7L));
    }

    @Test public void createListing_buy() {
        assertEquals("P2P_CREATE_LISTING:HV0xabc1:BUY:1001:201:901:25:46:150001:8",
            P2pCanonical.createListing("HV0xabc1", "BUY", 1001L, 201L, 901L, 25L, 46L, 150001L, 8L));
    }
}
