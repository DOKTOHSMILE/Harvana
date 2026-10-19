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
        /** Dollar value of the amount ("$1.24"), shown under it. */
        public String amountUsd = "";
        /** Sending and receiving addresses: shown as From → To cards when both are set. */
        public String from = "";
        public String to = "";
        /** Title reads "<title> pending" until confirmed, then "<title> completed". */
        public boolean stateTitle;
    }

    private final BottomSheetDialog dialog;
    private android.widget.ImageView icon;
    private android.animation.ObjectAnimator pulse;
    private boolean shownConfirmed;
    private final TextView status;
    private TextView title;
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

        // The state icon, in the app's duotone style with no background: an amber clock while the network has not
        // confirmed the payment, a green check in a circle once it has. A pending payment never shows the check.
        icon = new android.widget.ImageView(a);
        icon.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
        root.addView(icon, new LinearLayout.LayoutParams(dp(96), dp(96)));
        icon.setScaleX(0.2f); icon.setScaleY(0.2f);
        icon.animate().scaleX(1f).scaleY(1f).setDuration(380).setInterpolator(new android.view.animation.OvershootInterpolator()).start();

        title = text(info.title, 22, R.color.colorTextPrimary, true);
        title.setPadding(0, dp(24), 0, 0);
        root.addView(title);

        if (!info.amount.isEmpty()) {
            TextView amount = text(info.amount.replaceAll("(?i)\\b(HARV)(\\s+HARV)+\\b", "$1"), 34, R.color.colorTextPrimary, true);
            amount.setPadding(0, dp(8), 0, 0);
            root.addView(amount);
        }
        if (info.amountUsd != null && !info.amountUsd.isEmpty()) {
            TextView usd = text(info.amountUsd, 16, R.color.colorTextSecondary, false);
            usd.setPadding(0, dp(2), 0, 0);
            root.addView(usd);
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

        if (info.from != null && !info.from.isEmpty() && info.to != null && !info.to.isEmpty()) {
            View parties = android.view.LayoutInflater.from(a).inflate(R.layout.view_transfer_parties, root, false);
            SendReviewSheet.avatar(parties.findViewById(R.id.party_from_avatar), info.from);
            SendReviewSheet.avatar(parties.findViewById(R.id.party_to_avatar), info.to);
            ((TextView) parties.findViewById(R.id.party_from)).setText(SendReviewSheet.shortAddress(info.from));
            ((TextView) parties.findViewById(R.id.party_to)).setText(SendReviewSheet.shortAddress(info.to));
            LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            pp.topMargin = dp(24);
            root.addView(parties, pp);
        }

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
        share.setOnClickListener(v -> com.harvana.app.receipt.ReceiptLoader.forSuccess(info, r -> com.harvana.app.receipt.ReceiptShare.choose(a, r)));
        root.addView(share);
        String link = txLink(info.txHash);
        if (link != null) {
            TextView shareLink = text("Share transaction link", 14, R.color.colorAccent, true);
            shareLink.setPadding(0, dp(12), 0, dp(4));
            shareLink.setOnClickListener(v -> {
                android.content.Intent send = new android.content.Intent(android.content.Intent.ACTION_SEND);
                send.setType("text/plain");
                send.putExtra(android.content.Intent.EXTRA_TEXT, link);
                a.startActivity(android.content.Intent.createChooser(send, "Share transaction link"));
            });
            root.addView(shareLink);
        }

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
        dialog.setOnDismissListener(d -> { ring.stop(); if (pulse != null) pulse.cancel(); });
    }

    /** Shows the sheet on this activity. Must be called on the main thread. */
    public static PaymentSuccessSheet show(Activity a, Info info) {
        PaymentSuccessSheet s = new PaymentSuccessSheet(a, info);
        s.dialog.show();
        return s;
    }

    public boolean isShowing() { return dialog.isShowing(); }

    public void setConfirmed(boolean confirmed) {
        boolean wasPending = info != null && !shownConfirmed && icon.getDrawable() != null;
        info.confirmed = confirmed;
        showStateIcon(confirmed, wasPending);
        if (info.stateTitle && title != null) title.setText(info.title + (confirmed ? " completed" : " pending"));
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

    /** Amber clock while pending; green check when confirmed, popping in when it changes in front of the user. */
    private void showStateIcon(boolean confirmed, boolean animateChange) {
        if (confirmed) {
            if (shownConfirmed) return;
            shownConfirmed = true;
            if (pulse != null) pulse.cancel();
            icon.setAlpha(1f);
            final int to = color(R.color.colorSuccess);
            if (animateChange) {
                int from = color(R.color.colorAmber);
                icon.animate().scaleX(0.6f).scaleY(0.6f).alpha(0f).setDuration(140).withEndAction(() -> {
                    icon.setImageResource(R.drawable.ic_status_check);
                    android.animation.ValueAnimator fade = android.animation.ValueAnimator.ofObject(new android.animation.ArgbEvaluator(), from, to);
                    fade.setDuration(260);
                    fade.addUpdateListener(an -> tint((int) an.getAnimatedValue()));
                    fade.start();
                    icon.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(320)
                        .setInterpolator(new android.view.animation.OvershootInterpolator(2.2f)).start();
                }).start();
            } else {
                icon.setImageResource(R.drawable.ic_status_check);
                tint(to);
            }
            icon.setContentDescription("Confirmed");
        } else {
            icon.setImageResource(R.drawable.ic_status_pending);
            tint(color(R.color.colorAmber));
            icon.setContentDescription("Waiting for the network");
            if (pulse == null) {
                pulse = android.animation.ObjectAnimator.ofFloat(icon, "alpha", 1f, 0.55f);
                pulse.setDuration(900);
                pulse.setRepeatMode(android.animation.ValueAnimator.REVERSE);
                pulse.setRepeatCount(android.animation.ValueAnimator.INFINITE);
            }
            if (!pulse.isStarted()) pulse.start();
        }
    }

    /** Colours the duotone icon; SRC_IN keeps its two tones (line and soft fill) in the one colour. */
    private void tint(int c) {
        androidx.core.widget.ImageViewCompat.setImageTintMode(icon, android.graphics.PorterDuff.Mode.SRC_IN);
        androidx.core.widget.ImageViewCompat.setImageTintList(icon, android.content.res.ColorStateList.valueOf(c));
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

    /** The Harvana explorer page of a transaction, or null for a hash that is not a Harvana one. */
    private static String txLink(String hash) {
        if (hash == null || !hash.matches("^(0x)?[0-9a-fA-F]{64}$")) return null;
        return com.harvana.app.web3.Web3Home.EXPLORER_URL + "#/tx/" + hash;
    }

    private static String shorten(String v) {
        return v.length() > 24 && v.startsWith("HV") ? v.substring(0, 10) + "…" + v.substring(v.length() - 6) : v;
    }

    private int color(int res) { return ContextCompat.getColor(ctx, res); }
    private int dp(int v) { return Math.round(v * ctx.getResources().getDisplayMetrics().density); }
}
