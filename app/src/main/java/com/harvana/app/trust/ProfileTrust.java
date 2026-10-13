package com.harvana.app.trust;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The person behind a post, with their trust score. Reads the public profile card (PROFILES/{pid}); the trust fields on it
 * are written by the node and cannot be edited or removed by the user, so a score is shown on every profile that has one.
 * Nothing here calculates a score. Results are cached for a couple of minutes so a scrolling feed does not re-read the same author.
 */
public final class ProfileTrust {
    private ProfileTrust() {}

    public static final class Card {
        public String uid = "", pid = "", name = "", username = "", icon = "", wallet = "", trustTier = "";
        /** 0 to 100 once the node has scored this person; -1 before that. */
        public int percent = -1;
        public boolean scored() { return percent >= 0; }
    }

    public interface Callback { void onCard(Card card); }

    private static final long TTL_MS = 2 * 60 * 1000L;
    private static final Map<String, Card> CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Long> CACHED_AT = new ConcurrentHashMap<>();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    /** Drops the saved copy of one person, so the next resolve reads Firestore again. */
    public static void forget(String uid) { if (uid != null) { CACHE.remove(uid); CACHED_AT.remove(uid); } }

    /** Calls back on the main thread, always with a card (empty fields when the person cannot be read). */
    public static void resolve(String uid, Callback cb) {
        if (uid == null || uid.trim().isEmpty()) { MAIN.post(() -> cb.onCard(new Card())); return; }
        Long at = CACHED_AT.get(uid);
        Card cached = CACHE.get(uid);
        if (cached != null && at != null && System.currentTimeMillis() - at < TTL_MS) { MAIN.post(() -> cb.onCard(cached)); return; }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("USERS").document(uid).get().addOnCompleteListener(userTask -> {
            Card card = new Card();
            card.uid = uid;
            if (userTask.isSuccessful() && userTask.getResult() != null && userTask.getResult().exists()) {
                Map<String, Object> u = userTask.getResult().getData();
                if (u != null) {
                    card.name = fullName(text(u.get("first_name")), text(u.get("last_name")));
                    card.username = text(u.get("username"));
                    card.icon = text(u.get("icon"));
                    card.pid = text(u.get("pid"));
                }
            }
            if (!card.pid.matches("^[0-9]{10}$")) { finish(uid, card, cb); return; }
            db.collection("PROFILES").document(card.pid).get().addOnCompleteListener(cardTask -> {
                if (cardTask.isSuccessful() && cardTask.getResult() != null && cardTask.getResult().exists()) {
                    Map<String, Object> p = cardTask.getResult().getData();
                    if (p != null) {
                        String n = fullName(text(p.get("first_name")), text(p.get("last_name")));
                        if (!n.isEmpty()) card.name = n;
                        if (!text(p.get("username")).isEmpty()) card.username = text(p.get("username"));
                        // The photo the person set last is on their own record (Edit profile writes it there first);
                        // the public card's copy is only a fallback, so an old copy never wins over a new photo.
                        if (card.icon.isEmpty() && !text(p.get("icon")).isEmpty()) card.icon = text(p.get("icon"));
                        card.wallet = text(p.get("wallet"));
                        card.trustTier = text(p.get("trust_tier"));
                        if (p.get("trust_percent") instanceof Number) card.percent = Math.max(0, Math.min(100, ((Number) p.get("trust_percent")).intValue()));
                    }
                }
                finish(uid, card, cb);
            });
        });
    }

    private static void finish(String uid, Card card, Callback cb) {
        CACHE.put(uid, card);
        CACHED_AT.put(uid, System.currentTimeMillis());
        MAIN.post(() -> cb.onCard(card));
    }

    /** Fills {@code pill} with the person's trust score as soon as it is read. */
    public static void attach(TextView pill, String uid) {
        if (pill == null) return;
        bind(pill, null);
        resolve(uid, card -> bind(pill, card));
    }

    /** Puts "Name  ·  92% Gold" in {@code view}: the name first, the score once it is read (name only when unscored). */
    public static void nameWithTrust(android.content.Context context, TextView view, String uid, String fallback) {
        if (view == null) return;
        view.setText(fallback);
        com.harvana.app.group.MemberNameResolver.resolve(context, uid, fallback, name -> {
            view.setText(name);
            resolve(uid, card -> { if (card.scored()) view.setText(name + "  \u00b7  " + label(card)); });
        });
    }

    /** The label on a profile: "92% · Gold", or "Not scored yet" before the node has published one. */
    public static String label(Card c) {
        if (c == null || !c.scored()) return "Not scored yet";
        String tier = c.trustTier.isEmpty() ? tierFor(c.percent) : c.trustTier;
        return "New".equals(tier) ? "New · " + c.percent + "%" : c.percent + "% · " + tier;
    }

    /** Fills a small text view as a pill coloured by tier. Always visible: the score cannot be hidden. */
    public static void bind(TextView pill, Card c) {
        if (pill == null) return;
        pill.setVisibility(android.view.View.VISIBLE);
        pill.setText(label(c));
        int color = c != null && c.scored() ? tierColor(c.trustTier.isEmpty() ? tierFor(c.percent) : c.trustTier) : 0xFF8B98AD;
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(pill.getResources().getDisplayMetrics().density * 12);
        bg.setColor(Color.argb(40, Color.red(color), Color.green(color), Color.blue(color)));
        bg.setStroke(Math.max(1, (int) pill.getResources().getDisplayMetrics().density), Color.argb(120, Color.red(color), Color.green(color), Color.blue(color)));
        pill.setBackground(bg);
        pill.setTextColor(color);
        // The tier's medal, and a tap that shows every tier.
        String tier = c != null && c.scored() ? (c.trustTier.isEmpty() ? tierFor(c.percent) : c.trustTier) : "New";
        if (pill.getContext() instanceof android.app.Activity) {
            TierBadges.decorate((android.app.Activity) pill.getContext(), pill, tier, c == null ? 0 : c.percent, c != null && c.scored());
            android.graphics.drawable.Drawable[] ds = pill.getCompoundDrawablesRelative();
            if (ds[0] != null) {                                         // a little smaller in lists
                int px = Math.round(16 * pill.getResources().getDisplayMetrics().density);
                ds[0].setBounds(0, 0, px, px);
                pill.setCompoundDrawablesRelative(ds[0], null, null, null);
            }
        }
    }

    /** Same thresholds as the node's TrustPercent (Platinum also needs strong components, which the node already applied to the tier it sent). */
    public static String tierFor(int percent) {
        if (percent >= 85) return "Platinum";
        if (percent >= 65) return "Gold";
        if (percent >= 40) return "Silver";
        return "Bronze";
    }

    public static int tierColor(String tier) {
        if ("New".equals(tier)) return 0xFF8B98AD;
        if ("Platinum".equals(tier)) return 0xFF7DD3FC;
        if ("Gold".equals(tier)) return 0xFFFFC857;
        if ("Silver".equals(tier)) return 0xFFB4BFD0;
        return 0xFFD08A4E;
    }

    private static String fullName(String first, String last) { return (first + " " + last).trim(); }
    private static String text(Object o) { return o == null ? "" : String.valueOf(o).trim(); }
}
