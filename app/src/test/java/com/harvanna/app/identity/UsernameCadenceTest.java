package com.harvanna.app.identity;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class UsernameCadenceTest {
    @Test public void firstClaimAndSameNameAreFree() {
        assertNull(UsernameCadence.problem("", "ada", null, 0));
        assertNull(UsernameCadence.problem("ada", "ada", 1L, 2L));
        assertNull(UsernameCadence.problem("ada", "ngozi", null, 5L));
    }

    @Test public void changeIsBlockedForThirtyDays() {
        long claimed = 1_000_000L;
        String blocked = UsernameCadence.problem("ada", "ngozi", claimed, claimed + UsernameCadence.CHANGE_EVERY_MS - 1);
        assertTrue(blocked != null && blocked.contains("day"));
        assertNull(UsernameCadence.problem("ada", "ngozi", claimed, claimed + UsernameCadence.CHANGE_EVERY_MS));
    }

    @Test public void oldNameIsHeldForNinetyDays() {
        long released = 50L;
        assertTrue(UsernameCadence.held(released, released + UsernameCadence.HOLD_MS - 1));
        assertFalse(UsernameCadence.held(released, released + UsernameCadence.HOLD_MS));
    }
}
