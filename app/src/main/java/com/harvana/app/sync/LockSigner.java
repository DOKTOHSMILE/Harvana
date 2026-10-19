package com.harvana.app.sync;

import android.app.Activity;
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

    /** Templates the person cancelled this session: not asked again until the app restarts. */
    private static final java.util.Set<String> declined = java.util.Collections.synchronizedSet(new java.util.HashSet<>());
    /** Set while a lock waits for a screen; the next screen that opens asks the node again ({@link #onScreenOpened}). */
    private static volatile boolean waitingForScreen;
    private static volatile boolean asking;
    private static volatile boolean checkedThisSession;
    private static volatile boolean retriedNonce;

    private static void signAll(JSONArray templates, Done done) {
        new Handler(Looper.getMainLooper()).post(() -> confirm(templates, done));
    }

    /**
     * The member already asked for the action; this is the moment their own HARV moves. Every template is checked
     * (this wallet, Harvana chain, the type its purpose requires), then shown with amount, tFare and total, then the
     * PIN or fingerprint. Nothing is signed while no screen is open: the lock waits for one.
     */
    private static void confirm(JSONArray templates, Done done) {
        Context c = SketchApplication.getContext();
        JSONObject first = templates.optJSONObject(0);
        String from = first == null ? "" : first.optString("from", "");
        String active = WalletManager.getActiveWalletAddress(c);
        String owner = from.equalsIgnoreCase(active) || WalletManager.loadPrivateKey(c, from) != null ? from : active;
        String why = com.harvana.app.chain.LockTemplates.checkAll(templates, owner);
        if (why != null) { done.onDone(why); return; }
        boolean allDeclined = true;
        for (int i = 0; i < templates.length(); i++) if (!declined.contains(com.harvana.app.chain.LockTemplates.key(templates.optJSONObject(i)))) allDeclined = false;
        if (allDeclined) { done.onDone("You cancelled the payment."); return; }
        Activity screen = com.harvana.app.utils.ActivityTracker.resumed();
        if (screen == null || asking) {
            waitingForScreen = true;
            com.harvana.app.notify.NotificationCenter.post(c, com.harvana.app.notify.NotificationCenter.TX,
                "lock_wait_" + from, "Payment waiting", "Open Harvana to sign it.", "", -1);
            done.onDone("Open Harvana to sign this payment.");
            return;
        }
        asking = true;
        java.util.List<String[]> rows = new java.util.ArrayList<>();
        long total = 0;
        String ticker = com.harvana.app.CoinInfo.ticker();
        for (int i = 0; i < templates.length(); i++) {
            JSONObject t = templates.optJSONObject(i);
            String purpose = com.harvana.app.notify.TxEvents.purpose(t.optString("memo", ""));
            long amt = t.optLong("amount"), fee = t.optLong("maxFee");
            total += amt + fee;
            String n = templates.length() > 1 ? " " + (i + 1) : "";
            rows.add(new String[]{"For" + n, purpose.isEmpty() ? t.optString("type") : purpose});
            rows.add(new String[]{"Amount" + n, com.harvana.app.notify.TxEvents.amt(HarvanaNodeClient.toHarvDisplay(amt)) + " " + ticker});
            rows.add(new String[]{"tFare" + n, com.harvana.app.notify.TxEvents.amt(HarvanaNodeClient.toHarvDisplay(fee)) + " " + ticker});
            rows.add(new String[]{"To" + n, shortAddr(t.optString("to"))});
        }
        rows.add(new String[]{"From", shortAddr(from)});
        final boolean[] answered = {false};
        com.harvana.app.group.PaymentConfirmSheet.reviewThenAuthorize(screen, "Sign payment",
            com.harvana.app.notify.TxEvents.amt(HarvanaNodeClient.toHarvDisplay(total)) + " " + ticker, rows,
            () -> { answered[0] = true; asking = false; signNow(templates, done); },
            () -> {
                if (answered[0]) return;
                answered[0] = true;
                asking = false;
                for (int i = 0; i < templates.length(); i++) declined.add(com.harvana.app.chain.LockTemplates.key(templates.optJSONObject(i)));
                done.onDone("You cancelled the payment.");
            });
    }

    /** A screen opened: a lock that waited for one, or one lost with its response, is offered now (once per app run otherwise). */
    public static void onScreenOpened(Context c) {
        if (!waitingForScreen && checkedThisSession) return;
        waitingForScreen = false;
        checkedThisSession = true;
        String addr = WalletManager.getActiveWalletAddress(c);
        if (addr != null && !addr.isEmpty()) checkPending(addr, err -> { });
    }

    private static String shortAddr(String a) { return a == null || a.length() < 16 ? String.valueOf(a) : a.substring(0, 10) + "…" + a.substring(a.length() - 6); }

    private static void signNow(JSONArray templates, Done done) {
        retriedNonce = false;
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
                @Override public void onError(String e) {
                    if (e != null && e.contains("INVALID_NONCE") && !retriedNonce) {      // another payment went first: re-read and re-sign
                        retriedNonce = true;
                        HarvanaNodeClient.nodeGet("/api/v1/wallet/nonce/" + from, new HarvanaNodeClient.JsonCallback() {
                            @Override public void onSuccess(JSONObject n) { submitFrom(c, templates, i, n.optLong("nextNonce", 0), done); }
                            @Override public void onError(String e2) { done.onDone(e); }
                        });
                        return;
                    }
                    done.onDone(e);
                }
            });
        } catch (Exception e) {
            done.onDone("Could not sign the lock: " + e.getMessage());
        }
    }
}
