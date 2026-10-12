package com.harvana.app.group;

import android.app.Activity;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.harvana.app.R;
import com.harvana.app.circle.CircleUi;
import com.harvana.app.engine.VoteView;
import com.harvana.app.utils.JsonMaps;
import com.harvana.app.sync.NodeGroupClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Everything a goal group needs below its payment buttons, so there is no separate Actions & votes or Goals page for it:
 * open votes (payout, milestone, grant) with Yes and No, "Start a new goal" for the admin, "Return everyone's money"
 * (propose, vote, settle) and the goals the group has already finished.
 */
public final class GoalGroupPanel {
    private GoalGroupPanel() {}

    public static void render(NodePayActions.Ctx c, LinearLayout host) {
        if (c == null || c.activity == null || host == null || c.nodeId.isEmpty()) return;
        LinearLayout box = new LinearLayout(c.activity);
        box.setOrientation(LinearLayout.VERTICAL);
        host.addView(box);
        newGoal(c, box);
        NodeGroupClient.listFixedProposals(c.nodeId, new NodeGroupClient.Callback() {
            @Override public void onSuccess(JSONObject j) { c.activity.runOnUiThread(() -> votes(c, box, j)); }
            @Override public void onError(String e) { }
        });
        LinearLayout dissolveBox = new LinearLayout(c.activity);
        dissolveBox.setOrientation(LinearLayout.VERTICAL);
        box.addView(dissolveBox);
        NodeGroupClient.getFixedDissolve(c.nodeId, new NodeGroupClient.Callback() {
            @Override public void onSuccess(JSONObject j) { c.activity.runOnUiThread(() -> dissolve(c, dissolveBox, JsonMaps.toMap(j))); }
            @Override public void onError(String e) { c.activity.runOnUiThread(() -> dissolve(c, dissolveBox, null)); }
        });
        pastGoals(c, box);
    }

    // ── open votes ─────────────────────────────────────────────────────────

