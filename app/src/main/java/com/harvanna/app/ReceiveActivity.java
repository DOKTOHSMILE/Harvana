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
		activeAddress = com.harvanna.app.wallet.WalletManager.getActiveWalletAddress(this);
		validAddress = com.harvanna.app.tx.NodeSendValidator.isValidAddress(activeAddress);
		
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
				ClipData clip = ClipData.newPlainText("Harvanna Wallet Address", activeAddress);
				clipboard.setPrimaryClip(clip);
				SketchwareUtil.showMessage(this, "Address copied!");
			}
		};
		if (ivCopyAddress != null) ivCopyAddress.setOnClickListener(copyListener);
		if (tvDepositAddress != null) tvDepositAddress.setOnClickListener(copyListener);
		
		if (btnShare != null) {
			if (!validAddress) {
				btnShare.setEnabled(false);
				btnShare.setAlpha(0.4f);
			} else {
				btnShare.setOnClickListener(v -> {
					try {
						Bitmap bitmap = _getBitmapFromView(ivQrCode);
						Uri imageUri = _saveImageToCache(bitmap);
						if (imageUri != null) {
							_shareImage(imageUri, activeAddress);
						}
					} catch (Exception e) {
						SketchwareUtil.showMessage(this, "Couldn't prepare the QR image to share. Try again.");
					}
				});
			}
		}
		
	}
	
	public void _MORE() {
	}
	private String activeAddress;
	private boolean validAddress;
	private void _loadQrCode() {
		if (ivQrCode == null) return;
		if (!validAddress) {
			ivQrCode.setImageDrawable(null);
			return;
		}
		ivQrCode.setImageDrawable(null); // clear while loading
		new Thread(() -> {
			Bitmap qr = null;
			try {
				qr = HARVQRCodeGenerator.generateQRCode(activeAddress, 512);
			} catch (Exception ignored) {}
			
			final Bitmap finalQr = qr;
			runOnUiThread(() -> {
				if (isFinishing() || isDestroyed() || ivQrCode == null) return;
				if (finalQr != null) {
					ivQrCode.setImageBitmap(finalQr);
				} else {
					ivQrCode.setImageDrawable(null);
				}
			});
		}).start();
	}
	
	private void _setupAssetSpinner() {
		final List<String> assetList = new ArrayList<>();
		assetList.add((com.harvanna.app.CoinInfo.name() + " (" + com.harvanna.app.CoinInfo.ticker() + ")"));
		
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(
		this, android.R.layout.simple_spinner_item, assetList) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				TextView tv = (TextView) super.getView(position, convertView, parent);
				tv.setTextColor(getResources().getColor(R.color.colorTextPrimary));
				tv.setTextSize(16);
				tv.setPadding(24, 16, 24, 16);
				return tv;
			}
			@Override
			public View getDropDownView(int position, View convertView, ViewGroup parent) {
				TextView tv = (TextView) super.getDropDownView(position, convertView, parent);
				tv.setTextColor(getResources().getColor(R.color.colorTextPrimary));
				tv.setTextSize(16);
				tv.setPadding(32, 28, 32, 28);
				tv.setBackgroundColor(getResources().getColor(R.color.colorSurface));
				return tv;
			}
		};
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		if (spinnerAssets != null) spinnerAssets.setAdapter(adapter);
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
