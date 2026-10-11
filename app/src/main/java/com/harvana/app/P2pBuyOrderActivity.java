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
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.button.*;
import com.google.android.material.card.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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
import com.harvana.app.wallet.PinPromptHelper;
import com.harvana.app.wallet.WalletManager;
import com.harvana.app.p2p.P2pIds;
import com.harvana.app.p2p.P2pMoney;


public class P2pBuyOrderActivity extends AppCompatActivity {
	
	/** What this listing trades: "HARV" or a stable asset (hUSD, hNGN, …). Set when it loads. */
	private String p2pAsset = "HARV";
	
	private Toolbar _toolbar;
	private AppBarLayout _app_bar;
	private CoordinatorLayout _coordinator;
	
	private LinearLayout buy_order_container;
	private MaterialCardView seller_card;
	private MaterialCardView offer_card;
	private TextView amount_section_title;
	private TextView amount_section_hint;
	private MaterialCardView amount_input_card;
	private HorizontalScrollView quick_amount_scroll;
	private MaterialCardView order_calculation_card;
	private TextView validation_message;
	private MaterialButton create_order_button;
	private MaterialCardView escrow_card;
	private TextView security_note;
	private LinearLayout linear51;
	private LinearLayout seller_header;
	private View view53;
	private LinearLayout seller_stats;
	private FrameLayout seller_avatar_container;
	private LinearLayout seller_identity;
	private ImageView seller_avatar;
	private TextView seller_initial;
	private LinearLayout linear52;
	private TextView seller_level;
	private TextView seller_name;
	private ImageView seller_verified;
	private LinearLayout linear54;
	private LinearLayout linear55;
	private LinearLayout linear56;
	private TextView seller_trust_score;
	private TextView textview26;
	private TextView seller_groups;
	private TextView textview27;
	private TextView seller_completed_orders;
	private TextView textview28;
	private LinearLayout linear57;
	private TextView offer_title;
	private LinearLayout offer_price_row;
	private LinearLayout offer_available_row;
	private LinearLayout offer_limits_row;
	private LinearLayout offer_payment_row;
	private TextView textview29;
	private TextView offer_price;
	private TextView textview30;
	private TextView offer_available;
	private TextView textview31;
	private TextView offer_limits;
	private TextView textview32;
	private TextView offer_payment_methods;
	private LinearLayout linear58;
	private EditText amount_input;
	private TextView amount_currency;
	private LinearLayout quick_amounts;
	private MaterialButton quick_25;
	private MaterialButton quick_50;
	private MaterialButton quick_100;
	private MaterialButton quick_max;
	private LinearLayout order_calculation;
	private TextView calculation_title;
	private LinearLayout calculation_quantity_row;
	private LinearLayout calculation_price_row;
	private View view59;
	private LinearLayout calculation_subtotal_row;
	private LinearLayout calculation_maker_fee_row;
	private LinearLayout calculation_network_fee_row;
	private LinearLayout calculation_receive_row;
	private TextView textview33;
	private TextView calculation_harv;
	private TextView textview34;
	private TextView calculation_price;
	private TextView textview35;
	private TextView calculation_fiat;
	private TextView textview36;
	private TextView calculation_maker_fee;
	private TextView textview37;
	private TextView calculation_network_fee;
	private TextView textview38;
	private TextView calculation_receive;
	private LinearLayout linear60;
	private ImageView escrow_icon;
	private LinearLayout linear61;
	private TextView escrow_title;
	private TextView escrow_description;
	
