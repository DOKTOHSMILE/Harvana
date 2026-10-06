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
    // Free (flexible) mode: the picture stays fitted and the person moves and resizes the crop box itself.
    private boolean free;
    private static final int NONE = 0, MOVE = 1, L = 2, T = 4, R = 8, B = 16;
    private int grab = NONE;
    private float lastX, lastY;
    private final float slop;
    private final float minBox;

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
        slop = 28 * d;
        minBox = 56 * d;
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

    /**
     * @param aspect frame width divided by frame height; zero or less means a free box the person resizes
     * @param circle draw a round guide inside the frame (for profile pictures)
     */
    public void setup(Bitmap bmp, float aspect, boolean circle) {
        this.bitmap = bmp;
        this.free = aspect <= 0f;
        this.aspect = free ? 1f : aspect;
        this.circleGuide = circle;
        this.ready = false;
        this.face = FaceFocus.find(bmp);        // null when there is no clear face: the middle of the picture is used
        layoutFrame();
        invalidate();
    }

    private RectF face;

    /** Fixed frame: zoom and move the picture so the face fills a good part of the frame and sits in its middle. */
    private void focusFace() {
        if (face == null || free) return;
        float bw = bitmap.getWidth(), bh = bitmap.getHeight();
        float faceH = face.height() * bh, faceW = face.width() * bw;
        float want = Math.min(frame.height() * (aspect >= 1.6f ? 0.7f : 0.55f) / faceH, frame.width() * 0.6f / faceW);
        float s = Math.max(minScale, Math.min(maxScale, want));
        matrix.reset();
        matrix.postScale(s, s);
        float fx = face.centerX() * bw * s, fy = face.centerY() * bh * s;
        matrix.postTranslate(frame.centerX() - fx, frame.centerY() - fy);
        clamp();
    }

    @Override protected void onSizeChanged(int w, int h, int ow, int oh) { super.onSizeChanged(w, h, ow, oh); layoutFrame(); }

    private void layoutFrame() {
        int w = getWidth(), h = getHeight();
        if (w == 0 || h == 0 || bitmap == null) return;
        if (free) { layoutFree(w, h); return; }
        float pad = 20 * getResources().getDisplayMetrics().density;
        float fw = w - 2 * pad, fh = fw / aspect;
        if (fh > h - 2 * pad) { fh = h - 2 * pad; fw = fh * aspect; }
        frame.set((w - fw) / 2f, (h - fh) / 2f, (w + fw) / 2f, (h + fh) / 2f);
        minScale = Math.max(frame.width() / bitmap.getWidth(), frame.height() / bitmap.getHeight());
        maxScale = minScale * 8f;
        matrix.reset();
        matrix.postScale(minScale, minScale);
        matrix.postTranslate(frame.centerX() - bitmap.getWidth() * minScale / 2f, frame.centerY() - bitmap.getHeight() * minScale / 2f);
        focusFace();
        ready = true;
    }

    /** Free mode: the whole picture is fitted on screen and the box starts around most of it. */
    private void layoutFree(int w, int h) {
        float pad = 20 * getResources().getDisplayMetrics().density;
        float fit = Math.min((w - 2 * pad) / bitmap.getWidth(), (h - 2 * pad) / bitmap.getHeight());
        float iw = bitmap.getWidth() * fit, ih = bitmap.getHeight() * fit;
        float left = (w - iw) / 2f, top = (h - ih) / 2f;
        matrix.reset();
        matrix.postScale(fit, fit);
        matrix.postTranslate(left, top);
        imageBox.set(left, top, left + iw, top + ih);
        float inset = Math.min(iw, ih) * 0.08f;
        frame.set(left + inset, top + inset, left + iw - inset, top + ih - inset);
        if (face != null) {            // free box: start around the face, with room around it
            float pw = face.width() * iw * 0.7f, ph = face.height() * ih * 0.6f;
            RectF box = new RectF(left + face.left * iw - pw, top + face.top * ih - ph, left + face.right * iw + pw, top + face.bottom * ih + ph);
            box.intersect(imageBox);
            if (box.width() >= minBox && box.height() >= minBox) frame.set(box);
        }
        ready = true;
    }

    private final RectF imageBox = new RectF();

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

    private void startGrab(float x, float y) {
        grab = NONE;
        boolean nearL = Math.abs(x - frame.left) < slop, nearR = Math.abs(x - frame.right) < slop;
        boolean nearT = Math.abs(y - frame.top) < slop, nearB = Math.abs(y - frame.bottom) < slop;
        boolean withinX = x > frame.left - slop && x < frame.right + slop, withinY = y > frame.top - slop && y < frame.bottom + slop;
        if (nearL && withinY) grab |= L;
        if (nearR && withinY) grab |= R;
        if (nearT && withinX) grab |= T;
        if (nearB && withinX) grab |= B;
        if (grab == NONE && frame.contains(x, y)) grab = MOVE;
        lastX = x;
        lastY = y;
    }

    private void dragBox(float x, float y) {
        float dx = x - lastX, dy = y - lastY;
        lastX = x;
        lastY = y;
        if (grab == MOVE) {
            dx = Math.max(imageBox.left - frame.left, Math.min(imageBox.right - frame.right, dx));
            dy = Math.max(imageBox.top - frame.top, Math.min(imageBox.bottom - frame.bottom, dy));
            frame.offset(dx, dy);
            return;
        }
        if ((grab & L) != 0) frame.left = Math.max(imageBox.left, Math.min(frame.right - minBox, frame.left + dx));
        if ((grab & R) != 0) frame.right = Math.min(imageBox.right, Math.max(frame.left + minBox, frame.right + dx));
        if ((grab & T) != 0) frame.top = Math.max(imageBox.top, Math.min(frame.bottom - minBox, frame.top + dy));
        if ((grab & B) != 0) frame.bottom = Math.min(imageBox.bottom, Math.max(frame.top + minBox, frame.bottom + dy));
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        getParent().requestDisallowInterceptTouchEvent(true);
        if (free) {
            if (!ready) return true;
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN: startGrab(e.getX(), e.getY()); break;
                case MotionEvent.ACTION_MOVE: if (grab != NONE) { dragBox(e.getX(), e.getY()); invalidate(); } break;
                default: grab = NONE; break;
            }
            return true;
        }
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
        if (free) {
            float arm = 22 * getResources().getDisplayMetrics().density;
            line.setStrokeWidth(4 * getResources().getDisplayMetrics().density);
            c.drawLine(frame.left, frame.top, frame.left + arm, frame.top, line);       c.drawLine(frame.left, frame.top, frame.left, frame.top + arm, line);
            c.drawLine(frame.right, frame.top, frame.right - arm, frame.top, line);     c.drawLine(frame.right, frame.top, frame.right, frame.top + arm, line);
            c.drawLine(frame.left, frame.bottom, frame.left + arm, frame.bottom, line); c.drawLine(frame.left, frame.bottom, frame.left, frame.bottom - arm, line);
            c.drawLine(frame.right, frame.bottom, frame.right - arm, frame.bottom, line); c.drawLine(frame.right, frame.bottom, frame.right, frame.bottom - arm, line);
            line.setStrokeWidth(2 * getResources().getDisplayMetrics().density);
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
        int outH = free ? Math.max(1, Math.round(outW * (float) s.height() / s.width())) : Math.max(1, Math.round(outW / aspect));
        Bitmap out = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888);
        new Canvas(out).drawBitmap(bitmap, s, new Rect(0, 0, outW, outH), pic);
        return out;
    }
}
