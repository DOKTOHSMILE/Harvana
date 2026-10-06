package com.harvana.app.group;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.harvana.app.R;
import com.harvana.app.circle.CircleUi;
import com.harvana.app.engine.GroupAction;
import com.harvana.app.sync.NodeGroupClient;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Map;

/** The payment buttons on the group Payments tab. Same signed calls as the old full-screen group page. */
public final class NodePayActions {
    public static final class Ctx {
        public Activity activity;
        public String nodeId = "", fsGroupId = "", me = "", groupName = "";
        public Map<String, Object> group, member, line;
        public Runnable reload;
        public Button pressed;
    }

    private NodePayActions() {}

    public static void run(Ctx c, GroupAction a) {
        if (c == null || c.activity == null || a == null || !a.enabled) return;
        switch (a.id) {
            case GroupAction.JOIN: confirmJoin(c); break;
            case GroupAction.CONTRIBUTE:
                pay(c, "Round contribution", CircleUi.harv(num(c.group, "contributionAmountRaw")),
                        moneyRows(c, escrow(c), "Your payment for this round"),
                        () -> NodeGroupClient.contributeCircle(c.activity, c.me, c.nodeId, result(c, "Payment sent")));
                break;
            case GroupAction.WITHDRAW_BOND:
                pay(c, "Withdraw bond", CircleUi.harv(bondOf(c)),
                        moneyRows(c, "Your wallet", "Returned only if this group allows it"),
                        () -> NodeGroupClient.withdrawCircleBond(c.activity, c.me, c.nodeId, result(c, "Bond withdrawal requested")));
                break;
            case GroupAction.START_LINE: startLine(c); break;
            case GroupAction.ACCUM_CONTRIBUTE:
                pay(c, "Save today", CircleUi.harv(c.line == null ? 0 : num(c.line, "dailyAmountRaw")),
                        moneyRows(c, escrow(c), "One day on your savings line"),
                        () -> NodeGroupClient.contributeAccum(c.activity, c.me, c.nodeId, a.ref, result(c, "Saved")));
                break;
            case GroupAction.ACCUM_CONTRIBUTE_MULTI: saveMore(c, a.ref); break;
            case GroupAction.ACCUM_PACK:
                pay(c, "Request withdrawal", CircleUi.harv(c.line == null ? 0 : num(c.line, "amountPaidRaw")),
                        moneyRows(c, "Your wallet", "The admin still has to approve this"),
                        () -> NodeGroupClient.packAccumLine(c.activity, c.me, c.nodeId, a.ref, announced(c, "Withdrawal requested. Waiting for the admin.",
                                GroupProposalIndex.PACK, a.ref, "Withdrawal request", "")));
                break;
            case GroupAction.ACCUM_CLAIM:
                pay(c, "Claim savings", CircleUi.harv(c.line == null ? 0 : num(c.line, "amountPaidRaw")),
                        moneyRows(c, "Your wallet", "Pays your savings line out to you"),
                        () -> NodeGroupClient.claimAccumLine(c.activity, c.me, c.nodeId, a.ref, result(c, "Savings claimed")));
                break;
            case GroupAction.ACCUM_APPROVE_PACK:
                pay(c, "Approve withdrawal", a.label, moneyRows(c, "Member wallet", "Sends this member's savings out of the group"),
                        () -> NodeGroupClient.approveAccumPack(c.activity, c.me, c.nodeId, a.ref, result(c, "Approved")));
                break;
            case GroupAction.FIXED_DEPOSIT:
                amount(c, "Deposit", "Amount to deposit", raw ->
                        pay(c, "Deposit", CircleUi.harv(raw), moneyRows(c, escrow(c), "Added to this goal group"),
                                () -> NodeGroupClient.depositFixed(c.activity, c.me, c.nodeId, raw, result(c, "Deposit sent"))));
                break;
            case GroupAction.FIXED_REQUEST_DISBURSEMENT:
                pay(c, "Request payout", "Group balance", moneyRows(c, escrow(c), "The group votes before anyone is paid"),
                        () -> NodeGroupClient.requestFixedDisbursement(c.activity, c.me, c.nodeId, requestThenVote(c, "requestId", false, "Payout requested")));
                break;
            case GroupAction.FIXED_REQUEST_MILESTONE:
                reason(c, "Request a milestone release", "What will the money be used for?", (raw, text) ->
                        pay(c, "Request milestone", CircleUi.harv(raw), moneyRows(c, escrow(c), text),
                                () -> NodeGroupClient.requestFixedMilestone(c.activity, c.me, c.nodeId, raw, text, requestThenVote(c, "milestoneId", true, "Milestone requested"))));
                break;
            case GroupAction.FIXED_REQUEST_GRANT:
                reason(c, "Ask for an emergency grant", "Why do you need help?", (raw, text) ->
                        pay(c, "Request emergency grant", CircleUi.harv(raw), moneyRows(c, "Your wallet", text),
                                () -> NodeGroupClient.requestFixedGrant(c.activity, c.me, c.nodeId, raw, text, announced(c, "Grant requested. The group will vote.",
                                GroupProposalIndex.GRANT, "", "Emergency grant request", "grantId"))));
                break;
            default: break;
        }
    }

