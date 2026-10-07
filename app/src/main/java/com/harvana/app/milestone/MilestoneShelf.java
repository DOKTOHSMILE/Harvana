package com.harvana.app.milestone;

import android.app.Activity;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;


import com.harvana.app.utils.UiColors;

import java.util.ArrayList;
import java.util.List;

/**
 * The milestone shelf: badges grouped by family, three to a row, each row standing on a shelf. Earned badges glow in their
 * tier colour; on the owner's shelf locked ones show greyed with their progress. Tap a badge for how to earn it and its tiers.
 */
public final class MilestoneShelf {
    private MilestoneShelf() {}

    private static final int COLUMNS = 3;

    /** A card holding the shelf. {@code mine}: show locked badges and progress; otherwise only earned badges. */
    public static View card(Activity a, List<MilestoneLoader.Item> items, boolean mine) {
        LinearLayout c = new LinearLayout(a);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(a, 16), dp(a, 16), dp(a, 16), dp(a, 12));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(a, 18));
        bg.setColor(UiColors.surface());
        bg.setStroke(dp(a, 1), (UiColors.textTertiary() & 0x00FFFFFF) | 0x33000000);
        c.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(a, 12);
        c.setLayoutParams(lp);

        int earned = 0, total = 0;
        for (MilestoneLoader.Item it : items) { total++; if (it.tier > 0) earned++; }
        LinearLayout head = new LinearLayout(a);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.addView(text(a, "Milestones", 16, true, UiColors.textPrimary()), new LinearLayout.LayoutParams(0, -2, 1f));
        TextView chip = text(a, earned + " of " + total + " earned", 12, true, UiColors.accent());
        chip.setPadding(dp(a, 10), dp(a, 4), dp(a, 10), dp(a, 4));
        GradientDrawable cg = new GradientDrawable();
        cg.setCornerRadius(dp(a, 20));
        cg.setColor((UiColors.accent() & 0x00FFFFFF) | 0x22000000);
        chip.setBackground(cg);
        head.addView(chip);
        c.addView(head);
        TextView sub = text(a, mine
                ? "Earned from what you do. Network and Learn Hub milestones show on your public profile; your habits are only for you."
                : "Earned from what they have done on the network and on Learn Hub.", 12, false, UiColors.textSecondary());
        sub.setPadding(0, dp(a, 4), 0, dp(a, 4));
        c.addView(sub);

        boolean any = false;
        // The order the full-size viewer flicks through: the same badges, in shelf order.
        final List<MilestoneLoader.Item> shown = new ArrayList<>();
        for (String family : MilestoneCatalog.families()) {
            List<MilestoneLoader.Item> fam = new ArrayList<>();
            for (MilestoneLoader.Item it : items) if (it.def.family.equals(family) && (mine || it.tier > 0)) fam.add(it);
            if (mine) fam.sort((x, y) -> Integer.compare(y.tier, x.tier));
            shown.addAll(fam);
        }
        for (String family : MilestoneCatalog.families()) {
            List<MilestoneLoader.Item> row = new ArrayList<>();
            for (MilestoneLoader.Item it : items) if (it.def.family.equals(family) && (mine || it.tier > 0)) row.add(it);
            if (row.isEmpty()) continue;
            any = true;
            if (mine) row.sort((x, y) -> Integer.compare(y.tier, x.tier));
            TextView label = text(a, family.toUpperCase(java.util.Locale.US), 11, true, UiColors.textTertiary());
            label.setLetterSpacing(0.06f);
            label.setPadding(dp(a, 2), dp(a, 16), 0, dp(a, 6));
            c.addView(label);
            for (int i = 0; i < row.size(); i += COLUMNS) {
                LinearLayout line = new LinearLayout(a);
                line.setBaselineAligned(false);
                for (int j = 0; j < COLUMNS; j++) {
                    LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                    tl.setMargins(dp(a, 4), dp(a, 2), dp(a, 4), dp(a, 4));
                    if (i + j < row.size()) line.addView(tile(a, shown, row.get(i + j), mine), tl);
                    else line.addView(new View(a), tl);
                }
                c.addView(line);
                c.addView(shelf(a));
            }
        }
        if (!any) {
            TextView none = text(a, mine ? "Your first milestones come with your first payments, votes and posts."
                    : "No milestones yet.", 13, false, UiColors.textSecondary());
            none.setPadding(0, dp(a, 12), 0, dp(a, 6));
            c.addView(none);
        }
        return c;
    }

    /**
     * The short form for profiles and the trust page: only the milestones reached, in one row that scrolls sideways,
     * highest tier first. Tap a badge to flick through them full size. On your own, "See all" opens the whole shelf
     * with what is still to earn.
     */
    public static View strip(Activity a, List<MilestoneLoader.Item> items, boolean mine) {
        LinearLayout c = new LinearLayout(a);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(a, 16), dp(a, 14), dp(a, 16), dp(a, 14));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(a, 18));
        bg.setColor(UiColors.surface());
        bg.setStroke(dp(a, 1), (UiColors.textTertiary() & 0x00FFFFFF) | 0x33000000);
        c.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(a, 12);
        c.setLayoutParams(lp);

        final List<MilestoneLoader.Item> earned = new ArrayList<>();
        for (MilestoneLoader.Item it : items) if (it.tier > 0) earned.add(it);
        earned.sort((x, y) -> Integer.compare(y.tier, x.tier));

        LinearLayout head = new LinearLayout(a);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.addView(text(a, "Milestones reached", 16, true, UiColors.textPrimary()), new LinearLayout.LayoutParams(0, -2, 1f));
        if (mine) {
            TextView all = text(a, "See all " + items.size() + "  \u203A", 13, true, UiColors.accent());
            all.setPadding(dp(a, 8), dp(a, 4), 0, dp(a, 4));
            all.setOnClickListener(v -> showAll(a, items));
            head.addView(all);
        } else {
            head.addView(text(a, String.valueOf(earned.size()), 14, true, UiColors.accent()));
        }
        c.addView(head);

        if (earned.isEmpty()) {
            TextView none = text(a, mine ? "None yet. Your first come with your first payments, votes and posts."
                    : "No milestones reached yet.", 13, false, UiColors.textSecondary());
            none.setPadding(0, dp(a, 8), 0, 0);
            c.addView(none);
            return c;
        }
        android.widget.HorizontalScrollView scroll = new android.widget.HorizontalScrollView(a);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setClipToPadding(false);
        LinearLayout row = new LinearLayout(a);
        row.setPadding(0, dp(a, 10), 0, dp(a, 4));
        for (MilestoneLoader.Item it : earned) {
            LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(dp(a, 96), ViewGroup.LayoutParams.WRAP_CONTENT);
            tl.setMargins(0, dp(a, 2), dp(a, 8), dp(a, 4));
            row.addView(tile(a, earned, it, mine), tl);
        }
        scroll.addView(row);
        c.addView(scroll);
        return c;
    }

    /** The whole shelf (every family, locked ones with progress) in a full-height sheet. */
    public static void showAll(Activity a, List<MilestoneLoader.Item> items) {
        com.google.android.material.bottomsheet.BottomSheetDialog dlg = new com.harvana.app.ui.AppSheet(a);
        android.widget.ScrollView sv = new android.widget.ScrollView(a);
        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(a, 12), dp(a, 4), dp(a, 12), dp(a, 24));
        box.addView(card(a, items, true));
        sv.addView(box);
        dlg.setContentView(sv);
        com.harvana.app.group.BottomSheetUtils.expandFullScreen(dlg, sv);
        dlg.show();
    }

    /** One badge: the 3D badge on a light card in its tier colour, its name, and its tier or progress. Greyed while locked. */
    private static View tile(Activity a, List<MilestoneLoader.Item> all, MilestoneLoader.Item it, boolean mine) {
        LinearLayout t = new LinearLayout(a);
        t.setOrientation(LinearLayout.VERTICAL);
        t.setGravity(Gravity.CENTER_HORIZONTAL);
        t.setPadding(dp(a, 6), dp(a, 10), dp(a, 6), dp(a, 10));
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xFFFFFFFF, MilestoneCatalog.tierBackground(it.def, it.tier)});
        bg.setCornerRadius(dp(a, 16));
        bg.setStroke(dp(a, 1), (MilestoneCatalog.tierInk(it.def, it.tier) & 0x00FFFFFF) | 0x33000000);
        t.setBackground(bg);
        t.setElevation(dp(a, it.tier > 0 ? 3 : 0));
        t.setClickable(true);
        t.setFocusable(true);
        android.util.TypedValue tv = new android.util.TypedValue();
        a.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, tv, true);
        t.setForeground(androidx.core.content.ContextCompat.getDrawable(a, tv.resourceId));

        ImageView icon = new ImageView(a);
        icon.setImageResource(it.def.icon);
        if (it.tier == 0) greyOut(icon);
        t.addView(icon, new LinearLayout.LayoutParams(dp(a, 52), dp(a, 52)));

        TextView name = text(a, it.def.name, 12, true, it.tier > 0 ? MilestoneCatalog.CARD_TEXT : MilestoneCatalog.CARD_MUTED);
        name.setGravity(Gravity.CENTER);
        name.setMaxLines(2);
        name.setPadding(0, dp(a, 6), 0, 0);
        t.addView(name);

        String status = it.tier > 0 ? MilestoneCatalog.tierName(it.def, it.tier)
                : mine && !it.def.single() ? it.count + " / " + it.def.tiers[0] : "Not yet";
        TextView st = text(a, status, 11, true, MilestoneCatalog.tierInk(it.def, it.tier));
        st.setGravity(Gravity.CENTER);
        st.setPadding(0, dp(a, 2), 0, 0);
        t.addView(st);
        t.setContentDescription(it.def.name + ", " + status);
        t.setOnClickListener(v -> MilestoneViewer.show(a, all, all.indexOf(it), mine));
        return t;
    }

    /** Locked badges show in grey, faded. */
    static void greyOut(ImageView icon) {
        android.graphics.ColorMatrix m = new android.graphics.ColorMatrix();
        m.setSaturation(0f);
        icon.setColorFilter(new android.graphics.ColorMatrixColorFilter(m));
        icon.setAlpha(0.5f);
    }

    /** The plank a row of badges stands on. */
    private static View shelf(Activity a) {
        View plank = new View(a);
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{(UiColors.accent() & 0x00FFFFFF) | 0x40000000, (UiColors.accent() & 0x00FFFFFF) | 0x0A000000});
        g.setCornerRadius(dp(a, 3));
        plank.setBackground(g);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(a, 5));
        lp.setMargins(dp(a, 2), 0, dp(a, 2), dp(a, 6));
        plank.setLayoutParams(lp);
        return plank;
    }

    static TextView text(Activity a, String s, float sp, boolean bold, int color) {
        TextView t = new TextView(a);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(null, Typeface.BOLD);
        return t;
    }

    static int dp(Activity a, int v) { return Math.round(v * a.getResources().getDisplayMetrics().density); }
}
