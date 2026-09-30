package com.harvanna.app.wallet;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.SecretKeyFactory;

/**
 * Secure storage for wallet key material (mnemonic, private key, public key).
 *
 * Security model:
 *   - Master key lives in Android Keystore (hardware-backed when available)
 *   - Data is encrypted with AES-256-GCM (authenticated encryption)
 *   - Each encryption uses a fresh random 12-byte IV
 *   - Automatically migrates old weak storage on first read
 *
 * Compatible with Sketchware (no Tink / no security-crypto dependency).
 */
public final class SecureWalletStorage {

    private static final String TAG = "SecureWalletStorage";

    // ── Keystore ─────────────────────────────────────────────────────────────
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "harvanna_wallet_master_key_v2";
    private static final int GCM_IV_LENGTH = 12;          // 96 bits (recommended for GCM)
    private static final int GCM_TAG_LENGTH = 128;        // bits

    // ── Preferences ──────────────────────────────────────────────────────────
    private static final String PREFS_NAME = "harvanna_secure_wallet_v2";
    private static final String LEGACY_ENC_PREFS = "harvanna_secure_wallet_enc";   // previous weak AES
    private static final String LEGACY_MNEMONIC_PREFS = "harv_wallet_prefs";
    private static final String LEGACY_KEYS_PREFS = "harvanna_secure_wallet";

    // Old weak secret (only used for one-time migration)
    private static final String LEGACY_APP_SECRET = "HarvannaWalletSecureKey#2026!";

    private SecureWalletStorage() {}

    // ═══════════════════════════════════════════════════════════════════════════
    //  KEYSTORE HELPERS
    // ═══════════════════════════════════════════════════════════════════════════

    private static SecretKey getOrCreateMasterKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
        keyStore.load(null);

        if (keyStore.containsAlias(KEY_ALIAS)) {
            return (SecretKey) keyStore.getKey(KEY_ALIAS, null);
        }

