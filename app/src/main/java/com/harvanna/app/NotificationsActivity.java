package com.harvanna.app;

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

import com.harvanna.app.notify.NotificationCenter;
import com.harvanna.app.notify.NotificationSync;
import com.harvanna.app.utils.StatusBarUtil;
import com.harvanna.app.utils.UiColors;

import java.util.List;

/** Everything worth telling the user about, newest first. Tap an item to go to where it happened. */
public class NotificationsActivity extends AppCompatActivity {
    private LinearLayout list;
    private final BroadcastReceiver changed = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) { render(); }
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        StatusBarUtil.matchToolbar(this);
        setTitle("Notifications");
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(UiColors.background());
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        int p = dp(12);
        list.setPadding(p, p, p, p);
        sv.addView(list);
        setContentView(sv);
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
        List<NotificationCenter.Item> items = NotificationCenter.list(this);
        if (items.isEmpty()) {
            list.addView(tv("You're all caught up.\nPayment reminders, group notices, invitations and transfers will show here.", 14, false, UiColors.textSecondary()));
            return;
        }
        for (NotificationCenter.Item it : items) list.addView(row(it));
    }

    private View row(NotificationCenter.Item it) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.VERTICAL);
        r.setBackgroundColor(UiColors.surface());
        int p = dp(14);
        r.setPadding(p, p, p, p);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(8);
        r.setLayoutParams(lp);
        TextView head = tv((it.read ? "" : "● ") + it.title, 15, !it.read, it.read ? UiColors.textPrimary() : UiColors.accent());
        r.addView(head);
        if (!it.body.isEmpty()) r.addView(tv(it.body, 13, false, UiColors.textSecondary()));
        r.addView(tv(NotificationCenter.label(it.type) + " · " + ago(it.createdAt), 11, false, UiColors.textTertiary()));
        r.setOnClickListener(v -> open(it));
        return r;
    }

    private void open(NotificationCenter.Item it) {
        NotificationCenter.markRead(this, it.id);
        Intent i;
        switch (it.type) {
            case NotificationCenter.INVITE: i = new Intent(this, InvitesActivity.class); break;
            case NotificationCenter.TX:
                i = new Intent(this, MainActivity.class).putExtra("open_fragment", "asset")
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                break;
            default:
                if (!it.groupId.isEmpty()) {
                    i = new Intent(this, GroupDetailsActivity.class).putExtra("groupId", it.groupId);
                    if (it.tab >= 0) i.putExtra("tab", it.tab);
                } else return;
        }
        startActivity(i);
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
