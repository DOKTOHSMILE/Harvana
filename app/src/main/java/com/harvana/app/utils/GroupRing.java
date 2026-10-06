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
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(fill);
        g.setStroke(ring, color(groupType));
        v.setBackground(g);
        int gap = ring + Math.round(1.5f * d);                 // a hair of space so the ring reads as a ring, not an outline of the picture
        v.setPadding(gap, gap, gap, gap);
        v.setContentDescription("Group type: " + (color(groupType) == OTHER ? "other" : colorName(groupType)));
        v.setClipToOutline(true);
        v.setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override public void getOutline(View view, android.graphics.Outline o) { o.setOval(0, 0, view.getWidth(), view.getHeight()); }
        });
    }

    public static void apply(ImageView v, String groupType) { apply(v, groupType, 3f, Color.TRANSPARENT); }
}
