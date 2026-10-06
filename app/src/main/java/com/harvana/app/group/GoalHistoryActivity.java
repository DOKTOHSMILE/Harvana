package com.harvana.app.group;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.harvana.app.R;
import com.harvana.app.circle.CircleUi;
import com.harvana.app.sync.NodeGroupClient;
import com.harvana.app.utils.JsonMaps;
import com.harvana.app.wallet.WalletManager;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Every goal a goal group has run, newest first. A goal group keeps one ledger per goal inside the same group: the current goal
 * at the top, then each finished goal with what was raised against its target, whether it was paid out, its dates and what
 * you put into it. Read straight from the node.
 */
public class GoalHistoryActivity extends AppCompatActivity {

    public static void open(Context c, String nodeGroupId, String groupName) {
        c.startActivity(new Intent(c, GoalHistoryActivity.class).putExtra("nodeGroupId", nodeGroupId).putExtra("groupName", groupName));
    }

    private String nodeId = "", me = "", groupName = "";
    private SwipeRefreshLayout refresh;
    private LinearLayout content;
    private Map<String, Object> group, member;
    private boolean loadedOnce;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        nodeId = orEmpty(getIntent().getStringExtra("nodeGroupId"));
        groupName = orEmpty(getIntent().getStringExtra("groupName"));
        me = orEmpty(WalletManager.getActiveWalletAddress(this));

        refresh = new SwipeRefreshLayout(this);
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(CircleUi.color(this, R.color.colorBackground));
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int pad = CircleUi.dp(this, 16);
        content.setPadding(pad, CircleUi.dp(this, 8), pad, CircleUi.dp(this, 28));
        scroll.addView(content, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        refresh.addView(scroll);
        refresh.setOnRefreshListener(this::load);
        com.harvana.app.utils.AppBar.setContent(this, refresh, "Goals", groupName);
        load();
    }

    private void load() {
        refresh.setRefreshing(true);
        if (!loadedOnce) {
            content.removeAllViews();
            content.addView(com.harvana.app.utils.Shimmer.cards(this, 3));
        }
        NodeGroupClient.getFixedGroup(nodeId, new NodeGroupClient.Callback() {
            @Override public void onSuccess(JSONObject json) {
                group = JsonMaps.toMap(json);
                if (me.isEmpty()) { member = null; runOnUiThread(GoalHistoryActivity.this::render); return; }
                NodeGroupClient.getFixedMember(nodeId, me, new NodeGroupClient.Callback() {
                    @Override public void onSuccess(JSONObject m) { member = JsonMaps.toMap(m); runOnUiThread(GoalHistoryActivity.this::render); }
                    @Override public void onError(String e) { member = null; runOnUiThread(GoalHistoryActivity.this::render); }
                });
            }
            @Override public void onError(String e) {
                runOnUiThread(() -> {
                    refresh.setRefreshing(false);
                    content.removeAllViews();
                    content.addView(card("Couldn't load the goals", e != null && e.contains("404")
                        ? "This group doesn't exist on the network." : "Pull down to try again."));
                });
            }
        });
    }

