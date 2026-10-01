package com.harvanna.app.identity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Random;

public class PidTest {
    @Test public void issuedIdsAreValidAndTenDigits() {
        Random r = new Random(7);
        for (int i = 0; i < 50; i++) {
            String pid = Pid.issue(r);
            assertEquals(10, pid.length());
            assertTrue(pid, Pid.valid(pid));
        }
    }

    @Test public void aChangedDigitFails() {
        String pid = Pid.issue(new Random(1));
        char[] chars = pid.toCharArray();
        chars[3] = chars[3] == '0' ? '1' : '0';
        assertFalse(Pid.valid(new String(chars)));
    }

    @Test public void rejectsTheWrongShape() {
        assertFalse(Pid.valid(null));
        assertFalse(Pid.valid("123456789"));
        assertFalse(Pid.valid("12345678901"));
        assertFalse(Pid.valid("123456789a"));
    }

    @Test public void checkDigitIsStable() {
        int[] body = {7, 9, 9, 2, 7, 3, 9, 8, 7};
        int check = Pid.checkDigit(body);
        String pid = "799273987" + check;
        assertTrue(Pid.valid(pid));
        assertEquals(check, pid.charAt(9) - '0');
    }
}
