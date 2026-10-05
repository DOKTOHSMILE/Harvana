package com.harvana.app.social;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.harvana.app.utils.AppBar;
import com.harvana.app.utils.Shimmer;
import com.harvana.app.utils.UiColors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Find friends in my contacts, in the style of a messaging app: the contacts who are already on Harvana come first, each with a
 * Follow button, and every other contact follows below with an Invite button. Search narrows both lists by name.
 *
 * Contacts stay on the phone. Numbers and emails are scrambled before they are compared, and only members who chose to be found
 * can match (see DiscoveryStore).
 */
public class FindContactsActivity extends AppCompatActivity {
    private static final int HEADER = 0, MEMBER = 1, CONTACT = 2, NOTE = 3, LOADING = 4;

    private static final class Row {
        final int type;
        String text = "";
        DiscoveryStore.Match match;
        PhoneContacts.Entry entry;
        Row(int type) { this.type = type; }
    }

    private RecyclerView list;
    private EditText search;
    private Adapter adapter;
    private final List<Row> shown = new ArrayList<>();
    private List<DiscoveryStore.Match> members = new ArrayList<>();
    private List<PhoneContacts.Entry> others = new ArrayList<>();
    private final Set<String> followingIds = new HashSet<>();
    private String myUsername = "";
    private boolean allowed, checking = true, loadedContacts;
    private String denied = "";

    @Override public boolean onSupportNavigateUp() { finish(); return true; }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(UiColors.background());
        int pad = SocialUi.dp(this, 16);

