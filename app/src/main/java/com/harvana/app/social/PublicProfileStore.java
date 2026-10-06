package com.harvana.app.social;

import android.content.Context;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.harvana.app.engine.Position;
import com.harvana.app.firestore.UserProfileManager;
import com.harvana.app.sync.NodePortfolio;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * What other members may see of a person (PROFILES/{pid}, readable by every signed-in account) and how the person controls it.
 *
 * Always public: name, username, photo, the trust score and its parts (the node writes those, nobody can hide them), country,
 * state, member-since, and the counts of groups, completed cycles, loans and pending loans. Never public: amounts, group names
 * (a viewer only sees groups they also belong to), date of birth, phone, email, address. Public only when the member turns it on:
 * age and city. When one is off the phone removes it from the card, so it is not merely hidden on screen.
 *
 * The counts are written by the member's own phone from what the node and the group records say; they carry no money.
 * All calls run off the main thread and call back on that background thread.
 */
public final class PublicProfileStore {
    private PublicProfileStore() {}

    public static final int BIO_MAX = 140;
    private static final long T = 20;
    private static final Executor BG = Executors.newCachedThreadPool();

    public interface Cb<T> { void done(T value, String error); }

    /** A group both people belong to, with what the Groups list shows for it. Only such groups ever reach a profile. */
    public static final class SharedGroup {
        public final String id, name;
        public String type = "", frequency = "", state = "", photo = "", nodeKey = "";
        public long members, round, slots;
        public double amount;
        SharedGroup(String id, String name) { this.id = id; this.name = name; }
    }

    public static final class Card {
        public String uid = "", pid = "", name = "", username = "", icon = "", wallet = "", trustTier = "";
        public String country = "", state = "", city = "", bio = "";
        public int trustPercent = -1, age = -1;
        public boolean showAge, showCity;
        public long memberSince;
        /** When the member last had the app open (self-reported presence); 0 when unknown. */
        public long lastActiveAt;
        public int groups = -1, cycles = -1, loans = -1, pendingLoans = -1;
        public List<SharedGroup> sharedGroups = new ArrayList<>();
        public boolean iFollow, followsMe;
        /** The node's trust parts (REPUTATION/{wallet}) for the breakdown; empty when not published. */
        public Map<String, Object> trustParts = new HashMap<>();
        public boolean scored() { return trustPercent >= 0; }
    }

    private static FirebaseFirestore db() { return FirebaseFirestore.getInstance(); }
    private static String me() { return FirebaseAuth.getInstance().getCurrentUser() == null ? "" : FirebaseAuth.getInstance().getCurrentUser().getUid(); }
    private static String str(Object o) { return o == null ? "" : String.valueOf(o).trim(); }
    private static int num(Object o, int dflt) { return o instanceof Number ? ((Number) o).intValue() : dflt; }

