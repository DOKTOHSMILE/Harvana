package com.harvana.app.search;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.harvana.app.utils.AppBar;
import com.harvana.app.utils.UiColors;

import java.util.ArrayList;
import java.util.List;

/**
 * One search for the whole app. The chips are the categories; each chip shows only that kind of result,
 * and All shows them together with a heading on each kind.
 */
public class SearchActivity extends AppCompatActivity {
    private EditText query;
    private RecyclerView list;
    private TextView empty;
    private final ResultAdapter adapter = new ResultAdapter();
    private String category = AppSearch.ALL;
    private int generation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        float d = getResources().getDisplayMetrics().density;
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(UiColors.background());

        query = new EditText(this);
        query.setHint("Groups, lessons, people, settings");
        query.setSingleLine(true);
        query.setTextColor(UiColors.textPrimary());
        query.setHintTextColor(UiColors.textTertiary());
        int pad = Math.round(14 * d);
        query.setPadding(pad, pad, pad, pad);
        query.setBackgroundColor(UiColors.surface());
        root.addView(query, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        chips.setPadding(Math.round(8 * d), Math.round(8 * d), Math.round(8 * d), Math.round(4 * d));
        MaterialButton[] buttons = new MaterialButton[AppSearch.CATEGORIES.length];
        for (int i = 0; i < AppSearch.CATEGORIES.length; i++) {
            MaterialButton b = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
            b.setText(AppSearch.CATEGORIES[i]);
            b.setAllCaps(false);
            b.setTextSize(13);
            b.setInsetTop(0);
            b.setInsetBottom(0);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, Math.round(36 * d));
            lp.setMarginEnd(Math.round(6 * d));
            final int at = i;
            b.setOnClickListener(v -> {
                category = AppSearch.CATEGORIES[at];
                paintChips(buttons);
                run();
            });
            buttons[i] = b;
            chips.addView(b, lp);
        }
        paintChips(buttons);
        android.widget.HorizontalScrollView scroll = new android.widget.HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.addView(chips);
        root.addView(scroll);

        empty = new TextView(this);
        empty.setText("Search for a group, a lesson, a person, or something the app does.");
        empty.setTextColor(UiColors.textSecondary());
        empty.setPadding(pad, pad, pad, pad);
        root.addView(empty);

        list = new RecyclerView(this);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);
        root.addView(list, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        AppBar.setContent(this, root, "Search", null);
        query.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { run(); }
        });
        query.requestFocus();
        run();
    }

    private void paintChips(MaterialButton[] buttons) {
        for (int i = 0; i < buttons.length; i++) {
            boolean on = AppSearch.CATEGORIES[i].equals(category);
            buttons[i].setBackgroundTintList(android.content.res.ColorStateList.valueOf(on ? UiColors.accent() : UiColors.surface()));
            buttons[i].setTextColor(on ? UiColors.onAccent() : UiColors.textPrimary());
            buttons[i].setStrokeColor(android.content.res.ColorStateList.valueOf(on ? UiColors.accent() : UiColors.textTertiary()));
        }
    }

    private void run() {
        final int ticket = ++generation;
        String text = query.getText() == null ? "" : query.getText().toString();
        AppSearch.query(this, text, category, hits -> runOnUiThread(() -> {
            if (ticket != generation || isFinishing()) return;
            adapter.set(hits);
            boolean none = hits.isEmpty();
            empty.setVisibility(none ? View.VISIBLE : View.GONE);
            empty.setText(text.trim().isEmpty()
                    ? "Search for a group, a lesson, a person, or something the app does."
                    : "Nothing matches that in " + category.toLowerCase(java.util.Locale.US) + ".");
            list.setVisibility(none ? View.GONE : View.VISIBLE);
        }));
    }

    private final class ResultAdapter extends RecyclerView.Adapter<ResultAdapter.Holder> {
        private final List<Row> rows = new ArrayList<>();

        void set(List<AppSearch.Hit> hits) {
            rows.clear();
            String last = "";
            for (AppSearch.Hit h : hits) {
                if (AppSearch.ALL.equals(category) && !h.category.equals(last)) {
                    rows.add(Row.head(h.category));
                    last = h.category;
                }
                rows.add(Row.hit(h));
            }
            notifyDataSetChanged();
        }

        @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView t = new TextView(SearchActivity.this);
            int pad = Math.round(16 * getResources().getDisplayMetrics().density);
            t.setPadding(pad, viewType == 0 ? Math.round(14 * getResources().getDisplayMetrics().density) : Math.round(10 * getResources().getDisplayMetrics().density), pad, Math.round(10 * getResources().getDisplayMetrics().density));
            t.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return new Holder(t);
        }

        @Override public void onBindViewHolder(@NonNull Holder h, int position) {
            Row row = rows.get(position);
            TextView t = (TextView) h.itemView;
            if (row.head) {
                t.setText(row.title);
                t.setTextSize(12);
                t.setTypeface(null, android.graphics.Typeface.BOLD);
                t.setTextColor(UiColors.textSecondary());
                t.setOnClickListener(null);
                t.setClickable(false);
                return;
            }
            t.setText(row.hit.subtitle == null || row.hit.subtitle.isEmpty() ? row.hit.title : row.hit.title + "\n" + row.hit.subtitle);
            t.setTextSize(15);
            t.setTypeface(null, android.graphics.Typeface.NORMAL);
            t.setTextColor(UiColors.textPrimary());
            t.setClickable(true);
            t.setOnClickListener(v -> {
                if (row.hit.open == null) return;
                row.hit.open.setFlags(row.hit.open.getFlags() & ~android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(row.hit.open);
            });
        }

        @Override public int getItemViewType(int position) { return rows.get(position).head ? 0 : 1; }
        @Override public int getItemCount() { return rows.size(); }

        final class Holder extends RecyclerView.ViewHolder { Holder(View v) { super(v); } }
    }

    private static final class Row {
        final boolean head;
        final String title;
        final AppSearch.Hit hit;
        private Row(boolean head, String title, AppSearch.Hit hit) { this.head = head; this.title = title; this.hit = hit; }
        static Row head(String title) { return new Row(true, title, null); }
        static Row hit(AppSearch.Hit h) { return new Row(false, h.title, h); }
    }
}
