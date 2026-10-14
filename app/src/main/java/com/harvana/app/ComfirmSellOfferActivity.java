package com.harvana.app;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.harvana.app.p2p.P2pAsset;
import com.harvana.app.p2p.P2pKv;
import com.harvana.app.p2p.P2pListingPublisher;
import com.harvana.app.p2p.P2pMoney;
import com.harvana.app.p2p.P2pOffers;
import com.harvana.app.p2p.P2pPriceReference;
import com.harvana.app.p2p.PaymentMethodRows;
import com.harvana.app.p2p.ThemedDialogs;
import com.harvana.app.wallet.BalanceFormatter;
import com.harvana.app.wallet.PaymentMethod;
import com.harvana.app.wallet.PaymentMethodManager;
import com.harvana.app.wallet.WalletManager;

import java.util.ArrayList;
import java.util.List;

/** Publish a buy or sell offer from the create screens (res/layout/p2p_confirm_offer.xml). Extras come from P2pOfferWizard. */
public class ComfirmSellOfferActivity extends AppCompatActivity {

	private double amount, unitPrice, minOrderAmount, maxOrderAmount, marginPercent;
	private long durationHours;
	private int paymentWindowMinutes = 15, reqAccountAgeDays;
	private String side = "SELL", asset = P2pAsset.HARV, durationLabel, currency = "NGN", pricingType = "FIXED", termsOfTrade = "", address;
	private final List<PaymentMethod> methods = new ArrayList<>();
	private MaterialButton publish;
	private TextView tvError;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.p2p_confirm_offer);
		com.harvana.app.utils.StatusBarUtil.matchToolbar(this);
		Intent in = getIntent();
		side = "BUY".equals(in.getStringExtra("side")) ? "BUY" : "SELL";
		asset = P2pAsset.normalize(in.getStringExtra("asset"));
		amount = in.getDoubleExtra("amount", 0);
		unitPrice = in.getDoubleExtra("unitPrice", 0);
		minOrderAmount = in.getDoubleExtra("minOrderAmount", amount);
		maxOrderAmount = in.getDoubleExtra("maxOrderAmount", amount);
		durationHours = in.getLongExtra("durationHours", 168L);
		durationLabel = in.getStringExtra("durationLabel");
		if (in.getStringExtra("currency") != null) currency = in.getStringExtra("currency");
		if (in.getStringExtra("pricingType") != null) pricingType = in.getStringExtra("pricingType");
		marginPercent = in.getDoubleExtra("marginPercent", 0.0);
		paymentWindowMinutes = in.getIntExtra("paymentWindowMinutes", 15);
		if (in.getStringExtra("termsOfTrade") != null) termsOfTrade = in.getStringExtra("termsOfTrade");
		reqAccountAgeDays = in.getIntExtra("reqAccountAgeDays", 0);
		address = WalletManager.getActiveWalletAddress(this);
		List<String> ids = in.getStringArrayListExtra("selectedPaymentMethodIds");
		if (ids != null) for (PaymentMethod pm : PaymentMethodManager.loadAll(this)) if (ids.contains(pm.id)) methods.add(pm);

		publish = findViewById(R.id.btn_publish_offer);
		tvError = findViewById(R.id.tv_confirm_error);
		findViewById(R.id.iv_confirm_back).setOnClickListener(v -> finish());
		findViewById(R.id.btn_confirm_edit).setOnClickListener(v -> finish());
		publish.setOnClickListener(v -> onPublish());
		render();
	}

	private boolean sell() { return "SELL".equals(side); }

	private void render() {
		String tk = P2pAsset.ticker(asset);
		int color = ContextCompat.getColor(this, sell() ? R.color.p2pSell : R.color.p2pBuy);
		((TextView) findViewById(R.id.tv_confirm_title)).setText(sell() ? "Publish sell offer" : "Publish buy offer");
		P2pOffers.coinIcon(findViewById(R.id.iv_confirm_coin), asset);
		TextView sideView = findViewById(R.id.tv_confirm_side);
		sideView.setText((sell() ? "Sell " : "Buy ") + tk);
		sideView.setTextColor(color);
		((TextView) findViewById(R.id.tv_confirm_amount)).setText(BalanceFormatter.smartDecimal(amount) + " " + tk);
		publish.setBackgroundTintList(ColorStateList.valueOf(color));
		publish.setTextColor(ContextCompat.getColor(this, sell() ? R.color.p2pOnSell : R.color.p2pOnBuy));

		LinearLayout rows = findViewById(R.id.ll_confirm_rows);
		P2pKv.add(rows, "Price", P2pMoney.perUnit(unitPrice, currency, asset));
		P2pKv.add(rows, "Pricing", "MARGIN".equals(pricingType) ? String.format(java.util.Locale.US, "Floating %+.2f%%", marginPercent) : "Fixed");
		P2pKv.add(rows, sell() ? "Buyers pay you" : "You pay", P2pMoney.fiat(amount * unitPrice, currency));
		P2pKv.add(rows, "Trade limit", BalanceFormatter.smartDecimal(minOrderAmount) + " – " + BalanceFormatter.smartDecimal(maxOrderAmount) + " " + tk);
		P2pKv.add(rows, "Offer duration", durationLabel != null ? durationLabel : durationHours + " h");
		P2pKv.add(rows, "Payment window", paymentWindowMinutes + " min");
		P2pKv.add(rows, "Minimum account age", reqAccountAgeDays > 0 ? reqAccountAgeDays + " days" : "Any");
		P2pKv.add(rows, "Maker fee", "0.5% per trade");
		marketReference(rows);

		LinearLayout pay = findViewById(R.id.layout_confirm_payment_methods);
		if (methods.isEmpty()) P2pKv.add(pay, "Methods", "");
		for (PaymentMethod pm : methods) PaymentMethodRows.addReadOnly(this, pay, pm);
		if (!termsOfTrade.isEmpty()) {
			findViewById(R.id.ll_confirm_terms).setVisibility(View.VISIBLE);
			((TextView) findViewById(R.id.tv_confirm_terms)).setText(termsOfTrade);
		}
	}

	/** HARV in Naira shows the swap-pool band. Outside it, the offer will not be published. */
	private void marketReference(LinearLayout rows) {
		if (P2pAsset.isStable(asset) || !"NGN".equalsIgnoreCase(currency) || unitPrice <= 0) return;
		TextView v = P2pKv.add(rows, "Swap pool", "…");
		long mine = "MARGIN".equals(pricingType) ? 0L : P2pPriceReference.nairaToKobo(unitPrice);
		P2pPriceReference.fetchQuote(q -> {
			if (isFinishing() || isDestroyed()) return;
			if (q.pool && q.midKobo > 0) {
				long priced = "MARGIN".equals(pricingType)
					? Math.max(1L, Math.round(q.midKobo * (1.0 + marginPercent / 100.0)))
					: mine;
				boolean outside = !P2pPriceReference.allowed(priced, q.midKobo)
					|| ("MARGIN".equals(pricingType) && (marginPercent < -10.0 || marginPercent > 10.0));
				v.setText(P2pMoney.perUnit(q.midKobo / 100.0, "NGN", asset)
					+ " · " + P2pMoney.perUnit(q.floorKobo / 100.0, "NGN", asset)
					+ " to " + P2pMoney.perUnit(q.ceilKobo / 100.0, "NGN", asset)
					+ (outside ? " · outside the band" : ""));
				if (outside) v.setTextColor(ContextCompat.getColor(this, R.color.colorWarning));
				return;
			}
			if (q.midKobo <= 0 || "MARGIN".equals(pricingType)) { v.setText("—"); return; }
			double dev = P2pPriceReference.deviation(mine, q.midKobo);
			v.setText(P2pMoney.perUnit(q.midKobo / 100.0, "NGN", asset)
				+ String.format(java.util.Locale.US, " · %+d%%", Math.round(dev * 100)));
			if (Math.abs(dev) > P2pPriceReference.DEVIATION_FLAG) v.setTextColor(ContextCompat.getColor(this, R.color.colorWarning));
		});
	}

	private void showError(String text) {
		tvError.setText(text);
		tvError.setVisibility(text == null ? View.GONE : View.VISIBLE);
	}

	private void onPublish() {
		if (address == null || address.isEmpty()) { showError("No wallet on this device"); return; }
		if (sell() && methods.isEmpty()) { showError("Pick at least one payment method"); return; }
		if (FirebaseAuth.getInstance().getCurrentUser() == null) { showError("Sign in to publish"); return; }
		showError(null);
		publish.setEnabled(false);
		publish.setText("Publishing…");
		P2pListingPublisher.publish(this, asset, side, amount, pricingType, unitPrice, marginPercent, minOrderAmount, maxOrderAmount,
			durationHours, paymentWindowMinutes, termsOfTrade, reqAccountAgeDays, currency, methods, new P2pListingPublisher.Callback() {
				@Override public void onPublished(String listingId) {
					Toast.makeText(ComfirmSellOfferActivity.this, "Published", Toast.LENGTH_SHORT).show();
					Intent i = new Intent(ComfirmSellOfferActivity.this, P2pMerchantManagerActivity.class);
					i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(i);
					finish();
				}
				@Override public void onPartial(String listingId, String error) {
					publish.setText("Publish");
					ThemedDialogs.show(ThemedDialogs.alert(ComfirmSellOfferActivity.this)
						.setTitle("Offer live, payment details not saved")
						.setMessage(com.harvana.app.utils.ErrorText.plain(error))
						.setPositiveButton("OK", (d, w) -> {
							startActivity(new Intent(ComfirmSellOfferActivity.this, P2pMerchantManagerActivity.class));
							finish();
						}));
				}
				@Override public void onError(String error) {
					publish.setEnabled(true);
					publish.setText("Publish");
					showError(friendly(error));
				}
			});
	}

	/** The node's refusals as plain text. */
	private String friendly(String raw) {
		String tk = P2pAsset.ticker(asset);
		if (raw == null) return "Not published";
		if (com.harvana.app.utils.ErrorText.is(raw, "AMOUNT_OUT_OF_BOUNDS")) return "An offer is " + BalanceFormatter.smartDecimal(P2pAsset.minListing(asset))
			+ " – " + BalanceFormatter.smartDecimal(P2pAsset.maxListing(asset)) + " " + tk;
		if (com.harvana.app.utils.ErrorText.is(raw, "_NOT_ACTIVE")) return tk + " is not on for trading";
		if (com.harvana.app.utils.ErrorText.is(raw, "INVALID_MIN_ORDER_AMOUNT")) return "Minimum trade " + BalanceFormatter.smartDecimal(P2pAsset.minOrder(asset)) + " " + tk + " or more, up to the total";
		if (com.harvana.app.utils.ErrorText.is(raw, "INVALID_DURATION")) return "Offer duration: 1 hour to 30 days";
		if (com.harvana.app.utils.ErrorText.is(raw, "COIN_PAUSED")) return tk + " trading is paused";
		if (com.harvana.app.utils.ErrorText.is(raw, "INSUFFICIENT_AVAILABLE_BALANCE_FOR_LISTING")) return "Not enough " + tk + " available";
		return com.harvana.app.utils.ErrorText.plain(raw);
	}
}
