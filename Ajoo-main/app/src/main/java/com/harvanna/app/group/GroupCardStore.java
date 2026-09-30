package com.harvanna.app.group;

import android.content.Context;
import android.content.SharedPreferences;

import com.harvanna.app.firestore.GroupFirestoreManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Publishes and fetches a group's encrypted wallet card. Only ciphertext is
 * stored (the card payload is sealed with the admin's passphrase, which is
 * never uploaded), so members can back the card up without being able to
 * spend from the wallet. A failed publish is kept locally and retried.
 */
public final class GroupCardStore {
    public static final String DOC_ID = "walletCard";
    private static final String PREFS = "group_card_pending";

    public interface Callback {
        void onSuccess(Map<String, Object> card);
        void onError(String error);
    }

    public interface PublishCallback {
        void onDone(boolean published);
    }

    private GroupCardStore() {}

    public static void publish(Context ctx, String groupId, String walletAddress, String label,
                               String template, String qrPayload, String digitCode,
                               String createdByUid, PublishCallback cb) {
        Map<String, Object> f = new HashMap<>();
        f.put("walletAddress", walletAddress);
        f.put("label", label == null ? "" : label);
        f.put("template", template);
        f.put("qrPayload", qrPayload);
        f.put("digitCode", digitCode);
        f.put("createdBy", createdByUid == null ? "" : createdByUid);
        f.put("createdAt", System.currentTimeMillis());
        f.put("version", 1L);
        final Context app = ctx.getApplicationContext();
        GroupFirestoreManager.getInstance(app).setPrivateDoc(groupId, DOC_ID, f,
            new GroupFirestoreManager.SimpleCallback() {
                @Override public void onSuccess() {
                    clearPending(app, groupId);
                    if (cb != null) cb.onDone(true);
                }
                @Override public void onError(String error) {
                    savePending(app, groupId, f);
                    if (cb != null) cb.onDone(false);
                }
            });
    }

    /** Retries a previously failed publish for this group, if any. */
    public static void retryPending(Context ctx, String groupId) {
        final Context app = ctx.getApplicationContext();
        SharedPreferences sp = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String raw = sp.getString(groupId, null);
        if (raw == null) return;
        try {
            org.json.JSONObject o = new org.json.JSONObject(raw);
            Map<String, Object> f = new HashMap<>();
            java.util.Iterator<String> it = o.keys();
            while (it.hasNext()) { String k = it.next(); f.put(k, o.get(k)); }
            if (f.get("createdAt") instanceof Integer) f.put("createdAt", ((Integer) f.get("createdAt")).longValue());
            GroupFirestoreManager.getInstance(app).setPrivateDoc(groupId, DOC_ID, f,
                new GroupFirestoreManager.SimpleCallback() {
                    @Override public void onSuccess() { clearPending(app, groupId); }
                    @Override public void onError(String error) { }
                });
        } catch (Exception ignored) { }
    }

    public static void fetch(Context ctx, String groupId, Callback cb) {
        GroupFirestoreManager.getInstance(ctx.getApplicationContext()).getPrivateDoc(groupId, DOC_ID,
            new GroupFirestoreManager.DocCallback() {
                @Override public void onSuccess(Map<String, Object> data) { cb.onSuccess(data); }
                @Override public void onError(String error) { cb.onError(error); }
            });
    }

    private static void savePending(Context app, String groupId, Map<String, Object> f) {
        try {
            app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(groupId, new org.json.JSONObject(f).toString()).apply();
        } catch (Exception ignored) { }
    }

    private static void clearPending(Context app, String groupId) {
        app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(groupId).apply();
    }
}
