package com.harvanna.wallet.circle;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.harvanna.wallet.R;
import com.harvanna.wallet.sync.AjooNodeClient;
import com.harvanna.wallet.wallet.BalanceFormatter;

import java.math.BigDecimal;

/** Small view builders + amount helpers shared by the Circle screens (theme tokens only, day/night safe). */
final class CircleUi {

    private CircleUi() {}

    static int dp(Context c, int v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    static int color(Context c, int res) {
        return ContextCompat.getColor(c, res);
    }

    static TextView text(Context c, String s, float sp, int colorRes, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color(c, colorRes));
        if (bold) t.setTypeface(null, Typeface.BOLD);
        return t;
    }

    static LinearLayout card(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setBackgroundResource(R.drawable.bg_quick_action);
        l.setPadding(dp(c, 16), dp(c, 14), dp(c, 16), dp(c, 14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(c, 12);
        l.setLayoutParams(lp);
        return l;
    }

    /** "Label ........ value" row. */
    static View row(Context c, String label, String value) {
        LinearLayout r = new LinearLayout(c);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setPadding(0, dp(c, 5), 0, dp(c, 5));
        TextView l = text(c, label, 13.5f, R.color.colorTextSecondary, false);
        TextView v = text(c, value, 13.5f, R.color.colorTextPrimary, true);
        v.setGravity(Gravity.END);
        r.addView(l, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        r.addView(v, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.2f));
        return r;
    }

    static TextView chip(Context c, String label, int fgColorRes) {
        TextView t = text(c, label, 11.5f, fgColorRes, true);
        t.setPadding(dp(c, 10), dp(c, 3), dp(c, 10), dp(c, 3));
        t.setBackgroundResource(R.drawable.bg_chip_muted);
        return t;
    }

    static int stateColor(String state) {
        if (state == null) return R.color.colorTextSecondary;
        switch (state) {
            case "ACTIVE": case "COMPLETED": return R.color.colorSuccess;
            case "FORMING": case "LATE": return R.color.colorWarning;
            case "ABORTED": case "DEFAULTED_PRE_PAYOUT": case "DEFAULTED_POST_PAYOUT": return R.color.colorError;
            default: return R.color.colorTextSecondary;
        }
    }

    /** Raw units (1 HARV = 1e8) to a trimmed display string, without the unit. */
    static String harv(long raw) {
        return BalanceFormatter.smartDecimal(AjooNodeClient.toAjcDisplay(raw)) + " HARV";
    }

    /** Exact HARV text to raw units (no floating point). Returns -1 when invalid. */
    static long toRaw(String s) {
        try {
            if (s == null || s.trim().isEmpty()) return -1;
            BigDecimal v = new BigDecimal(s.trim());
            if (v.signum() <= 0 || v.scale() > 8) return -1;
            return v.movePointRight(8).longValueExact();
        } catch (Exception e) {
            return -1;
        }
    }

    /** Node rejection codes to plain language. */
    static String friendly(String error) {
        if (error == null) return "Something went wrong.";
        String e = error.toUpperCase();
        if (e.contains("INSUFFICIENT_BALANCE")) return "Not enough available balance to lock the required bond and fee.";
        if (e.contains("INVALID_MEMBER_LIMIT")) return "A circle needs between 3 and 12 members.";
        if (e.contains("INVALID_CYCLE_DURATION")) return "Cycle length must be between 1 and 30 days.";
        if (e.contains("CONTRIBUTION_TOO_LARGE")) return "That contribution is too large.";
        if (e.contains("SENDER_ADDRESS_FROZEN")) return "This wallet is frozen and can't join circles.";
        if (e.contains("COIN_PAUSED")) return "The network is temporarily paused. Try again later.";
        if (e.contains("CIRCLE_NOT_FORMING")) return "This circle is no longer open for new members.";
        if (e.contains("CIRCLE_NOT_ACTIVE")) return "This circle isn't running yet.";
        if (e.contains("INVALID_NONCE")) return "Your request was out of sequence. Pull to refresh and try again.";
        if (e.contains("INVALID_SIGNATURE")) return "Couldn't verify your signature. Please try again.";
        return error;
    }
}
