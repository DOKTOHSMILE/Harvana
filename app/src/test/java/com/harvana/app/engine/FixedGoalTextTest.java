package com.harvana.app.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FixedGoalTextTest {

    @Test
    public void aReachedGoalUnderAnAdminRuleHasNotPaidAnyone() {
        assertEquals(FixedGoalText.Stage.HELD, FixedGoalText.stage("ADMIN_TRIGGERED", "ACTIVE", true));
        String s = FixedGoalText.sentence("ADMIN_TRIGGERED", "ACTIVE", true, "40 HARV", "HV0xcd86…e3205d0");
        assertTrue(s, s.contains("held by the group wallet"));
        assertTrue(s, s.contains("has not been paid out"));
        assertTrue(s, s.contains("members vote"));
        assertFalse("must not claim the money was sent", s.contains("was sent") || s.contains("sending"));
    }

    @Test
    public void onlyACompletedGroupSaysTheMoneyWasSent() {
        assertEquals(FixedGoalText.Stage.PAID, FixedGoalText.stage("ADMIN_TRIGGERED", "COMPLETED", true));
        assertTrue(FixedGoalText.sentence("ALL_AT_ONCE_AT_GOAL", "COMPLETED", true, "40 HARV", "HV0xabc").contains("was sent to HV0xabc"));
    }

    @Test
    public void allAtOnceIsSendingUntilTheNetworkConfirms() {
        assertEquals(FixedGoalText.Stage.SENDING, FixedGoalText.stage("ALL_AT_ONCE_AT_GOAL", "ACTIVE", true));
        assertTrue(FixedGoalText.sentence("ALL_AT_ONCE_AT_GOAL", "ACTIVE", true, "40 HARV", "HV0xabc").contains("is sending"));
    }

    @Test
    public void anOpenGoalInvitesDeposits() {
        assertEquals(FixedGoalText.Stage.OPEN, FixedGoalText.stage("ADMIN_TRIGGERED", "ACTIVE", false));
        assertTrue(FixedGoalText.sentence("ADMIN_TRIGGERED", "ACTIVE", false, "5 HARV", "x").startsWith("Deposit any day"));
    }

    @Test
    public void fundsLeftOnlyWhileTheGroupIsLiveAndSomethingRemains() {
        assertTrue(FixedGoalText.fundsLeft("ACTIVE", 4_000_000_000L, 0));
        assertFalse(FixedGoalText.fundsLeft("ACTIVE", 4_000_000_000L, 4_000_000_000L));
        assertFalse(FixedGoalText.fundsLeft("COMPLETED", 4_000_000_000L, 0));
    }
}
