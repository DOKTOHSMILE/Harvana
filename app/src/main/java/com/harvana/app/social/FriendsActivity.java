package com.harvana.app.social;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.harvana.app.utils.AppBar;
import com.harvana.app.utils.UiColors;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Friends: a photo, name, @username and trust score for each person; follow / unfollow; pinch a friend (a nudge that
 * shows up as a notification on their phone); and your referrals. Your username is your referral code: friends enter it
 * when they sign up, and you confirm them here. People are found by exact username. There is no member directory.
 */
public class FriendsActivity extends AppCompatActivity {
    private LinearLayout searchResult, listBox;
    private EditText query;
    private MaterialButton tabFollowing, tabFollowers, tabBack, tabReferrals;
    private LinearLayout referralBox;
    private TextView codeLine, codeStats;
    private String myUsername = "";
    private List<ReferralStore.Ask> asks = new ArrayList<>();
    private List<Person> namedMe = new ArrayList<>();
    private List<Person> following = new ArrayList<>(), followers = new ArrayList<>();
    private boolean followingLoaded, followersLoaded;
    private boolean showingFollowers;
    /** 0 friends (mutual), 1 following, 2 followers, 3 referrals. */
    private int tab;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(UiColors.background());
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int p = SocialUi.dp(this, 16);
        root.setPadding(p, p, p, p);
        sv.addView(root);
        AppBar.setContent(this, sv, "Friends", "People you and they both follow");

        LinearLayout header = SocialUi.card(this);
        header.addView(SocialUi.text(this, "Friends", 28, true, UiColors.textPrimary()));
        header.addView(SocialUi.text(this, "Follow people, pinch a friend, and bring in people you know.", 13, false, UiColors.accent()));
        root.addView(header, 0);

