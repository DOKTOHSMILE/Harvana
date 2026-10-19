package com.harvana.app.notify;

import android.content.Context;
import android.content.SharedPreferences;

import com.harvana.app.husd.HusdClient;
import com.harvana.app.husd.Stable;
import com.harvana.app.wallet.AssetPrices;
import com.harvana.app.wallet.ChainAssetsSection;
import com.harvana.app.wallet.ChainBalances;
import com.harvana.app.wallet.CoinCatalog;
import com.harvana.app.wallet.CustomAssets;
import com.harvana.app.wallet.WalletManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Notifications for every asset other than HARV (HARV comes from the node's history, WalletTxNotifier): stable coins,
 * the other coins shown on Assets and the chains and tokens added by hand. Each asset's last balance is remembered; when
 * it goes up the person is told what arrived, when it goes down what left. The first check of an asset only records a
 * starting point, so nothing old is announced.
 */
public final class AssetBalanceNotifier {
    private AssetBalanceNotifier() {}

    public static void run(Context ctx) {
        final Context c = ctx.getApplicationContext();
        final String me = WalletManager.getActiveWalletAddress(c);
        if (me == null || me.trim().isEmpty()) return;
        final SharedPreferences sp = c.getSharedPreferences("asset_balance_notify", Context.MODE_PRIVATE);
        // Just switched to this wallet: this pass only records the balances, so changes from while it was not the active
        // wallet are not announced as if new.
        final boolean quiet = !sp.getString("watching", "").isEmpty() && !me.equals(sp.getString("watching", ""));
        sp.edit().putString("watching", me).apply();

        // Stable coins that are on.
        HusdClient.list(new HusdClient.Cb() {
            @Override public void ok(JSONObject r) {
                JSONArray arr = r.optJSONArray("assets");
                if (arr == null) return;
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject a = arr.optJSONObject(i);
                    if (a == null || !a.optBoolean("active") || !Stable.isStable(a.optString("asset"))) continue;
                    final String code = a.optString("asset");
                    HusdClient.balance(code, me, new HusdClient.Cb() {
                        @Override public void ok(JSONObject b) {
                            BigDecimal held = BigDecimal.valueOf(b.optLong("balanceRaw")).movePointLeft(6);
                            compare(c, sp, me, code, Stable.symbol(code), held, quiet);
                        }
                        @Override public void fail(String e) { }
                    });
                }
            }
            @Override public void fail(String e) { }
        });

        // Other coins and added chains/tokens: their balance calls block, so off the main thread.
        new Thread(() -> {
            try {
                Map<String, String> addrs = CoinCatalog.addresses(c, me);
                for (CoinCatalog.Coin coin : CoinCatalog.others()) {
                    if (!ChainAssetsSection.shown(c, coin.key)) continue;
                    String a = addrs.get(coin.key);
                    if (a == null || a.isEmpty()) continue;
                    Double held = AssetPrices.amount(ChainBalances.assetBalance(coin.key, a));
                    if (held != null) compare(c, sp, me, coin.key, coin.ticker, BigDecimal.valueOf(held), quiet);
                }
                String evm = CoinCatalog.savedAddress(c, me, "ETH");
                if (evm != null && evm.startsWith("0x")) {
                    for (CustomAssets.Asset asset : CustomAssets.all(c)) {
                        String b = CustomAssets.balanceOf(asset, evm);
                        if (!b.isEmpty()) compare(c, sp, me, asset.key(), asset.symbol, new BigDecimal(b), quiet);
                    }
                }
            } catch (Exception ignored) { }
        }, "asset-notify").start();
    }

    /** Pure decision, for tests: what to say when the balance went from {@code before} to {@code now}, or null for nothing. */
    static String[] message(BigDecimal before, BigDecimal now, String symbol) {
        if (before == null || now == null) return null;
        int cmp = now.compareTo(before);
        if (cmp == 0) return null;
        String delta = now.subtract(before).abs().stripTrailingZeros().toPlainString();
        return cmp > 0
                ? new String[]{"Received " + delta + " " + symbol, "Your " + symbol + " balance is now " + now.stripTrailingZeros().toPlainString() + "."}
                : new String[]{"Sent " + delta + " " + symbol, "Your " + symbol + " balance is now " + now.stripTrailingZeros().toPlainString() + "."};
    }

    /**
     * A new balance is announced only when two checks in a row read it: a chain API that answers 0 or a stale figure
     * once (and the right one next time) used to announce the same coins as sent and then received again.
     */
    static boolean confirmed(String candidate, BigDecimal now) {
        return candidate != null && new BigDecimal(candidate).compareTo(now) == 0;
    }

    private static synchronized void compare(Context c, SharedPreferences sp, String me, String key, String symbol, BigDecimal now, boolean quiet) {
        String pref = me + ":" + key, seen = "seen:" + pref;
        String saved = sp.getString(pref, null);
        if (saved == null || quiet) {                                  // first look, or just switched: only the starting point
            sp.edit().putString(pref, now.toPlainString()).remove(seen).apply();
            return;
        }
        BigDecimal before = new BigDecimal(saved);
        if (before.compareTo(now) == 0) { sp.edit().remove(seen).apply(); return; }
        if (!confirmed(sp.getString(seen, null), now)) { sp.edit().putString(seen, now.toPlainString()).apply(); return; }
        sp.edit().putString(pref, now.toPlainString()).remove(seen).apply();
        String[] m = message(before, now, symbol);
        if (m == null || com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() == null) return;
        NotificationCenter.post(c, NotificationCenter.TX, "asset_" + me + "_" + key + "_" + before.toPlainString() + "_" + now.toPlainString()
                + "_" + (System.currentTimeMillis() / 3_600_000L), m[0], m[1], "", -1);
    }
}
