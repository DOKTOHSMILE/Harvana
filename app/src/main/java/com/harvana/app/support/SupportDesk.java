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
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { cb.fail("Sign in first."); return; }
        String address = WalletManager.getActiveWalletAddress(c);
        if (address == null) address = "";
        long now = System.currentTimeMillis();
        Map<String, Object> ticket = new HashMap<>();
        ticket.put("ownerUid", user.getUid());
        ticket.put("ownerAddress", address);
        ticket.put("subject", subject);
        ticket.put("status", "OPEN");
        ticket.put("lastMessage", body);
        ticket.put("updatedAt", now);
        ticket.put("createdAt", now);
        FirebaseFirestore.getInstance().collection("SUPPORT_TICKETS").add(ticket)
                .addOnSuccessListener(ref -> {
                    Map<String, Object> msg = message("MEMBER", user.getUid(), "", body, now);
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
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { cb.fail("Sign in first."); return; }
        long now = System.currentTimeMillis();
        Map<String, Object> patch = new HashMap<>();
        patch.put("lastMessage", body);
        patch.put("updatedAt", now);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("SUPPORT_TICKETS").document(ticketId).update(patch)
                .addOnSuccessListener(v -> db.collection("SUPPORT_TICKETS").document(ticketId)
                        .collection("messages").add(message("MEMBER", user.getUid(), "", body, now))
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
                            messages.put(new JSONObject()
                                    .put("from", "STAFF".equals(from) ? (name == null || name.isEmpty() ? "Support" : name) : "You")
                                    .put("body", String.valueOf(doc.get("body"))));
                        }
                        cb.ok(new JSONObject().put("messages", messages));
                    } catch (Exception ex) {
                        cb.fail("Could not read this conversation.");
                    }
                });
    }

    private static Map<String, Object> message(String from, String uid, String staffName, String body, long at) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("from", from);
        msg.put("uid", uid);
        msg.put("body", body);
        msg.put("at", at);
        if (staffName != null && !staffName.isEmpty()) msg.put("staffName", staffName);
        return msg;
    }
}
