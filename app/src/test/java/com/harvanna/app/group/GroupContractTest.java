package com.harvanna.app.group;

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
        assertEquals("HARVANNA_GROUP_JOIN_ACK_V1:G:HV0xA:abc:5",
            GroupContract.signedMessage("G", "HV0xA", "abc", 5L));
    }
}
