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
                Object raw = profile == null ? null : profile.get("groups");
                if (!(raw instanceof List) || ((List<?>) raw).isEmpty()) { finish(done); return; }
                List<?> groups = (List<?>) raw;
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
