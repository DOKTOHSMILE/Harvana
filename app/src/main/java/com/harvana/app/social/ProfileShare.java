package com.harvana.app.social;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.net.Uri;

import androidx.core.content.FileProvider;

import com.harvana.app.HARVQRCodeGenerator;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Sharing a profile sends both a picture and words: a card with the name, @username, trust tier and the profile QR code
 * (scanning it in Harvana offers "Add friend"), plus a message that says who it is and how to find them. Apps that take only
 * one of the two still get something useful.
 */
public final class ProfileShare {
    private ProfileShare() {}

    /** The message that goes with the picture. */
    public static String message(String name, String username, int trustPercent, String tier) {
        String who = name == null || name.isEmpty() ? (username == null || username.isEmpty() ? "A member" : "@" + username) : name;
        StringBuilder b = new StringBuilder();
        b.append(who);
        if (username != null && !username.isEmpty() && name != null && !name.isEmpty()) b.append(" (@").append(username).append(")");
        b.append(" on Harvana");
        if (trustPercent >= 0 && tier != null && !tier.isEmpty()) b.append(", trust ").append(trustPercent).append("% ").append(tier);
        b.append(".");
        if (username != null && !username.isEmpty()) {
            b.append("\n\nScan the QR code in the Harvana app to add me, or search @").append(username).append(".");
        }
        return b.toString();
    }

    /** Shares the card picture with the message; without a username (no QR possible) it shares the message alone. */
    public static void share(Activity a, String name, String username, int trustPercent, String tier) {
        String text = message(name, username, trustPercent, tier);
        Bitmap card = username == null || username.isEmpty() ? null : card(name, username, trustPercent, tier);
        if (card != null) {
            try {
                File dir = new File(a.getCacheDir(), "images");
                dir.mkdirs();
                File f = new File(dir, "harvana-profile.png");
                try (FileOutputStream out = new FileOutputStream(f)) { card.compress(Bitmap.CompressFormat.PNG, 100, out); }
                Uri uri = FileProvider.getUriForFile(a, "com.harvana.app.fileprovider", f);
                Intent send = new Intent(Intent.ACTION_SEND).setType("image/png")
                        .putExtra(Intent.EXTRA_STREAM, uri)
                        .putExtra(Intent.EXTRA_TEXT, text)
                        .putExtra(Intent.EXTRA_SUBJECT, "Harvana profile")
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                send.setClipData(ClipData.newRawUri("Harvana profile", uri));     // lets the share sheet show the picture
                a.startActivity(Intent.createChooser(send, "Share profile"));
                return;
            } catch (Exception ignored) { }
        }
        a.startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), "Share profile"));
    }

    /** The share card: 1080 wide, the app's cream and ink, the unified QR in the middle. */
    static Bitmap card(String name, String username, int trustPercent, String tier) {
        int w = 1080, h = 1350;
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(0xFF053D3A);
        c.drawRect(0, 0, w, h, p);
        p.setColor(0xFFFFF8EE);
        c.drawRoundRect(new RectF(60, 60, w - 60, h - 60), 56, 56, p);

        p.setTextAlign(Paint.Align.CENTER);
        p.setColor(0xFF0B2F2C);
        p.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        p.setTextSize(64);
        String title = name == null || name.isEmpty() ? "@" + username : name;
        if (p.measureText(title) > w - 200) { p.setTextSize(48); }
        c.drawText(title, w / 2f, 210, p);
        p.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        p.setTextSize(42);
        p.setColor(0xFF587570);
        c.drawText("@" + username, w / 2f, 275, p);
        if (trustPercent >= 0 && tier != null && !tier.isEmpty()) {
            String t = trustPercent + "% · " + tier;
            p.setTextSize(36);
            float tw = p.measureText(t) + 64;
            p.setColor(0x220B6B61);
            c.drawRoundRect(new RectF(w / 2f - tw / 2, 305, w / 2f + tw / 2, 365), 30, 30, p);
            p.setColor(0xFF0B6B61);
            p.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
            c.drawText(t, w / 2f, 348, p);
        }
        Bitmap qr = HARVQRCodeGenerator.generateQRCode(com.harvana.app.scan.QrRouter.PROFILE_PREFIX + username, 720, null, false);
        if (qr != null) c.drawBitmap(qr, null, new RectF((w - 720) / 2f, 410, (w + 720) / 2f, 1130), new Paint(Paint.FILTER_BITMAP_FLAG));
        p.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        p.setTextSize(36);
        p.setColor(0xFF0B2F2C);
        c.drawText("Scan in the Harvana app to add me", w / 2f, 1200, p);
        p.setTextSize(30);
        p.setColor(0xFF587570);
        c.drawText("Harvana · save together, build trust", w / 2f, 1250, p);
        return bmp;
    }
}
