package com.harvana.app.support;

import android.content.Context;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.harvana.app.wallet.WalletManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/** Support conversations live in Firestore. The member's Firebase account owns the ticket. */
public final class SupportDesk {
    private SupportDesk() {}

    public interface Cb {
        void ok(JSONObject json);
        void fail(String error);
    }

    public static ListenerRegistration mine(Context c, Cb cb) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { cb.fail("Sign in first."); return null; }
        return FirebaseFirestore.getInstance().collection("SUPPORT_TICKETS")
                .whereEqualTo("ownerUid", user.getUid())
                .addSnapshotListener((snap, e) -> {
                    if (e != null) { cb.fail(e.getMessage()); return; }
                    try {
                        JSONArray tickets = new JSONArray();
                        if (snap != null) for (QueryDocumentSnapshot doc : snap) {
                            tickets.put(new JSONObject()
                                    .put("ticketId", doc.getId())
                                    .put("subject", String.valueOf(doc.get("subject")))
                                    .put("lastMessage", String.valueOf(doc.get("lastMessage") == null ? "" : doc.get("lastMessage")))
                                    .put("status", String.valueOf(doc.get("status"))));
                        }
                        cb.ok(new JSONObject().put("tickets", tickets));
                    } catch (Exception ex) {
                        cb.fail("Could not read conversations.");
                    }
                });
    }

    public static void open(Context c, String subject, String body, Cb cb) {
        open(c, subject, body, null, cb);
    }

    public static void open(Context c, String subject, String body, String image, Cb cb) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { cb.fail("Sign in first."); return; }
        boolean photo = image != null && !image.isEmpty();
        if ((body == null || body.isEmpty()) && !photo) { cb.fail("Write a message"); return; }
        String address = WalletManager.getActiveWalletAddress(c);
        if (address == null) address = "";
        long now = System.currentTimeMillis();
        String preview = body == null || body.isEmpty() ? "Photo" : body;
        Map<String, Object> ticket = new HashMap<>();
        ticket.put("ownerUid", user.getUid());
        ticket.put("ownerAddress", address);
        ticket.put("subject", subject);
        ticket.put("status", "OPEN");
        ticket.put("lastMessage", preview);
        ticket.put("lastFrom", "MEMBER");
        ticket.put("updatedAt", now);
        ticket.put("createdAt", now);
        FirebaseFirestore.getInstance().collection("SUPPORT_TICKETS").add(ticket)
                .addOnSuccessListener(ref -> {
                    Map<String, Object> msg = message("MEMBER", user.getUid(), "", body == null ? "" : body, now, image);
                    ref.collection("messages").add(msg)
                            .addOnSuccessListener(m -> {
                                try { cb.ok(new JSONObject().put("ticketId", ref.getId())); }
                                catch (Exception ex) { cb.fail("Sent, but the screen could not refresh."); }
                            })
                            .addOnFailureListener(err -> cb.fail(err.getMessage()));
                })
                .addOnFailureListener(err -> cb.fail(err.getMessage()));
    }

    public static void send(Context c, String ticketId, String body, Cb cb) {
        send(c, ticketId, body, null, cb);
    }

    public static void send(Context c, String ticketId, String body, String image, Cb cb) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { cb.fail("Sign in first."); return; }
        boolean photo = image != null && !image.isEmpty();
        if ((body == null || body.isEmpty()) && !photo) { cb.fail("Write a message"); return; }
        long now = System.currentTimeMillis();
        String preview = body == null || body.isEmpty() ? "Photo" : body;
        Map<String, Object> patch = new HashMap<>();
        patch.put("lastMessage", preview);
        patch.put("lastFrom", "MEMBER");
        patch.put("updatedAt", now);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("SUPPORT_TICKETS").document(ticketId).update(patch)
                .addOnSuccessListener(v -> db.collection("SUPPORT_TICKETS").document(ticketId)
                        .collection("messages").add(message("MEMBER", user.getUid(), "", body == null ? "" : body, now, image))
                        .addOnSuccessListener(m -> {
                            try { cb.ok(new JSONObject().put("ticketId", ticketId)); }
                            catch (Exception ex) { cb.fail("Sent."); }
                        })
                        .addOnFailureListener(err -> cb.fail(err.getMessage())))
                .addOnFailureListener(err -> cb.fail(err.getMessage()));
    }

    public static ListenerRegistration thread(String ticketId, Cb cb) {
        return FirebaseFirestore.getInstance().collection("SUPPORT_TICKETS").document(ticketId)
                .collection("messages").orderBy("at")
                .addSnapshotListener((snap, e) -> {
                    if (e != null) { cb.fail(e.getMessage()); return; }
                    try {
                        JSONArray messages = new JSONArray();
                        if (snap != null) for (QueryDocumentSnapshot doc : snap) {
                            String from = String.valueOf(doc.get("from"));
                            String name = doc.getString("staffName");
                            String image = doc.getString("image");
                            messages.put(new JSONObject()
                                    .put("id", doc.getId())
                                    .put("side", from)
                                    .put("from", "STAFF".equals(from) ? (name == null || name.isEmpty() ? "Support" : name) : "You")
                                    .put("body", String.valueOf(doc.get("body") == null ? "" : doc.get("body")))
                                    .put("image", image == null ? "" : image)
                                    .put("at", doc.get("at") == null ? 0 : doc.get("at")));
                        }
                        cb.ok(new JSONObject().put("messages", messages));
                    } catch (Exception ex) {
                        cb.fail("Could not read this conversation.");
                    }
                });
    }

    private static Map<String, Object> message(String from, String uid, String staffName, String body, long at, String image) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("from", from);
        msg.put("uid", uid);
        msg.put("body", body == null ? "" : body);
        msg.put("at", at);
        if (staffName != null && !staffName.isEmpty()) msg.put("staffName", staffName);
        if (image != null && !image.isEmpty()) msg.put("image", image);
        return msg;
    }

    private static com.google.firebase.firestore.ListenerRegistration watchReg;
    private static final java.util.Map<String, Long> seenTicket = new java.util.HashMap<>();
    private static boolean watchPrimed;

    /** Notifies this phone when support replies, including after the chat screen is closed. */
    public static void watchReplies(Context c) {
        Context app = c.getApplicationContext();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || watchReg != null) return;
        watchReg = FirebaseFirestore.getInstance().collection("SUPPORT_TICKETS")
                .whereEqualTo("ownerUid", user.getUid())
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;
                    boolean first = !watchPrimed;
                    watchPrimed = true;
                    for (QueryDocumentSnapshot doc : snap) {
                        long at = doc.getLong("updatedAt") == null ? 0L : doc.getLong("updatedAt");
                        Long prev = seenTicket.put(doc.getId(), at);
                        if (first || prev == null || at <= prev) continue;
                        if (!"STAFF".equals(String.valueOf(doc.get("lastFrom")))) continue;
                        String preview = String.valueOf(doc.get("lastMessage") == null ? "New reply" : doc.get("lastMessage"));
                        com.harvana.app.notify.NotificationCenter.post(app, com.harvana.app.notify.NotificationCenter.SYSTEM,
                                "support|" + doc.getId() + "|" + at, "Support", preview, "", -1);
                    }
                });
    }
}
