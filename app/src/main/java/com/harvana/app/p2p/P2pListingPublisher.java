package com.harvana.app.p2p;

import android.app.Activity;


import com.harvana.app.tx.P2PTradeRouter;
import com.harvana.app.wallet.PaymentMethod;
import com.harvana.app.wallet.WalletManager;
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
	public static void publish(Activity activity, String side, double amountHarv, 
	String pricingType, double unitPrice, double marginPercent,
	double minOrderHarv, double maxOrderHarv, long durationHours, int paymentWindowMinutes, String termsOfTrade, int reqAccountAgeDays, String currency,
	List<PaymentMethod> methods, Callback callback) {
		publish(activity, P2pAsset.HARV, side, amountHarv, pricingType, unitPrice, marginPercent, minOrderHarv, maxOrderHarv,
				durationHours, paymentWindowMinutes, termsOfTrade, reqAccountAgeDays, currency, methods, callback);
	}

	/** Any asset: "HARV", or a stable asset (hUSD, hNGN, …) whose escrow then moves that coin. Amounts are in coins. */
	public static void publish(Activity activity, String asset0, String side, double amountHarv,
	String pricingType, double unitPrice, double marginPercent,
	double minOrderHarv, double maxOrderHarv, long durationHours, int paymentWindowMinutes, String termsOfTrade, int reqAccountAgeDays, String currency,
	List<PaymentMethod> methods, Callback callback) {
		final String asset = P2pAsset.normalize(asset0);
		final String tk = P2pAsset.ticker(asset);
		
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
		if (amountHarv < P2pAsset.minListing(asset) || amountHarv > P2pAsset.maxListing(asset)) {
			cb.onError("A listing must be between " + com.harvana.app.wallet.BalanceFormatter.smartDecimal(P2pAsset.minListing(asset)) + " and "
					+ com.harvana.app.wallet.BalanceFormatter.smartDecimal(P2pAsset.maxListing(asset)) + " " + tk + " in total.");
			return;
		}
		if (minOrderHarv < P2pAsset.minOrder(asset) || minOrderHarv > amountHarv) {
			cb.onError("The minimum trade amount must be at least " + com.harvana.app.wallet.BalanceFormatter.smartDecimal(P2pAsset.minOrder(asset))
					+ " " + tk + " and no more than the total you're listing.");
			return;
		}
		if (P2pAsset.isStable(asset) && "MARGIN".equals(pricingType)) {
			cb.onError("Use a fixed price for " + tk + ".");
			return;
		}
		if (maxOrderHarv > amountHarv) {
			cb.onError("The maximum trade amount can't be more than the total you're listing.");
			return;
		}
		if (minOrderHarv > maxOrderHarv) {
			cb.onError("The minimum trade amount can't be more than the maximum.");
			return;
		}
		// The per-trade maximum is the lister's own cap. It only has to sit between the minimum and the total.
		// The node prices in kobo (Naira) per HARV; other currencies can't be signed on-chain.
		final String priceCurrency = currency == null || currency.isEmpty() ? P2pCurrencyFilterState.getCurrency() : currency;
		if (!"NGN".equalsIgnoreCase(priceCurrency)) {
			cb.onError("Offers are priced in Naira (NGN). Switch the currency to NGN to publish.");
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
		
		long amountRaw = P2pAsset.toRaw(amountHarv, asset);
		long minRaw = P2pAsset.toRaw(minOrderHarv, asset);
		
		final long maxRaw = P2pAsset.toRaw(maxOrderHarv, asset);
		final long[] priceKobo = new long[1];
		final Runnable startPublish = () -> {
			java.util.ArrayList<String[]> rows = new java.util.ArrayList<>();
			rows.add(new String[]{"Side", listingSide});
			rows.add(new String[]{"Coin", tk});
			rows.add(new String[]{"Each trade", com.harvana.app.wallet.BalanceFormatter.smart(minOrderHarv)
				+ " to " + com.harvana.app.wallet.BalanceFormatter.smart(maxOrderHarv)});
			rows.add(new String[]{"Price", "MARGIN".equals(pricingType)
				? (marginPercent + "% from the reference")
				: String.valueOf(unitPrice) + " " + fiatCode});
			rows.add(new String[]{"Open for", durationHours + " hours"});
			rows.add(new String[]{"Payment window", paymentWindowMinutes + " minutes"});
			rows.add(new String[]{"For", "SELL".equals(listingSide)
				? "Locks this " + tk + " into the listing until it sells or you cancel"
				: "Publishes a request. " + tk + " moves when someone fills it"});
			com.harvana.app.group.PaymentConfirmSheet.reviewThenAuthorize(activity,
			"SELL".equals(listingSide) ? "Publish sell offer" : "Publish buy request",
			com.harvana.app.wallet.BalanceFormatter.smart(amountHarv) + " " + tk, rows, () -> {
			final com.harvana.app.ui.ProgressSheet progress = new com.harvana.app.ui.ProgressSheet(activity);
			progress.setMessage("SELL".equals(listingSide) ? "Publishing offer…" : "Publishing request…");
			progress.setCancelable(false);
			progress.show();
			
			// 1. Send the liquidity lock request to Harvanachain
			P2PTradeRouter.createListing(activity.getApplicationContext(), address, listingSide,
			amountRaw, minRaw, maxRaw, durationHours, paymentWindowMinutes, priceKobo[0], asset, new P2PTradeRouter.Callback() {
				
				@Override
				public void onSuccess(org.json.JSONObject result) {
					String listingId = P2pIds.listingId(result);
					if (listingId.isEmpty()) {
						activity.runOnUiThread(() -> {
							if (!activity.isFinishing() && progress.isShowing()) progress.dismiss();
							cb.onError("Listing went live but the network didn't return an id. Check Merchant Manager.");
						});
						return;
					}
					
					// 2. Save the non-crypto metadata to Firestore
					// Updated to pass the pricingType and marginPercent
					com.harvana.app.firestore.P2PPaymentDetailsManager.getInstance(activity.getApplicationContext())
					.save(listingId, safe, pricingType, unitPrice, marginPercent, paymentWindowMinutes, termsOfTrade, reqAccountAgeDays, maxOrderHarv, fiatCode, firebaseUid,
					new com.harvana.app.firestore.P2PPaymentDetailsManager.SimpleCallback() {
						
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
							activity.runOnUiThread(() -> progress.setMessage("Database error. Undoing the lock…"));
							
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
		};
		final boolean harv = !P2pAsset.isStable(asset);
		if ("MARGIN".equals(pricingType)) {
			// HARV follows the hNGN swap pool, ±10%. A stable coin keeps the wider margin against whatever reference exists.
			P2pPriceReference.fetchQuote(q -> {
				if (q.midKobo <= 0) {
					activity.runOnUiThread(() -> cb.onError("Margin pricing needs a market reference and none is available yet. Use a fixed price."));
					return;
				}
				if (harv && q.pool && (marginPercent < -10.0 || marginPercent > 10.0)) {
					activity.runOnUiThread(() -> cb.onError("The swap pool only allows a margin of 10% either side."));
					return;
				}
				long price = Math.max(1L, Math.round(q.midKobo * (1.0 + marginPercent / 100.0)));
				if (harv && q.pool) price = Math.max(q.floorKobo, Math.min(q.ceilKobo, price));
				priceKobo[0] = price;
				activity.runOnUiThread(startPublish);
			});
		} else {
			priceKobo[0] = P2pPriceReference.nairaToKobo(unitPrice);
			if (priceKobo[0] < 1) {
				cb.onError("Enter a unit price greater than zero.");
				return;
			}
			if (!harv) { startPublish.run(); return; }
			P2pPriceReference.fetchQuote(q -> {
				if (q.pool && !P2pPriceReference.allowed(priceKobo[0], q.midKobo)) {
					String lo = P2pMoney.perUnit(q.floorKobo / 100.0, "NGN", asset);
					String hi = P2pMoney.perUnit(q.ceilKobo / 100.0, "NGN", asset);
					activity.runOnUiThread(() -> cb.onError("That price is outside the swap band (" + lo + " to " + hi + "). Stay within 10% of the pool."));
					return;
				}
				activity.runOnUiThread(startPublish);
			});
		}
	}
	
	public static String friendly(String rawError) {
		if (rawError == null) return "Unknown error.";
		if (com.harvana.app.utils.ErrorText.is(rawError, "AMOUNT_OUT_OF_BOUNDS")) return "A listing must be between 10 and 5,000 HARV in total.";
		if (com.harvana.app.utils.ErrorText.is(rawError, "INVALID_MAX_ORDER_AMOUNT")) return "The maximum trade amount must be between the minimum and the total you're listing.";
		if (com.harvana.app.utils.ErrorText.is(rawError, "INVALID_PAYMENT_WINDOW")) return "Payment window must be 15, 30, 45 or 60 minutes.";
		if (com.harvana.app.utils.ErrorText.is(rawError, "INVALID_UNIT_PRICE")) return "That price isn't valid. Enter a price per HARV in Naira.";
		if (com.harvana.app.utils.ErrorText.is(rawError, "PRICE_OUTSIDE_SWAP_BAND")) return "That price is more than 10% away from the swap pool. Move it inside the band and try again.";
		if (com.harvana.app.utils.ErrorText.is(rawError, "SWAP_PRICE_UNAVAILABLE")) return "The swap price could not be read. Try again in a moment.";
		if (com.harvana.app.utils.ErrorText.is(rawError, "INVALID_MIN_ORDER_AMOUNT")) return "The minimum trade amount must be at least 1 HARV and no more than the total you're listing.";
		if (com.harvana.app.utils.ErrorText.is(rawError, "INVALID_DURATION")) return "Offer duration must be between 1 hour and 30 days.";
		if (com.harvana.app.utils.ErrorText.is(rawError, "SENDER_ADDRESS_FROZEN")) return "This wallet is currently frozen and can't post listings.";
		if (com.harvana.app.utils.ErrorText.is(rawError, "COIN_PAUSED")) return "HARV trading is temporarily paused network-wide. Try again later.";
		if (com.harvana.app.utils.ErrorText.is(rawError, "INVALID_SIGNATURE") || com.harvana.app.utils.ErrorText.is(rawError, "INVALID_NONCE")) return "Something went wrong verifying your request — please try again.";
		return rawError;
	}
}
