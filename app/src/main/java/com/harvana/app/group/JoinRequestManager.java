package com.harvana.app.group;

import android.content.Context;
import android.util.Log;

import com.harvana.app.firestore.GroupFirestoreManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.HashMap;
import java.util.Map;

/**
 * Join request lifecycle — aligned with Group Engine Data Contract v1.0.
 *
 * Collection: GROUPS/{groupId}/joinRequests/{uid}
 *
 * Status machine:
 *   PENDING → APPROVED | REJECTED
 *
 * On APPROVED the following side-effects must all succeed (or be best-effort):
 *   1. members/{uid} created with next slot, status=ACTIVE, role=MEMBER
 *   2. GROUPS.membersCount incremented
 *   3. User profile groups list updated (Firestore USERS/{uid}.groups only)
 *   4. System notice posted
 *
 * Both admin-approve and member-vote paths must produce the same final state.
 */
public final class JoinRequestManager {

    private static final String TAG = "JoinRequestManager";

    public interface Callback {
        void onSuccess(String message);
        void onError(String error);
    }

    private JoinRequestManager() {}

    // ── Admin approve ────────────────────────────────────────────────────

    /**
     * Admin approves a pending join request.
     * nextSlot should be the next available slot (typically current membersCount + 1).
     * If nextSlot <= 0 the method will attempt to derive it from the group document.
     */
    public static void approve(Context ctx, String groupId, String requestDocId,
                               String requesterUid, int nextSlot, Callback cb) {
        if (ctx == null || groupId == null || groupId.isEmpty()
                || requesterUid == null || requesterUid.isEmpty()) {
            if (cb != null) cb.onError("Missing group or member id");
            return;
        }
        if (requestDocId == null || requestDocId.isEmpty()) {
            requestDocId = requesterUid;
        }

        // Spark plan has no Cloud Functions. Admin SDK path stays for Blaze later.
        approveLocal(ctx, groupId, requestDocId, requesterUid, nextSlot, cb);
    }

