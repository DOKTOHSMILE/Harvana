package com.harvana.app.group;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class GroupContractTest {
    private Map<String, Object> group(String type) {
        Map<String, Object> g = new HashMap<>();
        g.put("groupName", "Test"); g.put("groupType", type);
        g.put("contributionAmount", 50.0); g.put("frequency", "Monthly"); g.put("totalSlots", 10);
        return g;
    }

    @Test public void rotatingNeverOffersLoans() {
        Map<String, Object> g = group("Rotating Pool"); g.put("allowLoans", true);
        assertFalse(GroupContract.allowsLoans(g));
    }

    @Test public void accumulativeLoansFollowFlagAndDefaultOn() {
        Map<String, Object> g = group("Accumulative Funds");
        assertTrue(GroupContract.allowsLoans(g));
        g.put("allowLoans", false);
        assertFalse(GroupContract.allowsLoans(g));
    }

    @Test public void hashIsStableAndChangesWithTerms() {
        Map<String, Object> g = group("Fixed Contribution");
        String h1 = GroupContract.hash("G1", g, "HARV");
        assertEquals(h1, GroupContract.hash("G1", g, "HARV"));
        assertEquals(64, h1.length());
        g.put("contributionAmount", 60.0);
        assertNotEquals(h1, GroupContract.hash("G1", g, "HARV"));
        assertNotEquals(h1, GroupContract.hash("G2", g, "HARV"));
    }

    @Test public void votingSectionOnlyForNodeVotingTypes() {
        boolean fixedHasVote = false, rotHasWeights = false;
        for (GroupContract.Section s : GroupContract.sections(group("Fixed Contribution"), "HARV"))
            for (String p : s.points) if (p.contains("40%")) fixedHasVote = true;
        for (GroupContract.Section s : GroupContract.sections(group("Rotating Pool"), "HARV"))
            for (String p : s.points) if (p.contains("40%")) rotHasWeights = true;
        assertTrue(fixedHasVote);
        assertFalse(rotHasWeights);
    }

    @Test public void signedMessageFormat() {
        assertEquals("HARVANA_GROUP_JOIN_ACK_V1:G:HV0xA:abc:5",
            GroupContract.signedMessage("G", "HV0xA", "abc", 5L));
    }

    @Test public void startsWithSummaryAndHasExampleAndWords() {
        Map<String, Object> g = group("Rotating Pool");
        java.util.List<GroupContract.Section> s = GroupContract.sections(g, "HARV");
        assertEquals("In short", s.get(0).title);
        assertEquals("The circle and where the money goes", s.get(1).title);
        assertEquals("A simple example", s.get(2).title);
        assertEquals("Words explained", s.get(s.size() - 1).title);
        boolean hasPot = false;
        for (String p : s.get(2).points) if (p.contains("450 HARV")) hasPot = true;
        assertTrue(hasPot); // 9 x 50 (the member being paid does not pay)
    }

    @Test public void goalGroupContractStatesTheGoalWhoSetItAndWhereTheMoneyGoes() {
        Map<String, Object> g = group("Fixed Contribution");
        g.put("nodeGroupId", "FIXED-1");
        g.put("disbursementRule", "Admin-Triggered");
        g.put("beneficiaryAddress", "HV0x1111111111111111111111111111111111111111");
        StringBuilder all = new StringBuilder();
        boolean found = false;
        for (GroupContract.Section s : GroupContract.sections(g, "HARV")) {
            if (s.title.equals("The goal and where the money goes")) found = true;
            for (String p : s.points) all.append(p).append('\n');
        }
        assertTrue(found);
        String t = all.toString();
        assertTrue(t.contains("Who set the goal"));
        assertTrue(t.contains("group wallet"));
        assertTrue(t.contains("HV0x1111"));
        assertTrue(t.contains("Nothing is held in a Harvana company wallet"));
    }

    @Test public void everyGroupTypeStatesWhoStartedItAndWhereTheMoneyGoes() {
        String[][] kinds = {{"Rotating Pool", "The circle and where the money goes"}, {"Accumulative Funds", "The savings group and where the money goes"},
            {"Fixed Contribution", "The goal and where the money goes"}};
        String[] ids = {"CIRC-1", "ACCUM-1", "FIXED-1"};
        int n = 0;
        for (String[] k : kinds) {
            boolean found = false;
            StringBuilder all = new StringBuilder();
            Map<String, Object> grp = group(k[0]);
            grp.put("nodeGroupId", ids[n++]);
            for (GroupContract.Section s : GroupContract.sections(grp, "HARV")) {
                if (s.title.equals(k[1])) { found = true; for (String p : s.points) all.append(p); }
            }
            assertTrue(k[0], found);
            assertTrue(k[0], all.toString().contains("Who started it") || all.toString().contains("Who set the goal"));
            assertTrue(k[0], all.toString().contains("company wallet"));
        }
    }

    @Test public void aGroupKeptOnlyInTheAppDoesNotClaimANetworkWallet() {
        for (String type : new String[]{"Rotating Pool", "Accumulative Funds", "Fixed Contribution"}) {
            StringBuilder all = new StringBuilder();
            for (GroupContract.Section s : GroupContract.sections(group(type), "HARV")) for (String p : s.points) all.append(p);
            assertTrue(type, all.toString().contains("kept in the app only"));
            assertFalse(type, all.toString().contains("no private key"));
        }
    }

    @Test public void loanInterestIsPerDayAndOnlyForTheDaysHeld() {
        assertEquals(0.5, LoanInterest.dailyPercent(0.5, "DAILY", false), 1e-9);
        assertEquals("on the network the stored rate is already per day", 0.5, LoanInterest.dailyPercent(0.5, "MONTHLY", true), 1e-9);
        assertEquals("an old monthly rate is spread over 30 days", 0.1, LoanInterest.dailyPercent(3.0, "MONTHLY", false), 1e-9);
        assertEquals(5.0, LoanInterest.interest(100, 0.5, 10), 1e-9);
        assertEquals("repaid on day 15 of 30: 15 days", 7.5, LoanInterest.interest(100, 0.5, 15), 1e-9);
        assertEquals(15.0, LoanInterest.interest(100, 0.5, 30), 1e-9);
        assertEquals(2.5, LoanInterest.interest(100, 0.5, 5.0), 1e-9);
        assertEquals("half a day costs half a day", 0.25, LoanInterest.interest(100, 0.5, 0.5), 1e-9);
        assertEquals(1, LoanInterest.clampDays(0));
        assertEquals(30, LoanInterest.clampDays(90));
    }

    @Test public void theLoanSectionOfTheContractSaysPerDayAndShowsTheEarlyRepaymentExample() {
        Map<String, Object> g = group("Accumulative Funds");
        g.put("allowLoans", true); g.put("loanInterestRate", 0.5); g.put("loanInterestPeriod", "DAILY"); g.put("nodeGroupId", "ACCUM-1");
        StringBuilder all = new StringBuilder();
        for (GroupContract.Section s : GroupContract.sections(g, "HARV")) if (s.title.equals("Loans")) for (String p : s.points) all.append(p).append('\n');
        String t = all.toString();
        assertTrue(t, t.contains("per day"));
        assertTrue(t, t.contains("not a monthly charge"));
        assertTrue(t, t.contains("repay on day 15"));
        assertTrue(t, t.contains("7.5 HARV"));
        assertFalse(t, t.contains("per month"));
    }
}
