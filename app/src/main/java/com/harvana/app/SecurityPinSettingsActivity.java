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
import android.widget.Button;
import android.widget.LinearLayout;
import com.google.android.material.switchmaterial.SwitchMaterial;
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
import com.bumptech.glide.*;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.card.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.storage.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;
import org.json.*;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;


public class SecurityPinSettingsActivity extends AppCompatActivity {
	
	private Toolbar _toolbar;
	private AppBarLayout _app_bar;
	private CoordinatorLayout _coordinator;
	
	private LinearLayout linear43;
	private TextView textview54;
	private TextView textview55;
	private MaterialCardView cardview35;
	private MaterialCardView cardview37;
	private TextView tv_security_note;
	private LinearLayout linear44;
	private LinearLayout linear45;
	private LinearLayout linear46;
	private MaterialCardView cardview36;
	private TextView textview56;
	private TextView tv_pin_status;
	private TextView btn_pin_action;
	private LinearLayout linear47;
	private LinearLayout linear48;
	private SwitchMaterial switch_biometric;
	private TextView textview57;
	private TextView tv_biometric_status;
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.security_pin_settings);
		initialize(_savedInstanceState);
		FirebaseApp.initializeApp(this);
		initializeLogic();
		com.harvana.app.utils.AppBar.header(this, "Security & PIN", "Protect sending money", R.id.textview54);
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
		linear43 = findViewById(R.id.linear43);
		textview54 = findViewById(R.id.textview54);
		textview55 = findViewById(R.id.textview55);
		cardview35 = findViewById(R.id.cardview35);
		cardview37 = findViewById(R.id.cardview37);
		tv_security_note = findViewById(R.id.tv_security_note);
		linear44 = findViewById(R.id.linear44);
		linear45 = findViewById(R.id.linear45);
		linear46 = findViewById(R.id.linear46);
		cardview36 = findViewById(R.id.cardview36);
		textview56 = findViewById(R.id.textview56);
		tv_pin_status = findViewById(R.id.tv_pin_status);
		btn_pin_action = findViewById(R.id.btn_pin_action);
		linear47 = findViewById(R.id.linear47);
		linear48 = findViewById(R.id.linear48);
		switch_biometric = findViewById(R.id.switch_biometric);
		textview57 = findViewById(R.id.textview57);
		tv_biometric_status = findViewById(R.id.tv_biometric_status);
	}
	
	private void initializeLogic() {
		androidx.appcompat.app.ActionBar ab = getSupportActionBar();
		int backgroundColor = androidx.core.content.ContextCompat.getColor(this, R.color.colorBackground);
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			final Window window = this.getWindow();
			window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
			window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
			com.harvana.app.utils.StatusBarUtil.matchToolbar(this);
		}
		
		_refreshUi();
		// ══ TEMP DEBUG ENTRY — remove before shipping ══
		// Guardian Console has no real entry point by design; this exists
		// purely to reach it without ADB during testing.
		// ══ END TEMP DEBUG ENTRY ══
		
		btn_pin_action.setOnClickListener(v -> {
			if (com.harvana.app.wallet.SecureWalletStorage.hasTransactionPin(this)) {
				_requireOldPinThenChange();
			} else {
				_promptNewPin();
			}
		});
		
		switch_biometric.setOnCheckedChangeListener((buttonView, isChecked) -> {
			if (isChecked) {
				_enableBiometric();
			} else {
				com.harvana.app.wallet.SecureWalletStorage.setBiometricEnabled(this, false);
				_refreshUi();
			}
		});
	}
	
	public void _MORE() {
	}
	private void _refreshUi() {
		boolean hasPin = com.harvana.app.wallet.SecureWalletStorage.hasTransactionPin(this);
		tv_pin_status.setText(hasPin ? "Set" : "Not set");
		btn_pin_action.setText(hasPin ? "Change PIN" : "Set PIN");
		
		boolean biometricOn = com.harvana.app.wallet.SecureWalletStorage.isBiometricEnabled(this);
		// setOnCheckedChangeListener would otherwise re-fire during this
		// programmatic refresh and re-trigger the enable/disable flow.
		switch_biometric.setOnCheckedChangeListener(null);
		switch_biometric.setChecked(biometricOn);
		switch_biometric.setOnCheckedChangeListener((buttonView, isChecked) -> {
			if (isChecked) _enableBiometric();
			else {
				com.harvana.app.wallet.SecureWalletStorage.setBiometricEnabled(this, false);
				_refreshUi();
			}
		});
		
		tv_security_note.setVisibility(hasPin ? View.GONE : View.VISIBLE);
	}
	
	// ── PIN set / change ─────────────────────────────────────────────────
	
	private void _requireOldPinThenChange() {
		_showPinDialog("Enter your current PIN", null, oldPin -> {
			if (com.harvana.app.wallet.SecureWalletStorage.verifyTransactionPin(this, oldPin)) {
				_promptNewPin();
			} else {
				SketchwareUtil.showMessage(getApplicationContext(), "Incorrect PIN.");
			}
		});
	}
	
	private void _promptNewPin() {
		_showPinDialog("Choose a new 4-digit PIN", null, newPin -> {
			String weak = com.harvana.app.wallet.PinPolicy.weakReason(newPin);
			if (weak != null) {
				SketchwareUtil.showMessage(getApplicationContext(), weak);
				return;
			}
			_showPinDialog("Confirm your new PIN", null, confirmPin -> {
				if (!newPin.equals(confirmPin)) {
					SketchwareUtil.showMessage(getApplicationContext(), "PINs didn't match. Please try again.");
					return;
				}
				com.harvana.app.wallet.SecureWalletStorage.setTransactionPin(this, newPin);
				SketchwareUtil.showMessage(getApplicationContext(), "Transaction PIN saved.");
				_refreshUi();
			});
		});
	}
	
	private interface PinCallback {
		void onEntered(String pin);
	}
	
	private void _showPinDialog(String message, String prefill, PinCallback callback) {
		View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_pin_entry, null);
		TextView tvMessage = dialogView.findViewById(R.id.tv_pin_dialog_message);
		EditText etInput = dialogView.findViewById(R.id.et_pin_dialog_input);
		tvMessage.setText(message);
		
		new com.harvana.app.ui.SheetDialog.Builder(this)
		.setView(dialogView)
		.setPositiveButton("Continue", (dialog, which) -> {
			String entered = etInput.getText().toString().trim();
			callback.onEntered(entered);
		})
		.setNegativeButton("Cancel", null)
		.show();
	}
	
	// ── Biometric enrollment ─────────────────────────────────────────────
	// THE FIX: this project has zero existing biometric usage anywhere —
	// built here for real using androidx.biometric, not a fake toggle.
	// Requires the androidx.biometric dependency to actually be added in
	// Sketchware Pro's library manager before this compiles.
	
	private void _enableBiometric() {
		if (!com.harvana.app.wallet.SecureWalletStorage.hasTransactionPin(this)) {
			SketchwareUtil.showMessage(getApplicationContext(),
			"Set a transaction PIN first — biometric is a shortcut for it, not a replacement.");
			switch_biometric.setChecked(false);
			return;
		}
		
		BiometricManager biometricManager = BiometricManager.from(this);
		int canAuthenticate = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG);
		if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
			String reason;
			switch (canAuthenticate) {
				case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
				reason = "This device has no biometric hardware.";
				break;
				case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
				reason = "Biometric hardware is currently unavailable.";
				break;
				case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
				reason = "No fingerprint or face is enrolled on this device yet — add one in your phone's Settings first.";
				break;
				default:
				reason = "Biometric unlock isn't available on this device right now.";
			}
			SketchwareUtil.showMessage(getApplicationContext(), reason);
			switch_biometric.setChecked(false);
			return;
		}
		
		BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
		.setTitle("Enable Biometric Unlock")
		.setSubtitle("Confirm it's really you before turning this on")
		.setNegativeButtonText("Cancel")
		.build();
		
		BiometricPrompt biometricPrompt = new BiometricPrompt(this, ContextCompat.getMainExecutor(this),
		new BiometricPrompt.AuthenticationCallback() {
			@Override
			public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
				com.harvana.app.wallet.SecureWalletStorage.setBiometricEnabled(SecurityPinSettingsActivity.this, true);
				_refreshUi();
			}
			
			@Override
			public void onAuthenticationError(int errorCode, CharSequence errString) {
				switch_biometric.setChecked(false);
			}
			
			@Override
			public void onAuthenticationFailed() {
				// A single failed attempt doesn't cancel the prompt itself —
				// the system dialog stays open for another try. Only revert
				// the switch on an actual error/cancel above.
			}
		});
		
		biometricPrompt.authenticate(promptInfo);
	}
	
	{
	}
	
}
