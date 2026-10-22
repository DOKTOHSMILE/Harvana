package com.harvana.app.search;

import android.content.Context;
import android.content.Intent;

import java.util.HashMap;

import com.harvana.app.GroupDetailsActivity;
import com.harvana.app.LearnReadActivity;
import com.harvana.app.MainActivity;
import com.harvana.app.P2pMarketplaceActivity;
import com.harvana.app.ReceiveActivity;
import com.harvana.app.SendAssetActivity;
import com.harvana.app.SettingsActivity;
import com.harvana.app.offline.OfflineCacheManager;
import com.harvana.app.social.FriendsActivity;
import com.harvana.app.social.PublicProfileActivity;
import com.harvana.app.swap.SwapActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * One index for the whole app. Settings and actions are known here. Groups, Learn posts and people come from what this phone has already loaded, then a short network lookup for people.
 */
public final class AppSearch {
    private AppSearch() {
    }

    public static final String ALL = "All";
    public static final String GROUPS = "Groups";
    public static final String LEARN = "Learn";
    public static final String PEOPLE = "People";
    public static final String SETTINGS = "Settings";
    public static final String ACTIONS = "Actions";
    public static final String[] CATEGORIES = { ALL, GROUPS, LEARN, PEOPLE, SETTINGS, ACTIONS };

    public static final class Hit {
        public final String category, title, subtitle;
        public final Intent open;

        Hit(String category, String title, String subtitle, Intent open) {
            this.category = category;
            this.title = title;
            this.subtitle = subtitle;
            this.open = open;
        }
    }

    public interface Callback {
        void onHits(List<Hit> hits);
    }

    public static void query(Context context, String raw, String category, Callback cb) {
        Context app = context.getApplicationContext();
        String q = raw == null ? "" : raw.trim().toLowerCase(Locale.US);
        List<Hit> hits = new ArrayList<>();
        if (want(category, ACTIONS))
            actions(app, q, hits);
        if (want(category, SETTINGS))
            settings(app, q, hits);
        if (want(category, GROUPS))
            groups(app, q, hits);
        if (want(category, LEARN))
            learn(app, q, hits);
        if (!want(category, PEOPLE)) {
            cb.onHits(hits);
            return;
        }
        peopleLocal(app, q, hits);
        peopleNetwork(app, q, network -> {
            List<Hit> all = new ArrayList<>(hits);
            for (Hit h : network)
                if (!contains(all, h))
                    all.add(h);
            cb.onHits(all);
        });
    }

    private static boolean want(String category, String one) {
        return category == null || category.isEmpty() || ALL.equals(category)
                || one.equals(category);
    }

    private static boolean contains(List<Hit> hits, Hit h) {
        for (Hit e : hits)
            if (e.category.equals(h.category) && e.title.equals(h.title))
                return true;
        return false;
    }

    private static boolean match(String q, String... parts) {
        if (q.isEmpty())
            return true;
        StringBuilder hay = new StringBuilder();
        for (String p : parts)
            if (p != null)
                hay.append(p.toLowerCase(Locale.US)).append(' ');
        return hay.toString().contains(q);
    }

    private static void actions(Context app, String q, List<Hit> hits) {
        add(hits, ACTIONS, "Send", "Send a coin", q, new Intent(app, SendAssetActivity.class));
        add(hits, ACTIONS, "Receive", "Show your address", q,
                new Intent(app, ReceiveActivity.class));
        add(hits, ACTIONS, "Swap", "Swap between coins", q, new Intent(app, SwapActivity.class));
        add(hits, ACTIONS, "P2P marketplace", "Buy and sell with people", q,
                new Intent(app, P2pMarketplaceActivity.class));
        add(hits, ACTIONS, "Friends", "People you follow", q,
                new Intent(app, FriendsActivity.class));
        Intent home = new Intent(app, MainActivity.class);
        add(hits, ACTIONS, "Home", "Balances and activity", q, home);
        Intent groups = new Intent(app, MainActivity.class).putExtra("open_fragment", "groups");
        add(hits, ACTIONS, "Groups", "Your savings groups", q, groups);
        Intent learn = new Intent(app, MainActivity.class).putExtra("open_fragment", "learn");
        add(hits, ACTIONS, "Learn Hub", "Lessons and posts", q, learn);
    }

    private static void settings(Context app, String q, List<Hit> hits) {
        String[][] rows = { { "Settings", "Theme, currency, security" },
                { "Privacy", "Who can see you" }, { "Security", "PIN and backup" },
                { "Notifications", "What rings" }, { "Currency", "The money amounts are shown in" },
                { "Theme", "Light, dark, or follow the phone" }, };
        for (String[] row : rows)
            add(hits, SETTINGS, row[0], row[1], q, new Intent(app, SettingsActivity.class));
    }

