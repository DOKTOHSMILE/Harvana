package com.harvana.app.wallet;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Distributed rate-limit mitigation and collective caching architecture.
 *
 * Directs limited API calls to Firebase Firestore, then distributes the cached results
 * to every user across the network. Only ONE user at a time acquires the leader lock,
 * queries the live endpoint (CoinGecko, DexScreener, CoinMarketCap, Binance), and writes
 * fresh data back to Firestore.
 *
 * For the duration of TTL (e.g. 10 minutes for quotes, 4 hours for charts/metadata),
 * all other users read directly from the shared Firestore cache without hitting rate limits.
 * Once the TTL elapses, the first user whose app accesses the asset matches the timestamp,
 * acquires the mutex lock, and fetches fresh data to update Firestore for everyone.
 */
public final class FirestoreSharedCache {
    private static final String TAG = "FirestoreSharedCache";
    private static final String COLLECTION = "MARKET_CACHE";
    private static final long LOCK_TIMEOUT_MS = 45 * 1000L; // 45 seconds lock lease
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    // Local in-memory L1 cache to avoid redundant Firestore reads within the same process
    private static final Map<String, CachedEntry> L1_CACHE = new ConcurrentHashMap<>();

    private static class CachedEntry {
        final Map<String, Object> data;
        final long updatedAt;
        final long ttlMs;

        CachedEntry(Map<String, Object> data, long updatedAt, long ttlMs) {
            this.data = data;
            this.updatedAt = updatedAt;
            this.ttlMs = ttlMs;
        }

        boolean isFresh() {
            return (System.currentTimeMillis() - updatedAt) < ttlMs;
        }
    }

    public interface Fetcher {
        void fetch(FetchResult result);
    }

    public interface FetchResult {
        void onSuccess(Map<String, Object> data);
        void onError(String error);
    }

    public interface Callback {
        void onData(Map<String, Object> data, boolean fresh);
    }

    private FirestoreSharedCache() {}

    /**
     * Gets asset data from Firestore shared cache, or elects this client to fetch fresh data
     * if the cache has expired and no other client holds the active lock.
     */
    public static void getOrFetch(String assetKey, long ttlMs, Fetcher fetcher, Callback callback) {
        if (assetKey == null || assetKey.trim().isEmpty()) {
            if (callback != null) callback.onData(new HashMap<>(), false);
            return;
        }

        final String docId = sanitizeKey(assetKey);
        final long now = System.currentTimeMillis();

        // 1. Check L1 Memory Cache
        CachedEntry mem = L1_CACHE.get(docId);
        if (mem != null && mem.isFresh()) {
            if (callback != null) MAIN.post(() -> callback.onData(mem.data, false));
            return;
        }

        // 2. Check Firestore Shared Cache
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference docRef = db.collection(COLLECTION).document(docId);

        docRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                DocumentSnapshot snap = task.getResult();
                Long updatedAt = snap.getLong("updatedAt");
                Long lockedUntil = snap.getLong("lockedUntil");
                Map<String, Object> data = (Map<String, Object>) snap.get("data");

                long updated = updatedAt != null ? updatedAt : 0L;
                long lock = lockedUntil != null ? lockedUntil : 0L;

                // Cache is still fresh for everyone: distribute to user
                if (data != null && (now - updated) < ttlMs) {
                    L1_CACHE.put(docId, new CachedEntry(data, updated, ttlMs));
                    if (callback != null) MAIN.post(() -> callback.onData(data, false));
                    return;
                }

                // Cache is expired, but another user is currently fetching (holding the lock)
                if (data != null && lock > now) {
                    // Return existing data (stale-while-revalidate), do NOT hammer the external API
                    L1_CACHE.put(docId, new CachedEntry(data, updated, ttlMs));
                    if (callback != null) MAIN.post(() -> callback.onData(data, false));
                    return;
                }
            }

