package com.harvanna.app;

import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.ListenerRegistration;
import com.harvanna.app.firestore.NodeMirrors;
import com.harvanna.app.sync.NodeIdentityClient;
import com.harvanna.app.utils.UiColors;
import com.harvanna.app.wallet.WalletManager;

import org.json.JSONObject;

/**
 * Trust is both halves together. The number is the network's personhood score
 * (0–26), saved by the node into Firestore. The list under it is activity:
 * actions the network confirmed, plus what this member did in the app.
 * The phone does not invent the number.
 */
public class TrustScoreActivity extends AppCompatActivity {
    private LinearLayout root;
    private String address = "";
    private ListenerRegistration mirror;
    private int generation;

    @Override
    protected void onDestroy() {
        if (mirror != null) mirror.remove();
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.trust_score);
        TextView title = findViewById(R.id.header_title);
        TextView subtitle = findViewById(R.id.header_subtitle);
        title.setText("Trust score");
        subtitle.setText("Network and activity");
        findViewById(R.id.header_back).setOnClickListener(v -> finish());
        root = findViewById(R.id.trust_root);
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
        final int gen = ++generation;
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
        root.addView(scoreCard(score));
        Object summary = person.get("summary");
        if (summary != null && !String.valueOf(summary).isEmpty()) {
            TextView sum = tv(String.valueOf(summary), 13, false, UiColors.textSecondary());
            sum.setPadding(dp(4), dp(8), dp(4), 0);
            root.addView(sum);
        }

        LinearLayout saved = cardBox();
        saved.addView(tv("Saved by the network", 16, true, UiColors.textPrimary()));
        saved.addView(metricRow(R.drawable.ic_phone, "Device", bound ? "10" : "0",
                "Binding this phone is worth 10. This is the figure the network saved."));
        saved.addView(metricRow(R.drawable.ic_person, "Vouches", String.valueOf(Math.min(vouches, 8) * 2),
                vouches + " active " + (vouches == 1 ? "vouch" : "vouches") + "."));
        if (Boolean.TRUE.equals(person.get("established"))) saved.addView(note("Established. You can vouch for other members."));
        if (numLong(person.get("vouchCoolOffUntil")) > System.currentTimeMillis()) {
            saved.addView(note("You cannot vouch again until the cool-off ends."));
        }
        root.addView(saved);
        if (!bound) addBind();

        LinearLayout badges = cardBox();
        badges.addView(tv("Badges", 16, true, UiColors.textPrimary()));
        if (!person.containsKey("labels") || !(person.get("labels") instanceof java.util.List) || ((java.util.List<?>) person.get("labels")).isEmpty()) {
            badges.addView(note("No badges yet. Finish a rotating circle and the network writes the badge here."));
        } else {
            java.util.List<?> list = (java.util.List<?>) person.get("labels");
            Object notes = person.get("labelNotes");
            java.util.List<?> detail = notes instanceof java.util.List ? (java.util.List<?>) notes : java.util.Collections.emptyList();
            for (int i = 0; i < list.size(); i++) {
                String name = String.valueOf(list.get(i));
                String body = i < detail.size() ? String.valueOf(detail.get(i)) : name;
                badges.addView(tv(titleOf(name), 15, true, UiColors.textPrimary()));
                badges.addView(note(body));
            }
        }
        root.addView(badges);
        showActivity(gen, person.get("activity"), num(person.get("activityCount")));
    }

    private void showActivity(int gen, Object networkLines, int networkCount) {
        LinearLayout card = cardBox();
        card.addView(tv("Activity", 16, true, UiColors.textPrimary()));
        card.addView(note(networkCount + (networkCount == 1 ? " action confirmed on the network." : " actions confirmed on the network.")
                + " What you do in the app is listed with it. Neither changes the score."));
        java.util.List<com.harvanna.app.trust.AppActivity.Event> network =
                com.harvanna.app.trust.AppActivity.fromNetwork(networkLines);
        card.addView(tv("On the network", 14, true, UiColors.textPrimary()));
        if (network.isEmpty()) {
            card.addView(note("Nothing confirmed on the network yet."));
        } else {
            int shown = 0;
            for (com.harvanna.app.trust.AppActivity.Event e : network) {
                if (shown++ == 8) break;
                card.addView(line(when(e.at), e.title, "Confirmed by the network"));
            }
        }
        View divider = new View(this);
        divider.setBackgroundColor(UiColors.textTertiary());
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        dlp.topMargin = dp(12);
        dlp.bottomMargin = dp(8);
        card.addView(divider, dlp);
        root.addView(card);
        com.harvanna.app.trust.AppActivity.recent(events -> runOnUiThread(() -> {
            if (gen != generation) return;
            card.addView(tv("In the app", 14, true, UiColors.textPrimary()));
            if (events.isEmpty()) {
                card.addView(note("Nothing recorded in the app yet."));
                return;
            }
            int shown = 0;
            for (com.harvanna.app.trust.AppActivity.Event e : events) {
                if (shown++ == 8) break;
                card.addView(line(when(e.at), e.title, "Recorded in the app"));
            }
        }));
    }

    private static String when(long at) {
        if (at <= 0) return "Activity";
        return new java.text.SimpleDateFormat("d MMM", java.util.Locale.US).format(new java.util.Date(at));
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
                @Override public void onSuccess(JSONObject j) {
                    com.harvanna.app.trust.AppActivity.note(TrustScoreActivity.this, "BIND", "Bound this device");
                    runOnUiThread(TrustScoreActivity.this::load);
                }
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

    private LinearLayout scoreCard(int score) {
        LinearLayout c = cardBox();
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.BOTTOM);
        TextView big = tv(String.valueOf(score), 40, true, UiColors.accent());
        big.setPadding(0, 0, 0, 0);
        TextView of = tv(" / 26", 22, false, UiColors.textSecondary());
        of.setPadding(0, 0, 0, dp(6));
        row.addView(big);
        row.addView(of);
        c.addView(row);
        return c;
    }

    private LinearLayout cardBox() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        int p = dp(16);
        c.setPadding(p, p, p, p);
        GradientDrawable g = new GradientDrawable();
        g.setColor(UiColors.surface());
        g.setCornerRadius(dp(16));
        g.setStroke(dp(1), (UiColors.textTertiary() & 0x00FFFFFF) | 0x33000000);
        c.setBackground(g);
        c.setElevation(dp(2));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(12);
        c.setLayoutParams(lp);
        return c;
    }

    private LinearLayout metricRow(int icon, String name, String value, String hint) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(12), 0, 0);
        ImageView iv = new ImageView(this);
        iv.setImageDrawable(ContextCompat.getDrawable(this, icon));
        iv.setColorFilter(UiColors.textSecondary());
        int s = dp(20);
        row.addView(iv, new LinearLayout.LayoutParams(s, s));
        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        text.setPadding(dp(12), 0, 0, 0);
        text.addView(tv(name + "   " + value, 16, true, UiColors.textPrimary()));
        text.addView(note(hint));
        row.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return row;
    }

    private TextView note(String t) {
        TextView v = tv(t, 12, false, UiColors.textSecondary());
        v.setPadding(0, dp(2), 0, 0);
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
