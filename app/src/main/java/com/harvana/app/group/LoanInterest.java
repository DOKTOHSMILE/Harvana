package com.harvana.app.group;

/**
 * Loan interest is charged per day, for the days a loan is actually held: simple interest on the amount borrowed, no compounding,
 * fractional days counted. A loan taken for 30 days and repaid on day 15 pays 15 days of interest. Pure Java.
 * Groups on the network store the rate as a daily rate already (the network charges it per day). An older app-only group may still
 * carry a monthly or yearly rate; it is spread over 30 or 365 days so nobody's loan gets dearer.
 */
public final class LoanInterest {
    private LoanInterest() {}

    public static final int MIN_DAYS = 1;
    public static final int MAX_DAYS = 30;          // the network allows 24 to 720 hours

    /** The rate in percent per day. */
    public static double dailyPercent(double ratePercent, String storedPeriod, boolean onChain) {
        if (onChain) return ratePercent;
        String p = storedPeriod == null ? "" : storedPeriod.trim().toUpperCase(java.util.Locale.US);
        if ("DAILY".equals(p)) return ratePercent;
        if ("ANNUALLY".equals(p)) return ratePercent / 365.0;
        return ratePercent / 30.0;                   // an old "monthly" label, or none
    }

    /** Interest owed on {@code principal} after {@code daysHeld} days (fractions count) at {@code dailyPercent} per day. */
    public static double interest(double principal, double dailyPercent, double daysHeld) {
        return principal * (dailyPercent / 100.0) * Math.max(0.0, daysHeld);
    }

    public static int clampDays(int days) { return Math.max(MIN_DAYS, Math.min(MAX_DAYS, days)); }
}
