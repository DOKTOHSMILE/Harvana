package com.harvana.app.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.AttributeSet;
import android.view.ViewParent;

import androidx.appcompat.widget.AppCompatSpinner;
import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.shape.CornerFamily;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.ShapeAppearanceModel;
import com.harvana.app.R;

/**
 * A drop-in Spinner with the Material outlined-field look: rounded outline, surface fill, tall touch target and a rounded
 * popup. Same API as Spinner, so no calling code changes. When the spinner already sits inside a MaterialCardView the card is
 * the outline and the spinner stays borderless.
 */
public class MaterialSpinner extends AppCompatSpinner {
    public MaterialSpinner(Context c) { super(c); init(); }
    public MaterialSpinner(Context c, AttributeSet a) { super(c, a); init(); }
    public MaterialSpinner(Context c, AttributeSet a, int s) { super(c, a, s); init(); }

    private void init() {
        setMinimumHeight(dp(52));
        setPopupBackgroundDrawable(shape(R.color.colorSurface, R.color.colorCardStroke, 12));
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ViewParent p = getParent();
        while (p != null) { if (p instanceof MaterialCardView) return; p = p.getParent(); }
        setBackground(shape(R.color.colorSurface, R.color.colorCardStroke, 12));
        int h = dp(12);
        setPadding(h, getPaddingTop(), h, getPaddingBottom());
    }

    private android.graphics.drawable.Drawable shape(int fill, int stroke, int radiusDp) {
        ShapeAppearanceModel m = ShapeAppearanceModel.builder().setAllCorners(CornerFamily.ROUNDED, dp(radiusDp)).build();
        MaterialShapeDrawable d = new MaterialShapeDrawable(m);
        d.setFillColor(ColorStateList.valueOf(ContextCompat.getColor(getContext(), fill)));
        d.setStroke(dp(1), ContextCompat.getColor(getContext(), stroke));
        return d;
    }

    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density + 0.5f); }
}
