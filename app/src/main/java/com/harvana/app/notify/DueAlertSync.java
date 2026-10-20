package com.harvana.app.notify;

import android.content.Context;

import com.harvana.app.CoinInfo;
import com.harvana.app.firestore.GroupFirestoreManager;
import com.harvana.app.firestore.UserProfileManager;
import com.harvana.app.group.GroupKinds;
import com.harvana.app.group.PaymentDueResolver;
import com.harvana.app.wallet.BalanceFormatter;

import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Same due rules as the home strip, run from the background worker so a due still
 * reaches the phone after the member has left the app.
 */
public final class DueAlertSync {
    private DueAlertSync() {}

    public static void run(Context ctx, String uid, Runnable done) {
        final Context c = ctx.getApplicationContext();
        if (uid == null || uid.isEmpty()) { finish(done); return; }
        UserProfileManager.getInstance(c).getProfile(uid, new UserProfileManager.DocCallback() {
            @Override public void onSuccess(Map<String, Object> profile) {
                List<String> groups = com.harvana.app.firestore.UserProfileManager.groupIds(profile == null ? null : profile.get("groups"));
                if (groups.isEmpty()) { finish(done); return; }
                AtomicInteger left = new AtomicInteger(groups.size());
                for (Object item : groups) {
                    String gid = item == null ? "" : String.valueOf(item).trim();
                    if (gid.isEmpty()) {
                        if (left.decrementAndGet() == 0) finish(done);
                        continue;
                    }
                    checkOne(c, uid, gid, () -> {
                        if (left.decrementAndGet() == 0) finish(done);
                    });
                }
            }
            @Override public void onError(String error) { finish(done); }
        });
    }

    private static void checkOne(Context c, String uid, String gid, Runnable done) {
        GroupFirestoreManager mgr = GroupFirestoreManager.getInstance(c);
        mgr.getGroup(gid, new GroupFirestoreManager.DocCallback() {
            @Override public void onSuccess(Map<String, Object> data) {
                if (GroupKinds.isLegacyRotating(data)) { done.run(); return; }
                mgr.getMembers(gid, new GroupFirestoreManager.ListCallback() {
                    @Override public void onSuccess(List<Map<String, Object>> members) {
                        mgr.getPayments(gid, new GroupFirestoreManager.ListCallback() {
                            @Override public void onSuccess(List<Map<String, Object>> payments) {
                                postIfDue(c, uid, gid, data, members, payments);
                                alertLoans(c, uid, gid, data);
                                done.run();
                            }
                            @Override public void onError(String error) { done.run(); }
                        });
                    }
                    @Override public void onError(String error) { done.run(); }
                });
            }
            @Override public void onError(String error) { done.run(); }
        });
    }

    static void postIfDue(Context c, String uid, String gid, Map<String, Object> data,
                           List<Map<String, Object>> members, List<Map<String, Object>> payments) {
        PaymentDueResolver.Result result = PaymentDueResolver.resolve(
            data, members, payments, uid, System.currentTimeMillis(), TimeZone.getDefault());
        if (result.action != PaymentDueResolver.Action.PAY) return;
        // A goal group: deposits made straight on the network may be missing from the saved copy, so ask the network
        // (group state, goal reached, this member's last deposit) before alerting. Only a real missing deposit alerts.
        com.harvana.app.group.GroupIdentity id = com.harvana.app.group.GroupIdentityResolver.resolve(gid, data);
        if (id != null && id.onChain() && "FIXED".equals(id.family)) {
            String me = com.harvana.app.wallet.WalletManager.getActiveWalletAddress(c);
            com.harvana.app.sync.NodeGroupClient.getFixedGroup(id.protocolId(), new com.harvana.app.sync.NodeGroupClient.Callback() {
                @Override public void onSuccess(org.json.JSONObject g) {
                    String state = g.optString("state", "");
                    long target = g.optLong("targetGoalAmountRaw"), raised = g.optLong("raisedAmountRaw") - g.optLong("goalBaseRaisedRaw");
                    if (!"ACTIVE".equals(state) || (target > 0 && raised >= target)) return;
                    if (me == null || me.isEmpty()) { post(c, gid, data); return; }
                    com.harvana.app.sync.NodeGroupClient.getFixedMember(id.protocolId(), me, new com.harvana.app.sync.NodeGroupClient.Callback() {
                        @Override public void onSuccess(org.json.JSONObject m) {
                            if (!PaymentDueResolver.depositedThisPeriod(text(data, "frequency", ""), m.has("lastDepositTimestamp") ? m.optLong("lastDepositTimestamp") : null,
                                    System.currentTimeMillis(), TimeZone.getDefault())) post(c, gid, data);
                        }
                        @Override public void onError(String e) { post(c, gid, data); }      // not a member record yet: nothing deposited
                    });
                }
                @Override public void onError(String e) { }                                  // network unknown: no alert rather than a wrong one
            });
            return;
        }
        post(c, gid, data);
    }

    private static void alertLoans(Context c, String uid, String gid, Map<String, Object> group) {
        GroupFirestoreManager.getInstance(c).getLoans(gid, new GroupFirestoreManager.ListCallback() {
            @Override public void onSuccess(List<Map<String, Object>> loans) {
                String name = text(group, "groupName", "Group");
                long now = System.currentTimeMillis();
                String day = DueCopy.dayKey(now);
                for (Map<String, Object> loan : loans) {
                    if (!uid.equals(text(loan, "borrowerUid", ""))) continue;
                    String st = text(loan, "status", "").toUpperCase();
                    if (!st.equals("ACTIVE") && !st.equals("DISBURSED")) continue;
                    long created = loan.get("createdAt") instanceof Number ? ((Number) loan.get("createdAt")).longValue() : 0;
                    int days = loan.get("termDays") instanceof Number ? ((Number) loan.get("termDays")).intValue() : 0;
                    if (created <= 0 || days <= 0) continue;
                    long due = created + days * 86_400_000L;
                    if (now + 3L * 86_400_000L < due) continue;
                    String id = text(loan, "_docId", gid);
                    boolean late = now >= due;
                    NotificationCenter.post(c, NotificationCenter.LOAN, "loan_due_" + id + "_" + day,
                        (late ? "Loan overdue · " : "Loan due soon · ") + name,
                        late ? "This loan is past the term you agreed. Open Loans to repay."
                            : "This loan is due within 3 days. Open Loans to repay.",
                        gid, 3);
                }
            }
            @Override public void onError(String error) { }
        });
    }

    private static void post(Context c, String gid, Map<String, Object> data) {
        String name = text(data, "groupName", text(data, "name", "Group"));
        String amount = BalanceFormatter.smartDecimal(number(data, "contributionAmount"));
        String day = DueCopy.dayKey(System.currentTimeMillis());
        NotificationCenter.post(c, NotificationCenter.PAYMENT, DueCopy.key(gid, day),
            DueCopy.title(name), DueCopy.body(amount, CoinInfo.ticker()), gid, 2);
    }

    private static void finish(Runnable done) { if (done != null) done.run(); }

    private static String text(Map<String, Object> map, String key, String fallback) {
        Object value = map == null ? null : map.get(key);
        if (value == null) return fallback;
        String text = value.toString().trim();
        return text.isEmpty() || "null".equalsIgnoreCase(text) ? fallback : text;
    }

    private static double number(Map<String, Object> map, String key) {
        Object value = map == null ? null : map.get(key);
        if (value instanceof Number) return ((Number) value).doubleValue();
        if (value != null) {
            try { return Double.parseDouble(value.toString()); } catch (Exception ignored) { }
        }
        return 0;
    }
}
