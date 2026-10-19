package com.harvana.app.walletconnect;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.harvana.app.utils.ActivityTracker;
import com.harvana.app.utils.InfoTable;
import com.harvana.app.wallet.CustomAssets;
import com.harvana.app.wallet.Net;
import com.harvana.app.wallet.SolSigner;
import com.harvana.app.wallet.Web3Signer;

import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * What the person sees for WalletConnect: the connect sheet for a proposal, and the review sheet (then PIN or
 * fingerprint) for every signature or transaction a connected dApp asks for. Requests wait for an open screen.
 */
final class WcPrompts {
    private WcPrompts() {}

    static final int USER_REJECTED = 5000, UNSUPPORTED_METHOD = 5101, UNSUPPORTED_CHAINS = 5100, UNSUPPORTED_NAMESPACE = 5104;
    static final String[] SOL_MAINNET = {"5eykt4UsFv8P8NJdTREpY1vzqKqZKvdp", "4sGjMW1sUnHzSxGspuhpqLDx6wiyjNtZ"};
    static final String[] SOL_DEVNET = {"EtWTRABZaYq6iMfeYKouRu166VU2xqa1", "8E9rvCKLFQia2Y35HXjjpWzj8weVo44K"};

    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final List<Runnable> waiting = new ArrayList<>();
    private static boolean busy;

    private interface Show { void on(Activity a); }

    /** Runs on the open screen, one prompt at a time; holds it until a screen is open. */
    private static void onScreen(Show show) {
        Runnable r = () -> {
            Activity a = ActivityTracker.resumed();
            if (a == null || a.isFinishing()) { waiting.add(() -> onScreen(show)); return; }
            show.on(a);
        };
        if (busy) waiting.add(r); else { busy = true; r.run(); }
    }

    private static void next() {
        busy = false;
        if (!waiting.isEmpty()) { busy = true; waiting.remove(0).run(); }
    }

    /** A screen opened: shows what waited. */
    static void flush() { if (!busy && !waiting.isEmpty()) { busy = true; waiting.remove(0).run(); } }

    // ── proposal ────────────────────────────────────────────────────────

    static void proposal(Context app, WalletConnect.Proposal p) {
        IO.execute(() -> {
            String evm = null, sol = null;
            try { evm = Web3Signer.address(app); } catch (Exception ignored) { }
            try { sol = SolSigner.address(app); } catch (Exception ignored) { }
            final String evmAcc = evm, solAcc = sol;
            MAIN.post(() -> onScreen(a -> showProposal(a, p, evmAcc, solAcc)));
        });
    }

    private static void showProposal(Activity a, WalletConnect.Proposal p, String evm, String sol) {
        WalletConnect wc = WalletConnect.get(a);
        JSONObject ns;
        List<String> networks = new ArrayList<>();
        try {
            ns = namespaces(p.params, evm, sol, networks);
        } catch (Refused e) {
            wc.reject(p, e.code, e.getMessage());
            Toast.makeText(a, e.getMessage(), Toast.LENGTH_LONG).show();
            next();
            return;
        }
        JSONObject md = p.metadata();
        InfoTable t = new InfoTable(a);
        t.row("dApp", md.optString("name", "dApp"));
        t.row("Site", md.optString("url", ""));
        t.row("Networks", android.text.TextUtils.join(", ", networks));
        if (ns.has("eip155") && evm != null) t.row("EVM account", shortAddr(evm));
        if (ns.has("solana") && sol != null) t.row("Solana account", shortAddr(sol));
        final boolean[] done = {false};
        com.harvana.app.ui.SheetDialog d = new com.harvana.app.ui.SheetDialog.Builder(a)
                .setTitle("Connect to " + md.optString("name", "dApp"))
                .setView(t.view())
                .setNegativeButton("Cancel", (x, w) -> { done[0] = true; wc.reject(p, USER_REJECTED, "User rejected."); next(); })
                .setPositiveButton("Connect", (x, w) -> {
                    done[0] = true;
                    IO.execute(() -> {
                        try { wc.approve(p, ns); MAIN.post(() -> Toast.makeText(a, "Connected", Toast.LENGTH_SHORT).show()); }
                        catch (Exception e) { MAIN.post(() -> Toast.makeText(a, e.getMessage() == null ? "Could not connect" : e.getMessage(), Toast.LENGTH_LONG).show()); }
                    });
                    next();
                }).create();
        d.setOnDismissListener(x -> { if (!done[0]) { wc.reject(p, USER_REJECTED, "User rejected."); next(); } });
        d.show();
    }

