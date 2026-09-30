package com.harvanna.app.engine;

import static com.harvanna.app.engine.Amounts.*;

import java.util.Map;

/** Rules for an accumulative group: each member has a savings line; withdrawal is pack, approve, claim. */
public final class AccumSnapshot {
    private AccumSnapshot() {}

    public static GroupSnapshot of(Map<String, Object> group, Map<String, Object> member, Map<String, Object> line, String me) {
        GroupSnapshot s = new GroupSnapshot();
        s.family = "ACCUM";
        s.id = str(group, "groupId");
        s.state = str(group, "state");
        s.escrow = str(group, "escrowAddress");
        boolean admin = !me.isEmpty() && me.equals(str(group, "creatorAddress"));
        s.headline = "ACTIVE".equals(s.state) ? "Personal savings lines" : s.state;

        long bps = lng(group, "interestRateBps");
        s.term("Interest rate", String.format(java.util.Locale.US, "%.2f%%", bps / 100.0));
        s.term("Withdrawal rule", "CYCLE_END_ONLY".equals(str(group, "withdrawalRule")) ? "Only at the end of a cycle" : "Any time (admin approves)");
        s.term("Saved by all members", harv(lng(group, "totalActiveLineBalanceRaw")));
        s.term("Admin", admin ? "You" : "Group admin");

        String lineStatus = line == null ? "" : str(line, "status");
        s.myState = lineStatus;
        boolean hasLine = line != null && !line.isEmpty();
        if (hasLine) {
            long daily = lng(line, "dailyAmountRaw");
            s.my("Line type", str(line, "lineType").isEmpty() ? "FLEXIBLE" : str(line, "lineType"));
            s.my("Daily amount", harv(daily));
            s.my("Days paid", String.valueOf(lng(line, "daysPaid")));
            s.my("Saved in this line", harv(lng(line, "amountPaidRaw")));
            if (lng(line, "payoutRaw") > 0) s.my("Payout", harv(lng(line, "payoutRaw")));
            if ("LOCKED".equals(str(line, "lineType")) && lng(line, "lockedUntilTimestamp") > 0) {
                s.my("Locked until", java.text.DateFormat.getDateInstance().format(new java.util.Date(lng(line, "lockedUntilTimestamp"))));
            }
        }

        if (!hasLine && "ACTIVE".equals(s.state)) {
            s.actions.add(new GroupAction(GroupAction.START_LINE, "Start my savings line", true));
        }
        if (hasLine && "ACTIVE".equals(lineStatus)) {
            String lineId = str(line, "lineId");
            long daily = lng(line, "dailyAmountRaw");
            if ("SINGLE_LUMP_SUM".equals(str(line, "depositMode")) || "VARIABLE_AMOUNT".equals(str(line, "depositMode"))) {
                s.actions.add(new GroupAction(GroupAction.ACCUM_CONTRIBUTE_MULTI, "Add to my line", true, lineId));
            } else {
                s.actions.add(new GroupAction(GroupAction.ACCUM_CONTRIBUTE, "Save today " + harv(daily), true, lineId));
                s.actions.add(new GroupAction(GroupAction.ACCUM_CONTRIBUTE_MULTI, "Save several days", false, lineId));
            }
            boolean lockedNow = "LOCKED".equals(str(line, "lineType")) && lng(line, "lockedUntilTimestamp") > System.currentTimeMillis();
            if (lockedNow) s.notes.add("This line is locked and can't be withdrawn before its date.");
            else s.actions.add(new GroupAction(GroupAction.ACCUM_PACK, "Ask to withdraw (pack)", false, lineId));
        } else if (hasLine && "PENDING_PACK_APPROVAL".equals(lineStatus)) {
            s.notes.add("Waiting for the admin to approve your withdrawal.");
        } else if (hasLine && "CLAIMABLE".equals(lineStatus)) {
            s.actions.add(new GroupAction(GroupAction.ACCUM_CLAIM, "Claim my savings", true, str(line, "lineId")));
        }
        return s;
    }
}