            // Cache missing or expired with no active lock: try to acquire lock and fetch fresh
            tryAcquireLockAndFetch(docRef, docId, ttlMs, fetcher, callback);
        });
    }

    private static void tryAcquireLockAndFetch(DocumentReference docRef, String docId,
                                              long ttlMs, Fetcher fetcher, Callback callback) {
        final String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "guest_" + System.currentTimeMillis();
        final long now = System.currentTimeMillis();

        // Attempt transaction to atomically acquire leader lock
        FirebaseFirestore.getInstance().runTransaction(transaction -> {
            DocumentSnapshot snap = transaction.get(docRef);
            Long lockedUntil = snap.getLong("lockedUntil");
            Long updatedAt = snap.getLong("updatedAt");

            // If another user just took the lock or refreshed the cache, abort taking lock
            if (snap.exists()) {
                if (updatedAt != null && (now - updatedAt) < ttlMs) {
                    return false; // Already refreshed by another leader
                }
                if (lockedUntil != null && lockedUntil > now) {
                    return false; // Locked by another leader
                }
            }

            // Acquire lock for 45s
            Map<String, Object> lockData = new HashMap<>();
            lockData.put("lockedUntil", now + LOCK_TIMEOUT_MS);
            lockData.put("lockedBy", uid);
            transaction.set(docRef, lockData, SetOptions.merge());
            return true;
        }).addOnSuccessListener(acquiredLock -> {
            if (Boolean.TRUE.equals(acquiredLock)) {
                // We won the leader election! Execute the live API query
                if (fetcher == null) return;
                fetcher.fetch(new FetchResult() {
                    @Override
                    public void onSuccess(Map<String, Object> freshData) {
                        long refreshedAt = System.currentTimeMillis();

                        Map<String, Object> payload = new HashMap<>();
                        payload.put("data", freshData);
                        payload.put("updatedAt", refreshedAt);
                        payload.put("ttlMs", ttlMs);
                        payload.put("lockedUntil", 0L); // Release lock
                        payload.put("lockedBy", "");

                        // Save to Firestore so ALL other users instantly benefit
                        docRef.set(payload, SetOptions.merge())
                                .addOnSuccessListener(v -> Log.d(TAG, "Uploaded fresh market cache for " + docId))
                                .addOnFailureListener(e -> Log.w(TAG, "Failed writing market cache", e));

                        L1_CACHE.put(docId, new CachedEntry(freshData, refreshedAt, ttlMs));
                        if (callback != null) MAIN.post(() -> callback.onData(freshData, true));
                    }

                    @Override
                    public void onError(String error) {
                        Log.w(TAG, "Leader fetch failed for " + docId + ": " + error);
                        // Release lock so someone else can retry
                        Map<String, Object> release = new HashMap<>();
                        release.put("lockedUntil", 0L);
                        docRef.set(release, SetOptions.merge());

                        // Fall back to L1 cache if available
                        CachedEntry old = L1_CACHE.get(docId);
                        if (callback != null) MAIN.post(() -> callback.onData(old != null ? old.data : new HashMap<>(), false));
                    }
                });
            } else {
                // Someone else got the lock: read whatever Firestore has or use L1
                docRef.get().addOnSuccessListener(snap -> {
                    Map<String, Object> data = snap != null ? (Map<String, Object>) snap.get("data") : null;
                    if (data != null) {
                        L1_CACHE.put(docId, new CachedEntry(data, snap.getLong("updatedAt") != null ? snap.getLong("updatedAt") : now, ttlMs));
                        if (callback != null) MAIN.post(() -> callback.onData(data, false));
                    } else {
                        CachedEntry old = L1_CACHE.get(docId);
                        if (callback != null) MAIN.post(() -> callback.onData(old != null ? old.data : new HashMap<>(), false));
                    }
                });
            }
        }).addOnFailureListener(e -> {
            Log.w(TAG, "Transaction lock failed: " + e.getMessage());
            CachedEntry old = L1_CACHE.get(docId);
            if (callback != null) MAIN.post(() -> callback.onData(old != null ? old.data : new HashMap<>(), false));
        });
    }

    private static String sanitizeKey(String raw) {
        return raw.replaceAll("[^a-zA-Z0-9_\\-.]", "_").toLowerCase(java.util.Locale.US);
    }
}
