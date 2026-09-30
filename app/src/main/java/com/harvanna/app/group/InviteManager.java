package com.harvanna.app.group;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.harvanna.app.InvitesActivity;
import com.harvanna.app.firestore.GroupFirestoreManager;
import com.harvanna.app.utils.UiColors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Group invites, keyed by the invitee's email so people who haven't signed up yet can be invited too.
 * Stored in INVITES/{groupId}_{emailKey}. The invitee sees them on their next app open (and gets an
 * Android notification then); a true background push needs a server (FCM + Cloud Function), which
 * this project does not have yet.
 */
public final class InviteManager {
    public static final long TTL_MS = 7L * 24 * 60 * 60 * 1000;
    private static final String CHANNEL = "group_invites";
    private static final String PREFS = "group_invites_seen";

    public interface Done { void onDone(String errorOrNull); }
    public interface ListCb { void onList(List<Map<String, Object>> invites); void onError(String e); }

    private InviteManager() {}

    public static String normalizeEmail(String e) {
        return e == null ? "" : e.trim().toLowerCase(Locale.US);
    }

    public static boolean looksLikeEmail(String e) {
        return e != null && e.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    }

    /** Firestore-safe doc id. */
    public static String idFor(String groupId, String email) {
        return groupId + "_" + normalizeEmail(email).replaceAll("[^a-z0-9]", "-");
    }

    public static boolean isExpired(Map<String, Object> inv, long now) {
        Object o = inv.get("expiresAt");
        return o instanceof Number && ((Number) o).longValue() < now;
    }

    public static void send(Context ctx, String groupId, Map<String, Object> group, String email,
                            String message, Done done) {
        FirebaseUser me = FirebaseAuth.getInstance().getCurrentUser();
        String e = normalizeEmail(email);
        if (me == null) { done.onDone("Sign in first."); return; }
        if (!looksLikeEmail(e)) { done.onDone("Enter a valid email address."); return; }
        if (e.equals(normalizeEmail(me.getEmail()))) { done.onDone("You can't invite yourself."); return; }
        long now = System.currentTimeMillis();
        Map<String, Object> f = new HashMap<>();
        f.put("groupId", groupId);
        f.put("groupName", str(group, "groupName", "Group"));
        f.put("groupType", str(group, "groupType", ""));
        f.put("inviterUid", me.getUid());
        f.put("inviterName", me.getDisplayName() == null ? "A member" : me.getDisplayName());
        f.put("inviteeEmail", e);
        f.put("message", message == null ? "" : message.trim());
        f.put("status", "PENDING");
        f.put("createdAt", now);
        f.put("expiresAt", now + TTL_MS);
        GroupFirestoreManager.getInstance(ctx).setTopLevelDoc("INVITES", idFor(groupId, e), f,
            new GroupFirestoreManager.SimpleCallback() {
                @Override public void onSuccess() {
                    GroupFirestoreManager.getInstance(ctx).postSystemNotice(groupId, "Invitation sent",
                        "An invitation was sent to " + mask(e) + ".");
                    done.onDone(null);
                }
                @Override public void onError(String err) { done.onDone("Couldn't send the invite: " + err); }
            });
    }

    /** Pending, unexpired invites for the signed-in user's verified email. */
    public static void listMine(Context ctx, ListCb cb) {
        FirebaseUser me = FirebaseAuth.getInstance().getCurrentUser();
        if (me == null || me.getEmail() == null) { cb.onList(new ArrayList<>()); return; }
        GroupFirestoreManager.getInstance(ctx).queryTopLevel("INVITES", "inviteeEmail",
            normalizeEmail(me.getEmail()), 50, new GroupFirestoreManager.ListCallback() {
                @Override public void onSuccess(List<Map<String, Object>> items) {
                    long now = System.currentTimeMillis();
                    List<Map<String, Object>> out = new ArrayList<>();
                    for (Map<String, Object> m : items) {
                        if ("PENDING".equals(str(m, "status", "")) && !isExpired(m, now)) out.add(m);
                    }
                    cb.onList(out);
                }
                @Override public void onError(String error) { cb.onError(error); }
            });
    }

    public static void respond(Context ctx, String inviteId, boolean accepted, Done done) {
        Map<String, Object> f = new HashMap<>();
        f.put("status", accepted ? "ACCEPTED" : "DECLINED");
        f.put("respondedAt", System.currentTimeMillis());
        GroupFirestoreManager.getInstance(ctx).updateTopLevelFields("INVITES", inviteId, f,
            new GroupFirestoreManager.SimpleCallback() {
                @Override public void onSuccess() { if (done != null) done.onDone(null); }
                @Override public void onError(String error) { if (done != null) done.onDone(error); }
            });
    }

    /** Called when the app opens: raises a notification for invites not yet announced on this phone. */
    public static void checkAndNotify(Context ctx) {
        final Context app = ctx.getApplicationContext();
        listMine(app, new ListCb() {
            @Override public void onList(List<Map<String, Object>> invites) {
                SharedPreferences sp = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
                for (Map<String, Object> inv : invites) {
                    String id = str(inv, "_docId", "");
                    if (id.isEmpty() || sp.getBoolean(id, false)) continue;
                    sp.edit().putBoolean(id, true).apply();
                    com.harvanna.app.notify.NotificationCenter.post(app, com.harvanna.app.notify.NotificationCenter.INVITE, "invite_" + id,
                        "You're invited to " + str(inv, "groupName", "a group"),
                        str(inv, "inviterName", "A member") + " invited you. Open to read the details before you decide.", "", -1);
                }
            }
            @Override public void onError(String e) { }
        });
    }

    private static void notifyInvite(Context app, String id, String inviter, String group) {
        NotificationManager nm = (NotificationManager) app.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(new NotificationChannel(CHANNEL, "Group invitations", NotificationManager.IMPORTANCE_HIGH));
        }
        Intent tap = new Intent(app, InvitesActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);
        PendingIntent pi = PendingIntent.getActivity(app, id.hashCode(), tap, flags);
        NotificationCompat.Builder b = new NotificationCompat.Builder(app, CHANNEL)
            .setContentTitle("You're invited to " + group)
            .setContentText(inviter + " invited you. Tap to read the details before you decide.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setColor(UiColors.accent())
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH);
        try { nm.notify(id.hashCode(), b.build()); } catch (SecurityException ignored) { }
    }

    private static String mask(String email) {
        int at = email.indexOf('@');
        return at <= 1 ? email : email.charAt(0) + "***" + email.substring(at);
    }

    private static String str(Map<String, Object> m, String k, String fb) {
        Object o = m.get(k); return o == null || String.valueOf(o).trim().isEmpty() ? fb : String.valueOf(o).trim();
    }
}
