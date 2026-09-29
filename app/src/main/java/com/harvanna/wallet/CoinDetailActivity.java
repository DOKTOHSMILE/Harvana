package com.harvanna.wallet;

import com.harvanna.wallet.utils.UiColors;
import android.Manifest;
import android.animation.*;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
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
import android.view.View;
import android.view.View.*;
import android.view.animation.*;
import android.widget.*;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.biometric.*;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import com.harvanna.wallet.wallet.WalletManager;
import com.budiyev.android.codescanner.*;
import com.budiyev.android.codescanner.CodeScanner;
import com.budiyev.android.codescanner.CodeScannerView;
import com.budiyev.android.codescanner.DecodeCallback;
import com.bumptech.glide.*;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.*;
import com.google.android.material.card.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.storage.*;
import com.google.zxing.Result;
import java.io.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.*;
import java.util.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.regex.*;
import org.json.*;
import org.json.JSONObject;

public class CoinDetailActivity extends AppCompatActivity {
	
	private Toolbar _toolbar;
	private AppBarLayout _app_bar;
	private CoordinatorLayout _coordinator;
	private String walletAddress = "";
	private String wallet_address = "";
	private CodeScanner mCodeScanner;
	private EditText mScannerTarget;
	private static final int SEND_ASSET_REQUEST_CODE = 2001;
	
