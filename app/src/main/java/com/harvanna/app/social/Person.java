package com.harvanna.app.social;

import java.util.Map;

/** A member as shown in friends lists and search results: just what is public, never contact details. */
public final class Person {
    public final String uid, username, name, photo;
    public Person(String uid, String username, String name) { this(uid, username, name, ""); }
    public Person(String uid, String username, String name, String photo) {
        this.uid = uid == null ? "" : uid;
        this.username = username == null ? "" : username;
        this.name = name == null || name.trim().isEmpty() ? (username == null || username.isEmpty() ? "Member" : "@" + username) : name.trim();
        this.photo = photo == null ? "" : photo.trim();
    }
    public String handle() { return username.isEmpty() ? "" : "@" + username; }
    public String initials() {
        String source = name.startsWith("@") ? name.substring(1) : name;
        String[] parts = source.trim().split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) return "?";
        if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase(java.util.Locale.US);
        return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase(java.util.Locale.US);
    }

    public static Person fromDoc(String uid, Map<String, Object> m) {
        String user = str(m.get("username"));
        String name = str(m.get("name"));
        if (name.isEmpty()) name = (str(m.get("first_name")) + " " + str(m.get("last_name"))).trim();
        String photo = str(m.get("photoUrl"));
        if (photo.isEmpty()) photo = str(m.get("avatarUrl"));
        if (photo.isEmpty()) photo = str(m.get("profileImage"));
        return new Person(uid, user, name, photo);
    }
    private static String str(Object o) { return o == null ? "" : String.valueOf(o).trim(); }
}
