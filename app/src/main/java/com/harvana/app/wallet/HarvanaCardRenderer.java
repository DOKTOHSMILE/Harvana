package com.harvana.app.wallet;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.core.content.ContextCompat;

import com.harvana.app.HARVQRCodeGenerator;
import com.harvana.app.R;

import java.io.FileOutputStream;
import java.io.OutputStream;

public final class HarvanaCardRenderer {
	
	private static final int CARD_WIDTH_PX = 1013;
	private static final int CARD_HEIGHT_PX = 638;
	private static final float CORNER_RADIUS_PX = 40f;
	
	private HarvanaCardRenderer() {}
	
	// ── FRONT ────────────────────────────────────────────────────────────
	
	public static Bitmap renderFront(Context context, CardTemplate template, String walletAddress, String cardOwnerName) {
		Bitmap bitmap = Bitmap.createBitmap(CARD_WIDTH_PX, CARD_HEIGHT_PX, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		clipToRoundedRect(canvas);
		
		// Updated to pass Context so it can load the thematic background images
		drawBackground(context, canvas, template);
		
		float margin = 56f;
		
		// Title block: the card title and the line under it. The logo is a square coin, so it is drawn square, as tall as that block,
		// instead of being stretched into a wide box as before.
		Paint subtitle1 = new Paint(Paint.ANTI_ALIAS_FLAG);
		subtitle1.setColor(template.textPrimary);
		subtitle1.setTextSize(20f);
		subtitle1.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD));
		Paint subtitle2 = new Paint(Paint.ANTI_ALIAS_FLAG);
		subtitle2.setColor(template.textSecondary);
		subtitle2.setTextSize(14f);
		final float titleBaseline = 60f, subtitleBaseline = 82f;
		float blockTop = titleBaseline + subtitle1.getFontMetrics().ascent;           // top of the title's tallest letters
		float blockBottom = subtitleBaseline + subtitle2.getFontMetrics().descent;    // bottom of the line under it
		float logoSize = blockBottom - blockTop;
		drawAppIcon(context, canvas, margin, blockTop, logoSize, logoSize);
		float textX = margin + logoSize + 16f;
		canvas.drawText("Harvana Access Card", textX, titleBaseline, subtitle1);
		canvas.drawText("Encrypted wallet backup", textX, subtitleBaseline, subtitle2);
		
