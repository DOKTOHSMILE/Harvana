package com.harvana.app.support;

import android.content.Context;

import com.harvana.app.sync.HarvanaNodeClient;
import com.harvana.app.sync.NodeIdentityClient;
import com.harvana.app.wallet.WalletManager;

import org.json.JSONObject;

/** Signed calls to the member support desk. The node rejects anything this wallet did not sign. */
public final class SupportDesk {
    private SupportDesk() {}

    public interface Cb {
        void ok(JSONObject json);
        void fail(String error);
    }

    public static void open(Context c, String subject, String body, Cb cb) {
        post(c, "/api/v1/support/open", "HARVANA_SUPPORT_OPEN_V1:", subject, body, null, cb);
    }

    public static void send(Context c, String ticketId, String body, Cb cb) {
        post(c, "/api/v1/support/message", "HARVANA_SUPPORT_MESSAGE_V1:", null, body, ticketId, cb);
    }

    public static void mine(Context c, Cb cb) {
        post(c, "/api/v1/support/mine", "HARVANA_SUPPORT_LIST_V1:", null, null, null, cb);
    }

    public static void thread(Context c, String ticketId, Cb cb) {
        post(c, "/api/v1/support/thread", "HARVANA_SUPPORT_THREAD_V1:", null, null, ticketId, cb);
    }

    private static void post(Context c, String path, String prefix, String subject, String body, String ticketId, Cb cb) {
        String address = WalletManager.getActiveWalletAddress(c);
        if (address == null || address.isEmpty()) { cb.fail("Open a wallet first."); return; }
        long ts = System.currentTimeMillis();
        String canonical = prefix + address
                + (subject != null ? ":" + subject : "")
                + (ticketId != null ? ":" + ticketId : "")
                + (body != null ? ":" + body : "")
                + ":" + ts;
        try {
            JSONObject sig = NodeIdentityClient.signMessage(c, address, canonical);
            JSONObject payload = new JSONObject();
            payload.put("address", address);
            payload.put("publicKey", sig.getString("publicKey"));
            payload.put("timestamp", ts);
            payload.put("signature", sig.getString("signature"));
            if (subject != null) payload.put("subject", subject);
            if (body != null) payload.put("body", body);
            if (ticketId != null) payload.put("ticketId", ticketId);
            HarvanaNodeClient.nodePost(path, payload.toString(), new HarvanaNodeClient.JsonCallback() {
                @Override public void onSuccess(JSONObject json) { cb.ok(json); }
                @Override public void onError(String error) { cb.fail(error); }
            });
        } catch (Exception e) {
            cb.fail("Could not sign this message.");
        }
    }
}
