package com.harvana.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.harvana.app.firestore.GroupFirestoreManager;
import com.harvana.app.firestore.UserProfileManager;
import com.harvana.app.utils.UiColors;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/** Every active loan the user has, across all their groups, with a Pay button that opens that group's Loans tab. */
public class OpenLoansActivity extends AppCompatActivity {
    private LinearLayout list;

    private static final class Row {
        String groupId, groupName, nodeId = "", groupPhoto = "", groupType = "", asset = "";
        double principal, repaid;
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        com.harvana.app.utils.AppBar.setContent(this, R.layout.activity_list_screen, "Open loans", "Loans you have taken across your groups");
        list = findViewById(R.id.list_screen_items);
        list.addView(com.harvana.app.utils.Shimmer.rows(this, 3));
    }

    @Override protected void onResume() { super.onResume(); load(); }
    @Override public boolean onSupportNavigateUp() { finish(); return true; }

    @SuppressWarnings("unchecked")
    private void load() {
        FirebaseAuth au = FirebaseAuth.getInstance();
        if (au.getCurrentUser() == null) return;
        final String uid = au.getCurrentUser().getUid();
        UserProfileManager.getInstance(this).getProfile(uid, new UserProfileManager.DocCallback() {
            @Override public void onSuccess(Map<String, Object> profile) {
                List<String> ids = com.harvana.app.firestore.UserProfileManager.groupIds(profile.get("groups"));
                fetch(uid, ids);
            }
            @Override public void onError(String e) { runOnUiThread(() -> showError(e)); }
        });
    }

    private void fetch(String uid, List<String> ids) {
        // Groups on the node: what you owe comes from your member record on the node. Older groups: Firestore loans.
        String wallet = com.harvana.app.wallet.WalletManager.getActiveWalletAddress(this);
        com.harvana.app.sync.NodePortfolio.load(this, uid, wallet, portfolio -> {
            final List<Row> rows = new ArrayList<>();
            synchronized (portfolio.entries) {
                for (com.harvana.app.sync.NodePortfolio.Entry e : portfolio.entries) {
                    if (e.position.outstandingHarv() <= 0) continue;
                    Row r = new Row();
                    r.groupId = e.firestoreGroupId; r.groupName = e.name + " (on the network)";
                    r.principal = e.position.borrowedHarv; r.repaid = e.position.repaidHarv;
                    r.nodeId = e.nodeId;
                    r.asset = e.asset == null ? "" : e.asset;
                    rows.add(r);
                }
            }
            fetchLegacy(uid, portfolio.legacyGroupIds.isEmpty() && portfolio.entries.isEmpty() ? ids : portfolio.legacyGroupIds, rows);
        });
    }

    /** group id -> {picture, type}, filled as each group document arrives, so a loan row can show its group's picture. */
    private final java.util.Map<String, String[]> photos = new java.util.concurrent.ConcurrentHashMap<>();

    private void fetchLegacy(String uid, List<String> ids, List<Row> rows) {
        if (ids.isEmpty()) { runOnUiThread(() -> show(rows)); return; }
        final AtomicInteger left = new AtomicInteger(ids.size());
        for (String gid : ids) {
            final String groupId = gid;
            GroupFirestoreManager m = GroupFirestoreManager.getInstance(this);
            m.getGroup(groupId, new GroupFirestoreManager.DocCallback() {
                @Override public void onSuccess(Map<String, Object> grp) {
                    photos.put(groupId, new String[] {s(grp.get("groupPhoto")), s(grp.get("groupType"))});
                    loans(m, uid, groupId, s(grp.get("groupName")), rows, left);
                }
                @Override public void onError(String e) { loans(m, uid, groupId, "Group", rows, left); }
            });
        }
    }

