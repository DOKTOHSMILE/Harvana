package com.harvanna.app.tx;

/**
 * Pre-send validation for native HARV transfers.
 *
 * All checks are synchronous — call on any thread.
 * Run validateInputs() immediately when the user types.
 * Run validateBalance() after fetching the confirmed balance from the node.
 *
 * Keeps the send screen responsive:
 *   - Address / amount errors surface instantly (no network call)
 *   - Balance / fee check surfaces after the fee estimate returns (~200ms)
 */
public final class NodeSendValidator {

    private NodeSendValidator() {}

    // ── Result ────────────────────────────────────────────────────────────────

    public static class Result {
        public final boolean valid;
        public final String  errorMessage;  // null if valid

        private Result(boolean valid, String errorMessage) {
            this.valid        = valid;
            this.errorMessage = errorMessage;
        }

        public static Result ok()             { return new Result(true,  null); }
        public static Result fail(String msg) { return new Result(false, msg);  }
    }

    // ── Input validation (no network) ─────────────────────────────────────────

    /**
     * Validates address format and amount string.
     * Call this on every keystroke in the send form.
     */
    public static Result validateInputs(String recipientAddress, String amountInput) {

        // Address
        if (recipientAddress == null || recipientAddress.trim().isEmpty()) {
            return Result.fail("Recipient address is required");
        }
        String addr = recipientAddress.trim();
        if (addr.length() != 44) {
            return Result.fail("Address must be exactly 44 characters");
        }
        if (!addr.startsWith("HV0x")) {
            return Result.fail("Address must start with HV0x");
        }
        if (!addr.substring(4).matches("[0-9a-f]{40}")) {
            return Result.fail("Address contains invalid characters");
        }

        // Amount
        if (amountInput == null || amountInput.trim().isEmpty()) {
            return Result.fail("Amount is required");
        }
        double amount;
        try {
            amount = Double.parseDouble(amountInput.trim().replace(",", ""));
        } catch (NumberFormatException e) {
            return Result.fail("Enter a valid number");
        }
        if (!Double.isFinite(amount) || amount <= 0) {
            return Result.fail("Amount must be greater than zero");
        }
        if (amount < 0.00000001) {
            return Result.fail(("Amount is below the minimum (0.00000001 " + com.harvanna.app.CoinInfo.ticker() + ")"));
        }

        return Result.ok();
    }

    /**
     * Validates that the sender has enough confirmed balance to cover amount + fee.
     * Call this after receiving the fee estimate from the node.
     *
     * @param senderAddress       used for self-send check
     * @param recipientAddress    validated recipient
     * @param amountHarv           requested send amount
     * @param confirmedBalanceHarv live balance from WalletNodeBridge
     * @param estimatedFeeHarv     fee from HarvannaNodeClient.getFee()
     */
    public static Result validateBalance(String senderAddress,
                                          String recipientAddress,
                                          double amountHarv,
                                          double confirmedBalanceHarv,
                                          double estimatedFeeHarv) {
        // Self-send warning
        if (senderAddress != null && senderAddress.equalsIgnoreCase(recipientAddress)) {
            return Result.fail("You cannot send to your own address");
        }

        double total = amountHarv + estimatedFeeHarv;

        if (confirmedBalanceHarv < total) {
            double shortfall = total - confirmedBalanceHarv;
            return Result.fail(String.format(
                java.util.Locale.US,
                ("Insufficient balance. Need %.6f " + com.harvanna.app.CoinInfo.ticker() + " more (amount + fee = %.6f " + com.harvanna.app.CoinInfo.ticker() + ")"),
                shortfall, total));
        }

        return Result.ok();
    }

    /** Quick address-only format check. */
    public static boolean isValidAddress(String address) {
        return address != null
            && address.length() == 44
            && address.startsWith("HV0x")
            && address.substring(4).matches("[0-9a-f]{40}");
    }
}
