package com.harvanna.app.group;

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

import com.harvanna.app.R;
import com.harvanna.app.circle.CircleUi;
import com.harvanna.app.engine.AccumSnapshot;
import com.harvanna.app.engine.CircleSnapshot;
import com.harvanna.app.engine.FixedSnapshot;
import com.harvanna.app.engine.GroupAction;
import com.harvanna.app.engine.GroupSnapshot;
import com.harvanna.app.engine.VoteView;
import com.harvanna.app.sync.HarvannaNodeClient;
import com.harvanna.app.sync.NodeGroupClient;
import com.harvanna.app.utils.JsonMaps;
import com.harvanna.app.wallet.PinPromptHelper;
import com.harvanna.app.wallet.WalletManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * One screen for a group that lives on the node (Circle, Accumulative or Fixed). Everything shown comes from
 * the node; the engine classes decide what the member may do; every action is a signed request. Firestore is
 * used only to find open proposals (see GroupProposalIndex).
 */
public class NodeGroupActivity extends AppCompatActivity {
    private String nodeId = "", fsGroupId = "", me = "", family = "";
    private SwipeRefreshLayout refresh;
    private LinearLayout content;

    private Map<String, Object> group, member, line;
    private final List<Object[]> proposals = new ArrayList<>();   // {kind, id, Map record}
    private final List<Object[]> packRequests = new ArrayList<>(); // {lineId, Map line}
    private String trustTier = "";
    private List<String> myLabels = new ArrayList<>();

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        nodeId = orEmpty(getIntent().getStringExtra("nodeGroupId"));
        if (nodeId.isEmpty()) nodeId = orEmpty(getIntent().getStringExtra("circleId"));
        fsGroupId = orEmpty(getIntent().getStringExtra("firestoreGroupId"));
        family = NodeGroupClient.familyOf(nodeId);
        me = WalletManager.getActiveWalletAddress(this);
<<<<<<< HEAD
=======
        setTitle(titleFor(family));
>>>>>>> main

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
<<<<<<< HEAD
        com.harvanna.app.utils.AppBar.setContent(this, refresh, titleFor(family), nodeId);
        if (!fsGroupId.isEmpty()) {
            com.harvanna.app.firestore.GroupFirestoreManager.getInstance(this).getGroup(fsGroupId,
                new com.harvanna.app.firestore.GroupFirestoreManager.DocCallback() {
                    @Override public void onSuccess(java.util.Map<String, Object> data) {
                        Object n = data.get("groupName");
                        if (n != null && !String.valueOf(n).isEmpty()) {
                            runOnUiThread(() -> com.harvanna.app.utils.AppBar.setSubtitle(NodeGroupActivity.this, String.valueOf(n)));
                        }
                    }
                    @Override public void onError(String e) { }
                });
        }
=======
        setContentView(CircleUi.withToolbar(this, titleFor(family), refresh));
>>>>>>> main
        load();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (group != null) load();
        if (me != null && !me.isEmpty()) {
            com.harvanna.app.sync.LockSigner.checkPending(me, err -> runOnUiThread(() -> { if (err == null && group != null) load(); }));
        }
    }

    // ── loading ────────────────────────────────────────────────────────────

    private void load() {
        refresh.setRefreshing(true);
        NodeGroupClient.Callback onGroup = new NodeGroupClient.Callback() {
            @Override public void onSuccess(JSONObject json) { group = JsonMaps.toMap(json); loadMember(); }
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
        proposals.clear(); packRequests.clear(); trustTier = ""; myLabels = new ArrayList<>();
        final AtomicInteger left = new AtomicInteger(3);
        final Runnable done = () -> { if (left.decrementAndGet() == 0) runOnUiThread(this::render); };

        HarvannaNodeClient.nodeGet("/api/v1/wallet/group-trust/" + nodeId, new HarvannaNodeClient.JsonCallback() {
            @Override public void onSuccess(JSONObject j) { trustTier = j.optString("tier", j.optString("trustTier", "")); done.run(); }
            @Override public void onError(String e) { done.run(); }
        });
        if (me == null || me.isEmpty()) { done.run(); } else {
            HarvannaNodeClient.nodeGet("/api/v1/wallet/labels/" + me, new HarvannaNodeClient.JsonCallback() {
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
        if ("CIRCLE".equals(family)) { done.run(); return; }
        GroupProposalIndex.list(this, fsGroupId, entries -> loadProposals(entries, done));
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

    private void render() {
        refresh.setRefreshing(false);
        content.removeAllViews();
        GroupSnapshot s = snapshot();

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

        for (GroupAction a : s.actions) content.addView(button(a.label, a.primary, v -> onAction(a)));

        renderProposals();
        renderPackRequests();
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
            VoteView v = VoteView.of(title, rec, me == null ? "" : me);
            LinearLayout c = CircleUi.card(this);
            c.addView(CircleUi.text(this, v.title, 14, R.color.colorTextPrimary, true));
            c.addView(CircleUi.text(this, v.explain(), 12.5f, R.color.colorTextSecondary, false));
            if (v.open && v.eligible) {
                LinearLayout row = new LinearLayout(this);
                row.addView(button("Vote yes", true, x -> vote(kind, id, true)), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                row.addView(button("Vote no", false, x -> vote(kind, id, false)), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                c.addView(row);
            }
            content.addView(c);
        }
    }

    private void renderPackRequests() {
        boolean admin = group != null && me != null && me.equals(str(group, "creatorAddress"));
        if (!admin) return;
        for (Object[] p : packRequests) {
            @SuppressWarnings("unchecked") Map<String, Object> ln = (Map<String, Object>) p[1];
            if (!"PENDING_PACK_APPROVAL".equals(str(ln, "status"))) continue;
            final String lineId = (String) p[0];
            LinearLayout c = CircleUi.card(this);
            c.addView(CircleUi.text(this, "Withdrawal request", 14, R.color.colorTextPrimary, true));
            c.addView(CircleUi.text(this, shortAddr(str(ln, "memberAddress")) + " wants to withdraw " + CircleUi.harv(num(ln, "amountPaidRaw")) + ".", 12.5f, R.color.colorTextSecondary, false));
            c.addView(button("Approve", true, x -> PinPromptHelper.verifyPinThenProceed(this, () ->
                NodeGroupClient.approveAccumPack(getApplicationContext(), me, nodeId, lineId, result("Approved")))));
            content.addView(c);
        }
    }

    // ── actions ────────────────────────────────────────────────────────────

    private void onAction(GroupAction a) {
        switch (a.id) {
            case GroupAction.JOIN: confirmJoin(); break;
            case GroupAction.CONTRIBUTE: pin(() -> NodeGroupClient.contributeCircle(getApplicationContext(), me, nodeId, result("Payment sent"))); break;
            case GroupAction.WITHDRAW_BOND: pin(() -> NodeGroupClient.withdrawCircleBond(getApplicationContext(), me, nodeId, result("Bond withdrawal requested"))); break;
            case GroupAction.START_LINE: startLineDialog(); break;
            case GroupAction.ACCUM_CONTRIBUTE: pin(() -> NodeGroupClient.contributeAccum(getApplicationContext(), me, nodeId, a.ref, result("Saved"))); break;
            case GroupAction.ACCUM_CONTRIBUTE_MULTI: saveMoreDialog(a.ref); break;
            case GroupAction.ACCUM_PACK: pin(() -> NodeGroupClient.packAccumLine(getApplicationContext(), me, nodeId, a.ref, indexed("Withdrawal requested. Waiting for the admin.",
                GroupProposalIndex.PACK, a.ref, "Withdrawal request"))); break;
            case GroupAction.ACCUM_CLAIM: pin(() -> NodeGroupClient.claimAccumLine(getApplicationContext(), me, nodeId, a.ref, result("Savings claimed"))); break;
            case GroupAction.FIXED_DEPOSIT: amountDialog("Deposit", "Amount to deposit", raw ->
                pin(() -> NodeGroupClient.depositFixed(getApplicationContext(), me, nodeId, raw, result("Deposit sent")))); break;
            case GroupAction.FIXED_REQUEST_DISBURSEMENT: pin(() -> NodeGroupClient.requestFixedDisbursement(getApplicationContext(), me, nodeId,
                indexedFrom("requestId", "Payout requested. The group will vote.", GroupProposalIndex.DISBURSEMENT, "Payout request"))); break;
            case GroupAction.FIXED_REQUEST_MILESTONE: requestDialog("Request a milestone release", "What will the money be used for?", (raw, text) ->
                pin(() -> NodeGroupClient.requestFixedMilestone(getApplicationContext(), me, nodeId, raw, text,
                    indexedFrom("milestoneId", "Milestone requested. The group will vote.", GroupProposalIndex.MILESTONE, "Milestone request")))); break;
            case GroupAction.FIXED_REQUEST_GRANT: requestDialog("Ask for an emergency grant", "Why do you need help?", (raw, text) ->
                pin(() -> NodeGroupClient.requestFixedGrant(getApplicationContext(), me, nodeId, raw, text,
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
        new AlertDialog.Builder(this)
            .setTitle("Join this group?")
            .setMessage("You will lock a bond of " + CircleUi.harv(bond) + " now and pay " + CircleUi.harv(contribution)
                + " each round. The bond is returned when the group completes cleanly; missed payments are taken from it with a penalty.\n\nBy joining you sign these exact terms.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Sign and join", (d, w) -> pin(() ->
                NodeGroupClient.joinCircle(getApplicationContext(), me, nodeId, str(group, "termsHash"), result("You joined"))))
            .show();
    }

    private void startLineDialog() {
        final LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int p = CircleUi.dp(this, 16);
        box.setPadding(p, p, p, 0);
        final EditText daily = new EditText(this);
        daily.setHint("Amount to save each day");
        daily.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        box.addView(daily);
        final EditText lockDays = new EditText(this);
        lockDays.setHint("Lock for how many days? (leave empty for a flexible line)");
        lockDays.setInputType(InputType.TYPE_CLASS_NUMBER);
        box.addView(lockDays);
        new AlertDialog.Builder(this).setTitle("Start my savings line")
            .setMessage("A flexible line can be withdrawn with admin approval. A locked line can't be touched before its date.")
            .setView(box).setNegativeButton("Cancel", null)
            .setPositiveButton("Start", (d, w) -> {
                long raw = CircleUi.toRaw(daily.getText().toString());
                if (raw <= 0) { toast("Enter a daily amount."); return; }
                String days = lockDays.getText().toString().trim();
                final Long until = days.isEmpty() ? null : System.currentTimeMillis() + Long.parseLong(days) * 86_400_000L;
                pin(() -> NodeGroupClient.startAccumLine(getApplicationContext(), me, nodeId, raw, false,
                    until == null ? "FLEXIBLE" : "LOCKED", until, "ONGOING", null, result("Savings line started")));
            }).show();
    }

    private void saveMoreDialog(String lineId) {
        boolean ongoing = line == null || "ONGOING".equals(str(line, "depositMode")) || str(line, "depositMode").isEmpty();
        final long dailyRaw = line == null ? 0 : num(line, "dailyAmountRaw");
        if (ongoing && dailyRaw > 0) {
            final EditText days = new EditText(this);
            days.setHint("How many days to save now?");
            days.setInputType(InputType.TYPE_CLASS_NUMBER);
            new AlertDialog.Builder(this).setTitle("Save several days").setView(days).setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (d, w) -> {
                    int n = 0;
                    try { n = Integer.parseInt(days.getText().toString().trim()); } catch (Exception ignored) { }
                    if (n < 2) { toast("Enter 2 or more days."); return; }
                    final long total = dailyRaw * n;
                    pin(() -> NodeGroupClient.contributeAccumMultiple(getApplicationContext(), me, nodeId, lineId, total, result("Saved " + CircleUi.harv(total))));
                }).show();
        } else {
            amountDialog("Add to my line", "Amount", raw -> pin(() ->
                NodeGroupClient.contributeAccumVariable(getApplicationContext(), me, nodeId, lineId, raw, result("Saved"))));
        }
    }

    private interface RawCb { void got(long raw); }
    private interface RawTextCb { void got(long raw, String text); }

    private void amountDialog(String title, String hint, RawCb cb) {
        final EditText e = new EditText(this);
        e.setHint(hint);
        e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        new AlertDialog.Builder(this).setTitle(title).setView(e).setNegativeButton("Cancel", null)
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
        final EditText amount = new EditText(this);
        amount.setHint("Amount");
        amount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        box.addView(amount);
        final EditText text = new EditText(this);
        text.setHint(textHint);
        box.addView(text);
        new AlertDialog.Builder(this).setTitle(title)
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

    private void pin(Runnable r) { PinPromptHelper.verifyPinThenProceed(this, r::run); }

    private NodeGroupClient.Callback result(String ok) {
        return new NodeGroupClient.Callback() {
            @Override public void onSuccess(JSONObject json) { toast(ok); load(); }
            @Override public void onError(String error) { toast(CircleUi.friendly(error)); }
        };
    }

    /** Success also records a pointer so other members can find the proposal (see GroupProposalIndex). */
    private NodeGroupClient.Callback indexedFrom(String idKey, String ok, String kind, String title) {
        return new NodeGroupClient.Callback() {
            @Override public void onSuccess(JSONObject json) {
                GroupProposalIndex.post(getApplicationContext(), fsGroupId, kind, json.optString(idKey, ""), title, me);
                toast(ok); load();
            }
            @Override public void onError(String error) { toast(CircleUi.friendly(error)); }
        };
    }

    private NodeGroupClient.Callback indexed(String ok, String kind, String id, String title) {
        return new NodeGroupClient.Callback() {
            @Override public void onSuccess(JSONObject json) {
                GroupProposalIndex.post(getApplicationContext(), fsGroupId, kind, id, title, me);
                toast(ok); load();
            }
            @Override public void onError(String error) { toast(CircleUi.friendly(error)); }
        };
    }

    private Button button(String label, boolean primary, View.OnClickListener l) {
        Button bt = new Button(this);
        bt.setText(label);
        bt.setAllCaps(false);
        bt.setTextColor(CircleUi.color(this, primary ? R.color.colorTextOnAccent : R.color.colorAccentStrong));
        bt.setBackgroundResource(primary ? R.drawable.bg_btn_primary : R.drawable.bg_btn_outline_accent);
        bt.setOnClickListener(l);
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
