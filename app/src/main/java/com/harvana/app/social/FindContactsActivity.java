package com.harvana.app.social;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
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
        AppBar.setContent(this, com.harvana.app.R.layout.activity_find_contacts, "Find friends", "From your contacts");
        search = findViewById(com.harvana.app.R.id.et_find_contacts_search);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int c, int d) { }
            @Override public void onTextChanged(CharSequence s, int a, int c, int d) { }
            @Override public void afterTextChanged(Editable e) { rebuild(); }
        });
        list = findViewById(com.harvana.app.R.id.rv_find_contacts);
        adapter = new Adapter();
        list.setAdapter(adapter);

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
                denied = "Contacts access: off";
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
            ? "None yet"
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
            if (type == LOADING) {
                v = Shimmer.rows(FindContactsActivity.this, 3);
                v.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            } else {
                int layout = type == HEADER ? com.harvana.app.R.layout.item_find_header : type == NOTE ? com.harvana.app.R.layout.item_find_note : com.harvana.app.R.layout.item_find_person;
                v = getLayoutInflater().inflate(layout, parent, false);
            }
            return new RecyclerView.ViewHolder(v) { };
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder h, int i) {
            Row r = shown.get(i);
            View row = h.itemView;
            if (r.type == LOADING) return;
            if (r.type == HEADER) {
                ((TextView) row).setText(r.text);
            } else if (r.type == NOTE) {
                ((TextView) row.findViewById(com.harvana.app.R.id.tv_find_note)).setText(r.text);
                View allow = row.findViewById(com.harvana.app.R.id.btn_find_allow);
                boolean ask = !allowed && !denied.isEmpty();
                allow.setVisibility(ask ? View.VISIBLE : View.GONE);
                allow.setOnClickListener(ask ? v -> start() : null);
            } else if (r.type == MEMBER) {
                bindMember(row, r.match);
            } else {
                bindContact(row, r.entry);
            }
        }
    }

    /** A contact on Harvana: res/layout/item_find_person.xml with Follow (or Following). */
    private void bindMember(View row, DiscoveryStore.Match m) {
        TextView face = row.findViewById(com.harvana.app.R.id.tv_find_initials);
        face.setText(m.person.initials());
        face.setBackgroundResource(com.harvana.app.R.drawable.bg_circle_accent);
        face.setTextColor(UiColors.onAccent());
        TextView name = row.findViewById(com.harvana.app.R.id.tv_find_name);
        name.setText(label(m));
        name.setTypeface(null, android.graphics.Typeface.BOLD);
        String sub = (m.person.handle().isEmpty() ? "" : m.person.handle()) + (m.contactName != null && !m.contactName.isEmpty() && !m.contactName.equals(m.person.name) ? "  ·  " + m.person.name : "");
        sub(row, sub);
        boolean already = followingIds.contains(m.person.uid);
        MaterialButton b = button(row, already);
        b.setText(already ? "Following" : "Follow");
        b.setEnabled(!already);
        b.setOnClickListener(already ? null : v -> follow(m, b));
        row.setOnClickListener(v -> openProfile(m.person.uid));
    }

    /** A phone contact not on Harvana: res/layout/item_find_person.xml with Invite. */
    private void bindContact(View row, PhoneContacts.Entry e) {
        String name = e.name.isEmpty() ? firstOf(e) : e.name;
        TextView face = row.findViewById(com.harvana.app.R.id.tv_find_initials);
        face.setText(initials(name));
        face.setBackgroundResource(com.harvana.app.R.drawable.bg_oval_surface_alt);
        face.setTextColor(UiColors.textPrimary());
        TextView n = row.findViewById(com.harvana.app.R.id.tv_find_name);
        n.setText(name);
        n.setTypeface(null, android.graphics.Typeface.NORMAL);
        sub(row, e.name.isEmpty() ? "" : firstOf(e));
        MaterialButton b = button(row, true);
        b.setText("Invite");
        b.setEnabled(true);
        b.setOnClickListener(v -> invite(e));
        row.setOnClickListener(null);
        row.setClickable(false);
    }

    private static void sub(View row, String text) {
        TextView t = row.findViewById(com.harvana.app.R.id.tv_find_sub);
        t.setText(text);
        t.setVisibility(text.isEmpty() ? View.GONE : View.VISIBLE);
    }

    /** The row's filled or outlined button (the other one is hidden). */
    private static MaterialButton button(View row, boolean outlined) {
        MaterialButton filled = row.findViewById(com.harvana.app.R.id.btn_find_action), line = row.findViewById(com.harvana.app.R.id.btn_find_action_outlined);
        filled.setVisibility(outlined ? View.GONE : View.VISIBLE);
        line.setVisibility(outlined ? View.VISIBLE : View.GONE);
        return outlined ? line : filled;
    }

    private static String initials(String name) {
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) return "?";
        String a = parts[0].substring(0, 1);
        String b = parts.length > 1 ? parts[1].substring(0, 1) : "";
        return (a + b).toUpperCase(Locale.US);
    }
}
