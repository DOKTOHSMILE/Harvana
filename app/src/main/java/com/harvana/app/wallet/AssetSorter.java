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
    /** 24h change in percent, when the price source gives one. */
    private final Map<String, Double> change = new HashMap<>();
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
        for (String k : price.keySet()) sig.append(k).append('=').append(price.get(k)).append(change.get(k)).append(';');
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
        for (View src : order) {
            if (src.getVisibility() != View.VISIBLE) continue;
            shown.addView(row(src));
        }
    }

    private double valueOf(View r) {
        Double v = lastValue.get(key(r));
        return v == null ? 0 : v;
    }

    /**
     * One asset: icon · TICKER [Name] over price and 24h change on the left; amount over value on the right.
     */
    private View row(View src) {
        float d = c.getResources().getDisplayMetrics().density;
        String key = priceKey(src);
        boolean stable = com.harvana.app.husd.Stable.isStable(key);
        String ticker = stable ? com.harvana.app.husd.Stable.symbol(key) : text(src, R.id.tv_asset_symbol);
        if (ticker.isEmpty()) ticker = key;
        String name = stable ? com.harvana.app.husd.Stable.name(key) : text(src, R.id.tv_asset_name);
        if ("HARV".equals(key)) name = "Harvana";

        LinearLayout card = new LinearLayout(c);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(px(d, 14), px(d, 12), px(d, 14), px(d, 12));
        card.setMinimumHeight(px(d, 68));
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setCornerRadius(px(d, 16));
        bg.setColor(com.harvana.app.utils.UiColors.surface());
        bg.setStroke(Math.max(1, px(d, 1)), c.getResources().getColor(R.color.colorCardStroke));
        card.setBackground(bg);
        card.setClickable(true);
        card.setFocusable(true);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clp.setMargins(px(d, 12), px(d, 4), px(d, 12), px(d, 4));
        card.setLayoutParams(clp);

        ImageView iv = new ImageView(c);
        Drawable dr = logo(src);
        if (dr != null) {
            Drawable.ConstantState cs = dr.getConstantState();
            iv.setImageDrawable(cs != null ? cs.newDrawable(c.getResources()).mutate() : dr);
        }
        card.addView(iv, new LinearLayout.LayoutParams(px(d, 40), px(d, 40)));

        LinearLayout mid = new LinearLayout(c);
        mid.setOrientation(LinearLayout.VERTICAL);
        mid.setPadding(px(d, 12), 0, px(d, 8), 0);
        LinearLayout top = new LinearLayout(c);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView tk = tv(ticker, 16, true, com.harvana.app.utils.UiColors.textPrimary());
        top.addView(tk);
        if (name != null && !name.isEmpty() && !name.equalsIgnoreCase(ticker)) {
            TextView chip = tv(name, 11.5f, false, com.harvana.app.utils.UiColors.textSecondary());
            chip.setMaxLines(1);
            chip.setEllipsize(android.text.TextUtils.TruncateAt.END);
            chip.setPadding(px(d, 8), px(d, 2), px(d, 8), px(d, 2));
            android.graphics.drawable.GradientDrawable cb = new android.graphics.drawable.GradientDrawable();
            cb.setCornerRadius(px(d, 10));
            cb.setColor(com.harvana.app.utils.UiColors.surfaceAlt());
            chip.setBackground(cb);
            LinearLayout.LayoutParams chl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            chl.leftMargin = px(d, 8);
            top.addView(chip, chl);
        }
        mid.addView(top);
        LinearLayout bottom = new LinearLayout(c);
        bottom.setGravity(Gravity.CENTER_VERTICAL);
        String p = price.get(key);
        bottom.addView(tv(p == null ? "—" : p, 13, false, com.harvana.app.utils.UiColors.textSecondary()));
        Double ch = change.get(key);
        if (ch != null && !ch.isNaN()) {
            TextView ct = tv(String.format(java.util.Locale.US, "%s%.2f%%", ch >= 0 ? "+" : "", ch), 13, false,
                    ch >= 0 ? com.harvana.app.utils.UiColors.success() : com.harvana.app.utils.UiColors.error());
            ct.setPadding(px(d, 6), 0, 0, 0);
            bottom.addView(ct);
        }
        mid.addView(bottom);
        card.addView(mid, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout right = new LinearLayout(c);
        right.setOrientation(LinearLayout.VERTICAL);
        right.setGravity(Gravity.END);
        TextView amt = tv(text(src, R.id.tv_asset_balance), 16, true, com.harvana.app.utils.UiColors.textPrimary());
        amt.setGravity(Gravity.END);
        right.addView(amt);
        TextView val = tv(text(src, R.id.tv_asset_amount), 13, false, com.harvana.app.utils.UiColors.textSecondary());
        val.setGravity(Gravity.END);
        right.addView(val);
        card.addView(right);

        View realCard = src.findViewById(R.id.cardview1);
        card.setOnClickListener(v -> { if (realCard != null) realCard.performClick(); else src.performClick(); });
        return card;
    }

    private TextView tv(String s, float sp, boolean bold, int color) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setMaxLines(1);
        if (bold) t.setTypeface(null, android.graphics.Typeface.BOLD);
        return t;
    }

    private static int px(float d, int dp) { return Math.round(dp * d); }

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
            // HARV: the swap pools' price and its 24-hour change, in the person's currency.
            HarvPrice.get(q -> {
                double usd = HarvPrice.usd(c, q);
                if (Double.isNaN(usd)) return;
                price.put("HARV", FxRates.money(c, usd));
                if (!Double.isNaN(q.changePct)) change.put("HARV", q.changePct);
                lastSignature = "";
                sync();
            });
            // Other coins: market prices.
            AssetPrices.load(c, () -> {
                for (CoinCatalog.Coin coin : CoinCatalog.others()) {
                    AssetPrices.Quote q = AssetPrices.peek(c, coin.key);
                    if (q != null && q.usd > 0) {
                        price.put(coin.key.toUpperCase(java.util.Locale.US), FxRates.money(c, q.usd));
                        change.put(coin.key.toUpperCase(java.util.Locale.US), q.change);
                    }
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
