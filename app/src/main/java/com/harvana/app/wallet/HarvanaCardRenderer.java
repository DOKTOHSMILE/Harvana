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
	
	public static Bitmap renderFront(Context context, CardTemplate template, String walletAddress, String cardOwnerName, String cardDescription) {
		return renderFront(context, template, walletAddress, cardOwnerName, cardDescription, null);
	}

	/** {@code harvName}: the wallet's primary Harvana name ("doktohsmile.harv"), shown as a fact when it has one. */
	public static Bitmap renderFront(Context context, CardTemplate template, String walletAddress, String cardOwnerName, String cardDescription, String harvName) {
		Bitmap bitmap = Bitmap.createBitmap(CARD_WIDTH_PX, CARD_HEIGHT_PX, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		clipToRoundedRect(canvas);
		
		// Updated to pass Context so it can load the thematic background images
		drawBackground(context, canvas, template);
		
		float margin = 56f;
		
		// Title block (owner, 20 Oct 2026): a bigger logo, the full card name in bold, the protection under it.
		Paint subtitle1 = new Paint(Paint.ANTI_ALIAS_FLAG);
		subtitle1.setColor(template.textPrimary);
		subtitle1.setTextSize(25f);
		subtitle1.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD));
		Paint subtitle2 = new Paint(Paint.ANTI_ALIAS_FLAG);
		subtitle2.setColor(template.textSecondary);
		subtitle2.setTextSize(15f);
		float logoSize = 84f, logoTop = 34f;
		drawAppIcon(context, canvas, margin, logoTop, logoSize, logoSize);
		float textX = margin + logoSize + 18f;
		canvas.drawText("Harvana Protocol Wallet Access Card", textX, logoTop + 40f, subtitle1);
		canvas.drawText("Encrypted wallet backup  ·  AES-256-GCM", textX, logoTop + 66f, subtitle2);

		// Top Right Card Tier
		Paint badgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		badgePaint.setColor(template.accent);
		badgePaint.setTextSize(20f);
		badgePaint.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD));
		badgePaint.setTextAlign(Paint.Align.RIGHT);
		canvas.drawText(template.displayName.toUpperCase(), CARD_WIDTH_PX - margin, logoTop + 40f, badgePaint);

		Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		labelPaint.setColor(template.textSecondary);
		labelPaint.setTextSize(13f);
		labelPaint.setLetterSpacing(0.25f);
		labelPaint.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD));

		// Tag: the card's name ("My main wallet"), between the title and the address, in bold. It also names the
		// wallet when the card is restored (the QR carries it).
		String finalDescription = cardDescription != null ? cardDescription.trim() : "";
		if (!finalDescription.isEmpty()) {
			finalDescription = finalDescription.toUpperCase(java.util.Locale.US);
			if (finalDescription.length() > 28) finalDescription = finalDescription.substring(0, 27) + "…";
			canvas.drawText("TAG", margin, 166f, labelPaint);
			Paint descPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
			descPaint.setColor(template.accent);
			descPaint.setTextSize(30f);
			descPaint.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD));
			canvas.drawText(finalDescription, margin, 202f, descPaint);
		}

		// Wallet address, three rows of five-character groups
		canvas.drawText("WALLET ADDRESS", margin, 246f, labelPaint);
		Paint addrPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		addrPaint.setColor(template.textPrimary);
		addrPaint.setTextSize(34f);
		addrPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
		addrPaint.setLetterSpacing(0.04f);
		String cleanAddress = walletAddress != null ? walletAddress.replaceAll("(?i)^HV0X", "").toUpperCase() : "";
		if (cleanAddress.length() == 40) {
			canvas.drawText(String.format("HV0x    %s   %s", cleanAddress.substring(0, 5), cleanAddress.substring(5, 10)), margin, 292f, addrPaint);
			canvas.drawText(String.format("%s   %s   %s", cleanAddress.substring(10, 15), cleanAddress.substring(15, 20), cleanAddress.substring(20, 25)), margin, 340f, addrPaint);
			canvas.drawText(String.format("%s   %s   %s", cleanAddress.substring(25, 30), cleanAddress.substring(30, 35), cleanAddress.substring(35, 40)), margin, 388f, addrPaint);
		} else {
			canvas.drawText("HV0x " + cleanAddress, margin, 292f, addrPaint);
		}

		// Facts: two rows of label over value
		Paint factLabel = new Paint(Paint.ANTI_ALIAS_FLAG);
		factLabel.setColor(template.textSecondary);
		factLabel.setTextSize(12f);
		factLabel.setLetterSpacing(0.12f);
		factLabel.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD));
		Paint factValue = new Paint(Paint.ANTI_ALIAS_FLAG);
		factValue.setColor(template.textPrimary);
		factValue.setTextSize(20f);
		factValue.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
		Paint factSmall = new Paint(factValue);
		factSmall.setTextSize(16f);

		String finalName = (cardOwnerName != null && !cardOwnerName.trim().isEmpty()) ? cardOwnerName.toUpperCase() : "HARVANA MEMBER";
		if (finalName.length() > 24) finalName = finalName.substring(0, 22) + "...";
		float col2 = margin + 420f, col3 = margin + 590f;
		canvas.drawText("CARD OWNER", margin, 440f, factLabel);
		canvas.drawText(finalName, margin, 468f, factValue);
		canvas.drawText("ISSUED", col2, 440f, factLabel);
		canvas.drawText(new java.text.SimpleDateFormat("MM/yy", java.util.Locale.US).format(new java.util.Date()), col2, 468f, factValue);
		canvas.drawText("CARD NO.", col3, 440f, factLabel);
		canvas.drawText(cardNumber(walletAddress), col3, 468f, factValue);

		canvas.drawText("NETWORK", margin, 506f, factLabel);
		canvas.drawText("HARVANA PROTOCOL", margin, 530f, factSmall);
		if (harvName != null && !harvName.trim().isEmpty()) {
			String n = harvName.trim().toLowerCase(java.util.Locale.US);
			if (n.length() > 20) n = n.substring(0, 19) + "…";
			canvas.drawText("NAME", col2, 506f, factLabel);
			canvas.drawText(n, col2, 530f, factSmall);
		} else {
			canvas.drawText("KEY", col2, 506f, factLabel);
			canvas.drawText("ED25519", col2, 530f, factSmall);
		}
		canvas.drawText("PROTECTION", col3, 506f, factLabel);
		canvas.drawText("PBKDF2 + ARGON2ID", col3, 530f, factSmall);

		// Footer
		Paint footerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		footerPaint.setColor(template.textSecondary);
		footerPaint.setTextSize(15f);
		footerPaint.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD));
		canvas.drawText("HARVANA NETWORK  ·  harvannaprotocol.com", margin, CARD_HEIGHT_PX - 36f, footerPaint);

		// Draw the custom signature seal at the bottom right
		drawSignatureStamp(canvas, CARD_WIDTH_PX - margin - 100f, CARD_HEIGHT_PX - margin - 100f);
		
		if (template.metallic) {
			drawMetallicSheen(canvas);
		}
		
		return bitmap;
	}
	
	/** A card number for reading out over the phone: 12 digits from the address ("4821 7730 1192"). */
	static String cardNumber(String address) {
		try {
			byte[] h = java.security.MessageDigest.getInstance("SHA-256").digest(String.valueOf(address).toLowerCase(java.util.Locale.US).getBytes(java.nio.charset.StandardCharsets.UTF_8));
			String d = new java.math.BigInteger(1, h).toString().substring(0, 12);
			return d.substring(0, 4) + " " + d.substring(4, 8) + " " + d.substring(8, 12);
		} catch (Exception e) {
			return "—";
		}
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
		canvas.drawRect(0, 40, CARD_WIDTH_PX, 116, stripePaint);
		
		float margin = 56f;
		int qrSize = 300;
		float pad = 12f;
		float qrLeft = CARD_WIDTH_PX - qrSize - margin - 4f;
		float qrTop = 150f;
		
		// ── What this card is, in plain words ──
		Paint warnLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		warnLabelPaint.setColor(template.accent);
		warnLabelPaint.setTextSize(20f);
		warnLabelPaint.setLetterSpacing(0.08f);
		warnLabelPaint.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD));
		canvas.drawText("SOCIAL BACKUP", margin, 166f, warnLabelPaint);
		
		Paint warnBodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		warnBodyPaint.setColor(template.textSecondary);
		warnBodyPaint.setTextSize(15f);
		canvas.drawText("Encrypted. Safe to give to someone you trust.", margin, 194f, warnBodyPaint);
		canvas.drawText("Nobody can open it without your card password.", margin, 216f, warnBodyPaint);
		canvas.drawText("Keep the password apart from this card.", margin, 238f, warnBodyPaint);
		
		// ── Backup code, for when the QR can't be scanned (106 digits on a 12-word card, 145 on a 24-word card) ──
		Paint digitLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		digitLabelPaint.setColor(template.textSecondary);
		digitLabelPaint.setTextSize(13f);
		digitLabelPaint.setLetterSpacing(0.06f);
		digitLabelPaint.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD));
		canvas.drawText("BACKUP CODE  ·  TYPE IT IF THE QR WON'T SCAN", margin, 278f, digitLabelPaint);
		
		if (digitCode != null && (digitCode.length() == HarvanaCardCrypto.DIGIT_CODE_LENGTH
				|| digitCode.length() == HarvanaCardCrypto.DIGIT_CODE_LENGTH_24)) {
			Paint digitPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
			digitPaint.setColor(template.textPrimary);
			digitPaint.setTextSize(18f);
			digitPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
			
			int groupCount = (digitCode.length() + 4) / 5;
			float colWidth = 106f;
			float rowHeight = 30f;
			float startY = 310f;
			for (int i = 0; i < groupCount; i++) {
				int start = i * 5;
				String group = digitCode.substring(start, Math.min(start + 5, digitCode.length()));
				float x = margin + (i % 5) * colWidth;
				float y = startY + (i / 5) * rowHeight;
				canvas.drawText(group, x, y, digitPaint);
			}
		}
		
		// ── QR code, framed, with a caption under it ──
		Bitmap qr = HARVQRCodeGenerator.generateClassicQRCode(qrPayload, qrSize);
		if (qr != null) {
			Paint framePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
			framePaint.setColor(template.accent);
			RectF frameRect = new RectF(qrLeft - pad - 4f, qrTop - pad - 4f,
			qrLeft + qrSize + pad + 4f, qrTop + qrSize + pad + 4f);
			canvas.drawRoundRect(frameRect, 16f, 16f, framePaint);
			
			Paint qrBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
			qrBgPaint.setColor(Color.WHITE);
			RectF qrBgRect = new RectF(qrLeft - pad, qrTop - pad, qrLeft + qrSize + pad, qrTop + qrSize + pad);
			canvas.drawRoundRect(qrBgRect, 12f, 12f, qrBgPaint);
			
			canvas.drawBitmap(qr, qrLeft, qrTop, null);
			
			Paint capPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
			capPaint.setColor(template.textSecondary);
			capPaint.setTextSize(14f);
			capPaint.setTextAlign(Paint.Align.CENTER);
			canvas.drawText("Scan in Harvana to restore", qrLeft + qrSize / 2f, qrTop + qrSize + pad + 32f, capPaint);
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
		icon.setAlpha(255);
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
		} catch (Exception e) { callback.onError(com.harvana.app.utils.ErrorText.of(e)); }
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
		} catch (Exception e) { callback.onError(com.harvana.app.utils.ErrorText.of(e)); }
	}
}
