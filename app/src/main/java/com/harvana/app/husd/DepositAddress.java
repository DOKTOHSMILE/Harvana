package com.harvana.app.husd;

import org.bouncycastle.crypto.digests.KeccakDigest;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * A member's hUSD deposit address on an EVM chain, worked out on the phone with the same CREATE2 formula as
 * DepositForwarderFactory.addressFor in the node repository's bridge/contracts. Pure Java so it is tested against
 * the contract (DepositAddressTest).
 */
public final class DepositAddress {
    private DepositAddress() {}

    /** keccak256(0xff ++ factory ++ keccak256(harvanaAddress) ++ initCodeHash)[12:], as an EIP-55 address. */
    public static String of(String factory, String initCodeHash, String harvanaAddress) {
        byte[] f = hex(factory), code = hex(initCodeHash);
        if (f.length != 20 || code.length != 32) throw new IllegalArgumentException("bad factory config");
        byte[] salt = keccak(harvanaAddress.getBytes(StandardCharsets.UTF_8));
        byte[] buf = new byte[1 + 20 + 32 + 32];
        buf[0] = (byte) 0xff;
        System.arraycopy(f, 0, buf, 1, 20);
        System.arraycopy(salt, 0, buf, 21, 32);
        System.arraycopy(code, 0, buf, 53, 32);
        byte[] h = keccak(buf);
        byte[] addr = new byte[20];
        System.arraycopy(h, 12, addr, 0, 20);
        return checksum(addr);
    }

    static byte[] keccak(byte[] in) {
        KeccakDigest d = new KeccakDigest(256);
        d.update(in, 0, in.length);
        byte[] out = new byte[32];
        d.doFinal(out, 0);
        return out;
    }

    /** EIP-55 mixed-case address. */
    public static String checksum(byte[] addr) {
        StringBuilder lower = new StringBuilder();
        for (byte b : addr) lower.append(String.format(Locale.US, "%02x", b));
        byte[] h = keccak(lower.toString().getBytes(StandardCharsets.US_ASCII));
        StringBuilder out = new StringBuilder("0x");
        for (int i = 0; i < 40; i++) {
            char ch = lower.charAt(i);
            int nib = (h[i / 2] >> (i % 2 == 0 ? 4 : 0)) & 0xf;
            out.append(nib >= 8 ? Character.toUpperCase(ch) : ch);
        }
        return out.toString();
    }

    public static byte[] hex(String s) {
        String h = s == null ? "" : s.trim().replaceFirst("^0x", "");
        if (h.length() % 2 != 0) return new byte[0];
        byte[] b = new byte[h.length() / 2];
        for (int i = 0; i < b.length; i++) b[i] = (byte) Integer.parseInt(h.substring(2 * i, 2 * i + 2), 16);
        return b;
    }
}