        // Create a new AES-256 key that can only be used for encrypt/decrypt
        KeyGenerator keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);

        KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            builder.setUnlockedDeviceRequired(true);
        }

        keyGenerator.init(builder.build());
        return keyGenerator.generateKey();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  ENCRYPT / DECRYPT (AES-GCM)
    // ═══════════════════════════════════════════════════════════════════════════

    private static String encrypt(String plaintext) {
        if (plaintext == null) return null;
        try {
            SecretKey key = getOrCreateMasterKey();
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key);

            byte[] iv = cipher.getIV();               // 12 bytes
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            buffer.put(iv);
            buffer.put(ciphertext);

            return Base64.encodeToString(buffer.array(), Base64.NO_WRAP);
        } catch (Exception e) {
            Log.e(TAG, "Encryption failed", e);
            return null;
        }
    }

    private static String decrypt(String encryptedBase64) {
        if (encryptedBase64 == null || encryptedBase64.isEmpty()) return null;
        try {
            byte[] combined = Base64.decode(encryptedBase64, Base64.NO_WRAP);
            if (combined.length < GCM_IV_LENGTH + 16) { 
                return null;
            }

            ByteBuffer buffer = ByteBuffer.wrap(combined);
            byte[] iv = new byte[GCM_IV_LENGTH];
            buffer.get(iv);
            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);

            SecretKey key = getOrCreateMasterKey();
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);

            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            Log.e(TAG, "Decryption failed", e);
            return null;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  PREFERENCES ACCESS
    // ═══════════════════════════════════════════════════════════════════════════

    private static SharedPreferences getPrefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  PUBLIC API — MNEMONIC
    // ═══════════════════════════════════════════════════════════════════════════

    public static void saveMnemonic(Context context, String address, String mnemonic) {
        if (context == null || address == null || mnemonic == null) return;
        String encrypted = encrypt(mnemonic);
        if (encrypted != null) {
            getPrefs(context).edit()
                    .putString("mnemonic_" + address, encrypted)
                    .apply();
        }
    }

    public static String loadMnemonic(Context context, String address) {
        if (context == null || address == null) return null;

        String encrypted = getPrefs(context).getString("mnemonic_" + address, null);
        if (encrypted != null) {
            return decrypt(encrypted);
        }

        String migrated = migrateFromWeakAes(context, "mnemonic_" + address);
        if (migrated != null) {
            saveMnemonic(context, address, migrated);
            return migrated;
        }

        SharedPreferences legacy = context.getSharedPreferences(LEGACY_MNEMONIC_PREFS, Context.MODE_PRIVATE);
        String legacyValue = legacy.getString("seed_phrase", legacy.getString("mnemonic", null));
        if (legacyValue != null && !legacyValue.trim().isEmpty()) {
            saveMnemonic(context, address, legacyValue);
            legacy.edit().remove("seed_phrase").remove("mnemonic").apply();
            return legacyValue;
        }

        return null;
    }

    public static String getActiveMnemonic(Context context) {
        String address = WalletManager.getActiveWalletAddress(context);
        if (address == null || address.trim().isEmpty()) return null;
        return loadMnemonic(context, address);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  PUBLIC API — PRIVATE KEY
    // ═══════════════════════════════════════════════════════════════════════════

    public static void savePrivateKey(Context context, String address, String privateKeyB64) {
        if (context == null || address == null || privateKeyB64 == null) return;
        String encrypted = encrypt(privateKeyB64);
        if (encrypted != null) {
            getPrefs(context).edit()
                    .putString("pk_" + address, encrypted)
                    .apply();
        }
    }

    public static String loadPrivateKey(Context context, String address) {
        if (context == null || address == null) return null;

        String encrypted = getPrefs(context).getString("pk_" + address, null);
        if (encrypted != null) {
            return decrypt(encrypted);
        }

        String migrated = migrateFromWeakAes(context, "pk_" + address);
        if (migrated != null) {
            savePrivateKey(context, address, migrated);
            return migrated;
        }

        SharedPreferences legacy = context.getSharedPreferences(LEGACY_KEYS_PREFS, Context.MODE_PRIVATE);
        String legacyValue = legacy.getString("pk_" + address, null);
        if (legacyValue != null) {
            savePrivateKey(context, address, legacyValue);
            legacy.edit().remove("pk_" + address).apply();
            return legacyValue;
        }

        return null;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  PUBLIC API — PUBLIC KEY
    // ═══════════════════════════════════════════════════════════════════════════

    public static void savePublicKey(Context context, String address, String publicKeyB64) {
        if (context == null || address == null || publicKeyB64 == null) return;
        String encrypted = encrypt(publicKeyB64);
        if (encrypted != null) {
            getPrefs(context).edit()
                    .putString("pub_" + address, encrypted)
                    .apply();
        }
    }

    public static String loadPublicKey(Context context, String address) {
        if (context == null || address == null) return null;

        String encrypted = getPrefs(context).getString("pub_" + address, null);
        if (encrypted != null) {
            return decrypt(encrypted);
        }

        String migrated = migrateFromWeakAes(context, "pub_" + address);
        if (migrated != null) {
            savePublicKey(context, address, migrated);
            return migrated;
        }

        SharedPreferences legacy = context.getSharedPreferences(LEGACY_KEYS_PREFS, Context.MODE_PRIVATE);
        String legacyValue = legacy.getString("pub_" + address, null);
        if (legacyValue != null) {
            savePublicKey(context, address, legacyValue);
            legacy.edit().remove("pub_" + address).apply();
            return legacyValue;
        }

        return null;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  CLEANUP
    // ═══════════════════════════════════════════════════════════════════════════

    public static void deleteWalletKeys(Context context, String address) {
        if (context == null || address == null) return;
        getPrefs(context).edit()
                .remove("mnemonic_" + address)
                .remove("pk_" + address)
                .remove("pub_" + address)
                .apply();
    }

    public static void wipeLegacyStorage(Context context) {
        if (context == null) return;
        try {
            context.getSharedPreferences(LEGACY_ENC_PREFS, Context.MODE_PRIVATE).edit().clear().apply();
            context.getSharedPreferences(LEGACY_MNEMONIC_PREFS, Context.MODE_PRIVATE).edit().clear().apply();
            context.getSharedPreferences(LEGACY_KEYS_PREFS, Context.MODE_PRIVATE).edit().clear().apply();
            Log.i(TAG, "Legacy wallet storage wiped");
        } catch (Exception e) {
            Log.w(TAG, "Failed to wipe legacy storage", e);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  MIGRATION FROM WEAK AES
    // ═══════════════════════════════════════════════════════════════════════════

    private static String migrateFromWeakAes(Context context, String key) {
        try {
            SharedPreferences oldPrefs = context.getSharedPreferences(LEGACY_ENC_PREFS, Context.MODE_PRIVATE);
            String oldEncrypted = oldPrefs.getString(key, null);
            if (oldEncrypted == null) return null;

            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = digest.digest(LEGACY_APP_SECRET.getBytes(StandardCharsets.UTF_8));
            javax.crypto.spec.SecretKeySpec oldKey = new javax.crypto.spec.SecretKeySpec(keyBytes, "AES");

            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, oldKey);
            byte[] decoded = Base64.decode(oldEncrypted, Base64.NO_WRAP);
            byte[] decrypted = cipher.doFinal(decoded);
            String plaintext = new String(decrypted, StandardCharsets.UTF_8);

            oldPrefs.edit().remove(key).apply();
            Log.i(TAG, "Migrated key from weak storage: " + key);
            return plaintext;
        } catch (Exception e) {
            Log.w(TAG, "Weak-AES migration failed for " + key, e);
            return null;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  TRANSACTION PIN
    // ═══════════════════════════════════════════════════════════════════════════

    private static final int PIN_PBKDF2_ITERATIONS = 120_000;
    private static final int PIN_HASH_LENGTH_BITS = 256;

    public static void setTransactionPin(Context context, String pin) {
        if (context == null || pin == null || PinPolicy.weakReason(pin) != null) return;
        try {
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);
            byte[] hash = _pbkdf2(pin, salt);

            String combined = Base64.encodeToString(salt, Base64.NO_WRAP) + ":"
                    + Base64.encodeToString(hash, Base64.NO_WRAP);
            String encrypted = encrypt(combined);
            if (encrypted != null) {
                getPrefs(context).edit().putString("txn_pin_hash", encrypted).apply();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to set transaction PIN", e);
        }
    }

    /** Milliseconds until PIN entry is allowed again after too many wrong tries; 0 when not locked. */
    public static long pinLockRemainingMs(Context context) {
        if (context == null) return 0L;
        return PinPolicy.lockRemainingMs(getPrefs(context).getLong("pin_locked_until", 0L), System.currentTimeMillis());
    }

    public static boolean verifyTransactionPin(Context context, String enteredPin) {
        if (context == null || enteredPin == null) return false;
        if (pinLockRemainingMs(context) > 0) return false;
        boolean ok = _verifyTransactionPinNoLock(context, enteredPin);
        android.content.SharedPreferences.Editor ed = getPrefs(context).edit();
        if (ok) {
            ed.putInt("pin_failures", 0).putLong("pin_locked_until", 0L);
        } else {
            int f = getPrefs(context).getInt("pin_failures", 0) + 1;
            if (f >= PinPolicy.MAX_FAILURES) {
                ed.putInt("pin_failures", 0).putLong("pin_locked_until", System.currentTimeMillis() + PinPolicy.LOCKOUT_MS);
            } else {
                ed.putInt("pin_failures", f);
            }
        }
        ed.apply();
        return ok;
    }

    private static boolean _verifyTransactionPinNoLock(Context context, String enteredPin) {
        try {
            String encrypted = getPrefs(context).getString("txn_pin_hash", null);
            if (encrypted == null) return false;
            String combined = decrypt(encrypted);
            if (combined == null || !combined.contains(":")) return false;

            String[] parts = combined.split(":", 2);
            byte[] salt = Base64.decode(parts[0], Base64.NO_WRAP);
            byte[] storedHash = Base64.decode(parts[1], Base64.NO_WRAP);
            byte[] enteredHash = _pbkdf2(enteredPin, salt);

            return _constantTimeEquals(storedHash, enteredHash);
        } catch (Exception e) {
            Log.e(TAG, "Failed to verify transaction PIN", e);
            return false;
        }
    }

    public static boolean hasTransactionPin(Context context) {
        if (context == null) return false;
        return getPrefs(context).getString("txn_pin_hash", null) != null;
    }

    public static void clearTransactionPin(Context context) {
        if (context == null) return;
        getPrefs(context).edit().remove("txn_pin_hash").apply();
    }

    private static byte[] _pbkdf2(String pin, byte[] salt) throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
        // FIXED: Using PBEKeySpec from javax.crypto.spec instead of java.security.spec
        PBEKeySpec spec = new PBEKeySpec(
                pin.toCharArray(), salt, PIN_PBKDF2_ITERATIONS, PIN_HASH_LENGTH_BITS);
        return factory.generateSecret(spec).getEncoded();
    }

    private static boolean _constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) return false;
        int diff = 0;
        for (int i = 0; i < a.length; i++) {
            diff |= a[i] ^ b[i];
        }
        return diff == 0;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  BIOMETRIC PREFERENCE
    // ═══════════════════════════════════════════════════════════════════════════

    public static void setBiometricEnabled(Context context, boolean enabled) {
        if (context == null) return;
        getPrefs(context).edit().putBoolean("biometric_unlock_enabled", enabled).apply();
    }

    public static boolean isBiometricEnabled(Context context) {
        if (context == null) return false;
        return getPrefs(context).getBoolean("biometric_unlock_enabled", false);
    }
}