    private void loans(GroupFirestoreManager m, String uid, String gid, String name, List<Row> rows, AtomicInteger left) {
        m.getLoans(gid, new GroupFirestoreManager.ListCallback() {
            @Override public void onSuccess(List<Map<String, Object>> items) {
                synchronized (rows) {
                    for (Map<String, Object> l : items) {
                        if (!"ACTIVE".equalsIgnoreCase(s(l.get("status"))) || !uid.equals(s(l.get("borrowerUid")))) continue;
                        Row r = new Row();
                        r.groupId = gid; r.groupName = name;
                        String[] pt = photos.get(gid);
                        if (pt != null) { r.groupPhoto = pt[0]; r.groupType = pt[1]; }
                        r.principal = n(l.get("principalHarv")) > 0 ? n(l.get("principalHarv")) : n(l.get("amountHarv"));
                        r.repaid = n(l.get("repaidAmountHarv"));
                        rows.add(r);
                    }
                }
                if (left.decrementAndGet() == 0) runOnUiThread(() -> show(rows));
            }
            @Override public void onError(String e) {
                if (left.decrementAndGet() == 0) runOnUiThread(() -> show(rows));
            }
        });
    }

    private void show(List<Row> rows) {
        list.removeAllViews();
        if (rows.isEmpty()) {
            TextView e = (TextView) getLayoutInflater().inflate(R.layout.item_list_empty, list, false);
            e.setText("No open loans.");
            com.harvana.app.utils.EmptyState.illustrate(e, com.harvana.app.R.drawable.illus_empty_loans);
            list.addView(e);
            return;
        }
        // Summary: how many, and what is still owed in each coin.
        java.util.Map<String, Double> owed = new java.util.LinkedHashMap<>();
        for (Row r : rows) owed.merge(r.asset, Math.max(0, r.principal - r.repaid), Double::sum);
        com.harvana.app.utils.InfoTable sum = new com.harvana.app.utils.InfoTable(this);
        sum.row(R.drawable.ic_tx_loan_line, "Open loans", String.valueOf(rows.size()));
        for (java.util.Map.Entry<String, Double> o : owed.entrySet()) {
            sum.row(com.harvana.app.group.CoinLogo.of(this, o.getKey()), "Still to pay", fmt(o.getValue()) + " " + com.harvana.app.group.CoinLogo.symbol(o.getKey()));
        }
        list.addView(sum.view());
        for (Row r : rows) list.addView(card(r));
    }

    /** One loan: res/layout/item_open_loan.xml. */
    private View card(Row r) {
        View c = getLayoutInflater().inflate(R.layout.item_open_loan, list, false);
        double left = Math.max(0, r.principal - r.repaid);
        String coin = com.harvana.app.group.CoinLogo.symbol(r.asset);
        com.harvana.app.utils.GroupRing.show(c.findViewById(R.id.iv_loan_group), r.groupType, r.groupPhoto, R.drawable.ic_coin_harv);
        com.harvana.app.group.CoinLogo.show(c.findViewById(R.id.iv_loan_coin), r.asset);
        ((TextView) c.findViewById(R.id.tv_loan_group_name)).setText(r.groupName);
        com.harvana.app.utils.InfoTable t = new com.harvana.app.utils.InfoTable(this);
        t.row(R.drawable.ic_tx_loan_line, "Borrowed", fmt(r.principal) + " " + coin);
        t.row(R.drawable.ic_check_circle_line, "Repaid", fmt(r.repaid) + " " + coin);
        t.row(R.drawable.ic_wallet_line, "Still to pay", fmt(left) + " " + coin, UiColors.accent());
        ((LinearLayout) c.findViewById(R.id.loan_figures)).addView(t.view());
        MaterialButton pay = c.findViewById(R.id.btn_loan_pay);
        if (!r.nodeId.isEmpty()) pay.setText("Open group to repay");
        pay.setOnClickListener(v -> {
            Intent i = new Intent(this, GroupDetailsActivity.class);
            i.putExtra("groupId", r.groupId);
            i.putExtra("tab", 3);                       // the group's Loans tab
            startActivity(i);
        });
        return c;
    }

    private void showError(String e) {
        list.removeAllViews();
        TextView t = (TextView) getLayoutInflater().inflate(R.layout.item_list_empty, list, false);
        t.setText("Couldn't load your loans: " + e);
        t.setTextColor(UiColors.error());
        list.addView(t);
    }

    private static String fmt(double v) { return v == Math.rint(v) ? String.valueOf((long) v) : String.format(Locale.US, "%.2f", v); }
    private static double n(Object o) { return o instanceof Number ? ((Number) o).doubleValue() : 0; }
    private static String s(Object o) { return o == null ? "" : String.valueOf(o).trim(); }
}
