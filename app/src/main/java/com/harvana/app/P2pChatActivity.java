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

    private LinearLayout list;
    private ScrollView scroll;
    private EditText input, reachInput;
    private TextView theirPhone;
    private LinearLayout theirActions;
    private String orderId = "", other = "", savedMine = "";
    private ListenerRegistration reg, reachReg;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        orderId = str(getIntent().getStringExtra(EXTRA_ORDER));
        other = str(getIntent().getStringExtra(EXTRA_OTHER));
        String name = str(getIntent().getStringExtra(EXTRA_NAME));

        AppBar.setContent(this, R.layout.activity_p2p_chat, name.isEmpty() ? "Trade chat" : name, "Order " + shortId(orderId));
        scroll = findViewById(R.id.chat_scroll);
        list = findViewById(R.id.chat_messages);
        input = findViewById(R.id.et_chat_message);
        input.setFilters(new android.text.InputFilter[] {new android.text.InputFilter.LengthFilter(P2pChatStore.MAX_TEXT)});
        reachInput = findViewById(R.id.et_trade_phone);
        theirPhone = findViewById(R.id.tv_trade_their_phone);
        theirActions = findViewById(R.id.trade_reach_actions);
        findViewById(R.id.btn_trade_phone_save).setOnClickListener(v -> saveReach(v));
        android.view.View send = findViewById(R.id.btn_chat_send);
        send.setOnClickListener(v -> send(send));

        // Quick replies for the usual steps of a trade.
        LinearLayout quick = findViewById(R.id.chat_quick_replies);
        for (String q : new String[] {"Hello, I'm here", "I've sent the payment", "Payment received", "Please check your bank app",
                "What name is on the account?", "Give me a few minutes"}) {
            TextView chip = (TextView) getLayoutInflater().inflate(R.layout.item_chat_quick_reply, quick, false);
            chip.setText(q);
            chip.setOnClickListener(v -> { input.setText(q); input.setSelection(q.length()); });
            quick.addView(chip);
        }
        if (name.isEmpty() && !other.isEmpty()) {
            com.harvana.app.trust.ProfileTrust.resolve(other, card -> runOnUiThread(() -> {
                String n = !card.name.isEmpty() ? card.name : (!card.username.isEmpty() ? "@" + card.username : "");
                if (!n.isEmpty() && getSupportActionBar() != null) getSupportActionBar().setTitle(n);
            }));
        }
        P2pChatStore.ensure(orderId, other, (ok, err) -> runOnUiThread(() -> {
            if (!ok) { Toast.makeText(this, err, Toast.LENGTH_LONG).show(); return; }
            reg = P2pChatStore.listen(orderId, other, messages -> runOnUiThread(() -> show(messages)));
            reachReg = P2pChatStore.listenReach(orderId, other, (mine, theirs) -> runOnUiThread(() -> showReach(mine, theirs)));
        }));
        prefillOwnNumber();
    }

    @Override protected void onDestroy() {
        if (reg != null) reg.remove();
        if (reachReg != null) reachReg.remove();
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
                runOnUiThread(() -> { if (reachInput.getText().length() == 0) reachInput.setText(n); });
            }
            @Override public void onError(String error) { }
        });
    }

    private void showReach(String mine, String theirs) {
        savedMine = mine == null ? "" : mine;
        if (reachInput.getText().length() == 0 && !savedMine.isEmpty()) reachInput.setText(savedMine);
        if (theirs == null || theirs.isEmpty()) {
            theirPhone.setVisibility(android.view.View.GONE);
            theirActions.setVisibility(android.view.View.GONE);
            return;
        }
        theirPhone.setText("They left " + theirs);
        theirPhone.setVisibility(android.view.View.VISIBLE);
        theirActions.setVisibility(android.view.View.VISIBLE);
        String digits = theirs.replaceAll("[^0-9]", "");
        findViewById(R.id.btn_trade_call).setOnClickListener(v ->
            startActivity(new android.content.Intent(android.content.Intent.ACTION_DIAL, android.net.Uri.parse("tel:" + theirs))));
        findViewById(R.id.btn_trade_whatsapp).setOnClickListener(v ->
            startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://wa.me/" + digits))));
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
        P2pChatStore.send(orderId, other, t, (ok, err) -> runOnUiThread(() -> {
            button.setEnabled(true);
            if (ok) input.setText(""); else Toast.makeText(this, err, Toast.LENGTH_LONG).show();
        }));
    }

    private void show(List<P2pChatStore.Message> messages) {
        list.removeAllViews();
        if (messages.isEmpty()) {
            TextView empty = (TextView) getLayoutInflater().inflate(R.layout.item_list_empty, list, false);
            empty.setText("No messages yet.");
            list.addView(empty);
            return;
        }
        String me = P2pChatStore.me();
        SimpleDateFormat fmt = new SimpleDateFormat("HH:mm", Locale.US);
        SimpleDateFormat dayFmt = new SimpleDateFormat("EEE d MMM", Locale.US);
        String lastDay = "";
        for (P2pChatStore.Message m : messages) {
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
            if (!m.image.isEmpty()) {
                // A payment proof: the picture itself, tap to open it full size.
                android.widget.ImageView pic = bubble.findViewById(R.id.iv_chat_picture);
                pic.setVisibility(android.view.View.VISIBLE);
                com.bumptech.glide.Glide.with(this).load(m.image).into(pic);
                final String url = m.image;
                pic.setOnClickListener(v -> com.harvana.app.utils.ImageViewerActivity.open(this, url));
            }
            list.addView(bubble);
        }
        scroll.post(() -> scroll.fullScroll(ScrollView.FOCUS_DOWN));
    }


    private static String shortId(String s) { return s.length() > 14 ? s.substring(0, 12) + "…" : s; }
    private static String str(String s) { return s == null ? "" : s; }
}
