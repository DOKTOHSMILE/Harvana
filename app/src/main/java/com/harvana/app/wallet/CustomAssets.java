package com.harvana.app.wallet;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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
 * Chains and tokens the person adds themselves, from "Add coins":
 *  - a custom EVM chain by its chain ID: the name, coin, decimals, a working RPC, the explorer and the icon are fetched
 *    from the public chain registry (ethereum-lists/chains, the source of chainlist.org), and the RPC is checked to
 *    really answer with that chain ID;
 *  - a token by its contract address on any EVM chain (built-in or added): name, symbol and decimals are read from the
 *    contract itself, the icon from CoinGecko.
 * The address is the person's own EVM address (the Ethereum one from their recovery phrase). Rows show the balance and
 * open a sheet to receive (copy the address) and to see the address on the explorer.
 */
public final class CustomAssets {
    private CustomAssets() {}

    private static final String PREFS = "custom_assets";
    private static final OkHttpClient HTTP = new OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS).build();
    private static final MediaType JSON = MediaType.parse("application/json");
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final String REGISTRY = "https://raw.githubusercontent.com/ethereum-lists/chains/master/_data/";

    /** One added chain or token. */
    public static final class Asset {
        public String kind = "CHAIN";          // CHAIN or TOKEN
        public long chainId;
        public String chainName = "", name = "", symbol = "", rpc = "", explorer = "", icon = "", contract = "";
        public int decimals = 18;

        public String key() { return "TOKEN".equals(kind) ? "T:" + chainId + ":" + contract.toLowerCase(Locale.US) : "C:" + chainId; }

        JSONObject json() throws Exception {
            return new JSONObject().put("kind", kind).put("chainId", chainId).put("chainName", chainName).put("name", name)
                    .put("symbol", symbol).put("rpc", rpc).put("explorer", explorer).put("icon", icon)
                    .put("contract", contract).put("decimals", decimals);
        }

        static Asset of(JSONObject o) {
            Asset a = new Asset();
            a.kind = o.optString("kind", "CHAIN"); a.chainId = o.optLong("chainId"); a.chainName = o.optString("chainName");
            a.name = o.optString("name"); a.symbol = o.optString("symbol"); a.rpc = o.optString("rpc");
            a.explorer = o.optString("explorer"); a.icon = o.optString("icon"); a.contract = o.optString("contract");
            a.decimals = o.optInt("decimals", 18);
            return a;
        }
    }

    public interface Got { void got(Asset assetOrNull, String error); }

    // ── storage ────────────────────────────────────────────────────────────

    public static List<Asset> all(Context c) {
        List<Asset> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("list", "[]"));
            for (int i = 0; i < arr.length(); i++) out.add(Asset.of(arr.getJSONObject(i)));
        } catch (Exception ignored) { }
        return out;
    }

    private static void save(Context c, List<Asset> list) {
        JSONArray arr = new JSONArray();
        try { for (Asset a : list) arr.put(a.json()); } catch (Exception ignored) { }
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("list", arr.toString()).apply();
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
                a.icon = tokenIcon(chainId, addr);
                MAIN.post(() -> cb.got(a, null));
            } catch (Exception e) {
                MAIN.post(() -> cb.got(null, e.getMessage() == null ? "Could not read this token." : e.getMessage()));
            }
        }).start();
    }

    /** Balance as a plain number string, or "" when the network did not answer. Blocking. */
    static String balance(Asset a, String owner) {
        try {
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
    private static String tokenIcon(long chainId, String contract) {
        String platform;
        switch ((int) chainId) {
            case 1: platform = "ethereum"; break;
            case 56: platform = "binance-smart-chain"; break;
            case 137: platform = "polygon-pos"; break;
            case 8453: platform = "base"; break;
            case 42161: platform = "arbitrum-one"; break;
            case 10: platform = "optimistic-ethereum"; break;
            case 43114: platform = "avalanche"; break;
            case 250: platform = "fantom"; break;
            case 324: platform = "zksync"; break;
            case 59144: platform = "linea"; break;
            default: return "";
        }
        try {
            JSONObject j = getJson("https://api.coingecko.com/api/v3/coins/" + platform + "/contract/" + contract.toLowerCase(Locale.US));
            JSONObject img = j.optJSONObject("image");
            return img == null ? "" : img.optString("small", "");
        } catch (Exception e) {
            return "";
        }
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

    /** One row per added chain or token, in the same plain style as the other coins. */
    public static void render(Context c, LinearLayout box, String harvAddress) {
        if (box == null || c == null || harvAddress == null || harvAddress.isEmpty()) return;
        box.removeAllViews();
        List<Asset> list = all(c);
        if (list.isEmpty()) return;
        String owner = CoinCatalog.savedAddress(c, harvAddress, "ETH");
        if (owner == null || !owner.startsWith("0x")) return;
        LayoutInflater inf = LayoutInflater.from(c);
        for (Asset a : list) {
            View item = inf.inflate(R.layout.coin_list_card, box, false);
            item.setTag(a.key());
            ImageView logo = item.findViewById(R.id.iv_asset_logo);
            TextView name = item.findViewById(R.id.tv_asset_name);
            TextView bal = item.findViewById(R.id.tv_asset_balance);
            TextView symbol = item.findViewById(R.id.tv_asset_symbol);
            TextView side = item.findViewById(R.id.tv_asset_amount);
            if (name != null) name.setText("TOKEN".equals(a.kind) ? a.name + " · " + a.chainName : a.name);
            if (symbol != null) symbol.setText(a.symbol);
            if (side != null) side.setText("");
            if (bal != null) { bal.setText("…"); com.harvana.app.utils.BalancePrivacy.guard(bal); }
            if (logo != null) icon(c, logo, a);
            View row = item.findViewById(R.id.cardview1);
            (row != null ? row : item).setOnClickListener(v -> detail(c, a, owner, box, harvAddress));
            box.addView(item);
            new Thread(() -> {
                String b = balance(a, owner);
                if (bal != null) bal.post(() -> bal.setText(b.isEmpty() ? "—" : b));
            }).start();
        }
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

    private static void detail(Context c, Asset a, String owner, LinearLayout box, String harvAddress) {
        BottomSheetDialog d = new BottomSheetDialog(c);
        LinearLayout root = column(c);
        root.addView(title(c, a.name + " (" + a.symbol + ")"));
        root.addView(sub(c, "TOKEN".equals(a.kind)
                ? "A token you added on " + a.chainName + ". Contract " + a.contract + ". Decimals " + a.decimals + "."
                : "A chain you added. Chain ID " + a.chainId + ". The coin is " + a.symbol + ", with " + a.decimals + " decimals."));
        root.addView(sub(c, "Your address (the same as your Ethereum address):\n" + owner));
        root.addView(sub(c, "Only send " + a.symbol + " on " + a.chainName + " to this address. Sending from this app is not available for added "
                + ("TOKEN".equals(a.kind) ? "tokens" : "chains") + " yet; receive here and send from another wallet with the same recovery phrase."));
        root.addView(button(c, "Copy address", v -> {
            ClipboardManager cm = (ClipboardManager) c.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("Address", owner));
            Toast.makeText(c, "Copied", Toast.LENGTH_SHORT).show();
        }));
        if (!a.explorer.isEmpty()) root.addView(button(c, "See on the explorer", v -> {
            try { c.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(a.explorer + "/address/" + owner))); } catch (Exception ignored) { }
        }));
        root.addView(button(c, "Remove from my list", v -> {
            remove(c, a.key());
            d.dismiss();
            render(c, box, harvAddress);
        }));
        d.setContentView(root);
        d.show();
    }

    // ── the "Add a custom chain or token" sheet ────────────────────────────

    /** Opens the add sheet; {@code onAdded} runs after something was added so the list can redraw. */
    public static void showAdd(Context c, Runnable onAdded) {
        BottomSheetDialog d = new BottomSheetDialog(c);
        LinearLayout root = column(c);
        root.addView(title(c, "Add a chain or token"));
        root.addView(sub(c, "EVM networks only (Ethereum, BNB Chain, Polygon, Base, Arbitrum and the rest). Your address there is your Ethereum address."));

        // 1. Custom chain by chain ID
        root.addView(label(c, "A chain, by its chain ID"));
        EditText chainId = field(c, "Chain ID, for example 42161 for Arbitrum", InputType.TYPE_CLASS_NUMBER);
        root.addView(chainId);
        TextView chainStatus = sub(c, "");
        root.addView(button(c, "Find chain", v -> {
            long id;
            try { id = Long.parseLong(chainId.getText().toString().trim()); } catch (Exception e) { chainStatus.setText("Enter the chain ID as a number."); return; }
            chainStatus.setText("Looking it up…");
            fetchChain(id, (a, err) -> {
                if (a == null) { chainStatus.setText(err); return; }
                chainStatus.setText(a.chainName + " · " + a.symbol + " · RPC checked");
                confirm(c, a, () -> { add(c, a); d.dismiss(); if (onAdded != null) onAdded.run(); });
            });
        }));
        root.addView(chainStatus);

        // 2. Token by contract address
        root.addView(label(c, "A token, by its contract address"));
        List<Asset> chains = chainsForTokens(c);
        String[] names = new String[chains.size()];
        for (int i = 0; i < names.length; i++) names[i] = chains.get(i).chainName;
        final int[] picked = {0};
        TextView chainPick = button(c, "On: " + names[0], null);
        chainPick.setOnClickListener(v -> com.harvana.app.p2p.ThemedDialogs.show(com.harvana.app.p2p.ThemedDialogs.alert(c)
                .setTitle("Token is on").setItems(names, (dd, which) -> { picked[0] = which; chainPick.setText("On: " + names[which]); })));
        root.addView(chainPick);
        EditText contract = field(c, "Contract address, 0x…", InputType.TYPE_CLASS_TEXT);
        root.addView(contract);
        TextView tokenStatus = sub(c, "");
        root.addView(button(c, "Find token", v -> {
            Asset ch = chains.get(picked[0]);
            tokenStatus.setText("Reading the contract…");
            fetchToken(ch.chainId, ch.chainName, ch.rpc, ch.explorer, contract.getText().toString(), (a, err) -> {
                if (a == null) { tokenStatus.setText(err); return; }
                tokenStatus.setText(a.name + " · " + a.symbol + " · " + a.decimals + " decimals");
                confirm(c, a, () -> { add(c, a); d.dismiss(); if (onAdded != null) onAdded.run(); });
            });
        }));
        root.addView(tokenStatus);

        android.widget.ScrollView sv = new android.widget.ScrollView(c);
        sv.addView(root);
        d.setContentView(sv);
        d.show();
    }

    /** Built-in EVM chains plus any chain the person added, as a place a token can live. */
    private static List<Asset> chainsForTokens(Context c) {
        List<Asset> out = new ArrayList<>();
        for (String k : new String[]{"ETH", "BNB", "POL", "BASE"}) {
            Net.Evm e = Net.evm(k);
            Asset a = new Asset();
            a.chainId = e.chainId; a.chainName = e.name; a.rpc = e.rpc;
            a.explorer = e.addressPage.endsWith("/address/") ? e.addressPage.substring(0, e.addressPage.length() - "/address/".length()) : "";
            out.add(a);
        }
        for (Asset a : all(c)) if ("CHAIN".equals(a.kind)) out.add(a);
        return out;
    }

    private static void confirm(Context c, Asset a, Runnable yes) {
        String what = "TOKEN".equals(a.kind)
                ? a.name + " (" + a.symbol + ") on " + a.chainName + "\nContract " + a.contract
                : a.chainName + "\nChain ID " + a.chainId + " · coin " + a.symbol + "\nRPC " + a.rpc;
        com.harvana.app.p2p.ThemedDialogs.show(com.harvana.app.p2p.ThemedDialogs.alert(c)
                .setTitle("Add this?")
                .setMessage(what + "\n\nAnyone can make a token with any name. Add only a contract you got from a source you trust.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Add", (dd, w) -> yes.run()));
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
