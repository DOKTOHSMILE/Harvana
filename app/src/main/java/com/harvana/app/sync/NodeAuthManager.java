package com.harvana.app.sync;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Node-wide authentication. Every request this app makes to the node —
 * wallet balance, history, fee estimates, sending funds, P2P listings,
 * everything — now requires a valid auth token, not just the transaction's
 * own signature. HarvanaNodeClient's get()/post() call here automatically
 * before every request; nothing else in the app needs to know this exists.
 *
 * Reuses the exact same token contract already proven working against
 * this node by com.harvana.node.miner's mining-auth flow: POST address +
 * a device clientId to /api/v1/mining/auth, get back {token, expiresAt},
 * send the token back as "Authorization: Bearer <token>". The endpoint
 * path still says "mining" for historical reasons on the node side, but
 * the token it issues is a general node-session credential, not something
 * scoped to mining specifically — there's nothing mining-specific about
 * address+clientId-in, token-out.
 *
 * Concurrent callers are coalesced into a single in-flight fetch rather
 * than each firing their own request — a screen that fires five endpoint
 * calls at once on first load (a very normal pattern) would otherwise
 * cause five simultaneous, redundant token requests.
 */
public final class NodeAuthManager {

    private static final String PREFS = "harvana_node_auth";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_EXPIRES_AT = "expiresAt";

    /** Refresh this many ms before actual expiry, so a request already
     *  in flight doesn't get rejected mid-air by a token expiring the
     *  instant it's used. */
    private static final long REFRESH_MARGIN_MS = 30_000L;

    /** The node's rate limit on this auth endpoint has been observed to
     *  reject even ordinary, non-abusive traffic (a single P2P screen
     *  load, first token request of a session) with HTTP 429. That's a
     *  server-side limit this client can't remove — this is a mitigation,
     *  not a fix. On a 429 specifically, back off and retry a few times
     *  before surfacing an error, rather than failing the screen instantly
     *  on what may just be a momentary limit. */
    private static final int MAX_429_RETRIES = 3;
    private static final long RETRY_BASE_DELAY_MS = 3_000L;
    private static final Handler retryHandler = new Handler(Looper.getMainLooper());

    public interface TokenCallback {
        void onToken(String token);
        void onError(String error);
    }

    private static final Object lock = new Object();
    private static boolean fetchInFlight = false;
    private static final List<TokenCallback> waiters = new ArrayList<>();

    private NodeAuthManager() {}

    public static void getValidToken(TokenCallback cb) {
        Context context = com.harvana.app.SketchApplication.getContext();
        if (context == null) {
            cb.onError("The app is still starting. Try again in a moment.");
            return;
        }

        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String cached = prefs.getString(KEY_TOKEN, null);
        long expiresAtMs = toExpiryMs(prefs.getLong(KEY_EXPIRES_AT, 0L));

        if (cached != null && !cached.isEmpty()
                && System.currentTimeMillis() < (expiresAtMs - REFRESH_MARGIN_MS)) {
            cb.onToken(cached);
            return;
        }

        synchronized (lock) {
            waiters.add(cb);
            if (fetchInFlight) {
                // Another caller already triggered a fetch — this one
                // will be served the same result when it lands.
                return;
            }
            fetchInFlight = true;
        }

        String address = com.harvana.app.wallet.WalletManager.getActiveWalletAddress(context);
        if (address == null || address.isEmpty()) {
            failAllWaiters("No wallet on this device — cannot authenticate with the node");
            return;
        }

        String clientId;
        try {
            clientId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        } catch (Exception e) {
            clientId = "android";
        }
        if (clientId == null || clientId.isEmpty()) clientId = "android";

        _fetchWithRetry(context, prefs, address, clientId, 0);
    }

    private static void _fetchWithRetry(Context context, SharedPreferences prefs,
                                         String address, String clientId, int attempt) {
        HarvanaNodeClient.requestNodeToken(context, address, clientId, new HarvanaNodeClient.JsonCallback() {
            @Override
            public void onSuccess(JSONObject json) {
                String token = json.optString("token", json.optString("miningToken", ""));
                long exp = json.optLong("expiresAt", json.optLong("expires_at", 0L));
                if (token.isEmpty() && json.has("data")) {
                    JSONObject data = json.optJSONObject("data");
                    if (data != null) {
                        token = data.optString("token", data.optString("miningToken", ""));
                        if (exp <= 0L) exp = data.optLong("expiresAt", data.optLong("expires_at", 0L));
                    }
                }
                if (token.isEmpty()) {
                    failAllWaiters("Node returned an empty auth token");
                    return;
                }

                // Node sends epoch seconds. Cache compare uses currentTimeMillis.
                prefs.edit()
                        .putString(KEY_TOKEN, token)
                        .putLong(KEY_EXPIRES_AT, toExpiryMs(exp))
                        .apply();

                succeedAllWaiters(token);
            }

            @Override
            public void onError(String error) {
                boolean isRateLimited = com.harvana.app.utils.ErrorText.is(error, "429");
                if (isRateLimited && attempt < MAX_429_RETRIES) {
                    long delay = RETRY_BASE_DELAY_MS * (1L << attempt);
                    retryHandler.postDelayed(
                            () -> _fetchWithRetry(context, prefs, address, clientId, attempt + 1),
                            delay);
                    return;
                }
                failAllWaiters(isRateLimited
                        ? "The network is busy. Try again in a moment."
                        : com.harvana.app.utils.ErrorText.plain(error));
            }
        });
    }

    /** Node expiresAt is seconds. Older cache rows may already be ms. */
    private static long toExpiryMs(long exp) {
        if (exp <= 0L) return 0L;
        if (exp < 10_000_000_000L) return exp * 1000L;
        return exp;
    }

    /** Call after any request comes back 401/403 — the cached token is
     *  clearly no longer valid regardless of what expiry it claimed. */
    public static void invalidateCachedToken() {
        Context context = com.harvana.app.SketchApplication.getContext();
        if (context == null) return;
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_TOKEN)
                .remove(KEY_EXPIRES_AT)
                .apply();
    }

    private static void succeedAllWaiters(String token) {
        List<TokenCallback> toNotify;
        synchronized (lock) {
            toNotify = new ArrayList<>(waiters);
            waiters.clear();
            fetchInFlight = false;
        }
        for (TokenCallback w : toNotify) w.onToken(token);
    }

    private static void failAllWaiters(String error) {
        List<TokenCallback> toNotify;
        synchronized (lock) {
            toNotify = new ArrayList<>(waiters);
            waiters.clear();
            fetchInFlight = false;
        }
        for (TokenCallback w : toNotify) w.onError(error);
    }
}
