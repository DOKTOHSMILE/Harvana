package com.harvana.app.group;

import android.app.Activity;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.harvana.app.R;

import java.util.Map;

/**
 * Fills one group card (layout group_custom_view): picture with the type ring, name, ADMIN or MEMBER, state, type and
 * frequency, cycle progress, amount per slot, members and the network trust line. The Groups list and the profile's
 * Groups tab both use it, so a group looks the same wherever it is shown.
 */
public final class GroupCard {
    private GroupCard() {}

    public static void bind(Activity a, View view, Map<String, Object> group, String myUid) {
        if (a == null || view == null || group == null) return;
        LinearLayout details = view.findViewById(R.id.linear85);
        ImageView chevron = view.findViewById(R.id.img_hide);
        androidx.cardview.widget.CardView card = view.findViewById(R.id.cardview1);
        if (details != null && chevron != null && card != null) expansion(group, details, chevron, card);

        TextView name = view.findViewById(R.id.name_textview1);
        if (name != null) name.setText(str(group, "groupName", str(group, "name", "Untitled group")));

        ImageView icon = view.findViewById(R.id.iv_group_card_icon);
        if (icon != null) {
            // A coloured ring by group type (blue rotating, green accumulative, purple fixed), with or without a picture.
            com.harvana.app.utils.GroupRing.apply(icon, str(group, "groupType", ""), 1f, 0x00000000);
            String photo = str(group, "groupPhoto", "");
            if (photo.startsWith("http")) {
                icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
                try { com.bumptech.glide.Glide.with(icon.getContext()).load(photo).circleCrop().into(icon); } catch (Exception ignored) { }
            }
        }

        TextView role = view.findViewById(R.id.position_textview2);
        if (role != null) {
            boolean admin = myUid != null && !myUid.isEmpty() && myUid.equals(str(group, "creatorId", ""));
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(30);
            bg.setStroke(2, admin ? 0xFF10B881 : 0xFF13456D);
            bg.setColor(a.getResources().getColor(R.color.darkFaint10));
            role.setBackground(bg);
            role.setText(admin ? "ADMIN" : "MEMBER");
        }

        String state = str(group, "state", "");
        TextView kind = view.findViewById(R.id.type_freq_textview3);
        if (kind != null) kind.setText((state.isEmpty() ? "" : state + " · ") + str(group, "groupType", "—") + " · " + str(group, "frequency", "—"));

        long round = (long) num(group, "currentRound");
        long slots = (long) num(group, "totalSlots");
        if (slots <= 0) slots = 1;
        if (round <= 0) round = 1;
        int progress = (int) Math.min(100, (round * 100.0) / slots);
        TextView cycle = view.findViewById(R.id.current_cycle_textview4);
        if (cycle != null) cycle.setText("Cycle " + round + " of " + slots);
        TextView pct = view.findViewById(R.id.current_cycle_percent_textview5);
        if (pct != null) pct.setText(progress + "%");
        com.google.android.material.progressindicator.LinearProgressIndicator bar = view.findViewById(R.id.current_cycle_progressbar1);
        if (bar != null) bar.setProgress(progress);

        TextView amount = view.findViewById(R.id.amount_per_slot_textview6);
        if (amount != null) amount.setText(com.harvana.app.wallet.BalanceFormatter.smart(num(group, "contributionAmount")) + "/slot");
        TextView members = view.findViewById(R.id.number_of_members_textview7);
        if (members != null) {
            long n = (long) num(group, "membersCount");
            members.setText(n + (n == 1 ? " member" : " members"));
        }

        TextView trust = view.findViewById(R.id.group_trust_score_perc_textview8);
        if (trust != null) {
            String nodeId = com.harvana.app.trust.NetworkTrust.groupKey(group);
            trust.setTag(nodeId);
            trust.setText("Trust …");
            trust.setTextColor(0xFF9E9E9E);
            com.harvana.app.trust.NetworkTrust.group(nodeId, score -> a.runOnUiThread(() -> {
                if (!nodeId.equals(trust.getTag())) return;
                if (!score.published || score.summary.isEmpty()) { trust.setText("Not on the network"); return; }
                trust.setText(trustLine(score));
                if (score.percent >= 0) { trust.setTextColor(com.harvana.app.trust.ProfileTrust.tierColor(score.tier)); return; }
                if ("HIGH_TRUST".equals(score.summary) || score.summary.startsWith("High trust")) trust.setTextColor(0xFF4CAF50);
                else if ("ESTABLISHED".equals(score.summary) || score.summary.startsWith("Established")) trust.setTextColor(0xFFFF9800);
            }));
        }
    }

    /**
     * "Trust 11% · New" rather than the node's raw summary ("11% New · 4 members"): the member count in that summary is
     * the network's own count of wallets, which differs from the members the card already shows, and read as a stray string.
     */
    static String trustLine(com.harvana.app.trust.NetworkTrust.Score score) {
        if (score.percent >= 0) {
            String tier = score.tier == null ? "" : String.valueOf(score.tier).trim();
            if (!tier.isEmpty()) tier = tier.substring(0, 1).toUpperCase(java.util.Locale.US) + tier.substring(1).toLowerCase(java.util.Locale.US).replace('_', ' ');
            return "Trust " + score.percent + "%" + (tier.isEmpty() ? "" : " · " + tier);
        }
        return score.summary;
    }

    private static void expansion(Map<String, Object> group, LinearLayout lin, ImageView chevron, androidx.cardview.widget.CardView card) {
        card.setClipToOutline(true);
        boolean open = Boolean.TRUE.equals(group.get("isExpanded"));
        lin.setVisibility(open ? View.VISIBLE : View.GONE);
        chevron.setRotation(open ? 180f : 0f);
        chevron.setOnClickListener(v -> {
            boolean now = !Boolean.TRUE.equals(group.get("isExpanded"));
            group.put("isExpanded", now);
            lin.setVisibility(now ? View.VISIBLE : View.GONE);
            chevron.setRotation(now ? 180f : 0f);
        });
    }

    private static double num(Map<String, Object> m, String k) {
        Object v = m.get(k);
        if (v instanceof Number) return ((Number) v).doubleValue();
        try { return v == null ? 0 : Double.parseDouble(v.toString()); } catch (Exception e) { return 0; }
    }

    private static String str(Map<String, Object> m, String k, String fallback) {
        Object v = m.get(k);
        if (v == null) return fallback;
        String s = v.toString().trim();
        return s.isEmpty() || "null".equalsIgnoreCase(s) ? fallback : s;
    }
}
