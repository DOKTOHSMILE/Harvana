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

    /** Bitcoin's saved address moved to legacy P2PKH (13 Oct 2026): a new key, so the old SegWit one is derived again. */
    private static String key(String harvAddress, ChainKeys.Chain chain) {
        return harvAddress + "_" + chain.name() + (chain == ChainKeys.Chain.BTC ? "_p2pkh" : "") + (Net.TEST ? "_tn" : "");
    }

    private static SharedPreferences prefs(Context c) { return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE); }

    /** Every chain's address for this wallet, deriving and saving any that are missing. Run off the main thread. */
    public static Map<ChainKeys.Chain, String> ensure(Context c, String harvAddress) {
        Map<ChainKeys.Chain, String> out = new LinkedHashMap<>();
        if (c == null || harvAddress == null || harvAddress.isEmpty()) return out;
        String mnemonic = null;
        SharedPreferences.Editor ed = prefs(c).edit();
        boolean changed = false;
        for (ChainKeys.Chain chain : ChainKeys.Chain.values()) {
            String key = key(harvAddress, chain);
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

    /** The address already saved for this wallet and chain, or "" . Does not derive. */
    public static String saved(Context c, String harvAddress, ChainKeys.Chain chain) {
        if (c == null || harvAddress == null || chain == null) return "";
        String a = prefs(c).getString(key(harvAddress, chain), "");
        return a == null ? "" : a;
    }
}
