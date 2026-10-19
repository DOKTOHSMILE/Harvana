package com.harvana.app.sync;

import android.content.Context;
import android.content.SharedPreferences;

import com.harvana.app.wallet.WalletManager;
import com.harvana.app.sync.HarvanaNodeClient;

import org.json.JSONObject;

/**
 * Links the signed-in user's active wallet to them on the node (identity link + personhood bind), once
 * per wallet, and remembers the outcome. Safe to call repeatedly; it never blocks the caller.
 */
public final class NodeLinkManager {
    private static final String PREFS = "node_identity";
    private static boolean running;

    private NodeLinkManager() {}

    public interface Done { void onDone(String status); }

    public static String cachedStatus(Context c, String address) {
        return prefs(c).getString("status_" + address, "");
    }

    private static final long RECHECK_MS = 30L * 60_000L;
    private static long lastCheck;

    /**
     * Keeps this wallet linked to the account and bound to this phone. Device binding is asked of the node every time (at most every
     * 30 minutes), never trusted from a saved flag: a node that was reset forgets every binding, and a saved "bound" would then
     * hide that forever. Binding and linking are separate steps, so a link the network will not accept never blocks the binding.
     */
    public static synchronized void ensureLinked(Context ctx, Done done) {
        final Context c = ctx.getApplicationContext();
        final String addr = WalletManager.getActiveWalletAddress(c);
        if (addr == null || addr.isEmpty() || WalletManager.loadPrivateKey(c, addr) == null
                || com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() == null) {
            if (done != null) done.onDone("");
            return;
        }
        SharedPreferences p = prefs(c);
        long now = System.currentTimeMillis();
        if (p.getBoolean("linked_" + addr, false) && p.getBoolean("bound_" + addr, false) && now - lastCheck < RECHECK_MS) {
            com.harvana.app.identity.CombinedIdentity.stamp(c, addr);
            if (done != null) done.onDone(p.getString("status_" + addr, ""));
            return;
        }
        if (running) return;
        running = true;
        lastCheck = now;

        // 1. Add this wallet to the signed-in person (first, so the node knows it is theirs before the phone is bound).
        // 2. Bind it to this phone if the node says it is not bound yet. A phone belongs to one person; each of that
        //    person's wallets can be bound on it. Device binding is asked of the node, never trusted from a saved flag.
        link(c, addr, linkStatus -> ensureBound(c, addr, () -> finish(p, addr, linkStatus, done)));
    }

    private static void ensureBound(Context c, String addr, Runnable next) {
        SharedPreferences p = prefs(c);
        HarvanaNodeClient.nodeGet("/api/v1/personhood/" + addr, new HarvanaNodeClient.JsonCallback() {
            @Override public void onSuccess(JSONObject j) {
                boolean bound = j.optBoolean("bound", false);
                p.edit().putBoolean("bound_" + addr, bound).apply();
                if (bound) next.run(); else bind(c, addr, next);
            }
            @Override public void onError(String e) { next.run(); }      // node unreachable: leave the binding as it was
        });
    }

    /** Links the active wallet to the signed-in account now, whatever was remembered, and reports what the node said. */
    public static void linkNow(Context ctx, Done done) {
        final Context c = ctx.getApplicationContext();
        final String addr = WalletManager.getActiveWalletAddress(c);
        if (addr == null || addr.isEmpty() || WalletManager.loadPrivateKey(c, addr) == null
                || com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() == null) { if (done != null) done.onDone("No wallet on this phone."); return; }
        prefs(c).edit().remove("linked_" + addr).apply();
        link(c, addr, status -> finish(prefs(c), addr, status, done));
    }

    /** True when this phone has seen the node accept the link for the active wallet. */
    public static boolean isLinked(Context c) {
        String addr = WalletManager.getActiveWalletAddress(c);
        return addr != null && prefs(c).getBoolean("linked_" + addr, false);
    }

    /** The "Bind this device" button: binds, then links the wallet to the account, and reports what the node really said. */
    public static void bindNow(Context ctx, Done done) {
        final Context c = ctx.getApplicationContext();
        final String addr = WalletManager.getActiveWalletAddress(c);
        if (addr == null || addr.isEmpty() || WalletManager.loadPrivateKey(c, addr) == null) { if (done != null) done.onDone("No wallet on this phone."); return; }
        SharedPreferences p = prefs(c);
        p.edit().remove("status_" + addr).remove("linked_" + addr).apply();         // never show an old answer
        Runnable bindThenReport = () -> bind(c, addr, () -> {
            String bindStatus = p.getString("status_" + addr, "");
            String out = bindStatus + (p.getBoolean("linked_" + addr, false) ? " This wallet is linked to your account." : "");
            p.edit().putString("status_" + addr, out.trim()).apply();
            if (done != null) done.onDone(out.trim());
        });
        // Link first: a phone already bound to another of your wallets accepts this one only once both are yours.
        if (com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() == null) { bindThenReport.run(); return; }
        link(c, addr, linkStatus -> {
            if (!linkStatus.isEmpty()) p.edit().putString("link_note_" + addr, linkStatus).apply();
            bindThenReport.run();
        });
    }

