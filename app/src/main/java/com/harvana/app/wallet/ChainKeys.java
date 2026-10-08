package com.harvana.app.wallet;

import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.digests.KeccakDigest;
import org.bouncycastle.crypto.digests.RIPEMD160Digest;
import org.bouncycastle.crypto.ec.CustomNamedCurves;
import org.bouncycastle.math.ec.ECPoint;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Wallets on other chains from the same recovery phrase, at the paths the common wallets use, so the same words also open
 * them in MetaMask, Trust Wallet, Phantom, TronLink, Electrum and so on:
 *
 *   Bitcoin   m/84'/0'/0'/0/0    native SegWit, bc1...
 *   Litecoin  m/84'/2'/0'/0/0    native SegWit, ltc1...
 *   Dogecoin  m/44'/3'/0'/0/0    D...
 *   Ethereum  m/44'/60'/0'/0/0   0x... (the same address on BNB Chain, Polygon, Base, Arbitrum and other EVM chains)
 *   Tron      m/44'/195'/0'/0/0  T...
 *   Solana    m/44'/501'/0'/0'   base58 (ed25519, SLIP-0010)
 *
 * Pure Java plus BouncyCastle (already in the app), so it is checked against reference wallets (tools/ChainKeysCheck.java).
 * Making an address costs nothing: no server is involved.
 */
public final class ChainKeys {
    private ChainKeys() {}

    public enum Chain {
        BTC("Bitcoin", "BTC"), LTC("Litecoin", "LTC"), DOGE("Dogecoin", "DOGE"), ETH("Ethereum & EVM chains", "ETH"),
        TRX("Tron", "TRX"), SOL("Solana", "SOL");
        public final String title, ticker;
        Chain(String title, String ticker) { this.title = title; this.ticker = ticker; }
    }

    private static final X9ECParameters CURVE = CustomNamedCurves.getByName("secp256k1");
    private static final int H = 0x80000000;

    /** The receiving address on a chain for a phrase. */
    public static String address(String mnemonic, Chain chain) throws Exception {
        byte[] seed = PiKeys.bip39Seed(mnemonic);
        switch (chain) {
            case BTC: return segwit(Net.TEST ? "tb" : "bc", pub(derive(seed, btcPath())));
            case LTC: return segwit(Net.TEST ? "tltc" : "ltc", pub(derive(seed, ltcPath())));
            case DOGE: return base58Check((byte) (Net.TEST ? 0x71 : 0x1e), hash160(pub(derive(seed, dogePath()))));
            case ETH: return ethAddress(derive(seed, new int[]{44 | H, 60 | H, H, 0, 0}));
            case TRX: return base58Check((byte) 0x41, ethBytes(derive(seed, new int[]{44 | H, 195 | H, H, 0, 0})));
            case SOL: return base58(com.harvana.app.crypto.Ed25519.publicKeyFromSeed(PiKeys.slip10Ed25519(seed, new int[]{44, 501, 0, 0})));
            default: throw new IllegalArgumentException("chain");
        }
    }

    /** The private key (32 bytes) behind a chain's address; used only to sign, never stored. */
    public static byte[] privateKey(String mnemonic, Chain chain) throws Exception {
        byte[] seed = PiKeys.bip39Seed(mnemonic);
        switch (chain) {
            case BTC: return derive(seed, btcPath());
            case LTC: return derive(seed, ltcPath());
            case DOGE: return derive(seed, dogePath());
            case ETH: return derive(seed, new int[]{44 | H, 60 | H, H, 0, 0});
            case TRX: return derive(seed, new int[]{44 | H, 195 | H, H, 0, 0});
            case SOL: return PiKeys.slip10Ed25519(seed, new int[]{44, 501, 0, 0});
            default: throw new IllegalArgumentException("chain");
        }
    }

    private static int[] btcPath() { return new int[]{84 | H, (Net.TEST ? 1 : 0) | H, H, 0, 0}; }
    private static int[] ltcPath() { return new int[]{84 | H, (Net.TEST ? 1 : 2) | H, H, 0, 0}; }
    private static int[] dogePath() { return new int[]{44 | H, (Net.TEST ? 1 : 3) | H, H, 0, 0}; }

    // ── BIP-32 on secp256k1 ─────────────────────────────────────────────

    static byte[] derive(byte[] seed, int[] path) throws Exception {
        byte[] I = hmac("Bitcoin seed".getBytes(StandardCharsets.UTF_8), seed);
        byte[] k = Arrays.copyOfRange(I, 0, 32), c = Arrays.copyOfRange(I, 32, 64);
        BigInteger n = CURVE.getN();
        for (int index : path) {
            ByteBuffer data = ByteBuffer.allocate(37);
            if ((index & H) != 0) data.put((byte) 0).put(k); else data.put(pub(k));
            data.putInt(index);
            I = hmac(c, data.array());
            BigInteger child = new BigInteger(1, Arrays.copyOfRange(I, 0, 32)).add(new BigInteger(1, k)).mod(n);
            k = fixed32(child);
            c = Arrays.copyOfRange(I, 32, 64);
        }
        return k;
    }

