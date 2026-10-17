package com.harvana.app.wallet;

import android.content.Context;
import android.graphics.drawable.Drawable;
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
    private final String scope;                 // the wallet this list belongs to (the saved copy is per wallet)
    private final long attachedAt = System.currentTimeMillis();
    private String lastSignature = "";
    /** Last readable value per row, so a masked (privacy) or still-loading balance keeps its place. */
    private final Map<String, Double> lastValue = new HashMap<>();
    /** Last readable balance and value text per row, shown while a section is still loading. */
    private final Map<String, String[]> lastText = new HashMap<>();
    /** Price per coin in the person's currency, once known. */
    /** Price in US dollars; shown in the person's currency when the row is drawn, so a currency change shows at once. */
    private final Map<String, Double> price = new HashMap<>();
    /** 24h change in percent, when the price source gives one. */
    private final Map<String, Double> change = new HashMap<>();
    /** Rows from the saved copy, drawn until their section has loaded (only for the first moments). */
    private final List<Entry> saved = new ArrayList<>();
    /** What is on screen now, in order, with each row's views (updated in place, not rebuilt). */
    private final List<String> onScreen = new ArrayList<>();
    private final Map<String, Holder> holders = new HashMap<>();
    private boolean pricesAsked;
    private final Runnable rebuild = this::rebuildNow;

    /** Rows drawn from the saved copy are kept this long at most while their sections load. */
    private static final long SAVED_GRACE_MS = 4000;
    private static final String PREFS = "asset_list_cache";

    private AssetSorter(Context c, LinearLayout shown, ViewGroup[] sources) {
        this.c = c;
        this.shown = shown;
        this.sources = sources;
        this.scope = c.getSharedPreferences("harv_wallet_prefs", Context.MODE_PRIVATE).getString("active_wallet_address", "").trim();
        readCache();
    }

    /** Puts the sorted list where the first source box is, hides the boxes, and keeps the list in step with them. */
    public static AssetSorter attach(ViewGroup first, ViewGroup... others) {
        if (first == null || !(first.getParent() instanceof ViewGroup)) return null;
        ViewGroup column = (ViewGroup) first.getParent();
        LinearLayout shown = new LinearLayout(first.getContext());
        shown.setOrientation(LinearLayout.VERTICAL);
        shown.setLayoutTransition(null);
        column.addView(shown, column.indexOfChild(first), new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        ViewGroup[] all = new ViewGroup[others.length + 1];
        all[0] = first;
        System.arraycopy(others, 0, all, 1, others.length);
        for (ViewGroup g : all) if (g != null) g.setVisibility(View.GONE);
        AssetSorter s = new AssetSorter(first.getContext(), shown, all);
        column.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override public void onGlobalLayout() { s.sync(); }
        });
        s.rebuildNow();                                    // the saved copy, at once
        return s;
    }

    /** Notes a change; the list is redrawn once the burst of changes settles (sections load one after another). */
    public void sync() {
        String s = signature();
        if (s.equals(lastSignature)) return;
        lastSignature = s;
        if (!pricesAsked) { pricesAsked = true; loadPrices(); }
        shown.removeCallbacks(rebuild);
        shown.postDelayed(rebuild, 150);
    }

    private String signature() {
        StringBuilder sig = new StringBuilder();
        for (ViewGroup g : sources) {
            if (g == null) continue;
            for (int i = 0; i < g.getChildCount(); i++) {
                View r = g.getChildAt(i);
                if (r.findViewById(R.id.tv_asset_name) == null) continue;
                sig.append(key(r)).append('|').append(r.getVisibility()).append('|')
                        .append(text(r, R.id.tv_asset_balance)).append('|')
                        .append(text(r, R.id.tv_asset_amount)).append('|').append(System.identityHashCode(logo(r))).append(';');
            }
        }
        sig.append(FxRates.symbol(c)).append(';');
        for (String k : price.keySet()) sig.append(k).append('=').append(price.get(k)).append(change.get(k)).append(';');
        return sig.toString();
    }

    /** One row of the list: from a section's row, or from the saved copy. */
    private static final class Entry {
        String key, priceKey, ticker, name, balance, value;
        double sort;
        int order;
        View src;               // null for a row from the saved copy
        Drawable icon;
    }

    private static final class Holder {
        View card; ImageView icon; TextView ticker, chip, price, change, balance, value; View divider;
        View src;
    }

    private void rebuildNow() {
        List<Entry> entries = new ArrayList<>();
        java.util.Set<String> real = new java.util.HashSet<>();
        int n = 0;
        for (ViewGroup g : sources) {
            if (g == null) continue;
            for (int i = 0; i < g.getChildCount(); i++) {
                View r = g.getChildAt(i);
                if (r.findViewById(R.id.tv_asset_name) == null) continue;
                String k = key(r);
                real.add(k);
                if (r.getVisibility() != View.VISIBLE) continue;
                Entry e = fromRow(r);
                // Drop the native HARV row: it duplicates the HARV card the rest of the list already shows.
                if ("HARV".equals(e.priceKey)) continue;
                e.order = n++;
                entries.add(e);
            }
        }
        boolean early = System.currentTimeMillis() - attachedAt < SAVED_GRACE_MS;
        if (early) {
            boolean hidden = com.harvana.app.utils.BalancePrivacy.isHidden(c);
            for (Entry e : saved) {
                if (real.contains(e.key)) continue;           // its section has drawn it (or hidden it)
                if ("HARV".equals(e.priceKey)) continue;       // drop the saved HARV copy for the same reason
                e.order = n++;
                if (hidden) { e.balance = "\u2022\u2022\u2022\u2022"; e.value = "\u2022\u2022\u2022\u2022"; }
                entries.add(e);
            }
            if (!saved.isEmpty()) {                            // drop the leftovers once the grace time is over
                shown.removeCallbacks(rebuild);
                shown.postDelayed(rebuild, SAVED_GRACE_MS - (System.currentTimeMillis() - attachedAt) + 50);
            }
        }
        // Rank by value (largest first); rows with no value rank by their balance; zero balances rank by name.
        java.util.Collections.sort(entries, (a, b) -> {
            boolean aHasValue = a.sort > 0;
            boolean bHasValue = b.sort > 0;
            if (aHasValue != bHasValue) return aHasValue ? -1 : 1;
            if (aHasValue) {
                int cmp = Double.compare(b.sort, a.sort);
                if (cmp != 0) return cmp;
            } else {
                Double aAmt = money(a.balance);
                Double bAmt = money(b.balance);
                boolean aHasAmt = aAmt != null && aAmt > 0;
                boolean bHasAmt = bAmt != null && bAmt > 0;
                if (aHasAmt != bHasAmt) return aHasAmt ? -1 : 1;
                if (aHasAmt) {
                    int cmp = Double.compare(bAmt, aAmt);
                    if (cmp != 0) return cmp;
                }
            }
            String an = a.name == null ? "" : a.name;
            String bn = b.name == null ? "" : b.name;
            int cmp = an.compareToIgnoreCase(bn);
            return cmp != 0 ? cmp : Integer.compare(a.order, b.order);
        });

        List<String> keys = new ArrayList<>();
        for (Entry e : entries) keys.add(e.key);
        if (keys.equals(onScreen)) {
            for (Entry e : entries) bind(holders.get(e.key), e);      // same rows, same order: update in place
        } else {
            shown.removeAllViews();
            Map<String, Holder> keep = new HashMap<>(holders);
            holders.clear();
            for (int i = 0; i < entries.size(); i++) {
                Entry e = entries.get(i);
                Holder h = keep.get(e.key);
                if (h == null) h = make();
                bind(h, e);
                // Show the divider between rows; hide it under the last row so the list does not end with a line.
                if (h.divider != null) h.divider.setVisibility(i < entries.size() - 1 ? View.VISIBLE : View.GONE);
                holders.put(e.key, h);
                shown.addView(h.card);
            }
            onScreen.clear();
            onScreen.addAll(keys);
        }
        if (!real.isEmpty()) writeCache(entries);
    }

    private Entry fromRow(View src) {
        Entry e = new Entry();
        e.src = src;
        e.key = key(src);
        e.priceKey = priceKey(src);
        boolean stable = com.harvana.app.husd.Stable.isStable(e.priceKey);
        e.ticker = stable ? com.harvana.app.husd.Stable.symbol(e.priceKey) : text(src, R.id.tv_asset_symbol);
        if (e.ticker.isEmpty()) e.ticker = e.priceKey;
        e.name = stable ? com.harvana.app.husd.Stable.name(e.priceKey) : text(src, R.id.tv_asset_name);
        if ("HARV".equals(e.priceKey)) e.name = "Harvana";
        String bal = text(src, R.id.tv_asset_balance), val = text(src, R.id.tv_asset_amount);
        String[] old = lastText.get(e.key);
        // A masked figure (privacy, "••••") is shown as it is; only an empty or loading one uses the last figures.
        boolean balOk = money(bal) != null || bal.contains("\u2022"), valOk = money(val) != null || val.contains("\u2022");
        // A section still loading shows a placeholder: keep the last figures instead of flashing it.
        e.balance = balOk || old == null ? bal : old[0];
        e.value = valOk || old == null ? val : old[1];
        if (money(bal) != null && money(val) != null) lastText.put(e.key, new String[]{bal, val});
        Double v = money(val);
        if (v != null) lastValue.put(e.key, v);
        Double lv = lastValue.get(e.key);
        e.sort = lv == null ? 0 : lv;
        Drawable dr = logo(src);
        if (dr != null) {
            Drawable.ConstantState cs = dr.getConstantState();
            e.icon = cs != null ? cs.newDrawable(c.getResources()).mutate() : dr;
        }
        return e;
    }

    /** The views of one row: simple list row (icon · name over balance + symbol · value), no card surrounding. */
    private Holder make() {
        Holder h = new Holder();
        // Inflate the same plain list row the rest of the app uses: a single tappable row with a thin divider
        // under it, no rounded card, no horizontal margin. One row layout for every asset.
        View row = LayoutInflater.from(c).inflate(R.layout.coin_list_card, shown, false);
        h.card = row;
        h.icon = (ImageView) row.findViewById(R.id.iv_asset_logo);
        h.ticker = (TextView) row.findViewById(R.id.tv_asset_name);  // row 1: asset name
        h.balance = (TextView) row.findViewById(R.id.tv_asset_balance);
        h.chip = (TextView) row.findViewById(R.id.tv_asset_symbol);   // row 2: symbol next to balance
        h.value = (TextView) row.findViewById(R.id.tv_asset_amount);
        h.price = new TextView(c);                                    // unused, kept so existing code compiles
        h.change = new TextView(c);
        h.divider = row.findViewById(R.id.asset_divider);
        // Tappable area is the row itself (not the divider underneath), the same as every other section.
        View tap = row.findViewById(R.id.cardview1);
        if (tap == null) tap = row;
        tap.setOnClickListener(v -> {
            if (h.src == null) return;                         // a saved row: its section has not drawn it yet
            View realCard = h.src.findViewById(R.id.cardview1);
            if (realCard != null) realCard.performClick(); else h.src.performClick();
        });
        return h;
    }

    private void bind(Holder h, Entry e) {
        if (h == null) return;
        h.src = e.src;
        if (e.icon != null) h.icon.setImageDrawable(e.icon);
        else if (h.icon.getDrawable() == null) h.icon.setImageDrawable(savedIcon(e.priceKey));
        // Row 1: the name people read (e.g. "Harvana"); when the name is missing, fall back to the ticker.
        String displayName = (e.name != null && !e.name.isEmpty()) ? e.name : e.ticker;
        set(h.ticker, displayName);
        // Row 2: balance · symbol on the left, value on the right.
        set(h.balance, e.balance);
        set(h.chip, e.ticker);
        set(h.value, e.value);
    }

    /** Only touches the view when the text really changed (no relayout, no flicker). */
    private static void set(TextView t, String s) {
        String v = s == null ? "" : s;
        if (!v.contentEquals(t.getText())) t.setText(v);
    }

    /** The icon for a row from the saved copy, before its section draws the real one. */
    private Drawable savedIcon(String priceKey) {
        try {
            if ("HARV".equals(priceKey)) return c.getResources().getDrawable(R.drawable.ic_harv_coin);
            if (com.harvana.app.husd.Stable.isStable(priceKey)) return com.harvana.app.husd.StableIcon.drawable(c, priceKey);
            CoinCatalog.Coin coin = CoinCatalog.get(priceKey);
            if (coin != null && coin.icon != 0) return c.getResources().getDrawable(coin.icon);
        } catch (Exception ignored) { }
        return null;
    }

    // ── the saved copy ────────────────────────────────────────────────────────

    private void readCache() {
        if (scope.isEmpty()) return;
        try {
            org.json.JSONObject j = new org.json.JSONObject(c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(scope, "{}"));
            if (!FxRates.symbol(c).equals(j.optString("sym"))) return;   // saved in another currency: wait for fresh figures
            org.json.JSONObject pr = j.optJSONObject("usd"), chg = j.optJSONObject("change");
            if (pr != null) for (java.util.Iterator<String> it = pr.keys(); it.hasNext(); ) {
                String k = it.next();
                double v = pr.optDouble(k);
                if (!Double.isNaN(v)) price.put(k, v);
            }
            if (chg != null) for (java.util.Iterator<String> it = chg.keys(); it.hasNext(); ) { String k = it.next(); change.put(k, chg.optDouble(k)); }
            org.json.JSONArray rows = j.optJSONArray("rows");
            for (int i = 0; rows != null && i < rows.length(); i++) {
                org.json.JSONObject o = rows.optJSONObject(i);
                if (o == null) continue;
                Entry e = new Entry();
                e.key = o.optString("key");
                e.priceKey = o.optString("pk");
                e.ticker = o.optString("t");
                e.name = o.optString("n");
                e.balance = o.optString("b");
                e.value = o.optString("v");
                e.sort = o.optDouble("s", 0);
                e.icon = savedIcon(e.priceKey);
                saved.add(e);
                lastValue.put(e.key, e.sort);
                lastText.put(e.key, new String[]{e.balance, e.value});
            }
        } catch (Exception ignored) { }
    }

    private void writeCache(List<Entry> entries) {
        if (scope.isEmpty()) return;
        try {
            org.json.JSONObject j = new org.json.JSONObject();
            org.json.JSONObject pr = new org.json.JSONObject(), chg = new org.json.JSONObject();
            for (Map.Entry<String, Double> m : price.entrySet()) if (m.getValue() != null && !m.getValue().isNaN()) pr.put(m.getKey(), m.getValue());
            for (Map.Entry<String, Double> m : change.entrySet()) if (m.getValue() != null && !m.getValue().isNaN()) chg.put(m.getKey(), m.getValue());
            org.json.JSONArray rows = new org.json.JSONArray();
            for (Entry e : entries) {
                String[] t = lastText.get(e.key);                    // real figures only, never a masked one
                if (e.src == null || t == null) continue;
                rows.put(new org.json.JSONObject().put("key", e.key).put("pk", e.priceKey).put("t", e.ticker).put("n", e.name)
                        .put("b", t[0]).put("v", t[1]).put("s", e.sort));
            }
            j.put("sym", FxRates.symbol(c)).put("usd", pr).put("change", chg).put("rows", rows);
            c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(scope, j.toString()).apply();
        } catch (Exception ignored) { }
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
                if (!Double.isNaN(per) && per > 0) price.put(code, 1d / per);
            }
            // HARV: the swap pools' price and its 24-hour change, in the person's currency.
            HarvPrice.get(q -> {
                double usd = HarvPrice.usd(c, q);
                if (Double.isNaN(usd)) return;
                price.put("HARV", usd);
                if (!Double.isNaN(q.changePct)) change.put("HARV", q.changePct);
                lastSignature = "";
                sync();
            });
            // Other coins: market prices.
            AssetPrices.load(c, () -> {
                java.util.List<CoinCatalog.Coin> priced = new java.util.ArrayList<>(CoinCatalog.others());
                priced.addAll(CoinCatalog.addedCoins());
                for (CoinCatalog.Coin coin : priced) {
                    AssetPrices.Quote q = AssetPrices.peek(c, coin.key);
                    if (q != null && q.usd > 0) {
                        price.put(coin.key.toUpperCase(java.util.Locale.US), q.usd);
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
