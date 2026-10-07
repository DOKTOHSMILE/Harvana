package com.harvana.app.milestone;

import android.app.Activity;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.harvana.app.utils.UiColors;

import java.util.List;

/**
 * The full award, card by card: flick left and right through the shelf. Each card shows the 3D badge large on its light
 * tier card, the tier, what earns it, the tier ladder and progress, and where the count comes from.
 */
public final class MilestoneViewer {
    private MilestoneViewer() {}

    public static void show(Activity a, List<MilestoneLoader.Item> items, int start, boolean mine) {
        if (items == null || items.isEmpty()) return;
        BottomSheetDialog dlg = new com.harvana.app.ui.AppSheet(a);
        LinearLayout root = new LinearLayout(a);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(UiColors.surface());
        root.setPadding(0, dp(a, 12), 0, dp(a, 20));

        LinearLayout bar = new LinearLayout(a);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(a, 20), 0, dp(a, 12), dp(a, 8));
        TextView title = MilestoneShelf.text(a, mine ? "Your milestones" : "Milestones", 18, true, UiColors.textPrimary());
        bar.addView(title, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView counter = MilestoneShelf.text(a, "", 13, true, UiColors.textSecondary());
        counter.setPadding(0, 0, dp(a, 12), 0);
        bar.addView(counter);
        ImageView close = new ImageView(a);
        close.setImageResource(com.harvana.app.R.drawable.ic_close);
        close.setColorFilter(UiColors.textPrimary());
        close.setPadding(dp(a, 10), dp(a, 10), dp(a, 10), dp(a, 10));
        close.setContentDescription("Close");
        close.setOnClickListener(v -> dlg.dismiss());
        bar.addView(close, new LinearLayout.LayoutParams(dp(a, 44), dp(a, 44)));
        root.addView(bar);

        ViewPager2 pager = new ViewPager2(a);
        pager.setOffscreenPageLimit(2);
        RecyclerView rv = (RecyclerView) pager.getChildAt(0);
        rv.setPadding(dp(a, 36), 0, dp(a, 36), 0);
        rv.setClipToPadding(false);
        rv.setOverScrollMode(View.OVER_SCROLL_NEVER);
        // Neighbouring cards peek in, shrink and turn a little: a flick through a row of awards.
        pager.setPageTransformer((page, pos) -> {
            float p = Math.max(-1f, Math.min(1f, pos));
            page.setScaleY(1f - 0.08f * Math.abs(p));
            page.setScaleX(1f - 0.04f * Math.abs(p));
            page.setRotationY(-14f * p);
            page.setAlpha(1f - 0.35f * Math.abs(p));
        });
        pager.setAdapter(new Adapter(a, items, mine));
        root.addView(pager, new LinearLayout.LayoutParams(-1, 0, 1f));
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override public void onPageSelected(int position) {
                counter.setText((position + 1) + " of " + items.size());
            }
        });
        pager.setCurrentItem(Math.max(0, Math.min(items.size() - 1, start)), false);
        counter.setText((pager.getCurrentItem() + 1) + " of " + items.size());

        TextView hint = MilestoneShelf.text(a, "Swipe to see the next award", 12, false, UiColors.textTertiary());
        hint.setGravity(Gravity.CENTER);
        hint.setPadding(0, dp(a, 10), 0, 0);
        root.addView(hint);

        dlg.setContentView(root);
        com.harvana.app.group.BottomSheetUtils.expandFullScreen(dlg, root);
        dlg.show();
    }

    private static final class Holder extends RecyclerView.ViewHolder {
        Holder(View v) { super(v); }
    }

    private static final class Adapter extends RecyclerView.Adapter<Holder> {
        final Activity a; final List<MilestoneLoader.Item> items; final boolean mine;
        Adapter(Activity a, List<MilestoneLoader.Item> items, boolean mine) { this.a = a; this.items = items; this.mine = mine; }

        @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
            FrameLayout f = new FrameLayout(a);
            f.setLayoutParams(new ViewGroup.LayoutParams(-1, -1));
            f.setPadding(dp(a, 6), dp(a, 8), dp(a, 6), dp(a, 8));
            return new Holder(f);
        }

        @Override public void onBindViewHolder(@NonNull Holder h, int position) {
            FrameLayout f = (FrameLayout) h.itemView;
            f.removeAllViews();
            f.addView(card(items.get(position)), new FrameLayout.LayoutParams(-1, -1));
        }

        @Override public int getItemCount() { return items.size(); }

        @Override public void onViewAttachedToWindow(@NonNull Holder h) {
            View icon = h.itemView.findViewWithTag("badge");
            if (icon == null) return;
            icon.setScaleX(0.7f); icon.setScaleY(0.7f); icon.setRotation(-8f);
            icon.animate().scaleX(1f).scaleY(1f).rotation(0f).setDuration(420)
                    .setInterpolator(new android.view.animation.OvershootInterpolator(2.4f)).start();
        }

        /** One full award card. */
        View card(MilestoneLoader.Item it) {
            android.widget.ScrollView scroll = new android.widget.ScrollView(a);
            scroll.setFillViewport(true);
            LinearLayout c = new LinearLayout(a);
            c.setOrientation(LinearLayout.VERTICAL);
            c.setGravity(Gravity.CENTER_HORIZONTAL);
            c.setPadding(dp(a, 22), dp(a, 28), dp(a, 22), dp(a, 22));
            GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{0xFFFFFFFF, MilestoneCatalog.tierBackground(it.def, it.tier)});
            bg.setCornerRadius(dp(a, 28));
            bg.setStroke(dp(a, 1), (MilestoneCatalog.tierInk(it.def, it.tier) & 0x00FFFFFF) | 0x40000000);
            c.setBackground(bg);
            c.setElevation(dp(a, 6));
            int ink = MilestoneCatalog.tierInk(it.def, it.tier);

            ImageView icon = new ImageView(a);
            icon.setTag("badge");
            icon.setImageResource(it.def.icon);
            if (it.tier == 0) MilestoneShelf.greyOut(icon);
            c.addView(icon, new LinearLayout.LayoutParams(dp(a, 150), dp(a, 150)));

            TextView name = MilestoneShelf.text(a, it.def.name, 24, true, MilestoneCatalog.CARD_TEXT);
            name.setGravity(Gravity.CENTER);
            name.setPadding(0, dp(a, 14), 0, 0);
            c.addView(name);

            TextView tier = MilestoneShelf.text(a, MilestoneCatalog.tierName(it.def, it.tier), 13, true, ink);
            tier.setPadding(dp(a, 14), dp(a, 5), dp(a, 14), dp(a, 5));
            GradientDrawable pill = new GradientDrawable();
            pill.setCornerRadius(dp(a, 20));
            pill.setColor((ink & 0x00FFFFFF) | 0x1F000000);
            tier.setBackground(pill);
            LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(-2, -2);
            tl.topMargin = dp(a, 8);
            c.addView(tier, tl);

            TextView earn = MilestoneShelf.text(a, it.def.earn, 15, false, MilestoneCatalog.CARD_MUTED);
            earn.setGravity(Gravity.CENTER);
            earn.setPadding(0, dp(a, 14), 0, dp(a, 10));
            c.addView(earn);

            if (!it.def.single()) {
                String[] names = {"Bronze", "Silver", "Gold"};
                for (int i = 0; i < it.def.tiers.length; i++) {
                    boolean got = it.tier > i;
                    LinearLayout row = new LinearLayout(a);
                    row.setGravity(Gravity.CENTER_VERTICAL);
                    row.setPadding(dp(a, 12), dp(a, 8), dp(a, 12), dp(a, 8));
                    GradientDrawable rb = new GradientDrawable();
                    rb.setCornerRadius(dp(a, 12));
                    rb.setColor(got ? (MilestoneCatalog.tierBackground(it.def, i + 1) & 0x00FFFFFF) | 0xFF000000 : 0x14000000);
                    row.setBackground(rb);
                    android.widget.TextView tierName = MilestoneShelf.text(a, names[i], 14, true,
                            got ? MilestoneCatalog.tierInk(it.def, i + 1) : MilestoneCatalog.CARD_MUTED);
                    if (got) com.harvana.app.utils.Icons.checked(tierName, names[i]);     // a real check icon, not a text tick
                    row.addView(tierName, new LinearLayout.LayoutParams(0, -2, 1f));
                    row.addView(MilestoneShelf.text(a, it.def.tiers[i] + " " + it.def.unit, 13, false, MilestoneCatalog.CARD_MUTED));
                    LinearLayout.LayoutParams rl = new LinearLayout.LayoutParams(-1, -2);
                    rl.topMargin = dp(a, 6);
                    c.addView(row, rl);
                }
                if (mine || it.tier > 0) {
                    long next = it.nextTarget();
                    long prevT = it.tier > 0 ? it.def.tiers[it.tier - 1] : 0;
                    LinearProgressIndicator bar = new LinearProgressIndicator(a);
                    bar.setMax(1000);
                    bar.setProgressCompat(next == 0 ? 1000 : (int) Math.max(0, Math.min(1000, (it.count - prevT) * 1000 / Math.max(1, next - prevT))), false);
                    bar.setIndicatorColor(ink);
                    bar.setTrackColor(0x22000000);
                    bar.setTrackCornerRadius(dp(a, 4));
                    bar.setTrackThickness(dp(a, 8));
                    LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams(-1, -2);
                    bl.topMargin = dp(a, 16);
                    c.addView(bar, bl);
                    String now = (mine ? "You have " : "") + it.count + " " + it.def.unit + (mine ? "." : " so far.");
                    if (mine && next > 0) now += " " + (next - it.count) + " more for " + names[it.tier] + ".";
                    if (next == 0) now += " Top tier reached.";
                    TextView progress = MilestoneShelf.text(a, now, 14, true, MilestoneCatalog.CARD_TEXT);
                    progress.setGravity(Gravity.CENTER);
                    progress.setPadding(0, dp(a, 8), 0, 0);
                    c.addView(progress);
                }
            }

            String from;
            switch (it.def.source) {
                case LEARN: from = "Counted from Learn Hub posts, handshakes, comments and views. Shown on the public profile."; break;
                case PHONE: from = "Counted on this phone only. Only you can see it, and it does not change the trust score."; break;
                default: from = "Counted by the network from confirmed activity. Shown on the public profile.";
            }
            TextView source = MilestoneShelf.text(a, from, 12, false, MilestoneCatalog.CARD_MUTED);
            source.setGravity(Gravity.CENTER);
            source.setPadding(0, dp(a, 18), 0, 0);
            c.addView(source);
            scroll.addView(c, new ViewGroup.LayoutParams(-1, -2));
            return scroll;
        }
    }

    static int dp(Activity a, int v) { return MilestoneShelf.dp(a, v); }
}
