package com.harvana.app;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.harvana.app.p2p.P2pAsset;
import com.harvana.app.p2p.P2pCoinChips;
import com.harvana.app.p2p.P2pCurrencyFilterState;
import com.harvana.app.p2p.P2pKv;
import com.harvana.app.p2p.P2pMoney;
import com.harvana.app.p2p.P2pOffers;
import com.harvana.app.wallet.BalanceFormatter;
import com.harvana.app.wallet.WalletManager;

import java.util.List;
import java.util.Locale;

/**
 * Express: say how much, get the best open offer for it. Buying, you enter what you spend in fiat; selling, how much of
 * the coin you sell. The chosen offer opens in the take-offer screen with the amount filled in.
 */
public class P2pExpressActivity extends AppCompatActivity {

	private boolean sell;
	private String myAddress = "";
	private double balance = -1;
	private List<P2pOffers.Offer> offers;
	private String offersKey = "";
	private long offersAt;
	private P2pOffers.Offer best;
	private double bestCoins;
	private int seq;

	private final Handler handler = new Handler(Looper.getMainLooper());
	private final Runnable search = this::search;
	private final P2pAsset.Listener assetListener = code -> runOnUiThread(() -> { paint(); loadBalance(); schedule(); });

	private EditText etAmount;
	private TextView tvState;
	private MaterialButton go;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.p2p_express);
		com.harvana.app.utils.StatusBarUtil.matchToolbar(this);
		myAddress = WalletManager.getActiveWalletAddress(this);
		if (myAddress == null) myAddress = "";
		etAmount = findViewById(R.id.et_express_amount);
		tvState = findViewById(R.id.tv_express_state);
		go = findViewById(R.id.btn_express_go);
		findViewById(R.id.iv_express_back).setOnClickListener(v -> finish());
		findViewById(R.id.tab_express_buy).setOnClickListener(v -> setSide(false));
		findViewById(R.id.tab_express_sell).setOnClickListener(v -> setSide(true));
		go.setOnClickListener(v -> open());
		etAmount.addTextChangedListener(new TextWatcher() {
			@Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
			@Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
			@Override public void afterTextChanged(Editable s) { schedule(); }
		});
		ChipGroup coins = findViewById(R.id.chips_express_coins);
		P2pCoinChips.build(coins);
		P2pAsset.loadActive(() -> runOnUiThread(() -> P2pCoinChips.build(coins)));
		paint();
		loadBalance();
	}

	@Override protected void onStart() { super.onStart(); P2pAsset.addListener(assetListener); }
	@Override protected void onStop() { super.onStop(); P2pAsset.removeListener(assetListener); }
	@Override protected void onDestroy() { super.onDestroy(); handler.removeCallbacksAndMessages(null); }

	private String tk() { return P2pAsset.ticker(P2pAsset.get()); }
	private String ccy() { return P2pCurrencyFilterState.getCurrency(); }

	private void setSide(boolean s) {
		if (s == sell) return;
		sell = s;
		etAmount.setText("");
		paint();
		schedule();
	}

	private void paint() {
		int buyC = ContextCompat.getColor(this, R.color.p2pBuy), sellC = ContextCompat.getColor(this, R.color.p2pSell);
		int idle = ContextCompat.getColor(this, R.color.colorTextSecondary);
		tab(findViewById(R.id.tab_express_buy), !sell, buyC, ContextCompat.getColor(this, R.color.p2pOnBuy), idle);
		tab(findViewById(R.id.tab_express_sell), sell, sellC, ContextCompat.getColor(this, R.color.p2pOnSell), idle);
		((TextView) findViewById(R.id.tv_express_amount_label)).setText(sell ? "I sell" : "I spend");
		((TextView) findViewById(R.id.tv_express_unit)).setText(sell ? tk() : ccy());
		((TextView) findViewById(R.id.tv_express_balance)).setText(sell && balance >= 0 ? "Balance " + BalanceFormatter.smartDecimal(balance) + " " + tk() : "");
		go.setText((sell ? "Sell " : "Buy ") + tk());
		go.setBackgroundTintList(new ColorStateList(new int[][]{{-android.R.attr.state_enabled}, {}},
			new int[]{ContextCompat.getColor(this, R.color.colorCardStroke), sell ? sellC : buyC}));
		go.setTextColor(ContextCompat.getColor(this, sell ? R.color.p2pOnSell : R.color.p2pOnBuy));
		presets();
	}

	private void tab(TextView t, boolean on, int bg, int fg, int idle) {
		if (on) {
			android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
			d.setColor(bg);
			d.setCornerRadius(11 * getResources().getDisplayMetrics().density);
			t.setBackground(d);
			t.setTextColor(fg);
		} else { t.setBackground(null); t.setTextColor(idle); }
	}

	private void presets() {
		LinearLayout row = findViewById(R.id.ll_express_presets);
		row.removeAllViews();
		double[] values;
		String[] labels;
		if (sell) {
			if (balance <= 0) return;
			values = new double[]{balance * .25, balance * .5, balance * .75, balance};
			labels = new String[]{"25%", "50%", "75%", "Max"};
		} else {
			values = "NGN".equals(ccy()) ? new double[]{5_000, 20_000, 50_000, 100_000} : new double[]{10, 50, 100, 500};
			labels = new String[4];
			for (int i = 0; i < 4; i++) labels[i] = P2pMoney.symbol(ccy()) + String.format(Locale.US, "%,.0f", values[i]);
		}
		for (int i = 0; i < values.length; i++) {
			TextView t = (TextView) getLayoutInflater().inflate(R.layout.item_p2p_preset, row, false);
			t.setText(labels[i]);
			double v = values[i];
			t.setOnClickListener(x -> etAmount.setText(sell ? BalanceFormatter.smartDecimal(v) : String.format(Locale.US, "%.0f", v)));
			row.addView(t);
		}
	}

	private void loadBalance() {
		balance = -1;
		if (myAddress.isEmpty()) return;
		P2pAsset.balance(this, P2pAsset.get(), myAddress, new P2pAsset.BalanceCb() {
			@Override public void ok(double coins) { runOnUiThread(() -> { balance = coins; paint(); }); }
			@Override public void fail(String error) { }
		});
	}

	private void schedule() {
		handler.removeCallbacks(search);
		best = null;
		go.setEnabled(false);
		findViewById(R.id.card_express_match).setVisibility(View.GONE);
		double a = BalanceFormatter.parseUserInput(etAmount.getText().toString());
		tvState.setText(a > 0 ? "Finding the best offer…" : "");
		if (a > 0) handler.postDelayed(search, 450);
	}

	private void search() {
		double amount = BalanceFormatter.parseUserInput(etAmount.getText().toString());
		if (amount <= 0) return;
		if (sell && balance >= 0 && amount > balance) { tvState.setText("Balance " + BalanceFormatter.smartDecimal(balance) + " " + tk()); return; }
		String key = (sell ? "BUY" : "SELL") + ":" + P2pAsset.get();
		if (offers != null && key.equals(offersKey) && System.currentTimeMillis() - offersAt < 30_000) { pick(amount); return; }
		final int mine = ++seq;
		P2pOffers.load(this, sell ? "BUY" : "SELL", P2pAsset.get(), new P2pOffers.Callback() {
			@Override public void loaded(List<P2pOffers.Offer> list) {
				if (mine != seq || isFinishing()) return;
				offers = list;
				offersKey = key;
				offersAt = System.currentTimeMillis();
				pick(BalanceFormatter.parseUserInput(etAmount.getText().toString()));
			}
			@Override public void failed(String error) {
				if (mine == seq) tvState.setText(com.harvana.app.utils.ErrorText.plain(error));
			}
		});
	}

	/** The cheapest seller (buying) or the highest-paying buyer (selling) that takes this amount; ties go to more completed trades. */
	private void pick(double amount) {
		best = null;
		for (P2pOffers.Offer o : offers) {
			if (o.unitPrice <= 0 || !o.currency.equalsIgnoreCase(ccy())) continue;
			if (!myAddress.isEmpty() && myAddress.equals(o.posterAddress)) continue;
			double coins = sell ? amount : amount / o.unitPrice;
			if (coins < o.minOrder || coins > o.remaining) continue;
			if (best == null) { best = o; bestCoins = coins; continue; }
			boolean better = sell ? o.unitPrice > best.unitPrice : o.unitPrice < best.unitPrice;
			if (o.unitPrice == best.unitPrice) better = o.completedTrades > best.completedTrades;
			if (better) { best = o; bestCoins = coins; }
		}
		View card = findViewById(R.id.card_express_match);
		if (best == null) {
			tvState.setText("No offer takes this amount");
			card.setVisibility(View.GONE);
			go.setEnabled(false);
			return;
		}
		tvState.setText("");
		card.setVisibility(View.VISIBLE);
		((TextView) findViewById(R.id.tv_express_receive)).setText(sell
			? P2pMoney.fiat(bestCoins * best.unitPrice, best.currency)
			: BalanceFormatter.smartDecimal(bestCoins) + " " + tk());
		LinearLayout rows = findViewById(R.id.ll_express_match_rows);
		rows.removeAllViews();
		P2pKv.add(rows, sell ? "You sell" : "You pay", sell ? BalanceFormatter.smartDecimal(bestCoins) + " " + tk() : P2pMoney.fiat(amount, best.currency));
		P2pKv.add(rows, "Price", P2pMoney.perUnit(best.unitPrice, best.currency, best.asset));
		P2pKv.add(rows, "Trader", best.displayName + (best.verified ? " ✓" : "") + " · " + best.completedTrades + " trades");
		if (best.paymentWindowMinutes > 0) P2pKv.add(rows, "Payment window", best.paymentWindowMinutes + " min");
		if (!best.paymentLabels.isEmpty()) P2pKv.add(rows, "Payment", String.join(", ", best.paymentLabels));
		go.setEnabled(true);
	}

	private void open() {
		if (best == null) return;
		Intent i = new Intent(this, sell ? P2pSellOrderActivity.class : P2pBuyOrderActivity.class);
		i.putExtra("listingId", best.listingId);
		i.putExtra("coinAmount", bestCoins);
		startActivity(i);
	}
}
