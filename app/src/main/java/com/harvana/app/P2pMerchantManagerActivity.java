package com.harvana.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.harvana.app.banks.BankLogo;
import com.harvana.app.firestore.P2PPaymentDetailsManager;
import com.harvana.app.p2p.P2pAsset;
import com.harvana.app.p2p.P2pIds;
import com.harvana.app.p2p.P2pKv;
import com.harvana.app.p2p.P2pMoney;
import com.harvana.app.p2p.P2pOffers;
import com.harvana.app.p2p.P2pStatus;
import com.harvana.app.p2p.ThemedDialogs;
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

/** My offers: every offer this wallet posted, with the orders opened on each. Cancel stops new orders on what is left. */
public class P2pMerchantManagerActivity extends AppCompatActivity {

	private static final String[][] CHIPS = {{"ALL", "All"}, {"ACTIVE", "Active"}, {"ORDERS", "With orders"}, {"CLOSED", "Closed"}};

	private String myAddress = "", chip = "ALL";
	private final List<JSONObject> listings = new ArrayList<>(), orders = new ArrayList<>(), shown = new ArrayList<>();
	private final Map<String, Map<String, Object>> cards = new HashMap<>();

	private SwipeRefreshLayout refresh;
	private RecyclerView list;
	private View empty;
	private final Adapter adapter = new Adapter();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.p2p_merchant_manager);
		com.harvana.app.utils.StatusBarUtil.matchToolbar(this);
		myAddress = WalletManager.getActiveWalletAddress(this);
		if (myAddress == null) myAddress = "";
		refresh = findViewById(R.id.refresh_offers);
		list = findViewById(R.id.rv_offers);
		empty = findViewById(R.id.layout_offers_empty);
		list.setLayoutManager(new LinearLayoutManager(this));
		list.setAdapter(adapter);
		refresh.setColorSchemeColors(ContextCompat.getColor(this, R.color.colorAccent));
		refresh.setOnRefreshListener(this::load);
		findViewById(R.id.iv_offers_back).setOnClickListener(v -> finish());
		findViewById(R.id.iv_offers_refresh).setOnClickListener(v -> load());
		findViewById(R.id.fab_offers_new).setOnClickListener(v -> newOffer());
		label(R.id.stat_offers_active, "Active");
		label(R.id.stat_offers_orders, "With orders");
		label(R.id.stat_offers_closed, "Closed");

		ChipGroup chips = findViewById(R.id.chips_offers_status);
		for (String[] c : CHIPS) {
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
	}

	@Override
	protected void onResume() {
		super.onResume();
		load();
	}

	private void label(int tile, String text) { ((TextView) findViewById(tile).findViewById(R.id.tv_stat_label)).setText(text); }
	private void count(int tile, int n) { ((TextView) findViewById(tile).findViewById(R.id.tv_stat_value)).setText(String.valueOf(n)); }

	private void load() {
		if (myAddress.isEmpty()) { showEmpty("No wallet on this device"); return; }
		refresh.setRefreshing(true);
		P2PTradeRouter.listMine(getApplicationContext(), myAddress, new P2PTradeRouter.Callback() {
			@Override public void onSuccess(JSONObject result) {
				List<JSONObject> l = new ArrayList<>(), o = new ArrayList<>();
				JSONArray arr = result.optJSONArray("listings");
				for (int i = 0; arr != null && i < arr.length(); i++) {
					JSONObject e = arr.optJSONObject(i);
					if (e == null) continue;
					if ("ORDER".equals(e.optString("recordType", ""))) o.add(e);
					else if (myAddress.equals(poster(e))) l.add(e);
				}
				Collections.sort(l, (a, b) -> Long.compare(b.optLong("createdTimestamp", 0), a.optLong("createdTimestamp", 0)));
				runOnUiThread(() -> {
					refresh.setRefreshing(false);
					listings.clear();
					listings.addAll(l);
					orders.clear();
					orders.addAll(o);
					apply();
				});
			}
			@Override public void onError(String error) {
				runOnUiThread(() -> { refresh.setRefreshing(false); showEmpty(com.harvana.app.utils.ErrorText.plain(error)); });
			}
		});
	}

	private static String poster(JSONObject l) {
		return "BUY".equals(l.optString("side")) ? l.optString("buyerAddress", "") : l.optString("sellerAddress", "");
	}

	private List<JSONObject> ordersOf(String listingId) {
		List<JSONObject> out = new ArrayList<>();
		for (JSONObject o : orders) if (listingId.equals(o.optString("listingId", ""))) out.add(o);
		return out;
	}

	private boolean needsMe(JSONObject o) {
		String st = o.optString("status", "");
		boolean buyer = myAddress.equals(o.optString("buyerAddress", ""));
		return (buyer && "AWAITING_PAYMENT".equals(st)) || (!buyer && "PAYMENT_SUBMITTED".equals(st));
	}

	private void apply() {
		shown.clear();
		int active = 0, withOrders = 0, closed = 0;
		for (JSONObject l : listings) {
			boolean open = "OPEN".equals(l.optString("status"));
			boolean hasOrders = !ordersOf(P2pIds.listingId(l)).isEmpty();
			if (open) active++; else closed++;
			if (hasOrders) withOrders++;
			boolean keep = "ALL".equals(chip) || ("ACTIVE".equals(chip) && open) || ("CLOSED".equals(chip) && !open) || ("ORDERS".equals(chip) && hasOrders);
			if (keep) shown.add(l);
		}
		count(R.id.stat_offers_active, active);
		count(R.id.stat_offers_orders, withOrders);
		count(R.id.stat_offers_closed, closed);
		adapter.notifyDataSetChanged();
		if (shown.isEmpty()) showEmpty(listings.isEmpty() ? "No offers yet" : "None");
		else { empty.setVisibility(View.GONE); list.setVisibility(View.VISIBLE); }
	}

	private void showEmpty(String text) {
		list.setVisibility(View.GONE);
		empty.setVisibility(View.VISIBLE);
		((TextView) findViewById(R.id.tv_offers_empty)).setText(text);
	}

	private static String time(long ms) {
		return ms <= 0 ? "" : new SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()).format(new Date(ms));
	}

	private final class Adapter extends RecyclerView.Adapter<Adapter.Holder> {
		final class Holder extends RecyclerView.ViewHolder {
			Holder(View v) { super(v); }
		}

		@NonNull @Override
		public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
			return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_p2p_my_offer, parent, false));
		}

		@Override
		public void onBindViewHolder(@NonNull Holder h, int position) {
			View v = h.itemView;
			JSONObject l = shown.get(position);
			String id = P2pIds.listingId(l), asset = P2pAsset.of(l), tk = P2pAsset.ticker(asset);
			boolean sell = "SELL".equals(l.optString("side"));
			boolean open = "OPEN".equals(l.optString("status"));
			double total = P2pAsset.fromRaw(asset, l.optLong("totalAmount", 0));
			double left = P2pAsset.fromRaw(asset, l.optLong("remainingAmount", 0));
			long expires = l.optLong("expiresAt", 0);
			boolean expired = open && expires > 0 && expires < System.currentTimeMillis();
			List<JSONObject> mine = ordersOf(id);
			int waiting = 0;
			for (JSONObject o : mine) if (needsMe(o)) waiting++;
			v.setTag(id);

			P2pOffers.coinIcon(v.findViewById(R.id.iv_my_offer_coin), asset);
			TextView side = v.findViewById(R.id.tv_my_offer_side);
			side.setText((sell ? "Sell " : "Buy ") + tk);
			side.setTextColor(ContextCompat.getColor(P2pMerchantManagerActivity.this, sell ? R.color.p2pSell : R.color.p2pBuy));
			TextView st = v.findViewById(R.id.tv_my_offer_status);
			st.setText(expired ? "Expired" : open ? "Active" : "Closed");
			st.setTextColor(ContextCompat.getColor(P2pMerchantManagerActivity.this, expired ? R.color.colorAmber : open ? R.color.colorSuccess : R.color.colorTextTertiary));
			((TextView) v.findViewById(R.id.tv_my_offer_remaining)).setText(BalanceFormatter.smartDecimal(left) + " / " + BalanceFormatter.smartDecimal(total) + " " + tk);
			((LinearProgressIndicator) v.findViewById(R.id.pb_my_offer)).setProgress(total > 0 ? (int) Math.round(1000 * left / total) : 0);

			LinearLayout rows = v.findViewById(R.id.ll_my_offer_rows);
			rows.removeAllViews();
			TextView price = P2pKv.add(rows, "Price", "");
			TextView value = P2pKv.add(rows, "Value left", "");
			P2pKv.add(rows, "Limit", BalanceFormatter.smartDecimal(P2pAsset.fromRaw(asset, l.optLong("minOrderAmount", 0))) + " – "
				+ BalanceFormatter.smartDecimal(P2pAsset.fromRaw(asset, l.optLong("maxOrderAmount", l.optLong("remainingAmount", 0)))) + " " + tk);
			if (l.optLong("paymentWindowMinutes", 0) > 0) P2pKv.add(rows, "Payment window", l.optLong("paymentWindowMinutes") + " min");
			if (open && expires > 0) P2pKv.add(rows, "Expires", time(expires));
			P2pKv.add(rows, "Offer No.", id, true);
			LinearLayout methods = v.findViewById(R.id.ll_my_offer_methods);
			methods.removeAllViews();
			bindCard(id, asset, left, price, value, methods, v);

			MaterialButton ordersBtn = v.findViewById(R.id.btn_my_offer_orders);
			ordersBtn.setText("Orders " + mine.size() + (waiting > 0 ? " · " + waiting + " waiting" : ""));
			ordersBtn.setTextColor(ContextCompat.getColor(P2pMerchantManagerActivity.this, waiting > 0 ? R.color.colorAmber : R.color.colorAccent));
			ordersBtn.setEnabled(!mine.isEmpty());
			ordersBtn.setOnClickListener(x -> showOrders(mine));
			View cancel = v.findViewById(R.id.btn_my_offer_cancel);
			cancel.setVisibility(open ? View.VISIBLE : View.GONE);
			cancel.setOnClickListener(x -> confirmCancel(id, asset, sell, left));
			View priceBtn = v.findViewById(R.id.btn_my_offer_price);
			if (priceBtn != null) {
				priceBtn.setVisibility(open ? View.VISIBLE : View.GONE);
				priceBtn.setOnClickListener(x -> editPrice(id, asset));
			}
		}

		@Override
		public int getItemCount() { return shown.size(); }
	}

	private void bindCard(String id, String asset, double left, TextView price, TextView value, LinearLayout methods, View row) {
		Map<String, Object> cached = cards.get(id);
		if (cached != null) { fillCard(cached, asset, left, price, value, methods); return; }
		P2PPaymentDetailsManager.getInstance(getApplicationContext()).get(id, new P2PPaymentDetailsManager.DocCallback() {
			@Override public void onSuccess(Map<String, Object> d) {
				cards.put(id, d);
				runOnUiThread(() -> { if (id.equals(row.getTag())) fillCard(d, asset, left, price, value, methods); });
			}
			@Override public void onError(String error) { }
		});
	}

	private void fillCard(Map<String, Object> d, String asset, double left, TextView price, TextView value, LinearLayout methods) {
		Object p = d.get("unitPrice"), c = d.get("currency");
		String ccy = c == null ? null : String.valueOf(c);
		if (p instanceof Number) {
			double pr = ((Number) p).doubleValue();
			price.setText(P2pMoney.perUnit(pr, ccy, asset));
			value.setText(P2pMoney.fiat(pr * left, ccy));
		}
		methods.removeAllViews();
		List<String> labels = new ArrayList<>();
		Object names = d.get("paymentMethodNames");
		if (names instanceof List) for (Object n : (List<?>) names) if (n != null) labels.add(String.valueOf(n));
		Object ms = d.get("paymentMethods");
		if (labels.isEmpty() && ms instanceof List) for (Object m : (List<?>) ms) {
			if (m instanceof Map && ((Map<?, ?>) m).get("primaryField") != null) labels.add(String.valueOf(((Map<?, ?>) m).get("primaryField")));
		}
		for (String label : labels) {
			View chip = getLayoutInflater().inflate(R.layout.item_p2p_method_chip, methods, false);
			BankLogo.into((ImageView) chip.findViewById(R.id.iv_method_logo), label);
			((TextView) chip.findViewById(R.id.tv_method_name)).setText(label);
			methods.addView(chip);
			if (methods.getChildCount() == 3) break;
		}
	}

	private void showOrders(List<JSONObject> mine) {
		BottomSheetDialog d = ThemedDialogs.sheet(this);
		View v = getLayoutInflater().inflate(R.layout.sheet_p2p_list, null);
		((TextView) v.findViewById(R.id.tv_sheet_title)).setText("Orders on this offer");
		LinearLayout rows = v.findViewById(R.id.ll_sheet_rows);
		for (JSONObject o : mine) {
			String asset = P2pAsset.of(o), status = o.optString("status");
			boolean buyer = myAddress.equals(o.optString("buyerAddress"));
			View r = getLayoutInflater().inflate(R.layout.item_p2p_sheet_row, rows, false);
			ImageView icon = r.findViewById(R.id.iv_row_icon);
			icon.setVisibility(View.VISIBLE);
			P2pOffers.coinIcon(icon, asset);
			r.findViewById(R.id.tv_row_badge).setVisibility(View.GONE);
			((TextView) r.findViewById(R.id.tv_row_title)).setText(BalanceFormatter.smartDecimal(P2pAsset.fromRaw(asset, o.optLong("amount", 0)))
				+ " " + P2pAsset.ticker(asset));
			TextView sub = r.findViewById(R.id.tv_row_subtitle);
			sub.setText((needsMe(o) ? (buyer ? "Pay now" : "Release now") : P2pStatus.label(status)) + " · " + time(o.optLong("createdTimestamp", 0)));
			sub.setTextColor(ContextCompat.getColor(this, needsMe(o) ? R.color.colorAmber : P2pStatus.color(status)));
			r.setOnClickListener(x -> {
				d.dismiss();
				Intent i = new Intent(this, buyer ? P2pBuyTradeActivity.class : P2pSellTradeActivity.class);
				i.putExtra("orderId", P2pIds.orderId(o));
				startActivity(i);
			});
			rows.addView(r);
		}
		d.setContentView(v);
		d.show();
	}

	private void confirmCancel(String id, String asset, boolean sell, double left) {
		String tk = P2pAsset.ticker(asset);
		ArrayList<String[]> rows = new ArrayList<>();
		rows.add(new String[]{"Offer", (sell ? "Sell " : "Buy ") + tk});
		rows.add(new String[]{"Still listed", BalanceFormatter.smartDecimal(left) + " " + tk});
		if (sell) rows.add(new String[]{"Returns to", "Your wallet"});
		rows.add(new String[]{"Open orders", "Not affected"});
		com.harvana.app.group.PaymentConfirmSheet.reviewThenAuthorize(this, "Cancel offer", BalanceFormatter.smartDecimal(left) + " " + tk, rows, () ->
			P2PTradeRouter.cancelListing(getApplicationContext(), id, myAddress, new P2PTradeRouter.Callback() {
				@Override public void onSuccess(JSONObject result) {
					runOnUiThread(() -> { Toast.makeText(P2pMerchantManagerActivity.this, "Cancelled", Toast.LENGTH_SHORT).show(); load(); });
				}
				@Override public void onError(String error) {
					runOnUiThread(() -> Toast.makeText(P2pMerchantManagerActivity.this, com.harvana.app.utils.ErrorText.plain(error), Toast.LENGTH_LONG).show());
				}
			}));
	}

	private void editPrice(String id, String asset) {
		android.widget.EditText input = new android.widget.EditText(this);
		input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
		input.setHint("Price per " + P2pAsset.ticker(asset));
		int pad = Math.round(12 * getResources().getDisplayMetrics().density);
		input.setPadding(pad, pad, pad, pad);
		new com.harvana.app.ui.SheetDialog.Builder(this)
				.setTitle("Update price")
				.setMessage("Buyers see the new price straight away. Orders already open keep the price they agreed.")
				.setView(input)
				.setNegativeButton("Cancel", null)
				.setPositiveButton("Save price", (d, w) -> {
					double price;
					try { price = Double.parseDouble(input.getText().toString().trim()); }
					catch (Exception e) { price = 0; }
					if (price <= 0) {
						Toast.makeText(this, "Enter a price greater than zero.", Toast.LENGTH_SHORT).show();
						return;
					}
					final double next = price;
					com.harvana.app.firestore.P2PPaymentDetailsManager.getInstance(this).updateUnitPrice(id, next,
							new com.harvana.app.firestore.P2PPaymentDetailsManager.SimpleCallback() {
								@Override public void onSuccess() {
									runOnUiThread(() -> {
										java.util.Map<String, Object> card = cards.get(id);
										if (card != null) card.put("unitPrice", next);
										Toast.makeText(P2pMerchantManagerActivity.this, "Price updated", Toast.LENGTH_SHORT).show();
										if (list != null && list.getAdapter() != null) list.getAdapter().notifyDataSetChanged();
									});
								}
								@Override public void onError(String error) {
									runOnUiThread(() -> Toast.makeText(P2pMerchantManagerActivity.this,
											com.harvana.app.utils.ErrorText.plain(error), Toast.LENGTH_LONG).show());
								}
							});
				})
				.show();
	}

	private void newOffer() {
		BottomSheetDialog d = ThemedDialogs.sheet(this);
		View v = getLayoutInflater().inflate(R.layout.sheet_p2p_list, null);
		((TextView) v.findViewById(R.id.tv_sheet_title)).setText("New offer");
		LinearLayout rows = v.findViewById(R.id.ll_sheet_rows);
		String tk = P2pAsset.ticker(P2pAsset.get());
		for (int k = 0; k < 2; k++) {
			boolean sell = k == 0;
			View r = getLayoutInflater().inflate(R.layout.item_p2p_sheet_row, rows, false);
			ImageView icon = r.findViewById(R.id.iv_row_icon);
			icon.setVisibility(View.VISIBLE);
			P2pOffers.coinIcon(icon, P2pAsset.get());
			r.findViewById(R.id.tv_row_badge).setVisibility(View.GONE);
			TextView t = r.findViewById(R.id.tv_row_title);
			t.setText((sell ? "Sell " : "Buy ") + tk);
			t.setTextColor(ContextCompat.getColor(this, sell ? R.color.p2pSell : R.color.p2pBuy));
			r.findViewById(R.id.tv_row_subtitle).setVisibility(View.GONE);
			r.setOnClickListener(x -> {
				d.dismiss();
				startActivity(new Intent(this, sell ? CreateP2pSellOfferActivity.class : CreateP2pBuyOfferActivity.class));
			});
			rows.addView(r);
		}
		d.setContentView(v);
		d.show();
	}
}
