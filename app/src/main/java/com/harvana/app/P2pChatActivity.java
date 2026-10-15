package com.harvana.app;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.ListenerRegistration;
import com.harvana.app.p2p.P2pChatStore;
import com.harvana.app.utils.AppBar;
import com.harvana.app.utils.UiColors;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Chat between the two people on a P2P trade. Opened from the trade screen with the order id and the other person's uid.
 * Messages are kept for 30 days after the last one, so what was said can be shown if the trade is disputed.
 */
public class P2pChatActivity extends AppCompatActivity {
    public static final String EXTRA_ORDER = "orderId", EXTRA_OTHER = "otherUid", EXTRA_NAME = "otherName";
    public static final String EXTRA_CHAT = "chatId", EXTRA_AS_MOD = "asModerator";
    private static final int RC_FILE = 4113;

    /** A "Chat with the trader" button at the top of {@code container}. The order and the other person may not be known until the trade loads. */
    public static void addButton(android.app.Activity host, LinearLayout container, java.util.function.Supplier<String> orderId,
                                 java.util.function.Supplier<String> otherUid) {
        if (container == null) return;
        if (container.getOrientation() == LinearLayout.HORIZONTAL) {
            // A row of small links (dispute, help): the chat joins it as one more link.
            TextView link = new TextView(host);
            link.setText("Chat with the trader");
            link.setTextSize(12);
            link.setTypeface(null, Typeface.BOLD);
            link.setTextColor(UiColors.accent());
            int pad = Math.round(10 * host.getResources().getDisplayMetrics().density);
            link.setPadding(pad, pad, pad, pad);
            link.setOnClickListener(v -> {
                String order = orderId.get(), other = otherUid.get();
                if (order == null || order.isEmpty() || other == null || other.isEmpty()) {
                    Toast.makeText(host, "The trade is still loading. Try again in a moment.", Toast.LENGTH_SHORT).show();
                    return;
                }
                android.content.Intent i = new android.content.Intent(host, P2pChatActivity.class);
                i.putExtra(EXTRA_ORDER, order);
                i.putExtra(EXTRA_OTHER, other);
                host.startActivity(i);
            });
            container.addView(link, 0);
            return;
        }
        MaterialButton b = new MaterialButton(host, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        b.setText("Chat with the trader");
        b.setAllCaps(false);
        b.setOnClickListener(v -> {
            String order = orderId.get(), other = otherUid.get();
            if (order == null || order.isEmpty() || other == null || other.isEmpty()) {
                Toast.makeText(host, "The trade is still loading. Try again in a moment.", Toast.LENGTH_SHORT).show();
                return;
            }
            android.content.Intent i = new android.content.Intent(host, P2pChatActivity.class);
            i.putExtra(EXTRA_ORDER, order);
            i.putExtra(EXTRA_OTHER, other);
            host.startActivity(i);
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = Math.round(8 * host.getResources().getDisplayMetrics().density);
        container.addView(b, 0, lp);
    }

    private LinearLayout list, tradeReach;
    private ScrollView scroll;
    private EditText input, reachInput;
    private TextView theirPhone;
    private LinearLayout theirActions;
    private String orderId = "", other = "", savedMine = "", theirNumber = "", chatKey = "", moderatorUid = "";
    private boolean asModerator;
    private ListenerRegistration reg, reachReg, metaReg;
    private List<P2pChatStore.Message> lastMessages = java.util.Collections.emptyList();
    private final java.util.Map<String, String> names = new java.util.HashMap<>();

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        orderId = str(getIntent().getStringExtra(EXTRA_ORDER));
        other = str(getIntent().getStringExtra(EXTRA_OTHER));
        String name = str(getIntent().getStringExtra(EXTRA_NAME));
        asModerator = getIntent().getBooleanExtra(EXTRA_AS_MOD, false);
        String forced = str(getIntent().getStringExtra(EXTRA_CHAT));

        AppBar.setContent(this, R.layout.activity_p2p_chat, name.isEmpty() ? "Trade chat" : name, "Order " + shortId(orderId));
        scroll = findViewById(R.id.chat_scroll);
        list = findViewById(R.id.chat_messages);
        input = findViewById(R.id.et_chat_message);
        input.setFilters(new android.text.InputFilter[] {new android.text.InputFilter.LengthFilter(P2pChatStore.MAX_TEXT)});
        tradeReach = findViewById(R.id.trade_reach);
        reachInput = findViewById(R.id.et_trade_phone);
        theirPhone = findViewById(R.id.tv_trade_their_phone);
        theirActions = findViewById(R.id.trade_reach_actions);
        findViewById(R.id.btn_trade_phone_save).setOnClickListener(v -> saveReach(v));
        android.view.View send = findViewById(R.id.btn_chat_send);
        send.setOnClickListener(v -> send(send));
        findViewById(R.id.btn_chat_attach).setOnClickListener(v -> pickFile());

        // Quick replies for the usual steps of a trade.
        LinearLayout quick = findViewById(R.id.chat_quick_replies);
        for (String q : new String[] {"Hello, I'm here", "I've sent the payment", "Payment received", "Please check your bank app",
                "What name is on the account?", "Give me a few minutes"}) {
            TextView chip = (TextView) getLayoutInflater().inflate(R.layout.item_chat_quick_reply, quick, false);
            chip.setText(q);
            chip.setOnClickListener(v -> { input.setText(q); input.setSelection(q.length()); });
            quick.addView(chip);
        }
        String me = P2pChatStore.me();
        chatKey = !forced.isEmpty() ? forced : P2pChatStore.chatId(orderId, me, other);
        if (asModerator) {
            if (tradeReach != null) tradeReach.setVisibility(android.view.View.GONE);
            reg = P2pChatStore.listenId(chatKey, messages -> runOnUiThread(() -> show(messages)));
            metaReg = P2pChatStore.listenMeta(chatKey, (mod, appeal) -> runOnUiThread(() -> {
                moderatorUid = mod == null ? "" : mod;
                show(lastMessages);
            }));
            return;
        }
        if (name.isEmpty() && !other.isEmpty()) {
            com.harvana.app.trust.ProfileTrust.resolve(other, card -> runOnUiThread(() -> {
                String n = !card.name.isEmpty() ? card.name : (!card.username.isEmpty() ? "@" + card.username : "");
                if (!n.isEmpty() && getSupportActionBar() != null) getSupportActionBar().setTitle(n);
            }));
        }
        P2pChatStore.ensure(orderId, other, (ok, err) -> runOnUiThread(() -> {
            if (!ok) { Toast.makeText(this, err, Toast.LENGTH_LONG).show(); return; }
            P2pChatStore.admitIfAppealed(orderId, other);
            reg = P2pChatStore.listenId(chatKey, messages -> runOnUiThread(() -> show(messages)));
            metaReg = P2pChatStore.listenMeta(chatKey, (mod, appeal) -> runOnUiThread(() -> {
                moderatorUid = mod == null ? "" : mod;
                if (appeal && getSupportActionBar() != null && (getSupportActionBar().getSubtitle() == null
                        || !String.valueOf(getSupportActionBar().getSubtitle()).contains("Appeal"))) {
                    getSupportActionBar().setSubtitle("Appeal · " + shortId(orderId));
                }
                show(lastMessages);
            }));
            reachReg = P2pChatStore.listenReach(orderId, other, (mine, theirs) -> runOnUiThread(() -> showReach(mine, theirs)));
        }));
        prefillOwnNumber();
    }

    @Override protected void onDestroy() {
        if (reg != null) reg.remove();
        if (reachReg != null) reachReg.remove();
        if (metaReg != null) metaReg.remove();
        super.onDestroy();
    }

    private void prefillOwnNumber() {
        String me = P2pChatStore.me();
        if (me.isEmpty()) return;
        com.harvana.app.firestore.UserProfileManager.getInstance(this).getProfile(me, new com.harvana.app.firestore.UserProfileManager.DocCallback() {
            @Override public void onSuccess(java.util.Map<String, Object> data) {
                Object raw = data == null ? null : data.get("phone");
                String n = raw == null ? null : normalizePhone(String.valueOf(raw));
                if (n == null) return;
                runOnUiThread(() -> {
                    if (!savedMine.isEmpty()) return;
                    if (tradeReach != null) tradeReach.setVisibility(android.view.View.GONE);
                    P2pChatStore.saveReach(orderId, other, n, (ok, err) -> runOnUiThread(() -> {
                        if (ok) savedMine = n;
                        else if (tradeReach != null && savedMine.isEmpty()) tradeReach.setVisibility(android.view.View.VISIBLE);
                    }));
                });
            }
            @Override public void onError(String error) { }
        });
    }

    private void showReach(String mine, String theirs) {
        savedMine = mine == null ? "" : mine;
        if (tradeReach != null) tradeReach.setVisibility(savedMine.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
        if (!savedMine.isEmpty() && reachInput.getText().length() == 0) reachInput.setText(savedMine);
        theirNumber = theirs == null ? "" : theirs;
        invalidateOptionsMenu();
        if (theirs == null || theirs.isEmpty()) {
            theirPhone.setVisibility(android.view.View.GONE);
            theirActions.setVisibility(android.view.View.GONE);
            return;
        }
        theirPhone.setText("They left " + theirs);
        theirPhone.setVisibility(android.view.View.VISIBLE);
        theirActions.setVisibility(android.view.View.GONE);
    }

    private void dial() {
        if (theirNumber.isEmpty()) {
            Toast.makeText(this, "They haven't left a number on this trade.", Toast.LENGTH_LONG).show();
            return;
        }
        startActivity(new android.content.Intent(android.content.Intent.ACTION_DIAL, android.net.Uri.parse("tel:" + theirNumber)));
    }

    private void saveReach(android.view.View button) {
        String stored = normalizePhone(reachInput.getText().toString());
        if (stored == null) {
            Toast.makeText(this, "Enter a phone number with the country code.", Toast.LENGTH_LONG).show();
            return;
        }
        if (stored.equals(savedMine)) {
            Toast.makeText(this, "That number is already on this trade.", Toast.LENGTH_SHORT).show();
            return;
        }
        button.setEnabled(false);
        P2pChatStore.saveReach(orderId, other, stored, (ok, err) -> runOnUiThread(() -> {
            button.setEnabled(true);
            if (!ok) { Toast.makeText(this, err, Toast.LENGTH_LONG).show(); return; }
            savedMine = stored;
            if (tradeReach != null) tradeReach.setVisibility(android.view.View.GONE);
            Toast.makeText(this, "Number saved for this trade.", Toast.LENGTH_SHORT).show();
            P2pChatStore.notice(orderId, other, "I left a phone number on this trade, in case one of us is offline.");
        }));
    }

    private static String normalizePhone(String raw) {
        if (com.harvana.app.social.ContactLinks.problem("Phone", raw) != null) return null;
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.length() < 8 || digits.length() > 15) return null;
        return "+" + digits;
    }

    private void send(android.view.View button) {
        String t = input.getText().toString();
        if (t.trim().isEmpty()) return;
        button.setEnabled(false);
        P2pChatStore.Done done = (ok, err) -> runOnUiThread(() -> {
            button.setEnabled(true);
            if (ok) input.setText(""); else Toast.makeText(this, err, Toast.LENGTH_LONG).show();
        });
        if (asModerator) P2pChatStore.sendTo(chatKey, t, done);
        else P2pChatStore.send(orderId, other, t, done);
    }

    private void show(List<P2pChatStore.Message> messages) {
        lastMessages = messages == null ? java.util.Collections.emptyList() : messages;
        list.removeAllViews();
        if (lastMessages.isEmpty()) {
            TextView empty = (TextView) getLayoutInflater().inflate(R.layout.item_list_empty, list, false);
            empty.setText("No messages yet.");
            list.addView(empty);
            return;
        }
        String me = P2pChatStore.me();
        SimpleDateFormat fmt = new SimpleDateFormat("HH:mm", Locale.US);
        SimpleDateFormat dayFmt = new SimpleDateFormat("EEE d MMM", Locale.US);
        String lastDay = "";
        boolean group = moderatorUid != null && !moderatorUid.isEmpty();
        for (P2pChatStore.Message m : lastMessages) {
            String day = dayFmt.format(new Date(m.at));
            if (!day.equals(lastDay)) {
                lastDay = day;
                TextView sep = (TextView) getLayoutInflater().inflate(R.layout.item_chat_day, list, false);
                sep.setText(day);
                list.addView(sep);
            }
            boolean mine = m.from.equals(me);
            android.view.View bubble = getLayoutInflater().inflate(mine ? R.layout.item_chat_bubble_mine : R.layout.item_chat_bubble_theirs, list, false);
            ((TextView) bubble.findViewById(R.id.tv_chat_text)).setText(m.text);
            ((TextView) bubble.findViewById(R.id.tv_chat_time)).setText(fmt.format(new Date(m.at)));
            TextView who = bubble.findViewById(R.id.tv_chat_who);
            if (who != null && group && !mine) {
                who.setVisibility(android.view.View.VISIBLE);
                if (m.from.equals(moderatorUid)) who.setText("Moderator");
                else who.setText(labelFor(m.from));
            }
            if (!m.image.isEmpty()) {
                android.widget.ImageView pic = bubble.findViewById(R.id.iv_chat_picture);
                pic.setVisibility(android.view.View.VISIBLE);
                com.bumptech.glide.Glide.with(this).load(m.image).into(pic);
                final String url = m.image;
                pic.setOnClickListener(v -> com.harvana.app.utils.ImageViewerActivity.open(this, url));
            }
            if (!m.file.isEmpty()) {
                TextView file = bubble.findViewById(R.id.tv_chat_file);
                String label = m.fileName.isEmpty() ? "Open PDF" : m.fileName;
                file.setText(label);
                file.setVisibility(android.view.View.VISIBLE);
                final String url = m.file;
                file.setOnClickListener(v -> {
                    try { startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))); }
                    catch (Exception e) { Toast.makeText(this, "Couldn't open that file.", Toast.LENGTH_LONG).show(); }
                });
            }
            list.addView(bubble);
        }
        scroll.post(() -> scroll.fullScroll(ScrollView.FOCUS_DOWN));
    }

    private String labelFor(String uid) {
        String known = names.get(uid);
        if (known != null) return known;
        names.put(uid, "Trader");
        com.harvana.app.trust.ProfileTrust.resolve(uid, card -> runOnUiThread(() -> {
            String n = card == null ? "" : (!card.name.isEmpty() ? card.name : (!card.username.isEmpty() ? "@" + card.username : ""));
            if (!n.isEmpty()) names.put(uid, n);
            show(lastMessages);
        }));
        return "Trader";
    }

    private void pickFile() {
        android.content.Intent pick = new android.content.Intent(android.content.Intent.ACTION_GET_CONTENT);
        pick.addCategory(android.content.Intent.CATEGORY_OPENABLE);
        pick.setType("*/*");
        pick.putExtra(android.content.Intent.EXTRA_MIME_TYPES, new String[] {"image/*", "application/pdf"});
        try { startActivityForResult(pick, RC_FILE); }
        catch (android.content.ActivityNotFoundException e) { Toast.makeText(this, "No app can pick a file.", Toast.LENGTH_LONG).show(); }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != RC_FILE || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        android.net.Uri uri = data.getData();
        String type = "";
        try { type = getContentResolver().getType(uri); } catch (Exception ignored) { }
        if (type == null) type = "";
        String name = fileName(uri);
        Toast.makeText(this, "Uploading…", Toast.LENGTH_SHORT).show();
        if (type.startsWith("image/")) {
            com.harvana.app.utils.ImageHost.upload(this, uri, "p2p_chat/" + orderId, 1280, new com.harvana.app.utils.ImageHost.Callback() {
                @Override public void onSuccess(String url) {
                    P2pChatStore.sendFile(chatKey, "Photo", url, "", "", (ok, err) -> runOnUiThread(() ->
                            Toast.makeText(P2pChatActivity.this, ok ? "Sent" : (err == null ? "Not sent" : err), Toast.LENGTH_LONG).show()));
                }
                @Override public void onError(String message) { runOnUiThread(() -> Toast.makeText(P2pChatActivity.this, message, Toast.LENGTH_LONG).show()); }
            });
        } else if ("application/pdf".equals(type) || name.toLowerCase(java.util.Locale.US).endsWith(".pdf")) {
            com.harvana.app.utils.ImageHost.uploadPdf(this, uri, "p2p_chat/" + (orderId.isEmpty() ? "trade" : orderId), new com.harvana.app.utils.ImageHost.Callback() {
                @Override public void onSuccess(String url) {
                    P2pChatStore.sendFile(chatKey, name.isEmpty() ? "PDF" : name, "", url, name.isEmpty() ? "Document.pdf" : name, (ok, err) -> runOnUiThread(() ->
                            Toast.makeText(P2pChatActivity.this, ok ? "Sent" : (err == null ? "Not sent" : err), Toast.LENGTH_LONG).show()));
                }
                @Override public void onError(String message) { runOnUiThread(() -> Toast.makeText(P2pChatActivity.this, message, Toast.LENGTH_LONG).show()); }
            });
        } else {
            Toast.makeText(this, "Choose a picture or a PDF.", Toast.LENGTH_LONG).show();
        }
    }

    private String fileName(android.net.Uri uri) {
        String name = "";
        try (android.database.Cursor c = getContentResolver().query(uri, new String[] {android.provider.OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (c != null && c.moveToFirst()) name = c.getString(0);
        } catch (Exception ignored) { }
        if (name == null) name = "";
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (slash >= 0) name = name.substring(slash + 1);
        return name.length() > 80 ? name.substring(0, 80) : name;
    }

    @Override public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.p2p_chat_menu, menu);
        android.view.MenuItem call = menu.findItem(R.id.action_call);
        if (call != null && call.getIcon() != null) {
            android.graphics.drawable.Drawable icon = androidx.core.graphics.drawable.DrawableCompat.wrap(call.getIcon().mutate());
            android.util.TypedValue on = new android.util.TypedValue();
            getTheme().resolveAttribute(R.attr.onAppBarColor, on, true);
            androidx.core.graphics.drawable.DrawableCompat.setTint(icon, on.data);
            call.setIcon(icon);
        }
        return true;
    }

    @Override public boolean onPrepareOptionsMenu(android.view.Menu menu) {
        boolean has = theirNumber != null && !theirNumber.isEmpty();
        android.view.MenuItem call = menu.findItem(R.id.action_call);
        android.view.MenuItem wa = menu.findItem(R.id.action_whatsapp);
        if (call != null) call.setVisible(has && !asModerator);
        if (wa != null) wa.setVisible(has && !asModerator);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == R.id.action_call) { dial(); return true; }
        if (item.getItemId() == R.id.action_whatsapp) {
            String digits = theirNumber.replaceAll("[^0-9]", "");
            startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://wa.me/" + digits)));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private static String shortId(String s) { return s.length() > 14 ? s.substring(0, 12) + "…" : s; }
    private static String str(String s) { return s == null ? "" : s; }
}
