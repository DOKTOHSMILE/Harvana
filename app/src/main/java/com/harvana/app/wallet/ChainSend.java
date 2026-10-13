package com.harvana.app.wallet;

import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Sends a coin from the recovery phrase. The key never leaves the phone: this builds the transaction, signs it, and hands
 * the signed bytes to that coin's own public network. HARV and Pi have their own senders.
 */
public final class ChainSend {
    private ChainSend() {}

    private static final OkHttpClient HTTP = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build();
    private static final MediaType JSON = MediaType.parse("application/json");

    /** Signs and broadcasts. Returns the transaction id the network accepted. */
    public static String send(String key, String mnemonic, String from, String to, String amount) throws Exception {
        if (mnemonic == null || mnemonic.trim().isEmpty()) throw new IllegalArgumentException("Couldn't load the recovery phrase on this phone.");
        if (to == null || from == null || to.trim().equalsIgnoreCase(from.trim())) throw new IllegalArgumentException("That is your own address.");
        to = to.trim();
        switch (key) {
            case "ETH": { Net.Evm n = Net.evm("ETH"); return evm(mnemonic, from, to, amount, n.rpc, n.chainId, 18); }
            case "BNB": { Net.Evm n = Net.evm("BNB"); return evm(mnemonic, from, to, amount, n.rpc, n.chainId, 18); }
            case "POL": { Net.Evm n = Net.evm("POL"); return evm(mnemonic, from, to, amount, n.rpc, n.chainId, 18); }
            case "BASE": { Net.Evm n = Net.evm("BASE"); return evm(mnemonic, from, to, amount, n.rpc, n.chainId, 18); }
            case "BTC": return utxo(mnemonic, from, to, amount, ChainKeys.Chain.BTC, Net.btcApi(from), true, false);   // legacy P2PKH coins
            case "LTC": return utxo(mnemonic, from, to, amount, ChainKeys.Chain.LTC, Net.ltcApi(), true, true);
            case "DOGE": return utxo(mnemonic, from, to, amount, ChainKeys.Chain.DOGE, Net.TEST ? "" : "https://api.blockcypher.com/v1/doge/main", false, false);
            case "TRX": return tron(mnemonic, from, to, amount);
            case "SOL": return sol(mnemonic, from, to, amount);
            default: {
                CustomAssets.Asset a = CustomAssets.find(key);
                if (a == null) throw new IllegalArgumentException("Sending " + key + " from Harvana is not switched on.");
                if (a.near()) return a.token() ? NearProtocol.sendToken(a, mnemonic, from, to, amount) : NearProtocol.send(a, mnemonic, from, to, amount);
                if (Trc20.FAMILY.equals(a.family)) return Trc20.send(a, mnemonic, from, to, amount);
                if (SplToken.FAMILY.equals(a.family)) {
                    byte[] seed = ChainKeys.privateKey(mnemonic, ChainKeys.Chain.SOL);
                    try { return SplToken.send(a, seed, from, to, amount); }
                    finally { java.util.Arrays.fill(seed, (byte) 0); }
                }
                if ("TOKEN".equals(a.kind)) return erc20(mnemonic, from, to, amount, a);
                return evm(mnemonic, from, to, amount, a.rpc, a.chainId, a.decimals);
            }
        }
    }

