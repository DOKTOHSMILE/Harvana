package com.harvana.app.social;

import android.app.Activity;
import android.app.AlertDialog;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.harvana.app.group.InviteManager;
import com.harvana.app.utils.UiColors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * "Add people" block used when creating a group and in the invite sheet: pick from friends you follow, look someone up by
 * username, or choose one entry from the phone's contacts (invited by email). Nothing is sent until the caller sends.
 */
public final class PeoplePicker {
    private PeoplePicker() {}

    public static final class Selection {
        public final List<Person> people = new ArrayList<>();
        public final List<String> emails = new ArrayList<>();
        public int count() { return people.size() + emails.size(); }
        boolean hasPerson(String uid) { for (Person p : people) if (p.uid.equals(uid)) return true; return false; }
    }

    public interface Done { void onDone(int sent, int failed); }

    /** The "Add people" card. {@code sel} is filled as the person picks. */
    public static View section(Activity a, Selection sel) {
        LinearLayout box = SocialUi.card(a);
        box.addView(SocialUi.text(a, "Add people (optional)", 14, true, UiColors.textPrimary()));
        box.addView(SocialUi.text(a, "They get an invitation with the group's rules and decide for themselves.", 12, false, UiColors.textSecondary()));
        final TextView summary = SocialUi.text(a, "", 12, false, UiColors.textSecondary());
        summary.setPadding(0, SocialUi.dp(a, 6), 0, SocialUi.dp(a, 6));
        final Runnable refresh = () -> {
            if (sel.count() == 0) { summary.setText("No one added yet. You can also invite people later from the group."); return; }
            StringBuilder sb = new StringBuilder("Will invite: ");
            for (Person p : sel.people) sb.append(p.name).append(", ");
            for (String e : sel.emails) sb.append(e).append(", ");
            summary.setText(sb.substring(0, sb.length() - 2));
        };
        refresh.run();
        box.addView(summary);

        LinearLayout row = new LinearLayout(a);
        row.setOrientation(LinearLayout.HORIZONTAL);
        MaterialButton friends = SocialUi.button(a, "Add friends", true);
        MaterialButton username = SocialUi.button(a, "Username", true);
        MaterialButton contacts = SocialUi.button(a, "Contacts", true);
        friends.setOnClickListener(v -> chooseFriends(a, sel, refresh));
        username.setOnClickListener(v -> byUsername(a, sel, refresh));
        contacts.setOnClickListener(v -> new AlertDialog.Builder(a).setTitle("From my contacts")
            .setItems(new String[]{"Members already on Harvana", "Invite a contact by email"}, (d, which) -> {
                if (which == 0) ContactsFinder.find(a, matches -> chooseMatches(a, sel, matches, refresh));
                else ContactPickerActivity.pickEmail(a, (email, name) -> a.runOnUiThread(() -> {
                    String e = InviteManager.normalizeEmail(email);
                    if (!InviteManager.looksLikeEmail(e)) { Toast.makeText(a, "That contact has no valid email address.", Toast.LENGTH_LONG).show(); return; }
                    if (!sel.emails.contains(e)) sel.emails.add(e);
                    refresh.run();
                }));
            }).setNegativeButton("Cancel", null).show());
        for (View b : new View[]{friends, username, contacts}) {
            row.addView(b, new LinearLayout.LayoutParams(0, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        }
        box.addView(row);
        return box;
    }

    /** Opens the friend picker: search, tap one friend, or press and hold to tick several. */
    private static void chooseFriends(Activity a, Selection sel, Runnable refresh) {
        FriendPickerActivity.pick(a, sel.people, chosen -> {
            sel.people.clear();
            sel.people.addAll(chosen);
            a.runOnUiThread(refresh);
        });
    }

    private static void chooseMatches(Activity a, Selection sel, List<DiscoveryStore.Match> matches, Runnable refresh) {
        if (matches.isEmpty()) {
            Toast.makeText(a, "None of your contacts have turned on discovery yet.", Toast.LENGTH_LONG).show();
            return;
        }
        final String[] names = new String[matches.size()];
        final boolean[] checked = new boolean[matches.size()];
        for (int i = 0; i < matches.size(); i++) {
            DiscoveryStore.Match m = matches.get(i);
            names[i] = (m.contactName == null || m.contactName.isEmpty() ? m.person.name : m.contactName) + (m.person.handle().isEmpty() ? "" : "  " + m.person.handle());
            checked[i] = sel.hasPerson(m.person.uid);
        }
        new AlertDialog.Builder(a).setTitle("Contacts on Harvana")
            .setMultiChoiceItems(names, checked, (d, which, on) -> checked[which] = on)
            .setPositiveButton("Add", (d, w) -> {
                for (int i = 0; i < matches.size(); i++) {
                    Person p = matches.get(i).person;
                    if (checked[i] && !sel.hasPerson(p.uid)) sel.people.add(p);
                }
                refresh.run();
            }).setNegativeButton("Cancel", null).show();
    }

    private static void byUsername(Activity a, Selection sel, Runnable refresh) {
        final EditText et = SocialUi.edit(a, "@username");
        et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        new AlertDialog.Builder(a).setTitle("Find by username").setView(et)
            .setPositiveButton("Add", (d, w) -> SocialStore.findByUsername(et.getText().toString(), (p, err) -> a.runOnUiThread(() -> {
                if (p == null) { Toast.makeText(a, err, Toast.LENGTH_LONG).show(); return; }
                if (!sel.hasPerson(p.uid)) sel.people.add(p);
                refresh.run();
            }))).setNegativeButton("Cancel", null).show();
    }

    /** Sends an invitation to every picked person and email. Errors are counted, not thrown. */
    public static void sendAll(Activity ctx, String groupId, Map<String, Object> group, Selection sel, String message, Done done) {
        final int total = sel.count();
        if (total == 0) { if (done != null) done.onDone(0, 0); return; }
        final AtomicInteger left = new AtomicInteger(total), failed = new AtomicInteger();
        InviteManager.Done one = err -> {
            if (err != null) failed.incrementAndGet();
            if (left.decrementAndGet() == 0 && done != null) done.onDone(total - failed.get(), failed.get());
        };
        for (Person p : sel.people) InviteManager.sendToUser(ctx, groupId, group, p, message, one);
        for (String e : sel.emails) InviteManager.send(ctx, groupId, group, e, message, one);
    }
}
