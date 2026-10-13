package com.harvana.app.group;

import android.app.Activity;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.harvana.app.R;

/** One card of Actions & votes, from res/layout/item_action_card.xml: each setter shows its part; parts not set stay hidden. */
final class ActionCard {
    final View view;
    private final Activity a;

    ActionCard(Activity a, ViewGroup parent) {
        this.a = a;
        view = a.getLayoutInflater().inflate(R.layout.item_action_card, parent, false);
    }

    ActionCard title(String s) { ((TextView) view.findViewById(R.id.tv_action_title)).setText(s); return this; }

    ActionCard subtitle(String s) { return show(R.id.tv_action_subtitle, s); }

    ActionCard chip(String label, int colorRes) {
        TextView t = view.findViewById(R.id.tv_action_chip);
        t.setText(label);
        t.setTextColor(ContextCompat.getColor(a, colorRes));
        t.setVisibility(View.VISIBLE);
        return this;
    }

    TextView avatar() {
        TextView t = view.findViewById(R.id.tv_action_avatar);
        t.setVisibility(View.VISIBLE);
        return t;
    }

    TextView titleView() { return view.findViewById(R.id.tv_action_title); }

    ActionCard amount(String s) { return show(R.id.tv_action_amount, s); }

    ActionCard quote(String s) { return show(R.id.tv_action_quote, s == null || s.isEmpty() ? "" : "“" + s + "”"); }

    ActionCard meta(String s) { return show(R.id.tv_action_meta, s); }

    /** The vote bar: {@code percent} yes, in the accent colour while open and green once decided, with its line under it. */
    ActionCard bar(int percent, boolean open, String line) {
        LinearProgressIndicator bar = view.findViewById(R.id.action_vote_bar);
        bar.setProgress(Math.max(0, Math.min(100, percent)));
        bar.setIndicatorColor(ContextCompat.getColor(a, open ? R.color.colorAccent : R.color.colorSuccess));
        bar.setTrackColor(ContextCompat.getColor(a, R.color.colorCardStroke));
        bar.setVisibility(View.VISIBLE);
        return show(R.id.tv_action_vote_line, line);
    }

    /** A line under the card's figures; {@code colorRes} 0 for the usual colour. */
    ActionCard note(String s, int colorRes, boolean bold) {
        if (s == null || s.isEmpty()) return this;
        LinearLayout box = view.findViewById(R.id.action_notes);
        TextView t = (TextView) a.getLayoutInflater().inflate(R.layout.item_action_note, box, false);
        t.setText(s);
        if (colorRes != 0) t.setTextColor(ContextCompat.getColor(a, colorRes));
        if (bold) t.setTypeface(null, Typeface.BOLD);
        box.addView(t);
        return this;
    }

    ActionCard note(String s) { return note(s, 0, false); }

    /** Yes / No (or Accept / Reject) side by side. */
    ActionCard twoButtons(String yes, View.OnClickListener onYes, String no, View.OnClickListener onNo) {
        view.findViewById(R.id.action_two_buttons).setVisibility(View.VISIBLE);
        MaterialButton y = view.findViewById(R.id.btn_action_yes), n = view.findViewById(R.id.btn_action_no);
        y.setText(yes);
        y.setOnClickListener(onYes);
        n.setText(no);
        n.setOnClickListener(onNo);
        return this;
    }

    /** One full-width button: filled when {@code primary}, outlined otherwise. */
    MaterialButton button(String label, boolean primary, View.OnClickListener l) {
        MaterialButton b = view.findViewById(primary ? R.id.btn_action_main : R.id.btn_action_secondary);
        b.setText(label);
        b.setOnClickListener(l);
        b.setVisibility(View.VISIBLE);
        return b;
    }

    private ActionCard show(int id, String s) {
        TextView t = view.findViewById(id);
        t.setText(s == null ? "" : s);
        t.setVisibility(s == null || s.isEmpty() ? View.GONE : View.VISIBLE);
        return this;
    }
}
