package com.harvana.app.group;

import android.content.Context;
import android.widget.TextView;

import com.harvana.app.firestore.GroupFirestoreManager;
import com.harvana.app.firestore.UserProfileManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves a member's real display name from a Firebase uid or a group
 * wallet address, with a small in-memory cache so a scrolling list of
 * payments/loans/notices doesn't re-fetch the same person's profile once
 * per row.
 *
 * Before this existed, every screen that needed to show "who" had its own
 * copy of this logic, and they'd drifted:
 *   - PaymentFragmentActivity's PayAdapter read a "memberName" field that
 *     GroupPaymentHelper's contribution paths never actually wrote, so
 *     rotating/accumulative contributions always fell back to the literal
 *     word "Member".
 *   - LoansFragmentActivity resolved the borrower via legacy RTDB
 *     (userdb/{uid}) instead of Firestore, which is the actual source of
 *     truth post-migration — dual-write-only RTDB isn't guaranteed to have
 *     first_name/last_name for every user going forward.
 *   - HistoryFragmentActivity showed the raw HV0x... wallet address for
 *     every row, member or not — the "crazy characters" complaint.
 *   - GroupNoticeHelper hardcoded the literal string "Admin" as the sender
 *     of every announcement instead of the actual poster's name.
 * This resolves uid/wallet -> name the same way (Firestore USERS/{uid},
 * via UserProfileManager, which is the confirmed-authoritative profile
 * source) everywhere, once.
 */
public final class MemberNameResolver {

    private MemberNameResolver() {}

    public interface NameCallback {
        void onResolved(String displayName);
    }

    // uid -> resolved "First Last" (or just "First" if no last name).
    // Cleared per groupId is unnecessary — a uid's name is the same across
    // every group, so this is a single process-lifetime cache.
    private static final Map<String, String> uidNameCache = new ConcurrentHashMap<>();

    // groupId -> (walletAddress(lowercased) -> memberUid), used to map a
    // group-wallet transaction counterparty back to a member. Rebuilt each
    // time a screen calls preloadGroupWallets() since membership changes.
    private static final Map<String, Map<String, String>> groupWalletToUid = new ConcurrentHashMap<>();

    /** Resolve a uid to "First Last". Cached after the first successful fetch. */
    public static void resolve(Context context, String uid, String fallback, NameCallback cb) {
        if (uid == null || uid.trim().isEmpty()) {
            cb.onResolved(fallback);
            return;
        }
        String cached = uidNameCache.get(uid);
        if (cached != null) {
            cb.onResolved(cached);
            return;
        }
        UserProfileManager.getInstance(context.getApplicationContext())
                .getProfile(uid, new UserProfileManager.DocCallback() {
                    @Override
                    public void onSuccess(Map<String, Object> data) {
                        String name = _fullName(data, fallback);
                        uidNameCache.put(uid, name);
                        cb.onResolved(name);
                    }

                    @Override
                    public void onError(String error) {
                        cb.onResolved(fallback);
                    }
                });
    }

    /** Convenience overload — resolves straight into a TextView, guarding
     *  against the view having been recycled by the time the fetch returns
     *  by re-checking its current tag matches the uid it was asked for. */
    public static void resolveInto(Context context, String uid, String fallback, TextView target) {
        if (target == null) return;
        target.setText(fallback);
        target.setTag(uid);
        resolve(context, uid, fallback, name -> {
            if (uid != null && uid.equals(target.getTag())) {
                target.setText(name);
            }
        });
    }

    /**
     * Loads the group's member roster once and builds a walletAddress -> uid
     * map, so transaction counterparties that belong to the group can be
     * shown by name instead of by address. Call this once when a screen
     * loads (History tab, group wallet activity), then use
     * nameForWallet()/resolveWalletInto() per row.
     */
    public static void preloadGroupWallets(Context context, String groupId, Runnable onDone) {
        GroupFirestoreManager.getInstance(context.getApplicationContext())
                .getMembers(groupId, new GroupFirestoreManager.ListCallback() {
                    @Override
                    public void onSuccess(List<Map<String, Object>> members) {
                        Map<String, String> walletToUid = new HashMap<>();
                        if (members != null) {
                            for (Map<String, Object> m : members) {
                                String uid = _str(m, "memberUid");
                                String wallet = _str(m, "walletAddress");
                                if (wallet.isEmpty()) wallet = _str(m, "wallet_address");
                                if (!uid.isEmpty() && !wallet.isEmpty()) {
                                    walletToUid.put(wallet.toLowerCase(), uid);
                                }
                            }
                        }
                        groupWalletToUid.put(groupId, walletToUid);
                        if (onDone != null) onDone.run();
                    }

                    @Override
                    public void onError(String error) {
                        // Leave whatever was cached before (possibly nothing) —
                        // callers fall back to short-address display per row.
                        if (onDone != null) onDone.run();
                    }
                });
    }

    /** Resolves a wallet address to a member's name if it belongs to this
     *  group; otherwise calls back with the fallback (typically a shortened
     *  address) unchanged. Call preloadGroupWallets() first. */
    public static void resolveWallet(Context context, String groupId, String walletAddress,
                                     String fallback, NameCallback cb) {
        if (walletAddress == null) {
            cb.onResolved(fallback);
            return;
        }
        Map<String, String> walletToUid = groupWalletToUid.get(groupId);
        String uid = walletToUid != null ? walletToUid.get(walletAddress.toLowerCase()) : null;
        if (uid == null) {
            cb.onResolved(fallback);
            return;
        }
        resolve(context, uid, fallback, cb);
    }

    private static String _fullName(Map<String, Object> data, String fallback) {
        if (data == null) return fallback;
        Object f = data.get("first_name");
        Object l = data.get("last_name");
        String first = f != null ? String.valueOf(f).trim() : "";
        String last = l != null ? String.valueOf(l).trim() : "";
        String full = (first + " " + last).trim();
        return full.isEmpty() ? fallback : full;
    }

    private static String _str(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v == null) return "";
        String s = String.valueOf(v).trim();
        return "null".equalsIgnoreCase(s) ? "" : s;
    }
}
