package com.harvanna.app.wallet;

import android.content.Context;
import android.util.Base64;
import com.harvanna.app.ChainConstants;
import com.harvanna.app.ChainMath;
import com.harvanna.app.crypto.Ed25519;
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
    
    /**
     * Generate a standard 12-word recovery phrase (128-bit entropy + 4-bit checksum).
     * Returns null if the BIP-39 wordlist cannot be loaded.
     */    
    public static String generateMnemonic(Context context) {
        loadWordList(context);
        
        // STRICT CHECK: Ensure exact 2048 BIP-39 wordlist size
        if (WORDLIST == null || WORDLIST.size() != 2048) {
            return null; // Soft-fail to prevent generating weak keys
        }
        
        try {
            // 1. Generate 128 bits (16 bytes) of secure entropy
            byte[] entropy = new byte[16];
            new SecureRandom().nextBytes(entropy);
            return mnemonicFromEntropy(context, entropy);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * ADDED for the Harvanna Access Card digit-code feature: builds a mnemonic
     * from GIVEN 16-byte entropy instead of generating fresh random entropy.
     * This is the exact same bit-packing generateMnemonic() already used —
     * factored out so the card system can encrypt just the 16-byte entropy
     * (instead of the full ~89-byte word string) and regenerate the words
     * deterministically on restore, cutting the printable digit code from
     * ~320 digits down to ~106 with zero loss of security.
     *
     * @param entropy exactly 16 bytes. Returns null otherwise (matches this
     *                class's existing "never throw, return null" convention).
     */
    public static String mnemonicFromEntropy(Context context, byte[] entropy) {
        loadWordList(context);
        if (WORDLIST == null || WORDLIST.size() != 2048) return null;
        if (entropy == null || entropy.length != 16) return null;

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(entropy);
            byte checksumByte = hash[0];

            boolean[] bits = new boolean[132];
            for (int i = 0; i < 16; i++) {
                for (int j = 0; j < 8; j++) {
                    bits[i * 8 + j] = (entropy[i] & (1 << (7 - j))) != 0;
                }
            }
            for (int i = 0; i < 4; i++) {
                bits[128 + i] = (checksumByte & (1 << (7 - i))) != 0;
            }

            StringBuilder mnemonic = new StringBuilder();
            for (int i = 0; i < 12; i++) {
                int wordIndex = 0;
                for (int j = 0; j < 11; j++) {
                    wordIndex <<= 1;
                    if (bits[i * 11 + j]) {
                        wordIndex |= 1;
                    }
                }
                if (i > 0) mnemonic.append(' ');
                mnemonic.append(WORDLIST.get(wordIndex));
            }
            return mnemonic.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * ADDED for the Harvanna Access Card digit-code feature: the inverse of
     * mnemonicFromEntropy() — extracts the original 16-byte entropy back
     * out of a valid 12-word mnemonic. Validates the BIP-39 checksum in
     * the process (same check as isValidMnemonic()) and returns null if
     * it doesn't verify, so a card is never created from a bad phrase.
     */
    public static byte[] entropyFromMnemonic(Context context, String mnemonic) {
        if (mnemonic == null) return null;
        String[] words = mnemonic.trim().toLowerCase().split("\\s+");
        if (words.length != 12) return null;

        loadWordList(context);
        if (WORDLIST == null || WORDLIST.size() != 2048) return null;

        int[] wordIndices = new int[12];
        for (int i = 0; i < 12; i++) {
            int idx = WORDLIST.indexOf(words[i]);
            if (idx < 0) return null;
            wordIndices[i] = idx;
        }

        try {
            boolean[] bits = new boolean[132];
            for (int i = 0; i < 12; i++) {
                for (int j = 0; j < 11; j++) {
                    bits[i * 11 + j] = (wordIndices[i] & (1 << (10 - j))) != 0;
                }
            }

            byte[] entropy = new byte[16];
            for (int i = 0; i < 16; i++) {
                int b = 0;
                for (int j = 0; j < 8; j++) {
                    b <<= 1;
                    if (bits[i * 8 + j]) b |= 1;
                }
                entropy[i] = (byte) b;
            }

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(entropy);
            byte checksumByte = hash[0];
            for (int i = 0; i < 4; i++) {
                boolean actualBit = bits[128 + i];
                boolean expectedBit = (checksumByte & (1 << (7 - i))) != 0;
                if (actualBit != expectedBit) return null; // checksum mismatch
            }
            return entropy;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Derive a deterministic HV0x… address from a 12-word mnemonic.
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
