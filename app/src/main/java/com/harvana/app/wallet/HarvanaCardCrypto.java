package com.harvana.app.wallet;

import android.content.Context;
import android.util.Base64;

import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Harvana Access Card — password-encrypted wallet backup, deliberately
 * INDEPENDENT of the device's Android Keystore (unlike SecureWalletStorage).
 * A physical card must be decryptable on ANY device by anyone who knows the
 * password — Harvana itself must never be able to decrypt it.
 *
 * KEY DERIVATION — combined PBKDF2 + Argon2id (added Sept 2026 for extra
 * hardening on a system protecting real financial wallets): the same
 * password+salt run through TWO independent, differently-designed KDFs,
 * combined via SHA-256(key1 || key2) into the final AES-256 key. An
 * attacker must break BOTH schemes, not just one — PBKDF2 alone is
 * CPU-bound and more amenable to GPU/ASIC acceleration; Argon2id is
 * deliberately memory-hard and resists that kind of hardware speedup much
 * better. Neither stage is weakened by adding the other; the combiner can
 * only add security over either alone.
 *   Stage 1: PBKDF2-HMAC-SHA512, 300,000 iterations -> 256-bit key1
 *   Stage 2: Argon2id, OWASP-recommended mobile-interactive parameters
 *            (19 MiB memory, 2 iterations, 1 thread) -> 256-bit key2
 *   Final:   AES key = SHA-256(key1 || key2)
 *
 * FORMAT 2 (unchanged from before this hardening): encrypts the wallet's
 * raw 16-byte BIP-39 entropy — the words are 100% deterministically
 * reconstructible from entropy via WalletGenerator.mnemonicFromEntropy().
 * Payload is still 44 bytes: salt(8) || iv(12) || entropy_ciphertext+tag(16+8).
 *   QR code    — "HARVANACARD2:" + Base64(44 bytes)  -> ~70 characters
 *   Digit code — 106 decimal digits, grouped by 5 -> printable QR-damage fallback
 */
public final class HarvanaCardCrypto {

    public static final String FORMAT_PREFIX = "HARVANACARD2:";

    public static final int MIN_PASSWORD_LENGTH = 24;

    private static final int SALT_LENGTH = 8;
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH_BITS = 64;
    private static final int ENTROPY_LENGTH = 16;
    private static final int TOTAL_BYTES = SALT_LENGTH + GCM_IV_LENGTH + ENTROPY_LENGTH + (GCM_TAG_LENGTH_BITS / 8); // 44

    /** Fixed digit width for lossless round-trip — ceil(44 * log10(256)). */
    public static final int DIGIT_CODE_LENGTH = 106;

    private static final int PBKDF2_ITERATIONS = 300_000;
    private static final int AES_KEY_LENGTH_BITS = 256;

    // Argon2id cost parameters — OWASP's minimum recommended profile for an
    // interactive (user-waiting) context: memory=19 MiB, iterations=2,
    // parallelism=1. This already runs alongside PBKDF2's 300k iterations
    // on a background thread (see CreateHarvanaCardActivity/RestoreWalletActivity),
    // so combined cost is roughly 2-4 seconds on a modern phone — acceptable
    // for a deliberate, rare action like creating or restoring a wallet card.
    private static final int ARGON2_MEMORY_KB = 19_456;
    private static final int ARGON2_ITERATIONS = 2;
    private static final int ARGON2_PARALLELISM = 1;
    private static final int ARGON2_OUTPUT_BYTES = 32; // 256 bits

    private HarvanaCardCrypto() {}

    public static final class ValidationException extends Exception {
        public ValidationException(String message) { super(message); }
    }

