package com.harvana.app.engine;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class EngineSnapshotTest {
    private static Map<String, Object> m(Object... kv) {
        Map<String, Object> r = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) r.put((String) kv[i], kv[i + 1]);
        return r;
    }

    private static boolean has(GroupSnapshot s, String id) {
        for (GroupAction a : s.actions) if (a.id.equals(id)) return true;
        return false;
    }

    private Map<String, Object> circle(String state, int round) {
        return m("circleId", "CIRC-1", "state", state, "currentRound", round, "memberLimit", 4,
            "contributionAmountRaw", 3_000_000_000L, "requiredBondRaw", 9_000_000_000L, "cycleDurationHours", 168,
            "payoutOrder", new ArrayList<>(Arrays.asList("a", "b")), "escrowAddress", "HV0xE");
    }

    @Test public void circlePotIsOneLessThanMembers() {
        GroupSnapshot s = CircleSnapshot.of(circle("FORMING", 0), null);
        boolean ok = false;
        for (GroupSnapshot.Stat t : s.terms) if (t.label.equals("Pot per round") && t.value.equals("90 HARV")) ok = true;
        assertTrue("pot is 30 x (4-1)", ok);
    }

    @Test public void nonMemberCanJoinWhileForming() {
        assertTrue(has(CircleSnapshot.of(circle("FORMING", 0), null), GroupAction.JOIN));
        assertFalse(has(CircleSnapshot.of(circle("ACTIVE", 1), null), GroupAction.JOIN));
    }

    @Test public void memberPaysUnlessItsTheirTurn() {
        Map<String, Object> me = m("state", "ACTIVE", "payoutRound", 2);
        assertTrue(has(CircleSnapshot.of(circle("ACTIVE", 1), me), GroupAction.CONTRIBUTE));
        GroupSnapshot own = CircleSnapshot.of(circle("ACTIVE", 2), me);
        assertFalse(has(own, GroupAction.CONTRIBUTE));
        assertFalse(own.notes.isEmpty());
    }

    @Test public void completedCircleOffersBondWithdrawal() {
        assertTrue(has(CircleSnapshot.of(circle("COMPLETED", 4), m("state", "COMPLETED")), GroupAction.WITHDRAW_BOND));
    }

    @Test public void accumStartsThenSavesThenPacksThenClaims() {
        Map<String, Object> g = m("groupId", "ACCUM-1", "state", "ACTIVE", "creatorAddress", "adm", "interestRateBps", 500L);
        assertTrue(has(AccumSnapshot.of(g, null, null, "me"), GroupAction.START_LINE));
        Map<String, Object> line = m("lineId", "L1", "status", "ACTIVE", "dailyAmountRaw", 100_000_000L, "lineType", "FLEXIBLE", "depositMode", "ONGOING");
        GroupSnapshot active = AccumSnapshot.of(g, m(), line, "me");
        assertTrue(has(active, GroupAction.ACCUM_CONTRIBUTE));
        assertTrue(has(active, GroupAction.ACCUM_PACK));
        Map<String, Object> locked = m("lineId", "L1", "status", "ACTIVE", "dailyAmountRaw", 1L, "lineType", "LOCKED",
            "lockedUntilTimestamp", System.currentTimeMillis() + 86_400_000L, "depositMode", "ONGOING");
        assertFalse(has(AccumSnapshot.of(g, m(), locked, "me"), GroupAction.ACCUM_PACK));
        assertTrue(has(AccumSnapshot.of(g, m(), m("lineId", "L1", "status", "CLAIMABLE"), "me"), GroupAction.ACCUM_CLAIM));
        assertFalse(AccumSnapshot.of(g, m(), m("lineId", "L1", "status", "PENDING_PACK_APPROVAL"), "me").notes.isEmpty());
    }

    @Test public void fixedActionsFollowTheRule() {
        Map<String, Object> g = m("groupId", "FIXED-1", "state", "ACTIVE", "creatorAddress", "adm", "targetGoalAmountRaw", 1_000_000_000L,
            "raisedAmountRaw", 250_000_000L, "disbursementRule", "ADMIN_TRIGGERED");
        assertTrue(has(FixedSnapshot.of(g, m("contributedRaw", 1L), "adm"), GroupAction.FIXED_REQUEST_DISBURSEMENT));
        assertFalse(has(FixedSnapshot.of(g, m("contributedRaw", 1L), "someone"), GroupAction.FIXED_REQUEST_DISBURSEMENT));
        g.put("disbursementRule", "EMERGENCY_GRANT");
        assertTrue(has(FixedSnapshot.of(g, m("contributedRaw", 1L), "someone"), GroupAction.FIXED_REQUEST_GRANT));
        g.put("disbursementRule", "ALL_AT_ONCE_AT_GOAL");
        GroupSnapshot auto = FixedSnapshot.of(g, m(), "adm");
        assertEquals(250, auto.progressCurrent);
        assertFalse(auto.notes.isEmpty());
    }

    @Test public void voteSummaryUsesWeights() {
        Map<String, Object> weights = m("adm", 4000L, "a", 3000L, "b", 3000L);
        Map<String, Object> votes = m("adm", true, "a", true);
        Map<String, Object> rec = m("status", "PENDING", "voterWeights", weights, "votes", votes, "thresholdBps", 5100L, "minVoters", 2L);
        VoteView v = VoteView.of("Release", rec, "b");
        assertEquals(70, v.yesPercent);
        assertTrue(v.open);
        assertTrue(v.eligible);
        assertFalse(v.alreadyVoted);
        assertEquals(51, v.thresholdPercent);
        assertFalse(VoteView.of("x", rec, "stranger").eligible);
    }
}
