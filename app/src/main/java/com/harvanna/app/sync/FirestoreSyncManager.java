package com.harvanna.app.sync;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.harvanna.app.AddressUtil;
import com.harvanna.app.AjooConfig;
import com.harvanna.app.ChainConstants;
import com.harvanna.app.ChainMath;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class FirestoreSyncManager {
	
	public static final String PROJECT_ID = "capital-oracle-project";
	public static final String API_KEY = AjooConfig.FIREBASE_API_KEY;
	public static final String BASE_URL =
	"https://firestore.googleapis.com/v1/projects/" + PROJECT_ID + "/databases/(default)/documents/";
	
	private static final Handler mainHandler = new Handler(Looper.getMainLooper());
	private static final FirestoreSyncManager INSTANCE = new FirestoreSyncManager();
	
	private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
	
	private static final OkHttpClient HTTP = new OkHttpClient.Builder()
	.connectTimeout(10, TimeUnit.SECONDS)
	.readTimeout(12, TimeUnit.SECONDS)
	.writeTimeout(12, TimeUnit.SECONDS)
	.retryOnConnectionFailure(true)
	.build();
	
	private FirestoreSyncManager() {}
	
	public static FirestoreSyncManager getInstance() {
		return INSTANCE;
	}
	
	public interface SyncCallback {
		void onSuccess(String result);
		void onError(String error);
	}
	
	public interface WalletCallback {
		void onSuccess(Map<String, Object> wallet);
		void onError(String error);
	}
	
	public interface CollectionCallback {
		void onSuccess(List<Map<String, Object>> documents);
		void onError(String error);
	}
	
	public static class BatchWrite {
		public final String collection, documentId;
		public final Map<String, Object> data;
		
		public BatchWrite(String collection, String documentId, Map<String, Object> data) {
			this.collection = collection;
			this.documentId = documentId;
			this.data = data;
		}
	}
	
	// ── Wallet ───────────────────────────────────────────────────────────────
	
	/**
	 * @deprecated Wallets live on the AJC node (RocksDB), not Firestore.
	 * This method no longer writes COL_WALLETS. It soft-checks the node and
	 * returns a local stub map so legacy callers do not break.
	 * Prefer AjooNodeClient.getWallet(address, …) directly.
	 */
	@Deprecated
	public void getOrCreateWallet(final String address, final WalletCallback callback) {
		if (callback == null) return;
		if (!AddressUtil.isValidAddress(address)) {
			callback.onError("Invalid wallet address");
			return;
		}
		final String normalized = address.trim();
		// Node is source of truth — never create a Firestore WALLETS doc.
		AjooNodeClient.getWallet(normalized, new AjooNodeClient.JsonCallback() {
			@Override
			public void onSuccess(org.json.JSONObject json) {
				Map<String, Object> wallet = new HashMap<>();
				wallet.put("address", normalized);
				wallet.put("balance_ajc", 0.0);
				wallet.put("nonce", 0L);
				wallet.put("source", "node");
				try {
					if (json != null) {
						if (json.has("balance")) wallet.put("balance_raw", json.optLong("balance", 0L));
						if (json.has("available")) wallet.put("available_raw", json.optLong("available", 0L));
						if (json.has("nonce")) wallet.put("nonce", json.optLong("nonce", 0L));
					}
				} catch (Exception ignored) {}
				callback.onSuccess(wallet);
			}

			@Override
			public void onError(String error) {
				// Unknown address on a live node is fine (balance 0 until first credit).
				String err = error != null ? error : "";
				boolean nodeDown = err.contains("network error")
						|| err.contains("failed to connect")
						|| err.contains("Connection refused")
						|| err.contains("Unable to resolve")
						|| err.contains("timeout")
						|| err.contains("ETIMEDOUT");
				if (nodeDown) {
					callback.onError((com.harvanna.app.CoinInfo.ticker() + " node unreachable: ") + error);
					return;
				}
				Map<String, Object> wallet = new HashMap<>();
				wallet.put("address", normalized);
				wallet.put("balance_ajc", 0.0);
				wallet.put("nonce", 0L);
				wallet.put("source", "node");
				callback.onSuccess(wallet);
			}
		});
	}
	
	// ── CRUD ─────────────────────────────────────────────────────────────────
	
	public static void getDocument(String collection, String documentId, SyncCallback callback) {
		if (callback == null) return;
		executeRequest("GET", BASE_URL + collection + "/" + documentId + "?key=" + API_KEY, null, callback);
	}
	
	public static void getCollection(String collectionPath, final CollectionCallback callback) {
		if (callback == null) return;
		executeRequest("GET", BASE_URL + collectionPath + "?key=" + API_KEY, null, new SyncCallback() {
			@Override
			public void onSuccess(String json) {
				List<Map<String, Object>> docs = new ArrayList<>();
				try {
					JSONObject root = new JSONObject(json);
					JSONArray arr = root.optJSONArray("documents");
					if (arr != null) {
						for (int i = 0; i < arr.length(); i++) {
							JSONObject d = arr.getJSONObject(i);
							Map<String, Object> m = parseFirestoreDocument(d.toString());
							if (m != null) {
								String name = d.optString("name", "");
								if (!name.isEmpty()) {
									m.put("_documentId", name.substring(name.lastIndexOf('/') + 1));
								}
								docs.add(m);
							}
						}
					}
				} catch (Exception e) {
					callback.onError("Failed to parse collection: " + e.getMessage());
					return;
				}
				callback.onSuccess(docs);
			}
			
			@Override
			public void onError(String e) {
				callback.onError(e);
			}
		});
	}
	
	public static void updateDocument(String collection, String documentId,
	Map<String, Object> data, SyncCallback callback) {
		executeRequest("PATCH",
		BASE_URL + collection + "/" + documentId + "?key=" + API_KEY,
		mapToFirestoreJson(data),
		noopIfNull(callback));
	}
	
	public static void patchFields(String collection, String documentId,
	Map<String, Object> fields, SyncCallback callback) {
		if (fields == null || fields.isEmpty()) {
			if (callback != null) callback.onSuccess("{}");
			return;
		}
		try {
			StringBuilder url = new StringBuilder(BASE_URL)
			.append(collection).append("/").append(documentId)
			.append("?key=").append(API_KEY);
			for (String key : fields.keySet()) {
				url.append("&updateMask.fieldPaths=")
				.append(java.net.URLEncoder.encode(key, "UTF-8"));
			}
			executeRequest("PATCH", url.toString(), mapToFirestoreJson(fields), noopIfNull(callback));
		} catch (Exception e) {
			if (callback != null) callback.onError(e.getMessage());
		}
	}
	
	public static void createDocument(String collection, String documentId,
	Map<String, Object> data, SyncCallback callback) {
		executeRequest("POST",
		BASE_URL + collection + "?documentId=" + documentId + "&key=" + API_KEY,
		mapToFirestoreJson(data),
		noopIfNull(callback));
	}
	
	public static void deleteDocument(String collection, String documentId, SyncCallback callback) {
		executeRequest("DELETE",
		BASE_URL + collection + "/" + documentId + "?key=" + API_KEY,
		null,
		noopIfNull(callback));
	}
	
	public static void batchCommit(List<BatchWrite> writes, SyncCallback callback) {
		if (writes == null || writes.isEmpty()) {
			if (callback != null) callback.onError("No writes supplied");
			return;
		}
		try {
			JSONArray arr = new JSONArray();
			for (BatchWrite w : writes) {
				JSONObject update = new JSONObject();
				update.put("name", "projects/" + PROJECT_ID
				+ "/databases/(default)/documents/" + w.collection + "/" + w.documentId);
				update.put("fields", mapToFieldsJson(w.data));
				JSONObject write = new JSONObject();
				write.put("update", update);
				arr.put(write);
			}
			JSONObject body = new JSONObject();
			body.put("writes", arr);
			String url = "https://firestore.googleapis.com/v1/projects/" + PROJECT_ID
			+ "/databases/(default)/documents:commit?key=" + API_KEY;
			executeRequest("POST", url, body.toString(), callback);
		} catch (Exception e) {
			if (callback != null) callback.onError(e.getMessage());
		}
	}
	
	// ── Queries ──────────────────────────────────────────────────────────────
	
	public static void runStructuredQuery(String collection, String field, String value,
	final SyncCallback callback) {
		if (callback == null) return;
		try {
			JSONObject fieldFilter = new JSONObject()
			.put("field", new JSONObject().put("fieldPath", field))
			.put("op", "EQUAL")
			.put("value", new JSONObject().put("stringValue", value));
			
			JSONObject query = new JSONObject()
			.put("from", new JSONArray().put(new JSONObject().put("collectionId", collection)))
			.put("where", new JSONObject().put("fieldFilter", fieldFilter))
			.put("orderBy", new JSONArray().put(
			new JSONObject()
			.put("field", new JSONObject().put("fieldPath", "timestamp"))
			.put("direction", "DESCENDING")))
			.put("limit", 30);
			
			JSONObject body = new JSONObject().put("structuredQuery", query);
			String url = "https://firestore.googleapis.com/v1/projects/" + PROJECT_ID
			+ "/databases/(default)/documents:runQuery?key=" + API_KEY;
			executeRequest("POST", url, body.toString(), callback);
		} catch (Exception e) {
			callback.onError(e.getMessage() != null ? e.getMessage() : "Query build failed");
		}
	}
	
	public static void recordTransaction(Context context, String senderAddress, String recipientAddress,
	double amountAjc, String txHash, String blockHash,
	long blockHeight, long nonce, SyncCallback callback) {
		Map<String, Object> txData = new HashMap<>();
		txData.put("blockHash", blockHash != null ? blockHash : "");
		txData.put("blockNumber", blockHeight);
		txData.put("confirmations", 1L);
		txData.put("from", senderAddress);
		txData.put("nonce", nonce);
		txData.put("recipientAddress", recipientAddress);
		txData.put("senderAddress", senderAddress);
		txData.put("status", "confirmed");
		txData.put("timestamp", System.currentTimeMillis());
		txData.put("to", recipientAddress);
		txData.put("tokenSymbol", com.harvanna.app.CoinInfo.ticker());
		txData.put("txHash", txHash);
		txData.put("value", amountAjc);
		
		String docId = (txHash != null && !txHash.isEmpty()) ? txHash : "TX-" + System.currentTimeMillis();
		
		createDocument(ChainConstants.COL_TRANSACTIONS, docId, txData, new SyncCallback() {
			@Override
			public void onSuccess(String result) {
				if (callback != null) callback.onSuccess("Transaction recorded successfully");
				if (context != null) {
					Intent intent = new Intent("com.harvanna.app.ACTION_WALLET_RECEIVED");
					intent.putExtra("sender", senderAddress);
					intent.putExtra("amount", amountAjc);
					LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
				}
			}
			
			@Override
			public void onError(String error) {
				if (callback != null) callback.onError(error);
			}
		});
	}
	
	
	public static void queryTransactionsByAddress(final String address,
	final com.harvanna.app.tx.TransactionManager.TransactionListCallback cb) {
		if (cb == null || address == null || address.trim().isEmpty()) {
			if (cb != null) cb.onError("Invalid address");
			return;
		}
		
		final String addr = address.trim();
		final Map<String, Map<String, Object>> merged = new java.util.concurrent.ConcurrentHashMap<>();
		final int[] remaining = {4};
		final boolean[] finished = {false};
		
		mainHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				if (!finished[0]) {
					finished[0] = true;
					List<Map<String, Object>> list = new ArrayList<>(merged.values());
					Collections.sort(list, new java.util.Comparator<Map<String, Object>>() {
						@Override
						public int compare(Map<String, Object> a, Map<String, Object> b) {
							return Long.compare(ChainMath.toLong(b.get("timestamp")),
							ChainMath.toLong(a.get("timestamp")));
						}
					});
					cb.onSuccess(list);
				}
			}
		}, 9000);
		
		final Runnable finishOne = new Runnable() {
			@Override
			public void run() {
				int left = --remaining[0];
				if (left > 0 || finished[0]) return;
				
				finished[0] = true;
				List<Map<String, Object>> list = new ArrayList<>(merged.values());
				Collections.sort(list, new java.util.Comparator<Map<String, Object>>() {
					@Override
					public int compare(Map<String, Object> a, Map<String, Object> b) {
						return Long.compare(ChainMath.toLong(b.get("timestamp")),
						ChainMath.toLong(a.get("timestamp")));
					}
				});
				if (list.size() > 50) list = list.subList(0, 50);
				cb.onSuccess(list);
			}
		};
		
		SyncCallback sharedCallback = new SyncCallback() {
			@Override
			public void onSuccess(String json) {
				try {
					addQueryResults(merged, json);
				} catch (Exception ignored) {}
				finishOne.run();
			}
			
			@Override
			public void onError(String e) {
				finishOne.run();
			}
		};
		
		runStructuredQuery(ChainConstants.COL_TRANSACTIONS, "senderAddress", addr, sharedCallback);
		runStructuredQuery(ChainConstants.COL_TRANSACTIONS, "recipientAddress", addr, sharedCallback);
		runStructuredQuery(ChainConstants.COL_TRANSACTIONS, "from", addr, sharedCallback);
		runStructuredQuery(ChainConstants.COL_TRANSACTIONS, "to", addr, sharedCallback);
	}
	
	private static void addQueryResults(Map<String, Map<String, Object>> merged, String json) throws Exception {
		JSONArray arr = new JSONArray(json);
		for (int i = 0; i < arr.length(); i++) {
			JSONObject wrapper = arr.getJSONObject(i);
			JSONObject doc = wrapper.optJSONObject("document");
			if (doc == null) continue;
			Map<String, Object> parsed = parseFirestoreDocument(doc.toString());
			if (parsed == null) continue;
			String id = doc.optString("name",
			parsed.get("txHash") == null ? UUID.randomUUID().toString()
			: String.valueOf(parsed.get("txHash")));
			merged.put(id, parsed);
		}
	}
	
	public static void getWalletTransactionHistory(final String address, final CollectionCallback callback) {
		if (callback == null) return;
		if (address == null || address.trim().isEmpty()) {
			callback.onError("No wallet address");
			return;
		}
		
		final String addr = address.trim();
		String url = BASE_URL + ChainConstants.COL_TRANSACTIONS
		+ "?pageSize=80&orderBy=timestamp%20desc&key=" + API_KEY;
		
		executeRequest("GET", url, null, new SyncCallback() {
			@Override
			public void onSuccess(String json) {
				List<Map<String, Object>> result = new ArrayList<>();
				try {
					JSONObject root = new JSONObject(json);
					JSONArray arr = root.optJSONArray("documents");
					if (arr != null) {
						for (int i = 0; i < arr.length(); i++) {
							JSONObject d = arr.getJSONObject(i);
							Map<String, Object> m = parseFirestoreDocument(d.toString());
							if (m == null) continue;
							
							String from = String.valueOf(m.get("from"));
							String to = String.valueOf(m.get("to"));
							String sender = String.valueOf(m.get("senderAddress"));
							String recipient = String.valueOf(m.get("recipientAddress"));
							
							boolean match = addr.equalsIgnoreCase(from)
							|| addr.equalsIgnoreCase(to)
							|| addr.equalsIgnoreCase(sender)
							|| addr.equalsIgnoreCase(recipient);
							
							if (match) {
								String name = d.optString("name", "");
								if (!name.isEmpty()) {
									m.put("_documentId", name.substring(name.lastIndexOf('/') + 1));
								}
								result.add(m);
							}
						}
					}
				} catch (Exception e) {
					callback.onError("Parse error: " + e.getMessage());
					return;
				}
				callback.onSuccess(result);
			}
			
			@Override
			public void onError(String e) {
				callback.onError(e);
			}
		});
	}
	
	// ── Parse ────────────────────────────────────────────────────────────────
	
	public static Map<String, Object> parseFirestoreDocument(String json) {
		if (json == null || json.trim().isEmpty() || json.trim().equals("{}")) {
			return null;
		}
		try {
			JSONObject root = new JSONObject(json);
			
			if (!root.has("fields") && !root.has("name")) {
				Map<String, Object> map = new HashMap<>();
				Iterator<String> keys = root.keys();
				while (keys.hasNext()) {
					String k = keys.next();
					map.put(k, root.get(k));
				}
				return map.isEmpty() ? null : map;
			}
			
			JSONObject fields = root.optJSONObject("fields");
			if (fields == null && root.has("document")) {
				fields = root.getJSONObject("document").optJSONObject("fields");
			}
			if (fields == null) return null;
			
			Map<String, Object> map = new HashMap<>();
			Iterator<String> it = fields.keys();
			while (it.hasNext()) {
				String key = it.next();
				map.put(key, extractValue(fields.getJSONObject(key)));
			}
			String name = root.optString("name", "");
			if (!name.isEmpty()) {
				map.put("_documentId", name.substring(name.lastIndexOf('/') + 1));
			}
			return map;
		} catch (Exception e) {
			return null;
		}
	}
	
	// ── HTTP (OkHttp) ────────────────────────────────────────────────────────
	
	private static SyncCallback noopIfNull(SyncCallback cb) {
		if (cb != null) return cb;
		return new SyncCallback() {
			@Override public void onSuccess(String r) {}
			@Override public void onError(String e) {}
		};
	}
	
	private static void executeRequest(final String method, final String urlString,
	final String body, final SyncCallback callback) {
		if (callback == null) return;
		
		// Run on background so Tasks.await in AuthTokenProvider is safe
		new Thread(new Runnable() {
			@Override
			public void run() {
				String token = AuthTokenProvider.getIdToken(false);
				
				Request.Builder rb = new Request.Builder().url(urlString);
				if (token != null && !token.isEmpty()) {
					rb.header("Authorization", "Bearer " + token);
				}
				
				String m = method != null ? method.toUpperCase() : "GET";
				if ("POST".equals(m) || "PATCH".equals(m) || "PUT".equals(m)) {
					RequestBody reqBody = RequestBody.create(JSON, body != null ? body : "{}");
					rb.method(m, reqBody);
				} else if ("DELETE".equals(m)) {
					rb.delete();
				} else {
					rb.get();
				}
				
				HTTP.newCall(rb.build()).enqueue(new Callback() {
					@Override
					public void onFailure(Call call, final IOException e) {
						mainHandler.post(new Runnable() {
							@Override
							public void run() {
								callback.onError(e.getMessage() != null ? e.getMessage() : "Network error");
							}
						});
					}
					
					@Override
					public void onResponse(Call call, final Response response) {
						final int code = response.code();
						String responseBody = "";
						try {
							ResponseBody rbod = response.body();
							if (rbod != null) responseBody = rbod.string();
						} catch (Exception ignored) {}
						final String text = responseBody != null ? responseBody : "";
						
						// One retry with forced token refresh on 401
						if (code == 401) {
							String fresh = AuthTokenProvider.getIdToken(true);
							if (fresh != null && !fresh.isEmpty()) {
								Request.Builder retry = new Request.Builder().url(urlString)
								.header("Authorization", "Bearer " + fresh);
								if ("POST".equals(m) || "PATCH".equals(m) || "PUT".equals(m)) {
									retry.method(m, RequestBody.create(JSON, body != null ? body : "{}"));
								} else if ("DELETE".equals(m)) {
									retry.delete();
								} else {
									retry.get();
								}
								try {
									Response r2 = HTTP.newCall(retry.build()).execute();
									String t2 = "";
									try {
										if (r2.body() != null) t2 = r2.body().string();
									} catch (Exception ignored) {}
									final int c2 = r2.code();
									final String body2 = t2;
									mainHandler.post(new Runnable() {
										@Override
										public void run() {
											if (c2 >= 200 && c2 < 300) callback.onSuccess(body2);
											else callback.onError("HTTP " + c2 + ": " + body2);
										}
									});
									return;
								} catch (Exception e) {
									mainHandler.post(new Runnable() {
										@Override
										public void run() {
											callback.onError(e.getMessage() != null ? e.getMessage() : "Retry failed");
										}
									});
									return;
								}
							}
						}
						
						mainHandler.post(new Runnable() {
							@Override
							public void run() {
								if (code >= 200 && code < 300) callback.onSuccess(text);
								else callback.onError("HTTP " + code + ": " + text);
							}
						});
					}
				});
			}
		}).start();
	}
	// ── Firestore JSON helpers ───────────────────────────────────────────────
	
	private static String mapToFirestoreJson(Map<String, Object> data) {
		try {
			return new JSONObject().put("fields", mapToFieldsJson(data)).toString();
		} catch (Exception e) {
			return "{}";
		}
	}
	
	private static JSONObject mapToFieldsJson(Map<String, Object> data) throws Exception {
		JSONObject fields = new JSONObject();
		if (data != null) {
			for (Map.Entry<String, Object> e : data.entrySet()) {
				fields.put(e.getKey(), wrapValue(e.getValue()));
			}
		}
		return fields;
	}
	
	private static JSONObject wrapValue(Object value) throws Exception {
		JSONObject o = new JSONObject();
		if (value == null) o.put("nullValue", JSONObject.NULL);
		else if (value instanceof String) o.put("stringValue", value);
		else if (value instanceof Boolean) o.put("booleanValue", value);
		else if (value instanceof Integer || value instanceof Long)
		o.put("integerValue", String.valueOf(value));
		else if (value instanceof Double || value instanceof Float)
		o.put("doubleValue", value);
		else if (value instanceof Map) {
			JSONObject f = new JSONObject();
			for (Map.Entry<?, ?> e : ((Map<?, ?>) value).entrySet()) {
				f.put(String.valueOf(e.getKey()), wrapValue(e.getValue()));
			}
			o.put("mapValue", new JSONObject().put("fields", f));
		} else if (value instanceof Iterable) {
			JSONArray a = new JSONArray();
			for (Object x : (Iterable<?>) value) a.put(wrapValue(x));
			o.put("arrayValue", new JSONObject().put("values", a));
		} else {
			o.put("stringValue", String.valueOf(value));
		}
		return o;
	}
	
	private static Object extractValue(JSONObject v) throws Exception {
		if (v.has("stringValue")) return v.getString("stringValue");
		if (v.has("booleanValue")) return v.getBoolean("booleanValue");
		if (v.has("integerValue")) return Long.parseLong(v.getString("integerValue"));
		if (v.has("doubleValue")) return v.getDouble("doubleValue");
		if (v.has("nullValue")) return null;
		if (v.has("mapValue")) {
			JSONObject f = v.getJSONObject("mapValue").optJSONObject("fields");
			Map<String, Object> m = new HashMap<>();
			if (f != null) {
				Iterator<String> it = f.keys();
				while (it.hasNext()) {
					String k = it.next();
					m.put(k, extractValue(f.getJSONObject(k)));
				}
			}
			return m;
		}
		if (v.has("arrayValue")) {
			List<Object> l = new ArrayList<>();
			JSONArray a = v.getJSONObject("arrayValue").optJSONArray("values");
			if (a != null) {
				for (int i = 0; i < a.length(); i++) {
					l.add(extractValue(a.getJSONObject(i)));
				}
			}
			return l;
		}
		return null;
	}
}
