package com.harvana.app.notify;

import android.content.Context;

import com.harvana.app.firestore.GroupFirestoreManager;
import com.harvana.app.firestore.UserProfileManager;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Admin alerts without a Cloud Function. The same 15-minute pull that brings member notices
 * also looks at groups this person created.
 */
public final class AdminAlertSync {
    private AdminAlertSync() {}

    public static void run(Context ctx, String uid, Runnable done) {
        Context c = ctx.getApplicationContext();
        UserProfileManager.getInstance(c).getProfile(uid, new UserProfileManager.DocCallback() {
            @Override public void onSuccess(Map<String, Object> profile) {
                Object raw = profile == null ? null : profile.get("groups");
                if (!(raw instanceof List) || ((List<?>) raw).isEmpty()) { finish(done); return; }
                List<?> groups = (List<?>) raw;
                AtomicInteger left = new AtomicInteger(groups.size());
                for (Object item : groups) {
                    String gid = item == null ? "" : String.valueOf(item).trim();
                    if (gid.isEmpty()) {
                        if (left.decrementAndGet() == 0) finish(done);
                        continue;
                    }
                    check(c, uid, gid, () -> { if (left.decrementAndGet() == 0) finish(done); });
                }
            }
            @Override public void onError(String error) { finish(done); }
        });
    }

    private static void check(Context c, String uid, String gid, Runnable done) {
        GroupFirestoreManager mgr = GroupFirestoreManager.getInstance(c);
        mgr.getGroup(gid, new GroupFirestoreManager.DocCallback() {
            @Override public void onSuccess(Map<String, Object> group) {
                String admin = text(group, "creatorId");
                if (admin.isEmpty()) admin = text(group, "adminUid");
                if (admin.isEmpty()) admin = text(group, "createdBy");
                if (!uid.equals(admin)) { done.run(); return; }
                String name = text(group, "groupName");
                if (name.isEmpty()) name = "Your group";
                final String groupName = name;
                AtomicInteger parts = new AtomicInteger(2);
                Runnable step = () -> { if (parts.decrementAndGet() == 0) done.run(); };
                mgr.getJoinRequests(gid, new GroupFirestoreManager.ListCallback() {
                    @Override public void onSuccess(List<Map<String, Object>> rows) {
                        for (Map<String, Object> row : rows) {
                            String st = text(row, "status").toUpperCase();
                            if (!st.isEmpty() && !st.equals("PENDING")) continue;
                            String id = text(row, "_docId");
                            NotificationCenter.post(c, NotificationCenter.MEMBERSHIP, "admin_join_" + gid + "_" + id,
                                "Join request · " + groupName, "Someone asked to join. Open the group to accept or reject.", gid, 4);
                        }
                        step.run();
                    }
                    @Override public void onError(String error) { step.run(); }
                });
                mgr.getLoans(gid, new GroupFirestoreManager.ListCallback() {
                    @Override public void onSuccess(List<Map<String, Object>> rows) {
                        for (Map<String, Object> row : rows) {
                            String st = text(row, "status").toUpperCase();
                            if (!st.equals("PENDING") && !st.equals("PENDING_APPROVAL")) continue;
                            String id = text(row, "_docId");
                            NotificationCenter.post(c, NotificationCenter.LOAN, "admin_loan_" + gid + "_" + id,
                                "Loan request · " + groupName, "A member asked for a loan. Open Loans to decide.", gid, 3);
                        }
                        step.run();
                    }
                    @Override public void onError(String error) { step.run(); }
                });
            }
            @Override public void onError(String error) { done.run(); }
        });
    }

    private static void finish(Runnable done) { if (done != null) done.run(); }

    private static String text(Map<String, Object> map, String key) {
        Object v = map == null ? null : map.get(key);
        return v == null ? "" : String.valueOf(v).trim();
    }
}
