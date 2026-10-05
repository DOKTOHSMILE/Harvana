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
 * The node allows ONE wallet per user and ONE wallet per device hash; extra wallets are separate identities.
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
        u.getIdToken(false).addOnCompleteListener(t -> {
            if (!t.isSuccessful() || t.getResult() == null || t.getResult().getToken() == null) {
                cb.onError("Could not get sign-in token."); return;
            }
            try {
                long ts = System.currentTimeMillis();
                JSONObject body = signed(c, address, IdentityCanonical.identityLink(address, ts));
                body.put("address", address).put("timestamp", ts).put("idToken", t.getResult().getToken());
                post("/api/v1/identity/link", body, cb);
            } catch (Exception e) { cb.onError(e.getMessage()); }
        });
    }

    public static void bindPersonhood(Context c, String address, Callback cb) {
        try {
            long ts = System.currentTimeMillis();
            String dev = deviceHash(c);
            JSONObject body = signed(c, address, IdentityCanonical.personhoodBind(address, dev, ts));
            body.put("address", address).put("deviceAttestationHash", dev).put("issuedAt", ts);
            post("/api/v1/personhood/bind", body, cb);
        } catch (Exception e) { cb.onError(e.getMessage()); }
    }

    public static void vouch(Context c, String voucher, String subject, Callback cb) {
        try {
            long ts = System.currentTimeMillis();
            JSONObject body = signed(c, voucher, IdentityCanonical.personhoodVouch(voucher, subject, ts));
            body.put("voucherAddress", voucher).put("subjectAddress", subject).put("timestamp", ts);
            post("/api/v1/personhood/vouch", body, cb);
        } catch (Exception e) { cb.onError(e.getMessage()); }
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

    private static JSONObject signed(Context c, String address, String message) throws Exception {
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