    private static String evm(String mnemonic, String from, String to, String amount, String rpc, long chainId, int decimals) throws Exception {
        byte[] priv = ChainKeys.privateKey(mnemonic, ChainKeys.Chain.ETH);
        String mine = ChainKeys.address(mnemonic, ChainKeys.Chain.ETH);
        if (!mine.equalsIgnoreCase(from)) throw new IllegalArgumentException("This phone's key does not match the address on screen. Open the coin again.");
        if (!to.matches("(?i)0x[0-9a-f]{40}")) throw new IllegalArgumentException("Enter an address that starts with 0x.");
        BigInteger value = units(amount, decimals);
        BigInteger gasPrice = qty(rpc(rpc, "eth_gasPrice")).multiply(BigInteger.valueOf(110)).divide(BigInteger.valueOf(100));
        if (gasPrice.signum() == 0) gasPrice = BigInteger.valueOf(1_000_000_000L);
        long gas = 21000L;
        BigInteger balance = qty(rpc(rpc, "eth_getBalance", from, "latest"));
        BigInteger fee = gasPrice.multiply(BigInteger.valueOf(gas));
        if (balance.compareTo(value.add(fee)) < 0) throw new IllegalArgumentException("Not enough left to pay the network fee.");
        long nonce = qty(rpc(rpc, "eth_getTransactionCount", from, "pending")).longValue();
        String raw = EvmTx.raw(priv, nonce, gasPrice, gas, EvmTx.unhex(to), value, chainId);
        return rpc(rpc, "eth_sendRawTransaction", "0x" + raw);
    }

    /** An added ERC-20 token: transfer(to, amount) on its contract; the fee is paid in the chain's own coin. */
    private static String erc20(String mnemonic, String from, String to, String amount, CustomAssets.Asset a) throws Exception {
        byte[] priv = ChainKeys.privateKey(mnemonic, ChainKeys.Chain.ETH);
        try {
            String mine = ChainKeys.address(mnemonic, ChainKeys.Chain.ETH);
            if (!mine.equalsIgnoreCase(from)) throw new IllegalArgumentException("This phone's key does not match the address on screen. Open the coin again.");
            if (!to.matches("(?i)0x[0-9a-f]{40}")) throw new IllegalArgumentException("Enter an address that starts with 0x.");
            BigInteger value = units(amount, a.decimals);
            String rpc = a.rpc;
            String held = rpcRaw(rpc, "eth_call", "[{\"to\":\"" + a.contract + "\",\"data\":\"0x70a08231" + pad(from) + "\"},\"latest\"]");
            if (qty(held).compareTo(value) < 0) throw new IllegalArgumentException("Not enough " + a.symbol + ".");
            String data = "0xa9059cbb" + pad(to) + String.format("%64s", value.toString(16)).replace(' ', '0');
            BigInteger gasPrice = qty(rpc(rpc, "eth_gasPrice")).multiply(BigInteger.valueOf(110)).divide(BigInteger.valueOf(100));
            if (gasPrice.signum() == 0) gasPrice = BigInteger.valueOf(1_000_000_000L);
            long gas = qty(rpcRaw(rpc, "eth_estimateGas", "[{\"from\":\"" + from + "\",\"to\":\"" + a.contract + "\",\"data\":\"" + data + "\"}]"))
                    .multiply(BigInteger.valueOf(12)).divide(BigInteger.TEN).longValue();
            BigInteger fee = gasPrice.multiply(BigInteger.valueOf(gas));
            if (qty(rpc(rpc, "eth_getBalance", from, "latest")).compareTo(fee) < 0)
                throw new IllegalArgumentException("Not enough " + a.feeSymbol() + " to pay the network fee.");
            long nonce = qty(rpc(rpc, "eth_getTransactionCount", from, "pending")).longValue();
            String raw = EvmTx.rawWithData(priv, nonce, gasPrice, gas, EvmTx.unhex(a.contract), BigInteger.ZERO, EvmTx.unhex(data), a.chainId);
            return rpc(rpc, "eth_sendRawTransaction", "0x" + raw);
        } finally {
            java.util.Arrays.fill(priv, (byte) 0);
        }
    }

    /** JSON-RPC with the params already written as a JSON array (for object params such as eth_call's). */
    private static String rpcRaw(String url, String method, String paramsJson) throws Exception {
        JSONObject r = post(url, "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"" + method + "\",\"params\":" + paramsJson + "}");
        if (r.has("error")) {
            JSONObject e = r.optJSONObject("error");
            throw new IllegalArgumentException(e == null ? "The network rejected the payment." : e.optString("message", "The network rejected the payment."));
        }
        return r.getString("result");
    }

