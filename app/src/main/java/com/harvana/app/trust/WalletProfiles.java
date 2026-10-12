package com.harvana.app.trust;

import android.os.Handler;
import android.os.Looper;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Finds the person behind a wallet address. The node writes a verified person's wallet onto their public profile card
 * (PROFILES/{pid}.wallet) when they link their account, so a member the network knows only by wallet can still be shown
 * by the name, username and picture they chose in the app.
 */
public final class WalletProfiles {
    private WalletProfiles() {}

    public interface Callback { void onCard(ProfileTrust.Card cardOrNull); }

    private static final Map<String, ProfileTrust.Card> FOUND = new ConcurrentHashMap<>();
    private static final Map<String, Long> MISSED = new ConcurrentHashMap<>();
    private static final long MISS_TTL_MS = 5 * 60 * 1000L;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public static void resolve(final String wallet, final Callback cb) {
        if (wallet == null || wallet.isEmpty()) { MAIN.post(() -> cb.onCard(null)); return; }
        final String key = wallet.toLowerCase(java.util.Locale.US);
        ProfileTrust.Card hit = FOUND.get(key);
        if (hit != null) { MAIN.post(() -> cb.onCard(hit)); return; }
        Long missedAt = MISSED.get(key);
        if (missedAt != null && System.currentTimeMillis() - missedAt < MISS_TTL_MS) { MAIN.post(() -> cb.onCard(null)); return; }
        FirebaseFirestore.getInstance().collection("PROFILES").whereEqualTo("wallet", wallet).limit(1).get()
                .addOnCompleteListener(task -> {
                    ProfileTrust.Card card = null;
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (QueryDocumentSnapshot d : task.getResult()) { card = card(d.getId(), d.getData(), wallet); break; }
                    }
                    if (card != null) { done(key, card, cb); return; }
                    // Not the profile's main wallet: ask the network who owns it. Every wallet a person linked to their
                    // account belongs to them, so a payment from a second wallet shows under their name, not a new member.
                    byOwner(wallet, key, cb);
                });
    }

    private static void byOwner(String wallet, String key, Callback cb) {
        com.harvana.app.sync.HarvanaNodeClient.nodeGet("/api/v1/identity/owner/" + wallet, new com.harvana.app.sync.HarvanaNodeClient.JsonCallback() {
            @Override public void onSuccess(org.json.JSONObject j) {
                String pid = j == null ? "" : j.optString("pid", "");
                String main = j == null ? "" : j.optString("mainWallet", "");
                if (!pid.isEmpty()) {
                    FirebaseFirestore.getInstance().collection("PROFILES").document(pid).get().addOnCompleteListener(t -> {
                        if (t.isSuccessful() && t.getResult() != null && t.getResult().exists())
                            done(key, card(pid, t.getResult().getData(), wallet), cb);
                        else byMain(wallet, main, key, cb);
                    });
                } else {
                    byMain(wallet, main, key, cb);
                }
            }
            @Override public void onError(String e) { done(key, null, cb); }
        });
    }

    private static void byMain(String wallet, String main, String key, Callback cb) {
        if (main == null || main.isEmpty() || main.equalsIgnoreCase(wallet)) { done(key, null, cb); return; }
        FirebaseFirestore.getInstance().collection("PROFILES").whereEqualTo("wallet", main).limit(1).get().addOnCompleteListener(t -> {
            ProfileTrust.Card card = null;
            if (t.isSuccessful() && t.getResult() != null) for (QueryDocumentSnapshot d : t.getResult()) { card = card(d.getId(), d.getData(), wallet); break; }
            done(key, card, cb);
        });
    }

    private static ProfileTrust.Card card(String pid, Map<String, Object> p, String wallet) {
        if (p == null) return null;
        ProfileTrust.Card card = new ProfileTrust.Card();
        card.pid = pid;
        card.wallet = wallet;
        card.name = join(text(p.get("first_name")), text(p.get("last_name")));
        card.username = text(p.get("username"));
        card.icon = text(p.get("icon"));
        card.trustTier = text(p.get("trust_tier"));
        if (p.get("trust_percent") instanceof Number) card.percent = Math.max(0, Math.min(100, ((Number) p.get("trust_percent")).intValue()));
        return card;
    }

    private static void done(String key, ProfileTrust.Card card, Callback cb) {
        if (card != null) FOUND.put(key, card); else MISSED.put(key, System.currentTimeMillis());
        MAIN.post(() -> cb.onCard(card));
    }

    private static String text(Object o) { return o == null ? "" : String.valueOf(o).trim(); }
    private static String join(String a, String b) { return (a + " " + b).trim(); }
}
