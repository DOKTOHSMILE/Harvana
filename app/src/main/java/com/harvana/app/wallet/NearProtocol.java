package com.harvana.app.wallet;

import com.harvana.app.crypto.Ed25519;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * The NEAR protocol engine: how any NEAR-protocol network signs, reads and sends. It knows no network: the RPC, the
 * explorer, the key path and the history API all come from the chain the person added ({@link CustomAssets.Asset}).
 * Keys are ed25519 by SLIP-0010 on the chain's path; the account is the implicit one (the public key in hex).
 * A transaction is borsh-encoded and signed over its SHA-256; its id is that hash in base58.
 * Tokens on such a chain are NEP-141 contracts (ft_balance_of, ft_metadata, ft_transfer). Run off the main thread.
 */
public final class NearProtocol {
    private NearProtocol() {}

    public static final String FAMILY = "NEAR";
    /** Gas a plain transfer burns, about 0.45 Tgas; times the gas price gives the fee. */
    private static final long TRANSFER_GAS = 450_000_000_000L;
    /** Gas attached to each token call (30 Tgas, what NEAR wallets attach to ft_transfer). */
    private static final long CALL_GAS = 30_000_000_000_000L;
    /** Bytes of state an account keeps cost 10^19 yocto each; that part of the balance can't be spent. */
    private static final BigInteger STORAGE_PRICE = BigInteger.TEN.pow(19);

    private static final OkHttpClient HTTP = new OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build();
    private static final MediaType JSON = MediaType.parse("application/json");

    // ── reading a chain the person typed in ───────────────────────────────

    /** The network id the RPC reports ("mainnet", "testnet", …). Throws when the RPC does not answer as NEAR. */
    public static String networkId(String rpc) throws Exception {
        JSONObject r = call(rpc, "status", new JSONArray());
        JSONObject res = r.optJSONObject("result");
        if (res == null || res.optString("chain_id").isEmpty()) throw new IllegalStateException("That RPC did not answer as a NEAR network.");
        return res.optString("chain_id");
    }

    /** The SLIP-0010 path "m/44'/397'/0'" as indexes (every step hardened for ed25519). */
    static int[] path(String path) {
        String p = path == null || path.trim().isEmpty() ? "m/44'/397'/0'" : path.trim();
        String[] parts = p.replaceFirst("^m/", "").split("/");
        int[] out = new int[parts.length];
        for (int i = 0; i < parts.length; i++) out[i] = Integer.parseInt(parts[i].replace("'", "").replace("h", ""));
        return out;
    }

    static byte[] privateSeed(String mnemonic, String path) throws Exception {
        return PiKeys.slip10Ed25519(PiKeys.bip39Seed(mnemonic), path(path));
    }

    /** The implicit account on that path: the ed25519 public key in lowercase hex. */
    public static String address(String mnemonic, String path) throws Exception {
        return hex(Ed25519.publicKeyFromSeed(privateSeed(mnemonic, path)));
    }

    /** A NEAR account id: 64-hex implicit, 0x + 40-hex (ETH-implicit), or a named account such as alice.near. */
    public static boolean validAccount(String id) {
        if (id == null) return false;
        if (id.matches("^[0-9a-f]{64}$") || id.matches("^0x[0-9a-f]{40}$")) return true;
        return id.length() >= 2 && id.length() <= 64 && id.matches("^(([a-z\\d]+[\\-_])*[a-z\\d]+\\.)*([a-z\\d]+[\\-_])*[a-z\\d]+$");
    }

    // ── balances ──────────────────────────────────────────────────────────

    /** Raw balance and storage bytes; {0, 0} for an account the chain has not created yet. */
    static BigInteger[] account(String rpc, String id) throws Exception {
        JSONObject r = call(rpc, "query", new JSONObject().put("request_type", "view_account").put("finality", "final").put("account_id", id));
        JSONObject res = r.optJSONObject("result");
        if (res == null) {
            if (unknownAccount(r)) return new BigInteger[]{BigInteger.ZERO, BigInteger.ZERO};
            throw new IllegalStateException(message(r));
        }
        return new BigInteger[]{new BigInteger(res.optString("amount", "0")), BigInteger.valueOf(res.optLong("storage_usage"))};
    }

    /** The coin balance as a plain number. */
    public static String balance(CustomAssets.Asset chain, String id) throws Exception {
        return ChainBalances.amount(account(chain.rpc, id)[0], chain.decimals);
    }

    /** A NEP-141 token's balance as a plain number. */
    public static String tokenBalance(CustomAssets.Asset token, String id) throws Exception {
        Object r = view(token.rpc, token.contract, "ft_balance_of", new JSONObject().put("account_id", id));
        return ChainBalances.amount(new BigInteger(r == null ? "0" : String.valueOf(r)), token.decimals);
    }

