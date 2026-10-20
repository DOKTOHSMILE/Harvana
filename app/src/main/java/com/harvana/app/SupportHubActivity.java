package com.harvana.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.harvana.app.support.HelpArticles;
import com.harvana.app.support.SupportDesk;

import org.json.JSONArray;
import org.json.JSONObject;

/** Help home. The assistant is labelled as an assistant. Money and security go to a person. */
public class SupportHubActivity extends AppCompatActivity {

    private static final String[] CATEGORIES = {
            "Account", "Wallet", "Transfers", "P2P", "Escrow", "Groups", "Staking", "Security", "App", "Other"
    };
    private com.google.firebase.firestore.ListenerRegistration reg;

    @Override
    protected void onDestroy() {
        if (reg != null) reg.remove();
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_support_hub);
        Toolbar bar = findViewById(R.id.toolbar);
        setSupportActionBar(bar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        bar.setNavigationOnClickListener(v -> finish());
        bar.setTitle("Help & support");
        EditText search = findViewById(R.id.hub_search);
        TextView results = findViewById(R.id.hub_results);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                String found = HelpArticles.search(s.toString());
                results.setText(found.isEmpty() && s.length() > 0 ? "No approved answer matches that." : found);
            }
        });
        findViewById(R.id.hub_assistant).setOnClickListener(v -> startActivity(new Intent(this, SupportAssistantActivity.class)));
        findViewById(R.id.hub_person).setOnClickListener(v -> chat("Other", "NORMAL", "", ""));
        findViewById(R.id.hub_security).setOnClickListener(v -> chat("Security", "URGENT", "Security report", ""));
        LinearLayout cats = findViewById(R.id.hub_categories);
        for (String category : CATEGORIES) {
            MaterialButton b = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
            b.setAllCaps(false);
            b.setText(category);
            b.setOnClickListener(v -> chat(category, "Security".equals(category) ? "URGENT" : "NORMAL", category, ""));
            cats.addView(b);
        }
        LinearLayout tickets = findViewById(R.id.hub_tickets);
        reg = SupportDesk.mine(this, new SupportDesk.Cb() {
            @Override public void ok(JSONObject json) { runOnUiThread(() -> showTickets(tickets, json.optJSONArray("tickets"))); }
            @Override public void fail(String error) { runOnUiThread(() -> {
                tickets.removeAllViews();
                TextView t = new TextView(SupportHubActivity.this);
                t.setText(error == null ? "Sign in to see your requests." : error);
                t.setTextColor(getColor(R.color.colorTextSecondary));
                tickets.addView(t);
            }); }
        });
    }

    private void showTickets(LinearLayout host, JSONArray arr) {
        host.removeAllViews();
        if (arr == null || arr.length() == 0) {
            TextView empty = new TextView(this);
            empty.setText("No requests yet.");
            com.harvana.app.utils.EmptyState.illustrate(empty, com.harvana.app.R.drawable.illus_empty_chat);
            empty.setTextColor(getColor(R.color.colorTextSecondary));
            host.addView(empty);
            return;
        }
        for (int i = 0; i < arr.length(); i++) {
            JSONObject t = arr.optJSONObject(i);
            if (t == null) continue;
            MaterialButton row = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
            row.setAllCaps(false);
            String id = t.optString("ticketId");
            row.setText(t.optString("subject", "Request") + " · " + t.optString("status", ""));
            row.setOnClickListener(v -> startActivity(new Intent(this, SupportChatActivity.class).putExtra("ticketId", id)));
            host.addView(row);
        }
    }

    private void chat(String category, String priority, String subject, String draft) {
        startActivity(new Intent(this, SupportChatActivity.class)
                .putExtra("category", category)
                .putExtra("priority", priority)
                .putExtra("subject", subject)
                .putExtra("draft", draft));
    }
}
