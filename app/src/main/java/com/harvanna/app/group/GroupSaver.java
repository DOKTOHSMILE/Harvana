package com.harvanna.app.group;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.auth.FirebaseAuth;
import com.harvanna.app.firestore.GroupFirestoreManager;
import com.harvanna.app.firestore.UserProfileManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Saves a new group's record in Firestore (group document, creator as ADMIN member, launch notice, profile link).
 * For a group that already exists on the node, a failed save is queued on the phone and retried automatically, so the
 * group is never left on the node without a Firestore record. Every step is idempotent (same ids, PATCH writes).
 */
public final class GroupSaver {
    private static final String PREFS = "pending_group_saves";
    private static boolean retrying;

    public interface Done { void onDone(String errorOrNull); }

    private GroupSaver() {}

    public static void save(Context ctx, String uid, String groupId, Map<String, Object> map, Done done) {
        final Context c = ctx.getApplicationContext();
        final GroupFirestoreManager fm = GroupFirestoreManager.getInstance(c);
        fm.createGroup(groupId, map, new GroupFirestoreManager.SimpleCallback() {
            @Override public void onSuccess() {
                Map<String, Object> member = new HashMap<>();
                member.put("memberUid", uid);
                member.put("slot", 1);
                member.put("role", "ADMIN");
                member.put("status", "ACTIVE");
                member.put("joinedAt", System.currentTimeMillis());
                fm.addMember(groupId, uid, member, new GroupFirestoreManager.SimpleCallback() {
                    @Override public void onSuccess() {
                        fm.postSystemNotice(groupId, "Group launched", "This group is now active. Welcome!");
                        UserProfileManager.getInstance(c).addGroupToProfile(uid, groupId, new UserProfileManager.SimpleCallback() {
                            @Override public void onSuccess() { done.onDone(null); }
                            @Override public void onError(String e) { done.onDone(null); } // profile link is best-effort
                        });
                    }
                    @Override public void onError(String e) { done.onDone("Adding you as a member failed: " + e); }
                });
            }
            @Override public void onError(String e) { done.onDone("Saving the group failed: " + e); }
        });
    }

    // ── queue ─────────────────────────────────────────────────────────────

    public static void queue(Context ctx, String uid, String groupId, Map<String, Object> map) {
        try {
            JSONObject o = new JSONObject();
            o.put("uid", uid);
            o.put("map", new JSONObject(map));
            sp(ctx).edit().putString(groupId, o.toString()).apply();
        } catch (Exception ignored) { }
    }

    public static void unqueue(Context ctx, String groupId) { sp(ctx).edit().remove(groupId).apply(); }

    public static int pendingCount(Context ctx) { return sp(ctx).getAll().size(); }

    /** Retries every queued save that belongs to the signed-in user. Call on app start / resume. */
    public static synchronized void retryAll(Context ctx, Done anyDone) {
        final Context c = ctx.getApplicationContext();
        if (retrying || FirebaseAuth.getInstance().getCurrentUser() == null) return;
        final String me = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Map<String, ?> all = sp(c).getAll();
        if (all.isEmpty()) return;
        retrying = true;
        final int[] left = {0};
        for (Map.Entry<String, ?> e : all.entrySet()) {
            try {
                JSONObject o = new JSONObject(String.valueOf(e.getValue()));
                if (!me.equals(o.optString("uid"))) continue;
                final String gid = e.getKey();
                Map<String, Object> map = toMap(o.getJSONObject("map"));
                synchronized (left) { left[0]++; }
                save(c, me, gid, map, err -> {
                    if (err == null) { unqueue(c, gid); NodePortfolioReset.reset(); }
                    boolean last;
                    synchronized (left) { left[0]--; last = left[0] == 0; }
                    if (last) { synchronized (GroupSaver.class) { retrying = false; } }
                    if (anyDone != null) anyDone.onDone(err);
                });
            } catch (Exception ignored) { }
        }
        synchronized (left) { if (left[0] == 0) retrying = false; }
    }

    private static Map<String, Object> toMap(JSONObject o) throws Exception {
        Map<String, Object> m = new HashMap<>();
        Iterator<String> it = o.keys();
        while (it.hasNext()) {
            String k = it.next();
            Object v = o.get(k);
            if (v instanceof JSONArray || v instanceof JSONObject) continue;
            if (v instanceof Integer) v = ((Integer) v).longValue();
            m.put(k, v);
        }
        return m;
    }

    private static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Small indirection so this class doesn't depend on the portfolio cache type directly. */
    private static final class NodePortfolioReset {
        static void reset() { com.harvanna.app.sync.NodePortfolio.invalidate(); }
    }
}
