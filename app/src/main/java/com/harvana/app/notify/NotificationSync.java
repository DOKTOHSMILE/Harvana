package com.harvana.app.notify;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.auth.FirebaseAuth;
import com.harvana.app.firestore.GroupFirestoreManager;
import com.harvana.app.firestore.UserProfileManager;
import com.harvana.app.group.InviteManager;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Pulls what's new for the signed-in user and raises a phone notification even when Harvana
 * is not on screen. There is no push server, so WorkManager runs this about every 15 minutes
 * after the member leaves, and again after a reboot.
 */
public final class NotificationSync {
    private NotificationSync() {}

    public static void run(Context ctx, Runnable done) {
        final Context c = ctx.getApplicationContext();
        if (FirebaseAuth.getInstance().getCurrentUser() == null) { if (done != null) done.run(); return; }
        final String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        final AtomicInteger pending = new AtomicInteger(10);
        final Runnable finish = () -> { if (pending.decrementAndGet() == 0 && done != null) done.run(); };

        // 1. Invitations
        InviteManager.listMine(c, new InviteManager.ListCb() {
            @Override public void onList(List<Map<String, Object>> invites) {
                for (Map<String, Object> inv : invites) {
                    String id = s(inv.get("_docId"));
                    NotificationCenter.post(c, NotificationCenter.INVITE, "invite_" + id,
                        "You're invited to " + s(inv.get("groupName")),
                        s(inv.get("inviterName")) + " invited you. Open to read the details before you decide.", s(inv.get("groupId")), -1);
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

        // 5. Wallet: money received, sent and payments signed (from the node's confirmed history)
        WalletTxNotifier.run(c, events -> finish.run());
        // 5b. Every other asset (stable coins, other chains, added tokens): balance changes since the last check
        AssetBalanceNotifier.run(c);

        // 7. Pinches from friends
        com.harvana.app.social.PinchStore.poll(c, uid, finish);

        // 9. New messages on my P2P trades
        com.harvana.app.p2p.P2pChatStore.poll(c, uid, finish);

        // 8. Referrals: move my own along, and tell me when someone names me as their referrer
        com.harvana.app.social.ReferralStore.sync(c, (line, err) -> {
            com.harvana.app.social.ReferralStore.asksForMe((asks, e2) -> {
                for (com.harvana.app.social.ReferralStore.Ask a : asks) {
                    if (a.consented()) continue;
                    NotificationCenter.post(c, NotificationCenter.SOCIAL, "referral_ask_" + a.fromUid,
                        "Someone named you as their referrer",
                        "Open Friends, then Referrals, to confirm only if you know them. If they misbehave in groups, your trust score drops too.", "", -1);
                }
                finish.run();
            });
        });

        // 6. Contribution dues, including after the member has left the app
        DueAlertSync.run(c, uid, finish);

        // 10. Admin: join requests and loan applications. Pulled on the phone; no Cloud Function.
        AdminAlertSync.run(c, uid, finish);

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
                final SharedPreferences seen = c.getSharedPreferences("notif_notice_seen", Context.MODE_PRIVATE);
                for (Object o : groups) {
                    final String gid = s(o);
                    // First look at a group: set the baseline without reading anything (processNotices did the same after
                    // reading every notice).
                    if (!seen.contains(uid + "_" + gid)) {
                        seen.edit().putLong(uid + "_" + gid, System.currentTimeMillis()).apply();
                        if (left.decrementAndGet() == 0) finish.run();
                        continue;
                    }
                    final long since = seen.getLong(uid + "_" + gid, System.currentTimeMillis());
                    GroupFirestoreManager.getInstance(c).getGroup(gid, new GroupFirestoreManager.DocCallback() {
                        @Override public void onSuccess(Map<String, Object> group) {
                            final String groupName = s(group == null ? null : group.get("groupName"));
                            GroupFirestoreManager.getInstance(c).getNoticesSince(gid, since, 100, new GroupFirestoreManager.ListCallback() {
                                @Override public void onSuccess(List<Map<String, Object>> notices) {
                                    processNotices(c, uid, gid, groupName, notices);
                                    if (left.decrementAndGet() == 0) finish.run();
                                }
                                @Override public void onError(String e) { if (left.decrementAndGet() == 0) finish.run(); }
                            });
                        }
                        @Override public void onError(String e) { if (left.decrementAndGet() == 0) finish.run(); }
                    });
                }
            }
            @Override public void onError(String e) { finish.run(); }
        });
    }

    private static void processNotices(Context c, String uid, String gid, String groupName, List<Map<String, Object>> notices) {
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
            String who = s(n.get("actorName"));
            if (who.isEmpty()) who = s(n.get("memberName"));
            String named = title.isEmpty() ? "New group notice" : title;
            if (!groupName.isEmpty() && !named.contains(groupName)) named = groupName + " · " + named;
            if (!who.isEmpty() && !msg.contains(who)) msg = who + " — " + msg;
            // Open the page the notice is about: a waiting vote to Actions & votes, a new member to Members, a loan to Loans, ...
            int page = NoticeFilter.pageFor(n, cat);
            if (NoticeFilter.needsVote(n) && msg.isEmpty()) msg = "A vote is waiting. Open Actions & votes to vote.";
            // A vote or a join request is one notification however many notice documents were written for it.
            String noticeKey = "notice_" + s(n.get("_docId"));
            if (!s(n.get("proposalKind")).isEmpty()) noticeKey = "vote_" + gid + "_" + s(n.get("proposalKind")) + "_" + s(n.get("proposalId"));
            NotificationCenter.post(c, cat, noticeKey, named, msg, gid, page);
        }
        if (newest > since) sp.edit().putLong(key, newest).apply();
    }

    private static String s(Object o) { return o == null ? "" : String.valueOf(o); }
}
