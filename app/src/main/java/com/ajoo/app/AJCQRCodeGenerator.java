package com.ajoo.app;

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
 * Ajoo-styled QR code generator.
 * Dark theme + emerald modules + optional center logo.
 */
public class AJCQRCodeGenerator {

	private static final String TAG = "AJCQRCodeGenerator";

	// Ajoo colors
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
	 * @param useWhiteStyle true = classic black/white, false = Ajoo dark+emerald
	 */
	public static Bitmap generateQRCode(String text, int sizePx, Bitmap logo, boolean useWhiteStyle) {
		if (text == null || text.trim().isEmpty()) {
			Log.e(TAG, "Text is null or empty");
			return null;
		}
		if (sizePx < 128) sizePx = 256;

		try {
			Map<EncodeHintType, Object> hints = new HashMap<>();
			hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
			// Higher error correction so the logo doesn't break the code
			hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
			hints.put(EncodeHintType.MARGIN, 1);

			QRCodeWriter writer = new QRCodeWriter();
			BitMatrix matrix = writer.encode(text.trim(), BarcodeFormat.QR_CODE, sizePx, sizePx, hints);

			int width  = matrix.getWidth();
			int height = matrix.getHeight();

			int bgColor     = useWhiteStyle ? COLOR_WHITE : COLOR_BG;
			int moduleColor = useWhiteStyle ? Color.BLACK : COLOR_MODULE;

			int[] pixels = new int[width * height];
			for (int y = 0; y < height; y++) {
				int offset = y * width;
				for (int x = 0; x < width; x++) {
					pixels[offset + x] = matrix.get(x, y) ? moduleColor : bgColor;
				}
			}

			Bitmap qr = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
			qr.setPixels(pixels, 0, width, 0, 0, width, height);

			// Draw logo in the center if provided
			if (logo != null && !logo.isRecycled()) {
				qr = drawCenterLogo(qr, logo);
			}

			return qr;

		} catch (WriterException e) {
			Log.e(TAG, "QR encode failed: " + e.getMessage());
			return null;
		} catch (Exception e) {
			Log.e(TAG, "Unexpected error generating QR: " + e.getMessage(), e);
			return null;
		}
	}

	/**
	 * Convenience: generate Ajoo-styled QR with app logo in the center.
	 * Pass your logo drawable resource id (e.g. R.drawable.app_main_logo_2).
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