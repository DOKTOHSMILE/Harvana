package com.harvana.app.wallet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class PinPolicyTest {
    @Test public void weakPinsRefused() {
        for (String p : new String[]{"0000", "1111", "1234", "4321", "2580", "2345", "9876", "2024"}) assertNotNull(p, PinPolicy.weakReason(p));
    }
    @Test public void goodPinsAccepted() {
        for (String p : new String[]{"4827", "9051", "3168"}) assertNull(p, PinPolicy.weakReason(p));
    }
    @Test public void formatChecked() {
        assertNotNull(PinPolicy.weakReason("123"));
        assertNotNull(PinPolicy.weakReason("12a4"));
        assertNotNull(PinPolicy.weakReason(null));
    }
    @Test public void lockRemaining() {
        assertEquals(0L, PinPolicy.lockRemainingMs(1000, 2000));
        assertEquals(500L, PinPolicy.lockRemainingMs(2500, 2000));
    }
}
