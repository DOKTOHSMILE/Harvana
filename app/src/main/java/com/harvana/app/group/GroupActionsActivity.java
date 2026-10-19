package com.harvana.app.group;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

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

    /** An amount of this group, in its coin (HARV for a group made before coins). */
    private String gm(long raw) { return com.harvana.app.engine.Amounts.money(raw, com.harvana.app.engine.Amounts.assetOf(group)); }
    private long typed(String text) {
        return com.harvana.app.engine.Amounts.assetOf(group).isEmpty() ? CircleUi.toRaw(text) : NodePayActions.coinRaw(text);
    }
    private final List<Object[]> proposals = new ArrayList<>();      // {kind, id, Map record}
    private final List<Object[]> packRequests = new ArrayList<>();   // {lineId, Map line}
    private final List<Map<String, Object>> defaulted = new ArrayList<>();
    private final List<Map<String, Object>> joinRequests = new ArrayList<>();
    /** Savings wallet recoveries waiting for the admin (owner, 19 Oct 2026). */
    private final List<Map<String, Object>> recoveries = new ArrayList<>();
    private boolean loadedOnce;
    private boolean notOnNetwork;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        nodeId = orEmpty(getIntent().getStringExtra("nodeGroupId"));
        fsGroupId = orEmpty(getIntent().getStringExtra("firestoreGroupId"));
        family = NodeGroupClient.familyOf(nodeId);
        me = WalletManager.getActiveWalletAddress(this);

        com.harvana.app.utils.AppBar.setContent(this, R.layout.activity_group_actions, "Actions & votes", "");
        refresh = findViewById(R.id.group_actions_refresh);
        content = findViewById(R.id.group_actions_content);
        refresh.setOnRefreshListener(this::load);
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
                message(e != null && com.harvana.app.utils.ErrorText.is(e, "404") ? "This group doesn't exist on the network."
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
        proposals.clear(); packRequests.clear(); defaulted.clear(); joinRequests.clear(); recoveries.clear();
        final AtomicInteger left = new AtomicInteger("ACCUM".equals(family) ? 3 : 2);
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
        NodeGroupClient.listAccumRecoveries(nodeId, new NodeGroupClient.Callback() {
            @Override public void onSuccess(JSONObject j) {
                JSONArray arr = j == null ? null : j.optJSONArray("recoveries");
                for (int i = 0; arr != null && i < arr.length(); i++) {
                    JSONObject r = arr.optJSONObject(i);
                    if (r != null && "ADMIN".equals(r.optString("waitingFor"))) recoveries.add(JsonMaps.toMap(r));
                }
                done.run();
            }
            @Override public void onError(String e) { done.run(); }                 // an older node: no list
        });
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
            content.addView(new ActionCard(this, content).title("Not on the network").chip("OFF NETWORK", R.color.colorWarning).view);
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
        List<Map<String, Object>> toMove = iAmAdmin() ? recoveries : new ArrayList<>();
        int attention = mineToVote + joinCount + approvals.size() + defaulted.size() + toMove.size();

        content.addView(summaryCard(s, attention, openVotes.size(), joinCount, approvals.size() + toMove.size(), defaulted.size()));

        // 1. Needs your attention
        boolean any = !openVotes.isEmpty() || joinCount > 0 || !approvals.isEmpty() || !defaulted.isEmpty() || !toMove.isEmpty();
        content.addView(sectionHeader("Needs your attention", attention == 0 ? "" : String.valueOf(attention)));
        if (!any) {
            content.addView(emptyCard("Nothing is waiting on you", ""));
        }
        for (Object[] p : openVotes) content.addView(voteCard((String) p[0], (String) p[1], castMap(p[2]), true));
        if (joinCount > 0) for (Map<String, Object> r : joinRequests) content.addView(joinRequestCard(r));
        for (Object[] p : approvals) content.addView(withdrawalCard((String) p[0], castMap(p[1])));
        for (Map<String, Object> r : toMove) content.addView(recoveryCard(r));
        renderDefaults();

        // 2. What you can do
        List<GroupAction> doable = new ArrayList<>();
        for (GroupAction a : s.actions) if (a.enabled && isRequestAction(a.id)) doable.add(a);
        String rule0 = str(group, "disbursementRule");
        boolean paysOutInOne = "ALL_AT_ONCE_AT_GOAL".equals(rule0) || "ADMIN_TRIGGERED".equals(rule0);
        long goalRaised = Math.max(0L, num(group, "raisedAmountRaw") - num(group, "goalBaseRaisedRaw"));
        boolean reached = num(group, "targetGoalAmountRaw") > 0 && goalRaised >= num(group, "targetGoalAmountRaw");
        boolean newGoal = "FIXED".equals(family) && me != null && !me.isEmpty() && me.equalsIgnoreCase(str(group, "creatorAddress"))
            && (paysOutInOne ? "COMPLETED".equals(str(group, "state")) : reached);
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

        // Goal groups show their past goals on the Payments tab.

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

    /** res/layout/item_action_summary.xml: the group, my role, its state and the three counts. */
    private View summaryCard(GroupSnapshot s, int attention, int openVotes, int joinCount, int approvals, int defaults) {
        View card = getLayoutInflater().inflate(R.layout.item_action_summary, content, false);
        ((TextView) card.findViewById(R.id.tv_actions_group_name)).setText(groupName.isEmpty() ? titleFor(family) : groupName);
        ((TextView) card.findViewById(R.id.tv_actions_group_role)).setText(titleFor(family) + " · " + role());
        TextView state = card.findViewById(R.id.tv_actions_group_state);
        state.setText(s.state.isEmpty() ? "ACTIVE" : s.state);
        state.setTextColor(CircleUi.color(this, CircleUi.stateColor(s.state)));
        ((TextView) card.findViewById(R.id.tv_actions_open_votes)).setText(String.valueOf(openVotes));
        ((TextView) card.findViewById(R.id.tv_actions_join_requests)).setText(String.valueOf(joinCount));
        ((TextView) card.findViewById(R.id.tv_actions_to_approve)).setText(String.valueOf(approvals + defaults));
        TextView h = card.findViewById(R.id.tv_actions_attention);
        h.setText(attention == 0 ? "Up to date" : attention == 1 ? "1 thing needs you" : attention + " things need you");
        h.setTextColor(CircleUi.color(this, attention == 0 ? R.color.colorSuccess : R.color.colorWarning));
        return card;
    }

    private String role() {
        if (iAmAdmin() || iAmFirestoreAdmin()) return "You are the admin";
        if (member != null && !member.isEmpty()) return "You are a member";
        return "You are not a member";
    }

    /** res/layout/item_action_section.xml. */
    private View sectionHeader(String title, String count) {
        View h = getLayoutInflater().inflate(R.layout.item_action_section, content, false);
        ((TextView) h.findViewById(R.id.tv_action_section_title)).setText(title);
        TextView c = h.findViewById(R.id.tv_action_section_count);
        c.setText(count);
        c.setVisibility(count.isEmpty() ? View.GONE : View.VISIBLE);
        return h;
    }

    private View emptyCard(String title, String body) {
        return new ActionCard(this, content).title(title).note(body).view;
    }

    // ── votes ──────────────────────────────────────────────────────────────

    private View voteCard(final String kind, final String id, Map<String, Object> rec, boolean openList) {
        String label = GroupProposalIndex.DISBURSEMENT.equals(kind) ? "Payout request"
            : GroupProposalIndex.MILESTONE.equals(kind) ? "Milestone release" : "Emergency grant";
        VoteView v = VoteView.of(label, rec, me == null ? "" : me);
        ActionCard c = new ActionCard(this, content).title(label).chip(statusLabel(v), statusColor(v));
        long amt = num(rec, "amountRaw");
        if (amt > 0) c.amount(gm(amt));
        c.quote(!str(rec, "description").isEmpty() ? str(rec, "description") : str(rec, "reasonDescription"));
        String by = !str(rec, "requestedBy").isEmpty() ? str(rec, "requestedBy") : str(rec, "requesterAddress");
        long when = num(rec, "requestedTimestamp");
        if (!by.isEmpty() || when > 0) {
            String who = by.isEmpty() ? "" : (by.equalsIgnoreCase(me) ? "You" : shortAddr(by));
            c.meta((who.isEmpty() ? "" : "Asked by " + who) + (who.isEmpty() || when <= 0 ? "" : " · ") + (when > 0 ? date(when) : ""));
        }
        if (v.votersTotal > 0) {
            c.bar(v.yesPercent, v.open, v.yesPercent + "% yes · needs " + v.thresholdPercent + "% · " + v.votersYes + " of " + v.votersTotal + " voters");
        }
        if (v.open) {
            if (!v.eligible) {
                c.note("Not a voter on this", R.color.colorTextTertiary, false);
            } else {
                c.note(v.alreadyVoted ? "You voted " + (v.myVoteYes ? "yes" : "no") : "Your vote is needed",
                        v.alreadyVoted ? R.color.colorTextSecondary : R.color.colorWarning, true);
                c.twoButtons("Vote yes", v2 -> vote(kind, id, true, label), "Vote no", v2 -> vote(kind, id, false, label));
            }
        }
        return c.view;
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
        rows.add(new String[]{"tFare", "Taken from your wallet when this is sent"});
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

        ActionCard c = new ActionCard(this, content).subtitle("Wants to join " + (groupName.isEmpty() ? "this group" : groupName))
                .chip(vote ? "MEMBERS VOTE" : "ADMIN DECIDES", R.color.colorWarning);
        final TextView badge = c.avatar();
        MemberPhoto.apply(badge, reqUid);
        final TextView name = c.titleView();
        name.setText("Someone");
        MemberNameResolver.resolve(this, reqUid, "Someone", full -> {
            name.setText(full);
            if (!full.isEmpty()) badge.setText(full.substring(0, 1).toUpperCase(Locale.US));
        });
        c.quote(str(req, "message"));
        long when = num(req, "createdAt");
        if (when > 0) c.meta("Asked " + date(when));

        if (vote) {
            Object votes = req.get("votes");
            boolean voted = votes instanceof Map && ((Map<?, ?>) votes).containsKey(myUid);
            if (reqUid.equals(myUid)) {
                c.note("Your own request", R.color.colorTextTertiary, false);
            } else if (voted) {
                c.note("You have voted", R.color.colorTextSecondary, true);
            } else {
                int total = (int) num(fsData, "membersCount");
                c.note("Passes at 60% yes of " + Math.max(1, total) + (total == 1 ? " member" : " members"));
                c.twoButtons("Vote yes", v -> castJoinVote(reqUid, myUid, true), "Vote no", v -> castJoinVote(reqUid, myUid, false));
            }
        } else {
            c.twoButtons("Accept", v -> acceptJoin(docId, reqUid), "Reject", v -> rejectJoin(docId));
        }
        return c.view;
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
        String who = str(ln, "memberAddress");
        ActionCard c = new ActionCard(this, content).title("Withdrawal request").chip("NEEDS APPROVAL", R.color.colorWarning)
                .amount(gm(num(ln, "amountPaidRaw"))).meta(who.isEmpty() ? "A member" : shortAddr(who));
        GroupAction approve = new GroupAction(GroupAction.ACCUM_APPROVE_PACK, gm(num(ln, "amountPaidRaw")), true, lineId);
        c.button("Approve withdrawal", true, v -> NodePayActions.run(ctx((Button) v), approve));
        return c.view;
    }

    /** A member asks to move their savings to another wallet: old wallet, new wallet, when; the admin approves. */
    private View recoveryCard(final Map<String, Object> r) {
        final String from = str(r, "oldMemberAddress"), to = str(r, "newMemberAddress");
        ActionCard c = new ActionCard(this, content).title("Wallet recovery").chip("NEEDS APPROVAL", R.color.colorWarning)
                .meta(shortAddr(from) + " → " + shortAddr(to) + (num(r, "createdTimestamp") > 0 ? " · " + date(num(r, "createdTimestamp")) : ""));
        c.button("Approve move", true, v -> {
            java.util.ArrayList<String[]> rows = new java.util.ArrayList<>();
            if (!groupName.isEmpty()) rows.add(new String[]{"Group", groupName});
            rows.add(new String[]{"From wallet", from});
            rows.add(new String[]{"To wallet", to});
            PaymentConfirmSheet.reviewThenAuthorize(this, "Approve wallet recovery", "", rows, () ->
                    NodeGroupClient.accumWalletRecovery(getApplicationContext(), me, nodeId, from, to, result("Savings moved to the new wallet")));
        });
        return c.view;
    }

    @SuppressWarnings("unchecked")
    private void renderDefaults() {
        if (!"CIRCLE".equals(family) || defaulted.isEmpty()) return;
        boolean admin = iAmAdmin();
        for (final Map<String, Object> m : defaulted) {
            final String addr = String.valueOf(m.get("address"));
            boolean mineCase = addr.equalsIgnoreCase(me == null ? "" : me);
            ActionCard c = new ActionCard(this, content).title((mineCase ? "You" : shortAddr(addr)) + " defaulted").chip("DEFAULT", R.color.colorError);
            if (mineCase) renderCure(c, (Map<String, Object>) m.get("cureOffer"));
            Map<String, Object> proposal = (Map<String, Object>) m.get("proposal");
            boolean open = proposal != null && "PENDING".equals(str(proposal, "status"));
            if (!open) {
                if (admin && !mineCase) {
                    c.button("Ask the group to forgive", false, v -> PinPromptHelper.authorize(this, "Ask the group to forgive this default", () ->
                        NodeGroupClient.proposeForgiveness(getApplicationContext(), me, nodeId, addr, result("Asked. The voters decide."))));
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
                c.bar(pct, true, "Forgive: " + pct + "% yes · needs 51%");
                c.note(mineCase ? "Your own case" : eligible ? "Your vote is needed" : "Not a voter on this",
                        eligible && !mineCase ? R.color.colorWarning : R.color.colorTextTertiary, eligible && !mineCase);
                if (eligible && !mineCase) {
                    c.twoButtons("Vote yes", v -> PinPromptHelper.authorize(this, "Vote to forgive this default", () ->
                            NodeGroupClient.voteForgiveness(getApplicationContext(), me, nodeId, addr, true, result("Your yes vote is in"))),
                        "Vote no", v -> PinPromptHelper.authorize(this, "Vote against forgiving this default", () ->
                            NodeGroupClient.voteForgiveness(getApplicationContext(), me, nodeId, addr, false, result("Your no vote is in"))));
                }
            }
            content.addView(c.view);
        }
    }

    private void renderCure(ActionCard card, Map<String, Object> offer) {
        if (offer == null) return;
        boolean eligible = Boolean.TRUE.equals(offer.get("eligible"));
        final long amount = num(offer, "amountRaw");
        if (!eligible) {
            String why = str(offer, "reason");
            String text = "SHORTFALL_NOT_COVERED_BY_BOND".equals(why) ? "Bond did not cover the shortfall"
                : "CURE_WINDOW_CLOSED".equals(why) ? "14-day window to put it right: closed"
                : "CURE_LIMIT_REACHED".equals(why) ? "Defaults already put right: " + num(offer, "curesAllowed")
                : "";
            card.note(text);
            return;
        }
        long deadline = num(offer, "deadlineTimestamp");
        card.note("Put it right: " + gm(amount) + (deadline > 0 ? " · before " + new SimpleDateFormat("d MMM", Locale.US).format(new Date(deadline)) : ""));
        card.button("Put it right  ·  " + gm(amount), true, v -> pay("Put a default right", gm(amount),
            "Goes back into your own bond in this circle",
            () -> NodeGroupClient.cureDefault(getApplicationContext(), me, nodeId, result("Sent. The default is cleared once the network confirms it."))));
    }

    // ── actions ────────────────────────────────────────────────────────────

    private View actionCard(final GroupAction a) {
        ActionCard c = new ActionCard(this, content).title(actionTitle(a));
        c.button(actionButtonLabel(a), a.primary, v -> NodePayActions.run(ctx((Button) v), a));
        return c.view;
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
        ActionCard c = new ActionCard(this, content).title("Start a new goal");
        c.button("Start a new goal", false, v -> startNewGoal());
        return c.view;
    }

    private void startNewGoal() {
        if (group == null) return;
        View box = getLayoutInflater().inflate(R.layout.dialog_new_goal, null, false);
        final EditText name = box.findViewById(R.id.et_new_goal_name);
        name.setText("Goal " + (num(group, "goalNumber") > 0 ? num(group, "goalNumber") + 1 : 2));
        final EditText target = box.findViewById(R.id.et_new_goal_target);
        long old = num(group, "targetGoalAmountRaw");
        if (old > 0) target.setText(String.valueOf(old / 100_000_000.0).replaceAll("\\.0$", ""));
        new com.harvana.app.ui.SheetDialog.Builder(this).setTitle("Start a new goal").setView(box).setNegativeButton("Cancel", null)
            .setPositiveButton("Continue", (d, w) -> {
                final String n = name.getText().toString().trim();
                final long raw = typed(target.getText().toString());
                if (n.isEmpty() || raw <= 0) { toast("Enter a name and a target."); return; }
                pay("Start a new goal", gm(raw),
                    "New goal “" + n + "” in this group. Target shown. Nothing is locked.",
                    () -> NodeGroupClient.startNextFixedGoal(getApplicationContext(), me, nodeId, raw, n, result("New goal started.")));
            }).show();
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
        rows.add(new String[]{"tFare", "Taken from your wallet when this is sent"});
        PaymentConfirmSheet.reviewThenAuthorize(this, title, amount, rows, work);
    }




    private void message(String m) {
        content.removeAllViews();
        content.addView(emptyCard("Actions & votes", m));
    }

    private void toast(final String m) { runOnUiThread(() -> Toast.makeText(this, m, Toast.LENGTH_LONG).show()); }


    private static String date(long ms) { return new SimpleDateFormat("d MMM yyyy, h:mm a", Locale.US).format(new Date(ms)); }
    private static String titleFor(String f) {
        return "CIRCLE".equals(f) ? "Rotating group" : "ACCUM".equals(f) ? "Savings group" : "FIXED".equals(f) ? "Goal group" : "Group";
    }
    private static String shortAddr(String a) { return a.length() > 14 ? a.substring(0, 8) + "…" + a.substring(a.length() - 4) : a; }
    private static String orEmpty(String s) { return s == null ? "" : s; }
    private static String str(Map<String, Object> m, String k) { Object o = m == null ? null : m.get(k); return o == null ? "" : String.valueOf(o); }
    private static long num(Map<String, Object> m, String k) { Object o = m == null ? null : m.get(k); return o instanceof Number ? ((Number) o).longValue() : 0L; }
}
