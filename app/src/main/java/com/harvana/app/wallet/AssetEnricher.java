package com.harvana.app.wallet;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.regex.Pattern;

/**
 * Makes an added chain or token behave like a built-in coin: after it is added (and at start, once a day per asset) the
 * logo, the CoinGecko id (price, chart, description) and the Blockscout history source are looked up when missing.
 * Only empty fields are filled; anything the person typed stays. Off the main thread.
 */
public final class AssetEnricher {
    private AssetEnricher() {}

    private static final String PREFS = "asset_enrich";
    private static final long EVERY_MS = 24L * 60 * 60 * 1000;
    /** Test networks: their coins have no market price, so only the logo is looked up. */
    private static final Pattern TESTNET = Pattern.compile("(?i)test|sepolia|goerli|holesky|hoodi|devnet|amoy|mumbai|fuji|chapel|nile|shasta|alfajores|chiado");

    /** Every added asset that has not been checked in the last day. */
    public static void all(Context c) {
        if (c == null) return;
        Context app = c.getApplicationContext();
        new Thread(() -> {
            for (CustomAssets.Asset a : CustomAssets.all(app)) {
                if (!due(app, a.key())) continue;
                fill(app, a.copy());
            }
        }, "asset-enrich").start();
    }

    /** One asset, right after it was added or edited. */
    public static void enrich(Context c, String key) {
        if (c == null || key == null) return;
        Context app = c.getApplicationContext();
        new Thread(() -> {
            CustomAssets.Asset a = CustomAssets.find(key);
            if (a != null) fill(app, a.copy());
        }, "asset-enrich").start();
    }

    private static boolean due(Context c, String key) {
        return System.currentTimeMillis() - c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(key, 0) > EVERY_MS;
    }

    public static boolean testnet(CustomAssets.Asset a) {
        if (Net.TEST) return true;
        String name = a.chainName == null || a.chainName.isEmpty() ? a.name : a.chainName;
        return name != null && TESTNET.matcher(name).find();
    }

    private static void fill(Context c, CustomAssets.Asset a) {
        boolean changed = false;
        try {
            if (a.near()) return;                                          // NEAR assets are filled by NearProtocol
            boolean test = testnet(a);
            if (!a.token() && "EVM".equals(a.family)) {
                JSONObject reg = null;
                try { reg = CustomAssets.getJson(CustomAssets.REGISTRY + "chains/eip155-" + a.chainId + ".json"); } catch (Exception ignored) { }
                if (empty(a.icon)) {
                    String icon = reg == null ? "" : CustomAssets.chainIcon(reg.optString("icon", ""));
                    if (empty(icon)) icon = MultiCoinEngine.resolveChainIcon(a.chainId, "");
                    if (empty(icon)) icon = searchCoin(a.symbol)[1];
                    if (!empty(icon)) { a.icon = icon; changed = true; }
                }
                if (empty(a.geckoId) && !test) {
                    JSONObject p = CustomAssets.platform(a.chainId);
                    String id = p == null ? "" : p.optString("native_coin_id", "");
                    if (empty(id)) id = searchCoin(a.symbol)[0];
                    if (!empty(id)) { a.geckoId = id; changed = true; }
                }
                if (empty(a.scout)) {
                    String scout = blockscoutFor(a.explorer, reg);
                    if (!empty(scout)) { a.scout = scout; changed = true; }
                }
            } else if (a.token() && "EVM".equals(a.family)) {
                if (empty(a.icon) || (empty(a.geckoId) && !test)) {
                    String[] g = test ? new String[]{"", ""} : CustomAssets.geckoToken(a.chainId, a.contract);
                    if (empty(g[0]) && empty(g[1])) g = searchCoin(a.symbol);   // a test token borrows its mainnet logo
                    if (empty(a.icon) && !empty(g[1])) { a.icon = g[1]; changed = true; }
                    if (empty(a.geckoId) && !test && !empty(g[0])) { a.geckoId = g[0]; changed = true; }
                }
                if (empty(a.scout)) {
                    CustomAssets.Asset chain = CustomAssets.find(a.chainKey);
                    String scout = chain != null && !empty(chain.scout) ? chain.scout : blockscoutFor(a.explorer, null);
                    if (!empty(scout)) { a.scout = scout; changed = true; }
                }
            }
        } catch (Exception ignored) {
        } finally {
            c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putLong(a.key(), System.currentTimeMillis()).apply();
            if (changed && CustomAssets.update(c, a)) AssetPrices.forget(c);   // the next price call includes the new id
        }
    }

    /** The asset's explorer when it is a Blockscout, else a Blockscout listed for the chain in the registry. */
    private static String blockscoutFor(String explorer, JSONObject reg) {
        if (!empty(explorer) && CustomAssets.isBlockscout(explorer)) return explorer;
        JSONArray ex = reg == null ? null : reg.optJSONArray("explorers");
        for (int i = 0; ex != null && i < ex.length(); i++) {
            JSONObject e = ex.optJSONObject(i);
            if (e == null) continue;
            String url = e.optString("url", "");
            while (url.endsWith("/")) url = url.substring(0, url.length() - 1);
            if (url.isEmpty() || url.equals(explorer)) continue;
            if (CustomAssets.isBlockscout(url)) return url;
        }
        return "";
    }

    /**
     * CoinGecko's {id, logo} for a ticker: the exact-symbol match with the best market-cap rank among the top 1,000, so a
     * look-alike ticker does not get a price. {"", ""} when none.
     */
    static String[] searchCoin(String symbol) {
        if (empty(symbol)) return new String[]{"", ""};
        try {
            JSONObject j = CustomAssets.getJson("https://api.coingecko.com/api/v3/search?query=" + java.net.URLEncoder.encode(symbol, "UTF-8"));
            JSONArray coins = j.optJSONArray("coins");
            String bestId = "", bestIcon = "";
            int bestRank = Integer.MAX_VALUE;
            for (int i = 0; coins != null && i < coins.length(); i++) {
                JSONObject o = coins.optJSONObject(i);
                if (o == null || !symbol.equalsIgnoreCase(o.optString("symbol"))) continue;
                int rank = o.isNull("market_cap_rank") ? Integer.MAX_VALUE : o.optInt("market_cap_rank", Integer.MAX_VALUE);
                if (rank < bestRank && rank <= 1000) {
                    bestRank = rank;
                    bestId = o.optString("id", "");
                    bestIcon = o.optString("large", o.optString("thumb", ""));
                }
            }
            return new String[]{bestId, bestIcon};
        } catch (Exception e) {
            return new String[]{"", ""};
        }
    }

    private static boolean empty(String s) { return s == null || s.trim().isEmpty(); }

}
