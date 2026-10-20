package com.harvana.app.wallet;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Talks to Pi's own Horizon server (api.mainnet.minepi.com): the account (sequence, balance), recent payments, and sending a
 * signed payment (PiTx). Blocking calls: run them off the main thread. No Harvana server is involved.
 */
public final class PiClient {
    private PiClient() {}

    /** The Pi server for the network chosen on My wallet list. Read again each call, so a switch takes effect immediately. */
    public static String horizon() { return Net.piHorizon(); }
    private static final OkHttpClient HTTP = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build();

    public static final class Payment {
        public String direction = "", counterparty = "", amount = "", when = "", hash = "";
    }

    /** Sends {@code amountPi} Pi from the wallet behind {@code mnemonic} to {@code destination}. Returns the transaction hash. */
    public static String send(String mnemonic, String destination, String amountPi, String memo) throws Exception {
        byte[] seed = PiKeys.piSeed(mnemonic);
        String me = PiKeys.strKeyAccount(com.harvana.app.crypto.Ed25519.publicKeyFromSeed(seed));
        if (me.equals(destination)) throw new Exception("That is your own Pi address.");
        JSONObject acct = account(me);
        if (acct == null) throw new Exception("Your Pi wallet is not active on the network yet.");
        long seq = Long.parseLong(acct.getString("sequence"));
        boolean exists = account(destination) != null;
        int fee = baseFee();
        long maxTime = System.currentTimeMillis() / 1000 + 300;      // the network drops it after 5 minutes
        String env = PiTx.envelope(seed, seq, destination, PiTx.toStroops(amountPi), fee, maxTime, memo, !exists);
        Request req = new Request.Builder().url(horizon() + "/transactions").post(new FormBody.Builder().add("tx", env).build()).build();
        try (Response r = HTTP.newCall(req).execute()) {
            JSONObject j = new JSONObject(r.body() == null ? "{}" : r.body().string());
            if (r.isSuccessful()) return j.optString("hash", "");
            throw new Exception(friendly(j));
        }
    }

    /** The account JSON, or null when the network has no such account. */
    public static JSONObject account(String address) throws Exception {
        try (Response r = HTTP.newCall(new Request.Builder().url(horizon() + "/accounts/" + address).build()).execute()) {
            if (r.code() == 404) return null;
            if (!r.isSuccessful()) throw new Exception("Pi network unavailable (" + r.code() + ")");
            return new JSONObject(r.body() == null ? "{}" : r.body().string());
        }
    }

    /** The newest payments to and from an address. */
    public static List<Payment> history(String address) throws Exception {
        List<Payment> out = new ArrayList<>();
        try (Response r = HTTP.newCall(new Request.Builder().url(horizon() + "/accounts/" + address + "/payments?order=desc&limit=25").build()).execute()) {
            if (r.code() == 404) return out;
            JSONObject j = new JSONObject(r.body() == null ? "{}" : r.body().string());
            JSONObject embedded = j.optJSONObject("_embedded");
            JSONArray recs = embedded == null ? null : embedded.optJSONArray("records");
            if (recs == null) return out;
            for (int i = 0; i < recs.length(); i++) {
                JSONObject x = recs.getJSONObject(i);
                Payment p = new Payment();
                String type = x.optString("type");
                if ("create_account".equals(type)) {
                    boolean mine = address.equals(x.optString("account"));
                    p.direction = mine ? "Received" : "Sent";
                    p.counterparty = mine ? x.optString("funder") : x.optString("account");
                    p.amount = x.optString("starting_balance");
                } else if ("payment".equals(type) && "native".equals(x.optString("asset_type"))) {
                    boolean in = address.equals(x.optString("to"));
                    p.direction = in ? "Received" : "Sent";
                    p.counterparty = in ? x.optString("from") : x.optString("to");
                    p.amount = x.optString("amount");
                } else continue;
                p.when = x.optString("created_at").replace('T', ' ').replace("Z", "");
                p.hash = x.optString("transaction_hash");
                out.add(p);
            }
        }
        return out;
    }

    /** Live fee and the most that can be sent: Pi keeps a reserve of (2 + entries) × the base reserve in every wallet. */
    public static ChainSend.Quote quote(String address) throws Exception {
        ChainSend.Quote q = new ChainSend.Quote();
        q.feeSymbol = "PI";
        q.feeInSameCoin = true;
        int fee = baseFee();
        q.fee = java.math.BigDecimal.valueOf(fee).movePointLeft(7);
        JSONObject acc = account(address);
        java.math.BigDecimal native_ = java.math.BigDecimal.ZERO;
        org.json.JSONArray bal = acc == null ? null : acc.optJSONArray("balances");
        for (int i = 0; bal != null && i < bal.length(); i++) {
            JSONObject b = bal.optJSONObject(i);
            if (b != null && "native".equals(b.optString("asset_type"))) native_ = new java.math.BigDecimal(b.optString("balance", "0"));
        }
        java.math.BigDecimal baseReserve = new java.math.BigDecimal("0.5");
        try (Response r = HTTP.newCall(new Request.Builder().url(horizon() + "/ledgers?order=desc&limit=1").build()).execute()) {
            JSONObject j = new JSONObject(r.body() == null ? "{}" : r.body().string());
            JSONObject rec = j.optJSONObject("_embedded") == null ? null : j.optJSONObject("_embedded").optJSONArray("records").optJSONObject(0);
            if (rec != null && rec.optLong("base_reserve_in_stroops", 0) > 0) baseReserve = java.math.BigDecimal.valueOf(rec.optLong("base_reserve_in_stroops")).movePointLeft(7);
        } catch (Exception ignored) { }
        int entries = acc == null ? 0 : acc.optInt("subentry_count", 0);
        java.math.BigDecimal reserve = baseReserve.multiply(java.math.BigDecimal.valueOf(2L + entries));
        q.maxSend = native_.subtract(reserve).subtract(q.fee).max(java.math.BigDecimal.ZERO);
        return q;
    }

    private static int baseFee() {
        try (Response r = HTTP.newCall(new Request.Builder().url(horizon() + "/fee_stats").build()).execute()) {
            JSONObject j = new JSONObject(r.body() == null ? "{}" : r.body().string());
            int f = Integer.parseInt(j.optString("last_ledger_base_fee", "100000"));
            return Math.max(f, 100000);
        } catch (Exception e) {
            return 100000;                                   // 0.01 Pi
        }
    }

    private static String friendly(JSONObject err) {
        JSONObject extras = err.optJSONObject("extras");
        JSONObject codes = extras == null ? null : extras.optJSONObject("result_codes");
        String s = codes == null ? "" : codes.toString();
        if (s.contains("underfunded") || s.contains("insufficient_balance")) return "Not enough Pi (the network also keeps a small reserve in every wallet).";
        if (s.contains("low_reserve")) return "A new Pi wallet must receive at least the network's minimum balance.";
        if (s.contains("bad_seq")) return "Your Pi wallet changed while sending. Try again.";
        if (s.contains("tx_too_late")) return "The payment took too long and expired. Try again.";
        return err.optString("title", "The Pi network refused this payment") + (s.isEmpty() ? "" : " " + s);
    }
}
