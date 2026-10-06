package com.harvana.app.chain;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** Golden values produced by the node's own *Request.getCanonicalMessage() (58 cases: 29 actions x 2 variants). */
public class GroupCanonicalTest {

    @Test public void accumApprovePack_v0() {
        assertEquals("HARVANA_ACCUM_APPROVE_PACK_V1:V_groupId:V_lineId:V_adminAddress:1003",
            GroupCanonical.accumApprovePack("V_groupId", "V_lineId", "V_adminAddress", 1003L));
    }

    @Test public void accumApprovePack_v1() {
        assertEquals("HARVANA_ACCUM_APPROVE_PACK_V1:V_groupId:x y:V_lineId:x y:V_adminAddress:x y:1010",
            GroupCanonical.accumApprovePack("V_groupId:x y", "V_lineId:x y", "V_adminAddress:x y", 1010L));
    }

    @Test public void accumClaim_v0() {
        assertEquals("HARVANA_ACCUM_CLAIM_V1:V_groupId:V_lineId:V_memberAddress:1003",
            GroupCanonical.accumClaim("V_groupId", "V_lineId", "V_memberAddress", 1003L));
    }

    @Test public void accumClaim_v1() {
        assertEquals("HARVANA_ACCUM_CLAIM_V1:V_groupId:x y:V_lineId:x y:V_memberAddress:x y:1010",
            GroupCanonical.accumClaim("V_groupId:x y", "V_lineId:x y", "V_memberAddress:x y", 1010L));
    }

    @Test public void accumContributeMultiple_v0() {
        assertEquals("HARVANA_ACCUM_CONTRIBUTE_MULTI_V1:V_groupId:V_lineId:V_memberAddress:1003:1004",
            GroupCanonical.accumContributeMultiple("V_groupId", "V_lineId", "V_memberAddress", 1003L, 1004L));
    }

    @Test public void accumContributeMultiple_v1() {
        assertEquals("HARVANA_ACCUM_CONTRIBUTE_MULTI_V1:V_groupId:x y:V_lineId:x y:V_memberAddress:x y:1010:1011",
            GroupCanonical.accumContributeMultiple("V_groupId:x y", "V_lineId:x y", "V_memberAddress:x y", 1010L, 1011L));
    }

    @Test public void accumContribute_v0() {
        assertEquals("HARVANA_ACCUM_CONTRIBUTE_V1:V_groupId:V_lineId:V_memberAddress:1003",
            GroupCanonical.accumContribute("V_groupId", "V_lineId", "V_memberAddress", 1003L));
    }

    @Test public void accumContribute_v1() {
        assertEquals("HARVANA_ACCUM_CONTRIBUTE_V1:V_groupId:x y:V_lineId:x y:V_memberAddress:x y:1010",
            GroupCanonical.accumContribute("V_groupId:x y", "V_lineId:x y", "V_memberAddress:x y", 1010L));
    }

    @Test public void accumContributeVariable_v0() {
        assertEquals("HARVANA_ACCUM_CONTRIBUTE_VARIABLE_V1:V_groupId:V_lineId:V_memberAddress:1003:1004",
            GroupCanonical.accumContributeVariable("V_groupId", "V_lineId", "V_memberAddress", 1003L, 1004L));
    }

    @Test public void accumContributeVariable_v1() {
        assertEquals("HARVANA_ACCUM_CONTRIBUTE_VARIABLE_V1:V_groupId:x y:V_lineId:x y:V_memberAddress:x y:1010:1011",
            GroupCanonical.accumContributeVariable("V_groupId:x y", "V_lineId:x y", "V_memberAddress:x y", 1010L, 1011L));
    }

    @Test public void accumCreateGroup_v0() {
        assertEquals("HARVANA_ACCUM_CREATE_GROUP_V1:V_creatorAddress:1001:V_withdrawalRule:1003",
            GroupCanonical.accumCreateGroup("V_creatorAddress", 1001L, "V_withdrawalRule", 1003L));
    }

    @Test public void accumCreateGroup_v1() {
        assertEquals("HARVANA_ACCUM_CREATE_GROUP_V1:V_creatorAddress:x y:1008:V_withdrawalRule:x y:1010",
            GroupCanonical.accumCreateGroup("V_creatorAddress:x y", 1008L, "V_withdrawalRule:x y", 1010L));
    }

    @Test public void accumPack_v0() {
        assertEquals("HARVANA_ACCUM_PACK_V1:V_groupId:V_lineId:V_memberAddress:1003",
            GroupCanonical.accumPack("V_groupId", "V_lineId", "V_memberAddress", 1003L));
    }

