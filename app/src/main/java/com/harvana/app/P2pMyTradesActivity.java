package com.harvana.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.harvana.app.firestore.P2PPaymentDetailsManager;
import com.harvana.app.firestore.UserProfileManager;
import com.harvana.app.p2p.P2pAsset;
import com.harvana.app.p2p.P2pIds;
import com.harvana.app.p2p.P2pKv;
import com.harvana.app.p2p.P2pMoney;
import com.harvana.app.p2p.P2pOffers;
import com.harvana.app.p2p.P2pStatus;
import com.harvana.app.tx.P2PTradeRouter;
import com.harvana.app.wallet.BalanceFormatter;
import com.harvana.app.wallet.WalletManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** My orders: every P2P order this wallet is in, as buyer or seller. Active first (yours to act on at the top). */
public class P2pMyTradesActivity extends AppCompatActivity {

	private static final String[][] ACTIVE_CHIPS = {
		{"ALL", "All"}, {"AWAITING_PAYMENT", "Unpaid"}, {"PAYMENT_SUBMITTED", "Paid"}, {"DISPUTED", "Appeal"}};
	private static final String[][] DONE_CHIPS = {
		{"ALL", "All"}, {"COMPLETED", "Completed"}, {"CANCELLED", "Cancelled"}, {"RESOLVED", "Appeal resolved"}};

	private String myAddress = "";
	private boolean activeTab = true;
	private String chip = "ALL";
	private final List<JSONObject> all = new ArrayList<>();
	private final List<JSONObject> shown = new ArrayList<>();
	/** listingId → {price, currency} from the offer card, cached for the rows. */
	private final Map<String, String[]> offerCache = new HashMap<>();
	private final Map<String, String> nameCache = new HashMap<>();

