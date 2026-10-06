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
	private EditText etMemo;
	private TextView tvAvailable;
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
		String prefillTo = getIntent().getStringExtra("toAddress");
		if (prefillTo != null && !prefillTo.trim().isEmpty() && etRecipient != null) etRecipient.setText(prefillTo.trim());
		btnPaste = findViewById(R.id.btnPaste);
		btnScan = findViewById(R.id.btnScan);
		etAmount = findViewById(R.id.etAmount);
		etMemo = findViewById(R.id.etMemo);
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
						if (etRecipient != null) etRecipient.setText(pasted);
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
		if (isGroupSend) {
			if (getSupportActionBar() != null) getSupportActionBar().setTitle("Send from Group Wallet");
			if (tvSubtitle != null) {
				tvSubtitle.setText("Sending from the group's shared pool, not your personal wallet.");
			}
		}
		
		final ArrayList<HashMap<String, Object>> walletAssets = new ArrayList<>();
		final ArrayList<String> assetDisplayNames = new ArrayList<>();
		
		final ArrayAdapter<String> assetAdapter = new ArrayAdapter<String>(
		this, com.harvana.app.R.layout.spinner_item, assetDisplayNames) {
			@NonNull
			@Override
			public View getView(int pos, View convertView, @NonNull ViewGroup parent) {
				TextView tv = (TextView) super.getView(pos, convertView, parent);
				tv.setTextColor(Color.WHITE);
				tv.setTextSize(18);
				tv.setPadding(24, 16, 24, 16);
				return tv;
			}
			
			@Override
			public View getDropDownView(int pos, View convertView, @NonNull ViewGroup parent) {
				TextView tv = (TextView) super.getDropDownView(pos, convertView, parent);
				tv.setTextColor(Color.WHITE);
				tv.setTextSize(18);
				tv.setPadding(24, 24, 24, 24);
				tv.setBackgroundColor(UiColors.surface());
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
					walletAssets.add(native_);
					assetDisplayNames.add((com.harvana.app.CoinInfo.name() + " (" + com.harvana.app.CoinInfo.ticker() + ")"));
					
					assetAdapter.notifyDataSetChanged();
					if (spAsset != null && !walletAssets.isEmpty()) spAsset.setSelection(0);
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
					walletAssets.add(native_);
					assetDisplayNames.add((com.harvana.app.CoinInfo.name() + " (" + com.harvana.app.CoinInfo.ticker() + ")"));
					assetAdapter.notifyDataSetChanged();
					SketchwareUtil.showMessage(getApplicationContext(), "Could not fetch balance: " + error);
				});
			}
		});
		
		if (spAsset != null) {
			spAsset.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
				@Override
				public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
					if (pos < 0 || pos >= walletAssets.size()) return;
					
					HashMap<String, Object> asset = walletAssets.get(pos);
					BigDecimal balance = asset.get("balance") instanceof BigDecimal
					? (BigDecimal) asset.get("balance") : BigDecimal.ZERO;
					
					if (tvAvailable != null) {
						// APPLIED FIX
						tvAvailable.setText("Available: " + _formatUIBalance(balance.doubleValue()));
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
				
				android.app.ProgressDialog progress = new android.app.ProgressDialog(this);
				progress.setMessage("Broadcasting to node…");
				progress.setCancelable(false);
				
				java.util.ArrayList<String[]> payRows = new java.util.ArrayList<>();
				payRows.add(new String[]{"To", recipient});
				payRows.add(new String[]{"Asset", symbol});
				if (etMemo != null && etMemo.getText() != null && etMemo.getText().toString().trim().length() > 0) {
					payRows.add(new String[]{"Memo", etMemo.getText().toString().trim()});
				}
				payRows.add(new String[]{"Network fee", "Set when this token transfer is sent"});
				com.harvana.app.group.PaymentConfirmSheet.reviewThenAuthorize(SendAssetActivity.this,
				"Send " + symbol, amountText + " " + symbol, payRows, () -> {
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
				});
			});
		}
	}
	
	private void _showTransferConfirmSheet(TransactionRouter.PreparedTransfer prepared, String symbol) {
		final BottomSheetDialog dialog = new BottomSheetDialog(this);
		View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_transfer_confirm, null);
		dialog.setContentView(sheetView);
		dialog.setCancelable(false);
		com.harvana.app.group.BottomSheetUtils.expandFullScreen(dialog, sheetView);
		
		TextView tvAmount = sheetView.findViewById(R.id.tv_confirm_amount);
		TextView tvRecipient = sheetView.findViewById(R.id.tv_confirm_recipient);
		View rowMemo = sheetView.findViewById(R.id.row_confirm_memo);
		TextView tvMemo = sheetView.findViewById(R.id.tv_confirm_memo);
		TextView tvGasFee = sheetView.findViewById(R.id.tv_confirm_gas_fee);
		TextView tvTotal = sheetView.findViewById(R.id.tv_confirm_total);
		TextView tvNonce = sheetView.findViewById(R.id.tv_confirm_nonce);
		TextView tvBlockHeight = sheetView.findViewById(R.id.tv_confirm_block_height);
		TextView tvError = sheetView.findViewById(R.id.tv_confirm_error);
		View btnCancelSheet = sheetView.findViewById(R.id.btn_confirm_cancel);
		Button btnAccept = sheetView.findViewById(R.id.btn_confirm_accept);
		
		String recipient = (prepared.req != null && prepared.req.to != null) ? prepared.req.to : "";
		String shortRecipient = recipient.length() > 14
		? recipient.substring(0, 10) + "…" + recipient.substring(recipient.length() - 4)
		: recipient;
		
		double reqAmount = prepared.req != null ? prepared.req.amount : 0.0;
		double total = reqAmount + prepared.feeHarv;
		
		// APPLIED FIXES
		if (tvAmount != null) tvAmount.setText(_formatUIBalance(reqAmount));
		if (tvRecipient != null) tvRecipient.setText(shortRecipient);
		if (prepared.req != null && prepared.req.memo != null && !prepared.req.memo.isEmpty()) {
			if (rowMemo != null) rowMemo.setVisibility(View.VISIBLE);
			if (tvMemo != null) tvMemo.setText(prepared.req.memo);
		}
		if (tvGasFee != null) tvGasFee.setText(_formatUIBalance(prepared.feeHarv));
		if (tvTotal != null) tvTotal.setText(_formatUIBalance(total));
		if (tvNonce != null) tvNonce.setText(String.valueOf(prepared.nonce));
		if (tvBlockHeight != null) tvBlockHeight.setText(String.valueOf(prepared.blockHeight));
		
		if (btnCancelSheet != null) btnCancelSheet.setOnClickListener(v -> dialog.dismiss());
		
		if (btnAccept != null) {
			btnAccept.setText("Verify");
			btnAccept.setOnClickListener(v -> {
				com.harvana.app.wallet.PinPromptHelper.authorize(SendAssetActivity.this,
				"Send " + _formatUIBalance(reqAmount) + " " + symbol, () -> {
					String fromAddress = prepared.req != null ? prepared.req.from : null;
					
					// THE FIX: this screen had no PendingSpendGuard at all —
					// only btnAccept.setEnabled(false), transient in-memory
					// state that doesn't survive backing out and reopening the
					// screen, or the Activity being killed by Android mid-send.
					// Group contributions already use this exact reservation
					// correctly; this is the general wallet send screen —
					// arguably the more-used path — catching up to the same
					// protection. The node's own nonce/mempool checks remain
					// the real backstop either way; this is the fast local
					// double-tap guard in front of it.
					if (fromAddress != null && !PendingSpendGuard.tryReserve(SendAssetActivity.this, fromAddress, total)) {
						if (tvError != null) {
							tvError.setText("A send from this wallet is already in progress. Please wait for it to finish before trying again.");
							tvError.setVisibility(View.VISIBLE);
						}
						return;
					}
					
					btnAccept.setEnabled(false);
					btnAccept.setText("Sending…");
					if (tvError != null) tvError.setVisibility(View.GONE);
					
					TransactionRouter.executeNativeTransfer(prepared, new TransactionRouter.ExecutionCallback() {
						@Override
						public void onSuccess(com.harvana.app.tx.ExecutionResult result) {
							if (fromAddress != null) PendingSpendGuard.release(SendAssetActivity.this, fromAddress);
							runOnUiThread(() -> {
								dialog.dismiss();
								// THE FIX: used to setResult+finish immediately here with
								// no confirmation shown at all unless the caller happened
								// to be CoinDetailActivity, which showed it after the fact
								// via these same result extras. Shown directly now.
								
								// APPLIED FIX
								_showTransferSuccessSheet(
								_formatUIBalance(reqAmount),
								recipient,
								result != null ? result.txHash : "");
							});
						}
						
						@Override
						public void onError(String error) {
							if (fromAddress != null) PendingSpendGuard.release(SendAssetActivity.this, fromAddress);
							runOnUiThread(() -> {
								btnAccept.setEnabled(true);
								btnAccept.setText("Verify");
								if (tvError != null) {
									tvError.setText(error);
									tvError.setVisibility(View.VISIBLE);
								}
							});
						}
					});
				});
			});
		}
		
		dialog.show();
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
		mScannerDialog = new BottomSheetDialog(this);
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
			((TextView) candidateTitle).setTextColor(Color.WHITE);
			candidateTitle.setPadding(0, 50, 0, 50);
		} else if (bottomSheetView instanceof ViewGroup) {
			ViewGroup vg = (ViewGroup) bottomSheetView;
			if (vg.getChildCount() > 0 && vg.getChildAt(0) instanceof TextView) {
				TextView tv = (TextView) vg.getChildAt(0);
				tv.setTextColor(Color.WHITE);
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
		new com.google.android.material.bottomsheet.BottomSheetDialog(SendAssetActivity.this);
		
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
		if (requestCode == RC_SCAN_RECIPIENT && resultCode == Activity.RESULT_OK && data != null) {
			String address = data.getStringExtra(com.harvana.app.ScanQrCodeActivity.EXTRA_RESULT_TEXT);
			if (address != null && etRecipient != null) {
				etRecipient.setText(address);
			}
		}
	}
	{
	}
	
}
