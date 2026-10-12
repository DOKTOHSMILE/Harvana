package com.harvana.app.husd;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
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
 * Mint a coin (hNGN) with HARV: lock HARV, mint up to half its naira value, repay, and take the HARV back. A position
 * worth less than 150% of what it owes can be closed by anyone who pays its debt in the coin; they get that debt's
 * worth of HARV plus 10% out of the position. Open with extra "asset" (default HNGN).
 */
public class VaultActivity extends AppCompatActivity {

    private String address, code = "HNGN", sym = "hNGN";
    private JSONObject info = new JSONObject(), pos = new JSONObject();
    private TextView tvColl, tvDebt, tvRatio, tvCanMint, tvPrice, tvHow;
    private LinearLayout risky;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        address = WalletManager.getActiveWalletAddress(this);
        String asked = getIntent() == null ? null : getIntent().getStringExtra(HusdActivity.EXTRA_ASSET);
        if (asked != null && !asked.isEmpty()) code = asked;
        sym = Stable.symbol(code);

        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(UiColors.background());
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(24));
        sv.addView(root);

        LinearLayout head = card();
        head.addView(text("Your vault", 15, true, UiColors.textPrimary()));
        tvColl = row(head, "HARV locked");
        tvDebt = row(head, sym + " owed");
        tvRatio = row(head, "Cover");
        tvCanMint = row(head, "You can mint now");
        tvPrice = text("…", 12, false, UiColors.textSecondary());
        tvPrice.setPadding(0, dp(8), 0, 0);
        head.addView(tvPrice);
        LinearLayout a1 = new LinearLayout(this);
        a1.setPadding(0, dp(12), 0, 0);
        a1.addView(button("Lock HARV", this::lock), weight());
        a1.addView(button("Mint " + sym, this::mint), weight());
        head.addView(a1);
        LinearLayout a2 = new LinearLayout(this);
        a2.addView(button("Repay " + sym, this::repay), weight());
        a2.addView(button("Take HARV back", this::withdraw), weight());
        head.addView(a2);
        root.addView(head);

        root.addView(title("How it works"));
        LinearLayout hc = card();
        tvHow = text("…", 13, false, UiColors.textSecondary());
        hc.addView(tvHow);
        root.addView(hc);

        root.addView(title("Vaults under the line"));
        risky = card();
        root.addView(risky);

        AppBar.setContent(this, sv, "Mint " + sym + " with HARV", "Lock HARV, borrow " + sym + ", repay to unlock");
        tvHow.setText(howText());
        load();
    }

    @Override protected void onResume() { super.onResume(); load(); }

    private void load() {
        if (address == null || address.isEmpty()) { tvPrice.setText("No wallet"); return; }
        NodeGroupClient.vaultInfo(code, new NodeGroupClient.Callback() {
            @Override public void onSuccess(JSONObject r) { runOnUiThread(() -> { info = r; tvHow.setText(howText()); showPrice(); }); }
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
        if (!info.optBoolean("available")) { tvPrice.setText(sym + " isn't switched on for minting with HARV yet."); return; }
        if (info.optBoolean("priceOk")) {
            tvPrice.setText("HARV price: ₦" + naira(info.optLong("koboPerHarv")) + " (from completed P2P trades)");
        } else {
            tvPrice.setText(priceProblem(info.optString("priceProblem")));
        }
    }

    private void showPosition() {
        long coll = pos.optLong("collateralRaw"), debt = pos.optLong("debtRaw");
        tvColl.setText(BalanceFormatter.rawShort(coll) + " HARV" + (pos.has("collateralValueRaw")
                ? "  (₦" + HusdClient.format(pos.optLong("collateralValueRaw")) + ")" : ""));
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
            risky.addView(text("None. Every vault is above the line.", 13, false, UiColors.textTertiary()));
            return;
        }
        for (int i = 0; i < arr.length(); i++) {
            JSONObject p = arr.optJSONObject(i);
            if (p == null) continue;
            String owner = p.optString("owner");
            long debt = p.optLong("debtRaw");
            LinearLayout r = new LinearLayout(this);
            r.setGravity(Gravity.CENTER_VERTICAL);
            r.setPadding(0, dp(6), 0, dp(6));
            TextView t = text(shortAddr(owner) + "\nOwes " + HusdClient.format(debt) + " " + sym + " · cover " + (p.optInt("ratioBps") / 100) + "%",
                    13, false, UiColors.textPrimary());
            r.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            if (!owner.equals(address)) r.addView(button("Liquidate", () -> liquidate(owner, debt)));
            risky.addView(r);
        }
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    private void lock() {
        if (!ready(false)) return;
        askAmount("Lock HARV", "HARV to lock", true, raw -> confirm("Lock HARV", BalanceFormatter.rawShort(raw) + " HARV",
                new String[][]{{"Goes to", "Your vault (only you can take it back)"},
                        {"Counts", "Once the next block has it"},
                        {"Network fee", "Normal HARV gas"}},
                () -> run("LOCK", raw, null, "HARV locked. It counts once the next block has it.")));
    }

    private void mint() {
        if (!ready(true)) return;
        long can = pos.optLong("canMintRaw");
        long min = info.optLong("minMintRaw", 1000 * HusdClient.UNIT);
        if (can < min) { toast("Lock more HARV first: you can mint " + HusdClient.format(can) + " " + sym + " now, and the minimum is " + HusdClient.format(min) + "."); return; }
        askAmount("Mint " + sym, "Amount (up to " + HusdClient.format(can) + ")", false, raw -> {
            if (raw > can) { toast("At most " + HusdClient.format(can) + " " + sym + " against your HARV."); return; }
            if (raw < min) { toast("The minimum is " + HusdClient.format(min) + " " + sym + "."); return; }
            confirm("Mint " + sym, HusdClient.format(raw) + " " + sym, new String[][]{
                            {"You owe", HusdClient.format(pos.optLong("debtRaw") + raw) + " " + sym},
                            {"Keep above", (info.optInt("liquidationRatioBps", 15000) / 100) + "% or the vault can be liquidated"},
                            {"Network fee", "A small HARV action fee"}},
                    () -> run("MINT", raw, null, sym + " minted. It arrives with the next block."));
        });
    }

    private void repay() {
        long debt = pos.optLong("debtRaw");
        if (debt <= 0) { toast("You don't owe any " + sym + "."); return; }
        askAmount("Repay " + sym, "Amount (you owe " + HusdClient.format(debt) + ")", false, raw -> {
            if (raw > debt) { toast("You owe " + HusdClient.format(debt) + " " + sym + "."); return; }
            confirm("Repay " + sym, HusdClient.format(raw) + " " + sym, new String[][]{
                            {"Still owed", HusdClient.format(debt - raw) + " " + sym},
                            {"The coins", "Are burned: they leave circulation"},
                            {"Network fee", "A small HARV action fee"}},
                    () -> run("REPAY", raw, null, "Repayment sent."));
        });
    }

    private void withdraw() {
        long coll = pos.optLong("collateralRaw");
        if (coll <= 0) { toast("No HARV is locked."); return; }
        askAmount("Take HARV back", "HARV (locked: " + BalanceFormatter.rawShort(coll) + ")", true, raw -> {
            if (raw > coll) { toast("Only " + BalanceFormatter.rawShort(coll) + " HARV is locked."); return; }
            confirm("Take HARV back", BalanceFormatter.rawShort(raw) + " HARV", new String[][]{
                            {"Rule", "What stays must cover your debt " + (info.optInt("mintRatioBps", 20000) / 100) + "%"},
                            {"Network fee", "A small HARV action fee"}},
                    () -> run("WITHDRAW", raw, null, "Your HARV is on its way back."));
        });
    }

    private void liquidate(String owner, long debt) {
        if (!ready(true)) return;
        int bonus = info.optInt("liquidationBonusBps", 1000) / 100;
        confirm("Liquidate vault", HusdClient.format(debt) + " " + sym, new String[][]{
                        {"Vault", shortAddr(owner)},
                        {"You pay", HusdClient.format(debt) + " " + sym + " of your own (burned)"},
                        {"You get", "That much in HARV plus " + bonus + "%, from the vault"},
                        {"Network fee", "A small HARV action fee"}},
                () -> run("LIQUIDATE", 0, owner, "Liquidation sent. The HARV arrives after the next block."));
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
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(8), dp(20), 0);
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        e.setSingleLine(true);
        box.addView(e);
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this).setTitle(title).setView(box)
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
        if (!info.optBoolean("available")) { toast(sym + " isn't switched on for minting with HARV yet."); return false; }
        if (needPrice && !info.optBoolean("priceOk")) { toast(priceProblem(info.optString("priceProblem"))); return false; }
        return true;
    }

    private String priceProblem(String p) {
        if (p.contains("NOT_LIVE")) return "Minting opens once HARV has a market price: the network needs 10 completed P2P trades first. You can lock HARV now.";
        if (p.contains("STALE")) return "The HARV price is out of date (no recent P2P trades), so minting is paused for safety.";
        return "Minting is paused: the HARV price isn't usable right now" + (p.isEmpty() ? "." : " (" + p + ").");
    }

    private String howText() {
        int mint = info.optInt("mintRatioBps", 20000) / 100, liq = info.optInt("liquidationRatioBps", 15000) / 100,
                bonus = info.optInt("liquidationBonusBps", 1000) / 100;
        return "• Lock HARV in your vault. Only you can take it back.\n"
                + "• Mint " + sym + " up to the HARV's naira value divided by " + mint + "% (₦20,000 of HARV mints up to ₦10,000).\n"
                + "• The HARV price comes from completed P2P trades on Harvana. If it is missing or old, minting pauses.\n"
                + "• If HARV falls and your vault covers less than " + liq + "% of what you owe, anyone can repay your debt with their own "
                + sym + " and take that much HARV plus " + bonus + "% from your vault. The rest stays yours.\n"
                + "• Repay " + sym + " (it is burned) and take your HARV back whenever you like.\n"
                + "• Every " + sym + " minted this way is owed by a vault holding more HARV than it is worth, so the coin stays backed.";
    }

    private String plain(String e) {
        if (e == null) return "Couldn't send.";
        if (com.harvana.app.utils.ErrorText.is(e, "NOT_ENOUGH_COLLATERAL")) return "Not enough HARV locked for that amount.";
        if (com.harvana.app.utils.ErrorText.is(e, "WOULD_BE_UNDER_COLLATERALISED")) return "That would leave too little HARV for what you owe. Repay first.";
        if (com.harvana.app.utils.ErrorText.is(e, "PRICE_NOT_LIVE_YET") || com.harvana.app.utils.ErrorText.is(e, "PRICE_")) return priceProblem(e);
        if (com.harvana.app.utils.ErrorText.is(e, "VAULT_CAP_REACHED")) return "The network's limit for minted " + sym + " is reached for now.";
        if (com.harvana.app.utils.ErrorText.is(e, "POSITION_IS_SAFE")) return "That vault is back above the line.";
        if (com.harvana.app.utils.ErrorText.is(e, "MORE_THAN_THE_DEBT")) return "That is more than is owed.";
        if (com.harvana.app.utils.ErrorText.is(e, "INSUFFICIENT")) return "Not enough balance for this and its fee.";
        if (com.harvana.app.utils.ErrorText.is(e, "BELOW_MINIMUM")) return "That is below the minimum.";
        return com.harvana.app.utils.ErrorText.plain(e);
    }

    private static String naira(long kobo) {
        return String.format(java.util.Locale.US, "%,d.%02d", kobo / 100, kobo % 100);
    }

    private static String shortAddr(String a) { return a.length() > 14 ? a.substring(0, 8) + "…" + a.substring(a.length() - 4) : a; }

    private TextView row(LinearLayout parent, String label) {
        LinearLayout r = new LinearLayout(this);
        r.setPadding(0, dp(6), 0, 0);
        TextView l = text(label, 13, false, UiColors.textSecondary());
        r.addView(l, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView v = text("…", 14, true, UiColors.textPrimary());
        v.setGravity(Gravity.END);
        r.addView(v, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.3f));
        parent.addView(r);
        return v;
    }

    private TextView title(String s) {
        TextView t = text(s, 15, true, UiColors.textPrimary());
        t.setPadding(dp(4), dp(18), 0, dp(6));
        return t;
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(16), dp(16), dp(16), dp(16));
        android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
        g.setCornerRadius(dp(18));
        g.setColor(UiColors.surface());
        c.setBackground(g);
        return c;
    }

    private MaterialButton button(String label, Runnable click) {
        MaterialButton b = new MaterialButton(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setOnClickListener(v -> click.run());
        return b;
    }

    private LinearLayout.LayoutParams weight() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(dp(4), 0, dp(4), 0);
        return lp;
    }

    private TextView text(String s, float sp, boolean bold, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(null, Typeface.BOLD);
        t.setPadding(0, dp(2), 0, dp(2));
        return t;
    }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }
    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
