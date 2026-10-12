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
 * One stable asset's screen: hUSD (dollars, backed by USDC) or hEUR, hNGN and the others (each backed by a token of
 * its own currency). Deposit the reserve token from the supported chains without needing gas, send to anyone on
 * Harvana, and withdraw back to the reserve token. Open with extra "asset" = "HEUR", …; hUSD when absent.
 */
public class HusdActivity extends AppCompatActivity {

    public static final String EXTRA_ASSET = "asset";

    private String address;
    /** "HUSD", "HEUR", …; sym "hUSD"; reserve "USDC", "EURC", … */
    private String code = HusdClient.ASSET, sym = "hUSD", reserve = "USDC";
    private JSONObject info = new JSONObject();
    private JSONObject bal = new JSONObject();
    private TextView tvBalance, tvSub, tvHow;
    private LinearLayout history;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        address = WalletManager.getActiveWalletAddress(this);
        String asked = getIntent() == null ? null : getIntent().getStringExtra(EXTRA_ASSET);
        if (Stable.isStable(asked)) code = asked;
        sym = Stable.symbol(code);
        reserve = Stable.reserve(code, null);
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(UiColors.background());
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(24));
        sv.addView(root);

        LinearLayout head = card();
        head.setGravity(Gravity.CENTER_HORIZONTAL);
        ImageView logo = new ImageView(this);
        logo.setImageDrawable(StableIcon.drawable(this, code));
        head.addView(logo, new LinearLayout.LayoutParams(dp(56), dp(56)));
        tvBalance = text("… " + sym, 30, true, UiColors.textPrimary());
        tvBalance.setGravity(Gravity.CENTER);
        com.harvana.app.utils.BalancePrivacy.guard(tvBalance);
        head.addView(tvBalance);
        tvSub = text("1 " + sym + " = 1 " + Stable.name(code) + ", backed by " + reserve, 13, false, UiColors.textSecondary());
        tvSub.setGravity(Gravity.CENTER);
        head.addView(tvSub);
        LinearLayout actions = new LinearLayout(this);
        actions.setPadding(0, dp(14), 0, 0);
        actions.addView(action("Deposit", v -> deposit()), weight());
        actions.addView(action("Send", v -> send()), weight());
        actions.addView(action("Withdraw", v -> withdraw()), weight());
        head.addView(actions);
        root.addView(head);

        LinearLayout sc = card();                          // swap HARV or another coin for this one (SwapActivity)
        sc.addView(text("Swap for " + sym, 15, true, UiColors.textPrimary()));
        sc.addView(text("Swap HARV or another Harvana coin for " + sym + " at the pool's price, or swap " + sym + " back.",
                13, false, UiColors.textSecondary()));
        sc.addView(action("Swap", v -> startActivity(new android.content.Intent(this, com.harvana.app.swap.SwapActivity.class)
                .putExtra(com.harvana.app.swap.SwapActivity.EXTRA_FROM, "HARV").putExtra(com.harvana.app.swap.SwapActivity.EXTRA_TO, code))));
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.topMargin = dp(12);
        root.addView(sc, slp);

        if ("NGN".equals(Stable.currency(code))) {         // hNGN can also be minted against HARV (VaultActivity)
            LinearLayout vc = card();
            vc.addView(text("Mint " + sym + " with HARV", 15, true, UiColors.textPrimary()));
            vc.addView(text("No naira to deposit? Lock HARV and mint " + sym + " against it. Repay to unlock your HARV.",
                    13, false, UiColors.textSecondary()));
            vc.addView(action("Open my vault", v -> startActivity(new android.content.Intent(this, VaultActivity.class)
                    .putExtra(EXTRA_ASSET, code))));
            LinearLayout.LayoutParams vlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            vlp.topMargin = dp(12);
            root.addView(vc, vlp);
        }

        TextView how = text("How it works", 15, true, UiColors.textPrimary());
        how.setPadding(dp(4), dp(18), 0, dp(6));
        root.addView(how);
        LinearLayout hc = card();
        tvHow = text("…", 13, false, UiColors.textSecondary());
        hc.addView(tvHow);
        root.addView(hc);

        TextView wtitle = text("Withdrawals", 15, true, UiColors.textPrimary());
        wtitle.setPadding(dp(4), dp(18), 0, dp(6));
        root.addView(wtitle);
        history = card();
        root.addView(history);

        AppBar.setContent(this, sv, sym, Stable.name(code) + " inside Harvana");
        load();
    }

    @Override protected void onResume() { super.onResume(); load(); }

    private void load() {
        if (address == null || address.isEmpty()) { tvBalance.setText("No wallet"); return; }
        HusdClient.info(code, new HusdClient.Cb() {
            @Override public void ok(JSONObject r) { info = r; runOnUiThread(() -> {
                reserve = Stable.reserve(code, r);
                tvHow.setText(howText());
                if (!r.optBoolean("active")) tvSub.setText(sym + " is not switched on yet on the network.");
            }); }
            @Override public void fail(String e) { }
        });
        HusdClient.balance(code, address, new HusdClient.Cb() {
            @Override public void ok(JSONObject r) { bal = r; runOnUiThread(() -> {
                tvBalance.setText(HusdClient.format(r.optLong("availableRaw")) + " " + sym);
                long pin = r.optLong("pendingInRaw"), pout = r.optLong("pendingOutRaw");
                String extra = pin > 0 ? " · " + HusdClient.format(pin) + " arriving" : (pout > 0 ? " · " + HusdClient.format(pout) + " leaving" : "");
                tvSub.setText(r.optInt("freeTransfersLeft") + " free transfers left" + extra);
            }); }
            @Override public void fail(String e) { runOnUiThread(() -> tvSub.setText("Couldn't load: " + e)); }
        });
        HusdClient.withdrawals(code, address, new HusdClient.Cb() {
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
            TextView row = text(HusdClient.format(w.optLong("amountRaw")) + " " + sym + " → " + chainName(w.optString("chain")) + " "
                    + (to.length() > 12 ? to.substring(0, 6) + "…" + to.substring(to.length() - 4) : to)
                    + "\n" + (paid ? "Paid in " + reserve : "On its way: the bridge pays it after a few confirmations"),
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
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this).setTitle("Send " + reserve + " from which network?")
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
        root.addView(text("Your " + reserve + " deposit address on " + chainName(chain), 17, true, UiColors.textPrimary()));
        root.addView(text("Send only " + reserve + " on " + chainName(chain) + " to this address, for " + sym
                + ". Other coins, other networks, and other Harvana assets' deposit addresses can't be mixed up: each has its own.",
                13, false, UiColors.warning()));
        ImageView qr = new ImageView(this);
        try { qr.setImageBitmap(com.harvana.app.HARVQRCodeGenerator.unified(addr, dp(220), StableIcon.bitmap(this, code, 128))); }
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
            if (cm != null) cm.setPrimaryClip(ClipData.newPlainText(reserve + " deposit address", addr));
            toast("Copied");
        });
        root.addView(copy);
        MaterialButton sent = button("I've sent it");
        sent.setOnClickListener(v -> HusdClient.requestSweep(code, chain, address, new HusdClient.Cb() {
            @Override public void ok(JSONObject r) { runOnUiThread(() -> { sheet.dismiss();
                toast("Thanks. Your " + sym + " arrives after the network confirms the deposit, usually within minutes."); }); }
            @Override public void fail(String e) { runOnUiThread(() -> toast("Couldn't reach the network: " + e)); }
        }));
        root.addView(sent);
        long depFee = bridgeFee(chain);
        root.addView(text("Network fee on " + chainName(chain) + ": " + HusdClient.format(depFee) + " " + sym
                + ", taken from the deposit (twice that network's gas, for forwarding it into the vault). Send more than that.",
                12, false, UiColors.textSecondary()));
        root.addView(text("Minimum deposit 1 " + reserve + ". Each deposit address is yours alone and never changes.",
                12, false, UiColors.textTertiary()));
        sheet.setContentView(root);
        sheet.show();
    }

    // ── Send ──────────────────────────────────────────────────────────────────

    /** Sending uses the app's one Send screen, with this coin already picked; underneath, that screen sends it through
     *  the stable-asset engine (SendAssetActivity._sendStable). */
    private void send() {
        if (!active()) return;
        startActivity(new android.content.Intent(this, com.harvana.app.SendAssetActivity.class)
                .putExtra(com.harvana.app.wallet.CoinCatalog.EXTRA, code));
    }

    // ── Withdraw ──────────────────────────────────────────────────────────────

    private void withdraw() {
        if (!active()) return;
        // Only chains where this asset has a vault (the same ones deposits use).
        JSONObject factories = info.optJSONObject("depositFactories");
        List<String> list = new ArrayList<>();
        if (factories != null) for (Iterator<String> it = factories.keys(); it.hasNext(); ) list.add(it.next());
        if (list.isEmpty()) { toast("Withdrawals aren't open yet."); return; }
        String[] names = list.toArray(new String[0]);
        String[] labels = new String[names.length];
        for (int i = 0; i < names.length; i++) labels[i] = chainName(names[i]);
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this).setTitle("Receive " + reserve + " on which network?")
                .setItems(labels, (d, which) -> withdrawTo(names[which])).show();
    }

    private void withdrawTo(String chain) {
        LinearLayout box = form();
        EditText to = field(box, "Your " + chainName(chain) + " address (0x…)", InputType.TYPE_CLASS_TEXT);
        long min = fee("minWithdrawRaw", 5 * HusdClient.UNIT);
        EditText amount = field(box, "Amount in " + sym + " (at least " + HusdClient.format(min) + ")", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this).setTitle("Withdraw to " + reserve).setView(box)
                .setPositiveButton("Review", (d, w) -> {
                    String dest = to.getText().toString().trim();
                    Long units = HusdClient.parse(amount.getText().toString());
                    if (!HusdClient.isEvmAddress(dest)) { toast("That isn't a " + chainName(chain) + " address."); return; }
                    if (units == null || units < min) { toast("The minimum withdrawal is " + HusdClient.format(min) + " " + sym + "."); return; }
                    String burn = info.optString("burnAddress");
                    if (burn.isEmpty()) { toast("Withdrawals aren't open yet."); return; }
                    confirm("Withdraw to " + reserve, units, new String[][]{{"To", chainName(chain) + " " + shortAddr(dest)},
                                    {"You receive", HusdClient.format(units) + " " + reserve},
                                    {"Fee", HusdClient.format(bridgeFee(chain)) + " " + sym + " for " + chainName(chain) + "'s gas, plus " + feeText(true)},
                                    {"Arrives", "After the bridge confirms it, usually within the hour"}},
                            () -> submit(burn, units, code + "_WITHDRAW:" + chain + ":" + dest, "Withdrawal sent"));
                }).setNegativeButton("Cancel", null).show();
    }

    // ── Shared ────────────────────────────────────────────────────────────────

    private void confirm(String title, long units, String[][] rows, Runnable go) {
        List<String[]> list = new ArrayList<>();
        for (String[] r : rows) list.add(r);
        com.harvana.app.group.PaymentConfirmSheet.reviewThenAuthorize(this, title, HusdClient.format(units) + " " + sym, list, go);
    }

    private void submit(String to, long units, String memo, String done) {
        toast("Sending…");
        HusdClient.send(this, code, address, to, units, memo, new HusdClient.Cb() {
            @Override public void ok(JSONObject r) { runOnUiThread(() -> { toast(done + "."); load(); }); }
            @Override public void fail(String e) { runOnUiThread(() -> toast(plain(e))); }
        });
    }

    private String feeText(boolean withdrawal) {
        int free = bal.optInt("freeTransfersLeft");
        String base = "HARV gas if you hold HARV, otherwise " + HusdClient.format(fee("transferPaidInHusdRaw", 20_000L)) + " " + sym;
        return !withdrawal && free > 0 ? "Free (" + free + " free transfers left) if paid in " + sym + "; " + base : base;
    }

    /** The bridge fee on a chain (raw units of this coin): the node's per-chain figure, or its single older figure. */
    private long bridgeFee(String chain) {
        JSONObject f = info.optJSONObject("fees");
        JSONObject per = f == null ? null : f.optJSONObject("bridgeFeeRaw");
        if (per != null && per.has(chain)) return per.optLong(chain);
        return fee("withdrawFeeRaw", 500_000L);
    }

    /** One of the node's fee figures for this asset (raw units), or {@code fallback} before /info has loaded. */
    private long fee(String key, long fallback) {
        JSONObject f = info.optJSONObject("fees");
        return f != null && f.has(key) ? f.optLong(key, fallback) : fallback;
    }

    private String howText() {
        return "• Deposit " + reserve + " from a supported network to your own " + sym + " deposit address. You don't need any gas.\n"
                + "• Send " + sym + " to anyone on Harvana. If you hold HARV, the fee is paid in HARV; if not, it is "
                + HusdClient.format(fee("transferPaidInHusdRaw", 20_000L)) + " " + sym + ". Your first 3 " + sym + "-paid transfers are free.\n"
                + "• Deposits and withdrawals pay twice the other network's gas, in " + sym + ": "
                + HusdClient.format(bridgeFee("BASE")) + " on Base and similar networks, " + HusdClient.format(bridgeFee("ETHEREUM")) + " on Ethereum.\n"
                + "• If you hold no HARV, half of a transfer's fee pays the miner who adds it to the chain.\n"
                + "• Withdraw " + sym + " back to " + reserve + " at any address you choose. The minimum is " + HusdClient.format(fee("minWithdrawRaw", 5 * HusdClient.UNIT)) + " " + sym + ".";
    }

    private String plain(String e) {
        if (e == null) return "Couldn't send.";
        if (e.contains("INSUFFICIENT_" + code)) return "Not enough " + sym + " for this amount and its fee.";
        if (e.contains(code + "_NOT_ACTIVE")) return sym + " is not switched on yet.";
        if (e.contains(code + "_WITHDRAW_BELOW_MINIMUM")) return "That is below the minimum withdrawal.";
        if (e.contains("_SYSTEM_RECIPIENT") || e.contains("_ONLY")) return sym + " can't be sent to that address.";
        return "Couldn't send: " + e;
    }

    private boolean active() {
        if (info.optBoolean("active")) return true;
        toast(sym + " is not switched on yet on the network.");
        return false;
    }

    public static String chainName(String c) {
        switch (c) {
            case "BASE": return "Base";
            case "POLYGON": return "Polygon";
            case "ARBITRUM": return "Arbitrum";
            case "ETHEREUM": return "Ethereum";
            case "BSC": return "BNB Chain";
            case "CELO": return "Celo";
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
