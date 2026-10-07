package com.harvana.app.social;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.harvana.app.ActivityStatsActivity;
import com.harvana.app.EditProfileActivity;
import com.harvana.app.GroupDetailsActivity;
import com.harvana.app.ProfileActivity;
import com.harvana.app.R;
import com.harvana.app.SendAssetActivity;
import com.harvana.app.SettingsActivity;
import com.harvana.app.firestore.UserProfileManager;
import com.harvana.app.trust.BadgeChips;
import com.harvana.app.trust.NodeBadges;
import com.harvana.app.trust.ProfileTrust;
import com.harvana.app.utils.AppBar;
import com.harvana.app.utils.UiColors;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

/**
 * A member's profile, the same layout for everyone: photo, name and presence, a row of action tiles, an info card with the details
 * they show, and three tabs (Trust, Activity, Groups). Opened from Friends for other people and from the drawer for yourself.
 *
 * Others see: name, photo, trust score and parts, badges, country and state, member-since, counts of groups, cycles, loans and
 * pending loans, groups both belong to (tap to open), and age and city only if the member allows them. Never amounts, group names
 * you do not share, date of birth, phone or email. You see your own details too (phone, birthday, email) and your own actions.
 */
public class PublicProfileActivity extends AppCompatActivity {
    public static final String EXTRA_UID = "uid";

