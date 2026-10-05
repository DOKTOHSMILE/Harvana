package com.harvana.app.chain;

/**
 * Canonical messages the node verifies for every signed group / staking / referral
 * action. GENERATED from the node's own *Request.getCanonicalMessage() methods and
 * checked against the real node classes (see GroupCanonicalTest for golden values).
 * Each string must match the node byte-for-byte or its signature check fails.
 * Boxed Long / Boolean parameters are concatenated as-is (null prints "null"), exactly
 * like the node does.
 */
public final class GroupCanonical {

    private GroupCanonical() {}

    /** AccumApprovePackRequest.getCanonicalMessage() */
    public static String accumApprovePack(String groupId, String lineId, String adminAddress, long nonce) {
        return "HARVANA_ACCUM_APPROVE_PACK_V1:" + groupId + ":" + lineId + ":" + adminAddress + ":" + nonce;
    }

    /** AccumClaimRequest.getCanonicalMessage() */
    public static String accumClaim(String groupId, String lineId, String memberAddress, long nonce) {
        return "HARVANA_ACCUM_CLAIM_V1:" + groupId + ":" + lineId + ":" + memberAddress + ":" + nonce;
    }

    /** AccumContributeMultipleRequest.getCanonicalMessage() */
    public static String accumContributeMultiple(String groupId, String lineId, String memberAddress, long desiredAmountRaw, long nonce) {
        return "HARVANA_ACCUM_CONTRIBUTE_MULTI_V1:" + groupId + ":" + lineId + ":" + memberAddress + ":" + desiredAmountRaw + ":" + nonce;
    }

    /** AccumContributeRequest.getCanonicalMessage() */
    public static String accumContribute(String groupId, String lineId, String memberAddress, long nonce) {
        return "HARVANA_ACCUM_CONTRIBUTE_V1:" + groupId + ":" + lineId + ":" + memberAddress + ":" + nonce;
    }

    /** AccumContributeVariableRequest.getCanonicalMessage() */
    public static String accumContributeVariable(String groupId, String lineId, String memberAddress, long amountRaw, long nonce) {
        return "HARVANA_ACCUM_CONTRIBUTE_VARIABLE_V1:" + groupId + ":" + lineId + ":" + memberAddress + ":" + amountRaw + ":" + nonce;
    }

    /** AccumCreateGroupRequest.getCanonicalMessage() */
    public static String accumCreateGroup(String creatorAddress, long interestRateBps, String withdrawalRule, long nonce) {
        return "HARVANA_ACCUM_CREATE_GROUP_V1:" + creatorAddress + ":" + interestRateBps + ":" + withdrawalRule + ":" + nonce;
    }

    /** AccumPackRequest.getCanonicalMessage() */
    public static String accumPack(String groupId, String lineId, String memberAddress, long nonce) {
        return "HARVANA_ACCUM_PACK_V1:" + groupId + ":" + lineId + ":" + memberAddress + ":" + nonce;
    }

    /** AccumStartLineRequest.getCanonicalMessage() */
    public static String accumStartLine(String groupId, String memberAddress, long dailyAmountRaw, boolean autoCloseEnabled, String lineType, Long lockedUntilTimestamp, String depositMode, Long maxContributionRaw, long nonce) {
        return "HARVANA_ACCUM_START_LINE_V1:" + groupId + ":" + memberAddress + ":" + dailyAmountRaw + ":"
            + autoCloseEnabled + ":" + lineType + ":" + lockedUntilTimestamp + ":" + depositMode + ":" + maxContributionRaw + ":" + nonce;
    }

    /** AccumWalletRecoveryRequest.getCanonicalMessage() */
    public static String accumWalletRecovery(String groupId, String oldMemberAddress, String newMemberAddress, String actingAddress, long nonce) {
        return "HARVANA_ACCUM_WALLET_RECOVERY_V1:" + groupId + ":" + oldMemberAddress + ":" + newMemberAddress
            + ":" + actingAddress + ":" + nonce;
    }

