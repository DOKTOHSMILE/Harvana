package com.harvana.app.wallet;

import android.content.Context;
import android.util.Base64;

import com.harvana.app.crypto.Ed25519;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * The Solana side of the dApp browser: the active wallet's Solana account (m/44'/501'/0'/0'), message signing and
 * transaction signing. A transaction comes from the page already built (legacy or version 0); this class reads it to
 * show what it does, signs it in the slot that belongs to this account, and can send it. The key is derived for one
 * signature and wiped; the page only receives the account, signatures and the signed bytes.
 */
public final class SolSigner {
    private SolSigner() {}

    private static final OkHttpClient HTTP = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build();
    private static final MediaType JSON = MediaType.parse("application/json");

    public static final String SYSTEM = "11111111111111111111111111111111";
    private static final String[][] PROGRAMS = {
            {SYSTEM, "System"},
            {"TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA", "Token"},
            {"TokenzQdBNbLqP5VEhdkAS6EPFLC1PHnBqCXEpPxuEb", "Token-2022"},
            {"ATokenGPvbdGVxr1b2hvZbsiqW5xWH25efTNsLJA8knL", "Associated token"},
            {"ComputeBudget111111111111111111111111111111", "Compute budget"},
            {"MemoSq4gqABAXKb96qnH8TysNcWxMyWCqXgDLGmfcHr", "Memo"},
            {"Memo1UhkJRfHyvLMcVucJwxXeuD728EqVDDwQDxFMNo", "Memo"},
            {"JUP6LkbZbjS1jKKwapdHNy74zcZ3tLUZoi5QNyVTaV4", "Jupiter"},
            {"whirLbMiicVdio4qvUfM5KAg6Ct8VwpYzGff3uctyCc", "Orca Whirlpool"},
            {"675kPX9MHTjS2zt1qfr1NYHuzeLXfQM9H24wFSUt1Mp8", "Raydium AMM"},
            {"CAMMCzo5YL8w4VFF8KVHrK22GGUsp5VTaW7grrKgrWqK", "Raydium CLMM"},
            {"metaqbxxUerdq28cj1RbAWkYQm3ybzjb6a8bt518x1s", "Metaplex"},
    };

    /** "solana:devnet" on test, "solana:mainnet" on main (Wallet Standard chain ids). */
    public static String chain() { return Net.TEST ? "solana:devnet" : "solana:mainnet"; }
    public static String networkName() { return Net.TEST ? "Solana Devnet" : "Solana"; }

    public static String address(Context c) throws Exception {
        String m = SecureWalletStorage.getActiveMnemonic(c.getApplicationContext());
        if (m == null || m.trim().isEmpty()) throw new IllegalStateException("NO_PHRASE");
        return ChainKeys.address(m, ChainKeys.Chain.SOL);
    }

    public static byte[] publicKey(String address) { return ChainSend.b58(address); }

    /** A base58 string (an address or a transaction signature) as bytes. */
    public static byte[] base58(String s) { return ChainSend.b58(s); }

    private static byte[] key(Context c, String expect) throws Exception {
        String m = SecureWalletStorage.getActiveMnemonic(c.getApplicationContext());
        if (m == null || m.trim().isEmpty()) throw new IllegalStateException("NO_PHRASE");
        if (!ChainKeys.address(m, ChainKeys.Chain.SOL).equals(expect)) throw new IllegalStateException("WRONG_ACCOUNT");
        return ChainKeys.privateKey(m, ChainKeys.Chain.SOL);
    }

    /** signMessage: a plain Ed25519 signature over the bytes, base64. */
    public static String signMessage(Context c, String account, byte[] message) throws Exception {
        if (looksLikeTransaction(message)) throw new IllegalArgumentException("That message is a transaction.");
        byte[] seed = key(c, account);
        try { return Base64.encodeToString(Ed25519.sign(seed, message), Base64.NO_WRAP); }
        finally { Arrays.fill(seed, (byte) 0); }
    }