    @Test public void accumPack_v1() {
        assertEquals("HARVANA_ACCUM_PACK_V1:V_groupId:x y:V_lineId:x y:V_memberAddress:x y:1010",
            GroupCanonical.accumPack("V_groupId:x y", "V_lineId:x y", "V_memberAddress:x y", 1010L));
    }

    @Test public void accumStartLine_v0() {
        assertEquals("HARVANA_ACCUM_START_LINE_V1:V_groupId:V_memberAddress:1002:true:V_lineType:null:V_depositMode:null:1008",
            GroupCanonical.accumStartLine("V_groupId", "V_memberAddress", 1002L, true, "V_lineType", null, "V_depositMode", null, 1008L));
    }

    @Test public void accumStartLine_v1() {
        assertEquals("HARVANA_ACCUM_START_LINE_V1:V_groupId:x y:V_memberAddress:x y:1009:false:V_lineType:x y:1893456000000:V_depositMode:x y:1893456000000:1015",
            GroupCanonical.accumStartLine("V_groupId:x y", "V_memberAddress:x y", 1009L, false, "V_lineType:x y", 1893456000000L, "V_depositMode:x y", 1893456000000L, 1015L));
    }

    @Test public void accumWalletRecovery_v0() {
        assertEquals("HARVANA_ACCUM_WALLET_RECOVERY_V1:V_groupId:V_oldMemberAddress:V_newMemberAddress:V_actingAddress:1004",
            GroupCanonical.accumWalletRecovery("V_groupId", "V_oldMemberAddress", "V_newMemberAddress", "V_actingAddress", 1004L));
    }

    @Test public void accumWalletRecovery_v1() {
        assertEquals("HARVANA_ACCUM_WALLET_RECOVERY_V1:V_groupId:x y:V_oldMemberAddress:x y:V_newMemberAddress:x y:V_actingAddress:x y:1011",
            GroupCanonical.accumWalletRecovery("V_groupId:x y", "V_oldMemberAddress:x y", "V_newMemberAddress:x y", "V_actingAddress:x y", 1011L));
    }

    @Test public void adminRecovery_v0() {
        assertEquals("HARVANA_ADMIN_RECOVERY_REQUEST_V1:V_groupId:V_newAdminAddress:1002",
            GroupCanonical.adminRecovery("V_groupId", "V_newAdminAddress", 1002L));
    }

    @Test public void adminRecovery_v1() {
        assertEquals("HARVANA_ADMIN_RECOVERY_REQUEST_V1:V_groupId:x y:V_newAdminAddress:x y:1009",
            GroupCanonical.adminRecovery("V_groupId:x y", "V_newAdminAddress:x y", 1009L));
    }

    @Test public void adminRecoveryVote_v0() {
        assertEquals("HARVANA_ADMIN_RECOVERY_VOTE_V1:V_groupId:V_voterAddress:true:1003",
            GroupCanonical.adminRecoveryVote("V_groupId", "V_voterAddress", true, 1003L));
    }

    @Test public void adminRecoveryVote_v1() {
        assertEquals("HARVANA_ADMIN_RECOVERY_VOTE_V1:V_groupId:x y:V_voterAddress:x y:false:1010",
            GroupCanonical.adminRecoveryVote("V_groupId:x y", "V_voterAddress:x y", false, 1010L));
    }

    @Test public void circleContribute_v0() {
        assertEquals("HARVANA_CIRCLE_CONTRIBUTE_V1:V_circleId:V_memberAddress:1002",
            GroupCanonical.circleContribute("V_circleId", "V_memberAddress", 1002L));
    }

    @Test public void circleContribute_v1() {
        assertEquals("HARVANA_CIRCLE_CONTRIBUTE_V1:V_circleId:x y:V_memberAddress:x y:1009",
            GroupCanonical.circleContribute("V_circleId:x y", "V_memberAddress:x y", 1009L));
    }

    @Test public void circleCreate_v0() {
        assertEquals("HARVANA_CIRCLE_CREATE_V1:V_creatorAddress:1001:5:1003:1004",
            GroupCanonical.circleCreate("V_creatorAddress", 1001L, 5, 1003L, 1004L));
    }

    @Test public void circleCreate_v1() {
        assertEquals("HARVANA_CIRCLE_CREATE_V1:V_creatorAddress:x y:1008:6:1010:1011",
            GroupCanonical.circleCreate("V_creatorAddress:x y", 1008L, 6, 1010L, 1011L));
    }

