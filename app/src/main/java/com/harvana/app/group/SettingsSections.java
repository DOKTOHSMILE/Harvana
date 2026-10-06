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
        void makeAdmin(String uid, String name);
        void remove(String uid, String name);
        /** make = true names this member treasurer; false clears the role. */
        void treasurer(String uid, String name, String wallet, boolean make);
        /** Any member: ask the group to vote a treasurer in (wallet) or the current one out (empty wallet). */
        void proposeTreasurer(String name, String wallet);
        /** Cast a ballot on the open treasurer proposal. */
        void voteTreasurer(boolean yes);
    }

    /** The treasurer's wallet as the network knows it, "" when none. Set by the page before apply(). */
    public String treasurerWallet = "";
    /** True for a goal or savings group on the network, the only kind that can have a treasurer. */
    public boolean canHaveTreasurer;
    /** The open treasurer proposal from the network, or null. */
    public org.json.JSONObject treasurerProposal;
    /** This phone's wallet, to know whether it may vote. */
    public String myWallet = "";

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
    public void apply(String groupId, Map<String, Object> g, boolean isAdmin, int memberCount, boolean onChain, java.util.List<Map<String, Object>> members,
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
        if (canHaveTreasurer) add(extra(treasurerCard(isAdmin)), MEMBERS);
        add(extra(memberListCard(g, isAdmin, members)), MEMBERS);
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

    /** The optional treasurer: what it is, who has it, and the open proposal to add or remove one. */
    private View treasurerCard(boolean isAdmin) {
        LinearLayout c = card();
        c.addView(text("Treasurer (optional)", 16, true, UiColors.textPrimary()));
        c.addView(text(treasurerWallet.isEmpty()
                ? "This group has no treasurer. That is fine: the admin handles money requests. A treasurer can be added if the admin decides to, or if a member proposes one and the group, including the admin, votes yes."
                : "This group has a treasurer. The admin can remove the role at any time, or any member can propose removing it for the group to vote on.",
                12.5f, false, UiColors.textSecondary()));
        org.json.JSONObject p = treasurerProposal;
        if (p != null && "PENDING".equals(p.optString("status"))) {
            String target = p.optString("treasurerAddress", "");
            org.json.JSONObject votes = p.optJSONObject("votes");
            org.json.JSONObject weights = p.optJSONObject("voterWeights");
            int yes = 0, no = 0;
            if (votes != null) for (java.util.Iterator<String> it = votes.keys(); it.hasNext(); ) { if (votes.optBoolean(it.next())) yes++; else no++; }
            TextView t = text((target.isEmpty() ? "Open vote: remove the treasurer" : "Open vote: make " + shortWallet(target) + " treasurer")
                    + "\nYes " + yes + " \u00b7 No " + no + (p.optString("cosignBy").isEmpty() ? "" : "\nAdding a treasurer also needs the admin's yes."),
                    13.5f, true, UiColors.textPrimary());
            c.addView(t);
            boolean mayVote = weights != null && !myWallet.isEmpty() && weights.has(myWallet);
            if (mayVote) {
                c.addView(button("Vote yes", false, v -> host.voteTreasurer(true)));
                c.addView(button("Vote no", true, v -> host.voteTreasurer(false)));
            } else {
                c.addView(text("Only the admin and the group's most active members vote.", 12, false, UiColors.textTertiary()));
            }
        } else if (!treasurerWallet.isEmpty()) {
            c.addView(button("Propose removing the treasurer", true, v -> host.proposeTreasurer("", "")));
        }
        return c;
    }

    private static String shortWallet(String w) { return w.length() > 14 ? w.substring(0, 8) + "\u2026" + w.substring(w.length() - 4) : w; }

    // ── member list with roles and permissions ──

    private static final long VOTE_WAIT_MS = 48L * 3_600_000L;

    private View memberListCard(Map<String, Object> g, boolean isAdmin, java.util.List<Map<String, Object>> members) {
        LinearLayout c = card();
        c.addView(text("Roles and permissions", 16, true, UiColors.textPrimary()));
        c.addView(text("Tap a member to see what they can do. These come from the group's rules; they are not switches.", 12.5f, false, UiColors.textSecondary()));
        String adminUid = str(g, "creatorId", "");
        String myUid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() == null ? ""
                : com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
        java.util.List<Map<String, Object>> rows = new ArrayList<>();
        if (members != null) for (Map<String, Object> m : members) {
            String st = str(m, "status", "ACTIVE");
            if ("ACTIVE".equalsIgnoreCase(st)) rows.add(m);
        }
        java.util.Collections.sort(rows, (x, y) -> {
            boolean ax = str(x, "memberUid", "").equals(adminUid), ay = str(y, "memberUid", "").equals(adminUid);
            if (ax != ay) return ax ? -1 : 1;
            return str(x, "displayName", str(x, "name", "")).compareToIgnoreCase(str(y, "displayName", str(y, "name", "")));
        });
        if (rows.isEmpty()) { c.addView(text("No active members loaded yet.", 13, false, UiColors.textSecondary())); return c; }
        for (Map<String, Object> m : rows) c.addView(memberRow(g, m, adminUid, myUid, isAdmin));
        return c;
    }

    private View memberRow(Map<String, Object> g, Map<String, Object> m, String adminUid, String myUid, boolean viewerIsAdmin) {
        final String uid = str(m, "memberUid", "");
        final String name = str(m, "displayName", str(m, "name", "Member"));
        final boolean admin = !uid.isEmpty() && uid.equals(adminUid);
        final String walletOf = str(m, "walletAddress", str(m, "wallet", ""));
        final boolean treas = !treasurerWallet.isEmpty() && treasurerWallet.equalsIgnoreCase(walletOf);
        LinearLayout r = new LinearLayout(a);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(0, (int) (10 * d), 0, (int) (10 * d));
        TextView face = new TextView(a);
        face.setText(name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase(Locale.US));
        face.setGravity(Gravity.CENTER);
        face.setTypeface(null, Typeface.BOLD);
        face.setTextColor(UiColors.accent());
        GradientDrawable disc = new GradientDrawable();
        disc.setShape(GradientDrawable.OVAL);
        disc.setColor((UiColors.accent() & 0x00FFFFFF) | 0x33000000);
        face.setBackground(disc);
        r.addView(face, new LinearLayout.LayoutParams((int) (40 * d), (int) (40 * d)));
        LinearLayout mid = new LinearLayout(a);
        mid.setOrientation(LinearLayout.VERTICAL);
        mid.setPadding((int) (12 * d), 0, (int) (8 * d), 0);
        mid.addView(text(name + (uid.equals(myUid) ? " (you)" : ""), 14.5f, true, UiColors.textPrimary()));
        int slot = (int) num(m, "slot");
        String sub = (admin ? "Admin" : treas ? "Treasurer" : "Member") + (slot > 0 && str(g, "groupType", "").toLowerCase(Locale.US).contains("rotating") ? " \u00b7 Slot " + slot : "");
        mid.addView(text(sub, 12, false, UiColors.textSecondary()));
        r.addView(mid, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView badge = text(admin ? "ADMIN" : treas ? "TREASURER" : "MEMBER", 11, true, admin || treas ? 0xFF1A1208 : UiColors.textSecondary());
        badge.setPadding((int) (10 * d), (int) (4 * d), (int) (10 * d), (int) (4 * d));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(12 * d);
        bg.setColor(admin || treas ? UiColors.accent() : (UiColors.textTertiary() & 0x00FFFFFF) | 0x22000000);
        badge.setBackground(bg);
        r.addView(badge);
        r.setClickable(true);
        r.setOnClickListener(v -> showMember(g, m, uid, name, admin, treas, walletOf, uid.equals(myUid), viewerIsAdmin));
        return r;
    }

    private void showMember(Map<String, Object> g, Map<String, Object> m, final String uid, final String name, boolean admin,
                            final boolean treasurer, final String wallet, boolean me, boolean viewerIsAdmin) {
        String type = str(g, "groupType", "").toLowerCase(Locale.US);
        boolean fixed = type.contains("fixed"), accum = type.contains("accumul"), rotating = type.contains("rotating");
        boolean membersInvite = Boolean.TRUE.equals(g.get("membersCanInvite"));
        long joined = 0;
        Object ja = m.get("joinedAt");
        if (ja instanceof Number) joined = ((Number) ja).longValue();
        boolean waited = joined > 0 && System.currentTimeMillis() - joined >= VOTE_WAIT_MS;
        StringBuilder sb = new StringBuilder();
        sb.append(admin ? "Role: Admin\n" : treasurer ? "Role: Treasurer\n" : "Role: Member\n");
        if (joined > 0) sb.append("Joined: ").append(new SimpleDateFormat("d MMM yyyy", Locale.US).format(new Date(joined))).append("\n");
        if (!wallet.isEmpty()) sb.append("Wallet: ").append(wallet.length() > 14 ? wallet.substring(0, 8) + "\u2026" + wallet.substring(wallet.length() - 4) : wallet).append("\n");
        sb.append("\nWHAT THIS PERSON CAN DO\n");
        sb.append(yes(true)).append("Pay into the group from their own wallet\n");
        sb.append(yes(admin || membersInvite)).append("Invite people").append(admin || membersInvite ? "" : " (the admin has switched this off for members)").append("\n");
        if (fixed || accum) {
            sb.append(yes(waited)).append("Vote on big decisions (needs one payment and 48 hours in the group")
              .append(joined > 0 ? waited ? "; 48 hours have passed)" : "; 48 hours have not passed yet)" : ")").append("\n");
        }
        if (admin) {
            sb.append(yes(true)).append("Accept, remove and block members\n");
            sb.append(yes(true)).append("Change the group profile and settings\n");
            if (fixed || accum) sb.append(yes(true)).append("Ask for payouts, milestones and approve withdrawals (the group still votes where the rules say so)\n");
            sb.append(yes(true)).append("Hand the admin role to another member\n");
            sb.append(yes(true)).append("Close the group\n");
            sb.append("\nThe admin's vote counts 40% on big decisions (shared with the treasurer when there is one: the admin always holds more). The admin cannot take money from anyone's wallet, and cannot move the group wallet without the group's rules.\n");
        } else {
            if (treasurer) {
                if (accum) sb.append(yes(true)).append("Approve members' withdrawal requests (the money still goes only to the member it belongs to)\n");
                if (fixed) sb.append(yes(true)).append("Ask for payouts and milestone releases (the group votes, and the admin must vote yes too before anything is paid)\n");
                sb.append("\nThe treasurer has no extra vote and cannot change settings, the admin seat, or who is in the group.\n");
            }
            sb.append(no()).append("Remove members, change settings or close the group (admin only)\n");
            if (accum) sb.append(yes(true)).append("Save in their own line and ask to withdraw it (the admin approves)\n");
            if (rotating) sb.append(yes(true)).append("Receive the pot on their turn\n");
            if (fixed) sb.append(no()).append("Take their deposit out alone (the group votes)\n");
        }
        TextView t = text(sb.toString().trim(), 14, false, UiColors.textPrimary());
        int p = (int) (20 * d);
        t.setPadding(p, p / 2, p, p / 2);
        final com.harvana.app.ui.SheetDialog[] dlg = new com.harvana.app.ui.SheetDialog[1];
        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        box.addView(t);
        if (!admin && !uid.isEmpty() && canHaveTreasurer && !wallet.isEmpty() && !me) {
            if (viewerIsAdmin) {
                TextView tb = button(treasurer ? "Remove as treasurer" : "Make treasurer (you decide)", false, v -> { if (dlg[0] != null) dlg[0].dismiss(); host.treasurer(uid, name, wallet, !treasurer); });
                ((LinearLayout.LayoutParams) tb.getLayoutParams()).setMargins(p, ((LinearLayout.LayoutParams) tb.getLayoutParams()).topMargin, p, 0);
                box.addView(tb);
            }
            if (!treasurer) {
                TextView pb = button("Propose as treasurer (the group votes)", false, v -> { if (dlg[0] != null) dlg[0].dismiss(); host.proposeTreasurer(name, wallet); });
                ((LinearLayout.LayoutParams) pb.getLayoutParams()).setMargins(p, ((LinearLayout.LayoutParams) pb.getLayoutParams()).topMargin, p, 0);
                box.addView(pb);
            }
        }
        ScrollView sv = new ScrollView(a);
        sv.addView(box);
        com.harvana.app.ui.SheetDialog.Builder b = new com.harvana.app.ui.SheetDialog.Builder(a).setTitle(name).setView(sv)
            .setPositiveButton("Close", null);
        if (viewerIsAdmin && !me && !admin && !uid.isEmpty()) {
            b.setNeutralButton("Make admin", (dl, w) -> host.makeAdmin(uid, name));
            b.setNegativeButton("Remove", (dl, w) -> host.remove(uid, name));
        }
        dlg[0] = b.create();
        dlg[0].show();
    }

    private static String yes(boolean on) { return on ? "\u2705  " : "\u274C  "; }
    private static String no() { return "\u274C  "; }

    private static double num(Map<String, Object> m, String k) {
        Object o = m == null ? null : m.get(k);
        if (o instanceof Number) return ((Number) o).doubleValue();
        try { return o == null ? 0 : Double.parseDouble(String.valueOf(o)); } catch (Exception e) { return 0; }
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
        new com.harvana.app.ui.SheetDialog.Builder(a).setTitle("Group contract").setView(sv).setPositiveButton("Close", null).show();
    }

    private static String str(Map<String, Object> m, String k, String fb) {
        Object o = m == null ? null : m.get(k);
        return o == null || String.valueOf(o).trim().isEmpty() ? fb : String.valueOf(o).trim();
    }
}