    /** AdminRecoveryRequest.getCanonicalMessage() */
    public static String adminRecovery(String groupId, String newAdminAddress, long nonce) {
        return "HARVANA_ADMIN_RECOVERY_REQUEST_V1:" + groupId + ":" + newAdminAddress + ":" + nonce;
    }

    /** AdminRecoveryVoteRequest.getCanonicalMessage() */
    public static String adminRecoveryVote(String groupId, String voterAddress, boolean voteYes, long nonce) {
        return "HARVANA_ADMIN_RECOVERY_VOTE_V1:" + groupId + ":" + voterAddress + ":" + voteYes + ":" + nonce;
    }

    /** CircleContributeRequest.getCanonicalMessage() */
    public static String circleContribute(String circleId, String memberAddress, long nonce) {
        return "HARVANA_CIRCLE_CONTRIBUTE_V1:" + circleId + ":" + memberAddress + ":" + nonce;
    }

    /** CircleCreateRequest.getCanonicalMessage() */
    public static String circleCreate(String creatorAddress, long contributionAmountRaw, int memberLimit, long cycleDurationHours, long nonce) {
        return "HARVANA_CIRCLE_CREATE_V1:" + creatorAddress + ":" + contributionAmountRaw + ":" + memberLimit
            + ":" + cycleDurationHours + ":" + nonce;
    }

    /** CircleJoinRequest.getCanonicalMessage() */
    public static String circleJoin(String circleId, String memberAddress, String termsHash, long nonce) {
        return "HARVANA_CIRCLE_JOIN_V1:" + circleId + ":" + memberAddress + ":" + termsHash + ":" + nonce;
    }

    /** CircleWithdrawRequest.getCanonicalMessage() */
    public static String circleWithdraw(String circleId, String memberAddress, long nonce) {
        return "HARVANA_CIRCLE_WITHDRAW_V1:" + circleId + ":" + memberAddress + ":" + nonce;
    }

    /** FixedCreateGroupRequest.getCanonicalMessage() */
    public static String fixedCreateGroup(String creatorAddress, long targetGoalAmountRaw, String disbursementRule, String beneficiaryAddress, long interestRateBps, String groupName, String groupDescription, long nonce) {
        return "HARVANA_FIXED_CREATE_GROUP_V1:" + creatorAddress + ":" + targetGoalAmountRaw + ":"
            + disbursementRule + ":" + beneficiaryAddress + ":" + interestRateBps + ":"
            + groupName + ":" + groupDescription + ":" + nonce;
    }

    /** FixedDepositRequest.getCanonicalMessage() */
    public static String fixedDeposit(String groupId, String memberAddress, long amountRaw, long nonce) {
        return "HARVANA_FIXED_DEPOSIT_V1:" + groupId + ":" + memberAddress + ":" + amountRaw + ":" + nonce;
    }

    /** FixedDisbursementVoteRequest.getCanonicalMessage() */
    public static String fixedDisbursementVote(String groupId, String requestId, String voterAddress, boolean voteYes, long nonce) {
        return "HARVANA_FIXED_DISBURSEMENT_VOTE_V1:" + groupId + ":" + requestId + ":" + voterAddress + ":" + voteYes + ":" + nonce;
    }

    /** FixedGrantVoteRequest.getCanonicalMessage() */
    public static String fixedGrantVote(String groupId, String grantId, String voterAddress, boolean voteYes, long nonce) {
        return "HARVANA_FIXED_GRANT_VOTE_V1:" + groupId + ":" + grantId + ":" + voterAddress + ":" + voteYes + ":" + nonce;
    }

    /** FixedMilestoneVoteRequest.getCanonicalMessage() */
    public static String fixedMilestoneVote(String groupId, String milestoneId, String voterAddress, boolean voteYes, long nonce) {
        return "HARVANA_FIXED_MILESTONE_VOTE_V1:" + groupId + ":" + milestoneId + ":" + voterAddress + ":" + voteYes + ":" + nonce;
    }

