package com.harvana.app.walletconnect;

import android.util.Base64;

import org.bouncycastle.crypto.agreement.X25519Agreement;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.modes.ChaCha20Poly1305;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.HKDFParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.X25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.X25519PublicKeyParameters;
import org.bouncycastle.crypto.signers.Ed25519Signer;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

/**
 * The WalletConnect v2 cryptography: X25519 key agreement, HKDF-SHA256, ChaCha20-Poly1305 envelopes (type 0 and 1),
 * topics (SHA-256 of the symmetric key) and the Ed25519-signed relay token (did:key issuer).
 */
final class WcCrypto {
    private WcCrypto() {}

    private static final SecureRandom RNG = new SecureRandom();

    static byte[] random(int n) { byte[] b = new byte[n]; RNG.nextBytes(b); return b; }

    /** {private, public}, 32 bytes each. */
    static byte[][] x25519Pair() {
        X25519PrivateKeyParameters k = new X25519PrivateKeyParameters(RNG);
        return new byte[][]{k.getEncoded(), k.generatePublicKey().getEncoded()};
    }

    /** The session's symmetric key: HKDF-SHA256 (no salt, no info) over the X25519 shared secret. */
    static byte[] sharedKey(byte[] selfPrivate, byte[] peerPublic) {
        X25519Agreement a = new X25519Agreement();
        a.init(new X25519PrivateKeyParameters(selfPrivate, 0));
        byte[] secret = new byte[a.getAgreementSize()];
        a.calculateAgreement(new X25519PublicKeyParameters(peerPublic, 0), secret, 0);
        HKDFBytesGenerator h = new HKDFBytesGenerator(new SHA256Digest());
        h.init(new HKDFParameters(secret, null, null));
        byte[] out = new byte[32];
        h.generateBytes(out, 0, 32);
        java.util.Arrays.fill(secret, (byte) 0);
        return out;
    }

    static String topicOf(byte[] symKey) {
        try { return hex(MessageDigest.getInstance("SHA-256").digest(symKey)); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }

    /** Type-0 envelope: 0x00 | iv(12) | ChaCha20-Poly1305(json), base64. */
    static String seal(byte[] key, String json) throws Exception {
        byte[] iv = random(12);
        byte[] ct = aead(true, key, iv, json.getBytes(StandardCharsets.UTF_8));
        byte[] out = new byte[1 + 12 + ct.length];
        out[0] = 0;
        System.arraycopy(iv, 0, out, 1, 12);
        System.arraycopy(ct, 0, out, 13, ct.length);
        return Base64.encodeToString(out, Base64.NO_WRAP);
    }

    /** Opens a type-0 or type-1 envelope (type 1 carries the sender's public key before the iv). */
    static String open(byte[] key, String b64) throws Exception {
        byte[] b = Base64.decode(b64, Base64.DEFAULT);
        int at = b[0] == 1 ? 33 : 1;
        byte[] iv = java.util.Arrays.copyOfRange(b, at, at + 12);
        byte[] ct = java.util.Arrays.copyOfRange(b, at + 12, b.length);
        return new String(aead(false, key, iv, ct), StandardCharsets.UTF_8);
    }

    private static byte[] aead(boolean encrypt, byte[] key, byte[] iv, byte[] in) throws Exception {
        ChaCha20Poly1305 c = new ChaCha20Poly1305();
        c.init(encrypt, new AEADParameters(new KeyParameter(key), 128, iv, null));
        byte[] out = new byte[c.getOutputSize(in.length)];
        int n = c.processBytes(in, 0, in.length, out, 0);
        n += c.doFinal(out, n);
        return n == out.length ? out : java.util.Arrays.copyOf(out, n);
    }

    /** The relay's auth token: an EdDSA JWT whose issuer is this client's did:key. Valid for a day. */
    static String relayJwt(byte[] seed, String aud) {
        Ed25519PrivateKeyParameters k = new Ed25519PrivateKeyParameters(seed, 0);
        byte[] pub = k.generatePublicKey().getEncoded();
        long now = System.currentTimeMillis() / 1000;
        String header = "{\"alg\":\"EdDSA\",\"typ\":\"JWT\"}";
        String payload = "{\"iss\":\"" + didKey(pub) + "\",\"sub\":\"" + hex(random(32)) + "\",\"aud\":\"" + aud
                + "\",\"iat\":" + now + ",\"exp\":" + (now + 86_400) + "}";
        String data = b64url(header.getBytes(StandardCharsets.UTF_8)) + "." + b64url(payload.getBytes(StandardCharsets.UTF_8));
        Ed25519Signer s = new Ed25519Signer();
        s.init(true, k);
        byte[] d = data.getBytes(StandardCharsets.US_ASCII);
        s.update(d, 0, d.length);
        return data + "." + b64url(s.generateSignature());
    }

    /** did:key:z… for an Ed25519 public key (multicodec 0xed01, base58btc). */
    static String didKey(byte[] pub) {
        byte[] m = new byte[2 + pub.length];
        m[0] = (byte) 0xed;
        m[1] = 0x01;
        System.arraycopy(pub, 0, m, 2, pub.length);
        return "did:key:z" + base58(m);
    }

    static String b64url(byte[] b) { return Base64.encodeToString(b, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING); }

    static String hex(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (byte x : b) sb.append(String.format("%02x", x & 0xff));
        return sb.toString();
    }

    static byte[] unhex(String h) {
        String s = h.startsWith("0x") ? h.substring(2) : h;
        byte[] out = new byte[s.length() / 2];
        for (int i = 0; i < out.length; i++) out[i] = (byte) Integer.parseInt(s.substring(2 * i, 2 * i + 2), 16);
        return out;
    }

    private static final String B58 = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";

    static String base58(byte[] in) {
        BigInteger n = new BigInteger(1, in);
        StringBuilder sb = new StringBuilder();
        BigInteger base = BigInteger.valueOf(58);
        while (n.signum() > 0) { BigInteger[] qr = n.divideAndRemainder(base); sb.append(B58.charAt(qr[1].intValue())); n = qr[0]; }
        for (byte b : in) { if (b != 0) break; sb.append('1'); }
        return sb.reverse().toString();
    }

    static byte[] unbase58(String s) {
        BigInteger n = BigInteger.ZERO;
        for (char ch : s.toCharArray()) {
            int d = B58.indexOf(ch);
            if (d < 0) throw new IllegalArgumentException("Not base58");
            n = n.multiply(BigInteger.valueOf(58)).add(BigInteger.valueOf(d));
        }
        byte[] raw = n.toByteArray();
        int lead = 0;
        while (lead < s.length() && s.charAt(lead) == '1') lead++;
        int strip = raw.length > 1 && raw[0] == 0 ? 1 : 0;
        byte[] out = new byte[lead + raw.length - strip];
        System.arraycopy(raw, strip, out, lead, raw.length - strip);
        return out;
    }
}
