package com.harvanna.app.sync;

import android.content.Context;
import android.content.SharedPreferences;

import com.harvanna.app.wallet.WalletManager;

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

    public static synchronized void ensureLinked(Context ctx, Done done) {
        final Context c = ctx.getApplicationContext();
        final String addr = WalletManager.getActiveWalletAddress(c);
        if (addr == null || addr.isEmpty() || WalletManager.loadPrivateKey(c, addr) == null
                || com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() == null) {
            if (done != null) done.onDone("");
            return;
        }
        SharedPreferences p = prefs(c);
        if (p.getBoolean("linked_" + addr, false) && p.getBoolean("bound_" + addr, false)) {
            com.harvanna.app.identity.CombinedIdentity.stamp(c, addr);
            if (done != null) done.onDone(p.getString("status_" + addr, ""));
            return;
        }
        if (running) return;
        running = true;

        NodeIdentityClient.link(c, addr, new NodeIdentityClient.Callback() {
            @Override public void onSuccess(JSONObject j) {
                p.edit().putBoolean("linked_" + addr, true).apply();
                com.harvanna.app.identity.CombinedIdentity.stamp(c, addr);
                bind(c, addr, done);
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

    private static void bind(Context c, String addr, Done done) {
        SharedPreferences p = prefs(c);
        NodeIdentityClient.bindPersonhood(c, addr, new NodeIdentityClient.Callback() {
            @Override public void onSuccess(JSONObject j) {
                p.edit().putBoolean("bound_" + addr, true).apply();
                finish(p, addr, "Linked to your account and verified as a person on the network.", done);
            }
            @Override public void onError(String e) {
                String u = e == null ? "" : e;
                if (u.contains("PERSONHOOD_ALREADY_BOUND")) {
                    p.edit().putBoolean("bound_" + addr, true).apply();
                    finish(p, addr, "Linked to your account and verified as a person on the network.", done);
                } else if (u.contains("DEVICE_ALREADY_BOUND")) {
                    finish(p, addr, "Linked to your account. This phone is already verified for a different wallet.", done);
                } else {
                    finish(p, addr, "Linked to your account. Person verification is pending.", done);
                }
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