    /** Compressed public key (33 bytes). */
    static byte[] pub(byte[] priv) {
        return CURVE.getG().multiply(new BigInteger(1, priv)).normalize().getEncoded(true);
    }

    private static byte[] ethBytes(byte[] priv) {
        ECPoint p = CURVE.getG().multiply(new BigInteger(1, priv)).normalize();
        byte[] uncompressed = p.getEncoded(false);
        KeccakDigest kd = new KeccakDigest(256);
        kd.update(uncompressed, 1, 64);
        byte[] h = new byte[32];
        kd.doFinal(h, 0);
        return Arrays.copyOfRange(h, 12, 32);
    }

    /** EIP-55 mixed-case checksum address. */
    static String ethAddress(byte[] priv) {
        String hex = hex(ethBytes(priv));
        KeccakDigest kd = new KeccakDigest(256);
        byte[] in = hex.getBytes(StandardCharsets.US_ASCII);
        kd.update(in, 0, in.length);
        byte[] h = new byte[32];
        kd.doFinal(h, 0);
        StringBuilder out = new StringBuilder("0x");
        for (int i = 0; i < 40; i++) {
            char ch = hex.charAt(i);
            int nib = (h[i / 2] >> (i % 2 == 0 ? 4 : 0)) & 0xF;
            out.append(nib >= 8 ? Character.toUpperCase(ch) : ch);
        }
        return out.toString();
    }

    // ── encodings ───────────────────────────────────────────────────────

    static byte[] hash160(byte[] data) throws Exception {
        byte[] sha = MessageDigest.getInstance("SHA-256").digest(data);
        RIPEMD160Digest r = new RIPEMD160Digest();
        r.update(sha, 0, sha.length);
        byte[] out = new byte[20];
        r.doFinal(out, 0);
        return out;
    }

    private static final String B58 = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";

    static String base58(byte[] data) {
        BigInteger v = new BigInteger(1, data);
        StringBuilder sb = new StringBuilder();
        BigInteger b = BigInteger.valueOf(58);
        while (v.signum() > 0) { BigInteger[] qr = v.divideAndRemainder(b); sb.append(B58.charAt(qr[1].intValue())); v = qr[0]; }
        for (byte x : data) { if (x == 0) sb.append('1'); else break; }
        return sb.reverse().toString();
    }

    static String base58Check(byte version, byte[] payload) throws Exception {
        byte[] body = new byte[1 + payload.length];
        body[0] = version;
        System.arraycopy(payload, 0, body, 1, payload.length);
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] check = sha.digest(sha.digest(body));
        byte[] all = Arrays.copyOf(body, body.length + 4);
        System.arraycopy(check, 0, all, body.length, 4);
        return base58(all);
    }

    /** Native SegWit v0 key-hash address (BIP-173 bech32). */
    static String segwit(String hrp, byte[] compressedPub) throws Exception {
        byte[] prog = hash160(compressedPub);
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        data.write(0);                                         // witness version 0
        int acc = 0, bits = 0;
        for (byte b : prog) {
            acc = (acc << 8) | (b & 0xFF); bits += 8;
            while (bits >= 5) { data.write((acc >> (bits - 5)) & 31); bits -= 5; }
        }
        if (bits > 0) data.write((acc << (5 - bits)) & 31);
        byte[] d = data.toByteArray();
        int[] values = new int[hrp.length() * 2 + 1 + d.length + 6];
        int p = 0;
        for (char ch : hrp.toCharArray()) values[p++] = ch >> 5;
        values[p++] = 0;
        for (char ch : hrp.toCharArray()) values[p++] = ch & 31;
        for (byte x : d) values[p++] = x;
        int poly = polymod(values) ^ 1;
        String cs = "qpzry9x8gf2tvdw0s3jn54khce6mua7l";
        StringBuilder out = new StringBuilder(hrp).append('1');
        for (byte x : d) out.append(cs.charAt(x));
        for (int i = 0; i < 6; i++) out.append(cs.charAt((poly >> (5 * (5 - i))) & 31));
        return out.toString();
    }

    private static int polymod(int[] values) {
        int[] gen = {0x3b6a57b2, 0x26508e6d, 0x1ea119fa, 0x3d4233dd, 0x2a1462b3};
        int chk = 1;
        for (int v : values) {
            int b = chk >>> 25;
            chk = ((chk & 0x1ffffff) << 5) ^ v;
            for (int i = 0; i < 5; i++) if (((b >> i) & 1) != 0) chk ^= gen[i];
        }
        return chk;
    }

    private static byte[] hmac(byte[] key, byte[] data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA512");
        mac.init(new SecretKeySpec(key, "HmacSHA512"));
        return mac.doFinal(data);
    }

    private static byte[] fixed32(BigInteger v) {
        byte[] b = v.toByteArray();
        if (b.length == 32) return b;
        byte[] out = new byte[32];
        if (b.length > 32) System.arraycopy(b, b.length - 32, out, 0, 32);
        else System.arraycopy(b, 0, out, 32 - b.length, b.length);
        return out;
    }

    private static String hex(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (byte x : b) sb.append(String.format("%02x", x));
        return sb.toString();
    }
}
