package com.harvana.app.ui;

import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.view.animation.LinearInterpolator;

/** A small rolling ring for buttons that are waiting on a payment. Draws and animates itself. */
public final class SpinnerDrawable extends Drawable implements Animatable {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF box = new RectF();
    private final int size;
    private float angle;
    private ValueAnimator animator;

    public SpinnerDrawable(int sizePx, int color) {
        size = sizePx;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(Math.max(2f, sizePx / 8f));
        paint.setColor(color);
        setBounds(0, 0, sizePx, sizePx);
    }

    public void setColor(int color) { paint.setColor(color); invalidateSelf(); }

    @Override public void draw(Canvas c) {
        float inset = paint.getStrokeWidth();
        box.set(inset, inset, size - inset, size - inset);
        c.drawArc(box, angle, 270f, false, paint);
    }

    @Override public void start() {
        if (animator != null && animator.isRunning()) return;
        animator = ValueAnimator.ofFloat(0f, 360f);
        animator.setDuration(900);
        animator.setInterpolator(new LinearInterpolator());
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.addUpdateListener(a -> { angle = (float) a.getAnimatedValue(); invalidateSelf(); });
        animator.start();
    }

    @Override public void stop() { if (animator != null) { animator.cancel(); animator = null; } }
    @Override public boolean isRunning() { return animator != null && animator.isRunning(); }
    @Override public int getIntrinsicWidth() { return size; }
    @Override public int getIntrinsicHeight() { return size; }
    @Override public void setAlpha(int a) { paint.setAlpha(a); }
    @Override public void setColorFilter(ColorFilter f) { paint.setColorFilter(f); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
