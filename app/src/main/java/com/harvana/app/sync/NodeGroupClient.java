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

    /** Labels the node computes for one wallet (finished circles, on-time payments, vouches). Decoration only. */
    public static void getLabels(String address, Callback cb) {
        read("/api/v1/wallet/labels/" + address, cb);
    }

    /** Family of a group id, from the node's fixed id prefixes. */
    public static String familyOf(String groupId) {
        return com.harvana.app.group.GroupKinds.familyOf(groupId);
    }

    // ═════════════════════════════ HARV-BACKED COINS (vault) ═════════════════════════════

    /** The vault's terms and the live HARV price for one coin (HNGN): ratios, cap, priceOk / priceProblem. */
    public static void vaultInfo(String code, Callback cb) { read("/api/v1/vault/" + code + "/info", cb); }
    /** A wallet's HARV locked, coins owed, ratio, how much more it can mint now, and whether it can be liquidated. */
    public static void vaultPosition(String code, String address, Callback cb) { read("/api/v1/vault/" + code + "/position/" + address, cb); }
    /** Positions under the liquidation line. */
    public static void vaultAtRisk(String code, Callback cb) { read("/api/v1/vault/" + code + "/at-risk", cb); }

    /**
     * LOCK (amountRaw in HARV raw units), MINT, REPAY (coin raw units), WITHDRAW (HARV raw units), or LIQUIDATE (owner
     * = the position being closed; amount 0). Signed as "HARVANA_VAULT_V1:ACTION:CODE:address:amountRaw:owner:nonce".
     */
    public static void vault(Context c, String action, String code, String address, long amountRaw, String owner, Callback cb) {
        final String target = owner == null ? "" : owner;
        act(c, address, "/api/v1/vault/" + code + "/" + action.toLowerCase(java.util.Locale.US), new Spec() {
            public String canonical(long n) {
                return "HARVANA_VAULT_V1:" + action + ":" + code + ":" + address + ":" + amountRaw + ":" + target + ":" + n;
            }
            public void fill(JSONObject b, long n) throws JSONException {
                b.put("address", address).put("amountRaw", amountRaw);
                if (!target.isEmpty()) b.put("owner", target);
            }
        }, cb);
    }

    // ═════════════════════════════ SWAP (HARV ↔ coins, coin ↔ coin) ═════════════════════════════

    /** Every switched-on coin's pool: harvRaw, coinRaw, shares, open, coinRawPerHarv. */
    public static void swapPools(Callback cb) { read("/api/v1/swap/pools", cb); }

    /** The tFare of one movement right now (a normal transfer's), {perMovementRaw, swapFareRaw, …}. */
    public static void transport(Callback cb) { read("/api/v1/swap/transport", cb); }
    /** What {@code amountRaw} of {@code from} gets of {@code to} now: outRaw, priceImpactBps, feeBps, viaHarvRaw. */
    public static void swapQuote(String from, String to, long amountRaw, Callback cb) {
        HarvanaNodeClient.nodeGet("/api/v1/swap/quote?from=" + from + "&to=" + to + "&amountRaw=" + amountRaw, new HarvanaNodeClient.JsonCallback() {
            @Override public void onSuccess(JSONObject json) { cb.onSuccess(json); }
            @Override public void onError(String error) { cb.onError(error); }
        });
    }
    /** The wallet's swaps and pool actions, newest first (status PENDING, DONE, REFUNDED or SENT). */
    public static void swapOrders(String address, Callback cb) { read("/api/v1/swap/orders/" + address + "?limit=30", cb); }
    /** One swap or pool action: amounts, transport fare and every transaction hash. */
    public static void swapOrder(String id, Callback cb) { read("/api/v1/swap/order/" + id, cb); }

    /** The wallet's pool shares and what they hold now. */
    public static void swapPosition(String address, Callback cb) { read("/api/v1/swap/position/" + address, cb); }

    /** Signed as "HARVANA_SWAP_V1:ACTION:a:b:address:x:y:nonce" (SwapController on the node). */
    private static void swapAct(Context c, String path, String action, String a, String b, String address, long x, long y,
                                String[] keys, Callback cb) {
        act(c, address, path, new Spec() {
            public String canonical(long n) { return "HARVANA_SWAP_V1:" + action + ":" + a + ":" + b + ":" + address + ":" + x + ":" + y + ":" + n; }
            public void fill(JSONObject body, long n) throws JSONException {
                body.put("address", address);
                for (int i = 0; i + 1 < keys.length; i += 2) body.put(keys[i], keys[i + 1]);
            }
        }, cb);
    }

    /** Swap {@code amountRaw} of {@code from} ("HARV" or a coin code) for at least {@code minOutRaw} of {@code to}. */
    public static void swap(Context c, String address, String from, String to, long amountRaw, long minOutRaw, Callback cb) {
        swapAct(c, "/api/v1/swap/swap", "SWAP", from, to, address, amountRaw, minOutRaw,
                new String[]{"from", from, "to", to, "amountRaw", String.valueOf(amountRaw), "minOutRaw", String.valueOf(minOutRaw)}, cb);
    }

    /**
     * A Learn Hub gift through the node, which builds both transfers out of this wallet: 95% to the author, 5% to the
     * Harvana treasury, each with its tFare. Signed as "HARVANA_LEARN_GIFT_V1:from:contentId:author:amountRaw:nonce".
     */
    public static void learnGift(Context c, String address, String contentId, String author, long amountRaw, Callback cb) {
        act(c, address, "/api/v1/learn/gift", new Spec() {
            public String canonical(long n) { return "HARVANA_LEARN_GIFT_V1:" + address + ":" + contentId + ":" + author + ":" + amountRaw + ":" + n; }
            public void fill(JSONObject body, long n) throws JSONException {
                body.put("address", address).put("contentId", contentId).put("author", author).put("amountRaw", amountRaw);
            }
        }, cb);
    }

    /** Put HARV and a coin into the coin's pool for shares. */
    public static void swapAdd(Context c, String address, String code, long harvRaw, long coinRaw, Callback cb) {
        swapAct(c, "/api/v1/swap/add", "ADD", code, "HARV", address, harvRaw, coinRaw,
                new String[]{"asset", code, "harvRaw", String.valueOf(harvRaw), "coinRaw", String.valueOf(coinRaw)}, cb);
    }

    /** Turn pool shares back into HARV and the coin. */
    public static void swapRemove(Context c, String address, String code, long shares, Callback cb) {
        swapAct(c, "/api/v1/swap/remove", "REMOVE", code, "HARV", address, shares, 0L,
                new String[]{"asset", code, "shares", String.valueOf(shares)}, cb);
    }

    // ═════════════════════════════ CIRCLE ═════════════════════════════

    /** {@code asset}: the stable coin of the group (HNGN, HUSD, …); every amount is in its units. */
    public static void createCircle(Context c, String creator, long contributionRaw, int memberLimit, long cycleHours, String asset, Callback cb) {
        act(c, creator, "/api/v1/circle/create", new Spec() {
            public String canonical(long n) { return GroupCanonical.circleCreate(creator, contributionRaw, memberLimit, cycleHours, n, asset); }
            public void fill(JSONObject b, long n) throws JSONException {
                b.put("creatorAddress", creator).put("contributionAmountRaw", contributionRaw)
                 .put("memberLimit", memberLimit).put("cycleDurationHours", cycleHours).put("asset", asset);
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

    public static void createAccumGroup(Context c, String creator, long interestRateBps, String withdrawalRule, String asset, Callback cb) {
        act(c, creator, "/api/v1/accum/create-group", new Spec() {
            public String canonical(long n) { return GroupCanonical.accumCreateGroup(creator, interestRateBps, withdrawalRule, n, asset); }
            public void fill(JSONObject b, long n) throws JSONException {
                b.put("creatorAddress", creator).put("interestRateBps", interestRateBps).put("withdrawalRule", withdrawalRule).put("asset", asset);
            }
        }, cb);
    }

    /** lineType FLEXIBLE|LOCKED (lockedUntilMs required for LOCKED); depositMode ONGOING|SINGLE_LUMP_SUM|VARIABLE_AMOUNT (maxContributionRaw required for VARIABLE_AMOUNT). */
    public static void startAccumLine(Context c, String member, String groupId, long dailyRaw, boolean autoClose,
                                      String lineType, Long lockedUntilMs, String depositMode, Long maxContributionRaw, Callback cb) {
        startAccumLine(c, member, groupId, dailyRaw, autoClose, lineType, lockedUntilMs, depositMode, maxContributionRaw, null, cb);
    }

    /** {@code asset}: save in a stable coin (HNGN, HUSD, …) instead of HARV; amounts are then in that coin's units. */
    public static void startAccumLine(Context c, String member, String groupId, long dailyRaw, boolean autoClose,
                                      String lineType, Long lockedUntilMs, String depositMode, Long maxContributionRaw, String asset, Callback cb) {
        act(c, member, "/api/v1/accum/start-line", new Spec() {
            public String canonical(long n) {
                return GroupCanonical.accumStartLine(groupId, member, dailyRaw, autoClose, lineType, lockedUntilMs, depositMode, maxContributionRaw, n, asset);
            }
            public void fill(JSONObject b, long n) throws JSONException {
                b.put("groupId", groupId).put("memberAddress", member).put("dailyAmountRaw", dailyRaw)
                 .put("autoCloseEnabled", autoClose).put("lineType", lineType).put("depositMode", depositMode);
                if (asset != null && !asset.isEmpty()) b.put("asset", asset);
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

    /** The creator changes the stable coin a group is kept in (only while nothing is in the group). */
    public static void changeGroupCoin(Context c, String creator, String groupId, String asset, long amountRaw, Callback cb) {
        act(c, creator, "/api/v1/group-roles/coin", new Spec() {
            public String canonical(long n) { return GroupCanonical.groupCoin(groupId, creator, asset, amountRaw, n); }
            public void fill(JSONObject b, long n) throws JSONException {
                b.put("groupId", groupId).put("address", creator).put("asset", asset).put("amountRaw", amountRaw);
            }
        }, cb);
    }

    /** The member chooses which of their own wallets this group pays them into (payouts, a claim, a bond back). */
    public static void setPayoutWallet(Context c, String member, String groupId, String payoutWallet, Callback cb) {
        act(c, member, "/api/v1/group-roles/payout-wallet", new Spec() {
            public String canonical(long n) { return com.harvana.app.chain.GroupCanonical.payoutWallet(groupId, member, payoutWallet, n); }
            public void fill(JSONObject b, long n) throws JSONException {
                b.put("groupId", groupId).put("address", member).put("payoutWallet", payoutWallet);
            }
        }, cb);
    }

    /** Where this group pays the member now: {memberWallet, payoutWallet}. */
    public static void getPayoutWallet(String groupId, String address, Callback cb) {
        read("/api/v1/group-roles/payout-wallet/" + groupId + "/" + address, cb);
    }

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
                                        long interestRateBps, String name, String description, String asset, Callback cb) {
        act(c, creator, "/api/v1/fixed/create-group", new Spec() {
            public String canonical(long n) {
                return GroupCanonical.fixedCreateGroup(creator, targetGoalRaw, disbursementRule, beneficiary, interestRateBps, name, description, n, asset);
            }
            public void fill(JSONObject b, long n) throws JSONException {
                b.put("creatorAddress", creator).put("targetGoalAmountRaw", targetGoalRaw).put("disbursementRule", disbursementRule)
                 .put("beneficiaryAddress", beneficiary).put("interestRateBps", interestRateBps)
                 .put("groupName", name).put("groupDescription", description).put("asset", asset);
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

    /** Creator only, while nobody else has joined: closes the goal group and sends its wallet back to the creator. */
    public static void closeFixedGroup(Context c, String admin, String groupId, Callback cb) {
        act(c, admin, "/api/v1/fixed/close", new Spec() {
            public String canonical(long n) { return GroupCanonical.fixedClose(groupId, admin, n); }
            public void fill(JSONObject b, long n) throws JSONException { b.put("groupId", groupId).put("adminAddress", admin); }
        }, cb);
    }

    /** Creator only: starts the next goal inside the same group (same members, rule and wallet, new ledger). */
    public static void startNextFixedGoal(Context c, String admin, String groupId, long targetRaw, String goalName, Callback cb) {
        act(c, admin, "/api/v1/fixed/new-goal", new Spec() {
            public String canonical(long n) { return GroupCanonical.fixedNewGoal(groupId, admin, targetRaw, goalName, n); }
            public void fill(JSONObject b, long n) throws JSONException {
                b.put("groupId", groupId).put("adminAddress", admin).put("targetGoalAmountRaw", targetRaw).put("goalName", goalName == null ? "" : goalName);
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

    /**
     * Returning everyone's money from a goal group: PROPOSE (opens a vote, the proposer votes yes), VOTE, or FINISH
     * (settles a vote that is decided, e.g. after seven days with no "no"). Refunds go back in proportion to what each
     * member put in.
     */
    public static void proposeFixedDissolve(Context c, String address, String groupId, Callback cb) {
        act(c, address, "/api/v1/fixed/propose-dissolve", dissolve(groupId, address, "PROPOSE", true), cb);
    }

    public static void voteFixedDissolve(Context c, String address, String groupId, boolean yes, Callback cb) {
        act(c, address, "/api/v1/fixed/vote-dissolve", dissolve(groupId, address, "VOTE", yes), cb);
    }

    public static void finishFixedDissolve(Context c, String address, String groupId, Callback cb) {
        act(c, address, "/api/v1/fixed/finish-dissolve", dissolve(groupId, address, "FINISH", false), cb);
    }

    /** {"status":"NONE"} when nobody has asked; otherwise PENDING, PASSED or REJECTED with weights, votes and decidesBy. */
    public static void getFixedDissolve(String groupId, Callback cb) { read("/api/v1/fixed/" + groupId + "/dissolve", cb); }

    private static Spec dissolve(String groupId, String address, String action, boolean yes) {
        return new Spec() {
            public String canonical(long n) { return GroupCanonical.fixedDissolve(action, groupId, address, yes, n); }
            public void fill(JSONObject b, long n) throws JSONException {
                b.put("groupId", groupId).put("address", address).put("action", action).put("voteYes", yes);
            }
        };
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
        } catch (JSONException e) { cb.onError("The app couldn't prepare this request. Try again."); }
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

    /** Claims a HARV staker's share of one stable coin's fees ("HUSD", "HNGN", …). Like every claim it pays the
     *  HARV network fee first (ACTION_FEE:STABLE_SHARE_CLAIM:<nonce>). */
    public static void claimStableShare(Context c, String code, String address, Callback cb) {
        String path = ("HUSD".equals(code) ? "/api/v1/husd" : "/api/v1/stable/" + code) + "/staker-share/claim";
        withActionNonce(address, (nonce, err) -> {
            if (nonce < 0) { cb.onError(err); return; }
            actNonce(c, address, path, nonce, new Spec() {
                public String canonical(long n) { return "STABLE_SHARE_CLAIM:" + code + ":" + address + ":" + n; }
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
                answer.answer(com.harvana.app.utils.ErrorText.is(e, "404") || com.harvana.app.utils.ErrorText.is(e, "NOT_FOUND") ? Boolean.FALSE : null);
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
        payActionFee(context, address, path, nonce, feeHash -> {
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
                if (feeHash != null) body.put("feeTxHash", feeHash);
                HarvanaNodeClient.nodePost(path, body.toString(), new HarvanaNodeClient.JsonCallback() {
                    @Override public void onSuccess(JSONObject json) { cb.onSuccess(json); }
                    @Override public void onError(String error) { cb.onError(error); }
                });
            } catch (Exception e) {
                cb.onError("Your wallet couldn't sign this request. Try again.");
            }
        }, cb);
    }

    /** Fetch nextRequestNonce → build canonical → sign with the wallet key → POST. */
    private static void act(Context context, String address, String path, Spec spec, Callback cb0) {
        final Callback cb = wrapPayment(path, address, cb0);
        HarvanaNodeClient.nodeGet("/api/v1/wallet/nonce/" + address, new HarvanaNodeClient.JsonCallback() {
            @Override
            public void onSuccess(JSONObject wallet) {
                long nonce = wallet.optLong("nextRequestNonce", wallet.optLong("requestNonce", 0) + 1);
                payActionFee(context, address, path, nonce, feeHash -> {
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
                        if (feeHash != null) body.put("feeTxHash", feeHash);
                        HarvanaNodeClient.nodePost(path, body.toString(), new HarvanaNodeClient.JsonCallback() {
                            @Override public void onSuccess(JSONObject json) { cb.onSuccess(json); }
                            @Override public void onError(String error) { cb.onError(error); }
                        });
                    } catch (Exception e) {
                        cb.onError("Your wallet couldn't sign this request. Try again.");
                    }
                }, cb);
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
            case "/api/v1/group-roles/coin": return "Changed the group coin";
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
                    com.harvana.app.tx.PaymentTracker.Review review = com.harvana.app.tx.PaymentTracker.takeReview();
                    if (raw > 0) info.amount = com.harvana.app.wallet.BalanceFormatter.smartDecimal(
                            com.harvana.app.wallet.BalanceFormatter.rawToHarv(raw)) + " " + com.harvana.app.CoinInfo.ticker();
                    else if (review != null && !review.amount.isEmpty()) info.amount = review.amount;   // the network's answer has no amount: use what was approved
                    java.util.ArrayList<String[]> rows = new java.util.ArrayList<>();
                    boolean hasFrom = false;
                    if (review != null) for (String[] r : review.rows) {
                        if (r == null || r.length < 2 || r[0] == null) continue;
                        String label = r[0].trim();
                        if ("tFare".equalsIgnoreCase(label)) continue;          // the fee is its own line on the receipt
                        if ("From".equalsIgnoreCase(label)) { hasFrom = true; rows.add(new String[]{"From", address}); continue; }
                        rows.add(r);
                    }
                    if (!hasFrom) rows.add(new String[]{"From", address});
                    info.rows = rows.toArray(new String[0][]);
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

    // ═════════════════════ no action is free: network fee from the user's wallet ═════════════════════

    private interface FeeNext { void go(String feeTxHash); }

    /** The actions that pay HARV out of a keyless wallet to the person asking. The node refuses them without a fee. */
    private static String feeKind(String path) {
        switch (path) {
            case "/api/v1/staking/claim": return "STAKING_CLAIM";
            case "/api/v1/staking/unstake": return "STAKING_UNSTAKE";
            case "/api/v1/accum/claim": return "ACCUM_CLAIM";
            case "/api/v1/circle/withdraw-bond": return "CIRCLE_WITHDRAW";
            case "/api/v1/fixed/request-disbursement": return "FIXED_DISBURSE";
            case "/api/v1/fixed/new-goal": return "FIXED_NEW_GOAL";
            case "/api/v1/fixed/close": return "FIXED_CLOSE";
            case "/api/v1/fixed/request-milestone": return "FIXED_REQUEST_MILESTONE";
            case "/api/v1/fixed/request-grant": return "FIXED_REQUEST_GRANT";
            case "/api/v1/fixed/vote-disbursement": return "FIXED_VOTE_DISBURSEMENT";
            case "/api/v1/fixed/vote-milestone": return "FIXED_VOTE_MILESTONE";
            case "/api/v1/fixed/vote-grant": return "FIXED_VOTE_GRANT";
            case "/api/v1/fixed/propose-dissolve": return "FIXED_DISSOLVE";
            case "/api/v1/fixed/vote-dissolve": return "FIXED_VOTE_DISSOLVE";
            case "/api/v1/fixed/finish-dissolve": return "FIXED_FINISH_DISSOLVE";
            case "/api/v1/genloan/request": return "GENLOAN_REQUEST";
            default:
                if (path.endsWith("/staker-share/claim")) return "STABLE_SHARE_CLAIM";
                if (path.startsWith("/api/v1/vault/")) {           // lock pays its own gas; the rest pay the action fee
                    String act = path.substring(path.lastIndexOf('/') + 1);
                    switch (act) {
                        case "mint": return "VAULT_MINT";
                        case "repay": return "VAULT_REPAY";
                        case "withdraw": return "VAULT_WITHDRAW";
                        case "liquidate": return "VAULT_LIQUIDATE";
                        default: return null;
                    }
                }
                return null;
        }
    }

    /**
     * Pays the network fee first: a normal signed transfer to yourself carrying ACTION_FEE:kind:nonce, which charges
     * gas like any transfer (so the wallet must hold HARV). Its hash goes into the request as feeTxHash.
     */
    private static void payActionFee(Context context, String address, String path, long nonce, FeeNext next, Callback cb) {
        final String kind = feeKind(path);
        if (kind == null) { next.go(null); return; }
        com.harvana.app.tx.TransactionRouter.routeTransaction(context, address, address,
                new java.math.BigDecimal("0.00000001"), null, "ACTION_FEE:" + kind + ":" + nonce,
                new com.harvana.app.tx.TransactionRouter.RouterCallback() {
                    @Override public void onSuccess(String txHash) { next.go(txHash); }
                    @Override public void onError(String code, String msg) {
                        cb.onError("tFare not paid. Every action needs a small fee in HARV from your wallet. " + (msg == null ? "" : msg));
                    }
                });
    }
}
