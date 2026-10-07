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
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.biometric.*;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import com.budiyev.android.codescanner.*;
import com.bumptech.glide.*;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.button.*;
import com.google.android.material.card.*;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.storage.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;
import org.json.*;

public class BackupRecoveryActivity extends AppCompatActivity {
	
	private Toolbar _toolbar;
	private AppBarLayout _app_bar;
	private CoordinatorLayout _coordinator;
	
	private LinearLayout linear49;
	private MaterialTextView textview52;
	private MaterialTextView textview53;
	private MaterialCardView cardview8;
	private LinearLayout layoutLockedState;
	private LinearLayout layoutRevealedState;
	private LinearLayout linear50;
	private ImageView imageview44;
	private LinearLayout linear51;
	private MaterialTextView textview54;
	private MaterialTextView textview55;
	private MaterialCardView cardview9;
	private MaterialTextView textview56;
	private MaterialTextView textview57;
	private MaterialButton btnRevealPhrase;
	private ImageView imageview45;
	private MaterialCardView cardview10;
	private MaterialButton btnCopyPhrase;
	private MaterialTextView textview58;
	private GridLayout gridMnemonicWords;
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.backup_recovery);
		initialize(_savedInstanceState);
		initializeLogic();
		com.harvana.app.utils.AppBar.header(this, "Backup & recovery", "Your recovery phrase", R.id.textview52);
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
		linear49 = findViewById(R.id.linear49);
		textview52 = findViewById(R.id.textview52);
		textview53 = findViewById(R.id.textview53);
		cardview8 = findViewById(R.id.cardview8);
		layoutLockedState = findViewById(R.id.layoutLockedState);
		layoutRevealedState = findViewById(R.id.layoutRevealedState);
		linear50 = findViewById(R.id.linear50);
		imageview44 = findViewById(R.id.imageview44);
		linear51 = findViewById(R.id.linear51);
		textview54 = findViewById(R.id.textview54);
		textview55 = findViewById(R.id.textview55);
		cardview9 = findViewById(R.id.cardview9);
		textview56 = findViewById(R.id.textview56);
		textview57 = findViewById(R.id.textview57);
		btnRevealPhrase = findViewById(R.id.btnRevealPhrase);
		imageview45 = findViewById(R.id.imageview45);
		cardview10 = findViewById(R.id.cardview10);
		btnCopyPhrase = findViewById(R.id.btnCopyPhrase);
		textview58 = findViewById(R.id.textview58);
		gridMnemonicWords = findViewById(R.id.gridMnemonicWords);
	}
	
	private void initializeLogic() {
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
		
		btnRevealPhrase.setOnClickListener(v ->
		com.harvana.app.wallet.PinPromptHelper.verifyPinThenProceed(this, this::_revealPhrase));
		
		btnCopyPhrase.setOnClickListener(v -> _copyToClipboard());
		
	}
	
	public void _MORE() {
	}
	private String revealedMnemonic = null;
	private final Handler clipboardClearHandler = new Handler(Looper.getMainLooper());
	
	private void _revealPhrase() {
		String mnemonic = com.harvana.app.wallet.SecureWalletStorage.getActiveMnemonic(getApplicationContext());
		if (mnemonic == null || mnemonic.trim().isEmpty()) {
			Toast.makeText(this, "Couldn't load your recovery phrase on this device.", Toast.LENGTH_LONG).show();
			return;
		}
		revealedMnemonic = mnemonic;
		_populateWordGrid(mnemonic);
		_showPiAddress(mnemonic);
		layoutLockedState.setVisibility(View.GONE);
		layoutRevealedState.setVisibility(View.VISIBLE);
	}
	
	/** Under the words: the Pi wallet the same 24 words open (in Harvana and in Pi's own app), with a copy button. */
	private void _showPiAddress(String mnemonic) {
		android.view.ViewGroup parent = (android.view.ViewGroup) gridMnemonicWords.getParent();
		View old = parent.findViewWithTag("pi_address_row");
		if (old != null) parent.removeView(old);
		String harv = com.harvana.app.wallet.WalletManager.getActiveWalletAddress(this);
		String pi = com.harvana.app.wallet.PiWalletStore.get(this, harv);
		if (pi.isEmpty()) pi = com.harvana.app.wallet.PiWalletStore.rememberFromPhrase(this, harv, mnemonic);
		if (pi.isEmpty()) return;                      // a 12-word phrase: no Pi wallet
		final String piAddr = pi;
		float dd = getResources().getDisplayMetrics().density;
		LinearLayout row = new LinearLayout(this);
		row.setTag("pi_address_row");
		row.setOrientation(LinearLayout.VERTICAL);
		row.setPadding((int) (8 * dd), (int) (14 * dd), (int) (8 * dd), (int) (4 * dd));
		MaterialTextView label = new MaterialTextView(this);
		label.setText("These words also open your Pi wallet");
		label.setTypeface(null, Typeface.BOLD);
		label.setTextSize(14f);
		row.addView(label);
		MaterialTextView addr = new MaterialTextView(this);
		addr.setText(piAddr.substring(0, 8) + "\u2026" + piAddr.substring(piAddr.length() - 8) + "   Copy");
		addr.setTextSize(13f);
		addr.setAlpha(0.8f);
		addr.setPadding(0, (int) (4 * dd), 0, 0);
		addr.setOnClickListener(v -> {
			android.content.ClipboardManager cm = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
			if (cm != null) cm.setPrimaryClip(android.content.ClipData.newPlainText("Pi address", piAddr));
			android.widget.Toast.makeText(this, "Pi address copied", android.widget.Toast.LENGTH_SHORT).show();
		});
		row.addView(addr);
		parent.addView(row, parent.indexOfChild(gridMnemonicWords) + 1);
	}

	private void _populateWordGrid(String mnemonic) {
		gridMnemonicWords.removeAllViews();
		String[] words = mnemonic.trim().split("\\s+");
		
		// Calculate dynamic dimensions based on screen density for perfect scaling
		float density = getResources().getDisplayMetrics().density;
		int margin = (int) (6 * density);
		// 24 words read best three to a row; a 12-word phrase keeps two.
		final int cols = words.length > 12 ? 3 : 2;
		gridMnemonicWords.setColumnCount(cols);
		int paddingInner = (int) ((cols == 3 ? 10 : 14) * density);
		int numToWordSpacing = (int) ((cols == 3 ? 8 : 12) * density);
		
		for (int i = 0; i < words.length; i++) {
			// 1. Create a modern outlined card for each word
			MaterialCardView wordCard = new MaterialCardView(this);
			wordCard.setRadius(10 * density);
			wordCard.setCardElevation(0);
			wordCard.setStrokeWidth((int) Math.max(1, 1 * density));
			wordCard.setStrokeColor(Color.parseColor("#33888888")); // Subtle universal grey outline
			wordCard.setCardBackgroundColor(Color.TRANSPARENT);
			
			// 2. Create an inner horizontal layout
			LinearLayout innerLayout = new LinearLayout(this);
			innerLayout.setOrientation(LinearLayout.HORIZONTAL);
			innerLayout.setPadding(paddingInner, paddingInner, paddingInner, paddingInner);
			innerLayout.setGravity(Gravity.CENTER_VERTICAL);
			
			// 3. Style the index number (Muted and small)
			MaterialTextView numView = new MaterialTextView(this);
			numView.setText(String.valueOf(i + 1));
			numView.setTextSize(13f);
			numView.setAlpha(0.45f);
			
			// 4. Style the actual seed word (Bold and prominent)
			MaterialTextView wordView = new MaterialTextView(this);
			wordView.setText(words[i]);
			wordView.setTextSize(cols == 3 ? 13f : 15f);
			wordView.setSingleLine(true);
			wordView.setTypeface(null, Typeface.BOLD);
			wordView.setPadding(numToWordSpacing, 0, 0, 0);
			
			// Assembly
			innerLayout.addView(numView);
			innerLayout.addView(wordView);
			wordCard.addView(innerLayout);
			
			GridLayout.LayoutParams params = new GridLayout.LayoutParams();
			params.width = 0;
			params.columnSpec = GridLayout.spec(i % cols, 1f);
			params.rowSpec = GridLayout.spec(i / cols);
			params.setMargins(margin, margin, margin, margin);
			wordCard.setLayoutParams(params);
			
			gridMnemonicWords.addView(wordCard);
		}
	}
	
	private void _copyToClipboard() {
		if (revealedMnemonic == null) return;
		android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
		ClipData clip = ClipData.newPlainText("Recovery Phrase", revealedMnemonic);
		clipboard.setPrimaryClip(clip);
		Toast.makeText(this, "Copied — will clear from clipboard in 30 seconds", Toast.LENGTH_SHORT).show();
		
		clipboardClearHandler.removeCallbacksAndMessages(null);
		clipboardClearHandler.postDelayed(() -> {
			ClipData emptyClip = ClipData.newPlainText("Cleared", "");
			if (clipboard != null) {
				clipboard.setPrimaryClip(emptyClip);
			}
		}, 30_000);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		revealedMnemonic = null;
		layoutRevealedState.setVisibility(View.GONE);
		layoutLockedState.setVisibility(View.VISIBLE);
		gridMnemonicWords.removeAllViews();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		clipboardClearHandler.removeCallbacksAndMessages(null);
	}
	
	{
	}
	
}
