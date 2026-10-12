package com.harvana.app.web3;

import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.animation.DecelerateInterpolator;

/**
 * The address bar's background: a rounded pill that fills from the left with the accent colour while the page loads,
 * behind the address text, then fades out when the page is done.
 */
public final class LoadingPill extends Drawable {

    private final Paint base = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint edge = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF box = new RectF();
    private final float radius;
    private final int fillAlpha;
    private float shown;                // 0..1 of the width
    private float strength = 1f;        // the fill's opacity, faded out at the end
    private ValueAnimator grow, fade;

    public LoadingPill(int baseColor, int accent, float radiusPx) {
        base.setColor(baseColor);
        fillAlpha = 0x3D;
        fill.setColor(accent);
        edge.setColor(accent);
        radius = radiusPx;
    }

    /** A new page: start from a small sliver so the person sees it at once. */
    public void start() {
        cancel();
        strength = 1f;
        shown = 0f;
        to(0.08f, 180);
    }

    /** 0–100 from the WebView. At 100 the fill completes, then fades. */
    public void setProgress(int p) {
        float target = Math.max(0.08f, Math.min(1f, p / 100f));
        if (p >= 100) {
            if (strength < 1f && shown == 0f) return;              // already finished
            to(1f, 160);
            if (fade != null) fade.cancel();
            fade = ValueAnimator.ofFloat(1f, 0f);
            fade.setStartDelay(160);
            fade.setDuration(380);
            fade.addUpdateListener(a -> {
                strength = (float) a.getAnimatedValue();
                if (strength <= 0f) shown = 0f;
                invalidateSelf();
            });
            fade.start();
            return;
        }
        if (fade != null && fade.isRunning()) { fade.cancel(); strength = 1f; }
        if (target > shown) to(target, 260);
    }

    private void to(float target, long ms) {
        if (grow != null) grow.cancel();
        grow = ValueAnimator.ofFloat(shown, target);
        grow.setDuration(ms);
        grow.setInterpolator(new DecelerateInterpolator());
        grow.addUpdateListener(a -> { shown = (float) a.getAnimatedValue(); invalidateSelf(); });
        grow.start();
    }

    private void cancel() {
        if (grow != null) grow.cancel();
        if (fade != null) fade.cancel();
    }

    @Override
    public void draw(Canvas canvas) {
        box.set(getBounds());
        canvas.drawRoundRect(box, radius, radius, base);
        if (shown <= 0f || strength <= 0f) return;
        float right = box.left + box.width() * shown;
        int save = canvas.save();
        canvas.clipRect(box.left, box.top, right, box.bottom);
        fill.setAlpha(Math.round(fillAlpha * strength));
        canvas.drawRoundRect(box, radius, radius, fill);
        // a thin bright line along the bottom of the filled part
        edge.setAlpha(Math.round(0xE6 * strength));
        float h = Math.max(2f, box.height() * 0.06f);
        canvas.clipRect(box.left, box.bottom - h, right, box.bottom);
        canvas.drawRoundRect(box, radius, radius, edge);
        canvas.restoreToCount(save);
    }

    @Override public void setAlpha(int alpha) { base.setAlpha(alpha); invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter cf) { base.setColorFilter(cf); invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
