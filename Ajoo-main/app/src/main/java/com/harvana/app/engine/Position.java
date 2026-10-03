package com.harvana.app.engine;

/** What one member's place in one group adds up to, in HARV, for totals and the trust score. Pure data. */
public final class Position {
    public String family = "";
    public double savedHarv;          // money the member has put in and can get back (line, deposits, locked bond)
    public double borrowedHarv, repaidHarv;
    public int expectedPayments, confirmedPayments, paymentGaps;
    public boolean stillIn = true;
    public boolean cycleCompleted;
    public long joinedMs;

    public double outstandingHarv() { return Math.max(0, borrowedHarv - repaidHarv); }
}
