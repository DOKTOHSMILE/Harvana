package com.harvana.app.swap;

import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
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

import com.google.android.material.button.MaterialButton;
import com.harvana.app.R;
import com.harvana.app.husd.HusdClient;
import com.harvana.app.husd.Stable;
import com.harvana.app.husd.StableIcon;
import com.harvana.app.sync.HarvanaNodeClient;
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
 * Swap on the Harvana chain: HARV ↔ any stable coin (hNGN, hUSD, …) and coin ↔ coin through HARV, from the network's
 * pools (node HarvSwap). Below the swap, the pools themselves: anyone holding HARV and a coin can add both and earn the
 * 0.3% every swap through that pool pays, and take their share back at any time.
 * Open with extra "from" / "to" (asset codes, "HARV" for HARV) to pick the pair.
 */
public class SwapActivity extends AppCompatActivity {

    public static final String EXTRA_FROM = "from", EXTRA_TO = "to";
    private static final String HARV = "HARV";
    private static final long SLIPPAGE_BPS = 100;     // 1%: the swap comes back whole if the price moves more than that

    private String address, from = HARV, to = "HNGN";
    private final List<String> assets = new ArrayList<>();
    private JSONArray pools = new JSONArray();
    private JSONObject quote;
    private long fromBalance = -1;

