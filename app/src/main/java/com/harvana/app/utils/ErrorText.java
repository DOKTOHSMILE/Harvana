package com.harvana.app.utils;

import org.json.JSONObject;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Turns every failure from the network into one plain English sentence, and makes sure nothing about the server ever
 * reaches the screen: no addresses, paths, timestamps, status words, class names or raw replies.
 *
 * The node answers a refusal with a code, {"error":"NO_LIQUIDITY_HNGN"}. {@link #fromHttp} reads only that code (never the
 * rest of the reply) and returns the sentence for it. Screens and logic that check for a code ("404", "NOT_FOUND",
 * "INSUFFICIENT_HARV", …) keep working through {@link #is}, which knows which code each sentence came from.
 */
public final class ErrorText {
    private ErrorText() {}

    /** Sentence → the code it was made from ("HTTP 404 NOT_FOUND"), for {@link #is}. Bounded, newest kept. */
    private static final Map<String, String> ORIGIN = Collections.synchronizedMap(new LinkedHashMap<String, String>(64, 0.75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, String> e) { return size() > 300; }
    });

    /** A reply that was not 2xx: its HTTP status and body. */
    public static String fromHttp(int status, String body) {
        String code = codeIn(body);
        if (code.isEmpty()) code = statusCode(status);
        String text = english(code, status);
        ORIGIN.put(text, "HTTP " + status + " " + code);
        return text;
    }

    /** A reply with a human message of its own (our cloud functions): kept if it is a safe sentence, else as {@link #fromHttp}. */
    public static String withStatus(int status, String message) {
        String m = message == null ? "" : message.trim();
        if (m.isEmpty() || m.startsWith("{") || m.matches("[A-Z][A-Z0-9_:]{2,80}")) return fromHttp(status, m);
        String text = plain(m);
        ORIGIN.put(text, "HTTP " + status + " " + statusCode(status));
        return text;
    }

    /** No answer at all (no connection, timeout, bad certificate…). The exception's own text is never shown. */
    public static String fromException(Throwable e) {
        String n = e == null ? "" : e.getClass().getSimpleName();
        String text;
        if (n.contains("UnknownHost") || n.contains("NoRouteToHost") || n.contains("ConnectException")) {
            text = "No connection to the Harvana network. Check your internet and try again.";
        } else if (n.contains("Timeout")) {
            text = "The network took too long to answer. Check your internet and try again.";
        } else if (n.contains("SSL") || n.contains("Certificate")) {
            text = "A secure connection to the network couldn't be made. Check your phone's date and time, or try another network.";
        } else {
            text = "Couldn't reach the Harvana network. Try again in a moment.";
        }
        ORIGIN.put(text, "NETWORK " + n.toUpperCase(Locale.ROOT));
        return text;
    }

    /**
     * Any exception, for showing: a connection problem reads as one, a Firebase refusal by its kind, anything else as a
     * plain "something went wrong". The exception's own message (which can name files, hosts or classes) is never shown.
     */
    public static String of(Throwable e) {
        if (e == null) return "Something went wrong. Try again.";
        String n = e.getClass().getName();
        if (e instanceof java.io.IOException && !(e instanceof java.io.FileNotFoundException)) return fromException(e);
        String m = String.valueOf(e.getMessage()).toUpperCase(Locale.ROOT);
        if (n.contains("firebase") || n.contains("Firebase")) {
            if (m.contains("PERMISSION_DENIED")) return "You aren't allowed to do that.";
            if (m.contains("UNAVAILABLE") || m.contains("NETWORK")) return "No connection. Check your internet and try again.";
            if (m.contains("NOT_FOUND")) return "That isn't there any more.";
            if (m.contains("UNAUTHENTICATED")) return "Sign in again and try once more.";
        }
        android.util.Log.w("ErrorText", "shown as a plain error", e);
        return "Something went wrong. Try again.";
    }

    /** The answer was 2xx but not readable. */
    public static String unreadable() {
        String text = "The network sent an answer the app couldn't read. Update the app or try again.";
        ORIGIN.put(text, "UNREADABLE_REPLY");
        return text;
    }

    /**
     * True when {@code error} contains {@code needle} (a code like "404", "NOT_FOUND", "INSUFFICIENT_"), either as written
     * or in the code a sentence of this class came from. Case does not matter.
     */
    public static boolean is(String error, String needle) {
        if (error == null || needle == null) return false;
        String e = error.toUpperCase(Locale.ROOT), n = needle.toUpperCase(Locale.ROOT);
        if (e.contains(n)) return true;
        synchronized (ORIGIN) {
            for (Map.Entry<String, String> m : ORIGIN.entrySet()) {
                if (m.getValue().toUpperCase(Locale.ROOT).contains(n) && e.contains(m.getKey().toUpperCase(Locale.ROOT))) return true;
            }
        }
        return false;
    }

    /**
     * Any error text made safe to show: a sentence of this class is returned as it is; anything else that looks like a
     * code or a raw reply becomes English; text with server details (paths, addresses, JSON) is replaced.
     */
    public static String plain(String error) {
        if (error == null || error.trim().isEmpty()) return "Something went wrong. Try again.";
        String e = error.trim();
        if (ORIGIN.containsKey(e)) return e;
        if (e.startsWith("{")) return fromHttp(0, e);
        if (e.matches("[A-Z][A-Z0-9_]{2,}")) return english(e, 0);
        if (e.contains("/api/") || e.contains("://") || e.contains("Exception") || e.contains("\tat ") || e.contains("\"timestamp\"")) {
            return "Something went wrong. Try again.";
        }
        return e;
    }

    // ── codes ─────────────────────────────────────────────────────────────────

    /** The node's error code from a reply body, or "" (only an UPPER_CASE code is taken, never other text). */
    static String codeIn(String body) {
        if (body == null) return "";
        try {
            JSONObject j = new JSONObject(body);
            for (String k : new String[]{"error", "code", "message", "reason"}) {
                String v = j.optString(k, "").trim();
                if (v.matches("[A-Z][A-Z0-9_:]{2,80}")) return v;
            }
        } catch (Exception ignored) { }
        String b = body.trim();
        return b.matches("[A-Z][A-Z0-9_]{2,80}") ? b : "";
    }

    static String statusCode(int status) {
        switch (status) {
            case 400: return "BAD_REQUEST";
            case 401: return "UNAUTHORIZED";
            case 403: return "FORBIDDEN";
            case 404: return "NOT_FOUND";
            case 405: return "METHOD_NOT_ALLOWED";
            case 409: return "CONFLICT";
            case 413: return "TOO_LARGE";
            case 429: return "TOO_MANY_REQUESTS";
            default: return status >= 500 ? "SERVER_ERROR" : "REFUSED";
        }
    }

    private static final Map<String, String> WORDS = new LinkedHashMap<>();
    static {
        // general
        WORDS.put("NOT_FOUND", "This isn't on the network. It may have been removed, or this part of the network isn't switched on yet.");
        WORDS.put("BAD_REQUEST", "The network couldn't accept this request. Check what you entered and try again.");
        WORDS.put("UNAUTHORIZED", "Your session with the network has ended. Try again; the app signs in again by itself.");
        WORDS.put("FORBIDDEN", "This wallet isn't allowed to do that.");
        WORDS.put("METHOD_NOT_ALLOWED", "This version of the app asked the network in a way it no longer accepts. Update the app.");
        WORDS.put("CONFLICT", "This was already done.");
        WORDS.put("TOO_LARGE", "That is too large to send.");
        WORDS.put("TOO_MANY_REQUESTS", "Too many requests just now. Wait a moment and try again.");
        WORDS.put("SERVER_ERROR", "The network is having trouble right now. Try again in a minute.");
        WORDS.put("INTERNAL_ERROR", "The network is having trouble right now. Try again in a minute.");
        WORDS.put("UNAVAILABLE", "The network is busy or restarting. Try again in a minute.");
        WORDS.put("REFUSED", "The network refused this request.");
        // signing and nonces
        WORDS.put("INVALID_SIGNATURE", "This wallet's signature couldn't be checked. Try again.");
        WORDS.put("MISSING_SIGNATURE", "This request wasn't signed by your wallet. Try again.");
        WORDS.put("PUBLIC_KEY_DOES_NOT_OWN_ADDRESS", "This wallet's key doesn't match its address. Re-import the wallet from its recovery phrase.");
        WORDS.put("PUBLIC_KEY_MISMATCH", "This wallet's key doesn't match its address. Re-import the wallet from its recovery phrase.");
        WORDS.put("INVALID_NONCE", "Another request from this wallet went through first. Try again.");
        WORDS.put("NONCE_ALREADY_USED", "Another request from this wallet went through first. Try again.");
        WORDS.put("NODE_HELD_WALLET", "That is a Harvana group, P2P or swap wallet. Pay it from its own screen, not by sending to the address.");
        WORDS.put("WALLET_LINKED_LESS_THAN_24H", "This wallet was added to your account less than 24 hours ago. Use the wallet that holds the savings, or try again after 24 hours.");
        WORDS.put("NOT_YOUR_WALLET", "That wallet isn't on the same account as the wallet that holds this place.");
        WORDS.put("NOT_THE_LINE_OWNER", "This savings line belongs to another wallet. Switch to the wallet that holds it.");
        WORDS.put("WALLET_ALREADY_LINKED", "This wallet belongs to another account. A wallet can belong to one person only.");
        WORDS.put("INVALID_ID_TOKEN", "Your sign-in couldn't be checked. Sign out and sign in again.");
        WORDS.put("ID_TOKEN_EXPIRED", "Your sign-in has expired. Sign out and sign in again.");
        WORDS.put("STALE_TIMESTAMP", "Your phone's clock is off. Set the date and time to automatic and try again.");
        WORDS.put("INVALID_ADDRESS", "That isn't a valid Harvana address.");
        WORDS.put("INVALID_AMOUNT", "Enter an amount greater than zero.");
        WORDS.put("AMOUNT_TOO_SMALL", "That amount is too small.");
        WORDS.put("INSUFFICIENT_BALANCE", "Not enough HARV in this wallet for this and its tFare.");
        WORDS.put("INSUFFICIENT_HARV", "Not enough HARV in this wallet for this and its tFare.");
        WORDS.put("SENDER_ADDRESS_FROZEN", "This wallet is frozen and can't send right now.");
        WORDS.put("RESERVED_MEMO_PREFIX", "That note is reserved by the network. Use a different note.");
        WORDS.put("MEMO_TOO_LONG", "The note is too long.");
        WORDS.put("ACTION_FEE_MISSING", "The small tFare for this action wasn't paid. Make sure the wallet holds some HARV and try again.");
        WORDS.put("ACTION_FEE_NOT_FOUND", "The tFare for this action hasn't arrived yet. Try again in a moment.");
        WORDS.put("UNKNOWN_ASSET", "The network doesn't know that coin.");
        // swaps and vaults
        WORDS.put("PRICE_MOVED", "The price moved. Check the new estimate and try again.");
        WORDS.put("SAME_ASSET", "Pick two different coins.");
        WORDS.put("PICK_A_COIN", "Pick a coin to pair with HARV.");
        WORDS.put("MORE_THAN_YOUR_SHARES", "That is more than your share of the pool.");
        WORDS.put("ESCROW_NOT_CONFIRMED", "The seller's coins are not locked in escrow yet. Wait for the lock, then pay.");
        WORDS.put("TOO_MANY_UNPAID_ORDERS", "Too many unpaid orders cancelled in the last 24 hours. New orders open again later.");
        WORDS.put("DISPUTE_AFTER_PAYMENT_MARKED", "An appeal opens after the payment is marked paid.");
        WORDS.put("ORDER_NOT_CANCELLABLE", "This order can no longer be cancelled.");
        WORDS.put("NOT_ORDER_BUYER", "Only the buyer can do this.");
        WORDS.put("REASON_REQUIRED", "Add the appeal details.");
        WORDS.put("REASON_TOO_LONG", "The appeal details are too long.");
        WORDS.put("CANNOT_FULFILL_OWN_LISTING", "You can't trade with your own offer.");
        WORDS.put("PRICE_NOT_LIVE_YET", "HARV has no market price yet: the network needs 10 completed P2P trades first.");
        WORDS.put("NOT_ENOUGH_COLLATERAL", "Not enough HARV locked for that amount.");
        WORDS.put("WOULD_BE_UNDER_COLLATERALISED", "That would leave too little HARV for what you owe. Repay first.");
        WORDS.put("VAULT_CAP_REACHED", "The network's limit for coins minted this way is reached for now.");
        WORDS.put("PRICES_DISAGREE", "HARV's pool price and market price are too far apart. Minting and withdrawals wait until they agree.");
        WORDS.put("LIQUIDATION_PENDING", "This vault is being liquidated. Try again in a minute.");
        WORDS.put("LIQUIDATION_ALREADY_PENDING", "Someone is already liquidating this vault.");
        WORDS.put("VAULT_HOURLY_LIMIT_REACHED", "The hourly limit for minting is reached. Try again next hour.");
        WORDS.put("POSITION_IS_SAFE", "That vault is back above the line.");
        WORDS.put("MORE_THAN_THE_DEBT", "That is more than is owed.");
        WORDS.put("MINT_BELOW_MINIMUM", "That is below the minimum you can mint.");
        // groups
        WORDS.put("GROUP_NOT_FOUND", "This group isn't on the network any more.");
        WORDS.put("NOT_A_MEMBER", "Only members of this group can do that.");
        WORDS.put("NOT_A_GROUP_MEMBER", "Only members of this group can do that.");
        WORDS.put("ALREADY_A_MEMBER", "You're already a member of this group.");
        WORDS.put("GROUP_FULL", "This group is full.");
        WORDS.put("ADMIN_ONLY", "Only the group's admin can do that.");
        WORDS.put("ALREADY_VOTED", "You've already voted on this.");
    }

    /** The sentence for a code. Families ("INSUFFICIENT_HNGN", "HEUR_NOT_ACTIVE", "NO_LIQUIDITY_HUSD") are handled
     *  by pattern; any other code is spelled out as words, so even a new one reads as English. */
    static String english(String code, int status) {
        String c = code.contains(":") ? code.substring(0, code.indexOf(':')) : code;
        String w = WORDS.get(c);
        if (w != null) return w;
        if (c.startsWith("INSUFFICIENT_")) return "Not enough " + coin(c.substring(13)) + " in this wallet for this and its fee.";
        if (c.endsWith("_NOT_ACTIVE")) return coin(c.substring(0, c.length() - 11)) + " isn't switched on yet on the network.";
        if (c.startsWith("NO_LIQUIDITY_")) return "The HARV / " + coin(c.substring(13)) + " pool is empty. Add HARV and the coin to it to open it.";
        if (c.startsWith("VAULT_NOT_AVAILABLE_FOR_")) return coin(c.substring(24)) + " can't be minted with HARV yet.";
        if (c.startsWith("PRICE_UNAVAILABLE")) return "The HARV price is out of date, so this is paused for safety. Try again later.";
        if (c.startsWith("SYSTEM_TX_REJECTED")) return "The network couldn't build this transaction. Try again in a minute.";
        if (c.startsWith("BLOCK_REJECTED")) return "The network refused this transaction.";
        if (c.endsWith("_NOT_FOUND")) return words(c.substring(0, c.length() - 10)) + " wasn't found on the network.";
        if (c.startsWith("ALREADY_") || c.endsWith("_ALREADY_EXISTS")) return words(c) + ".";
        if (c.startsWith("INVALID_")) return "That " + words(c.substring(8)).toLowerCase(Locale.ROOT) + " isn't valid.";
        if (c.startsWith("NOT_")) return words(c) + ".";
        if (status >= 500) return WORDS.get("SERVER_ERROR");
        return "The network refused this: " + words(c).toLowerCase(Locale.ROOT) + ".";
    }

    /** "HNGN" → "hNGN", "HARV" → "HARV", anything else as words. */
    private static String coin(String c) {
        if ("HARV".equals(c) || "BALANCE".equals(c)) return "HARV";
        if (c.length() == 4 && c.charAt(0) == 'H') return "h" + c.substring(1);
        return words(c).toLowerCase(Locale.ROOT);
    }

    /** "MORE_THAN_YOUR_SHARES" → "More than your shares". */
    static String words(String c) {
        String s = c.replace('_', ' ').trim().toLowerCase(Locale.ROOT);
        return s.isEmpty() ? "" : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
