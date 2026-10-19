package com.harvana.app.swap;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The swap screen's arithmetic and form rules, kept free of Android so they can be tested. Amounts are raw chain units:
 * HARV has 8 decimals, the Harvana coins 6.
 */
public final class SwapMath {

    private SwapMath() {}

    public static final String HARV = "HARV";
    /** Below this the amount is "0"; above, too large for the chain's 64-bit amounts. */
    static final int MAX_DIGITS = 18;

    public static int decimals(String code) { return HARV.equals(code) ? 8 : 6; }

    /** Typed text → raw units. 0 when empty; -1 when not a number, negative, or longer than the coin's decimals. */
    public static long toRaw(String code, String text) {
        String s = text == null ? "" : text.trim().replace(",", "");
        if (s.isEmpty() || ".".equals(s)) return 0;
        try {
            BigDecimal v = new BigDecimal(s);
            if (v.signum() < 0) return -1;
            if (v.signum() == 0) return 0;
            if (v.stripTrailingZeros().scale() > decimals(code)) return -1;
            BigDecimal raw = v.movePointRight(decimals(code)).setScale(0, RoundingMode.UNNECESSARY);
            if (raw.precision() > MAX_DIGITS) return -1;
            return raw.longValueExact();
        } catch (Exception e) {
            return -1;
        }
    }

    /** True when the typed text has more decimals than the coin keeps. */
    public static boolean tooManyDecimals(String code, String text) {
        try {
            return new BigDecimal(text.trim().replace(",", "")).stripTrailingZeros().scale() > decimals(code);
        } catch (Exception e) {
            return false;
        }
    }

    /** Raw units → plain text for the amount box (no grouping). */
    public static String plain(String code, long raw) {
        return BigDecimal.valueOf(raw, decimals(code)).stripTrailingZeros().toPlainString();
    }

    /** The least the node may pay out: the estimate less the slippage tolerance, never below 1 unit. */
    public static long minOut(long out, long slippageBps) {
        if (out <= 0) return 0;
        long cut = BigDecimal.valueOf(out).multiply(BigDecimal.valueOf(slippageBps))
                .divide(BigDecimal.valueOf(10_000), 0, RoundingMode.UP).longValue();
        return Math.max(1, out - cut);
    }

    /**
     * The most that can be swapped from this balance. A HARV swap also pays the tFare and must leave 1 unit (the node's
     * check: fare + 1 + amount ≤ balance); a coin swap pays its tFare in HARV, so the whole coin balance can go.
     */
    public static long maxSpendable(String from, long balance, long fareRaw) {
        if (balance <= 0) return 0;
        if (!HARV.equals(from)) return balance;
        return Math.max(0, balance - Math.max(0, fareRaw) - 1);
    }

    /** What the Review Swap button says and whether it can be pressed. */
    public static final class Button {
        public final String label;
        public final boolean enabled;
        public final String problem;        // a short reason under the card, or ""

        Button(String label, boolean enabled, String problem) {
            this.label = label;
            this.enabled = enabled;
            this.problem = problem;
        }
    }

    /**
     * The form state, in the order the prompt lists it. {@code balance}/{@code harvBalance} -1 = not known yet;
     * {@code quoteOut} -1 = no quote yet; {@code quoteError} non-empty = the node refused to quote.
     */
    public static Button button(boolean hasWallet, boolean submitting, String text, String from, String to,
                                boolean pairOpen, long balance, long harvBalance, long fareRaw,
                                long quoteOut, String quoteError) {
        if (!hasWallet) return new Button("Create or import a wallet", false, "");
        if (submitting) return new Button("Submitting…", false, "");
        long in = toRaw(from, text);
        if (in == 0) return new Button("Enter an amount", false, "");
        if (in < 0) {
            return new Button("Invalid amount", false, tooManyDecimals(from, text)
                    ? "At most " + decimals(from) + " decimals for " + from + "." : "Enter a positive number.");
        }
        if (from.equals(to)) return new Button("Pick two different assets", false, "");
        if (!pairOpen) return new Button("Pair unavailable", false, "This pair has no liquidity right now.");
        if (balance >= 0 && in > maxSpendable(from, balance, fareRaw)) {
            return new Button("Insufficient balance", false, HARV.equals(from) && in <= balance
                    ? "Keep enough HARV for the tFare." : "More than your available balance.");
        }
        if (!HARV.equals(from) && harvBalance >= 0 && fareRaw >= 0 && harvBalance < fareRaw + 1) {
            return new Button("Not enough HARV for tFare", false, "A swap's tFare is paid in HARV.");
        }
        if (quoteError != null && !quoteError.isEmpty()) return new Button("Quote unavailable", false, quoteError);
        if (quoteOut < 0) return new Button("Getting quote…", false, "");
        if (quoteOut == 0) return new Button("Amount too small", false, "This amount returns nothing after fees.");
        return new Button("Review Swap", true, "");
    }

    /** The node's order status as a word for the screen, and its colour group. */
    public static String statusWord(String status) {
        switch (status == null ? "" : status) {
            case "DONE": return "Completed";
            case "REFUNDED": return "Refunded";
            case "PENDING": return "Confirming";
            default: return status == null || status.isEmpty() ? "Pending" : status;
        }
    }

    /** 1 = success, 0 = waiting, -1 = failed or returned. */
    public static int statusTone(String status) {
        if ("DONE".equals(status)) return 1;
        if ("REFUNDED".equals(status)) return -1;
        return 0;
    }
}