    /** Reads a member's public view. Works for the viewer's own profile too (then there is nothing shared to compute). */
    public static void load(String uid, Cb<Card> cb) {
        BG.execute(() -> {
            try {
                Card c = new Card();
                c.uid = uid;
                DocumentSnapshot u = Tasks.await(db().collection("USERS").document(uid).get(), T, TimeUnit.SECONDS);
                Map<String, Object> user = u.exists() && u.getData() != null ? u.getData() : new HashMap<>();
                c.name = (str(user.get("first_name")) + " " + str(user.get("last_name"))).trim();
                c.username = str(user.get("username"));
                c.icon = str(user.get("icon"));
                c.pid = str(user.get("pid"));
                c.country = str(user.get("country"));
                c.state = str(user.get("state"));
                if (user.get("timestamp") instanceof Number) c.memberSince = ((Number) user.get("timestamp")).longValue();
                if (user.get("lastActiveAt") instanceof Number) c.lastActiveAt = ((Number) user.get("lastActiveAt")).longValue();
                List<String> theirGroups = groupIds(user.get("groups"));

                if (c.pid.matches("^[0-9]{10}$")) {
                    DocumentSnapshot p = Tasks.await(db().collection("PROFILES").document(c.pid).get(), T, TimeUnit.SECONDS);
                    if (p.exists() && p.getData() != null) {
                        Map<String, Object> card = p.getData();
                        String n = (str(card.get("first_name")) + " " + str(card.get("last_name"))).trim();
                        if (!n.isEmpty()) c.name = n;
                        if (!str(card.get("username")).isEmpty()) c.username = str(card.get("username"));
                        if (!str(card.get("icon")).isEmpty()) c.icon = str(card.get("icon"));
                        if (!str(card.get("country")).isEmpty()) c.country = str(card.get("country"));
                        if (!str(card.get("state")).isEmpty()) c.state = str(card.get("state"));
                        c.wallet = str(card.get("wallet"));
                        c.trustTier = str(card.get("trust_tier"));
                        c.trustPercent = card.get("trust_percent") instanceof Number ? Math.max(0, Math.min(100, num(card.get("trust_percent"), 0))) : -1;
                        c.showAge = Boolean.TRUE.equals(card.get("show_age"));
                        c.showCity = Boolean.TRUE.equals(card.get("show_city"));
                        if (c.showAge && card.get("age") instanceof Number) c.age = num(card.get("age"), -1);
                        if (c.showCity) c.city = str(card.get("city"));
                        c.bio = str(card.get("bio"));
                        if (card.get("member_since") instanceof Number) c.memberSince = ((Number) card.get("member_since")).longValue();
                        c.groups = num(card.get("groups_count"), -1);
                        c.cycles = num(card.get("cycles_completed"), -1);
                        c.loans = num(card.get("loans_count"), -1);
                        c.pendingLoans = num(card.get("loans_pending"), -1);
                    }
                }
                if (c.groups < 0 && !theirGroups.isEmpty()) c.groups = theirGroups.size();

                if (!c.wallet.isEmpty()) {
                    DocumentSnapshot r = Tasks.await(db().collection("REPUTATION").document(c.wallet).get(), T, TimeUnit.SECONDS);
                    if (r.exists() && r.getData() != null) c.trustParts = r.getData();
                }

                String mine = me();
                if (!mine.isEmpty() && !mine.equals(uid)) {
                    c.iFollow = Tasks.await(db().collection("FRIENDS").document(mine).collection("list").document(uid).get(), T, TimeUnit.SECONDS).exists();
                    c.followsMe = Tasks.await(db().collection("FOLLOWERS").document(mine).collection("list").document(uid).get(), T, TimeUnit.SECONDS).exists();
                    // Groups both belong to: the viewer's own groups checked against the other person's list. Only these names are shown.
                    DocumentSnapshot mineDoc = Tasks.await(db().collection("USERS").document(mine).get(), T, TimeUnit.SECONDS);
                    List<String> myGroups = groupIds(mineDoc.exists() && mineDoc.getData() != null ? mineDoc.getData().get("groups") : null);
                    for (String gid : myGroups) {
                        if (!theirGroups.contains(gid) || c.sharedGroups.size() >= 12) continue;
                        try {
                            DocumentSnapshot g = Tasks.await(db().collection("GROUPS").document(gid).get(), T, TimeUnit.SECONDS);
                            Map<String, Object> gd = g.exists() && g.getData() != null ? g.getData() : new HashMap<>();
                            // The group's name is stored as groupName (older documents may have name).
                            String gname = str(gd.get("groupName"));
                            if (gname.isEmpty()) gname = str(gd.get("name"));
                            SharedGroup sg = new SharedGroup(gid, gname.isEmpty() ? "Untitled group" : gname);
                            sg.type = str(gd.get("groupType"));
                            sg.frequency = str(gd.get("frequency"));
                            sg.state = str(gd.get("state"));
                            for (String k : new String[] {"groupPhoto", "groupImage", "imageUrl", "photoUrl", "icon"}) {
                                String v = str(gd.get(k));
                                if (v.startsWith("http")) { sg.photo = v; break; }
                            }
                            sg.members = num(gd.get("membersCount"), 0);
                            sg.round = num(gd.get("currentRound"), 0);
                            sg.slots = num(gd.get("totalSlots"), 0);
                            sg.amount = gd.get("contributionAmount") instanceof Number ? ((Number) gd.get("contributionAmount")).doubleValue() : 0.0;
                            sg.nodeKey = com.harvana.app.trust.NetworkTrust.groupKey(gd);
                            c.sharedGroups.add(sg);
                        } catch (Exception ignored) { }
                    }
                }
                cb.done(c, null);
            } catch (Exception e) { cb.done(null, e.getMessage() == null ? e.toString() : e.getMessage()); }
        });
    }

