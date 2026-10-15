package com.harvana.app.p2p;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.harvana.app.notify.NotificationCenter;
import com.harvana.app.trust.ProfileTrust;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * The small chat on a P2P trade, so the two people can sort out payment details without leaving Harvana (where a scammer
 * would take them to an app nobody can look into).
 *
 *   P2P_CHATS/{orderId}_{uidA}_{uidB}            members [A, B] then a moderator uid once the trade is appealed
 *   P2P_CHATS/{...}/messages/{id}                from, text, at, image (optional picture), file + fileName (optional PDF)
 *
 * The id carries both people, so nobody can take over the chat of a trade they are not in by creating its document first.
 * Messages are kept for 30 days after the last one (so a dispute can be shown what was said), and the first participant to open
 * the chat after that deletes it: there is no server job to do it.
 */
public final class P2pChatStore {
    private P2pChatStore() {}

    public static final long KEEP_MS = 30L * 24 * 60 * 60 * 1000;
    public static final int MAX_TEXT = 500;

    public static final class Message {
        public String id = "", from = "", text = "", image = "", file = "", fileName = "";
        public long at;
    }

    public interface Listener { void onMessages(List<Message> messages); }
    public interface Done { void done(boolean ok, String error); }

    private static final Executor BG = Executors.newCachedThreadPool();

    private static FirebaseFirestore db() { return FirebaseFirestore.getInstance(); }
    public static String me() { return FirebaseAuth.getInstance().getCurrentUser() == null ? "" : FirebaseAuth.getInstance().getCurrentUser().getUid(); }

    /** The chat id for a trade between two people. Same on both phones. */
    public static String chatId(String orderId, String uidA, String uidB) {
        String[] pair = sortedPair(uidA, uidB);
        return orderId + "_" + pair[0] + "_" + pair[1];
    }

    static String[] sortedPair(String a, String b) {
        String[] p = {a, b};
        Arrays.sort(p);
        return p;
    }

    /** Creates the chat the first time either person opens it (nothing happens if it already exists). */
    public static void ensure(String orderId, String other, Done cb) {
        final String me = me();
        if (me.isEmpty() || other == null || other.isEmpty() || me.equals(other)) { BG.execute(() -> cb.done(false, "The other person is not known yet.")); return; }
        final String id = chatId(orderId, me, other);
        BG.execute(() -> {
            try {
                // A chat that is not there yet cannot be read: the rules look at its members, and a missing
                // document has none, so that read is refused. That refusal is not a failure. Create it.
                DocumentSnapshot existing = read(id);
                if (existing != null && existing.exists()) {
                    Long exp = existing.getLong("expiresAt");
                    if (exp == null || exp >= System.currentTimeMillis()) { cb.done(true, null); return; }
                    purge(id);
                }
                create(id, me, other);
                cb.done(true, null);
            } catch (Exception e) {
                DocumentSnapshot again = read(id);
                if (again != null && again.exists()) { cb.done(true, null); return; }
                cb.done(false, fail(e, "Could not open the chat."));
            }
        });
    }

    /** Null when the read is refused (including a chat that does not exist yet). */
    private static DocumentSnapshot read(String id) {
        try { return Tasks.await(db().collection("P2P_CHATS").document(id).get(), 20, TimeUnit.SECONDS); }
        catch (Exception e) { return null; }
    }

    private static void create(String id, String me, String other) throws Exception {
        String[] pair = sortedPair(me, other);
        long now = System.currentTimeMillis();
        Map<String, Object> f = new HashMap<>();
        f.put("members", Arrays.asList(pair[0], pair[1]));
        f.put("createdAt", now);
        f.put("lastAt", now);
        f.put("lastFrom", "");
        f.put("expiresAt", now + KEEP_MS);
        Tasks.await(db().collection("P2P_CHATS").document(id).set(f), 20, TimeUnit.SECONDS);
    }

    /** A sentence for the screen. Never the exception text (it names hosts and classes). */
    private static String fail(Exception e, String fallback) {
        StringBuilder blob = new StringBuilder();
        for (Throwable t = e; t != null; t = t.getCause()) blob.append(' ').append(t.getClass().getName()).append(' ').append(t.getMessage());
        String u = blob.toString().toUpperCase(Locale.ROOT);
        if (u.contains("PERMISSION_DENIED")) return "The database refused this. Publish the latest Firestore rules, then try again.";
        if (u.contains("UNAUTHENTICATED")) return "Sign in again and try once more.";
        if (u.contains("UNAVAILABLE") || u.contains("NETWORK") || u.contains("OFFLINE") || u.contains("UNABLE TO RESOLVE"))
            return "No connection. Check your internet and try again.";
        return fallback;
    }

