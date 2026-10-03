package com.harvana.app.engine;

import static com.harvana.app.engine.Amounts.*;

import java.util.List;
import java.util.Map;

/** Rules for a rotating circle: what the node says, and what the member can do about it. */
public final class CircleSnapshot {
    private CircleSnapshot() {}

    @SuppressWarnings("unchecked")
    public static GroupSnapshot of(Map<String, Object> circle, Map<String, Object> member) {
        GroupSnapshot s = new GroupSnapshot();
        s.family = "CIRCLE";
        s.id = str(circle, "circleId");
        s.state = str(circle, "state");
        s.escrow = str(circle, "escrowAddress");
        long contribution = lng(circle, "contributionAmountRaw");
        int limit = (int) lng(circle, "memberLimit");
        int round = (int) lng(circle, "currentRound");
        Object order = circle.get("payoutOrder");
        int joined = order instanceof List ? ((List<Object>) order).size() : 0;
        boolean forming = "FORMING".equals(s.state);
        boolean active = "ACTIVE".equals(s.state);

        if (forming) {
            s.headline = joined + " of " + limit + " members joined";
            s.progressCurrent = joined; s.progressMax = Math.max(limit, 1);
            if (lng(circle, "formationDeadline") > 0) s.deadlineNote = "Must fill in time or it is cancelled and bonds are returned.";
        } else if (active) {
            s.headline = "Round " + round + " of " + limit;
            s.progressCurrent = Math.min(round, limit); s.progressMax = Math.max(limit, 1);
        } else {
            s.headline = "COMPLETED".equals(s.state) ? "Completed" : "ABORTED".equals(s.state) ? "Cancelled - bonds returned" : s.state;
        }

        s.term("Contribution per round", harv(contribution));
        s.term("Pot per round", harv(contribution * Math.max(0, limit - 1)));
        s.term("Round length", cycle(lng(circle, "cycleDurationHours")));
        s.term("Bond to join", harv(lng(circle, "requiredBondRaw")));
        s.term("Members", String.valueOf(limit));

        boolean isMember = member != null && !member.isEmpty();
        String ms = isMember ? str(member, "state") : "";
        s.myState = ms;
        if (isMember) {
            long payoutRound = lng(member, "payoutRound");
            if (payoutRound > 0) s.my("You are paid in round", String.valueOf(payoutRound));
            s.my("Paid so far", harv(lng(member, "paidRaw")));
            if (member.containsKey("remainingObligationRaw")) s.my("Still to pay", harv(lng(member, "remainingObligationRaw")));
            s.my("Bond locked", harv(lng(member, "bondRemainingRaw")));
            if (lng(member, "penaltyAccruedRaw") > 0) s.my("Penalties", harv(lng(member, "penaltyAccruedRaw")));
            if (lng(member, "payoutReceivedRaw") > 0) s.my("Payout received", harv(lng(member, "payoutReceivedRaw")));
            if ("LATE".equals(ms)) s.notes.add("You're late. Pay now to avoid penalties taken from your bond.");
            if (active && payoutRound > 0 && payoutRound == round) s.notes.add("It's your turn to be paid this round, so nothing is due from you.");
        }

        if (!isMember && forming) {
            s.actions.add(new GroupAction(GroupAction.JOIN, "Join and lock bond", true));
        } else if (isMember && active && ("ACTIVE".equals(ms) || "LATE".equals(ms))
                && !(lng(member, "payoutRound") == round && round > 0)) {
            s.actions.add(new GroupAction(GroupAction.CONTRIBUTE, "Pay " + harv(contribution), true));
        } else if (isMember && "COMPLETED".equals(s.state)) {
            s.actions.add(new GroupAction(GroupAction.WITHDRAW_BOND, "Withdraw bond", true));
        }
        return s;
    }
}
