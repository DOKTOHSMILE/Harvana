package com.harvana.app.group;

import android.content.Context;
import android.util.Log;

import com.harvana.app.firestore.GroupFirestoreManager;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Group notices — aligned with Group Engine Data Contract.
 *
 * Collection: GROUPS/{groupId}/notices/{noticeId}
 *
 * Schema (all writers should use these keys):
 *   type        SYSTEM | ANNOUNCEMENT
 *   title       short headline
 *   message     body
 *   sender      "System" | "Admin" | display label
 *   creatorId   uid for announcements & system notices (used for dynamic name resolution)
 *   priority    NORMAL | HIGH
 *   createdAt   long epoch millis (canonical time)
 *   timestamp   string of createdAt (legacy readers)
 *   system      boolean true for auto activity notices
 */
public final class GroupNoticeHelper {

    private static final String TAG = "GroupNoticeHelper";

    public static final String TYPE_SYSTEM = "SYSTEM";
    public static final String TYPE_ANNOUNCEMENT = "ANNOUNCEMENT";
    public static final String PRIORITY_NORMAL = "NORMAL";
    public static final String PRIORITY_HIGH = "HIGH";
    /** Who a notice is for: every member (except whoever caused it), or one member. */
    public static final String AUDIENCE_ALL = "ALL", AUDIENCE_TARGET = "TARGET";

    public interface Callback {
        void onSuccess(String noticeId);
        void onError(String error);
    }

    public interface ListCallback {
        void onSuccess(List<Map<String, Object>> notices);
        void onError(String error);
    }

    private GroupNoticeHelper() {}

    // ── Builders ─────────────────────────────────────────────────────────

    public static Map<String, Object> buildSystem(String title, String message) {
        String currentUid = "";
        try {
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            }
        } catch (Exception ignored) {}
        
