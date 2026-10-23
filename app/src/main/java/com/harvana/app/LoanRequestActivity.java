package com.harvana.app;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.harvana.app.engine.Amounts;
import com.harvana.app.husd.HusdClient;
import com.harvana.app.utils.AppBar;
import com.harvana.app.wallet.BalanceFormatter;

/**
 * Applying for a group loan. A full screen, not a sheet: the amount is in the coin the group saves in.
 */
public class LoanRequestActivity extends AppCompatActivity {
    public static final String EXTRA_GROUP = "groupId";
    public static final String EXTRA_RATE = "rate";
    public static final String EXTRA_COIN = "coin";
    public static final String EXTRA_WALLET = "wallet";
    public static final String EXTRA_GROUP_WALLET = "groupWallet";
    public static final String EXTRA_AVAILABLE = "available";

    private String groupId = "", coin = "", myWallet = "", groupWallet = "";
    private double rate, available;
    private TextView subtitle, principal, interest, total, after, empty;
    private View rows;
    private EditText amountInput, daysInput, noteInput;

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        groupId = str(getIntent().getStringExtra(EXTRA_GROUP));
        coin = str(getIntent().getStringExtra(EXTRA_COIN));
        myWallet = str(getIntent().getStringExtra(EXTRA_WALLET));
        groupWallet = str(getIntent().getStringExtra(EXTRA_GROUP_WALLET));
        rate = getIntent().getDoubleExtra(EXTRA_RATE, 0);
        available = getIntent().getDoubleExtra(EXTRA_AVAILABLE, 0);
        if (myWallet.isEmpty()) myWallet = com.harvana.app.wallet.WalletManager.getActiveWalletAddress(this);

        AppBar.setContent(this, R.layout.bottom_sheet_loan_apply, "Apply for a loan", "");
        View handle = findViewById(R.id.view_apply_handle);
        if (handle != null && handle.getParent() instanceof View) ((View) handle.getParent()).setVisibility(View.GONE);
        TextView title = findViewById(R.id.tv_apply_title);
        if (title != null) title.setVisibility(View.GONE);

        subtitle = findViewById(R.id.tv_apply_subtitle);
        TextView amountLabel = findViewById(R.id.textview5);
        TextView rateLine = findViewById(R.id.tv_apply_interest_line);
        TextView daysLabel = findViewById(R.id.textview7);
        amountInput = findViewById(R.id.et_apply_amount);
        daysInput = findViewById(R.id.et_apply_periods);
        noteInput = findViewById(R.id.et_apply_reason);
        empty = findViewById(R.id.tv_apply_calc_empty);
        rows = findViewById(R.id.layout_apply_calc_rows);
        after = findViewById(R.id.tv_apply_calc_after);
        principal = findViewById(R.id.tv_apply_calc_principal);
        interest = findViewById(R.id.tv_apply_calc_interest);
        total = findViewById(R.id.tv_apply_calc_total);
        TextView dest = findViewById(R.id.tv_apply_disbursement_wallet);
        View cancel = findViewById(R.id.btn_apply_cancel);
        View review = findViewById(R.id.btn_apply_review);

        String unit = Amounts.unitLabel(coin);
        if (amountLabel != null) amountLabel.setText("AMOUNT (" + unit + ")");
        if (daysLabel != null) daysLabel.setText("I PLAN TO KEEP IT FOR (1 TO 30 DAYS)");
        if (rateLine != null) rateLine.setText(rate > 0
                ? String.format(java.util.Locale.US, "%.2f%% / daily", rate) : "0% (no interest)");
        if (dest != null) dest.setText(shortAddr(myWallet));
        paintAvailable();

