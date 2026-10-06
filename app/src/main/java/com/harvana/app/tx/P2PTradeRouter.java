package com.harvana.app.tx;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import com.harvana.app.crypto.Ed25519;
import com.harvana.app.sync.HarvanaNodeClient;
import com.harvana.app.wallet.WalletManager;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

/**
* Local signing for the five P2P market actions (create/claim/confirm/
* cancel/dispute), mirroring TransactionRouter.executeNativeTransfer()'s
* exact pattern: fetch the current on-chain nonce, build the canonical
* message in the exact shape the node's model class expects, sign locally
* with the wallet's private key, submit alongside the public key.
*
* Every canonical message format here is copied verbatim from the
* corresponding node model class's getCanonicalMessage() — a mismatch of
* even one separator or field order produces a signature that verifies
* correctly on the Android side but is silently rejected by the node as
* INVALID_SIGNATURE, with nothing on this side indicating why.
*/
public final class P2PTradeRouter {
	
	private static final String TAG = "P2PTradeRouter";
	
	private P2PTradeRouter() {}
	
	public interface Callback {
		void onSuccess(JSONObject result);
		void onError(String error);
	}
	
	// ── Create listing ──────────────────────────────────────────────────────
	// Canonical (P2PListingCreateRequest.getCanonicalMessage()) - node format, byte for byte:
	//   "P2P_CREATE_LISTING:" + poster + ":" + side + ":" + totalAmount + ":" + minOrderAmount + ":" + maxOrderAmount
	//   + ":" + durationHours + ":" + paymentWindowMinutes + ":" + unitPricePerHarvRaw + ":" + nonce
	// unitPricePerHarvRaw is the seller's price in KOBO per 1 HARV (1 Naira = 100 kobo).
	// The request nonce is the wallet's nextRequestNonce.
	
	public static void createListing(Context context, String posterAddress, String side, long totalAmountRaw,
	long minOrderAmountRaw, long maxOrderAmountRaw, long durationHours, long paymentWindowMinutes,
	long unitPricePerHarvRaw, Callback callback) {
		HarvanaNodeClient.getWallet(posterAddress, new HarvanaNodeClient.JsonCallback() {
			@Override
			public void onSuccess(JSONObject walletJson) {
				long nonce = walletJson.optLong("nextRequestNonce", walletJson.optLong("nonce", 0) + 1);
				String canonical = com.harvana.app.chain.P2pCanonical.createListing(posterAddress, side, totalAmountRaw,
				minOrderAmountRaw, maxOrderAmountRaw, durationHours, paymentWindowMinutes, unitPricePerHarvRaw, nonce);
				
				_signAndSubmit(context, posterAddress, canonical, callback, (sigB64, pubKeyB64, cb) ->
				HarvanaNodeClient.submitP2PCreateListing(posterAddress, side, totalAmountRaw, minOrderAmountRaw,
				maxOrderAmountRaw, durationHours, paymentWindowMinutes, unitPricePerHarvRaw,
				nonce, sigB64, pubKeyB64,
				_wrap(cb)));
			}
			
			@Override
			public void onError(String error) {
				callback.onError("Could not fetch current nonce: " + error);
			}
		});
	}
	
	// Canonical (P2PClaimPaymentRequest.getCanonicalMessage()):
	//   "P2P_CLAIM_PAYMENT:" + listingId + ":" + buyerAddress + ":" + amount + ":" + nonce
	
	public static void claimPayment(Context context, String listingId, String buyerAddress, long amountRaw, Callback callback0) {
		final Callback callback = _paying("Escrow funded", amountRaw, buyerAddress, callback0);
		HarvanaNodeClient.getWallet(buyerAddress, new HarvanaNodeClient.JsonCallback() {
			@Override
			public void onSuccess(JSONObject walletJson) {
				long nonce = walletJson.optLong("nextRequestNonce", walletJson.optLong("nonce", 0) + 1);
				String canonical = "P2P_CLAIM_PAYMENT:" + listingId + ":" + buyerAddress + ":" + amountRaw + ":" + nonce;
				
				_signAndSubmit(context, buyerAddress, canonical, callback, (sigB64, pubKeyB64, cb) ->
				HarvanaNodeClient.submitP2PClaimPayment(listingId, buyerAddress, amountRaw, nonce, sigB64, pubKeyB64,
				_wrap(cb)));
			}
			
			@Override
			public void onError(String error) {
				callback.onError("Could not fetch current nonce: " + error);
			}
		});
	}
	
	// ── Confirm received (seller releases escrow to buyer, for one Order) ───
	// Canonical (P2PConfirmReceivedRequest.getCanonicalMessage()):
	//   "P2P_CONFIRM_RECEIVED:" + orderId + ":" + sellerAddress + ":" + nonce
	
