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

    /** Each row is a CoinCatalog.Coin, or a stable asset's code ("HUSD", "HNGN", …) when the caller asked for them. */
    private final List<Object> shown = new ArrayList<>();
    /** EXTRA_STABLE: Send lists Harvana's stable coins too (only those the network has on). */
    public static final String EXTRA_STABLE = "withStable";
    /** EXTRA_ALL_STABLE: Receive lists every Harvana stable coin (they all arrive on the Harvana address). */
    public static final String EXTRA_ALL_STABLE = "allStable";
    private final List<String> stable = new ArrayList<>();
    private String lastQuery = "";
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
        if (getIntent().getBooleanExtra(EXTRA_ALL_STABLE, false)) stable.addAll(java.util.Arrays.asList(com.harvana.app.husd.Stable.CODES));
        show("");
        if (getIntent().getBooleanExtra(EXTRA_STABLE, false)) {
            com.harvana.app.p2p.P2pAsset.loadActive(() -> runOnUiThread(() -> {
                stable.clear();
                for (String c : com.harvana.app.p2p.P2pAsset.available()) if (com.harvana.app.husd.Stable.isStable(c)) stable.add(c);
                show(lastQuery);
            }));
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void show(String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.US);
        lastQuery = q;
        shown.clear();
        boolean stableAdded = false;
        for (CoinCatalog.Coin c : CoinCatalog.all()) {
            if (q.isEmpty()
                    || c.name.toLowerCase(Locale.US).contains(q)
                    || c.ticker.toLowerCase(Locale.US).contains(q)
                    || c.liveNetwork().toLowerCase(Locale.US).contains(q)
                    || c.key.toLowerCase(Locale.US).contains(q)) shown.add(c);
            // Harvana's stable coins sit right under HARV.
            if (!stableAdded && c.nativeHarv) { stableAdded = true; addStable(q); }
        }
        if (!stableAdded) addStable(q);
        empty.setVisibility(shown.isEmpty() ? View.VISIBLE : View.GONE);
        if (list.getAdapter() != null) list.getAdapter().notifyDataSetChanged();
    }

    private void addStable(String q) {
        for (String code : stable) {
            String sym = com.harvana.app.husd.Stable.symbol(code), name = com.harvana.app.husd.Stable.name(code);
            if (q.isEmpty() || sym.toLowerCase(Locale.US).contains(q) || name.toLowerCase(Locale.US).contains(q)
                    || "stable harvana".contains(q)) shown.add(code);
        }
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
            Object item = shown.get(position);
            if (item instanceof String) {
                String code = (String) item;
                boolean on = code.equals(current);
                h.icon.setImageDrawable(com.harvana.app.husd.StableIcon.drawable(PickAssetActivity.this, code));
                h.name.setText(com.harvana.app.husd.Stable.symbol(code) + " · " + com.harvana.app.husd.Stable.name(code));
                h.net.setText("Harvana · stable coin");
                h.tick.setText(com.harvana.app.husd.Stable.symbol(code));
                h.tick.setTextColor(on ? com.harvana.app.utils.UiColors.accent() : com.harvana.app.utils.UiColors.textSecondary());
                h.row.setOnClickListener(v -> {
                    setResult(RESULT_OK, getIntent().putExtra(CoinCatalog.EXTRA, code));
                    finish();
                });
                return;
            }
            CoinCatalog.Coin c = (CoinCatalog.Coin) item;
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
