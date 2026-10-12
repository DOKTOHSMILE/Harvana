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
import android.widget.EditText;
import android.widget.LinearLayout;
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
import com.harvana.app.firestore.P2PPaymentDetailsManager;
import com.harvana.app.firestore.P2PTradeStatsManager;
import com.harvana.app.firestore.UserProfileManager;
import com.harvana.app.sync.HarvanaNodeClient;
import com.harvana.app.tx.P2PTradeRouter;
import com.harvana.app.wallet.BalanceFormatter;
import com.harvana.app.wallet.PaymentMethod;
import com.harvana.app.wallet.PaymentMethodManager;
import com.harvana.app.wallet.PinPromptHelper;
import com.harvana.app.wallet.WalletManager;
import com.harvana.app.wallet.WalletNodeBridge;
import com.google.firebase.auth.FirebaseAuth;
import com.harvana.app.p2p.P2pIds;
import com.harvana.app.p2p.P2pMoney;
import com.harvana.app.p2p.ThemedDialogs;


public class P2pSellOrderActivity extends AppCompatActivity {
	
	/** What this listing trades: "HARV" or a stable asset (hUSD, hNGN, …). Set when it loads. */
	private String p2pAsset = "HARV";
	
	private Toolbar _toolbar;
	private AppBarLayout _app_bar;
	private CoordinatorLayout _coordinator;
	
