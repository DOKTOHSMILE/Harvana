package com.harvana.app;

import android.Manifest;
import android.animation.*;
import android.app.*;
import android.app.Activity;
import android.content.*;
import android.content.Intent;
import android.content.SharedPreferences;
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
import androidx.biometric.*;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.slidingpanelayout.*;
import androidx.swiperefreshlayout.*;
import androidx.viewpager2.widget.ViewPager2;
import com.harvana.app.wallet.SecureWalletStorage;
import com.harvana.app.wallet.WalletListManager;
import com.harvana.app.wallet.WalletManager;
import com.budiyev.android.codescanner.*;
import com.budiyev.android.codescanner.CodeScanner;
import com.budiyev.android.codescanner.CodeScannerView;
import com.budiyev.android.codescanner.DecodeCallback;
import com.bumptech.glide.*;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.card.*;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.*;
import com.google.zxing.Result;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;
import org.json.*;

public class AssetFragmentActivity extends Fragment {
	
	private AppBarLayout app_bar_layout2;
	private MaterialCardView cardview16;
	private CollapsingToolbarLayout collapsingtoolbar2;
	private TabLayout tablayout1;
	private LinearLayout linear2;
	private MaterialCardView cardview_switch_wallet;
	private LinearLayout linear7;
	private LinearLayout linear3;
	private LinearLayout linear_switch_wallet;
	private ImageView imageview6;
	private TextView tv_active_wallet_label;
	private ImageView imageview3;
	private TextView textview10;
	private LinearLayout linear54;
	private TextView tv_fiat_value;
	private double harvFiat;
	private double chainFiat;
	private String shownSymbol;
	private ImageView imageview4;
	private MaterialCardView cardview1;
	private MaterialCardView cardview2;
	private MaterialCardView cardview3;
	private LinearLayout linear4;
	private ImageView imageview1;
	private TextView textview4;
	private LinearLayout linear5;
	private ImageView imageview2;
	private TextView textview5;
	private LinearLayout linear6;
	private ImageView imageview5;
	private TextView textview6;
	private ViewPager2 viewpager1;
	
	private SharedPreferences walletsp;
	private FirebaseAuth fbauth;
	private OnCompleteListener<AuthResult> _fbauth_create_user_listener;
	private OnCompleteListener<AuthResult> _fbauth_sign_in_listener;
	private OnCompleteListener<Void> _fbauth_reset_password_listener;
	private OnCompleteListener<Void> fbauth_updateEmailListener;
	private OnCompleteListener<Void> fbauth_updatePasswordListener;
	private OnCompleteListener<Void> fbauth_emailVerificationSentListener;
	private OnCompleteListener<Void> fbauth_deleteUserListener;
	private OnCompleteListener<Void> fbauth_updateProfileListener;
	private OnCompleteListener<AuthResult> fbauth_phoneAuthListener;
	private OnCompleteListener<AuthResult> fbauth_googleSignInListener;
	private Intent i = new Intent();
	
	@NonNull
	@Override
	public View onCreateView(@NonNull LayoutInflater _inflater, @Nullable ViewGroup _container, @Nullable Bundle _savedInstanceState) {
		View _view = _inflater.inflate(R.layout.asset_fragment, _container, false);
		initialize(_savedInstanceState, _view);
		FirebaseApp.initializeApp(getContext());
		initializeLogic();
		return _view;
	}
	