    /** FixedRequestDisbursementRequest.getCanonicalMessage() */
    public static String fixedRequestDisbursement(String groupId, String requesterAddress, long nonce) {
        return "HARVANA_FIXED_REQUEST_DISBURSEMENT_V1:" + groupId + ":" + requesterAddress + ":" + nonce;
    }

    /** FixedRequestGrantRequest.getCanonicalMessage() */
    public static String fixedRequestGrant(String groupId, String requesterAddress, long amountRaw, String reasonDescription, long nonce) {
        return "HARVANA_FIXED_REQUEST_GRANT_V1:" + groupId + ":" + requesterAddress + ":" + amountRaw + ":" + reasonDescription + ":" + nonce;
    }

    /** FixedRequestMilestoneRequest.getCanonicalMessage() */
    public static String fixedRequestMilestone(String groupId, String requesterAddress, long milestoneAmountRaw, String milestoneDescription, long nonce) {
        return "HARVANA_FIXED_REQUEST_MILESTONE_V1:" + groupId + ":" + requesterAddress + ":" + milestoneAmountRaw
            + ":" + milestoneDescription + ":" + nonce;
    }

    /** GenLoanGuardianSignatureRequest.getCanonicalMessage() */
    public static String genLoanGuardianSignature(String groupId, String loanId) {
        return "HARVANA_GENLOAN_APPROVE_V1:" + groupId + ":" + loanId;
    }

    /** GenLoanRepayRequest.getCanonicalMessage() */
    public static String genLoanRepay(String groupId, String loanId, String borrowerAddress, long amountRaw, long nonce) {
        return "HARVANA_GENLOAN_REPAY_V1:" + groupId + ":" + loanId + ":" + borrowerAddress + ":" + amountRaw + ":" + nonce;
    }

    /** GenLoanRequestRequest.getCanonicalMessage() */
    public static String genLoanRequest(String groupId, String borrowerAddress, long principalRaw, long durationHours, long nonce) {
        return "HARVANA_GENLOAN_REQUEST_V1:" + groupId + ":" + borrowerAddress + ":" + principalRaw + ":" + durationHours + ":" + nonce;
    }

    /** RecordReferralRequest.getConsentMessage(): the referrer\'s own signature, so nobody can be named as a referrer without agreeing. */
    public static String referralConsent(String referredAddress, String referrerAddress) {
        return "HARVANA_REFERRAL_CONSENT_V1:" + referredAddress + ":" + referrerAddress;
    }

    /** RecordReferralRequest.getCanonicalMessage() */
    public static String recordReferral(String referredAddress, String referrerAddress, long nonce) {
        return "HARVANA_RECORD_REFERRAL_V1:" + referredAddress + ":" + referrerAddress + ":" + nonce;
    }

    /** StakingClaimRequest.getCanonicalMessage() */
    public static String stakingClaim(String address, long nonce) {
        return "HARVANA_STAKING_CLAIM_V1:" + address + ":" + nonce;
    }

    /** StakingUnstakeRequest.getCanonicalMessage() */
    public static String stakingUnstake(String address, long amountRaw, long nonce) {
        return "HARVANA_STAKING_UNSTAKE_V1:" + address + ":" + amountRaw + ":" + nonce;
    }


    /** ForgiveDefaultRequest.getCanonicalMessage(): the circle admin asks the group to forgive one member's default. */
    public static String forgiveDefault(String circleId, String memberAddress, String proposerAddress, long nonce) {
        return "HARVANA_FORGIVE_DEFAULT_REQUEST_V1:" + circleId + ":" + memberAddress + ":" + proposerAddress + ":" + nonce;
    }

    /** ForgiveDefaultVoteRequest.getCanonicalMessage() */
    public static String forgiveDefaultVote(String circleId, String memberAddress, String voterAddress, boolean voteYes, long nonce) {
        return "HARVANA_FORGIVE_DEFAULT_VOTE_V1:" + circleId + ":" + memberAddress + ":" + voterAddress + ":" + voteYes + ":" + nonce;
    }
}
