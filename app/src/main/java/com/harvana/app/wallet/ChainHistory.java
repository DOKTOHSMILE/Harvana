package com.harvana.app.wallet;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Recent transactions of the other coins the user turned on (Add coins on the Assets tab), shaped like the HARV history rows
 * so the wallet's Activity list can show them all together. Pi, Bitcoin and Litecoin read public explorers that need no key.
 * Run off the main thread.
 */
public final class ChainHistory {
    private ChainHistory() {}

    private static final OkHttpClient HTTP = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).build();

    public static List<HashMap<String, Object>> load(Context c, String harvAddress) {
        List<HashMap<String, Object>> out = new ArrayList<>();
        if (c == null || harvAddress == null || harvAddress.isEmpty()) return out;
        Context app = c.getApplicationContext();
        if (ChainAssetsSection.shown(app, "PI")) out.addAll(one(app, harvAddress, "PI"));
        if (ChainAssetsSection.shown(app, ChainKeys.Chain.BTC.name())) out.addAll(one(app, harvAddress, "BTC"));
        if (ChainAssetsSection.shown(app, ChainKeys.Chain.LTC.name())) out.addAll(one(app, harvAddress, "LTC"));
        return out;
    }

    /** Recent transactions for one coin, even if that coin is hidden on the Assets tab. */
    public static List<HashMap<String, Object>> one(Context c, String harvAddress, String key) {
        return one(c, harvAddress, key, null);
    }

    /** {@code knownAddress} is the address already on screen, used when the wallet store has not caught up. */
    public static List<HashMap<String, Object>> one(Context c, String harvAddress, String key, String knownAddress) {
        List<HashMap<String, Object>> out = new ArrayList<>();
        if (c == null || key == null) return out;
        if ((harvAddress == null || harvAddress.isEmpty()) && (knownAddress == null || knownAddress.isEmpty())) return out;
        Context app = c.getApplicationContext();
        try {
            if ("PI".equals(key)) {
                String pi = knownAddress != null && !knownAddress.isEmpty() ? knownAddress : PiWalletStore.get(app, harvAddress);
                if (pi == null || pi.isEmpty()) return out;
                for (PiClient.Payment p : PiClient.history(pi)) {
                    boolean in = "Received".equals(p.direction);
                    out.add(row("PI", "Pi Network", in, in ? p.counterparty : pi, in ? pi : p.counterparty,
                            parse(p.amount), parseIso(p.when), p.hash, "https://blockexplorer.minepi.com/mainnet/transactions/" + p.hash));
                }
                return out;
            }
            Map<ChainKeys.Chain, String> addrs = harvAddress == null || harvAddress.isEmpty()
                    ? new java.util.HashMap<>() : ChainWalletStore.ensure(app, harvAddress);
            String addr = "BTC".equals(key) ? known(knownAddress, addrs, ChainKeys.Chain.BTC)
                    : "LTC".equals(key) ? known(knownAddress, addrs, ChainKeys.Chain.LTC) : null;
            if (addr == null || addr.isEmpty()) return out;
            if ("BTC".equals(key)) mempool(out, "https://mempool.space", "BTC", "Bitcoin", addr);
            else if ("LTC".equals(key)) mempool(out, "https://litecoinspace.org", "LTC", "Litecoin", addr);
        } catch (Exception ignored) { }
        return out;
    }

    private static String known(String knownAddress, Map<ChainKeys.Chain, String> addrs, ChainKeys.Chain chain) {
        if (knownAddress != null && !knownAddress.isEmpty()) return knownAddress;
        return addrs.get(chain);
    }

    /** Esplora-style explorers: the net change to this address in each transaction decides sent or received. */
    private static void mempool(List<HashMap<String, Object>> out, String base, String ticker, String network, String address) {
        try (Response r = HTTP.newCall(new Request.Builder().url(base + "/api/address/" + address + "/txs").build()).execute()) {
            if (!r.isSuccessful() || r.body() == null) return;
            JSONArray txs = new JSONArray(r.body().string());
            for (int i = 0; i < Math.min(25, txs.length()); i++) {
                JSONObject tx = txs.getJSONObject(i);
                long inSum = 0, outSum = 0;
                String other = "";
                JSONArray vin = tx.optJSONArray("vin"), vout = tx.optJSONArray("vout");
                if (vin != null) for (int k = 0; k < vin.length(); k++) {
                    JSONObject prev = vin.getJSONObject(k).optJSONObject("prevout");
                    if (prev == null) continue;
                    if (address.equals(prev.optString("scriptpubkey_address"))) outSum += prev.optLong("value");
                    else if (other.isEmpty()) other = prev.optString("scriptpubkey_address");
                }
                String firstOut = "";
                if (vout != null) for (int k = 0; k < vout.length(); k++) {
                    JSONObject o = vout.getJSONObject(k);
                    if (address.equals(o.optString("scriptpubkey_address"))) inSum += o.optLong("value");
                    else if (firstOut.isEmpty()) firstOut = o.optString("scriptpubkey_address");
                }
                long net = inSum - outSum;
                boolean in = net >= 0;
                JSONObject st = tx.optJSONObject("status");
                boolean confirmed = st != null && st.optBoolean("confirmed");
                long ts = st != null && st.has("block_time") ? st.optLong("block_time") * 1000L : System.currentTimeMillis();
                String txid = tx.optString("txid");
                HashMap<String, Object> m = row(ticker, network, in, in ? other : address, in ? address : firstOut,
                        BigDecimal.valueOf(Math.abs(net)).movePointLeft(8).doubleValue(), ts, txid, base + "/tx/" + txid);
                m.put("status", confirmed ? "confirmed" : "pending");
                m.put("harvFee", BigDecimal.valueOf(tx.optLong("fee")).movePointLeft(8).doubleValue());
                if (st != null) m.put("blockNumber", st.optLong("block_height"));
                out.add(m);
            }
        } catch (Exception ignored) { }
    }

    private static HashMap<String, Object> row(String ticker, String network, boolean in, String from, String to,
                                               double amount, long ts, String hash, String explorer) {
        HashMap<String, Object> m = new HashMap<>();
        m.put("tokenSymbol", ticker);
        m.put("chain", network);
        m.put("incoming", in);
        m.put("senderAddress", from == null ? "" : from);
        m.put("recipientAddress", to == null ? "" : to);
        m.put("value", amount);
        m.put("timestamp", ts);
        m.put("txHash", hash == null ? "" : hash);
        m.put("status", "confirmed");
        m.put("memo", "");
        m.put("explorerUrl", explorer);
        return m;
    }

    private static double parse(String s) {
        try { return Double.parseDouble(s); } catch (Exception e) { return 0; }
    }

    private static long parseIso(String s) {
        try {
            java.text.SimpleDateFormat f = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US);
            f.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            return f.parse(s).getTime();
        } catch (Exception e) { return System.currentTimeMillis(); }
    }
}
