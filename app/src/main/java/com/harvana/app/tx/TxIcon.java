package com.harvana.app.tx;

import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.shape.RelativeCornerSize;

/**
 * Activity rows reuse one icon view. A full-colour logo and a line glyph were leaving that view
 * square on some rows and round on others. Every bind clips the icon, and its bubble, to a circle.
 */
public final class TxIcon {
    private TxIcon() {}

    public static void circle(ImageView iv) {
        if (iv == null) return;
        iv.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View v, android.graphics.Outline o) {
                o.setOval(0, 0, v.getWidth(), v.getHeight());
            }
        });
        iv.setClipToOutline(true);
        if (iv instanceof ShapeableImageView) {
            ShapeableImageView s = (ShapeableImageView) iv;
            s.setShapeAppearanceModel(s.getShapeAppearanceModel().toBuilder()
                    .setAllCornerSizes(new RelativeCornerSize(0.5f)).build());
        }
        if (iv.getParent() instanceof MaterialCardView) {
            ((MaterialCardView) iv.getParent()).setRadius(1000f);
        }
    }
}
