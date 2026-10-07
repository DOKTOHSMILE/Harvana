package com.harvana.app;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
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
    private EditText input;
    private String orderId = "", other = "";
    private ListenerRegistration reg;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        orderId = str(getIntent().getStringExtra(EXTRA_ORDER));
        other = str(getIntent().getStringExtra(EXTRA_OTHER));
        String name = str(getIntent().getStringExtra(EXTRA_NAME));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(UiColors.background());

        // Safety line: one short card, not a wall of text.
        TextView notice = text("Pay only into the account on the trade screen. Never pay to a link or account sent here. "
                + "Chats are kept 30 days in case of a dispute.", 12, false, UiColors.textSecondary());
        notice.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_shield_line, 0, 0, 0);
        notice.setCompoundDrawablePadding(dp(10));
        notice.setGravity(Gravity.CENTER_VERTICAL);
        GradientDrawable nb = new GradientDrawable();
        nb.setCornerRadius(dp(12));
        nb.setColor((UiColors.warning() & 0x00FFFFFF) | 0x1F000000);
        notice.setBackground(nb);
        notice.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        nlp.setMargins(dp(12), dp(8), dp(12), dp(4));
        root.addView(notice, nlp);

        scroll = new ScrollView(this);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(12), dp(4), dp(12), dp(8));
        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // Quick replies for the usual steps of a trade.
        android.widget.HorizontalScrollView quickScroll = new android.widget.HorizontalScrollView(this);
        quickScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout quick = new LinearLayout(this);
        quick.setPadding(dp(10), dp(4), dp(10), 0);
        for (String q : new String[] {"Hello, I'm here", "I've sent the payment", "Payment received", "Please check your bank app",
                "What name is on the account?", "Give me a few minutes"}) {
            TextView chip = text(q, 12, false, UiColors.textPrimary());
            GradientDrawable cb = new GradientDrawable();
            cb.setCornerRadius(dp(16));
            cb.setColor(UiColors.surface());
            cb.setStroke(dp(1), (UiColors.textTertiary() & 0x00FFFFFF) | 0x40000000);
            chip.setBackground(cb);
            chip.setPadding(dp(12), dp(7), dp(12), dp(7));
            chip.setOnClickListener(v -> { input.setText(q); input.setSelection(q.length()); });
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            clp.rightMargin = dp(6);
            quick.addView(chip, clp);
        }
        quickScroll.addView(quick);
        root.addView(quickScroll);

        // Input: a rounded field with a round send button.
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(10), dp(8), dp(10), dp(10));
        input = new EditText(this);
        input.setHint("Write a message");
        input.setTextSize(14);
        input.setTextColor(UiColors.textPrimary());
        input.setHintTextColor(UiColors.textTertiary());
        input.setMaxLines(4);
        GradientDrawable ib = new GradientDrawable();
        ib.setCornerRadius(dp(22));
        ib.setColor(UiColors.surface());
        input.setBackground(ib);
        input.setPadding(dp(16), dp(10), dp(16), dp(10));
        input.setFilters(new android.text.InputFilter[] {new android.text.InputFilter.LengthFilter(P2pChatStore.MAX_TEXT)});
        bar.addView(input, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        android.widget.ImageButton send = new android.widget.ImageButton(this);
        send.setImageResource(R.drawable.ic_pf_send_line);
        send.setColorFilter(getResources().getColor(R.color.colorTextOnAccent));
        send.setContentDescription("Send");
        GradientDrawable sb = new GradientDrawable();
        sb.setShape(GradientDrawable.OVAL);
        sb.setColor(UiColors.accent());
        send.setBackground(sb);
        send.setPadding(dp(10), dp(10), dp(10), dp(10));
        send.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
        send.setOnClickListener(v -> send(send));
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(dp(44), dp(44));
        slp.leftMargin = dp(8);
        bar.addView(send, slp);
        root.addView(bar);

        AppBar.setContent(this, root, name.isEmpty() ? "Trade chat" : name, "Order " + shortId(orderId));
        if (name.isEmpty() && !other.isEmpty()) {
            com.harvana.app.trust.ProfileTrust.resolve(other, card -> runOnUiThread(() -> {
                String n = !card.name.isEmpty() ? card.name : (!card.username.isEmpty() ? "@" + card.username : "");
                if (!n.isEmpty() && getSupportActionBar() != null) getSupportActionBar().setTitle(n);
            }));
        }
        P2pChatStore.ensure(orderId, other, (ok, err) -> runOnUiThread(() -> {
            if (!ok) { Toast.makeText(this, err, Toast.LENGTH_LONG).show(); return; }
            reg = P2pChatStore.listen(orderId, other, messages -> runOnUiThread(() -> show(messages)));
        }));
    }

    @Override protected void onDestroy() { if (reg != null) reg.remove(); super.onDestroy(); }

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
            TextView empty = text("No messages yet. Say hello and agree how you will pay.", 13, false, UiColors.textSecondary());
            empty.setPadding(dp(8), dp(16), dp(8), 0);
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
                TextView sep = text(day, 11, true, UiColors.textTertiary());
                GradientDrawable sbg = new GradientDrawable();
                sbg.setCornerRadius(dp(10));
                sbg.setColor(UiColors.surface());
                sep.setBackground(sbg);
                sep.setPadding(dp(10), dp(3), dp(10), dp(3));
                LinearLayout.LayoutParams sl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                sl.gravity = Gravity.CENTER_HORIZONTAL;
                sl.topMargin = dp(12);
                list.addView(sep, sl);
            }
            boolean mine = m.from.equals(me);
            LinearLayout bubble = new LinearLayout(this);
            bubble.setOrientation(LinearLayout.VERTICAL);
            bubble.setPadding(dp(12), dp(8), dp(12), dp(8));
            GradientDrawable g = new GradientDrawable();
            g.setCornerRadius(dp(14));
            g.setColor(mine ? UiColors.accent() : UiColors.surface());
            if (!mine) g.setStroke(dp(1), (UiColors.textTertiary() & 0x00FFFFFF) | 0x33000000);
            bubble.setBackground(g);
            bubble.addView(text(m.text, 14, false, mine ? getResources().getColor(R.color.colorTextOnAccent) : UiColors.textPrimary()));
            if (!m.image.isEmpty()) {
                // A payment proof: the picture itself, tap to open it full size.
                android.widget.ImageView pic = new android.widget.ImageView(this);
                pic.setAdjustViewBounds(true);
                pic.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
                com.bumptech.glide.Glide.with(this).load(m.image).into(pic);
                final String url = m.image;
                pic.setOnClickListener(v -> startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))));
                LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(dp(200), dp(200));
                plp.topMargin = dp(6);
                bubble.addView(pic, plp);
            }
            bubble.addView(text(fmt.format(new Date(m.at)), 10, false, mine ? getResources().getColor(R.color.colorTextOnAccentMuted) : UiColors.textTertiary()));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.gravity = mine ? Gravity.END : Gravity.START;
            lp.topMargin = dp(6);
            lp.leftMargin = mine ? dp(48) : 0;
            lp.rightMargin = mine ? 0 : dp(48);
            list.addView(bubble, lp);
        }
        scroll.post(() -> scroll.fullScroll(ScrollView.FOCUS_DOWN));
    }

    private TextView text(String s, float sp, boolean bold, int color) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(sp); t.setTextColor(color);
        if (bold) t.setTypeface(null, Typeface.BOLD);
        return t;
    }

    private static String shortId(String s) { return s.length() > 14 ? s.substring(0, 12) + "…" : s; }
    private static String str(String s) { return s == null ? "" : s; }
    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
