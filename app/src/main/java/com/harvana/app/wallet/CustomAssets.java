package com.harvana.app.wallet;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.harvana.app.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Chains and tokens the person adds themselves, from "Add coins". Nothing about them is written into the app: every
 * fact is fetched when it is added and read live afterwards.
 *  - an EVM chain by its chain ID: name, coin, decimals, a working RPC, explorer and icon from the public chain
 *    registry (ethereum-lists/chains); the RPC is checked to answer with that chain ID; the price id from CoinGecko;
 *  - a token by its contract on any EVM chain: name, symbol and decimals read from the contract, icon and price id
 *    from CoinGecko;
 *  - any chain of a protocol the app speaks, typed in by hand: name, coin, decimals, RPC, explorer pages, history API,
 *    icon, price id and key path are all fields; "Fill" reads what it can from the RPC or a public registry.
 *  - any token under any of those chains, with its own row that can be shown, hidden or removed.
 * A protocol (EVM, NEAR) is the code that knows how a network signs and reads; a chain is only data. Each chain and
 * token opens the same coin page as the built-in coins (balance, price, chart, history, send, receive).
 */
public final class CustomAssets {
    private CustomAssets() {}

    private static final String PREFS = "custom_assets";
    private static final OkHttpClient HTTP = new OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS).build();
    private static final MediaType JSON = MediaType.parse("application/json");
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final String REGISTRY = "https://raw.githubusercontent.com/ethereum-lists/chains/master/_data/";

    /** The protocols the app speaks. A chain of any of them can be added as data; a new protocol needs its engine. */
    public static final String[] PROTOCOLS = {"EVM", NearProtocol.FAMILY};

    /** One added chain or token. Every field is data: fetched when added, and editable. */
    public static final class Asset {
        public String kind = "CHAIN";          // CHAIN or TOKEN
        /** The protocol: EVM or NEAR. */
        public String family = "EVM";
        /** EVM chain id; NEAR network id (what the RPC reports). */
        public long chainId;
        public String networkId = "";
        /** A token's chain: its key ("C:1", "N:mainnet", or a built-in coin key such as "ETH"). */
        public String chainKey = "";
        public String chainName = "", name = "", symbol = "", rpc = "", explorer = "", icon = "", contract = "";
        /** Explorer page templates ({address}, {tx}); empty = explorer + /address/… and /tx/…. */
        public String addressUrl = "", txUrl = "";
        /** History API: Blockscout host (EVM) or NearBlocks-style API base (NEAR); "" = no history. */
        public String scout = "", historyApi = "";
        /** CoinGecko id for the price, chart and description ("" = none). */
        public String geckoId = "";
        /** Key path for protocols that use one per chain (NEAR: "m/44'/397'/0'"). */
        public String derivation = "";
        public int decimals = 18;
        /** A token hidden from the Assets list (kept, so it can be shown again). */
        public boolean hidden;

        public boolean near() { return NearProtocol.FAMILY.equals(family); }
        public boolean token() { return "TOKEN".equals(kind); }

        public String key() {
            if (token()) return near() ? "T:" + chainKey + ":" + contract : "T:" + chainId + ":" + contract.toLowerCase(Locale.US);
            return near() ? "N:" + networkId : "C:" + chainId;
        }

        /** The coin that pays this one's network fee. */
        public String feeSymbol() {
            if (!token()) return symbol;
            Asset c = find(chainKey.isEmpty() ? "C:" + chainId : chainKey);
            if (c != null) return c.symbol;
            for (String k : new String[]{"ETH", "BNB", "POL", "BASE"}) if (Net.evm(k).chainId == chainId) return "BNB".equals(k) ? "BNB" : "POL".equals(k) ? "POL" : "ETH";
            return "";
        }

        public String addressPage(String address) {
            if (!addressUrl.isEmpty()) return addressUrl.replace("{address}", address);
            return explorer.isEmpty() ? "" : explorer + "/address/" + address;
        }

        public String txPage(String hash) {
            if (!txUrl.isEmpty()) return txUrl.replace("{tx}", hash);
            return explorer.isEmpty() ? "" : explorer + "/tx/" + hash;
        }

        JSONObject json() throws Exception {
            return new JSONObject().put("kind", kind).put("family", family).put("chainId", chainId).put("networkId", networkId)
                    .put("chainKey", chainKey).put("chainName", chainName).put("name", name).put("symbol", symbol).put("rpc", rpc)
                    .put("explorer", explorer).put("addressUrl", addressUrl).put("txUrl", txUrl).put("icon", icon)
                    .put("contract", contract).put("decimals", decimals).put("geckoId", geckoId).put("scout", scout)
                    .put("historyApi", historyApi).put("derivation", derivation).put("hidden", hidden);
        }

        static Asset of(JSONObject o) {
            Asset a = new Asset();
            a.kind = o.optString("kind", "CHAIN"); a.family = o.optString("family", "EVM"); a.chainId = o.optLong("chainId");
            a.networkId = o.optString("networkId"); a.chainKey = o.optString("chainKey");
            a.chainName = o.optString("chainName"); a.name = o.optString("name"); a.symbol = o.optString("symbol"); a.rpc = o.optString("rpc");
            a.explorer = o.optString("explorer"); a.addressUrl = o.optString("addressUrl"); a.txUrl = o.optString("txUrl");
            a.icon = o.optString("icon"); a.contract = o.optString("contract");
            a.decimals = o.optInt("decimals", 18); a.geckoId = o.optString("geckoId"); a.scout = o.optString("scout");
            a.historyApi = o.optString("historyApi"); a.derivation = o.optString("derivation"); a.hidden = o.optBoolean("hidden");
            if (a.token() && a.chainKey.isEmpty() && !a.near()) a.chainKey = "C:" + a.chainId;
            return a;
        }

        Asset copy() { return of(jsonOrEmpty()); }

        private JSONObject jsonOrEmpty() { try { return json(); } catch (Exception e) { return new JSONObject(); } }
    }

    public interface Got { void got(Asset assetOrNull, String error); }

    // ── storage ────────────────────────────────────────────────────────────

    private static volatile List<Asset> cache;

    public static List<Asset> all(Context c) {
        List<Asset> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("list", "[]"));
            for (int i = 0; i < arr.length(); i++) out.add(Asset.of(arr.getJSONObject(i)));
        } catch (Exception ignored) { }
        cache = java.util.Collections.unmodifiableList(new ArrayList<>(out));
        return out;
    }

    /** The added list as last read (SketchApplication reads it at start). Safe on any thread. */
    public static List<Asset> cached() {
        List<Asset> c = cache;
        return c == null ? java.util.Collections.emptyList() : c;
    }

    /** The added chain or token with this key, or null. */
    public static Asset find(String key) {
        if (key == null) return null;
        for (Asset a : cached()) if (a.key().equals(key)) return a;
        return null;
    }

    private static void save(Context c, List<Asset> list) {
        JSONArray arr = new JSONArray();
        try { for (Asset a : list) arr.put(a.json()); } catch (Exception ignored) { }
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("list", arr.toString()).apply();
        cache = java.util.Collections.unmodifiableList(new ArrayList<>(list));
    }

    // ── addresses ──────────────────────────────────────────────────────────

    /** This wallet's address on an added chain, if already known. Never derives, so safe on the main thread. */
    public static String savedAddress(Context c, String harvAddress, Asset a) {
        if (c == null || a == null || harvAddress == null || harvAddress.isEmpty()) return "";
        if (a.near()) return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("near:" + harvAddress + ":" + pathOf(a), "");
        return CoinCatalog.savedAddress(c, harvAddress, "ETH");
    }

    /** The key path a chain's account uses (a token uses its chain's). */
    static String pathOf(Asset a) {
        Asset chain = a.token() ? find(a.chainKey) : a;
        String p = chain == null ? a.derivation : chain.derivation;
        return p == null || p.isEmpty() ? "m/44'/397'/0'" : p;
    }

    /** As {@link #savedAddress}, deriving it from the recovery phrase when needed. Run off the main thread. */
    public static String address(Context c, String harvAddress, Asset a) {
        String have = savedAddress(c, harvAddress, a);
        if (have != null && !have.isEmpty()) return have;
        try {
            if (a.near()) {
                String m = SecureWalletStorage.getActiveMnemonic(c.getApplicationContext());
                if (m == null || m.trim().isEmpty()) return "";
                String id = NearProtocol.address(m, pathOf(a));
                c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("near:" + harvAddress + ":" + pathOf(a), id).apply();
                return id;
            }
            String eth = CoinCatalog.addresses(c, harvAddress).get("ETH");
            return eth == null ? "" : eth;
        } catch (Exception e) {
            return "";
        }
    }

    static void add(Context c, Asset a) {
        List<Asset> list = all(c);
        for (int i = list.size() - 1; i >= 0; i--) if (list.get(i).key().equals(a.key())) list.remove(i);
        list.add(a);
        save(c, list);
    }

    static void remove(Context c, String key) {
        List<Asset> list = all(c);
        for (int i = list.size() - 1; i >= 0; i--) if (list.get(i).key().equals(key)) list.remove(i);
        save(c, list);
    }

    // ── fetching chain and token data ──────────────────────────────────────

    /** A chain from the registry by chain ID, with an RPC that answered with that chain ID. Runs off the main thread. */
    public static void fetchChain(long chainId, Got cb) {
        new Thread(() -> {
            try {
                JSONObject j = getJson(REGISTRY + "chains/eip155-" + chainId + ".json");
                Asset a = new Asset();
                a.kind = "CHAIN";
                a.chainId = chainId;
                a.chainName = j.optString("name", "Chain " + chainId);
                JSONObject cur = j.optJSONObject("nativeCurrency");
                a.name = a.chainName;
                a.symbol = cur == null ? "" : cur.optString("symbol", "");
                a.decimals = cur == null ? 18 : cur.optInt("decimals", 18);
                JSONArray ex = j.optJSONArray("explorers");
                if (ex != null && ex.length() > 0 && ex.optJSONObject(0) != null) a.explorer = trimSlash(ex.optJSONObject(0).optString("url", ""));
                JSONArray rpcs = j.optJSONArray("rpc");
                if (rpcs != null) for (int i = 0; i < rpcs.length() && a.rpc.isEmpty(); i++) {
                    String r = rpcs.optString(i, "");
                    if (!r.startsWith("https://") || r.contains("${")) continue;
                    try { if (chainIdOf(r) == chainId) a.rpc = r; } catch (Exception ignored) { }
                }
                if (a.rpc.isEmpty()) throw new Exception("No public RPC for this chain answered. Try again later.");
                a.icon = chainIcon(j.optString("icon", ""));
                JSONObject p = platform(chainId);
                if (p != null) a.geckoId = p.optString("native_coin_id", "");
                if (isBlockscout(a.explorer)) a.scout = a.explorer;
                MAIN.post(() -> cb.got(a, null));
            } catch (java.io.FileNotFoundException | NotFound e) {
                MAIN.post(() -> cb.got(null, "No chain with ID " + chainId + " is listed."));
            } catch (Exception e) {
                MAIN.post(() -> cb.got(null, e.getMessage() == null ? "Could not load this chain." : e.getMessage()));
            }
        }).start();
    }

    /** A token on an EVM chain, read from its contract. Runs off the main thread. */
    public static void fetchToken(long chainId, String chainName, String rpc, String explorer, String contract, Got cb) {
        new Thread(() -> {
            try {
                String addr = contract.trim();
                if (!addr.matches("^0x[0-9a-fA-F]{40}$")) throw new Exception("A token contract is 0x followed by 40 letters and numbers.");
                Asset a = new Asset();
                a.kind = "TOKEN";
                a.chainId = chainId;
                a.chainKey = "C:" + chainId;
                a.chainName = chainName;
                a.rpc = rpc;
                a.explorer = explorer;
                a.contract = addr;
                a.symbol = abiString(call(rpc, addr, "0x95d89b41"));
                a.name = abiString(call(rpc, addr, "0x06fdde03"));
                String dec = call(rpc, addr, "0x313ce567");
                a.decimals = dec.length() > 2 ? new BigInteger(dec.substring(2), 16).intValue() : 18;
                if (a.symbol.isEmpty()) throw new Exception("This address is not a token on " + chainName + ".");
                if (a.name.isEmpty()) a.name = a.symbol;
                String[] g = geckoToken(chainId, addr);
                a.geckoId = g[0];
                a.icon = g[1];
                if (isBlockscout(explorer)) a.scout = explorer;
                MAIN.post(() -> cb.got(a, null));
            } catch (Exception e) {
                MAIN.post(() -> cb.got(null, e.getMessage() == null ? "Could not read this token." : e.getMessage()));
            }
        }).start();
    }

    /** A NEAR-protocol chain from its RPC: the network id the RPC reports. Every other field is the person's. */
    public static void fetchNearChain(String rpc, Got cb) {
        new Thread(() -> {
            try {
                Asset a = new Asset();
                a.kind = "CHAIN";
                a.family = NearProtocol.FAMILY;
                a.rpc = rpc.trim();
                a.networkId = NearProtocol.networkId(a.rpc.split(",")[0].trim());
                a.decimals = 24;                                     // NEAR's protocol unit (yocto)
                a.derivation = "m/44'/397'/0'";
                MAIN.post(() -> cb.got(a, null));
            } catch (Exception e) {
                MAIN.post(() -> cb.got(null, e.getMessage() == null ? "That RPC did not answer." : e.getMessage()));
            }
        }).start();
    }

    /** A NEP-141 token on a NEAR-protocol chain, read from its ft_metadata. */
    public static void fetchNearToken(Asset chain, String contract, Got cb) {
        new Thread(() -> {
            try {
                String id = contract.trim().toLowerCase(Locale.US);
                if (!NearProtocol.validAccount(id)) throw new Exception("Enter the token's account, such as usdt.tether-token.near.");
                String[] m = NearProtocol.tokenMetadata(chain.rpc, id);
                Asset a = new Asset();
                a.kind = "TOKEN";
                a.family = NearProtocol.FAMILY;
                a.chainKey = chain.key();
                a.chainName = chain.chainName;
                a.rpc = chain.rpc;
                a.explorer = chain.explorer;
                a.addressUrl = chain.addressUrl;
                a.txUrl = chain.txUrl;
                a.contract = id;
                a.name = m[0].isEmpty() ? m[1] : m[0];
                a.symbol = m[1];
                a.decimals = Integer.parseInt(m[2]);
                a.icon = m[3];
                MAIN.post(() -> cb.got(a, null));
            } catch (Exception e) {
                MAIN.post(() -> cb.got(null, e.getMessage() == null ? "Could not read this token." : e.getMessage()));
            }
        }).start();
    }

    /** Balance as a plain number string, or "" when the network did not answer. Blocking. */
    public static String balanceOf(Asset a, String owner) { return balance(a, owner); }

    static String balance(Asset a, String owner) {
        try {
            if (a.near()) return a.token() ? NearProtocol.tokenBalance(a, owner) : NearProtocol.balance(a, owner);
            BigInteger raw;
            if ("TOKEN".equals(a.kind)) {
                String data = "0x70a08231" + String.format("%64s", owner.substring(2).toLowerCase(Locale.US)).replace(' ', '0');
                String hex = call(a.rpc, a.contract, data);
                raw = hex.length() > 2 ? new BigInteger(hex.substring(2), 16) : BigInteger.ZERO;
            } else {
                JSONObject r = rpc(a.rpc, "eth_getBalance", new JSONArray().put(owner).put("latest"));
                String hex = r.optString("result", "0x0");
                raw = new BigInteger(hex.length() > 2 ? hex.substring(2) : "0", 16);
            }
            return ChainBalances.amount(raw, a.decimals);
        } catch (Exception e) {
            return "";
        }
    }

    private static long chainIdOf(String rpcUrl) throws Exception {
        JSONObject r = rpc(rpcUrl, "eth_chainId", new JSONArray());
        String hex = r.optString("result", "");
        return hex.startsWith("0x") ? Long.parseLong(hex.substring(2), 16) : -1;
    }

    private static String call(String rpcUrl, String to, String data) throws Exception {
        JSONObject r = rpc(rpcUrl, "eth_call", new JSONArray().put(new JSONObject().put("to", to).put("data", data)).put("latest"));
        if (r.has("error")) throw new Exception("This address is not a token on this chain.");
        return r.optString("result", "0x");
    }

    private static JSONObject rpc(String url, String method, JSONArray params) throws Exception {
        String body = new JSONObject().put("jsonrpc", "2.0").put("id", 1).put("method", method).put("params", params).toString();
        try (Response r = HTTP.newCall(new Request.Builder().url(url).post(RequestBody.create(body, JSON)).build()).execute()) {
            if (!r.isSuccessful()) throw new Exception("HTTP " + r.code());
            return new JSONObject(r.body() == null ? "{}" : r.body().string());
        }
    }

    private static final class NotFound extends Exception { }

    private static JSONObject getJson(String url) throws Exception {
        try (Response r = HTTP.newCall(new Request.Builder().url(url).build()).execute()) {
            if (r.code() == 404) throw new NotFound();
            if (!r.isSuccessful()) throw new Exception("HTTP " + r.code());
            return new JSONObject(r.body() == null ? "{}" : r.body().string());
        }
    }

    /** The registry's icon for a chain: icons/<name>.json lists an ipfs:// image, served through a public gateway. */
    private static String chainIcon(String name) {
        if (name == null || name.isEmpty()) return "";
        try (Response r = HTTP.newCall(new Request.Builder().url(REGISTRY + "icons/" + name + ".json").build()).execute()) {
            if (!r.isSuccessful() || r.body() == null) return "";
            JSONArray arr = new JSONArray(r.body().string());
            String u = arr.length() > 0 && arr.optJSONObject(0) != null ? arr.optJSONObject(0).optString("url", "") : "";
            return u.startsWith("ipfs://") ? "https://ipfs.io/ipfs/" + u.substring(7) : u;
        } catch (Exception e) {
            return "";
        }
    }

    /** CoinGecko's small token image, when it knows the token. */
    private static volatile JSONArray platforms;

    /** CoinGecko's platform for an EVM chain id ({id, chain_identifier, native_coin_id, …}), or null. Blocking. */
    static JSONObject platform(long chainId) {
        try {
            JSONArray list = platforms;
            if (list == null) {
                Request req = new Request.Builder().url("https://api.coingecko.com/api/v3/asset_platforms").get().build();
                try (Response r = HTTP.newCall(req).execute()) { list = new JSONArray(r.body() == null ? "[]" : r.body().string()); }
                platforms = list;
            }
            for (int i = 0; i < list.length(); i++) {
                JSONObject p = list.optJSONObject(i);
                if (p != null && !p.isNull("chain_identifier") && p.optLong("chain_identifier", -1) == chainId) return p;
            }
        } catch (Exception ignored) { }
        return null;
    }

    /** A token's CoinGecko {id, icon}, found through its chain's platform. Blocking; {"", ""} when unknown. */
    private static String[] geckoToken(long chainId, String contract) {
        JSONObject p = platform(chainId);
        if (p == null) return new String[]{"", ""};
        try {
            JSONObject j = getJson("https://api.coingecko.com/api/v3/coins/" + p.optString("id") + "/contract/" + contract.toLowerCase(Locale.US));
            JSONObject img = j.optJSONObject("image");
            return new String[]{j.optString("id", ""), img == null ? "" : img.optString("small", "")};
        } catch (Exception e) {
            return new String[]{"", ""};
        }
    }

    /** True when the explorer is a Blockscout (its API gives the address history). */
    private static boolean isBlockscout(String explorer) {
        if (explorer == null || explorer.isEmpty()) return false;
        try { return getJson(explorer + "/api/v2/stats").has("total_blocks"); } catch (Exception e) { return false; }
    }

    /** Decodes an ABI string return, or a bytes32 one (older tokens such as MKR). */
    static String abiString(String hex) {
        if (hex == null || hex.length() < 4) return "";
        String h = hex.startsWith("0x") ? hex.substring(2) : hex;
        try {
            byte[] bytes;
            if (h.length() == 64) {
                bytes = hexBytes(h);
            } else {
                int len = new BigInteger(h.substring(64, 128), 16).intValue();
                bytes = hexBytes(h.substring(128, 128 + len * 2));
            }
            return new String(bytes, java.nio.charset.StandardCharsets.UTF_8).replace("\u0000", "").trim();
        } catch (Exception e) {
            return "";
        }
    }

    private static byte[] hexBytes(String h) {
        byte[] b = new byte[h.length() / 2];
        for (int i = 0; i < b.length; i++) b[i] = (byte) Integer.parseInt(h.substring(i * 2, i * 2 + 2), 16);
        return b;
    }

    private static String trimSlash(String s) { return s.endsWith("/") ? s.substring(0, s.length() - 1) : s; }

    // ── the Assets tab ─────────────────────────────────────────────────────

    /** One row per added chain or token, in the same style as the other coins; a row opens the coin page. */
    public static void render(Context c, LinearLayout box, String harvAddress) {
        if (box == null || c == null || harvAddress == null || harvAddress.isEmpty()) return;
        box.removeAllViews();
        List<Asset> list = all(c);
        if (list.isEmpty()) return;
        LayoutInflater inf = LayoutInflater.from(c);
        for (Asset a : list) {
            if (a.hidden) continue;
            View item = inf.inflate(R.layout.coin_list_card, box, false);
            item.setTag(a.key());
            ImageView logo = item.findViewById(R.id.iv_asset_logo);
            TextView name = item.findViewById(R.id.tv_asset_name);
            TextView bal = item.findViewById(R.id.tv_asset_balance);
            TextView symbol = item.findViewById(R.id.tv_asset_symbol);
            TextView side = item.findViewById(R.id.tv_asset_amount);
            if (name != null) name.setText(a.chainName);
            if (symbol != null) symbol.setText(a.symbol);
            if (side != null) side.setText("");
            if (bal != null) { bal.setText("…"); com.harvana.app.utils.BalancePrivacy.guard(bal); }
            if (logo != null) icon(c, logo, a);
            View row = item.findViewById(R.id.cardview1);
            View tap = row != null ? row : item;
            tap.setOnClickListener(v -> open(c, a, harvAddress));
            tap.setOnLongClickListener(v -> { manage(c, a.token() ? (find(a.chainKey) != null ? find(a.chainKey) : a) : a, () -> render(c, box, harvAddress)); return true; });
            box.addView(item);
            new Thread(() -> {
                String owner = address(c, harvAddress, a);
                String b = owner.isEmpty() ? "" : balance(a, owner);
                if (bal != null) bal.post(() -> bal.setText(b.isEmpty() ? "—" : b));
                // Its worth in the person's currency, once a price is known.
                if (b.isEmpty() || side == null) return;
                AssetPrices.load(c, () -> FxRates.load(c, () -> {
                    AssetPrices.Quote q = AssetPrices.peek(c, a.key());
                    Double held = AssetPrices.amount(b);
                    if (q == null || held == null) return;
                    side.setText(FxRates.money(c, held * q.usd));
                    com.harvana.app.utils.BalancePrivacy.guard(side);
                }));
            }).start();
        }
    }

    /** The coin page, the same one every coin uses. */
    public static void open(Context c, Asset a, String harvAddress) {
        new Thread(() -> {
            String owner = address(c, harvAddress, a);
            MAIN.post(() -> c.startActivity(new Intent(c, com.harvana.app.CoinDetailActivity.class)
                    .putExtra(CoinCatalog.EXTRA, a.key())
                    .putExtra("walletAddress", owner)));
        }).start();
    }

    /** Removes an added chain or token from the list (the coin page's menu). */
    public static void removeKey(Context c, String key) { remove(c, key); }

    /** Loads an added asset's icon (URL) into a view, with a letter badge until it arrives. */
    public static void icon(Context c, ImageView logo, String url, String symbol) {
        Asset a = new Asset();
        a.icon = url == null ? "" : url;
        a.symbol = symbol == null ? "" : symbol;
        icon(c, logo, a);
    }

    private static void icon(Context c, ImageView logo, Asset a) {
        if (a.icon.isEmpty()) { logo.setImageDrawable(letter(c, a.symbol)); return; }
        try {
            com.bumptech.glide.Glide.with(c).load(a.icon).circleCrop().placeholder(letter(c, a.symbol)).error(letter(c, a.symbol)).into(logo);
        } catch (Exception e) {
            logo.setImageDrawable(letter(c, a.symbol));
        }
    }

    /** A round badge with the first letter of the symbol, for a chain or token with no icon. */
    private static android.graphics.drawable.Drawable letter(Context c, String symbol) {
        final String ch = symbol == null || symbol.isEmpty() ? "?" : symbol.substring(0, 1).toUpperCase(Locale.US);
        final int bg = ContextCompat.getColor(c, R.color.colorAccent);
        final int fg = ContextCompat.getColor(c, R.color.colorSurface);
        return new android.graphics.drawable.Drawable() {
            final android.graphics.Paint p = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
            @Override public void draw(android.graphics.Canvas canvas) {
                android.graphics.Rect b = getBounds();
                p.setColor(bg);
                canvas.drawCircle(b.exactCenterX(), b.exactCenterY(), Math.min(b.width(), b.height()) / 2f, p);
                p.setColor(fg);
                p.setTextSize(b.height() * 0.5f);
                p.setTextAlign(android.graphics.Paint.Align.CENTER);
                p.setFakeBoldText(true);
                canvas.drawText(ch, b.exactCenterX(), b.exactCenterY() - (p.descent() + p.ascent()) / 2, p);
            }
            @Override public void setAlpha(int alpha) { }
            @Override public void setColorFilter(android.graphics.ColorFilter cf) { }
            @Override public int getOpacity() { return android.graphics.PixelFormat.TRANSLUCENT; }
        };
    }

    // ── the "Add a custom chain or token" sheet ────────────────────────────

    /** Opens the add sheet; {@code onAdded} runs after something was added so the list can redraw. */
    public static void showAdd(Context c, Runnable onAdded) {
        BottomSheetDialog d = new com.harvana.app.ui.AppSheet(c);
        LinearLayout root = column(c);
        root.addView(title(c, "Add"));
        root.addView(button(c, "A chain", v -> { d.dismiss(); editChain(c, null, onAdded); }));
        root.addView(button(c, "A token on a chain", v -> { d.dismiss(); addToken(c, null, onAdded); }));
        root.addView(button(c, "Tokens on a chain", v -> {
            d.dismiss();
            List<Asset> chains = chainsForTokens(c);
            String[] names = new String[chains.size()];
            for (int i = 0; i < names.length; i++) names[i] = chains.get(i).chainName;
            com.harvana.app.p2p.ThemedDialogs.show(com.harvana.app.p2p.ThemedDialogs.alert(c).setTitle("Chain")
                    .setItems(names, (dd, which) -> manage(c, chains.get(which), onAdded)));
        }));
        d.setContentView(root);
        d.show();
    }

    // ── a chain, typed in (Fill reads what it can) ─────────────────────────

    /** The chain form. {@code editing} is an added chain to change, or null for a new one. */
    public static void editChain(Context c, Asset editing, Runnable onDone) {
        BottomSheetDialog d = new com.harvana.app.ui.AppSheet(c);
        LinearLayout root = column(c);
        root.addView(title(c, editing == null ? "Add a chain" : editing.chainName));
        final String[] protocol = {editing == null ? "EVM" : editing.family};
        TextView pick = button(c, "Protocol: " + protocol[0], null);
        root.addView(pick);

        EditText lookup = field(c, "", InputType.TYPE_CLASS_TEXT);
        TextView status = sub(c, "");
        TextView fill = button(c, "Fill", null);
        EditText name = field(c, "Name", InputType.TYPE_CLASS_TEXT);
        EditText symbol = field(c, "Coin symbol", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        EditText decimals = field(c, "Decimals", InputType.TYPE_CLASS_NUMBER);
        EditText rpc = field(c, "RPC URL (several: comma between)", InputType.TYPE_TEXT_VARIATION_URI);
        EditText explorer = field(c, "Explorer URL", InputType.TYPE_TEXT_VARIATION_URI);
        EditText addressUrl = field(c, "Address page, with {address}", InputType.TYPE_TEXT_VARIATION_URI);
        EditText txUrl = field(c, "Transaction page, with {tx}", InputType.TYPE_TEXT_VARIATION_URI);
        EditText history = field(c, "History API", InputType.TYPE_TEXT_VARIATION_URI);
        EditText icon = field(c, "Icon URL", InputType.TYPE_TEXT_VARIATION_URI);
        EditText gecko = field(c, "CoinGecko id", InputType.TYPE_CLASS_TEXT);
        EditText path = field(c, "Key path", InputType.TYPE_CLASS_TEXT);
        root.addView(lookup);
        root.addView(fill);
        root.addView(status);
        for (EditText e : new EditText[]{name, symbol, decimals, rpc, explorer, addressUrl, txUrl, history, icon, gecko, path}) root.addView(e);

        Runnable protocolChanged = () -> {
            boolean near = NearProtocol.FAMILY.equals(protocol[0]);
            pick.setText("Protocol: " + protocol[0]);
            lookup.setHint(near ? "RPC URL" : "Chain ID");
            lookup.setInputType(near ? InputType.TYPE_TEXT_VARIATION_URI : InputType.TYPE_CLASS_NUMBER);
            path.setVisibility(near ? View.VISIBLE : View.GONE);
            history.setHint(near ? "History API (NearBlocks-style)" : "History API (Blockscout)");
        };
        pick.setOnClickListener(v -> com.harvana.app.p2p.ThemedDialogs.show(com.harvana.app.p2p.ThemedDialogs.alert(c).setTitle("Protocol")
                .setItems(PROTOCOLS, (dd, which) -> { protocol[0] = PROTOCOLS[which]; protocolChanged.run(); })));
        protocolChanged.run();

        // Fills the fields from a fetched chain; what the person already typed is kept.
        Fill put = a -> {
            setIfEmpty(name, a.chainName); setIfEmpty(symbol, a.symbol); setIfEmpty(decimals, String.valueOf(a.decimals));
            setIfEmpty(rpc, a.rpc); setIfEmpty(explorer, a.explorer); setIfEmpty(addressUrl, a.addressUrl); setIfEmpty(txUrl, a.txUrl);
            setIfEmpty(history, a.scout.isEmpty() ? a.historyApi : a.scout); setIfEmpty(icon, a.icon); setIfEmpty(gecko, a.geckoId);
            setIfEmpty(path, a.derivation);
            if (lookup.getText().toString().trim().isEmpty()) lookup.setText(NearProtocol.FAMILY.equals(a.family) ? a.rpc : String.valueOf(a.chainId));
        };
        if (editing != null) {
            put.on(editing);
            lookup.setText(editing.near() ? editing.rpc : String.valueOf(editing.chainId));
        }
        final Asset[] found = {editing == null ? null : editing.copy()};
        fill.setOnClickListener(v -> {
            String q = lookup.getText().toString().trim();
            if (q.isEmpty()) { status.setText(lookup.getHint()); return; }
            status.setText("…");
            Got got = (a, err) -> {
                if (a == null) { status.setText(err); return; }
                found[0] = a;
                status.setText(a.near() ? a.networkId : String.valueOf(a.chainId));
                put.on(a);
            };
            if (NearProtocol.FAMILY.equals(protocol[0])) fetchNearChain(q, got);
            else {
                try { fetchChain(Long.parseLong(q), got); } catch (NumberFormatException e) { status.setText("Chain ID"); }
            }
        });

        root.addView(button(c, "Save", v -> {
            Asset a = found[0] != null ? found[0].copy() : new Asset();
            a.kind = "CHAIN";
            a.family = protocol[0];
            a.chainName = text(name);
            a.name = a.chainName;
            a.symbol = text(symbol).toUpperCase(Locale.US);
            a.rpc = text(rpc);
            a.explorer = trimSlash(text(explorer));
            a.addressUrl = text(addressUrl);
            a.txUrl = text(txUrl);
            a.icon = text(icon);
            a.geckoId = text(gecko).toLowerCase(Locale.US);
            a.derivation = text(path);
            String hist = trimSlash(text(history));
            if (a.near()) { a.historyApi = hist; a.scout = ""; } else { a.scout = hist; a.historyApi = ""; }
            try { a.decimals = Integer.parseInt(text(decimals)); } catch (Exception e) { status.setText("Decimals"); return; }
            if (a.chainName.isEmpty() || a.symbol.isEmpty() || a.rpc.isEmpty()) { status.setText("Name, coin symbol and RPC URL are needed."); return; }
            status.setText("Checking the RPC…");
            final String q = lookup.getText().toString().trim();
            new Thread(() -> {
                String err = null;
                try {
                    if (a.near()) a.networkId = NearProtocol.networkId(a.rpc.split(",")[0].trim());
                    else {
                        long want = Long.parseLong(q);
                        long got = chainIdOf(a.rpc.split(",")[0].trim());
                        if (got != want) throw new Exception("That RPC answers as chain " + got + ", not " + want + ".");
                        a.chainId = want;
                    }
                } catch (NumberFormatException e) { err = "Chain ID"; }
                catch (Exception e) { err = e.getMessage() == null ? "The RPC did not answer." : e.getMessage(); }
                final String error = err;
                MAIN.post(() -> {
                    if (error != null) { status.setText(error); return; }
                    if (editing != null && !editing.key().equals(a.key())) remove(c, editing.key());
                    add(c, a);
                    d.dismiss();
                    if (onDone != null) onDone.run();
                });
            }).start();
        }));
        android.widget.ScrollView sv = new android.widget.ScrollView(c);
        sv.addView(root);
        d.setContentView(sv);
        d.show();
    }

    // ── a token under a chain ──────────────────────────────────────────────

    /** The token form; {@code chainKey} preselects the chain (null = the first). */
    public static void addToken(Context c, String chainKey, Runnable onDone) {
        BottomSheetDialog d = new com.harvana.app.ui.AppSheet(c);
        LinearLayout root = column(c);
        root.addView(title(c, "Add a token"));
        List<Asset> chains = chainsForTokens(c);
        String[] names = new String[chains.size()];
        final int[] picked = {0};
        for (int i = 0; i < names.length; i++) {
            names[i] = chains.get(i).chainName;
            if (chainKey != null && chainKey.equals(chains.get(i).key())) picked[0] = i;
        }
        TextView chainPick = button(c, "On: " + names[picked[0]], null);
        root.addView(chainPick);
        EditText contract = field(c, "", InputType.TYPE_CLASS_TEXT);
        TextView fill = button(c, "Fill", null);
        TextView status = sub(c, "");
        EditText name = field(c, "Name", InputType.TYPE_CLASS_TEXT);
        EditText symbol = field(c, "Symbol", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        EditText decimals = field(c, "Decimals", InputType.TYPE_CLASS_NUMBER);
        EditText icon = field(c, "Icon URL", InputType.TYPE_TEXT_VARIATION_URI);
        EditText gecko = field(c, "CoinGecko id", InputType.TYPE_CLASS_TEXT);
        root.addView(contract);
        root.addView(fill);
        root.addView(status);
        for (EditText e : new EditText[]{name, symbol, decimals, icon, gecko}) root.addView(e);
        Runnable hint = () -> contract.setHint(chains.get(picked[0]).near() ? "Token account, such as usdt.tether-token.near" : "Contract address, 0x…");
        hint.run();
        chainPick.setOnClickListener(v -> com.harvana.app.p2p.ThemedDialogs.show(com.harvana.app.p2p.ThemedDialogs.alert(c)
                .setTitle("Chain").setItems(names, (dd, which) -> { picked[0] = which; chainPick.setText("On: " + names[which]); hint.run(); })));
        final Asset[] found = {null};
        fill.setOnClickListener(v -> {
            Asset ch = chains.get(picked[0]);
            status.setText("…");
            Got got = (a, err) -> {
                if (a == null) { status.setText(err); return; }
                found[0] = a;
                status.setText(a.name);
                setIfEmpty(name, a.name); setIfEmpty(symbol, a.symbol); setIfEmpty(decimals, String.valueOf(a.decimals));
                setIfEmpty(icon, a.icon); setIfEmpty(gecko, a.geckoId);
            };
            if (ch.near()) fetchNearToken(ch, contract.getText().toString(), got);
            else fetchToken(ch.chainId, ch.chainName, ch.rpc, ch.explorer, contract.getText().toString(), got);
        });
        root.addView(button(c, "Save", v -> {
            Asset ch = chains.get(picked[0]);
            String id = text(contract);
            Asset a = found[0] != null && found[0].contract.equalsIgnoreCase(id) ? found[0].copy() : new Asset();
            a.kind = "TOKEN";
            a.family = ch.family;
            a.chainId = ch.chainId;
            a.chainKey = ch.key();
            a.chainName = ch.chainName;
            a.rpc = ch.rpc;
            a.explorer = ch.explorer;
            a.addressUrl = ch.addressUrl;
            a.txUrl = ch.txUrl;
            if (a.scout.isEmpty()) a.scout = ch.scout;
            a.contract = ch.near() ? id.toLowerCase(Locale.US) : id;
            a.name = text(name);
            a.symbol = text(symbol);
            a.icon = text(icon);
            a.geckoId = text(gecko).toLowerCase(Locale.US);
            try { a.decimals = Integer.parseInt(text(decimals)); } catch (Exception e) { status.setText("Decimals"); return; }
            if (a.contract.isEmpty() || a.symbol.isEmpty()) { status.setText("Contract and symbol are needed."); return; }
            if (!ch.near() && !a.contract.matches("^0x[0-9a-fA-F]{40}$")) { status.setText("A contract is 0x and 40 characters."); return; }
            if (ch.near() && !NearProtocol.validAccount(a.contract)) { status.setText("Token account, such as usdt.tether-token.near"); return; }
            if (a.name.isEmpty()) a.name = a.symbol;
            add(c, a);
            d.dismiss();
            if (onDone != null) onDone.run();
        }));
        android.widget.ScrollView sv = new android.widget.ScrollView(c);
        sv.addView(root);
        d.setContentView(sv);
        d.show();
    }

    // ── one chain's tokens: show, hide, remove; edit or remove the chain ───

    /** Every token on this chain with a switch to show or hide it; add a token; edit or remove an added chain. */
    public static void manage(Context c, Asset chain, Runnable onChange) {
        BottomSheetDialog d = new com.harvana.app.ui.AppSheet(c);
        LinearLayout root = column(c);
        root.addView(title(c, chain.chainName));
        String key = chain.token() ? chain.chainKey : chain.key();
        List<Asset> tokens = new ArrayList<>();
        for (Asset t : all(c)) if (t.token() && key.equals(t.chainKey)) tokens.add(t);
        if (!tokens.isEmpty()) root.addView(label(c, "Tokens"));
        for (Asset t : tokens) {
            LinearLayout row = new LinearLayout(c);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setMinimumHeight(dp(c, 52));
            row.setClickable(true);
            ImageView logo = new ImageView(c);
            icon(c, logo, t);
            row.addView(logo, new LinearLayout.LayoutParams(dp(c, 28), dp(c, 28)));
            TextView words = sub(c, t.symbol + "  ·  " + t.name);
            words.setTextColor(ContextCompat.getColor(c, R.color.colorTextPrimary));
            words.setPadding(dp(c, 12), 0, 0, 0);
            row.addView(words, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            com.google.android.material.switchmaterial.SwitchMaterial shown = new com.google.android.material.switchmaterial.SwitchMaterial(c);
            shown.setChecked(!t.hidden);
            shown.setOnCheckedChangeListener((b, on) -> { t.hidden = !on; add(c, t); if (onChange != null) onChange.run(); });
            row.addView(shown);
            row.setOnClickListener(v -> shown.toggle());
            row.setOnLongClickListener(v -> {
                com.harvana.app.p2p.ThemedDialogs.show(com.harvana.app.p2p.ThemedDialogs.alert(c).setTitle("Remove " + t.symbol + "?")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Remove", (dd, w) -> { remove(c, t.key()); d.dismiss(); if (onChange != null) onChange.run(); manage(c, chain, onChange); }));
                return true;
            });
            root.addView(row);
        }
        root.addView(button(c, "Add a token", v -> { d.dismiss(); addToken(c, key, onChange); }));
        Asset added = find(key);
        if (added != null && !added.token()) {
            root.addView(button(c, "Edit chain", v -> { d.dismiss(); editChain(c, added, onChange); }));
            root.addView(button(c, "Remove chain", v -> com.harvana.app.p2p.ThemedDialogs.show(com.harvana.app.p2p.ThemedDialogs.alert(c)
                    .setTitle("Remove " + added.chainName + " and its tokens?")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Remove", (dd, w) -> {
                        for (Asset t : tokens) remove(c, t.key());
                        remove(c, added.key());
                        d.dismiss();
                        if (onChange != null) onChange.run();
                    }))));
        }
        android.widget.ScrollView sv = new android.widget.ScrollView(c);
        sv.addView(root);
        d.setContentView(sv);
        d.show();
    }

    private interface Fill { void on(Asset a); }

    private static String text(EditText e) { return e.getText() == null ? "" : e.getText().toString().trim(); }

    private static void setIfEmpty(EditText e, String v) {
        if (v != null && !v.isEmpty() && text(e).isEmpty()) e.setText(v);
    }

    /** The built-in EVM chains and every chain the person added (any protocol), as places a token can live. */
    private static List<Asset> chainsForTokens(Context c) {
        List<Asset> out = new ArrayList<>();
        for (String k : new String[]{"ETH", "BNB", "POL", "BASE"}) {
            Net.Evm e = Net.evm(k);
            Asset a = new Asset();
            a.chainId = e.chainId; a.chainName = e.name; a.rpc = e.rpc; a.symbol = "BNB".equals(k) ? "BNB" : "POL".equals(k) ? "POL" : "ETH";
            a.explorer = e.addressPage.endsWith("/address/") ? e.addressPage.substring(0, e.addressPage.length() - "/address/".length()) : "";
            a.scout = e.scout == null ? "" : e.scout;
            out.add(a);
        }
        for (Asset a : all(c)) if (!a.token()) out.add(a);
        return out;
    }

    // ── small builders ─────────────────────────────────────────────────────

    private static LinearLayout column(Context c) {
        LinearLayout root = new LinearLayout(c);
        root.setOrientation(LinearLayout.VERTICAL);
        int p = dp(c, 20);
        root.setPadding(p, p, p, p);
        return root;
    }

    private static TextView title(Context c, String s) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(18);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextColor(ContextCompat.getColor(c, R.color.colorTextPrimary));
        return t;
    }

    private static TextView label(Context c, String s) {
        TextView t = title(c, s);
        t.setTextSize(14);
        t.setPadding(0, dp(c, 16), 0, dp(c, 4));
        return t;
    }

    private static TextView sub(Context c, String s) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(13);
        t.setTextColor(ContextCompat.getColor(c, R.color.colorTextSecondary));
        t.setPadding(0, dp(c, 4), 0, dp(c, 4));
        return t;
    }

    private static EditText field(Context c, String hint, int type) {
        EditText e = new com.google.android.material.textfield.TextInputEditText(c);
        e.setHint(hint);
        e.setInputType(type);
        e.setSingleLine(true);
        return e;
    }

    private static TextView button(Context c, String s, View.OnClickListener l) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(14);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextColor(ContextCompat.getColor(c, R.color.colorPrimary));
        t.setGravity(android.view.Gravity.CENTER_VERTICAL);
        t.setMinHeight(dp(c, 44));
        android.util.TypedValue tv = new android.util.TypedValue();
        c.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, tv, true);
        t.setBackgroundResource(tv.resourceId);
        if (l != null) t.setOnClickListener(l);
        return t;
    }

    private static int dp(Context c, int v) { return Math.round(v * c.getResources().getDisplayMetrics().density); }
}
