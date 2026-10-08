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
		
		if (btnShare != null) {
			btnShare.setOnClickListener(v -> {
				if (!validAddress || activeAddress == null || activeAddress.isEmpty()) {
					SketchwareUtil.showMessage(this, "This address is not ready yet.");
					return;
				}
				try {
					Bitmap bitmap = ivQrCode == null ? null : _getBitmapFromView(ivQrCode);
					Uri imageUri = bitmap == null ? null : _saveImageToCache(bitmap);
					if (imageUri != null) _shareImage(imageUri, activeAddress);
					else SketchwareUtil.showMessage(this, "Couldn't prepare the QR image to share. Try again.");
				} catch (Exception e) {
					SketchwareUtil.showMessage(this, "Couldn't prepare the QR image to share. Try again.");
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
				if (!forAddr.equals(activeAddress) || icon != qrIcon) return;
				if (finalQr != null) ivQrCode.setImageBitmap(finalQr);
			});
		}).start();
	}
	
	private final java.util.List<com.harvana.app.wallet.CoinCatalog.Coin> receiveCoins = new java.util.ArrayList<>();
	private final java.util.Map<String, String> receiveAddrs = new java.util.HashMap<>();

	private void _setupAssetSpinner() {
		receiveCoins.clear();
		receiveCoins.addAll(com.harvana.app.wallet.CoinCatalog.all());
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
			java.util.Map<String, String> found = com.harvana.app.wallet.CoinCatalog.addresses(getApplicationContext(), harv);
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
		startActivityForResult(new Intent(this, PickAssetActivity.class).putExtra(com.harvana.app.wallet.CoinCatalog.EXTRA, key), RC_PICK_ASSET);
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
		android.graphics.drawable.Drawable d = androidx.core.content.ContextCompat.getDrawable(this, icon);
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
		if ((addr == null || addr.isEmpty()) && coin.nativeHarv) addr = activeAddress;
		if (getSupportActionBar() != null) {
			getSupportActionBar().setTitle("Receive " + coin.ticker);
			getSupportActionBar().setSubtitle(coin.network);
		}
		if (textview1 != null) textview1.setText(coin.name);
		if (tvQrHint != null) tvQrHint.setText("Scan to send " + coin.ticker + " to this address");
		if (addr == null || addr.isEmpty()) {
			validAddress = false;
			qrPayload = "";
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
		if (same) {
			if (tvDepositAddress != null) tvDepositAddress.setText(addr);
			return;
		}
		qrIcon = coin.icon;
		activeAddress = addr;
		qrPayload = payUri(coin.key, addr);
		validAddress = addr.length() >= 20;
		if (tvDepositAddress != null) tvDepositAddress.setText(addr);
		if (btnShare != null) { btnShare.setEnabled(true); btnShare.setAlpha(1f); }
		if (ivQrCode != null) ivQrCode.setImageDrawable(null);
		_loadQrCode();
	}

	/** What another wallet should see when it scans. Copy and share still use the plain address. */
	private static String payUri(String key, String address) {
		switch (key) {
			case "BTC": return "bitcoin:" + address;
			case "LTC": return "litecoin:" + address;
			case "DOGE": return "dogecoin:" + address;
			default: return address;
		}
	}
	
	private void _shareImage(final Uri imageUri, final String text) {
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setType("image/png");
		intent.putExtra(Intent.EXTRA_STREAM, imageUri);
		intent.putExtra(Intent.EXTRA_TEXT, text);
		intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		startActivity(Intent.createChooser(intent, "Share Wallet Address"));
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
