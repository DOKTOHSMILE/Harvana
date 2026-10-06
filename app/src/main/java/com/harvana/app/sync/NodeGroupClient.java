package com.harvana.app.sync;

import android.content.Context;
import android.util.Base64;

import com.harvana.app.chain.GroupCanonical;
import com.harvana.app.crypto.Ed25519;
import com.harvana.app.wallet.WalletManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

/**
 * Group, roster, and staking screens read the Firestore copy the node pushes.
 * If that copy is missing, or Firestore refuses the read, the same call uses
 * the node. No switch to flip when the Firebase plan changes. A signed
 * unstake or claim still reads the live nonce from the node.
 *
 * Request nonce: the node requires each signed request to carry the wallet's
 * "nextRequestNonce" (see GET /api/v1/wallet/balance/{address}); this client fetches
 * it fresh for every action.
 *
 * Amounts are RAW units (1 HARV = 100,000,000). Ids: "CIRC-…", "ACCUM-…", "FIXED-…".
 */
public final class NodeGroupClient {

    public interface Callback {
        void onSuccess(JSONObject json);
        void onError(String error);
    }

    private NodeGroupClient() {}

    // ═════════════════════════════ READS ═════════════════════════════

    public static void getCircle(String circleId, Callback cb) { mirrored("GROUP_STATE", circleId, "/api/v1/circle/" + circleId, cb); }
    public static void getCircleMember(String circleId, String address, Callback cb) { read("/api/v1/circle/" + circleId + "/member/" + address, cb); }
    public static void getCircleMembers(String circleId, Callback cb) { mirrored("GROUP_MEMBERS", circleId, "/api/v1/circle/" + circleId + "/members", cb); }

    public static void getAccumGroup(String groupId, Callback cb) { mirrored("GROUP_STATE", groupId, "/api/v1/accum/group/" + groupId, cb); }
    public static void getAccumMember(String groupId, String address, Callback cb) { read("/api/v1/accum/group/" + groupId + "/member/" + address, cb); }
    public static void getAccumLine(String groupId, String lineId, Callback cb) { read("/api/v1/accum/group/" + groupId + "/line/" + lineId, cb); }
    public static void getAccumMembers(String groupId, Callback cb) { mirrored("GROUP_MEMBERS", groupId, "/api/v1/accum/group/" + groupId + "/members", cb); }

    public static void getFixedGroup(String groupId, Callback cb) { mirrored("GROUP_STATE", groupId, "/api/v1/fixed/group/" + groupId, cb); }
    public static void getFixedMember(String groupId, String address, Callback cb) { read("/api/v1/fixed/group/" + groupId + "/member/" + address, cb); }
    public static void getFixedMembers(String groupId, Callback cb) { mirrored("GROUP_MEMBERS", groupId, "/api/v1/fixed/group/" + groupId + "/members", cb); }
    public static void getFixedMilestone(String groupId, String id, Callback cb) { read("/api/v1/fixed/group/" + groupId + "/milestone/" + id, cb); }
    public static void getFixedGrant(String groupId, String id, Callback cb) { read("/api/v1/fixed/group/" + groupId + "/grant/" + id, cb); }
    public static void getFixedDisbursementRequest(String groupId, String id, Callback cb) { read("/api/v1/fixed/group/" + groupId + "/disbursement-request/" + id, cb); }

