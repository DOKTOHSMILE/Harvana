package com.harvana.app.group;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.harvana.app.R;
import com.harvana.app.circle.CircleUi;
import com.harvana.app.engine.AccumSnapshot;
import com.harvana.app.engine.CircleSnapshot;
import com.harvana.app.engine.FixedSnapshot;
import com.harvana.app.engine.GroupAction;
import com.harvana.app.engine.GroupSnapshot;
import com.harvana.app.engine.VoteView;
import com.harvana.app.firestore.GroupFirestoreManager;
import com.harvana.app.sync.NodeGroupClient;
import com.harvana.app.utils.JsonMaps;
import com.harvana.app.wallet.PinPromptHelper;
import com.harvana.app.wallet.WalletManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Actions & votes: everything in a group that needs a decision, in one place. Payments are not here: deposits, contributions and
 * savings live only on the group's Payments tab.
 *
 *   Needs your attention   votes you can cast, join requests, withdrawals waiting for approval, defaulted members
 *   What you can do        ask for a payout, a milestone release or an emergency grant, request a withdrawal, start a new goal
 *   Recent decisions       votes that have closed and how they ended
 *
 * Everything shown comes from the network (or, for join requests, from the group's own record); every action is a signed
 * request that needs the member's PIN.
 */
public class GroupActionsActivity extends AppCompatActivity {
    private String nodeId = "", fsGroupId = "", me = "", family = "", groupName = "";
    private SwipeRefreshLayout refresh;
    private LinearLayout content;

    private Map<String, Object> group, member, line, fsData;
    private final List<Object[]> proposals = new ArrayList<>();      // {kind, id, Map record}
    private final List<Object[]> packRequests = new ArrayList<>();   // {lineId, Map line}
    private final List<Map<String, Object>> defaulted = new ArrayList<>();
    private final List<Map<String, Object>> joinRequests = new ArrayList<>();
    private boolean loadedOnce;
    private boolean notOnNetwork;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        nodeId = orEmpty(getIntent().getStringExtra("nodeGroupId"));
        fsGroupId = orEmpty(getIntent().getStringExtra("firestoreGroupId"));
        family = NodeGroupClient.familyOf(nodeId);
        me = WalletManager.getActiveWalletAddress(this);

        refresh = new SwipeRefreshLayout(this);
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(CircleUi.color(this, R.color.colorBackground));
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int pad = CircleUi.dp(this, 16);
        content.setPadding(pad, CircleUi.dp(this, 8), pad, CircleUi.dp(this, 28));
        scroll.addView(content, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        refresh.addView(scroll);
        refresh.setOnRefreshListener(this::load);
        com.harvana.app.utils.AppBar.setContent(this, refresh, "Actions & votes", "");
        if (!fsGroupId.isEmpty()) {
            GroupFirestoreManager.getInstance(this).getGroup(fsGroupId, new GroupFirestoreManager.DocCallback() {
                @Override public void onSuccess(Map<String, Object> data) {
                    fsData = new java.util.HashMap<>(data);
                    Object n = data.get("groupName");
                    if (n != null && !String.valueOf(n).isEmpty()) {
                        groupName = String.valueOf(n);
                        runOnUiThread(() -> com.harvana.app.utils.AppBar.setSubtitle(GroupActionsActivity.this, groupName));
                    }
                }
                @Override public void onError(String e) { }
            });
        }
        load();
    }

    @Override protected void onResume() {
        super.onResume();
        if (loadedOnce) load();
    }

    // ── loading ────────────────────────────────────────────────────────────

    private void load() {
        refresh.setRefreshing(true);
        if (!loadedOnce) {
            content.removeAllViews();
            content.addView(com.harvana.app.utils.Shimmer.cards(this, 3));
        }
        NodeGroupClient.Callback onGroup = new NodeGroupClient.Callback() {
            @Override public void onSuccess(JSONObject json) {
                group = JsonMaps.toMap(json);
                NodeGroupClient.existsOnNode(family, nodeId, exists -> notOnNetwork = Boolean.FALSE.equals(exists));
                loadMember();
            }
            @Override public void onError(String e) {
                refresh.setRefreshing(false);
                message(e != null && e.contains("404") ? "This group doesn't exist on the network."
                    : "Couldn't load this group right now. Pull down to try again.");
            }
        };
        if ("CIRCLE".equals(family)) NodeGroupClient.getCircle(nodeId, onGroup);
        else if ("ACCUM".equals(family)) NodeGroupClient.getAccumGroup(nodeId, onGroup);
        else if ("FIXED".equals(family)) NodeGroupClient.getFixedGroup(nodeId, onGroup);
        else { refresh.setRefreshing(false); message("Unknown group type."); }
    }

    private void loadMember() {
        if (me == null || me.isEmpty()) { member = null; line = null; loadExtras(); return; }
        NodeGroupClient.Callback cb = new NodeGroupClient.Callback() {
            @Override public void onSuccess(JSONObject json) { member = JsonMaps.toMap(json); loadLine(); }
            @Override public void onError(String e) { member = null; line = null; loadExtras(); }      // 404: not a member
        };
        if ("CIRCLE".equals(family)) NodeGroupClient.getCircleMember(nodeId, me, cb);
        else if ("ACCUM".equals(family)) NodeGroupClient.getAccumMember(nodeId, me, cb);
        else NodeGroupClient.getFixedMember(nodeId, me, cb);
    }

    private void loadLine() {
        Object lid = "ACCUM".equals(family) && member != null ? member.get("currentLineId") : null;
        if (lid == null || String.valueOf(lid).isEmpty()) { line = null; loadExtras(); return; }
        NodeGroupClient.getAccumLine(nodeId, String.valueOf(lid), new NodeGroupClient.Callback() {
            @Override public void onSuccess(JSONObject json) { line = JsonMaps.toMap(json); loadExtras(); }
            @Override public void onError(String e) { line = null; loadExtras(); }
        });
    }

    /** Votes, withdrawal requests, defaults and join requests. Each part is best-effort and the page shows what arrived. */
    private void loadExtras() {
        proposals.clear(); packRequests.clear(); defaulted.clear(); joinRequests.clear();
        final AtomicInteger left = new AtomicInteger(2);
        final Runnable done = () -> { if (left.decrementAndGet() == 0) runOnUiThread(this::render); };

        loadJoinRequests(done);
        if ("CIRCLE".equals(family)) { loadDefaults(done); return; }
        if ("FIXED".equals(family)) {
            NodeGroupClient.listFixedProposals(nodeId, new NodeGroupClient.Callback() {
                @Override public void onSuccess(JSONObject j) { fillListedProposals(j); done.run(); }
                @Override public void onError(String e) { GroupProposalIndex.list(GroupActionsActivity.this, fsGroupId, entries -> loadProposals(entries, done)); }
            });
            return;
        }
        NodeGroupClient.listAccumPendingPacks(nodeId, new NodeGroupClient.Callback() {
            @Override public void onSuccess(JSONObject j) { fillListedPacks(j); done.run(); }
            @Override public void onError(String e) { GroupProposalIndex.list(GroupActionsActivity.this, fsGroupId, entries -> loadProposals(entries, done)); }
        });
    }

    private void loadJoinRequests(final Runnable done) {
        if (fsGroupId.isEmpty()) { done.run(); return; }
        GroupFirestoreManager.getInstance(this).getJoinRequests(fsGroupId, new GroupFirestoreManager.ListCallback() {
            @Override public void onSuccess(List<Map<String, Object>> list) {
                if (list != null) for (Map<String, Object> r : list) {
                    Object st = r == null ? null : r.get("status");
                    if (r != null && (st == null || "PENDING".equalsIgnoreCase(String.valueOf(st)))) joinRequests.add(r);
                }
                done.run();
            }
            @Override public void onError(String e) { done.run(); }
        });
    }

    private void loadDefaults(final Runnable done) {
        NodeGroupClient.getCircleMembers(nodeId, new NodeGroupClient.Callback() {
            @Override public void onSuccess(JSONObject j) {
                JSONArray rows = j == null ? null : j.optJSONArray("members");
                final List<Map<String, Object>> found = new ArrayList<>();
                if (rows != null) for (int i = 0; i < rows.length(); i++) {
                    JSONObject r = rows.optJSONObject(i);
                    if (r == null) continue;
                    String st = r.optString("state", ""), res = r.optString("defaultResolution", "");
                    if (st.startsWith("DEFAULTED") && !"FORGIVEN".equals(res) && !"CURED".equals(res) && found.size() < 10) found.add(JsonMaps.toMap(r));
                }
                if (found.isEmpty()) { done.run(); return; }
                final AtomicInteger pending = new AtomicInteger(found.size());
                for (final Map<String, Object> m : found) {
                    final String addr = String.valueOf(m.get("address"));
                    NodeGroupClient.getForgiveness(nodeId, addr, new NodeGroupClient.Callback() {
                        @Override public void onSuccess(JSONObject p) { m.put("proposal", JsonMaps.toMap(p)); next(); }
                        @Override public void onError(String e) { next(); }
                        private void next() {
                            if (me != null && addr.equalsIgnoreCase(me)) {
                                NodeGroupClient.getCureOffer(nodeId, addr, new NodeGroupClient.Callback() {
                                    @Override public void onSuccess(JSONObject o) { m.put("cureOffer", JsonMaps.toMap(o)); finish(); }
                                    @Override public void onError(String e) { finish(); }
                                });
                            } else finish();
                        }
                        private void finish() {
                            synchronized (defaulted) { if (!defaulted.contains(m)) defaulted.add(m); }
                            if (pending.decrementAndGet() == 0) done.run();
                        }
                    });
                }
            }
            @Override public void onError(String e) { done.run(); }
        });
    }

    private void fillListedProposals(JSONObject j) {
        JSONArray arr = j == null ? null : j.optJSONArray("proposals");
        if (arr == null) return;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject row = arr.optJSONObject(i);
            if (row != null) proposals.add(new Object[]{row.optString("kind", ""), row.optString("id", ""), JsonMaps.toMap(row)});
        }
    }

    private void fillListedPacks(JSONObject j) {
        JSONArray arr = j == null ? null : j.optJSONArray("pendingPacks");
        if (arr == null) return;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject row = arr.optJSONObject(i);
            if (row == null) continue;
            Map<String, Object> rowMap = JsonMaps.toMap(row);
            rowMap.put("status", "PENDING_PACK_APPROVAL");
            packRequests.add(new Object[]{row.optString("lineId", ""), rowMap});
        }
    }

    private void loadProposals(List<GroupProposalIndex.Entry> entries, final Runnable done) {
        if (entries.isEmpty()) { done.run(); return; }
        final AtomicInteger n = new AtomicInteger(entries.size());
        for (final GroupProposalIndex.Entry e : entries) {
            NodeGroupClient.Callback cb = new NodeGroupClient.Callback() {
                @Override public void onSuccess(JSONObject j) {
                    synchronized (proposals) {
                        if (GroupProposalIndex.PACK.equals(e.kind)) {
                            Map<String, Object> m = JsonMaps.toMap(j);
                            m.put("status", "PENDING_PACK_APPROVAL");
                            packRequests.add(new Object[]{e.id, m});
                        } else proposals.add(new Object[]{e.kind, e.id, JsonMaps.toMap(j)});
                    }
                    if (n.decrementAndGet() == 0) done.run();
                }
                @Override public void onError(String err) { if (n.decrementAndGet() == 0) done.run(); }
            };
            switch (e.kind) {
                case GroupProposalIndex.DISBURSEMENT: NodeGroupClient.getFixedDisbursementRequest(nodeId, e.id, cb); break;
                case GroupProposalIndex.MILESTONE: NodeGroupClient.getFixedMilestone(nodeId, e.id, cb); break;
                case GroupProposalIndex.GRANT: NodeGroupClient.getFixedGrant(nodeId, e.id, cb); break;
                case GroupProposalIndex.PACK: NodeGroupClient.getAccumLine(nodeId, e.id, cb); break;
                default: if (n.decrementAndGet() == 0) done.run();
            }
        }
    }

    // ── page ───────────────────────────────────────────────────────────────

    private GroupSnapshot snapshot() {
        if ("CIRCLE".equals(family)) return CircleSnapshot.of(group, member);
        if ("ACCUM".equals(family)) return AccumSnapshot.of(group, member, line, me == null ? "" : me);
        return FixedSnapshot.of(group, member, me == null ? "" : me);
    }

    private boolean iAmAdmin() {
        return group != null && me != null && !me.isEmpty() && me.equalsIgnoreCase(str(group, "creatorAddress"));
    }

    private boolean iAmTreasurer() {
        return group != null && me != null && !me.isEmpty() && me.equalsIgnoreCase(str(group, "treasurerAddress"));
    }

    private boolean isMemberVoteGroup() {
        return fsData != null && "MEMBER_VOTE".equals(str(fsData, "joinApprovalMethod"));
    }

    private boolean iAmFirestoreAdmin() {
        com.google.firebase.auth.FirebaseUser u = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        return u != null && fsData != null && u.getUid().equals(str(fsData, "creatorId"));
    }

    private void render() {
        loadedOnce = true;
        refresh.setRefreshing(false);
        content.removeAllViews();
        GroupSnapshot s = snapshot();

        if (notOnNetwork) {
            LinearLayout warn = CircleUi.card(this);
            warn.addView(CircleUi.text(this, "Not on the network", 15, R.color.colorWarning, true));
            warn.addView(CircleUi.text(this, "The network no longer has this group, so votes and requests for it will be refused.", 12.5f, R.color.colorTextSecondary, false));
            content.addView(warn);
        }

        // Which things are waiting on this person
        List<Object[]> openVotes = new ArrayList<>(), closed = new ArrayList<>();
        for (Object[] p : proposals) {
            @SuppressWarnings("unchecked") Map<String, Object> rec = (Map<String, Object>) p[2];
            VoteView v = VoteView.of("", rec, me == null ? "" : me);
            (v.open ? openVotes : closed).add(p);
        }
        sortNewestFirst(openVotes); sortNewestFirst(closed);
        List<Object[]> approvals = new ArrayList<>();
        if (iAmAdmin() || iAmTreasurer()) for (Object[] p : packRequests) {
            @SuppressWarnings("unchecked") Map<String, Object> ln = (Map<String, Object>) p[1];
            if ("PENDING_PACK_APPROVAL".equals(str(ln, "status"))) approvals.add(p);
        }
        boolean canDecideJoin = isMemberVoteGroup() || iAmFirestoreAdmin();
        int joinCount = canDecideJoin ? joinRequests.size() : 0;
        int mineToVote = 0;
        for (Object[] p : openVotes) {
            @SuppressWarnings("unchecked") VoteView v = VoteView.of("", (Map<String, Object>) p[2], me == null ? "" : me);
            if (v.eligible && !v.alreadyVoted) mineToVote++;
        }
        int attention = mineToVote + joinCount + approvals.size() + defaulted.size();

        content.addView(summaryCard(s, attention, openVotes.size(), joinCount, approvals.size(), defaulted.size()));

        // 1. Needs your attention
        boolean any = !openVotes.isEmpty() || joinCount > 0 || !approvals.isEmpty() || !defaulted.isEmpty();
        content.addView(sectionHeader("Needs your attention", attention == 0 ? "" : String.valueOf(attention)));
        if (!any) {
            content.addView(emptyCard("Nothing is waiting on you",
                "Votes, join requests and approvals appear here as soon as someone asks. You will also get a notification."));
        }
        for (Object[] p : openVotes) content.addView(voteCard((String) p[0], (String) p[1], castMap(p[2]), true));
        if (joinCount > 0) for (Map<String, Object> r : joinRequests) content.addView(joinRequestCard(r));
        for (Object[] p : approvals) content.addView(withdrawalCard((String) p[0], castMap(p[1])));
        renderDefaults();

        // 2. What you can do
        List<GroupAction> doable = new ArrayList<>();
        for (GroupAction a : s.actions) if (a.enabled && isRequestAction(a.id)) doable.add(a);
        boolean newGoal = "FIXED".equals(family) && me != null && !me.isEmpty() && member != null && !member.isEmpty()
            && ("COMPLETED".equals(str(group, "state")) || (num(group, "targetGoalAmountRaw") > 0 && num(group, "raisedAmountRaw") >= num(group, "targetGoalAmountRaw")));
        if (!doable.isEmpty() || newGoal) {
            content.addView(sectionHeader("What you can do", ""));
            for (GroupAction a : doable) content.addView(actionCard(a));
            if (newGoal) content.addView(newGoalCard());
        }

        // 3. Recent decisions
        if (!closed.isEmpty()) {
            content.addView(sectionHeader("Recent decisions", String.valueOf(closed.size())));
            for (Object[] p : closed) content.addView(voteCard((String) p[0], (String) p[1], castMap(p[2]), false));
        }

        TextView foot = CircleUi.text(this, "Deposits, contributions and savings are on the group's Payments tab.", 12, R.color.colorTextTertiary, false);
        foot.setPadding(CircleUi.dp(this, 4), CircleUi.dp(this, 18), CircleUi.dp(this, 4), 0);
        content.addView(foot);
        Button copy = button("Copy group ID", false, v -> copy(nodeId));
        content.addView(copy);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object o) { return (Map<String, Object>) o; }

    private void sortNewestFirst(List<Object[]> list) {
        Collections.sort(list, (x, y) -> Long.compare(num(castMap(y[2]), "requestedTimestamp"), num(castMap(x[2]), "requestedTimestamp")));
    }

    private static boolean isRequestAction(String id) {
        return GroupAction.FIXED_REQUEST_DISBURSEMENT.equals(id) || GroupAction.FIXED_REQUEST_MILESTONE.equals(id)
            || GroupAction.FIXED_REQUEST_GRANT.equals(id) || GroupAction.ACCUM_PACK.equals(id)
            || GroupAction.ACCUM_CLAIM.equals(id) || GroupAction.WITHDRAW_BOND.equals(id);
    }

    // ── summary ────────────────────────────────────────────────────────────

    private View summaryCard(GroupSnapshot s, int attention, int openVotes, int joinCount, int approvals, int defaults) {
        LinearLayout card = CircleUi.card(this);
        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.addView(CircleUi.text(this, groupName.isEmpty() ? titleFor(family) : groupName, 17, R.color.colorTextPrimary, true));
        titles.addView(CircleUi.text(this, titleFor(family) + " · " + role(), 12.5f, R.color.colorTextSecondary, false));
        top.addView(titles, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        top.addView(CircleUi.chip(this, s.state.isEmpty() ? "ACTIVE" : s.state, CircleUi.stateColor(s.state)));
        card.addView(top);

        LinearLayout stats = new LinearLayout(this);
        stats.setPadding(0, CircleUi.dp(this, 14), 0, 0);
        stats.addView(stat(String.valueOf(openVotes), "Open votes"), statParams());
        stats.addView(stat(String.valueOf(joinCount), "Join requests"), statParams());
        stats.addView(stat(String.valueOf(approvals + defaults), "To approve"), statParams());
        card.addView(stats);

        String hint = attention == 0 ? "You are up to date."
            : attention == 1 ? "1 thing needs you." : attention + " things need you.";
        TextView h = CircleUi.text(this, hint, 12.5f, attention == 0 ? R.color.colorSuccess : R.color.colorWarning, true);
        h.setPadding(0, CircleUi.dp(this, 12), 0, 0);
        card.addView(h);
        return card;
    }

    private LinearLayout.LayoutParams statParams() { return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f); }

    private View stat(String value, String label) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setGravity(Gravity.CENTER_HORIZONTAL);
        c.addView(CircleUi.text(this, value, 22, R.color.colorTextPrimary, true));
        c.addView(CircleUi.text(this, label, 11.5f, R.color.colorTextSecondary, false));
        return c;
    }

    private String role() {
        if (iAmAdmin() || iAmFirestoreAdmin()) return "You are the admin";
        if (member != null && !member.isEmpty()) return "You are a member";
        return "You are not a member";
    }

    private View sectionHeader(String title, String count) {
        LinearLayout h = new LinearLayout(this);
        h.setGravity(Gravity.CENTER_VERTICAL);
        h.setPadding(CircleUi.dp(this, 4), CircleUi.dp(this, 22), CircleUi.dp(this, 4), CircleUi.dp(this, 0));
        h.addView(CircleUi.text(this, title.toUpperCase(Locale.US), 11.5f, R.color.colorTextTertiary, true),
            new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (!count.isEmpty()) h.addView(CircleUi.chip(this, count, R.color.colorAccent));
        return h;
    }

    private View emptyCard(String title, String body) {
        LinearLayout c = CircleUi.card(this);
        c.setGravity(Gravity.CENTER_HORIZONTAL);
        c.addView(CircleUi.text(this, title, 14.5f, R.color.colorTextPrimary, true));
        TextView t = CircleUi.text(this, body, 12.5f, R.color.colorTextSecondary, false);
        t.setGravity(Gravity.CENTER_HORIZONTAL);
        t.setPadding(0, CircleUi.dp(this, 4), 0, 0);
        c.addView(t);
        return c;
    }

    // ── votes ──────────────────────────────────────────────────────────────

    private View voteCard(final String kind, final String id, Map<String, Object> rec, boolean openList) {
        String label = GroupProposalIndex.DISBURSEMENT.equals(kind) ? "Payout request"
            : GroupProposalIndex.MILESTONE.equals(kind) ? "Milestone release" : "Emergency grant";
        VoteView v = VoteView.of(label, rec, me == null ? "" : me);
        LinearLayout c = CircleUi.card(this);

        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.addView(CircleUi.text(this, label, 15, R.color.colorTextPrimary, true), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        head.addView(CircleUi.chip(this, statusLabel(v), statusColor(v)));
        c.addView(head);

        long amt = num(rec, "amountRaw");
        if (amt > 0) {
            TextView a = CircleUi.text(this, CircleUi.harv(amt), 22, R.color.colorTextPrimary, true);
            a.setPadding(0, CircleUi.dp(this, 6), 0, 0);
            c.addView(a);
        }
        String why = !str(rec, "description").isEmpty() ? str(rec, "description") : str(rec, "reasonDescription");
        if (!why.isEmpty()) {
            TextView w = CircleUi.text(this, "“" + why + "”", 13.5f, R.color.colorTextSecondary, false);
            w.setPadding(0, CircleUi.dp(this, 6), 0, 0);
            c.addView(w);
        }
        String by = !str(rec, "requestedBy").isEmpty() ? str(rec, "requestedBy") : str(rec, "requesterAddress");
        long when = num(rec, "requestedTimestamp");
        if (!by.isEmpty() || when > 0) {
            String who = by.isEmpty() ? "" : (by.equalsIgnoreCase(me) ? "You" : shortAddr(by));
            TextView m = CircleUi.text(this, (who.isEmpty() ? "" : "Asked by " + who) + (who.isEmpty() || when <= 0 ? "" : " · ") + (when > 0 ? date(when) : ""),
                11.5f, R.color.colorTextTertiary, false);
            m.setPadding(0, CircleUi.dp(this, 6), 0, 0);
            c.addView(m);
        }

        if (v.votersTotal > 0) {
            LinearProgressIndicator bar = new LinearProgressIndicator(this);
            bar.setMax(100);
            bar.setProgress(Math.min(100, v.yesPercent));
            bar.setIndicatorColor(CircleUi.color(this, v.open ? R.color.colorAccent : R.color.colorSuccess));
            bar.setTrackColor(CircleUi.color(this, R.color.colorCardStroke));
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            bp.topMargin = CircleUi.dp(this, 12);
            c.addView(bar, bp);
            c.addView(CircleUi.text(this, v.yesPercent + "% yes of the vote · needs " + v.thresholdPercent + "% · "
                + v.votersYes + " of " + v.votersTotal + " voters said yes", 12, R.color.colorTextSecondary, false));
        }

        if (v.open) {
            if (!v.eligible) {
                c.addView(CircleUi.text(this, "You can't vote on this one. Only the admin and the most active members can.", 12, R.color.colorTextTertiary, false));
            } else {
                String mine = v.alreadyVoted ? "You voted " + (v.myVoteYes ? "yes" : "no") + ". You can change it until it closes." : "Your vote is needed.";
                TextView t = CircleUi.text(this, mine, 12.5f, v.alreadyVoted ? R.color.colorTextSecondary : R.color.colorWarning, true);
                t.setPadding(0, CircleUi.dp(this, 10), 0, 0);
                c.addView(t);
                LinearLayout row = new LinearLayout(this);
                row.addView(voteButton("Vote yes", true, v2 -> vote(kind, id, true, label)), buttonCell(true));
                row.addView(voteButton("Vote no", false, v2 -> vote(kind, id, false, label)), buttonCell(false));
                c.addView(row);
            }
        }
        return c;
    }

    private String statusLabel(VoteView v) {
        if (v.open) return "OPEN";
        switch (v.status) {
            case "APPROVED": return "PASSED";
            case "COMPLETED": return "PAID OUT";
            case "REJECTED": return "REJECTED";
            case "FAILED_INSUFFICIENT_FUNDS": return "NOT ENOUGH FUNDS";
            default: return v.status.replace('_', ' ');
        }
    }

    private int statusColor(VoteView v) {
        if (v.open) return R.color.colorWarning;
        return "APPROVED".equals(v.status) || "COMPLETED".equals(v.status) ? R.color.colorSuccess : R.color.colorError;
    }

    private void vote(final String kind, final String id, final boolean yes, String label) {
        java.util.ArrayList<String[]> rows = new java.util.ArrayList<>();
        if (!groupName.isEmpty()) rows.add(new String[]{"Group", groupName});
        rows.add(new String[]{"Vote on", label});
        rows.add(new String[]{"Your vote", yes ? "Yes" : "No"});
        rows.add(new String[]{"Network fee", "Taken from your wallet when this is sent"});
        PaymentConfirmSheet.reviewThenAuthorize(this, yes ? "Vote yes" : "Vote no", "", rows, () -> {
            NodeGroupClient.Callback cb = result(yes ? "Your yes vote is in" : "Your no vote is in");
            if (GroupProposalIndex.DISBURSEMENT.equals(kind)) NodeGroupClient.voteFixedDisbursement(getApplicationContext(), me, nodeId, id, yes, cb);
            else if (GroupProposalIndex.MILESTONE.equals(kind)) NodeGroupClient.voteFixedMilestone(getApplicationContext(), me, nodeId, id, yes, cb);
            else NodeGroupClient.voteFixedGrant(getApplicationContext(), me, nodeId, id, yes, cb);
        });
    }

    // ── join requests ──────────────────────────────────────────────────────

    private View joinRequestCard(final Map<String, Object> req) {
        final String reqUid = !str(req, "requesterUid").isEmpty() ? str(req, "requesterUid") : str(req, "memberUid");
        final String docId = !str(req, "_docId").isEmpty() ? str(req, "_docId") : reqUid;
        final boolean vote = isMemberVoteGroup();
        final String myUid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() == null ? ""
            : com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();

        LinearLayout c = CircleUi.card(this);
        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        final TextView badge = CircleUi.text(this, "?", 14, R.color.colorTextOnAccent, true);
        badge.setGravity(Gravity.CENTER);
        badge.setBackgroundResource(R.drawable.bg_avatar);
        LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams(CircleUi.dp(this, 40), CircleUi.dp(this, 40));
        bl.setMarginEnd(CircleUi.dp(this, 12));
        head.addView(badge, bl);
        MemberPhoto.apply(badge, reqUid);
        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        final TextView name = CircleUi.text(this, "Someone", 15, R.color.colorTextPrimary, true);
        titles.addView(name);
        titles.addView(CircleUi.text(this, "Wants to join " + (groupName.isEmpty() ? "this group" : groupName), 12, R.color.colorTextSecondary, false));
        head.addView(titles, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        head.addView(CircleUi.chip(this, vote ? "MEMBERS VOTE" : "ADMIN DECIDES", R.color.colorWarning));
        c.addView(head);
        MemberNameResolver.resolve(this, reqUid, "Someone", full -> {
            name.setText(full);
            if (!full.isEmpty()) badge.setText(full.substring(0, 1).toUpperCase(Locale.US));
        });

        String msg = str(req, "message");
        if (!msg.isEmpty()) {
            TextView m = CircleUi.text(this, "“" + msg + "”", 13, R.color.colorTextSecondary, false);
            m.setPadding(0, CircleUi.dp(this, 8), 0, 0);
            c.addView(m);
        }
        long when = num(req, "createdAt");
        if (when > 0) {
            TextView t = CircleUi.text(this, "Asked " + date(when), 11.5f, R.color.colorTextTertiary, false);
            t.setPadding(0, CircleUi.dp(this, 6), 0, 0);
            c.addView(t);
        }

        if (vote) {
            Object votes = req.get("votes");
            boolean voted = votes instanceof Map && ((Map<?, ?>) votes).containsKey(myUid);
            if (reqUid.equals(myUid)) {
                c.addView(CircleUi.text(this, "This is your own request. Other members vote on it.", 12, R.color.colorTextTertiary, false));
            } else if (voted) {
                c.addView(CircleUi.text(this, "You have voted on this request.", 12.5f, R.color.colorTextSecondary, true));
            } else {
                int total = (int) num(fsData, "membersCount");
                c.addView(CircleUi.text(this, "Passes when at least 60% of the " + Math.max(1, total) + (total == 1 ? " member votes" : " members vote") + " yes"
                    + (total <= 1 ? ", so your Yes is enough." : "."), 12, R.color.colorTextSecondary, false));
                LinearLayout row = new LinearLayout(this);
                row.addView(voteButton("Vote yes", true, v -> castJoinVote(reqUid, myUid, true)), buttonCell(true));
                row.addView(voteButton("Vote no", false, v -> castJoinVote(reqUid, myUid, false)), buttonCell(false));
                c.addView(row);
            }
        } else {
            LinearLayout row = new LinearLayout(this);
            row.addView(voteButton("Accept", true, v -> acceptJoin(docId, reqUid)), buttonCell(true));
            row.addView(voteButton("Reject", false, v -> rejectJoin(docId)), buttonCell(false));
            c.addView(row);
        }
        return c;
    }

    private void castJoinVote(String reqUid, String myUid, boolean yes) {
        GroupFirestoreManager.getInstance(this).castJoinVote(fsGroupId, reqUid, myUid, yes, new GroupFirestoreManager.VoteCallback() {
            @Override public void onVoteCast(String resolution, int yesVotes, int noVotes, int total) {
                String msg = "APPROVED".equals(resolution) ? "Approved by vote. The member was added."
                    : "REJECTED".equals(resolution) ? "Rejected by vote." : "Vote recorded.";
                toast(msg); runOnUiThread(GroupActionsActivity.this::load);
            }
            @Override public void onError(String error) { toast(error == null ? "The vote could not be sent." : error); runOnUiThread(GroupActionsActivity.this::load); }
        });
    }

    private void acceptJoin(String docId, String reqUid) {
        int slot = (int) num(fsData, "membersCount") + 1;
        JoinRequestManager.approve(this, fsGroupId, docId, reqUid, slot, new JoinRequestManager.Callback() {
            @Override public void onSuccess(String message) { toast(message == null ? "Accepted" : message); runOnUiThread(GroupActionsActivity.this::load); }
            @Override public void onError(String error) { toast(error); runOnUiThread(GroupActionsActivity.this::load); }
        });
    }

    private void rejectJoin(String docId) {
        JoinRequestManager.reject(this, fsGroupId, docId, new JoinRequestManager.Callback() {
            @Override public void onSuccess(String message) { toast(message == null ? "Rejected" : message); runOnUiThread(GroupActionsActivity.this::load); }
            @Override public void onError(String error) { toast(error); runOnUiThread(GroupActionsActivity.this::load); }
        });
    }

    // ── withdrawals and defaults ───────────────────────────────────────────

    private View withdrawalCard(final String lineId, final Map<String, Object> ln) {
        LinearLayout c = CircleUi.card(this);
        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.addView(CircleUi.text(this, "Withdrawal request", 15, R.color.colorTextPrimary, true), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        head.addView(CircleUi.chip(this, "NEEDS APPROVAL", R.color.colorWarning));
        c.addView(head);
        TextView a = CircleUi.text(this, CircleUi.harv(num(ln, "amountPaidRaw")), 22, R.color.colorTextPrimary, true);
        a.setPadding(0, CircleUi.dp(this, 6), 0, 0);
        c.addView(a);
        String who = str(ln, "memberAddress");
        c.addView(CircleUi.text(this, (who.isEmpty() ? "A member" : shortAddr(who)) + " asked to take their savings out of the group. Approving sends the money to them.",
            12.5f, R.color.colorTextSecondary, false));
        GroupAction approve = new GroupAction(GroupAction.ACCUM_APPROVE_PACK, CircleUi.harv(num(ln, "amountPaidRaw")), true, lineId);
        Button b = button("Approve withdrawal", true, v -> NodePayActions.run(ctx((Button) v), approve));
        c.addView(b);
        return c;
    }

    @SuppressWarnings("unchecked")
    private void renderDefaults() {
        if (!"CIRCLE".equals(family) || defaulted.isEmpty()) return;
        boolean admin = iAmAdmin();
        for (final Map<String, Object> m : defaulted) {
            final String addr = String.valueOf(m.get("address"));
            LinearLayout c = CircleUi.card(this);
            boolean mineCase = addr.equalsIgnoreCase(me == null ? "" : me);
            LinearLayout head = new LinearLayout(this);
            head.setGravity(Gravity.CENTER_VERTICAL);
            head.addView(CircleUi.text(this, (mineCase ? "You" : shortAddr(addr)) + " defaulted", 15, R.color.colorTextPrimary, true),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            head.addView(CircleUi.chip(this, "DEFAULT", R.color.colorError));
            c.addView(head);
            if (mineCase) renderCure(c, (Map<String, Object>) m.get("cureOffer"));
            Map<String, Object> proposal = (Map<String, Object>) m.get("proposal");
            boolean open = proposal != null && "PENDING".equals(str(proposal, "status"));
            if (!open) {
                c.addView(CircleUi.text(this, "The group can vote to forgive a default. Forgiving it takes it off this member's record. It does not move any money.",
                    12.5f, R.color.colorTextSecondary, false));
                if (admin && !mineCase) {
                    c.addView(button("Ask the group to forgive", false, v -> PinPromptHelper.authorize(this, "Ask the group to forgive this default", () ->
                        NodeGroupClient.proposeForgiveness(getApplicationContext(), me, nodeId, addr, result("Asked. The voters decide.")))));
                }
            } else {
                long yes = 0, total = 0;
                boolean eligible = false;
                Object weights = proposal.get("voterWeights"), votes = proposal.get("votes");
                if (weights instanceof Map) for (Map.Entry<?, ?> e : ((Map<?, ?>) weights).entrySet()) {
                    long w = e.getValue() instanceof Number ? ((Number) e.getValue()).longValue() : 0;
                    total += w;
                    if (votes instanceof Map && Boolean.TRUE.equals(((Map<?, ?>) votes).get(e.getKey()))) yes += w;
                    if (String.valueOf(e.getKey()).equalsIgnoreCase(me)) eligible = true;
                }
                int pct = total == 0 ? 0 : (int) (yes * 100 / total);
                LinearProgressIndicator bar = new LinearProgressIndicator(this);
                bar.setMax(100);
                bar.setProgress(pct);
                bar.setIndicatorColor(CircleUi.color(this, R.color.colorAccent));
                bar.setTrackColor(CircleUi.color(this, R.color.colorCardStroke));
                LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                bp.topMargin = CircleUi.dp(this, 10);
                c.addView(bar, bp);
                c.addView(CircleUi.text(this, "The admin asked the group to forgive this default. " + pct + "% yes, 51% needed. "
                    + (mineCase ? "You cannot vote on your own case." : eligible ? "Your vote is needed." : "You are not one of the voters."),
                    12.5f, R.color.colorTextSecondary, false));
                if (eligible && !mineCase) {
                    LinearLayout row = new LinearLayout(this);
                    row.addView(voteButton("Vote yes", true, v -> PinPromptHelper.authorize(this, "Vote to forgive this default", () ->
                        NodeGroupClient.voteForgiveness(getApplicationContext(), me, nodeId, addr, true, result("Your yes vote is in")))), buttonCell(true));
                    row.addView(voteButton("Vote no", false, v -> PinPromptHelper.authorize(this, "Vote against forgiving this default", () ->
                        NodeGroupClient.voteForgiveness(getApplicationContext(), me, nodeId, addr, false, result("Your no vote is in")))), buttonCell(false));
                    c.addView(row);
                }
            }
            content.addView(c);
        }
    }

    private void renderCure(LinearLayout card, Map<String, Object> offer) {
        if (offer == null) return;
        boolean eligible = Boolean.TRUE.equals(offer.get("eligible"));
        final long amount = num(offer, "amountRaw");
        if (!eligible) {
            String why = str(offer, "reason");
            String text = "SHORTFALL_NOT_COVERED_BY_BOND".equals(why) ? "Your bond did not cover everything you missed, so only the group can forgive this default."
                : "CURE_WINDOW_CLOSED".equals(why) ? "The 14 days to put this right have passed. The group can still vote to forgive it."
                : "CURE_LIMIT_REACHED".equals(why) ? "You have already put " + num(offer, "curesAllowed") + " defaults right. The group can still vote to forgive this one."
                : "";
            if (!text.isEmpty()) card.addView(CircleUi.text(this, text, 12.5f, R.color.colorTextSecondary, false));
            return;
        }
        long deadline = num(offer, "deadlineTimestamp");
        card.addView(CircleUi.text(this, "You can put this right: pay back " + CircleUi.harv(amount) + " of the bond the default used"
            + (deadline > 0 ? ", before " + new SimpleDateFormat("d MMM", Locale.US).format(new Date(deadline)) : "")
            + ". The penalty is not refunded. It goes back into your bond and the default stops counting against you.", 12.5f, R.color.colorTextSecondary, false));
        card.addView(button("Put it right  ·  " + CircleUi.harv(amount), true, v -> pay("Put a default right", CircleUi.harv(amount),
            "Goes back into your own bond in this circle",
            () -> NodeGroupClient.cureDefault(getApplicationContext(), me, nodeId, result("Sent. The default is cleared once the network confirms it.")))));
    }

    // ── actions ────────────────────────────────────────────────────────────

    private View actionCard(final GroupAction a) {
        LinearLayout c = CircleUi.card(this);
        c.addView(CircleUi.text(this, actionTitle(a), 15, R.color.colorTextPrimary, true));
        TextView d = CircleUi.text(this, actionExplain(a), 12.5f, R.color.colorTextSecondary, false);
        d.setPadding(0, CircleUi.dp(this, 4), 0, 0);
        c.addView(d);
        c.addView(button(actionButtonLabel(a), a.primary, v -> NodePayActions.run(ctx((Button) v), a)));
        return c;
    }

    private String actionTitle(GroupAction a) {
        switch (a.id) {
            case GroupAction.FIXED_REQUEST_DISBURSEMENT: return "Ask for a payout";
            case GroupAction.FIXED_REQUEST_MILESTONE: return "Ask for a milestone release";
            case GroupAction.FIXED_REQUEST_GRANT: return "Ask for an emergency grant";
            case GroupAction.ACCUM_PACK: return "Request a withdrawal";
            case GroupAction.ACCUM_CLAIM: return "Claim your savings";
            case GroupAction.WITHDRAW_BOND: return "Withdraw your bond";
            default: return a.label;
        }
    }

    private String actionExplain(GroupAction a) {
        switch (a.id) {
            case GroupAction.FIXED_REQUEST_DISBURSEMENT: return "Asks the group to release the money held for this goal. Members vote before anyone is paid.";
            case GroupAction.FIXED_REQUEST_MILESTONE: return "Asks for part of the money for a step you have reached. Say what it is for. Members vote.";
            case GroupAction.FIXED_REQUEST_GRANT: return "Ask the group for help from the money held here. Be clear and honest: members vote and the reason stays on record.";
            case GroupAction.ACCUM_PACK: return "Asks the admin to approve taking your savings out. Nothing moves until they approve.";
            case GroupAction.ACCUM_CLAIM: return "Pays your savings line out to your wallet.";
            case GroupAction.WITHDRAW_BOND: return "Returns the bond you locked, when the group allows it.";
            default: return "";
        }
    }

    private String actionButtonLabel(GroupAction a) {
        switch (a.id) {
            case GroupAction.FIXED_REQUEST_DISBURSEMENT: return "Request payout";
            case GroupAction.FIXED_REQUEST_MILESTONE: return "Request milestone";
            case GroupAction.FIXED_REQUEST_GRANT: return "Ask for a grant";
            case GroupAction.ACCUM_PACK: return "Request withdrawal";
            case GroupAction.ACCUM_CLAIM: return "Claim savings";
            case GroupAction.WITHDRAW_BOND: return "Withdraw bond";
            default: return a.label;
        }
    }

    private NodePayActions.Ctx ctx(Button pressed) {
        NodePayActions.Ctx c = new NodePayActions.Ctx();
        c.activity = this;
        c.nodeId = nodeId;
        c.fsGroupId = fsGroupId;
        c.me = me == null ? "" : me;
        c.group = group;
        c.member = member;
        c.line = line;
        c.groupName = groupName;
        c.reload = this::load;
        c.pressed = pressed;
        return c;
    }

    // ── start a new goal (goal groups that have reached their goal) ────────

    private View newGoalCard() {
        LinearLayout c = CircleUi.card(this);
        c.addView(CircleUi.text(this, "Start a new goal", 15, R.color.colorTextPrimary, true));
        TextView d = CircleUi.text(this, "Creates a new goal group with the same payout rule and beneficiary, a name and target you choose, and the same profile. "
            + "The network charges its usual creation fee.", 12.5f, R.color.colorTextSecondary, false);
        d.setPadding(0, CircleUi.dp(this, 4), 0, 0);
        c.addView(d);
        c.addView(button("Start a new goal", false, v -> startNewGoal()));
        return c;
    }

    private void startNewGoal() {
        if (group == null) return;
        final String rule = str(group, "disbursementRule");
        final String who = str(group, "beneficiaryAddress");
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = CircleUi.dp(this, 20);
        box.setPadding(pad, CircleUi.dp(this, 8), pad, 0);
        final EditText name = new com.google.android.material.textfield.TextInputEditText(this);
        name.setHint("Name of the new goal");
        name.setText(groupName.isEmpty() ? "" : groupName + " 2");
        final EditText target = new com.google.android.material.textfield.TextInputEditText(this);
        target.setHint("Target amount");
        target.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        long old = num(group, "targetGoalAmountRaw");
        if (old > 0) target.setText(String.valueOf(old / 100_000_000.0).replaceAll("\\.0$", ""));
        box.addView(name);
        box.addView(target);
        new com.harvana.app.ui.SheetDialog.Builder(this).setTitle("Start a new goal").setView(box).setNegativeButton("Cancel", null)
            .setPositiveButton("Continue", (d, w) -> {
                final String n = name.getText().toString().trim();
                final long raw = CircleUi.toRaw(target.getText().toString());
                if (n.isEmpty() || raw <= 0) { toast("Enter a name and a target."); return; }
                pay("Start a new goal", CircleUi.harv(100_000_000L),
                    "Creation fee. Target " + CircleUi.harv(raw) + " for “" + n + "”. Nothing is locked.",
                    () -> createGoal(n, raw, rule, who));
            }).show();
    }

    private void createGoal(final String goalName, final long targetRaw, String rule, String beneficiary) {
        final String desc = fsData != null && fsData.get("groupDescription") != null ? String.valueOf(fsData.get("groupDescription")) : "";
        NodeGroupClient.createFixedGroup(getApplicationContext(), me, targetRaw, rule, beneficiary, 0L, goalName, desc, new NodeGroupClient.Callback() {
            @Override public void onSuccess(JSONObject json) {
                final String id = json.optString("groupId", json.optString("circleId", ""));
                if (id.isEmpty()) { toast("The network accepted it but returned no id."); return; }
                NodeGroupClient.getFixedGroup(id, new NodeGroupClient.Callback() {
                    @Override public void onSuccess(JSONObject g) { saveGoal(id, goalName, targetRaw, g.optString("escrowAddress", "")); }
                    @Override public void onError(String e) { saveGoal(id, goalName, targetRaw, ""); }
                });
            }
            @Override public void onError(String error) { toast(CircleUi.friendly(error)); runOnUiThread(GroupActionsActivity.this::load); }
        });
    }

    private void saveGoal(String nodeGroupId, String goalName, long targetRaw, String escrow) {
        com.google.firebase.auth.FirebaseUser u = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (u == null) { toast("Your goal exists on the network. Group ID: " + nodeGroupId); return; }
        final String docId = java.util.UUID.randomUUID().toString();
        java.util.Map<String, Object> g = fsData == null ? new java.util.HashMap<String, Object>() : new java.util.HashMap<>(fsData);
        for (String k : new String[]{"groupId", "firestoreDocId", "circleId", "chatLinks", "inviteCode", "members", "memberUids"}) g.remove(k);
        g.put("groupId", docId);
        g.put("firestoreDocId", docId);
        g.put("timestamp", String.valueOf(System.currentTimeMillis()));
        g.put("groupName", goalName);
        g.put("creatorId", u.getUid());
        g.put("status", "Active");
        g.put("membersCount", 1);
        g.put("totalCollected", 0.0);
        g.put("targetGoalAmount", targetRaw / 100_000_000.0);
        g.put("groupWalletAddress", escrow);
        g.put("nodeGroupId", nodeGroupId);
        g.put("family", GroupKinds.familyOf(nodeGroupId));
        g.put("onChain", true);
        final Context ctx = getApplicationContext();
        GroupSaver.queue(ctx, u.getUid(), docId, g);
        GroupSaver.save(ctx, u.getUid(), docId, g, error -> runOnUiThread(() -> {
            if (error != null) { toast("Your goal exists on the network, but saving its details failed. Group ID: " + nodeGroupId); return; }
            GroupSaver.unqueue(ctx, docId);
            toast("New goal created.");
            startActivity(new android.content.Intent(ctx, com.harvana.app.GroupDetailsActivity.class).putExtra("groupId", docId)
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK));
        }));
    }

    // ── plumbing ───────────────────────────────────────────────────────────

    private NodeGroupClient.Callback result(final String ok) {
        return new NodeGroupClient.Callback() {
            @Override public void onSuccess(JSONObject json) {
                com.harvana.app.trust.AppActivity.note(GroupActionsActivity.this, "GROUP", ok);
                toast(ok); runOnUiThread(GroupActionsActivity.this::load);
            }
            @Override public void onError(String error) { toast(CircleUi.friendly(error)); runOnUiThread(GroupActionsActivity.this::load); }
        };
    }

    /** Summary of what is about to be signed, then PIN. */
    private void pay(String title, String amount, String detail, Runnable work) {
        java.util.ArrayList<String[]> rows = new java.util.ArrayList<>();
        if (!groupName.isEmpty()) rows.add(new String[]{"Group", groupName});
        if (detail != null && !detail.isEmpty()) rows.add(new String[]{"For", detail});
        rows.add(new String[]{"From", "Your wallet"});
        rows.add(new String[]{"Network fee", "Taken from your wallet when this is sent"});
        PaymentConfirmSheet.reviewThenAuthorize(this, title, amount, rows, work);
    }

    private Button voteButton(String label, boolean yes, View.OnClickListener l) {
        Button b = com.harvana.app.ui.Btn.of(this, label, yes, l);
        b.setMinHeight(0);
        return b;
    }

    private LinearLayout.LayoutParams buttonCell(boolean first) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, CircleUi.dp(this, 50), 1f);
        lp.topMargin = CircleUi.dp(this, 12);
        if (first) lp.setMarginEnd(CircleUi.dp(this, 6)); else lp.setMarginStart(CircleUi.dp(this, 6));
        return lp;
    }

    private Button button(String label, boolean primary, View.OnClickListener l) {
        Button bt = com.harvana.app.ui.Btn.of(this, label, primary, l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, CircleUi.dp(this, 50));
        lp.topMargin = CircleUi.dp(this, 12);
        bt.setLayoutParams(lp);
        return bt;
    }

    private void message(String m) {
        content.removeAllViews();
        content.addView(emptyCard("Actions & votes", m));
    }

    private void toast(final String m) { runOnUiThread(() -> Toast.makeText(this, m, Toast.LENGTH_LONG).show()); }

    private void copy(String text) {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("Group", text));
        toast("Copied");
    }

    private static String date(long ms) { return new SimpleDateFormat("d MMM yyyy, h:mm a", Locale.US).format(new Date(ms)); }
    private static String titleFor(String f) {
        return "CIRCLE".equals(f) ? "Rotating group" : "ACCUM".equals(f) ? "Savings group" : "FIXED".equals(f) ? "Goal group" : "Group";
    }
    private static String shortAddr(String a) { return a.length() > 14 ? a.substring(0, 8) + "…" + a.substring(a.length() - 4) : a; }
    private static String orEmpty(String s) { return s == null ? "" : s; }
    private static String str(Map<String, Object> m, String k) { Object o = m == null ? null : m.get(k); return o == null ? "" : String.valueOf(o); }
    private static long num(Map<String, Object> m, String k) { Object o = m == null ? null : m.get(k); return o instanceof Number ? ((Number) o).longValue() : 0L; }
}
