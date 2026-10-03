package com.harvana.app.group;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class PresenceStateTest {
    private Map<String, Object> p(long lastActive) {
        Map<String, Object> m = new HashMap<>(); m.put("lastActiveAt", lastActive); return m;
    }

    @Test public void thresholds() {
        long now = 10_000_000L;
        assertEquals(PresenceState.ONLINE, PresenceState.of(p(now - 60_000), now));
        assertEquals(PresenceState.AWAY, PresenceState.of(p(now - 10 * 60_000), now));
        assertEquals(PresenceState.OFFLINE, PresenceState.of(p(now - 3 * 3_600_000L), now));
    }

    @Test public void unknownWhenMissingOrHidden() {
        assertEquals(PresenceState.UNKNOWN, PresenceState.of(new HashMap<>(), 1L));
        Map<String, Object> m = p(1L); m.put("showActivityStatus", false);
        assertEquals(PresenceState.UNKNOWN, PresenceState.of(m, 2L));
        assertEquals("", PresenceState.label(m, 2L));
    }

    @Test public void labels() {
        long now = 100_000_000L;
        assertEquals("Online now", PresenceState.label(p(now - 1000), now));
        assertEquals("Active 30 min ago", PresenceState.label(p(now - 30 * 60_000L), now));
        assertEquals("Last seen 3 h ago", PresenceState.label(p(now - 3 * 3_600_000L), now));
        assertEquals("Last seen 2 d ago", PresenceState.label(p(now - 2 * 86_400_000L), now));
    }
}