    public static void getLoan(String groupId, String loanId, Callback cb) { read("/api/v1/genloan/group/" + groupId + "/loan/" + loanId, cb); }
    /** What a cure would cost and until when, or why it is not available. */
    public static void getCureOffer(String circleId, String memberAddress, Callback cb) { read("/api/v1/circle/" + circleId + "/cure/" + memberAddress, cb); }
    public static void getForgiveness(String circleId, String memberAddress, Callback cb) { read("/api/v1/forgive-default/" + circleId + "/" + memberAddress, cb); }
    public static void getAdminRecovery(String groupId, Callback cb) { read("/api/v1/admin-recovery/group/" + groupId, cb); }
    /** Open disbursements, milestones and grants. The node list is the source, not a notice. */
    public static void listFixedProposals(String groupId, Callback cb) {
        read("/api/v1/fixed/group/" + groupId + "/proposals?status=OPEN", cb);
    }
    /** Savings lines waiting for the admin. */
    public static void listAccumPendingPacks(String groupId, Callback cb) {
        read("/api/v1/accum/group/" + groupId + "/pending-packs", cb);
    }
    public static void getReferral(String referredAddress, Callback cb) { read("/api/v1/referral/" + referredAddress, cb); }
    /** Live position, including the action nonce. Used before a signed unstake or claim. */
    public static void getStakingPosition(String address, Callback cb) { read("/api/v1/staking/position/" + address, cb); }
    /** Earn screen. The copy if Firestore has it, otherwise the node. */
    public static void readStakingPosition(String address, Callback cb) { mirrored("STAKING", address, "/api/v1/staking/position/" + address, cb); }
    public static void getStakingInfo(Callback cb) { mirrored("STAKING_INFO", "current", "/api/v1/staking/info", cb); }

    /** Live group tier from member labels on the node. Firestore must not invent this. */
    public static void getGroupTrust(String groupId, Callback cb) {
        read("/api/v1/wallet/group-trust/" + groupId, cb);
    }

    /** Family of a group id, from the node's fixed id prefixes. */
    public static String familyOf(String groupId) {
        return com.harvana.app.group.GroupKinds.familyOf(groupId);
    }

    // ═════════════════════════════ CIRCLE ═════════════════════════════

    public static void createCircle(Context c, String creator, long contributionRaw, int memberLimit, long cycleHours, Callback cb) {
        act(c, creator, "/api/v1/circle/create", new Spec() {
            public String canonical(long n) { return GroupCanonical.circleCreate(creator, contributionRaw, memberLimit, cycleHours, n); }
            public void fill(JSONObject b, long n) throws JSONException {
                b.put("creatorAddress", creator).put("contributionAmountRaw", contributionRaw)
                 .put("memberLimit", memberLimit).put("cycleDurationHours", cycleHours);
            }
        }, cb);
    }

    /** termsHash comes from the circle record (GET /circle/{id}); the member signs the same terms the creator set. */
    public static void joinCircle(Context c, String member, String circleId, String termsHash, Callback cb) {
        act(c, member, "/api/v1/circle/join", new Spec() {
            public String canonical(long n) { return GroupCanonical.circleJoin(circleId, member, termsHash, n); }
            public void fill(JSONObject b, long n) throws JSONException {
                b.put("circleId", circleId).put("memberAddress", member).put("termsHash", termsHash);
            }
        }, cb);
    }

    public static void contributeCircle(Context c, String member, String circleId, Callback cb) {
        act(c, member, "/api/v1/circle/contribute", memberOnly(circleId, member, false), cb);
    }

    public static void withdrawCircleBond(Context c, String member, String circleId, Callback cb) {
        act(c, member, "/api/v1/circle/withdraw-bond", memberOnly(circleId, member, true), cb);
    }

    // ═════════════════════════════ ACCUMULATIVE ═════════════════════════════

    public static void createAccumGroup(Context c, String creator, long interestRateBps, String withdrawalRule, Callback cb) {
        act(c, creator, "/api/v1/accum/create-group", new Spec() {
            public String canonical(long n) { return GroupCanonical.accumCreateGroup(creator, interestRateBps, withdrawalRule, n); }
            public void fill(JSONObject b, long n) throws JSONException {
                b.put("creatorAddress", creator).put("interestRateBps", interestRateBps).put("withdrawalRule", withdrawalRule);
            }
        }, cb);
    }

    /** lineType FLEXIBLE|LOCKED (lockedUntilMs required for LOCKED); depositMode ONGOING|SINGLE_LUMP_SUM|VARIABLE_AMOUNT (maxContributionRaw required for VARIABLE_AMOUNT). */
    public static void startAccumLine(Context c, String member, String groupId, long dailyRaw, boolean autoClose,
                                      String lineType, Long lockedUntilMs, String depositMode, Long maxContributionRaw, Callback cb) {
        act(c, member, "/api/v1/accum/start-line", new Spec() {
            public String canonical(long n) {
                return GroupCanonical.accumStartLine(groupId, member, dailyRaw, autoClose, lineType, lockedUntilMs, depositMode, maxContributionRaw, n);
            }
            public void fill(JSONObject b, long n) throws JSONException {
                b.put("groupId", groupId).put("memberAddress", member).put("dailyAmountRaw", dailyRaw)
                 .put("autoCloseEnabled", autoClose).put("lineType", lineType).put("depositMode", depositMode);
                if (lockedUntilMs != null) b.put("lockedUntilTimestamp", lockedUntilMs.longValue());
                if (maxContributionRaw != null) b.put("maxContributionRaw", maxContributionRaw.longValue());
            }
        }, cb);
    }

