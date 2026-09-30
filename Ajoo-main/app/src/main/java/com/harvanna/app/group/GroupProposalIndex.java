package com.harvanna.app.group;

import android.content.Context;

import com.harvanna.app.firestore.GroupFirestoreManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The node stores each vote / withdrawal request but has no "list open proposals" call, so other members
 * cannot discover them. This index is only a pointer (kind + id) kept in the group's notices; the node record
 * fetched with that id is always the truth. Missing or forged entries can only hide or add a pointer to a
 * record that the node itself then confirms or denies.
 */
public final class GroupProposalIndex {
    public static final String DISBURSEMENT = "DISBURSEMENT", MILESTONE = "MILESTONE", GRANT = "GRANT", PACK = "PACK";

    public static final class Entry {
        public final String kind, id, extra;
        Entry(String kind, String id, String extra) { this.kind = kind; this.id = id; this.extra = extra; }
    }

    public interface ListCb { void onList(List<Entry> entries); }

    private GroupProposalIndex() {}

    public static void post(Context c, String firestoreGroupId, String kind, String id, String title, String extra) {
        if (firestoreGroupId == null || firestoreGroupId.isEmpty() || id == null || id.isEmpty()) return;
        Map<String, Object> f = new HashMap<>();
        long now = System.currentTimeMillis();
        f.put("type", "SYSTEM");
        f.put("system", true);
        f.put("title", title);
        f.put("message", title);
        f.put("sender", "System");
        f.put("creatorId", "");
        f.put("priority", "NORMAL");
        f.put("createdAt", now);
        f.put("timestamp", String.valueOf(now));
        f.put("proposalKind", kind);
        f.put("proposalId", id);
        f.put("proposalExtra", extra == null ? "" : extra);
        GroupFirestoreManager.getInstance(c).addNotice(firestoreGroupId, f, new GroupFirestoreManager.IdCallback() {
            @Override public void onSuccess(String docId) { }
            @Override public void onError(String error) { }
        });
    }

    public static void list(Context c, String firestoreGroupId, ListCb cb) {
        if (firestoreGroupId == null || firestoreGroupId.isEmpty()) { cb.onList(new ArrayList<>()); return; }
        GroupFirestoreManager.getInstance(c).getNotices(firestoreGroupId, new GroupFirestoreManager.ListCallback() {
            @Override public void onSuccess(List<Map<String, Object>> items) {
                Map<String, Entry> byId = new LinkedHashMap<>();
                for (Map<String, Object> n : items) {
                    Object k = n.get("proposalKind"), id = n.get("proposalId");
                    if (k == null || id == null || String.valueOf(id).isEmpty()) continue;
                    byId.put(kindId(k, id), new Entry(String.valueOf(k), String.valueOf(id),
                        n.get("proposalExtra") == null ? "" : String.valueOf(n.get("proposalExtra"))));
                }
                cb.onList(new ArrayList<>(byId.values()));
            }
            @Override public void onError(String error) { cb.onList(new ArrayList<>()); }
        });
    }

    private static String kindId(Object k, Object id) { return k + ":" + id; }
}