    @SuppressWarnings("unchecked")
    private static List<String> groupIds(Object raw) {
        List<String> out = new ArrayList<>();
        if (raw instanceof List) for (Object g : (List<Object>) raw) {
            String id = g instanceof Map ? str(((Map<?, ?>) g).get("id")) : str(g);
            if (!id.isEmpty() && !"null".equalsIgnoreCase(id) && !out.contains(id)) out.add(id);
        }
        return out;
    }

    /** The viewer's own switches, as stored on the card. */
    public static void loadMine(Cb<Card> cb) {
        String mine = me();
        if (mine.isEmpty()) { BG.execute(() -> cb.done(null, "Sign in first.")); return; }
        load(mine, cb);
    }

    /** Saves the two switches and the bio, then refreshes the card so a switch turned off removes the value at once. */
    public static void savePrefs(Context context, boolean showAge, boolean showCity, String bio, Cb<Boolean> cb) {
        final Map<String, Object> prefs = new HashMap<>();
        prefs.put("show_age", showAge);
        prefs.put("show_city", showCity);
        String clean = bio == null ? "" : bio.trim();
        if (clean.length() > BIO_MAX) clean = clean.substring(0, BIO_MAX);
        if (clean.isEmpty()) prefs.put("bio", FieldValue.delete()); else prefs.put("bio", clean);
        publish(context, prefs, cb);
    }

    /** Refreshes the counts, location, age and member-since on the card, keeping the member's switches as they are. */
    public static void publishMine(Context context) { publish(context, new HashMap<>(), (ok, e) -> { }); }