	public static void markOrderPaid(Context context, String orderId, String buyerAddress, Callback callback) {
		HarvanaNodeClient.getWallet(buyerAddress, new HarvanaNodeClient.JsonCallback() {
			@Override
			public void onSuccess(JSONObject walletJson) {
				long nonce = walletJson.optLong("nextRequestNonce", walletJson.optLong("nonce", 0) + 1);
				String canonical = "P2P_MARK_PAID:" + orderId + ":" + buyerAddress + ":" + nonce;
				
				_signAndSubmit(context, buyerAddress, canonical, callback, (sigB64, pubKeyB64, cb) ->
				HarvanaNodeClient.submitP2PMarkPaid(orderId, buyerAddress, nonce, sigB64, pubKeyB64,
				_wrap(cb)));
			}
			
			@Override
			public void onError(String error) {
				callback.onError("Could not fetch current nonce: " + error);
			}
		});
	}
	
	public static void confirmReceived(Context context, String orderId, String sellerAddress, Callback callback) {
		HarvanaNodeClient.getWallet(sellerAddress, new HarvanaNodeClient.JsonCallback() {
			@Override
			public void onSuccess(JSONObject walletJson) {
				long nonce = walletJson.optLong("nextRequestNonce", walletJson.optLong("nonce", 0) + 1);
				String canonical = "P2P_CONFIRM_RECEIVED:" + orderId + ":" + sellerAddress + ":" + nonce;
				
				_signAndSubmit(context, sellerAddress, canonical, callback, (sigB64, pubKeyB64, cb) ->
				HarvanaNodeClient.submitP2PConfirmReceived(orderId, sellerAddress, nonce, sigB64, pubKeyB64,
				_wrap(cb)));
			}
			
			@Override
			public void onError(String error) {
				callback.onError("Could not fetch current nonce: " + error);
			}
		});
	}
	
	// ── Cancel listing (seller, only while still OPEN — refunds only what's
	// still unclaimed; any already-claimed Orders are unaffected) ───────────
	// Canonical (P2PCancelListingRequest.getCanonicalMessage()):
	//   "P2P_CANCEL_LISTING:" + listingId + ":" + sellerAddress + ":" + nonce
	
	public static void cancelListing(Context context, String listingId, String sellerAddress, Callback callback) {
		HarvanaNodeClient.getWallet(sellerAddress, new HarvanaNodeClient.JsonCallback() {
			@Override
			public void onSuccess(JSONObject walletJson) {
				long nonce = walletJson.optLong("nextRequestNonce", walletJson.optLong("nonce", 0) + 1);
				String canonical = "P2P_CANCEL_LISTING:" + listingId + ":" + sellerAddress + ":" + nonce;
				
				// No action is free: the fee transfer first (the wallet must hold HARV), then the cancellation.
				TransactionRouter.routeTransaction(context, sellerAddress, sellerAddress,
				new java.math.BigDecimal("0.00000001"), null, "ACTION_FEE:P2P_CANCEL_LISTING:" + nonce,
				new TransactionRouter.RouterCallback() {
					@Override public void onSuccess(String feeTxHash) {
						_signAndSubmit(context, sellerAddress, canonical, callback, (sigB64, pubKeyB64, cb) ->
						HarvanaNodeClient.submitP2PCancelListing(listingId, sellerAddress, nonce, sigB64, pubKeyB64,
						feeTxHash, _wrap(cb)));
					}
					@Override public void onError(String code, String msg) {
						callback.onError("Network fee not paid. Every action needs a small fee in HARV from your wallet. " + (msg == null ? "" : msg));
					}
				});
			}
			
			@Override
			public void onError(String error) {
				callback.onError("Could not fetch current nonce: " + error);
			}
		});
	}
	
	// ── Raise dispute (either party to one specific Order) ──────────────────
	// Canonical (P2PRaiseDisputeRequest.getCanonicalMessage()):
	//   "P2P_RAISE_DISPUTE:" + orderId + ":" + raiserAddress + ":" + safe(reason) + ":" + nonce
	//   where safe(reason) is "" if reason is null - matched exactly below,
	//   not by omitting the field, since the node's signature check
	//   concatenates an empty string, not a skipped segment.
	
	public static void raiseDispute(Context context, String orderId, String raiserAddress, String reason,
	Callback callback) {
		HarvanaNodeClient.getWallet(raiserAddress, new HarvanaNodeClient.JsonCallback() {
			@Override
			public void onSuccess(JSONObject walletJson) {
				long nonce = walletJson.optLong("nextRequestNonce", walletJson.optLong("nonce", 0) + 1);
				String safeReason = reason != null ? reason : "";
				String canonical = "P2P_RAISE_DISPUTE:" + orderId + ":" + raiserAddress + ":" + safeReason + ":" + nonce;
				
				_signAndSubmit(context, raiserAddress, canonical, callback, (sigB64, pubKeyB64, cb) ->
				HarvanaNodeClient.submitP2PRaiseDispute(orderId, raiserAddress, safeReason, nonce, sigB64, pubKeyB64,
				_wrap(cb)));
			}
			
			@Override
			public void onError(String error) {
				callback.onError("Could not fetch current nonce: " + error);
			}
		});
	}
	
