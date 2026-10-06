package com.harvana.app.utils;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;

import androidx.annotation.NonNull;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;

import java.nio.charset.Charset;
import java.security.MessageDigest;

/**
 * Glide's centre crop, but aimed at the face: when the picture has a face, the visible window is placed so the face is in it (a little
 * above the middle, leaving head room); when it has none, it is the ordinary centre crop. Used for the cover pictures of Learn posts.
 */
public final class FaceCrop extends BitmapTransformation {
    private static final byte[] ID = "com.harvana.app.utils.FaceCrop.v1".getBytes(Charset.forName("UTF-8"));

    @Override
    protected Bitmap transform(@NonNull BitmapPool pool, @NonNull Bitmap src, int outW, int outH) {
        if (outW <= 0 || outH <= 0) return src;
        float scale = Math.max(outW / (float) src.getWidth(), outH / (float) src.getHeight());
        float sw = src.getWidth() * scale, sh = src.getHeight() * scale;
        float fx = 0.5f, fy = 0.5f;
        RectF face = FaceFocus.find(src);
        if (face != null) { fx = face.centerX(); fy = Math.max(0f, face.centerY() - 0.12f * face.height()); }   // a little head room
        float left = clamp(fx * sw - outW / 2f, 0f, sw - outW);
        float top = clamp(fy * sh - outH / 2f, 0f, sh - outH);
        Bitmap out = pool.get(outW, outH, src.getConfig() != null ? src.getConfig() : Bitmap.Config.ARGB_8888);
        out.setHasAlpha(src.hasAlpha());
        Matrix m = new Matrix();
        m.setScale(scale, scale);
        m.postTranslate(-left, -top);
        new android.graphics.Canvas(out).drawBitmap(src, m, new android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG));
        return out;
    }

    private static float clamp(float v, float lo, float hi) { return hi < lo ? lo : Math.max(lo, Math.min(hi, v)); }

    @Override public void updateDiskCacheKey(@NonNull MessageDigest md) { md.update(ID); }
    @Override public boolean equals(Object o) { return o instanceof FaceCrop; }
    @Override public int hashCode() { return ID.length * 31 + 7; }
}
