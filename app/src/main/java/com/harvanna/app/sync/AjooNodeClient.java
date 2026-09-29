package com.harvanna.app.sync;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import com.harvanna.app.crypto.Ed25519;
import com.harvanna.app.wallet.SecureWalletStorage;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
* HTTP client for AJC Node — the source of truth for all AJC chain data.
*
* Runs over TLS to https://harvannaprotocol.com using the system trust store.
*
* UNIT CONVENTION:
*   The node stores and transmits balances, amounts and fees as raw long units.
*   1 AJC = AJC_UNITS_PER_TOKEN raw units.
*   Always convert at the UI boundary — never store raw units in display fields.
*/
public final class AjooNodeClient {
	
	public static final String BASE = "https://harvannaprotocol.com";
	
	/** 1 AJC = 100,000,000 raw units (same ratio as 1 BTC = 1e8 satoshis). */
	public static final long AJC_UNITS_PER_TOKEN = 100_000_000L;
	
	private static final String TAG = "AjooNode";
	
	// FIX 1: Use CachedThreadPool to avoid starvation if requests back up
	private static final ExecutorService IO = Executors.newCachedThreadPool();
	
	// FIX 2: Ensure all callbacks execute on the Main UI Thread
	private static final Handler mainHandler = new Handler(Looper.getMainLooper());
	
	public interface JsonCallback {
		void onSuccess(JSONObject json);
		void onError(String error);
	}
	
	private AjooNodeClient() {}
	
	// ── Wallet ───────────────────────────────────────────────────────────────
	
	public static void getWallet(String address, JsonCallback cb) {
		get("/api/v1/wallet/balance/" + address, cb);
	}
	
	public static void getHistory(String address, JsonCallback cb) {
		getHistory(address, 0, 20, cb);
	}
	
	/**
* Node's /api/v1/wallet/transactions/{address} defaults to page=0&size=20
* server-side (BlockchainManager.getTransactionHistory) - the no-arg
* overload above only ever gets the most recent 20 transactions, which
* is what made the wallet history list and the Airdrop screen's mined-
* rewards total both silently short. Callers that need more than one
* page should use this directly.
*/	
	public static void getHistory(String address, int page, int size, JsonCallback cb) {
		get("/api/v1/wallet/transactions/" + address + "?page=" + page + "&size=" + size, cb);
	}
	
	// ── Transactions ─────────────────────────────────────────────────────────
	
	public static void getFee(String from, String to, long rawAmount, JsonCallback cb) {
		get("/api/v1/tx/estimate-fee?from=" + from + "&to=" + to + "&amount=" + rawAmount, cb);
	}
	
	public static void submitSignedTx(String from, String to, long rawAmount, long estimatedGas,
	long nonce, String signatureB64, String publicKeyB64,
	JsonCallback cb) {
		submitSignedTx(from, to, rawAmount, estimatedGas, nonce, signatureB64, publicKeyB64, null, cb);
	}
	
	public static void submitSignedTx(String from, String to, long rawAmount, long estimatedGas,
	long nonce, String signatureB64, String publicKeyB64,
	String memo, JsonCallback cb) {
		try {
			JSONObject body = new JSONObject();
			body.put("from",      from);
			body.put("to",        to);
			body.put("amount",    rawAmount);
			body.put("gas",       estimatedGas);
			body.put("nonce",     nonce);
			body.put("signature", signatureB64);
			body.put("publicKey", publicKeyB64);
			if (memo != null && !memo.isEmpty()) {
				body.put("memo", memo);
			}
			post("/api/v1/tx/send", body.toString(), cb);
		} catch (Exception e) {
			String errorMsg = e.getMessage() != null ? e.getMessage() : "build body failed";
			mainHandler.post(() -> cb.onError(errorMsg));
		}
	}
	
	public static void getTxStatus(String txHash, JsonCallback cb) {
		get("/api/v1/tx/status/" + txHash, cb);
	}
	
	public static void getPending(String address, JsonCallback cb) {
		get("/api/v1/wallet/pending/" + address, cb);
	}
	
	// ── Chain ────────────────────────────────────────────────────────────────
	
	public static void getTip(JsonCallback cb) {
		get("/api/v1/blocks/latest", cb);
	}
	
