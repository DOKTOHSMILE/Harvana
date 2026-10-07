package com.harvana.app.crypto;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * PBKDF2-HMAC-SHA512 built on HmacSHA512.
 *
 * SecretKeyFactory "PBKDF2WithHmacSHA512" only exists from Android 8.0
 * (API 26); this app supports API 23, where that lookup throws. Output is
 * byte-identical to the platform implementation for the same inputs
 * (password as UTF-8 bytes), so existing PIN hashes and wallets stay valid.
 */
public final class Pbkdf2 {

	private Pbkdf2() {}

	/** @param dkLen output length in bytes, 1..64 (a single HMAC-SHA512 block) */
	public static byte[] hmacSha512(byte[] password, byte[] salt, int iterations, int dkLen) throws Exception {
		if (iterations < 1 || dkLen < 1 || dkLen > 64) throw new IllegalArgumentException("bad PBKDF2 parameters");
		Mac mac = Mac.getInstance("HmacSHA512");
		mac.init(new SecretKeySpec(password, "HmacSHA512"));
		byte[] block = java.util.Arrays.copyOf(salt, salt.length + 4);
		block[salt.length + 3] = 1;                        // INT(1), big-endian
		byte[] u = mac.doFinal(block);
		byte[] t = u.clone();
		for (int i = 1; i < iterations; i++) {
			u = mac.doFinal(u);
			for (int j = 0; j < t.length; j++) t[j] ^= u[j];
		}
		return java.util.Arrays.copyOf(t, dkLen);
	}
}
