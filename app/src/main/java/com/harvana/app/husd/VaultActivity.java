package com.harvana.app.husd;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.harvana.app.sync.NodeGroupClient;
import com.harvana.app.utils.AppBar;
import com.harvana.app.utils.UiColors;
import com.harvana.app.wallet.BalanceFormatter;
import com.harvana.app.wallet.WalletManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Mint a coin (hNGN, hUSD, …) with HARV: lock HARV, mint up to half its value in the coin, repay, and take the HARV back. A position
 * worth less than 150% of what it owes can be closed by anyone who pays its debt in the coin; they get that debt's
 * worth of HARV plus 10% out of the position. Open with extra "asset" (default HNGN).
 */
public class VaultActivity extends AppCompatActivity {

    private String address, code = "HNGN", sym = "hNGN";
    private JSONObject info = new JSONObject(), pos = new JSONObject();
    private TextView tvColl, tvDebt, tvRatio, tvCanMint, tvPrice;
    private LinearLayout risky;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        address = WalletManager.getActiveWalletAddress(this);
        String asked = getIntent() == null ? null : getIntent().getStringExtra(HusdActivity.EXTRA_ASSET);
        if (asked != null && !asked.isEmpty()) code = asked;
        sym = Stable.symbol(code);

        AppBar.setContent(this, com.harvana.app.R.layout.activity_vault, "Mint " + sym + " with HARV", null);
        tvColl = findViewById(com.harvana.app.R.id.tv_vault_locked);
        tvDebt = findViewById(com.harvana.app.R.id.tv_vault_owed);
        ((TextView) findViewById(com.harvana.app.R.id.tv_vault_owed_label)).setText(sym + " owed");
        tvRatio = findViewById(com.harvana.app.R.id.tv_vault_cover);
        tvCanMint = findViewById(com.harvana.app.R.id.tv_vault_can_mint);
        tvPrice = findViewById(com.harvana.app.R.id.tv_vault_price);
        com.google.android.material.button.MaterialButton mintBtn = findViewById(com.harvana.app.R.id.btn_vault_mint), repayBtn = findViewById(com.harvana.app.R.id.btn_vault_repay);
        mintBtn.setText("Mint " + sym);
        repayBtn.setText("Repay " + sym);
        findViewById(com.harvana.app.R.id.btn_vault_lock).setOnClickListener(v -> lock());
        mintBtn.setOnClickListener(v -> mint());
        repayBtn.setOnClickListener(v -> repay());
        findViewById(com.harvana.app.R.id.btn_vault_withdraw).setOnClickListener(v -> withdraw());
        risky = findViewById(com.harvana.app.R.id.vault_risky);
        load();
    }

    @Override protected void onResume() { super.onResume(); load(); }

    private void load() {
        if (address == null || address.isEmpty()) { tvPrice.setText("No wallet"); return; }
        NodeGroupClient.vaultInfo(code, new NodeGroupClient.Callback() {
            @Override public void onSuccess(JSONObject r) { runOnUiThread(() -> { info = r; showPrice(); }); }
            @Override public void onError(String e) { runOnUiThread(() -> tvPrice.setText(com.harvana.app.utils.ErrorText.plain(e))); }
        });
        NodeGroupClient.vaultPosition(code, address, new NodeGroupClient.Callback() {
            @Override public void onSuccess(JSONObject r) { runOnUiThread(() -> { pos = r; showPosition(); }); }
            @Override public void onError(String e) { }
        });
        NodeGroupClient.vaultAtRisk(code, new NodeGroupClient.Callback() {
            @Override public void onSuccess(JSONObject r) { runOnUiThread(() -> showAtRisk(r.optJSONArray("positions"))); }
            @Override public void onError(String e) { runOnUiThread(() -> showAtRisk(null)); }
        });
    }

    private void showPrice() {
        if (!info.optBoolean("available")) { tvPrice.setText("—"); return; }
        if (info.optBoolean("priceOk")) {
            if ("SWAP_24H_LOW".equals(info.optString("priceSource")) && info.has("borrowKoboPerHarv"))
                tvPrice.setText(cur() + naira(info.optLong("borrowKoboPerHarv")) + " · Swap 24h low");
            else tvPrice.setText(cur() + naira(info.optLong("koboPerHarv")));
        } else {
            tvPrice.setText(priceProblem(info.optString("priceProblem")));
        }
    }

    private void showPosition() {
        long coll = pos.optLong("collateralRaw"), debt = pos.optLong("debtRaw");
        tvColl.setText(BalanceFormatter.rawShort(coll) + " HARV" + (pos.has("collateralValueRaw")
                ? "  (" + cur() + HusdClient.format(pos.optLong("collateralValueRaw")) + ")" : ""));
        tvDebt.setText(HusdClient.format(debt) + " " + sym);
        if (debt <= 0 || pos.isNull("ratioBps")) {
            tvRatio.setText("Nothing owed");
            tvRatio.setTextColor(UiColors.textPrimary());
        } else {
            int bps = pos.optInt("ratioBps");
            int liq = info.optInt("liquidationRatioBps", 15000), mint = info.optInt("mintRatioBps", 20000);
            tvRatio.setText((bps / 100) + "%" + (pos.optBoolean("liquidatable") ? " · under " + (liq / 100) + "%, can be liquidated"
                    : bps < mint ? " · add HARV or repay to stay safe" : " · safe"));
            tvRatio.setTextColor(bps < liq ? UiColors.error() : bps < mint ? UiColors.warning() : UiColors.success());
        }
        tvCanMint.setText(pos.has("canMintRaw") ? HusdClient.format(pos.optLong("canMintRaw")) + " " + sym : "—");
    }

    private void showAtRisk(JSONArray arr) {
        risky.removeAllViews();
        if (arr == null || arr.length() == 0) {
            TextView none = (TextView) getLayoutInflater().inflate(com.harvana.app.R.layout.item_list_empty, risky, false);
            none.setText("None");
            none.setPadding(0, dp(12), 0, dp(12));
            risky.addView(none);
            return;
        }
        for (int i = 0; i < arr.length(); i++) {
            JSONObject p = arr.optJSONObject(i);
            if (p == null) continue;
            String owner = p.optString("owner");
            long debt = p.optLong("debtRaw");
            android.view.View r = getLayoutInflater().inflate(com.harvana.app.R.layout.item_vault_at_risk, risky, false);
            ((TextView) r.findViewById(com.harvana.app.R.id.tv_vault_risk_owner)).setText(shortAddr(owner));
            ((TextView) r.findViewById(com.harvana.app.R.id.tv_vault_risk_owes)).setText("Owes " + HusdClient.format(debt) + " " + sym + " · cover " + (p.optInt("ratioBps") / 100) + "%");
            if (!owner.equals(address)) {
                android.view.View liq = r.findViewById(com.harvana.app.R.id.btn_vault_liquidate);
                liq.setVisibility(android.view.View.VISIBLE);
                liq.setOnClickListener(v -> liquidate(owner, debt));
            }
            risky.addView(r);
        }
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    private void lock() {
        if (!ready(false)) return;
        askAmount("Lock HARV", "HARV to lock", true, raw -> confirm("Lock HARV", BalanceFormatter.rawShort(raw) + " HARV",
                new String[][]{{"To", "Your vault"}},
                () -> run("LOCK", raw, null, "HARV locked.")));
    }

    private void mint() {
        if (!ready(true)) return;
        long can = pos.optLong("canMintRaw");
        long min = info.optLong("minMintRaw", 1000 * HusdClient.UNIT);
        if (can < min) { toast("Minimum " + HusdClient.format(min) + " " + sym + ". Available: " + HusdClient.format(can) + "."); return; }
        askAmount("Mint " + sym, "Amount (up to " + HusdClient.format(can) + ")", false, raw -> {
            if (raw > can) { toast("Maximum " + HusdClient.format(can) + " " + sym + "."); return; }
            if (raw < min) { toast("Minimum " + HusdClient.format(min) + " " + sym + "."); return; }
            confirm("Mint " + sym, HusdClient.format(raw) + " " + sym, new String[][]{
                            {"You owe", HusdClient.format(pos.optLong("debtRaw") + raw) + " " + sym},
                            {"Liquidation below", (info.optInt("liquidationRatioBps", 15000) / 100) + "%"}},
                    () -> run("MINT", raw, null, sym + " minted."));
        });
    }

    private void repay() {
        long debt = pos.optLong("debtRaw");
        if (debt <= 0) { toast("You don't owe any " + sym + "."); return; }
        askAmount("Repay " + sym, "Amount (you owe " + HusdClient.format(debt) + ")", false, raw -> {
            if (raw > debt) { toast("You owe " + HusdClient.format(debt) + " " + sym + "."); return; }
            confirm("Repay " + sym, HusdClient.format(raw) + " " + sym, new String[][]{
                            {"Still owed", HusdClient.format(debt - raw) + " " + sym},
                            },
                    () -> run("REPAY", raw, null, "Repayment sent."));
        });
    }

    private void withdraw() {
        long coll = pos.optLong("collateralRaw");
        if (coll <= 0) { toast("No HARV is locked."); return; }
        askAmount("Take HARV back", "HARV (locked: " + BalanceFormatter.rawShort(coll) + ")", true, raw -> {
            if (raw > coll) { toast("Only " + BalanceFormatter.rawShort(coll) + " HARV is locked."); return; }
            confirm("Take HARV back", BalanceFormatter.rawShort(raw) + " HARV", new String[][]{
                            {"Locked after", BalanceFormatter.rawShort(coll - raw) + " HARV"}},
                    () -> run("WITHDRAW", raw, null, "Sent."));
        });
    }

    private void liquidate(String owner, long debt) {
        if (!ready(true)) return;
        int bonus = info.optInt("liquidationBonusBps", 1000) / 100;
        confirm("Liquidate vault", HusdClient.format(debt) + " " + sym, new String[][]{
                        {"Vault", shortAddr(owner)},
                        {"You pay", HusdClient.format(debt) + " " + sym},
                        {"Bonus", bonus + "%"}},
                () -> run("LIQUIDATE", 0, owner, "Liquidation sent."));
    }

    private void run(String action, long raw, String owner, String done) {
        toast("Sending…");
        NodeGroupClient.vault(this, action, code, address, raw, owner, new NodeGroupClient.Callback() {
            @Override public void onSuccess(JSONObject r) { runOnUiThread(() -> { toast(done); load(); }); }
            @Override public void onError(String e) { runOnUiThread(() -> toast(plain(e))); }
        });
    }

    // ── Shared ────────────────────────────────────────────────────────────────

    private interface Amount { void got(long raw); }

    private void askAmount(String title, String hint, boolean harv, Amount next) {
        android.view.View box = getLayoutInflater().inflate(com.harvana.app.R.layout.dialog_amount_entry, null, false);
        EditText e = box.findViewById(com.harvana.app.R.id.et_amount_entry);
        e.setHint(hint);
        new com.harvana.app.ui.SheetDialog.Builder(this).setTitle(title).setView(box)
                .setPositiveButton("Review", (d, w) -> {
                    String s = e.getText().toString().trim();
                    long raw;
                    if (harv) {
                        double v = BalanceFormatter.parseUserInput(s);
                        raw = v > 0 ? BalanceFormatter.harvToRaw(v) : 0;
                    } else {
                        Long u = HusdClient.parse(s);
                        raw = u == null ? 0 : u;
                    }
                    if (raw <= 0) { toast("Enter an amount."); return; }
                    next.got(raw);
                }).setNegativeButton("Cancel", null).show();
    }

    private void confirm(String title, String amount, String[][] rows, Runnable go) {
        List<String[]> list = new ArrayList<>();
        for (String[] r : rows) list.add(r);
        com.harvana.app.group.PaymentConfirmSheet.reviewThenAuthorize(this, title, amount, list, go);
    }

    /** Minting and liquidating need a live HARV price; locking only needs the coin to be on. */
    private boolean ready(boolean needPrice) {
        if (!info.optBoolean("available")) { toast(sym + " minting is off."); return false; }
        if (needPrice && !info.optBoolean("priceOk")) { toast("HARV price: " + priceProblem(info.optString("priceProblem"))); return false; }
        return true;
    }

    private String priceProblem(String p) {
        if (p.contains("NOT_LIVE")) return "Not live yet";
        if (p.contains("STALE")) return "Out of date";
        return "Unavailable";
    }


    private String plain(String e) {
        if (e == null) return "Couldn't send.";
        if (com.harvana.app.utils.ErrorText.is(e, "NOT_ENOUGH_COLLATERAL")) return "Not enough HARV locked for that amount.";
        if (com.harvana.app.utils.ErrorText.is(e, "POOL_TOO_SHALLOW")) return "Closed: the " + sym + " swap pool needs more HARV.";
        if (com.harvana.app.utils.ErrorText.is(e, "VAULT_NOT_AVAILABLE")) return "Not open for " + sym + ".";
        if (com.harvana.app.utils.ErrorText.is(e, "WOULD_BE_UNDER_COLLATERALISED")) return "That would leave too little HARV for what you owe. Repay first.";
        if (com.harvana.app.utils.ErrorText.is(e, "PRICE_NOT_LIVE_YET") || com.harvana.app.utils.ErrorText.is(e, "PRICE_")) return "HARV price: " + priceProblem(e);
        if (com.harvana.app.utils.ErrorText.is(e, "PRICES_DISAGREE") || com.harvana.app.utils.ErrorText.is(e, "LIQUIDATION_PENDING")
                || com.harvana.app.utils.ErrorText.is(e, "VAULT_HOURLY_LIMIT_REACHED")) return com.harvana.app.utils.ErrorText.plain(e);
        if (com.harvana.app.utils.ErrorText.is(e, "VAULT_CAP_REACHED")) return "The network's limit for minted " + sym + " is reached for now.";
        if (com.harvana.app.utils.ErrorText.is(e, "POSITION_IS_SAFE")) return "That vault is back above the line.";
        if (com.harvana.app.utils.ErrorText.is(e, "MORE_THAN_THE_DEBT")) return "That is more than is owed.";
        if (com.harvana.app.utils.ErrorText.is(e, "INSUFFICIENT")) return "Not enough balance for this and its fee.";
        if (com.harvana.app.utils.ErrorText.is(e, "BELOW_MINIMUM")) return "That is below the minimum.";
        return com.harvana.app.utils.ErrorText.plain(e);
    }

    /** The coin's money sign: ₦ for hNGN, $ for hUSD, € for hEUR, £ for hGBP, else the currency code. */
    private String cur() {
        String c = Stable.currency(code);
        return "NGN".equals(c) ? "₦" : "USD".equals(c) ? "$" : "EUR".equals(c) ? "€" : "GBP".equals(c) ? "£" : c + " ";
    }

    /** Hundredths of the coin per HARV as money (kobo for hNGN, cents for hUSD). */
    private static String naira(long kobo) {
        return String.format(java.util.Locale.US, "%,d.%02d", kobo / 100, kobo % 100);
    }

    private static String shortAddr(String a) { return a.length() > 14 ? a.substring(0, 8) + "…" + a.substring(a.length() - 4) : a; }







    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }
    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
