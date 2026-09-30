package com.harvanna.app.social;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class UsernameRulesTest {
    @Test public void normalizes() {
        assertEquals("chinedu.okoro", UsernameRules.normalize("  @Chinedu.Okoro "));
        assertEquals("", UsernameRules.normalize(null));
    }
    @Test public void goodNames() {
        for (String n : new String[]{"ada", "chinedu_o", "ngozi.eze", "user123", "a1b2c3"}) assertNull(n, UsernameRules.problem(n));
    }
    @Test public void badNames() {
        for (String n : new String[]{"", "ab", "1abc", "_abc", "abc.", "abc_", "a..b", "a__b", "has space", "UPPER!", "admin", "harvanna", "harvanna_support", "abcdefghijklmnopqrstu"})
            assertNotNull(n, UsernameRules.problem(UsernameRules.normalize(n)));
    }
}