    private static String pad(String address) {
        String a = address.startsWith("0x") || address.startsWith("0X") ? address.substring(2) : address;
        return String.format("%64s", a.toLowerCase(java.util.Locale.US)).replace(' ', '0');
    }

    private static String rpc(String url, String method, String... params) throws Exception {
        StringBuilder p = new StringBuilder();
        for (String s : params) {
            if (p.length() > 0) p.append(',');
            p.append('"').append(s).append('"');
        }
        JSONObject r = post(url, "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"" + method + "\",\"params\":[" + p + "]}");
        if (r.has("error")) {
            JSONObject e = r.optJSONObject("error");
            throw new IllegalArgumentException(e == null ? "The network rejected the payment." : e.optString("message", "The network rejected the payment."));
        }
        return r.getString("result");
    }

    /**
     * {@code esplora}: the coins are read from and sent through an Esplora API (mempool.space style), else Dogecoin's.
     * {@code witness}: this wallet's coins are SegWit (P2WPKH, Litecoin); else legacy P2PKH (Bitcoin, Dogecoin).
     */
    private static String utxo(String mnemonic, String from, String to, String amount, ChainKeys.Chain chain, String api, boolean esplora, boolean witness) throws Exception {
        byte[] priv = ChainKeys.privateKey(mnemonic, chain);
        String mine = ChainKeys.address(mnemonic, chain);
        if (!mine.equals(from)) throw new IllegalArgumentException("This phone's key does not match the address on screen. Open the coin again.");
        long sats = exact(units(amount, 8));
        if (sats < ("DOGE".equals(chain.ticker) ? 100000 : 546)) throw new IllegalArgumentException("That amount is too small for " + chain.ticker + ".");
        byte[] payScript = scriptFor(to, chain);
        byte[] changeScript = scriptFor(mine, chain);
        List<BtcTx.In> ins = new ArrayList<>();
        long sum = 0;
        long fee = 0;
        long change = 0;
        JSONArray utxos = esplora ? getArray(api + "/api/address/" + from + "/utxo") : dogeUtxos(from);
        int rate = esplora ? feeRate(api) : 100;
        int dust = "DOGE".equals(chain.ticker) ? 100000 : 546;
        for (int i = 0; i < utxos.length() && ins.size() < 50; i++) {
            JSONObject u = utxos.getJSONObject(i);
            if (esplora && u.has("status") && !u.getJSONObject("status").optBoolean("confirmed", true)) continue;
            long value = u.optLong("value");
            if (value <= 0) continue;
            String idKey = esplora ? "txid" : "tx_hash";
            String voutKey = esplora ? "vout" : "tx_output_n";
            if (!u.has(idKey) || !u.has(voutKey)) continue;
            byte[] txid = BtcTx.reverse(EvmTx.unhex(u.getString(idKey)));
            ins.add(new BtcTx.In(txid, u.getInt(voutKey), value));
            sum += value;
            fee = (long) rate * vsize(ins.size(), 2, witness);
            if (sum >= sats + fee) {
                change = sum - sats - fee;
                if (change < dust) {
                    fee = (long) rate * vsize(ins.size(), 1, witness);
                    change = sum - sats - fee;
                    if (change < 0) continue;
                    if (change < dust) { fee += change; change = 0; }
                }
                break;
            }
        }
        if (ins.isEmpty() || sum < sats + fee) throw new IllegalArgumentException("Not enough confirmed coins to pay that and the network fee.");
        List<BtcTx.Out> outs = new ArrayList<>();
        outs.add(new BtcTx.Out(sats, payScript));
        if (change > 0) outs.add(new BtcTx.Out(change, changeScript));
        String raw = witness ? BtcTx.p2wpkh(priv, ins, outs) : BtcTx.p2pkh(priv, ins, outs);
        int size = witness ? BtcTx.vbytes(raw) : raw.length() / 2;
        long need = (long) rate * Math.max(size, 1);
        if (need > fee) {
            long gap = need - fee;
            if (change >= gap) {
                change -= gap;
                if (change < dust) { fee += change; change = 0; }
            } else if (sum >= sats + need) {
                change = 0;
            } else throw new IllegalArgumentException("Not enough confirmed coins to pay that and the network fee.");
            outs.clear();
            outs.add(new BtcTx.Out(sats, payScript));
            if (change > 0) outs.add(new BtcTx.Out(change, changeScript));
            raw = witness ? BtcTx.p2wpkh(priv, ins, outs) : BtcTx.p2pkh(priv, ins, outs);
        }
        if (esplora) return postText(api + "/api/tx", raw);
        if (Net.TEST) {
            JSONObject pushed = post(Net.dogeRpc(), "{\"jsonrpc\":\"1.0\",\"id\":1,\"method\":\"sendrawtransaction\",\"params\":[\"" + raw + "\"]}");
            if (pushed.has("error")) {
                JSONObject e = pushed.optJSONObject("error");
                throw new IllegalArgumentException(e == null ? "Dogecoin rejected the payment." : e.optString("message", "Dogecoin rejected the payment."));
            }
            return pushed.getString("result");
        }
        JSONObject pushed = post(api + "/txs/push", "{\"tx\":\"" + raw + "\"}");
        JSONObject tx = pushed.optJSONObject("tx");
        String hash = tx == null ? pushed.optString("hash", "") : tx.optString("hash", "");
        if (hash.isEmpty()) throw new IllegalArgumentException(pushed.optString("error", "Dogecoin rejected the payment."));
        return hash;
    }

