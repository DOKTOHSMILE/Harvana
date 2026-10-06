package com.harvana.app.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

/**
 * Shows a picture under a fixed-shape frame. The person drags the picture and pinches to zoom until the part they want is inside the frame;
 * {@link #crop} then returns exactly that part. The frame keeps the chosen shape (1:1 for a profile picture, wide for a cover) and the picture
 * can never be moved so far that an empty edge shows inside the frame.
 */
public class CropView extends View {
    private Bitmap bitmap;
    private final Matrix matrix = new Matrix();
    private final RectF frame = new RectF();
    private float aspect = 1f;          // frame width / frame height
    private boolean circleGuide;
    private float minScale = 1f, maxScale = 8f;
    private boolean ready;

    private final Paint dim = new Paint();
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint thin = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pic = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final ScaleGestureDetector scaler;
    private final GestureDetector mover;
    private final float[] v = new float[9];

    public CropView(Context c) {
        super(c);
        float d = c.getResources().getDisplayMetrics().density;
        dim.setColor(0xB3000000);
        line.setColor(Color.WHITE); line.setStyle(Paint.Style.STROKE); line.setStrokeWidth(2 * d);
        thin.setColor(0x66FFFFFF); thin.setStyle(Paint.Style.STROKE); thin.setStrokeWidth(1 * d);
        scaler = new ScaleGestureDetector(c, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override public boolean onScale(ScaleGestureDetector s) {
                if (!ready) return true;
                float now = scale();
                float f = Math.max(minScale / now, Math.min(maxScale / now, s.getScaleFactor()));
                matrix.postScale(f, f, s.getFocusX(), s.getFocusY());
                clamp();
                invalidate();
                return true;
            }
        });
        mover = new GestureDetector(c, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onScroll(MotionEvent a, MotionEvent b, float dx, float dy) {
                if (!ready || scaler.isInProgress()) return true;
                matrix.postTranslate(-dx, -dy);
                clamp();
                invalidate();
                return true;
            }
        });
    }

    /** @param aspect frame width divided by frame height; @param circle draw a round guide inside the frame (for profile pictures) */
    public void setup(Bitmap bmp, float aspect, boolean circle) {
        this.bitmap = bmp;
        this.aspect = aspect;
        this.circleGuide = circle;
        this.ready = false;
        layoutFrame();
        invalidate();
    }

    @Override protected void onSizeChanged(int w, int h, int ow, int oh) { super.onSizeChanged(w, h, ow, oh); layoutFrame(); }

    private void layoutFrame() {
        int w = getWidth(), h = getHeight();
        if (w == 0 || h == 0 || bitmap == null) return;
        float pad = 20 * getResources().getDisplayMetrics().density;
        float fw = w - 2 * pad, fh = fw / aspect;
        if (fh > h - 2 * pad) { fh = h - 2 * pad; fw = fh * aspect; }
        frame.set((w - fw) / 2f, (h - fh) / 2f, (w + fw) / 2f, (h + fh) / 2f);
        minScale = Math.max(frame.width() / bitmap.getWidth(), frame.height() / bitmap.getHeight());
        maxScale = minScale * 8f;
        matrix.reset();
        matrix.postScale(minScale, minScale);
        matrix.postTranslate(frame.centerX() - bitmap.getWidth() * minScale / 2f, frame.centerY() - bitmap.getHeight() * minScale / 2f);
        ready = true;
    }

    private float scale() { matrix.getValues(v); return v[Matrix.MSCALE_X]; }

    /** Keeps the picture covering the whole frame. */
    private void clamp() {
        RectF r = new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight());
        matrix.mapRect(r);
        float dx = 0, dy = 0;
        if (r.left > frame.left) dx = frame.left - r.left;
        if (r.right < frame.right) dx = frame.right - r.right;
        if (r.top > frame.top) dy = frame.top - r.top;
        if (r.bottom < frame.bottom) dy = frame.bottom - r.bottom;
        matrix.postTranslate(dx, dy);
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        getParent().requestDisallowInterceptTouchEvent(true);
        scaler.onTouchEvent(e);
        mover.onTouchEvent(e);
        return true;
    }

    @Override protected void onDraw(Canvas c) {
        if (bitmap == null || !ready) return;
        c.drawBitmap(bitmap, matrix, pic);
        int w = getWidth(), h = getHeight();
        c.drawRect(0, 0, w, frame.top, dim);
        c.drawRect(0, frame.bottom, w, h, dim);
        c.drawRect(0, frame.top, frame.left, frame.bottom, dim);
        c.drawRect(frame.right, frame.top, w, frame.bottom, dim);
        c.drawRect(frame, line);
        for (int i = 1; i <= 2; i++) {
            c.drawLine(frame.left + frame.width() * i / 3f, frame.top, frame.left + frame.width() * i / 3f, frame.bottom, thin);
            c.drawLine(frame.left, frame.top + frame.height() * i / 3f, frame.right, frame.top + frame.height() * i / 3f, thin);
        }
        if (circleGuide) c.drawCircle(frame.centerX(), frame.centerY(), Math.min(frame.width(), frame.height()) / 2f, line);
    }

    /** The part of the picture inside the frame, at most {@code maxWidth} pixels wide. */
    public Bitmap crop(int maxWidth) {
        Matrix inv = new Matrix();
        if (bitmap == null || !matrix.invert(inv)) return null;
        RectF src = new RectF(frame);
        inv.mapRect(src);
        Rect s = new Rect(Math.max(0, Math.round(src.left)), Math.max(0, Math.round(src.top)),
                Math.min(bitmap.getWidth(), Math.round(src.right)), Math.min(bitmap.getHeight(), Math.round(src.bottom)));
        if (s.width() <= 0 || s.height() <= 0) return null;
        int outW = Math.min(maxWidth, s.width());
        int outH = Math.max(1, Math.round(outW / aspect));
        Bitmap out = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888);
        new Canvas(out).drawBitmap(bitmap, s, new Rect(0, 0, outW, outH), pic);
        return out;
    }
}
