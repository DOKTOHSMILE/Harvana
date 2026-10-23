package com.harvana.app.notify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.View;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

/**
 * One unread count for every bell. Home and the main toolbar both read {@link NotificationCenter#unreadCount}
 * and repaint when the inbox changes, so they cannot drift apart.
 */
public final class NotifBell {
    private NotifBell() {}

    public static int count(Context c) {
        return c == null ? 0 : NotificationCenter.unreadCount(c);
    }

    /** Paints {@code badge}. Hidden at zero. Returns the count it wrote. */
    public static int paint(TextView badge, int n) {
        if (badge == null) return n;
        if (n <= 0) {
            badge.setVisibility(View.GONE);
            return 0;
        }
        badge.setVisibility(View.VISIBLE);
        badge.setText(n > 99 ? "99+" : String.valueOf(n));
        return n;
    }

    /** Repaint {@code badge} for as long as {@code host} is started. Call again from onResume; remove with {@link #stop}. */
    public static BroadcastReceiver listen(Fragment host, TextView badge) {
        if (host == null || host.getContext() == null) return null;
        BroadcastReceiver r = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) { paint(badge, count(context)); }
        };
        LocalBroadcastManager.getInstance(host.requireContext())
                .registerReceiver(r, new IntentFilter(NotificationCenter.ACTION_CHANGED));
        paint(badge, count(host.getContext()));
        return r;
    }

    public static void stop(Context c, BroadcastReceiver r) {
        if (c == null || r == null) return;
        try { LocalBroadcastManager.getInstance(c).unregisterReceiver(r); } catch (Exception ignored) { }
    }
}
