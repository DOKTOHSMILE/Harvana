package com.harvana.app;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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
        com.harvana.app.utils.AppBar.setContent(this, R.layout.activity_pick_asset, "Choose asset", null);
        EditText search = findViewById(R.id.et_pick_asset_search);
        empty = findViewById(R.id.tv_pick_asset_empty);
        list = findViewById(R.id.rv_pick_asset);
        list.setAdapter(new Adapter());

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

    private final class Adapter extends RecyclerView.Adapter<Adapter.Holder> {
        @NonNull @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View row = getLayoutInflater().inflate(R.layout.item_pick_asset, parent, false);
            return new Holder(row, row.findViewById(R.id.iv_pick_asset_icon), row.findViewById(R.id.tv_pick_asset_name),
                    row.findViewById(R.id.tv_pick_asset_network), row.findViewById(R.id.tv_pick_asset_ticker));
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
