package com.harvana.app.p2p;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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

    @Test public void swapBandMatchesTheNode() {
        // 4,315 kobo: floor max(1, 4315 * 9000 / 10000) = 3,883; ceil 4315 * 11000 / 10000 = 4,746.
        assertEquals(3883L, P2pPriceReference.floorKobo(4315L));
        assertEquals(4746L, P2pPriceReference.ceilKobo(4315L));
        assertTrue(P2pPriceReference.allowed(3883L, 4315L));
        assertTrue(P2pPriceReference.allowed(4746L, 4315L));
        assertFalse(P2pPriceReference.allowed(3882L, 4315L));
        assertFalse(P2pPriceReference.allowed(4747L, 4315L));
        assertTrue(P2pPriceReference.allowed(15_000_000L, 0L));
        assertEquals(1L, P2pPriceReference.floorKobo(1L));
        assertEquals(1L, P2pPriceReference.ceilKobo(1L));
    }
}
