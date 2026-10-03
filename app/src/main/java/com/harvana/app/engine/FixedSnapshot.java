package com.harvana.app.engine;

import static com.harvana.app.engine.Amounts.*;

import java.util.Map;

/** Rules for a fixed-goal group: deposits in; money out only by the rule and, mostly, a weighted vote. */
public final class FixedSnapshot {
    private FixedSnapshot() {}

    public static GroupSnapshot of(Map<String, Object> group, Map<String, Object> member, String me) {
        GroupSnapshot s = new GroupSnapshot();
        s.family = "FIXED";
        s.id = str(group, "groupId");
        s.state = str(group, "state");
        s.escrow = str(group, "escrowAddress");
        long target = lng(group, "targetGoalAmountRaw"), raised = lng(group, "raisedAmountRaw");
        String rule = str(group, "disbursementRule");
        boolean admin = !me.isEmpty() && me.equals(str(group, "creatorAddress"));
        s.headline = harv(raised) + " of " + harv(target);
        s.progressCurrent = (int) Math.min(1000, target > 0 ? raised * 1000 / target : 0);
        s.progressMax = 1000;

        s.term("Goal", harv(target));
        s.term("Raised", harv(raised));
        s.term("Paid out so far", harv(lng(group, "totalDisbursedRaw")));
        s.term("How money is paid out", ruleLabel(rule));
        if (!str(group, "beneficiaryAddress").isEmpty()) s.term("Paid to", shortAddr(str(group, "beneficiaryAddress")));
        s.term("Admin", admin ? "You" : "Group admin");

        boolean isMember = member != null && !member.isEmpty();
        if (isMember) {
            s.myState = "MEMBER";
            s.my("You have put in", harv(lng(member, "contributedRaw")));
        }
        boolean active = "ACTIVE".equals(s.state);
        if (active) {
            s.actions.add(new GroupAction(GroupAction.FIXED_DEPOSIT, "Deposit", true));
            if ("ADMIN_TRIGGERED".equals(rule) && admin) s.actions.add(new GroupAction(GroupAction.FIXED_REQUEST_DISBURSEMENT, "Request payout (group votes)", false));
            if ("MILESTONE_BASED".equals(rule) && admin) s.actions.add(new GroupAction(GroupAction.FIXED_REQUEST_MILESTONE, "Request a milestone release", false));
            if ("EMERGENCY_GRANT".equals(rule) && isMember) s.actions.add(new GroupAction(GroupAction.FIXED_REQUEST_GRANT, "Ask for an emergency grant", false));
        } else if (!s.state.isEmpty()) {
            s.actions.add(GroupAction.closed("This goal is closed"));
        }
        if ("ALL_AT_ONCE_AT_GOAL".equals(rule)) s.notes.add("When the goal is reached, the full amount is paid to the beneficiary automatically. No vote is needed.");
        return s;
    }

    static String ruleLabel(String rule) {
        switch (rule) {
            case "ALL_AT_ONCE_AT_GOAL": return "All at once when the goal is reached";
            case "ADMIN_TRIGGERED": return "Admin asks, the group votes";
            case "MILESTONE_BASED": return "Released in milestones, each voted";
            case "EMERGENCY_GRANT": return "Emergency grants, each voted";
            default: return rule;
        }
    }

    static String shortAddr(String a) { return a.length() > 14 ? a.substring(0, 8) + "…" + a.substring(a.length() - 4) : a; }
}
