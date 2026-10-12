package com.harvana.app.group;

import java.util.ArrayList;
import java.util.List;

/**
 * Decides whether, and how, a group may be closed. Pure logic (no Android, no network) so every rule is unit-tested.
 *
 * What the network allows: it never erases a group. A circle that fails to fill is ABORTED by the node itself when
 * its formation deadline passes (bonds refunded); a completed circle keeps its history; accumulative and fixed
 * groups have no cancel at all. Money and history therefore stay on the network for good. So there are only two
 * outcomes the app can offer:
 *   ARCHIVE - close the group in the app: off Discover, joining stops, members keep their history, admin can reopen.
 *   DELETE  - remove the app's own records for a group that never touched the network and has nothing open.
 *   REFUND_AND_DELETE - a goal (FIXED) group nobody else has joined: the node closes it and sends whatever its wallet holds
 *             back to the admin (all of it is theirs), then the app's records are removed.
 * and a third, NONE, with plain reasons and next steps when neither is safe.
 */
public final class GroupClosePolicy {
    private GroupClosePolicy() {}

    public enum Action { NONE, ARCHIVE, DELETE, REFUND_AND_DELETE }

    public static final class Facts {
        public boolean admin;
        public boolean alreadyClosed;
        public boolean onChain;
        /** The group's id on the network (empty when not on the network). */
        public String nodeId = "";
        /** CIRCLE, ACCUM or FIXED when onChain. */
        public String family = "";
        /** The network's state for the group ("FORMING", "ACTIVE", "COMPLETED", "ABORTED"); empty when unknown. */
        public String nodeState = "";
        /** Coins still held by the group's network escrow (raw units); -1 when unknown. */
        public long escrowBalanceRaw = -1;
        /** The group's coin ("" for HARV). */
        public String asset = "";
        /** Active members other than the admin. */
        public int otherActiveMembers;
        public int openLoans;
        public int pendingPayoutRequests;
    }

    public static final class Decision {
        public final Action action;
        public final String headline;
        public final List<String> reasons = new ArrayList<>();
        public final List<String> nextSteps = new ArrayList<>();
        Decision(Action a, String h) { action = a; headline = h; }
    }

    public static Decision decide(Facts f) {
        if (!f.admin) {
            Decision d = new Decision(Action.NONE, "Only the admin can close this group");
            d.reasons.add("Closing a group is an admin decision. You can leave the group from the menu instead.");
            return d;
        }
        if (f.alreadyClosed) {
            Decision d = new Decision(Action.NONE, "This group is already closed");
            d.nextSteps.add("You can reopen it from here if you change your mind.");
            return d;
        }

        Decision d = new Decision(Action.NONE, "This group can't be closed yet");
        // Blockers that apply to every group.
        if (f.openLoans > 0) {
            d.reasons.add(f.openLoans + (f.openLoans == 1 ? " loan is" : " loans are") + " still open.");
            d.nextSteps.add("Wait until every loan is repaid or settled.");
        }
        if (f.pendingPayoutRequests > 0) {
            d.reasons.add(f.pendingPayoutRequests + (f.pendingPayoutRequests == 1 ? " payout request is" : " payout requests are") + " waiting for an answer.");
            d.nextSteps.add("Approve or decline the pending payout requests.");
        }

        if (!f.onChain) {
            // App-only group: nothing on the network to protect.
            if (!d.reasons.isEmpty()) return d;
            if (f.otherActiveMembers == 0) {
                Decision ok = new Decision(Action.DELETE, "Delete this group");
                ok.reasons.add("Nobody else has joined and nothing is open, so it can be removed completely.");
                return ok;
            }
            Decision ok = new Decision(Action.ARCHIVE, "Close this group");
            ok.reasons.add(f.otherActiveMembers + (f.otherActiveMembers == 1 ? " other member keeps" : " other members keep") + " read access to the history.");
            return ok;
        }

        // On the network.
        String state = f.nodeState == null ? "" : f.nodeState.toUpperCase();
        if (state.isEmpty()) {
            d.reasons.add("We couldn't check the group's state on the network.");
            d.nextSteps.add("Check your connection and try again.");
            return d;
        }
        // A goal group the admin made and nobody else joined: the network can close it and refund the admin.
        if ("FIXED".equals(f.family) && f.otherActiveMembers == 0 && d.reasons.isEmpty()) {
            if (state.equals("CLOSED")) {
                Decision ok = new Decision(Action.DELETE, "Delete this group");
                ok.reasons.add("The group is already closed on the network and its money was sent back, so it can be removed from the app.");
                return ok;
            }
            if (state.equals("CLOSING")) {
                d.reasons.add("Your refund is on its way and the network is closing the group.");
                d.nextSteps.add("Try again in a minute, once the refund has confirmed.");
                return d;
            }
            if (state.equals("ACTIVE") || state.equals("COMPLETED")) {
                if (f.escrowBalanceRaw < 0) {
                    d.reasons.add("We couldn't read the group's balance on the network.");
                    d.nextSteps.add("Check your connection and try again.");
                    return d;
                }
                Decision ok = new Decision(Action.REFUND_AND_DELETE, "Close and delete this group");
                ok.reasons.add(f.escrowBalanceRaw > 0
                    ? "Nobody else has joined. The " + com.harvana.app.engine.Amounts.money(f.escrowBalanceRaw, f.asset) + " in the group wallet is yours and goes back to your wallet."
                    : "Nobody else has joined and the group wallet is empty.");
                ok.reasons.add("The network closes the group (no more deposits), then it is removed from the app. A small tFare applies.");
                return ok;
            }
        }
        boolean ended = state.equals("ABORTED") || state.equals("COMPLETED");
        if ("CIRCLE".equals(f.family)) {
            if (state.equals("FORMING")) {
                d.reasons.add("The circle is still filling up and members may already have locked their bonds.");
                d.nextSteps.add("If it doesn't fill before its deadline, the network closes it by itself and refunds every bond automatically.");
            } else if (state.equals("ACTIVE")) {
                d.reasons.add("Rounds are running and the network is holding the members' money.");
                d.nextSteps.add("Let the circle finish its rounds. It becomes closable once it is completed and every bond is withdrawn.");
            } else if (!ended) {
                d.reasons.add("The circle is in an unexpected state (" + state + ").");
            }
        } else {
            if (!ended && !"ACTIVE".equals(state)) {
                d.reasons.add("The group is in an unexpected state (" + state + ").");
            }
            if ("ACTIVE".equals(state) && f.otherActiveMembers > 0) {
                d.reasons.add("Members are still saving in this group and the network has no way to end an active group.");
                d.nextSteps.add("Ask members to finish or withdraw first.");
            }
        }
        if (ended || "ACTIVE".equals(state)) {
            if (f.escrowBalanceRaw < 0) {
                d.reasons.add("We couldn't read the group's balance on the network.");
                d.nextSteps.add("Check your connection and try again.");
            } else if (f.escrowBalanceRaw > 0) {
                d.reasons.add("The group's network escrow still holds coins that belong to members.");
                d.nextSteps.add("Every member must withdraw their share (bond or savings) first.");
            }
        }
        if (!d.reasons.isEmpty()) return d;

        Decision ok = new Decision(Action.ARCHIVE, "Close this group");
        ok.reasons.add("Nothing is owed and nothing is open. The group's record stays on the network for good, so it is closed in the app rather than erased.");
        return ok;
    }

    static String harv(long raw) {
        return java.math.BigDecimal.valueOf(raw, 8).stripTrailingZeros().toPlainString() + " HARV";
    }
}
