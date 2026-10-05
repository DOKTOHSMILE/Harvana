package com.harvana.app.trust;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.harvana.app.utils.UiColors;

import java.util.List;

/** Draws the node's named badges as a row of small chips. Tapping a chip says what it means. */
public final class BadgeChips {
    private BadgeChips() {}

    /** Replaces the contents of {@code host} with one chip per badge; hides the host when there are none and {@code hideWhenEmpty}. */
    public static void fill(LinearLayout host, List<NodeBadges.Badge> badges, boolean hideWhenEmpty) {
        Context c = host.getContext();
        host.removeAllViews();
        if (badges.isEmpty()) {
            if (hideWhenEmpty) { host.setVisibility(android.view.View.GONE); return; }
            TextView none = new TextView(c);
            none.setText("No badges yet");
            none.setTextSize(12.5f);
            none.setTextColor(UiColors.textTertiary());
            host.addView(none);
            host.setVisibility(android.view.View.VISIBLE);
            return;
        }
        host.setVisibility(android.view.View.VISIBLE);
        HorizontalScrollView scroll = new HorizontalScrollView(c);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        for (NodeBadges.Badge b : badges) row.addView(chip(c, b));
        scroll.addView(row);
        host.addView(scroll, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private static TextView chip(Context c, NodeBadges.Badge b) {
        float d = c.getResources().getDisplayMetrics().density;
        TextView t = new TextView(c);
        t.setText(b.title);
        t.setTextSize(12f);
        t.setTextColor(UiColors.textPrimary());
        t.setPadding((int) (12 * d), (int) (7 * d), (int) (12 * d), (int) (7 * d));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(40 * d);
        bg.setColor(UiColors.surface());
        bg.setStroke((int) Math.max(1, d), b.permanent ? UiColors.accent() : UiColors.success());
        t.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMarginEnd((int) (8 * d));
        lp.bottomMargin = (int) (2 * d);
        t.setLayoutParams(lp);
        t.setOnClickListener(v -> Toast.makeText(c, b.title + (b.detail.isEmpty() ? "" : ": " + b.detail), Toast.LENGTH_LONG).show());
        return t;
    }
}
