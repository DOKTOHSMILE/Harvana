package com.harvanna.app.firestore;

import android.content.Context;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.List;

/**
 * GROUND-UP (Sept 2026) — P2P trade reputation stats, moved here after
 * being wrongly built on the node first. Trade count and success rate are
 * public profile/reputation data — the same category as TrustScoreEngine's
 * groups/cycles/tenure, which already live in Firestore, not the node.
 * The node's job is custody and signature verification of actual fund
 * movement; a full order-history scan to compute a reputation percentage
 * has nothing to do with that and doesn't belong running on every
 * candidate check in the Express matcher.
 *
 * Native Firestore SDK, like UserProfileManager —
 * kept as its own small class/collection (P2P_TRADE_STATS/{uid}) rather
 * than folded into USERS/{uid}, since this is conceptually its own
 * lifecycle (incremented once per terminal order, read far more often
 * than it's written).
 *
 * Self-reported by design, same trust model as the rest of this app's
 * Firestore-side reputation data (nothing here is enforced by a backend
 * function) — each party records their own outcome when they observe a
 * terminal order status client-side. That's a real limit worth naming
 * plainly: it's not a security boundary, it's a reputation signal.
 */
public class P2PTradeStatsManager {

    private static final String TAG = "P2PTradeStatsManager";

    private static P2PTradeStatsManager instance;

    public static synchronized P2PTradeStatsManager getInstance(Context context) {
        if (instance == null) instance = new P2PTradeStatsManager();
        return instance;
    }

    private P2PTradeStatsManager() {
    }

    public interface StatsCallback {
        /** successRatePercent is 0 when totalTrades is 0 — no trades yet
         *  is not the same as a 0% success rate, callers should check
         *  totalTrades before reading anything into the rate. */
        void onSuccess(int totalTrades, int successfulTrades, double successRatePercent);
        void onError(String error);
    }

    public interface SimpleCallback {
        void onSuccess();
        void onError(String error);
    }

    // GROUND-UP REWRITE (Sept 2026): the client-write path that used to
    // live here (hasRecordedOutcomeFor/recordOutcomeOnce) is gone on
    // purpose — see the class doc. The node is now the only writer of
    // completed-trade events, as a side effect of a release it already
    // signature-verified. Firestore rules should deny client writes to
    // P2P_COMPLETED_TRADES outright; this class is read-only now.

    /** Query-based, not a counter — see class doc. Currently only counts
     *  genuine successes (the node only writes P2P_COMPLETED_TRADES on
     *  the real COMPLETED path). Cancelled orders and disputes resolved
     *  against someone aren't recorded anywhere yet, so a true success
     *  RATE (successes ÷ everything attempted) isn't answerable honestly
     *  right now — only a real completed-trade count is. successRate is
     *  returned as -1 to make that explicit rather than quietly showing
     *  a misleading 100%; callers should treat -1 as "not available,"
     *  not "zero." */
    public interface FullStatsCallback {
        /** Same successRatePercent honesty rule as StatsCallback.
         *  uniqueCounterparties is real, not estimated — derived by
         *  reading the actual counterparty out of each completed-trade
         *  record this uid appears in and counting distinct ones, using
         *  data the node already writes (participantUids) rather than a
         *  new collection or a new write path. */
        void onSuccess(int totalTrades, int successfulTrades, double successRatePercent, int uniqueCounterparties);
        void onError(String error);
    }

    public void getFullStats(String uid, FullStatsCallback callback) {
        query(uid, (snap, err) -> {
            if (err != null) { callback.onError(err); return; }
            java.util.Set<String> counterparties = new java.util.HashSet<>();
            int count = 0;
            for (DocumentSnapshot d : snap) {
                count++;
                Object participantsObj = d.get("participantUids");
                if (participantsObj instanceof List) {
                    for (Object p : (List<?>) participantsObj) {
                        String pUid = String.valueOf(p);
                        if (!uid.equals(pUid)) counterparties.add(pUid);
                    }
                }
            }
            callback.onSuccess(count, count, -1, counterparties.size());
        });
    }

    public void getStats(String uid, StatsCallback callback) {
        query(uid, (snap, err) -> {
            if (err != null) { callback.onError(err); return; }
            int count = 0;
            for (DocumentSnapshot ignored : snap) count++;
            callback.onSuccess(count, count, -1);
        });
    }

    private interface Result { void done(Iterable<? extends DocumentSnapshot> docs, String error); }

    /** Completed trades this uid took part in (native Firestore query). */
    private void query(String uid, Result result) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            result.done(null, "Not signed in \u2014 no current Firebase user.");
            return;
        }
        FirebaseFirestore.getInstance().collection("P2P_COMPLETED_TRADES")
                .whereArrayContains("participantUids", uid).get()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        result.done(null, describe(task.getException()));
                    } else {
                        result.done(task.getResult(), null);
                    }
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
