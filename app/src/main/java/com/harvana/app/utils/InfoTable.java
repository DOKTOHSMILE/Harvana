package com.harvana.app.utils;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Important figures as a table: one row per fact, its name on the left and its value on the right, grouped under small
 * section titles. Used wherever the app used to explain numbers in a sentence (swap details, staking estimates, swap
 * records), so a figure is always found in the same place and never buried in text.
 *
 *   InfoTable t = new InfoTable(context);
 *   t.section("This swap");
 *   t.row("You pay", "1,000 hNGN");
 *   t.row("Price impact", "0.63%", UiColors.warning());
 *   parent.addView(t.view());
 */
public final class InfoTable {

    private final Context c;
    private final LinearLayout box;
    private final float d;

    public InfoTable(Context c) {
        this.c = c;
        this.d = c.getResources().getDisplayMetrics().density;
        box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(px(14), px(6), px(14), px(6));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(px(14));
        bg.setColor(UiColors.surface());
        box.setBackground(bg);
    }

    public LinearLayout view() { return box; }

    /** Removes every row (to refill it with new figures). */
    public InfoTable clear() { box.removeAllViews(); return this; }

    /** A small title above the rows that follow. */
    public InfoTable section(String title) {
        TextView t = new TextView(c);
        t.setText(title.toUpperCase(java.util.Locale.US));
        t.setTextSize(11f);
        t.setLetterSpacing(0.06f);
        t.setTypeface(null, Typeface.BOLD);
        t.setTextColor(UiColors.textTertiary());
        t.setPadding(0, px(box.getChildCount() == 0 ? 6 : 14), 0, px(4));
        box.addView(t);
        return this;
    }

    public InfoTable row(String label, CharSequence value) { return row(label, value, UiColors.textPrimary()); }

    /** One fact. Rows are divided by a hairline. */
    public InfoTable row(String label, CharSequence value, int valueColor) {
        LinearLayout r = new LinearLayout(c);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setMinimumHeight(px(36));
        r.setPadding(0, px(6), 0, px(6));
        TextView l = new TextView(c);
        l.setText(label);
        l.setTextSize(13f);
        l.setTextColor(UiColors.textSecondary());
        r.addView(l, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView v = new TextView(c);
        v.setText(value == null || value.length() == 0 ? "—" : value);
        v.setTextSize(13.5f);
        v.setTypeface(null, Typeface.BOLD);
        v.setTextColor(valueColor);
        v.setGravity(Gravity.END);
        v.setTextIsSelectable(true);
        r.addView(v, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.4f));
        if (box.getChildCount() > 0 && !(box.getChildAt(box.getChildCount() - 1) instanceof TextView)) {
            View line = new View(c);
            line.setBackgroundColor(UiColors.textTertiary() & 0x22FFFFFF);
            box.addView(line, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, px(1) / 2)));
        }
        box.addView(r);
        return this;
    }

    /** A short note under the rows (only for what is not a figure). */
    public InfoTable note(String text) {
        TextView t = new TextView(c);
        t.setText(text);
        t.setTextSize(11.5f);
        t.setTextColor(UiColors.textTertiary());
        t.setPadding(0, px(8), 0, px(4));
        box.addView(t);
        return this;
    }

    private int px(int dp) { return Math.round(dp * d); }
}