    public static void contributeAccum(Context c, String member, String groupId, String lineId, Callback cb) {
        act(c, member, "/api/v1/accum/contribute", line(groupId, lineId, member, "contribute", -1), cb);
    }

    public static void contributeAccumMultiple(Context c, String member, String groupId, String lineId, long desiredAmountRaw, Callback cb) {
        act(c, member, "/api/v1/accum/contribute-multiple", line(groupId, lineId, member, "multi", desiredAmountRaw), cb);
    }

    public static void contributeAccumVariable(Context c, String member, String groupId, String lineId, long amountRaw, Callback cb) {
        act(c, member, "/api/v1/accum/contribute-variable", line(groupId, lineId, member, "variable", amountRaw), cb);
    }

    public static void packAccumLine(Context c, String member, String groupId, String lineId, Callback cb) {
        act(c, member, "/api/v1/accum/pack", line(groupId, lineId, member, "pack", -1), cb);
    }

    public static void claimAccumLine(Context c, String member, String groupId, String lineId, Callback cb) {
        act(c, member, "/api/v1/accum/claim", line(groupId, lineId, member, "claim", -1), cb);
    }

    /** Any member proposes a treasurer (or, with an empty address, removing the current one) for the group to vote on. */
    public static void proposeTreasurer(Context c, String proposer, String groupId, String treasurer, Callback cb) {
        act(c, proposer, "/api/v1/group-roles/treasurer-propose", new Spec() {
            public String canonical(long n) { return GroupCanonical.groupTreasurerPropose(groupId, proposer, treasurer, n); }
            public void fill(JSONObject b, long n) throws JSONException {
                b.put("groupId", groupId).put("proposerAddress", proposer).put("treasurerAddress", treasurer);
            }
        }, cb);
    }

    public static void voteTreasurer(Context c, String voter, String groupId, boolean yes, Callback cb) {
        act(c, voter, "/api/v1/group-roles/treasurer-vote", new Spec() {
            public String canonical(long n) { return GroupCanonical.groupTreasurerVote(groupId, voter, yes, n); }
            public void fill(JSONObject b, long n) throws JSONException {
                b.put("groupId", groupId).put("voterAddress", voter).put("yes", yes);
            }
        }, cb);
    }

    public static void getTreasurerProposal(String groupId, Callback cb) { read("/api/v1/group-roles/treasurer-proposal/" + groupId, cb); }

    /** The admin names a member as treasurer, or clears the role with an empty address. */
    public static void setTreasurer(Context c, String admin, String groupId, String treasurer, Callback cb) {
        act(c, admin, "/api/v1/group-roles/treasurer", new Spec() {
            public String canonical(long n) { return GroupCanonical.groupTreasurer(groupId, admin, treasurer, n); }
            public void fill(JSONObject b, long n) throws JSONException {
                b.put("groupId", groupId).put("adminAddress", admin).put("treasurerAddress", treasurer);
            }
        }, cb);
    }

    public static void approveAccumPack(Context c, String admin, String groupId, String lineId, Callback cb) {
        act(c, admin, "/api/v1/accum/approve-pack", new Spec() {
            public String canonical(long n) { return GroupCanonical.accumApprovePack(groupId, lineId, admin, n); }
            public void fill(JSONObject b, long n) throws JSONException {
                b.put("groupId", groupId).put("lineId", lineId).put("adminAddress", admin);
            }
        }, cb);
    }