    @SuppressWarnings("unchecked")
    private static void groups(Context app, String q, List<Hit> hits) {
        List<Map<String, Object>> lists = new ArrayList<>();
        List<HashMap<String, Object>> discover = OfflineCacheManager.loadList(app,
                "discover_groups");
        if (discover != null)
            lists.addAll(discover);
        com.google.firebase.auth.FirebaseUser me = com.google.firebase.auth.FirebaseAuth
                .getInstance().getCurrentUser();
        if (me != null) {
            Map<String, Object> profile = OfflineCacheManager.load(app, "profile_" + me.getUid());
            for (String id : com.harvana.app.firestore.UserProfileManager
                    .groupIds(profile == null ? null : profile.get("groups"))) {
                Map<String, Object> g = OfflineCacheManager.load(app, "group_" + id);
                if (g != null)
                    lists.add(g);
            }
        }
        java.util.HashSet<String> seen = new java.util.HashSet<>();
        for (Map<String, Object> g : lists) {
            String id = str(g, "groupId");
            if (id.isEmpty())
                id = str(g, "_docId");
            String name = str(g, "groupName");
            if (name.isEmpty() || !seen.add(id.isEmpty() ? name : id))
                continue;
            String sub = str(g, "groupType");
            String freq = str(g, "frequency");
            if (!freq.isEmpty())
                sub = sub.isEmpty() ? freq : sub + " · " + freq;
            if (!match(q, name, sub, str(g, "city"), str(g, "state")))
                continue;
            Intent i = new Intent(app, GroupDetailsActivity.class).putExtra("groupId", id);
            hits.add(new Hit(GROUPS, name, sub.isEmpty() ? "Group" : sub, i));
            if (hits.size() > 80)
                return;
        }
    }

    private static void learn(Context app, String q, List<Hit> hits) {
        List<HashMap<String, Object>> posts = OfflineCacheManager.loadList(app, "learn_content");
        if (posts == null)
            return;
        int n = 0;
        for (Map<String, Object> m : posts) {
            String title = str(m, "title");
            String body = str(m, "description");
            if (body.isEmpty())
                body = str(m, "body");
            String cat = str(m, "category");
            if (!match(q, title, body, cat))
                continue;
            Intent i = new Intent(app, LearnReadActivity.class);
            i.putExtra(LearnReadActivity.EXTRA_ID,
                    str(m, "id").isEmpty() ? str(m, "_docId") : str(m, "id"));
            i.putExtra(LearnReadActivity.EXTRA_TITLE, title);
            i.putExtra(LearnReadActivity.EXTRA_BODY, body);
            i.putExtra(LearnReadActivity.EXTRA_CATEGORY, cat);
            i.putExtra(LearnReadActivity.EXTRA_IMAGE, str(m, "imageUrl"));
            i.putExtra(LearnReadActivity.EXTRA_VIDEO, str(m, "videoUrl"));
            i.putExtra(LearnReadActivity.EXTRA_AUTHOR, str(m, "authorUid"));
            hits.add(new Hit(LEARN, title.isEmpty() ? "Learn post" : title,
                    cat.isEmpty() ? "Learn Hub" : cat, i));
            if (++n == 30)
                return;
        }
    }

    private static void peopleLocal(Context app, String q, List<Hit> hits) {
        // The signed-in person, so "me" is findable without a network call.
        com.google.firebase.auth.FirebaseUser me = com.google.firebase.auth.FirebaseAuth
                .getInstance().getCurrentUser();
        if (me == null)
            return;
        Map<String, Object> profile = OfflineCacheManager.load(app, "profile_" + me.getUid());
        String name = profile == null ? ""
                : str(profile, "first_name") + " " + str(profile, "last_name");
        String user = profile == null ? "" : str(profile, "username");
        if (match(q, name, user, "me", "profile")) {
            hits.add(new Hit(PEOPLE, name.trim().isEmpty() ? "You" : name.trim(),
                    user.isEmpty() ? "Your profile" : "@" + user,
                    new Intent(app, PublicProfileActivity.class)
                            .putExtra(PublicProfileActivity.EXTRA_UID, me.getUid())));
        }
    }

    private static void peopleNetwork(Context app, String q, Callback cb) {
        if (q.length() < 2) {
            cb.onHits(new ArrayList<>());
            return;
        }
        com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("PROFILES")
                .orderBy("username").startAt(q).endAt(q + "\uf8ff").limit(12).get()
                .addOnSuccessListener(snap -> {
                    List<Hit> hits = new ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot d : snap.getDocuments()) {
                        String user = d.getString("username");
                        String name = d.getString("name");
                        if (name == null || name.isEmpty())
                            name = user;
                        String uid = d.getString("uid");
                        if (uid == null || uid.isEmpty())
                            uid = d.getId();
                        hits.add(new Hit(PEOPLE, name == null ? "Member" : name,
                                user == null || user.isEmpty() ? "Profile" : "@" + user,
                                new Intent(app, PublicProfileActivity.class)
                                        .putExtra(PublicProfileActivity.EXTRA_UID, uid)));
                    }
                    cb.onHits(hits);
                }).addOnFailureListener(e -> cb.onHits(new ArrayList<>()));
    }

    private static void add(List<Hit> hits, String category, String title, String subtitle,
            String q, Intent open) {
        if (!match(q, title, subtitle))
            return;
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        hits.add(new Hit(category, title, subtitle, open));
    }

    private static String str(Map<String, Object> m, String k) {
        Object v = m == null ? null : m.get(k);
        return v == null ? "" : String.valueOf(v).trim();
    }
}
