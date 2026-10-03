package com.harvana.app.social;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.EditText;
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
 * Friends: find a member by exact username, follow / unfollow, see who follows you, and reach people who follow you back
 * through the contact links they chose to share. There is no member directory to browse.
 */
public class FriendsActivity extends AppCompatActivity {
    private LinearLayout searchResult, listBox, contactMatches;
    private EditText query;
    private MaterialButton tabFollowing, tabFollowers, tabBack;
    private List<Person> following = new ArrayList<>(), followers = new ArrayList<>();
    private boolean showingFollowers;
    /** 0 friends (mutual), 1 following, 2 followers. */
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
        TextView title = SocialUi.text(this, "Friends", 28, true, UiColors.textPrimary());
        header.addView(title);
        header.addView(SocialUi.text(this, "Mutual follows. A photo when they have one, their initials when they don't.", 13, false, UiColors.accent()));
        root.addView(header, 0);

        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabFollowing = SocialUi.button(this, "Friends", false);
        tabFollowers = SocialUi.button(this, "Following", true);
        tabBack = SocialUi.button(this, "Followers", true);
        tabFollowing.setOnClickListener(v -> { tab = 0; render(); });
        tabFollowers.setOnClickListener(v -> { tab = 1; render(); });
        tabBack.setOnClickListener(v -> { tab = 2; render(); });
        tabs.addView(tabFollowing, new LinearLayout.LayoutParams(0, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        tabs.addView(tabFollowers, new LinearLayout.LayoutParams(0, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        tabs.addView(tabBack, new LinearLayout.LayoutParams(0, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(tabs);

        listBox = SocialUi.card(this);
        root.addView(listBox);

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
        MaterialButton scan = SocialUi.button(this, "Find friends in my contacts", true);
        contactMatches = new LinearLayout(this);
        contactMatches.setOrientation(LinearLayout.VERTICAL);
        scan.setOnClickListener(v -> ContactsFinder.find(this, matches -> showMatches(matches)));
        fromContacts.addView(scan);
        fromContacts.addView(contactMatches);
        root.addView(fromContacts);
    }

    @Override protected void onResume() { super.onResume(); reload(); }

    private void reload() {
        SocialStore.following((list, err) -> { following = list; runOnUiThread(this::render); });
        SocialStore.followers((list, err) -> { followers = list; runOnUiThread(this::render); });
    }

    private void showMatches(List<DiscoveryStore.Match> matches) {
        contactMatches.removeAllViews();
        if (matches.isEmpty()) {
            contactMatches.addView(SocialUi.text(this, "None of your contacts have turned on discovery yet. Ask them to enable it on their contact card, or find them by username.", 13, false, UiColors.textSecondary()));
            return;
        }
        Set<String> mine = followingIds();
        for (DiscoveryStore.Match m : matches) {
            boolean already = mine.contains(m.person.uid);
            Person shown = m.contactName == null || m.contactName.isEmpty() ? m.person
                : new Person(m.person.uid, m.person.username, m.contactName + (m.person.name.equals(m.contactName) ? "" : "  \u00b7  " + m.person.name));
            contactMatches.addView(SocialUi.personRow(this, shown, already ? "Following" : "Follow", already, v -> {
                if (already) return;
                SocialStore.follow(m.person, (ok, e) -> runOnUiThread(() -> {
                    if (ok) { Toast.makeText(this, "You now follow " + m.person.name, Toast.LENGTH_SHORT).show(); reload(); showMatches(matches); }
                    else Toast.makeText(this, e, Toast.LENGTH_LONG).show();
                }));
            }, null));
        }
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

    private void render() {
        List<Person> friends = mutual();
        tabFollowing.setText("Friends (" + friends.size() + ")");
        tabFollowers.setText("Following (" + following.size() + ")");
        tabBack.setText("Followers (" + followers.size() + ")");
        listBox.removeAllViews();
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
            listBox.addView(SocialUi.personRow(this, p, action, outlined, v -> {
                if (tab == 2 && !iFollow) {
                    SocialStore.follow(p, (ok, e) -> runOnUiThread(() -> { if (!ok) Toast.makeText(this, e, Toast.LENGTH_LONG).show(); reload(); }));
                    return;
                }
                if (tab == 2) return;
                new AlertDialog.Builder(this).setTitle("Unfollow " + p.name + "?")
                    .setMessage("They leave your friends list, and they will no longer see your contact links.")
                    .setPositiveButton("Unfollow", (d, w) -> SocialStore.unfollow(p.uid, (ok, e) -> runOnUiThread(this::reload)))
                    .setNegativeButton("Cancel", null).show();
            }, v -> openCard(p)));
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
    private void openCard(Person p) {
        SocialStore.contactsOf(p.uid, (card, err) -> runOnUiThread(() -> {
            AlertDialog.Builder d = new AlertDialog.Builder(this).setTitle(p.name + (p.handle().isEmpty() ? "" : "  " + p.handle()));
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
                try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(uris.get(which)))); }
                catch (Exception e) { Toast.makeText(this, "No app can open that.", Toast.LENGTH_SHORT).show(); }
            }).setNegativeButton("Close", null).show();
        }));
    }
}
