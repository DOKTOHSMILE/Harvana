package com.harvanna.app.identity;

import android.content.Context;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * One person, two records. The node says which wallet is theirs and what the
 * network trusts. The account record says their name, photo and contact.
 * Neither replaces the other.
 */
public final class CombinedIdentity {
    private CombinedIdentity() {}

    public static final class Account {
        public String uid = "";
        public String name = "";
        public String username = "";
        public String photo = "";
        public String phone = "";
        public String email = "";
        public String wallet = "";
    }

    public interface Callback { void onAccount(Account account); }

    /** Profile of the signed-in user, plus the wallet the node linked. */
    public static void load(Callback cb) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { cb.onAccount(new Account()); return; }
        FirebaseDatabase.getInstance().getReference("userdb").child(user.getUid()).get()
                .addOnSuccessListener(snap -> {
                    Account a = new Account();
                    a.uid = user.getUid();
                    a.email = user.getEmail() == null ? "" : user.getEmail();
                    Map<String, Object> m = snap.getValue() instanceof Map ? cast(snap.getValue()) : new HashMap<>();
                    String first = str(m.get("first_name"));
                    String last = str(m.get("last_name"));
                    a.name = (first + " " + last).trim();
                    if (a.name.isEmpty()) a.name = str(m.get("name"));
                    if (a.name.isEmpty() && user.getDisplayName() != null) a.name = user.getDisplayName();
                    a.username = str(m.get("username"));
                    a.photo = str(m.get("icon"));
                    if (a.photo.isEmpty()) a.photo = str(m.get("photoUrl"));
                    if (a.photo.isEmpty() && user.getPhotoUrl() != null) a.photo = user.getPhotoUrl().toString();
                    a.phone = str(m.get("phone"));
                    if (a.email.isEmpty()) a.email = str(m.get("email"));
                    a.wallet = str(m.get("walletAddress"));
                    cb.onAccount(a);
                })
                .addOnFailureListener(e -> {
                    Account a = new Account();
                    a.uid = user.getUid();
                    a.name = user.getDisplayName() == null ? "" : user.getDisplayName();
                    a.email = user.getEmail() == null ? "" : user.getEmail();
                    cb.onAccount(a);
                });
    }

    /** After the node accepts the link, stamp that wallet onto the account and a public Firestore identity. */
    public static void stamp(Context c, String address) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || address == null || address.isEmpty()) return;
        Map<String, Object> profile = new HashMap<>();
        profile.put("walletAddress", address);
        profile.put("identityLinked", true);
        FirebaseDatabase.getInstance().getReference("userdb").child(user.getUid()).updateChildren(profile);
        load(account -> {
            Map<String, Object> doc = new HashMap<>();
            doc.put("address", address);
            doc.put("uid", user.getUid());
            doc.put("displayName", account.name);
            doc.put("username", account.username);
            doc.put("photoUrl", account.photo);
            doc.put("source", "node+account");
            doc.put("updatedAt", System.currentTimeMillis());
            FirebaseFirestore.getInstance().collection("IDENTITY").document(address).set(doc, SetOptions.merge());
        });
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> cast(Object v) { return (Map<String, Object>) v; }

    private static String str(Object o) { return o == null ? "" : String.valueOf(o).trim(); }
}
