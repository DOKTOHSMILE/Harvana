package com.harvana.app;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.harvana.app.wallet.CoinCatalog;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Full-screen coin list with search. Send and Receive open this instead of a dropdown. */
public class PickAssetActivity extends AppCompatActivity {

    private final List<CoinCatalog.Coin> shown = new ArrayList<>();
    private String current = "";
    private TextView empty;
    private RecyclerView list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String extra = getIntent().getStringExtra(CoinCatalog.EXTRA);
        current = extra == null ? "" : extra;
        setTitle("Choose asset");
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        float d = getResources().getDisplayMetrics().density;
        int pad = Math.round(16 * d);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(com.harvana.app.utils.UiColors.background());
        root.setPadding(pad, pad, pad, 0);

        EditText search = new EditText(this);
        search.setHint("Search name, ticker or network");
        search.setSingleLine(true);
        search.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        search.setTextColor(com.harvana.app.utils.UiColors.textPrimary());
        search.setHintTextColor(com.harvana.app.utils.UiColors.textTertiary());
        search.setBackground(field());
        search.setPadding(pad, Math.round(12 * d), pad, Math.round(12 * d));
        root.addView(search, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        empty = new TextView(this);
        empty.setText("No coin matches that.");
        empty.setTextColor(com.harvana.app.utils.UiColors.textSecondary());
        empty.setPadding(pad / 2, pad, pad / 2, pad);
        empty.setVisibility(View.GONE);
        root.addView(empty);

        list = new RecyclerView(this);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(new Adapter());
        root.addView(list, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { show(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });
        show("");
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void show(String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.US);
        shown.clear();
        for (CoinCatalog.Coin c : CoinCatalog.all()) {
            if (q.isEmpty()
                    || c.name.toLowerCase(Locale.US).contains(q)
                    || c.ticker.toLowerCase(Locale.US).contains(q)
                    || c.liveNetwork().toLowerCase(Locale.US).contains(q)
                    || c.key.toLowerCase(Locale.US).contains(q)) shown.add(c);
        }
        empty.setVisibility(shown.isEmpty() ? View.VISIBLE : View.GONE);
        if (list.getAdapter() != null) list.getAdapter().notifyDataSetChanged();
    }

    private android.graphics.drawable.Drawable field() {
        android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
        g.setColor(com.harvana.app.utils.UiColors.surface());
        g.setCornerRadius(14 * getResources().getDisplayMetrics().density);
        g.setStroke(Math.round(getResources().getDisplayMetrics().density),
                androidx.core.content.ContextCompat.getColor(this, R.color.colorCardStroke));
        return g;
    }

    private final class Adapter extends RecyclerView.Adapter<Adapter.Holder> {
        @NonNull @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            float d = getResources().getDisplayMetrics().density;
            int pad = Math.round(12 * d);
            LinearLayout row = new LinearLayout(PickAssetActivity.this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(pad, pad, pad, pad);
            row.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            ImageView icon = new ImageView(PickAssetActivity.this);
            int s = Math.round(40 * d);
            row.addView(icon, new LinearLayout.LayoutParams(s, s));

            LinearLayout text = new LinearLayout(PickAssetActivity.this);
            text.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            tp.leftMargin = pad;
            row.addView(text, tp);

            TextView name = new TextView(PickAssetActivity.this);
            name.setTextSize(16);
            name.setTypeface(null, android.graphics.Typeface.BOLD);
            name.setTextColor(com.harvana.app.utils.UiColors.textPrimary());
            text.addView(name);

            TextView net = new TextView(PickAssetActivity.this);
            net.setTextSize(12);
            net.setTextColor(com.harvana.app.utils.UiColors.textSecondary());
            text.addView(net);

            TextView tick = new TextView(PickAssetActivity.this);
            tick.setTextSize(13);
            tick.setTypeface(null, android.graphics.Typeface.BOLD);
            tick.setTextColor(com.harvana.app.utils.UiColors.textSecondary());
            row.addView(tick);
            return new Holder(row, icon, name, net, tick);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            CoinCatalog.Coin c = shown.get(position);
            boolean on = c.key.equals(current);
            h.icon.setImageResource(c.icon);
            h.name.setText(c.name);
            h.net.setText(c.liveNetwork());
            h.tick.setText(c.ticker);
            h.tick.setTextColor(on ? com.harvana.app.utils.UiColors.accent() : com.harvana.app.utils.UiColors.textSecondary());
            h.row.setOnClickListener(v -> {
                setResult(RESULT_OK, getIntent().putExtra(CoinCatalog.EXTRA, c.key));
                finish();
            });
        }

        @Override public int getItemCount() { return shown.size(); }

        final class Holder extends RecyclerView.ViewHolder {
            final ImageView icon;
            final TextView name, net, tick;
            final View row;
            Holder(View row, ImageView icon, TextView name, TextView net, TextView tick) {
                super(row);
                this.row = row;
                this.icon = icon;
                this.name = name;
                this.net = net;
                this.tick = tick;
            }
        }
    }
}
