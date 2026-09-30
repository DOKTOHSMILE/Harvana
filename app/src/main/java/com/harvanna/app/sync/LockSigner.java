package com.harvanna.app.sync;

import android.content.Context;
import android.util.Base64;

import com.harvanna.app.SketchApplication;
import com.harvanna.app.chain.LockV2;
import com.harvanna.app.crypto.Ed25519;
import com.harvanna.app.wallet.WalletManager;

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
        HarvannaNodeClient.nodeGet("/api/v1/locks/pending/" + address, new HarvannaNodeClient.JsonCallback() {
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
        final Context c = SketchApplication.getContext();
        final String from = templates.optJSONObject(0) == null ? "" : templates.optJSONObject(0).optString("from", "");
        if (from.isEmpty()) { done.onDone("The network sent a lock without an address."); return; }
        HarvannaNodeClient.nodeGet("/api/v1/wallet/nonce/" + from, new HarvannaNodeClient.JsonCallback() {
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
            HarvannaNodeClient.nodePost("/api/v1/locks/submit", body.toString(), new HarvannaNodeClient.JsonCallback() {
<<<<<<< HEAD
                @Override public void onSuccess(JSONObject r) {
                    String purpose = com.harvanna.app.notify.TxEvents.purpose(t.optString("memo", ""));
                    String scope = com.harvanna.app.notify.TxEvents.scope(t.optString("memo", ""));
                    com.harvanna.app.notify.NotificationCenter.post(c, com.harvanna.app.notify.NotificationCenter.TX,
                        "signed_" + from + "_" + nonce, "Payment signed",
                        "You signed " + com.harvanna.app.notify.TxEvents.amt(HarvannaNodeClient.toHarvDisplay(t.optLong("amount"))) + " " + com.harvanna.app.CoinInfo.ticker()
                            + (scope.isEmpty() ? "" : " for your " + scope) + (purpose.isEmpty() ? "" : " (" + purpose.toLowerCase(java.util.Locale.US) + ")") + ". The network is processing it.", "", -1);
                    submitFrom(c, templates, i + 1, nonce + 1, done);
                }
=======
                @Override public void onSuccess(JSONObject r) { submitFrom(c, templates, i + 1, nonce + 1, done); }
>>>>>>> main
                @Override public void onError(String e) { done.onDone(e); }
            });
        } catch (Exception e) {
            done.onDone("Could not sign the lock: " + e.getMessage());
        }
    }
}
