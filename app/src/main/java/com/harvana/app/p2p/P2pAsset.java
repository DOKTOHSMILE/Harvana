package com.harvana.app.p2p;

import com.harvana.app.husd.HusdClient;
import com.harvana.app.husd.Stable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Which coin P2P is trading: HARV, or a stable asset (hUSD, hNGN, …; node docs/STABLE_ASSETS.md, "P2P"). The choice
 * is made once in the marketplace's coin picker and shared by every P2P screen, like the currency filter. A listing
 * or order carries its own "asset", so screens showing one always use that, whatever is picked now.
 *
 * Amounts: HARV has 8 decimals on chain, stable assets 6. Every P2P amount goes through here.
 */
public final class P2pAsset {
    private P2pAsset() {}

    public static final String HARV = "HARV";

    public interface Listener { void onAssetChanged(String code); }

    private static String current = HARV;
    private static final List<Listener> listeners = new ArrayList<>();
    /** Stable assets the node has switched on (filled by {@link #loadActive}); HARV is always available. */
    private static final List<String> active = new ArrayList<>();

    public static String get() { return current; }

    public static void set(String code) {
        String c = normalize(code);
        if (c.equals(current)) return;
        current = c;
        for (Listener l : new ArrayList<>(listeners)) l.onAssetChanged(current);
    }

    public static void addListener(Listener l) { if (!listeners.contains(l)) listeners.add(l); }
    public static void removeListener(Listener l) { listeners.remove(l); }

    /** "HARV" for HARV (and for anything blank or unknown), else the stable code. */
    public static String normalize(String code) {
        if (code == null) return HARV;
        String c = code.trim().toUpperCase(java.util.Locale.ROOT);
        return Stable.isStable(c) ? c : HARV;
    }

    public static boolean isStable(String code) { return !HARV.equals(normalize(code)); }

    /** The asset a node listing or order carries ("asset"); HARV for records made before stable assets. */
    public static String of(JSONObject rec) { return normalize(rec == null ? null : rec.optString("asset", HARV)); }
    public static String of(Map<String, Object> rec) {
        Object a = rec == null ? null : rec.get("asset");
        return normalize(a == null ? null : String.valueOf(a));
    }

    public static String ticker(String code) {
        return isStable(code) ? Stable.symbol(normalize(code)) : com.harvana.app.CoinInfo.ticker();
    }

    public static String name(String code) {
        return isStable(code) ? Stable.symbol(normalize(code)) + " · " + Stable.name(normalize(code)) : com.harvana.app.CoinInfo.ticker() + " — Harvana Protocol";
    }

    public static long unitsPerCoin(String code) {
        return isStable(code) ? HusdClient.UNIT : com.harvana.app.sync.HarvanaNodeClient.HARV_UNITS_PER_TOKEN;
    }

    /** 12.5 → raw units of this asset. */
    public static long toRaw(double amount, String code) {
        return new java.math.BigDecimal(String.valueOf(amount)).multiply(java.math.BigDecimal.valueOf(unitsPerCoin(code)))
                .setScale(0, java.math.RoundingMode.DOWN).longValueExact();
    }

    /** Raw units of this asset → 12.5. */
    public static double fromRaw(long raw, String code) { return raw / (double) unitsPerCoin(code); }

    /** Argument order used by the P2P screens (code first). */
    public static double fromRaw(String code, long raw) { return fromRaw(raw, code); }
    public static long toRaw(String code, double amount) { return toRaw(amount, code); }

    /** The smallest and largest listing the node takes, in coins (node P2PMarketManager.createListing). */
    public static double minListing(String code) {
        switch (normalize(code)) {
            case "HNGN": return 7_500;
            case "HGHS": return 60;
            case "HKES": return 650;
            case "HZAR": return 90;
            case HARV: return 10;
            default: return 5;                    // hUSD, hEUR, hGBP
        }
    }

    /** The smallest single trade: 1 HARV; for a stable coin, 10x its fee (every escrow move pays the coin's fee). */
    public static double minOrder(String code) {
        switch (normalize(code)) {
            case "HNGN": return 300;
            case "HGHS": return 3;
            case "HKES": return 25;
            case "HZAR": return 3.6;
            default: return 1;
        }
    }

    public static double maxListing(String code) {
        switch (normalize(code)) {
            case "HNGN": return 1_500_000_000d;
            case "HGHS": return 15_000_000d;
            case "HKES": return 130_000_000d;
            case "HZAR": return 18_000_000d;
            case HARV: return 5_000;
            default: return 1_000_000;
        }
    }

    // ── Which stable assets are on ─────────────────────────────────────────

    public static List<String> available() {
        List<String> out = new ArrayList<>();
        out.add(HARV);
        synchronized (active) { out.addAll(active); }
        return out;
    }

    /** Asks the node which stable assets are on; {@code done} runs on the caller's callback thread. */
    public static void loadActive(Runnable done) {
        HusdClient.list(new HusdClient.Cb() {
            @Override public void ok(JSONObject r) {
                JSONArray arr = r.optJSONArray("assets");
                List<String> on = new ArrayList<>();
                if (arr != null) for (int i = 0; i < arr.length(); i++) {
                    JSONObject a = arr.optJSONObject(i);
                    if (a != null && a.optBoolean("active") && Stable.isStable(a.optString("asset"))) on.add(a.optString("asset"));
                }
                synchronized (active) { active.clear(); active.addAll(on); }
                if (done != null) done.run();
            }
            @Override public void fail(String e) { if (done != null) done.run(); }
        });
    }

    // ── Balance of one asset ───────────────────────────────────────────────

    public interface BalanceCb { void ok(double coins); void fail(String error); }

    /** What this address can use of the asset, in coins: HARV from the node's live balance, a stable asset's
     *  "available" (confirmed less what is queued to leave). Callbacks arrive off the main thread. */
    public static void balance(android.content.Context c, String code, String address, BalanceCb cb) {
        if (isStable(code)) {
            HusdClient.balance(normalize(code), address, new HusdClient.Cb() {
                @Override public void ok(JSONObject r) { cb.ok(r.optLong("availableRaw") / (double) HusdClient.UNIT); }
                @Override public void fail(String e) { cb.fail(e); }
            });
            return;
        }
        com.harvana.app.wallet.WalletNodeBridge.fetchLiveBalance(c, address, new com.harvana.app.wallet.WalletNodeBridge.BalanceCallback() {
            @Override public void onSuccess(double harv, long nonce, long height) { cb.ok(harv); }
            @Override public void onError(String e) { cb.fail(e); }
        });
    }
}
