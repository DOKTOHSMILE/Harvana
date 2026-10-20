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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * The SPL token standard on Solana (the Token program and Token-2022): any mint, read from the chain. A holding sits in
 * the owner's associated token account; a transfer creates the receiver's one when it is missing (the sender pays its
 * rent) and moves the amount with TransferChecked. Run off the main thread.
 */
public final class SplToken {
    private SplToken() {}

    public static final String FAMILY = "SOLANA";
    static final String SYSTEM = "11111111111111111111111111111111";
    static final String TOKEN = "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA";
    static final String TOKEN_2022 = "TokenzQdBNbLqP5VEhdkAS6EPFLC1PHnBqCXEpPxuEb";
    static final String ATA = "ATokenGPvbdGVxr1b2hvZbsiqW5xWH25efTNsLJA8knL";

    private static final OkHttpClient HTTP = new OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build();
    private static final MediaType JSON = MediaType.parse("application/json");

    /** {decimals, token program} of a mint, or null when the address is not a mint. */
    public static String[] mint(String rpc, String mint) throws Exception {
        JSONObject r = call(rpc, "getAccountInfo", new JSONArray().put(mint).put(new JSONObject().put("encoding", "jsonParsed")));
        JSONObject value = r.optJSONObject("result") == null ? null : r.optJSONObject("result").optJSONObject("value");
        if (value == null) return null;
        String owner = value.optString("owner");
        if (!TOKEN.equals(owner) && !TOKEN_2022.equals(owner)) return null;
        JSONObject data = value.optJSONObject("data");
        JSONObject parsed = data == null ? null : data.optJSONObject("parsed");
        if (parsed == null || !"mint".equals(parsed.optString("type"))) return null;
        JSONObject info = parsed.optJSONObject("info");
        return new String[]{String.valueOf(info == null ? 0 : info.optInt("decimals")), owner};
    }

    /** What the owner holds of this mint, as a plain number. */
    public static String balance(CustomAssets.Asset token, String owner) throws Exception {
        BigInteger raw = BigInteger.ZERO;
        for (String[] a : accounts(token.rpc, owner, token.contract)) raw = raw.add(new BigInteger(a[1]));
        return ChainBalances.amount(raw, token.decimals);
    }

    /** The owner's token accounts for a mint: {address, raw amount}. */
    static List<String[]> accounts(String rpc, String owner, String mint) throws Exception {
        JSONObject r = call(rpc, "getTokenAccountsByOwner", new JSONArray().put(owner).put(new JSONObject().put("mint", mint))
                .put(new JSONObject().put("encoding", "jsonParsed")));
        List<String[]> out = new ArrayList<>();
        JSONObject res = r.optJSONObject("result");
        JSONArray value = res == null ? null : res.optJSONArray("value");
        for (int i = 0; value != null && i < value.length(); i++) {
            JSONObject v = value.optJSONObject(i);
            JSONObject info = v == null ? null : v.optJSONObject("account").optJSONObject("data").optJSONObject("parsed").optJSONObject("info");
            JSONObject amt = info == null ? null : info.optJSONObject("tokenAmount");
            if (amt != null) out.add(new String[]{v.optString("pubkey"), amt.optString("amount", "0")});
        }
        return out;
    }

    /** The associated token account of {@code owner} for {@code mint} under {@code tokenProgram}. */
    static byte[] associated(byte[] owner, byte[] tokenProgram, byte[] mint) throws Exception {
        byte[] program = ChainSend.b58(ATA);
        for (int bump = 255; bump >= 0; bump--) {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            sha.update(owner);
            sha.update(tokenProgram);
            sha.update(mint);
            sha.update((byte) bump);
            sha.update(program);
            sha.update("ProgramDerivedAddress".getBytes(StandardCharsets.UTF_8));
            byte[] h = sha.digest();
            if (!Ed25519.onCurve(h)) return h;
        }
        throw new IllegalStateException("No associated account.");
    }

    /** Rent that creates a receiver's token account (165 bytes), paid in SOL by the sender. */
    static final long ACCOUNT_RENT_LAMPORTS = 2_039_280L;

    /** Live fee in SOL: 5,000 lamports, plus the rent for the receiver's token account when they have none yet. */
    static ChainSend.Quote quote(CustomAssets.Asset token, String from, String to, ChainSend.Quote q) throws Exception {
        q.feeSymbol = "SOL";
        long lamports = 5000L;
        if (to != null && !to.isEmpty()) {
            try { if (accounts(token.rpc, to, token.contract).isEmpty()) lamports += ACCOUNT_RENT_LAMPORTS; }
            catch (Exception ignored) { }
        }
        q.fee = BigDecimal.valueOf(lamports).movePointLeft(9);
        q.maxSend = new BigDecimal(balance(token, from));
        return q;
    }

