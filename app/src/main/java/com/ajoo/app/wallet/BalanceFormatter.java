package com.ajoo.app.wallet;

import com.ajoo.app.sync.AjooNodeClient;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Single source of truth for AJC balance display across the entire app.
 *
 * UNIT RULE:
 *   Raw units (long)  = what the node stores and sends  (e.g. 150_000_000)
 *   AJC display (double) = what the user sees           (e.g. 1.5 AJC)
 *   Conversion: 1 AJC = 100,000,000 raw units (AjooNodeClient.AJC_UNITS_PER_TOKEN)
 *
 * NEVER call WalletManager.getNativeBalance() in new code — it reads the old
 * "balance_ajc" Firestore field which the node no longer writes to directly.
 * Use rawToAjc() or fetchAndFormat() from WalletNodeBridge instead.
 */
public final class BalanceFormatter {

    private BalanceFormatter() {}

    // ── Conversion ────────────────────────────────────────────────────────────

    /** Convert raw node units to AJC display amount. */
    public static double rawToAjc(long rawUnits) {
        return AjooNodeClient.toAjcDisplay(rawUnits);
    }

    /** Convert AJC display amount to raw node units for tx submission. */
    public static long ajcToRaw(double ajcAmount) {
        return AjooNodeClient.toRawUnits(ajcAmount);
    }

    // ── Formatting ────────────────────────────────────────────────────────────

    /**
     * Full precision format — use on the wallet home screen.
     * Example: 1,234.56789000 AJC
     */
    public static String full(double ajcAmount) {
        return decimal(ajcAmount, 8) + " AJC";
    }

    /**
     * Short format — use in lists and transaction rows.
     * Example: 1,234.5679 AJC
     */
    public static String short4(double ajcAmount) {
        return decimal(ajcAmount, 4) + " AJC";
    }

    /**
     * Two decimal format — use for fee display.
     * Example: 0.00 AJC
     */
    public static String fee(double ajcAmount) {
        return decimal(ajcAmount, 6) + " AJC";
    }

    /**
     * Trims trailing zeros while keeping whatever precision is actually
     * meaningful, up to 4 decimal places — the general-purpose format for
     * anywhere a balance or amount is shown to a person, not a ledger.
     * Examples: 2.0000 -> "2 AJC", 2.5000 -> "2.5 AJC", 2.5678 -> "2.5678 AJC"
     */
    public static String smart(double ajcAmount) {
        return smartDecimal(ajcAmount) + " AJC";
    }

    /** Same trimming, without the " AJC" suffix — for places that need the
     *  bare number (e.g. inside a larger formatted string). Falls back to
     *  more precision for genuinely non-zero small amounts (gas fees can be
     *  a few millionths of an AJC) rather than misleadingly showing "0"
     *  for something that isn't actually zero. */
    public static String smartDecimal(double ajcAmount) {
        if (!Double.isFinite(ajcAmount)) return "0";
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        if (ajcAmount != 0 && Math.abs(ajcAmount) < 0.0001) {
            DecimalFormat small = new DecimalFormat("#,##0.########", symbols);
            return small.format(ajcAmount);
        }
        DecimalFormat df = new DecimalFormat("#,##0.####", symbols);
        return df.format(ajcAmount);
    }

    /**
     * From raw units — convenience for direct node values.
     * Example: rawFull(150_000_000L) → "1.50000000 AJC"
     */
    public static String rawFull(long rawUnits) {
        return full(rawToAjc(rawUnits));
    }

    public static String rawShort(long rawUnits) {
        return short4(rawToAjc(rawUnits));
    }

    /**
     * Staleness label — shows how fresh the balance is.
     * Example: "Updated 12s ago" or "Live"
     */
    public static String stalenessLabel(long cacheAgeMs) {
        if (cacheAgeMs < 0) return "Not loaded";
        if (cacheAgeMs < 5_000) return "Live";
        if (cacheAgeMs < 60_000) return "Updated " + (cacheAgeMs / 1000) + "s ago";
        if (cacheAgeMs < 3_600_000) return "Updated " + (cacheAgeMs / 60_000) + "m ago";
        return "Stale";
    }

    /**
     * Parse a user-typed amount string to a double.
     * Returns -1.0 if the string is invalid or negative.
     */
    public static double parseUserInput(String input) {
        if (input == null || input.trim().isEmpty()) return -1.0;
        try {
            double v = Double.parseDouble(input.trim().replace(",", ""));
            return (Double.isFinite(v) && v > 0) ? v : -1.0;
        } catch (NumberFormatException e) {
            return -1.0;
        }
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private static String decimal(double value, int decimals) {
        if (!Double.isFinite(value)) return "0";
        StringBuilder pattern = new StringBuilder("#,##0.");
        for (int i = 0; i < decimals; i++) pattern.append("0");
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        DecimalFormat df = new DecimalFormat(pattern.toString(), symbols);
        return df.format(value);
    }
}
