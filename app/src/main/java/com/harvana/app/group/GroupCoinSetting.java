package com.harvana.app.group;

import android.app.Activity;
import android.text.InputType;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.harvana.app.husd.Stable;
import com.harvana.app.utils.UiColors;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The group's coin in Group settings: one row, "Group coin · hNGN". The creator can change it while nothing is in
 * the group (the node refuses once anyone has joined or saved). A rotating group's contribution and a goal group's
 * goal are entered again in the new coin.
 */
public final class GroupCoinSetting {
    private GroupCoinSetting() {}

    /** Adds the row to {@code host} at {@code index}; returns it, or null for a group not on the network. */
    public static android.view.View attach(Activity a, android.view.ViewGroup host, int index, String groupId, Map<String, Object> groupData, boolean isCreator) {
        GroupIdentity gi = GroupIdentityResolver.resolve(groupId, groupData);
        if (!gi.onChain()) return null;
        Object asset = groupData == null ? null : groupData.get("asset");
        String coin = asset == null ? "" : String.valueOf(asset);
        float d = a.getResources().getDisplayMetrics().density;
        LinearLayout row = new LinearLayout(a);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight((int) (52 * d));
        row.setPadding((int) (16 * d), 0, (int) (16 * d), 0);
        TextView label = new TextView(a);
        label.setText("Group coin");
        label.setTextSize(15);
        label.setTextColor(UiColors.textPrimary());
        row.addView(label, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView value = new TextView(a);
        value.setText(Stable.isStable(coin) ? Stable.symbol(coin) + " · " + Stable.name(coin) : "HARV");
        value.setTextSize(15);
        value.setTypeface(null, android.graphics.Typeface.BOLD);
        value.setTextColor(isCreator ? UiColors.accent() : UiColors.textSecondary());
        row.addView(value);
        if (isCreator) {
            android.util.TypedValue tv = new android.util.TypedValue();
            a.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, tv, true);
            row.setBackgroundResource(tv.resourceId);
            row.setOnClickListener(v -> pick(a, gi, groupId, coin, value));
        }
        host.addView(row, Math.max(0, Math.min(index, host.getChildCount())));
        return row;
    }

    private static void pick(Activity a, GroupIdentity gi, String groupId, String current, TextView value) {
        com.harvana.app.husd.HusdClient.list(new com.harvana.app.husd.HusdClient.Cb() {
            @Override public void ok(JSONObject r) {
                JSONArray arr = r.optJSONArray("assets");
                List<String> codes = new ArrayList<>();
                for (int i = 0; arr != null && i < arr.length(); i++) {
                    JSONObject o = arr.optJSONObject(i);
                    String code = o == null ? "" : o.optString("asset");
                    if (o != null && o.optBoolean("active") && Stable.isStable(code) && !code.equals(current)) codes.add(code);
                }
                a.runOnUiThread(() -> {
                    if (a.isFinishing() || codes.isEmpty()) return;
                    String[] names = new String[codes.size()];
                    for (int i = 0; i < names.length; i++) names[i] = Stable.symbol(codes.get(i)) + " · " + Stable.name(codes.get(i));
                    com.harvana.app.p2p.ThemedDialogs.show(com.harvana.app.p2p.ThemedDialogs.alert(a).setTitle("Group coin")
                            .setItems(names, (dd, which) -> amount(a, gi, groupId, codes.get(which), value)));
                });
            }
            @Override public void fail(String e) { a.runOnUiThread(() -> Toast.makeText(a, e, Toast.LENGTH_SHORT).show()); }
        });
    }

    /** A rotating group's contribution, or a goal group's goal, in the new coin; a savings group needs no amount. */
    private static void amount(Activity a, GroupIdentity gi, String groupId, String code, TextView value) {
        boolean rotating = "CIRCLE".equals(gi.family) || gi.protocolId().startsWith("CIRC-");
        boolean goal = "FIXED".equals(gi.family);
        if (!rotating && !goal) { send(a, gi, groupId, code, 0, value); return; }
        EditText field = new com.google.android.material.textfield.TextInputEditText(a);
        field.setHint((rotating ? "Contribution" : "Goal") + " in " + Stable.symbol(code));
        field.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        field.setSingleLine(true);
        com.harvana.app.p2p.ThemedDialogs.show(com.harvana.app.p2p.ThemedDialogs.alert(a)
                .setTitle(Stable.symbol(code))
                .setView(field)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Change", (dd, w) -> {
                    Long units = com.harvana.app.husd.HusdClient.parse(field.getText() == null ? "" : field.getText().toString());
                    if (units == null || units <= 0) { Toast.makeText(a, (rotating ? "Contribution" : "Goal") + " in " + Stable.symbol(code), Toast.LENGTH_SHORT).show(); return; }
                    send(a, gi, groupId, code, units, value);
                }));
    }

    private static void send(Activity a, GroupIdentity gi, String groupId, String code, long amountRaw, TextView value) {
        String me = com.harvana.app.wallet.WalletManager.getActiveWalletAddress(a);
        com.harvana.app.sync.NodeGroupClient.changeGroupCoin(a, me, gi.protocolId(), code, amountRaw, new com.harvana.app.sync.NodeGroupClient.Callback() {
            @Override public void onSuccess(JSONObject json) {
                // The group's card and pages read these fields: keep them in step with the network.
                Map<String, Object> patch = new HashMap<>();
                patch.put("asset", code);
                if (amountRaw > 0) {
                    double units = amountRaw / (double) com.harvana.app.husd.HusdClient.UNIT;
                    if ("FIXED".equals(gi.family)) patch.put("targetGoalAmount", units); else patch.put("contributionAmount", units);
                }
                com.harvana.app.firestore.GroupFirestoreManager.getInstance(a).updateGroupFields(groupId, patch,
                        new com.harvana.app.firestore.GroupFirestoreManager.SimpleCallback() {
                            @Override public void onSuccess() { }
                            @Override public void onError(String error) { }
                        });
                a.runOnUiThread(() -> value.setText(Stable.symbol(code) + " · " + Stable.name(code)));
            }
            @Override public void onError(String e) {
                a.runOnUiThread(() -> Toast.makeText(a, friendly(e), Toast.LENGTH_LONG).show());
            }
        });
    }

    private static String friendly(String e) {
        if (e == null) return "Couldn't change the coin.";
        if (e.contains("MEMBERS_HAVE_JOINED")) return "Members have joined: the coin is fixed.";
        if (e.contains("GROUP_HAS_SAVINGS")) return "The group holds savings: the coin is fixed.";
        if (e.contains("GROUP_HAS_STARTED")) return "The group has started: the coin is fixed.";
        if (e.contains("ONLY_THE_CREATOR")) return "Only the creator can change the coin.";
        return e;
    }
}