	private TextView tabActive, tabDone, tvEmpty, tvActionCount;
	private ChipGroup chips;
	private View banner, empty;
	private SwipeRefreshLayout refresh;
	private RecyclerView list;
	private final Adapter adapter = new Adapter();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.p2p_my_trades);
		com.harvana.app.utils.StatusBarUtil.matchToolbar(this);
		myAddress = WalletManager.getActiveWalletAddress(this);
		if (myAddress == null) myAddress = "";
		tabActive = findViewById(R.id.tab_orders_active);
		tabDone = findViewById(R.id.tab_orders_done);
		chips = findViewById(R.id.chips_orders_status);
		banner = findViewById(R.id.banner_orders_action);
		tvActionCount = findViewById(R.id.tv_orders_action_count);
		refresh = findViewById(R.id.refresh_orders);
		list = findViewById(R.id.rv_orders);
		empty = findViewById(R.id.layout_orders_empty);
		tvEmpty = findViewById(R.id.tv_orders_empty);
		list.setLayoutManager(new LinearLayoutManager(this));
		list.setAdapter(adapter);
		refresh.setColorSchemeColors(ContextCompat.getColor(this, R.color.colorAccent));
		refresh.setOnRefreshListener(this::load);
		findViewById(R.id.iv_orders_back).setOnClickListener(v -> finish());
		findViewById(R.id.iv_orders_refresh).setOnClickListener(v -> load());
		findViewById(R.id.btn_orders_market).setOnClickListener(v -> {
			startActivity(new Intent(this, P2pMarketplaceActivity.class));
			finish();
		});
		tabActive.setOnClickListener(v -> setTab(true));
		tabDone.setOnClickListener(v -> setTab(false));
		setTab(true);
	}

	@Override
	protected void onResume() {
		super.onResume();
		load();
	}

	private void setTab(boolean active) {
		activeTab = active;
		int on = ContextCompat.getColor(this, R.color.colorTextPrimary), off = ContextCompat.getColor(this, R.color.colorTextTertiary);
		tabActive.setTextColor(active ? on : off);
		tabDone.setTextColor(active ? off : on);
		chip = "ALL";
		chips.setOnCheckedChangeListener(null);
		chips.removeAllViews();
		for (String[] c : active ? ACTIVE_CHIPS : DONE_CHIPS) {
			Chip ch = new Chip(this);
			ch.setId(View.generateViewId());
			ch.setText(c[1]);
			ch.setTag(c[0]);
			ch.setCheckable(true);
			ch.setCheckedIconVisible(false);
			chips.addView(ch);
			if ("ALL".equals(c[0])) ch.setChecked(true);
		}
		chips.setOnCheckedChangeListener((g, id) -> {
			Chip ch = g.findViewById(id);
			if (ch != null) { chip = String.valueOf(ch.getTag()); apply(); }
		});
		apply();
	}

	private void load() {
		if (myAddress.isEmpty()) { refresh.setRefreshing(false); showEmpty("No wallet on this device"); return; }
		refresh.setRefreshing(true);
		P2PTradeRouter.listMine(getApplicationContext(), myAddress, new P2PTradeRouter.Callback() {
			@Override public void onSuccess(JSONObject result) {
				List<JSONObject> orders = new ArrayList<>();
				JSONArray arr = result.optJSONArray("listings");
				for (int i = 0; arr != null && i < arr.length(); i++) {
					JSONObject e = arr.optJSONObject(i);
					if (e != null && "ORDER".equals(e.optString("recordType", ""))) orders.add(e);
				}
				Collections.sort(orders, (a, b) -> {
					int na = needsMe(a) ? 0 : 1, nb = needsMe(b) ? 0 : 1;
					if (na != nb) return Integer.compare(na, nb);
					return Long.compare(b.optLong("createdTimestamp", 0), a.optLong("createdTimestamp", 0));
				});
				runOnUiThread(() -> {
					refresh.setRefreshing(false);
					all.clear();
					all.addAll(orders);
					apply();
				});
			}
			@Override public void onError(String error) {
				runOnUiThread(() -> {
					refresh.setRefreshing(false);
					all.clear();
					apply();
					showEmpty(com.harvana.app.utils.ErrorText.plain(error));
				});
			}
		});
	}

	/** The next step is mine: the buyer pays, the seller confirms and releases. */
	private boolean needsMe(JSONObject o) {
		String st = o.optString("status", "");
		boolean buyer = myAddress.equals(o.optString("buyerAddress", ""));
		return (buyer && "AWAITING_PAYMENT".equals(st)) || (!buyer && "PAYMENT_SUBMITTED".equals(st));
	}

	private void apply() {
		shown.clear();
		int mine = 0;
		for (JSONObject o : all) {
			String st = o.optString("status", "");
			if (P2pStatus.active(st) != activeTab) continue;
			if (needsMe(o)) mine++;
			boolean keep;
			switch (chip) {
				case "ALL": keep = true; break;
				case "RESOLVED": keep = st.startsWith("DISPUTE_RESOLVED"); break;
				case "DISPUTED": keep = "DISPUTED".equals(st) || "STUCK_NEEDS_MANUAL_REVIEW".equals(st); break;
				case "PAYMENT_SUBMITTED": keep = "PAYMENT_SUBMITTED".equals(st) || "RELEASE_PROCESSING".equals(st); break;
				default: keep = chip.equals(st);
			}
			if (keep) shown.add(o);
		}
		adapter.notifyDataSetChanged();
		banner.setVisibility(activeTab && mine > 0 ? View.VISIBLE : View.GONE);
		tvActionCount.setText(mine == 1 ? "1 order waiting for you" : mine + " orders waiting for you");
		if (shown.isEmpty()) showEmpty(activeTab ? "No active orders" : "No completed orders");
		else { empty.setVisibility(View.GONE); list.setVisibility(View.VISIBLE); }
	}

	private void showEmpty(String text) {
		list.setVisibility(View.GONE);
		empty.setVisibility(View.VISIBLE);
		tvEmpty.setText(text);
	}

	private static String shortAddr(String a) {
		return a == null || a.length() < 14 ? (a == null ? "" : a) : a.substring(0, 8) + "\u2026" + a.substring(a.length() - 4);
	}

	private static String time(long ms) {
		return ms <= 0 ? "" : new SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()).format(new Date(ms));
	}

	private final class Adapter extends RecyclerView.Adapter<Adapter.Holder> {

		final class Holder extends RecyclerView.ViewHolder {
			final ImageView coin;
			final TextView side, coinName, status, avatar, counterparty, timeView;
			final LinearLayout rows;

			Holder(View v) {
				super(v);
				coin = v.findViewById(R.id.iv_order_coin);
				side = v.findViewById(R.id.tv_order_side);
				coinName = v.findViewById(R.id.tv_order_coin);
				status = v.findViewById(R.id.tv_order_status);
				rows = v.findViewById(R.id.ll_order_rows);
				avatar = v.findViewById(R.id.tv_order_avatar);
				counterparty = v.findViewById(R.id.tv_order_counterparty);
				timeView = v.findViewById(R.id.tv_order_time);
			}
		}

		@NonNull @Override
		public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
			return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_p2p_order, parent, false));
		}

		@Override
		public void onBindViewHolder(@NonNull Holder h, int position) {
			JSONObject o = shown.get(position);
			String orderId = P2pIds.orderId(o);
			String listingId = o.optString("listingId", "");
			String status = o.optString("status", "");
			String asset = P2pAsset.of(o);
			String tk = P2pAsset.ticker(asset);
			boolean buyer = myAddress.equals(o.optString("buyerAddress", ""));
			double qty = P2pAsset.fromRaw(o.optLong("amount", o.optLong("buyerEntitlementRaw", 0)), asset);
			h.itemView.setTag(orderId);

			P2pOffers.coinIcon(h.coin, asset);
			h.side.setText(buyer ? "Buy" : "Sell");
			h.side.setTextColor(ContextCompat.getColor(P2pMyTradesActivity.this, buyer ? R.color.p2pBuy : R.color.p2pSell));
			h.coinName.setText(tk);
			h.status.setText(needsMe(o) ? (buyer ? "Pay now" : "Release now") : P2pStatus.label(status));
			h.status.setTextColor(ContextCompat.getColor(P2pMyTradesActivity.this, needsMe(o) ? R.color.colorAmber : P2pStatus.color(status)));
			h.timeView.setText(time(o.optLong("createdTimestamp", 0)));

			h.rows.removeAllViews();
			P2pKv.add(h.rows, "Amount", BalanceFormatter.smart(qty) + " " + tk);
			long kobo = o.optLong("unitPricePerHarvRaw", 0);
			TextView price = P2pKv.add(h.rows, "Price", kobo > 0 ? P2pMoney.fiat(kobo / 100.0, null) : "");
			TextView total = P2pKv.add(h.rows, "Total", kobo > 0 ? P2pMoney.fiat(qty * kobo / 100.0, null) : "");
			P2pKv.add(h.rows, "Order No.", orderId, true);
			bindPrice(listingId, orderId, qty, h.itemView, price, total);

			String other = buyer ? o.optString("sellerAddress", "") : o.optString("buyerAddress", "");
			h.counterparty.setText(shortAddr(other));
			h.avatar.setText("?");
			bindCounterparty(buyer, listingId, orderId, h);

			h.itemView.setOnClickListener(v -> {
				Intent i = new Intent(P2pMyTradesActivity.this, buyer ? P2pBuyTradeActivity.class : P2pSellTradeActivity.class);
				i.putExtra("orderId", orderId);
				startActivity(i);
			});
		}

		@Override
		public int getItemCount() { return shown.size(); }
	}

	/** Price and total in the offer's own currency (the offer card has it; the order has only the number). */
	private void bindPrice(String listingId, String orderId, double qty, View row, TextView price, TextView total) {
		if (listingId.isEmpty()) return;
		String[] cached = offerCache.get(listingId);
		if (cached != null) { setPrice(cached, qty, price, total); return; }
		P2PPaymentDetailsManager.getInstance(getApplicationContext()).get(listingId, new P2PPaymentDetailsManager.DocCallback() {
			@Override public void onSuccess(Map<String, Object> data) {
				Object p = data.get("unitPrice"), c = data.get("currency");
				if (!(p instanceof Number)) return;
				String[] v = {String.valueOf(((Number) p).doubleValue()), c == null ? null : String.valueOf(c)};
				offerCache.put(listingId, v);
				runOnUiThread(() -> { if (orderId.equals(row.getTag())) setPrice(v, qty, price, total); });
			}
			@Override public void onError(String error) { }
		});
	}

	private static void setPrice(String[] v, double qty, TextView price, TextView total) {
		double p = Double.parseDouble(v[0]);
		price.setText(P2pMoney.fiat(p, v[1]));
		total.setText(P2pMoney.fiat(p * qty, v[1]));
	}

	private void bindCounterparty(boolean iAmBuyer, String listingId, String orderId, Adapter.Holder h) {
		String key = (iAmBuyer ? "L:" : "O:") + (iAmBuyer ? listingId : orderId);
		String cached = nameCache.get(key);
		if (cached != null) { setName(h, cached); return; }
		if (iAmBuyer) {
			if (listingId.isEmpty()) return;
			P2PPaymentDetailsManager.getInstance(getApplicationContext()).get(listingId, new P2PPaymentDetailsManager.DocCallback() {
				@Override public void onSuccess(Map<String, Object> data) {
					Object uid = data.get("sellerUid");
					if (uid != null) profileName(String.valueOf(uid), key, orderId, h);
				}
				@Override public void onError(String error) { }
			});
		} else {
			P2PPaymentDetailsManager.getInstance(getApplicationContext()).getBuyerUidForOrder(listingId, orderId, new P2PPaymentDetailsManager.UidCallback() {
				@Override public void onFound(String uid) { profileName(uid, key, orderId, h); }
				@Override public void onNotFound() { }
				@Override public void onError(String error) { }
			});
		}
	}

	private void profileName(String uid, String key, String orderId, Adapter.Holder h) {
		UserProfileManager.getInstance(getApplicationContext()).getProfile(uid, new UserProfileManager.DocCallback() {
			@Override public void onSuccess(Map<String, Object> p) {
				String name = P2pOffers.displayName(p.get("first_name"), p.get("last_name"));
				nameCache.put(key, name);
				runOnUiThread(() -> { if (orderId.equals(h.itemView.getTag())) setName(h, name); });
			}
			@Override public void onError(String error) { }
		});
	}

	private static void setName(Adapter.Holder h, String name) {
		h.counterparty.setText(name);
		h.avatar.setText(name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase(Locale.US));
	}
}
