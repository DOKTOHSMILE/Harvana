package com.harvana.app;

import com.harvana.app.utils.UiColors;
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
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.biometric.*;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.budiyev.android.codescanner.*;
import com.bumptech.glide.*;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.card.*;
import com.google.android.material.textfield.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.storage.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;
import org.json.*;
import android.Manifest;
import android.content.pm.PackageManager;
import com.budiyev.android.codescanner.CodeScanner;
import com.budiyev.android.codescanner.CodeScannerView;
import com.budiyev.android.codescanner.DecodeCallback;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.zxing.Result;
import android.util.Base64;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.harvana.app.wallet.HarvanaCardCrypto;
import com.harvana.app.wallet.SecureWalletStorage;
import com.harvana.app.wallet.WalletGenerator;
import com.harvana.app.wallet.WalletManager;


public class RestoreWalletActivity extends AppCompatActivity {
	
	private AppBarLayout app_bar_layout6;
	private NestedScrollView vscroll2;
	private Toolbar toolbar;
	private LinearLayout linear7;
	private MaterialCardView cardview3;
	private TextView textview5;
	private MaterialCardView cardview4;
	private LinearLayout linear8;
	private TextView textview3;
	private TextView textview4;
	private Button btn_scan_card;
	private Button btn_enter_code_manually;
	private LinearLayout linear9;
	private TextView textview6;
	private TextInputLayout textinputlayout3;
	private Button btn_restore_from_phrase;
	private TextInputEditText et_phrase;
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		getWindow().setFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE, android.view.WindowManager.LayoutParams.FLAG_SECURE); // no screenshots or recents thumbnails of secrets
		setContentView(R.layout.restore_wallet);
		initialize(_savedInstanceState);
		String scannedCard = getIntent().getStringExtra(EXTRA_SCANNED_CARD);
		if (scannedCard != null && scannedCard.startsWith(HarvanaCardCrypto.FORMAT_PREFIX) && _savedInstanceState == null) {
			pendingScannedPayload = scannedCard;
			getWindow().getDecorView().post(() -> showPasswordBottomSheet(false));
		}
		FirebaseApp.initializeApp(this);
		initializeLogic();
		com.harvana.app.utils.AppBar.header(this, "Restore wallet", "From your Harvana Access Card", R.id.textview3);
	}
	
	private void initialize(Bundle _savedInstanceState) {
		app_bar_layout6 = findViewById(R.id.app_bar_layout6);
		vscroll2 = findViewById(R.id.vscroll2);
		toolbar = findViewById(R.id.toolbar);
		linear7 = findViewById(R.id.linear7);
		cardview3 = findViewById(R.id.cardview3);
		textview5 = findViewById(R.id.textview5);
		cardview4 = findViewById(R.id.cardview4);
		linear8 = findViewById(R.id.linear8);
		textview3 = findViewById(R.id.textview3);
		textview4 = findViewById(R.id.textview4);
		btn_scan_card = findViewById(R.id.btn_scan_card);
		btn_enter_code_manually = findViewById(R.id.btn_enter_code_manually);
		linear9 = findViewById(R.id.linear9);
		textview6 = findViewById(R.id.textview6);
		textinputlayout3 = findViewById(R.id.textinputlayout3);
		btn_restore_from_phrase = findViewById(R.id.btn_restore_from_phrase);
		et_phrase = findViewById(R.id.et_phrase);
	}
	
	private void initializeLogic() {
		embedded = getIntent() != null && getIntent().getBooleanExtra(EXTRA_EMBEDDED, false);
		
		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		toolbar.setNavigationOnClickListener(v -> onBackPressed());
		
		etPhrase = findViewById(R.id.et_phrase);
		btnRestoreFromPhrase = findViewById(R.id.btn_restore_from_phrase);
		btnScanCard = findViewById(R.id.btn_scan_card);
		btnEnterCodeManually = findViewById(R.id.btn_enter_code_manually);
		
		btnRestoreFromPhrase.setOnClickListener(v -> onRestoreFromPhraseClicked());
		btnScanCard.setOnClickListener(v -> onScanCardClicked());
		if (btnEnterCodeManually != null) {
			btnEnterCodeManually.setOnClickListener(v -> showManualCodeBottomSheet());
		}
		
	}
	
	public void _MORE() {
	}
	private static final int RC_CAMERA = 4102;
	private static final int RC_MANDATORY_CARD = 8802;
	
	public static final String EXTRA_EMBEDDED = "embedded";
	public static final String EXTRA_RESULT_ADDRESS = "restored_address";
	
	private EditText etPhrase;
	private Button btnRestoreFromPhrase;
	private Button btnScanCard;
	private Button btnEnterCodeManually;
	
	private CodeScanner codeScanner;
	private BottomSheetDialog scannerDialog;
	private String pendingScannedPayload;

	/** A wallet access card scanned anywhere in the app (QrRouter): open straight at the card password. */
	public static final String EXTRA_SCANNED_CARD = "scanned_card";
	private boolean embedded;
	
	private String pendingMnemonic;
	private String pendingAddress;
	private String pendingSeedB64;
	private String pendingPubKeyB64;
	
	private void onRestoreFromPhraseClicked() {
		String phrase = etPhrase.getText().toString().trim().toLowerCase();
		if (!WalletGenerator.isValidMnemonic(this, phrase)) {
			SketchwareUtil.showMessage(getApplicationContext(),
			"That doesn't look like a valid recovery phrase. Check the spelling of each word.");
			return;
		}
		prepareThenGateOnCard(phrase);
	}
	
	private void prepareThenGateOnCard(String mnemonic) {
		try {
			String address = WalletGenerator.deriveAddressFromMnemonic(mnemonic);
			if (address == null) {
				SketchwareUtil.showMessage(getApplicationContext(), "Couldn't derive a wallet address from this phrase.");
				return;
			}
			byte[] seed = Base64.decode(WalletGenerator.derivePrivateKey(mnemonic), Base64.NO_WRAP);
			byte[] pubKey = com.harvana.app.crypto.Ed25519.publicKeyFromSeed(seed);
			
			pendingMnemonic = mnemonic;
			pendingAddress = address;
			pendingSeedB64 = WalletGenerator.derivePrivateKey(mnemonic);
			pendingPubKeyB64 = Base64.encodeToString(pubKey, Base64.NO_WRAP);
			
			Intent cardIntent = new Intent(this, CreateHarvanaCardActivity.class);
			com.harvana.app.wallet.PhraseHandoff.hold(pendingMnemonic, pendingAddress);
			cardIntent.putExtra(CreateHarvanaCardActivity.EXTRA_MANDATORY, true);
			cardIntent.putExtra(CreateHarvanaCardActivity.EXTRA_WALLET_ADDRESS, pendingAddress);
			cardIntent.putExtra(CreateHarvanaCardActivity.EXTRA_LABEL,
			"this restored wallet — you only have the phrase backup so far");
			startActivityForResult(cardIntent, RC_MANDATORY_CARD);
		} catch (Exception e) {
			SketchwareUtil.showMessage(getApplicationContext(), "Something went wrong preparing this wallet. Please try again.");
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == RC_SCAN_QR && resultCode == Activity.RESULT_OK && data != null) {
			pendingScannedPayload = data.getStringExtra(com.harvana.app.ScanQrCodeActivity.EXTRA_RESULT_TEXT);
			showPasswordBottomSheet(false);
			return;
		}
		if (requestCode == RC_MANDATORY_CARD && resultCode == Activity.RESULT_OK) {
			persistAndFinish(pendingMnemonic, pendingAddress, pendingSeedB64, pendingPubKeyB64);
		}
	}
	
	// ── Path B: restore from Harvana Access Card QR — no gate ──────────────
	
	private static final int RC_SCAN_QR = 4103;
	
	private void onScanCardClicked() {
		Intent scanIntent = new Intent(this, com.harvana.app.ScanQrCodeActivity.class);
		scanIntent.putExtra(com.harvana.app.ScanQrCodeActivity.EXTRA_TITLE, "Scan Harvana Card");
		scanIntent.putExtra(com.harvana.app.ScanQrCodeActivity.EXTRA_REQUIRED_PREFIX, HarvanaCardCrypto.FORMAT_PREFIX);
		startActivityForResult(scanIntent, RC_SCAN_QR);
	}
	
	private void showScannerBottomSheet() {
		scannerDialog = new com.harvana.app.ui.AppSheet(this);
		View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_scanner, null);
		scannerDialog.setContentView(sheetView);
		
		CodeScannerView scannerComponent = sheetView.findViewById(R.id.scanner_view);
		if (scannerComponent != null) {
			FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
			FrameLayout.LayoutParams.MATCH_PARENT, 
			FrameLayout.LayoutParams.MATCH_PARENT
			);
			scannerComponent.setLayoutParams(lp);
			
			codeScanner = new CodeScanner(this, scannerComponent);
			codeScanner.setDecodeCallback(new DecodeCallback() {
				@Override
				public void onDecoded(@NonNull final Result result) {
					runOnUiThread(() -> {
						String text = result.getText() != null ? result.getText().trim() : "";
						if (scannerDialog != null && scannerDialog.isShowing()) scannerDialog.dismiss();
						if (text.startsWith(HarvanaCardCrypto.FORMAT_PREFIX)) {
							pendingScannedPayload = text;
							showPasswordBottomSheet(false);
						} else {
							SketchwareUtil.showMessage(getApplicationContext(), "That QR code isn't an Harvana Access Card.");
						}
					});
				}
			});
		}
		
		scannerDialog.setOnShowListener(dialog -> {
			BottomSheetDialog d = (BottomSheetDialog) dialog;
			FrameLayout bottomSheetInternal = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
			if (bottomSheetInternal != null) {
				bottomSheetInternal.setBackgroundColor(Color.TRANSPARENT);
				float radius = 24f * getResources().getDisplayMetrics().density;
				GradientDrawable background = new GradientDrawable();
				background.setShape(GradientDrawable.RECTANGLE);
				background.setColor(UiColors.surface());
				background.setCornerRadii(new float[]{radius, radius, radius, radius, 0f, 0f, 0f, 0f});
				sheetView.setBackground(background);
				
				BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheetInternal);
				behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
				behavior.setSkipCollapsed(true);
				behavior.setFitToContents(false);
				behavior.setDraggable(true);
				
				ViewGroup.LayoutParams lp = bottomSheetInternal.getLayoutParams();
				lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
				bottomSheetInternal.setLayoutParams(lp);
			}
			if (codeScanner != null) codeScanner.startPreview();
		});
		
		scannerDialog.setOnDismissListener(dialog -> { if (codeScanner != null) codeScanner.releaseResources(); });
		scannerDialog.show();
	}
	
	// ── Path C: restore from typed backup digit code — no gate ──────────
	
	private String pendingManualDigitCode;
	
	private void showManualCodeBottomSheet() {
		BottomSheetDialog dialog = new com.harvana.app.ui.AppSheet(this);
		View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_manual_code, null);
		dialog.setContentView(sheetView);
		
		// Ensure bottom sheet resizes above keyboard
		if (dialog.getWindow() != null) {
			dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
		}
		
		EditText etCode = sheetView.findViewById(R.id.et_manual_code);
		TextView tvError = sheetView.findViewById(R.id.tv_manual_code_error);
		Button btnContinue = sheetView.findViewById(R.id.btn_continue_manual_code);
		
		btnContinue.setOnClickListener(v -> {
			String raw = etCode.getText().toString().replaceAll("[^0-9]", "");
			if (raw.length() != HarvanaCardCrypto.DIGIT_CODE_LENGTH_24 && raw.length() != HarvanaCardCrypto.DIGIT_CODE_LENGTH) {
				tvError.setText("That's " + raw.length() + " digits. A card code has " + HarvanaCardCrypto.DIGIT_CODE_LENGTH_24
				+ " digits (" + HarvanaCardCrypto.DIGIT_CODE_LENGTH + " on an older card). Check for a missing or extra digit.");
				tvError.setVisibility(View.VISIBLE);
				return;
			}
			pendingManualDigitCode = raw;
			dialog.dismiss();
			showPasswordBottomSheet(true);
		});
		
		dialog.setOnShowListener(d -> {
			BottomSheetDialog bsd = (BottomSheetDialog) d;
			FrameLayout inner = bsd.findViewById(com.google.android.material.R.id.design_bottom_sheet);
			if (inner != null) {
				BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(inner);
				behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
				behavior.setSkipCollapsed(true);
			}
		});
		
		dialog.show();
	}
	
	// ── Shared: password entry after either QR scan or manual code entry ──
	
	private void showPasswordBottomSheet(boolean fromManualCode) {
		BottomSheetDialog dialog = new com.harvana.app.ui.AppSheet(this);
		View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_card_password, null);
		dialog.setContentView(sheetView);
		
		// Ensure bottom sheet resizes above keyboard
		if (dialog.getWindow() != null) {
			dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
		}
		
		EditText etPassword = sheetView.findViewById(R.id.et_card_sentence);
		TextView tvError = sheetView.findViewById(R.id.tv_card_sentence_error);
		Button btnUnlock = sheetView.findViewById(R.id.btn_unlock_card);
		
		btnUnlock.setOnClickListener(v -> {
			String password = etPassword.getText().toString();
			btnUnlock.setEnabled(false);
			btnUnlock.setText("Decrypting…");
			
			new Thread(() -> {
				HarvanaCardCrypto.DecryptResult result = fromManualCode
				? HarvanaCardCrypto.decryptDigitCode(this, pendingManualDigitCode, password)
				: HarvanaCardCrypto.decryptCardPayload(this, pendingScannedPayload, password);
				
				runOnUiThread(() -> {
					if (result.success) {
						dialog.dismiss();
						if (WalletGenerator.isValidMnemonic(this, result.mnemonic)) {
							restoreFromCardDirectly(result.mnemonic);
						} else {
							SketchwareUtil.showMessage(getApplicationContext(),
							"Decrypted, but the recovered phrase looks invalid. This card may be corrupted.");
						}
					} else {
						btnUnlock.setEnabled(true);
						btnUnlock.setText("Unlock");
						tvError.setText(result.errorMessage);
						tvError.setVisibility(View.VISIBLE);
					}
				});
			}).start();
		});
		
		dialog.setOnShowListener(d -> {
			BottomSheetDialog bsd = (BottomSheetDialog) d;
			FrameLayout inner = bsd.findViewById(com.google.android.material.R.id.design_bottom_sheet);
			if (inner != null) {
				BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(inner);
				behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
				behavior.setSkipCollapsed(true);
			}
		});
		
		dialog.show();
	}
	
	private void restoreFromCardDirectly(String mnemonic) {
		try {
			String address = WalletGenerator.deriveAddressFromMnemonic(mnemonic);
			if (address == null) {
				SketchwareUtil.showMessage(getApplicationContext(), "Couldn't derive a wallet address from this card.");
				return;
			}
			byte[] seed = Base64.decode(WalletGenerator.derivePrivateKey(mnemonic), Base64.NO_WRAP);
			byte[] pubKey = com.harvana.app.crypto.Ed25519.publicKeyFromSeed(seed);
			String pubKeyB64 = Base64.encodeToString(pubKey, Base64.NO_WRAP);
			String seedB64 = WalletGenerator.derivePrivateKey(mnemonic);
			
			persistAndFinish(mnemonic, address, seedB64, pubKeyB64);
		} catch (Exception e) {
			SketchwareUtil.showMessage(getApplicationContext(), "Something went wrong restoring this wallet. Please try again.");
		}
	}
	
	// ── Shared persistence ────────────────────────────────────────────────
	
	private void persistAndFinish(String mnemonic, String address, String seedB64, String pubKeyB64) {
		// The Pi wallet that belongs to the same phrase (a Pi phrase of 24 words gives the very wallet Pi's app shows).
		com.harvana.app.wallet.PiWalletStore.rememberFromPhrase(getApplicationContext(), address, mnemonic);
		try {
			com.harvana.app.wallet.WalletListManager.addWalletToList(this, address, null);
			SecureWalletStorage.saveMnemonic(this, address, mnemonic);
			WalletManager.saveKeysLocally(this, address, seedB64, pubKeyB64);
			
			getSharedPreferences("harv_wallet_prefs", MODE_PRIVATE)
			.edit()
			.putString("active_wallet_address", address)
			.putString("active_address", address)
			.putString("wallet_address", address)
			.putBoolean("is_wallet_created", true)
			.putBoolean("wallet_initialized", true)
			.apply();
			
			SketchwareUtil.showMessage(getApplicationContext(), "Wallet restored.");
			
			if (embedded) {
				Intent result = new Intent();
				result.putExtra(EXTRA_RESULT_ADDRESS, address);
				setResult(RESULT_OK, result);
				finish();
			} else {
				Intent intent = new Intent(this, MainActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
				startActivity(intent);
				finish();
			}
		} catch (Exception e) {
			SketchwareUtil.showMessage(getApplicationContext(), "Something went wrong saving this wallet. Please try again.");
		}
	}
	
	{
	}
	
}
