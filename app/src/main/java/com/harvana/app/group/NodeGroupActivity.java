package com.harvana.app.group;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
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
import com.harvana.app.sync.HarvanaNodeClient;
import com.harvana.app.wallet.PinPromptHelper;
import com.harvana.app.sync.NodeGroupClient;
import com.harvana.app.utils.JsonMaps;
import com.harvana.app.wallet.WalletManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * One screen for a group that lives on the node (Circle, Accumulative or Fixed). Everything shown comes from
 * the node; the engine classes decide what the member may do; every action is a signed request.
 */
public class NodeGroupActivity extends AppCompatActivity {
    private String nodeId = "", fsGroupId = "", me = "", family = "";
    private SwipeRefreshLayout refresh;
    private LinearLayout content;

    private Map<String, Object> group, member, line;
    private final List<Object[]> proposals = new ArrayList<>();   // {kind, id, Map record}
    private final List<Object[]> packRequests = new ArrayList<>(); // {lineId, Map line}
    private Map<String, Object> recovery;
    private String trustTier = "";
    private String groupName = "";
    private List<String> myLabels = new ArrayList<>();
    private Button pressed;
    private java.util.Map<String, Object> fsData;      // this group's Firestore profile, copied into a new goal

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        nodeId = orEmpty(getIntent().getStringExtra("nodeGroupId"));
        if (nodeId.isEmpty()) nodeId = orEmpty(getIntent().getStringExtra("circleId"));
        fsGroupId = orEmpty(getIntent().getStringExtra("firestoreGroupId"));
        family = NodeGroupClient.familyOf(nodeId);
        me = WalletManager.getActiveWalletAddress(this);

        refresh = new SwipeRefreshLayout(this);
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(CircleUi.color(this, R.color.colorBackground));
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int pad = CircleUi.dp(this, 18);
        content.setPadding(pad, pad, pad, pad);
        scroll.addView(content, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        refresh.addView(scroll);
        refresh.setOnRefreshListener(this::load);
        com.harvana.app.utils.AppBar.setContent(this, refresh, titleFor(family), nodeId);
        if (!fsGroupId.isEmpty()) {
            com.harvana.app.firestore.GroupFirestoreManager.getInstance(this).getGroup(fsGroupId,
                new com.harvana.app.firestore.GroupFirestoreManager.DocCallback() {
                    @Override public void onSuccess(java.util.Map<String, Object> data) {
                        fsData = new java.util.HashMap<>(data);
                        Object n = data.get("groupName");
                        if (n != null && !String.valueOf(n).isEmpty()) {
                            groupName = String.valueOf(n);
                            runOnUiThread(() -> com.harvana.app.utils.AppBar.setSubtitle(NodeGroupActivity.this, groupName));
                        }
                    }
                    @Override public void onError(String e) { }
                });
        }
        
        load();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (group != null) load();
        if (me != null && !me.isEmpty()) {
            com.harvana.app.sync.LockSigner.checkPending(me, err -> runOnUiThread(() -> { if (err == null && group != null) load(); }));
        }
    }

    // ── loading ────────────────────────────────────────────────────────────

