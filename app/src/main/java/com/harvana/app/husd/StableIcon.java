package com.harvana.app.husd;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import androidx.core.content.ContextCompat;

import com.harvana.app.R;
import com.harvana.app.wallet.FxRates;

/** A coin for each stable asset: hUSD's own drawable, and for the others a coloured coin with the currency's sign. */
public final class StableIcon {
    private StableIcon() {}

    public static Drawable drawable(Context c, String code) {
        if ("HUSD".equals(code)) return ContextCompat.getDrawable(c, R.drawable.ic_coin_husd);
        return new BitmapDrawable(c.getResources(), bitmap(code, 144));
    }

    /** The same coin as a bitmap (for the middle of a QR code). */
    public static Bitmap bitmap(Context c, String code, int px) {
        if (!"HUSD".equals(code)) return bitmap(code, px);
        Drawable d = ContextCompat.getDrawable(c, R.drawable.ic_coin_husd);
        Bitmap b = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888);
        if (d != null) { d.setBounds(0, 0, px, px); d.draw(new Canvas(b)); }
        return b;
    }

    private static Bitmap bitmap(String code, int px) {
        int[] colors = colors(Stable.currency(code));
        Bitmap b = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(b);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        float r = px / 2f;
        p.setColor(colors[0]);
        cv.drawCircle(r, r, r, p);
        p.setColor(colors[1]);
        cv.drawCircle(r, r, r * 0.86f, p);
        String sign = FxRates.symbolFor(Stable.currency(code));
        p.setColor(Color.WHITE);
        p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(px * (sign.length() > 2 ? 0.30f : sign.length() == 2 ? 0.40f : 0.52f));
        Paint.FontMetrics fm = p.getFontMetrics();
        cv.drawText(sign, r, r - (fm.ascent + fm.descent) / 2f, p);
        return b;
    }

    /** {ring, face}. Each currency its own colour so a list of them reads at a glance. */
    private static int[] colors(String currency) {
        switch (currency) {
            case "EUR": return new int[]{0xFF1F4FB5, 0xFF3A6FE0};
            case "GBP": return new int[]{0xFF5B2A86, 0xFF7D45B0};
            case "NGN": return new int[]{0xFF0B7A3B, 0xFF16A34A};
            case "GHS": return new int[]{0xFFB7791F, 0xFFE0A030};
            case "KES": return new int[]{0xFF9B1C1C, 0xFFC93636};
            case "ZAR": return new int[]{0xFF0E6E6E, 0xFF159A9A};
            default:    return new int[]{0xFF1B8A5A, 0xFF2FB57A};
        }
    }
}
