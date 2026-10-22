package com.harvana.app.swap;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.harvana.app.R;
import com.harvana.app.sync.NodeGroupClient;

import org.json.JSONObject;

/**
 * After Confirm: what the node says about this swap, step by step. Submitting → submitted (the order and its first
 * transaction are queued) → waiting for blocks (the node's order record is PENDING) → completed (DONE, with what
 * arrived) or returned (REFUNDED: the price moved past the minimum, so the coins came back). Nothing is called
 * successful before the node's record says DONE, and nothing is sent again from here.
 */
final class SwapStatusSheet {

    private final SwapActivity host;
    private final BottomSheetDialog dialog;
    private final View root;
    private final String from, to;
    private final long in, estimate;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private String orderId = "", txHash = "";
    private final Runnable poll = this::poll;

    private SwapStatusSheet(SwapActivity host, String from, String to, long in, long estimate, Runnable onClose) {
        this.host = host;
        this.from = from;
        this.to = to;
        this.in = in;
        this.estimate = estimate;
        dialog = new com.harvana.app.ui.AppSheet(host);
        root = host.getLayoutInflater().inflate(R.layout.sheet_swap_status, null);
        dialog.setContentView(root);
        dialog.setCancelable(false);
        dialog.setOnDismissListener(x -> { ui.removeCallbacks(poll); onClose.run(); });
        root.findViewById(R.id.btn_swap_status_done).setOnClickListener(v -> dialog.dismiss());
    }

    static SwapStatusSheet show(SwapActivity host, String from, String to, long in, long estimate, Runnable onClose) {
        SwapStatusSheet s = new SwapStatusSheet(host, from, to, in, estimate, onClose);
        s.render("Submitting swap", 0, null);
        s.dialog.show();
        return s;
    }

    /** The node accepted the order: follow its record. */
    void submitted(String orderId, String txHash) {
        this.orderId = orderId == null ? "" : orderId;
        this.txHash = txHash == null ? "" : txHash;
        dialog.setCancelable(true);
        render("Transaction pending", 0, null);
        ui.postDelayed(poll, 4_000);
    }

    /** The node refused the request (nothing was queued). */
    void failed(String reason, Runnable retry) {
        dialog.setCancelable(true);
        render("Swap failed", -1, null);
        TextView title = root.findViewById(R.id.tv_swap_status_title);
        LinearLayout rows = root.findViewById(R.id.swap_status_rows);
        rows.removeAllViews();
        row(rows, "Reason", reason);
        row(rows, "Amount", SwapText.amount(from, in) + " (not sent)");
        MaterialButton again = root.findViewById(R.id.btn_swap_status_secondary);
        again.setText("Refresh quote");
        again.setVisibility(View.VISIBLE);
        again.setOnClickListener(v -> { dialog.dismiss(); retry.run(); });
        title.setContentDescription("Swap failed. " + reason);
    }

    private void poll() {
        if (orderId.isEmpty() || !dialog.isShowing()) return;
        NodeGroupClient.swapOrder(orderId, new NodeGroupClient.Callback() {
            @Override public void onSuccess(JSONObject o) { host.runOnUiThread(() -> {
                if (!dialog.isShowing()) return;
                String st = o.optString("status");
                if ("DONE".equals(st)) render("Swap Successful", 1, o);
                else if ("REFUNDED".equals(st)) render("Swap returned", -1, o);
                else { render("Transaction pending", 0, o); ui.postDelayed(poll, 5_000); }
            }); }
            @Override public void onError(String e) { host.runOnUiThread(() -> { if (dialog.isShowing()) ui.postDelayed(poll, 8_000); }); }
        });
    }

    /** tone: 0 waiting, 1 done, -1 failed or returned. {@code o}: the node's order record, when read. */
    private void render(String title, int tone, JSONObject o) {
        ((TextView) root.findViewById(R.id.tv_swap_status_title)).setText(title);
        ProgressBar pb = root.findViewById(R.id.pb_swap_status);
        ImageView iv = root.findViewById(R.id.iv_swap_status);
        pb.setVisibility(tone == 0 ? View.VISIBLE : View.GONE);
        iv.setVisibility(tone == 0 ? View.GONE : View.VISIBLE);
        if (tone != 0) {
            iv.setImageResource(tone > 0 ? R.drawable.ic_check_circle : R.drawable.ic_info);
            iv.setColorFilter(host.tone(tone));
        }
        LinearLayout steps = root.findViewById(R.id.swap_status_steps);
        steps.removeAllViews();
        boolean submittedOk = !orderId.isEmpty();
        step(steps, "Authorised on this phone", true);
        step(steps, "Submitted to the Harvana node", submittedOk);
        step(steps, "Confirmed by the network", tone != 0 && submittedOk);

        LinearLayout rows = root.findViewById(R.id.swap_status_rows);
        rows.removeAllViews();
        row(rows, "You paid", SwapText.amount(from, in));
        if (o != null && "DONE".equals(o.optString("status")) && o.has("outRaw")) {
            row(rows, "You received", SwapText.amount(to, o.optLong("outRaw")));
        } else if (o != null && "REFUNDED".equals(o.optString("status"))) {
            row(rows, "Returned to you", SwapText.amount(from, in));
        } else {
            row(rows, "You receive (estimated)", SwapText.amount(to, estimate));
        }
        if (!txHash.isEmpty()) {
            View r = row(rows, "Transaction", SwapText.shortAddr(txHash) + "  ⧉");
            r.setContentDescription("Copy transaction hash");
            r.setOnClickListener(v -> copy(txHash));
        }
        if (o != null) row(rows, "Date", android.text.format.DateFormat.format("d MMM yyyy · HH:mm", o.optLong("at")).toString());
        MaterialButton secondary = root.findViewById(R.id.btn_swap_status_secondary);
        if (!txHash.isEmpty() && tone != -1) {
            secondary.setText("View in explorer");
            secondary.setVisibility(View.VISIBLE);
            secondary.setOnClickListener(v -> com.harvana.app.ExplorerActivity.openTx(host, txHash));
        } else if (!orderId.isEmpty()) {
            secondary.setText("Details");
            secondary.setVisibility(View.VISIBLE);
            secondary.setOnClickListener(v -> host.startActivity(new android.content.Intent(host, SwapOrderActivity.class)
                    .putExtra(SwapOrderActivity.EXTRA_ID, orderId)));
        } else {
            secondary.setVisibility(View.GONE);
        }
    }

    private void step(LinearLayout box, String label, boolean done) {
        TextView t = new TextView(host);
        t.setText((done ? "✓  " : "○  ") + label);
        t.setTextSize(13);
        t.setTextColor(ContextCompat.getColor(host, done ? R.color.swapSuccess : R.color.swapTextSecondary));
        int pad = Math.round(4 * host.getResources().getDisplayMetrics().density);
        t.setPadding(0, pad, 0, pad);
        box.addView(t);
    }

    private View row(LinearLayout box, String label, String value) {
        View r = host.getLayoutInflater().inflate(R.layout.item_swap_detail, box, false);
        ((TextView) r.findViewById(R.id.tv_swap_detail_label)).setText(label);
        ((TextView) r.findViewById(R.id.tv_swap_detail_value)).setText(value);
        box.addView(r);
        return r;
    }

    private void copy(String s) {
        ClipboardManager cm = (ClipboardManager) host.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("Transaction hash", s));
        Toast.makeText(host, "Copied", Toast.LENGTH_SHORT).show();
    }
}