    private static void votes(NodePayActions.Ctx c, LinearLayout box, JSONObject j) {
        JSONArray arr = j == null ? null : j.optJSONArray("proposals");
        if (arr == null || arr.length() == 0) return;
        List<LinearLayout> cards = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject row = arr.optJSONObject(i);
            if (row == null) continue;
            String kind = row.optString("kind", ""), id = row.optString("id", "");
            String label = GroupProposalIndex.DISBURSEMENT.equals(kind) ? "Payout request"
                    : GroupProposalIndex.MILESTONE.equals(kind) ? "Milestone release" : "Emergency grant";
            Map<String, Object> rec = JsonMaps.toMap(row);
            VoteView v = VoteView.of(label, rec, c.me);
            if (!v.open) continue;
            LinearLayout card = CircleUi.card(c.activity);
            card.addView(CircleUi.text(c.activity, label + (num(rec, "amountRaw") > 0 ? " · " + NodePayActions.gm(c, num(rec, "amountRaw")) : ""),
                    15, R.color.colorTextPrimary, true));
            String why = !str(rec, "description").isEmpty() ? str(rec, "description") : str(rec, "reasonDescription");
            if (!why.isEmpty()) card.addView(CircleUi.text(c.activity, "“" + why + "”", 13, R.color.colorTextSecondary, false));
            card.addView(CircleUi.text(c.activity, v.yesPercent + "% yes · needs " + v.thresholdPercent + "% · "
                    + v.votersYes + " of " + v.votersTotal + " voters said yes", 12, R.color.colorTextSecondary, false));
            if (!v.eligible) {
                card.addView(CircleUi.text(c.activity, "Only the admin and the most active members vote on this.", 12, R.color.colorTextTertiary, false));
            } else {
                if (v.alreadyVoted) card.addView(CircleUi.text(c.activity, "You voted " + (v.myVoteYes ? "yes" : "no") + ". You can change it until it closes.",
                        12, R.color.colorTextSecondary, false));
                card.addView(yesNo(c, yes -> {
                    NodeGroupClient.Callback cb = NodePayActions.result(c, yes ? "Your yes vote is in" : "Your no vote is in");
                    if (GroupProposalIndex.DISBURSEMENT.equals(kind)) NodeGroupClient.voteFixedDisbursement(c.activity, c.me, c.nodeId, id, yes, cb);
                    else if (GroupProposalIndex.MILESTONE.equals(kind)) NodeGroupClient.voteFixedMilestone(c.activity, c.me, c.nodeId, id, yes, cb);
                    else NodeGroupClient.voteFixedGrant(c.activity, c.me, c.nodeId, id, yes, cb);
                }, label));
            }
            cards.add(card);
        }
        if (cards.isEmpty()) return;
        int at = 0;
        box.addView(header(c.activity, "Open votes"), at++);
        for (LinearLayout card : cards) box.addView(card, at++);
    }

    // ── start a new goal ───────────────────────────────────────────────────

    private static void newGoal(NodePayActions.Ctx c, LinearLayout box) {
        if (c.me.isEmpty() || !c.me.equalsIgnoreCase(str(c.group, "creatorAddress"))) return;
        long target = num(c.group, "targetGoalAmountRaw");
        long raised = Math.max(0L, num(c.group, "raisedAmountRaw") - num(c.group, "goalBaseRaisedRaw"));
        boolean inOne = "ALL_AT_ONCE_AT_GOAL".equals(str(c.group, "disbursementRule"));
        boolean ready = inOne ? "COMPLETED".equals(str(c.group, "state")) : target > 0 && raised >= target;
        if (!ready) return;
        box.addView(NodePayActions.button(c.activity, "Start a new goal", false, v -> newGoalSheet(c)));
    }

    /** The "Start a new goal" form. Also opened by the "+ New goal" box on the goal board. */
    public static void newGoalSheet(NodePayActions.Ctx c) {
        if (c == null || c.activity == null) return;
        if (c.me.isEmpty() || !c.me.equalsIgnoreCase(str(c.group, "creatorAddress"))) {
            NodePayActions.toast(c, "Only the group admin can start a new goal.");
            return;
        }
        long target = num(c.group, "targetGoalAmountRaw");
        long raised = Math.max(0L, num(c.group, "raisedAmountRaw") - num(c.group, "goalBaseRaisedRaw"));
        boolean inOne = "ALL_AT_ONCE_AT_GOAL".equals(str(c.group, "disbursementRule"));
        boolean ready = inOne ? "COMPLETED".equals(str(c.group, "state")) : target > 0 && raised >= target;
        if (!ready) {
            NodePayActions.toast(c, "A new goal can start once this goal is reached" + (inOne ? " and paid out." : "."));
            return;
        }
        LinearLayout form = new LinearLayout(c.activity);
        form.setOrientation(LinearLayout.VERTICAL);
        int p = CircleUi.dp(c.activity, 16);
        form.setPadding(p, p, p, 0);
        EditText name = new com.google.android.material.textfield.TextInputEditText(c.activity);
        name.setHint("Name of the new goal");
        form.addView(name);
        EditText amount = new com.google.android.material.textfield.TextInputEditText(c.activity);
        amount.setHint("Target amount");
        amount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        form.addView(amount);
        new com.harvana.app.ui.SheetDialog.Builder(c.activity).setTitle("Start a new goal")
                .setMessage("Same members, rule and wallet, with its own ledger. Nobody has to join again.")
                .setView(form).setNegativeButton("Cancel", null)
                .setPositiveButton("Continue", (d, w) -> {
                    String n = name.getText().toString().trim();
                    long raw = NodePayActions.typed(c, amount.getText().toString());
                    if (n.isEmpty() || raw <= 0) { NodePayActions.toast(c, "Enter a name and a target."); return; }
                    NodePayActions.pay(c, "Start a new goal", NodePayActions.gm(c, raw),
                            NodePayActions.moneyRows(c, NodePayActions.escrow(c), "New goal “" + n + "”. Nothing is locked."),
                            () -> NodeGroupClient.startNextFixedGoal(c.activity, c.me, c.nodeId, raw, n, NodePayActions.result(c, "New goal started.")));
                }).show();
    }

    // ── return everyone's money ────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private static void dissolve(NodePayActions.Ctx c, LinearLayout box, Map<String, Object> d) {
        box.removeAllViews();
        String state = str(c.group, "state");
        if ("DISSOLVED".equals(state)) {
            LinearLayout card = CircleUi.card(c.activity);
            card.addView(CircleUi.text(c.activity, "Everyone's money was returned", 15, R.color.colorTextPrimary, true));
            card.addView(CircleUi.text(c.activity, "This group voted to close. " + NodePayActions.gm(c, num(c.group, "dissolvedRefundRaw"))
                    + " went back to the members in proportion to what each put in.", 12.5f, R.color.colorTextSecondary, false));
            box.addView(header(c.activity, "Group closed"));
            box.addView(card);
            return;
        }
        if (!"ACTIVE".equals(state) && !state.isEmpty()) return;
        String status = d == null ? "NONE" : str(d, "status");
        boolean contributor = num(c.member, "contributedRaw") > 0;
        boolean creator = !c.me.isEmpty() && c.me.equalsIgnoreCase(str(c.group, "creatorAddress"));

        LinearLayout card = CircleUi.card(c.activity);
        card.addView(CircleUi.text(c.activity, "Return everyone's money", 15, R.color.colorTextPrimary, true));
        if (!"PENDING".equals(status)) {
            String intro = "If the group can't go on, the members can vote to close it. Everything left in the group wallet goes back "
                    + "to each member in proportion to what they put in. Votes count by money put in; if nobody says no within 7 days, it passes.";
            if ("REJECTED".equals(status)) intro = "The last vote to close this group was turned down. " + intro;
            card.addView(CircleUi.text(c.activity, intro, 12.5f, R.color.colorTextSecondary, false));
            if (contributor || creator) {
                card.addView(NodePayActions.button(c.activity, "Ask to return everyone's money", false, v -> {
                    c.pressed = (Button) v;
                    NodePayActions.pay(c, "Return everyone's money", "Group balance",
                            NodePayActions.moneyRows(c, "Each member's wallet", "Opens a vote. Your yes is counted now."),
                            () -> NodeGroupClient.proposeFixedDissolve(c.activity, c.me, c.nodeId,
                                    NodePayActions.result(c, "Vote opened. The members are asked to agree.")));
                }));
            }
            box.addView(header(c.activity, "If the group can't go on"));
            box.addView(card);
            return;
        }

        Map<String, Object> weights = d.get("weights") instanceof Map ? (Map<String, Object>) d.get("weights") : new java.util.HashMap<>();
        Map<String, Object> votes = d.get("votes") instanceof Map ? (Map<String, Object>) d.get("votes") : new java.util.HashMap<>();
        long total = 0, yes = 0, no = 0;
        for (Map.Entry<String, Object> e : weights.entrySet()) {
            long w = e.getValue() instanceof Number ? ((Number) e.getValue()).longValue() : 0;
            total += w;
            Object vote = votes.get(e.getKey());
            if (Boolean.TRUE.equals(vote)) yes += w; else if (Boolean.FALSE.equals(vote)) no += w;
        }
        long by = num(d, "decidesBy");
        boolean silencePassed = by > 0 && System.currentTimeMillis() >= by && no == 0;
        card.addView(CircleUi.text(c.activity, "A vote to close this group and return the money is open.", 12.5f, R.color.colorTextSecondary, false));
        card.addView(CircleUi.row(c.activity, "Yes", pct(yes, total)));
        card.addView(CircleUi.row(c.activity, "No", pct(no, total)));
        if (by > 0) card.addView(CircleUi.row(c.activity, "Passes on", new SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()).format(new Date(by))
                + (no == 0 ? "" : " (only if nobody says no)")));
        if (weights.containsKey(c.me)) {
            Object mine = votes.get(c.me);
            if (mine != null) card.addView(CircleUi.text(c.activity, "You voted " + (Boolean.TRUE.equals(mine) ? "yes" : "no") + ". You can change it.",
                    12, R.color.colorTextSecondary, false));
            card.addView(yesNo(c, y -> NodeGroupClient.voteFixedDissolve(c.activity, c.me, c.nodeId, y,
                    NodePayActions.result(c, y ? "Your yes vote is in" : "Your no vote is in")), "Return everyone's money"));
        }
        if (silencePassed || yes * 2 > total) {
            card.addView(NodePayActions.button(c.activity, "Return the money now", true, v -> {
                c.pressed = (Button) v;
                NodePayActions.pay(c, "Return everyone's money", "Group balance",
                        NodePayActions.moneyRows(c, "Each member's wallet", "The vote has passed. Refunds go out now."),
                        () -> NodeGroupClient.finishFixedDissolve(c.activity, c.me, c.nodeId, NodePayActions.result(c, "Money is on its way back to every member.")));
            }));
        }
        box.addView(header(c.activity, "Vote to close the group"));
        box.addView(card);
    }

    // ── finished goals ─────────────────────────────────────────────────────

    private static void pastGoals(NodePayActions.Ctx c, LinearLayout box) {
        Object gl = c.group == null ? null : c.group.get("goals");
        if (!(gl instanceof List) || ((List<?>) gl).isEmpty()) return;
        List<?> past = (List<?>) gl;
        box.addView(header(c.activity, "Finished goals"));
        LinearLayout card = CircleUi.card(c.activity);
        for (int i = past.size() - 1; i >= 0; i--) {
            if (!(past.get(i) instanceof Map)) continue;
            @SuppressWarnings("unchecked") Map<String, Object> g = (Map<String, Object>) past.get(i);
            String name = str(g, "name");
            String title = "Goal " + num(g, "number") + (name.isEmpty() ? "" : " · " + name);
            card.addView(CircleUi.row(c.activity, title, NodePayActions.gm(c, num(g, "raisedRaw")) + " of " + NodePayActions.gm(c, num(g, "targetRaw"))
                    + (Boolean.TRUE.equals(g.get("paidOut")) ? " · paid out" : " · kept")));
        }
        box.addView(card);
    }

    // ── small builders ─────────────────────────────────────────────────────

    interface YesNo { void vote(boolean yes); }

    private static LinearLayout yesNo(NodePayActions.Ctx c, YesNo cb, String label) {
        LinearLayout row = new LinearLayout(c.activity);
        row.setGravity(Gravity.CENTER_VERTICAL);
        for (boolean yes : new boolean[]{true, false}) {
            Button b = NodePayActions.button(c.activity, yes ? "Vote yes" : "Vote no", yes, v -> {
                java.util.ArrayList<String[]> rows = new java.util.ArrayList<>();
                if (!c.groupName.isEmpty()) rows.add(new String[]{"Group", c.groupName});
                rows.add(new String[]{"Vote on", label});
                rows.add(new String[]{"Your vote", yes ? "Yes" : "No"});
                rows.add(new String[]{"tFare", "Taken from your wallet when this is sent"});
                c.pressed = (Button) v;
                NodePayActions.pay(c, yes ? "Vote yes" : "Vote no", "", rows, () -> cb.vote(yes));
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, CircleUi.dp(c.activity, 44), 1f);
            lp.topMargin = CircleUi.dp(c.activity, 10);
            if (yes) lp.rightMargin = CircleUi.dp(c.activity, 6); else lp.leftMargin = CircleUi.dp(c.activity, 6);
            row.addView(b, lp);
        }
        return row;
    }

    private static TextView header(Activity a, String s) {
        TextView t = CircleUi.text(a, s, 12, R.color.colorTextPrimary, true);
        t.setPadding(0, CircleUi.dp(a, 16), 0, CircleUi.dp(a, 6));
        return t;
    }

    private static String pct(long part, long total) { return total <= 0 ? "0%" : (part * 100 / total) + "% of the money"; }

    private static String str(Map<String, Object> m, String k) {
        Object o = m == null ? null : m.get(k);
        return o == null ? "" : String.valueOf(o);
    }

    private static long num(Map<String, Object> m, String k) {
        Object o = m == null ? null : m.get(k);
        return o instanceof Number ? ((Number) o).longValue() : 0L;
    }
}
