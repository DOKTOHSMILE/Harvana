package com.harvanna.app.social;

import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Phone numbers and email addresses turned into the anonymous keys used for contact discovery (pure logic).
 * A number is reduced to its international digits ("2348012345678"), then scrambled with SHA-256, so the shared index
 * never stores a readable number or email. The phone's own contact list is scrambled the same way on the phone and only
 * the scrambled keys are compared.
 */
public final class PhoneNumbers {
    private PhoneNumbers() {}

    private static final String[] CODES = {"1", "7", "20", "27", "30", "31", "32", "33", "34", "36", "39", "40", "41", "43", "44", "45", "46", "47", "48", "49",
        "51", "52", "53", "54", "55", "56", "57", "58", "60", "61", "62", "63", "64", "65", "66", "81", "82", "84", "86", "90", "91", "92", "93", "94", "95", "98",
        "211", "212", "213", "216", "218", "220", "221", "222", "223", "224", "225", "226", "227", "228", "229", "230", "231", "232", "233", "234", "235", "236",
        "237", "238", "239", "240", "241", "242", "243", "244", "245", "248", "249", "250", "251", "252", "253", "254", "255", "256", "257", "258", "260", "261",
        "262", "263", "264", "265", "266", "267", "268", "269", "351", "352", "353", "354", "358", "380", "420", "852", "880", "886", "961", "962", "966", "971", "972", "974"};
    private static final Set<String> CODE_SET = new HashSet<>(java.util.Arrays.asList(CODES));

    /** Country calling code at the start of an international number ("2348012345678" -> "234"), or "" when unknown. */
    public static String countryCodeOf(String internationalDigits) {
        if (internationalDigits == null) return "";
        for (int len = 3; len >= 1; len--) {
            if (internationalDigits.length() > len && CODE_SET.contains(internationalDigits.substring(0, len))) return internationalDigits.substring(0, len);
        }
        return "";
    }

    /**
     * International digits for a number as written in a phone book, or "" if it cannot be trusted.
     * {@code defaultCountryCode} (digits, e.g. "234") is used for local numbers such as 0801 234 5678.
     */
    public static String normalize(String raw, String defaultCountryCode) {
        if (raw == null) return "";
        String t = raw.trim();
        boolean plus = t.startsWith("+");
        String d = t.replaceAll("[^0-9]", "");
        if (d.isEmpty()) return "";
        String cc = defaultCountryCode == null ? "" : defaultCountryCode.replaceAll("[^0-9]", "");
        if (plus) { /* already international */ }
        else if (d.startsWith("00")) d = d.substring(2);
        else if (d.startsWith("0") && !cc.isEmpty()) d = cc + d.substring(1);
        else if (!cc.isEmpty() && d.startsWith(cc) && d.length() >= cc.length() + 7) { /* already has the code */ }
        else if (!cc.isEmpty() && d.length() >= 7 && d.length() <= 10) d = cc + d;
        return d.length() >= 8 && d.length() <= 15 ? d : "";
    }

    public static String phoneKey(String internationalDigits) { return sha256("p:" + internationalDigits); }

    public static String emailKey(String email) { return sha256("e:" + (email == null ? "" : email.trim().toLowerCase(Locale.US))); }

    static String sha256(String s) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(s.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : h) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { throw new IllegalStateException(e); }
    }
}
