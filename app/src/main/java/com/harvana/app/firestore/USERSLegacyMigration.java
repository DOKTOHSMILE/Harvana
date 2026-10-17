package com.harvana.app.firestore;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * One-time migration for legacy accounts whose {@code USERS/{uid}} document still
 * carries the old public/private fields merged together. The split was implemented
 * in {@link UserProfileManager} and enforced in the Firestore rules on 18 Oct
 * 2026, but existing documents still hold private fields in the public doc.
 *
 * This utility finds those leftover fields, copies them to
 * {@code USERS/{uid}/private/kyc} (the owner-only subcollection), and removes
 * them from the public document. Everything happens in one batch per user.
 *
 * Operator usage (one-time, on a phone signed in as moderator or via the Admin
 * SDK on the build host):
 *
 * <pre>
 *   USERSLegacyMigration migration = new USERSLegacyMigration();
 *   migration.runAll(uids, new USERSLegacyMigration.Listener() {
 *       public void onUser(String uid, int moved, String error) { ... }
 *       public void onDone(int users, int fields) { ... }
 *   });
 * </pre>
 *
 * The {@code publicProfileUpdateOnly()} rule in the Firestore file is graceful
 * (it only blocks updates that TOUCH a private field), so this script is not
 * required for the app to keep working — it is the cleaner end-state.
 *
 * Pure Java where possible; the Firestore calls use the native SDK. Idempotent:
 * running it twice moves nothing the second time.
 */
public final class USERSLegacyMigration {

    private static final String TAG = "USERSLegacyMigration";
    private final FirebaseFirestore db;

    public USERSLegacyMigration() {
        this(FirebaseFirestore.getInstance());
    }

    /** Test seam: pass a mock or a custom Firestore instance. */
    public USERSLegacyMigration(FirebaseFirestore db) {
        this.db = db;
    }

    public interface Listener {
        /** Called once per user. {@code moved} is the number of fields copied to private/kyc. */
        void onUser(String uid, int moved, String error);
        /** Called once at the end. {@code users} is the count processed; {@code fields} the total moved. */
        void onDone(int users, int fields);
    }

    /**
     * Run the migration for the given uids. The list is processed sequentially
     * so a single bad document does not blow up the whole batch and so the
     * listener gets a clean per-user report. Use {@code runOne} from a debug
     * screen for a single account.
     */
    public void runAll(java.util.List<String> uids, Listener listener) {
        if (uids == null || uids.isEmpty()) { if (listener != null) listener.onDone(0, 0); return; }
        final int[] totalUsers = {0};
        final int[] totalFields = {0};
        runSequentially(uids, 0, totalUsers, totalFields, listener);
    }

    private void runSequentially(java.util.List<String> uids, int idx, int[] totalUsers, int[] totalFields, Listener listener) {
        if (idx >= uids.size()) { if (listener != null) listener.onDone(totalUsers[0], totalFields[0]); return; }
        String uid = uids.get(idx);
        runOne(uid, (moved, error) -> {
            if (moved > 0) totalFields[0] += moved;
            totalUsers[0] += 1;
            if (listener != null) listener.onUser(uid, moved, error);
            runSequentially(uids, idx + 1, totalUsers, totalFields, listener);
        });
    }

    /**
     * Run the migration for a single user. Returns the number of fields moved,
     * or -1 on error. Idempotent: if there is nothing to move, returns 0.
     */
    public void runOne(final String uid, final ResultCallback cb) {
        if (uid == null || uid.isEmpty()) { cb.onResult(-1, "uid is empty"); return; }
        DocumentReference pub = db.collection("USERS").document(uid);
        pub.get().addOnSuccessListener(snap -> {
            try {
                int moved = migrate(snap, pub);
                cb.onResult(moved, null);
            } catch (Exception e) {
                Log.e(TAG, "migrate failed for " + uid, e);
                cb.onResult(-1, String.valueOf(e.getMessage()));
            }
        }).addOnFailureListener(e -> {
            Log.w(TAG, "read failed for " + uid + ": " + e.getMessage());
            cb.onResult(-1, String.valueOf(e.getMessage()));
        });
    }

    /** Test seam: synchronous version of the migration that takes a snapshot. */
    int migrate(DocumentSnapshot snap, DocumentReference pub) {
        if (snap == null || !snap.exists()) return 0;
        Map<String, Object> data = snap.getData();
        if (data == null) return 0;
        Set<String> leftover = PrivateProfile.leftoversIn(data);
        if (leftover.isEmpty()) return 0;

        // Build the private patch: every leftover field and its current value.
        Map<String, Object> privPatch = new HashMap<>();
        for (String f : leftover) privPatch.put(f, data.get(f));

        // Build the public patch: remove the leftover fields.
        Map<String, Object> pubPatch = new HashMap<>();
        for (String f : leftover) pubPatch.put(f, FieldValue.delete());

        // Write both in one batch so we never end up with the private fields
        // in neither doc (or in both).
        WriteBatch batch = db.batch();
        batch.set(pub.collection("private").document(PrivateProfile.DOCUMENT), privPatch, com.google.firebase.firestore.SetOptions.merge());
        batch.update(pub, pubPatch);
        Task<Void> commit = batch.commit();
        try {
            Tasks.await(commit);
        } catch (java.util.concurrent.ExecutionException e) {
            throw new IllegalStateException(e.getCause() == null ? e : e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
        return leftover.size();
    }

    /** Convenience: return the list of fields that would be moved for a snapshot, without writing. */
    public static java.util.Set<String> preview(DocumentSnapshot snap) {
        if (snap == null || !snap.exists()) return java.util.Collections.emptySet();
        return PrivateProfile.leftoversIn(snap.getData());
    }

    public interface ResultCallback {
        /** {@code moved} is the number of fields copied (0 = nothing to do), or -1 on error. */
        void onResult(int moved, String error);
    }

    /**
     * Build the list of uids to migrate. Iterates every {@code USERS/{uid}} in
     * the database; the caller is responsible for running this off the main
     * thread and for rate-limiting (the Firestore SDK queues writes, but a
     * huge account base can spike reads).
     */
    public void listAllUids(UidsCallback cb) {
        db.collection("USERS").get().addOnSuccessListener(snap -> {
            java.util.List<String> out = new java.util.ArrayList<>();
            for (DocumentSnapshot d : snap.getDocuments()) out.add(d.getId());
            cb.onUids(out);
        }).addOnFailureListener(e -> cb.onUids(null));
    }

    public interface UidsCallback {
        void onUids(java.util.List<String> uids);
    }
}
