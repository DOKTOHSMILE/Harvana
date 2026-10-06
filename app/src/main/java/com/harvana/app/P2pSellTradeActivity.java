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
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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
import com.harvana.app.wallet.PinPromptHelper;
import com.harvana.app.wallet.WalletManager;
import com.harvana.app.p2p.P2pMoney;
import com.harvana.app.p2p.ThemedDialogs;
import com.google.firebase.auth.FirebaseAuth;


public class P2pSellTradeActivity extends AppCompatActivity {
	
	private Toolbar _toolbar;
	private AppBarLayout _app_bar;
	private CoordinatorLayout _coordinator;
	
	private LinearLayout sell_trade_root;
	private MaterialCardView card_sell_action;
	private MaterialCardView card_sell_value;
	private MaterialCardView card_sell_buyer;
	private MaterialCardView card_sell_escrow;
	private MaterialCardView card_sell_payment;
	private MaterialCardView card_sell_warning;
	private MaterialCardView card_sell_action_button;
	private MaterialCardView card_sell_timeline;
	private MaterialCardView card_sell_release;
	private MaterialCardView card_sell_security;
	private LinearLayout linear154;
	private MaterialCardView card_sell_completed;
	private LinearLayout linear134;
	private TextView tv_sell_action_title;
	private TextView tv_sell_action_description;
	private LinearLayout linear135;
	private ProgressBar pb_sell_trade;
	private TextView textview65;
	private TextView tv_sell_timer;
	private LinearLayout linear136;
	private TextView textview66;
	private TextView tv_sell_harv_amount;
	private TextView tv_sell_value_caption;
	private View view137;
	private LinearLayout linear138;
	private LinearLayout linear139;
	private LinearLayout row_sell_maker_fee;
	private TextView textview67;
	private TextView tv_sell_fiat_amount;
	private TextView textview68;
	private TextView tv_sell_price;
	private TextView textview69;
	private TextView tv_sell_maker_fee;
	private LinearLayout linear140;
	private TextView textview70;
	private LinearLayout linear141;
	private LinearLayout linear144;
	private FrameLayout frame_layout4;
	private LinearLayout linear142;
	private TextView tv_sell_buyer_initial;
	private ImageView iv_sell_buyer_avatar;
	private LinearLayout linear143;
	private TextView tv_sell_buyer_info;
	private TextView tv_sell_buyer_name;
	private ImageView iv_sell_buyer_verified;
	private TextView tv_sell_buyer_trust;
	private TextView tv_sell_buyer_trades;
	private LinearLayout linear145;
	private LinearLayout linear146;
	private TextView tv_sell_escrow_description;
	private TextView textview72;
	private TextView tv_sell_escrow_tx_hash;
	private TextView textview71;
	private TextView tv_sell_escrow_badge;
	private LinearLayout linear147;
	private TextView textview73;
	private TextView tv_sell_payment_status;
	private TextView tv_sell_payment_status_description;
	private LinearLayout layout_sell_payment_received;
	private View view148;
	private TextView tv_sell_payment_received_amount;
	private TextView tv_sell_payment_reference;
	private TextView tv_sell_payment_time;
	private ImageView iv_sell_payment_proof;
	private LinearLayout linear149;
	private TextView textview74;
	private TextView tv_sell_warning;
	private LinearLayout linear150;
	private TextView tv_sell_action_button_title;
	private TextView tv_sell_action_button_description;
	private CheckBox cb_sell_payment_confirm;
	private MaterialButton btn_sell_confirm_payment;
	private LinearLayout linear151;
	private TextView textview75;
	private TextView tv_sell_step_1;
	private TextView tv_sell_step_2;
	private TextView tv_sell_step_3;
	private TextView tv_sell_step_4;
	private TextView tv_sell_step_5;
	private TextView tv_sell_step_6;
	private LinearLayout linear152;
	private TextView textview76;
	private TextView tv_sell_release_status;
	private TextView tv_sell_release_tx_hash;
	private TextView tv_sell_release_network;
	private TextView tv_sell_release_confirmations;
	private LinearLayout linear153;
	private TextView textview77;
	private TextView textview78;
	private TextView tv_sell_dispute;
	private TextView tv_sell_support;
	private LinearLayout linear155;
	private ImageView iv_sell_completed;
	private TextView tv_sell_completed_title;
	private TextView tv_sell_completed_description;
	private TextView tv_sell_completed_tx_hash;
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.p2p_sell_trade);
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
		sell_trade_root = findViewById(R.id.sell_trade_root);
		card_sell_action = findViewById(R.id.card_sell_action);
		card_sell_value = findViewById(R.id.card_sell_value);
		card_sell_buyer = findViewById(R.id.card_sell_buyer);
		card_sell_escrow = findViewById(R.id.card_sell_escrow);
		card_sell_payment = findViewById(R.id.card_sell_payment);
		card_sell_warning = findViewById(R.id.card_sell_warning);
		card_sell_action_button = findViewById(R.id.card_sell_action_button);
		card_sell_timeline = findViewById(R.id.card_sell_timeline);
		card_sell_release = findViewById(R.id.card_sell_release);
		card_sell_security = findViewById(R.id.card_sell_security);
		linear154 = findViewById(R.id.linear154);
		P2pChatActivity.addButton(this, linear154, () -> orderId, () -> chatOtherUid);
		card_sell_completed = findViewById(R.id.card_sell_completed);
		linear134 = findViewById(R.id.linear134);
		tv_sell_action_title = findViewById(R.id.tv_sell_action_title);
		tv_sell_action_description = findViewById(R.id.tv_sell_action_description);
		linear135 = findViewById(R.id.linear135);
		pb_sell_trade = findViewById(R.id.pb_sell_trade);
		textview65 = findViewById(R.id.textview65);
		tv_sell_timer = findViewById(R.id.tv_sell_timer);
		linear136 = findViewById(R.id.linear136);
		textview66 = findViewById(R.id.textview66);
		tv_sell_harv_amount = findViewById(R.id.tv_sell_harv_amount);
		tv_sell_value_caption = findViewById(R.id.tv_sell_value_caption);
		view137 = findViewById(R.id.view137);
		linear138 = findViewById(R.id.linear138);
		linear139 = findViewById(R.id.linear139);
		row_sell_maker_fee = findViewById(R.id.row_sell_maker_fee);
		textview67 = findViewById(R.id.textview67);
		tv_sell_fiat_amount = findViewById(R.id.tv_sell_fiat_amount);
		textview68 = findViewById(R.id.textview68);
		tv_sell_price = findViewById(R.id.tv_sell_price);
		textview69 = findViewById(R.id.textview69);
		tv_sell_maker_fee = findViewById(R.id.tv_sell_maker_fee);
		linear140 = findViewById(R.id.linear140);
		textview70 = findViewById(R.id.textview70);
		linear141 = findViewById(R.id.linear141);
		linear144 = findViewById(R.id.linear144);
		frame_layout4 = findViewById(R.id.frame_layout4);
		linear142 = findViewById(R.id.linear142);
		tv_sell_buyer_initial = findViewById(R.id.tv_sell_buyer_initial);
		iv_sell_buyer_avatar = findViewById(R.id.iv_sell_buyer_avatar);
		linear143 = findViewById(R.id.linear143);
		tv_sell_buyer_info = findViewById(R.id.tv_sell_buyer_info);
		tv_sell_buyer_name = findViewById(R.id.tv_sell_buyer_name);
		iv_sell_buyer_verified = findViewById(R.id.iv_sell_buyer_verified);
		tv_sell_buyer_trust = findViewById(R.id.tv_sell_buyer_trust);
		tv_sell_buyer_trades = findViewById(R.id.tv_sell_buyer_trades);
		linear145 = findViewById(R.id.linear145);
		linear146 = findViewById(R.id.linear146);
		tv_sell_escrow_description = findViewById(R.id.tv_sell_escrow_description);
		textview72 = findViewById(R.id.textview72);
		tv_sell_escrow_tx_hash = findViewById(R.id.tv_sell_escrow_tx_hash);
		textview71 = findViewById(R.id.textview71);
		tv_sell_escrow_badge = findViewById(R.id.tv_sell_escrow_badge);
		linear147 = findViewById(R.id.linear147);
		textview73 = findViewById(R.id.textview73);
		tv_sell_payment_status = findViewById(R.id.tv_sell_payment_status);
		tv_sell_payment_status_description = findViewById(R.id.tv_sell_payment_status_description);
		layout_sell_payment_received = findViewById(R.id.layout_sell_payment_received);
		view148 = findViewById(R.id.view148);
		tv_sell_payment_received_amount = findViewById(R.id.tv_sell_payment_received_amount);
		tv_sell_payment_reference = findViewById(R.id.tv_sell_payment_reference);
		tv_sell_payment_time = findViewById(R.id.tv_sell_payment_time);
		iv_sell_payment_proof = findViewById(R.id.iv_sell_payment_proof);
		linear149 = findViewById(R.id.linear149);
		textview74 = findViewById(R.id.textview74);
		tv_sell_warning = findViewById(R.id.tv_sell_warning);
		linear150 = findViewById(R.id.linear150);
		tv_sell_action_button_title = findViewById(R.id.tv_sell_action_button_title);
		tv_sell_action_button_description = findViewById(R.id.tv_sell_action_button_description);
		cb_sell_payment_confirm = findViewById(R.id.cb_sell_payment_confirm);
		btn_sell_confirm_payment = findViewById(R.id.btn_sell_confirm_payment);
		linear151 = findViewById(R.id.linear151);
		textview75 = findViewById(R.id.textview75);
		tv_sell_step_1 = findViewById(R.id.tv_sell_step_1);
		tv_sell_step_2 = findViewById(R.id.tv_sell_step_2);
		tv_sell_step_3 = findViewById(R.id.tv_sell_step_3);
		tv_sell_step_4 = findViewById(R.id.tv_sell_step_4);
		tv_sell_step_5 = findViewById(R.id.tv_sell_step_5);
		tv_sell_step_6 = findViewById(R.id.tv_sell_step_6);
		linear152 = findViewById(R.id.linear152);
		textview76 = findViewById(R.id.textview76);
		tv_sell_release_status = findViewById(R.id.tv_sell_release_status);
		tv_sell_release_tx_hash = findViewById(R.id.tv_sell_release_tx_hash);
		tv_sell_release_network = findViewById(R.id.tv_sell_release_network);
		tv_sell_release_confirmations = findViewById(R.id.tv_sell_release_confirmations);
		linear153 = findViewById(R.id.linear153);
		textview77 = findViewById(R.id.textview77);
		textview78 = findViewById(R.id.textview78);
		tv_sell_dispute = findViewById(R.id.tv_sell_dispute);
		tv_sell_support = findViewById(R.id.tv_sell_support);
		linear155 = findViewById(R.id.linear155);
		iv_sell_completed = findViewById(R.id.iv_sell_completed);
		tv_sell_completed_title = findViewById(R.id.tv_sell_completed_title);
		tv_sell_completed_description = findViewById(R.id.tv_sell_completed_description);
		tv_sell_completed_tx_hash = findViewById(R.id.tv_sell_completed_tx_hash);
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
			getSupportActionBar().setTitle(("Sell " + com.harvana.app.CoinInfo.ticker()));
			getSupportActionBar().setSubtitle(("Order #" + com.harvana.app.CoinInfo.ticker() + "-000000"));
		}
		
		
		orderId = getIntent().getStringExtra("orderId");
		myAddress = WalletManager.getActiveWalletAddress(this);
		if (getSupportActionBar() != null) getSupportActionBar().setTitle("Sell Trade");
		
		// Honest, flagged gap — see class doc: no real proof-upload
		// persistence exists yet, so this is a plain placeholder, not a
		// broken image.
		iv_sell_payment_proof.setVisibility(View.GONE);
		
		cb_sell_payment_confirm.setOnCheckedChangeListener((buttonView, isChecked) ->
		btn_sell_confirm_payment.setEnabled(isChecked));
		btn_sell_confirm_payment.setEnabled(false);
		btn_sell_confirm_payment.setOnClickListener(v -> _confirmReceived());
		
		tv_sell_dispute.setOnClickListener(v -> _showDisputeDialog());
		if (tv_sell_support != null) {
			tv_sell_support.setOnClickListener(v -> Toast.makeText(this,
			"In-app support isn't live yet — dispute this trade if you need a review.", Toast.LENGTH_LONG).show());
		}
		
		if (orderId == null || orderId.isEmpty()) {
			_showFatalError("No order specified.");
			return;
		}
		
		_loadOrder();
	}
	
	public void _MORE() {
	}
	private String orderId = "";
	private String listingId = "";
	private String buyerAddress = "";
	private String chatOtherUid = "";
	private String sellerAddress = "";
	private String myAddress = "";
	private String status = "";
	private String orderReferenceLabel = "";
	private double unitPrice = -1;
	private long netAmountToBuyerRaw = 0;
	private long amountRaw = 0;
	private String listingCurrency = "NGN";
	private boolean countdownFired = false;
	private long platformFeeRaw = 0;
	private long markedPaidTimestamp = 0;
	private String escrowTxHashValue = "";
	private String releaseTxHashValue = "";
	
	private final Handler tickHandler = new Handler(Looper.getMainLooper());
	private Runnable tickRunnable;
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Create the status indicator text view
		TextView expressStatus = new TextView(this);
		expressStatus.setText("WAITING");
		expressStatus.setTextSize(10f); // Smaller text size for an indicator
		expressStatus.setTextColor(com.harvana.app.utils.UiColors.onAccent()); 
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
	
	
	private void _setStatusBadge(String label) {
		if (getSupportActionBar() == null) return;
		getSupportActionBar().setSubtitle(orderReferenceLabel.isEmpty() ? label : orderReferenceLabel + " · " + label);
	}
	
	private void _showFatalError(String message) {
		sell_trade_root.removeAllViews();
		TextView tv = new TextView(this);
		tv.setText(message);
		tv.setPadding(48, 96, 48, 48);
		sell_trade_root.addView(tv);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		if (orderId != null && !orderId.isEmpty()
		&& ("AWAITING_PAYMENT".equals(status) || "PAYMENT_SUBMITTED".equals(status))) {
			_loadOrder();
		}
	}
	
	private void _loadOrder() {
		HarvanaNodeClient.getP2POrder(orderId, new HarvanaNodeClient.JsonCallback() {
			@Override
			public void onSuccess(JSONObject order) {
				listingId = order.optString("listingId", "");
				buyerAddress = order.optString("buyerAddress", "");
				sellerAddress = order.optString("sellerAddress", "");
				status = order.optString("status", "");
				netAmountToBuyerRaw = order.optLong("netAmountToBuyer", order.optLong("buyerEntitlementRaw", order.optLong("amount", 0)));
				amountRaw = order.optLong("amount", netAmountToBuyerRaw);
				platformFeeRaw = order.optLong("platformFeeRaw", 0);
				markedPaidTimestamp = order.optLong("markedPaidTimestamp", 0);
				escrowTxHashValue = order.optString("escrowTxHash", "");
				releaseTxHashValue = order.optString("releaseTxHash", "");
				String orderReference = order.optString("orderId", orderId);
				
				if (myAddress == null || (!myAddress.equals(buyerAddress) && !myAddress.equals(sellerAddress))) {
					runOnUiThread(() -> _showFatalError("This trade doesn't belong to this device's wallet."));
					return;
				}
				
				runOnUiThread(() -> {
					orderReferenceLabel = orderReference;
					_renderValueCard();
					_renderStatus();
				});
				
				_loadBuyerIdentity();
				_loadOwnPaymentDetails();
			}
			@Override
			public void onError(String error) {
				runOnUiThread(() -> _showFatalError("Couldn't load this trade: " + error));
			}
		});
	}
	
	// ── Mutual visibility: resolve the buyer (this is the real gap fix) ──
	
	private void _loadBuyerIdentity() {
		tv_sell_buyer_info.setText(_shortAddr(buyerAddress));
		P2PPaymentDetailsManager.getInstance(getApplicationContext())
		.getBuyerUidForOrder(listingId, orderId, new P2PPaymentDetailsManager.UidCallback() {
			@Override
			public void onFound(String buyerUid) {
				chatOtherUid = buyerUid;
				UserProfileManager.getInstance(getApplicationContext()).getProfile(buyerUid, new UserProfileManager.DocCallback() {
					@Override
					public void onSuccess(Map<String, Object> profile) {
						String name = _resolveName(profile);
						Object iconObj = profile.get("icon");
						String iconUrl = iconObj != null ? String.valueOf(iconObj) : "";
						runOnUiThread(() -> {
							tv_sell_buyer_name.setText(name);
							_applyAvatar(name, iconUrl);
						});
					}
					@Override
					public void onError(String error) {
						runOnUiThread(() -> tv_sell_buyer_name.setText("Trader"));
					}
				});
				
				TrustScoreEngine.compute(getApplicationContext(), buyerUid, new TrustScoreEngine.Callback() {
					@Override
					public void onComplete(TrustScoreEngine.Result result) {
						runOnUiThread(() -> {
							iv_sell_buyer_verified.setVisibility(result.verified ? View.VISIBLE : View.GONE);
						});
					}
					@Override
					public void onError(String error) { }
				});
				com.harvana.app.trust.NetworkTrust.wallet(buyerAddress, score -> runOnUiThread(() ->
					tv_sell_buyer_trust.setText("Trust " + score.label())));
				
				P2PTradeStatsManager.getInstance(getApplicationContext()).getStats(buyerUid, new P2PTradeStatsManager.StatsCallback() {
					@Override
					public void onSuccess(int totalTrades, int successfulTrades, double successRatePercent) {
						runOnUiThread(() -> tv_sell_buyer_trades.setText(totalTrades + " trades"));
					}
					@Override
					public void onError(String error) { }
				});
			}
			@Override
			public void onNotFound() {
				runOnUiThread(() -> tv_sell_buyer_name.setText("Trader"));
			}
			@Override
			public void onError(String error) {
				runOnUiThread(() -> tv_sell_buyer_name.setText("Trader"));
			}
		});
	}
	
	private String _resolveName(Map<String, Object> profile) {
		Object first = profile.get("first_name");
		Object last = profile.get("last_name");
		String firstStr = first != null ? String.valueOf(first).trim() : "";
		String lastStr = last != null ? String.valueOf(last).trim() : "";
		return !firstStr.isEmpty()
		? (lastStr.isEmpty() ? firstStr : firstStr + " " + lastStr.substring(0, 1).toUpperCase(Locale.US) + ".")
		: "Trader";
	}
	
	private void _applyAvatar(String name, String iconUrl) {
		if (iconUrl != null && iconUrl.startsWith("http")) {
			tv_sell_buyer_initial.setVisibility(View.GONE);
			iv_sell_buyer_avatar.setVisibility(View.VISIBLE);
			Glide.with(getApplicationContext()).load(Uri.parse(iconUrl)).circleCrop().into(iv_sell_buyer_avatar);
		} else {
			iv_sell_buyer_avatar.setVisibility(View.GONE);
			tv_sell_buyer_initial.setVisibility(View.VISIBLE);
			tv_sell_buyer_initial.setText(name.length() > 0 ? name.substring(0, 1).toUpperCase(Locale.US) : "?");
		}
	}
	
	// ── The seller's own receiving account for this order (for their own confirmation, not payment instructions) ──
	
	private void _loadOwnPaymentDetails() {
		P2PPaymentDetailsManager.getInstance(getApplicationContext()).getOrderPaymentDetails(orderId, new P2PPaymentDetailsManager.DocCallback() {
			@Override
			public void onSuccess(Map<String, Object> data) {
				Object priceObj = null; // unitPrice isn't stored here; resolved via listing below
				runOnUiThread(() -> {
					Object identifier = data.get("accountIdentifier");
					Object primary = data.get("primaryField");
					tv_sell_payment_reference.setText(orderId);
					if (primary != null && identifier != null) {
						tv_sell_payment_status_description.setText("Buyer is paying into your " + primary + " account ending " + _lastFour(String.valueOf(identifier)) + ".");
					}
				});
			}
			@Override
			public void onError(String error) {
				runOnUiThread(() -> tv_sell_payment_status_description.setText("Couldn't load your payment account for this order."));
			}
		});
		
		if (!listingId.isEmpty()) {
			P2PPaymentDetailsManager.getInstance(getApplicationContext()).get(listingId, new P2PPaymentDetailsManager.DocCallback() {
				@Override
				public void onSuccess(Map<String, Object> data) {
					Object priceObj = data.get("unitPrice");
					unitPrice = priceObj instanceof Number ? ((Number) priceObj).doubleValue() : -1;
					Object currencyObj = data.get("currency");
					if (currencyObj != null && !String.valueOf(currencyObj).trim().isEmpty()) {
						listingCurrency = String.valueOf(currencyObj).trim();
					}
					runOnUiThread(() -> _renderValueCard());
				}
				@Override
				public void onError(String error) { }
			});
		}
	}
	
	private static String _lastFour(String s) {
		return s.length() > 4 ? s.substring(s.length() - 4) : s;
	}
	
	private void _renderValueCard() {
		double claimedHarv = HarvanaNodeClient.toHarvDisplay(amountRaw > 0 ? amountRaw : netAmountToBuyerRaw);
		double feeHarv = HarvanaNodeClient.toHarvDisplay(platformFeeRaw);
		tv_sell_harv_amount.setText(BalanceFormatter.smart(claimedHarv));
		tv_sell_value_caption.setText(("You're providing this " + com.harvana.app.CoinInfo.ticker()));
		String ccy = listingCurrency != null ? listingCurrency : com.harvana.app.p2p.P2pCurrencyFilterState.getCurrency();
		tv_sell_fiat_amount.setText(unitPrice >= 0 ? P2pMoney.fiat(claimedHarv * unitPrice, ccy) : "—");
		tv_sell_price.setText(unitPrice >= 0 ? P2pMoney.perHarv(unitPrice, ccy) : "—");
		tv_sell_maker_fee.setText(String.format(Locale.US, ("-%s " + com.harvana.app.CoinInfo.ticker()), BalanceFormatter.smartDecimal(feeHarv)));
		tv_sell_payment_received_amount.setText(unitPrice >= 0 ? P2pMoney.fiat(claimedHarv * unitPrice, ccy) : "—");
	}
	
	// ── Status-driven rendering ──────────────────────────────────────────
	
	private void _renderStatus() {
		card_sell_action.setVisibility(View.GONE);
		card_sell_action_button.setVisibility(View.GONE);
		card_sell_warning.setVisibility(View.GONE);
		card_sell_completed.setVisibility(View.GONE);
		if (tickRunnable != null) tickHandler.removeCallbacks(tickRunnable);
		
		switch (status) {
			case "AWAITING_PAYMENT":
			_setStatusBadge("Awaiting Payment");
			card_sell_action.setVisibility(View.VISIBLE);
			tv_sell_action_title.setText("Waiting for the buyer to pay");
			tv_sell_action_description.setText(("Your " + com.harvana.app.CoinInfo.ticker() + " is already locked in escrow. Nothing to do until they mark payment sent."));
			tv_sell_payment_status.setText("Not yet paid");
			layout_sell_payment_received.setVisibility(View.GONE);
			break;
			
			case "PAYMENT_SUBMITTED":
			_setStatusBadge("Payment Submitted");
			card_sell_action.setVisibility(View.VISIBLE);
			tv_sell_action_title.setText("Confirm you've received payment");
			tv_sell_action_description.setText(("Check your account, then confirm below. " + com.harvana.app.CoinInfo.ticker() + " only releases after your confirmation."));
			tv_sell_payment_status.setText("Buyer marked as paid");
			layout_sell_payment_received.setVisibility(View.VISIBLE);
			card_sell_action_button.setVisibility(View.VISIBLE);
			cb_sell_payment_confirm.setChecked(false);
			btn_sell_confirm_payment.setEnabled(false);
			_startCountdown(markedPaidTimestamp, 45);
			break;
			
			case "COMPLETED":
			_setStatusBadge("Completed");
			card_sell_completed.setVisibility(View.VISIBLE);
			tv_sell_completed_title.setText("Trade completed");
			tv_sell_completed_description.setText(("You confirmed payment and the " + com.harvana.app.CoinInfo.ticker() + " has been released to the buyer."));
			tv_sell_completed_tx_hash.setText(releaseTxHashValue.isEmpty() ? "—" : releaseTxHashValue);
			tv_sell_release_status.setText("Verified");
			tv_sell_release_tx_hash.setText(releaseTxHashValue.isEmpty() ? "—" : releaseTxHashValue);
			break;
			
			case "DISPUTED":
			_setStatusBadge("Disputed");
			card_sell_warning.setVisibility(View.VISIBLE);
			tv_sell_warning.setText("This trade is under dispute review. Settlement is paused until a guardian resolves it.");
			break;
			
			case "CANCELLED":
			_setStatusBadge("Cancelled");
			card_sell_warning.setVisibility(View.VISIBLE);
			tv_sell_warning.setText(("This order was cancelled. No " + com.harvana.app.CoinInfo.ticker() + " moved."));
			break;
			
			default:
			_setStatusBadge(status);
			break;
		}
		
		_renderTimeline();
	}
	
	private void _renderTimeline() {
		boolean escrowDone = !escrowTxHashValue.isEmpty();
		boolean paymentMarked = "PAYMENT_SUBMITTED".equals(status) || "COMPLETED".equals(status) || "DISPUTED".equals(status);
		boolean completed = "COMPLETED".equals(status);
		
		_setStep(tv_sell_step_1, true);
		_setStep(tv_sell_step_2, escrowDone);
		_setStep(tv_sell_step_3, paymentMarked);
		_setStep(tv_sell_step_4, completed);
		_setStep(tv_sell_step_5, completed);
		_setStep(tv_sell_step_6, completed);
		
		tv_sell_escrow_badge.setText(escrowDone ? "Locked" : "Pending");
		tv_sell_escrow_tx_hash.setText(escrowDone ? escrowTxHashValue : "—");
	}
	
	private void _setStep(TextView dot, boolean done) {
		dot.setText(done ? "✓" : "○");
		dot.setTextColor(androidx.core.content.ContextCompat.getColor(this, done ? R.color.colorSuccess : R.color.colorTextTertiary));
	}
	
	private void _startCountdown(long fromTimestamp, int minutes) {
		countdownFired = false;
		if (fromTimestamp <= 0) { tv_sell_timer.setText(""); return; }
		long deadline = fromTimestamp + (minutes * 60_000L);
		tickRunnable = new Runnable() {
			@Override
			public void run() {
				long remaining = deadline - System.currentTimeMillis();
				if (remaining <= 0) {
					if (!countdownFired) {
						countdownFired = true;
						tv_sell_timer.setText("Time expired");
					}
					return;
				}
				long min = remaining / 60000;
				long sec = (remaining % 60000) / 1000;
				tv_sell_timer.setText(String.format(Locale.US, "%02d:%02d remaining", min, sec));
				tickHandler.postDelayed(this, 1000);
			}
		};
		tickHandler.post(tickRunnable);
	}
	
	// ── Actions ──────────────────────────────────────────────────────────
	
	private void _confirmReceived() {
		ThemedDialogs.show(ThemedDialogs.alert(this)
		.setTitle("Confirm payment received")
		.setMessage(("Only confirm if you have genuinely verified the money is in your account. This releases the escrowed " + com.harvana.app.CoinInfo.ticker() + " to the buyer immediately and cannot be undone."))
		.setPositiveButton("Confirm & Release", (dialog, which) -> {
			double claimed = HarvanaNodeClient.toHarvDisplay(amountRaw > 0 ? amountRaw : netAmountToBuyerRaw);
			String ccy = listingCurrency == null ? "NGN" : listingCurrency;
			java.util.ArrayList<String[]> rows = new java.util.ArrayList<>();
			rows.add(new String[]{"Releases", BalanceFormatter.smart(claimed)});
			rows.add(new String[]{"To", buyerAddress == null || buyerAddress.isEmpty() ? "The buyer" : buyerAddress});
			rows.add(new String[]{"You were paid", unitPrice >= 0 ? P2pMoney.fiat(claimed * unitPrice, ccy) : "The agreed fiat"});
			rows.add(new String[]{"For", "You confirmed the fiat is in your account. This cannot be undone."});
			com.harvana.app.group.PaymentConfirmSheet.reviewThenAuthorize(this,
			"Release escrow", BalanceFormatter.smart(claimed), rows, () -> {
				btn_sell_confirm_payment.setEnabled(false);
				btn_sell_confirm_payment.setText("Releasing…");
				P2PTradeRouter.confirmReceived(getApplicationContext(), orderId, myAddress, new P2PTradeRouter.Callback() {
					@Override
					public void onSuccess(JSONObject result) {
						runOnUiThread(() -> _loadOrder());
					}
					@Override
					public void onError(String error) {
						runOnUiThread(() -> {
							btn_sell_confirm_payment.setEnabled(true);
							btn_sell_confirm_payment.setText("Confirm Payment Received");
							Toast.makeText(P2pSellTradeActivity.this, "Couldn't confirm: " + error, Toast.LENGTH_LONG).show();
						});
					}
				});
			});
		})
		.setNegativeButton("Cancel", null));
	}
	
	private void _showDisputeDialog() {
		EditText input = new EditText(this);
		input.setHint("What went wrong?");
		input.setTextColor(ThemedDialogs.onSurface(this));
		input.setHintTextColor(ThemedDialogs.onSurfaceVariant(this));
		int pad = (int) (16 * getResources().getDisplayMetrics().density);
		input.setPadding(pad, pad, pad, pad);
		ThemedDialogs.show(ThemedDialogs.alert(this)
		.setTitle("Raise a dispute")
		.setMessage("A guardian will review this trade.")
		.setView(input)
		.setPositiveButton("Submit", (dialog, which) -> {
			String reason = input.getText().toString().trim();
			if (reason.isEmpty()) reason = "No reason provided";
			String finalReason = reason;
			PinPromptHelper.authorize(this, "Raise a dispute on this trade", () -> {
				P2PTradeRouter.raiseDispute(getApplicationContext(), orderId, myAddress, finalReason, new P2PTradeRouter.Callback() {
					@Override
					public void onSuccess(JSONObject result) {
						runOnUiThread(() -> _loadOrder());
					}
					@Override
					public void onError(String error) {
						runOnUiThread(() -> Toast.makeText(P2pSellTradeActivity.this, "Couldn't raise dispute: " + error, Toast.LENGTH_LONG).show());
					}
				});
			});
		})
		.setNegativeButton("Cancel", null));
	}
	
	private static String _shortAddr(String address) {
		if (address == null || address.length() < 12) return address != null ? address : "";
		return address.substring(0, 8) + "…" + address.substring(address.length() - 4);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (tickRunnable != null) tickHandler.removeCallbacks(tickRunnable);
	}
	
	{
	}
	
}
