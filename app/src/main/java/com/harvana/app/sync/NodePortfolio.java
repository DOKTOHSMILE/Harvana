package com.harvana.app.sync;

import android.content.Context;

import com.harvana.app.engine.NodePositions;
import com.harvana.app.engine.Position;
import com.harvana.app.firestore.GroupFirestoreManager;
import com.harvana.app.firestore.UserProfileManager;
import com.harvana.app.group.GroupKinds;
import com.harvana.app.utils.JsonMaps;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The signed-in user's groups, split into groups that live on the node (read from the node: the source of
 * truth for money) and older Firestore-only groups (which the old code paths still total up). Totals and the
 * trust score merge both.
 */
public final class NodePortfolio {
    public static final class Entry {
        public String firestoreGroupId, nodeId, name, family;
        /** The group's coin ("HNGN", …); empty for a HARV group made before coins. Position amounts are in it. */
        public String asset = "";
        public Position position;

        /** Dollars per unit of this entry's coin; NaN when unknown. */
        public double usdPerUnit(Context c) {
            if (com.harvana.app.husd.Stable.isStable(asset)) {
                double per = com.harvana.app.wallet.FxRates.perUsd(c, com.harvana.app.husd.Stable.currency(asset));
                return Double.isNaN(per) || per <= 0 ? Double.NaN : 1d / per;
            }
            return com.harvana.app.wallet.HarvPrice.usd(c, com.harvana.app.wallet.HarvPrice.peek());
        }
    }

    public static final class Portfolio {
        public final List<Entry> entries = Collections.synchronizedList(new ArrayList<>());
        public final List<String> legacyGroupIds = new ArrayList<>();
        /** Groups the app shows: those on the network that this wallet is in (old Firestore-only groups no longer count). */
        public int groupCount() { return entries.size(); }
        public double savedHarv() { double s = 0; synchronized (entries) { for (Entry e : entries) s += e.position.savedHarv; } return s; }
        public double outstandingHarv() { double s = 0; synchronized (entries) { for (Entry e : entries) s += e.position.outstandingHarv(); } return s; }
        /** Loans still open, in dollars, so the profile can show them in the currency chosen in Settings. */
        public double outstandingUsd(Context c) {
            double s = 0;
            synchronized (entries) {
                for (Entry e : entries) {
                    double v = e.position.outstandingHarv();
                    if (v <= 0) continue;
                    double u = e.usdPerUnit(c);
                    if (Double.isNaN(u)) return Double.NaN;
                    s += v * u;
                }
            }
            return s;
        }
        public double borrowedHarv() { double s = 0; synchronized (entries) { for (Entry e : entries) s += e.position.borrowedHarv; } return s; }
        /** Everything saved across groups, in dollars (each group in its own coin); NaN when a price is missing. */
        public double savedUsd(Context c) {
            double s = 0;
            synchronized (entries) {
                for (Entry e : entries) {
                    if (e.position.savedHarv <= 0) continue;
                    double u = e.usdPerUnit(c);
                    if (Double.isNaN(u)) return Double.NaN;
                    s += e.position.savedHarv * u;
                }
            }
            return s;
        }
        /**
         * Everything saved, in the coins the groups are kept in (owner, 13 Oct 2026: a group's savings show in the
         * currency it was set in, whatever the app's display currency): "₦11,080.40", or "₦11,080.40 · $20.00".
         */
        public String savedInGroupCoins() {
            java.util.Map<String, Double> by = new java.util.LinkedHashMap<>();
            synchronized (entries) {
                for (Entry e : entries) {
                    if (e.position.savedHarv <= 0) continue;
                    String k = e.asset == null ? "" : e.asset;
                    Double prev = by.get(k);
                    by.put(k, (prev == null ? 0 : prev) + e.position.savedHarv);
                }
            }
            if (by.isEmpty()) return "";
            StringBuilder out = new StringBuilder();
            for (java.util.Map.Entry<String, Double> m : by.entrySet()) {
                if (out.length() > 0) out.append(" · ");
                String coin = m.getKey();
                if (com.harvana.app.husd.Stable.isStable(coin)) {
                    out.append(com.harvana.app.wallet.FxRates.format(
                            com.harvana.app.wallet.FxRates.symbolFor(com.harvana.app.husd.Stable.currency(coin)), m.getValue()));
                } else {
                    out.append(String.format(java.util.Locale.US, "%,.2f HARV", m.getValue()));
                }
            }
            return out.toString();
        }