        return buildSystem(title, message, currentUid);
    }

    /**
     * A notice about something that happened, written by the person who caused it ("a member joined", "a loan was applied for").
     * Every other member is told; the person who caused it is not. (Not to be confused with the reminder below.)
     */
    public static Map<String, Object> buildSystem(String title, String message, String actorUid) {
        Map<String, Object> fields = base(title, message);
        fields.put("creatorId", actorUid != null ? actorUid : "");     // kept for older readers that resolve the name from it
        fields.put("actorUid", actorUid != null ? actorUid : "");
        fields.put("audience", AUDIENCE_ALL);
        return fields;
    }

    /** A notice only one member should be told about (a payment reminder to the member who still owes). */
    public static Map<String, Object> buildTargeted(String title, String message, String targetUid, String actorUid, String category) {
        Map<String, Object> fields = base(title, message);
        fields.put("creatorId", targetUid != null ? targetUid : "");
        fields.put("targetUid", targetUid != null ? targetUid : "");
        fields.put("actorUid", actorUid != null ? actorUid : "");
        fields.put("audience", AUDIENCE_TARGET);
        fields.put("category", category == null ? "GROUP" : category);
        return fields;
    }

    /** Something every member can act on: a vote is waiting. {@code page} is where the member goes (see NotificationRouter). */
    public static Map<String, Object> buildVote(String title, String message, String actorUid, int page) {
        Map<String, Object> fields = buildSystem(title, message, "");
        fields.put("actorUid", actorUid != null ? actorUid : "");
        fields.put("needsVote", true);
        fields.put("page", page);
        return fields;
    }

    private static Map<String, Object> base(String title, String message) {
        long now = System.currentTimeMillis();
        Map<String, Object> fields = new HashMap<>();
        fields.put("type", TYPE_SYSTEM);
        fields.put("title", title != null ? title : "Update");
        fields.put("message", message != null ? message : "");
        fields.put("sender", "System");
        fields.put("priority", PRIORITY_NORMAL);
        fields.put("createdAt", now);
        fields.put("timestamp", String.valueOf(now));
        fields.put("system", true);
        return fields;
    }

    public static Map<String, Object> buildAnnouncement(String title, String message,
                                                        String creatorUid, boolean highPriority) {
        long now = System.currentTimeMillis();
        Map<String, Object> fields = new HashMap<>();
        fields.put("type", TYPE_ANNOUNCEMENT);
        fields.put("title", title != null ? title.trim() : "");
        fields.put("message", message != null ? message.trim() : "");
        fields.put("sender", "Admin");
        fields.put("creatorId", creatorUid != null ? creatorUid : "");
        fields.put("priority", highPriority ? PRIORITY_HIGH : PRIORITY_NORMAL);
        fields.put("createdAt", now);
        fields.put("timestamp", String.valueOf(now));
        fields.put("system", false);
        return fields;
    }

    // ── Write ────────────────────────────────────────────────────────────

    public static void postSystem(Context context, String groupId, String title, String message) {
        if (context == null || GroupValidation.isBlank(groupId)) return;
        GroupFirestoreManager.getInstance(context)
                .addNotice(groupId, buildSystem(title, message), new GroupFirestoreManager.IdCallback() {
                    @Override
                    public void onSuccess(String id) { /* best-effort */ }

                    @Override
                    public void onError(String error) {
                        Log.w(TAG, "system notice failed: " + error);
                    }
                });
    }

    public static void postAnnouncement(Context context, String groupId,
                                        String title, String message, Callback callback) {
        if (callback == null) return;
        String err = GroupValidation.firstError(
                GroupValidation.requireGroupId(groupId),
                context == null ? "Missing context." : null
        );
        if (err != null) {
            callback.onError(err);
            return;
        }
        String t = title != null ? title.trim() : "";
        String m = message != null ? message.trim() : "";
        if (t.isEmpty() && m.isEmpty()) {
            callback.onError("Write a title or message.");
            return;
        }
        if (t.isEmpty()) t = "Announcement";

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        GroupFirestoreManager.getInstance(context)
                .addNotice(groupId, buildAnnouncement(t, m, uid, false),
                        new GroupFirestoreManager.IdCallback() {
                            @Override
                            public void onSuccess(String id) {
                                callback.onSuccess(id != null ? id : "ok");
                            }

                            @Override
                            public void onError(String error) {
                                callback.onError(error);
                            }
                        });
    }

    // ── Read / sort ──────────────────────────────────────────────────────

    public static void listNotices(Context context, String groupId, ListCallback callback) {
        if (callback == null) return;
        if (context == null || GroupValidation.isBlank(groupId)) {
            callback.onError("Missing group id.");
            return;
        }
        GroupFirestoreManager.getInstance(context)
                .getNotices(groupId, new GroupFirestoreManager.ListCallback() {
                    @Override
                    public void onSuccess(List<Map<String, Object>> items) {
                        List<Map<String, Object>> list = items != null
                                ? new ArrayList<>(items) : new ArrayList<>();
                        sortNewestFirst(list);
                        callback.onSuccess(unique(list));
                    }

                    @Override
                    public void onError(String error) {
                        callback.onError(error);
                    }
                });
    }

    /** Drops a second copy of the same notice. A payment used to write two, and a proposal stored its title as the message. */
    public static List<Map<String, Object>> unique(List<Map<String, Object>> notices) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (notices == null) return out;
        for (Map<String, Object> n : notices) {
            if (n == null || isDuplicate(out, n)) continue;
            out.add(n);
        }
        return out;
    }

    private static boolean isDuplicate(List<Map<String, Object>> kept, Map<String, Object> n) {
        String title = norm(text(n, "title"));
        String message = norm(text(n, "message").isEmpty() ? text(n, "body") : text(n, "message"));
        String id = text(n, "_docId");
        String creator = text(n, "creatorId");
        long at = createdAtMillis(n);
        for (Map<String, Object> k : kept) {
            if (!id.isEmpty() && id.equals(text(k, "_docId"))) return true;
            String kt = norm(text(k, "title"));
            if (!kt.equals(title) || title.isEmpty()) continue;
            String km = norm(text(k, "message").isEmpty() ? text(k, "body") : text(k, "message"));
            if (km.equals(message)) return true;
            String kc = text(k, "creatorId");
            long dt = Math.abs(createdAtMillis(k) - at);
            if (dt < 120_000L && !creator.isEmpty() && creator.equals(kc)
                    && "contribution received".equals(title)) {
                return true;
            }
        }
        return false;
    }

    private static String text(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v == null ? "" : String.valueOf(v).trim();
    }

    private static String norm(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.US).replaceAll("\\s+", " ");
    }

    public static void sortNewestFirst(List<Map<String, Object>> notices) {
        if (notices == null || notices.size() < 2) return;
        Collections.sort(notices, new Comparator<Map<String, Object>>() {
            @Override
            public int compare(Map<String, Object> a, Map<String, Object> b) {
                long ta = createdAtMillis(a);
                long tb = createdAtMillis(b);
                return Long.compare(tb, ta);
            }
        });
    }

    public static long createdAtMillis(Map<String, Object> notice) {
        if (notice == null) return 0L;
        Object c = notice.get("createdAt");
        if (c instanceof Number) return ((Number) c).longValue();
        if (c != null) {
            try {
                return Long.parseLong(String.valueOf(c));
            } catch (Exception ignored) {}
        }
        Object t = notice.get("timestamp");
        if (t instanceof Number) return ((Number) t).longValue();
        if (t != null) {
            try {
                return Long.parseLong(String.valueOf(t));
            } catch (Exception ignored) {}
        }
        return 0L;
    }

    public static String formatDate(Map<String, Object> notice) {
        long ms = createdAtMillis(notice);
        if (ms <= 0) return "";
        try {
            return new SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.getDefault())
                    .format(new Date(ms));
        } catch (Exception e) {
            return String.valueOf(ms);
        }
    }

    public static boolean isSystem(Map<String, Object> notice) {
        if (notice == null) return false;
        Object sys = notice.get("system");
        if (sys instanceof Boolean) return (Boolean) sys;
        String type = notice.get("type") != null ? String.valueOf(notice.get("type")) : "";
        return TYPE_SYSTEM.equalsIgnoreCase(type)
                || "System".equalsIgnoreCase(String.valueOf(notice.get("sender")));
    }

    public static String displayAuthor(Map<String, Object> notice) {
        if (isSystem(notice)) return "System";
        Object s = notice != null ? notice.get("sender") : null;
        return s != null && !String.valueOf(s).isEmpty() ? String.valueOf(s) : "Admin";
    }

    // ── Offline replay ──────────────────────────────────────────────────

    public static void replayNoticeFromQueue(Context context, Map<String, Object> payload,
                                             com.harvana.app.offline.OfflineActionQueue.FlushCallback callback) {
        Object groupIdObj = payload.get("groupId");
        Object titleObj = payload.get("title");
        Object messageObj = payload.get("message");
        String groupId = groupIdObj != null ? String.valueOf(groupIdObj) : "";
        String title = titleObj != null ? String.valueOf(titleObj) : "";
        String message = messageObj != null ? String.valueOf(messageObj) : "";

        postAnnouncement(context, groupId, title, message, new Callback() {
            @Override public void onSuccess(String noticeId) { 
                if (callback != null) callback.onSuccess(); 
            }
            @Override public void onError(String error) { 
                if (callback != null) callback.onError(error); 
            }
        });
    }
}