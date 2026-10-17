package com.harvana.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.harvana.app.support.SupportDesk;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** A member's chat with support. Every message is signed by the wallet on this phone. */
public class SupportChatActivity extends AppCompatActivity {

    private EditText subject, body;
    private TextView status;
    private final Rows adapter = new Rows();
    private String ticketId = "";
    private com.google.firebase.firestore.ListenerRegistration ticketsReg;
    private com.google.firebase.firestore.ListenerRegistration threadReg;

    @Override
    protected void onDestroy() {
        if (ticketsReg != null) ticketsReg.remove();
        if (threadReg != null) threadReg.remove();
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_member_support);
        Toolbar bar = findViewById(R.id.toolbar);
        setSupportActionBar(bar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        bar.setNavigationOnClickListener(v -> finish());
        bar.setTitle("Support");
        subject = findViewById(R.id.support_subject);
        body = findViewById(R.id.support_body);
        status = findViewById(R.id.support_status);
        RecyclerView list = findViewById(R.id.support_list);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);
        findViewById(R.id.support_send).setOnClickListener(v -> send());
        status.setText("Your conversations");
        ticketsReg = SupportDesk.mine(this, new SupportDesk.Cb() {
            @Override public void ok(JSONObject json) { runOnUiThread(() -> { if (ticketId.isEmpty()) showTickets(json.optJSONArray("tickets")); }); }
            @Override public void fail(String error) { runOnUiThread(() -> status.setText(plain(error))); }
        });
    }

    private void showTickets(JSONArray tickets) {
        List<JSONObject> rows = new ArrayList<>();
        if (tickets != null) for (int i = 0; i < tickets.length(); i++) {
            JSONObject t = tickets.optJSONObject(i);
            if (t != null) rows.add(t);
        }
        adapter.set(rows, true);
        if (rows.isEmpty()) status.setText("No conversations yet. Write one below.");
    }

    private void send() {
        String text = body.getText() == null ? "" : body.getText().toString().trim();
        if (text.isEmpty()) { body.setError("Write a message"); return; }
        if (ticketId.isEmpty()) {
            String about = subject.getText() == null ? "" : subject.getText().toString().trim();
            if (about.isEmpty()) { subject.setError("Say what this is about"); return; }
            SupportDesk.open(this, about, text, new SupportDesk.Cb() {
                @Override public void ok(JSONObject json) {
                    runOnUiThread(() -> {
                        ticketId = json.optString("ticketId", "");
                        body.setText("");
                        status.setText("Sent. Ticket " + ticketId);
                        load();
                    });
                }
                @Override public void fail(String error) { runOnUiThread(() -> toast(error)); }
            });
            return;
        }
        SupportDesk.send(this, ticketId, text, new SupportDesk.Cb() {
            @Override public void ok(JSONObject json) { runOnUiThread(() -> { body.setText(""); load(); }); }
            @Override public void fail(String error) { runOnUiThread(() -> toast(error)); }
        });
    }

    private void load() {
        if (ticketId.isEmpty()) return;
        subject.setVisibility(View.GONE);
        if (threadReg != null) threadReg.remove();
        threadReg = SupportDesk.thread(ticketId, new SupportDesk.Cb() {
            @Override public void ok(JSONObject json) {
                runOnUiThread(() -> {
                    JSONArray arr = json.optJSONArray("messages");
                    List<JSONObject> rows = new ArrayList<>();
                    if (arr != null) for (int i = 0; i < arr.length(); i++) {
                        JSONObject m = arr.optJSONObject(i);
                        if (m != null) rows.add(m);
                    }
                    adapter.set(rows, false);
                    status.setText("Ticket " + ticketId);
                });
            }
            @Override public void fail(String error) { runOnUiThread(() -> toast(error)); }
        });
    }

    private void toast(String error) {
        Toast.makeText(this, plain(error), Toast.LENGTH_LONG).show();
    }

    private static String plain(String error) {
        if (error == null || error.isEmpty()) return "Could not reach support.";
        if (error.contains("TOO_MANY")) return "Too many messages. Wait a while and try again.";
        if (error.contains("TIMESTAMP")) return "This phone's clock is too far off. Set the time and try again.";
        return error.length() > 180 ? error.substring(0, 180) : error;
    }

    private class Rows extends RecyclerView.Adapter<Rows.VH> {
        private final List<JSONObject> items = new ArrayList<>();
        private boolean tickets;
        void set(List<JSONObject> rows, boolean tickets) {
            items.clear();
            items.addAll(rows);
            this.tickets = tickets;
            notifyDataSetChanged();
        }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView tv = new TextView(parent.getContext());
            int pad = (int) (16 * parent.getResources().getDisplayMetrics().density);
            tv.setPadding(pad, pad, pad, pad);
            tv.setTextColor(0xFF1C1917);
            tv.setTextSize(15);
            return new VH(tv);
        }
        @Override public void onBindViewHolder(@NonNull VH holder, int position) {
            JSONObject row = items.get(position);
            TextView tv = (TextView) holder.itemView;
            if (tickets) {
                tv.setText(row.optString("subject", "Conversation") + "\n" + row.optString("lastMessage", ""));
                tv.setOnClickListener(v -> {
                    ticketId = row.optString("ticketId", "");
                    load();
                });
            } else {
                String who = row.optString("from");
                tv.setText(who + "\n" + row.optString("body", ""));
                tv.setOnClickListener(null);
            }
        }
        @Override public int getItemCount() { return items.size(); }
        class VH extends RecyclerView.ViewHolder { VH(View v) { super(v); } }
    }
}
