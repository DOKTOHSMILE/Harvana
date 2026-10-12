package com.harvana.app.wallet;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.harvana.app.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Assets list as one list, largest holding first, with each coin's market price on its row.
 *
 * The rows are still drawn by their own sections (HARV, the stable coins, the other coins, the ones added by hand),
 * each into its own box, exactly as before; those boxes are hidden. This shows one copy of every row, sorted by its
 * value in the person's currency (the figure on the right), and a tap on a copy taps the real row. Whenever a section
 * changes a row (a balance arrives, the search hides it, privacy masks it), the copies are redrawn.
 */
public final class AssetSorter {

    private final Context c;
    private final LinearLayout shown;
    private final ViewGroup[] sources;
    private String lastSignature = "";
    /** Last readable value per row, so a masked (privacy) balance keeps its place. */
    private final Map<String, Double> lastValue = new HashMap<>();
    /** Price per coin in the person's currency, once known. */
    private final Map<String, String> price = new HashMap<>();
    private boolean pricesAsked;

    private AssetSorter(Context c, LinearLayout shown, ViewGroup[] sources) {
        this.c = c;
        this.shown = shown;
        this.sources = sources;
    }

    /** Puts the sorted list where the first source box is, hides the boxes, and keeps the list in step with them. */
    public static AssetSorter attach(ViewGroup first, ViewGroup... others) {
        if (first == null || !(first.getParent() instanceof ViewGroup)) return null;
        ViewGroup column = (ViewGroup) first.getParent();
        LinearLayout shown = new LinearLayout(first.getContext());
        shown.setOrientation(LinearLayout.VERTICAL);
        column.addView(shown, column.indexOfChild(first), new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        ViewGroup[] all = new ViewGroup[others.length + 1];
        all[0] = first;
        System.arraycopy(others, 0, all, 1, others.length);
        for (ViewGroup g : all) if (g != null) g.setVisibility(View.GONE);
        AssetSorter s = new AssetSorter(first.getContext(), shown, all);
        column.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override public void onGlobalLayout() { s.sync(); }
        });
        s.sync();
        return s;
    }

    /** Redraws the list when any row changed. Cheap when nothing did. */
    public void sync() {
        List<View> rows = new ArrayList<>();
        StringBuilder sig = new StringBuilder();
        for (ViewGroup g : sources) {
            if (g == null) continue;
            for (int i = 0; i < g.getChildCount(); i++) {
                View r = g.getChildAt(i);
                if (r.findViewById(R.id.tv_asset_name) == null) continue;
                rows.add(r);
                sig.append(key(r)).append('|').append(r.getVisibility()).append('|')
                        .append(text(r, R.id.tv_asset_name)).append('|').append(text(r, R.id.tv_asset_balance)).append('|')
                        .append(text(r, R.id.tv_asset_amount)).append('|').append(System.identityHashCode(logo(r))).append(';');
            }
        }
        for (String k : price.keySet()) sig.append(k).append('=').append(price.get(k)).append(';');
        String s = sig.toString();
        if (s.equals(lastSignature)) return;
        lastSignature = s;
        if (!pricesAsked) { pricesAsked = true; loadPrices(); }

        // Largest value first; equal values keep the sections' own order (HARV, stable coins, other coins, added).
        List<View> order = new ArrayList<>(rows);
        for (View r : rows) {
            Double v = money(text(r, R.id.tv_asset_amount));
            if (v != null) lastValue.put(key(r), v);
        }
        java.util.Collections.sort(order, (a, b) -> Double.compare(valueOf(b), valueOf(a)));   // a stable sort

        shown.removeAllViews();
        LayoutInflater inf = LayoutInflater.from(c);
        for (View src : order) {
            if (src.getVisibility() != View.VISIBLE) continue;
            View copy = inf.inflate(R.layout.coin_list_card, shown, false);
            fill(copy, src);
            shown.addView(copy);
        }
    }

    private double valueOf(View r) {
        Double v = lastValue.get(key(r));
        return v == null ? 0 : v;
    }

    private void fill(View copy, View src) {
        copyText(copy, src, R.id.tv_asset_name);
        copyText(copy, src, R.id.tv_asset_balance);
        copyText(copy, src, R.id.tv_asset_symbol);
        copyText(copy, src, R.id.tv_asset_amount);
        Drawable d = logo(src);
        ImageView iv = copy.findViewById(R.id.iv_asset_logo);
        if (iv != null && d != null) {
            Drawable.ConstantState cs = d.getConstantState();
            iv.setImageDrawable(cs != null ? cs.newDrawable(c.getResources()).mutate() : d);
        }
        // The price, on the name line at the right.
        String p = price.get(priceKey(src));
        TextView name = copy.findViewById(R.id.tv_asset_name);
        if (p != null && name != null && name.getParent() instanceof LinearLayout) {
            LinearLayout details = (LinearLayout) name.getParent();
            int at = details.indexOfChild(name);
            details.removeView(name);
            LinearLayout line = new LinearLayout(c);
            line.setGravity(Gravity.CENTER_VERTICAL);
            line.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            TextView pt = new TextView(c);
            pt.setText(p);
            pt.setTextSize(12f);
            pt.setTextColor(c.getResources().getColor(R.color.colorTextSecondary));
            pt.setGravity(Gravity.END);
            line.addView(pt);
            details.addView(line, at);
        }
        View realCard = src.findViewById(R.id.cardview1);
        View copyCard = copy.findViewById(R.id.cardview1);
        View tap = copyCard != null ? copyCard : copy;
        tap.setOnClickListener(v -> { if (realCard != null) realCard.performClick(); else src.performClick(); });
    }

    // ── prices ────────────────────────────────────────────────────────────────

    /** "HARV", a stable code ("HNGN") or a coin key ("ETH"). */
    private static String priceKey(View r) {
        Object tag = r.getTag();
        if (tag instanceof String && !((String) tag).isEmpty()) return ((String) tag).toUpperCase(java.util.Locale.US);
        return text(r, R.id.tv_asset_symbol).toUpperCase(java.util.Locale.US);
    }

    private void loadPrices() {
        FxRates.load(c, () -> {
            // Stable coins: one coin is one unit of its currency.
            for (String code : com.harvana.app.husd.Stable.CODES) {
                double per = FxRates.perUsd(c, com.harvana.app.husd.Stable.currency(code));
                if (!Double.isNaN(per) && per > 0) price.put(code, FxRates.money(c, 1d / per));
            }
            // HARV: the guarded price from completed P2P trades (naira), shown in the person's currency.
            com.harvana.app.p2p.P2pPriceReference.fetch((kobo, n) -> shown.post(() -> {
                double ngnPerUsd = FxRates.perUsd(c, "NGN");
                if (kobo > 0 && !Double.isNaN(ngnPerUsd) && ngnPerUsd > 0) {
                    price.put("HARV", FxRates.money(c, (kobo / 100d) / ngnPerUsd));
                    lastSignature = "";
                    sync();
                }
            }));
            // Other coins: market prices.
            AssetPrices.load(c, () -> {
                for (CoinCatalog.Coin coin : CoinCatalog.others()) {
                    AssetPrices.Quote q = AssetPrices.peek(c, coin.key);
                    if (q != null && q.usd > 0) price.put(coin.key.toUpperCase(java.util.Locale.US), FxRates.money(c, q.usd));
                }
                lastSignature = "";
                sync();
            });
            lastSignature = "";
            sync();
        });
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static String key(View r) {
        Object tag = r.getTag();
        return (tag instanceof String ? (String) tag : "") + "/" + text(r, R.id.tv_asset_name) + "/" + text(r, R.id.tv_asset_symbol);
    }

    private static String text(View r, int id) {
        View v = r.findViewById(id);
        return v instanceof TextView && ((TextView) v).getText() != null ? ((TextView) v).getText().toString() : "";
    }

    private static Drawable logo(View r) {
        View v = r.findViewById(R.id.iv_asset_logo);
        return v instanceof ImageView ? ((ImageView) v).getDrawable() : null;
    }

    private static void copyText(View copy, View src, int id) {
        View a = copy.findViewById(id), b = src.findViewById(id);
        if (a instanceof TextView && b instanceof TextView) {
            ((TextView) a).setText(((TextView) b).getText());
            ((TextView) a).setTextColor(((TextView) b).getCurrentTextColor());
        }
    }

    /** "₦9,930.05" → 9930.05; null when there is no number (still loading, or hidden by privacy). */
    static Double money(String s) {
        if (s == null) return null;
        String digits = s.replaceAll("[^0-9.]", "");
        if (digits.isEmpty() || digits.equals(".")) return null;
        try { return Double.parseDouble(digits); } catch (NumberFormatException e) { return null; }
    }
}
