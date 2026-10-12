package com.harvana.app.wallet;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * The wallet side of the dApp browser for EVM chains (Ethereum, BNB Chain, Polygon, Base): the account, read calls
 * forwarded to the chain's RPC, and signing. The private key is derived only for one signature, inside this class,
 * and never leaves it; the page only ever receives addresses, signatures and transaction hashes.
 */
public final class Web3Signer {
    private Web3Signer() {}

    private static final OkHttpClient HTTP = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build();
    private static final MediaType JSON = MediaType.parse("application/json");

    /** The chains the browser offers, by the app's coin key. */
    public static final String[] CHAINS = {"ETH", "BNB", "POL", "BASE"};

    public static Net.Evm net(String key) { return Net.evm(key); }

    /** The active wallet's EVM address (the same one on every EVM chain). */
    public static String address(Context c) throws Exception {
        String m = SecureWalletStorage.getActiveMnemonic(c.getApplicationContext());
        if (m == null || m.trim().isEmpty()) throw new IllegalStateException("NO_PHRASE");
        return ChainKeys.address(m, ChainKeys.Chain.ETH);
    }

    private static byte[] key(Context c, String expect) throws Exception {
        String m = SecureWalletStorage.getActiveMnemonic(c.getApplicationContext());
        if (m == null || m.trim().isEmpty()) throw new IllegalStateException("NO_PHRASE");
        String mine = ChainKeys.address(m, ChainKeys.Chain.ETH);
        if (expect != null && !mine.equalsIgnoreCase(expect)) throw new IllegalStateException("WRONG_ACCOUNT");
        return ChainKeys.privateKey(m, ChainKeys.Chain.ETH);
    }

    /** personal_sign: "\x19Ethereum Signed Message:\n" + length + message. */
    public static String personalSign(Context c, String account, byte[] message) throws Exception {
        byte[] prefix = ("\u0019Ethereum Signed Message:\n" + message.length).getBytes(StandardCharsets.UTF_8);
        byte[] all = new byte[prefix.length + message.length];
        System.arraycopy(prefix, 0, all, 0, prefix.length);
        System.arraycopy(message, 0, all, prefix.length, message.length);
        byte[] priv = key(c, account);
        try { return EvmTx.signHash65(priv, EvmTx.keccak(all)); }
        finally { java.util.Arrays.fill(priv, (byte) 0); }
    }

    /** eth_signTypedData_v4. */
    public static String signTypedData(Context c, String account, JSONObject typed) throws Exception {
        byte[] digest = Eip712.digest(typed);
        byte[] priv = key(c, account);
        try { return EvmTx.signHash65(priv, digest); }
        finally { java.util.Arrays.fill(priv, (byte) 0); }
    }

    /** Fills in what the page left out (gas, price, nonce) and returns {gasLimit, gasPriceWei} for review. */
    public static long[] estimate(Net.Evm n, String from, JSONObject tx) throws Exception {
        long gas = tx.has("gas") ? qty(tx.optString("gas")).longValue() : 0;
        if (gas <= 0) {
            JSONObject call = new JSONObject().put("from", from);
            for (String k : new String[]{"to", "value", "data"}) if (tx.has(k)) call.put(k, tx.get(k));
            gas = qty(rpc(n.rpc, "eth_estimateGas", new JSONArray().put(call)).toString()).longValue() * 12 / 10;
        }
        BigInteger price = tx.has("gasPrice") ? qty(tx.optString("gasPrice"))
                : qty(rpc(n.rpc, "eth_gasPrice", new JSONArray()).toString()).multiply(BigInteger.valueOf(110)).divide(BigInteger.valueOf(100));
        if (price.signum() == 0) price = BigInteger.valueOf(1_000_000_000L);
        return new long[]{gas, price.min(BigInteger.valueOf(Long.MAX_VALUE)).longValue()};
    }