	private void initialize(Bundle _savedInstanceState, View _view) {
		app_bar_layout2 = _view.findViewById(R.id.app_bar_layout2);
		cardview16 = _view.findViewById(R.id.cardview16);
		collapsingtoolbar2 = _view.findViewById(R.id.collapsingtoolbar2);
		tablayout1 = _view.findViewById(R.id.tablayout1);
		linear2 = _view.findViewById(R.id.linear2);
		cardview_switch_wallet = _view.findViewById(R.id.cardview_switch_wallet);
		linear7 = _view.findViewById(R.id.linear7);
		linear3 = _view.findViewById(R.id.linear3);
		linear_switch_wallet = _view.findViewById(R.id.linear_switch_wallet);
		imageview6 = _view.findViewById(R.id.imageview6);
		tv_active_wallet_label = _view.findViewById(R.id.tv_active_wallet_label);
		imageview3 = _view.findViewById(R.id.imageview3);
		textview10 = _view.findViewById(R.id.textview10);
		linear54 = _view.findViewById(R.id.linear54);
		tv_fiat_value = _view.findViewById(R.id.tv_fiat_value);
		imageview4 = _view.findViewById(R.id.imageview4);
		com.harvana.app.utils.BalancePrivacy.bindEye(imageview3);
		com.harvana.app.utils.BalancePrivacy.bindEye(imageview4);
		com.harvana.app.utils.BalancePrivacy.guard(tv_fiat_value);
		if (tv_fiat_value != null) tv_fiat_value.setOnClickListener(v -> _pickCurrency());
		if (textview10 != null) textview10.setOnClickListener(v -> _pickCurrency());
		cardview1 = _view.findViewById(R.id.cardview1);
		cardview2 = _view.findViewById(R.id.cardview2);
		cardview3 = _view.findViewById(R.id.cardview3);
		linear4 = _view.findViewById(R.id.linear4);
		imageview1 = _view.findViewById(R.id.imageview1);
		textview4 = _view.findViewById(R.id.textview4);
		linear5 = _view.findViewById(R.id.linear5);
		imageview2 = _view.findViewById(R.id.imageview2);
		textview5 = _view.findViewById(R.id.textview5);
		linear6 = _view.findViewById(R.id.linear6);
		imageview5 = _view.findViewById(R.id.imageview5);
		textview6 = _view.findViewById(R.id.textview6);
		viewpager1 = _view.findViewById(R.id.viewpager1);
		walletsp = getContext().getSharedPreferences("walletsp", Activity.MODE_PRIVATE);
		fbauth = FirebaseAuth.getInstance();
		
		cardview_switch_wallet.setOnClickListener(_v -> {
			i.setClass(getContext().getApplicationContext(), WalletListActivity.class);
			startActivity(i);
			
		});
		
		cardview1.setOnClickListener(_v -> {
			i.setClass(getContext().getApplicationContext(), ReceiveActivity.class);
			startActivity(i);
		});
		
		cardview2.setOnClickListener(_v -> {
			i.setClass(getContext().getApplicationContext(), SendAssetActivity.class);
			startActivityForResult(i, SEND_ASSET_REQUEST_CODE);
		});
		
		cardview3.setOnClickListener(_v -> {
			i.setClass(getContext().getApplicationContext(), P2pMarketplaceActivity.class);
			startActivity(i);
			
		});
		
		fbauth_updateEmailListener = _param1 -> {
			final boolean _success = _param1.isSuccessful();
			final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
			
		};
		
		fbauth_updatePasswordListener = _param1 -> {
			final boolean _success = _param1.isSuccessful();
			final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
			
		};
		
		fbauth_emailVerificationSentListener = _param1 -> {
			final boolean _success = _param1.isSuccessful();
			final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
			
		};
		
		fbauth_deleteUserListener = _param1 -> {
			final boolean _success = _param1.isSuccessful();
			final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
			
		};
		
		fbauth_phoneAuthListener = task -> {
			final boolean _success = task.isSuccessful();
			final String _errorMessage = task.getException() != null ? task.getException().getMessage() : "";
			
		};
		
		fbauth_updateProfileListener = _param1 -> {
			final boolean _success = _param1.isSuccessful();
			final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
			
		};
		
		fbauth_googleSignInListener = task -> {
			final boolean _success = task.isSuccessful();
			final String _errorMessage = task.getException() != null ? task.getException().getMessage() : "";
			
		};
		
		_fbauth_create_user_listener = _param1 -> {
			final boolean _success = _param1.isSuccessful();
			final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
			
		};
		
		_fbauth_sign_in_listener = _param1 -> {
			final boolean _success = _param1.isSuccessful();
			final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
			
		};
		
		_fbauth_reset_password_listener = _param1 -> {
			final boolean _success = _param1.isSuccessful();
			
		};
	}
	