    /** {name, symbol, decimals, icon} from a NEP-141 contract's ft_metadata. */
    public static String[] tokenMetadata(String rpc, String contract) throws Exception {
        Object r = view(rpc, contract, "ft_metadata", new JSONObject());
        if (!(r instanceof JSONObject)) throw new IllegalStateException("That account is not a token on this network.");
        JSONObject m = (JSONObject) r;
        String icon = m.optString("icon", "");
        if (icon.startsWith("data:")) icon = "";                              // inline images are not stored
        return new String[]{m.optString("name"), m.optString("symbol"), String.valueOf(m.optInt("decimals", 18)), icon};
    }

    /** The fee of one plain transfer now, in the chain's smallest unit. */
    public static BigInteger transferFee(String rpc) throws Exception {
        JSONObject r = call(rpc, "gas_price", new JSONArray().put(JSONObject.NULL));
        JSONObject res = r.optJSONObject("result");
        BigInteger price = res == null ? BigInteger.valueOf(100_000_000L) : new BigInteger(res.optString("gas_price", "100000000"));
        return price.multiply(BigInteger.valueOf(TRANSFER_GAS));
    }

    // ── sending ───────────────────────────────────────────────────────────

    /** Sends the chain's coin; returns the transaction id. */
    public static String send(CustomAssets.Asset chain, String mnemonic, String from, String to, String amount) throws Exception {
        BigDecimal d;
        try { d = new BigDecimal(amount != null ? amount.trim().replace(",", ".") : ""); }
        catch (Exception e) { throw new IllegalArgumentException("Enter a valid amount."); }
        if (d.signum() <= 0) throw new IllegalArgumentException("Enter an amount above zero.");
        BigInteger deposit;
        try { deposit = d.movePointRight(chain.decimals).toBigIntegerExact(); }
        catch (ArithmeticException e) { throw new IllegalArgumentException("Too many decimal places for " + chain.symbol + "."); }
        if (!validAccount(to)) throw new IllegalArgumentException("Enter an account: 64 characters, or a name such as alice.near.");
        BigInteger[] acc = account(chain.rpc, from);
        BigInteger spendable = acc[0].subtract(acc[1].multiply(STORAGE_PRICE));
        if (spendable.compareTo(deposit.add(transferFee(chain.rpc))) < 0) throw new IllegalArgumentException("Not enough " + chain.symbol + " for this amount and the network fee.");
        ByteArrayOutputStream actions = new ByteArrayOutputStream();
        transferAction(actions, deposit);
        return sign(chain.rpc, chain.derivation, mnemonic, from, to, 1, actions.toByteArray());
    }

    /** Sends a NEP-141 token: registers the receiver's storage first when it has none, then ft_transfer. */
    public static String sendToken(CustomAssets.Asset token, String mnemonic, String from, String to, String amount) throws Exception {
        BigDecimal d2;
        try { d2 = new BigDecimal(amount != null ? amount.trim().replace(",", ".") : ""); }
        catch (Exception e) { throw new IllegalArgumentException("Enter a valid amount."); }
        if (d2.signum() <= 0) throw new IllegalArgumentException("Enter an amount above zero.");
        BigInteger value;
        try { value = d2.movePointRight(token.decimals).toBigIntegerExact(); }
        catch (ArithmeticException e) { throw new IllegalArgumentException("Too many decimal places for " + token.symbol + "."); }
        if (!validAccount(to)) throw new IllegalArgumentException("Enter an account: 64 characters, or a name such as alice.near.");
        BigInteger held = new BigInteger(String.valueOf(view(token.rpc, token.contract, "ft_balance_of", new JSONObject().put("account_id", from))));
        if (held.compareTo(value) < 0) throw new IllegalArgumentException("Not enough " + token.symbol + ".");
        ByteArrayOutputStream actions = new ByteArrayOutputStream();
        int count = 0;
        Object registered = view(token.rpc, token.contract, "storage_balance_of", new JSONObject().put("account_id", to));
        if (registered == null || registered == JSONObject.NULL) {
            Object bounds = view(token.rpc, token.contract, "storage_balance_bounds", new JSONObject());
            String min = bounds instanceof JSONObject ? ((JSONObject) bounds).optString("min", "1250000000000000000000") : "1250000000000000000000";
            callAction(actions, "storage_deposit", new JSONObject().put("account_id", to).put("registration_only", true).toString(), CALL_GAS, new BigInteger(min));
            count++;
        }
        callAction(actions, "ft_transfer", new JSONObject().put("receiver_id", to).put("amount", value.toString()).toString(), CALL_GAS, BigInteger.ONE);
        count++;
        return sign(token.rpc, chainOf(token).derivation, mnemonic, from, token.contract, count, actions.toByteArray());
    }