	public static void getNodeStatus(JsonCallback cb) {
		get("/api/v1/node/status", cb);
	}
	
	/** Returns circulatingSupplyRaw + holderCount, computed directly from
*  every wallet's real balance on the node. Used for CoinDetailActivity's
*  "Total Holders" row, which previously had nothing calling this at
*  all — hardcoded to "Not available" and never updated. */	
	public static void getSupply(JsonCallback cb) {
		get("/api/v1/supply", cb);
	}
	
	// ── P2P Market ───────────────────────────────────────────────────────────
	// Pure network I/O, same division as submitSignedTx above — signing
	// itself lives in P2PTradeRouter, mirroring TransactionRouter's split.
	
	public static void getP2POpenListings(int page, int size, JsonCallback cb) {
		get("/api/v1/p2p/listings/open?page=" + page + "&size=" + size, cb);
	}
	
	public static void getP2POpenListings(int page, int size, String side, JsonCallback cb) {
		get("/api/v1/p2p/listings/open?page=" + page + "&size=" + size + "&side=" + side, cb);
	}
	
	public static void getP2PListing(String listingId, JsonCallback cb) {
		get("/api/v1/p2p/listing/" + listingId, cb);
	}
	
	// New — orders are now their own fetchable record, separate from the
	// listing they were claimed against.
	public static void getP2POrder(String orderId, JsonCallback cb) {
		get("/api/v1/p2p/order/" + orderId, cb);
	}
	
	// ── Guardian console only — not called from any normal wallet flow ──
	
	public static void getDisputedOrders(JsonCallback cb) {
		get("/api/v1/p2p/disputed-orders", cb);
	}
	
	public static void getDisputeStatus(String orderId, JsonCallback cb) {
		get("/api/v1/p2p/dispute-status/" + orderId, cb);
	}
	
	public static void submitGuardianSignature(String orderId, String resolution, long actionNonce,
	String publicKeyB64, String signatureB64, JsonCallback cb) {
		try {
			JSONObject body = new JSONObject();
			body.put("orderId", orderId);
			body.put("resolution", resolution);
			body.put("actionNonce", actionNonce);
			body.put("publicKey", publicKeyB64);
			body.put("signature", signatureB64);
			post("/api/v1/p2p/submit-guardian-signature", body.toString(), cb);
		} catch (Exception e) {
			String errorMsg = e.getMessage() != null ? e.getMessage() : "build body failed";
			mainHandler.post(() -> cb.onError(errorMsg));
		}
	}
	
	/** Read-only preview — same numbers a real claim would lock in
*  (platform fee, real byte-based gas, final net), computed fresh
*  each call so it reflects current network congestion. Safe to call
*  on every amount change while a buyer is still deciding. */	
	/** Read-only, no listingId needed — lets a Create Buy/Sell Offer
	*  screen show the maker fee it will eventually owe once matched,
	*  before any listing (and therefore any escrow) exists. Never
	*  computed locally in the app — the node is the only source of
	*  truth for this percentage and always will be, so a future rate
	*  change here needs no app update. */
	/** Read-only — no signature needed, moves nothing. Combine client-side
	*  with TrustScoreEngine's verified/tier fields for the full Insured
	*  Trader badge; this call alone only tells you the real, node-verified
	*  deposit side of that badge. */
	public static void getP2PInsuranceStatus(String address, JsonCallback cb) {
		try {
			String url = "/api/v1/p2p/insurance-status?address=" + java.net.URLEncoder.encode(address, "UTF-8");
			get(url, cb);
		} catch (Exception e) {
			String errorMsg = e.getMessage() != null ? e.getMessage() : "build request failed";
			mainHandler.post(() -> cb.onError(errorMsg));
		}
	}

	public static void submitP2PInsuranceDeposit(String traderAddress, long amount, long nonce,
	String signatureB64, String publicKeyB64, JsonCallback cb) {
		try {
			JSONObject body = new JSONObject();
			body.put("traderAddress", traderAddress);
			body.put("amount",        amount);
			body.put("nonce",         nonce);
			body.put("signature",     signatureB64);
			body.put("publicKey",     publicKeyB64);
			post("/api/v1/p2p/deposit-insurance", body.toString(), cb);
		} catch (Exception e) {
			String errorMsg = e.getMessage() != null ? e.getMessage() : "build body failed";
			mainHandler.post(() -> cb.onError(errorMsg));
		}
	}
	