    private void render() {
        loadedOnce = true;
        refresh.setRefreshing(false);
        content.removeAllViews();
        if (group == null) return;
        if (groupName.isEmpty() && !str(group, "groupName").isEmpty()) {
            groupName = str(group, "groupName");
            com.harvana.app.utils.AppBar.setSubtitle(this, groupName);
        }

        List<?> past = group.get("goals") instanceof List ? (List<?>) group.get("goals") : java.util.Collections.emptyList();
        long current = Math.max(1L, num(group, "goalNumber"));
        long pooled = num(group, "raisedAmountRaw");

        // Summary across every goal.
        LinearLayout sum = CircleUi.card(this);
        sum.addView(CircleUi.text(this, "All goals", 15, R.color.colorTextPrimary, true));
        sum.addView(CircleUi.row(this, "Goals run", String.valueOf(current)));
        sum.addView(CircleUi.row(this, "Finished", String.valueOf(past.size())));
        sum.addView(CircleUi.row(this, "Raised in total", CircleUi.harv(pooled)));
        if (member != null) sum.addView(CircleUi.row(this, "You put in, all goals", CircleUi.harv(num(member, "contributedRaw"))));
        content.addView(sum);

        // Current goal.
        content.addView(header("Current goal"));
        long target = num(group, "targetGoalAmountRaw");
        long raised = Math.max(0L, pooled - num(group, "goalBaseRaisedRaw"));
        String name = current > 1 ? str(group, "goalName") : str(group, "groupName");
        String state = str(group, "state");
        LinearLayout cur = CircleUi.card(this);
        cur.addView(CircleUi.text(this, title(current, name), 15, R.color.colorTextPrimary, true));
        LinearProgressIndicator bar = new LinearProgressIndicator(this);
        bar.setMax(1000);
        bar.setProgressCompat((int) Math.min(1000, target > 0 ? raised * 1000 / target : 0), false);
        bar.setTrackCornerRadius(CircleUi.dp(this, 4));
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(-1, -2);
        blp.setMargins(0, CircleUi.dp(this, 10), 0, CircleUi.dp(this, 6));
        cur.addView(bar, blp);
        cur.addView(CircleUi.row(this, "Raised", CircleUi.harv(raised) + " of " + CircleUi.harv(target)));
        cur.addView(CircleUi.row(this, "Status", "COMPLETED".equals(state) ? "Paid out"
            : target > 0 && raised >= target ? "Goal reached" : "Collecting"));
        long started = current > 1 ? num(group, "goalStartedTimestamp") : num(group, "createdTimestamp");
        if (started > 0) cur.addView(CircleUi.row(this, "Started", date(started)));
        if (member != null) cur.addView(CircleUi.row(this, "You put in", CircleUi.harv(mine(current))));
        content.addView(cur);

        // Finished goals, newest first.
        content.addView(header("Finished goals"));
        if (past.isEmpty()) {
            content.addView(card("No finished goals yet",
                "When this goal is reached, the admin can start the next goal here in the same group. Every finished goal is kept on this page."));
        }
        for (int i = past.size() - 1; i >= 0; i--) {
            if (!(past.get(i) instanceof Map)) continue;
            @SuppressWarnings("unchecked") Map<String, Object> g = (Map<String, Object>) past.get(i);
            long n = num(g, "number");
            LinearLayout c = CircleUi.card(this);
            c.addView(CircleUi.text(this, title(n, str(g, "name")), 15, R.color.colorTextPrimary, true));
            c.addView(CircleUi.row(this, "Raised", CircleUi.harv(num(g, "raisedRaw")) + " of " + CircleUi.harv(num(g, "targetRaw"))));
            c.addView(CircleUi.row(this, "Paid out", Boolean.TRUE.equals(g.get("paidOut")) ? "Yes" : "Kept in the group wallet"));
            long s = num(g, "startedTimestamp"), e = num(g, "closedTimestamp");
            if (s > 0) c.addView(CircleUi.row(this, "Started", date(s)));
            if (e > 0) c.addView(CircleUi.row(this, "Closed", date(e)));
            if (member != null) c.addView(CircleUi.row(this, "You put in", CircleUi.harv(mine(n))));
            content.addView(c);
        }
    }

    /** What you put into one goal. Goals 2+ are recorded per goal; goal 1 is whatever is left of your total. */
    private long mine(long goalNo) {
        if (member == null) return 0L;
        Map<?, ?> by = member.get("contributedByGoal") instanceof Map ? (Map<?, ?>) member.get("contributedByGoal") : null;
        if (goalNo > 1) {
            Object v = by == null ? null : by.get(String.valueOf(goalNo));
            return v instanceof Number ? ((Number) v).longValue() : 0L;
        }
        long rest = num(member, "contributedRaw");
        if (by != null) for (Object v : by.values()) if (v instanceof Number) rest -= ((Number) v).longValue();
        return Math.max(0L, rest);
    }

    private String title(long n, String name) { return "Goal " + n + (name.isEmpty() ? "" : " · " + name); }

    private TextView header(String t) {
        TextView h = CircleUi.text(this, t.toUpperCase(Locale.US), 11.5f, R.color.colorTextTertiary, true);
        h.setPadding(CircleUi.dp(this, 4), CircleUi.dp(this, 22), CircleUi.dp(this, 4), 0);
        return h;
    }

    private LinearLayout card(String title, String body) {
        LinearLayout c = CircleUi.card(this);
        c.addView(CircleUi.text(this, title, 14.5f, R.color.colorTextPrimary, true));
        TextView t = CircleUi.text(this, body, 12.5f, R.color.colorTextSecondary, false);
        t.setPadding(0, CircleUi.dp(this, 4), 0, 0);
        c.addView(t);
        return c;
    }

    private static String date(long ms) { return new SimpleDateFormat("d MMM yyyy", Locale.US).format(new Date(ms)); }
    private static String orEmpty(String s) { return s == null ? "" : s; }
    private static String str(Map<String, Object> m, String k) { Object o = m == null ? null : m.get(k); return o == null ? "" : String.valueOf(o); }
    private static long num(Map<String, Object> m, String k) { Object o = m == null ? null : m.get(k); return o instanceof Number ? ((Number) o).longValue() : 0L; }
}
