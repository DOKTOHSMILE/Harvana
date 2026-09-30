package com.harvanna.app.social;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

public class PhoneNumbersTest {
    @Test public void localAndInternationalFormsMatch() {
        String a = PhoneNumbers.normalize("0801 234 5678", "234");
        assertEquals("2348012345678", a);
        assertEquals(a, PhoneNumbers.normalize("+234 801 234 5678", "234"));
        assertEquals(a, PhoneNumbers.normalize("00234-801-234-5678", "234"));
        assertEquals(a, PhoneNumbers.normalize("234 801 234 5678", "234"));
        assertEquals(a, PhoneNumbers.normalize("(801) 234-5678", "234"));
    }
    @Test public void otherCountryStaysOtherCountry() {
        assertEquals("447911123456", PhoneNumbers.normalize("+44 7911 123456", "234"));
    }
    @Test public void junkIsRejected() {
        assertEquals("", PhoneNumbers.normalize("", "234"));
        assertEquals("", PhoneNumbers.normalize("abc", "234"));
        assertEquals("", PhoneNumbers.normalize("123", "234"));
        assertEquals("", PhoneNumbers.normalize("+1234567890123456789", "234"));
    }
    @Test public void detectsCountryCode() {
        assertEquals("234", PhoneNumbers.countryCodeOf("2348012345678"));
        assertEquals("44", PhoneNumbers.countryCodeOf("447911123456"));
        assertEquals("1", PhoneNumbers.countryCodeOf("14155550123"));
        assertEquals("", PhoneNumbers.countryCodeOf("99999999"));
    }
    @Test public void keysAreStableAndAnonymous() {
        assertEquals(PhoneNumbers.phoneKey("2348012345678"), PhoneNumbers.phoneKey("2348012345678"));
        assertNotEquals(PhoneNumbers.phoneKey("2348012345678"), PhoneNumbers.phoneKey("2348012345679"));
        assertEquals(64, PhoneNumbers.phoneKey("2348012345678").length());
        assertEquals(PhoneNumbers.emailKey("Ada@Example.com "), PhoneNumbers.emailKey("ada@example.com"));
        assertNotEquals(PhoneNumbers.phoneKey("2348012345678"), PhoneNumbers.emailKey("2348012345678"));
    }
}
