package com.harvana.app.social;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/** Age in whole years from a stored date of birth. Pure Java so a JVM test covers it. */
public final class AgeOf {
    private AgeOf() {}

    /** The date may be dd/MM/yyyy, d/M/yyyy, yyyy-MM-dd or just a year. -1 when it cannot be read or is not a believable age. */
    public static int fromDob(String dob, Calendar now) {
        if (dob == null || dob.trim().isEmpty()) return -1;
        String s = dob.trim();
        for (String pattern : new String[] {"dd/MM/yyyy", "yyyy-MM-dd", "d/M/yyyy"}) {
            try {
                SimpleDateFormat f = new SimpleDateFormat(pattern, Locale.US);
                f.setLenient(false);
                Calendar born = Calendar.getInstance();
                born.setTime(f.parse(s));
                return years(born, now);
            } catch (Exception ignored) { }
        }
        if (s.matches("^(19|20)[0-9]{2}$")) return check(now.get(Calendar.YEAR) - Integer.parseInt(s));
        return -1;
    }

    /** The date of birth as a date, or null when it is only a year or cannot be read. */
    public static java.util.Date parse(String dob) {
        if (dob == null) return null;
        for (String pattern : new String[] {"dd/MM/yyyy", "yyyy-MM-dd", "d/M/yyyy"}) {
            try {
                SimpleDateFormat f = new SimpleDateFormat(pattern, Locale.US);
                f.setLenient(false);
                return f.parse(dob.trim());
            } catch (Exception ignored) { }
        }
        return null;
    }

    static int years(Calendar born, Calendar now) {
        int age = now.get(Calendar.YEAR) - born.get(Calendar.YEAR);
        if (now.get(Calendar.DAY_OF_YEAR) < born.get(Calendar.DAY_OF_YEAR)) age--;
        return check(age);
    }

    private static int check(int age) { return age >= 0 && age < 130 ? age : -1; }
}