    public static void accumWalletRecovery(Context c, String acting, String groupId, String oldMember, String newMember, Callback cb) {
        act(c, acting, "/api/v1/accum/wallet-recovery", new Spec() {
            public String canonical(long n) { return GroupCanonical.accumWalletRecovery(groupId, oldMember, newMember, acting, n); }
            public void fill(JSONObject b, long n) throws JSONException {
                b.put("groupId", groupId).put("oldMemberAddress", oldMember).put("newMemberAddress", newMember).put("actingAddress", acting);
            }
        }, cb);
    }

    // ═════════════════════════════ FIXED (goal / project / emergency pools) ═════════════════════════════

    public static void createFixedGroup(Context c, String creator, long targetGoalRaw, String disbursementRule, String beneficiary,
                                        long interestRateBps, String name, String description, Callback cb) {
        act(c, creator, "/api/v1/fixed/create-group", new Spec() {
            public String canonical(long n) {
                return GroupCanonical.fixedCreateGroup(creator, targetGoalRaw, disbursementRule, beneficiary, interestRateBps, name, description, n);
            }
            public void fill(JSONObject b, long n) throws JSONException {
                b.put("creatorAddress", creator).put("targetGoalAmountRaw", targetGoalRaw).put("disbursementRule", disbursementRule)
                 .put("beneficiaryAddress", beneficiary).put("interestRateBps", interestRateBps)
                 .put("groupName", name).put("groupDescription", description);
            }
        }, cb);
    }

    public static void depositFixed(Context c, String member, String groupId, long amountRaw, Callback cb) {
        act(c, member, "/api/v1/fixed/deposit", new Spec() {
            public String canonical(long n) { return GroupCanonical.fixedDeposit(groupId, member, amountRaw, n); }
            public void fill(JSONObject b, long n) throws JSONException {
                b.put("groupId", groupId).put("memberAddress", member).put("amountRaw", amountRaw);
            }
        }, cb);
    }

    public static void requestFixedDisbursement(Context c, String requester, String groupId, Callback cb) {
        act(c, requester, "/api/v1/fixed/request-disbursement", new Spec() {
            public String canonical(long n) { return GroupCanonical.fixedRequestDisbursement(groupId, requester, n); }
            public void fill(JSONObject b, long n) throws JSONException {
                b.put("groupId", groupId).put("requesterAddress", requester);
            }
        }, cb);
    }

    public static void voteFixedDisbursement(Context c, String voter, String groupId, String requestId, boolean yes, Callback cb) {
        act(c, voter, "/api/v1/fixed/vote-disbursement", vote(groupId, "requestId", requestId, voter, yes, 0), cb);
    }

    public static void requestFixedMilestone(Context c, String requester, String groupId, long amountRaw, String description, Callback cb) {
        act(c, requester, "/api/v1/fixed/request-milestone", new Spec() {
            public String canonical(long n) { return GroupCanonical.fixedRequestMilestone(groupId, requester, amountRaw, description, n); }
            public void fill(JSONObject b, long n) throws JSONException {
                b.put("groupId", groupId).put("requesterAddress", requester).put("milestoneAmountRaw", amountRaw).put("milestoneDescription", description);
            }
        }, cb);
    }

    public static void voteFixedMilestone(Context c, String voter, String groupId, String milestoneId, boolean yes, Callback cb) {
        act(c, voter, "/api/v1/fixed/vote-milestone", vote(groupId, "milestoneId", milestoneId, voter, yes, 1), cb);
    }

    public static void requestFixedGrant(Context c, String requester, String groupId, long amountRaw, String reason, Callback cb) {
        act(c, requester, "/api/v1/fixed/request-grant", new Spec() {
            public String canonical(long n) { return GroupCanonical.fixedRequestGrant(groupId, requester, amountRaw, reason, n); }
            public void fill(JSONObject b, long n) throws JSONException {
                b.put("groupId", groupId).put("requesterAddress", requester).put("amountRaw", amountRaw).put("reasonDescription", reason);
            }
        }, cb);
    }

    public static void voteFixedGrant(Context c, String voter, String groupId, String grantId, boolean yes, Callback cb) {
        act(c, voter, "/api/v1/fixed/vote-grant", vote(groupId, "grantId", grantId, voter, yes, 2), cb);
    }

    // ═════════════════════════════ LOANS (inside a group) ═════════════════════════════

