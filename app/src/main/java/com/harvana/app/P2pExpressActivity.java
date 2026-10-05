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
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.biometric.*;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import com.harvana.app.firestore.P2PPaymentDetailsManager;
import com.harvana.app.firestore.P2PTradeStatsManager;
import com.harvana.app.p2p.P2pCurrencyFilterState;
import com.harvana.app.p2p.P2pIds;
import com.harvana.app.p2p.P2pMoney;
import com.harvana.app.p2p.ThemedDialogs;
import com.harvana.app.sync.HarvanaNodeClient;
import com.harvana.app.tx.P2PTradeRouter;
import com.harvana.app.wallet.*;
import com.budiyev.android.codescanner.*;
import com.bumptech.glide.*;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.card.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.*;
import org.json.*;



public class P2pExpressActivity extends AppCompatActivity {
	
	private Toolbar _toolbar;
	private AppBarLayout _app_bar;
	private CoordinatorLayout _coordinator;
	
	private LinearLayout express_root;
	private MaterialCardView card_express_membership;
	private LinearLayout express_trade_selector;
	private MaterialCardView card_express_match;
	private TextView textview237;
	private LinearLayout linear394;
	private MaterialCardView card_express_liquidity;
	private MaterialCardView card_express_security;
	private TextView tv_express_footer;
	private LinearLayout linear380;
	private LinearLayout linear381;
	private LinearLayout linear383;
	private LinearLayout linear382;
	private TextView tv_express_tier_level;
	private TextView tv_express_tier;
	private TextView tv_express_tier_description;
	private LinearLayout linear384;
	private LinearLayout linear385;
	private LinearLayout linear386;
	private TextView tv_express_trust;
	private TextView textview228;
	private TextView tv_express_completed;
	private TextView textview229;
	private TextView tv_express_limit;
	private TextView textview230;
	private TextView express_buy_tab;
	private TextView express_sell_tab;
	private LinearLayout linear387;
	private TextView tv_express_action_title;
	private TextView tv_express_action_description;
	private TextView textview231;
	private LinearLayout layout_express_amount;
	private HorizontalScrollView hscroll5;
	private TextView textview233;
	private LinearLayout layout_express_payment_method;
	private MaterialCardView card_express_quote;
	private LinearLayout layout_express_match_status;
	private TextView btn_express_find_match;
	private TextView textview232;
	private EditText et_express_amount;
	private LinearLayout linear388;
	private TextView express_quick_1;
	private TextView express_quick_2;
	private TextView express_quick_3;
	private TextView express_quick_4;
	private TextView tv_express_payment_method;
	private TextView tv_express_payment_arrow;
	private LinearLayout linear389;
	private LinearLayout linear390;
	private LinearLayout linear391;
	private LinearLayout linear392;
	private TextView textview234;
	private TextView tv_express_rate;
	private TextView textview235;
	private TextView tv_express_fiat;
	private TextView textview236;
	private TextView tv_express_fee;
	private TextView tv_express_match_indicator;
	private LinearLayout linear393;
	private TextView tv_express_match_title;
	private TextView tv_express_match_description;
	private MaterialCardView card_express_fast;
	private MaterialCardView card_express_premium;
	private LinearLayout linear395;
	private TextView textview238;
	private TextView textview239;
	private LinearLayout linear396;
	private TextView textview240;
	private TextView textview241;
	private LinearLayout linear397;
	private LinearLayout linear398;
	private LinearLayout linear399;
	private TextView textview242;
	private TextView tv_express_liquidity_status;
	private LinearLayout linear400;
	private LinearLayout linear401;
	private LinearLayout linear402;
	private TextView tv_express_available_harv;
	private TextView textview243;
	private TextView tv_express_counterparties;
	private TextView textview244;
	private TextView tv_express_average_time;
	private TextView textview245;
	private LinearLayout linear403;
	private TextView textview246;
	private LinearLayout linear404;
	private TextView textview247;
	private TextView tv_express_security;
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.p2p_express);
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
		express_root = findViewById(R.id.express_root);
		card_express_membership = findViewById(R.id.card_express_membership);
		express_trade_selector = findViewById(R.id.express_trade_selector);
		card_express_match = findViewById(R.id.card_express_match);
		textview237 = findViewById(R.id.textview237);
		linear394 = findViewById(R.id.linear394);
		card_express_liquidity = findViewById(R.id.card_express_liquidity);
		card_express_security = findViewById(R.id.card_express_security);
		tv_express_footer = findViewById(R.id.tv_express_footer);
		linear380 = findViewById(R.id.linear380);
		linear381 = findViewById(R.id.linear381);
		linear383 = findViewById(R.id.linear383);
		linear382 = findViewById(R.id.linear382);
		tv_express_tier_level = findViewById(R.id.tv_express_tier_level);
		tv_express_tier = findViewById(R.id.tv_express_tier);
		tv_express_tier_description = findViewById(R.id.tv_express_tier_description);
		linear384 = findViewById(R.id.linear384);
		linear385 = findViewById(R.id.linear385);
		linear386 = findViewById(R.id.linear386);
		tv_express_trust = findViewById(R.id.tv_express_trust);
		textview228 = findViewById(R.id.textview228);
		tv_express_completed = findViewById(R.id.tv_express_completed);
		textview229 = findViewById(R.id.textview229);
		tv_express_limit = findViewById(R.id.tv_express_limit);
		textview230 = findViewById(R.id.textview230);
		express_buy_tab = findViewById(R.id.express_buy_tab);
		express_sell_tab = findViewById(R.id.express_sell_tab);
		linear387 = findViewById(R.id.linear387);
		tv_express_action_title = findViewById(R.id.tv_express_action_title);
		tv_express_action_description = findViewById(R.id.tv_express_action_description);
		textview231 = findViewById(R.id.textview231);
		layout_express_amount = findViewById(R.id.layout_express_amount);
		hscroll5 = findViewById(R.id.hscroll5);
		textview233 = findViewById(R.id.textview233);
		layout_express_payment_method = findViewById(R.id.layout_express_payment_method);
		card_express_quote = findViewById(R.id.card_express_quote);
		layout_express_match_status = findViewById(R.id.layout_express_match_status);
		btn_express_find_match = findViewById(R.id.btn_express_find_match);
		textview232 = findViewById(R.id.textview232);
		et_express_amount = findViewById(R.id.et_express_amount);
		linear388 = findViewById(R.id.linear388);
		express_quick_1 = findViewById(R.id.express_quick_1);
		express_quick_2 = findViewById(R.id.express_quick_2);
		express_quick_3 = findViewById(R.id.express_quick_3);
		express_quick_4 = findViewById(R.id.express_quick_4);
		tv_express_payment_method = findViewById(R.id.tv_express_payment_method);
		tv_express_payment_arrow = findViewById(R.id.tv_express_payment_arrow);
		linear389 = findViewById(R.id.linear389);
		linear390 = findViewById(R.id.linear390);
		linear391 = findViewById(R.id.linear391);
		linear392 = findViewById(R.id.linear392);
		textview234 = findViewById(R.id.textview234);
		tv_express_rate = findViewById(R.id.tv_express_rate);
		textview235 = findViewById(R.id.textview235);
		tv_express_fiat = findViewById(R.id.tv_express_fiat);
		textview236 = findViewById(R.id.textview236);
		tv_express_fee = findViewById(R.id.tv_express_fee);
		tv_express_match_indicator = findViewById(R.id.tv_express_match_indicator);
		linear393 = findViewById(R.id.linear393);
		tv_express_match_title = findViewById(R.id.tv_express_match_title);
		tv_express_match_description = findViewById(R.id.tv_express_match_description);
		card_express_fast = findViewById(R.id.card_express_fast);
		card_express_premium = findViewById(R.id.card_express_premium);
		linear395 = findViewById(R.id.linear395);
		textview238 = findViewById(R.id.textview238);
		textview239 = findViewById(R.id.textview239);
		linear396 = findViewById(R.id.linear396);
		textview240 = findViewById(R.id.textview240);
		textview241 = findViewById(R.id.textview241);
		linear397 = findViewById(R.id.linear397);
		linear398 = findViewById(R.id.linear398);
		linear399 = findViewById(R.id.linear399);
		textview242 = findViewById(R.id.textview242);
		tv_express_liquidity_status = findViewById(R.id.tv_express_liquidity_status);
		linear400 = findViewById(R.id.linear400);
		linear401 = findViewById(R.id.linear401);
		linear402 = findViewById(R.id.linear402);
		tv_express_available_harv = findViewById(R.id.tv_express_available_harv);
		textview243 = findViewById(R.id.textview243);
		tv_express_counterparties = findViewById(R.id.tv_express_counterparties);
		textview244 = findViewById(R.id.textview244);
		tv_express_average_time = findViewById(R.id.tv_express_average_time);
		textview245 = findViewById(R.id.textview245);
		linear403 = findViewById(R.id.linear403);
		textview246 = findViewById(R.id.textview246);
		linear404 = findViewById(R.id.linear404);
		textview247 = findViewById(R.id.textview247);
		tv_express_security = findViewById(R.id.tv_express_security);
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
		
		myAddress = WalletManager.getActiveWalletAddress(this);
		myUid = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
		myMethods = PaymentMethodManager.loadAll(this);
		
		tv_express_average_time.setText("Not yet tracked");
		card_express_match.setVisibility(View.GONE);
		card_express_quote.setVisibility(View.GONE);
		layout_express_match_status.setVisibility(View.GONE);
		
		express_buy_tab.setOnClickListener(v -> _setMode(true));
		express_sell_tab.setOnClickListener(v -> _setMode(false));
		layout_express_payment_method.setOnClickListener(v -> _showPaymentMethodPicker());
		if (express_quick_1 != null) express_quick_1.setOnClickListener(v -> { et_express_amount.setText("10"); });
		if (express_quick_2 != null) express_quick_2.setOnClickListener(v -> { et_express_amount.setText("25"); });
		if (express_quick_3 != null) express_quick_3.setOnClickListener(v -> { et_express_amount.setText("50"); });
		if (express_quick_4 != null) express_quick_4.setOnClickListener(v -> { et_express_amount.setText("100"); });
		btn_express_find_match.setOnClickListener(v -> {
			if (matchedListingId == null) _searchForMatch(); else _confirmAndClaim();
		});
		et_express_amount.addTextChangedListener(new TextWatcher() {
			@Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
			@Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
			@Override public void afterTextChanged(Editable s) { _onAmountChanged(); }
		});
		
		_checkEligibility();
		_loadWalletBalance();
		
		
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Create the status indicator text view
		TextView expressStatus = new TextView(this);
		expressStatus.setText("EXPRESS");
		expressStatus.setTextSize(10f); // Smaller text size for an indicator
		expressStatus.setTextColor(Color.WHITE); 
		expressStatus.setPadding(24, 12, 24, 12); // Left, Top, Right, Bottom padding
		expressStatus.setGravity(android.view.Gravity.CENTER);
		
		// Create a rounded background programmatically
		android.graphics.drawable.GradientDrawable background = new android.graphics.drawable.GradientDrawable();
		background.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
		background.setCornerRadius(12f); // Rounded corners
		
		// Set the background color (You can replace Color.BLUE with your specific theme color)
		int badgeColor = com.google.android.material.color.MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimary, android.graphics.Color.parseColor("#1E3A8A"));
		background.setColor(badgeColor); 
		
		expressStatus.setBackground(background);
		
		// Add layout parameters to ensure it fits nicely in the toolbar with a right margin
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
		LinearLayout.LayoutParams.WRAP_CONTENT, 
		LinearLayout.LayoutParams.WRAP_CONTENT
		);
		params.setMargins(0, 0, 48, 0); // Padding from the right edge of the screen
		expressStatus.setLayoutParams(params);
		
		// Add the indicator to the App Bar menu
		MenuItem item = menu.add("Express Status");
		item.setActionView(expressStatus);
		item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		
		// (Optional) Disable clicking if it's strictly a visual indicator
		expressStatus.setClickable(false);
		
		return super.onCreateOptionsMenu(menu);
	}
	

	public void _MORE() {
	}
	private String myAddress = "";
	private String myUid = "";
	private boolean buyMode = true;
	private boolean iAmEligible = false;
	private double walletBalanceHarv = 0;
	private PaymentMethod selectedMethod = null;
	private List<PaymentMethod> myMethods = new ArrayList<>();
	
	private final Handler debounceHandler = new Handler(Looper.getMainLooper());
	private Runnable pendingSearch;
	private CountDownTimer previewTimer;
	
	private String matchedListingId = null;
	private String matchedPosterAddress = null;
	private String matchedSellerUid = null;
	private long matchedHarvAmountRaw = 0;
	private double matchedPrice = 0;
	private double lastSpend = 0;
	
	private void _loadWalletBalance() {
		if (myAddress == null || myAddress.isEmpty()) return;
		WalletNodeBridge.fetchLiveBalance(this, myAddress, new WalletNodeBridge.BalanceCallback() {
			@Override
			public void onSuccess(double balanceHarv, long nonce, long blockHeight) {
				walletBalanceHarv = balanceHarv;
			}
			@Override
			public void onError(String error) { }
		});
	}
	
	// ── Requester's own eligibility (new vs the old Express) ────────────
	
	private void _checkEligibility() {
		if (myUid == null || myAddress == null || myAddress.isEmpty()) {
			_renderIneligible("No wallet or account found on this device.");
			return;
		}
		TrustScoreEngine.compute(getApplicationContext(), myUid, new TrustScoreEngine.Callback() {
			@Override
			public void onComplete(TrustScoreEngine.Result result) {
				boolean cleanRecord = result.verified && result.reportCount == 0;
				com.harvana.app.trust.NetworkTrust.wallet(myAddress, score -> runOnUiThread(() -> {
					tv_express_trust.setText(score.label());
					tv_express_tier_level.setText(score.label());
				}));
				
				HarvanaNodeClient.getP2PInsuranceStatus(myAddress, new HarvanaNodeClient.JsonCallback() {
					@Override
					public void onSuccess(JSONObject insuranceJson) {
						boolean meetsDeposit = insuranceJson.optBoolean("meetsInsuranceThreshold", false);
						P2PTradeStatsManager.getInstance(getApplicationContext()).getStats(myUid, new P2PTradeStatsManager.StatsCallback() {
							@Override
							public void onSuccess(int totalTrades, int successfulTrades, double successRatePercent) {
								tv_express_completed.setText(String.valueOf(totalTrades));
								boolean insured = meetsDeposit && cleanRecord;
								boolean premium = insured && totalTrades >= 100;
								runOnUiThread(() -> {
									if (premium) _renderEligible();
									else _renderIneligible(!cleanRecord
									? "You need to be verified with a clean trust record."
									: !meetsDeposit
									? "You need an active insurance deposit."
									: "You need at least 100 completed trades (" + totalTrades + " so far).");
								});
							}
							@Override
							public void onError(String error) {
								runOnUiThread(() -> _renderIneligible("Couldn't verify your eligibility right now."));
							}
						});
					}
					@Override
					public void onError(String error) {
						runOnUiThread(() -> _renderIneligible("Couldn't verify your eligibility right now."));
					}
				});
			}
			@Override
			public void onError(String error) {
				runOnUiThread(() -> _renderIneligible("Couldn't verify your eligibility right now."));
			}
		});
	}
	
	private void _renderEligible() {
		iAmEligible = true;
		tv_express_tier.setText("PREMIUM TRADER");
		tv_express_tier_description.setText("You qualify for Express — instant matching with other Insured Traders.");
		card_express_match.setVisibility(View.VISIBLE);
		_setMode(true);
	}
	
	private void _renderIneligible(String reason) {
		iAmEligible = false;
		tv_express_tier.setText("NOT ELIGIBLE YET");
		tv_express_tier_description.setText(reason + " Build your record on the regular Buy/Sell market, or check Insurance for what's missing.");
		card_express_match.setVisibility(View.GONE);
		tv_express_limit.setText("—");
	}
	
	// ── Mode + amount ────────────────────────────────────────────────────
	
	private void _setMode(boolean buy) {
		buyMode = buy;
		express_buy_tab.setAlpha(buy ? 1f : 0.6f);
		express_sell_tab.setAlpha(!buy ? 1f : 0.6f);
		tv_express_action_title.setText(buy ? ("Buy " + com.harvana.app.CoinInfo.ticker() + " instantly") : ("Sell " + com.harvana.app.CoinInfo.ticker() + " instantly"));
		tv_express_action_description.setText(buy
		? ("Matches you with the best-priced Insured Trader selling " + com.harvana.app.CoinInfo.ticker() + " right now.")
		: ("Matches you with the best-priced Insured Trader buying " + com.harvana.app.CoinInfo.ticker() + " right now."));
		et_express_amount.setText("");
		layout_express_payment_method.setVisibility(buy ? View.GONE : View.VISIBLE);
		tv_express_payment_method.setText(buy ? "Any payment method" : (selectedMethod != null ? selectedMethod.primaryField : "Select receiving account"));
		_resetMatch();
		card_express_quote.setVisibility(View.GONE);
		layout_express_match_status.setVisibility(View.GONE);
	}
	
	private void _showPaymentMethodPicker() {
		if (buyMode) {
			Toast.makeText(this, "Payment-method filtering for Buy mode isn't wired to the matcher yet — matches consider all methods for now.", Toast.LENGTH_LONG).show();
			return;
		}
		if (myMethods.isEmpty()) {
			ThemedDialogs.show(ThemedDialogs.alert(this).setTitle("No payment methods saved")
			.setMessage("Add a bank or mobile money account in Settings before using Express to sell.")
			.setPositiveButton("OK", null));
			return;
		}
		String[] labels = new String[myMethods.size()];
		for (int i = 0; i < myMethods.size(); i++) labels[i] = myMethods.get(i).label + " (" + myMethods.get(i).displaySummary() + ")";
		ThemedDialogs.show(ThemedDialogs.alert(this).setTitle("Receive payment into").setItems(labels, (dialog, which) -> {
			selectedMethod = myMethods.get(which);
			tv_express_payment_method.setText(selectedMethod.primaryField);
		}));
	}
	
	private void _resetMatch() {
		matchedListingId = null;
		matchedHarvAmountRaw = 0;
		matchedPrice = 0;
		if (previewTimer != null) previewTimer.cancel();
		btn_express_find_match.setText("Find Match");
		btn_express_find_match.setEnabled(false);
	}
	
	private void _onAmountChanged() {
		_resetMatch();
		double amount = BalanceFormatter.parseUserInput(et_express_amount.getText().toString());
		lastSpend = amount;
		if (pendingSearch != null) debounceHandler.removeCallbacks(pendingSearch);
		card_express_quote.setVisibility(View.GONE);
		layout_express_match_status.setVisibility(View.GONE);
		if (amount <= 0) return;
		btn_express_find_match.setEnabled(true);
	}
	
	// ── Matching — same qualification rules as before, now gated on the requester too ──
	
	private void _searchForMatch() {
		double spend = BalanceFormatter.parseUserInput(et_express_amount.getText().toString());
		if (spend <= 0) return;
		btn_express_find_match.setEnabled(false);
		btn_express_find_match.setText("Searching…");
		layout_express_match_status.setVisibility(View.VISIBLE);
		tv_express_match_indicator.setText("●");
		tv_express_match_title.setText("Searching for the best available price…");
		tv_express_match_description.setText("");
		
		String side = buyMode ? "SELL" : "BUY";
		HarvanaNodeClient.getP2POpenListings(0, 50, side, new HarvanaNodeClient.JsonCallback() {
			@Override
			public void onSuccess(JSONObject result) {
				List<JSONObject> listings = new ArrayList<>();
				JSONArray arr = result.optJSONArray("listings");
				if (arr != null) for (int i = 0; i < arr.length(); i++) {
					JSONObject l = arr.optJSONObject(i);
					if (l != null) listings.add(l);
				}
				if (listings.isEmpty()) { _applyNoMatch(); return; }
				
				AtomicInteger remaining = new AtomicInteger(listings.size());
				AtomicReference<Object[]> best = new AtomicReference<>(null);
				AtomicReference<Double> totalLiquidity = new AtomicReference<>(0.0);
				AtomicInteger qualifyingCount = new AtomicInteger(0);
				
				for (JSONObject listing : listings) {
					_evaluateOne(listing, spend, (candidate, liquidityIfQualified) -> {
						if (liquidityIfQualified != null) {
							synchronized (totalLiquidity) { totalLiquidity.set(totalLiquidity.get() + liquidityIfQualified); }
							qualifyingCount.incrementAndGet();
						}
						if (candidate != null) {
							synchronized (best) {
								Object[] current = best.get();
								if (current == null) best.set(candidate);
								else {
									double cp = (double) current[1], np = (double) candidate[1];
									if (buyMode ? np < cp : np > cp) best.set(candidate);
								}
							}
						}
						if (remaining.decrementAndGet() == 0) {
							runOnUiThread(() -> {
								tv_express_available_harv.setText(BalanceFormatter.smartDecimal(totalLiquidity.get()) + (" " + com.harvana.app.CoinInfo.ticker()));
								tv_express_counterparties.setText(String.valueOf(qualifyingCount.get()));
								tv_express_liquidity_status.setText(qualifyingCount.get() > 0 ? "Live" : "None right now");
							});
							_applyBestMatch(best.get(), spend);
						}
					});
				}
			}
			@Override
			public void onError(String error) {
				runOnUiThread(() -> {
					tv_express_match_title.setText("Couldn't reach Harvana Protocol");
					tv_express_match_description.setText(error);
					btn_express_find_match.setEnabled(true);
					btn_express_find_match.setText("Find Match");
				});
			}
		});
	}
	
	private interface CandidateCallback { void onResult(Object[] candidate, Double liquidityIfQualified); }
	
	private void _evaluateOne(JSONObject listing, double spend, CandidateCallback callback) {
		String listingId = P2pIds.listingId(listing);
		String posterAddress = "BUY".equals(listing.optString("side", ""))
		? listing.optString("buyerAddress", "")
		: listing.optString("sellerAddress", "");
		long remainingRaw = listing.optLong("remainingAmount", 0);
		long minOrderRaw = listing.optLong("minOrderAmount", 0);
		if (listingId.isEmpty() || posterAddress.isEmpty()) { callback.onResult(null, null); return; }
		if (myAddress != null && myAddress.equals(posterAddress)) { callback.onResult(null, null); return; }
		if (myAddress != null && myAddress.equals(posterAddress)) { callback.onResult(null, null); return; }
		
		HarvanaNodeClient.getP2PInsuranceStatus(posterAddress, new HarvanaNodeClient.JsonCallback() {
			@Override
			public void onSuccess(JSONObject insuranceJson) {
				if (!insuranceJson.optBoolean("meetsInsuranceThreshold", false)) { callback.onResult(null, null); return; }
				long insuranceDepositRaw = insuranceJson.optLong("insuranceDepositRaw", 0);
				
				P2PPaymentDetailsManager.getInstance(getApplicationContext()).get(listingId, new P2PPaymentDetailsManager.DocCallback() {
					@Override
					public void onSuccess(Map<String, Object> data) {
						Object priceObj = data.get("unitPrice");
						double price = priceObj instanceof Number ? ((Number) priceObj).doubleValue() : -1;
						Object uidObj = data.get("sellerUid");
						String uid = uidObj != null ? String.valueOf(uidObj) : "";
						Object currencyObj = data.get("currency");
						String listingCcy = currencyObj != null ? String.valueOf(currencyObj) : "";
						if (!listingCcy.isEmpty() && !listingCcy.equalsIgnoreCase(P2pCurrencyFilterState.getCurrency())) {
							callback.onResult(null, null); return;
						}
						if (price <= 0 || uid.isEmpty()) { callback.onResult(null, null); return; }
						
						TrustScoreEngine.compute(getApplicationContext(), uid, new TrustScoreEngine.Callback() {
							@Override
							public void onComplete(TrustScoreEngine.Result r) {
								boolean qualifies = r.verified && r.reportCount == 0;
								if (!qualifies) { callback.onResult(null, null); return; }
								
								double liquidity = HarvanaNodeClient.toHarvDisplay(Math.min(remainingRaw, insuranceDepositRaw));
								double candidateHarv = buyMode ? (spend / price) : spend;
								long candidateHarvRaw = HarvanaNodeClient.toRawUnits(candidateHarv);
								boolean fits = candidateHarvRaw >= minOrderRaw && candidateHarvRaw <= remainingRaw && candidateHarvRaw <= insuranceDepositRaw;
								callback.onResult(fits ? new Object[]{listingId, price, candidateHarvRaw, posterAddress, uid} : null, liquidity);
							}
							@Override
							public void onError(String error) { callback.onResult(null, null); }
						});
					}
					@Override
					public void onError(String error) { callback.onResult(null, null); }
				});
			}
			@Override
			public void onError(String error) { callback.onResult(null, null); }
		});
	}
	
	private void _applyNoMatch() {
		runOnUiThread(() -> {
			layout_express_match_status.setVisibility(View.VISIBLE);
			tv_express_match_indicator.setText("○");
			tv_express_match_title.setText("No open listings right now");
			tv_express_match_description.setText("Try the regular market instead.");
			btn_express_find_match.setEnabled(true);
			btn_express_find_match.setText("Find Match");
		});
	}
	
	private void _applyBestMatch(Object[] best, double spend) {
		runOnUiThread(() -> {
			btn_express_find_match.setEnabled(true);
			if (best == null) {
				tv_express_match_indicator.setText("○");
				tv_express_match_title.setText("No Insured Trader can fill this amount right now");
				tv_express_match_description.setText("Try a smaller amount, or use the regular market.");
				btn_express_find_match.setText("Find Match");
				return;
			}
			matchedListingId = (String) best[0];
			matchedPrice = (double) best[1];
			matchedHarvAmountRaw = (long) best[2];
			matchedPosterAddress = (String) best[3];
			matchedSellerUid = (String) best[4];
			
			double harvAmount = HarvanaNodeClient.toHarvDisplay(matchedHarvAmountRaw);
			String ccy = P2pCurrencyFilterState.getCurrency();
			card_express_quote.setVisibility(View.VISIBLE);
			tv_express_rate.setText(("1 " + com.harvana.app.CoinInfo.ticker() + " ≈ ") + P2pMoney.fiat(matchedPrice, ccy));
			tv_express_fiat.setText(P2pMoney.fiat(harvAmount * matchedPrice, ccy));
			tv_express_fee.setText("Shown after confirming — set by the node, never estimated here.");
			
			tv_express_match_indicator.setText("✓");
			tv_express_match_title.setText("Matched — best available price found");
			tv_express_match_description.setText(buyMode
			? "You'll receive " + BalanceFormatter.smart(harvAmount) + (" " + com.harvana.app.CoinInfo.ticker() + ".")
			: "You'll provide " + BalanceFormatter.smart(harvAmount) + (" " + com.harvana.app.CoinInfo.ticker() + "."));
			btn_express_find_match.setText("Confirm & Trade");
			_startFreshnessCountdown();
		});
	}
	
	private void _startFreshnessCountdown() {
		if (previewTimer != null) previewTimer.cancel();
		previewTimer = new CountDownTimer(30_000L, 1000) {
			@Override
			public void onTick(long millisUntilFinished) {
				tv_express_match_description.setText(String.format(Locale.US,
				"Price held for %ds — a quote window, not an on-chain reservation.", millisUntilFinished / 1000));
			}
			@Override
			public void onFinish() {
				tv_express_match_title.setText("Quote expired — re-checking…");
				_resetMatch();
				_searchForMatch();
			}
		}.start();
	}
	
	// ── Confirm — same claimPayment() everything else uses, then hand off ──
	
	private void _confirmAndClaim() {
		if (matchedListingId == null) return;
		if (!buyMode && selectedMethod == null) {
			Toast.makeText(this, "Select a payment account to receive into first.", Toast.LENGTH_LONG).show();
			_showPaymentMethodPicker();
			return;
		}
		if (previewTimer != null) previewTimer.cancel();
		
		double harvAmount = HarvanaNodeClient.toHarvDisplay(matchedHarvAmountRaw);
		String ccy = P2pCurrencyFilterState.getCurrency();
		java.util.ArrayList<String[]> rows = new java.util.ArrayList<>();
		rows.add(new String[]{"You", buyMode ? "Buy HARV" : "Sell HARV"});
		rows.add(new String[]{"Counterparty", matchedPosterAddress == null ? "Matched trader" : matchedPosterAddress});
		rows.add(new String[]{"Price", "1 HARV ≈ " + P2pMoney.fiat(matchedPrice, ccy)});
		rows.add(new String[]{"Fiat", P2pMoney.fiat(harvAmount * matchedPrice, ccy)});
		rows.add(new String[]{"Network fee", "Set by the node when this trade opens"});
		com.harvana.app.group.PaymentConfirmSheet.reviewThenAuthorize(this,
		buyMode ? "Buy on express" : "Sell on express",
		BalanceFormatter.smart(harvAmount), rows, () -> {
			btn_express_find_match.setEnabled(false);
			btn_express_find_match.setText("Confirming…");
			
			P2PTradeRouter.claimPayment(getApplicationContext(), matchedListingId, myAddress, matchedHarvAmountRaw, new P2PTradeRouter.Callback() {
				@Override
				public void onSuccess(JSONObject result) {
					String newOrderId = P2pIds.orderId(result);
					if (newOrderId.isEmpty()) {
						runOnUiThread(() -> Toast.makeText(P2pExpressActivity.this, "Matched, but couldn't open the trade. Check My Trades.", Toast.LENGTH_LONG).show());
						return;
					}
					if (buyMode) {
						if (myUid != null) {
							P2PPaymentDetailsManager.getInstance(getApplicationContext())
							.markBuyerClaimed(matchedListingId, myUid, newOrderId, new P2PPaymentDetailsManager.SimpleCallback() {
								@Override public void onSuccess() { }
								@Override public void onError(String error) { }
							});
						}
						_goToTrade(newOrderId, true);
					} else {
						P2PPaymentDetailsManager.getInstance(getApplicationContext())
						.saveOrderPaymentDetails(newOrderId, selectedMethod, myUid, matchedSellerUid, new P2PPaymentDetailsManager.SimpleCallback() {
							@Override public void onSuccess() { _goToTrade(newOrderId, false); }
							@Override public void onError(String error) { _goToTrade(newOrderId, false); }
						});
					}
				}
				@Override
				public void onError(String error) {
					runOnUiThread(() -> {
						btn_express_find_match.setEnabled(true);
						btn_express_find_match.setText("Confirm & Trade");
						Toast.makeText(P2pExpressActivity.this, "Couldn't complete the match: " + error, Toast.LENGTH_LONG).show();
					});
				}
			});
		});
	}
	
	private void _goToTrade(String orderId, boolean iAmBuyer) {
		runOnUiThread(() -> {
			Intent intent = new Intent(P2pExpressActivity.this, iAmBuyer ? P2pBuyTradeActivity.class : P2pSellTradeActivity.class);
			intent.putExtra("orderId", orderId);
			startActivity(intent);
			finish();
		});
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (pendingSearch != null) debounceHandler.removeCallbacks(pendingSearch);
		if (previewTimer != null) previewTimer.cancel();
	}
	
	{
	}
	
}
