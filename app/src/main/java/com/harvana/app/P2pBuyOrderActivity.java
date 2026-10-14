package com.harvana.app;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.harvana.app.banks.BankLogo;
import com.harvana.app.firestore.P2PPaymentDetailsManager;
import com.harvana.app.p2p.P2pAsset;
import com.harvana.app.p2p.P2pChatStore;
import com.harvana.app.p2p.P2pIds;
import com.harvana.app.p2p.P2pKv;
import com.harvana.app.p2p.P2pMoney;
import com.harvana.app.p2p.P2pOffers;
import com.harvana.app.p2p.PaymentMethodRows;
import com.harvana.app.sync.HarvanaNodeClient;
import com.harvana.app.tx.P2PTradeRouter;
import com.harvana.app.wallet.BalanceFormatter;
import com.harvana.app.wallet.PaymentMethod;
import com.harvana.app.wallet.PaymentMethodManager;
import com.harvana.app.wallet.WalletManager;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Take an offer (res/layout/p2p_take_offer.xml). Buying (this class) takes a SELL offer: you pay the poster in fiat
 * and the coin is released to you from escrow. Selling ({@link P2pSellOrderActivity}) takes a BUY offer: your coin
 * goes into escrow and the poster pays you into the account you pick here.
 *
 * Amounts can be typed in the coin or in fiat; the other box follows. The node previews the order (fee, tFare,
 * what the buyer receives) before the button turns on.
 */
public class P2pBuyOrderActivity extends AppCompatActivity {

	/** Overridden by {@link P2pSellOrderActivity}. */
	protected boolean selling() { return false; }

	private String listingId = "", myAddress = "", asset = P2pAsset.HARV, tk = "HARV";
	private P2pOffers.Offer offer;
	private double balance = -1;
	private PaymentMethod receiveInto;
	private List<PaymentMethod> myMethods = new ArrayList<>();
	private long previewedRaw = -1, feeRaw, gasRaw, netRaw;
	private boolean syncing;

	private final Handler handler = new Handler(Looper.getMainLooper());
	private Runnable pendingPreview;

