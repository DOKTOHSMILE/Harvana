package com.harvana.app.social;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.harvana.app.notify.NotificationCenter;
import com.harvana.app.trust.ProfileTrust;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * A pinch is a light nudge to a friend: "I want something, come and check." It is one small document per sender and
 * person, PINCHES/{them}/from/{me}, so a person can only ever be pinched once an hour by the same friend. The receiver's
 * phone picks it up with its other notifications and shows who pinched. The name is looked up from the sender's
 * profile, never taken from the pinch itself, so a pinch cannot pose as someone else.
 */
public final class PinchStore {
    private PinchStore() {}

    public interface Cb { void done(boolean ok, String error); }

    private static final Executor BG = Executors.newCachedThreadPool();
    private static final long T = 20;
    public static final long COOLDOWN_MS = 60L * 60 * 1000;

    private static FirebaseFirestore db() { return FirebaseFirestore.getInstance(); }
    private static String me() { return FirebaseAuth.getInstance().getCurrentUser() == null ? "" : FirebaseAuth.getInstance().getCurrentUser().getUid(); }

    /** Milliseconds until {@code uid} can be pinched again from this phone; 0 when they can be pinched now. */
    public static long waitMs(Context c, String uid) {
        long last = c.getApplicationContext().getSharedPreferences("pinch_sent", Context.MODE_PRIVATE).getLong(me() + "_" + uid, 0L);
        return Math.max(0L, last + COOLDOWN_MS - System.currentTimeMillis());
    }

    public static void send(Context context, Person to, Cb cb) {
        final Context c = context.getApplicationContext();
        final String uid = me();
        if (uid.isEmpty() || to.uid.isEmpty() || uid.equals(to.uid)) { BG.execute(() -> cb.done(false, "You cannot pinch yourself.")); return; }
        long wait = waitMs(c, to.uid);
        if (wait > 0) { BG.execute(() -> cb.done(false, "You pinched " + to.name + " a moment ago. Try again in " + Math.max(1, wait / 60000L) + " min.")); return; }
        BG.execute(() -> {
            try {
                Map<String, Object> f = new HashMap<>();
                f.put("from", uid);
                f.put("at", FieldValue.serverTimestamp());
                Tasks.await(db().collection("PINCHES").document(to.uid).collection("from").document(uid).set(f), T, TimeUnit.SECONDS);
                c.getSharedPreferences("pinch_sent", Context.MODE_PRIVATE).edit().putLong(uid + "_" + to.uid, System.currentTimeMillis()).apply();
                cb.done(true, null);
            } catch (Exception e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                boolean denied = cause instanceof FirebaseFirestoreException && ((FirebaseFirestoreException) cause).getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED;
                cb.done(false, denied ? "You can pinch someone you follow, once an hour." : "Could not pinch right now.");
            }
        });
    }

    /** Raises a notification for each pinch received since the last look. Called with the other notification checks. */
    public static void poll(Context context, String uid, Runnable done) {
        final Context c = context.getApplicationContext();
        BG.execute(() -> {
            try {
                SharedPreferences sp = c.getSharedPreferences("pinch_seen", Context.MODE_PRIVATE);
                // Only pinches since the last look (with a 10-minute overlap for clock drift); the per-friend check below
                // still drops anything already shown. Before, every run read one document per friend who ever pinched.
                long last = sp.getLong(uid + "__polled", 0L);
                sp.edit().putLong(uid + "__polled", System.currentTimeMillis()).apply();
                com.google.firebase.firestore.Query q = db().collection("PINCHES").document(uid).collection("from");
                if (last > 0) q = q.whereGreaterThan("at", new com.google.firebase.Timestamp(new java.util.Date(last - 10 * 60_000L)));
                for (QueryDocumentSnapshot d : Tasks.await(q.limit(50).get(), T, TimeUnit.SECONDS)) {
                    com.google.firebase.Timestamp ts = d.getTimestamp("at");
                    long at = ts == null ? 0L : ts.toDate().getTime();
                    String key = uid + "_" + d.getId();
                    if (at <= sp.getLong(key, 0L)) continue;
                    sp.edit().putLong(key, at).apply();
                    final String fromUid = d.getId();
                    final long when = at;
                    final CountDownLatch named = new CountDownLatch(1);
                    ProfileTrust.resolve(fromUid, card -> {
                        String who = !card.name.isEmpty() ? card.name : !card.username.isEmpty() ? "@" + card.username : "A friend";
                        NotificationCenter.post(c, NotificationCenter.SOCIAL, "pinch_" + fromUid + "_" + when,
                            who + " pinched you", "Go check what they want.", "", -1);
                        named.countDown();
                    });
                    named.await(10, TimeUnit.SECONDS);
                }
            } catch (Exception ignored) { }
            if (done != null) done.run();
        });
    }
}
