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

        // 1. Is this wallet bound on the node right now?
        HarvanaNodeClient.nodeGet("/api/v1/personhood/" + addr, new HarvanaNodeClient.JsonCallback() {
            @Override public void onSuccess(JSONObject j) {
                boolean bound = j.optBoolean("bound", false);
                p.edit().putBoolean("bound_" + addr, bound).apply();
                if (bound) link(c, addr, done);
                else bind(c, addr, () -> link(c, addr, done));
            }
            @Override public void onError(String e) { link(c, addr, done); }      // node unreachable: leave the binding as it was
        });
    }

    /** The "Bind this device" button: binds now, whatever was remembered, and reports what the node said. */
    public static void bindNow(Context ctx, Done done) {
        final Context c = ctx.getApplicationContext();
        final String addr = WalletManager.getActiveWalletAddress(c);
        if (addr == null || addr.isEmpty() || WalletManager.loadPrivateKey(c, addr) == null) { if (done != null) done.onDone("No wallet on this phone."); return; }
        bind(c, addr, () -> { if (done != null) done.onDone(prefs(c).getString("status_" + addr, "")); });
    }

    private static void link(Context c, String addr, Done done) {
        SharedPreferences p = prefs(c);
        if (p.getBoolean("linked_" + addr, false)) { com.harvana.app.identity.CombinedIdentity.stamp(c, addr); finish(p, addr, "", done); return; }
        NodeIdentityClient.link(c, addr, new NodeIdentityClient.Callback() {
            @Override public void onSuccess(JSONObject j) {
                p.edit().putBoolean("linked_" + addr, true).apply();
                com.harvana.app.identity.CombinedIdentity.stamp(c, addr);
                finish(p, addr, "", done);
            }
            @Override public void onError(String e) {
                String u = e == null ? "" : e;
                if (u.contains("IDENTITY_LINKS_DISABLED")) finish(p, addr, "The network isn't accepting account links yet.", done);
                else if (u.contains("WALLET_ALREADY_LINKED")) finish(p, addr, "This wallet is already linked to another account.", done);
                else if (u.contains("USER_ALREADY_LINKED")) finish(p, addr, "Your account is already linked to a different wallet. The network allows one wallet per account.", done);
                else finish(p, addr, "", done); // transient: try again next time
            }
        });
    }

    private static void bind(Context c, String addr, Runnable next) {
        SharedPreferences p = prefs(c);
        NodeIdentityClient.bindPersonhood(c, addr, new NodeIdentityClient.Callback() {
            @Override public void onSuccess(JSONObject j) {
                p.edit().putBoolean("bound_" + addr, true).putString("status_" + addr, "This phone is bound to your wallet on the network.").apply();
                next.run();
            }
            @Override public void onError(String e) {
                String u = e == null ? "" : e;
                if (u.contains("PERSONHOOD_ALREADY_BOUND")) {
                    p.edit().putBoolean("bound_" + addr, true).putString("status_" + addr, "This phone is bound to your wallet on the network.").apply();
                } else if (u.contains("DEVICE_ALREADY_BOUND")) {
                    p.edit().putBoolean("bound_" + addr, false).putString("status_" + addr, "This phone is already bound to a different wallet on the network. One phone can be bound to one wallet.").apply();
                } else {
                    p.edit().putString("status_" + addr, "Device binding is pending. It will be tried again.").apply();
                }
                next.run();
            }
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
