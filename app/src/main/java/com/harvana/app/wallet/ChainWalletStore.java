package com.harvana.app.wallet;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The public addresses on other chains (Bitcoin, Ethereum, Tron, ...) that a Harvana wallet's recovery phrase opens. They are
 * worked out once on the phone from the phrase (ChainKeys) and kept here; keys are never stored, they are derived again from
 * the phrase only when something has to be signed.
 */
public final class ChainWalletStore {
    private ChainWalletStore() {}

    private static final String PREFS = "chain_wallets";

    private static SharedPreferences prefs(Context c) { return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE); }

    /** Every chain's address for this wallet, deriving and saving any that are missing. Run off the main thread. */
    public static Map<ChainKeys.Chain, String> ensure(Context c, String harvAddress) {
        Map<ChainKeys.Chain, String> out = new LinkedHashMap<>();
        if (c == null || harvAddress == null || harvAddress.isEmpty()) return out;
        String mnemonic = null;
        SharedPreferences.Editor ed = prefs(c).edit();
        boolean changed = false;
        for (ChainKeys.Chain chain : ChainKeys.Chain.values()) {
            String key = harvAddress + "_" + chain.name();
            String a = prefs(c).getString(key, "");
            if (a.isEmpty()) {
                if (mnemonic == null) mnemonic = SecureWalletStorage.loadMnemonic(c, harvAddress);
                if (mnemonic == null || mnemonic.trim().isEmpty()) continue;   // a wallet imported from a key alone has no phrase
                try { a = ChainKeys.address(mnemonic, chain); ed.putString(key, a); changed = true; } catch (Exception ignored) { continue; }
            }
            out.put(chain, a);
        }
        if (changed) ed.apply();
        if (mnemonic != null && PiWalletStore.get(c, harvAddress).isEmpty()) PiWalletStore.rememberFromPhrase(c, harvAddress, mnemonic);
        return out;
    }
}
