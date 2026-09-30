package com.harvanna.app;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.harvanna.app.firestore.GroupFirestoreManager;
import com.harvanna.app.group.GroupContract;
import com.harvanna.app.group.InviteManager;
import com.harvanna.app.utils.StatusBarUtil;
import com.harvanna.app.utils.UiColors;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/** The invitee's inbox: who invited them, and a clear summary of each group before they decide. */
public class InvitesActivity extends AppCompatActivity {
    private LinearLayout list;
    private TextView empty;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(UiColors.background());
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int p = dp(16);
        root.setPadding(p, p, p, p);
        sv.addView(root);
        com.harvanna.app.utils.AppBar.setContent(this, sv, "Invitations", "Groups that invited you to join");
        list = root;
        empty = tv("Loading invitations…", 14, false, UiColors.textSecondary());
        root.addView(empty);
    }

    @Override protected void onResume() { super.onResume(); load(); }
    @Override public boolean onSupportNavigateUp() { finish(); return true; }

    private void load() {
        InviteManager.listMine(this, new InviteManager.ListCb() {
            @Override public void onList(List<Map<String, Object>> invites) {
                runOnUiThread(() -> show(invites));
            }
            @Override public void onError(String e) {
                runOnUiThread(() -> { list.removeAllViews(); empty = tv("Couldn't load invitations: " + e, 14, false, UiColors.error()); list.addView(empty); });
            }
        });
    }

    private void show(List<Map<String, Object>> invites) {
        list.removeAllViews();
        if (invites.isEmpty()) {
            list.addView(tv("No pending invitations.", 14, false, UiColors.textSecondary()));
            return;
        }
        for (Map<String, Object> inv : invites) list.addView(card(inv));
    }

    private View card(Map<String, Object> inv) {
        final String id = s(inv, "_docId");
        final String gid = s(inv, "groupId");
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackgroundColor(UiColors.surface());
        int p = dp(14);
        c.setPadding(p, p, p, p);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(12);
        c.setLayoutParams(lp);

        c.addView(tv(s(inv, "groupName"), 17, true, UiColors.textPrimary()));
        c.addView(tv(s(inv, "inviterName") + " invited you", 13, false, UiColors.textSecondary()));
        String msg = s(inv, "message");
        if (!msg.isEmpty()) c.addView(tv("“" + msg + "”", 13, false, UiColors.textPrimary()));
        final TextView summary = tv("Loading group details…", 13, false, UiColors.textSecondary());
        c.addView(summary);

        GroupFirestoreManager.getInstance(this).getGroup(gid, new GroupFirestoreManager.DocCallback() {
            @Override public void onSuccess(Map<String, Object> g) {
                runOnUiThread(() -> summary.setText(summaryOf(g)));
            }
            @Override public void onError(String e) {
                runOnUiThread(() -> summary.setText("Group details unavailable right now."));
            }
        });

        MaterialButton review = new MaterialButton(this);
        review.setText("Read the details and join");
        review.setAllCaps(false);
        review.setOnClickListener(v -> {
            Intent i = new Intent(this, GroupJoinRequestActivity.class);
            i.putExtra("groupId", gid);
            i.putExtra("inviteId", id);
            startActivity(i);
        });
        c.addView(review);

        MaterialButton decline = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        decline.setText("Decline");
        decline.setAllCaps(false);
        decline.setOnClickListener(v -> InviteManager.respond(this, id, false, err -> runOnUiThread(() -> {
            if (err != null) Toast.makeText(this, "Couldn't decline: " + err, Toast.LENGTH_LONG).show();
            else load();
        })));
        c.addView(decline);
        return c;
    }

    private String summaryOf(Map<String, Object> g) {
        String ticker = CoinInfo.ticker();
        StringBuilder sb = new StringBuilder();
        sb.append(s(g, "groupType")).append('\n');
        Object amt = g.get("contributionAmount");
        if (amt instanceof Number) sb.append("Contribution: ").append(amt).append(' ').append(ticker)
            .append(s(g, "frequency").isEmpty() ? "" : " · " + s(g, "frequency")).append('\n');
        sb.append("Members: ").append(num(g, "membersCount")).append(" of ").append(num(g, "totalSlots")).append('\n');
        sb.append("Loans: ").append(GroupContract.allowsLoans(g) ? "yes" : "no").append('\n');
        sb.append("Joining: ").append(s(g, "joinApprovalMethod").isEmpty()
            ? (Boolean.FALSE.equals(g.get("requireApprovalToJoin")) ? "open" : "admin approves") : s(g, "joinApprovalMethod").toLowerCase(Locale.US)).append('\n');
        sb.append("You will read the full rules and sign before you join. You need a wallet to join.");
        return sb.toString();
    }

    private static long num(Map<String, Object> m, String k) {
        Object o = m.get(k); return o instanceof Number ? ((Number) o).longValue() : 0;
    }
    private static String s(Map<String, Object> m, String k) {
        Object o = m.get(k); return o == null ? "" : String.valueOf(o).trim();
    }
    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
    private TextView tv(String t, int sp, boolean bold, int color) {
        TextView v = new TextView(this);
        v.setText(t); v.setTextSize(sp); v.setTextColor(color);
        if (bold) v.setTypeface(null, Typeface.BOLD);
        v.setPadding(0, dp(4), 0, dp(4));
        return v;
    }
}
