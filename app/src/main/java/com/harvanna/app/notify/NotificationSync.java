package com.harvanna.app.notify;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.auth.FirebaseAuth;
import com.harvanna.app.firestore.GroupFirestoreManager;
import com.harvanna.app.firestore.UserProfileManager;
import com.harvanna.app.group.InviteManager;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Pulls what's new for the signed-in user (group notices, invitations, answers to join requests) and
 * feeds it into the NotificationCenter. There is no server push in this project, so this runs while the
 * app is open (every ~90 s) and in the background about every 15 minutes (WorkManager's minimum).
 */
public final class NotificationSync {
    private NotificationSync() {}

    public static void run(Context ctx, Runnable done) {
        final Context c = ctx.getApplicationContext();
        if (FirebaseAuth.getInstance().getCurrentUser() == null) { if (done != null) done.run(); return; }
        final String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
<<<<<<< HEAD
        final AtomicInteger pending = new AtomicInteger(5);
=======
        final AtomicInteger pending = new AtomicInteger(4);
>>>>>>> main
        final Runnable finish = () -> { if (pending.decrementAndGet() == 0 && done != null) done.run(); };

        // 1. Invitations
        InviteManager.listMine(c, new InviteManager.ListCb() {
            @Override public void onList(List<Map<String, Object>> invites) {
                for (Map<String, Object> inv : invites) {
                    String id = s(inv.get("_docId"));
                    NotificationCenter.post(c, NotificationCenter.INVITE, "invite_" + id,
                        "You're invited to " + s(inv.get("groupName")),
                        s(inv.get("inviterName")) + " invited you. Open to read the details before you decide.", "", -1);
                }
                finish.run();
            }
            @Override public void onError(String e) { finish.run(); }
        });

        // 2. Answers to my join requests
        GroupFirestoreManager.getInstance(c).queryMyJoinRequests(uid, new GroupFirestoreManager.ListCallback() {
            @Override public void onSuccess(List<Map<String, Object>> reqs) {
                for (Map<String, Object> r : reqs) {
                    String st = s(r.get("status")).toUpperCase();
                    String gid = s(r.get("groupId")), name = s(r.get("groupName"));
                    if (st.equals("APPROVED")) {
                        NotificationCenter.post(c, NotificationCenter.MEMBERSHIP, "join_" + gid + "_approved",
                            "You're in: " + name, "Your request to join was approved. Welcome to the group.", gid, 0);
                    } else if (st.equals("REJECTED") || st.equals("DECLINED")) {
                        NotificationCenter.post(c, NotificationCenter.MEMBERSHIP, "join_" + gid + "_rejected",
                            "Join request not accepted: " + name, "The group's admin didn't accept your request this time.", "", -1);
                    }
                }
                finish.run();
            }
            @Override public void onError(String e) { finish.run(); }
        });

<<<<<<< HEAD
        // 5. Wallet: money received, sent and payments signed (from the node's confirmed history)
        WalletTxNotifier.run(c, events -> finish.run());

=======
>>>>>>> main
        // 4. Review verdicts on my Learn Hub submissions
        GroupFirestoreManager.getInstance(c).getMyLearnSubmissions(uid, new GroupFirestoreManager.ListCallback() {
            @Override public void onSuccess(List<Map<String, Object>> subs) {
                for (Map<String, Object> sub : subs) {
                    String id = s(sub.get("_docId")), st = s(sub.get("status")).toUpperCase(), title = s(sub.get("title"));
                    if (st.equals("APPROVED")) {
                        NotificationCenter.post(c, NotificationCenter.SYSTEM, "learn_" + id + "_approved",
                            "Published: " + title, "Your Learn Hub article passed review and is now live.", "", -1);
                    } else if (st.equals("REJECTED")) {
                        String why = s(sub.get("reviewMessage"));
                        NotificationCenter.post(c, NotificationCenter.SYSTEM, "learn_" + id + "_rejected",
                            "Not published: " + title, why.isEmpty() ? "It didn't pass the automatic review." : why, "", -1);
                    }
                }
                finish.run();
            }
            @Override public void onError(String e) { finish.run(); }
        });

        // 3. New notices in my groups
        UserProfileManager.getInstance(c).getProfile(uid, new UserProfileManager.DocCallback() {
            @Override public void onSuccess(Map<String, Object> profile) {
                Object g = profile.get("groups");
                if (!(g instanceof List) || ((List<?>) g).isEmpty()) { finish.run(); return; }
                final List<?> groups = (List<?>) g;
                final AtomicInteger left = new AtomicInteger(groups.size());
                for (Object o : groups) {
                    final String gid = s(o);
                    GroupFirestoreManager.getInstance(c).getNotices(gid, new GroupFirestoreManager.ListCallback() {
                        @Override public void onSuccess(List<Map<String, Object>> notices) {
                            processNotices(c, uid, gid, notices);
                            if (left.decrementAndGet() == 0) finish.run();
                        }
                        @Override public void onError(String e) { if (left.decrementAndGet() == 0) finish.run(); }
                    });
                }
            }
            @Override public void onError(String e) { finish.run(); }
        });
    }

    private static void processNotices(Context c, String uid, String gid, List<Map<String, Object>> notices) {
        SharedPreferences sp = c.getSharedPreferences("notif_notice_seen", Context.MODE_PRIVATE);
        String key = uid + "_" + gid;
        long now = System.currentTimeMillis();
        if (!sp.contains(key)) { sp.edit().putLong(key, now).apply(); return; } // first run: baseline only
        long since = sp.getLong(key, now), newest = since;
        for (Map<String, Object> n : notices) {
            String cat = NoticeFilter.categoryFor(n, uid, since);
            long created = n.get("createdAt") instanceof Number ? ((Number) n.get("createdAt")).longValue() : 0;
            if (created > newest) newest = created;
            if (cat == null) continue;
            String title = s(n.get("title")), msg = s(n.get("message"));
            NotificationCenter.post(c, cat, "notice_" + s(n.get("_docId")),
                title.isEmpty() ? "New group notice" : title, msg, gid, cat.equals(NotificationCenter.PAYMENT) ? 2 : 4);
        }
        if (newest > since) sp.edit().putLong(key, newest).apply();
    }

    private static String s(Object o) { return o == null ? "" : String.valueOf(o); }
}
