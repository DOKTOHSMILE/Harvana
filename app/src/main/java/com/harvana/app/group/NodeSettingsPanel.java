package com.harvana.app.group;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.harvana.app.firestore.GroupFirestoreManager;
import com.harvana.app.sync.NodeGroupClient;
import com.harvana.app.utils.JsonMaps;
import com.harvana.app.utils.UiColors;
import com.harvana.app.utils.InfoTable;
import com.harvana.app.wallet.PinPromptHelper;
import com.harvana.app.wallet.WalletManager;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * The network half of Group Settings. A group has two halves that are read together: the Firestore profile (name, photo,
 * description, chat, notices) and the group on the network (members, money, rules, votes). The network is the authority for
 * everything about money and who is in the group. This panel shows what the network says, says where it differs from the
 * profile and fixes the profile from it, and carries the decisions that only the network can make: who decides, what starts
 * a vote, and a vote to replace an admin who is gone.
 */
public final class NodeSettingsPanel {
    private NodeSettingsPanel() {}

    /** Sign-out: nothing of the last account stays in memory. */
    public static void forgetAll() { synchronized (FACTS_CACHE) { FACTS_CACHE.clear(); } }

    public static void attach(Activity a, LinearLayout host, String firestoreGroupId, Map<String, Object> groupData) {
        final GroupIdentity id = GroupIdentityResolver.resolve(firestoreGroupId, groupData);
        final String me = WalletManager.getActiveWalletAddress(a);
        LinearLayout card = card(a);
        host.addView(card);
        card.addView(text(a, "Network control", 16, true, UiColors.textPrimary()));
        if (!id.onChain()) {
            card.addView(table(a).row("Network", "Not on the network").view());
            return;
        }
        final InfoTable facts = table(a);
        facts.view().setVisibility(View.GONE);
        final View factsLoading = com.harvana.app.utils.Shimmer.lines(a, 4);
        String cachedFacts = FACTS_CACHE.get(id.protocolId());
        if (cachedFacts != null) {               // seen before: show it straight away, refresh underneath
            fill(facts, cachedFacts);
            facts.view().setVisibility(View.VISIBLE);
            factsLoading.setVisibility(View.GONE);
        }
        card.addView(factsLoading);
        card.addView(facts.view());
        final LinearLayout actions = new LinearLayout(a);
        actions.setOrientation(LinearLayout.VERTICAL);
        card.addView(actions);

        final LinearLayout rules = card(a);
        host.addView(rules);
        rules.addView(text(a, "Votes", 16, true, UiColors.textPrimary()));
        rules.addView(rulesTable(a, id.family).view());

        final LinearLayout recovery = card(a);
        host.addView(recovery);
        recovery.addView(text(a, "Admin recovery", 16, true, UiColors.textPrimary()));

        NodeGroupClient.Callback onGroup = new NodeGroupClient.Callback() {
            @Override public void onSuccess(JSONObject json) {
                Map<String, Object> g = JsonMaps.toMap(json);
                a.runOnUiThread(() -> { factsLoading.setVisibility(View.GONE); facts.view().setVisibility(View.VISIBLE); showFacts(a, id, firestoreGroupId, groupData, g, me, facts, actions); });
                loadMembers(a, id, count -> a.runOnUiThread(() -> compare(a, id, firestoreGroupId, groupData, g, count, me, facts, actions)));
            }
            @Override public void onError(String e) { a.runOnUiThread(() -> { factsLoading.setVisibility(View.GONE); facts.view().setVisibility(View.VISIBLE); facts.clear().row("Network", "Not reachable", UiColors.warning()); }); }
        };
        if ("CIRCLE".equals(id.family)) NodeGroupClient.getCircle(id.protocolId(), onGroup);
        else if ("ACCUM".equals(id.family)) NodeGroupClient.getAccumGroup(id.protocolId(), onGroup);
        else NodeGroupClient.getFixedGroup(id.protocolId(), onGroup);

        NodeGroupClient.getAdminRecovery(id.protocolId(), new NodeGroupClient.Callback() {
            @Override public void onSuccess(JSONObject j) { a.runOnUiThread(() -> showRecovery(a, id, recovery, JsonMaps.toMap(j), me)); }
            @Override public void onError(String e) { a.runOnUiThread(() -> showRecovery(a, id, recovery, null, me)); }
        });
    }