		// Top Right Card Tier
		Paint badgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		badgePaint.setColor(template.accent);
		badgePaint.setTextSize(20f);
		badgePaint.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD));
		badgePaint.setTextAlign(Paint.Align.RIGHT);
		canvas.drawText(template.displayName.toUpperCase(), CARD_WIDTH_PX - margin, 76f, badgePaint);
		
		// Wallet Address Section
		Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		labelPaint.setColor(template.textSecondary);
		labelPaint.setTextSize(14f);
		canvas.drawText("W A L L E T   A D D R E S S", margin, 210f, labelPaint);
		
		Paint addrPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		addrPaint.setColor(template.textPrimary);
		addrPaint.setTextSize(36f);
		addrPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
		addrPaint.setLetterSpacing(0.04f); // Adds luxurious horizontal spread
		
		// Case-insensitive strip of any existing HV0x variation
		String cleanAddress = walletAddress != null ? walletAddress.replaceAll("(?i)^HV0X", "").toUpperCase() : "";
		
		if (cleanAddress.length() == 40) {
			// New 3-Row format, well-spaced
			String row1 = String.format("HV0x    %s   %s",
			cleanAddress.substring(0, 5), cleanAddress.substring(5, 10));
			String row2 = String.format("%s   %s   %s",
			cleanAddress.substring(10, 15), cleanAddress.substring(15, 20), cleanAddress.substring(20, 25));
			String row3 = String.format("%s   %s   %s",
			cleanAddress.substring(25, 30), cleanAddress.substring(30, 35), cleanAddress.substring(35, 40));
			
			canvas.drawText(row1, margin, 270f, addrPaint);
			canvas.drawText(row2, margin, 325f, addrPaint);
			canvas.drawText(row3, margin, 380f, addrPaint);
		} else {
			// Fallback
			canvas.drawText("HV0x " + cleanAddress, margin, 270f, addrPaint);
		}
		
		// Card Owner
		Paint ownerLabel = new Paint(Paint.ANTI_ALIAS_FLAG);
		ownerLabel.setColor(template.textSecondary);
		ownerLabel.setTextSize(12f);
		canvas.drawText("CARD OWNER", margin, 450f, ownerLabel);
		
		Paint ownerNamePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		ownerNamePaint.setColor(template.textPrimary);
		ownerNamePaint.setTextSize(24f);
		ownerNamePaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL));
		
		String finalName = (cardOwnerName != null && !cardOwnerName.trim().isEmpty()) ? cardOwnerName.toUpperCase() : "HARVANA MEMBER";
		
		// Safeguard: Truncate impossibly long names so they never bleed into the expiry date
		if (finalName.length() > 24) {
			finalName = finalName.substring(0, 22) + "...";
		}
		
		canvas.drawText(finalName, margin, 480f, ownerNamePaint);
		
		// Validity Block - Shifted significantly to the right (from +350f to +600f)
		Paint expiryLabel = new Paint(Paint.ANTI_ALIAS_FLAG);
		expiryLabel.setColor(template.textSecondary);
		expiryLabel.setTextSize(12f);
		canvas.drawText("VALID THROUGH", margin + 600f, 450f, expiryLabel);
		
		Paint expiryValue = new Paint(Paint.ANTI_ALIAS_FLAG);
		expiryValue.setColor(template.textPrimary);
		expiryValue.setTextSize(20f);
		expiryValue.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
		canvas.drawText("INFINITY", margin + 600f, 480f, expiryValue);
		
		// Footer & Faint Silver Stamp
		Paint footerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		footerPaint.setColor(template.textSecondary);
		footerPaint.setTextSize(16f);
		canvas.drawText("HARVANA NETWORK", margin, CARD_HEIGHT_PX - 40f, footerPaint);
		
		// Draw the custom signature seal at the bottom right
		drawSignatureStamp(canvas, CARD_WIDTH_PX - margin - 100f, CARD_HEIGHT_PX - margin - 100f);
		
		if (template.metallic) {
			drawMetallicSheen(canvas);
		}
		
		return bitmap;
	}
	
	// ── BACK ─────────────────────────────────────────────────────────────
	
	public static Bitmap renderBack(Context context, CardTemplate template, String qrPayload, String digitCode) {
		Bitmap bitmap = Bitmap.createBitmap(CARD_WIDTH_PX, CARD_HEIGHT_PX, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		clipToRoundedRect(canvas);
		
		// Updated to pass Context
		drawBackground(context, canvas, template);
		
		Paint stripePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		stripePaint.setColor(Color.argb(230, 10, 10, 10));
		canvas.drawRect(0, 50, CARD_WIDTH_PX, 170, stripePaint);
		
		float margin = 56f;
		
		// ── SOCIAL RECOVERY DISCLAIMER (Updated) ──
		Paint warnLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		warnLabelPaint.setColor(template.accent);
		warnLabelPaint.setTextSize(20f);
		warnLabelPaint.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD));
		canvas.drawText("SOCIAL RECOVERY ENABLED", margin, 205f, warnLabelPaint);
		
		Paint warnBodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		warnBodyPaint.setColor(template.textSecondary);
		warnBodyPaint.setTextSize(16f);
		canvas.drawText("The wallet access card is fully encrypted and safe to share", margin, 235f, warnBodyPaint);
		canvas.drawText("for social backup. Assets cannot be accessed without the", margin, 260f, warnBodyPaint);
		canvas.drawText("custodian's decryption password.", margin, 285f, warnBodyPaint);
		
		Paint digitLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		digitLabelPaint.setColor(template.textSecondary);
		digitLabelPaint.setTextSize(14f);
		canvas.drawText("BACKUP CODE (if QR can't be scanned)", margin, 325f, digitLabelPaint);
		
		if (digitCode != null && digitCode.length() == HarvanaCardCrypto.DIGIT_CODE_LENGTH) {
			Paint digitPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
			digitPaint.setColor(template.textPrimary);
			digitPaint.setTextSize(17f);
			digitPaint.setTypeface(Typeface.MONOSPACE);
			
			String[] groups = new String[22];
			for (int i = 0; i < 22; i++) {
				int start = i * 5;
				int end = Math.min(start + 5, digitCode.length());
				groups[i] = digitCode.substring(start, end);
			}
			
			// Adjusted spacing downwards to clear the new 3-line text
			float colWidth = 145f;
			float rowHeight = 32f;
			float startY = 365f; 
			
			for (int i = 0; i < groups.length; i++) {
				int col = i % 4;
				int row = i / 4;
				float x = margin + col * colWidth;
				float y = startY + row * rowHeight;
				canvas.drawText(groups[i], x, y, digitPaint);
			}
		}
		
		// QR code
		int qrSize = 280;
		Bitmap qr = HARVQRCodeGenerator.generateClassicQRCode(qrPayload, qrSize);
		if (qr != null) {
			float qrLeft = CARD_WIDTH_PX - qrSize - margin - 16f;
			float qrTop = 195f;
			float pad = 12f;
			
			Paint framePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
			framePaint.setColor(template.accent);
			RectF frameRect = new RectF(qrLeft - pad - 4f, qrTop - pad - 4f,
			qrLeft + qrSize + pad + 4f, qrTop + qrSize + pad + 4f);
			canvas.drawRoundRect(frameRect, 14f, 14f, framePaint);
			
			Paint qrBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
			qrBgPaint.setColor(Color.WHITE);
			RectF qrBgRect = new RectF(qrLeft - pad, qrTop - pad, qrLeft + qrSize + pad, qrTop + qrSize + pad);
			canvas.drawRoundRect(qrBgRect, 10f, 10f, qrBgPaint);
			
			canvas.drawBitmap(qr, qrLeft, qrTop, null);
		}
		
		// Card Type Label
		Paint stampPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		stampPaint.setColor(template.textSecondary);
		stampPaint.setTextSize(14f);
		stampPaint.setTypeface(Typeface.MONOSPACE);
		canvas.drawText("CARD TYPE: " + template.displayName.toUpperCase(), margin, CARD_HEIGHT_PX - 40f, stampPaint);
		
		if (template.metallic) {
			drawMetallicSheen(canvas);
		}
		
		return bitmap;
	}
	
	// ── Shared drawing helpers ───────────────────────────────────────────
	
	private static void clipToRoundedRect(Canvas canvas) {
		Path clip = new Path();
		clip.addRoundRect(0, 0, CARD_WIDTH_PX, CARD_HEIGHT_PX,
		CORNER_RADIUS_PX, CORNER_RADIUS_PX, Path.Direction.CW);
		canvas.clipPath(clip);
	}
	
	// Updated Background Method to support Thematic Image Drawables (Nature, Matrix, Cyber)
	private static void drawBackground(Context context, Canvas canvas, CardTemplate template) {
		if (template.backgroundDrawableId != 0) {
			// If this is a thematic card (Oasis, Node, Mint), draw the image stretching the canvas
			Drawable bg = ContextCompat.getDrawable(context, template.backgroundDrawableId);
			if (bg != null) {
				bg.setBounds(0, 0, CARD_WIDTH_PX, CARD_HEIGHT_PX);
				bg.draw(canvas);
				
				// Optional: Add a very faint dark overlay to ensure white text stays readable over complex images
				Paint overlay = new Paint();
				overlay.setColor(Color.BLACK);
				overlay.setAlpha(40);
				canvas.drawRect(0, 0, CARD_WIDTH_PX, CARD_HEIGHT_PX, overlay);
				return;
			}
		}
		
		// Fallback for Classic, Obsidian, Gold (Solid colors / Gradients)
		Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		LinearGradient gradient = new LinearGradient(
		0, 0, CARD_WIDTH_PX, CARD_HEIGHT_PX,
		template.gradientColors, null, Shader.TileMode.CLAMP);
		bgPaint.setShader(gradient);
		canvas.drawRect(0, 0, CARD_WIDTH_PX, CARD_HEIGHT_PX, bgPaint);
		
		// Guilloche rings for plain gradient cards
		Paint watermark = new Paint(Paint.ANTI_ALIAS_FLAG);
		watermark.setColor(template.textPrimary);
		watermark.setAlpha(12);
		watermark.setStyle(Paint.Style.STROKE);
		watermark.setStrokeWidth(2f);
		for (int i = 0; i < 7; i++) {
			canvas.drawCircle(CARD_WIDTH_PX, CARD_HEIGHT_PX / 2f, 150 + (i * 90), watermark);
			canvas.drawCircle(0, CARD_HEIGHT_PX, 200 + (i * 120), watermark);
		}
	}
	
	/**
     * Programmatically draws a solid silver square stamp with repeating micro-watermark text
     */	
	private static void drawSignatureStamp(Canvas canvas, float x, float y) {
		float stampSize = 100f; // Perfect square
		float cornerRadius = 15f; // ~5dp at canvas scale
		RectF stampRect = new RectF(x, y, x + stampSize, y + stampSize);
		
		canvas.save();
		
		// 1. Draw the thin, solid silver border (1dp) - Removed the background fill completely
		Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		borderPaint.setColor(Color.parseColor("#C0C0C0")); // Solid silver, 100% opacity
		borderPaint.setStyle(Paint.Style.STROKE);
		borderPaint.setStrokeWidth(2f); // ~1dp at canvas scale
		canvas.drawRoundRect(stampRect, cornerRadius, cornerRadius, borderPaint);
		
		// 2. Clip the canvas so the repeating text stays strictly inside the square
		Path clipPath = new Path();
		clipPath.addRoundRect(stampRect, cornerRadius, cornerRadius, Path.Direction.CW);
		canvas.clipPath(clipPath);
		
		// 3. Draw the repeating text watermark in solid silver - Shrunk to 10f (~4dp)
		Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		textPaint.setColor(Color.parseColor("#C0C0C0")); // Solid silver, 100% opacity
		textPaint.setTextSize(10f); // MICRO-PRINT SIZE (~4dp)
		textPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
		
		// Rotate the text slightly for that authentic stamp/seal look
		canvas.rotate(-15f, x + (stampSize / 2), y + (stampSize / 2));
		
		// Fill the area with repeating "HARVANACHAIN"
		for (int row = -2; row < 8; row++) {
			for (int col = -2; col < 5; col++) {
				canvas.drawText("HARVANACHAIN ", x + (col * 70f), y + (row * 16f), textPaint);
			}
		}
		
		canvas.restore();
	}
	
	private static void drawMetallicSheen(Canvas canvas) {
		Paint sheenPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		LinearGradient sheen = new LinearGradient(
		0, 0, CARD_WIDTH_PX * 0.6f, CARD_HEIGHT_PX,
		new int[]{Color.argb(0, 255, 255, 255), Color.argb(35, 255, 255, 255), Color.argb(0, 255, 255, 255)},
		new float[]{0.35f, 0.5f, 0.65f},
		Shader.TileMode.CLAMP);
		sheenPaint.setShader(sheen);
		canvas.drawRect(0, 0, CARD_WIDTH_PX, CARD_HEIGHT_PX, sheenPaint);
	}
	
	private static void drawAppIcon(Context context, Canvas canvas, float x, float y, float w, float h) {
		Drawable icon = ContextCompat.getDrawable(context, R.drawable.ic_coin_harv);
		if (icon == null) return;
		canvas.save();
		icon.setBounds((int) x, (int) y, (int) (x + w), (int) (y + h));
		icon.setAlpha(178);
		icon.draw(canvas);
		canvas.restore();
	}
	
	// ── Export ───────────────────────────────────────────────────────────
	
	public interface ExportCallback {
		void onSuccess(Uri savedUri);
		void onError(String error);
	}
	
	public static void saveAsPng(Context context, Bitmap bitmap, String displayName, ExportCallback callback) {
		try {
			Uri uri;
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
				ContentValues values = new ContentValues();
				values.put(MediaStore.Images.Media.DISPLAY_NAME, displayName + ".png");
				values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
				values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Harvana");
				uri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
			} else {
				java.io.File dir = new java.io.File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Harvana");
				if (!dir.exists()) dir.mkdirs();
				java.io.File file = new java.io.File(dir, displayName + ".png");
				uri = Uri.fromFile(file);
			}
			if (uri == null) { callback.onError("Couldn't create the image file."); return; }
			try (OutputStream out = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ? context.getContentResolver().openOutputStream(uri) : new FileOutputStream(uri.getPath())) {
				if (out == null) { callback.onError("Couldn't open the image file for writing."); return; }
				bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
			}
			callback.onSuccess(uri);
		} catch (Exception e) { callback.onError(e.getMessage() != null ? e.getMessage() : "Failed to save image."); }
	}
	
	public static void saveAsPdf(Context context, Bitmap front, Bitmap back, String displayName, ExportCallback callback) {
		try {
			PdfDocument document = new PdfDocument();
			PdfDocument.PageInfo frontInfo = new PdfDocument.PageInfo.Builder(front.getWidth(), front.getHeight(), 1).create();
			PdfDocument.Page frontPage = document.startPage(frontInfo);
			frontPage.getCanvas().drawBitmap(front, 0, 0, null);
			document.finishPage(frontPage);
			
			PdfDocument.PageInfo backInfo = new PdfDocument.PageInfo.Builder(back.getWidth(), back.getHeight(), 2).create();
			PdfDocument.Page backPage = document.startPage(backInfo);
			backPage.getCanvas().drawBitmap(back, 0, 0, null);
			document.finishPage(backPage);
			
			Uri uri;
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
				ContentValues values = new ContentValues();
				values.put(MediaStore.Downloads.DISPLAY_NAME, displayName + ".pdf");
				values.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
				values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Harvana");
				uri = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
			} else {
				java.io.File dir = new java.io.File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Harvana");
				if (!dir.exists()) dir.mkdirs();
				java.io.File file = new java.io.File(dir, displayName + ".pdf");
				uri = Uri.fromFile(file);
			}
			if (uri == null) { document.close(); callback.onError("Couldn't create the PDF file."); return; }
			try (OutputStream out = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ? context.getContentResolver().openOutputStream(uri) : new FileOutputStream(uri.getPath())) {
				if (out == null) { document.close(); callback.onError("Couldn't open the PDF file for writing."); return; }
				document.writeTo(out);
			}
			document.close();
			callback.onSuccess(uri);
		} catch (Exception e) { callback.onError(e.getMessage() != null ? e.getMessage() : "Failed to save PDF."); }
	}
}
