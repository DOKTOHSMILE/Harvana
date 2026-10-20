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
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.biometric.*;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
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
import com.harvana.app.group.PendingSpendGuard;
import android.Manifest;
import android.content.pm.PackageManager;
import android.view.inputmethod.InputMethodManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.harvana.app.wallet.WalletManager;
import com.harvana.app.tx.TransactionRouter;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.zxing.Result;
import android.content.ClipboardManager;
import java.math.BigDecimal;
import com.google.android.material.card.MaterialCardView;


public class SendAssetActivity extends AppCompatActivity {
	
	private Toolbar _toolbar;
	private AppBarLayout _app_bar;
	private CoordinatorLayout _coordinator;
	private static final int RC_CAMERA = 100;
	private BottomSheetDialog mScannerDialog;
	private CodeScanner mCodeScanner;
	private EditText mScannerTarget;
	
	private TextView tvSubtitle;
	private NestedScrollView vscroll3;
	private LinearLayout linear4;
	private LinearLayout linear26;
	private TextView tvLabelAsset;
	private MaterialCardView cardview3;
	private TextView tvLabelRecipient;
	private MaterialCardView cardview2;
	private TextView tvLabelAmount;
	private MaterialCardView cardview1;
	private TextView tvLabelMemo;
	private MaterialCardView cardviewMemo;
	private LinearLayout linear2;
	private LinearLayout linear27;
	private LinearLayout linear18;
	private Spinner spAsset;
	private LinearLayout linear17;
	private EditText etRecipient;
	private ImageView btnPaste;
	private ImageView btnScan;
	private EditText etAmount;
	private com.harvana.app.send.AmountPreview amountPreview;
	private EditText etMemo;
	private TextView tvAvailable;
	private ArrayList<HashMap<String, Object>> walletAssets = new ArrayList<>();
	private ArrayList<String> assetDisplayNames = new ArrayList<>();
	private ArrayAdapter<String> assetAdapter;
	private LinearLayout linear19;
	private LinearLayout linear20;
	private LinearLayout linear21;
	private LinearLayout linear22;
	private MaterialButton btnNum1;
	private MaterialButton btnNum2;
	private MaterialButton btnNum3;
	private MaterialButton btnNum4;
	private MaterialButton btnNum5;
	private MaterialButton btnNum6;
	private MaterialButton btnNum7;
	private MaterialButton btnNum8;
	private MaterialButton btnNum9;
	private MaterialButton btnNumDot;
	private MaterialButton btnNum0;
	private MaterialButton btnNumDel;
	private MaterialButton btnCancel;
	private MaterialButton btnSend;
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.send_asset);
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
		tvSubtitle = findViewById(R.id.tvSubtitle);
		if (tvSubtitle != null && tvSubtitle.getParent() instanceof android.view.ViewGroup) {
			com.harvana.app.utils.NetworkStatusBanner.refresh((android.view.ViewGroup) tvSubtitle.getParent(), 0,
					com.harvana.app.wallet.WalletManager.getActiveWalletAddress(this));
		}
		vscroll3 = findViewById(R.id.vscroll3);
		linear4 = findViewById(R.id.linear4);
		linear26 = findViewById(R.id.linear26);
		tvLabelAsset = findViewById(R.id.tvLabelAsset);
		cardview3 = findViewById(R.id.cardview3);
		tvLabelRecipient = findViewById(R.id.tvLabelRecipient);
		cardview2 = findViewById(R.id.cardview2);
		tvLabelAmount = findViewById(R.id.tvLabelAmount);
		cardview1 = findViewById(R.id.cardview1);
		tvLabelMemo = findViewById(R.id.tvLabelMemo);
		cardviewMemo = findViewById(R.id.cardviewMemo);
		linear2 = findViewById(R.id.linear2);
		linear27 = findViewById(R.id.linear27);
		linear18 = findViewById(R.id.linear18);
		spAsset = findViewById(R.id.spAsset);
		linear17 = findViewById(R.id.linear17);
		etRecipient = findViewById(R.id.etRecipient);
		if (etRecipient != null) etRecipient.addTextChangedListener(new android.text.TextWatcher() {
			@Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
			@Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
			@Override public void afterTextChanged(android.text.Editable e) {
				String t = e.toString().trim();
				nameHandler.removeCallbacksAndMessages(null);
				if (t.toLowerCase(java.util.Locale.US).endsWith(".harv")) nameHandler.postDelayed(() -> _resolveRecipientName(t, false, null), 400);
				else if (!t.equalsIgnoreCase(resolvedNameAddress)) _showRecipientName("");
			}
		});
		String prefillTo = getIntent().getStringExtra("toAddress");
		if (prefillTo != null && !prefillTo.trim().isEmpty() && etRecipient != null) etRecipient.setText(prefillTo.trim());
		btnPaste = findViewById(R.id.btnPaste);
		btnScan = findViewById(R.id.btnScan);
		etAmount = findViewById(R.id.etAmount);
		etMemo = findViewById(R.id.etMemo);
		String prefillAmount = getIntent().getStringExtra("amount");
		String prefillMemo = getIntent().getStringExtra("memo");
		if (prefillAmount != null && !prefillAmount.trim().isEmpty() && etAmount != null) etAmount.setText(prefillAmount.trim());
		if (prefillMemo != null && !prefillMemo.trim().isEmpty() && etMemo != null) etMemo.setText(prefillMemo.trim());
		tvAvailable = findViewById(R.id.tvAvailable);
		linear19 = findViewById(R.id.linear19);
		linear20 = findViewById(R.id.linear20);
		linear21 = findViewById(R.id.linear21);
		linear22 = findViewById(R.id.linear22);
		btnNum1 = findViewById(R.id.btnNum1);
		btnNum2 = findViewById(R.id.btnNum2);
		btnNum3 = findViewById(R.id.btnNum3);
		btnNum4 = findViewById(R.id.btnNum4);
		btnNum5 = findViewById(R.id.btnNum5);
		btnNum6 = findViewById(R.id.btnNum6);
		btnNum7 = findViewById(R.id.btnNum7);
		btnNum8 = findViewById(R.id.btnNum8);
		btnNum9 = findViewById(R.id.btnNum9);
		btnNumDot = findViewById(R.id.btnNumDot);
		btnNum0 = findViewById(R.id.btnNum0);
		btnNumDel = findViewById(R.id.btnNumDel);
		btnCancel = findViewById(R.id.btnCancel);
		btnSend = findViewById(R.id.btnSend);
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
		
		_setupSend();
		_setupCustomKeyboard();
	}
	
	
	@Override
	public void onPause() {
		super.onPause();
		if (mCodeScanner != null) {
			mCodeScanner.releaseResources();
		}
	}
	public void _MORE() {
	}
	private boolean isProcessingSend = false;
	private static final int RC_SCAN_RECIPIENT = 4104;
	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == RC_CAMERA) {
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				_showScannerBottomSheet();
			} else {
				SketchwareUtil.showMessage(getApplicationContext(), "Camera permission is required to scan a QR code.");
			}
		}
	}
	
	private void _setupSend() {
		int strokeColor = ContextCompat.getColor(this, R.color.colorAccent);
		int bgColor = ContextCompat.getColor(this, R.color.darkFaint10);
		
		for (EditText et : new EditText[]{etRecipient, etAmount}) {
			if (et != null) {
				et.setShowSoftInputOnFocus(false);
				et.setFocusable(true);
				et.setFocusableInTouchMode(true);
				et.setCursorVisible(false);
				et.setTextIsSelectable(false);
				et.setLongClickable(false);
			}
		}
		
		if (etAmount != null) etAmount.setOnClickListener(this::hideSystemKeyboard);
		if (etRecipient != null) etRecipient.setOnClickListener(this::hideSystemKeyboard);
		
		if (btnPaste != null) {
			btnPaste.setOnClickListener(v -> {
				ClipboardManager cb = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
				if (cb != null && cb.hasPrimaryClip() && cb.getPrimaryClip() != null) {
					ClipData.Item item = cb.getPrimaryClip().getItemAt(0);
					if (item != null && item.getText() != null) {
						String pasted = item.getText().toString().trim();
						_applyScanned(pasted);
						SketchwareUtil.showMessage(getApplicationContext(), "Address pasted");
					} else {
						SketchwareUtil.showMessage(getApplicationContext(), "Clipboard is empty");
					}
				} else {
					SketchwareUtil.showMessage(getApplicationContext(), "Clipboard is empty");
				}
			});
		}
		
		if (btnScan != null) {
			btnScan.setOnClickListener(v -> {
				Intent scanIntent = new Intent(this, com.harvana.app.ScanQrCodeActivity.class);
				scanIntent.putExtra(com.harvana.app.ScanQrCodeActivity.EXTRA_TITLE, "Scan Wallet Address");
				scanIntent.putExtra(com.harvana.app.ScanQrCodeActivity.EXTRA_MODE, com.harvana.app.ScanQrCodeActivity.MODE_WALLET_ADDRESS);
				startActivityForResult(scanIntent, RC_SCAN_RECIPIENT);
			});
		}
		
		View.OnClickListener numClickListener = v -> {
			if (etAmount == null) return;
			String btn = ((Button) v).getText().toString();
			String curr = etAmount.getText().toString();
			
			if (btn.equals(".")) {
				if (curr.contains(".")) return;
				etAmount.setText(curr.isEmpty() ? "0." : curr + ".");
				return;
			}
			if (curr.contains(".")) {
				int dot = curr.indexOf(".");
				if (curr.substring(dot + 1).length() >= 8) return;
			}
			etAmount.setText(curr.equals("0") ? btn : curr + btn);
		};
		
		for (Button b : new Button[]{btnNum1, btnNum2, btnNum3, btnNum4, btnNum5,
			btnNum6, btnNum7, btnNum8, btnNum9, btnNum0, btnNumDot}) {
			if (b != null) b.setOnClickListener(numClickListener);
		}
		
		if (btnNumDel != null) {
			btnNumDel.setOnClickListener(v -> {
				if (etAmount == null) return;
				String curr = etAmount.getText().toString();
				if (!curr.isEmpty()) {
					etAmount.setText(curr.substring(0, curr.length() - 1));
				}
			});
		}
		
		// Group-wallet override: GroupDetailsActivity passes the
		// group's wallet address here for admin transfers, instead of this
		// screen always defaulting to the signed-in user's own wallet.
		// Replaces the old custom bottom sheet in that activity, which
		// duplicated this entire send flow just to swap the sender address.
		String fromWalletOverride = getIntent().getStringExtra("fromWalletAddress");
		final boolean isGroupSend = fromWalletOverride != null && !fromWalletOverride.trim().isEmpty();
		final String myAddress = isGroupSend ? fromWalletOverride.trim() : WalletManager.getActiveWalletAddress(this);
		if (myAddress == null || myAddress.isEmpty()) {
			SketchwareUtil.showMessage(getApplicationContext(), "Please create or recover a wallet first.");
			finish();
			return;
		}
		// What the amount is worth and the node's fee, shown before signing; the amount can be typed in money or in the coin.
		if (etAmount != null && tvAvailable != null) {
			amountPreview = new com.harvana.app.send.AmountPreview(this, etAmount, tvAvailable, new com.harvana.app.send.AmountPreview.Source() {
				private HashMap<String, Object> sel() {
					int pos = spAsset == null ? -1 : spAsset.getSelectedItemPosition();
					return pos >= 0 && pos < walletAssets.size() ? walletAssets.get(pos) : null;
				}
				@Override public String assetKey() { HashMap<String, Object> a = sel(); return a == null ? "HARV" : String.valueOf(a.get("assetKey")); }
				@Override public String symbol() { HashMap<String, Object> a = sel(); return a == null ? "HARV" : String.valueOf(a.get("symbol")); }
				@Override public String from() { HashMap<String, Object> a = sel(); return a != null && a.get("address") instanceof String ? String.valueOf(a.get("address")) : myAddress; }
				@Override public String to() { return etRecipient == null ? "" : etRecipient.getText().toString().trim(); }
			});
		}
		if (isGroupSend) {
			if (getSupportActionBar() != null) getSupportActionBar().setTitle("Send from Group Wallet");
			if (tvSubtitle != null) {
				tvSubtitle.setText("Sending from the group's shared pool, not your personal wallet.");
			}
		}
		
		walletAssets = new ArrayList<>();
		assetDisplayNames = new ArrayList<>();
		
		assetAdapter = new ArrayAdapter<String>(
		this, com.harvana.app.R.layout.spinner_item, assetDisplayNames) {
			@NonNull
			@Override
			public View getView(int pos, View convertView, @NonNull ViewGroup parent) {
				TextView tv = (TextView) super.getView(pos, convertView, parent);
				tv.setTextColor(com.harvana.app.utils.UiColors.textPrimary());
				tv.setTextSize(18);
				tv.setPadding(24, 16, 24, 16);
				_coinIcon(tv, pos);
				return tv;
			}
			
			@Override
			public View getDropDownView(int pos, View convertView, @NonNull ViewGroup parent) {
				TextView tv = (TextView) super.getDropDownView(pos, convertView, parent);
				tv.setTextColor(com.harvana.app.utils.UiColors.textPrimary());
				tv.setTextSize(18);
				tv.setPadding(24, 24, 24, 24);
				tv.setBackgroundColor(UiColors.surface());
				_coinIcon(tv, pos);
				return tv;
			}
		};
		assetAdapter.setDropDownViewResource(com.harvana.app.R.layout.spinner_dropdown_item);
		if (spAsset != null) spAsset.setAdapter(assetAdapter);
		
		com.harvana.app.sync.HarvanaNodeClient.getWallet(myAddress,
		new com.harvana.app.sync.HarvanaNodeClient.JsonCallback() {
			@Override
			public void onSuccess(JSONObject json) {
				runOnUiThread(() -> {
					walletAssets.clear();
					assetDisplayNames.clear();
					
					long rawBalance = json.optLong("balance", 0L);
					double harvBalance = com.harvana.app.sync.HarvanaNodeClient.toHarvDisplay(rawBalance);
					
					HashMap<String, Object> native_ = new HashMap<>();
					native_.put("contractAddress", "");
					native_.put("symbol", com.harvana.app.CoinInfo.ticker());
					native_.put("name", "Harvanacoin");
					native_.put("balance", BigDecimal.valueOf(harvBalance));
					native_.put("decimals", 8);
					native_.put("isNative", true);
					native_.put("isListed", true);
					native_.put("assetKey", "HARV");
					native_.put("icon", com.harvana.app.R.drawable.ic_coin_harv);
					walletAssets.add(native_);
					assetDisplayNames.add((com.harvana.app.CoinInfo.name() + " (" + com.harvana.app.CoinInfo.ticker() + ")"));
					
					_includeChainAssets(myAddress);
					assetAdapter.notifyDataSetChanged();
					_selectRequestedAsset();
					if (spAsset != null && !walletAssets.isEmpty() && spAsset.getSelectedItemPosition() < 0) spAsset.setSelection(0);
				});
			}
			
			@Override
			public void onError(String error) {
				runOnUiThread(() -> {
					walletAssets.clear();
					assetDisplayNames.clear();
					HashMap<String, Object> native_ = new HashMap<>();
					native_.put("contractAddress", "");
					native_.put("symbol", com.harvana.app.CoinInfo.ticker());
					native_.put("name", "Harvanacoin");
					native_.put("balance", BigDecimal.ZERO);
					native_.put("decimals", 8);
					native_.put("isNative", true);
					native_.put("isListed", true);
					native_.put("assetKey", "HARV");
					native_.put("icon", com.harvana.app.R.drawable.ic_coin_harv);
					walletAssets.add(native_);
					assetDisplayNames.add((com.harvana.app.CoinInfo.name() + " (" + com.harvana.app.CoinInfo.ticker() + ")"));
					_includeChainAssets(myAddress);
					assetAdapter.notifyDataSetChanged();
					_selectRequestedAsset();
					SketchwareUtil.showMessage(getApplicationContext(), "Could not fetch balance: " + error);
				});
			}
		});
		
		if (spAsset != null) {
			spAsset.setOnTouchListener((v, event) -> {
				if (event.getAction() == android.view.MotionEvent.ACTION_UP) _openAssetPicker();
				return true;
			});
			spAsset.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
				@Override
				public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
					if (pos < 0 || pos >= walletAssets.size()) return;
					
					HashMap<String, Object> asset = walletAssets.get(pos);
					String ready = asset.get("balanceText") instanceof String ? String.valueOf(asset.get("balanceText")) : "";
					if (tvAvailable != null) {
						if (!ready.isEmpty()) tvAvailable.setText("Available: " + ready);
						else {
							BigDecimal balance = asset.get("balance") instanceof BigDecimal
							? (BigDecimal) asset.get("balance") : BigDecimal.ZERO;
							tvAvailable.setText("Available: " + _formatUIBalance(balance.doubleValue()) + " " + asset.get("symbol"));
						}
					}
					String key = String.valueOf(asset.get("assetKey"));
					if (amountPreview != null) amountPreview.assetChanged();
					if (etRecipient != null) etRecipient.setHint(_addressHint(key));
					_paintRecents(key, String.valueOf(asset.get("symbol")));
					String from = asset.get("address") instanceof String ? String.valueOf(asset.get("address")) : "";
					if (tvSubtitle != null && getIntent().getStringExtra("fromWalletAddress") == null) {
						com.harvana.app.wallet.CoinCatalog.Coin coin = com.harvana.app.wallet.CoinCatalog.get(key);
						if (!from.isEmpty()) tvSubtitle.setText("From " + from);
						else if (coin != null) tvSubtitle.setText("Send " + coin.ticker + " on " + coin.liveNetwork() + ".");
					}
				}
				
				@Override
				public void onNothingSelected(AdapterView<?> parent) {}
			});
		}
		
		if (btnCancel != null) btnCancel.setOnClickListener(v -> finish());
		
		if (btnSend != null) {
			btnSend.setOnClickListener(view -> {
				if (isProcessingSend) return; // Prevent rapid double-taps
				if (amountPreview != null && !amountPreview.normalise()) return;   // typed in money: turn it into the coin amount first
				// A .harv name: look up the wallet it stands for, put that address in the field, then send as usual.
				String typedTo = etRecipient != null && etRecipient.getText() != null ? etRecipient.getText().toString().trim() : "";
				if (typedTo.toLowerCase(java.util.Locale.US).endsWith(".harv")) {
					_resolveRecipientName(typedTo, true, view);
					return;
				}
				
				if (spAsset == null || walletAssets.isEmpty()) {
					SketchwareUtil.showMessage(getApplicationContext(), "No asset selected.");
					return;
				}
				int assetPos = spAsset.getSelectedItemPosition();
				if (assetPos < 0 || assetPos >= walletAssets.size()) {
					SketchwareUtil.showMessage(getApplicationContext(), "No asset selected.");
					return;
				}
				HashMap<String, Object> selectedAsset = walletAssets.get(assetPos);
				String symbol = String.valueOf(selectedAsset.get("symbol"));
				String assetKey = String.valueOf(selectedAsset.get("assetKey"));
				if ("PI".equals(assetKey)) {
					_sendPi(view, selectedAsset);
					return;
				}
				// Harvana's stable coins (hUSD, hNGN, …): same screen, but they move through the stable-asset engine
				// (mintId = the coin's code, fee in HARV gas or the coin itself), not the HARV transfer.
				if (com.harvana.app.husd.Stable.isStable(assetKey)) {
					_sendStable(view, selectedAsset, assetKey);
					return;
				}
				if (assetKey != null && !assetKey.isEmpty() && !"HARV".equals(assetKey) && !"null".equals(assetKey)) {
					_sendChain(view, selectedAsset, assetKey);
					return;
				}
				String contract = String.valueOf(selectedAsset.get("contractAddress"));
				
				String recipient = etRecipient != null ? etRecipient.getText().toString().trim() : "";
				if (recipient.isEmpty()) {
					SketchwareUtil.showMessage(getApplicationContext(), "Please enter a recipient address.");
					return;
				}
				
				String amountText = etAmount != null ? etAmount.getText().toString().trim() : "";
				if (amountText.isEmpty()) {
					SketchwareUtil.showMessage(getApplicationContext(), "Please enter an amount.");
					return;
				}
				
				final BigDecimal transferAmount;
				try {
					transferAmount = new BigDecimal(amountText);
				} catch (Exception e) {
					SketchwareUtil.showMessage(getApplicationContext(), "Invalid amount.");
					return;
				}
				
				if (transferAmount.compareTo(BigDecimal.ZERO) <= 0) {
					SketchwareUtil.showMessage(getApplicationContext(), "Amount must be greater than zero.");
					return;
				}
				
				BigDecimal walletBalance = selectedAsset.get("balance") instanceof BigDecimal
				? (BigDecimal) selectedAsset.get("balance") : BigDecimal.ZERO;
				
				if (transferAmount.compareTo(walletBalance) > 0) {
					SketchwareUtil.showMessage(getApplicationContext(), "Insufficient " + symbol + " balance.");
					return;
				}
				
				final String contractForTx = (contract == null || contract.isEmpty() || "null".equalsIgnoreCase(contract)) ? null : contract;
				
				if (contractForTx == null) {
					isProcessingSend = true; // Lock immediately
					view.setEnabled(false);
					com.harvana.app.tx.TransactionRequest prepReq = new com.harvana.app.tx.TransactionRequest();
					prepReq.context = this;
					prepReq.setSender(myAddress);
					prepReq.setRecipient(recipient);
					prepReq.setAmount(transferAmount);
					prepReq.setContractAddress(null);
					prepReq.setMemo(etMemo != null ? etMemo.getText().toString().trim() : "");
					
					TransactionRouter.prepareNativeTransfer(prepReq, new TransactionRouter.PrepareCallback() {
						@Override
						public void onPrepared(TransactionRouter.PreparedTransfer prepared) {
							runOnUiThread(() -> {
								isProcessingSend = false; // Unlock on success
								view.setEnabled(true);
								_showTransferConfirmSheet(prepared, symbol);
							});
						}
						
						@Override
						public void onError(String error) {
							runOnUiThread(() -> {
								isProcessingSend = false; // Unlock on error
								view.setEnabled(true);
								SketchwareUtil.showMessage(getApplicationContext(), "Could not prepare transfer: " + error);
							});
						}
					});
					return;
				}
				
				com.harvana.app.ui.ProgressSheet progress = new com.harvana.app.ui.ProgressSheet(this);
				progress.setMessage("Broadcasting to node…");
				progress.setCancelable(false);
				
				java.util.ArrayList<String[]> payRows = new java.util.ArrayList<>();
				payRows.add(new String[]{"To", recipient});
				payRows.add(new String[]{"Asset", symbol});
				if (etMemo != null && etMemo.getText() != null && etMemo.getText().toString().trim().length() > 0) {
					payRows.add(new String[]{"Memo", etMemo.getText().toString().trim()});
				}
				com.harvana.app.ui.SendReviewSheet.Data review = _review(symbol, amountText + " " + symbol, transferAmount.doubleValue(), recipient, "Harvana");
				review.feeText = "HARV";
				for (String[] r : payRows) if (!"To".equals(r[0]) && !"Asset".equals(r[0])) review.rows.add(r);
				com.harvana.app.tx.DuplicateSendGuard.check(SendAssetActivity.this, recipient, symbol, amountText, () ->
				com.harvana.app.ui.SendReviewSheet.show(SendAssetActivity.this, review, () -> {
				isProcessingSend = true;
				view.setEnabled(false);
				progress.show();
				
				TransactionRouter.routeTransaction(
				this,
				myAddress,
				recipient,
				transferAmount,
				contractForTx,
				new TransactionRouter.RouterCallback() {
					@Override
					public void onSuccess(final String txHash) {
						isProcessingSend = false;
						com.harvana.app.tx.DuplicateSendGuard.record(SendAssetActivity.this, recipient, symbol, amountText);
						if (!isFinishing() && !isDestroyed() && progress.isShowing()) {
							progress.dismiss();
						}
						runOnUiThread(() -> {
							setResult(RESULT_OK);
							finish();
						});
					}
					
					@Override
					public void onError(final String errorCode, final String errorMessage) {
						isProcessingSend = false;
						if (!isFinishing() && !isDestroyed() && progress.isShowing()) {
							progress.dismiss();
						}
						runOnUiThread(() -> {
							view.setEnabled(true);
							SketchwareUtil.showMessage(getApplicationContext(), errorMessage);
						});
					}
				});
				}));
			});
		}
	}
	
	private void _sendChain(View sendButton, HashMap<String, Object> asset, String assetKey) {
		com.harvana.app.wallet.CoinCatalog.Coin coin = com.harvana.app.wallet.CoinCatalog.get(assetKey);
		String recipient = etRecipient != null ? etRecipient.getText().toString().trim() : "";
		if (recipient.isEmpty()) { SketchwareUtil.showMessage(getApplicationContext(), "Please enter a recipient address."); return; }
		String amountText = etAmount != null ? etAmount.getText().toString().trim() : "";
		if (amountText.isEmpty()) { SketchwareUtil.showMessage(getApplicationContext(), "Please enter an amount."); return; }
		final java.math.BigDecimal amount;
		try { amount = new java.math.BigDecimal(amountText); }
		catch (Exception e) { SketchwareUtil.showMessage(getApplicationContext(), "Invalid amount."); return; }
		if (amount.compareTo(java.math.BigDecimal.ZERO) <= 0) {
			SketchwareUtil.showMessage(getApplicationContext(), "Amount must be greater than zero.");
			return;
		}
		if (!Boolean.TRUE.equals(asset.get("balanceReady"))) {
			SketchwareUtil.showMessage(getApplicationContext(), "The balance is still loading. Wait a moment and try again.");
			return;
		}
		java.math.BigDecimal have = asset.get("balance") instanceof java.math.BigDecimal
				? (java.math.BigDecimal) asset.get("balance") : java.math.BigDecimal.ZERO;
		if (amount.compareTo(have) > 0) {
			SketchwareUtil.showMessage(getApplicationContext(), "Insufficient " + coin.ticker + " balance.");
			return;
		}
		String mine = asset.get("address") instanceof String ? String.valueOf(asset.get("address")) : "";
		if (mine.isEmpty()) {
			SketchwareUtil.showMessage(getApplicationContext(), "This address is still loading. Try again in a moment.");
			return;
		}
		com.harvana.app.ui.SendReviewSheet.Data review = _review(assetKey, amountText + " " + coin.ticker, amount.doubleValue(), recipient, coin.liveNetwork());
		review.feeText = coin.feeTicker();
		review.rows.add(new String[]{"From", mine});
		com.harvana.app.tx.DuplicateSendGuard.check(this, recipient, coin.ticker, amountText, () ->
		com.harvana.app.ui.SendReviewSheet.show(this, review, () -> {
			if (isProcessingSend) return;
			isProcessingSend = true;
			sendButton.setEnabled(false);
			final String from = mine;
			new Thread(() -> {
				String err = null;
				try {
					String phrase = com.harvana.app.wallet.SecureWalletStorage.getActiveMnemonic(getApplicationContext());
					String hash = com.harvana.app.wallet.ChainSend.send(assetKey, phrase, from, recipient, amountText);
					com.harvana.app.wallet.ChainHistory.remember(getApplicationContext(), assetKey, hash, from, recipient, amountText);
				} catch (Exception e) {
					err = e.getMessage() == null ? "The network rejected the payment." : e.getMessage();
				}
				final String fail = err;
				runOnUiThread(() -> {
					isProcessingSend = false;
					if (isFinishing()) return;
					sendButton.setEnabled(true);
					if (fail != null) SketchwareUtil.showMessage(getApplicationContext(), fail);
					else {
						com.harvana.app.tx.DuplicateSendGuard.record(getApplicationContext(), recipient, coin.ticker, amountText);
						SketchwareUtil.showMessage(getApplicationContext(), coin.ticker + " sent.");
						setResult(RESULT_OK);
						finish();
					}
				});
			}).start();
		}));
	}

	private void _sendPi(View sendButton, HashMap<String, Object> asset) {
		String recipient = etRecipient != null ? etRecipient.getText().toString().trim() : "";
		if (!com.harvana.app.wallet.PiKeys.isValidAddress(recipient)) {
			SketchwareUtil.showMessage(getApplicationContext(), "Enter a Pi address. It starts with G and is 56 characters.");
			return;
		}
		String amountText = etAmount != null ? etAmount.getText().toString().trim() : "";
		final java.math.BigDecimal amount;
		try { amount = new java.math.BigDecimal(amountText); }
		catch (Exception e) { SketchwareUtil.showMessage(getApplicationContext(), "Invalid amount."); return; }
		if (amount.compareTo(java.math.BigDecimal.ZERO) <= 0) {
			SketchwareUtil.showMessage(getApplicationContext(), "Amount must be greater than zero.");
			return;
		}
		if (!Boolean.TRUE.equals(asset.get("balanceReady"))) {
			SketchwareUtil.showMessage(getApplicationContext(), "The balance is still loading. Wait a moment and try again.");
			return;
		}
		java.math.BigDecimal have = asset.get("balance") instanceof java.math.BigDecimal
				? (java.math.BigDecimal) asset.get("balance") : java.math.BigDecimal.ZERO;
		if (amount.compareTo(have) > 0) {
			SketchwareUtil.showMessage(getApplicationContext(), "Insufficient PI balance.");
			return;
		}
		String mine = String.valueOf(asset.get("address"));
		if (recipient.equals(mine)) {
			SketchwareUtil.showMessage(getApplicationContext(), "That is your own Pi address.");
			return;
		}
		final String memo = etMemo != null && etMemo.getText() != null ? etMemo.getText().toString().trim() : "";
		com.harvana.app.ui.SendReviewSheet.Data review = _review("PI", amountText + " PI", amount.doubleValue(), recipient, "Pi Network");
		review.feeText = "PI";
		review.rows.add(new String[]{"From", mine});
		if (!memo.isEmpty()) review.rows.add(new String[]{"Memo", memo});
		com.harvana.app.tx.DuplicateSendGuard.check(this, recipient, "PI", amountText, () ->
		com.harvana.app.ui.SendReviewSheet.show(this, review, () -> {
			if (isProcessingSend) return;
			isProcessingSend = true;
			sendButton.setEnabled(false);
			final String amt = amountText;
			new Thread(() -> {
				String err = null;
				try {
					String phrase = com.harvana.app.wallet.SecureWalletStorage.getActiveMnemonic(getApplicationContext());
					if (phrase == null || phrase.trim().isEmpty()) err = "Couldn't load the recovery phrase on this phone.";
					else {
						String derived = com.harvana.app.wallet.PiKeys.piAddress(phrase);
						if (mine != null && !mine.isEmpty() && derived != null && !mine.equals(derived)) err = "The Pi address on screen is not the one this recovery phrase opens. Open Pi again.";
						else com.harvana.app.wallet.PiClient.send(phrase, recipient, amt, memo);
					}
				} catch (Exception e) {
					err = e.getMessage() == null ? "The Pi network rejected the payment." : e.getMessage();
				}
				final String fail = err;
				runOnUiThread(() -> {
					isProcessingSend = false;
					if (isFinishing()) return;
					sendButton.setEnabled(true);
					if (fail != null) SketchwareUtil.showMessage(getApplicationContext(), fail);
					else {
						com.harvana.app.tx.DuplicateSendGuard.record(getApplicationContext(), recipient, "PI", amt);
						SketchwareUtil.showMessage(getApplicationContext(), "Pi sent.");
						setResult(RESULT_OK);
						finish();
					}
				});
			}).start();
		}));
	}

	/** The selected coin's icon, as shown in the coin picker. */
	private android.graphics.drawable.Drawable _assetDrawable() {
		int pos = spAsset == null ? -1 : spAsset.getSelectedItemPosition();
		int icon = com.harvana.app.R.drawable.ic_coin_harv;
		android.graphics.drawable.Drawable own = null;
		if (walletAssets != null && pos >= 0 && pos < walletAssets.size() && walletAssets.get(pos).get("icon") instanceof Integer) {
			icon = (Integer) walletAssets.get(pos).get("icon");
		}
		if (walletAssets != null && pos >= 0 && pos < walletAssets.size()) {
			Object key = walletAssets.get(pos).get("assetKey");
			if (key != null && com.harvana.app.husd.Stable.isStable(String.valueOf(key))) own = com.harvana.app.husd.StableIcon.drawable(this, String.valueOf(key));
		}
		return own != null ? own : androidx.core.content.ContextCompat.getDrawable(this, icon);
	}

	/** Review & Send for the selected coin. */
	private final android.os.Handler nameHandler = new android.os.Handler(android.os.Looper.getMainLooper());
	/** The wallet a typed .harv name stood for, after it was put in the field (so the label under it stays). */
	private String resolvedNameAddress = "";

	/** Looks up a typed name. {@code thenSend}: put the address in the field and press Send again. */
	private void _resolveRecipientName(String name, boolean thenSend, View sendButton) {
		com.harvana.app.sync.NodeGroupClient.resolveName(name, new com.harvana.app.sync.NodeGroupClient.Callback() {
			@Override public void onSuccess(org.json.JSONObject j) {
				runOnUiThread(() -> {
					String owner = j.optString("owner", "");
					if (owner.isEmpty() || isFinishing()) return;
					_showRecipientName(j.optString("name", name) + " → " + owner);
					if (!thenSend || etRecipient == null) return;
					resolvedNameAddress = owner;
					etRecipient.setText(owner);
					if (sendButton != null) sendButton.performClick();
				});
			}
			@Override public void onError(String e) {
				runOnUiThread(() -> {
					_showRecipientName(name + " · not found");
					if (thenSend) SketchwareUtil.showMessage(getApplicationContext(), name + " is not a claimed name.");
				});
			}
		});
	}

	private void _showRecipientName(String text) {
		TextView tv = findViewById(R.id.tv_recipient_name);
		if (tv == null) return;
		tv.setText(text);
		tv.setVisibility(text.isEmpty() ? View.GONE : View.VISIBLE);
	}

	private com.harvana.app.ui.SendReviewSheet.Data _review(String key, String amountText, double amount, String recipient, String network) {
		com.harvana.app.ui.SendReviewSheet.Data d = new com.harvana.app.ui.SendReviewSheet.Data();
		d.assetKey = key;
		d.icon = _assetDrawable();
		d.amountText = amountText;
		d.amount = amount;
		d.recipient = recipient;
		d.network = network;
		return d;
	}

	private void _coinIcon(TextView tv, int pos) {
		int icon = com.harvana.app.R.drawable.ic_coin_harv;
		android.graphics.drawable.Drawable own = null;
		if (walletAssets != null && pos >= 0 && pos < walletAssets.size() && walletAssets.get(pos).get("icon") instanceof Integer) {
			icon = (Integer) walletAssets.get(pos).get("icon");
		}
		if (walletAssets != null && pos >= 0 && pos < walletAssets.size()) {
			Object key = walletAssets.get(pos).get("assetKey");
			if (key != null && com.harvana.app.husd.Stable.isStable(String.valueOf(key))) own = com.harvana.app.husd.StableIcon.drawable(this, String.valueOf(key));
		}
		android.graphics.drawable.Drawable d = own != null ? own : androidx.core.content.ContextCompat.getDrawable(this, icon);
		if (d == null) return;
		int s = (int) (22 * getResources().getDisplayMetrics().density);
		d = d.mutate();
		d.setBounds(0, 0, s, s);
		tv.setCompoundDrawables(d, null, null, null);
		tv.setCompoundDrawablePadding(s / 3);
	}

	/** Adds every other coin under HARV. Balances fill in afterwards, without clearing the list. */
	private void _includeChainAssets(String harvAddress) {
		if (walletAssets == null || assetDisplayNames == null) return;
		for (HashMap<String, Object> row : walletAssets) {
			if (!"HARV".equals(String.valueOf(row.get("assetKey")))) return;
		}
		java.util.List<com.harvana.app.wallet.CoinCatalog.Coin> chainCoins = new java.util.ArrayList<>(com.harvana.app.wallet.CoinCatalog.others());
		chainCoins.addAll(com.harvana.app.wallet.CoinCatalog.addedCoins());          // chains and tokens the person added
		for (com.harvana.app.wallet.CoinCatalog.Coin c : chainCoins) {
			HashMap<String, Object> row = new HashMap<>();
			row.put("contractAddress", "external");
			row.put("symbol", c.ticker);
			row.put("name", c.name);
			row.put("assetKey", c.key);
			row.put("balance", BigDecimal.ZERO);
			row.put("icon", c.icon);
			row.put("decimals", c.decimals);
			walletAssets.add(row);
			assetDisplayNames.add(c.name + " (" + c.ticker + ")");
		}
		_includeStableAssets(harvAddress);
		final String mine = harvAddress;
		new Thread(() -> {
			java.util.Map<String, String> addrs = new java.util.HashMap<>(com.harvana.app.wallet.CoinCatalog.addresses(getApplicationContext(), mine));
			for (com.harvana.app.wallet.CoinCatalog.Coin c : chainCoins) {
				if (c.added == null) continue;
				String a = com.harvana.app.wallet.CustomAssets.address(getApplicationContext(), mine, c.added);
				if (a != null && !a.isEmpty()) addrs.put(c.key, a);
			}
			java.util.Map<String, String> bals = new java.util.HashMap<>();
			for (com.harvana.app.wallet.CoinCatalog.Coin c : chainCoins) {
				String a = addrs.get(c.key);
				if (a == null || a.isEmpty()) continue;
				bals.put(c.key, com.harvana.app.wallet.ChainBalances.assetBalance(c.key, a));
			}
			runOnUiThread(() -> {
				if (isFinishing() || walletAssets == null) return;
				for (HashMap<String, Object> row : walletAssets) {
					String key = String.valueOf(row.get("assetKey"));
					String line = bals.get(key);
					if (line == null) continue;
					row.put("balanceText", line);
					row.put("address", addrs.get(key));
					row.put("balanceReady", true);
					try {
						String num = line.split(" ")[0].replace(",", "");
						if (num.length() > 0 && Character.isDigit(num.charAt(0))) row.put("balance", new BigDecimal(num));
						else row.put("balance", BigDecimal.ZERO);
					} catch (Exception ignored) { row.put("balance", BigDecimal.ZERO); }
				}
				if (assetAdapter != null) assetAdapter.notifyDataSetChanged();
				if (spAsset != null && spAsset.getOnItemSelectedListener() != null && spAsset.getSelectedItemPosition() >= 0) {
					spAsset.getOnItemSelectedListener().onItemSelected(spAsset, null, spAsset.getSelectedItemPosition(), 0);
				}
			});
		}).start();
	}

	/** Adds Harvana's stable coins the network has on (hUSD, hNGN, …) right under HARV, with their balances. They live
	 *  at the HARV address and are sent through the stable-asset engine (see _sendStable). */
	private void _includeStableAssets(String harvAddress) {
		if (harvAddress == null || harvAddress.isEmpty()) return;
		com.harvana.app.husd.HusdClient.list(new com.harvana.app.husd.HusdClient.Cb() {
			@Override public void ok(org.json.JSONObject r) {
				org.json.JSONArray arr = r.optJSONArray("assets");
				java.util.List<String> on = new java.util.ArrayList<>();
				if (arr != null) for (int i = 0; i < arr.length(); i++) {
					org.json.JSONObject a = arr.optJSONObject(i);
					if (a != null && a.optBoolean("active") && com.harvana.app.husd.Stable.isStable(a.optString("asset"))) on.add(a.optString("asset"));
				}
				if (on.isEmpty()) return;
				runOnUiThread(() -> {
					if (isFinishing() || walletAssets == null || assetDisplayNames == null) return;
					final String keep = _selectedKey();          // rows are inserted above it: keep the same coin selected
					int at = 1;                                   // right after HARV
					for (String code : on) {
						boolean have = false;
						for (HashMap<String, Object> row : walletAssets) if (code.equals(String.valueOf(row.get("assetKey")))) have = true;
						if (have) continue;
						HashMap<String, Object> row = new HashMap<>();
						row.put("contractAddress", "stable");
						row.put("symbol", com.harvana.app.husd.Stable.symbol(code));
						row.put("name", com.harvana.app.husd.Stable.name(code));
						row.put("assetKey", code);
						row.put("balance", BigDecimal.ZERO);
						row.put("decimals", 6);
						row.put("address", harvAddress);
						int pos = Math.min(at++, walletAssets.size());
						walletAssets.add(pos, row);
						assetDisplayNames.add(pos, com.harvana.app.husd.Stable.symbol(code) + " · " + com.harvana.app.husd.Stable.name(code));
						com.harvana.app.husd.HusdClient.balance(code, harvAddress, new com.harvana.app.husd.HusdClient.Cb() {
							@Override public void ok(org.json.JSONObject b) {
								long raw = b.optLong("availableRaw");
								runOnUiThread(() -> {
									row.put("balance", java.math.BigDecimal.valueOf(raw, 6));
									row.put("balanceText", com.harvana.app.husd.HusdClient.format(raw) + " " + com.harvana.app.husd.Stable.symbol(code));
									row.put("balanceReady", true);
									row.put("freeTransfersLeft", b.optInt("freeTransfersLeft"));
									if (assetAdapter != null) assetAdapter.notifyDataSetChanged();
									if (spAsset != null && spAsset.getOnItemSelectedListener() != null && spAsset.getSelectedItemPosition() >= 0) {
										spAsset.getOnItemSelectedListener().onItemSelected(spAsset, null, spAsset.getSelectedItemPosition(), 0);
									}
								});
							}
							@Override public void fail(String e) { }
						});
					}
					if (assetAdapter != null) assetAdapter.notifyDataSetChanged();
					if (keep != null && !keep.equals(_selectedKey())) _selectKey(keep);
					_selectRequestedAsset();
				});
			}
			@Override public void fail(String e) { }
		});
	}

	/** Sends a stable coin from the same Send screen. Underneath it is the stable-asset engine: a v1 transfer signed
	 *  with mintId = the coin's code; the node takes HARV gas if the wallet has it, otherwise the coin's own small fee
	 *  (the first 3 are free). */
	private void _sendStable(View sendButton, HashMap<String, Object> asset, String code) {
		String sym = com.harvana.app.husd.Stable.symbol(code);
		String recipient = etRecipient != null ? etRecipient.getText().toString().trim() : "";
		if (!recipient.matches("^HV0x[0-9a-fA-F]{40}$")) {
			SketchwareUtil.showMessage(getApplicationContext(), sym + " goes to a Harvana address (HV0x…).");
			return;
		}
		String amountText = etAmount != null ? etAmount.getText().toString().trim() : "";
		Long units = com.harvana.app.husd.HusdClient.parse(amountText);
		if (units == null) { SketchwareUtil.showMessage(getApplicationContext(), "Enter an amount, up to 6 decimals."); return; }
		if (!Boolean.TRUE.equals(asset.get("balanceReady"))) {
			SketchwareUtil.showMessage(getApplicationContext(), "The balance is still loading. Wait a moment and try again.");
			return;
		}
		java.math.BigDecimal have = asset.get("balance") instanceof java.math.BigDecimal ? (java.math.BigDecimal) asset.get("balance") : java.math.BigDecimal.ZERO;
		if (java.math.BigDecimal.valueOf(units, 6).compareTo(have) > 0) {
			SketchwareUtil.showMessage(getApplicationContext(), "Insufficient " + sym + " balance.");
			return;
		}
		String mine = String.valueOf(asset.get("address"));
		if (recipient.equalsIgnoreCase(mine)) { SketchwareUtil.showMessage(getApplicationContext(), "That's this wallet."); return; }
		final String memo = etMemo != null && etMemo.getText() != null ? etMemo.getText().toString().trim() : "";
		if (memo.toUpperCase(java.util.Locale.ROOT).matches("^H[A-Z]{3}_.*")) {
			SketchwareUtil.showMessage(getApplicationContext(), "That note is reserved by the network. Use a different note.");
			return;
		}
		sendButton.setEnabled(false);
		com.harvana.app.husd.HusdClient.fare(code, mine, recipient, units, memo.isEmpty() ? null : memo, new com.harvana.app.husd.HusdClient.Cb() {
			@Override public void ok(org.json.JSONObject q) {
				runOnUiThread(() -> {
					if (isFinishing()) return;
					sendButton.setEnabled(true);
					if (units > q.optLong("maxSendRaw")) {
						SketchwareUtil.showMessage(getApplicationContext(), "Max " + com.harvana.app.husd.HusdClient.format(q.optLong("maxSendRaw")) + " " + sym
								+ " · tFare " + com.harvana.app.husd.HusdClient.fareText(q, sym));
						return;
					}
					_reviewStable(sendButton, code, sym, mine, recipient, units, memo, q);
				});
			}
			@Override public void fail(String e) {
				runOnUiThread(() -> {
					if (isFinishing()) return;
					sendButton.setEnabled(true);
					SketchwareUtil.showMessage(getApplicationContext(), "Couldn't get the tFare: " + e);
				});
			}
		});
	}

	private void _reviewStable(View sendButton, String code, String sym, String mine, String recipient, long units, String memo, org.json.JSONObject q) {
		java.util.ArrayList<String[]> rows = new java.util.ArrayList<>();
		rows.add(new String[]{"To", recipient});
		rows.add(new String[]{"From", mine});
		rows.add(new String[]{"Network", "Harvana · " + sym});
		if (!memo.isEmpty()) rows.add(new String[]{"Note", memo});
		rows.add(new String[]{"tFare", com.harvana.app.husd.HusdClient.fareText(q, sym)});
		long coinFee = q.optLong("coinFeeRaw");
		rows.add(new String[]{"Total", com.harvana.app.husd.HusdClient.format(units + coinFee) + " " + sym
				+ ("HARV".equals(q.optString("payIn")) ? " + " + com.harvana.app.husd.HusdClient.fareText(q, sym) : "")});
		String shown = com.harvana.app.husd.HusdClient.format(units) + " " + sym;
		String plainAmount = com.harvana.app.husd.HusdClient.format(units).replace(",", "");
		com.harvana.app.ui.SendReviewSheet.Data review = _review(code, shown, units / 1e6, recipient, "Harvana");
		review.feeText = com.harvana.app.husd.HusdClient.fareText(q, sym);
		String payIn = q.optString("payIn");
		if ("HARV".equals(payIn)) { review.feeAmount = q.optLong("harvGasRaw") / 1e8; review.feeKey = "HARV"; }
		else if (!"FREE".equals(payIn)) { review.feeAmount = q.optLong("coinTFareRaw", q.optLong("coinFeeRaw")) / 1e6; review.feeKey = code; }
		for (String[] r : rows) if (!"To".equals(r[0]) && !"Network".equals(r[0]) && !"tFare".equals(r[0])) review.rows.add(r);
		com.harvana.app.tx.DuplicateSendGuard.check(this, recipient, sym, plainAmount, () ->
		com.harvana.app.ui.SendReviewSheet.show(this, review, () -> {
			if (isProcessingSend) return;
			isProcessingSend = true;
			sendButton.setEnabled(false);
			com.harvana.app.husd.HusdClient.send(getApplicationContext(), code, mine, recipient, units, memo.isEmpty() ? null : memo,
					new com.harvana.app.husd.HusdClient.Cb() {
				@Override public void ok(org.json.JSONObject r) {
					runOnUiThread(() -> {
						isProcessingSend = false;
						if (isFinishing()) return;
						com.harvana.app.tx.DuplicateSendGuard.record(getApplicationContext(), recipient, sym, plainAmount);
						String hash = r == null ? "" : r.optString("txHash", r.optString("hash", ""));
						com.harvana.app.ui.PaymentSuccessSheet.Info info = new com.harvana.app.ui.PaymentSuccessSheet.Info();
						info.title = "Transfer";
						info.stateTitle = true;
						info.amount = shown;
						info.amountUsd = com.harvana.app.wallet.UsdValue.text(getApplicationContext(), code, units / 1e6);
						info.from = mine;
						info.to = recipient;
						info.rows = new String[][] {{"To", recipient}, {"From", mine}, {"tFare", review.feeText}, {"Transaction", hash}};
						info.txHash = hash;
						com.harvana.app.tx.PaymentTracker.success(info);
						com.harvana.app.tx.PaymentTracker.Handle h = com.harvana.app.tx.PaymentTracker.begin();
						com.harvana.app.tx.PaymentTracker.endWhenConfirmed(h, hash, () -> com.harvana.app.tx.PaymentTracker.markConfirmed(hash));
						setResult(RESULT_OK);
						finish();
					});
				}
				@Override public void fail(String e) {
					runOnUiThread(() -> {
						isProcessingSend = false;
						if (isFinishing()) return;
						sendButton.setEnabled(true);
						String msg = e == null ? "Couldn't send." : com.harvana.app.utils.ErrorText.is(e, "INSUFFICIENT_" + code) ? "Not enough " + sym + " for this amount and its fee."
								: e.contains(code + "_NOT_ACTIVE") ? sym + " is not switched on yet." : "Couldn't send: " + e;
						SketchwareUtil.showMessage(getApplicationContext(), msg);
					});
				}
			});
		}));
	}

	private static final int RC_PICK_ASSET = 78;

	/** Addresses this phone sent this coin to before, newest first; a tap fills the recipient. */
	private void _paintRecents(String key, String symbol) {
		android.widget.LinearLayout box = findViewById(R.id.recent_recipients);
		View scroll = findViewById(R.id.recent_recipients_scroll);
		if (box == null || scroll == null) return;
		box.removeAllViews();
		boolean harvana = _addressHint(key).startsWith("HV0x");
		java.util.List<String> recents = com.harvana.app.tx.RecentRecipients.forCoin(this, symbol, harvana);
		for (String to : recents) {
			View chip = getLayoutInflater().inflate(R.layout.item_recent_recipient, box, false);
			com.harvana.app.ui.SendReviewSheet.avatar(chip.findViewById(R.id.recent_avatar), to);
			((TextView) chip.findViewById(R.id.recent_address)).setText(com.harvana.app.ui.SendReviewSheet.shortAddress(to));
			chip.setOnClickListener(v -> { if (etRecipient != null) { etRecipient.setText(to); etRecipient.setSelection(to.length()); } });
			box.addView(chip);
		}
		scroll.setVisibility(recents.isEmpty() ? View.GONE : View.VISIBLE);
	}

	private static String _addressHint(String key) {
		boolean test = com.harvana.app.wallet.Net.TEST;
		if (key == null) return "HV0x...";
		switch (key) {
			case "PI": return "G...";
			case "BTC": return test ? "m... or n..." : "1...";
			case "LTC": return test ? "tltc1..." : "ltc1...";
			case "DOGE": return test ? "n..." : "D...";
			case "ETH":
			case "BNB":
			case "POL":
			case "BASE": return "0x...";
			case "TRX": return "T...";
			case "SOL": return "Solana address";
			default: {
				com.harvana.app.wallet.CustomAssets.Asset added = com.harvana.app.wallet.CustomAssets.find(key);
				if (added != null) {                                          // an added chain or token: its chain's address shape
					if (added.near()) return "Account name or 64 characters";
					if (com.harvana.app.wallet.Trc20.FAMILY.equals(added.family)) return "T...";
					if (com.harvana.app.wallet.SplToken.FAMILY.equals(added.family)) return "Solana address";
					return "0x...";
				}
			}
				return "HV0x...";              // HARV and the stable coins (hUSD, hNGN, …)
		}
	}

	private void _openAssetPicker() {
		String key = "HARV";
		if (spAsset != null && walletAssets != null) {
			int i = spAsset.getSelectedItemPosition();
			if (i >= 0 && i < walletAssets.size()) key = String.valueOf(walletAssets.get(i).get("assetKey"));
		}
		startActivityForResult(new Intent(this, PickAssetActivity.class).putExtra(com.harvana.app.wallet.CoinCatalog.EXTRA, key)
				.putExtra(PickAssetActivity.EXTRA_STABLE, true), RC_PICK_ASSET);
	}

	private String pendingCoin;

	private void _applyScanned(String raw) {
		com.harvana.app.scan.WalletQr.Hit hit = com.harvana.app.scan.WalletQr.parse(raw);
		if (etRecipient != null) etRecipient.setText(hit == null ? (raw == null ? "" : raw.trim()) : hit.address);
		if (hit != null && hit.amount != null && !hit.amount.isEmpty() && etAmount != null) etAmount.setText(hit.amount);
		if (hit != null && hit.memo != null && !hit.memo.isEmpty() && etMemo != null) etMemo.setText(hit.memo);
		// Keep the chosen coin when the address already fits it: a Harvana address fits HARV and every stable coin,
		// a 0x address fits every EVM coin. A payment request names the coin, so that one is selected.
		boolean named = hit != null && ((hit.amount != null && !hit.amount.isEmpty()) || (hit.memo != null && !hit.memo.isEmpty()));
		if (hit != null && (named || !_sameFamily(_selectedKey(), hit.key))) {
			pendingCoin = hit.key;
			_selectKey(hit.key);
		}
	}

	private String _selectedKey() {
		if (spAsset == null || walletAssets == null) return null;
		int i = spAsset.getSelectedItemPosition();
		return i >= 0 && i < walletAssets.size() ? String.valueOf(walletAssets.get(i).get("assetKey")) : null;
	}

	private static String _family(String key) {
		if (key == null) return "";
		if ("HARV".equals(key) || com.harvana.app.husd.Stable.isStable(key)) return "HV";
		com.harvana.app.wallet.CustomAssets.Asset added = com.harvana.app.wallet.CustomAssets.find(key);
		if (added != null) {                                              // a token's address is its chain's
			if (added.near()) return "NEAR";
			if (com.harvana.app.wallet.Trc20.FAMILY.equals(added.family)) return "TRX";
			if (com.harvana.app.wallet.SplToken.FAMILY.equals(added.family)) return "SOL";
			return "EVM";                                                  // a 0x address fits every EVM chain, built-in or added
		}
		switch (key) {
			case "ETH": case "BNB": case "POL": case "BASE": return "EVM";
			default: return key;
		}
	}

	private static boolean _sameFamily(String a, String b) {
		return a != null && b != null && _family(a).equals(_family(b));
	}

	private void _selectKey(String key) {
		if (key == null || spAsset == null || walletAssets == null) return;
		for (int i = 0; i < walletAssets.size(); i++) {
			if (key.equals(String.valueOf(walletAssets.get(i).get("assetKey")))) {
				spAsset.setSelection(i);
				pendingCoin = null;
				return;
			}
		}
	}

	/** The coin the screen was opened for, applied once; after that the person's own pick is never changed. */
	private boolean requestApplied;

	private void _selectRequestedAsset() {
		if (spAsset == null || walletAssets == null) return;
		if (pendingCoin != null) { _selectKey(pendingCoin); return; }
		if (requestApplied) return;
		String want = getIntent().getStringExtra(com.harvana.app.wallet.CoinCatalog.EXTRA);
		if (want == null) { requestApplied = true; return; }
		for (HashMap<String, Object> row : walletAssets) {
			if (want.equals(String.valueOf(row.get("assetKey")))) { _selectKey(want); requestApplied = true; return; }
		}
	}

	private void _showTransferConfirmSheet(TransactionRouter.PreparedTransfer prepared, String symbol) {
		String to = prepared.req != null && prepared.req.to != null ? prepared.req.to : "";
		String amt = java.math.BigDecimal.valueOf(prepared.req != null ? prepared.req.amount : 0.0).toPlainString();
		com.harvana.app.tx.DuplicateSendGuard.check(this, to, symbol, amt, () -> _showTransferConfirmSheetNow(prepared, symbol));
	}

	private void _showTransferConfirmSheetNow(TransactionRouter.PreparedTransfer prepared, String symbol) {
		String recipient = (prepared.req != null && prepared.req.to != null) ? prepared.req.to : "";
		double reqAmount = prepared.req != null ? prepared.req.amount : 0.0;
		double total = reqAmount + prepared.feeHarv;
		String fromAddress = prepared.req != null ? prepared.req.from : null;

		com.harvana.app.ui.SendReviewSheet.Data review = _review(symbol, _formatUIBalance(reqAmount) + " " + symbol, reqAmount, recipient, "Harvana");
		review.feeText = _formatUIBalance(prepared.feeHarv) + " " + symbol;
		review.feeAmount = prepared.feeHarv;
		review.feeKey = "HARV";
		if (fromAddress != null) review.rows.add(new String[]{"From", fromAddress});
		if (prepared.req != null && prepared.req.memo != null && !prepared.req.memo.isEmpty()) review.rows.add(new String[]{"Memo", prepared.req.memo});
		review.rows.add(new String[]{"Total", _formatUIBalance(total) + " " + symbol});

		com.harvana.app.ui.SendReviewSheet.show(this, review, sheet -> {
			// Local double-tap guard that survives leaving the screen; the node's nonce and mempool checks are the real backstop.
			if (fromAddress != null && !PendingSpendGuard.tryReserve(SendAssetActivity.this, fromAddress, total)) {
				sheet.error("A send from this wallet is already in progress.");
				return;
			}
			sheet.busy(true);
			final com.harvana.app.tx.PaymentTracker.Handle inFlight = com.harvana.app.tx.PaymentTracker.begin();
			TransactionRouter.executeNativeTransfer(prepared, new TransactionRouter.ExecutionCallback() {
				@Override
				public void onSuccess(com.harvana.app.tx.ExecutionResult result) {
					if (fromAddress != null) PendingSpendGuard.release(SendAssetActivity.this, fromAddress);
					com.harvana.app.tx.DuplicateSendGuard.record(SendAssetActivity.this, recipient, symbol, java.math.BigDecimal.valueOf(reqAmount).toPlainString());
					final String hash = result != null && result.txHash != null ? result.txHash : "";
					com.harvana.app.ui.PaymentSuccessSheet.Info info = new com.harvana.app.ui.PaymentSuccessSheet.Info();
					info.title = "Transfer";
					info.stateTitle = true;
					info.amount = _formatUIBalance(reqAmount) + " " + symbol;
					info.amountUsd = com.harvana.app.wallet.UsdValue.text(SendAssetActivity.this, "HARV", reqAmount);
					info.from = fromAddress == null ? "" : fromAddress;
					info.to = recipient;
					info.rows = new String[][] {{"To", recipient}, {"From", fromAddress == null ? "" : fromAddress},
							{"tFare", review.feeText}, {"Transaction", hash}};
					info.txHash = hash;
					com.harvana.app.tx.PaymentTracker.success(info);
					com.harvana.app.tx.PaymentTracker.endWhenConfirmed(inFlight, hash, () -> com.harvana.app.tx.PaymentTracker.markConfirmed(hash));
					runOnUiThread(() -> {
						sheet.dismiss();
						setResult(RESULT_OK);
						finish();
					});
				}

				@Override
				public void onError(String error) {
					inFlight.end();
					if (fromAddress != null) PendingSpendGuard.release(SendAssetActivity.this, fromAddress);
					runOnUiThread(() -> sheet.error(error));
				}
			});
		}, null);
	}
	
	private void hideSystemKeyboard(View view) {
		InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
		if (imm != null && view != null) {
			imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
		}
	}
	
	public void _checkPermissionAndShowBottomSheet() {
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
			ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, RC_CAMERA);
		} else {
			_showScannerBottomSheet();
		}
	}
	
	public void _showScannerBottomSheet() {
		mScannerDialog = new com.harvana.app.ui.AppSheet(this);
		View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_scanner, null);
		mScannerDialog.setContentView(bottomSheetView);
		
		CodeScannerView scannerComponent = bottomSheetView.findViewById(R.id.scanner_view);
		if (scannerComponent != null) {
			LinearLayout.LayoutParams scannerLp = new LinearLayout.LayoutParams(
			LinearLayout.LayoutParams.MATCH_PARENT,
			LinearLayout.LayoutParams.MATCH_PARENT
			);
			scannerLp.weight = 1;
			scannerComponent.setLayoutParams(scannerLp);
			
			mCodeScanner = new CodeScanner(this, scannerComponent);
			mCodeScanner.setDecodeCallback(new DecodeCallback() {
				@Override
				public void onDecoded(@NonNull final Result result) {
					runOnUiThread(() -> {
						if (mScannerTarget != null) {
							mScannerTarget.setText(result.getText().trim());
						}
						if (mScannerDialog != null && mScannerDialog.isShowing()) {
							mScannerDialog.dismiss();
						}
					});
				}
			});
		}
		
		View candidateTitle = bottomSheetView.findViewById(R.id.textview2);
		if (candidateTitle instanceof TextView) {
			((TextView) candidateTitle).setTextColor(com.harvana.app.utils.UiColors.textPrimary());
			candidateTitle.setPadding(0, 50, 0, 50);
		} else if (bottomSheetView instanceof ViewGroup) {
			ViewGroup vg = (ViewGroup) bottomSheetView;
			if (vg.getChildCount() > 0 && vg.getChildAt(0) instanceof TextView) {
				TextView tv = (TextView) vg.getChildAt(0);
				tv.setTextColor(com.harvana.app.utils.UiColors.textPrimary());
				tv.setPadding(0, 50, 0, 50);
			}
		}
		
		mScannerDialog.setOnShowListener(dialog -> {
			BottomSheetDialog d = (BottomSheetDialog) dialog;
			FrameLayout bottomSheetInternal = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
			
			if (bottomSheetInternal != null) {
				bottomSheetInternal.setBackgroundColor(Color.TRANSPARENT);
				
				float radius = 24f * getResources().getDisplayMetrics().density;
				GradientDrawable background = new GradientDrawable();
				background.setShape(GradientDrawable.RECTANGLE);
				background.setColor(UiColors.surface());
				background.setCornerRadii(new float[]{radius, radius, radius, radius, 0f, 0f, 0f, 0f});
				bottomSheetView.setBackground(background);
				
				BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheetInternal);
				behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
				behavior.setSkipCollapsed(true);
				behavior.setFitToContents(false);
				behavior.setDraggable(true);
				
				ViewGroup.LayoutParams lp = bottomSheetInternal.getLayoutParams();
				lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
				bottomSheetInternal.setLayoutParams(lp);
			}
			
			if (mCodeScanner != null) {
				mCodeScanner.startPreview();
			}
		});
		
		mScannerDialog.setOnDismissListener(dialog -> {
			if (mCodeScanner != null) {
				mCodeScanner.releaseResources();
			}
		});
		
		mScannerDialog.show();
	}
	
	private void _showTransferSuccessSheet(String amountText, String recipientAddress, String txHash) {
		final com.google.android.material.bottomsheet.BottomSheetDialog dialog =
		new com.harvana.app.ui.AppSheet(SendAssetActivity.this);
		
		View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_transfer_success, null);
		dialog.setContentView(sheetView);
		
		dialog.setOnShowListener(d -> {
			android.widget.FrameLayout bottomSheetInternal =
			((com.google.android.material.bottomsheet.BottomSheetDialog) d)
			.findViewById(com.google.android.material.R.id.design_bottom_sheet);
			
			if (bottomSheetInternal != null) {
				bottomSheetInternal.setBackgroundColor(Color.TRANSPARENT);
				com.google.android.material.bottomsheet.BottomSheetBehavior<android.widget.FrameLayout> behavior =
				com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheetInternal);
				behavior.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
				behavior.setSkipCollapsed(true);
				behavior.setFitToContents(false);
				behavior.setDraggable(false);
				ViewGroup.LayoutParams lp = bottomSheetInternal.getLayoutParams();
				lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
				bottomSheetInternal.setLayoutParams(lp);
			}
		});
		
		TextView tvAmount = sheetView.findViewById(R.id.tv_success_amount);
		TextView tvRecipientLabel = sheetView.findViewById(R.id.tv_success_recipient_label);
		TextView tvRecipientAddress = sheetView.findViewById(R.id.tv_success_recipient_address);
		TextView tvDatetime = sheetView.findViewById(R.id.tv_success_datetime);
		View btnShare = sheetView.findViewById(R.id.btn_success_share);
		View btnDone = sheetView.findViewById(R.id.btn_success_done);
		
		final String safeRecipient = recipientAddress != null ? recipientAddress : "";
		String shortAddr = safeRecipient.length() > 14
		? safeRecipient.substring(0, 10) + "\u2026" + safeRecipient.substring(safeRecipient.length() - 4)
		: safeRecipient;
		final String dateTimeStr = new java.text.SimpleDateFormat("dd MMMM yyyy, hh:mm a", java.util.Locale.US)
		.format(new java.util.Date());
		
		if (tvAmount != null) tvAmount.setText(amountText);
		if (tvRecipientLabel != null) tvRecipientLabel.setText("To " + shortAddr);
		if (tvRecipientAddress != null) tvRecipientAddress.setText(safeRecipient);
		if (tvDatetime != null) tvDatetime.setText(dateTimeStr);
		
		if (btnShare != null) {
			btnShare.setOnClickListener(v -> {
				String shareText = "Transfer Successful\n\n" + amountText
				+ "\nTo: " + safeRecipient
				+ "\n" + dateTimeStr
				+ (txHash != null && !txHash.isEmpty() ? "\n\nTx: " + txHash : "");
				android.content.Intent shareIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
				shareIntent.setType("text/plain");
				shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareText);
				startActivity(android.content.Intent.createChooser(shareIntent, "Share Receipt"));
			});
		}
		
		// THE FIX: this is the actual send screen - the sheet was only ever
		// shown by CoinDetailActivity after the fact, via result extras,
		// meaning Send launched from anywhere else (the main wallet Send
		// button, most commonly) never showed any success confirmation at
		// all. Shown directly here now, with Done doing what this screen
		// used to do immediately on success - the person just sees this
		// sheet first.
		if (btnDone != null) {
			btnDone.setOnClickListener(v -> {
				dialog.dismiss();
				setResult(RESULT_OK);
				finish();
			});
		}
		
		dialog.show();
	}
	
	private void _setupCustomKeyboard() {
		if (etAmount != null) {
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
				etAmount.setShowSoftInputOnFocus(false);
			}
			etAmount.setFocusable(true);
			etAmount.setFocusableInTouchMode(true);
			etAmount.setCursorVisible(true);
			etAmount.setOnClickListener(this::hideSystemKeyboard);
		}
		
		View.OnClickListener numClickListener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (etAmount == null) return;
				Button btn = (Button) v;
				String val = btn.getText().toString().trim();
				String current = etAmount.getText().toString().trim();
				
				// Handle decimal point
				if (val.equals(".")) {
					if (current.isEmpty()) {
						etAmount.setText("0.");
					} else if (!current.contains(".")) {
						etAmount.setText(current + ".");
					}
					etAmount.setSelection(etAmount.getText().length());
					return;
				}
				
				// Cap at 8 decimal places for crypto accuracy
				if (current.contains(".")) {
					int dotIndex = current.indexOf(".");
					if (current.substring(dotIndex + 1).length() >= 8) {
						return;
					}
				}
				
				// Replace single '0' or append
				if (current.equals("0")) {
					etAmount.setText(val);
				} else {
					etAmount.setText(current + val);
				}
				etAmount.setSelection(etAmount.getText().length());
			}
		};
		
		// Attach click listeners to keypad buttons
		Button[] buttons = new Button[]{
			btnNum0, btnNum1, btnNum2, btnNum3, btnNum4,
			btnNum5, btnNum6, btnNum7, btnNum8, btnNum9, btnNumDot
		};
		
		for (Button btn : buttons) {
			if (btn != null) {
				btn.setOnClickListener(numClickListener);
			}
		}
		
		// Single click delete / Backspace
		if (btnNumDel != null) {
			btnNumDel.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (etAmount == null) return;
					String current = etAmount.getText().toString();
					if (!current.isEmpty()) {
						etAmount.setText(current.substring(0, current.length() - 1));
						etAmount.setSelection(etAmount.getText().length());
					}
				}
			});
			
			// Long press delete to clear full amount
			btnNumDel.setOnLongClickListener(new View.OnLongClickListener() {
				@Override
				public boolean onLongClick(View v) {
					if (etAmount != null) {
						etAmount.setText("");
					}
					return true;
				}
			});
		}
	}
	
	// NEW UI FORMATTER HELPER
	private String _formatUIBalance(double v) {
		try {
			java.text.DecimalFormat uiDf = new java.text.DecimalFormat("#,##0.########", new java.text.DecimalFormatSymbols(java.util.Locale.US));
			return uiDf.format(v);
		} catch (Exception e) {
			return String.valueOf(v);
		}
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == RC_PICK_ASSET && resultCode == Activity.RESULT_OK && data != null && spAsset != null && walletAssets != null) {
			String key = data.getStringExtra(com.harvana.app.wallet.CoinCatalog.EXTRA);
			for (int i = 0; i < walletAssets.size(); i++) {
				if (key != null && key.equals(String.valueOf(walletAssets.get(i).get("assetKey")))) {
					spAsset.setSelection(i);
					return;
				}
			}
		}
		if (requestCode == RC_SCAN_RECIPIENT && resultCode == Activity.RESULT_OK && data != null) {
			String address = data.getStringExtra(com.harvana.app.ScanQrCodeActivity.EXTRA_RESULT_TEXT);
			if (address != null) _applyScanned(address);
		}
	}
	{
	}
	
}