    public static Button button(Activity a, String label, boolean primary, android.view.View.OnClickListener l) {
        Button bt = com.harvana.app.ui.Btn.of(a, label, primary, l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, CircleUi.dp(a, 52));
        lp.topMargin = CircleUi.dp(a, 12);
        bt.setLayoutParams(lp);
        return bt;
    }

    public static void copyId(Activity a, String id) {
        ClipboardManager cm = (ClipboardManager) a.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("Group", id));
        Toast.makeText(a, "Copied", Toast.LENGTH_SHORT).show();
    }

    private static void confirmJoin(Ctx c) {
        long bond = num(c.group, "requiredBondRaw"), contribution = num(c.group, "contributionAmountRaw");
        ArrayList<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Bond locked now", CircleUi.harv(bond)});
        rows.add(new String[]{"Each round after that", CircleUi.harv(contribution)});
        rows.add(new String[]{"To", escrow(c)});
        rows.add(new String[]{"Bond comes back", "When the group finishes cleanly. Missed payments are taken from it."});
        pay(c, "Join this group", CircleUi.harv(bond), rows, () ->
                NodeGroupClient.joinCircle(c.activity, c.me, c.nodeId, str(c.group, "termsHash"), result(c, "You joined")));
    }

    private static void startLine(Ctx c) {
        LinearLayout box = new LinearLayout(c.activity);
        box.setOrientation(LinearLayout.VERTICAL);
        int p = CircleUi.dp(c.activity, 16);
        box.setPadding(p, p, p, 0);
        EditText daily = new com.google.android.material.textfield.TextInputEditText(c.activity);
        daily.setHint("Amount to save each day");
        daily.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        box.addView(daily);
        EditText lockDays = new com.google.android.material.textfield.TextInputEditText(c.activity);
        lockDays.setHint("Lock for how many days? Leave empty for a flexible line");
        lockDays.setInputType(InputType.TYPE_CLASS_NUMBER);
        box.addView(lockDays);
        new com.harvana.app.ui.SheetDialog.Builder(c.activity).setTitle("Start my savings line")
                .setMessage("A flexible line can be withdrawn after the admin approves. A locked line cannot be touched before its date.")
                .setView(box).setNegativeButton("Cancel", null)
                .setPositiveButton("Start", (d, w) -> {
                    long raw = CircleUi.toRaw(daily.getText().toString());
                    if (raw <= 0) { toast(c, "Enter a daily amount."); return; }
                    String days = lockDays.getText().toString().trim();
                    Long until = days.isEmpty() ? null : System.currentTimeMillis() + Long.parseLong(days) * 86_400_000L;
                    pay(c, "Start a savings line", CircleUi.harv(raw),
                            moneyRows(c, "Your savings line", until == null
                                    ? "Flexible. Opening the line does not pay today."
                                    : "Locked until the date you chose. Opening it does not pay today."),
                            () -> NodeGroupClient.startAccumLine(c.activity, c.me, c.nodeId, raw, false,
                                    until == null ? "FLEXIBLE" : "LOCKED", until, "ONGOING", null, result(c, "Savings line started")));
                }).show();
    }

    private static void saveMore(Ctx c, String lineId) {
        boolean ongoing = c.line == null || "ONGOING".equals(str(c.line, "depositMode")) || str(c.line, "depositMode").isEmpty();
        long dailyRaw = c.line == null ? 0 : num(c.line, "dailyAmountRaw");
        if (ongoing && dailyRaw > 0) {
            EditText days = new com.google.android.material.textfield.TextInputEditText(c.activity);
            days.setHint("How many days to save now?");
            days.setInputType(InputType.TYPE_CLASS_NUMBER);
            new com.harvana.app.ui.SheetDialog.Builder(c.activity).setTitle("Save several days").setView(days).setNegativeButton("Cancel", null)
                    .setPositiveButton("Save", (d, w) -> {
                        int n = 0;
                        try { n = Integer.parseInt(days.getText().toString().trim()); } catch (Exception ignored) { }
                        if (n < 2) { toast(c, "Enter 2 or more days."); return; }
                        long total = dailyRaw * n;
                        pay(c, "Save several days", CircleUi.harv(total),
                                moneyRows(c, escrow(c), n + " days at " + CircleUi.harv(dailyRaw) + " each"),
                                () -> NodeGroupClient.contributeAccumMultiple(c.activity, c.me, c.nodeId, lineId, total, result(c, "Saved")));
                    }).show();
        } else {
            amount(c, "Add to my line", "Amount", raw -> pay(c, "Add to my line", CircleUi.harv(raw),
                    moneyRows(c, escrow(c), "Extra saving on this line"),
                    () -> NodeGroupClient.contributeAccumVariable(c.activity, c.me, c.nodeId, lineId, raw, result(c, "Saved"))));
        }
    }

    private static void amount(Ctx c, String title, String hint, RawCb cb) {
        EditText e = new com.google.android.material.textfield.TextInputEditText(c.activity);
        e.setHint(hint);
        e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        new com.harvana.app.ui.SheetDialog.Builder(c.activity).setTitle(title).setView(e).setNegativeButton("Cancel", null)
                .setPositiveButton("Continue", (d, w) -> {
                    long raw = CircleUi.toRaw(e.getText().toString());
                    if (raw <= 0) { toast(c, "Enter an amount."); return; }
                    cb.got(raw);
                }).show();
    }

    private interface RawCb { void got(long raw); }
    private interface RawText { void got(long raw, String text); }

    private static void reason(Ctx c, String title, String textHint, RawText cb) {
        LinearLayout box = new LinearLayout(c.activity);
        box.setOrientation(LinearLayout.VERTICAL);
        int p = CircleUi.dp(c.activity, 16);
        box.setPadding(p, p, p, 0);
        EditText amount = new com.google.android.material.textfield.TextInputEditText(c.activity);
        amount.setHint("Amount");
        amount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        box.addView(amount);
        EditText text = new com.google.android.material.textfield.TextInputEditText(c.activity);
        text.setHint(textHint);
        box.addView(text);
        new com.harvana.app.ui.SheetDialog.Builder(c.activity).setTitle(title).setView(box).setNegativeButton("Cancel", null)
                .setPositiveButton("Send", (d, w) -> {
                    long raw = CircleUi.toRaw(amount.getText().toString());
                    String t = text.getText().toString().trim();
                    if (raw <= 0 || t.length() < 5) { toast(c, "Enter an amount and a short explanation."); return; }
                    cb.got(raw, t);
                }).show();
    }

    /** Pays back the picked missed days of the member's own savings line: review, PIN, then the signed back-pay. */
    public static void backpay(Ctx c, java.util.List<String> dates) {
        if (c.line == null || dates == null || dates.isEmpty()) return;
        long dailyRaw = num(c.line, "dailyAmountRaw");
        long total = dailyRaw * dates.size();
        String lineId = str(c.line, "lineId");
        pay(c, "Pay back missed days", CircleUi.harv(total),
                moneyRows(c, escrow(c), dates.size() + (dates.size() == 1 ? " missed day" : " missed days") + " at " + CircleUi.harv(dailyRaw) + " each"),
                () -> NodeGroupClient.backpayAccum(c.activity, c.me, c.nodeId, lineId, dates, result(c, "Paid back. It shows once the network confirms it.")));
    }

    private static void pay(Ctx c, String title, String amount, java.util.List<String[]> rows, Runnable work) {
        Button source = c.pressed;
        c.pressed = null;
        PaymentConfirmSheet.reviewThenAuthorize(c.activity, title, amount, rows, () -> {
            if (source != null) { source.setEnabled(false); source.setText("Working…"); }
            work.run();
        });
    }

    /**
     * The admin who asks for a payout or a milestone approves it by asking, so their yes vote is cast straight after the
     * request. The node's quorum counts yes votes, and without this a two-person group sat unfinished after the other member
     * said yes, because the admin had not voted on their own request. Passing still needs a yes from someone else.
     */
    private static NodeGroupClient.Callback requestThenVote(Ctx c, String idField, boolean milestone, String ok) {
        return new NodeGroupClient.Callback() {
            @Override public void onSuccess(JSONObject json) {
                String id = json == null ? "" : json.optString(idField, "");
                if (!id.isEmpty()) GroupProposalIndex.post(c.activity, c.fsGroupId, milestone ? GroupProposalIndex.MILESTONE : GroupProposalIndex.DISBURSEMENT,
                        id, milestone ? "Milestone request" : "Payout request", c.me);   // tells every member a vote is waiting
                if (id.isEmpty()) { result(c, ok).onSuccess(json); return; }
                NodeGroupClient.Callback voted = new NodeGroupClient.Callback() {
                    @Override public void onSuccess(JSONObject v) { result(c, ok + ". Your yes vote is in. The members vote next.").onSuccess(v); }
                    @Override public void onError(String e) { result(c, ok + ", but your own yes vote did not go through. Vote from Actions & votes.").onSuccess(null); }
                };
                if (milestone) NodeGroupClient.voteFixedMilestone(c.activity, c.me, c.nodeId, id, true, voted);
                else NodeGroupClient.voteFixedDisbursement(c.activity, c.me, c.nodeId, id, true, voted);
            }
            @Override public void onError(String error) { result(c, ok).onError(error); }
        };
    }

    /** Like result(), and also adds the request to the group's notices so every member is told a vote is waiting. */
    private static NodeGroupClient.Callback announced(Ctx c, String ok, String kind, String knownId, String title, String idField) {
        return new NodeGroupClient.Callback() {
            @Override public void onSuccess(JSONObject json) {
                String id = knownId.isEmpty() && json != null && !idField.isEmpty() ? json.optString(idField, "") : knownId;
                if (!id.isEmpty()) GroupProposalIndex.post(c.activity, c.fsGroupId, kind, id, title, c.me);
                result(c, ok).onSuccess(json);
            }
            @Override public void onError(String error) { result(c, ok).onError(error); }
        };
    }

    private static NodeGroupClient.Callback result(Ctx c, String ok) {
        return new NodeGroupClient.Callback() {
            @Override public void onSuccess(JSONObject json) {
                com.harvana.app.trust.AppActivity.note(c.activity, "GROUP", ok);
                toast(c, ok);
                if (c.reload != null) c.activity.runOnUiThread(c.reload);
            }
            @Override public void onError(String error) {
                toast(c, CircleUi.friendly(error));
                if (c.reload != null) c.activity.runOnUiThread(c.reload);
            }
        };
    }

    private static ArrayList<String[]> moneyRows(Ctx c, String to, String detail) {
        ArrayList<String[]> rows = new ArrayList<>();
        if (c.groupName != null && !c.groupName.isEmpty()) rows.add(new String[]{"Group", c.groupName});
        rows.add(new String[]{"To", to == null || to.isEmpty() ? escrow(c) : to});
        if (detail != null && !detail.isEmpty()) rows.add(new String[]{"For", detail});
        rows.add(new String[]{"From", "Your wallet"});
        rows.add(new String[]{"Network fee", "Taken from your wallet when this is sent"});
        return rows;
    }

    private static String escrow(Ctx c) {
        String e = str(c.group, "escrowAddress");
        return e.isEmpty() ? "This group's network wallet" : e;
    }

    private static long bondOf(Ctx c) {
        long bond = num(c.member, "bondRaw");
        if (bond <= 0) bond = num(c.member, "bondLockedRaw");
        if (bond <= 0) bond = num(c.group, "requiredBondRaw");
        return bond;
    }

    private static void toast(Ctx c, String m) {
        if (c.activity != null) c.activity.runOnUiThread(() -> Toast.makeText(c.activity, m, Toast.LENGTH_LONG).show());
    }

    private static String str(Map<String, Object> m, String k) {
        Object o = m == null ? null : m.get(k);
        return o == null ? "" : String.valueOf(o);
    }

    private static long num(Map<String, Object> m, String k) {
        Object o = m == null ? null : m.get(k);
        return o instanceof Number ? ((Number) o).longValue() : 0L;
    }
}