    /** Live messages, oldest first. Remove the returned registration when the screen closes. */
    public static ListenerRegistration listen(String orderId, String other, Listener l) {
        return listenId(chatId(orderId, me(), other), l);
    }

    public static ListenerRegistration listenId(String id, Listener l) {
        return db().collection("P2P_CHATS").document(id).collection("messages").orderBy("at", Query.Direction.ASCENDING).limit(300)
                .addSnapshotListener((snap, err) -> {
                    if (snap == null) return;
                    List<Message> out = new ArrayList<>();
                    for (QueryDocumentSnapshot d : snap) {
                        Message m = new Message();
                        m.id = d.getId();
                        m.from = d.getString("from") == null ? "" : d.getString("from");
                        m.text = d.getString("text") == null ? "" : d.getString("text");
                        m.image = d.getString("image") == null ? "" : d.getString("image");
                        m.file = d.getString("file") == null ? "" : d.getString("file");
                        m.fileName = d.getString("fileName") == null ? "" : d.getString("fileName");
                        Timestamp ts = d.getTimestamp("at");
                        m.at = ts == null ? System.currentTimeMillis() : ts.toDate().getTime();
                        out.add(m);
                    }
                    l.onMessages(out);
                });
    }

    public interface MetaListener { void onMeta(String moderatorUid, boolean appeal); }

    /** The chat head: whether an appeal is open and which moderator was added. */
    public static ListenerRegistration listenMeta(String id, MetaListener l) {
        return db().collection("P2P_CHATS").document(id).addSnapshotListener((snap, err) -> {
            String mod = "";
            boolean appeal = false;
            if (snap != null && snap.exists()) {
                String m = snap.getString("moderator");
                mod = m == null ? "" : m;
                Boolean a = snap.getBoolean("appeal");
                appeal = a != null && a;
            }
            l.onMeta(mod, appeal);
        });
    }

    /**
     * Marks the trade appealed and, when a moderator account is on duty, adds that person as the third member.
     * The chat id does not change, so the two traders and the moderator share one thread.
     */
    public static void openAppeal(String orderId, String other, Done cb) {
        ensure(orderId, other, (ok, err) -> {
            if (!ok) { if (cb != null) cb.done(false, err); return; }
            admit(chatId(orderId, me(), other), orderId, true, cb);
        });
    }

    /** If this chat was already marked appealed and still has only the two traders, add a moderator now. */
    public static void admitIfAppealed(String orderId, String other) {
        admit(chatId(orderId, me(), other), orderId, false, (ok, err) -> { });
    }

    private static void admit(String id, String orderId, boolean forceAppeal, Done cb) {
        BG.execute(() -> {
            try {
                DocumentSnapshot doc = read(id);
                if (doc == null || !doc.exists()) { if (cb != null) cb.done(false, "Could not open the chat."); return; }
                boolean appealed = Boolean.TRUE.equals(doc.getBoolean("appeal")) || forceAppeal;
                if (!appealed) { if (cb != null) cb.done(true, null); return; }
                String existingMod = doc.getString("moderator");
                if (existingMod != null && !existingMod.isEmpty()) { if (cb != null) cb.done(true, null); return; }
                List<String> members = new ArrayList<>();
                Object raw = doc.get("members");
                if (raw instanceof List) for (Object o : (List<?>) raw) if (o != null) members.add(String.valueOf(o));
                if (members.size() >= 3) { if (cb != null) cb.done(true, null); return; }
                String picked = "";
                for (DocumentSnapshot m : Tasks.await(db().collection("MODERATORS").whereEqualTo("active", true).limit(8).get(), 20, TimeUnit.SECONDS)) {
                    if (members.contains(m.getId())) continue;
                    picked = m.getId();
                    break;
                }
                Map<String, Object> head = new HashMap<>();
                head.put("appeal", true);
                if (orderId != null && !orderId.isEmpty()) head.put("orderId", orderId);
                head.put("lastAt", System.currentTimeMillis());
                boolean joined = false;
                if (!picked.isEmpty() && members.size() == 2) {
                    members.add(picked);
                    head.put("members", members);
                    head.put("moderator", picked);
                    joined = true;
                }
                Tasks.await(db().collection("P2P_CHATS").document(id).update(head), 20, TimeUnit.SECONDS);
                if (joined) sendNow(id, me(), "A Harvana moderator joined this appeal. The three of us can talk here.", null);
                if (cb != null) cb.done(true, null);
            } catch (Exception e) {
                if (cb != null) cb.done(false, fail(e, "Could not add a moderator."));
            }
        });
    }

