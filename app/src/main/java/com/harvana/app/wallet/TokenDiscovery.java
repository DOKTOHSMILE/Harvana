package com.harvana.app.wallet;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Tokens the wallet holds but has not listed are added to Assets by themselves, once. Removed ones never come back;
 * hidden ones stay hidden (the switch in Add coins). Where each chain's holdings are read:
 *  - EVM chains with a Blockscout explorer (built-in or added): its token list for the address;
 *  - any EVM chain, with a GoldRush (Covalent) key the person pasted in Add coins: its balances list (BNB Chain,
 *    Polygon and Base have no keyless index);
 *  - Tron: the account's TRC-20 list; Solana: the owner's token accounts (Token and Token-2022);
 *  - NEAR chains with a history API: the account's inventory.
 * Runs at most every 10 minutes per wallet, off the main thread.
 */
public final class TokenDiscovery {
    private TokenDiscovery() {}

    private static final String PREFS = "token_discovery";
    private static final long EVERY_MS = 10 * 60_000L;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final OkHttpClient HTTP = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).build();
    private static volatile boolean running;

    public static String indexKey(Context c) { return prefs(c).getString("goldrush_key", ""); }
    public static void setIndexKey(Context c, String key) { prefs(c).edit().putString("goldrush_key", key == null ? "" : key.trim()).apply(); }

    /** A token the person removed: never added again by discovery. */
    static void forget(Context c, String key) {
        Set<String> s = new HashSet<>(prefs(c).getStringSet("removed", new HashSet<>()));
        s.add(key);
        prefs(c).edit().putStringSet("removed", s).apply();
    }

    /** Looks for new tokens; {@code onAdded} runs on the main thread when at least one was added. */
    public static void run(Context c, String harvAddress, Runnable onAdded) {
        if (c == null || harvAddress == null || harvAddress.isEmpty() || running) return;
        Context app = c.getApplicationContext();
        SharedPreferences p = prefs(app);
        if (System.currentTimeMillis() - p.getLong("at:" + harvAddress, 0) < EVERY_MS) return;
        running = true;
        new Thread(() -> {
            int added = 0;
            try {
                p.edit().putLong("at:" + harvAddress, System.currentTimeMillis()).apply();
                List<CustomAssets.Asset> found = new ArrayList<>();
                String eth = CoinCatalog.addresses(app, harvAddress).get("ETH");
                String key = indexKey(app);
                for (CustomAssets.Asset ch : CustomAssets.searchableChains(app)) {
                    if (!"EVM".equals(ch.family) || eth == null || eth.isEmpty()) continue;
                    if (!ch.scout.isEmpty()) blockscout(ch, eth, found);
                    else if (!key.isEmpty()) goldrush(ch, eth, key, found);
                }
                String trx = CoinCatalog.savedAddress(app, harvAddress, "TRX");
                if (trx != null && !trx.isEmpty()) tron(trx, found);
                String sol = CoinCatalog.savedAddress(app, harvAddress, "SOL");
                if (sol != null && !sol.isEmpty()) solana(sol, found);
                for (CustomAssets.Asset ch : CustomAssets.cached()) {
                    if (!ch.near() || ch.token() || ch.historyApi.isEmpty()) continue;
                    String id = CustomAssets.address(app, harvAddress, ch);
                    if (!id.isEmpty()) near(ch, id, found);
                }
                Set<String> removed = p.getStringSet("removed", new HashSet<>());
                for (CustomAssets.Asset a : found) {
                    if (a == null || CustomAssets.find(a.key()) != null || removed.contains(a.key())) continue;
                    CustomAssets.add(app, a);
                    added++;
                }
            } catch (Exception ignored) {
            } finally {
                running = false;
            }
            if (added > 0 && onAdded != null) MAIN.post(onAdded);
        }, "token-discovery").start();
    }

    private static void blockscout(CustomAssets.Asset ch, String owner, List<CustomAssets.Asset> out) {
        try {
            JSONObject j = get(ch.scout + "/api/v2/addresses/" + owner + "/tokens?type=ERC-20");
            JSONArray items = j.optJSONArray("items");
            for (int i = 0; items != null && i < items.length(); i++) {
                JSONObject it = items.getJSONObject(i), t = it.optJSONObject("token");
                if (t == null || new BigInteger(it.optString("value", "0")).signum() <= 0) continue;
                if ("spam".equalsIgnoreCase(t.optString("reputation"))) continue;
                String contract = t.optString("address_hash", t.optString("address"));
                out.add(evm(ch, contract, t.optString("symbol"), t.optString("name"), t.optInt("decimals", 18), t.optString("icon_url", "")));
            }
        } catch (Exception ignored) { }
    }

    private static void goldrush(CustomAssets.Asset ch, String owner, String key, List<CustomAssets.Asset> out) {
        try {
            JSONObject j = get("https://api.covalenthq.com/v1/" + ch.chainId + "/address/" + owner + "/balances_v2/?no-spam=true&key=" + key);
            JSONObject data = j.optJSONObject("data");
            JSONArray items = data == null ? null : data.optJSONArray("items");
            for (int i = 0; items != null && i < items.length(); i++) {
                JSONObject t = items.getJSONObject(i);
                if (t.optBoolean("native_token") || !"cryptocurrency".equals(t.optString("type", "cryptocurrency"))) continue;
                if (new BigInteger(t.optString("balance", "0")).signum() <= 0) continue;
                out.add(evm(ch, t.optString("contract_address"), t.optString("contract_ticker_symbol"), t.optString("contract_name"),
                        t.optInt("contract_decimals", 18), t.optString("logo_url", "")));
            }
        } catch (Exception ignored) { }
    }

    private static CustomAssets.Asset evm(CustomAssets.Asset ch, String contract, String symbol, String name, int decimals, String icon) {
        if (contract == null || !contract.matches("^0x[0-9a-fA-F]{40}$") || symbol == null || symbol.isEmpty()) return null;
        CustomAssets.Asset a = new CustomAssets.Asset();
        a.kind = "TOKEN";
        a.family = "EVM";
        a.chainId = ch.chainId;
        a.chainKey = "C:" + ch.chainId;
        a.chainName = ch.chainName;
        a.rpc = ch.rpc;
        a.explorer = ch.explorer;
        a.addressUrl = ch.addressUrl;
        a.txUrl = ch.txUrl;
        a.scout = ch.scout;
        a.contract = contract;
        a.symbol = symbol;
        a.name = name == null || name.isEmpty() ? symbol : name;
        a.decimals = decimals;
        a.icon = icon == null ? "" : icon;
        return a;
    }

    private static void tron(String owner, List<CustomAssets.Asset> out) {
        try {
            JSONObject j = get(Net.tronApi() + "/v1/accounts/" + owner);
            JSONArray data = j.optJSONArray("data");
            JSONArray trc20 = data == null || data.length() == 0 ? null : data.getJSONObject(0).optJSONArray("trc20");
            for (int i = 0; trc20 != null && i < trc20.length(); i++) {
                JSONObject t = trc20.getJSONObject(i);
                for (java.util.Iterator<String> it = t.keys(); it.hasNext(); ) {
                    String contract = it.next();
                    if (new BigInteger(t.optString(contract, "0")).signum() <= 0) continue;
                    if (CustomAssets.find("T:TRX:" + contract) != null) continue;
                    out.add(TokenScan.tron(contract));
                }
            }
        } catch (Exception ignored) { }
    }

    private static void solana(String owner, List<CustomAssets.Asset> out) {
        for (String program : new String[]{SplToken.TOKEN, SplToken.TOKEN_2022}) {
            try {
                JSONObject body = new JSONObject().put("jsonrpc", "2.0").put("id", 1).put("method", "getTokenAccountsByOwner")
                        .put("params", new JSONArray().put(owner).put(new JSONObject().put("programId", program)).put(new JSONObject().put("encoding", "jsonParsed")));
                Request req = new Request.Builder().url(Net.solRpc()).post(okhttp3.RequestBody.create(body.toString(), okhttp3.MediaType.parse("application/json"))).build();
                JSONObject j;
                try (Response r = HTTP.newCall(req).execute()) { j = new JSONObject(r.body() == null ? "{}" : r.body().string()); }
                JSONObject res = j.optJSONObject("result");
                JSONArray value = res == null ? null : res.optJSONArray("value");
                for (int i = 0; value != null && i < value.length(); i++) {
                    JSONObject info = value.getJSONObject(i).getJSONObject("account").getJSONObject("data").getJSONObject("parsed").getJSONObject("info");
                    String mint = info.optString("mint");
                    JSONObject amt = info.optJSONObject("tokenAmount");
                    if (amt == null || new BigInteger(amt.optString("amount", "0")).signum() <= 0) continue;
                    if (CustomAssets.find("T:SOL:" + mint) != null) continue;
                    out.add(TokenScan.solana(mint));
                }
            } catch (Exception ignored) { }
        }
    }

    private static void near(CustomAssets.Asset ch, String id, List<CustomAssets.Asset> out) {
        try {
            JSONObject j = get(ch.historyApi + "/v1/account/" + id + "/inventory");
            JSONObject inv = j.optJSONObject("inventory");
            JSONArray fts = inv == null ? null : inv.optJSONArray("fts");
            for (int i = 0; fts != null && i < fts.length(); i++) {
                JSONObject t = fts.getJSONObject(i);
                if (new java.math.BigDecimal(t.optString("amount", "0")).signum() <= 0) continue;
                String contract = t.optString("contract").toLowerCase(Locale.US);
                if (CustomAssets.find("T:" + ch.key() + ":" + contract) != null) continue;
                out.add(TokenScan.near(ch, contract));
            }
        } catch (Exception ignored) { }
    }

    private static JSONObject get(String url) throws Exception {
        try (Response r = HTTP.newCall(new Request.Builder().url(url).get().build()).execute()) {
            String text = r.body() == null ? "{}" : r.body().string();
            return new JSONObject(text.isEmpty() || !text.trim().startsWith("{") ? "{}" : text);
        }
    }

    private static SharedPreferences prefs(Context c) { return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE); }
}
