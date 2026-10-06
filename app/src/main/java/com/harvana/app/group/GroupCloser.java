package com.harvana.app.group;

import android.content.Context;

import com.google.firebase.auth.FirebaseAuth;
import com.harvana.app.firestore.GroupFirestoreManager;
import com.harvana.app.firestore.UserProfileManager;
import com.harvana.app.sync.HarvanaNodeClient;
import com.harvana.app.sync.NodeGroupClient;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Closing or deleting a group: gathers the facts (the group's record, its members, loans and payout requests, and
 * for network groups the network's own state and escrow balance), lets {@link GroupClosePolicy} decide, and then
 * carries out the outcome. The rules live in the policy; this class only fetches and writes.
 */
public final class GroupCloser {
    private GroupCloser() {}

    public interface EvalCb { void onDecision(GroupClosePolicy.Decision d, GroupClosePolicy.Facts f); void onError(String e); }
    public interface DoneCb { void onDone(); void onError(String e); }

    public static final String CLOSED = "Closed";

    public static void evaluate(Context ctx, String groupId, EvalCb cb) {
        final Context c = ctx.getApplicationContext();
        final GroupFirestoreManager fm = GroupFirestoreManager.getInstance(c);
        final String me = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
        fm.getGroup(groupId, new GroupFirestoreManager.DocCallback() {
            @Override public void onSuccess(Map<String, Object> group) {
                final GroupClosePolicy.Facts f = new GroupClosePolicy.Facts();
                final String creator = s(group.get("creatorId"));
                f.admin = !me.isEmpty() && me.equals(creator);
                f.alreadyClosed = CLOSED.equalsIgnoreCase(s(group.get("status")));
                final String nodeId = s(group.get("nodeGroupId"));
                f.onChain = GroupKinds.isOnChain(group);
                f.family = f.onChain ? NodeGroupClient.familyOf(nodeId) : "";
                f.nodeId = f.onChain ? nodeId : "";

                final int[] left = {f.onChain ? 4 : 3};
                final String[] failure = {null};
                final Runnable done = () -> {
                    if (failure[0] != null) { cb.onError(failure[0]); return; }
                    cb.onDecision(GroupClosePolicy.decide(f), f);
                };
                final Runnable step = () -> { synchronized (left) { if (--left[0] > 0) return; } done.run(); };

                fm.getMembers(groupId, new GroupFirestoreManager.ListCallback() {
                    @Override public void onSuccess(List<Map<String, Object>> members) {
                        for (Map<String, Object> m : members) {
                            String st = s(m.get("status")).toUpperCase(Locale.US);
                            if ((st.isEmpty() || st.equals("ACTIVE")) && !creator.equals(s(m.get("memberUid")))) f.otherActiveMembers++;
                        }
                        step.run();
                    }
                    @Override public void onError(String e) { failure[0] = "Couldn't read the members: " + e; step.run(); }
                });
                fm.getLoans(groupId, new GroupFirestoreManager.ListCallback() {
                    @Override public void onSuccess(List<Map<String, Object>> loans) {
                        for (Map<String, Object> l : loans) {
                            String st = s(l.get("status")).toUpperCase(Locale.US);
                            if (!(st.equals("REPAID") || st.equals("REJECTED") || st.equals("DECLINED") || st.equals("CANCELLED")
                                || st.equals("DEFAULTED") || st.equals("WRITTEN_OFF") || st.equals("CLOSED"))) f.openLoans++;
                        }
                        // Payout requests next (kept in the same step count).
                        fm.getPayoutRequests(groupId, new GroupFirestoreManager.ListCallback() {
                            @Override public void onSuccess(List<Map<String, Object>> reqs) {
                                for (Map<String, Object> r : reqs) if (s(r.get("status")).equalsIgnoreCase("PENDING")) f.pendingPayoutRequests++;
                                step.run();
                            }
                            @Override public void onError(String e) { failure[0] = "Couldn't read the payout requests: " + e; step.run(); }
                        });
                        step.run();
                    }
                    @Override public void onError(String e) { failure[0] = "Couldn't read the loans: " + e; step.run(); step.run(); }
                });

                if (f.onChain) {
                    NodeGroupClient.Callback onNode = new NodeGroupClient.Callback() {
                        @Override public void onSuccess(JSONObject json) {
                            f.nodeState = json.optString("state", "");
                            String escrow = json.optString("escrowAddress", "");
                            if (escrow.isEmpty()) { f.escrowBalanceRaw = -1; step.run(); return; }
                            HarvanaNodeClient.getWallet(escrow, new HarvanaNodeClient.JsonCallback() {
                                @Override public void onSuccess(JSONObject w) { f.escrowBalanceRaw = w.optLong("balance", -1); step.run(); }
                                @Override public void onError(String e) { f.escrowBalanceRaw = -1; step.run(); }
                            });
                        }
                        @Override public void onError(String e) { f.nodeState = ""; step.run(); }
                    };
                    if ("CIRCLE".equals(f.family)) NodeGroupClient.getCircle(nodeId, onNode);
                    else if ("ACCUM".equals(f.family)) NodeGroupClient.getAccumGroup(nodeId, onNode);
                    else if ("FIXED".equals(f.family)) NodeGroupClient.getFixedGroup(nodeId, onNode);
                    else { f.nodeState = ""; step.run(); }
                }
            }
            @Override public void onError(String e) { cb.onError("Couldn't read the group: " + e); }
        });
    }

    /** Closes the group in the app: off Discover, joining stops, history stays, the admin can reopen it. */
    public static void archive(Context ctx, String groupId, String currentVisibility, String reason, DoneCb cb) {
        final Context c = ctx.getApplicationContext();
        final GroupFirestoreManager fm = GroupFirestoreManager.getInstance(c);
        String me = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
        // Members are told first (they read notices while the group is still open to them).
        fm.postSystemNotice(groupId, "Group closed",
            "The admin closed this group" + (reason == null || reason.isEmpty() ? "." : ": " + reason)
            + " Your history stays available. Nothing here changes what you are owed on the network.");
        // Pending join requests can no longer be answered.
        fm.getJoinRequests(groupId, new GroupFirestoreManager.ListCallback() {
            @Override public void onSuccess(List<Map<String, Object>> reqs) {
                for (Map<String, Object> r : reqs) {
                    if (!s(r.get("status")).equalsIgnoreCase("PENDING")) continue;
                    Map<String, Object> p = new HashMap<>();
                    p.put("status", "REJECTED");
                    p.put("reviewedAt", System.currentTimeMillis());
                    fm.updateJoinRequestFields(groupId, s(r.get("_docId")), p, new NoopCb());
                }
            }
            @Override public void onError(String e) { }
        });
        Map<String, Object> patch = new HashMap<>();
        patch.put("status", CLOSED);
        patch.put("closedAt", System.currentTimeMillis());
        patch.put("closedBy", me);
        patch.put("closedReason", reason == null ? "" : reason);
        patch.put("closedPrevVisibility", currentVisibility == null ? "Private" : currentVisibility);
        patch.put("visibility", "Private");
        fm.updateGroupFields(groupId, patch, new GroupFirestoreManager.SimpleCallback() {
            @Override public void onSuccess() { cb.onDone(); }
            @Override public void onError(String e) { cb.onError(e); }
        });
    }

    public static void reopen(Context ctx, String groupId, String previousVisibility, DoneCb cb) {
        GroupFirestoreManager fm = GroupFirestoreManager.getInstance(ctx.getApplicationContext());
        Map<String, Object> patch = new HashMap<>();
        patch.put("status", "Active");
        patch.put("visibility", previousVisibility == null || previousVisibility.isEmpty() ? "Private" : previousVisibility);
        patch.put("closedAt", null);
        fm.updateGroupFields(groupId, patch, new GroupFirestoreManager.SimpleCallback() {
            @Override public void onSuccess() {
                fm.postSystemNotice(groupId, "Group reopened", "The admin reopened this group.");
                cb.onDone();
            }
            @Override public void onError(String e) { cb.onError(e); }
        });
    }

    /**
     * Permanently removes a group from Firestore and from this phone: its invitations (deleted first, while the group
     * still exists for the rules to check the admin against), the group with every sub-collection, the deleter's
     * profile entry and the offline copies. Called for app-only groups with nothing open, and for a goal group its admin
     * closed on the network (records there are kept by the network itself).
     */
    public static void delete(Context ctx, String groupId, DoneCb cb) {
        final Context c = ctx.getApplicationContext();
        GroupFirestoreManager.getInstance(c).deleteTopLevelWhere("INVITES", "groupId", groupId, new GroupFirestoreManager.SimpleCallback() {
            @Override public void onSuccess() { deleteTree(c, groupId, cb); }
            @Override public void onError(String e) { deleteTree(c, groupId, cb); }   // best effort: invitations to a missing group expire anyway
        });
    }

    private static void deleteTree(Context c, String groupId, DoneCb cb) {
        String me = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
        GroupFirestoreManager.getInstance(c).deleteGroupTree(groupId, new GroupFirestoreManager.SimpleCallback() {
            @Override public void onSuccess() {
                GroupSaver.unqueue(c, groupId); // a queued retry must not bring it back
                forgetLocally(c, groupId);
                if (!me.isEmpty()) {
                    UserProfileManager.getInstance(c).removeGroupFromProfile(me, groupId, new UserProfileManager.SimpleCallback() {
                        @Override public void onSuccess() { cb.onDone(); }
                        @Override public void onError(String e) { cb.onDone(); } // best-effort; other lists drop a missing group by themselves
                    });
                } else cb.onDone();
            }
            @Override public void onError(String e) { cb.onError(e); }
        });
    }

    /**
     * A goal group nobody else joined: the node closes it and sends the group wallet back to the admin (signed with the admin's
     * wallet, network fee included), then the app's records are deleted. Nothing is deleted if the node refuses.
     */
    public static void refundAndDelete(android.app.Activity a, String groupId, String nodeGroupId, DoneCb cb) {
        final Context c = a.getApplicationContext();
        String wallet = com.harvana.app.wallet.WalletManager.getActiveWalletAddress(c);
        if (wallet == null || wallet.isEmpty()) { cb.onError("Open the wallet that created this group first."); return; }
        NodeGroupClient.closeFixedGroup(c, wallet, nodeGroupId, new NodeGroupClient.Callback() {
            @Override public void onSuccess(JSONObject json) {
                com.harvana.app.trust.AppActivity.note(c, "GROUP", "Closed a goal group and asked for a refund");
                com.harvana.app.utils.MediaCleaner.delete(a, com.harvana.app.utils.MediaCleaner.GROUP_PHOTO, groupId, () -> delete(c, groupId, cb));
            }
            @Override public void onError(String e) { cb.onError(com.harvana.app.circle.CircleUi.friendly(e)); }
        });
    }

    /** Drops this phone's cached copies of a deleted group so it cannot reappear from the offline cache. */
    static void forgetLocally(Context c, String groupId) {
        for (String k : new String[]{"group_" + groupId, "members_" + groupId, "loans_" + groupId, "loan_payments_" + groupId}) {
            com.harvana.app.offline.OfflineCacheManager.remove(c, k);
        }
        com.harvana.app.offline.OfflineCacheManager.remove(c, "discover_groups");   // rebuilt on the next Discover load
    }

    private static final class NoopCb implements GroupFirestoreManager.SimpleCallback {
        @Override public void onSuccess() { }
        @Override public void onError(String error) { }
    }

    private static String s(Object o) { return o == null ? "" : String.valueOf(o); }
}
