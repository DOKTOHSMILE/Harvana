package com.harvana.app.trust;

import com.google.firebase.firestore.FirebaseFirestore;
import com.harvana.app.sync.NodeGroupClient;
import com.harvana.app.sync.NodeIdentityClient;

import org.json.JSONObject;

import java.util.Map;

/** The trust number the node already saved. This class does not calculate one. */
public final class NetworkTrust {
    public static final int MAX = 26;

    private NetworkTrust() {}

    public static final class Score {
        public int score;
        /** The node's trust percentage (0 to 100), or -1 when it has not published one. */
        public int percent = -1;
        public String tier = "";
        public int verifiedReads, activeDays30, giftsSent;
        public boolean published;
        public boolean bound;
        public int vouches;
        public int badges;
        public boolean established;
        public String summary = "";
    }

    public interface Callback { void onScore(Score score); }

    /** Chain id for a group row, or "" when the row exists only in Firestore. */
    public static String groupKey(Map<String, Object> g) {
        if (g == null) return "";
        String id = com.harvana.app.group.GroupKinds.nodeId(g);
        if (!NodeGroupClient.familyOf(id).isEmpty()) return id;
        for (String key : new String[]{"groupId", "id", "_docId"}) {
            Object v = g.get(key);
            if (v == null) continue;
            String s = String.valueOf(v).trim();
            if (!NodeGroupClient.familyOf(s).isEmpty()) return s;
        }
        return "";
    }

    public static void wallet(String address, Callback cb) {
        if (address == null || !address.startsWith("HV0x")) {
            cb.onScore(new Score());
            return;
        }
        FirebaseFirestore.getInstance().collection("REPUTATION").document(address).get()
                .addOnSuccessListener(snap -> {
                    if (snap != null && snap.exists() && snap.getData() != null) {
                        cb.onScore(fromMap(snap.getData()));
                        return;
                    }
                    fromNode(address, cb);
                })
                .addOnFailureListener(e -> fromNode(address, cb));
    }

    public static void group(String groupId, Callback cb) {
        if (groupId == null || NodeGroupClient.familyOf(groupId).isEmpty()) {
            cb.onScore(new Score());
            return;
        }
        FirebaseFirestore.getInstance().collection("GROUP_TRUST").document(groupId).get()
                .addOnSuccessListener(snap -> {
                    if (snap != null && snap.exists() && snap.getData() != null) {
                        cb.onScore(fromGroup(snap.getData()));
                        return;
                    }
                    NodeGroupClient.getGroupTrust(groupId, new NodeGroupClient.Callback() {
                        @Override public void onSuccess(JSONObject json) { cb.onScore(fromGroupJson(json)); }
                        @Override public void onError(String error) { cb.onScore(new Score()); }
                    });
                })
                .addOnFailureListener(e -> NodeGroupClient.getGroupTrust(groupId, new NodeGroupClient.Callback() {
                    @Override public void onSuccess(JSONObject json) { cb.onScore(fromGroupJson(json)); }
                    @Override public void onError(String error) { cb.onScore(new Score()); }
                }));
    }

    private static void fromNode(String address, Callback cb) {
        NodeIdentityClient.lookup(address, new NodeIdentityClient.Callback() {
            @Override public void onSuccess(JSONObject j) { cb.onScore(fromPerson(j)); }
            @Override public void onError(String error) { cb.onScore(new Score()); }
        });
    }

    private static Score fromMap(Map<String, Object> doc) {
        Score s = new Score();
        s.published = true;
        s.score = num(doc.get("trustScore"));
        if (doc.get("trustPercent") instanceof Number) s.percent = num(doc.get("trustPercent"));
        s.tier = doc.get("trustTier") == null ? "" : String.valueOf(doc.get("trustTier"));
        s.verifiedReads = num(doc.get("verifiedReads"));
        s.activeDays30 = num(doc.get("activeDays30"));
        s.giftsSent = num(doc.get("giftsSent"));
        s.bound = Boolean.TRUE.equals(doc.get("bound"));
        s.vouches = num(doc.get("effectiveVouchCount"));
        s.established = Boolean.TRUE.equals(doc.get("established"));
        Object labels = doc.get("labels");
        s.badges = labels instanceof java.util.List ? ((java.util.List<?>) labels).size() : 0;
        Object summary = doc.get("summary");
        s.summary = summary == null ? "" : String.valueOf(summary);
        return s;
    }

    private static Score fromPerson(JSONObject j) {
        Score s = new Score();
        if (j == null || !j.has("trustScore")) return s;
        s.published = true;
        s.score = j.optInt("trustScore", 0);
        if (j.has("trustPercent")) s.percent = j.optInt("trustPercent", -1);
        s.tier = j.optString("trustTier", "");
        s.bound = j.optBoolean("bound", false);
        s.vouches = j.optInt("effectiveVouchCount", 0);
        s.established = j.optBoolean("established", false);
        s.summary = s.score + " / " + MAX;
        return s;
    }

    private static Score fromGroup(Map<String, Object> doc) {
        Score s = new Score();
        s.published = true;
        Object summary = doc.get("summary");
        Object tier = doc.get("tier");
        s.summary = summary != null ? String.valueOf(summary) : (tier == null ? "" : String.valueOf(tier));
        return s;
    }

    private static Score fromGroupJson(JSONObject j) {
        Score s = new Score();
        if (j == null) return s;
        s.published = true;
        String summary = j.optString("summary", "");
        s.summary = summary.isEmpty() ? j.optString("tier", "") : summary;
        return s;
    }

    private static int num(Object o) { return o instanceof Number ? ((Number) o).intValue() : 0; }
}
