package com.harvana.app.wallet;

import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Balances on other chains, read straight from free public services (no Harvana server, nothing to pay for):
 *   Bitcoin  mempool.space, Litecoin litecoinspace.org (same API), Dogecoin api.blockcypher.com,
 *   Ethereum / BNB Chain / Base / Polygon: publicnode.com JSON-RPC, Tron api.trongrid.io (TRX and USDT), Solana the public RPC,
 *   Pi Network: Pi's own Horizon server (api.mainnet.minepi.com).
 * Each call is made from the phone; a failure shows as "unavailable" and never blocks anything else. Blocking: run off the main thread.
 */
public final class ChainBalances {
    private ChainBalances() {}

    private static final OkHttpClient HTTP = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).build();
    private static final MediaType JSON = MediaType.parse("application/json");
    public static final String USDT_TRON = "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t";

    /** One line per asset held, e.g. "0.0012 BTC" or "12.5 TRX · 40 USDT". */
    public static String describe(ChainKeys.Chain chain, String address) {
        try {
            switch (chain) {
                case BTC: return mempool("https://mempool.space/api/address/", address, "BTC");
                case LTC: return mempool("https://litecoinspace.org/api/address/", address, "LTC");
                case DOGE: {
                    JSONObject j = get("https://api.blockcypher.com/v1/doge/main/addrs/" + address + "/balance");
                    return amount(BigInteger.valueOf(j.optLong("final_balance")), 8) + " DOGE";
                }
                case ETH: {
                    String eth = amount(evmBalance("https://ethereum-rpc.publicnode.com", address), 18);
                    String bnb = amount(evmBalance("https://bsc-rpc.publicnode.com", address), 18);
                    String pol = amount(evmBalance("https://polygon-bor-rpc.publicnode.com", address), 18);
                    String base = amount(evmBalance("https://base-rpc.publicnode.com", address), 18);
                    return eth + " ETH · " + bnb + " BNB · " + pol + " POL · " + base + " ETH on Base";
                }
                case TRX: {
                    JSONObject j = get("https://api.trongrid.io/v1/accounts/" + address);
                    JSONArray data = j.optJSONArray("data");
                    if (data == null || data.length() == 0) return "0 TRX";
                    JSONObject acct = data.getJSONObject(0);
                    String trx = amount(BigInteger.valueOf(acct.optLong("balance")), 6) + " TRX";
                    JSONArray trc20 = acct.optJSONArray("trc20");
                    if (trc20 != null) for (int i = 0; i < trc20.length(); i++) {
                        JSONObject t = trc20.getJSONObject(i);
                        if (t.has(USDT_TRON)) return trx + " · " + amount(new BigInteger(t.getString(USDT_TRON)), 6) + " USDT";
                    }
                    return trx;
                }
                case SOL: {
                    JSONObject r = post("https://api.mainnet-beta.solana.com",
                            "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"getBalance\",\"params\":[\"" + address + "\"]}");
                    return amount(BigInteger.valueOf(r.getJSONObject("result").optLong("value")), 9) + " SOL";
                }
            }
        } catch (Exception e) {
            return "Balance unavailable";
        }
        return "";
    }

    /** The Pi balance (native), or "Not active yet" for an account the Pi network has not created. */
    public static String pi(String address) {
        try {
            Request req = new Request.Builder().url("https://api.mainnet.minepi.com/accounts/" + address).build();
            try (Response r = HTTP.newCall(req).execute()) {
                if (r.code() == 404) return "Not active yet";
                JSONObject j = new JSONObject(r.body() == null ? "{}" : r.body().string());
                JSONArray b = j.optJSONArray("balances");
                if (b != null) for (int i = 0; i < b.length(); i++) {
                    JSONObject x = b.getJSONObject(i);
                    if ("native".equals(x.optString("asset_type"))) return trim(x.optString("balance", "0")) + " Pi";
                }
                return "0 Pi";
            }
        } catch (Exception e) {
            return "Balance unavailable";
        }
    }

    private static String mempool(String base, String address, String unit) throws Exception {
        JSONObject s = get(base + address).getJSONObject("chain_stats");
        long sats = s.optLong("funded_txo_sum") - s.optLong("spent_txo_sum");
        return amount(BigInteger.valueOf(sats), 8) + " " + unit;
    }

    private static BigInteger evmBalance(String rpc, String address) throws Exception {
        JSONObject r = post(rpc, "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"eth_getBalance\",\"params\":[\"" + address + "\",\"latest\"]}");
        String hex = r.optString("result", "0x0");
        return new BigInteger(hex.startsWith("0x") ? hex.substring(2).isEmpty() ? "0" : hex.substring(2) : hex, 16);
    }

    private static JSONObject get(String url) throws Exception {
        try (Response r = HTTP.newCall(new Request.Builder().url(url).build()).execute()) {
            if (!r.isSuccessful()) throw new Exception("HTTP " + r.code());
            return new JSONObject(r.body() == null ? "{}" : r.body().string());
        }
    }

    private static JSONObject post(String url, String body) throws Exception {
        try (Response r = HTTP.newCall(new Request.Builder().url(url).post(RequestBody.create(body, JSON)).build()).execute()) {
            if (!r.isSuccessful()) throw new Exception("HTTP " + r.code());
            return new JSONObject(r.body() == null ? "{}" : r.body().string());
        }
    }

    static String amount(BigInteger raw, int decimals) {
        return trim(new BigDecimal(raw).movePointLeft(decimals).toPlainString());
    }

    private static String trim(String s) {
        if (!s.contains(".")) return s;
        s = s.replaceAll("0+$", "");
        return s.endsWith(".") ? s.substring(0, s.length() - 1) : s;
    }
}
