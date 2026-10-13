package com.harvana.app;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.harvana.app.sync.NodePortfolio;
import com.harvana.app.utils.InfoTable;
import com.harvana.app.utils.UiColors;
import com.harvana.app.wallet.FxRates;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Total saved: everything this person has saved across their groups, in the currency they chose in Settings, then the
 * same money by coin, by group (each in the coin it is kept in, tap to open the group), and their loans. Read from the
 * network through {@link NodePortfolio}.
 */
public class TotalSavedActivity extends AppCompatActivity {

    private LinearLayout page;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(UiColors.background());
        page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(16), dp(8), dp(16), dp(32));
        scroll.addView(page);
        com.harvana.app.utils.AppBar.setContent(this, scroll, "Total saved", null);
        page.addView(com.harvana.app.utils.Shimmer.cards(this, 3));
        load();
    }

    @Override public boolean onSupportNavigateUp() { finish(); return true; }

    private void load() {
        com.google.firebase.auth.FirebaseUser u = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        String wallet = com.harvana.app.wallet.WalletManager.getActiveWalletAddress(this);
        if (u == null) { show(null); return; }
        NodePortfolio.invalidate();
        FxRates.load(this, () -> NodePortfolio.load(this, u.getUid(), wallet, p -> runOnUiThread(() -> show(p))));
    }

    private void show(NodePortfolio.Portfolio p) {
        if (isFinishing()) return;
        page.removeAllViews();
        List<NodePortfolio.Entry> entries = new ArrayList<>();
        if (p != null) synchronized (p.entries) { entries.addAll(p.entries); }

        // ── the total, in the chosen currency ──
        double usd = p == null ? 0 : p.savedUsd(this);
        TextView label = text("TOTAL SAVED · " + FiatValueHelperCode(), 12, true, UiColors.textSecondary());
        label.setLetterSpacing(0.06f);
        page.addView(label);
        TextView big = text(Double.isNaN(usd) ? "…" : FxRates.money(this, usd), 34, true, UiColors.textPrimary());
        big.setPadding(0, dp(4), 0, dp(2));
        page.addView(big);
        int saving = 0;
        for (NodePortfolio.Entry e : entries) if (e.position.savedHarv > 0) saving++;
        page.addView(text(saving + (saving == 1 ? " group" : " groups") + " · " + coinsOf(entries).size()
                + (coinsOf(entries).size() == 1 ? " coin" : " coins"), 13, false, UiColors.textSecondary()));

        if (entries.isEmpty()) {
            page.addView(gap(16));
            page.addView(new InfoTable(this).row("Saved", "0").view());
            return;
        }

        // ── by coin ──
        page.addView(gap(20));
        InfoTable coins = new InfoTable(this);
        coins.section("By coin");
        for (Map.Entry<String, Double> c : coinsOf(entries).entrySet()) {
            String coin = c.getKey();
            String inCoin = coinMoney(coin, c.getValue());
            double per = unitUsd(coin);
            String inMine = Double.isNaN(per) ? "" : "  ·  " + FxRates.money(this, c.getValue() * per);
            coins.row(coinName(coin), inCoin + inMine);
        }
        page.addView(coins.view());

        // ── by group: tap to open ──
        page.addView(gap(16));
        page.addView(sectionTitle("By group"));
        for (NodePortfolio.Entry e : entries) page.addView(groupRow(e));

        // ── loans and standing ──
        double borrowedUsd = 0, owedUsd = 0;
        int open = 0;
        for (NodePortfolio.Entry e : entries) {
            double per = e.usdPerUnit(this);
            if (Double.isNaN(per)) continue;
            borrowedUsd += e.position.borrowedHarv * per;
            owedUsd += e.position.outstandingHarv() * per;
            if (e.position.outstandingHarv() > 0) open++;
        }
        page.addView(gap(16));
        InfoTable loans = new InfoTable(this);
        loans.section("Loans");
        loans.row("Open loans", String.valueOf(open));
        loans.row("Borrowed", FxRates.money(this, borrowedUsd));
        loans.row("Still owed", FxRates.money(this, owedUsd), owedUsd > 0 ? UiColors.warning() : UiColors.textPrimary());
        loans.row("Saved after loans", FxRates.money(this, Double.isNaN(usd) ? 0 : Math.max(0, usd - owedUsd)));
        page.addView(loans.view());

        page.addView(gap(16));
        InfoTable kinds = new InfoTable(this);
        kinds.section("By kind of group");
        Map<String, Integer> byKind = new LinkedHashMap<>();
        for (NodePortfolio.Entry e : entries) byKind.merge(kindName(e.family), 1, Integer::sum);
        for (Map.Entry<String, Integer> k : byKind.entrySet()) kinds.row(k.getKey(), String.valueOf(k.getValue()));
        page.addView(kinds.view());
    }

    /** One group: its name and kind, what is saved in its coin, and the same in the chosen currency. */
    private View groupRow(NodePortfolio.Entry e) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(12), dp(14), dp(12));
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setCornerRadius(dp(14));
        bg.setColor(UiColors.surface());
        row.setBackground(bg);
        LinearLayout words = new LinearLayout(this);
        words.setOrientation(LinearLayout.VERTICAL);
        words.addView(text(e.name, 15, true, UiColors.textPrimary()));
        words.addView(text(kindName(e.family) + " · " + coinName(e.asset), 12, false, UiColors.textSecondary()));
        row.addView(words, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        LinearLayout nums = new LinearLayout(this);
        nums.setOrientation(LinearLayout.VERTICAL);
        nums.setGravity(Gravity.END);
        nums.addView(text(coinMoney(e.asset, e.position.savedHarv), 15, true, UiColors.textPrimary()));
        double per = e.usdPerUnit(this);
        if (!Double.isNaN(per)) nums.addView(text(FxRates.money(this, e.position.savedHarv * per), 12, false, UiColors.textSecondary()));
        if (e.position.outstandingHarv() > 0) nums.addView(text("Owes " + coinMoney(e.asset, e.position.outstandingHarv()), 12, false, UiColors.warning()));
        row.addView(nums);
        android.util.TypedValue tv = new android.util.TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackground, tv, true);
        row.setForeground(getDrawable(tv.resourceId));
        row.setOnClickListener(v -> startActivity(new Intent(this, GroupDetailsActivity.class).putExtra("groupId", e.firestoreGroupId)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(8);
        row.setLayoutParams(lp);
        return row;
    }

    // ── small helpers ──

    private static Map<String, Double> coinsOf(List<NodePortfolio.Entry> entries) {
        Map<String, Double> by = new LinkedHashMap<>();
        for (NodePortfolio.Entry e : entries) {
            if (e.position.savedHarv <= 0) continue;
            by.merge(e.asset == null ? "" : e.asset, e.position.savedHarv, Double::sum);
        }
        return by;
    }

    private double unitUsd(String coin) {
        NodePortfolio.Entry probe = new NodePortfolio.Entry();
        probe.asset = coin;
        return probe.usdPerUnit(this);
    }

    private static String coinName(String coin) {
        return com.harvana.app.husd.Stable.isStable(coin) ? com.harvana.app.husd.Stable.symbol(coin) : CoinInfo.ticker();
    }

    private static String coinMoney(String coin, double units) {
        if (com.harvana.app.husd.Stable.isStable(coin)) {
            return FxRates.format(FxRates.symbolFor(com.harvana.app.husd.Stable.currency(coin)), units);
        }
        return String.format(Locale.US, "%,.2f %s", units, CoinInfo.ticker());
    }

    private static String kindName(String family) {
        return "CIRCLE".equals(family) ? "Rotating" : "ACCUM".equals(family) ? "Savings" : "FIXED".equals(family) ? "Goal" : "Group";
    }

    private String FiatValueHelperCode() {
        return com.harvana.app.wallet.FiatValueHelper.symbolToCurrencyCode(FxRates.symbol(this));
    }

    private TextView sectionTitle(String s) {
        TextView t = text(s.toUpperCase(Locale.US), 11, true, UiColors.textTertiary());
        t.setLetterSpacing(0.06f);
        t.setPadding(dp(4), 0, 0, 0);
        return t;
    }

    private TextView text(String s, float sp, boolean bold, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private View gap(int h) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(h)));
        return v;
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
