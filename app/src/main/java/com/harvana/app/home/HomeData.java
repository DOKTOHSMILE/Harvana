package com.harvana.app.home;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import com.google.firebase.auth.FirebaseAuth;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Everything the Home tab shows apart from the wallet total, loaded in one pass and handed over in one piece.
 *
 * Home used to run a dozen loaders, each painting its own card when its answer came in, several of them twice per
 * open (once from the cache, once from the network) and again on every resume: cards blanked, shimmered and refilled
 * one by one. Now the last snapshot is kept (memory and phone storage, per account) and painted at once when Home
 * opens; a refresh gathers every part in the background and publishes one new snapshot when all parts have answered
 * (or after {@link #TIMEOUT_MS}, keeping the previous value of any part that did not). Home repaints only the sections
 * whose values changed.
 *
 * Sections (JSON, so the snapshot stores as it is): name, groupIds, pending, groups, saved, trust, loans, upcoming,
 * trending, learn. A section that has never loaded is absent, and only then does Home show a placeholder.
 */
public final class HomeData {
    private HomeData() {}

    public interface Listener { void onSnapshot(JSONObject snapshot); }

    private static final String PREFS = "home_snapshot";
    private static final long TIMEOUT_MS = 15_000L;
    private static final long FRESH_MS = 5_000L;             // tab switches within this of the last refresh load nothing
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private static JSONObject last;
    private static String lastUid = "";
    private static long lastAt;
    private static Refresh running;
    private static final List<Listener> listeners = new ArrayList<>();

    public static void listen(Listener l) { if (l != null && !listeners.contains(l)) listeners.add(l); }
    public static void unlisten(Listener l) { listeners.remove(l); }

    /** Sign-out: nothing of the last account stays in memory. */
    public static void forget() { last = null; lastUid = ""; lastAt = 0; running = null; }

    private static String uid() {
        return FirebaseAuth.getInstance().getCurrentUser() == null ? "" : FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    /** The last snapshot for the signed-in person (empty object when there is none yet). Main thread. */
    public static JSONObject cached(Context c) {
        String uid = uid();
        if (last != null && uid.equals(lastUid)) return last;
        last = new JSONObject();
        lastUid = uid;
        lastAt = 0;
        if (c != null && !uid.isEmpty()) {
            try { last = new JSONObject(prefs(c).getString(uid, "{}")); } catch (Exception ignored) { }
        }
        return last;
    }

    /** Loads every part and publishes one snapshot. A refresh already running is joined; a fresh one is not repeated. */
    public static void refresh(Context ctx, boolean force) {
        if (ctx == null) return;
        String uid = uid();
        cached(ctx);
        if (uid.isEmpty()) return;
        if (running != null && uid.equals(running.uid)) return;
        if (!force && System.currentTimeMillis() - lastAt < FRESH_MS) return;
        running = new Refresh(ctx.getApplicationContext(), uid, copy(last));
        running.start();
    }

    private static SharedPreferences prefs(Context c) { return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE); }

    private static JSONObject copy(JSONObject o) {
        try { return new JSONObject(o == null ? "{}" : o.toString()); } catch (Exception e) { return new JSONObject(); }
    }

    private static void publish(Refresh r) {
        if (running != r) return;
        running = null;
        if (!r.uid.equals(uid())) return;                 // signed out or switched account meanwhile
        last = r.draft;
        lastUid = r.uid;
        lastAt = System.currentTimeMillis();
        try { prefs(r.ctx).edit().putString(r.uid, r.draft.toString()).apply(); } catch (Exception ignored) { }
        postDueNotices(r.ctx, r.draft);
        for (Listener l : new ArrayList<>(listeners)) l.onSnapshot(last);
        if (r.healed) refresh(r.ctx, true);              // a membership was repaired: its group belongs in the figures
    }

    /** One "payment due" notice per group per day (the key dedupes repeats). */
    private static void postDueNotices(Context c, JSONObject s) {
        JSONObject up = s.optJSONObject("upcoming");
        JSONArray rows = up == null ? null : up.optJSONArray("rows");
        if (rows == null) return;
        String day = com.harvana.app.notify.DueCopy.dayKey(System.currentTimeMillis());
        for (int i = 0; i < rows.length(); i++) {
            JSONObject r = rows.optJSONObject(i);
            if (r == null || !"PAY".equals(r.optString("action"))) continue;
            String gid = r.optString("groupId");
            com.harvana.app.notify.NotificationCenter.post(c, com.harvana.app.notify.NotificationCenter.PAYMENT,
                    com.harvana.app.notify.DueCopy.key(gid, day),
                    com.harvana.app.notify.DueCopy.title(r.optString("groupName", "Group")),
                    com.harvana.app.notify.DueCopy.body(com.harvana.app.wallet.BalanceFormatter.smartDecimal(r.optDouble("amount", 0)),
                            com.harvana.app.CoinInfo.ticker()), gid, 2);
        }
    }

    /** One refresh: every part runs at once; each answer is applied on the main thread; one publish at the end. */
    private static final class Refresh {
        final Context ctx;
        final String uid;
        final JSONObject draft;
        int left;
        boolean healed, published;

        Refresh(Context ctx, String uid, JSONObject draft) { this.ctx = ctx; this.uid = uid; this.draft = draft; }

        void start() {
            MAIN.postDelayed(this::finish, TIMEOUT_MS);
            left = 1;                                       // the profile; it adds the parts that need the group list
            com.harvana.app.firestore.UserProfileManager.getInstance(ctx).getProfile(uid,
                new com.harvana.app.firestore.UserProfileManager.DocCallback() {
                    @Override public void onSuccess(Map<String, Object> data) { MAIN.post(() -> onProfile(data, true)); }
                    @Override public void onError(String error) {
                        // No profile yet (404) is an empty one; a network failure keeps the saved group list.
                        boolean empty = error != null && com.harvana.app.utils.ErrorText.is(error, "404");
                        MAIN.post(() -> onProfile(null, empty));
                    }
                });
        }

        private void onProfile(Map<String, Object> data, boolean fresh) {
            List<String> ids = new ArrayList<>();
            if (fresh) {
                if (data != null) {
                    put(draft, "name", str(data, "first_name", ""));
                    Object g = data.get("groups");
                    if (g instanceof List) for (Object o : (List<?>) g) if (o != null && !o.toString().trim().isEmpty()) ids.add(o.toString().trim());
                    JSONArray pend = new JSONArray();
                    Object p = data.get("pendingGroupIds");
                    if (p instanceof List) for (Object o : (List<?>) p) if (o != null) pend.put(o.toString());
                    put(draft, "pending", pend);
                }
                put(draft, "groupIds", new JSONArray(ids));
            } else {
                JSONArray saved = draft.optJSONArray("groupIds");
                if (saved != null) for (int i = 0; i < saved.length(); i++) ids.add(saved.optString(i));
            }
            left += 6;
            loadUpcoming(ids);
            loadPortfolio(ids.size());
            loadFirestoreLoans(ids);
            loadTrust();
            loadTrending(new HashSet<>(ids));
            loadLearn();
            if (fresh && data != null) {
                left++;
                com.harvana.app.group.JoinRequestManager.reconcilePending(ctx, uid, removed -> MAIN.post(this::done));
                com.harvana.app.group.JoinRequestManager.selfHealApprovedMemberships(ctx, uid, n -> { if (n > 0) healed = true; });
            }
            done();
        }

        private void done() { if (--left <= 0) finish(); }

        private void finish() {
            if (published) return;
            published = true;
            publish(this);
        }

        // ---- Upcoming payments: only groups with something to do (pay, or a member to remind), two at most.

        private void loadUpcoming(List<String> ids) {
            if (ids.isEmpty()) { put(draft, "upcoming", rowsSection(new ArrayList<>())); done(); return; }
            final List<JSONObject> rows = new ArrayList<>();
            final int[] waiting = {ids.size()};
            final boolean[] network = {false};
            final Runnable one = () -> MAIN.post(() -> {
                if (--waiting[0] > 0) return;
                // A dropped connection keeps the last list rather than showing "nothing due".
                if (!(network[0] && rows.isEmpty())) {
                    Collections.sort(rows, (x, y) -> {
                        int a = rank(x.optString("action")) - rank(y.optString("action"));
                        if (a != 0) return a;
                        int n = x.optString("groupName").compareToIgnoreCase(y.optString("groupName"));
                        return n != 0 ? n : x.optString("groupId").compareTo(y.optString("groupId"));
                    });
                    List<JSONObject> due = new ArrayList<>();
                    for (JSONObject r : rows) if (rank(r.optString("action")) < 2 && due.size() < 2) due.add(r);
                    names(due, () -> { put(draft, "upcoming", rowsSection(due)); done(); });
                    return;
                }
                done();
            });
            final com.harvana.app.firestore.GroupFirestoreManager mgr = com.harvana.app.firestore.GroupFirestoreManager.getInstance(ctx);
            for (String gid : ids) {
                mgr.getGroup(gid, new com.harvana.app.firestore.GroupFirestoreManager.DocCallback() {
                    @Override public void onSuccess(Map<String, Object> g) {
                        if (com.harvana.app.group.GroupKinds.isLegacyRotating(g) || com.harvana.app.group.GroupCloser.isDeleted(g)
                                || com.harvana.app.group.GroupCloser.isClosed(g)) { one.run(); return; }
                        mgr.getMembers(gid, new com.harvana.app.firestore.GroupFirestoreManager.ListCallback() {
                            @Override public void onSuccess(List<Map<String, Object>> members) {
                                mgr.getPayments(gid, new com.harvana.app.firestore.GroupFirestoreManager.ListCallback() {
                                    @Override public void onSuccess(List<Map<String, Object>> payments) { upcomingRow(rows, gid, g, members, payments, one); }
                                    @Override public void onError(String e) { flag(network, e); upcomingRow(rows, gid, g, members, null, one); }
                                });
                            }
                            @Override public void onError(String e) { flag(network, e); one.run(); }
                        });
                    }
                    @Override public void onError(String e) { flag(network, e); one.run(); }
                });
            }
        }

        private void upcomingRow(List<JSONObject> rows, String gid, Map<String, Object> data, List<Map<String, Object>> members,
                                 List<Map<String, Object>> payments, Runnable done) {
            com.harvana.app.group.PaymentDueResolver.Result r = com.harvana.app.group.PaymentDueResolver.resolve(
                    data, members, payments, uid, System.currentTimeMillis(), java.util.TimeZone.getDefault());
            final JSONObject row = new JSONObject();
            put(row, "groupId", gid);
            put(row, "groupName", str(data, "groupName", str(data, "name", "Group")));
            put(row, "groupPhoto", str(data, "groupPhoto", ""));
            put(row, "groupType", str(data, "groupType", ""));
            put(row, "amount", num(data, "contributionAmount"));
            put(row, "frequency", str(data, "frequency", ""));
            put(row, "action", r.action.name());
            put(row, "otherUid", r.otherMemberUid != null ? r.otherMemberUid : "");
            put(row, "statusLine", r.statusLine);
            put(row, "badge", r.badge);
            final Runnable keep = () -> { synchronized (rows) { rows.add(row); } done.run(); };
            // The saved copy of a network group can be out of date (goal reached, circle still forming, round already paid):
            // before offering "Pay", ask the network and apply the rules the Payments tab uses.
            final com.harvana.app.group.GroupIdentity identity = com.harvana.app.group.GroupIdentityResolver.resolve(gid, data);
            boolean checkable = identity != null && identity.onChain() && ("FIXED".equals(identity.family) || "CIRCLE".equals(identity.family));
            if (!"PAY".equals(r.action.name()) || !checkable) { keep.run(); return; }
            final String me = com.harvana.app.wallet.WalletManager.getActiveWalletAddress(ctx);
            final String family = identity.family;
            final String nodeId = identity.protocolId();
            final com.harvana.app.sync.NodeGroupClient.Callback onGroup = new com.harvana.app.sync.NodeGroupClient.Callback() {
                @Override public void onSuccess(JSONObject groupJson) {
                    final Map<String, Object> group = com.harvana.app.utils.JsonMaps.toMap(groupJson);
                    com.harvana.app.sync.NodeGroupClient.Callback onMember = new com.harvana.app.sync.NodeGroupClient.Callback() {
                        @Override public void onSuccess(JSONObject m) { decide(group, com.harvana.app.utils.JsonMaps.toMap(m)); }
                        @Override public void onError(String error) { decide(group, null); }
                    };
                    if (me == null || me.isEmpty()) { decide(group, null); return; }
                    if ("CIRCLE".equals(family)) com.harvana.app.sync.NodeGroupClient.getCircleMember(nodeId, me, onMember);
                    else com.harvana.app.sync.NodeGroupClient.getFixedMember(nodeId, me, onMember);
                }
                @Override public void onError(String error) { keep.run(); }
                private void decide(Map<String, Object> group, Map<String, Object> member) {
                    try {
                        com.harvana.app.engine.GroupSnapshot snap = "CIRCLE".equals(family)
                                ? com.harvana.app.engine.CircleSnapshot.of(group, member)
                                : com.harvana.app.engine.FixedSnapshot.of(group, member, me == null ? "" : me);
                        boolean canPay = false;
                        String reason = "";
                        for (com.harvana.app.engine.GroupAction a : snap.actions) {
                            if (a.enabled && (com.harvana.app.engine.GroupAction.CONTRIBUTE.equals(a.id) || com.harvana.app.engine.GroupAction.FIXED_DEPOSIT.equals(a.id))) canPay = true;
                            if (!a.enabled && reason.isEmpty()) reason = a.label;
                        }
                        if (canPay && "FIXED".equals(family) && member != null
                                && com.harvana.app.group.PaymentDueResolver.depositedThisPeriod(str(data, "frequency", ""), member.get("lastDepositTimestamp"),
                                System.currentTimeMillis(), java.util.TimeZone.getDefault())) {
                            put(row, "action", "NONE");
                            put(row, "badge", "Paid");
                        } else if (!canPay && !snap.actions.isEmpty()) {
                            put(row, "action", "NONE");
                            put(row, "statusLine", reason);
                        }
                    } catch (Exception ignored) { }
                    keep.run();
                }
            };
            if ("CIRCLE".equals(family)) com.harvana.app.sync.NodeGroupClient.getCircle(nodeId, onGroup);
            else com.harvana.app.sync.NodeGroupClient.getFixedGroup(nodeId, onGroup);
        }

        /** "Waiting on <name>" is part of the snapshot, so a Remind row never changes after it is shown. */
        private void names(List<JSONObject> rows, Runnable then) {
            final int[] n = {1};
            final Runnable step = () -> MAIN.post(() -> { if (--n[0] == 0) then.run(); });
            for (JSONObject r : rows) {
                String other = r.optString("otherUid");
                if (!"REMIND".equals(r.optString("action")) || other.isEmpty()) continue;
                n[0]++;
                com.harvana.app.group.MemberNameResolver.resolve(ctx, other, "a member", who -> {
                    put(r, "otherName", who);
                    put(r, "statusLine", "Waiting on " + who);
                    step.run();
                });
            }
            step.run();
        }

        // ---- Groups, total saved and the node's loans: the node's records, in each group's coin.

        private void loadPortfolio(int listed) {
            String wallet = com.harvana.app.wallet.WalletManager.getActiveWalletAddress(ctx);
            com.harvana.app.sync.NodePortfolio.load(ctx, uid, wallet, p -> com.harvana.app.wallet.FxRates.load(ctx,
                    () -> com.harvana.app.wallet.HarvPrice.get(q -> MAIN.post(() -> {
                        int active = p.groupCount();
                        JSONObject g = new JSONObject();
                        put(g, "active", active);
                        put(draft, "groups", g);
                        double usd = p.savedUsd(ctx);
                        String text = Double.isNaN(usd) ? p.savedInGroupCoins() : com.harvana.app.wallet.FxRates.money(ctx, usd);
                        JSONObject s = new JSONObject();
                        put(s, "text", text == null || text.isEmpty() ? "0" : text);
                        put(s, "across", Math.max(active, listed));
                        put(draft, "saved", s);
                        put(draft, "nodeLoans", obj("count", p.loansOpen(), "out", p.outstandingHarv()));
                        done();
                    }))));
        }

        // ---- Loans kept in Firestore (older groups) where I am the borrower.

        private void loadFirestoreLoans(List<String> ids) {
            if (ids.isEmpty()) { put(draft, "fsLoans", obj("count", 0, "out", 0d)); done(); return; }
            final int[] count = {0};
            final double[] out = {0};
            final int[] waiting = {ids.size()};
            final boolean[] network = {false};
            for (String gid : ids) {
                com.harvana.app.firestore.GroupFirestoreManager.getInstance(ctx).getLoans(gid, new com.harvana.app.firestore.GroupFirestoreManager.ListCallback() {
                    @Override public void onSuccess(List<Map<String, Object>> items) {
                        MAIN.post(() -> {
                            if (items != null) for (Map<String, Object> loan : items) {
                                if (!"ACTIVE".equalsIgnoreCase(str(loan, "status", "")) || !uid.equals(str(loan, "borrowerUid", ""))) continue;
                                count[0]++;
                                double principal = num(loan, "principalHarv");
                                if (principal <= 0) principal = num(loan, "amountHarv");
                                out[0] += Math.max(0, principal - num(loan, "repaidAmountHarv"));
                            }
                            end();
                        });
                    }
                    @Override public void onError(String e) { flag(network, e); MAIN.post(this::end); }
                    private void end() {
                        if (--waiting[0] > 0) return;
                        if (!network[0]) put(draft, "fsLoans", obj("count", count[0], "out", out[0]));
                        done();
                    }
                });
            }
        }

        // ---- Trust: the person's one score across linked wallets, else the active wallet's own.

        private void loadTrust() {
            com.harvana.app.trust.ProfileTrust.forget(uid);
            com.harvana.app.trust.ProfileTrust.resolve(uid, card -> {
                if (card.scored()) {
                    trust(card.percent + "%", 100, card.percent,
                            card.trustTier.isEmpty() ? com.harvana.app.trust.ProfileTrust.tierFor(card.percent) : card.trustTier);
                    done();
                    return;
                }
                String address = com.harvana.app.wallet.WalletManager.getActiveWalletAddress(ctx);
                com.harvana.app.trust.NetworkTrust.wallet(address, score -> MAIN.post(() -> {
                    boolean pct = score.percent >= 0;
                    int max = pct ? 100 : com.harvana.app.trust.NetworkTrust.MAX;
                    trust(pct ? score.percent + "%" : score.published ? String.valueOf(score.score) : "—", max,
                            Math.max(0, Math.min(max, pct ? score.percent : score.score)),
                            pct ? (score.tier.isEmpty() ? "trust score" : score.tier) : (score.summary.isEmpty() ? "of 26" : score.summary));
                    done();
                }));
            });
        }

        private void trust(String value, int max, int progress, String sub) {
            JSONObject t = new JSONObject();
            put(t, "value", value);
            put(t, "max", max);
            put(t, "progress", progress);
            put(t, "sub", sub);
            put(draft, "trust", t);
        }

        // ---- Trending public groups: most members first, two shown, with their trust line already resolved.

        private void loadTrending(Set<String> mine) {
            com.harvana.app.firestore.GroupFirestoreManager.getInstance(ctx).queryGroups("visibility", "Public", 30,
                new com.harvana.app.firestore.GroupFirestoreManager.ListCallback() {
                    @Override public void onSuccess(List<Map<String, Object>> list) {
                        List<Map<String, Object>> sorted = new ArrayList<>();
                        if (list != null) for (Map<String, Object> g : list) {
                            String gid = str(g, "_docId", str(g, "groupId", str(g, "id", "")));
                            if (mine.contains(gid) || uid.equals(str(g, "creatorId", ""))) continue;
                            if (com.harvana.app.group.GroupKinds.isLegacyRotating(g) || com.harvana.app.group.GroupCloser.isClosed(g)
                                    || com.harvana.app.group.GroupCloser.isDeleted(g)) continue;
                            sorted.add(g);
                        }
                        Collections.sort(sorted, (a, b) -> {
                            int m = Double.compare(num(b, "membersCount"), num(a, "membersCount"));
                            if (m != 0) return m;
                            int n = str(a, "groupName", "").compareToIgnoreCase(str(b, "groupName", ""));
                            return n != 0 ? n : str(a, "_docId", "").compareTo(str(b, "_docId", ""));
                        });
                        final List<JSONObject> rows = new ArrayList<>();
                        for (int i = 0; i < Math.min(2, sorted.size()); i++) {
                            Map<String, Object> g = sorted.get(i);
                            JSONObject r = new JSONObject();
                            int members = (int) num(g, "membersCount");
                            if (members <= 0) members = (int) num(g, "totalSlots");
                            put(r, "groupId", str(g, "_docId", str(g, "groupId", str(g, "id", ""))));
                            put(r, "name", str(g, "groupName", str(g, "name", "Public group")));
                            put(r, "photo", str(g, "groupPhoto", ""));
                            put(r, "type", str(g, "groupType", ""));
                            put(r, "meta", str(g, "location", str(g, "city", "Nigeria")) + " · " + members + " members · " + str(g, "frequency", "Weekly"));
                            put(r, "nodeId", com.harvana.app.trust.NetworkTrust.groupKey(g));
                            put(r, "trust", "Not on the network");
                            rows.add(r);
                        }
                        final int[] n = {1};
                        final Runnable step = () -> MAIN.post(() -> { if (--n[0] == 0) { put(draft, "trending", rowsSection(rows)); done(); } });
                        for (JSONObject r : rows) {
                            String nodeId = r.optString("nodeId");
                            if (nodeId.isEmpty()) continue;
                            n[0]++;
                            com.harvana.app.trust.NetworkTrust.group(nodeId, score -> {
                                if (score.published && !score.summary.isEmpty()) {
                                    put(r, "trust", score.summary);
                                    if (score.percent >= 65 || score.summary.startsWith("High trust")) put(r, "badge", score.percent >= 0 ? score.tier : "High trust");
                                }
                                step.run();
                            });
                        }
                        step.run();
                    }
                    @Override public void onError(String e) { MAIN.post(() -> done()); }
                });
        }

        // ---- Learn Hub: the two newest public posts (or my own).

        private void loadLearn() {
            com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("LEARN_CONTENT").get().addOnCompleteListener(task -> {
                if (!task.isSuccessful() || task.getResult() == null) { done(); return; }
                List<Map<String, Object>> posts = new ArrayList<>();
                for (com.google.firebase.firestore.QueryDocumentSnapshot d : task.getResult()) {
                    Map<String, Object> m = com.harvana.app.firestore.GroupFirestoreManager.readFromSdk(d.getData());
                    String v = m.get("visibility") == null ? "" : String.valueOf(m.get("visibility"));
                    if (uid.equals(String.valueOf(m.get("authorUid"))) || v.isEmpty() || "public".equals(v)) posts.add(m);
                }
                Collections.sort(posts, (x, y) -> Long.compare((long) num(y, "createdAt"), (long) num(x, "createdAt")));
                List<JSONObject> rows = new ArrayList<>();
                for (int i = 0; i < Math.min(2, posts.size()); i++) {
                    Map<String, Object> m = posts.get(i);
                    JSONObject r = new JSONObject();
                    put(r, "title", str(m, "title", "Learn article"));
                    put(r, "category", str(m, "category", "Finance"));
                    put(r, "views", (long) num(m, "views"));
                    put(r, "at", (long) num(m, "createdAt"));
                    put(r, "image", str(m, "imageUrl", ""));
                    rows.add(r);
                }
                put(draft, "learn", rowsSection(rows));
                done();
            });
        }
    }

    // ---- helpers

    private static int rank(String action) { return "PAY".equals(action) ? 0 : "REMIND".equals(action) ? 1 : 2; }

    private static JSONObject rowsSection(List<JSONObject> rows) {
        JSONObject s = new JSONObject();
        put(s, "rows", new JSONArray(rows));
        return s;
    }

    private static void flag(boolean[] network, String error) {
        if (com.harvana.app.offline.NetworkStatus.isConnectivityError(error)) network[0] = true;
    }

    private static JSONObject obj(Object... kv) {
        JSONObject o = new JSONObject();
        for (int i = 0; i + 1 < kv.length; i += 2) put(o, String.valueOf(kv[i]), kv[i + 1]);
        return o;
    }

    static void put(JSONObject o, String k, Object v) { try { o.put(k, v); } catch (Exception ignored) { } }

    static String str(Map<String, Object> m, String k, String d) {
        Object v = m == null ? null : m.get(k);
        if (v == null) return d;
        String s = v.toString().trim();
        return s.isEmpty() || "null".equalsIgnoreCase(s) ? d : s;
    }

    static double num(Map<String, Object> m, String k) {
        Object v = m == null ? null : m.get(k);
        if (v instanceof Number) return ((Number) v).doubleValue();
        try { return v == null ? 0 : Double.parseDouble(v.toString()); } catch (Exception e) { return 0; }
    }
}
