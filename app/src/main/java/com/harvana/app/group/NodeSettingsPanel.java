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

    public static void attach(Activity a, LinearLayout host, String firestoreGroupId, Map<String, Object> groupData) {
        final GroupIdentity id = GroupIdentityResolver.resolve(firestoreGroupId, groupData);
        final String me = WalletManager.getActiveWalletAddress(a);
        LinearLayout card = card(a);
        host.addView(card);
        card.addView(text(a, "Network control", 16, true, UiColors.textPrimary()));
        if (!id.onChain()) {
            card.addView(text(a, "This group is not on the network. Its money and members are kept in the app only, so there are no network settings to show.",
                    12, false, UiColors.textSecondary()));
            return;
        }
        card.addView(text(a, "The network holds this group's money and says who is in it. The profile (name, photo, description, chat, notices) is kept in the app. "
                + "Both are read together, and where they differ the network wins.", 12, false, UiColors.textSecondary()));
        final TextView facts = text(a, "Reading the network…", 13, false, UiColors.textPrimary());
        facts.setPadding(0, dp(a, 8), 0, dp(a, 4));
        facts.setVisibility(View.GONE);
        final View factsLoading = com.harvana.app.utils.Shimmer.lines(a, 4);
        String cachedFacts = FACTS_CACHE.get(id.protocolId());
        if (cachedFacts != null) {               // seen before: show it straight away, refresh underneath
            facts.setText(cachedFacts);
            facts.setTypeface(Typeface.MONOSPACE);
            facts.setVisibility(View.VISIBLE);
            factsLoading.setVisibility(View.GONE);
        }
        card.addView(factsLoading);
        card.addView(facts);
        final LinearLayout actions = new LinearLayout(a);
        actions.setOrientation(LinearLayout.VERTICAL);
        card.addView(actions);

        final LinearLayout rules = card(a);
        host.addView(rules);
        rules.addView(text(a, "Who decides, and what starts a vote", 16, true, UiColors.textPrimary()));
        rules.addView(text(a, rulesText(id.family), 12.5f, false, UiColors.textSecondary()));

        final LinearLayout recovery = card(a);
        host.addView(recovery);
        recovery.addView(text(a, "Admin recovery", 16, true, UiColors.textPrimary()));

        NodeGroupClient.Callback onGroup = new NodeGroupClient.Callback() {
            @Override public void onSuccess(JSONObject json) {
                Map<String, Object> g = JsonMaps.toMap(json);
                a.runOnUiThread(() -> { factsLoading.setVisibility(View.GONE); facts.setVisibility(View.VISIBLE); showFacts(a, id, firestoreGroupId, groupData, g, me, facts, actions); });
                loadMembers(a, id, count -> a.runOnUiThread(() -> compare(a, id, firestoreGroupId, groupData, g, count, me, actions)));
            }
            @Override public void onError(String e) { a.runOnUiThread(() -> { factsLoading.setVisibility(View.GONE); facts.setVisibility(View.VISIBLE); facts.setText("Could not read the network right now. Pull to refresh later."); }); }
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
                                  TextView facts, LinearLayout actions) {
        String state = str(g.get("state"), str(g.get("status"), "—"));
        String admin = str(g.get("creatorAddress"), "—");
        String escrow = str(g.get("escrowAddress"), "—");
        StringBuilder b = new StringBuilder();
        b.append("Network id  ").append(id.protocolId());
        b.append("\nKind  ").append(familyName(id.family));
        b.append("\nState  ").append(state);
        if (g.get("disbursementRule") != null) b.append("\nHow money leaves  ").append(String.valueOf(g.get("disbursementRule")).replace('_', ' ').toLowerCase());
        b.append("\nGroup wallet  ").append(short_(escrow));
        b.append("\nAdmin  ").append(short_(admin)).append(admin.equalsIgnoreCase(me) ? " (you)" : "");
        facts.setText(b.toString());
        facts.setTypeface(Typeface.MONOSPACE);
        FACTS_CACHE.put(id.protocolId(), b.toString());
        actions.removeAllViews();
        actions.addView(button(a, "Copy network id", false, v -> NodePayActions.copyId(a, id.protocolId())));
        if (!"—".equals(escrow)) actions.addView(button(a, "Copy group wallet", false, v -> NodePayActions.copyId(a, escrow)));
        actions.addView(button(a, "Open actions and votes", true, v -> {
            Intent i = new Intent(a, NodeGroupActivity.class);
            i.putExtra("nodeGroupId", id.protocolId());
            i.putExtra("firestoreGroupId", fsId);
            a.startActivity(i);
        }));
    }

    /** Compares the profile with the network and, for the admin, brings the profile up to date. */
    private static void compare(Activity a, GroupIdentity id, String fsId, Map<String, Object> profile, Map<String, Object> g, int networkMembers,
                                String me, LinearLayout actions) {
        if (networkMembers < 0) return;
        Object fs = profile.get("membersCount");
        int profileMembers = fs instanceof Number ? ((Number) fs).intValue() : -1;
        boolean admin = str(g.get("creatorAddress"), "").equalsIgnoreCase(me);
        TextView line = text(a, "", 12.5f, false, UiColors.textSecondary());
        line.setPadding(0, dp(a, 8), 0, 0);
        boolean differs = profileMembers != networkMembers;
        line.setText(differs
                ? "Out of step: the profile says " + (profileMembers < 0 ? "no member count" : profileMembers + " members")
                    + ", the network says " + networkMembers + "."
                : "In step: the profile and the network agree on " + networkMembers + " members.");
        actions.addView(line);
        if (differs && admin) {
            actions.addView(button(a, "Update the profile from the network", false, v -> {
                Map<String, Object> patch = new HashMap<>();
                patch.put("membersCount", networkMembers);
                patch.put("protocolState", str(g.get("state"), ""));
                if (g.get("raisedAmountRaw") instanceof Number) patch.put("raisedAmountRaw", ((Number) g.get("raisedAmountRaw")).longValue());
                if (g.get("escrowAddress") != null) patch.put("groupWalletAddress", String.valueOf(g.get("escrowAddress")));
                GroupFirestoreManager.getInstance(a).updateGroupFields(fsId, patch, new GroupFirestoreManager.SimpleCallback() {
                    @Override public void onSuccess() { a.runOnUiThread(() -> { Toast.makeText(a, "Profile updated from the network.", Toast.LENGTH_SHORT).show(); line.setText("In step."); }); }
                    @Override public void onError(String e) { a.runOnUiThread(() -> Toast.makeText(a, "Could not update the profile: " + e, Toast.LENGTH_LONG).show()); }
                });
            }));
        }
    }

    // ── rules text ─────────────────────────────────────────────────────────

    static String rulesText(String family) {
        String common = "When a vote is needed, the admin counts for 40% (25% if the group has a treasurer, who counts 15%) and the other members share 60% (the five who have put in the most, or everyone if fewer). "
                + "A vote passes at 51% yes, with at least two yes votes and one of them from someone other than the person who asked. "
                + "Only members who have paid in at least once and joined 48 hours ago can vote, and the voters are fixed when the vote starts. "
                + "You can change your vote until it is decided.";
        String starts;
        if ("FIXED".equals(family)) {
            starts = "What starts a vote:\n"
                    + "• Admin asks for a payout (rule: admin asks, the group votes).\n"
                    + "• Admin asks for a milestone release (rule: milestones).\n"
                    + "• A member asks for an emergency grant (rule: emergency grants).\n"
                    + "• Someone takes over from an admin who is gone (admin recovery).\n"
                    + "With the all-at-once rule nobody votes: the whole pot goes to the wallet chosen when the group was created, when the goal is reached.\n"
                    + "The admin's request counts as the admin's yes vote.";
        } else if ("ACCUM".equals(family)) {
            starts = "What starts a decision:\n"
                    + "• A member asks to take their savings out early. The admin alone approves or not, and the network pays it out when they approve.\n"
                    + "• Someone takes over from an admin who is gone (admin recovery, a vote).\n"
                    + "Nothing else needs a vote.";
        } else {
            starts = "What starts a decision:\n"
                    + "• Nothing about money is voted. Each round's payout goes to the next member on the schedule, and bonds cover missed payments.\n"
                    + "• Someone takes over from an admin who is gone (admin recovery, a vote).";
        }
        return starts + "\n\n" + common;
    }

    // ── admin recovery ─────────────────────────────────────────────────────

    private static void showRecovery(Activity a, GroupIdentity id, LinearLayout box, Map<String, Object> rec, String me) {
        TextView info = text(a, "", 12.5f, false, UiColors.textSecondary());
        box.addView(info);
        boolean pending = rec != null && "PENDING".equals(String.valueOf(rec.get("status")));
        if (!pending) {
            info.setText("Use this only if the admin is gone or has lost their phone. A member asks to take over, and the voters decide. "
                    + "The vote lasts 14 days.");
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
        info.setText(short_(next) + " asked to become admin. Yes " + (total == 0 ? 0 : yes * 100 / total) + "% of the vote, 51% needed. "
                + (eligible ? "You can vote." : "You are not one of the voters."));
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