	private FirebaseAuth fauth;
	private OnCompleteListener<AuthResult> _fauth_create_user_listener;
	private OnCompleteListener<AuthResult> _fauth_sign_in_listener;
	private OnCompleteListener<Void> _fauth_reset_password_listener;
	private OnCompleteListener<Void> fauth_updateEmailListener;
	private OnCompleteListener<Void> fauth_updatePasswordListener;
	private OnCompleteListener<Void> fauth_emailVerificationSentListener;
	private OnCompleteListener<Void> fauth_deleteUserListener;
	private OnCompleteListener<Void> fauth_updateProfileListener;
	private OnCompleteListener<AuthResult> fauth_phoneAuthListener;
	private OnCompleteListener<AuthResult> fauth_googleSignInListener;
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.p2p_buy_order);
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
		buy_order_container = findViewById(R.id.buy_order_container);
		seller_card = findViewById(R.id.seller_card);
		offer_card = findViewById(R.id.offer_card);
		amount_section_title = findViewById(R.id.amount_section_title);
		amount_section_hint = findViewById(R.id.amount_section_hint);
		amount_input_card = findViewById(R.id.amount_input_card);
		quick_amount_scroll = findViewById(R.id.quick_amount_scroll);
		order_calculation_card = findViewById(R.id.order_calculation_card);
		validation_message = findViewById(R.id.validation_message);
		create_order_button = findViewById(R.id.create_order_button);
		escrow_card = findViewById(R.id.escrow_card);
		security_note = findViewById(R.id.security_note);
		linear51 = findViewById(R.id.linear51);
		seller_header = findViewById(R.id.seller_header);
		view53 = findViewById(R.id.view53);
		seller_stats = findViewById(R.id.seller_stats);
		seller_avatar_container = findViewById(R.id.seller_avatar_container);
		seller_identity = findViewById(R.id.seller_identity);
		seller_avatar = findViewById(R.id.seller_avatar);
		seller_initial = findViewById(R.id.seller_initial);
		linear52 = findViewById(R.id.linear52);
		seller_level = findViewById(R.id.seller_level);
		seller_name = findViewById(R.id.seller_name);
		seller_verified = findViewById(R.id.seller_verified);
		linear54 = findViewById(R.id.linear54);
		linear55 = findViewById(R.id.linear55);
		linear56 = findViewById(R.id.linear56);
		seller_trust_score = findViewById(R.id.seller_trust_score);
		textview26 = findViewById(R.id.textview26);
		seller_groups = findViewById(R.id.seller_groups);
		textview27 = findViewById(R.id.textview27);
		seller_completed_orders = findViewById(R.id.seller_completed_orders);
		textview28 = findViewById(R.id.textview28);
		linear57 = findViewById(R.id.linear57);
		offer_title = findViewById(R.id.offer_title);
		offer_price_row = findViewById(R.id.offer_price_row);
		offer_available_row = findViewById(R.id.offer_available_row);
		offer_limits_row = findViewById(R.id.offer_limits_row);
		offer_payment_row = findViewById(R.id.offer_payment_row);
		textview29 = findViewById(R.id.textview29);
		offer_price = findViewById(R.id.offer_price);
		textview30 = findViewById(R.id.textview30);
		offer_available = findViewById(R.id.offer_available);
		textview31 = findViewById(R.id.textview31);
		offer_limits = findViewById(R.id.offer_limits);
		textview32 = findViewById(R.id.textview32);
		offer_payment_methods = findViewById(R.id.offer_payment_methods);
		linear58 = findViewById(R.id.linear58);
		amount_input = findViewById(R.id.amount_input);
		amount_currency = findViewById(R.id.amount_currency);
		quick_amounts = findViewById(R.id.quick_amounts);
		quick_25 = findViewById(R.id.quick_25);
		quick_50 = findViewById(R.id.quick_50);
		quick_100 = findViewById(R.id.quick_100);
		quick_max = findViewById(R.id.quick_max);
		order_calculation = findViewById(R.id.order_calculation);
		calculation_title = findViewById(R.id.calculation_title);
		calculation_quantity_row = findViewById(R.id.calculation_quantity_row);
		calculation_price_row = findViewById(R.id.calculation_price_row);
		view59 = findViewById(R.id.view59);
		calculation_subtotal_row = findViewById(R.id.calculation_subtotal_row);
		calculation_maker_fee_row = findViewById(R.id.calculation_maker_fee_row);
		calculation_network_fee_row = findViewById(R.id.calculation_network_fee_row);
		calculation_receive_row = findViewById(R.id.calculation_receive_row);
		textview33 = findViewById(R.id.textview33);
		calculation_harv = findViewById(R.id.calculation_harv);
		textview34 = findViewById(R.id.textview34);
		calculation_price = findViewById(R.id.calculation_price);
		textview35 = findViewById(R.id.textview35);
		calculation_fiat = findViewById(R.id.calculation_fiat);
		textview36 = findViewById(R.id.textview36);
		calculation_maker_fee = findViewById(R.id.calculation_maker_fee);
		textview37 = findViewById(R.id.textview37);
		calculation_network_fee = findViewById(R.id.calculation_network_fee);
		textview38 = findViewById(R.id.textview38);
		calculation_receive = findViewById(R.id.calculation_receive);
		linear60 = findViewById(R.id.linear60);
		escrow_icon = findViewById(R.id.escrow_icon);
		linear61 = findViewById(R.id.linear61);
		escrow_title = findViewById(R.id.escrow_title);
		escrow_description = findViewById(R.id.escrow_description);
		fauth = FirebaseAuth.getInstance();
		
		fauth_updateEmailListener = _param1 -> {
			final boolean _success = _param1.isSuccessful();
			final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
			
		};
		
		fauth_updatePasswordListener = _param1 -> {
			final boolean _success = _param1.isSuccessful();
			final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
			
		};
		
		fauth_emailVerificationSentListener = _param1 -> {
			final boolean _success = _param1.isSuccessful();
			final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
			
		};
		
		fauth_deleteUserListener = _param1 -> {
			final boolean _success = _param1.isSuccessful();
			final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
			
		};
		
		fauth_phoneAuthListener = task -> {
			final boolean _success = task.isSuccessful();
			final String _errorMessage = task.getException() != null ? task.getException().getMessage() : "";
			
		};
		
		fauth_updateProfileListener = _param1 -> {
			final boolean _success = _param1.isSuccessful();
			final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
			
		};
		
		fauth_googleSignInListener = task -> {
			final boolean _success = task.isSuccessful();
			final String _errorMessage = task.getException() != null ? task.getException().getMessage() : "";
			
		};
		
		_fauth_create_user_listener = _param1 -> {
			final boolean _success = _param1.isSuccessful();
			final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
			
		};
		
		_fauth_sign_in_listener = _param1 -> {
			final boolean _success = _param1.isSuccessful();
			final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
			
		};
		
		_fauth_reset_password_listener = _param1 -> {
			final boolean _success = _param1.isSuccessful();
			
		};
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
		
		listingId = getIntent().getStringExtra("listingId");
		myAddress = WalletManager.getActiveWalletAddress(this);
		if (getSupportActionBar() != null) {
			getSupportActionBar().setTitle("Buy Order");
			getSupportActionBar().setSubtitle("Review and confirm your order");
		}
		order_calculation_card.setVisibility(View.GONE);
		validation_message.setVisibility(View.GONE);
		create_order_button.setEnabled(false);
		create_order_button.setText(("Buy " + com.harvana.app.p2p.P2pAsset.ticker(p2pAsset)));
		
		com.harvana.app.utils.Shimmer.loading(seller_name);
		seller_initial.setVisibility(View.VISIBLE);
		seller_avatar.setVisibility(View.GONE);
		seller_verified.setVisibility(View.GONE);
		
		if (listingId == null || listingId.isEmpty()) {
			_showFatalError("No listing specified.");
			return;
		}
		
		amount_input.addTextChangedListener(new TextWatcher() {
			@Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
			@Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
			@Override public void afterTextChanged(Editable s) { _onAmountChanged(); }
		});
		quick_25.setOnClickListener(v -> _setAmount(remainingHarv * 0.25));
		quick_50.setOnClickListener(v -> _setAmount(remainingHarv * 0.50));
		quick_100.setOnClickListener(v -> _setAmount(remainingHarv * 0.75));
		quick_max.setOnClickListener(v -> _setAmount(remainingHarv));
		create_order_button.setOnClickListener(v -> _confirmCreateOrder());
		
		_loadListing();
		
	}
	
	public void _MORE() {
	}
	private String listingId = "";
	private String sellerAddress = "";
	private String myAddress = "";
	private double unitPrice = -1;
	/** The seller's account, for the trade chat and its notices. */
	private String sellerUidForChat = "";
	private String listingCurrency = "NGN";
	private double remainingHarv = 0;
	private double minOrderHarv = 0;
	private long lastPreviewedAmountRaw = -1;
	private long lastPlatformFeeRaw = 0;
	private long lastNetworkFeeRaw = 0;
	private long lastNetAmountRaw = 0;
	private final Handler debounceHandler = new Handler(Looper.getMainLooper());
	private Runnable pendingFeeFetch;
	
	private void _showFatalError(String message) {
		buy_order_container.removeAllViews();
		TextView tv = new TextView(this);
		tv.setText(message);
		tv.setPadding(48, 96, 48, 48);
		buy_order_container.addView(tv);
	}
	
	// ── Load listing + Firestore metadata + seller trust ────────────────
	
	private void _loadListing() {
		HarvanaNodeClient.getP2PListing(listingId, new HarvanaNodeClient.JsonCallback() {
			@Override
			public void onSuccess(JSONObject listing) {
				p2pAsset = com.harvana.app.p2p.P2pAsset.of(listing);   // HARV or a stable asset: its amounts and ticker
				runOnUiThread(() -> { if (create_order_button != null) create_order_button.setText("Buy " + com.harvana.app.p2p.P2pAsset.ticker(p2pAsset)); });
				if (!"OPEN".equals(listing.optString("status", ""))) {
					runOnUiThread(() -> _showFatalError("This listing is no longer open."));
					return;
				}
				if (!"SELL".equals(listing.optString("side", ""))) {
					runOnUiThread(() -> _showFatalError("This isn't a sell listing — you can't buy from it here."));
					return;
				}
				sellerAddress = listing.optString("sellerAddress", "");
				if (myAddress != null && !myAddress.isEmpty() && myAddress.equals(sellerAddress)) {
					runOnUiThread(() -> {
						_showFatalError("You can't trade with yourself");
						create_order_button.setEnabled(false);
					});
					return;
				}
				remainingHarv = com.harvana.app.p2p.P2pAsset.fromRaw(p2pAsset, listing.optLong("remainingAmount", 0));
				minOrderHarv = com.harvana.app.p2p.P2pAsset.fromRaw(p2pAsset, listing.optLong("minOrderAmount", 0));
				// The price the node signed into the listing (kobo per HARV); Firestore may add or confirm it.
				long nodeKobo = listing.optLong("unitPricePerHarvRaw", 0L);
				if (nodeKobo > 0) unitPrice = nodeKobo / 100.0;
				
				runOnUiThread(() -> {
					offer_available.setText(String.format(Locale.US, ("%.4f " + com.harvana.app.p2p.P2pAsset.ticker(p2pAsset)), remainingHarv));
					offer_limits.setText(String.format(Locale.US, ("Min %.4f · Max %.4f " + com.harvana.app.p2p.P2pAsset.ticker(p2pAsset)), minOrderHarv, remainingHarv));
					amount_section_hint.setText(String.format(Locale.US,
					("Available %.4f " + com.harvana.app.p2p.P2pAsset.ticker(p2pAsset) + " · Minimum %.4f " + com.harvana.app.p2p.P2pAsset.ticker(p2pAsset)), remainingHarv, minOrderHarv));
					quick_25.setText(BalanceFormatter.smartDecimal(remainingHarv * 0.25));
					quick_50.setText(BalanceFormatter.smartDecimal(remainingHarv * 0.50));
					quick_100.setText(BalanceFormatter.smartDecimal(remainingHarv * 0.75));
					// The quick-amount chips: readable text on their dark fill (they were the same colour as the chip).
					for (TextView q : new TextView[]{quick_25, quick_50, quick_100, quick_max}) if (q != null) q.setTextColor(com.harvana.app.utils.UiColors.textPrimary());
					if (offer_price != null && unitPrice > 0) offer_price.setText(P2pMoney.perUnit(unitPrice, listingCurrency, p2pAsset));
				});
				
				if (sellerAddress.isEmpty()) {
					runOnUiThread(() -> seller_name.setText("Trader"));
				}
				_loadPaymentDetails();
			}
			@Override
			public void onError(String error) {
				runOnUiThread(() -> _showFatalError("Couldn't load this listing: " + error));
			}
		});
	}
	
	private void _loadPaymentDetails() {
		P2PPaymentDetailsManager.getInstance(getApplicationContext()).get(listingId, new P2PPaymentDetailsManager.DocCallback() {
			@Override
			public void onSuccess(Map<String, Object> data) {
				Object priceObj = data.get("unitPrice");
				if (priceObj instanceof Number && ((Number) priceObj).doubleValue() > 0) unitPrice = ((Number) priceObj).doubleValue();
				Object currencyObj = data.get("currency");
				if (currencyObj != null && !String.valueOf(currencyObj).trim().isEmpty()) {
					listingCurrency = String.valueOf(currencyObj).trim();
				}
				Object sellerUidObj = data.get("sellerUid");
				String sellerUid = sellerUidObj != null ? String.valueOf(sellerUidObj) : "";
				sellerUidForChat = sellerUid;
				
				runOnUiThread(() -> {
					offer_price.setText(unitPrice >= 0 ? P2pMoney.perUnit(unitPrice, listingCurrency, p2pAsset) : "Unavailable");
					_bindPaymentMethodsText(data);
				});
				
				if (sellerUid.isEmpty()) {
					runOnUiThread(() -> seller_name.setText("Trader"));
					return;
				}
				
				UserProfileManager.getInstance(getApplicationContext()).getProfile(sellerUid, new UserProfileManager.DocCallback() {
					@Override
					public void onSuccess(Map<String, Object> profile) {
						Object first = profile.get("first_name");
						Object last = profile.get("last_name");
						String firstStr = first != null ? String.valueOf(first).trim() : "";
						String lastStr = last != null ? String.valueOf(last).trim() : "";
						String name = !firstStr.isEmpty()
						? (lastStr.isEmpty() ? firstStr : firstStr + " " + lastStr.substring(0, 1).toUpperCase(Locale.US) + ".")
						: "Trader";
						Object iconObj = profile.get("icon");
						String iconUrl = iconObj != null ? String.valueOf(iconObj) : "";
						
						runOnUiThread(() -> {
							seller_name.setText(name);
							if (iconUrl.startsWith("http")) {
								seller_initial.setVisibility(View.GONE);
								seller_avatar.setVisibility(View.VISIBLE);
								Glide.with(getApplicationContext()).load(Uri.parse(iconUrl)).circleCrop().into(seller_avatar);
							} else {
								seller_avatar.setVisibility(View.GONE);
								seller_initial.setVisibility(View.VISIBLE);
								seller_initial.setText(name.length() > 0 ? name.substring(0, 1).toUpperCase(Locale.US) : "?");
							}
						});
					}
					@Override
					public void onError(String error) {
						runOnUiThread(() -> seller_name.setText("Trader"));
					}
				});
				
				com.harvana.app.TrustScoreEngine.compute(getApplicationContext(), sellerUid, new com.harvana.app.TrustScoreEngine.Callback() {
					@Override
					public void onComplete(com.harvana.app.TrustScoreEngine.Result result) {
						runOnUiThread(() -> {
							seller_verified.setVisibility(result.verified ? View.VISIBLE : View.GONE);
							seller_groups.setText(String.valueOf(result.totalGroups));
						});
					}
					@Override
					public void onError(String error) { }
				});
				com.harvana.app.trust.NetworkTrust.wallet(sellerAddress, score -> runOnUiThread(() -> {
					seller_trust_score.setText(score.label());
					seller_level.setText(score.summary.isEmpty() ? "Not on the network" : score.summary);
				}));
				
				P2PTradeStatsManager.getInstance(getApplicationContext()).getStats(sellerUid, new P2PTradeStatsManager.StatsCallback() {
					@Override
					public void onSuccess(int totalTrades, int successfulTrades, double successRatePercent) {
						runOnUiThread(() -> seller_completed_orders.setText(String.valueOf(totalTrades)));
					}
					@Override
					public void onError(String error) { }
				});
			}
			@Override
			public void onError(String error) {
				runOnUiThread(() -> {
					offer_price.setText("Unavailable");
					seller_name.setText("Trader");
				});
			}
		});
	}
	
	private void _bindPaymentMethodsText(Map<String, Object> data) {
		Object methodsObj = data.get("paymentMethods");
		if (!(methodsObj instanceof List)) { offer_payment_methods.setText("—"); return; }
		List<?> methods = (List<?>) methodsObj;
		StringBuilder sb = new StringBuilder();
		for (Object m : methods) {
			if (!(m instanceof Map)) continue;
			Object primary = ((Map<?, ?>) m).get("primaryField");
			if (primary == null) continue;
			if (sb.length() > 0) sb.append(", ");
			sb.append(primary);
		}
		offer_payment_methods.setText(sb.length() > 0 ? sb.toString() : "—");
	}
	
	// ── Amount entry + live fee preview ──────────────────────────────────
	
	private void _setAmount(double harv) {
		amount_input.setText(BalanceFormatter.smartDecimal(Math.min(harv, remainingHarv)));
		amount_input.setSelection(amount_input.getText().length());
	}
	
	private void _onAmountChanged() {
		double amount = BalanceFormatter.parseUserInput(amount_input.getText().toString());
		if (pendingFeeFetch != null) debounceHandler.removeCallbacks(pendingFeeFetch);
		
		if (amount <= 0) {
			order_calculation_card.setVisibility(View.GONE);
			validation_message.setVisibility(View.GONE);
			create_order_button.setEnabled(false);
			return;
		}
		if (amount < minOrderHarv) {
			_showValidationError(String.format(Locale.US, ("Minimum order is %.4f " + com.harvana.app.p2p.P2pAsset.ticker(p2pAsset) + "."), minOrderHarv));
			return;
		}
		if (amount > remainingHarv) {
			_showValidationError(String.format(Locale.US, ("Only %.4f " + com.harvana.app.p2p.P2pAsset.ticker(p2pAsset) + " available."), remainingHarv));
			return;
		}
		if (myAddress == null || myAddress.isEmpty()) {
			_showValidationError("No wallet found on this device.");
			return;
		}
		
		validation_message.setVisibility(View.GONE);
		long amountRaw = com.harvana.app.p2p.P2pAsset.toRaw(p2pAsset, amount);
		pendingFeeFetch = () -> _fetchPreview(amountRaw);
		debounceHandler.postDelayed(pendingFeeFetch, 400);
	}
	
	private void _showValidationError(String msg) {
		validation_message.setText(msg);
		validation_message.setVisibility(View.VISIBLE);
		order_calculation_card.setVisibility(View.GONE);
		create_order_button.setEnabled(false);
	}
	
	private void _fetchPreview(long amountRaw) {
		HarvanaNodeClient.previewP2PClaim(listingId, myAddress, amountRaw, new HarvanaNodeClient.JsonCallback() {
			@Override
			public void onSuccess(JSONObject result) {
				long currentAmountRaw = com.harvana.app.p2p.P2pAsset.toRaw(p2pAsset, BalanceFormatter.parseUserInput(amount_input.getText().toString()));
				if (currentAmountRaw != amountRaw) return; // stale — amount changed while this was in flight
				
				lastPreviewedAmountRaw = amountRaw;
				lastPlatformFeeRaw = result.optLong("platformFeeRaw", 0);
				lastNetworkFeeRaw = result.optLong("releaseGasRaw", 0);
				lastNetAmountRaw = result.optLong("netAmountToBuyer", 0);
				
				double amountHarv = com.harvana.app.p2p.P2pAsset.fromRaw(p2pAsset, amountRaw);
				double feeHarv = com.harvana.app.p2p.P2pAsset.fromRaw(p2pAsset, lastPlatformFeeRaw);
				double networkFeeHarv = com.harvana.app.p2p.P2pAsset.fromRaw(p2pAsset, lastNetworkFeeRaw);
				double netHarv = com.harvana.app.p2p.P2pAsset.fromRaw(p2pAsset, lastNetAmountRaw);
				double fiatTotal = unitPrice >= 0 ? amountHarv * unitPrice : -1;
				
				runOnUiThread(() -> {
					order_calculation_card.setVisibility(View.VISIBLE);
					calculation_harv.setText(BalanceFormatter.smart(amountHarv));
					calculation_price.setText(unitPrice >= 0 ? P2pMoney.fiat(unitPrice, listingCurrency) : "—");
					calculation_fiat.setText(fiatTotal >= 0 ? P2pMoney.fiat(fiatTotal, listingCurrency) : "—");
					calculation_maker_fee.setText(String.format(Locale.US, ("-%s " + com.harvana.app.p2p.P2pAsset.ticker(p2pAsset)), BalanceFormatter.smartDecimal(feeHarv)));
					calculation_network_fee.setText(String.format(Locale.US, ("-%s " + com.harvana.app.p2p.P2pAsset.ticker(p2pAsset)), BalanceFormatter.smartDecimal(networkFeeHarv)));
					calculation_receive.setText(BalanceFormatter.smart(netHarv));
					create_order_button.setEnabled(true);
				});
			}
			@Override
			public void onError(String error) {
				runOnUiThread(() -> _showValidationError("Couldn't check this order with Harvana Protocol: " + error));
			}
		});
	}
	
	// ── Create the order (this is the actual claim against the listing) ─
	
	private void _confirmCreateOrder() {
		double amount = BalanceFormatter.parseUserInput(amount_input.getText().toString());
		long amountRaw = com.harvana.app.p2p.P2pAsset.toRaw(p2pAsset, amount);
		if (lastPreviewedAmountRaw != amountRaw) {
			_showValidationError("Still checking this order — try again in a moment.");
			return;
		}
		
		double amountHarv = com.harvana.app.p2p.P2pAsset.fromRaw(p2pAsset, amountRaw);
		double fiatTotal = unitPrice >= 0 ? amountHarv * unitPrice : -1;
		java.util.ArrayList<String[]> rows = new java.util.ArrayList<>();
		rows.add(new String[]{"You pay", fiatTotal >= 0 ? P2pMoney.fiat(fiatTotal, listingCurrency) : "The listed price"});
		rows.add(new String[]{"Platform fee", BalanceFormatter.smart(com.harvana.app.p2p.P2pAsset.fromRaw(p2pAsset, lastPlatformFeeRaw))});
		rows.add(new String[]{"Network fee", BalanceFormatter.smart(com.harvana.app.p2p.P2pAsset.fromRaw(p2pAsset, lastNetworkFeeRaw))});
		rows.add(new String[]{"You receive", BalanceFormatter.smart(com.harvana.app.p2p.P2pAsset.fromRaw(p2pAsset, lastNetAmountRaw))});
		rows.add(new String[]{"For", "Opening this buy against the listing"});
		com.harvana.app.group.PaymentConfirmSheet.reviewThenAuthorize(this,
		"Open this buy", BalanceFormatter.smart(amountHarv), rows, () -> {
			create_order_button.setEnabled(false);
			create_order_button.setText("Creating order…");
			
			P2PTradeRouter.claimPayment(getApplicationContext(), listingId, myAddress, amountRaw, p2pAsset, new P2PTradeRouter.Callback() {
				@Override
				public void onSuccess(JSONObject result) {
					String newOrderId = P2pIds.orderId(result);
					com.harvana.app.p2p.P2pChatStore.notice(newOrderId, sellerUidForChat, "New order: " + com.harvana.app.wallet.BalanceFormatter.smart(amountRaw / (double) com.harvana.app.p2p.P2pAsset.unitsPerCoin(p2pAsset)) + " " + com.harvana.app.p2p.P2pAsset.ticker(p2pAsset) + ". I will pay within the payment window.");
					String claimerUid = FirebaseAuth.getInstance().getCurrentUser() != null
					? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
					if (claimerUid != null && !newOrderId.isEmpty()) {
						P2PPaymentDetailsManager.getInstance(getApplicationContext())
						.markBuyerClaimed(listingId, claimerUid, newOrderId, new P2PPaymentDetailsManager.SimpleCallback() {
							@Override public void onSuccess() { }
							@Override public void onError(String error) {
								runOnUiThread(() -> Toast.makeText(P2pBuyOrderActivity.this,
								"Order created, but couldn't register you as the buyer: " + error, Toast.LENGTH_LONG).show());
							}
						});
					}
					runOnUiThread(() -> {
						if (!newOrderId.isEmpty()) {
							Intent intent = new Intent(P2pBuyOrderActivity.this, P2pBuyTradeActivity.class);
							intent.putExtra("orderId", newOrderId);
							startActivity(intent);
							finish();
						} else {
							create_order_button.setEnabled(true);
							create_order_button.setText(("Buy " + com.harvana.app.p2p.P2pAsset.ticker(p2pAsset)));
							_showValidationError("Order created, but couldn't open the trade automatically. Check My Trades.");
						}
					});
				}
				@Override
				public void onError(String error) {
					runOnUiThread(() -> {
						create_order_button.setEnabled(true);
						create_order_button.setText(("Buy " + com.harvana.app.p2p.P2pAsset.ticker(p2pAsset)));
						_showValidationError("Couldn't create this order — the listing may have just changed: " + error);
					});
				}
			});
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