    private void load() {
        boolean first = group == null;
        refresh.setRefreshing(true);
        if (first) {
            content.removeAllViews();
            content.addView(com.harvana.app.utils.Shimmer.cards(this, 3));
        }
        NodeGroupClient.Callback onGroup = new NodeGroupClient.Callback() {
            @Override public void onSuccess(JSONObject json) {
                group = JsonMaps.toMap(json);
                // The group page can come from a saved copy in Firestore. Ask the node itself whether it still has the group.
                NodeGroupClient.existsOnNode(family, nodeId, exists -> {
                    boolean gone = Boolean.FALSE.equals(exists);
                    if (gone != notOnNetwork) { notOnNetwork = gone; runOnUiThread(() -> { if (group != null) render(); }); }
                });
                loadMember();
            }
            @Override public void onError(String e) {
                refresh.setRefreshing(false);
                message(e != null && e.contains("404") ? "This group doesn't exist on the network." : "Couldn't load the group: " + e);
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
            @Override public void onError(String e) { member = null; line = null; loadExtras(); } // 404 = not a member
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

    /** Group trust tier, my badges, and open proposals / pack requests. Each part is best-effort. */
    private void loadExtras() {
        proposals.clear(); packRequests.clear(); recovery = null; trustTier = ""; myLabels = new ArrayList<>();
        final AtomicInteger left = new AtomicInteger(3);
        final Runnable done = () -> { if (left.decrementAndGet() == 0) runOnUiThread(this::render); };

        HarvanaNodeClient.nodeGet("/api/v1/wallet/group-trust/" + nodeId, new HarvanaNodeClient.JsonCallback() {
            @Override public void onSuccess(JSONObject j) { trustTier = j.optString("tier", j.optString("trustTier", "")); done.run(); }
            @Override public void onError(String e) { done.run(); }
        });
        if (me == null || me.isEmpty()) { done.run(); } else {
            HarvanaNodeClient.nodeGet("/api/v1/wallet/labels/" + me, new HarvanaNodeClient.JsonCallback() {
                @Override public void onSuccess(JSONObject j) {
                    JSONArray a = j.optJSONArray("labels");
                    if (a != null) for (int i = 0; i < a.length(); i++) {
                        Object o = a.opt(i);
                        myLabels.add(o instanceof JSONObject ? ((JSONObject) o).optString("label", ((JSONObject) o).optString("name", "")) : String.valueOf(o));
                    }
                    done.run();
                }
                @Override public void onError(String e) { done.run(); }
            });
        }
        if ("CIRCLE".equals(family)) { loadRecovery(() -> loadDefaults(done)); return; }
        if ("FIXED".equals(family)) {
            NodeGroupClient.listFixedProposals(nodeId, new NodeGroupClient.Callback() {
                @Override public void onSuccess(JSONObject j) { fillListedProposals(j); loadRecovery(done); }
                @Override public void onError(String e) { GroupProposalIndex.list(NodeGroupActivity.this, fsGroupId, entries -> loadProposals(entries, () -> loadRecovery(done))); }
            });
            return;
        }
        NodeGroupClient.listAccumPendingPacks(nodeId, new NodeGroupClient.Callback() {
            @Override public void onSuccess(JSONObject j) { fillListedPacks(j); loadRecovery(done); }
            @Override public void onError(String e) { GroupProposalIndex.list(NodeGroupActivity.this, fsGroupId, entries -> loadProposals(entries, () -> loadRecovery(done))); }
        });
    }

    /** Members of this circle who defaulted and are not yet forgiven, each with the open forgiveness vote if there is one. */
    private final List<Map<String, Object>> defaulted = new ArrayList<>();

    private void loadDefaults(Runnable done) {
        defaulted.clear();
        NodeGroupClient.getCircleMembers(nodeId, new NodeGroupClient.Callback() {
            @Override public void onSuccess(JSONObject j) {
                JSONArray rows = j == null ? null : j.optJSONArray("members");
                final List<Map<String, Object>> found = new ArrayList<>();
                if (rows != null) for (int i = 0; i < rows.length(); i++) {
                    JSONObject r = rows.optJSONObject(i);
                    if (r == null) continue;
                    String st = r.optString("state", "");
                    String res = r.optString("defaultResolution", "");
                    if (st.startsWith("DEFAULTED") && !"FORGIVEN".equals(res) && !"CURED".equals(res) && found.size() < 10) found.add(JsonMaps.toMap(r));
                }
                if (found.isEmpty()) { done.run(); return; }
                final AtomicInteger pending = new AtomicInteger(found.size());
                for (Map<String, Object> m : found) {
                    final String addr = String.valueOf(m.get("address"));
                    NodeGroupClient.getForgiveness(nodeId, addr, new NodeGroupClient.Callback() {
                        @Override public void onSuccess(JSONObject p) { m.put("proposal", JsonMaps.toMap(p)); next(); }
                        @Override public void onError(String e) { next(); }
                        /** My own default also needs the cure offer: what it costs and until when. */
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

    private void loadRecovery(Runnable done) {
        NodeGroupClient.getAdminRecovery(nodeId, new NodeGroupClient.Callback() {
            @Override public void onSuccess(JSONObject j) { recovery = JsonMaps.toMap(j); done.run(); }
            @Override public void onError(String e) { done.run(); }
        });
    }

    private void fillListedProposals(JSONObject j) {
        JSONArray arr = j == null ? null : j.optJSONArray("proposals");
        if (arr == null) return;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject row = arr.optJSONObject(i);
            if (row == null) continue;
            proposals.add(new Object[]{row.optString("kind", ""), row.optString("id", ""), JsonMaps.toMap(row)});
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

    private void loadProposals(List<GroupProposalIndex.Entry> entries, Runnable done) {
        if (entries.isEmpty()) { done.run(); return; }
        final AtomicInteger n = new AtomicInteger(entries.size());
        for (GroupProposalIndex.Entry e : entries) {
            NodeGroupClient.Callback cb = new NodeGroupClient.Callback() {
                @Override public void onSuccess(JSONObject j) {
                    synchronized (proposals) {
                        if (GroupProposalIndex.PACK.equals(e.kind)) packRequests.add(new Object[]{e.id, JsonMaps.toMap(j)});
                        else proposals.add(new Object[]{e.kind, e.id, JsonMaps.toMap(j)});
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

    // ── rendering ──────────────────────────────────────────────────────────

    private GroupSnapshot snapshot() {
        if ("CIRCLE".equals(family)) return CircleSnapshot.of(group, member);
        if ("ACCUM".equals(family)) return AccumSnapshot.of(group, member, line, me == null ? "" : me);
        return FixedSnapshot.of(group, member, me == null ? "" : me);
    }

    /** True when the node answered "not found" for this group while the page shows a saved copy of it. */
    private boolean notOnNetwork;

    private void render() {
        boardPaysToday = false;
        refresh.setRefreshing(false);
        content.removeAllViews();
        GroupSnapshot s = snapshot();

        if (notOnNetwork) {
            LinearLayout warn = CircleUi.card(this);
            warn.addView(CircleUi.text(this, "Not on the network", 15, R.color.colorWarning, true));
            warn.addView(CircleUi.text(this, "The network no longer has this group (it was reset, or it never finished being created). "
                    + "The numbers below are an old saved copy. Payments, votes and payout requests for it will be refused. "
                    + "Create the group again to use it.", 12.5f, R.color.colorTextSecondary, false));
            content.addView(warn);
        }

        LinearLayout head = new LinearLayout(this);
        head.setGravity(android.view.Gravity.CENTER_VERTICAL);
        head.addView(CircleUi.text(this, titleFor(family) + " " + shortId(nodeId), 20, R.color.colorTextPrimary, true),
            new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        head.addView(CircleUi.chip(this, s.state, CircleUi.stateColor(s.state)));
        content.addView(head);
        if (!trustTier.isEmpty()) {
            content.addView(CircleUi.text(this, "Group trust: " + trustTier.replace('_', ' ').toLowerCase(java.util.Locale.US), 12.5f, R.color.colorTextSecondary, false));
        }

        LinearLayout prog = CircleUi.card(this);
        prog.addView(CircleUi.text(this, s.headline, 16, R.color.colorTextPrimary, true));
        if (s.progressMax > 0) {
            ProgressBar pb = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            pb.setMax(s.progressMax);
            pb.setProgress(s.progressCurrent);
            pb.setProgressTintList(android.content.res.ColorStateList.valueOf(CircleUi.color(this, R.color.colorAccent)));
            prog.addView(pb, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, CircleUi.dp(this, 10)));
        }
        if (!s.deadlineNote.isEmpty()) prog.addView(CircleUi.text(this, s.deadlineNote, 12, R.color.colorTextTertiary, false));
        content.addView(prog);

        LinearLayout terms = CircleUi.card(this);
        terms.addView(CircleUi.text(this, "How this group works", 15, R.color.colorTextPrimary, true));
        for (GroupSnapshot.Stat t : s.terms) terms.addView(CircleUi.row(this, t.label, t.value));
        if (!s.escrow.isEmpty()) {
            TextView esc = CircleUi.text(this, "Held by the network at " + s.escrow, 11.5f, R.color.colorTextTertiary, false);
            esc.setOnClickListener(v -> copy(s.escrow));
            terms.addView(esc);
        }
        content.addView(terms);

        if (!s.mine.isEmpty() || !s.myState.isEmpty()) {
            LinearLayout mine = CircleUi.card(this);
            LinearLayout mh = new LinearLayout(this);
            mh.setGravity(android.view.Gravity.CENTER_VERTICAL);
            mh.addView(CircleUi.text(this, "Your position", 15, R.color.colorTextPrimary, true), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            if (!s.myState.isEmpty()) mh.addView(CircleUi.chip(this, s.myState, CircleUi.stateColor(s.myState)));
            mine.addView(mh);
            for (GroupSnapshot.Stat t : s.mine) mine.addView(CircleUi.row(this, t.label, t.value));
            if (!myLabels.isEmpty()) mine.addView(CircleUi.text(this, "Your badges: " + join(myLabels), 12, R.color.colorTextSecondary, false));
            content.addView(mine);
        }
        for (String n : s.notes) content.addView(CircleUi.text(this, n, 12.5f, R.color.colorWarning, false));

        GroupTypeBoard.Board board = GroupTypeBoard.fromNode(family, group, member, line, me,
                System.currentTimeMillis(), java.util.TimeZone.getDefault());
        if (board != null && board.kind != null && !board.kind.isEmpty()
                && (board.cells != null && !board.cells.isEmpty() || !board.subtitle.isEmpty())) {
            LinearLayout card = CircleUi.card(this);
            card.addView(CircleUi.text(this, board.title, 15, R.color.colorTextPrimary, true));
            if (!board.subtitle.isEmpty()) {
                card.addView(CircleUi.text(this, board.subtitle, 12.5f, R.color.colorTextSecondary, false));
            }
            LinearLayout host = new LinearLayout(this);
            host.setOrientation(LinearLayout.VERTICAL);
            card.addView(host);
            final GroupTypeBoardView.Selection sel = "ACCUM".equals(family) && line != null ? new GroupTypeBoardView.Selection() : null;
            if (sel != null) {
                double daily = num(line, "dailyAmountRaw") / 100_000_000.0;
                card.addView(BackpayBar.create(this, sel, daily, dates -> paySelection(dates)));
            }
            boardPaysToday = sel != null;
            GroupTypeBoardView.fill(host, board, cell -> {
                if (cell.newGoal) { startNewGoal(); return; }
                if (cell.backpayable || (cell.forwardPayable && !cell.payToday)) {   // one box: pay that day (ahead days are paid in order from today)
                    java.util.List<String> upTo = new java.util.ArrayList<>();
                    for (GroupTypeBoard.Cell c : board.cells) {
                        if (cell.backpayable ? c.isoDate.equals(cell.isoDate) : (c.forwardPayable && c.isoDate.compareTo(cell.isoDate) <= 0)) upTo.add(c.isoDate);
                    }
                    paySelection(upTo);
                    return;
                }
                if (cell.payToday) {
                    for (GroupAction a : s.actions) {
                        if (GroupAction.ACCUM_CONTRIBUTE.equals(a.id) || GroupAction.CONTRIBUTE.equals(a.id)
                                || GroupAction.FIXED_DEPOSIT.equals(a.id)) {
                            onAction(a);
                            return;
                        }
                    }
                }
                GroupTypeBoardView.showDetail(this, cell);
            }, sel);
            if (!board.legend.isEmpty()) {
                card.addView(CircleUi.text(this, board.legend, 11.5f, R.color.colorTextTertiary, false));
            }
            content.addView(card);
        }

        for (GroupAction a : s.actions) {
            if (boardPaysToday && GroupAction.ACCUM_CONTRIBUTE.equals(a.id)) continue;   // the calendar's today box pays it
            content.addView(actionButton(a));
        }

        renderProposals();
        renderPackRequests();
        renderDefaults();
        content.addView(button("Copy group ID", false, v -> copy(nodeId)));
    }

    private void renderProposals() {
        if (proposals.isEmpty()) return;
        content.addView(CircleUi.text(this, "Votes", 16, R.color.colorTextPrimary, true));
        for (Object[] p : proposals) {
            final String kind = (String) p[0], id = (String) p[1];
            @SuppressWarnings("unchecked") Map<String, Object> rec = (Map<String, Object>) p[2];
            String title = kind.equals(GroupProposalIndex.DISBURSEMENT) ? "Payout request"
                : kind.equals(GroupProposalIndex.MILESTONE) ? "Milestone: " + str(rec, "description")
                : "Emergency grant: " + str(rec, "reasonDescription");
            long amt = num(rec, "amountRaw");
            if (amt > 0) title += " (" + CircleUi.harv(amt) + ")";
            VoteView vote = VoteView.of(title, rec, me == null ? "" : me);
            LinearLayout c = CircleUi.card(this);
            c.addView(CircleUi.text(this, vote.title, 14, R.color.colorTextPrimary, true));
            c.addView(CircleUi.text(this, vote.explain(), 12.5f, R.color.colorTextSecondary, false));
            if (vote.open && vote.eligible) {
                LinearLayout row = new LinearLayout(this);
                row.addView(actionButton(new GroupAction(GroupAction.VOTE_YES, "Vote yes", true), v -> vote(kind, id, true)), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                row.addView(actionButton(new GroupAction(GroupAction.VOTE_NO, "Vote no", false), v -> vote(kind, id, false)), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                c.addView(row);
            }
            content.addView(c);
        }
    }

    /** Circles only: defaulted members, and the group's vote on forgiving each one. Forgiving moves no money. */
    private void renderDefaults() {
        if (!"CIRCLE".equals(family) || defaulted.isEmpty()) return;
        boolean admin = group != null && me != null && me.equals(str(group, "creatorAddress"));
        content.addView(CircleUi.text(this, "Defaults", 16, R.color.colorTextPrimary, true));
        for (Map<String, Object> m : defaulted) {
            final String addr = String.valueOf(m.get("address"));
            LinearLayout c = CircleUi.card(this);
            boolean mineCase = addr.equalsIgnoreCase(me == null ? "" : me);
            c.addView(CircleUi.text(this, (mineCase ? "You" : shortAddr(addr)) + " defaulted", 14, R.color.colorTextPrimary, true));
            if (mineCase) renderCure(c, (Map<String, Object>) m.get("cureOffer"));
            @SuppressWarnings("unchecked") Map<String, Object> proposal = (Map<String, Object>) m.get("proposal");
            boolean open = proposal != null && "PENDING".equals(str(proposal, "status"));
            if (!open) {
                c.addView(CircleUi.text(this, "The group can vote to forgive a default. Forgiving it takes it off this member's record. "
                        + "It does not move any money.", 12.5f, R.color.colorTextSecondary, false));
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
                c.addView(CircleUi.text(this, "The admin asked the group to forgive this default. Yes " + (total == 0 ? 0 : yes * 100 / total)
                        + "% of the vote, 51% needed. " + (mineCase ? "You cannot vote on your own case." : eligible ? "You can vote." : "You are not one of the voters."),
                        12.5f, R.color.colorTextSecondary, false));
                if (eligible && !mineCase) {
                    LinearLayout row = new LinearLayout(this);
                    row.addView(button("Vote yes", true, v -> PinPromptHelper.authorize(this, "Vote to forgive this default", () ->
                        NodeGroupClient.voteForgiveness(getApplicationContext(), me, nodeId, addr, true, result("Voted yes")))), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                    row.addView(button("Vote no", false, v -> PinPromptHelper.authorize(this, "Vote against forgiving this default", () ->
                        NodeGroupClient.voteForgiveness(getApplicationContext(), me, nodeId, addr, false, result("Voted no")))), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                    c.addView(row);
                }
            }
            content.addView(c);
        }
    }

    /** My own default: what putting it right costs and by when. The node fixes the amount and decides if it is allowed. */
    private void renderCure(LinearLayout card, Map<String, Object> offer) {
        if (offer == null) return;
        boolean eligible = Boolean.TRUE.equals(offer.get("eligible"));
        long amount = num(offer, "amountRaw");
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
                + (deadline > 0 ? ", before " + new java.text.SimpleDateFormat("d MMM", java.util.Locale.US).format(new java.util.Date(deadline)) : "")
                + ". The penalty is not refunded. It goes back into your bond and the default stops counting against you. You can do this "
                + num(offer, "curesAllowed") + " times in all (" + num(offer, "curesUsed") + " used).", 12.5f, R.color.colorTextSecondary, false));
        card.addView(button("Put it right  \u00b7  " + CircleUi.harv(amount), true, v -> pay("Put a default right", CircleUi.harv(amount),
            moneyRows(escrowOrGroup(), "Goes back into your own bond in this circle"),
            () -> NodeGroupClient.cureDefault(getApplicationContext(), me, nodeId, result("Sent. The default is cleared once the network confirms it.")))));
    }

    private void renderPackRequests() {
        boolean admin = group != null && me != null && (me.equals(str(group, "creatorAddress")) || me.equals(str(group, "treasurerAddress")));
        if (!admin) return;
        for (Object[] p : packRequests) {
            @SuppressWarnings("unchecked") Map<String, Object> ln = (Map<String, Object>) p[1];
            if (!"PENDING_PACK_APPROVAL".equals(str(ln, "status"))) continue;
            final String lineId = (String) p[0];
            LinearLayout c = CircleUi.card(this);
            c.addView(CircleUi.text(this, "Withdrawal request", 14, R.color.colorTextPrimary, true));
            c.addView(CircleUi.text(this, shortAddr(str(ln, "memberAddress")) + " wants to withdraw " + CircleUi.harv(num(ln, "amountPaidRaw")) + ".", 12.5f, R.color.colorTextSecondary, false));
            c.addView(actionButton(new GroupAction(GroupAction.ACCUM_APPROVE_PACK, "Approve", true), x ->
                pay("Approve withdrawal", CircleUi.harv(num(ln, "amountPaidRaw")),
                    moneyRows(str(ln, "memberAddress"), "Sends this member's savings out of the group"),
                    () -> NodeGroupClient.approveAccumPack(getApplicationContext(), me, nodeId, lineId, result("Approved")))));
            content.addView(c);
        }
    }

    // ── actions ────────────────────────────────────────────────────────────

    /**
     * A new goal for a goal group, done entirely in the app: it is a new goal group made with the same payout rule and beneficiary, a
     * name and a target of the member's choosing, and the same profile (picture, place, rules). The network charges its usual creation fee.
     */
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
        long old = (long) num(group, "targetGoalAmountRaw");
        if (old > 0) target.setText(String.valueOf(old / 100_000_000.0).replaceAll("\\.0$", ""));
        box.addView(name);
        box.addView(target);
        new com.harvana.app.ui.SheetDialog.Builder(this).setTitle("Start a new goal").setView(box).setNegativeButton("Cancel", null)
            .setPositiveButton("Continue", (d, w) -> {
                final String n = name.getText().toString().trim();
                final long raw = CircleUi.toRaw(target.getText().toString());
                if (n.isEmpty() || raw <= 0) { toast("Enter a name and a target."); return; }
                pay("Start a new goal", CircleUi.harv(100_000_000L),
                    moneyRows("The network", "Creation fee. Target " + CircleUi.harv(raw) + " for \u201c" + n + "\u201d. Nothing is locked."),
                    () -> createGoal(n, raw, rule, who));
            }).show();
    }

    private void createGoal(final String goalName, final long targetRaw, String rule, String beneficiary) {
        final Context ctx = getApplicationContext();
        final String desc = fsData != null && fsData.get("groupDescription") != null ? String.valueOf(fsData.get("groupDescription")) : "";
        NodeGroupClient.createFixedGroup(ctx, me, targetRaw, rule, beneficiary, 0L, goalName, desc, new NodeGroupClient.Callback() {
            @Override public void onSuccess(JSONObject json) {
                final String id = json.optString("groupId", json.optString("circleId", ""));
                if (id.isEmpty()) { toast("The network accepted it but returned no id."); return; }
                NodeGroupClient.getFixedGroup(id, new NodeGroupClient.Callback() {
                    @Override public void onSuccess(JSONObject g) { saveGoal(id, goalName, targetRaw, g.optString("escrowAddress", "")); }
                    @Override public void onError(String e) { saveGoal(id, goalName, targetRaw, ""); }
                });
            }
            @Override public void onError(String error) { toast(CircleUi.friendly(error)); load(); }
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

    /**
     * Pays the picked day boxes of the savings line. Missed days go through back-pay (those exact dates); today and days ahead are
     * paid in order from today as one multi-day contribution. A mix is two payments, back-pay first, behind one review and PIN.
     */
    private void paySelection(java.util.List<String> dates) {
        if (dates == null || dates.isEmpty() || line == null) return;
        final String todayIso = GroupTypeBoard.isoDay(System.currentTimeMillis());
        final java.util.List<String> back = new java.util.ArrayList<>(), ahead = new java.util.ArrayList<>();
        for (String d : dates) { if (d.compareTo(todayIso) < 0) back.add(d); else ahead.add(d); }
        final long daily = (long) num(line, "dailyAmountRaw");
        final String lineId = str(line, "lineId");
        long total = daily * dates.size();
        String what = (back.isEmpty() ? "" : back.size() + (back.size() == 1 ? " missed day" : " missed days"))
                + (back.isEmpty() || ahead.isEmpty() ? "" : " and ")
                + (ahead.isEmpty() ? "" : ahead.size() + (ahead.size() == 1 ? " day" : " days") + " from today");
        final Context ctx = getApplicationContext();
        pay(dates.size() == 1 ? "Pay for this day" : "Pay for " + dates.size() + " days", CircleUi.harv(total),
            moneyRows(escrowOrGroup(), what + " at " + CircleUi.harv(daily) + " each"),
            () -> {
                if (back.isEmpty()) {
                    if (ahead.size() == 1) NodeGroupClient.contributeAccum(ctx, me, nodeId, lineId, result("Saved"));
                    else NodeGroupClient.contributeAccumMultiple(ctx, me, nodeId, lineId, daily * ahead.size(), result("Saved"));
                    return;
                }
                NodeGroupClient.backpayAccum(ctx, me, nodeId, lineId, back, ahead.isEmpty()
                    ? result("Paid back. It shows once the network confirms it.")
                    : new NodeGroupClient.Callback() {
                        @Override public void onSuccess(JSONObject json) {
                            if (ahead.size() == 1) NodeGroupClient.contributeAccum(ctx, me, nodeId, lineId, result("Saved"));
                            else NodeGroupClient.contributeAccumMultiple(ctx, me, nodeId, lineId, daily * ahead.size(), result("Saved"));
                        }
                        @Override public void onError(String error) { toast(CircleUi.friendly(error)); load(); }
                    });
            });
    }

    /** True when the savings calendar is shown: its today box is the way to save today, so the separate button is left out. */
    private boolean boardPaysToday;

    private void onAction(GroupAction a) {
        switch (a.id) {
            case GroupAction.JOIN: confirmJoin(); break;
            case GroupAction.CONTRIBUTE:
                pay("Round contribution", CircleUi.harv(num(group, "contributionAmountRaw")),
                    moneyRows(escrowOrGroup(), "Your payment for this round"),
                    () -> NodeGroupClient.contributeCircle(getApplicationContext(), me, nodeId, result("Payment sent")));
                break;
            case GroupAction.WITHDRAW_BOND:
                pay("Withdraw bond", CircleUi.harv(bondOf()),
                    moneyRows("Your wallet", "Returned only if this group allows it"),
                    () -> NodeGroupClient.withdrawCircleBond(getApplicationContext(), me, nodeId, result("Bond withdrawal requested")));
                break;
            case GroupAction.START_LINE: startLineDialog(); break;
            case GroupAction.ACCUM_CONTRIBUTE:
                pay("Save today", CircleUi.harv(line == null ? 0 : num(line, "dailyAmountRaw")),
                    moneyRows(escrowOrGroup(), "One day on your savings line"),
                    () -> NodeGroupClient.contributeAccum(getApplicationContext(), me, nodeId, a.ref, result("Saved")));
                break;
            case GroupAction.ACCUM_CONTRIBUTE_MULTI: saveMoreDialog(a.ref); break;
            case GroupAction.ACCUM_PACK:
                pay("Request withdrawal", CircleUi.harv(line == null ? 0 : num(line, "amountPaidRaw")),
                    moneyRows("Your wallet", "The admin still has to approve this"),
                    () -> NodeGroupClient.packAccumLine(getApplicationContext(), me, nodeId, a.ref, indexed("Withdrawal requested. Waiting for the admin.",
                        GroupProposalIndex.PACK, a.ref, "Withdrawal request")));
                break;
            case GroupAction.ACCUM_CLAIM:
                pay("Claim savings", CircleUi.harv(line == null ? 0 : num(line, "amountPaidRaw")),
                    moneyRows("Your wallet", "Pays your savings line out to you"),
                    () -> NodeGroupClient.claimAccumLine(getApplicationContext(), me, nodeId, a.ref, result("Savings claimed")));
                break;
            case GroupAction.FIXED_DEPOSIT: amountDialog("Deposit", "Amount to deposit", raw ->
                pay("Deposit", CircleUi.harv(raw), moneyRows(escrowOrGroup(), "Added to this goal group"),
                    () -> NodeGroupClient.depositFixed(getApplicationContext(), me, nodeId, raw, result("Deposit sent")))); break;
            case GroupAction.FIXED_REQUEST_DISBURSEMENT:
                pay("Request payout", potLabel(), moneyRows(escrowOrGroup(), "The group votes before anyone is paid"),
                    () -> NodeGroupClient.requestFixedDisbursement(getApplicationContext(), me, nodeId,
                        indexedFromThenVote("requestId", "Payout requested. The group will vote.", GroupProposalIndex.DISBURSEMENT, "Payout request")));
                break;
            case GroupAction.FIXED_REQUEST_MILESTONE: requestDialog("Request a milestone release", "What will the money be used for?", (raw, text) ->
                pay("Request milestone", CircleUi.harv(raw), moneyRows(escrowOrGroup(), text),
                    () -> NodeGroupClient.requestFixedMilestone(getApplicationContext(), me, nodeId, raw, text,
                        indexedFromThenVote("milestoneId", "Milestone requested. The group will vote.", GroupProposalIndex.MILESTONE, "Milestone request")))); break;
            case GroupAction.FIXED_REQUEST_GRANT: requestDialog("Ask for an emergency grant", "Why do you need help?", (raw, text) ->
                pay("Request emergency grant", CircleUi.harv(raw), moneyRows("Your wallet", text),
                    () -> NodeGroupClient.requestFixedGrant(getApplicationContext(), me, nodeId, raw, text,
                        indexedFrom("grantId", "Grant requested. The group will vote.", GroupProposalIndex.GRANT, "Emergency grant request")))); break;
            default: break;
        }
    }

    private void vote(String kind, String id, boolean yes) {
        pin(() -> {
            NodeGroupClient.Callback cb = result(yes ? "Voted yes" : "Voted no");
            if (GroupProposalIndex.DISBURSEMENT.equals(kind)) NodeGroupClient.voteFixedDisbursement(getApplicationContext(), me, nodeId, id, yes, cb);
            else if (GroupProposalIndex.MILESTONE.equals(kind)) NodeGroupClient.voteFixedMilestone(getApplicationContext(), me, nodeId, id, yes, cb);
            else NodeGroupClient.voteFixedGrant(getApplicationContext(), me, nodeId, id, yes, cb);
        });
    }

    private void confirmJoin() {
        final long bond = num(group, "requiredBondRaw"), contribution = num(group, "contributionAmountRaw");
        java.util.ArrayList<String[]> rows = new java.util.ArrayList<>();
        rows.add(new String[]{"Bond locked now", CircleUi.harv(bond)});
        rows.add(new String[]{"Each round after that", CircleUi.harv(contribution)});
        rows.add(new String[]{"To", escrowOrGroup()});
        rows.add(new String[]{"Bond comes back", "When the group finishes cleanly. Missed payments are taken from it."});
        rows.add(new String[]{"You are signing", "The exact terms already set for this group"});
        pay("Join this group", CircleUi.harv(bond), rows, () ->
            NodeGroupClient.joinCircle(getApplicationContext(), me, nodeId, str(group, "termsHash"), result("You joined")));
    }

    private void startLineDialog() {
        final LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int p = CircleUi.dp(this, 16);
        box.setPadding(p, p, p, 0);
        final EditText daily = new com.google.android.material.textfield.TextInputEditText(this);
        daily.setHint("Amount to save each day");
        daily.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        box.addView(daily);
        final EditText lockDays = new com.google.android.material.textfield.TextInputEditText(this);
        lockDays.setHint("Lock for how many days? (leave empty for a flexible line)");
        lockDays.setInputType(InputType.TYPE_CLASS_NUMBER);
        box.addView(lockDays);
        new com.harvana.app.ui.SheetDialog.Builder(this).setTitle("Start my savings line")
            .setMessage("A flexible line can be withdrawn with admin approval. A locked line can't be touched before its date.")
            .setView(box).setNegativeButton("Cancel", null)
            .setPositiveButton("Start", (d, w) -> {
                long raw = CircleUi.toRaw(daily.getText().toString());
                if (raw <= 0) { toast("Enter a daily amount."); return; }
                String days = lockDays.getText().toString().trim();
                final Long until = days.isEmpty() ? null : System.currentTimeMillis() + Long.parseLong(days) * 86_400_000L;
                pay("Start a savings line", CircleUi.harv(raw),
                    moneyRows("Your savings line", until == null
                        ? "Flexible. Opening the line does not pay today."
                        : "Locked until the date you chose. Opening it does not pay today."),
                    () -> NodeGroupClient.startAccumLine(getApplicationContext(), me, nodeId, raw, false,
                        until == null ? "FLEXIBLE" : "LOCKED", until, "ONGOING", null, result("Savings line started")));
            }).show();
    }

    private void saveMoreDialog(String lineId) {
        boolean ongoing = line == null || "ONGOING".equals(str(line, "depositMode")) || str(line, "depositMode").isEmpty();
        final long dailyRaw = line == null ? 0 : num(line, "dailyAmountRaw");
        if (ongoing && dailyRaw > 0) {
            final EditText days = new com.google.android.material.textfield.TextInputEditText(this);
            days.setHint("How many days to save now?");
            days.setInputType(InputType.TYPE_CLASS_NUMBER);
            new com.harvana.app.ui.SheetDialog.Builder(this).setTitle("Save several days").setView(days).setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (d, w) -> {
                    int n = 0;
                    try { n = Integer.parseInt(days.getText().toString().trim()); } catch (Exception ignored) { }
                    if (n < 2) { toast("Enter 2 or more days."); return; }
                    final long total = dailyRaw * n;
                    pay("Save several days", CircleUi.harv(total),
                        moneyRows(escrowOrGroup(), n + " days at " + CircleUi.harv(dailyRaw) + " each"),
                        () -> NodeGroupClient.contributeAccumMultiple(getApplicationContext(), me, nodeId, lineId, total, result("Saved " + CircleUi.harv(total))));
                }).show();
        } else {
            amountDialog("Add to my line", "Amount", raw -> pay("Add to my line", CircleUi.harv(raw),
                moneyRows(escrowOrGroup(), "Extra saving on this line"),
                () -> NodeGroupClient.contributeAccumVariable(getApplicationContext(), me, nodeId, lineId, raw, result("Saved"))));
        }
    }

    private interface RawCb { void got(long raw); }
    private interface RawTextCb { void got(long raw, String text); }

    private void amountDialog(String title, String hint, RawCb cb) {
        final EditText e = new com.google.android.material.textfield.TextInputEditText(this);
        e.setHint(hint);
        e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        new com.harvana.app.ui.SheetDialog.Builder(this).setTitle(title).setView(e).setNegativeButton("Cancel", null)
            .setPositiveButton("Continue", (d, w) -> {
                long raw = CircleUi.toRaw(e.getText().toString());
                if (raw <= 0) { toast("Enter an amount."); return; }
                cb.got(raw);
            }).show();
    }

    private void requestDialog(String title, String textHint, RawTextCb cb) {
        final LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int p = CircleUi.dp(this, 16);
        box.setPadding(p, p, p, 0);
        final EditText amount = new com.google.android.material.textfield.TextInputEditText(this);
        amount.setHint("Amount");
        amount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        box.addView(amount);
        final EditText text = new com.google.android.material.textfield.TextInputEditText(this);
        text.setHint(textHint);
        box.addView(text);
        new com.harvana.app.ui.SheetDialog.Builder(this).setTitle(title)
            .setMessage("Everyone who can vote will see this. Be clear and honest: the description is permanent.")
            .setView(box).setNegativeButton("Cancel", null)
            .setPositiveButton("Send", (d, w) -> {
                long raw = CircleUi.toRaw(amount.getText().toString());
                String t = text.getText().toString().trim();
                if (raw <= 0 || t.length() < 5) { toast("Enter an amount and a short explanation."); return; }
                cb.got(raw, t);
            }).show();
    }

    // ── plumbing ───────────────────────────────────────────────────────────

    private NodeGroupClient.Callback result(String ok) {
        return new NodeGroupClient.Callback() {
            @Override public void onSuccess(JSONObject json) {
                com.harvana.app.trust.AppActivity.note(NodeGroupActivity.this, "GROUP", ok);
                toast(ok); load();
            }
            @Override public void onError(String error) { toast(CircleUi.friendly(error)); load(); }
        };
    }

    /** Success also records a pointer so other members can find the proposal (see GroupProposalIndex). */
    private NodeGroupClient.Callback indexedFrom(String idKey, String ok, String kind, String title) {
        return new NodeGroupClient.Callback() {
            @Override public void onSuccess(JSONObject json) {
                GroupProposalIndex.post(getApplicationContext(), fsGroupId, kind, json.optString(idKey, ""), title, me);
                com.harvana.app.trust.AppActivity.note(NodeGroupActivity.this, "GROUP", ok);
                toast(ok); load();
            }
            @Override public void onError(String error) { toast(CircleUi.friendly(error)); load(); }
        };
    }

    /**
     * For a request the admin makes (payout, milestone): the admin approves by asking, so their yes vote is cast right after.
     * The node counts yes votes toward its quorum, and a two-person group could not finish without it.
     */
    private NodeGroupClient.Callback indexedFromThenVote(String idKey, String ok, String kind, String title) {
        return new NodeGroupClient.Callback() {
            @Override public void onSuccess(JSONObject json) {
                String id = json.optString(idKey, "");
                GroupProposalIndex.post(getApplicationContext(), fsGroupId, kind, id, title, me);
                com.harvana.app.trust.AppActivity.note(NodeGroupActivity.this, "GROUP", ok);
                if (id.isEmpty()) { toast(ok); load(); return; }
                NodeGroupClient.Callback voted = new NodeGroupClient.Callback() {
                    @Override public void onSuccess(JSONObject v) { toast(ok + " Your yes vote is in."); load(); }
                    @Override public void onError(String e) { toast(ok + " Your own yes vote did not go through: vote from the list below."); load(); }
                };
                if (GroupProposalIndex.MILESTONE.equals(kind)) NodeGroupClient.voteFixedMilestone(getApplicationContext(), me, nodeId, id, true, voted);
                else NodeGroupClient.voteFixedDisbursement(getApplicationContext(), me, nodeId, id, true, voted);
            }
            @Override public void onError(String error) { toast(CircleUi.friendly(error)); load(); }
        };
    }

    private NodeGroupClient.Callback indexed(String ok, String kind, String id, String title) {
        return new NodeGroupClient.Callback() {
            @Override public void onSuccess(JSONObject json) {
                GroupProposalIndex.post(getApplicationContext(), fsGroupId, kind, id, title, me);
                com.harvana.app.trust.AppActivity.note(NodeGroupActivity.this, "GROUP", ok);
                toast(ok); load();
            }
            @Override public void onError(String error) { toast(CircleUi.friendly(error)); load(); }
        };
    }

    private Button actionButton(GroupAction a) {
        return actionButton(a, v -> onAction(a));
    }

    private Button actionButton(GroupAction a, View.OnClickListener go) {
        Button bt = button(a.label, a.primary && a.enabled, v -> {
            if (!a.enabled) return;
            pressed = (Button) v;
            go.onClick(v);
        });
        bt.setEnabled(a.enabled);
        if (!a.enabled) bt.setAlpha(0.55f);
        return bt;
    }

    private void markBusy(Button source) {
        if (source == null) return;
        source.setEnabled(false);
        source.setText("Working…");
    }

    private void pin(Runnable work) {
        String action = "Authorize this before it is sent.";
        if (pressed != null && pressed.getText() != null && pressed.getText().length() > 0) {
            action = "Authorize: " + pressed.getText();
        }
        pin(action, work);
    }

    private void pin(String action, Runnable work) {
        final Button source = pressed;
        pressed = null;
        java.util.ArrayList<String[]> rows = new java.util.ArrayList<>();
        rows.add(new String[]{"You are authorizing", action});
        pay("Review before you continue", "", rows, () -> {
            markBusy(source);
            work.run();
        });
    }

    /** Summary of the payment, then PIN. Busy state starts only after both succeed. */
    private void pay(String title, String amount, java.util.List<String[]> rows, Runnable work) {
        final Button source = pressed;
        pressed = null;
        PaymentConfirmSheet.reviewThenAuthorize(this, title, amount, rows, () -> {
            markBusy(source);
            work.run();
        });
    }

    private java.util.ArrayList<String[]> moneyRows(String to, String detail) {
        java.util.ArrayList<String[]> rows = new java.util.ArrayList<>();
        if (groupName != null && !groupName.isEmpty()) rows.add(new String[]{"Group", groupName});
        rows.add(new String[]{"To", to == null || to.isEmpty() ? escrowOrGroup() : to});
        if (detail != null && !detail.isEmpty()) rows.add(new String[]{"For", detail});
        rows.add(new String[]{"From", "Your wallet"});
        rows.add(new String[]{"Network fee", "Taken from your wallet when this is sent"});
        return rows;
    }

    private String escrowOrGroup() {
        String escrow = str(group, "escrowAddress");
        return escrow.isEmpty() ? "This group's network wallet" : escrow;
    }

    private long bondOf() {
        long bond = num(member, "bondRaw");
        if (bond <= 0) bond = num(member, "bondLockedRaw");
        if (bond <= 0) bond = num(group, "requiredBondRaw");
        return bond;
    }

    private String potLabel() {
        long pot = num(group, "totalDepositedRaw");
        if (pot <= 0) pot = num(group, "currentBalanceRaw");
        return pot > 0 ? CircleUi.harv(pot) : "Group balance";
    }

    private Button button(String label, boolean primary, View.OnClickListener l) {
        Button bt = com.harvana.app.ui.Btn.of(this, label, primary, l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, CircleUi.dp(this, 52));
        lp.topMargin = CircleUi.dp(this, 12);
        bt.setLayoutParams(lp);
        return bt;
    }

    private void message(String m) {
        content.removeAllViews();
        content.addView(CircleUi.text(this, m, 14, R.color.colorTextSecondary, false));
    }

    private void toast(String m) { runOnUiThread(() -> Toast.makeText(this, m, Toast.LENGTH_LONG).show()); }

    private void copy(String text) {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("Group", text));
        toast("Copied");
    }

    private static String titleFor(String f) {
        return "CIRCLE".equals(f) ? "Rotating group" : "ACCUM".equals(f) ? "Savings group" : "FIXED".equals(f) ? "Goal group" : "Group";
    }
    private static String shortId(String id) { return id.length() > 14 ? id.substring(0, 10) + "…" + id.substring(id.length() - 4) : id; }
    private static String shortAddr(String a) { return a.length() > 14 ? a.substring(0, 8) + "…" + a.substring(a.length() - 4) : a; }
    private static String orEmpty(String s) { return s == null ? "" : s; }
    private static String str(Map<String, Object> m, String k) { Object o = m == null ? null : m.get(k); return o == null ? "" : String.valueOf(o); }
    private static long num(Map<String, Object> m, String k) { Object o = m == null ? null : m.get(k); return o instanceof Number ? ((Number) o).longValue() : 0; }
    private static String join(List<String> l) { StringBuilder sb = new StringBuilder(); for (String s : l) { if (sb.length() > 0) sb.append(", "); sb.append(s); } return sb.toString(); }
}
