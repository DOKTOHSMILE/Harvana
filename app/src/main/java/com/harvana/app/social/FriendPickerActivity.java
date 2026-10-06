package com.harvana.app.social;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.harvana.app.trust.ProfileTrust;
import com.harvana.app.utils.AppBar;
import com.harvana.app.utils.UiColors;

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

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(UiColors.background());

        search = SocialUi.edit(this, "Search friends or type @username");
        search.setPadding(SocialUi.dp(this, 16), SocialUi.dp(this, 12), SocialUi.dp(this, 16), SocialUi.dp(this, 12));
        search.setCompoundDrawablePadding(SocialUi.dp(this, 8));
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int c, int d) { }
            @Override public void onTextChanged(CharSequence s, int a, int c, int d) { }
            @Override public void afterTextChanged(Editable s) { render(); }
        });
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sp.setMargins(SocialUi.dp(this, 12), SocialUi.dp(this, 8), SocialUi.dp(this, 12), 0);
        root.addView(search, sp);

        hint = SocialUi.text(this, "", 12, false, UiColors.textSecondary());
        hint.setPadding(SocialUi.dp(this, 16), SocialUi.dp(this, 8), SocialUi.dp(this, 16), SocialUi.dp(this, 4));
        root.addView(hint);

        ScrollView scroll = new ScrollView(this);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(SocialUi.dp(this, 12), 0, SocialUi.dp(this, 12), SocialUi.dp(this, 12));
        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(SocialUi.dp(this, 12), SocialUi.dp(this, 8), SocialUi.dp(this, 12), SocialUi.dp(this, 8));
        bar.setBackgroundColor(UiColors.surface());
        allButton = SocialUi.button(this, "Select all", true);
        allButton.setOnClickListener(v -> selectAll());
        bar.addView(allButton);
        addButton = SocialUi.button(this, "Add", false);
        addButton.setOnClickListener(v -> finishWithChosen());
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        ap.leftMargin = SocialUi.dp(this, 8);
        bar.addView(addButton, ap);
        root.addView(bar);

        AppBar.setContent(this, root, "Add friends", "People you follow");
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
            list.addView(SocialUi.text(this, everyone.isEmpty() ? "You do not follow anyone yet. Add friends from the Friends screen, or find someone by username here."
                    : "No friend matches that.", 13, false, UiColors.textSecondary()));
        }
        for (Person p : shown) list.addView(row(p));
        String handle = q.startsWith("@") ? q.substring(1) : q;
        if (handle.length() >= 3 && handle.matches("^[A-Za-z0-9_.]+$") && !hasUsername(handle)) {
            MaterialButton find = SocialUi.button(this, "Find @" + handle.toLowerCase(Locale.US) + " on Harvana", true);
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
        hint.setText(selecting ? n + (n == 1 ? " friend selected. Tap to tick or untick." : " friends selected. Tap to tick or untick.")
                : "Tap a friend to add them. Press and hold to pick several.");
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

    private View row(Person p) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, SocialUi.dp(this, 8), 0, SocialUi.dp(this, 8));

        int size = SocialUi.dp(this, 24);
        FrameLayout face = new FrameLayout(this);
        TextView initials = SocialUi.text(this, p.initials(), 10, true, 0xFFFFFFFF);
        initials.setGravity(Gravity.CENTER);
        android.graphics.drawable.GradientDrawable disc = new android.graphics.drawable.GradientDrawable();
        disc.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        disc.setColors(new int[] {0xFF2BA597, 0xFF0B6B61});
        disc.setOrientation(android.graphics.drawable.GradientDrawable.Orientation.TL_BR);
        initials.setBackground(disc);
        face.addView(initials, new FrameLayout.LayoutParams(size, size));
        ImageView photo = new ImageView(this);
        photo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        face.addView(photo, new FrameLayout.LayoutParams(size, size));
        LinearLayout.LayoutParams faceLp = new LinearLayout.LayoutParams(size, size);
        faceLp.rightMargin = SocialUi.dp(this, 12);
        row.addView(face, faceLp);

        LinearLayout names = new LinearLayout(this);
        names.setOrientation(LinearLayout.VERTICAL);
        names.addView(SocialUi.text(this, p.name, 15, true, UiColors.textPrimary()));
        String sub = (p.handle().isEmpty() ? "" : p.handle()) + (mutual.contains(p.uid) ? (p.handle().isEmpty() ? "" : "  ·  ") + "Friends" : "");
        if (!sub.isEmpty()) names.addView(SocialUi.text(this, sub, 12, false, UiColors.textSecondary()));
        TextView trust = SocialUi.text(this, "", 11, true, UiColors.textSecondary());
        trust.setPadding(SocialUi.dp(this, 8), SocialUi.dp(this, 2), SocialUi.dp(this, 8), SocialUi.dp(this, 2));
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tl.topMargin = SocialUi.dp(this, 3);
        names.addView(trust, tl);
        ProfileTrust.resolve(p.uid, card -> {
            ProfileTrust.bind(trust, card);
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
        });
        row.addView(names, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        if (selecting) {
            CheckBox box = new CheckBox(this);
            box.setChecked(chosen.containsKey(p.uid));
            box.setClickable(false);
            row.addView(box);
        }
        row.setBackground(chosen.containsKey(p.uid) ? tint() : null);
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

    private android.graphics.drawable.Drawable tint() {
        android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
        g.setCornerRadius(SocialUi.dp(this, 12));
        g.setColor((UiColors.accent() & 0x00FFFFFF) | 0x1F000000);
        return g;
    }

    private void finishWithChosen() {
        Consumer<List<Person>> cb = pending;
        pending = null;
        if (cb != null) cb.accept(new ArrayList<>(chosen.values()));
        finish();
    }
}
