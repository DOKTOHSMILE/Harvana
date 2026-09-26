package com.ajoo.app.firestore;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;

import okhttp3.*;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
* REST-based Firestore access for P2P_PAYMENT_DETAILS/{listingId} — same
* REST-not-SDK, OkHttp, Bearer-token pattern as UserProfileManager and
* GroupFirestoreManager, kept as its own class for the same reason those
* two are separate: a distinct collection with its own lifecycle.
*
* This collection exists specifically so bank/mobile-money details never
* touch the blockchain node at all — they used to be part of the signed
* P2P_CREATE_LISTING message and get persisted directly into the node's
* RocksDB listing (and denormalized into every order against it), which
* meant sensitive PII was living in blockchain-adjacent state alongside
* genuinely crypto-relevant data. The node's signed message and its own
* storage now carry only sellerAddress/amount/price/currency/nonce; this
* class is where the payment details actually live, written by the app
* right after the node confirms a listing was created, and read by
* whichever authenticated buyer claims that listing.
*/
public class P2PPaymentDetailsManager {
	
	private static final String TAG = "P2PPaymentDetailsManager";
	private static final String PROJECT_ID = "capital-oracle-project";
	private static final String BASE_URL = "https://firestore.googleapis.com/v1/projects/" + PROJECT_ID + "/databases/(default)/documents";
	private static final String COLLECTION = "P2P_PAYMENT_DETAILS";
	
	private static P2PPaymentDetailsManager instance;
	private final OkHttpClient client;
	
	public static synchronized P2PPaymentDetailsManager getInstance(Context context) {
		if (instance == null) {
			instance = new P2PPaymentDetailsManager();
		}
		return instance;
	}
	
	private P2PPaymentDetailsManager() {
		client = new OkHttpClient.Builder()
		.connectTimeout(15, TimeUnit.SECONDS)
		.readTimeout(15, TimeUnit.SECONDS)
		.writeTimeout(15, TimeUnit.SECONDS)
		.build();
	}
	
	public interface SimpleCallback {
		void onSuccess();
		void onError(String error);
	}
	
	public interface DocCallback {
		void onSuccess(Map<String, Object> data);
		void onError(String error);
	}
	
	/** Called right after P2PTradeRouter.createListing() returns a real
*  listingId — writes the payment details and pricing structure the 
*  listing's buyer will need, entirely outside anything the node sees. */	
	public void save(String listingId, java.util.List<com.ajoo.app.wallet.PaymentMethod> paymentMethods,
	String pricingType, double unitPrice, double marginPercent, int paymentWindowMinutes, String termsOfTrade, int reqAccountAgeDays, double maxOrderAjc, String currency, String sellerUid, SimpleCallback callback) {
		Map<String, Object> fields = new HashMap<>();
		fields.put("listingId", listingId);
		fields.put("pricingType", pricingType);
		fields.put("unitPrice", unitPrice);
		fields.put("marginPercent", marginPercent);
		fields.put("paymentWindowMinutes", paymentWindowMinutes);
		fields.put("termsOfTrade", termsOfTrade != null ? termsOfTrade : "");
		// reqKyc and reqCompletionRate are deliberately NOT here — this app
		// has no identity-verification system and no real trade-completion
		// tracking (only happy-path COMPLETED trades are recorded anywhere;
		// cancellations/disputes aren't), so both those filter rows are
		// left disabled in the UI rather than storing a requirement this
		// app cannot actually check. reqAccountAgeDays is real: it's
		// checked against Firebase Auth's own account-creation timestamp,
		// which needs no new tracking infrastructure at all.
		fields.put("reqAccountAgeDays", reqAccountAgeDays);
		// ⚠️ maxOrderAjc is DISPLAY-ONLY right now, not enforced. The
		// node's createListing signed message and claim logic (see
		// P2PTradeRouter.createListing) only ever carried a MIN order
		// amount — there is no max field anywhere in the node's listing
		// schema or claim-processing path. Saving it here lets the app
		// show a merchant's intended max on their own listing, but a
		// buyer can still claim up to the full remaining listing amount
		// today; real liquidity protection needs a node-side change
		// (new schema field + claim-time check), not just this.
		fields.put("maxOrderAjc", maxOrderAjc);
		fields.put("currency", currency);
		
		java.util.List<Map<String, Object>> methodMaps = new java.util.ArrayList<>();
		for (com.ajoo.app.wallet.PaymentMethod pm : paymentMethods) {
			Map<String, Object> m = new HashMap<>();
			m.put("type", pm.type.name());
			m.put("primaryField", pm.primaryField);
			m.put("accountIdentifier", pm.accountIdentifier);
			m.put("accountName", pm.accountName);
			methodMaps.add(m);
		}
		fields.put("paymentMethods", methodMaps);
		
		if (!paymentMethods.isEmpty()) {
			fields.put("accountName", paymentMethods.get(0).accountName);
		}
		fields.put("sellerUid", sellerUid);
		fields.put("createdAtMs", System.currentTimeMillis());
		
		_authorizedRequest("PATCH", "/" + COLLECTION + "/" + listingId, _encodeFields(fields), new _RawCallback() {
			@Override public void onSuccess(String body) { callback.onSuccess(); }
			@Override public void onError(String error) { callback.onError(error); }
		});
	}
	