    public static void requestLoan(Context c, String borrower, String groupId, long principalRaw, long durationHours, Callback cb) {
        act(c, borrower, "/api/v1/genloan/request", new Spec() {
            public String canonical(long n) { return GroupCanonical.genLoanRequest(groupId, borrower, principalRaw, durationHours, n); }
            public void fill(JSONObject b, long n) throws JSONException {
                b.put("groupId", groupId).put("borrowerAddress", borrower).put("principalRaw", principalRaw).put("durationHours", durationHours);
            }
        }, cb);
    }

    public static void repayLoan(Context c, String borrower, String groupId, String loanId, long amountRaw, Callback cb) {
        act(c, borrower, "/api/v1/genloan/repay", new Spec() {
            public String canonical(long n) { return GroupCanonical.genLoanRepay(groupId, loanId, borrower, amountRaw, n); }
            public void fill(JSONObject b, long n) throws JSONException {
                b.put("groupId", groupId).put("loanId", loanId).put("borrowerAddress", borrower).put("amountRaw", amountRaw);
            }
        }, cb);
    }

    /** A guardian key signs HARVANA_GENLOAN_APPROVE_V1. This is not a wallet action. */
    public static void stageGenLoanGuardian(String groupId, String loanId, String publicKey, String signature, Callback cb) {
        try {
            JSONObject body = new JSONObject();
            body.put("groupId", groupId).put("loanId", loanId);
            body.put("guardianPublicKey", publicKey).put("guardianSignature", signature);
            HarvanaNodeClient.nodePost("/api/v1/genloan/stage-guardian-signature", body.toString(), new HarvanaNodeClient.JsonCallback() {
                @Override public void onSuccess(JSONObject json) { cb.onSuccess(json); }
                @Override public void onError(String error) { cb.onError(error); }
            });
        } catch (JSONException e) { cb.onError(e.getMessage()); }
    }

    // ═════════════════════════════ ADMIN RECOVERY / REFERRAL / STAKING ═════════════════════════════

    public static void requestAdminRecovery(Context c, String requester, String groupId, String newAdmin, Callback cb) {
        act(c, requester, "/api/v1/admin-recovery/request", new Spec() {
            public String canonical(long n) { return GroupCanonical.adminRecovery(groupId, newAdmin, n); }
            public void fill(JSONObject b, long n) throws JSONException { b.put("groupId", groupId).put("newAdminAddress", newAdmin); }
        }, cb);
    }

    public static void voteAdminRecovery(Context c, String voter, String groupId, boolean yes, Callback cb) {
        act(c, voter, "/api/v1/admin-recovery/vote", new Spec() {
            public String canonical(long n) { return GroupCanonical.adminRecoveryVote(groupId, voter, yes, n); }
            public void fill(JSONObject b, long n) throws JSONException { b.put("groupId", groupId).put("voterAddress", voter).put("voteYes", yes); }
        }, cb);
    }

    /** Pays back specific missed days of a savings line; the charge is the daily amount times the dates. */
    public static void backpayAccum(Context c, String member, String groupId, String lineId, java.util.List<String> dates, Callback cb) {
        act(c, member, "/api/v1/accum/backpay", new Spec() {
            public String canonical(long n) { return GroupCanonical.accumBackpay(groupId, lineId, member, dates, n); }
            public void fill(JSONObject b, long n) throws JSONException {
                b.put("groupId", groupId).put("lineId", lineId).put("memberAddress", member).put("dates", new org.json.JSONArray(dates));
            }
        }, cb);
    }

    /** The member who defaulted puts back what the default took from their own bond. The node fixes the amount. */
    public static void cureDefault(Context c, String member, String circleId, Callback cb) {
        act(c, member, "/api/v1/circle/cure-default", new Spec() {
            public String canonical(long n) { return GroupCanonical.cureDefault(circleId, member, n); }
            public void fill(JSONObject b, long n) throws JSONException { b.put("circleId", circleId).put("memberAddress", member); }
        }, cb);
    }

    /** The circle admin asks the group to forgive a member's default. The group's weighted vote decides; no money moves. */
    public static void proposeForgiveness(Context c, String admin, String circleId, String member, Callback cb) {
        act(c, admin, "/api/v1/forgive-default/request", new Spec() {
            public String canonical(long n) { return GroupCanonical.forgiveDefault(circleId, member, admin, n); }
            public void fill(JSONObject b, long n) throws JSONException { b.put("circleId", circleId).put("memberAddress", member).put("proposerAddress", admin); }
        }, cb);
    }

