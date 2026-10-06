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
        long target = lng(group, "targetGoalAmountRaw"), pooled = lng(group, "raisedAmountRaw");
        // Each goal has its own ledger inside the same group; older groups have no base and read as one goal.
        long raised = Math.max(0L, pooled - lng(group, "goalBaseRaisedRaw"));
        long goalNo = Math.max(1L, lng(group, "goalNumber"));
        String rule = str(group, "disbursementRule");
        boolean admin = !me.isEmpty() && me.equals(str(group, "creatorAddress"));
        s.headline = harv(raised) + " of " + harv(target);
        s.progressCurrent = (int) Math.min(1000, target > 0 ? raised * 1000 / target : 0);
        s.progressMax = 1000;

        if (goalNo > 1) s.term("Goal number", String.valueOf(goalNo));
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
        boolean goalMet = target > 0 && raised >= target;
        boolean completed = "COMPLETED".equals(s.state);
        boolean open = "ACTIVE".equals(s.state) || s.state.isEmpty();
        long left = pooled - lng(group, "totalDisbursedRaw") - lng(group, "paidOutBeforeGoalRaw");
        if (open && !goalMet && !completed) s.actions.add(new GroupAction(GroupAction.FIXED_DEPOSIT, "Deposit", true));
        // Reaching the goal closes deposits. It does not move the money: under every rule except "all at once" the pot stays in the
        // group wallet until the admin (or a member, for grants) asks and the group votes, so those actions stay available.
        boolean treasurer = !me.isEmpty() && me.equals(str(group, "treasurerAddress"));
        if (open && !completed && left > 0) {
            if ("ADMIN_TRIGGERED".equals(rule) && (admin || treasurer)) s.actions.add(new GroupAction(GroupAction.FIXED_REQUEST_DISBURSEMENT, "Request payout (group votes)", false));
            if ("MILESTONE_BASED".equals(rule) && (admin || treasurer)) s.actions.add(new GroupAction(GroupAction.FIXED_REQUEST_MILESTONE, "Request a milestone release", false));
            if ("EMERGENCY_GRANT".equals(rule) && isMember) s.actions.add(new GroupAction(GroupAction.FIXED_REQUEST_GRANT, "Ask for an emergency grant", false));
        }
        if (goalMet || completed) {
            String who = str(group, "beneficiaryAddress");
            who = who.length() > 14 ? who.substring(0, 8) + "…" + who.substring(who.length() - 4) : who;
            if (who.isEmpty()) who = "the wallet chosen when this group was created";
            s.actions.add(GroupAction.closed(FixedGoalText.sentence(rule, s.state, goalMet, harv(raised), who)));
        } else if (!open && !s.state.isEmpty()) {
            s.actions.add(GroupAction.closed("Deposits are paused while this goal is " + s.state + "."));
        }
        if ("ALL_AT_ONCE_AT_GOAL".equals(rule)) {
            s.notes.add("When the goal is reached, the full amount is paid automatically to the wallet chosen at creation. No vote, and deposits stop. A day you skip is not a missed payment.");
        } else if ("ADMIN_TRIGGERED".equals(rule)) {
            s.notes.add("Reaching the goal does not send the money. The admin requests a payout and the group votes.");
        } else if ("MILESTONE_BASED".equals(rule)) {
            s.notes.add("Money leaves in parts. Each part is voted before it is paid.");
        } else if ("EMERGENCY_GRANT".equals(rule)) {
            s.notes.add("Nothing is sent automatically. A member asks, the group votes, then that person is paid.");
        }
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
