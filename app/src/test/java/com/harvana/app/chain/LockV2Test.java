package com.harvana.app.chain;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class LockV2Test {
    private static String hex(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (byte x : b) sb.append(String.format("%02x", x));
        return sb.toString();
    }

    /** Golden value produced with the node's own CanonicalCodec.signingBytes code path. */
    @Test public void matchesNodeEncoding() {
        String from = "HV0x" + "1".repeat(40), to = "HV0x" + "2".repeat(40);
        byte[] b = LockV2.signingBytes(2, "HARVANNA-HARV-1", "CIRCLE_BOND", from, to, "HARV",
            120_000_000_000L, 1_000_000L, 0L, 7L, 1_900_000_000_000L, "CIRCLE_BOND_LOCK:CIRC-abc", "");
        assertEquals("0000000e48415256414e4e415f54585f5632000000020000000f48415256414e4e412d484152562d310000000b434952434c455f424f4e440000002c48563078313131313131313131313131313131313131313131313131313131313131313131313131313131310000002c485630783232323232323232323232323232323232323232323232323232323232323232323232323232323200000004484152560000001bf08eb00000000000000f424000000000000000000000000000000007000001ba60d3380000000019434952434c455f424f4e445f4c4f434b3a434952432d61626300000000", hex(b));
    }

    @Test public void nullStringsEncodeAsEmpty() {
        byte[] a = LockV2.signingBytes(2, "c", "t", "f", "to", "HARV", 1, 2, 0, 3, 0, null, null);
        byte[] b = LockV2.signingBytes(2, "c", "t", "f", "to", "HARV", 1, 2, 0, 3, 0, "", "");
        assertEquals(hex(b), hex(a));
    }
}