    @Test public void circleJoin_v0() {
        assertEquals("HARVANA_CIRCLE_JOIN_V1:V_circleId:V_memberAddress:V_termsHash:1003",
            GroupCanonical.circleJoin("V_circleId", "V_memberAddress", "V_termsHash", 1003L));
    }

    @Test public void circleJoin_v1() {
        assertEquals("HARVANA_CIRCLE_JOIN_V1:V_circleId:x y:V_memberAddress:x y:V_termsHash:x y:1010",
            GroupCanonical.circleJoin("V_circleId:x y", "V_memberAddress:x y", "V_termsHash:x y", 1010L));
    }

    @Test public void circleWithdraw_v0() {
        assertEquals("HARVANA_CIRCLE_WITHDRAW_V1:V_circleId:V_memberAddress:1002",
            GroupCanonical.circleWithdraw("V_circleId", "V_memberAddress", 1002L));
    }

    @Test public void circleWithdraw_v1() {
        assertEquals("HARVANA_CIRCLE_WITHDRAW_V1:V_circleId:x y:V_memberAddress:x y:1009",
            GroupCanonical.circleWithdraw("V_circleId:x y", "V_memberAddress:x y", 1009L));
    }

    @Test public void fixedCreateGroup_v0() {
        assertEquals("HARVANA_FIXED_CREATE_GROUP_V1:V_creatorAddress:1001:V_disbursementRule:V_beneficiaryAddress:1004:V_groupName:V_groupDescription:1007",
            GroupCanonical.fixedCreateGroup("V_creatorAddress", 1001L, "V_disbursementRule", "V_beneficiaryAddress", 1004L, "V_groupName", "V_groupDescription", 1007L));
    }

    @Test public void fixedCreateGroup_v1() {
        assertEquals("HARVANA_FIXED_CREATE_GROUP_V1:V_creatorAddress:x y:1008:V_disbursementRule:x y:V_beneficiaryAddress:x y:1011:V_groupName:x y:V_groupDescription:x y:1014",
            GroupCanonical.fixedCreateGroup("V_creatorAddress:x y", 1008L, "V_disbursementRule:x y", "V_beneficiaryAddress:x y", 1011L, "V_groupName:x y", "V_groupDescription:x y", 1014L));
    }

    @Test public void fixedDeposit_v0() {
        assertEquals("HARVANA_FIXED_DEPOSIT_V1:V_groupId:V_memberAddress:1002:1003",
            GroupCanonical.fixedDeposit("V_groupId", "V_memberAddress", 1002L, 1003L));
    }

    @Test public void fixedDeposit_v1() {
        assertEquals("HARVANA_FIXED_DEPOSIT_V1:V_groupId:x y:V_memberAddress:x y:1009:1010",
            GroupCanonical.fixedDeposit("V_groupId:x y", "V_memberAddress:x y", 1009L, 1010L));
    }

    @Test public void fixedDisbursementVote_v0() {
        assertEquals("HARVANA_FIXED_DISBURSEMENT_VOTE_V1:V_groupId:V_requestId:V_voterAddress:true:1004",
            GroupCanonical.fixedDisbursementVote("V_groupId", "V_requestId", "V_voterAddress", true, 1004L));
    }

    @Test public void fixedDisbursementVote_v1() {
        assertEquals("HARVANA_FIXED_DISBURSEMENT_VOTE_V1:V_groupId:x y:V_requestId:x y:V_voterAddress:x y:false:1011",
            GroupCanonical.fixedDisbursementVote("V_groupId:x y", "V_requestId:x y", "V_voterAddress:x y", false, 1011L));
    }

    @Test public void fixedGrantVote_v0() {
        assertEquals("HARVANA_FIXED_GRANT_VOTE_V1:V_groupId:V_grantId:V_voterAddress:true:1004",
            GroupCanonical.fixedGrantVote("V_groupId", "V_grantId", "V_voterAddress", true, 1004L));
    }

    @Test public void fixedGrantVote_v1() {
        assertEquals("HARVANA_FIXED_GRANT_VOTE_V1:V_groupId:x y:V_grantId:x y:V_voterAddress:x y:false:1011",
            GroupCanonical.fixedGrantVote("V_groupId:x y", "V_grantId:x y", "V_voterAddress:x y", false, 1011L));
    }

