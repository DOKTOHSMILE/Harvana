package com.harvanna.wallet.p2p;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class P2pPriceReferenceTest {

    @Test public void nairaToKobo_rounds() {
        assertEquals(150000L, P2pPriceReference.nairaToKobo(1500.0));
        assertEquals(150050L, P2pPriceReference.nairaToKobo(1500.5));
    }

    @Test public void deviation_signedAndSafe() {
        assertEquals(0.2, P2pPriceReference.deviation(120L, 100L), 1e-9);
        assertEquals(-0.1, P2pPriceReference.deviation(90L, 100L), 1e-9);
        assertEquals(0.0, P2pPriceReference.deviation(100L, 0L), 0.0);
    }

    @Test public void flagThresholdIsFifteenPercent() {
        assertTrue(Math.abs(P2pPriceReference.deviation(116L, 100L)) > P2pPriceReference.DEVIATION_FLAG);
        assertTrue(Math.abs(P2pPriceReference.deviation(114L, 100L)) < P2pPriceReference.DEVIATION_FLAG);
    }
}
