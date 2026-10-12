package com.harvana.app.group;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.harvana.app.sync.AuthTokenProvider;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Calls Harvana Cloud Functions for join approve / reject / vote.
 * Server runs admission + vote threshold inside transactions so
 * membersCount and race conditions stay correct.
 *
 * Deploy functions first (see harvana_functions/). Until deploy succeeds,
 * callers should fall back to the local JoinRequestManager path.
 */
public final class GroupCloudFunctions {

    private static final String TAG = "GroupCloudFunctions";
    private static final String PROJECT_ID = "capital-oracle-project";
    private static final String REGION = "us-central1";
    private static final String BASE =
            "https://" + REGION + "-" + PROJECT_ID + ".cloudfunctions.net/";

    private static final ExecutorService IO = Executors.newCachedThreadPool();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public interface Callback {
        void onSuccess(JSONObject data);
        void onError(String error);
    }

    private GroupCloudFunctions() {}

    public static void approveJoin(String groupId, String requesterUid, int slot, Callback cb) {
        try {
            JSONObject data = new JSONObject();
            data.put("groupId", groupId);
            data.put("requesterUid", requesterUid);
            if (slot > 0) data.put("slot", slot);
            call("approveJoin", data, cb);
        } catch (Exception e) {
            if (cb != null) cb.onError(com.harvana.app.utils.ErrorText.of(e));
        }
    }

    public static void rejectJoin(String groupId, String requesterUid, Callback cb) {
        try {
            JSONObject data = new JSONObject();
            data.put("groupId", groupId);
            data.put("requesterUid", requesterUid);
            call("rejectJoin", data, cb);
        } catch (Exception e) {
            if (cb != null) cb.onError(com.harvana.app.utils.ErrorText.of(e));
        }
    }

    public static void castJoinVote(String groupId, String requesterUid, boolean approve, Callback cb) {
        try {
            JSONObject data = new JSONObject();
            data.put("groupId", groupId);
            data.put("requesterUid", requesterUid);
            data.put("approve", approve);
            call("castJoinVote", data, cb);
        } catch (Exception e) {
            if (cb != null) cb.onError(com.harvana.app.utils.ErrorText.of(e));
        }
    }

    private static void call(String functionName, JSONObject data, Callback cb) {
        IO.execute(() -> {
            HttpURLConnection c = null;
            try {
                String token = AuthTokenProvider.getIdToken(false);
                if (token == null || token.isEmpty()) {
                    postError(cb, "Not signed in.");
                    return;
                }

                JSONObject body = new JSONObject();
                body.put("data", data);

                c = (HttpURLConnection) new URL(BASE + functionName).openConnection();
                c.setRequestMethod("POST");
                c.setConnectTimeout(20_000);
                c.setReadTimeout(30_000);
                c.setDoOutput(true);
                c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                c.setRequestProperty("Authorization", "Bearer " + token);

                byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
                c.setFixedLengthStreamingMode(bytes.length);
                try (OutputStream os = c.getOutputStream()) {
                    os.write(bytes);
                }

                int code = c.getResponseCode();
                String response = read(c, code);

                if (code == 401) {
                    // Retry once with forced token refresh
                    token = AuthTokenProvider.getIdToken(true);
                    if (token == null) {
                        postError(cb, "Auth expired.");
                        return;
                    }
                    c.disconnect();
                    c = (HttpURLConnection) new URL(BASE + functionName).openConnection();
                    c.setRequestMethod("POST");
                    c.setConnectTimeout(20_000);
                    c.setReadTimeout(30_000);
                    c.setDoOutput(true);
                    c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                    c.setRequestProperty("Authorization", "Bearer " + token);
                    bytes = body.toString().getBytes(StandardCharsets.UTF_8);
                    c.setFixedLengthStreamingMode(bytes.length);
                    try (OutputStream os = c.getOutputStream()) {
                        os.write(bytes);
                    }
                    code = c.getResponseCode();
                    response = read(c, code);
                }

                if (code >= 200 && code < 300) {
                    JSONObject json = new JSONObject(response);
                    // Callable protocol: { "result": { ... } }
                    JSONObject result = json.optJSONObject("result");
                    if (result == null) result = json;
                    final JSONObject out = result;
                    MAIN.post(() -> {
                        if (cb != null) cb.onSuccess(out);
                    });
                } else {
                    String msg = response;
                    try {
                        JSONObject err = new JSONObject(response);
                        if (err.has("error")) {
                            Object e = err.get("error");
                            if (e instanceof JSONObject) {
                                msg = ((JSONObject) e).optString("message", response);
                            } else {
                                msg = String.valueOf(e);
                            }
                        }
                    } catch (Exception ignored) {}
                    postError(cb, com.harvana.app.utils.ErrorText.withStatus(code, msg));
                }
            } catch (Exception e) {
                Log.w(TAG, functionName + " failed", e);
                postError(cb, e.getMessage() != null ? e.getMessage() : "network error");
            } finally {
                if (c != null) c.disconnect();
            }
        });
    }

    private static void postError(Callback cb, String error) {
        MAIN.post(() -> {
            if (cb != null) cb.onError(error != null ? error : "error");
        });
    }

    private static String read(HttpURLConnection c, int code) throws Exception {
        java.io.InputStream in = (code >= 200 && code < 300)
                ? c.getInputStream() : c.getErrorStream();
        if (in == null) return "";
        BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close();
        return sb.toString();
    }
}
