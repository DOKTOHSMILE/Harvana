package com.harvanna.app.identity;

import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.harvanna.app.firestore.UserProfileManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Writes the two documents that split a person from their KYC file.
 * PIDS/{pid} stores only the owner's uid and is unreadable by clients.
 * PROFILES/{pid} is the public card: name, photo, username, tier.
 * The private USERS/{uid} document keeps the ID number, address and next of kin.
 */
public final class IdentityStore {
    private static final String TAG = "IdentityStore";

    public interface Cb {
        void done(String pid, String error);
    }

    private IdentityStore() {}

    /**
     * Gives this account a PID if it does not have a valid one, writes the
     * private link, then the public card. Safe to call again.
     */
    public static void ensure(String uid, Map<String, Object> privateProfile, Cb cb) {
        if (uid == null || uid.isEmpty()) {
            cb.done(null, "Sign in first.");
            return;
        }
        Object raw = privateProfile == null ? null : privateProfile.get("pid");
        String existing = raw == null ? "" : String.valueOf(raw).trim();
        if (Pid.valid(existing)) {
            publish(uid, existing, privateProfile, cb);
            return;
        }
        String pid = Pid.issue();
        Map<String, Object> fields = privateProfile == null ? new HashMap<>() : privateProfile;
        writeLink(uid, pid, fields, new Cb() {
            @Override
            public void done(String issued, String error) {
                if (error != null) {
                    cb.done(null, error);
                    return;
                }
                Map<String, Object> patch = new HashMap<>();
                patch.put("pid", issued);
                UserProfileManager.getInstance(null).updateProfileFields(uid, patch, new UserProfileManager.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        cb.done(issued, null);
                    }
                    @Override
                    public void onError(String err) {
                        cb.done(issued, err);
                    }
                });
            }
        });
    }

    /** Allocates a PID and writes both documents. Does not touch USERS. */
    public static void allocate(String uid, Map<String, Object> privateProfile, Cb cb) {
        if (uid == null || uid.isEmpty()) {
            cb.done(null, "Sign in first.");
            return;
        }
        writeLink(uid, Pid.issue(), privateProfile, cb);
    }

    private static void publish(String uid, String pid, Map<String, Object> privateProfile, Cb cb) {
        writeLink(uid, pid, privateProfile, cb);
    }

    private static void writeLink(String uid, String pid, Map<String, Object> privateProfile, Cb cb) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> link = new HashMap<>();
        link.put("uid", uid);
        db.collection("PIDS").document(pid).set(link).addOnCompleteListener(linkTask -> {
            Map<String, Object> card = publicCard(pid, privateProfile);
            db.collection("PROFILES").document(pid).set(card).addOnCompleteListener(cardTask -> {
                if (cardTask.isSuccessful()) {
                    cb.done(pid, null);
                    return;
                }
                // A repeat publish updates a card that already exists. A first
                // publish fails here when PIDS was rejected (rules not deployed yet).
                Log.w(TAG, "Public card not written: " + message(cardTask.getException()));
                if (linkTask.isSuccessful()) cb.done(pid, message(cardTask.getException()));
                else cb.done(null, message(linkTask.getException()));
            });
        });
    }

    static Map<String, Object> publicCard(String pid, Map<String, Object> src) {
        Map<String, Object> card = new HashMap<>();
        card.put("pid", pid);
        card.put("first_name", text(src, "first_name"));
        card.put("last_name", text(src, "last_name"));
        card.put("username", text(src, "username"));
        card.put("icon", text(src, "icon"));
        card.put("tier", text(src, "tier").isEmpty() ? "2" : text(src, "tier"));
        String trust = text(src, "trust_score");
        card.put("trust_score", trust.isEmpty() ? "30" : trust);
        return card;
    }

    private static String text(Map<String, Object> src, String key) {
        if (src == null || src.get(key) == null) return "";
        String v = String.valueOf(src.get(key)).trim();
        return "null".equals(v) ? "" : v;
    }

    private static String message(Exception e) {
        if (e == null || e.getMessage() == null) return "Couldn't publish your Harvanna ID.";
        return e.getMessage();
    }
}
