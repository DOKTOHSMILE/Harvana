package com.harvana.app.utils;

import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.shape.ShapeAppearanceModel;

/**
 * Icons sit whole and centred in their bubble. A tinted icon (a vector, not a photo) is shown at its own size or smaller,
 * never cropped: no "center" crop of a large vector, and no doubled padding (ShapeableImageView counts android:padding as
 * content padding as well, which pushed the icon against the shape's edge). A bubble that asks for a fill colour
 * (backgroundTint) gets a round one.
 */
public final class IconFit {
    private IconFit() {}

    /** Fixes every tinted icon under {@code root}. Photos (no tint) are left as they are. */
    public static void apply(View root) {
        if (root == null) return;
        if (root instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) root;
            for (int i = 0; i < g.getChildCount(); i++) apply(g.getChildAt(i));
            return;
        }
        if (!(root instanceof ImageView)) return;
        ImageView iv = (ImageView) root;
        if (iv.getImageTintList() == null) return;                 // a photo or a coloured coin: its own rules
        if (iv.getTag(com.harvana.app.R.id.icon_fit_done) != null) return;
        iv.setTag(com.harvana.app.R.id.icon_fit_done, Boolean.TRUE);

        float d = iv.getResources().getDisplayMetrics().density;
        ViewGroup.LayoutParams lp = iv.getLayoutParams();
        int size = lp == null ? 0 : Math.min(lp.width, lp.height);
        boolean bubble = iv.getBackgroundTintList() != null && size > (int) (28 * d);

        if (iv instanceof ShapeableImageView) {
            ShapeableImageView s = (ShapeableImageView) iv;
            s.setContentPadding(0, 0, 0, 0);
            if (bubble) s.setShapeAppearanceModel(ShapeAppearanceModel.builder().setAllCornerSizes(size / 2f).build());
        }
        // The icon takes about half the bubble, the rest is even space around it.
        int inset = bubble ? Math.max(0, (int) (size * 0.24f)) : 0;
        iv.setPadding(inset, inset, inset, inset);
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        iv.setAdjustViewBounds(false);

        if (bubble) {
            ColorStateList fill = iv.getBackgroundTintList();
            GradientDrawable disc = new GradientDrawable();
            disc.setShape(GradientDrawable.OVAL);
            disc.setColor(fill);
            iv.setBackgroundTintList(null);
            iv.setBackground(disc);
        }
    }
}
