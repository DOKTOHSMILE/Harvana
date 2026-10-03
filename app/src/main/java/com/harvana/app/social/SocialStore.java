package com.harvana.app.social;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.Timestamp;
import com.harvana.app.identity.UsernameCadence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Usernames, follow / unfollow, friends lists and contact cards, on the native Firestore SDK.
 *
 * There is deliberately no global member directory: a person is found only by typing their exact username
 * (USERNAMES/{name} can be read one at a time but never listed), and everyone keeps their own list.
 *   USERNAMES/{name}                  -> {uid, name}                    one owner per name
 *   FRIENDS/{me}/list/{other}         -> people I follow                only I can read or write
 *   FOLLOWERS/{other}/list/{me}       -> people who follow {other}      written by the follower, read by {other}
 *   CONTACTS/{uid}                    -> phone + chat links             readable only by the owner and by mutual friends
 * All calls run off the main thread and call back on that background thread.
 */
public final class SocialStore {
    private SocialStore() {}

    public interface Cb<T> { void done(T value, String error); }

    private static final Executor BG = Executors.newCachedThreadPool();
    private static final long T = 20;

    private static FirebaseFirestore db() { return FirebaseFirestore.getInstance(); }
    private static String me() { return FirebaseAuth.getInstance().getCurrentUser() == null ? "" : FirebaseAuth.getInstance().getCurrentUser().getUid(); }

    private static String describe(Throwable t) {
        Throwable c = t.getCause() != null ? t.getCause() : t;
        if (c instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException fe = (FirebaseFirestoreException) c;
            switch (fe.getCode()) {
                case PERMISSION_DENIED: return "Not allowed.";
                case UNAVAILABLE: case DEADLINE_EXCEEDED: return "Network error: check your connection.";
                default: return fe.getMessage() == null ? fe.getCode().name() : fe.getMessage();
            }
        }
        return c.getMessage() == null ? "Something went wrong." : c.getMessage();
    }

    // ── usernames ──────────────────────────────────────────────────────

    /** The signed-in user's current username ("" when none). */
    public static void myUsername(Cb<String> cb) {
        BG.execute(() -> {
            try {
                DocumentSnapshot d = Tasks.await(db().collection("USERS").document(me()).get(), T, TimeUnit.SECONDS);
                Object u = d.get("username");
                cb.done(u == null ? "" : String.valueOf(u), null);
            } catch (Exception e) { cb.done("", describe(e)); }
        });
    }

    /** Claims {@code desired} for the signed-in user (and releases their old one) in one transaction. */
    public static void claimUsername(String desired, Cb<String> cb) {
        final String name = UsernameRules.normalize(desired);
        final String problem = UsernameRules.problem(name);
        if (problem != null) { BG.execute(() -> cb.done(null, problem)); return; }
        final String uid = me();
        if (uid.isEmpty()) { BG.execute(() -> cb.done(null, "Sign in first.")); return; }
        BG.execute(() -> {
            try {
                Tasks.await(db().runTransaction(tx -> {
                    DocumentReference target = db().collection("USERNAMES").document(name);
                    DocumentReference userRef = db().collection("USERS").document(uid);
                    DocumentSnapshot t = tx.get(target);
                    if (t.exists() && !uid.equals(t.getString("uid"))) throw new IllegalStateException("TAKEN");
                    DocumentSnapshot u = tx.get(userRef);
                    String old = u.getString("username");
                    String why = UsernameCadence.problem(old, name, changedAt(u.get("usernameChangedAt")), System.currentTimeMillis());
                    if (why != null) throw new IllegalStateException("WAIT:" + why);
                    if (old != null && !old.isEmpty() && !old.equals(name)) {
                        Map<String, Object> hold = new HashMap<>();
                        hold.put("uid", uid);
                        hold.put("releasedAt", FieldValue.serverTimestamp());
                        tx.set(db().collection("USERNAME_HOLDS").document(old), hold);
                        tx.delete(db().collection("USERNAMES").document(old));
                    }
                    Map<String, Object> claim = new HashMap<>();
                    claim.put("uid", uid); claim.put("name", name); claim.put("claimedAt", System.currentTimeMillis());
                    tx.set(target, claim);
                    Map<String, Object> f = new HashMap<>();
                    f.put("username", name);
                    f.put("usernameChangedAt", FieldValue.serverTimestamp());
                    tx.set(userRef, f, SetOptions.merge());
                    return null;
                }), T, TimeUnit.SECONDS);
                cb.done(name, null);
            } catch (Exception e) {
                Throwable c = e.getCause() != null ? e.getCause() : e;
                if (c instanceof IllegalStateException && c.getMessage() != null && c.getMessage().startsWith("WAIT:")) {
                    cb.done(null, c.getMessage().substring(5));
                } else {
                    cb.done(null, c instanceof IllegalStateException && "TAKEN".equals(c.getMessage()) ? "That username is already taken." : describe(e));
                }
            }
        });
    }

