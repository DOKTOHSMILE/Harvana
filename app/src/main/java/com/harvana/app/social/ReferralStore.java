package com.harvana.app.social;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.harvana.app.chain.GroupCanonical;
import com.harvana.app.sync.NodeGroupClient;
import com.harvana.app.sync.NodeIdentityClient;
import com.harvana.app.trust.ProfileTrust;
import com.harvana.app.wallet.PinPromptHelper;
import com.harvana.app.wallet.WalletManager;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Referrals by username. Whoever invites a person answers for them: the node lowers a referrer's trust score when someone
 * they referred hard-defaults. So a referral is recorded on the network only when BOTH sides agree.
 *
 *   1. At sign-up the new member may name a username. It is saved on their profile (referred_by_uid, referred_by_username).
 *   2. Once they have a wallet, they leave a request: REFERRAL_ASKS/{referrerUid}/from/{theirUid}.
 *   3. The referrer sees it in Friends and confirms with their PIN. That signs a consent for exactly that person.
 *   4. The new member's phone sends the node both signatures. The node checks them and records the referral, once.
 *
 * Everything runs off the main thread and calls back on that thread.
 */
public final class ReferralStore {
    private ReferralStore() {}

    public interface Cb<T> { void done(T value, String error); }

    /** What a referrer sees: someone named them and is waiting for them to confirm, or has been confirmed. */
    public static final class Ask {
        public String fromUid = "", referredAddress = "", status = "ASKED";
        public long at;
        public boolean consented() { return "CONSENTED".equals(status); }
    }

    private static final Executor BG = Executors.newCachedThreadPool();
    private static final long T = 20;
    private static final String PREFS = "referral_state";

    private static FirebaseFirestore db() { return FirebaseFirestore.getInstance(); }
    private static String me() { return FirebaseAuth.getInstance().getCurrentUser() == null ? "" : FirebaseAuth.getInstance().getCurrentUser().getUid(); }

    // ── the new member's side ──────────────────────────────────────────

    /**
     * Moves a pending referral one step forward. Safe to call often: it does nothing once the referral is recorded or when
     * the member named nobody. {@code done} receives a short status line, or "" when there is nothing to say.
     */
    public static void sync(Context context, Cb<String> done) {
        final Context c = context.getApplicationContext();
        final String uid = me();
        if (uid.isEmpty()) { BG.execute(() -> done.done("", null)); return; }
        final SharedPreferences sp = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (sp.getBoolean("done_" + uid, false)) { BG.execute(() -> done.done("", null)); return; }
        BG.execute(() -> {
            try {
                DocumentSnapshot mine = Tasks.await(db().collection("USERS").document(uid).get(), T, TimeUnit.SECONDS);
                String referrerUid = str(mine.get("referred_by_uid"));
                if (referrerUid.isEmpty()) { sp.edit().putBoolean("done_" + uid, true).apply(); done.done("", null); return; }
                String wallet = WalletManager.getActiveWalletAddress(c);
                if (wallet == null || !wallet.startsWith("HV0x")) { done.done("Create a wallet to finish your referral.", null); return; }

                DocumentSnapshot ask = Tasks.await(db().collection("REFERRAL_ASKS").document(referrerUid).collection("from").document(uid).get(), T, TimeUnit.SECONDS);
                if (!ask.exists()) {
                    Map<String, Object> f = new HashMap<>();
                    f.put("from", uid);
                    f.put("referredAddress", wallet);
                    f.put("at", System.currentTimeMillis());
                    Tasks.await(db().collection("REFERRAL_ASKS").document(referrerUid).collection("from").document(uid).set(f), T, TimeUnit.SECONDS);
                    done.done("Waiting for @" + str(mine.get("referred_by_username")) + " to confirm.", null);
                    return;
                }
                String sig = str(ask.get("consent_sig")), key = str(ask.get("consent_pub")), referrerWallet = str(ask.get("consent_address"));
                if (sig.isEmpty() || key.isEmpty() || referrerWallet.isEmpty()) {
                    done.done("Waiting for @" + str(mine.get("referred_by_username")) + " to confirm.", null);
                    return;
                }
                NodeGroupClient.recordReferral(c, wallet, referrerWallet, key, sig, new NodeGroupClient.Callback() {
                    @Override public void onSuccess(JSONObject json) {
                        sp.edit().putBoolean("done_" + uid, true).apply();
                        done.done("Referral recorded.", null);
                    }
                    @Override public void onError(String error) {
                        // Recorded earlier (maybe from another install): nothing more to do. Any other refusal is tried again next time.
                        if (error != null && com.harvana.app.utils.ErrorText.is(error, "REFERRAL_ALREADY_RECORDED")) sp.edit().putBoolean("done_" + uid, true).apply();
                        done.done("", error);
                    }
                });
            } catch (Exception e) { done.done("", e.getMessage()); }
        });
    }