    private static JSONArray dogeUtxos(String address) throws Exception {
        if (!Net.TEST) {
            JSONObject j = get("https://api.blockcypher.com/v1/doge/main/addrs/" + address + "?unspentOnly=true&limit=50");
            JSONArray refs = j.optJSONArray("txrefs");
            return refs == null ? new JSONArray() : refs;
        }
        JSONObject j = get(Net.dogeUtxo(address));
        JSONArray outputs = j.optJSONObject("data") == null ? null : j.getJSONObject("data").optJSONArray("outputs");
        JSONArray refs = new JSONArray();
        if (outputs == null) return refs;
        for (int i = 0; i < outputs.length(); i++) {
            JSONObject o = outputs.getJSONObject(i);
            JSONObject r = new JSONObject();
            r.put("tx_hash", o.optString("hash"));
            r.put("tx_output_n", o.optInt("index"));
            r.put("value", new java.math.BigDecimal(o.optString("value", "0")).movePointRight(8).longValue());
            refs.put(r);
        }
        return refs;
    }

    private static int feeRate(String api) {
        try {
            JSONObject j = get(api + "/api/v1/fees/recommended");
            int rate = j.optInt("halfHourFee", 0);
            return rate > 0 ? rate : 2;
        } catch (Exception e) {
            return 5;
        }
    }

    private static int vsize(int inputs, int outputs, boolean segwit) {
        if (segwit) return 11 + 68 * inputs + 31 * outputs;
        return 10 + 148 * inputs + 34 * outputs;
    }

    private static String tron(String mnemonic, String from, String to, String amount) throws Exception {
        byte[] priv = ChainKeys.privateKey(mnemonic, ChainKeys.Chain.TRX);
        String mine = ChainKeys.address(mnemonic, ChainKeys.Chain.TRX);
        if (!mine.equals(from)) throw new IllegalArgumentException("This phone's key does not match the address on screen. Open the coin again.");
        if (to.length() < 30 || to.charAt(0) != 'T') throw new IllegalArgumentException("Enter a Tron address. It starts with T.");
        long sun = exact(units(amount, 6));
        JSONObject tx = post(Net.tronApi() + "/wallet/createtransaction",
                "{\"owner_address\":\"" + from + "\",\"to_address\":\"" + to + "\",\"amount\":" + sun + ",\"visible\":true}");
        if (!tx.has("txID")) throw new IllegalArgumentException(tx.optString("Error", "Tron could not build the payment."));
        return tronBroadcast(priv, tx);
    }