    public static void voteForgiveness(Context c, String voter, String circleId, String member, boolean yes, Callback cb) {
        act(c, voter, "/api/v1/forgive-default/vote", new Spec() {
            public String canonical(long n) { return GroupCanonical.forgiveDefaultVote(circleId, member, voter, yes, n); }
            public void fill(JSONObject b, long n) throws JSONException {
                b.put("circleId", circleId).put("memberAddress", member).put("voterAddress", voter).put("voteYes", yes);
            }
        }, cb);
    }

    /** Signed by the referred wallet here; {@code consentKey} and {@code consentSig} are the referrer\'s own signature, given earlier. */
    public static void recordReferral(Context c, String referred, String referrer, String consentKey, String consentSig, Callback cb) {
        act(c, referred, "/api/v1/referral/record", new Spec() {
            public String canonical(long n) { return GroupCanonical.recordReferral(referred, referrer, n); }
            public void fill(JSONObject b, long n) throws JSONException {
                b.put("referredAddress", referred).put("referrerAddress", referrer)
                 .put("referrerPublicKey", consentKey).put("referrerSignature", consentSig);
            }
        }, cb);
    }

    public static void claimStakingRewards(Context c, String address, Callback cb) {
        withActionNonce(address, (nonce, err) -> {
            if (nonce < 0) { cb.onError(err); return; }
            actNonce(c, address, "/api/v1/staking/claim", nonce, new Spec() {
                public String canonical(long n) { return GroupCanonical.stakingClaim(address, n); }
                public void fill(JSONObject b, long n) throws JSONException { b.put("address", address); }
            }, cb);
        });
    }

    public static void unstake(Context c, String address, String lotId, long amountRaw, Callback cb) {
        withActionNonce(address, (nonce, err) -> {
            if (nonce < 0) { cb.onError(err); return; }
            actNonce(c, address, "/api/v1/staking/unstake", nonce, new Spec() {
                public String canonical(long n) { return GroupCanonical.stakingUnstake(address, lotId, amountRaw, n); }
                public void fill(JSONObject b, long n) throws JSONException {
                    b.put("address", address).put("amountRaw", amountRaw);
                    if (lotId != null && !lotId.isEmpty()) b.put("lotId", lotId);
                }
            }, cb);
        });
    }

    // ═════════════════════════════ plumbing ═════════════════════════════

    private interface Spec {
        String canonical(long nonce);
        void fill(JSONObject body, long nonce) throws JSONException;
    }

    private static Spec memberOnly(String circleId, String member, boolean withdraw) {
        return new Spec() {
            public String canonical(long n) {
                return withdraw ? GroupCanonical.circleWithdraw(circleId, member, n) : GroupCanonical.circleContribute(circleId, member, n);
            }
            public void fill(JSONObject b, long n) throws JSONException { b.put("circleId", circleId).put("memberAddress", member); }
        };
    }

    private static Spec line(String groupId, String lineId, String member, String kind, long amountRaw) {
        return new Spec() {
            public String canonical(long n) {
                switch (kind) {
                    case "multi":    return GroupCanonical.accumContributeMultiple(groupId, lineId, member, amountRaw, n);
                    case "variable": return GroupCanonical.accumContributeVariable(groupId, lineId, member, amountRaw, n);
                    case "pack":     return GroupCanonical.accumPack(groupId, lineId, member, n);
                    case "claim":    return GroupCanonical.accumClaim(groupId, lineId, member, n);
                    default:         return GroupCanonical.accumContribute(groupId, lineId, member, n);
                }
            }
            public void fill(JSONObject b, long n) throws JSONException {
                b.put("groupId", groupId).put("lineId", lineId).put("memberAddress", member);
                if ("multi".equals(kind)) b.put("desiredAmountRaw", amountRaw);
                if ("variable".equals(kind)) b.put("amountRaw", amountRaw);
            }
        };
    }

