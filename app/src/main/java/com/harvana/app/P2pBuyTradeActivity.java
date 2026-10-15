package com.harvana.app;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.harvana.app.banks.BankLogo;
import com.harvana.app.firestore.P2PPaymentDetailsManager;
import com.harvana.app.firestore.P2PTradeStatsManager;
import com.harvana.app.firestore.UserProfileManager;
import com.harvana.app.p2p.P2pAsset;
import com.harvana.app.p2p.P2pChatStore;
import com.harvana.app.p2p.P2pKv;
import com.harvana.app.p2p.P2pMoney;
import com.harvana.app.p2p.P2pOffers;
import com.harvana.app.p2p.P2pOrderViews;
import com.harvana.app.p2p.P2pStatus;
import com.harvana.app.sync.HarvanaNodeClient;
import com.harvana.app.tx.P2PTradeRouter;
import com.harvana.app.wallet.BalanceFormatter;
import com.harvana.app.wallet.PinPromptHelper;
import com.harvana.app.wallet.WalletManager;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * One P2P order, from either side (res/layout/p2p_trade.xml). The role comes from the order: this wallet is its buyer or
 * its seller. {@link P2pSellTradeActivity} is the same screen.
 *
 * Buyer: pay the seller's account (copy each detail), add a proof picture, tap "I've paid"; cancel while unpaid; remind
 * or appeal once paid. Seller: see the buyer's proof, check your own account, tick and release; appeal once marked paid.
 * The order reloads every 15 s while it is open.
 */
public class P2pBuyTradeActivity extends AppCompatActivity {

	private static final long RELEASE_WINDOW_MIN = 45;     // node P2P_RELEASE_WINDOW_MINUTES
	private static final long REFRESH_MS = 15_000;
	/** Picture picker. Must stay inside 16 bits: this app's fragment library rejects anything higher, and the
	 *  Activity Result launcher draws codes above that and crashes with "Can only use lower 16 bits for requestCode". */
	private static final int RC_PROOF = 4107;

	private String orderId = "", myAddress = "";
	private JSONObject order;
	private boolean buyer;
	private String asset = P2pAsset.HARV, tk = "HARV", status = "", currency = null;
	private double unitPrice = -1, coins;
	private String otherUid = "", otherName = "Trader";
	/** Where the buyer pays: bank / number / holder. */
	private String payBank = "", payNumber = "", payName = "";
	private String proofUrl = "";
	private long lastRemind;

	private final Handler handler = new Handler(Looper.getMainLooper());
	private final Runnable tick = this::tickTimer;
	private final Runnable autoRefresh = this::load;

