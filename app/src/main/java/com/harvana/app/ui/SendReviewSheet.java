package com.harvana.app.ui;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.harvana.app.R;
import com.harvana.app.wallet.UsdValue;

import java.util.ArrayList;
import java.util.List;

/**
 * Review &amp; Send (owner, 19 Oct 2026, from the HOT Wallet screens): the coin and amount with its dollar value, an
 * arrow to the recipient, then the full address with copy, the network fee in dollars with the coin amount under it,
 * any other rows, and one Transfer button. Transfer asks for the PIN, then hands over to the send.
 */
public final class SendReviewSheet {

    public static final class Data {
        /** Asset key for prices (HARV, HNGN, SOL…). */
        public String assetKey = "HARV";
        public Drawable icon;
        public double amount;
        /** "5 HARV". */
        public String amountText = "";
        public String recipient = "";
        public String network = "Harvana";
        /** The fee in its own coin ("0.0003 HARV"), or a value when it is not known yet. */
        public String feeText = "";
        /** Fee amount and the coin it is paid in, for its dollar value; NaN when not known. */
        public double feeAmount = Double.NaN;
        public String feeKey = "HARV";
        /** Other rows {label, value}: From, Memo, Total. */
        public List<String[]> rows = new ArrayList<>();
        public String title = "Review & Send";
    }

    public interface Send { void go(SendReviewSheet sheet); }

    private final Activity a;
    private final BottomSheetDialog dialog;
    private final MaterialButton send;
    private final TextView error;
    private final TextView feeUsd, feeCoin;
    private final Data d;
    private boolean going;

    private SendReviewSheet(Activity a, Data d, Send onSend, Runnable onCancelled) {
        this.a = a;
        this.d = d;
        dialog = new AppSheet(a);
        View v = LayoutInflater.from(a).inflate(R.layout.sheet_send_review, null);
        dialog.setContentView(v);
        com.harvana.app.group.BottomSheetUtils.expandFullScreen(dialog, v);

        ((TextView) v.findViewById(R.id.review_title)).setText(d.title);
        v.findViewById(R.id.review_close).setOnClickListener(x -> dialog.dismiss());

        ImageView icon = v.findViewById(R.id.review_coin_icon);
        if (d.icon != null) icon.setImageDrawable(d.icon);
        ((TextView) v.findViewById(R.id.review_amount)).setText(d.amountText);
        TextView amountUsd = v.findViewById(R.id.review_amount_usd);

        TextView avatar = v.findViewById(R.id.review_avatar);
        avatar(avatar, d.recipient);
        ((TextView) v.findViewById(R.id.review_to_name)).setText(shortAddress(d.recipient));
        ((TextView) v.findViewById(R.id.review_to_network)).setText(d.network);

        TextView recipient = v.findViewById(R.id.review_recipient);
        recipient.setText(d.recipient);
        v.findViewById(R.id.review_copy).setOnClickListener(x -> {
            android.content.ClipboardManager cm = (android.content.ClipboardManager) a.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
            if (cm != null) cm.setPrimaryClip(android.content.ClipData.newPlainText("Recipient", d.recipient));
            android.widget.Toast.makeText(a, "Copied", android.widget.Toast.LENGTH_SHORT).show();
        });

        feeUsd = v.findViewById(R.id.review_fee_usd);
        feeCoin = v.findViewById(R.id.review_fee_coin);
        v.findViewById(R.id.review_fee_info).setVisibility(View.GONE);

        LinearLayout rows = v.findViewById(R.id.review_rows);
        LayoutInflater inf = LayoutInflater.from(a);
        for (String[] r : d.rows) {
            if (r == null || r.length < 2 || r[1] == null || r[1].trim().isEmpty()) continue;
            View row = inf.inflate(R.layout.item_review_row, rows, false);
            ((TextView) row.findViewById(R.id.row_label)).setText(r[0]);
            ((TextView) row.findViewById(R.id.row_value)).setText(shortAddress(r[1].trim()));
            rows.addView(row);
        }

        error = v.findViewById(R.id.review_error);
        send = v.findViewById(R.id.review_send);
        send.setOnClickListener(x -> com.harvana.app.wallet.PinPromptHelper.authorize(a, "Send " + d.amountText, () -> {
            List<String[]> receipt = new ArrayList<>();
            receipt.add(new String[]{"To", d.recipient});
            receipt.add(new String[]{"Network", d.network});
            receipt.add(new String[]{"tFare", feeLine()});
            receipt.addAll(d.rows);
            com.harvana.app.tx.PaymentTracker.noteReview(d.amountText, receipt);
            going = true;
            onSend.go(this);
        }));
        dialog.setOnDismissListener(x -> { if (!going && onCancelled != null) onCancelled.run(); });

        paintValues(amountUsd);
        UsdValue.load(a, () -> { if (dialog.isShowing()) paintValues(amountUsd); });
    }

    private void paintValues(TextView amountUsd) {
        String usd = UsdValue.text(a, d.assetKey, d.amount);
        amountUsd.setText(usd.isEmpty() ? "—" : usd);
        String fee = Double.isNaN(d.feeAmount) ? "" : UsdValue.text(a, d.feeKey, d.feeAmount);
        if (fee.isEmpty()) {
            feeUsd.setText(d.feeText.isEmpty() ? "—" : d.feeText);
            feeCoin.setVisibility(View.GONE);
        } else {
            feeUsd.setText(fee);
            feeCoin.setText(d.feeText);
            feeCoin.setVisibility(d.feeText.isEmpty() ? View.GONE : View.VISIBLE);
        }
    }

    private String feeLine() {
        String fee = Double.isNaN(d.feeAmount) ? "" : UsdValue.text(a, d.feeKey, d.feeAmount);
        return fee.isEmpty() ? d.feeText : d.feeText + " (" + fee + ")";
    }

    /** Shows the sheet; {@code onSend} runs after the PIN, with the sheet still open. */
    public static SendReviewSheet show(Activity a, Data d, Send onSend, Runnable onCancelled) {
        SendReviewSheet s = new SendReviewSheet(a, d, onSend, onCancelled);
        s.dialog.show();
        return s;
    }

    /** Closes the sheet and runs {@code then}: for sends that show their own progress. */
    public static SendReviewSheet show(Activity a, Data d, Runnable then) {
        return show(a, d, s -> { s.dismiss(); then.run(); }, null);
    }

    public void busy(boolean on) {
        send.setEnabled(!on);
        send.setText(on ? "Sending…" : "Transfer");
        if (on) error.setVisibility(View.GONE);
        dialog.setCancelable(!on);
    }

    public void error(String msg) {
        going = false;
        busy(false);
        error.setText(msg);
        error.setVisibility(View.VISIBLE);
    }

    public void dismiss() { if (dialog.isShowing()) dialog.dismiss(); }


    /** A round avatar for an address: its first character on a colour taken from the address. */
    public static void avatar(TextView v, String address) {
        v.setText("");                                    // a picture of the address, not a letter (owner, 20 Oct 2026)
        v.setBackground(Identicon.of(address));
    }

    public static String shortAddress(String v) {
        if (v == null) return "";
        boolean addr = v.matches("^(HV0x|AJ0x|0x)[0-9a-fA-F]{20,}$") || (v.length() > 30 && !v.contains(" "));
        return addr && v.length() > 16 ? v.substring(0, 6) + "…" + v.substring(v.length() - 6) : v;
    }
}
