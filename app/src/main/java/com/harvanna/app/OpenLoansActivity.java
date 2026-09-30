package com.harvanna.app;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.harvanna.app.firestore.GroupFirestoreManager;
import com.harvanna.app.firestore.UserProfileManager;
import com.harvanna.app.utils.StatusBarUtil;
import com.harvanna.app.utils.UiColors;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/** Every active loan the user has, across all their groups, with a Pay button that opens that group's Loans tab. */
public class OpenLoansActivity extends AppCompatActivity {
    private LinearLayout list;
    private TextView summary;

    private static final class Row {
        String groupId, groupName;
        double principal, repaid;
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        StatusBarUtil.matchToolbar(this);
        setTitle("Open loans");
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(UiColors.background());
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        int p = dp(16);
        list.setPadding(p, p, p, p);
        sv.addView(list);
        setContentView(sv);
        summary = tv("Loading your loans…", 14, false, UiColors.textSecondary());
        list.addView(summary);
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
                Object g = profile.get("groups");
                List<String> ids = new ArrayList<>();
                if (g instanceof List) for (Object o : (List<Object>) g) if (o != null && !String.valueOf(o).trim().isEmpty()) ids.add(String.valueOf(o).trim());
                fetch(uid, ids);
            }
            @Override public void onError(String e) { runOnUiThread(() -> showError(e)); }
        });
    }

    private void fetch(String uid, List<String> ids) {
        if (ids.isEmpty()) { runOnUiThread(() -> show(new ArrayList<>())); return; }
        final List<Row> rows = new ArrayList<>();
        final AtomicInteger left = new AtomicInteger(ids.size());
        for (String gid : ids) {
            final String groupId = gid;
            GroupFirestoreManager m = GroupFirestoreManager.getInstance(this);
            m.getGroup(groupId, new GroupFirestoreManager.DocCallback() {
                @Override public void onSuccess(Map<String, Object> grp) {
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
        double outstanding = 0;
        for (Row r : rows) outstanding += Math.max(0, r.principal - r.repaid);
        if (rows.isEmpty()) {
            list.addView(tv("You have no open loans.", 15, true, UiColors.textPrimary()));
            list.addView(tv("When you borrow from a group, the loan shows here with a Pay button.", 13, false, UiColors.textSecondary()));
            return;
        }
        list.addView(tv(rows.size() + (rows.size() == 1 ? " open loan" : " open loans") + " · "
            + fmt(outstanding) + " " + CoinInfo.ticker() + " still to repay", 14, true, UiColors.textPrimary()));
        for (Row r : rows) list.addView(card(r));
    }

    private View card(Row r) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackgroundColor(UiColors.surface());
        int p = dp(14);
        c.setPadding(p, p, p, p);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(10);
        c.setLayoutParams(lp);
        double left = Math.max(0, r.principal - r.repaid);
        c.addView(tv(r.groupName, 16, true, UiColors.textPrimary()));
        c.addView(tv("Borrowed " + fmt(r.principal) + " " + CoinInfo.ticker() + " · Repaid " + fmt(r.repaid), 13, false, UiColors.textSecondary()));
        c.addView(tv("Still to pay: " + fmt(left) + " " + CoinInfo.ticker(), 14, true, UiColors.accent()));
        MaterialButton pay = new MaterialButton(this);
        pay.setText("Pay");
        pay.setAllCaps(false);
        pay.setOnClickListener(v -> {
            Intent i = new Intent(this, GroupDetailsActivity.class);
            i.putExtra("groupId", r.groupId);
            i.putExtra("tab", 3);
            startActivity(i);
        });
        c.addView(pay);
        return c;
    }

    private void showError(String e) {
        list.removeAllViews();
        list.addView(tv("Couldn't load your loans: " + e, 14, false, UiColors.error()));
    }

    private static String fmt(double v) { return v == Math.rint(v) ? String.valueOf((long) v) : String.format(Locale.US, "%.2f", v); }
    private static double n(Object o) { return o instanceof Number ? ((Number) o).doubleValue() : 0; }
    private static String s(Object o) { return o == null ? "" : String.valueOf(o).trim(); }
    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
    private TextView tv(String t, int sp, boolean bold, int color) {
        TextView v = new TextView(this);
        v.setText(t); v.setTextSize(sp); v.setTextColor(color);
        if (bold) v.setTypeface(null, Typeface.BOLD);
        v.setPadding(0, dp(3), 0, dp(3));
        return v;
    }
}
