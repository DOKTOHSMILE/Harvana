package com.harvana.app.social;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.harvana.app.trust.ProfileTrust;
import com.harvana.app.utils.AppBar;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Choose friends to add to a group. Search by name or @username. Tap a friend to add just them; press and hold one to start
 * selecting several: a checkbox appears on every friend and a tap then ticks or unticks. Someone who is not yet a friend can be
 * found by their exact username and added too. Calls back with everyone chosen when the person taps Add.
 */
public class FriendPickerActivity extends AppCompatActivity {

    private static Consumer<List<Person>> pending;
    private static List<Person> preselected = new ArrayList<>();

    /** Opens the picker. {@code result} gets the full chosen list (including those already chosen) when the person confirms. */
    public static void pick(android.app.Activity host, List<Person> already, Consumer<List<Person>> result) {
        pending = result;
        preselected = new ArrayList<>(already);
        host.startActivity(new android.content.Intent(host, FriendPickerActivity.class));
    }

    private final Map<String, Person> everyone = new LinkedHashMap<>();
    private final Map<String, Person> chosen = new LinkedHashMap<>();
    private final java.util.Set<String> mutual = new java.util.HashSet<>();
    private LinearLayout list;
    private TextView hint;
    private MaterialButton addButton, allButton;
    private EditText search;
    private boolean selecting;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        for (Person p : preselected) { everyone.put(p.uid, p); chosen.put(p.uid, p); }
        selecting = !chosen.isEmpty();