	/** Called by the buyer's client once they've claimed a listing —
*  Firestore security rules are what should actually gate this (only
*  the matched buyer or the seller can read a given listingId's
*  document), replacing the old approach of the node deciding what to
*  redact based on listing status. */	
	public void get(String listingId, DocCallback callback) {
		_authorizedRequest("GET", "/" + COLLECTION + "/" + listingId, null, new _RawCallback() {
			@Override
			public void onSuccess(String body) {
				try {
					callback.onSuccess(_decodeDocument(new JSONObject(body)));
				} catch (Exception e) {
					callback.onError("Failed to parse payment details: " + e.getMessage());
				}
			}
			@Override public void onError(String error) { callback.onError(error); }
		});
	}
	
	/** Called right after P2PTradeRouter.claimPayment() succeeds. A
*  single listing can be partially claimed by several different
*  buyers — remainingAmount exists specifically so that's possible —
*  so this can't be a single scalar buyerUid field on the main
*  document; a second buyer's claim would silently overwrite the
*  first buyer's read access. Instead each claimer gets their own tiny
*  marker document in an authorizedBuyers subcollection, and the
*  security rule below checks for its existence rather than comparing
*  a single field. */	
	public void markBuyerClaimed(String listingId, String buyerUid, String orderId, SimpleCallback callback) {
		Map<String, Object> fields = new HashMap<>();
		fields.put("orderId", orderId);
		fields.put("claimedAtMs", System.currentTimeMillis());
		
		_authorizedRequest("PATCH", "/" + COLLECTION + "/" + listingId + "/authorizedBuyers/" + buyerUid,
		_encodeFields(fields), new _RawCallback() {
			@Override public void onSuccess(String body) { callback.onSuccess(); }
			@Override public void onError(String error) { callback.onError(error); }
		});
	}
	
	/** Resolves the buyer's Firestore uid for a specific order — the
*  piece that was missing for mutual visibility. authorizedBuyers
*  documents are keyed BY buyerUid (see markBuyerClaimed above), so
*  finding "the buyer for this order" means querying that
*  subcollection for orderId == X and reading the matching
*  document's own id, not a field inside it. This is what lets a
*  SELLER look up who they're trading with — previously only the
*  buyer's own device could resolve their own identity, via their
*  own FirebaseAuth session, with no path for the other party at all. */	
	public interface UidCallback { void onFound(String buyerUid); void onNotFound(); void onError(String error); }
	
	public void getBuyerUidForOrder(String listingId, String orderId, UidCallback callback) {
		try {
			JSONObject query = new JSONObject();
			JSONObject structuredQuery = new JSONObject();
			JSONArray from = new JSONArray();
			from.put(new JSONObject().put("collectionId", "authorizedBuyers"));
			structuredQuery.put("from", from);
			JSONObject fieldFilter = new JSONObject();
			fieldFilter.put("field", new JSONObject().put("fieldPath", "orderId"));
			fieldFilter.put("op", "EQUAL");
			fieldFilter.put("value", new JSONObject().put("stringValue", orderId));
			structuredQuery.put("where", new JSONObject().put("fieldFilter", fieldFilter));
			query.put("structuredQuery", structuredQuery);
			
			_authorizedRequest("POST", "/" + COLLECTION + "/" + listingId + ":runQuery", query, new _RawCallback() {
				@Override
				public void onSuccess(String body) {
					try {
						JSONArray results = new JSONArray(body);
						for (int i = 0; i < results.length(); i++) {
							JSONObject entry = results.getJSONObject(i);
							if (!entry.has("document")) continue;
							String name = entry.getJSONObject("document").optString("name", "");
							String buyerUid = name.substring(name.lastIndexOf('/') + 1);
							if (!buyerUid.isEmpty()) { callback.onFound(buyerUid); return; }
						}
						callback.onNotFound();
					} catch (Exception e) {
						callback.onError("Failed to parse buyer lookup: " + e.getMessage());
					}
				}
				@Override public void onError(String error) { callback.onError(error); }
			});
		} catch (Exception e) {
			callback.onError("Failed to build buyer lookup query: " + e.getMessage());
		}
	}
	
	// ── Order-level payment details (Sept 17 2026) ──────────────────────
	private static final String ORDER_COLLECTION = "P2P_ORDER_PAYMENT_DETAILS";
	
