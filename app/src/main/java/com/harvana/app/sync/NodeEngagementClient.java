package com.harvana.app.sync;

import android.content.Context;

import com.harvana.app.chain.EngagementCanonical;

import org.json.JSONObject;

/**
 * Sends the node a signed receipt when a reader finishes a Learn Hub piece, and reads back the owner's own activity
 * breakdown. The node decides what counts (time on the piece, one count per piece, daily cap, automated patterns);
 * the app only reports what the reader did. See the node's docs/TRUST_SCORE.md.
 */
public final class NodeEngagementClient {
    private NodeEngagementClient() {}

    public interface Callback {
        void onSuccess(JSONObject json);
        void onError(String error);
    }

    public static void submitRead(Context c, String address, String contentId, int wordCount, long dwellMs, boolean reachedEnd, Callback cb) {
        try {
            long ts = System.currentTimeMillis();
            JSONObject body = NodeIdentityClient.signed(c, address, EngagementCanonical.read(address, contentId, dwellMs, ts));
            body.put("address", address).put("contentId", contentId).put("wordCount", wordCount)
                .put("dwellMs", dwellMs).put("reachedEnd", reachedEnd).put("timestamp", ts);
            post("/api/v1/engagement/read", body, cb);
        } catch (Exception e) { cb.onError(com.harvana.app.utils.ErrorText.of(e)); }
    }

    /** Tells the node on which UTC days (yyyyMMdd) this account was really in use on the phone; feeds days-active in the trust score. */
    public static void submitLiveness(Context c, String address, java.util.List<String> days, Callback cb) {
        try {
            long ts = System.currentTimeMillis();
            String csv = android.text.TextUtils.join(",", days);
            JSONObject body = NodeIdentityClient.signed(c, address, EngagementCanonical.liveness(address, csv, ts));
            org.json.JSONArray arr = new org.json.JSONArray();
            for (String d : days) arr.put(d);
            body.put("address", address).put("days", arr).put("timestamp", ts);
            post("/api/v1/engagement/liveness", body, cb);
        } catch (Exception e) { cb.onError(com.harvana.app.utils.ErrorText.of(e)); }
    }

    public static void ownerStats(Context c, String address, Callback cb) {
        try {
            long ts = System.currentTimeMillis();
            JSONObject body = NodeIdentityClient.signed(c, address, EngagementCanonical.stats(address, ts));
            body.put("address", address).put("timestamp", ts);
            post("/api/v1/engagement/stats", body, cb);
        } catch (Exception e) { cb.onError(com.harvana.app.utils.ErrorText.of(e)); }
    }

    private static void post(String path, JSONObject body, Callback cb) {
        HarvanaNodeClient.nodePost(path, body.toString(), new HarvanaNodeClient.JsonCallback() {
            @Override public void onSuccess(JSONObject j) { cb.onSuccess(j); }
            @Override public void onError(String e) { cb.onError(e); }
        });
    }
}