    // ── the referrer's side ────────────────────────────────────────────

    /** People who named me and are waiting for me to confirm (or whom I already confirmed). */
    public static void asksForMe(Cb<List<Ask>> cb) {
        final String uid = me();
        BG.execute(() -> {
            try {
                List<Ask> out = new ArrayList<>();
                for (QueryDocumentSnapshot d : Tasks.await(db().collection("REFERRAL_ASKS").document(uid).collection("from").get(), T, TimeUnit.SECONDS)) {
                    Ask a = new Ask();
                    a.fromUid = d.getId();
                    a.referredAddress = str(d.get("referredAddress"));
                    a.status = str(d.get("status")).isEmpty() ? "ASKED" : str(d.get("status"));
                    a.at = d.get("at") instanceof Number ? ((Number) d.get("at")).longValue() : 0L;
                    out.add(a);
                }
                java.util.Collections.sort(out, (x, y) -> Long.compare(y.at, x.at));
                cb.done(out, null);
            } catch (Exception e) { cb.done(new ArrayList<>(), e.getMessage()); }
        });
    }

    /** Confirms with the PIN: signs a consent for exactly this person's address and leaves it for their phone to submit. */
    public static void approve(Activity host, Ask ask, Cb<Boolean> cb) {
        final String wallet = WalletManager.getActiveWalletAddress(host);
        if (wallet == null || !wallet.startsWith("HV0x")) { BG.execute(() -> cb.done(false, "Create a wallet first.")); return; }
        if (ask.referredAddress.isEmpty()) { BG.execute(() -> cb.done(false, "They have not created a wallet yet.")); return; }
        PinPromptHelper.authorize(host, "Confirm that you know this person and are referring them", () -> BG.execute(() -> {
            try {
                JSONObject signed = NodeIdentityClient.signMessage(host, wallet, GroupCanonical.referralConsent(ask.referredAddress, wallet));
                Map<String, Object> f = new HashMap<>();
                f.put("consent_address", wallet);
                f.put("consent_pub", signed.getString("publicKey"));
                f.put("consent_sig", signed.getString("signature"));
                f.put("status", "CONSENTED");
                Tasks.await(db().collection("REFERRAL_ASKS").document(me()).collection("from").document(ask.fromUid).update(f), T, TimeUnit.SECONDS);
                cb.done(true, null);
            } catch (Exception e) { cb.done(false, e.getMessage()); }
        }));
    }

    public static void decline(Ask ask, Cb<Boolean> cb) {
        BG.execute(() -> {
            try {
                Tasks.await(db().collection("REFERRAL_ASKS").document(me()).collection("from").document(ask.fromUid).delete(), T, TimeUnit.SECONDS);
                cb.done(true, null);
            } catch (Exception e) { cb.done(false, e.getMessage()); }
        });
    }

    /** Everyone who named me as their referrer at sign-up, whether or not they have asked yet. */
    public static void namedMe(Cb<List<Person>> cb) {
        final String uid = me();
        BG.execute(() -> {
            try {
                List<Person> out = new ArrayList<>();
                for (QueryDocumentSnapshot d : Tasks.await(db().collection("USERS").whereEqualTo("referred_by_uid", uid).get(), T, TimeUnit.SECONDS)) {
                    out.add(Person.fromDoc(d.getId(), d.getData()));
                }
                cb.done(out, null);
            } catch (Exception e) { cb.done(new ArrayList<>(), e.getMessage()); }
        });
    }

    /** True when this person's profile card already carries a wallet, so a referral can be confirmed for them. */
    public static void walletOf(String uid, Cb<String> cb) {
        ProfileTrust.resolve(uid, card -> cb.done(card.wallet, null));
    }

    private static String str(Object o) { return o == null ? "" : String.valueOf(o).trim(); }
}
