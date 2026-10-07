package com.harvana.app.wallet;

import com.harvana.app.crypto.Ed25519;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;

import javax.crypto.Mac;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * The Pi Network wallet that belongs to the same recovery phrase as the Harvana wallet.
 *
 * Pi's chain is built on Stellar's, and the Pi wallet derives its key the Stellar way (SEP-0005), with Pi's coin number:
 *   BIP-39 seed (PBKDF2-HMAC-SHA512, 2048 rounds, salt "mnemonic")
 *   -> SLIP-0010 ed25519 derivation along m/44'/314159'/0'
 *   -> the 32-byte Ed25519 seed; the public key's StrKey ("G...", 56 characters) is the Pi address.
 * So the same 24 words restore the same Pi wallet here and in Pi's own app.
 *
 * The HARV key from the same phrase is unchanged (WalletGenerator: the first 32 bytes of the BIP-39 seed), so every existing
 * Harvana wallet keeps its address. Pure Java (no Android types) so it is checked against published test vectors.
 */
public final class PiKeys {
    private PiKeys() {}

    /** Pi's coin number in the derivation path (Stellar's is 148). */
    public static final int PI_COIN = 314159;

    /** The Pi signing seed (32 bytes) for a phrase. */
    public static byte[] piSeed(String mnemonic) throws Exception {
        return slip10Ed25519(bip39Seed(mnemonic), new int[]{44, PI_COIN, 0});
    }

    /** The Pi address ("G..."). */
    public static String piAddress(String mnemonic) throws Exception {
        return strKeyAccount(Ed25519.publicKeyFromSeed(piSeed(mnemonic)));
    }

    /** The account address for a path's account number on any Stellar-family coin (used to check against SEP-0005 vectors). */
    public static String address(String mnemonic, int coin, int account) throws Exception {
        return strKeyAccount(Ed25519.publicKeyFromSeed(slip10Ed25519(bip39Seed(mnemonic), new int[]{44, coin, account})));
    }

    /** The full 64-byte BIP-39 seed (no passphrase). */
    public static byte[] bip39Seed(String mnemonic) throws Exception {
        String m = Normalizer.normalize(mnemonic.trim().toLowerCase().replaceAll("\\s+", " "), Normalizer.Form.NFKD);
        PBEKeySpec spec = new PBEKeySpec(m.toCharArray(), "mnemonic".getBytes(StandardCharsets.UTF_8), 2048, 512);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512").generateSecret(spec).getEncoded();
        } finally {
            spec.clearPassword();
        }
    }

    /** SLIP-0010 for ed25519: every step is hardened. Returns the 32-byte private key at the path. */
    static byte[] slip10Ed25519(byte[] seed, int[] path) throws Exception {
        byte[] I = hmac("ed25519 seed".getBytes(StandardCharsets.UTF_8), seed);
        byte[] key = java.util.Arrays.copyOfRange(I, 0, 32);
        byte[] chain = java.util.Arrays.copyOfRange(I, 32, 64);
        for (int index : path) {
            ByteBuffer data = ByteBuffer.allocate(1 + 32 + 4);
            data.put((byte) 0).put(key).putInt(index | 0x80000000);
            I = hmac(chain, data.array());
            key = java.util.Arrays.copyOfRange(I, 0, 32);
            chain = java.util.Arrays.copyOfRange(I, 32, 64);
        }
        return key;
    }

    private static byte[] hmac(byte[] key, byte[] data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA512");
        mac.init(new SecretKeySpec(key, "HmacSHA512"));
        return mac.doFinal(data);
    }

    /** Stellar StrKey for an account public key: version byte 6<<3 ('G'), the key, CRC16-XModem (little-endian), base32. */
    public static String strKeyAccount(byte[] publicKey) {
        byte[] payload = new byte[1 + 32 + 2];
        payload[0] = (byte) (6 << 3);
        System.arraycopy(publicKey, 0, payload, 1, 32);
        int crc = crc16(payload, 33);
        payload[33] = (byte) (crc & 0xFF);
        payload[34] = (byte) ((crc >> 8) & 0xFF);
        return base32(payload);
    }

    /** True for a well-formed Pi/Stellar account address (checks the checksum). */
    public static boolean isValidAddress(String a) {
        if (a == null || a.length() != 56 || a.charAt(0) != 'G') return false;
        byte[] raw = unbase32(a);
        if (raw == null || raw.length != 35 || raw[0] != (byte) (6 << 3)) return false;
        int crc = crc16(raw, 33);
        return raw[33] == (byte) (crc & 0xFF) && raw[34] == (byte) ((crc >> 8) & 0xFF);
    }

    private static int crc16(byte[] b, int len) {
        int crc = 0;
        for (int i = 0; i < len; i++) {
            crc ^= (b[i] & 0xFF) << 8;
            for (int j = 0; j < 8; j++) crc = (crc & 0x8000) != 0 ? ((crc << 1) ^ 0x1021) & 0xFFFF : (crc << 1) & 0xFFFF;
        }
        return crc;
    }

    private static final String B32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    private static String base32(byte[] data) {
        StringBuilder out = new StringBuilder();
        int buffer = 0, bits = 0;
        for (byte d : data) {
            buffer = (buffer << 8) | (d & 0xFF);
            bits += 8;
            while (bits >= 5) { out.append(B32.charAt((buffer >> (bits - 5)) & 31)); bits -= 5; }
        }
        if (bits > 0) out.append(B32.charAt((buffer << (5 - bits)) & 31));
        return out.toString();
    }

    private static byte[] unbase32(String s) {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        int buffer = 0, bits = 0;
        for (char ch : s.toCharArray()) {
            int v = B32.indexOf(ch);
            if (v < 0) return null;
            buffer = (buffer << 5) | v;
            bits += 5;
            if (bits >= 8) { out.write((buffer >> (bits - 8)) & 0xFF); bits -= 8; }
        }
        return out.toByteArray();
    }
}
