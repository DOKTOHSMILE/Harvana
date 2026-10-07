package com.harvana.app.firestore;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Map;

/**
 * Deleting keeps a copy for a year. Before any document is deleted, a copy goes to {@code DELETED/{auto}}: what it was
 * ({@code kind}), where it lived ({@code path}), its fields ({@code data}), who deleted it and when, and {@code purgeAt}
 * one year later. The app can write these copies but never read, change or delete them (rules), so deleted data is gone
 * from every screen at once. A Firestore TTL policy on {@code DELETED.purgeAt} removes each copy after its year.
 * See docs/DATA_RETENTION.md.
 */
public final class SoftDelete {
    private SoftDelete() {}

    public static final String COLLECTION = "DELETED";
    public static final long KEEP_MS = 365L * 24 * 60 * 60 * 1000;

    /** The archive record for a document about to be deleted. */
    public static Map<String, Object> record(String kind, DocumentSnapshot d) {
        Map<String, Object> r = new HashMap<>();
        r.put("kind", kind);
        r.put("path", d.getReference().getPath());
        Map<String, Object> data = d.getData();
        r.put("data", data == null ? new HashMap<String, Object>() : data);
        String uid = FirebaseAuth.getInstance().getCurrentUser() == null ? "" : FirebaseAuth.getInstance().getCurrentUser().getUid();
        r.put("deletedBy", uid);
        r.put("deletedAt", FieldValue.serverTimestamp());
        r.put("purgeAt", new Timestamp(new java.util.Date(System.currentTimeMillis() + KEEP_MS)));
        return r;
    }

    /**
     * Reads a document, then archives and deletes it in one batch. A document that is already gone just completes.
     * If the read fails nothing is deleted, so nothing is ever lost without its copy.
     */
    public static com.google.android.gms.tasks.Task<Void> archiveThenDelete(String kind, com.google.firebase.firestore.DocumentReference ref) {
        return ref.get().continueWithTask(t -> {
            if (!t.isSuccessful()) throw t.getException() == null ? new Exception("Couldn't read it") : t.getException();
            DocumentSnapshot d = t.getResult();
            if (d == null || !d.exists()) return com.google.android.gms.tasks.Tasks.forResult(null);
            WriteBatch b = FirebaseFirestore.getInstance().batch();
            archiveAndDelete(b, kind, d);
            return b.commit();
        });
    }

    /** Adds "archive this document, then delete it" to a batch (two writes). */
    public static void archiveAndDelete(WriteBatch batch, String kind, DocumentSnapshot d) {
        if (d == null || !d.exists()) return;
        batch.set(FirebaseFirestore.getInstance().collection(COLLECTION).document(), record(kind, d));
        batch.delete(d.getReference());
    }
}
