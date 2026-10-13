package com.harvana.app.wallet;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Finds a token by its contract address on every chain at once, the way Trust Wallet does: the address is tried on
 * each EVM chain (built-in and added), on Tron (TRC-20), on Solana (SPL mint) and on each added NEAR chain (NEP-141).
 * Every chain where it answers as a token is a result, with its name, symbol, decimals, icon and price id.
 */
public final class TokenScan {
    private TokenScan() {}

    public interface Found { void found(List<CustomAssets.Asset> tokens); }

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    /** True when the text has the shape of a token address on any chain the app speaks. */
    public static boolean looksLikeContract(String q) {
        if (q == null) return false;
        String t = q.trim();
        return t.matches("^0x[0-9a-fA-F]{40}$") || t.matches("^[1-9A-HJ-NP-Za-km-z]{32,44}$")
                || (t.contains(".") && NearProtocol.validAccount(t.toLowerCase(java.util.Locale.US)));
    }

    public static void search(Context c, String query, Found cb) {
        final String q = query == null ? "" : query.trim();
        final Context app = c.getApplicationContext();
        new Thread(() -> {
            List<Callable<CustomAssets.Asset>> jobs = new ArrayList<>();
            List<CustomAssets.Asset> chains = CustomAssets.searchableChains(app);
            if (q.matches("^0x[0-9a-fA-F]{40}$")) {
                for (CustomAssets.Asset ch : chains) if ("EVM".equals(ch.family)) jobs.add(() -> CustomAssets.readEvmToken(ch, q));
            }
            if (q.matches("^T[1-9A-HJ-NP-Za-km-z]{33}$")) jobs.add(() -> tron(q));
            if (q.matches("^[1-9A-HJ-NP-Za-km-z]{32,44}$")) jobs.add(() -> solana(q));
            String nearId = q.toLowerCase(java.util.Locale.US);
            if (NearProtocol.validAccount(nearId) && (nearId.contains(".") || nearId.matches("^[0-9a-f]{64}$"))) {
                for (CustomAssets.Asset ch : chains) if (ch.near()) jobs.add(() -> near(ch, nearId));
            }
            List<CustomAssets.Asset> out = new ArrayList<>();
            ExecutorService pool = Executors.newFixedThreadPool(Math.max(1, Math.min(6, jobs.size())));
            try {
                for (Future<CustomAssets.Asset> f : pool.invokeAll(jobs, 25, TimeUnit.SECONDS)) {
                    if (f.isCancelled()) continue;
                    try { CustomAssets.Asset a = f.get(); if (a != null) out.add(a); } catch (Exception ignored) { }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                pool.shutdownNow();
            }
            MAIN.post(() -> cb.found(out));
        }, "token-scan").start();
    }

    static CustomAssets.Asset tron(String contract) {
        String[] m = Trc20.read(contract);
        if (m == null) return null;
        CustomAssets.Asset a = base(Trc20.FAMILY, "TRX", contract);
        a.rpc = Net.tronApi();
        a.addressUrl = Net.tronAddressPage("{address}").replace("%7Baddress%7D", "{address}");
        a.txUrl = Net.tronTxPage("{tx}").replace("%7Btx%7D", "{tx}");
        a.name = m[0];
        a.symbol = m[1];
        a.decimals = Integer.parseInt(m[2]);
        if (!Net.TEST) { String[] g = CustomAssets.geckoToken("tron", contract); a.geckoId = g[0]; a.icon = g[1]; }
        return a;
    }

    static CustomAssets.Asset solana(String mint) {
        try {
            String[] m = SplToken.mint(Net.solRpc(), mint);
            if (m == null) return null;
            CustomAssets.Asset a = base(SplToken.FAMILY, "SOL", mint);
            a.rpc = Net.solRpc();
            a.addressUrl = Net.solAddressPage("{address}").replace("%7Baddress%7D", "{address}");
            a.txUrl = Net.solTxPage("{tx}").replace("%7Btx%7D", "{tx}");
            a.decimals = Integer.parseInt(m[0]);
            a.symbol = mint.substring(0, 4).toUpperCase(java.util.Locale.US);
            a.name = a.symbol;
            // Name, symbol and icon from Jupiter's token list (any listed mint), price id from CoinGecko.
            try {
                okhttp3.Request req = new okhttp3.Request.Builder().url("https://lite-api.jup.ag/tokens/v2/search?query=" + mint).get().build();
                try (okhttp3.Response r = new okhttp3.OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(12, TimeUnit.SECONDS).build().newCall(req).execute()) {
                    JSONArray list = new JSONArray(r.body() == null ? "[]" : r.body().string());
                    for (int i = 0; i < list.length(); i++) {
                        JSONObject t = list.optJSONObject(i);
                        if (t == null || !mint.equals(t.optString("id"))) continue;
                        if (!t.optString("symbol").isEmpty()) a.symbol = t.optString("symbol");
                        if (!t.optString("name").isEmpty()) a.name = t.optString("name");
                        a.icon = t.optString("icon", "");
                    }
                }
            } catch (Exception ignored) { }
            if (!Net.TEST) { String[] g = CustomAssets.geckoToken("solana", mint); a.geckoId = g[0]; if (a.icon.isEmpty()) a.icon = g[1]; }
            return a;
        } catch (Exception e) {
            return null;
        }
    }

    static CustomAssets.Asset near(CustomAssets.Asset chain, String id) {
        try {
            String[] m = NearProtocol.tokenMetadata(chain.rpc, id);
            CustomAssets.Asset a = base(NearProtocol.FAMILY, chain.key(), id);
            a.chainName = chain.chainName;
            a.rpc = chain.rpc;
            a.explorer = chain.explorer;
            a.addressUrl = chain.addressUrl;
            a.txUrl = chain.txUrl;
            a.name = m[0].isEmpty() ? m[1] : m[0];
            a.symbol = m[1];
            a.decimals = Integer.parseInt(m[2]);
            a.icon = m[3];
            return a;
        } catch (Exception e) {
            return null;
        }
    }

    private static CustomAssets.Asset base(String family, String chainKey, String contract) {
        CustomAssets.Asset a = new CustomAssets.Asset();
        a.kind = "TOKEN";
        a.family = family;
        a.chainKey = chainKey;
        a.contract = contract;
        CoinCatalog.Coin chain = CoinCatalog.get(chainKey);
        a.chainName = chain != null && chain.key.equals(chainKey) ? chain.liveNetwork() : "";
        return a;
    }
}
