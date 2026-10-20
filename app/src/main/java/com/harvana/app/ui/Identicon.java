package com.harvana.app.ui;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * A picture made from an address (owner, 20 Oct 2026: no letters as icons): a 5×5 mirrored pattern in a colour taken
 * from the address's hash, on a soft disc of the same colour. The same address always draws the same picture, so a
 * person can recognise a wallet at a glance. {@link #coin} draws a token without a logo the same way, inside a ring.
 */
public final class Identicon extends Drawable {

    private final boolean[] cells = new boolean[25];
    private final int fg, bg;
    private final boolean ring;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private Identicon(String seed, boolean ring) {
        byte[] h;
        try { h = MessageDigest.getInstance("SHA-256").digest((seed == null ? "" : seed).getBytes(StandardCharsets.UTF_8)); }
        catch (Exception e) { h = new byte[32]; }
        for (int r = 0; r < 5; r++) {
            for (int c = 0; c < 3; c++) {
                boolean on = (h[r * 3 + c] & 1) == 1;
                cells[r * 5 + c] = on;
                cells[r * 5 + (4 - c)] = on;                    // mirrored, like a face
            }
        }
        float hue = ((h[20] & 0xff) * 360f) / 256f;
        fg = Color.HSVToColor(new float[]{hue, 0.55f, 0.92f});
        bg = Color.HSVToColor(new float[]{hue, 0.35f, 0.28f});
        this.ring = ring;
    }

    /** A wallet address's picture. */
    public static Identicon of(String address) { return new Identicon(address == null ? "" : address.trim().toLowerCase(java.util.Locale.ROOT), false); }

    /** A token with no logo of its own: its contract's picture inside a coin ring. */
    public static Identicon coin(String key) { return new Identicon("coin:" + key, true); }

    @Override public void draw(Canvas canvas) {
        Rect b = getBounds();
        float size = Math.min(b.width(), b.height()), cx = b.exactCenterX(), cy = b.exactCenterY(), r = size / 2f;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(bg);
        canvas.drawCircle(cx, cy, r, paint);
        if (ring) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(size * 0.06f);
            paint.setColor(fg);
            canvas.drawCircle(cx, cy, r - size * 0.05f, paint);
            paint.setStyle(Paint.Style.FILL);
        }
        // The grid sits inside the disc (about 62% of its width), drawn as rounded squares.
        float grid = size * (ring ? 0.5f : 0.62f), cell = grid / 5f, x0 = cx - grid / 2f, y0 = cy - grid / 2f, corner = cell * 0.22f;
        paint.setColor(fg);
        Path p = new Path();
        for (int i = 0; i < 25; i++) {
            if (!cells[i]) continue;
            float x = x0 + (i % 5) * cell, y = y0 + (i / 5) * cell;
            p.addRoundRect(x + cell * 0.06f, y + cell * 0.06f, x + cell * 0.94f, y + cell * 0.94f, corner, corner, Path.Direction.CW);
        }
        canvas.drawPath(p, paint);
    }

    @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); }
    @Override public void setColorFilter(ColorFilter cf) { paint.setColorFilter(cf); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    @Override public int getIntrinsicWidth() { return 96; }
    @Override public int getIntrinsicHeight() { return 96; }
}
