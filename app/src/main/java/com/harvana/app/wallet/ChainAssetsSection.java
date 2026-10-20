package com.harvana.app.wallet;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.harvana.app.CoinDetailActivity;
import com.harvana.app.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The other coins under HARV on the Assets tab. Each coin is its own row, in the same list as HARV,
 * and opens that coin's detail page. The member can hide a coin with "Add coins", or add a chain or token of their
 * own there (CustomAssets).
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

    static void setShown(Context c, String key, boolean on) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(key, on).apply();
    }

    /** Opens "Add coins" (its own screen, ManageAssetsActivity); {@code onChange} runs when it closes after a change. */
    public static void showPicker(Context c, Runnable onChange) {
        ManageAssetsActivity.open(c, onChange);
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
                // Every balance and price comes from one AssetBook: the last book at once, then all new values together.
                AssetBook book = AssetBook.of(harvAddress);
                apply(box, book.cached(c));
                book.listen("chains", (all, fresh) -> apply(box, all));
                book.refresh(c);
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
            if (side != null) {
                side.setText("…");
                com.harvana.app.utils.BalancePrivacy.guard(side);
            }
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

    /** Draws each holding in the currency the person chose, then adds them up. */
    public static void repaint(LinearLayout box) {
        if (box == null) return;
        FxRates.load(box.getContext(), () -> paintPrices(box));
    }

    public interface Total { void onSum(double fiat); }

    public static void onTotal(LinearLayout box, Total sum) {
        if (box != null) box.setTag(R.id.btn_add_coins, sum);
    }

    /** Writes the value of what is held, not the price of one coin. A row still loading is left as "…". */
    private static void paintPrices(LinearLayout box) {
        if (box == null) return;
        double sum = 0;
        boolean any = false;
        for (int i = 0; i < box.getChildCount(); i++) {
            View item = box.getChildAt(i);
            if (!(item.getTag() instanceof String)) continue;
            TextView side = item.findViewById(R.id.tv_asset_amount);
            TextView bal = item.findViewById(R.id.tv_asset_balance);
            if (side == null) continue;
            Object heldObj = bal == null ? null : bal.getTag(R.id.tv_asset_balance);
            if (!(heldObj instanceof Double)) continue;
            AssetPrices.Quote quote = AssetPrices.peek(box.getContext(), (String) item.getTag());
            if (quote == null) continue;
            double usd = ((Double) heldObj) * quote.usd;
            double fiat = FxRates.fiat(box.getContext(), usd);
            String next = FxRates.money(box.getContext(), usd);
            if (!next.contentEquals(side.getText())) side.setText(next);
            if (!Double.isNaN(fiat)) { sum += fiat; any = true; }
        }
        Object listener = box.getTag(R.id.btn_add_coins);
        if (any && listener instanceof Total) ((Total) listener).onSum(sum);
    }

    /** Writes every row's balance from the book, then the values and the total, in one pass. */
    static void apply(LinearLayout box, Map<String, String> book) {
        if (box == null) return;
        for (int i = 0; i < box.getChildCount(); i++) {
            View item = box.getChildAt(i);
            if (!(item.getTag() instanceof String)) continue;
            String line = book.get((String) item.getTag());
            TextView tv = item.findViewById(R.id.tv_asset_balance);
            if (tv == null || line == null) continue;
            String number = numberOf(line);
            if (number.isEmpty()) continue;
            if (!number.contentEquals(tv.getText())) tv.setText(number);
            Double amt = AssetPrices.amount(number);
            if (amt != null) tv.setTag(R.id.tv_asset_balance, amt);
        }
        paintPrices(box);
    }

    /** Extracts a clean numeric balance or returns empty string if invalid. */
    private static String numberOf(String line) {
        if (line == null) return "";
        String t = line.trim();
        String lower = t.toLowerCase();
        
        if (lower.contains("unavailable") || lower.contains("error") || lower.contains("fail") || lower.contains("loading")) {
            return "";
        }
        
        String[] parts = t.split("\\s+");
        if (parts.length > 0) {
            String cleanNumber = parts[0].replaceAll("[^0-9.]", "");
            if (!cleanNumber.isEmpty() && !cleanNumber.equals(".")) {
                try {
                    Double.parseDouble(cleanNumber);
                    return cleanNumber;
                } catch (NumberFormatException ignored) {}
            }
        }
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