	private EditText etCoin, etFiat;
	private TextView tvError, tvBalance;
	private LinearLayout offerRows, paymentHost, summary, presets;
	private View summaryCard;
	private MaterialButton confirm;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.p2p_take_offer);
		com.harvana.app.utils.StatusBarUtil.matchToolbar(this);
		listingId = getIntent().getStringExtra("listingId");
		myAddress = WalletManager.getActiveWalletAddress(this);
		if (myAddress == null) myAddress = "";

		etCoin = findViewById(R.id.et_take_coin);
		etFiat = findViewById(R.id.et_take_fiat);
		tvError = findViewById(R.id.tv_take_error);
		tvBalance = findViewById(R.id.tv_take_balance);
		offerRows = findViewById(R.id.ll_take_offer_rows);
		paymentHost = findViewById(R.id.ll_take_payment);
		summary = findViewById(R.id.ll_take_summary);
		summaryCard = findViewById(R.id.card_take_summary);
		presets = findViewById(R.id.ll_take_presets);
		confirm = findViewById(R.id.btn_take_confirm);

		findViewById(R.id.iv_take_back).setOnClickListener(v -> finish());
		findViewById(R.id.btn_take_max).setOnClickListener(v -> setCoin(maxCoin()));
		confirm.setOnClickListener(v -> confirm());
		etCoin.addTextChangedListener(watcher(true));
		etFiat.addTextChangedListener(watcher(false));
		paint();

		if (listingId == null || listingId.isEmpty()) { fatal("No offer"); return; }
		if (selling()) {
			myMethods = PaymentMethodManager.loadAll(this);
			for (PaymentMethod m : myMethods) if (m.isDefault) receiveInto = m;
			if (receiveInto == null && !myMethods.isEmpty()) receiveInto = myMethods.get(0);
		}
		load();
	}

	// ── Labels that depend on side and coin ──────────────────────────────

	private void paint() {
		boolean sell = selling();
		int color = ContextCompat.getColor(this, sell ? R.color.p2pSell : R.color.p2pBuy);
		int onColor = ContextCompat.getColor(this, sell ? R.color.p2pOnSell : R.color.p2pOnBuy);
		TextView title = findViewById(R.id.tv_take_title);
		title.setText((sell ? "Sell " : "Buy ") + tk);
		title.setTextColor(ContextCompat.getColor(this, R.color.colorTextPrimary));
		P2pOffers.coinIcon(findViewById(R.id.iv_take_coin), asset);
		((TextView) findViewById(R.id.tv_take_coin_label)).setText(sell ? "I sell" : "I receive");
		((TextView) findViewById(R.id.tv_take_fiat_label)).setText(sell ? "I receive" : "I pay");
		((TextView) findViewById(R.id.tv_take_coin_ticker)).setText(tk);
		((TextView) findViewById(R.id.tv_take_payment_title)).setText(sell ? "Receive payment into" : "Payment methods");
		confirm.setText((sell ? "Sell " : "Buy ") + tk);
		confirm.setBackgroundTintList(new ColorStateList(new int[][]{{-android.R.attr.state_enabled}, {}},
			new int[]{ContextCompat.getColor(this, R.color.colorCardStroke), color}));
		confirm.setTextColor(onColor);
	}

	// ── Load ─────────────────────────────────────────────────────────────

	private void load() {
		P2pOffers.loadOne(this, listingId, new P2pOffers.OneCallback() {
			@Override public void loaded(P2pOffers.Offer o) {
				if (isFinishing()) return;
				String status = o.raw.optString("status", "");
				if (!"OPEN".equals(status)) { fatal("This offer is closed"); return; }
				if (!(selling() ? "BUY" : "SELL").equals(o.side)) { fatal("This offer is on the other side"); return; }
				if (!myAddress.isEmpty() && myAddress.equals(o.posterAddress)) { fatal("This is your own offer"); return; }
				offer = o;
				asset = o.asset;
				tk = P2pAsset.ticker(asset);
				paint();
				bindOffer();
				if (selling()) loadBalance();
				else bindPresets();
				double want = getIntent().getDoubleExtra("coinAmount", 0);
				if (want > 0) setCoin(Math.min(want, o.remaining));
			}
			@Override public void failed(String error) { fatal(com.harvana.app.utils.ErrorText.plain(error)); }
		});
	}

	private void loadBalance() {
		if (myAddress.isEmpty()) return;
		P2pAsset.balance(this, asset, myAddress, new P2pAsset.BalanceCb() {
			@Override public void ok(double coins) {
				runOnUiThread(() -> {
					balance = coins;
					tvBalance.setText("Balance " + BalanceFormatter.smartDecimal(coins) + " " + tk);
					bindPresets();
					validate();
				});
			}
			@Override public void fail(String error) { }
		});
	}

	private void bindOffer() {
		P2pOffers.Offer o = offer;
		((TextView) findViewById(R.id.tv_take_name)).setText(o.displayName);
		TextView initial = findViewById(R.id.tv_take_initial);
		ImageView avatar = findViewById(R.id.iv_take_avatar);
		if (o.iconUrl.startsWith("http")) {
			avatar.setVisibility(View.VISIBLE);
			initial.setVisibility(View.INVISIBLE);
			Glide.with(this).load(Uri.parse(o.iconUrl)).circleCrop().into(avatar);
		} else {
			initial.setText(o.displayName.isEmpty() ? "?" : o.displayName.substring(0, 1).toUpperCase(Locale.US));
		}
		findViewById(R.id.v_take_online).setVisibility(o.online ? View.VISIBLE : View.GONE);
		findViewById(R.id.iv_take_verified).setVisibility(o.verified ? View.VISIBLE : View.GONE);
		StringBuilder st = new StringBuilder(o.completedTrades + (o.completedTrades == 1 ? " trade" : " trades"));
		if (o.completionPercent >= 0) st.append(" · ").append(String.format(Locale.US, "%.1f%%", o.completionPercent));
		if (o.trustScore >= 0) st.append(" · Trust ").append(o.trustLabel);
		if (o.premium) st.append(" · Premium"); else if (o.insured) st.append(" · Insured");
		((TextView) findViewById(R.id.tv_take_stats)).setText(st);
		((TextView) findViewById(R.id.tv_take_window)).setText(o.paymentWindowMinutes > 0 ? o.paymentWindowMinutes + " min" : "—");

		((TextView) findViewById(R.id.tv_take_price)).setText(o.unitPrice > 0 ? P2pMoney.fiat(o.unitPrice, o.currency) : "—");
		((TextView) findViewById(R.id.tv_take_price_unit)).setText("/ " + tk);
		((TextView) findViewById(R.id.tv_take_fiat_ccy)).setText(o.currency);
		offerRows.removeAllViews();
		P2pKv.add(offerRows, "Available", BalanceFormatter.smartDecimal(o.remaining) + " " + tk);
		P2pKv.add(offerRows, "Limit", BalanceFormatter.smartDecimal(o.minOrder) + " – " + BalanceFormatter.smartDecimal(o.remaining) + " " + tk);
		if (o.unitPrice > 0) P2pKv.add(offerRows, "Limit (" + o.currency + ")",
			P2pMoney.fiat(o.minOrder * o.unitPrice, o.currency) + " – " + P2pMoney.fiat(o.remaining * o.unitPrice, o.currency));

		if (!o.terms.isEmpty()) {
			findViewById(R.id.ll_take_terms).setVisibility(View.VISIBLE);
			((TextView) findViewById(R.id.tv_take_terms)).setText(o.terms);
		}
		bindPayment();
	}

	private void bindPayment() {
		paymentHost.removeAllViews();
		if (!selling()) {
			// The poster's methods: you pay through one of these.
			if (offer.paymentLabels.isEmpty()) { P2pKv.add(paymentHost, "Methods", ""); return; }
			for (String label : offer.paymentLabels) {
				View chip = getLayoutInflater().inflate(R.layout.item_payment_method_select, paymentHost, false);
				BankLogo.into((ImageView) chip.findViewById(R.id.pm_logo), label);
				((TextView) chip.findViewById(R.id.pm_title)).setText(label);
				chip.findViewById(R.id.pm_subtitle).setVisibility(View.GONE);
				chip.setForeground(null);
				paymentHost.addView(chip);
			}
			return;
		}
		if (myMethods.isEmpty()) {
			MaterialButton add = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
			add.setText("Add a payment account");
			add.setAllCaps(false);
			add.setOnClickListener(v -> startActivity(new Intent(this, PaymentMethodsActivity.class)));
			paymentHost.addView(add);
			return;
		}
		View row = PaymentMethodRows.row(this, paymentHost, receiveInto, true);
		row.setOnClickListener(v -> PaymentMethodRows.pickSheet(this, "Receive payment into", myMethods, receiveInto, pm -> {
			receiveInto = pm;
			bindPayment();
			validate();
		}));
		paymentHost.addView(row);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (selling() && offer != null && myMethods.isEmpty()) {
			myMethods = PaymentMethodManager.loadAll(this);
			if (!myMethods.isEmpty()) { receiveInto = myMethods.get(0); bindPayment(); validate(); }
		}
	}

	// ── Amount ───────────────────────────────────────────────────────────

	private double maxCoin() {
		if (offer == null) return 0;
		return selling() && balance >= 0 ? Math.min(offer.remaining, balance) : offer.remaining;
	}

	private void bindPresets() {
		presets.removeAllViews();
		double max = maxCoin();
		if (max <= 0) return;
		int[] pct = {25, 50, 75, 100};
		for (int p : pct) {
			TextView t = (TextView) getLayoutInflater().inflate(R.layout.item_p2p_preset, presets, false);
			t.setText(p == 100 ? "100%" : p + "%");
			double v = max * p / 100.0;
			t.setOnClickListener(x -> setCoin(v));
			presets.addView(t);
		}
	}

	private void setCoin(double coins) {
		etCoin.setText(coins > 0 ? BalanceFormatter.smartDecimal(coins) : "");
		etCoin.setSelection(etCoin.getText().length());
	}

	private TextWatcher watcher(boolean coinBox) {
		return new TextWatcher() {
			@Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
			@Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
			@Override public void afterTextChanged(Editable s) {
				if (syncing || offer == null) return;
				syncing = true;
				double v = BalanceFormatter.parseUserInput(s.toString());
				if (offer.unitPrice > 0) {
					if (coinBox) etFiat.setText(v > 0 ? String.format(Locale.US, "%.2f", v * offer.unitPrice) : "");
					else etCoin.setText(v > 0 ? BalanceFormatter.smartDecimal(v / offer.unitPrice) : "");
				}
				syncing = false;
				validate();
			}
		};
	}

	private double coinAmount() { return BalanceFormatter.parseUserInput(etCoin.getText().toString()); }

	private void validate() {
		if (offer == null) return;
		if (pendingPreview != null) handler.removeCallbacks(pendingPreview);
		previewedRaw = -1;
		confirm.setEnabled(false);
		summaryCard.setVisibility(View.GONE);
		double amount = coinAmount();
		if (amount <= 0) { error(null); return; }
		if (amount < offer.minOrder) { error("Minimum " + BalanceFormatter.smartDecimal(offer.minOrder) + " " + tk); return; }
		if (amount > offer.remaining) { error("Maximum " + BalanceFormatter.smartDecimal(offer.remaining) + " " + tk); return; }
		if (selling() && balance >= 0 && amount > balance) { error("Balance " + BalanceFormatter.smartDecimal(balance) + " " + tk); return; }
		if (myAddress.isEmpty()) { error("No wallet on this device"); return; }
		if (selling() && receiveInto == null) { error("Add a payment account to receive into"); return; }
		error(null);
		long raw = P2pAsset.toRaw(asset, amount);
		pendingPreview = () -> preview(raw);
		handler.postDelayed(pendingPreview, 350);
	}

	private void error(String text) {
		tvError.setText(text == null ? "" : text);
		tvError.setVisibility(text == null ? View.GONE : View.VISIBLE);
	}

	private void preview(long raw) {
		HarvanaNodeClient.previewP2PClaim(listingId, myAddress, raw, new HarvanaNodeClient.JsonCallback() {
			@Override public void onSuccess(JSONObject r) {
				runOnUiThread(() -> {
					if (P2pAsset.toRaw(asset, coinAmount()) != raw) return;   // the amount changed meanwhile
					previewedRaw = raw;
					feeRaw = r.optLong("platformFeeRaw", 0);
					gasRaw = r.optLong("releaseGasRaw", 0);
					netRaw = r.optLong("netAmountToBuyer", 0);
					showSummary(raw);
					confirm.setEnabled(true);
				});
			}
			@Override public void onError(String e) { runOnUiThread(() -> error(com.harvana.app.utils.ErrorText.plain(e))); }
		});
	}

	private void showSummary(long raw) {
		summary.removeAllViews();
		double coins = P2pAsset.fromRaw(asset, raw);
		P2pKv.add(summary, selling() ? "You sell" : "Order amount", BalanceFormatter.smartDecimal(coins) + " " + tk);
		if (offer.unitPrice > 0) {
			P2pKv.add(summary, "Price", P2pMoney.perUnit(offer.unitPrice, offer.currency, asset));
			P2pKv.add(summary, selling() ? "You receive" : "You pay", P2pMoney.fiat(coins * offer.unitPrice, offer.currency));
		}
		P2pKv.add(summary, "Maker fee", BalanceFormatter.smartDecimal(P2pAsset.fromRaw(asset, feeRaw)) + " " + tk);
		P2pKv.add(summary, "tFare", BalanceFormatter.smartDecimal(P2pAsset.fromRaw(asset, gasRaw)) + " " + tk);
		TextView net = P2pKv.add(summary, selling() ? "Buyer receives" : "You receive",
			BalanceFormatter.smartDecimal(P2pAsset.fromRaw(asset, netRaw)) + " " + tk);
		net.setTypeface(null, android.graphics.Typeface.BOLD);
		summaryCard.setVisibility(View.VISIBLE);
	}

	// ── Confirm and open the order ───────────────────────────────────────

	private void confirm() {
		long raw = P2pAsset.toRaw(asset, coinAmount());
		if (raw != previewedRaw) { validate(); return; }
		double coins = P2pAsset.fromRaw(asset, raw);
		ArrayList<String[]> rows = new ArrayList<>();
		rows.add(new String[]{selling() ? "You sell" : "You buy", BalanceFormatter.smartDecimal(coins) + " " + tk});
		if (offer.unitPrice > 0) rows.add(new String[]{selling() ? "You receive" : "You pay", P2pMoney.fiat(coins * offer.unitPrice, offer.currency)});
		rows.add(new String[]{"Price", P2pMoney.perUnit(offer.unitPrice, offer.currency, asset)});
		rows.add(new String[]{"Maker fee", BalanceFormatter.smartDecimal(P2pAsset.fromRaw(asset, feeRaw)) + " " + tk});
		rows.add(new String[]{"tFare", BalanceFormatter.smartDecimal(P2pAsset.fromRaw(asset, gasRaw)) + " " + tk});
		rows.add(new String[]{selling() ? "Buyer receives" : "You receive", BalanceFormatter.smartDecimal(P2pAsset.fromRaw(asset, netRaw)) + " " + tk});
		if (selling()) rows.add(new String[]{"Paid into", receiveInto.primaryField + " · " + receiveInto.accountIdentifier});
		rows.add(new String[]{"Trader", offer.displayName});
		com.harvana.app.group.PaymentConfirmSheet.reviewThenAuthorize(this,
			(selling() ? "Sell " : "Buy ") + tk, BalanceFormatter.smartDecimal(coins) + " " + tk, rows, () -> open(raw, coins));
	}

	private void open(long raw, double coins) {
		confirm.setEnabled(false);
		confirm.setText("Opening order…");
		P2PTradeRouter.claimPayment(getApplicationContext(), listingId, myAddress, raw, asset, new P2PTradeRouter.Callback() {
			@Override public void onSuccess(JSONObject result) {
				String orderId = P2pIds.orderId(result);
				String myUid = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
				P2pChatStore.notice(orderId, offer.posterUid, (selling() ? "New order: I am selling you " : "New order: I am buying ")
					+ BalanceFormatter.smartDecimal(coins) + " " + tk + ".");
				if (orderId.isEmpty()) {
					runOnUiThread(() -> { paint(); error("Order opened. See My orders."); });
					return;
				}
				Runnable go = () -> runOnUiThread(() -> {
					Intent i = new Intent(P2pBuyOrderActivity.this, selling() ? P2pSellTradeActivity.class : P2pBuyTradeActivity.class);
					i.putExtra("orderId", orderId);
					startActivity(i);
					finish();
				});
				if (myUid == null) { go.run(); return; }
				P2PPaymentDetailsManager m = P2PPaymentDetailsManager.getInstance(getApplicationContext());
				P2PPaymentDetailsManager.SimpleCallback after = new P2PPaymentDetailsManager.SimpleCallback() {
					@Override public void onSuccess() { go.run(); }
					@Override public void onError(String error) {
						runOnUiThread(() -> Toast.makeText(P2pBuyOrderActivity.this, com.harvana.app.utils.ErrorText.plain(error), Toast.LENGTH_LONG).show());
						go.run();
					}
				};
				if (selling()) m.saveOrderPaymentDetails(orderId, receiveInto, myUid, offer.posterUid, after);   // where the buyer pays you
				else m.markBuyerClaimed(listingId, myUid, orderId, after);                                       // so the seller sees who you are
			}
			@Override public void onError(String error) {
				runOnUiThread(() -> {
					paint();
					confirm.setEnabled(true);
					error(com.harvana.app.utils.ErrorText.plain(error));
				});
			}
		});
	}

	private void fatal(String text) {
		LinearLayout body = findViewById(R.id.ll_take_body);
		body.removeAllViews();
		TextView t = new TextView(this);
		t.setText(text);
		t.setTextSize(16);
		t.setTextColor(ContextCompat.getColor(this, R.color.colorTextSecondary));
		t.setGravity(android.view.Gravity.CENTER);
		t.setPadding(0, 160, 0, 0);
		body.addView(t);
		confirm.setEnabled(false);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (pendingPreview != null) handler.removeCallbacks(pendingPreview);
	}
}
