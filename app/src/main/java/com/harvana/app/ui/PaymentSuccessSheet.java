package com.harvana.app.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.harvana.app.R;

import androidx.core.content.ContextCompat;

/**
 * Full-screen "it worked" sheet shown after any successful payment in the app. The line under the amount
 * starts as "Confirming on the network…" with a rolling ring and turns to "Confirmed" once the chain has the
 * transaction, if the sheet is still open.
 */
public final class PaymentSuccessSheet {

    /** What a payment tells the user. rows are {label, value} pairs. */
    public static final class Info {
        public String title = "Payment successful";
        public String amount = "";
        public String note = "";
        public String[][] rows = new String[0][];
        public String txHash = "";
        public boolean confirmed;
    }

    private final BottomSheetDialog dialog;
    private final TextView status;
    private final SpinnerDrawable ring;
    private final Context ctx;
    private final Info info;

    private PaymentSuccessSheet(Activity a, Info info) {
        ctx = a;
        this.info = info;
        dialog = new AppSheet(a);

        LinearLayout root = new LinearLayout(a);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(24), dp(48), dp(24), dp(24));

        TextView tick = new TextView(a);
        tick.setText("✓");
        tick.setGravity(Gravity.CENTER);
        tick.setTextSize(44);
        tick.setTextColor(color(R.color.colorTextOnAccent));
        tick.setTypeface(null, Typeface.BOLD);
        android.graphics.drawable.GradientDrawable disc = new android.graphics.drawable.GradientDrawable();
        disc.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        disc.setColor(color(R.color.colorSuccess));
        tick.setBackground(disc);
        root.addView(tick, new LinearLayout.LayoutParams(dp(96), dp(96)));
        tick.setScaleX(0.2f); tick.setScaleY(0.2f);
        tick.animate().scaleX(1f).scaleY(1f).setDuration(380).setInterpolator(new android.view.animation.OvershootInterpolator()).start();

        TextView title = text(info.title, 22, R.color.colorTextPrimary, true);
        title.setPadding(0, dp(24), 0, 0);
        root.addView(title);

        if (!info.amount.isEmpty()) {
            TextView amount = text(info.amount, 34, R.color.colorTextPrimary, true);
            amount.setPadding(0, dp(8), 0, 0);
            root.addView(amount);
        }
        if (!info.note.isEmpty()) {
            TextView note = text(info.note, 14, R.color.colorTextMuted, false);
            note.setPadding(dp(12), dp(8), dp(12), 0);
            root.addView(note);
        }

        LinearLayout statusRow = new LinearLayout(a);
        statusRow.setOrientation(LinearLayout.HORIZONTAL);
        statusRow.setGravity(Gravity.CENTER);
        statusRow.setPadding(0, dp(16), 0, 0);
        status = text("", 13, R.color.colorTextMuted, false);
        ring = new SpinnerDrawable(dp(16), color(R.color.colorAccent));
        status.setCompoundDrawablesRelativeWithIntrinsicBounds(ring, null, null, null);
        status.setCompoundDrawablePadding(dp(8));
        statusRow.addView(status);
        root.addView(statusRow);
        setConfirmed(info.confirmed);

        if (info.rows.length > 0) {
            LinearLayout card = new LinearLayout(a);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(16), dp(8), dp(16), dp(8));
            android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
            bg.setCornerRadius(dp(16));
            bg.setColor(color(R.color.colorSurface));
            bg.setStroke(dp(1), color(R.color.colorCardStroke));
            card.setBackground(bg);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cp.topMargin = dp(28);
            for (String[] r : info.rows) {
                if (r == null || r.length < 2 || r[1] == null || r[1].isEmpty()) continue;
                LinearLayout line = new LinearLayout(a);
                line.setOrientation(LinearLayout.HORIZONTAL);
                line.setPadding(0, dp(8), 0, dp(8));
                TextView k = text(r[0], 13, R.color.colorTextMuted, false);
                k.setGravity(Gravity.START);
                TextView v = text(shorten(r[1]), 13, R.color.colorTextPrimary, true);
                v.setGravity(Gravity.END);
                line.addView(k, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
                line.addView(v, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2));
                card.addView(line);
            }
            root.addView(card, cp);
        }

        View spacer = new View(a);
        root.addView(spacer, new LinearLayout.LayoutParams(1, 0, 1));

        TextView share = text("Share receipt", 14, R.color.colorAccent, true);
        share.setPadding(0, dp(20), 0, dp(4));
        share.setOnClickListener(v -> com.harvana.app.receipt.ReceiptShare.choose(a, com.harvana.app.receipt.Receipts.fromSuccess(info)));
        root.addView(share);

        MaterialButton done = new MaterialButton(a);
        done.setText("Done");
        done.setAllCaps(false);
        done.setTextColor(color(R.color.colorTextOnAccent));
        done.setCornerRadius(dp(14));
        done.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color(R.color.colorAccent)));
        done.setOnClickListener(v -> dialog.dismiss());
        LinearLayout.LayoutParams dp0 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56));
        dp0.topMargin = dp(24);
        root.addView(done, dp0);

        ScrollView scroll = new ScrollView(a);
        scroll.setFillViewport(true);
        scroll.addView(root, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        dialog.setContentView(scroll);
        com.harvana.app.group.BottomSheetUtils.expandFullScreen(dialog, scroll);
        dialog.setOnDismissListener(d -> ring.stop());
    }

    /** Shows the sheet on this activity. Must be called on the main thread. */
    public static PaymentSuccessSheet show(Activity a, Info info) {
        PaymentSuccessSheet s = new PaymentSuccessSheet(a, info);
        s.dialog.show();
        return s;
    }

    public boolean isShowing() { return dialog.isShowing(); }

    public void setConfirmed(boolean confirmed) {
        info.confirmed = confirmed;
        if (confirmed) {
            ring.stop();
            status.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, null, null);
            status.setText("Confirmed on the network");
            status.setTextColor(color(R.color.colorSuccess));
        } else {
            status.setText("Confirming on the network…");
            status.setCompoundDrawablesRelativeWithIntrinsicBounds(ring, null, null, null);
            ring.start();
        }
    }

    private TextView text(String s, int sp, int colorRes, boolean bold) {
        TextView t = new TextView(ctx);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color(colorRes));
        t.setGravity(Gravity.CENTER);
        if (bold) t.setTypeface(null, Typeface.BOLD);
        return t;
    }

    private static String shorten(String v) {
        return v.length() > 24 && v.startsWith("HV") ? v.substring(0, 10) + "…" + v.substring(v.length() - 6) : v;
    }

    private int color(int res) { return ContextCompat.getColor(ctx, res); }
    private int dp(int v) { return Math.round(v * ctx.getResources().getDisplayMetrics().density); }
}
