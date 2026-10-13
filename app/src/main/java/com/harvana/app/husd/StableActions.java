package com.harvana.app.husd;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.harvana.app.utils.InfoTable;
import com.harvana.app.utils.UiColors;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A stable coin's own actions on its coin page (CoinDetailActivity): deposit the reserve token, withdraw back to it,
 * send, swap, the hNGN vault, and the fees and limits. hUSD is backed by USDC, hEUR by EURC, and so on.
 */
public final class StableActions {

    private final AppCompatActivity a;
    public final String code, sym;
    private String reserve;
    private final String address;
    private final Runnable onChange;
    private JSONObject info = new JSONObject();
    private JSONObject bal = new JSONObject();

    public StableActions(AppCompatActivity a, String code, String address, Runnable onChange) {
        this.a = a;
        this.code = code;
        this.sym = Stable.symbol(code);
        this.reserve = Stable.reserve(code, null);
        this.address = address;
        this.onChange = onChange;
    }

    public JSONObject info() { return info; }
    public JSONObject balance() { return bal; }
    public String reserve() { return reserve; }

    /** Reads the coin's settings and this wallet's balance; {@code then} runs on the main thread after each answer. */
    public void load(Runnable then) {
        HusdClient.info(code, new HusdClient.Cb() {
            @Override public void ok(JSONObject r) { a.runOnUiThread(() -> { info = r; reserve = Stable.reserve(code, r); if (then != null) then.run(); }); }
            @Override public void fail(String e) { }
        });
        if (address == null || address.isEmpty()) return;
        HusdClient.balance(code, address, new HusdClient.Cb() {
            @Override public void ok(JSONObject r) { a.runOnUiThread(() -> { bal = r; if (then != null) then.run(); }); }
            @Override public void fail(String e) { }
        });
    }

    // ── actions ───────────────────────────────────────────────────────────────

    public void send() {
        if (!active()) return;
        a.startActivity(new Intent(a, com.harvana.app.SendAssetActivity.class).putExtra(com.harvana.app.wallet.CoinCatalog.EXTRA, code));
    }

    public void swap() {
        a.startActivity(new Intent(a, com.harvana.app.swap.SwapActivity.class)
                .putExtra(com.harvana.app.swap.SwapActivity.EXTRA_FROM, "HARV").putExtra(com.harvana.app.swap.SwapActivity.EXTRA_TO, code));
    }

    public void vault() {
        a.startActivity(new Intent(a, VaultActivity.class).putExtra(HusdActivity.EXTRA_ASSET, code));
    }

    /** True when at least one chain takes deposits of the reserve token. */
    public boolean depositsOpen() { return info != null && !chains().isEmpty(); }

    public void deposit() {
        if (!active()) return;
        List<String> chains = chains();
        if (chains.isEmpty()) { toast("Deposits aren't open yet."); return; }
        String[] names = chains.toArray(new String[0]);
        String[] labels = new String[names.length];
        for (int i = 0; i < names.length; i++) labels[i] = HusdActivity.chainName(names[i]);
        JSONObject factories = info.optJSONObject("depositFactories");
        new com.harvana.app.ui.SheetDialog.Builder(a).setTitle("Deposit " + reserve + " from")
                .setItems(labels, (d, which) -> showDepositAddress(names[which], factories == null ? null : factories.optJSONObject(names[which]))).show();
    }

