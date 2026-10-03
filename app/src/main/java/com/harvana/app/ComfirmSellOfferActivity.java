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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.*;
import androidx.core.widget.NestedScrollView;
import com.harvana.app.p2p.P2pListingPublisher;
import com.harvana.app.p2p.P2pMoney;
import com.harvana.app.p2p.ThemedDialogs;
import com.harvana.app.wallet.BalanceFormatter;
import com.harvana.app.wallet.PaymentMethod;
import com.harvana.app.wallet.PaymentMethodManager;
import com.harvana.app.wallet.WalletManager;
import com.budiyev.android.codescanner.*;
import com.bumptech.glide.*;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.material.button.*;
import com.google.android.material.card.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.*;
import org.json.*;



public class ComfirmSellOfferActivity extends AppCompatActivity {
	
	private NestedScrollView vscroll1;
	private LinearLayout linear3;
	private LinearLayout linear4;
	private MaterialCardView cardview5;
	private TextView textview7;
	private MaterialCardView cardview6;
	private MaterialCardView cardview7;
	private MaterialCardView cardview8;
	private MaterialButton btn_publish_offer;
	private MaterialButton btn_save_as_draft;
	private MaterialButton btn_cancel_sell_offer;
	private TextView textview14;
	private MaterialCardView cardview9;
	private ImageView btn_confirm_sell_back;
	private LinearLayout linear5;
	private TextView textview6;
	private TextView tv_confirm_title;
	private TextView tv_confirm_subtitle;
	private LinearLayout linear6;
	private TextView tv_confirm_seller_initial;
	private LinearLayout linear7;
	private TextView tv_confirm_seller_status;
	private TextView tv_confirm_seller_name;
	private TextView tv_confirm_seller_rating;
	private LinearLayout linear8;
	private LinearLayout linear9;
	private LinearLayout linear10;
	private View view11;
	private LinearLayout linear12;
	private LinearLayout linear13;
	private LinearLayout linear14;
	private TextView tv_confirm_amount_label;
	private TextView tv_confirm_amount;
	private TextView textview8;
	private TextView tv_confirm_unit_price;
	private TextView tv_confirm_pay_direction_label;
	private TextView tv_confirm_buyers_pay;
	private TextView textview9;
	private TextView tv_confirm_trade_limit;
	private TextView textview10;
	private TextView tv_confirm_duration;
	private LinearLayout linear15;
	private LinearLayout linear16;
	private View view17;
	private LinearLayout layout_confirm_payment_methods;
	private TextView textview11;
	private TextView btn_confirm_edit_payment;
	private LinearLayout linear18;
	private TextView textview12;
	private TextView textview13;
	private LinearLayout linear19;
	private TextView textview15;
	private LinearLayout linear20;
	private LinearLayout linear21;
	private LinearLayout linear22;
	private LinearLayout linear23;
	private LinearLayout linear24;
	private ImageView imageview1;
	private TextView textview16;
	private TextView textview17;
	private ImageView imageview2;
	private TextView textview18;
	private TextView textview19;
	private ImageView imageview3;
	private TextView textview20;
	private TextView textview21;
	private ImageView imageview4;
	private TextView textview22;
	private TextView textview23;
	
