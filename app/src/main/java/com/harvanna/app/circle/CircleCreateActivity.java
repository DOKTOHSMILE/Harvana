package com.harvanna.app.circle;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.harvanna.app.R;
import com.harvanna.app.sync.AjooNodeClient;
import com.harvanna.app.sync.NodeGroupClient;
import com.harvanna.app.wallet.PinPromptHelper;
import com.harvanna.app.wallet.WalletManager;

import org.json.JSONObject;

/**
 * Create a rotating-savings Circle ON THE NODE (source of truth).
 * Shows exactly what will be locked BEFORE anything is signed: the creator's bond is 2x an ordinary
 * member's bond (contribution x (members-1)) plus a 1 HARV non-refundable creation fee.
 * Limits (node): 3-12 members, cycle 24-720 h.
 */
public class CircleCreateActivity extends AppCompatActivity {

    private static final long CREATION_FEE_RAW = 100_000_000L;   // 1 HARV
    private static final long CREATOR_BOND_MULTIPLIER = 2L;
    private static final int MIN_MEMBERS = 3, MAX_MEMBERS = 12;
    private static final long[] CYCLE_HOURS = {24, 72, 168, 336, 720};
    private static final String[] CYCLE_LABELS = {"Daily", "3 days", "Weekly", "2 weeks", "Monthly"};

    private String address = "";
    private int members = 5;
    private int cycleIndex = 2;
    private long availableRaw = -1;

