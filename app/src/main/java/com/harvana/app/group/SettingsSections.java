package com.harvana.app.group;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.harvana.app.CoinInfo;
import com.harvana.app.utils.UiColors;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Lays the group settings page out in the standard sections: General, Access, Members, Money and rules, Network, More. The existing
 * cards keep all their logic; this only decides which section each one lives in, shows one section at a time under a chip bar, and adds
 * the cards a group settings page is expected to have (invite, members summary, group info with ID, the contract, leave).
 */
public final class SettingsSections {

    public interface Host {
        void invite();
        void transferAdmin();
        void leave();
    }

    public static final String[] NAMES = {"General", "Access", "Members", "Money & rules", "Network", "More"};
    private static final int GENERAL = 0, ACCESS = 1, MEMBERS = 2, MONEY = 3, NETWORK = 4, MORE = 5;
    private static final String TAG_TABS = "settings_tabs", TAG_EXTRA = "settings_extra";

    private final Activity a;
    private final LinearLayout page;
    private final View scroller;
    private final Host host;
    private final float d;
    private int selected;
    private final List<View> views = new ArrayList<>();
    private final List<Integer> where = new ArrayList<>();
    private final TextView[] chips = new TextView[NAMES.length];

    public SettingsSections(Activity a, LinearLayout page, View scroller, Host host) {
        this.a = a;
        this.page = page;
        this.scroller = scroller;
        this.host = host;
        this.d = a.getResources().getDisplayMetrics().density;
    }

    /** Call after every (re)build of the page. The views are the page's own cards; null ones are skipped. */
    public void apply(String groupId, Map<String, Object> g, boolean isAdmin, int memberCount, boolean onChain,
                      View hero, View about, View access, View money, View rules, View wallet, View notifications,
                      View blockedHeader, View networkPanel, View closeSection) {
        for (int i = page.getChildCount() - 1; i >= 0; i--) if (TAG_EXTRA.equals(page.getChildAt(i).getTag())) page.removeViewAt(i);
        if (page.findViewWithTag(TAG_TABS) == null) page.addView(buildTabs(), 0);

        views.clear();
        where.clear();
        add(hero, GENERAL); add(about, GENERAL);
        add(access, ACCESS);
        add(money, MONEY); add(rules, MONEY); add(wallet, MONEY);
        add(networkPanel, NETWORK);
        add(notifications, MORE);
        // The blocked-members header and everything after it (the list and its note) belong to Members.
        int from = blockedHeader == null ? -1 : page.indexOfChild(blockedHeader);
        if (from >= 0) for (int i = from; i < page.getChildCount(); i++) {
            View c = page.getChildAt(i);
            if (c == networkPanel || c == closeSection || TAG_EXTRA.equals(c.getTag()) || TAG_TABS.equals(c.getTag())) continue;
            add(c, MEMBERS);
        }

        add(extra(inviteCard(groupId, g, isAdmin)), ACCESS);
        add(extra(membersCard(isAdmin, memberCount)), MEMBERS);
        add(extra(infoCard(groupId, g, onChain)), MORE);
        if (closeSection != null) { page.removeView(closeSection); page.addView(closeSection); add(closeSection, MORE); }
        add(extra(leaveCard()), MORE);
        show(selected);
    }

    private void add(View v, int section) { if (v == null) return; views.add(v); where.add(section); }

    private View extra(View v) { v.setTag(TAG_EXTRA); page.addView(v); return v; }

    private void show(int section) {
        selected = section;
        for (int i = 0; i < views.size(); i++) views.get(i).setVisibility(where.get(i) == section ? View.VISIBLE : View.GONE);
        for (int i = 0; i < chips.length; i++) if (chips[i] != null) paintChip(chips[i], i == section);
        if (scroller != null) scroller.scrollTo(0, 0);
    }

    // ── chip bar ──