        public int loansOpen() { int n = 0; synchronized (entries) { for (Entry e : entries) if (e.position.outstandingHarv() > 0) n++; } return n; }
    }

    public interface Callback { void onDone(Portfolio p); }

    private static Portfolio cached;
    private static long cachedAt;
    private static String cachedFor = "";
    private static final long TTL_MS = 30_000L;

    private NodePortfolio() {}

    public static synchronized void invalidate() { cached = null; }

    @SuppressWarnings("unchecked")
    public static void load(Context ctx, String uid, String wallet, Callback cb) {
        synchronized (NodePortfolio.class) {
            if (cached != null && uid.equals(cachedFor) && System.currentTimeMillis() - cachedAt < TTL_MS) { cb.onDone(cached); return; }
        }
        final Context c = ctx.getApplicationContext();
        UserProfileManager.getInstance(c).getProfile(uid, new UserProfileManager.DocCallback() {
            @Override public void onSuccess(Map<String, Object> profile) {
                List<String> ids = UserProfileManager.groupIds(profile.get("groups"));
                resolve(c, uid, wallet, ids, cb);
            }
            @Override public void onError(String e) { finish(uid, new Portfolio(), cb); }
        });
    }

    private static void resolve(Context c, String uid, String wallet, List<String> ids, Callback cb) {
        final Portfolio p = new Portfolio();
        if (ids.isEmpty()) { finish(uid, p, cb); return; }
        final AtomicInteger left = new AtomicInteger(ids.size());
        final java.util.List<String> gone = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
        final Runnable one = () -> {
            if (left.decrementAndGet() != 0) return;
            if (!gone.isEmpty()) {
                UserProfileManager.getInstance(c).removeGroupsFromProfile(uid, new java.util.ArrayList<>(gone), new UserProfileManager.SimpleCallback() {
                    @Override public void onSuccess() {}
                    @Override public void onError(String error) {}
                });
            }
            finish(uid, p, cb);
        };
        for (String id : ids) {
            GroupFirestoreManager.getInstance(c).getGroup(id, new GroupFirestoreManager.DocCallback() {
                @Override public void onSuccess(Map<String, Object> fs) {
                    // A deleted group leaves the profile; a closed one stops counting (it is on its way out).
                    if (com.harvana.app.group.GroupCloser.isDeleted(fs)) { gone.add(id); one.run(); return; }
                    if (com.harvana.app.group.GroupCloser.isClosed(fs)) { one.run(); return; }
                    final String nodeId = GroupKinds.nodeId(fs);
                    // A group from before the network (Firestore only) is no longer shown anywhere: it leaves the profile
                    // (owner, 20 Oct 2026: "my profile still counts groups the app no longer recognises").
                    if (nodeId.isEmpty()) { synchronized (p.legacyGroupIds) { p.legacyGroupIds.add(id); } gone.add(id); one.run(); return; }
                    fetchPosition(nodeId, wallet, fs, id, p, () -> { gone.add(id); one.run(); }, one);
                }
                @Override public void onError(String e) {
                    // A deleted group is gone. Do not keep counting it. A network error is left alone.
                    if (e != null && (com.harvana.app.utils.ErrorText.is(e, "404") || com.harvana.app.utils.ErrorText.is(e, "NOT_FOUND"))) gone.add(id);
                    one.run();
                }
            });
        }
    }

