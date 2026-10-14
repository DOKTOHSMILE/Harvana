package com.harvana.app;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.harvana.app.banks.BankLogo;
import com.harvana.app.p2p.P2pAsset;
import com.harvana.app.p2p.P2pCurrencyFilterState;
import com.harvana.app.p2p.P2pMoney;
import com.harvana.app.p2p.P2pOffers;
import com.harvana.app.p2p.ThemedDialogs;
import com.harvana.app.wallet.BalanceFormatter;
import com.harvana.app.wallet.WalletManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The P2P market. Buy shows SELL offers (you buy the coin from the poster); Sell shows BUY offers (you sell to the
 * poster). One coin at a time (HARV or a stable coin the network has on) and one fiat currency; filters run on the
 * loaded offers. Tapping an offer opens it to start an order.
 */
public class P2pMarketplaceActivity extends AppCompatActivity {

	private static final String[][] CURRENCIES = {
		{"NGN", "Nigerian Naira"}, {"USD", "US Dollar"}, {"EUR", "Euro"}, {"GBP", "British Pound"},
		{"GHS", "Ghanaian Cedi"}, {"KES", "Kenyan Shilling"}, {"ZAR", "South African Rand"},
	};

	private boolean sellMode;
	private final List<P2pOffers.Offer> all = new ArrayList<>();
	private final List<P2pOffers.Offer> shown = new ArrayList<>();
	private String myAddress = "";
	private int loadSeq;

	// Filters
	private Double amount;
	private final Set<String> methods = new LinkedHashSet<>();
	private boolean verifiedOnly, insuredOnly, premiumOnly, onlineOnly;
	private int sort; // 0 price, 1 trades, 2 completion, 3 trust

	private TextView tabBuy, tabSell, tvCurrency, tvAmount, tvPayment, tvEmptyTitle, dotOrders;
	private View btnAmount, btnPayment, btnFilters, emptyView;
	private MaterialButton btnEmpty;
	private ChipGroup coinChips;
	private SwipeRefreshLayout refresh;
	private RecyclerView list;
	private final OfferAdapter adapter = new OfferAdapter();

