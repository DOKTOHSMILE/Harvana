package com.harvana.app;

import android.animation.*;
import android.app.*;
import android.content.*;
import android.content.res.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.media.*;
import android.net.*;
import android.os.*;
import android.text.*;
import android.text.style.*;
import android.util.*;
import android.view.*;
import android.view.View.*;
import android.view.animation.*;
import android.widget.*;
import android.widget.TextView;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.biometric.*;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.budiyev.android.codescanner.*;
import com.budiyev.android.codescanner.CodeScannerView;
import com.bumptech.glide.*;
import com.google.android.material.appbar.AppBarLayout;
import com.google.firebase.FirebaseApp;
import com.google.firebase.storage.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;
import org.json.*;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.budiyev.android.codescanner.CodeScanner;
import com.budiyev.android.codescanner.CodeScannerView;
import com.budiyev.android.codescanner.DecodeCallback;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import com.harvana.app.wallet.HarvanaCardCrypto;


public class ScanQrCodeActivity extends AppCompatActivity {
	
	private Toolbar _toolbar;
	private AppBarLayout _app_bar;
	private CoordinatorLayout _coordinator;
	
	private CodeScannerView scanner_view;
	private android.view.View btn_pick_from_gallery;
	private TextView tv_scan_error;
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.scan_qr_code);
		initialize(_savedInstanceState);
		FirebaseApp.initializeApp(this);
		initializeLogic();
	}
	
	private void initialize(Bundle _savedInstanceState) {
		_app_bar = findViewById(R.id._app_bar);
		_coordinator = findViewById(R.id._coordinator);
		_toolbar = findViewById(R.id._toolbar);
		setSupportActionBar(_toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);
		_toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _v) {
				onBackPressed();
			}
		});
		scanner_view = findViewById(R.id.scanner_view);
		btn_pick_from_gallery = findViewById(R.id.btn_pick_from_gallery);
		tv_scan_error = findViewById(R.id.tv_scan_error);
	}
	
	private void initializeLogic() {
		String title = getIntent() != null ? getIntent().getStringExtra(EXTRA_TITLE) : null;
		setTitle(title != null && !title.trim().isEmpty() ? title : "Scan Code");
		
		mode = getIntent() != null ? getIntent().getStringExtra(EXTRA_MODE) : null;
		if (mode == null) mode = MODE_ANY;
		legacyRequiredPrefix = getIntent() != null ? getIntent().getStringExtra(EXTRA_REQUIRED_PREFIX) : null;
		
		scannerView = findViewById(R.id.scanner_view);
		tvError = findViewById(R.id.tv_scan_error);
		
		android.view.View btnGallery = findViewById(R.id.btn_pick_from_gallery);
		if (btnGallery != null) {
			btnGallery.setOnClickListener(v -> onPickFromGalleryClicked());
		}
		
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
		== PackageManager.PERMISSION_GRANTED) {
			setupScanner();
		} else {
			ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, RC_CAMERA_PERMISSION);
		}
		
	}
	
	
	@Override
	public void onBackPressed() {
		setResult(RESULT_CANCELED);
		finish();
	}
	public void _MORE() {
	}
	private static final int RC_CAMERA_PERMISSION = 5601;
	private static final int RC_PICK_IMAGE = 5602;
	
	public static final String EXTRA_RESULT_TEXT = "scanned_text";
	/** Optional. Shown in the toolbar title. Defaults to "Scan Code". */
	public static final String EXTRA_TITLE = "title";
	
	/** What kind of code this scan should accept. Determines validation —
*  this Activity never routes based on mode, it only accepts/rejects. */
	public static final String EXTRA_MODE = "scan_mode";
	/** Accept anything (no validation) — the default if EXTRA_MODE is omitted. */
	public static final String MODE_ANY = "any";
	/** Accept only a well-formed HV0x wallet address (AddressUtil.isValidAddress). */
	public static final String MODE_WALLET_ADDRESS = "wallet_address";
	/** Accept only an Harvana Access Card QR (HarvanaCardCrypto.FORMAT_PREFIX). */
	public static final String MODE_HARVANA_CARD = "harvana_card";
	
	/** Legacy — still honored if a caller passes it instead of EXTRA_MODE,
*  but EXTRA_MODE is the preferred way to express intent going forward. */
	public static final String EXTRA_REQUIRED_PREFIX = "required_prefix";
	
	private CodeScanner codeScanner;
	private CodeScannerView scannerView;
	private android.widget.TextView tvError;
	private String mode;
	private String legacyRequiredPrefix;
	
	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == RC_CAMERA_PERMISSION) {
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				setupScanner();
			} else {
				SketchwareUtil.showMessage(getApplicationContext(),
				"Camera permission denied. You can still pick an image from your gallery.");
			}
		}
	}
	
	private void setupScanner() {
		codeScanner = new CodeScanner(this, scannerView);
		codeScanner.setDecodeCallback(new DecodeCallback() {
			@Override
			public void onDecoded(@NonNull final Result result) {
				runOnUiThread(() -> handleDecodedText(result.getText()));
			}
		});
	}
	
	// ── Gallery picking ──────────────────────────────────────────────────
	
	private void onPickFromGalleryClicked() {
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("image/*");
		try {
			startActivityForResult(Intent.createChooser(intent, "Select QR code image"), RC_PICK_IMAGE);
		} catch (Exception e) {
			SketchwareUtil.showMessage(getApplicationContext(), "No gallery app found to pick an image.");
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode != RC_PICK_IMAGE || resultCode != RESULT_OK || data == null || data.getData() == null) {
			return;
		}
		Uri imageUri = data.getData();
		new Thread(() -> {
			String decodedText = decodeQrFromImageUri(imageUri);
			runOnUiThread(() -> {
				if (decodedText != null) {
					handleDecodedText(decodedText);
				} else {
					SketchwareUtil.showMessage(getApplicationContext(),
					"Couldn't find a QR code in that image. Try a clearer photo.");
				}
			});
		}).start();
	}
	
	private String decodeQrFromImageUri(Uri imageUri) {
		try (java.io.InputStream inputStream = getContentResolver().openInputStream(imageUri)) {
			if (inputStream == null) return null;
			Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
			if (bitmap == null) return null;
			// Big photos (a printed receipt, an access card on a desk) decode better scaled to a sensible size.
			int max = Math.max(bitmap.getWidth(), bitmap.getHeight());
			if (max > 1600) {
				float f = 1600f / max;
				bitmap = Bitmap.createScaledBitmap(bitmap, Math.round(bitmap.getWidth() * f), Math.round(bitmap.getHeight() * f), true);
			}
			int width = bitmap.getWidth();
			int height = bitmap.getHeight();
			int[] pixels = new int[width * height];
			bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
			java.util.Map<com.google.zxing.DecodeHintType, Object> hints = new java.util.EnumMap<>(com.google.zxing.DecodeHintType.class);
			hints.put(com.google.zxing.DecodeHintType.TRY_HARDER, Boolean.TRUE);
			hints.put(com.google.zxing.DecodeHintType.POSSIBLE_FORMATS, java.util.Collections.singletonList(com.google.zxing.BarcodeFormat.QR_CODE));
			RGBLuminanceSource source = new RGBLuminanceSource(width, height, pixels);
			MultiFormatReader reader = new MultiFormatReader();
			try {
				return reader.decode(new BinaryBitmap(new HybridBinarizer(source)), hints).getText();
			} catch (Exception first) {
				// Light-on-dark codes (the app's dark cards) and uneven light: try the inverted image, then a global threshold.
				try { return reader.decode(new BinaryBitmap(new HybridBinarizer(source.invert())), hints).getText(); }
				catch (Exception second) {
					return reader.decode(new BinaryBitmap(new com.google.zxing.common.GlobalHistogramBinarizer(source)), hints).getText();
				}
			}
		} catch (Exception e) {
			return null;
		}
	}
	
	// ── Shared result handling (camera or gallery) — mode-based validation ──
	
	private void handleDecodedText(String rawText) {
		String text = rawText != null ? rawText.trim() : "";
		
		String rejectionMessage = validateForMode(text);
		if (rejectionMessage != null) {
			if (tvError != null) {
				tvError.setText(rejectionMessage);
				tvError.setVisibility(View.VISIBLE);
			}
			return; // keep scanning / let them pick another image
		}
		
		Intent result = new Intent();
		result.putExtra(EXTRA_RESULT_TEXT, text);
		setResult(RESULT_OK, result);
		finish();
	}
	
	/** Returns null if the text is acceptable for the current mode, or a
*  user-facing rejection message if not. */
	private String validateForMode(String text) {
		if (legacyRequiredPrefix != null && !legacyRequiredPrefix.isEmpty()) {
			return text.startsWith(legacyRequiredPrefix)
			? null : "That's not the right kind of code. Try again.";
		}
		switch (mode) {
			case MODE_WALLET_ADDRESS:
			return com.harvana.app.AddressUtil.isValidAddress(text)
			? null : "That doesn't look like a wallet address. Try again.";
			case MODE_HARVANA_CARD:
			return text.startsWith(HarvanaCardCrypto.FORMAT_PREFIX)
			? null : "That's not an Harvana Access Card QR code.";
			case MODE_ANY:
			default:
			return null;
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		if (codeScanner != null) codeScanner.startPreview();
	}
	
	@Override
	protected void onPause() {
		if (codeScanner != null) codeScanner.releaseResources();
		super.onPause();
	}
	
	{
	}
	
}
