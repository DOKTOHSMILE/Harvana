package com.harvana.app.social;

import java.util.Locale;

/**
 * Off-app contact options a member can put on their profile (phone number and chat / social links), and how each one
 * is turned into a safe address to open. Pure logic. Only https and tel addresses are ever produced.
 */
public final class ContactLinks {
    private ContactLinks() {}

    public static final String[] TYPES = {"Phone", "Email", "WhatsApp", "Telegram", "Instagram", "X", "Facebook", "TikTok", "Snapchat", "Signal", "Website"};

    /** Null when the entry is acceptable. */
    public static String problem(String type, String value) {
        String v = value == null ? "" : value.trim();
        if (v.isEmpty()) return "Enter the " + (type == null ? "contact" : type.toLowerCase(Locale.US)) + " detail.";
        if (v.length() > 120) return "That's too long.";
        if ("Email".equals(type)) {
            return v.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$") ? null : "Enter a valid email address.";
        }
        if ("Phone".equals(type) || "WhatsApp".equals(type) || "Signal".equals(type)) {
            String digits = v.replaceAll("[^0-9]", "");
            if (digits.length() < 7 || digits.length() > 15) return "Enter a full phone number with country code.";
            if (!v.matches("[+0-9 ()\\-]+")) return "A phone number can only contain digits, spaces, + ( ) and -.";
            return null;
        }
        if ("Website".equals(type)) {
            String lower = v.toLowerCase(Locale.US);
            if (lower.startsWith("http://")) return "Use a secure https:// address.";
            if (lower.contains(" ") ) return "A web address can't contain spaces.";
            if (lower.startsWith("javascript:") || lower.startsWith("data:") || lower.startsWith("file:")) return "That address isn't allowed.";
            return null;
        }
        // handle style
        String h = handle(v);
        if (h.isEmpty() || !h.matches("[A-Za-z0-9_.\\-/]+")) return "Enter just the username (letters, numbers, dots, underscores).";
        return null;
    }

    private static String handle(String v) {
        String s = v.trim();
        while (s.startsWith("@")) s = s.substring(1);
        return s;
    }

    /** The address to open for this contact, or null if it is not valid. */
    public static String uri(String type, String value) {
        if (problem(type, value) != null) return null;
        String v = value.trim();
        String digits = v.replaceAll("[^0-9]", "");
        switch (type) {
            case "Phone": return "tel:+" + digits;
            case "Email": return "mailto:" + v;
            case "WhatsApp": return "https://wa.me/" + digits;
            case "Signal": return "https://signal.me/#p/+" + digits;
            case "Telegram": return "https://t.me/" + handle(v);
            case "Instagram": return "https://instagram.com/" + handle(v);
            case "X": return "https://x.com/" + handle(v);
            case "Facebook": return "https://facebook.com/" + handle(v);
            case "TikTok": return "https://tiktok.com/@" + handle(v);
            case "Snapchat": return "https://snapchat.com/add/" + handle(v);
            case "Website": {
                String lower = v.toLowerCase(Locale.US);
                return lower.startsWith("https://") ? v : "https://" + v;
            }
            default: return null;
        }
    }
}
