package com.harvana.app.receipt;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.harvana.app.ui.SheetDialog;

import java.io.File;

/**
 * The one way the app shares a receipt or a statement. Files are written to the app's cache (no storage permission,
 * nothing left lying around in Documents) and handed to the share sheet through the FileProvider.
 */
public final class ReceiptShare {
    private ReceiptShare() {}

    private static final String AUTHORITY = "com.harvana.app.fileprovider";

    /** Asks "Image or PDF?" and then shares the receipt that way. */
    public static void choose(final Activity a, final Receipt r) {
        new SheetDialog.Builder(a)
                .setTitle("Share receipt")
                .setItems(new CharSequence[] {"Image (best for chats)", "PDF document"}, (d, which) -> {
                    if (which == 0) image(a, r); else pdf(a, r);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    public static void image(Activity a, Receipt r) {
        try {
            File f = ReceiptRenderer.renderPng(a, r, cacheFile(a, "images", "harvana-receipt-" + r.number() + ".png"));
            send(a, f, "image/png", "Harvana " + r.title.toLowerCase(java.util.Locale.US) + " " + r.number(), summary(r));
        } catch (Exception e) {
            Toast.makeText(a, "Could not make the receipt: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public static void pdf(Activity a, Receipt r) {
        try {
            File f = ReceiptRenderer.renderPdf(a, r, cacheFile(a, "docs", "harvana-receipt-" + r.number() + ".pdf"));
            send(a, f, "application/pdf", "Harvana " + r.title.toLowerCase(java.util.Locale.US) + " " + r.number(), summary(r));
        } catch (Exception e) {
            Toast.makeText(a, "Could not make the receipt: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /** Shares a file that lives anywhere (a CSV or DOCX the app wrote): it is copied to the cache first so the FileProvider can serve it. */
    public static void shareFile(Activity a, File file, String mime, String subject) {
        try {
            File copy = cacheFile(a, "docs", file.getName());
            if (!copy.equals(file)) {
                try (java.io.InputStream in = new java.io.FileInputStream(file); java.io.OutputStream out = new java.io.FileOutputStream(copy)) {
                    byte[] buf = new byte[16384]; int n;
                    while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                }
            }
            send(a, copy, mime, subject, null);
        } catch (Exception e) {
            Toast.makeText(a, "Could not share the file: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /** Shares any file under the cache folders the FileProvider exposes (images/ or docs/). */
    public static void send(Activity a, File file, String mime, String subject, String text) {
        Uri uri = FileProvider.getUriForFile(a, AUTHORITY, file);
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType(mime);
        send.putExtra(Intent.EXTRA_STREAM, uri);
        send.putExtra(Intent.EXTRA_SUBJECT, subject);
        if (text != null) send.putExtra(Intent.EXTRA_TEXT, text);
        send.setClipData(ClipData.newRawUri("receipt", uri));
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        a.startActivity(Intent.createChooser(send, "Share"));
    }

    public static File cacheFile(Context c, String folder, String name) {
        File dir = new File(c.getCacheDir(), folder);
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, name);
    }

    /** Draws page 1 of a PDF into a sharp PNG (used for the statement image). */
    public static void pdfFirstPageToPng(File pdf, File png) throws Exception {
        android.os.ParcelFileDescriptor fd = android.os.ParcelFileDescriptor.open(pdf, android.os.ParcelFileDescriptor.MODE_READ_ONLY);
        try (android.graphics.pdf.PdfRenderer renderer = new android.graphics.pdf.PdfRenderer(fd)) {
            android.graphics.pdf.PdfRenderer.Page page = renderer.openPage(0);
            android.graphics.Bitmap bmp = android.graphics.Bitmap.createBitmap(page.getWidth() * 3, page.getHeight() * 3, android.graphics.Bitmap.Config.ARGB_8888);
            bmp.eraseColor(android.graphics.Color.WHITE);
            page.render(bmp, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            page.close();
            try (java.io.FileOutputStream out = new java.io.FileOutputStream(png)) { bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out); }
            bmp.recycle();
        }
    }

    private static String summary(Receipt r) {
        return r.title + " " + r.number() + "\n" + (r.direction < 0 ? "- " : r.direction > 0 ? "+ " : "") + r.amount + " " + r.symbol
                + (r.status == null || r.status.isEmpty() ? "" : "\nStatus: " + r.status);
    }
}
