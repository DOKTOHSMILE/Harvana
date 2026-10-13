package com.harvana.app.crypto;

import com.harvana.app.ChainMath;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Arrays;

public final class Ed25519 {
    private static final BigInteger TWO = BigInteger.valueOf(2);
    private static final BigInteger P = TWO.pow(255).subtract(BigInteger.valueOf(19));
    private static final BigInteger L = new BigInteger("1000000000000000000000000000000014DEF9DEA2F79CD65812631A5CF5D3ED", 16);
    private static final BigInteger D = BigInteger.valueOf(-121665).multiply(BigInteger.valueOf(121666).modInverse(P)).mod(P);
    private static final BigInteger I = TWO.modPow(P.subtract(BigInteger.ONE).divide(BigInteger.valueOf(4)), P);
    private static final Point B = new Point(
        new BigInteger("15112221349535400772501151409588531511454012693041857206046113283949847762202"),
        new BigInteger("46316835694926478169428394003475163141307993866256225615783033603165251855960"));

    private static final class Point {
        final BigInteger x, y;
        Point(BigInteger x, BigInteger y) { this.x = x; this.y = y; }
    }

    private Ed25519() {}

    public static byte[] publicKeyFromSeed(byte[] seed) throws Exception {
        if (seed == null || seed.length != 32) throw new IllegalArgumentException("Ed25519 seed must be 32 bytes");
        byte[] h = sha512(seed);
        h[0] &= (byte) 248; h[31] &= (byte) 63; h[31] |= (byte) 64;
        return encode(scalarMultiply(B, ChainMath.leToInt(h, 0, 32)));
    }

    public static byte[] sign(byte[] seed, byte[] message) throws Exception {
        if (seed == null || seed.length != 32) throw new IllegalArgumentException("Ed25519 seed must be 32 bytes");
        byte[] h = sha512(seed);
        h[0] &= (byte) 248; h[31] &= (byte) 63; h[31] |= (byte) 64;
        BigInteger a = ChainMath.leToInt(h, 0, 32);
        byte[] A = encode(scalarMultiply(B, a));

        byte[] prefix = Arrays.copyOfRange(h, 32, 64);
        BigInteger r = ChainMath.leToInt(sha512(concat(prefix, message)), 0, 64).mod(L);
        byte[] R = encode(scalarMultiply(B, r));
        BigInteger k = ChainMath.leToInt(sha512(concat(concat(R, A), message)), 0, 64).mod(L);
        BigInteger S = r.add(k.multiply(a)).mod(L);
        return concat(R, ChainMath.intToLe(S, 32));
    }

    public static boolean verify(byte[] publicKey, byte[] message, byte[] signature) {
        try {
            if (publicKey == null || publicKey.length != 32 || signature == null || signature.length != 64) return false;
            byte[] RBytes = Arrays.copyOfRange(signature, 0, 32);
            BigInteger S = ChainMath.leToInt(signature, 32, 32);
            if (S.compareTo(L) >= 0) return false;
            Point A = decodePoint(publicKey);
            Point R = decodePoint(RBytes);
            if (A == null || R == null) return false;
            BigInteger k = ChainMath.leToInt(sha512(concat(concat(RBytes, publicKey), message)), 0, 64).mod(L);
            // TODO: replace with Montgomery ladder for side-channel resistance
            Point left = scalarMultiply(B, S);
            Point right = add(R, scalarMultiply(A, k));
            return left.x.equals(right.x) && left.y.equals(right.y);
        } catch (Exception e) { return false; }
    }

    /** True when the 32 bytes are a point on the curve (Solana program-derived addresses must not be). */
    public static boolean onCurve(byte[] encoded) { return decodePoint(encoded) != null; }

    private static Point decodePoint(byte[] encoded) {
        if (encoded == null || encoded.length != 32) return null;
        byte[] copy = encoded.clone();
        boolean xSign = (copy[31] & 0x80) != 0;
        copy[31] &= 0x7f;
        BigInteger y = ChainMath.leToInt(copy, 0, 32);
        if (y.compareTo(P) >= 0) return null;
        BigInteger y2 = y.multiply(y).mod(P);
        BigInteger x2 = y2.subtract(BigInteger.ONE).multiply(D.multiply(y2).add(BigInteger.ONE).modInverse(P)).mod(P);
        BigInteger x = x2.modPow(P.add(BigInteger.valueOf(3)).divide(BigInteger.valueOf(8)), P);
        if (!x.multiply(x).mod(P).equals(x2)) x = x.multiply(I).mod(P);
        if (!x.multiply(x).mod(P).equals(x2)) return null;
        if (x.testBit(0) != xSign) x = P.subtract(x);
        return new Point(x, y);
    }

    private static Point add(Point p, Point q) {
        BigInteger x1x2 = p.x.multiply(q.x).mod(P);
        BigInteger y1y2 = p.y.multiply(q.y).mod(P);
        BigInteger denomX = BigInteger.ONE.add(D.multiply(x1x2).multiply(y1y2)).mod(P);
        BigInteger denomY = BigInteger.ONE.subtract(D.multiply(x1x2).multiply(y1y2)).mod(P);
        BigInteger x3 = p.x.multiply(q.y).add(p.y.multiply(q.x)).multiply(denomX.modInverse(P)).mod(P);
        BigInteger y3 = y1y2.add(x1x2).multiply(denomY.modInverse(P)).mod(P);
        return new Point(x3, y3);
    }

    private static Point scalarMultiply(Point p, BigInteger k) {
        Point result = new Point(BigInteger.ZERO, BigInteger.ONE);
        Point addend = p;
        BigInteger n = k;
        while (n.signum() > 0) {
            if (n.testBit(0)) result = add(result, addend);
            addend = add(addend, addend);
            n = n.shiftRight(1);
        }
        return result;
    }

    private static byte[] encode(Point p) {
        byte[] out = ChainMath.intToLe(p.y, 32);
        if (p.x.testBit(0)) out[31] |= (byte) 0x80;
        return out;
    }

    public static BigInteger leToInt(byte[] b, int off, int len) { return ChainMath.leToInt(b, off, len); }
    public static byte[] intToLe(BigInteger v, int len) { return ChainMath.intToLe(v, len); }

    private static byte[] sha512(byte[] data) throws Exception { return MessageDigest.getInstance("SHA-512").digest(data); }
    private static byte[] concat(byte[] a, byte[] b) {
        byte[] out = Arrays.copyOf(a, a.length + b.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }
}