    private static Spec vote(String groupId, String idKey, String id, String voter, boolean yes, int which) {
        return new Spec() {
            public String canonical(long n) {
                if (which == 0) return GroupCanonical.fixedDisbursementVote(groupId, id, voter, yes, n);
                if (which == 1) return GroupCanonical.fixedMilestoneVote(groupId, id, voter, yes, n);
                return GroupCanonical.fixedGrantVote(groupId, id, voter, yes, n);
            }
            public void fill(JSONObject b, long n) throws JSONException {
                b.put("groupId", groupId).put(idKey, id).put("voterAddress", voter).put("voteYes", yes);
            }
        };
    }

    /**
     * Asks the node itself, skipping the saved copy in Firestore: does it still have this group? {@code answer} gets true when it does,
     * false only on a clear "not found", and null when the node could not be reached (so a bad connection is never reported as a missing group).
     */
    public interface Exists { void answer(Boolean exists); }

    public static void existsOnNode(String family, String groupId, Exists answer) {
        String path = "CIRCLE".equals(family) ? "/api/v1/circle/" + groupId
                : "ACCUM".equals(family) ? "/api/v1/accum/group/" + groupId : "/api/v1/fixed/group/" + groupId;
        read(path, new Callback() {
            @Override public void onSuccess(JSONObject json) { answer.answer(Boolean.TRUE); }
            @Override public void onError(String error) {
                String e = error == null ? "" : error.toUpperCase();
                answer.answer(e.contains("404") || e.contains("NOT_FOUND") ? Boolean.FALSE : null);
            }
        });
    }

    private static void read(String path, Callback cb) {
        HarvanaNodeClient.nodeGet(path, new HarvanaNodeClient.JsonCallback() {
            @Override public void onSuccess(JSONObject json) { cb.onSuccess(json); }
            @Override public void onError(String error) { cb.onError(error); }
        });
    }

    /** Firestore first. A missing document or a rules rejection falls through to the node. */
    private static void mirrored(String collection, String id, String path, Callback cb) {
        if (id == null || id.isEmpty()) { read(path, cb); return; }
        try {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection(collection).document(id).get()
                    .addOnSuccessListener(snap -> {
                        String body = snap != null && snap.exists() ? snap.getString("body") : null;
                        if (body != null && !body.isEmpty()) {
                            try { cb.onSuccess(new JSONObject(body)); return; }
                            catch (JSONException ignored) { }
                        }
                        read(path, cb);
                    })
                    .addOnFailureListener(e -> read(path, cb));
        } catch (Exception e) {
            read(path, cb);
        }
    }

    private interface NonceNext { void ready(long nonce, String error); }

    /** Claim and unstake use the staking action nonce, not the transfer nonce. */
    private static void withActionNonce(String address, NonceNext next) {
        getStakingPosition(address, new Callback() {
            @Override public void onSuccess(JSONObject json) { next.ready(json.optLong("actionNonce", 0) + 1, null); }
            @Override public void onError(String error) { next.ready(-1L, error); }
        });
    }

    private static void actNonce(Context context, String address, String path, long nonce, Spec spec, Callback cb0) {
        final Callback cb = wrapPayment(path, address, cb0);
        try {
            String seedB64 = WalletManager.loadPrivateKey(context, address);
            String pubB64 = WalletManager.getPublicKey(context, address);
            if (seedB64 == null || pubB64 == null) {
                cb.onError("No local signing key for this wallet.");
                return;
            }
            byte[] sig = Ed25519.sign(Base64.decode(seedB64, Base64.NO_WRAP),
                    spec.canonical(nonce).getBytes(StandardCharsets.UTF_8));
            JSONObject body = new JSONObject();
            spec.fill(body, nonce);
            body.put("nonce", nonce)
                .put("signature", Base64.encodeToString(sig, Base64.NO_WRAP))
                .put("publicKey", pubB64);
            HarvanaNodeClient.nodePost(path, body.toString(), new HarvanaNodeClient.JsonCallback() {
                @Override public void onSuccess(JSONObject json) { cb.onSuccess(json); }
                @Override public void onError(String error) { cb.onError(error); }
            });
        } catch (Exception e) {
            cb.onError("Signing failed: " + e.getMessage());
        }
    }

