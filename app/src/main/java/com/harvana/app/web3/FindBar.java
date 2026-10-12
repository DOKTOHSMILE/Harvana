package com.harvana.app.web3;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.harvana.app.R;
import com.harvana.app.utils.UiColors;

/** Find in page: query, "3/12", previous, next, close. Hidden until {@link #show()}. */
public final class FindBar {

    private final LinearLayout bar;
    private final EditText query;
    private final TextView count;
    private final WebView web;

    public FindBar(Context c, WebView web) {
        this.web = web;
        bar = new LinearLayout(c);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackgroundColor(UiColors.surface());
        int p = dp(c, 6);
        bar.setPadding(dp(c, 12), p, p, p);
        bar.setVisibility(View.GONE);

        query = new EditText(c);
        query.setSingleLine(true);
        query.setHint("Find in page");
        query.setTextSize(14);
        query.setBackground(null);
        query.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        query.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int n) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int n) { }
            @Override public void afterTextChanged(Editable s) {
                if (s.length() == 0) { web.clearMatches(); count.setText(""); }
                else web.findAllAsync(s.toString());
            }
        });
        query.setOnEditorActionListener((v, id, ev) -> { web.findNext(true); return true; });
        bar.addView(query, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        count = new TextView(c);
        count.setTextSize(13);
        count.setTextColor(UiColors.textSecondary());
        count.setPadding(dp(c, 8), 0, dp(c, 8), 0);
        bar.addView(count);

        bar.addView(button(c, R.drawable.ic_br_up, "Previous", () -> web.findNext(false)), new LinearLayout.LayoutParams(dp(c, 40), dp(c, 40)));
        bar.addView(button(c, R.drawable.ic_br_down, "Next", () -> web.findNext(true)), new LinearLayout.LayoutParams(dp(c, 40), dp(c, 40)));
        bar.addView(button(c, R.drawable.ic_dismiss_circle_line, "Close", this::hide), new LinearLayout.LayoutParams(dp(c, 40), dp(c, 40)));

        web.setFindListener((active, total, done) -> {
            if (!done) return;
            count.setText(total == 0 ? "0/0" : (active + 1) + "/" + total);
            count.setTextColor(total == 0 && query.length() > 0 ? UiColors.error() : UiColors.textSecondary());
        });
    }

    public View view() { return bar; }

    public boolean isShown() { return bar.getVisibility() == View.VISIBLE; }

    public void show() {
        bar.setVisibility(View.VISIBLE);
        query.requestFocus();
        InputMethodManager imm = (InputMethodManager) query.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) query.post(() -> imm.showSoftInput(query, InputMethodManager.SHOW_IMPLICIT));
        if (query.length() > 0) web.findAllAsync(query.getText().toString());
    }

    public void hide() {
        web.clearMatches();
        count.setText("");
        bar.setVisibility(View.GONE);
        InputMethodManager imm = (InputMethodManager) query.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(query.getWindowToken(), 0);
    }

    private static ImageView button(Context c, int icon, String label, Runnable r) {
        ImageView iv = new ImageView(c);
        iv.setImageResource(icon);
        iv.setContentDescription(label);
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        int pad = Math.round(8 * c.getResources().getDisplayMetrics().density);
        iv.setPadding(pad, pad, pad, pad);
        iv.setColorFilter(UiColors.textPrimary());
        iv.setOnClickListener(v -> r.run());
        return iv;
    }

    private static int dp(Context c, int v) { return Math.round(v * c.getResources().getDisplayMetrics().density); }
}
