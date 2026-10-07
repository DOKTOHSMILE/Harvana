package com.harvana.app.p2p;

import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.CompoundButtonCompat;

import com.harvana.app.ComfirmSellOfferActivity;
import com.harvana.app.P2pMerchantManagerActivity;
import com.harvana.app.PaymentMethodsActivity;
import com.harvana.app.R;
import com.harvana.app.ReceiveActivity;
import com.harvana.app.wallet.BalanceFormatter;
import com.harvana.app.wallet.FiatValueHelper;
import com.harvana.app.wallet.PaymentMethod;
import com.harvana.app.wallet.PaymentMethodManager;
import com.harvana.app.wallet.WalletManager;
import com.harvana.app.wallet.WalletNodeBridge;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Wires the Sketchware-generated Create Buy/Sell Offer screens.
 * Those activities previously had empty initializeLogic() so every
 * Preview / Publish / Max / duration tap did nothing.
 */
public final class P2pOfferWizard {

	private static final long[] DURATION_HOURS = {6, 12, 24, 72, 168, 336, 720};
	private static final String[] DURATION_LABELS = {
		"6 hours", "12 hours", "24 hours", "3 days", "7 days", "14 days", "30 days"
	};

	private static final Map<AppCompatActivity, Runnable> PAYMENT_REFRESH = new WeakHashMap<>();

	private static final String INSUFFICIENT_BALANCE_ERROR = ("You don't have that much " + com.harvana.app.CoinInfo.ticker() + " available.");

	private P2pOfferWizard() {}

	public static void refreshPayments(AppCompatActivity activity) {
		Runnable r = PAYMENT_REFRESH.get(activity);
		if (r != null) r.run();
	}

	public static void attachSell(AppCompatActivity activity) {
		attach(activity, true);
	}

	public static void attachBuy(AppCompatActivity activity) {
		attach(activity, false);
	}

