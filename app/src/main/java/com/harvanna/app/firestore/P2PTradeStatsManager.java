package com.harvanna.app.firestore;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;

import okhttp3.*;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * GROUND-UP (Sept 2026) — P2P trade reputation stats, moved here after
 * being wrongly built on the node first. Trade count and success rate are
 * public profile/reputation data — the same category as TrustScoreEngine's
 * groups/cycles/tenure, which already live in Firestore, not the node.
 * The node's job is custody and signature verification of actual fund
 * movement; a full order-history scan to compute a reputation percentage
 * has nothing to do with that and doesn't belong running on every
 * candidate check in the Express matcher.
 *
 * Same REST-not-SDK, OkHttp, Bearer-token pattern as UserProfileManager —
 * kept as its own small class/collection (P2P_TRADE_STATS/{uid}) rather
 * than folded into USERS/{uid}, since this is conceptually its own
 * lifecycle (incremented once per terminal order, read far more often
 * than it's written).
 *
 * Self-reported by design, same trust model as the rest of this app's
 * Firestore-side reputation data (nothing here is enforced by a backend
 * function) — each party records their own outcome when they observe a
 * terminal order status client-side. That's a real limit worth naming
 * plainly: it's not a security boundary, it's a reputation signal.
 */
public class P2PTradeStatsManager {

    private static final String TAG = "P2PTradeStatsManager";
    private static final String PROJECT_ID = "capital-oracle-project";
    private static final String BASE_URL = "https://firestore.googleapis.com/v1/projects/" + PROJECT_ID + "/databases/(default)/documents";

    private static P2PTradeStatsManager instance;
    private final OkHttpClient client;

    public static synchronized P2PTradeStatsManager getInstance(Context context) {
        if (instance == null) instance = new P2PTradeStatsManager();
        return instance;
    }

    private P2PTradeStatsManager() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();
    }

    public interface StatsCallback {
        /** successRatePercent is 0 when totalTrades is 0 — no trades yet
         *  is not the same as a 0% success rate, callers should check
         *  totalTrades before reading anything into the rate. */
        void onSuccess(int totalTrades, int successfulTrades, double successRatePercent);
        void onError(String error);
    }

    public interface SimpleCallback {
        void onSuccess();
        void onError(String error);
    }

    // GROUND-UP REWRITE (Sept 2026): the client-write path that used to
    // live here (hasRecordedOutcomeFor/recordOutcomeOnce) is gone on
    // purpose — see the class doc. The node is now the only writer of
    // completed-trade events, as a side effect of a release it already
    // signature-verified. Firestore rules should deny client writes to
    // P2P_COMPLETED_TRADES outright; this class is read-only now.

    /** Query-based, not a counter — see class doc. Currently only counts
     *  genuine successes (the node only writes P2P_COMPLETED_TRADES on
     *  the real COMPLETED path). Cancelled orders and disputes resolved
     *  against someone aren't recorded anywhere yet, so a true success
     *  RATE (successes ÷ everything attempted) isn't answerable honestly
     *  right now — only a real completed-trade count is. successRate is
     *  returned as -1 to make that explicit rather than quietly showing
     *  a misleading 100%; callers should treat -1 as "not available,"
     *  not "zero." */
    public interface FullStatsCallback {
        /** Same successRatePercent honesty rule as StatsCallback.
         *  uniqueCounterparties is real, not estimated — derived by
         *  reading the actual counterparty out of each completed-trade
         *  record this uid appears in and counting distinct ones, using
         *  data the node already writes (participantUids) rather than a
         *  new collection or a new write path. */
        void onSuccess(int totalTrades, int successfulTrades, double successRatePercent, int uniqueCounterparties);
        void onError(String error);
    }

    public void getFullStats(String uid, FullStatsCallback callback) {
        try {
            JSONObject query = new JSONObject();
            JSONObject structuredQuery = new JSONObject();
            JSONArray from = new JSONArray();
            from.put(new JSONObject().put("collectionId", "P2P_COMPLETED_TRADES"));
            structuredQuery.put("from", from);
            JSONObject fieldFilter = new JSONObject();
            fieldFilter.put("field", new JSONObject().put("fieldPath", "participantUids"));
            fieldFilter.put("op", "ARRAY_CONTAINS");
            fieldFilter.put("value", new JSONObject().put("stringValue", uid));
            structuredQuery.put("where", new JSONObject().put("fieldFilter", fieldFilter));
            query.put("structuredQuery", structuredQuery);

            _authorizedRequest("POST", ":runQuery", query, new _RawCallback() {
                @Override
                public void onSuccess(String body) {
                    try {
                        JSONArray results = new JSONArray(body);
                        int count = 0;
                        java.util.Set<String> counterparties = new java.util.HashSet<>();
                        for (int i = 0; i < results.length(); i++) {
                            JSONObject entry = results.getJSONObject(i);
                            if (!entry.has("document")) continue;
                            count++;
                            JSONObject doc = entry.getJSONObject("document");
                            Map<String, Object> fields = _decodeDocument(doc);
                            Object participantsObj = fields.get("participantUids");
                            if (participantsObj instanceof List) {
                                for (Object p : (List<?>) participantsObj) {
                                    String pUid = String.valueOf(p);
                                    if (!uid.equals(pUid)) counterparties.add(pUid);
                                }
                            }
                        }
                        callback.onSuccess(count, count, -1, counterparties.size());
                    } catch (Exception e) {
                        callback.onError("Failed to parse trade-stats query: " + e.getMessage());
                    }
                }
                @Override
                public void onError(String error) { callback.onError(error); }
            });
        } catch (Exception e) {
            callback.onError("Failed to build trade-stats query: " + e.getMessage());
        }
    }

    public void getStats(String uid, StatsCallback callback) {
        try {
            JSONObject query = new JSONObject();
            JSONObject structuredQuery = new JSONObject();
            JSONArray from = new JSONArray();
            from.put(new JSONObject().put("collectionId", "P2P_COMPLETED_TRADES"));
            structuredQuery.put("from", from);
            JSONObject fieldFilter = new JSONObject();
            fieldFilter.put("field", new JSONObject().put("fieldPath", "participantUids"));
            fieldFilter.put("op", "ARRAY_CONTAINS");
            fieldFilter.put("value", new JSONObject().put("stringValue", uid));
            structuredQuery.put("where", new JSONObject().put("fieldFilter", fieldFilter));
            query.put("structuredQuery", structuredQuery);

            _authorizedRequest("POST", ":runQuery", query, new _RawCallback() {
                @Override
                public void onSuccess(String body) {
                    try {
                        JSONArray results = new JSONArray(body);
                        int count = 0;
                        for (int i = 0; i < results.length(); i++) {
                            if (results.getJSONObject(i).has("document")) count++;
                        }
                        callback.onSuccess(count, count, -1);
                    } catch (Exception e) {
                        callback.onError("Failed to parse trade-stats query: " + e.getMessage());
                    }
                }
                @Override
                public void onError(String error) { callback.onError(error); }
            });
        } catch (Exception e) {
            callback.onError("Failed to build trade-stats query: " + e.getMessage());
        }
    }

    // ── Firestore typed-value encoding (same wire format as UserProfileManager) ──

    private JSONObject _encodeFields(Map<String, Object> fields) {
        try {
            JSONObject root = new JSONObject();
            JSONObject fieldsObj = new JSONObject();
            for (Map.Entry<String, Object> entry : fields.entrySet()) {
                fieldsObj.put(entry.getKey(), _encodeValue(entry.getValue()));
            }
            root.put("fields", fieldsObj);
            return root;
        } catch (Exception e) {
            Log.e(TAG, "Failed to encode fields: " + e.getMessage());
            return new JSONObject();
        }
    }

    private JSONObject _encodeValue(Object value) throws Exception {
        JSONObject wrapper = new JSONObject();
        if (value == null) {
            wrapper.put("nullValue", JSONObject.NULL);
        } else if (value instanceof String) {
            wrapper.put("stringValue", value);
        } else if (value instanceof Integer || value instanceof Long) {
            wrapper.put("integerValue", String.valueOf(value));
        } else if (value instanceof Double || value instanceof Float) {
            wrapper.put("doubleValue", value);
        } else if (value instanceof Boolean) {
            wrapper.put("booleanValue", value);
        } else {
            wrapper.put("stringValue", String.valueOf(value));
        }
        return wrapper;
    }

    private Map<String, Object> _decodeDocument(JSONObject doc) throws Exception {
        Map<String, Object> result = new HashMap<>();
        if (!doc.has("fields")) return result;
        JSONObject fields = doc.getJSONObject("fields");
        Iterator<String> keys = fields.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            result.put(key, _decodeValue(fields.getJSONObject(key)));
        }
        return result;
    }

    private Object _decodeValue(JSONObject wrapper) throws Exception {
        if (wrapper.has("stringValue")) return wrapper.getString("stringValue");
        if (wrapper.has("integerValue")) return Long.parseLong(wrapper.getString("integerValue"));
        if (wrapper.has("doubleValue")) return wrapper.getDouble("doubleValue");
        if (wrapper.has("booleanValue")) return wrapper.getBoolean("booleanValue");
        if (wrapper.has("arrayValue")) {
            List<Object> result = new java.util.ArrayList<>();
            JSONObject arrayValue = wrapper.getJSONObject("arrayValue");
            if (arrayValue.has("values")) {
                JSONArray values = arrayValue.getJSONArray("values");
                for (int i = 0; i < values.length(); i++) {
                    result.add(_decodeValue(values.getJSONObject(i)));
                }
            }
            return result;
        }
        return null;
    }

    // ── HTTP + auth plumbing (identical to UserProfileManager) ──────────

    private interface _RawCallback {
        void onSuccess(String body);
        void onError(String error);
    }

    private void _authorizedRequest(String method, String path, JSONObject body, _RawCallback callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            callback.onError("Not signed in — no current Firebase user.");
            return;
        }
        user.getIdToken(false).addOnCompleteListener(new OnCompleteListener<GetTokenResult>() {
            @Override
            public void onComplete(Task<GetTokenResult> task) {
                if (task.isSuccessful() && task.getResult() != null) {
                    _executeRequest(method, path, body, task.getResult().getToken(), callback, true);
                } else {
                    String msg = task.getException() != null ? task.getException().getMessage() : "unknown error";
                    callback.onError("Failed to obtain ID token: " + msg);
                }
            }
        });
    }

    private void _executeRequest(String method, String path, JSONObject body, String idToken, _RawCallback callback, boolean allowRetryOn401) {
        Request.Builder builder = new Request.Builder()
                .url(BASE_URL + path)
                .header("Authorization", "Bearer " + idToken);

        RequestBody requestBody = null;
        if (body != null) {
            requestBody = RequestBody.create(body.toString(), MediaType.parse("application/json"));
        }

        switch (method) {
            case "GET": builder.get(); break;
            case "POST": builder.post(requestBody != null ? requestBody : RequestBody.create("", MediaType.parse("application/json"))); break;
            case "PATCH": builder.patch(requestBody != null ? requestBody : RequestBody.create("", MediaType.parse("application/json"))); break;
            default: callback.onError("Unsupported HTTP method: " + method); return;
        }

        client.newCall(builder.build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                if (response.code() == 401 && allowRetryOn401) {
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user == null) {
                        callback.onError("Not signed in — can't refresh token after 401.");
                        return;
                    }
                    user.getIdToken(true).addOnCompleteListener(new OnCompleteListener<GetTokenResult>() {
                        @Override
                        public void onComplete(Task<GetTokenResult> task) {
                            if (task.isSuccessful() && task.getResult() != null) {
                                _executeRequest(method, path, body, task.getResult().getToken(), callback, false);
                            } else {
                                String msg = task.getException() != null ? task.getException().getMessage() : "unknown error";
                                callback.onError("Token refresh failed after 401: " + msg);
                            }
                        }
                    });
                    return;
                }
                if (!response.isSuccessful()) {
                    callback.onError("HTTP " + response.code() + ": " + responseBody);
                    return;
                }
                callback.onSuccess(responseBody);
            }
        });
    }
}