    /** Signs a serialized transaction in this account's slot and returns the whole transaction, base64. */
    public static String signTransaction(Context c, String account, byte[] tx) throws Exception {
        Parsed p = parse(tx);
        int slot = p.slotOf(account);
        if (slot < 0) throw new IllegalArgumentException("This account does not sign that transaction.");
        byte[] seed = key(c, account);
        try {
            byte[] sig = Ed25519.sign(seed, p.message);
            byte[] out = tx.clone();
            System.arraycopy(sig, 0, out, p.sigOffset + 64 * slot, 64);
            return Base64.encodeToString(out, Base64.NO_WRAP);
        } finally { Arrays.fill(seed, (byte) 0); }
    }

    /** Signs, sends, and returns the transaction signature (base58), which is its id on Solana. */
    public static String signAndSend(Context c, String account, byte[] tx, boolean skipPreflight) throws Exception {
        String signed = signTransaction(c, account, tx);
        JSONObject opts = new JSONObject().put("encoding", "base64").put("skipPreflight", skipPreflight).put("preflightCommitment", "confirmed");
        JSONObject body = new JSONObject().put("jsonrpc", "2.0").put("id", 1).put("method", "sendTransaction")
                .put("params", new JSONArray().put(signed).put(opts));
        Request req = new Request.Builder().url(Net.solRpc()).post(RequestBody.create(body.toString(), JSON)).build();
        try (Response r = HTTP.newCall(req).execute()) {
            String text = r.body() == null ? "{}" : r.body().string();
            JSONObject j = new JSONObject(text.isEmpty() ? "{}" : text);
            if (j.has("error")) throw new IllegalArgumentException(j.getJSONObject("error").optString("message", "Solana rejected the transaction."));
            if (!j.has("result")) throw new IllegalArgumentException("Solana did not answer.");
            return j.getString("result");
        }
    }

    /** What the review sheet shows: {label, value} rows. */
    public static List<String[]> describe(byte[] tx, String account) {
        Parsed p = parse(tx);
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Version", p.version < 0 ? "Legacy" : "v" + p.version});
        rows.add(new String[]{"Fee payer", p.keys.length == 0 ? "—" : (p.keys[0].equals(account) ? "This account" : shortAddr(p.keys[0]))});
        Set<String> programs = new LinkedHashSet<>();
        long out = lamportsOut(tx, account), unitPrice = 0, units = 200_000L * Math.max(1, p.instructions.size());
        boolean unitsSet = false;
        for (Ix ix : p.instructions) {
            String prog = ix.program < p.keys.length ? p.keys[ix.program] : "?";
            programs.add(name(prog));
            if ("ComputeBudget111111111111111111111111111111".equals(prog) && ix.data.length >= 1) {
                if (ix.data[0] == 2 && ix.data.length >= 5) { units = le32(ix.data, 1) & 0xFFFFFFFFL; unitsSet = true; }
                if (ix.data[0] == 3 && ix.data.length >= 9) unitPrice = le64(ix.data, 1);
            }
        }
        if (!unitsSet) units = Math.min(units, 1_400_000L);
        long fee = 5000L * p.required + (unitPrice * units + 999_999L) / 1_000_000L;
        if (out > 0) rows.add(new String[]{"SOL out", sol(out) + " SOL"});
        rows.add(new String[]{"Programs", String.join(", ", programs)});
        rows.add(new String[]{"Instructions", String.valueOf(p.instructions.size())});
        rows.add(new String[]{"Signers", String.valueOf(p.required)});
        if (p.lookups > 0) rows.add(new String[]{"Lookup tables", String.valueOf(p.lookups)});
        rows.add(new String[]{"Network fee", sol(fee) + " SOL"});
        return rows;
    }

    /** SOL leaving this account through System transfers, in lamports. */
    public static long lamportsOut(byte[] tx, String account) {
        Parsed p = parse(tx);
        long out = 0;
        for (Ix ix : p.instructions) {
            String prog = ix.program < p.keys.length ? p.keys[ix.program] : "";
            if (SYSTEM.equals(prog) && ix.data.length >= 12 && le32(ix.data, 0) == 2 && ix.accounts.length >= 1
                    && ix.accounts[0] < p.keys.length && p.keys[ix.accounts[0]].equals(account)) out += le64(ix.data, 4);
        }
        return out;
    }

    public static String sol(long lamports) {
        java.math.BigDecimal v = java.math.BigDecimal.valueOf(lamports, 9).stripTrailingZeros();
        return v.scale() < 0 ? v.setScale(0).toPlainString() : v.toPlainString();
    }

    // ── reading a transaction ──────────────────────────────────────────────────

