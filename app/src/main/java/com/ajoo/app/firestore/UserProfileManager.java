package com.ajoo.app.firestore;

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
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * REST-based Firestore access for user profile data - USERS/{uid}. Same
 * REST-not-SDK, OkHttp, Bearer-token pattern as GroupFirestoreManager
 * (kept as a separate class rather than folded into it, since "user
 * profile" and "group data" are conceptually distinct collections with
 * their own lifecycle).
 *
 * Migrated from RTDB (userdb/{uid}) as of this change - a clean cutover,
 * not a dual-write, since this project's existing RTDB user data is test
 * data being discarded, not real users being migrated live.
 *
 * REQUIRES a real, signed-in FirebaseAuth.getCurrentUser() to get a
 * Bearer token for every call here - this is exactly why signup's old
 * hardcoded-OTP-with-commented-out-signInWithCredential() path could
 * never have worked against Firestore. Real auth (anonymous sign-in,
 * wired in as part of this same change) has to establish a genuine
 * session before any of these calls can succeed.
 */
public class UserProfileManager {

    private static final String TAG = "UserProfileManager";
    private static final String PROJECT_ID = "capital-oracle-project";
    private static final String BASE_URL = "https://firestore.googleapis.com/v1/projects/" + PROJECT_ID + "/databases/(default)/documents";

    private static UserProfileManager instance;
    private final OkHttpClient client;

    public static synchronized UserProfileManager getInstance(Context context) {
        if (instance == null) {
            instance = new UserProfileManager();
        }
        return instance;
    }

