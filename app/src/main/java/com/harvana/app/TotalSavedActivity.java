package com.harvana.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
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

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        com.harvana.app.utils.AppBar.setContent(this, R.layout.activity_total_saved, "Total saved", null);
        ((LinearLayout) findViewById(R.id.total_saved_loading)).addView(com.harvana.app.utils.Shimmer.cards(this, 3));
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
        findViewById(R.id.total_saved_loading).setVisibility(View.GONE);
        findViewById(R.id.total_saved_content).setVisibility(View.VISIBLE);
        LinearLayout byCoin = findViewById(R.id.total_saved_by_coin);
        LinearLayout groups = findViewById(R.id.total_saved_groups);
        LinearLayout loansBox = findViewById(R.id.total_saved_loans);
        LinearLayout kindsBox = findViewById(R.id.total_saved_kinds);
        byCoin.removeAllViews(); groups.removeAllViews(); loansBox.removeAllViews(); kindsBox.removeAllViews();
        List<NodePortfolio.Entry> entries = new ArrayList<>();
        if (p != null) synchronized (p.entries) { entries.addAll(p.entries); }

        // ── the total, in the chosen currency ──
        double usd = p == null ? 0 : p.savedUsd(this);
        ((TextView) findViewById(R.id.tv_total_saved_label)).setText("TOTAL SAVED · " + FiatValueHelperCode());
        ((TextView) findViewById(R.id.tv_total_saved_amount)).setText(Double.isNaN(usd) ? "…" : FxRates.money(this, usd));
        int saving = 0;
        for (NodePortfolio.Entry e : entries) if (e.position.savedHarv > 0) saving++;
        int coinCount = coinsOf(entries).size();
        ((TextView) findViewById(R.id.tv_total_saved_counts)).setText(saving + (saving == 1 ? " group" : " groups") + " · "
                + coinCount + (coinCount == 1 ? " coin" : " coins"));

        boolean none = entries.isEmpty();
        findViewById(R.id.total_saved_groups_section).setVisibility(none ? View.GONE : View.VISIBLE);
        if (none) {
            byCoin.addView(new InfoTable(this).row("Saved", "0").view());
            return;
        }

        // ── by coin ──
        InfoTable coins = new InfoTable(this);
        coins.section("By coin");
        for (Map.Entry<String, Double> c : coinsOf(entries).entrySet()) {
            String coin = c.getKey();
            String inCoin = coinMoney(coin, c.getValue());
            double per = unitUsd(coin);
            String inMine = Double.isNaN(per) ? "" : "  ·  " + FxRates.money(this, c.getValue() * per);
            coins.row(com.harvana.app.group.CoinLogo.of(this, coin), coinName(coin), inCoin + inMine);
        }
        byCoin.addView(coins.view());

        // ── by group: tap to open ──
        for (NodePortfolio.Entry e : entries) groups.addView(groupRow(groups, e));

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
        InfoTable loans = new InfoTable(this);
        loans.section("Loans");
        loans.row("Open loans", String.valueOf(open));
        loans.row("Borrowed", FxRates.money(this, borrowedUsd));
        loans.row("Still owed", FxRates.money(this, owedUsd), owedUsd > 0 ? UiColors.warning() : UiColors.textPrimary());
        loans.row("Saved after loans", FxRates.money(this, Double.isNaN(usd) ? 0 : Math.max(0, usd - owedUsd)));
        loansBox.addView(loans.view());

        InfoTable kinds = new InfoTable(this);
        kinds.section("By kind of group");
        Map<String, Integer> byKind = new LinkedHashMap<>();
        for (NodePortfolio.Entry e : entries) byKind.merge(kindName(e.family), 1, Integer::sum);
        for (Map.Entry<String, Integer> k : byKind.entrySet()) kinds.row(k.getKey(), String.valueOf(k.getValue()));
        kindsBox.addView(kinds.view());
    }

    /** One group (res/layout/item_total_saved_group.xml): name and kind, saved in its coin, and the same in the chosen currency. */
    private View groupRow(ViewGroup parent, NodePortfolio.Entry e) {
        View row = getLayoutInflater().inflate(R.layout.item_total_saved_group, parent, false);
        com.harvana.app.group.CoinLogo.show(row.findViewById(R.id.iv_saved_group_coin), e.asset == null ? "" : e.asset);
        ((TextView) row.findViewById(R.id.tv_saved_group_name)).setText(e.name);
        ((TextView) row.findViewById(R.id.tv_saved_group_kind)).setText(kindName(e.family) + " · " + coinName(e.asset));
        ((TextView) row.findViewById(R.id.tv_saved_group_amount)).setText(coinMoney(e.asset, e.position.savedHarv));
        double per = e.usdPerUnit(this);
        TextView fiat = row.findViewById(R.id.tv_saved_group_fiat);
        if (!Double.isNaN(per)) { fiat.setText(FxRates.money(this, e.position.savedHarv * per)); fiat.setVisibility(View.VISIBLE); }
        TextView owes = row.findViewById(R.id.tv_saved_group_owes);
        if (e.position.outstandingHarv() > 0) {
            owes.setText("Owes " + coinMoney(e.asset, e.position.outstandingHarv()));
            owes.setVisibility(View.VISIBLE);
        }
        row.setOnClickListener(v -> startActivity(new Intent(this, GroupDetailsActivity.class).putExtra("groupId", e.firestoreGroupId)));
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
}