    /** {@code missing} runs instead of {@code done} when the network has no such group (removed from the chain). */
    private static void fetchPosition(String nodeId, String wallet, Map<String, Object> fs, String fsId, Portfolio p, Runnable missing, Runnable done) {
        final String family = NodeGroupClient.familyOf(nodeId);
        final String name = fs.get("groupName") == null ? "Group" : String.valueOf(fs.get("groupName"));
        if (wallet == null || wallet.isEmpty()) { done.run(); return; }
        NodeGroupClient.Callback onGroup = new NodeGroupClient.Callback() {
            @Override public void onSuccess(JSONObject groupJson) {
                final Map<String, Object> group = JsonMaps.toMap(groupJson);
                NodeGroupClient.Callback onMember = new NodeGroupClient.Callback() {
                    @Override public void onSuccess(JSONObject memberJson) {
                        final Map<String, Object> member = JsonMaps.toMap(memberJson);
                        if ("ACCUM".equals(family) && member.get("currentLineId") != null) {
                            NodeGroupClient.getAccumLine(nodeId, String.valueOf(member.get("currentLineId")), new NodeGroupClient.Callback() {
                                @Override public void onSuccess(JSONObject l) {
                                    // A line kept in a stable coin: its amounts are in that coin even when the group record names none.
                                    String lineCoin = l.optString("asset", "");
                                    if (lineCoin.isEmpty()) lineCoin = String.valueOf(group.get("asset") == null ? "" : group.get("asset"));
                                    if (com.harvana.app.husd.Stable.isStable(lineCoin) && !com.harvana.app.husd.Stable.isStable(String.valueOf(group.get("asset")))) group.put("asset", lineCoin);
                                    Map<String, Object> line = JsonMaps.toMap(l);
                                    if ((line.get("asset") == null || String.valueOf(line.get("asset")).isEmpty()) && com.harvana.app.husd.Stable.isStable(lineCoin)) line.put("asset", lineCoin);
                                    add(p, fsId, nodeId, name, family, group, NodePositions.accum(member, line, System.currentTimeMillis()), done);
                                }
                                @Override public void onError(String e) { add(p, fsId, nodeId, name, family, group, NodePositions.accum(member, null, 0), done); }
                            });
                        } else if ("ACCUM".equals(family)) {
                            add(p, fsId, nodeId, name, family, group, NodePositions.accum(member, null, 0), done);
                        } else if ("CIRCLE".equals(family)) {
                            add(p, fsId, nodeId, name, family, group, NodePositions.circle(group, member), done);
                        } else {
                            add(p, fsId, nodeId, name, family, group, NodePositions.fixed(group, member), done);
                        }
                    }
                    @Override public void onError(String e) { done.run(); } // not a member on the node (yet)
                };
                if ("CIRCLE".equals(family)) NodeGroupClient.getCircleMember(nodeId, wallet, onMember);
                else if ("ACCUM".equals(family)) NodeGroupClient.getAccumMember(nodeId, wallet, onMember);
                else NodeGroupClient.getFixedMember(nodeId, wallet, onMember);
            }
            @Override public void onError(String e) {
                boolean notThere = e != null && (com.harvana.app.utils.ErrorText.is(e, "404") || com.harvana.app.utils.ErrorText.is(e, "NOT_FOUND"));
                (notThere ? missing : done).run();
            }
        };
        if ("CIRCLE".equals(family)) NodeGroupClient.getCircle(nodeId, onGroup);
        else if ("ACCUM".equals(family)) NodeGroupClient.getAccumGroup(nodeId, onGroup);
        else NodeGroupClient.getFixedGroup(nodeId, onGroup);
    }

    private static void add(Portfolio p, String fsId, String nodeId, String name, String family, Map<String, Object> group, Position pos, Runnable done) {
        if (pos.stillIn || pos.savedHarv > 0) {
            Entry e = new Entry();
            e.firestoreGroupId = fsId; e.nodeId = nodeId; e.name = name; e.family = family; e.position = pos;
            Object a = group == null ? null : group.get("asset");
            e.asset = a == null ? "" : String.valueOf(a);
            p.entries.add(e);
        }
        done.run();
    }

    private static synchronized void finish(String uid, Portfolio p, Callback cb) {
        cached = p; cachedAt = System.currentTimeMillis(); cachedFor = uid;
        cb.onDone(p);
    }
}
