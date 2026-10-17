package com.harvana.app.chain;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;

import com.harvana.app.crypto.Ed25519;
import com.harvana.app.sync.HarvanaNodeClient;
import com.harvana.app.wallet.SecureWalletStorage;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

/**
 * Signed-call client for the node's group endpoints (Accum, Fixed, GenLoan,
 * Circle, Staking, Identity). This is the foundation for closing G1, G2, G3
 * from {@code docs/CROSS_REPO_AUDIT.md} — the canonical messages already
 * live in {@link GroupCanonical}, the v2 byte layout in {@link LockV2}, and
 * the node has the endpoints. Until this class existed, nothing in the
 * wallet actually called them; the app used Firestore for Accum, Fixed and
 * GenLoan. Circle is wired through {@link com.harvana.app.group.NodePayActions}
 * (kept for backwards compatibility — both paths share this class's
 * sign-and-submit helpers).
 *
 * <h2>Sign-and-submit pattern</h2>
 * Every method follows the same shape as
 * {@link com.harvana.app.tx.P2PTradeRouter#createListing}:
 * <ol>
 *   <li>Fetch the wallet's current {@code nextRequestNonce} from the node.</li>
 *   <li>Build the canonical message via {@link GroupCanonical} (v1 today;
 *       v2 when the node flips {@code harvana.consensus.v2-lock-activation-height}).</li>
 *   <li>Sign with the wallet's Ed25519 key
 *       ({@link com.harvana.app.crypto.Ed25519}).</li>
 *   <li>POST to the node's endpoint with the signature, public key, and
 *       canonical fields.</li>
 *   <li>On success, deliver the response JSON to the caller on the main thread.</li>
 * </ol>
 *
 * <h2>Endpoints</h2>
 * The node paths this class calls. Each method's javadoc cites the node
 * model class and the canonical-message format. All paths are
 * {@code POST /api/v1/{family}/{action}} with a JSON body
 * {@code {signature, publicKey, nonce, ...fields}} except where noted.
 *
 * <ul>
 *   <li>Accum:  /accum/create-group, /accum/start-line, /accum/contribute,
 *               /accum/contribute-multiple, /accum/contribute-variable,
 *               /accum/claim, /accum/pack, /accum/approve-pack,
 *               /accum/wallet-recovery</li>
 *   <li>Fixed:  /fixed/create-group, /fixed/deposit,
 *               /fixed/request-disbursement, /fixed/disbursement-vote,
 *               /fixed/request-milestone, /fixed/milestone-vote,
 *               /fixed/request-grant, /fixed/grant-vote, /fixed/close,
 *               /fixed/new-goal, /fixed/dissolve-vote</li>
 *   <li>GenLoan: /genloan/request, /genloan/repay, /genloan/guardian-signature</li>
 *   <li>Staking: /staking/delegate, /staking/undelegate, /staking/claim</li>
 *   <li>Identity: /identity/link, /identity/resolve, /identity/recover</li>
 * </ul>
 *
 * <h2>Threading</h2>
 * All network calls run on a background executor; every callback fires on
 * the main thread. Never call from a UI thread that needs to block on the
 * response — the wallet's UI is already async.
 */
public final class NodeGroupClient {

    private NodeGroupClient() {}

    /**
     * Returns true when the node's v2 lock signing is active. The wallet reads
     * {@code /api/v1/node/status} once at app start (or on first group action)
     * and caches the activation height here. Until the node flips
     * {@code harvana.consensus.v2-lock-activation-height} away from -1, this
     * returns false and the wallet uses v1 canonical strings.
     *
     * When v2 is active, the wallet will build the typed-tx payload via
     * {@link LockV2#signingBytes} and call the v2 endpoint family the node
     * exposes for that purpose. The switch is centralised here so the UI
     * never has to know which signing format is live.
     */
    public static volatile boolean v2SigningActive = false;

