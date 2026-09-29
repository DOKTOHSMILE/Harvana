package com.harvanna.app;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Locale;
import java.util.Map;

/**
 * Helper to bind GROUPS/{id} Firestore fields onto the redesigned Group UI.
 * Call from ActivityGroupDetailsActivity / OverviewFragmentActivity after load.
 */
public final class GroupUiBinder {

    private GroupUiBinder() {}

    public static void bindHeader(View root, Map<String, Object> group, int memberCount, boolean isAdmin) {
        if (root == null || group == null) return;

        setText(root, "tv_group_name", str(group, "groupName", "Group"));
        String freq = str(group, "frequency", "—");
        String slots = str(group, "totalSlots", str(group, "slots", "0"));
        String vis = str(group, "visibility", "Private");
        setText(root, "tv_group_meta", freq + " · " + slots + " slots · " + vis);

        double amount = num(group, "contributionAmount", num(group, "amount", 0));
        setText(root, "tv_header_amount", formatHarv(amount));
        setText(root, "tv_header_members", memberCount + " / " + slots);

        Object next = group.get("nextPayoutLabel");
        setText(root, "tv_header_next", next != null ? String.valueOf(next) : "—");

        View badge = root.findViewWithTag("tv_admin_badge");
        if (badge == null) {
            // fallback by id if inflated in activity
        }
        TextView adminBadge = findTv(root, "tv_admin_badge");
        if (adminBadge != null) {
            adminBadge.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
        }
    }

    public static void bindOverview(View root, Map<String, Object> group, int paidThisRound) {
        if (root == null || group == null) return;

        setText(root, "tv_overview_description",
                str(group, "groupDescription", str(group, "description", "No description yet.")));
        setText(root, "tv_rule_type", str(group, "groupType", str(group, "type", "—")));
        setText(root, "tv_rule_frequency", str(group, "frequency", "—"));
        String slots = str(group, "totalSlots", str(group, "slots", "0"));
        setText(root, "tv_rule_slots", slots);
        setText(root, "tv_rule_visibility", str(group, "visibility", "—"));

        int totalSlots = (int) num(group, "totalSlots", num(group, "slots", 0));
        int currentSlot = (int) num(group, "currentSlot", 1);
        if (totalSlots <= 0) totalSlots = 1;
        if (currentSlot < 1) currentSlot = 1;

        setText(root, "tv_cycle_status", "Slot " + currentSlot + " of " + totalSlots);
        int percent = Math.min(100, (int) ((currentSlot * 100.0) / totalSlots));
        setText(root, "tv_cycle_percent", percent + "%");

        View progress = root.findViewById(id(root, "view_cycle_progress"));
        if (progress != null && progress.getParent() instanceof View) {
            progress.post(() -> {
                View parent = (View) progress.getParent();
                int w = parent.getWidth();
                if (w > 0) {
                    ViewGroup.LayoutParams lp = progress.getLayoutParams();
                    lp.width = Math.max(8, (w * percent) / 100);
                    progress.setLayoutParams(lp);
                }
            });
        }

        double amount = num(group, "contributionAmount", num(group, "amount", 0));
        setText(root, "tv_pool_contribution", formatHarv(amount));
        setText(root, "tv_pool_total", formatHarv(amount * totalSlots));
        setText(root, "tv_pool_paid", paidThisRound + " / " + totalSlots);
    }

    public static void setAdminActionsVisible(View root, boolean isAdmin) {
        View actions = root.findViewById(id(root, "linear_admin_actions"));
        if (actions != null) actions.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
        TextView invite = findTv(root, "btn_members_invite");
        if (invite != null) invite.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
        TextView notice = findTv(root, "btn_new_notice");
        if (notice != null) notice.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private static String formatHarv(double v) {
        if (Math.abs(v - Math.rint(v)) < 0.0001) {
            return String.format(Locale.US, ("%.0f " + com.harvanna.app.CoinInfo.ticker()), v);
        }
        return com.harvanna.app.wallet.BalanceFormatter.smart(v);
    }

    private static String str(Map<String, Object> m, String key, String def) {
        Object v = m.get(key);
        if (v == null) return def;
        String s = String.valueOf(v).trim();
        return s.isEmpty() || "null".equalsIgnoreCase(s) ? def : s;
    }

    private static double num(Map<String, Object> m, String key, double def) {
        Object v = m.get(key);
        if (v instanceof Number) return ((Number) v).doubleValue();
        if (v != null) {
            try { return Double.parseDouble(String.valueOf(v)); } catch (Exception ignored) {}
        }
        return def;
    }

    private static void setText(View root, String idName, String value) {
        TextView tv = findTv(root, idName);
        if (tv != null) tv.setText(value != null ? value : "");
    }

    private static TextView findTv(View root, String idName) {
        int id = id(root, idName);
        if (id == 0) return null;
        View v = root.findViewById(id);
        return v instanceof TextView ? (TextView) v : null;
    }

    private static int id(View root, String name) {
        return root.getResources().getIdentifier(name, "id", root.getContext().getPackageName());
    }
}