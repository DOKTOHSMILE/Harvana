package com.harvana.app.scan;

import android.app.Activity;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.widget.ImageViewCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.harvana.app.R;
import com.harvana.app.sync.HarvanaNodeClient;
import com.harvana.app.utils.UiColors;

import org.json.JSONObject;

/**
 * A scanned receipt, checked against the network: confirmed (with block, amount, sender, recipient and time), still
 * waiting to be confirmed, or not known to the network (the receipt may not be genuine).
 */
public final class TxVerifySheet {
    private TxVerifySheet() {}

    public static void show(Activity a, String hash) {
        BottomSheetDialog dlg = new com.harvana.app.ui.AppSheet(a);
        LinearLayout root = new LinearLayout(a);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = dp(a, 24);
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(UiColors.surface());
        ImageView icon = new ImageView(a);
        icon.setImageResource(R.drawable.ic_status_pending);
        tint(icon, a.getColor(R.color.colorAmber));
        root.addView(icon, new LinearLayout.LayoutParams(dp(a, 72), dp(a, 72)));
        TextView title = text(a, "Checking this receipt on the network…", 18, true, UiColors.textPrimary());
        title.setPadding(0, dp(a, 12), 0, 0);
        root.addView(title);
        TextView body = text(a, hash.isEmpty() ? "" : shortHash(hash), 13, false, UiColors.textSecondary());
        body.setPadding(0, dp(a, 6), 0, dp(a, 8));
        root.addView(body);
        LinearLayout rows = new LinearLayout(a);
        rows.setOrientation(LinearLayout.VERTICAL);
        root.addView(rows, new LinearLayout.LayoutParams(-1, -2));
        dlg.setContentView(root);
        dlg.show();

        if (hash.isEmpty()) { notFound(a, icon, title, body); return; }
        HarvanaNodeClient.nodeGet("/api/v1/tx/" + hash, new HarvanaNodeClient.JsonCallback() {
            @Override public void onSuccess(JSONObject tx) {
                a.runOnUiThread(() -> {
                    icon.setImageResource(R.drawable.ic_status_check);
                    tint(icon, a.getColor(R.color.colorSuccess));
                    title.setText("Genuine: confirmed on the network");
                    body.setText("This transaction is recorded in block " + tx.optLong("blockHeight") + ".");
                    rows.addView(row(a, "Amount", com.harvana.app.wallet.BalanceFormatter.smart(tx.optLong("amount") / 100_000_000.0)));
                    rows.addView(row(a, "From", tx.optString("from")));
                    rows.addView(row(a, "To", tx.optString("to")));
                    if (tx.optLong("timestamp") > 0) rows.addView(row(a, "When",
                            new java.text.SimpleDateFormat("d MMM yyyy, HH:mm", java.util.Locale.getDefault()).format(new java.util.Date(tx.optLong("timestamp")))));
                    String kind = com.harvana.app.tx.TxKind.of(tx.optString("memo"), false) == null ? "" : com.harvana.app.tx.TxKind.of(tx.optString("memo"), false).title;
                    if (!kind.isEmpty()) rows.addView(row(a, "For", kind));
                    rows.addView(row(a, "Transaction", shortHash(hash)));
                });
            }
            @Override public void onError(String error) {
                HarvanaNodeClient.getTxStatus(hash, new HarvanaNodeClient.JsonCallback() {
                    @Override public void onSuccess(JSONObject s) {
                        a.runOnUiThread(() -> {
                            if ("PENDING".equalsIgnoreCase(s.optString("status"))) {
                                title.setText("Sent, not confirmed yet");
                                body.setText("The network has this transaction and is waiting to put it in a block. Scan again in a minute.");
                            } else notFound(a, icon, title, body);
                        });
                    }
                    @Override public void onError(String e) { a.runOnUiThread(() -> notFound(a, icon, title, body)); }
                });
            }
        });
    }

    private static void notFound(Activity a, ImageView icon, TextView title, TextView body) {
        icon.setImageResource(R.drawable.ic_warning);
        tint(icon, a.getColor(R.color.colorError));
        title.setText("Not found on the network");
        body.setText("The network has no transaction for this receipt. It may not be genuine, or it was never sent. Check your connection and scan again before relying on it.");
    }

    private static LinearLayout row(Activity a, String k, String v) {
        LinearLayout r = new LinearLayout(a);
        r.setPadding(0, dp(a, 7), 0, dp(a, 7));
        TextView kt = text(a, k, 13, false, UiColors.textSecondary());
        kt.setGravity(Gravity.START);
        r.addView(kt, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView vt = text(a, v.length() > 24 && v.startsWith("HV") ? v.substring(0, 10) + "…" + v.substring(v.length() - 6) : v, 13, true, UiColors.textPrimary());
        vt.setGravity(Gravity.END);
        r.addView(vt, new LinearLayout.LayoutParams(0, -2, 1.6f));
        return r;
    }

    private static String shortHash(String h) { return h.length() > 20 ? h.substring(0, 10) + "…" + h.substring(h.length() - 8) : h; }

    private static void tint(ImageView v, int c) {
        ImageViewCompat.setImageTintMode(v, android.graphics.PorterDuff.Mode.SRC_IN);
        ImageViewCompat.setImageTintList(v, android.content.res.ColorStateList.valueOf(c));
    }

    private static TextView text(Activity a, String s, float sp, boolean bold, int color) {
        TextView t = new TextView(a);
        t.setText(s); t.setTextSize(sp); t.setTextColor(color); t.setGravity(Gravity.CENTER);
        if (bold) t.setTypeface(null, android.graphics.Typeface.BOLD);
        return t;
    }

    private static int dp(Activity a, int v) { return Math.round(v * a.getResources().getDisplayMetrics().density); }
}