    /**
     * Called once on app start (or first group action) to read the node's
     * current activation height. The caller passes the value verbatim; the
     * class compares against zero and sets the volatile flag.
     *
     * @param v2LockActivationHeight the node's reported value; -1 or any
     *                               negative value means v1 is live.
     */
    public static void onNodeStatus(long v2LockActivationHeight) {
        v2SigningActive = v2LockActivationHeight >= 0L;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Accum  (savings lines inside a group)
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Create an Accum group. Node: {@code AccumCreateGroupRequest.getCanonicalMessage()}.
     * Canonical: {@code HARVANA_ACCUM_CREATE_GROUP_V1:creator:interestRateBps:withdrawalRule:nonce[:asset]}.
     */
    public static void accumCreateGroup(android.content.Context ctx, String creatorAddress,
                                        long interestRateBps, String withdrawalRule,
                                        String asset, SignCallback cb) {
        _signed(ctx, creatorAddress, fields -> GroupCanonical.fromFields("accumCreateGroup", fields),
                new Object[]{creatorAddress, interestRateBps, withdrawalRule, 0L, asset},
                "/api/v1/accum/create-group",
                _json(creatorAddress, interestRateBps, withdrawalRule, asset, null, null, null, null, null, null, null, null, null, null, null),
                cb);
    }

    /**
     * Open a savings line in an existing Accum group. Node: {@code POST /api/v1/accum/start-line}.
     * Canonical: {@code HARVANA_ACCUM_START_LINE_V1:groupId:member:dailyAmountRaw:autoCloseEnabled:lineType:lockedUntilTimestamp:depositMode:maxContributionRaw:nonce[:asset]}.
     * Built by {@link GroupCanonical#accumStartLine} and signed with the
     * wallet's Ed25519 key.
     */
    public static void accumStartLine(android.content.Context ctx, String memberAddress,
                                      String groupId, long dailyAmountRaw, boolean autoCloseEnabled,
                                      String lineType, Long lockedUntilTs,
                                      String depositMode, Long maxContributionRaw,
                                      String asset, SignCallback cb) {
        _signed(ctx, memberAddress, fields -> GroupCanonical.fromFields("accumStartLine", fields),
                new Object[]{groupId, memberAddress, dailyAmountRaw, autoCloseEnabled,
                        lineType, lockedUntilTs, depositMode, maxContributionRaw, 0L, asset},
                "/api/v1/accum/start-line",
                _json(memberAddress, null, null, asset, groupId, dailyAmountRaw, lineType,
                        lockedUntilTs, depositMode, maxContributionRaw, null, null, null, null, null),
                cb);
    }

    /** Accum contribute. Canonical: {@code HARVANA_ACCUM_CONTRIBUTE_V1:groupId:lineId:member:nonce}. */
    public static void accumContribute(android.content.Context ctx, String memberAddress,
                                       String groupId, String lineId, SignCallback cb) {
        _signed(ctx, memberAddress, fields -> GroupCanonical.fromFields("accumContribute", fields),
                new Object[]{groupId, lineId, memberAddress, 0L},
                "/api/v1/accum/contribute",
                _json(memberAddress, null, null, null, groupId, 0L, null, 0L, null, 0L, lineId, null, null, null, null),
                cb);
    }

    /** Accum contribute-multiple (catch-up). Canonical: {@code HARVANA_ACCUM_CONTRIBUTE_MULTI_V1:groupId:lineId:member:desiredAmount:nonce}. */
    public static void accumContributeMultiple(android.content.Context ctx, String memberAddress,
                                               String groupId, String lineId, long desiredAmountRaw,
                                               SignCallback cb) {
        _signed(ctx, memberAddress, fields -> GroupCanonical.fromFields("accumContributeMultiple", fields),
                new Object[]{groupId, lineId, memberAddress, desiredAmountRaw, 0L},
                "/api/v1/accum/contribute-multiple",
                _json(memberAddress, null, null, null, groupId, 0L, null, 0L, null, 0L, lineId, desiredAmountRaw, null, null, null),
                cb);
    }

    /** Accum contribute-variable. Canonical: {@code HARVANA_ACCUM_CONTRIBUTE_VARIABLE_V1:groupId:lineId:member:amount:nonce}. */
    public static void accumContributeVariable(android.content.Context ctx, String memberAddress,
                                               String groupId, String lineId, long amountRaw,
                                               SignCallback cb) {
        _signed(ctx, memberAddress, fields -> GroupCanonical.fromFields("accumContributeVariable", fields),
                new Object[]{groupId, lineId, memberAddress, amountRaw, 0L},
                "/api/v1/accum/contribute-variable",
                _json(memberAddress, null, null, null, groupId, 0L, null, 0L, null, 0L, lineId, amountRaw, null, null, null),
                cb);
    }

    /** Accum claim (mature line). Canonical: {@code HARVANA_ACCUM_CLAIM_V1:groupId:lineId:member:nonce}. */
    public static void accumClaim(android.content.Context ctx, String memberAddress,
                                 String groupId, String lineId, SignCallback cb) {
        _signed(ctx, memberAddress, fields -> GroupCanonical.fromFields("accumClaim", fields),
                new Object[]{groupId, lineId, memberAddress, 0L},
                "/api/v1/accum/claim",
                _json(memberAddress, null, null, null, groupId, 0L, null, 0L, null, 0L, lineId, null, null, null, null),
                cb);
    }

    /** Accum pack (early close). Canonical: {@code HARVANA_ACCUM_PACK_V1:groupId:lineId:member:nonce}. */
    public static void accumPack(android.content.Context ctx, String memberAddress,
                                 String groupId, String lineId, SignCallback cb) {
        _signed(ctx, memberAddress, fields -> GroupCanonical.fromFields("accumPack", fields),
                new Object[]{groupId, lineId, memberAddress, 0L},
                "/api/v1/accum/pack",
                _json(memberAddress, null, null, null, groupId, 0L, null, 0L, null, 0L, lineId, null, null, null, null),
                cb);
    }

    /** Accum approve-pack (admin). Canonical: {@code HARVANA_ACCUM_APPROVE_PACK_V1:groupId:lineId:admin:nonce}. */
    public static void accumApprovePack(android.content.Context ctx, String adminAddress,
                                        String groupId, String lineId, SignCallback cb) {
        _signed(ctx, adminAddress, fields -> GroupCanonical.fromFields("accumApprovePack", fields),
                new Object[]{groupId, lineId, adminAddress, 0L},
                "/api/v1/accum/approve-pack",
                _json(adminAddress, null, null, null, groupId, 0L, null, 0L, null, 0L, lineId, null, null, null, null),
                cb);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Fixed  (goal / project / emergency pools)
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Create a Fixed group. Canonical:
     * {@code HARVANA_FIXED_CREATE_GROUP_V1:creator:targetGoal:disbursementRule:beneficiary:interestRateBps:name:description:nonce[:asset]}.
     */
    public static void fixedCreateGroup(android.content.Context ctx, String creatorAddress,
                                        long targetGoalAmountRaw, String disbursementRule,
                                        String beneficiaryAddress, long interestRateBps,
                                        String groupName, String groupDescription, String asset,
                                        SignCallback cb) {
        _signed(ctx, creatorAddress, fields -> GroupCanonical.fromFields("fixedCreateGroup", fields),
                new Object[]{creatorAddress, targetGoalAmountRaw, disbursementRule,
                        beneficiaryAddress, interestRateBps, groupName, groupDescription, 0L, asset},
                "/api/v1/fixed/create-group",
                _json(creatorAddress, interestRateBps, disbursementRule, asset, null,
                        targetGoalAmountRaw, null, 0L, null, 0L, null, null, beneficiaryAddress, groupName, groupDescription),
                cb);
    }

    /** Fixed deposit. Canonical: {@code HARVANA_FIXED_DEPOSIT_V1:groupId:member:amount:nonce}. */
    public static void fixedDeposit(android.content.Context ctx, String memberAddress,
                                    String groupId, long amountRaw, SignCallback cb) {
        _signed(ctx, memberAddress, fields -> GroupCanonical.fromFields("fixedDeposit", fields),
                new Object[]{groupId, memberAddress, amountRaw, 0L},
                "/api/v1/fixed/deposit",
                _json(memberAddress, null, null, null, groupId, amountRaw, null, 0L, null, 0L, null, null, null, null, null),
                cb);
    }

    /** Fixed request disbursement. Canonical: {@code HARVANA_FIXED_REQUEST_DISBURSEMENT_V1:groupId:requester:nonce}. */
    public static void fixedRequestDisbursement(android.content.Context ctx, String requesterAddress,
                                                String groupId, SignCallback cb) {
        _signed(ctx, requesterAddress, fields -> GroupCanonical.fromFields("fixedRequestDisbursement", fields),
                new Object[]{groupId, requesterAddress, 0L},
                "/api/v1/fixed/request-disbursement",
                _json(requesterAddress, null, null, null, groupId, 0L, null, 0L, null, 0L, null, null, null, null, null),
                cb);
    }

    /** Fixed disbursement vote. Canonical: {@code HARVANA_FIXED_DISBURSEMENT_VOTE_V1:groupId:requestId:voter:voteYes:nonce}. */
    public static void fixedDisbursementVote(android.content.Context ctx, String voterAddress,
                                             String groupId, String requestId, boolean voteYes,
                                             SignCallback cb) {
        _signed(ctx, voterAddress, fields -> GroupCanonical.fromFields("fixedDisbursementVote", fields),
                new Object[]{groupId, requestId, voterAddress, voteYes, 0L},
                "/api/v1/fixed/disbursement-vote",
                _json(voterAddress, null, null, null, groupId, 0L, null, 0L, null, 0L, null, null, null, null, null),
                cb);
    }

    /** Fixed request milestone. Canonical: {@code HARVANA_FIXED_REQUEST_MILESTONE_V1:groupId:requester:amount:description:nonce}. */
    public static void fixedRequestMilestone(android.content.Context ctx, String requesterAddress,
                                              String groupId, long milestoneAmountRaw,
                                              String milestoneDescription, SignCallback cb) {
        _signed(ctx, requesterAddress, fields -> GroupCanonical.fromFields("fixedRequestMilestone", fields),
                new Object[]{groupId, requesterAddress, milestoneAmountRaw, milestoneDescription, 0L},
                "/api/v1/fixed/request-milestone",
                _json(requesterAddress, null, null, null, groupId, 0L, null, 0L, null, 0L, null, milestoneAmountRaw, null, null, milestoneDescription),
                cb);
    }

    /** Fixed milestone vote. Canonical: {@code HARVANA_FIXED_MILESTONE_VOTE_V1:groupId:milestoneId:voter:voteYes:nonce}. */
    public static void fixedMilestoneVote(android.content.Context ctx, String voterAddress,
                                         String groupId, String milestoneId, boolean voteYes,
                                         SignCallback cb) {
        _signed(ctx, voterAddress, fields -> GroupCanonical.fromFields("fixedMilestoneVote", fields),
                new Object[]{groupId, milestoneId, voterAddress, voteYes, 0L},
                "/api/v1/fixed/milestone-vote",
                _json(voterAddress, null, null, null, groupId, 0L, null, 0L, null, 0L, null, null, null, null, null),
                cb);
    }

    /** Fixed request grant (emergency). Canonical: {@code HARVANA_FIXED_REQUEST_GRANT_V1:groupId:requester:amount:reason:nonce}. */
    public static void fixedRequestGrant(android.content.Context ctx, String requesterAddress,
                                         String groupId, long amountRaw, String reasonDescription,
                                         SignCallback cb) {
        _signed(ctx, requesterAddress, fields -> GroupCanonical.fromFields("fixedRequestGrant", fields),
                new Object[]{groupId, requesterAddress, amountRaw, reasonDescription, 0L},
                "/api/v1/fixed/request-grant",
                _json(requesterAddress, null, null, null, groupId, 0L, null, 0L, null, 0L, null, amountRaw, null, null, reasonDescription),
                cb);
    }

    /** Fixed grant vote. Canonical: {@code HARVANA_FIXED_GRANT_VOTE_V1:groupId:grantId:voter:voteYes:nonce}. */
    public static void fixedGrantVote(android.content.Context ctx, String voterAddress,
                                     String groupId, String grantId, boolean voteYes, SignCallback cb) {
        _signed(ctx, voterAddress, fields -> GroupCanonical.fromFields("fixedGrantVote", fields),
                new Object[]{groupId, grantId, voterAddress, voteYes, 0L},
                "/api/v1/fixed/grant-vote",
                _json(voterAddress, null, null, null, groupId, 0L, null, 0L, null, 0L, null, null, null, null, null),
                cb);
    }

    /** Fixed close. Canonical: {@code HARVANA_FIXED_CLOSE_V1:groupId:admin:nonce}. */
    public static void fixedClose(android.content.Context ctx, String adminAddress,
                                  String groupId, SignCallback cb) {
        _signed(ctx, adminAddress, fields -> GroupCanonical.fromFields("fixedClose", fields),
                new Object[]{groupId, adminAddress, 0L},
                "/api/v1/fixed/close",
                _json(adminAddress, null, null, null, groupId, 0L, null, 0L, null, 0L, null, null, null, null, null),
                cb);
    }

    /** Fixed new goal. Canonical: {@code HARVANA_FIXED_NEW_GOAL_V1:groupId:admin:target:goalName:nonce}. */
    public static void fixedNewGoal(android.content.Context ctx, String adminAddress,
                                    String groupId, long targetGoalAmountRaw, String goalName,
                                    SignCallback cb) {
        _signed(ctx, adminAddress, fields -> GroupCanonical.fromFields("fixedNewGoal", fields),
                new Object[]{groupId, adminAddress, targetGoalAmountRaw, goalName, 0L},
                "/api/v1/fixed/new-goal",
                _json(adminAddress, null, null, null, groupId, targetGoalAmountRaw, null, 0L, null, 0L, null, null, null, goalName, null),
                cb);
    }

    /** Fixed dissolve vote. Canonical: {@code HARVANA_FIXED_DISSOLVE_V1:action:groupId:address:voteYes:nonce}. */
    public static void fixedDissolveVote(android.content.Context ctx, String voterAddress,
                                         String action, String groupId, boolean voteYes,
                                         SignCallback cb) {
        _signed(ctx, voterAddress, fields -> GroupCanonical.fromFields("fixedDissolve", fields),
                new Object[]{action, groupId, voterAddress, voteYes, 0L},
                "/api/v1/fixed/dissolve-vote",
                _json(voterAddress, null, null, null, groupId, 0L, null, 0L, null, 0L, null, null, null, null, null),
                cb);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  GenLoan  (borrow / repay / guardian signature)
    // ════════════════════════════════════════════════════════════════════════

    /** GenLoan request. Canonical: {@code HARVANA_GENLOAN_REQUEST_V1:groupId:borrower:principal:durationHours:nonce}. */
    public static void genLoanRequest(android.content.Context ctx, String borrowerAddress,
                                      String groupId, long principalRaw, long durationHours,
                                      SignCallback cb) {
        _signed(ctx, borrowerAddress, fields -> GroupCanonical.fromFields("genLoanRequest", fields),
                new Object[]{groupId, borrowerAddress, principalRaw, durationHours, 0L},
                "/api/v1/genloan/request",
                _json(borrowerAddress, null, null, null, groupId, principalRaw, null, 0L, null, 0L, null, null, null, null, null),
                cb);
    }

    /** GenLoan repay. Canonical: {@code HARVANA_GENLOAN_REPAY_V1:groupId:loanId:borrower:amount:nonce}. */
    public static void genLoanRepay(android.content.Context ctx, String borrowerAddress,
                                    String groupId, String loanId, long amountRaw, SignCallback cb) {
        _signed(ctx, borrowerAddress, fields -> GroupCanonical.fromFields("genLoanRepay", fields),
                new Object[]{groupId, loanId, borrowerAddress, amountRaw, 0L},
                "/api/v1/genloan/repay",
                _json(borrowerAddress, null, null, null, groupId, amountRaw, null, 0L, null, 0L, null, null, null, null, null),
                cb);
    }

    /**
     * GenLoan guardian signature. Canonical: {@code HARVANA_GENLOAN_APPROVE_V1:groupId:loanId}.
     * <strong>No nonce.</strong> The guardian's signature is a one-time approval
     * stored on the node, not a transaction that needs replay protection.
     * Uses {@link _signedNoNonce} because there is no nonce to fetch.
     */
    public static void genLoanGuardianSignature(android.content.Context ctx, String guardianAddress,
                                                String groupId, String loanId, SignCallback cb) {
        _signedNoNonce(ctx, guardianAddress, fields -> GroupCanonical.fromFields("genLoanGuardianSignature", fields),
                new Object[]{groupId, loanId},
                "/api/v1/genloan/guardian-signature",
                _json(guardianAddress, null, null, null, groupId, 0L, null, 0L, null, 0L, null, null, null, null, null),
                cb);
    }

    /**
     * Accum wallet recovery (member address change). Canonical:
     * {@code HARVANA_ACCUM_WALLET_RECOVERY_V1:groupId:oldMember:newMember:acting:nonce}.
     */
    public static void accumWalletRecovery(android.content.Context ctx, String actingAddress,
                                           String groupId, String oldMemberAddress,
                                           String newMemberAddress, SignCallback cb) {
        _signed(ctx, actingAddress, fields -> GroupCanonical.fromFields("accumWalletRecovery", fields),
                new Object[]{groupId, oldMemberAddress, newMemberAddress, actingAddress, 0L},
                "/api/v1/accum/wallet-recovery",
                _json(actingAddress, null, null, null, groupId, 0L, null, 0L, null, 0L, null, null, null, null, null),
                cb);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Sign-and-submit core
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Every method above routes through here. The steps:
     * <ol>
     *   <li>Read the wallet's {@code nextRequestNonce} from the node.</li>
     *   <li>Build the canonical message via the supplied {@link CanonicalBuilder}
     *       (a thin functional interface around {@link GroupCanonical}).</li>
     *   <li>Sign with the wallet's Ed25519 key.</li>
     *   <li>POST the JSON body to the node.</li>
     *   <li>Deliver the response on the main thread.</li>
     * </ol>
     *
     * <p>The {@code fields} array is the ordered argument list for the
     * canonical builder. The last element is always the nonce placeholder
     * (a {@code Long} that {@link _signed} replaces with the live value before
     * calling the builder). All other entries are passed through in order.
     *
     * <p>The {@code body} JSON is the per-endpoint payload; the {@code _signed}
     * helper fills in {@code signature}, {@code publicKey} and {@code nonce}
     * after the canonical message is built and signed.
     */
    private static void _signed(android.content.Context ctx, String address,
                                CanonicalBuilder builder, Object[] fields,
                                String path, org.json.JSONObject body, SignCallback cb) {
        if (address == null || address.isEmpty()) { _postOnMain(cb, "No active wallet."); return; }
        com.harvana.app.sync.HarvanaNodeClient.getWallet(address, new com.harvana.app.sync.HarvanaNodeClient.JsonCallback() {
            @Override public void onSuccess(org.json.JSONObject walletJson) {
                try {
                    long nonce = walletJson.optLong("nextRequestNonce",
                            walletJson.optLong("nonce", 0L) + 1L);
                    // Replace the nonce placeholder (always the last element).
                    fields[fields.length - 1] = nonce;
                    String canonical = builder.build(fields);
                    _signAndPost(ctx, address, canonical, body, path, true, nonce, cb);
                } catch (Exception e) {
                    _postOnMain(cb, "Sign failed: " + e.getMessage());
                }
            }
            @Override public void onError(String error) { _postOnMain(cb, "Couldn't reach the network: " + error); }
        });
    }

    /**
     * Sign-and-submit for canonical messages that have <strong>no nonce</strong>
     * (e.g. {@code HARVANA_GENLOAN_APPROVE_V1:groupId:loanId}). Used for
     * one-time approvals that the node stores; no replay protection needed
     * because the node rejects duplicate signatures anyway.
     */
    private static void _signedNoNonce(android.content.Context ctx, String address,
                                       CanonicalBuilder builder, Object[] fields,
                                       String path, org.json.JSONObject body, SignCallback cb) {
        if (address == null || address.isEmpty()) { _postOnMain(cb, "No active wallet."); return; }
        try {
            String canonical = builder.build(fields);
            _signAndPost(ctx, address, canonical, body, path, false, 0L, cb);
        } catch (Exception e) {
            _postOnMain(cb, "Sign failed: " + e.getMessage());
        }
    }

    /** Shared sign + post path used by both {@link _signed} and {@link _signedNoNonce}. */
    private static void _signAndPost(android.content.Context ctx, String address,
                                     String canonical, org.json.JSONObject body,
                                     String path, boolean putNonce, long nonce, SignCallback cb) {
        try {
            String seedB64 = com.harvana.app.wallet.WalletManager.loadPrivateKey(ctx, address);
            String pubB64 = com.harvana.app.wallet.WalletManager.getPublicKey(ctx, address);
            if (seedB64 == null || pubB64 == null) { _postOnMain(cb, "This wallet has no signing key on this device."); return; }
            byte[] seed = android.util.Base64.decode(seedB64, android.util.Base64.NO_WRAP);
            byte[] sig;
            try {
                sig = com.harvana.app.crypto.Ed25519.sign(seed, canonical.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            } finally {
                java.util.Arrays.fill(seed, (byte) 0);
            }
            String sigB64 = android.util.Base64.encodeToString(sig, android.util.Base64.NO_WRAP);
            body.put("signature", sigB64);
            body.put("publicKey", pubB64);
            if (putNonce) body.put("nonce", nonce);
            com.harvana.app.sync.HarvanaNodeClient.nodePost(path, body.toString(),
                    new com.harvana.app.sync.HarvanaNodeClient.JsonCallback() {
                        @Override public void onSuccess(org.json.JSONObject resp) { _postOnMain(cb, resp); }
                        @Override public void onError(String error) { _postOnMain(cb, error); }
                    });
        } catch (Exception e) {
            _postOnMain(cb, "Sign failed: " + e.getMessage());
        }
    }

    /** Functional interface that builds the canonical message from the fields array. */
    public interface CanonicalBuilder {
        String build(Object[] fields);
    }

    /** Callback delivered on the main thread. */
    public interface SignCallback {
        void onSuccess(org.json.JSONObject response);
        void onError(String error);
    }

    private static void _postOnMain(SignCallback cb, Object result) {
        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
            if (result instanceof org.json.JSONObject) cb.onSuccess((org.json.JSONObject) result);
            else cb.onError(String.valueOf(result));
        });
    }

    /**
     * Build a per-endpoint JSON body. Most fields are null in most calls; the
     * helper packs only what the endpoint needs. Order of arguments here is
     * stable so the resulting JSON keys are stable.
     */
    private static org.json.JSONObject _json(String address, Long interestRateBps,
                                             String withdrawalRule, String asset, String groupId,
                                             Long amountRaw, String lineType, Long lockedUntilTs,
                                             String depositMode, Long maxContributionRaw,
                                             String lineId, Long secondaryAmountRaw,
                                             String beneficiaryAddress, String nameField, String descriptionField) {
        org.json.JSONObject j = new org.json.JSONObject();
        try {
            if (address != null) j.put("address", address);
            if (interestRateBps != null) j.put("interestRateBps", interestRateBps);
            if (withdrawalRule != null) j.put("withdrawalRule", withdrawalRule);
            if (asset != null) j.put("asset", asset);
            if (groupId != null) j.put("groupId", groupId);
            if (amountRaw != null) j.put("amountRaw", amountRaw);
            if (lineType != null) j.put("lineType", lineType);
            if (lockedUntilTs != null) j.put("lockedUntilTs", lockedUntilTs);
            if (depositMode != null) j.put("depositMode", depositMode);
            if (maxContributionRaw != null) j.put("maxContributionRaw", maxContributionRaw);
            if (lineId != null) j.put("lineId", lineId);
            if (secondaryAmountRaw != null) j.put("secondaryAmountRaw", secondaryAmountRaw);
            if (beneficiaryAddress != null) j.put("beneficiaryAddress", beneficiaryAddress);
            if (nameField != null) j.put("name", nameField);
            if (descriptionField != null) j.put("description", descriptionField);
        } catch (Exception e) { /* never happens for JSONObject.put on a fresh object */ }
        return j;
    }
}