    /** Sends {@code amount} of the token; returns the transaction signature. */
    public static String send(CustomAssets.Asset token, byte[] seed, String from, String to, String amount) throws Exception {
        byte[] payer = Ed25519.publicKeyFromSeed(seed);
        if (!ChainKeys.base58(payer).equals(from)) throw new IllegalArgumentException("This phone's key does not match the address on screen. Open the coin again.");
        byte[] receiver = ChainSend.b58(to);
        if (receiver.length != 32) throw new IllegalArgumentException("Enter a Solana address.");
        BigDecimal d;
        try { d = new BigDecimal(amount != null ? amount.trim().replace(",", ".") : ""); }
        catch (Exception e) { throw new IllegalArgumentException("Enter a valid amount."); }
        if (d.signum() <= 0) throw new IllegalArgumentException("Enter an amount above zero.");
        BigInteger value;
        try { value = d.movePointRight(token.decimals).toBigIntegerExact(); }
        catch (ArithmeticException e) { throw new IllegalArgumentException("Too many decimal places for " + token.symbol + " (maximum " + token.decimals + ")."); }
        String[] m = mint(token.rpc, token.contract);
        if (m == null) throw new IllegalArgumentException("That token is not a mint on this network.");
        byte[] mint = ChainSend.b58(token.contract), program = ChainSend.b58(m[1]);
        String[] source = null;
        for (String[] a : accounts(token.rpc, from, token.contract)) if (new BigInteger(a[1]).compareTo(value) >= 0) { source = a; break; }
        if (source == null) throw new IllegalArgumentException("Not enough " + token.symbol + ".");
        byte[] dest = associated(receiver, program, mint);
        JSONObject info = call(token.rpc, "getAccountInfo", new JSONArray().put(ChainKeys.base58(dest)).put(new JSONObject().put("encoding", "base64")));
        boolean create = info.optJSONObject("result") == null || info.optJSONObject("result").isNull("value");
        final String[] src = source;
        final int decimals = Integer.parseInt(m[0]);
        return SolSend.send(token.rpc.split(",")[0].trim(), blockhash -> {
            byte[] message = message(payer, ChainSend.b58(src[0]), dest, receiver, mint, program, value.longValue(),
                    decimals, create, blockhash);
            byte[] sig = Ed25519.sign(seed, message);
            ByteArrayOutputStream tx = new ByteArrayOutputStream();
            tx.write(1);
            tx.write(sig, 0, 64);
            tx.write(message, 0, message.length);
            return tx.toByteArray();
        });
    }

    /**
     * A legacy message: [create the receiver's associated account (idempotent)], then TransferChecked.
     * Accounts: payer (signer, writable); source, destination (writable); receiver, mint, programs (read-only).
     */
    static byte[] message(byte[] payer, byte[] source, byte[] dest, byte[] receiver, byte[] mint, byte[] program,
                          long amount, int decimals, boolean create, byte[] blockhash) {
        List<byte[]> keys = new ArrayList<>();
        keys.add(payer);
        keys.add(source);
        keys.add(dest);
        if (create) keys.add(receiver);
        keys.add(mint);
        keys.add(program);
        if (create) { keys.add(ChainSend.b58(SYSTEM)); keys.add(ChainSend.b58(ATA)); }
        int readOnly = keys.size() - 3;
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        o.write(1);                                    // one signature
        o.write(0);                                    // no read-only signers
        o.write(readOnly);
        compact(o, keys.size());
        for (byte[] k : keys) o.write(k, 0, 32);
        o.write(blockhash, 0, 32);
        compact(o, create ? 2 : 1);
        if (create) {
            o.write(index(keys, ChainSend.b58(ATA)));
            byte[] acc = {0, (byte) index(keys, dest), (byte) index(keys, receiver), (byte) index(keys, mint),
                    (byte) index(keys, ChainSend.b58(SYSTEM)), (byte) index(keys, program)};
            compact(o, acc.length);
            o.write(acc, 0, acc.length);
            compact(o, 1);
            o.write(1);                                // CreateIdempotent
        }
        o.write(index(keys, program));
        byte[] acc = {(byte) index(keys, source), (byte) index(keys, mint), (byte) index(keys, dest), 0};
        compact(o, acc.length);
        o.write(acc, 0, acc.length);
        compact(o, 10);
        o.write(12);                                   // TransferChecked
        for (int i = 0; i < 8; i++) o.write((int) (amount >>> (8 * i)) & 0xFF);
        o.write(decimals);
        return o.toByteArray();
    }

    private static int index(List<byte[]> keys, byte[] k) {
        for (int i = 0; i < keys.size(); i++) if (Arrays.equals(keys.get(i), k)) return i;
        throw new IllegalStateException("account");
    }

    private static void compact(ByteArrayOutputStream o, int v) {
        while (true) {
            int b = v & 0x7F;
            v >>>= 7;
            if (v == 0) { o.write(b); return; }
            o.write(b | 0x80);
        }
    }

    static JSONObject call(String rpc, String method, JSONArray params) throws Exception {
        String body = new JSONObject().put("jsonrpc", "2.0").put("id", 1).put("method", method).put("params", params).toString();
        Request req = new Request.Builder().url(rpc).post(RequestBody.create(body, JSON)).build();
        try (Response r = HTTP.newCall(req).execute()) {
            String text = r.body() == null ? "{}" : r.body().string();
            return new JSONObject(text.isEmpty() ? "{}" : text);
        }
    }
}
