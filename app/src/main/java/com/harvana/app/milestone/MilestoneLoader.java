package com.harvana.app.milestone;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Collects a member's milestone counts: from the node (confirmed activity), from Learn Hub documents (posts, handshakes,
 * comments, views) and, for the owner only, from this phone. Calls back on the main thread with every catalog milestone.
 */
public final class MilestoneLoader {
    private MilestoneLoader() {}

    public static final class Item {
        public final MilestoneCatalog.Def def;
        public final long count;
        public final int tier;
        Item(MilestoneCatalog.Def def, long count) { this.def = def; this.count = count; this.tier = MilestoneCatalog.tierFor(def, count); }
        public long nextTarget() { return tier < def.tiers.length ? def.tiers[tier] : 0L; }
    }

    public interface Callback { void onItems(List<Item> items); }

    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final long TTL_MS = 2 * 60 * 1000L;
    private static final Map<String, List<Item>> CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Long> CACHED_AT = new ConcurrentHashMap<>();
    /** A post with at least this many handshakes counts toward Master Writer. */
    public static final int ENGAGING_POST_HANDSHAKES = 5;

    /** @param mine true on the owner's own shelf: adds the phone-only milestones. */
    public static void load(Context ctx, String wallet, String uid, boolean mine, Callback cb) {
        final Context c = ctx.getApplicationContext();
        final String key = (wallet == null ? "" : wallet) + "|" + (uid == null ? "" : uid) + "|" + mine;
        Long at = CACHED_AT.get(key);
        if (at != null && System.currentTimeMillis() - at < TTL_MS && CACHE.get(key) != null) {
            List<Item> hit = CACHE.get(key);
            MAIN.post(() -> cb.onItems(hit));
            return;
        }
        final Map<String, Long> counts = new ConcurrentHashMap<>();
        final AtomicInteger left = new AtomicInteger(2);
        final Runnable done = () -> {
            if (left.decrementAndGet() != 0) return;
            if (mine) addPhone(c, counts);
            List<Item> items = new ArrayList<>();
            for (MilestoneCatalog.Def d : MilestoneCatalog.all()) {
                if (d.source == MilestoneCatalog.Source.PHONE && !mine) continue;
                Long n = counts.get(d.id);
                items.add(new Item(d, n == null ? 0L : n));
            }
            CACHE.put(key, items);
            CACHED_AT.put(key, System.currentTimeMillis());
            MAIN.post(() -> cb.onItems(items));
        };

        if (wallet == null || wallet.isEmpty()) done.run();
        else com.harvana.app.sync.HarvanaNodeClient.nodeGet("/api/v1/personhood/" + wallet + "/milestones", new com.harvana.app.sync.HarvanaNodeClient.JsonCallback() {
            @Override public void onSuccess(JSONObject json) {
                JSONArray arr = json == null ? null : json.optJSONArray("milestones");
                if (arr != null) for (int i = 0; i < arr.length(); i++) {
                    JSONObject m = arr.optJSONObject(i);
                    if (m != null && MilestoneCatalog.get(m.optString("id")) != null) counts.put(m.optString("id"), m.optLong("count", 0));
                }
                done.run();
            }
            @Override public void onError(String error) { done.run(); }
        });

        if (uid == null || uid.isEmpty()) done.run();
        else loadLearn(uid, counts, done);
    }

    /** Learn Hub writer counts, read from the documents themselves (posts, and the count of each post's reactions and comments). */
    private static void loadLearn(String uid, Map<String, Long> counts, Runnable done) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("LEARN_CONTENT").whereEqualTo("authorUid", uid).limit(60).get().addOnCompleteListener(t -> {
            if (!t.isSuccessful() || t.getResult() == null || t.getResult().isEmpty()) { done.run(); return; }
            List<DocumentSnapshot> posts = t.getResult().getDocuments();
            long views = 0;
            for (DocumentSnapshot p : posts) if (p.get("views") instanceof Number) views += ((Number) p.get("views")).longValue();
            counts.put("storyteller", (long) posts.size());
            counts.put("widely_read", views);
            final long[] handshakes = {0}, comments = {0}, engaging = {0};
            final AtomicInteger pending = new AtomicInteger(posts.size() * 2);
            final Runnable one = () -> {
                if (pending.decrementAndGet() != 0) return;
                counts.put("crowd_favourite", handshakes[0]);
                counts.put("conversation_starter", comments[0]);
                counts.put("master_writer", engaging[0]);
                done.run();
            };
            for (DocumentSnapshot p : posts) {
                p.getReference().collection("reactions").count().get(AggregateSource.SERVER).addOnCompleteListener(r -> {
                    long n = r.isSuccessful() && r.getResult() != null ? r.getResult().getCount() : 0L;
                    synchronized (handshakes) { handshakes[0] += n; if (n >= ENGAGING_POST_HANDSHAKES) engaging[0]++; }
                    one.run();
                });
                p.getReference().collection("comments").count().get(AggregateSource.SERVER).addOnCompleteListener(r -> {
                    long n = r.isSuccessful() && r.getResult() != null ? r.getResult().getCount() : 0L;
                    synchronized (handshakes) { comments[0] += n; }
                    one.run();
                });
            }
        });
    }

    private static void addPhone(Context c, Map<String, Long> counts) {
        try {
            com.harvana.app.utils.ActivityTracker.Summary s = com.harvana.app.utils.ActivityTracker.summary(c);
            counts.put("regular_visitor", (long) s.sessions);
            counts.put("explorer", (long) s.screens.size());
        } catch (Throwable ignored) { }
    }

    /** Drops cached shelves (after something that may have earned a milestone). */
    public static void invalidate() { CACHE.clear(); CACHED_AT.clear(); }
}
