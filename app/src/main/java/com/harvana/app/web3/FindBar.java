package com.harvana.app.web3;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.harvana.app.R;
import com.harvana.app.utils.UiColors;

/** Find in page: query, "3/12", previous, next, close. Hidden until {@link #show()}. */
public final class FindBar {

    private final LinearLayout bar;
    private final EditText query;
    private final TextView count;
    private WebView web;

    public FindBar(Context c, WebView first) {
        this.web = first;
        bar = (LinearLayout) android.view.LayoutInflater.from(c).inflate(R.layout.view_find_bar, null, false);
        query = bar.findViewById(R.id.find_bar_query);
        count = bar.findViewById(R.id.find_bar_count);
        query.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int n) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int n) { }
            @Override public void afterTextChanged(Editable s) {
                if (s.length() == 0) { web.clearMatches(); count.setText(""); }
                else web.findAllAsync(s.toString());
            }
        });
        query.setOnEditorActionListener((v, id, ev) -> { web.findNext(true); return true; });
        bar.findViewById(R.id.find_bar_previous).setOnClickListener(v -> web.findNext(false));
        bar.findViewById(R.id.find_bar_next).setOnClickListener(v -> web.findNext(true));
        bar.findViewById(R.id.find_bar_close).setOnClickListener(v -> hide());

        listen();
    }

    private void listen() {
        web.setFindListener((active, total, done) -> {
            if (!done) return;
            count.setText(total == 0 ? "0/0" : (active + 1) + "/" + total);
            count.setTextColor(total == 0 && query.length() > 0 ? UiColors.error() : UiColors.textSecondary());
        });
    }

    /** The page to search (the browser's open tab). */
    public void setWeb(WebView w) {
        if (isShown()) hide();
        web = w;
        listen();
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
}
