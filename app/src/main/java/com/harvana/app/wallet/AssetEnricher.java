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

    /**
     * Every added asset that is due (a day after its last check; an hour when it still has no logo that loads), then
     * {@code onChanged} on the main thread if any of them changed.
     */
    public static void all(Context c, Runnable onChanged) {
        if (c == null) return;
        Context app = c.getApplicationContext();
        new Thread(() -> {
            boolean any = false;
            for (CustomAssets.Asset a : CustomAssets.all(app)) {
                if (!due(app, a.key())) continue;
                if (fill(app, a.copy())) any = true;
            }
            if (any && onChanged != null) new android.os.Handler(android.os.Looper.getMainLooper()).post(onChanged);
        }, "asset-enrich").start();
    }

    public static void all(Context c) { all(c, null); }

    /** A row could not show this asset's logo: look for another one at the next pass (once per run of the app). */
    public static void iconFailed(Context c, String key) {
        if (c == null || key == null || !failedThisRun.add(key)) return;
        c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(key).putBoolean("bad:" + key, true).apply();
    }

    private static final java.util.Set<String> failedThisRun = java.util.Collections.synchronizedSet(new java.util.HashSet<>());

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
        android.content.SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long every = p.getBoolean("noicon:" + key, false) ? 60L * 60 * 1000 : EVERY_MS;
        return System.currentTimeMillis() - p.getLong(key, 0) > every;
    }

    public static boolean testnet(CustomAssets.Asset a) {
        if (Net.TEST) return true;
        String name = a.chainName == null || a.chainName.isEmpty() ? a.name : a.chainName;
        return name != null && TESTNET.matcher(name).find();
    }

    /** Fills what is missing; true when the asset changed. */
    private static boolean fill(Context c, CustomAssets.Asset a) {
        boolean changed = false;
        android.content.SharedPreferences prefs = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        try {
            // The logo, for every kind of added coin (owner, 19 Oct 2026): a missing one, or one that does not load
            // (gone, an SVG the app cannot draw, an ipfs:// link), is replaced by the first source whose picture loads.
            boolean bad = prefs.getBoolean("bad:" + a.key(), false);
            if (empty(a.icon) || bad || !drawable(a.icon)) {
                String found = findLogo(a);
                if (!empty(found) && !found.equals(a.icon)) { a.icon = found; changed = true; }
            }
            prefs.edit().putBoolean("noicon:" + a.key(), empty(a.icon) || !drawable(a.icon)).remove("bad:" + a.key()).apply();
            if (a.near() || !"EVM".equals(a.family)) return changed;       // the rest below is for EVM chains and tokens
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
            prefs.edit().putLong(a.key(), System.currentTimeMillis()).apply();
            if (changed && CustomAssets.update(c, a)) AssetPrices.forget(c);   // the next price call includes the new id
        }
        return changed;
    }

    // ── logos ─────────────────────────────────────────────────────────────

    /** The first logo, from the sources for this kind of coin, whose picture really loads. "" when none does. */
    static String findLogo(CustomAssets.Asset a) {
        for (String url : logoSources(a)) {
            for (String u : gateways(https(url))) if (!empty(u) && loads(u)) return u;
        }
        return "";
    }

    /** Where a logo may be found, best first. */
    static java.util.List<String> logoSources(CustomAssets.Asset a) {
        java.util.List<String> out = new java.util.ArrayList<>();
        boolean test = testnet(a);
        String tw = "https://raw.githubusercontent.com/trustwallet/assets/master/blockchains/";
        try {
            if (a.token() && "EVM".equals(a.family)) {
                if (!test) out.add(CustomAssets.geckoToken(a.chainId, a.contract)[1]);
                out.add(dexScreener(a.contract));
                String slug = twSlug(a.chainId);
                if (!slug.isEmpty() && a.contract.matches("^0x[0-9a-fA-F]{40}$")) {
                    out.add(tw + slug + "/assets/" + com.harvana.app.husd.DepositAddress.checksum(com.harvana.app.husd.DepositAddress.hex(a.contract)) + "/logo.png");
                }
                if (test) out.add(searchCoin(a.symbol)[1]);                   // a test token borrows its mainnet logo
            } else if (a.token() && SplToken.FAMILY.equals(a.family)) {
                out.add(dexScreener(a.contract));                            // a CDN; Jupiter's are often ipfs links
                out.add(jupiter(a.contract));
                if (!test) out.add(CustomAssets.geckoToken("solana", a.contract)[1]);
                out.add(tw + "solana/assets/" + a.contract + "/logo.png");
            } else if (a.token() && Trc20.FAMILY.equals(a.family)) {
                if (!test) out.add(CustomAssets.geckoToken("tron", a.contract)[1]);
                out.add(tw + "tron/assets/" + a.contract + "/logo.png");
                out.add(dexScreener(a.contract));
            } else if (a.token() && a.near()) {
                out.add(a.icon);                                             // the token's own ft_metadata icon, when it is a picture
                if (!test) out.add(CustomAssets.geckoToken("near-protocol", a.contract)[1]);
                out.add(dexScreener(a.contract));
                out.add(tw + "near/assets/" + a.contract + "/logo.png");
            } else if (a.near()) {
                out.add(tw + "near/info/logo.png");
                out.add(searchCoin(a.symbol)[1]);
            } else if ("EVM".equals(a.family)) {
                JSONObject reg = null;
                try { reg = CustomAssets.getJson(CustomAssets.REGISTRY + "chains/eip155-" + a.chainId + ".json"); } catch (Exception ignored) { }
                if (reg != null) out.add(CustomAssets.chainIcon(reg.optString("icon", "")));
                out.add(MultiCoinEngine.resolveChainIcon(a.chainId, ""));
                out.add(searchCoin(a.symbol)[1]);
            } else {
                out.add(searchCoin(a.symbol)[1]);
            }
        } catch (Exception ignored) { }
        return out;
    }

    private static String twSlug(long chainId) {
        if (chainId == 1) return "ethereum";
        if (chainId == 56) return "smartchain";
        if (chainId == 137) return "polygon";
        if (chainId == 8453) return "base";
        if (chainId == 42161) return "arbitrum";
        if (chainId == 10) return "optimism";
        if (chainId == 43114) return "avalanchec";
        return "";
    }

    private static String dexScreener(String token) {
        try {
            JSONArray pairs = CustomAssets.getJson("https://api.dexscreener.com/latest/dex/tokens/" + token).optJSONArray("pairs");
            for (int i = 0; pairs != null && i < pairs.length(); i++) {
                JSONObject info = pairs.getJSONObject(i).optJSONObject("info");
                if (info != null && !info.optString("imageUrl", "").isEmpty()) return info.optString("imageUrl");
            }
        } catch (Exception ignored) { }
        return "";
    }

    private static String jupiter(String mint) {
        try (okhttp3.Response r = HTTP.newCall(new okhttp3.Request.Builder().url("https://lite-api.jup.ag/tokens/v2/search?query=" + mint).build()).execute()) {
            JSONArray list = new JSONArray(r.body() == null ? "[]" : r.body().string());
            for (int i = 0; i < list.length(); i++) {
                JSONObject t = list.optJSONObject(i);
                if (t != null && mint.equals(t.optString("id"))) return t.optString("icon", "");
            }
        } catch (Exception ignored) { }
        return "";
    }

    /** ipfs:// through a public gateway; anything else as it is. */
    static String https(String url) {
        if (empty(url)) return "";
        String u = url.trim();
        if (u.startsWith("ipfs://")) u = "https://ipfs.io/ipfs/" + u.substring(7).replaceFirst("^ipfs/", "");
        return u;
    }

    /**
     * An IPFS picture through more than one public gateway: ipfs.io refuses busy callers ("too many requests"), which
     * is why some added tokens (PUMP's listed logo is on ipfs.io) showed only a letter.
     */
    static java.util.List<String> gateways(String url) {
        java.util.List<String> out = new java.util.ArrayList<>();
        if (empty(url)) return out;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("^https?://[^/]+/ipfs/(.+)$").matcher(url);
        if (m.find()) {
            out.add("https://gateway.pinata.cloud/ipfs/" + m.group(1));
            out.add("https://ipfs.io/ipfs/" + m.group(1));
            out.add("https://dweb.link/ipfs/" + m.group(1));
        } else out.add(url);
        return out;
    }

    /** A kind of picture the app can draw: PNG, JPEG, WebP or GIF (not SVG). */
    static boolean drawable(String url) {
        if (empty(url)) return false;
        String u = url.trim().toLowerCase(java.util.Locale.US);
        if (u.startsWith("data:")) return u.startsWith("data:image/") && !u.startsWith("data:image/svg");
        return (u.startsWith("https://") || u.startsWith("http://")) && !u.split("\\?")[0].endsWith(".svg");
    }

    private static final okhttp3.OkHttpClient HTTP = new okhttp3.OkHttpClient.Builder()
            .connectTimeout(8, java.util.concurrent.TimeUnit.SECONDS).readTimeout(10, java.util.concurrent.TimeUnit.SECONDS).build();

    /** True when the address answers with a picture the app can draw. */
    static boolean loads(String url) {
        if (!drawable(url)) return false;
        if (url.startsWith("data:")) return true;
        try (okhttp3.Response r = HTTP.newCall(new okhttp3.Request.Builder().url(url).header("Range", "bytes=0-1023").header("User-Agent", "Harvana").build()).execute()) {
            if (!r.isSuccessful()) return false;
            String type = r.header("Content-Type", "");
            type = type == null ? "" : type.toLowerCase(java.util.Locale.US);
            if (type.contains("svg")) return false;
            return type.startsWith("image/") || type.contains("octet-stream") || type.isEmpty();
        } catch (Exception e) {
            return false;
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