        LinearLayout code = SocialUi.card(this);
        LinearLayout codeTop = new LinearLayout(this);
        codeTop.setOrientation(LinearLayout.HORIZONTAL);
        codeTop.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout codeText = new LinearLayout(this);
        codeText.setOrientation(LinearLayout.VERTICAL);
        codeText.addView(SocialUi.text(this, "Your referral code", 12, true, UiColors.textSecondary()));
        codeLine = SocialUi.text(this, "Loading...", 22, true, UiColors.textPrimary());
        com.harvana.app.utils.Shimmer.watch(codeLine);
        codeText.addView(codeLine);
        codeTop.addView(codeText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        MaterialButton share = SocialUi.compactButton(this, "Share", false);
        share.setOnClickListener(v -> shareCode());
        LinearLayout.LayoutParams shareLp = (LinearLayout.LayoutParams) share.getLayoutParams();
        shareLp.setMarginStart(SocialUi.dp(this, 12));
        codeTop.addView(share, shareLp);
        code.addView(codeTop);
        codeStats = SocialUi.text(this, "Friends enter your username when they create their account. You confirm them under Referrals.", 12, false, UiColors.textSecondary());
        codeStats.setPadding(0, SocialUi.dp(this, 10), 0, 0);
        code.addView(codeStats);
        root.addView(code);

        LinearLayout find = SocialUi.card(this);
        find.addView(SocialUi.text(this, "Add a friend", 14, true, UiColors.textPrimary()));
        find.addView(SocialUi.text(this, "Type their exact username. There is no public member list.", 12, false, UiColors.textSecondary()));
        LinearLayout line = new LinearLayout(this);
        line.setOrientation(LinearLayout.HORIZONTAL);
        query = SocialUi.edit(this, "@username");
        line.addView(query, new LinearLayout.LayoutParams(0, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        MaterialButton go = SocialUi.button(this, "Find", false);
        go.setOnClickListener(v -> search(go));
        line.addView(go);
        find.addView(line);
        searchResult = new LinearLayout(this);
        searchResult.setOrientation(LinearLayout.VERTICAL);
        find.addView(searchResult);
        root.addView(find);

        LinearLayout fromContacts = SocialUi.card(this);
        fromContacts.addView(SocialUi.text(this, "From your contacts", 14, true, UiColors.textPrimary()));
        fromContacts.addView(SocialUi.text(this, "See which of your contacts are already on Harvana, and invite the rest.", 12, false, UiColors.textSecondary()));
        MaterialButton scan = SocialUi.button(this, "Find friends in my contacts", true);
        scan.setOnClickListener(v -> startActivity(new Intent(this, FindContactsActivity.class)));
        LinearLayout.LayoutParams scanLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        scanLp.topMargin = SocialUi.dp(this, 10);
        fromContacts.addView(scan, scanLp);
        root.addView(fromContacts);

        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabFollowing = SocialUi.button(this, "Friends", false);
        tabFollowers = SocialUi.button(this, "Following", true);
        tabBack = SocialUi.button(this, "Followers", true);
        tabReferrals = SocialUi.button(this, "Referrals", true);
        tabFollowing.setOnClickListener(v -> { tab = 0; render(); });
        tabFollowers.setOnClickListener(v -> { tab = 1; render(); });
        tabBack.setOnClickListener(v -> { tab = 2; render(); });
        tabReferrals.setOnClickListener(v -> { tab = 3; render(); });
        for (MaterialButton t : new MaterialButton[] {tabFollowing, tabFollowers, tabBack, tabReferrals}) {
            t.setTextSize(12);
            t.setLetterSpacing(0f);
            t.setMaxLines(1);
            t.setInsetTop(0); t.setInsetBottom(0);
            t.setPadding(SocialUi.dp(this, 4), t.getPaddingTop(), SocialUi.dp(this, 4), t.getPaddingBottom());
            LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(0, SocialUi.dp(this, 40), 1f);
            tl.setMarginStart(SocialUi.dp(this, 3)); tl.setMarginEnd(SocialUi.dp(this, 3));
            tabs.addView(t, tl);
        }
        LinearLayout.LayoutParams tabsLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tabsLp.topMargin = SocialUi.dp(this, 4);
        tabsLp.bottomMargin = SocialUi.dp(this, 12);
        root.addView(tabs, tabsLp);

        listBox = SocialUi.card(this);
        root.addView(listBox);

    }

    @Override protected void onResume() { super.onResume(); reload(); }

    private void reload() {
        SocialStore.following((list, err) -> { following = list; followingLoaded = true; runOnUiThread(this::render); });
        SocialStore.followers((list, err) -> { followers = list; followersLoaded = true; runOnUiThread(this::render); });
        SocialStore.myUsername((name, err) -> runOnUiThread(() -> { myUsername = name == null ? "" : name; showCode(); }));
        ReferralStore.sync(this, (line, err) -> { });
        ReferralStore.asksForMe((list, err) -> { asks = list; runOnUiThread(this::render); });
        ReferralStore.namedMe((list, err) -> { namedMe = list; runOnUiThread(this::render); });
    }

    private void showCode() {
        if (myUsername.isEmpty()) {
            codeLine.setText("No username yet");
            codeStats.setText("Choose a username on your contact card. It becomes your referral code.");
        } else {
            codeLine.setText("@" + myUsername);
        }
    }

    private void shareCode() {
        if (myUsername.isEmpty()) { startActivity(new Intent(this, ContactCardActivity.class)); return; }
        Intent send = new Intent(Intent.ACTION_SEND).setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, "Join me on Harvana. When you create your account, enter @" + myUsername + " as the friend who invited you.");
        startActivity(Intent.createChooser(send, "Share my referral code"));
    }

    private Set<String> followingIds() {
        Set<String> s = new HashSet<>();
        for (Person p : following) s.add(p.uid);
        return s;
    }

    private void search(MaterialButton go) {
        searchResult.removeAllViews();
        go.setEnabled(false);
        SocialStore.findByUsername(query.getText().toString(), (person, err) -> runOnUiThread(() -> {
            go.setEnabled(true);
            searchResult.removeAllViews();
            if (person == null) { searchResult.addView(SocialUi.text(this, err == null ? "Not found." : err, 13, false, UiColors.textSecondary())); return; }
            boolean already = followingIds().contains(person.uid);
            searchResult.addView(SocialUi.personRow(this, person, already ? "Following" : "Follow", already, v -> {
                if (already) return;
                SocialStore.follow(person, (ok, e) -> runOnUiThread(() -> {
                    if (ok) { Toast.makeText(this, "You now follow " + person.name, Toast.LENGTH_SHORT).show(); searchResult.removeAllViews(); reload(); }
                    else Toast.makeText(this, e, Toast.LENGTH_LONG).show();
                }));
            }, null));
        }));
    }

    /** The selected tab is filled; the others are outlined. Called on every render, so tapping a tab visibly moves the selection. */
    private void styleTabs() {
        MaterialButton[] all = {tabFollowing, tabFollowers, tabBack, tabReferrals};
        for (int i = 0; i < all.length; i++) {
            MaterialButton t = all[i];
            boolean on = i == tab;
            t.setBackgroundTintList(android.content.res.ColorStateList.valueOf(on ? UiColors.accent() : android.graphics.Color.TRANSPARENT));
            t.setTextColor(on ? UiColors.onAccent() : UiColors.textSecondary());
            t.setStrokeColor(android.content.res.ColorStateList.valueOf(on ? UiColors.accent() : ((UiColors.textTertiary() & 0x00FFFFFF) | 0x66000000)));
            t.setStrokeWidth(SocialUi.dp(this, 1));
            t.setTypeface(null, on ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        }
    }

    private void render() {
        styleTabs();
        List<Person> friends = mutual();
        tabFollowing.setText("Friends " + friends.size());
        tabFollowers.setText("Following " + following.size());
        tabBack.setText("Followers " + followers.size());
        int waiting = 0;
        for (ReferralStore.Ask a : asks) if (!a.consented()) waiting++;
        tabReferrals.setText(waiting > 0 ? "Referrals (" + waiting + ")" : "Referrals");
        if (!myUsername.isEmpty()) codeStats.setText(namedMe.size() + (namedMe.size() == 1 ? " person has" : " people have") + " named you as their referrer."
                + " If someone you referred defaults in a group, your trust score drops too.");
        listBox.removeAllViews();
        if (tab != 3 && !(followingLoaded && followersLoaded)) { listBox.addView(com.harvana.app.utils.Shimmer.rows(this, 5)); return; }
        if (tab == 3) { renderReferrals(); return; }
        List<Person> list = tab == 1 ? following : tab == 2 ? followers : friends;
        if (list.isEmpty()) {
            String empty = tab == 1 ? "You don't follow anyone yet. Add a friend by username below."
                    : tab == 2 ? "Nobody follows you yet."
                    : "No friends yet. A friend is someone you follow who follows you back.";
            listBox.addView(SocialUi.text(this, empty, 13, false, UiColors.textSecondary()));
            return;
        }
        Set<String> mine = followingIds();
        for (Person p : list) {
            boolean iFollow = mine.contains(p.uid);
            boolean friend = tab == 0;
            String action = friend || (tab == 1) ? "Unfollow" : (iFollow ? "Friends" : "Follow back");
            boolean outlined = friend || tab == 1 || iFollow;
            listBox.addView(friendRow(p, action, outlined, iFollow, v -> {
                if (tab == 2 && !iFollow) {
                    SocialStore.follow(p, (ok, e) -> runOnUiThread(() -> { if (!ok) Toast.makeText(this, e, Toast.LENGTH_LONG).show(); reload(); }));
                    return;
                }
                if (tab == 2) return;
                new AlertDialog.Builder(this).setTitle("Unfollow " + p.name + "?")
                    .setMessage("They leave your friends list, and they will no longer see your contact links.")
                    .setPositiveButton("Unfollow", (d, w) -> SocialStore.unfollow(p.uid, (ok, e) -> runOnUiThread(this::reload)))
                    .setNegativeButton("Cancel", null).show();
            }, v -> openProfile(p)));
        }
    }

    /** One person: photo, name, @username, trust score, and the buttons. The photo and score arrive after the row is drawn. */
    private View friendRow(Person p, String action, boolean outlined, boolean canPinch, View.OnClickListener onAction, View.OnClickListener onRow) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, SocialUi.dp(this, 8), 0, SocialUi.dp(this, 8));

        int size = SocialUi.dp(this, 40);
        FrameLayout face = new FrameLayout(this);
        TextView initials = SocialUi.text(this, p.initials(), 16, true, 0xFF1A1208);
        initials.setGravity(Gravity.CENTER);
        android.graphics.drawable.GradientDrawable disc = new android.graphics.drawable.GradientDrawable();
        disc.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        disc.setColor(0xFFF0B45A);
        initials.setBackground(disc);
        face.addView(initials, new FrameLayout.LayoutParams(size, size));
        ImageView photo = new ImageView(this);
        photo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        face.addView(photo, new FrameLayout.LayoutParams(size, size));
        LinearLayout.LayoutParams faceLp = new LinearLayout.LayoutParams(size, size);
        faceLp.rightMargin = SocialUi.dp(this, 10);
        row.addView(face, faceLp);

        LinearLayout names = new LinearLayout(this);
        names.setOrientation(LinearLayout.VERTICAL);
        TextView name = SocialUi.text(this, p.name, 15, true, UiColors.textPrimary());
        names.addView(name);
        TextView handle = SocialUi.text(this, p.handle(), 12, false, UiColors.textSecondary());
        if (!p.handle().isEmpty()) names.addView(handle);
        TextView trust = SocialUi.text(this, "", 11, true, UiColors.textSecondary());
        trust.setPadding(SocialUi.dp(this, 8), SocialUi.dp(this, 2), SocialUi.dp(this, 8), SocialUi.dp(this, 2));
        LinearLayout.LayoutParams trustLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        trustLp.topMargin = SocialUi.dp(this, 4);
        names.addView(trust, trustLp);
        com.harvana.app.trust.ProfileTrust.resolve(p.uid, card -> {
            com.harvana.app.trust.ProfileTrust.bind(trust, card);
            if (!card.wallet.isEmpty()) com.harvana.app.trust.NodeBadges.load(card.wallet, list -> {
                if (list.isEmpty()) return;
                LinearLayout chips = new LinearLayout(this);
                chips.setOrientation(LinearLayout.VERTICAL);
                chips.setPadding(0, SocialUi.dp(this, 4), 0, 0);
                com.harvana.app.trust.BadgeChips.fill(chips, list, true);
                names.addView(chips);
            });
            if (!card.icon.isEmpty() && card.icon.startsWith("http")) {
                try { com.bumptech.glide.Glide.with(this).load(card.icon).circleCrop().into(photo); initials.setVisibility(View.INVISIBLE); }
                catch (Exception ignored) { }
            }
            if (p.handle().isEmpty() && !card.username.isEmpty()) { handle.setText("@" + card.username); if (handle.getParent() == null) names.addView(handle, 1); }
        });
        row.addView(names, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        MaterialButton main = SocialUi.compactButton(this, action, outlined);
        main.setOnClickListener(onAction);
        buttons.addView(main);
        if (canPinch) {
            MaterialButton pinch = SocialUi.compactButton(this, "Pinch", true);
            ((LinearLayout.LayoutParams) pinch.getLayoutParams()).setMarginStart(SocialUi.dp(this, 8));
            long wait = PinchStore.waitMs(this, p.uid);
            if (wait > 0) { pinch.setText("Pinched"); pinch.setEnabled(false); }
            pinch.setOnClickListener(v -> {
                pinch.setEnabled(false);
                PinchStore.send(this, p, (ok, err) -> runOnUiThread(() -> {
                    if (ok) { pinch.setText("Pinched"); Toast.makeText(this, p.name + " will get a notification that you pinched them.", Toast.LENGTH_SHORT).show(); }
                    else { pinch.setEnabled(true); Toast.makeText(this, err, Toast.LENGTH_LONG).show(); }
                }));
            });
            buttons.addView(pinch);
        }
        row.addView(buttons);
        if (onRow != null) { row.setClickable(true); row.setOnClickListener(onRow); }
        return row;
    }

    /** People who named me, what each is waiting on, and the ones I have to confirm. */
    private void renderReferrals() {
        listBox.addView(SocialUi.text(this, "Only confirm people you know and trust. If someone you referred defaults in a group, your trust score drops too, "
                + "10 points for each, up to 30.", 12, false, UiColors.textSecondary()));
        java.util.Map<String, ReferralStore.Ask> byUid = new java.util.HashMap<>();
        for (ReferralStore.Ask a : asks) byUid.put(a.fromUid, a);
        if (namedMe.isEmpty() && asks.isEmpty()) {
            TextView none = SocialUi.text(this, "Nobody has named you as their referrer yet. Share your code from the card above.", 13, false, UiColors.textSecondary());
            none.setPadding(0, SocialUi.dp(this, 10), 0, 0);
            listBox.addView(none);
            return;
        }
        for (Person p : namedMe) {
            ReferralStore.Ask a = byUid.get(p.uid);
            String state = a == null ? "Has not set up a wallet yet" : a.consented() ? "Confirmed" : a.referredAddress.isEmpty() ? "Waiting for their wallet" : "Waiting for you";
            View row = friendRow(p, a != null && !a.consented() && !a.referredAddress.isEmpty() ? "Confirm" : state, a != null && a.consented() || a == null, false, v -> {
                if (a == null || a.consented() || a.referredAddress.isEmpty()) return;
                new AlertDialog.Builder(this).setTitle("Confirm " + p.name + "?")
                    .setMessage("You are saying you know " + p.name + " and are referring them. You will confirm with your PIN. "
                        + "If they default in a group, your trust score drops.")
                    .setPositiveButton("Confirm", (d, w) -> ReferralStore.approve(this, a, (ok, err) -> runOnUiThread(() -> {
                        Toast.makeText(this, ok ? "Confirmed. It is recorded when " + p.name + " opens the app." : err, Toast.LENGTH_LONG).show();
                        reload();
                    })))
                    .setNeutralButton("Decline", (d, w) -> ReferralStore.decline(a, (ok, err) -> runOnUiThread(this::reload)))
                    .setNegativeButton("Cancel", null).show();
            }, null);
            listBox.addView(row);
            TextView st = SocialUi.text(this, state, 11, false, UiColors.textSecondary());
            st.setPadding(SocialUi.dp(this, 60), 0, 0, SocialUi.dp(this, 6));
            listBox.addView(st);
        }
    }

    /** People both sides follow. */
    private List<Person> mutual() {
        Set<String> back = new HashSet<>();
        for (Person p : followers) back.add(p.uid);
        List<Person> out = new ArrayList<>();
        for (Person p : following) if (back.contains(p.uid)) out.add(p);
        return out;
    }

    /** Contact links of a person, only when we follow each other. */
    /** Tapping a person opens their public profile. Their contact links stay behind "Contact" on that screen's menu path (mutual friends only). */
    private void openProfile(Person p) {
        startActivity(new Intent(this, PublicProfileActivity.class).putExtra(PublicProfileActivity.EXTRA_UID, p.uid));
    }

    /** Contact links of a mutual friend (also used from the public profile). */
    static void openCard(androidx.appcompat.app.AppCompatActivity a, Person p) {
        SocialStore.contactsOf(p.uid, (card, err) -> a.runOnUiThread(() -> {
            AlertDialog.Builder d = new AlertDialog.Builder(a).setTitle(p.name + (p.handle().isEmpty() ? "" : "  " + p.handle()));
            if (card == null) {
                d.setMessage("NOT_MUTUAL".equals(err)
                    ? "You can see how to reach " + p.name + " once you follow each other."
                    : "Couldn't load: " + err);
                d.setPositiveButton("OK", null).show();
                return;
            }
            List<String> labels = new ArrayList<>();
            final List<String> uris = new ArrayList<>();
            Object ph = card.get("phone");
            if (ph != null && !String.valueOf(ph).trim().isEmpty()) {
                String u = ContactLinks.uri("Phone", String.valueOf(ph));
                if (u != null) { labels.add("Call  " + ph); uris.add(u); }
            }
            Object links = card.get("links");
            if (links instanceof List) for (Object o : (List<?>) links) {
                if (!(o instanceof Map)) continue;
                String type = String.valueOf(((Map<?, ?>) o).get("type")), val = String.valueOf(((Map<?, ?>) o).get("value"));
                String u = ContactLinks.uri(type, val);
                if (u != null) { labels.add(type + "  " + val); uris.add(u); }
            }
            if (labels.isEmpty()) { d.setMessage(p.name + " hasn't added contact links yet.").setPositiveButton("OK", null).show(); return; }
            d.setItems(labels.toArray(new String[0]), (dlg, which) -> {
                try { a.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(uris.get(which)))); }
                catch (Exception e) { Toast.makeText(a, "No app can open that.", Toast.LENGTH_SHORT).show(); }
            }).setNegativeButton("Close", null).show();
        }));
    }
}
