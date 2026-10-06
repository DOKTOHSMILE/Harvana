package com.harvana.app.group;

import android.content.Context;
import android.content.SharedPreferences;

import com.harvana.app.firestore.GroupFirestoreManager;

import java.util.Map;

/**
 * "Remind" for an overdue member: posts a system notice in the group's notice feed (visible to every member,
 * naming the person) and rate-limits it so nobody can be nagged repeatedly. This is an in-app notice only;
 * it does not send a push notification.
 */
public final class GroupReminders {

    private static final String PREFS = "group_reminders";
    private static final long COOLDOWN_MS = 6L * 60 * 60 * 1000;   // one reminder per member per group per 6 hours

    public interface Callback {
        void onSent();
        void onError(String error);
    }

    private GroupReminders() {}

    public static boolean canRemind(Context ctx, String groupId, String memberUid) {
        long last = ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(key(groupId, memberUid), 0L);
        return System.currentTimeMillis() - last >= COOLDOWN_MS;
    }

    public static void remind(Context ctx, String groupId, String memberUid, String memberName, String amountText, Callback cb) {
        if (groupId == null || groupId.isEmpty() || memberUid == null || memberUid.isEmpty()) {
            if (cb != null) cb.onError("Missing group or member.");
            return;
        }
        if (!canRemind(ctx, groupId, memberUid)) {
            if (cb != null) cb.onError("You already reminded " + memberName + " recently. Try again later.");
            return;
        }
        String name = memberName == null || memberName.isEmpty() ? "A member" : memberName;
        String message = name + ", your contribution" + (amountText == null || amountText.isEmpty() ? "" : " of " + amountText)
                + " for this cycle is still due. Please pay when you can.";
        Map<String, Object> fields = GroupNoticeHelper.buildTargeted("Payment reminder", message, memberUid,
                com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() == null ? "" : com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid(), "PAYMENT");
        GroupFirestoreManager.getInstance(ctx.getApplicationContext()).addNotice(groupId, fields, new GroupFirestoreManager.IdCallback() {
            @Override public void onSuccess(String id) {
                ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                        .edit().putLong(key(groupId, memberUid), System.currentTimeMillis()).apply();
                if (cb != null) cb.onSent();
            }
            @Override public void onError(String error) {
                if (cb != null) cb.onError(error != null ? error : "Couldn't send the reminder.");
            }
        });
    }

    private static String key(String groupId, String memberUid) { return groupId + "|" + memberUid; }
}
