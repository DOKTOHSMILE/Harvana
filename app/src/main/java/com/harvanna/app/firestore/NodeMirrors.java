package com.harvanna.app.firestore;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.Map;

/** Firestore copies the node writes after it has validated them. The app listens. It does not calculate these. */
public final class NodeMirrors {
    private NodeMirrors() {}

    public interface Doc { void onDoc(Map<String, Object> data); }

    public static ListenerRegistration wallet(String address, Doc cb) {
        return FirebaseFirestore.getInstance().collection("REPUTATION").document(address)
                .addSnapshotListener((snap, e) -> cb.onDoc(e != null || snap == null || !snap.exists() ? null : snap.getData()));
    }

    public static ListenerRegistration group(String groupId, Doc cb) {
        return FirebaseFirestore.getInstance().collection("GROUP_TRUST").document(groupId)
                .addSnapshotListener((snap, e) -> cb.onDoc(e != null || snap == null || !snap.exists() ? null : snap.getData()));
    }
}
