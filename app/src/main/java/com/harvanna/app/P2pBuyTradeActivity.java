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
import com.harvanna.app.firestore.P2PPaymentDetailsManager;
import com.harvanna.app.firestore.P2PTradeStatsManager;
import com.harvanna.app.firestore.UserProfileManager;
import com.harvanna.app.sync.HarvannaNodeClient;
import com.harvanna.app.tx.P2PTradeRouter;
import com.harvanna.app.wallet.BalanceFormatter;
import com.harvanna.app.wallet.PinPromptHelper;
import com.harvanna.app.wallet.WalletManager;
import com.harvanna.app.p2p.P2pMoney;
import com.harvanna.app.p2p.ThemedDialogs;
import com.google.firebase.auth.FirebaseAuth;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;


public class P2pBuyTradeActivity extends AppCompatActivity {
	
	private Toolbar _toolbar;
	private AppBarLayout _app_bar;
	private CoordinatorLayout _coordinator;
	
	private LinearLayout trade_root;
	private MaterialCardView action_status_card;
	private MaterialCardView value_card;
	private MaterialCardView participants_card;
	private MaterialCardView escrow_card;
	private TextView payment_heading;
	private MaterialCardView payment_card;
	private MaterialCardView payment_warning;
	private MaterialCardView proof_card;
	private MaterialButton primary_action;
	private MaterialCardView timeline_card;
	private MaterialCardView blockchain_card;
	private MaterialCardView security_card;
	private LinearLayout support_actions;
	private MaterialCardView completed_card;
	private LinearLayout action_status_content;
	private LinearLayout linear80;
	private ProgressBar trade_progress;
	private LinearLayout linear81;
	private TextView countdown;
	private TextView current_action_title;
	private TextView current_action_description;
	private LinearLayout linear82;
	private TextView value_title;
	private TextView harv_amount_large;
	private TextView fiat_amount_large;
	private TextView value_direction;
	private View view83;
	private LinearLayout value_details;
	private LinearLayout linear84;
	private LinearLayout maker_fee_row;
	private LinearLayout network_fee_row;
	private TextView textview42;
	private TextView unit_price;
	private TextView textview43;
	private TextView maker_fee;
	private TextView textview44;
	private TextView network_fee;
	private LinearLayout linear85;
	private TextView participants_title;
	private LinearLayout seller_row;
	private LinearLayout buyer_row;
	private FrameLayout frame_layout86;
	private LinearLayout linear87;
	private TextView seller_role;
	private ImageView seller_avatar;
	private TextView seller_initial;
	private TextView seller_name;
	private TextView seller_meta;
	private FrameLayout frame_layout88;
	private LinearLayout linear89;
	private TextView buyer_role;
	private ImageView buyer_avatar;
	private TextView buyer_initial;
	private TextView buyer_name;
	private TextView buyer_meta;
	private LinearLayout linear90;
	private LinearLayout linear91;
	private TextView escrow_description;
	private LinearLayout escrow_transaction_row;
	private TextView escrow_title;
	private TextView escrow_state;
	private TextView escrow_tx_label;
	private TextView escrow_tx_hash;
	private LinearLayout linear92;
	private TextView bank_name;
	private TextView account_number;
	private TextView account_name;
	private LinearLayout payment_amount_box;
	private TextView payment_reference;
	private TextView textview45;
	private TextView payment_amount;
	private TextView payment_warning_text;
	private LinearLayout linear93;
	private TextView proof_title;
	private TextView proof_description;
	private FrameLayout proof_picker;
	private ImageView proof_preview;
	private LinearLayout proof_placeholder;
	private TextView proof_placeholder_title;
	private TextView proof_placeholder_action;
	private LinearLayout timeline;
	private TextView timeline_title;
	private LinearLayout step_order;
	private LinearLayout step_escrow;
	private LinearLayout step_payment;
	private LinearLayout step_seller;
	private LinearLayout step_release;
	private LinearLayout step_complete;
	private TextView step_order_dot;
	private LinearLayout linear94;
	private TextView step_order_title;
	private TextView step_order_time;
	private TextView step_escrow_dot;
	private LinearLayout linear95;
	private TextView step_escrow_title;
	private TextView step_escrow_status;
	private TextView step_payment_dot;
	private LinearLayout linear96;
	private TextView step_payment_title;
	private TextView step_payment_status;
	private TextView step_seller_dot;
	private LinearLayout linear97;
	private TextView step_seller_title;
	private TextView step_seller_status;
	private TextView step_release_dot;
	private LinearLayout linear98;
	private TextView step_release_title;
	private TextView step_release_status;
	private TextView step_complete_dot;
	private LinearLayout linear99;
	private TextView step_complete_title;
	private TextView step_complete_status;
	private LinearLayout linear100;
	private LinearLayout linear101;
	private TextView release_tx_label;
	private TextView release_tx_hash;
	private LinearLayout blockchain_details;
	private TextView blockchain_title;
	private TextView blockchain_status;
	private TextView release_network;
	private TextView release_confirmation;
	private LinearLayout linear102;
	private TextView security_title;
	private TextView security_text;
	private MaterialButton dispute_button;
	private TextView support_text;
	private TextView support_button;
	private LinearLayout linear103;
	private ImageView completed_icon;
	private TextView completed_title;
	private TextView completed_description;
	private TextView completed_transaction;
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.p2p_buy_trade);
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
		trade_root = findViewById(R.id.trade_root);
		action_status_card = findViewById(R.id.action_status_card);
		value_card = findViewById(R.id.value_card);
		participants_card = findViewById(R.id.participants_card);
		escrow_card = findViewById(R.id.escrow_card);
		payment_heading = findViewById(R.id.payment_heading);
		payment_card = findViewById(R.id.payment_card);
		payment_warning = findViewById(R.id.payment_warning);
		proof_card = findViewById(R.id.proof_card);
		primary_action = findViewById(R.id.primary_action);
		timeline_card = findViewById(R.id.timeline_card);
		blockchain_card = findViewById(R.id.blockchain_card);
		security_card = findViewById(R.id.security_card);
		support_actions = findViewById(R.id.support_actions);
		completed_card = findViewById(R.id.completed_card);
		action_status_content = findViewById(R.id.action_status_content);
		linear80 = findViewById(R.id.linear80);
		trade_progress = findViewById(R.id.trade_progress);
		linear81 = findViewById(R.id.linear81);
		countdown = findViewById(R.id.countdown);
		current_action_title = findViewById(R.id.current_action_title);
		current_action_description = findViewById(R.id.current_action_description);
		linear82 = findViewById(R.id.linear82);
		value_title = findViewById(R.id.value_title);
		harv_amount_large = findViewById(R.id.harv_amount_large);
		fiat_amount_large = findViewById(R.id.fiat_amount_large);
		value_direction = findViewById(R.id.value_direction);
		view83 = findViewById(R.id.view83);
		value_details = findViewById(R.id.value_details);
		linear84 = findViewById(R.id.linear84);
		maker_fee_row = findViewById(R.id.maker_fee_row);
		network_fee_row = findViewById(R.id.network_fee_row);
		textview42 = findViewById(R.id.textview42);
		unit_price = findViewById(R.id.unit_price);
		textview43 = findViewById(R.id.textview43);
		maker_fee = findViewById(R.id.maker_fee);
		textview44 = findViewById(R.id.textview44);
		network_fee = findViewById(R.id.network_fee);
		linear85 = findViewById(R.id.linear85);
		participants_title = findViewById(R.id.participants_title);
		seller_row = findViewById(R.id.seller_row);
		buyer_row = findViewById(R.id.buyer_row);
		frame_layout86 = findViewById(R.id.frame_layout86);
		linear87 = findViewById(R.id.linear87);
		seller_role = findViewById(R.id.seller_role);
		seller_avatar = findViewById(R.id.seller_avatar);
		seller_initial = findViewById(R.id.seller_initial);
		seller_name = findViewById(R.id.seller_name);
		seller_meta = findViewById(R.id.seller_meta);
		frame_layout88 = findViewById(R.id.frame_layout88);
		linear89 = findViewById(R.id.linear89);
		buyer_role = findViewById(R.id.buyer_role);
		buyer_avatar = findViewById(R.id.buyer_avatar);
		buyer_initial = findViewById(R.id.buyer_initial);
		buyer_name = findViewById(R.id.buyer_name);
		buyer_meta = findViewById(R.id.buyer_meta);
		linear90 = findViewById(R.id.linear90);
		linear91 = findViewById(R.id.linear91);
		escrow_description = findViewById(R.id.escrow_description);
		escrow_transaction_row = findViewById(R.id.escrow_transaction_row);
		escrow_title = findViewById(R.id.escrow_title);
		escrow_state = findViewById(R.id.escrow_state);
		escrow_tx_label = findViewById(R.id.escrow_tx_label);
		escrow_tx_hash = findViewById(R.id.escrow_tx_hash);
		linear92 = findViewById(R.id.linear92);
		bank_name = findViewById(R.id.bank_name);
		account_number = findViewById(R.id.account_number);
		account_name = findViewById(R.id.account_name);
		payment_amount_box = findViewById(R.id.payment_amount_box);
		payment_reference = findViewById(R.id.payment_reference);
		textview45 = findViewById(R.id.textview45);
		payment_amount = findViewById(R.id.payment_amount);
		payment_warning_text = findViewById(R.id.payment_warning_text);
		linear93 = findViewById(R.id.linear93);
		proof_title = findViewById(R.id.proof_title);
		proof_description = findViewById(R.id.proof_description);
		proof_picker = findViewById(R.id.proof_picker);
		proof_preview = findViewById(R.id.proof_preview);
		proof_placeholder = findViewById(R.id.proof_placeholder);
		proof_placeholder_title = findViewById(R.id.proof_placeholder_title);
		proof_placeholder_action = findViewById(R.id.proof_placeholder_action);
		timeline = findViewById(R.id.timeline);
		timeline_title = findViewById(R.id.timeline_title);
		step_order = findViewById(R.id.step_order);
		step_escrow = findViewById(R.id.step_escrow);
		step_payment = findViewById(R.id.step_payment);
		step_seller = findViewById(R.id.step_seller);
		step_release = findViewById(R.id.step_release);
		step_complete = findViewById(R.id.step_complete);
		step_order_dot = findViewById(R.id.step_order_dot);
		linear94 = findViewById(R.id.linear94);
		step_order_title = findViewById(R.id.step_order_title);
		step_order_time = findViewById(R.id.step_order_time);
		step_escrow_dot = findViewById(R.id.step_escrow_dot);
		linear95 = findViewById(R.id.linear95);
		step_escrow_title = findViewById(R.id.step_escrow_title);
		step_escrow_status = findViewById(R.id.step_escrow_status);
		step_payment_dot = findViewById(R.id.step_payment_dot);
		linear96 = findViewById(R.id.linear96);
		step_payment_title = findViewById(R.id.step_payment_title);
		step_payment_status = findViewById(R.id.step_payment_status);
		step_seller_dot = findViewById(R.id.step_seller_dot);
		linear97 = findViewById(R.id.linear97);
		step_seller_title = findViewById(R.id.step_seller_title);
		step_seller_status = findViewById(R.id.step_seller_status);
		step_release_dot = findViewById(R.id.step_release_dot);
		linear98 = findViewById(R.id.linear98);
		step_release_title = findViewById(R.id.step_release_title);
		step_release_status = findViewById(R.id.step_release_status);
		step_complete_dot = findViewById(R.id.step_complete_dot);
		linear99 = findViewById(R.id.linear99);
		step_complete_title = findViewById(R.id.step_complete_title);
		step_complete_status = findViewById(R.id.step_complete_status);
		linear100 = findViewById(R.id.linear100);
		linear101 = findViewById(R.id.linear101);
		release_tx_label = findViewById(R.id.release_tx_label);
		release_tx_hash = findViewById(R.id.release_tx_hash);
		blockchain_details = findViewById(R.id.blockchain_details);
		blockchain_title = findViewById(R.id.blockchain_title);
		blockchain_status = findViewById(R.id.blockchain_status);
		release_network = findViewById(R.id.release_network);
		release_confirmation = findViewById(R.id.release_confirmation);
		linear102 = findViewById(R.id.linear102);
		security_title = findViewById(R.id.security_title);
		security_text = findViewById(R.id.security_text);
		dispute_button = findViewById(R.id.dispute_button);
		support_text = findViewById(R.id.support_text);
		support_button = findViewById(R.id.support_button);
		linear103 = findViewById(R.id.linear103);
		completed_icon = findViewById(R.id.completed_icon);
		completed_title = findViewById(R.id.completed_title);
		completed_description = findViewById(R.id.completed_description);
		completed_transaction = findViewById(R.id.completed_transaction);
	}
	
	private void initializeLogic() {
		androidx.appcompat.app.ActionBar ab = getSupportActionBar();
		int backgroundColor = androidx.core.content.ContextCompat.getColor(this, R.color.colorBackground);
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			final Window window = this.getWindow();
			window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
			window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
			com.harvanna.app.utils.StatusBarUtil.matchToolbar(this);
		}
		
		if (getSupportActionBar() != null) {
			getSupportActionBar().setTitle("P2P Trade");
			getSupportActionBar().setSubtitle(("Order #" + com.harvanna.app.CoinInfo.ticker() + "-8F42D1"));
		}
		
		
		orderId = getIntent().getStringExtra("orderId");
		myAddress = WalletManager.getActiveWalletAddress(this);
		if (getSupportActionBar() != null) getSupportActionBar().setTitle("Buy Trade");
		
		imagePicker = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
			if (uri == null) return;
			proof_placeholder.setVisibility(View.GONE);
			proof_preview.setVisibility(View.VISIBLE);
			Glide.with(this).load(uri).into(proof_preview);
			// GROUND-UP, honestly scoped: this previews locally only.
			// There's no Firestore field defined yet for a persisted
			// payment-proof URL, and inventing one without a real place
			// for the seller to see it would just be UI theater —
			// flagged here rather than faking a successful upload.
		});
		proof_picker.setOnClickListener(v -> imagePicker.launch("image/*"));
		
		dispute_button.setOnClickListener(v -> _showDisputeDialog());
		if (support_button != null) {
			support_button.setOnClickListener(v -> Toast.makeText(this,
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
	private long releaseGasRaw = 0;
	private long paymentClaimedTimestamp = 0;
	private long markedPaidTimestamp = 0;
	private String escrowTxHashValue = "";
	private String releaseTxHashValue = "";
	
	private final Handler tickHandler = new Handler(Looper.getMainLooper());
	private Runnable tickRunnable;
	private ActivityResultLauncher<String> imagePicker;
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Create the status indicator text view
		TextView expressStatus = new TextView(this);
		expressStatus.setText("PAYMENT PENDING");
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
	
	private void _setStatusBadge(String label) {
		if (getSupportActionBar() == null) return;
		getSupportActionBar().setSubtitle(orderReferenceLabel.isEmpty() ? label : orderReferenceLabel + " · " + label);
	}
	
	private void _showFatalError(String message) {
		trade_root.removeAllViews();
		TextView tv = new TextView(this);
		tv.setText(message);
		tv.setPadding(48, 96, 48, 48);
		trade_root.addView(tv);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		if (orderId != null && !orderId.isEmpty()) _loadOrder();
	}
	
	// ── Load order + render ──────────────────────────────────────────────
	
	private void _loadOrder() {
		HarvannaNodeClient.getP2POrder(orderId, new HarvannaNodeClient.JsonCallback() {
			@Override
			public void onSuccess(JSONObject order) {
				listingId = order.optString("listingId", "");
				buyerAddress = order.optString("buyerAddress", "");
				sellerAddress = order.optString("sellerAddress", "");
				status = order.optString("status", "");
				netAmountToBuyerRaw = order.optLong("netAmountToBuyer", order.optLong("buyerEntitlementRaw", order.optLong("amount", 0)));
				amountRaw = order.optLong("amount", netAmountToBuyerRaw);
				platformFeeRaw = order.optLong("platformFeeRaw", 0);
				releaseGasRaw = order.optLong("releaseGasRaw", 0);
				paymentClaimedTimestamp = order.optLong("paymentClaimedTimestamp", 0);
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
					_renderStatus();
				});
				
				_loadSellerAndPaymentDetails();
				_loadBuyerIdentity();
			}
			@Override
			public void onError(String error) {
				runOnUiThread(() -> _showFatalError("Couldn't load this trade: " + error));
			}
		});
	}
	
	private void _loadSellerAndPaymentDetails() {
		P2PPaymentDetailsManager mgr = P2PPaymentDetailsManager.getInstance(getApplicationContext());
		mgr.getOrderPaymentDetails(orderId, new P2PPaymentDetailsManager.DocCallback() {
			@Override
			public void onSuccess(Map<String, Object> data) {
				Object sellerUidObj = data.get("sellerUid");
				String sellerUid = sellerUidObj != null ? String.valueOf(sellerUidObj) : "";
				runOnUiThread(() -> {
					P2PPaymentDetailsManager.bindAccount(data, bank_name, account_number, account_name);
					payment_reference.setText(orderId);
				});
				_loadListingPriceAndSeller(sellerUid, false);
			}
			@Override
			public void onError(String error) {
				_loadListingPriceAndSeller("", true);
			}
		});
	}
	
	private void _loadListingPriceAndSeller(String preferredSellerUid, boolean bindPaymentFromListing) {
		if (listingId.isEmpty()) return;
		P2PPaymentDetailsManager.getInstance(getApplicationContext()).get(listingId, new P2PPaymentDetailsManager.DocCallback() {
			@Override
			public void onSuccess(Map<String, Object> data) {
				Object priceObj = data.get("unitPrice");
				unitPrice = priceObj instanceof Number ? ((Number) priceObj).doubleValue() : -1;
				Object currencyObj = data.get("currency");
				if (currencyObj != null && !String.valueOf(currencyObj).trim().isEmpty()) {
					listingCurrency = String.valueOf(currencyObj).trim();
				}
				Object sellerUidObj = data.get("sellerUid");
				String sellerUid = preferredSellerUid != null && !preferredSellerUid.isEmpty()
				? preferredSellerUid
				: (sellerUidObj != null ? String.valueOf(sellerUidObj) : "");
				
				runOnUiThread(() -> {
					_renderValueCard();
					if (bindPaymentFromListing) _bindFirstPaymentMethod(data);
				});
				
				if (sellerUid.isEmpty()) { runOnUiThread(() -> seller_name.setText("Trader")); return; }
				
				UserProfileManager.getInstance(getApplicationContext()).getProfile(sellerUid, new UserProfileManager.DocCallback() {
					@Override
					public void onSuccess(Map<String, Object> profile) {
						String name = _resolveName(profile);
						Object iconObj = profile.get("icon");
						String iconUrl = iconObj != null ? String.valueOf(iconObj) : "";
						runOnUiThread(() -> {
							seller_name.setText(name);
							seller_meta.setText(_shortAddr(sellerAddress));
							_applyAvatar(name, iconUrl, seller_avatar, seller_initial);
							com.harvanna.app.trust.NetworkTrust.wallet(sellerAddress, score -> runOnUiThread(() -> {
								String line = _shortAddr(sellerAddress);
								if (score.published) line = line + " · " + score.score + " / 26";
								seller_meta.setText(line);
							}));
						});
					}
					@Override
					public void onError(String error) {
						runOnUiThread(() -> seller_name.setText("Trader"));
					}
				});
			}
			@Override
			public void onError(String error) {
				runOnUiThread(() -> {
					seller_name.setText("Trader");
					unit_price.setText("—");
					if (bindPaymentFromListing) {
						bank_name.setText("—");
						account_number.setText("—");
						account_name.setText("—");
					}
				});
			}
		});
	}
	
	private void _loadBuyerIdentity() {
		String myUid = FirebaseAuth.getInstance().getCurrentUser() != null
		? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
		final String shortBuyer = _shortAddr(buyerAddress);
		runOnUiThread(() -> buyer_meta.setText(shortBuyer));
		if (myUid == null) { runOnUiThread(() -> buyer_name.setText("Trader")); return; }
		UserProfileManager.getInstance(getApplicationContext()).getProfile(myUid, new UserProfileManager.DocCallback() {
			@Override
			public void onSuccess(Map<String, Object> profile) {
				String name = _resolveName(profile);
				Object iconObj = profile.get("icon");
				String iconUrl = iconObj != null ? String.valueOf(iconObj) : "";
				runOnUiThread(() -> {
					buyer_name.setText(name);
					_applyAvatar(name, iconUrl, buyer_avatar, buyer_initial);
				});
			}
			@Override
			public void onError(String error) {
				runOnUiThread(() -> buyer_name.setText("Trader"));
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
	
	private void _applyAvatar(String name, String iconUrl, ImageView avatarView, TextView initialView) {
		if (iconUrl != null && iconUrl.startsWith("http")) {
			initialView.setVisibility(View.GONE);
			avatarView.setVisibility(View.VISIBLE);
			Glide.with(getApplicationContext()).load(Uri.parse(iconUrl)).circleCrop().into(avatarView);
		} else {
			avatarView.setVisibility(View.GONE);
			initialView.setVisibility(View.VISIBLE);
			initialView.setText(name.length() > 0 ? name.substring(0, 1).toUpperCase(Locale.US) : "?");
		}
	}
	
	private void _bindFirstPaymentMethod(Map<String, Object> data) {
		P2PPaymentDetailsManager.bindAccount(data, bank_name, account_number, account_name);
		payment_reference.setText(orderId);
	}
	
	private void _renderValueCard() {
		double claimedHarv = HarvannaNodeClient.toHarvDisplay(amountRaw > 0 ? amountRaw : netAmountToBuyerRaw);
		double feeHarv = HarvannaNodeClient.toHarvDisplay(platformFeeRaw);
		double networkFeeHarv = HarvannaNodeClient.toHarvDisplay(releaseGasRaw);
		harv_amount_large.setText(BalanceFormatter.smart(claimedHarv));
		String ccy = listingCurrency != null ? listingCurrency : com.harvanna.app.p2p.P2pCurrencyFilterState.getCurrency();
		fiat_amount_large.setText(unitPrice >= 0 ? com.harvanna.app.p2p.P2pMoney.fiat(claimedHarv * unitPrice, ccy) : "—");
		unit_price.setText(unitPrice >= 0 ? com.harvanna.app.p2p.P2pMoney.perHarv(unitPrice, ccy) : "—");
		maker_fee.setText(String.format(Locale.US, ("-%s " + com.harvanna.app.CoinInfo.ticker()), BalanceFormatter.smartDecimal(feeHarv)));
		network_fee.setText(String.format(Locale.US, ("-%s " + com.harvanna.app.CoinInfo.ticker()), BalanceFormatter.smartDecimal(networkFeeHarv)));
		payment_amount.setText(unitPrice >= 0 ? com.harvanna.app.p2p.P2pMoney.fiat(claimedHarv * unitPrice, ccy) : "—");
	}
	
	// ── Status-driven rendering ──────────────────────────────────────────
	
	private void _renderStatus() {
		_hideAllSections();
		
		switch (status) {
			case "AWAITING_PAYMENT":
			_setStatusBadge("Awaiting Payment");
			current_action_title.setText("Pay the seller");
			current_action_description.setText("Send the exact fiat amount below, then confirm payment.");
			action_status_card.setVisibility(View.VISIBLE);
			escrow_card.setVisibility(View.VISIBLE);
			payment_card.setVisibility(View.VISIBLE);
			proof_card.setVisibility(View.VISIBLE);
			timeline_card.setVisibility(View.VISIBLE);
			security_card.setVisibility(View.VISIBLE);
			primary_action.setVisibility(View.VISIBLE);
			primary_action.setText("I've Made Payment");
			primary_action.setEnabled(true);
			primary_action.setOnClickListener(v -> _confirmMarkPaid());
			dispute_button.setVisibility(myAddress.equals(buyerAddress) ? View.VISIBLE : View.GONE);
			_startCountdown(paymentClaimedTimestamp, 45);
			break;
			
			case "PAYMENT_SUBMITTED":
			_setStatusBadge("Payment Submitted");
			current_action_title.setText("Waiting for seller to confirm receipt");
			current_action_description.setText("The seller has 45 minutes to confirm your payment before this auto-escalates for review.");
			action_status_card.setVisibility(View.VISIBLE);
			escrow_card.setVisibility(View.VISIBLE);
			payment_card.setVisibility(View.VISIBLE);
			timeline_card.setVisibility(View.VISIBLE);
			security_card.setVisibility(View.VISIBLE);
			primary_action.setVisibility(View.GONE);
			dispute_button.setVisibility(View.VISIBLE);
			_startCountdown(markedPaidTimestamp, 45);
			break;
			
			case "COMPLETED":
			_setStatusBadge("Completed");
			completed_card.setVisibility(View.VISIBLE);
			completed_title.setText("Trade completed");
			completed_description.setText(("The seller confirmed your payment and the " + com.harvanna.app.CoinInfo.ticker() + " has been released to your wallet."));
			completed_transaction.setText(releaseTxHashValue.isEmpty() ? "—" : releaseTxHashValue);
			blockchain_card.setVisibility(View.VISIBLE);
			blockchain_status.setText("Verified");
			release_tx_hash.setText(releaseTxHashValue.isEmpty() ? "—" : releaseTxHashValue);
			timeline_card.setVisibility(View.VISIBLE);
			break;
			
			case "DISPUTED":
			_setStatusBadge("Disputed");
			current_action_title.setText("This trade is under dispute review");
			current_action_description.setText("A guardian will review this trade. Settlement is paused until it's resolved.");
			action_status_card.setVisibility(View.VISIBLE);
			timeline_card.setVisibility(View.VISIBLE);
			security_card.setVisibility(View.VISIBLE);
			primary_action.setVisibility(View.GONE);
			dispute_button.setVisibility(View.GONE);
			break;
			
			case "CANCELLED":
			_setStatusBadge("Cancelled");
			current_action_title.setText("This order was cancelled");
			current_action_description.setText(("No " + com.harvanna.app.CoinInfo.ticker() + " moved for this order."));
			action_status_card.setVisibility(View.VISIBLE);
			timeline_card.setVisibility(View.VISIBLE);
			break;
			
			default:
			_setStatusBadge(status);
			action_status_card.setVisibility(View.VISIBLE);
			current_action_title.setText("Status: " + status);
			current_action_description.setText("");
			break;
		}
		
		_renderTimeline();
	}
	
	private void _hideAllSections() {
		action_status_card.setVisibility(View.GONE);
		escrow_card.setVisibility(View.GONE);
		payment_card.setVisibility(View.GONE);
		proof_card.setVisibility(View.GONE);
		blockchain_card.setVisibility(View.GONE);
		security_card.setVisibility(View.GONE);
		completed_card.setVisibility(View.GONE);
		primary_action.setVisibility(View.GONE);
		dispute_button.setVisibility(View.GONE);
		if (tickRunnable != null) tickHandler.removeCallbacks(tickRunnable);
	}
	
	private void _renderTimeline() {
		boolean escrowDone = !escrowTxHashValue.isEmpty();
		boolean paymentMarked = "PAYMENT_SUBMITTED".equals(status) || "COMPLETED".equals(status) || "DISPUTED".equals(status);
		boolean completed = "COMPLETED".equals(status);
		boolean disputed = "DISPUTED".equals(status);
		
		step_order_title.setText("Order created");
		_setStepDone(step_order_dot, true);
		
		step_escrow_status.setText(escrowDone ? "Locked" : "Pending");
		_setStepDone(step_escrow_dot, escrowDone);
		
		step_payment_status.setText(paymentMarked ? "Marked paid" : "Awaiting your payment");
		_setStepDone(step_payment_dot, paymentMarked);
		
		step_seller_status.setText(disputed ? "Disputed" : (completed ? "Confirmed" : "Waiting"));
		_setStepDone(step_seller_dot, completed);
		
		step_release_status.setText(completed ? "Released" : "Not yet");
		_setStepDone(step_release_dot, completed);
		
		step_complete_status.setText(completed ? "Completed" : "Not yet");
		_setStepDone(step_complete_dot, completed);
		
		escrow_state.setText(escrowDone ? "Locked" : "Pending");
		if (escrowDone) {
			escrow_transaction_row.setVisibility(View.VISIBLE);
			escrow_tx_hash.setText(escrowTxHashValue);
		} else {
			escrow_transaction_row.setVisibility(View.GONE);
		}
	}
	
	private void _setStepDone(TextView dot, boolean done) {
		dot.setText(done ? "✓" : "○");
		dot.setTextColor(done ? 0xFF1FD18C : 0xFF8A93A6);
	}
	
	// ── Countdown ────────────────────────────────────────────────────────
	
	private void _startCountdown(long fromTimestamp, int minutes) {
		countdownFired = false;
		if (fromTimestamp <= 0) { countdown.setText(""); return; }
		long deadline = fromTimestamp + (minutes * 60_000L);
		tickRunnable = new Runnable() {
			@Override
			public void run() {
				long remaining = deadline - System.currentTimeMillis();
				if (remaining <= 0) {
					if (!countdownFired) {
						countdownFired = true;
						countdown.setText("Time expired");
					}
					return;
				}
				long min = remaining / 60000;
				long sec = (remaining % 60000) / 1000;
				countdown.setText(String.format(Locale.US, "%02d:%02d remaining", min, sec));
				tickHandler.postDelayed(this, 1000);
			}
		};
		tickHandler.post(tickRunnable);
	}
	
	// ── Actions ──────────────────────────────────────────────────────────
	
	private void _confirmMarkPaid() {
		ThemedDialogs.show(ThemedDialogs.alert(this)
		.setTitle("Confirm payment sent")
		.setMessage("Only continue if you have actually sent the fiat payment to the seller's account shown above. Falsely marking this paid can suspend your account.")
		.setPositiveButton("I've Paid", (dialog, which) -> {
			PinPromptHelper.verifyPinThenProceed(this, () -> {
				primary_action.setEnabled(false);
				primary_action.setText("Submitting…");
				P2PTradeRouter.markOrderPaid(getApplicationContext(), orderId, myAddress, new P2PTradeRouter.Callback() {
					@Override
					public void onSuccess(JSONObject result) {
						runOnUiThread(() -> _loadOrder());
					}
					@Override
					public void onError(String error) {
						runOnUiThread(() -> {
							primary_action.setEnabled(true);
							primary_action.setText("I've Made Payment");
							Toast.makeText(P2pBuyTradeActivity.this, "Couldn't submit: " + error, Toast.LENGTH_LONG).show();
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
		.setMessage("A guardian will review this trade. Only do this if there's a genuine problem with payment.")
		.setView(input)
		.setPositiveButton("Submit", (dialog, which) -> {
			String reason = input.getText().toString().trim();
			if (reason.isEmpty()) reason = "No reason provided";
			String finalReason = reason;
			PinPromptHelper.verifyPinThenProceed(this, () -> {
				P2PTradeRouter.raiseDispute(getApplicationContext(), orderId, myAddress, finalReason, new P2PTradeRouter.Callback() {
					@Override
					public void onSuccess(JSONObject result) {
						runOnUiThread(() -> _loadOrder());
					}
					@Override
					public void onError(String error) {
						runOnUiThread(() -> Toast.makeText(P2pBuyTradeActivity.this, "Couldn't raise dispute: " + error, Toast.LENGTH_LONG).show());
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