    private LinearLayout root;
    private LinearLayout tabContent;
    private String uid = "";
    private PublicProfileStore.Card card;
    private Map<String, Object> selfData;
    private int tab;

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
        root.setPadding(p, 0, p, SocialUi.dp(this, 28));
        sv.addView(root);
        AppBar.setContent(this, sv, "", "");
        root.addView(com.harvana.app.utils.Shimmer.profile(this));
    }

    @Override protected void onResume() { super.onResume(); load(); }
    @Override public boolean onSupportNavigateUp() { finish(); return true; }

    private boolean isMe() {
        com.google.firebase.auth.FirebaseUser u = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        return u != null && u.getUid().equals(uid);
    }

    private void load() {
        if (uid.isEmpty()) { root.removeAllViews(); root.addView(SocialUi.text(this, "This profile can't be opened.", 14, false, UiColors.textSecondary())); return; }
        PublicProfileStore.load(uid, (c, err) -> runOnUiThread(() -> {
            if (isFinishing()) return;
            if (c == null) {
                root.removeAllViews();
                root.addView(SocialUi.text(this, "Couldn't load this profile: " + err, 14, false, UiColors.textSecondary()));
                return;
            }
            card = c;
            if (isMe()) PublicProfileStore.publishMine(getApplicationContext());
            invalidateOptionsMenu();
            render();
            if (isMe()) UserProfileManager.getInstance(getApplicationContext()).getProfile(uid, new UserProfileManager.DocCallback() {
                @Override public void onSuccess(Map<String, Object> data) { runOnUiThread(() -> { selfData = data; if (!isFinishing()) render(); }); }
                @Override public void onError(String error) { }
            });
        }));
    }

    // ── Menu ─────────────────────────────────────────────────────────────

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (card == null) return true;
        if (isMe()) {
            menu.add(0, 1, 0, "Share profile");
            if (!card.username.isEmpty()) menu.add(0, 2, 1, "Copy username");
            menu.add(0, 3, 2, "Change username");
            menu.add(0, 4, 3, "Profile privacy");
            menu.add(0, 5, 4, "Edit profile");
            menu.add(0, 6, 5, "My QR code");
            menu.add(0, 9, 6, "Settings");
        } else {
            menu.add(0, 1, 0, "Share profile");
            if (!card.username.isEmpty()) menu.add(0, 2, 1, "Copy username");
            if (card.iFollow && card.followsMe) menu.add(0, 7, 2, "Contact details");
            if (card.iFollow) menu.add(0, 8, 3, "Unfollow");
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (card == null) return super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case 1: shareProfile(); return true;
            case 2: copy("@" + card.username); return true;
            case 3: startActivity(new Intent(this, ContactCardActivity.class)); return true;
            case 4: startActivity(new Intent(this, ProfilePrivacyActivity.class)); return true;
            case 5: startActivity(new Intent(this, EditProfileActivity.class)); return true;
            case 6: startActivity(new Intent(this, ProfileQrActivity.class)); return true;
            case 9: startActivity(new Intent(this, ProfileSettingsActivity.class)); return true;
            case 7: FriendsActivity.openCard(this, person()); return true;
            case 8: SocialStore.unfollow(card.uid, (ok, e) -> runOnUiThread(() -> { if (!ok) toast(e); load(); })); return true;
            default: return super.onOptionsItemSelected(item);
        }
    }

    private Person person() { return new Person(card.uid, card.username, card.name, card.icon); }

    private void copy(String text) {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("Harvana", text));
        toast("Copied");
    }

    private void shareProfile() {
        // A card picture (name, @username, tier and the profile QR) together with a message.
        ProfileShare.share(this, card.name, card.username, card.scored() ? card.trustPercent : -1, card.scored() ? tierName() : "");
    }

    private void toast(String s) { Toast.makeText(this, s == null ? "Something went wrong." : s, Toast.LENGTH_LONG).show(); }

    // ── Page ─────────────────────────────────────────────────────────────

    private void render() {
        root.removeAllViews();
        boolean mine = isMe();
        // The cover reaches the screen edges and the picture overlaps it, so the root must not clip to its padding.
        root.setClipToPadding(false);
        root.setClipChildren(false);
        root.addView(topArea(mine));
        root.addView(header(mine));
        root.addView(actionRow(mine));
        if (mine && selfData != null) root.addView(personalCard());
        root.addView(tabs());
        tabContent = new LinearLayout(this);
        tabContent.setOrientation(LinearLayout.VERTICAL);
        root.addView(tabContent);
        renderTab();
    }

    /** The wide picture at the top of the profile, edge to edge. A soft colour when there is none; on my own profile it says how to add one. */
    private View coverBanner(boolean mine) {
        int side = SocialUi.dp(this, 16);
        FrameLayout banner = new FrameLayout(this);
        GradientDrawable fallback = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[] {(UiColors.accent() & 0x00FFFFFF) | 0x66000000, UiColors.surface()});
        banner.setBackground(fallback);
        ImageView pic = new ImageView(this);
        pic.setScaleType(ImageView.ScaleType.CENTER_CROP);
        banner.addView(pic, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        if (card.cover.startsWith("http")) {
            try { com.bumptech.glide.Glide.with(this).load(card.cover).centerCrop().into(pic); } catch (Exception ignored) { }
            banner.setOnClickListener(v -> com.harvana.app.utils.ImageViewerActivity.open(this, card.cover));      // tap to see it whole
        }
        if (mine) {
            TextView edit = pill(card.cover.isEmpty() ? "Add cover" : "Change cover", UiColors.textPrimary());
            GradientDrawable chip = new GradientDrawable();
            chip.setColor(0x99000000);
            chip.setCornerRadius(SocialUi.dp(this, 14));
            edit.setBackground(chip);
            edit.setTextColor(0xFFFFFFFF);
            FrameLayout.LayoutParams ep = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.END | Gravity.BOTTOM);
            ep.setMargins(0, 0, side, SocialUi.dp(this, 10));
            edit.setClickable(true);
            edit.setOnClickListener(v -> startActivity(new Intent(this, EditProfileActivity.class)));
            banner.addView(edit, ep);
        }
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, coverHeight());
        lp.setMargins(-side, 0, -side, 0);
        banner.setLayoutParams(lp);
        return banner;
    }

    /** The cover is as wide as the screen and 2 high for every 5 wide, the shape people crop to in Edit profile. */
    private int coverHeight() { return Math.round(getResources().getDisplayMetrics().widthPixels / 2.5f); }

    /**
     * The cover with the round picture on it, as one frame: the picture is drawn after the cover, so it always sits on top, and it needs
     * no negative margin or drawing outside its parent (an earlier version hid half of it behind the cover).
     */
    private View topArea(boolean mine) {
        int size = SocialUi.dp(this, 40);           // profile avatar: 40 dp on the scale, on the left over the cover
        FrameLayout top = new FrameLayout(this);
        top.setClipChildren(false);
        top.addView(coverBanner(mine));
        FrameLayout.LayoutParams fp = new FrameLayout.LayoutParams(size, size, Gravity.TOP | Gravity.START);
        fp.topMargin = coverHeight() - size / 2;
        fp.setMarginStart(SocialUi.dp(this, 2));
        top.addView(avatarView(size), fp);
        top.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, coverHeight() + size / 2));
        return top;
    }

    private View avatarView(int size) {
        int tint = card.scored() ? ProfileTrust.tierColor(tierName()) : 0xFF8B98AD;
        FrameLayout face = new FrameLayout(this);
        TextView initials = SocialUi.text(this, initialsOf(), 15, true, 0xFFFFFFFF);
        initials.setGravity(Gravity.CENTER);
        GradientDrawable disc = new GradientDrawable();
        disc.setShape(GradientDrawable.OVAL);
        disc.setColors(new int[] {0xFF2BA597, 0xFF0B6B61});
        disc.setOrientation(GradientDrawable.Orientation.TL_BR);
        disc.setStroke(SocialUi.dp(this, 2), tint);
        initials.setBackground(disc);
        face.addView(initials, new FrameLayout.LayoutParams(size, size));
        ImageView photo = new ImageView(this);
        photo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        face.addView(photo, new FrameLayout.LayoutParams(size, size));
        if (card.icon.startsWith("http")) {
            try { com.bumptech.glide.Glide.with(this).load(card.icon).circleCrop().into(photo); initials.setVisibility(View.INVISIBLE); }
            catch (Exception ignored) { }
            face.setOnClickListener(v -> com.harvana.app.utils.ImageViewerActivity.open(this, card.icon));        // tap to see it whole
        }
        return face;
    }

    /**
     * The identity block, laid out like a social profile: name with the trust chip, @username, bio, a line of facts (where, age, since when)
     * and the follower counts. Left aligned under the picture.
     */
    private View header(boolean mine) {
        LinearLayout h = new LinearLayout(this);
        h.setOrientation(LinearLayout.VERTICAL);
        h.setPadding(SocialUi.dp(this, 2), SocialUi.dp(this, 6), 0, SocialUi.dp(this, 14));
        int tint = card.scored() ? ProfileTrust.tierColor(tierName()) : 0xFF8B98AD;

        LinearLayout nameRow = new LinearLayout(this);
        nameRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView name = SocialUi.text(this, card.name.isEmpty() ? (card.username.isEmpty() ? "Member" : "@" + card.username) : card.name, 22, true, UiColors.textPrimary());
        name.setMaxLines(2);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        nameRow.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView chip = pill(card.scored() ? ("New".equals(tierName()) ? "New · " + card.trustPercent + "%" : card.trustPercent + "% · " + tierName()) : "New · not scored yet", tint);
        ((LinearLayout.LayoutParams) chip.getLayoutParams()).setMarginStart(SocialUi.dp(this, 8));
        // The tier's medal beside the score; tap for every tier (reached in colour, the rest dimmed).
        com.harvana.app.trust.TierBadges.decorate(this, chip, tierName(), card.trustPercent, card.scored());
        nameRow.addView(chip);
        h.addView(nameRow);

        String status = mine ? "" : PublicProfileStore.presence(card.lastActiveAt, System.currentTimeMillis());
        String handle = card.username.isEmpty() ? "" : "@" + card.username;
        String second = handle + (!handle.isEmpty() && !status.isEmpty() ? "  \u00b7  " : "") + status;
        if (!second.isEmpty()) {
            TextView t = SocialUi.text(this, second, 14, false, UiColors.textSecondary());
            t.setPadding(0, SocialUi.dp(this, 2), 0, 0);
            h.addView(t);
        }
        if (!mine) {
            String rel = card.iFollow && card.followsMe ? "Friends" : card.iFollow ? "You follow" : card.followsMe ? "Follows you" : "";
            if (!rel.isEmpty()) {
                TextView r = SocialUi.text(this, rel, 12.5f, true, UiColors.accent());
                r.setPadding(0, SocialUi.dp(this, 4), 0, 0);
                h.addView(r);
            }
        }
        if (!card.bio.isEmpty()) {
            TextView bio = SocialUi.text(this, card.bio, 15, false, UiColors.textPrimary());
            bio.setPadding(0, SocialUi.dp(this, 12), 0, 0);
            h.addView(bio);
        }

        // Where, age, since when. Country and state are always public; city and age only if the member allows them.
        java.util.List<String> facts = new java.util.ArrayList<>();
        String where = card.country.isEmpty() ? "" : (card.state.isEmpty() ? card.country : card.state + ", " + card.country);
        if (mine && selfData != null && selfData.get("city") != null && !String.valueOf(selfData.get("city")).trim().isEmpty() && !where.isEmpty()) {
            where = String.valueOf(selfData.get("city")).trim() + ", " + where;
        } else if (!card.city.isEmpty() && !where.isEmpty()) {
            where = card.city + ", " + where;
        }
        if (!where.isEmpty()) facts.add(where);
        if (!mine && card.showAge && card.age >= 0) facts.add(card.age + " years old");
        if (card.memberSince > 0) facts.add("Joined " + new SimpleDateFormat("MMM yyyy", Locale.US).format(new Date(card.memberSince)));
        if (!facts.isEmpty()) {
            TextView meta = SocialUi.text(this, android.text.TextUtils.join("   \u00b7   ", facts), 13, false, UiColors.textSecondary());
            meta.setPadding(0, SocialUi.dp(this, 10), 0, 0);
            h.addView(meta);
        }

        // Counts. Mine: who I follow and who follows me (opens Friends). Others: the public counts only.
        final TextView counts = SocialUi.text(this, "", 14, false, UiColors.textSecondary());
        counts.setPadding(0, SocialUi.dp(this, 10), 0, 0);
        if (mine) {
            counts.setText(countsLine);
            SocialStore.following((following, e1) -> SocialStore.followers((followers, e2) -> runOnUiThread(() -> {
                countsLine = countsText(following == null ? 0 : following.size(), "Following", followers == null ? 0 : followers.size(), "Followers");
                counts.setText(countsLine);
            })));
            counts.setOnClickListener(v -> startActivity(new Intent(this, FriendsActivity.class)));
        } else if (card.groups >= 0) {
            counts.setText(countsText(card.groups, card.groups == 1 ? "Group" : "Groups", Math.max(0, card.cycles), "Cycles completed"));
        }
        if (counts.getText().length() > 0 || mine) h.addView(counts);
        return h;
    }

    private CharSequence countsLine = "";

    /** "188 Following   123 Followers" with the numbers in bold. */
    private CharSequence countsText(int a, String aLabel, int b, String bLabel) {
        android.text.SpannableStringBuilder sb = new android.text.SpannableStringBuilder();
        int[] parts = {a, b};
        String[] labels = {aLabel, bLabel};
        for (int i = 0; i < 2; i++) {
            if (i > 0) sb.append("   ");
            int start = sb.length();
            sb.append(String.valueOf(parts[i]));
            sb.setSpan(new android.text.style.StyleSpan(Typeface.BOLD), start, sb.length(), 0);
            sb.setSpan(new android.text.style.ForegroundColorSpan(UiColors.textPrimary()), start, sb.length(), 0);
            sb.append(' ').append(labels[i]);
        }
        return sb;
    }

    /** The tier shown for a member. Before the network has scored them it is "New", never a made-up Bronze. */
    private String tierName() {
        if (!card.trustTier.isEmpty()) return card.trustTier;
        return card.scored() ? ProfileTrust.tierFor(card.trustPercent) : "New";
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

    private String initialsOf() {
        String s = card.name.isEmpty() ? card.username : card.name;
        String[] parts = s.trim().split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) return "?";
        if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase(Locale.US);
        return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase(Locale.US);
    }

    // ── Action tiles ─────────────────────────────────────────────────────

    private View actionRow(boolean mine) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 0, 0, SocialUi.dp(this, 14));
        if (mine) {
            // Two wide pill buttons, like a social profile. Settings, the QR code and privacy are in the menu at the top right.
            row.addView(pillButton("Share", v -> shareProfile()));
            row.addView(pillButton("Edit profile", v -> startActivity(new Intent(this, EditProfileActivity.class))));
            return row;
        }
        TextView follow = actionTile(R.drawable.ic_pf_person_add, card.iFollow ? "Unfollow" : (card.followsMe ? "Follow back" : "Follow"), null);
        follow.setOnClickListener(v -> {
            follow.setEnabled(false);
            if (card.iFollow) SocialStore.unfollow(card.uid, (ok, e) -> runOnUiThread(() -> { if (!ok) toast(e); load(); }));
            else SocialStore.follow(person(), (ok, e) -> runOnUiThread(() -> { if (!ok) toast(e); load(); }));
        });
        row.addView(follow);
        TextView pinch = actionTile(R.drawable.ic_pf_bell, "Pinch", null);
        if (!card.iFollow) { pinch.setAlpha(0.45f); pinch.setOnClickListener(v -> toast("Follow " + (card.name.isEmpty() ? "this member" : card.name) + " to pinch them.")); }
        else if (PinchStore.waitMs(this, card.uid) > 0) { pinch.setText("Pinched"); pinch.setAlpha(0.6f); pinch.setEnabled(false); }
        else pinch.setOnClickListener(v -> {
            pinch.setEnabled(false);
            PinchStore.send(this, person(), (ok, err) -> runOnUiThread(() -> {
                if (ok) { pinch.setText("Pinched"); pinch.setAlpha(0.6f); toast(person().name + " will get a notification."); }
                else { pinch.setEnabled(true); toast(err); }
            }));
        });
        row.addView(pinch);
        TextView send = actionTile(R.drawable.ic_pf_send, "Send", null);
        if (card.wallet.isEmpty()) { send.setAlpha(0.45f); send.setOnClickListener(v -> toast("This member has no wallet linked yet.")); }
        else send.setOnClickListener(v -> startActivity(new Intent(this, SendAssetActivity.class).putExtra("toAddress", card.wallet)));
        row.addView(send);
        row.addView(actionTile(R.drawable.ic_pf_share, "Share", v -> shareProfile()));
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.addView(row);
        final TextView block = SocialUi.text(this, "Block", 13.5f, true, UiColors.textSecondary());
        block.setGravity(Gravity.CENTER);
        block.setPadding(0, SocialUi.dp(this, 4), 0, SocialUi.dp(this, 10));
        SocialStore.blockedIds((ids, err) -> runOnUiThread(() -> {
            final boolean isBlocked = ids != null && ids.contains(card.uid);
            block.setText(isBlocked ? "Unblock this member" : "Block this member");
            block.setOnClickListener(v -> {
                if (isBlocked) SocialStore.unblock(card.uid, (ok, e) -> runOnUiThread(() -> { toast(ok ? "Unblocked." : e); load(); }));
                else new com.harvana.app.ui.SheetDialog.Builder(this).setTitle("Block " + (card.name.isEmpty() ? "this member" : card.name) + "?")
                    .setMessage("They are removed from your followers, you stop following them, and they cannot follow you again. Their Learn posts are hidden from you. They are not told.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Block", (d, w) -> SocialStore.block(person(), (ok, e) -> runOnUiThread(() -> { toast(ok ? "Blocked." : e); load(); }))).show();
            });
        }));
        col.addView(block);
        return col;
    }

    /** An icon over a label in a rounded block; the whole block is the tap target. The label is the tile's text, so it can change. */
    private TextView pillButton(String label, View.OnClickListener click) {
        TextView t = SocialUi.text(this, label, 15, true, UiColors.textPrimary());
        t.setGravity(Gravity.CENTER);
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(SocialUi.dp(this, 24));
        g.setColor(0x00000000);
        g.setStroke(SocialUi.dp(this, 1), (UiColors.textTertiary() & 0x00FFFFFF) | 0xAA000000);
        t.setBackground(new android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf((UiColors.accent() & 0x00FFFFFF) | 0x33000000), g, null));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, SocialUi.dp(this, 44), 1f);
        lp.setMargins(SocialUi.dp(this, 4), 0, SocialUi.dp(this, 4), 0);
        t.setLayoutParams(lp);
        t.setClickable(true);
        t.setOnClickListener(click);
        return t;
    }

    private TextView actionTile(int icon, String label, View.OnClickListener click) {
        TextView t = new TextView(this);
        t.setText(label);
        t.setTextSize(12);
        t.setTypeface(null, Typeface.BOLD);
        t.setTextColor(UiColors.textPrimary());
        t.setGravity(Gravity.CENTER);
        t.setMaxLines(1);
        android.graphics.drawable.Drawable d = ContextCompat.getDrawable(this, icon);
        if (d != null) {
            d = d.mutate();
            androidx.core.graphics.drawable.DrawableCompat.setTint(d, UiColors.textPrimary());
            int s = SocialUi.dp(this, 24);
            d.setBounds(0, 0, s, s);
            t.setCompoundDrawables(null, d, null, null);
        }
        t.setCompoundDrawablePadding(SocialUi.dp(this, 6));
        t.setPadding(0, SocialUi.dp(this, 14), 0, SocialUi.dp(this, 12));
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(SocialUi.dp(this, 16));
        g.setColor(UiColors.surface());
        g.setStroke(SocialUi.dp(this, 1), (UiColors.textTertiary() & 0x00FFFFFF) | 0x33000000);
        t.setBackground(new android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf((UiColors.accent() & 0x00FFFFFF) | 0x33000000), g, null));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(SocialUi.dp(this, 4), 0, SocialUi.dp(this, 4), 0);
        t.setLayoutParams(lp);
        t.setClickable(true);
        t.setFocusable(true);
        if (click != null) t.setOnClickListener(click);
        return t;
    }

    // ── Foldable cards ───────────────────────────────────────────────────

    private final java.util.Set<String> opened = new java.util.HashSet<>();

    /** A card that shows a title and a one-line summary, and opens to reveal {@code body}. Whether it is open survives a refresh. */
    private LinearLayout fold(String key, String title, String summary, View body) {
        final LinearLayout c = SocialUi.card(this);
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout words = new LinearLayout(this);
        words.setOrientation(LinearLayout.VERTICAL);
        words.addView(SocialUi.text(this, title, 16, true, UiColors.textPrimary()));
        if (summary != null && !summary.isEmpty()) {
            TextView sub = SocialUi.text(this, summary, 13, false, UiColors.textSecondary());
            sub.setPadding(0, SocialUi.dp(this, 2), 0, 0);
            words.addView(sub);
        }
        head.addView(words, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        final TextView chevron = SocialUi.text(this, opened.contains(key) ? "\u25B4" : "\u25BE", 18, true, UiColors.accent());
        head.addView(chevron);
        head.setClickable(true);
        c.addView(head);
        body.setVisibility(opened.contains(key) ? View.VISIBLE : View.GONE);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bp.topMargin = SocialUi.dp(this, 8);
        c.addView(body, bp);
        head.setOnClickListener(v -> {
            boolean open = body.getVisibility() != View.VISIBLE;
            body.setVisibility(open ? View.VISIBLE : View.GONE);
            chevron.setText(open ? "\u25B4" : "\u25BE");
            if (open) opened.add(key); else opened.remove(key);
        });
        return c;
    }

    /** My own contact and identity details: only I see these, so they sit folded away under one heading. */
    private View personalCard() {
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        String phone = PublicProfileStore.tidyPhone(str(selfData, "phone"));
        String email = str(selfData, "email");
        String job = str(selfData, "occupation");
        String address = str(selfData, "address");
        String dob = str(selfData, "dob");
        String pid = str(selfData, "pid");
        int rows = 0;
        if (!phone.isEmpty()) { body.addView(infoRow(phone, "Mobile", 0, v -> startActivity(new Intent(this, ContactCardActivity.class)))); rows++; }
        if (!email.isEmpty() && !"null".equals(email)) { body.addView(infoRow(email, "Email", 0, null)); rows++; }
        if (!job.isEmpty()) { body.addView(infoRow(job, "Occupation", 0, null)); rows++; }
        if (!address.isEmpty()) { body.addView(infoRow(address, "Address", 0, null)); rows++; }
        Date born = AgeOf.parse(dob);
        if (born != null) {
            int age = AgeOf.fromDob(dob, java.util.Calendar.getInstance());
            body.addView(infoRow(new SimpleDateFormat("MMM d, yyyy", Locale.US).format(born) + (age >= 0 ? " (" + age + " years old)" : ""), "Birthday", 0, null));
            rows++;
        }
        if (!pid.isEmpty()) { body.addView(infoRow(pid, "Harvana ID", 0, null)); rows++; }
        MaterialButton edit = SocialUi.button(this, "Edit these details", true);
        edit.setOnClickListener(v -> startActivity(new Intent(this, EditProfileActivity.class)));
        LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, SocialUi.dp(this, 46));
        ep.topMargin = SocialUi.dp(this, 8);
        body.addView(edit, ep);
        return fold("personal", "Personal details", "Only you can see these", body);
    }

    private static String str(Map<String, Object> m, String k) {
        Object v = m == null ? null : m.get(k);
        return v == null || "null".equals(String.valueOf(v)) ? "" : String.valueOf(v).trim();
    }

    // ── Sections that used to live on the separate personal profile screen ──
    // Friends, the trust facts, totals across groups, the business-profile card and my Learn articles are now here, for my own
    // profile only; other people's profiles never show them.

    // What each section last showed, kept for the life of this screen so a refresh does not bring the placeholder back.
    private String friendsLineCache, savedCache, loansCache;
    private com.harvana.app.trust.NetworkTrust.Score factsCache;
    private java.util.List<com.google.firebase.firestore.DocumentSnapshot> learnCache;

    private View friendsRow() {
        final LinearLayout c = SocialUi.card(this);
        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout words = new LinearLayout(this);
        words.setOrientation(LinearLayout.VERTICAL);
        words.addView(SocialUi.text(this, "Friends", 16, true, UiColors.textPrimary()));
        final TextView sub = SocialUi.text(this, friendsLineCache != null ? friendsLineCache : "Loading…", 13, false, UiColors.textSecondary());
        if (friendsLineCache == null) com.harvana.app.utils.Shimmer.watch(sub);   // placeholder only the first time
        sub.setPadding(0, SocialUi.dp(this, 2), 0, 0);
        words.addView(sub);
        head.addView(words, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        head.addView(SocialUi.text(this, "See all  \u203A", 13, true, UiColors.accent()));
        c.addView(head);
        c.setClickable(true);
        c.setOnClickListener(v -> startActivity(new Intent(this, FriendsActivity.class)));
        SocialStore.following((following, e1) -> SocialStore.followers((followers, e2) -> runOnUiThread(() -> {
            java.util.Set<String> back = new java.util.HashSet<>();
            if (followers != null) for (Person p : followers) back.add(p.uid);
            java.util.List<String> names = new java.util.ArrayList<>();
            if (following != null) for (Person p : following) if (back.contains(p.uid)) names.add(p.name);
            friendsLineCache = names.isEmpty() ? "No friends yet. A friend is someone you follow who follows you back."
                    : names.size() + (names.size() == 1 ? " friend" : " friends") + "  \u00b7  "
                    + android.text.TextUtils.join(", ", names.subList(0, Math.min(2, names.size()))) + (names.size() > 2 ? " and " + (names.size() - 2) + " more" : "");
            sub.setText(friendsLineCache);
        })));
        return c;
    }

    /** Device, vouches, badges and established, as the network reports them (the facts behind the score). */
    private View networkFactsCard() {
        final LinearLayout c = SocialUi.card(this);
        c.addView(SocialUi.text(this, "On the network", 16, true, UiColors.textPrimary()));
        final LinearLayout rows = new LinearLayout(this);
        rows.setOrientation(LinearLayout.VERTICAL);
        if (factsCache != null) fillFacts(rows, factsCache);
        else rows.addView(com.harvana.app.utils.Shimmer.lines(this, 4));
        c.addView(rows);
        String addr = com.harvana.app.wallet.WalletManager.getActiveWalletAddress(this);
        if (addr == null || addr.isEmpty()) {
            rows.removeAllViews();
            rows.addView(SocialUi.text(this, "Create a wallet to build a record on the network.", 13, false, UiColors.textSecondary()));
            return c;
        }
        com.harvana.app.trust.NetworkTrust.wallet(addr, score -> runOnUiThread(() -> {
            factsCache = score;
            fillFacts(rows, score);
        }));
        return c;
    }

    private void fillFacts(LinearLayout rows, com.harvana.app.trust.NetworkTrust.Score score) {
        rows.removeAllViews();
        rows.addView(factRow("Device", score.bound ? "Bound" : "Not bound"));
        rows.addView(factRow("Vouches", String.valueOf(score.vouches)));
        rows.addView(factRow("Badges", String.valueOf(score.badges)));
        rows.addView(factRow("Established", score.established ? "Yes" : "No"));
        if (isMe()) myWalletRows(rows);
        if (isMe() && (!score.bound || !com.harvana.app.sync.NodeLinkManager.isLinked(this))) {
            // Binding ties the wallet to this phone (it raises the base trust score). Offered right here, not only on the Trust screen.
            TextView bind = pillButton("Bind this device", v -> {
                v.setEnabled(false);
                ((TextView) v).setText("Binding…");
                com.harvana.app.sync.NodeLinkManager.bindNow(this, status -> runOnUiThread(() -> {
                    toast(status == null || status.isEmpty() ? "Done." : status);
                    factsCache = null;
                    load();
                }));
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, SocialUi.dp(this, 44));
            lp.topMargin = SocialUi.dp(this, 12);
            bind.setLayoutParams(lp);
            rows.addView(bind);
            rows.addView(SocialUi.text(this, "Ties your wallet to this phone and lifts the base score. No ID is uploaded. A phone belongs to one person; all your own wallets can be bound on it.", 12, false, UiColors.textSecondary()));
        }
    }

    /**
     * The wallets the network counts as mine. Each wallet I sign in with is added to me (signed by that wallet), never
     * swapped in: my profile and history stay on my main wallet, and what any of them does counts toward me.
     */
    private void myWalletRows(LinearLayout rows) {
        final LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        rows.addView(list);
        com.harvana.app.sync.NodeIdentityClient.myWallets(new com.harvana.app.sync.NodeIdentityClient.Callback() {
            @Override public void onSuccess(org.json.JSONObject j) {
                org.json.JSONArray ws = j.optJSONArray("wallets");
                runOnUiThread(() -> {
                    list.removeAllViews();
                    if (ws == null || ws.length() == 0) return;
                    list.addView(factRow("Wallets", String.valueOf(ws.length())));
                    for (int i = 0; i < ws.length(); i++) {
                        org.json.JSONObject w = ws.optJSONObject(i);
                        if (w == null) continue;
                        String a = w.optString("address", "");
                        String shortA = a.length() > 14 ? a.substring(0, 8) + "\u2026" + a.substring(a.length() - 6) : a;
                        list.addView(factRow(w.optBoolean("main") ? "Main wallet" : "Also yours", shortA));
                    }
                    list.addView(SocialUi.text(PublicProfileActivity.this,
                        "Every wallet you sign in with is added to you, signed by that wallet. Your profile stays on your main wallet, and what any of them does counts toward your trust and badges.",
                        12, false, UiColors.textSecondary()));
                });
            }
            @Override public void onError(String e) { }      // older network or offline: the list just isn't shown
        });
    }

    private View factRow(String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setPadding(0, SocialUi.dp(this, 10), 0, 0);
        row.addView(SocialUi.text(this, label, 14, false, UiColors.textSecondary()), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(SocialUi.text(this, value, 14, true, UiColors.textPrimary()));
        return row;
    }

    /** What I have saved and borrowed across my groups on the network. Private: only shown on my own profile. */
    private View totalsCard() {
        LinearLayout c = SocialUi.card(this);
        c.addView(SocialUi.text(this, "Across all groups", 16, true, UiColors.textPrimary()));
        LinearLayout row = new LinearLayout(this);
        row.setPadding(0, SocialUi.dp(this, 10), 0, 0);
        final TextView saved = SocialUi.text(this, savedCache != null ? savedCache : "Loading…", 20, true, UiColors.textPrimary());
        final TextView loans = SocialUi.text(this, loansCache != null ? loansCache : "Loading…", 20, true, UiColors.textPrimary());
        if (savedCache == null) { com.harvana.app.utils.Shimmer.watch(saved); com.harvana.app.utils.Shimmer.watch(loans); }
        row.addView(totalTile("TOTAL SAVED", saved), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(totalTile("TOTAL LOANS TAKEN", loans), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        c.addView(row);
        com.google.firebase.auth.FirebaseUser me = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        String wallet = com.harvana.app.wallet.WalletManager.getActiveWalletAddress(this);
        if (me == null) { saved.setText("0 " + com.harvana.app.CoinInfo.ticker()); loans.setText("0 " + com.harvana.app.CoinInfo.ticker()); return c; }
        com.harvana.app.sync.NodePortfolio.load(this, me.getUid(), wallet, p -> runOnUiThread(() -> {
            savedCache = harv(p.savedHarv());
            loansCache = harv(p.outstandingHarv());
            saved.setText(savedCache);
            loans.setText(loansCache);
        }));
        return c;
    }

    private static String harv(double v) {
        return new java.text.DecimalFormat("#,##0.##", new java.text.DecimalFormatSymbols(Locale.US)).format(v) + " " + com.harvana.app.CoinInfo.ticker();
    }

    private View totalTile(String label, TextView value) {
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        TextView l = SocialUi.text(this, label, 10, true, UiColors.textSecondary());
        l.setLetterSpacing(0.08f);
        t.addView(l);
        value.setPadding(0, SocialUi.dp(this, 4), 0, 0);
        t.addView(value);
        return t;
    }

    private View businessCard() {
        LinearLayout c = SocialUi.card(this);
        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.addView(SocialUi.text(this, "Register Business Profile", 16, true, UiColors.textPrimary()), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        head.addView(pill("Coming soon", UiColors.textSecondary()));
        c.addView(head);
        TextView sub = SocialUi.text(this, "Show your company instead of your personal name.", 13, false, UiColors.textSecondary());
        sub.setPadding(0, SocialUi.dp(this, 6), 0, SocialUi.dp(this, 8));
        c.addView(sub);
        for (String line : new String[] {"CAC-verified company name shown to members", "Company logo used as group avatar", "Verified Business badge on all your groups"}) {
            TextView t = SocialUi.text(this, "\u2713  " + line, 13, false, UiColors.textPrimary());
            t.setPadding(0, SocialUi.dp(this, 4), 0, 0);
            c.addView(t);
        }
        return c;
    }

    /** The Learn Hub articles I have published. */
    private View learnCard() {
        final LinearLayout c = SocialUi.card(this);
        c.addView(SocialUi.text(this, "My Learn contributions", 16, true, UiColors.textPrimary()));
        final LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        if (learnCache != null) fillLearn(list, learnCache);
        else list.addView(com.harvana.app.utils.Shimmer.rows(this, 2));
        c.addView(list);
        com.google.firebase.auth.FirebaseUser me = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (me == null) { list.removeAllViews(); return c; }
        com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("LEARN_CONTENT")
            .whereEqualTo("authorUid", me.getUid()).limit(30).get()
            .addOnSuccessListener(qs -> {
                java.util.List<com.google.firebase.firestore.DocumentSnapshot> docs = new java.util.ArrayList<>(qs.getDocuments());
                java.util.Collections.sort(docs, (x, y) -> Long.compare(num(y.get("createdAt")), num(x.get("createdAt"))));
                learnCache = docs;
                fillLearn(list, docs);
            })
            .addOnFailureListener(e -> {
                if (learnCache != null) return;     // keep what is shown if a refresh fails
                list.removeAllViews();
                list.addView(SocialUi.text(this, "Couldn't load your articles.", 13, false, UiColors.textSecondary()));
            });
        return c;
    }

    private void fillLearn(LinearLayout list, java.util.List<com.google.firebase.firestore.DocumentSnapshot> docs) {
        list.removeAllViews();
        if (docs.isEmpty()) {
            TextView none = SocialUi.text(this, "No contributions yet. Articles you publish in Learn Hub show here.", 13, false, UiColors.textSecondary());
            none.setPadding(0, SocialUi.dp(this, 8), 0, SocialUi.dp(this, 8));
            list.addView(none);
            MaterialButton write = SocialUi.button(this, "Write an article", true);
            write.setOnClickListener(v -> startActivity(new Intent(this, com.harvana.app.CreateLearnhubContentActivity.class)));
            list.addView(write, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, SocialUi.dp(this, 46)));
            return;
        }
        for (com.google.firebase.firestore.DocumentSnapshot d : docs) list.addView(learnRow(d));
    }

    private View learnRow(final com.google.firebase.firestore.DocumentSnapshot d) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, SocialUi.dp(this, 12), 0, SocialUi.dp(this, 4));
        final String title = d.getString("title") == null ? "Untitled" : d.getString("title");
        final long views = num(d.get("views"));
        final String vis = visibilityOf(d);
        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        text.addView(SocialUi.text(this, title, 15, true, UiColors.textPrimary()));
        TextView meta = SocialUi.text(this, (d.getString("category") == null ? "General" : d.getString("category"))
                + "  \u00b7  " + views + (views == 1 ? " view" : " views") + "  \u00b7  " + visibilityLabel(vis), 12, false, UiColors.textSecondary());
        meta.setPadding(0, SocialUi.dp(this, 2), 0, 0);
        text.addView(meta);
        row.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView more = SocialUi.text(this, "\u22EE", 22, true, UiColors.textSecondary());
        more.setGravity(Gravity.CENTER);
        more.setPadding(SocialUi.dp(this, 12), SocialUi.dp(this, 4), SocialUi.dp(this, 4), SocialUi.dp(this, 4));
        more.setOnClickListener(v -> learnMenu(v, d, title, vis));
        row.addView(more);
        row.setClickable(true);
        row.setOnClickListener(v -> startActivity(new Intent(this, com.harvana.app.LearnReadActivity.class)
                .putExtra(com.harvana.app.LearnReadActivity.EXTRA_ID, d.getId())
                .putExtra(com.harvana.app.LearnReadActivity.EXTRA_TITLE, title)
                .putExtra(com.harvana.app.LearnReadActivity.EXTRA_BODY, d.getString("description") == null ? "" : d.getString("description"))
                .putExtra(com.harvana.app.LearnReadActivity.EXTRA_CATEGORY, d.getString("category") == null ? "" : d.getString("category"))
                .putExtra(com.harvana.app.LearnReadActivity.EXTRA_IMAGE, d.getString("imageUrl") == null ? "" : d.getString("imageUrl"))
                .putExtra(com.harvana.app.LearnReadActivity.EXTRA_VIDEO, d.getString("videoUrl") == null ? "" : d.getString("videoUrl"))
                .putExtra(com.harvana.app.LearnReadActivity.EXTRA_AUTHOR, d.getString("authorUid") == null ? "" : d.getString("authorUid"))
                .putExtra(com.harvana.app.LearnReadActivity.EXTRA_CREATED, num(d.get("createdAt")))
                .putExtra(com.harvana.app.LearnReadActivity.EXTRA_VIEWS, views)));
        return row;
    }

    private static String visibilityOf(com.google.firebase.firestore.DocumentSnapshot d) {
        String v = d.getString("visibility");
        return "followers".equals(v) || "private".equals(v) ? v : "public";
    }

    private static String visibilityLabel(String v) {
        return "followers".equals(v) ? "Followers" : "private".equals(v) ? "Only me" : "Public";
    }

    /** More menu on one of my articles: open, edit, who can see it, delete. */
    private void learnMenu(View anchor, final com.google.firebase.firestore.DocumentSnapshot d, final String title, final String vis) {
        android.widget.PopupMenu m = new android.widget.PopupMenu(this, anchor);
        m.getMenu().add(0, 1, 0, "Edit");
        m.getMenu().add(0, 2, 1, "Visibility: " + visibilityLabel(vis));
        m.getMenu().add(0, 3, 2, "Copy text");
        m.getMenu().add(0, 4, 3, "Delete");
        m.setOnMenuItemClickListener(it -> {
            switch (it.getItemId()) {
                case 1: editLearn(d); break;
                case 2: pickVisibility(d, vis); break;
                case 3: {
                    android.content.ClipboardManager cm = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    if (cm != null) cm.setPrimaryClip(android.content.ClipData.newPlainText("article", title + "\n\n" + (d.getString("description") == null ? "" : d.getString("description"))));
                    toast("Copied.");
                    break;
                }
                default:
                    new com.harvana.app.ui.SheetDialog.Builder(this).setTitle("Delete this article?")
                        .setMessage("\u201c" + title + "\u201d is removed for everyone. A copy is kept privately for one year, then erased.")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Delete", (dl, w) -> com.harvana.app.firestore.SoftDelete.archiveThenDelete("LEARN_CONTENT", d.getReference())
                            .addOnSuccessListener(x -> { learnCache.remove(d); toast("Deleted."); load(); })
                            .addOnFailureListener(e -> toast("Couldn't delete it. Try again."))).show();
            }
            return true;
        });
        m.show();
    }

    private void pickVisibility(final com.google.firebase.firestore.DocumentSnapshot d, String current) {
        final String[] keys = {"public", "followers", "private"};
        final String[] names = {"Public \u2014 everyone in Learn", "Followers \u2014 only people who follow me", "Only me \u2014 hidden from everyone else"};
        int checked = "followers".equals(current) ? 1 : "private".equals(current) ? 2 : 0;
        new com.harvana.app.ui.SheetDialog.Builder(this).setTitle("Who can see this article?")
            .setSingleChoiceItems(names, checked, (dl, which) -> {
                dl.dismiss();
                java.util.Map<String, Object> up = new java.util.HashMap<>();
                up.put("visibility", keys[which]);
                d.getReference().update(up)
                    .addOnSuccessListener(x -> { toast("Now: " + visibilityLabel(keys[which]) + "."); load(); })
                    .addOnFailureListener(e -> toast("Couldn't change it. The new rules may not be published yet."));
            }).setNegativeButton("Cancel", null).show();
    }

    private void editLearn(final com.google.firebase.firestore.DocumentSnapshot d) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = SocialUi.dp(this, 20);
        box.setPadding(pad, SocialUi.dp(this, 8), pad, 0);
        final android.widget.EditText t = new com.google.android.material.textfield.TextInputEditText(this);
        t.setHint("Title");
        t.setText(d.getString("title"));
        t.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(120)});
        final android.widget.EditText b = new com.google.android.material.textfield.TextInputEditText(this);
        b.setHint("Article text");
        b.setText(d.getString("description"));
        b.setMinLines(6);
        b.setGravity(Gravity.TOP);
        b.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(8000)});
        box.addView(t);
        box.addView(b);
        new com.harvana.app.ui.SheetDialog.Builder(this).setTitle("Edit article").setView(box)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save", (dl, w) -> {
                String nt = t.getText().toString().trim(), nb = b.getText().toString().trim();
                if (nt.isEmpty() || nb.isEmpty()) { toast("Title and text can't be empty."); return; }
                java.util.Map<String, Object> up = new java.util.HashMap<>();
                up.put("title", nt);
                up.put("description", nb);
                up.put("editedAt", System.currentTimeMillis());
                d.getReference().update(up)
                    .addOnSuccessListener(x -> { toast("Saved."); load(); })
                    .addOnFailureListener(e -> toast("Couldn't save. The new rules may not be published yet."));
            }).show();
    }

    private static long num(Object o) { return o instanceof Number ? ((Number) o).longValue() : 0L; }

    // ── Info card ────────────────────────────────────────────────────────

    private View infoCard(boolean mine) {
        LinearLayout c = SocialUi.card(this);
        boolean any = false;
        if (!card.bio.isEmpty()) { c.addView(infoRow(card.bio, "Bio", 0, null)); any = true; }
        if (!card.username.isEmpty()) {
            c.addView(infoRow("@" + card.username, "Username", mine ? R.drawable.ic_pf_qr : 0,
                    mine ? v -> startActivity(new Intent(this, ProfileQrActivity.class)) : null));
            any = true;
        }
        String where = card.country.isEmpty() ? "" : (card.state.isEmpty() ? card.country : card.state + ", " + card.country);
        if (mine && selfData != null && selfData.get("city") != null && !String.valueOf(selfData.get("city")).trim().isEmpty() && !where.isEmpty()) {
            where = String.valueOf(selfData.get("city")).trim() + ", " + where;
        } else if (!card.city.isEmpty() && !where.isEmpty()) {
            where = card.city + ", " + where;
        }
        if (!where.isEmpty()) { c.addView(infoRow(where, "Location", 0, null)); any = true; }
        if (!mine && card.showAge && card.age >= 0) { c.addView(infoRow(card.age + " years old", "Age", 0, null)); any = true; }
        if (card.memberSince > 0) {
            c.addView(infoRow(new SimpleDateFormat("MMM yyyy", Locale.US).format(new Date(card.memberSince)), "Member since", 0, null));
            any = true;
        }
        if (!any) c.addView(SocialUi.text(this, "Nothing to show yet.", 13, false, UiColors.textSecondary()));
        return c;
    }

    /** Value on top, small caption under it: the way contact details are laid out in a chat app. */
    private View infoRow(String value, String caption, int trailingIcon, View.OnClickListener click) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, SocialUi.dp(this, 8), 0, SocialUi.dp(this, 8));
        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        text.addView(SocialUi.text(this, value, 16, false, UiColors.textPrimary()));
        TextView cap = SocialUi.text(this, caption, 13, false, UiColors.textSecondary());
        cap.setPadding(0, SocialUi.dp(this, 2), 0, 0);
        text.addView(cap);
        row.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (trailingIcon != 0) {
            ImageView iv = new ImageView(this);
            iv.setImageResource(trailingIcon);
            iv.setColorFilter(UiColors.accent());
            row.addView(iv, new LinearLayout.LayoutParams(SocialUi.dp(this, 26), SocialUi.dp(this, 26)));
        }
        if (click != null) { row.setClickable(true); row.setOnClickListener(click); }
        return row;
    }

    // ── Tabs ─────────────────────────────────────────────────────────────

    private View tabs() {
        LinearLayout wrap = new LinearLayout(this);
        wrap.setGravity(Gravity.CENTER);
        wrap.setPadding(0, SocialUi.dp(this, 4), 0, SocialUi.dp(this, 12));
        LinearLayout pillBar = new LinearLayout(this);
        pillBar.setOrientation(LinearLayout.HORIZONTAL);
        pillBar.setPadding(SocialUi.dp(this, 4), SocialUi.dp(this, 4), SocialUi.dp(this, 4), SocialUi.dp(this, 4));
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(SocialUi.dp(this, 26));
        g.setColor(UiColors.surface());
        pillBar.setBackground(g);
        // Your own profile has a fourth tab for your data: wallets, device, network facts, activity and devices.
        String[] names = isMe() ? new String[]{"Trust", "Activity", "Groups", "My data"} : new String[]{"Trust", "Activity", "Groups"};
        int side = isMe() ? 13 : 20;
        final TextView[] labels = new TextView[names.length];
        for (int i = 0; i < names.length; i++) {
            final int idx = i;
            TextView t = SocialUi.text(this, names[i], 14, true, UiColors.textSecondary());
            t.setGravity(Gravity.CENTER);
            t.setPadding(SocialUi.dp(this, side), SocialUi.dp(this, 9), SocialUi.dp(this, side), SocialUi.dp(this, 9));
            t.setClickable(true);
            labels[i] = t;
            t.setOnClickListener(v -> { tab = idx; styleTabs(labels); renderTab(); });
            pillBar.addView(t);
        }
        styleTabs(labels);
        wrap.addView(pillBar);
        return wrap;
    }

    private void styleTabs(TextView[] labels) {
        for (int i = 0; i < labels.length; i++) {
            boolean on = i == tab;
            GradientDrawable g = new GradientDrawable();
            g.setCornerRadius(SocialUi.dp(this, 22));
            g.setColor(on ? ((UiColors.accent() & 0x00FFFFFF) | 0x33000000) : android.graphics.Color.TRANSPARENT);
            labels[i].setBackground(g);
            labels[i].setTextColor(on ? UiColors.accent() : UiColors.textSecondary());
        }
    }

    private void renderTab() {
        if (tabContent == null) return;
        tabContent.removeAllViews();
        if (tab == 0) {
            tabContent.addView(trustCard());
            tabContent.addView(milestonesCard());
        }
        else if (tab == 1) {
            tabContent.addView(activityCard());
            if (isMe()) tabContent.addView(learnCard());
        } else if (tab == 2) {
            tabContent.addView(sharedCard());
            if (isMe()) { tabContent.addView(totalsCard()); tabContent.addView(businessCard()); }
        } else {
            tabContent.addView(networkFactsCard());          // device, vouches, established, and your wallets
            tabContent.addView(myDataLinks());
        }
        TextView foot = SocialUi.text(this, "Trust, country and state are always public. Amounts and group names are never shown. "
                + "Age and city are shown only if the member turns them on.", 11.5f, false, UiColors.textTertiary());
        foot.setPadding(SocialUi.dp(this, 4), SocialUi.dp(this, 10), SocialUi.dp(this, 4), 0);
        tabContent.addView(foot);
    }

    // ── Trust ────────────────────────────────────────────────────────────

    private View trustCard() {
        LinearLayout c = SocialUi.card(this);
        c.addView(SocialUi.text(this, "Trust score", 16, true, UiColors.textPrimary()));
        if (!card.scored()) {
            c.addView(SocialUi.text(this, "Tier: " + tierName(), 15, true, ProfileTrust.tierColor(tierName())));
            c.addView(SocialUi.text(this, "The network hasn't published a score for this member yet.", 13, false, UiColors.textSecondary()));
            return c;
        }
        String tier = tierName();
        int color = ProfileTrust.tierColor(tier);
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.BOTTOM);
        top.setPadding(0, SocialUi.dp(this, 6), 0, 0);
        top.addView(SocialUi.text(this, card.trustPercent + "%", 34, true, color));
        TextView t = SocialUi.text(this, "  " + tier, 16, true, UiColors.textPrimary());
        t.setPadding(0, 0, 0, SocialUi.dp(this, 5));
        top.addView(t);
        c.addView(top);
        c.addView(bar(card.trustPercent, color));
        String[][] parts = {{"Personhood", "trustPersonhoodPct"}, {"Reliability", "trustReliabilityPct"}, {"Conduct", "trustConductPct"},
                {"Tenure", "trustTenurePct"}, {"Engagement", "trustEngagementPct"}};
        if (card.trustParts.get("trustPersonhoodPct") instanceof Number) {
            final LinearLayout breakdown = new LinearLayout(this);
            breakdown.setOrientation(LinearLayout.VERTICAL);
            breakdown.setVisibility(opened.contains("breakdown") ? View.VISIBLE : View.GONE);
            final TextView toggle = SocialUi.text(this, opened.contains("breakdown") ? "Hide breakdown \u25B4" : "Show breakdown \u25BE", 14, true, UiColors.accent());
            toggle.setPadding(0, SocialUi.dp(this, 14), 0, SocialUi.dp(this, 2));
            toggle.setOnClickListener(v -> {
                boolean open = breakdown.getVisibility() != View.VISIBLE;
                breakdown.setVisibility(open ? View.VISIBLE : View.GONE);
                toggle.setText(open ? "Hide breakdown \u25B4" : "Show breakdown \u25BE");
                if (open) opened.add("breakdown"); else opened.remove("breakdown");
            });
            c.addView(toggle);
            c.addView(breakdown);
            for (String[] part : parts) {
                int v = card.trustParts.get(part[1]) instanceof Number ? ((Number) card.trustParts.get(part[1])).intValue() : 0;
                LinearLayout line = new LinearLayout(this);
                line.setOrientation(LinearLayout.VERTICAL);
                line.setPadding(0, SocialUi.dp(this, 12), 0, 0);
                LinearLayout head = new LinearLayout(this);
                head.addView(SocialUi.text(this, part[0], 13, false, UiColors.textSecondary()), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                head.addView(SocialUi.text(this, v + "%", 13, true, UiColors.textPrimary()));
                line.addView(head);
                line.addView(bar(v, UiColors.accent()));
                breakdown.addView(line);
            }
        }
        TextView note = SocialUi.text(this, "Worked out by the network. Nobody can hide or change it.", 12, false, UiColors.textTertiary());
        note.setPadding(0, SocialUi.dp(this, 12), 0, 0);
        c.addView(note);
        if (isMe()) {
            TextView more = SocialUi.text(this, "How to raise it  ›", 14, true, UiColors.accent());
            more.setPadding(0, SocialUi.dp(this, 12), 0, 0);
            more.setOnClickListener(v -> startActivity(new Intent(this, com.harvana.app.TrustScoreActivity.class)));
            c.addView(more);
        }
        return c;
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

    /** Shortcuts to the rest of my own data: what the phone counted, my devices, and what the network holds. */
    private View myDataLinks() {
        LinearLayout c = SocialUi.card(this);
        c.addView(SocialUi.text(this, "More of your data", 16, true, UiColors.textPrimary()));
        c.addView(linkRow("My activity", "Time in the app, screens, streaks and your habit badges",
            v -> startActivity(new Intent(this, ActivityStatsActivity.class))));
        c.addView(linkRow("Devices", "Phones signed in to your account",
            v -> startActivity(new Intent(this, DevicesActivity.class))));
        c.addView(linkRow("Trust score in full", "Every part of your score and how to raise it",
            v -> startActivity(new Intent(this, com.harvana.app.TrustScoreActivity.class))));
        TextView keep = SocialUi.text(this, "Anything you delete leaves every screen at once. A private copy is kept for one year in case of a dispute, then erased.",
            11.5f, false, UiColors.textTertiary());
        keep.setPadding(0, SocialUi.dp(this, 12), 0, 0);
        c.addView(keep);
        return c;
    }

    private View linkRow(String title, String sub, View.OnClickListener go) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, SocialUi.dp(this, 12), 0, SocialUi.dp(this, 2));
        LinearLayout words = new LinearLayout(this);
        words.setOrientation(LinearLayout.VERTICAL);
        words.addView(SocialUi.text(this, title, 14, true, UiColors.textPrimary()));
        words.addView(SocialUi.text(this, sub, 12, false, UiColors.textSecondary()));
        row.addView(words, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(SocialUi.text(this, "\u203A", 20, true, UiColors.accent()));
        row.setClickable(true);
        row.setOnClickListener(go);
        return row;
    }

    /** Milestones reached, in one short row. On my own profile, "See all" opens the whole shelf with what is still to earn. */
    private View milestonesCard() {
        final LinearLayout holder = new LinearLayout(this);
        holder.setOrientation(LinearLayout.VERTICAL);
        holder.addView(com.harvana.app.utils.Shimmer.cards(this, 1));
        final boolean mine = isMe();
        String wallet = mine ? com.harvana.app.wallet.WalletManager.getActiveWalletAddress(this) : card.wallet;
        com.harvana.app.milestone.MilestoneLoader.load(this, wallet, uid, mine, items -> {
            if (isFinishing()) return;
            holder.removeAllViews();
            holder.addView(com.harvana.app.milestone.MilestoneShelf.strip(this, items, mine));
        });
        return holder;
    }

    private View badgesCard() {
        LinearLayout c = SocialUi.card(this);
        c.addView(SocialUi.text(this, "Badges", 16, true, UiColors.textPrimary()));
        final LinearLayout chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.VERTICAL);
        chips.setPadding(0, SocialUi.dp(this, 10), 0, 0);
        c.addView(chips);
        if (card.wallet.isEmpty()) { BadgeChips.fill(chips, new java.util.ArrayList<>(), false); return c; }
        NodeBadges.load(card.wallet, list -> BadgeChips.fill(chips, list, false));
        return c;
    }

    // ── Activity ─────────────────────────────────────────────────────────

    private View activityCard() {
        LinearLayout c = SocialUi.card(this);
        c.addView(SocialUi.text(this, "Activity", 16, true, UiColors.textPrimary()));
        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setPadding(SocialUi.dp(this, 2), SocialUi.dp(this, 10), SocialUi.dp(this, 2), SocialUi.dp(this, 2));
        LinearLayout r1 = new LinearLayout(this);
        r1.setBaselineAligned(false);
        r1.addView(countTile("Groups", card.groups));
        r1.addView(countTile("Cycles completed", card.cycles));
        LinearLayout r2 = new LinearLayout(this);
        r2.setBaselineAligned(false);
        r2.addView(countTile("Loans", card.loans));
        r2.addView(countTile("Pending loans", card.pendingLoans));
        grid.addView(r1);
        grid.addView(r2);
        c.addView(grid);
        TextView note = SocialUi.text(this, "Counts only. Amounts and group names are never shown.", 12, false, UiColors.textTertiary());
        note.setPadding(0, SocialUi.dp(this, 8), 0, 0);
        c.addView(note);
        return c;
    }

    /** One count in the Activity grid: compact (about 60 dp), number on top in the accent colour, label under it, with space between tiles. */
    private View countTile(String label, int value) {
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setPadding(SocialUi.dp(this, 14), SocialUi.dp(this, 8), SocialUi.dp(this, 14), SocialUi.dp(this, 8));
        t.setMinimumHeight(SocialUi.dp(this, 60));
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(SocialUi.dp(this, 14));
        g.setColor((UiColors.accent() & 0x00FFFFFF) | 0x12000000);
        g.setStroke(SocialUi.dp(this, 1), (UiColors.accent() & 0x00FFFFFF) | 0x26000000);
        t.setBackground(g);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        int m = SocialUi.dp(this, 6);
        lp.setMargins(m, m, m, m);                                  // space between the tiles and from the card edge
        t.setLayoutParams(lp);
        t.addView(SocialUi.text(this, value < 0 ? "\u2014" : String.valueOf(value), 20, true, UiColors.accent()));
        TextView l = SocialUi.text(this, label, 12, false, UiColors.textSecondary());
        l.setPadding(0, SocialUi.dp(this, 1), 0, 0);
        t.addView(l);
        return t;
    }

    // ── Groups in common ─────────────────────────────────────────────────

    /** One shared group, laid out like a row of the Groups list: round image, name, type, members, cycle progress and trust. */
    private View sharedGroupRow(final PublicProfileStore.SharedGroup g) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, SocialUi.dp(this, 14), 0, SocialUi.dp(this, 4));
        row.setClickable(true);
        row.setOnClickListener(v -> startActivity(new Intent(this, GroupDetailsActivity.class).putExtra("groupId", g.id)));

        // Circular image: the group's photo when it has one, otherwise the HARV coin on the accent colour, as in the Groups list.
        int size = SocialUi.dp(this, 48);
        ImageView face = new ImageView(this);
        face.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        GradientDrawable disc = new GradientDrawable();
        disc.setShape(GradientDrawable.OVAL);
        disc.setColor(UiColors.accent());
        face.setBackground(disc);
        face.setClipToOutline(true);
        com.harvana.app.utils.GroupRing.apply(face, g.type, 1f, 0x00000000);      // the ring colour says what kind of group it is
        if (g.photo.startsWith("http")) {
            face.setScaleType(ImageView.ScaleType.CENTER_CROP);
            try { com.bumptech.glide.Glide.with(this).load(g.photo).circleCrop().into(face); } catch (Exception ignored) { face.setImageResource(R.drawable.ic_coin_harv); }
        } else {
            face.setImageResource(R.drawable.ic_coin_harv);
            face.setPadding(SocialUi.dp(this, 10), SocialUi.dp(this, 10), SocialUi.dp(this, 10), SocialUi.dp(this, 10));
        }
        row.addView(face, new LinearLayout.LayoutParams(size, size));

        LinearLayout words = new LinearLayout(this);
        words.setOrientation(LinearLayout.VERTICAL);
        words.setPadding(SocialUi.dp(this, 12), 0, 0, 0);
        words.addView(SocialUi.text(this, g.name, 16, true, UiColors.textPrimary()));

        StringBuilder kind = new StringBuilder();
        if (!g.state.isEmpty()) kind.append(g.state).append("  \u00b7  ");
        kind.append(g.type.isEmpty() ? "Group" : g.type);
        if (!g.frequency.isEmpty()) kind.append("  \u00b7  ").append(g.frequency);
        TextView kindLine = SocialUi.text(this, kind.toString(), 12.5f, false, UiColors.textSecondary());
        kindLine.setPadding(0, SocialUi.dp(this, 2), 0, 0);
        words.addView(kindLine);

        StringBuilder facts = new StringBuilder();
        facts.append(g.members).append(g.members == 1 ? " member" : " members");
        if (g.amount > 0) facts.append("  \u00b7  ").append(com.harvana.app.wallet.BalanceFormatter.smart(g.amount)).append("/slot");
        TextView factLine = SocialUi.text(this, facts.toString(), 12.5f, false, UiColors.textSecondary());
        factLine.setPadding(0, SocialUi.dp(this, 2), 0, 0);
        words.addView(factLine);

        if (g.slots > 0) {
            long round = Math.max(1, g.round);
            int pct = (int) Math.min(100, (round * 100.0) / g.slots);
            LinearLayout cycle = new LinearLayout(this);
            cycle.setGravity(Gravity.CENTER_VERTICAL);
            cycle.setPadding(0, SocialUi.dp(this, 8), 0, 0);
            cycle.addView(SocialUi.text(this, "Cycle " + round + " of " + g.slots, 11.5f, false, UiColors.textTertiary()), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            cycle.addView(SocialUi.text(this, pct + "%", 11.5f, true, UiColors.textSecondary()));
            words.addView(cycle);
            words.addView(bar(pct, UiColors.accent()));
        }

        final TextView trust = SocialUi.text(this, "Trust …", 12, true, UiColors.textTertiary());
        trust.setPadding(0, SocialUi.dp(this, 6), 0, 0);
        words.addView(trust);
        if (g.nodeKey.isEmpty()) {
            trust.setText("Not on the network");
        } else {
            com.harvana.app.trust.NetworkTrust.group(g.nodeKey, score -> runOnUiThread(() -> {
                if (!score.published || score.summary.isEmpty()) { trust.setText("Not on the network"); return; }
                trust.setText(score.summary);
                if (score.percent >= 0) trust.setTextColor(ProfileTrust.tierColor(score.tier));
            }));
        }
        row.addView(words, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(SocialUi.text(this, "Open  \u203A", 13, true, UiColors.accent()));
        return row;
    }

    private java.util.List<PublicProfileStore.SharedGroup> adminGroupsCache;

    private void fillAdminGroups(LinearLayout list, java.util.List<PublicProfileStore.SharedGroup> groups) {
        list.removeAllViews();
        if (groups.isEmpty()) {
            TextView none = SocialUi.text(this, "You are not the admin of any group yet. Create one from the Groups tab.", 13, false, UiColors.textSecondary());
            none.setPadding(0, SocialUi.dp(this, 10), 0, 0);
            list.addView(none);
            return;
        }
        for (PublicProfileStore.SharedGroup g : groups) list.addView(sharedGroupRow(g));
    }

    private View sharedCard() {
        boolean mine = isMe();
        LinearLayout c = SocialUi.card(this);
        c.addView(SocialUi.text(this, mine ? "Your groups" : "Groups you share", 16, true, UiColors.textPrimary()));
        if (mine) {
            TextView note = SocialUi.text(this, "Groups you are the admin of. Others only see how many groups you are in; group names show only for groups you both belong to.", 13, false, UiColors.textSecondary());
            note.setPadding(0, SocialUi.dp(this, 6), 0, SocialUi.dp(this, 2));
            c.addView(note);
            final LinearLayout list = new LinearLayout(this);
            list.setOrientation(LinearLayout.VERTICAL);
            c.addView(list);
            if (adminGroupsCache != null) fillAdminGroups(list, adminGroupsCache);
            else {
                list.addView(com.harvana.app.utils.Shimmer.lines(this, 3));
                PublicProfileStore.adminGroups(card.uid, (groups, err) -> runOnUiThread(() -> {
                    if (isFinishing()) return;
                    if (groups == null) { list.removeAllViews(); list.addView(SocialUi.text(this, "Couldn't load your groups: " + err, 13, false, UiColors.textSecondary())); return; }
                    adminGroupsCache = groups;
                    fillAdminGroups(list, groups);
                }));
            }
            return c;
        }
        if (card.sharedGroups.isEmpty()) {
            TextView none = SocialUi.text(this, "You and " + (card.name.isEmpty() ? "this member" : card.name) + " have no groups in common"
                    + (card.groups > 0 ? ". They are in " + card.groups + (card.groups == 1 ? " group." : " groups.") : "."), 13, false, UiColors.textSecondary());
            none.setPadding(0, SocialUi.dp(this, 6), 0, 0);
            c.addView(none);
            return c;
        }
        for (PublicProfileStore.SharedGroup g : card.sharedGroups) c.addView(sharedGroupRow(g));
        return c;
    }
}