	private LinearLayout linear3;
	private NestedScrollView vscroll2;
	private LinearLayout layout_buttons;
	private LinearLayout linear10;
	private MaterialCardView cardview1;
	private LinearLayout linear7;
	private TextView textview14;
	private TextView textview15;
	private MaterialCardView cardview4;
	private TextView textview2;
	private MaterialCardView cardview7;
	private MaterialCardView cardview8;
	private TextView tv_coin_info_note;
	private ImageView img_coin_logo;
	private TextView tv_coin_balance;
	private TextView tv_wallet_address;
	private LinearLayout linear11;
	private TextView tv_coin_description;
	private LinearLayout linear12;
	private LinearLayout row_current_supply;
	private LinearLayout row_max_supply;
	private LinearLayout row_circulating_supply;
	private LinearLayout row_total_holders;
	private LinearLayout row_block_height;
	private LinearLayout row_block_reward;
	private LinearLayout row_halving;
	private LinearLayout row_network;
	private TextView tv_stat_label_1;
	private TextView tv_stat_value_1;
	private TextView tv_stat_label_2;
	private TextView tv_stat_value_2;
	private TextView tv_stat_label_3;
	private TextView tv_stat_value_3;
	private TextView tv_stat_label_4;
	private TextView tv_stat_value_4;
	private TextView tv_stat_label_5;
	private TextView tv_stat_value_5;
	private TextView tv_stat_label_6;
	private TextView tv_stat_value_6;
	private TextView tv_stat_label_7;
	private TextView tv_stat_value_7;
	private TextView tv_stat_label_8;
	private TextView tv_stat_value_8;
	private MaterialButton btn_receive;
	private MaterialButton btn_send;
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.coin_detail);
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
		linear3 = findViewById(R.id.linear3);
		vscroll2 = findViewById(R.id.vscroll2);
		layout_buttons = findViewById(R.id.layout_buttons);
		linear10 = findViewById(R.id.linear10);
		cardview1 = findViewById(R.id.cardview1);
		linear7 = findViewById(R.id.linear7);
		textview14 = findViewById(R.id.textview14);
		textview15 = findViewById(R.id.textview15);
		cardview4 = findViewById(R.id.cardview4);
		textview2 = findViewById(R.id.textview2);
		cardview7 = findViewById(R.id.cardview7);
		cardview8 = findViewById(R.id.cardview8);
		tv_coin_info_note = findViewById(R.id.tv_coin_info_note);
		img_coin_logo = findViewById(R.id.img_coin_logo);
		tv_coin_balance = findViewById(R.id.tv_coin_balance);
		tv_wallet_address = findViewById(R.id.tv_wallet_address);
		linear11 = findViewById(R.id.linear11);
		tv_coin_description = findViewById(R.id.tv_coin_description);
		linear12 = findViewById(R.id.linear12);
		row_current_supply = findViewById(R.id.row_current_supply);
		row_max_supply = findViewById(R.id.row_max_supply);
		row_circulating_supply = findViewById(R.id.row_circulating_supply);
		row_total_holders = findViewById(R.id.row_total_holders);
		row_block_height = findViewById(R.id.row_block_height);
		row_block_reward = findViewById(R.id.row_block_reward);
		row_halving = findViewById(R.id.row_halving);
		row_network = findViewById(R.id.row_network);
		tv_stat_label_1 = findViewById(R.id.tv_stat_label_1);
		tv_stat_value_1 = findViewById(R.id.tv_stat_value_1);
		tv_stat_label_2 = findViewById(R.id.tv_stat_label_2);
		tv_stat_value_2 = findViewById(R.id.tv_stat_value_2);
		tv_stat_label_3 = findViewById(R.id.tv_stat_label_3);
		tv_stat_value_3 = findViewById(R.id.tv_stat_value_3);
		tv_stat_label_4 = findViewById(R.id.tv_stat_label_4);
		tv_stat_value_4 = findViewById(R.id.tv_stat_value_4);
		tv_stat_label_5 = findViewById(R.id.tv_stat_label_5);
		tv_stat_value_5 = findViewById(R.id.tv_stat_value_5);
		tv_stat_label_6 = findViewById(R.id.tv_stat_label_6);
		tv_stat_value_6 = findViewById(R.id.tv_stat_value_6);
		tv_stat_label_7 = findViewById(R.id.tv_stat_label_7);
		tv_stat_value_7 = findViewById(R.id.tv_stat_value_7);
		tv_stat_label_8 = findViewById(R.id.tv_stat_label_8);
		tv_stat_value_8 = findViewById(R.id.tv_stat_value_8);
		btn_receive = findViewById(R.id.btn_receive);
		btn_send = findViewById(R.id.btn_send);
		
		btn_receive.setOnClickListener(_v -> _receiveBottomSheet());
		
		btn_send.setOnClickListener(_v -> {
			Intent intent = new Intent(CoinDetailActivity.this, SendAssetActivity.class);
			startActivityForResult(intent, SEND_ASSET_REQUEST_CODE);
			
		});
	}
	
	private void initializeLogic() {
		androidx.appcompat.app.ActionBar ab = getSupportActionBar();
		int backgroundColor = androidx.core.content.ContextCompat.getColor(this, R.color.colorBackground);
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			final Window window = this.getWindow();
			window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
			window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
			com.harvanna.wallet.utils.StatusBarUtil.matchToolbar(this);
		}
		
		walletAddress = getIntent().getStringExtra("walletAddress");
		wallet_address = getIntent().getStringExtra("walletAddress");
		if (walletAddress != null && !walletAddress.trim().isEmpty()) {
			tv_wallet_address.setText(walletAddress);
			_getBallance(walletAddress, tv_coin_balance);
		} else {
			tv_wallet_address.setText("No Wallet Address Found");
		}
		
		setTitle("Ajoo Coin");
		_loadTokenomicsInfo();
		
	}
	
	
	@Override
	public void onStart() {
		super.onStart();
		// Add to onStart
		androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).registerReceiver(
		globalTxReceiver, 
		new android.content.IntentFilter("com.harvanna.wallet.ACTION_WALLET_RECEIVED")
		);
	}
	
	@Override
	public void onStop() {
		super.onStop();
		// Add to onStop
		try {
			androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).unregisterReceiver(globalTxReceiver);
		} catch (Exception ignored) {}
		
	}
	public void _MORE() {
	}
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == SEND_ASSET_REQUEST_CODE && resultCode == RESULT_OK) {
			_loadAvailableCoins();
			if (data != null && data.hasExtra("txHash")) {
				_showTransferSuccessSheet(
				data.getStringExtra("amountText"),
				data.getStringExtra("recipient"),
				data.getStringExtra("txHash"));
			}
		}
	}
	
	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == 1000) {
			initializeLogic();
		}
	}
	
	private void _loadTokenomicsInfo() {
		_setStatRow(R.id.row_current_supply, "Current Supply", "Loading…");
		_setStatRow(R.id.row_max_supply, "Max Supply", "Loading…");
		_setStatRow(R.id.row_circulating_supply, "Circulating Supply", "Loading…");
		_setStatRow(R.id.row_total_holders, "Total Holders", "Loading…");
		_setStatRow(R.id.row_block_height, "Block Height", "Loading…");
		_setStatRow(R.id.row_block_reward, "Block Reward", "Loading…");
		_setStatRow(R.id.row_halving, "Next Halving", "Loading…");
		_setStatRow(R.id.row_network, "Network", "AjooChain testnet");
		
		// Total Holders — was hardcoded to "Not available" with nothing
		// ever calling the node for it. Independent of the genesis/tip
		// fetch below since it doesn't depend on either.
		com.harvanna.wallet.sync.AjooNodeClient.getSupply(new com.harvanna.wallet.sync.AjooNodeClient.JsonCallback() {
			@Override
			public void onSuccess(org.json.JSONObject supplyJson) {
				long holderCount = supplyJson.optLong("holderCount", -1L);
				runOnUiThread(() -> _setStatRow(R.id.row_total_holders, "Total Holders",
				holderCount >= 0 ? String.valueOf(holderCount) : "Unavailable"));
			}
			
			@Override
			public void onError(String error) {
				runOnUiThread(() -> _setStatRow(R.id.row_total_holders, "Total Holders", "Unavailable"));
			}
		});
		
		// The node is the only source of truth for these two - fetched
		// first, since everything below is derived from them. No
		// client-side fallback if this fails: "Unavailable" is honest,
		// silently guessing a value the node hasn't confirmed is exactly
		// what produced a wrong max supply before (ChainConstants had
		// 50.0/210_000 hardcoded - the node's real values are 1.25/
		// 23_260_000, confirmed directly against the live node).
		com.harvanna.wallet.sync.AjooNodeClient.getNodeStatus(new com.harvanna.wallet.sync.AjooNodeClient.JsonCallback() {
			@Override
			public void onSuccess(org.json.JSONObject statusJson) {
				long rawGenesisReward = statusJson.optLong("genesisReward", -1L);
				long halvingInterval = statusJson.optLong("halvingInterval", -1L);
				
				if (rawGenesisReward <= 0 || halvingInterval <= 0) {
					runOnUiThread(() -> _markTokenomicsUnavailable());
					return;
				}
				
				double genesisReward = com.harvanna.wallet.sync.AjooNodeClient.toAjcDisplay(rawGenesisReward);
				
				runOnUiThread(() -> _setStatRow(R.id.row_max_supply, "Max Supply",
				com.harvanna.wallet.wallet.BalanceFormatter.smart(
				ChainMath.calculateMaxSupply(genesisReward, halvingInterval))));
				
				com.harvanna.wallet.sync.AjooNodeClient.getTip(new com.harvanna.wallet.sync.AjooNodeClient.JsonCallback() {
					@Override
					public void onSuccess(org.json.JSONObject tipJson) {
						long height = tipJson.optLong("blockHeight", -1L);
						long rawReward = tipJson.optLong("reward", 0L);
						
						double circulating = ChainMath.calculateCirculatingSupply(
						height, genesisReward, halvingInterval);
						// Prefer the node's own live-reported reward over the
						// locally-recomputed one when it's present - the real,
						// authoritative figure rather than a client-side guess.
						double blockReward = rawReward > 0
						? com.harvanna.wallet.sync.AjooNodeClient.toAjcDisplay(rawReward)
						: ChainMath.currentBlockReward(height, genesisReward, halvingInterval);
						long blocksLeft = ChainMath.blocksUntilNextHalving(height, halvingInterval);
						int era = ChainMath.currentHalvingEra(height, halvingInterval);
						
						runOnUiThread(() -> {
							_setStatRow(R.id.row_current_supply, "Current Supply",
							com.harvanna.wallet.wallet.BalanceFormatter.smart(circulating));
							_setStatRow(R.id.row_circulating_supply, "Circulating Supply",
							com.harvanna.wallet.wallet.BalanceFormatter.smart(circulating));
							_setStatRow(R.id.row_block_height, "Block Height", String.valueOf(height));
							_setStatRow(R.id.row_block_reward, "Block Reward",
							com.harvanna.wallet.wallet.BalanceFormatter.smart(blockReward));
							_setStatRow(R.id.row_halving, "Next Halving",
							blocksLeft + " blocks (era " + era + ")");
						});
					}
					
					@Override
					public void onError(String error) {
						runOnUiThread(() -> {
							_setStatRow(R.id.row_current_supply, "Current Supply", "Unavailable");
							_setStatRow(R.id.row_circulating_supply, "Circulating Supply", "Unavailable");
							_setStatRow(R.id.row_block_height, "Block Height", "Unavailable");
							_setStatRow(R.id.row_block_reward, "Block Reward", "Unavailable");
							_setStatRow(R.id.row_halving, "Next Halving", "Unavailable");
						});
					}
				});
			}
			
			@Override
			public void onError(String error) {
				runOnUiThread(() -> _markTokenomicsUnavailable());
			}
		});
	}
	
	private void _markTokenomicsUnavailable() {
		_setStatRow(R.id.row_current_supply, "Current Supply", "Unavailable");
		_setStatRow(R.id.row_max_supply, "Max Supply", "Unavailable");
		_setStatRow(R.id.row_circulating_supply, "Circulating Supply", "Unavailable");
		_setStatRow(R.id.row_block_height, "Block Height", "Unavailable");
		_setStatRow(R.id.row_block_reward, "Block Reward", "Unavailable");
		_setStatRow(R.id.row_halving, "Next Halving", "Unavailable");
	}
	
	private void _setStatRow(int rowId, String label, String value) {
		_setStatRow(rowId, 0, 0, label, value);
	}
	
	// THE FIX: each row used to look up a shared "tv_stat_label"/
	// "tv_stat_value" child id, which is valid in raw Android XML (lookups
	// are scoped per-row so it's not actually ambiguous) but Sketchware
	// Pro's editor maintains its own per-screen id registry and doesn't
	// carry a duplicate id across 8 sibling rows on one screen the way
	// plain XML allows — only the first row (Current Supply) survived
	// intact once the file was opened/saved in Sketchware, which is why
	// every other row stayed stuck on its literal XML placeholder ("—").
	// Every row's label/value pair now has its own unique id
	// (tv_stat_label_1..8 / tv_stat_value_1..8), and this maps rowId to
	// its specific pair instead of relying on a shared child id at all.
	private void _setStatRow(int rowId, int labelIdOverride, int valueIdOverride, String label, String value) {
		View row = findViewById(rowId);
		if (row == null) return;
		int labelId = labelIdOverride != 0 ? labelIdOverride : _labelIdFor(rowId);
		int valueId = valueIdOverride != 0 ? valueIdOverride : _valueIdFor(rowId);
		TextView tvLabel = row.findViewById(labelId);
		TextView tvValue = row.findViewById(valueId);
		if (tvLabel != null) tvLabel.setText(label);
		if (tvValue != null) tvValue.setText(value);
	}
	
	private int _labelIdFor(int rowId) {
		if (rowId == R.id.row_current_supply) return R.id.tv_stat_label_1;
		if (rowId == R.id.row_max_supply) return R.id.tv_stat_label_2;
		if (rowId == R.id.row_circulating_supply) return R.id.tv_stat_label_3;
		if (rowId == R.id.row_total_holders) return R.id.tv_stat_label_4;
		if (rowId == R.id.row_block_height) return R.id.tv_stat_label_5;
		if (rowId == R.id.row_block_reward) return R.id.tv_stat_label_6;
		if (rowId == R.id.row_halving) return R.id.tv_stat_label_7;
		if (rowId == R.id.row_network) return R.id.tv_stat_label_8;
		return 0;
	}
	
	private int _valueIdFor(int rowId) {
		if (rowId == R.id.row_current_supply) return R.id.tv_stat_value_1;
		if (rowId == R.id.row_max_supply) return R.id.tv_stat_value_2;
		if (rowId == R.id.row_circulating_supply) return R.id.tv_stat_value_3;
		if (rowId == R.id.row_total_holders) return R.id.tv_stat_value_4;
		if (rowId == R.id.row_block_height) return R.id.tv_stat_value_5;
		if (rowId == R.id.row_block_reward) return R.id.tv_stat_value_6;
		if (rowId == R.id.row_halving) return R.id.tv_stat_value_7;
		if (rowId == R.id.row_network) return R.id.tv_stat_value_8;
		return 0;
	}
	
	public void _getBallance(final String _wallet, final TextView _tv_balance) {
		if (_wallet == null || _wallet.trim().isEmpty()) return;
		com.harvanna.wallet.sync.AjooNodeClient.getWallet(_wallet, new com.harvanna.wallet.sync.AjooNodeClient.JsonCallback() {
			@Override
			public void onSuccess(org.json.JSONObject json) {
				runOnUiThread(() -> {
					long rawBalance = json.optLong("balance", 0L);
					double ajcBalance = com.harvanna.wallet.sync.AjooNodeClient.toAjcDisplay(rawBalance);
					String formattedBalance = com.harvanna.wallet.wallet.BalanceFormatter.short4(ajcBalance);
					_tv_balance.setText(formattedBalance);
				});
			}
			@Override
			public void onError(String errorMessage) {
				runOnUiThread(() -> {
					Toast.makeText(CoinDetailActivity.this, "Balance error: " + errorMessage, Toast.LENGTH_SHORT).show();
				});
			}
		});
	}
	
	private final BroadcastReceiver globalTxReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent != null && "com.harvanna.wallet.ACTION_WALLET_RECEIVED".equals(intent.getAction())) {
				double amount = intent.getDoubleExtra("amount", 0.0);
				String sender = intent.getStringExtra("sender");
				com.harvanna.wallet.utils.InAppNotificationHelper.showTransactionNotification(
				CoinDetailActivity.this,
				"Incoming Transfer", 
				"Received " + String.format(Locale.US, "%.5f", amount) + " AJC",
				sender
				);
			}
		}
	};
	
	{
	}
	
	public void _loadAvailableCoins() {
		if (wallet_address == null || wallet_address.trim().isEmpty()) {
			return;
		}
		final Context context = this;
		java.util.List<java.util.HashMap<String, Object>> cachedCoins =
		com.harvanna.wallet.wallet.WalletCache.loadList(context, "coins_" + wallet_address);
		if (cachedCoins != null) {
			updateCoinListOnUiThread(new ArrayList<>(cachedCoins));
		}
		SharedPreferences currencySp = getSharedPreferences("currencysp", Context.MODE_PRIVATE);
		final String currencySymbol = currencySp.getString("currency", "\u20a6");
		final String currencyCode = com.harvanna.wallet.wallet.FiatValueHelper.symbolToCurrencyCode(currencySymbol);
		com.harvanna.wallet.sync.AjooNodeClient.getWallet(wallet_address,
		new com.harvanna.wallet.sync.AjooNodeClient.JsonCallback() {
			@Override
			public void onSuccess(org.json.JSONObject json) {
				long   rawBalance = json.optLong("balance", 0L);
				double ajcBalance = com.harvanna.wallet.sync.AjooNodeClient.toAjcDisplay(rawBalance);
				
				com.harvanna.wallet.wallet.FiatValueHelper.convertAjcToFiat(ajcBalance, currencyCode, (fiatValue, rateAvailable) -> {
					final ArrayList<HashMap<String, Object>> loadedCoins = new ArrayList<>();
					HashMap<String, Object> nativeCoin = new HashMap<>();
					nativeCoin.put("coin_name",       "Ajoo Coin");
					nativeCoin.put("coin_symbol",      "AJC");
					nativeCoin.put("coin_balance",
					String.format(java.util.Locale.US, "%.8f", ajcBalance));
					// THE FIX: hardcoded "0.00", plus a pre-existing bug
					// independent of that — this was symbol-prefixed
					// ("₦0.00") while WalletAssetFragmentActivity reads
					// this exact same cached field expecting a raw
					// parseable number, applying the symbol itself at
					// render time. That mismatch meant Double.parseDouble()
					// was silently failing on every load, always falling
					// into that screen's own "₦0.00" catch-block fallback
					// rather than ever successfully parsing anything.
					nativeCoin.put("coin_value", rateAvailable
					? String.format(java.util.Locale.US, "%.2f", fiatValue)
					: "0");
					nativeCoin.put("coin_logo",       "");
					nativeCoin.put("contractAddress", "");
					nativeCoin.put("isNative",        true);
					nativeCoin.put("isListed",        true);
					nativeCoin.put("isVerified",      true);
					loadedCoins.add(nativeCoin);
					com.harvanna.wallet.wallet.WalletCache.saveList(
					context, "coins_" + wallet_address, loadedCoins);
					runOnUiThread(() -> updateCoinListOnUiThread(loadedCoins));
				});
			}
			@Override
			public void onError(String error) {
				android.util.Log.w("loadAvailableCoins", "Node fetch failed: " + error);
			}
		});
	}
	
	public void _receiveBottomSheet() {
		final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(CoinDetailActivity.this);
		View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_receive, null);
		bottomSheetDialog.setContentView(sheetView);
		
		bottomSheetDialog.setOnShowListener(dialog -> {
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
		});
		
		final Button btnShare = sheetView.findViewById(R.id.btn_share);
		final Spinner spinnerAssets = sheetView.findViewById(R.id.spinner_assets);
		final TextView tvDepositAddress = sheetView.findViewById(R.id.tv_deposit_address);
		final ImageView ivQrCode = sheetView.findViewById(R.id.iv_qr_code);
		final ImageView ivCopyAddress = sheetView.findViewById(R.id.iv_copy_address);
		final TextView tvClose = sheetView.findViewById(R.id.tv_close_sheet);
		
		final String activeAddress = WalletManager.getActiveWalletAddress(this);
		final boolean validAddress = com.harvanna.wallet.tx.NodeSendValidator.isValidAddress(activeAddress);
		
		if (tvDepositAddress != null) {
			tvDepositAddress.setText(validAddress ? activeAddress : "Create or load a wallet to receive assets");
		}
		
		if (ivQrCode != null) {
			if (validAddress) {
				ivQrCode.setImageDrawable(null);
				new Thread(() -> {
					Bitmap qr = null;
					try {
						qr = AJCQRCodeGenerator.generateQRCode(activeAddress, 512);
					} catch (Exception ignored) {}
					final Bitmap finalQr = qr;
					runOnUiThread(() -> {
						if (!isFinishing() && !isDestroyed() && ivQrCode != null && finalQr != null) {
							ivQrCode.setImageBitmap(finalQr);
						}
					});
				}).start();
			} else {
				ivQrCode.setImageDrawable(null);
			}
		}
		
		final List<String> assetList = new ArrayList<>();
		assetList.add("Ajoocoin (AJC)");
		
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, assetList) {
			@NonNull
			@Override
			public View getView(int position, View convertView, @NonNull ViewGroup parent) {
				TextView tv = (TextView) super.getView(position, convertView, parent);
				tv.setTextColor(Color.WHITE);
				tv.setTextSize(16);
				tv.setPadding(24, 16, 24, 16);
				return tv;
			}
			@Override
			public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
				TextView tv = (TextView) super.getDropDownView(position, convertView, parent);
				tv.setTextColor(Color.WHITE);
				tv.setTextSize(16);
				tv.setPadding(32, 28, 32, 28);
				tv.setBackgroundColor(UiColors.surface());
				return tv;
			}
		};
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		if (spinnerAssets != null) spinnerAssets.setAdapter(adapter);
		
		View.OnClickListener copyListener = v -> {
			if (!validAddress) {
				SketchwareUtil.showMessage(CoinDetailActivity.this, "No valid wallet address to copy.");
				return;
			}
			android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
			if (clipboard != null) {
				ClipData clip = ClipData.newPlainText("Ajoo Wallet Address", activeAddress);
				clipboard.setPrimaryClip(clip);
				SketchwareUtil.showMessage(CoinDetailActivity.this, "Address copied!");
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
						Bitmap bitmap = getBitmapFromView(ivQrCode);
						Uri imageUri = saveImageToCache(bitmap);
						if (imageUri != null) {
							_shareImage(imageUri, activeAddress);
						}
					} catch (Exception e) {
						SketchwareUtil.showMessage(CoinDetailActivity.this, e.toString());
					}
				});
			}
		}
		
		if (tvClose != null) tvClose.setOnClickListener(v -> bottomSheetDialog.dismiss());
		bottomSheetDialog.show();
	}
	
	private void _showTransferSuccessSheet(String amountText, String recipientAddress, String txHash) {
		final com.google.android.material.bottomsheet.BottomSheetDialog dialog =
		new com.google.android.material.bottomsheet.BottomSheetDialog(CoinDetailActivity.this);
		
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
		
		if (btnDone != null) {
			btnDone.setOnClickListener(v -> dialog.dismiss());
		}
		
		dialog.show();
	}
	
	public void updateCoinListOnUiThread(final java.util.ArrayList<java.util.HashMap<String, Object>> loadedCoins) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
			}
		});
	}
	
	public String getFirestoreString(org.json.JSONObject fields, String key) {
		try {
			if (fields != null && fields.has(key)) {
				org.json.JSONObject valObj = fields.getJSONObject(key);
				if (valObj.has("stringValue")) {
					return valObj.getString("stringValue");
				}
			}
		} catch (Exception e) {}
		return "";
	}
	
	public boolean getFirestoreBoolean(org.json.JSONObject fields, String key) {
		try {
			if (fields != null && fields.has(key)) {
				org.json.JSONObject valObj = fields.getJSONObject(key);
				if (valObj.has("booleanValue")) {
					return valObj.getBoolean("booleanValue");
				}
			}
		} catch (Exception e) {}
		return false;
	}
	
	public String getTokenIcon(org.json.JSONObject fields) {
		return getFirestoreString(fields, "logoUrl");
	}
	
	public double parseBalance(Object value) {
		if (value instanceof Number) {
			return ((Number) value).doubleValue();
		}
		try {
			return Double.parseDouble(String.valueOf(value));
		} catch (Exception e) {
			return 0.0;
		}
	}
	
	private void hideSystemKeyboard(View view) {
		android.view.inputmethod.InputMethodManager imm =
		(android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
		if (imm != null && view != null) {
			imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
		}
	}
	
	public void _shareImage(final Uri _imageUri, final String _text) {
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setType("image/png");
		intent.putExtra(Intent.EXTRA_STREAM, _imageUri);
		intent.putExtra(Intent.EXTRA_TEXT, _text);
		intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		startActivity(Intent.createChooser(intent, "Share Layout Via"));
	}
	
	private Bitmap getBitmapFromView(View view) {
		Bitmap returnedBitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(returnedBitmap);
		Drawable bgDrawable = view.getBackground();
		if (bgDrawable != null) {
			bgDrawable.draw(canvas);
		} else {
			canvas.drawColor(android.graphics.Color.WHITE);
		}
		view.draw(canvas);
		return returnedBitmap;
	}
	
	private Uri saveImageToCache(Bitmap bitmap) {
		File imagesFolder = new File(getCacheDir(), "images");
		Uri uri = null;
		try {
			imagesFolder.mkdirs();
			File file = new File(imagesFolder, "shared_layout.png");
			FileOutputStream stream = new FileOutputStream(file);
			bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream);
			stream.flush();
			stream.close();
			uri = androidx.core.content.FileProvider.getUriForFile(this,getPackageName() + ".fileprovider", file);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return uri;
	}
	
	public void _checkPermissionAndShowBottomSheet() {
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
			ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 100);
		} else {
			_showScannerBottomSheet();
		}
	}
	
	public void _showScannerBottomSheet() {
		final com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog = 
		new com.google.android.material.bottomsheet.BottomSheetDialog(this);
		
		View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_scanner, null);
		bottomSheetDialog.setContentView(bottomSheetView);
		
		bottomSheetDialog.setOnShowListener(dialog -> {
			com.google.android.material.bottomsheet.BottomSheetDialog d = 
			(com.google.android.material.bottomsheet.BottomSheetDialog) dialog;
			
			android.widget.FrameLayout bottomSheetInternal = 
			d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
			
			if (bottomSheetInternal != null) {
				bottomSheetInternal.setBackgroundColor(android.graphics.Color.TRANSPARENT);
				float radius = 24f * getResources().getDisplayMetrics().density;
				android.graphics.drawable.GradientDrawable background = new android.graphics.drawable.GradientDrawable();
				background.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
				background.setColor(UiColors.surface());
				background.setCornerRadii(new float[]{radius, radius, radius, radius, 0f, 0f, 0f, 0f});
				bottomSheetView.setBackground(background);
				com.google.android.material.bottomsheet.BottomSheetBehavior<android.widget.FrameLayout> behavior = 
				com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheetInternal);
				behavior.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
				behavior.setSkipCollapsed(true);
				behavior.setFitToContents(false);
				behavior.setDraggable(true);
				android.view.ViewGroup.LayoutParams lp = bottomSheetInternal.getLayoutParams();
				lp.height = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
				bottomSheetInternal.setLayoutParams(lp);
			}
			if (mCodeScanner != null) mCodeScanner.startPreview();
		});
		
		android.widget.TextView tvTitle = (android.widget.TextView) ((android.widget.LinearLayout) bottomSheetView).getChildAt(0);
		tvTitle.setTextColor(android.graphics.Color.WHITE);
		tvTitle.setPadding(0, 50, 0, 50); 
		
		com.budiyev.android.codescanner.CodeScannerView scannerComponent = bottomSheetView.findViewById(R.id.scanner_view);
		android.widget.LinearLayout.LayoutParams scannerLp = new android.widget.LinearLayout.LayoutParams(
		android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 
		android.widget.LinearLayout.LayoutParams.MATCH_PARENT
		);
		scannerLp.weight = 1;
		scannerComponent.setLayoutParams(scannerLp);
		
		mCodeScanner = new com.budiyev.android.codescanner.CodeScanner(this, scannerComponent);
		mCodeScanner.setDecodeCallback(new com.budiyev.android.codescanner.DecodeCallback() {
			@Override
			public void onDecoded(@androidx.annotation.NonNull final com.google.zxing.Result result) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (mScannerTarget != null) {
							mScannerTarget.setText(result.getText().trim());
						}
						bottomSheetDialog.dismiss();
					}
				});
			}
		});
		
		bottomSheetDialog.setOnDismissListener(dialog -> {
			if (mCodeScanner != null) mCodeScanner.releaseResources();
		});
		
		bottomSheetDialog.show();
	}
	
	{
	}
	
}
