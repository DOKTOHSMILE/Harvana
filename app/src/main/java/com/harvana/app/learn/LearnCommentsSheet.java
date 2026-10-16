package com.harvana.app.learn;

import android.app.Activity;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.harvana.app.R;
import com.harvana.app.group.MemberNameResolver;

import java.util.List;

/** The comments on a post, newest first, with a box to add one. Built in code so it needs no layout file. */
public final class LearnCommentsSheet {
    private LearnCommentsSheet() {}

    public static void show(Activity host, String contentId, Runnable onPosted) {
        BottomSheetDialog sheet = new com.harvana.app.ui.AppSheet(host);
        LinearLayout root = new LinearLayout(host);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(host, 18);
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(ContextCompat.getColor(host, R.color.colorSurface));

        TextView title = text(host, "Comments", 17, true, R.color.colorTextPrimary);
        root.addView(title);

        LinearLayout list = new LinearLayout(host);
        list.setOrientation(LinearLayout.VERTICAL);
        ScrollView scroll = new ScrollView(host);
        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, dp(host, 280)));

        LinearLayout inputRow = new LinearLayout(host);
        inputRow.setGravity(Gravity.CENTER_VERTICAL);
        EditText input = new com.google.android.material.textfield.TextInputEditText(host);
        input.setHint("Add a comment");
        input.setHintTextColor(ContextCompat.getColor(host, R.color.colorTextMuted));
        input.setTextColor(ContextCompat.getColor(host, R.color.colorTextPrimary));
        input.setMaxLines(4);
        input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(LearnSocial.MAX_COMMENT_CHARS)});
        MaterialButton send = new MaterialButton(host);
        send.setText("Post");
        send.setAllCaps(false);
        inputRow.addView(input, new LinearLayout.LayoutParams(0, -2, 1f));
        inputRow.addView(send);
        root.addView(inputRow);

        Runnable load = () -> LearnSocial.loadComments(contentId, 50, comments -> host.runOnUiThread(() -> fill(host, list, comments)));
        load.run();

        send.setOnClickListener(v -> {
            send.setEnabled(false);
            LearnSocial.addComment(contentId, input.getText().toString(), (ok, error) -> host.runOnUiThread(() -> {
                send.setEnabled(true);
                if (!ok) { Toast.makeText(host, error, Toast.LENGTH_SHORT).show(); return; }
                input.setText("");
                load.run();
                if (onPosted != null) onPosted.run();
            }));
        });

        sheet.setContentView(root);
        sheet.show();
    }

    /** The same comments, drawn straight under the post in the reader: a header with the count, the box, then the list. */
    public static void inline(Activity host, LinearLayout box, String contentId, Runnable onPosted) {
        if (box == null || contentId == null || contentId.isEmpty()) return;
        box.removeAllViews();
        TextView title = text(host, "Comments", 17, true, R.color.colorTextPrimary);
        title.setPadding(0, dp(host, 8), 0, dp(host, 8));
        box.addView(title);

        LinearLayout inputRow = new LinearLayout(host);
        inputRow.setGravity(Gravity.CENTER_VERTICAL);
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setCornerRadius(dp(host, 22));
        bg.setColor(ContextCompat.getColor(host, R.color.colorSurfaceAlt));
        inputRow.setBackground(bg);
        inputRow.setPadding(dp(host, 14), dp(host, 2), dp(host, 4), dp(host, 2));
        EditText input = new EditText(host);
        input.setBackground(null);
        input.setHint("Add a comment");
        input.setTextSize(14);
        input.setHintTextColor(ContextCompat.getColor(host, R.color.colorTextMuted));
        input.setTextColor(ContextCompat.getColor(host, R.color.colorTextPrimary));
        input.setMaxLines(4);
        input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(LearnSocial.MAX_COMMENT_CHARS)});
        MaterialButton send = new MaterialButton(host, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        send.setText("Post");
        send.setAllCaps(false);
        inputRow.addView(input, new LinearLayout.LayoutParams(0, -2, 1f));
        inputRow.addView(send);
        box.addView(inputRow);

        LinearLayout list = new LinearLayout(host);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(0, dp(host, 6), 0, 0);
        box.addView(list);

        Runnable load = () -> LearnSocial.loadComments(contentId, 50, comments -> host.runOnUiThread(() -> {
            title.setText(comments.isEmpty() ? "Comments" : "Comments (" + comments.size() + ")");
            fill(host, list, comments);
        }));
        load.run();
        send.setOnClickListener(v -> {
            send.setEnabled(false);
            LearnSocial.addComment(contentId, input.getText().toString(), (ok, error) -> host.runOnUiThread(() -> {
                send.setEnabled(true);
                if (!ok) { Toast.makeText(host, error, Toast.LENGTH_SHORT).show(); return; }
                input.setText("");
                load.run();
                if (onPosted != null) onPosted.run();
            }));
        });
    }

    private static void fill(Activity host, LinearLayout list, List<LearnSocial.Comment> comments) {
        list.removeAllViews();
        if (comments.isEmpty()) {
            list.addView(text(host, "No comments yet. Be the first.", 13, false, R.color.colorTextTertiary));
            return;
        }
        long now = System.currentTimeMillis();
        for (LearnSocial.Comment c : comments) {
            LinearLayout row = new LinearLayout(host);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(0, dp(host, 10), 0, dp(host, 6));
            TextView who = text(host, "Member", 13, true, R.color.colorTextPrimary);
            MemberNameResolver.resolveInto(host, c.uid, "Member", who);
            TextView when = text(host, LearnRules.timeAgo(c.at, now), 11, false, R.color.colorTextTertiary);
            TextView body = text(host, c.text, 14, false, R.color.colorTextSecondary);
            row.addView(who);
            row.addView(when);
            row.addView(body);
            list.addView(row);
        }
    }

    private static TextView text(Activity host, String s, int sp, boolean bold, int colorRes) {
        TextView t = new TextView(host);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(ContextCompat.getColor(host, colorRes));
        if (bold) t.setTypeface(null, Typeface.BOLD);
        return t;
    }

    private static int dp(Activity host, int v) { return (int) (v * host.getResources().getDisplayMetrics().density); }
}
