package com.harvanna.app.social;

import java.util.Map;

/** A member as shown in friends lists and search results: just what is public, never contact details. */
public final class Person {
    public final String uid, username, name;
    public Person(String uid, String username, String name) {
        this.uid = uid == null ? "" : uid;
        this.username = username == null ? "" : username;
        this.name = name == null || name.trim().isEmpty() ? (username == null || username.isEmpty() ? "Member" : "@" + username) : name.trim();
    }
    public String handle() { return username.isEmpty() ? "" : "@" + username; }

    public static Person fromDoc(String uid, Map<String, Object> m) {
        String user = str(m.get("username"));
        String name = str(m.get("name"));
        if (name.isEmpty()) name = (str(m.get("first_name")) + " " + str(m.get("last_name"))).trim();
        return new Person(uid, user, name);
    }
    private static String str(Object o) { return o == null ? "" : String.valueOf(o).trim(); }
}
