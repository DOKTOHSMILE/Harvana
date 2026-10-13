package com.harvana.app.swap;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.harvana.app.husd.HusdClient;
import com.harvana.app.husd.Stable;
import com.harvana.app.sync.NodeGroupClient;
import com.harvana.app.utils.AppBar;
import com.harvana.app.utils.InfoTable;
import com.harvana.app.utils.UiColors;
import com.harvana.app.wallet.BalanceFormatter;

import org.json.JSONObject;

/**
 * One swap, pool deposit or pool withdrawal, as the node recorded it (GET /api/v1/swap/order/{id}): status, what was
 * paid, quoted and received (or refunded), the transport fare, and every transaction, each opening in the explorer.
 * Refreshes itself while it is still waiting for blocks.
 */
public class SwapOrderActivity extends AppCompatActivity {

    public static final String EXTRA_ID = "orderId";
    private String id;
    private final android.os.Handler ui = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable poll = this::load;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        id = getIntent() == null ? null : getIntent().getStringExtra(EXTRA_ID);
        AppBar.setContent(this, com.harvana.app.R.layout.activity_swap_order, "Swap", "Transaction record");
        load();
    }

    @Override protected void onDestroy() { ui.removeCallbacks(poll); super.onDestroy(); }

    private void load() {
        if (id == null || id.isEmpty()) return;
        NodeGroupClient.swapOrder(id, new NodeGroupClient.Callback() {
            @Override public void onSuccess(JSONObject o) { runOnUiThread(() -> show(o)); }
            @Override public void onError(String e) { runOnUiThread(() -> {
                TextView err = findViewById(com.harvana.app.R.id.tv_swap_order_error);
                err.setText(com.harvana.app.utils.ErrorText.plain(e));
                err.setVisibility(android.view.View.VISIBLE);
            }); }
        });
    }

    private void show(JSONObject o) {
        if (isFinishing()) return;
        String kind = o.optString("kind"), status = o.optString("status");
        findViewById(com.harvana.app.R.id.tv_swap_order_error).setVisibility(android.view.View.GONE);
        findViewById(com.harvana.app.R.id.swap_order_content).setVisibility(android.view.View.VISIBLE);
        LinearLayout figures = findViewById(com.harvana.app.R.id.swap_order_figures), txBox = findViewById(com.harvana.app.R.id.swap_order_txs);
        figures.removeAllViews();
        txBox.removeAllViews();

        // Headline: the action and its status.
        ((TextView) findViewById(com.harvana.app.R.id.tv_swap_order_title)).setText(title(o));
        TextView st = findViewById(com.harvana.app.R.id.tv_swap_order_status);
        st.setText(statusText(status));
        st.setTextColor(statusColor(status));

        InfoTable t = new InfoTable(this);
        if ("SWAP".equals(kind)) {
            String from = o.optString("from"), to = o.optString("to");
            t.section("Amounts")
             .row("You paid", amount(from, o.optLong("amountInRaw")))
             .row("Quoted", amount(to, o.optLong("quotedOutRaw")))
             .row("Least you accepted", amount(to, o.optLong("minOutRaw")));
            if (o.has("outRaw")) {
                long out = o.optLong("outRaw"), in = o.optLong("amountInRaw");
                t.row("You received", amount(to, out), UiColors.success());
                double rate = units(to, out) / Math.max(1e-18, units(from, in));
                t.row("Rate", "1 " + sym(from) + " = " + BalanceFormatter.smartDecimal(rate) + " " + sym(to));
            }
            if ("REFUNDED".equals(status)) {
                t.row("Sent back to you", amount(from, o.optLong("amountInRaw")), UiColors.warning());
                t.row("Why", reason(o.optString("reason")));
            }
        } else if ("ADD".equals(kind)) {
            String code = o.optString("asset");
            t.section("Amounts")
             .row("HARV put in", amount("HARV", o.optLong("harvInRaw")))
             .row(sym(code) + " put in", amount(code, o.optLong("coinInRaw")));
            if (o.has("sharesMinted")) {
                t.row("Pool shares received", String.valueOf(o.optLong("sharesMinted")), UiColors.success())
                 .row("HARV used", amount("HARV", o.optLong("harvUsedRaw")))
                 .row(sym(code) + " used", amount(code, o.optLong("coinUsedRaw")));
                if (o.optLong("harvBackRaw") > 0) t.row("HARV sent back", amount("HARV", o.optLong("harvBackRaw")));
                if (o.optLong("coinBackRaw") > 0) t.row(sym(code) + " sent back", amount(code, o.optLong("coinBackRaw")));
            }
            if ("REFUNDED".equals(status)) t.row("Why", reason(o.optString("reason")));
        } else {
            String code = o.optString("asset");
            t.section("Amounts")
             .row("Pool shares returned", String.valueOf(o.optLong("shares")))
             .row("HARV received", amount("HARV", o.optLong("harvOutRaw")), UiColors.success())
             .row(sym(code) + " received", amount(code, o.optLong("coinOutRaw")), UiColors.success());
        }
        t.section("tFare")
         .row("tFare", amount("HARV", o.optLong("transportFareRaw")))
         .row("Movements", String.valueOf(o.optInt("transportMovements")));
        t.section("Record")
         .row("Started", android.text.format.DateFormat.format("d MMM yyyy, HH:mm", o.optLong("at")))
         .row("Finished", o.has("doneAt") ? android.text.format.DateFormat.format("d MMM yyyy, HH:mm", o.optLong("doneAt")) : "Waiting for blocks")
         .row("Order", shortHash(o.optString("orderId")));
        figures.addView(t.view());

        // Every transaction, each opening in the explorer.
        InfoTable txs = new InfoTable(this);
        addTx(txs, "tFare", o.optString("fareTx"));
        addTx(txs, "In", o.optString("inTx"));
        addTx(txs, "Coin in", o.optString("coinInTx"));
        addTx(txs, "Out", o.optString("outTx"));
        addTx(txs, "Coin out", o.optString("coinOutTx"));
        addTx(txs, "Sent back", o.optString("refundTx"));
        txBox.addView(txs.view());

        ui.removeCallbacks(poll);
        if ("PENDING".equals(status)) ui.postDelayed(poll, 10_000);
    }

    private void addTx(InfoTable t, String label, String hash) {
        if (hash == null || hash.isEmpty()) return;
        t.row(label, shortHash(hash) + "  ›", UiColors.accent());
        android.view.View row = t.view().getChildAt(t.view().getChildCount() - 1);
        row.setOnClickListener(v -> com.harvana.app.ExplorerActivity.openTx(this, hash));
    }

    // ── shared with the list on the Swap screen ───────────────────────────────

    static String title(JSONObject o) {
        String kind = o.optString("kind");
        if ("SWAP".equals(kind)) return sym(o.optString("from")) + " → " + sym(o.optString("to"));
        if ("ADD".equals(kind)) return "Added to HARV / " + sym(o.optString("asset"));
        return "Took back from HARV / " + sym(o.optString("asset"));
    }

    static String statusText(String s) {
        switch (s == null ? "" : s) {
            case "DONE": return "Completed";
            case "SENT": return "Sent";
            case "REFUNDED": return "Refunded";
            default: return "Waiting for blocks";
        }
    }

    static int statusColor(String s) {
        if ("DONE".equals(s) || "SENT".equals(s)) return UiColors.success();
        if ("REFUNDED".equals(s)) return UiColors.warning();
        return UiColors.textSecondary();
    }

    private static String reason(String r) {
        switch (r == null ? "" : r) {
            case "PRICE_MOVED": return "The price moved past your limit";
            case "TOO_SMALL": return "Too small for the pool";
            case "COIN_OFF": return "That coin was switched off";
            case "MISMATCH": return "The amount did not match the order";
            default: return r == null || r.isEmpty() ? "—" : r;
        }
    }

    private static String sym(String code) {
        return code == null || code.isEmpty() || "HARV".equalsIgnoreCase(code) ? "HARV" : Stable.symbol(code);
    }

    private static double units(String code, long raw) {
        return "HARV".equalsIgnoreCase(code) || code == null || code.isEmpty() ? raw / 1e8 : raw / (double) HusdClient.UNIT;
    }

    private static String amount(String code, long raw) {
        String n = "HARV".equalsIgnoreCase(code) || code == null || code.isEmpty()
                ? BalanceFormatter.smartDecimal(BalanceFormatter.rawToHarv(raw)) : HusdClient.format(raw);
        return n + " " + sym(code);
    }

    private static String shortHash(String h) { return h == null || h.length() < 16 ? String.valueOf(h) : h.substring(0, 10) + "…" + h.substring(h.length() - 6); }
}
