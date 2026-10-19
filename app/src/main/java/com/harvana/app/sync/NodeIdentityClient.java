package com.harvana.app.sync;

import android.content.Context;
import android.provider.Settings;
import android.util.Base64;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.harvana.app.chain.IdentityCanonical;
import com.harvana.app.crypto.Ed25519;
import com.harvana.app.wallet.WalletManager;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Tells the node who owns a wallet, so trust can follow the person.
 *  - identity link: wallet signature + Firebase ID token (the node keeps only an HMAC of the uid).
 *  - personhood bind: wallet signature over a device attestation hash (10 trust points).
 *  - lookup: the node's own trust view of any wallet.
 * One person, many wallets: every wallet the user signs in with is added to them (the first stays the main one) and the
 * node counts each wallet's activity toward the person. A wallet belongs to one person, and a phone to one person.
 */
public final class NodeIdentityClient {
    private NodeIdentityClient() {}

    public interface Callback {
        void onSuccess(JSONObject json);
        void onError(String error);
    }

    public static void lookup(String address, Callback cb) {
        HarvanaNodeClient.nodeGet("/api/v1/personhood/" + address, new HarvanaNodeClient.JsonCallback() {
            @Override public void onSuccess(JSONObject j) { cb.onSuccess(j); }
            @Override public void onError(String e) { cb.onError(e); }
        });
    }

    public static void link(Context c, String address, Callback cb) {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null) { cb.onError("Sign in first."); return; }
        u.getIdToken(true).addOnCompleteListener(t -> {                       // a fresh token: an old one is refused
            if (!t.isSuccessful() || t.getResult() == null || t.getResult().getToken() == null) {
                cb.onError("Could not get sign-in token."); return;
            }
            final String token = t.getResult().getToken();
            // The profile id lets the node put the trust score on the public profile card. Linking works without it.
            com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("USERS").document(u.getUid()).get()
                    .addOnCompleteListener(r -> {
                        String pid = "";
                        if (r.isSuccessful() && r.getResult() != null && r.getResult().getString("pid") != null) pid = r.getResult().getString("pid");
                        try {
                            long ts = System.currentTimeMillis();
                            JSONObject body = signed(c, address, IdentityCanonical.identityLink(address, ts));
                            body.put("address", address).put("timestamp", ts).put("idToken", token);
                            if (!pid.isEmpty()) body.put("pid", pid);
                            post("/api/v1/identity/link", body, cb);
                        } catch (Exception e) { cb.onError(com.harvana.app.utils.ErrorText.of(e)); }
                    });
        });
    }

    /** The signed-in user's own linked wallets, main first: {"wallets":[{"address","main","addedAt"}]}. */
    public static void myWallets(Callback cb) {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null) { cb.onError("Sign in first."); return; }
        u.getIdToken(false).addOnCompleteListener(t -> {
            if (!t.isSuccessful() || t.getResult() == null || t.getResult().getToken() == null) { cb.onError("Could not get sign-in token."); return; }
            try { post("/api/v1/identity/wallets", new JSONObject().put("idToken", t.getResult().getToken()), cb); }
            catch (Exception e) { cb.onError(com.harvana.app.utils.ErrorText.of(e)); }
        });
    }

    public static void bindPersonhood(Context c, String address, Callback cb) {
        try {
            long ts = System.currentTimeMillis();
            String dev = deviceHash(c);
            JSONObject body = signed(c, address, IdentityCanonical.personhoodBind(address, dev, ts));
            body.put("address", address).put("deviceAttestationHash", dev).put("issuedAt", ts);
            post("/api/v1/personhood/bind", body, cb);
        } catch (Exception e) { cb.onError(com.harvana.app.utils.ErrorText.of(e)); }
    }

    public static void vouch(Context c, String voucher, String subject, Callback cb) {
        try {
            long ts = System.currentTimeMillis();
            JSONObject body = signed(c, voucher, IdentityCanonical.personhoodVouch(voucher, subject, ts));
            body.put("voucherAddress", voucher).put("subjectAddress", subject).put("timestamp", ts);
            post("/api/v1/personhood/vouch", body, cb);
        } catch (Exception e) { cb.onError(com.harvana.app.utils.ErrorText.of(e)); }
    }

    /** 64 hex chars. Client-asserted (the node documents real attestation as future Play Integrity work). */
    static String deviceHash(Context c) throws Exception {
        String id = Settings.Secure.getString(c.getContentResolver(), Settings.Secure.ANDROID_ID);
        byte[] d = MessageDigest.getInstance("SHA-256")
            .digest(("HARVANA_DEVICE_V1|" + id + "|" + c.getPackageName()).getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : d) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    /** Signs {@code message} with the wallet\'s key: {publicKey, signature}. For consents that are checked by the node later. */
    public static JSONObject signMessage(Context c, String address, String message) throws Exception {
        return signed(c, address, message);
    }

    static JSONObject signed(Context c, String address, String message) throws Exception {
        String seed = WalletManager.loadPrivateKey(c, address);
        String pub = WalletManager.getPublicKey(c, address);
        if (seed == null || pub == null) throw new IllegalStateException("No local signing key for this wallet.");
        byte[] sig = Ed25519.sign(Base64.decode(seed, Base64.NO_WRAP), message.getBytes(StandardCharsets.UTF_8));
        return new JSONObject().put("publicKey", pub).put("signature", Base64.encodeToString(sig, Base64.NO_WRAP));
    }

    private static void post(String path, JSONObject body, Callback cb) {
        HarvanaNodeClient.nodePost(path, body.toString(), new HarvanaNodeClient.JsonCallback() {
            @Override public void onSuccess(JSONObject j) { cb.onSuccess(j); }
            @Override public void onError(String e) { cb.onError(e); }
        });
    }
}
