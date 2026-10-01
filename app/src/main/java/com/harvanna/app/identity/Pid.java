package com.harvanna.app.identity;

import java.security.SecureRandom;
import java.util.Random;

/**
 * Permanent 10-digit Harvanna ID. Random, not derived from the Firebase uid.
 * The last digit is a Luhn check digit so a typed ID with one wrong digit is rejected.
 * The ID never changes. It is the public key; the Firebase uid stays private.
 */
public final class Pid {
    public static final int LENGTH = 10;

    private Pid() {}

    public static String issue() {
        return issue(new SecureRandom());
    }

    public static String issue(Random random) {
        int[] body = new int[LENGTH - 1];
        for (int i = 0; i < body.length; i++) body[i] = random.nextInt(10);
        int check = checkDigit(body);
        StringBuilder sb = new StringBuilder(LENGTH);
        for (int n : body) sb.append(n);
        sb.append(check);
        return sb.toString();
    }

    /** True only for 10 digits whose last digit is the Luhn check. */
    public static boolean valid(String pid) {
        if (pid == null || pid.length() != LENGTH) return false;
        int[] digits = new int[LENGTH];
        for (int i = 0; i < LENGTH; i++) {
            char c = pid.charAt(i);
            if (c < '0' || c > '9') return false;
            digits[i] = c - '0';
        }
        int[] body = new int[LENGTH - 1];
        System.arraycopy(digits, 0, body, 0, body.length);
        return checkDigit(body) == digits[LENGTH - 1];
    }

    /** Check digit for the first 9 digits. The rightmost body digit is doubled, because the check sits at the end. */
    static int checkDigit(int[] body) {
        int sum = 0;
        boolean doubleIt = true;
        for (int i = body.length - 1; i >= 0; i--) {
            int n = body[i];
            if (doubleIt) {
                n *= 2;
                if (n > 9) n -= 9;
            }
            sum += n;
            doubleIt = !doubleIt;
        }
        return (10 - (sum % 10)) % 10;
    }
}