    private static CustomAssets.Asset chainOf(CustomAssets.Asset token) {
        CustomAssets.Asset c = CustomAssets.find(token.chainKey);
        return c == null ? token : c;
    }

    /** Builds, signs and sends one transaction with these borsh-encoded actions. */
    private static String sign(String rpc, String path, String mnemonic, String from, String receiver, int actionCount, byte[] actions) throws Exception {
        byte[] seed = privateSeed(mnemonic, path);
        try {
            byte[] pub = Ed25519.publicKeyFromSeed(seed);
            if (!hex(pub).equals(from)) throw new IllegalArgumentException("This phone's key does not match the address on screen. Open the coin again.");
            JSONObject k = call(rpc, "query", new JSONObject().put("request_type", "view_access_key").put("finality", "final")
                    .put("account_id", from).put("public_key", "ed25519:" + ChainKeys.base58(pub)));
            JSONObject key = k.optJSONObject("result");
            if (key == null) throw new IllegalArgumentException(unknownAccount(k) ? "This account has nothing on this network yet. Receive some first." : message(k));
            byte[] tx = encode(from, pub, key.optLong("nonce") + 1, receiver, ChainSend.b58(key.optString("block_hash")), actionCount, actions);
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(tx);
            String b64 = android.util.Base64.encodeToString(signed(tx, Ed25519.sign(seed, hash)), android.util.Base64.NO_WRAP);
            JSONObject sent = call(rpc, "send_tx", new JSONObject().put("signed_tx_base64", b64).put("wait_until", "INCLUDED"));
            if (sent.has("error") && message(sent).toLowerCase(java.util.Locale.US).contains("method not found")) sent = call(rpc, "broadcast_tx_commit", new JSONArray().put(b64));
            if (sent.has("error")) throw new IllegalArgumentException(message(sent));
            return ChainKeys.base58(hash);
        } finally {
            java.util.Arrays.fill(seed, (byte) 0);
        }
    }

    /** Transaction { signer_id, public_key, nonce, receiver_id, block_hash, actions }, borsh. */
    static byte[] encode(String signer, byte[] pub, long nonce, String receiver, byte[] blockHash, int actionCount, byte[] actions) {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        string(o, signer);
        o.write(0);                                     // key type ed25519
        o.write(pub, 0, 32);
        le(o, nonce, 8);
        string(o, receiver);
        o.write(blockHash, 0, 32);
        le(o, actionCount, 4);
        o.write(actions, 0, actions.length);
        return o.toByteArray();
    }

    /** Action::Transfer { deposit: u128 }. */
    static void transferAction(ByteArrayOutputStream o, BigInteger deposit) {
        o.write(3);
        u128(o, deposit);
    }

    /** Action::FunctionCall { method_name, args, gas, deposit }. */
    static void callAction(ByteArrayOutputStream o, String method, String argsJson, long gas, BigInteger deposit) {
        o.write(2);
        string(o, method);
        byte[] args = argsJson.getBytes(StandardCharsets.UTF_8);
        le(o, args.length, 4);
        o.write(args, 0, args.length);
        le(o, gas, 8);
        u128(o, deposit);
    }

    static byte[] signed(byte[] tx, byte[] sig) {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        o.write(tx, 0, tx.length);
        o.write(0);                                     // signature type ed25519
        o.write(sig, 0, 64);
        return o.toByteArray();
    }

    // ── history ───────────────────────────────────────────────────────────

    /**
     * Recent transactions from the chain's history API (NearBlocks-style: {api}/v1/account/{id}/txns), shaped like the
     * other coins' history rows. Empty when the chain has no history API set.
     */
    public static List<HashMap<String, Object>> history(CustomAssets.Asset chain, String id) {
        List<HashMap<String, Object>> out = new ArrayList<>();
        if (chain.historyApi == null || chain.historyApi.isEmpty()) return out;
        try {
            Request req = new Request.Builder().url(chain.historyApi + "/v1/account/" + id + "/txns?per_page=25").get().build();
            JSONObject j;
            try (Response r = HTTP.newCall(req).execute()) { j = new JSONObject(r.body() == null ? "{}" : r.body().string()); }
            JSONArray txns = j.optJSONArray("txns");
            for (int i = 0; txns != null && i < txns.length(); i++) {
                JSONObject t = txns.optJSONObject(i);
                if (t == null) continue;
                String from = t.optString("predecessor_account_id", t.optString("signer_account_id"));
                String to = t.optString("receiver_account_id");
                JSONObject agg = t.optJSONObject("actions_agg");
                double value;
                try { value = new BigDecimal(agg == null ? "0" : agg.optString("deposit", "0")).movePointLeft(chain.decimals).doubleValue(); } catch (Exception e) { value = 0; }
                long ts = 0;
                try { ts = new BigDecimal(t.optString("block_timestamp", "0")).movePointLeft(6).longValue(); } catch (Exception ignored) { }
                String hash = t.optString("transaction_hash");
                JSONObject outcomes = t.optJSONObject("outcomes");
                HashMap<String, Object> m = new HashMap<>();
                m.put("tokenSymbol", chain.symbol);
                m.put("chain", chain.chainName);
                m.put("incoming", id.equals(to) && !id.equals(from));
                m.put("senderAddress", from);
                m.put("recipientAddress", to);
                m.put("value", value);
                m.put("timestamp", ts);
                m.put("txHash", hash);
                m.put("status", outcomes == null || outcomes.optBoolean("status", true) ? "confirmed" : "failed");
                m.put("memo", "");
                m.put("explorerUrl", chain.txPage(hash));
                out.add(m);
            }
        } catch (Exception ignored) { }
        return out;
    }

