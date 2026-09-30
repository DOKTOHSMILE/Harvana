package com.harvanna.app.notify;

import java.util.Map;

/** Decides whether a group notice should notify this user. Pure Java. */
public final class NoticeFilter {
    private NoticeFilter() {}

    /** Result: category "GROUP" or "PAYMENT", or null when the notice shouldn't notify. */
    public static String categoryFor(Map<String, Object> notice, String myUid, long sinceMs) {
        long created = num(notice.get("createdAt"));
        if (created <= sinceMs) return null;
        String type = str(notice.get("type"));
        String creator = str(notice.get("creatorId"));
        boolean system = Boolean.TRUE.equals(notice.get("system")) || "SYSTEM".equals(type);
        if (system) {
            // System notices are only for the person they target (e.g. a payment reminder).
            if (!creator.isEmpty() && creator.equals(myUid)) return "PAYMENT";
            return null;
        }
        if (!creator.isEmpty() && creator.equals(myUid)) return null; // your own announcement
        return "GROUP";
    }

    private static long num(Object o) {
        if (o instanceof Number) return ((Number) o).longValue();
        try { return o == null ? 0 : Long.parseLong(String.valueOf(o)); } catch (Exception e) { return 0; }
    }
    private static String str(Object o) { return o == null ? "" : String.valueOf(o); }
}