	public void saveOrderPaymentDetails(String orderId, com.ajoo.app.wallet.PaymentMethod method, String sellerUid, String buyerUid, SimpleCallback callback) {
		Map<String, Object> fields = new HashMap<>();
		fields.put("orderId", orderId);
		fields.put("sellerUid", sellerUid);
		fields.put("buyerUid", buyerUid);
		if (method != null) {
			fields.put("primaryField", method.primaryField);
			fields.put("accountIdentifier", method.accountIdentifier);
			fields.put("accountName", method.accountName);
		}
		fields.put("createdAtMs", System.currentTimeMillis());
		
		_authorizedRequest("PATCH", "/" + ORDER_COLLECTION + "/" + orderId, _encodeFields(fields), new _RawCallback() {
			@Override public void onSuccess(String body) { callback.onSuccess(); }
			@Override public void onError(String error) { callback.onError(error); }
		});
	}
	
	public void getOrderPaymentDetails(String orderId, DocCallback callback) {
		_authorizedRequest("GET", "/" + ORDER_COLLECTION + "/" + orderId, null, new _RawCallback() {
			@Override
			public void onSuccess(String body) {
				try {
					callback.onSuccess(_decodeDocument(new JSONObject(body)));
				} catch (Exception e) {
					callback.onError("Failed to parse order payment details: " + e.getMessage());
				}
			}
			@Override public void onError(String error) { callback.onError(error); }
		});
	}
	
	public static void bindAccount(Map data, android.widget.TextView bank, android.widget.TextView number, android.widget.TextView name) {
		if (bank == null || number == null || name == null) return;
		String primary = null, identifier = null, accountName = null;
		Object methodsObj = data != null ? data.get("paymentMethods") : null;
		if (methodsObj instanceof java.util.List && !((java.util.List<?>) methodsObj).isEmpty()) {
			Object first = ((java.util.List<?>) methodsObj).get(0);
			if (first instanceof Map) {
				Map<?, ?> m = (Map<?, ?>) first;
				if (m.get("primaryField") != null) primary = String.valueOf(m.get("primaryField"));
				if (m.get("accountIdentifier") != null) identifier = String.valueOf(m.get("accountIdentifier"));
				if (m.get("accountName") != null) accountName = String.valueOf(m.get("accountName"));
			}
		}
		if ((primary == null || primary.isEmpty()) && data != null && data.get("primaryField") != null) {
			primary = String.valueOf(data.get("primaryField"));
		}
		if ((identifier == null || identifier.isEmpty()) && data != null && data.get("accountIdentifier") != null) {
			identifier = String.valueOf(data.get("accountIdentifier"));
		}
		if ((accountName == null || accountName.isEmpty()) && data != null && data.get("accountName") != null) {
			accountName = String.valueOf(data.get("accountName"));
		}
		bank.setText(primary != null && !primary.isEmpty() ? primary : "—");
		number.setText(identifier != null && !identifier.isEmpty() ? identifier : "—");
		name.setText(accountName != null && !accountName.isEmpty() ? accountName : "—");
	}
	
	// ── HTTP + auth plumbing ──
	
	private interface _RawCallback {
		void onSuccess(String body);
		void onError(String error);
	}
	
	private void _authorizedRequest(String method, String path, JSONObject body, _RawCallback callback) {
		FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
		if (user == null) {
			callback.onError("Not signed in — no current Firebase user.");
			return;
		}
		user.getIdToken(false).addOnCompleteListener(new OnCompleteListener<GetTokenResult>() {
			@Override
			public void onComplete(Task<GetTokenResult> task) {
				if (task.isSuccessful() && task.getResult() != null) {
					_executeRequest(method, path, body, task.getResult().getToken(), callback, true);
				} else {
					String msg = task.getException() != null ? task.getException().getMessage() : "unknown error";
					callback.onError("Failed to obtain ID token: " + msg);
				}
			}
		});
	}
	
