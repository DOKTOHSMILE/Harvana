package com.harvanna.app;

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
import android.widget.CheckBox;
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
import com.google.android.material.button.*;
import com.google.android.material.card.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.storage.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;
import org.json.*;

public class CreateWalletActivity extends AppCompatActivity {
	
	private AppBarLayout app_bar_layout4;
	private NestedScrollView vscroll2;
	private Toolbar toolbar;
	private LinearLayout linear5;
	private TextView tv_title;
	private TextView textview4;
	private MaterialCardView cardview1;
	private MaterialCardView cardview2;
	private MaterialButton btn_finish_setup;
	private LinearLayout linear6;
	private LinearLayout linear7;
	private LinearLayout linear10;
	private LinearLayout linear13;
	private LinearLayout linear16;
	private LinearLayout linear19;
	private LinearLayout linear22;
	private LinearLayout linear8;
	private LinearLayout linear9;
	private TextView tv_num_1;
	private TextView tv_word_1;
	private TextView tv_num_2;
	private TextView tv_word_2;
	private LinearLayout linear11;
	private LinearLayout linear12;
	private TextView tv_num_3;
	private TextView tv_word_3;
	private TextView tv_num_4;
	private TextView tv_word_4;
	private LinearLayout linear14;
	private LinearLayout linear15;
	private TextView tv_num_5;
	private TextView tv_word_5;
	private TextView tv_num_6;
	private TextView tv_word_6;
	private LinearLayout linear17;
	private LinearLayout linear18;
	private TextView tv_num_7;
	private TextView tv_word_7;
	private TextView tv_num_8;
	private TextView tv_word_8;
	private LinearLayout linear20;
	private LinearLayout linear21;
	private TextView tv_num_9;
	private TextView tv_word_9;
	private TextView tv_num_10;
	private TextView tv_word_10;
	private LinearLayout linear23;
	private LinearLayout linear24;
	private TextView tv_num_11;
	private TextView tv_word_11;
	private TextView tv_num_12;
	private TextView tv_word_12;
	private LinearLayout linear25;
	private CheckBox cb_agree;
	private TextView textview5;
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.create_wallet);
		initialize(_savedInstanceState);
		FirebaseApp.initializeApp(this);
		initializeLogic();
		com.harvanna.app.utils.AppBar.header(this, "Create wallet", "Your new seed phrase", R.id.tv_title);
	}
	
	private void initialize(Bundle _savedInstanceState) {
		app_bar_layout4 = findViewById(R.id.app_bar_layout4);
		vscroll2 = findViewById(R.id.vscroll2);
		toolbar = findViewById(R.id.toolbar);
		linear5 = findViewById(R.id.linear5);
		tv_title = findViewById(R.id.tv_title);
		textview4 = findViewById(R.id.textview4);
		cardview1 = findViewById(R.id.cardview1);
		cardview2 = findViewById(R.id.cardview2);
		btn_finish_setup = findViewById(R.id.btn_finish_setup);
		linear6 = findViewById(R.id.linear6);
		linear7 = findViewById(R.id.linear7);
		linear10 = findViewById(R.id.linear10);
		linear13 = findViewById(R.id.linear13);
		linear16 = findViewById(R.id.linear16);
		linear19 = findViewById(R.id.linear19);
		linear22 = findViewById(R.id.linear22);
		linear8 = findViewById(R.id.linear8);
		linear9 = findViewById(R.id.linear9);
		tv_num_1 = findViewById(R.id.tv_num_1);
		tv_word_1 = findViewById(R.id.tv_word_1);
		tv_num_2 = findViewById(R.id.tv_num_2);
		tv_word_2 = findViewById(R.id.tv_word_2);
		linear11 = findViewById(R.id.linear11);
		linear12 = findViewById(R.id.linear12);
		tv_num_3 = findViewById(R.id.tv_num_3);
		tv_word_3 = findViewById(R.id.tv_word_3);
		tv_num_4 = findViewById(R.id.tv_num_4);
		tv_word_4 = findViewById(R.id.tv_word_4);
		linear14 = findViewById(R.id.linear14);
		linear15 = findViewById(R.id.linear15);
		tv_num_5 = findViewById(R.id.tv_num_5);
		tv_word_5 = findViewById(R.id.tv_word_5);
		tv_num_6 = findViewById(R.id.tv_num_6);
		tv_word_6 = findViewById(R.id.tv_word_6);
		linear17 = findViewById(R.id.linear17);
		linear18 = findViewById(R.id.linear18);
		tv_num_7 = findViewById(R.id.tv_num_7);
		tv_word_7 = findViewById(R.id.tv_word_7);
		tv_num_8 = findViewById(R.id.tv_num_8);
		tv_word_8 = findViewById(R.id.tv_word_8);
		linear20 = findViewById(R.id.linear20);
		linear21 = findViewById(R.id.linear21);
		tv_num_9 = findViewById(R.id.tv_num_9);
		tv_word_9 = findViewById(R.id.tv_word_9);
		tv_num_10 = findViewById(R.id.tv_num_10);
		tv_word_10 = findViewById(R.id.tv_word_10);
		linear23 = findViewById(R.id.linear23);
		linear24 = findViewById(R.id.linear24);
		tv_num_11 = findViewById(R.id.tv_num_11);
		tv_word_11 = findViewById(R.id.tv_word_11);
		tv_num_12 = findViewById(R.id.tv_num_12);
		tv_word_12 = findViewById(R.id.tv_word_12);
		linear25 = findViewById(R.id.linear25);
		cb_agree = findViewById(R.id.cb_agree);
		textview5 = findViewById(R.id.textview5);
	}
	
	private void initializeLogic() {
		TextView tvTitle = findViewById(R.id.tv_title);
		if (tvTitle != null) tvTitle.setText("Your New Wallet Seed Phrase");
		
		final int[] tvNumIds = {
			R.id.tv_num_1, R.id.tv_num_2, R.id.tv_num_3, R.id.tv_num_4,
			R.id.tv_num_5, R.id.tv_num_6, R.id.tv_num_7, R.id.tv_num_8,
			R.id.tv_num_9, R.id.tv_num_10, R.id.tv_num_11, R.id.tv_num_12
		};
		final int[] tvWordIds = {
			R.id.tv_word_1, R.id.tv_word_2, R.id.tv_word_3, R.id.tv_word_4,
			R.id.tv_word_5, R.id.tv_word_6, R.id.tv_word_7, R.id.tv_word_8,
			R.id.tv_word_9, R.id.tv_word_10, R.id.tv_word_11, R.id.tv_word_12
		};
		for (int i = 0; i < 12; i++) {
			TextView tvNum = findViewById(tvNumIds[i]);
			TextView tvWord = findViewById(tvWordIds[i]);
			if (tvNum != null) tvNum.setText((i + 1) + ".");
			if (tvWord != null) tvWord.setText("…");
		}
		
		final CheckBox cbAgree = findViewById(R.id.cb_agree);
		btnFinish = findViewById(R.id.btn_finish_setup);
		if (btnFinish == null) {
			SketchwareUtil.showMessage(getApplicationContext(), "Setup button not found.");
			finish();
			return;
		}
		btnFinish.setEnabled(false);
		btnFinish.setText("Continue to Backup Card");
		
		final Context context = this;
		new Thread(() -> {
			try {
				final String mnemonic = com.harvanna.app.wallet.WalletGenerator.generateMnemonic(context);
				if (mnemonic == null || mnemonic.trim().isEmpty()) {
					runOnUiThread(() -> {
						SketchwareUtil.showMessage(context, "Failed to generate recovery phrase.");
						finish();
					});
					return;
				}
				
				final String walletAddress = com.harvanna.app.wallet.WalletGenerator.deriveAddressFromMnemonic(mnemonic);
				if (walletAddress == null || !walletAddress.startsWith("HV0x") || walletAddress.length() != 44) {
					runOnUiThread(() -> {
						SketchwareUtil.showMessage(context, "Failed to derive valid HV0x address.");
						finish();
					});
					return;
				}
				
				final String[] phraseWords = mnemonic.trim().split("\\s+");
				if (phraseWords.length != 12) {
					runOnUiThread(() -> {
						SketchwareUtil.showMessage(context, "Invalid phrase length.");
						finish();
					});
					return;
				}
				
				final String seedB64;
				final String pubKeyB64;
				try {
					javax.crypto.spec.PBEKeySpec spec = new javax.crypto.spec.PBEKeySpec(
					mnemonic.toCharArray(),
					"mnemonic".getBytes(java.nio.charset.StandardCharsets.UTF_8),
					2048, 512);
					javax.crypto.SecretKeyFactory skf = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
					byte[] fullSeed = skf.generateSecret(spec).getEncoded();
					byte[] ed25519Seed = java.util.Arrays.copyOf(fullSeed, 32);
					byte[] publicKey = com.harvanna.app.crypto.Ed25519.publicKeyFromSeed(ed25519Seed);
					seedB64 = android.util.Base64.encodeToString(ed25519Seed, android.util.Base64.NO_WRAP);
					pubKeyB64 = android.util.Base64.encodeToString(publicKey, android.util.Base64.NO_WRAP);
					spec.clearPassword();
				} catch (Exception e) {
					runOnUiThread(() -> {
						SketchwareUtil.showMessage(context, "Key derivation failed: " + e.getMessage());
						finish();
					});
					return;
				}
				
				runOnUiThread(() -> {
					pendingMnemonic = mnemonic;
					pendingWalletAddress = walletAddress;
					pendingSeedB64 = seedB64;
					pendingPubKeyB64 = pubKeyB64;
					
					for (int i = 0; i < 12; i++) {
						TextView tvNum = findViewById(tvNumIds[i]);
						TextView tvWord = findViewById(tvWordIds[i]);
						if (tvNum != null) tvNum.setText((i + 1) + ".");
						if (tvWord != null) tvWord.setText(phraseWords[i]);
					}
					btnFinish.setEnabled(true);
					btnFinish.setOnClickListener(v -> {
						if (cbAgree != null && !cbAgree.isChecked()) {
							SketchwareUtil.showMessage(context, "Please confirm you have backed up the phrase.");
							return;
						}
						launchMandatoryCardCreation();
					});
				});
			} catch (Exception e) {
				runOnUiThread(() -> {
					SketchwareUtil.showMessage(context, "Error: " + e.getMessage());
					finish();
				});
			}
		}).start();
		
	}
	
	public void _MORE() {
	}
	private static final int RC_MANDATORY_CARD = 8801;
	
	private String pendingMnemonic;
	private String pendingWalletAddress;
	private String pendingSeedB64;
	private String pendingPubKeyB64;
	
	private MaterialButton btnFinish;
	
	private void launchMandatoryCardCreation() {
		Intent cardIntent = new Intent(this, CreateHarvannaCardActivity.class);
		cardIntent.putExtra(CreateHarvannaCardActivity.EXTRA_MANDATORY, true);
		cardIntent.putExtra(CreateHarvannaCardActivity.EXTRA_RAW_MNEMONIC, pendingMnemonic);
		cardIntent.putExtra(CreateHarvannaCardActivity.EXTRA_WALLET_ADDRESS, pendingWalletAddress);
		cardIntent.putExtra(CreateHarvannaCardActivity.EXTRA_LABEL, "your new wallet");
		startActivityForResult(cardIntent, RC_MANDATORY_CARD);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode != RC_MANDATORY_CARD) return;
		
		if (resultCode == Activity.RESULT_OK) {
			persistWalletAndFinish();
		}
		// RESULT_CANCELED: stay on this screen, mnemonic still in memory,
		// btnFinish still wired — they can try the backup card step again.
	}
	
	private void persistWalletAndFinish() {
		try {
			com.harvanna.app.wallet.WalletListManager.addWalletToList(this, pendingWalletAddress, null);
			
			SharedPreferences prefs = getSharedPreferences("harv_wallet_prefs", Context.MODE_PRIVATE);
			prefs.edit()
			.putString("active_wallet_address", pendingWalletAddress)
			.putString("active_address", pendingWalletAddress)
			.putString("wallet_address", pendingWalletAddress)
			.putBoolean("is_wallet_created", true)
			.putBoolean("wallet_initialized", true)
			.putLong("wallet_created_timestamp", System.currentTimeMillis())
			.apply();
			
			com.harvanna.app.wallet.SecureWalletStorage.saveMnemonic(this, pendingWalletAddress, pendingMnemonic);
			com.harvanna.app.wallet.WalletManager.saveKeysLocally(this, pendingWalletAddress, pendingSeedB64, pendingPubKeyB64);
			
			SketchwareUtil.showMessage(getApplicationContext(),
			"Wallet Created!\n" + pendingWalletAddress.substring(0, 10) + "…");
			
			Intent result = new Intent();
			result.putExtra("wallet_address", pendingWalletAddress);
			setResult(RESULT_OK, result);
			finish();
		} catch (Exception e) {
			SketchwareUtil.showMessage(getApplicationContext(), "Save failed: " + e.getMessage());
		}
	}
	
	@Override
	public void onBackPressed() {
		// No half-created wallet to worry about — nothing has been persisted
		// yet at this point, so leaving is always safe.
		super.onBackPressed();
	}
	
	{
	}
	
}
