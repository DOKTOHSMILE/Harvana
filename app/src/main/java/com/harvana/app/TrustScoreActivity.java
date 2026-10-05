package com.harvana.app;

import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.ListenerRegistration;
import com.harvana.app.firestore.NodeMirrors;
import com.harvana.app.sync.NodeIdentityClient;
import com.harvana.app.utils.UiColors;
import com.harvana.app.wallet.WalletManager;

import org.json.JSONObject;

/**
 * The trust score is one percentage, worked out by the node from what the network confirmed (personhood, paying on
 * time, conduct, how long the wallet has been around) plus verified reading in Learn Hub, and saved into Firestore.
 * This screen shows it, what it is made of and how to raise it. The phone does not invent the number.
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
            root.addView(tv("\u2014", 36, true, UiColors.accent()));
            root.addView(tv("Reading the network…", 14, false, UiColors.textSecondary()));
            com.harvana.app.trust.NetworkTrust.wallet(address, score -> runOnUiThread(() -> {
                if (score.published) {
                    java.util.Map<String, Object> live = new java.util.HashMap<>();
                    live.put("trustScore", score.score);
                    if (score.percent >= 0) { live.put("trustPercent", score.percent); live.put("trustTier", score.tier); }
                    live.put("verifiedReads", score.verifiedReads);
                    live.put("activeDays30", score.activeDays30);
                    live.put("giftsSent", score.giftsSent);
                    live.put("bound", score.bound);
                    live.put("effectiveVouchCount", score.vouches);
                    live.put("established", score.established);
                    live.put("summary", score.summary.isEmpty() ? (score.score + " / 26") : score.summary);
                    render(live);
                    return;
                }
                root.removeAllViews();
                root.addView(tv("\u2014", 36, true, UiColors.accent()));
                root.addView(tv("The network has not published a score for this wallet yet. Bind this device and it will appear here once Firestore has it.", 14, false, UiColors.textSecondary()));
                addBind();
            }));
            return;
        }
        boolean bound = Boolean.TRUE.equals(person.get("bound"));
        int score = num(person.get("trustScore"));
        int vouches = num(person.get("effectiveVouchCount"));
        if (person.get("trustPercent") instanceof Number) {
            root.addView(heroCard(person));
            root.addView(partsCard(person));
            root.addView(engagementCard(person));
        } else {
            root.addView(scoreCard(score));
            root.addView(note("The node has not worked out a percentage for this wallet yet. It appears after the next confirmed action."));
        }
        LinearLayout account = cardBox();
        account.addView(tv("Account", 16, true, UiColors.textPrimary()));
        TextView who = note("Reading your profile…");
        account.addView(who);
        root.addView(account, 0);
        com.harvana.app.identity.CombinedIdentity.load(a -> runOnUiThread(() -> {
            if (gen != generation) return;
            StringBuilder bld = new StringBuilder();
            bld.append(a.name.isEmpty() ? "Signed-in account" : a.name);
            if (!a.username.isEmpty()) bld.append("\n@").append(a.username);
            if (!a.phone.isEmpty()) bld.append("\n").append(a.phone);
            if (!a.email.isEmpty()) bld.append("\n").append(a.email);
            bld.append("\n").append(address);
            who.setText(bld.toString());
        }));
        Object summary = person.get("summary");
        if (summary != null && !String.valueOf(summary).isEmpty()) {
            TextView sum = tv(String.valueOf(summary), 13, false, UiColors.textSecondary());
            sum.setPadding(dp(4), dp(8), dp(4), 0);
            root.addView(sum);
        }

        LinearLayout saved = cardBox();
        saved.addView(tv("Personhood detail", 16, true, UiColors.textPrimary()));
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
        java.util.List<com.harvana.app.trust.AppActivity.Event> network =
                com.harvana.app.trust.AppActivity.fromNetwork(networkLines);
        card.addView(tv("On the network", 14, true, UiColors.textPrimary()));
        if (network.isEmpty()) {
            card.addView(note("Nothing confirmed on the network yet."));
        } else {
            int shown = 0;
            for (com.harvana.app.trust.AppActivity.Event e : network) {
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
        com.harvana.app.trust.AppActivity.recent(events -> runOnUiThread(() -> {
            if (gen != generation) return;
            card.addView(tv("In the app", 14, true, UiColors.textPrimary()));
            if (events.isEmpty()) {
                card.addView(note("Nothing recorded in the app yet."));
                return;
            }
            int shown = 0;
            for (com.harvana.app.trust.AppActivity.Event e : events) {
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
                    com.harvana.app.trust.AppActivity.note(TrustScoreActivity.this, "BIND", "Bound this device");
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


    private static final String[][] TIERS = {{"Bronze", "0"}, {"Silver", "40"}, {"Gold", "65"}, {"Platinum", "85"}};


    /** The tier is the node's own word (trustTier); the percentage is only used when the node sent no tier. */
    private int tierIndex(java.util.Map<String, Object> p) {
        Object t = p.get("trustTier");
        if (t != null) for (int i = 0; i < TIERS.length; i++) if (TIERS[i][0].equalsIgnoreCase(String.valueOf(t))) return i;
        int percent = Math.max(0, Math.min(100, num(p.get("trustPercent"))));
        int idx = 0;
        for (int i = 0; i < TIERS.length; i++) if (percent >= Integer.parseInt(TIERS[i][1])) idx = i;
        return idx;
    }

    private static int blend(int base, int over, float amount) {
        int r = Math.round(((base >> 16) & 0xFF) * (1 - amount) + ((over >> 16) & 0xFF) * amount);
        int g = Math.round(((base >> 8) & 0xFF) * (1 - amount) + ((over >> 8) & 0xFF) * amount);
        int b = Math.round((base & 0xFF) * (1 - amount) + (over & 0xFF) * amount);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /** One swipeable page per tier, opening on the current one: Surpassed, Current or Yet to unlock, as in a loyalty level screen. */
    private LinearLayout tierPager(java.util.Map<String, Object> p) {
        final int percent = Math.max(0, Math.min(100, num(p.get("trustPercent"))));
        final int current = tierIndex(p);
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams wlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        wlp.topMargin = dp(12);
        wrap.setLayoutParams(wlp);

        // Each page is a whole tier screen: the tier card, the road to that tier and the next badge. Built up front so the pager can be
        // given the height of the tallest one.
        final java.util.List<View> pages = new java.util.ArrayList<>();
        int screenW = getResources().getDisplayMetrics().widthPixels - dp(32);
        int tallest = 0;
        for (int i = 0; i < TIERS.length; i++) {
            LinearLayout page = new LinearLayout(this);
            page.setOrientation(LinearLayout.VERTICAL);
            page.addView(tierPage(i, current, percent), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(176)));
            page.addView(journeyCard(p, i));
            page.addView(nextBadgeCard(p));
            page.measure(View.MeasureSpec.makeMeasureSpec(screenW, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            tallest = Math.max(tallest, page.getMeasuredHeight());
            pages.add(page);
        }
        androidx.viewpager2.widget.ViewPager2 pager = new androidx.viewpager2.widget.ViewPager2(this);
        pager.setAdapter(new androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>() {
            @Override public androidx.recyclerview.widget.RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                FrameLayout f = new FrameLayout(TrustScoreActivity.this);
                f.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                return new androidx.recyclerview.widget.RecyclerView.ViewHolder(f) { };
            }
            @Override public void onBindViewHolder(androidx.recyclerview.widget.RecyclerView.ViewHolder h, int i) {
                FrameLayout f = (FrameLayout) h.itemView;
                f.removeAllViews();
                View v = pages.get(i);
                if (v.getParent() instanceof ViewGroup) ((ViewGroup) v.getParent()).removeView(v);
                f.addView(v);
            }
            @Override public int getItemCount() { return TIERS.length; }
        });
        pager.setCurrentItem(current, false);
        wrap.addView(pager, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, tallest + dp(4)));
        tintScreen(current, false);

        final LinearLayout dots = new LinearLayout(this);
        dots.setGravity(Gravity.CENTER);
        dots.setPadding(0, dp(8), 0, 0);
        final View[] dot = new View[TIERS.length];
        for (int i = 0; i < TIERS.length; i++) {
            dot[i] = new View(this);
            dots.addView(dot[i], new LinearLayout.LayoutParams(dp(6), dp(6)));
            ((LinearLayout.LayoutParams) dot[i].getLayoutParams()).setMargins(dp(3), 0, dp(3), 0);
        }
        pager.registerOnPageChangeCallback(new androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            @Override public void onPageSelected(int pos) { paintDots(dot, pos); tintScreen(pos, true); }
        });
        paintDots(dot, current);
        wrap.addView(dots);
        return wrap;
    }

    private int screenTint = 0;

    /** The whole screen takes the colour of the tier on show, mixed into the theme background so day and night keep their own feel. */
    private void tintScreen(int tier, boolean animate) {
        View screen = findViewById(R.id.trust_screen);
        if (screen == null) return;
        int to = blend(UiColors.background(), com.harvana.app.trust.ProfileTrust.tierColor(TIERS[tier][0]), 0.16f);
        if (!animate || screenTint == 0) { screen.setBackgroundColor(to); screenTint = to; return; }
        android.animation.ValueAnimator a = android.animation.ValueAnimator.ofObject(new android.animation.ArgbEvaluator(), screenTint, to);
        a.setDuration(350);
        a.addUpdateListener(x -> screen.setBackgroundColor((int) x.getAnimatedValue()));
        a.start();
        screenTint = to;
    }

    private void paintDots(View[] dot, int sel) {
        for (int i = 0; i < dot.length; i++) {
            GradientDrawable g = new GradientDrawable();
            g.setCornerRadius(dp(3));
            g.setColor(i == sel ? com.harvana.app.trust.ProfileTrust.tierColor(TIERS[i][0]) : (UiColors.textTertiary() & 0x00FFFFFF) | 0x55000000);
            dot[i].setBackground(g);
            ViewGroup.LayoutParams lp = dot[i].getLayoutParams();
            lp.width = dp(i == sel ? 18 : 6);
            dot[i].setLayoutParams(lp);
        }
    }

    private View tierPage(int i, int current, int percent) {
        String name = TIERS[i][0];
        int at = Integer.parseInt(TIERS[i][1]);
        int color = com.harvana.app.trust.ProfileTrust.tierColor(name);
        boolean locked = i > current;
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.HORIZONTAL);
        page.setGravity(Gravity.CENTER_VERTICAL);
        page.setPadding(dp(20), dp(16), dp(20), dp(16));
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{blend(UiColors.surface(), color, 0.30f), blend(UiColors.surface(), color, 0.06f)});
        bg.setCornerRadius(dp(20));
        bg.setStroke(dp(1), (color & 0x00FFFFFF) | 0x55000000);
        page.setBackground(bg);
        LinearLayout.LayoutParams m = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        page.setLayoutParams(m);

        FrameLayout badge = new FrameLayout(this);
        GradientDrawable halo = new GradientDrawable();
        halo.setShape(GradientDrawable.OVAL);
        halo.setColor((color & 0x00FFFFFF) | (locked ? 0x22000000 : 0x44000000));
        badge.setBackground(halo);
        ImageView icon = new ImageView(this);
        icon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.icon_verified_round));
        icon.setColorFilter(color);
        icon.setAlpha(locked ? 0.45f : 1f);
        badge.addView(icon, new FrameLayout.LayoutParams(dp(54), dp(54), Gravity.CENTER));
        page.addView(badge, new LinearLayout.LayoutParams(dp(96), dp(96)));

        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        text.setPadding(dp(18), 0, 0, 0);
        text.addView(tv(name, 28, true, UiColors.textPrimary()));
        String status = i < current ? "Surpassed" : i == current ? "Current" : "Yet to unlock";
        text.addView(tv(status, 16, true, locked ? UiColors.textSecondary() : color));
        String line = i == 0 ? "Everyone starts here"
                : locked ? (at > percent ? (at - percent) + " more points to unlock" : "Needs reliability and conduct at 85% or more")
                : "Reached at " + at + "% trust";
        text.addView(note(line));
        if (i == current) text.addView(note("Your score: " + percent + "%"));
        page.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return page;
    }

    /** The road to the tier on this page: "Level up to Gold", "You are at Silver" or "Surpassed Bronze", with the knob where the score is. */
    private LinearLayout journeyCard(java.util.Map<String, Object> p, int page) {
        int percent = Math.max(0, Math.min(100, num(p.get("trustPercent"))));
        int cur = tierIndex(p);
        int color = com.harvana.app.trust.ProfileTrust.tierColor(TIERS[page][0]);
        LinearLayout c = cardBox();
        String head = page > cur ? (page == cur + 1 ? "Level up to " : "Work towards ") + TIERS[page][0]
                : page == cur ? "You are at " + TIERS[page][0] : "You have surpassed " + TIERS[page][0];
        c.addView(tv(head, 17, true, UiColors.textPrimary()));
        com.harvana.app.trust.TierJourneyView road = new com.harvana.app.trust.TierJourneyView(this);
        c.addView(road, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        road.set(percent, color);
        if (page > cur) {
            int left = Integer.parseInt(TIERS[page][1]) - percent;
            c.addView(tv(left > 0 ? left + " points left to reach " + TIERS[page][0]
                    : TIERS[page][0] + " also needs reliability and conduct at 85% or more", 14, true, UiColors.textPrimary()));
            c.addView(note(weakestPart(p)));
        } else if (page == cur && cur == TIERS.length - 1) {
            c.addView(note("Keep paying on time and the score holds."));
        } else if (page == cur) {
            c.addView(note("Your score is " + percent + "%."));
        }
        c.addView(note("The node works this out and rechecks it with every confirmed action."));
        return c;
    }

    /** Names the part of the score with the most room left, so the advice points somewhere real. */
    private String weakestPart(java.util.Map<String, Object> p) {
        String[] names = {"Personhood", "Reliability", "Conduct", "Tenure", "Engagement"};
        String[] keys = {"trustPersonhoodPct", "trustReliabilityPct", "trustConductPct", "trustTenurePct", "trustEngagementPct"};
        int[] weight = {30, 25, 15, 15, 15};
        String tips[] = {"Bind this device and ask established members to vouch for you.", "Pay circle contributions in full and on time.",
                "Finish circles cleanly.", "Stay active; time on the network counts.", "Read Learn Hub posts to the end on different days."};
        int best = 0; double room = -1;
        for (int i = 0; i < names.length; i++) {
            double r = weight[i] * (100 - num(p.get(keys[i]))) / 100.0;
            if (r > room) { room = r; best = i; }
        }
        return room <= 0 ? "Every part is full." : "Biggest gain: " + names[best] + " (up to " + Math.round(room) + " points). " + tips[best];
    }

    /** The next badge you can earn, with a bar that fills as you get closer. Only badges the node really awards. */
    private LinearLayout nextBadgeCard(java.util.Map<String, Object> p) {
        java.util.Set<String> held = new java.util.HashSet<>();
        if (p.get("labels") instanceof java.util.List) for (Object o : (java.util.List<?>) p.get("labels")) held.add(String.valueOf(o));
        int vouches = num(p.get("effectiveVouchCount"));
        int conduct = num(p.get("trustConductPct"));
        int reliability = num(p.get("trustReliabilityPct"));
        String[][] order = {
            {"VOUCHED", "Vouched", "One established member vouches for you", String.valueOf(Math.min(vouches, 1) * 100), "vouches"},
            {"CIRCLE_VETERAN", "Circle veteran", "Finish one rotating circle", conduct > 0 ? "100" : "0", ""},
            {"ZERO_DEFAULT", "Zero default", "Finish a circle without ever defaulting", conduct > 0 ? "100" : "0", ""},
            {"EARLY_PAYER", "Early payer", "Pay at least 10 contributions, 90% of them on time", String.valueOf(reliability), "reliability"}};
        String[] next = null;
        for (String[] o : order) if (!held.contains(o[0])) { next = o; break; }
        LinearLayout c = cardBox();
        c.addView(tv("Next badge", 17, true, UiColors.textPrimary()));
        if (next == null) {
            c.addView(note("You hold every badge the network awards for now. Insured trader comes from a P2P insurance deposit."));
            return c;
        }
        c.addView(tv(next[1], 15, true, UiColors.accent()));
        c.addView(note(next[2]));
        final int target = Integer.parseInt(next[3]);
        final android.widget.ProgressBar bar = bar(0, 100, UiColors.accent());
        c.addView(bar);
        android.animation.ObjectAnimator.ofInt(bar, "progress", 0, target).setDuration(900).start();
        c.addView(note(target >= 100 ? "Requirement met; the badge appears after the next confirmed action."
                : "reliability".equals(next[4]) ? "Your payment reliability so far: " + target + "%" : target + "% of the way"));
        return c;
    }

    private LinearLayout partsCard(java.util.Map<String, Object> p) {
        LinearLayout c = cardBox();
        c.addView(tv("What it is made of", 16, true, UiColors.textPrimary()));
        c.addView(partRow("Personhood", 30, num(p.get("trustPersonhoodPct")),
                "Bind this device and get vouched for by established members."));
        c.addView(partRow("Reliability", 25, num(p.get("trustReliabilityPct")),
                "Pay every circle contribution in full and on time. It counts fully after 5 obligations."));
        c.addView(partRow("Conduct", 15, num(p.get("trustConductPct")),
                "Finish circles cleanly. Finish 3 for the full part; each default takes half of it away."));
        c.addView(partRow("Tenure", 15, num(p.get("trustTenurePct")),
                "Time on the network and number of confirmed actions. Full at a year and 50 actions."));
        c.addView(partRow("Engagement", 15, num(p.get("trustEngagementPct")),
                "Read Learn Hub posts to the end, on different days, and gift creators."));
        return c;
    }

    private LinearLayout partRow(String name, int weight, int pct, String tip) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, dp(12), 0, 0);
        TextView top = tv(name + "   " + pct + "%   \u00b7   worth " + weight + " of 100", 14, true, UiColors.textPrimary());
        row.addView(top);
        row.addView(bar(pct, 100, UiColors.accent()));
        row.addView(note(tip));
        return row;
    }

    private LinearLayout engagementCard(java.util.Map<String, Object> p) {
        LinearLayout c = cardBox();
        c.addView(tv("Your Learn Hub activity", 16, true, UiColors.textPrimary()));
        c.addView(note(num(p.get("verifiedReads")) + " verified reads   \u00b7   " + num(p.get("activeDays30")) + " active days in the last 30   \u00b7   "
                + num(p.get("giftsSent")) + " gifts sent"));
        c.addView(note("Only reads the network verified count: you reached the end, stayed long enough and read each post once. "
                + "Reading that looks automated counts as zero."));
        MaterialButton open = new MaterialButton(this);
        open.setAllCaps(false);
        open.setText("See my activity");
        open.setOnClickListener(v -> startActivity(new android.content.Intent(this, ActivityStatsActivity.class)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(10);
        c.addView(open, lp);
        return c;
    }

    private android.widget.ProgressBar bar(int value, int max, int color) {
        android.widget.ProgressBar b = new android.widget.ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        b.setMax(max);
        b.setProgress(Math.max(0, Math.min(max, value)));
        b.setProgressTintList(android.content.res.ColorStateList.valueOf(color));
        b.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf((UiColors.textTertiary() & 0x00FFFFFF) | 0x33000000));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(10));
        lp.topMargin = dp(8);
        b.setLayoutParams(lp);
        return b;
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