    @Test public void fixedMilestoneVote_v0() {
        assertEquals("HARVANA_FIXED_MILESTONE_VOTE_V1:V_groupId:V_milestoneId:V_voterAddress:true:1004",
            GroupCanonical.fixedMilestoneVote("V_groupId", "V_milestoneId", "V_voterAddress", true, 1004L));
    }

    @Test public void fixedMilestoneVote_v1() {
        assertEquals("HARVANA_FIXED_MILESTONE_VOTE_V1:V_groupId:x y:V_milestoneId:x y:V_voterAddress:x y:false:1011",
            GroupCanonical.fixedMilestoneVote("V_groupId:x y", "V_milestoneId:x y", "V_voterAddress:x y", false, 1011L));
    }

    @Test public void fixedRequestDisbursement_v0() {
        assertEquals("HARVANA_FIXED_REQUEST_DISBURSEMENT_V1:V_groupId:V_requesterAddress:1002",
            GroupCanonical.fixedRequestDisbursement("V_groupId", "V_requesterAddress", 1002L));
    }

    @Test public void fixedRequestDisbursement_v1() {
        assertEquals("HARVANA_FIXED_REQUEST_DISBURSEMENT_V1:V_groupId:x y:V_requesterAddress:x y:1009",
            GroupCanonical.fixedRequestDisbursement("V_groupId:x y", "V_requesterAddress:x y", 1009L));
    }

    @Test public void fixedNewGoal() {
        assertEquals("HARVANA_FIXED_NEW_GOAL_V1:G:A:500000000:Phase 2:7", GroupCanonical.fixedNewGoal("G", "A", 500000000L, "Phase 2", 7L));
        assertEquals("HARVANA_FIXED_NEW_GOAL_V1:G:A:1::8", GroupCanonical.fixedNewGoal("G", "A", 1L, null, 8L));
    }

    @Test public void fixedRequestGrant_v0() {
        assertEquals("HARVANA_FIXED_REQUEST_GRANT_V1:V_groupId:V_requesterAddress:1002:V_reasonDescription:1004",
            GroupCanonical.fixedRequestGrant("V_groupId", "V_requesterAddress", 1002L, "V_reasonDescription", 1004L));
    }

    @Test public void fixedRequestGrant_v1() {
        assertEquals("HARVANA_FIXED_REQUEST_GRANT_V1:V_groupId:x y:V_requesterAddress:x y:1009:V_reasonDescription:x y:1011",
            GroupCanonical.fixedRequestGrant("V_groupId:x y", "V_requesterAddress:x y", 1009L, "V_reasonDescription:x y", 1011L));
    }

    @Test public void fixedRequestMilestone_v0() {
        assertEquals("HARVANA_FIXED_REQUEST_MILESTONE_V1:V_groupId:V_requesterAddress:1002:V_milestoneDescription:1004",
            GroupCanonical.fixedRequestMilestone("V_groupId", "V_requesterAddress", 1002L, "V_milestoneDescription", 1004L));
    }

    @Test public void fixedRequestMilestone_v1() {
        assertEquals("HARVANA_FIXED_REQUEST_MILESTONE_V1:V_groupId:x y:V_requesterAddress:x y:1009:V_milestoneDescription:x y:1011",
            GroupCanonical.fixedRequestMilestone("V_groupId:x y", "V_requesterAddress:x y", 1009L, "V_milestoneDescription:x y", 1011L));
    }

    @Test public void genLoanGuardianSignature_v0() {
        assertEquals("HARVANA_GENLOAN_APPROVE_V1:V_groupId:V_loanId",
            GroupCanonical.genLoanGuardianSignature("V_groupId", "V_loanId"));
    }

    @Test public void genLoanGuardianSignature_v1() {
        assertEquals("HARVANA_GENLOAN_APPROVE_V1:V_groupId:x y:V_loanId:x y",
            GroupCanonical.genLoanGuardianSignature("V_groupId:x y", "V_loanId:x y"));
    }

    @Test public void genLoanRepay_v0() {
        assertEquals("HARVANA_GENLOAN_REPAY_V1:V_groupId:V_loanId:V_borrowerAddress:1003:1004",
            GroupCanonical.genLoanRepay("V_groupId", "V_loanId", "V_borrowerAddress", 1003L, 1004L));
    }

    @Test public void genLoanRepay_v1() {
        assertEquals("HARVANA_GENLOAN_REPAY_V1:V_groupId:x y:V_loanId:x y:V_borrowerAddress:x y:1010:1011",
            GroupCanonical.genLoanRepay("V_groupId:x y", "V_loanId:x y", "V_borrowerAddress:x y", 1010L, 1011L));
    }

