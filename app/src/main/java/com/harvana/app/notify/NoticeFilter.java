package com.harvana.app.notify;

import java.util.Map;

/** Decides whether a group notice should notify this user, and under which heading. Pure Java. */
public final class NoticeFilter {
    private NoticeFilter() {}

    /** Result: category "GROUP" or "PAYMENT", or null when the notice shouldn't notify this person. */
    public static String categoryFor(Map<String, Object> notice, String myUid, long sinceMs) {
        long created = num(notice.get("createdAt"));
        if (created <= sinceMs) return null;
        String audience = str(notice.get("audience"));
        String actor = str(notice.get("actorUid"));
        String creator = str(notice.get("creatorId"));
        boolean system = Boolean.TRUE.equals(notice.get("system")) || "SYSTEM".equals(str(notice.get("type")));

        // Notices written with an audience say exactly who they are for.
        if ("TARGET".equals(audience)) {
            if (!str(notice.get("targetUid")).equals(myUid) || actor.equals(myUid)) return null;
            return "PAYMENT".equals(str(notice.get("category"))) ? "PAYMENT" : "GROUP";
        }
        if ("ALL".equals(audience)) {
            return !actor.isEmpty() && actor.equals(myUid) ? null : "GROUP";       // everyone but the person who caused it
        }

        // Older notices carry no audience. A payment reminder is for the one member it names; every other system notice (a member
        // joined, a loan applied for, a vote waiting) is for the whole group except the person who did it.
        if (system) {
            if (str(notice.get("title")).toLowerCase(java.util.Locale.US).startsWith("payment reminder")) {
                return !creator.isEmpty() && creator.equals(myUid) ? "PAYMENT" : null;
            }
            return !creator.isEmpty() && creator.equals(myUid) ? null : "GROUP";
        }
        if (!creator.isEmpty() && creator.equals(myUid)) return null;               // your own announcement
        return "GROUP";
    }

    /** True when a member can act on the notice (vote). */
    public static boolean needsVote(Map<String, Object> notice) {
        return Boolean.TRUE.equals(notice.get("needsVote")) || !str(notice.get("proposalKind")).isEmpty();
    }

    /** The page a tap should open: 99 votes, otherwise by what the notice says, or the Notice tab. */
    public static int pageFor(Map<String, Object> notice, String category) {
        if (notice.get("page") instanceof Number) return ((Number) notice.get("page")).intValue();
        if (needsVote(notice)) return 99;
        String about = (str(notice.get("title")) + " " + str(notice.get("message"))).toLowerCase(java.util.Locale.US);
        if (about.contains("joined") || about.contains("new member") || about.contains("left the group") || about.contains("removed")) return 1;
        if (about.contains("loan")) return 3;
        return "PAYMENT".equals(category) ? 2 : 4;
    }

    private static long num(Object o) {
        if (o instanceof Number) return ((Number) o).longValue();
        try { return o == null ? 0 : Long.parseLong(String.valueOf(o)); } catch (Exception e) { return 0; }
    }
    private static String str(Object o) { return o == null ? "" : String.valueOf(o); }
}
