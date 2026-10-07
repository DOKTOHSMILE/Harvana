package com.harvana.app.wallet;

import android.content.Context;
import android.util.Base64;
import com.harvana.app.ChainConstants;
import com.harvana.app.ChainMath;
import com.harvana.app.crypto.Ed25519;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * BIP-39 standard mnemonic + HV0x address derivation.
 * Safe for Sketchware: never throws on missing assets — returns null instead.
 */
public class WalletGenerator {
    
    private static volatile List<String> WORDLIST;
    
    private static void loadWordList(Context context) {
        if (WORDLIST != null) return;
        synchronized (WalletGenerator.class) {
            if (WORDLIST != null) return;
            List<String> list = new ArrayList<>();
            if (context != null) {
                try (InputStream is = context.getAssets().open("bip39_english.txt");
                BufferedReader reader = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim().toLowerCase();
                        if (!line.isEmpty()) list.add(line);
                    }
                } catch (Exception ignored) {
                    // Asset missing — fall through to empty list
                }
            }
            WORDLIST = Collections.unmodifiableList(list);
        }
    }
    
    /** New wallets get 24 words: the same phrase then opens the HARV wallet and the Pi wallet (PiKeys). */
    public static final int NEW_WALLET_WORDS = 24;

    /**
     * Generate a standard BIP-39 recovery phrase of {@link #NEW_WALLET_WORDS} words (256-bit entropy + 8-bit checksum).
     * Returns null if the BIP-39 wordlist cannot be loaded.
     */
    public static String generateMnemonic(Context context) {
        return generateMnemonic(context, NEW_WALLET_WORDS);
    }

    /** A phrase of 12, 15, 18, 21 or 24 words. */
    public static String generateMnemonic(Context context, int words) {
        loadWordList(context);
        if (WORDLIST == null || WORDLIST.size() != 2048) return null;   // never generate from a broken list
        if (words % 3 != 0 || words < 12 || words > 24) return null;
        try {
            byte[] entropy = new byte[words * 4 / 3];                   // 12 words -> 16 bytes ... 24 words -> 32 bytes
            new SecureRandom().nextBytes(entropy);
            return mnemonicFromEntropy(context, entropy);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Words from given entropy (16, 20, 24, 28 or 32 bytes; standard BIP-39 bit packing with a checksum of entropy/32 bits).
     * The Access Card stores only this entropy, and rebuilds the words from it on restore. Returns null on bad input.
     */
    public static String mnemonicFromEntropy(Context context, byte[] entropy) {
        loadWordList(context);
        if (WORDLIST == null || WORDLIST.size() != 2048) return null;
        if (entropy == null || entropy.length < 16 || entropy.length > 32 || entropy.length % 4 != 0) return null;
        try {
            int entBits = entropy.length * 8, csBits = entBits / 32, total = entBits + csBits, words = total / 11;
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(entropy);
            boolean[] bits = new boolean[total];
            for (int i = 0; i < entBits; i++) bits[i] = (entropy[i / 8] & (1 << (7 - (i % 8)))) != 0;
            for (int i = 0; i < csBits; i++) bits[entBits + i] = (hash[i / 8] & (1 << (7 - (i % 8)))) != 0;
            StringBuilder mnemonic = new StringBuilder();
            for (int w = 0; w < words; w++) {
                int idx = 0;
                for (int j = 0; j < 11; j++) { idx <<= 1; if (bits[w * 11 + j]) idx |= 1; }
                if (w > 0) mnemonic.append(' ');
                mnemonic.append(WORDLIST.get(idx));
            }
            return mnemonic.toString();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * The inverse: the entropy inside a valid phrase of 12, 15, 18, 21 or 24 words, after checking every word is in the list
     * and the BIP-39 checksum verifies. Returns null otherwise, so nothing is ever built from a mistyped phrase.
     */
    public static byte[] entropyFromMnemonic(Context context, String mnemonic) {
        if (mnemonic == null) return null;
        String[] words = mnemonic.trim().toLowerCase().split("\\s+");
        int n = words.length;
        if (n < 12 || n > 24 || n % 3 != 0) return null;
        loadWordList(context);
        if (WORDLIST == null || WORDLIST.size() != 2048) return null;
        try {
            int total = n * 11, csBits = total / 33, entBits = total - csBits;
            boolean[] bits = new boolean[total];
            for (int w = 0; w < n; w++) {
                int idx = WORDLIST.indexOf(words[w]);
                if (idx < 0) return null;
                for (int j = 0; j < 11; j++) bits[w * 11 + j] = (idx & (1 << (10 - j))) != 0;
            }
            byte[] entropy = new byte[entBits / 8];
            for (int i = 0; i < entBits; i++) if (bits[i]) entropy[i / 8] |= (byte) (1 << (7 - (i % 8)));
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(entropy);
            for (int i = 0; i < csBits; i++) {
                boolean expected = (hash[i / 8] & (1 << (7 - (i % 8)))) != 0;
                if (bits[entBits + i] != expected) return null;          // checksum mismatch
            }
            return entropy;
        } catch (Exception e) {
            return null;
        }
    }

    /** The Pi Network address ("G...") for the same phrase, or null. See PiKeys. */
    public static String derivePiAddress(String mnemonic) {
        try { return PiKeys.piAddress(mnemonic); } catch (Exception e) { return null; }
    }

    /**
     * Derive a deterministic HV0x… address from a recovery phrase (any BIP-39 length; unchanged since the first wallets).
     * Always returns 44-char "HV0x" + 40 hex, or null on failure.
     */    
    public static String deriveAddressFromMnemonic(String mnemonic) {
        if (mnemonic == null || mnemonic.trim().isEmpty()) return null;
        try {
            byte[] seed = deriveRawSeed(mnemonic);
            // Ed25519 requires exactly 32 bytes
            byte[] pubKey = Ed25519.publicKeyFromSeed(seed);
            return addressFromPublicKey(pubKey);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Secure BIP-39 PBKDF2 seed derivation (Key Stretching).
     */
    private static byte[] deriveRawSeed(String mnemonic) throws Exception {
        char[] chars = mnemonic.trim().toLowerCase().toCharArray();
        byte[] salt = "mnemonic".getBytes(StandardCharsets.UTF_8);
        
        // Key stretching using PBKDF2WithHmacSHA512 (Standard BIP-39 algorithm)
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
        PBEKeySpec spec = new PBEKeySpec(chars, salt, 2048, 512); // 2048 iterations, 512-bit output
        byte[] fullSeed = skf.generateSecret(spec).getEncoded();
        
        // Take the first 32 bytes for Ed25519 seed compatibility
        byte[] ed25519Seed = new byte[32];
        System.arraycopy(fullSeed, 0, ed25519Seed, 0, 32);
        return ed25519Seed;
    }
    
    public static Map<String, Object> createInitialWalletDoc(String address) {
        Map<String, Object> doc = new HashMap<>();
        doc.put("address", address);
        doc.put("nonce", 0L);
        doc.put("balance_harv", 0.0);
        doc.put("assets", new ArrayList<Object>());
        doc.put("createdAt", System.currentTimeMillis());
        doc.put("isContract", false);
        doc.put("contractBytecode", "");
        doc.put("lastActivity", System.currentTimeMillis());
        doc.put("transactionCount", 0L);
        return doc;
    }
    
    /**
     * Verifies mnemonic format, dictionary presence, AND the real BIP-39
     * checksum (the 4 bits encoded in the last word, derived from
     * SHA-256(entropy)) — reverses generateMnemonic()'s exact bit-packing
     * to reconstruct the entropy, recomputes the checksum, and compares.
     */
    public static boolean isValidMnemonic(Context context, String mnemonic) {
        return entropyFromMnemonic(context, mnemonic) != null;
    }
    
    public static String derivePrivateKey(String mnemonic) {
        try {
            return Base64.encodeToString(deriveRawSeed(mnemonic), Base64.NO_WRAP);
        } catch (Exception e) {
            return null;
        }
    }
    
    public static String addressFromPublicKey(byte[] pubKey) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(pubKey);
        String hex = ChainMath.bytesToHex(hash);
        return ChainConstants.ADDRESS_PREFIX + hex.substring(0, ChainConstants.ADDRESS_HEX_LENGTH);
    }
}
