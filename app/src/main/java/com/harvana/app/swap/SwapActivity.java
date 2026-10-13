package com.harvana.app.swap;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

        AppBar.setContent(this, R.layout.activity_swap, "Swap", "HARV and the Harvana coins");
        etAmount = findViewById(R.id.et_swap_amount);
        ivFrom = findViewById(R.id.iv_swap_from);
        tvFrom = findViewById(R.id.tv_swap_from);
        ivTo = findViewById(R.id.iv_swap_to);
        tvTo = findViewById(R.id.tv_swap_to);
        findViewById(R.id.swap_chip_from).setOnClickListener(v -> pick(true));
        findViewById(R.id.swap_chip_to).setOnClickListener(v -> pick(false));
        tvFromBal = findViewById(R.id.tv_swap_balance);
        findViewById(R.id.tv_swap_max).setOnClickListener(v -> { if (fromBalance > 0) etAmount.setText(plain(from, fromBalance)); });
        findViewById(R.id.iv_swap_flip).setOnClickListener(v -> { String f = from; from = to; to = f; sidesChanged(); });
        tvOut = findViewById(R.id.tv_swap_out);
        tvDetails = findViewById(R.id.tv_swap_problem);          // only for a problem (empty pool, …)
        details = new com.harvana.app.utils.InfoTable(this);
        ((LinearLayout) findViewById(R.id.swap_details)).addView(details.view());
        details.view().setVisibility(View.GONE);
        btnSwap = findViewById(R.id.btn_swap);
        btnSwap.setOnClickListener(v -> review());
        swapsList = findViewById(R.id.swap_orders);
        poolList = findViewById(R.id.swap_pools);

        etAmount.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b2, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b2, int c) { }
            @Override public void afterTextChanged(Editable s) { ui.removeCallbacks(requote); ui.postDelayed(requote, 350); }
        });

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
                poolList.addView(line(com.harvana.app.utils.ErrorText.plain(e)));
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
                swapsList.addView(line(com.harvana.app.utils.ErrorText.plain(e)));
            }); }
        });
    }

    private void showSwaps(JSONArray arr) {
        swapsList.removeAllViews();
        if (arr == null || arr.length() == 0) { swapsList.addView(line("No swaps yet.")); return; }
        for (int i = 0; i < Math.min(arr.length(), 10); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            View row = getLayoutInflater().inflate(R.layout.item_swap_order, swapsList, false);
            ((TextView) row.findViewById(R.id.tv_swap_order_name)).setText(SwapOrderActivity.title(o));
            ((TextView) row.findViewById(R.id.tv_swap_order_when)).setText(android.text.format.DateUtils.getRelativeTimeSpanString(o.optLong("at")));
            String st = o.optString("status");
            TextView badge = row.findViewById(R.id.tv_swap_order_badge);
            badge.setText(SwapOrderActivity.statusText(st));
            badge.setTextColor(SwapOrderActivity.statusColor(st));
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
                        .row("tFare", fmt("HARV", fare) + " HARV")
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
        rows.add(new String[]{"tFare", fmt("HARV", quote.optLong("transportFareRaw")) + " HARV"});
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
            poolList.addView(line("No coin is switched on yet."));
            return;
        }
        for (int i = 0; i < pools.length(); i++) {
            JSONObject p = pools.optJSONObject(i);
            if (p == null) continue;
            String code = p.optString("asset");
            View row = getLayoutInflater().inflate(R.layout.item_swap_pool, poolList, false);
            ((ImageView) row.findViewById(R.id.iv_swap_pool_coin)).setImageDrawable(icon(code));
            boolean open = p.optBoolean("open");
            ((TextView) row.findViewById(R.id.tv_swap_pool_name)).setText("HARV / " + sym(code));
            ((TextView) row.findViewById(R.id.tv_swap_pool_amounts)).setText(open
                    ? fmt(HARV, p.optLong("harvRaw")) + " HARV + " + fmt(code, p.optLong("coinRaw")) + " " + sym(code)
                    : "Empty");
            JSONObject my = myPosition(code);
            MaterialButton add = row.findViewById(R.id.btn_swap_pool_add);
            add.setText(open ? "Add" : "List");
            add.setOnClickListener(v -> com.harvana.app.web3.Web3Home.open(this,
                    com.harvana.app.web3.Web3Home.SWAP_URL + (open ? "#liquidity" : "#list")));
            if (my != null) {
                TextView mineTv = row.findViewById(R.id.tv_swap_pool_mine);
                mineTv.setText("Yours: " + pct(my.optLong("shareBps")) + " = " + fmt(HARV, my.optLong("harvRaw")) + " HARV + "
                        + fmt(code, my.optLong("coinRaw")) + " " + sym(code));
                mineTv.setVisibility(View.VISIBLE);
                View remove = row.findViewById(R.id.btn_swap_pool_remove);
                remove.setVisibility(View.VISIBLE);
                remove.setOnClickListener(v -> com.harvana.app.web3.Web3Home.open(this, com.harvana.app.web3.Web3Home.SWAP_URL + "#liquidity"));
            }
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
        new com.harvana.app.ui.SheetDialog.Builder(this).setTitle(isFrom ? "You pay" : "You get")
                .setItems(labels, (d, which) -> {
                    String c = list.get(which);
                    if (isFrom) { if (c.equals(to)) to = from; from = c; }
                    else { if (c.equals(from)) from = to; to = c; }
                    sidesChanged();
                }).show();
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





    /** A short line in a card (empty list, error): res/layout/item_list_empty.xml. */
    private TextView line(String s) {
        TextView t = (TextView) getLayoutInflater().inflate(R.layout.item_list_empty, null, false);
        t.setText(s);
        t.setGravity(android.view.Gravity.START);
        t.setPadding(0, dp(10), 0, dp(10));
        t.setTextSize(13);
        return t;
    }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }
    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
