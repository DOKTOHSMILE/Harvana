package com.harvana.app.banks;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

/** Puts a bank's logo in an ImageView: the bundled picture, else the directory's, else a coloured circle with its initials. */
public final class BankLogo {
    private BankLogo() {}

    /** By whatever name was saved ("GTBank", "Guaranty Trust Bank"…). Unknown names get initials. */
    public static void into(ImageView v, String bankName) {
        if (v == null) return;
        BankDirectory.Bank b = BankDirectory.match(v.getContext(), bankName);
        if (b == null) {
            BankDirectory.Bank unknown = new BankDirectory.Bank();
            unknown.name = bankName == null || bankName.trim().isEmpty() ? "?" : bankName.trim();
            into(v, unknown);
        } else {
            into(v, b);
        }
    }

    public static void into(ImageView v, BankDirectory.Bank b) {
        if (v == null || b == null) return;
        v.setImageTintList(null);
        v.clearColorFilter();
        Drawable fallback = initials(v.getContext(), b, 96);
        Object src = !b.logoAsset.isEmpty() ? Uri.parse("file:///android_asset/" + b.logoAsset)
                : (b.logoUrl.startsWith("https://") ? b.logoUrl : null);
        if (src == null) { v.setImageDrawable(fallback); return; }
        try {
            Glide.with(v.getContext()).load(src)
                    .apply(new RequestOptions().circleCrop().placeholder(fallback).error(fallback))
                    .into(v);
        } catch (IllegalArgumentException destroyed) {
            v.setImageDrawable(fallback);          // the screen was closing
        }
    }

    /** A round badge in the bank's colour with its initials, used offline and for banks without a logo. */
    public static Drawable initials(Context c, BankDirectory.Bank b, int sizePx) {
        Bitmap bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(bmp);
        int color;
        try { color = b.color.isEmpty() ? colorFor(b.display()) : Color.parseColor(b.color); } catch (Exception e) { color = colorFor(b.display()); }
        Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
        bg.setColor(color);
        cv.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, bg);
        Paint t = new Paint(Paint.ANTI_ALIAS_FLAG);
        t.setColor(Color.WHITE);
        t.setTextSize(sizePx * 0.38f);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextAlign(Paint.Align.CENTER);
        float y = sizePx / 2f - (t.descent() + t.ascent()) / 2f;
        cv.drawText(b.initials(), sizePx / 2f, y, t);
        return new BitmapDrawable(c.getResources(), bmp);
    }

    private static int colorFor(String s) {
        int[] palette = {0xFF0B5F57, 0xFF1E3A8A, 0xFF7C3AED, 0xFFB45309, 0xFF047857, 0xFFBE123C, 0xFF0369A1, 0xFF4D7C0F};
        return palette[Math.abs((s == null ? "" : s).hashCode()) % palette.length];
    }
}
