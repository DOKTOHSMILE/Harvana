package com.harvana.app.social;

import java.util.Locale;

/** Username format rules (pure logic). Usernames are unique, chosen by the user, and stored lower-case. */
public final class UsernameRules {
    private UsernameRules() {}

    public static final int MIN = 3, MAX = 20;
    private static final String[] RESERVED = {"admin", "administrator", "harvana", "harv", "support", "help", "moderator", "mod", "guardian",
        "node", "official", "staff", "system", "root", "wallet", "team", "security", "billing", "null", "undefined", "me", "everyone"};

    /** Lower-case, trimmed, without a leading @. */
    public static String normalize(String raw) {
        if (raw == null) return "";
        String s = raw.trim().toLowerCase(Locale.US);
        while (s.startsWith("@")) s = s.substring(1);
        return s;
    }

    /** Null when the (normalized) name is acceptable, otherwise a plain reason. */
    public static String problem(String normalized) {
        if (normalized == null || normalized.isEmpty()) return "Choose a username.";
        if (normalized.length() < MIN) return "Use at least " + MIN + " characters.";
        if (normalized.length() > MAX) return "Use at most " + MAX + " characters.";
        if (!normalized.matches("[a-z0-9_.]+")) return "Use only letters, numbers, dots and underscores.";
        if (!Character.isLetter(normalized.charAt(0))) return "Start with a letter.";
        char last = normalized.charAt(normalized.length() - 1);
        if (last == '.' || last == '_') return "Don't end with a dot or underscore.";
        if (normalized.contains("..") || normalized.contains("__") || normalized.contains("._") || normalized.contains("_.")) return "Don't put dots or underscores next to each other.";
        for (String r : RESERVED) if (r.equals(normalized)) return "That name is reserved.";
        if (normalized.startsWith("harvana") || normalized.startsWith("admin.") || normalized.startsWith("support.")) return "That name looks official, so it's reserved.";
        return null;
    }
}
