package com.harvana.app.notify;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.firebase.auth.FirebaseAuth;
import com.harvana.app.NotificationsActivity;
import com.harvana.app.utils.UiColors;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * The app's single notification inbox. Everything worth telling the user goes through post(): it is
 * saved here (so the Notifications screen can list it), de-duplicated, and shown as an Android
 * notification if the user's notification settings allow that category.
 */
public final class NotificationCenter {
    public static final String ACTION_CHANGED = "com.harvana.app.ACTION_NOTIFICATIONS_CHANGED";
    public static final String TX = "TX", GROUP = "GROUP", PAYMENT = "PAYMENT", LOAN = "LOAN", INVITE = "INVITE", MEMBERSHIP = "MEMBERSHIP", SYSTEM = "SYSTEM", SOCIAL = "SOCIAL";
    private static final int CAP = 200;

    public static final class Item {
        public String id, type, title, body, groupId;
        public int tab = -1;
        public long createdAt;
        /** When the thing happened, not when this phone stored the notice. Missing on older rows, so those use createdAt. */
        public long eventAt;
        public boolean read;
    }

    private NotificationCenter() {}

    private static String prefsName() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
            ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "anon";
        return "notif_center_" + uid;
    }

    private static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences(prefsName(), Context.MODE_PRIVATE);
    }

    public static synchronized List<Item> list(Context c) {
        List<Item> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(sp(c).getString("items", "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                Item it = new Item();
                it.id = o.optString("id"); it.type = o.optString("type"); it.title = o.optString("title");
                it.body = o.optString("body"); it.groupId = o.optString("groupId");
                it.tab = o.optInt("tab", -1); it.createdAt = o.optLong("createdAt"); it.read = o.optBoolean("read");
                it.eventAt = o.optLong("eventAt", it.createdAt);
                out.add(it);
            }
        } catch (Exception ignored) { }
        return out;
    }

    /** Clears copies that were saved before this rule: a "submitted" notice whose transfer was later confirmed. */
    public static synchronized void tidy(Context c) {
        List<Item> items = list(c);
        java.util.Set<String> confirmed = new java.util.HashSet<>();
        for (Item i : items) if (i.id.startsWith("tx_") && (i.id.endsWith("_out") || i.id.endsWith("_in"))) confirmed.add(i.id.substring(0, i.id.lastIndexOf('_')));
        boolean changed = false;
        for (java.util.Iterator<Item> it = items.iterator(); it.hasNext(); ) {
            Item i = it.next();
            if (i.id.endsWith("_submitted") && confirmed.contains(i.id.substring(0, i.id.length() - "_submitted".length()))) { it.remove(); changed = true; }
        }
        if (changed) save(c, items);
    }

    public static int unreadCount(Context c) {
        int n = 0;
        for (Item i : list(c)) if (!i.read) n++;
        return n;
    }

    /** dedupeKey: same key never notifies twice (e.g. a tx hash, or noticeId). */
    public static synchronized boolean post(Context ctx, String type, String dedupeKey, String title, String body,
                                            String groupId, int tab) {
        return post(ctx, type, dedupeKey, title, body, groupId, tab, System.currentTimeMillis(), true);
    }

    /**
     * @param eventAt when the action happened. The inbox sorts by this, not by when the notice was stored.
     * @param tray false keeps it in the inbox only, so an old transfer cannot pop the status bar.
     */
    public static synchronized boolean post(Context ctx, String type, String dedupeKey, String title, String body,
                                            String groupId, int tab, long eventAt, boolean tray) {
        Context c = ctx.getApplicationContext();
        List<Item> items = list(c);
        for (Item i : items) if (i.id.equals(dedupeKey)) return false;
        // The first check after signing in only fills the inbox: what it finds happened before, and popping it on the
        // status bar at every sign-in repeated old news.
        if (tray && !primed(c)) tray = false;
        // The confirmation of a transfer replaces its "on its way" notice instead of sitting beside it.
        if (dedupeKey.startsWith("tx_") && (dedupeKey.endsWith("_out") || dedupeKey.endsWith("_in"))) {
            String stem = dedupeKey.substring(0, dedupeKey.lastIndexOf('_'));
            for (java.util.Iterator<Item> it2 = items.iterator(); it2.hasNext(); ) if (it2.next().id.equals(stem + "_submitted")) it2.remove();
        }
        Item it = new Item();
        it.id = dedupeKey; it.type = type; it.title = title; it.body = body;
        it.groupId = groupId == null ? "" : groupId; it.tab = tab; it.createdAt = System.currentTimeMillis();
        it.eventAt = eventAt > 0 ? eventAt : it.createdAt;
        items.add(0, it);
        while (items.size() > CAP) items.remove(items.size() - 1);
        save(c, items);
        if (tray) show(c, it);
        LocalBroadcastManager.getInstance(c).sendBroadcast(new Intent(ACTION_CHANGED));
        return true;
    }

    /** True once a full check has run for the signed-in account on this phone. */
    public static boolean primed(Context c) { return sp(c).getBoolean("primed", false); }

    /** Called when the first full check after signing in has finished. */
    public static void markPrimed(Context c) { sp(c).edit().putBoolean("primed", true).apply(); }

    public static synchronized void markRead(Context c, String id) {
        List<Item> items = list(c);
        for (Item i : items) if (i.id.equals(id)) i.read = true;
        save(c, items);
        LocalBroadcastManager.getInstance(c).sendBroadcast(new Intent(ACTION_CHANGED));
    }

    public static synchronized void markAllRead(Context c) {
        List<Item> items = list(c);
        for (Item i : items) i.read = true;
        save(c, items);
        LocalBroadcastManager.getInstance(c).sendBroadcast(new Intent(ACTION_CHANGED));
    }

    public static synchronized void clear(Context c) {
        save(c, new ArrayList<>());
        LocalBroadcastManager.getInstance(c).sendBroadcast(new Intent(ACTION_CHANGED));
    }

    private static void save(Context c, List<Item> items) {
        try {
            JSONArray a = new JSONArray();
            for (Item i : items) {
                a.put(new JSONObject().put("id", i.id).put("type", i.type).put("title", i.title).put("body", i.body)
                    .put("groupId", i.groupId).put("tab", i.tab).put("createdAt", i.createdAt)
                    .put("eventAt", i.eventAt).put("read", i.read));
            }
            sp(c).edit().putString("items", a.toString()).apply();
        } catch (Exception ignored) { }
    }

    /** User's category switches (Notification settings). Invites, membership and system are always on. */
    static boolean allowed(Context c, String type) {
        SharedPreferences p = c.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE);
        switch (type) {
            case TX: return p.getBoolean("transaction_alerts", true);
            case PAYMENT: return p.getBoolean("payment_reminders", true);
            case LOAN: return p.getBoolean("loan_reminders", true);
            case GROUP: return p.getBoolean("group_notices", true);
            default: return true;
        }
    }

    private static void show(Context c, Item it) {
        if (!allowed(c, it.type)) return;
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        SharedPreferences p = c.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE);
        boolean sound = p.getBoolean("sound", true), vibrate = p.getBoolean("vibrate", true);
        int importance = importance(it.type);
        // A new channel id: Android will not lower a channel that was already created at high importance.
        String channel = "harv2_" + it.type.toLowerCase() + "_" + importance + (sound ? "_s" : "") + (vibrate ? "_v" : "");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(channel, label(it.type), importance);
            if (!sound) ch.setSound(null, null);
            ch.enableVibration(vibrate);
            nm.createNotificationChannel(ch);
        }
        Intent tap = new Intent(c, NotificationRouteActivity.class).putExtra(NotificationRouteActivity.EXTRA_ID, it.id)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);
        PendingIntent pi = PendingIntent.getActivity(c, it.id.hashCode(), tap, flags);
        NotificationCompat.Builder b = new NotificationCompat.Builder(c, channel)
            .setContentTitle(it.title).setContentText(it.body)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(it.body))
            .setSmallIcon(com.harvana.app.R.drawable.ic_stat_notify)
            .setColor(UiColors.accent())
            .setContentIntent(pi).setAutoCancel(true)
            .setPriority(priority(importance));
        try { nm.notify(it.id.hashCode(), b.build()); } catch (SecurityException ignored) { }
    }

    /** Transactions and money due outrank group notices, which outrank social and system. */
    public static int rank(String type) {
        if (TX.equals(type) || PAYMENT.equals(type) || LOAN.equals(type)) return 3;
        if (GROUP.equals(type) || INVITE.equals(type) || MEMBERSHIP.equals(type)) return 2;
        if (SOCIAL.equals(type)) return 1;
        return 0;
    }

    private static int importance(String type) {
        int r = rank(type);
        if (r >= 3) return NotificationManager.IMPORTANCE_HIGH;
        if (r == 2) return NotificationManager.IMPORTANCE_DEFAULT;
        return NotificationManager.IMPORTANCE_LOW;
    }

    private static int priority(int importance) {
        if (importance >= NotificationManager.IMPORTANCE_HIGH) return NotificationCompat.PRIORITY_HIGH;
        if (importance >= NotificationManager.IMPORTANCE_DEFAULT) return NotificationCompat.PRIORITY_DEFAULT;
        return NotificationCompat.PRIORITY_LOW;
    }

    public static String label(String type) {
        switch (type) {
            case TX: return "Transactions";
            case GROUP: return "Group notices";
            case PAYMENT: return "Payment reminders";
            case LOAN: return "Loans";
            case INVITE: return "Invitations";
            case MEMBERSHIP: return "Membership";
            case SOCIAL: return "Friends";
            default: return "Harvana";
        }
    }
}