        search = SocialUi.edit(this, "Search your contacts");
        search.setPadding(pad, SocialUi.dp(this, 12), pad, SocialUi.dp(this, 12));
        GradientDrawable box = new GradientDrawable();
        box.setColor(UiColors.surface());
        box.setCornerRadius(SocialUi.dp(this, 24));
        box.setStroke(SocialUi.dp(this, 1), (UiColors.textTertiary() & 0x00FFFFFF) | 0x33000000);
        search.setBackground(box);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sp.setMargins(pad, SocialUi.dp(this, 10), pad, SocialUi.dp(this, 6));
        root.addView(search, sp);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int c, int d) { }
            @Override public void onTextChanged(CharSequence s, int a, int c, int d) { }
            @Override public void afterTextChanged(Editable e) { rebuild(); }
        });

        list = new RecyclerView(this);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setClipToPadding(false);
        list.setPadding(pad, SocialUi.dp(this, 4), pad, SocialUi.dp(this, 24));
        adapter = new Adapter();
        list.setAdapter(adapter);
        root.addView(list, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        AppBar.setContent(this, root, "Find friends", "From your contacts");

        SocialStore.myUsername((name, err) -> myUsername = name == null ? "" : name);
        rebuild();
        start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        SocialStore.following((people, err) -> {
            Set<String> ids = new HashSet<>();
            if (people != null) for (Person p : people) ids.add(p.uid);
            runOnUiThread(() -> { followingIds.clear(); followingIds.addAll(ids); rebuild(); });
        });
    }

    // ── Loading ──────────────────────────────────────────────────────────

    private void start() {
        ContactsAccessActivity.ensure(this, granted -> runOnUiThread(() -> {
            allowed = granted;
            if (!granted) {
                checking = false;
                denied = "Contacts access is off. Allow it to see which of your contacts are on Harvana. If nothing happens, turn it on in Settings > Apps > Harvana > Permissions.";
                rebuild();
                return;
            }
            denied = "";
            checking = true;
            rebuild();
            SocialStore.myCountryCode((cc, err) -> runOnUiThread(() -> ContactsFinder.withCountryCode(this, cc, this::scan)));
        }));
    }

    private void scan(String countryCode) {
        new Thread(() -> {
            final List<PhoneContacts.Entry> entries = PhoneContacts.read(this);
            runOnUiThread(() -> { others = entries; loadedContacts = true; rebuild(); });
            DiscoveryStore.lookup(entries, countryCode, (found, err) -> runOnUiThread(() -> {
                checking = false;
                if (err != null) Toast.makeText(this, err, Toast.LENGTH_LONG).show();
                else {
                    members = found;
                    Collections.sort(members, (a, c) -> label(a).compareToIgnoreCase(label(c)));
                }
                rebuild();
            }));
        }).start();
    }

    private static String label(DiscoveryStore.Match m) {
        return m.contactName == null || m.contactName.isEmpty() ? m.person.name : m.contactName;
    }

    // ── Rows ─────────────────────────────────────────────────────────────

    private void rebuild() {
        final String q = search.getText().toString().trim().toLowerCase(Locale.US);
        shown.clear();

        if (!allowed && !denied.isEmpty()) { shown.add(note(denied)); adapter.notifyDataSetChanged(); return; }

        // Names already matched to a member, so the same person is not offered an invite as well.
        Set<String> matchedNames = new HashSet<>();
        for (DiscoveryStore.Match m : members) if (m.contactName != null) matchedNames.add(m.contactName.toLowerCase(Locale.US));

        List<DiscoveryStore.Match> mm = new ArrayList<>();
        for (DiscoveryStore.Match m : members) {
            if (q.isEmpty() || label(m).toLowerCase(Locale.US).contains(q) || m.person.handle().toLowerCase(Locale.US).contains(q)) mm.add(m);
        }
        List<PhoneContacts.Entry> cc = new ArrayList<>();
        for (PhoneContacts.Entry e : others) {
            if (!e.name.isEmpty() && matchedNames.contains(e.name.toLowerCase(Locale.US))) continue;
            String shownName = e.name.isEmpty() ? firstOf(e) : e.name;
            if (shownName.isEmpty()) continue;
            if (q.isEmpty() || shownName.toLowerCase(Locale.US).contains(q)) cc.add(e);
        }

        Row h1 = new Row(HEADER);
        h1.text = checking ? "On Harvana" : "On Harvana  ·  " + mm.size();
        shown.add(h1);
        if (checking && members.isEmpty()) shown.add(new Row(LOADING));
        else if (mm.isEmpty()) shown.add(note(members.isEmpty()
            ? "None of your contacts have turned on discovery yet. Invite them below, or find someone by username on the Friends screen."
            : "No matches for “" + search.getText().toString().trim() + "”."));
        else for (DiscoveryStore.Match m : mm) { Row r = new Row(MEMBER); r.match = m; shown.add(r); }

        Row h2 = new Row(HEADER);
        h2.text = loadedContacts ? "Invite to Harvana  ·  " + cc.size() : "Invite to Harvana";
        shown.add(h2);
        if (!loadedContacts) shown.add(new Row(LOADING));
        else if (cc.isEmpty()) shown.add(note(others.isEmpty() ? "No contacts found on this phone." : "No other contacts match."));
        else for (PhoneContacts.Entry e : cc) { Row r = new Row(CONTACT); r.entry = e; shown.add(r); }
        adapter.notifyDataSetChanged();
    }

    private Row note(String text) { Row r = new Row(NOTE); r.text = text; return r; }

    private static String firstOf(PhoneContacts.Entry e) {
        return !e.phones.isEmpty() ? e.phones.get(0) : !e.emails.isEmpty() ? e.emails.get(0) : "";
    }

    // ── Actions ──────────────────────────────────────────────────────────

    private void follow(DiscoveryStore.Match m, MaterialButton button) {
        button.setEnabled(false);
        SocialStore.follow(m.person, (ok, e) -> runOnUiThread(() -> {
            if (ok) { followingIds.add(m.person.uid); Toast.makeText(this, "You now follow " + m.person.name, Toast.LENGTH_SHORT).show(); rebuild(); }
            else { button.setEnabled(true); Toast.makeText(this, e == null ? "Couldn't follow." : e, Toast.LENGTH_LONG).show(); }
        }));
    }

    private void invite(PhoneContacts.Entry e) {
        String name = e.name.isEmpty() ? "" : e.name.split("\\s+")[0];
        String text = (name.isEmpty() ? "Hi, " : "Hi " + name + ", ") + "join me on Harvana, the savings and trust network."
            + (myUsername.isEmpty() ? "" : " When you create your account, enter @" + myUsername + " as the friend who invited you.");
        Intent send;
        if (!e.phones.isEmpty()) {
            send = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + Uri.encode(e.phones.get(0)))).putExtra("sms_body", text);
        } else {
            send = Intent.createChooser(new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), "Invite");
        }
        try { startActivity(send); }
        catch (Exception ex) {
            startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), "Invite"));
        }
    }

    private void openProfile(String uid) {
        startActivity(new Intent(this, PublicProfileActivity.class).putExtra(PublicProfileActivity.EXTRA_UID, uid));
    }

    // ── Adapter ──────────────────────────────────────────────────────────

    private final class Adapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        @Override public int getItemCount() { return shown.size(); }
        @Override public int getItemViewType(int i) { return shown.get(i).type; }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int type) {
            View v;
            if (type == LOADING) v = Shimmer.rows(FindContactsActivity.this, 3);
            else {
                LinearLayout row = new LinearLayout(FindContactsActivity.this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                v = row;
            }
            v.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return new RecyclerView.ViewHolder(v) { };
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder h, int i) {
            Row r = shown.get(i);
            if (r.type == LOADING) return;
            LinearLayout row = (LinearLayout) h.itemView;
            row.removeAllViews();
            row.setOnClickListener(null);
            row.setClickable(false);
            row.setPadding(0, 0, 0, 0);
            int gap = SocialUi.dp(FindContactsActivity.this, 8);
            if (r.type == HEADER) {
                TextView t = SocialUi.text(FindContactsActivity.this, r.text, 13, true, UiColors.accent());
                t.setAllCaps(true);
                t.setLetterSpacing(0.05f);
                row.setPadding(SocialUi.dp(FindContactsActivity.this, 4), SocialUi.dp(FindContactsActivity.this, 18), 0, gap);
                row.addView(t);
            } else if (r.type == NOTE) {
                TextView t = SocialUi.text(FindContactsActivity.this, r.text, 13, false, UiColors.textSecondary());
                row.setPadding(SocialUi.dp(FindContactsActivity.this, 4), gap, SocialUi.dp(FindContactsActivity.this, 4), gap);
                row.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                if (!allowed && !denied.isEmpty()) {
                    MaterialButton allow = SocialUi.compactButton(FindContactsActivity.this, "Allow", false);
                    allow.setOnClickListener(v -> start());
                    row.addView(allow);
                }
            } else if (r.type == MEMBER) {
                bindMember(row, r.match);
            } else {
                bindContact(row, r.entry);
            }
        }
    }

    private void bindMember(LinearLayout row, DiscoveryStore.Match m) {
        row.setPadding(0, SocialUi.dp(this, 8), 0, SocialUi.dp(this, 8));
        row.addView(avatar(m.person.initials(), UiColors.accent(), UiColors.onAccent()));
        LinearLayout words = new LinearLayout(this);
        words.setOrientation(LinearLayout.VERTICAL);
        words.addView(SocialUi.text(this, label(m), 16, true, UiColors.textPrimary()));
        String sub = (m.person.handle().isEmpty() ? "" : m.person.handle()) + (m.contactName != null && !m.contactName.isEmpty() && !m.contactName.equals(m.person.name) ? "  ·  " + m.person.name : "");
        if (!sub.isEmpty()) words.addView(SocialUi.text(this, sub, 13, false, UiColors.textSecondary()));
        LinearLayout.LayoutParams wp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        wp.setMarginStart(SocialUi.dp(this, 12));
        row.addView(words, wp);
        boolean already = followingIds.contains(m.person.uid);
        final MaterialButton b = SocialUi.compactButton(this, already ? "Following" : "Follow", already);
        if (already) b.setEnabled(false);
        else b.setOnClickListener(v -> follow(m, b));
        row.addView(b);
        row.setClickable(true);
        row.setOnClickListener(v -> openProfile(m.person.uid));
    }

    private void bindContact(LinearLayout row, PhoneContacts.Entry e) {
        row.setPadding(0, SocialUi.dp(this, 8), 0, SocialUi.dp(this, 8));
        String name = e.name.isEmpty() ? firstOf(e) : e.name;
        row.addView(avatar(initials(name), (UiColors.textTertiary() & 0x00FFFFFF) | 0x44000000, UiColors.textPrimary()));
        LinearLayout words = new LinearLayout(this);
        words.setOrientation(LinearLayout.VERTICAL);
        words.addView(SocialUi.text(this, name, 16, false, UiColors.textPrimary()));
        String sub = e.name.isEmpty() ? "" : firstOf(e);
        if (!sub.isEmpty()) words.addView(SocialUi.text(this, sub, 13, false, UiColors.textSecondary()));
        LinearLayout.LayoutParams wp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        wp.setMarginStart(SocialUi.dp(this, 12));
        row.addView(words, wp);
        MaterialButton b = SocialUi.compactButton(this, "Invite", true);
        b.setOnClickListener(v -> invite(e));
        row.addView(b);
    }

    private View avatar(String letters, int fill, int ink) {
        int size = SocialUi.dp(this, 44);
        TextView t = SocialUi.text(this, letters, 16, true, ink);
        t.setGravity(Gravity.CENTER);
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(fill);
        t.setBackground(g);
        t.setLayoutParams(new LinearLayout.LayoutParams(size, size));
        return t;
    }

    private static String initials(String name) {
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) return "?";
        String a = parts[0].substring(0, 1);
        String b = parts.length > 1 ? parts[1].substring(0, 1) : "";
        return (a + b).toUpperCase(Locale.US);
    }
}
