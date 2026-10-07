package com.harvana.app.wallet;

import android.content.Context;

/**
 * Remembers the Pi address that belongs to each Harvana wallet (both come from the same recovery phrase, see PiKeys).
 * Only the public "G..." address is kept here; the Pi key is derived from the phrase when it is needed and never stored apart.
 */
public final class PiWalletStore {
    private PiWalletStore() {}

    private static final String PREFS = "pi_wallets";

    public static void save(Context c, String harvAddress, String piAddress) {
        if (c == null || harvAddress == null || piAddress == null || !PiKeys.isValidAddress(piAddress)) return;
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(harvAddress, piAddress).apply();
    }

    /** The Pi address for a Harvana wallet, or "" when the wallet was made before Pi support (or from 12 words). */
    public static String get(Context c, String harvAddress) {
        if (c == null || harvAddress == null) return "";
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(harvAddress, "");
    }

    /** Works out and saves the Pi address from a phrase. Cheap enough to run on any thread; returns it or "". */
    public static String rememberFromPhrase(Context c, String harvAddress, String mnemonic) {
        // Pi's own wallet uses 24 words: only such a phrase gives a Pi wallet the person can also open in Pi's app.
        if (mnemonic == null || mnemonic.trim().split("\\s+").length != 24) return "";
        String pi = WalletGenerator.derivePiAddress(mnemonic);
        if (pi == null) return "";
        save(c, harvAddress, pi);
        return pi;
    }
}