    // ── facts and sync ─────────────────────────────────────────────────────

    private interface CountCb { void got(int count); }

    private static void loadMembers(Activity a, GroupIdentity id, CountCb cb) {
        NodeGroupClient.Callback c = new NodeGroupClient.Callback() {
            @Override public void onSuccess(JSONObject j) {
                int n = j.optInt("count", j.optJSONArray("members") == null ? -1 : j.optJSONArray("members").length());
                cb.got(n);
            }
            @Override public void onError(String e) { cb.got(-1); }
        };
        if ("CIRCLE".equals(id.family)) NodeGroupClient.getCircleMembers(id.protocolId(), c);
        else if ("ACCUM".equals(id.family)) NodeGroupClient.getAccumMembers(id.protocolId(), c);
        else NodeGroupClient.getFixedMembers(id.protocolId(), c);
    }

    /** The last facts shown for each group, so coming back to the panel shows them at once instead of a placeholder. */
    private static final java.util.Map<String, String> FACTS_CACHE = new java.util.HashMap<>();

    private static void showFacts(Activity a, GroupIdentity id, String fsId, Map<String, Object> profile, Map<String, Object> g, String me,
                                  InfoTable facts, LinearLayout actions) {
        String state = str(g.get("state"), str(g.get("status"), "—"));
        String admin = str(g.get("creatorAddress"), "—");
        String escrow = str(g.get("escrowAddress"), "—");
        StringBuilder b = new StringBuilder();
        line(b, "Network id", id.protocolId());
        line(b, "Kind", familyName(id.family));
        line(b, "State", state);
        if (g.get("disbursementRule") != null) line(b, "Money leaves", String.valueOf(g.get("disbursementRule")).replace('_', ' ').toLowerCase());
        line(b, "Group wallet", short_(escrow));
        line(b, "Admin", short_(admin) + (admin.equalsIgnoreCase(me) ? " (you)" : ""));
        fill(facts, b.toString());
        FACTS_CACHE.put(id.protocolId(), b.toString());
        actions.removeAllViews();
        actions.addView(button(a, "Copy network id", false, v -> NodePayActions.copyId(a, id.protocolId())));
        if (!"—".equals(escrow)) actions.addView(button(a, "Copy group wallet", false, v -> NodePayActions.copyId(a, escrow)));
        if (!"FIXED".equals(com.harvana.app.sync.NodeGroupClient.familyOf(id.protocolId()))) actions.addView(button(a, "Open actions and votes", true, v -> {
            Intent i = new Intent(a, GroupActionsActivity.class);
            i.putExtra("nodeGroupId", id.protocolId());
            i.putExtra("firestoreGroupId", fsId);
            a.startActivity(i);
        }));
    }