    /** Signs a Tron transaction the API built (its txID) and broadcasts it; returns the txID. Also used for TRC-20. */
    static String tronBroadcast(byte[] priv, JSONObject tx) throws Exception {
        byte[] hash = EvmTx.unhex(tx.getString("txID"));
        java.math.BigInteger[] rs = EvmTx.sign(priv, hash);
        int rec = EvmTx.recoverId(hash, rs[0], rs[1], priv);
        byte[] sig = new byte[65];
        put32(sig, 0, rs[0]);
        put32(sig, 32, rs[1]);
        sig[64] = (byte) rec;
        tx.put("signature", new JSONArray().put(EvmTx.hex(sig)));
        JSONObject out = post(Net.tronApi() + "/wallet/broadcasttransaction", tx.toString());
        if (!out.optBoolean("result", false)) throw new IllegalArgumentException(tronMessage(out));
        return tx.getString("txID");
    }

    static JSONObject tronPost(String path, String json) throws Exception { return post(Net.tronApi() + path, json); }

    private static String sol(String mnemonic, String from, String to, String amount) throws Exception {
        byte[] seed = ChainKeys.privateKey(mnemonic, ChainKeys.Chain.SOL);
        String mine = ChainKeys.address(mnemonic, ChainKeys.Chain.SOL);
        if (!mine.equals(from)) throw new IllegalArgumentException("This phone's key does not match the address on screen. Open the coin again.");
        byte[] dest = b58(to);
        if (dest.length != 32) throw new IllegalArgumentException("Enter a Solana address.");
        long lamports = exact(units(amount, 9));
        long have = solBalance(from);
        if (have < lamports + 5000L) throw new IllegalArgumentException("Not enough SOL left to pay the network fee.");
        if (solBalance(to) == 0 && lamports < 890880L) throw new IllegalArgumentException("That Solana account is new. Send at least 0.00089088 SOL so the network can create it.");
        JSONObject bh = post(Net.solRpc(),
                "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"getLatestBlockhash\",\"params\":[{\"commitment\":\"confirmed\"}]}");
        String block = bh.getJSONObject("result").getJSONObject("value").getString("blockhash");
        byte[] tx = SolTx.transfer(seed, dest, lamports, b58(block));
        String b64 = android.util.Base64.encodeToString(tx, android.util.Base64.NO_WRAP);
        JSONObject sent = post(Net.solRpc(),
                "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"sendTransaction\",\"params\":[\"" + b64 + "\",{\"encoding\":\"base64\"}]}");
        if (sent.has("error")) throw new IllegalArgumentException(sent.getJSONObject("error").optString("message", "Solana rejected the payment."));
        return sent.getString("result");
    }

    private static BigInteger qty(String hex) {
        String s = hex == null ? "" : hex.trim();
        if (s.startsWith("0x") || s.startsWith("0X")) s = s.substring(2);
        if (s.isEmpty()) return BigInteger.ZERO;
        return new BigInteger(s, 16);
    }

    private static long solBalance(String address) throws Exception {
        JSONObject r = post(Net.solRpc(),
                "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"getBalance\",\"params\":[\"" + address + "\"]}");
        if (r.has("error")) throw new IllegalArgumentException(r.getJSONObject("error").optString("message", "Solana did not answer."));
        return r.getJSONObject("result").optLong("value");
    }

    private static String tronMessage(JSONObject out) {
        String m = out.optString("message", "");
        if (m.matches("[0-9a-fA-F]{8,}")) {
            try {
                byte[] b = EvmTx.unhex(m);
                String text = new String(b, java.nio.charset.StandardCharsets.UTF_8).trim();
                if (!text.isEmpty()) return text;
            } catch (Exception ignored) {}
        }
        if (!m.isEmpty()) return m;
        return out.optString("code", "Tron rejected the payment.");
    }

    private static BigInteger units(String amount, int decimals) {
        BigDecimal d;
        try { d = new BigDecimal(amount.trim()); }
        catch (Exception e) { throw new IllegalArgumentException("Invalid amount."); }
        if (d.signum() <= 0) throw new IllegalArgumentException("Amount must be greater than zero.");
        try { return d.movePointRight(decimals).toBigIntegerExact(); }
        catch (ArithmeticException e) { throw new IllegalArgumentException("Too many decimal places for this coin."); }
    }

