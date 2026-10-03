package com.harvana.app.group;

import android.content.Context;
import android.util.Log;

import com.harvana.app.firestore.GroupFirestoreManager;
import com.google.firebase.auth.FirebaseAuth;

import java.util.HashMap;
import java.util.Map;

/**
 * Standardised contribution / payment recording for the Group Engine.
 *
 * Ensures every money movement related to a group lands in
 * GROUPS/{groupId}/payments with the correct contract fields:
 *   type, status, amountHarv, from, to, memberUid, slot, txRef, timestamp
 *
 * Use this instead of writing payment maps ad-hoc from activities.
 */
public final class GroupPaymentHelper {
	
	private static final String TAG = "GroupPaymentHelper";
	
	public interface Callback {
		void onSuccess(String paymentDocId);
		void onError(String error);
	}
	
	private GroupPaymentHelper() {}
	
	// ── Rotating / general contribution ──────────────────────────────────
	
	public static void recordContributionPending(Context context,
	String groupId,
	String memberUid,
	int slot,
	double amountHarv,
	String fromWallet,
	String groupWallet,
	String txRef,
	Callback callback) {
		String err = GroupValidation.firstError(
		GroupValidation.requireGroupId(groupId),
		GroupValidation.requirePositiveAmount(amountHarv),
		GroupValidation.requireValidWallet(fromWallet, "Member wallet"),
		GroupValidation.requireValidWallet(groupWallet, "Group wallet")
		);
		if (err != null) {
			if (callback != null) callback.onError(err);
			return;
		}
		
		Map<String, Object> payment = new HashMap<>();
		payment.put("type", "CONTRIBUTION");
		payment.put("status", "PENDING");
		payment.put("memberUid", memberUid != null ? memberUid : currentUid());
		payment.put("slot", slot);
		payment.put("amountHarv", amountHarv);
		payment.put("from", fromWallet.trim());
		payment.put("to", groupWallet.trim());
		payment.put("txRef", txRef != null ? txRef : "");
		payment.put("timestamp", System.currentTimeMillis());
		
		GroupFirestoreManager.getInstance(context)
		.addPayment(groupId, payment, new GroupFirestoreManager.IdCallback() {
			@Override
			public void onSuccess(String id) {
				if (callback != null) callback.onSuccess(id);
			}
			
			@Override
			public void onError(String error) {
				if (callback != null) callback.onError(error);
			}
		});
	}
	
	public static void recordContributionConfirmed(Context context,
	String groupId,
	String memberUid,
	int slot,
	double amountHarv,
	String fromWallet,
	String groupWallet,
	String txRef,
	Callback callback) {
		String err = GroupValidation.firstError(
		GroupValidation.requireGroupId(groupId),
		GroupValidation.requirePositiveAmount(amountHarv),
		GroupValidation.requireValidWallet(fromWallet, "Member wallet"),
		GroupValidation.requireValidWallet(groupWallet, "Group wallet")
		);
		if (err != null) {
			if (callback != null) callback.onError(err);
			return;
		}
		if (txRef == null || txRef.trim().isEmpty()) {
			if (callback != null) callback.onError("Missing on-chain tx hash — cannot mark CONFIRMED");
			return;
		}

		GroupOnChainGate.requireConfirmedTx(txRef, new GroupOnChainGate.Callback() {
			@Override
			public void onConfirmed(String hash, long blockHeight) {
				writeConfirmedContribution(context, groupId, memberUid, slot, amountHarv,
						fromWallet, groupWallet, hash, callback);
			}
			@Override
			public void onError(String error) {
				if (callback != null) callback.onError(error);
			}
		});
	}

	private static void writeConfirmedContribution(Context context,
			String groupId, String memberUid, int slot, double amountHarv,
			String fromWallet, String groupWallet, String txRef, Callback callback) {
		Map<String, Object> payment = new HashMap<>();
		payment.put("type", "CONTRIBUTION");
		payment.put("status", "CONFIRMED");
		payment.put("memberUid", memberUid != null ? memberUid : currentUid());
		payment.put("slot", slot);
		payment.put("amountHarv", amountHarv);
		payment.put("from", fromWallet.trim());
		payment.put("to", groupWallet.trim());
		payment.put("txRef", txRef != null ? txRef : "");
		payment.put("timestamp", System.currentTimeMillis());
		payment.put("confirmedAt", System.currentTimeMillis());
		
		GroupFirestoreManager.getInstance(context)
		.addPayment(groupId, payment, new GroupFirestoreManager.IdCallback() {
			@Override
			public void onSuccess(String id) {
				if (callback != null) callback.onSuccess(id);
			}
			
			@Override
			public void onError(String error) {
				if (callback != null) callback.onError(error);
			}
		});
	}
	
	// ── Fixed daily ──────────────────────────────────────────────────────
	
