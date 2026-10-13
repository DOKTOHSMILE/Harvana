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

    /** One fact. Rows are divided by a hairline; each row is res/layout/item_info_row.xml (50dp). */
    public InfoTable row(String label, CharSequence value, int valueColor) {
        return row(0, label, value, valueColor);
    }

    /** A row with an icon before its label (a coin's logo, a calendar, a wallet…); 0 for none. */
    public InfoTable row(int iconRes, String label, CharSequence value) { return row(iconRes, label, value, UiColors.textPrimary()); }

    public InfoTable row(int iconRes, String label, CharSequence value, int valueColor) {
        View r = android.view.LayoutInflater.from(c).inflate(com.harvana.app.R.layout.item_info_row, box, false);
        ((TextView) r.findViewById(com.harvana.app.R.id.info_label)).setText(label);
        TextView v = r.findViewById(com.harvana.app.R.id.info_value);
        v.setText(value == null || value.length() == 0 ? "\u2014" : value);
        v.setTextColor(valueColor);
        if (iconRes != 0) {
            android.widget.ImageView i = r.findViewById(com.harvana.app.R.id.info_icon);
            i.setImageResource(iconRes);
            i.setVisibility(View.VISIBLE);
        }
        addDivided(r);
        return this;
    }

    /** A row whose icon is a drawable (a stable coin's logo is drawn, not a resource). */
    public InfoTable row(android.graphics.drawable.Drawable icon, String label, CharSequence value) {
        row(0, label, value, UiColors.textPrimary());
        View r = box.getChildAt(box.getChildCount() - 1);
        android.widget.ImageView i = r.findViewById(com.harvana.app.R.id.info_icon);
        if (icon != null && i != null) { i.setImageDrawable(icon); i.setVisibility(View.VISIBLE); }
        return this;
    }

    private void addDivided(View r) {
        if (box.getChildCount() > 0 && !(box.getChildAt(box.getChildCount() - 1) instanceof TextView)) {
            View line = new View(c);
            line.setBackgroundColor(UiColors.textTertiary() & 0x22FFFFFF);
            box.addView(line, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, px(1) / 2)));
        }
        box.addView(r);
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