    public static void validatePassword(String password) throws ValidationException {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new ValidationException(
                    "Password must be at least " + MIN_PASSWORD_LENGTH + " characters (including spaces).");
        }
        if (password.trim().length() < MIN_PASSWORD_LENGTH) {
            throw new ValidationException(
                    "Password must contain at least " + MIN_PASSWORD_LENGTH + " real characters, not just spaces.");
        }
    }

    // ── Encryption ───────────────────────────────────────────────────────

    private static byte[] encryptToRawBytes(Context context, String mnemonic, String password) throws Exception {
        if (mnemonic == null || mnemonic.trim().isEmpty()) {
            throw new IllegalArgumentException("Mnemonic must not be empty.");
        }
        validatePassword(password);

        byte[] entropy = WalletGenerator.entropyFromMnemonic(context, mnemonic);
        if (entropy == null) {
            throw new IllegalArgumentException("This mnemonic's checksum doesn't verify — cannot create a card from it.");
        }

        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);

        SecretKeySpec key = deriveKey(password, salt);

        byte[] iv = new byte[GCM_IV_LENGTH];
        random.nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));

        byte[] ciphertextWithTag = cipher.doFinal(entropy);

        byte[] raw = new byte[TOTAL_BYTES];
        System.arraycopy(salt, 0, raw, 0, SALT_LENGTH);
        System.arraycopy(iv, 0, raw, SALT_LENGTH, GCM_IV_LENGTH);
        System.arraycopy(ciphertextWithTag, 0, raw, SALT_LENGTH + GCM_IV_LENGTH, ciphertextWithTag.length);
        return raw;
    }

    public static String encryptToQrString(Context context, String mnemonic, String password) throws Exception {
        byte[] raw = encryptToRawBytes(context, mnemonic, password);
        return FORMAT_PREFIX + Base64.encodeToString(raw, Base64.NO_WRAP);
    }

    public static String encryptToDigitCode(Context context, String mnemonic, String password) throws Exception {
        byte[] raw = encryptToRawBytes(context, mnemonic, password);
        return rawBytesToDigitString(raw);
    }

    // ── Decryption ───────────────────────────────────────────────────────

    public static final class DecryptResult {
        public final boolean success;
        public final String mnemonic;
        public final String errorMessage;

        private DecryptResult(boolean success, String mnemonic, String errorMessage) {
            this.success = success;
            this.mnemonic = mnemonic;
            this.errorMessage = errorMessage;
        }

        static DecryptResult ok(String mnemonic) { return new DecryptResult(true, mnemonic, null); }
        static DecryptResult fail(String message) { return new DecryptResult(false, null, message); }
    }

    public static DecryptResult decryptCardPayload(Context context, String scannedText, String password) {
        if (scannedText == null || !scannedText.startsWith(FORMAT_PREFIX)) {
            return DecryptResult.fail("This QR code isn't an Harvana Access Card.");
        }
        try {
            byte[] raw = Base64.decode(scannedText.substring(FORMAT_PREFIX.length()), Base64.NO_WRAP);
            return decryptRawBytes(context, raw, password);
        } catch (Exception e) {
            return DecryptResult.fail("This card's QR code looks damaged or incomplete.");
        }
    }

    public static DecryptResult decryptDigitCode(Context context, String digitCode, String password) {
        if (digitCode == null) {
            return DecryptResult.fail("Enter the digit code from the back of the card.");
        }
        String cleaned = digitCode.replaceAll("[^0-9]", "");
        if (cleaned.length() != DIGIT_CODE_LENGTH) {
            return DecryptResult.fail("That's " + cleaned.length() + " digits — this code should be exactly "
                    + DIGIT_CODE_LENGTH + " digits long. Check for a missing or extra digit.");
        }
        try {
            byte[] raw = digitStringToRawBytes(cleaned);
            return decryptRawBytes(context, raw, password);
        } catch (Exception e) {
            return DecryptResult.fail("Couldn't read this code. Please check it against the card.");
        }
    }

    private static DecryptResult decryptRawBytes(Context context, byte[] raw, String password) {
        if (raw == null || raw.length != TOTAL_BYTES) {
            return DecryptResult.fail("This card's data looks damaged or incomplete.");
        }
        if (password == null || password.trim().isEmpty()) {
            return DecryptResult.fail("Enter the password for this card.");
        }
        try {
            byte[] salt = new byte[SALT_LENGTH];
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] ciphertextWithTag = new byte[raw.length - SALT_LENGTH - GCM_IV_LENGTH];
            System.arraycopy(raw, 0, salt, 0, SALT_LENGTH);
            System.arraycopy(raw, SALT_LENGTH, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(raw, SALT_LENGTH + GCM_IV_LENGTH, ciphertextWithTag, 0, ciphertextWithTag.length);

            SecretKeySpec key = deriveKey(password, salt);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));

            byte[] entropy = cipher.doFinal(ciphertextWithTag);
            String mnemonic = WalletGenerator.mnemonicFromEntropy(context, entropy);
            if (mnemonic == null) {
                return DecryptResult.fail("Decrypted, but couldn't rebuild a valid recovery phrase from it.");
            }
            return DecryptResult.ok(mnemonic);
        } catch (javax.crypto.AEADBadTagException e) {
            return DecryptResult.fail("Wrong password. Please try again.");
        } catch (Exception e) {
            return DecryptResult.fail("Couldn't read this card: " + e.getMessage());
        }
    }

    // ── Digit encoding (BigInteger base-10, fixed width for lossless round-trip) ──

    private static String rawBytesToDigitString(byte[] raw) {
        BigInteger value = new BigInteger(1, raw);
        String digits = value.toString();
        StringBuilder sb = new StringBuilder();
        for (int i = digits.length(); i < DIGIT_CODE_LENGTH; i++) sb.append('0');
        sb.append(digits);
        return sb.toString();
    }

    private static byte[] digitStringToRawBytes(String digits) {
        BigInteger value = new BigInteger(digits);
        byte[] rawSigned = value.toByteArray();
        byte[] result = new byte[TOTAL_BYTES];
        int srcStart = Math.max(0, rawSigned.length - TOTAL_BYTES);
        int destStart = Math.max(0, TOTAL_BYTES - rawSigned.length);
        int copyLen = rawSigned.length - srcStart;
        System.arraycopy(rawSigned, srcStart, result, destStart, copyLen);
        return result;
    }

    public static String formatDigitCodeForDisplay(String rawDigits) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rawDigits.length(); i++) {
            if (i > 0 && i % 5 == 0) sb.append(' ');
            sb.append(rawDigits.charAt(i));
        }
        return sb.toString();
    }

    // ── Combined KDF: PBKDF2-HMAC-SHA512 + Argon2id -> SHA-256(key1 || key2) ──

    private static SecretKeySpec deriveKey(String password, byte[] salt) throws Exception {
        byte[] key1 = derivePbkdf2(password, salt);
        byte[] key2 = deriveArgon2id(password, salt);

        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        sha256.update(key1);
        sha256.update(key2);
        byte[] combined = sha256.digest(); // 32 bytes = 256 bits, exactly what AES-256 needs

        // Defensive zeroing of the intermediate key material — best-effort,
        // since Java strings/arrays aren't guaranteed-erasable, but costs nothing.
        java.util.Arrays.fill(key1, (byte) 0);
        java.util.Arrays.fill(key2, (byte) 0);

        return new SecretKeySpec(combined, "AES");
    }

    private static byte[] derivePbkdf2(String password, byte[] salt) throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, AES_KEY_LENGTH_BITS);
        return factory.generateSecret(spec).getEncoded();
    }

    private static byte[] deriveArgon2id(String password, byte[] salt) {
        Argon2Parameters params = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                .withIterations(ARGON2_ITERATIONS)
                .withMemoryAsKB(ARGON2_MEMORY_KB)
                .withParallelism(ARGON2_PARALLELISM)
                .withSalt(salt)
                .build();

        Argon2BytesGenerator generator = new Argon2BytesGenerator();
        generator.init(params);

        byte[] passwordBytes = password.getBytes(StandardCharsets.UTF_8);
        byte[] output = new byte[ARGON2_OUTPUT_BYTES];
        generator.generateBytes(passwordBytes, output);
        java.util.Arrays.fill(passwordBytes, (byte) 0);
        return output;
    }
}
