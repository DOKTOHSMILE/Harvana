package com.harvana.app.wallet;

import android.content.Context;
import android.content.Intent;
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

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.harvana.app.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * "Add coins" as its own screen: every coin, Harvana stable coin, and chain or token the person added, each with a
 * switch to show or hide it on Assets, and a filter on top. A recycled list, so hundreds of tokens scroll smoothly.
 * Long-press an added chain or token for its chain's tokens, edit and remove.
 */
public class ManageAssetsActivity extends AppCompatActivity {

    /** Runs once when the screen closes after something changed (the Assets list redraws). */
    private static Runnable onClosed;
    private boolean changed;

    public static void open(Context c, Runnable whenChanged) {
        onClosed = whenChanged;
        c.startActivity(new Intent(c, ManageAssetsActivity.class));
    }

    private static final int HEADER = 0, ROW = 1;

    private static final class Item {
        int type;
        String title, sub = "", key, iconUrl = "";
        int icon;
        String kind;                       // COIN, STABLE, ADDED, FOUND (a search result), MANUAL (type a token in)
        CustomAssets.Asset asset;
    }

    private final List<Item> all = new ArrayList<>();
    private final List<Item> shown = new ArrayList<>();
    private Adapter adapter;
    private String query = "";
    /** A contract address in Search: every chain is scanned for it and the tokens found head the list. */
    private final List<Item> found = new ArrayList<>();
    private String scanned = "";
    private final android.os.Handler debounce = new android.os.Handler(android.os.Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        com.harvana.app.utils.AppBar.setContent(this, R.layout.activity_manage_assets, "Add coins", null);
        EditText filter = findViewById(R.id.et_manage_assets_search);
        findViewById(R.id.btn_manage_assets_add_chain).setOnClickListener(v -> CustomAssets.editChain(this, null, this::reload));
        RecyclerView list = findViewById(R.id.rv_manage_assets);
        list.setHasFixedSize(true);
        list.setItemViewCacheSize(20);
        adapter = new Adapter();
        list.setAdapter(adapter);

        filter.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int c, int d) { }
            @Override public void onTextChanged(CharSequence s, int a, int c, int d) { }
            @Override public void afterTextChanged(Editable s) {
                String raw = s.toString().trim();
                query = raw.toLowerCase(Locale.US);
                debounce.removeCallbacksAndMessages(null);
                if (!TokenScan.looksLikeContract(raw)) { found.clear(); scanned = ""; }
                else if (!raw.equals(scanned)) debounce.postDelayed(() -> scan(raw), 500);
                applyFilter();
            }
        });
        reload();
    }

    @Override public boolean onSupportNavigateUp() { finish(); return true; }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        debounce.removeCallbacksAndMessages(null);
        Runnable r = onClosed;
        onClosed = null;
        if (changed && r != null) r.run();
    }

    private void reload() {
        changed = true;
        all.clear();
        all.add(header("Coins"));
        for (CoinCatalog.Coin coin : CoinCatalog.others()) {
            Item i = row("COIN", coin.key, coin.name, coin.ticker + " · " + coin.liveNetwork());
            i.icon = coin.icon;
            all.add(i);
        }
        all.add(header("Harvana stable coins"));
        for (String code : com.harvana.app.husd.Stable.CODES) {
            Item i = row("STABLE", code, com.harvana.app.husd.Stable.symbol(code), com.harvana.app.husd.Stable.name(code));
            all.add(i);
        }
        List<CustomAssets.Asset> added = CustomAssets.all(this);
        if (!added.isEmpty()) all.add(header("Added"));
        for (CustomAssets.Asset a : added) {
            Item i = row("ADDED", a.key(), a.token() ? a.symbol + " · " + a.name : a.chainName, a.token() ? a.chainName : a.family + " · " + a.symbol);
            i.icon = R.drawable.ic_coin_generic;
            i.iconUrl = a.icon;
            i.asset = a;
            all.add(i);
        }
        applyFilter();
    }

    /** Scans every chain for the address in Search; the tokens found show first, each with its toggle. */
    private void scan(String address) {
        scanned = address;
        found.clear();
        found.add(header("Searching"));
        applyFilter();
        TokenScan.search(this, address, tokens -> {
            if (isFinishing() || !address.equals(scanned)) return;
            found.clear();
            found.add(header(tokens.isEmpty() ? "Found · 0" : "Found · " + tokens.size()));
            for (CustomAssets.Asset a : tokens) {
                Item i = row("FOUND", a.key(), a.symbol + " · " + a.name, a.chainName);
                i.icon = R.drawable.ic_coin_generic;
                i.iconUrl = a.icon == null ? "" : a.icon;
                i.asset = a;
                found.add(i);
            }
            Item manual = row("MANUAL", "", "Add on a chain", address);
            manual.icon = R.drawable.ic_coin_generic;
            found.add(manual);
            applyFilter();
        });
    }

    private void applyFilter() {
        shown.clear();
        if (!found.isEmpty()) shown.addAll(found);
        Item pendingHeader = null;
        for (Item i : all) {
            if (i.type == HEADER) { pendingHeader = i; continue; }
            if (!query.isEmpty() && !(i.title + " " + i.sub + " " + (i.asset == null ? "" : i.asset.contract)).toLowerCase(Locale.US).contains(query)) continue;
            if (pendingHeader != null) { shown.add(pendingHeader); pendingHeader = null; }
            shown.add(i);
        }
        adapter.notifyDataSetChanged();
    }

    private boolean isOn(Item i) {
        if ("FOUND".equals(i.kind)) { CustomAssets.Asset a = CustomAssets.find(i.key); return a != null && !a.hidden; }
        if ("ADDED".equals(i.kind)) return !i.asset.hidden;
        return ChainAssetsSection.shown(this, i.key);
    }

    private void setOn(Item i, boolean on) {
        changed = true;
        if ("FOUND".equals(i.kind)) {
            CustomAssets.Asset have = CustomAssets.find(i.key);
            CustomAssets.Asset a = have != null ? have : i.asset.copy();
            a.hidden = !on;
            CustomAssets.add(this, a);
            reload();
            return;
        }
        if ("ADDED".equals(i.kind)) { i.asset.hidden = !on; CustomAssets.add(this, i.asset); }
        else ChainAssetsSection.setShown(this, i.key, on);
    }

    // ── list ──────────────────────────────────────────────────────────────

    private final class Adapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        @Override public int getItemViewType(int p) { return shown.get(p).type; }
        @Override public int getItemCount() { return shown.size(); }

        @NonNull @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
            if (type == HEADER) {
                return new RecyclerView.ViewHolder(getLayoutInflater().inflate(R.layout.item_manage_assets_header, parent, false)) { };
            }
            return new RowHolder(getLayoutInflater().inflate(R.layout.item_manage_assets_row, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int p) {
            Item i = shown.get(p);
            if (i.type == HEADER) { ((TextView) h.itemView).setText(i.title); return; }
            RowHolder r = (RowHolder) h;
            r.title.setText(i.title);
            r.sub.setText(i.sub);
            if ("STABLE".equals(i.kind)) r.icon.setImageDrawable(com.harvana.app.husd.StableIcon.drawable(ManageAssetsActivity.this, i.key));
            else if (!i.iconUrl.isEmpty()) CustomAssets.icon(ManageAssetsActivity.this, r.icon, i.iconUrl, i.title);
            else r.icon.setImageResource(i.icon);
            r.toggle.setOnCheckedChangeListener(null);
            if ("MANUAL".equals(i.kind)) {
                r.toggle.setVisibility(View.GONE);
                r.itemView.setOnClickListener(v -> CustomAssets.addToken(ManageAssetsActivity.this, null, i.sub, ManageAssetsActivity.this::reload));
                r.itemView.setOnLongClickListener(null);
                int next = p + 1;
                r.divider.setVisibility(next < shown.size() && shown.get(next).type == ROW ? View.VISIBLE : View.GONE);
                return;
            }
            r.toggle.setVisibility(View.VISIBLE);
            r.toggle.setChecked(isOn(i));
            r.toggle.setOnCheckedChangeListener((b, on) -> setOn(i, on));
            r.itemView.setOnClickListener(v -> r.toggle.toggle());
            r.itemView.setOnLongClickListener("ADDED".equals(i.kind) ? v -> {
                CustomAssets.Asset chain = i.asset.token() && CustomAssets.find(i.asset.chainKey) != null ? CustomAssets.find(i.asset.chainKey) : i.asset;
                CustomAssets.manage(ManageAssetsActivity.this, chain, ManageAssetsActivity.this::reload);
                return true;
            } : null);
            int next = p + 1;
            r.divider.setVisibility(next < shown.size() && shown.get(next).type == ROW ? View.VISIBLE : View.GONE);
        }
    }

    private final class RowHolder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView title, sub;
        final SwitchMaterial toggle;
        final View divider;

        RowHolder(View v) {
            super(v);
            icon = v.findViewById(R.id.iv_manage_asset_icon);
            title = v.findViewById(R.id.tv_manage_asset_title);
            sub = v.findViewById(R.id.tv_manage_asset_sub);
            toggle = v.findViewById(R.id.sw_manage_asset);
            divider = v.findViewById(R.id.manage_asset_divider);
        }
    }

    private Item header(String title) { Item i = new Item(); i.type = HEADER; i.title = title; return i; }

    private Item row(String kind, String key, String title, String sub) {
        Item i = new Item();
        i.type = ROW;
        i.kind = kind;
        i.key = key;
        i.title = title;
        i.sub = sub;
        return i;
    }

}
