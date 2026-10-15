package com.harvana.app.notify;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Turns confirmed wallet transactions into the notifications a member should see (pure logic, unit-tested):
 * money received, money sent, and payments the wallet signed for a group or trade (locks, contributions, bonds).
 */
public final class TxEvents {
    private TxEvents() {}

    public static final class Tx {
        public String hash = "", from = "", to = "", memo = "", status = "confirmed";
        public double amount;
        public long timestamp;
        /** Display symbol when this row is not the default coin. Empty means use the ticker passed to classify. */
        public String symbol = "";
    }

    public static final class Event {
        public final String kind; // RECEIVED, SENT, SIGNED
        public final String key, title, body;
        /** When the transfer was confirmed. 0 until classify sets it. */
        public long at;
        Event(String kind, String key, String title, String body) { this.kind = kind; this.key = key; this.title = title; this.body = body; }
    }

    /** "circle", "savings group", "fixed group", "trade" or "" from a transaction memo such as CIRCLE_CONTRIBUTION:CIRC-1:2. */
    public static String scope(String memo) {
        String m = memo == null ? "" : memo.toUpperCase(Locale.US);
        if (m.startsWith("CIRCLE_") || m.startsWith("CIRC")) return "circle";
        if (m.startsWith("ACCUM")) return "savings group";
        if (m.startsWith("FIXED")) return "fixed group";
        if (m.startsWith("P2P")) return "trade";
        if (m.startsWith("GENLOAN") || m.startsWith("LOAN")) return "loan";
        return "";
    }

    /** What a memo means for the person: "Refund", "Payout", "Bond withdrawal", "Reward", "Penalty" or "". */
    public static String purpose(String memo) {
        String m = memo == null ? "" : memo.toUpperCase(Locale.US);
        if (m.contains("REFUND")) return "Refund";
        if (m.contains("PAYOUT") || m.contains("DISBURSE")) return "Payout";
        if (m.contains("WITHDRAWAL")) return "Withdrawal";
        if (m.contains("REWARD")) return "Reward";
        if (m.contains("PENALTY")) return "Penalty";
        return "";
    }

    public static String shortAddr(String a) {
        return a == null || a.length() <= 14 ? (a == null ? "" : a) : a.substring(0, 8) + "…" + a.substring(a.length() - 4);
    }

    /** Four decimals without trailing zeros: 12.5 -> "12.5", 3 -> "3". */
    public static String amt(double v) {
        String s = String.format(Locale.US, "%.4f", v);
        s = s.contains(".") ? s.replaceAll("0+$", "").replaceAll("\\.$", "") : s;
        return s.isEmpty() ? "0" : s;
    }

    /** Only confirmed transactions newer than {@code baselineTs}; each yields at most one event. */
    public static List<Event> classify(List<Tx> txs, String me, long baselineTs, String ticker) {
        List<Event> out = new ArrayList<>();
        if (me == null || me.isEmpty()) return out;
        for (Tx t : txs) {
            if (t.hash.isEmpty() || t.timestamp <= baselineTs) continue;
            if (!"confirmed".equalsIgnoreCase(t.status)) continue;
            boolean incoming = me.equalsIgnoreCase(t.to) && !me.equalsIgnoreCase(t.from);
            boolean outgoing = me.equalsIgnoreCase(t.from) && !me.equalsIgnoreCase(t.to);
            String scope = scope(t.memo), purpose = purpose(t.memo);
            String unit = t.symbol != null && !t.symbol.isEmpty() ? t.symbol : ticker;
            String amount = amt(t.amount) + " " + unit;
            if (incoming) {
                String title = purpose.isEmpty() ? unit + " received" : purpose + " received";
                String body = amount + (scope.isEmpty() ? " from " + shortAddr(t.from) : " from your " + scope);
                out.add(event("RECEIVED", "tx_" + t.hash + "_in", title, body, t.timestamp));
            } else if (outgoing) {
                if (scope.isEmpty()) {
                    out.add(event("SENT", "tx_" + t.hash + "_out", unit + " sent", amount + " to " + shortAddr(t.to) + " was confirmed.", t.timestamp));
                } else {
                    out.add(event("SIGNED", "tx_" + t.hash + "_out", "Payment signed and confirmed",
                        amount + " for your " + scope + (purpose.isEmpty() ? "" : " (" + purpose.toLowerCase(Locale.US) + ")") + " went through.", t.timestamp));
                }
            }
        }
        return out;
    }

    private static Event event(String kind, String key, String title, String body, long at) {
        Event e = new Event(kind, key, title, body);
        e.at = at;
        return e;
    }
}