    private static void link(Context c, String addr, Done done) {
        SharedPreferences p = prefs(c);
        if (p.getBoolean("linked_" + addr, false)) { com.harvana.app.identity.CombinedIdentity.stamp(c, addr); done.onDone(""); return; }
        NodeIdentityClient.link(c, addr, new NodeIdentityClient.Callback() {
            @Override public void onSuccess(JSONObject j) {
                p.edit().putBoolean("linked_" + addr, true).apply();
                com.harvana.app.identity.CombinedIdentity.stamp(c, addr);
                done.onDone("");
            }
            @Override public void onError(String e) {
                String u = e == null ? "" : e;
                if (com.harvana.app.utils.ErrorText.is(u, "IDENTITY_LINKS_DISABLED")) done.onDone("The network isn't accepting account links yet.");
                else if (com.harvana.app.utils.ErrorText.is(u, "WALLET_ALREADY_LINKED")) done.onDone("This wallet belongs to another account. A wallet can belong to one person only.");
                else if (com.harvana.app.utils.ErrorText.is(u, "TOO_MANY_WALLETS")) done.onDone("Your account already has the most wallets the network allows (20).");
                else if (com.harvana.app.utils.ErrorText.is(u, "USER_ALREADY_LINKED")) done.onDone("The network is still on the one-wallet rule. This wallet will be added to your account after the next network update.");
                else if (com.harvana.app.utils.ErrorText.is(u, "NETWORK") || com.harvana.app.utils.ErrorText.is(u, "SERVER_ERROR")
                        || com.harvana.app.utils.ErrorText.is(u, "UNAVAILABLE")) done.onDone("");   // transient: try again next time
                else done.onDone(com.harvana.app.utils.ErrorText.plain(u));          // what the network said, so it can be fixed
            }
        });
    }

    private static void bind(Context c, String addr, Runnable next) {
        SharedPreferences p = prefs(c);
        NodeIdentityClient.bindPersonhood(c, addr, new NodeIdentityClient.Callback() {
            @Override public void onSuccess(JSONObject j) {
                p.edit().putBoolean("bound_" + addr, true).remove("conflict_" + addr).putString("status_" + addr, "This phone is bound to your wallet on the network.").apply();
                next.run();
            }
            @Override public void onError(String e) {
                String u = e == null ? "" : e;
                if (com.harvana.app.utils.ErrorText.is(u, "PERSONHOOD_ALREADY_BOUND")) {
                    p.edit().putBoolean("bound_" + addr, true).putString("status_" + addr, "This phone is bound to your wallet on the network.").apply();
                } else if (com.harvana.app.utils.ErrorText.is(u, "DEVICE_ALREADY_BOUND")) {
                    p.edit().putBoolean("bound_" + addr, false).putBoolean("conflict_" + addr, true)
                            .putString("status_" + addr, "This phone is already bound to a wallet that is not on your account.").apply();
                } else {
                    p.edit().putString("status_" + addr, "Device binding is pending. It will be tried again.").apply();
                }
                next.run();
            }
        });
    }

    /** True when the last bind of this wallet was refused because the phone is bound to a wallet outside this account. */
    public static boolean deviceConflict(Context c, String addr) {
        return addr != null && prefs(c).getBoolean("conflict_" + addr, false);
    }

    public interface Holder { void found(String addressOrEmpty); }

    /**
     * Which other wallet on this phone the phone is bound to: a wallet kept here (its key is on this phone) that the
     * network says is bound. Empty when none of the wallets here is, i.e. the phone was bound with a wallet that is gone.
     */
    public static void findHolder(Context ctx, String addr, Holder cb) {
        final Context c = ctx.getApplicationContext();
        java.util.List<String> others = new java.util.ArrayList<>();
        for (com.harvana.app.wallet.WalletListManager.WalletEntry e : com.harvana.app.wallet.WalletListManager.getWalletList(c)) {
            if (e == null || e.address == null || e.address.equalsIgnoreCase(addr) || WalletManager.loadPrivateKey(c, e.address) == null) continue;
            if (!others.contains(e.address)) others.add(e.address);
        }
        check(others, 0, cb);
    }

    private static void check(java.util.List<String> list, int i, Holder cb) {
        if (i >= list.size()) { cb.found(""); return; }
        String a = list.get(i);
        HarvanaNodeClient.nodeGet("/api/v1/personhood/" + a, new HarvanaNodeClient.JsonCallback() {
            @Override public void onSuccess(JSONObject j) { if (j.optBoolean("bound", false)) cb.found(a); else check(list, i + 1, cb); }
            @Override public void onError(String e) { check(list, i + 1, cb); }
        });
    }

    /**
     * Adds {@code holder} (a wallet on this phone that the phone is bound to) to the signed-in account, signed by that
     * wallet, then binds the active wallet. The network allows the second wallet on the phone once both are the same person.
     */
    public static void adoptHolderThenBind(Context ctx, String holder, Done done) {
        final Context c = ctx.getApplicationContext();
        SharedPreferences p = prefs(c);
        p.edit().remove("linked_" + holder).apply();
        link(c, holder, linkStatus -> {
            if (!p.getBoolean("linked_" + holder, false)) {
                if (done != null) done.onDone(linkStatus.isEmpty() ? "That wallet could not be added to your account. Try again." : linkStatus);
                return;
            }
            String addr = WalletManager.getActiveWalletAddress(c);
            p.edit().remove("conflict_" + addr).apply();
            bindNow(c, done);
        });
    }

    private static void finish(SharedPreferences p, String addr, String status, Done done) {
        running = false;
        if (!status.isEmpty()) p.edit().putString("status_" + addr, status).apply();
        if (done != null) done.onDone(status);
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
