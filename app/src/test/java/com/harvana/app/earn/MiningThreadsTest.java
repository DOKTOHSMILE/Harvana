package com.harvana.app.earn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MiningThreadsTest {
    @Test public void topCountIsTheHotOneAndTheCapLeavesACore() {
        assertEquals(3, MiningThreads.safeCap(4));
        assertTrue(MiningThreads.dangerous(4, 4));
        assertFalse(MiningThreads.dangerous(3, 4));
        assertEquals(2, MiningThreads.heat(4, 4));
        assertEquals(0, MiningThreads.heat(1, 4));
    }
}