	public static void previewP2PListingFee(String side, long amount, JsonCallback cb) {
		try {
			String url = "/api/v1/p2p/preview-listing-fee?side=" + java.net.URLEncoder.encode(side, "UTF-8")
			+ "&amount=" + amount;
			get(url, cb);
		} catch (Exception e) {
			String errorMsg = e.getMessage() != null ? e.getMessage() : "build request failed";
			mainHandler.post(() -> cb.onError(errorMsg));
		}
	}

	public static void previewP2PClaim(String listingId, String buyerAddress, long amount, JsonCallback cb) {
		try {
			String url = "/api/v1/p2p/preview-claim?listingId=" + java.net.URLEncoder.encode(listingId, "UTF-8")
			+ "&buyerAddress=" + java.net.URLEncoder.encode(buyerAddress, "UTF-8")
			+ "&amount=" + amount;
			get(url, cb);
		} catch (Exception e) {
			String errorMsg = e.getMessage() != null ? e.getMessage() : "build request failed";
			mainHandler.post(() -> cb.onError(errorMsg));
		}
	}
	
	public static void submitP2PCreateListing(String posterAddress, String side, long totalAmount, long minOrderAmount,
	long maxOrderAmount, long durationHours, long paymentWindowMinutes, long unitPricePerHarvRaw,
	long nonce, String signatureB64, String publicKeyB64,
	JsonCallback cb) {
		try {
			JSONObject body = new JSONObject();
			body.put("posterAddress",   posterAddress);
			body.put("side",            side);
			body.put("totalAmount",     totalAmount);
			body.put("minOrderAmount",  minOrderAmount);
			body.put("maxOrderAmount",  maxOrderAmount);
			body.put("durationHours",   durationHours);
			body.put("paymentWindowMinutes", paymentWindowMinutes);
			body.put("unitPricePerHarvRaw",  unitPricePerHarvRaw);
			body.put("nonce",           nonce);
			body.put("signature",       signatureB64);
			body.put("publicKey",       publicKeyB64);
			post("/api/v1/p2p/create-listing", body.toString(), cb);
		} catch (Exception e) {
			String errorMsg = e.getMessage() != null ? e.getMessage() : "build body failed";
			mainHandler.post(() -> cb.onError(errorMsg));
		}
	}
	
	// THE REDESIGN: claiming now takes a specific amount out of the
	// listing's remaining pool, rather than always taking the whole thing.
	public static void submitP2PClaimPayment(String listingId, String buyerAddress, long amount, long nonce,
	String signatureB64, String publicKeyB64, JsonCallback cb) {
		try {
			JSONObject body = new JSONObject();
			body.put("listingId",    listingId);
			body.put("buyerAddress", buyerAddress);
			body.put("amount",       amount);
			body.put("nonce",        nonce);
			body.put("signature",    signatureB64);
			body.put("publicKey",    publicKeyB64);
			post("/api/v1/p2p/claim-payment", body.toString(), cb);
		} catch (Exception e) {
			String errorMsg = e.getMessage() != null ? e.getMessage() : "build body failed";
			mainHandler.post(() -> cb.onError(errorMsg));
		}
	}
	
	// Renamed listingId -> orderId to match the node's model — confirming
	// now targets a specific Order, not the listing it came from.
	public static void submitP2PMarkPaid(String orderId, String buyerAddress, long nonce,
	String signatureB64, String publicKeyB64, JsonCallback cb) {
		try {
			JSONObject body = new JSONObject();
			body.put("orderId",      orderId);
			body.put("buyerAddress", buyerAddress);
			body.put("nonce",        nonce);
			body.put("signature",    signatureB64);
			body.put("publicKey",    publicKeyB64);
			post("/api/v1/p2p/mark-paid", body.toString(), cb);
		} catch (Exception e) {
			String errorMsg = e.getMessage() != null ? e.getMessage() : "build body failed";
			mainHandler.post(() -> cb.onError(errorMsg));
		}
	}
	
