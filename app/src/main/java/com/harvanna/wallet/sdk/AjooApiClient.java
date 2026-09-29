package com.harvanna.wallet.sdk;

import android.os.Handler;
import android.os.Looper;
import okhttp3.*;
import org.json.JSONObject;
import java.io.IOException;

/**
 * OkHttp REST client for direct AJC node calls.
 * Endpoint corrected to /api/v1/tx/send (was /v1/transactions).
 * Payload now matches server TransactionPayload exactly.
 */
public final class AjooApiClient {

    private static String baseUrl;
    private static final OkHttpClient CLIENT = new OkHttpClient();
    
    // FIX 1: Added a Main Thread Handler so UI updates don't crash the app.
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    private AjooApiClient() {}

    public interface Callback {
        void onSuccess(String body);
        void onError(String error);
    }

    public static void configure(String url) {
        if (url == null || url.trim().isEmpty())
            throw new IllegalArgumentException("URL cannot be empty");
        baseUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    /**
     * POST /api/v1/tx/send
     *
     * Sends a fully signed transaction payload to the node.
     * The node validates, adds to mempool, and writes Firestore after confirmation.
     *
     * Required fields in payload JSON:
     *   from, to, amount (raw long), gas (raw long), nonce,
     *   signature (Base64), publicKey (Base64)
     *
     * Build the payload JSON using AjooNodeClient.buildCanonicalMessage() for signing.
     */
    public static void submitSignedTransaction(JSONObject payload, Callback cb) {
        if (baseUrl == null || baseUrl.isEmpty()) {
            // Immediately route configuration errors to the callback
            mainHandler.post(() -> cb.onError("AjooApiClient not configured. Call configure(url) first."));
            return;
        }
            
        try {
            MediaType JSON = MediaType.parse("application/json; charset=utf-8");
            Request req = new Request.Builder()
                    .url(baseUrl + "/api/v1/tx/send")
                    .post(RequestBody.create(JSON, payload.toString()))
                    .build();

            CLIENT.newCall(req).enqueue(new okhttp3.Callback() {
                @Override
                public void onFailure(Call c, IOException e) { 
                    mainHandler.post(() -> cb.onError(e.getMessage() != null ? e.getMessage() : "Network error")); 
                }
                
                @Override
                public void onResponse(Call c, Response r) {
                    // FIX 2: Wrapped in try-catch because r.body().string() can throw IOException
                    try {
                        final String b = r.body() == null ? "" : r.body().string();
                        if (r.isSuccessful()) {
                            mainHandler.post(() -> cb.onSuccess(b));
                        } else {
                            mainHandler.post(() -> cb.onError("HTTP " + r.code() + ": " + b));
                        }
                    } catch (Exception e) {
                        mainHandler.post(() -> cb.onError("Failed to read server response: " + e.getMessage()));
                    }
                }
            });
        } catch (Exception e) {
            mainHandler.post(() -> cb.onError(e.getMessage()));
        }
    }

    /**
     * GET /api/v1/tx/status/{txHash}
     */
    public static void getTxStatus(String txHash, Callback cb) {
        if (baseUrl == null || baseUrl.isEmpty()) {
            mainHandler.post(() -> cb.onError("AjooApiClient not configured."));
            return;
        }
            
        try {
            Request req = new Request.Builder()
                    .url(baseUrl + "/api/v1/tx/status/" + txHash)
                    .get()
                    .build();
                    
            CLIENT.newCall(req).enqueue(new okhttp3.Callback() {
                @Override
                public void onFailure(Call c, IOException e) { 
                    mainHandler.post(() -> cb.onError(e.getMessage() != null ? e.getMessage() : "Network error")); 
                }
                
                @Override
                public void onResponse(Call c, Response r) {
                    // FIX 2: Wrapped in try-catch
                    try {
                        final String b = r.body() == null ? "" : r.body().string();
                        if (r.isSuccessful()) {
                            mainHandler.post(() -> cb.onSuccess(b));
                        } else {
                            mainHandler.post(() -> cb.onError("HTTP " + r.code() + ": " + b));
                        }
                    } catch (Exception e) {
                        mainHandler.post(() -> cb.onError("Failed to read server response: " + e.getMessage()));
                    }
                }
            });
        } catch (Exception e) {
            mainHandler.post(() -> cb.onError(e.getMessage()));
        }
    }
}
