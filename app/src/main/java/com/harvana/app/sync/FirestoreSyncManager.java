package com.harvana.app.sync;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.harvana.app.AddressUtil;
import com.harvana.app.ChainConstants;
import com.harvana.app.ChainMath;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.google.android.gms.tasks.Task;

public class FirestoreSyncManager {
	
	private static final long WRITE_TIMEOUT_MS = 15_000L;
	private static final String DOC_PREFIX = "projects/capital-oracle-project/databases/(default)/documents/";
	
	private static final Handler mainHandler = new Handler(Looper.getMainLooper());
	private static final FirestoreSyncManager INSTANCE = new FirestoreSyncManager();
	
	private FirestoreSyncManager() {}
	
	public static FirestoreSyncManager getInstance() {
		return INSTANCE;
	}
	
	public interface SyncCallback {
		void onSuccess(String result);
		void onError(String error);
		/** Error-only view of a callback, so shared helpers can report failures. */
		interface ErrorSink { void onError(String error); }
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
	 * @deprecated Wallets live on the HARV node (RocksDB), not Firestore.
	 * This method no longer writes COL_WALLETS. It soft-checks the node and
	 * returns a local stub map so legacy callers do not break.
	 * Prefer HarvanaNodeClient.getWallet(address, …) directly.
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
		HarvanaNodeClient.getWallet(normalized, new HarvanaNodeClient.JsonCallback() {
			@Override
			public void onSuccess(org.json.JSONObject json) {
				Map<String, Object> wallet = new HashMap<>();
				wallet.put("address", normalized);
				wallet.put("balance_harv", 0.0);
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
					callback.onError((com.harvana.app.CoinInfo.ticker() + " node unreachable: ") + error);
					return;
				}
				Map<String, Object> wallet = new HashMap<>();
				wallet.put("address", normalized);
				wallet.put("balance_harv", 0.0);
				wallet.put("nonce", 0L);
				wallet.put("source", "node");
				callback.onSuccess(wallet);
			}
		});
	}
	
	// ── CRUD ─────────────────────────────────────────────────────────────────
	
	/**
	 * Document as a REST-shaped JSON string ({"name": ..., "fields": {...}}), the
	 * shape parseFirestoreDocument() and the existing callers expect. The data
	 * itself now comes from the native Firestore SDK.
	 */
	public static void getDocument(String collection, String documentId, SyncCallback callback) {
		if (callback == null) return;
		DocumentReference ref = ref(collection, documentId, callback);
		if (ref == null) return;
		ref.get().addOnCompleteListener(task -> {
			if (!task.isSuccessful()) { callback.onError(describe(task.getException())); return; }
			DocumentSnapshot snap = task.getResult();
			if (snap == null || !snap.exists()) { callback.onError("HTTP 404: document " + ref.getPath() + " not found"); return; }
			try {
				callback.onSuccess(docJson(ref.getPath(), snap.getData()).toString());
			} catch (Exception e) {
				callback.onError("Failed to read document: " + e.getMessage());
			}
		});
	}
	
	public static void getCollection(String collectionPath, final CollectionCallback callback) {
		if (callback == null) return;
		if (!signedIn(callback::onError)) return;
		FirebaseFirestore.getInstance().collection(collectionPath).get().addOnCompleteListener(task -> {
			if (!task.isSuccessful()) { callback.onError(describe(task.getException())); return; }
			List<Map<String, Object>> docs = new ArrayList<>();
			if (task.getResult() != null) {
				for (QueryDocumentSnapshot d : task.getResult()) {
					Map<String, Object> m = readMap(d.getData());
					m.put("_documentId", d.getId());
					docs.add(m);
				}
			}
			callback.onSuccess(docs);
		});
	}
	
	/** Replaces the whole document with data. */
	public static void updateDocument(String collection, String documentId,
	Map<String, Object> data, SyncCallback callback) {
		SyncCallback cb = noopIfNull(callback);
		DocumentReference ref = ref(collection, documentId, cb);
		if (ref == null) return;
		await(ref.set(writeMap(data)), cb);
	}
	
	/** Field-masked merge: only the given top-level fields change. */
	public static void patchFields(String collection, String documentId,
	Map<String, Object> fields, SyncCallback callback) {
		if (fields == null || fields.isEmpty()) {
			if (callback != null) callback.onSuccess("{}");
			return;
		}
		SyncCallback cb = noopIfNull(callback);
		DocumentReference ref = ref(collection, documentId, cb);
		if (ref == null) return;
		await(ref.set(writeMap(fields), SetOptions.mergeFields(new ArrayList<>(fields.keySet()))), cb);
	}
	
	public static void createDocument(String collection, String documentId,
	Map<String, Object> data, SyncCallback callback) {
		// Fixed document ids make this idempotent: a repeat writes the same content again.
		updateDocument(collection, documentId, data, callback);
	}
	
	public static void deleteDocument(String collection, String documentId, SyncCallback callback) {
		SyncCallback cb = noopIfNull(callback);
		DocumentReference ref = ref(collection, documentId, cb);
		if (ref == null) return;
		await(ref.delete(), cb);
	}
	
	public static void batchCommit(List<BatchWrite> writes, SyncCallback callback) {
		if (writes == null || writes.isEmpty()) {
			if (callback != null) callback.onError("No writes supplied");
			return;
		}
		SyncCallback cb = noopIfNull(callback);
		if (!signedIn(cb::onError)) return;
		try {
			WriteBatch batch = FirebaseFirestore.getInstance().batch();
			for (BatchWrite w : writes) {
				batch.set(FirebaseFirestore.getInstance().collection(w.collection).document(w.documentId), writeMap(w.data));
			}
			await(batch.commit(), cb);
		} catch (Exception e) {
			cb.onError(e.getMessage() != null ? e.getMessage() : "Batch failed");
		}
	}
	
	// ── Queries ──────────────────────────────────────────────────────────────
	
	/** Equality query, newest first (max 30); result is a REST-shaped JSON array of {"document": ...}. */
	public static void runStructuredQuery(String collection, String field, String value,
	final SyncCallback callback) {
		if (callback == null) return;
		if (!signedIn(callback::onError)) return;
		FirebaseFirestore.getInstance().collection(collection).whereEqualTo(field, value)
		.orderBy("timestamp", Query.Direction.DESCENDING).limit(30).get().addOnCompleteListener(task -> {
			if (!task.isSuccessful()) { callback.onError(describe(task.getException())); return; }
			try {
				JSONArray arr = new JSONArray();
				if (task.getResult() != null) {
					for (QueryDocumentSnapshot d : task.getResult()) {
						arr.put(new JSONObject().put("document", docJson(d.getReference().getPath(), d.getData())));
					}
				}
				callback.onSuccess(arr.toString());
			} catch (Exception e) {
				callback.onError(e.getMessage() != null ? e.getMessage() : "Query failed");
			}
		});
	}
	
	public static void recordTransaction(Context context, String senderAddress, String recipientAddress,
	double amountHarv, String txHash, String blockHash,
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
		txData.put("tokenSymbol", com.harvana.app.CoinInfo.ticker());
		txData.put("txHash", txHash);
		txData.put("value", amountHarv);
		
		String docId = (txHash != null && !txHash.isEmpty()) ? txHash : "TX-" + System.currentTimeMillis();
		
		createDocument(ChainConstants.COL_TRANSACTIONS, docId, txData, new SyncCallback() {
			@Override
			public void onSuccess(String result) {
				if (callback != null) callback.onSuccess("Transaction recorded successfully");
				if (context != null) {
					Intent intent = new Intent("com.harvana.app.ACTION_WALLET_RECEIVED");
					intent.putExtra("sender", senderAddress);
					intent.putExtra("amount", amountHarv);
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
	final com.harvana.app.tx.TransactionManager.TransactionListCallback cb) {
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
		if (!signedIn(callback::onError)) return;
		FirebaseFirestore.getInstance().collection(ChainConstants.COL_TRANSACTIONS)
		.orderBy("timestamp", Query.Direction.DESCENDING).limit(80).get().addOnCompleteListener(task -> {
			if (!task.isSuccessful()) { callback.onError(describe(task.getException())); return; }
			List<Map<String, Object>> result = new ArrayList<>();
			if (task.getResult() != null) {
				for (QueryDocumentSnapshot d : task.getResult()) {
					Map<String, Object> m = readMap(d.getData());
					String from = String.valueOf(m.get("from"));
					String to = String.valueOf(m.get("to"));
					String sender = String.valueOf(m.get("senderAddress"));
					String recipient = String.valueOf(m.get("recipientAddress"));
					boolean match = addr.equalsIgnoreCase(from)
					|| addr.equalsIgnoreCase(to)
					|| addr.equalsIgnoreCase(sender)
					|| addr.equalsIgnoreCase(recipient);
					if (match) {
						m.put("_documentId", d.getId());
						result.add(m);
					}
				}
			}
			callback.onSuccess(result);
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
	
	// ── Helpers ────────────────────────────────────────────────────────────
	
	private static SyncCallback noopIfNull(SyncCallback cb) {
		if (cb != null) return cb;
		return new SyncCallback() {
			@Override public void onSuccess(String r) {}
			@Override public void onError(String e) {}
		};
	}
	
	// ── Native SDK plumbing ──────────────────────────────────────────────────
	
	private static boolean signedIn(SyncCallback.ErrorSink onError) {
		if (FirebaseAuth.getInstance().getCurrentUser() != null) return true;
		mainHandler.post(() -> onError.onError("Not signed in \u2014 no current Firebase user."));
		return false;
	}
	
	private static DocumentReference ref(String collection, String documentId, SyncCallback cb) {
		if (!signedIn(cb::onError)) return null;
		if (collection == null || collection.isEmpty() || documentId == null || documentId.isEmpty()) {
			mainHandler.post(() -> cb.onError("HTTP 400: missing collection or document id"));
			return null;
		}
		return FirebaseFirestore.getInstance().collection(collection).document(documentId);
	}
	
	/** Reports exactly one result: the SDK's ack, or a network error after WRITE_TIMEOUT_MS. */
	private static void await(Task<Void> task, final SyncCallback cb) {
		final AtomicBoolean done = new AtomicBoolean(false);
		final Runnable timeout = () -> {
			if (done.compareAndSet(false, true)) {
				cb.onError("Network error: write not acknowledged yet (queued; will sync when back online)");
			}
		};
		mainHandler.postDelayed(timeout, WRITE_TIMEOUT_MS);
		task.addOnCompleteListener(t -> {
			if (!done.compareAndSet(false, true)) return;
			mainHandler.removeCallbacks(timeout);
			if (t.isSuccessful()) cb.onSuccess("{}");
			else cb.onError(describe(t.getException()));
		});
	}
	
	private static String describe(Exception e) {
		if (e instanceof FirebaseFirestoreException) {
			FirebaseFirestoreException fe = (FirebaseFirestoreException) e;
			String msg = fe.getMessage() != null ? fe.getMessage() : fe.getCode().name();
			switch (fe.getCode()) {
				case NOT_FOUND:           return "HTTP 404: " + msg;
				case PERMISSION_DENIED:   return "HTTP 403: " + msg;
				case UNAUTHENTICATED:     return "HTTP 401: " + msg;
				case UNAVAILABLE:
				case DEADLINE_EXCEEDED:
				case CANCELLED:           return "Network error: " + msg;
				case INVALID_ARGUMENT:
				case FAILED_PRECONDITION:
				case ALREADY_EXISTS:      return "HTTP 400: " + msg;
				default:                  return "HTTP 500: " + msg;
			}
		}
		return "Network error: " + (e != null && e.getMessage() != null ? e.getMessage() : "unknown error");
	}
	
	private static Map<String, Object> writeMap(Map<String, Object> data) { return com.harvana.app.firestore.GroupFirestoreManager.cleanForWrite(data); }
	private static Map<String, Object> readMap(Map<String, Object> data) { return com.harvana.app.firestore.GroupFirestoreManager.readFromSdk(data); }
	
	private static JSONObject docJson(String path, Map<String, Object> data) throws Exception {
		return new JSONObject().put("name", DOC_PREFIX + path).put("fields", mapToFieldsJson(readMap(data)));
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
