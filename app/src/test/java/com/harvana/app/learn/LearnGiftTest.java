package com.harvana.app.learn;

import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

public class LearnGiftTest {

    @Test public void harvanaGetsFivePercentAndTheAuthorTheRest() {
        BigDecimal[] p = LearnGift.split(new BigDecimal("1"));
        assertEquals(0, p[0].compareTo(new BigDecimal("0.05")));
        assertEquals(0, p[1].compareTo(new BigDecimal("0.95")));
        BigDecimal[] small = LearnGift.split(new BigDecimal("0.1"));
        assertEquals(0, small[0].add(small[1]).compareTo(new BigDecimal("0.1")));
    }

    @Test public void theTreasuryIsTheNodesDerivedAddress() {
        assertEquals("HV0x5ce3ef914c96f22e630c6b4b73aaceef1ec5f49a", LearnGift.TREASURY);
    }
}
