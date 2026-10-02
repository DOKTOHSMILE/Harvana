package com.harvanna.app.notify;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class DueCopyTest {
    @Test public void namesTheGroupAndDoesNotRepeatTheTicker() {
        assertEquals("Payment due: FINANCIAL SPARK ROTATION", DueCopy.title("FINANCIAL SPARK ROTATION"));
        assertEquals("Your payment of 0.2 HARV is due.", DueCopy.body("0.2", "HARV"));
        assertEquals("Your payment of 0.2 HARV is due.", DueCopy.body("0.2 HARV", "HARV"));
    }

    @Test public void oneAlertPerGroupPerDay() {
        String day = DueCopy.dayKey(1_700_000_000_000L);
        assertEquals(8, day.length());
        assertFalse(day.contains("-"));
        assertEquals("due_g1_" + day, DueCopy.key("g1", day));
    }
}
