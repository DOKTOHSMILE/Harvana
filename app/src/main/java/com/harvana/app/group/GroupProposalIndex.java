package com.harvana.app.group;

import android.content.Context;

import com.harvana.app.firestore.GroupFirestoreManager;

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
        // Every member is told a vote is waiting (the pointer is also how other members find the proposal).
        String actor = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() == null ? ""
            : com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
        Map<String, Object> f = GroupNoticeHelper.buildVote(title, "A vote is waiting. Open Actions & votes to vote.", actor, 99);
        f.put("proposalKind", kind);
        f.put("proposalId", id);
        f.put("proposalExtra", extra == null ? "" : extra);
        // One notice per proposal, whatever happens twice (a retry, two screens): the id is the proposal's own.
        String noticeId = ("proposal_" + kind + "_" + id).replaceAll("[^A-Za-z0-9_-]", "_");
        GroupFirestoreManager.getInstance(c).setNotice(firestoreGroupId, noticeId, f, new GroupFirestoreManager.SimpleCallback() {
            @Override public void onSuccess() { }
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