    /** eth_sendTransaction after the person approved it: signs (legacy, EIP-155) and broadcasts. Returns the hash. */
    public static String send(Context c, Net.Evm n, String from, JSONObject tx, long gas, long gasPrice) throws Exception {
        String to = tx.optString("to", "");
        if (!to.isEmpty() && !to.matches("(?i)0x[0-9a-f]{40}")) throw new IllegalStateException("BAD_RECIPIENT");
        BigInteger value = tx.has("value") ? qty(tx.optString("value")) : BigInteger.ZERO;
        String dataHex = tx.optString("data", tx.optString("input", ""));
        byte[] data = dataHex.isEmpty() || "0x".equals(dataHex) ? new byte[0] : EvmTx.unhex(strip(dataHex));
        BigInteger balance = qty(rpc(n.rpc, "eth_getBalance", new JSONArray().put(from).put("latest")).toString());
        if (balance.compareTo(value.add(BigInteger.valueOf(gasPrice).multiply(BigInteger.valueOf(gas)))) < 0) {
            throw new IllegalStateException("INSUFFICIENT_FUNDS");
        }
        long nonce = qty(rpc(n.rpc, "eth_getTransactionCount", new JSONArray().put(from).put("pending")).toString()).longValue();
        byte[] priv = key(c, from);
        String raw;
        try {
            raw = EvmTx.rawWithData(priv, nonce, BigInteger.valueOf(gasPrice), gas, to.isEmpty() ? new byte[0] : EvmTx.unhex(strip(to)), value, data, n.chainId);
        } finally { java.util.Arrays.fill(priv, (byte) 0); }
        return String.valueOf(rpc(n.rpc, "eth_sendRawTransaction", new JSONArray().put("0x" + raw)));
    }

    /** Read-only methods a page may call directly; everything else needs the person. */
    public static boolean isReadOnly(String method) {
        switch (method) {
            case "eth_blockNumber": case "eth_call": case "eth_estimateGas": case "eth_gasPrice":
            case "eth_getBalance": case "eth_getBlockByNumber": case "eth_getBlockByHash": case "eth_getCode":
            case "eth_getLogs": case "eth_getStorageAt": case "eth_getTransactionByHash": case "eth_getTransactionCount":
            case "eth_getTransactionReceipt": case "eth_feeHistory": case "eth_maxPriorityFeePerGas": case "net_listening":
            case "eth_syncing": case "web3_clientVersion":
                return true;
            default: return false;
        }
    }

    /** Forwards a read call; returns the raw JSON result (a string, object, array or null). */
    public static Object rpc(String url, String method, JSONArray params) throws Exception {
        JSONObject body = new JSONObject().put("jsonrpc", "2.0").put("id", 1).put("method", method).put("params", params == null ? new JSONArray() : params);
        try (Response r = HTTP.newCall(new Request.Builder().url(url).post(RequestBody.create(body.toString(), JSON)).build()).execute()) {
            String text = r.body() == null ? "" : r.body().string();
            JSONObject j = new JSONObject(text);
            if (j.has("error")) {
                JSONObject e = j.optJSONObject("error");
                throw new RpcError(e == null ? -32603 : e.optInt("code", -32603), e == null ? "RPC error" : e.optString("message", "RPC error"));
            }
            return j.opt("result");
        }
    }

    public static final class RpcError extends Exception {
        public final int code;
        RpcError(int code, String message) { super(message); this.code = code; }
    }

    public static BigInteger qty(String hex) {
        if (hex == null || hex.isEmpty() || "null".equals(hex)) return BigInteger.ZERO;
        String h = strip(hex);
        return h.isEmpty() ? BigInteger.ZERO : new BigInteger(h, 16);
    }

    public static byte[] unhex(String h) { String s = strip(h); return EvmTx.unhex(s.length() % 2 == 1 ? "0" + s : s); }

    private static String strip(String h) { return h.startsWith("0x") || h.startsWith("0X") ? h.substring(2) : h; }
}
