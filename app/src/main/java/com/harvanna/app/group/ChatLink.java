package com.harvanna.app.group;

import java.net.URI;
import java.util.Locale;

/** Validation for the external group-chat link. Only https links to well-known chat apps are accepted. */
public final class ChatLink {
    private ChatLink() {}

    /** Returns the normalized https URL, or null if the link is not an accepted chat link. */
    public static String normalize(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isEmpty() || s.length() > 300 || s.contains(" ")) return null;
        if (!s.toLowerCase(Locale.US).startsWith("https://")) {
            if (s.contains("://")) return null;
            s = "https://" + s;
        }
        try {
            URI u = new URI(s);
            String host = u.getHost() == null ? "" : u.getHost().toLowerCase(Locale.US);
            String path = u.getPath() == null ? "" : u.getPath();
            if (u.getUserInfo() != null || host.isEmpty()) return null;
            if (platform(host, path) == null) return null;
            return s;
        } catch (Exception e) {
            return null;
        }
    }

    public static String platformOf(String url) {
        try {
            URI u = new URI(url);
            return platform(u.getHost().toLowerCase(Locale.US), u.getPath() == null ? "" : u.getPath());
        } catch (Exception e) { return null; }
    }

    private static String platform(String host, String path) {
        if (host.equals("chat.whatsapp.com")) return path.length() > 1 ? "WhatsApp" : null;
        if (host.equals("whatsapp.com") || host.equals("www.whatsapp.com")) return path.startsWith("/channel/") ? "WhatsApp" : null;
        if (host.equals("t.me") || host.equals("telegram.me")) return path.length() > 1 ? "Telegram" : null;
        if (host.equals("signal.group")) return "Signal";
        if (host.equals("discord.gg")) return path.length() > 1 ? "Discord" : null;
        if (host.equals("discord.com") || host.equals("www.discord.com")) return path.startsWith("/invite/") ? "Discord" : null;
        if (host.equals("facebook.com") || host.equals("www.facebook.com") || host.equals("m.facebook.com")) {
            return path.startsWith("/groups/") || path.startsWith("/messages/") ? "Facebook" : null;
        }
        if (host.equals("m.me")) return path.length() > 1 ? "Messenger" : null;
        return null;
    }
}