    private EditText etContribution;
    private TextView tvMembers, tvSummary, tvAvailable, tvError;
    private Button btnCreate;
    private final Button[] cycleButtons = new Button[CYCLE_HOURS.length];

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setTitle("Create a Circle");
        address = WalletManager.getActiveWalletAddress(this);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(CircleUi.color(this, R.color.colorBackground));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = CircleUi.dp(this, 18);
        root.setPadding(pad, pad, pad, pad);
        scroll.addView(root, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        root.addView(CircleUi.text(this, "Rotating savings circle", 22, R.color.colorTextPrimary, true));
        root.addView(CircleUi.text(this, "Everyone pays the same amount each round; one member receives the pot each round until everyone has been paid. Bonds are locked on the Harvanna node and returned when the circle completes cleanly.", 13.5f, R.color.colorTextSecondary, false));

        // contribution
        LinearLayout c1 = CircleUi.card(this);
        c1.addView(CircleUi.text(this, "Contribution per round (HARV)", 13, R.color.colorTextSecondary, false));
        etContribution = new EditText(this);
        etContribution.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etContribution.setHint("e.g. 50");
        etContribution.setTextColor(CircleUi.color(this, R.color.colorTextPrimary));
        etContribution.setHintTextColor(CircleUi.color(this, R.color.colorTextMuted));
        etContribution.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b2, int c) {}
            public void onTextChanged(CharSequence s, int a, int b2, int c) {}
            public void afterTextChanged(Editable e) { refresh(); }
        });
        c1.addView(etContribution);
        root.addView(c1);

        // members stepper
        LinearLayout c2 = CircleUi.card(this);
        c2.addView(CircleUi.text(this, "Members (including you)", 13, R.color.colorTextSecondary, false));
        LinearLayout step = new LinearLayout(this);
        step.setGravity(Gravity.CENTER_VERTICAL);
        Button minus = new Button(this); minus.setText("−"); minus.setAllCaps(false);
        Button plus = new Button(this); plus.setText("+"); plus.setAllCaps(false);
        tvMembers = CircleUi.text(this, String.valueOf(members), 20, R.color.colorTextPrimary, true);
        tvMembers.setGravity(Gravity.CENTER);
        minus.setOnClickListener(v -> { if (members > MIN_MEMBERS) { members--; refresh(); } });
        plus.setOnClickListener(v -> { if (members < MAX_MEMBERS) { members++; refresh(); } });
        step.addView(minus, new LinearLayout.LayoutParams(CircleUi.dp(this, 56), CircleUi.dp(this, 48)));
        step.addView(tvMembers, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        step.addView(plus, new LinearLayout.LayoutParams(CircleUi.dp(this, 56), CircleUi.dp(this, 48)));
        c2.addView(step);
        c2.addView(CircleUi.text(this, MIN_MEMBERS + " to " + MAX_MEMBERS + " members", 12, R.color.colorTextTertiary, false));
        root.addView(c2);

        // cycle presets
        LinearLayout c3 = CircleUi.card(this);
        c3.addView(CircleUi.text(this, "How often is a round paid?", 13, R.color.colorTextSecondary, false));
        LinearLayout chips = new LinearLayout(this);
        for (int i = 0; i < CYCLE_HOURS.length; i++) {
            final int idx = i;
            Button bt = new Button(this);
            bt.setAllCaps(false);
            bt.setText(CYCLE_LABELS[i]);
            bt.setTextSize(12);
            bt.setOnClickListener(v -> { cycleIndex = idx; refresh(); });
            cycleButtons[i] = bt;
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, CircleUi.dp(this, 44), 1f);
            lp.rightMargin = CircleUi.dp(this, 4);
            chips.addView(bt, lp);
        }
        c3.addView(chips);
        root.addView(c3);

        // disclosure
        LinearLayout c4 = CircleUi.card(this);
        c4.addView(CircleUi.text(this, "What you lock up front", 15, R.color.colorTextPrimary, true));
        tvSummary = CircleUi.text(this, "", 13.5f, R.color.colorTextPrimary, false);
        tvSummary.setLineSpacing(0, 1.25f);
        c4.addView(tvSummary);
        tvAvailable = CircleUi.text(this, "Checking your balance…", 12.5f, R.color.colorTextSecondary, false);
        c4.addView(tvAvailable);
        root.addView(c4);

        tvError = CircleUi.text(this, "", 13, R.color.colorError, false);
        root.addView(tvError);

        btnCreate = new Button(this);
        btnCreate.setText("Create circle");
        btnCreate.setAllCaps(false);
        btnCreate.setTextColor(CircleUi.color(this, R.color.colorTextOnAccent));
        btnCreate.setBackgroundResource(R.drawable.bg_btn_primary);
        btnCreate.setOnClickListener(v -> onCreateClicked());
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, CircleUi.dp(this, 52));
        blp.topMargin = CircleUi.dp(this, 16);
        root.addView(btnCreate, blp);

        setContentView(scroll);
        refresh();
        loadBalance();
    }

    private long contributionRaw() { return CircleUi.toRaw(etContribution.getText().toString()); }

    private void refresh() {
        tvMembers.setText(String.valueOf(members));
        for (int i = 0; i < cycleButtons.length; i++) {
            boolean sel = i == cycleIndex;
            cycleButtons[i].setBackgroundResource(sel ? R.drawable.bg_btn_primary : R.drawable.bg_btn_outline_accent);
            cycleButtons[i].setTextColor(CircleUi.color(this, sel ? R.color.colorTextOnAccent : R.color.colorAccentStrong));
        }
        long c = contributionRaw();
        if (c <= 0) {
            tvSummary.setText("Enter a contribution to see the amounts.");
            btnCreate.setEnabled(false);
            btnCreate.setAlpha(0.5f);
            return;
        }
        long perMember = c * (members - 1);
        long creatorBond = perMember * CREATOR_BOND_MULTIPLIER;
        long total = creatorBond + CREATION_FEE_RAW;
        tvSummary.setText(
            "• Each round every member pays " + CircleUi.harv(c) + "\n"
          + "• The pot each round: " + CircleUi.harv(c * members) + "\n"
          + "• Every other member locks a bond of " + CircleUi.harv(perMember) + " to join\n"
          + "• Your bond (2× a member's): " + CircleUi.harv(creatorBond) + "\n"
          + "• Creation fee (non-refundable): " + CircleUi.harv(CREATION_FEE_RAW) + "\n\n"
          + "Total locked from you now: " + CircleUi.harv(total) + "\n"
          + "Network fees are extra.");
        boolean enough = availableRaw < 0 || availableRaw >= total;
        tvError.setText(enough ? "" : "Your available balance is less than the " + CircleUi.harv(total) + " needed.");
        btnCreate.setEnabled(enough);
        btnCreate.setAlpha(enough ? 1f : 0.5f);
    }

    private void loadBalance() {
        if (address == null || address.isEmpty()) { tvAvailable.setText("No active wallet."); return; }
        AjooNodeClient.getWallet(address, new AjooNodeClient.JsonCallback() {
            @Override public void onSuccess(JSONObject json) {
                availableRaw = json.optLong("available", json.optLong("balance", 0L));
                tvAvailable.setText("Available: " + CircleUi.harv(availableRaw));
                refresh();
            }
            @Override public void onError(String error) { tvAvailable.setText("Couldn't read your balance."); }
        });
    }

    private void onCreateClicked() {
        final long c = contributionRaw();
        if (c <= 0) { tvError.setText("Enter a valid contribution."); return; }
        if (address == null || address.isEmpty()) { tvError.setText("No active wallet found."); return; }
        PinPromptHelper.verifyPinThenProceed(this, () -> {
            btnCreate.setEnabled(false);
            NodeGroupClient.createCircle(getApplicationContext(), address, c, members, CYCLE_HOURS[cycleIndex],
                new NodeGroupClient.Callback() {
                    @Override public void onSuccess(JSONObject json) {
                        String id = json.optString("circleId", "");
                        if (id.isEmpty()) {
                            Toast.makeText(CircleCreateActivity.this, "Created, but the node returned no id.", Toast.LENGTH_LONG).show();
                            finish();
                            return;
                        }
                        Toast.makeText(CircleCreateActivity.this, "Circle created", Toast.LENGTH_SHORT).show();
                        Intent i = new Intent(CircleCreateActivity.this, CircleActivity.class);
                        i.putExtra("circleId", id);
                        startActivity(i);
                        finish();
                    }
                    @Override public void onError(String error) {
                        btnCreate.setEnabled(true);
                        tvError.setText(CircleUi.friendly(error));
                    }
                });
        });
    }
}
