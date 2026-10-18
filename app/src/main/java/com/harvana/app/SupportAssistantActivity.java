package com.harvana.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.harvana.app.support.HelpArticles;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** On-device assistant. It uses approved articles only. It cannot move funds or call an outside model. */
public class SupportAssistantActivity extends AppCompatActivity {

    private final List<JSONObject> turns = new ArrayList<>();
    private final Rows rows = new Rows();
    private EditText input;
    private String category = "Other";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_support_assistant);
        Toolbar bar = findViewById(R.id.toolbar);
        setSupportActionBar(bar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        bar.setNavigationOnClickListener(v -> finish());
        bar.setTitle("Assistant");
        RecyclerView list = findViewById(R.id.assistant_list);
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        list.setLayoutManager(lm);
        list.setAdapter(rows);
        input = findViewById(R.id.assistant_input);
        findViewById(R.id.assistant_send).setOnClickListener(v -> ask());
        findViewById(R.id.assistant_human).setOnClickListener(v -> handoff());
        say(false, "I'm the Harvanna assistant, not a person. I answer from approved help articles. I cannot release escrow, reverse a transfer, or change an account. Ask for a person any time.");
    }

    private void ask() {
        String text = input.getText() == null ? "" : input.getText().toString().trim();
        if (text.isEmpty()) return;
        input.setText("");
        if (HelpArticles.answer(text).text.startsWith("Stop.")) say(true, "[secret removed]");
        else say(true, text);
        HelpArticles.Answer a = HelpArticles.answer(text);
        category = a.category;
        say(false, a.text);
    }

    private void say(boolean mine, String text) {
        try { turns.add(new JSONObject().put("mine", mine).put("text", text)); } catch (Exception ignored) {}
        rows.notifyDataSetChanged();
    }

    private void handoff() {
        StringBuilder summary = new StringBuilder();
        summary.append("Assistant handoff. Category ").append(category).append(".\n");
        int start = Math.max(0, turns.size() - 12);
        for (int i = start; i < turns.size(); i++) {
            JSONObject t = turns.get(i);
            summary.append(t.optBoolean("mine") ? "Member: " : "Assistant: ").append(t.optString("text")).append("\n");
        }
        String draft = summary.toString();
        if (draft.length() > 1800) draft = draft.substring(0, 1800);
        startActivity(new Intent(this, SupportChatActivity.class)
                .putExtra("category", category)
                .putExtra("priority", "Security".equals(category) || "P2P".equals(category) ? "HIGH" : "NORMAL")
                .putExtra("subject", category + " — assistant handoff")
                .putExtra("draft", draft));
    }

    private class Rows extends RecyclerView.Adapter<Rows.VH> {
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_support_bubble, parent, false));
        }
        @Override public void onBindViewHolder(@NonNull VH h, int position) {
            JSONObject row = turns.get(position);
            boolean mine = row.optBoolean("mine");
            int ink = getColor(mine ? R.color.colorTextOnAccent : R.color.colorTextPrimary);
            h.bubble.setBackgroundResource(mine ? R.drawable.bg_bubble_mine : R.drawable.bg_bubble_theirs);
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) h.bubble.getLayoutParams();
            lp.gravity = mine ? Gravity.END : Gravity.START;
            h.bubble.setLayoutParams(lp);
            h.who.setText(mine ? "You" : "Assistant");
            h.who.setTextColor(ink);
            h.body.setText(row.optString("text"));
            h.body.setTextColor(ink);
            h.time.setText("");
            h.image.setVisibility(View.GONE);
        }
        @Override public int getItemCount() { return turns.size(); }
        class VH extends RecyclerView.ViewHolder {
            final View bubble, image; final TextView who, body, time;
            VH(View v) {
                super(v);
                bubble = v.findViewById(R.id.bubble);
                who = v.findViewById(R.id.bubble_who);
                body = v.findViewById(R.id.bubble_body);
                time = v.findViewById(R.id.bubble_time);
                image = v.findViewById(R.id.bubble_image);
            }
        }
    }
}