    private void showDepositAddress(String chain, JSONObject f) {
        if (f == null) return;
        String addr;
        try { addr = HusdClient.depositAddress(f.optString("factory"), f.optString("initCodeHash"), address); }
        catch (Exception e) { toast("This network's deposit settings look wrong."); return; }
        BottomSheetDialog sheet = com.harvana.app.p2p.ThemedDialogs.sheet(a);
        LinearLayout root = new LinearLayout(a);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(20), dp(20), dp(24));
        root.setBackgroundColor(UiColors.surface());
        root.addView(text(reserve + " deposit address · " + HusdActivity.chainName(chain), 17, true, UiColors.textPrimary()));
        ImageView qr = new ImageView(a);
        try { qr.setImageBitmap(com.harvana.app.HARVQRCodeGenerator.unified(addr, dp(220), StableIcon.bitmap(a, code, 128))); }
        catch (Exception ignored) { }
        LinearLayout.LayoutParams qlp = new LinearLayout.LayoutParams(dp(220), dp(220));
        qlp.gravity = Gravity.CENTER_HORIZONTAL;
        qlp.topMargin = dp(12);
        root.addView(qr, qlp);
        TextView at = text(addr, 14, true, UiColors.textPrimary());
        at.setGravity(Gravity.CENTER);
        at.setTextIsSelectable(true);
        at.setPadding(0, dp(10), 0, dp(10));
        root.addView(at);
        InfoTable t = new InfoTable(a);
        t.row("Send only", reserve + " on " + HusdActivity.chainName(chain), UiColors.warning())
                .row("You get", sym)
                .row("tFare", HusdClient.format(bridgeFee(chain)) + " " + sym)
                .row("Minimum deposit", "1 " + reserve);
        root.addView(t.view());
        MaterialButton copy = button("Copy address");
        copy.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) a.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) cm.setPrimaryClip(ClipData.newPlainText(reserve + " deposit address", addr));
            toast("Copied");
        });
        root.addView(copy);
        MaterialButton sent = button("I've sent it");
        sent.setOnClickListener(v -> HusdClient.requestSweep(code, chain, address, new HusdClient.Cb() {
            @Override public void ok(JSONObject r) { a.runOnUiThread(() -> { sheet.dismiss(); toast("Deposit requested"); }); }
            @Override public void fail(String e) { a.runOnUiThread(() -> toast(com.harvana.app.utils.ErrorText.plain(e))); }
        }));
        root.addView(sent);
        sheet.setContentView(root);
        sheet.show();
    }

    public void withdraw() {
        if (!active()) return;
        List<String> list = chains();
        if (list.isEmpty()) { toast("Withdrawals aren't open yet."); return; }
        String[] names = list.toArray(new String[0]);
        String[] labels = new String[names.length];
        for (int i = 0; i < names.length; i++) labels[i] = HusdActivity.chainName(names[i]);
        new com.harvana.app.ui.SheetDialog.Builder(a).setTitle("Withdraw " + reserve + " to")
                .setItems(labels, (d, which) -> withdrawTo(names[which])).show();
    }

    private void withdrawTo(String chain) {
        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(8), dp(20), 0);
        EditText to = field(box, HusdActivity.chainName(chain) + " address (0x…)", InputType.TYPE_CLASS_TEXT);
        long min = fee("minWithdrawRaw", 5 * HusdClient.UNIT);
        EditText amount = field(box, "Amount in " + sym + " (min " + HusdClient.format(min) + ")", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        new com.harvana.app.ui.SheetDialog.Builder(a).setTitle("Withdraw to " + reserve).setView(box)
                .setPositiveButton("Review", (d, w) -> {
                    String dest = to.getText().toString().trim();
                    Long units = HusdClient.parse(amount.getText().toString());
                    if (!HusdClient.isEvmAddress(dest)) { toast("That isn't a " + HusdActivity.chainName(chain) + " address."); return; }
                    if (units == null || units < min) { toast("The minimum withdrawal is " + HusdClient.format(min) + " " + sym + "."); return; }
                    String burn = info.optString("burnAddress");
                    if (burn.isEmpty()) { toast("Withdrawals aren't open yet."); return; }
                    List<String[]> rows = new ArrayList<>();
                    rows.add(new String[]{"To", HusdActivity.chainName(chain) + " " + shortAddr(dest)});
                    rows.add(new String[]{"You receive", HusdClient.format(units) + " " + reserve});
                    rows.add(new String[]{"Bridge tFare", HusdClient.format(bridgeFee(chain)) + " " + sym});
                    rows.add(new String[]{"tFare", feeText()});
                    com.harvana.app.group.PaymentConfirmSheet.reviewThenAuthorize(a, "Withdraw to " + reserve, HusdClient.format(units) + " " + sym, rows,
                            () -> submit(burn, units, code + "_WITHDRAW:" + chain + ":" + dest));
                }).setNegativeButton("Cancel", null).show();
    }

    private void submit(String to, long units, String memo) {
        toast("Sending…");
        HusdClient.send(a, code, address, to, units, memo, new HusdClient.Cb() {
            @Override public void ok(JSONObject r) { a.runOnUiThread(() -> { toast("Withdrawal sent."); if (onChange != null) onChange.run(); }); }
            @Override public void fail(String e) { a.runOnUiThread(() -> toast(plain(e))); }
        });
    }

    // ── figures ───────────────────────────────────────────────────────────────

    /** The coin's fees and limits, one figure per row. */
    public void fillFees(InfoTable t) {
        t.clear()
                .section("Sending on Harvana")
                .row("tFare (with HARV)", "HARV")
                .row("tFare (no HARV)", HusdClient.format(fee("transferPaidInHusdRaw", 20_000L)) + " " + sym);
        if (bal.optInt("freeTransfersLeft", 0) > 0) t.row("Free transfers left", String.valueOf(bal.optInt("freeTransfersLeft")));
        t.section("Deposits and withdrawals (" + reserve + ")");
        for (String chain : chains()) t.row(HusdActivity.chainName(chain), HusdClient.format(bridgeFee(chain)) + " " + sym);
        t.row("Minimum withdrawal", HusdClient.format(fee("minWithdrawRaw", 5 * HusdClient.UNIT)) + " " + sym)
                .row("Value", "1 " + sym + " = 1 " + Stable.name(code))
                .row("Backed by", reserve)
                .row("Status", info.optBoolean("active") ? "On" : "Not switched on");
    }

    private List<String> chains() {
        JSONObject factories = info.optJSONObject("depositFactories");
        List<String> out = new ArrayList<>();
        if (factories != null) for (Iterator<String> it = factories.keys(); it.hasNext(); ) out.add(it.next());
        return out;
    }

    private String feeText() {
        return "HARV, or " + HusdClient.format(fee("transferPaidInHusdRaw", 20_000L)) + " " + sym + " with no HARV";
    }

    private long bridgeFee(String chain) {
        JSONObject f = info.optJSONObject("fees");
        JSONObject per = f == null ? null : f.optJSONObject("bridgeFeeRaw");
        if (per != null && per.has(chain)) return per.optLong(chain);
        return fee("withdrawFeeRaw", 500_000L);
    }

    private long fee(String key, long fallback) {
        JSONObject f = info.optJSONObject("fees");
        return f != null && f.has(key) ? f.optLong(key, fallback) : fallback;
    }

    private String plain(String e) {
        if (e == null) return "Couldn't send.";
        if (com.harvana.app.utils.ErrorText.is(e, "INSUFFICIENT_" + code)) return "Not enough " + sym + " for this amount and its tFare.";
        if (e.contains(code + "_NOT_ACTIVE")) return sym + " is not switched on yet.";
        if (e.contains(code + "_WITHDRAW_BELOW_MINIMUM")) return "That is below the minimum withdrawal.";
        return com.harvana.app.utils.ErrorText.plain(e);
    }

    private boolean active() {
        if (info.optBoolean("active")) return true;
        toast(sym + " is not switched on yet.");
        return false;
    }

    private static String shortAddr(String s) { return s.length() > 14 ? s.substring(0, 8) + "…" + s.substring(s.length() - 4) : s; }

    private EditText field(LinearLayout box, String hint, int type) {
        EditText e = new EditText(a);
        e.setHint(hint);
        e.setInputType(type);
        e.setSingleLine(true);
        box.addView(e, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return e;
    }

    private MaterialButton button(String label) {
        MaterialButton b = new MaterialButton(a);
        b.setText(label);
        b.setAllCaps(false);
        return b;
    }

    private TextView text(String s, float sp, boolean bold, int color) {
        TextView t = new TextView(a);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(null, android.graphics.Typeface.BOLD);
        return t;
    }

    private void toast(String s) { Toast.makeText(a, s, Toast.LENGTH_LONG).show(); }
    private int dp(int v) { return Math.round(v * a.getResources().getDisplayMetrics().density); }
}
