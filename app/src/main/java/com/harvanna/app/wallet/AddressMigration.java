package com.harvanna.app.wallet;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import com.harvanna.app.ChainConstants;
import com.harvanna.app.crypto.Ed25519;

import java.util.ArrayList;
import java.util.List;

/**
 * One-time, idempotent migration of locally stored wallets from the legacy
 * "AJ0x" address prefix to "HV0x".
 *
 * The HV0x address is RE-DERIVED from the wallet's own key material
 * (public key, else the private seed, else the mnemonic) rather than by
 * string-swapping the prefix, so the result is exactly what the node computes:
 * "HV0x" + first 40 hex of SHA-256(raw 32-byte Ed25519 public key).
 *
 * For each legacy address this:
 *   1. copies mnemonic / private key / public key to entries keyed by the new address,
 *      and verifies the copy reads back identically before touching the old entries;
 *   2. rewrites the active-wallet prefs, the wallet list index and the balance cache key;
 *   3. removes the old-address key entries only after step 1 verified.
 * A wallet with no recoverable key material is left untouched (never guessed).
 */
public final class AddressMigration {

    private static final String TAG = "AddressMigration";
    private static final String LEGACY_PREFIX = "AJ0x";

    private AddressMigration() {}

    /** Safe to call on every app start; does nothing once no legacy addresses remain. */
    public static synchronized void run(Context context) {
        if (context == null) return;
        try {
            Context app = context.getApplicationContext();
            for (String old : collectLegacyAddresses(app)) {
                migrateOne(app, old);
            }
        } catch (Exception e) {
            Log.w(TAG, "Address migration failed; will retry next start", e);
        }
    }

    private static List<String> collectLegacyAddresses(Context app) {
        List<String> found = new ArrayList<>();
        SharedPreferences w = app.getSharedPreferences("harv_wallet_prefs", Context.MODE_PRIVATE);
        for (String k : new String[]{"active_wallet_address", "wallet_address", "active_address"}) {
            addIfLegacy(found, w.getString(k, ""));
        }
        addIfLegacy(found, app.getSharedPreferences("walletsp", Context.MODE_PRIVATE).getString("wallet_address", ""));
        for (WalletListManager.WalletEntry e : WalletListManager.getWalletList(app)) {
            addIfLegacy(found, e.address);
        }
        return found;
    }

    private static void addIfLegacy(List<String> out, String a) {
        if (a == null) return;
        a = a.trim();
        if (a.startsWith(LEGACY_PREFIX) && !out.contains(a)) out.add(a);
    }

    private static void migrateOne(Context app, String old) {
        String pkB64 = SecureWalletStorage.loadPrivateKey(app, old);
        String pubB64 = SecureWalletStorage.loadPublicKey(app, old);
        String mnemonic = SecureWalletStorage.loadMnemonic(app, old);

        String fresh = null;
        try {
            byte[] pub = null;
            if (pubB64 != null) {
                pub = Base64.decode(pubB64, Base64.NO_WRAP);
            } else if (pkB64 != null) {
                pub = Ed25519.publicKeyFromSeed(Base64.decode(pkB64, Base64.NO_WRAP));
                pubB64 = Base64.encodeToString(pub, Base64.NO_WRAP);
            } else if (mnemonic != null) {
                fresh = WalletGenerator.deriveAddressFromMnemonic(mnemonic);
            }
            if (fresh == null && pub != null && pub.length == 32) {
                fresh = WalletGenerator.addressFromPublicKey(pub);
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not derive HV0x address for a legacy wallet", e);
        }

        if (fresh == null || !fresh.startsWith(ChainConstants.ADDRESS_PREFIX)) {
            Log.w(TAG, "No usable key material for a legacy wallet; left unchanged");
            return;
        }

        // 1. copy keys under the new address, verify, only then drop the old entries
        if (mnemonic != null) SecureWalletStorage.saveMnemonic(app, fresh, mnemonic);
        if (pkB64 != null) SecureWalletStorage.savePrivateKey(app, fresh, pkB64);
        if (pubB64 != null) SecureWalletStorage.savePublicKey(app, fresh, pubB64);
        // saves use apply(); reads see the in-memory value immediately, which is what we verify
        boolean ok = same(mnemonic, SecureWalletStorage.loadMnemonic(app, fresh))
                && same(pkB64, SecureWalletStorage.loadPrivateKey(app, fresh))
                && same(pubB64, SecureWalletStorage.loadPublicKey(app, fresh));
        if (!ok) {
            Log.w(TAG, "Key copy did not verify; legacy wallet left unchanged");
            return;
        }

        // 2. repoint everything that stores the address
        SharedPreferences w = app.getSharedPreferences("harv_wallet_prefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor we = w.edit();
        for (String k : new String[]{"active_wallet_address", "wallet_address", "active_address"}) {
            if (old.equals(w.getString(k, "").trim())) we.putString(k, fresh);
        }
        we.apply();
        SharedPreferences ws = app.getSharedPreferences("walletsp", Context.MODE_PRIVATE);
        if (old.equals(ws.getString("wallet_address", "").trim())) {
            ws.edit().putString("wallet_address", fresh).apply();
        }
        WalletListManager.replaceAddress(app, old, fresh);
        app.getSharedPreferences("harvanna_wallet_cache", Context.MODE_PRIVATE).edit().remove(old).apply();

        // 3. drop old key entries
        SecureWalletStorage.deleteWalletKeys(app, old);
        Log.i(TAG, "Migrated a legacy AJ0x wallet to HV0x");
    }

    private static boolean same(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}
