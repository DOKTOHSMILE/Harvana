package com.harvana.app.group;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class PaymentDueResolverTest {

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    private static long at(int y, int m, int d, int h) {
        Calendar c = Calendar.getInstance(UTC);
        c.clear();
        c.set(y, m - 1, d, h, 0, 0);
        return c.getTimeInMillis();
    }

    private static Map<String, Object> group(String type, String freq, int slot) {
        Map<String, Object> g = new HashMap<>();
        g.put("groupType", type);
        g.put("frequency", freq);
        g.put("status", "ACTIVE");
        if (slot > 0) g.put("currentSlot", slot);
        return g;
    }

    private static List<Map<String, Object>> members(String... uids) {
        List<Map<String, Object>> l = new ArrayList<>();
        for (String u : uids) { Map<String, Object> m = new HashMap<>(); m.put("memberUid", u); m.put("status", "ACTIVE"); l.add(m); }
        return l;
    }

    private static Map<String, Object> pay(String uid, String status, int slot, long ts) {
        Map<String, Object> p = new HashMap<>();
        p.put("memberUid", uid); p.put("type", "CONTRIBUTION"); p.put("status", status);
        if (slot > 0) p.put("slot", slot);
        p.put("timestamp", ts);
        return p;
    }

    @Test public void iOweAndNothingRecordedMeansPay() {
        PaymentDueResolver.Result r = PaymentDueResolver.resolve(group("Rotating Pool", "Weekly", 2),
            members("me", "a", "b"), new ArrayList<>(), "me", at(2026, 9, 30, 12), UTC);
        assertEquals(PaymentDueResolver.Action.PAY, r.action);
        assertEquals(3, r.unpaidCount);
    }

    @Test public void iPaidButAnotherOwesMeansRemindFirstOther() {
        List<Map<String, Object>> pays = new ArrayList<>();
        pays.add(pay("me", "CONFIRMED", 2, at(2026, 9, 29, 10)));
        pays.add(pay("b", "CONFIRMED", 2, at(2026, 9, 29, 11)));
        PaymentDueResolver.Result r = PaymentDueResolver.resolve(group("Rotating Pool", "Weekly", 2),
            members("me", "a", "b"), pays, "me", at(2026, 9, 30, 12), UTC);
        assertEquals(PaymentDueResolver.Action.REMIND, r.action);
        assertEquals("a", r.otherMemberUid);
    }

    @Test public void everyonePaidMeansNoneAndPaid() {
        List<Map<String, Object>> pays = new ArrayList<>();
        for (String u : new String[]{"me", "a"}) pays.add(pay(u, "CONFIRMED", 3, at(2026, 9, 29, 10)));
        PaymentDueResolver.Result r = PaymentDueResolver.resolve(group("Rotating Pool", "Weekly", 3),
            members("me", "a"), pays, "me", at(2026, 9, 30, 12), UTC);
        assertEquals(PaymentDueResolver.Action.NONE, r.action);
        assertEquals("Paid", r.badge);
        assertNull(r.otherMemberUid);
    }

    @Test public void myPendingPaymentIsConfirmingNotPayAgain() {
        List<Map<String, Object>> pays = new ArrayList<>();
        pays.add(pay("me", "PENDING", 1, at(2026, 9, 30, 9)));
        PaymentDueResolver.Result r = PaymentDueResolver.resolve(group("Rotating Pool", "Weekly", 1),
            members("me"), pays, "me", at(2026, 9, 30, 12), UTC);
        assertEquals(PaymentDueResolver.Action.NONE, r.action);
        assertEquals("Confirming", r.badge);
    }

    @Test public void previousSlotPaymentDoesNotCountForCurrentSlot() {
        List<Map<String, Object>> pays = new ArrayList<>();
        pays.add(pay("me", "CONFIRMED", 1, at(2026, 9, 1, 10)));
        PaymentDueResolver.Result r = PaymentDueResolver.resolve(group("Rotating Pool", "Weekly", 2),
            members("me"), pays, "me", at(2026, 9, 30, 12), UTC);
        assertEquals(PaymentDueResolver.Action.PAY, r.action);
    }

    /** Goal groups ("Fixed Contribution") have no schedule: a quiet day is not a missed payment, so they never say
     *  "Payment due" (owner report, 12 Oct 2026: a reached goal kept sending daily alerts). */
    @Test public void goalGroupsNeverRaisePaymentDue() {
        List<Map<String, Object>> pays = new ArrayList<>();
        pays.add(pay("me", "CONFIRMED", 0, at(2026, 8, 30, 10)));   // last month only
        PaymentDueResolver.Result r = PaymentDueResolver.resolve(group("Fixed Contribution", "Daily", 0),
            members("me"), pays, "me", at(2026, 9, 30, 12), UTC);
        assertEquals(PaymentDueResolver.Action.NONE, r.action);
        Map<String, Object> g = group("Group", "Daily", 0);
        g.put("nodeGroupId", "FIXED-abc");
        assertEquals(PaymentDueResolver.Action.NONE, PaymentDueResolver.resolve(g, members("me"), new ArrayList<>(), "me",
            at(2026, 9, 30, 12), UTC).action);
    }

    @Test public void goalGroupsWithAPerPeriodAmountAreDueOnlyWhenThisPeriodIsUnpaid() {
        Map<String, Object> g = group("Fixed Contribution", "Daily", 0);
        g.put("contributionAmount", 10);
        g.put("targetGoalAmount", 100);
        g.put("raisedAmount", 20);
        List<Map<String, Object>> pays = new ArrayList<>();
        pays.add(pay("me", "CONFIRMED", 0, at(2026, 9, 29, 10)));            // yesterday only
        assertEquals(PaymentDueResolver.Action.PAY,
            PaymentDueResolver.resolve(g, members("me"), pays, "me", at(2026, 9, 30, 12), UTC).action);
        pays.add(pay("me", "CONFIRMED", 0, at(2026, 9, 30, 9)));             // today
        assertEquals(PaymentDueResolver.Action.NONE,
            PaymentDueResolver.resolve(g, members("me"), pays, "me", at(2026, 9, 30, 12), UTC).action);
        g.put("raisedAmount", 100);                                           // goal reached: never due
        assertEquals(PaymentDueResolver.Action.NONE,
            PaymentDueResolver.resolve(g, members("me"), new ArrayList<>(), "me", at(2026, 9, 30, 12), UTC).action);
        g.put("raisedAmount", 20);
        g.put("state", "DISSOLVED");                                          // money returned: never due
        assertEquals(PaymentDueResolver.Action.NONE,
            PaymentDueResolver.resolve(g, members("me"), new ArrayList<>(), "me", at(2026, 9, 30, 12), UTC).action);
    }

    @Test public void theNetworksLastDepositCountsForThisPeriod() {
        assertTrue(PaymentDueResolver.depositedThisPeriod("Daily", at(2026, 9, 30, 8), at(2026, 9, 30, 12), UTC));
        assertFalse(PaymentDueResolver.depositedThisPeriod("Daily", at(2026, 9, 29, 23), at(2026, 9, 30, 12), UTC));
        assertFalse(PaymentDueResolver.depositedThisPeriod("Daily", null, at(2026, 9, 30, 12), UTC));
    }

    @Test public void accumulativeGroupsHaveNoSchedule() {
        PaymentDueResolver.Result r = PaymentDueResolver.resolve(group("Accumulative Funds", "Daily", 0),
            members("me"), new ArrayList<>(), "me", at(2026, 9, 30, 12), UTC);
        assertEquals(PaymentDueResolver.Action.NONE, r.action);
    }

    @Test public void inactiveMembersDoNotBlockAndInactiveGroupIsNone() {
        List<Map<String, Object>> ms = members("me");
        Map<String, Object> left = new HashMap<>(); left.put("memberUid", "x"); left.put("status", "LEFT"); ms.add(left);
        List<Map<String, Object>> pays = new ArrayList<>();
        pays.add(pay("me", "CONFIRMED", 1, at(2026, 9, 30, 9)));
        assertEquals(PaymentDueResolver.Action.NONE,
            PaymentDueResolver.resolve(group("Rotating Pool", "Weekly", 1), ms, pays, "me", at(2026, 9, 30, 12), UTC).action);
        Map<String, Object> closed = group("Rotating Pool", "Weekly", 1);
        closed.put("status", "CLOSED");
        assertEquals(PaymentDueResolver.Action.NONE,
            PaymentDueResolver.resolve(closed, members("me"), new ArrayList<>(), "me", at(2026, 9, 30, 12), UTC).action);
    }

    @Test public void weekStartsMonday() {
        // Wed 30 Sep 2026 -> Monday 28 Sep 2026 00:00
        assertEquals(at(2026, 9, 28, 0), PaymentDueResolver.periodStartMs("Weekly", at(2026, 9, 30, 12), UTC));
        assertEquals(at(2026, 9, 1, 0), PaymentDueResolver.periodStartMs("Monthly", at(2026, 9, 30, 12), UTC));
        assertEquals(at(2026, 9, 30, 0), PaymentDueResolver.periodStartMs("Daily", at(2026, 9, 30, 12), UTC));
    }
}
