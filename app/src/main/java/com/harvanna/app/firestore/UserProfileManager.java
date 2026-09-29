package com.harvanna.app.firestore;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.SetOptions;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * User profile data - USERS/{uid} - via the native Firestore SDK (migrated
 * from hand-built REST/OkHttp calls). The SDK attaches the signed-in user's
 * ID token itself and provides offline persistence (see SketchApplication,
 * where persistence is enabled explicitly).
 *
 * The public API and the error-string contract are unchanged so existing
 * callers keep working: a missing document is reported as "HTTP 404: ..."
 * and a connectivity failure as "Network error: ..." (this is what
 * NetworkStatus.isConnectivityError() and the "404 means new user" checks
 * in the callers key on).
 *
 * Offline write behaviour: the SDK queues a write locally and completes its
 * Task only once the server acknowledges it. To keep the old "fail fast when
 * offline" callback contract, a write not acknowledged within WRITE_TIMEOUT_MS
 * is reported as a "Network error: ..." - the write itself stays queued in
 * the SDK and syncs on reconnect. Every write here is idempotent (set /
 * field-masked merge / array dedupe), so a later retry or OfflineActionQueue
 * replay is harmless.
 */
public class UserProfileManager {

    private static final String TAG = "UserProfileManager";
    private static final String COLLECTION = "USERS";
    private static final long WRITE_TIMEOUT_MS = 15_000L;

    private static UserProfileManager instance;
    private final Handler handler = new Handler(Looper.getMainLooper());

    public static synchronized UserProfileManager getInstance(Context context) {
        if (instance == null) {
            instance = new UserProfileManager();
        }
        return instance;
    }

    private UserProfileManager() {}

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
        DocumentReference ref = _docOrFail(uid, callback);
        if (ref == null) return;
        try {
            _awaitWrite(ref.set(fields), callback);
        } catch (Exception e) {
            callback.onError("Invalid profile data: " + e.getMessage());
        }
    }

    /** Masked partial update - only touches the fields provided, same
     *  distinction GroupFirestoreManager draws between addMember() (full
     *  replace) and updateMemberFields() (masked) - the same class of bug
     *  (silently wiping unrelated fields) applies here just as much. */
    public void updateProfileFields(String uid, Map<String, Object> fields, SimpleCallback callback) {
        DocumentReference ref = _docOrFail(uid, callback);
        if (ref == null) return;
        try {
            // mergeFields replaces each listed top-level field wholesale (same as the old
            // REST updateMask) and, like the old PATCH, creates the doc if it doesn't exist.
            _awaitWrite(ref.set(fields, SetOptions.mergeFields(new ArrayList<>(fields.keySet()))), callback);
        } catch (Exception e) {
            callback.onError("Invalid profile data: " + e.getMessage());
        }
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
        DocumentReference ref = _docOrFail(uid, new SimpleCallback() {
            @Override public void onSuccess() { }
            @Override public void onError(String error) { callback.onError(error); }
        });
        if (ref == null) return;
        // Default source: server when reachable, local cache when offline.
        ref.get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                callback.onError(_describe(task.getException()));
                return;
            }
            DocumentSnapshot snap = task.getResult();
            if (snap == null || !snap.exists()) {
                callback.onError("HTTP 404: document USERS/" + uid + " not found");
                return;
            }
            Map<String, Object> data = snap.getData();
            callback.onSuccess(data != null ? data : new HashMap<String, Object>());
        });
    }

    // ── SDK plumbing ──────────────────────────────────────────────────

    private DocumentReference _docOrFail(String uid, SimpleCallback callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            callback.onError("Not signed in — no current Firebase user.");
            return null;
        }
        if (uid == null || uid.isEmpty()) {
            callback.onError("Missing uid.");
            return null;
        }
        return FirebaseFirestore.getInstance().collection(COLLECTION).document(uid);
    }

    /** Reports exactly one result: the SDK's ack, or a network error after WRITE_TIMEOUT_MS. */
    private void _awaitWrite(Task<Void> task, SimpleCallback callback) {
        final AtomicBoolean done = new AtomicBoolean(false);
        final Runnable timeout = () -> {
            if (done.compareAndSet(false, true)) {
                callback.onError("Network error: write not acknowledged yet (queued; will sync when back online)");
            }
        };
        handler.postDelayed(timeout, WRITE_TIMEOUT_MS);
        task.addOnCompleteListener(t -> {
            if (!done.compareAndSet(false, true)) return;
            handler.removeCallbacks(timeout);
            if (t.isSuccessful()) callback.onSuccess();
            else callback.onError(_describe(t.getException()));
        });
    }

    /** Maps SDK failures onto the app's existing "HTTP <code>: " / "Network error: " strings. */
    private static String _describe(Exception e) {
        if (e instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException fe = (FirebaseFirestoreException) e;
            String msg = fe.getMessage() != null ? fe.getMessage() : fe.getCode().name();
            switch (fe.getCode()) {
                case NOT_FOUND:           return "HTTP 404: " + msg;
                case PERMISSION_DENIED:   return "HTTP 403: " + msg;
                case UNAUTHENTICATED:     return "HTTP 401: " + msg;
                case UNAVAILABLE:
                case DEADLINE_EXCEEDED:
                case CANCELLED:           return "Network error: " + msg;
                case INVALID_ARGUMENT:
                case FAILED_PRECONDITION:
                case ALREADY_EXISTS:      return "HTTP 400: " + msg;
                default:                  return "HTTP 500: " + msg;
            }
        }
        return "Network error: " + (e != null && e.getMessage() != null ? e.getMessage() : "unknown error");
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
                                                       com.harvanna.app.offline.OfflineActionQueue.FlushCallback callback) {
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