	private void _executeRequest(String method, String path, JSONObject body, String idToken, _RawCallback callback, boolean allowRetryOn401) {
		Request.Builder builder = new Request.Builder()
		.url(BASE_URL + path)
		.header("Authorization", "Bearer " + idToken);
		
		RequestBody requestBody = null;
		if (body != null) {
			requestBody = RequestBody.create(body.toString(), MediaType.parse("application/json"));
		}
		
		switch (method) {
			case "GET": builder.get(); break;
			case "PATCH": builder.patch(requestBody != null ? requestBody : RequestBody.create("", MediaType.parse("application/json"))); break;
			case "POST": builder.post(requestBody != null ? requestBody : RequestBody.create("{}", MediaType.parse("application/json"))); break;
			default: callback.onError("Unsupported HTTP method: " + method); return;
		}
		
		client.newCall(builder.build()).enqueue(new Callback() {
			@Override
			public void onFailure(Call call, IOException e) {
				callback.onError("Network error: " + e.getMessage());
			}
			
			@Override
			public void onResponse(Call call, Response response) throws IOException {
				String responseBody = response.body() != null ? response.body().string() : "";
				if (response.code() == 401 && allowRetryOn401) {
					FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
					if (user == null) {
						callback.onError("Not signed in — can't refresh token after 401.");
						return;
					}
					user.getIdToken(true).addOnCompleteListener(new OnCompleteListener<GetTokenResult>() {
						@Override
						public void onComplete(Task<GetTokenResult> task) {
							if (task.isSuccessful() && task.getResult() != null) {
								_executeRequest(method, path, body, task.getResult().getToken(), callback, false);
							} else {
								String msg = task.getException() != null ? task.getException().getMessage() : "unknown error";
								callback.onError("Token refresh failed after 401: " + msg);
							}
						}
					});
					return;
				}
				if (!response.isSuccessful()) {
					callback.onError("HTTP " + response.code() + ": " + responseBody);
					return;
				}
				callback.onSuccess(responseBody);
			}
		});
	}
	
	private JSONObject _encodeFields(Map<String, Object> fields) {
		try {
			JSONObject root = new JSONObject();
			JSONObject fieldsObj = new JSONObject();
			for (Map.Entry<String, Object> entry : fields.entrySet()) {
				fieldsObj.put(entry.getKey(), _encodeValue(entry.getValue()));
			}
			root.put("fields", fieldsObj);
			return root;
		} catch (Exception e) {
			Log.e(TAG, "Failed to encode fields: " + e.getMessage());
			return new JSONObject();
		}
	}
	
	private JSONObject _encodeValue(Object value) throws Exception {
		JSONObject wrapper = new JSONObject();
		if (value == null) {
			wrapper.put("nullValue", JSONObject.NULL);
		} else if (value instanceof String) {
			wrapper.put("stringValue", value);
		} else if (value instanceof Integer || value instanceof Long) {
			wrapper.put("integerValue", String.valueOf(value));
		} else if (value instanceof Double || value instanceof Float) {
			wrapper.put("doubleValue", value);
		} else if (value instanceof Boolean) {
			wrapper.put("booleanValue", value);
		} else if (value instanceof java.util.List) {
			JSONArray values = new JSONArray();
			for (Object item : (java.util.List<?>) value) {
				values.put(_encodeValue(item));
			}
			JSONObject arrayValue = new JSONObject();
			arrayValue.put("values", values);
			wrapper.put("arrayValue", arrayValue);
		} else if (value instanceof java.util.Map) {
			JSONObject mapFields = new JSONObject();
			for (Map.Entry<?, ?> entry : ((java.util.Map<?, ?>) value).entrySet()) {
				mapFields.put(String.valueOf(entry.getKey()), _encodeValue(entry.getValue()));
			}
			JSONObject mapValue = new JSONObject();
			mapValue.put("fields", mapFields);
			wrapper.put("mapValue", mapValue);
		} else {
			wrapper.put("stringValue", String.valueOf(value));
		}
		return wrapper;
	}
	
	private Map<String, Object> _decodeDocument(JSONObject doc) throws Exception {
		Map<String, Object> result = new HashMap<>();
		JSONObject fields = doc.optJSONObject("fields");
		if (fields == null) return result;
		java.util.Iterator<String> keys = fields.keys();
		while (keys.hasNext()) {
			String key = keys.next();
			result.put(key, _decodeValue(fields.getJSONObject(key)));
		}
		return result;
	}
	
	private Object _decodeValue(JSONObject wrapper) throws Exception {
		if (wrapper.has("stringValue")) return wrapper.getString("stringValue");
		if (wrapper.has("integerValue")) return Long.parseLong(wrapper.getString("integerValue"));
		if (wrapper.has("doubleValue")) return wrapper.getDouble("doubleValue");
		if (wrapper.has("booleanValue")) return wrapper.getBoolean("booleanValue");
		if (wrapper.has("nullValue")) return null;
		if (wrapper.has("arrayValue")) {
			java.util.List<Object> list = new java.util.ArrayList<>();
			JSONArray values = wrapper.getJSONObject("arrayValue").optJSONArray("values");
			if (values != null) {
				for (int i = 0; i < values.length(); i++) {
					list.add(_decodeValue(values.getJSONObject(i)));
				}
			}
			return list;
		}
		if (wrapper.has("mapValue")) {
			Map<String, Object> map = new HashMap<>();
			JSONObject mapFields = wrapper.getJSONObject("mapValue").optJSONObject("fields");
			if (mapFields != null) {
				java.util.Iterator<String> keys = mapFields.keys();
				while (keys.hasNext()) {
					String key = keys.next();
					map.put(key, _decodeValue(mapFields.getJSONObject(key)));
				}
			}
			return map;
		}
		return null;
	}
}
