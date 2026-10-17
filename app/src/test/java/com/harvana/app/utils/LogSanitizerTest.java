package com.harvana.app.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * JVM unit tests for {@link LogSanitizer}. Pure Java on purpose, so the same
 * tests can run in the in-process VM and in CI without an Android emulator.
 */
public class LogSanitizerTest {

    @Test
    public void address_returnsEmptyForNullOrEmpty() {
        assertEquals("", LogSanitizer.address(null));
        assertEquals("", LogSanitizer.address(""));
    }

    @Test
    public void address_returnsShortStringsUnchanged() {
        // Anything below 12 chars is not an address; pass through.
        assertEquals("hi", LogSanitizer.address("hi"));
        assertEquals("HV0xabcd", LogSanitizer.address("HV0xabcd"));
    }

    @Test
    public void address_shortensHV0x() {
        String addr = "HV0x1234567890abcdef1234567890abcdef12345678";
        String out = LogSanitizer.address(addr);
        assertNotNull(out);
        assertTrue("should start with HV0x", out.startsWith("HV0x"));
        assertTrue("should contain ellipsis", out.contains("…"));
        assertTrue("should end with last 4 hex chars", out.endsWith("3456"));
        // Strictly less than the full address (the whole point).
        assertTrue("should be shorter than the input", out.length() < addr.length());
    }

    @Test
    public void address_shortensAJ0x() {
        String addr = "AJ0xabcdef0123456789abcdef0123456789abcdef01";
        String out = LogSanitizer.address(addr);
        assertTrue(out.startsWith("AJ0x"));
        assertTrue(out.contains("…"));
    }

    @Test
    public void address_shortens0xEthStyle() {
        String addr = "0x1234567890abcdef1234567890abcdef12345678";
        String out = LogSanitizer.address(addr);
        assertTrue(out.startsWith("0x"));
        assertTrue(out.contains("…"));
    }

    @Test
    public void address_passesNonAddressThrough() {
        // A long string that is not an address should NOT be shortened.
        String not = "this is a long sentence that is not a wallet address at all really";
        assertEquals(not, LogSanitizer.address(not));
    }

    @Test
    public void sanitize_passesNullAndEmpty() {
        assertEquals(null, LogSanitizer.sanitize(null));
        assertEquals("", LogSanitizer.sanitize(""));
    }

    @Test
    public void sanitize_passesShortStringUnchanged() {
        String s = "Hello world";
        assertEquals(s, LogSanitizer.sanitize(s));
    }

    @Test
    public void sanitize_replacesEmbeddedAddress() {
        String msg = "Connecting to wallet WS at HV0x1234567890abcdef1234567890abcdef12345678 done";
        String out = LogSanitizer.sanitize(msg);
        assertNotNull(out);
        assertFalse("should not contain full address", out.contains("1234567890abcdef1234567890abcdef12345678"));
        assertTrue("should still contain the rest of the message", out.contains("Connecting to wallet WS at"));
        assertTrue("should still contain 'done'", out.contains("done"));
    }

    @Test
    public void sanitize_handlesMultipleAddresses() {
        String a1 = "HV0x1234567890abcdef1234567890abcdef12345678";
        String a2 = "0xabcdef0123456789abcdef0123456789abcdef01";
        String msg = "from " + a1 + " to " + a2 + " ok";
        String out = LogSanitizer.sanitize(msg);
        assertFalse(out.contains("1234567890abcdef1234567890abcdef12345678"));
        assertFalse(out.contains("abcdef0123456789abcdef0123456789abcdef01"));
        assertTrue(out.contains("from "));
        assertTrue(out.contains("to "));
        assertTrue(out.contains(" ok"));
    }

    @Test
    public void sanitize_passesNonAddressLongStringUnchanged() {
        // A long string with no address-shaped substring should not be rewritten.
        String msg = "BALANCE_UPDATE received from network with 3 confirmations and a non-trivial payload";
        assertEquals(msg, LogSanitizer.sanitize(msg));
    }
}
