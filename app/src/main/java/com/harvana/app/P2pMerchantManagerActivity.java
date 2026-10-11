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
import android.widget.HorizontalScrollView;
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
import androidx.recyclerview.widget.*;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
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
import com.harvana.app.sync.HarvanaNodeClient;
import com.harvana.app.tx.P2PTradeRouter;
import com.harvana.app.wallet.BalanceFormatter;
import com.harvana.app.wallet.PinPromptHelper;
import com.harvana.app.wallet.WalletManager;
import com.harvana.app.p2p.P2pIds;
import com.harvana.app.p2p.P2pMoney;
import com.harvana.app.p2p.ThemedDialogs;


public class P2pMerchantManagerActivity extends AppCompatActivity {
	
	private Toolbar _toolbar;
	private AppBarLayout _app_bar;
	private CoordinatorLayout _coordinator;
	
	private LinearLayout merchant_manager_root;
	private MaterialButton btn_create_offer;
	private MaterialCardView card_manager_summary;
	private HorizontalScrollView manager_filter_scroll;
	private LinearLayout linear244;
	private RecyclerView recycler_merchant_trades;
	private LinearLayout layout_manager_empty;
	private MaterialCardView card_manager_info;
	private LinearLayout linear239;
	private TextView textview141;
	private LinearLayout linear240;
	private LinearLayout linear241;
	private LinearLayout linear242;
	private LinearLayout linear243;
	private TextView tv_summary_active;
	private TextView textview142;
	private TextView tv_summary_locked;
	private TextView textview143;
	private TextView tv_summary_completed;
	private TextView textview144;
	private LinearLayout manager_filter_container;
	private TextView filter_manager_all;
	private TextView filter_manager_active;
	private TextView filter_manager_matched;
	private TextView filter_manager_paused;
	private TextView filter_manager_closed;
	private TextView tv_manager_section_title;
	private TextView tv_manager_result_count;
	private TextView textview145;
	private TextView textview146;
	private LinearLayout linear245;
	private TextView textview147;
	private TextView tv_manager_info;
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.p2p_merchant_manager);
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
		merchant_manager_root = findViewById(R.id.merchant_manager_root);
		btn_create_offer = findViewById(R.id.btn_create_offer);
		card_manager_summary = findViewById(R.id.card_manager_summary);
		manager_filter_scroll = findViewById(R.id.manager_filter_scroll);
		linear244 = findViewById(R.id.linear244);
		recycler_merchant_trades = findViewById(R.id.recycler_merchant_trades);
		com.harvana.app.utils.Shimmer.hold(recycler_merchant_trades, com.harvana.app.utils.Shimmer.rows(recycler_merchant_trades.getContext(), 5), 4000, "P2pMerchantManagerActivity");
		layout_manager_empty = findViewById(R.id.layout_manager_empty);
		card_manager_info = findViewById(R.id.card_manager_info);
		linear239 = findViewById(R.id.linear239);
		textview141 = findViewById(R.id.textview141);
		linear240 = findViewById(R.id.linear240);
		linear241 = findViewById(R.id.linear241);
		linear242 = findViewById(R.id.linear242);
		linear243 = findViewById(R.id.linear243);
		tv_summary_active = findViewById(R.id.tv_summary_active);
		textview142 = findViewById(R.id.textview142);
		tv_summary_locked = findViewById(R.id.tv_summary_locked);
		textview143 = findViewById(R.id.textview143);
		tv_summary_completed = findViewById(R.id.tv_summary_completed);
		textview144 = findViewById(R.id.textview144);
		manager_filter_container = findViewById(R.id.manager_filter_container);
		filter_manager_all = findViewById(R.id.filter_manager_all);
		filter_manager_active = findViewById(R.id.filter_manager_active);
		filter_manager_matched = findViewById(R.id.filter_manager_matched);
		filter_manager_paused = findViewById(R.id.filter_manager_paused);
		filter_manager_closed = findViewById(R.id.filter_manager_closed);
		tv_manager_section_title = findViewById(R.id.tv_manager_section_title);
		tv_manager_result_count = findViewById(R.id.tv_manager_result_count);
		textview145 = findViewById(R.id.textview145);
		textview146 = findViewById(R.id.textview146);
		linear245 = findViewById(R.id.linear245);
		textview147 = findViewById(R.id.textview147);
		tv_manager_info = findViewById(R.id.tv_manager_info);
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
		
		adapter = new Recycler_merchant_offersAdapter(visible);
		recycler_merchant_trades.setLayoutManager(new LinearLayoutManager(this));
		recycler_merchant_trades.setAdapter(adapter);
		
		tv_manager_info.setText("To stop taking new orders, cancel the offer and post a new one later. Cancelling only returns what nobody has claimed; orders already open carry on.");
		filter_manager_paused.setVisibility(View.GONE);   // the node has no pause; the tab only ever showed nothing
		
		btn_create_offer.setOnClickListener(v -> _showCreateOfferChoice());
		
		_setFilter("ALL", filter_manager_all);
		filter_manager_all.setOnClickListener(v -> _setFilter("ALL", filter_manager_all));
		filter_manager_active.setOnClickListener(v -> _setFilter("ACTIVE", filter_manager_active));
		filter_manager_matched.setOnClickListener(v -> _setFilter("MATCHED", filter_manager_matched));
		filter_manager_paused.setOnClickListener(v -> _setFilter("PAUSED", filter_manager_paused));
		filter_manager_closed.setOnClickListener(v -> _setFilter("CLOSED", filter_manager_closed));
		
		_loadMine();
		
	}
	
	@Override
	public void onResume() {
		super.onResume();
		if (!allListings.isEmpty() || !allOrders.isEmpty()) _loadMine();
	}
	

	public void _MORE() {
	}
	private String myAddress = "";
	private final List<JSONObject> allListings = new ArrayList<>();
	private final List<JSONObject> allOrders = new ArrayList<>();
	private final List<JSONObject> visible = new ArrayList<>();
	private String activeFilter = "ALL";
	private Recycler_merchant_offersAdapter adapter;
	
	/** Create Offer: a curved bottom sheet with Sell and Buy side by side. */
	private void _showCreateOfferChoice() {
		com.google.android.material.bottomsheet.BottomSheetDialog sheet = ThemedDialogs.sheet(this);
		LinearLayout root = ThemedDialogs.sheetRoot(this);
		root.addView(ThemedDialogs.sheetTitle(this, "Create offer"));
		TextView sub = ThemedDialogs.sheetLabel(this, "Choose what you want to do with HARV.");
		root.addView(sub);
		
		LinearLayout row = new LinearLayout(this);
		row.setOrientation(LinearLayout.HORIZONTAL);
		row.setPadding(0, ThemedDialogs.dp(this, 16), 0, ThemedDialogs.dp(this, 12));
		row.addView(_choiceButton("Sell HARV", "I have HARV to sell", androidx.core.content.ContextCompat.getColor(this, R.color.colorError), v -> {
			sheet.dismiss();
			startActivity(new Intent(this, CreateP2pSellOfferActivity.class));
		}), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
		View gap = new View(this);
		row.addView(gap, new LinearLayout.LayoutParams(ThemedDialogs.dp(this, 14), 1));
		row.addView(_choiceButton("Buy HARV", "I want to acquire HARV", androidx.core.content.ContextCompat.getColor(this, R.color.colorSuccess), v -> {
			sheet.dismiss();
			startActivity(new Intent(this, CreateP2pBuyOfferActivity.class));
		}), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
		root.addView(row);
		
		sheet.setContentView(root);
		sheet.getBehavior().setSkipCollapsed(true);
		sheet.getBehavior().setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
		sheet.show();
	}
	
	/** A tall filled button with a title and a one-line caption; text always contrasts with the fill. */
	private View _choiceButton(String title, String caption, int fill, View.OnClickListener click) {
		LinearLayout box = new LinearLayout(this);
		box.setOrientation(LinearLayout.VERTICAL);
		box.setGravity(android.view.Gravity.CENTER);
		int pad = ThemedDialogs.dp(this, 14);
		box.setPadding(pad, pad + ThemedDialogs.dp(this, 4), pad, pad + ThemedDialogs.dp(this, 4));
		android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
		bg.setColor(fill);
		bg.setCornerRadius(ThemedDialogs.dp(this, 16));
		box.setBackground(new android.graphics.drawable.RippleDrawable(
			android.content.res.ColorStateList.valueOf(0x33FFFFFF), bg, null));
		TextView t = new TextView(this);
		t.setText(title);
		t.setTextSize(17f);
		t.setTypeface(null, android.graphics.Typeface.BOLD);
		t.setTextColor(com.harvana.app.utils.UiColors.onFill(fill));
		t.setGravity(android.view.Gravity.CENTER);
		box.addView(t);
		TextView c = new TextView(this);
		c.setText(caption);
		c.setTextSize(12f);
		c.setTextColor(0xE6FFFFFF);
		c.setGravity(android.view.Gravity.CENTER);
		c.setPadding(0, ThemedDialogs.dp(this, 2), 0, 0);
		box.addView(c);
		box.setClickable(true);
		box.setFocusable(true);
		box.setOnClickListener(click);
		return box;
	}
	
	private void _loadMine() {
		if (myAddress == null || myAddress.isEmpty()) {
			tv_manager_result_count.setText("No wallet found on this device.");
			return;
		}
		P2PTradeRouter.listMine(getApplicationContext(), myAddress, new P2PTradeRouter.Callback() {
			@Override
			public void onSuccess(JSONObject result) {
				List<JSONObject> listings = new ArrayList<>();
				List<JSONObject> orders = new ArrayList<>();
				JSONArray arr = result.optJSONArray("listings");
				if (arr != null) {
					for (int i = 0; i < arr.length(); i++) {
						JSONObject entry = arr.optJSONObject(i);
						if (entry == null) continue;
						if ("ORDER".equals(entry.optString("recordType", ""))) orders.add(entry);
						else listings.add(entry);
					}
				}
				runOnUiThread(() -> {
					allListings.clear();
					allListings.addAll(listings);
					allOrders.clear();
					allOrders.addAll(orders);
					_applyFilter();
				});
			}
			@Override
			public void onError(String error) {
				runOnUiThread(() -> {
					tv_manager_result_count.setText("Couldn't load: " + error);
					layout_manager_empty.setVisibility(View.VISIBLE);
					recycler_merchant_trades.setVisibility(View.GONE);
				});
			}
		});
	}
	
	// ── Filtering ────────────────────────────────────────────────────────
	
	private void _setFilter(String filter, TextView selectedTab) {
		activeFilter = filter;
		for (TextView t : new TextView[]{filter_manager_all, filter_manager_active, filter_manager_matched, filter_manager_paused, filter_manager_closed}) {
			_styleTab(t, t == selectedTab);
		}
		_applyFilter();
	}
	
	/** Selected tab: filled with a tint of the accent, accent text. Others: plain, secondary text. Every tab has a tap ripple. */
	private void _styleTab(TextView t, boolean selected) {
		int accent = androidx.core.content.ContextCompat.getColor(this, R.color.colorAccent);
		android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
		bg.setCornerRadius(ThemedDialogs.dp(this, 20));
		bg.setColor(selected ? ((accent & 0x00FFFFFF) | 0x33000000) : android.graphics.Color.TRANSPARENT);
		if (selected) bg.setStroke(ThemedDialogs.dp(this, 1), (accent & 0x00FFFFFF) | 0x99000000);
		t.setBackground(new android.graphics.drawable.RippleDrawable(
			android.content.res.ColorStateList.valueOf((accent & 0x00FFFFFF) | 0x22000000), bg, null));
		t.setTextColor(selected ? accent : androidx.core.content.ContextCompat.getColor(this, R.color.colorTextSecondary));
		t.setTypeface(null, selected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
		t.setAlpha(1f);
		t.setClickable(true);
		t.setFocusable(true);
	}
	
	private void _applyFilter() {
		visible.clear();
		int activeCount = 0, matchedCount = 0, closedCount = 0;
		for (JSONObject listing : allListings) {
			String status = listing.optString("status", "");
			long total = listing.optLong("totalAmount", 0);
			long remaining = listing.optLong("remainingAmount", 0);
			boolean isOpen = "OPEN".equals(status);
			boolean hasOrders = remaining < total;
			boolean isActive = isOpen;
			boolean isMatched = isOpen && hasOrders;
			boolean isClosed = !isOpen;
			
			if (isActive) activeCount++;
			if (isMatched) matchedCount++;
			if (isClosed) closedCount++;
			
			boolean include;
			switch (activeFilter) {
				case "ACTIVE": include = isActive; break;
				case "MATCHED": include = isMatched; break;
				case "PAUSED": include = false; break; // always empty — real, not a bug
				case "CLOSED": include = isClosed; break;
				default: include = true;
			}
			if (include) visible.add(listing);
		}
		
		tv_summary_active.setText(String.valueOf(activeCount));
		tv_summary_locked.setText(String.valueOf(matchedCount));
		tv_summary_completed.setText(String.valueOf(closedCount));
		tv_manager_result_count.setText(visible.size() + " listing" + (visible.size() == 1 ? "" : "s"));
		
		adapter.notifyDataSetChanged();
		boolean empty = visible.isEmpty();
		recycler_merchant_trades.setVisibility(empty ? View.GONE : View.VISIBLE);
		layout_manager_empty.setVisibility(empty ? View.VISIBLE : View.GONE);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuItem refreshItem = menu.add("Refresh");
		refreshItem.setIcon(R.drawable.ic_refresh);
		refreshItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if ("Refresh".contentEquals(item.getTitle())) { _loadMine(); return true; }
		return super.onOptionsItemSelected(item);
	}
	
	public class Recycler_merchant_offersAdapter extends RecyclerView.Adapter<Recycler_merchant_offersAdapter.ViewHolder> {
		
		List<JSONObject> _data;
		
		public Recycler_merchant_offersAdapter(List<JSONObject> _arr) {
			_data = _arr;
		}
		
		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			LayoutInflater _inflater = getLayoutInflater();
			View _v = _inflater.inflate(R.layout.p2p_merchant_offer_item, parent, false);
			return new ViewHolder(_v);
		}
		
		@Override
		public void onBindViewHolder(ViewHolder _holder, final int _position) {
			View _view = _holder.itemView;
			JSONObject listing = _data.get(_position);
			
			final com.google.android.material.card.MaterialCardView card_offer_type = _view.findViewById(R.id.card_offer_type);
			final TextView tv_offer_type = _view.findViewById(R.id.tv_offer_type);
			final TextView tv_offer_status = _view.findViewById(R.id.tv_offer_status);
			final TextView tv_offer_id = _view.findViewById(R.id.tv_offer_id);
			final TextView tv_offer_amount = _view.findViewById(R.id.tv_offer_amount);
			final TextView tv_offer_price = _view.findViewById(R.id.tv_offer_price);
			final TextView tv_offer_fiat_value = _view.findViewById(R.id.tv_offer_fiat_value);
			final TextView tv_offer_payment_one = _view.findViewById(R.id.tv_offer_payment_one);
			final TextView tv_offer_payment_two = _view.findViewById(R.id.tv_offer_payment_two);
			final TextView tv_offer_payment_none = _view.findViewById(R.id.tv_offer_payment_none);
			final TextView tv_offer_orders_count = _view.findViewById(R.id.tv_offer_orders_count);
			final TextView tv_offer_created = _view.findViewById(R.id.tv_offer_created);
			final TextView tv_offer_expiry = _view.findViewById(R.id.tv_offer_expiry);
			final TextView tv_offer_details = _view.findViewById(R.id.tv_offer_details);
			final TextView tv_offer_orders_btn = _view.findViewById(R.id.tv_offer_orders_btn);
			final TextView tv_offer_cancel = _view.findViewById(R.id.tv_offer_cancel);
			
			String listingId = P2pIds.listingId(listing);
			String side = listing.optString("side", "");
			String status = listing.optString("status", "");
			double totalHarv = HarvanaNodeClient.toHarvDisplay(listing.optLong("totalAmount", 0));
			double remainingHarv = HarvanaNodeClient.toHarvDisplay(listing.optLong("remainingAmount", 0));
			long createdAt = listing.optLong("createdTimestamp", 0);
			long expiresAt = listing.optLong("expiresAt", 0);
			boolean isOpen = "OPEN".equals(status);
			boolean hasOrders = remainingHarv < totalHarv;
			boolean isSell = "SELL".equals(side);
			
			int myOrderCount = 0;
			for (JSONObject order : allOrders) {
				if (listingId.equals(order.optString("listingId", ""))) myOrderCount++;
			}
			
			tv_offer_type.setText(isSell ? "SELL" : "BUY");
			card_offer_type.setCardBackgroundColor(getResources().getColor(isSell ? R.color.colorAccent : R.color.colorSuccess));
			String stateLabel = !isOpen ? "Closed" : (hasOrders ? "Matched" : "Active");
			tv_offer_status.setText(stateLabel);
			tv_offer_id.setText(_shortId(listingId));
			tv_offer_amount.setText(String.format(Locale.US, ("%.4f / %.4f " + com.harvana.app.CoinInfo.ticker()), remainingHarv, totalHarv));
			com.harvana.app.utils.Shimmer.loading(tv_offer_price);
			com.harvana.app.utils.Shimmer.loading(tv_offer_fiat_value);
			tv_offer_orders_count.setText(myOrderCount + " order" + (myOrderCount == 1 ? "" : "s"));
			tv_offer_created.setText("Listed " + _formatDate(createdAt));
			tv_offer_expiry.setText(expiresAt > 0 ? _formatExpiry(expiresAt) : "");
			tv_offer_payment_one.setVisibility(View.GONE);
			tv_offer_payment_two.setVisibility(View.GONE);
			tv_offer_payment_none.setVisibility(View.GONE);
			
			final int finalOrderCount = myOrderCount; // lambdas below need an effectively-final capture
			
			tv_offer_details.setOnClickListener(v -> ThemedDialogs.show(ThemedDialogs.alert(P2pMerchantManagerActivity.this)
			.setTitle(_shortId(listingId))
			.setMessage((isSell ? "You are selling " : "You are buying ") + com.harvana.app.CoinInfo.ticker()
			+ "\nState: " + stateLabel + "\nTotal: " + BalanceFormatter.smart(totalHarv)
			+ "\nRemaining: " + BalanceFormatter.smart(remainingHarv)
			+ "\nListed: " + _formatDate(createdAt)
			+ (expiresAt > 0 ? "\nExpires: " + _formatDate(expiresAt) : "")
			+ "\nOrders against this listing: " + finalOrderCount)
			.setPositiveButton("Close", null)));
			
			int needsMe = 0;
			for (JSONObject order : allOrders) if (listingId.equals(order.optString("listingId", "")) && _needsMe(order)) needsMe++;
			tv_offer_orders_btn.setText("Orders (" + myOrderCount + ")" + (needsMe > 0 ? " · " + needsMe + " need you" : ""));
			tv_offer_orders_btn.setTextColor(getResources().getColor(needsMe > 0 ? R.color.colorWarning : R.color.colorAccent));
			tv_offer_orders_btn.setOnClickListener(v -> _showOrdersForListing(listingId, side));
			
			tv_offer_cancel.setVisibility(isOpen ? View.VISIBLE : View.GONE);
			tv_offer_cancel.setOnClickListener(v -> _confirmCancelListing(listingId, side, remainingHarv));
			
			_view.setTag(listingId);
			
			if (!listingId.isEmpty()) {
				P2PPaymentDetailsManager.getInstance(getApplicationContext()).get(listingId, new P2PPaymentDetailsManager.DocCallback() {
					@Override
					public void onSuccess(Map<String, Object> data) {
						Object priceObj = data.get("unitPrice");
						double price = priceObj instanceof Number ? ((Number) priceObj).doubleValue() : -1;
						Object currencyObj = data.get("currency");
						String ccy = currencyObj != null && !String.valueOf(currencyObj).trim().isEmpty()
						? String.valueOf(currencyObj).trim() : com.harvana.app.p2p.P2pCurrencyFilterState.getCurrency();
						Object methodsObj = data.get("paymentMethods");
						List<String> labels = new ArrayList<>();
						if (methodsObj instanceof List) {
							for (Object m : (List<?>) methodsObj) {
								if (!(m instanceof Map)) continue;
								Object primary = ((Map<?, ?>) m).get("primaryField");
								if (primary != null && !String.valueOf(primary).trim().isEmpty()) labels.add(String.valueOf(primary).trim());
							}
						}
						runOnUiThread(() -> {
							if (!listingId.equals(_view.getTag())) return;
							tv_offer_price.setText(price >= 0 ? P2pMoney.perHarv(price, ccy) : "Unavailable");
							tv_offer_fiat_value.setText(price >= 0 ? P2pMoney.fiat(remainingHarv * price, ccy) : "Unavailable");
							if (labels.isEmpty()) {
								tv_offer_payment_none.setVisibility(View.VISIBLE);
							} else {
								tv_offer_payment_one.setText(_truncate(labels.get(0)));
								tv_offer_payment_one.setVisibility(View.VISIBLE);
								if (labels.size() > 1) {
									tv_offer_payment_two.setText(_truncate(labels.get(1)));
									tv_offer_payment_two.setVisibility(View.VISIBLE);
								}
							}
						});
					}
					@Override
					public void onError(String error) {
						runOnUiThread(() -> {
							if (!listingId.equals(_view.getTag())) return;
							tv_offer_price.setText("Unavailable");
							tv_offer_fiat_value.setText("Unavailable");
							tv_offer_payment_none.setVisibility(View.VISIBLE);
						});
					}
				});
			}
		}
		
		@Override
		public int getItemCount() {
			return _data.size();
		}
		
		public class ViewHolder extends RecyclerView.ViewHolder {
			public ViewHolder(View v) {
				super(v);
			}
		}
	}
	
	/** True when the next step of this order is mine: the buyer pays, the seller confirms and releases. */
	private boolean _needsMe(JSONObject order) {
		String st = order.optString("status", "");
		boolean buyer = myAddress != null && myAddress.equals(order.optString("buyerAddress", ""));
		return (buyer && "AWAITING_PAYMENT".equals(st)) || (!buyer && "PAYMENT_SUBMITTED".equals(st));
	}

	/** The order's state in plain words, from my side of it. */
	private String _orderState(JSONObject order) {
		boolean buyer = myAddress != null && myAddress.equals(order.optString("buyerAddress", ""));
		switch (order.optString("status", "")) {
			case "AWAITING_PAYMENT": return buyer ? "pay the seller" : "waiting for the buyer to pay";
			case "PAYMENT_SUBMITTED": return buyer ? "waiting for the seller to release" : "buyer says paid: confirm and release";
			case "COMPLETED": return "completed";
			case "CANCELLED": return "cancelled";
			case "DISPUTED": return "in dispute";
			default: return order.optString("status", "").toLowerCase(Locale.US).replace('_', ' ');
		}
	}

	private void _showOrdersForListing(String listingId, String side) {
		List<JSONObject> matched = new ArrayList<>();
		for (JSONObject order : allOrders) {
			if (listingId.equals(order.optString("listingId", ""))) matched.add(order);
		}
		if (matched.isEmpty()) {
			ThemedDialogs.show(ThemedDialogs.alert(this).setTitle("No orders yet").setMessage("Nobody has claimed against this listing yet.").setPositiveButton("Close", null));
			return;
		}
		String[] labels = new String[matched.size()];
		for (int i = 0; i < matched.size(); i++) {
			JSONObject o = matched.get(i);
			labels[i] = (_needsMe(o) ? "● " : "") + BalanceFormatter.smart(HarvanaNodeClient.toHarvDisplay(
					o.optLong("amount", o.optLong("netAmountToBuyer", 0)))) + " · " + _orderState(o);
		}
		ThemedDialogs.show(ThemedDialogs.alert(this).setTitle("Orders on this listing").setItems(labels, (dialog, which) -> {
			JSONObject order = matched.get(which);
			String orderId = P2pIds.orderId(order);
			boolean iAmSeller = myAddress.equals(order.optString("sellerAddress", ""));
			Intent intent = new Intent(this, iAmSeller ? P2pSellTradeActivity.class : P2pBuyTradeActivity.class);
			intent.putExtra("orderId", orderId);
			startActivity(intent);
		}));
	}
	
	private void _confirmCancelListing(String listingId, String side, double remainingHarv) {
		java.util.ArrayList<String[]> rows = new java.util.ArrayList<>();
		rows.add(new String[]{"For", "Stops new orders on what is still listed. Orders already opened are not touched. Locked HARV that nobody has claimed comes back to your wallet."});
		rows.add(new String[]{"Offer", "SELL".equals(side) ? "Selling " + com.harvana.app.CoinInfo.ticker() : "Buying " + com.harvana.app.CoinInfo.ticker()});
		rows.add(new String[]{"Still listed", com.harvana.app.wallet.BalanceFormatter.smart(remainingHarv)});
		rows.add(new String[]{"Returns to", "Your wallet"});
		com.harvana.app.group.PaymentConfirmSheet.reviewThenAuthorize(this,
		"Cancel this listing", com.harvana.app.wallet.BalanceFormatter.smart(remainingHarv), rows, () -> {
			P2PTradeRouter.cancelListing(getApplicationContext(), listingId, myAddress, new P2PTradeRouter.Callback() {
				@Override
				public void onSuccess(JSONObject result) {
					runOnUiThread(() -> _loadMine());
				}
				@Override
				public void onError(String error) {
					runOnUiThread(() -> Toast.makeText(P2pMerchantManagerActivity.this, "Couldn't cancel: " + error, Toast.LENGTH_LONG).show());
				}
			});
		});
	}
	
	private static String _truncate(String s) {
		return s.length() > 12 ? s.substring(0, 11) + "…" : s;
	}
	
	private static String _shortId(String id) {
		if (id == null || id.length() < 10) return id != null ? id : "";
		return id.substring(0, 8) + "…";
	}
	
	private static String _formatDate(long epochMs) {
		if (epochMs <= 0) return "—";
		return new SimpleDateFormat("MMM d, h:mm a", Locale.US).format(new Date(epochMs));
	}
	
	private static String _formatExpiry(long expiresAtMs) {
		long remaining = expiresAtMs - System.currentTimeMillis();
		if (remaining <= 0) return "Expired";
		long hours = remaining / (3600_000L);
		if (hours >= 24) return (hours / 24) + "d left";
		return hours + "h left";
	}
	
	{
	}
	
}