    private static final class Ix {
        int program; int[] accounts; byte[] data;
    }

    private static final class Parsed {
        int sigOffset, sigCount, required, version = -1, lookups;
        byte[] message;
        String[] keys;
        List<Ix> instructions = new ArrayList<>();

        int slotOf(String account) {
            for (int i = 0; i < Math.min(required, sigCount); i++) if (keys[i].equals(account)) return i;
            return -1;
        }
    }

    private static Parsed parse(byte[] tx) {
        try {
            Parsed p = new Parsed();
            int[] at = {0};
            p.sigCount = cu16(tx, at);
            p.sigOffset = at[0];
            at[0] += 64 * p.sigCount;
            int msgStart = at[0];
            int first = tx[at[0]] & 0xFF;
            if ((first & 0x80) != 0) {
                p.version = first & 0x7F;
                if (p.version != 0) throw new IllegalArgumentException("Unsupported transaction version.");
                at[0]++;
            }
            p.required = tx[at[0]++] & 0xFF;
            at[0] += 2;                                                   // read-only signed and unsigned counts
            int n = cu16(tx, at);
            p.keys = new String[n];
            for (int i = 0; i < n; i++) { p.keys[i] = ChainKeys.base58(Arrays.copyOfRange(tx, at[0], at[0] + 32)); at[0] += 32; }
            at[0] += 32;                                                  // recent blockhash
            int ixCount = cu16(tx, at);
            for (int i = 0; i < ixCount; i++) {
                Ix ix = new Ix();
                ix.program = tx[at[0]++] & 0xFF;
                int ac = cu16(tx, at);
                ix.accounts = new int[ac];
                for (int k = 0; k < ac; k++) ix.accounts[k] = tx[at[0]++] & 0xFF;
                int dl = cu16(tx, at);
                ix.data = Arrays.copyOfRange(tx, at[0], at[0] + dl);
                at[0] += dl;
                p.instructions.add(ix);
            }
            if (p.version == 0) {
                p.lookups = cu16(tx, at);
                for (int i = 0; i < p.lookups; i++) {
                    at[0] += 32;
                    int writable = cu16(tx, at);
                    at[0] += writable;
                    int readOnly = cu16(tx, at);
                    at[0] += readOnly;
                }
            }
            if (at[0] != tx.length || p.sigCount != p.required || p.required > n) throw new IllegalArgumentException("Bad transaction.");
            p.message = Arrays.copyOfRange(tx, msgStart, tx.length);
            return p;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Bad transaction.");
        }
    }

    /**
     * True when the bytes read as a whole transaction or as a bare transaction message: a signature over either would
     * authorise that transaction, so signMessage refuses them.
     */
    private static boolean looksLikeTransaction(byte[] b) {
        try { parse(b); return true; } catch (Exception ignored) { }
        try {
            if (b.length < 4) return false;
            int required = (b[0] & 0x80) != 0 ? b[1] & 0xFF : b[0] & 0xFF;
            if (required == 0 || required > 127) return false;
            byte[] tx = new byte[1 + 64 * required + b.length];
            tx[0] = (byte) required;
            System.arraycopy(b, 0, tx, 1 + 64 * required, b.length);
            parse(tx);
            return true;
        } catch (Exception e) { return false; }
    }

    private static int cu16(byte[] b, int[] at) {
        int v = 0;
        for (int i = 0; i < 3; i++) {
            int x = b[at[0]++] & 0xFF;
            v |= (x & 0x7F) << (7 * i);
            if ((x & 0x80) == 0) return v;
        }
        throw new IllegalArgumentException("Bad transaction.");
    }

    private static long le32(byte[] b, int o) {
        return (b[o] & 0xFFL) | (b[o + 1] & 0xFFL) << 8 | (b[o + 2] & 0xFFL) << 16 | (b[o + 3] & 0xFFL) << 24;
    }

    private static long le64(byte[] b, int o) { return le32(b, o) | le32(b, o + 4) << 32; }

    private static String name(String program) {
        for (String[] p : PROGRAMS) if (p[0].equals(program)) return p[1];
        return shortAddr(program);
    }

    private static String shortAddr(String a) { return a == null || a.length() < 12 ? String.valueOf(a) : a.substring(0, 4) + "…" + a.substring(a.length() - 4); }
}
