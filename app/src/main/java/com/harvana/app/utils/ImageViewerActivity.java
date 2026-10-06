package com.harvana.app.utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

/** A picture shown full screen on black: pinch to zoom, drag to look around, double-tap to zoom in or back, the close button or Back to leave. */
public class ImageViewerActivity extends AppCompatActivity {
    private static final String X_URL = "url";

    public static void open(Context c, String url) {
        if (url == null || !url.startsWith("http")) return;
        c.startActivity(new Intent(c, ImageViewerActivity.class).putExtra(X_URL, url));
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        String url = getIntent().getStringExtra(X_URL);
        if (url == null) { finish(); return; }
        float d = getResources().getDisplayMetrics().density;
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        final ZoomView zoom = new ZoomView(this);
        root.addView(zoom, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        final ProgressBar busy = new ProgressBar(this);
        root.addView(busy, new FrameLayout.LayoutParams((int) (40 * d), (int) (40 * d), Gravity.CENTER));
        android.widget.ImageView close = new android.widget.ImageView(this);
        close.setImageResource(com.harvana.app.R.drawable.ic_close);
        close.setColorFilter(Color.WHITE);
        close.setPadding((int) (11 * d), (int) (11 * d), (int) (11 * d), (int) (11 * d));
        close.setBackgroundColor(0x66000000);
        close.setContentDescription("Close");
        close.setOnClickListener(v -> finish());
        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams((int) (44 * d), (int) (44 * d), Gravity.TOP | Gravity.START);
        cp.setMargins((int) (12 * d), (int) (36 * d), 0, 0);
        root.addView(close, cp);
        setContentView(root);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);

        Glide.with(this).asBitmap().load(url).override(2048, 2048).into(new CustomTarget<Bitmap>() {
            @Override public void onResourceReady(@NonNull Bitmap bmp, @Nullable Transition<? super Bitmap> t) { busy.setVisibility(View.GONE); zoom.show(bmp); }
            @Override public void onLoadFailed(@Nullable Drawable errorDrawable) {
                busy.setVisibility(View.GONE);
                Toast.makeText(ImageViewerActivity.this, "Couldn't load the picture.", Toast.LENGTH_LONG).show();
            }
            @Override public void onLoadCleared(@Nullable Drawable placeholder) { }
        });
    }

    /** The zoomable picture. It starts fitted inside the screen and never leaves an empty gap on a side it fills. */
    private static final class ZoomView extends View {
        private Bitmap bmp;
        private final Matrix matrix = new Matrix();
        private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        private final float[] v = new float[9];
        private float fit = 1f;
        private final ScaleGestureDetector scaler;
        private final GestureDetector taps;

        ZoomView(Context c) {
            super(c);
            scaler = new ScaleGestureDetector(c, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                @Override public boolean onScale(ScaleGestureDetector s) {
                    if (bmp == null) return true;
                    float now = scale();
                    float f = Math.max(fit / now, Math.min(fit * 6f / now, s.getScaleFactor()));
                    matrix.postScale(f, f, s.getFocusX(), s.getFocusY());
                    keepInside();
                    invalidate();
                    return true;
                }
            });
            taps = new GestureDetector(c, new GestureDetector.SimpleOnGestureListener() {
                @Override public boolean onScroll(MotionEvent a, MotionEvent b, float dx, float dy) {
                    if (bmp == null || scaler.isInProgress()) return true;
                    matrix.postTranslate(-dx, -dy);
                    keepInside();
                    invalidate();
                    return true;
                }
                @Override public boolean onDoubleTap(MotionEvent e) {
                    if (bmp == null) return true;
                    float now = scale();
                    if (now > fit * 1.5f) place();
                    else if (face != null) zoomToFace();                            // a face: go straight to it
                    else { matrix.postScale(3f, 3f, e.getX(), e.getY()); keepInside(); }
                    invalidate();
                    return true;
                }
            });
        }

        private android.graphics.RectF face;     // the main face, 0..1, or null

        void show(Bitmap b) {
            bmp = b;
            place();
            invalidate();
            // Looked for off the main thread; the first double-tap then goes to the face when there is one.
            new Thread(() -> face = com.harvana.app.utils.FaceFocus.find(b)).start();
        }

        /** Zoom so the face fills about half the screen width and sits in the middle. */
        private void zoomToFace() {
            float faceW = face.width() * bmp.getWidth();
            float target = Math.max(fit * 1.6f, Math.min(fit * 6f, getWidth() * 0.5f / faceW));
            matrix.reset();
            matrix.postScale(target, target);
            matrix.postTranslate(getWidth() / 2f - face.centerX() * bmp.getWidth() * target, getHeight() / 2f - face.centerY() * bmp.getHeight() * target);
            keepInside();
        }

        @Override protected void onSizeChanged(int w, int h, int ow, int oh) { super.onSizeChanged(w, h, ow, oh); if (bmp != null) place(); }

        private void place() {
            if (bmp == null || getWidth() == 0) return;
            fit = Math.min(getWidth() / (float) bmp.getWidth(), getHeight() / (float) bmp.getHeight());
            matrix.reset();
            matrix.postScale(fit, fit);
            matrix.postTranslate((getWidth() - bmp.getWidth() * fit) / 2f, (getHeight() - bmp.getHeight() * fit) / 2f);
        }

        private float scale() { matrix.getValues(v); return v[Matrix.MSCALE_X]; }

        private void keepInside() {
            RectF r = new RectF(0, 0, bmp.getWidth(), bmp.getHeight());
            matrix.mapRect(r);
            float dx = 0, dy = 0;
            if (r.width() <= getWidth()) dx = (getWidth() - r.width()) / 2f - r.left;
            else if (r.left > 0) dx = -r.left;
            else if (r.right < getWidth()) dx = getWidth() - r.right;
            if (r.height() <= getHeight()) dy = (getHeight() - r.height()) / 2f - r.top;
            else if (r.top > 0) dy = -r.top;
            else if (r.bottom < getHeight()) dy = getHeight() - r.bottom;
            matrix.postTranslate(dx, dy);
        }

        @Override public boolean onTouchEvent(MotionEvent e) {
            scaler.onTouchEvent(e);
            taps.onTouchEvent(e);
            return true;
        }

        @Override protected void onDraw(Canvas c) { if (bmp != null) c.drawBitmap(bmp, matrix, paint); }
    }
}
