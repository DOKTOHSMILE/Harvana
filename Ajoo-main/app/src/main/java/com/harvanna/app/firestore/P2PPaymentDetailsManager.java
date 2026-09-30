package com.harvanna.app.firestore;

import android.content.Context;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
* Firestore access (native SDK) for P2P_PAYMENT_DETAILS/{listingId} - same
* pattern as UserProfileManager and GroupFirestoreManager, kept as its own
* class for the same reason those two are separate: a distinct collection with its own lifecycle.
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
	private static final long WRITE_TIMEOUT_MS = 15_000L;
	private static final Executor BG = Executors.newCachedThreadPool();
	private static final ScheduledExecutorService TIMER = Executors.newSingleThreadScheduledExecutor();
	private static final String COLLECTION = "P2P_PAYMENT_DETAILS";
	
	private static P2PPaymentDetailsManager instance;
	
	public static synchronized P2PPaymentDetailsManager getInstance(Context context) {
		if (instance == null) {
			instance = new P2PPaymentDetailsManager();
		}
		return instance;
	}
	
	private P2PPaymentDetailsManager() {
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
	public void save(String listingId, java.util.List<com.harvanna.app.wallet.PaymentMethod> paymentMethods,
	String pricingType, double unitPrice, double marginPercent, int paymentWindowMinutes, String termsOfTrade, int reqAccountAgeDays, double maxOrderHarv, String currency, String sellerUid, SimpleCallback callback) {
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
		// ⚠️ maxOrderHarv is DISPLAY-ONLY right now, not enforced. The
		// node's createListing signed message and claim logic (see
		// P2PTradeRouter.createListing) only ever carried a MIN order
		// amount — there is no max field anywhere in the node's listing
		// schema or claim-processing path. Saving it here lets the app
		// show a merchant's intended max on their own listing, but a
		// buyer can still claim up to the full remaining listing amount
		// today; real liquidity protection needs a node-side change
		// (new schema field + claim-time check), not just this.
		fields.put("maxOrderHarv", maxOrderHarv);
		fields.put("currency", currency);
		
		java.util.List<Map<String, Object>> methodMaps = new java.util.ArrayList<>();
		for (com.harvanna.app.wallet.PaymentMethod pm : paymentMethods) {
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
		
		write(doc(COLLECTION, listingId), fields, callback);
	}
	
	/** Called by the buyer's client once they've claimed a listing —
*  Firestore security rules are what should actually gate this (only
*  the matched buyer or the seller can read a given listingId's
*  document), replacing the old approach of the node deciding what to
*  redact based on listing status. */	
	public void get(String listingId, DocCallback callback) {
		read(doc(COLLECTION, listingId), callback);
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
		
		write(doc(COLLECTION, listingId, "authorizedBuyers", buyerUid), fields, callback);
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
		if (FirebaseAuth.getInstance().getCurrentUser() == null) {
			BG.execute(() -> callback.onError("Not signed in \u2014 no current Firebase user."));
			return;
		}
		FirebaseFirestore.getInstance().collection(COLLECTION).document(listingId).collection("authorizedBuyers")
				.whereEqualTo("orderId", orderId).get().addOnCompleteListener(BG, task -> {
			if (!task.isSuccessful()) { callback.onError(describe(task.getException())); return; }
			QuerySnapshot snap = task.getResult();
			if (snap != null) {
				for (QueryDocumentSnapshot d : snap) {
					if (!d.getId().isEmpty()) { callback.onFound(d.getId()); return; }
				}
			}
			callback.onNotFound();
		});
	}
	
	// ── Order-level payment details (Sept 17 2026) ──────────────────────
	private static final String ORDER_COLLECTION = "P2P_ORDER_PAYMENT_DETAILS";
	
	public void saveOrderPaymentDetails(String orderId, com.harvanna.app.wallet.PaymentMethod method, String sellerUid, String buyerUid, SimpleCallback callback) {
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
		
		write(doc(ORDER_COLLECTION, orderId), fields, callback);
	}
	
	public void getOrderPaymentDetails(String orderId, DocCallback callback) {
		read(doc(ORDER_COLLECTION, orderId), callback);
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
	
	// ── SDK plumbing ──

	private static DocumentReference doc(String... path) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < path.length; i++) { if (i > 0) sb.append('/'); sb.append(path[i]); }
		return FirebaseFirestore.getInstance().document(sb.toString());
	}

	private static void write(DocumentReference ref, Map<String, Object> fields, SimpleCallback callback) {
		if (FirebaseAuth.getInstance().getCurrentUser() == null) {
			BG.execute(() -> callback.onError("Not signed in \u2014 no current Firebase user."));
			return;
		}
		final AtomicBoolean done = new AtomicBoolean(false);
		TIMER.schedule(() -> {
			if (done.compareAndSet(false, true)) {
				BG.execute(() -> callback.onError("Network error: write not acknowledged yet (queued; will sync when back online)"));
			}
		}, WRITE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
		ref.set(GroupFirestoreManager.clean(fields)).addOnCompleteListener(BG, t -> {
			if (!done.compareAndSet(false, true)) return;
			if (t.isSuccessful()) callback.onSuccess(); else callback.onError(describe(t.getException()));
		});
	}

	private static void read(DocumentReference ref, DocCallback callback) {
		if (FirebaseAuth.getInstance().getCurrentUser() == null) {
			BG.execute(() -> callback.onError("Not signed in \u2014 no current Firebase user."));
			return;
		}
		ref.get().addOnCompleteListener(BG, task -> {
			if (!task.isSuccessful()) { callback.onError(describe(task.getException())); return; }
			DocumentSnapshot snap = task.getResult();
			if (snap == null || !snap.exists()) { callback.onError("HTTP 404: document " + ref.getPath() + " not found"); return; }
			callback.onSuccess(GroupFirestoreManager.read(snap.getData()));
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
				default:                  return "HTTP 500: " + msg;
			}
		}
		return "Network error: " + (e != null && e.getMessage() != null ? e.getMessage() : "unknown error");
	}
}
