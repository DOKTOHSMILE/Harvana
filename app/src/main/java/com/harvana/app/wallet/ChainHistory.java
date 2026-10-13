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
 * Pi, Bitcoin, Litecoin, Dogecoin, Ethereum, BNB, Polygon, Base, Tron and Solana each read that coin's public explorer.
 * Nothing here needs a key. Run off the main thread.
 */
public final class ChainHistory {
    private ChainHistory() {}

    private static final OkHttpClient HTTP = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).build();

    public static List<HashMap<String, Object>> load(Context c, String harvAddress) {
        List<HashMap<String, Object>> out = new ArrayList<>();
        if (c == null || harvAddress == null || harvAddress.isEmpty()) return out;
        Context app = c.getApplicationContext();
        List<java.util.concurrent.Callable<List<HashMap<String, Object>>>> jobs = new ArrayList<>();
        for (CoinCatalog.Coin coin : CoinCatalog.others()) {
            if (!ChainAssetsSection.shown(app, coin.key)) continue;
            jobs.add(() -> one(app, harvAddress, coin.key));
        }
        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(4);
        try {
            for (java.util.concurrent.Future<List<HashMap<String, Object>>> f : pool.invokeAll(jobs, 25, TimeUnit.SECONDS)) {
                if (f.isCancelled()) continue;
                try { out.addAll(f.get()); } catch (Exception ignored) { }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            pool.shutdownNow();
        }
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
            String addr = knownAddress != null && !knownAddress.isEmpty() ? knownAddress : CoinCatalog.savedAddress(app, harvAddress, key);
            if (addr == null || addr.isEmpty()) return out;
            switch (key) {
                case "PI":
                    for (PiClient.Payment p : PiClient.history(addr)) {
                        boolean in = "Received".equals(p.direction);
                        out.add(row("PI", Net.label("Pi Network"), in, in ? p.counterparty : addr, in ? addr : p.counterparty,
                                parse(p.amount), parseIso(p.when), p.hash, Net.piTxPage(p.hash)));
                    }
                    break;
                case "BTC": mempool(out, Net.btcApi(), "BTC", Net.label("Bitcoin"), addr); break;
                case "LTC": mempool(out, Net.ltcApi(), "LTC", Net.label("Litecoin"), addr); break;
                case "DOGE": doge(out, addr); break;
                case "ETH": evmList(out, "ETH", addr); break;
                case "BNB": evmList(out, "BNB", addr); break;
                case "POL": evmList(out, "POL", addr); break;
                case "BASE": evmList(out, "BASE", addr); break;
                case "TRX": tron(out, addr); break;
                case "SOL": solana(out, addr); break;
                default: {
                    CustomAssets.Asset a = CustomAssets.find(key);
                    if (a == null) break;
                    if (a.near() && !a.token()) out.addAll(NearProtocol.history(a, addr));
                    else if (a.near()) break;                                    // NEP-141 transfers: on the explorer
                    else if ("CHAIN".equals(a.kind) && !a.scout.isEmpty()) blockscout(out, a.scout, a.symbol, a.chainName, addr, a.decimals, a.txPage(""));
                    else if ("TOKEN".equals(a.kind) && !a.scout.isEmpty()) scoutTokens(out, a, addr);
                    break;
                }
            }
            remembered(c, key, out);
        } catch (Exception ignored) { }
        return out;
    }

    /**
     * The full record of one transaction, from that coin's own explorer. Null when the explorer has nothing.
     * Run off the main thread.
     */
    public static HashMap<String, Object> detail(String key, String hash) {
        if (key == null || hash == null || hash.isEmpty()) return null;
        try {
            switch (key) {
                case "BTC": return mempoolTx(Net.btcApi(), "BTC", Net.label("Bitcoin"), hash);
                case "LTC": return mempoolTx(Net.ltcApi(), "LTC", Net.label("Litecoin"), hash);
                case "DOGE": return dogeTx(hash);
                case "ETH":
                case "BNB":
                case "POL":
                case "BASE": return evmDetail(key, hash);
                case "TRX": return tronTx(hash);
                case "SOL": return solTx(hash);
                case "PI": return piTx(hash);
                default: return null;
            }
        } catch (Exception e) {
            return null;
        }
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
                        BigDecimal.valueOf(Math.abs(net)).movePointLeft(8).doubleValue(), ts, txid, txPage(ticker, txid));
                m.put("status", confirmed ? "confirmed" : "pending");
                m.put("harvFee", BigDecimal.valueOf(tx.optLong("fee")).movePointLeft(8).doubleValue());
                if (st != null) m.put("blockNumber", st.optLong("block_height"));
                out.add(m);
            }
        } catch (Exception ignored) { }
    }

    private static String txPage(String ticker, String hash) {
        return "LTC".equals(ticker) ? Net.ltcPage("tx", hash) : Net.btcPage("tx", hash);
    }

    private static void evmList(List<HashMap<String, Object>> out, String key, String address) {
        Net.Evm n = Net.evm(key);
        if (n.scout == null || n.scout.isEmpty()) return;
        blockscout(out, n.scout, key, n.name, address, 18, n.txPage);
    }

    private static HashMap<String, Object> evmDetail(String key, String hash) throws Exception {
        Net.Evm n = Net.evm(key);
        if (n.scout != null && !n.scout.isEmpty()) {
            try { return scoutTx(n.scout, key, n.name, hash, 18, n.txPage + hash); }
            catch (Exception ignored) { }
        }
        return evmRpc(n, key, hash);
    }

    private static HashMap<String, Object> evmRpc(Net.Evm n, String key, String hash) throws Exception {
        JSONObject tx = post(n.rpc, "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"eth_getTransactionByHash\",\"params\":[\"" + hash + "\"]}");
        JSONObject body = tx.optJSONObject("result");
        if (body == null) return null;
        JSONObject receipt = post(n.rpc, "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"eth_getTransactionReceipt\",\"params\":[\"" + hash + "\"]}").optJSONObject("result");
        String from = body.optString("from");
        String to = body.optString("to");
        long block = hexLong(body.optString("blockNumber"));
        long tip = hexLong(post(n.rpc, "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"eth_blockNumber\",\"params\":[]}").optString("result"));
        long time = System.currentTimeMillis();
        if (block > 0) {
            JSONObject blk = post(n.rpc, "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"eth_getBlockByNumber\",\"params\":[\"0x" + Long.toHexString(block) + "\",false]}").optJSONObject("result");
            if (blk != null) time = hexLong(blk.optString("timestamp")) * 1000L;
        }
        boolean ok = receipt == null || !"0x0".equals(receipt.optString("status"));
        long gasUsed = receipt == null ? hexLong(body.optString("gas")) : hexLong(receipt.optString("gasUsed"));
        HashMap<String, Object> m = row(key, n.name, false, from, to, shift(body.optString("value"), 18), time, hash, n.txPage + hash);
        m.put("status", body.optString("blockNumber").isEmpty() ? "pending" : (ok ? "confirmed" : "failed"));
        m.put("harvFee", shift(new java.math.BigInteger(Long.toString(hexLong(body.optString("gasPrice")))).multiply(java.math.BigInteger.valueOf(gasUsed)).toString(), 18));
        m.put("gasUsed", gasUsed);
        m.put("gasLimit", hexLong(body.optString("gas")));
        m.put("gasPrice", hexLong(body.optString("gasPrice")));
        m.put("blockNumber", block);
        m.put("nonce", hexLong(body.optString("nonce")));
        if (block > 0 && tip >= block) m.put("confirmations", tip - block + 1);
        stamp(m, body);
        return m;
    }

    private static long hexLong(String hex) {
        if (hex == null || hex.isEmpty()) return 0;
        String s = hex.startsWith("0x") || hex.startsWith("0X") ? hex.substring(2) : hex;
        if (s.isEmpty()) return 0;
        try { return new java.math.BigInteger(s, 16).longValue(); } catch (Exception e) { return 0; }
    }

    /** A payment this phone already broadcast, so it shows even before an explorer lists it. */
    public static void remember(android.content.Context c, String key, String hash, String from, String to, String amount) {
        if (c == null || key == null || hash == null || hash.isEmpty()) return;
        android.content.SharedPreferences p = c.getSharedPreferences("chain_sent", android.content.Context.MODE_PRIVATE);
        String prior = p.getString("rows", "");
        String line = key + "\t" + hash + "\t" + (from == null ? "" : from) + "\t" + (to == null ? "" : to) + "\t" + (amount == null ? "0" : amount) + "\t" + System.currentTimeMillis();
        StringBuilder next = new StringBuilder(line);
        int kept = 0;
        for (String old : prior.split("\n")) {
            if (old.isEmpty() || old.contains("\t" + hash + "\t")) continue;
            next.append('\n').append(old);
            if (++kept >= 30) break;
        }
        p.edit().putString("rows", next.toString()).apply();
    }

    private static void remembered(android.content.Context c, String key, List<HashMap<String, Object>> out) {
        if (c == null) return;
        String prior = c.getSharedPreferences("chain_sent", android.content.Context.MODE_PRIVATE).getString("rows", "");
        if (prior == null || prior.isEmpty()) return;
        for (String line : prior.split("\n")) {
            String[] p = line.split("\t", -1);
            if (p.length < 6 || !key.equals(p[0])) continue;
            boolean seen = false;
            for (HashMap<String, Object> row : out) if (p[1].equals(String.valueOf(row.get("txHash")))) seen = true;
            if (seen) continue;
            double amt = parse(p[4]);
            HashMap<String, Object> m = row(key, netName(key), false, p[2], p[3], amt, parseLong(p[5]), p[1], pageFor(key, p[1]));
            m.put("status", "pending");
            out.add(0, m);
        }
    }

    private static String netName(String key) {
        switch (key) {
            case "BTC": return Net.label("Bitcoin");
            case "LTC": return Net.label("Litecoin");
            case "DOGE": return Net.label("Dogecoin");
            case "TRX": return Net.TEST ? "Tron Nile" : "Tron";
            case "SOL": return Net.TEST ? "Solana devnet" : "Solana";
            case "PI": return Net.label("Pi Network");
            default: return Net.evm(key).name;
        }
    }

    private static String pageFor(String key, String hash) {
        switch (key) {
            case "BTC": return Net.btcPage("tx", hash);
            case "LTC": return Net.ltcPage("tx", hash);
            case "DOGE": return Net.dogeTxPage(hash);
            case "TRX": return Net.tronTxPage(hash);
            case "SOL": return Net.solTxPage(hash);
            case "PI": return Net.piTxPage(hash);
            default: return Net.evm(key).txPage + hash;
        }
    }

    private static long parseLong(String s) {
        try { return Long.parseLong(s); } catch (Exception e) { return System.currentTimeMillis(); }
    }
    private static HashMap<String, Object> mempoolTx(String base, String ticker, String network, String hash) throws Exception {
        JSONObject tx = get(base + "/api/tx/" + hash);
        long tip = 0;
        try { tip = Long.parseLong(body(base + "/api/blocks/tip/height").trim()); } catch (Exception ignored) { }
        JSONObject st = tx.optJSONObject("status");
        boolean confirmed = st != null && st.optBoolean("confirmed");
        long height = st == null ? 0 : st.optLong("block_height");
        long conf = confirmed && tip >= height && height > 0 ? tip - height + 1 : 0;
        String from = first(tx.optJSONArray("vin"), true);
        String to = first(tx.optJSONArray("vout"), false);
        double amount = 0;
        JSONArray vout = tx.optJSONArray("vout");
        if (vout != null && vout.length() > 0) amount = vout.getJSONObject(0).optLong("value") / 1e8;
        HashMap<String, Object> m = row(ticker, network, false, from, to, amount,
                st != null && st.has("block_time") ? st.optLong("block_time") * 1000L : System.currentTimeMillis(),
                hash, txPage(ticker, hash));
        m.put("status", confirmed ? "confirmed" : "pending");
        m.put("harvFee", tx.optLong("fee") / 1e8);
        m.put("blockNumber", height);
        m.put("confirmations", conf);
        stamp(m, tx);
        return m;
    }

    private static void stamp(HashMap<String, Object> m, JSONObject raw) {
        if (m == null || raw == null) return;
        try { m.put("raw", raw.toString(2)); }
        catch (Exception e) { m.put("raw", raw.toString()); }
    }

    private static String first(JSONArray arr, boolean inputs) {
        if (arr == null || arr.length() == 0) return "";
        JSONObject o = arr.optJSONObject(0);
        if (o == null) return "";
        if (inputs) o = o.optJSONObject("prevout");
        return o == null ? "" : o.optString("scriptpubkey_address");
    }

    private static void doge(List<HashMap<String, Object>> out, String address) {
        if (Net.TEST) { dogeTest(out, address); return; }
        try {
            JSONObject j = get("https://api.blockcypher.com/v1/doge/main/addrs/" + address + "?limit=20");
            JSONArray refs = j.optJSONArray("txrefs");
            if (refs == null) return;
            Map<String, long[]> net = new java.util.LinkedHashMap<>();
            Map<String, String> when = new HashMap<>();
            for (int i = 0; i < refs.length(); i++) {
                JSONObject r = refs.getJSONObject(i);
                String hash = r.optString("tx_hash");
                long[] n = net.get(hash);
                if (n == null) { n = new long[3]; net.put(hash, n); }
                long value = r.optLong("value");
                if (r.optInt("tx_input_n", -1) >= 0) n[0] -= value; else n[0] += value;
                n[1] = r.optLong("confirmations");
                n[2] = r.optLong("block_height");
                when.put(hash, r.optString("confirmed"));
            }
            int shown = 0;
            for (Map.Entry<String, long[]> e : net.entrySet()) {
                if (shown++ >= 20) break;
                boolean in = e.getValue()[0] >= 0;
                HashMap<String, Object> m = row("DOGE", "Dogecoin", in, in ? "" : address, in ? address : "",
                        Math.abs(e.getValue()[0]) / 1e8, parseIso(when.get(e.getKey())), e.getKey(),
                        "https://blockchair.com/dogecoin/transaction/" + e.getKey());
                m.put("status", e.getValue()[1] > 0 ? "confirmed" : "pending");
                m.put("confirmations", e.getValue()[1]);
                m.put("blockNumber", e.getValue()[2]);
                out.add(m);
            }
        } catch (Exception ignored) { }
    }

    private static void dogeTest(List<HashMap<String, Object>> out, String address) {
        try {
            JSONObject j = get(Net.dogeTxs(address));
            JSONArray txs = j.optJSONObject("data") == null ? null : j.getJSONObject("data").optJSONArray("transactions");
            if (txs == null) return;
            for (int i = 0; i < txs.length(); i++) {
                JSONObject tx = txs.getJSONObject(i);
                double change = parse(tx.optString("balance_change"));
                boolean in = change >= 0;
                String hash = tx.optString("hash");
                HashMap<String, Object> m = row("DOGE", Net.label("Dogecoin"), in, in ? "" : address, in ? address : "",
                        Math.abs(change), tx.optLong("time") * 1000L, hash, Net.dogeTxPage(hash));
                m.put("status", tx.optLong("block") > 0 ? "confirmed" : "pending");
                m.put("blockNumber", tx.optLong("block"));
                out.add(m);
            }
        } catch (Exception ignored) { }
    }

    private static HashMap<String, Object> dogeTx(String hash) throws Exception {
        if (Net.TEST) {
            JSONObject tx = get(Net.dogeTx(hash)).getJSONObject("data");
            String from = soAddr(tx.optJSONArray("inputs"));
            String to = soAddr(tx.optJSONArray("outputs"));
            double value = 0;
            JSONArray outs = tx.optJSONArray("outputs");
            if (outs != null && outs.length() > 0) value = parse(outs.getJSONObject(0).optString("value"));
            HashMap<String, Object> m = row("DOGE", Net.label("Dogecoin"), false, from, to, value,
                    tx.optLong("time") * 1000L, hash, Net.dogeTxPage(hash));
            m.put("status", tx.optLong("confirmations") > 0 ? "confirmed" : "pending");
            m.put("confirmations", tx.optLong("confirmations"));
            m.put("blockNumber", tx.optLong("block_height"));
            m.put("harvFee", parse(tx.optString("fee")));
            stamp(m, tx);
            return m;
        }
        JSONObject tx = get("https://api.blockcypher.com/v1/doge/main/txs/" + hash);
        String from = addrOf(tx.optJSONArray("inputs"));
        String to = addrOf(tx.optJSONArray("outputs"));
        long value = 0;
        JSONArray outs = tx.optJSONArray("outputs");
        if (outs != null && outs.length() > 0) value = outs.getJSONObject(0).optLong("value");
        HashMap<String, Object> m = row("DOGE", "Dogecoin", false, from, to, value / 1e8,
                parseIso(tx.optString("confirmed")), hash, "https://blockchair.com/dogecoin/transaction/" + hash);
        m.put("status", tx.optLong("confirmations") > 0 ? "confirmed" : "pending");
        m.put("confirmations", tx.optLong("confirmations"));
        m.put("blockNumber", tx.optLong("block_height"));
        m.put("harvFee", tx.optLong("fees") / 1e8);
        stamp(m, tx);
        return m;
    }

    private static String soAddr(JSONArray arr) {
        if (arr == null || arr.length() == 0) return "";
        JSONObject o = arr.optJSONObject(0);
        return o == null ? "" : o.optString("address");
    }

    private static String addrOf(JSONArray arr) {
        if (arr == null || arr.length() == 0) return "";
        JSONObject o = arr.optJSONObject(0);
        if (o == null) return "";
        JSONArray a = o.optJSONArray("addresses");
        return a == null || a.length() == 0 ? "" : a.optString(0);
    }

    /** An added token's transfers for this address, from the chain's Blockscout. */
    private static void scoutTokens(List<HashMap<String, Object>> out, CustomAssets.Asset a, String address) {
        try {
            JSONObject j = get(a.scout + "/api/v2/addresses/" + address + "/token-transfers?token=" + a.contract);
            JSONArray items = j.optJSONArray("items");
            for (int i = 0; items != null && i < Math.min(20, items.length()); i++) {
                JSONObject t = items.getJSONObject(i);
                String from = who(t.optJSONObject("from")), to = who(t.optJSONObject("to"));
                JSONObject total = t.optJSONObject("total");
                String hash = t.optString("transaction_hash", t.optString("tx_hash"));
                out.add(row(a.symbol, a.chainName, address.equalsIgnoreCase(to), from, to,
                        shift(total == null ? "0" : total.optString("value", "0"), a.decimals), iso(t.optString("timestamp")), hash, a.txPage(hash)));
            }
        } catch (Exception ignored) { }
    }

    private static void blockscout(List<HashMap<String, Object>> out, String host, String ticker, String network, String address, int decimals, String txBase) {
        try {
            JSONObject j = get(host + "/api/v2/addresses/" + address + "/transactions");
            JSONArray items = j.optJSONArray("items");
            if (items == null) return;
            for (int i = 0; i < Math.min(20, items.length()); i++) out.add(scout(items.getJSONObject(i), ticker, network, address, decimals, txBase));
        } catch (Exception ignored) { }
    }

    private static HashMap<String, Object> scoutTx(String host, String ticker, String network, String hash, int decimals, String explorer) throws Exception {
        JSONObject tx = get(host + "/api/v2/transactions/" + hash);
        HashMap<String, Object> m = scout(tx, ticker, network, "", decimals, explorer);
        stamp(m, tx);
        return m;
    }

    private static HashMap<String, Object> scout(JSONObject tx, String ticker, String network, String mine, int decimals, String txBase) {
        String from = who(tx.optJSONObject("from"));
        String to = who(tx.optJSONObject("to"));
        boolean in = mine != null && mine.equalsIgnoreCase(to);
        String status = tx.optString("status", "ok");
        boolean ok = "ok".equalsIgnoreCase(status) || "success".equalsIgnoreCase(status);
        String hash = tx.optString("hash");
        String link = txBase.endsWith(hash) ? txBase : txBase + hash;
        HashMap<String, Object> m = row(ticker, network, in, from, to, shift(tx.optString("value"), decimals), iso(tx.optString("timestamp")), hash, link);
        m.put("status", tx.optInt("confirmations", ok ? 1 : 0) == 0 && !ok ? "failed" : (ok ? "confirmed" : "pending"));
        if ("error".equalsIgnoreCase(status)) m.put("status", "failed");
        JSONObject fee = tx.optJSONObject("fee");
        m.put("harvFee", shift(fee == null ? tx.optString("fee") : fee.optString("value"), decimals));
        m.put("blockNumber", tx.optLong("block_number"));
        m.put("nonce", tx.optLong("nonce"));
        m.put("confirmations", tx.optLong("confirmations"));
        return m;
    }

    private static String who(JSONObject o) { return o == null ? "" : o.optString("hash"); }

    private static void tron(List<HashMap<String, Object>> out, String address) {
        try {
            JSONObject j = get(Net.tronScan() + "/api/transaction?sort=-timestamp&limit=20&start=0&address=" + address);
            JSONArray data = j.optJSONArray("data");
            if (data == null) return;
            for (int i = 0; i < data.length(); i++) {
                JSONObject tx = data.getJSONObject(i);
                if (tx.optInt("contractType", 1) != 1 && tx.has("contractType") && tx.optInt("contractType") != 1) continue;
                String from = tx.optString("ownerAddress");
                String to = tx.optString("toAddress");
                boolean in = address.equals(to);
                String hash = tx.optString("hash");
                HashMap<String, Object> m = row("TRX", "Tron", in, from, to, shift(tx.optString("amount", "0"), 6),
                        tx.optLong("timestamp"), hash, Net.tronTxPage(hash));
                boolean ok = "SUCCESS".equalsIgnoreCase(tx.optString("contractRet")) || tx.optBoolean("confirmed");
                m.put("status", ok ? "confirmed" : "pending");
                m.put("blockNumber", tx.optLong("block"));
                m.put("harvFee", shift(String.valueOf(tx.optLong("fee")), 6));
                out.add(m);
            }
        } catch (Exception ignored) { }
    }

    private static HashMap<String, Object> tronTx(String hash) throws Exception {
        JSONObject tx = get(Net.tronScan() + "/api/transaction-info?hash=" + hash);
        String from = tx.optString("ownerAddress");
        String to = tx.optString("toAddress");
        HashMap<String, Object> m = row("TRX", "Tron", false, from, to, shift(tx.optString("contractData", ""), 6) == 0
                        ? shift(String.valueOf(tx.optLong("amount")), 6) : shift(tx.optString("amount", "0"), 6),
                tx.optLong("timestamp"), hash, Net.tronTxPage(hash));
        JSONObject data = tx.optJSONObject("contractData");
        if (data != null) {
            if (from.isEmpty()) m.put("senderAddress", data.optString("owner_address"));
            if (to.isEmpty()) m.put("recipientAddress", data.optString("to_address"));
            if (data.has("amount")) m.put("value", shift(String.valueOf(data.optLong("amount")), 6));
        }
        boolean ok = tx.optBoolean("confirmed") || "SUCCESS".equalsIgnoreCase(tx.optString("contractRet"));
        m.put("status", ok ? "confirmed" : "pending");
        m.put("blockNumber", tx.optLong("block"));
        m.put("harvFee", shift(String.valueOf(tx.optLong("cost", tx.optLong("fee"))), 6));
        m.put("confirmations", tx.optLong("confirmations"));
        stamp(m, tx);
        return m;
    }

    private static void solana(List<HashMap<String, Object>> out, String address) {
        try {
            JSONObject sigs = post(Net.solRpc(),
                    "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"getSignaturesForAddress\",\"params\":[\"" + address + "\",{\"limit\":8}]}");
            JSONArray arr = sigs.optJSONArray("result");
            if (arr == null) return;
            for (int i = 0; i < arr.length(); i++) {
                String sig = arr.getJSONObject(i).optString("signature");
                HashMap<String, Object> m = solParsed(sig, address);
                if (m != null) out.add(m);
            }
        } catch (Exception ignored) { }
    }

    private static HashMap<String, Object> solTx(String hash) {
        return solParsed(hash, "");
    }

    private static HashMap<String, Object> solParsed(String sig, String mine) {
        try {
            JSONObject r = post(Net.solRpc(),
                    "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"getTransaction\",\"params\":[\"" + sig + "\",{\"encoding\":\"jsonParsed\",\"maxSupportedTransactionVersion\":0}]}");
            JSONObject tx = r.optJSONObject("result");
            if (tx == null) return null;
            JSONObject meta = tx.optJSONObject("meta");
            JSONObject msg = tx.getJSONObject("transaction").getJSONObject("message");
            JSONArray keys = msg.optJSONArray("accountKeys");
            JSONArray pre = meta == null ? null : meta.optJSONArray("preBalances");
            JSONArray post = meta == null ? null : meta.optJSONArray("postBalances");
            long fee = meta == null ? 0 : meta.optLong("fee");
            int me = indexOf(keys, mine);
            long delta = 0;
            String other = "";
            if (pre != null && post != null) {
                if (me >= 0) delta = post.optLong(me) - pre.optLong(me);
                for (int i = 0; i < post.length(); i++) {
                    if (i == me) continue;
                    long d = post.optLong(i) - pre.optLong(i);
                    if (d != 0 && other.isEmpty()) other = pubkey(keys, i);
                }
            }
            boolean in = delta > 0;
            long moved = in ? delta : Math.max(0, -delta - (me == 0 ? fee : 0));
            String from = in ? other : (me >= 0 ? pubkey(keys, me) : pubkey(keys, 0));
            String to = in ? (me >= 0 ? pubkey(keys, me) : mine) : other;
            HashMap<String, Object> m = row("SOL", "Solana", in, from, to, moved / 1e9,
                    tx.optLong("blockTime") * 1000L, sig, Net.solTxPage(sig));
            boolean failed = meta != null && meta.has("err") && !meta.isNull("err");
            m.put("status", failed ? "failed" : (tx.optLong("blockTime") > 0 ? "confirmed" : "pending"));
            m.put("harvFee", fee / 1e9);
            m.put("blockNumber", tx.optLong("slot"));
            if (mine == null || mine.isEmpty()) stamp(m, tx);
            return m;
        } catch (Exception e) {
            return null;
        }
    }

    private static int indexOf(JSONArray keys, String mine) {
        if (keys == null || mine == null || mine.isEmpty()) return -1;
        for (int i = 0; i < keys.length(); i++) if (mine.equals(pubkey(keys, i))) return i;
        return -1;
    }

    private static String pubkey(JSONArray keys, int i) {
        if (keys == null || i < 0 || i >= keys.length()) return "";
        Object o = keys.opt(i);
        if (o instanceof String) return (String) o;
        if (o instanceof JSONObject) return ((JSONObject) o).optString("pubkey");
        return "";
    }

    private static HashMap<String, Object> piTx(String hash) throws Exception {
        JSONObject tx = get(Net.piHorizon() + "/transactions/" + hash);
        boolean ok = tx.optBoolean("successful", true);
        HashMap<String, Object> m = row("PI", "Pi Network", false, tx.optString("source_account"), "",
                0, iso(tx.optString("created_at")), hash, Net.piTxPage(hash));
        m.put("status", ok ? "confirmed" : "failed");
        m.put("harvFee", shift(tx.optString("fee_charged", "0"), 7));
        m.put("blockNumber", tx.optLong("ledger"));
        m.put("memo", tx.optString("memo"));
        stamp(m, tx);
        return m;
    }

    private static JSONObject get(String url) throws Exception {
        try (Response r = HTTP.newCall(new Request.Builder().url(url).header("User-Agent", "Harvana").build()).execute()) {
            if (!r.isSuccessful() || r.body() == null) throw new Exception("HTTP " + r.code());
            return new JSONObject(r.body().string());
        }
    }

    private static String body(String url) throws Exception {
        try (Response r = HTTP.newCall(new Request.Builder().url(url).header("User-Agent", "Harvana").build()).execute()) {
            if (!r.isSuccessful() || r.body() == null) throw new Exception("HTTP " + r.code());
            return r.body().string();
        }
    }

    private static JSONObject post(String url, String json) throws Exception {
        try (Response r = HTTP.newCall(new Request.Builder().url(url)
                .post(okhttp3.RequestBody.create(json, okhttp3.MediaType.parse("application/json"))).build()).execute()) {
            if (!r.isSuccessful() || r.body() == null) throw new Exception("HTTP " + r.code());
            return new JSONObject(r.body().string());
        }
    }

    private static double shift(String raw, int decimals) {
        if (raw == null || raw.isEmpty()) return 0;
        try { return new BigDecimal(raw).movePointLeft(decimals).doubleValue(); }
        catch (Exception e) { return 0; }
    }

    private static long iso(String s) {
        if (s == null || s.isEmpty()) return System.currentTimeMillis();
        return parseIso(s.replace('T', ' ').replace("Z", ""));
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
            if (s == null || s.isEmpty()) return System.currentTimeMillis();
            String t = s.replace('T', ' ').replace("Z", "").trim();
            int dot = t.indexOf('.');
            if (dot > 0) t = t.substring(0, dot);
            java.text.SimpleDateFormat f = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US);
            f.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            return f.parse(s).getTime();
        } catch (Exception e) { return System.currentTimeMillis(); }
    }
}
