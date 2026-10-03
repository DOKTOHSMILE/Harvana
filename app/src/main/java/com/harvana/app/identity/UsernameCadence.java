package com.harvana.app.identity;

/**
 * A username may change once every 30 days. The previous name stays reserved
 * for 90 days so nobody else can take it and inherit trust or friend links.
 */
public final class UsernameCadence {
    public static final long CHANGE_EVERY_MS = 30L * 24 * 60 * 60 * 1000;
    public static final long HOLD_MS = 90L * 24 * 60 * 60 * 1000;

    private UsernameCadence() {}

    /**
     * Null when moving from {@code oldName} to {@code newName} is allowed at {@code now}.
     * A first claim (no previous name) is always allowed. {@code lastChangedAtMillis}
     * is when the current name was claimed, or null if that stamp is missing.
     */
    public static String problem(String oldName, String newName, Long lastChangedAtMillis, long now) {
        if (oldName == null || oldName.isEmpty() || oldName.equals(newName)) return null;
        if (lastChangedAtMillis == null || lastChangedAtMillis <= 0) return null;
        long next = lastChangedAtMillis + CHANGE_EVERY_MS;
        if (now >= next) return null;
        long days = Math.max(1, (next - now + (24L * 60 * 60 * 1000) - 1) / (24L * 60 * 60 * 1000));
        return "You can change your username again in " + days + (days == 1 ? " day." : " days.");
    }

    /** True while a name released at {@code releasedAtMillis} must not be given to anyone else. */
    public static boolean held(long releasedAtMillis, long now) {
        return now < releasedAtMillis + HOLD_MS;
    }
}
