package com.harvanna.app.social;

import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.harvanna.app.utils.AppBar;
import com.harvanna.app.utils.UiColors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** My username, phone number and chat / social links, so members I follow (and who follow me back) can reach me off the app. */
public class ContactCardActivity extends AppCompatActivity {
    private EditText username, phone;
    private TextView usernameStatus;
    private LinearLayout linkRows;
    private final List<Object[]> rows = new ArrayList<>(); // {Spinner, EditText, View}

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
        AppBar.setContent(this, sv, "My contact card", "Username, phone and chat links");

        // Username
        LinearLayout u = SocialUi.card(this);
        u.addView(SocialUi.text(this, "Username", 14, true, UiColors.textPrimary()));
        u.addView(SocialUi.text(this, "Unique to you. Others find you by typing it exactly. 3 to 20 letters, numbers, dots or underscores.", 12, false, UiColors.textSecondary()));
        username = SocialUi.edit(this, "e.g. ngozi.eze");
        username.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        u.addView(username);
        usernameStatus = SocialUi.text(this, "", 12, false, UiColors.textSecondary());
        u.addView(usernameStatus);
        MaterialButton claim = SocialUi.button(this, "Save username", false);
        claim.setOnClickListener(v -> claim(claim));
        u.addView(claim);
        root.addView(u);

        // Contacts
        LinearLayout c = SocialUi.card(this);
        c.addView(SocialUi.text(this, "How to reach me off the app", 14, true, UiColors.textPrimary()));
        c.addView(SocialUi.text(this, "Only people I follow who also follow me back can see these.", 12, false, UiColors.textSecondary()));
        phone = SocialUi.edit(this, "Phone number with country code, e.g. +234 801 234 5678");
        phone.setInputType(InputType.TYPE_CLASS_PHONE);
        c.addView(phone);
        linkRows = new LinearLayout(this);
        linkRows.setOrientation(LinearLayout.VERTICAL);
        c.addView(linkRows);
        MaterialButton add = SocialUi.button(this, "+ Add a chat or social link", true);
        add.setOnClickListener(v -> addRow("WhatsApp", ""));
        c.addView(add);
        MaterialButton save = SocialUi.button(this, "Save contact card", false);
        save.setOnClickListener(v -> saveContacts(save));
        c.addView(save);
        root.addView(c);

        load();
    }

    private void load() {
        SocialStore.myUsername((name, err) -> runOnUiThread(() -> {
            if (name != null && !name.isEmpty()) { username.setText(name); usernameStatus.setText("Your username is @" + name); }
            else usernameStatus.setText("You haven't chosen a username yet.");
        }));
        String uid = FirebaseAuth.getInstance().getCurrentUser() == null ? "" : FirebaseAuth.getInstance().getCurrentUser().getUid();
        SocialStore.contactsOf(uid, (card, err) -> runOnUiThread(() -> {
            if (card == null) return;
            Object ph = card.get("phone");
            if (ph != null) phone.setText(String.valueOf(ph));
            Object links = card.get("links");
            if (links instanceof List) for (Object o : (List<?>) links) {
                if (o instanceof Map) addRow(String.valueOf(((Map<?, ?>) o).get("type")), String.valueOf(((Map<?, ?>) o).get("value")));
            }
        }));
    }

    private void claim(MaterialButton btn) {
        String name = UsernameRules.normalize(username.getText().toString());
        String problem = UsernameRules.problem(name);
        if (problem != null) { usernameStatus.setText(problem); return; }
        btn.setEnabled(false);
        usernameStatus.setText("Checking…");
        SocialStore.claimUsername(name, (saved, err) -> runOnUiThread(() -> {
            btn.setEnabled(true);
            if (err != null) usernameStatus.setText(err);
            else { username.setText(saved); usernameStatus.setText("Your username is @" + saved); }
        }));
    }

    private void addRow(String type, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, SocialUi.dp(this, 6), 0, 0);
        Spinner sp = new Spinner(this);
        sp.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, ContactLinks.TYPES));
        for (int i = 0; i < ContactLinks.TYPES.length; i++) if (ContactLinks.TYPES[i].equals(type)) sp.setSelection(i);
        row.addView(sp, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.1f));
        EditText val = SocialUi.edit(this, "Username or number");
        val.setText(value == null || "null".equals(value) ? "" : value);
        row.addView(val, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.6f));
        MaterialButton rm = SocialUi.button(this, "✕", true);
        rm.setMinWidth(0); rm.setMinimumWidth(0);
        row.addView(rm);
        Object[] entry = {sp, val, row};
        rm.setOnClickListener(v -> { linkRows.removeView(row); rows.remove(entry); });
        rows.add(entry);
        linkRows.addView(row);
    }

    private void saveContacts(MaterialButton btn) {
        String ph = phone.getText().toString().trim();
        if (!ph.isEmpty()) {
            String pr = ContactLinks.problem("Phone", ph);
            if (pr != null) { Toast.makeText(this, pr, Toast.LENGTH_LONG).show(); return; }
        }
        List<Map<String, Object>> links = new ArrayList<>();
        for (Object[] r : rows) {
            String type = String.valueOf(((Spinner) r[0]).getSelectedItem());
            String value = ((EditText) r[1]).getText().toString().trim();
            if (value.isEmpty()) continue;
            String pr = ContactLinks.problem(type, value);
            if (pr != null) { Toast.makeText(this, type + ": " + pr, Toast.LENGTH_LONG).show(); return; }
            Map<String, Object> m = new HashMap<>();
            m.put("type", type); m.put("value", value);
            links.add(m);
        }
        btn.setEnabled(false);
        SocialStore.saveContacts(ph, links, (ok, err) -> runOnUiThread(() -> {
            btn.setEnabled(true);
            Toast.makeText(this, err == null ? "Contact card saved." : "Couldn't save: " + err, Toast.LENGTH_LONG).show();
        }));
    }
}
