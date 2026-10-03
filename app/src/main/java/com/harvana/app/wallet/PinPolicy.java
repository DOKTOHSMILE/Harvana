package com.harvana.app.wallet;

/** Rules for the transaction PIN: which PINs are too easy to guess, and how long a run of wrong entries locks it. */
public final class PinPolicy {
    private PinPolicy() {}

    public static final int MAX_FAILURES = 5;
    public static final long LOCKOUT_MS = 5 * 60 * 1000L;

    private static final String[] COMMON = {"0000", "1234", "4321", "1212", "2580", "0852", "1004", "2000", "2020", "2021", "2022", "2023", "2024", "2025", "2026", "6969", "1122", "1010", "0007"};

    /** Null when the PIN is acceptable, otherwise the reason it is refused. */
    public static String weakReason(String pin) {
        if (pin == null || !pin.matches("\\d{4}")) return "PIN must be exactly 4 digits.";
        boolean same = true, up = true, down = true;
        for (int i = 1; i < 4; i++) {
            int d = pin.charAt(i) - pin.charAt(i - 1);
            if (d != 0) same = false;
            if (d != 1) up = false;
            if (d != -1) down = false;
        }
        if (same) return "That PIN is too easy to guess (repeated digits). Choose another.";
        if (up || down) return "That PIN is too easy to guess (a run of digits). Choose another.";
        for (String c : COMMON) if (c.equals(pin)) return "That PIN is one of the most common. Choose another.";
        return null;
    }

    /** Lock time remaining, or 0 when not locked. */
    public static long lockRemainingMs(long lockedUntil, long now) {
        return lockedUntil > now ? lockedUntil - now : 0L;
    }
}
