package com.harvana.app.wallet;

import com.harvana.app.sync.HarvanaNodeClient;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Single source of truth for HARV balance display across the entire app.
 *
 * UNIT RULE:
 *   Raw units (long)  = what the node stores and sends  (e.g. 150_000_000)
 *   HARV display (double) = what the user sees           (e.g. 1.5 HARV)
 *   Conversion: 1 HARV = 100,000,000 raw units (HarvanaNodeClient.HARV_UNITS_PER_TOKEN)
 *
 * NEVER call WalletManager.getNativeBalance() in new code — it reads the old
 * "balance_harv" Firestore field which the node no longer writes to directly.
 * Use rawToHarv() or fetchAndFormat() from WalletNodeBridge instead.
 */
public final class BalanceFormatter {

    private BalanceFormatter() {}

    // ── Conversion ────────────────────────────────────────────────────────────

    /** Convert raw node units to HARV display amount. */
    public static double rawToHarv(long rawUnits) {
        return HarvanaNodeClient.toHarvDisplay(rawUnits);
    }

    /** Convert HARV display amount to raw node units for tx submission. */
    public static long harvToRaw(double harvAmount) {
        return HarvanaNodeClient.toRawUnits(harvAmount);
    }

    // ── Formatting ────────────────────────────────────────────────────────────

    /**
     * Full precision format — use on the wallet home screen.
     * Example: 1,234.56789000 HARV
     */
    public static String full(double harvAmount) {
        return decimal(harvAmount, 8) + " HARV";
    }

    /**
     * Short format — use in lists and transaction rows.
     * Example: 1,234.5679 HARV
     */
    public static String short4(double harvAmount) {
        return decimal(harvAmount, 4) + " HARV";
    }

    /**
     * Two decimal format — use for fee display.
     * Example: 0.00 HARV
     */
    public static String fee(double harvAmount) {
        return decimal(harvAmount, 6) + " HARV";
    }

    /**
     * Trims trailing zeros while keeping whatever precision is actually
     * meaningful, up to 4 decimal places — the general-purpose format for
     * anywhere a balance or amount is shown to a person, not a ledger.
     * Examples: 2.0000 -> "2 HARV", 2.5000 -> "2.5 HARV", 2.5678 -> "2.5678 HARV"
     */
    public static String smart(double harvAmount) {
        return smartDecimal(harvAmount) + " HARV";
    }

    /** Same trimming, without the " HARV" suffix — for places that need the
     *  bare number (e.g. inside a larger formatted string). Falls back to
     *  more precision for genuinely non-zero small amounts (gas fees can be
     *  a few millionths of an HARV) rather than misleadingly showing "0"
     *  for something that isn't actually zero. */
    public static String smartDecimal(double harvAmount) {
        if (!Double.isFinite(harvAmount)) return "0";
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        if (harvAmount != 0 && Math.abs(harvAmount) < 0.0001) {
            DecimalFormat small = new DecimalFormat("#,##0.########", symbols);
            return small.format(harvAmount);
        }
        DecimalFormat df = new DecimalFormat("#,##0.####", symbols);
        return df.format(harvAmount);
    }

    /**
     * A card has no room for a full figure. 10,000 and up is shown as 10k.
     * 1,000,000 and up is shown as 1M. Smaller amounts stay exact.
     */
    public static String card(double amount) {
        if (!Double.isFinite(amount)) return "0";
        double a = Math.abs(amount);
        if (a >= 1_000_000d) return trimCard(amount / 1_000_000d) + "M";
        if (a >= 10_000d) return trimCard(amount / 1_000d) + "k";
        return smartDecimal(amount);
    }

    private static String trimCard(double v) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        DecimalFormat df = new DecimalFormat(Math.abs(v) >= 100 ? "0" : "0.#", symbols);
        return df.format(v);
    }

    /**
     * From raw units — convenience for direct node values.
     * Example: rawFull(150_000_000L) → "1.50000000 HARV"
     */
    public static String rawFull(long rawUnits) {
        return full(rawToHarv(rawUnits));
    }

    public static String rawShort(long rawUnits) {
        return short4(rawToHarv(rawUnits));
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
