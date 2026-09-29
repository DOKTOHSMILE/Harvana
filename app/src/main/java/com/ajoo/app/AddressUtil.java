package com.ajoo.app;

/**
 * Utility class for handling, formatting, and validating Ajoo Chain wallet addresses.
 */
public class AddressUtil {

    private static final java.util.regex.Pattern ADDRESS_PATTERN =
            java.util.regex.Pattern.compile("^HV0x[0-9a-f]{40}$");

    private static final java.util.regex.Pattern BLOCK_HASH_PATTERN =
            java.util.regex.Pattern.compile("^HV0x[0-9a-f]{64}$");

    public static boolean isValidBlockHash(String hash) {
        if (hash == null) return false;
        return BLOCK_HASH_PATTERN.matcher(hash).matches();
    }

    /**
     * Validates if the provided string is a properly formatted Ajoo Chain address.
     * Enforces the mandatory "HV0x" prefix rule.
     */
    public static boolean isValidAddress(String address) {
        if (address == null) return false;
        return ADDRESS_PATTERN.matcher(address).matches();
    }

    /**
     * Truncates a long wallet address for cleaner UI display.
     * Example: "HV0x1234567890abcdef1234567890abcdef123456" -> "HV0x1234...3456"
     */
    public static String truncateAddress(String address) {
        if (address == null || address.trim().isEmpty()) {
            return "";
        }
        
        String normalized = address.trim();
        
        if (normalized.length() <= 12) {
            return normalized;
        }
        
        // Keep the HV0x + first 4 chars, add ellipsis, keep last 4 chars
        return normalized.substring(0, 8) + "..." + normalized.substring(normalized.length() - 4);
    }
    
    /**
     * Helper to safely format a token and address together for UI display.
     * Example: "AJUSD (HV0xABCD...1234)"
     */
    public static String formatTokenWithAddress(String symbol, String contractAddress) {
        String tokenSymbol = (symbol != null) ? symbol : "Unknown";
        String truncated = truncateAddress(contractAddress);
        
        if (truncated.isEmpty()) {
            return tokenSymbol;
        }
        
        return tokenSymbol + " (" + truncated + ")";
    }
}