	private FirebaseAuth fauth;
	private OnCompleteListener<AuthResult> _fauth_create_user_listener;
	private OnCompleteListener<AuthResult> _fauth_sign_in_listener;
	private OnCompleteListener<Void> _fauth_reset_password_listener;
	private OnCompleteListener<Void> fauth_updateEmailListener;
	private OnCompleteListener<Void> fauth_updatePasswordListener;
	private OnCompleteListener<Void> fauth_emailVerificationSentListener;
	private OnCompleteListener<Void> fauth_deleteUserListener;
	private OnCompleteListener<Void> fauth_updateProfileListener;
	private OnCompleteListener<AuthResult> fauth_phoneAuthListener;
	private OnCompleteListener<AuthResult> fauth_googleSignInListener;
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.comfirm_sell_offer);
		initialize(_savedInstanceState);
		FirebaseApp.initializeApp(this);
		initializeLogic();
	}
	
	private void initialize(Bundle _savedInstanceState) {
		vscroll1 = findViewById(R.id.vscroll1);
		linear3 = findViewById(R.id.linear3);
		linear4 = findViewById(R.id.linear4);
		cardview5 = findViewById(R.id.cardview5);
		textview7 = findViewById(R.id.textview7);
		cardview6 = findViewById(R.id.cardview6);
		cardview7 = findViewById(R.id.cardview7);
		cardview8 = findViewById(R.id.cardview8);
		btn_publish_offer = findViewById(R.id.btn_publish_offer);
		btn_save_as_draft = findViewById(R.id.btn_save_as_draft);
		btn_cancel_sell_offer = findViewById(R.id.btn_cancel_sell_offer);
		textview14 = findViewById(R.id.textview14);
		cardview9 = findViewById(R.id.cardview9);
		btn_confirm_sell_back = findViewById(R.id.btn_confirm_sell_back);
		linear5 = findViewById(R.id.linear5);
		textview6 = findViewById(R.id.textview6);
		tv_confirm_title = findViewById(R.id.tv_confirm_title);
		tv_confirm_subtitle = findViewById(R.id.tv_confirm_subtitle);
		linear6 = findViewById(R.id.linear6);
		tv_confirm_seller_initial = findViewById(R.id.tv_confirm_seller_initial);
		linear7 = findViewById(R.id.linear7);
		tv_confirm_seller_status = findViewById(R.id.tv_confirm_seller_status);
		tv_confirm_seller_name = findViewById(R.id.tv_confirm_seller_name);
		tv_confirm_seller_rating = findViewById(R.id.tv_confirm_seller_rating);
		linear8 = findViewById(R.id.linear8);
		linear9 = findViewById(R.id.linear9);
		linear10 = findViewById(R.id.linear10);
		view11 = findViewById(R.id.view11);
		linear12 = findViewById(R.id.linear12);
		linear13 = findViewById(R.id.linear13);
		linear14 = findViewById(R.id.linear14);
		tv_confirm_amount_label = findViewById(R.id.tv_confirm_amount_label);
		tv_confirm_amount = findViewById(R.id.tv_confirm_amount);
		textview8 = findViewById(R.id.textview8);
		tv_confirm_unit_price = findViewById(R.id.tv_confirm_unit_price);
		tv_confirm_pay_direction_label = findViewById(R.id.tv_confirm_pay_direction_label);
		tv_confirm_buyers_pay = findViewById(R.id.tv_confirm_buyers_pay);
		textview9 = findViewById(R.id.textview9);
		tv_confirm_trade_limit = findViewById(R.id.tv_confirm_trade_limit);
		textview10 = findViewById(R.id.textview10);
		tv_confirm_duration = findViewById(R.id.tv_confirm_duration);
		linear15 = findViewById(R.id.linear15);
		linear16 = findViewById(R.id.linear16);
		view17 = findViewById(R.id.view17);
		layout_confirm_payment_methods = findViewById(R.id.layout_confirm_payment_methods);
		textview11 = findViewById(R.id.textview11);
		btn_confirm_edit_payment = findViewById(R.id.btn_confirm_edit_payment);
		linear18 = findViewById(R.id.linear18);
		textview12 = findViewById(R.id.textview12);
		textview13 = findViewById(R.id.textview13);
		linear19 = findViewById(R.id.linear19);
		textview15 = findViewById(R.id.textview15);
		linear20 = findViewById(R.id.linear20);
		linear21 = findViewById(R.id.linear21);
		linear22 = findViewById(R.id.linear22);
		linear23 = findViewById(R.id.linear23);
		linear24 = findViewById(R.id.linear24);
		imageview1 = findViewById(R.id.imageview1);
		textview16 = findViewById(R.id.textview16);
		textview17 = findViewById(R.id.textview17);
		imageview2 = findViewById(R.id.imageview2);
		textview18 = findViewById(R.id.textview18);
		textview19 = findViewById(R.id.textview19);
		imageview3 = findViewById(R.id.imageview3);
		textview20 = findViewById(R.id.textview20);
		textview21 = findViewById(R.id.textview21);
		imageview4 = findViewById(R.id.imageview4);
		textview22 = findViewById(R.id.textview22);
		textview23 = findViewById(R.id.textview23);
		fauth = FirebaseAuth.getInstance();
		
		fauth_updateEmailListener = _param1 -> {
			final boolean _success = _param1.isSuccessful();
			final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
			
		};
		
		fauth_updatePasswordListener = _param1 -> {
			final boolean _success = _param1.isSuccessful();
			final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
			
		};
		
		fauth_emailVerificationSentListener = _param1 -> {
			final boolean _success = _param1.isSuccessful();
			final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
			
		};
		
		fauth_deleteUserListener = _param1 -> {
			final boolean _success = _param1.isSuccessful();
			final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
			
		};
		
		fauth_phoneAuthListener = task -> {
			final boolean _success = task.isSuccessful();
			final String _errorMessage = task.getException() != null ? task.getException().getMessage() : "";
			
		};
		
		fauth_updateProfileListener = _param1 -> {
			final boolean _success = _param1.isSuccessful();
			final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
			
		};
		
		fauth_googleSignInListener = task -> {
			final boolean _success = task.isSuccessful();
			final String _errorMessage = task.getException() != null ? task.getException().getMessage() : "";
			
		};
		
		_fauth_create_user_listener = _param1 -> {
			final boolean _success = _param1.isSuccessful();
			final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
			
		};
		
		_fauth_sign_in_listener = _param1 -> {
			final boolean _success = _param1.isSuccessful();
			final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
			
		};
		
		_fauth_reset_password_listener = _param1 -> {
			final boolean _success = _param1.isSuccessful();
			
		};
	}
	
	private void initializeLogic() {
		btn_confirm_sell_back.setOnClickListener(v -> finish());
		btn_confirm_edit_payment.setOnClickListener(v -> finish()); // back to the editable screen underneath
		btn_cancel_sell_offer.setOnClickListener(v -> ThemedDialogs.show(ThemedDialogs.alert(this)
		.setTitle("Cancel this offer?")
		.setMessage("Nothing has been published yet — you'll lose what you entered.")
		.setPositiveButton("Discard", (d, w) -> {
			Intent marketplace = new Intent(ComfirmSellOfferActivity.this, P2pMarketplaceActivity.class);
			marketplace.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(marketplace);
			finish();
		})
		.setNegativeButton("Keep Editing", null)));
		
		// THE HONEST GAP: no draft-persistence system exists anywhere in
		// this app yet — saving one here would either silently do
		// nothing or need a whole new storage layer this button alone
		// shouldn't invent. Says so plainly rather than pretending to
		// save something that goes nowhere.
		btn_save_as_draft.setOnClickListener(v -> Toast.makeText(this,
		"Drafts aren't supported yet — publish now, or go back and finish this later.", Toast.LENGTH_LONG).show());
		
		btn_publish_offer.setOnClickListener(v -> _onPublish());
		
		Intent in = getIntent();
		side = in.getStringExtra("side");
		if (side == null || side.isEmpty()) side = "SELL";
		amount = in.getDoubleExtra("amount", 0);
		unitPrice = in.getDoubleExtra("unitPrice", 0);
		minOrderAmount = in.getDoubleExtra("minOrderAmount", amount);
		maxOrderAmount = in.getDoubleExtra("maxOrderAmount", amount);
		durationHours = in.getLongExtra("durationHours", 168L);
		durationLabel = in.getStringExtra("durationLabel");
		currency = in.getStringExtra("currency");
		if (currency == null) currency = "NGN";
		pricingType = in.getStringExtra("pricingType");
		if (pricingType == null || pricingType.isEmpty()) pricingType = "FIXED";
		marginPercent = in.getDoubleExtra("marginPercent", 0.0);
		paymentWindowMinutes = in.getIntExtra("paymentWindowMinutes", 15);
		termsOfTrade = in.getStringExtra("termsOfTrade");
		if (termsOfTrade == null) termsOfTrade = "";
		reqAccountAgeDays = in.getIntExtra("reqAccountAgeDays", 0);
		
		sellerAddress = WalletManager.getActiveWalletAddress(this);
		
		List<String> selectedIds = in.getStringArrayListExtra("selectedPaymentMethodIds");
		List<PaymentMethod> all = PaymentMethodManager.loadAll(this);
		selectedMethods = new ArrayList<>();
		if (selectedIds != null) {
			for (PaymentMethod pm : all) {
				if (selectedIds.contains(pm.id)) selectedMethods.add(pm);
			}
		}
		
		_renderSummary();
		_renderPaymentMethods();
		
	}
	
	public void _MORE() {
	}
	private double amount, unitPrice, minOrderAmount, maxOrderAmount, marginPercent;
	private long durationHours;
	private int paymentWindowMinutes = 15;
	private int reqAccountAgeDays = 0;
	private String durationLabel, currency, sellerAddress, side = "SELL", pricingType = "FIXED", termsOfTrade = "";
	private List<PaymentMethod> selectedMethods = new ArrayList<>();
	
	
	private void _renderSummary() {
		boolean isSell = "SELL".equals(side);
		tv_confirm_title.setText(isSell ? "Confirm Sell Offer" : "Confirm Buy Offer");
		tv_confirm_subtitle.setText(isSell ? "Review and publish your offer" : "Review and publish your request");
		tv_confirm_amount_label.setText(isSell ? "Amount to sell" : "Amount to buy");
		tv_confirm_pay_direction_label.setText(isSell ? "Buyers will pay you (est.)" : "You will pay (est.)");
		
		// THE HONEST GAP, matching this file's own layout comments: no
		// ratings/reviews/trade-count/online-status system exists
		// anywhere in this app — showing the real wallet address is
		// accurate, a fabricated rating would not be.
		String addr = sellerAddress != null ? sellerAddress : "";
		tv_confirm_seller_initial.setText(addr.length() > 4 ? addr.substring(2, 3).toUpperCase(Locale.US) : "?");
		tv_confirm_seller_name.setText(addr.isEmpty() ? "Your wallet" : addr);
		tv_confirm_seller_rating.setText("No rating yet");
		
		tv_confirm_amount.setText(BalanceFormatter.smartDecimal(amount) + (" " + com.harvana.app.CoinInfo.ticker()));
		tv_confirm_unit_price.setText(P2pMoney.perHarv(unitPrice, currency));
		_showPriceReference();
		tv_confirm_buyers_pay.setText(P2pMoney.fiat(amount * unitPrice, currency));
		tv_confirm_trade_limit.setText(String.format(Locale.US, ("Min %s " + com.harvana.app.CoinInfo.ticker() + " • Max %s " + com.harvana.app.CoinInfo.ticker()),
		BalanceFormatter.smartDecimal(minOrderAmount), BalanceFormatter.smartDecimal(maxOrderAmount)));
		tv_confirm_duration.setText(durationLabel != null ? durationLabel : (durationHours + " hours"));
	}
	
	private void _renderPaymentMethods() {
		layout_confirm_payment_methods.removeAllViews();
		for (PaymentMethod pm : selectedMethods) {
			TextView row = new TextView(this);
			row.setText("✓  " + (pm.label != null && !pm.label.isEmpty() ? pm.label : pm.primaryField));
			int padH = (int) (16 * getResources().getDisplayMetrics().density);
			int padV = (int) (10 * getResources().getDisplayMetrics().density);
			row.setPadding(padH, padV, padH, padV);
			row.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.colorP2pTextPrimary));
			row.setTextSize(13.5f);
			layout_confirm_payment_methods.addView(row);
		}
	}
	
	/** Shows the market reference (median of open listings, NOT the node mark - see P2pPriceReference) and
	 *  flags, without blocking, a price that deviates from it by more than 15%. */
	private void _showPriceReference() {
		if (!"NGN".equalsIgnoreCase(currency) || !"FIXED".equals(pricingType) || unitPrice <= 0) return;
		final long myKobo = com.harvana.app.p2p.P2pPriceReference.nairaToKobo(unitPrice);
		final TextView note = new TextView(this);
		note.setTextSize(12.5f);
		note.setPadding(0, (int) (6 * getResources().getDisplayMetrics().density), 0, 0);
		note.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.colorP2pTextSecondary));
		note.setText("Checking market reference…");
		android.view.ViewGroup parent = (android.view.ViewGroup) tv_confirm_subtitle.getParent();
		parent.addView(note, parent.indexOfChild(tv_confirm_subtitle) + 1);
		
		com.harvana.app.p2p.P2pPriceReference.fetch((medianKobo, samples) -> {
			if (isFinishing() || isDestroyed()) return;
			if (medianKobo <= 0) {
				note.setText("No market reference yet (not enough open listings). Your price is used as entered.");
				return;
			}
			double dev = com.harvana.app.p2p.P2pPriceReference.deviation(myKobo, medianKobo);
			String ref = "Market reference: " + P2pMoney.perHarv(medianKobo / 100.0, "NGN")
					+ " (median of " + samples + " open listing" + (samples == 1 ? "" : "s") + ")";
			if (Math.abs(dev) > com.harvana.app.p2p.P2pPriceReference.DEVIATION_FLAG) {
				note.setText(ref + "\nYour price is " + Math.round(Math.abs(dev) * 100) + "% "
						+ (dev > 0 ? "above" : "below")
						+ " it. You can still publish, but a price this far from the market may be rejected or ignored by buyers.");
				note.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.colorWarning));
			} else {
				note.setText(ref);
			}
		});
	}
	
	private String _formatMoney(double value) {
		return String.format(Locale.US, "%,.2f", value);
	}
	
	private void _onPublish() {
		boolean isSell = "SELL".equals(side);
		if (sellerAddress == null || sellerAddress.isEmpty()) {
			Toast.makeText(this, "No active wallet found.", Toast.LENGTH_SHORT).show();
			return;
		}
		if (isSell && selectedMethods.isEmpty()) {
			Toast.makeText(this, "No payment methods selected — go back and pick at least one.", Toast.LENGTH_SHORT).show();
			return;
		}
		
		FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
		if (currentUser == null) {
			Toast.makeText(this, "You must be signed in to publish an offer.", Toast.LENGTH_SHORT).show();
			return;
		}
		final String firebaseUid = currentUser.getUid();
		
		btn_publish_offer.setEnabled(false);
		P2pListingPublisher.publish(this, side, amount, pricingType, unitPrice, marginPercent, minOrderAmount, maxOrderAmount, durationHours, paymentWindowMinutes, termsOfTrade, reqAccountAgeDays, currency, selectedMethods,
		new P2pListingPublisher.Callback() {
			@Override
			public void onPublished(String listingId) {
				// publish() already saved the payment details itself (its
				// own onPublished is only invoked after that save already
				// succeeded) — this used to call .save() a second time
				// here too, a pointless duplicate write that just happened
				// to keep compiling until a signature change caught it.
				// Removed rather than patched back into sync again.
				Toast.makeText(ComfirmSellOfferActivity.this, isSell ? "Offer published." : "Buy request published.", Toast.LENGTH_SHORT).show();
				Intent marketplace = new Intent(ComfirmSellOfferActivity.this, P2pMarketplaceActivity.class);
				marketplace.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(marketplace);
				finish();
			}
			@Override
			public void onPartial(String listingId, String error) {
				btn_publish_offer.setEnabled(true);
				ThemedDialogs.show(ThemedDialogs.alert(ComfirmSellOfferActivity.this)
				.setTitle("Listing published, payment details failed")
				.setMessage("Your offer is live on-chain, but saving your payment details failed: " + error
				+ "\n\nBuyers won't see how to pay you until this is fixed. Try again from My Activity.")
				.setPositiveButton("OK", null));
			}
			@Override
			public void onError(String error) {
				btn_publish_offer.setEnabled(true);
				Toast.makeText(ComfirmSellOfferActivity.this, "Couldn't publish offer: " + error, Toast.LENGTH_LONG).show();
			}
		});
	}
	
	/** Translates the node's raw rejection codes into something a person
*  can actually act on — confirmed live that AMOUNT_OUT_OF_BOUNDS
*  was reaching the user completely raw ("HTTP 400:
*  {"error":"AMOUNT_OUT_OF_BOUNDS"}"). Checked by substring rather
*  than parsed as clean JSON, since the string this receives also
*  carries the "HTTP 400:" prefix ahead of the actual JSON body.
*  Falls back to the raw string for anything not recognized here,
*  so a genuinely unexpected error is never hidden, just the common,
*  anticipated ones made readable. */
	private String _friendlyPublishError(String rawError) {
		if (rawError == null) return "Unknown error.";
		if (rawError.contains("AMOUNT_OUT_OF_BOUNDS")) return ("A listing must be between 10 and 5,000 " + com.harvana.app.CoinInfo.ticker() + " in total.");
		if (rawError.contains("INVALID_MIN_ORDER_AMOUNT")) return ("The minimum trade amount must be at least 1 " + com.harvana.app.CoinInfo.ticker() + " and no more than the total you're listing.");
		if (rawError.contains("INVALID_DURATION")) return "Offer duration must be between 1 hour and 30 days.";
		if (rawError.contains("SENDER_ADDRESS_FROZEN")) return "This wallet is currently frozen and can't post listings.";
		if (rawError.contains("COIN_PAUSED")) return (com.harvana.app.CoinInfo.ticker() + " trading is temporarily paused network-wide. Try again later.");
		if (rawError.contains("INVALID_SIGNATURE") || rawError.contains("INVALID_NONCE")) return "Something went wrong verifying your request — please try again.";
		return rawError;
	}
	
	{
	}
	
}