	private final P2pAsset.Listener assetListener = code -> runOnUiThread(() -> { buildCoinChips(); load(); });
	private final P2pCurrencyFilterState.Listener currencyListener = code -> runOnUiThread(() -> {
		tvCurrency.setText(code);
		applyFilters();
	});

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.p2p_marketplace);
		com.harvana.app.utils.StatusBarUtil.matchToolbar(this);
		myAddress = WalletManager.getActiveWalletAddress(this);
		if (myAddress == null) myAddress = "";
		sellMode = "SELL".equals(getIntent().getStringExtra("side"));

		tabBuy = findViewById(R.id.tab_p2p_buy);
		tabSell = findViewById(R.id.tab_p2p_sell);
		tvCurrency = findViewById(R.id.tv_p2p_currency);
		tvAmount = findViewById(R.id.tv_p2p_filter_amount);
		tvPayment = findViewById(R.id.tv_p2p_filter_payment);
		btnAmount = findViewById(R.id.btn_p2p_filter_amount);
		btnPayment = findViewById(R.id.btn_p2p_filter_payment);
		btnFilters = findViewById(R.id.btn_p2p_filters);
		coinChips = findViewById(R.id.chips_p2p_coins);
		refresh = findViewById(R.id.refresh_p2p_offers);
		list = findViewById(R.id.rv_p2p_offers);
		emptyView = findViewById(R.id.layout_p2p_empty);
		tvEmptyTitle = findViewById(R.id.tv_p2p_empty_title);
		btnEmpty = findViewById(R.id.btn_p2p_empty_action);
		dotOrders = findViewById(R.id.tv_open_trades_dot);

		list.setLayoutManager(new LinearLayoutManager(this));
		list.setAdapter(adapter);
		refresh.setColorSchemeColors(ContextCompat.getColor(this, R.color.colorAccent));
		refresh.setOnRefreshListener(this::load);

		findViewById(R.id.iv_p2p_back).setOnClickListener(v -> finish());
		findViewById(R.id.btn_p2p_currency).setOnClickListener(v -> showCurrencySheet());
		findViewById(R.id.iv_p2p_express).setOnClickListener(v -> startActivity(new Intent(this, P2pExpressActivity.class)));
		findViewById(R.id.btn_p2p_orders).setOnClickListener(v -> startActivity(new Intent(this, P2pMyTradesActivity.class)));
		findViewById(R.id.iv_p2p_my_ads).setOnClickListener(v -> startActivity(new Intent(this, P2pMerchantManagerActivity.class)));
		findViewById(R.id.fab_p2p_new_offer).setOnClickListener(v -> showNewOfferSheet());
		tabBuy.setOnClickListener(v -> setSide(false));
		tabSell.setOnClickListener(v -> setSide(true));
		btnAmount.setOnClickListener(v -> showFilters(true));
		btnPayment.setOnClickListener(v -> showFilters(false));
		btnFilters.setOnClickListener(v -> showFilters(false));

		tvCurrency.setText(P2pCurrencyFilterState.getCurrency());
		paintSide();
		buildCoinChips();
		P2pAsset.loadActive(() -> runOnUiThread(this::buildCoinChips));
		load();
	}

	@Override
	protected void onStart() {
		super.onStart();
		P2pAsset.addListener(assetListener);
		P2pCurrencyFilterState.addListener(currencyListener);
	}

	@Override
	protected void onStop() {
		super.onStop();
		P2pAsset.removeListener(assetListener);
		P2pCurrencyFilterState.removeListener(currencyListener);
	}

	@Override
	protected void onResume() {
		super.onResume();
		refreshOrdersDot();
	}

	// ── Side and coin ────────────────────────────────────────────────────

	private void setSide(boolean sell) {
		if (sell == sellMode) return;
		sellMode = sell;
		paintSide();
		load();
	}

	private void paintSide() {
		int buy = ContextCompat.getColor(this, R.color.p2pBuy), onBuy = ContextCompat.getColor(this, R.color.p2pOnBuy);
		int sell = ContextCompat.getColor(this, R.color.p2pSell), onSell = ContextCompat.getColor(this, R.color.p2pOnSell);
		int idle = ContextCompat.getColor(this, R.color.colorTextSecondary);
		styleTab(tabBuy, !sellMode, buy, onBuy, idle);
		styleTab(tabSell, sellMode, sell, onSell, idle);
	}

	private void styleTab(TextView t, boolean on, int bg, int fg, int idle) {
		if (on) {
			android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
			d.setColor(bg);
			d.setCornerRadius(dp(11));
			t.setBackground(d);
			t.setTextColor(fg);
		} else {
			t.setBackground(null);
			t.setTextColor(idle);
		}
	}

	private void buildCoinChips() { com.harvana.app.p2p.P2pCoinChips.build(coinChips); }

	// ── Loading and filtering ────────────────────────────────────────────

	private void load() {
		final int seq = ++loadSeq;
		refresh.setRefreshing(true);
		// Buy tab: offers whose poster sells (SELL listings). Sell tab: offers whose poster buys (BUY listings).
		P2pOffers.load(this, sellMode ? "BUY" : "SELL", P2pAsset.get(), new P2pOffers.Callback() {
			@Override public void loaded(List<P2pOffers.Offer> offers) {
				if (seq != loadSeq || isFinishing()) return;
				refresh.setRefreshing(false);
				all.clear();
				all.addAll(offers);
				applyFilters();
			}
			@Override public void failed(String error) {
				if (seq != loadSeq || isFinishing()) return;
				refresh.setRefreshing(false);
				all.clear();
				shown.clear();
				adapter.notifyDataSetChanged();
				showEmpty(com.harvana.app.utils.ErrorText.plain(error), "Retry", v -> load());
			}
		});
	}

	private List<P2pOffers.Offer> filtered(Double amt, Set<String> m, boolean ver, boolean ins, boolean prem, boolean onl) {
		List<P2pOffers.Offer> out = new ArrayList<>();
		String ccy = P2pCurrencyFilterState.getCurrency();
		for (P2pOffers.Offer o : all) {
			if (!myAddress.isEmpty() && myAddress.equals(o.posterAddress)) continue;     // never your own offer
			if (o.unitPrice <= 0 || o.remaining <= 0) continue;
			if (!o.currency.equalsIgnoreCase(ccy)) continue;
			if (amt != null && (amt < o.minOrder || amt > o.remaining)) continue;
			if (!m.isEmpty() && Collections.disjoint(o.paymentLabels, m)) continue;
			if (ver && !o.verified) continue;
			if (ins && !o.insured) continue;
			if (prem && !o.premium) continue;
			if (onl && !o.online) continue;
			out.add(o);
		}
		return out;
	}

	private void applyFilters() {
		shown.clear();
		shown.addAll(filtered(amount, methods, verifiedOnly, insuredOnly, premiumOnly, onlineOnly));
		final boolean buyer = !sellMode;
		switch (sort) {
			case 1: Collections.sort(shown, (a, b) -> Integer.compare(b.completedTrades, a.completedTrades)); break;
			case 2: Collections.sort(shown, (a, b) -> Double.compare(b.completionPercent, a.completionPercent)); break;
			case 3: Collections.sort(shown, (a, b) -> Integer.compare(b.trustScore, a.trustScore)); break;
			// Best price: the cheapest seller for a buyer, the highest-paying buyer for a seller.
			default: Collections.sort(shown, (a, b) -> buyer ? Double.compare(a.unitPrice, b.unitPrice) : Double.compare(b.unitPrice, a.unitPrice));
		}
		adapter.notifyDataSetChanged();

		String tk = P2pAsset.ticker(P2pAsset.get());
		tvAmount.setText(amount != null ? BalanceFormatter.smartDecimal(amount) + " " + tk : "Amount");
		btnAmount.setActivated(amount != null);
		tvPayment.setText(methods.isEmpty() ? "Payment" : methods.size() == 1 ? methods.iterator().next() : "Payment · " + methods.size());
		btnPayment.setActivated(!methods.isEmpty());
		btnFilters.setActivated(verifiedOnly || insuredOnly || premiumOnly || onlineOnly || sort != 0);

		if (shown.isEmpty()) {
			boolean filtering = amount != null || !methods.isEmpty() || verifiedOnly || insuredOnly || premiumOnly || onlineOnly;
			if (filtering) showEmpty("No offers match", "Reset filters", v -> { resetFilters(); applyFilters(); });
			else showEmpty(sellMode ? "No buy offers for " + tk : "No sell offers for " + tk,
				sellMode ? "Post a sell offer" : "Post a buy offer",
				v -> startActivity(new Intent(this, sellMode ? CreateP2pSellOfferActivity.class : CreateP2pBuyOfferActivity.class)));
		} else {
			emptyView.setVisibility(View.GONE);
			list.setVisibility(View.VISIBLE);
		}
	}

	private void showEmpty(String title, String action, View.OnClickListener click) {
		list.setVisibility(View.GONE);
		emptyView.setVisibility(View.VISIBLE);
		tvEmptyTitle.setText(title);
		btnEmpty.setText(action);
		btnEmpty.setOnClickListener(click);
	}

	private void resetFilters() {
		amount = null;
		methods.clear();
		verifiedOnly = insuredOnly = premiumOnly = onlineOnly = false;
		sort = 0;
	}

	// ── Sheets ───────────────────────────────────────────────────────────

	private void showFilters(boolean focusAmount) {
		BottomSheetDialog d = ThemedDialogs.sheet(this);
		View v = getLayoutInflater().inflate(R.layout.sheet_p2p_filters, null);
		TextInputLayout til = v.findViewById(R.id.til_filter_amount);
		TextInputEditText et = v.findViewById(R.id.et_filter_amount);
		ChipGroup methodChips = v.findViewById(R.id.chips_filter_methods);
		SwitchMaterial swVer = v.findViewById(R.id.sw_filter_verified), swIns = v.findViewById(R.id.sw_filter_insured),
			swPrem = v.findViewById(R.id.sw_filter_premium), swOnl = v.findViewById(R.id.sw_filter_online);
		ChipGroup sortChips = v.findViewById(R.id.chips_filter_sort);
		MaterialButton apply = v.findViewById(R.id.btn_filters_apply);

		til.setSuffixText(P2pAsset.ticker(P2pAsset.get()));
		if (amount != null) et.setText(BalanceFormatter.smartDecimal(amount));
		swVer.setChecked(verifiedOnly);
		swIns.setChecked(insuredOnly);
		swPrem.setChecked(premiumOnly);
		swOnl.setChecked(onlineOnly);
		int[] sortIds = {R.id.chip_sort_price, R.id.chip_sort_trades, R.id.chip_sort_completion, R.id.chip_sort_trust};
		sortChips.check(sortIds[sort]);

		Set<String> picked = new LinkedHashSet<>(methods);
		Set<String> available = new LinkedHashSet<>();
		for (P2pOffers.Offer o : all) available.addAll(o.paymentLabels);
		available.addAll(methods);
		v.findViewById(R.id.tv_filter_methods_none).setVisibility(available.isEmpty() ? View.VISIBLE : View.GONE);

		Runnable recount = () -> {
			double a = BalanceFormatter.parseUserInput(et.getText() == null ? "" : et.getText().toString());
			int n = filtered(a > 0 ? a : null, picked, swVer.isChecked(), swIns.isChecked(), swPrem.isChecked(), swOnl.isChecked()).size();
			apply.setText("Show " + n + (n == 1 ? " offer" : " offers"));
		};
		for (String label : available) {
			Chip c = new Chip(this);
			c.setText(label);
			c.setCheckable(true);
			c.setChecked(picked.contains(label));
			c.setChipIconVisible(true);
			c.setChipIconSize(dp(18));
			BankLogo.intoChip(c, label);
			c.setOnCheckedChangeListener((b, on) -> { if (on) picked.add(label); else picked.remove(label); recount.run(); });
			methodChips.addView(c);
		}
		CompoundListener sw = (b, on) -> recount.run();
		swVer.setOnCheckedChangeListener(sw::changed);
		swIns.setOnCheckedChangeListener(sw::changed);
		swPrem.setOnCheckedChangeListener(sw::changed);
		swOnl.setOnCheckedChangeListener(sw::changed);
		et.addTextChangedListener(new TextWatcher() {
			@Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
			@Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
			@Override public void afterTextChanged(Editable s) { recount.run(); }
		});
		recount.run();

		v.findViewById(R.id.iv_filters_close).setOnClickListener(x -> d.dismiss());
		v.findViewById(R.id.btn_filters_reset).setOnClickListener(x -> {
			et.setText("");
			picked.clear();
			for (int i = 0; i < methodChips.getChildCount(); i++) ((Chip) methodChips.getChildAt(i)).setChecked(false);
			swVer.setChecked(false);
			swIns.setChecked(false);
			swPrem.setChecked(false);
			swOnl.setChecked(false);
			sortChips.check(R.id.chip_sort_price);
			recount.run();
		});
		apply.setOnClickListener(x -> {
			double a = BalanceFormatter.parseUserInput(et.getText() == null ? "" : et.getText().toString());
			amount = a > 0 ? a : null;
			methods.clear();
			methods.addAll(picked);
			verifiedOnly = swVer.isChecked();
			insuredOnly = swIns.isChecked();
			premiumOnly = swPrem.isChecked();
			onlineOnly = swOnl.isChecked();
			int checked = sortChips.getCheckedChipId();
			sort = checked == R.id.chip_sort_trades ? 1 : checked == R.id.chip_sort_completion ? 2 : checked == R.id.chip_sort_trust ? 3 : 0;
			applyFilters();
			d.dismiss();
		});
		d.setContentView(v);
		d.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
		if (focusAmount) {
			et.requestFocus();
			if (d.getWindow() != null) d.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
				| android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
		}
		d.show();
	}

	private interface CompoundListener { void changed(android.widget.CompoundButton b, boolean on); }

	private void showCurrencySheet() {
		BottomSheetDialog d = ThemedDialogs.sheet(this);
		View v = getLayoutInflater().inflate(R.layout.sheet_p2p_list, null);
		((TextView) v.findViewById(R.id.tv_sheet_title)).setText("Currency");
		LinearLayout rows = v.findViewById(R.id.ll_sheet_rows);
		String current = P2pCurrencyFilterState.getCurrency();
		for (String[] c : CURRENCIES) {
			View r = getLayoutInflater().inflate(R.layout.item_p2p_sheet_row, rows, false);
			((TextView) r.findViewById(R.id.tv_row_badge)).setText(P2pMoney.symbol(c[0]));
			((TextView) r.findViewById(R.id.tv_row_title)).setText(c[0]);
			((TextView) r.findViewById(R.id.tv_row_subtitle)).setText(c[1]);
			r.findViewById(R.id.iv_row_check).setVisibility(c[0].equals(current) ? View.VISIBLE : View.GONE);
			r.setOnClickListener(x -> {
				d.dismiss();
				P2pCurrencyFilterState.setCurrency(c[0]);   // the listener re-filters
			});
			rows.addView(r);
		}
		d.setContentView(v);
		d.show();
	}

	private void showNewOfferSheet() {
		BottomSheetDialog d = ThemedDialogs.sheet(this);
		View v = getLayoutInflater().inflate(R.layout.sheet_p2p_list, null);
		((TextView) v.findViewById(R.id.tv_sheet_title)).setText("New offer");
		LinearLayout rows = v.findViewById(R.id.ll_sheet_rows);
		String tk = P2pAsset.ticker(P2pAsset.get());
		addOfferRow(d, rows, "Sell " + tk, "Buyers pay you " + P2pCurrencyFilterState.getCurrency(), CreateP2pSellOfferActivity.class, R.color.p2pSell);
		addOfferRow(d, rows, "Buy " + tk, "You pay sellers " + P2pCurrencyFilterState.getCurrency(), CreateP2pBuyOfferActivity.class, R.color.p2pBuy);
		d.setContentView(v);
		d.show();
	}

	private void addOfferRow(BottomSheetDialog d, LinearLayout rows, String title, String sub, Class<?> target, int color) {
		View r = getLayoutInflater().inflate(R.layout.item_p2p_sheet_row, rows, false);
		ImageView icon = r.findViewById(R.id.iv_row_icon);
		icon.setVisibility(View.VISIBLE);
		P2pOffers.coinIcon(icon, P2pAsset.get());
		r.findViewById(R.id.tv_row_badge).setVisibility(View.GONE);
		TextView t = r.findViewById(R.id.tv_row_title);
		t.setText(title);
		t.setTextColor(ContextCompat.getColor(this, color));
		((TextView) r.findViewById(R.id.tv_row_subtitle)).setText(sub);
		r.setOnClickListener(x -> { d.dismiss(); startActivity(new Intent(this, target)); });
		rows.addView(r);
	}

	/** Red count on My orders: orders waiting for payment, for release, or under appeal. */
	private void refreshOrdersDot() {
		if (myAddress.isEmpty()) { dotOrders.setVisibility(View.GONE); return; }
		com.harvana.app.tx.P2PTradeRouter.listMine(getApplicationContext(), myAddress, new com.harvana.app.tx.P2PTradeRouter.Callback() {
			@Override public void onSuccess(org.json.JSONObject result) {
				int open = 0;
				org.json.JSONArray arr = result.optJSONArray("listings");
				for (int k = 0; arr != null && k < arr.length(); k++) {
					org.json.JSONObject e = arr.optJSONObject(k);
					if (e == null || !"ORDER".equals(e.optString("recordType", ""))) continue;
					String st = e.optString("status", "");
					if ("AWAITING_PAYMENT".equals(st) || "PAYMENT_SUBMITTED".equals(st) || "DISPUTED".equals(st)) open++;
				}
				final int n = open;
				runOnUiThread(() -> {
					dotOrders.setText(n > 9 ? "9+" : String.valueOf(n));
					dotOrders.setVisibility(n > 0 ? View.VISIBLE : View.GONE);
				});
			}
			@Override public void onError(String error) { }
		});
	}

	private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

	// ── Offer rows (res/layout/item_p2p_offer.xml) ───────────────────────

	private final class OfferAdapter extends RecyclerView.Adapter<OfferAdapter.Holder> {

		final class Holder extends RecyclerView.ViewHolder {
			final TextView initial, name, stats, badge, price, unit, window, available, limit;
			final ImageView avatar, verified;
			final View online;
			final LinearLayout methodsRow;
			final MaterialButton action;

			Holder(View v) {
				super(v);
				initial = v.findViewById(R.id.tv_offer_initial);
				avatar = v.findViewById(R.id.iv_offer_avatar);
				online = v.findViewById(R.id.v_offer_online);
				name = v.findViewById(R.id.tv_offer_name);
				verified = v.findViewById(R.id.iv_offer_verified);
				stats = v.findViewById(R.id.tv_offer_stats);
				badge = v.findViewById(R.id.tv_offer_badge);
				price = v.findViewById(R.id.tv_offer_price);
				unit = v.findViewById(R.id.tv_offer_price_unit);
				window = v.findViewById(R.id.tv_offer_window);
				available = v.findViewById(R.id.tv_offer_available);
				limit = v.findViewById(R.id.tv_offer_limit);
				methodsRow = v.findViewById(R.id.ll_offer_methods);
				action = v.findViewById(R.id.btn_offer_action);
			}
		}

		@NonNull @Override
		public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
			return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_p2p_offer, parent, false));
		}

		@Override
		public void onBindViewHolder(@NonNull Holder h, int position) {
			P2pOffers.Offer o = shown.get(position);
			String tk = P2pAsset.ticker(o.asset);
			h.name.setText(o.displayName);
			if (o.iconUrl.startsWith("http")) {
				h.avatar.setVisibility(View.VISIBLE);
				h.initial.setVisibility(View.INVISIBLE);
				Glide.with(h.avatar.getContext()).load(Uri.parse(o.iconUrl)).circleCrop().into(h.avatar);
			} else {
				h.avatar.setVisibility(View.GONE);
				h.initial.setVisibility(View.VISIBLE);
				h.initial.setText(o.displayName.isEmpty() ? "?" : o.displayName.substring(0, 1).toUpperCase(Locale.US));
			}
			h.online.setVisibility(o.online ? View.VISIBLE : View.GONE);
			h.verified.setVisibility(o.verified ? View.VISIBLE : View.GONE);
			StringBuilder st = new StringBuilder(o.completedTrades + (o.completedTrades == 1 ? " trade" : " trades"));
			if (o.completionPercent >= 0) st.append(" · ").append(String.format(Locale.US, "%.1f%%", o.completionPercent));
			if (o.trustScore >= 0) st.append(" · Trust ").append(o.trustLabel);
			h.stats.setText(st);
			String badge = o.premium ? "Premium" : o.insured ? "Insured" : "";
			h.badge.setText(badge);
			h.badge.setVisibility(badge.isEmpty() ? View.GONE : View.VISIBLE);

			h.price.setText(P2pMoney.fiat(o.unitPrice, o.currency));
			h.unit.setText("/ " + tk);
			h.window.setText(o.paymentWindowMinutes > 0 ? o.paymentWindowMinutes + " min" : "");
			h.window.setVisibility(o.paymentWindowMinutes > 0 ? View.VISIBLE : View.GONE);
			h.available.setText(BalanceFormatter.smartDecimal(o.remaining) + " " + tk);
			h.limit.setText(P2pMoney.fiat(o.minOrder * o.unitPrice, o.currency) + " – " + P2pMoney.fiat(o.remaining * o.unitPrice, o.currency));

			h.methodsRow.removeAllViews();
			int count = 0;
			for (String label : o.paymentLabels) {
				if (count == 2) {
					TextView more = new TextView(h.methodsRow.getContext());
					more.setText("+" + (o.paymentLabels.size() - 2));
					more.setTextSize(11);
					more.setTextColor(ContextCompat.getColor(more.getContext(), R.color.colorTextSecondary));
					h.methodsRow.addView(more);
					break;
				}
				View chip = LayoutInflater.from(h.methodsRow.getContext()).inflate(R.layout.item_p2p_method_chip, h.methodsRow, false);
				BankLogo.into((ImageView) chip.findViewById(R.id.iv_method_logo), label);
				((TextView) chip.findViewById(R.id.tv_method_name)).setText(label);
				h.methodsRow.addView(chip);
				count++;
			}

			boolean buying = "SELL".equals(o.side);      // a SELL offer is one you buy from
			int bg = ContextCompat.getColor(P2pMarketplaceActivity.this, buying ? R.color.p2pBuy : R.color.p2pSell);
			int fg = ContextCompat.getColor(P2pMarketplaceActivity.this, buying ? R.color.p2pOnBuy : R.color.p2pOnSell);
			h.action.setText((buying ? "Buy " : "Sell ") + tk);
			h.action.setBackgroundTintList(ColorStateList.valueOf(bg));
			h.action.setTextColor(fg);
			View.OnClickListener open = v -> {
				Intent i = new Intent(P2pMarketplaceActivity.this, buying ? P2pBuyOrderActivity.class : P2pSellOrderActivity.class);
				i.putExtra("listingId", o.listingId);
				startActivity(i);
			};
			h.action.setOnClickListener(open);
			h.itemView.setOnClickListener(open);
		}

		@Override
		public int getItemCount() { return shown.size(); }
	}
}