	public static void submitP2PConfirmReceived(String orderId, String sellerAddress, long nonce,
	String signatureB64, String publicKeyB64, JsonCallback cb) {
		try {
			JSONObject body = new JSONObject();
			body.put("orderId",       orderId);
			body.put("sellerAddress", sellerAddress);
			body.put("nonce",         nonce);
			body.put("signature",     signatureB64);
			body.put("publicKey",     publicKeyB64);
			post("/api/v1/p2p/confirm-received", body.toString(), cb);
		} catch (Exception e) {
			String errorMsg = e.getMessage() != null ? e.getMessage() : "build body failed";
			mainHandler.post(() -> cb.onError(errorMsg));
		}
	}
	
	public static void submitP2PCancelListing(String listingId, String sellerAddress, long nonce,
	String signatureB64, String publicKeyB64, JsonCallback cb) {
		try {
			JSONObject body = new JSONObject();
			body.put("listingId",     listingId);
			body.put("sellerAddress", sellerAddress);
			body.put("nonce",         nonce);
			body.put("signature",     signatureB64);
			body.put("publicKey",     publicKeyB64);
			post("/api/v1/p2p/cancel-listing", body.toString(), cb);
		} catch (Exception e) {
			String errorMsg = e.getMessage() != null ? e.getMessage() : "build body failed";
			mainHandler.post(() -> cb.onError(errorMsg));
		}
	}
	
	// Renamed listingId -> orderId to match the node's model — a dispute
	// is now specific to one buyer's Order, not the whole listing.
	public static void submitP2PRaiseDispute(String orderId, String raiserAddress, String reason, long nonce,
	String signatureB64, String publicKeyB64, JsonCallback cb) {
		try {
			JSONObject body = new JSONObject();
			body.put("orderId",       orderId);
			body.put("raiserAddress", raiserAddress);
			body.put("reason",        reason != null ? reason : "");
			body.put("nonce",         nonce);
			body.put("signature",     signatureB64);
			body.put("publicKey",     publicKeyB64);
			post("/api/v1/p2p/raise-dispute", body.toString(), cb);
		} catch (Exception e) {
			String errorMsg = e.getMessage() != null ? e.getMessage() : "build body failed";
			mainHandler.post(() -> cb.onError(errorMsg));
		}
	}
	
	public static void submitP2PListMine(String address, long timestamp, String signatureB64, String publicKeyB64,
	JsonCallback cb) {
		try {
			JSONObject body = new JSONObject();
			body.put("address",   address);
			body.put("timestamp", timestamp);
			body.put("signature", signatureB64);
			body.put("publicKey", publicKeyB64);
			post("/api/v1/p2p/list-mine", body.toString(), cb);
		} catch (Exception e) {
			String errorMsg = e.getMessage() != null ? e.getMessage() : "build body failed";
			mainHandler.post(() -> cb.onError(errorMsg));
		}
	}
	
	// ── Conversion helpers ────────────────────────────────────────────────────
	
	public static long toRawUnits(double ajcAmount) {
		return Math.round(ajcAmount * AJC_UNITS_PER_TOKEN);
	}
	
	public static double toAjcDisplay(long rawUnits) {
		return rawUnits / (double) AJC_UNITS_PER_TOKEN;
	}
	
	public static String buildCanonicalMessage(String from, String to, long rawAmount, long nonce) {
		return buildCanonicalMessage(from, to, rawAmount, nonce, null);
	}
	
	/** Must match the node's TransactionPayload.getCanonicalMessage() exactly:
*  from + to + amount + nonce + memo + mintId. Native transfers have no
*  mintId, so it's always appended as an empty string here - a
*  mismatched formula here would make the node's recomputed canonical
*  message different from what was actually signed, failing signature
*  verification for every memo-bearing transaction. */	
	public static String buildCanonicalMessage(String from, String to, long rawAmount, long nonce, String memo) {
		String safeMemo = memo != null ? memo : "";
		return from + to + rawAmount + nonce + safeMemo;
	}
	
	// ── Internal HTTP helpers ─────────────────────────────────────────────────
	
	/** Every wallet/tx/P2P endpoint call in this file goes through here —
*  a valid node auth token is fetched first (from cache, or freshly
*  requested if missing/expired) and attached as a Bearer header
*  before the actual request goes out. Nothing above this point in
*  the file needs to know auth exists at all. */	
	/** Authenticated GET for callers outside this class (group engines etc.). */
	public static void nodeGet(String path, JsonCallback cb) { get(path, cb); }
	
