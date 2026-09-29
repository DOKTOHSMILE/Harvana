package com.harvanna.wallet.circle;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.harvanna.wallet.R;
import com.harvanna.wallet.sync.NodeGroupClient;
import com.harvanna.wallet.wallet.PinPromptHelper;
import com.harvanna.wallet.wallet.WalletManager;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * A Circle as the NODE sees it (GET /api/v1/circle/{id} and .../member/{address}); nothing here is read from
 * Firestore. Actions are signed requests: join (needs the circle's termsHash), contribute, withdraw bond.
 */
public class CircleActivity extends AppCompatActivity {

    private String circleId = "";
    private String me = "";

    private SwipeRefreshLayout refresh;
    private LinearLayout content;
    private JSONObject circle;
    private JSONObject member;      // null when the wallet is not a member
    private boolean memberChecked;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setTitle("Circle");
        circleId = getIntent().getStringExtra("circleId");
        if (circleId == null) circleId = "";
        me = WalletManager.getActiveWalletAddress(this);

        refresh = new SwipeRefreshLayout(this);
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(CircleUi.color(this, R.color.colorBackground));
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int pad = CircleUi.dp(this, 18);
        content.setPadding(pad, pad, pad, pad);
        scroll.addView(content, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        refresh.addView(scroll);
        refresh.setOnRefreshListener(this::load);
        setContentView(refresh);
        load();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (circle != null) load();
    }

    private void load() {
        refresh.setRefreshing(true);
        NodeGroupClient.getCircle(circleId, new NodeGroupClient.Callback() {
            @Override public void onSuccess(JSONObject json) {
                circle = json;
                loadMember();
            }
            @Override public void onError(String error) {
                refresh.setRefreshing(false);
                showMessage(error != null && error.contains("404") ? "This circle doesn't exist on the node." : "Couldn't load the circle: " + error);
            }
        });
    }

    private void loadMember() {
        if (me == null || me.isEmpty()) { member = null; memberChecked = true; render(); return; }
        NodeGroupClient.getCircleMember(circleId, me, new NodeGroupClient.Callback() {
            @Override public void onSuccess(JSONObject json) { member = json; memberChecked = true; render(); }
            @Override public void onError(String error) { member = null; memberChecked = true; render(); } // 404 = not a member
        });
    }

    private void showMessage(String m) {
        content.removeAllViews();
        content.addView(CircleUi.text(this, m, 14, R.color.colorTextSecondary, false));
    }

    private void render() {
        refresh.setRefreshing(false);
        content.removeAllViews();
        final String state = circle.optString("state", "");
        final long contribution = circle.optLong("contributionAmountRaw");
        final int limit = circle.optInt("memberLimit");
        final int round = circle.optInt("currentRound");
        final long cycleHours = circle.optLong("cycleDurationHours");
        JSONArray order = circle.optJSONArray("payoutOrder");
        int joined = order != null ? order.length() : 0;

        // header
        LinearLayout head = new LinearLayout(this);
        head.setGravity(android.view.Gravity.CENTER_VERTICAL);
        TextView title = CircleUi.text(this, "Circle " + shortId(circleId), 22, R.color.colorTextPrimary, true);
        head.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        head.addView(CircleUi.chip(this, state, CircleUi.stateColor(state)));
        content.addView(head);

        // progress
        LinearLayout prog = CircleUi.card(this);
        boolean forming = "FORMING".equals(state);
        prog.addView(CircleUi.text(this, forming ? joined + " of " + limit + " members joined"
                : ("ACTIVE".equals(state) ? "Round " + round + " of " + limit : stateSentence(state)), 16, R.color.colorTextPrimary, true));
        ProgressBar pb = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        pb.setMax(Math.max(limit, 1));
        pb.setProgress(forming ? joined : Math.min(round, limit));
        pb.setProgressTintList(android.content.res.ColorStateList.valueOf(CircleUi.color(this, R.color.colorAccent)));
        prog.addView(pb, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, CircleUi.dp(this, 10)));
        if (forming) {
            long deadline = circle.optLong("formationDeadline");
            if (deadline > 0) prog.addView(CircleUi.text(this, "Must fill by " + java.text.DateFormat.getDateTimeInstance().format(new java.util.Date(deadline))
                    + " or it is cancelled and bonds are returned.", 12, R.color.colorTextTertiary, false));
        }
        content.addView(prog);

        // terms
        LinearLayout terms = CircleUi.card(this);
        terms.addView(CircleUi.text(this, "Terms", 15, R.color.colorTextPrimary, true));
        terms.addView(CircleUi.row(this, "Contribution per round", CircleUi.harv(contribution)));
        terms.addView(CircleUi.row(this, "Pot per round", CircleUi.harv(contribution * limit)));
        terms.addView(CircleUi.row(this, "Round length", cycleLabel(cycleHours)));
        terms.addView(CircleUi.row(this, "Bond to join", CircleUi.harv(circle.optLong("requiredBondRaw"))));
        terms.addView(CircleUi.row(this, "Members", String.valueOf(limit)));
        TextView escrow = CircleUi.text(this, "Escrow: " + circle.optString("escrowAddress", "—"), 11.5f, R.color.colorTextTertiary, false);
        escrow.setOnClickListener(v -> copy(circle.optString("escrowAddress", "")));
        terms.addView(escrow);
        content.addView(terms);

        // my position
        if (member != null) {
            String ms = member.optString("state", "");
            LinearLayout mine = CircleUi.card(this);
            LinearLayout mh = new LinearLayout(this);
            mh.setGravity(android.view.Gravity.CENTER_VERTICAL);
            mh.addView(CircleUi.text(this, "Your position", 15, R.color.colorTextPrimary, true), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            mh.addView(CircleUi.chip(this, ms, CircleUi.stateColor(ms)));
            mine.addView(mh);
            if (member.has("payoutRound")) mine.addView(CircleUi.row(this, "You are paid in round", String.valueOf(member.optInt("payoutRound"))));
            mine.addView(CircleUi.row(this, "Paid so far", CircleUi.harv(member.optLong("paidRaw"))));
            if (member.has("remainingObligationRaw")) mine.addView(CircleUi.row(this, "Still to pay", CircleUi.harv(member.optLong("remainingObligationRaw"))));
            mine.addView(CircleUi.row(this, "Bond locked", CircleUi.harv(member.optLong("bondRemainingRaw"))));
            if (member.optLong("penaltyAccruedRaw") > 0) mine.addView(CircleUi.row(this, "Penalties", CircleUi.harv(member.optLong("penaltyAccruedRaw"))));
            if (member.optLong("payoutReceivedRaw") > 0) mine.addView(CircleUi.row(this, "Payout received", CircleUi.harv(member.optLong("payoutReceivedRaw"))));
            if ("LATE".equals(ms)) mine.addView(CircleUi.text(this, "You're late. Pay now to avoid penalties taken from your bond.", 12.5f, R.color.colorWarning, false));
            content.addView(mine);
        }

        // actions
        boolean isMember = member != null;
        String ms = isMember ? member.optString("state", "") : "";
        if (!isMember && forming && memberChecked) {
            content.addView(actionButton("Join circle", true, v -> confirmJoin()));
        } else if (isMember && ("ACTIVE".equals(state)) && ("ACTIVE".equals(ms) || "LATE".equals(ms))) {
            content.addView(actionButton("Contribute " + CircleUi.harv(contribution), true, v -> doContribute()));
        } else if (isMember && "COMPLETED".equals(state)) {
            content.addView(actionButton("Withdraw bond", true, v -> doWithdraw()));
        }

        Button share = actionButton("Copy circle ID", false, v -> copy(circleId));
        content.addView(share);
    }

    private Button actionButton(String label, boolean primary, android.view.View.OnClickListener l) {
        Button bt = new Button(this);
        bt.setText(label);
        bt.setAllCaps(false);
        bt.setTextColor(CircleUi.color(this, primary ? R.color.colorTextOnAccent : R.color.colorAccentStrong));
        bt.setBackgroundResource(primary ? R.drawable.bg_btn_primary : R.drawable.bg_btn_outline_accent);
        bt.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, CircleUi.dp(this, 52));
        lp.topMargin = CircleUi.dp(this, 12);
        bt.setLayoutParams(lp);
        return bt;
    }

    private void confirmJoin() {
        final long bond = circle.optLong("requiredBondRaw");
        final long contribution = circle.optLong("contributionAmountRaw");
        new AlertDialog.Builder(this)
            .setTitle("Join this circle?")
            .setMessage("You will lock a bond of " + CircleUi.harv(bond) + " now and pay " + CircleUi.harv(contribution)
                + " each round (" + cycleLabel(circle.optLong("cycleDurationHours")) + "). The bond is returned when the circle completes cleanly; "
                + "missed payments are taken from it with a penalty.\n\nBy joining you sign these exact terms.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Sign and join", (d, w) -> PinPromptHelper.verifyPinThenProceed(this, () ->
                NodeGroupClient.joinCircle(getApplicationContext(), me, circleId, circle.optString("termsHash", ""), result("Joined the circle"))))
            .show();
    }

    private void doContribute() {
        PinPromptHelper.verifyPinThenProceed(this, () ->
            NodeGroupClient.contributeCircle(getApplicationContext(), me, circleId, result("Contribution sent")));
    }

    private void doWithdraw() {
        PinPromptHelper.verifyPinThenProceed(this, () ->
            NodeGroupClient.withdrawCircleBond(getApplicationContext(), me, circleId, result("Bond withdrawal requested")));
    }

    private NodeGroupClient.Callback result(String okMessage) {
        return new NodeGroupClient.Callback() {
            @Override public void onSuccess(JSONObject json) {
                Toast.makeText(CircleActivity.this, okMessage, Toast.LENGTH_SHORT).show();
                load();
            }
            @Override public void onError(String error) {
                Toast.makeText(CircleActivity.this, CircleUi.friendly(error), Toast.LENGTH_LONG).show();
            }
        };
    }

    private void copy(String text) {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("Circle", text));
        Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show();
    }

    private static String shortId(String id) {
        return id.length() > 14 ? id.substring(0, 10) + "…" + id.substring(id.length() - 4) : id;
    }

    private static String cycleLabel(long hours) {
        if (hours % 24 == 0) {
            long d = hours / 24;
            return d == 1 ? "Daily" : (d == 7 ? "Weekly" : "Every " + d + " days");
        }
        return "Every " + hours + " hours";
    }

    private static String stateSentence(String state) {
        switch (state) {
            case "COMPLETED": return "Completed";
            case "ABORTED": return "Cancelled - bonds returned";
            default: return state;
        }
    }
}
