package com.harvana.app.swap;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.harvana.app.R;
import com.harvana.app.husd.HusdClient;
import com.harvana.app.husd.Stable;
import com.harvana.app.husd.StableIcon;
import com.harvana.app.sync.HarvanaNodeClient;
import com.harvana.app.sync.NodeGroupClient;
import com.harvana.app.utils.AppBar;
import com.harvana.app.wallet.BalanceFormatter;
import com.harvana.app.wallet.WalletManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Swap on the Harvana chain: HARV ↔ any Harvana coin, and coin ↔ coin through HARV, priced by the node's pools
 * (HarvSwap). Only swapping lives here; pools are managed from Web3 → Liquidity.
 *
 * <p>Every figure comes from the node: balances, the quote (out, pool fee, Harvana's fee, price impact, tFare), and
 * the order record. The minimum received is sent with the swap and the node returns the coins if the pool would pay
 * less, so the slippage setting is enforced, not cosmetic. Open with extras "from" / "to" to pick the pair.
 */
public class SwapActivity extends AppCompatActivity {

    public static final String EXTRA_FROM = "from", EXTRA_TO = "to";
    private static final String HARV = SwapMath.HARV;
    private static final long REQUOTE_MS = 15_000L;
    private static final long[] SLIPPAGE_PRESETS = {10, 50, 100, 300};
    private static final String PREFS = "swap", KEY_SLIPPAGE = "slippageBps";

    private String address, from = HARV, to = "HNGN";
    private final List<String> assets = new ArrayList<>();
    private JSONArray pools = new JSONArray();
    private final Map<String, Long> balances = new HashMap<>();
    private JSONObject quote;
    private long quotedIn = -1, quoteAt, fareRaw = -1, slippageBps = 100;
    private String quoteError = "";
    private boolean submitting, detailsOpen, showAll;
    private JSONArray swaps = new JSONArray();
    private String ordersError = "";

    private EditText etAmount;
    private View chipFrom, chipTo;
    private TextView tvBalance, tvOut, tvRate, tvProblem, tvSlippage, tvDetailsToggle, tvViewAll;
    private LinearLayout details, ordersBox;
    private MaterialButton btnSwap;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final Runnable requote = this::requote;
    private final Runnable refresh = new Runnable() {
        @Override public void run() {
            if (!submitting && SwapMath.toRaw(from, text()) > 0) requote();
            ui.postDelayed(this, REQUOTE_MS);
        }
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        address = WalletManager.getActiveWalletAddress(this);
        loadFx();
        if (getIntent() != null) {
            String f = getIntent().getStringExtra(EXTRA_FROM), t = getIntent().getStringExtra(EXTRA_TO);
            if (f != null && !f.isEmpty()) from = f.toUpperCase(java.util.Locale.US);
            if (t != null && !t.isEmpty()) to = t.toUpperCase(java.util.Locale.US);
            if (from.equals(to)) to = HARV.equals(from) ? "HNGN" : HARV;
        }
        assets.add(HARV);
        slippageBps = prefs().getLong(KEY_SLIPPAGE, 100);

        AppBar.setContent(this, R.layout.activity_swap, "Swap", "Trade assets securely and seamlessly");
        clampWidth();
        etAmount = findViewById(R.id.et_swap_amount);
        chipFrom = findViewById(R.id.swap_chip_from);
        chipTo = findViewById(R.id.swap_chip_to);
        tvBalance = findViewById(R.id.tv_swap_balance);
        tvOut = findViewById(R.id.tv_swap_out);
        tvRate = findViewById(R.id.tv_swap_rate);
        tvProblem = findViewById(R.id.tv_swap_problem);
        tvSlippage = findViewById(R.id.tv_swap_slippage);
        tvDetailsToggle = findViewById(R.id.tv_swap_details_toggle);
        tvViewAll = findViewById(R.id.tv_swap_view_all);
        details = findViewById(R.id.swap_details);
        ordersBox = findViewById(R.id.swap_orders);
        btnSwap = findViewById(R.id.btn_swap);

        chipFrom.setOnClickListener(v -> pick(true));
        chipTo.setOnClickListener(v -> pick(false));
        findViewById(R.id.tv_swap_max).setOnClickListener(v -> max());
        findViewById(R.id.tv_swap_half).setOnClickListener(v -> half());
        findViewById(R.id.iv_swap_flip).setOnClickListener(v -> flip());
        findViewById(R.id.swap_rate_row).setOnClickListener(v -> { detailsOpen = !detailsOpen; showQuote(); });
        findViewById(R.id.swap_slippage_row).setOnClickListener(v -> pickSlippage());
        tvViewAll.setOnClickListener(v -> { showAll = !showAll; showSwaps(); });
        btnSwap.setOnClickListener(v -> review());
        etAmount.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b2, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b2, int c) { }
            @Override public void afterTextChanged(Editable s) {
                quote = null;
                quoteError = "";
                showQuote();
                ui.removeCallbacks(requote);
                ui.postDelayed(requote, 350);
            }
        });
        showSlippage();
        sidesChanged();
        com.harvana.app.wallet.UsdValue.load(this, () -> { if (!isFinishing()) { showPrices(); showQuote(); } });
    }

    @Override protected void onResume() {
        super.onResume();
        loadPools();
        loadBalances();
        loadSwaps();
        ui.removeCallbacks(refresh);
        ui.postDelayed(refresh, REQUOTE_MS);
    }

    @Override protected void onPause() { ui.removeCallbacks(refresh); ui.removeCallbacks(requote); super.onPause(); }

    private SharedPreferences prefs() { return getSharedPreferences(PREFS, Context.MODE_PRIVATE); }

    /** Tablets and wide windows: a centred column of at most 560dp instead of a stretched form. */
    private void clampWidth() {
        View content = findViewById(R.id.swap_content);
        float density = getResources().getDisplayMetrics().density;
        int widthDp = (int) (getResources().getDisplayMetrics().widthPixels / density);
        if (widthDp > 600 && content != null) {
            ViewGroup.LayoutParams lp = content.getLayoutParams();
            lp.width = (int) (560 * density);
            content.setLayoutParams(lp);
        }
    }

    private String text() { return etAmount.getText() == null ? "" : etAmount.getText().toString(); }

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
                requote();
            }); }
            @Override public void onError(String e) { runOnUiThread(() -> { quoteError = SwapText.problem(e); showQuote(); }); }
        });
    }

    /** Every asset's balance, for the form and the selector. A balance not yet read stays unknown, never 0. */
    private void loadBalances() {
        if (address == null || address.isEmpty()) { updateButton(); return; }
        HarvanaNodeClient.getWallet(address, new HarvanaNodeClient.JsonCallback() {
            @Override public void onSuccess(JSONObject j) { runOnUiThread(() -> { balances.put(HARV, j.optLong("balance", 0L)); showBalance(); }); }
            @Override public void onError(String e) { }
        });
        for (String code : Stable.CODES) {
            HusdClient.balance(code, address, new HusdClient.Cb() {
                @Override public void ok(JSONObject r) { runOnUiThread(() -> { balances.put(code, r.optLong("availableRaw")); showBalance(); }); }
                @Override public void fail(String e) { }
            });
        }
    }

    private long balance(String code) { Long b = balances.get(code); return b == null ? -1 : b; }

    private void showBalance() {
        long bal = balance(from);
        tvBalance.setText(address == null || address.isEmpty() ? "No wallet"
                : bal < 0 ? "Available: …" : "Available: " + SwapText.amount(from, bal));
        updateButton();
    }

    /** The wallet's swaps (pool deposits and withdrawals are not swaps), newest first. */
    private void loadSwaps() {
        if (address == null || address.isEmpty()) { showSwaps(); return; }
        NodeGroupClient.swapOrders(address, new NodeGroupClient.Callback() {
            @Override public void onSuccess(JSONObject r) { runOnUiThread(() -> {
                JSONArray all = r.optJSONArray("orders"), only = new JSONArray();
                for (int i = 0; all != null && i < all.length(); i++) {
                    JSONObject o = all.optJSONObject(i);
                    if (o != null && "SWAP".equals(o.optString("kind"))) only.put(o);
                }
                swaps = only;
                ordersError = "";
                showSwaps();
            }); }
            @Override public void onError(String e) { runOnUiThread(() -> { ordersError = SwapText.problem(e); showSwaps(); }); }
        });
    }

    private void showSwaps() {
        ordersBox.removeAllViews();
        if (!ordersError.isEmpty()) { ordersBox.addView(SwapText.line(this, ordersError)); return; }
        if (swaps.length() == 0) {
            ordersBox.addView(SwapText.line(this, "No swaps yet\nYour completed and pending swaps will appear here."));
            tvViewAll.setVisibility(View.GONE);
            return;
        }
        int n = showAll ? swaps.length() : Math.min(5, swaps.length());
        for (int i = 0; i < n; i++) {
            JSONObject o = swaps.optJSONObject(i);
            View row = getLayoutInflater().inflate(R.layout.item_swap_recent, ordersBox, false);
            String f = o.optString("from"), t = o.optString("to"), st = o.optString("status");
            ((ImageView) row.findViewById(R.id.iv_swap_recent_from)).setImageDrawable(icon(f));
            ((ImageView) row.findViewById(R.id.iv_swap_recent_to)).setImageDrawable(icon(t));
            ((TextView) row.findViewById(R.id.tv_swap_recent_pair)).setText(SwapText.sym(f) + " » " + SwapText.sym(t));
            ((TextView) row.findViewById(R.id.tv_swap_recent_paid)).setText(SwapText.amount(f, o.optLong("amountInRaw")));
            ((TextView) row.findViewById(R.id.tv_swap_recent_got)).setText("DONE".equals(st) && o.has("outRaw")
                    ? SwapText.amount(t, o.optLong("outRaw"))
                    : "REFUNDED".equals(st) ? "Returned" : "≈ " + SwapText.amount(t, o.optLong("quotedOutRaw")));
            ((TextView) row.findViewById(R.id.tv_swap_recent_when)).setText(
                    android.text.format.DateFormat.format("d MMM yyyy · HH:mm", o.optLong("at")));
            TextView badge = row.findViewById(R.id.tv_swap_recent_status);
            badge.setText(SwapMath.statusWord(st));
            badge.setTextColor(tone(SwapMath.statusTone(st)));
            row.setContentDescription(SwapText.sym(f) + " to " + SwapText.sym(t) + ", " + SwapMath.statusWord(st) + ". View details.");
            String id = o.optString("orderId");
            row.setOnClickListener(v -> startActivity(new android.content.Intent(this, SwapOrderActivity.class)
                    .putExtra(SwapOrderActivity.EXTRA_ID, id)));
            ordersBox.addView(row);
        }
        tvViewAll.setVisibility(swaps.length() > 5 ? View.VISIBLE : View.GONE);
        tvViewAll.setText(showAll ? "Show less" : "View all »");
    }

    int tone(int t) {
        return ContextCompat.getColor(this, t > 0 ? R.color.swapSuccess : t < 0 ? R.color.swapError : R.color.swapWarning);
    }

    // ── form ──────────────────────────────────────────────────────────────────

    private void sidesChanged() {
        bindToken(chipFrom, from);
        bindToken(chipTo, to);
        showPrices();
        quote = null;
        quoteError = "";
        fareRaw = -1;
        showBalance();
        requote();
    }

    private void bindToken(View chip, String code) {
        ((ImageView) chip.findViewById(R.id.iv_swap_token)).setImageDrawable(icon(code));
        ((TextView) chip.findViewById(R.id.tv_swap_token_symbol)).setText(SwapText.sym(code));
        ((TextView) chip.findViewById(R.id.tv_swap_token_name)).setText(SwapText.name(code));
        chip.setContentDescription("Selected asset " + SwapText.sym(code) + ". Tap to change.");
    }

    /** Reverses the pair: both coins, the balance, the quote and the checks follow. The typed amount stays. */
    private void flip() {
        String f = from;
        from = to;
        to = f;
        sidesChanged();
    }

    /** Half of what can be spent (after the tFare on a HARV swap). */
    private void half() {
        long bal = balance(from);
        if (bal <= 0) return;
        long spend = SwapMath.maxSpendable(from, bal, Math.max(0, fareRaw));
        etAmount.setText(SwapMath.plain(from, spend / 2));
    }

    /** The two price cards: each coin of the pair in dollars, with the 24-hour change for HARV when the node sent one. */
    private void showPrices() {
        priceCard(findViewById(R.id.swap_price_from), from);
        priceCard(findViewById(R.id.swap_price_to), to);
    }

    private void priceCard(View card, String code) {
        if (card == null) return;
        ((ImageView) card.findViewById(R.id.iv_price_icon)).setImageDrawable(icon(code));
        ((TextView) card.findViewById(R.id.tv_price_symbol)).setText(SwapText.sym(code));
        double usd = com.harvana.app.wallet.UsdValue.per(this, code);
        ((TextView) card.findViewById(R.id.tv_price_usd)).setText(Double.isNaN(usd) ? "—" : usdPrice(usd));
        TextView ch = card.findViewById(R.id.tv_price_change);
        double pct = Double.NaN;
        if (HARV.equals(code)) {
            com.harvana.app.wallet.HarvPrice.Quote q = com.harvana.app.wallet.HarvPrice.peek();
            if (q != null) pct = q.changePct;
        }
        if (Double.isNaN(pct)) {
            ch.setText("24h —");
            ch.setTextColor(ContextCompat.getColor(this, R.color.swapTextSecondary));
        } else {
            ch.setText(String.format(java.util.Locale.US, "24h %s%.2f%%", pct >= 0 ? "+" : "", pct));
            ch.setTextColor(tone(pct >= 0 ? 1 : -1));
        }
    }

    /** A price with enough digits to see small coins: $1.24, $0.0123, $0.000652. */
    static String usdPrice(double v) {
        if (v >= 1) return String.format(java.util.Locale.US, "$%,.2f", v);
        if (v <= 0) return "$0";
        int digits = Math.min(10, 2 + (int) Math.ceil(-Math.log10(v)));
        return "$" + new java.math.BigDecimal(v).setScale(digits, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    /** "≈ $12.40" under an amount, or "" when the coin's price is not known. */
    private String usdUnder(String code, long raw) {
        if (raw <= 0) return "";
        double amount = raw / Math.pow(10, SwapMath.decimals(code));
        String t = com.harvana.app.wallet.UsdValue.text(this, code, amount);
        return t.isEmpty() ? "" : "≈ " + t;
    }

    private void max() {
        long bal = balance(from);
        if (bal <= 0) return;
        if (HARV.equals(from) && fareRaw < 0) {
            // The tFare comes with any quote; ask for one first so MAX leaves room for it.
            NodeGroupClient.swapQuote(from, to, bal, new NodeGroupClient.Callback() {
                @Override public void onSuccess(JSONObject q) { runOnUiThread(() -> {
                    fareRaw = q.optLong("transportFareRaw", -1);
                    if (fareRaw >= 0) etAmount.setText(SwapMath.plain(from, SwapMath.maxSpendable(from, bal, fareRaw)));
                }); }
                @Override public void onError(String e) { runOnUiThread(() -> { quoteError = SwapText.problem(e); updateButton(); }); }
            });
            return;
        }
        etAmount.setText(SwapMath.plain(from, SwapMath.maxSpendable(from, bal, fareRaw)));
    }

    private boolean pairOpen() { return open(from) && open(to); }

    private boolean open(String code) {
        if (HARV.equals(code)) return true;
        JSONObject p = pool(code);
        return p != null && p.optBoolean("open");
    }

    private JSONObject pool(String code) {
        for (int i = 0; i < pools.length(); i++) {
            JSONObject p = pools.optJSONObject(i);
            if (p != null && code.equals(p.optString("asset"))) return p;
        }
        return null;
    }

    private void requote() {
        long in = SwapMath.toRaw(from, text());
        if (in <= 0 || !pairOpen() || from.equals(to)) {
            quote = null;
            showQuote();
            return;
        }
        final String f = from, t = to;
        NodeGroupClient.swapQuote(f, t, in, new NodeGroupClient.Callback() {
            @Override public void onSuccess(JSONObject q) { runOnUiThread(() -> {
                if (!f.equals(from) || !t.equals(to) || in != SwapMath.toRaw(from, text())) return;   // an older answer
                quote = q;
                quotedIn = in;
                quoteAt = System.currentTimeMillis();
                quoteError = "";
                fareRaw = q.optLong("transportFareRaw", fareRaw);
                showQuote();
            }); }
            @Override public void onError(String e) { runOnUiThread(() -> {
                if (!f.equals(from) || !t.equals(to)) return;
                quote = null;
                quoteError = SwapText.problem(e);
                showQuote();
            }); }
        });
    }

    /** Output, rate line and the details panel from the current quote (or the pools' spot rate before one). */
    private void showQuote() {
        long out = quote == null ? -1 : quote.optLong("outRaw");
        tvOut.setText(out >= 0 ? SwapText.amount(to, out).replace(" " + SwapText.sym(to), "")
                : SwapMath.toRaw(from, text()) > 0 && quoteError.isEmpty() ? "…" : "0.00");
        ((TextView) findViewById(R.id.tv_swap_in_usd)).setText(usdUnder(from, Math.max(0, SwapMath.toRaw(from, text()))));
        ((TextView) findViewById(R.id.tv_swap_out_usd)).setText(usdUnder(to, Math.max(0, out)));
        String rate = quote != null && quotedIn > 0 ? SwapText.rate(from, to, quotedIn, out) : SwapText.spotRate(from, to, pools);
        tvRate.setText(rate.isEmpty() ? (pairOpen() ? "Rate after you enter an amount" : "No liquidity for this pair") : rate);
        tvDetailsToggle.setText(detailsOpen ? "Hide" : "Details");
        findViewById(R.id.iv_swap_details_chevron).setRotation(detailsOpen ? 180 : 0);
        details.removeAllViews();
        if (detailsOpen && quote != null) {
            for (String[] r : detailRows()) addDetail(details, r[0], r[1], r.length > 2 ? Integer.parseInt(r[2]) : 0);
        }
        details.setVisibility(detailsOpen && quote != null ? View.VISIBLE : View.GONE);
        updateButton();
    }

    /** The quote as label/value rows ([2] = tone: 1 good, -1 warning). Shared with the review sheet. */
    private List<String[]> detailRows() {
        List<String[]> rows = new ArrayList<>();
        if (quote == null) return rows;
        long out = quote.optLong("outRaw"), impact = quote.optLong("priceImpactBps");
        rows.add(new String[]{"Minimum received", SwapText.amount(to, SwapMath.minOut(out, slippageBps))});
        rows.add(new String[]{"Network fee (tFare)", SwapText.amount(HARV, quote.optLong("transportFareRaw"))});
        long harvanaBps = quote.optLong("harvanaFeeBps", 0);
        long poolBps = quote.optLong("feeBps") - harvanaBps;
        rows.add(new String[]{"Pool fee" + (quote.has("viaHarvRaw") ? " (2 pools)" : ""), SwapText.pct(poolBps)});
        if (harvanaBps > 0) rows.add(new String[]{"Harvana fee", SwapText.pct(harvanaBps)});
        if (quote.optLong("spreadBps", 0) > 0) rows.add(new String[]{"Spread", SwapText.pct(quote.optLong("spreadBps"))});
        rows.add(new String[]{"Price impact", SwapText.pct(impact), impact >= 500 ? "-1" : "0"});
        rows.add(new String[]{"Route", quote.has("viaHarvRaw")
                ? SwapText.sym(from) + " » HARV » " + SwapText.sym(to) : SwapText.sym(from) + " » " + SwapText.sym(to)});
        // Non-naira pools are priced at the real exchange rate (node: GET /swap/fx); its source and time.
        if (fx != null && fx.optBoolean("anchored") && fx.optLong("at") > 0 && !(isNaira(from) && isNaira(to))) {
            rows.add(new String[]{"Exchange rates", fx.optString("source") + " · "
                    + new java.text.SimpleDateFormat("d MMM, HH:mm", java.util.Locale.US).format(new java.util.Date(fx.optLong("at")))});
        }
        return rows;
    }

    /** The node's exchange rates and where they came from; loaded once when the screen opens. */
    private org.json.JSONObject fx;

    private static boolean isNaira(String code) { return "HARV".equals(code) || "HNGN".equals(code); }

    private void loadFx() {
        com.harvana.app.sync.HarvanaNodeClient.nodeGet("/api/v1/swap/fx", new com.harvana.app.sync.HarvanaNodeClient.JsonCallback() {
            @Override public void onSuccess(org.json.JSONObject j) { fx = j; }
            @Override public void onError(String e) { }
        });
    }

    private void addDetail(LinearLayout box, String label, String value, int tone) {
        View row = getLayoutInflater().inflate(R.layout.item_swap_detail, box, false);
        ((TextView) row.findViewById(R.id.tv_swap_detail_label)).setText(label);
        TextView v = row.findViewById(R.id.tv_swap_detail_value);
        v.setText(value);
        if (tone < 0) v.setTextColor(ContextCompat.getColor(this, R.color.swapWarning));
        box.addView(row);
    }

    private void updateButton() {
        SwapMath.Button b = SwapMath.button(address != null && !address.isEmpty(), submitting, text(), from, to, pairOpen(),
                balance(from), balance(HARV), fareRaw, quote == null ? -1 : quote.optLong("outRaw"), quoteError);
        btnSwap.setText(b.label);
        btnSwap.setEnabled(b.enabled);
        tvProblem.setText(b.problem);
        tvProblem.setVisibility(b.problem.isEmpty() ? View.GONE : View.VISIBLE);
    }

    // ── slippage ──────────────────────────────────────────────────────────────

    private void showSlippage() { tvSlippage.setText(SwapText.pct(slippageBps)); }

    private void pickSlippage() {
        String[] labels = new String[SLIPPAGE_PRESETS.length];
        for (int i = 0; i < labels.length; i++) labels[i] = SwapText.pct(SLIPPAGE_PRESETS[i]) + (SLIPPAGE_PRESETS[i] == slippageBps ? "  ✓" : "");
        new com.harvana.app.ui.SheetDialog.Builder(this).setTitle("Slippage tolerance")
                .setItems(labels, (d, which) -> {
                    slippageBps = SLIPPAGE_PRESETS[which];
                    prefs().edit().putLong(KEY_SLIPPAGE, slippageBps).apply();
                    showSlippage();
                    showQuote();
                }).show();
    }

    // ── assets ────────────────────────────────────────────────────────────────

    private void pick(boolean isFrom) {
        SwapAssetSheet.show(this, isFrom ? "You pay" : "You receive", new ArrayList<>(assets), pools, balances,
                isFrom ? from : to, code -> {
                    if (isFrom) { if (code.equals(to)) to = from; from = code; }
                    else { if (code.equals(from)) from = to; to = code; }
                    sidesChanged();
                });
    }

    android.graphics.drawable.Drawable icon(String code) {
        return HARV.equals(code) ? ContextCompat.getDrawable(this, R.drawable.ic_harv_coin) : StableIcon.drawable(this, code);
    }

    // ── review and submit ─────────────────────────────────────────────────────

    /** Fresh quote first (an old one may no longer hold), then the review sheet with exactly what will be sent. */
    private void review() {
        long in = SwapMath.toRaw(from, text());
        if (in <= 0 || submitting) return;
        btnSwap.setEnabled(false);
        btnSwap.setText("Checking price…");
        final String f = from, t = to;
        NodeGroupClient.swapQuote(f, t, in, new NodeGroupClient.Callback() {
            @Override public void onSuccess(JSONObject q) { runOnUiThread(() -> {
                if (isFinishing() || !f.equals(from) || !t.equals(to) || in != SwapMath.toRaw(from, text())) { updateButton(); return; }
                quote = q;
                quotedIn = in;
                quoteAt = System.currentTimeMillis();
                fareRaw = q.optLong("transportFareRaw", fareRaw);
                showQuote();
                if (!btnSwap.isEnabled()) return;           // the fresh quote changed the checks (balance, too small)
                confirm(in, q);
            }); }
            @Override public void onError(String e) { runOnUiThread(() -> { quoteError = SwapText.problem(e); showQuote(); }); }
        });
    }

    private void confirm(long in, JSONObject q) {
        long out = q.optLong("outRaw"), min = SwapMath.minOut(out, slippageBps);
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"You receive (estimated)", SwapText.amount(to, out)});
        rows.add(new String[]{"Rate", SwapText.rate(from, to, in, out)});
        for (String[] r : detailRows()) rows.add(new String[]{r[0], r[1]});
        rows.add(new String[]{"Slippage tolerance", SwapText.pct(slippageBps)});
        rows.add(new String[]{"Network", "Harvana chain"});
        rows.add(new String[]{"Wallet", SwapText.shortAddr(address)});
        String f = from, t = to;
        com.harvana.app.group.PaymentConfirmSheet.reviewThenAuthorize(this, "Swap " + SwapText.sym(f) + " for " + SwapText.sym(t),
                SwapText.amount(f, in), rows, () -> submit(f, t, in, out, min), this::updateButton);
    }

    private void submit(String f, String t, long in, long out, long min) {
        if (submitting) return;
        submitting = true;
        updateButton();
        SwapStatusSheet sheet = SwapStatusSheet.show(this, f, t, in, out, () -> {
            loadBalances();
            loadSwaps();
        });
        NodeGroupClient.swap(this, address, f, t, in, min, new NodeGroupClient.Callback() {
            @Override public void onSuccess(JSONObject r) { runOnUiThread(() -> {
                submitting = false;
                etAmount.setText("");
                sheet.submitted(r.optString("orderId"), r.optString("txHash"));
                loadBalances();
                loadSwaps();
            }); }
            @Override public void onError(String e) { runOnUiThread(() -> {
                submitting = false;
                updateButton();
                sheet.failed(SwapText.problem(e), () -> review());
            }); }
        });
    }
}
