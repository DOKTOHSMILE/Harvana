package com.harvana.app.group;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.harvana.app.utils.UiColors;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The strip under a savings calendar that appears once missed days are picked: how many, what they cost, Pay and Clear.
 * The cost is the daily amount times the days picked, which is exactly what the node charges.
 */
public final class BackpayBar {
    private BackpayBar() {}

    public interface Pay { void pay(List<String> isoDates); }

    /** Builds the bar, hooks it to the selection and returns it (hidden until something is picked). */
    public static LinearLayout create(Context c, GroupTypeBoardView.Selection sel, double dailyHarv, Pay pay) {
        LinearLayout bar = new LinearLayout(c);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        int p = dp(c, 12);
        bar.setPadding(p, p, p, p);
        android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
        g.setCornerRadius(dp(c, 14));
        g.setColor((UiColors.accent() & 0x00FFFFFF) | 0x26000000);
        g.setStroke(dp(c, 1), (UiColors.accent() & 0x00FFFFFF) | 0x88000000);
        bar.setBackground(g);
        LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        barLp.topMargin = dp(c, 10);
        bar.setLayoutParams(barLp);
        bar.setVisibility(View.GONE);

        LinearLayout text = new LinearLayout(c);
        text.setOrientation(LinearLayout.VERTICAL);
        final TextView title = new TextView(c);
        title.setTextSize(14);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(UiColors.textPrimary());
        final TextView sub = new TextView(c);
        sub.setTextSize(12);
        sub.setTextColor(UiColors.textSecondary());
        text.addView(title);
        text.addView(sub);
        bar.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        MaterialButton clear = new MaterialButton(c, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        clear.setText("Clear");
        clear.setAllCaps(false);
        clear.setOnClickListener(v -> { sel.clear(); if (sel.onChange != null) sel.onChange.run(); });
        LinearLayout.LayoutParams cl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cl.setMarginEnd(dp(c, 8));
        bar.addView(clear, cl);
        MaterialButton go = new MaterialButton(c);
        go.setText("Pay");
        go.setAllCaps(false);
        go.setOnClickListener(v -> { if (!sel.picked.isEmpty()) pay.pay(new ArrayList<>(sel.picked)); });
        bar.addView(go);

        final Runnable previous = sel.onChange;
        sel.onChange = () -> {
            int n = sel.picked.size();
            bar.setVisibility(n == 0 ? View.GONE : View.VISIBLE);
            title.setText(n + (n == 1 ? " day selected" : " days selected"));
            sub.setText(String.format(Locale.US, "%s HARV to pay them", trim(dailyHarv * n)));
            if (previous != null) previous.run();
        };
        return bar;
    }

    private static String trim(double v) {
        String s = String.format(Locale.US, "%.4f", v);
        s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        return s.isEmpty() ? "0" : s;
    }

    private static int dp(Context c, int v) { return Math.round(v * c.getResources().getDisplayMetrics().density); }
}
