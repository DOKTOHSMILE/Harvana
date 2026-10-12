package com.harvana.app.circle;

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

import com.harvana.app.R;
import com.harvana.app.sync.HarvanaNodeClient;
import com.harvana.app.wallet.BalanceFormatter;

import java.math.BigDecimal;

/** Small view builders + amount helpers shared by the Circle screens (theme tokens only, day/night safe). */
public final class CircleUi {

    private CircleUi() {}

    public static int dp(Context c, int v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    public static int color(Context c, int res) {
        return ContextCompat.getColor(c, res);
    }

    public static TextView text(Context c, String s, float sp, int colorRes, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color(c, colorRes));
        if (bold) t.setTypeface(null, Typeface.BOLD);
        return t;
    }

    public static LinearLayout card(Context c) {
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
    public static View row(Context c, String label, String value) {
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

    public static TextView chip(Context c, String label, int fgColorRes) {
        TextView t = text(c, label, 11.5f, fgColorRes, true);
        t.setPadding(dp(c, 10), dp(c, 3), dp(c, 10), dp(c, 3));
        t.setBackgroundResource(R.drawable.bg_chip_muted);
        return t;
    }

    public static int stateColor(String state) {
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
        return BalanceFormatter.smartDecimal(HarvanaNodeClient.toHarvDisplay(raw)) + " HARV";
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
        if (com.harvana.app.utils.ErrorText.is(e, "GROUP_NOT_FOUND") || com.harvana.app.utils.ErrorText.is(e, "CIRCLE_NOT_FOUND"))
            return "The network doesn't have this group. What the app shows is an old saved copy, so this action can't go through.";
        if (com.harvana.app.utils.ErrorText.is(e, "NOT_ADMIN_TRIGGERED_RULE")) return "This goal doesn't pay out by an admin request. Its rule decides how the money leaves.";
        if (com.harvana.app.utils.ErrorText.is(e, "FIXED_GROUP_NOT_ACTIVE")) return "This goal group isn't active, so it can't take that action.";
        if (com.harvana.app.utils.ErrorText.is(e, "NOT_THE_GROUP_ADMIN")) return "Only the group's admin (or its treasurer, for money requests) can do this.";
        if (com.harvana.app.utils.ErrorText.is(e, "TREASURER_NOT_A_MEMBER")) return "The treasurer must be an active member of the group.";
        if (com.harvana.app.utils.ErrorText.is(e, "TREASURER_CANNOT_BE_THE_ADMIN")) return "The admin already has every power, so they cannot also be the treasurer.";
        if (com.harvana.app.utils.ErrorText.is(e, "INVALID_TREASURER_ADDRESS")) return "That member has no wallet address on file.";
        if (com.harvana.app.utils.ErrorText.is(e, "INSUFFICIENT_BALANCE")) return "Not enough available balance for the bond or the creation fee.";
        if (com.harvana.app.utils.ErrorText.is(e, "INVALID_MEMBER_LIMIT")) return "A circle needs between 3 and 12 members.";
        if (com.harvana.app.utils.ErrorText.is(e, "INVALID_CYCLE_DURATION")) return "Cycle length must be between 1 and 30 days.";
        if (com.harvana.app.utils.ErrorText.is(e, "CONTRIBUTION_TOO_LARGE")) return "That contribution is too large.";
        if (com.harvana.app.utils.ErrorText.is(e, "SENDER_ADDRESS_FROZEN")) return "This wallet is frozen and can't join circles.";
        if (com.harvana.app.utils.ErrorText.is(e, "COIN_PAUSED")) return "The network is temporarily paused. Try again later.";
        if (com.harvana.app.utils.ErrorText.is(e, "CIRCLE_NOT_FORMING")) return "This circle is no longer open for new members.";
        if (com.harvana.app.utils.ErrorText.is(e, "CIRCLE_NOT_ACTIVE")) return "This circle isn't running yet.";
        if (com.harvana.app.utils.ErrorText.is(e, "INVALID_NONCE")) return "Your request was out of sequence. Pull to refresh and try again.";
        if (com.harvana.app.utils.ErrorText.is(e, "INVALID_SIGNATURE")) return "Couldn't verify your signature. Please try again.";
        if (com.harvana.app.utils.ErrorText.is(e, "NOT_ELIGIBLE_NO_CONTRIBUTIONS")) return "You need to have paid into this group at least once first.";
        if (com.harvana.app.utils.ErrorText.is(e, "NOT_YET_ELIGIBLE")) return "New members can vote after 48 hours in the group.";
        if (com.harvana.app.utils.ErrorText.is(e, "EXPOSURE_LIMIT_EXCEEDED")) return "You're already committed to as many circles as your account allows.";
        if (com.harvana.app.utils.ErrorText.is(e, "TERMS_HASH_MISMATCH")) return "The circle's terms changed. Reopen it and read them again.";
        if (com.harvana.app.utils.ErrorText.is(e, "ADMIN_MUST_JOIN_FIRST")) return "The admin has to join first and lock their bond. Try again once they have.";
        if (com.harvana.app.utils.ErrorText.is(e, "CIRCLE_FULL")) return "This circle is full.";
        if (com.harvana.app.utils.ErrorText.is(e, "ALREADY_A_MEMBER")) return "You're already a member of this circle.";
        if (com.harvana.app.utils.ErrorText.is(e, "ALREADY_CONTRIBUTED_THIS_ROUND")) return "You've already paid this round.";
        if (com.harvana.app.utils.ErrorText.is(e, "NO_CONTRIBUTION_DUE_ON_OWN_PAYOUT_ROUND")) return "It's your turn to be paid this round, so nothing is due from you.";
        if (com.harvana.app.utils.ErrorText.is(e, "FORMATION_DEADLINE_EXPIRED")) return "This circle ran out of time to fill.";
        if (com.harvana.app.utils.ErrorText.is(e, "MEMBER_NOT_ELIGIBLE_TO_CONTRIBUTE")) return "Your place in this circle can't pay right now.";
        if (com.harvana.app.utils.ErrorText.is(e, "MAX_FEE_TOO_LOW")) return "The transport fare changed. Please try again.";
        if (com.harvana.app.utils.ErrorText.is(e, "LOCK_TEMPLATE_EXPIRED") || com.harvana.app.utils.ErrorText.is(e, "NO_PENDING_LOCK")) return "That step expired. Start the action again.";
        if (e.contains("SIGNING THE LOCK FAILED")) return "The action was accepted but your phone couldn't finish signing the payment. Open the circle again to retry.";
        return error;
    }
}