    @Test public void genLoanRequest_v0() {
        assertEquals("HARVANA_GENLOAN_REQUEST_V1:V_groupId:V_borrowerAddress:1002:1003:1004",
            GroupCanonical.genLoanRequest("V_groupId", "V_borrowerAddress", 1002L, 1003L, 1004L));
    }

    @Test public void genLoanRequest_v1() {
        assertEquals("HARVANA_GENLOAN_REQUEST_V1:V_groupId:x y:V_borrowerAddress:x y:1009:1010:1011",
            GroupCanonical.genLoanRequest("V_groupId:x y", "V_borrowerAddress:x y", 1009L, 1010L, 1011L));
    }

    @Test public void recordReferral_v0() {
        assertEquals("HARVANA_RECORD_REFERRAL_V1:V_referredAddress:V_referrerAddress:1002",
            GroupCanonical.recordReferral("V_referredAddress", "V_referrerAddress", 1002L));
    }

    @Test public void recordReferral_v1() {
        assertEquals("HARVANA_RECORD_REFERRAL_V1:V_referredAddress:x y:V_referrerAddress:x y:1009",
            GroupCanonical.recordReferral("V_referredAddress:x y", "V_referrerAddress:x y", 1009L));
    }

    @Test public void stakingClaim_v0() {
        assertEquals("HARVANA_STAKING_CLAIM_V1:V_address:1001",
            GroupCanonical.stakingClaim("V_address", 1001L));
    }

    @Test public void stakingClaim_v1() {
        assertEquals("HARVANA_STAKING_CLAIM_V1:V_address:x y:1008",
            GroupCanonical.stakingClaim("V_address:x y", 1008L));
    }

    @Test public void stakingUnstake_v0() {
        assertEquals("HARVANA_STAKING_UNSTAKE_V1:V_address:1001:1002",
            GroupCanonical.stakingUnstake("V_address", 1001L, 1002L));
    }

    @Test public void stakingUnstake_ofOneStake_signsTheStakeId() {
        assertEquals("HARVANA_STAKING_UNSTAKE_V2:V_address:LOT-abc:500:7",
            GroupCanonical.stakingUnstake("V_address", "LOT-abc", 500L, 7L));
        assertEquals("HARVANA_STAKING_UNSTAKE_V1:V_address:500:7",
            GroupCanonical.stakingUnstake("V_address", null, 500L, 7L));
    }

    @Test public void stakingUnstake_v1() {
        assertEquals("HARVANA_STAKING_UNSTAKE_V1:V_address:x y:1008:1009",
            GroupCanonical.stakingUnstake("V_address:x y", 1008L, 1009L));
    }


    @Test public void forgiveDefault_matchesTheNode() {
        assertEquals("HARVANA_FORGIVE_DEFAULT_REQUEST_V1:CIRC-1:HV0xabc:HV0xadm:1004",
            GroupCanonical.forgiveDefault("CIRC-1", "HV0xabc", "HV0xadm", 1004L));
    }

    @Test public void forgiveDefaultVote_matchesTheNode() {
        assertEquals("HARVANA_FORGIVE_DEFAULT_VOTE_V1:CIRC-1:HV0xabc:HV0xvot:true:1005",
            GroupCanonical.forgiveDefaultVote("CIRC-1", "HV0xabc", "HV0xvot", true, 1005L));
        assertEquals("HARVANA_FORGIVE_DEFAULT_VOTE_V1:CIRC-1:HV0xabc:HV0xvot:false:1006",
            GroupCanonical.forgiveDefaultVote("CIRC-1", "HV0xabc", "HV0xvot", false, 1006L));
    }

    @Test public void accumBackpay_sortsTheDatesLikeTheNode() {
        assertEquals("HARVANA_ACCUM_BACKPAY_V1:ACCUM-g:LINE-l:HV0xabc:2026-10-01,2026-10-02,2026-10-03:7",
            GroupCanonical.accumBackpay("ACCUM-g", "LINE-l", "HV0xabc", java.util.Arrays.asList("2026-10-03", "2026-10-01", "2026-10-02"), 7L));
    }

    @Test public void cureDefault_matchesTheNode() {
        assertEquals("HARVANA_CURE_DEFAULT_V1:CIRC-1:HV0xabc:9", GroupCanonical.cureDefault("CIRC-1", "HV0xabc", 9L));
    }
}