    /** Opens the appeal thread for the signed-in moderator. The chat id stays the traders' id. */
    public static void openAppealChat(android.app.Activity a, String orderId) {
        final String me = me();
        if (me.isEmpty() || orderId == null || orderId.isEmpty()) return;
        db().collection("P2P_CHATS").whereArrayContains("members", me).limit(40).get()
                .addOnSuccessListener(snap -> {
                    String found = "";
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        String oid = d.getString("orderId");
                        if (orderId.equals(oid) || d.getId().startsWith(orderId + "_")) { found = d.getId(); break; }
                    }
                    if (found.isEmpty()) {
                        android.widget.Toast.makeText(a, "No appeal chat for this order yet.", android.widget.Toast.LENGTH_LONG).show();
                        return;
                    }
                    a.startActivity(new android.content.Intent(a, com.harvana.app.P2pChatActivity.class)
                            .putExtra(com.harvana.app.P2pChatActivity.EXTRA_ORDER, orderId)
                            .putExtra(com.harvana.app.P2pChatActivity.EXTRA_CHAT, found)
                            .putExtra(com.harvana.app.P2pChatActivity.EXTRA_AS_MOD, true)
                            .putExtra(com.harvana.app.P2pChatActivity.EXTRA_NAME, "Appeal"));
                })
                .addOnFailureListener(e -> android.widget.Toast.makeText(a, "Could not open the appeal chat.", android.widget.Toast.LENGTH_LONG).show());
    }

    /** A picture or PDF already uploaded. Everyone in the thread sees it. */
    public static void sendFile(String chatId, String text, String imageUrl, String fileUrl, String fileName, Done cb) {
        final String me = me();
        final String body = text == null || text.trim().isEmpty() ? "Attachment" : text.trim();
        BG.execute(() -> {
            try {
                Map<String, Object> m = new HashMap<>();
                m.put("from", me);
                m.put("text", body.length() > MAX_TEXT ? body.substring(0, MAX_TEXT) : body);
                m.put("at", FieldValue.serverTimestamp());
                if (imageUrl != null && imageUrl.startsWith("https://")) m.put("image", imageUrl);
                if (fileUrl != null && fileUrl.startsWith("https://")) {
                    m.put("file", fileUrl);
                    String name = fileName == null || fileName.trim().isEmpty() ? "Document.pdf" : fileName.trim();
                    if (name.length() > 80) name = name.substring(0, 80);
                    m.put("fileName", name);
                }
                long now = System.currentTimeMillis();
                Map<String, Object> head = new HashMap<>();
                head.put("lastAt", now);
                head.put("lastFrom", me);
                head.put("expiresAt", now + KEEP_MS);
                WriteBatch b = db().batch();
                b.set(db().collection("P2P_CHATS").document(chatId).collection("messages").document(), m);
                b.update(db().collection("P2P_CHATS").document(chatId), head);
                Tasks.await(b.commit(), 20, TimeUnit.SECONDS);
                if (cb != null) cb.done(true, null);
            } catch (Exception e) { if (cb != null) cb.done(false, fail(e, "Could not send the file.")); }
        });
    }

    /** Writes into a known chat. Does not create a second chat, so a moderator stays in the traders' thread. */
    public static void sendTo(String chatId, String text, Done cb) {
        final String t = text == null ? "" : text.trim();
        if (t.isEmpty()) { BG.execute(() -> { if (cb != null) cb.done(false, "Write something first."); }); return; }
        sendNow(chatId, me(), t.length() > MAX_TEXT ? t.substring(0, MAX_TEXT) : t, cb);
    }

    /**
     * A trade event (new order, payment sent, payment confirmed, dispute) written into the trade chat, so the other person is
     * notified on their phone by the usual chat check, and the event stays on record for a dispute. Fire and forget.
     */
    public static void notice(String orderId, String other, String text) {
        if (orderId == null || orderId.isEmpty() || other == null || other.isEmpty()) return;
        send(orderId, other, text, (ok, err) -> { });
    }

    /** A payment proof: the uploaded picture's address as a message both traders see (and a dispute can be shown). */
    public static void sendProof(String orderId, String other, String imageUrl, Done cb) {
        if (imageUrl == null || !imageUrl.startsWith("https://")) { BG.execute(() -> cb.done(false, "The picture did not upload.")); return; }
        ensure(orderId, other, (ok, err) -> {
            if (!ok) { cb.done(false, err); return; }
            final String me = me();
            final String id = chatId(orderId, me, other);
            try {
                Map<String, Object> m = new HashMap<>();
                m.put("from", me);
                m.put("text", "Payment proof");
                m.put("image", imageUrl);
                m.put("at", FieldValue.serverTimestamp());
                long now = System.currentTimeMillis();
                Map<String, Object> head = new HashMap<>();
                head.put("lastAt", now);
                head.put("lastFrom", me);
                head.put("expiresAt", now + KEEP_MS);
                WriteBatch b = db().batch();
                b.set(db().collection("P2P_CHATS").document(id).collection("messages").document(), m);
                b.update(db().collection("P2P_CHATS").document(id), head);
                Tasks.await(b.commit(), 20, TimeUnit.SECONDS);
                cb.done(true, null);
            } catch (Exception e) { cb.done(false, fail(e, "Could not send the proof. Check your connection.")); }
        });
    }

    public interface ProofCb { void onProof(String imageUrl, long at); }

    /** The newest payment proof on a trade, or "" when there is none yet (or the chat does not exist). */
    public static void latestProof(String orderId, String other, ProofCb cb) {
        final String id = chatId(orderId, me(), other);
        BG.execute(() -> {
            try {
                for (DocumentSnapshot d : Tasks.await(db().collection("P2P_CHATS").document(id).collection("messages")
                        .orderBy("at", Query.Direction.DESCENDING).limit(100).get(), 20, TimeUnit.SECONDS).getDocuments()) {
                    String img = d.getString("image");
                    if (img != null && img.startsWith("https://")) {
                        Timestamp ts = d.getTimestamp("at");
                        cb.onProof(img, ts == null ? 0L : ts.toDate().getTime());
                        return;
                    }
                }
            } catch (Exception ignored) { }
            cb.onProof("", 0L);
        });
    }

    public interface ReachListener { void onReach(String myPhone, String theirPhone); }

    /**
     * A phone number this person leaves on the trade, so the other side can call or WhatsApp them if they are offline.
     * Stored as + and digits only. Not their private profile number unless they choose to put that here.
     */
    public static void saveReach(String orderId, String other, String phone, Done cb) {
        final String me = me();
        final String stored = phone == null ? "" : phone;
        if (me.isEmpty() || !stored.matches("\\+[0-9]{8,15}")) {
            BG.execute(() -> cb.done(false, "Enter a phone number with the country code."));
            return;
        }
        ensure(orderId, other, (ok, err) -> {
            if (!ok) { cb.done(false, err == null ? "Could not open the trade." : err); return; }
            BG.execute(() -> {
                try {
                    Map<String, Object> f = new HashMap<>();
                    f.put("phone", stored);
                    f.put("updatedAt", FieldValue.serverTimestamp());
                    Tasks.await(db().collection("P2P_CHATS").document(chatId(orderId, me, other))
                            .collection("reach").document(me).set(f), 20, TimeUnit.SECONDS);
                    cb.done(true, null);
                } catch (Exception e) { cb.done(false, fail(e, "Could not save the number. Check your connection.")); }
            });
        });
    }

    /** Live phone numbers the two people have chosen to share on this trade. */
    public static ListenerRegistration listenReach(String orderId, String other, ReachListener l) {
        final String id = chatId(orderId, me(), other);
        final String me = me();
        return db().collection("P2P_CHATS").document(id).collection("reach")
                .addSnapshotListener((snap, err) -> {
                    String mine = "", theirs = "";
                    if (snap != null) {
                        for (DocumentSnapshot d : snap.getDocuments()) {
                            String phone = d.getString("phone");
                            if (phone == null || phone.isEmpty()) continue;
                            if (d.getId().equals(me)) mine = phone;
                            else theirs = phone;
                        }
                    }
                    l.onReach(mine, theirs);
                });
    }

    public static void send(String orderId, String other, String text, Done cb) {
        final String me = me();
        final String t = text == null ? "" : text.trim();
        if (t.isEmpty()) { BG.execute(() -> cb.done(false, "Write something first.")); return; }
        final String body = t.length() > MAX_TEXT ? t.substring(0, MAX_TEXT) : t;
        final String id = chatId(orderId, me, other);
        // The chat may not exist yet (first message, or the other side has not opened it): create it, then write.
        ensure(orderId, other, (ok, err) -> { if (!ok) { cb.done(false, err == null ? "Could not open the chat." : err); return; } sendNow(id, me, body, cb); });
    }

    private static void sendNow(String id, String me, String body, Done cb) {
        BG.execute(() -> {
            try {
                Map<String, Object> m = new HashMap<>();
                m.put("from", me);
                m.put("text", body);
                m.put("at", FieldValue.serverTimestamp());
                long now = System.currentTimeMillis();
                Map<String, Object> head = new HashMap<>();
                head.put("lastAt", now);
                head.put("lastFrom", me);
                head.put("expiresAt", now + KEEP_MS);
                WriteBatch b = db().batch();
                b.set(db().collection("P2P_CHATS").document(id).collection("messages").document(), m);
                b.update(db().collection("P2P_CHATS").document(id), head);
                Tasks.await(b.commit(), 20, TimeUnit.SECONDS);
                if (cb != null) cb.done(true, null);
            } catch (Exception e) { if (cb != null) cb.done(false, fail(e, "Could not send. Check your connection.")); }
        });
    }

    /** Deletes an expired chat and its messages. Only allowed (by the rules) once the keep time has passed. */
    private static void purge(String id) {
        try {
            // Each message and then the chat is archived for a year (a trade dispute may need it) and deleted.
            WriteBatch b = db().batch();
            int n = 0;
            for (QueryDocumentSnapshot d : Tasks.await(db().collection("P2P_CHATS").document(id).collection("messages").limit(400).get(), 20, TimeUnit.SECONDS)) {
                com.harvana.app.firestore.SoftDelete.archiveAndDelete(b, "P2P_CHATS/messages", d);
                if (++n == 200) { Tasks.await(b.commit(), 20, TimeUnit.SECONDS); b = db().batch(); n = 0; }
            }
            if (n > 0) Tasks.await(b.commit(), 20, TimeUnit.SECONDS);
            Tasks.await(com.harvana.app.firestore.SoftDelete.archiveThenDelete("P2P_CHATS", db().collection("P2P_CHATS").document(id)), 10, TimeUnit.SECONDS);
        } catch (Exception ignored) { }
    }

    /** Raises a notification for each trade chat where the other person wrote last since the last look. Run with the other notification checks. */
    public static void poll(Context context, String uid, Runnable done) {
        final Context c = context.getApplicationContext();
        BG.execute(() -> {
            try {
                SharedPreferences sp = c.getSharedPreferences("p2p_chat_seen", Context.MODE_PRIVATE);
                for (QueryDocumentSnapshot d : Tasks.await(db().collection("P2P_CHATS").whereArrayContains("members", uid).limit(50).get(), 20, TimeUnit.SECONDS)) {
                    String from = d.getString("lastFrom");
                    Long at = d.getLong("lastAt");
                    if (from == null || from.isEmpty() || from.equals(uid) || at == null) continue;
                    String key = uid + "_" + d.getId();
                    if (at <= sp.getLong(key, 0L)) continue;
                    sp.edit().putLong(key, at).apply();
                    final long when = at;
                    final String chat = d.getId();
                    final CountDownLatch named = new CountDownLatch(1);
                    ProfileTrust.resolve(from, card -> {
                        String who = !card.name.isEmpty() ? card.name : !card.username.isEmpty() ? "@" + card.username : "Your trade partner";
                        NotificationCenter.post(c, NotificationCenter.SOCIAL, "p2pchat_" + chat + "_" + when,
                                who + " wrote on your trade", "Open My trades to read it.", "", -1);
                        named.countDown();
                    });
                    named.await(10, TimeUnit.SECONDS);
                }
            } catch (Exception ignored) { }
            if (done != null) done.run();
        });
    }
}
