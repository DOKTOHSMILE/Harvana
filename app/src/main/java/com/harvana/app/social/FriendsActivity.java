package com.harvana.app.social;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;
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
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(UiColors.background());
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int p = SocialUi.dp(this, 14);
        root.setPadding(p, SocialUi.dp(this, 10), p, p);
        sv.addView(root);
        AppBar.setContent(this, sv, "Friends", "People you and they both follow");

        // Pill tabs
        HorizontalScrollView chipScroll = new HorizontalScrollView(this);
        chipScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout chipRow = new LinearLayout(this);
        chipRow.setOrientation(LinearLayout.HORIZONTAL);
        String[] names = {"Friends", "Following", "Followers", "Referrals", "Blocked"};
        for (int i = 0; i < 5; i++) {
            final int which = i;
            TextView c = SocialUi.text(this, names[i], 14, true, UiColors.textPrimary());
            c.setGravity(Gravity.CENTER);
            c.setPadding(SocialUi.dp(this, 16), 0, SocialUi.dp(this, 16), 0);
            c.setOnClickListener(v -> { tab = which; filter = ""; searchBox.setText(""); render(); });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, SocialUi.dp(this, 38));
            lp.setMarginEnd(SocialUi.dp(this, 8));
            chipRow.addView(c, lp);
            chips[i] = c;
        }
        chipScroll.addView(chipRow);
        root.addView(chipScroll);

        // Ways to bring people in
        LinearLayout add = new LinearLayout(this);
        add.setOrientation(LinearLayout.HORIZONTAL);
        add.addView(quick("@  Username", v -> askUsername()));
        add.addView(quick("Contacts", v -> startActivity(new Intent(this, FindContactsActivity.class))));
        add.addView(quick("Invite", v -> shareCode()));
        LinearLayout.LayoutParams addLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        addLp.topMargin = SocialUi.dp(this, 12);
        root.addView(add, addLp);

        // Search
        searchBox = new EditText(this);
        searchBox.setSingleLine(true);
        searchBox.setTextSize(15);
        searchBox.setTextColor(UiColors.textPrimary());
        searchBox.setHintTextColor(UiColors.textTertiary());
        searchBox.setHint("Search friends");
        searchBox.setCompoundDrawablePadding(SocialUi.dp(this, 8));
        searchBox.setPadding(SocialUi.dp(this, 16), 0, SocialUi.dp(this, 16), 0);
        GradientDrawable box = new GradientDrawable();
        box.setCornerRadius(SocialUi.dp(this, 26));
        box.setColor(UiColors.surface());
        searchBox.setBackground(box);
        searchBox.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int c, int d) { }
            @Override public void onTextChanged(CharSequence s, int a, int c, int d) { }
            @Override public void afterTextChanged(Editable e) { filter = e.toString().trim(); render(); }
        });
        LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, SocialUi.dp(this, 46));
        sLp.topMargin = SocialUi.dp(this, 12);
        root.addView(searchBox, sLp);

        countLine = SocialUi.text(this, "", 20, true, UiColors.textPrimary());
        countLine.setPadding(SocialUi.dp(this, 2), SocialUi.dp(this, 18), 0, SocialUi.dp(this, 6));
        root.addView(countLine);

        referralHead = new LinearLayout(this);
        referralHead.setOrientation(LinearLayout.VERTICAL);
        root.addView(referralHead);

        listBox = new LinearLayout(this);
        listBox.setOrientation(LinearLayout.VERTICAL);
        root.addView(listBox);
        render();
    }

    private TextView quick(String label, View.OnClickListener click) {
        TextView t = SocialUi.text(this, label, 13.5f, true, UiColors.accent());
        t.setGravity(Gravity.CENTER);
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(SocialUi.dp(this, 22));
        g.setStroke(SocialUi.dp(this, 1), (UiColors.accent() & 0x00FFFFFF) | 0x88000000);
        t.setBackground(g);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, SocialUi.dp(this, 40), 1f);
        lp.setMarginEnd(SocialUi.dp(this, 8));
        t.setLayoutParams(lp);
        t.setOnClickListener(click);
        return t;
    }

    /** A pill button for a row. Filled for the main action, outlined for a state like "Following". */
    private TextView pill(String label, boolean filled, View.OnClickListener click) {
        TextView t = SocialUi.text(this, label, 13.5f, true, filled ? UiColors.onAccent() : UiColors.textPrimary());
        t.setGravity(Gravity.CENTER);
        t.setPadding(SocialUi.dp(this, 16), 0, SocialUi.dp(this, 16), 0);
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(SocialUi.dp(this, 20));
        if (filled) g.setColor(UiColors.accent());
        else g.setStroke(SocialUi.dp(this, 1), (UiColors.textTertiary() & 0x00FFFFFF) | 0xAA000000);
        t.setBackground(g);
        t.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, SocialUi.dp(this, 36)));
        t.setOnClickListener(click);
        return t;
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
        new AlertDialog.Builder(this).setTitle("Find by username")
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
            new AlertDialog.Builder(this).setTitle(person.name + (person.handle().isEmpty() ? "" : "  " + person.handle()))
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
        new AlertDialog.Builder(this).setTitle("Block " + p.name + "?")
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
            TextView t = SocialUi.text(this, blockedList.isEmpty()
                ? "You have not blocked anyone. Blocking removes someone from your followers, stops them following you and hides their Learn posts."
                : "No blocked member matches that.", 14, false, UiColors.textSecondary());
            t.setPadding(SocialUi.dp(this, 2), SocialUi.dp(this, 8), 0, 0);
            listBox.addView(t);
            return;
        }
        for (Person p : list) {
            LinearLayout line = new LinearLayout(this);
            line.setOrientation(LinearLayout.HORIZONTAL);
            line.setGravity(Gravity.CENTER_VERTICAL);
            line.setPadding(0, SocialUi.dp(this, 12), 0, SocialUi.dp(this, 12));
            LinearLayout names = new LinearLayout(this);
            names.setOrientation(LinearLayout.VERTICAL);
            names.addView(SocialUi.text(this, p.name, 16, true, UiColors.textPrimary()));
            if (!p.handle().isEmpty()) names.addView(SocialUi.text(this, p.handle(), 13, false, UiColors.textSecondary()));
            line.addView(names, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            line.addView(pill("Unblock", false, v -> SocialStore.unblock(p.uid, (ok, e) -> runOnUiThread(() -> {
                Toast.makeText(this, ok ? p.name + " is unblocked." : e, ok ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG).show();
                reload();
            }))));
            listBox.addView(line);
            View rule = new View(this);
            rule.setBackgroundColor((UiColors.textTertiary() & 0x00FFFFFF) | 0x22000000);
            listBox.addView(rule, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        }
    }

    private void unfollowAsk(Person p) {
        new AlertDialog.Builder(this).setTitle("Unfollow " + p.name + "?")
            .setMessage("They leave your friends list, and they will no longer see your contact links.")
            .setPositiveButton("Unfollow", (d, w) -> SocialStore.unfollow(p.uid, (ok, e) -> runOnUiThread(this::reload)))
            .setNegativeButton("Cancel", null).show();
    }

    private void styleChips() {
        for (int i = 0; i < chips.length; i++) {
            boolean on = i == tab;
            GradientDrawable g = new GradientDrawable();
            g.setCornerRadius(SocialUi.dp(this, 20));
            g.setColor(on ? ((UiColors.accent() & 0x00FFFFFF) | 0x33000000) : UiColors.surface());
            chips[i].setBackground(g);
            chips[i].setTextColor(on ? UiColors.accent() : UiColors.textPrimary());
        }
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
            String empty = tab == 1 ? "You don't follow anyone yet. Use Username or Contacts above to find people."
                    : tab == 2 ? "Nobody follows you yet. Share your code with Invite."
                    : "No friends yet. A friend is someone you follow who follows you back.";
            TextView t = SocialUi.text(this, empty, 14, false, UiColors.textSecondary());
            t.setPadding(SocialUi.dp(this, 2), SocialUi.dp(this, 8), 0, 0);
            listBox.addView(t);
            return;
        }
        Set<String> mine = followingIds(), theirs = followerIds();
        for (Person p : list) listBox.addView(friendRow(p, mine.contains(p.uid), theirs.contains(p.uid)));
        if (filter.length() >= 2) {                               // the person may not be in your lists at all
            TextView find = SocialUi.text(this, "Search Harvana for \u201C" + filter + "\u201D", 14.5f, true, UiColors.accent());
            find.setPadding(SocialUi.dp(this, 2), SocialUi.dp(this, 16), 0, SocialUi.dp(this, 16));
            find.setOnClickListener(v -> lookup(filter));
            listBox.addView(find);
        }
    }

    /**
     * One person, laid out like a social list: photo, then (a small "Follows you" if it applies) name with the trust score, @username
     * and badges, and on the right one pill button and a menu. The photo and score arrive after the row is drawn.
     */
    private View friendRow(Person p, boolean iFollow, boolean followsMe) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);

        LinearLayout line = new LinearLayout(this);
        line.setOrientation(LinearLayout.HORIZONTAL);
        line.setGravity(Gravity.CENTER_VERTICAL);
        line.setPadding(0, SocialUi.dp(this, 10), 0, SocialUi.dp(this, 10));

        int size = SocialUi.dp(this, 52);
        FrameLayout face = new FrameLayout(this);
        TextView initials = SocialUi.text(this, p.initials(), 18, true, 0xFF1A1208);
        initials.setGravity(Gravity.CENTER);
        GradientDrawable disc = new GradientDrawable();
        disc.setShape(GradientDrawable.OVAL);
        disc.setColor(0xFFF0B45A);
        initials.setBackground(disc);
        face.addView(initials, new FrameLayout.LayoutParams(size, size));
        ImageView photo = new ImageView(this);
        photo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        face.addView(photo, new FrameLayout.LayoutParams(size, size));
        LinearLayout.LayoutParams faceLp = new LinearLayout.LayoutParams(size, size);
        faceLp.setMarginEnd(SocialUi.dp(this, 12));
        line.addView(face, faceLp);
        if (p.photo.startsWith("http")) {
            try { com.bumptech.glide.Glide.with(this).load(p.photo).circleCrop().into(photo); initials.setVisibility(View.INVISIBLE); } catch (Exception ignored) { }
        }

        LinearLayout names = new LinearLayout(this);
        names.setOrientation(LinearLayout.VERTICAL);
        if (followsMe && tab == 1) names.addView(SocialUi.text(this, "Follows you", 12, true, UiColors.textTertiary()));
        TextView name = SocialUi.text(this, p.name, 16, true, UiColors.textPrimary());
        name.setMaxLines(1);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        names.addView(name);
        TextView handle = SocialUi.text(this, p.handle(), 13, false, UiColors.textSecondary());
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
                LinearLayout chipsBox = new LinearLayout(this);
                chipsBox.setOrientation(LinearLayout.VERTICAL);
                chipsBox.setPadding(0, SocialUi.dp(this, 4), 0, 0);
                com.harvana.app.trust.BadgeChips.fill(chipsBox, list, true);
                names.addView(chipsBox);
            });
            if (!card.icon.isEmpty() && card.icon.startsWith("http")) {
                try { com.bumptech.glide.Glide.with(this).load(card.icon).circleCrop().into(photo); initials.setVisibility(View.INVISIBLE); }
                catch (Exception ignored) { }
            }
            if (p.handle().isEmpty() && !card.username.isEmpty()) { handle.setText("@" + card.username); if (handle.getParent() == null) names.addView(handle, 1); }
        });
        line.addView(names, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        // Following is a state (tap to unfollow); Follow back is the call to action.
        TextView main = iFollow
            ? pill("Following", false, v -> unfollowAsk(p))
            : pill(followsMe ? "Follow back" : "Follow", true, v -> follow(p));
        ((LinearLayout.LayoutParams) main.getLayoutParams()).setMarginStart(SocialUi.dp(this, 8));
        line.addView(main);

        TextView more = SocialUi.text(this, "\u22EF", 22, true, UiColors.textSecondary());
        more.setGravity(Gravity.CENTER);
        line.addView(more, new LinearLayout.LayoutParams(SocialUi.dp(this, 36), SocialUi.dp(this, 36)));
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
        line.setClickable(true);
        line.setOnClickListener(v -> openProfile(p));
        row.addView(line);

        View rule = new View(this);
        rule.setBackgroundColor((UiColors.textTertiary() & 0x00FFFFFF) | 0x22000000);
        row.addView(rule, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
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
        LinearLayout code = SocialUi.card(this);
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        text.addView(SocialUi.text(this, "Your referral code", 12, true, UiColors.textSecondary()));
        codeLine = SocialUi.text(this, myUsername.isEmpty() ? "No username yet" : "@" + myUsername, 22, true, UiColors.textPrimary());
        text.addView(codeLine);
        top.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        top.addView(pill("Share", true, v -> shareCode()));
        code.addView(top);
        codeStats = SocialUi.text(this, myUsername.isEmpty()
            ? "Choose a username on your contact card. It becomes your referral code."
            : namedMe.size() + (namedMe.size() == 1 ? " person has" : " people have") + " named you as their referrer. "
              + "Only confirm people you know and trust: if someone you referred defaults in a group, your trust score drops too, 10 points for each, up to 30.",
            12, false, UiColors.textSecondary());
        codeStats.setPadding(0, SocialUi.dp(this, 10), 0, 0);
        code.addView(codeStats);
        referralHead.addView(code);

        java.util.Map<String, ReferralStore.Ask> byUid = new java.util.HashMap<>();
        for (ReferralStore.Ask a : asks) byUid.put(a.fromUid, a);
        if (namedMe.isEmpty() && asks.isEmpty()) {
            TextView none = SocialUi.text(this, "Nobody has named you as their referrer yet. Share your code with Invite.", 14, false, UiColors.textSecondary());
            none.setPadding(SocialUi.dp(this, 2), SocialUi.dp(this, 4), 0, 0);
            listBox.addView(none);
            return;
        }
        for (Person p : namedMe) {
            if (!matches(p)) continue;
            final ReferralStore.Ask a = byUid.get(p.uid);
            boolean myTurn = a != null && !a.consented() && !a.referredAddress.isEmpty();
            String state = a == null ? "Has not set up a wallet yet" : a.consented() ? "Confirmed" : a.referredAddress.isEmpty() ? "Waiting for their wallet" : "Waiting for you";
            LinearLayout row = (LinearLayout) friendRowPlain(p, state);
            if (myTurn) {
                LinearLayout buttons = new LinearLayout(this);
                buttons.setOrientation(LinearLayout.HORIZONTAL);
                buttons.setPadding(SocialUi.dp(this, 64), 0, 0, SocialUi.dp(this, 10));
                TextView confirm = pill("Confirm", true, v -> new AlertDialog.Builder(this).setTitle("Confirm " + p.name + "?")
                    .setMessage("You are saying you know " + p.name + " and are referring them. You will confirm with your PIN. "
                        + "If they default in a group, your trust score drops.")
                    .setPositiveButton("Confirm", (d, w) -> ReferralStore.approve(this, a, (ok, err) -> runOnUiThread(() -> {
                        Toast.makeText(this, ok ? "Confirmed. It is recorded when " + p.name + " opens the app." : err, Toast.LENGTH_LONG).show();
                        reload();
                    })))
                    .setNegativeButton("Cancel", null).show());
                TextView decline = pill("Decline", false, v -> ReferralStore.decline(a, (ok, err) -> runOnUiThread(this::reload)));
                ((LinearLayout.LayoutParams) decline.getLayoutParams()).setMarginStart(SocialUi.dp(this, 8));
                buttons.addView(confirm);
                buttons.addView(decline);
                row.addView(buttons, row.getChildCount() - 1);      // above the divider
            }
            listBox.addView(row);
        }
    }

    /** A person with a line of status under the name and no buttons: the base of a referral row. */
    private View friendRowPlain(Person p, String status) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        LinearLayout line = new LinearLayout(this);
        line.setGravity(Gravity.CENTER_VERTICAL);
        line.setPadding(0, SocialUi.dp(this, 10), 0, SocialUi.dp(this, 10));
        int size = SocialUi.dp(this, 52);
        FrameLayout face = new FrameLayout(this);
        TextView initials = SocialUi.text(this, p.initials(), 18, true, 0xFF1A1208);
        initials.setGravity(Gravity.CENTER);
        GradientDrawable disc = new GradientDrawable();
        disc.setShape(GradientDrawable.OVAL);
        disc.setColor(0xFFF0B45A);
        initials.setBackground(disc);
        face.addView(initials, new FrameLayout.LayoutParams(size, size));
        ImageView photo = new ImageView(this);
        photo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        face.addView(photo, new FrameLayout.LayoutParams(size, size));
        LinearLayout.LayoutParams faceLp = new LinearLayout.LayoutParams(size, size);
        faceLp.setMarginEnd(SocialUi.dp(this, 12));
        line.addView(face, faceLp);
        if (p.photo.startsWith("http")) {
            try { com.bumptech.glide.Glide.with(this).load(p.photo).circleCrop().into(photo); initials.setVisibility(View.INVISIBLE); } catch (Exception ignored) { }
        }
        LinearLayout names = new LinearLayout(this);
        names.setOrientation(LinearLayout.VERTICAL);
        names.addView(SocialUi.text(this, p.name, 16, true, UiColors.textPrimary()));
        if (!p.handle().isEmpty()) names.addView(SocialUi.text(this, p.handle(), 13, false, UiColors.textSecondary()));
        names.addView(SocialUi.text(this, status, 12.5f, false, UiColors.textTertiary()));
        line.addView(names, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        line.setClickable(true);
        line.setOnClickListener(v -> openProfile(p));
        row.addView(line);
        View rule = new View(this);
        rule.setBackgroundColor((UiColors.textTertiary() & 0x00FFFFFF) | 0x22000000);
        row.addView(rule, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        return row;
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