        AppBar.setContent(this, com.harvana.app.R.layout.activity_friend_picker, "Add friends", "People you follow");
        search = findViewById(com.harvana.app.R.id.et_friend_picker_search);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int c, int d) { }
            @Override public void onTextChanged(CharSequence s, int a, int c, int d) { }
            @Override public void afterTextChanged(Editable s) { render(); }
        });
        hint = findViewById(com.harvana.app.R.id.tv_friend_picker_hint);
        list = findViewById(com.harvana.app.R.id.friend_picker_list);
        allButton = findViewById(com.harvana.app.R.id.btn_friend_picker_all);
        addButton = findViewById(com.harvana.app.R.id.btn_friend_picker_add);
        addButton.setOnClickListener(v -> finishWithChosen());
        render();
        SocialStore.following((people, err) -> {
            for (Person p : people) if (!everyone.containsKey(p.uid)) everyone.put(p.uid, p);
            SocialStore.followers((back, e2) -> {
                for (Person p : back) mutual.add(p.uid);
                runOnUiThread(this::render);
            });
        });
    }

    @Override protected void onDestroy() { if (isFinishing()) pending = null; super.onDestroy(); }

    private void selectAll() {
        selecting = true;
        for (Person p : visible()) chosen.put(p.uid, p);
        render();
    }

    private List<Person> visible() {
        String q = search.getText().toString().trim().toLowerCase(Locale.US);
        if (q.startsWith("@")) q = q.substring(1);
        List<Person> out = new ArrayList<>();
        for (Person p : everyone.values()) {
            if (q.isEmpty() || p.name.toLowerCase(Locale.US).contains(q) || p.username.toLowerCase(Locale.US).contains(q)) out.add(p);
        }
        java.util.Collections.sort(out, (x, y) -> {
            boolean mx = mutual.contains(x.uid), my = mutual.contains(y.uid);
            if (mx != my) return mx ? -1 : 1;
            return x.name.compareToIgnoreCase(y.name);
        });
        return out;
    }

    private void render() {
        list.removeAllViews();
        List<Person> shown = visible();
        String q = search.getText().toString().trim();
        if (shown.isEmpty()) {
            TextView none = (TextView) getLayoutInflater().inflate(com.harvana.app.R.layout.item_list_empty, list, false);
            none.setText(everyone.isEmpty() ? "No friends yet" : "No match");
            com.harvana.app.utils.EmptyState.illustrate(none, everyone.isEmpty() ? com.harvana.app.R.drawable.illus_empty_groups : com.harvana.app.R.drawable.illus_empty_search);
            list.addView(none);
        }
        for (Person p : shown) list.addView(row(p));
        String handle = q.startsWith("@") ? q.substring(1) : q;
        if (handle.length() >= 3 && handle.matches("^[A-Za-z0-9_.]+$") && !hasUsername(handle)) {
            MaterialButton find = (MaterialButton) getLayoutInflater().inflate(com.harvana.app.R.layout.item_list_button, list, false);
            find.setText("Find @" + handle.toLowerCase(Locale.US));
            find.setOnClickListener(v -> SocialStore.findByUsername(handle, (person, err) -> runOnUiThread(() -> {
                if (person == null) { Toast.makeText(this, err == null ? "Not found." : err, Toast.LENGTH_LONG).show(); return; }
                everyone.put(person.uid, person);
                chosen.put(person.uid, person);
                selecting = true;
                search.setText("");
            })));
            list.addView(find);
        }
        int n = chosen.size();
        hint.setText(n + " selected");
        addButton.setText(n == 0 ? "Add" : "Add " + n);
        addButton.setEnabled(n > 0);
        allButton.setText(selecting && n == visible().size() && n > 0 ? "Clear" : "Select all");
        allButton.setOnClickListener(v -> {
            if (allButton.getText().toString().equals("Clear")) { chosen.clear(); selecting = false; render(); }
            else selectAll();
        });
    }

    private boolean hasUsername(String handle) {
        for (Person p : everyone.values()) if (p.username.equalsIgnoreCase(handle)) return true;
        return false;
    }

    /** One friend: res/layout/item_person_row.xml. */
    private View row(Person p) {
        View row = getLayoutInflater().inflate(com.harvana.app.R.layout.item_person_row, list, false);
        TextView initials = row.findViewById(com.harvana.app.R.id.tv_person_initials);
        initials.setText(p.initials());
        ImageView photo = row.findViewById(com.harvana.app.R.id.iv_person_photo);
        ((TextView) row.findViewById(com.harvana.app.R.id.tv_person_name)).setText(p.name);
        String sub = (p.handle().isEmpty() ? "" : p.handle()) + (mutual.contains(p.uid) ? (p.handle().isEmpty() ? "" : "  ·  ") + "Friends" : "");
        TextView handle = row.findViewById(com.harvana.app.R.id.tv_person_handle);
        if (!sub.isEmpty()) { handle.setText(sub); handle.setVisibility(View.VISIBLE); }
        TextView trust = row.findViewById(com.harvana.app.R.id.tv_person_trust);
        LinearLayout chips = row.findViewById(com.harvana.app.R.id.person_badges);
        ProfileTrust.resolve(p.uid, card -> {
            ProfileTrust.bind(trust, card);
            if (!card.wallet.isEmpty()) com.harvana.app.trust.NodeBadges.load(card.wallet, badges -> {
                if (badges.isEmpty()) return;
                com.harvana.app.trust.BadgeChips.fill(chips, badges, true);
                chips.setVisibility(View.VISIBLE);
            });
            if (!card.icon.isEmpty() && card.icon.startsWith("http")) {
                try { com.bumptech.glide.Glide.with(this).load(card.icon).circleCrop().into(photo); initials.setVisibility(View.INVISIBLE); }
                catch (Exception ignored) { }
            }
        });
        CheckBox box = row.findViewById(com.harvana.app.R.id.cb_person);
        box.setVisibility(selecting ? View.VISIBLE : View.GONE);
        box.setChecked(chosen.containsKey(p.uid));
        if (chosen.containsKey(p.uid)) row.setBackgroundResource(com.harvana.app.R.drawable.bg_row_selected);
        row.setOnClickListener(v -> {
            if (selecting) {
                if (chosen.containsKey(p.uid)) chosen.remove(p.uid); else chosen.put(p.uid, p);
                if (chosen.isEmpty()) selecting = false;
                render();
            } else {
                chosen.put(p.uid, p);
                finishWithChosen();
            }
        });
        row.setOnLongClickListener(v -> {
            selecting = true;
            chosen.put(p.uid, p);
            render();
            return true;
        });
        return row;
    }

    private void finishWithChosen() {
        Consumer<List<Person>> cb = pending;
        pending = null;
        if (cb != null) cb.accept(new ArrayList<>(chosen.values()));
        finish();
    }
}