    /** Compares the profile with the network and, for the admin, brings the profile up to date. */
    private static void compare(Activity a, GroupIdentity id, String fsId, Map<String, Object> profile, Map<String, Object> g, int networkMembers,
                                String me, InfoTable facts, LinearLayout actions) {
        if (networkMembers < 0) return;
        Object fs = profile.get("membersCount");
        int profileMembers = fs instanceof Number ? ((Number) fs).intValue() : -1;
        boolean admin = str(g.get("creatorAddress"), "").equalsIgnoreCase(me);
        boolean differs = profileMembers != networkMembers;
        facts.row("Members", networkMembers + (differs ? "  ·  app " + (profileMembers < 0 ? "—" : String.valueOf(profileMembers)) : "  ·  in step"),
                differs ? UiColors.warning() : UiColors.success());
        if (differs && admin) {
            actions.addView(button(a, "Update the profile from the network", false, v -> {
                Map<String, Object> patch = new HashMap<>();
                patch.put("membersCount", networkMembers);
                patch.put("protocolState", str(g.get("state"), ""));
                if (g.get("raisedAmountRaw") instanceof Number) patch.put("raisedAmountRaw", ((Number) g.get("raisedAmountRaw")).longValue());
                if (g.get("escrowAddress") != null) patch.put("groupWalletAddress", String.valueOf(g.get("escrowAddress")));
                GroupFirestoreManager.getInstance(a).updateGroupFields(fsId, patch, new GroupFirestoreManager.SimpleCallback() {
                    @Override public void onSuccess() { a.runOnUiThread(() -> Toast.makeText(a, "Profile updated from the network.", Toast.LENGTH_SHORT).show()); }
                    @Override public void onError(String e) { a.runOnUiThread(() -> Toast.makeText(a, "Could not update the profile: " + e, Toast.LENGTH_LONG).show()); }
                });
            }));
        }
    }

    // ── rows ───────────────────────────────────────────────────────────────

    /** A table inside a card (the card already has the padding and the fill). */
    private static InfoTable table(Activity a) {
        InfoTable t = new InfoTable(a);
        t.view().setPadding(0, dp(a, 4), 0, 0);
        t.view().setBackground(null);
        return t;
    }

    private static void line(StringBuilder b, String label, String value) {
        if (b.length() > 0) b.append('\n');
        b.append(label).append('\t').append(value);
    }

    /** Rows from "label\tvalue" lines (the form kept in the cache). */
    private static void fill(InfoTable t, String lines) {
        t.clear();
        for (String l : lines.split("\n")) {
            int tab = l.indexOf('\t');
            if (tab > 0) t.row(l.substring(0, tab), l.substring(tab + 1));
        }
    }

    /** Who votes and what starts a vote, as rows. */
    static InfoTable rulesTable(Activity a, String family) {
        InfoTable t = table(a);
        t.section("Starts a vote");
        if ("FIXED".equals(family)) {
            t.row("Payout", "Admin asks · group votes");
            t.row("Milestone release", "Admin asks · group votes");
            t.row("Emergency grant", "Member asks · group votes");
            t.row("All at once", "No vote · paid at goal");
            t.row("Admin's request", "Counts as admin's yes");
        } else if ("ACCUM".equals(family)) {
            t.row("Early withdrawal", "Admin approves · no vote");
        } else {
            t.row("Payouts", "No vote · by schedule");
            t.row("Missed payment", "Covered by bonds");
        }
        t.row("Admin recovery", "Vote · 14 days");
        t.section("Weights");
        t.row("Admin", "40% (shared with treasurer)");
        t.row("Members", "60% · top 5 by deposits");
        t.section("Passing");
        t.row("Yes needed", "51%");
        t.row("Yes votes", "2 or more, one not the asker");
        t.row("Voters", "Paid in once · joined 48 h+");
        t.row("Voter list", "Fixed when the vote starts");
        t.row("Changing a vote", "Until decided");
        return t;
    }

    // ── admin recovery ─────────────────────────────────────────────────────

