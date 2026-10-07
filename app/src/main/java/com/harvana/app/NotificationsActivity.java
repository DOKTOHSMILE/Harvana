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
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.harvana.app.notify.NotificationCenter;
import com.harvana.app.notify.NotificationRouter;
import com.harvana.app.notify.NotificationSync;
import com.harvana.app.utils.StatusBarUtil;
import com.harvana.app.utils.UiColors;

import java.util.List;

/** Everything worth telling the user about, newest first, split into tabs. Tap an item to go to where it happened. */
public class NotificationsActivity extends AppCompatActivity {
    private static final String[] TABS = {"All", "Transactions", "Groups", "Others"};
    private LinearLayout list;
    private com.google.android.material.tabs.TabLayout tabs;
    private int tab = 0;
    private final BroadcastReceiver changed = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) { render(); }
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(UiColors.background());
        tabs = new com.google.android.material.tabs.TabLayout(this);
        tabs.setBackgroundColor(UiColors.background());
        tabs.setTabMode(com.google.android.material.tabs.TabLayout.MODE_SCROLLABLE);
        tabs.setSelectedTabIndicatorColor(UiColors.accent());
        tabs.setTabTextColors(UiColors.textSecondary(), UiColors.textPrimary());
        for (String t : TABS) tabs.addTab(tabs.newTab().setText(t));
        tabs.addOnTabSelectedListener(new com.google.android.material.tabs.TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(com.google.android.material.tabs.TabLayout.Tab t) { tab = t.getPosition(); render(); }
            @Override public void onTabUnselected(com.google.android.material.tabs.TabLayout.Tab t) {}
            @Override public void onTabReselected(com.google.android.material.tabs.TabLayout.Tab t) {}
        });
        root.addView(tabs);
        ScrollView sv = new ScrollView(this);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        int p = dp(12);
        list.setPadding(p, p, p, p);
        sv.addView(list);
        root.addView(sv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        com.harvana.app.utils.AppBar.setContent(this, root, "Notifications", "Group, payment and account updates");
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
            int g = group(it.type);
            if (!it.read) { unread[0]++; unread[g]++; }
            if (tab == 0 || tab == g) items.add(it);
        }
        for (int i = 0; i < TABS.length; i++) {
            com.google.android.material.tabs.TabLayout.Tab t = tabs.getTabAt(i);
            if (t != null) t.setText(unread[i] > 0 ? TABS[i] + " (" + unread[i] + ")" : TABS[i]);
        }
        if (items.isEmpty()) {
            TextView e = tv(tab == 0 ? "You're all caught up.\nPayment reminders, group notices, invitations and transfers will show here."
                    : "Nothing in " + TABS[tab] + " yet.", 14, false, UiColors.textSecondary());
            e.setGravity(android.view.Gravity.CENTER);
            e.setPadding(dp(16), dp(48), dp(16), dp(16));
            list.addView(e);
            return;
        }
        String day = null;
        for (NotificationCenter.Item it : items) {
            String d = dayLabel(it.createdAt);
            if (!d.equals(day)) {
                day = d;
                TextView h = tv(d, 12, true, UiColors.textTertiary());
                h.setPadding(dp(4), dp(10), 0, dp(6));
                list.addView(h);
            }
            list.addView(row(it));
        }
    }

    /** Tab index for a notification type: 1 transactions, 2 groups, 3 everything else. */
    private static int group(String type) {
        if (NotificationCenter.TX.equals(type) || NotificationCenter.PAYMENT.equals(type) || NotificationCenter.LOAN.equals(type)) return 1;
        if (NotificationCenter.GROUP.equals(type) || NotificationCenter.INVITE.equals(type) || NotificationCenter.MEMBERSHIP.equals(type)) return 2;
        return 3;
    }

    private static int icon(String type) {
        switch (group(type)) {
            case 1: return NotificationCenter.LOAN.equals(type) ? R.drawable.ic_tx_loan_line : R.drawable.ic_wallet_line;
            case 2: return R.drawable.ic_groups_line;
            default: return NotificationCenter.SOCIAL.equals(type) ? R.drawable.ic_person_line : R.drawable.ic_notifications_line;
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

    private View row(NotificationCenter.Item it) {
        com.google.android.material.card.MaterialCardView card = new com.google.android.material.card.MaterialCardView(this);
        card.setCardBackgroundColor(UiColors.surface());
        card.setRadius(dp(14));
        card.setCardElevation(0);
        card.setStrokeWidth(it.read ? 0 : dp(1));
        card.setStrokeColor(UiColors.accent() & 0x00FFFFFF | 0x66000000);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(8);
        card.setLayoutParams(lp);

        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        int p = dp(14);
        r.setPadding(p, p, p, p);
        android.widget.ImageView ic = new android.widget.ImageView(this);
        ic.setImageResource(icon(it.type));
        ic.setColorFilter(UiColors.accent());
        int ip = dp(9);
        ic.setPadding(ip, ip, ip, ip);
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        bg.setColor(UiColors.accent() & 0x00FFFFFF | 0x22000000);
        ic.setBackground(bg);
        r.addView(ic, new LinearLayout.LayoutParams(dp(40), dp(40)));

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(12), 0, 0, 0);
        LinearLayout top = new LinearLayout(this);
        top.setGravity(android.view.Gravity.CENTER_VERTICAL);
        TextView head = tv(it.title, 15, !it.read, UiColors.textPrimary());
        top.addView(head, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        top.addView(tv(ago(it.createdAt), 11, false, UiColors.textTertiary()));
        if (!it.read) {
            View dot = new View(this);
            android.graphics.drawable.GradientDrawable dd = new android.graphics.drawable.GradientDrawable();
            dd.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            dd.setColor(UiColors.accent());
            dot.setBackground(dd);
            LinearLayout.LayoutParams dl = new LinearLayout.LayoutParams(dp(8), dp(8));
            dl.leftMargin = dp(6);
            top.addView(dot, dl);
        }
        col.addView(top);
        if (!it.body.isEmpty()) col.addView(tv(it.body, 13, false, UiColors.textSecondary()));
        col.addView(tv(NotificationCenter.label(it.type), 11, false, UiColors.textTertiary()));
        r.addView(col, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        card.addView(r);
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
    private TextView tv(String t, int sp, boolean bold, int color) {
        TextView v = new TextView(this);
        v.setText(t); v.setTextSize(sp); v.setTextColor(color);
        if (bold) v.setTypeface(null, Typeface.BOLD);
        v.setPadding(0, dp(2), 0, dp(2));
        return v;
    }
}
