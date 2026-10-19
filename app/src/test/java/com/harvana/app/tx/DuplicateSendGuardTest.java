package com.harvana.app.tx;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DuplicateSendGuardTest {

    @Test public void sameAmountWrittenDifferentlyMatches() {
        assertEquals(DuplicateSendGuard.amount("1.5"), DuplicateSendGuard.amount("1.50"));
        assertEquals(DuplicateSendGuard.amount("5000"), DuplicateSendGuard.amount("5,000.00"));
        assertEquals("0.00000001", DuplicateSendGuard.amount("0.00000001"));
    }

    @Test public void addressesMatchWhateverTheCase() {
        assertEquals(DuplicateSendGuard.norm("HV0xAbC"), DuplicateSendGuard.norm(" hv0xabc "));
    }
}
