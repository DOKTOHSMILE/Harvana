package com.harvana.app;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.harvana.app.support.ChatPhoto;
import com.harvana.app.support.SupportDesk;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** A member's chat with support. */
public class SupportChatActivity extends AppCompatActivity {

    private EditText subject, body;
    private TextView status, photoReady;
    private RecyclerView list;
    private final Rows adapter = new Rows();
    private String ticketId = "";
    private String category = "Other";
    private String priority = "NORMAL";
    private String pendingPhoto = "";
    private com.google.firebase.firestore.ListenerRegistration ticketsReg;
    private com.google.firebase.firestore.ListenerRegistration threadReg;
    private com.google.firebase.firestore.ListenerRegistration ticketReg;
    // startActivityForResult with a small code: the ActivityResultLauncher's generated request codes do not fit the
    // 16 bits androidx.fragment 1.2.5 accepts ("Can only use lower 16 bits for requestCode").
    private static final int RC_PHOTO = 4108;

    private void pickPhoto() {
        Intent pick = new Intent(Intent.ACTION_GET_CONTENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE);
        try { startActivityForResult(pick, RC_PHOTO); }
        catch (android.content.ActivityNotFoundException e) { android.widget.Toast.makeText(this, "No photo picker", android.widget.Toast.LENGTH_SHORT).show(); }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_PHOTO && resultCode == RESULT_OK && data != null) onPhoto(data.getData());
    }

    @Override
    protected void onDestroy() {
        if (ticketsReg != null) ticketsReg.remove();
        if (threadReg != null) threadReg.remove();
        if (ticketReg != null) ticketReg.remove();
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
        photoReady = findViewById(R.id.support_photo_ready);
        list = findViewById(R.id.support_list);
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        list.setLayoutManager(lm);
        list.setAdapter(adapter);
        findViewById(R.id.support_send).setOnClickListener(v -> send());
        findViewById(R.id.support_photo).setOnClickListener(v -> pickPhoto());
        photoReady.setOnClickListener(v -> {
            pendingPhoto = "";
            photoReady.setVisibility(View.GONE);
        });
        findViewById(R.id.support_reopen).setOnClickListener(v -> SupportDesk.reopen(this, ticketId, new SupportDesk.Cb() {
            @Override public void ok(JSONObject json) { runOnUiThread(() -> toast("Reopened.")); }
            @Override public void fail(String error) { runOnUiThread(() -> toast(error)); }
        }));
        findViewById(R.id.support_rate).setOnClickListener(v -> rate());
        status.setText("Your conversations");
        if (getIntent() != null) {
            String cat = getIntent().getStringExtra("category");
            String pri = getIntent().getStringExtra("priority");
            String about = getIntent().getStringExtra("subject");
            String draft = getIntent().getStringExtra("draft");
            if (cat != null && !cat.isEmpty()) category = cat;
            if (pri != null && !pri.isEmpty()) priority = pri;
            if (about != null && !about.isEmpty()) subject.setText(about);
            if (draft != null && !draft.isEmpty()) body.setText(draft);
            String opened = getIntent().getStringExtra("ticketId");
            if (opened != null && !opened.isEmpty()) {
                ticketId = opened;
                load();
            }
        }
        try { SupportDesk.watchReplies(this); } catch (Throwable ignored) {}
        ticketsReg = SupportDesk.mine(this, new SupportDesk.Cb() {
            @Override public void ok(JSONObject json) { runOnUiThread(() -> { if (ticketId.isEmpty()) showTickets(json.optJSONArray("tickets")); }); }
            @Override public void fail(String error) { runOnUiThread(() -> status.setText(plain(error))); }
        });
    }

    private void onPhoto(Uri uri) {
        if (uri == null) return;
        new Thread(() -> {
            try {
                String jpeg = ChatPhoto.jpeg(this, uri);
                runOnUiThread(() -> {
                    if (jpeg.isEmpty()) { toast("That photo could not be read."); return; }
                    pendingPhoto = jpeg;
                    photoReady.setVisibility(View.VISIBLE);
                });
            } catch (Exception e) {
                runOnUiThread(() -> toast("That photo could not be read."));
            }
        }, "support-photo").start();
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
        String photo = pendingPhoto;
        if (text.isEmpty() && photo.isEmpty()) { body.setError("Write a message"); return; }
        if (ticketId.isEmpty()) {
            String about = subject.getText() == null ? "" : subject.getText().toString().trim();
            if (about.isEmpty()) { subject.setError("Say what this is about"); return; }
            SupportDesk.open(this, about, text, photo, category, priority, new SupportDesk.Cb() {
                @Override public void ok(JSONObject json) {
                    runOnUiThread(() -> {
                        ticketId = json.optString("ticketId", "");
                        clearComposer();
                        status.setText("Sent. Ticket " + ticketId);
                        load();
                    });
                }
                @Override public void fail(String error) { runOnUiThread(() -> toast(error)); }
            });
            return;
        }
        SupportDesk.send(this, ticketId, text, photo, new SupportDesk.Cb() {
            @Override public void ok(JSONObject json) { runOnUiThread(() -> { clearComposer(); load(); }); }
            @Override public void fail(String error) { runOnUiThread(() -> toast(error)); }
        });
    }

    private void clearComposer() {
        body.setText("");
        pendingPhoto = "";
        photoReady.setVisibility(View.GONE);
    }

    private void load() {
        if (ticketId.isEmpty()) return;
        subject.setVisibility(View.GONE);
        if (ticketReg != null) ticketReg.remove();
        ticketReg = SupportDesk.ticket(ticketId, new SupportDesk.Cb() {
            @Override public void ok(JSONObject json) { runOnUiThread(() -> applyTicket(json)); }
            @Override public void fail(String error) { runOnUiThread(() -> toast(error)); }
        });
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
                    if (!rows.isEmpty()) list.scrollToPosition(rows.size() - 1);
                });
            }
            @Override public void fail(String error) { runOnUiThread(() -> toast(error)); }
        });
    }

    private void applyTicket(JSONObject json) {
        String st = json.optString("status", "OPEN");
        boolean closed = "RESOLVED".equals(st) || "CLOSED".equals(st);
        findViewById(R.id.support_closed_actions).setVisibility(closed ? View.VISIBLE : View.GONE);
        findViewById(R.id.support_body).setEnabled(!closed);
        findViewById(R.id.support_send).setEnabled(!closed);
        findViewById(R.id.support_photo).setEnabled(!closed);
        String cat = json.optString("category", "");
        status.setText("Ticket " + ticketId + (cat.isEmpty() ? "" : " · " + cat) + " · " + st.replace('_', ' '));
    }

    private void rate() {
        String[] stars = new String[] { "1", "2", "3", "4", "5" };
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("How was this?")
                .setItems(stars, (d, which) -> SupportDesk.rate(this, ticketId, which + 1, new SupportDesk.Cb() {
                    @Override public void ok(JSONObject json) { runOnUiThread(() -> toast("Thank you.")); }
                    @Override public void fail(String error) { runOnUiThread(() -> toast(error)); }
                }))
                .show();
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

    private void showPhoto(String b64) {
        Bitmap bmp = ChatPhoto.decode(b64);
        if (bmp == null) return;
        Dialog d = new Dialog(this);
        ImageView iv = new ImageView(this);
        iv.setImageBitmap(bmp);
        iv.setAdjustViewBounds(true);
        d.setContentView(iv);
        d.show();
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
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_support_bubble, parent, false);
            return new VH(v);
        }
        @Override public void onBindViewHolder(@NonNull VH h, int position) {
            JSONObject row = items.get(position);
            if (tickets) {
                paint(h, false, row.optString("subject", "Conversation") + " · " + row.optString("status", ""), row.optString("lastMessage", ""), "", 0);
                h.itemView.setOnClickListener(v -> {
                    ticketId = row.optString("ticketId", "");
                    load();
                });
                return;
            }
            boolean mine = "MEMBER".equals(row.optString("side")) || "You".equals(row.optString("from"));
            paint(h, mine, row.optString("from"), row.optString("body", ""), row.optString("image", ""), row.optLong("at"));
            h.itemView.setOnClickListener(null);
        }
        private void paint(VH h, boolean mine, String who, String text, String image, long at) {
            int ink = getColor(mine ? R.color.colorTextOnAccent : R.color.colorTextPrimary);
            int mute = getColor(mine ? R.color.colorTextOnAccentMuted : R.color.colorTextSecondary);
            h.bubble.setBackgroundResource(mine ? R.drawable.bg_bubble_mine : R.drawable.bg_bubble_theirs);
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) h.bubble.getLayoutParams();
            lp.gravity = mine ? Gravity.END : Gravity.START;
            h.bubble.setLayoutParams(lp);
            h.who.setText(who);
            h.who.setTextColor(mute);
            h.body.setText(text);
            h.body.setTextColor(ink);
            h.body.setVisibility(text == null || text.isEmpty() ? View.GONE : View.VISIBLE);
            h.time.setTextColor(mute);
            h.time.setText(at > 0 ? DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(at)) : "");
            if (image == null || image.isEmpty()) {
                h.image.setVisibility(View.GONE);
                h.image.setImageDrawable(null);
                h.image.setOnClickListener(null);
            } else {
                h.image.setVisibility(View.VISIBLE);
                h.image.setImageDrawable(null);
                h.image.setTag(image);
                h.image.setOnClickListener(v -> showPhoto(image));
                new Thread(() -> {
                    Bitmap bmp = ChatPhoto.decode(image);
                    h.image.post(() -> { if (image.equals(h.image.getTag())) h.image.setImageBitmap(bmp); });
                }, "support-decode").start();
            }
        }
        @Override public int getItemCount() { return items.size(); }
        class VH extends RecyclerView.ViewHolder {
            final View bubble; final TextView who, body, time; final ImageView image;
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
