package com.harvana.app.receipt;

import android.app.Activity;
import android.content.Context;
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
            File f = ReceiptRenderer.renderPng(a, shown(a, r), cacheFile(a, "images", "harvana-receipt-" + r.number() + ".png"));
            send(a, f, "image/png", "Harvana " + r.title.toLowerCase(java.util.Locale.US) + " " + r.number(), summary(shown(a, r)));
        } catch (Exception e) {
            Toast.makeText(a, "Could not make the receipt: " + com.harvana.app.utils.ErrorText.of(e), Toast.LENGTH_LONG).show();
        }
    }

    public static void pdf(Activity a, Receipt r) {
        try {
            File f = ReceiptRenderer.renderPdf(a, shown(a, r), cacheFile(a, "docs", "harvana-receipt-" + r.number() + ".pdf"));
            send(a, f, "application/pdf", "Harvana " + r.title.toLowerCase(java.util.Locale.US) + " " + r.number(), summary(shown(a, r)));
        } catch (Exception e) {
            Toast.makeText(a, "Could not make the receipt: " + com.harvana.app.utils.ErrorText.of(e), Toast.LENGTH_LONG).show();
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
            Toast.makeText(a, "Could not share the file: " + com.harvana.app.utils.ErrorText.of(e), Toast.LENGTH_LONG).show();
        }
    }

    /** Shares any file under the cache folders the FileProvider exposes (images/ or docs/). */
    public static void send(Activity a, File file, String mime, String subject, String text) {
        Uri uri = FileProvider.getUriForFile(a, AUTHORITY, file);
        com.harvana.app.share.ShareCenter.file(a, uri, mime, subject, text, "Share");
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

    /** When balances are hidden, the shared receipt hides amounts too. The reference number stays. */
    private static Receipt shown(Activity a, Receipt r) {
        if (a == null || r == null || !com.harvana.app.utils.BalancePrivacy.isHidden(a)) return r;
        Receipt copy = new Receipt();
        copy.title = r.title;
        copy.status = r.status;
        copy.amount = com.harvana.app.utils.BalancePrivacy.mask(r.amount);
        copy.symbol = r.symbol;
        copy.direction = r.direction;
        copy.timestamp = r.timestamp;
        copy.reference = r.reference;
        for (Receipt.Section s : r.sections) {
            Receipt.Section out = copy.section(s.title);
            for (String[] row : s.rows) {
                if (row == null || row.length < 2) continue;
                String value = row[1];
                boolean mono = value.startsWith("\u0001");
                String label = row[0] == null ? "" : row[0].toLowerCase(java.util.Locale.US);
                boolean money = label.contains("amount") || label.contains("value") || label.contains("fee")
                        || label.contains("fare") || label.contains("total") || label.contains("price") || label.contains("paid");
                if (money && !mono) value = com.harvana.app.utils.BalancePrivacy.mask(value);
                if (mono) out.mono(row[0], value.startsWith("\u0001") ? value.substring(1) : value);
                else out.row(row[0], value);
            }
        }
        return copy;
    }

    private static String summary(Receipt r) {
        return r.title + " " + r.number() + "\n" + (r.direction < 0 ? "- " : r.direction > 0 ? "+ " : "") + r.amount + " " + r.symbol
                + (r.status == null || r.status.isEmpty() ? "" : "\nStatus: " + r.status);
    }
}
