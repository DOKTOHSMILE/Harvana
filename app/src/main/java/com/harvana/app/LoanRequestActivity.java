package com.harvana.app;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.harvana.app.engine.Amounts;
import com.harvana.app.group.LoanInterest;
import com.harvana.app.husd.HusdClient;
import com.harvana.app.utils.StatusBarUtil;
import com.harvana.app.wallet.BalanceFormatter;

/**
 * Applying for a group loan. The most that can be asked is 30% of what the group wallet holds, in the coin
 * the group saves in. Nothing is sent until the review is confirmed.
 */
public class LoanRequestActivity extends AppCompatActivity {
    public static final String EXTRA_GROUP = "groupId";
    public static final String EXTRA_RATE = "rate";
    public static final String EXTRA_COIN = "coin";
    public static final String EXTRA_WALLET = "wallet";
    public static final String EXTRA_GROUP_WALLET = "groupWallet";
    public static final String EXTRA_AVAILABLE = "available";

    /** The offer on this screen: 30% of the group wallet. The group still votes before anything is paid. */
    private static final double OFFER = 0.30;
    private static final double[] PRESETS = {5_000, 10_000, 20_000};
    private static final int[] DAY_CHOICES = {1, 7, 15, 30};

    private String groupId = "", coin = "", myWallet = "", groupWallet = "";
    private double rate, available;
    private int selectedDays = 15;
    private boolean painting, seeded;

    private TextView offerBalance, offerMax, repayPrincipal, repayInterest, repayDays, repayTotal, purposeCount, walletLine;
    private EditText amountInput, daysInput, purposeInput;
    private View clearAmount;
    private LinearLayout chipRow;
    private MaterialButton[] dayButtons;

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

        setContentView(R.layout.activity_loan_request);
        StatusBarUtil.matchToolbar(this);

        offerBalance = findViewById(R.id.tv_offer_balance);
        offerMax = findViewById(R.id.tv_offer_max);
        repayPrincipal = findViewById(R.id.tv_repay_principal);
        repayInterest = findViewById(R.id.tv_repay_interest);
        repayDays = findViewById(R.id.tv_repay_days);
        repayTotal = findViewById(R.id.tv_repay_total);
        purposeCount = findViewById(R.id.tv_purpose_count);
        walletLine = findViewById(R.id.tv_loan_wallet);
        amountInput = findViewById(R.id.et_loan_amount);
        daysInput = findViewById(R.id.et_loan_days);
        purposeInput = findViewById(R.id.et_loan_purpose);
        clearAmount = findViewById(R.id.btn_loan_clear);
        chipRow = findViewById(R.id.row_amount_chips);
        dayButtons = new MaterialButton[]{
                findViewById(R.id.btn_days_1), findViewById(R.id.btn_days_7),
                findViewById(R.id.btn_days_15), findViewById(R.id.btn_days_30)};

        TextView coinChip = findViewById(R.id.tv_loan_coin);
        String unit = unit();
        if (coinChip != null) coinChip.setText(unit);
        if (walletLine != null) walletLine.setText(shortAddr(myWallet));

        findViewById(R.id.btn_loan_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_loan_cancel).setOnClickListener(v -> finish());
        findViewById(R.id.btn_loan_review).setOnClickListener(v -> review());
        findViewById(R.id.btn_loan_copy).setOnClickListener(v -> copyWallet());
        if (coinChip != null) coinChip.setOnClickListener(v ->
                toast("This group saves in " + unit + ". The loan is in that coin."));
        if (clearAmount != null) clearAmount.setOnClickListener(v -> amountInput.setText(""));

