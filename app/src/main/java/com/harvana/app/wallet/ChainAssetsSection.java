package com.harvana.app.wallet;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.harvana.app.CoinDetailActivity;
import com.harvana.app.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The other coins under HARV on the Assets tab. Each coin is its own row, in the same card as HARV,
 * and opens that coin's detail page. The member can hide a coin with "Add coins".
 */
public final class ChainAssetsSection {
    private ChainAssetsSection() {}

    private static final String PREFS = "asset_chain_toggles";

    public static boolean shown(Context c, String key) {
        android.content.SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (p.contains(key)) return p.getBoolean(key, true);
        // The old single "Ethereum" switch used to cover BNB, Polygon and Base as well.
        if (("BNB".equals(key) || "POL".equals(key) || "BASE".equals(key)) && p.contains("ETH")) return p.getBoolean("ETH", true);
        return true;
    }

    private static void setShown(Context c, String key, boolean on) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(key, on).apply();
    }

    /** Opens the picker; {@code onChange} runs after any toggle so the list can redraw. */
    public static void showPicker(Context c, Runnable onChange) {
        BottomSheetDialog d = new BottomSheetDialog(c);
        LinearLayout root = new LinearLayout(c);
        root.setOrientation(LinearLayout.VERTICAL);
        int p = dp(c, 20);
        root.setPadding(p, p, p, p);
        TextView title = new TextView(c);
        title.setText("Add coins");
        title.setTextSize(18);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(ContextCompat.getColor(c, R.color.colorTextPrimary));
        root.addView(title);
        TextView sub = new TextView(c);
        sub.setText("Each coin is its own wallet, from the same recovery phrase. Turn one off to hide it.");
        sub.setTextSize(13);
        sub.setTextColor(ContextCompat.getColor(c, R.color.colorTextSecondary));
        sub.setPadding(0, dp(c, 4), 0, dp(c, 12));
        root.addView(sub);
        for (CoinCatalog.Coin coin : CoinCatalog.others()) root.addView(toggleRow(c, coin, onChange));
        d.setContentView(root);
        d.show();
    }

    private static View toggleRow(Context c, CoinCatalog.Coin coin, Runnable onChange) {
        LinearLayout row = new LinearLayout(c);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(c, 8), 0, dp(c, 8));
        ImageView icon = new ImageView(c);
        int s = dp(c, 36);
        icon.setLayoutParams(new LinearLayout.LayoutParams(s, s));
        icon.setImageResource(coin.icon);
        row.addView(icon);
        TextView t = new TextView(c);
        t.setText(coin.name);
        t.setTextSize(15);
        t.setTextColor(ContextCompat.getColor(c, R.color.colorTextPrimary));
        t.setPadding(dp(c, 12), 0, 0, 0);
        row.addView(t, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        SwitchMaterial sw = new SwitchMaterial(c);
        sw.setChecked(shown(c, coin.key));
        sw.setOnCheckedChangeListener((b, on) -> { setShown(c, coin.key, on); if (onChange != null) onChange.run(); });
        row.addView(sw);
        row.setOnClickListener(v -> sw.toggle());
        return row;
    }

    /** Draws one card per shown coin. A refresh updates the numbers in place and does not clear the list first. */
    public static void render(Context c, LinearLayout box, String harvAddress) {
        if (box == null || c == null || harvAddress == null || harvAddress.isEmpty()) return;
        final Context app = c.getApplicationContext();
        new Thread(() -> {
            Map<String, String> addrs = CoinCatalog.addresses(app, harvAddress);
            List<CoinCatalog.Coin> show = new ArrayList<>();
            for (CoinCatalog.Coin coin : CoinCatalog.others()) {
                if (!shown(app, coin.key)) continue;
                String a = addrs.get(coin.key);
                if (a != null && !a.isEmpty()) show.add(coin);
            }
            box.post(() -> {
                if (!sameKeys(box, show)) rebuild(c, box, harvAddress, show, addrs);
                else refreshOpens(c, box, harvAddress);
                paintPrices(box);
                for (CoinCatalog.Coin coin : show) refreshBalance(c, box, coin, addrs.get(coin.key));
                AssetPrices.load(c, () -> paintPrices(box));
            });
        }).start();
    }

    private static boolean sameKeys(LinearLayout box, List<CoinCatalog.Coin> show) {
        if (box.getChildCount() != show.size()) return false;
        for (int i = 0; i < show.size(); i++) if (!show.get(i).key.equals(box.getChildAt(i).getTag())) return false;
        return true;
    }

    private static void rebuild(Context c, LinearLayout box, String harvAddress, List<CoinCatalog.Coin> show, Map<String, String> addrs) {
        box.removeAllViews();
        LayoutInflater inf = LayoutInflater.from(c);
        for (CoinCatalog.Coin coin : show) {
            View item = inf.inflate(R.layout.coin_list_card, box, false);
            item.setTag(coin.key);
            ImageView logo = item.findViewById(R.id.iv_asset_logo);
            TextView name = item.findViewById(R.id.tv_asset_name);
            TextView bal = item.findViewById(R.id.tv_asset_balance);
            TextView symbol = item.findViewById(R.id.tv_asset_symbol);
            TextView side = item.findViewById(R.id.tv_asset_amount);
            if (logo != null) logo.setImageResource(coin.icon);
            if (name != null) name.setText(coin.name);
            if (bal != null) {
                bal.setTag("bal_" + coin.key);
                bal.setText("…");
                com.harvana.app.utils.BalancePrivacy.guard(bal);
            }
            if (symbol != null) symbol.setText(coin.ticker);
            AssetPrices.Quote quote = AssetPrices.peek(c, coin.key);
            if (side != null) side.setText(quote != null ? AssetPrices.usd(quote.usd) : coin.network);
            View card = item.findViewById(R.id.cardview1);
            View.OnClickListener open = v -> openFresh(c, harvAddress, coin, bal == null ? "" : bal.getText().toString());
            if (card != null) card.setOnClickListener(open); else item.setOnClickListener(open);
            box.addView(item);
        }
    }

    /** The row stays, but its tap must read the address saved now, not the one it was built with. */
    private static void refreshOpens(Context c, LinearLayout box, String harvAddress) {
        for (int i = 0; i < box.getChildCount(); i++) {
            View item = box.getChildAt(i);
            if (!(item.getTag() instanceof String)) continue;
            CoinCatalog.Coin coin = CoinCatalog.get((String) item.getTag());
            TextView bal = item.findViewById(R.id.tv_asset_balance);
            View.OnClickListener open = v -> openFresh(c, harvAddress, coin, bal == null ? "" : bal.getText().toString());
            View card = item.findViewById(R.id.cardview1);
            if (card != null) card.setOnClickListener(open); else item.setOnClickListener(open);
        }
    }

    private static void openFresh(Context c, String harvAddress, CoinCatalog.Coin coin, String shownBalance) {
        String saved = CoinCatalog.savedAddress(c, harvAddress, coin.key);
        openDetail(c, coin, saved, shownBalance);
    }

    /** Writes the saved price onto each row, and leaves a row alone when its text is already that price. */
    private static void paintPrices(LinearLayout box) {
        if (box == null) return;
        for (int i = 0; i < box.getChildCount(); i++) {
            View item = box.getChildAt(i);
            if (!(item.getTag() instanceof String)) continue;
            TextView side = item.findViewById(R.id.tv_asset_amount);
            AssetPrices.Quote quote = AssetPrices.peek(box.getContext(), (String) item.getTag());
            if (side == null || quote == null) continue;
            String next = AssetPrices.usd(quote.usd);
            if (!next.contentEquals(side.getText())) side.setText(next);
        }
    }

    private static void refreshBalance(Context c, LinearLayout box, CoinCatalog.Coin coin, String address) {
        View bal = box.findViewWithTag("bal_" + coin.key);
        if (!(bal instanceof TextView) || address == null) return;
        final TextView tv = (TextView) bal;
        new Thread(() -> {
            String line = ChainBalances.assetBalance(coin.key, address);
            String number = numberOf(line, coin.ticker);
            tv.post(() -> {
                if (!coin.key.equals(String.valueOf(tv.getTag()).replace("bal_", ""))) return;
                if (!number.isEmpty()) tv.setText(number);
                else if (tv.getText() == null || tv.getText().length() == 0 || "…".equals(tv.getText().toString())) tv.setText(line);
            });
        }).start();
    }

    /** "0.01 BTC" -> "0.01". Leaves a status sentence whole. */
    private static String numberOf(String line, String ticker) {
        if (line == null) return "";
        String t = line.trim();
        if (t.endsWith(" " + ticker)) return t.substring(0, t.length() - ticker.length() - 1).trim();
        return "";
    }

    private static void openDetail(Context c, CoinCatalog.Coin coin, String address, String shownBalance) {
        Intent i = new Intent(c, CoinDetailActivity.class);
        i.putExtra(CoinCatalog.EXTRA, coin.key);
        i.putExtra("walletAddress", address == null ? "" : address);
        i.putExtra("assetBalance", shownBalance == null ? "" : shownBalance);
        if (!(c instanceof android.app.Activity)) i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        c.startActivity(i);
    }

    private static int dp(Context c, int v) { return Math.round(v * c.getResources().getDisplayMetrics().density); }
}
