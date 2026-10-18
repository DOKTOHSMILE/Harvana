package com.harvana.app.support;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/** A chat photo small enough to sit on the message itself. */
public final class ChatPhoto {
    private ChatPhoto() {}

    public static String jpeg(Context c, Uri uri) throws Exception {
        InputStream in = c.getContentResolver().openInputStream(uri);
        if (in == null) return "";
        Bitmap raw = BitmapFactory.decodeStream(in);
        in.close();
        if (raw == null) return "";
        int longest = Math.max(raw.getWidth(), raw.getHeight());
        Bitmap scaled = raw;
        if (longest > 1024) {
            float s = 1024f / longest;
            scaled = Bitmap.createScaledBitmap(raw, Math.max(1, (int) (raw.getWidth() * s)), Math.max(1, (int) (raw.getHeight() * s)), true);
            if (scaled != raw) raw.recycle();
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int q = 70;
        scaled.compress(Bitmap.CompressFormat.JPEG, q, out);
        while (out.size() > 180_000 && q > 35) {
            out.reset();
            q -= 10;
            scaled.compress(Bitmap.CompressFormat.JPEG, q, out);
        }
        if (scaled != raw) scaled.recycle();
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP);
    }

    public static Bitmap decode(String b64) {
        if (b64 == null || b64.isEmpty()) return null;
        try {
            byte[] bytes = Base64.decode(b64, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (Exception e) {
            return null;
        }
    }
}