    private static Long changedAt(Object v) {
        if (v instanceof Number) return ((Number) v).longValue();
        if (v instanceof Timestamp) return ((Timestamp) v).toDate().getTime();
        return null;
    }

    /** Finds a member by exact username. */
    public static void findByUsername(String typed, Cb<Person> cb) {
        final String name = UsernameRules.normalize(typed);
        if (UsernameRules.problem(name) != null) { BG.execute(() -> cb.done(null, "No member has that username.")); return; }
        BG.execute(() -> {
            try {
                DocumentSnapshot c = Tasks.await(db().collection("USERNAMES").document(name).get(), T, TimeUnit.SECONDS);
                if (!c.exists()) { cb.done(null, "No member has that username."); return; }
                String uid = c.getString("uid");
                DocumentSnapshot u = Tasks.await(db().collection("USERS").document(uid).get(), T, TimeUnit.SECONDS);
                Map<String, Object> m = u.exists() && u.getData() != null ? new HashMap<>(u.getData()) : new HashMap<>();
                m.put("username", name);
                cb.done(Person.fromDoc(uid, m), null);
            } catch (Exception e) { cb.done(null, describe(e)); }
        });
    }

    // ── follow / unfollow ──────────────────────────────────────────────

    public static void follow(Person p, Cb<Boolean> cb) {
        final String uid = me();
        if (uid.isEmpty() || uid.equals(p.uid)) { BG.execute(() -> cb.done(false, uid.equals(p.uid) ? "That's you." : "Sign in first.")); return; }
        BG.execute(() -> {
            try {
                DocumentSnapshot mine = Tasks.await(db().collection("USERS").document(uid).get(), T, TimeUnit.SECONDS);
                Person self = Person.fromDoc(uid, mine.exists() && mine.getData() != null ? mine.getData() : new HashMap<>());
                long now = System.currentTimeMillis();
                Map<String, Object> mineRow = new HashMap<>();
                mineRow.put("uid", p.uid); mineRow.put("username", p.username); mineRow.put("name", p.name); mineRow.put("since", now);
                Map<String, Object> theirRow = new HashMap<>();
                theirRow.put("uid", uid); theirRow.put("username", self.username); theirRow.put("name", self.name); theirRow.put("since", now);
                WriteBatch b = db().batch();
                b.set(db().collection("FRIENDS").document(uid).collection("list").document(p.uid), mineRow);
                b.set(db().collection("FOLLOWERS").document(p.uid).collection("list").document(uid), theirRow);
                Tasks.await(b.commit(), T, TimeUnit.SECONDS);
                com.harvana.app.trust.AppActivity.note(null, "FRIEND", "Added " + (p.name == null || p.name.isEmpty() ? "a member" : p.name));
                cb.done(true, null);
            } catch (Exception e) { cb.done(false, describe(e)); }
        });
    }