    private static long exact(BigInteger v) {
        try { return v.longValueExact(); }
        catch (ArithmeticException e) { throw new IllegalArgumentException("That amount is too large."); }
    }

    static byte[] scriptFor(String address, ChainKeys.Chain chain) throws Exception {
        String a = address.trim();
        if (a.regionMatches(true, 0, "bc1", 0, 3) || a.regionMatches(true, 0, "tb1", 0, 3)
                || a.regionMatches(true, 0, "ltc1", 0, 4) || a.regionMatches(true, 0, "tltc1", 0, 5)) return BtcTx.p2wpkhScript(bech32Program(a.toLowerCase()));
        if (chain == ChainKeys.Chain.BTC) {
            // Legacy (1… / m… n…) and P2SH (3… / 2…) Bitcoin addresses.
            boolean p2sh = Net.TEST ? a.charAt(0) == '2' : a.charAt(0) == '3';
            if (p2sh) return p2shScript(base58check(a, Net.TEST ? 0xc4 : 0x05));
            return BtcTx.p2pkhScript(base58check(a, Net.TEST ? 0x6f : 0x00));
        }
        int version = chain == ChainKeys.Chain.DOGE ? (Net.TEST ? 0x71 : 0x1e) : chain == ChainKeys.Chain.LTC ? 0x30 : 0x00;
        if (a.charAt(0) == '3' || a.charAt(0) == 'M') throw new IllegalArgumentException("That address type is not supported. Use a normal " + chain.ticker + " address.");
        return BtcTx.p2pkhScript(base58check(a, version));
    }

    /** OP_HASH160 <20 bytes> OP_EQUAL. */
    private static byte[] p2shScript(byte[] hash20) {
        byte[] s = new byte[23];
        s[0] = (byte) 0xa9; s[1] = 0x14;
        System.arraycopy(hash20, 0, s, 2, 20);
        s[22] = (byte) 0x87;
        return s;
    }

    private static byte[] bech32Program(String addr) {
        int sep = addr.lastIndexOf('1');
        if (sep < 1) throw new IllegalArgumentException("That address is not valid.");
        String hrp = addr.substring(0, sep);
        String data = addr.substring(sep + 1);
        String cs = "qpzry9x8gf2tvdw0s3jn54khce6mua7l";
        int[] v = new int[data.length()];
        for (int i = 0; i < data.length(); i++) {
            int n = cs.indexOf(data.charAt(i));
            if (n < 0) throw new IllegalArgumentException("That address is not valid.");
            v[i] = n;
        }
        if (polymod(hrp, v) != 1) throw new IllegalArgumentException("That address is not valid.");
        if (v[0] != 0) throw new IllegalArgumentException("Only a native SegWit address (tb1, bc1, tltc1 or ltc1) can be paid here.");
        int acc = 0, bits = 0, p = 0;
        byte[] out = new byte[40];
        for (int i = 1; i < v.length - 6; i++) {
            acc = (acc << 5) | v[i];
            bits += 5;
            if (bits >= 8) { out[p++] = (byte) ((acc >> (bits - 8)) & 0xFF); bits -= 8; }
        }
        if (p != 20) throw new IllegalArgumentException("Only a normal SegWit address can be paid here.");
        return java.util.Arrays.copyOf(out, 20);
    }

    private static int polymod(String hrp, int[] data) {
        int[] gen = {0x3b6a57b2, 0x26508e6d, 0x1ea119fa, 0x3d4233dd, 0x2a1462b3};
        int chk = 1;
        int[] exp = new int[hrp.length() * 2 + 1 + data.length];
        int n = 0;
        for (int i = 0; i < hrp.length(); i++) exp[n++] = hrp.charAt(i) >> 5;
        exp[n++] = 0;
        for (int i = 0; i < hrp.length(); i++) exp[n++] = hrp.charAt(i) & 31;
        for (int x : data) exp[n++] = x;
        for (int v : exp) {
            int b = chk >>> 25;
            chk = ((chk & 0x1ffffff) << 5) ^ v;
            for (int i = 0; i < 5; i++) if (((b >> i) & 1) != 0) chk ^= gen[i];
        }
        return chk;
    }

