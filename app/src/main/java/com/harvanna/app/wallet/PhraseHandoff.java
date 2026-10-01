package com.harvanna.app.wallet;

/**
 * Holds a recovery phrase in process memory for the next screen.
 * It is never placed on an Intent, so it is not written into the activity
 * record, recent-tasks snapshot, or another app's logs.
 * The phrase is dropped when that screen finishes. A rotation keeps it.
 */
public final class PhraseHandoff {
    private static final long MAX_AGE_MS = 10L * 60L * 1000L;
    private static String phrase;
    private static String address;
    private static long heldAt;

    private PhraseHandoff() {}

    public static void hold(String mnemonic, String walletAddress) {
        phrase = mnemonic;
        address = walletAddress == null ? "" : walletAddress.trim();
        heldAt = System.currentTimeMillis();
    }

    /** The phrase for this address, or null if none was held or it has expired. */
    public static String peek(String walletAddress) {
        if (phrase == null) return null;
        if (System.currentTimeMillis() - heldAt > MAX_AGE_MS) {
            clear();
            return null;
        }
        String want = walletAddress == null ? "" : walletAddress.trim();
        if (!address.isEmpty() && !address.equals(want)) return null;
        return phrase;
    }

    public static void clear() {
        phrase = null;
        address = "";
        heldAt = 0L;
    }
}
