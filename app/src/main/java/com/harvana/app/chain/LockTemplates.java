package com.harvana.app.chain;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Checks a v2 member-lock template before the wallet signs it (node docs ANDROID_V2_SIGNING.md). The wallet signs only
 * a lock that moves this wallet's own HARV, on the Harvana chain, with the type the node requires for its memo
 * (the node's MemberLocks.TYPE_BY_PREFIX, copied here). Anything else is refused, whatever the node sent.
 */
public final class LockTemplates {
    private LockTemplates() {}

    public static final String CHAIN_ID = "HARVANA-HARV-1";

    /** Memo prefix → required type. Same table as the node's core/MemberLocks. */
    public static final Map<String, String> TYPE_BY_PREFIX;
    static {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("CIRCLE_CREATION_FEE:", "GROUP_FEE");
        m.put("ACCUM_CREATION_FEE:", "GROUP_FEE");
        m.put("FIXED_CREATION_FEE:", "GROUP_FEE");
        m.put("CIRCLE_BOND_LOCK:", "CIRCLE_BOND");
        m.put("CIRCLE_CONTRIBUTION:", "CIRCLE_CONTRIBUTE");
        m.put("CIRCLE_CURE:", "CIRCLE_BOND");
        m.put("ACCUM_LINE_CONTRIBUTION:", "ACCUM_CONTRIBUTE");
        m.put("ACCUM_LINE_VARIABLE_CONTRIBUTION:", "ACCUM_CONTRIBUTE");
        m.put("FIXED_DEPOSIT:", "FIXED_DEPOSIT");
        m.put("GENLOAN_REPAY:", "GENLOAN_REPAY");
        m.put("P2P_LISTING_LOCK:", "P2P_LOCK");
        m.put("P2P_FULFILL_LOCK:", "P2P_LOCK");
        m.put("P2P_INSURANCE_DEPOSIT:", "P2P_INSURANCE_DEPOSIT");
        TYPE_BY_PREFIX = java.util.Collections.unmodifiableMap(m);
    }

    /** The type a memo requires, or null when the memo is not a member lock. */
    public static String requiredType(String memo) {
        if (memo == null) return null;
        for (Map.Entry<String, String> e : TYPE_BY_PREFIX.entrySet()) if (memo.startsWith(e.getKey())) return e.getValue();
        return null;
    }

    /** Null when the template may be signed by {@code wallet}; otherwise why not. */
    public static String check(JSONObject t, String wallet) {
        if (t == null) return "Empty payment.";
        if (t.optInt("version", 0) != 2) return "Unsupported payment version.";
        if (!CHAIN_ID.equals(t.optString("chainId"))) return "Payment is for another chain.";
        String from = t.optString("from");
        if (wallet == null || wallet.isEmpty() || !from.equalsIgnoreCase(wallet)) return "Payment is not from this wallet.";
        String to = t.optString("to");
        if (!to.matches("HV0x[0-9a-fA-F]{40}")) return "Payment has no valid destination.";
        if (to.equalsIgnoreCase(from)) return "Payment goes back to the same wallet.";
        if (!"HARV".equals(t.optString("assetId", "HARV"))) return "Payment is not in HARV.";
        if (t.optLong("amount", 0) <= 0) return "Payment amount is not positive.";
        if (t.optLong("maxFee", -1) < 0) return "Payment fee is invalid.";
        if (t.optLong("gasLimit", 0) != 0) return "Payment gas limit is invalid.";
        if (!t.optString("payload", "").isEmpty()) return "Payment carries unexpected data.";
        String need = requiredType(t.optString("memo", ""));
        if (need == null) return "Payment is not a group or P2P lock.";
        if (!need.equals(t.optString("type"))) return "Payment type does not match its purpose.";
        long expires = t.optLong("templateExpiresTimestamp", 0);
        if (expires > 0 && expires < System.currentTimeMillis()) return "Payment request expired. Do the action again.";
        return null;
    }

    /** Null when every template may be signed; else the first reason. */
    public static String checkAll(JSONArray templates, String wallet) {
        if (templates == null || templates.length() == 0) return "No payment to sign.";
        if (templates.length() > 20) return "Too many payments in one request.";
        for (int i = 0; i < templates.length(); i++) {
            String why = check(templates.optJSONObject(i), wallet);
            if (why != null) return why;
        }
        return null;
    }

    /** A stable key for one template (to avoid asking twice for the same one in a session). */
    public static String key(JSONObject t) {
        return t.optString("from") + "|" + t.optString("to") + "|" + t.optLong("amount") + "|" + t.optString("memo");
    }
}
