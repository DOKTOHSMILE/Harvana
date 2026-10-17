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

    /** The group's coin ("asset" on the node record); empty for a group made before coins (HARV). */
    static String coinOf(Ctx c) { return com.harvana.app.engine.Amounts.assetOf(c.group); }

    /** An amount of this group, in its coin. */
    static String gm(Ctx c, long raw) { return com.harvana.app.engine.Amounts.money(raw, coinOf(c)); }

    /** An amount typed for this group, in its coin's smallest units. */
    static long typed(Ctx c, String text) { return coinOf(c).isEmpty() ? CircleUi.toRaw(text) : coinRaw(text); }

    public static void run(Ctx c, GroupAction a) {
        if (c == null || c.activity == null || a == null || !a.enabled) return;
        switch (a.id) {
            case GroupAction.JOIN: confirmJoin(c); break;
            case GroupAction.CONTRIBUTE:
                pay(c, "Round contribution", gm(c, num(c.group, "contributionAmountRaw")),
                        moneyRows(c, escrow(c), "Your payment for this round"),
                        () -> NodeGroupClient.contributeCircle(c.activity, c.me, c.nodeId, result(c, "Payment sent")));
                break;
            case GroupAction.WITHDRAW_BOND:
                choosePayoutWallet(c, to -> pay(c, "Withdraw bond", gm(c, bondOf(c)),
                        moneyRows(c, shortWallet(to), "Returned only if this group allows it"),
                        () -> thenPayTo(c, to, () -> NodeGroupClient.withdrawCircleBond(c.activity, c.me, c.nodeId, result(c, "Bond withdrawal requested")))));
                break;
            case GroupAction.START_LINE: startLine(c); break;
            case GroupAction.ACCUM_CONTRIBUTE:
                pay(c, "Save today", lineMoney(c, c.line == null ? 0 : num(c.line, "dailyAmountRaw")),
                        moneyRows(c, escrow(c), "One day on your savings line"),
                        () -> NodeGroupClient.contributeAccum(c.activity, c.me, c.nodeId, a.ref, result(c, "Saved")));
                break;
            case GroupAction.ACCUM_CONTRIBUTE_MULTI: saveMore(c, a.ref); break;
            case GroupAction.ACCUM_PACK:
                pay(c, "Request withdrawal", lineMoney(c, c.line == null ? 0 : num(c.line, "amountPaidRaw")),
                        moneyRows(c, "Your wallet", "The admin still has to approve this"),
                        () -> NodeGroupClient.packAccumLine(c.activity, c.me, c.nodeId, a.ref, announced(c, "Withdrawal requested. Waiting for the admin.",
                                GroupProposalIndex.PACK, a.ref, "Withdrawal request", "")));
                break;
            case GroupAction.ACCUM_CLAIM:
                choosePayoutWallet(c, to -> pay(c, "Claim savings", lineMoney(c, c.line == null ? 0 : num(c.line, "amountPaidRaw")),
                        moneyRows(c, shortWallet(to), "Pays your savings line out to you"),
                        () -> thenPayTo(c, to, () -> NodeGroupClient.claimAccumLine(c.activity, c.me, c.nodeId, a.ref, result(c, "Savings claimed")))));
                break;
            case GroupAction.ACCUM_APPROVE_PACK:
                pay(c, "Approve withdrawal", a.label, moneyRows(c, "Member wallet", "Sends this member's savings out of the group"),
                        () -> NodeGroupClient.approveAccumPack(c.activity, c.me, c.nodeId, a.ref, result(c, "Approved")));
                break;
            case GroupAction.ACCUM_WALLET_RECOVERY:
                // Member-initiated payout-address change. Signs
                // HARVANA_ACCUM_WALLET_RECOVERY_V1:groupId:oldMember:newMember:acting:nonce
                // where "acting" is the current wallet and "oldMember" is the address
                // currently on the node for this member. The node replaces the
                // payout address; future claims / packs go to the new one.
                promptWalletRecovery(c, a.ref);
                break;
            case GroupAction.FIXED_DEPOSIT:
                amount(c, "Deposit", "Amount to deposit", raw ->
                        pay(c, "Deposit", gm(c, raw), moneyRows(c, escrow(c), "Added to this goal group"),
                                () -> NodeGroupClient.depositFixed(c.activity, c.me, c.nodeId, raw, result(c, "Deposit sent"))));
                break;
            case GroupAction.FIXED_REQUEST_DISBURSEMENT:
                pay(c, "Request payout", "Group balance", moneyRows(c, escrow(c), "The group votes before anyone is paid"),
                        () -> NodeGroupClient.requestFixedDisbursement(c.activity, c.me, c.nodeId, requestThenVote(c, "requestId", false, "Payout requested")));
                break;
            case GroupAction.FIXED_REQUEST_MILESTONE:
                reason(c, "Request a milestone release", "What will the money be used for?", (raw, text) ->
                        pay(c, "Request milestone", gm(c, raw), moneyRows(c, escrow(c), text),
                                () -> NodeGroupClient.requestFixedMilestone(c.activity, c.me, c.nodeId, raw, text, requestThenVote(c, "milestoneId", true, "Milestone requested"))));
                break;
            case GroupAction.FIXED_REQUEST_GRANT:
                reason(c, "Ask for an emergency grant", "Why do you need help?", (raw, text) ->
                        pay(c, "Request emergency grant", gm(c, raw), moneyRows(c, "Your wallet", text),
                                () -> NodeGroupClient.requestFixedGrant(c.activity, c.me, c.nodeId, raw, text, announced(c, "Grant requested. The group will vote.",
                                GroupProposalIndex.GRANT, "", "Emergency grant request", "grantId"))));
                break;
            default: break;
        }
    }

    /**
     * Accum wallet recovery: member-initiated payout-address change.
     * Opens a sheet that asks for the new HV0x / 0x address, validates it, then
     * signs {@code HARVANA_ACCUM_WALLET_RECOVERY_V1:groupId:oldMember:newMember:acting:nonce}
     * via the new {@code chain.NodeGroupClient.accumWalletRecovery} (the canonical
     * helper is in {@code GroupCanonical.accumWalletRecovery}).
     */
    private static void promptWalletRecovery(Ctx c, String lineId) {
        if (c == null || c.activity == null) return;
        final EditText input = new EditText(c.activity);
        input.setHint("HV0x… (full address)");
        input.setSingleLine(true);
        input.setTypeface(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.NORMAL);
        new com.harvana.app.ui.SheetDialog.Builder(c.activity)
                .setTitle("Change my payout wallet")
                .setMessage("The new address receives all future claim / pack payouts for this line.\n\nCurrent: " + shortWallet(c.me))
                .setView(input)
                .setPositiveButton("Submit", (x, w) -> {
                    String newAddr = input.getText().toString().trim();
                    if (!com.harvana.app.group.WalletRecoverySheet.isPlausibleAddress(newAddr)) {
                        Toast.makeText(c.activity, "Not a valid HV0x / 0x address.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // Sign and submit via the new chain.NodeGroupClient.
                    // accumWalletRecovery(ctx, actingAddress, groupId, oldMember, newMember, callback)
                    // — acting is the current wallet, oldMember is the current wallet
                    // (we are switching our own address), newMember is the new address.
                    com.harvana.app.chain.NodeGroupClient.accumWalletRecovery(
                            c.activity, c.me, c.nodeId, c.me, newAddr,
                            new com.harvana.app.chain.NodeGroupClient.SignCallback() {
                                @Override public void onSuccess(org.json.JSONObject resp) {
                                    Toast.makeText(c.activity,
                                            "Payout wallet changed. Future claims go to " + shortWallet(newAddr) + ".",
                                            Toast.LENGTH_SHORT).show();
                                    if (c.reload != null) c.reload.run();
                                }
                                @Override public void onError(String error) {
                                    Toast.makeText(c.activity, error, Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
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
        rows.add(new String[]{"Bond locked now", gm(c, bond)});
        rows.add(new String[]{"Each round after that", gm(c, contribution)});
        rows.add(new String[]{"To", escrow(c)});
        rows.add(new String[]{"Bond comes back", "When the group finishes cleanly. Missed payments are taken from it."});
        pay(c, "Join this group", gm(c, bond), rows, () ->
                NodeGroupClient.joinCircle(c.activity, c.me, c.nodeId, str(c.group, "termsHash"), result(c, "You joined")));
    }

    /**
     * Start a savings line. "Save in" chooses the coin: HARV (its price moves; for members who accept that) or a stable
     * coin the network has switched on (hNGN, hUSD, …), which keeps its value for members who want low risk.
     */
    private static void startLine(Ctx c) {
        LinearLayout box = new LinearLayout(c.activity);
        box.setOrientation(LinearLayout.VERTICAL);
        int p = CircleUi.dp(c.activity, 16);
        box.setPadding(p, p, p, 0);
        final String[] asset = {coinOf(c)};                            // the group's coin; "" = HARV (older groups)
        final java.util.List<String> codes = new java.util.ArrayList<>();
        codes.add("");
        com.google.android.material.button.MaterialButton coin = new com.google.android.material.button.MaterialButton(
                c.activity, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        coin.setAllCaps(false);
        coin.setText("Save in: HARV (price can move)");
        coin.setOnClickListener(v -> {
            String[] names = new String[codes.size()];
            for (int i = 0; i < names.length; i++) names[i] = codes.get(i).isEmpty() ? "HARV (price can move)"
                    : com.harvana.app.husd.Stable.symbol(codes.get(i)) + " (keeps its value)";
            com.harvana.app.p2p.ThemedDialogs.show(com.harvana.app.p2p.ThemedDialogs.alert(c.activity).setTitle("Save in")
                    .setItems(names, (dd, which) -> { asset[0] = codes.get(which); coin.setText("Save in: " + names[which]); }));
        });
        if (asset[0].isEmpty()) box.addView(coin);                     // a group with a coin saves only in it
        if (asset[0].isEmpty()) com.harvana.app.husd.HusdClient.list(new com.harvana.app.husd.HusdClient.Cb() {
            @Override public void ok(org.json.JSONObject r) {
                org.json.JSONArray arr = r.optJSONArray("assets");
                if (arr != null) for (int i = 0; i < arr.length(); i++) {
                    org.json.JSONObject o = arr.optJSONObject(i);
                    if (o != null && o.optBoolean("active") && com.harvana.app.husd.Stable.isStable(o.optString("asset"))) codes.add(o.optString("asset"));
                }
            }
            @Override public void fail(String e) { }
        });
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
                    String code = asset[0];
                    long raw = code.isEmpty() ? typed(c, daily.getText().toString()) : coinRaw(daily.getText().toString());
                    if (raw <= 0) { toast(c, "Enter a daily amount."); return; }
                    String days = lockDays.getText().toString().trim();
                    Long until = days.isEmpty() ? null : System.currentTimeMillis() + Long.parseLong(days) * 86_400_000L;
                    pay(c, "Start a savings line", com.harvana.app.engine.Amounts.money(raw, code),
                            moneyRows(c, "Your savings line", (code.isEmpty() ? "Saved in HARV. " : "Saved in " + com.harvana.app.husd.Stable.symbol(code) + ", which keeps its value. ")
                                    + (until == null ? "Flexible. Opening the line does not pay today."
                                    : "Locked until the date you chose. Opening it does not pay today.")),
                            () -> NodeGroupClient.startAccumLine(c.activity, c.me, c.nodeId, raw, false,
                                    until == null ? "FLEXIBLE" : "LOCKED", until, "ONGOING", null, code.isEmpty() ? null : code,
                                    result(c, "Savings line started")));
                }).show();
    }

    /** A stable coin amount typed by the person, in the coin's smallest units (6 decimals). */
    public static long coinRaw(String text) {
        try { return new java.math.BigDecimal(text.trim().replace(",", "")).movePointRight(6).longValue(); } catch (Exception e) { return 0; }
    }

    /** An amount of the context's savings line, in its coin. */
    static String lineMoney(Ctx c, long raw) {
        return com.harvana.app.engine.Amounts.money(raw, com.harvana.app.engine.Amounts.assetOf(c.line));
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
                        pay(c, "Save several days", lineMoney(c, total),
                                moneyRows(c, escrow(c), n + " days at " + lineMoney(c, dailyRaw) + " each"),
                                () -> NodeGroupClient.contributeAccumMultiple(c.activity, c.me, c.nodeId, lineId, total, result(c, "Saved")));
                    }).show();
        } else {
            amount(c, "Add to my line", "Amount", typed -> {
                // The amount box reads 8 decimals (HARV); a stable coin line counts in 6.
                long raw = com.harvana.app.engine.Amounts.assetOf(c.line).isEmpty() ? typed : typed / 100;
                pay(c, "Add to my line", lineMoney(c, raw),
                    moneyRows(c, escrow(c), "Extra saving on this line"),
                    () -> NodeGroupClient.contributeAccumVariable(c.activity, c.me, c.nodeId, lineId, raw, result(c, "Saved")));
            });
        }
    }

    static void amount(Ctx c, String title, String hint, RawCb cb) {
        EditText e = new com.google.android.material.textfield.TextInputEditText(c.activity);
        e.setHint(hint);
        e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        new com.harvana.app.ui.SheetDialog.Builder(c.activity).setTitle(title).setView(e).setNegativeButton("Cancel", null)
                .setPositiveButton("Continue", (d, w) -> {
                    long raw = typed(c, e.getText().toString());
                    if (raw <= 0) { toast(c, "Enter an amount."); return; }
                    cb.got(raw);
                }).show();
    }

    interface RawCb { void got(long raw); }
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
                    long raw = typed(c, amount.getText().toString());
                    String t = text.getText().toString().trim();
                    if (raw <= 0 || t.length() < 5) { toast(c, "Enter an amount and a short explanation."); return; }
                    cb.got(raw, t);
                }).show();
    }

    /** Pays the picked days of the member's own savings line (past, today or ahead): review, PIN, then the signed request. */
    public static void backpay(Ctx c, java.util.List<String> dates) {
        if (c.line == null || dates == null || dates.isEmpty()) return;
        long dailyRaw = num(c.line, "dailyAmountRaw");
        long total = dailyRaw * dates.size();
        String lineId = str(c.line, "lineId");
        // The dates are the earliest unpaid days of the line, which are the ones the node pays (days go in order).
        pay(c, dates.size() == 1 ? "Pay " + dates.get(0) : "Pay " + dates.size() + " days", lineMoney(c, total),
                moneyRows(c, escrow(c), android.text.TextUtils.join(", ", dates) + " · " + lineMoney(c, dailyRaw) + " a day"),
                () -> NodeGroupClient.backpayAccum(c.activity, c.me, c.nodeId, lineId, dates, result(c, "Paid back. It shows once the network confirms it.")));
    }

    interface WalletChoice { void chosen(String wallet); }

    /**
     * Where the group sends the member's money: they pick one of their own wallets (the ones on their account). With one
     * wallet there is nothing to pick. The network accepts only the member's own wallets.
     */
    static void choosePayoutWallet(Ctx c, WalletChoice then) {
        com.harvana.app.sync.NodeIdentityClient.myWallets(new com.harvana.app.sync.NodeIdentityClient.Callback() {
            @Override public void onSuccess(JSONObject j) {
                org.json.JSONArray ws = j == null ? null : j.optJSONArray("wallets");
                java.util.List<String> mine = new java.util.ArrayList<>();
                for (int i = 0; ws != null && i < ws.length(); i++) {
                    JSONObject w = ws.optJSONObject(i);
                    String a = w == null ? "" : w.optString("address", "");
                    if (!a.isEmpty() && !mine.contains(a)) mine.add(a);
                }
                c.activity.runOnUiThread(() -> pickFrom(c, mine, then));
            }
            @Override public void onError(String e) { c.activity.runOnUiThread(() -> then.chosen(c.me)); }   // older network: the wallet in use
        });
    }

    private static void pickFrom(Ctx c, java.util.List<String> mine, WalletChoice then) {
        if (c.activity.isFinishing()) return;
        if (mine.size() <= 1) { then.chosen(c.me); return; }
        String[] labels = new String[mine.size()];
        int sel = 0;
        for (int i = 0; i < labels.length; i++) {
            String a = mine.get(i);
            String name = "";
            for (com.harvana.app.wallet.WalletListManager.WalletEntry e : com.harvana.app.wallet.WalletListManager.getWalletList(c.activity)) {
                if (e != null && a.equalsIgnoreCase(e.address) && e.label != null) name = e.label;
            }
            labels[i] = (name.isEmpty() ? "" : name + " · ") + shortWallet(a) + (a.equalsIgnoreCase(c.me) ? " · in use" : "");
            if (a.equalsIgnoreCase(c.me)) sel = i;
        }
        final int[] picked = {sel};
        new com.harvana.app.ui.SheetDialog.Builder(c.activity).setTitle("Pay to")
                .setSingleChoiceItems(labels, sel, (d, which) -> picked[0] = which)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Continue", (d, w) -> then.chosen(mine.get(picked[0])))
                .show();
    }

    /** Records the chosen wallet with the group (when it is not already where the group pays), then does the work. */
    static void thenPayTo(Ctx c, String to, Runnable work) {
        NodeGroupClient.getPayoutWallet(c.nodeId, c.me, new NodeGroupClient.Callback() {
            @Override public void onSuccess(JSONObject now) {
                if (to.equalsIgnoreCase(now == null ? "" : now.optString("payoutWallet", ""))) { work.run(); return; }
                set();
            }
            @Override public void onError(String e) {
                if (to.equalsIgnoreCase(c.me)) work.run(); else set();
            }
            private void set() {
                NodeGroupClient.setPayoutWallet(c.activity, c.me, c.nodeId, to, new NodeGroupClient.Callback() {
                    @Override public void onSuccess(JSONObject json) { work.run(); }
                    @Override public void onError(String e) { result(c, "").onError(e); }
                });
            }
        });
    }

    static String shortWallet(String a) {
        return a == null || a.length() <= 14 ? String.valueOf(a) : a.substring(0, 8) + "\u2026" + a.substring(a.length() - 4);
    }

    static void pay(Ctx c, String title, String amount, java.util.List<String[]> rows, Runnable work) {
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
                    @Override public void onError(String e) { result(c, ok + ", but your own yes vote did not go through. Vote from the Payments tab.").onSuccess(null); }
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

    static NodeGroupClient.Callback result(Ctx c, String ok) {
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

    static ArrayList<String[]> moneyRows(Ctx c, String to, String detail) {
        ArrayList<String[]> rows = new ArrayList<>();
        if (c.groupName != null && !c.groupName.isEmpty()) rows.add(new String[]{"Group", c.groupName});
        rows.add(new String[]{"To", to == null || to.isEmpty() ? escrow(c) : to});
        if (detail != null && !detail.isEmpty()) rows.add(new String[]{"For", detail});
        rows.add(new String[]{"From", "Your wallet"});
        rows.add(new String[]{"tFare", "HARV"});
        return rows;
    }

    static String escrow(Ctx c) {
        String e = str(c.group, "escrowAddress");
        return e.isEmpty() ? "This group's network wallet" : e;
    }

    private static long bondOf(Ctx c) {
        long bond = num(c.member, "bondRaw");
        if (bond <= 0) bond = num(c.member, "bondLockedRaw");
        if (bond <= 0) bond = num(c.group, "requiredBondRaw");
        return bond;
    }

    static void toast(Ctx c, String m) {
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