	// ── List mine ────────────────────────────────────────────────────────
	// Canonical (P2PListMineRequest.getCanonicalMessage()):
	//   "P2P_LIST_MINE:" + address + ":" + timestamp
	// No nonce fetch needed here, unlike every action above — this is a
	// read, not a state change, so there's nothing to serialize against.
	// A fresh timestamp (checked server-side against a 5-minute window)
	// is what stops a captured request from being replayed indefinitely.
	
	public static void listMine(Context context, String address, Callback callback) {
		long timestamp = System.currentTimeMillis();
		String canonical = "P2P_LIST_MINE:" + address + ":" + timestamp;
		
		_signAndSubmit(context, address, canonical, callback, (sigB64, pubKeyB64, cb) ->
		HarvanaNodeClient.submitP2PListMine(address, timestamp, sigB64, pubKeyB64, _wrap(cb)));
	}
	
	// ── Insured Trader deposit ────────────────────────────────────────────
	// Canonical (P2PInsuranceDepositRequest.getCanonicalMessage()):
	//   "P2P_INSURANCE_DEPOSIT:" + traderAddress + ":" + amount + ":" + nonce
	
	public static void depositInsurance(Context context, String traderAddress, long amountRaw, Callback callback0) {
		final Callback callback = _paying("Insurance deposit sent", amountRaw, traderAddress, callback0);
		HarvanaNodeClient.getWallet(traderAddress, new HarvanaNodeClient.JsonCallback() {
			@Override
			public void onSuccess(JSONObject walletJson) {
				long nonce = walletJson.optLong("nextRequestNonce", walletJson.optLong("nonce", 0) + 1);
				String canonical = "P2P_INSURANCE_DEPOSIT:" + traderAddress + ":" + amountRaw + ":" + nonce;
				
				_signAndSubmit(context, traderAddress, canonical, callback, (sigB64, pubKeyB64, cb) ->
				HarvanaNodeClient.submitP2PInsuranceDeposit(traderAddress, amountRaw, nonce, sigB64, pubKeyB64,
				_wrap(cb)));
			}
			
			@Override
			public void onError(String error) {
				callback.onError("Could not fetch current nonce: " + error);
			}
		});
	}
	
	/** Locks the payment buttons while this runs and shows the success sheet when it works. */
	private static Callback _paying(String title, long amountRaw, String from, Callback inner) {
		final PaymentTracker.Handle handle = PaymentTracker.begin();
		return new Callback() {
			@Override public void onSuccess(JSONObject result) {
				try {
					com.harvana.app.ui.PaymentSuccessSheet.Info info = new com.harvana.app.ui.PaymentSuccessSheet.Info();
					info.title = title;
					info.amount = com.harvana.app.wallet.BalanceFormatter.smart(
					com.harvana.app.wallet.BalanceFormatter.rawToHarv(amountRaw)) + " " + com.harvana.app.CoinInfo.ticker();
					info.rows = new String[][] {{"From", from}, {"For", "Peer-to-peer trade"}};
					info.note = "The network has accepted it.";
					PaymentTracker.success(info);
				} catch (Throwable ignored) {}
				handle.end();
				inner.onSuccess(result);
			}
			@Override public void onError(String error) {
				handle.end();
				inner.onError(error);
			}
		};
	}
	
	// ── Shared signing plumbing ──────────────────────────────────────────────
	
	private interface SubmitFn {
		void submit(String signatureB64, String publicKeyB64, Callback callback);
	}
	
	private static void _signAndSubmit(Context context, String address, String canonicalMessage,
	Callback callback, SubmitFn submitFn) {
		try {
			String seedB64 = WalletManager.loadPrivateKey(context, address);
			if (seedB64 == null) {
				callback.onError("No local private key for " + address);
				return;
			}
			byte[] seed = Base64.decode(seedB64, Base64.NO_WRAP);
			
			byte[] sig = Ed25519.sign(seed, canonicalMessage.getBytes(StandardCharsets.UTF_8));
			String signatureB64 = Base64.encodeToString(sig, Base64.NO_WRAP);
			
			String publicKeyB64 = WalletManager.getPublicKey(context, address);
			if (publicKeyB64 == null) {
				callback.onError("No local public key for " + address);
				return;
			}
			
			submitFn.submit(signatureB64, publicKeyB64, callback);
		} catch (Exception e) {
			Log.e(TAG, "Signing failed: " + e.getMessage(), e);
			callback.onError("Signing failed: " + e.getMessage());
		}
	}
	
	private static HarvanaNodeClient.JsonCallback _wrap(Callback callback) {
		return new HarvanaNodeClient.JsonCallback() {
			@Override public void onSuccess(JSONObject result) { callback.onSuccess(result); }
			@Override public void onError(String error) { callback.onError(error); }
		};
	}
}
