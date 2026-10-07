package com.harvana.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.HashMap;
import java.util.Map;

/**
 * Harvana-styled QR code generator.
 * Dark theme + emerald modules + optional center logo.
 */
public class HARVQRCodeGenerator {

	private static final String TAG = "HARVQRCodeGenerator";

	// Harvana colors
	private static final int COLOR_BG       = Color.parseColor("#0F1623"); // dark navy
	private static final int COLOR_MODULE   = Color.parseColor("#10B981"); // emerald
	private static final int COLOR_WHITE    = Color.WHITE;

	/**
	 * Generate a dark-theme QR code (no logo).
	 *
	 * @param text   content (wallet address, etc.)
	 * @param sizePx size in pixels (e.g. 512)
	 */
	public static Bitmap generateQRCode(String text, int sizePx) {
		return generateQRCode(text, sizePx, null, false);
	}

	/** Default size 512 px */
	public static Bitmap generateQRCode(String text) {
		return generateQRCode(text, 512, null, false);
	}

	/**
	 * Generate QR with optional center logo.
	 *
	 * @param text          content to encode
	 * @param sizePx        final bitmap size
	 * @param logo          center logo (can be null)
	 * @param useWhiteStyle true = classic black on white, false = Harvana navy on white (both scan everywhere)
	 */
	public static Bitmap generateQRCode(String text, int sizePx, Bitmap logo, boolean useWhiteStyle) {
		// One design for every code the app makes (receive, coin page, profile, receipts, access card), whatever the
		// caller asked for: Harvana ink modules on white, rounded, with teal finder eyes and the HARV coin in the middle.
		return unified(text, sizePx, logo);
	}

	/** Harvana ink (the receipt's ink) and teal, on white: dark on light, so every scanner reads it. */
	static final int INK = 0xFF053D3A, TEAL = 0xFF0B6B61, PAPER = Color.WHITE;

	/**
	 * The one Harvana QR style. Modules are drawn one by one as rounded squares; the three finder patterns as rounded
	 * rings with a solid centre; a quiet border of 2 modules; the coin badge covers under 7% of the area, well inside
	 * what error correction H (30%) recovers.
	 */
	public static Bitmap unified(String text, int sizePx, Bitmap logo) {
		if (text == null || text.trim().isEmpty()) {
			Log.e(TAG, "Text is null or empty");
			return null;
		}
		if (sizePx < 128) sizePx = 256;
		try {
			Map<EncodeHintType, Object> hints = new HashMap<>();
			hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
			hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
			hints.put(EncodeHintType.MARGIN, 0);
			BitMatrix m = new QRCodeWriter().encode(text.trim(), BarcodeFormat.QR_CODE, 0, 0, hints);   // one cell per module
			int n = m.getWidth();
			int quiet = 2;
			float cell = sizePx / (float) (n + 2 * quiet);
			float off = quiet * cell;

			Bitmap out = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
			Canvas c = new Canvas(out);
			c.drawColor(PAPER);
			Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
			p.setColor(INK);
			float r = cell * 0.32f;
			for (int y = 0; y < n; y++) {
				for (int x = 0; x < n; x++) {
					if (!m.get(x, y) || inFinder(x, y, n)) continue;
					float l = off + x * cell, t = off + y * cell;
					c.drawRoundRect(new RectF(l + cell * 0.04f, t + cell * 0.04f, l + cell * 0.96f, t + cell * 0.96f), r, r, p);
				}
			}
			drawEye(c, off, off, cell);
			drawEye(c, off + (n - 7) * cell, off, cell);
			drawEye(c, off, off + (n - 7) * cell, cell);

			float badge = sizePx * 0.2f;
			float cx = sizePx / 2f, cy = sizePx / 2f;
			Paint w = new Paint(Paint.ANTI_ALIAS_FLAG);
			w.setColor(PAPER);
			c.drawCircle(cx, cy, badge / 2f + cell * 0.8f, w);
			if (logo != null && !logo.isRecycled()) {
				Bitmap sl = Bitmap.createScaledBitmap(logo, (int) badge, (int) badge, true);
				c.drawBitmap(sl, cx - badge / 2f, cy - badge / 2f, null);
			} else {
				drawCoin(c, cx, cy, badge / 2f);
			}
			return out;
		} catch (WriterException e) {
			Log.e(TAG, "QR encode failed: " + e.getMessage());
			return null;
		} catch (Exception e) {
			Log.e(TAG, "Unexpected error generating QR: " + e.getMessage(), e);
			return null;
		}
	}

