package com.harvana.app.chain;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Byte layout the node verifies for a v2-signed member lock (node docs/ANDROID_V2_SIGNING.md, mirror of
 * consensus/CanonicalCodec.signingBytes): domain, then each field as int32-length-prefixed UTF-8 strings and
 * big-endian ints/longs. No JSON, no separators.
 */
public final class LockV2 {
    public static final String DOMAIN = "HARVANNA_TX_V2";

    private LockV2() {}

    public static byte[] signingBytes(int version, String chainId, String type, String from, String to, String assetId,
                                      long amount, long maxFee, long gasLimit, long nonce, long expiry,
                                      String memo, String payload) {
        try {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            DataOutputStream d = new DataOutputStream(b);
            str(d, DOMAIN);
            d.writeInt(version);
            str(d, chainId);
            str(d, type);
            str(d, from);
            str(d, to);
            str(d, assetId);
            d.writeLong(amount);
            d.writeLong(maxFee);
            d.writeLong(gasLimit);
            d.writeLong(nonce);
            d.writeLong(expiry);
            str(d, memo);
            str(d, payload);
            d.flush();
            return b.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("lock encoding failed", e);
        }
    }

    private static void str(DataOutputStream d, String s) throws Exception {
        byte[] x = s == null ? new byte[0] : s.getBytes(StandardCharsets.UTF_8);
        d.writeInt(x.length);
        d.write(x);
    }
}
