package com.harvana.app.social;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Contact discovery, the way messaging apps do it but opt-in. A member who turns "let my contacts find me" on publishes
 * scrambled keys of their phone number (and verified email): PHONE_INDEX/{sha256} and EMAIL_INDEX/{sha256}. Someone who
 * allows access to their contacts has each number and email scrambled on their phone and asks which keys exist. The index
 * holds no readable phone number or email and cannot be listed; entries are only fetched by exact key.
 * The phone number on a contact card is typed by the member and not verified, so a match is a lead to check, never proof.
 */
public final class DiscoveryStore {
    private DiscoveryStore() {}

    public static final class Match {
        public final Person person;
        public final String contactName;
        Match(Person p, String contactName) { person = p; this.contactName = contactName; }
    }

    public interface Cb<T> { void done(T value, String error); }

    private static final Executor BG = Executors.newCachedThreadPool();
    private static final long T = 25;

    private static FirebaseFirestore db() { return FirebaseFirestore.getInstance(); }
    private static String me() { return FirebaseAuth.getInstance().getCurrentUser() == null ? "" : FirebaseAuth.getInstance().getCurrentUser().getUid(); }

    /** Publishes (or, with enabled=false, removes) my scrambled keys. {@code myPhoneInternational} may be empty. */
    public static void setDiscoverable(boolean enabled, String myPhoneInternational, String verifiedEmail, String name, String username, Cb<Boolean> cb) {
        final String uid = me();
        BG.execute(() -> {
            try {
                DocumentSnapshot cardDoc = Tasks.await(db().collection("CONTACTS").document(uid).get(), T, TimeUnit.SECONDS);
                List<String> old = new ArrayList<>();
                Object prev = cardDoc.get("indexKeys");
                if (prev instanceof List) for (Object o : (List<?>) prev) old.add(String.valueOf(o));
                WriteBatch b = db().batch();
                for (String k : old) {
                    b.delete(db().collection(k.startsWith("p_") ? "PHONE_INDEX" : "EMAIL_INDEX").document(k.substring(2)));
                }
                List<String> keys = new ArrayList<>();
                if (enabled) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("uid", uid); row.put("username", username == null ? "" : username); row.put("name", name == null ? "" : name);
                    if (myPhoneInternational != null && !myPhoneInternational.isEmpty()) {
                        String k = PhoneNumbers.phoneKey(myPhoneInternational);
                        b.set(db().collection("PHONE_INDEX").document(k), row);
                        keys.add("p_" + k);
                    }
                    if (verifiedEmail != null && !verifiedEmail.isEmpty()) {
                        String k = PhoneNumbers.emailKey(verifiedEmail);
                        b.set(db().collection("EMAIL_INDEX").document(k), row);
                        keys.add("e_" + k);
                    }
                }
                Map<String, Object> f = new HashMap<>();
                f.put("indexKeys", keys);
                f.put("discoverable", enabled);
                b.set(db().collection("CONTACTS").document(uid), f, com.google.firebase.firestore.SetOptions.merge());
                Tasks.await(b.commit(), T, TimeUnit.SECONDS);
                cb.done(true, null);
            } catch (Exception e) {
                Throwable c = e.getCause() != null ? e.getCause() : e;
                cb.done(false, c.getMessage() == null ? "Couldn't update." : c.getMessage());
            }
        });
    }

    /** Which of these phone-book entries belong to members who turned discovery on? */
    public static void lookup(List<PhoneContacts.Entry> entries, String defaultCountryCode, Cb<List<Match>> cb) {
        final String self = me();
        BG.execute(() -> {
            try {
                // key -> the phone-book name it came from
                Map<String, String> phoneKeys = new LinkedHashMap<>(), emailKeys = new LinkedHashMap<>();
                for (PhoneContacts.Entry e : entries) {
                    for (String p : e.phones) {
                        String n = PhoneNumbers.normalize(p, defaultCountryCode);
                        if (!n.isEmpty()) phoneKeys.put(PhoneNumbers.phoneKey(n), e.name);
                    }
                    for (String m : e.emails) if (m.contains("@")) emailKeys.put(PhoneNumbers.emailKey(m), e.name);
                }
                Map<String, Match> found = new LinkedHashMap<>(); // by uid
                query("PHONE_INDEX", phoneKeys, self, found);
                query("EMAIL_INDEX", emailKeys, self, found);
                cb.done(new ArrayList<>(found.values()), null);
            } catch (Exception e) {
                Throwable c = e.getCause() != null ? e.getCause() : e;
                cb.done(new ArrayList<>(), c.getMessage() == null ? "Couldn't check your contacts." : c.getMessage());
            }
        });
    }

    private static void query(String collection, Map<String, String> keys, String self, Map<String, Match> found) throws Exception {
        List<String> all = new ArrayList<>(keys.keySet());
        for (int i = 0; i < all.size(); i += 10) {
            List<String> chunk = all.subList(i, Math.min(all.size(), i + 10));
            QuerySnapshot qs = Tasks.await(db().collection(collection).whereIn(FieldPath.documentId(), chunk).limit(10).get(), T, TimeUnit.SECONDS);
            for (DocumentSnapshot d : qs.getDocuments()) {
                String uid = d.getString("uid");
                if (uid == null || uid.equals(self) || found.containsKey(uid)) continue;
                found.put(uid, new Match(new Person(uid, d.getString("username"), d.getString("name")), keys.get(d.getId())));
            }
        }
    }
}
