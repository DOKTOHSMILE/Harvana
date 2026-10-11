package com.harvana.app.husd;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.harvana.app.R;
import com.harvana.app.utils.AppBar;
import com.harvana.app.utils.UiColors;
import com.harvana.app.wallet.WalletManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * hUSD: dollars inside Harvana, backed one for one by USDC in the bridge vault. Deposit USDC from Base, Polygon and
 * the other supported chains without needing ETH, send hUSD to anyone on Harvana, and withdraw back to USDC.
 */
public class HusdActivity extends AppCompatActivity {

    private String address;
    private JSONObject info = new JSONObject();
    private JSONObject bal = new JSONObject();
    private TextView tvBalance, tvSub;
    private LinearLayout history;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        address = WalletManager.getActiveWalletAddress(this);
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(UiColors.background());
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(24));
        sv.addView(root);

        LinearLayout head = card();
        head.setGravity(Gravity.CENTER_HORIZONTAL);
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.ic_coin_husd);
        head.addView(logo, new LinearLayout.LayoutParams(dp(56), dp(56)));
        tvBalance = text("… hUSD", 30, true, UiColors.textPrimary());
        tvBalance.setGravity(Gravity.CENTER);
        com.harvana.app.utils.BalancePrivacy.guard(tvBalance);
        head.addView(tvBalance);
        tvSub = text("1 hUSD = 1 US dollar, backed by USDC", 13, false, UiColors.textSecondary());
        tvSub.setGravity(Gravity.CENTER);
        head.addView(tvSub);
        LinearLayout actions = new LinearLayout(this);
        actions.setPadding(0, dp(14), 0, 0);
        actions.addView(action("Deposit", v -> deposit()), weight());
        actions.addView(action("Send", v -> send()), weight());
        actions.addView(action("Withdraw", v -> withdraw()), weight());
        head.addView(actions);
        root.addView(head);

        TextView how = text("How it works", 15, true, UiColors.textPrimary());
        how.setPadding(dp(4), dp(18), 0, dp(6));
        root.addView(how);
        LinearLayout hc = card();
        hc.addView(text("• Deposit USDC from Base, Polygon, Arbitrum, Ethereum or BNB Chain to your own deposit address. You don't need ETH or any gas.\n"
                + "• Send hUSD to anyone on Harvana. If you hold HARV, the fee is paid in HARV; if not, it is 0.02 hUSD. Your first 3 hUSD-paid transfers are free.\n"
                + "• Withdraw hUSD back to USDC at any address you choose. A 0.50 hUSD fee covers the gas on that chain. The minimum is 5 hUSD.",
                13, false, UiColors.textSecondary()));
        root.addView(hc);

        TextView wtitle = text("Withdrawals", 15, true, UiColors.textPrimary());
        wtitle.setPadding(dp(4), dp(18), 0, dp(6));
        root.addView(wtitle);
        history = card();
        root.addView(history);

        AppBar.setContent(this, sv, "hUSD", "Dollars inside Harvana");
        load();
    }

    @Override protected void onResume() { super.onResume(); load(); }

    private void load() {
        if (address == null || address.isEmpty()) { tvBalance.setText("No wallet"); return; }
        HusdClient.info(new HusdClient.Cb() {
            @Override public void ok(JSONObject r) { info = r; runOnUiThread(() -> {
                if (!r.optBoolean("active")) tvSub.setText("hUSD is not switched on yet on the network.");
            }); }
            @Override public void fail(String e) { }
        });
        HusdClient.balance(address, new HusdClient.Cb() {
            @Override public void ok(JSONObject r) { bal = r; runOnUiThread(() -> {
                tvBalance.setText(HusdClient.format(r.optLong("availableRaw")) + " hUSD");
                long pin = r.optLong("pendingInRaw"), pout = r.optLong("pendingOutRaw");
                String extra = pin > 0 ? " · " + HusdClient.format(pin) + " arriving" : (pout > 0 ? " · " + HusdClient.format(pout) + " leaving" : "");
                tvSub.setText(r.optInt("freeTransfersLeft") + " free transfers left" + extra);
            }); }
            @Override public void fail(String e) { runOnUiThread(() -> tvSub.setText("Couldn't load: " + e)); }
        });
        HusdClient.withdrawals(address, new HusdClient.Cb() {
            @Override public void ok(JSONObject r) { runOnUiThread(() -> showWithdrawals(r.optJSONArray("withdrawals"))); }
            @Override public void fail(String e) { }
        });
    }

    private void showWithdrawals(JSONArray arr) {
        history.removeAllViews();
        if (arr == null || arr.length() == 0) {
            history.addView(text("No withdrawals yet.", 13, false, UiColors.textTertiary()));
            return;
        }
        for (int i = 0; i < arr.length(); i++) {
            JSONObject w = arr.optJSONObject(i);
            if (w == null) continue;
            boolean paid = "PAID".equals(w.optString("status"));
            String to = w.optString("to");
            TextView row = text(HusdClient.format(w.optLong("amountRaw")) + " hUSD → " + w.optString("chain") + " "
                    + (to.length() > 12 ? to.substring(0, 6) + "…" + to.substring(to.length() - 4) : to)
                    + "\n" + (paid ? "Paid in USDC" : "On its way: the bridge pays it after a few confirmations"),
                    13, false, paid ? UiColors.textPrimary() : UiColors.warning());
            row.setPadding(0, dp(6), 0, dp(6));
            history.addView(row);
        }
    }

    // ── Deposit ───────────────────────────────────────────────────────────────

    private void deposit() {
        if (!active()) return;
        JSONObject factories = info.optJSONObject("depositFactories");
        List<String> chains = new ArrayList<>();
        if (factories != null) for (Iterator<String> it = factories.keys(); it.hasNext(); ) chains.add(it.next());
        if (chains.isEmpty()) { toast("Deposits aren't open yet."); return; }
        String[] names = chains.toArray(new String[0]);
        String[] labels = new String[names.length];
        for (int i = 0; i < names.length; i++) labels[i] = chainName(names[i]);
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this).setTitle("Send USDC from which network?")
                .setItems(labels, (d, which) -> showDepositAddress(names[which], factories.optJSONObject(names[which]))).show();
    }

    private void showDepositAddress(String chain, JSONObject f) {
        String addr;
        try { addr = HusdClient.depositAddress(f.optString("factory"), f.optString("initCodeHash"), address); }
        catch (Exception e) { toast("This network's deposit settings look wrong."); return; }
        BottomSheetDialog sheet = com.harvana.app.p2p.ThemedDialogs.sheet(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(20), dp(20), dp(24));
        root.setBackgroundColor(UiColors.surface());
        root.addView(text("Your USDC deposit address on " + chainName(chain), 17, true, UiColors.textPrimary()));
        root.addView(text("Send only USDC on " + chainName(chain) + " to this address. Other coins or networks can't be credited.",
                13, false, UiColors.warning()));
        ImageView qr = new ImageView(this);
        try { qr.setImageBitmap(com.harvana.app.HARVQRCodeGenerator.generateQRCodeWithLogo(this, addr, dp(220), R.drawable.ic_coin_husd)); }
        catch (Exception ignored) { }
        LinearLayout.LayoutParams qlp = new LinearLayout.LayoutParams(dp(220), dp(220));
        qlp.gravity = Gravity.CENTER_HORIZONTAL;
        qlp.topMargin = dp(12);
        root.addView(qr, qlp);
        TextView a = text(addr, 14, true, UiColors.textPrimary());
        a.setGravity(Gravity.CENTER);
        a.setTextIsSelectable(true);
        a.setPadding(0, dp(10), 0, dp(10));
        root.addView(a);
        MaterialButton copy = button("Copy address");
        copy.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("USDC deposit address", addr));
            toast("Copied");
        });
        root.addView(copy);
        MaterialButton sent = button("I've sent it");
        sent.setOnClickListener(v -> HusdClient.requestSweep(chain, address, new HusdClient.Cb() {
            @Override public void ok(JSONObject r) { runOnUiThread(() -> { sheet.dismiss();
                toast("Thanks. Your hUSD arrives after the network confirms the deposit, usually within minutes."); }); }
            @Override public void fail(String e) { runOnUiThread(() -> toast("Couldn't reach the network: " + e)); }
        }));
        root.addView(sent);
        root.addView(text("Minimum deposit 1 USDC. Each deposit address is yours alone and never changes.",
                12, false, UiColors.textTertiary()));
        sheet.setContentView(root);
        sheet.show();
    }

    // ── Send ──────────────────────────────────────────────────────────────────

    private void send() {
        if (!active()) return;
        LinearLayout box = form();
        EditText to = field(box, "Harvana address (HV0x…)", InputType.TYPE_CLASS_TEXT);
        EditText amount = field(box, "Amount in hUSD", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this).setTitle("Send hUSD").setView(box)
                .setPositiveButton("Review", (d, w) -> {
                    String dest = to.getText().toString().trim();
                    Long units = HusdClient.parse(amount.getText().toString());
                    if (!dest.matches("^HV0x[0-9a-f]{40}$")) { toast("That isn't a Harvana address."); return; }
                    if (dest.equals(address)) { toast("That's this wallet."); return; }
                    if (units == null) { toast("Enter an amount, up to 6 decimals."); return; }
                    confirm("Send hUSD", units, new String[][]{{"To", shortAddr(dest)}, {"Fee", feeText(false)}},
                            () -> submit(dest, units, null, "Sent"));
                }).setNegativeButton("Cancel", null).show();
    }

    // ── Withdraw ──────────────────────────────────────────────────────────────

    private void withdraw() {
        if (!active()) return;
        JSONArray chains = info.optJSONArray("chains");
        List<String> list = new ArrayList<>();
        if (chains != null) for (int i = 0; i < chains.length(); i++) list.add(chains.optString(i));
        if (list.isEmpty()) { toast("Withdrawals aren't open yet."); return; }
        String[] names = list.toArray(new String[0]);
        String[] labels = new String[names.length];
        for (int i = 0; i < names.length; i++) labels[i] = chainName(names[i]);
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this).setTitle("Receive USDC on which network?")
                .setItems(labels, (d, which) -> withdrawTo(names[which])).show();
    }

    private void withdrawTo(String chain) {
        LinearLayout box = form();
        EditText to = field(box, "Your " + chainName(chain) + " address (0x…)", InputType.TYPE_CLASS_TEXT);
        EditText amount = field(box, "Amount in hUSD (at least 5)", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this).setTitle("Withdraw to USDC").setView(box)
                .setPositiveButton("Review", (d, w) -> {
                    String dest = to.getText().toString().trim();
                    Long units = HusdClient.parse(amount.getText().toString());
                    if (!HusdClient.isEvmAddress(dest)) { toast("That isn't a " + chainName(chain) + " address."); return; }
                    if (units == null || units < 5 * HusdClient.UNIT) { toast("The minimum withdrawal is 5 hUSD."); return; }
                    String burn = info.optString("burnAddress");
                    if (burn.isEmpty()) { toast("Withdrawals aren't open yet."); return; }
                    confirm("Withdraw to USDC", units, new String[][]{{"To", chainName(chain) + " " + shortAddr(dest)},
                                    {"You receive", HusdClient.format(units) + " USDC"}, {"Fee", "0.50 hUSD, plus " + feeText(true)},
                                    {"Arrives", "After the bridge confirms it, usually within the hour"}},
                            () -> submit(burn, units, "HUSD_WITHDRAW:" + chain + ":" + dest, "Withdrawal sent"));
                }).setNegativeButton("Cancel", null).show();
    }

    // ── Shared ────────────────────────────────────────────────────────────────

    private void confirm(String title, long units, String[][] rows, Runnable go) {
        List<String[]> list = new ArrayList<>();
        for (String[] r : rows) list.add(r);
        com.harvana.app.group.PaymentConfirmSheet.reviewThenAuthorize(this, title, HusdClient.format(units) + " hUSD", list, go);
    }

    private void submit(String to, long units, String memo, String done) {
        toast("Sending…");
        HusdClient.send(this, address, to, units, memo, new HusdClient.Cb() {
            @Override public void ok(JSONObject r) { runOnUiThread(() -> { toast(done + "."); load(); }); }
            @Override public void fail(String e) { runOnUiThread(() -> toast(plain(e))); }
        });
    }

    private String feeText(boolean withdrawal) {
        int free = bal.optInt("freeTransfersLeft");
        String base = "HARV gas if you hold HARV, otherwise 0.02 hUSD";
        return !withdrawal && free > 0 ? "Free (" + free + " free transfers left) if paid in hUSD; " + base : base;
    }

    private static String plain(String e) {
        if (e == null) return "Couldn't send.";
        if (e.contains("INSUFFICIENT_HUSD")) return "Not enough hUSD for this amount and its fee.";
        if (e.contains("HUSD_NOT_ACTIVE")) return "hUSD is not switched on yet.";
        if (e.contains("HUSD_WITHDRAW_BELOW_MINIMUM")) return "The minimum withdrawal is 5 hUSD.";
        if (e.contains("HUSD_SYSTEM_RECIPIENT")) return "hUSD can't be sent to that address.";
        return "Couldn't send: " + e;
    }

    private boolean active() {
        if (info.optBoolean("active")) return true;
        toast("hUSD is not switched on yet on the network.");
        return false;
    }

    static String chainName(String c) {
        switch (c) {
            case "BASE": return "Base";
            case "POLYGON": return "Polygon";
            case "ARBITRUM": return "Arbitrum";
            case "ETHEREUM": return "Ethereum";
            case "BSC": return "BNB Chain";
            default: return c;
        }
    }

    private static String shortAddr(String a) { return a.length() > 14 ? a.substring(0, 8) + "…" + a.substring(a.length() - 4) : a; }

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

    private LinearLayout form() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(8), dp(20), 0);
        return box;
    }

    private EditText field(LinearLayout box, String hint, int type) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setInputType(type);
        e.setSingleLine(true);
        box.addView(e);
        return e;
    }

    private View action(String label, View.OnClickListener l) {
        MaterialButton b = button(label);
        b.setOnClickListener(l);
        return b;
    }

    private MaterialButton button(String label) {
        MaterialButton b = new MaterialButton(this);
        b.setText(label);
        b.setAllCaps(false);
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