    /** Fetch nextRequestNonce → build canonical → sign with the wallet key → POST. */
    private static void act(Context context, String address, String path, Spec spec, Callback cb0) {
        final Callback cb = wrapPayment(path, address, cb0);
        HarvanaNodeClient.nodeGet("/api/v1/wallet/nonce/" + address, new HarvanaNodeClient.JsonCallback() {
            @Override
            public void onSuccess(JSONObject wallet) {
                try {
                    long nonce = wallet.optLong("nextRequestNonce", wallet.optLong("requestNonce", 0) + 1);
                    String seedB64 = WalletManager.loadPrivateKey(context, address);
                    String pubB64 = WalletManager.getPublicKey(context, address);
                    if (seedB64 == null || pubB64 == null) {
                        cb.onError("No local signing key for this wallet.");
                        return;
                    }
                    byte[] sig = Ed25519.sign(Base64.decode(seedB64, Base64.NO_WRAP),
                            spec.canonical(nonce).getBytes(StandardCharsets.UTF_8));
                    JSONObject body = new JSONObject();
                    spec.fill(body, nonce);
                    body.put("nonce", nonce)
                        .put("signature", Base64.encodeToString(sig, Base64.NO_WRAP))
                        .put("publicKey", pubB64);
                    HarvanaNodeClient.nodePost(path, body.toString(), new HarvanaNodeClient.JsonCallback() {
                        @Override public void onSuccess(JSONObject json) { cb.onSuccess(json); }
                        @Override public void onError(String error) { cb.onError(error); }
                    });
                } catch (Exception e) {
                    cb.onError("Signing failed: " + e.getMessage());
                }
            }

            @Override
            public void onError(String error) { cb.onError("Could not fetch request nonce: " + error); }
        });
    }

    // ═════════════════════ payments: locked buttons + success sheet ═════════════════════

    /** What the success sheet calls each money-moving action; anything not listed is not a payment. */
    private static String paymentTitle(String path) {
        switch (path) {
            case "/api/v1/circle/contribute": return "Contribution sent";
            case "/api/v1/circle/join": return "Joined the group";
            case "/api/v1/circle/cure-default": return "Default cured";
            case "/api/v1/accum/contribute":
            case "/api/v1/accum/contribute-multiple":
            case "/api/v1/accum/contribute-variable": return "Savings paid";
            case "/api/v1/accum/backpay": return "Missed days paid back";
            case "/api/v1/fixed/deposit": return "Deposit sent";
            case "/api/v1/genloan/repay": return "Loan repayment sent";
            case "/api/v1/staking/claim": return "Rewards claimed";
            case "/api/v1/staking/unstake": return "Unstake started";
            default: return null;
        }
    }

    /** Locks the payment buttons while this action runs and shows the success sheet when it works. */
    private static Callback wrapPayment(String path, String address, Callback inner) {
        final String title = paymentTitle(path);
        if (title == null) return inner;
        final com.harvana.app.tx.PaymentTracker.Handle handle = com.harvana.app.tx.PaymentTracker.begin();
        return new Callback() {
            @Override public void onSuccess(JSONObject json) {
                try {
                    com.harvana.app.ui.PaymentSuccessSheet.Info info = new com.harvana.app.ui.PaymentSuccessSheet.Info();
                    info.title = title;
                    long raw = json == null ? 0 : json.optLong("amountRaw", json.optLong("totalRaw", 0));
                    if (raw > 0) info.amount = com.harvana.app.wallet.BalanceFormatter.smart(
                            com.harvana.app.wallet.BalanceFormatter.rawToHarv(raw)) + " " + com.harvana.app.CoinInfo.ticker();
                    info.rows = new String[][] {{"From", address}};
                    String hash = json == null ? "" : json.optString("txHash", json.optString("payoutId", ""));
                    info.txHash = hash;
                    info.note = path.contains("/staking/")
                            ? "It is on its way to your wallet."
                            : "The network has accepted it. It shows as confirmed once the next block has it.";
                    com.harvana.app.tx.PaymentTracker.success(info);
                } catch (Throwable ignored) {}
                handle.end();
                inner.onSuccess(json);
            }
            @Override public void onError(String error) {
                handle.end();
                inner.onError(error);
            }
        };
    }
}
