package com.harvanna.app.group;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class GroupClosePolicyTest {
    private static GroupClosePolicy.Facts base() {
        GroupClosePolicy.Facts f = new GroupClosePolicy.Facts();
        f.admin = true;
        return f;
    }
    private static GroupClosePolicy.Action act(GroupClosePolicy.Facts f) { return GroupClosePolicy.decide(f).action; }

    @Test public void onlyAdminCanClose() {
        GroupClosePolicy.Facts f = base(); f.admin = false;
        assertEquals(GroupClosePolicy.Action.NONE, act(f));
    }

    @Test public void alreadyClosedOffersNothing() {
        GroupClosePolicy.Facts f = base(); f.alreadyClosed = true;
        assertEquals(GroupClosePolicy.Action.NONE, act(f));
    }

    @Test public void appOnlyEmptyGroupIsDeleted() {
        assertEquals(GroupClosePolicy.Action.DELETE, act(base()));
    }

    @Test public void appOnlyGroupWithMembersIsArchived() {
        GroupClosePolicy.Facts f = base(); f.otherActiveMembers = 3;
        assertEquals(GroupClosePolicy.Action.ARCHIVE, act(f));
    }

    @Test public void openLoansOrPayoutsBlockEverything() {
        GroupClosePolicy.Facts f = base(); f.openLoans = 2;
        assertEquals(GroupClosePolicy.Action.NONE, act(f));
        f = base(); f.pendingPayoutRequests = 1; f.onChain = true; f.family = "CIRCLE"; f.nodeState = "ABORTED"; f.escrowBalanceRaw = 0;
        GroupClosePolicy.Decision d = GroupClosePolicy.decide(f);
        assertEquals(GroupClosePolicy.Action.NONE, d.action);
        assertFalse(d.nextSteps.isEmpty());
    }

    @Test public void onChainGroupIsNeverHardDeleted() {
        GroupClosePolicy.Facts f = base(); f.onChain = true; f.family = "CIRCLE"; f.nodeState = "ABORTED"; f.escrowBalanceRaw = 0;
        assertEquals(GroupClosePolicy.Action.ARCHIVE, act(f));
    }

    @Test public void formingAndActiveCirclesCannotBeClosed() {
        GroupClosePolicy.Facts f = base(); f.onChain = true; f.family = "CIRCLE"; f.escrowBalanceRaw = 0;
        f.nodeState = "FORMING";
        GroupClosePolicy.Decision d = GroupClosePolicy.decide(f);
        assertEquals(GroupClosePolicy.Action.NONE, d.action);
        assertTrue(d.nextSteps.get(0).contains("automatically"));
        f.nodeState = "ACTIVE";
        assertEquals(GroupClosePolicy.Action.NONE, act(f));
    }

    @Test public void completedCircleWithBondsLeftIsBlocked() {
        GroupClosePolicy.Facts f = base(); f.onChain = true; f.family = "CIRCLE"; f.nodeState = "COMPLETED"; f.escrowBalanceRaw = 500;
        assertEquals(GroupClosePolicy.Action.NONE, act(f));
        f.escrowBalanceRaw = 0;
        assertEquals(GroupClosePolicy.Action.ARCHIVE, act(f));
    }

    @Test public void unknownNetworkFactsBlock() {
        GroupClosePolicy.Facts f = base(); f.onChain = true; f.family = "ACCUM";
        assertEquals(GroupClosePolicy.Action.NONE, act(f));           // state unknown
        f.nodeState = "ACTIVE";
        assertEquals(GroupClosePolicy.Action.NONE, act(f));           // balance unknown
        f.escrowBalanceRaw = 0;
        assertEquals(GroupClosePolicy.Action.ARCHIVE, act(f));        // empty, nobody else in it
    }

    @Test public void activeSavingsGroupWithMembersIsBlocked() {
        GroupClosePolicy.Facts f = base(); f.onChain = true; f.family = "FIXED"; f.nodeState = "ACTIVE"; f.escrowBalanceRaw = 0; f.otherActiveMembers = 2;
        assertEquals(GroupClosePolicy.Action.NONE, act(f));
        f.nodeState = "COMPLETED";
        assertEquals(GroupClosePolicy.Action.ARCHIVE, act(f));
    }
}
