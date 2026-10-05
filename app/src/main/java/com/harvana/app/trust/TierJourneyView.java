package com.harvana.app.trust;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import com.harvana.app.utils.UiColors;

/**
 * The road from Bronze to Platinum: a track with a tick and name at each tier threshold, a filled stretch up to the current
 * percentage, and a bubble over the knob that says the number. The fill animates in when the score first arrives, so it reads
 * as the next tier loading. Colours come from the theme palette; only the fill and knob take the tier colour.
 */
public class TierJourneyView extends View {
    private static final String[] NAMES = {"Bronze", "Silver", "Gold", "Platinum"};
    private static final int[] AT = {0, 40, 65, 85};

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private int percent = 0;
    private float shown = 0f;
    private int tierColor = 0xFFD08A4E;
    private ValueAnimator animator;

    public TierJourneyView(Context c) { super(c); }
    public TierJourneyView(Context c, AttributeSet a) { super(c, a); }

    public void set(int percent, int tierColor) {
        this.percent = Math.max(0, Math.min(100, percent));
        this.tierColor = tierColor;
        if (animator != null) animator.cancel();
        animator = ValueAnimator.ofFloat(shown, this.percent);
        animator.setDuration(900);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(a -> { shown = (float) a.getAnimatedValue(); invalidate(); });
        animator.start();
    }

    @Override protected void onMeasure(int w, int h) {
        setMeasuredDimension(View.resolveSize(getSuggestedMinimumWidth(), w), Math.round(88 * getResources().getDisplayMetrics().density));
    }

    @Override protected void onDetachedFromWindow() {
        if (animator != null) animator.cancel();
        super.onDetachedFromWindow();
    }

    @Override protected void onDraw(Canvas canvas) {
        float d = getResources().getDisplayMetrics().density;
        float left = 18 * d, right = getWidth() - 18 * d, trackW = right - left;
        float cy = 42 * d, th = 10 * d;

        paint.setStyle(Paint.Style.FILL);
        paint.setColor((UiColors.textTertiary() & 0x00FFFFFF) | 0x33000000);
        rect.set(left, cy - th / 2, right, cy + th / 2);
        canvas.drawRoundRect(rect, th / 2, th / 2, paint);

        float x = left + trackW * shown / 100f;
        paint.setColor(tierColor);
        rect.set(left, cy - th / 2, Math.max(left + th, x), cy + th / 2);
        canvas.drawRoundRect(rect, th / 2, th / 2, paint);

        paint.setTextSize(12 * d);
        for (int i = 0; i < NAMES.length; i++) {
            float tx = left + trackW * AT[i] / 100f;
            boolean reached = shown >= AT[i];
            paint.setColor(reached ? tierColor : (UiColors.textTertiary() & 0x00FFFFFF) | 0x66000000);
            canvas.drawCircle(tx, cy, 3.5f * d, paint);
            paint.setColor(UiColors.textPrimary());
            paint.setTypeface(Typeface.DEFAULT_BOLD);
            paint.setTextAlign(i == 0 ? Paint.Align.LEFT : i == NAMES.length - 1 ? Paint.Align.RIGHT : Paint.Align.CENTER);
            float lx = i == 0 ? left - 4 * d : i == NAMES.length - 1 ? right + 4 * d : tx;
            canvas.drawText(NAMES[i], lx, cy + 26 * d, paint);
            paint.setTypeface(Typeface.DEFAULT);
            paint.setColor(UiColors.textSecondary());
            paint.setTextSize(11 * d);
            canvas.drawText(AT[i] + "%", lx, cy + 42 * d, paint);
            paint.setTextSize(12 * d);
        }

        // knob
        paint.setColor(UiColors.surface());
        canvas.drawCircle(x, cy, 11 * d, paint);
        paint.setColor(tierColor);
        canvas.drawCircle(x, cy, 8 * d, paint);

        // bubble
        String label = Math.round(shown) + "%";
        paint.setTextSize(12 * d);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        float bw = paint.measureText(label) + 16 * d, bh = 22 * d;
        float bx = Math.max(bw / 2, Math.min(getWidth() - bw / 2, x));
        rect.set(bx - bw / 2, 2 * d, bx + bw / 2, 2 * d + bh);
        paint.setColor(tierColor);
        canvas.drawRoundRect(rect, bh / 2, bh / 2, paint);
        paint.setColor(UiColors.onAccent());
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(label, bx, 2 * d + bh / 2 + 4 * d, paint);
        paint.setTypeface(Typeface.DEFAULT);
        paint.setTextAlign(Paint.Align.LEFT);
    }
}
