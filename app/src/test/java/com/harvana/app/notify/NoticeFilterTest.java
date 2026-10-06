package com.harvana.app.notify;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class NoticeFilterTest {
    private Map<String, Object> n(String type, String creator, boolean system, long at) {
        Map<String, Object> m = new HashMap<>();
        m.put("type", type); m.put("creatorId", creator); m.put("system", system); m.put("createdAt", at);
        return m;
    }

    @Test public void announcementFromOthersNotifies() {
        assertEquals("GROUP", NoticeFilter.categoryFor(n("ANNOUNCEMENT", "admin", false, 10), "me", 5));
    }

    @Test public void ownAnnouncementAndOldOnesDoNot() {
        assertNull(NoticeFilter.categoryFor(n("ANNOUNCEMENT", "me", false, 10), "me", 5));
        assertNull(NoticeFilter.categoryFor(n("ANNOUNCEMENT", "admin", false, 5), "me", 5));
    }

    @Test public void olderSystemNoticeGoesToEveryoneButWhoDidIt() {
        assertNull(NoticeFilter.categoryFor(n("SYSTEM", "me", true, 10), "me", 5));
        assertEquals("GROUP", NoticeFilter.categoryFor(n("SYSTEM", "other", true, 10), "me", 5));
        assertEquals("GROUP", NoticeFilter.categoryFor(n("SYSTEM", "", true, 10), "me", 5));
    }

    @Test public void olderPaymentReminderIsOnlyForTheNamedMember() {
        Map<String, Object> mine = n("SYSTEM", "me", true, 10); mine.put("title", "Payment reminder");
        Map<String, Object> theirs = n("SYSTEM", "other", true, 10); theirs.put("title", "Payment reminder");
        assertEquals("PAYMENT", NoticeFilter.categoryFor(mine, "me", 5));
        assertNull(NoticeFilter.categoryFor(theirs, "me", 5));
    }

    @Test public void noticeForEveryoneSkipsTheActor() {
        Map<String, Object> m = n("SYSTEM", "actor", true, 10); m.put("audience", "ALL"); m.put("actorUid", "actor");
        assertNull(NoticeFilter.categoryFor(m, "actor", 5));
        assertEquals("GROUP", NoticeFilter.categoryFor(m, "someone", 5));
    }

    @Test public void targetedNoticeIsOnlyForTheTarget() {
        Map<String, Object> m = n("SYSTEM", "t", true, 10); m.put("audience", "TARGET"); m.put("targetUid", "t"); m.put("actorUid", "a"); m.put("category", "PAYMENT");
        assertEquals("PAYMENT", NoticeFilter.categoryFor(m, "t", 5));
        assertNull(NoticeFilter.categoryFor(m, "other", 5));
        assertNull(NoticeFilter.categoryFor(m, "a", 5));
    }

    @Test public void voteNoticeOpensTheVotesPage() {
        Map<String, Object> m = n("SYSTEM", "", true, 10); m.put("needsVote", true); m.put("page", 99);
        assertTrue(NoticeFilter.needsVote(m));
        assertEquals(99, NoticeFilter.pageFor(m, "GROUP"));
        Map<String, Object> old = n("SYSTEM", "", true, 10); old.put("proposalKind", "GRANT");
        assertEquals(99, NoticeFilter.pageFor(old, "GROUP"));
    }
}
