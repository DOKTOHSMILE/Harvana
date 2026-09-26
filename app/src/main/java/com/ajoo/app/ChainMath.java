package com.ajoo.app;

import java.math.BigInteger;
import java.util.Map;

public final class ChainMath {

    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    private ChainMath() {}

    // ==========================================================
    // 1. TOKENOMICS & HALVING MATHEMATICS
    // ==========================================================

    /**
     * Calculates the theoretical maximum supply under a geometric halving schedule.
     * Max = 2 * InitialReward * HalvingInterval
     */
    public static double calculateMaxSupply(double initialReward, long halvingInterval) {
        if (halvingInterval <= 0 || initialReward <= 0) return 0.0;
        return 2.0 * initialReward * (double) halvingInterval;
    }

    /**
     * Computes the cumulative circulating supply mined up to the current block height.
     */
    public static double calculateCirculatingSupply(long currentHeight, double initialReward, long halvingInterval) {
        if (halvingInterval <= 0 || currentHeight <= 0 || initialReward <= 0) return 0.0;

        double totalMined = 0.0;
        long remainingBlocks = currentHeight;
        double currentReward = initialReward;

        while (remainingBlocks > 0 && currentReward > 0.00000001) {
            long blocksInCurrentEra = Math.min(remainingBlocks, halvingInterval);
            totalMined += blocksInCurrentEra * currentReward;
            remainingBlocks -= blocksInCurrentEra;
            currentReward /= 2.0;
        }

        return totalMined;
    }

    /**
     * Calculates the block reward for a specific block height.
     */
    public static double currentBlockReward(long currentHeight, double initialReward, long halvingInterval) {
        if (halvingInterval <= 0 || currentHeight < 0) return initialReward;
        int era = (int) (currentHeight / halvingInterval);
        if (era >= 64) return 0.0; // Bit-shift overflow safety cutoff
        return initialReward / Math.pow(2.0, era);
    }

    /**
     * Calculates remaining blocks until the next reward halving event.
     */
    public static long blocksUntilNextHalving(long currentHeight, long halvingInterval) {
        if (halvingInterval <= 0) return 0L;
        if (currentHeight < 0) currentHeight = 0;
        long nextHalvingHeight = ((currentHeight / halvingInterval) + 1) * halvingInterval;
        return nextHalvingHeight - currentHeight;
    }

    /**
     * Returns the current halving era index (0 = initial, 1 = 1st halving, etc.).
     */
    public static int currentHalvingEra(long currentHeight, long halvingInterval) {
        if (halvingInterval <= 0 || currentHeight <= 0) return 0;
        return (int) (currentHeight / halvingInterval);
    }

    // ==========================================================
    // 2. DATA & FIRESTORE FIELD CONVERSIONS
    // ==========================================================

    public static double toDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Map) {
            Object v = ((Map<?, ?>) value).get("doubleValue");
            if (v == null) v = ((Map<?, ?>) value).get("integerValue");
            return toDouble(v);
        }
        if (value instanceof Number) return ((Number) value).doubleValue();
        try { 
            return Double.parseDouble(String.valueOf(value)); 
        } catch (Exception e) { 
            return 0.0; 
        }
    }

    public static long toLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Map) {
            Object v = ((Map<?, ?>) value).get("integerValue");
            if (v == null) v = ((Map<?, ?>) value).get("doubleValue");
            return toLong(v);
        }
        if (value instanceof Number) return ((Number) value).longValue();
        try { 
            return Long.parseLong(String.valueOf(value)); 
        } catch (Exception e) { 
            return 0L; 
        }
    }

    public static boolean toBoolean(Object value) {
        if (value == null) return false;
        if (value instanceof Map) return toBoolean(((Map<?, ?>) value).get("booleanValue"));
        if (value instanceof Boolean) return (Boolean) value;
        return Boolean.parseBoolean(String.valueOf(value));
    }

    public static String toString(Object value, String fallback) {
        if (value == null) return fallback;
        if (value instanceof Map) return toString(((Map<?, ?>) value).get("stringValue"), fallback);
        return value instanceof String ? (String) value : String.valueOf(value);
    }

    // ==========================================================
    // 3. CRYPTOGRAPHIC & BYTE CONVERSIONS
    // ==========================================================

    public static String bytesToHex(byte[] bytes) {
        if (bytes == null) return "";
        char[] out = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xff;
            out[i * 2] = HEX_CHARS[v >>> 4];
            out[i * 2 + 1] = HEX_CHARS[v & 0x0f];
        }
        return new String(out);
    }

    public static BigInteger leToInt(byte[] b, int off, int len) {
        byte[] reversed = new byte[len];
        for (int i = 0; i < len; i++) reversed[len - 1 - i] = b[off + i];
        return new BigInteger(1, reversed);
    }

    public static byte[] intToLe(BigInteger v, int len) {
        byte[] source = v.toByteArray();
        byte[] out = new byte[len];
        for (int i = 0; i < len; i++) {
            int src = source.length - 1 - i;
            out[i] = src >= 0 ? source[src] : 0;
        }
        return out;
    }
}