	public static void recordFixedDailyPending(Context context,
	String groupId,
	String memberUid,
	String memberName,
	double dailyAmount,
	int day,
	String monthKey,
	String fromWallet,
	String groupWallet,
	String txRef,
	java.util.List<Integer> existingDaysPaid,
	Callback callback) {
		String err = GroupValidation.firstError(
		GroupValidation.requireGroupId(groupId),
		GroupValidation.requirePositiveAmount(dailyAmount)
		);
		if (err != null) {
			if (callback != null) callback.onError(err);
			return;
		}
		if (day < 1 || day > FixedPaymentEngine.DAYS_IN_GRID) {
			if (callback != null) callback.onError("Invalid day (1–31).");
			return;
		}
		
		Map<String, Object> payment = FixedPaymentEngine.pendingDailyPayment(
		memberUid, memberName, dailyAmount, day, monthKey, txRef, fromWallet, groupWallet);
		
		GroupFirestoreManager.getInstance(context)
		.addPayment(groupId, payment, new GroupFirestoreManager.IdCallback() {
			@Override
			public void onSuccess(String paymentId) {
				Map<String, Object> memberUpdate = FixedPaymentEngine.memberDayUpdate(
				existingDaysPaid, day, monthKey != null ? monthKey : FixedPaymentEngine.currentMonthKey());
				
				GroupFirestoreManager.getInstance(context)
				.updateMemberFields(groupId, memberUid, memberUpdate,
				new GroupFirestoreManager.SimpleCallback() {
					@Override
					public void onSuccess() {
						if (callback != null) callback.onSuccess(paymentId);
					}
					
					@Override
					public void onError(String error) {
						Log.w(TAG, "Member day update failed: " + error);
						if (callback != null) callback.onSuccess(paymentId);
					}
				});
			}
			
			@Override
			public void onError(String error) {
				if (callback != null) callback.onError(error);
			}
		});
	}
	
	// ── Confirm existing payment ─────────────────────────────────────────
	
	public static void confirmPayment(Context context,
	String groupId,
	String paymentDocId,
	String txRef,
	Callback callback) {
		if (GroupValidation.isBlank(groupId) || GroupValidation.isBlank(paymentDocId)) {
			if (callback != null) callback.onError("Missing group or payment id.");
			return;
		}
		if (GroupValidation.isBlank(txRef)) {
			if (callback != null) callback.onError("Missing on-chain tx hash — cannot mark CONFIRMED");
			return;
		}

		GroupOnChainGate.requireConfirmedTx(txRef, new GroupOnChainGate.Callback() {
			@Override
			public void onConfirmed(String hash, long blockHeight) {
				patchPaymentConfirmed(context, groupId, paymentDocId, hash, callback);
			}
			@Override
			public void onError(String error) {
				if (callback != null) callback.onError(error);
			}
		});
	}

	private static void patchPaymentConfirmed(Context context, String groupId,
			String paymentDocId, String txRef, Callback callback) {
		Map<String, Object> fields = FixedPaymentEngine.confirmPaymentFields(txRef);
		
		GroupFirestoreManager.getInstance(context)
		.updatePaymentStatus(groupId, paymentDocId, fields,
		new GroupFirestoreManager.SimpleCallback() {
			@Override
			public void onSuccess() {
				if (callback != null) callback.onSuccess(paymentDocId);
			}
			
			@Override
			public void onError(String error) {
				if (callback != null) callback.onError(error);
			}
		});
	}
	
	private static String currentUid() {
		return FirebaseAuth.getInstance().getCurrentUser() != null
		? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
	}
	
	// ── Offline replay ──────────────────────────────────────────────────
	
	public static void replayContributionFromQueue(Context context, Map<String, Object> payload, 
                                                   com.harvana.app.offline.OfflineActionQueue.FlushCallback callback) {
        String groupId = _s(payload, "groupId");
        String memberUid = _s(payload, "memberUid");
        String memberName = _s(payload, "memberName");
        int slot = payload.get("slot") instanceof Number ? ((Number) payload.get("slot")).intValue() : 0;
        double amountHarv = payload.get("amountHarv") instanceof Number ? ((Number) payload.get("amountHarv")).doubleValue() : 0;
        String from = _s(payload, "from");
        String groupWallet = _s(payload, "groupWallet");
        String txRef = _s(payload, "txRef");

        recordContributionPending(context, groupId, memberUid, slot, amountHarv, from, groupWallet, txRef, new Callback() {
            @Override 
            public void onSuccess(String paymentDocId) {
                if (memberName != null && !memberName.isEmpty()) {
                    Map<String, Object> update = new HashMap<>();
                    update.put("memberName", memberName);
                    com.harvana.app.firestore.GroupFirestoreManager.getInstance(context)
                        .updatePaymentStatus(groupId, paymentDocId, update, null);
                }
                if (callback != null) callback.onSuccess();
            }
            @Override 
            public void onError(String error) {
                if (callback != null) callback.onError(error);
            }
        });
    }
	
	private static String _s(Map<String, Object> m, String k) {
		Object v = m.get(k);
		return v != null ? String.valueOf(v) : "";
	}
}