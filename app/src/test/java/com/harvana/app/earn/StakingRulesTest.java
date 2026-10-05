package com.harvana.app.earn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class StakingRulesTest {
    private static final long H = 60L * 60 * 1000;
    private static final long NOW = 1_800_000_000_000L;

    @Test public void firstClaimOpens24HoursAfterTheFirstStake() {
        assertEquals(NOW - 5 * H + 24 * H, StakingRules.nextClaimAt(100, 0, NOW - 5 * H));
    }

    @Test public void laterClaimsCountFromTheLastClaim() {
        assertEquals(NOW - H + 24 * H, StakingRules.nextClaimAt(100, NOW - H, NOW - 100 * H));
    }

    @Test public void noStakeOrNoTimestampMeansNoClaimWindow() {
        assertEquals(0, StakingRules.nextClaimAt(0, NOW, NOW));
        assertEquals(0, StakingRules.nextClaimAt(100, 0, 0));
    }

    @Test public void aLockInTheFutureIsReportedAndAnEndedOrFlexibleOneIsNot() {
        long unlock = NOW + 3 * 24 * H;
        assertEquals(unlock, StakingRules.lockedUntil(100, unlock, NOW));
        assertEquals(0, StakingRules.lockedUntil(100, NOW - 1, NOW));
        assertEquals(0, StakingRules.lockedUntil(100, 0, NOW));
        assertEquals(0, StakingRules.lockedUntil(0, unlock, NOW));
    }

    @Test public void nodeRefusalCodesAreRecognisedInsideTheirMessages() {
        assertEquals("STAKE_LOCKED", StakingRules.refusalCode("HTTP 400: STAKE_LOCKED: Principal is locked ... Unlocks on Oct 30."));
        assertEquals("CLAIM_AVAILABLE_EVERY_24_HOURS", StakingRules.refusalCode("CLAIM_AVAILABLE_EVERY_24_HOURS"));
        assertEquals("REMAINING_STAKE_BELOW_MINIMUM", StakingRules.refusalCode("x REMAINING_STAKE_BELOW_MINIMUM x"));
        assertNull(StakingRules.refusalCode("Network error: timeout"));
        assertNull(StakingRules.refusalCode(null));
    }
}
