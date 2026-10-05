package com.harvana.app.earn;

/**
 * The staking rules the screen has to explain to the user, as pure functions so they can be tested on the JVM.
 * They mirror the node (StakingLedger): a position claims once every 24 hours, and locked principal cannot be
 * unstaked before its unlock time.
 */
public final class StakingRules {
    private StakingRules() {}

    public static final long CLAIM_INTERVAL_MS = 24L * 60 * 60 * 1000;

    /**
     * When the next claim opens: 24 h after the last claim, or after the first stake when nothing has been claimed.
     * 0 when there is no stake or no timestamp to count from.
     */
    public static long nextClaimAt(long stakedRaw, long lastClaimedMs, long firstStakedMs) {
        if (stakedRaw <= 0) return 0;
        long base = lastClaimedMs > 0 ? lastClaimedMs : firstStakedMs;
        return base > 0 ? base + CLAIM_INTERVAL_MS : 0;
    }

    /** Unlock time of locked principal, or 0 when it is flexible, unstaked, or the lock has already ended. */
    public static long lockedUntil(long stakedRaw, long unlockMs, long nowMs) {
        if (stakedRaw <= 0) return 0;
        return unlockMs > nowMs ? unlockMs : 0;
    }

    /** The node's refusal codes the app has a sentence for; null when the text is not one of them. */
    public static String refusalCode(String raw) {
        if (raw == null) return null;
        for (String code : new String[] {"STAKE_LOCKED", "CLAIM_AVAILABLE_EVERY_24_HOURS", "REMAINING_STAKE_BELOW_MINIMUM",
                "NOTHING_TO_CLAIM", "INSUFFICIENT_STAKE"}) {
            if (raw.contains(code)) return code;
        }
        return null;
    }
}
