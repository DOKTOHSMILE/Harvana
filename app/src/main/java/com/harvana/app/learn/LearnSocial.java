package com.harvana.app.learn;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reactions and comments on a Learn Hub post. Counts are counted from the documents, never from a number a user can write:
 *   LEARN_CONTENT/{id}/reactions/{uid}   one fire per person
 *   LEARN_CONTENT/{id}/comments/{auto}   a comment: uid, text, at
 *   LEARN_CONTENT/{id}/gifts/{txHash}    a gift sent from the reader's wallet: uid, to, amount, txHash, at (one per transfer)
 */
public final class LearnSocial {
    private LearnSocial() {}

    public static final int MAX_COMMENT_CHARS = 500;

    public interface LongCallback { void on(long value); }
    public interface BoolCallback { void on(boolean value); }
    public interface DoneCallback { void on(boolean ok, String error); }
    public static final class Comment {
        public String id = "", uid = "", text = "";
        public long at;
    }
    public interface CommentsCallback { void on(List<Comment> comments); }

    private static CollectionReference reactions(String contentId) {
        return FirebaseFirestore.getInstance().collection("LEARN_CONTENT").document(contentId).collection("reactions");
    }

    private static CollectionReference gifts(String contentId) {
        return FirebaseFirestore.getInstance().collection("LEARN_CONTENT").document(contentId).collection("gifts");
    }

    private static CollectionReference comments(String contentId) {
        return FirebaseFirestore.getInstance().collection("LEARN_CONTENT").document(contentId).collection("comments");
    }

    private static String me() {
        return FirebaseAuth.getInstance().getCurrentUser() == null ? "" : FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    public static void fireCount(String contentId, LongCallback cb) { count(reactions(contentId), cb); }

    public static void commentCount(String contentId, LongCallback cb) { count(comments(contentId), cb); }

    public static void giftCount(String contentId, LongCallback cb) { count(gifts(contentId), cb); }

    /** Records a gift the signed-in user just sent, the same way a reaction is recorded. The document id is the transfer's hash, so it is written once. */
    public static void recordGift(String contentId, String txHash, String amount, String toWallet, DoneCallback cb) {
        String uid = me();
        if (uid.isEmpty() || contentId == null || contentId.isEmpty() || txHash == null || txHash.isEmpty()) { if (cb != null) cb.on(false, "Nothing to record."); return; }
        Map<String, Object> doc = new HashMap<>();
        doc.put("uid", uid);
        doc.put("to", toWallet == null ? "" : toWallet);
        doc.put("amount", amount == null ? "" : amount);
        doc.put("txHash", txHash);
        doc.put("at", System.currentTimeMillis());
        gifts(contentId).document(txHash).set(doc).addOnCompleteListener(t -> { if (cb != null) cb.on(t.isSuccessful(), t.isSuccessful() ? "" : "Could not record the gift."); });
    }

    private static void count(Query q, LongCallback cb) {
        q.count().get(AggregateSource.SERVER).addOnCompleteListener(t -> cb.on(t.isSuccessful() && t.getResult() != null ? t.getResult().getCount() : 0L));
    }

    public static void hasFired(String contentId, BoolCallback cb) {
        String uid = me();
        if (uid.isEmpty()) { cb.on(false); return; }
        reactions(contentId).document(uid).get().addOnCompleteListener(t -> cb.on(t.isSuccessful() && t.getResult() != null && t.getResult().exists()));
    }

    /** Adds or removes the signed-in user's fire. */
    public static void setFire(String contentId, boolean on, DoneCallback cb) {
        String uid = me();
        if (uid.isEmpty()) { cb.on(false, "Sign in first."); return; }
        if (on) {
            Map<String, Object> doc = new HashMap<>();
            doc.put("uid", uid);
            doc.put("at", System.currentTimeMillis());
            reactions(contentId).document(uid).set(doc).addOnCompleteListener(t -> cb.on(t.isSuccessful(), t.isSuccessful() ? "" : "Could not react."));
        } else {
            reactions(contentId).document(uid).delete().addOnCompleteListener(t -> cb.on(t.isSuccessful(), t.isSuccessful() ? "" : "Could not remove the reaction."));
        }
    }

    public static void addComment(String contentId, String text, DoneCallback cb) {
        String uid = me();
        String t = text == null ? "" : text.trim();
        if (uid.isEmpty()) { cb.on(false, "Sign in first."); return; }
        if (t.isEmpty()) { cb.on(false, "Write something first."); return; }
        if (t.length() > MAX_COMMENT_CHARS) t = t.substring(0, MAX_COMMENT_CHARS);
        Map<String, Object> doc = new HashMap<>();
        doc.put("uid", uid);
        doc.put("text", t);
        doc.put("at", System.currentTimeMillis());
        comments(contentId).add(doc).addOnCompleteListener(task -> cb.on(task.isSuccessful(), task.isSuccessful() ? "" : "Could not post your comment."));
    }

    /** Newest first. */
    public static void loadComments(String contentId, int limit, CommentsCallback cb) {
        comments(contentId).orderBy("at", Query.Direction.DESCENDING).limit(limit).get().addOnCompleteListener(t -> {
            List<Comment> out = new ArrayList<>();
            if (t.isSuccessful() && t.getResult() != null) {
                for (com.google.firebase.firestore.DocumentSnapshot d : t.getResult().getDocuments()) {
                    Comment c = new Comment();
                    c.id = d.getId();
                    c.uid = d.getString("uid") == null ? "" : d.getString("uid");
                    c.text = d.getString("text") == null ? "" : d.getString("text");
                    c.at = d.getLong("at") == null ? 0L : d.getLong("at");
                    out.add(c);
                }
            }
            cb.on(out);
        });
    }
}
