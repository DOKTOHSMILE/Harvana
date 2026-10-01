package com.harvanna.app;

import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.ListenerRegistration;
import com.harvanna.app.firestore.NodeMirrors;
import com.harvanna.app.sync.NodeIdentityClient;
import com.harvanna.app.utils.UiColors;
import com.harvanna.app.wallet.WalletManager;

import org.json.JSONObject;

/**
 * Trust as the node records it. The number is personhood: 10 once this device is bound to the
 * wallet, plus 2 for each active vouch, up to 8. Labels are live badges from circle history.
 * Neither is stored in Firebase, and neither is a 100-point profile score.
 */
public class TrustScoreActivity extends AppCompatActivity {
    private LinearLayout root;
    private String address = "";
    private ListenerRegistration mirror;

    @Override
    protected void onDestroy() {
        if (mirror != null) mirror.remove();
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(UiColors.background());
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int p = dp(16);
        root.setPadding(p, p, p, dp(28));
        sv.addView(root);
        com.harvanna.app.utils.AppBar.setContent(this, sv, "Trust score", "Recorded on the network");
    }

    @Override protected void onResume() { super.onResume(); load(); }
    @Override public boolean onSupportNavigateUp() { finish(); return true; }

    private void load() {
        root.removeAllViews();
        root.addView(tv("Reading the network…", 14, false, UiColors.textSecondary()));
        address = WalletManager.getActiveWalletAddress(this);
        if (address == null || address.isEmpty()) {
            root.removeAllViews();
            root.addView(tv("No wallet yet", 22, true, UiColors.textPrimary()));
            root.addView(tv("Create a wallet first. Trust is recorded against the wallet, not the profile.", 14, false, UiColors.textSecondary()));
            return;
        }
        if (mirror != null) mirror.remove();
        mirror = NodeMirrors.wallet(address, data -> runOnUiThread(() -> render(data)));
    }

    private void render(java.util.Map<String, Object> person) {
        root.removeAllViews();
        if (person == null) {
            root.addView(tv("— / 26", 36, true, UiColors.accent()));
            root.addView(tv("Reading the network…", 14, false, UiColors.textSecondary()));
            com.harvanna.app.trust.NetworkTrust.wallet(address, score -> runOnUiThread(() -> {
                if (score.published) {
                    java.util.Map<String, Object> live = new java.util.HashMap<>();
                    live.put("trustScore", score.score);
                    live.put("bound", score.bound);
                    live.put("effectiveVouchCount", score.vouches);
                    live.put("established", score.established);
                    live.put("summary", score.summary.isEmpty() ? (score.score + " / 26") : score.summary);
                    render(live);
                    return;
                }
                root.removeAllViews();
                root.addView(tv("— / 26", 36, true, UiColors.accent()));
                root.addView(tv("The network has not published a score for this wallet yet. Bind this device and it will appear here once Firestore has it.", 14, false, UiColors.textSecondary()));
                addBind();
            }));
            return;
        }
        boolean bound = Boolean.TRUE.equals(person.get("bound"));
        int score = num(person.get("trustScore"));
        int vouches = num(person.get("effectiveVouchCount"));
        root.addView(tv(score + " / 26", 36, true, UiColors.accent()));
        Object summary = person.get("summary");
        root.addView(tv(summary == null ? "" : String.valueOf(summary), 14, false, UiColors.textSecondary()));

        root.addView(head("Saved by the network"));
        root.addView(line("Device", bound ? "10" : "0", "Binding this phone is worth 10. Firestore is showing the figure the node saved."));
        root.addView(line("Vouches", String.valueOf(Math.min(vouches, 8) * 2), vouches + " active " + (vouches == 1 ? "vouch" : "vouches") + "."));
        if (Boolean.TRUE.equals(person.get("established"))) root.addView(tv("Established. You can vouch for other members.", 13, false, UiColors.textPrimary()));
        if (numLong(person.get("vouchCoolOffUntil")) > System.currentTimeMillis()) {
            root.addView(tv("You cannot vouch again until the cool-off ends.", 13, false, UiColors.error()));
        }
        if (!bound) addBind();

        if (person.containsKey("labels")) {
        root.addView(head("Badges"));
        Object names = person.get("labels");
        Object notes = person.get("labelNotes");
        if (!(names instanceof java.util.List) || ((java.util.List<?>) names).isEmpty()) {
            root.addView(card("No badges yet", "Finish a rotating circle. The node writes the badge here. This screen does not invent one."));
        } else {
            java.util.List<?> list = (java.util.List<?>) names;
            java.util.List<?> detail = notes instanceof java.util.List ? (java.util.List<?>) notes : java.util.Collections.emptyList();
            for (int i = 0; i < list.size(); i++) {
                String name = String.valueOf(list.get(i));
                String body = i < detail.size() ? String.valueOf(detail.get(i)) : name;
                root.addView(card(titleOf(name), body));
            }
        }
        }
    }

