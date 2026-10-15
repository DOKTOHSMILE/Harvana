package com.harvana.app.social;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.harvana.app.R;
import com.google.android.material.button.MaterialButton;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.harvana.app.utils.AppBar;
import com.harvana.app.utils.UiColors;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Friends, laid out like the social apps people already know: pill tabs (Friends, Following, Followers, Referrals), a search box
 * that filters the list as you type (and can look a username up on Harvana), a count line, and plain rows with a photo, name,
 * @username, trust score and one pill button (Following, or Follow back) plus a menu for the rest (profile, pinch, contact links,
 * unfollow). Referrals has your code and the people waiting for your Confirm. People are found by exact username; there is no
 * member directory.
 */
public class FriendsActivity extends AppCompatActivity {
    private LinearLayout listBox, referralHead;
    private EditText searchBox;
    private TextView countLine;
    private final TextView[] chips = new TextView[5];
    private TextView codeLine, codeStats;
    private String myUsername = "";
    private String filter = "";
    private List<ReferralStore.Ask> asks = new ArrayList<>();
    private List<Person> namedMe = new ArrayList<>();
    private List<Person> following = new ArrayList<>(), followers = new ArrayList<>();
    private boolean followingLoaded, followersLoaded;
    /** 0 friends (mutual), 1 following, 2 followers, 3 referrals. */
    private int tab;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        AppBar.setContent(this, com.harvana.app.R.layout.activity_friends, "Friends", "People you and they both follow");
        int[] chipIds = {com.harvana.app.R.id.chip_friends_0, com.harvana.app.R.id.chip_friends_1, com.harvana.app.R.id.chip_friends_2, com.harvana.app.R.id.chip_friends_3, com.harvana.app.R.id.chip_friends_4};
        for (int i = 0; i < 5; i++) {
            final int which = i;
            chips[i] = findViewById(chipIds[i]);
            chips[i].setOnClickListener(v -> { tab = which; filter = ""; searchBox.setText(""); render(); });
        }
        sizePillIcons();
        findViewById(com.harvana.app.R.id.btn_friends_username).setOnClickListener(v -> askUsername());
        findViewById(com.harvana.app.R.id.btn_friends_contacts).setOnClickListener(v -> startActivity(new Intent(this, FindContactsActivity.class)));
        findViewById(com.harvana.app.R.id.btn_friends_invite).setOnClickListener(v -> shareCode());
        searchBox = findViewById(com.harvana.app.R.id.et_friends_search);
        searchBox.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int c, int d) { }
            @Override public void onTextChanged(CharSequence s, int a, int c, int d) { }
            @Override public void afterTextChanged(Editable e) { filter = e.toString().trim(); render(); }
        });
        countLine = findViewById(com.harvana.app.R.id.tv_friends_count);
        referralHead = findViewById(com.harvana.app.R.id.friends_referral_head);
        listBox = findViewById(com.harvana.app.R.id.friends_list);
        render();
    }

    @Override protected void onResume() { super.onResume(); reload(); }

    private void reload() {
        SocialStore.following((list, err) -> { following = list; followingLoaded = true; runOnUiThread(this::render); });
        SocialStore.followers((list, err) -> { followers = list; followersLoaded = true; runOnUiThread(this::render); });
        SocialStore.blocked((list, err) -> { blockedList = list; blockedLoaded = true; runOnUiThread(this::render); });
        SocialStore.myUsername((name, err) -> runOnUiThread(() -> { myUsername = name == null ? "" : name; render(); }));
        ReferralStore.sync(this, (line, err) -> { });
        ReferralStore.asksForMe((list, err) -> { asks = list; runOnUiThread(this::render); });
        ReferralStore.namedMe((list, err) -> { namedMe = list; runOnUiThread(this::render); });
    }

    /** Leading icons of the three way buttons, all the same 24dp so one does not look bigger than the others. */
    private void sizePillIcons() {
        int[] ids = {com.harvana.app.R.id.btn_friends_username, com.harvana.app.R.id.btn_friends_contacts, com.harvana.app.R.id.btn_friends_invite};
        int size = (int) (24 * getResources().getDisplayMetrics().density);
        for (int id : ids) {
            android.widget.TextView b = findViewById(id);
            if (b == null) continue;
            android.graphics.drawable.Drawable[] ds = b.getCompoundDrawables();
            android.graphics.drawable.Drawable d = ds.length > 0 ? ds[0] : null;
            if (d != null) { d.setBounds(0, 0, size, size); b.setCompoundDrawables(d, null, null, null); }
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

    private Set<String> followerIds() {
        Set<String> s = new HashSet<>();
        for (Person p : followers) s.add(p.uid);
        return s;
    }

    /** "@ Username": type an exact username, see who it is, and follow. */
    private void askUsername() {
        final EditText input = SocialUi.edit(this, "@username");
        input.setPadding(SocialUi.dp(this, 24), SocialUi.dp(this, 16), SocialUi.dp(this, 24), SocialUi.dp(this, 8));
        new com.harvana.app.ui.SheetDialog.Builder(this).setTitle("Find by username")
            .setMessage("Type their exact username. There is no public member list.")
            .setView(input)
            .setPositiveButton("Find", (d, w) -> lookup(input.getText().toString()))
            .setNegativeButton("Cancel", null).show();
    }

    private void lookup(String typed) {
        SocialStore.findByUsername(typed, (person, err) -> runOnUiThread(() -> {
            if (person == null) { Toast.makeText(this, err == null ? "Not found." : err, Toast.LENGTH_LONG).show(); return; }
            if (followingIds().contains(person.uid)) { openProfile(person); return; }
            for (Person b : blockedList) if (b.uid.equals(person.uid)) { Toast.makeText(this, "You blocked " + person.name + ". Unblock them from the Blocked tab first.", Toast.LENGTH_LONG).show(); return; }
            new com.harvana.app.ui.SheetDialog.Builder(this).setTitle(person.name + (person.handle().isEmpty() ? "" : "  " + person.handle()))
                .setPositiveButton("Follow", (d, w) -> follow(person))
                .setNeutralButton("View profile", (d, w) -> openProfile(person))
                .setNegativeButton("Cancel", null).show();
        }));
    }

    private void follow(Person person) {
        SocialStore.follow(person, (ok, e) -> runOnUiThread(() -> {
            if (ok) Toast.makeText(this, "You now follow " + person.name, Toast.LENGTH_SHORT).show();
            else Toast.makeText(this, e, Toast.LENGTH_LONG).show();
            reload();
        }));
    }

    private List<Person> blockedList = new ArrayList<>();
    private boolean blockedLoaded;

    private void blockAsk(Person p) {
        new com.harvana.app.ui.SheetDialog.Builder(this).setTitle("Block " + p.name + "?")
            .setMessage("They are removed from your followers and friends, you stop following them, and they cannot follow you or see your contact links. "
                + "Their Learn posts are hidden from you. They are not told. You can unblock them any time from the Blocked tab.")
            .setPositiveButton("Block", (d, w) -> SocialStore.block(p, (ok, e) -> runOnUiThread(() -> {
                Toast.makeText(this, ok ? p.name + " is blocked." : e, ok ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG).show();
                reload();
            })))
            .setNegativeButton("Cancel", null).show();
    }

    /** Everyone I have blocked, each with an Unblock button. */
    private void renderBlocked() {
        if (!blockedLoaded) {
            countLine.setText("Blocked");
            listBox.addView(com.harvana.app.utils.Shimmer.rows(this, 3));
            return;
        }
        List<Person> list = new ArrayList<>();
        for (Person p : blockedList) if (matches(p)) list.add(p);
        countLine.setText(filter.isEmpty() ? blockedList.size() + (blockedList.size() == 1 ? " blocked member" : " blocked members") : list.size() + " of " + blockedList.size());
        if (list.isEmpty()) {
            emptyLine(blockedList.isEmpty() ? "None" : "No match");
            return;
        }
        for (Person p : list) {
            View row = personRow(p, "");
            MaterialButton unblock = row.findViewById(R.id.btn_friend_following);
            unblock.setText("Unblock");
            unblock.setVisibility(View.VISIBLE);
            unblock.setOnClickListener(v -> SocialStore.unblock(p.uid, (ok, e) -> runOnUiThread(() -> {
                Toast.makeText(this, ok ? p.name + " is unblocked." : e, ok ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG).show();
                reload();
            })));
            row.findViewById(R.id.friend_line).setOnClickListener(null);
            listBox.addView(row);
        }
    }

    private void unfollowAsk(Person p) {
        new com.harvana.app.ui.SheetDialog.Builder(this).setTitle("Unfollow " + p.name + "?")
            .setMessage("They leave your friends list, and they will no longer see your contact links.")
            .setPositiveButton("Unfollow", (d, w) -> SocialStore.unfollow(p.uid, (ok, e) -> runOnUiThread(this::reload)))
            .setNegativeButton("Cancel", null).show();
    }

    private void styleChips() {
        for (int i = 0; i < chips.length; i++) {
            boolean on = i == tab;
            chips[i].setBackgroundResource(on ? R.drawable.bg_chip_on : R.drawable.bg_chip_off);
            chips[i].setTextColor(on ? UiColors.accent() : UiColors.textPrimary());
        }
    }

    /** A short line in the list (nothing yet, no match): res/layout/item_list_empty.xml. */
    private void emptyLine(String text) {
        TextView t = (TextView) getLayoutInflater().inflate(R.layout.item_list_empty, listBox, false);
        t.setText(text);
        t.setPadding(SocialUi.dp(this, 2), SocialUi.dp(this, 16), 0, SocialUi.dp(this, 8));
        listBox.addView(t);
    }

    private boolean matches(Person p) {
        if (filter.isEmpty()) return true;
        String q = filter.toLowerCase(Locale.US);
        if (q.startsWith("@")) q = q.substring(1);
        return p.name.toLowerCase(Locale.US).contains(q) || p.username.toLowerCase(Locale.US).contains(q);
    }

    private void render() {
        if (listBox == null) return;
        styleChips();
        List<Person> friends = mutual();
        chips[0].setText("Friends");
        chips[1].setText("Following");
        chips[2].setText("Followers");
        int waiting = 0;
        for (ReferralStore.Ask a : asks) if (!a.consented()) waiting++;
        chips[3].setText(waiting > 0 ? "Referrals (" + waiting + ")" : "Referrals");
        chips[4].setText("Blocked");
        searchBox.setHint(tab == 4 ? "Search blocked" : tab == 3 ? "Search referrals" : tab == 2 ? "Search followers" : tab == 1 ? "Search following" : "Search friends");

        listBox.removeAllViews();
        referralHead.removeAllViews();
        if (tab == 3) { countLine.setText("Referrals"); renderReferrals(); return; }
        if (tab == 4) { renderBlocked(); return; }
        if (!(followingLoaded && followersLoaded)) {
            countLine.setText(tab == 1 ? "Following" : tab == 2 ? "Followers" : "Friends");
            listBox.addView(com.harvana.app.utils.Shimmer.rows(this, 6));
            return;
        }
        List<Person> source = tab == 1 ? following : tab == 2 ? new ArrayList<>(followers) : friends;
        if (tab == 2) {                                           // people you have not followed back come first
            Set<String> mine = followingIds();
            List<Person> notYet = new ArrayList<>(), already = new ArrayList<>();
            for (Person p : source) (mine.contains(p.uid) ? already : notYet).add(p);
            source = new ArrayList<>(notYet);
            source.addAll(already);
        }
        List<Person> list = new ArrayList<>();
        for (Person p : source) if (matches(p)) list.add(p);
        int total = source.size();
        String noun = tab == 1 ? "Following" : tab == 2 ? (total == 1 ? "Follower" : "Followers") : (total == 1 ? "Friend" : "Friends");
        countLine.setText(filter.isEmpty() ? total + " " + noun : list.size() + " of " + total);

        if (list.isEmpty() && filter.isEmpty()) {
            emptyLine("None yet");
            return;
        }
        Set<String> mine = followingIds(), theirs = followerIds();
        for (Person p : list) listBox.addView(friendRow(p, mine.contains(p.uid), theirs.contains(p.uid)));
        if (filter.length() >= 2) {                               // the person may not be in your lists at all
            MaterialButton find = (MaterialButton) getLayoutInflater().inflate(R.layout.item_list_button, listBox, false);
            find.setText("Search Harvana for \u201C" + filter + "\u201D");
            find.setOnClickListener(v -> lookup(filter));
            listBox.addView(find);
        }
    }

    /**
     * One person, laid out like a social list: photo, then (a small "Follows you" if it applies) name with the trust score, @username
     * and badges, and on the right one pill button and a menu. The photo and score arrive after the row is drawn.
     */
    private View friendRow(Person p, boolean iFollow, boolean followsMe) {
        View row = personRow(p, "");
        if (followsMe && tab == 1) row.findViewById(R.id.tv_friend_follows_you).setVisibility(View.VISIBLE);

        // Following is a state (tap to unfollow); Follow back is the call to action.
        if (iFollow) {
            View following = row.findViewById(R.id.btn_friend_following);
            following.setVisibility(View.VISIBLE);
            following.setOnClickListener(v -> unfollowAsk(p));
        } else {
            MaterialButton follow = row.findViewById(R.id.btn_friend_follow);
            follow.setText(followsMe ? "Follow back" : "Follow");
            follow.setVisibility(View.VISIBLE);
            follow.setOnClickListener(v -> follow(p));
        }

        View more = row.findViewById(R.id.iv_friend_more);
        more.setVisibility(View.VISIBLE);
        final boolean friend = iFollow && followsMe;
        more.setOnClickListener(v -> {
            PopupMenu m = new PopupMenu(this, v);
            m.getMenu().add(0, 1, 0, "View profile");
            if (friend) { m.getMenu().add(0, 2, 1, "Pinch"); m.getMenu().add(0, 3, 2, "Contact links"); }
            if (iFollow) m.getMenu().add(0, 4, 3, "Unfollow");
            m.getMenu().add(0, 5, 4, "Block");
            m.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case 1: openProfile(p); return true;
                    case 2: pinch(p); return true;
                    case 3: openCard(this, p); return true;
                    case 4: unfollowAsk(p); return true;
                    case 5: blockAsk(p); return true;
                    default: return false;
                }
            });
            m.show();
        });
        return row;
    }

    /**
     * One person (res/layout/item_friend.xml): photo or initials, name, @username, an optional status line, trust and badges.
     * The photo, score and badges arrive after the row is drawn. Buttons are hidden; the caller shows the ones it needs.
     */
    private View personRow(Person p, String status) {
        View row = getLayoutInflater().inflate(R.layout.item_friend, listBox, false);
        TextView initials = row.findViewById(R.id.tv_friend_initials);
        initials.setText(p.initials());
        ImageView photo = row.findViewById(R.id.iv_friend_photo);
        if (p.photo.startsWith("http")) {
            try { com.bumptech.glide.Glide.with(this).load(p.photo).circleCrop().into(photo); initials.setVisibility(View.INVISIBLE); } catch (Exception ignored) { }
        }
        ((TextView) row.findViewById(R.id.tv_friend_name)).setText(p.name);
        TextView handle = row.findViewById(R.id.tv_friend_handle);
        if (!p.handle().isEmpty()) { handle.setText(p.handle()); handle.setVisibility(View.VISIBLE); }
        TextView st = row.findViewById(R.id.tv_friend_status);
        if (!status.isEmpty()) { st.setText(status); st.setVisibility(View.VISIBLE); }
        TextView trust = row.findViewById(R.id.tv_friend_trust);
        LinearLayout badges = row.findViewById(R.id.friend_badges);
        com.harvana.app.trust.ProfileTrust.resolve(p.uid, card -> {
            com.harvana.app.trust.ProfileTrust.bind(trust, card);
            if (!card.wallet.isEmpty()) com.harvana.app.trust.NodeBadges.load(card.wallet, list -> {
                if (list.isEmpty()) return;
                com.harvana.app.trust.BadgeChips.fill(badges, list, true);
                badges.setVisibility(View.VISIBLE);
            });
            if (!card.icon.isEmpty() && card.icon.startsWith("http")) {
                try { com.bumptech.glide.Glide.with(this).load(card.icon).circleCrop().into(photo); initials.setVisibility(View.INVISIBLE); }
                catch (Exception ignored) { }
            }
            if (p.handle().isEmpty() && !card.username.isEmpty()) { handle.setText("@" + card.username); handle.setVisibility(View.VISIBLE); }
        });
        row.findViewById(R.id.friend_line).setOnClickListener(v -> openProfile(p));
        return row;
    }

    private void pinch(Person p) {
        long wait = PinchStore.waitMs(this, p.uid);
        if (wait > 0) { Toast.makeText(this, "You pinched " + p.name + " a moment ago.", Toast.LENGTH_SHORT).show(); return; }
        PinchStore.send(this, p, (ok, err) -> runOnUiThread(() -> Toast.makeText(this,
            ok ? p.name + " will get a notification that you pinched them." : err, ok ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG).show()));
    }

    /** Your code, then the people who named you: Confirm / Decline when it is your turn, otherwise what they are waiting on. */
    private void renderReferrals() {
        View code = getLayoutInflater().inflate(R.layout.view_referral_code, referralHead, false);
        codeLine = code.findViewById(R.id.tv_referral_code);
        codeLine.setText(myUsername.isEmpty() ? "—" : "@" + myUsername);
        codeStats = code.findViewById(R.id.tv_referral_named);
        codeStats.setText(String.valueOf(namedMe.size()));
        code.findViewById(R.id.btn_referral_share).setOnClickListener(v -> shareCode());
        referralHead.addView(code);

        java.util.Map<String, ReferralStore.Ask> byUid = new java.util.HashMap<>();
        for (ReferralStore.Ask a : asks) byUid.put(a.fromUid, a);
        if (namedMe.isEmpty() && asks.isEmpty()) { emptyLine("None yet"); return; }
        for (Person p : namedMe) {
            if (!matches(p)) continue;
            final ReferralStore.Ask a = byUid.get(p.uid);
            boolean myTurn = a != null && !a.consented() && !a.referredAddress.isEmpty();
            String state = a == null ? "No wallet yet" : a.consented() ? "Confirmed" : a.referredAddress.isEmpty() ? "Waiting for their wallet" : "Waiting for you";
            View row = personRow(p, state);
            if (myTurn) {
                row.findViewById(R.id.friend_referral_buttons).setVisibility(View.VISIBLE);
                row.findViewById(R.id.btn_friend_confirm).setOnClickListener(v -> new com.harvana.app.ui.SheetDialog.Builder(this).setTitle("Confirm " + p.name + "?")
                    .setMessage("You are saying you know " + p.name + " and are referring them. You will confirm with your PIN. "
                        + "If they default in a group, your trust score drops.")
                    .setPositiveButton("Confirm", (d, w) -> ReferralStore.approve(this, a, (ok, err) -> runOnUiThread(() -> {
                        Toast.makeText(this, ok ? "Confirmed. It is recorded when " + p.name + " opens the app." : err, Toast.LENGTH_LONG).show();
                        reload();
                    })))
                    .setNegativeButton("Cancel", null).show());
                row.findViewById(R.id.btn_friend_decline).setOnClickListener(v -> ReferralStore.decline(a, (ok, err) -> runOnUiThread(this::reload)));
            }
            listBox.addView(row);
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
            com.harvana.app.ui.SheetDialog.Builder d = new com.harvana.app.ui.SheetDialog.Builder(a).setTitle(p.name + (p.handle().isEmpty() ? "" : "  " + p.handle()));
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
