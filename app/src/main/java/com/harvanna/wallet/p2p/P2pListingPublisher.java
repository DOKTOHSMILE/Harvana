package com.harvanna.wallet.p2p;

import android.app.Activity;
import android.app.ProgressDialog;

import com.harvanna.wallet.tx.P2PTradeRouter;
import com.harvanna.wallet.wallet.PaymentMethod;
import com.harvanna.wallet.wallet.PinPromptHelper;
import com.harvanna.wallet.wallet.WalletManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public final class P2pListingPublisher {
	
	private P2pListingPublisher() {}
	
	public interface Callback {
		void onPublished(String listingId);
		void onPartial(String listingId, String firestoreError);
		void onError(String error);
	}
	
	// FIX 1: Added pricingType and marginPercent to the method signature
	public static void publish(Activity activity, String side, double amountAjc, 
	String pricingType, double unitPrice, double marginPercent,
	double minOrderAjc, double maxOrderAjc, long durationHours, int paymentWindowMinutes, String termsOfTrade, int reqAccountAgeDays, String currency,
	List<PaymentMethod> methods, Callback callback) {
		
		final Callback cb = callback != null ? callback : new Callback() {
			@Override public void onPublished(String listingId) {}
			@Override public void onPartial(String listingId, String firestoreError) {}
			@Override public void onError(String error) {}
		};
		
		if (activity == null) {
			cb.onError("No activity.");
			return;
		}
		
		String address = WalletManager.getActiveWalletAddress(activity);
		if (address == null || address.isEmpty()) {
			cb.onError("No active wallet found.");
			return;
		}
		
		FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
		if (user == null) {
			cb.onError("You need to be signed in to publish an offer.");
			return;
		}
		
		List<PaymentMethod> safe = methods != null ? methods : new ArrayList<>();
		if ("SELL".equals(side) && safe.isEmpty()) {
			cb.onError("Pick at least one payment method so buyers know where to pay you.");
			return;
		}
		if (amountAjc < 10 || amountAjc > 5000) {
			cb.onError("A listing must be between 10 and 5,000 AJC in total.");
			return;
		}
		if (minOrderAjc < 1 || minOrderAjc > amountAjc) {
			cb.onError("The minimum trade amount must be at least 1 AJC and no more than the total you're listing.");
			return;
		}
		if (maxOrderAjc > amountAjc) {
			cb.onError("The maximum trade amount can't be more than the total you're listing.");
			return;
		}
		if (minOrderAjc > maxOrderAjc) {
			cb.onError("The minimum trade amount can't be more than the maximum.");
			return;
		}
		
		// FIX 1.5: Validate the new pricing model
		if ("MARGIN".equals(pricingType)) {
			if (marginPercent < -20.0 || marginPercent > 20.0) {
				cb.onError("Margin percentage cannot exceed ±20%.");
				return;
			}
		} else {
			if (unitPrice <= 0) {
				cb.onError("Enter a unit price greater than zero.");
				return;
			}
		}
		
		if (durationHours < 1 || durationHours > 720) {
			cb.onError("Offer duration must be between 1 hour and 30 days.");
			return;
		}
		if (paymentWindowMinutes < 15 || paymentWindowMinutes > 60) {
			cb.onError("Payment window must be 15, 30, 45, or 60 minutes.");
			return;
		}
		if (termsOfTrade != null && termsOfTrade.length() > 500) {
			cb.onError("Terms of Trade can't be longer than 500 characters.");
			return;
		}
		if (reqAccountAgeDays != 0 && reqAccountAgeDays != 30 && reqAccountAgeDays != 90 && reqAccountAgeDays != 180) {
			cb.onError("Minimum account age must be 0, 30, 90, or 180 days.");
			return;
		}
		
		final String firebaseUid = user.getUid();
		final String listingSide = side == null || side.isEmpty() ? "SELL" : side;
		final String fiatCode = currency == null || currency.isEmpty() ? P2pCurrencyFilterState.getCurrency() : currency;
		
		long amountRaw = com.harvanna.wallet.sync.AjooNodeClient.toRawUnits(amountAjc);
		long minRaw = com.harvanna.wallet.sync.AjooNodeClient.toRawUnits(minOrderAjc);
		
		PinPromptHelper.verifyPinThenProceed(activity, () -> {
			final ProgressDialog progress = new ProgressDialog(activity, com.harvanna.wallet.R.style.AjooP2pAlertDialog);
			progress.setMessage("SELL".equals(listingSide) ? "Publishing offer…" : "Publishing request…");
			progress.setCancelable(false);
			progress.show();
			
			// 1. Send the liquidity lock request to Ajoochain
			P2PTradeRouter.createListing(activity.getApplicationContext(), address, listingSide,
			amountRaw, minRaw, durationHours, new P2PTradeRouter.Callback() {
				
				@Override
				public void onSuccess(org.json.JSONObject result) {
					String listingId = P2pIds.listingId(result);
					if (listingId.isEmpty()) {
						activity.runOnUiThread(() -> {
							if (!activity.isFinishing() && progress.isShowing()) progress.dismiss();
							cb.onError("Listing went live but the node didn't return an id. Check Merchant Manager.");
						});
						return;
					}
					
					// 2. Save the non-crypto metadata to Firestore
					// Updated to pass the pricingType and marginPercent
					com.harvanna.wallet.firestore.P2PPaymentDetailsManager.getInstance(activity.getApplicationContext())
					.save(listingId, safe, pricingType, unitPrice, marginPercent, paymentWindowMinutes, termsOfTrade, reqAccountAgeDays, maxOrderAjc, fiatCode, firebaseUid,
					new com.harvanna.wallet.firestore.P2PPaymentDetailsManager.SimpleCallback() {
						
						@Override
						public void onSuccess() {
							activity.runOnUiThread(() -> {
								if (!activity.isFinishing() && progress.isShowing()) progress.dismiss();
								cb.onPublished(listingId);
							});
						}
						
						@Override
						public void onError(String firestoreError) {
							// FIX 2: THE ROLLBACK (Orphaned Listing Prevention)
							activity.runOnUiThread(() -> progress.setMessage("Database error. Reverting blockchain lock..."));
							
							P2PTradeRouter.cancelListing(activity.getApplicationContext(), address, listingId, new P2PTradeRouter.Callback() {
								@Override
								public void onSuccess(org.json.JSONObject revertResult) {
									// Rollback successful, funds returned.
									activity.runOnUiThread(() -> {
										if (!activity.isFinishing() && progress.isShowing()) progress.dismiss();
										cb.onError("Database sync failed. Your listing was cancelled and funds have been unlocked securely.");
									});
								}
								
								@Override
								public void onError(String revertError) {
									// Rollback failed (e.g. internet completely dropped). 
									// Escalate to onPartial so the UI can cache it for a background retry.
									activity.runOnUiThread(() -> {
										if (!activity.isFinishing() && progress.isShowing()) progress.dismiss();
										cb.onPartial(listingId, "Network dropped completely. Offer is on-chain but missing details. ID: " + listingId);
									});
								}
							});
						}
					});
				}
				
				@Override
				public void onError(String error) {
					activity.runOnUiThread(() -> {
						if (!activity.isFinishing() && progress.isShowing()) progress.dismiss();
						cb.onError(friendly(error));
					});
				}
			});
		});
	}
	
	public static String friendly(String rawError) {
		if (rawError == null) return "Unknown error.";
		if (rawError.contains("AMOUNT_OUT_OF_BOUNDS")) return "A listing must be between 10 and 5,000 AJC in total.";
		if (rawError.contains("INVALID_MIN_ORDER_AMOUNT")) return "The minimum trade amount must be at least 1 AJC and no more than the total you're listing.";
		if (rawError.contains("INVALID_DURATION")) return "Offer duration must be between 1 hour and 30 days.";
		if (rawError.contains("SENDER_ADDRESS_FROZEN")) return "This wallet is currently frozen and can't post listings.";
		if (rawError.contains("COIN_PAUSED")) return "AJC trading is temporarily paused network-wide. Try again later.";
		if (rawError.contains("INVALID_SIGNATURE") || rawError.contains("INVALID_NONCE")) return "Something went wrong verifying your request — please try again.";
		return rawError;
	}
}
