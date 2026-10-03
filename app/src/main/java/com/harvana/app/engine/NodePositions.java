package com.harvana.app.engine;

import static com.harvana.app.engine.Amounts.*;

import java.util.Map;

/** Turns the node's member records into a Position (for Home totals and the trust score). */
public final class NodePositions {
    private static final double RAW = 100_000_000.0;
    private static final long DAY_MS = 24L * 60 * 60 * 1000;

    private NodePositions() {}

    public static Position circle(Map<String, Object> circle, Map<String, Object> member) {
        Position p = new Position();
        p.family = "CIRCLE";
        if (member == null || member.isEmpty()) { p.stillIn = false; return p; }
        long c = lng(circle, "contributionAmountRaw");
        int limit = (int) lng(circle, "memberLimit");
        int round = (int) lng(circle, "currentRound");
        String state = str(circle, "state"), ms = str(member, "state");
        long payoutRound = lng(member, "payoutRound");
        p.savedHarv = lng(member, "bondRemainingRaw") / RAW;
        p.joinedMs = lng(member, "joinedTimestamp");
        int completedRounds = "COMPLETED".equals(state) ? limit : "ACTIVE".equals(state) ? Math.max(0, round - 1) : 0;
        int expected = completedRounds - (payoutRound > 0 && payoutRound <= completedRounds ? 1 : 0);
        int paid = c > 0 ? (int) (lng(member, "paidRaw") / c) : 0;
        p.expectedPayments = Math.max(0, expected);
        p.confirmedPayments = Math.max(0, Math.min(p.expectedPayments, paid));
        p.paymentGaps = ms.startsWith("DEFAULTED") ? Math.max(1, p.expectedPayments - p.confirmedPayments)
            : "LATE".equals(ms) ? 1 : 0;
        p.stillIn = !"ABORTED".equals(state) && !"WITHDRAWN".equals(ms);
        p.cycleCompleted = "COMPLETED".equals(state) && "COMPLETED".equals(ms) || "WITHDRAWN".equals(ms);
        return p;
    }

    public static Position accum(Map<String, Object> member, Map<String, Object> line, long nowMs) {
        Position p = new Position();
        p.family = "ACCUM";
        if (member == null || member.isEmpty()) { p.stillIn = false; return p; }
        p.borrowedHarv = lng(member, "borrowedRaw") / RAW;
        p.repaidHarv = lng(member, "repaidRaw") / RAW;
        p.joinedMs = lng(member, "joinedTimestamp");
        p.cycleCompleted = lng(member, "totalLinesCompleted") > 0;
        if (line != null && !line.isEmpty()) {
            String st = str(line, "status");
            boolean live = "ACTIVE".equals(st) || "PENDING_PACK_APPROVAL".equals(st) || "CLAIMABLE".equals(st);
            p.savedHarv = live ? lng(line, "amountPaidRaw") / RAW : 0;
            long start = lng(line, "startTimestamp");
            long daysElapsed = start > 0 ? Math.min(30, Math.max(0, (nowMs - start) / DAY_MS)) : 0;
            p.expectedPayments = (int) daysElapsed;
            p.confirmedPayments = (int) Math.min(daysElapsed, lng(line, "daysPaid"));
            p.paymentGaps = Math.max(0, p.expectedPayments - p.confirmedPayments) > 2 ? 1 : 0;
        }
        return p;
    }

    public static Position fixed(Map<String, Object> group, Map<String, Object> member) {
        Position p = new Position();
        p.family = "FIXED";
        if (member == null || member.isEmpty()) { p.stillIn = false; return p; }
        p.savedHarv = lng(member, "contributedRaw") / RAW;
        p.borrowedHarv = lng(member, "borrowedRaw") / RAW;
        p.repaidHarv = lng(member, "repaidRaw") / RAW;
        p.joinedMs = lng(member, "joinedTimestamp");
        p.expectedPayments = 1;
        p.confirmedPayments = p.savedHarv > 0 ? 1 : 0;
        p.cycleCompleted = "COMPLETED".equals(str(group, "state")) && p.savedHarv > 0;
        return p;
    }
}