	private static boolean inFinder(int x, int y, int n) {
		return (x < 7 && y < 7) || (x >= n - 7 && y < 7) || (x < 7 && y >= n - 7);
	}

	/** A finder eye: a 7-module rounded ring in teal and a 3-module rounded centre in ink. */
	private static void drawEye(Canvas c, float l, float t, float cell) {
		Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
		p.setStyle(Paint.Style.STROKE);
		p.setStrokeWidth(cell);
		p.setColor(TEAL);
		float h = cell / 2f;
		c.drawRoundRect(new RectF(l + h, t + h, l + 7 * cell - h, t + 7 * cell - h), cell * 1.6f, cell * 1.6f, p);
		p.setStyle(Paint.Style.FILL);
		p.setColor(INK);
		c.drawRoundRect(new RectF(l + 2 * cell, t + 2 * cell, l + 5 * cell, t + 5 * cell), cell * 0.9f, cell * 0.9f, p);
	}

	/** The HARV coin, drawn: coral-to-gold disc with a darker edge and a white H (matches ic_harv_coin). */
	private static void drawCoin(Canvas c, float cx, float cy, float rad) {
		Paint edge = new Paint(Paint.ANTI_ALIAS_FLAG);
		edge.setShader(new android.graphics.LinearGradient(cx - rad, 0, cx + rad, 0, 0xFFB8423E, 0xFFC79200, android.graphics.Shader.TileMode.CLAMP));
		c.drawCircle(cx, cy + rad * 0.07f, rad, edge);
		Paint face = new Paint(Paint.ANTI_ALIAS_FLAG);
		face.setShader(new android.graphics.LinearGradient(cx - rad, 0, cx + rad, 0,
				new int[]{0xFFFF6F6B, 0xFFFF9A38, 0xFFFFC907}, null, android.graphics.Shader.TileMode.CLAMP));
		c.drawCircle(cx, cy - rad * 0.03f, rad * 0.97f, face);
		Paint h = new Paint(Paint.ANTI_ALIAS_FLAG);
		h.setColor(PAPER);
		float bw = rad * 0.22f, hh = rad * 0.9f, gap = rad * 0.3f, top = cy - hh / 2f - rad * 0.03f;
		c.drawRect(cx - gap - bw, top, cx - gap, top + hh, h);
		c.drawRect(cx + gap, top, cx + gap + bw, top + hh, h);
		c.drawRect(cx - gap, top + hh * 0.4f, cx + gap, top + hh * 0.6f, h);
	}

	/**
	 * Convenience: generate Harvana-styled QR with app logo in the center.
	 * Pass your logo drawable resource id (e.g. R.drawable.ic_coin_harv).
	 */
	public static Bitmap generateQRCodeWithLogo(Context context, String text, int sizePx, int logoResId) {
		Bitmap logo = null;
		try {
			logo = BitmapFactory.decodeResource(context.getResources(), logoResId);
		} catch (Exception e) {
			Log.w(TAG, "Could not load logo resource: " + e.getMessage());
		}
		return generateQRCode(text, sizePx, logo, false);
	}

	/** Classic black & white (for maximum scanner compatibility) */
	public static Bitmap generateClassicQRCode(String text, int sizePx) {
		return generateQRCode(text, sizePx, null, true);
	}

	// ── Internal helpers ─────────────────────────────────────────────────────

	private static Bitmap drawCenterLogo(Bitmap qr, Bitmap logo) {
		int qrSize = qr.getWidth();
		// Logo should be \~20-22% of QR size so it stays scannable
		int logoSize = (int) (qrSize * 0.22f);

		Bitmap scaledLogo = Bitmap.createScaledBitmap(logo, logoSize, logoSize, true);

		Bitmap result = qr.copy(Bitmap.Config.ARGB_8888, true);
		Canvas canvas = new Canvas(result);

		// White rounded background behind the logo for contrast
		float left = (qrSize - logoSize) / 2f;
		float top  = (qrSize - logoSize) / 2f;
		float pad  = logoSize * 0.12f;

		Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		bgPaint.setColor(Color.WHITE);
		bgPaint.setStyle(Paint.Style.FILL);

		RectF bgRect = new RectF(
				left - pad,
				top - pad,
				left + logoSize + pad,
				top + logoSize + pad
		);
		canvas.drawRoundRect(bgRect, pad, pad, bgPaint);

		// Draw the logo
		canvas.drawBitmap(scaledLogo, left, top, null);

		if (scaledLogo != logo) {
			scaledLogo.recycle();
		}

		return result;
	}
}