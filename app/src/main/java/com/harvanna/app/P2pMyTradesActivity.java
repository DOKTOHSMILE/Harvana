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
import com.google.android.material.card.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.storage.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;
import org.json.*;
import com.harvanna.app.firestore.P2PPaymentDetailsManager;
import com.harvanna.app.firestore.UserProfileManager;
import com.harvanna.app.sync.HarvannaNodeClient;
import com.harvanna.app.tx.P2PTradeRouter;
import com.harvanna.app.wallet.BalanceFormatter;
import com.harvanna.app.wallet.WalletManager;
import com.harvanna.app.p2p.P2pIds;
import com.harvanna.app.p2p.P2pMoney;


public class P2pMyTradesActivity extends AppCompatActivity {
	
	private Toolbar _toolbar;
	private AppBarLayout _app_bar;
	private CoordinatorLayout _coordinator;
	
	private LinearLayout my_trades_root;
	private MaterialCardView card_trade_summary;
	private HorizontalScrollView my_trades_filter_scroll;
	private LinearLayout linear268;
	private RecyclerView recycler_my_trades;
	private LinearLayout layout_my_trades_empty;
	private MaterialCardView card_my_trades_info;
	private LinearLayout linear260;
	private LinearLayout linear261;
	private LinearLayout linear263;
	private LinearLayout linear262;
	private TextView tv_trade_summary_count;
	private TextView textview151;
	private TextView tv_trade_summary_period;
	private LinearLayout linear264;
	private LinearLayout linear265;
	private LinearLayout linear266;
	private LinearLayout linear267;
	private TextView tv_summary_ongoing;
	private TextView textview152;
	private TextView tv_summary_completed;
	private TextView textview153;
	private TextView tv_summary_cancelled;
	private TextView textview154;
	private TextView tv_summary_disputed;
	private TextView textview155;
	private LinearLayout my_trades_filter_container;
	private TextView filter_trade_all;
	private TextView filter_trade_ongoing;
	private TextView filter_trade_completed;
	private TextView filter_trade_cancelled;
	private TextView filter_trade_disputed;
	private TextView tv_trades_section_title;
	private TextView tv_trades_result_count;
	private TextView textview156;
	private TextView tv_empty_trades_description;
	private LinearLayout linear269;
	private TextView textview157;
	private TextView tv_my_trades_info;
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.p2p_my_trades);
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
		my_trades_root = findViewById(R.id.my_trades_root);
		card_trade_summary = findViewById(R.id.card_trade_summary);
		my_trades_filter_scroll = findViewById(R.id.my_trades_filter_scroll);
		linear268 = findViewById(R.id.linear268);
		recycler_my_trades = findViewById(R.id.recycler_my_trades);
		layout_my_trades_empty = findViewById(R.id.layout_my_trades_empty);
		card_my_trades_info = findViewById(R.id.card_my_trades_info);
		linear260 = findViewById(R.id.linear260);
		linear261 = findViewById(R.id.linear261);
		linear263 = findViewById(R.id.linear263);
		linear262 = findViewById(R.id.linear262);
		tv_trade_summary_count = findViewById(R.id.tv_trade_summary_count);
		textview151 = findViewById(R.id.textview151);
		tv_trade_summary_period = findViewById(R.id.tv_trade_summary_period);
		linear264 = findViewById(R.id.linear264);
		linear265 = findViewById(R.id.linear265);
		linear266 = findViewById(R.id.linear266);
		linear267 = findViewById(R.id.linear267);
		tv_summary_ongoing = findViewById(R.id.tv_summary_ongoing);
		textview152 = findViewById(R.id.textview152);
		tv_summary_completed = findViewById(R.id.tv_summary_completed);
		textview153 = findViewById(R.id.textview153);
		tv_summary_cancelled = findViewById(R.id.tv_summary_cancelled);
		textview154 = findViewById(R.id.textview154);
		tv_summary_disputed = findViewById(R.id.tv_summary_disputed);
		textview155 = findViewById(R.id.textview155);
		my_trades_filter_container = findViewById(R.id.my_trades_filter_container);
		filter_trade_all = findViewById(R.id.filter_trade_all);
		filter_trade_ongoing = findViewById(R.id.filter_trade_ongoing);
		filter_trade_completed = findViewById(R.id.filter_trade_completed);
		filter_trade_cancelled = findViewById(R.id.filter_trade_cancelled);
		filter_trade_disputed = findViewById(R.id.filter_trade_disputed);
		tv_trades_section_title = findViewById(R.id.tv_trades_section_title);
		tv_trades_result_count = findViewById(R.id.tv_trades_result_count);
		textview156 = findViewById(R.id.textview156);
		tv_empty_trades_description = findViewById(R.id.tv_empty_trades_description);
		linear269 = findViewById(R.id.linear269);
		textview157 = findViewById(R.id.textview157);
		tv_my_trades_info = findViewById(R.id.tv_my_trades_info);
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
			getSupportActionBar().setTitle("My Trades");
			getSupportActionBar().setSubtitle("Transactions you've personally taken part in");
		}
		myAddress = WalletManager.getActiveWalletAddress(this);
		
		adapter = new Recycler_my_tradesAdapter(visible);
		recycler_my_trades.setLayoutManager(new LinearLayoutManager(this));
		recycler_my_trades.setAdapter(adapter);
		
		tv_trade_summary_period.setText("All time");
		tv_my_trades_info.setText("This is your personal trade history — both when you bought and when you sold. Managing your own listings happens in Merchant Manager instead.");
		
		
		_setFilter("ALL", filter_trade_all);
		filter_trade_all.setOnClickListener(v -> _setFilter("ALL", filter_trade_all));
		filter_trade_ongoing.setOnClickListener(v -> _setFilter("ONGOING", filter_trade_ongoing));
		filter_trade_completed.setOnClickListener(v -> _setFilter("COMPLETED", filter_trade_completed));
		filter_trade_cancelled.setOnClickListener(v -> _setFilter("CANCELLED", filter_trade_cancelled));
		filter_trade_disputed.setOnClickListener(v -> _setFilter("DISPUTED", filter_trade_disputed));
		
		_loadMine();
		
	}
	
	
	@Override
	public void onResume() {
		super.onResume();
		if (!allOrders.isEmpty()) _loadMine();
	}
	public void _MORE() {
	}
	private String myAddress = "";
	private final List<JSONObject> allOrders = new ArrayList<>();
	private final List<JSONObject> visible = new ArrayList<>();
	private String activeFilter = "ALL";
	private Recycler_my_tradesAdapter adapter;
	
	private void _loadMine() {
		if (myAddress == null || myAddress.isEmpty()) {
			tv_trades_result_count.setText("No wallet found on this device.");
			return;
		}
		P2PTradeRouter.listMine(getApplicationContext(), myAddress, new P2PTradeRouter.Callback() {
			@Override
			public void onSuccess(JSONObject result) {
				List<JSONObject> orders = new ArrayList<>();
				JSONArray arr = result.optJSONArray("listings");
				if (arr != null) {
					for (int i = 0; i < arr.length(); i++) {
						JSONObject entry = arr.optJSONObject(i);
						if (entry != null && "ORDER".equals(entry.optString("recordType", ""))) orders.add(entry);
					}
				}
				orders.sort((a, b) -> Long.compare(b.optLong("createdTimestamp", 0), a.optLong("createdTimestamp", 0)));
				runOnUiThread(() -> {
					allOrders.clear();
					allOrders.addAll(orders);
					_applyFilter();
				});
			}
			@Override
			public void onError(String error) {
				runOnUiThread(() -> {
					tv_trades_result_count.setText("Couldn't load: " + error);
					layout_my_trades_empty.setVisibility(View.VISIBLE);
					recycler_my_trades.setVisibility(View.GONE);
				});
			}
		});
	}
	
	private void _setFilter(String filter, TextView selectedTab) {
		activeFilter = filter;
		filter_trade_all.setAlpha(0.6f);
		filter_trade_ongoing.setAlpha(0.6f);
		filter_trade_completed.setAlpha(0.6f);
		filter_trade_cancelled.setAlpha(0.6f);
		filter_trade_disputed.setAlpha(0.6f);
		selectedTab.setAlpha(1f);
		_applyFilter();
	}
	
	private void _applyFilter() {
		visible.clear();
		int ongoing = 0, completed = 0, cancelled = 0, disputed = 0;
		for (JSONObject order : allOrders) {
			String status = order.optString("status", "");
			boolean isOngoing = "AWAITING_PAYMENT".equals(status) || "PAYMENT_SUBMITTED".equals(status);
			boolean isCompleted = "COMPLETED".equals(status);
			boolean isCancelled = "CANCELLED".equals(status);
			boolean isDisputed = "DISPUTED".equals(status);
			
			if (isOngoing) ongoing++;
			if (isCompleted) completed++;
			if (isCancelled) cancelled++;
			if (isDisputed) disputed++;
			
			boolean include;
			switch (activeFilter) {
				case "ONGOING": include = isOngoing; break;
				case "COMPLETED": include = isCompleted; break;
				case "CANCELLED": include = isCancelled; break;
				case "DISPUTED": include = isDisputed; break;
				default: include = true;
			}
			if (include) visible.add(order);
		}
		
		tv_summary_ongoing.setText(String.valueOf(ongoing));
		tv_summary_completed.setText(String.valueOf(completed));
		tv_summary_cancelled.setText(String.valueOf(cancelled));
		tv_summary_disputed.setText(String.valueOf(disputed));
		tv_trade_summary_count.setText(String.valueOf(allOrders.size()));
		tv_trades_result_count.setText(visible.size() + " trade" + (visible.size() == 1 ? "" : "s"));
		
		adapter.notifyDataSetChanged();
		boolean empty = visible.isEmpty();
		recycler_my_trades.setVisibility(empty ? View.GONE : View.VISIBLE);
		layout_my_trades_empty.setVisibility(empty ? View.VISIBLE : View.GONE);
		if (empty) {
			tv_empty_trades_description.setText(allOrders.isEmpty()
			? ("Once you buy or sell " + com.harvanna.app.CoinInfo.ticker() + " through P2P, your trades will show up here.")
			: "No trades match this filter right now.");
		}
	}
	
	public class Recycler_my_tradesAdapter extends RecyclerView.Adapter<Recycler_my_tradesAdapter.ViewHolder> {
		
		List<JSONObject> _data;
		
		public Recycler_my_tradesAdapter(List<JSONObject> _arr) {
			_data = _arr;
		}
		
		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			LayoutInflater _inflater = getLayoutInflater();
			View _v = _inflater.inflate(R.layout.p2p_my_trade_item, parent, false);
			return new ViewHolder(_v);
		}
		
		@Override
		public void onBindViewHolder(ViewHolder _holder, final int _position) {
			View _view = _holder.itemView;
			JSONObject order = _data.get(_position);
			
			final TextView tv_my_trade_order_id = _view.findViewById(R.id.tv_my_trade_order_id);
			final LinearLayout layout_my_trade_state = _view.findViewById(R.id.layout_my_trade_state);
			final TextView tv_my_trade_open = _view.findViewById(R.id.tv_my_trade_open);
			final TextView tv_my_trade_type = _view.findViewById(R.id.tv_my_trade_type);
			final TextView tv_my_trade_status = _view.findViewById(R.id.tv_my_trade_status);
			final TextView tv_my_trade_menu = _view.findViewById(R.id.tv_my_trade_menu);
			final TextView tv_my_trade_avatar = _view.findViewById(R.id.tv_my_trade_avatar);
			final TextView tv_my_trade_counterparty = _view.findViewById(R.id.tv_my_trade_counterparty);
			final TextView tv_my_trade_counterparty_meta = _view.findViewById(R.id.tv_my_trade_counterparty_meta);
			final TextView tv_my_trade_harv = _view.findViewById(R.id.tv_my_trade_harv);
			final TextView tv_my_trade_fiat = _view.findViewById(R.id.tv_my_trade_fiat);
			final TextView tv_my_trade_state_title = _view.findViewById(R.id.tv_my_trade_state_title);
			final TextView tv_my_trade_state_description = _view.findViewById(R.id.tv_my_trade_state_description);
			final TextView tv_my_trade_date = _view.findViewById(R.id.tv_my_trade_date);
			final TextView tv_my_trade_tx_state = _view.findViewById(R.id.tv_my_trade_tx_state);
			
			String orderId = P2pIds.orderId(order);
			String listingId = order.optString("listingId", "");
			String buyerAddress = order.optString("buyerAddress", "");
			String sellerAddress = order.optString("sellerAddress", "");
			String status = order.optString("status", "");
			long claimedRaw = order.optLong("amount", order.optLong("netAmountToBuyer", order.optLong("buyerEntitlementRaw", 0)));
			double harvAmount = HarvannaNodeClient.toHarvDisplay(claimedRaw);
			long createdAt = order.optLong("createdTimestamp", 0);
			String releaseTxHash = order.optString("releaseTxHash", "");
			boolean iWasBuyer = myAddress.equals(buyerAddress);
			
			tv_my_trade_type.setText(iWasBuyer ? "Bought" : "Sold");
			tv_my_trade_status.setText(_statusLabel(status));
			tv_my_trade_order_id.setText(_shortId(orderId));
			tv_my_trade_harv.setText(BalanceFormatter.smart(harvAmount));
			tv_my_trade_fiat.setText("Loading…");
			tv_my_trade_date.setText(_formatDate(createdAt));
			tv_my_trade_tx_state.setText(releaseTxHash.isEmpty() ? "No release yet" : _shortId(releaseTxHash));
			tv_my_trade_menu.setVisibility(View.GONE);
			
			switch (status) {
				case "AWAITING_PAYMENT":
				layout_my_trade_state.setVisibility(View.VISIBLE);
				tv_my_trade_state_title.setText(iWasBuyer ? "Awaiting your payment" : "Awaiting buyer's payment");
				tv_my_trade_state_description.setText("Escrow is locked; nothing released yet.");
				break;
				case "PAYMENT_SUBMITTED":
				layout_my_trade_state.setVisibility(View.VISIBLE);
				tv_my_trade_state_title.setText(iWasBuyer ? "Waiting for seller to confirm" : "Confirm you've received payment");
				tv_my_trade_state_description.setText("Payment was marked sent.");
				break;
				case "DISPUTED":
				layout_my_trade_state.setVisibility(View.VISIBLE);
				tv_my_trade_state_title.setText("Under dispute review");
				tv_my_trade_state_description.setText("A guardian will resolve this.");
				break;
				default:
				layout_my_trade_state.setVisibility(View.GONE);
			}
			
			tv_my_trade_open.setText("View");
			tv_my_trade_open.setOnClickListener(v -> {
				Intent intent = new Intent(P2pMyTradesActivity.this, iWasBuyer ? P2pBuyTradeActivity.class : P2pSellTradeActivity.class);
				intent.putExtra("orderId", orderId);
				startActivity(intent);
			});
			
			_view.setTag(orderId);
			
			// Mutual visibility: resolve whichever party ISN'T me, using
			// the same real lookups Buy Trade / Sell Trade already use.
			tv_my_trade_avatar.setText("?");
			tv_my_trade_counterparty.setText("Loading…");
			tv_my_trade_counterparty_meta.setText("");
			
			if (iWasBuyer) {
				if (!listingId.isEmpty()) {
					P2PPaymentDetailsManager.getInstance(getApplicationContext()).get(listingId, new P2PPaymentDetailsManager.DocCallback() {
						@Override
						public void onSuccess(Map<String, Object> data) {
							Object priceObj = data.get("unitPrice");
							double price = priceObj instanceof Number ? ((Number) priceObj).doubleValue() : -1;
							Object currencyObj = data.get("currency");
							String ccy = currencyObj != null && !String.valueOf(currencyObj).trim().isEmpty()
							? String.valueOf(currencyObj).trim() : com.harvanna.app.p2p.P2pCurrencyFilterState.getCurrency();
							runOnUiThread(() -> {
								if (!orderId.equals(_view.getTag())) return;
								tv_my_trade_fiat.setText(price >= 0 ? P2pMoney.fiat(harvAmount * price, ccy) : "Unavailable");
							});
							Object uidObj = data.get("sellerUid");
							String sellerUid = uidObj != null ? String.valueOf(uidObj) : "";
							if (sellerUid.isEmpty()) { runOnUiThread(() -> { if (orderId.equals(_view.getTag())) tv_my_trade_counterparty.setText("Trader"); }); return; }
							_resolveCounterparty(sellerUid, tv_my_trade_avatar, tv_my_trade_counterparty, tv_my_trade_counterparty_meta, sellerAddress, orderId, _view);
						}
						@Override
						public void onError(String error) {
							runOnUiThread(() -> {
								if (!orderId.equals(_view.getTag())) return;
								tv_my_trade_counterparty.setText("Trader");
								tv_my_trade_fiat.setText("Unavailable");
							});
						}
					});
				}
			} else {
				P2PPaymentDetailsManager.getInstance(getApplicationContext()).getBuyerUidForOrder(listingId, orderId, new P2PPaymentDetailsManager.UidCallback() {
					@Override
					public void onFound(String buyerUid) {
						_resolveCounterparty(buyerUid, tv_my_trade_avatar, tv_my_trade_counterparty, tv_my_trade_counterparty_meta, buyerAddress, orderId, _view);
					}
					@Override
					public void onNotFound() {
						runOnUiThread(() -> { if (orderId.equals(_view.getTag())) tv_my_trade_counterparty.setText("Trader"); });
					}
					@Override
					public void onError(String error) {
						runOnUiThread(() -> { if (orderId.equals(_view.getTag())) tv_my_trade_counterparty.setText("Trader"); });
					}
				});
				if (!listingId.isEmpty()) {
					P2PPaymentDetailsManager.getInstance(getApplicationContext()).get(listingId, new P2PPaymentDetailsManager.DocCallback() {
						@Override
						public void onSuccess(Map<String, Object> data) {
							Object priceObj = data.get("unitPrice");
							double price = priceObj instanceof Number ? ((Number) priceObj).doubleValue() : -1;
							Object currencyObj = data.get("currency");
							String ccy = currencyObj != null && !String.valueOf(currencyObj).trim().isEmpty()
							? String.valueOf(currencyObj).trim() : com.harvanna.app.p2p.P2pCurrencyFilterState.getCurrency();
							runOnUiThread(() -> {
								if (!orderId.equals(_view.getTag())) return;
								tv_my_trade_fiat.setText(price >= 0 ? P2pMoney.fiat(harvAmount * price, ccy) : "Unavailable");
							});
						}
						@Override
						public void onError(String error) {
							runOnUiThread(() -> {
								if (!orderId.equals(_view.getTag())) return;
								tv_my_trade_fiat.setText("Unavailable");
							});
						}
					});
				}
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
	
	private void _resolveCounterparty(String uid, TextView avatar, TextView name, TextView meta, String fallbackAddress, String rowId, View row) {
		UserProfileManager.getInstance(getApplicationContext()).getProfile(uid, new UserProfileManager.DocCallback() {
			@Override
			public void onSuccess(Map<String, Object> profile) {
				Object first = profile.get("first_name");
				Object last = profile.get("last_name");
				String firstStr = first != null ? String.valueOf(first).trim() : "";
				String lastStr = last != null ? String.valueOf(last).trim() : "";
				String resolvedName = !firstStr.isEmpty()
				? (lastStr.isEmpty() ? firstStr : firstStr + " " + lastStr.substring(0, 1).toUpperCase(Locale.US) + ".")
				: "Trader";
				runOnUiThread(() -> {
					if (rowId != null && row != null && !rowId.equals(row.getTag())) return;
					name.setText(resolvedName);
					avatar.setText(resolvedName.length() > 0 ? resolvedName.substring(0, 1).toUpperCase(Locale.US) : "?");
					meta.setText(_shortAddr(fallbackAddress));
				});
			}
			@Override
			public void onError(String error) {
				runOnUiThread(() -> {
					if (rowId != null && row != null && !rowId.equals(row.getTag())) return;
					name.setText("Trader");
				});
			}
		});
	}
	
	private static String _statusLabel(String status) {
		switch (status) {
			case "AWAITING_PAYMENT": return "Awaiting Payment";
			case "PAYMENT_SUBMITTED": return "Payment Submitted";
			case "COMPLETED": return "Completed";
			case "DISPUTED": return "Disputed";
			case "CANCELLED": return "Cancelled";
			default: return status;
		}
	}
	
	private static String _shortId(String id) {
		if (id == null || id.length() < 10) return id != null ? id : "";
		return id.substring(0, 8) + "…";
	}
	
	private static String _shortAddr(String address) {
		if (address == null || address.length() < 12) return address != null ? address : "";
		return address.substring(0, 8) + "…" + address.substring(address.length() - 4);
	}
	
	private static String _formatDate(long epochMs) {
		if (epochMs <= 0) return "—";
		return new SimpleDateFormat("MMM d, h:mm a", Locale.US).format(new Date(epochMs));
	}
	
	// GROUND-UP FIX (Sept 17 2026): Sketchware's actual generated layout
	// has no tv_my_trades_refresh TextView at all — refresh lives as a
	// real ActionBar menu item instead, same pattern already used in
	// Merchant Manager.
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
	
	{
	}
	
}