    private static void publish(Context context, Map<String, Object> extra, Cb<Boolean> cb) {
        final Context app = context.getApplicationContext();
        BG.execute(() -> {
            try {
                String uid = me();
                if (uid.isEmpty()) { cb.done(false, "Sign in first."); return; }
                final Map<String, Object>[] holder = new Map[1];
                final String[] err = new String[1];
                final java.util.concurrent.CountDownLatch gate = new java.util.concurrent.CountDownLatch(1);
                UserProfileManager.getInstance(app).getProfile(uid, new UserProfileManager.DocCallback() {
                    @Override public void onSuccess(Map<String, Object> data) { holder[0] = data; gate.countDown(); }
                    @Override public void onError(String error) { err[0] = error; gate.countDown(); }
                });
                gate.await(T, TimeUnit.SECONDS);
                Map<String, Object> me = holder[0];
                if (me == null) { cb.done(false, err[0] == null ? "Profile not available." : err[0]); return; }
                String pid = str(me.get("pid"));
                if (!pid.matches("^[0-9]{10}$")) { cb.done(false, "Your account has no public profile yet."); return; }

                DocumentSnapshot existing = Tasks.await(db().collection("PROFILES").document(pid).get(), T, TimeUnit.SECONDS);
                Map<String, Object> old = existing.exists() && existing.getData() != null ? existing.getData() : new HashMap<>();
                boolean showAge = extra.containsKey("show_age") ? Boolean.TRUE.equals(extra.get("show_age")) : Boolean.TRUE.equals(old.get("show_age"));
                boolean showCity = extra.containsKey("show_city") ? Boolean.TRUE.equals(extra.get("show_city")) : Boolean.TRUE.equals(old.get("show_city"));

                Map<String, Object> out = new LinkedHashMap<>(extra);
                out.put("pid", pid);
                out.put("show_age", showAge);
                out.put("show_city", showCity);
                out.put("country", str(me.get("country")));
                out.put("state", str(me.get("state")));
                int age = showAge ? ageFromDob(str(me.get("dob"))) : -1;
                if (age >= 0) out.put("age", age); else out.put("age", FieldValue.delete());
                String city = str(me.get("city"));
                if (showCity && !city.isEmpty()) out.put("city", city); else out.put("city", FieldValue.delete());
                if (me.get("timestamp") instanceof Number) out.put("member_since", ((Number) me.get("timestamp")).longValue());

                // Counts only, no amounts: from the same portfolio the Home screen uses.
                try {
                    final NodePortfolio.Portfolio[] pf = new NodePortfolio.Portfolio[1];
                    final java.util.concurrent.CountDownLatch pg = new java.util.concurrent.CountDownLatch(1);
                    String wallet = com.harvana.app.wallet.WalletManager.getActiveWalletAddress(app);
                    NodePortfolio.load(app, uid, wallet, p -> { pf[0] = p; pg.countDown(); });
                    pg.await(T, TimeUnit.SECONDS);
                    if (pf[0] != null) {
                        int cycles = 0, loans = 0, pending = 0;
                        synchronized (pf[0].entries) {
                            for (NodePortfolio.Entry e : pf[0].entries) {
                                Position pos = e.position;
                                if (pos == null) continue;
                                if (pos.cycleCompleted) cycles++;
                                if (pos.borrowedHarv > 0) loans++;
                                if (pos.outstandingHarv() > 0) pending++;
                            }
                        }
                        out.put("groups_count", pf[0].groupCount());
                        out.put("cycles_completed", cycles);
                        out.put("loans_count", loans);
                        out.put("loans_pending", pending);
                    }
                } catch (Exception ignored) { }
                out.put("stats_updated", System.currentTimeMillis());

                Tasks.await(db().collection("PROFILES").document(pid).set(out, SetOptions.merge()), T, TimeUnit.SECONDS);
                cb.done(true, null);
            } catch (Exception e) { cb.done(false, e.getMessage() == null ? e.toString() : e.getMessage()); }
        });
    }

    /** A stored phone is country code plus number; when two full numbers were joined, keep the first (Nigerian numbers are +234 and ten digits). */
    public static String tidyPhone(String raw) {
        if (raw == null) return "";
        String digits = raw.replaceAll("[^0-9]", "");
        boolean plus = raw.trim().startsWith("+");
        if (digits.startsWith("234") && digits.length() > 13) digits = digits.substring(0, 13);
        else if (digits.length() > 15) digits = digits.substring(0, 15);
        return digits.isEmpty() ? "" : (plus || digits.startsWith("234") ? "+" : "") + digits;
    }

    /** "Online" within five minutes, otherwise "last seen" with a rough time; empty when the member has no presence record. */
    public static String presence(long lastActiveAt, long now) {
        if (lastActiveAt <= 0) return "";
        long ago = now - lastActiveAt;
        if (ago < 5 * 60 * 1000L) return "online";
        if (ago < 60 * 60 * 1000L) return "last seen " + (ago / 60000L) + " min ago";
        if (ago < 24 * 60 * 60 * 1000L) return "last seen " + (ago / 3600000L) + " h ago";
        if (ago < 2 * 24 * 60 * 60 * 1000L) return "last seen yesterday";
        if (ago < 7 * 24 * 60 * 60 * 1000L) return "last seen within a week";
        return "last seen a while ago";
    }

    /** Whole years from a date of birth; see {@link AgeOf}. */
    public static int ageFromDob(String dob) { return AgeOf.fromDob(dob, Calendar.getInstance()); }
}
