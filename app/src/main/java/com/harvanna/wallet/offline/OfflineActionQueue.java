package com.harvanna.wallet.offline;

import android.content.Context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Queue of writes that were attempted while offline (or failed with a
 * genuine connectivity error) and need to be replayed once the network is
 * back. Persisted through OfflineCacheManager (encrypted — a queued action
 * can contain amounts, wallet addresses, memos), so a queued action
 * survives an app restart, not just a screen rotation.
 *
 * This is deliberately generic — it stores a "type" tag and a JSON payload,
 * not typed request objects — because the app has many different kinds of
 * writes (contribution payments, loan repayments, notices, join requests)
 * and a generic queue plus a small per-type handler is far less code than
 * a bespoke queue per feature. To make a write offline-safe:
 *
 *   1. When the real call fails and NetworkStatus.isConnectivityError(error)
 *      is true, call OfflineActionQueue.enqueue(context, "CONTRIBUTION",
 *      payloadMap) instead of just showing the error.
 *   2. Register a handler for that type (see ActionHandler) — normally
 *      just the same code that would have run on a successful tap, given
 *      the queued payload back.
 *   3. Call OfflineActionQueue.flush(context, handlerRegistry) when
 *      connectivity returns — MainActivity is the natural place to check
 *      this on resume and whenever a connectivity-change broadcast fires.
 */
public final class OfflineActionQueue {

    private static final String QUEUE_KEY = "pending_actions_v1";

    private OfflineActionQueue() {}

    public interface ActionHandler {
        /** Replay this action. Call back onSuccess() once truly persisted
         *  server-side, or onError(error) if it fails again (including
         *  another connectivity error, in which case the queue stops
         *  flushing for this run rather than burning through retries). */
        void execute(Context context, Map<String, Object> payload, FlushCallback callback);
    }

    public interface FlushCallback {
        void onSuccess();
        void onError(String error);
    }

    /** Registry the caller builds once (usually in MainActivity) mapping
     *  an action type string to the code that knows how to replay it. */
    public interface HandlerRegistry {
        ActionHandler handlerFor(String type);
    }

    public static class PendingAction {
        public final String id;
        public final String type;
        public final Map<String, Object> payload;
        public final long queuedAtMs;
        public final int attempts;

        PendingAction(String id, String type, Map<String, Object> payload, long queuedAtMs, int attempts) {
            this.id = id;
            this.type = type;
            this.payload = payload;
            this.queuedAtMs = queuedAtMs;
            this.attempts = attempts;
        }
    }

    /** Queues an action for later replay. Returns the generated id. */
    public static String enqueue(Context context, String type, Map<String, Object> payload) {
        String id = UUID.randomUUID().toString();
        List<HashMap<String, Object>> pending = _loadRaw(context);
        HashMap<String, Object> entry = new HashMap<>();
        entry.put("id", id);
        entry.put("type", type);
        entry.put("payload", payload != null ? payload : new HashMap<>());
        entry.put("queuedAtMs", System.currentTimeMillis());
        entry.put("attempts", 0);
        pending.add(entry);
        OfflineCacheManager.saveList(context, QUEUE_KEY, pending);
        return id;
    }

    public static List<PendingAction> getPending(Context context) {
        List<HashMap<String, Object>> raw = _loadRaw(context);
        List<PendingAction> result = new ArrayList<>();
        for (HashMap<String, Object> entry : raw) {
            result.add(_toPendingAction(entry));
        }
        return result;
    }

    public static int pendingCount(Context context) {
        return _loadRaw(context).size();
    }

    public static void remove(Context context, String id) {
        List<HashMap<String, Object>> pending = _loadRaw(context);
        List<HashMap<String, Object>> kept = new ArrayList<>();
        for (HashMap<String, Object> entry : pending) {
            if (!id.equals(String.valueOf(entry.get("id")))) kept.add(entry);
        }
        OfflineCacheManager.saveList(context, QUEUE_KEY, kept);
    }

    /**
     * Replays every queued action in order, oldest first. Stops early (and
     * leaves the remaining actions queued) the moment one fails with a
     * genuine connectivity error, on the assumption the network dropped
     * again mid-flush — no point burning through the rest of the queue
     * against a dead connection. A non-connectivity failure (the server
     * genuinely rejected the action) removes it from the queue and moves
     * on, rather than retrying something that will never succeed.
     */
    public static void flush(Context context, HandlerRegistry registry, Runnable onDone) {
        if (!NetworkStatus.isOnline(context)) {
            if (onDone != null) onDone.run();
            return;
        }
        List<PendingAction> pending = getPending(context);
        _flushNext(context, registry, pending, 0, onDone);
    }

    private static void _flushNext(Context context, HandlerRegistry registry,
                                   List<PendingAction> pending, int index, Runnable onDone) {
        if (index >= pending.size()) {
            if (onDone != null) onDone.run();
            return;
        }
        PendingAction action = pending.get(index);
        ActionHandler handler = registry != null ? registry.handlerFor(action.type) : null;
        
        if (handler == null) {
            // No handler registered for this type (yet) — leave it queued,
            // move on to the next action rather than losing it.
            _flushNext(context, registry, pending, index + 1, onDone);
            return;
        }
        
        handler.execute(context, action.payload, new FlushCallback() {
            @Override
            public void onSuccess() {
                remove(context, action.id);
                _flushNext(context, registry, pending, index + 1, onDone);
            }

            @Override
            public void onError(String error) {
                if (NetworkStatus.isConnectivityError(error)) {
                    // Still offline — stop here, keep everything from this
                    // point on queued for the next flush attempt.
                    if (onDone != null) onDone.run();
                    return;
                }
                // Real rejection (validation failed, etc.) — this action
                // will never succeed as-is. Drop it rather than retrying
                // forever; the underlying screen already showed the error
                // at enqueue time.
                remove(context, action.id);
                _flushNext(context, registry, pending, index + 1, onDone);
            }
        });
    }

    // ── storage helpers ──────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private static List<HashMap<String, Object>> _loadRaw(Context context) {
        List<HashMap<String, Object>> loaded = OfflineCacheManager.loadList(context, QUEUE_KEY);
        return loaded != null ? loaded : new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    private static PendingAction _toPendingAction(HashMap<String, Object> entry) {
        String id = String.valueOf(entry.get("id"));
        String type = String.valueOf(entry.get("type"));
        Object payloadObj = entry.get("payload");
        Map<String, Object> payload = payloadObj instanceof Map
                ? (Map<String, Object>) payloadObj : new HashMap<>();
        long queuedAtMs = entry.get("queuedAtMs") instanceof Number
                ? ((Number) entry.get("queuedAtMs")).longValue() : 0L;
        int attempts = entry.get("attempts") instanceof Number
                ? ((Number) entry.get("attempts")).intValue() : 0;
        return new PendingAction(id, type, payload, queuedAtMs, attempts);
    }
}
