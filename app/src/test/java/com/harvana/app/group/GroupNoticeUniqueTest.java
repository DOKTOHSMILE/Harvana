package com.harvana.app.group;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class GroupNoticeUniqueTest {
    private Map<String, Object> n(String id, String title, String message, String actor, long at) {
        Map<String, Object> m = new HashMap<>();
        m.put("_docId", id); m.put("title", title); m.put("message", message); m.put("actorUid", actor); m.put("createdAt", at);
        return m;
    }

    @Test public void sameWordsWithinTwoMinutesFromTheSamePersonAreOneNotice() {
        List<Map<String, Object>> in = new ArrayList<>();
        in.add(n("a", "Contribution received", "A member paid.", "u1", 1_000_000L));
        in.add(n("b", "Contribution received", "A member paid.", "u1", 1_030_000L));
        assertEquals(1, GroupNoticeHelper.unique(in).size());
    }

    @Test public void sameWordsOnAnotherDayOrFromSomeoneElseStay() {
        List<Map<String, Object>> in = new ArrayList<>();
        in.add(n("a", "New member joined", "A new member has joined the group.", "u1", 1_000_000L));
        in.add(n("b", "New member joined", "A new member has joined the group.", "u1", 90_000_000L));
        in.add(n("c", "New member joined", "A new member has joined the group.", "u2", 90_010_000L));
        assertEquals(3, GroupNoticeHelper.unique(in).size());
    }

    @Test public void twoProposalsWithTheSameTitleAreNotMerged() {
        Map<String, Object> p1 = n("a", "Payout request", "A vote is waiting.", "u1", 1_000_000L);
        p1.put("proposalKind", "DISBURSEMENT"); p1.put("proposalId", "D1");
        Map<String, Object> p2 = n("b", "Payout request", "A vote is waiting.", "u1", 1_010_000L);
        p2.put("proposalKind", "DISBURSEMENT"); p2.put("proposalId", "D2");
        Map<String, Object> p1Again = n("c", "Payout request", "A vote is waiting.", "u1", 1_020_000L);
        p1Again.put("proposalKind", "DISBURSEMENT"); p1Again.put("proposalId", "D1");
        List<Map<String, Object>> in = new ArrayList<>();
        in.add(p1); in.add(p2); in.add(p1Again);
        assertEquals(2, GroupNoticeHelper.unique(in).size());
    }
}