	private void initializeLogic() {
		Context context = getContext();
		if (context == null) return;
		
		SharedPreferences prefs = getContext().getSharedPreferences("harv_wallet_prefs", Context.MODE_PRIVATE);
		
		// 1. Get active address (Fast Disk I/O)
		activeAddress = prefs.getString("active_wallet_address", "").trim();
		if (activeAddress.isEmpty()) {
			activeAddress = prefs.getString("wallet_address", "").trim();
		}
		
		// 2. Other non-sensitive metadata
		String network = prefs.getString("network", (com.harvana.app.CoinInfo.name() + " Testnet"));
		String nativeSymbol = prefs.getString("native_symbol", com.harvana.app.CoinInfo.ticker());
		boolean isWalletCreated = prefs.getBoolean("is_wallet_created", false);
		long createdTimestamp = prefs.getLong("wallet_created_timestamp", 0L);
		
		// 3. Load wallet list map (Fast JSON Parsing)
		String jsonWalletData = prefs.getString("wallet_listmap_data", "{}");
		if (jsonWalletData != null && !jsonWalletData.isEmpty() && !jsonWalletData.equals("[]")) {
			try {
				org.json.JSONObject o = new org.json.JSONObject(jsonWalletData);
				wallet_data_map = new HashMap<>();
				java.util.Iterator<String> keys = o.keys();
				while (keys.hasNext()) {
					String k = keys.next();
					Object v = o.get(k);
					wallet_data_map.put(k, v == org.json.JSONObject.NULL ? null : v);
				}
				
				if (Boolean.TRUE.equals(wallet_data_map.get("is_active"))) {
					if (wallet_data_map.get("wallet_address") != null) {
						activeAddress = wallet_data_map.get("wallet_address").toString().trim();
					}
					if (wallet_data_map.get("native_symbol") != null) {
						native_symbol = wallet_data_map.get("native_symbol").toString().trim();
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				wallet_data_map = new HashMap<>();
			}
		}
		
		// 4. Instantly set up the ViewPager so the UI doesn't freeze
		_setupViewPager();
		com.harvana.app.utils.IncomingTxWatcher.start(getContext().getApplicationContext());
		
		// 5. PUSH HEAVY CRYPTO MATH TO A BACKGROUND THREAD
		new Thread(new Runnable() {
			@Override
			public void run() {
				// Only do the heavy derivation if the address is STILL empty after checking SharedPreferences
				if (activeAddress.isEmpty()) {
					String seedPhrase = SecureWalletStorage.getActiveMnemonic(getActivity());
					if (seedPhrase != null && !seedPhrase.isEmpty()) {
						try {
							// THIS is the line that was freezing your UI!
							activeAddress = WalletManager.deriveAddressFromSeed(seedPhrase).trim();
						} catch (Exception e) {
							activeAddress = "";
						}
					}
				}
				
				// 6. Return to the Main UI Thread to update the variables and load the coins
				if (getActivity() != null) {
					getActivity().runOnUiThread(new Runnable() {
						@Override
						public void run() {
							wallet_address = activeAddress;
							_loadAvailableCoins();
						}
					});
				}
			}
		}).start();
		
		
	}
	
	
	@Override
	public void onResume() {
		super.onResume();
		_updateActiveWalletLabel();
		if (getContext() != null) {
			String symbol = com.harvana.app.wallet.FxRates.symbol(getContext());
			if (shownSymbol != null && !shownSymbol.equals(symbol)) {
				harvFiat = 0;
				chainFiat = 0;
			}
			com.harvana.app.wallet.FxRates.load(getContext(), this::_paintTotal);
		}
		_loadAvailableCoins();
	}
	
	@Override
	public void onStart() {
		super.onStart();
		androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(getContext()).registerReceiver(
		globalTxReceiver, 
		new android.content.IntentFilter("com.harvana.app.ACTION_WALLET_RECEIVED")
		);
		
		
	}
	
	@Override
	public void onStop() {
		super.onStop();
		try {
			androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(globalTxReceiver);
		} catch (Exception ignored) {}
		
		
	}
	public void _MORE() {
	}
	private String wallet_address = "";
	private String privateKeyString = "";
	private String activeAddress = "";
	private String native_symbol = "";
	private CodeScanner mCodeScanner;
	private EditText mScannerTarget;
	private HashMap<String, Object> wallet_data_map = new HashMap<>();
	private static final int SEND_ASSET_REQUEST_CODE = 2001;
	
	private ArrayList<HashMap<String, Object>> newWalletDataListMap = new ArrayList<>();
	
	private void _updateActiveWalletLabel() {
		if (tv_active_wallet_label == null || getContext() == null) return;
		tv_active_wallet_label.setText(WalletListManager.getActiveWalletLabel(getContext()));
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		// A Fragment's own onActivityResult - not the host Activity's -
		// since startActivityForResult was called on this Fragment
		// directly, the result routes back here.
		if (requestCode == SEND_ASSET_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
			_loadAvailableCoins();
			if (getContext() != null) {
				SketchwareUtil.showMessage(getContext().getApplicationContext(), "Transfer sent.");
			}
		}
	}
	
	public void _setupViewPager() {
		frag = new FragFragmentAdapter(this);  // Fragment host
		viewpager1.setAdapter(frag);
		tablayout1.setInlineLabel(true);
		tablayout1.setTabTextColors(0xFF9AA3B2, getResources().getColor(R.color.colorAccent));
		tablayout1.setTabRippleColor(new android.content.res.ColorStateList(
		new int[][]{new int[]{android.R.attr.state_pressed}}, 
		new int[]{requireContext().getResources().getColor(R.color.colorAccent)}
		));
		tablayout1.setSelectedTabIndicatorColor(getResources().getColor(R.color.colorAccent));
		tablayout1.setSelectedTabIndicatorHeight(5);
		
		new TabLayoutMediator(tablayout1, viewpager1, new TabLayoutMediator.TabConfigurationStrategy() {
			@Override
			public void onConfigureTab(@NonNull TabLayout.Tab tab, int position) {
				switch (position) {
					case 0:
					tab.setText("Assets");
					break;
					case 1:
					tab.setText("Activity");
					break;
					case 2:
					tab.setText("Earn");
					break;
				}
			}
		}).attach();
		
	}
	private FragFragmentAdapter frag;
	
	public class FragFragmentAdapter extends androidx.viewpager2.adapter.FragmentStateAdapter {
		
		public FragFragmentAdapter(@androidx.annotation.NonNull Fragment fragment) {
			super(fragment);
		}
		
		@androidx.annotation.NonNull
		@Override
		public androidx.fragment.app.Fragment createFragment(int position) {
			switch (position) {
				case 0:
				return new WalletAssetFragmentActivity();
				case 1:
				return new WalletActivityFragmentActivity();
				case 2:
				return new WalletEarnFragmentActivity();
				default:
				return new WalletAssetFragmentActivity();
			}
		}
		
		@Override
		public int getItemCount() {
			return 3;
		}
	}
	
	
	public void updateCoinListOnUiThread(final java.util.ArrayList<java.util.HashMap<String, Object>> loadedCoins) {
		if (getActivity() != null) getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (loadedCoins == null || loadedCoins.isEmpty()) return;
				
				java.util.HashMap<String, Object> nativeCoin = loadedCoins.get(0);
				Object fiatObj = nativeCoin.get("coin_value");
				Object savedCurrency = nativeCoin.get("coin_currency");
				if (fiatObj == null || tv_fiat_value == null) return;
				if (savedCurrency != null && getContext() != null
						&& !String.valueOf(savedCurrency).equals(com.harvana.app.wallet.FxRates.symbol(getContext()))) return;
				
				try {
					harvFiat = Double.parseDouble(fiatObj.toString());
				} catch (NumberFormatException e) {
					harvFiat = 0;
				}
				_paintTotal();
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
		(android.view.inputmethod.InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
		if (imm != null && view != null) {
			imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
		}
	}
	
	
	
	public void _loadAvailableCoins() {
		
		if (wallet_address == null || wallet_address.trim().isEmpty()) {
			return;
		}
		
		final Context context = requireContext();
		
		// Show cached coins instantly while the node fetch is in-flight
		java.util.List<java.util.HashMap<String, Object>> cachedCoins =
		com.harvana.app.wallet.WalletCache.loadList(context, "coins_" + wallet_address);
		if (cachedCoins != null && !cachedCoins.isEmpty()) {
			updateCoinListOnUiThread(new ArrayList<>(cachedCoins));
		}
		
		SharedPreferences currencySp = getContext().getSharedPreferences("currencysp", Context.MODE_PRIVATE);
		final String currencySymbol = currencySp.getString("currency", "₦");
		
		com.harvana.app.wallet.WalletNodeBalanceHelper.load(wallet_address,
		(bal, avail, pin, pout, pending, err) -> {
			
			if (err != null && !err.isEmpty()) {
				android.util.Log.w("loadAvailableCoins", "Node fetch failed: " + err);
				return; 
			}
			
			double harvBalance = avail >= 0 ? avail : bal;
			com.harvana.app.wallet.FiatValueHelper.valueOfHarv(context, harvBalance, currencySymbol, (fiatValue, rateAvailable) -> {
				final ArrayList<HashMap<String, Object>> loadedCoins = new ArrayList<>();
				
				HashMap<String, Object> nativeCoin = new HashMap<>();
				nativeCoin.put("coin_name",       com.harvana.app.CoinInfo.name());
				nativeCoin.put("coin_symbol",      com.harvana.app.CoinInfo.ticker());
				nativeCoin.put("coin_balance",
				String.format(java.util.Locale.US, "%.8f", harvBalance));
				nativeCoin.put("coin_value", rateAvailable
				? String.format(java.util.Locale.US, "%.2f", fiatValue)
				: "0");
				nativeCoin.put("coin_currency", currencySymbol);
				nativeCoin.put("coin_logo",       "");
				nativeCoin.put("contractAddress", "");
				nativeCoin.put("isNative",        true);
				nativeCoin.put("isListed",        true);
				nativeCoin.put("isVerified",      true);
				loadedCoins.add(nativeCoin);
				
				com.harvana.app.wallet.WalletCache.saveList(
				context, "coins_" + wallet_address, loadedCoins);
				
				if (getActivity() != null) getActivity().runOnUiThread(() -> updateCoinListOnUiThread(loadedCoins));
			});
		});
		
	}
	
	
	
	public void setChainFiat(double sum) {
		chainFiat = sum;
		if (getActivity() != null) getActivity().runOnUiThread(this::_paintTotal);
	}

	private void _paintTotal() {
		if (tv_fiat_value == null || getContext() == null) return;
		String symbol = com.harvana.app.wallet.FxRates.symbol(getContext());
		shownSymbol = symbol;
		tv_fiat_value.setText(com.harvana.app.wallet.FxRates.format(symbol, harvFiat + chainFiat));
		if (textview10 != null) {
			String code = com.harvana.app.wallet.FiatValueHelper.symbolToCurrencyCode(symbol);
			textview10.setText("TOTAL BALANCE · " + code);
		}
	}

	private void _pickCurrency() {
		if (getContext() == null) return;
		com.harvana.app.wallet.FxRates.load(getContext(), () -> {
			if (!isAdded()) return;
			String[][] currencies = {
				{"₦", "NGN", "NGN — Nigerian Naira"},
				{"$", "USD", "USD — US Dollar"},
				{"£", "GBP", "GBP — British Pound"},
				{"€", "EUR", "EUR — Euro"},
				{"GH₵", "GHS", "GHS — Ghanaian Cedi"},
				{"KSh", "KES", "KES — Kenyan Shilling"},
				{"R", "ZAR", "ZAR — South African Rand"},
			};
			String current = com.harvana.app.wallet.FxRates.symbol(getContext());
			String[] labels = new String[currencies.length];
			int selected = 0;
			for (int i = 0; i < currencies.length; i++) {
				String rate = com.harvana.app.wallet.FxRates.rateLine(getContext(), currencies[i][1]);
				labels[i] = rate.isEmpty() || "USD".equals(currencies[i][1]) ? currencies[i][2] : currencies[i][2] + "  ·  " + rate;
				if (currencies[i][0].equals(current)) selected = i;
			}
			new com.harvana.app.ui.SheetDialog.Builder(requireContext())
			.setTitle("Currency")
			.setSingleChoiceItems(labels, selected, (dialog, which) -> {
				com.harvana.app.wallet.FxRates.save(getContext(), currencies[which][0]);
				harvFiat = 0;
				chainFiat = 0;
				shownSymbol = currencies[which][0];
				_paintTotal();
				dialog.dismiss();
				_loadAvailableCoins();
				androidx.fragment.app.Fragment page = getChildFragmentManager().findFragmentByTag("f0");
				if (page instanceof WalletAssetFragmentActivity) ((WalletAssetFragmentActivity) page).repaintMoney();
			})
			.setNegativeButton("Cancel", null)
			.show();
		});
	}

	public void _inAppNotification() {
	}
	
	private final BroadcastReceiver globalTxReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent != null && "com.harvana.app.ACTION_WALLET_RECEIVED".equals(intent.getAction())) {
				double amount = intent.getDoubleExtra("amount", 0.0);
				String sender = intent.getStringExtra("sender");
				
				// Pass 4 arguments: Activity, Title, Message, Sender
				com.harvana.app.utils.InAppNotificationHelper.showTransactionNotification(
				requireActivity(), // Replace with your current Activity
				"Incoming Transfer", 
				"Received " + String.format(Locale.US, "%.5f", amount) + (" " + com.harvana.app.CoinInfo.ticker()),
				sender
				);
			}
		}
	};
	