    // ── helpers ───────────────────────────────────────────────────────────

    /** A view call: the contract method's JSON result, or null. */
    private static Object view(String rpc, String contract, String method, JSONObject args) throws Exception {
        String b64 = android.util.Base64.encodeToString(args.toString().getBytes(StandardCharsets.UTF_8), android.util.Base64.NO_WRAP);
        JSONObject r = call(rpc, "query", new JSONObject().put("request_type", "call_function").put("finality", "final")
                .put("account_id", contract).put("method_name", method).put("args_base64", b64));
        JSONObject res = r.optJSONObject("result");
        if (res == null) throw new IllegalStateException(message(r));
        if (res.has("error")) throw new IllegalStateException(res.optString("error"));
        JSONArray bytes = res.optJSONArray("result");
        if (bytes == null) return null;
        byte[] b = new byte[bytes.length()];
        for (int i = 0; i < b.length; i++) b[i] = (byte) bytes.optInt(i);
        String text = new String(b, StandardCharsets.UTF_8).trim();
        if (text.isEmpty() || "null".equals(text)) return null;
        return new org.json.JSONTokener(text).nextValue();
    }

    /** JSON-RPC to the chain's RPC; several may be given, separated by commas, and are tried in turn. */
    private static JSONObject call(String rpcs, String method, Object params) throws Exception {
        String body = new JSONObject().put("jsonrpc", "2.0").put("id", "h").put("method", method).put("params", params).toString();
        Exception last = null;
        for (String url : (rpcs == null ? "" : rpcs).split(",")) {
            if (url.trim().isEmpty()) continue;
            try {
                Request req = new Request.Builder().url(url.trim()).post(RequestBody.create(body, JSON)).build();
                try (Response r = HTTP.newCall(req).execute()) {
                    String text = r.body() == null ? "" : r.body().string();
                    if (text.isEmpty() || !text.trim().startsWith("{")) throw new IllegalStateException("The RPC did not answer (" + r.code() + ").");
                    return new JSONObject(text);
                }
            } catch (Exception e) { last = e; }
        }
        throw last == null ? new IllegalStateException("No RPC is set for this chain.") : last;
    }

    private static boolean unknownAccount(JSONObject r) {
        String s = r.toString();
        return s.contains("UNKNOWN_ACCOUNT") || s.contains("does not exist") || s.contains("UNKNOWN_ACCESS_KEY");
    }

    private static String message(JSONObject r) {
        JSONObject e = r.optJSONObject("error");
        if (e == null) return "The network refused this.";
        JSONObject cause = e.optJSONObject("cause");
        String m = cause != null ? cause.optString("name", "") : "";
        if (m.isEmpty()) m = e.optString("message", "The network refused this.");
        Object data = e.opt("data");
        return data instanceof String && !((String) data).isEmpty() ? m + ": " + data : m;
    }

    private static void string(ByteArrayOutputStream o, String s) {
        byte[] b = s.getBytes(StandardCharsets.UTF_8);
        le(o, b.length, 4);
        o.write(b, 0, b.length);
    }

    private static void u128(ByteArrayOutputStream o, BigInteger v) {
        byte[] d = new byte[16];
        byte[] be = v.toByteArray();
        for (int i = 0; i < be.length && i < 16; i++) d[i] = be[be.length - 1 - i];
        o.write(d, 0, 16);
    }

    private static void le(ByteArrayOutputStream o, long v, int n) {
        for (int i = 0; i < n; i++) o.write((int) (v >>> (8 * i)) & 0xFF);
    }

    static String hex(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (byte x : b) sb.append(String.format("%02x", x & 0xFF));
        return sb.toString();
    }
}
