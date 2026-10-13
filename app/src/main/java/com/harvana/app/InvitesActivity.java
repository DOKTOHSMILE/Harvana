package com.harvana.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.harvana.app.firestore.GroupFirestoreManager;
import com.harvana.app.group.GroupContract;
import com.harvana.app.group.InviteManager;
import com.harvana.app.utils.UiColors;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/** The invitee's inbox: who invited them, and a clear summary of each group before they decide. */
public class InvitesActivity extends AppCompatActivity {
    private LinearLayout list;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        com.harvana.app.utils.AppBar.setContent(this, R.layout.activity_list_screen, "Invitations", "Groups that invited you to join");
        list = findViewById(R.id.list_screen_items);
        list.addView(com.harvana.app.utils.Shimmer.rows(this, 4));
    }

    @Override protected void onResume() { super.onResume(); load(); }
    @Override public boolean onSupportNavigateUp() { finish(); return true; }

    private void load() {
        InviteManager.listMine(this, new InviteManager.ListCb() {
            @Override public void onList(List<Map<String, Object>> invites) {
                runOnUiThread(() -> show(invites));
            }
            @Override public void onError(String e) {
                runOnUiThread(() -> { list.removeAllViews(); emptyLine("Couldn't load invitations: " + e).setTextColor(UiColors.error()); });
            }
        });
    }

    private void show(List<Map<String, Object>> invites) {
        list.removeAllViews();
        if (invites.isEmpty()) { emptyLine("No pending invitations."); return; }
        for (Map<String, Object> inv : invites) list.addView(card(inv));
    }

    private TextView emptyLine(String text) {
        TextView e = (TextView) getLayoutInflater().inflate(R.layout.item_list_empty, list, false);
        e.setText(text);
        list.addView(e);
        return e;
    }

    /** One invitation: res/layout/item_invite.xml. The group's figures fill in as a table once the group is read. */
    private View card(Map<String, Object> inv) {
        final String id = s(inv, "_docId");
        final String gid = s(inv, "groupId");
        View c = getLayoutInflater().inflate(R.layout.item_invite, list, false);
        final android.widget.ImageView face = c.findViewById(R.id.iv_invite_group);
        com.harvana.app.utils.GroupRing.show(face, "", "", R.drawable.ic_coin_harv);
        ((TextView) c.findViewById(R.id.tv_invite_group_name)).setText(s(inv, "groupName"));
        ((TextView) c.findViewById(R.id.tv_invite_from)).setText(s(inv, "inviterName"));
        String msg = s(inv, "message");
        TextView note = c.findViewById(R.id.tv_invite_message);
        if (!msg.isEmpty()) { note.setText("“" + msg + "”"); note.setVisibility(View.VISIBLE); }
        final LinearLayout details = c.findViewById(R.id.invite_details);
        details.addView(com.harvana.app.utils.Shimmer.rows(this, 3));

        GroupFirestoreManager.getInstance(this).getGroup(gid, new GroupFirestoreManager.DocCallback() {
            @Override public void onSuccess(Map<String, Object> g) {
                runOnUiThread(() -> {
                    details.removeAllViews();
                    details.addView(detailsOf(g).view());
                    com.harvana.app.utils.GroupRing.show(face, s(g, "groupType"), s(g, "groupPhoto"), R.drawable.ic_coin_harv);
                });
            }
            @Override public void onError(String e) {
                runOnUiThread(() -> { details.removeAllViews(); details.addView(new com.harvana.app.utils.InfoTable(InvitesActivity.this).row("Group", "—").view()); });
            }
        });

        c.findViewById(R.id.btn_invite_join).setOnClickListener(v -> {
            Intent i = new Intent(this, GroupJoinRequestActivity.class);
            i.putExtra("groupId", gid);
            i.putExtra("inviteId", id);
            startActivity(i);
        });
        c.findViewById(R.id.btn_invite_decline).setOnClickListener(v -> InviteManager.respond(this, id, false, err -> runOnUiThread(() -> {
            if (err != null) Toast.makeText(this, "Couldn't decline: " + err, Toast.LENGTH_LONG).show();
            else load();
        })));
        return c;
    }

    /** The group's figures, one per row, with the group coin's logo on the contribution. */
    private com.harvana.app.utils.InfoTable detailsOf(Map<String, Object> g) {
        String coin = s(g, "asset");
        com.harvana.app.utils.InfoTable t = new com.harvana.app.utils.InfoTable(this);
        t.row(R.drawable.ic_groups_line, "Type", s(g, "groupType"));
        Object amt = g.get("contributionAmount");
        if (amt instanceof Number) {
            t.row(com.harvana.app.group.CoinLogo.of(this, coin), "Contribution",
                com.harvana.app.wallet.BalanceFormatter.smart(((Number) amt).doubleValue()) + " " + com.harvana.app.group.CoinLogo.symbol(coin)
                + (s(g, "frequency").isEmpty() ? "" : " · " + s(g, "frequency")));
        }
        t.row(R.drawable.ic_person_line, "Members", num(g, "membersCount") + " / " + num(g, "totalSlots"));
        t.row(R.drawable.ic_tx_loan_line, "Loans", GroupContract.allowsLoans(g) ? "Yes" : "No");
        t.row(R.drawable.ic_check_circle_line, "Joining", s(g, "joinApprovalMethod").isEmpty()
            ? (Boolean.FALSE.equals(g.get("requireApprovalToJoin")) ? "Open" : "Admin approves") : s(g, "joinApprovalMethod").toLowerCase(Locale.US));
        return t;
    }

    private static long num(Map<String, Object> m, String k) {
        Object o = m.get(k); return o instanceof Number ? ((Number) o).longValue() : 0;
    }
    private static String s(Map<String, Object> m, String k) {
        Object o = m.get(k); return o == null ? "" : String.valueOf(o).trim();
    }
}