	private LinearLayout sell_order_root;
	private MaterialCardView card_sell_available;
	private MaterialCardView card_sell_amount;
	private MaterialCardView card_sell_price;
	private MaterialCardView card_sell_payment_method;
	private MaterialCardView card_sell_preview;
	private MaterialCardView card_sell_conditions;
	private MaterialCardView card_sell_protection;
	private MaterialButton btn_create_sell_order;
	private TextView tv_sell_order_note;
	private LinearLayout linear193;
	private LinearLayout linear194;
	private TextView tv_sell_max_hint;
	private TextView textview111;
	private TextView tv_sell_available_harv;
	private LinearLayout linear195;
	private TextView textview112;
	private LinearLayout linear196;
	private TextView tv_sell_amount_error;
	private LinearLayout layout_sell_presets;
	private EditText et_sell_amount;
	private TextView textview113;
	private TextView tv_sell_preset_1;
	private TextView tv_sell_preset_2;
	private TextView tv_sell_preset_3;
	private TextView tv_sell_preset_max;
	private LinearLayout linear197;
	private TextView textview114;
	private LinearLayout linear198;
	private TextView tv_sell_price_hint;
	private TextView textview115;
	private EditText et_sell_price;
	private TextView textview116;
	private LinearLayout linear199;
	private TextView textview117;
	private TextView tv_sell_payment_method_description;
	private LinearLayout layout_sell_payment_account;
	private LinearLayout linear200;
	private TextView tv_sell_change_account;
	private TextView tv_sell_bank_name;
	private TextView tv_sell_account_preview;
	private LinearLayout linear201;
	private TextView textview118;
	private LinearLayout linear202;
	private LinearLayout linear203;
	private LinearLayout linear204;
	private LinearLayout row_sell_preview_fee;
	private View view205;
	private LinearLayout linear206;
	private TextView textview119;
	private TextView tv_sell_preview_harv;
	private TextView textview120;
	private TextView tv_sell_preview_price;
	private TextView textview121;
	private TextView tv_sell_preview_gross;
	private TextView textview122;
	private TextView tv_sell_preview_fee;
	private TextView textview123;
	private TextView tv_sell_preview_receive;
	private LinearLayout linear207;
	private TextView textview124;
	private TextView tv_sell_condition_limits;
	private TextView tv_sell_condition_window;
	private LinearLayout linear208;
	private TextView textview125;
	private LinearLayout linear209;
	private TextView textview126;
	private TextView tv_sell_protection_description;
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.p2p_sell_order);
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
		sell_order_root = findViewById(R.id.sell_order_root);
		card_sell_available = findViewById(R.id.card_sell_available);
		card_sell_amount = findViewById(R.id.card_sell_amount);
		card_sell_price = findViewById(R.id.card_sell_price);
		card_sell_payment_method = findViewById(R.id.card_sell_payment_method);
		card_sell_preview = findViewById(R.id.card_sell_preview);
		card_sell_conditions = findViewById(R.id.card_sell_conditions);
		card_sell_protection = findViewById(R.id.card_sell_protection);
		btn_create_sell_order = findViewById(R.id.btn_create_sell_order);
		tv_sell_order_note = findViewById(R.id.tv_sell_order_note);
		linear193 = findViewById(R.id.linear193);
		linear194 = findViewById(R.id.linear194);
		tv_sell_max_hint = findViewById(R.id.tv_sell_max_hint);
		textview111 = findViewById(R.id.textview111);
		tv_sell_available_harv = findViewById(R.id.tv_sell_available_harv);
		linear195 = findViewById(R.id.linear195);
		textview112 = findViewById(R.id.textview112);
		linear196 = findViewById(R.id.linear196);
		tv_sell_amount_error = findViewById(R.id.tv_sell_amount_error);
		layout_sell_presets = findViewById(R.id.layout_sell_presets);
		et_sell_amount = findViewById(R.id.et_sell_amount);
		textview113 = findViewById(R.id.textview113);
		tv_sell_preset_1 = findViewById(R.id.tv_sell_preset_1);
		tv_sell_preset_2 = findViewById(R.id.tv_sell_preset_2);
		tv_sell_preset_3 = findViewById(R.id.tv_sell_preset_3);
		tv_sell_preset_max = findViewById(R.id.tv_sell_preset_max);
		linear197 = findViewById(R.id.linear197);
		textview114 = findViewById(R.id.textview114);
		linear198 = findViewById(R.id.linear198);
		tv_sell_price_hint = findViewById(R.id.tv_sell_price_hint);
		textview115 = findViewById(R.id.textview115);
		et_sell_price = findViewById(R.id.et_sell_price);
		textview116 = findViewById(R.id.textview116);
		linear199 = findViewById(R.id.linear199);
		textview117 = findViewById(R.id.textview117);
		tv_sell_payment_method_description = findViewById(R.id.tv_sell_payment_method_description);
		layout_sell_payment_account = findViewById(R.id.layout_sell_payment_account);
		linear200 = findViewById(R.id.linear200);
		tv_sell_change_account = findViewById(R.id.tv_sell_change_account);
		tv_sell_bank_name = findViewById(R.id.tv_sell_bank_name);
		tv_sell_account_preview = findViewById(R.id.tv_sell_account_preview);
		linear201 = findViewById(R.id.linear201);
		textview118 = findViewById(R.id.textview118);
		linear202 = findViewById(R.id.linear202);
		linear203 = findViewById(R.id.linear203);
		linear204 = findViewById(R.id.linear204);
		row_sell_preview_fee = findViewById(R.id.row_sell_preview_fee);
		view205 = findViewById(R.id.view205);
		linear206 = findViewById(R.id.linear206);
		textview119 = findViewById(R.id.textview119);
		tv_sell_preview_harv = findViewById(R.id.tv_sell_preview_harv);
		textview120 = findViewById(R.id.textview120);
		tv_sell_preview_price = findViewById(R.id.tv_sell_preview_price);
		textview121 = findViewById(R.id.textview121);
		tv_sell_preview_gross = findViewById(R.id.tv_sell_preview_gross);
		textview122 = findViewById(R.id.textview122);
		tv_sell_preview_fee = findViewById(R.id.tv_sell_preview_fee);
		textview123 = findViewById(R.id.textview123);
		tv_sell_preview_receive = findViewById(R.id.tv_sell_preview_receive);
		linear207 = findViewById(R.id.linear207);
		textview124 = findViewById(R.id.textview124);
		tv_sell_condition_limits = findViewById(R.id.tv_sell_condition_limits);
		tv_sell_condition_window = findViewById(R.id.tv_sell_condition_window);
		linear208 = findViewById(R.id.linear208);
		textview125 = findViewById(R.id.textview125);
		linear209 = findViewById(R.id.linear209);
		textview126 = findViewById(R.id.textview126);
		tv_sell_protection_description = findViewById(R.id.tv_sell_protection_description);
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
		
		if (getSupportActionBar() != null) {
			getSupportActionBar().setTitle(("Sell " + com.harvana.app.p2p.P2pAsset.ticker(p2pAsset)));
			getSupportActionBar().setSubtitle("Set the amount and price you want to sell.");
		}
		
		listingId = getIntent().getStringExtra("listingId");
		myAddress = WalletManager.getActiveWalletAddress(this);
		if (getSupportActionBar() != null) {
			getSupportActionBar().setTitle("Sell Order");
			getSupportActionBar().setSubtitle(("Review and confirm — you'll be providing the " + com.harvana.app.p2p.P2pAsset.ticker(p2pAsset)));
		}
		
		// GROUND-UP FIX: price is locked from the buyer's listing, never
		// set here — the shell's original copy ("Set a price that buyers
		// will see on your listing") belonged to a listing-creation
		// screen, not this one, and directly contradicted the master
		// prompt's locked-price rule.
		et_sell_price.setEnabled(false);
		et_sell_price.setFocusable(false);
		tv_sell_price_hint.setText("This price is set by the buyer's offer and can't be changed here.");
		
		card_sell_preview.setVisibility(View.GONE);
		tv_sell_amount_error.setVisibility(View.GONE);
		btn_create_sell_order.setEnabled(false);
		btn_create_sell_order.setText(("Sell " + com.harvana.app.p2p.P2pAsset.ticker(p2pAsset)));
		tv_sell_protection_description.setText(("Your " + com.harvana.app.p2p.P2pAsset.ticker(p2pAsset) + " is held in escrow the moment this order is created — it only releases to the buyer once you confirm you've received their payment."));
		
		if (listingId == null || listingId.isEmpty()) {
			_showFatalError("No listing specified.");
			return;
		}
		
		myMethods = PaymentMethodManager.loadAll(this);
		layout_sell_payment_account.setOnClickListener(v -> _showPaymentMethodPicker());
		tv_sell_change_account.setOnClickListener(v -> _showPaymentMethodPicker());
		if (!myMethods.isEmpty()) {
			selectedMethod = myMethods.get(0);
			_refreshSelectedMethodLabel();
		}
		
		tv_sell_max_hint.setOnClickListener(v -> _setAmount(Math.min(walletBalanceHarv, remainingHarv)));
		tv_sell_preset_max.setOnClickListener(v -> _setAmount(Math.min(walletBalanceHarv, remainingHarv)));
		et_sell_amount.addTextChangedListener(new TextWatcher() {
			@Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
			@Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
			@Override public void afterTextChanged(Editable s) { _onAmountChanged(); }
		});
		btn_create_sell_order.setOnClickListener(v -> _confirmCreateOrder());
		
		_loadWalletBalance();
		_loadListing();
		
	}
	
	public void _MORE() {
	}
	private String listingId = "";
	private String buyerAddress = "";
	private String buyerUid = "";
	private String myAddress = "";
	private double unitPrice = -1;
	private String listingCurrency = "NGN";
	private double remainingHarv = 0;
	private double minOrderHarv = 0;
	private double walletBalanceHarv = 0;
	private PaymentMethod selectedMethod = null;
	private List<PaymentMethod> myMethods = new ArrayList<>();
	
	private long lastPreviewedAmountRaw = -1;
	private long lastPlatformFeeRaw = 0;
	private long lastNetworkFeeRaw = 0;
	private long lastNetAmountRaw = 0;
	private final Handler debounceHandler = new Handler(Looper.getMainLooper());
	private Runnable pendingFeeFetch;
	
	private void _showFatalError(String message) {
		sell_order_root.removeAllViews();
		TextView tv = new TextView(this);
		tv.setText(message);
		tv.setPadding(48, 96, 48, 48);
		sell_order_root.addView(tv);
	}
	
	private void _loadWalletBalance() {
		if (myAddress == null || myAddress.isEmpty()) return;
		// The listing's own coin: HARV, or the stable asset it asks for (called again once the listing has loaded).
		com.harvana.app.p2p.P2pAsset.balance(this, p2pAsset, myAddress, new com.harvana.app.p2p.P2pAsset.BalanceCb() {
			@Override
			public void ok(double balanceHarv) {
				walletBalanceHarv = balanceHarv;
				runOnUiThread(() -> {
					tv_sell_available_harv.setText(BalanceFormatter.smart(balanceHarv) + " " + com.harvana.app.p2p.P2pAsset.ticker(p2pAsset));
					_refreshPresets();
				});
			}
			@Override
			public void fail(String error) {
				runOnUiThread(() -> tv_sell_available_harv.setText("Unavailable"));
			}
		});
	}
	
	// ── Load the BUY listing + buyer identity ───────────────────────────
	
	private void _loadListing() {
		HarvanaNodeClient.getP2PListing(listingId, new HarvanaNodeClient.JsonCallback() {
			@Override
			public void onSuccess(JSONObject listing) {
				p2pAsset = com.harvana.app.p2p.P2pAsset.of(listing);   // HARV or a stable asset: its amounts and ticker
				if (com.harvana.app.p2p.P2pAsset.isStable(p2pAsset)) _loadWalletBalance();
				runOnUiThread(() -> { if (btn_create_sell_order != null) btn_create_sell_order.setText("Sell " + com.harvana.app.p2p.P2pAsset.ticker(p2pAsset)); });
				if (!"OPEN".equals(listing.optString("status", ""))) {
					runOnUiThread(() -> _showFatalError("This offer is no longer open."));
					return;
				}
				if (!"BUY".equals(listing.optString("side", ""))) {
					runOnUiThread(() -> _showFatalError("This isn't a buy listing — you can't sell into it here."));
					return;
				}
				buyerAddress = listing.optString("buyerAddress", "");
				if (myAddress != null && !myAddress.isEmpty() && myAddress.equals(buyerAddress)) {
					runOnUiThread(() -> {
						_showFatalError("You can't trade with yourself");
						btn_create_sell_order.setEnabled(false);
					});
					return;
				}
				remainingHarv = com.harvana.app.p2p.P2pAsset.fromRaw(p2pAsset, listing.optLong("remainingAmount", 0));
				minOrderHarv = com.harvana.app.p2p.P2pAsset.fromRaw(p2pAsset, listing.optLong("minOrderAmount", 0));
				// The price the node signed into the listing (kobo per HARV); the Firestore card may add or confirm it.
				long nodeKobo = listing.optLong("unitPricePerHarvRaw", 0L);
				if (nodeKobo > 0) unitPrice = nodeKobo / 100.0;
				
				runOnUiThread(() -> {
					if (unitPrice >= 0) et_sell_price.setText(String.format(Locale.US, "%,.2f", unitPrice));
					tv_sell_condition_limits.setText(String.format(Locale.US,
					("Min %.4f · Max %.4f " + com.harvana.app.p2p.P2pAsset.ticker(p2pAsset)), minOrderHarv, remainingHarv));
					tv_sell_condition_window.setText("You'll have 45 minutes to confirm receipt once the buyer marks payment sent.");
					_refreshPresets();
				});
				
				_loadPaymentDetails();
			}
			@Override
			public void onError(String error) {
				runOnUiThread(() -> _showFatalError("Couldn't load this offer: " + error));
			}
		});
	}
	
	private void _loadPaymentDetails() {
		P2PPaymentDetailsManager.getInstance(getApplicationContext()).get(listingId, new P2PPaymentDetailsManager.DocCallback() {
			@Override
			public void onSuccess(Map<String, Object> data) {
				Object priceObj = data.get("unitPrice");
				if (priceObj instanceof Number) unitPrice = ((Number) priceObj).doubleValue();   // else keep the node's price
				Object currencyObj = data.get("currency");
				if (currencyObj != null && !String.valueOf(currencyObj).trim().isEmpty()) {
					listingCurrency = String.valueOf(currencyObj).trim();
				}
				// On a BUY listing this field holds the POSTER's uid —
				// i.e. the buyer's — even though it's still called
				// sellerUid (a naming leftover from when this collection
				// was designed SELL-only). Real data, misleading name.
				Object uidObj = data.get("sellerUid");
				buyerUid = uidObj != null ? String.valueOf(uidObj) : "";
				
				runOnUiThread(() -> {
					et_sell_price.setText(unitPrice >= 0 ? String.format(Locale.US, "%,.2f", unitPrice) : "");
				});
				
				if (buyerUid.isEmpty()) return;
				UserProfileManager.getInstance(getApplicationContext()).getProfile(buyerUid, new UserProfileManager.DocCallback() {
					@Override
					public void onSuccess(Map<String, Object> profile) {
						Object first = profile.get("first_name");
						Object last = profile.get("last_name");
						String firstStr = first != null ? String.valueOf(first).trim() : "";
						String lastStr = last != null ? String.valueOf(last).trim() : "";
						String name = !firstStr.isEmpty()
						? (lastStr.isEmpty() ? firstStr : firstStr + " " + lastStr.substring(0, 1).toUpperCase(Locale.US) + ".")
						: "Trader";
						runOnUiThread(() -> { if (getSupportActionBar() != null) getSupportActionBar().setSubtitle("Fulfilling " + name + "'s buy request"); });
					}
					@Override
					public void onError(String error) { }
				});
			}
			@Override
			public void onError(String error) {
				// No offer card in Firestore (an older offer): keep the node's price if it had one.
				runOnUiThread(() -> et_sell_price.setText(unitPrice >= 0 ? String.format(Locale.US, "%,.2f", unitPrice) : ""));
			}
		});
	}
	
	// ── Payment method picker (seller's own saved accounts) ─────────────
	
	private void _showPaymentMethodPicker() {
		if (myMethods.isEmpty()) {
			ThemedDialogs.show(ThemedDialogs.alert(this)
			.setTitle("No payment methods saved")
			.setMessage("Add a bank or mobile money account in Settings before fulfilling a buy offer.")
			.setPositiveButton("OK", null));
			return;
		}
		String[] labels = new String[myMethods.size()];
		for (int i = 0; i < myMethods.size(); i++) labels[i] = myMethods.get(i).label + " (" + myMethods.get(i).displaySummary() + ")";
		ThemedDialogs.show(ThemedDialogs.alert(this)
		.setTitle("Receive payment into")
		.setItems(labels, (dialog, which) -> {
			selectedMethod = myMethods.get(which);
			_refreshSelectedMethodLabel();
		}));
	}
	
	private void _refreshSelectedMethodLabel() {
		if (selectedMethod == null) {
			tv_sell_bank_name.setText("Select payment account");
			tv_sell_account_preview.setText("No account selected");
			return;
		}
		tv_sell_bank_name.setText(selectedMethod.primaryField);
		tv_sell_account_preview.setText(selectedMethod.accountName + " · " + selectedMethod.accountIdentifier);
	}
	
	// ── Amount ───────────────────────────────────────────────────────────
	
	private void _refreshPresets() {
		double fillable = Math.min(walletBalanceHarv, remainingHarv);
		tv_sell_preset_1.setText(BalanceFormatter.smartDecimal(fillable * 0.25));
		tv_sell_preset_2.setText(BalanceFormatter.smartDecimal(fillable * 0.5));
		tv_sell_preset_3.setText(BalanceFormatter.smartDecimal(fillable * 0.75));
		tv_sell_preset_1.setOnClickListener(v -> _setAmount(fillable * 0.25));
		tv_sell_preset_2.setOnClickListener(v -> _setAmount(fillable * 0.5));
		tv_sell_preset_3.setOnClickListener(v -> _setAmount(fillable * 0.75));
	}
	
	private void _setAmount(double harv) {
		et_sell_amount.setText(BalanceFormatter.smartDecimal(Math.max(0, harv)));
		et_sell_amount.setSelection(et_sell_amount.getText().length());
	}
	
	private void _onAmountChanged() {
		double amount = BalanceFormatter.parseUserInput(et_sell_amount.getText().toString());
		if (pendingFeeFetch != null) debounceHandler.removeCallbacks(pendingFeeFetch);
		
		if (amount <= 0) {
			card_sell_preview.setVisibility(View.GONE);
			tv_sell_amount_error.setVisibility(View.GONE);
			btn_create_sell_order.setEnabled(false);
			return;
		}
		if (amount < minOrderHarv) {
			_showAmountError(String.format(Locale.US, ("Minimum order is %.4f " + com.harvana.app.p2p.P2pAsset.ticker(p2pAsset) + "."), minOrderHarv));
			return;
		}
		if (amount > remainingHarv) {
			_showAmountError(String.format(Locale.US, ("The buyer only wants %.4f " + com.harvana.app.p2p.P2pAsset.ticker(p2pAsset) + "."), remainingHarv));
			return;
		}
		if (amount > walletBalanceHarv) {
			_showAmountError(String.format(Locale.US, ("You only have %.4f " + com.harvana.app.p2p.P2pAsset.ticker(p2pAsset) + " available."), walletBalanceHarv));
			return;
		}
		if (myAddress == null || myAddress.isEmpty()) {
			_showAmountError("No wallet found on this device.");
			return;
		}
		
		tv_sell_amount_error.setVisibility(View.GONE);
		long amountRaw = com.harvana.app.p2p.P2pAsset.toRaw(p2pAsset, amount);
		pendingFeeFetch = () -> _fetchPreview(amountRaw);
		debounceHandler.postDelayed(pendingFeeFetch, 400);
	}
	
	private void _showAmountError(String msg) {
		tv_sell_amount_error.setText(msg);
		tv_sell_amount_error.setVisibility(View.VISIBLE);
		card_sell_preview.setVisibility(View.GONE);
		btn_create_sell_order.setEnabled(false);
	}
	
	private void _fetchPreview(long amountRaw) {
		HarvanaNodeClient.previewP2PClaim(listingId, myAddress, amountRaw, new HarvanaNodeClient.JsonCallback() {
			@Override
			public void onSuccess(JSONObject result) {
				long currentAmountRaw = com.harvana.app.p2p.P2pAsset.toRaw(p2pAsset, BalanceFormatter.parseUserInput(et_sell_amount.getText().toString()));
				if (currentAmountRaw != amountRaw) return;
				
				lastPreviewedAmountRaw = amountRaw;
				lastPlatformFeeRaw = result.optLong("platformFeeRaw", 0);
				lastNetworkFeeRaw = result.optLong("releaseGasRaw", 0);
				lastNetAmountRaw = result.optLong("netAmountToBuyer", 0);
				
				double amountHarv = com.harvana.app.p2p.P2pAsset.fromRaw(p2pAsset, amountRaw);
				double feeHarv = com.harvana.app.p2p.P2pAsset.fromRaw(p2pAsset, lastPlatformFeeRaw);
				double fiatTotal = unitPrice >= 0 ? amountHarv * unitPrice : -1;
				
				runOnUiThread(() -> {
					card_sell_preview.setVisibility(View.VISIBLE);
					tv_sell_preview_harv.setText(BalanceFormatter.smart(amountHarv));
					tv_sell_preview_price.setText(unitPrice >= 0 ? P2pMoney.fiat(unitPrice, listingCurrency) : "—");
					tv_sell_preview_gross.setText(fiatTotal >= 0 ? P2pMoney.fiat(fiatTotal, listingCurrency) : "—");
					tv_sell_preview_fee.setText(String.format(Locale.US, ("%s " + com.harvana.app.p2p.P2pAsset.ticker(p2pAsset)), BalanceFormatter.smartDecimal(feeHarv)));
					tv_sell_preview_receive.setText(fiatTotal >= 0 ? P2pMoney.fiat(fiatTotal, listingCurrency) : "—");
					btn_create_sell_order.setEnabled(selectedMethod != null);
					if (selectedMethod == null) {
						tv_sell_amount_error.setText("Select a payment account to receive into before continuing.");
						tv_sell_amount_error.setVisibility(View.VISIBLE);
					}
				});
			}
			@Override
			public void onError(String error) {
				runOnUiThread(() -> _showAmountError("Couldn't check this order with Harvana Protocol: " + error));
			}
		});
	}
	
	// ── Create the order ─────────────────────────────────────────────────
	
	private void _confirmCreateOrder() {
		if (selectedMethod == null) {
			_showPaymentMethodPicker();
			return;
		}
		double amount = BalanceFormatter.parseUserInput(et_sell_amount.getText().toString());
		long amountRaw = com.harvana.app.p2p.P2pAsset.toRaw(p2pAsset, amount);
		if (lastPreviewedAmountRaw != amountRaw) {
			_showAmountError("Still checking this order — try again in a moment.");
			return;
		}
		
		double amountHarv = com.harvana.app.p2p.P2pAsset.fromRaw(p2pAsset, amountRaw);
		double fiatTotal = unitPrice >= 0 ? amountHarv * unitPrice : -1;
		java.util.ArrayList<String[]> rows = new java.util.ArrayList<>();
		rows.add(new String[]{"You sell", BalanceFormatter.smart(amountHarv)});
		rows.add(new String[]{"You should receive", fiatTotal >= 0 ? P2pMoney.fiat(fiatTotal, listingCurrency) : "The listed price"});
		rows.add(new String[]{"Platform fee", BalanceFormatter.smart(com.harvana.app.p2p.P2pAsset.fromRaw(p2pAsset, lastPlatformFeeRaw))});
		rows.add(new String[]{"Paid into", selectedMethod == null ? "Your payment account" : String.valueOf(selectedMethod)});
		rows.add(new String[]{"tFare", "Set by the node when this order opens"});
		com.harvana.app.group.PaymentConfirmSheet.reviewThenAuthorize(this,
		"Open this sell", BalanceFormatter.smart(amountHarv), rows, () -> {
			btn_create_sell_order.setEnabled(false);
			btn_create_sell_order.setText("Creating order…");
			
			P2PTradeRouter.claimPayment(getApplicationContext(), listingId, myAddress, amountRaw, p2pAsset, new P2PTradeRouter.Callback() {
				@Override
				public void onSuccess(JSONObject result) {
					String newOrderId = P2pIds.orderId(result);
					// Tell the buyer who posted the offer, through the trade chat (their phone's chat check notifies them).
					com.harvana.app.p2p.P2pChatStore.notice(newOrderId, buyerUid, "New order: I am selling you "
							+ BalanceFormatter.smart(amountHarv) + " " + com.harvana.app.p2p.P2pAsset.ticker(p2pAsset)
							+ ". It is held in escrow for you. Pay into the account on the trade screen, then tap \"I have paid\".");
					String myUid = FirebaseAuth.getInstance().getCurrentUser() != null
					? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
					
					if (myUid == null || newOrderId.isEmpty()) {
						runOnUiThread(() -> {
							btn_create_sell_order.setEnabled(true);
							btn_create_sell_order.setText(("Sell " + com.harvana.app.p2p.P2pAsset.ticker(p2pAsset)));
							_showAmountError("Order created, but couldn't finish setup. Check My Trades.");
						});
						return;
					}
					
					P2PPaymentDetailsManager.getInstance(getApplicationContext())
					.saveOrderPaymentDetails(newOrderId, selectedMethod, myUid, buyerUid, new P2PPaymentDetailsManager.SimpleCallback() {
						@Override
						public void onSuccess() { _goToTrade(newOrderId); }
						@Override
						public void onError(String error) {
							// The order itself is real on-chain either way —
							// only the buyer's payment instructions failed
							// to save. Proceed, but the trade screen will
							// need to handle a missing payment-details doc
							// gracefully rather than assume it's always there.
							_goToTrade(newOrderId);
						}
					});
				}
				@Override
				public void onError(String error) {
					runOnUiThread(() -> {
						btn_create_sell_order.setEnabled(true);
						btn_create_sell_order.setText(("Sell " + com.harvana.app.p2p.P2pAsset.ticker(p2pAsset)));
						_showAmountError("Couldn't create this order — the offer may have just changed: " + error);
					});
				}
			});
		});
	}
	
	private void _goToTrade(String orderId) {
		runOnUiThread(() -> {
			Intent intent = new Intent(P2pSellOrderActivity.this, P2pSellTradeActivity.class);
			intent.putExtra("orderId", orderId);
			startActivity(intent);
			finish();
		});
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (pendingFeeFetch != null) debounceHandler.removeCallbacks(pendingFeeFetch);
	}
	
	{
	}
	
}