    private View buildTabs() {
        HorizontalScrollView hs = new HorizontalScrollView(a);
        hs.setTag(TAG_TABS);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(a);
        row.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < NAMES.length; i++) {
            final int at = i;
            TextView c = new TextView(a);
            c.setText(NAMES[i]);
            c.setTextSize(13);
            c.setTypeface(null, Typeface.BOLD);
            c.setGravity(Gravity.CENTER);
            c.setPadding((int) (16 * d), 0, (int) (16 * d), 0);
            c.setOnClickListener(v -> show(at));
            chips[i] = c;
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, (int) (36 * d));
            lp.setMarginEnd((int) (8 * d));
            row.addView(c, lp);
            paintChip(c, i == selected);
        }
        hs.addView(row);
        hs.setPadding(0, 0, 0, (int) (4 * d));
        return hs;
    }

    private void paintChip(TextView c, boolean on) {
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(18 * d);
        g.setColor(on ? UiColors.accent() : (UiColors.accent() & 0x00FFFFFF) | 0x22000000);
        c.setBackground(g);
        c.setTextColor(on ? 0xFF1A1208 : UiColors.textPrimary());
    }

    // ── cards ──

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(a);
        c.setOrientation(LinearLayout.VERTICAL);
        int p = (int) (16 * d);
        c.setPadding(p, p, p, p);
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(16 * d);
        g.setColor(UiColors.surface());
        g.setStroke((int) d, (UiColors.textTertiary() & 0x00FFFFFF) | 0x33000000);
        c.setBackground(g);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = (int) (16 * d);
        c.setLayoutParams(lp);
        return c;
    }

    private TextView text(String s, float sp, boolean bold, int color) {
        TextView t = new TextView(a);
        t.setText(s); t.setTextSize(sp); t.setTextColor(color);
        if (bold) t.setTypeface(null, Typeface.BOLD);
        t.setPadding(0, (int) (3 * d), 0, (int) (3 * d));
        return t;
    }

    private TextView button(String label, boolean danger, View.OnClickListener l) {
        TextView b = new TextView(a);
        b.setText(label);
        b.setTextSize(14);
        b.setTypeface(null, Typeface.BOLD);
        b.setGravity(Gravity.CENTER);
        int fg = danger ? 0xFFE5484D : UiColors.accent();
        b.setTextColor(fg);
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(12 * d);
        g.setStroke((int) (1.5f * d), fg);
        b.setBackground(g);
        b.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (46 * d));
        lp.topMargin = (int) (10 * d);
        b.setLayoutParams(lp);
        return b;
    }

    private View inviteCard(String groupId, Map<String, Object> g, boolean isAdmin) {
        LinearLayout c = card();
        c.addView(text("Invite people", 16, true, UiColors.textPrimary()));
        boolean membersMay = Boolean.TRUE.equals(g.get("membersCanInvite"));
        c.addView(text(isAdmin || membersMay ? "Send an invitation by email, or share the group ID so someone can find it."
                : "Only the admin can invite people to this group. You can still share its ID.", 12.5f, false, UiColors.textSecondary()));
        if (isAdmin || membersMay) c.addView(button("Invite someone", false, v -> host.invite()));
        c.addView(button("Share group ID", false, v -> {
            android.content.Intent s = new android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain");
            s.putExtra(android.content.Intent.EXTRA_TEXT, "Join " + str(g, "groupName", "my group") + " on Harvana. Group ID: " + groupId);
            a.startActivity(android.content.Intent.createChooser(s, "Share group ID"));
        }));
        return c;
    }

    private View membersCard(boolean isAdmin, int count) {
        LinearLayout c = card();
        c.addView(text("Members", 16, true, UiColors.textPrimary()));
        c.addView(text(count > 0 ? count + (count == 1 ? " member" : " members") : "Members load from the group's Members tab.", 13, false, UiColors.textSecondary()));
        c.addView(text(isAdmin ? "You are the admin. You can hand the admin role to another member."
                : "The admin manages who joins and who is removed.", 12.5f, false, UiColors.textSecondary()));
        if (isAdmin) c.addView(button("Transfer admin role", false, v -> host.transferAdmin()));
        return c;
    }

    private View infoCard(String groupId, Map<String, Object> g, boolean onChain) {
        LinearLayout c = card();
        c.addView(text("About this group", 16, true, UiColors.textPrimary()));
        c.addView(row("Type", str(g, "groupType", "Savings group")));
        c.addView(row("Status", str(g, "status", "Active")));
        c.addView(row("Kept", onChain ? "On the Harvana network" : "In the app only"));
        long ts = 0;
        try { ts = Long.parseLong(str(g, "timestamp", "0")); } catch (Exception ignored) { }
        if (ts > 0) c.addView(row("Created", new SimpleDateFormat("d MMM yyyy", Locale.US).format(new Date(ts))));
        c.addView(row("Group ID", groupId));
        c.addView(button("Copy group ID", false, v -> {
            ClipboardManager cm = (ClipboardManager) a.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("group id", groupId));
            Toast.makeText(a, "Copied.", Toast.LENGTH_SHORT).show();
        }));
        c.addView(button("Read the contract", false, v -> showContract(groupId, g)));
        return c;
    }

    private View leaveCard() {
        LinearLayout c = card();
        c.addView(text("Leave", 16, true, 0xFFE5484D));
        c.addView(text("You lose access to this group's payments, loans and notices. The admin must hand over the admin role before leaving.", 12.5f, false, UiColors.textSecondary()));
        c.addView(button("Leave this group", true, v -> host.leave()));
        return c;
    }

    private View row(String k, String v) {
        LinearLayout r = new LinearLayout(a);
        r.setPadding(0, (int) (6 * d), 0, (int) (2 * d));
        TextView l = text(k, 13, false, UiColors.textSecondary());
        r.addView(l, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.9f));
        TextView val = text(v, 13, true, UiColors.textPrimary());
        val.setGravity(Gravity.END);
        r.addView(val, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.6f));
        return r;
    }

    /** The same text a new member signs, to read again at any time. */
    private void showContract(String groupId, Map<String, Object> g) {
        StringBuilder sb = new StringBuilder();
        for (GroupContract.Section s : GroupContract.sections(g, CoinInfo.ticker())) {
            sb.append(s.title.toUpperCase(Locale.US)).append('\n');
            for (String p : s.points) sb.append("• ").append(p).append('\n');
            sb.append('\n');
        }
        TextView t = text(sb.toString().trim(), 14, false, UiColors.textPrimary());
        int p = (int) (20 * d);
        t.setPadding(p, p / 2, p, p / 2);
        ScrollView sv = new ScrollView(a);
        sv.addView(t);
        new androidx.appcompat.app.AlertDialog.Builder(a).setTitle("Group contract").setView(sv).setPositiveButton("Close", null).show();
    }

    private static String str(Map<String, Object> m, String k, String fb) {
        Object o = m == null ? null : m.get(k);
        return o == null || String.valueOf(o).trim().isEmpty() ? fb : String.valueOf(o).trim();
    }
}
