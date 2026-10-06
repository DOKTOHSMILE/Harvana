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
        assertEquals("A simple example", s.get(1).title);
        assertEquals("Words explained", s.get(s.size() - 1).title);
        boolean hasPot = false;
        for (String p : s.get(1).points) if (p.contains("450 HARV")) hasPot = true;
        assertTrue(hasPot); // 9 x 50 (the member being paid does not pay)
    }

    @Test public void goalGroupContractStatesTheGoalWhoSetItAndWhereTheMoneyGoes() {
        Map<String, Object> g = group("Fixed Contribution");
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
}
