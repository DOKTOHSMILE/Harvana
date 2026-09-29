package com.harvanna.app.group;

/**
 * Shared validation helpers for the Group Engine.
 * All managers should call these instead of duplicating checks.
 * Keeps error messages consistent and the contract rules in one place.
 */
public final class GroupValidation {

    private GroupValidation() {}

    public static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /** AJC wallet addresses must start with HV0x and be reasonably long. */
    public static boolean isValidAjcWallet(String address) {
        if (address == null) return false;
        String a = address.trim();
        return a.startsWith("HV0x") && a.length() >= 10;
    }

    public static String requireUid(String uid, String fallbackError) {
        if (isBlank(uid)) return fallbackError != null ? fallbackError : "Not signed in.";
        return null;
    }

    public static String requireGroupId(String groupId) {
        if (isBlank(groupId)) return "Missing group id.";
        return null;
    }

    public static String requirePositiveAmount(double amountAjc) {
        if (amountAjc <= 0) return "Amount must be greater than zero.";
        return null;
    }

    public static String requireValidWallet(String address, String role) {
        if (!isValidAjcWallet(address)) {
            return (role != null ? role : "Wallet") + " must be a valid HV0x address.";
        }
        return null;
    }

    public static String requireSlot(int slot) {
        if (slot < 0) return "Invalid slot.";
        return null;
    }

    /**
     * Runs a list of validation checks. Returns the first error message,
     * or null if everything passed.
     */
    public static String firstError(String... errors) {
        if (errors == null) return null;
        for (String e : errors) {
            if (e != null) return e;
        }
        return null;
    }
}