	{
		
	}
	
	
	public void _showScannerBottomSheet() {
		
		final com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog = 
		new com.harvana.app.ui.AppSheet(requireContext());
		
		View bottomSheetView = getActivity().getLayoutInflater().inflate(R.layout.bottom_sheet_scanner, null);
		bottomSheetDialog.setContentView(bottomSheetView);
		
		// 1. FULL SCREEN + ROUNDED TOP CORNERS (Matching your Send menu)
		bottomSheetDialog.setOnShowListener(dialog -> {
			com.google.android.material.bottomsheet.BottomSheetDialog d = 
			(com.google.android.material.bottomsheet.BottomSheetDialog) dialog;
			
			android.widget.FrameLayout bottomSheetInternal = 
			d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
			
			if (bottomSheetInternal != null) {
				bottomSheetInternal.setBackgroundColor(android.graphics.Color.TRANSPARENT);
				
				// Scanner Bottom Sheet Background & Title (Adapted for Theme)
				float radius = 24f * getResources().getDisplayMetrics().density;
				android.graphics.drawable.GradientDrawable background = new android.graphics.drawable.GradientDrawable();
				background.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
				background.setColor(requireContext().getResources().getColor(R.color.colorSurface)); // Dynamic Background
				background.setCornerRadii(new float[]{radius, radius, radius, radius, 0f, 0f, 0f, 0f});
				bottomSheetView.setBackground(background);
				
				// FIX TITLE VISIBILITY
				android.widget.TextView tvTitle = (android.widget.TextView) ((android.widget.LinearLayout) bottomSheetView).getChildAt(0);
				tvTitle.setTextColor(requireContext().getResources().getColor(R.color.colorTextPrimary)); // Dynamic Text
				tvTitle.setPadding(0, 50, 0, 50); 
				
				
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
			
			// Start camera preview safely after the view is fully expanded
			if (mCodeScanner != null) mCodeScanner.startPreview();
		});
		
		// 2. FIX TITLE VISIBILITY (Change text to white for the dark background)
		android.widget.TextView tvTitle = (android.widget.TextView) ((android.widget.LinearLayout) bottomSheetView).getChildAt(0);
		tvTitle.setTextColor(com.harvana.app.utils.UiColors.textPrimary());
		tvTitle.setPadding(0, 50, 0, 50); 
		
		// 3. FORCE SCANNER TO FILL SCREEN 
		com.budiyev.android.codescanner.CodeScannerView scannerComponent = bottomSheetView.findViewById(R.id.scanner_view);
		android.widget.LinearLayout.LayoutParams scannerLp = new android.widget.LinearLayout.LayoutParams(
		android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 
		android.widget.LinearLayout.LayoutParams.MATCH_PARENT
		);
		scannerLp.weight = 1;
		scannerComponent.setLayoutParams(scannerLp);
		
		// 4. INITIALIZE SCANNER LOGIC
		mCodeScanner = new com.budiyev.android.codescanner.CodeScanner(requireActivity(), scannerComponent);
		mCodeScanner.setDecodeCallback(new com.budiyev.android.codescanner.DecodeCallback() {
			@Override
			public void onDecoded(@androidx.annotation.NonNull final com.google.zxing.Result result) {
				if (getActivity() != null) getActivity().runOnUiThread(new Runnable() {
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
	
	
	public void _checkPermissionAndShowBottomSheet() {
		
		if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
			// Replaced CAMERA_PERMISSION_CODE with 100 here
			ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.CAMERA}, 100);
		} else {
			_showScannerBottomSheet();
		}
		
	}
	
	{
	}
	
}
