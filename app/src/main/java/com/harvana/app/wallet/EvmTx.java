package com.harvana.app.wallet;

import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.digests.KeccakDigest;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.ec.CustomNamedCurves;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.crypto.signers.HMacDSAKCalculator;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.util.Arrays;

/** A legacy Ethereum transaction (type 0, EIP-155). One signer for ETH, BNB, Polygon and Base. */
final class EvmTx {
    private EvmTx() {}

    private static final X9ECParameters CURVE = CustomNamedCurves.getByName("secp256k1");
    private static final ECDomainParameters DOMAIN = new ECDomainParameters(CURVE.getCurve(), CURVE.getG(), CURVE.getN(), CURVE.getH());

    /** Signed raw transaction, hex without 0x. */
    static String raw(byte[] priv, long nonce, BigInteger gasPrice, long gasLimit, byte[] to, BigInteger value, long chainId) {
        byte[] signHash = keccak(rlp(new byte[][]{
                rlpInt(BigInteger.valueOf(nonce)), rlpInt(gasPrice), rlpInt(BigInteger.valueOf(gasLimit)),
                to, rlpInt(value), new byte[0], rlpInt(BigInteger.valueOf(chainId)), new byte[0], new byte[0]
        }));
        BigInteger[] rs = sign(priv, signHash);
        int rec = recoverId(signHash, rs[0], rs[1], priv);
        long v = chainId * 2 + 35 + rec;
        return hex(rlp(new byte[][]{
                rlpInt(BigInteger.valueOf(nonce)), rlpInt(gasPrice), rlpInt(BigInteger.valueOf(gasLimit)),
                to, rlpInt(value), new byte[0], rlpInt(BigInteger.valueOf(v)), rlpInt(rs[0]), rlpInt(rs[1])
        }));
    }

    /** As {@link #raw}, with call data and an optional recipient (empty {@code to} creates a contract). For dApps. */
    static String rawWithData(byte[] priv, long nonce, BigInteger gasPrice, long gasLimit, byte[] to, BigInteger value, byte[] data, long chainId) {
        byte[] t = to == null ? new byte[0] : to, d = data == null ? new byte[0] : data;
        byte[] signHash = keccak(rlp(new byte[][]{
                rlpInt(BigInteger.valueOf(nonce)), rlpInt(gasPrice), rlpInt(BigInteger.valueOf(gasLimit)),
                t, rlpInt(value), d, rlpInt(BigInteger.valueOf(chainId)), new byte[0], new byte[0]
        }));
        BigInteger[] rs = sign(priv, signHash);
        int rec = recoverId(signHash, rs[0], rs[1], priv);
        long v = chainId * 2 + 35 + rec;
        return hex(rlp(new byte[][]{
                rlpInt(BigInteger.valueOf(nonce)), rlpInt(gasPrice), rlpInt(BigInteger.valueOf(gasLimit)),
                t, rlpInt(value), d, rlpInt(BigInteger.valueOf(v)), rlpInt(rs[0]), rlpInt(rs[1])
        }));
    }

    /** A 65-byte Ethereum signature (r ‖ s ‖ v, v = 27 or 28) over a 32-byte hash, hex with 0x. */
    static String signHash65(byte[] priv, byte[] hash) {
        BigInteger[] rs = sign(priv, hash);
        int rec = recoverId(hash, rs[0], rs[1], priv);
        byte[] out = new byte[65];
        byte[] r = rs[0].toByteArray(), s = rs[1].toByteArray();
        System.arraycopy(r, Math.max(0, r.length - 32), out, 32 - Math.min(32, r.length), Math.min(32, r.length));
        System.arraycopy(s, Math.max(0, s.length - 32), out, 64 - Math.min(32, s.length), Math.min(32, s.length));
        out[64] = (byte) (27 + rec);
        return "0x" + hex(out);
    }

    /** Low-s RFC6979 signature over a 32-byte hash. Shared with Bitcoin and Tron. */
    static BigInteger[] sign(byte[] priv, byte[] hash) {
        ECDSASigner signer = new ECDSASigner(new HMacDSAKCalculator(new SHA256Digest()));
        signer.init(true, new ECPrivateKeyParameters(new BigInteger(1, priv), DOMAIN));
        BigInteger[] rs = signer.generateSignature(hash);
        BigInteger half = CURVE.getN().shiftRight(1);
        if (rs[1].compareTo(half) > 0) rs[1] = CURVE.getN().subtract(rs[1]);
        return rs;
    }

    /** Which recovery id (0 or 1) reproduces this private key's Ethereum address. */
    static int recoverId(byte[] hash, BigInteger r, BigInteger s, byte[] priv) {
        byte[] want = ethAddressBytes(priv);
        for (int id = 0; id < 2; id++) {
            ECPoint q = recover(hash, r, s, id);
            if (q != null && Arrays.equals(want, ethAddressBytes(q))) return id;
        }
        throw new IllegalStateException("Could not recover the signing key.");
    }

