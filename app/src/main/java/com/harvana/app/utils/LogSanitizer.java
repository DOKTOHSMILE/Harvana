package com.harvana.app.utils;

import android.util.Log;

/**
 * Strip wallet addresses, amounts and other PII from log messages before they hit logcat.
 *
 * Logcat is readable by any app with {@code READ_LOGS} on a rooted device and by ADB. The
 * wallet address is the most sensitive field (it is the user's identity on the network), so
 * the canonical rule is: never put an address in a log message, even at {@code Log.i} level.
 * If a future change needs to log a value that might be an address, route it through
 * {@link #address(String)} which shortens it to a 6+4 suffix that is safe to log.
 *
 * This class is pure Java on purpose so it can be unit-tested on the JVM.
 */
public final class LogSanitizer {
    private LogSanitizer() {}

    /** HV0x… / 0x… / AJ0x… / Bitcoin-style base58 addresses. 24+ chars after the prefix. */
    private static final java.util.regex.Pattern ADDRESS = java.util.regex.Pattern.compile(
            "\\b(?:HV0x|hv0x|AJ0x|aj0x|0x)[0-9a-fA-F]{16,}\\b"   // hex with prefix
    );

    /**
     * Returns {@code addr} unchanged if it does not look like a wallet address (so the caller
     * can pass user-typed strings through), or a shortened form {@code HV0xabcd…wxyz} if it
     * does. The shortened form is safe to log: it identifies which wallet without exposing it.
     */
    public static String address(String addr) {
        if (addr == null || addr.isEmpty()) return "";
        if (addr.length() < 12) return addr;
        // Already an address-shaped string? Shorten it.
        if (addr.startsWith("HV0x") || addr.startsWith("hv0x")
                || addr.startsWith("AJ0x") || addr.startsWith("aj0x")
                || addr.startsWith("0x")) {
            String prefix = addr.substring(0, 4);
            return prefix + addr.substring(4, 8) + "…" + addr.substring(addr.length() - 4);
        }
        return addr;
    }

    /**
     * Returns a copy of {@code msg} with any embedded wallet-shaped strings replaced by
     * their shortened form. Use this if you must log a message that may contain an address
     * (for example, a server response). Better: don't log the address at all.
     */
    public static String sanitize(String msg) {
        if (msg == null || msg.isEmpty()) return msg;
        java.util.regex.Matcher m = ADDRESS.matcher(msg);
        StringBuilder out = new StringBuilder(msg.length());
        int last = 0;
        while (m.find()) {
            out.append(msg, last, m.start());
            String match = m.group();
            out.append(address(match));
            last = m.end();
        }
        if (last < msg.length()) out.append(msg, last, msg.length());
        return out.toString();
    }

    /**
     * Convenience: log at info level with sanitization. Prefer this over a raw
     * {@code Log.i(tag, msg)} whenever {@code msg} might contain an address.
     */
    public static void i(String tag, String msg) {
        Log.i(tag, sanitize(msg));
    }

    /** Convenience: log at warn level with sanitization. */
    public static void w(String tag, String msg) {
        Log.w(tag, sanitize(msg));
    }

    /** Convenience: log at warn level with sanitization and a throwable. */
    public static void w(String tag, String msg, Throwable t) {
        Log.w(tag, sanitize(msg), t);
    }
}