    /** Client-side approve path (used when Cloud Functions are unavailable). */
    private static void approveLocal(Context ctx, String groupId, String requestDocId,
                                     String requesterUid, int nextSlot, Callback cb) {
        final GroupFirestoreManager mgr = GroupFirestoreManager.getInstance(ctx);
        final String reqId = requestDocId;
        final int requestedSlot = nextSlot;

        // Resolve current membersCount so we can both assign slot and bump correctly.
        mgr.getGroup(groupId, new GroupFirestoreManager.DocCallback() {
            @Override
            public void onSuccess(Map<String, Object> groupData) {
                GroupIdentity identity = GroupIdentityResolver.resolve(groupId, groupData);
                if (identity.kind == GroupIdentity.Kind.ON_CHAIN_CIRCLE) {
                    if (cb != null) cb.onError("This circle adds a member only when they sign its terms. Approving the request does not put them on the network.");
                    return;
                }
                int currentCount = 0;
                Object countObj = groupData != null ? groupData.get("membersCount") : null;
                if (countObj instanceof Number) {
                    currentCount = ((Number) countObj).intValue();
                }
                final int resolvedSlot = requestedSlot > 0 ? requestedSlot : (currentCount + 1);
                final int newCount = currentCount + 1;

                Map<String, Object> patch = new HashMap<>();
                patch.put("status", "APPROVED");
                long now = System.currentTimeMillis();
                patch.put("reviewedAt", now);
                patch.put("resolvedAt", now);
                patch.put("resolvedBy", currentUidOr(requesterUid));

                mgr.updateJoinRequestFields(groupId, reqId, patch, new GroupFirestoreManager.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        admitCarryingContract(ctx, mgr, groupId, requesterUid, resolvedSlot, newCount, cb);
                    }

                    @Override
                    public void onError(String error) {
                        if (cb != null) cb.onError("Update request failed: " + error);
                    }
                });
            }

            @Override
            public void onError(String error) {
                // Fallback: still try to admit with the supplied slot
                Log.w(TAG, "Could not load group for membersCount: " + error);
                int fallbackSlot = requestedSlot > 0 ? requestedSlot : 1;
                Map<String, Object> patch = new HashMap<>();
                patch.put("status", "APPROVED");
                long now = System.currentTimeMillis();
                patch.put("reviewedAt", now);
                patch.put("resolvedAt", now);
                patch.put("resolvedBy", currentUidOr(requesterUid));

                mgr.updateJoinRequestFields(groupId, reqId, patch, new GroupFirestoreManager.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        admitCarryingContract(ctx, mgr, groupId, requesterUid, fallbackSlot, fallbackSlot, cb);
                    }

                    @Override
                    public void onError(String err) {
                        if (cb != null) cb.onError("Update request failed: " + err);
                    }
                });
            }
        });
    }

    /**
     * Shared admission sequence used by both admin approve and the vote path.
     * Side-effects: member doc → reconcile membersCount from live roster →
     * notice → profile groups.
     *
     * membersCount is set by counting ACTIVE members (not a blind +1) so two
     * near-simultaneous admits cannot both write the same stale count.
     * Not a true server transaction — use Cloud Functions when Blaze is on.
     */
    public static void admitMember(Context ctx,
                                   GroupFirestoreManager mgr,
                                   String groupId,
                                   String newMemberUid,
                                   int slot,
                                   int newMembersCount,
                                   Callback cb) {
        admitMember(ctx, mgr, groupId, newMemberUid, slot, newMembersCount, null, cb);
    }

    /** Admits a member, copying the signed-contract fields from their join request so the record is kept. */
    private static void admitCarryingContract(Context ctx, GroupFirestoreManager mgr, String groupId,
                                              String uid, int slot, int count, Callback cb) {
        mgr.getJoinRequest(groupId, uid, new GroupFirestoreManager.DocCallback() {
            @Override public void onSuccess(Map<String, Object> req) {
                Map<String, Object> extras = new HashMap<>();
                if (req != null) {
                    for (String k : new String[]{"walletAddress", "contractHash", "contractSig", "contractSignedAt"}) {
                        if (req.get(k) != null) extras.put(k, req.get(k));
                    }
                }
                admitMember(ctx, mgr, groupId, uid, slot, count, extras, cb);
            }
            @Override public void onError(String error) {
                admitMember(ctx, mgr, groupId, uid, slot, count, null, cb);
            }
        });
    }

    /** extras: signed-contract fields (walletAddress, contractHash, contractSig, contractSignedAt). */
    public static void admitMember(Context ctx,
                                   GroupFirestoreManager mgr,
                                   String groupId,
                                   String newMemberUid,
                                   int slot,
                                   int newMembersCount,
                                   Map<String, Object> extras,
                                   Callback cb) {
        Map<String, Object> member = new HashMap<>();
        if (extras != null) member.putAll(extras);
        member.put("memberUid", newMemberUid);
        member.put("slot", slot > 0 ? slot : 1);
        member.put("joinedAt", System.currentTimeMillis());
        member.put("status", "ACTIVE");
        member.put("role", "MEMBER");

        mgr.addMember(groupId, newMemberUid, member, new GroupFirestoreManager.SimpleCallback() {
            @Override
            public void onSuccess() {
                reconcileMembersCount(mgr, groupId, newMembersCount, () ->
                        finishAdmission(ctx, mgr, groupId, newMemberUid, cb));
            }

            @Override
            public void onError(String error) {
                // Member may already exist from a concurrent admit — still reconcile
                if (error != null && (com.harvana.app.utils.ErrorText.is(error, "409") || com.harvana.app.utils.ErrorText.is(error, "ALREADY"))) {
                    reconcileMembersCount(mgr, groupId, newMembersCount, () ->
                            finishAdmission(ctx, mgr, groupId, newMemberUid, cb));
                } else if (cb != null) {
                    cb.onError("Add member failed: " + error);
                }
            }
        });
    }

    /**
     * Set GROUPS.membersCount to the number of ACTIVE member docs.
     * Fallback to expectedCount if the members list cannot be loaded.
     */
    public static void reconcileMembersCount(GroupFirestoreManager mgr,
                                             String groupId,
                                             int expectedCount,
                                             Runnable onDone) {
        if (mgr == null || groupId == null || groupId.isEmpty()) {
            if (onDone != null) onDone.run();
            return;
        }
        mgr.getMembers(groupId, new GroupFirestoreManager.ListCallback() {
            @Override
            public void onSuccess(java.util.List<Map<String, Object>> items) {
                int active = 0;
                if (items != null) {
                    for (Map<String, Object> m : items) {
                        if (m == null) continue;
                        Object st = m.get("status");
                        String status = st != null ? st.toString() : "ACTIVE";
                        if ("ACTIVE".equalsIgnoreCase(status) || status.isEmpty()) {
                            active++;
                        }
                    }
                }
                // Trust the live roster, including 0 after the last member leaves.
                // Only fall back when the list itself couldn't be read.
                int count = items != null ? active : Math.max(expectedCount, 0);
                Map<String, Object> countUpdate = new HashMap<>();
                countUpdate.put("membersCount", count);
                mgr.updateGroupFields(groupId, countUpdate, new GroupFirestoreManager.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        if (onDone != null) onDone.run();
                    }

                    @Override
                    public void onError(String error) {
                        Log.w(TAG, "reconcile membersCount failed: " + error);
                        if (onDone != null) onDone.run();
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.w(TAG, "getMembers for reconcile failed: " + error);
                Map<String, Object> countUpdate = new HashMap<>();
                countUpdate.put("membersCount", Math.max(expectedCount, 1));
                mgr.updateGroupFields(groupId, countUpdate, new GroupFirestoreManager.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        if (onDone != null) onDone.run();
                    }

                    @Override
                    public void onError(String err) {
                        if (onDone != null) onDone.run();
                    }
                });
            }
        });
    }

    private static void finishAdmission(Context ctx,
                                        GroupFirestoreManager mgr,
                                        String groupId,
                                        String newMemberUid,
                                        Callback cb) {
        mgr.postSystemNotice(groupId, "New member joined",
                "A new member has joined the group.");

        appendGroupToUserProfile(ctx, newMemberUid, groupId, new Callback() {
            @Override
            public void onSuccess(String message) {
                if (cb != null) cb.onSuccess("Member approved and added.");
            }

            @Override
            public void onError(String error) {
                if (cb != null) {
                    cb.onSuccess("Member added (profile groups update: " + error + ")");
                }
            }
        });
    }

    // ── Reject ───────────────────────────────────────────────────────────

    public static void reject(Context ctx, String groupId, String requestDocId, Callback cb) {
        if (ctx == null || groupId == null || requestDocId == null || requestDocId.isEmpty()) {
            if (cb != null) cb.onError("Missing ids");
            return;
        }
        Map<String, Object> patch = new HashMap<>();
        patch.put("status", "REJECTED");
        patch.put("reviewedAt", System.currentTimeMillis());
        patch.put("resolvedAt", System.currentTimeMillis());
        patch.put("resolvedBy", currentUidOr(""));
        GroupFirestoreManager.getInstance(ctx)
                .updateJoinRequestFields(groupId, requestDocId, patch, new GroupFirestoreManager.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        if (cb != null) cb.onSuccess("Request rejected.");
                    }

                    @Override
                    public void onError(String err) {
                        if (cb != null) cb.onError(err);
                    }
                });
    }

    private static String currentUidOr(String fallback) {
        try {
            FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
            if (u != null && u.getUid() != null && !u.getUid().isEmpty()) return u.getUid();
        } catch (Exception ignored) {}
        return fallback != null ? fallback : "";
    }

    /** OPEN (settings) and NONE (create-group wizard) both mean no approval gate. */
    public static boolean isOpenJoin(String method) {
        if (method == null) return false;
        String m = method.trim().toUpperCase();
        return "OPEN".equals(m) || "NONE".equals(m);
    }

    /**
     * Member leaves: mark LEFT, drop the group from their own profile,
     * recount ACTIVE members, post a notice. Profile write is on the
     * leaver's own device so firestore.rules isSelf() allows it.
     */
    public static void leaveGroup(Context ctx, String groupId, String uid, Callback cb) {
        if (ctx == null || GroupValidation.isBlank(groupId) || GroupValidation.isBlank(uid)) {
            if (cb != null) cb.onError("Missing group or member id");
            return;
        }
        final GroupFirestoreManager mgr = GroupFirestoreManager.getInstance(ctx);
        Map<String, Object> patch = new HashMap<>();
        patch.put("status", "LEFT");
        patch.put("leftAt", System.currentTimeMillis());
        mgr.updateMemberFields(groupId, uid, patch, new GroupFirestoreManager.SimpleCallback() {
            @Override
            public void onSuccess() {
                // Drop stale "I'm still in this group" cache so Groups tab
                // doesn't flash the left group on the next open.
                try {
                    com.harvana.app.offline.OfflineCacheManager.remove(ctx, "my_groups_" + uid);
                    com.harvana.app.offline.OfflineCacheManager.remove(ctx, "members_" + groupId);
                } catch (Exception ignored) {}

                // Best-effort: mark the join request so an old APPROVED
                // doc can't be used to self-heal this membership back.
                Map<String, Object> reqPatch = new HashMap<>();
                reqPatch.put("status", "LEFT");
                reqPatch.put("leftAt", System.currentTimeMillis());
                mgr.updateJoinRequestFields(groupId, uid, reqPatch, new GroupFirestoreManager.SimpleCallback() {
                    @Override public void onSuccess() {}
                    @Override public void onError(String error) {}
                });

                com.harvana.app.firestore.UserProfileManager.getInstance(ctx)
                        .removeGroupFromProfile(uid, groupId, new com.harvana.app.firestore.UserProfileManager.SimpleCallback() {
                            @Override
                            public void onSuccess() {
                                reconcileMembersCount(mgr, groupId, 0, () -> {
                                    mgr.postSystemNotice(groupId, "Member left",
                                            "A member has left the group.");
                                    if (cb != null) cb.onSuccess("You have left the group.");
                                });
                            }

                            @Override
                            public void onError(String error) {
                                reconcileMembersCount(mgr, groupId, 0, () -> {
                                    mgr.postSystemNotice(groupId, "Member left",
                                            "A member has left the group.");
                                    if (cb != null) {
                                        cb.onSuccess("Left the group (profile cleanup: " + error + ")");
                                    }
                                });
                            }
                        });
            }

            @Override
            public void onError(String error) {
                if (cb != null) cb.onError("Couldn't leave group: " + error);
            }
        });
    }

    /** Reactivate a LEFT member (open groups / rejoin after leaving). */
    public static void reactivateMember(Context ctx, String groupId, String uid, Callback cb) {
        if (ctx == null || GroupValidation.isBlank(groupId) || GroupValidation.isBlank(uid)) {
            if (cb != null) cb.onError("Missing group or member id");
            return;
        }
        final GroupFirestoreManager mgr = GroupFirestoreManager.getInstance(ctx);
        Map<String, Object> patch = new HashMap<>();
        patch.put("status", "ACTIVE");
        patch.put("joinedAt", System.currentTimeMillis());
        mgr.updateMemberFields(groupId, uid, patch, new GroupFirestoreManager.SimpleCallback() {
            @Override
            public void onSuccess() {
                reconcileMembersCount(mgr, groupId, 1, () ->
                        appendGroupToUserProfile(ctx, uid, groupId, new Callback() {
                            @Override
                            public void onSuccess(String message) {
                                if (cb != null) cb.onSuccess("You're back in the group.");
                            }
                            @Override
                            public void onError(String error) {
                                if (cb != null) cb.onSuccess("Rejoined (profile update: " + error + ")");
                            }
                        }));
            }
            @Override
            public void onError(String error) {
                if (cb != null) cb.onError(error);
            }
        });
    }

    /**
     * Admin removes a member. Marks KICKED (not LEFT) so they can't
     * silently re-activate on an open group, appends them to
     * blockedMemberUids, recounts ACTIVE members, posts a notice.
     * Their USERS/{uid}.groups row is cleaned on THEIR device by
     * self-heal (isSelf) — the admin cannot write it.
     */
    public static void kickMember(Context ctx, String groupId, String targetUid, Callback cb) {
        if (ctx == null || GroupValidation.isBlank(groupId) || GroupValidation.isBlank(targetUid)) {
            if (cb != null) cb.onError("Missing group or member id");
            return;
        }
        String actor = currentUidOr("");
        if (actor.equals(targetUid)) {
            if (cb != null) cb.onError("You can't remove yourself — use Leave group.");
            return;
        }
        final GroupFirestoreManager mgr = GroupFirestoreManager.getInstance(ctx);
        Map<String, Object> patch = new HashMap<>();
        patch.put("status", "KICKED");
        patch.put("kickedAt", System.currentTimeMillis());
        patch.put("kickedBy", actor);
        mgr.updateMemberFields(groupId, targetUid, patch, new GroupFirestoreManager.SimpleCallback() {
            @Override
            public void onSuccess() {
                mgr.getGroup(groupId, new GroupFirestoreManager.DocCallback() {
                    @Override
                    public void onSuccess(Map<String, Object> group) {
                        java.util.List<Object> blocked = new java.util.ArrayList<>();
                        Object raw = group != null ? group.get("blockedMemberUids") : null;
                        if (raw instanceof java.util.List) {
                            for (Object o : (java.util.List<?>) raw) {
                                if (o != null && !String.valueOf(o).trim().isEmpty()) {
                                    blocked.add(String.valueOf(o).trim());
                                }
                            }
                        }
                        if (!blocked.contains(targetUid)) blocked.add(targetUid);
                        Map<String, Object> gPatch = new HashMap<>();
                        gPatch.put("blockedMemberUids", blocked);
                        mgr.updateGroupFields(groupId, gPatch, new GroupFirestoreManager.SimpleCallback() {
                            @Override public void onSuccess() { finishKick(mgr, groupId, cb); }
                            @Override public void onError(String error) { finishKick(mgr, groupId, cb); }
                        });
                    }
                    @Override
                    public void onError(String error) {
                        finishKick(mgr, groupId, cb);
                    }
                });
            }
            @Override
            public void onError(String error) {
                if (cb != null) cb.onError("Couldn't remove member: " + error);
            }
        });
    }

    private static void finishKick(GroupFirestoreManager mgr, String groupId, Callback cb) {
        reconcileMembersCount(mgr, groupId, 0, () -> {
            mgr.postSystemNotice(groupId, "Member removed",
                    "A member was removed from the group.");
            if (cb != null) cb.onSuccess("Member removed.");
        });
    }

    public static boolean isActiveMemberStatus(String status) {
        if (status == null || status.trim().isEmpty()) return true;
        return "ACTIVE".equalsIgnoreCase(status.trim());
    }

    public static boolean isInactiveMemberStatus(String status) {
        if (status == null) return false;
        String s = status.trim();
        return "LEFT".equalsIgnoreCase(s) || "KICKED".equalsIgnoreCase(s) || "REMOVED".equalsIgnoreCase(s);
    }

    // ── Profile append (Firestore only) ─────────────────────────────────────

    /**
     * Appends the group id to USERS/{uid}.groups in Firestore.
     * RTDB userdb/{uid}/groups is no longer written. TrustScoreEngine and
     * Home read Firestore first; RTDB remains a read-only fallback for
     * legacy accounts that never got a Firestore groups array.
     */
    public static void appendGroupToUserProfile(Context ctx, String uid, String groupId, Callback cb) {
        if (ctx == null || GroupValidation.isBlank(uid) || GroupValidation.isBlank(groupId)) {
            if (cb != null) cb.onError("Missing profile or group id.");
            return;
        }
        com.harvana.app.firestore.UserProfileManager.getInstance(ctx)
                .addGroupToProfile(uid, groupId, new com.harvana.app.firestore.UserProfileManager.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        if (cb != null) cb.onSuccess("ok");
                    }

                    @Override
                    public void onError(String error) {
                        Log.w(TAG, "Failed to add group to Firestore profile: " + error);
                        if (cb != null) cb.onError(error != null ? error : "Profile update failed");
                    }
                });
    }

    // ── Self-heal (client-only workaround, no Cloud Functions required) ────

    public interface SelfHealCallback {
        /** healedCount is how many groups were newly appended - 0 means nothing was missing. */
        void onDone(int healedCount);
    }

    /**
     * Runs entirely on the signed-in user's OWN device with THEIR OWN uid,
     * so every write here satisfies firestore.rules' "allow update: if
     * isSelf(uid)" on USERS/{uid}.
     *
     * Source of truth is the members subcollection (status ACTIVE vs
     * LEFT/KICKED), not joinRequests. An old APPROVED join request must
     * not put a leaver back into USERS.groups — that was undoing Leave.
     */
    public static void selfHealApprovedMemberships(Context ctx, String myUid, SelfHealCallback cb) {
        if (ctx == null || GroupValidation.isBlank(myUid)) {
            if (cb != null) cb.onDone(0);
            return;
        }
        GroupFirestoreManager.getInstance(ctx).queryMyMemberships(myUid,
                new GroupFirestoreManager.ListCallback() {
                    @Override
                    public void onSuccess(java.util.List<Map<String, Object>> members) {
                        java.util.Set<String> activeIds = new java.util.HashSet<>();
                        java.util.Set<String> inactiveIds = new java.util.HashSet<>();
                        if (members != null) {
                            for (Map<String, Object> m : members) {
                                if (m == null) continue;
                                Object gidObj = m.get("groupId");
                                if (gidObj == null || String.valueOf(gidObj).trim().isEmpty()) continue;
                                String gid = String.valueOf(gidObj).trim();
                                Object st = m.get("status");
                                String status = st != null ? String.valueOf(st) : "ACTIVE";
                                if (isInactiveMemberStatus(status)) inactiveIds.add(gid);
                                else if (isActiveMemberStatus(status)) activeIds.add(gid);
                            }
                        }
                        loadPendingThenDiff(ctx, myUid, activeIds, inactiveIds, cb);
                    }

                    @Override
                    public void onError(String error) {
                        Log.w(TAG, "queryMyMemberships failed, join-request fallback: " + error);
                        fallbackHealFromJoinRequests(ctx, myUid, cb);
                    }
                });
    }

    private static void loadPendingThenDiff(Context ctx, String myUid,
                                            java.util.Set<String> activeIds,
                                            java.util.Set<String> inactiveIds,
                                            SelfHealCallback cb) {
        GroupFirestoreManager.getInstance(ctx).queryMyJoinRequests(myUid,
                new GroupFirestoreManager.ListCallback() {
                    @Override
                    public void onSuccess(java.util.List<Map<String, Object>> requests) {
                        java.util.List<String> resolvedGroupIds = new java.util.ArrayList<>();
                        if (requests != null) {
                            for (Map<String, Object> r : requests) {
                                if (r == null) continue;
                                Object status = r.get("status");
                                Object gid = r.get("groupId");
                                if (status == null || gid == null || String.valueOf(gid).trim().isEmpty()) continue;
                                String st = String.valueOf(status);
                                String id = String.valueOf(gid).trim();
                                if ("APPROVED".equalsIgnoreCase(st)
                                        || "REJECTED".equalsIgnoreCase(st)
                                        || "LEFT".equalsIgnoreCase(st)
                                        || "KICKED".equalsIgnoreCase(st)) {
                                    resolvedGroupIds.add(id);
                                }
                                // Never treat APPROVED as membership if the
                                // member doc says LEFT/KICKED.
                            }
                        }
                        diffAgainstProfile(ctx, myUid, new java.util.ArrayList<>(activeIds),
                                resolvedGroupIds, inactiveIds, cb);
                    }

                    @Override
                    public void onError(String error) {
                        Log.w(TAG, "join-request query for pending cleanup failed: " + error);
                        diffAgainstProfile(ctx, myUid, new java.util.ArrayList<>(activeIds),
                                new java.util.ArrayList<>(), inactiveIds, cb);
                    }
                });
    }

    /** Fallback when the members collection-group query is denied / missing. */
    private static void fallbackHealFromJoinRequests(Context ctx, String myUid, SelfHealCallback cb) {
        GroupFirestoreManager.getInstance(ctx).queryMyJoinRequests(myUid,
                new GroupFirestoreManager.ListCallback() {
                    @Override
                    public void onSuccess(java.util.List<Map<String, Object>> requests) {
                        java.util.List<String> approvedGroupIds = new java.util.ArrayList<>();
                        java.util.List<String> resolvedGroupIds = new java.util.ArrayList<>();
                        if (requests != null) {
                            for (Map<String, Object> r : requests) {
                                if (r == null) continue;
                                Object status = r.get("status");
                                Object gid = r.get("groupId");
                                if (status == null || gid == null || String.valueOf(gid).trim().isEmpty()) continue;
                                String st = String.valueOf(status);
                                String id = String.valueOf(gid).trim();
                                if ("APPROVED".equalsIgnoreCase(st)) {
                                    approvedGroupIds.add(id);
                                    resolvedGroupIds.add(id);
                                } else if ("REJECTED".equalsIgnoreCase(st)
                                        || "LEFT".equalsIgnoreCase(st)
                                        || "KICKED".equalsIgnoreCase(st)) {
                                    resolvedGroupIds.add(id);
                                }
                            }
                        }
                        verifyApprovedStillActive(ctx, myUid, approvedGroupIds, resolvedGroupIds, cb);
                    }

                    @Override
                    public void onError(String error) {
                        Log.w(TAG, "selfHealApprovedMemberships query failed: " + error);
                        if (cb != null) cb.onDone(0);
                    }
                });
    }

    /** Drop APPROVED join-request ids whose member doc is LEFT/KICKED/missing. */
    private static void verifyApprovedStillActive(Context ctx, String myUid,
                                                  java.util.List<String> approvedGroupIds,
                                                  java.util.List<String> resolvedGroupIds,
                                                  SelfHealCallback cb) {
        if (approvedGroupIds.isEmpty()) {
            diffAgainstProfile(ctx, myUid, approvedGroupIds, resolvedGroupIds,
                    new java.util.HashSet<>(), cb);
            return;
        }
        final java.util.List<String> stillActive = new java.util.ArrayList<>();
        final java.util.Set<String> inactive = new java.util.HashSet<>();
        final int[] remaining = {approvedGroupIds.size()};
        final GroupFirestoreManager mgr = GroupFirestoreManager.getInstance(ctx);
        for (String gid : approvedGroupIds) {
            mgr.getMember(gid, myUid, new GroupFirestoreManager.DocCallback() {
                @Override
                public void onSuccess(Map<String, Object> member) {
                    Object st = member != null ? member.get("status") : null;
                    String status = st != null ? String.valueOf(st) : "ACTIVE";
                    if (isInactiveMemberStatus(status)) inactive.add(gid);
                    else if (isActiveMemberStatus(status)) stillActive.add(gid);
                    done();
                }
                @Override
                public void onError(String error) {
                    // 404: never became a member (or doc gone) — don't add.
                    done();
                }
                private void done() {
                    remaining[0]--;
                    if (remaining[0] <= 0) {
                        diffAgainstProfile(ctx, myUid, stillActive, resolvedGroupIds, inactive, cb);
                    }
                }
            });
        }
    }

    private static void diffAgainstProfile(Context ctx, String myUid,
                                           java.util.List<String> activeGroupIds,
                                           java.util.List<String> resolvedGroupIds,
                                           java.util.Set<String> inactiveGroupIds,
                                           SelfHealCallback cb) {
        com.harvana.app.firestore.UserProfileManager.getInstance(ctx).getProfile(myUid,
                new com.harvana.app.firestore.UserProfileManager.DocCallback() {
                    @Override
                    public void onSuccess(Map<String, Object> profile) {
                        java.util.Set<String> existing = new java.util.HashSet<>();
                        Object groups = profile != null ? profile.get("groups") : null;
                        if (groups instanceof java.util.List) {
                            for (Object g : (java.util.List<?>) groups) {
                                if (g != null) existing.add(String.valueOf(g));
                            }
                        }
                        java.util.Set<String> stillPending = new java.util.HashSet<>();
                        Object pending = profile != null ? profile.get("pendingGroupIds") : null;
                        if (pending instanceof java.util.List) {
                            for (Object g : (java.util.List<?>) pending) {
                                if (g != null) stillPending.add(String.valueOf(g));
                            }
                        }
                        java.util.List<String> missingGroups = new java.util.ArrayList<>();
                        for (String gid : activeGroupIds) {
                            if (!existing.contains(gid) && (inactiveGroupIds == null || !inactiveGroupIds.contains(gid))) {
                                missingGroups.add(gid);
                            }
                        }
                        java.util.List<String> staleGroups = new java.util.ArrayList<>();
                        if (inactiveGroupIds != null) {
                            for (String gid : inactiveGroupIds) {
                                if (existing.contains(gid)) staleGroups.add(gid);
                            }
                        }
                        java.util.List<String> pendingToClean = new java.util.ArrayList<>();
                        for (String gid : resolvedGroupIds) {
                            if (stillPending.contains(gid)) pendingToClean.add(gid);
                        }
                        if (missingGroups.isEmpty() && staleGroups.isEmpty() && pendingToClean.isEmpty()) {
                            if (cb != null) cb.onDone(0);
                            return;
                        }
                        healNext(ctx, myUid, missingGroups, 0, 0, healed ->
                                dropNext(ctx, myUid, staleGroups, 0, healed, dropped ->
                                        cleanPendingNext(ctx, myUid, pendingToClean, 0, dropped, cb)));
                    }

                    @Override
                    public void onError(String error) {
                        if (error != null && com.harvana.app.utils.ErrorText.is(error, "404")) {
                            healNext(ctx, myUid, activeGroupIds, 0, 0, cb);
                            return;
                        }
                        Log.w(TAG, "selfHeal profile read failed: " + error);
                        if (cb != null) cb.onDone(0);
                    }
                });
    }

    /** Appends groups one at a time. */
    private static void healNext(Context ctx, String myUid, java.util.List<String> groupIds,
                                 int index, int healedCount, SelfHealCallback cb) {
        if (index >= groupIds.size()) {
            if (cb != null) cb.onDone(healedCount);
            return;
        }
        String groupId = groupIds.get(index);
        appendGroupToUserProfile(ctx, myUid, groupId, new Callback() {
            @Override
            public void onSuccess(String message) {
                healNext(ctx, myUid, groupIds, index + 1, healedCount + 1, cb);
            }

            @Override
            public void onError(String error) {
                healNext(ctx, myUid, groupIds, index + 1, healedCount, cb);
            }
        });
    }

    /** Drops LEFT/KICKED groups from USERS.groups so Leave actually sticks. */
    private static void dropNext(Context ctx, String myUid, java.util.List<String> groupIds,
                                 int index, int changedCount, SelfHealCallback cb) {
        if (index >= groupIds.size()) {
            if (cb != null) cb.onDone(changedCount);
            return;
        }
        String groupId = groupIds.get(index);
        com.harvana.app.firestore.UserProfileManager.getInstance(ctx)
                .removeGroupFromProfile(myUid, groupId,
                        new com.harvana.app.firestore.UserProfileManager.SimpleCallback() {
                            @Override
                            public void onSuccess() {
                                dropNext(ctx, myUid, groupIds, index + 1, changedCount + 1, cb);
                            }
                            @Override
                            public void onError(String error) {
                                dropNext(ctx, myUid, groupIds, index + 1, changedCount, cb);
                            }
                        });
    }

    /**
     * A "Pending" mark on my profile that no longer matches a real waiting request is cleared: the request was approved, rejected,
     * withdrawn, or never saved (a failed write, a deleted group). Reads each marked group's join request directly, so it does
     * not depend on the collection query the other self-heal uses. Calls back with how many marks were removed.
     */
    public static void reconcilePending(Context ctx, String myUid, SelfHealCallback cb) {
        if (ctx == null || myUid == null || myUid.isEmpty()) { if (cb != null) cb.onDone(0); return; }
        final com.harvana.app.firestore.UserProfileManager profiles = com.harvana.app.firestore.UserProfileManager.getInstance(ctx);
        profiles.getProfile(myUid, new com.harvana.app.firestore.UserProfileManager.DocCallback() {
            @Override public void onSuccess(Map<String, Object> profile) {
                final java.util.List<String> marked = new java.util.ArrayList<>();
                Object pending = profile != null ? profile.get("pendingGroupIds") : null;
                if (pending instanceof java.util.List) for (Object g : (java.util.List<?>) pending) if (g != null) marked.add(String.valueOf(g));
                if (marked.isEmpty()) { if (cb != null) cb.onDone(0); return; }
                final java.util.List<String> stale = new java.util.ArrayList<>();
                final int[] left = {marked.size()};
                final GroupFirestoreManager mgr = GroupFirestoreManager.getInstance(ctx);
                for (String gid : marked) {
                    mgr.getJoinRequest(gid, myUid, new GroupFirestoreManager.DocCallback() {
                        @Override public void onSuccess(Map<String, Object> req) {
                            Object st = req == null ? null : req.get("status");
                            if (st == null || !"PENDING".equalsIgnoreCase(String.valueOf(st))) stale.add(gid);
                            finish();
                        }
                        @Override public void onError(String error) {
                            if (error != null && (com.harvana.app.utils.ErrorText.is(error, "404") || com.harvana.app.utils.ErrorText.is(error, "NOT_FOUND"))) stale.add(gid);   // no such request
                            finish();
                        }
                        private void finish() {
                            left[0]--;
                            if (left[0] <= 0) cleanPendingNext(ctx, myUid, stale, 0, 0, cb);
                        }
                    });
                }
            }
            @Override public void onError(String error) { if (cb != null) cb.onDone(0); }
        });
    }

    /** Strips pendingGroupIds for every request that's actually resolved. */
    private static void cleanPendingNext(Context ctx, String myUid, java.util.List<String> groupIds,
                                         int index, int healedCount, SelfHealCallback cb) {
        if (index >= groupIds.size()) {
            if (cb != null) cb.onDone(healedCount);
            return;
        }
        String groupId = groupIds.get(index);
        com.harvana.app.firestore.UserProfileManager.getInstance(ctx)
                .removePendingGroupFromProfile(myUid, groupId,
                        new com.harvana.app.firestore.UserProfileManager.SimpleCallback() {
                            @Override
                            public void onSuccess() {
                                cleanPendingNext(ctx, myUid, groupIds, index + 1, healedCount + 1, cb);
                            }

                            @Override
                            public void onError(String error) {
                                cleanPendingNext(ctx, myUid, groupIds, index + 1, healedCount, cb);
                            }
                        });
    }
}