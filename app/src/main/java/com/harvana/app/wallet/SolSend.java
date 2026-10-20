package com.harvana.app.wallet;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Sends a signed Solana transaction (owner, 20 Oct 2026: "Transaction simulation failed" on a plain SOL send).
 * The blockhash is read at "confirmed"; the node's check (preflight) now runs at the same commitment, where it
 * defaulted to "finalized" and could not find that blockhash yet. A "blockhash not found" is retried once with a new
 * one, and Solana's own reason (the failed instruction and its log) is turned into a plain message.
 */
final class SolSend {
    private SolSend() {}

    interface Build { byte[] tx(byte[] blockhash) throws Exception; }

    static String send(String rpc, Build build) throws Exception {
        String last = "";
        for (int attempt = 0; attempt < 2; attempt++) {
            JSONObject bh = SplToken.call(rpc, "getLatestBlockhash", new JSONArray().put(new JSONObject().put("commitment", "confirmed")));
            byte[] blockhash = ChainSend.b58(bh.getJSONObject("result").getJSONObject("value").getString("blockhash"));
            String b64 = android.util.Base64.encodeToString(build.tx(blockhash), android.util.Base64.NO_WRAP);
            JSONObject opts = new JSONObject().put("encoding", "base64").put("preflightCommitment", "confirmed").put("maxRetries", 5);
            JSONObject sent = SplToken.call(rpc, "sendTransaction", new JSONArray().put(b64).put(opts));
            if (!sent.has("error")) return sent.getString("result");
            JSONObject err = sent.getJSONObject("error");
            last = explain(err);
            if (!last.toLowerCase(java.util.Locale.US).contains("blockhash")) break;   // only a stale blockhash is worth a retry
        }
        throw new IllegalArgumentException(last);
    }

    /** Solana's message plus what the simulation saw, in words a person can act on. */
    static String explain(JSONObject err) {
        String msg = err.optString("message", "Solana rejected the payment.");
        JSONObject data = err.optJSONObject("data");
        String detail = data == null ? "" : String.valueOf(data.opt("err"));
        JSONArray logs = data == null ? null : data.optJSONArray("logs");
        String all = (msg + " " + detail + " " + (logs == null ? "" : logs.toString())).toLowerCase(java.util.Locale.US);
        if (all.contains("insufficientfundsforrent")) {
            return "Solana keeps at least 0.00089088 SOL in every account. Send a bit less (or everything), or send more to a new account.";
        }
        if (all.contains("insufficient lamports") || all.contains("insufficient funds")) return "Not enough SOL for the amount and the network fee.";
        if (all.contains("blockhash not found")) return "Solana was busy. Try again in a moment.";
        if (all.contains("accountnotfound") || all.contains("no record of a prior credit")) return "This Solana account has no SOL yet on the network.";
        String last = "";
        for (int i = logs == null ? -1 : logs.length() - 1; i >= 0; i--) {
            String l = logs.optString(i);
            if (l.contains("failed") || l.contains("Error") || l.contains("error")) { last = l.replace("Program log: ", ""); break; }
        }
        if (!last.isEmpty()) return msg.replace("Transaction simulation failed: ", "") + " · " + last;
        return detail.isEmpty() || "null".equals(detail) ? msg : msg + " · " + detail;
    }
}