	private static void attach(AppCompatActivity a, boolean sell) {
		String p = sell ? "sell" : "buy";
		EditText etAmount = a.findViewById(id(a, "et_" + p + "_amount"));
		EditText etPrice = a.findViewById(id(a, "et_" + p + "_price"));
		TextView tvAvailable = sell ? a.findViewById(R.id.tv_sell_available_balance) : null;
		TextView tvYou = a.findViewById(id(a, sell ? "tv_sell_you_receive" : "tv_buy_you_pay"));
		TextView tvLimit = a.findViewById(id(a, "tv_" + p + "_trade_limit_value"));
		TextView tvDuration = a.findViewById(id(a, "tv_" + p + "_offer_duration_value"));
		TextView tvError1 = a.findViewById(id(a, "tv_" + p + "_step1_error"));
		TextView tvError2 = a.findViewById(id(a, "tv_" + p + "_step2_error"));
		LinearLayout paymentList = a.findViewById(id(a, "layout_" + p + "_payment_methods"));
		LinearLayout panel1 = a.findViewById(id(a, "panel_" + p + "_step_1"));
		LinearLayout panel2 = a.findViewById(id(a, "panel_" + p + "_step_2"));
		LinearLayout panel3 = a.findViewById(id(a, "panel_" + p + "_step_3"));
		TextView stepLabel = a.findViewById(id(a, "tv_" + p + "_step_label"));
		MaterialButton btnPreview = a.findViewById(id(a, "btn_preview_" + p + "_offer"));
		MaterialButton btnPublish = a.findViewById(id(a, "btn_publish_" + p + "_offer"));
		MaterialButton btnBack = a.findViewById(id(a, "btn_" + p + "_back_to_step1"));
		View rowLimit = a.findViewById(id(a, "row_" + p + "_trade_limit"));
		View rowDuration = a.findViewById(id(a, "row_" + p + "_offer_duration"));
		View btnEditPay = a.findViewById(id(a, "btn_" + p + "_edit_payment_methods"));
		View btnViewOffers = a.findViewById(id(a, "btn_" + p + "_view_my_offers"));
		View btnAnother = a.findViewById(id(a, "btn_" + p + "_create_another"));
		MaterialButton btnMax = sell ? a.findViewById(R.id.btn_sell_max) : null;
		TextView tvPriceCurrency = a.findViewById(id(a, "tv_" + p + "_price_currency"));
		View rowPaymentWindow = a.findViewById(id(a, "row_" + p + "_payment_window"));
		TextView tvPaymentWindow = a.findViewById(id(a, "tv_" + p + "_payment_window_value"));

		// --- Pricing engine (Fixed vs Floating) ---
		MaterialButtonToggleGroup togglePricingMode = a.findViewById(id(a, "toggle_" + p + "_pricing_mode"));
		View groupPriceFixed = a.findViewById(id(a, "group_" + p + "_price_fixed"));
		View groupPriceFloating = a.findViewById(id(a, "group_" + p + "_price_floating"));
		EditText etMargin = a.findViewById(id(a, "et_" + p + "_margin"));
		TextView tvCalculatedPrice = a.findViewById(id(a, "tv_" + p + "_calculated_price"));
		TextView tvPriceNote = a.findViewById(id(a, "tv_" + p + "_price_note"));
		EditText etTerms = a.findViewById(id(a, "et_" + p + "_terms"));
		TextView tvTermsCounter = a.findViewById(id(a, "tv_" + p + "_terms_counter"));
		View btnDeposit1 = a.findViewById(id(a, "btn_" + p + "_deposit_harv_1"));
		View btnDeposit2 = a.findViewById(id(a, "btn_" + p + "_deposit_harv_2"));
		View rowMinAccountAge = a.findViewById(id(a, "row_" + p + "_min_account_age"));
		TextView tvMinAccountAge = a.findViewById(id(a, "tv_" + p + "_min_account_age_value"));
		final int[] minAccountAgeDays = {0};
		String priceNoteDefault = tvPriceNote != null ? tvPriceNote.getText().toString() : "";
		final boolean[] floatingMode = {false};
		final double[] indexPrice = {-1};
		// Node-side, the settlement engine currently hardcodes a uniform
		// 15-minute AWAITING_PAYMENT window for every trade regardless of
		// listing (see the master P2P spec) — so this default matches
		// that, but a NON-default choice here is saved to Firestore only;
		// it won't actually change trade behavior until the node itself
		// is updated to read a per-listing window instead of its hardcoded
		// constant. Said plainly rather than implying this already works
		// end-to-end.
		final int[] paymentWindowMinutes = {15};

		final double[] available = {-1};
		final double[] minOrder = {1};
		final double[] maxOrder = {-1};
		final long[] durationHours = {168};
		final Set<String> selectedIds = new HashSet<>();
		final String currency = P2pCurrencyFilterState.getCurrency();

		if (tvPriceCurrency != null) tvPriceCurrency.setText(currency);
		if (tvDuration != null) tvDuration.setText("7 days");
		if (tvLimit != null) tvLimit.setText(("Min 1 · Max — " + com.harvana.app.CoinInfo.ticker()));
		if (tvError1 != null) tvError1.setVisibility(View.GONE);
		if (tvError2 != null) tvError2.setVisibility(View.GONE);

		// The "index price" is honestly sourced from FiatValueHelper's own
		// definition of HARV's market rate — the cheapest currently-open
		// SELL listing in this currency (see that class's own comment: HARV
		// has no real external exchange listing anywhere, so a fabricated
		// Binance/CMC-style feed would be dishonest). Both the fat-finger
		// guardrail (Fixed mode) and the margin calculation (Floating
		// mode) key off this same real number.
		FiatValueHelper.getCheapestRate(currency, rate -> {
			indexPrice[0] = (rate != null && rate > 0) ? rate : -1;
			_refreshPricingDisplay(floatingMode[0], etPrice, etMargin, tvPriceNote,
				tvCalculatedPrice, priceNoteDefault, indexPrice[0], currency);
		});

		if (togglePricingMode != null) {
			togglePricingMode.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
				if (!isChecked) return;
				boolean nowFloating = checkedId == id(a, "btn_" + p + "_price_floating");
				floatingMode[0] = nowFloating;
				if (groupPriceFixed != null) groupPriceFixed.setVisibility(nowFloating ? View.GONE : View.VISIBLE);
				if (groupPriceFloating != null) groupPriceFloating.setVisibility(nowFloating ? View.VISIBLE : View.GONE);
				_refreshPricingDisplay(floatingMode[0], etPrice, etMargin, tvPriceNote,
					tvCalculatedPrice, priceNoteDefault, indexPrice[0], currency);
			});
		}

		showStep(panel1, panel2, panel3, stepLabel, 1, sell);

		String address = WalletManager.getActiveWalletAddress(a);
		if (sell && tvAvailable != null) {
			tvAvailable.setText("…");
			WalletNodeBridge.fetchLiveBalance(a, address, new WalletNodeBridge.BalanceCallback() {
				@Override public void onSuccess(double balanceHarv, long nonce, long blockHeight) {
					available[0] = balanceHarv;
					tvAvailable.setText(BalanceFormatter.smart(balanceHarv));
				}
				@Override public void onError(String error) {
					available[0] = -1;
					tvAvailable.setText("—");
				}
			});
		}

		Runnable refreshPay = () -> bindPaymentMethods(a, paymentList, selectedIds, sell);
		PAYMENT_REFRESH.put(a, refreshPay);
		refreshPay.run();

		TextWatcher totals = new SimpleWatcher() {
			@Override public void afterTextChanged(Editable s) {
				double amount = parse(etAmount);
				double price = effectivePrice(floatingMode[0], etPrice, etMargin, indexPrice[0]);
				if (tvYou != null) {
					tvYou.setText(amount > 0 && price > 0 ? P2pMoney.fiat(amount * price, currency) : "—");
				}
				// maxOrder tracks the full amount until the merchant explicitly
				// customizes it via the Trade Limit sheet (currentMax stays -1
				// until then) — so typing a new amount keeps Max in sync
				// automatically right up until that first customization.
				if (maxOrder[0] < 0 || maxOrder[0] > amount) maxOrder[0] = amount;
				if (minOrder[0] > amount && amount > 0) minOrder[0] = Math.max(1, amount);
				if (minOrder[0] > maxOrder[0]) minOrder[0] = maxOrder[0];
				if (tvLimit != null && amount > 0) {
					tvLimit.setText(String.format(Locale.US, ("Min %s · Max %s " + com.harvana.app.CoinInfo.ticker()),
						BalanceFormatter.smartDecimal(minOrder[0]), BalanceFormatter.smartDecimal(maxOrder[0])));
				}
				_refreshPricingDisplay(floatingMode[0], etPrice, etMargin, tvPriceNote,
					tvCalculatedPrice, priceNoteDefault, indexPrice[0], currency);
			}
		};
		if (etAmount != null) etAmount.addTextChangedListener(totals);
		if (etPrice != null) etPrice.addTextChangedListener(totals);
		if (etMargin != null) etMargin.addTextChangedListener(totals);
		if (etAmount != null) etAmount.addTextChangedListener(new CommaFormattingWatcher(etAmount));
		if (etPrice != null) etPrice.addTextChangedListener(new CommaFormattingWatcher(etPrice));

		if (btnMax != null) {
			btnMax.setOnClickListener(v -> {
				if (available[0] > 0) etAmount.setText(BalanceFormatter.smartDecimal(available[0]));
			});
		}

		if (rowLimit != null) {
			rowLimit.setClickable(true);
			rowLimit.setFocusable(true);
			rowLimit.setOnClickListener(v -> showLimitSheet(a, parse(etAmount), minOrder[0], maxOrder[0], (chosenMin, chosenMax) -> {
				minOrder[0] = chosenMin;
				maxOrder[0] = chosenMax;
				if (tvLimit != null) tvLimit.setText(String.format(Locale.US, ("Min %s · Max %s " + com.harvana.app.CoinInfo.ticker()),
					BalanceFormatter.smartDecimal(chosenMin), BalanceFormatter.smartDecimal(chosenMax)));
			}));
		}
		if (rowDuration != null) {
			rowDuration.setClickable(true);
			rowDuration.setFocusable(true);
			rowDuration.setOnClickListener(v -> showDurationSheet(a, durationHours[0], (hours, label) -> {
				durationHours[0] = hours;
				if (tvDuration != null) tvDuration.setText(label);
			}));
		}
		if (rowPaymentWindow != null) {
			rowPaymentWindow.setClickable(true);
			rowPaymentWindow.setFocusable(true);
			rowPaymentWindow.setOnClickListener(v -> showPaymentWindowSheet(a, paymentWindowMinutes[0], (minutes, label) -> {
				paymentWindowMinutes[0] = minutes;
				if (tvPaymentWindow != null) tvPaymentWindow.setText(label);
			}));
		}

		if (etTerms != null && tvTermsCounter != null) {
			tvTermsCounter.setText(etTerms.getText().length() + "/500");
			etTerms.addTextChangedListener(new SimpleWatcher() {
				@Override public void afterTextChanged(Editable s) {
					tvTermsCounter.setText(s.length() + "/500");
				}
			});
		}

		if (rowMinAccountAge != null) {
			rowMinAccountAge.setClickable(true);
			rowMinAccountAge.setFocusable(true);
			rowMinAccountAge.setOnClickListener(v -> showMinAccountAgeSheet(a, minAccountAgeDays[0], (days, label) -> {
				minAccountAgeDays[0] = days;
				if (tvMinAccountAge != null) tvMinAccountAge.setText(label);
			}));
		}

		if (btnDeposit1 != null) btnDeposit1.setOnClickListener(v -> {
			a.startActivity(new Intent(a, ReceiveActivity.class));
		});
		if (btnDeposit2 != null) btnDeposit2.setOnClickListener(v -> {
			a.startActivity(new Intent(a, ReceiveActivity.class));
		});

		if (btnPreview != null) btnPreview.setOnClickListener(v -> {
			List<PaymentMethod> selected = selectedMethods(a, selectedIds);
			double price = effectivePrice(floatingMode[0], etPrice, etMargin, indexPrice[0]);
			String err = validate(sell, parse(etAmount), price, minOrder[0], maxOrder[0], selected, available[0],
				floatingMode[0], parseMargin(etMargin), indexPrice[0]);
			if (btnDeposit1 != null) btnDeposit1.setVisibility(INSUFFICIENT_BALANCE_ERROR.equals(err) ? View.VISIBLE : View.GONE);
			if (err != null) { showError(tvError1, err); return; }
			showError(tvError1, null);
			fillReview(a, sell, parse(etAmount), price, minOrder[0], maxOrder[0], durationHours[0],
				tvDuration != null ? tvDuration.getText().toString() : "7 days", currency, selected);
			showStep(panel1, panel2, panel3, stepLabel, 2, sell);
		});

		if (btnBack != null) btnBack.setOnClickListener(v -> showStep(panel1, panel2, panel3, stepLabel, 1, sell));

		if (btnEditPay != null) btnEditPay.setOnClickListener(v -> {
			a.startActivity(new Intent(a, PaymentMethodsActivity.class));
		});

		if (btnPublish != null) btnPublish.setOnClickListener(v -> {
			List<PaymentMethod> selected = selectedMethods(a, selectedIds);
			double price = effectivePrice(floatingMode[0], etPrice, etMargin, indexPrice[0]);
			String err = validate(sell, parse(etAmount), price, minOrder[0], maxOrder[0], selected, available[0],
				floatingMode[0], parseMargin(etMargin), indexPrice[0]);
			if (btnDeposit2 != null) btnDeposit2.setVisibility(INSUFFICIENT_BALANCE_ERROR.equals(err) ? View.VISIBLE : View.GONE);
			if (err != null) { showError(tvError2, err); return; }
			showError(tvError2, null);
			Intent confirm = new Intent(a, ComfirmSellOfferActivity.class);
			confirm.putExtra("side", sell ? "SELL" : "BUY");
			confirm.putExtra("amount", parse(etAmount));
			confirm.putExtra("unitPrice", price);
			confirm.putExtra("minOrderAmount", minOrder[0]);
			confirm.putExtra("maxOrderAmount", maxOrder[0]);
			confirm.putExtra("durationHours", durationHours[0]);
			confirm.putExtra("durationLabel", tvDuration != null ? tvDuration.getText().toString() : "7 days");
			confirm.putExtra("currency", currency);
			confirm.putExtra("pricingType", floatingMode[0] ? "MARGIN" : "FIXED");
			confirm.putExtra("marginPercent", floatingMode[0] ? parseMargin(etMargin) : 0.0);
			confirm.putExtra("paymentWindowMinutes", paymentWindowMinutes[0]);
			confirm.putExtra("termsOfTrade", etTerms != null ? etTerms.getText().toString().trim() : "");
			confirm.putExtra("reqAccountAgeDays", minAccountAgeDays[0]);
			confirm.putStringArrayListExtra("selectedPaymentMethodIds", new ArrayList<>(selectedIds));
			a.startActivity(confirm);
		});

		if (btnViewOffers != null) btnViewOffers.setOnClickListener(v ->
			a.startActivity(new Intent(a, P2pMerchantManagerActivity.class)));
		if (btnAnother != null) btnAnother.setOnClickListener(v -> {
			if (etAmount != null) etAmount.setText("");
			if (etPrice != null) etPrice.setText("");
			showStep(panel1, panel2, panel3, stepLabel, 1, sell);
		});
	}

	private static int id(AppCompatActivity a, String name) {
		return a.getResources().getIdentifier(name, "id", a.getPackageName());
	}

	private static double parse(EditText et) {
		if (et == null) return 0;
		return BalanceFormatter.parseUserInput(et.getText().toString());
	}

	// Margin needs a sign (a discount is a negative margin), which
	// BalanceFormatter.parseUserInput deliberately rejects (it's built for
	// HARV amounts, which are never negative) — so this gets its own parser.
	private static double parseMargin(EditText etMargin) {
		if (etMargin == null) return 0;
		String raw = etMargin.getText().toString().trim();
		if (raw.isEmpty() || raw.equals("-") || raw.equals("+")) return 0;
		try {
			double v = Double.parseDouble(raw.replace(",", ""));
			return Double.isFinite(v) ? v : 0;
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	/** The unit price actually used for validation/preview/publish: the raw
	 *  Fixed input, or Index_Price * (1 + margin/100) in Floating mode. */
	private static double effectivePrice(boolean floating, EditText etPrice, EditText etMargin, double indexPrice) {
		if (!floating) return parse(etPrice);
		if (indexPrice <= 0) return 0;
		return indexPrice * (1 + (parseMargin(etMargin) / 100.0));
	}

	// Fixed mode: the fat-finger guardrail — warns (doesn't block) when the
	// entered price is >10% away from the real index price. Floating mode:
	// live preview of the calculated price from the current margin.
	private static void _refreshPricingDisplay(boolean floating, EditText etPrice, EditText etMargin,
	                                            TextView tvNote, TextView tvCalculatedPrice,
	                                            String noteDefault, double indexPrice, String currency) {
		if (!floating) {
			if (tvNote == null) return;
			double price = parse(etPrice);
			if (indexPrice > 0 && price > 0) {
				double diffPct = Math.abs(price - indexPrice) / indexPrice * 100.0;
				if (diffPct > 10) {
					tvNote.setText(String.format(Locale.US,
						"Your price is significantly different from the market rate of %s.",
						P2pMoney.perHarv(indexPrice, currency)));
					tvNote.setTextColor(tvNote.getResources().getColor(R.color.colorWarning)); // colorWarning
					return;
				}
			}
			tvNote.setText(noteDefault);
			tvNote.setTextColor(tvNote.getContext().getResources().getColor(R.color.colorTextPrimary));
			tvNote.setAlpha(0.8f);
		} else {
			if (tvCalculatedPrice == null) return;
			if (indexPrice <= 0) {
				tvCalculatedPrice.setText("No market rate available yet — at least one open SELL listing is needed to price against.");
				return;
			}
			double margin = parseMargin(etMargin);
			double calculated = indexPrice * (1 + (margin / 100.0));
			if (margin < -20 || margin > 20) {
				tvCalculatedPrice.setText("Margin must be between -20% and +20%.");
				tvCalculatedPrice.setTextColor(tvCalculatedPrice.getResources().getColor(R.color.colorError)); // colorError
			} else {
				tvCalculatedPrice.setText(String.format(Locale.US, "Current price: %s (market %s)",
					P2pMoney.perHarv(calculated, currency), P2pMoney.perHarv(indexPrice, currency)));
				tvCalculatedPrice.setTextColor(tvCalculatedPrice.getContext().getResources().getColor(R.color.colorAccent));
			}
		}
	}

	private static String validate(boolean sell, double amount, double price, double minOrder, double maxOrder,
	                               List<PaymentMethod> methods, double available,
	                               boolean floating, double marginPercent, double indexPrice) {
		if (amount < 10 || amount > 5000) return ("A listing must be between 10 and 5,000 " + com.harvana.app.CoinInfo.ticker() + ".");
		if (floating) {
			if (indexPrice <= 0) return "No market rate available yet — floating pricing needs at least one open listing to price against. Use Fixed Price instead.";
			if (marginPercent < -20 || marginPercent > 20) return "Floating margin must be between -20% and +20%.";
		}
		if (price <= 0) return "Enter a unit price greater than zero.";
		if (minOrder < 1) return ("Minimum trade must be at least 1 " + com.harvana.app.CoinInfo.ticker() + ".");
		if (maxOrder > amount) return "Maximum trade can't be more than the total you're listing.";
		if (minOrder > maxOrder) return "Minimum trade can't be more than the maximum trade.";
		if (sell && methods.isEmpty()) return "Pick at least one payment method so buyers know where to pay you.";
		if (sell && available < 0) return ("Couldn't load your " + com.harvana.app.CoinInfo.ticker() + " balance — go back and try again.");
		if (sell && amount > available) return INSUFFICIENT_BALANCE_ERROR;
		return null;
	}

	private static void showError(TextView tv, String msg) {
		if (tv == null) return;
		if (msg == null) { tv.setVisibility(View.GONE); return; }
		tv.setText(msg);
		tv.setVisibility(View.VISIBLE);
	}

	private static void showStep(View p1, View p2, View p3, TextView label, int step, boolean sell) {
		if (p1 != null) p1.setVisibility(step == 1 ? View.VISIBLE : View.GONE);
		if (p2 != null) p2.setVisibility(step == 2 ? View.VISIBLE : View.GONE);
		if (p3 != null) p3.setVisibility(step == 3 ? View.VISIBLE : View.GONE);
		if (label != null) {
			if (step == 1) label.setText("Step 1 of 3 · Amount & Price");
			else if (step == 2) label.setText(sell ? "Step 2 of 3 · Review sell offer" : "Step 2 of 3 · Review buy offer");
			else label.setText("Step 3 of 3 · Published");
		}
	}

	private static List<PaymentMethod> selectedMethods(AppCompatActivity a, Set<String> ids) {
		List<PaymentMethod> out = new ArrayList<>();
		for (PaymentMethod pm : PaymentMethodManager.loadAll(a)) {
			if (ids.contains(pm.id)) out.add(pm);
		}
		return out;
	}

	private static void bindPaymentMethods(AppCompatActivity a, LinearLayout list, Set<String> selectedIds, boolean sell) {
		if (list == null) return;
		list.removeAllViews();
		List<PaymentMethod> all = PaymentMethodManager.loadAll(a);
		if (all.isEmpty()) {
			TextView empty = new TextView(a);
			empty.setText(sell
				? "No payment methods yet. Add a bank or mobile-money account in Settings so buyers can pay you."
				: "Optional — add a payment method you can pay with, or skip and choose at trade time.");
			empty.setTextColor(ThemedDialogs.onSurfaceVariant(a));
			empty.setTextSize(13f);
			empty.setPadding(0, 8, 0, 12);
			list.addView(empty);
			MaterialButton add = new MaterialButton(a, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
			add.setText("Add payment method");
			add.setTextColor(ThemedDialogs.accent(a));
			add.setOnClickListener(v -> a.startActivity(new Intent(a, PaymentMethodsActivity.class)));
			list.addView(add);
			return;
		}
		if (selectedIds.isEmpty()) {
			for (PaymentMethod pm : all) {
				if (pm.isDefault) selectedIds.add(pm.id);
			}
			if (selectedIds.isEmpty() && sell) selectedIds.add(all.get(0).id);
		}
		for (PaymentMethod pm : all) {
			LinearLayout row = ThemedDialogs.sheetRow(a);
			CheckBox cb = new com.google.android.material.checkbox.MaterialCheckBox(a);
			cb.setText((pm.label != null && !pm.label.isEmpty() ? pm.label : pm.primaryField)
				+ "\n" + pm.displaySummary());
			cb.setTextColor(ThemedDialogs.onSurface(a));
			cb.setChecked(selectedIds.contains(pm.id));
			CompoundButtonCompat.setButtonTintList(cb, android.content.res.ColorStateList.valueOf(ThemedDialogs.accent(a)));
			cb.setOnCheckedChangeListener((button, checked) -> {
				if (checked) selectedIds.add(pm.id); else selectedIds.remove(pm.id);
			});
			row.addView(cb);
			list.addView(row);
		}
	}

	private static void fillReview(AppCompatActivity a, boolean sell, double amount, double price,
	                               double minOrder, double maxOrder, long hours, String durationLabel, String currency,
	                               List<PaymentMethod> methods) {
		String p = sell ? "sell" : "buy";
		setText(a, "tv_" + p + "_review_amount", BalanceFormatter.smart(amount));
		setText(a, "tv_" + p + "_review_unit_price", P2pMoney.perHarv(price, currency));
		setText(a, "tv_" + p + "_review_total_price", P2pMoney.fiat(amount * price, currency));
		setText(a, "tv_" + p + "_review_fee", "Set by the network at match time");
		setText(a, "tv_" + p + "_review_trade_limit",
			String.format(Locale.US, "Min %s · Max %s", BalanceFormatter.smart(minOrder), BalanceFormatter.smart(maxOrder)));
		setText(a, "tv_" + p + "_review_duration", durationLabel);
		LinearLayout pay = a.findViewById(id(a, "layout_" + p + "_review_payment_methods"));
		if (pay != null) {
			pay.removeAllViews();
			if (methods.isEmpty()) {
				TextView t = new TextView(a);
				t.setText("None selected");
				t.setTextColor(ThemedDialogs.onSurfaceVariant(a));
				pay.addView(t);
			} else {
				for (PaymentMethod pm : methods) {
					TextView t = new TextView(a);
					t.setText("✓  " + (pm.label != null && !pm.label.isEmpty() ? pm.label : pm.displaySummary()));
					t.setTextColor(ThemedDialogs.onSurface(a));
					t.setTextSize(13.5f);
					t.setPadding(0, 8, 0, 8);
					pay.addView(t);
				}
			}
		}
	}

	private static void setText(AppCompatActivity a, String name, String value) {
		TextView tv = a.findViewById(id(a, name));
		if (tv != null) tv.setText(value);
	}

	private static void showDurationSheet(AppCompatActivity a, long current, DurationPick pick) {
		BottomSheetDialog dialog = ThemedDialogs.sheet(a);
		LinearLayout root = ThemedDialogs.sheetRoot(a);
		root.addView(ThemedDialogs.sheetTitle(a, "Offer duration"));
		RadioGroup group = new RadioGroup(a);
		group.setOrientation(RadioGroup.VERTICAL);
		RadioButton selected = null;
		for (int i = 0; i < DURATION_HOURS.length; i++) {
			RadioButton rb = new RadioButton(a);
			rb.setText(DURATION_LABELS[i]);
			rb.setTextColor(ThemedDialogs.onSurface(a));
			rb.setTag(DURATION_HOURS[i]);
			rb.setMinHeight(ThemedDialogs.dp(a, 48));
			CompoundButtonCompat.setButtonTintList(rb, android.content.res.ColorStateList.valueOf(ThemedDialogs.accent(a)));
			if (DURATION_HOURS[i] == current) selected = rb;
			group.addView(rb);
		}
		if (selected != null) selected.setChecked(true);
		root.addView(group);
		LinearLayout buttons = ThemedDialogs.sheetButtonRow(a);
		MaterialButton cancel = ThemedDialogs.outlineButton(a, "Cancel");
		MaterialButton ok = ThemedDialogs.filledButton(a, "Confirm");
		cancel.setOnClickListener(v -> dialog.dismiss());
		ok.setOnClickListener(v -> {
			int checked = group.getCheckedRadioButtonId();
			RadioButton rb = group.findViewById(checked);
			if (rb != null) pick.onPicked((Long) rb.getTag(), rb.getText().toString());
			dialog.dismiss();
		});
		buttons.addView(cancel);
		buttons.addView(ok);
		root.addView(buttons);
		dialog.setContentView(root);
		dialog.show();
	}

	private static void showLimitSheet(AppCompatActivity a, double amount, double currentMin, double currentMax, LimitPick pick) {
		BottomSheetDialog dialog = ThemedDialogs.sheet(a);
		LinearLayout root = ThemedDialogs.sheetRoot(a);
		root.addView(ThemedDialogs.sheetTitle(a, "Trade limit"));
		root.addView(ThemedDialogs.sheetLabel(a, "The smallest and largest amount a single buyer can take from this listing — a real cap, not just descriptive copy, so no one trade can lock up your whole offer while unpaid."));
		EditText minInput = new com.google.android.material.textfield.TextInputEditText(a);
		minInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
		minInput.setTextColor(ThemedDialogs.onSurface(a));
		minInput.setHintTextColor(ThemedDialogs.onSurfaceVariant(a));
		minInput.setHint(("Min " + com.harvana.app.CoinInfo.ticker()));
		if (currentMin > 0) minInput.setText(BalanceFormatter.smartDecimal(currentMin));
		root.addView(minInput);
		EditText maxInput = new com.google.android.material.textfield.TextInputEditText(a);
		maxInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
		maxInput.setTextColor(ThemedDialogs.onSurface(a));
		maxInput.setHintTextColor(ThemedDialogs.onSurfaceVariant(a));
		maxInput.setHint(("Max " + com.harvana.app.CoinInfo.ticker()));
		double initialMax = currentMax > 0 ? currentMax : amount;
		if (initialMax > 0) maxInput.setText(BalanceFormatter.smartDecimal(initialMax));
		root.addView(maxInput);
		LinearLayout buttons = ThemedDialogs.sheetButtonRow(a);
		MaterialButton cancel = ThemedDialogs.outlineButton(a, "Cancel");
		MaterialButton ok = ThemedDialogs.filledButton(a, "Confirm");
		cancel.setOnClickListener(v -> dialog.dismiss());
		ok.setOnClickListener(v -> {
			double minAmount = BalanceFormatter.parseUserInput(minInput.getText().toString());
			double maxAmount = BalanceFormatter.parseUserInput(maxInput.getText().toString());
			if (minAmount < 1) minAmount = 1;
			if (maxAmount <= 0 || (amount > 0 && maxAmount > amount)) maxAmount = amount > 0 ? amount : minAmount;
			if (minAmount > maxAmount) minAmount = maxAmount;
			pick.onPicked(minAmount, maxAmount);
			dialog.dismiss();
		});
		buttons.addView(cancel);
		buttons.addView(ok);
		root.addView(buttons);
		dialog.setContentView(root);
		dialog.show();
	}

	private static final int[] PAYMENT_WINDOW_MINUTES = {15, 30, 45, 60};
	private static final String[] PAYMENT_WINDOW_LABELS = {"15 Minutes", "30 Minutes", "45 Minutes", "1 Hour"};

	private static void showPaymentWindowSheet(AppCompatActivity a, int current, PaymentWindowPick pick) {
		BottomSheetDialog dialog = ThemedDialogs.sheet(a);
		LinearLayout root = ThemedDialogs.sheetRoot(a);
		root.addView(ThemedDialogs.sheetTitle(a, "Payment window"));
		root.addView(ThemedDialogs.sheetLabel(a, "How long a buyer has to pay before escrow auto-cancels the trade."));
		RadioGroup group = new RadioGroup(a);
		group.setOrientation(RadioGroup.VERTICAL);
		RadioButton selected = null;
		for (int i = 0; i < PAYMENT_WINDOW_MINUTES.length; i++) {
			RadioButton rb = new RadioButton(a);
			rb.setText(PAYMENT_WINDOW_LABELS[i]);
			rb.setTextColor(ThemedDialogs.onSurface(a));
			rb.setTag(PAYMENT_WINDOW_MINUTES[i]);
			rb.setMinHeight(ThemedDialogs.dp(a, 48));
			CompoundButtonCompat.setButtonTintList(rb, android.content.res.ColorStateList.valueOf(ThemedDialogs.accent(a)));
			if (PAYMENT_WINDOW_MINUTES[i] == current) selected = rb;
			group.addView(rb);
		}
		if (selected != null) selected.setChecked(true);
		root.addView(group);
		LinearLayout buttons = ThemedDialogs.sheetButtonRow(a);
		MaterialButton cancel = ThemedDialogs.outlineButton(a, "Cancel");
		MaterialButton ok = ThemedDialogs.filledButton(a, "Confirm");
		cancel.setOnClickListener(v -> dialog.dismiss());
		ok.setOnClickListener(v -> {
			int checked = group.getCheckedRadioButtonId();
			RadioButton rb = group.findViewById(checked);
			if (rb != null) pick.onPicked((Integer) rb.getTag(), rb.getText().toString());
			dialog.dismiss();
		});
		buttons.addView(cancel);
		buttons.addView(ok);
		root.addView(buttons);
		dialog.setContentView(root);
		dialog.show();
	}

	private static final int[] MIN_ACCOUNT_AGE_DAYS = {0, 30, 90, 180};
	private static final String[] MIN_ACCOUNT_AGE_LABELS = {"No limit", "30 days", "90 days", "180 days"};

	private static void showMinAccountAgeSheet(AppCompatActivity a, int current, MinAccountAgePick pick) {
		BottomSheetDialog dialog = ThemedDialogs.sheet(a);
		LinearLayout root = ThemedDialogs.sheetRoot(a);
		root.addView(ThemedDialogs.sheetTitle(a, "Minimum account age"));
		root.addView(ThemedDialogs.sheetLabel(a, "Only accounts older than this can take your offer."));
		RadioGroup group = new RadioGroup(a);
		group.setOrientation(RadioGroup.VERTICAL);
		RadioButton selected = null;
		for (int i = 0; i < MIN_ACCOUNT_AGE_DAYS.length; i++) {
			RadioButton rb = new RadioButton(a);
			rb.setText(MIN_ACCOUNT_AGE_LABELS[i]);
			rb.setTextColor(ThemedDialogs.onSurface(a));
			rb.setTag(MIN_ACCOUNT_AGE_DAYS[i]);
			rb.setMinHeight(ThemedDialogs.dp(a, 48));
			CompoundButtonCompat.setButtonTintList(rb, android.content.res.ColorStateList.valueOf(ThemedDialogs.accent(a)));
			if (MIN_ACCOUNT_AGE_DAYS[i] == current) selected = rb;
			group.addView(rb);
		}
		if (selected != null) selected.setChecked(true);
		root.addView(group);
		LinearLayout buttons = ThemedDialogs.sheetButtonRow(a);
		MaterialButton cancel = ThemedDialogs.outlineButton(a, "Cancel");
		MaterialButton ok = ThemedDialogs.filledButton(a, "Confirm");
		cancel.setOnClickListener(v -> dialog.dismiss());
		ok.setOnClickListener(v -> {
			int checked = group.getCheckedRadioButtonId();
			RadioButton rb = group.findViewById(checked);
			if (rb != null) pick.onPicked((Integer) rb.getTag(), rb.getText().toString());
			dialog.dismiss();
		});
		buttons.addView(cancel);
		buttons.addView(ok);
		root.addView(buttons);
		dialog.setContentView(root);
		dialog.show();
	}

	private interface DurationPick { void onPicked(long hours, String label); }
	private interface LimitPick { void onPicked(double minHarv, double maxHarv); }
	private interface PaymentWindowPick { void onPicked(int minutes, String label); }
	private interface MinAccountAgePick { void onPicked(int days, String label); }

	private static abstract class SimpleWatcher implements TextWatcher {
		@Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
		@Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
	}

	// Live comma-formatting for et_amount/et_price (e.g. "1000500" ->
	// "1,000,500" as the user types). Safe against the classic TextWatcher
	// infinite-loop trap: the `editing` guard means setText() re-triggering
	// this same watcher just short-circuits immediately instead of
	// recursing. BalanceFormatter.parseUserInput() and this file's own
	// parse()/parseMargin() already strip commas before parsing, so no
	// downstream calculation needed any change to work with this.
	private static class CommaFormattingWatcher extends SimpleWatcher {
		private final EditText target;
		private boolean editing = false;
		CommaFormattingWatcher(EditText target) { this.target = target; }
		@Override public void afterTextChanged(Editable s) {
			if (editing) return;
			String raw = s.toString().replace(",", "");
			if (raw.isEmpty()) return;
			String integerPart = raw;
			String decimalPart = null;
			int dot = raw.indexOf('.');
			if (dot >= 0) {
				integerPart = raw.substring(0, dot);
				decimalPart = raw.substring(dot + 1).replace(".", "");
			}
			if (integerPart.isEmpty()) integerPart = "0";
			String formattedInt;
			try {
				formattedInt = new java.text.DecimalFormat("#,###").format(Long.parseLong(integerPart));
			} catch (NumberFormatException e) {
				formattedInt = integerPart; // absurdly large input: show raw digits rather than crash
			}
			String formatted = dot >= 0 ? formattedInt + "." + decimalPart : formattedInt;
			if (formatted.equals(s.toString())) return;
			editing = true;
			target.setText(formatted);
			target.setSelection(Math.min(formatted.length(), target.getText().length()));
			editing = false;
		}
	}
}
