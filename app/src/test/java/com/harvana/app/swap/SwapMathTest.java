package com.harvana.app.swap;

import org.junit.Test;

import static org.junit.Assert.*;

public class SwapMathTest {

    @Test public void amountsParseToRawUnitsAndRejectBadInput() {
        assertEquals(0, SwapMath.toRaw("HARV", ""));
        assertEquals(0, SwapMath.toRaw("HARV", "0"));
        assertEquals(-1, SwapMath.toRaw("HARV", "-1"));
        assertEquals(-1, SwapMath.toRaw("HARV", "abc"));
        assertEquals(250_000_000L, SwapMath.toRaw("HARV", "2.5"));
        assertEquals(1_500_000L, SwapMath.toRaw("HNGN", "1.5"));
        assertEquals("7 decimals for a 6-decimal coin", -1, SwapMath.toRaw("HNGN", "1.0000001"));
        assertEquals("trailing zeros are fine", 1_000_000L, SwapMath.toRaw("HNGN", "1.000000000"));
        assertEquals("too large", -1, SwapMath.toRaw("HARV", "999999999999999999999"));
        assertTrue(SwapMath.tooManyDecimals("HARV", "0.123456789"));
        assertEquals("2.5", SwapMath.plain("HARV", 250_000_000L));
    }

    @Test public void minimumReceivedFollowsTheSlippage() {
        assertEquals(990, SwapMath.minOut(1000, 100));
        assertEquals(995, SwapMath.minOut(1000, 50));
        assertEquals(1, SwapMath.minOut(1, 300));
        assertEquals(0, SwapMath.minOut(0, 100));
    }

    @Test public void maxLeavesRoomForTheTFareOnHarvOnly() {
        assertEquals(1000 - 30 - 1, SwapMath.maxSpendable("HARV", 1000, 30));
        assertEquals(500, SwapMath.maxSpendable("HNGN", 500, 30));
        assertEquals(0, SwapMath.maxSpendable("HARV", 10, 30));
    }

    @Test public void theButtonFollowsTheFormState() {
        assertEquals("Create or import a wallet", SwapMath.button(false, false, "1", "HARV", "HNGN", true, 1, 1, 1, 5, "").label);
        assertEquals("Enter an amount", SwapMath.button(true, false, "", "HARV", "HNGN", true, -1, -1, -1, -1, "").label);
        assertEquals("Invalid amount", SwapMath.button(true, false, "1.123456789", "HARV", "HNGN", true, -1, -1, -1, -1, "").label);
        assertEquals("Pair unavailable", SwapMath.button(true, false, "1", "HARV", "HNGN", false, -1, -1, -1, -1, "").label);
        assertEquals("Insufficient balance", SwapMath.button(true, false, "2", "HARV", "HNGN", true, 100_000_000L, -1, 10, 5, "").label);
        SwapMath.Button fare = SwapMath.button(true, false, "1", "HARV", "HNGN", true, 100_000_000L, -1, 10, 5, "");
        assertFalse("1 HARV with exactly 1 HARV leaves nothing for the tFare", fare.enabled);
        assertEquals("Not enough HARV for tFare", SwapMath.button(true, false, "1", "HNGN", "HARV", true, 5_000_000L, 5, 10, 5, "").label);
        assertEquals("Quote unavailable", SwapMath.button(true, false, "1", "HARV", "HNGN", true, -1, -1, -1, -1, "No liquidity").label);
        assertEquals("Getting quote…", SwapMath.button(true, false, "1", "HARV", "HNGN", true, -1, -1, -1, -1, "").label);
        assertEquals("Amount too small", SwapMath.button(true, false, "1", "HARV", "HNGN", true, -1, -1, -1, 0, "").label);
        SwapMath.Button ok = SwapMath.button(true, false, "1", "HARV", "HNGN", true, 200_000_000L, -1, 10, 5, "");
        assertEquals("Review Swap", ok.label);
        assertTrue(ok.enabled);
        assertFalse("no double submit", SwapMath.button(true, true, "1", "HARV", "HNGN", true, 200_000_000L, -1, 10, 5, "").enabled);
    }

    @Test public void statusesAreTheNodesOwn() {
        assertEquals("Completed", SwapMath.statusWord("DONE"));
        assertEquals("Refunded", SwapMath.statusWord("REFUNDED"));
        assertEquals("Confirming", SwapMath.statusWord("PENDING"));
        assertEquals(1, SwapMath.statusTone("DONE"));
        assertEquals(-1, SwapMath.statusTone("REFUNDED"));
        assertEquals(0, SwapMath.statusTone("PENDING"));
    }
}