    private ImageView ivFrom, ivTo;
    private TextView tvFrom, tvTo, tvFromBal, tvOut, tvDetails;
    private com.harvana.app.utils.InfoTable details;
    private LinearLayout swapsList;
    private EditText etAmount;
    private MaterialButton btnSwap;
    private LinearLayout poolList;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final Runnable requote = this::requote;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        address = WalletManager.getActiveWalletAddress(this);
        if (getIntent() != null) {
            String f = getIntent().getStringExtra(EXTRA_FROM), t = getIntent().getStringExtra(EXTRA_TO);
            if (f != null && !f.isEmpty()) from = f.toUpperCase(java.util.Locale.US);
            if (t != null && !t.isEmpty()) to = t.toUpperCase(java.util.Locale.US);
            if (from.equals(to)) to = HARV.equals(from) ? "HNGN" : HARV;
        }
        assets.add(HARV);

        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(UiColors.background());
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(24));
        sv.addView(root);

        // You pay
        LinearLayout pay = card();
        pay.addView(text("You pay", 13, false, UiColors.textSecondary()));
        LinearLayout payRow = new LinearLayout(this);
        payRow.setGravity(Gravity.CENTER_VERTICAL);
        etAmount = new EditText(this);
        etAmount.setHint("0");
        etAmount.setTextSize(26);
        etAmount.setTypeface(null, Typeface.BOLD);
        etAmount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etAmount.setSingleLine(true);
        etAmount.setBackground(null);
        payRow.addView(etAmount, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        LinearLayout chipFrom = chip(true);
        payRow.addView(chipFrom);
        pay.addView(payRow);
        LinearLayout balRow = new LinearLayout(this);
        tvFromBal = text("Balance: …", 12, false, UiColors.textTertiary());
        balRow.addView(tvFromBal, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView max = text("Max", 13, true, UiColors.accent());
        max.setOnClickListener(v -> { if (fromBalance > 0) etAmount.setText(plain(from, fromBalance)); });
        balRow.addView(max);
        pay.addView(balRow);
        root.addView(pay);

        // flip
        TextView flip = text("⇅", 22, true, UiColors.accent());
        flip.setGravity(Gravity.CENTER);
        flip.setPadding(0, dp(4), 0, dp(4));
        flip.setContentDescription("Swap the two sides");
        flip.setOnClickListener(v -> { String f = from; from = to; to = f; sidesChanged(); });
        root.addView(flip);

        // You get
        LinearLayout get = card();
        get.addView(text("You get (estimate)", 13, false, UiColors.textSecondary()));
        LinearLayout getRow = new LinearLayout(this);
        getRow.setGravity(Gravity.CENTER_VERTICAL);
        tvOut = text("0", 26, true, UiColors.textPrimary());
        getRow.addView(tvOut, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        getRow.addView(chip(false));
        get.addView(getRow);
        root.addView(get);

        tvDetails = text("", 12, false, UiColors.warning());          // only for a problem (empty pool, …)
        tvDetails.setPadding(dp(6), dp(10), dp(6), dp(4));
        root.addView(tvDetails);
        details = new com.harvana.app.utils.InfoTable(this);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dlp.topMargin = dp(4);
        root.addView(details.view(), dlp);
        details.view().setVisibility(View.GONE);

        btnSwap = new MaterialButton(this);
        btnSwap.setText("Swap");
        btnSwap.setAllCaps(false);
        btnSwap.setEnabled(false);
        btnSwap.setOnClickListener(v -> review());
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52));
        blp.topMargin = dp(8);
        root.addView(btnSwap, blp);

        TextView yt = text("Your swaps", 15, true, UiColors.textPrimary());
        yt.setPadding(dp(4), dp(22), 0, dp(6));
        root.addView(yt);
        swapsList = card();
        root.addView(swapsList);

        TextView pt = text("Pools", 15, true, UiColors.textPrimary());
        pt.setPadding(dp(4), dp(22), 0, dp(6));
        root.addView(pt);
        poolList = card();
        root.addView(poolList);

        etAmount.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b2, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b2, int c) { }
            @Override public void afterTextChanged(Editable s) { ui.removeCallbacks(requote); ui.postDelayed(requote, 350); }
        });

        AppBar.setContent(this, sv, "Swap", "HARV and the Harvana coins");
        sidesChanged();
        loadPools();
    }

    @Override protected void onResume() { super.onResume(); loadPools(); loadBalance(); loadSwaps(); }

    // ── data ──────────────────────────────────────────────────────────────────

    private void loadPools() {
        NodeGroupClient.swapPools(new NodeGroupClient.Callback() {
            @Override public void onSuccess(JSONObject r) { runOnUiThread(() -> {
                pools = r.optJSONArray("pools") == null ? new JSONArray() : r.optJSONArray("pools");
                assets.clear();
                assets.add(HARV);
                for (int i = 0; i < pools.length(); i++) {
                    JSONObject p = pools.optJSONObject(i);
                    if (p != null) assets.add(p.optString("asset"));
                }
                showPools();
                requote();
            }); }
            @Override public void onError(String e) { runOnUiThread(() -> {
                poolList.removeAllViews();
                poolList.addView(text(com.harvana.app.utils.ErrorText.plain(e), 13, false, UiColors.textTertiary()));
            }); }
        });
        if (address == null || address.isEmpty()) return;
        NodeGroupClient.swapPosition(address, new NodeGroupClient.Callback() {
            @Override public void onSuccess(JSONObject r) { runOnUiThread(() -> { mine = r.optJSONArray("positions"); showPools(); }); }
            @Override public void onError(String e) { }
        });
    }

    private JSONArray mine = new JSONArray();

    /** The wallet's swaps and pool actions, newest first; a tap opens the full record (SwapOrderActivity). */
    private void loadSwaps() {
        if (address == null || address.isEmpty() || swapsList == null) return;
        NodeGroupClient.swapOrders(address, new NodeGroupClient.Callback() {
            @Override public void onSuccess(JSONObject r) { runOnUiThread(() -> showSwaps(r.optJSONArray("orders"))); }
            @Override public void onError(String e) { runOnUiThread(() -> {
                swapsList.removeAllViews();
                swapsList.addView(text(com.harvana.app.utils.ErrorText.plain(e), 13, false, UiColors.textTertiary()));
            }); }
        });
    }

    private void showSwaps(JSONArray arr) {
        swapsList.removeAllViews();
        if (arr == null || arr.length() == 0) { swapsList.addView(text("No swaps yet.", 13, false, UiColors.textTertiary())); return; }
        for (int i = 0; i < Math.min(arr.length(), 10); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(8), 0, dp(8));
            TextView left = text(SwapOrderActivity.title(o) + "\n" + android.text.format.DateUtils.getRelativeTimeSpanString(o.optLong("at")),
                    13, false, UiColors.textPrimary());
            row.addView(left, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            String st = o.optString("status");
            TextView badge = text(SwapOrderActivity.statusText(st), 12, true, SwapOrderActivity.statusColor(st));
            row.addView(badge);
            row.setOnClickListener(v -> startActivity(new android.content.Intent(this, SwapOrderActivity.class)
                    .putExtra(SwapOrderActivity.EXTRA_ID, o.optString("orderId"))));
            swapsList.addView(row);
        }
    }

    private void loadBalance() {
        fromBalance = -1;
        tvFromBal.setText("Balance: …");
        if (address == null || address.isEmpty()) { tvFromBal.setText("No wallet"); return; }
        final String side = from;
        if (HARV.equals(side)) {
            HarvanaNodeClient.getWallet(address, new HarvanaNodeClient.JsonCallback() {
                @Override public void onSuccess(JSONObject j) { runOnUiThread(() -> { if (side.equals(from)) showBalance(j.optLong("balance", 0L)); }); }
                @Override public void onError(String e) { }
            });
        } else {
            HusdClient.balance(side, address, new HusdClient.Cb() {
                @Override public void ok(JSONObject r) { runOnUiThread(() -> { if (side.equals(from)) showBalance(r.optLong("availableRaw")); }); }
                @Override public void fail(String e) { }
            });
        }
    }

    private void showBalance(long raw) {
        fromBalance = raw;
        tvFromBal.setText("Balance: " + fmt(from, raw) + " " + sym(from));
    }

    private void sidesChanged() {
        ivFrom.setImageDrawable(icon(from));
        tvFrom.setText(sym(from));
        ivTo.setImageDrawable(icon(to));
        tvTo.setText(sym(to));
        loadBalance();
        requote();
    }

    private void requote() {
        quote = null;
        btnSwap.setEnabled(false);
        long in = raw(from, etAmount.getText().toString());
        if (in <= 0) {
            tvOut.setText("0");
            tvDetails.setText("");
            String rate = rateLine();
            details.clear();
            if (!rate.isEmpty()) details.row("Rate", rate.replace("1 " + sym(from) + " ≈ ", "1 " + sym(from) + " = "));
            details.view().setVisibility(rate.isEmpty() ? View.GONE : View.VISIBLE);
            return;
        }
        final String f = from, t = to;
        tvOut.setText("…");
        NodeGroupClient.swapQuote(f, t, in, new NodeGroupClient.Callback() {
            @Override public void onSuccess(JSONObject q) { runOnUiThread(() -> {
                if (!f.equals(from) || !t.equals(to) || in != raw(from, etAmount.getText().toString())) return;   // an older answer
                quote = q;
                long out = q.optLong("outRaw");
                tvOut.setText(fmt(to, out));
                long impact = q.optLong("priceImpactBps");
                long fare = q.optLong("transportFareRaw");
                int moves = q.optInt("transportMovements", 2);
                tvDetails.setText("");
                details.clear()
                        .section("This swap")
                        .row("You pay", fmt(from, in) + " " + sym(from))
                        .row("You get (estimate)", fmt(to, out) + " " + sym(to))
                        .row("You get at least", fmt(to, minOut(out)) + " " + sym(to))
                        .row("Rate", rateLine().replace(" ≈ ", " = "))
                        .section("Costs")
                        .row("Pool fee", pct(q.optLong("feeBps")) + (q.has("viaHarvRaw") ? " (2 pools)" : ""))
                        .row("Price impact", pct(impact), impact >= 500 ? UiColors.warning() : UiColors.textPrimary())
                        .row("Transport fare", fmt("HARV", fare) + " HARV")
                        .row("Movements", String.valueOf(moves));
                details.view().setVisibility(View.VISIBLE);
                boolean enough = fromBalance < 0 || in <= fromBalance;
                btnSwap.setEnabled(out > 0 && enough);
                btnSwap.setText(enough ? "Swap" : "Not enough " + sym(from));
            }); }
            @Override public void onError(String e) { runOnUiThread(() -> {
                if (!f.equals(from) || !t.equals(to)) return;
                tvOut.setText("—");
                details.view().setVisibility(View.GONE);
                tvDetails.setText(problem(e));
            }); }
        });
    }

    /** "1 HARV ≈ 1,000 hNGN" from the pools (spot, before fee and impact). */
    private String rateLine() {
        double fromPerHarv = perHarv(from), toPerHarv = perHarv(to);
        if (fromPerHarv <= 0 || toPerHarv <= 0) return "";
        double rate = toPerHarv / fromPerHarv;       // units of `to` for one unit of `from`
        return "1 " + sym(from) + " ≈ " + BalanceFormatter.smartDecimal(rate) + " " + sym(to);
    }

    /** Units of this asset one HARV buys at the pool's spot price (1 for HARV, 0 when the pool is empty). */
    private double perHarv(String code) {
        if (HARV.equals(code)) return 1;
        JSONObject p = pool(code);
        return p == null || !p.optBoolean("open") ? 0 : p.optLong("coinRawPerHarv") / (double) HusdClient.UNIT;
    }

    private JSONObject pool(String code) {
        for (int i = 0; i < pools.length(); i++) {
            JSONObject p = pools.optJSONObject(i);
            if (p != null && code.equals(p.optString("asset"))) return p;
        }
        return null;
    }

    // ── swap ──────────────────────────────────────────────────────────────────

    private void review() {
        if (quote == null) return;
        long in = quote.optLong("amountInRaw"), out = quote.optLong("outRaw");
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"You get about", fmt(to, out) + " " + sym(to)});
        rows.add(new String[]{"At least", fmt(to, minOut(out)) + " " + sym(to)});
        rows.add(new String[]{"Price impact", pct(quote.optLong("priceImpactBps"))});
        rows.add(new String[]{"Pool fee", pct(quote.optLong("feeBps"))});
        rows.add(new String[]{"Transport fare", fmt("HARV", quote.optLong("transportFareRaw")) + " HARV · " + quote.optInt("transportMovements", 2) + " movements"});
        com.harvana.app.group.PaymentConfirmSheet.reviewThenAuthorize(this, "Swap " + sym(from) + " for " + sym(to),
                fmt(from, in) + " " + sym(from), rows, () -> {
                    btnSwap.setEnabled(false);
                    toast("Swapping…");
                    NodeGroupClient.swap(this, address, from, to, in, minOut(out), new NodeGroupClient.Callback() {
                        @Override public void onSuccess(JSONObject r) { runOnUiThread(() -> {
                            toast("Swap sent.");
                            etAmount.setText("");
                            loadBalance();
                            loadPools();
                            loadSwaps();
                        }); }
                        @Override public void onError(String e) { runOnUiThread(() -> { toast(problem(e)); btnSwap.setEnabled(true); }); }
                    });
                });
    }

    private static long minOut(long out) { return Math.max(1, out - out * SLIPPAGE_BPS / 10_000); }

    // ── pools ─────────────────────────────────────────────────────────────────

    private void showPools() {
        poolList.removeAllViews();
        if (pools.length() == 0) {
            poolList.addView(text("No coin is switched on yet.", 13, false, UiColors.textTertiary()));
            return;
        }
        for (int i = 0; i < pools.length(); i++) {
            JSONObject p = pools.optJSONObject(i);
            if (p == null) continue;
            String code = p.optString("asset");
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(8), 0, dp(8));
            ImageView ic = new ImageView(this);
            ic.setImageDrawable(icon(code));
            row.addView(ic, new LinearLayout.LayoutParams(dp(32), dp(32)));
            boolean open = p.optBoolean("open");
            String body = "HARV / " + sym(code) + "\n" + (open
                    ? fmt(HARV, p.optLong("harvRaw")) + " HARV + " + fmt(code, p.optLong("coinRaw")) + " " + sym(code)
                    : "Empty");
            JSONObject my = myPosition(code);
            if (my != null) body += "\nYours: " + pct(my.optLong("shareBps")) + " = " + fmt(HARV, my.optLong("harvRaw")) + " HARV + "
                    + fmt(code, my.optLong("coinRaw")) + " " + sym(code);
            TextView t = text(body, 13, false, UiColors.textPrimary());
            t.setPadding(dp(10), 0, dp(6), 0);
            row.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            LinearLayout btns = new LinearLayout(this);
            btns.setOrientation(LinearLayout.VERTICAL);
            btns.addView(small(open ? "Add" : "List", () -> com.harvana.app.web3.Web3Home.open(this,
                    com.harvana.app.web3.Web3Home.SWAP_URL + (open ? "#liquidity" : "#list"))));
            if (my != null) btns.addView(small("Remove", () -> com.harvana.app.web3.Web3Home.open(this, com.harvana.app.web3.Web3Home.SWAP_URL + "#liquidity")));
            row.addView(btns);
            poolList.addView(row);
        }
    }

    private JSONObject myPosition(String code) {
        if (mine == null) return null;
        for (int i = 0; i < mine.length(); i++) {
            JSONObject m = mine.optJSONObject(i);
            if (m != null && code.equals(m.optString("asset"))) return m;
        }
        return null;
    }

    // ── assets ────────────────────────────────────────────────────────────────

    private void pick(boolean isFrom) {
        List<String> list = new ArrayList<>(assets);
        String[] labels = new String[list.size()];
        for (int i = 0; i < labels.length; i++) labels[i] = sym(list.get(i)) + (HARV.equals(list.get(i)) ? "" : "  ·  " + Stable.name(list.get(i)));
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this).setTitle(isFrom ? "You pay" : "You get")
                .setItems(labels, (d, which) -> {
                    String c = list.get(which);
                    if (isFrom) { if (c.equals(to)) to = from; from = c; }
                    else { if (c.equals(from)) from = to; to = c; }
                    sidesChanged();
                }).show();
    }

    private LinearLayout chip(boolean isFrom) {
        LinearLayout c = new LinearLayout(this);
        c.setGravity(Gravity.CENTER_VERTICAL);
        c.setPadding(dp(10), dp(6), dp(12), dp(6));
        android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
        g.setCornerRadius(dp(20));
        g.setColor(UiColors.background());
        c.setBackground(g);
        ImageView iv = new ImageView(this);
        c.addView(iv, new LinearLayout.LayoutParams(dp(24), dp(24)));
        TextView tv = text("", 15, true, UiColors.textPrimary());
        tv.setPadding(dp(6), 0, dp(2), 0);
        c.addView(tv);
        c.addView(text("▾", 13, false, UiColors.textSecondary()));
        c.setOnClickListener(v -> pick(isFrom));
        if (isFrom) { ivFrom = iv; tvFrom = tv; } else { ivTo = iv; tvTo = tv; }
        return c;
    }

    private android.graphics.drawable.Drawable icon(String code) {
        return HARV.equals(code) ? androidx.core.content.ContextCompat.getDrawable(this, R.drawable.ic_harv_coin) : StableIcon.drawable(this, code);
    }

    private static String sym(String code) { return HARV.equals(code) ? com.harvana.app.CoinInfo.ticker() : Stable.symbol(code); }

    /** Typed text → raw units (HARV 8 decimals, coins 6). */
    private static long raw(String code, String text) {
        String s = text == null ? "" : text.trim().replace(",", "");
        if (s.isEmpty()) return 0;
        try {
            java.math.BigDecimal v = new java.math.BigDecimal(s);
            if (v.signum() <= 0) return 0;
            return v.movePointRight(HARV.equals(code) ? 8 : 6).setScale(0, java.math.RoundingMode.DOWN).longValueExact();
        } catch (Exception e) {
            return 0;
        }
    }

    private static String fmt(String code, long raw) {
        return HARV.equals(code) ? BalanceFormatter.smartDecimal(BalanceFormatter.rawToHarv(raw)) : HusdClient.format(raw);
    }

    /** Raw units → plain text for an input box (no grouping commas). */
    private static String plain(String code, long raw) {
        return java.math.BigDecimal.valueOf(raw, HARV.equals(code) ? 8 : 6).stripTrailingZeros().toPlainString();
    }

    private static String pct(long bps) { return BalanceFormatter.smartDecimal(bps / 100.0) + "%"; }

    private String problem(String e) {
        if (e == null) return "Couldn't swap.";
        if (com.harvana.app.utils.ErrorText.is(e, "NO_LIQUIDITY")) return "That pool is empty. Add HARV and the coin below to open it.";
        if (com.harvana.app.utils.ErrorText.is(e, "PRICE_MOVED")) return "The price moved. Check the new estimate and try again.";
        if (com.harvana.app.utils.ErrorText.is(e, "AMOUNT_TOO_SMALL")) return "That amount is too small to swap.";
        if (com.harvana.app.utils.ErrorText.is(e, "INSUFFICIENT")) return "Not enough balance for this and its fee.";
        if (com.harvana.app.utils.ErrorText.is(e, "_NOT_ACTIVE")) return "That coin isn't switched on yet.";
        if (com.harvana.app.utils.ErrorText.is(e, "MORE_THAN_YOUR_SHARES")) return "That is more than your share of the pool.";
        if (com.harvana.app.utils.ErrorText.is(e, "SAME_ASSET")) return "Pick two different assets.";
        return com.harvana.app.utils.ErrorText.plain(e);
    }

    // ── views ─────────────────────────────────────────────────────────────────

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(16), dp(14), dp(16), dp(14));
        android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
        g.setCornerRadius(dp(18));
        g.setColor(UiColors.surface());
        c.setBackground(g);
        return c;
    }

    private EditText field(LinearLayout box, String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        e.setSingleLine(true);
        box.addView(e);
        return e;
    }

    private MaterialButton small(String label, Runnable click) {
        MaterialButton b = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        b.setText(label);
        b.setAllCaps(false);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setOnClickListener(v -> click.run());
        return b;
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
