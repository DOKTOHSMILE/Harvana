package com.harvanna.app.wallet;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracks the list of wallets this device knows about, so a user can hold
 * more than one personal wallet and switch between them.
 *
 * SecureWalletStorage already stores each wallet's mnemonic/keys keyed by
 * its own address — it already supported multiple wallets' secrets
 * side-by-side, it just had no INDEX of which addresses exist. This class
 * is purely that index (a small JSON array in SharedPreferences — no new
 * storage dependency) plus which one is currently active.
 *
 * "Active wallet" reuses the existing harv_wallet_prefs/active_wallet_address
 * key that WalletManager.getActiveWalletAddress() already reads — this
 * class doesn't introduce a second source of truth for that.
 *
 * Removing a wallet from this list does NOT delete its keys from
 * SecureWalletStorage — that's a separate, more dangerous action this
 * class deliberately doesn't do. Removing here only hides it from the
 * "My Wallets" list; its funds and keys are untouched.
 */
public final class WalletListManager {

    private static final String PREFS = "harv_wallet_list_prefs";
    private static final String KEY_LIST = "wallet_list_json";

    private WalletListManager() {}

    public static final class WalletEntry {
        public final String address;
        public final String label;
        public final long createdAt;

        public WalletEntry(String address, String label, long createdAt) {
            this.address = address;
            this.label = label;
            this.createdAt = createdAt;
        }
    }

    /** Adds a wallet to the list if it isn't already there (no-op on a
     *  duplicate address — safe to call every time a wallet is created or
     *  restored without worrying about double-entries). */
    public static void addWalletToList(Context context, String address, String label) {
        if (address == null || address.trim().isEmpty()) return;

        List<WalletEntry> list = getWalletList(context);
        for (WalletEntry entry : list) {
            if (entry.address.equals(address)) return; // already present
        }

        String finalLabel = (label != null && !label.trim().isEmpty())
                ? label.trim()
                : "Wallet " + (list.size() + 1);

        list.add(new WalletEntry(address, finalLabel, System.currentTimeMillis()));
        saveList(context, list);
    }

    public static List<WalletEntry> getWalletList(Context context) {
        List<WalletEntry> result = new ArrayList<>();
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_LIST, null);
        if (json == null || json.trim().isEmpty()) return result;

        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                result.add(new WalletEntry(
                        obj.optString("address", ""),
                        obj.optString("label", ""),
                        obj.optLong("createdAt", 0L)
                ));
            }
        } catch (Exception ignored) {
            // Corrupted list — fail safe to empty rather than crash. The
            // underlying wallets' keys in SecureWalletStorage are untouched
            // either way; only this index would need rebuilding.
        }
        return result;
    }

    public static void removeFromList(Context context, String address) {
        List<WalletEntry> list = getWalletList(context);
        List<WalletEntry> filtered = new ArrayList<>();
        for (WalletEntry entry : list) {
            if (!entry.address.equals(address)) filtered.add(entry);
        }
        saveList(context, filtered);
    }

    /** Rewrites an entry's address in place (label and createdAt preserved). */
    public static void replaceAddress(Context context, String oldAddress, String newAddress) {
        List<WalletEntry> updated = new ArrayList<>();
        boolean hasNew = false;
        for (WalletEntry entry : getWalletList(context)) {
            if (entry.address.equals(newAddress)) hasNew = true;
        }
        for (WalletEntry entry : getWalletList(context)) {
            if (entry.address.equals(oldAddress)) {
                if (!hasNew) updated.add(new WalletEntry(newAddress, entry.label, entry.createdAt));
            } else {
                updated.add(entry);
            }
        }
        saveList(context, updated);
    }

    public static void renameWallet(Context context, String address, String newLabel) {
        if (newLabel == null || newLabel.trim().isEmpty()) return;
        List<WalletEntry> list = getWalletList(context);
        List<WalletEntry> updated = new ArrayList<>();
        for (WalletEntry entry : list) {
            if (entry.address.equals(address)) {
                updated.add(new WalletEntry(entry.address, newLabel.trim(), entry.createdAt));
            } else {
                updated.add(entry);
            }
        }
        saveList(context, updated);
    }

    /** Switches which wallet is "active" — reuses the same SharedPreferences
     *  key WalletManager.getActiveWalletAddress() already reads, so every
     *  existing screen that checks the active wallet keeps working
     *  unchanged once this is called. */
    public static void setActiveWallet(Context context, String address) {
        context.getSharedPreferences("harv_wallet_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString("active_wallet_address", address)
                .putString("active_address", address)
                .putString("wallet_address", address)
                .apply();
    }

    public static String getActiveWallet(Context context) {
        return WalletManager.getActiveWalletAddress(context);
    }

    /** Returns the active wallet's saved label for display (e.g. in
     *  AssetFragmentActivity's "Switch Wallet" header). Runs the same
     *  first-run backfill WalletListActivity does, so a pre-existing
     *  wallet from before this feature existed still gets a real label
     *  instead of falling through to the generic fallback below. */
    public static String getActiveWalletLabel(Context context) {
        String activeAddress = getActiveWallet(context);
        if (activeAddress == null || activeAddress.trim().isEmpty()) {
            return "No Wallet";
        }

        List<WalletEntry> list = getWalletList(context);
        if (list.isEmpty()) {
            addWalletToList(context, activeAddress, "My Wallet");
            list = getWalletList(context);
        }

        for (WalletEntry entry : list) {
            if (entry.address.equals(activeAddress)) {
                return entry.label;
            }
        }

        return "My Wallet";
    }

    public static int getWalletCount(Context context) {
        return getWalletList(context).size();
    }

    private static void saveList(Context context, List<WalletEntry> list) {
        try {
            JSONArray arr = new JSONArray();
            for (WalletEntry entry : list) {
                JSONObject obj = new JSONObject();
                obj.put("address", entry.address);
                obj.put("label", entry.label);
                obj.put("createdAt", entry.createdAt);
                arr.put(obj);
            }
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_LIST, arr.toString())
                    .apply();
        } catch (Exception ignored) {
            // Best-effort persistence — a failed save here means the list
            // index doesn't update, but never touches the actual wallet
            // keys, so nothing is lost, only the list view might be stale.
        }
    }
}
