package com.harvana.app.trust;

import android.content.Context;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * What the member did inside the app. This is the Firestore half of the trust
 * record. It does not change the 0–26 score. The network half is written by the node.
 */
public final class AppActivity {
    private AppActivity() {}

    public static final class Event {
        public final String title;
        public final long at;
        public final String source;
        public Event(String title, long at, String source) {
            this.title = title == null ? "" : title;
            this.at = at;
            this.source = source == null ? "app" : source;
        }
    }

    public interface ListCallback { void onEvents(List<Event> events); }

    /** Best-effort. A failed write never blocks the action that just succeeded. */
    public static void note(Context c, String kind, String title) {
        try {
            FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
            if (u == null || title == null || title.isEmpty()) return;
            String k = kind == null ? "APP" : kind;
            if (k.length() > 32) k = k.substring(0, 32);
            String t = title.length() > 80 ? title.substring(0, 80) : title;
            Map<String, Object> row = new HashMap<>();
            row.put("kind", k);
            row.put("title", t);
            row.put("detail", "");
            row.put("at", System.currentTimeMillis());
            row.put("source", "app");
            FirebaseFirestore.getInstance().collection("USERS").document(u.getUid())
                    .collection("ACTIVITY").add(row);
        } catch (Exception ignored) { }
    }

    public static void recent(ListCallback cb) {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null) { cb.onEvents(new ArrayList<>()); return; }
        FirebaseFirestore.getInstance().collection("USERS").document(u.getUid())
                .collection("ACTIVITY").orderBy("at", Query.Direction.DESCENDING).limit(20)
                .get()
                .addOnSuccessListener(snap -> {
                    List<Event> out = new ArrayList<>();
                    if (snap != null) {
                        for (com.google.firebase.firestore.DocumentSnapshot d : snap.getDocuments()) {
                            Object at = d.get("at");
                            out.add(new Event(d.getString("title"), at instanceof Number ? ((Number) at).longValue() : 0L, "app"));
                        }
                    }
                    cb.onEvents(out);
                })
                .addOnFailureListener(e -> cb.onEvents(new ArrayList<>()));
    }

    /** Network lines stored as {@code millis|title} on the reputation copy. */
    public static List<Event> fromNetwork(Object raw) {
        List<Event> out = new ArrayList<>();
        if (!(raw instanceof List)) return out;
        for (Object row : (List<?>) raw) {
            String line = String.valueOf(row);
            int bar = line.indexOf('|');
            if (bar <= 0) continue;
            long at = 0L;
            try { at = Long.parseLong(line.substring(0, bar)); } catch (NumberFormatException ignored) { }
            out.add(new Event(line.substring(bar + 1), at, "network"));
        }
        return out;
    }
}