	/** Authenticated POST for callers outside this class (group engines etc.). */
	public static void nodePost(String path, String json, JsonCallback cb) { post(path, json, cb); }
	
	private static void get(String path, JsonCallback cb) {
		NodeAuthManager.getValidToken(new NodeAuthManager.TokenCallback() {
			@Override
			public void onToken(String token) {
				rawGet(path, token, cb);
			}
			
			@Override
			public void onError(String error) {
				mainHandler.post(() -> cb.onError("Node authentication failed: " + error));
			}
		});
	}
	
	private static void post(String path, String json, JsonCallback cb) {
		NodeAuthManager.getValidToken(new NodeAuthManager.TokenCallback() {
			@Override
			public void onToken(String token) {
				rawPost(path, json, token, cb);
			}
			
			@Override
			public void onError(String error) {
				mainHandler.post(() -> cb.onError("Node authentication failed: " + error));
			}
		});
	}
	
	/** The auth endpoint itself — deliberately bypasses get()/post() above
*  so requesting a token never depends on already having one. Not
*  private: NodeAuthManager (a different class, same package) calls
*  this directly.
*
*  Includes "app": "ajoo-app" so the node can distinguish this app's
*  traffic from com.ajoo.node.miner's, which hits the exact same
*  endpoint. Without this, a device running both apps on the same
*  IP/address has no way to avoid sharing one rate-limit budget
*  between mining traffic and ordinary wallet-app usage — this field
*  only helps once the node itself is updated to key its rate limits
*  on it; sending it alone doesn't change node-side behavior. */	
	static void requestNodeToken(String address, String clientId, JsonCallback cb) {
		requestNodeToken(null, address, clientId, cb);
	}
	
	static void requestNodeToken(Context context, String address, String clientId, JsonCallback cb) {
		if (context != null && address != null && !address.isEmpty()) {
			requestSignedNodeToken(context, address, clientId, cb);
			return;
		}
		postUnsignedNodeToken(address, clientId, cb);
	}
	
	private static void requestSignedNodeToken(Context context, String address, String clientId, JsonCallback cb) {
		try {
			JSONObject ch = new JSONObject();
			ch.put("address", address);
			ch.put("clientId", clientId != null ? clientId : "");
			ch.put("app", "ajoo-app");
			rawPost("/api/v1/mining/challenge", ch.toString(), null, new JsonCallback() {
				@Override
				public void onSuccess(JSONObject json) {
					try {
						String challenge = json.optString("challenge", "");
						String canonical = json.optString("canonical", "");
						if (challenge.isEmpty()) {
							postUnsignedNodeToken(address, clientId, cb);
							return;
						}
						if (canonical.isEmpty()) {
							canonical = "HARVANNA_AUTH|" + address + "|" + challenge + "|"
							+ (clientId == null ? "" : clientId) + "|ajoo-app";
						}
						String pkB64 = SecureWalletStorage.loadPrivateKey(context, address);
						String pubB64 = SecureWalletStorage.loadPublicKey(context, address);
						if (pkB64 == null || pubB64 == null) {
							postUnsignedNodeToken(address, clientId, cb);
							return;
						}
						byte[] seed = Base64.decode(pkB64, Base64.NO_WRAP);
						byte[] sig = Ed25519.sign(seed, canonical.getBytes(StandardCharsets.UTF_8));
						JSONObject auth = new JSONObject();
						auth.put("address", address);
						auth.put("clientId", clientId != null ? clientId : "");
						auth.put("app", "ajoo-app");
						auth.put("challenge", challenge);
						auth.put("publicKey", pubB64);
						auth.put("signature", Base64.encodeToString(sig, Base64.NO_WRAP));
						rawPost("/api/v1/mining/auth", auth.toString(), null, cb);
					} catch (Exception e) {
						Log.w(TAG, "signed node auth failed, falling back", e);
						postUnsignedNodeToken(address, clientId, cb);
					}
				}
				
				@Override
				public void onError(String error) {
					postUnsignedNodeToken(address, clientId, cb);
				}
			});
		} catch (Exception e) {
			postUnsignedNodeToken(address, clientId, cb);
		}
	}
	
