package com.harvana.app.group;

import java.util.Map;

/** Online / away / offline from a profile's self-reported lastActiveAt. Pure Java. */
public enum PresenceState {
    ONLINE, AWAY, OFFLINE, UNKNOWN;

    public static final long ONLINE_MS = 5L * 60 * 1000;
    public static final long AWAY_MS = 60L * 60 * 1000;

    public static PresenceState of(Map<String, Object> profile, long now) {
        if (profile == null || Boolean.FALSE.equals(profile.get("showActivityStatus"))) return UNKNOWN;
        Object raw = profile.get("lastActiveAt");
        if (!(raw instanceof Number)) return UNKNOWN;
        long age = now - ((Number) raw).longValue();
        if (age < 0) age = 0;
        if (age < ONLINE_MS) return ONLINE;
        if (age < AWAY_MS) return AWAY;
        return OFFLINE;
    }

    public static String label(Map<String, Object> profile, long now) {
        PresenceState s = of(profile, now);
        if (s == UNKNOWN) return "";
        if (s == ONLINE) return "Online now";
        long mins = (now - ((Number) profile.get("lastActiveAt")).longValue()) / 60000L;
        if (mins < 60) return "Active " + mins + " min ago";
        long hours = mins / 60;
        if (hours < 24) return "Last seen " + hours + " h ago";
        return "Last seen " + (hours / 24) + " d ago";
    }
}
