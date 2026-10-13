package com.harvana.app.trust;

import android.app.Activity;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.harvana.app.R;
import com.harvana.app.utils.UiColors;

/**
 * The trust tiers as medals: New, Bronze, Silver, Gold, Platinum (the node's TrustPercent thresholds). A tier chip shows the
 * medal beside the percentage; tapping it opens every tier, the ones reached in colour and the rest dimmed with what they need.
 */
public final class TierBadges {
    private TierBadges() {}

    public static final String[] TIERS = {"New", "Bronze", "Silver", "Gold", "Platinum"};
    private static final int[] FROM = {0, 0, 40, 65, 85};
    private static final int[] ICONS = {R.drawable.tier_new, R.drawable.tier_bronze, R.drawable.tier_silver, R.drawable.tier_gold, R.drawable.tier_platinum};
    private static final String[] MEANS = {
        "Just joined. Your score starts with your first confirmed action.",
        "On the network and building a record.",
        "A steady record: payments on time and a bound phone.",
        "Trusted by many: a long, clean record and people who vouch for you.",
        "The top tier: an outstanding record over a long time, with strong reliability and conduct."
    };

    public static int index(String tier) {
        for (int i = 0; i < TIERS.length; i++) if (TIERS[i].equalsIgnoreCase(tier == null ? "" : tier.trim())) return i;
        return 0;
    }

    public static int icon(String tier) { return ICONS[index(tier)]; }

    /** Puts the tier's medal at the start of a chip and makes the chip open the tiers dialog. */
    public static void decorate(Activity a, TextView chip, String tier, int percent, boolean scored) {
        Drawable d = ContextCompat.getDrawable(a, icon(scored ? tier : "New"));
        if (d != null) {
            int px = dp(a, 20);
            d.setBounds(0, 0, px, px);
            chip.setCompoundDrawablesRelative(d, null, null, null);
            chip.setCompoundDrawablePadding(dp(a, 6));
        }
        chip.setClickable(true);
        chip.setContentDescription((scored ? tier : "New") + " tier. Tap to see every tier.");
        chip.setOnClickListener(v -> show(a, scored ? tier : "New", scored ? percent : -1));
    }

    /** Every tier, reached ones in colour, the others dimmed. {@code percent} is -1 when there is no score yet. */
    public static void show(Activity a, String tier, int percent) {
        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(a, 20), dp(a, 4), dp(a, 20), 0);

        int at = index(tier);
        ImageView hero = new ImageView(a);
        hero.setImageResource(ICONS[at]);
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(dp(a, 96), dp(a, 96));
        hlp.gravity = Gravity.CENTER_HORIZONTAL;
        box.addView(hero, hlp);
        TextView title = text(a, TIERS[at] + (percent >= 0 ? " · " + percent + "%" : ""), 22, true, UiColors.textPrimary());
        title.setGravity(Gravity.CENTER);
        box.addView(title);
        String next = at + 1 < TIERS.length && percent >= 0
                ? (FROM[at + 1] - percent > 0 ? (FROM[at + 1] - percent) + " points to " + TIERS[at + 1] : "Almost " + TIERS[at + 1])
                : at == TIERS.length - 1 ? "The top tier" : "Your score appears after your first confirmed action";
        TextView sub = text(a, next, 13, false, UiColors.textSecondary());
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, dp(a, 2), 0, dp(a, 12));
        box.addView(sub);

        LinearLayout list = new LinearLayout(a);
        list.setOrientation(LinearLayout.VERTICAL);
        for (int i = 0; i < TIERS.length; i++) {
            boolean reached = i <= at;
            LinearLayout row = new LinearLayout(a);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(a, 10), dp(a, 8), dp(a, 10), dp(a, 8));
            GradientDrawable rb = new GradientDrawable();
            rb.setCornerRadius(dp(a, 16));
            rb.setColor(i == at ? (UiColors.accent() & 0x00FFFFFF) | 0x1F000000 : UiColors.surfaceAlt());
            row.setBackground(rb);
            ImageView m = new ImageView(a);
            m.setImageResource(ICONS[i]);
            if (!reached) dim(m);
            row.addView(m, new LinearLayout.LayoutParams(dp(a, 48), dp(a, 48)));
            LinearLayout words = new LinearLayout(a);
            words.setOrientation(LinearLayout.VERTICAL);
            words.setPadding(dp(a, 12), 0, 0, 0);
            words.addView(text(a, TIERS[i] + (i >= 2 ? "  ·  from " + FROM[i] + "%" : ""), 15, true,
                    reached ? UiColors.textPrimary() : UiColors.textTertiary()));
            words.addView(text(a, reached ? MEANS[i] : "Locked. " + MEANS[i], 12, false, reached ? UiColors.textSecondary() : UiColors.textTertiary()));
            row.addView(words, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            if (i == at) row.addView(text(a, "You", 12, true, UiColors.accent()));
            LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rlp.topMargin = dp(a, 8);
            list.addView(row, rlp);
        }
        box.addView(list);
        new com.harvana.app.ui.SheetDialog.Builder(a).setView(box).setPositiveButton("Close", null).show();
    }

    /** Not reached yet: grey and faded. */
    static void dim(ImageView v) {
        ColorMatrix m = new ColorMatrix();
        m.setSaturation(0f);
        v.setColorFilter(new ColorMatrixColorFilter(m));
        v.setAlpha(0.4f);
    }

    private static TextView text(Activity a, String s, float sp, boolean bold, int color) {
        TextView t = new TextView(a);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(null, Typeface.BOLD);
        return t;
    }

    private static int dp(Activity a, int v) { return Math.round(v * a.getResources().getDisplayMetrics().density); }
}