    private UserProfileManager() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();
    }

    public interface DocCallback {
        void onSuccess(Map<String, Object> data);
        void onError(String error);
    }

    public interface SimpleCallback {
        void onSuccess();
        void onError(String error);
    }

    /** Full-document PATCH - used for signup (creating the doc) and any
     *  case that's genuinely replacing the whole profile, not one field. */
    public void saveProfile(String uid, Map<String, Object> fields, SimpleCallback callback) {
        _authorizedRequest("PATCH", "/USERS/" + uid, _encodeFields(fields), new _RawCallback() {
            @Override public void onSuccess(String body) { callback.onSuccess(); }
            @Override public void onError(String error) { callback.onError(error); }
        });
    }

    /** Masked partial update - only touches the fields provided, same
     *  distinction GroupFirestoreManager draws between addMember() (full
     *  replace) and updateMemberFields() (masked) - the same class of bug
     *  (silently wiping unrelated fields) applies here just as much. */
    public void updateProfileFields(String uid, Map<String, Object> fields, SimpleCallback callback) {
        StringBuilder mask = new StringBuilder();
        for (String key : fields.keySet()) {
            if (mask.length() > 0) mask.append("&");
            mask.append("updateMask.fieldPaths=").append(key);
        }
        _authorizedRequest("PATCH", "/USERS/" + uid + "?" + mask, _encodeFields(fields), new _RawCallback() {
            @Override public void onSuccess(String body) { callback.onSuccess(); }
            @Override public void onError(String error) { callback.onError(error); }
        });
    }

    /**
     * Adds a group id to this user's "groups" array field on USERS/{uid}.
     * Reads the doc first: if "groups" already exists, the id is appended
     * (skipping it if already present); if the doc has no "groups" field yet
     * (or the doc itself doesn't exist - 404), the field is created fresh
     * with just this one id. Callers: CreateNewGroupActivity (creator joins
     * their own new group) and JoinRequestManager.appendGroupToUserProfile
     * (join-request approval, both the admin-approve and vote-approve paths).
     */
    public void addGroupToProfile(String uid, String groupId, SimpleCallback callback) {
        getProfile(uid, new DocCallback() {
            @Override
            public void onSuccess(Map<String, Object> data) {
                _mergeAndSaveGroup(uid, data, groupId, callback);
            }
            @Override
            public void onError(String error) {
                if (error != null && error.contains("404")) {
                    // No profile doc / no "groups" field yet - create it fresh.
                    _mergeAndSaveGroup(uid, null, groupId, callback);
                } else {
                    callback.onError(error);
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void _mergeAndSaveGroup(String uid, Map<String, Object> data, String groupId, SimpleCallback callback) {
        List<Object> groups = new ArrayList<>();
        Object existing = data != null ? data.get("groups") : null;
        if (existing instanceof List) {
            groups.addAll((List<Object>) existing);
        }
        boolean alreadyIn = false;
        for (Object g : groups) {
            if (g != null && groupId.equals(String.valueOf(g))) {
                alreadyIn = true;
                break;
            }
        }
        if (!alreadyIn) groups.add(groupId);

        Map<String, Object> update = new HashMap<>();
        update.put("groups", groups);
        updateProfileFields(uid, update, callback);
    }

    /** Removes a group id from USERS/{uid}.groups. Call on leave. */
    public void removeGroupFromProfile(String uid, String groupId, SimpleCallback callback) {
        getProfile(uid, new DocCallback() {
            @Override
            public void onSuccess(Map<String, Object> data) {
                _mergeArrayField(uid, data, "groups", groupId, false, callback);
            }
            @Override
            public void onError(String error) {
                if (error != null && error.contains("404")) {
                    callback.onSuccess();
                } else {
                    callback.onError(error);
                }
            }
        });
    }

    // ── Pending group ids (join requests awaiting approval) ────────────

    /** Adds a group id to USERS/{uid}.pendingGroupIds. Call right after a
     *  join request is successfully submitted. */
    public void addPendingGroupToProfile(String uid, String groupId, SimpleCallback callback) {
        getProfile(uid, new DocCallback() {
            @Override
            public void onSuccess(Map<String, Object> data) {
                _mergeArrayField(uid, data, "pendingGroupIds", groupId, true, callback);
            }
            @Override
            public void onError(String error) {
                if (error != null && error.contains("404")) {
                    _mergeArrayField(uid, null, "pendingGroupIds", groupId, true, callback);
                } else {
                    callback.onError(error);
                }
            }
        });
    }

    /** Removes a group id from USERS/{uid}.pendingGroupIds. Call once a
     *  join request resolves (approved or rejected) — approval also calls
     *  addGroupToProfile() separately; this only clears the pending marker. */
    public void removePendingGroupFromProfile(String uid, String groupId, SimpleCallback callback) {
        getProfile(uid, new DocCallback() {
            @Override
            public void onSuccess(Map<String, Object> data) {
                _mergeArrayField(uid, data, "pendingGroupIds", groupId, false, callback);
            }
            @Override
            public void onError(String error) {
                // Nothing to remove from a profile that doesn't exist / has
                // no pendingGroupIds field yet — treat as already-clean.
                if (error != null && error.contains("404")) {
                    callback.onSuccess();
                } else {
                    callback.onError(error);
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void _mergeArrayField(String uid, Map<String, Object> data, String fieldName,
                                  String value, boolean add, SimpleCallback callback) {
        List<Object> items = new ArrayList<>();
        Object existing = data != null ? data.get(fieldName) : null;
        if (existing instanceof List) {
            items.addAll((List<Object>) existing);
        }
        boolean present = false;
        for (Object item : items) {
            if (item != null && value.equals(String.valueOf(item))) {
                present = true;
                break;
            }
        }
        if (add && !present) {
            items.add(value);
        } else if (!add && present) {
            items.removeIf(item -> item != null && value.equals(String.valueOf(item)));
        } else if (!add) {
            // Nothing to remove — still a no-op success, not an error.
            callback.onSuccess();
            return;
        } else {
            // Already present — no-op success (dedupe).
            callback.onSuccess();
            return;
        }

        Map<String, Object> update = new HashMap<>();
        update.put(fieldName, items);
        updateProfileFields(uid, update, callback);
    }

    /** Real presence, self-reported — same mechanism every messaging app
     *  uses ("last seen"), not a fabricated always-on status. Call this
     *  when the current signed-in user is actively viewing something
     *  presence-relevant (e.g. the marketplace); reading it back
     *  elsewhere (isRecentlyActive below) is how another user's row gets
     *  a real "Online" indicator instead of an invented one. */
    public void markSelfActiveNow(String uid) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("lastActiveAt", System.currentTimeMillis());
        updateProfileFields(uid, fields, new SimpleCallback() {
            @Override public void onSuccess() { }
            @Override public void onError(String error) { } // best-effort — presence is a UI nicety, never worth surfacing a failure for
        });
    }

    /** True only if lastActiveAt is a real, recent timestamp — never true
     *  by default for a profile that's never called markSelfActiveNow(). */
    public static boolean isRecentlyActive(Map<String, Object> profileData) {
        Object raw = profileData.get("lastActiveAt");
        if (!(raw instanceof Number)) return false;
        long lastActiveAt = ((Number) raw).longValue();
        return (System.currentTimeMillis() - lastActiveAt) < ONLINE_WINDOW_MS;
    }

    private static final long ONLINE_WINDOW_MS = 5 * 60 * 1000L; // 5 minutes

    public void getProfile(String uid, DocCallback callback) {
        _authorizedRequest("GET", "/USERS/" + uid, null, new _RawCallback() {
            @Override
            public void onSuccess(String body) {
                try {
                    JSONObject json = new JSONObject(body);
                    callback.onSuccess(_decodeDocument(json));
                } catch (Exception e) {
                    callback.onError("Failed to parse profile document: " + e.getMessage());
                }
            }
            @Override public void onError(String error) { callback.onError(error); }
        });
    }

    // ── Firestore typed-value encoding ────────────────────────────────

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

    @SuppressWarnings("unchecked")
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
        } else if (value instanceof List) {
            JSONArray valuesArray = new JSONArray();
            for (Object item : (List<Object>) value) {
                valuesArray.put(_encodeValue(item));
            }
            JSONObject arrayWrapper = new JSONObject();
            arrayWrapper.put("values", valuesArray);
            wrapper.put("arrayValue", arrayWrapper);
        } else if (value instanceof Map) {
            JSONObject mapFields = new JSONObject();
            for (Map.Entry<String, Object> entry : ((Map<String, Object>) value).entrySet()) {
                mapFields.put(entry.getKey(), _encodeValue(entry.getValue()));
            }
            JSONObject mapWrapper = new JSONObject();
            mapWrapper.put("fields", mapFields);
            wrapper.put("mapValue", mapWrapper);
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
        if (wrapper.has("nullValue")) return null;
        if (wrapper.has("arrayValue")) {
            List<Object> list = new ArrayList<>();
            JSONObject arrayValue = wrapper.getJSONObject("arrayValue");
            if (arrayValue.has("values")) {
                JSONArray values = arrayValue.getJSONArray("values");
                for (int i = 0; i < values.length(); i++) {
                    list.add(_decodeValue(values.getJSONObject(i)));
                }
            }
            return list;
        }
        if (wrapper.has("mapValue")) {
            Map<String, Object> map = new HashMap<>();
            JSONObject mapValue = wrapper.getJSONObject("mapValue");
            if (mapValue.has("fields")) {
                JSONObject mapFields = mapValue.getJSONObject("fields");
                Iterator<String> keys = mapFields.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    map.put(key, _decodeValue(mapFields.getJSONObject(key)));
                }
            }
            return map;
        }
        return null;
    }

    // ── HTTP + auth plumbing ──────────────────────────────────────────

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

    // ── Offline replay ──────────────────────────────────────────────────

    /** Reference-implementation handler for OfflineActionQueue type
     *  "PENDING_GROUP_MARK" — replays the pendingGroupIds write from
     *  GroupJoinRequestActivity when it failed offline. The join request
     *  document itself already exists by the time this is queued (it's
     *  written first, synchronously, before this best-effort marker), so
     *  replaying this only affects the Discover page's pending-button
     *  state, never a double request. */
    public static void replayPendingGroupMarkFromQueue(android.content.Context context,
                                                       Map<String, Object> payload,
                                                       com.ajoo.app.offline.OfflineActionQueue.FlushCallback callback) {
        Object uidObj = payload.get("uid");
        Object groupIdObj = payload.get("groupId");
        String uid = uidObj != null ? String.valueOf(uidObj) : "";
        String groupId = groupIdObj != null ? String.valueOf(groupIdObj) : "";

        if (groupId.isEmpty() || uid.isEmpty()) {
            if (callback != null) callback.onError("Missing groupId or uid in payload");
            return;
        }

        getInstance(context).addPendingGroupToProfile(uid, groupId, new SimpleCallback() {
            @Override public void onSuccess() { 
                if (callback != null) callback.onSuccess(); 
            }
            @Override public void onError(String error) { 
                if (callback != null) callback.onError(error); 
            }
        });
    }
}