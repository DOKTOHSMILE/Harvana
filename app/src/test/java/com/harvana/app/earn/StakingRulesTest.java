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

    @Test public void rewardRateFollowsTheDripAndTheWeight() {
        double drip = StakingRules.yearlyDrip(10_000, 10);          // 5 HARV a day
        assertEquals(1825.0, drip, 1e-9);
        double w = StakingRules.totalWeight(drip, drip / 50_000.0);
        assertEquals(50_000.0, w, 1e-6);
        double apr = StakingRules.aprFor(1000, 2.5, drip, w);       // 1825 x 2.5 / 52,500
        assertEquals(0.086904761, apr, 1e-6);
        assertEquals(0.0908, StakingRules.apy(apr), 1e-3);
        assertEquals(1000 * apr * 30 / 365.0, StakingRules.earned(1000, apr, 30), 1e-9);
        assertEquals(0, StakingRules.aprFor(0, 1, drip, w), 0);
        assertEquals(0, StakingRules.aprFor(1000, 1, 0, w), 0);
        assertEquals(2.5, StakingRules.weight(90), 0);
        assertEquals(1.0, StakingRules.weight(0), 0);
    }
}
