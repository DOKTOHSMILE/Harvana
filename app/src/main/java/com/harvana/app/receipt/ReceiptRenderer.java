package com.harvana.app.receipt;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Picture;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;

import com.harvana.app.HARVQRCodeGenerator;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Draws receipts and statements. One look everywhere: an Ink Teal header with the Harvana mark, a large amount, a
 * status pill, the facts in a clean card, network details, and a footer with the receipt number and a QR code of the
 * transaction hash. Page width is 595 points (A4) and everything is laid out in those units, so the PDF and the
 * image are the same drawing.
 */
public final class ReceiptRenderer {
    private ReceiptRenderer() {}

    static final int W = 595, H = 842, M = 36;
    static final int INK = 0xFF053D3A, SAND = 0xFFFFE2B8, PAPER = 0xFFFFFFFF, CARD = 0xFFFFF8EE, LINE = 0xFFEBDCC5;
    static final int TEXT = 0xFF0B2F2C, MUTED = 0xFF587570;

    // ── single receipt ───────────────────────────────────────────────────

    /** Draws the receipt starting at the top of the canvas and returns the y where it ends. */
    static float drawReceipt(Canvas c, Receipt r) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        c.drawColor(PAPER);

        // header band
        p.setColor(INK);
        c.drawRect(0, 0, W, 104, p);
        mark(c, p, M, 28, 48);
        p.setColor(SAND); p.setTextSize(22f); p.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD)); p.setLetterSpacing(0.12f);
        c.drawText("HARVANA", M + 62, 56, p);
        p.setLetterSpacing(0f); p.setTextSize(11f); p.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        p.setColor(0xFFA9C4BE);
        c.drawText("Harvana Protocol Network", M + 62, 74, p);
        p.setTextAlign(Paint.Align.RIGHT); p.setColor(SAND); p.setTextSize(13f); p.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        c.drawText(r.title.toUpperCase(Locale.US), W - M, 54, p);
        p.setTextSize(10.5f); p.setColor(0xFFA9C4BE); p.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        c.drawText("No. " + r.number(), W - M, 72, p);
        p.setTextAlign(Paint.Align.LEFT);
        p.setColor(SAND); c.drawRect(0, 104, W, 107, p);

        // hero
        float y = 168;
        String sign = r.direction > 0 ? "+ " : r.direction < 0 ? "− " : "";
        p.setTextAlign(Paint.Align.CENTER);
        p.setColor(TEXT); p.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD)); p.setTextSize(38f);
        c.drawText(sign + r.amount + "  " + r.symbol, W / 2f, y, p);
        y += 30;
        pill(c, p, W / 2f, y, r.status);
        y += 40;
        p.setTextAlign(Paint.Align.CENTER); p.setColor(MUTED); p.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL)); p.setTextSize(12f);
        c.drawText(r.timestamp > 0 ? formatDate(r.timestamp) : "Waiting to be recorded on the network", W / 2f, y, p);
        p.setTextAlign(Paint.Align.LEFT);
        y += 34;

        // sections
        for (Receipt.Section s : r.sections) {
            if (s.rows.isEmpty()) continue;
            p.setColor(MUTED); p.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD)); p.setTextSize(10.5f); p.setLetterSpacing(0.12f);
            c.drawText(s.title.toUpperCase(Locale.US), M, y, p);
            p.setLetterSpacing(0f);
            y += 10;
            y = card(c, p, s, y) + 24;
        }

        // footer
        y = Math.max(y, 640);
        p.setColor(LINE); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(1f); p.setPathEffect(new DashPathEffect(new float[] {4, 4}, 0));
        c.drawLine(M, y, W - M, y, p);
        p.setPathEffect(null); p.setStyle(Paint.Style.FILL);
        Bitmap qr = null;
        if (r.reference != null && !r.reference.isEmpty()) {
            try { qr = HARVQRCodeGenerator.generateQRCode(com.harvana.app.scan.QrRouter.receiptPayload(r.reference), 480); } catch (Throwable ignored) { }
        }
        y += qr != null ? 40 : 26;          // room for a code large enough to scan from paper or a screen
        float textRight = qr != null ? W - M - 108 : W - M;
        p.setColor(TEXT); p.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD)); p.setTextSize(12f);
        c.drawText("Thank you for using Harvana", M, y, p);
        p.setColor(MUTED); p.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL)); p.setTextSize(10f);
        y += 17;
        for (String line : wrap("This receipt is generated from the Harvana network record. Keep the receipt number to ask about this payment. HARV is a digital asset and this is not a bank statement.", p, textRight - M)) {
            c.drawText(line, M, y, p); y += 13;
        }
        y += 6;
        c.drawText("Generated " + formatDate(System.currentTimeMillis()), M, y, p);
        if (qr != null) {
            // Drawn into a 96-point box from the full-size bitmap (not shrunk first), so the squares stay sharp.
            Paint smooth = new Paint(Paint.FILTER_BITMAP_FLAG);
            c.drawBitmap(qr, null, new android.graphics.RectF(W - M - 96, y - 96, W - M, y), smooth);
            p.setTextAlign(Paint.Align.RIGHT); p.setTextSize(8f);
            c.drawText("Scan: transaction hash", W - M, y + 6, p);
            p.setTextAlign(Paint.Align.LEFT);
        }
        y += 28;
        p.setColor(INK); c.drawRect(0, y, W, y + 6, p);
        return y + 6;
    }

    private static float card(Canvas c, Paint p, Receipt.Section s, float top) {
        // measure
        Paint v = new Paint(Paint.ANTI_ALIAS_FLAG);
        List<List<String>> lines = new ArrayList<>();
        List<Boolean> mono = new ArrayList<>();
        float valueW = W - 2 * M - 40 - 128;
        float h = 8;
        for (String[] row : s.rows) {
            boolean m = row[1].startsWith("\u0001");
            String val = m ? row[1].substring(1) : row[1];
            v.setTypeface(m ? Typeface.MONOSPACE : Typeface.create("sans-serif-medium", Typeface.NORMAL));
            v.setTextSize(m ? 10.5f : 12f);
            List<String> w = wrap(val, v, valueW);
            lines.add(w); mono.add(m);
            h += Math.max(36, 20 + w.size() * 15);
        }
        RectF box = new RectF(M, top, W - M, top + h);
        p.setColor(CARD); c.drawRoundRect(box, 14, 14, p);
        p.setColor(LINE); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(1f); c.drawRoundRect(box, 14, 14, p); p.setStyle(Paint.Style.FILL);
        float y = top + 4;
        for (int i = 0; i < s.rows.size(); i++) {
            List<String> w = lines.get(i);
            float rowH = Math.max(36, 20 + w.size() * 15);
            p.setColor(MUTED); p.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL)); p.setTextSize(11f);
            c.drawText(s.rows.get(i)[0], M + 20, y + 23, p);
            p.setColor(TEXT);
            p.setTypeface(mono.get(i) ? Typeface.MONOSPACE : Typeface.create("sans-serif-medium", Typeface.NORMAL));
            p.setTextSize(mono.get(i) ? 10.5f : 12f);
            p.setTextAlign(Paint.Align.RIGHT);
            float ly = y + 23;
            for (String line : w) { c.drawText(line, W - M - 20, ly, p); ly += 15; }
            p.setTextAlign(Paint.Align.LEFT);
            y += rowH;
            if (i < s.rows.size() - 1) { p.setColor(LINE); c.drawLine(M + 20, y, W - M - 20, y, p); }
        }
        return top + h;
    }

    private static void pill(Canvas c, Paint p, float cx, float baseline, String status) {
        String st = status == null ? "" : status.toUpperCase(Locale.US);
        int fg, bg; String label;
        if ("CONFIRMED".equals(st) || "SUCCESS".equals(st)) { fg = 0xFF17724A; bg = 0xFFE3F4EA; label = "✓  CONFIRMED"; }
        else if ("PENDING".equals(st)) { fg = 0xFF8A5A12; bg = 0xFFFBEFD5; label = "●  PENDING"; }
        else if (st.isEmpty()) { fg = MUTED; bg = LINE; label = "RECORDED"; }
        else { fg = 0xFFA8322E; bg = 0xFFFBE4E3; label = st; }
        p.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD)); p.setTextSize(12f); p.setLetterSpacing(0.08f);
        float tw = p.measureText(label);
        RectF r = new RectF(cx - tw / 2 - 16, baseline - 17, cx + tw / 2 + 16, baseline + 8);
        p.setColor(bg); c.drawRoundRect(r, 13, 13, p);
        p.setColor(fg); p.setTextAlign(Paint.Align.CENTER); c.drawText(label, cx, baseline, p);
        p.setLetterSpacing(0f); p.setTextAlign(Paint.Align.LEFT);
    }

    /** The Harvana coin, the same picture used everywhere else. */
    private static void mark(Canvas c, Paint p, float x, float y, float size) {
        android.content.Context ctx = com.harvana.app.SketchApplication.getContext();
        android.graphics.Bitmap coin = null;
        if (ctx != null) {
            try { coin = android.graphics.BitmapFactory.decodeResource(ctx.getResources(), com.harvana.app.R.drawable.ic_coin_harv); }
            catch (Exception ignored) {}
        }
        if (coin != null) {
            Paint smooth = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
            c.drawBitmap(coin, null, new RectF(x, y, x + size, y + size), smooth);
            return;
        }
        p.setColor(SAND); c.drawCircle(x + size / 2, y + size / 2, size / 2, p);
    }

    // ── statement (many transactions) ────────────────────────────────────

    public static final class Row {
        public String when = "", what = "", detail = "", amount = "", status = "";
        public int direction;
    }

    private static final int ROW_H = 44, FIRST_TOP = 232, NEXT_TOP = 110, BOTTOM = 790;

    /** A multi-page A4 statement in the same style. */
    public static File renderStatement(Context ctx, String title, String subtitle, List<Row> rows, String totalIn, String totalOut, File out) throws Exception {
        int firstCap = (BOTTOM - FIRST_TOP) / ROW_H, nextCap = (BOTTOM - NEXT_TOP) / ROW_H;
        int pages = 1;
        int left = Math.max(0, rows.size() - firstCap);
        while (left > 0) { pages++; left -= nextCap; }
        PdfDocument doc = new PdfDocument();
        int idx = 0;
        for (int pg = 1; pg <= pages; pg++) {
            PdfDocument.Page page = doc.startPage(new PdfDocument.PageInfo.Builder(W, H, pg).create());
            Canvas c = page.getCanvas();
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            c.drawColor(PAPER);
            p.setColor(INK); c.drawRect(0, 0, W, pg == 1 ? 104 : 74, p);
            mark(c, p, M, pg == 1 ? 28 : 14, pg == 1 ? 48 : 44);
            p.setColor(SAND); p.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD)); p.setTextSize(pg == 1 ? 22f : 18f); p.setLetterSpacing(0.12f);
            c.drawText("HARVANA", M + 62, pg == 1 ? 56 : 44, p);
            p.setLetterSpacing(0f);
            p.setTextAlign(Paint.Align.RIGHT); p.setTextSize(13f);
            c.drawText(title.toUpperCase(Locale.US), W - M, pg == 1 ? 54 : 42, p);
            p.setColor(0xFFA9C4BE); p.setTextSize(10.5f); p.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
            c.drawText(subtitle, W - M, pg == 1 ? 72 : 58, p);
            p.setTextAlign(Paint.Align.LEFT);
            p.setColor(SAND); c.drawRect(0, pg == 1 ? 104 : 74, W, pg == 1 ? 107 : 77, p);
            float y;
            int cap;
            if (pg == 1) {
                // summary strip
                RectF box = new RectF(M, 128, W - M, 196);
                p.setColor(CARD); c.drawRoundRect(box, 14, 14, p);
                p.setColor(LINE); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(1f); c.drawRoundRect(box, 14, 14, p); p.setStyle(Paint.Style.FILL);
                String[][] cells = {{"TRANSACTIONS", String.valueOf(rows.size())}, {"MONEY IN", totalIn}, {"MONEY OUT", totalOut}};
                for (int i = 0; i < 3; i++) {
                    float cx = M + 20 + i * ((W - 2 * M - 40) / 3f);
                    p.setColor(MUTED); p.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD)); p.setTextSize(9f); p.setLetterSpacing(0.1f);
                    c.drawText(cells[i][0], cx, 152, p); p.setLetterSpacing(0f);
                    p.setColor(TEXT); p.setTextSize(15f); c.drawText(cells[i][1], cx, 176, p);
                }
                y = FIRST_TOP - 20; cap = firstCap;
            } else { y = NEXT_TOP - 10; cap = nextCap; }
            // column heads
            p.setColor(MUTED); p.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD)); p.setTextSize(9f); p.setLetterSpacing(0.1f);
            c.drawText("DATE", M, y, p); c.drawText("DESCRIPTION", M + 96, y, p);
            p.setTextAlign(Paint.Align.RIGHT); c.drawText("AMOUNT", W - M, y, p); p.setTextAlign(Paint.Align.LEFT); p.setLetterSpacing(0f);
            y += 8;
            p.setColor(LINE); c.drawLine(M, y, W - M, y, p);
            for (int k = 0; k < cap && idx < rows.size(); k++, idx++) {
                Row r = rows.get(idx);
                if (k % 2 == 0) { p.setColor(0xFFFFFBF4); c.drawRect(M, y, W - M, y + ROW_H, p); }
                p.setColor(MUTED); p.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL)); p.setTextSize(10f);
                c.drawText(r.when, M + 4, y + 19, p);
                p.setColor(TEXT); p.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD)); p.setTextSize(11.5f);
                c.drawText(clip(r.what, p, 250), M + 96, y + 19, p);
                p.setColor(MUTED); p.setTypeface(Typeface.MONOSPACE); p.setTextSize(9f);
                c.drawText(clip(r.detail, p, 330), M + 96, y + 34, p);
                p.setTextAlign(Paint.Align.RIGHT);
                p.setColor(r.direction > 0 ? 0xFF17724A : TEXT); p.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD)); p.setTextSize(12f);
                c.drawText((r.direction > 0 ? "+ " : r.direction < 0 ? "− " : "") + r.amount, W - M - 4, y + 19, p);
                p.setColor(MUTED); p.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL)); p.setTextSize(9f);
                c.drawText(r.status, W - M - 4, y + 34, p);
                p.setTextAlign(Paint.Align.LEFT);
                y += ROW_H;
                p.setColor(LINE); c.drawLine(M, y, W - M, y, p);
            }
            // page footer
            p.setColor(MUTED); p.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL)); p.setTextSize(9f);
            c.drawText("Generated " + formatDate(System.currentTimeMillis()) + "  ·  Harvana Protocol Network", M, H - 26, p);
            p.setTextAlign(Paint.Align.RIGHT); c.drawText("Page " + pg + " of " + pages, W - M, H - 26, p); p.setTextAlign(Paint.Align.LEFT);
            p.setColor(INK); c.drawRect(0, H - 8, W, H, p);
            doc.finishPage(page);
        }
        try (FileOutputStream fos = new FileOutputStream(out)) { doc.writeTo(fos); }
        doc.close();
        return out;
    }

    // ── single receipt outputs ───────────────────────────────────────────

    public static File renderPdf(Context ctx, Receipt r, File out) throws Exception {
        PdfDocument doc = new PdfDocument();
        PdfDocument.Page page = doc.startPage(new PdfDocument.PageInfo.Builder(W, H, 1).create());
        drawReceipt(page.getCanvas(), r);
        doc.finishPage(page);
        try (FileOutputStream fos = new FileOutputStream(out)) { doc.writeTo(fos); }
        doc.close();
        return out;
    }

    /** The same drawing as an image, cropped to its content and drawn at 3x so it stays sharp when zoomed. */
    public static File renderPng(Context ctx, Receipt r, File out) throws Exception {
        Picture pic = new Picture();
        Canvas rec = pic.beginRecording(W, 2000);
        float end = drawReceipt(rec, r);
        pic.endRecording();
        int scale = 3;
        Bitmap bmp = Bitmap.createBitmap(W * scale, Math.round(end * scale), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        c.scale(scale, scale);
        c.drawColor(Color.WHITE);
        c.drawPicture(pic);
        try (FileOutputStream fos = new FileOutputStream(out)) { bmp.compress(Bitmap.CompressFormat.PNG, 100, fos); }
        bmp.recycle();
        return out;
    }

    // ── text helpers ─────────────────────────────────────────────────────

    static String formatDate(long ms) {
        return new SimpleDateFormat("d MMM yyyy, h:mm a", Locale.US).format(new Date(ms));
    }

    static List<String> wrap(String text, Paint p, float maxW) {
        List<String> out = new ArrayList<>();
        String rest = text == null ? "" : text;
        while (!rest.isEmpty()) {
            int n = p.breakText(rest, true, maxW, null);
            if (n >= rest.length()) { out.add(rest); break; }
            int cut = n;
            int space = rest.lastIndexOf(' ', n);
            if (space > n / 2) cut = space + 1;
            out.add(rest.substring(0, cut).trim());
            rest = rest.substring(cut);
        }
        if (out.isEmpty()) out.add("");
        return out;
    }

    static String clip(String s, Paint p, float maxW) {
        if (s == null) return "";
        if (p.measureText(s) <= maxW) return s;
        int n = p.breakText(s, true, maxW - p.measureText("…"), null);
        return s.substring(0, Math.max(0, n)) + "…";
    }
}