    public static void unfollow(String otherUid, Cb<Boolean> cb) {
        final String uid = me();
        BG.execute(() -> {
            try {
                WriteBatch b = db().batch();
                b.delete(db().collection("FRIENDS").document(uid).collection("list").document(otherUid));
                b.delete(db().collection("FOLLOWERS").document(otherUid).collection("list").document(uid));
                Tasks.await(b.commit(), T, TimeUnit.SECONDS);
                cb.done(true, null);
            } catch (Exception e) { cb.done(false, describe(e)); }
        });
    }

    public static void following(Cb<List<Person>> cb) { list("FRIENDS", cb); }
    public static void followers(Cb<List<Person>> cb) { list("FOLLOWERS", cb); }

    private static void list(String root, Cb<List<Person>> cb) {
        final String uid = me();
        BG.execute(() -> {
            try {
                List<Person> out = new ArrayList<>();
                for (QueryDocumentSnapshot d : Tasks.await(db().collection(root).document(uid).collection("list").get(), T, TimeUnit.SECONDS)) {
                    out.add(new Person(d.getString("uid") == null ? d.getId() : d.getString("uid"), d.getString("username"), d.getString("name")));
                }
                java.util.Collections.sort(out, (a, b) -> a.name.compareToIgnoreCase(b.name));
                cb.done(out, null);
            } catch (Exception e) { cb.done(new ArrayList<>(), describe(e)); }
        });
    }

    // ── contact cards ──────────────────────────────────────────────────

    public static void saveContacts(String phone, List<Map<String, Object>> links, Cb<Boolean> cb) {
        Map<String, Object> f = new HashMap<>();
        f.put("phone", phone == null ? "" : phone.trim());
        f.put("links", links);
        f.put("updatedAt", System.currentTimeMillis());
        BG.execute(() -> {
            try {
                Tasks.await(db().collection("CONTACTS").document(me()).set(f), T, TimeUnit.SECONDS);
                cb.done(true, null);
            } catch (Exception e) { cb.done(false, describe(e)); }
        });
    }

    /** A contact card. For someone else it only loads when you follow each other; otherwise the error says so. */
    @SuppressWarnings("unchecked")
    public static void contactsOf(String uid, Cb<Map<String, Object>> cb) {
        BG.execute(() -> {
            try {
                DocumentSnapshot d = Tasks.await(db().collection("CONTACTS").document(uid).get(), T, TimeUnit.SECONDS);
                cb.done(d.exists() && d.getData() != null ? d.getData() : new HashMap<String, Object>(), null);
            } catch (Exception e) {
                Throwable c = e.getCause() != null ? e.getCause() : e;
                boolean denied = c instanceof FirebaseFirestoreException && ((FirebaseFirestoreException) c).getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED;
                cb.done(null, denied ? "NOT_MUTUAL" : describe(e));
            }
        });
    }

    /** Country calling code of the signed-in member ("234"), from their contact card or profile phone; "" when it can't be told. */
    public static void myCountryCode(Cb<String> cb) {
        final String uid = me();
        BG.execute(() -> {
            try {
                String phone = "";
                DocumentSnapshot c = Tasks.await(db().collection("CONTACTS").document(uid).get(), T, TimeUnit.SECONDS);
                if (c.exists() && c.getString("phone") != null) phone = c.getString("phone");
                String cc = PhoneNumbers.countryCodeOf(PhoneNumbers.normalize(phone, ""));
                if (cc.isEmpty()) {
                    DocumentSnapshot u = Tasks.await(db().collection("USERS").document(uid).get(), T, TimeUnit.SECONDS);
                    String pp = u.getString("phone") == null ? "" : u.getString("phone");
                    cc = PhoneNumbers.countryCodeOf(PhoneNumbers.normalize(pp, ""));
                }
                cb.done(cc, null);
            } catch (Exception e) { cb.done("", describe(e)); }
        });
    }
}
