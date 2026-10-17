package com.harvana.app.sync;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;

import com.harvana.app.SketchApplication;
import com.harvana.app.chain.LockV2;
import com.harvana.app.crypto.Ed25519;
import com.harvana.app.wallet.WalletManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

/**
 * v2 member locks. When the node has v2 locks switched on, an action that moves the member's own HARV into a
 * group or P2P escrow answers with "locksToSign": templates the member must sign themselves (node docs
 * ANDROID_V2_SIGNING.md). Below the activation height the node queues the lock itself and this class does nothing.
 */
public final class LockSigner {
    private LockSigner() {}

    public interface Done { void onDone(String errorOrNull); }

    /** If the response carries locksToSign, sign and submit them in order, then call done. */
    public static void handle(JSONObject response, Done done) {
        JSONArray templates = response == null ? null : response.optJSONArray("locksToSign");
        if (templates == null || templates.length() == 0) { done.onDone(null); return; }
        signAll(templates, done);
    }

    /** After a lost response: sign anything still pending for this address. */
    public static void checkPending(String address, Done done) {
        HarvanaNodeClient.nodeGet("/api/v1/locks/pending/" + address, new HarvanaNodeClient.JsonCallback() {
            @Override public void onSuccess(JSONObject j) {
                JSONArray t = j.optJSONArray("locks");
                if (t == null) t = j.optJSONArray("locksToSign");
                if (t == null) t = j.optJSONArray("templates");
                if (t == null || t.length() == 0) { done.onDone(null); return; }
                signAll(t, done);
            }
            @Override public void onError(String e) { done.onDone(null); }
        });
    }

    private static void signAll(JSONArray templates, Done done) {
        new Handler(Looper.getMainLooper()).post(() -> confirm(templates, done));
    }

    /** The member already asked for the action. This is the moment their own HARV actually moves. */
    private static void confirm(JSONArray templates, Done done) {
        Activity screen = com.harvana.app.utils.ActivityTracker.resumed();
        if (screen == null) {
            signNow(templates, done);
            return;
        }
        new AlertDialog.Builder(screen)
            .setTitle("Sign this payment")
            .setMessage(describe(templates))
            .setPositiveButton("Sign", (d, w) -> signNow(templates, done))
            .setNegativeButton("Cancel", (d, w) -> done.onDone("You cancelled the payment."))
            .setCancelable(false)
            .show();
    }

    private static String describe(JSONArray templates) {
        String ticker = com.harvana.app.CoinInfo.ticker();
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < templates.length(); i++) {
            JSONObject t = templates.optJSONObject(i);
            if (t == null) continue;
            if (out.length() > 0) out.append("\n\n");
            String amount = com.harvana.app.notify.TxEvents.amt(HarvanaNodeClient.toHarvDisplay(t.optLong("amount")));
            out.append("You pay ").append(amount).append(" ").append(ticker).append(".");
            long fee = t.optLong("maxFee");
            if (fee > 0) {
                out.append(" The network fee is at most ")
                    .append(com.harvana.app.notify.TxEvents.amt(HarvanaNodeClient.toHarvDisplay(fee)))
                    .append(" ").append(ticker).append(".");
            }
            String purpose = com.harvana.app.notify.TxEvents.purpose(t.optString("memo", ""));
            if (!purpose.isEmpty()) out.append(" This is for ").append(purpose.toLowerCase(java.util.Locale.US)).append(".");
        }
        if (out.length() == 0) out.append("You pay the amount the network asked for.");
        out.append("\n\nSigning cannot be undone.");
        return out.toString();
    }

    private static void signNow(JSONArray templates, Done done) {
        final Context c = SketchApplication.getContext();
        final String from = templates.optJSONObject(0) == null ? "" : templates.optJSONObject(0).optString("from", "");
        if (from.isEmpty()) { done.onDone("The network sent a lock without an address."); return; }
        HarvanaNodeClient.nodeGet("/api/v1/wallet/nonce/" + from, new HarvanaNodeClient.JsonCallback() {
            @Override public void onSuccess(JSONObject n) {
                submitFrom(c, templates, 0, n.optLong("nextNonce", 0), done);
            }
            @Override public void onError(String e) { done.onDone("Could not read the chain nonce: " + e); }
        });
    }

    private static void submitFrom(Context c, JSONArray templates, int i, long nonce, Done done) {
        if (i >= templates.length()) { done.onDone(null); return; }
        try {
            JSONObject t = templates.getJSONObject(i);
            String from = t.getString("from");
            String seed = WalletManager.loadPrivateKey(c, from);
            String pub = WalletManager.getPublicKey(c, from);
            if (seed == null || pub == null) { done.onDone("No signing key for " + from); return; }
            long expiry = System.currentTimeMillis() + 60L * 60 * 1000;
            byte[] bytes = LockV2.signingBytes(t.optInt("version", 2), t.optString("chainId"), t.optString("type"),
                from, t.optString("to"), t.optString("assetId", "HARV"), t.optLong("amount"), t.optLong("maxFee"),
                t.optLong("gasLimit"), nonce, expiry, t.optString("memo", ""), t.optString("payload", ""));
            byte[] sig = Ed25519.sign(Base64.decode(seed, Base64.NO_WRAP), bytes);
            JSONObject body = new JSONObject()
                .put("version", t.optInt("version", 2)).put("chainId", t.optString("chainId"))
                .put("type", t.optString("type")).put("from", from).put("to", t.optString("to"))
                .put("assetId", t.optString("assetId", "HARV")).put("amount", t.optLong("amount"))
                .put("maxFee", t.optLong("maxFee")).put("gasLimit", t.optLong("gasLimit"))
                .put("nonce", nonce).put("expiry", expiry)
                .put("memo", t.optString("memo", "")).put("payload", t.optString("payload", ""))
                .put("publicKey", pub).put("signature", Base64.encodeToString(sig, Base64.NO_WRAP));
            HarvanaNodeClient.nodePost("/api/v1/locks/submit", body.toString(), new HarvanaNodeClient.JsonCallback() {
                @Override public void onSuccess(JSONObject r) {
                    String purpose = com.harvana.app.notify.TxEvents.purpose(t.optString("memo", ""));
                    String scope = com.harvana.app.notify.TxEvents.scope(t.optString("memo", ""));
                    com.harvana.app.notify.NotificationCenter.post(c, com.harvana.app.notify.NotificationCenter.TX,
                        "signed_" + from + "_" + nonce, "Payment signed",
                        "You signed " + com.harvana.app.notify.TxEvents.amt(HarvanaNodeClient.toHarvDisplay(t.optLong("amount"))) + " " + com.harvana.app.CoinInfo.ticker()
                            + (scope.isEmpty() ? "" : " for your " + scope) + (purpose.isEmpty() ? "" : " (" + purpose.toLowerCase(java.util.Locale.US) + ")") + ". The network is processing it.", "", -1);
                    submitFrom(c, templates, i + 1, nonce + 1, done);
                }
                @Override public void onError(String e) { done.onDone(e); }
            });
        } catch (Exception e) {
            done.onDone("Could not sign the lock: " + e.getMessage());
        }
    }
}