    static byte[] der(BigInteger r, BigInteger s) {
        byte[] rb = r.toByteArray(), sb = s.toByteArray();
        byte[] out = new byte[6 + rb.length + sb.length];
        out[0] = 0x30;
        out[1] = (byte) (out.length - 2);
        out[2] = 0x02;
        out[3] = (byte) rb.length;
        System.arraycopy(rb, 0, out, 4, rb.length);
        int p = 4 + rb.length;
        out[p] = 0x02;
        out[p + 1] = (byte) sb.length;
        System.arraycopy(sb, 0, out, p + 2, sb.length);
        return out;
    }

    private static ECPoint recover(byte[] hash, BigInteger r, BigInteger s, int recId) {
        BigInteger n = CURVE.getN();
        BigInteger x = r.add(BigInteger.valueOf(recId / 2).multiply(n));
        if (x.compareTo(CURVE.getCurve().getField().getCharacteristic()) >= 0) return null;
        byte[] comp = new byte[33];
        comp[0] = (byte) ((recId & 1) == 0 ? 0x02 : 0x03);
        byte[] xb = x.toByteArray();
        if (xb.length > 32) xb = Arrays.copyOfRange(xb, xb.length - 32, xb.length);
        System.arraycopy(xb, 0, comp, 33 - xb.length, xb.length);
        ECPoint R;
        try { R = CURVE.getCurve().decodePoint(comp); } catch (Exception e) { return null; }
        if (!R.multiply(n).isInfinity()) return null;
        BigInteger e = new BigInteger(1, hash);
        BigInteger rInv = r.modInverse(n);
        return R.multiply(s).add(CURVE.getG().multiply(e.negate().mod(n))).multiply(rInv).normalize();
    }

    private static byte[] ethAddressBytes(byte[] priv) {
        ECPoint p = CURVE.getG().multiply(new BigInteger(1, priv)).normalize();
        return ethAddressBytes(p);
    }

    private static byte[] ethAddressBytes(ECPoint p) {
        byte[] u = p.normalize().getEncoded(false);
        KeccakDigest kd = new KeccakDigest(256);
        kd.update(u, 1, 64);
        byte[] h = new byte[32];
        kd.doFinal(h, 0);
        return Arrays.copyOfRange(h, 12, 32);
    }

    static byte[] keccak(byte[] in) {
        KeccakDigest kd = new KeccakDigest(256);
        kd.update(in, 0, in.length);
        byte[] h = new byte[32];
        kd.doFinal(h, 0);
        return h;
    }

    private static byte[] rlp(byte[][] items) {
        int n = 0;
        byte[][] enc = new byte[items.length][];
        for (int i = 0; i < items.length; i++) { enc[i] = rlpBytes(items[i]); n += enc[i].length; }
        byte[] prefix = lenPrefix(0xc0, n);
        byte[] out = new byte[prefix.length + n];
        System.arraycopy(prefix, 0, out, 0, prefix.length);
        int p = prefix.length;
        for (byte[] e : enc) { System.arraycopy(e, 0, out, p, e.length); p += e.length; }
        return out;
    }

    private static byte[] rlpBytes(byte[] b) {
        if (b.length == 1 && (b[0] & 0xFF) < 0x80) return b;
        byte[] prefix = lenPrefix(0x80, b.length);
        byte[] out = new byte[prefix.length + b.length];
        System.arraycopy(prefix, 0, out, 0, prefix.length);
        System.arraycopy(b, 0, out, prefix.length, b.length);
        return out;
    }

    private static byte[] lenPrefix(int base, int len) {
        if (len < 56) return new byte[]{(byte) (base + len)};
        byte[] lb = BigInteger.valueOf(len).toByteArray();
        if (lb[0] == 0) lb = Arrays.copyOfRange(lb, 1, lb.length);
        byte[] out = new byte[1 + lb.length];
        out[0] = (byte) (base + 55 + lb.length);
        System.arraycopy(lb, 0, out, 1, lb.length);
        return out;
    }

    private static byte[] rlpInt(BigInteger v) {
        if (v.signum() == 0) return new byte[0];
        byte[] b = v.toByteArray();
        if (b[0] == 0) b = Arrays.copyOfRange(b, 1, b.length);
        return b;
    }

    static String hex(byte[] b) {
        StringBuilder s = new StringBuilder(b.length * 2);
        for (byte x : b) s.append(String.format("%02x", x & 0xFF));
        return s.toString();
    }

    static byte[] unhex(String h) {
        String s = h.startsWith("0x") || h.startsWith("0X") ? h.substring(2) : h;
        byte[] out = new byte[s.length() / 2];
        for (int i = 0; i < out.length; i++) out[i] = (byte) Integer.parseInt(s.substring(i * 2, i * 2 + 2), 16);
        return out;
    }
}
