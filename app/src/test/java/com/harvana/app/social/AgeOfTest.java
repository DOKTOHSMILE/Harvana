package com.harvana.app.social;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Calendar;
import java.util.GregorianCalendar;

public class AgeOfTest {
    private static Calendar day(int y, int m, int d) { return new GregorianCalendar(y, m - 1, d); }

    @Test public void countsWholeYearsFromDayMonthYear() {
        assertEquals(27, AgeOf.fromDob("11/02/1999", day(2026, 10, 5)));
        assertEquals(26, AgeOf.fromDob("11/02/1999", day(2026, 2, 10)));
        assertEquals(27, AgeOf.fromDob("11/02/1999", day(2026, 2, 11)));
    }

    @Test public void readsIsoDatesAndBareYears() {
        assertEquals(36, AgeOf.fromDob("1990-01-01", day(2026, 10, 5)));
        assertEquals(26, AgeOf.fromDob("2000", day(2026, 10, 5)));
    }

    @Test public void returnsMinusOneWhenItCannotBeTrusted() {
        assertEquals(-1, AgeOf.fromDob("", day(2026, 10, 5)));
        assertEquals(-1, AgeOf.fromDob(null, day(2026, 10, 5)));
        assertEquals(-1, AgeOf.fromDob("not a date", day(2026, 10, 5)));
        assertEquals(-1, AgeOf.fromDob("01/01/2030", day(2026, 10, 5)));
        assertEquals(-1, AgeOf.fromDob("01/01/1850", day(2026, 10, 5)));
    }
}
