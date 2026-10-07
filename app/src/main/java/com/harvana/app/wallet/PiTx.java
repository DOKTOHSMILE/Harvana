package com.harvana.app.wallet;

import com.harvana.app.crypto.Ed25519;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * A Pi Network payment, built and signed on the phone. Pi's chain is Stellar's, so this is a Stellar transaction envelope
 * (XDR) with one operation:
 *   PAYMENT of native Pi to an account that exists, or
 *   CREATE_ACCOUNT (with the amount as its starting balance) to an account the network has not seen yet.
 * Signed over SHA-256(SHA-256("Pi Network") || ENVELOPE_TYPE_TX || transaction). Pure Java: checked byte for byte against
 * the official Stellar SDK (tools/pi/pi_tx_check.py).
 */
public final class PiTx {
    private PiTx() {}

    public static final String NETWORK_PASSPHRASE = "Pi Network";
    /** 1 Pi = 10,000,000 stroops. */
    public static final long STROOPS_PER_PI = 10_000_000L;

    /**
     * The base64 envelope to POST to /transactions as {@code tx}.
     * @param seed       the Pi signing seed (PiKeys.piSeed)
     * @param sequence   the source account's current sequence number (the transaction uses sequence + 1)
     * @param destination the recipient's G... address
     * @param stroops    amount
     * @param fee        total fee in stroops for the one operation (Pi's base fee is 0.01 Pi = 100,000)
     * @param maxTime    unix seconds after which the network refuses it (0 = no limit)
     * @param memo       up to 28 bytes of text, or "" for none
     * @param create     true when the destination account does not exist yet
     */
    public static String envelope(byte[] seed, long sequence, String destination, long stroops, int fee, long maxTime,
                                  String memo, boolean create) throws Exception {
        byte[] pub = Ed25519.publicKeyFromSeed(seed);
        byte[] dest = decodeAccount(destination);
        byte[] memoBytes = memo == null ? new byte[0] : memo.getBytes(StandardCharsets.UTF_8);
        if (memoBytes.length > 28) throw new IllegalArgumentException("MEMO_TOO_LONG");

        ByteArrayOutputStream txb = new ByteArrayOutputStream();
        DataOutputStream tx = new DataOutputStream(txb);
        tx.writeInt(0); tx.write(pub);                     // source: MuxedAccount KEY_TYPE_ED25519
        tx.writeInt(fee);                                  // fee (uint32)
        tx.writeLong(sequence + 1);                        // seqNum
        tx.writeInt(1);                                    // Preconditions: PRECOND_TIME
        tx.writeLong(0); tx.writeLong(maxTime);            // TimeBounds min, max
        if (memoBytes.length == 0) tx.writeInt(0);         // MEMO_NONE
        else { tx.writeInt(1); writeOpaque(tx, memoBytes); }   // MEMO_TEXT
        tx.writeInt(1);                                    // one operation
        tx.writeInt(0);                                    // no per-operation source
        if (create) {
            tx.writeInt(0);                                // CREATE_ACCOUNT
            tx.writeInt(0); tx.write(dest);                // destination AccountID (PUBLIC_KEY_TYPE_ED25519)
            tx.writeLong(stroops);                         // startingBalance
        } else {
            tx.writeInt(1);                                // PAYMENT
            tx.writeInt(0); tx.write(dest);                // destination MuxedAccount
            tx.writeInt(0);                                // asset: native
            tx.writeLong(stroops);
        }
        tx.writeInt(0);                                    // ext
        byte[] txBytes = txb.toByteArray();

        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] networkId = sha.digest(NETWORK_PASSPHRASE.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream payload = new ByteArrayOutputStream();
        payload.write(networkId);
        payload.write(new byte[]{0, 0, 0, 2});            // ENVELOPE_TYPE_TX
        payload.write(txBytes);
        byte[] hash = sha.digest(payload.toByteArray());
        byte[] sig = Ed25519.sign(seed, hash);

        ByteArrayOutputStream envb = new ByteArrayOutputStream();
        DataOutputStream env = new DataOutputStream(envb);
        env.writeInt(2);                                   // ENVELOPE_TYPE_TX
        env.write(txBytes);
        env.writeInt(1);                                   // one signature
        env.write(pub, 28, 4);                             // hint: last 4 bytes of the public key
        writeOpaque(env, sig);
        return base64(envb.toByteArray());
    }

    /** "12.5" -> stroops; rejects more than 7 decimals or non-positive amounts. */
    public static long toStroops(String pi) {
        java.math.BigDecimal v = new java.math.BigDecimal(pi.trim());
        if (v.signum() <= 0 || v.scale() > 7) throw new IllegalArgumentException("BAD_AMOUNT");
        return v.movePointRight(7).longValueExact();
    }

    static byte[] decodeAccount(String g) {
        if (!PiKeys.isValidAddress(g)) throw new IllegalArgumentException("BAD_ADDRESS");
        String B32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int buffer = 0, bits = 0;
        for (char ch : g.toCharArray()) {
            buffer = (buffer << 5) | B32.indexOf(ch);
            bits += 5;
            if (bits >= 8) { out.write((buffer >> (bits - 8)) & 0xFF); bits -= 8; }
        }
        byte[] raw = out.toByteArray();
        return java.util.Arrays.copyOfRange(raw, 1, 33);
    }

    /** Standard base64 with padding (java.util.Base64 needs Android 8; the app supports Android 6). */
    static String base64(byte[] b) {
        String t = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < b.length; i += 3) {
            int n = (b[i] & 0xFF) << 16 | (i + 1 < b.length ? (b[i + 1] & 0xFF) << 8 : 0) | (i + 2 < b.length ? b[i + 2] & 0xFF : 0);
            out.append(t.charAt((n >> 18) & 63)).append(t.charAt((n >> 12) & 63));
            out.append(i + 1 < b.length ? t.charAt((n >> 6) & 63) : '=');
            out.append(i + 2 < b.length ? t.charAt(n & 63) : '=');
        }
        return out.toString();
    }

    private static void writeOpaque(DataOutputStream o, byte[] b) throws java.io.IOException {
        o.writeInt(b.length);
        o.write(b);
        int pad = (4 - b.length % 4) % 4;
        for (int i = 0; i < pad; i++) o.write(0);
    }
}
