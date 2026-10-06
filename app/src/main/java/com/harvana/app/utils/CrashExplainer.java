package com.harvana.app.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns a crash stack trace into plain English: what happened, on which screen, and what to try. The raw trace is untouched and still
 * shown under "Crash message:". No Android classes are used, so this runs in plain unit tests.
 */
public final class CrashExplainer {
    private CrashExplainer() {}

    public static final class Result {
        /** One or two sentences a non-programmer can read. */
        public final String meaning;
        /** What the person can do about it. */
        public final String advice;
        /** Where in the app it happened, e.g. "Edit profile screen (EditProfileActivity, line 95)"; empty if unknown. */
        public final String where;
        Result(String meaning, String advice, String where) { this.meaning = meaning; this.advice = advice; this.where = where; }
    }

    private static final Pattern NPE_INVOKE = Pattern.compile("Attempt to (invoke|read from|write to) (virtual |interface )?(method|field) '([^']+)' on a null object reference");
    private static final Pattern APP_FRAME = Pattern.compile("at (com\\.harvana\\.app[\\w.$]*)\\.([\\w$<>]+)\\((\\w+\\.java):(\\d+)\\)");

    public static Result explain(String trace) {
        if (trace == null || trace.trim().isEmpty()) return new Result("No crash was recorded.", "", "");
        String type = "", message = "";
        // The real cause is the last "Caused by:", otherwise the first line.
        String[] lines = trace.split("\n");
        String head = null;
        for (String l : lines) {
            String t = l.trim();
            if (t.startsWith("Caused by:")) head = t.substring("Caused by:".length()).trim();
        }
        if (head == null) {
            for (String l : lines) {
                String t = l.trim();
                if (!t.isEmpty() && !t.startsWith("FATAL CRASH")) { head = t; break; }
            }
        }
        if (head == null) head = "";
        int colon = head.indexOf(':');
        type = (colon > 0 ? head.substring(0, colon) : head).trim();
        message = colon > 0 ? head.substring(colon + 1).trim() : "";
        String simple = type.contains(".") ? type.substring(type.lastIndexOf('.') + 1) : type;

        String where = where(trace);
        String meaning = meaning(type, simple, message, trace);
        String advice = advice(type, simple, message, trace);
        return new Result(meaning, advice, where);
    }

    // ── Where ────────────────────────────────────────────────────────────

    private static String where(String trace) {
        Matcher m = APP_FRAME.matcher(trace);
        if (!m.find()) return "";
        String cls = m.group(1);
        String simple = cls.substring(cls.lastIndexOf('.') + 1);
        int dollar = simple.indexOf('$');
        if (dollar > 0) simple = simple.substring(0, dollar);
        return screenName(simple) + " (" + simple + ", " + m.group(3) + " line " + m.group(4) + ")";
    }