	private SwipeRefreshLayout refresh;
	private TextView tvTitle, tvTimerLabel, tvTimer, tvBanner;
	private LinearLayout steps, orderRows, escrowRows;
	private MaterialButton primary, secondary;
	private View actionBar;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.p2p_trade);
		com.harvana.app.utils.StatusBarUtil.matchToolbar(this);
		orderId = getIntent().getStringExtra("orderId");
		myAddress = WalletManager.getActiveWalletAddress(this);
		if (myAddress == null) myAddress = "";
		refresh = findViewById(R.id.refresh_trade);
		tvTitle = findViewById(R.id.tv_trade_title);
		tvTimerLabel = findViewById(R.id.tv_trade_timer_label);
		tvTimer = findViewById(R.id.tv_trade_timer);
		tvBanner = findViewById(R.id.tv_trade_banner);
		steps = findViewById(R.id.ll_trade_steps);
		orderRows = findViewById(R.id.ll_trade_order_rows);
		escrowRows = findViewById(R.id.ll_trade_escrow_rows);
		primary = findViewById(R.id.btn_trade_primary);
		secondary = findViewById(R.id.btn_trade_secondary);
		actionBar = findViewById(R.id.bar_trade_actions);
		refresh.setColorSchemeColors(ContextCompat.getColor(this, R.color.colorAccent));
		refresh.setOnRefreshListener(this::load);
		findViewById(R.id.iv_trade_back).setOnClickListener(v -> finish());
		findViewById(R.id.iv_trade_refresh).setOnClickListener(v -> load());
		findViewById(R.id.tv_trade_cancel).setOnClickListener(v -> cancelOrder());
		findViewById(R.id.btn_trade_chat).setOnClickListener(v -> openChat());
		if (orderId == null || orderId.isEmpty()) { fatal("No order"); return; }
		tvTitle.setText(orderId);
		load();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (order != null) load();
	}

	@Override
	protected void onPause() {
		super.onPause();
		handler.removeCallbacks(autoRefresh);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		handler.removeCallbacksAndMessages(null);
	}

	// ── Load ─────────────────────────────────────────────────────────────

	private void load() {
		handler.removeCallbacks(autoRefresh);
		refresh.setRefreshing(true);
		HarvanaNodeClient.getP2POrder(orderId, new HarvanaNodeClient.JsonCallback() {
			@Override public void onSuccess(JSONObject o) {
				runOnUiThread(() -> {
					refresh.setRefreshing(false);
					String b = o.optString("buyerAddress", ""), s = o.optString("sellerAddress", "");
					if (!myAddress.equals(b) && !myAddress.equals(s)) { fatal("This order belongs to another wallet"); return; }
					boolean first = order == null;
					order = o;
					buyer = myAddress.equals(b);
					asset = P2pAsset.of(o);
					tk = P2pAsset.ticker(asset);
					status = o.optString("status", "");
					coins = P2pAsset.fromRaw(asset, o.optLong("amount", o.optLong("buyerEntitlementRaw", 0)));
					long kobo = o.optLong("unitPricePerHarvRaw", 0);
					if (unitPrice <= 0 && kobo > 0) unitPrice = kobo / 100.0;
					render();
					if (first) { loadOfferAndPayment(); loadCounterparty(); }
					if (P2pStatus.active(status)) handler.postDelayed(autoRefresh, REFRESH_MS);
				});
			}
			@Override public void onError(String error) {
				runOnUiThread(() -> {
					refresh.setRefreshing(false);
					if (order == null) fatal(com.harvana.app.utils.ErrorText.plain(error));
					else Toast.makeText(P2pBuyTradeActivity.this, com.harvana.app.utils.ErrorText.plain(error), Toast.LENGTH_SHORT).show();
				});
			}
		});
	}

	/** Price and currency from the offer card; where the buyer pays (the order's own copy, else the offer's first method). */
	private void loadOfferAndPayment() {
		P2PPaymentDetailsManager m = P2PPaymentDetailsManager.getInstance(getApplicationContext());
		String listingId = order.optString("listingId", "");
		if (!listingId.isEmpty()) m.get(listingId, new P2PPaymentDetailsManager.DocCallback() {
			@Override public void onSuccess(Map<String, Object> d) {
				Object p = d.get("unitPrice"), c = d.get("currency");
				runOnUiThread(() -> {
					if (p instanceof Number && ((Number) p).doubleValue() > 0) unitPrice = ((Number) p).doubleValue();
					if (c != null && !String.valueOf(c).trim().isEmpty()) currency = String.valueOf(c).trim();
					if (payBank.isEmpty()) setPayment(d);
					render();
				});
			}
			@Override public void onError(String error) { }
		});
		m.getOrderPaymentDetails(orderId, new P2PPaymentDetailsManager.DocCallback() {
			@Override public void onSuccess(Map<String, Object> d) { runOnUiThread(() -> { setPayment(d); render(); }); }
			@Override public void onError(String error) { }
		});
	}

	private void setPayment(Map<String, Object> d) {
		Map<?, ?> src = d;
		Object methods = d.get("paymentMethods");
		if (methods instanceof List && !((List<?>) methods).isEmpty() && ((List<?>) methods).get(0) instanceof Map) {
			src = (Map<?, ?>) ((List<?>) methods).get(0);
		}
		if (src.get("primaryField") != null) payBank = String.valueOf(src.get("primaryField"));
		if (src.get("accountIdentifier") != null) payNumber = String.valueOf(src.get("accountIdentifier"));
		if (src.get("accountName") != null) payName = String.valueOf(src.get("accountName"));
	}

	private void loadCounterparty() {
		P2PPaymentDetailsManager m = P2PPaymentDetailsManager.getInstance(getApplicationContext());
		String listingId = order.optString("listingId", "");
		if (buyer) {
			// The seller: on the order's payment copy, else the offer's poster.
			m.getOrderPaymentDetails(orderId, new P2PPaymentDetailsManager.DocCallback() {
				@Override public void onSuccess(Map<String, Object> d) {
					Object uid = d.get("sellerUid");
					if (uid != null && !String.valueOf(uid).isEmpty()) profile(String.valueOf(uid)); else fromListing();
				}
				@Override public void onError(String error) { fromListing(); }
				private void fromListing() {
					if (listingId.isEmpty()) return;
					m.get(listingId, new P2PPaymentDetailsManager.DocCallback() {
						@Override public void onSuccess(Map<String, Object> d) {
							Object uid = d.get("sellerUid");
							if (uid != null) profile(String.valueOf(uid));
						}
						@Override public void onError(String error) { }
					});
				}
			});
		} else {
			m.getBuyerUidForOrder(listingId, orderId, new P2PPaymentDetailsManager.UidCallback() {
				@Override public void onFound(String uid) { profile(uid); }
				@Override public void onNotFound() { }
				@Override public void onError(String error) { }
			});
		}
	}

	private void profile(String uid) {
		otherUid = uid;
		loadProof();
		UserProfileManager.getInstance(getApplicationContext()).getProfile(uid, new UserProfileManager.DocCallback() {
			@Override public void onSuccess(Map<String, Object> p) {
				String name = P2pOffers.displayName(p.get("first_name"), p.get("last_name"));
				Object icon = p.get("icon");
				runOnUiThread(() -> {
					otherName = name;
					((TextView) findViewById(R.id.tv_trade_name)).setText(name);
					if (icon != null && String.valueOf(icon).startsWith("http")) {
						ImageView av = findViewById(R.id.iv_trade_avatar);
						av.setVisibility(View.VISIBLE);
						findViewById(R.id.tv_trade_initial).setVisibility(View.INVISIBLE);
						Glide.with(P2pBuyTradeActivity.this).load(Uri.parse(String.valueOf(icon))).circleCrop().into(av);
					} else {
						((TextView) findViewById(R.id.tv_trade_initial)).setText(name.substring(0, 1).toUpperCase(Locale.US));
					}
					render();
				});
			}
			@Override public void onError(String error) { }
		});
		TrustScoreEngine.compute(getApplicationContext(), uid, new TrustScoreEngine.Callback() {
			@Override public void onComplete(TrustScoreEngine.Result r) {
				runOnUiThread(() -> findViewById(R.id.iv_trade_verified).setVisibility(r.verified ? View.VISIBLE : View.GONE));
			}
			@Override public void onError(String error) { }
		});
		P2PTradeStatsManager.getInstance(getApplicationContext()).getStats(uid, new P2PTradeStatsManager.StatsCallback() {
			@Override public void onSuccess(int total, int ok, double pct) {
				runOnUiThread(() -> ((TextView) findViewById(R.id.tv_trade_role)).setText((buyer ? "Seller" : "Buyer")
					+ " · " + total + (total == 1 ? " trade" : " trades") + (total > 0 ? String.format(Locale.US, " · %.1f%%", pct) : "")));
			}
			@Override public void onError(String error) { }
		});
	}

	private void loadProof() {
		if (otherUid.isEmpty()) return;
		// The buyer's proof lives in the chat; the seller reads the buyer's, the buyer sees their own.
		P2pChatStore.latestProof(orderId, otherUid, (url, at) -> runOnUiThread(() -> {
			if (url != null && !url.isEmpty()) { proofUrl = url; render(); }
		}));
	}

	// ── Render ───────────────────────────────────────────────────────────

	private String fiat(double v) { return P2pMoney.fiat(v, currency); }

	private void render() {
		if (order == null || isFinishing()) return;
		int sideColor = ContextCompat.getColor(this, buyer ? R.color.p2pBuy : R.color.p2pSell);
		boolean escrowOk = order.optBoolean("escrowConfirmed", true);
		double total = unitPrice > 0 ? coins * unitPrice : -1;

		// Order card
		P2pOffers.coinIcon(findViewById(R.id.iv_trade_coin), asset);
		TextView side = findViewById(R.id.tv_trade_side);
		side.setText((buyer ? "Buy " : "Sell ") + tk);
		side.setTextColor(sideColor);
		TextView st = findViewById(R.id.tv_trade_status);
		st.setText(P2pStatus.label(status));
		st.setTextColor(ContextCompat.getColor(this, P2pStatus.color(status)));
		((TextView) findViewById(R.id.tv_trade_fiat)).setText(total > 0 ? fiat(total) : BalanceFormatter.smartDecimal(coins) + " " + tk);
		if (((TextView) findViewById(R.id.tv_trade_role)).getText().length() == 0) {
			((TextView) findViewById(R.id.tv_trade_role)).setText(buyer ? "Seller" : "Buyer");
		}
		orderRows.removeAllViews();
		P2pKv.add(orderRows, "Quantity", BalanceFormatter.smartDecimal(coins) + " " + tk);
		if (unitPrice > 0) P2pKv.add(orderRows, "Price", P2pMoney.perUnit(unitPrice, currency, asset));
		long fee = order.optLong("platformFeeRaw", 0), gas = order.optLong("releaseGasRaw", 0);
		if (fee > 0) P2pKv.add(orderRows, "Maker fee", BalanceFormatter.smartDecimal(P2pAsset.fromRaw(asset, fee)) + " " + tk);
		if (gas > 0) P2pKv.add(orderRows, "tFare", BalanceFormatter.smartDecimal(P2pAsset.fromRaw(asset, gas)) + " " + tk);
		long net = order.optLong("netAmountToBuyer", order.optLong("buyerEntitlementRaw", 0));
		if (net > 0) P2pKv.add(orderRows, buyer ? "You receive" : "Buyer receives", BalanceFormatter.smartDecimal(P2pAsset.fromRaw(asset, net)) + " " + tk);
		P2pKv.add(orderRows, "Order No.", order.optString("orderId", orderId), true);
		P2pKv.add(orderRows, "Created", time(order.optLong("paymentClaimedTimestamp", order.optLong("createdTimestamp", 0))));

		// Escrow card
		TextView es = findViewById(R.id.tv_trade_escrow_state);
		boolean released = !order.optString("releaseTxHash", "").isEmpty();
		es.setText(released ? "Released" : "CANCELLED".equals(status) ? "Returned" : escrowOk ? "Locked" : "Locking");
		es.setTextColor(ContextCompat.getColor(this, escrowOk || released ? R.color.colorSuccess : R.color.colorAmber));
		escrowRows.removeAllViews();
		P2pKv.add(escrowRows, "Lock transaction", order.optString("escrowTxHash", ""), !order.optString("escrowTxHash", "").isEmpty());
		if (released) P2pKv.add(escrowRows, "Release transaction", order.optString("releaseTxHash"), true);
		if (!order.optString("refundTxHash", "").isEmpty()) P2pKv.add(escrowRows, "Refund transaction", order.optString("refundTxHash"), true);

		P2pOrderViews.events(this, findViewById(R.id.order_events), order.optJSONArray("events"),
			order.optString("buyerAddress"), order.optString("sellerAddress"));

		// State: title, timer, banner, steps, actions
		steps.removeAllViews();
		tvBanner.setVisibility(View.GONE);
		findViewById(R.id.tv_trade_cancel).setVisibility(View.GONE);
		hideActions();
		handler.removeCallbacks(tick);
		timerDeadline = 0;

		switch (status) {
			case "AWAITING_PAYMENT":
				timerDeadline = order.optLong("paymentClaimedTimestamp", 0) + order.optLong("paymentWindowMinutes", 15) * 60_000L;
				if (buyer) {
					tvTitle.setText("Pay the seller");
					tvTimerLabel.setText("Pay within");
					if (!escrowOk) banner("Escrow lock pending · do not pay yet", R.drawable.bg_chip_pending);
					stepPayTo(1, total);
					stepProof(2, true);
					step(3, "Notify the seller", null, true);
					findViewById(R.id.tv_trade_cancel).setVisibility(View.VISIBLE);
					action(primary, escrowOk ? "I've paid, notify seller" : "Locking escrow…", sideColor, escrowOk, v -> markPaid(total));
					loadUnpaidLimit();
				} else {
					tvTitle.setText("Waiting for payment");
					tvTimerLabel.setText("Buyer pays within");
					stepReceiveInto(1, total, "Buyer pays into");
				}
				break;
			case "PAYMENT_SUBMITTED":
				timerDeadline = order.optLong("markedPaidTimestamp", 0) + RELEASE_WINDOW_MIN * 60_000L;
				if (buyer) {
					tvTitle.setText("Pending release");
					tvTimerLabel.setText("Seller releases within");
					stepPayTo(1, total);
					stepProof(2, true);
					action(secondary, "Appeal", 0, true, v -> appeal());
					action(primary, "Remind seller", ContextCompat.getColor(this, R.color.colorAccent), true, v -> remind());
				} else {
					tvTitle.setText("Verify payment");
					tvTimerLabel.setText("Release within");
					stepReceiveInto(1, total, "Check this account");
					stepProof(2, false);
					CheckBox box = stepConfirm(3, total);
					action(secondary, "Appeal", 0, true, v -> appeal());
					action(primary, "Release " + tk, sideColor, false, v -> release(total));
					box.setOnCheckedChangeListener((b, on) -> primary.setEnabled(on));
				}
				break;
			case "RELEASE_PROCESSING":
				tvTitle.setText("Releasing");
				tvTimerLabel.setText("");
				break;
			case "DISPUTED": {
				tvTitle.setText("Appeal open");
				tvTimerLabel.setText(time(order.optLong("disputeRaisedAtMs", 0)));
				String by = order.optString("disputeRaisedBy", "");
				banner("Appeal by " + (by.equals(myAddress) ? "you" : by.isEmpty() ? "the network" : buyer ? "the seller" : "the buyer")
					+ " · escrow held for guardians", R.drawable.bg_chip_overdue);
				stepProof(1, buyer);
				break;
			}
			case "COMPLETED":
				tvTitle.setText(buyer ? "Received " + BalanceFormatter.smartDecimal(P2pAsset.fromRaw(asset, net > 0 ? net : 0)) + " " + tk : "Completed");
				tvTimerLabel.setText(time(order.optLong("completedTimestamp", 0)));
				banner("Completed", R.drawable.bg_chip_paid);
				action(primary, "Trade again", sideColor, true, v -> {
					startActivity(new Intent(this, P2pMarketplaceActivity.class).putExtra("side", buyer ? "BUY" : "SELL"));
					finish();
				});
				break;
			case "DISPUTE_RESOLVED_BUYER":
			case "DISPUTE_RESOLVED_SELLER":
				tvTitle.setText(P2pStatus.label(status));
				tvTimerLabel.setText("");
				break;
			case "CANCELLED":
				tvTitle.setText("Cancelled");
				tvTimerLabel.setText(time(order.optLong("cancelledTimestamp", 0)));
				String reason = order.optString("cancelReason", "");
				if (!reason.isEmpty()) banner(reason.startsWith("TIMEOUT") ? "Payment window ended" : "BUYER_CANCELLED".equals(reason) ? "Cancelled by buyer" : reason, R.drawable.bg_chip_muted);
				break;
			default:
				tvTitle.setText(P2pStatus.label(status));
				tvTimerLabel.setText("");
		}
		if (timerDeadline > 0) tickTimer(); else tvTimer.setText("");
	}

	private long timerDeadline;

	private void tickTimer() {
		if (timerDeadline <= 0) return;
		long left = timerDeadline - System.currentTimeMillis();
		if (left <= 0) { tvTimer.setText("00:00"); tvTimer.setTextColor(ContextCompat.getColor(this, R.color.colorError)); return; }
		tvTimer.setText(String.format(Locale.US, "%02d:%02d", left / 60_000, (left / 1000) % 60));
		tvTimer.setTextColor(ContextCompat.getColor(this, left < 5 * 60_000 ? R.color.colorError : R.color.colorAmber));
		handler.postDelayed(tick, 1000);
	}

	private void banner(String text, int bg) {
		tvBanner.setText(text);
		tvBanner.setBackgroundResource(bg);
		tvBanner.setVisibility(View.VISIBLE);
	}

	private LinearLayout step(int n, String title, View content, boolean last) {
		View v = getLayoutInflater().inflate(R.layout.item_p2p_step, steps, false);
		((TextView) v.findViewById(R.id.tv_step_number)).setText(String.valueOf(n));
		((TextView) v.findViewById(R.id.tv_step_title)).setText(title);
		v.findViewById(R.id.v_step_line).setVisibility(last ? View.INVISIBLE : View.VISIBLE);
		LinearLayout c = v.findViewById(R.id.ll_step_content);
		if (content != null) c.addView(content);
		else c.setVisibility(View.GONE);
		steps.addView(v);
		return c;
	}

	private LinearLayout card() {
		LinearLayout box = new LinearLayout(this);
		box.setOrientation(LinearLayout.VERTICAL);
		box.setBackgroundResource(R.drawable.bg_p2p_method_chip);
		int p = Math.round(12 * getResources().getDisplayMetrics().density);
		box.setPadding(p, p, p, p);
		return box;
	}

	/** Buyer: the seller's account, every line copyable. */
	private void stepPayTo(int n, double total) {
		LinearLayout box = card();
		LinearLayout head = new LinearLayout(this);
		head.setGravity(android.view.Gravity.CENTER_VERTICAL);
		ImageView logo = new ImageView(this);
		int s = Math.round(28 * getResources().getDisplayMetrics().density);
		head.addView(logo, new LinearLayout.LayoutParams(s, s));
		BankLogo.into(logo, payBank);
		TextView bank = new TextView(this);
		bank.setText(payBank.isEmpty() ? "…" : payBank);
		bank.setTextSize(15);
		bank.setTypeface(null, android.graphics.Typeface.BOLD);
		bank.setTextColor(ContextCompat.getColor(this, R.color.colorTextPrimary));
		bank.setPadding(s / 3, 0, 0, 0);
		head.addView(bank);
		box.addView(head);
		if (total > 0) P2pKv.add(box, "You pay", fiat(total), true).setTypeface(null, android.graphics.Typeface.BOLD);
		P2pKv.add(box, "Account number", payNumber, !payNumber.isEmpty());
		P2pKv.add(box, "Account name", payName, !payName.isEmpty());
		P2pKv.add(box, "Reference", order.optString("orderId", orderId), true);
		step(n, "Transfer to the seller", box, false);
	}

	/** Seller: the account the buyer pays into. */
	private void stepReceiveInto(int n, double total, String title) {
		LinearLayout box = card();
		P2pKv.add(box, "Bank", payBank);
		P2pKv.add(box, "Account number", payNumber);
		P2pKv.add(box, "Account name", payName);
		if (total > 0) P2pKv.add(box, "Amount", fiat(total)).setTypeface(null, android.graphics.Typeface.BOLD);
		P2pKv.add(box, "Reference", order.optString("orderId", orderId), true);
		step(n, title, box, false);
	}

	private void stepProof(int n, boolean mine) {
		View v = getLayoutInflater().inflate(R.layout.item_p2p_proof, steps, false);
		ImageView img = v.findViewById(R.id.iv_proof);
		TextView empty = v.findViewById(R.id.tv_proof_empty);
		if (!proofUrl.isEmpty()) {
			img.setVisibility(View.VISIBLE);
			v.findViewById(R.id.ll_proof_empty).setVisibility(View.GONE);
			Glide.with(this).load(proofUrl).into(img);
			v.setOnClickListener(x -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(proofUrl))));
		} else if (mine && ("AWAITING_PAYMENT".equals(status) || "PAYMENT_SUBMITTED".equals(status) || "DISPUTED".equals(status))) {
			empty.setText("Add payment proof");
			v.setOnClickListener(x -> pickProof());
		} else {
			empty.setText(mine ? "No proof" : "No proof from the buyer yet");
			v.setForeground(null);
		}
		step(n, mine ? "Payment proof" : "Buyer's payment proof", v, false);
	}

	private CheckBox stepConfirm(int n, double total) {
		CheckBox box = new CheckBox(this);
		box.setText("Received " + (total > 0 ? fiat(total) : "the payment") + " from " + otherName);
		box.setTextColor(ContextCompat.getColor(this, R.color.colorTextPrimary));
		androidx.core.widget.CompoundButtonCompat.setButtonTintList(box, ColorStateList.valueOf(ContextCompat.getColor(this, R.color.colorAccent)));
		step(n, "Confirm and release", box, true);
		return box;
	}

	private void hideActions() {
		primary.setVisibility(View.GONE);
		secondary.setVisibility(View.GONE);
		actionBar.setVisibility(View.GONE);
	}

	private void action(MaterialButton b, String text, int color, boolean enabled, View.OnClickListener click) {
		b.setText(text);
		b.setEnabled(enabled);
		b.setOnClickListener(click);
		if (color != 0) {
			b.setBackgroundTintList(new ColorStateList(new int[][]{{-android.R.attr.state_enabled}, {}},
				new int[]{ContextCompat.getColor(this, R.color.colorCardStroke), color}));
			b.setTextColor(ContextCompat.getColor(this, buyer ? R.color.p2pOnBuy : R.color.p2pOnSell));
		}
		b.setVisibility(View.VISIBLE);
		actionBar.setVisibility(View.VISIBLE);
	}

	private void loadUnpaidLimit() {
		HarvanaNodeClient.getP2PUnpaidStatus(myAddress, new HarvanaNodeClient.JsonCallback() {
			@Override public void onSuccess(JSONObject r) {
				runOnUiThread(() -> P2pOrderViews.unpaidLimit(P2pBuyTradeActivity.this, findViewById(R.id.order_limits), r));
			}
			@Override public void onError(String error) { }
		});
	}

	// ── Actions ──────────────────────────────────────────────────────────

	private void markPaid(double total) {
		ArrayList<String[]> rows = new ArrayList<>();
		rows.add(new String[]{"Paid", total > 0 ? fiat(total) : "The agreed amount"});
		rows.add(new String[]{"To", (payBank + " · " + payNumber).trim()});
		rows.add(new String[]{"For", BalanceFormatter.smartDecimal(coins) + " " + tk});
		com.harvana.app.group.PaymentConfirmSheet.reviewThenAuthorize(this, "I've paid", total > 0 ? fiat(total) : "", rows, () -> {
			primary.setEnabled(false);
			primary.setText("Sending…");
			P2PTradeRouter.markOrderPaid(getApplicationContext(), orderId, myAddress, cb("Marked paid", () ->
				P2pChatStore.notice(orderId, otherUid, "I have paid " + (total > 0 ? fiat(total) : "") + ". Please check and release.")));
		});
	}

	private void release(double total) {
		ArrayList<String[]> rows = new ArrayList<>();
		rows.add(new String[]{"Release", BalanceFormatter.smartDecimal(coins) + " " + tk});
		rows.add(new String[]{"To", otherName});
		rows.add(new String[]{"Received", total > 0 ? fiat(total) : "The agreed amount"});
		com.harvana.app.group.PaymentConfirmSheet.reviewThenAuthorize(this, "Release " + tk, BalanceFormatter.smartDecimal(coins) + " " + tk, rows, () -> {
			primary.setEnabled(false);
			primary.setText("Releasing…");
			P2PTradeRouter.confirmReceived(getApplicationContext(), orderId, myAddress, cb("Released", () ->
				P2pChatStore.notice(orderId, otherUid, "Payment received. " + tk + " released.")));
		});
	}

	private void cancelOrder() {
		ArrayList<String[]> rows = new ArrayList<>();
		rows.add(new String[]{"Order", orderId});
		rows.add(new String[]{"Amount", BalanceFormatter.smartDecimal(coins) + " " + tk});
		com.harvana.app.group.PaymentConfirmSheet.reviewThenAuthorize(this, "Cancel order", "Unpaid order", rows, () ->
			P2PTradeRouter.cancelOrder(getApplicationContext(), orderId, myAddress, cb("Cancelled", () ->
				P2pChatStore.notice(orderId, otherUid, "I cancelled this order before paying."))));
	}

	private void remind() {
		long now = System.currentTimeMillis();
		if (now - lastRemind < 5 * 60_000L) { Toast.makeText(this, "Reminded", Toast.LENGTH_SHORT).show(); return; }
		lastRemind = now;
		P2pChatStore.notice(orderId, otherUid, "Reminder: I paid for order " + orderId + ". Please check and release.");
		Toast.makeText(this, "Reminder sent", Toast.LENGTH_SHORT).show();
	}

	private void appeal() {
		P2pOrderViews.appealForm(this, buyer ? "Paid, not released" : "Payment not received", reason ->
			PinPromptHelper.authorize(this, "Appeal this order", () ->
				P2PTradeRouter.raiseDispute(getApplicationContext(), orderId, myAddress, reason, cb("Appeal opened", () ->
					P2pChatStore.notice(orderId, otherUid, "I opened an appeal: " + reason)))));
	}

	private P2PTradeRouter.Callback cb(String done, Runnable after) {
		return new P2PTradeRouter.Callback() {
			@Override public void onSuccess(JSONObject result) {
				if (after != null) after.run();
				runOnUiThread(() -> { Toast.makeText(P2pBuyTradeActivity.this, done, Toast.LENGTH_SHORT).show(); load(); });
			}
			@Override public void onError(String error) {
				runOnUiThread(() -> {
					Toast.makeText(P2pBuyTradeActivity.this, com.harvana.app.utils.ErrorText.plain(error), Toast.LENGTH_LONG).show();
					render();
				});
			}
		};
	}

	private void pickProof() {
		Intent pick = new Intent(Intent.ACTION_GET_CONTENT);
		pick.addCategory(Intent.CATEGORY_OPENABLE);
		pick.setType("image/*");
		try { startActivityForResult(pick, RC_PROOF); }
		catch (android.content.ActivityNotFoundException e) {
			Toast.makeText(this, "No app can pick a picture.", Toast.LENGTH_LONG).show();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == RC_PROOF && resultCode == RESULT_OK && data != null && data.getData() != null) uploadProof(data.getData());
	}

	private void uploadProof(Uri uri) {
		if (otherUid.isEmpty()) { Toast.makeText(this, "Loading the trader", Toast.LENGTH_SHORT).show(); return; }
		Toast.makeText(this, "Uploading…", Toast.LENGTH_SHORT).show();
		com.harvana.app.utils.ImageHost.upload(getApplicationContext(), uri, "p2p_proofs/" + orderId, 1280, new com.harvana.app.utils.ImageHost.Callback() {
			@Override public void onSuccess(String url) {
				P2pChatStore.sendProof(orderId, otherUid, url, (ok, err) -> runOnUiThread(() -> {
					if (ok) { proofUrl = url; render(); }
					Toast.makeText(P2pBuyTradeActivity.this, ok ? "Proof sent" : (err == null ? "Not sent" : err), Toast.LENGTH_SHORT).show();
				}));
			}
			@Override public void onError(String message) {
				runOnUiThread(() -> Toast.makeText(P2pBuyTradeActivity.this, message, Toast.LENGTH_LONG).show());
			}
		});
	}

	private void openChat() {
		if (otherUid.isEmpty()) { Toast.makeText(this, "Loading the trader", Toast.LENGTH_SHORT).show(); return; }
		Intent i = new Intent(this, P2pChatActivity.class);
		i.putExtra(P2pChatActivity.EXTRA_ORDER, orderId);
		i.putExtra(P2pChatActivity.EXTRA_OTHER, otherUid);
		i.putExtra(P2pChatActivity.EXTRA_NAME, otherName);
		startActivity(i);
	}

	private static String time(long ms) {
		return ms <= 0 ? "" : new SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()).format(new Date(ms));
	}

	private void fatal(String text) {
		LinearLayout body = findViewById(R.id.ll_trade_body);
		body.removeAllViews();
		TextView t = new TextView(this);
		t.setText(text);
		t.setTextSize(16);
		t.setTextColor(ContextCompat.getColor(this, R.color.colorTextSecondary));
		t.setGravity(android.view.Gravity.CENTER);
		t.setPadding(0, 160, 0, 0);
		body.addView(t);
		hideActions();
	}
}