    private void addBind() {
        MaterialButton bind = new MaterialButton(this);
        bind.setAllCaps(false);
        bind.setText("Bind this device");
        bind.setOnClickListener(v -> bindDevice());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(12);
        root.addView(bind, lp);
    }

    private void bindDevice() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Bind this device?")
            .setMessage("This ties the wallet to this phone and sets the base score to 10. It does not upload an ID. One device hash can be bound to one wallet.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Bind", (d, w) -> NodeIdentityClient.bindPersonhood(this, address, new NodeIdentityClient.Callback() {
                @Override public void onSuccess(JSONObject j) { runOnUiThread(TrustScoreActivity.this::load); }
                @Override public void onError(String e) {
                    runOnUiThread(() -> android.widget.Toast.makeText(TrustScoreActivity.this, e, android.widget.Toast.LENGTH_LONG).show());
                }
            }))
            .show();
    }

    private static String titleOf(String name) {
        if ("CIRCLE_VETERAN".equals(name)) return "Circle veteran";
        if ("FOUNDING_MEMBER".equals(name)) return "Founding member";
        if ("ZERO_DEFAULT".equals(name)) return "Zero default";
        if ("EARLY_PAYER".equals(name)) return "Early payer";
        if ("VOUCHED".equals(name)) return "Vouched";
        return name;
    }

    private static int num(Object o) { return o instanceof Number ? ((Number) o).intValue() : 0; }
    private static long numLong(Object o) { return o instanceof Number ? ((Number) o).longValue() : 0; }

    private ViewGroup line(String name, String value, String note) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, dp(8), 0, dp(4));
        TextView top = tv(name + "  ·  " + value, 15, true, UiColors.textPrimary());
        row.addView(top);
        row.addView(tv(note, 12, false, UiColors.textSecondary()));
        return row;
    }

    private ViewGroup card(String title, String body) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        int p = dp(14);
        c.setPadding(p, p, p, p);
        GradientDrawable g = new GradientDrawable();
        g.setColor(UiColors.surface());
        g.setCornerRadius(dp(14));
        c.setBackground(g);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(8);
        c.setLayoutParams(lp);
        c.addView(tv(title, 15, true, UiColors.textPrimary()));
        c.addView(tv(body, 13, false, UiColors.textSecondary()));
        return c;
    }

    private TextView head(String t) {
        TextView v = tv(t, 16, true, UiColors.textPrimary());
        v.setPadding(0, dp(22), 0, dp(4));
        return v;
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    private TextView tv(String t, int sp, boolean bold, int color) {
        TextView v = new TextView(this);
        v.setText(t);
        v.setTextSize(sp);
        v.setTextColor(color);
        if (bold) v.setTypeface(null, Typeface.BOLD);
        v.setPadding(0, dp(2), 0, dp(2));
        return v;
    }
}
