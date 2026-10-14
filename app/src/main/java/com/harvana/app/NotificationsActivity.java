package com.harvana.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.harvana.app.notify.NotificationCenter;
import com.harvana.app.notify.NotificationRouter;
import com.harvana.app.notify.NotificationSync;

import java.util.List;

/** Everything worth telling the user about, newest first, split into tabs. Tap an item to go to where it happened. */
public class NotificationsActivity extends AppCompatActivity {
    private static final String[] TABS = {"All", "Transactions", "Groups", "Trades", "Others"};
    private LinearLayout list;
    private com.google.android.material.tabs.TabLayout tabs;
    private int tab = 0;
    private final BroadcastReceiver changed = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) { render(); }
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        com.harvana.app.utils.AppBar.setContent(this, R.layout.activity_notifications, "Notifications", "Group, payment and account updates");
        list = findViewById(R.id.notifications_list);
        tabs = findViewById(R.id.notifications_tabs);
        tabs.setTabMode(com.google.android.material.tabs.TabLayout.MODE_SCROLLABLE);
        com.harvana.app.utils.Toggles.pill(tabs);
        for (String t : TABS) tabs.addTab(tabs.newTab().setText(t));
        tabs.addOnTabSelectedListener(new com.google.android.material.tabs.TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(com.google.android.material.tabs.TabLayout.Tab t) { tab = t.getPosition(); render(); }
            @Override public void onTabUnselected(com.google.android.material.tabs.TabLayout.Tab t) {}
            @Override public void onTabReselected(com.google.android.material.tabs.TabLayout.Tab t) {}
        });
    }

    @Override protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(this).registerReceiver(changed, new IntentFilter(NotificationCenter.ACTION_CHANGED));
    }
    @Override protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(changed);
    }
    @Override protected void onResume() {
        super.onResume();
        NotificationCenter.tidy(this);
        render();
        NotificationSync.run(this, null); // pick up anything new right now
    }

    @Override public boolean onCreateOptionsMenu(Menu m) {
        m.add(0, 1, 0, "Mark all read").setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        m.add(0, 2, 1, "Clear all").setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        m.add(0, 3, 2, "Notification settings").setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        return true;
    }
    @Override public boolean onOptionsItemSelected(MenuItem it) {
        if (it.getItemId() == 1) NotificationCenter.markAllRead(this);
        else if (it.getItemId() == 2) NotificationCenter.clear(this);
        else if (it.getItemId() == 3) startActivity(new Intent(this, NotificationSettingsActivity.class));
        else if (it.getItemId() == android.R.id.home) finish();
        return true;
    }

    private void render() {
        list.removeAllViews();
        List<NotificationCenter.Item> all = NotificationCenter.list(this);
        int[] unread = new int[TABS.length];
        java.util.ArrayList<NotificationCenter.Item> items = new java.util.ArrayList<>();
        for (NotificationCenter.Item it : all) {
            int g = group(it);
            if (!it.read) { unread[0]++; unread[g]++; }
            if (tab == 0 || tab == g) items.add(it);
        }
        for (int i = 0; i < TABS.length; i++) {
            com.google.android.material.tabs.TabLayout.Tab t = tabs.getTabAt(i);
            if (t != null) t.setText(unread[i] > 0 ? TABS[i] + " (" + unread[i] + ")" : TABS[i]);
        }
        if (items.isEmpty()) {
            TextView e = (TextView) getLayoutInflater().inflate(R.layout.item_list_empty, list, false);
            e.setText(tab == 0 ? "You're all caught up.\nPayment reminders, group notices, trade messages and transfers will show here."
                    : "Nothing in " + TABS[tab] + " yet.");
            list.addView(e);
            return;
        }
        String day = null;
        for (NotificationCenter.Item it : items) {
            String d = dayLabel(it.createdAt);
            if (!d.equals(day)) {
                day = d;
                TextView h = (TextView) getLayoutInflater().inflate(R.layout.item_list_day_header, list, false);
                h.setText(d);
                list.addView(h);
            }
            list.addView(row(it));
        }
    }

    /** Tab index: 1 transactions, 2 groups, 3 trades, 4 everything else. */
    private static int group(NotificationCenter.Item it) {
        if (it.id != null && it.id.startsWith("p2pchat_")) return 3;
        if (NotificationCenter.TX.equals(it.type) || NotificationCenter.PAYMENT.equals(it.type) || NotificationCenter.LOAN.equals(it.type)) return 1;
        if (NotificationCenter.GROUP.equals(it.type) || NotificationCenter.INVITE.equals(it.type) || NotificationCenter.MEMBERSHIP.equals(it.type)) return 2;
        return 4;
    }

    private static int icon(NotificationCenter.Item it) {
        if (it.id != null && it.id.startsWith("p2pchat_")) return R.drawable.ic_person_line;
        switch (group(it)) {
            case 1: return NotificationCenter.LOAN.equals(it.type) ? R.drawable.ic_tx_loan_line : R.drawable.ic_wallet_line;
            case 2: return R.drawable.ic_groups_line;
            default: return NotificationCenter.SOCIAL.equals(it.type) ? R.drawable.ic_person_line : R.drawable.ic_notifications_line;
        }
    }

    private static String dayLabel(long t) {
        java.util.Calendar now = java.util.Calendar.getInstance(), c = java.util.Calendar.getInstance();
        c.setTimeInMillis(t);
        if (now.get(java.util.Calendar.YEAR) == c.get(java.util.Calendar.YEAR)) {
            int diff = now.get(java.util.Calendar.DAY_OF_YEAR) - c.get(java.util.Calendar.DAY_OF_YEAR);
            if (diff == 0) return "Today";
            if (diff == 1) return "Yesterday";
        }
        return new java.text.SimpleDateFormat("d MMM yyyy", java.util.Locale.getDefault()).format(new java.util.Date(t));
    }

    /** One notification: res/layout/item_notification.xml. */
    private View row(NotificationCenter.Item it) {
        com.google.android.material.card.MaterialCardView card = (com.google.android.material.card.MaterialCardView)
                getLayoutInflater().inflate(R.layout.item_notification, list, false);
        card.setStrokeWidth(it.read ? 0 : dp(1));
        ((android.widget.ImageView) card.findViewById(R.id.iv_notification_icon)).setImageResource(icon(it));
        TextView head = card.findViewById(R.id.tv_notification_title);
        head.setText(it.title);
        head.setTypeface(null, it.read ? Typeface.NORMAL : Typeface.BOLD);
        ((TextView) card.findViewById(R.id.tv_notification_time)).setText(ago(it.createdAt));
        card.findViewById(R.id.notification_unread_dot).setVisibility(it.read ? View.GONE : View.VISIBLE);
        TextView body = card.findViewById(R.id.tv_notification_body);
        if (!it.body.isEmpty()) { body.setText(it.body); body.setVisibility(View.VISIBLE); }
        ((TextView) card.findViewById(R.id.tv_notification_kind)).setText(NotificationCenter.label(it.type));
        card.setOnClickListener(v -> open(it));
        return card;
    }

    private void open(NotificationCenter.Item it) {
        NotificationRouter.open(this, it);
    }

    private static String ago(long t) {
        long m = Math.max(0, (System.currentTimeMillis() - t) / 60000L);
        if (m < 1) return "just now";
        if (m < 60) return m + " min ago";
        if (m < 1440) return (m / 60) + " h ago";
        return (m / 1440) + " d ago";
    }
    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
