package com.harvana.app.utils;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.ImageView;

import java.util.Locale;

/**
 * The colour ring around a group's picture. Each kind of group has its own colour, so a member can tell the kind from the ring
 * alone, anywhere a group picture appears (lists, the group page, settings, shared groups on a profile):
 * blue is a Rotating Pool, green is Accumulative Funds, purple is a Fixed Contribution goal, grey is anything else.
 */
public final class GroupRing {
    private GroupRing() {}

    /** The ring is 1dp wide everywhere. */
    public static final float RING_DP = 1f;

    public static final int ROTATING = 0xFF3B82F6, ACCUM = 0xFF10B981, FIXED = 0xFFA855F7, OTHER = 0xFF94A3B8;

    public static int color(String groupType) {
        String t = groupType == null ? "" : groupType.toLowerCase(Locale.US);
        if (t.contains("rotat")) return ROTATING;
        if (t.contains("accum")) return ACCUM;
        if (t.contains("fixed")) return FIXED;
        return OTHER;
    }

    /** The plain-English name of the colour, for a caption. */
    public static String colorName(String groupType) {
        int c = color(groupType);
        return c == ROTATING ? "blue" : c == ACCUM ? "green" : c == FIXED ? "purple" : "grey";
    }

    /**
     * Draws the ring on a round picture: a round background with a coloured edge, and padding so the picture sits inside it.
     * The picture itself should be loaded with circleCrop. {@code fill} is the colour behind a transparent or missing picture.
     */
    public static void apply(ImageView v, String groupType, float ringDp, int fill) {
        float d = v.getResources().getDisplayMetrics().density;
        int ring = Math.max(1, Math.round(ringDp * d));
        if (v instanceof com.google.android.material.imageview.ShapeableImageView) {
            // A Material image view wipes everything outside its own shape, padding included, so a ring drawn as a background
            // in the padding would be cut. Use its native round shape and border instead.
            com.google.android.material.imageview.ShapeableImageView sv = (com.google.android.material.imageview.ShapeableImageView) v;
            sv.setBackground(null);
            sv.setShapeAppearanceModel(com.google.android.material.shape.ShapeAppearanceModel.builder()
                .setAllCorners(com.google.android.material.shape.CornerFamily.ROUNDED, new com.google.android.material.shape.RelativeCornerSize(0.5f)).build());
            sv.setStrokeColor(android.content.res.ColorStateList.valueOf(color(groupType)));
            sv.setStrokeWidth(ring);
            int pad = ring + Math.round(1f * d);
            sv.setPadding(pad, pad, pad, pad);
            sv.setContentDescription("Group type: " + (color(groupType) == OTHER ? "other" : colorName(groupType)));
            return;
        }
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(fill);
        g.setStroke(ring, color(groupType));
        v.setBackground(g);
        int gap = ring + Math.round(1f * d);                 // a hair of space so the ring reads as a ring, not an outline of the picture
        v.setPadding(gap, gap, gap, gap);
        v.setContentDescription("Group type: " + (color(groupType) == OTHER ? "other" : colorName(groupType)));
        v.setClipToOutline(true);
        v.setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override public void getOutline(View view, android.graphics.Outline o) { o.setOval(0, 0, view.getWidth(), view.getHeight()); }
        });
    }

    /**
     * Shows a group on a round image: the ring for its type, and its picture when it has one, otherwise {@code placeholderRes}
     * (0 keeps whatever drawable the view already has). Safe on recycled views: a view that showed a picture before is reset.
     */
    public static void show(ImageView v, String groupType, String photoUrl, int placeholderRes) {
        apply(v, groupType, RING_DP, Color.TRANSPARENT);
        try {
            if (photoUrl != null && photoUrl.startsWith("http")) {
                v.setImageTintList(null);
                v.setScaleType(ImageView.ScaleType.CENTER_CROP);
                com.bumptech.glide.Glide.with(v.getContext().getApplicationContext()).load(photoUrl).circleCrop().into(v);
            } else {
                com.bumptech.glide.Glide.with(v.getContext().getApplicationContext()).clear(v);
                if (placeholderRes != 0) v.setImageResource(placeholderRes);
                v.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            }
        } catch (Exception ignored) { }
    }

    public static void apply(ImageView v, String groupType) { apply(v, groupType, RING_DP, Color.TRANSPARENT); }
}
