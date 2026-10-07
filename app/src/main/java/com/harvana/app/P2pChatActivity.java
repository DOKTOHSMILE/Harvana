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

        TextView notice = text("Keep the trade inside Harvana. Pay only into the escrow shown on the trade, never to a link or account someone sends you here. "
                + "Messages are kept for 30 days after the last one, in case the trade is disputed.", 12, false, UiColors.textSecondary());
        notice.setPadding(dp(16), dp(10), dp(16), dp(10));
        root.addView(notice);

        scroll = new ScrollView(this);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(12), dp(4), dp(12), dp(8));
        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(10), dp(6), dp(10), dp(6));
        input = new com.google.android.material.textfield.TextInputEditText(this);
        input.setHint("Message");
        input.setTextColor(UiColors.textPrimary());
        input.setHintTextColor(UiColors.textTertiary());
        input.setMaxLines(4);
        input.setFilters(new android.text.InputFilter[] {new android.text.InputFilter.LengthFilter(P2pChatStore.MAX_TEXT)});
        bar.addView(input, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        MaterialButton send = new MaterialButton(this);
        send.setText("Send");
        send.setAllCaps(false);
        send.setOnClickListener(v -> send(send));
        bar.addView(send);
        root.addView(bar);

        AppBar.setContent(this, root, name.isEmpty() ? "Trade chat" : name, "Order " + shortId(orderId));
        P2pChatStore.ensure(orderId, other, (ok, err) -> runOnUiThread(() -> {
            if (!ok) { Toast.makeText(this, err, Toast.LENGTH_LONG).show(); return; }
            reg = P2pChatStore.listen(orderId, other, messages -> runOnUiThread(() -> show(messages)));
        }));
    }

    @Override protected void onDestroy() { if (reg != null) reg.remove(); super.onDestroy(); }

    private void send(MaterialButton button) {
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
        SimpleDateFormat fmt = new SimpleDateFormat("d MMM, HH:mm", Locale.US);
        for (P2pChatStore.Message m : messages) {
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