    /** "EditProfileActivity" becomes "Edit profile screen". */
    static String screenName(String cls) {
        String base = cls.replaceAll("(Fragment)?Activity$", "").replaceAll("Fragment$", "");
        if (base.isEmpty()) return "A screen";
        String spaced = base.replaceAll("([a-z0-9])([A-Z])", "$1 $2").toLowerCase();
        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1) + (cls.contains("Fragment") ? " tab" : " screen");
    }

    // ── What happened ────────────────────────────────────────────────────

    private static String meaning(String type, String simple, String message, String trace) {
        switch (simple) {
            case "NullPointerException": {
                Matcher m = NPE_INVOKE.matcher(message);
                if (m.find()) {
                    String what = m.group(4);
                    String member = what.contains(".") ? what.substring(what.lastIndexOf('.') + 1) : what;
                    String owner = what.contains("(") ? what.substring(0, what.indexOf('(')) : what;
                    String ownerCls = owner.contains(".") ? owner.substring(0, owner.lastIndexOf('.')) : owner;
                    String ownerSimple = ownerCls.contains(".") ? ownerCls.substring(ownerCls.lastIndexOf('.') + 1) : ownerCls;
                    String verb = m.group(1).equals("invoke") ? "use '" + member.replace("()", "") + "' on" : m.group(1).equals("read from") ? "read '" + member + "' from" : "change '" + member + "' on";
                    return "The app tried to " + verb + " a " + friendlyType(ownerSimple) + " that did not exist at that moment. "
                            + "This usually means something was used before it finished loading, or after it had already been removed from the screen.";
                }
                return "The app tried to use something that was not there (an empty value). This usually means data or a screen element was used before it finished loading, or after it was gone.";
            }
            case "IndexOutOfBoundsException": case "ArrayIndexOutOfBoundsException": case "StringIndexOutOfBoundsException":
                return "The app asked for an item in a list (or a letter in a text) at a position that does not exist. " + (message.isEmpty() ? "" : "Detail: " + message + ". ")
                        + "This usually means a list was empty or shorter than expected.";
            case "ClassCastException":
                return "The app treated one kind of thing as another kind, and they did not match. This usually means data came back in a different shape than the app expected.";
            case "NumberFormatException":
                return "The app tried to read a number, but the text was not a valid number" + (message.isEmpty() ? "." : " (" + message + ").");
            case "ArithmeticException":
                return message.contains("zero") ? "The app tried to divide by zero." : "A calculation could not be completed (" + message + ").";
            case "OutOfMemoryError":
                return "The phone ran out of memory for the app. This usually happens when a very large picture or a very long list is loaded at once.";
            case "StackOverflowError":
                return "The app got stuck repeating the same step over and over until it ran out of room (an endless loop).";
            case "ConcurrentModificationException":
                return "The app changed a list while it was still going through it. This is a timing problem between two things happening at once.";
            case "NetworkOnMainThreadException":
                return "The app tried to use the internet in a way that freezes the screen, which Android does not allow.";
            case "CalledFromWrongThreadException":
                return "Something running in the background tried to change what is on the screen, which only the main screen thread may do.";
            case "NoClassDefFoundError": case "ClassNotFoundException": case "NoSuchMethodError": case "NoSuchFieldError": case "AbstractMethodError":
                return "A piece of the app that it needs is missing or does not match. This usually means the app was built from files that do not agree with each other, or a build is only partly updated.";
            case "UnsatisfiedLinkError":
                return "A built-in native component could not be loaded on this phone.";
            case "InflateException":
                return "A screen layout could not be created. A layout file has a mistake, or it uses something this phone does not support" + (trace.contains("Binary XML file line") ? " (see the line named below)." : ".");
            case "NotFoundException":
                return "The app asked for a picture, colour, text or layout that is not in the app" + (message.isEmpty() ? "." : " (" + message + ").");
            case "BadTokenException":
                return "The app tried to show a pop-up on a screen that was already closing.";
            case "ActivityNotFoundException":
                return "The app tried to open a screen or another app that is not installed or not registered" + (message.isEmpty() ? "." : " (" + message + ").");
            case "SecurityException":
                return "Android refused an action because the app does not have permission for it" + (message.isEmpty() ? "." : ": " + message);
            case "FileNotFoundException":
                return "The app looked for a file that is not there" + (message.isEmpty() ? "." : " (" + message + ").");
            case "JSONException":
                return "The app received data from the server that it could not read" + (message.isEmpty() ? "." : " (" + message + ").");
            case "SQLiteException": case "SQLiteFullException":
                return "The app's local database had a problem" + (message.isEmpty() ? "." : ": " + message);
            case "UnknownHostException": case "SocketTimeoutException": case "ConnectException": case "SSLException": case "SSLHandshakeException":
                return "The app could not reach the server. The internet connection may be down or very weak.";
            case "FirebaseFirestoreException":
                return message.contains("PERMISSION_DENIED") ? "The database refused the request because the account is not allowed to do that. This is usually a rules setting on the server."
                        : message.contains("UNAVAILABLE") ? "The database could not be reached. The internet connection may be down."
                        : "The online database reported a problem" + (message.isEmpty() ? "." : ": " + message);
            case "IllegalArgumentException":
                return "The app was given a value it cannot accept" + (message.isEmpty() ? "." : " (" + message + ").");
            case "UnsupportedOperationException":
                return "The app tried an action that this kind of list or object does not allow" + (message.isEmpty() ? "." : " (" + message + ").");
            case "IllegalStateException": {
                String m = message.toLowerCase();
                if (m.contains("not attached to") || m.contains("not attached to an activity"))
                    return "A part of a screen tried to do something after the screen it belongs to had already closed.";
                if (m.contains("already started") || m.contains("already been started"))
                    return "The app tried to start something that was already running.";
                if (m.contains("can't be called") || m.contains("cannot be called") || m.contains("destroyed"))
                    return "The app tried to do something after a screen had already been closed.";
                if (m.contains("lifecycle") || m.contains("after onsaveinstancestate"))
                    return "The app tried to change a screen at a moment when Android does not allow it (the screen was being saved or closed).";
                return "The app was in a state it did not expect" + (message.isEmpty() ? "." : ": " + message);
            }
            case "RuntimeException":
                if (message.contains("Unable to start activity") || message.contains("Unable to instantiate activity"))
                    return "A screen failed while it was opening.";
                return "The app hit an unexpected error" + (message.isEmpty() ? "." : ": " + message);
            default:
                return "The app hit an error it could not recover from (" + (simple.isEmpty() ? "unknown" : simple) + ")" + (message.isEmpty() ? "." : ": " + message);
        }
    }

    private static String friendlyType(String simple) {
        switch (simple) {
            case "View": return "screen element";
            case "TextView": return "text box";
            case "EditText": return "text field";
            case "ImageView": return "picture";
            case "Button": case "MaterialButton": return "button";
            case "RecyclerView": case "ListView": return "list";
            case "Context": return "screen context";
            case "String": return "piece of text";
            case "Map": case "HashMap": return "set of values";
            case "List": case "ArrayList": return "list of items";
            case "Object": return "value";
            default: return simple.isEmpty() ? "item" : simple;
        }
    }

    // ── What to do ───────────────────────────────────────────────────────

    private static String advice(String type, String simple, String message, String trace) {
        List<String> tips = new ArrayList<>();
        tips.add("Tap Restart App and try again.");
        switch (simple) {
            case "OutOfMemoryError": tips.add("Close other apps first, and try again with a smaller picture."); break;
            case "UnknownHostException": case "SocketTimeoutException": case "ConnectException": case "SSLException": case "SSLHandshakeException":
                tips.clear(); tips.add("Check your internet connection, then tap Restart App."); break;
            case "NoClassDefFoundError": case "ClassNotFoundException": case "NoSuchMethodError": case "NoSuchFieldError":
                tips.add("If you build the app yourself, do a clean build and reinstall."); break;
            case "SecurityException": tips.add("Check the app's permissions in your phone's settings."); break;
            case "FirebaseFirestoreException":
                if (message.contains("PERMISSION_DENIED")) tips.add("The server's rules may need updating. Send the log below to the developer.");
                break;
            default: break;
        }
        tips.add("If it keeps happening, tap Copy Log and send it to the developer.");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tips.size(); i++) { if (i > 0) sb.append(' '); sb.append(tips.get(i)); }
        return sb.toString();
    }
}