    private static byte[] base58check(String addr, int version) throws Exception {
        byte[] raw = b58(addr);
        if (raw.length != 25 || (raw[0] & 0xFF) != version) throw new IllegalArgumentException("That address is not valid for this coin.");
        byte[] body = java.util.Arrays.copyOf(raw, 21);
        byte[] check = BtcTx.dsha(body);
        for (int i = 0; i < 4; i++) if (check[i] != raw[21 + i]) throw new IllegalArgumentException("That address is not valid.");
        return java.util.Arrays.copyOfRange(body, 1, 21);
    }

    static byte[] b58(String s) {
        java.math.BigInteger v = java.math.BigInteger.ZERO;
        java.math.BigInteger b = java.math.BigInteger.valueOf(58);
        String alpha = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
        for (int i = 0; i < s.length(); i++) {
            int n = alpha.indexOf(s.charAt(i));
            if (n < 0) throw new IllegalArgumentException("That address is not valid.");
            v = v.multiply(b).add(java.math.BigInteger.valueOf(n));
        }
        byte[] bytes = v.toByteArray();
        if (bytes[0] == 0) bytes = java.util.Arrays.copyOfRange(bytes, 1, bytes.length);
        int zeros = 0;
        while (zeros < s.length() && s.charAt(zeros) == '1') zeros++;
        byte[] out = new byte[zeros + bytes.length];
        System.arraycopy(bytes, 0, out, zeros, bytes.length);
        return out;
    }

    private static void put32(byte[] dest, int off, java.math.BigInteger v) {
        byte[] b = v.toByteArray();
        if (b.length > 32) b = java.util.Arrays.copyOfRange(b, b.length - 32, b.length);
        System.arraycopy(b, 0, dest, off + 32 - b.length, b.length);
    }

    private static JSONObject get(String url) throws Exception {
        try (Response r = HTTP.newCall(new Request.Builder().url(url).build()).execute()) {
            String body = r.body() == null ? "" : r.body().string();
            if (!r.isSuccessful()) throw new IllegalArgumentException(plain(body, r.code()));
            return new JSONObject(body);
        }
    }

    private static JSONArray getArray(String url) throws Exception {
        try (Response r = HTTP.newCall(new Request.Builder().url(url).build()).execute()) {
            String body = r.body() == null ? "[]" : r.body().string();
            if (!r.isSuccessful()) throw new IllegalArgumentException(plain(body, r.code()));
            return new JSONArray(body);
        }
    }

    private static JSONObject post(String url, String json) throws Exception {
        Request req = new Request.Builder().url(url).post(RequestBody.create(json, JSON)).build();
        try (Response r = HTTP.newCall(req).execute()) {
            String body = r.body() == null ? "{}" : r.body().string();
            if (body.isEmpty()) body = "{}";
            if (!r.isSuccessful() && !body.startsWith("{")) throw new IllegalArgumentException(plain(body, r.code()));
            return new JSONObject(body);
        }
    }

    private static String postText(String url, String text) throws Exception {
        Request req = new Request.Builder().url(url).post(RequestBody.create(text, MediaType.parse("text/plain"))).build();
        try (Response r = HTTP.newCall(req).execute()) {
            String body = r.body() == null ? "" : r.body().string();
            if (!r.isSuccessful()) throw new IllegalArgumentException(plain(body, r.code()));
            return body.trim();
        }
    }

    private static String plain(String body, int code) {
        String t = body == null ? "" : body.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
        if (t.length() > 160) t = t.substring(0, 160);
        return t.isEmpty() ? "The network did not accept the payment (" + code + ")." : t;
    }
}