	private static void postUnsignedNodeToken(String address, String clientId, JsonCallback cb) {
		try {
			JSONObject body = new JSONObject();
			body.put("address", address);
			body.put("clientId", clientId != null ? clientId : "");
			body.put("app", "ajoo-app");
			rawPost("/api/v1/mining/auth", body.toString(), null, cb);
		} catch (Exception e) {
			String errorMsg = e.getMessage() != null ? e.getMessage() : "build auth request failed";
			mainHandler.post(() -> cb.onError(errorMsg));
		}
	}
	
	/** bearerToken may be null — used only by requestNodeToken() above,
*  since a request for a token obviously can't itself require one. */	
	private static void rawGet(String path, String bearerToken, JsonCallback cb) {
		IO.execute(() -> {
			HttpURLConnection c = null;
			try {
				c = (HttpURLConnection) new URL(BASE + path).openConnection();
				c.setRequestMethod("GET");
				c.setConnectTimeout(15_000);
				c.setReadTimeout(20_000);
				if (bearerToken != null && !bearerToken.isEmpty()) {
					c.setRequestProperty("Authorization", "Bearer " + bearerToken);
				}
				int code = c.getResponseCode();
				String body = read(c, code);
				
				if (code == 401 || code == 403) {
					NodeAuthManager.invalidateCachedToken();
				}
				
				if (code >= 200 && code < 300) {
					// FIX 3: Catch JSON parsing errors safely
					try {
						JSONObject json = new JSONObject(body);
						mainHandler.post(() -> cb.onSuccess(json));
					} catch (JSONException je) {
						mainHandler.post(() -> cb.onError("Invalid JSON format from server"));
					}
				} else {
					mainHandler.post(() -> cb.onError("HTTP " + code + ": " + body));
				}
			} catch (Exception e) {
				Log.e(TAG, "GET " + path, e);
				final String errorMsg = e.getMessage() != null ? e.getMessage() : "network error";
				mainHandler.post(() -> cb.onError(errorMsg));
			} finally {
				if (c != null) c.disconnect();
			}
		});
	}
	
	/** bearerToken may be null — used only by requestNodeToken() above. */
	private static void rawPost(String path, String json, String bearerToken, JsonCallback cb) {
		IO.execute(() -> {
			HttpURLConnection c = null;
			try {
				c = (HttpURLConnection) new URL(BASE + path).openConnection();
				c.setRequestMethod("POST");
				c.setConnectTimeout(15_000);
				c.setReadTimeout(20_000);
				c.setDoOutput(true);
				c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
				if (bearerToken != null && !bearerToken.isEmpty()) {
					c.setRequestProperty("Authorization", "Bearer " + bearerToken);
				}
				byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
				c.setFixedLengthStreamingMode(bytes.length);
				try (OutputStream os = c.getOutputStream()) { os.write(bytes); }
				int code = c.getResponseCode();
				String body = read(c, code);
				
				if (code == 401 || code == 403) {
					NodeAuthManager.invalidateCachedToken();
				}
				
				if (code >= 200 && code < 300) {
					// FIX 3: Catch JSON parsing errors safely
					try {
						JSONObject responseJson = new JSONObject(body);
						mainHandler.post(() -> cb.onSuccess(responseJson));
					} catch (JSONException je) {
						mainHandler.post(() -> cb.onError("Invalid JSON format from server"));
					}
				} else {
					mainHandler.post(() -> cb.onError("HTTP " + code + ": " + body));
				}
			} catch (Exception e) {
				Log.e(TAG, "POST " + path, e);
				final String errorMsg = e.getMessage() != null ? e.getMessage() : "network error";
				mainHandler.post(() -> cb.onError(errorMsg));
			} finally {
				if (c != null) c.disconnect();
			}
		});
	}
	
	private static String read(HttpURLConnection c, int code) throws Exception {
		java.io.InputStream in = (code >= 200 && code < 300)
		? c.getInputStream() : c.getErrorStream();
		if (in == null) return "";
		BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
		StringBuilder sb = new StringBuilder();
		String line;
		while ((line = br.readLine()) != null) sb.append(line);
		br.close();
		return sb.toString();
	}
}