        TextWatcher watch = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { paintQuote(); }
        };
        if (amountInput != null) amountInput.addTextChangedListener(watch);
        if (daysInput != null) daysInput.addTextChangedListener(watch);
        if (cancel != null) cancel.setOnClickListener(v -> finish());
        if (review != null) review.setOnClickListener(v -> review());
        loadAvailable();
    }

    private void loadAvailable() {
        if (!groupWallet.startsWith("HV0x")) return;
        String code = Amounts.stableCode(coin);
        if (!code.isEmpty()) {
            HusdClient.balance(code, groupWallet, new HusdClient.Cb() {
                @Override public void ok(org.json.JSONObject r) {
                    long raw = r.optLong("availableRaw", r.optLong("balanceRaw", 0));
                    available = Amounts.human(raw, coin);
                    runOnUiThread(LoanRequestActivity.this::paintAvailable);
                }
                @Override public void fail(String error) { }
            });
            return;
        }
        com.harvana.app.sync.HarvanaNodeClient.getWallet(groupWallet, new com.harvana.app.sync.HarvanaNodeClient.JsonCallback() {
            @Override public void onSuccess(org.json.JSONObject json) {
                available = com.harvana.app.sync.HarvanaNodeClient.toHarvDisplay(json.optLong("balance", 0L));
                runOnUiThread(LoanRequestActivity.this::paintAvailable);
            }
            @Override public void onError(String error) { }
        });
    }

    private void paintAvailable() {
        if (subtitle == null) return;
        String unit = Amounts.unitLabel(coin);
        subtitle.setText(available > 0
                ? "Group wallet has " + BalanceFormatter.smartDecimal(available) + " " + unit + " available."
                : "Group wallet has 0 " + unit + " available.");
    }

    private void paintQuote() {
        double amt = BalanceFormatter.parseUserInput(amountInput == null ? "" : amountInput.getText().toString());
        int days = days();
        if (amt <= 0) {
            if (empty != null) empty.setVisibility(View.VISIBLE);
            if (rows != null) rows.setVisibility(View.GONE);
            return;
        }
        double due = com.harvana.app.group.LoanInterest.interest(amt, rate, days);
        String unit = Amounts.unitLabel(coin);
        if (empty != null) empty.setVisibility(View.GONE);
        if (rows != null) rows.setVisibility(View.VISIBLE);
        if (after != null) after.setText("If held " + days + (days == 1 ? " day" : " days")
                + " at " + rate + "% a day. Repay earlier and you pay interest only for the days held.");
        if (principal != null) principal.setText(BalanceFormatter.smartDecimal(amt) + " " + unit);
        if (interest != null) interest.setText(BalanceFormatter.smartDecimal(due) + " " + unit);
        if (total != null) total.setText(BalanceFormatter.smartDecimal(amt + due) + " " + unit);
    }

    private void review() {
        double amt = BalanceFormatter.parseUserInput(amountInput == null ? "" : amountInput.getText().toString());
        if (amt <= 0) { toast("Enter a valid amount."); return; }
        if (available >= 0 && amt > available) {
            toast("Can't request more than the group wallet holds ("
                    + BalanceFormatter.smartDecimal(available) + " " + Amounts.unitLabel(coin) + " available).");
            return;
        }
        int days = days();
        if (days < com.harvana.app.group.LoanInterest.MIN_DAYS || days > com.harvana.app.group.LoanInterest.MAX_DAYS) {
            toast("Choose 1 to 30 days.");
            return;
        }
        String note = noteInput == null ? "" : noteInput.getText().toString().trim();
        findViewById(R.id.btn_apply_review).setEnabled(false);
        com.harvana.app.group.GroupLoanManager.applyForLoan(this, groupId, myWallet, amt, rate, "DAILY", note, days, coin,
                new com.harvana.app.group.GroupLoanManager.Callback() {
                    @Override public void onSuccess(String message) { runOnUiThread(() -> { toast(message); finish(); }); }
                    @Override public void onError(String error) {
                        runOnUiThread(() -> {
                            View b = findViewById(R.id.btn_apply_review);
                            if (b != null) b.setEnabled(true);
                            toast(error);
                        });
                    }
                });
    }

    private int days() {
        try {
            String raw = daysInput == null ? "1" : daysInput.getText().toString().trim();
            return com.harvana.app.group.LoanInterest.clampDays(raw.isEmpty() ? 1 : Integer.parseInt(raw));
        } catch (Exception e) { return 1; }
    }

    private void toast(String m) { Toast.makeText(this, m == null ? "" : m, Toast.LENGTH_LONG).show(); }
    private static String str(String s) { return s == null ? "" : s.trim(); }
    private static String shortAddr(String a) {
        if (a == null || a.length() < 12) return a == null ? "" : a;
        return a.substring(0, 6) + "…" + a.substring(a.length() - 4);
    }
}