    static final class Refused extends Exception {
        final int code;
        Refused(int code, String m) { super(m); this.code = code; }
    }

    /** The namespaces to approve: every requested EVM chain and Solana cluster this wallet signs for. */
    static JSONObject namespaces(JSONObject params, String evm, String sol, List<String> networkNames) throws Refused {
        Set<String> evmChains = new LinkedHashSet<>(), solChains = new LinkedHashSet<>();
        Set<String> evmMethods = new LinkedHashSet<>(), evmEvents = new LinkedHashSet<>(), solMethods = new LinkedHashSet<>(), solEvents = new LinkedHashSet<>();
        Set<String> requiredEvm = new LinkedHashSet<>(), requiredSol = new LinkedHashSet<>();
        for (int pass = 0; pass < 2; pass++) {
            boolean required = pass == 0;
            JSONObject obj = params.optJSONObject(required ? "requiredNamespaces" : "optionalNamespaces");
            if (obj == null) continue;
            for (java.util.Iterator<String> it = obj.keys(); it.hasNext(); ) {
                String key = it.next();
                JSONObject n = obj.optJSONObject(key);
                String space = key.contains(":") ? key.substring(0, key.indexOf(':')) : key;
                List<String> chains = new ArrayList<>();
                if (key.contains(":")) chains.add(key);
                JSONArray cs = n == null ? null : n.optJSONArray("chains");
                for (int i = 0; cs != null && i < cs.length(); i++) chains.add(cs.optString(i));
                if ("eip155".equals(space)) {
                    if (required) requiredEvm.addAll(chains);
                    evmChains.addAll(chains);
                    addAll(evmMethods, n, "methods");
                    addAll(evmEvents, n, "events");
                } else if ("solana".equals(space)) {
                    if (required) requiredSol.addAll(chains);
                    solChains.addAll(chains);
                    addAll(solMethods, n, "methods");
                    addAll(solEvents, n, "events");
                } else if (required) {
                    throw new Refused(UNSUPPORTED_NAMESPACE, "This dApp needs " + space + ", which Harvana does not sign for.");
                }
            }
        }
        JSONObject out = new JSONObject();
        try {
            if (!evmChains.isEmpty() && evm != null) {
                JSONArray chains = new JSONArray(), accounts = new JSONArray();
                for (String c : evmChains) {
                    long id = chainNumber(c);
                    String key = evmKey(id);
                    if (key == null) {
                        if (requiredEvm.contains(c)) throw new Refused(UNSUPPORTED_CHAINS, "Chain " + id + " is not in this wallet. Add it under Networks first.");
                        continue;
                    }
                    chains.put("eip155:" + id);
                    accounts.put("eip155:" + id + ":" + evm);
                    networkNames.add(Web3Signer.net(key).name);
                }
                if (chains.length() > 0) out.put("eip155", new JSONObject().put("chains", chains).put("accounts", accounts)
                        .put("methods", new JSONArray(evmMethods)).put("events", new JSONArray(evmEvents)));
            }
            if (!solChains.isEmpty() && sol != null) {
                String[] ours = Net.TEST ? SOL_DEVNET : SOL_MAINNET;
                JSONArray chains = new JSONArray(), accounts = new JSONArray();
                for (String c : solChains) {
                    String ref = c.substring(c.indexOf(':') + 1);
                    boolean mine = false;
                    for (String o : ours) if (o.equals(ref)) mine = true;
                    if (!mine) {
                        if (requiredSol.contains(c)) throw new Refused(UNSUPPORTED_CHAINS, "That Solana cluster is not the one this wallet uses (" + SolSigner.networkName() + ").");
                        continue;
                    }
                    chains.put(c);
                    accounts.put(c + ":" + sol);
                }
                if (chains.length() > 0) {
                    networkNames.add(SolSigner.networkName());
                    out.put("solana", new JSONObject().put("chains", chains).put("accounts", accounts)
                            .put("methods", new JSONArray(solMethods)).put("events", new JSONArray(solEvents)));
                }
            }
        } catch (Refused r) {
            throw r;
        } catch (Exception e) {
            throw new Refused(UNSUPPORTED_CHAINS, "Could not read what this dApp asks for.");
        }
        if (out.length() == 0) throw new Refused(UNSUPPORTED_CHAINS, "None of the networks this dApp asks for are in this wallet.");
        return out;
    }