    private static void showRecovery(Activity a, GroupIdentity id, LinearLayout box, Map<String, Object> rec, String me) {
        InfoTable info = table(a);
        box.addView(info.view());
        boolean pending = rec != null && "PENDING".equals(String.valueOf(rec.get("status")));
        if (!pending) {
            info.row("Open request", "None");
            info.row("Vote length", "14 days");
            box.addView(button(a, "Ask to take over as admin", false, v -> PinPromptHelper.authorize(a, "Ask to become this group's admin", () ->
                    NodeGroupClient.requestAdminRecovery(a, me, id.protocolId(), me, done(a, "Your request is open. The voters decide.")))));
            return;
        }
        String next = String.valueOf(rec.get("newAdminAddress"));
        long yes = 0, total = 0;
        boolean eligible = false;
        Object weights = rec.get("voterWeights"), votes = rec.get("votes");
        if (weights instanceof Map) {
            for (Map.Entry<?, ?> e : ((Map<?, ?>) weights).entrySet()) {
                long w = e.getValue() instanceof Number ? ((Number) e.getValue()).longValue() : 0;
                total += w;
                if (votes instanceof Map && Boolean.TRUE.equals(((Map<?, ?>) votes).get(e.getKey()))) yes += w;
                if (String.valueOf(e.getKey()).equalsIgnoreCase(me)) eligible = true;
            }
        }
        info.row("Asked to be admin", short_(next));
        info.row("Yes", (total == 0 ? 0 : yes * 100 / total) + "% of 51%");
        info.row("You", eligible ? "Can vote" : "Not a voter");
        if (eligible) {
            box.addView(button(a, "Vote yes", true, v -> PinPromptHelper.authorize(a, "Vote to change this group's admin", () ->
                    NodeGroupClient.voteAdminRecovery(a, me, id.protocolId(), true, done(a, "Voted yes")))));
            box.addView(button(a, "Vote no", false, v -> PinPromptHelper.authorize(a, "Vote against changing this group's admin", () ->
                    NodeGroupClient.voteAdminRecovery(a, me, id.protocolId(), false, done(a, "Voted no")))));
        }
    }

    private static NodeGroupClient.Callback done(Activity a, String ok) {
        return new NodeGroupClient.Callback() {
            @Override public void onSuccess(JSONObject j) { a.runOnUiThread(() -> Toast.makeText(a, ok, Toast.LENGTH_LONG).show()); }
            @Override public void onError(String e) { a.runOnUiThread(() -> Toast.makeText(a, com.harvana.app.circle.CircleUi.friendly(e), Toast.LENGTH_LONG).show()); }
        };
    }

    // ── small views ────────────────────────────────────────────────────────

    private static String familyName(String f) {
        return "CIRCLE".equals(f) ? "Rotating circle" : "ACCUM".equals(f) ? "Accumulative savings" : "Fixed goal";
    }

    private static String str(Object o, String d) { return o == null || String.valueOf(o).isEmpty() ? d : String.valueOf(o); }

    private static String short_(String s) { return s != null && s.length() > 14 ? s.substring(0, 8) + "…" + s.substring(s.length() - 4) : s; }

    private static int dp(Activity a, float v) { return (int) (v * a.getResources().getDisplayMetrics().density); }

    private static TextView text(Activity a, String s, float sp, boolean bold, int color) {
        TextView t = new TextView(a);
        t.setText(s); t.setTextSize(sp); t.setTextColor(color);
        if (bold) t.setTypeface(null, Typeface.BOLD);
        return t;
    }

    private static MaterialButton button(Activity a, String label, boolean primary, android.view.View.OnClickListener l) {
        MaterialButton b = primary ? new MaterialButton(a) : new MaterialButton(a, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        b.setText(label); b.setAllCaps(false); b.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(a, 8);
        b.setLayoutParams(lp);
        return b;
    }

    private static LinearLayout card(Activity a) {
        LinearLayout l = new LinearLayout(a);
        l.setOrientation(LinearLayout.VERTICAL);
        int p = dp(a, 14);
        l.setPadding(p, p, p, p);
        GradientDrawable g = new GradientDrawable();
        g.setColor(UiColors.surface()); g.setCornerRadius(dp(a, 14));
        g.setStroke(dp(a, 1), (UiColors.textTertiary() & 0x00FFFFFF) | 0x33000000);
        l.setBackground(g);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(a, 12);
        l.setLayoutParams(lp);
        return l;
    }
}
