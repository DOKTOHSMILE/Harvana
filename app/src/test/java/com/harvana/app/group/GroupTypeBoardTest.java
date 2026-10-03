package com.harvana.app.group;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class GroupTypeBoardTest {

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    private static long at(int y, int m, int d) {
        Calendar c = Calendar.getInstance(UTC);
        c.clear();
        c.set(y, m - 1, d, 12, 0, 0);
        return c.getTimeInMillis();
    }

    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put(String.valueOf(kv[i]), kv[i + 1]);
        return m;
    }

    @Test
    public void accumulativeStampsWriteThePaymentDateInTheBox() {
        long sep30 = at(2026, 9, 30);
        long oct2 = at(2026, 10, 2);
        List<Map<String, Object>> payments = new ArrayList<>();
        payments.add(map("status", "CONFIRMED", "memberUid", "me", "memberName", "Ada",
                "amountHarv", 500d, "timestamp", sep30, "type", "CONTRIBUTION"));
        payments.add(map("status", "PENDING", "memberUid", "me", "amountHarv", 200d,
                "timestamp", oct2, "type", "ACCUM"));
        payments.add(map("status", "CONFIRMED", "memberUid", "other", "amountHarv", 900d,
                "timestamp", sep30, "type", "CONTRIBUTION"));
        payments.add(map("status", "CONFIRMED", "memberUid", "me", "amountHarv", 50d,
                "timestamp", sep30, "type", "LOAN_REPAY"));

        GroupTypeBoard.Board board = GroupTypeBoard.fromFirestore(
                map("groupType", "Accumulative Funds"),
                null, payments, "me", true, sep30, UTC);

        assertEquals(GroupTypeBoard.ACCUMULATIVE, board.kind);
        assertEquals(2, board.cells.size());
        assertEquals("30", board.cells.get(0).top);
        assertEquals("Sep", board.cells.get(0).bottom);
        assertEquals(GroupTypeBoard.PAID, board.cells.get(0).state);
        assertEquals("2", board.cells.get(1).top);
        assertEquals("Oct", board.cells.get(1).bottom);
        assertEquals(GroupTypeBoard.PENDING, board.cells.get(1).state);
        assertTrue(board.subtitle.contains("500"));
        assertEquals("Your savings card", board.title);

        GroupTypeBoard.Board everyone = GroupTypeBoard.fromFirestore(
                map("groupType", "Accumulative Funds"),
                null, payments, "me", false, sep30, UTC);
        assertEquals(3, everyone.cells.size());
        assertEquals("Members' savings cards", everyone.title);
    }

    @Test
    public void fixedCalendarWritesTheMonthInsideEachDay() {
        long sep30 = at(2026, 9, 30);
        Map<String, Object> me = map("memberUid", "me", "fixedMonthKey", "2026-09", "fixedDaysPaid", ints(1, 2));
        List<Map<String, Object>> members = new ArrayList<>();
        members.add(me);
        GroupTypeBoard.Board board = GroupTypeBoard.fromFirestore(
                map("groupType", "Fixed Contribution"),
                members, null, "me", true, sep30, UTC);

        assertEquals(31, board.cells.size());
        assertEquals("1", board.cells.get(0).top);
        assertEquals("Sep", board.cells.get(0).bottom);
        assertEquals(GroupTypeBoard.PAID, board.cells.get(0).state);
        assertEquals(GroupTypeBoard.TODAY, board.cells.get(29).state);
        assertTrue(board.cells.get(29).payToday);
        assertEquals(GroupTypeBoard.MISSED, board.cells.get(2).state);
    }

    @Test
    public void rotatingSlotShowsWhoAndTheDateTheyPaid() {
        long paidOn = at(2026, 9, 12);
        List<Map<String, Object>> members = new ArrayList<>();
        members.add(map("memberUid", "a", "slot", 1, "displayName", "Adaeze"));
        members.add(map("memberUid", "b", "slot", 2, "displayName", "Bola"));
        List<Map<String, Object>> payments = new ArrayList<>();
        payments.add(map("status", "CONFIRMED", "memberUid", "a", "round", 1L, "timestamp", paidOn, "amountHarv", 100d));

        GroupTypeBoard.Board board = GroupTypeBoard.fromFirestore(
                map("groupType", "Rotating Pool", "totalSlots", 2, "currentRound", 2),
                members, payments, "b", false, paidOn, UTC);

        assertEquals(2, board.cells.size());
        assertEquals(GroupTypeBoard.OPEN, board.cells.get(0).state);
        assertEquals("Adaeze", board.cells.get(0).bottom);
        assertEquals(GroupTypeBoard.TURN, board.cells.get(1).state);
        assertEquals("Bola", board.nextRecipient);
    }

    @Test
    public void nodeSavingsLineWritesEachDaysDate() {
        long start = at(2026, 9, 1);
        Map<String, Object> line = map("startTimestamp", start, "daysPaid", 2L, "amountPaidRaw", 200_000_000L);
        GroupTypeBoard.Board board = GroupTypeBoard.fromNode(
                "ACCUM", null, null, line, "HV0xme", at(2026, 9, 4), UTC);
        assertEquals(30, board.cells.size());
        assertEquals("1", board.cells.get(0).top);
        assertEquals("Sep", board.cells.get(0).bottom);
        assertEquals(GroupTypeBoard.PAID, board.cells.get(0).state);
        assertEquals(GroupTypeBoard.PAID, board.cells.get(1).state);
        assertEquals(GroupTypeBoard.MISSED, board.cells.get(2).state);
        assertEquals(GroupTypeBoard.TODAY, board.cells.get(3).state);
    }

    private static List<Integer> ints(int... n) {
        List<Integer> l = new ArrayList<>();
        for (int i : n) l.add(i);
        return l;
    }
}
