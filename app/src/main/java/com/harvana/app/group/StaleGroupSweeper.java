package com.harvana.app.group;

import android.content.Context;
import android.content.SharedPreferences;

import com.harvana.app.firestore.GroupFirestoreManager;
import com.harvana.app.firestore.UserProfileManager;
import com.harvana.app.offline.OfflineCacheManager;
import com.harvana.app.sync.HarvanaNodeClient;
import com.harvana.app.sync.NodeGroupClient;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Clears groups the network no longer has. A group that lives on the network keeps a profile in Firestore; if the node's data is reset
 * (a new chain, a wiped server) the profile is left behind and the app keeps listing a group that cannot take a payment, a vote or a payout.
 * This asks the node itself, group by group, and removes the dead ones from the member's account.
 *
 * It is careful, because removing a real group by mistake is much worse than leaving a dead one a little longer:
 *   - it only runs when the node answers a health check with a chain height, so a down or restarting node never counts;
 *   - only a clear "not found" counts as missing, never a timeout or an error;
 *   - a group must be missing on two separate checks at least ten minutes apart;
 *   - groups created in the last hour are never touched (the profile is saved right after the network accepts the group);
 *   - one pass at most every 30 minutes.
 * What is cleared: the group is removed from the member's list and local copies; if the member is its admin, the group's profile in
 * Firestore is deleted too, since nothing is left to manage. Other members' lists clear when they open the app.
 */
public final class StaleGroupSweeper {
    private StaleGroupSweeper() {}

    public interface Done { void cleared(List<String> groupIds); }

    private static final String PREFS = "stale_groups";
    private static final long MIN_PASS_GAP_MS = 30L * 60_000L, MIN_STRIKE_GAP_MS = 10L * 60_000L, MIN_AGE_MS = 60L * 60_000L;
    private static boolean running;

    public static void sweep(Context context, String uid, List<HashMap<String, Object>> groups, Done done) {
        if (context == null || uid == null || uid.isEmpty() || groups == null || groups.isEmpty()) return;
        final Context app = context.getApplicationContext();
        final SharedPreferences prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        final long now = System.currentTimeMillis();
        synchronized (StaleGroupSweeper.class) {
            if (running || now - prefs.getLong("lastPass", 0) < MIN_PASS_GAP_MS) return;
            running = true;
        }
        final List<HashMap<String, Object>> candidates = new ArrayList<>();
        for (HashMap<String, Object> g : groups) {
            String nodeId = nodeIdOf(g);
            if (nodeId.isEmpty() || GroupKinds.familyOf(nodeId).isEmpty()) continue;
            if (now - createdAt(g) < MIN_AGE_MS) continue;
            candidates.add(g);
        }
        if (candidates.isEmpty()) { finish(prefs, now); return; }

        // The node must be up and reporting a chain before anything it says about a missing group means anything.
        HarvanaNodeClient.nodeGet("/api/v1/chain/info", new HarvanaNodeClient.JsonCallback() {
            @Override public void onSuccess(JSONObject json) {
                if (json == null || json.optLong("blockHeight", json.optLong("height", -1)) < 0) { finish(prefs, now); return; }
                check(app, prefs, uid, candidates, 0, new ArrayList<>(), done, now);
            }
            @Override public void onError(String error) { finish(prefs, now); }
        });
    }

    private static void check(Context app, SharedPreferences prefs, String uid, List<HashMap<String, Object>> list, int i,
                              List<String> cleared, Done done, long now) {
        if (i >= list.size()) {
            finish(prefs, now);
            if (!cleared.isEmpty() && done != null) done.cleared(cleared);
            return;
        }
        final HashMap<String, Object> g = list.get(i);
        final String nodeId = nodeIdOf(g);
        final String gid = str(g.get("groupId"));
        NodeGroupClient.existsOnNode(GroupKinds.familyOf(nodeId), nodeId, exists -> {
            String key = "strike_" + nodeId;
            if (Boolean.TRUE.equals(exists)) {
                prefs.edit().remove(key).apply();
            } else if (Boolean.FALSE.equals(exists)) {
                long first = prefs.getLong(key, 0);
                if (first == 0) prefs.edit().putLong(key, now).apply();                        // first time missing: note it, wait
                else if (now - first >= MIN_STRIKE_GAP_MS) {                                  // missing again, long enough after
                    clear(app, uid, gid, g);
                    cleared.add(gid);
                    prefs.edit().remove(key).apply();
                }
            }                                                                                 // null: node unreachable, say nothing
            check(app, prefs, uid, list, i + 1, cleared, done, now);
        });
    }

    private static void clear(Context app, String uid, String gid, Map<String, Object> g) {
        UserProfileManager.getInstance(app).removeGroupFromProfile(uid, gid, new UserProfileManager.SimpleCallback() {
            @Override public void onSuccess() { }
            @Override public void onError(String error) { }
        });
        OfflineCacheManager.remove(app, "group_" + gid);
        if (uid.equals(str(g.get("creatorId")))) {
            // Its picture is deleted first (the node checks the admin from the group profile), then the profile itself.
            com.harvana.app.utils.MediaCleaner.delete(app, com.harvana.app.utils.MediaCleaner.GROUP_PHOTO, gid, () ->
                GroupFirestoreManager.getInstance(app).deleteGroupTree(gid, new GroupFirestoreManager.SimpleCallback() {
                    @Override public void onSuccess() { }
                    @Override public void onError(String error) { }
                }));
        }
    }

    private static void finish(SharedPreferences prefs, long now) {
        prefs.edit().putLong("lastPass", now).apply();
        synchronized (StaleGroupSweeper.class) { running = false; }
    }

    private static String nodeIdOf(Map<String, Object> g) {
        String id = str(g.get("nodeGroupId"));
        if (id.isEmpty()) id = str(g.get("circleId"));
        return id;
    }

    private static long createdAt(Map<String, Object> g) {
        try { return Long.parseLong(str(g.get("timestamp"))); } catch (Exception e) { return 0L; }
    }

    private static String str(Object o) { return o == null ? "" : String.valueOf(o).trim(); }
}
