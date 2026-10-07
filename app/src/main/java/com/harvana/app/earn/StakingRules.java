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

    // ── Reward rate (mirrors StakingLedger's drip; see the node's docs/STAKING.md) ──
    //
    //   yearly drip      D = pool x 0.05% x 365 x (10 s / observed block seconds)
    //   total weight     W = sum of (stake x lock weight) over every staker
    //   base APR         D / W                         (what a flexible 1x stake earns)
    //   your APR         D x m / W = base APR x m      (m = your lock weight, or the stake-weighted average of your stakes)
    //   a new stake A    D x m / (W + A x m)           (your own stake joins the pool it is split across)
    //   APY              (1 + APR / 365)^365 - 1       (if every daily claim is staked again)
    //   pool when full   the drip equals the daily mint: 0.125 HARV x 8,640 blocks = 1,080 HARV a day

    public static final double DAILY_DRIP = 0.0005;
    public static final double FULL_POOL_DAILY_HARV = 1080.0;

    /** Lock weight for a lock length, as the node sets it from the stake memo. */
    public static double weight(int lockDays) {
        return lockDays >= 90 ? 2.5 : lockDays >= 30 ? 1.75 : lockDays >= 7 ? 1.25 : 1.0;
    }

    /** One year of the pool's current drip in HARV, at the observed block pace (10 s target when unknown). */
    public static double yearlyDrip(double poolHarv, double blockSeconds) {
        double pace = blockSeconds > 0 ? 10.0 / blockSeconds : 1.0;
        return Math.max(0, poolHarv) * DAILY_DRIP * 365.0 * pace;
    }

    /** Total weighted stake, worked back from the node's flexible-rate estimate. 0 when nobody stakes. */
    public static double totalWeight(double yearlyDripHarv, double baseApr) {
        return baseApr > 0 ? yearlyDripHarv / baseApr : 0;
    }

    /**
     * The APR (as a fraction, 0.12 = 12%) a new stake of {@code amountHarv} at weight {@code m} would earn once it joins.
     * 0 for an empty amount or an empty pool.
     */
    public static double aprFor(double amountHarv, double m, double yearlyDripHarv, double totalWeight) {
        if (amountHarv <= 0 || yearlyDripHarv <= 0 || m <= 0) return 0;
        return yearlyDripHarv * m / (totalWeight + amountHarv * m);
    }

    /** APY when every daily reward is staked again. */
    public static double apy(double apr) {
        if (apr <= 0) return 0;
        return Math.pow(1 + apr / 365.0, 365.0) - 1;
    }

    /** Simple earnings over {@code days} at an APR. */
    public static double earned(double amountHarv, double apr, int days) {
        return Math.max(0, amountHarv) * Math.max(0, apr) * days / 365.0;
    }
}
