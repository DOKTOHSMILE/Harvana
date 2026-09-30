package com.harvanna.app.circle;

import android.app.Activity;
import android.content.Context;
import android.util.TypedValue;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.harvanna.app.R;
import com.harvanna.app.sync.HarvannaNodeClient;
import com.harvanna.app.wallet.BalanceFormatter;

import java.math.BigDecimal;

/** Small view builders + amount helpers shared by the Circle screens (theme tokens only, day/night safe). */
public final class CircleUi {

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
    public static String harv(long raw) {
        return BalanceFormatter.smartDecimal(HarvannaNodeClient.toHarvDisplay(raw)) + " HARV";
    }

    /** Exact HARV text to raw units (no floating point). Returns -1 when invalid. */
    public static long toRaw(String s) {
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
    public static String friendly(String error) {
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
        if (e.contains("NOT_ELIGIBLE_NO_CONTRIBUTIONS")) return "You need to have paid into this group at least once first.";
        if (e.contains("NOT_YET_ELIGIBLE")) return "New members can vote after 48 hours in the group.";
        if (e.contains("EXPOSURE_LIMIT_EXCEEDED")) return "You're already committed to as many circles as your account allows.";
        if (e.contains("TERMS_HASH_MISMATCH")) return "The circle's terms changed. Reopen it and read them again.";
        if (e.contains("CIRCLE_FULL")) return "This circle is full.";
        if (e.contains("ALREADY_A_MEMBER")) return "You're already a member of this circle.";
        if (e.contains("ALREADY_CONTRIBUTED_THIS_ROUND")) return "You've already paid this round.";
        if (e.contains("NO_CONTRIBUTION_DUE_ON_OWN_PAYOUT_ROUND")) return "It's your turn to be paid this round, so nothing is due from you.";
        if (e.contains("FORMATION_DEADLINE_EXPIRED")) return "This circle ran out of time to fill.";
        if (e.contains("MEMBER_NOT_ELIGIBLE_TO_CONTRIBUTE")) return "Your place in this circle can't pay right now.";
        if (e.contains("MAX_FEE_TOO_LOW")) return "The network fee changed. Please try again.";
        if (e.contains("LOCK_TEMPLATE_EXPIRED") || e.contains("NO_PENDING_LOCK")) return "That step expired. Start the action again.";
        if (e.contains("SIGNING THE LOCK FAILED")) return "The action was accepted but your phone couldn't finish signing the payment. Open the circle again to retry.";
        return error;
    }

    /** Toolbar (theme appBarColor, back arrow) above the body, plus a status bar that matches it. */
    static LinearLayout withToolbar(Activity a, String title, View body) {
        TypedValue bar = new TypedValue();
        TypedValue on = new TypedValue();
        a.getTheme().resolveAttribute(R.attr.appBarColor, bar, true);
        a.getTheme().resolveAttribute(R.attr.onAppBarColor, on, true);
        TypedValue h = new TypedValue();
        a.getTheme().resolveAttribute(androidx.appcompat.R.attr.actionBarSize, h, true);
        int height = TypedValue.complexToDimensionPixelSize(h.data, a.getResources().getDisplayMetrics());

        androidx.appcompat.widget.Toolbar tb = new androidx.appcompat.widget.Toolbar(a);
        tb.setBackgroundColor(bar.data);
        tb.setTitle(title);
        tb.setTitleTextColor(on.data);
        android.graphics.drawable.Drawable back = androidx.core.content.ContextCompat.getDrawable(a, R.drawable.ic_arrow_back);
        if (back != null) {
            back = androidx.core.graphics.drawable.DrawableCompat.wrap(back.mutate());
            androidx.core.graphics.drawable.DrawableCompat.setTint(back, on.data);
            tb.setNavigationIcon(back);
        }
        tb.setNavigationContentDescription("Back");
        tb.setNavigationOnClickListener(v -> a.finish());

        LinearLayout root = new LinearLayout(a);
        root.setOrientation(LinearLayout.VERTICAL);
        root.addView(tb, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height));
        root.addView(body, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        com.harvanna.app.utils.StatusBarUtil.matchToolbar(a);
        return root;
    }
}
