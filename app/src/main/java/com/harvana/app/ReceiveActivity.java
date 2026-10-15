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
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
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
import com.google.android.material.button.*;
import com.google.android.material.card.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.storage.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;
import org.json.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import android.content.ClipData;
import android.content.ClipboardManager;
import androidx.core.content.FileProvider;


public class ReceiveActivity extends AppCompatActivity {
	
	private Toolbar _toolbar;
	private AppBarLayout _app_bar;
	private CoordinatorLayout _coordinator;
	
	private LinearLayout linear5;
	private TextView textview1;
	private MaterialCardView cardview1;
	private MaterialCardView cardview2;
	private LinearLayout ll_qr_card;
	private TextView tvQrHint;
	private MaterialButton btnShare;
	private Spinner spinnerAssets;
	private LinearLayout linear6;
	private TextView tvDepositAddress;
	private ImageView ivCopyAddress;
	private ImageView ivQrCode;
	private EditText etReceiveAmount;
	private EditText etReceiveMemo;
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.receive);
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
		linear5 = findViewById(R.id.linear5);
		textview1 = findViewById(R.id.textview1);
		cardview1 = findViewById(R.id.cardview1);
		cardview2 = findViewById(R.id.cardview2);
		ll_qr_card = findViewById(R.id.ll_qr_card);
		tvQrHint = findViewById(R.id.tvQrHint);
		btnShare = findViewById(R.id.btnShare);
		spinnerAssets = findViewById(R.id.spinnerAssets);
		linear6 = findViewById(R.id.linear6);
		tvDepositAddress = findViewById(R.id.tvDepositAddress);
		ivCopyAddress = findViewById(R.id.ivCopyAddress);
		ivQrCode = findViewById(R.id.ivQrCode);
		etReceiveAmount = findViewById(R.id.etReceiveAmount);
		etReceiveMemo = findViewById(R.id.etReceiveMemo);
	}
	
	private void initializeLogic() {
		if (getSupportActionBar() != null) {
			getSupportActionBar().setTitle("Receive Assets");
			getSupportActionBar().setSubtitle("Choose an asset to view its deposit address.");
		}
		activeAddress = com.harvana.app.wallet.WalletManager.getActiveWalletAddress(this);
		validAddress = com.harvana.app.tx.NodeSendValidator.isValidAddress(activeAddress);
		
		if (tvDepositAddress != null) {
			tvDepositAddress.setText(validAddress
			? activeAddress
			: "Create or load a wallet to receive assets");
		}
		
		_loadQrCode();
		_setupAssetSpinner();
		
		View.OnClickListener copyListener = v -> {
			if (!validAddress) {
				SketchwareUtil.showMessage(this, "No valid wallet address to copy.");
				return;
			}
			ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
			if (clipboard != null) {
				ClipData clip = ClipData.newPlainText("Wallet address", activeAddress);
				clipboard.setPrimaryClip(clip);
				SketchwareUtil.showMessage(this, "Address copied!");
			}
		};
		if (ivCopyAddress != null) ivCopyAddress.setOnClickListener(copyListener);
		if (tvDepositAddress != null) tvDepositAddress.setOnClickListener(copyListener);
		android.text.TextWatcher qrWatch = new android.text.TextWatcher() {
			@Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			@Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
			@Override public void afterTextChanged(Editable s) { _scheduleQr(); }
		};
		if (etReceiveAmount != null) etReceiveAmount.addTextChangedListener(qrWatch);
		if (etReceiveMemo != null) etReceiveMemo.addTextChangedListener(qrWatch);
		
		if (btnShare != null) {
			btnShare.setOnClickListener(v -> {
				if (!validAddress || activeAddress == null || activeAddress.isEmpty()) {
					SketchwareUtil.showMessage(this, "This address is not ready yet.");
					return;
				}
				if (qrBuilt == null) { SketchwareUtil.showMessage(this, "The code is not ready yet."); return; }
				try {
					Bitmap card = _renderRequestCard();
					if (card == null) { SketchwareUtil.showMessage(this, "Couldn't build the request card. Try again."); return; }
					Uri imageUri = _saveImageToCache(card);
					if (imageUri != null) _shareImage(imageUri, _shareText());
					else SketchwareUtil.showMessage(this, "Couldn't save the request card. Try again.");
				} catch (Exception e) {
					SketchwareUtil.showMessage(this, "Couldn't build the request card. Try again.");
				}
			});
		}
		
	}
	
	public void _MORE() {
	}
	private String activeAddress;
	private boolean validAddress;
	private int qrIcon = R.drawable.ic_coin_harv;
	private String qrPayload = "";
	private String receiveKey = "HARV";
	private String receiveTicker = "HARV";
	/** The finished code, kept so the slip can be drawn from it once it is built. */
	private Bitmap qrBuilt;
	/** True while the screen is showing the slip rather than the bare code. */
	private boolean showCard;
	private final android.os.Handler qrHandler = new android.os.Handler(android.os.Looper.getMainLooper());
	private final Runnable qrRebuild = this::_rebuildQr;
	private boolean addressesReady = false;
	private void _loadQrCode() {
		if (ivQrCode == null) return;
		if (!validAddress) {
			ivQrCode.setImageDrawable(null);
			return;
		}
		final String forAddr = activeAddress;
		final String encoded = qrPayload == null || qrPayload.isEmpty() ? activeAddress : qrPayload;
		final int icon = qrIcon;
		new Thread(() -> {
			Bitmap qr = null;
			try {
				qr = HARVQRCodeGenerator.generateQRCodeWithLogo(ReceiveActivity.this, encoded, 512, icon);
			} catch (Exception ignored) {}
			
			final Bitmap finalQr = qr;
			runOnUiThread(() -> {
				if (isFinishing() || isDestroyed() || ivQrCode == null) return;
				String now = qrPayload == null || qrPayload.isEmpty() ? activeAddress : qrPayload;
				if (!forAddr.equals(activeAddress) || icon != qrIcon || !encoded.equals(now)) return;
				if (finalQr != null) {
					qrBuilt = finalQr;
					showCard = false;
					ivQrCode.setImageDrawable(null);
					ivQrCode.setImageBitmap(qrBuilt);
				}
			});
		}).start();
	}
	
	private final java.util.List<com.harvana.app.wallet.CoinCatalog.Coin> receiveCoins = new java.util.ArrayList<>();
	private final java.util.Map<String, String> receiveAddrs = new java.util.HashMap<>();

	private void _setupAssetSpinner() {
		receiveCoins.clear();
		// HARV, then every Harvana stable coin (same Harvana address), then the other chains.
		for (com.harvana.app.wallet.CoinCatalog.Coin c : com.harvana.app.wallet.CoinCatalog.all()) {
			receiveCoins.add(c);
			if ("HARV".equals(c.key)) for (String code : com.harvana.app.husd.Stable.CODES) receiveCoins.add(com.harvana.app.wallet.CoinCatalog.stable(code));
		}
		receiveCoins.addAll(com.harvana.app.wallet.CoinCatalog.addedCoins());     // chains and tokens the person added
		java.util.List<String> labels = new java.util.ArrayList<>();
		for (com.harvana.app.wallet.CoinCatalog.Coin c : receiveCoins) labels.add(c.name + " (" + c.ticker + ")");
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(
		this, com.harvana.app.R.layout.spinner_item, labels) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				TextView tv = (TextView) super.getView(position, convertView, parent);
				tv.setTextColor(getResources().getColor(R.color.colorTextPrimary));
				tv.setTextSize(16);
				tv.setPadding(24, 16, 24, 16);
				_coinIcon(tv, position);
				return tv;
			}
			@Override
			public View getDropDownView(int position, View convertView, ViewGroup parent) {
				TextView tv = (TextView) super.getDropDownView(position, convertView, parent);
				tv.setTextColor(getResources().getColor(R.color.colorTextPrimary));
				tv.setTextSize(16);
				tv.setPadding(32, 28, 32, 28);
				tv.setBackgroundColor(getResources().getColor(R.color.colorSurface));
				_coinIcon(tv, position);
				return tv;
			}
		};
		adapter.setDropDownViewResource(com.harvana.app.R.layout.spinner_dropdown_item);
		if (spinnerAssets != null) {
			spinnerAssets.setAdapter(adapter);
			spinnerAssets.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
				@Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) { _showReceiveCoin(position); }
				@Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
			});
			spinnerAssets.setOnTouchListener((v, event) -> {
				if (event.getAction() == android.view.MotionEvent.ACTION_UP) _openAssetPicker();
				return true;
			});
			String want = getIntent().getStringExtra(com.harvana.app.wallet.CoinCatalog.EXTRA);
			if (want != null) {
				for (int i = 0; i < receiveCoins.size(); i++) if (want.equals(receiveCoins.get(i).key)) { spinnerAssets.setSelection(i); break; }
			}
		}
		final String harv = activeAddress;
		new Thread(() -> {
			java.util.Map<String, String> found = new java.util.HashMap<>(com.harvana.app.wallet.CoinCatalog.addresses(getApplicationContext(), harv));
			for (com.harvana.app.wallet.CoinCatalog.Coin c : com.harvana.app.wallet.CoinCatalog.addedCoins()) {
				String a = com.harvana.app.wallet.CustomAssets.address(getApplicationContext(), harv, c.added);
				if (a != null && !a.isEmpty()) found.put(c.key, a);
			}
			runOnUiThread(() -> {
				if (isFinishing()) return;
				receiveAddrs.clear();
				receiveAddrs.putAll(found);
				addressesReady = true;
				if (spinnerAssets != null) _showReceiveCoin(spinnerAssets.getSelectedItemPosition());
			});
		}).start();
	}

	private static final int RC_PICK_ASSET = 77;

	private void _openAssetPicker() {
		String key = "";
		if (spinnerAssets != null) {
			int i = spinnerAssets.getSelectedItemPosition();
			if (i >= 0 && i < receiveCoins.size()) key = receiveCoins.get(i).key;
		}
		startActivityForResult(new Intent(this, PickAssetActivity.class).putExtra(com.harvana.app.wallet.CoinCatalog.EXTRA, key)
				.putExtra(PickAssetActivity.EXTRA_ALL_STABLE, true), RC_PICK_ASSET);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode != RC_PICK_ASSET || resultCode != RESULT_OK || data == null || spinnerAssets == null) return;
		String key = data.getStringExtra(com.harvana.app.wallet.CoinCatalog.EXTRA);
		if (key == null) return;
		for (int i = 0; i < receiveCoins.size(); i++) {
			if (key.equals(receiveCoins.get(i).key)) { spinnerAssets.setSelection(i); return; }
		}
	}

	private void _coinIcon(TextView tv, int pos) {
		int icon = pos >= 0 && pos < receiveCoins.size() ? receiveCoins.get(pos).icon : com.harvana.app.R.drawable.ic_coin_harv;
		String key = pos >= 0 && pos < receiveCoins.size() ? receiveCoins.get(pos).key : "";
		android.graphics.drawable.Drawable d = com.harvana.app.husd.Stable.isStable(key)
				? com.harvana.app.husd.StableIcon.drawable(this, key)
				: androidx.core.content.ContextCompat.getDrawable(this, icon);
		if (d == null) return;
		int s = (int) (22 * getResources().getDisplayMetrics().density);
		d = d.mutate();
		d.setBounds(0, 0, s, s);
		tv.setCompoundDrawables(d, null, null, null);
		tv.setCompoundDrawablePadding(s / 3);
	}

	private void _showReceiveCoin(int position) {
		if (position < 0 || position >= receiveCoins.size()) return;
		com.harvana.app.wallet.CoinCatalog.Coin coin = receiveCoins.get(position);
		String addr = receiveAddrs.get(coin.key);
		if ((addr == null || addr.isEmpty()) && coin.nativeHarv) addr = receiveAddrs.get("HARV");
		if ((addr == null || addr.isEmpty()) && coin.nativeHarv) addr = com.harvana.app.wallet.WalletManager.getActiveWalletAddress(this);
		if (getSupportActionBar() != null) {
			getSupportActionBar().setTitle("Receive " + coin.ticker);
			getSupportActionBar().setSubtitle(coin.liveNetwork());
		}
		if (textview1 != null) textview1.setText(coin.name);
		receiveKey = coin.key;
		receiveTicker = coin.ticker;
		if (addr == null || addr.isEmpty()) {
			validAddress = false;
			qrPayload = "";
			activeAddress = "";
			if (tvDepositAddress != null) tvDepositAddress.setText(!addressesReady
					? "Preparing this address…"
					: ("PI".equals(coin.key)
						? "This phrase has no Pi wallet. Pi needs 24 words."
						: "This address is not ready on this phone."));
			if (ivQrCode != null) ivQrCode.setImageDrawable(null);
			if (btnShare != null) { btnShare.setEnabled(false); btnShare.setAlpha(0.4f); }
			return;
		}
		boolean same = addr.equals(activeAddress) && coin.icon == qrIcon && validAddress
				&& ivQrCode != null && ivQrCode.getDrawable() != null;
		qrIcon = coin.icon;
		activeAddress = addr;
		validAddress = addr.length() >= 20;
		if (tvDepositAddress != null) tvDepositAddress.setText(addr);
		if (btnShare != null) { btnShare.setEnabled(true); btnShare.setAlpha(1f); }
		if (!same) {
			qrPayload = "";
			if (ivQrCode != null) ivQrCode.setImageDrawable(null);
		}
		_rebuildQr();
	}

	private void _scheduleQr() {
		qrHandler.removeCallbacks(qrRebuild);
		qrHandler.postDelayed(qrRebuild, 200);
	}

	/** Rebuilds the code from the address plus whatever amount and memo are typed. Does not depend on the coin being new. */
	private void _rebuildQr() {
		if (!validAddress || activeAddress == null || activeAddress.isEmpty()) return;
		String amount = etReceiveAmount == null || etReceiveAmount.getText() == null ? "" : etReceiveAmount.getText().toString();
		String memo = etReceiveMemo == null || etReceiveMemo.getText() == null ? "" : etReceiveMemo.getText().toString();
		String next = com.harvana.app.scan.WalletQr.payload(receiveKey, activeAddress, amount, memo);
		boolean request = !com.harvana.app.scan.WalletQr.cleanAmount(amount).isEmpty()
				|| !com.harvana.app.scan.WalletQr.cleanMemo(memo).isEmpty();
		if (tvQrHint != null) {
			String amt = com.harvana.app.scan.WalletQr.cleanAmount(amount);
			tvQrHint.setText(request
					? "Scan to send " + (amt.isEmpty() ? receiveTicker : amt + " " + receiveTicker)
					: "Scan to send " + receiveTicker + " to this address");
		}
		if (next.equals(qrPayload) && ivQrCode != null && ivQrCode.getDrawable() != null) return;
		qrPayload = next;
		if (ivQrCode != null) ivQrCode.setImageDrawable(null);
		_loadQrCode();
	}

	/** Puts the slip on screen, so what is shown is what Share sends. */
	private void _toggleCard() {
		if (qrBuilt == null || ivQrCode == null) { SketchwareUtil.showMessage(this, "The code is not ready yet."); return; }
		showCard = !showCard;
		ivQrCode.setImageDrawable(null);
		ivQrCode.setImageBitmap(showCard ? _renderRequestCard() : qrBuilt);
		if (tvQrHint != null) tvQrHint.setText(showCard ? "Request card ready to share" : _hintText());
	}

	/**
	 * The request slip: the same code, drawn on a card in the app's own colours, with the amount, note and address
	 * printed under it. A friend who scans it pays; a friend who reads it sees what the payment is for.
	 */
	private Bitmap _renderRequestCard() {
		float density = getResources().getDisplayMetrics().density;
		int w = Math.round(600 * density), h = Math.round(860 * density);
		int pad = Math.round(24 * density);
		float corner = Math.round(24 * density);
		Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
		Canvas c = new Canvas(out);
		int accent = androidx.core.content.ContextCompat.getColor(this, R.color.colorAccent);
		int accentDark = androidx.core.content.ContextCompat.getColor(this, R.color.colorAccentDark);
		int strong = androidx.core.content.ContextCompat.getColor(this, R.color.colorAccentStrong);
		int ink = androidx.core.content.ContextCompat.getColor(this, R.color.colorTextPrimary);
		int secondary = androidx.core.content.ContextCompat.getColor(this, R.color.colorTextSecondary);
		int amber = androidx.core.content.ContextCompat.getColor(this, R.color.colorWarning);
		int onAccent = androidx.core.content.ContextCompat.getColor(this, R.color.colorTextOnAccent);
		int onAccentMuted = androidx.core.content.ContextCompat.getColor(this, R.color.colorTextOnAccentMuted);
		Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);

		RectF box = new RectF(0, 0, w, h);
		fill.setShader(new LinearGradient(0, 0, w, h, accent, accentDark, Shader.TileMode.CLAMP));
		c.drawRoundRect(box, corner, corner, fill);

		// a faint coin behind the panel, so the card reads as a Harvana card and not a plain box
		fill.setShader(null);
		fill.setColor(androidx.core.content.ContextCompat.getColor(this, R.color.colorOverlayLight));
		c.drawRoundRect(new RectF(w - 200 * density, -80 * density, w + 90 * density, 130 * density), 100 * density, 100 * density, fill);

		int left = pad, right = w - pad, top = pad, bottom = h - Math.round(210 * density);
		float panelW = right - left;
		fill.setColor(androidx.core.content.ContextCompat.getColor(this, R.color.colorQrBackground));
		c.drawRoundRect(new RectF(left, top, right, bottom), 18 * density, 18 * density, fill);

		Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
		text.setTextAlign(Paint.Align.LEFT);

		// ── Header row ──────────────────────────────────────────────
		float y = top + 44 * density;
		text.setColor(strong);
		text.setTextSize(14f * density);
		text.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
		c.drawText("PAYMENT REQUEST", left + 24 * density, y, text);

		text.setColor(accent);
		text.setTextSize(26f * density);
		c.drawText("HARVANA", left + 24 * density, y + 40 * density, text);

		// ticker chip (right-aligned)
		String label = receiveTicker == null || receiveTicker.trim().isEmpty() ? receiveKey : receiveTicker;
		Paint chipText = new Paint(text);
		chipText.setTextSize(13f * density);
		float chipW = chipText.measureText(label) + 28 * density;
		fill.setColor(strong);
		c.drawRoundRect(new RectF(right - chipW - 24 * density, top + 24 * density, right - 24 * density, top + 56 * density), 16 * density, 16 * density, fill);
		chipText.setColor(androidx.core.content.ContextCompat.getColor(this, R.color.colorTextOnAccent));
		c.drawText(label.toUpperCase(java.util.Locale.US), right - chipW - 10 * density, top + 46 * density, chipText);

		// ── QR section ──────────────────────────────────────────────
		float mid = (left + right) / 2f;
		text.setTextAlign(Paint.Align.CENTER);
		text.setColor(ink);
		text.setTextSize(15f * density);
		text.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
		c.drawText("SCAN TO PAY", mid, top + 130 * density, text);

		Bitmap square = _square(qrBuilt);
		int qrSize = Math.round(panelW * 0.62f);
		c.drawBitmap(Bitmap.createScaledBitmap(square, qrSize, qrSize, true), mid - qrSize / 2f, top + 156 * density, null);

		// ── Divider ─────────────────────────────────────────────────
		float split = bottom - 48 * density;
		fill.setColor(androidx.core.content.ContextCompat.getColor(this, R.color.colorCardStroke));
		float dash = 10 * density;
		for (float x = left + 16 * density; x < right - 8 * density; x += dash * 2) {
			c.drawLine(x, split, Math.min(x + dash, right - 8 * density), split, fill);
		}

		// ── Amount + memo ───────────────────────────────────────────
		String amount = etReceiveAmount == null || etReceiveAmount.getText() == null ? "" : etReceiveAmount.getText().toString().trim();
		String memo = etReceiveMemo == null || etReceiveMemo.getText() == null ? "" : etReceiveMemo.getText().toString().trim();

		float lineH = 30 * density;
		float tx = left + 24 * density;
		float maxTextW = panelW - 48 * density;

		text.setTextAlign(Paint.Align.LEFT);
		text.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
		text.setTextSize(20f * density);
		text.setColor(strong);
		String amtLine = amount.isEmpty() ? "Open amount" : amount + "  " + receiveTicker;
		drawWrapped(c, amtLine, tx, split + lineH, maxTextW, lineH, text);

		text.setTextSize(14f * density);
		text.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
		text.setColor(secondary);
		String memoLine = memo.isEmpty() ? qrPayload : memo;
		drawWrapped(c, memoLine, tx, split + lineH * 2.2f, maxTextW, lineH * 0.85f, text);

		// ── Instruction strip ───────────────────────────────────────
		float instrTop = bottom + 20 * density;
		fill.setColor(androidx.core.content.ContextCompat.getColor(this, R.color.colorOverlayLight));
		c.drawRoundRect(new RectF(left, instrTop, right, instrTop + 80 * density), 14 * density, 14 * density, fill);

		text.setTextAlign(Paint.Align.LEFT);
		text.setColor(onAccent);
		text.setTextSize(12.5f * density);
		text.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
		c.drawText("HOW TO PAY", left + 20 * density, instrTop + 26 * density, text);

		text.setColor(onAccentMuted);
		text.setTextSize(12f * density);
		text.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
		c.drawText("Open the Harvana app and scan this code to complete the payment.", left + 20 * density, instrTop + 52 * density, text);

		// ── Footer strip ────────────────────────────────────────────
		float footTop = h - 72 * density;
		fill.setColor(amber);
		c.drawRoundRect(new RectF(left, footTop, right, footTop + 48 * density), 24 * density, 24 * density, fill);
		text.setTextAlign(Paint.Align.CENTER);
		text.setColor(strong);
		text.setTextSize(12.5f * density);
		text.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
		c.drawText("HARVANA PAY", mid, footTop + 31 * density, text);
		return out;
	}

	/** A square copy of the code so the panel never stretches it. */
	private Bitmap _square(Bitmap b) {
		int side = Math.min(b.getWidth(), b.getHeight());
		return Bitmap.createBitmap(b, (b.getWidth() - side) / 2, (b.getHeight() - side) / 2, side, side);
	}

	/** Draws s, wrapping onto as many lines as needed starting at (x, y). */
	private void drawWrapped(Canvas c, String s, float x, float y, float maxW, float lineH, Paint p) {
		if (s == null || s.isEmpty()) return;
		if (p.measureText(s) <= maxW) { c.drawText(s, x, y, p); return; }
		String[] words = s.split(" ");
		StringBuilder line = new StringBuilder();
		float cy = y;
		for (String word : words) {
			String test = line.length() == 0 ? word : line + " " + word;
			if (p.measureText(test) <= maxW) { line.append(line.length() == 0 ? "" : " ").append(word); continue; }
			if (line.length() > 0) { c.drawText(line.toString(), x, cy, p); cy += lineH; line = new StringBuilder(); }
			line.append(word);
		}
		if (line.length() > 0) c.drawText(line.toString(), x, cy, p);
	}

	private String _hintText() {
		String amt = com.harvana.app.scan.WalletQr.cleanAmount(etReceiveAmount == null || etReceiveAmount.getText() == null ? "" : etReceiveAmount.getText().toString());
		return amt.isEmpty()
				? "Scan to send " + receiveTicker + " to this address"
				: "Scan to send " + amt + " " + receiveTicker;
	}

	private void _shareImage(final Uri imageUri, final String text) {
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setType("image/png");
		intent.putExtra(Intent.EXTRA_STREAM, imageUri);
		intent.putExtra(Intent.EXTRA_TEXT, text);
		intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		startActivity(Intent.createChooser(intent, "Share request"));
	}

	/** What accompanies the card: a short line that tells the sender exactly what to do. */
	private String _shareText() {
		String amount = etReceiveAmount == null || etReceiveAmount.getText() == null ? "" : etReceiveAmount.getText().toString().trim();
		String memo = etReceiveMemo == null || etReceiveMemo.getText() == null ? "" : etReceiveMemo.getText().toString().trim();
		StringBuilder sb = new StringBuilder("You've got a payment request on Harvana");
		if (!amount.isEmpty()) {
			sb.append(": ").append(amount).append(' ').append(receiveTicker);
			if (!memo.isEmpty()) { sb.append(" - ").append(memo); }
			sb.append('.');
		} else if (!memo.isEmpty()) {
			sb.append(": ").append(memo).append('.');
		} else {
			sb.append('.');
		}
		sb.append("\n\nOpen Harvana and scan the code to complete the payment.");
		return sb.toString();
	}
	
	private Bitmap _getBitmapFromView(View view) {
		Bitmap returnedBitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(returnedBitmap);
		Drawable bgDrawable = view.getBackground();
		if (bgDrawable != null) {
			bgDrawable.draw(canvas);
		} else {
			canvas.drawColor(Color.WHITE);
		}
		view.draw(canvas);
		return returnedBitmap;
	}
	
	private Uri _saveImageToCache(Bitmap bitmap) {
		File imagesFolder = new File(getCacheDir(), "images");
		Uri uri = null;
		try {
			imagesFolder.mkdirs();
			File file = new File(imagesFolder, "wallet_qr.png");
			FileOutputStream stream = new FileOutputStream(file);
			bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream);
			stream.flush();
			stream.close();
			uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return uri;
	}
	
	{
	}
	
}
