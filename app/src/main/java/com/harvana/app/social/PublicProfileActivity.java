package com.harvana.app.social;

import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.harvana.app.GroupDetailsActivity;
import com.harvana.app.SendAssetActivity;
import com.harvana.app.trust.BadgeChips;
import com.harvana.app.trust.NodeBadges;
import com.harvana.app.trust.ProfileTrust;
import com.harvana.app.utils.AppBar;
import com.harvana.app.utils.UiColors;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * A member's public profile: what they have chosen to show and what the network shows about everyone.
 * Always shown: name, photo, trust score and its parts, country and state, member-since, counts of groups, cycles, loans and
 * pending loans. Shown only when the member allows it: age and city. Never shown: amounts, group names (only groups both people
 * belong to, and tapping one opens it), date of birth, phone, email. Opened from the Friends list.
 */
public class PublicProfileActivity extends AppCompatActivity {
    public static final String EXTRA_UID = "uid";

    private LinearLayout root;
    private String uid = "";

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        uid = getIntent().getStringExtra(EXTRA_UID);
        if (uid == null) uid = "";
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(UiColors.background());
        sv.setFillViewport(true);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int p = SocialUi.dp(this, 16);
        root.setPadding(p, p, p, SocialUi.dp(this, 28));
        sv.addView(root);
        AppBar.setContent(this, sv, "Profile", "");
        root.addView(SocialUi.text(this, "Loading…", 14, false, UiColors.textSecondary()));
    }

    @Override protected void onResume() { super.onResume(); load(); }
    @Override public boolean onSupportNavigateUp() { finish(); return true; }

    private void load() {
        if (uid.isEmpty()) { root.removeAllViews(); root.addView(SocialUi.text(this, "This profile can't be opened.", 14, false, UiColors.textSecondary())); return; }
        PublicProfileStore.load(uid, (card, err) -> runOnUiThread(() -> {
            if (isFinishing()) return;
            root.removeAllViews();
            if (card == null) {
                root.addView(SocialUi.text(this, "Couldn't load this profile: " + err, 14, false, UiColors.textSecondary()));
                return;
            }
            render(card);
        }));
    }

    private boolean isMe(PublicProfileStore.Card c) {
        com.google.firebase.auth.FirebaseUser u = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        return u != null && u.getUid().equals(c.uid);
    }

    private void render(PublicProfileStore.Card c) {
        boolean mine = isMe(c);
        root.addView(heroCard(c, mine));
        root.addView(actions(c, mine));
        root.addView(aboutCard(c, mine));
        root.addView(trustCard(c));
        root.addView(badgesCard(c));
        root.addView(activityCard(c));
        root.addView(sharedCard(c, mine));
        TextView foot = SocialUi.text(this, "Trust, country and state are always public. Amounts and group names are never shown. "
                + "Age and city are shown only if the member turns them on.", 11.5f, false, UiColors.textTertiary());
        foot.setPadding(SocialUi.dp(this, 4), SocialUi.dp(this, 4), SocialUi.dp(this, 4), 0);
        root.addView(foot);
    }

    // ── Hero ─────────────────────────────────────────────────────────────

    private View heroCard(PublicProfileStore.Card c, boolean mine) {
        LinearLayout card = SocialUi.card(this);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        int size = SocialUi.dp(this, 88);
        int tint = c.scored() ? ProfileTrust.tierColor(c.trustTier.isEmpty() ? ProfileTrust.tierFor(c.trustPercent) : c.trustTier) : 0xFF8B98AD;
        FrameLayout face = new FrameLayout(this);
        TextView initials = SocialUi.text(this, initialsOf(c), 30, true, UiColors.onAccent());
        initials.setGravity(Gravity.CENTER);
        GradientDrawable disc = new GradientDrawable();
        disc.setShape(GradientDrawable.OVAL);
        disc.setColor(UiColors.accent());
        disc.setStroke(SocialUi.dp(this, 3), tint);
        initials.setBackground(disc);
        face.addView(initials, new FrameLayout.LayoutParams(size, size));
        ImageView photo = new ImageView(this);
        photo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        face.addView(photo, new FrameLayout.LayoutParams(size, size));
        if (c.icon.startsWith("http")) {
            try { com.bumptech.glide.Glide.with(this).load(c.icon).circleCrop().into(photo); initials.setVisibility(View.INVISIBLE); }
            catch (Exception ignored) { }
        }
        card.addView(face, new LinearLayout.LayoutParams(size, size));

        TextView name = SocialUi.text(this, c.name.isEmpty() ? (c.username.isEmpty() ? "Member" : "@" + c.username) : c.name, 22, true, UiColors.textPrimary());
        name.setGravity(Gravity.CENTER);
        name.setPadding(0, SocialUi.dp(this, 12), 0, 0);
        card.addView(name);
        if (!c.username.isEmpty()) {
            TextView handle = SocialUi.text(this, "@" + c.username, 14, false, UiColors.textSecondary());
            handle.setGravity(Gravity.CENTER);
            card.addView(handle);
        }
        LinearLayout pills = new LinearLayout(this);
        pills.setOrientation(LinearLayout.HORIZONTAL);
        pills.setGravity(Gravity.CENTER);
        pills.setPadding(0, SocialUi.dp(this, 12), 0, 0);
        pills.addView(pill(c.scored() ? (("New".equals(c.trustTier) ? "New · " + c.trustPercent + "%" : c.trustPercent + "% · " + (c.trustTier.isEmpty() ? ProfileTrust.tierFor(c.trustPercent) : c.trustTier))) : "Not scored yet", tint));
        if (!mine) {
            String rel = c.iFollow && c.followsMe ? "Friends" : c.iFollow ? "You follow" : c.followsMe ? "Follows you" : "";
            if (!rel.isEmpty()) pills.addView(pill(rel, UiColors.textSecondary()));
        } else {
            pills.addView(pill("This is you", UiColors.accent()));
        }
        card.addView(pills);
        if (!c.bio.isEmpty()) {
            TextView bio = SocialUi.text(this, c.bio, 14, false, UiColors.textPrimary());
            bio.setGravity(Gravity.CENTER);
            bio.setPadding(SocialUi.dp(this, 8), SocialUi.dp(this, 14), SocialUi.dp(this, 8), 0);
            bio.setLineSpacing(SocialUi.dp(this, 2), 1f);
            card.addView(bio);
        }
        return card;
    }

    private TextView pill(String text, int color) {
        TextView t = SocialUi.text(this, text, 12, true, color);
        t.setPadding(SocialUi.dp(this, 12), SocialUi.dp(this, 5), SocialUi.dp(this, 12), SocialUi.dp(this, 5));
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(SocialUi.dp(this, 14));
        g.setColor((color & 0x00FFFFFF) | 0x22000000);
        g.setStroke(SocialUi.dp(this, 1), (color & 0x00FFFFFF) | 0x77000000);
        t.setBackground(g);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(SocialUi.dp(this, 4), 0, SocialUi.dp(this, 4), 0);
        t.setLayoutParams(lp);
        return t;
    }

    private static String initialsOf(PublicProfileStore.Card c) {
        String s = c.name.isEmpty() ? c.username : c.name;
        String[] parts = s.trim().split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) return "?";
        if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase(Locale.US);
        return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase(Locale.US);
    }

    // ── Actions ──────────────────────────────────────────────────────────

    private View actions(PublicProfileStore.Card c, boolean mine) {
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 0, 0, SocialUi.dp(this, 14));
        final com.harvana.app.social.Person person = new com.harvana.app.social.Person(c.uid, c.username, c.name, c.icon);
        if (mine) {
            row.addView(action("Privacy settings", false, v -> startActivity(new Intent(this, ProfilePrivacyActivity.class))));
        } else {
            MaterialButton follow = action(c.iFollow ? "Unfollow" : (c.followsMe ? "Follow back" : "Follow"), c.iFollow, null);
            follow.setOnClickListener(v -> {
                follow.setEnabled(false);
                if (c.iFollow) SocialStore.unfollow(c.uid, (ok, e) -> runOnUiThread(() -> { if (!ok) toast(e); load(); }));
                else SocialStore.follow(person, (ok, e) -> runOnUiThread(() -> { if (!ok) toast(e); load(); }));
            });
            row.addView(follow);
            if (c.iFollow) {
                MaterialButton pinch = action("Pinch", true, null);
                if (PinchStore.waitMs(this, c.uid) > 0) { pinch.setText("Pinched"); pinch.setEnabled(false); }
                pinch.setOnClickListener(v -> {
                    pinch.setEnabled(false);
                    PinchStore.send(this, person, (ok, err) -> runOnUiThread(() -> {
                        if (ok) { pinch.setText("Pinched"); toast(person.name + " will get a notification."); }
                        else { pinch.setEnabled(true); toast(err); }
                    }));
                });
                row.addView(pinch);
            }
            if (c.iFollow && c.followsMe) row.addView(action("Contact", true, v -> FriendsActivity.openCard(this, person)));
            if (!c.wallet.isEmpty()) {
                row.addView(action("Send HARV", true, v -> startActivity(new Intent(this, SendAssetActivity.class).putExtra("toAddress", c.wallet))));
            }
        }
        row.addView(action("Share", true, v -> {
            String line = (c.name.isEmpty() ? "A member" : c.name) + (c.username.isEmpty() ? "" : " (@" + c.username + ")") + " on Harvana"
                    + (c.scored() ? ", trust " + c.trustPercent + "%" : "") + ".";
            startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, line), "Share profile"));
        }));
        scroll.addView(row);
        return scroll;
    }

    private MaterialButton action(String label, boolean outlined, View.OnClickListener click) {
        MaterialButton b = SocialUi.compactButton(this, label, outlined);
        b.setTextSize(13);
        ((LinearLayout.LayoutParams) b.getLayoutParams()).height = SocialUi.dp(this, 40);
        ((LinearLayout.LayoutParams) b.getLayoutParams()).setMarginEnd(SocialUi.dp(this, 8));
        if (click != null) b.setOnClickListener(click);
        return b;
    }

    private void toast(String s) { Toast.makeText(this, s == null ? "Something went wrong." : s, Toast.LENGTH_LONG).show(); }

    // ── About ────────────────────────────────────────────────────────────

    private View aboutCard(PublicProfileStore.Card c, boolean mine) {
        LinearLayout card = SocialUi.card(this);
        card.addView(SocialUi.text(this, "About", 16, true, UiColors.textPrimary()));
        String where = c.country.isEmpty() ? "" : (c.state.isEmpty() ? c.country : c.state + ", " + c.country);
        card.addView(infoRow("Location", where.isEmpty() ? "Not provided" : where, !where.isEmpty()));
        card.addView(infoRow("City", c.showCity && !c.city.isEmpty() ? c.city : "Private", c.showCity && !c.city.isEmpty()));
        card.addView(infoRow("Age", c.showAge && c.age >= 0 ? c.age + " years" : "Private", c.showAge && c.age >= 0));
        String since = c.memberSince > 0 ? new SimpleDateFormat("MMM yyyy", Locale.US).format(new Date(c.memberSince)) : "Not recorded";
        card.addView(infoRow("Member since", since, c.memberSince > 0));
        return card;
    }

    private View infoRow(String label, String value, boolean shown) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, SocialUi.dp(this, 10), 0, 0);
        row.addView(SocialUi.text(this, label, 14, false, UiColors.textSecondary()), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView v = SocialUi.text(this, value, 14, shown, shown ? UiColors.textPrimary() : UiColors.textTertiary());
        v.setGravity(Gravity.END);
        row.addView(v, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return row;
    }

    // ── Trust ────────────────────────────────────────────────────────────

    private View trustCard(PublicProfileStore.Card c) {
        LinearLayout card = SocialUi.card(this);
        card.addView(SocialUi.text(this, "Trust score", 16, true, UiColors.textPrimary()));
        if (!c.scored()) {
            card.addView(SocialUi.text(this, "The network hasn't published a score for this member yet.", 13, false, UiColors.textSecondary()));
            return card;
        }
        String tier = c.trustTier.isEmpty() ? ProfileTrust.tierFor(c.trustPercent) : c.trustTier;
        int color = ProfileTrust.tierColor(tier);
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.BOTTOM);
        top.setPadding(0, SocialUi.dp(this, 6), 0, 0);
        top.addView(SocialUi.text(this, c.trustPercent + "%", 34, true, color));
        TextView t = SocialUi.text(this, "  " + tier, 16, true, UiColors.textPrimary());
        t.setPadding(0, 0, 0, SocialUi.dp(this, 5));
        top.addView(t);
        card.addView(top);
        card.addView(bar(c.trustPercent, color));
        String[][] parts = {{"Personhood", "trustPersonhoodPct"}, {"Reliability", "trustReliabilityPct"}, {"Conduct", "trustConductPct"},
                {"Tenure", "trustTenurePct"}, {"Engagement", "trustEngagementPct"}};
        if (c.trustParts.get("trustPersonhoodPct") instanceof Number) {
            for (String[] part : parts) {
                int v = c.trustParts.get(part[1]) instanceof Number ? ((Number) c.trustParts.get(part[1])).intValue() : 0;
                LinearLayout line = new LinearLayout(this);
                line.setOrientation(LinearLayout.VERTICAL);
                line.setPadding(0, SocialUi.dp(this, 12), 0, 0);
                LinearLayout head = new LinearLayout(this);
                head.addView(SocialUi.text(this, part[0], 13, false, UiColors.textSecondary()), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                head.addView(SocialUi.text(this, v + "%", 13, true, UiColors.textPrimary()));
                line.addView(head);
                line.addView(bar(v, UiColors.accent()));
                card.addView(line);
            }
        }
        TextView note = SocialUi.text(this, "Worked out by the network. Nobody can hide or change it.", 12, false, UiColors.textTertiary());
        note.setPadding(0, SocialUi.dp(this, 12), 0, 0);
        card.addView(note);
        return card;
    }

    private ProgressBar bar(int value, int color) {
        ProgressBar b = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        b.setMax(100);
        b.setProgress(Math.max(0, Math.min(100, value)));
        b.setProgressTintList(android.content.res.ColorStateList.valueOf(color));
        b.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf((UiColors.textTertiary() & 0x00FFFFFF) | 0x33000000));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, SocialUi.dp(this, 8));
        lp.topMargin = SocialUi.dp(this, 8);
        b.setLayoutParams(lp);
        return b;
    }

    // ── Badges ───────────────────────────────────────────────────────────

    private View badgesCard(PublicProfileStore.Card c) {
        LinearLayout card = SocialUi.card(this);
        card.addView(SocialUi.text(this, "Badges", 16, true, UiColors.textPrimary()));
        final LinearLayout chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.VERTICAL);
        chips.setPadding(0, SocialUi.dp(this, 10), 0, 0);
        card.addView(chips);
        if (c.wallet.isEmpty()) { BadgeChips.fill(chips, new java.util.ArrayList<>(), false); return card; }
        NodeBadges.load(c.wallet, list -> BadgeChips.fill(chips, list, false));
        return card;
    }

    // ── Activity counts ──────────────────────────────────────────────────

    private View activityCard(PublicProfileStore.Card c) {
        LinearLayout card = SocialUi.card(this);
        card.addView(SocialUi.text(this, "Activity", 16, true, UiColors.textPrimary()));
        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setPadding(0, SocialUi.dp(this, 10), 0, 0);
        LinearLayout r1 = new LinearLayout(this);
        r1.addView(tile("Groups", c.groups), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        r1.addView(tile("Cycles completed", c.cycles), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        LinearLayout r2 = new LinearLayout(this);
        r2.addView(tile("Loans", c.loans), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        r2.addView(tile("Pending loans", c.pendingLoans), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        grid.addView(r1);
        grid.addView(r2);
        card.addView(grid);
        TextView note = SocialUi.text(this, "Counts only. Amounts and group names are never shown.", 12, false, UiColors.textTertiary());
        note.setPadding(0, SocialUi.dp(this, 8), 0, 0);
        card.addView(note);
        return card;
    }

    private View tile(String label, int value) {
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        t.setGravity(Gravity.CENTER);
        int pad = SocialUi.dp(this, 12);
        t.setPadding(pad, pad, pad, pad);
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(SocialUi.dp(this, 14));
        g.setColor((UiColors.textTertiary() & 0x00FFFFFF) | 0x14000000);
        t.setBackground(g);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(SocialUi.dp(this, 4), SocialUi.dp(this, 4), SocialUi.dp(this, 4), SocialUi.dp(this, 4));
        t.setLayoutParams(lp);
        t.addView(SocialUi.text(this, value < 0 ? "—" : String.valueOf(value), 22, true, UiColors.textPrimary()));
        TextView l = SocialUi.text(this, label, 11.5f, false, UiColors.textSecondary());
        l.setGravity(Gravity.CENTER);
        t.addView(l);
        return t;
    }

    // ── Groups in common ─────────────────────────────────────────────────

    private View sharedCard(PublicProfileStore.Card c, boolean mine) {
        LinearLayout card = SocialUi.card(this);
        card.addView(SocialUi.text(this, mine ? "Your groups" : "Groups you share", 16, true, UiColors.textPrimary()));
        if (mine) {
            TextView note = SocialUi.text(this, "Others only see how many groups you are in. Group names show only for groups you both belong to.", 13, false, UiColors.textSecondary());
            note.setPadding(0, SocialUi.dp(this, 6), 0, 0);
            card.addView(note);
            return card;
        }
        if (c.sharedGroups.isEmpty()) {
            TextView none = SocialUi.text(this, "You and " + (c.name.isEmpty() ? "this member" : c.name) + " have no groups in common"
                    + (c.groups > 0 ? ". They are in " + c.groups + (c.groups == 1 ? " group." : " groups.") : "."), 13, false, UiColors.textSecondary());
            none.setPadding(0, SocialUi.dp(this, 6), 0, 0);
            card.addView(none);
            return card;
        }
        for (PublicProfileStore.SharedGroup g : c.sharedGroups) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, SocialUi.dp(this, 12), 0, SocialUi.dp(this, 4));
            TextView name = SocialUi.text(this, g.name, 15, true, UiColors.textPrimary());
            row.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            row.addView(SocialUi.text(this, "Open  ›", 13, true, UiColors.accent()));
            row.setClickable(true);
            row.setOnClickListener(v -> startActivity(new Intent(this, GroupDetailsActivity.class).putExtra("groupId", g.id)));
            card.addView(row);
        }
        return card;
    }
}