        TextWatcher watch = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { if (!painting) paint(); }
        };
        amountInput.addTextChangedListener(watch);
        daysInput.addTextChangedListener(watch);
        purposeInput.addTextChangedListener(watch);
        for (int i = 0; i < dayButtons.length; i++) {
            int days = DAY_CHOICES[i];
            dayButtons[i].setOnClickListener(v -> {
                selectedDays = days;
                painting = true;
                daysInput.setText("");
                painting = false;
                paint();
            });
        }
        paint();
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
                    runOnUiThread(LoanRequestActivity.this::paint);
                }
                @Override public void fail(String error) { }
            });
            return;
        }
        com.harvana.app.sync.HarvanaNodeClient.getWallet(groupWallet, new com.harvana.app.sync.HarvanaNodeClient.JsonCallback() {
            @Override public void onSuccess(org.json.JSONObject json) {
                available = com.harvana.app.sync.HarvanaNodeClient.toHarvDisplay(json.optLong("balance", 0L));
                runOnUiThread(LoanRequestActivity.this::paint);
            }
            @Override public void onError(String error) { }
        });
    }

    private double maxOffer() { return available > 0 ? available * OFFER : 0; }

    private double amount() {
        return BalanceFormatter.parseUserInput(amountInput.getText().toString());
    }

    private int days() {
        String raw = daysInput.getText().toString().trim();
        if (!raw.isEmpty()) {
            try { return Integer.parseInt(raw); } catch (NumberFormatException ignored) { return selectedDays; }
        }
        return selectedDays;
    }

    private void paint() {
        if (!seeded && amountInput.getText().length() == 0 && maxOffer() > 0) {
            seeded = true;
            painting = true;
            amountInput.setText(trim(maxOffer()));
            painting = false;
        }
        String unit = unit();
        double max = maxOffer();
        offerBalance.setText(money(available) + " " + unit);
        offerMax.setText(max > 0 ? money(max) + " " + unit : "0 " + unit);
        if (clearAmount != null) clearAmount.setVisibility(amountInput.getText().length() == 0 ? View.GONE : View.VISIBLE);
        purposeCount.setText(purposeInput.getText().length() + "/200");
        paintChips(max);
        paintDays();

        double amt = amount();
        int d = days();
        if (amt <= 0) {
            repayPrincipal.setText("—");
            repayInterest.setText("—");
            repayDays.setText("");
            repayTotal.setText("—");
            return;
        }
        if (d < LoanInterest.MIN_DAYS || d > LoanInterest.MAX_DAYS) {
            repayPrincipal.setText(money(amt) + " " + unit);
            repayInterest.setText("—");
            repayDays.setText("Use 1 to 30 days");
            repayTotal.setText("—");
            return;
        }
        double due = LoanInterest.interest(amt, rate, d);
        repayPrincipal.setText(money(amt) + " " + unit);
        repayInterest.setText(money(due) + " " + unit);
        repayDays.setText("(" + d + (d == 1 ? " day)" : " days)"));
        repayTotal.setText(money(amt + due) + " " + unit);
    }

    private void paintChips(double max) {
        chipRow.removeAllViews();
        int gap = dp(8);
        for (double preset : PRESETS) {
            if (max > 0 && preset <= max + 0.0001) chipRow.addView(chip(money(preset), preset), chipLp(gap));
        }
        if (max > 0) chipRow.addView(chip(money(max), max), chipLp(0));
    }

    private LinearLayout.LayoutParams chipLp(int end) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(40));
        lp.setMarginEnd(end);
        return lp;
    }

    private MaterialButton chip(String label, double value) {
        MaterialButton b = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        b.setText(label);
        b.setAllCaps(false);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setInsetTop(0);
        b.setInsetBottom(0);
        b.setCornerRadius(dp(20));
        boolean selected = Math.abs(amount() - value) < 0.01;
        int accent = ContextCompat.getColor(this, R.color.colorAccent);
        int onAccent = ContextCompat.getColor(this, R.color.colorTextOnAccent);
        int text = ContextCompat.getColor(this, R.color.colorTextPrimary);
        int stroke = ContextCompat.getColor(this, R.color.colorFieldBorder);
        b.setStrokeWidth(dp(1));
        if (selected) {
            b.setBackgroundTintList(ColorStateList.valueOf(accent));
            b.setTextColor(onAccent);
            b.setStrokeColor(ColorStateList.valueOf(accent));
        } else {
            b.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
            b.setTextColor(text);
            b.setStrokeColor(ColorStateList.valueOf(stroke));
        }
        b.setOnClickListener(v -> {
            painting = true;
            amountInput.setText(trim(value));
            amountInput.setSelection(amountInput.getText().length());
            painting = false;
            paint();
        });
        return b;
    }

    private void paintDays() {
        int d = days();
        boolean typed = daysInput.getText().toString().trim().length() > 0;
        int accent = ContextCompat.getColor(this, R.color.colorAccent);
        int text = ContextCompat.getColor(this, R.color.colorTextPrimary);
        int stroke = ContextCompat.getColor(this, R.color.colorFieldBorder);
        for (int i = 0; i < dayButtons.length; i++) {
            boolean on = !typed && DAY_CHOICES[i] == selectedDays || typed && DAY_CHOICES[i] == d;
            MaterialButton b = dayButtons[i];
            b.setStrokeWidth(dp(on ? 1.5f : 1));
            b.setStrokeColor(ColorStateList.valueOf(on ? accent : stroke));
            b.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
            b.setTextColor(on ? accent : text);
        }
    }

    private void review() {
        double amt = amount();
        double max = maxOffer();
        String unit = unit();
        if (amt <= 0) { toast("Enter a valid amount."); return; }
        if (max <= 0) {
            toast("This group wallet has no " + unit + " to lend yet.");
            return;
        }
        if (amt > max + 0.0001) {
            toast("The most this group can offer is " + money(max) + " " + unit + " (30% of the group wallet).");
            return;
        }
        int d = days();
        if (d < LoanInterest.MIN_DAYS || d > LoanInterest.MAX_DAYS) {
            toast("Choose 1 to 30 days.");
            return;
        }
        double due = LoanInterest.interest(amt, rate, d);
        String purpose = purposeInput.getText().toString().trim();
        String body = money(amt) + " " + unit + " for " + d + (d == 1 ? " day" : " days")
                + "\nInterest " + money(due) + " " + unit
                + "\nYou repay " + money(amt + due) + " " + unit
                + "\nTo " + shortAddr(myWallet)
                + (purpose.isEmpty() ? "" : "\nPurpose: " + purpose)
                + "\n\nThe group still has to approve this. Nothing moves until it does.";
        new com.harvana.app.ui.SheetDialog.Builder(this)
                .setTitle("Review application")
                .setMessage(body)
                .setNegativeButton("Back", null)
                .setPositiveButton("Submit", (dlg, w) -> submit(amt, d, purpose))
                .show();
    }

    private void submit(double amt, int days, String purpose) {
        View review = findViewById(R.id.btn_loan_review);
        review.setEnabled(false);
        com.harvana.app.group.GroupLoanManager.applyForLoan(this, groupId, myWallet, amt, rate, "DAILY", purpose, days, coin,
                new com.harvana.app.group.GroupLoanManager.Callback() {
                    @Override public void onSuccess(String message) { runOnUiThread(() -> { toast(message); finish(); }); }
                    @Override public void onError(String error) {
                        runOnUiThread(() -> {
                            review.setEnabled(true);
                            toast(error);
                        });
                    }
                });
    }

    private void copyWallet() {
        if (myWallet.isEmpty()) { toast("No wallet is linked yet."); return; }
        ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("wallet", myWallet));
        toast("Wallet address copied.");
    }

    private String unit() {
        String u = Amounts.unitLabel(coin);
        return u == null || u.isEmpty() ? "HARV" : u;
    }

    private static String money(double n) { return BalanceFormatter.smartDecimal(n); }

    /** A chip writes a plain number, not "36,000", so the field can be parsed again. */
    private static String trim(double n) {
        if (n == Math.rint(n)) return String.valueOf((long) n);
        return String.valueOf(n);
    }

    private int dp(float d) { return Math.round(d * getResources().getDisplayMetrics().density); }

    private void toast(String m) { Toast.makeText(this, m == null ? "" : m, Toast.LENGTH_LONG).show(); }

    private static String str(String s) { return s == null ? "" : s.trim(); }

    private static String shortAddr(String a) {
        if (a == null || a.length() < 12) return a == null || a.isEmpty() ? "No wallet linked" : a;
        return a.substring(0, 6) + "…" + a.substring(a.length() - 4);
    }
}
