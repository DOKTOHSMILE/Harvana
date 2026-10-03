package com.harvana.app.engine;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class NodePositionsTest {
    private static Map<String, Object> m(Object... kv) {
        Map<String, Object> r = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) r.put((String) kv[i], kv[i + 1]);
        return r;
    }

    @Test public void circleCountsPaidRoundsAndSkipsOwnPayoutRound() {
        Map<String, Object> circle = m("state", "ACTIVE", "currentRound", 4, "memberLimit", 5, "contributionAmountRaw", 1_000_000_000L);
        // rounds 1-3 done; own payout round was 2, so 2 payments expected; paid 2
        Position p = NodePositions.circle(circle, m("state", "ACTIVE", "payoutRound", 2, "paidRaw", 2_000_000_000L, "bondRemainingRaw", 4_000_000_000L));
        assertEquals(2, p.expectedPayments);
        assertEquals(2, p.confirmedPayments);
        assertEquals(0, p.paymentGaps);
        assertEquals(40.0, p.savedHarv, 0.0001);
    }

    @Test public void circleMissedPaymentsAndDefaultsShowAsGaps() {
        Map<String, Object> circle = m("state", "ACTIVE", "currentRound", 4, "memberLimit", 5, "contributionAmountRaw", 1_000_000_000L);
        Position late = NodePositions.circle(circle, m("state", "LATE", "payoutRound", 5, "paidRaw", 2_000_000_000L));
        assertEquals(3, late.expectedPayments);
        assertEquals(2, late.confirmedPayments);
        assertEquals(1, late.paymentGaps);
        Position def = NodePositions.circle(circle, m("state", "DEFAULTED_PRE_PAYOUT", "payoutRound", 5, "paidRaw", 1_000_000_000L));
        assertTrue(def.paymentGaps >= 1);
    }

    @Test public void notAMemberIsNotIn() {
        assertFalse(NodePositions.circle(m(), null).stillIn);
        assertFalse(NodePositions.accum(null, null, 0).stillIn);
        assertFalse(NodePositions.fixed(m(), m()).stillIn);
    }

    @Test public void accumSavedAndDaysPaid() {
        long now = 100L * 86_400_000L;
        Map<String, Object> line = m("status", "ACTIVE", "amountPaidRaw", 500_000_000L, "startTimestamp", now - 10L * 86_400_000L, "daysPaid", 9L);
        Position p = NodePositions.accum(m("borrowedRaw", 300_000_000L, "repaidRaw", 100_000_000L), line, now);
        assertEquals(5.0, p.savedHarv, 0.0001);
        assertEquals(10, p.expectedPayments);
        assertEquals(9, p.confirmedPayments);
        assertEquals(2.0, p.outstandingHarv(), 0.0001);
    }

    @Test public void fixedContributionsAndLoan() {
        Position p = NodePositions.fixed(m("state", "ACTIVE"), m("contributedRaw", 250_000_000L, "borrowedRaw", 100_000_000L, "repaidRaw", 100_000_000L));
        assertEquals(2.5, p.savedHarv, 0.0001);
        assertEquals(0.0, p.outstandingHarv(), 0.0001);
        assertEquals(1, p.confirmedPayments);
    }
}
