package com.harvana.app.utils;

import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.RectF;
import android.media.FaceDetector;

/**
 * Finds the main face in a picture, on the phone, with Android's built-in detector (no network, no extra library, nothing uploaded).
 * The answer is a rectangle in 0..1 of the picture's width and height, or null when there is no clear face, in which case callers
 * fall back to the middle of the picture. The detector sees upright faces only; pictures here are already turned upright.
 */
public final class FaceFocus {
    private FaceFocus() {}

    private static final int DETECT_PX = 360;       // detection runs on a small copy: fast, and enough to find a face

    /** The largest confident face, as a rectangle in 0..1, or null. */
    public static RectF find(Bitmap src) {
        if (src == null || src.getWidth() < 32 || src.getHeight() < 32) return null;
        Bitmap small = null;
        try {
            float k = Math.min(1f, DETECT_PX / (float) Math.max(src.getWidth(), src.getHeight()));
            int w = Math.max(2, Math.round(src.getWidth() * k)) & ~1;           // the detector needs an even width
            int h = Math.max(2, Math.round(src.getHeight() * k));
            small = Bitmap.createScaledBitmap(src, w, h, true).copy(Bitmap.Config.RGB_565, false);
            FaceDetector.Face[] faces = new FaceDetector.Face[4];
            int n = new FaceDetector(small.getWidth(), small.getHeight(), faces.length).findFaces(small, faces);
            FaceDetector.Face best = null;
            float bestScore = 0;
            for (int i = 0; i < n; i++) {
                FaceDetector.Face f = faces[i];
                if (f == null || f.confidence() < 0.4f) continue;
                float score = f.eyesDistance() * f.confidence();
                if (score > bestScore) { bestScore = score; best = f; }
            }
            if (best == null) return null;
            PointF mid = new PointF();
            best.getMidPoint(mid);
            float d = best.eyesDistance();
            // A face is about 2.6 eye-distances wide and 3.4 tall, and its middle sits a little below the line of the eyes.
            float cx = mid.x, cy = mid.y + 0.5f * d, fw = 2.6f * d, fh = 3.4f * d;
            RectF r = new RectF((cx - fw / 2f) / small.getWidth(), (cy - fh / 2f) / small.getHeight(),
                    (cx + fw / 2f) / small.getWidth(), (cy + fh / 2f) / small.getHeight());
            r.left = Math.max(0f, r.left); r.top = Math.max(0f, r.top); r.right = Math.min(1f, r.right); r.bottom = Math.min(1f, r.bottom);
            return r.width() > 0.02f && r.height() > 0.02f ? r : null;
        } catch (Throwable t) {
            return null;
        } finally {
            if (small != null) small.recycle();
        }
    }
}
