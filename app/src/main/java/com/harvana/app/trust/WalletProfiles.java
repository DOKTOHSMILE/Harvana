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
                        for (QueryDocumentSnapshot d : task.getResult()) {
                            Map<String, Object> p = d.getData();
                            card = new ProfileTrust.Card();
                            card.pid = d.getId();
                            card.wallet = wallet;
                            card.name = join(text(p.get("first_name")), text(p.get("last_name")));
                            card.username = text(p.get("username"));
                            card.icon = text(p.get("icon"));
                            card.trustTier = text(p.get("trust_tier"));
                            if (p.get("trust_percent") instanceof Number) card.percent = Math.max(0, Math.min(100, ((Number) p.get("trust_percent")).intValue()));
                            break;
                        }
                    }
                    if (card != null) FOUND.put(key, card); else MISSED.put(key, System.currentTimeMillis());
                    final ProfileTrust.Card result = card;
                    MAIN.post(() -> cb.onCard(result));
                });
    }

    private static String text(Object o) { return o == null ? "" : String.valueOf(o).trim(); }
    private static String join(String a, String b) { return (a + " " + b).trim(); }
}