    private static void addAll(Set<String> into, JSONObject n, String field) {
        JSONArray a = n == null ? null : n.optJSONArray(field);
        for (int i = 0; a != null && i < a.length(); i++) into.add(a.optString(i));
    }

    static long chainNumber(String caip2) {
        try { return Long.parseLong(caip2.substring(caip2.indexOf(':') + 1)); } catch (Exception e) { return -1; }
    }

    /** The wallet's key for an EVM chain id: a built-in chain, or one the person added; null when neither. */
    static String evmKey(long chainId) {
        if (chainId <= 0) return null;
        for (String k : Web3Signer.CHAINS) if (Web3Signer.net(k).chainId == chainId) return k;
        return CustomAssets.find("C:" + chainId) != null ? "C:" + chainId : null;
    }

    // ── requests ────────────────────────────────────────────────────────

    static void request(Context app, WalletConnect.Request r) {
        onScreen(a -> {
            try { handle(a, r); }
            catch (Exception e) { WalletConnect.get(a).error(r, -32602, "Invalid params."); next(); }
        });
    }

    private interface Work { Object run() throws Exception; }

    private static void handle(Activity a, WalletConnect.Request r) throws Exception {
        WalletConnect wc = WalletConnect.get(a);
        String site = r.session.url().isEmpty() ? r.session.name() : r.session.url();
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"dApp", r.session.name()});
        rows.add(new String[]{"Site", site});
        switch (r.method) {
            case "personal_sign": {
                String evm = Web3Signer.address(a);
                String p0 = r.arrayParams.optString(0, ""), p1 = r.arrayParams.optString(1, "");
                String data = p0.equalsIgnoreCase(evm) ? p1 : p0;
                byte[] bytes = data.startsWith("0x") ? Web3Signer.unhex(data) : data.getBytes(StandardCharsets.UTF_8);
                rows.add(new String[]{"Account", shortAddr(evm)});
                String shown = printable(bytes) ? new String(bytes, StandardCharsets.UTF_8) : "0x" + WcCrypto.hex(bytes);
                rows.add(new String[]{"Message", shown.length() > 1200 ? shown.substring(0, 1200) + "…" : shown});
                review(a, r, "Sign message", "", rows, () -> Web3Signer.personalSign(a, evm, bytes));
                return;
            }
            case "eth_signTypedData":
            case "eth_signTypedData_v3":
            case "eth_signTypedData_v4": {
                String evm = Web3Signer.address(a);
                Object p0 = r.arrayParams.opt(0), p1 = r.arrayParams.opt(1);
                Object raw = String.valueOf(p0).equalsIgnoreCase(evm) ? p1 : p0;
                JSONObject typed = raw instanceof JSONObject ? (JSONObject) raw : new JSONObject(String.valueOf(raw));
                JSONObject domain = typed.optJSONObject("domain");
                rows.add(new String[]{"Account", shortAddr(evm)});
                if (domain != null && domain.has("name")) rows.add(new String[]{"App", domain.optString("name")});
                if (domain != null && domain.has("verifyingContract")) rows.add(new String[]{"Contract", domain.optString("verifyingContract")});
                rows.add(new String[]{"Type", typed.optString("primaryType")});
                JSONObject msg = typed.optJSONObject("message");
                String body = msg == null ? "" : msg.toString(2);
                rows.add(new String[]{"Data", body.length() > 1500 ? body.substring(0, 1500) + "…" : body});
                review(a, r, "Sign data", "", rows, () -> Web3Signer.signTypedData(a, evm, typed));
                return;
            }
            case "eth_sendTransaction": {
                String evm = Web3Signer.address(a);
                JSONObject tx = r.arrayParams.getJSONObject(0);
                if (!tx.optString("from", evm).equalsIgnoreCase(evm)) { wc.error(r, 4100, "Unknown account."); next(); return; }
                String key = evmKey(chainNumber(r.chainId));
                if (key == null) { wc.error(r, UNSUPPORTED_CHAINS, "Chain not in this wallet."); next(); return; }
                Net.Evm n = Web3Signer.net(key);
                String sym = coin(key);
                IO.execute(() -> {
                    try {
                        long[] g = Web3Signer.estimate(n, evm, tx);
                        MAIN.post(() -> {
                            BigInteger value = tx.has("value") ? Web3Signer.qty(tx.optString("value")) : BigInteger.ZERO;
                            BigInteger fee = BigInteger.valueOf(g[0]).multiply(BigInteger.valueOf(g[1]));
                            String data = tx.optString("data", tx.optString("input", ""));
                            String to = tx.optString("to", "");
                            rows.add(new String[]{"To", to.isEmpty() ? "New contract" : to});
                            rows.add(new String[]{"Network", n.name});
                            rows.add(new String[]{"Function", data.length() >= 10 ? data.substring(0, 10) : "Transfer"});
                            rows.add(new String[]{"Network fee (max)", ether(fee) + " " + sym});
                            rows.add(new String[]{"Total (max)", ether(value.add(fee)) + " " + sym});
                            review(a, r, "Send transaction", ether(value) + " " + sym, rows, () -> Web3Signer.send(a, n, evm, tx, g[0], g[1]));
                        });
                    } catch (Exception e) {
                        MAIN.post(() -> { wc.error(r, -32603, "Could not estimate this transaction."); next(); });
                    }
                });
                return;
            }
            case "wallet_switchEthereumChain": {
                JSONObject p = r.arrayParams.optJSONObject(0);
                String c = p == null ? "" : p.optString("chainId", "");
                long id = c.startsWith("0x") ? Long.parseLong(c.substring(2), 16) : chainNumber("x:" + c);
                if (evmKey(id) == null) wc.error(r, 4902, "Unrecognized chain ID.");
                else wc.result(r, null);
                next();
                return;
            }
            case "solana_getAccounts":
            case "solana_requestAccounts": {
                String sol = SolSigner.address(a);
                wc.result(r, new JSONArray().put(new JSONObject().put("pubkey", sol)));
                next();
                return;
            }
            case "solana_signMessage": {
                String sol = SolSigner.address(a);
                byte[] msg = WcCrypto.unbase58(r.objectParams.getString("message"));
                rows.add(new String[]{"Account", shortAddr(sol)});
                rows.add(new String[]{"Network", SolSigner.networkName()});
                String shown = printable(msg) ? new String(msg, StandardCharsets.UTF_8) : "0x" + WcCrypto.hex(msg);
                rows.add(new String[]{"Message", shown.length() > 1200 ? shown.substring(0, 1200) + "…" : shown});
                review(a, r, "Sign message", "", rows, () -> new JSONObject().put("signature",
                        WcCrypto.base58(android.util.Base64.decode(SolSigner.signMessage(a, sol, msg), android.util.Base64.NO_WRAP))));
                return;
            }
            case "solana_signTransaction":
            case "solana_signAndSendTransaction": {
                String sol = SolSigner.address(a);
                byte[] tx = android.util.Base64.decode(r.objectParams.getString("transaction"), android.util.Base64.DEFAULT);
                rows.add(new String[]{"Account", shortAddr(sol)});
                rows.add(new String[]{"Network", SolSigner.networkName()});
                rows.addAll(SolSigner.describe(tx, sol));
                long out = SolSigner.lamportsOut(tx, sol);
                boolean send = "solana_signAndSendTransaction".equals(r.method);
                review(a, r, send ? "Send transaction" : "Sign transaction", out > 0 ? SolSigner.sol(out) + " SOL" : "", rows, () -> {
                    if (send) {
                        JSONObject o = r.objectParams.optJSONObject("sendOptions");
                        return new JSONObject().put("signature", SolSigner.signAndSend(a, sol, tx, o != null && o.optBoolean("skipPreflight")));
                    }
                    String signed = SolSigner.signTransaction(a, sol, tx);
                    byte[] s = android.util.Base64.decode(signed, android.util.Base64.NO_WRAP);
                    return new JSONObject().put("signature", WcCrypto.base58(ourSignature(tx, s))).put("transaction", signed);
                });
                return;
            }
            case "solana_signAllTransactions": {
                String sol = SolSigner.address(a);
                JSONArray list = r.objectParams.getJSONArray("transactions");
                if (list.length() == 0 || list.length() > 20) { wc.error(r, -32602, "Invalid params."); next(); return; }
                List<byte[]> txs = new ArrayList<>();
                long out = 0;
                for (int i = 0; i < list.length(); i++) {
                    byte[] tx = android.util.Base64.decode(list.getString(i), android.util.Base64.DEFAULT);
                    SolSigner.describe(tx, sol);
                    out += SolSigner.lamportsOut(tx, sol);
                    txs.add(tx);
                }
                rows.add(new String[]{"Account", shortAddr(sol)});
                rows.add(new String[]{"Transactions", String.valueOf(txs.size())});
                if (out > 0) rows.add(new String[]{"SOL out", SolSigner.sol(out) + " SOL"});
                review(a, r, "Sign transactions", out > 0 ? SolSigner.sol(out) + " SOL" : "", rows, () -> {
                    JSONArray signed = new JSONArray();
                    for (byte[] tx : txs) signed.put(SolSigner.signTransaction(a, sol, tx));
                    return new JSONObject().put("transactions", signed);
                });
                return;
            }
            case "eth_sign":
            case "eth_signTransaction":
            default:
                wc.error(r, UNSUPPORTED_METHOD, "Method not supported.");
                next();
        }
    }

    /** The review sheet, then PIN or fingerprint, then the work off the main thread; the answer goes to the dApp. */
    private static void review(Activity a, WalletConnect.Request r, String title, String amount, List<String[]> rows, Work work) {
        WalletConnect wc = WalletConnect.get(a);
        final boolean[] answered = {false};
        com.harvana.app.group.PaymentConfirmSheet.reviewThenAuthorize(a, title, amount, rows,
                () -> {
                    answered[0] = true;
                    IO.execute(() -> {
                        try {
                            Object res = work.run();
                            wc.result(r, res);
                            MAIN.post(WcPrompts::next);
                        } catch (Exception e) {
                            wc.error(r, -32603, e.getMessage() == null ? "Failed." : e.getMessage());
                            MAIN.post(() -> { Toast.makeText(a, com.harvana.app.utils.ErrorText.plain(e.getMessage()), Toast.LENGTH_LONG).show(); next(); });
                        }
                    });
                },
                () -> { if (!answered[0]) { answered[0] = true; wc.error(r, USER_REJECTED, "User rejected."); next(); } });
    }

    /** The 64-byte signature that changed between the unsigned and the signed transaction (this wallet's slot). */
    static byte[] ourSignature(byte[] before, byte[] after) {
        int n = before[0] & 0x7f, off = 1;                                   // compact-u16; a transaction has < 128 signers
        for (int i = 0; i < n; i++) {
            int at = off + 64 * i;
            for (int k = 0; k < 64; k++) {
                if (before[at + k] != after[at + k]) return java.util.Arrays.copyOfRange(after, at, at + 64);
            }
        }
        return java.util.Arrays.copyOfRange(after, off, off + 64);
    }

    private static String coin(String key) {
        if (key != null && key.startsWith("C:")) {
            CustomAssets.Asset c = CustomAssets.find(key);
            if (c != null && !c.symbol.isEmpty()) return c.symbol;
        }
        return "BNB".equals(key) ? "BNB" : "POL".equals(key) ? "POL" : "AVAX".equals(key) ? "AVAX" : "FTM".equals(key) ? "FTM"
                : "CRO".equals(key) ? "CRO" : "GNOSIS".equals(key) ? "xDAI" : "ETH";
    }

    private static String ether(BigInteger wei) { return new BigDecimal(wei).movePointLeft(18).stripTrailingZeros().toPlainString(); }

    static String shortAddr(String s) { return s == null || s.length() < 14 ? String.valueOf(s) : s.substring(0, 8) + "…" + s.substring(s.length() - 6); }

    private static boolean printable(byte[] b) {
        for (byte x : b) { int c = x & 0xff; if (c < 0x09 || (c > 0x0d && c < 0x20)) return false; }
        return true;
    }
}
