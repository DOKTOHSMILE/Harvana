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

    @Test public void systemNoticeOnlyWhenTargetedAtMe() {
        assertEquals("PAYMENT", NoticeFilter.categoryFor(n("SYSTEM", "me", true, 10), "me", 5));
        assertNull(NoticeFilter.categoryFor(n("SYSTEM", "other", true, 10), "me", 5));
        assertNull(NoticeFilter.categoryFor(n("SYSTEM", "", true, 10), "me", 5));
    }
}
