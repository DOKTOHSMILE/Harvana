package com.harvana.app.wallet;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.harvana.app.R;
import com.harvana.app.utils.UiColors;

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
        String kind;                       // COIN, STABLE, ADDED
        CustomAssets.Asset asset;
    }

    private final List<Item> all = new ArrayList<>();
    private final List<Item> shown = new ArrayList<>();
    private Adapter adapter;
    private String query = "";

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setBackgroundColor(UiColors.background());
        int pad = dp(16);

        EditText filter = new com.google.android.material.textfield.TextInputEditText(this);
        filter.setHint("Search");
        filter.setSingleLine(true);
        filter.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.icon_search_24, 0, 0, 0);
        filter.setCompoundDrawablePadding(dp(8));
        LinearLayout.LayoutParams flp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        flp.setMargins(pad, dp(8), pad, 0);
        body.addView(filter, flp);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.VERTICAL);
        actions.setPadding(pad, 0, pad, 0);
        actions.addView(action("Search a token by contract address", v -> CustomAssets.search(this, filter.getText().toString(), this::reload)));
        actions.addView(action("Add a chain", v -> CustomAssets.editChain(this, null, this::reload)));
        actions.addView(action("Add a token on a chain", v -> CustomAssets.addToken(this, null, this::reload)));
        actions.addView(action("Token index key (GoldRush)", v -> indexKey()));
        body.addView(actions);

        RecyclerView list = new RecyclerView(this);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setHasFixedSize(true);
        list.setItemViewCacheSize(20);
        adapter = new Adapter();
        list.setAdapter(adapter);
        body.addView(list, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        com.harvana.app.utils.AppBar.setContent(this, body, "Add coins", null);

        filter.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int c, int d) { }
            @Override public void onTextChanged(CharSequence s, int a, int c, int d) { }
            @Override public void afterTextChanged(Editable s) { query = s.toString().trim().toLowerCase(Locale.US); applyFilter(); }
        });
        reload();
    }

    @Override public boolean onSupportNavigateUp() { finish(); return true; }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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

    private void applyFilter() {
        shown.clear();
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
        if ("ADDED".equals(i.kind)) return !i.asset.hidden;
        return ChainAssetsSection.shown(this, i.key);
    }

    private void setOn(Item i, boolean on) {
        changed = true;
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
                TextView t = new TextView(parent.getContext());
                t.setTextSize(13);
                t.setTypeface(Typeface.DEFAULT_BOLD);
                t.setAllCaps(true);
                t.setTextColor(UiColors.textSecondary());
                t.setPadding(dp(16), dp(18), dp(16), dp(6));
                t.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                return new RecyclerView.ViewHolder(t) { };
            }
            return new RowHolder(parent.getContext());
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

        RowHolder(Context c) {
            super(new LinearLayout(c));
            LinearLayout outer = (LinearLayout) itemView;
            outer.setOrientation(LinearLayout.VERTICAL);
            outer.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            android.util.TypedValue tv = new android.util.TypedValue();
            c.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, tv, true);
            outer.setBackgroundResource(tv.resourceId);
            LinearLayout line = new LinearLayout(c);
            line.setGravity(Gravity.CENTER_VERTICAL);
            line.setPadding(dp(16), dp(10), dp(16), dp(10));
            line.setMinimumHeight(dp(60));
            icon = new ImageView(c);
            line.addView(icon, new LinearLayout.LayoutParams(dp(36), dp(36)));
            LinearLayout words = new LinearLayout(c);
            words.setOrientation(LinearLayout.VERTICAL);
            words.setPadding(dp(12), 0, dp(8), 0);
            title = new TextView(c);
            title.setTextSize(15);
            title.setTextColor(UiColors.textPrimary());
            title.setMaxLines(1);
            sub = new TextView(c);
            sub.setTextSize(12);
            sub.setTextColor(UiColors.textSecondary());
            sub.setMaxLines(1);
            words.addView(title);
            words.addView(sub);
            line.addView(words, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            toggle = new SwitchMaterial(c);
            line.addView(toggle);
            outer.addView(line);
            divider = com.harvana.app.ui.SheetDialog.divider(c);
            LinearLayout.LayoutParams dlp = com.harvana.app.ui.SheetDialog.dividerParams(c);
            dlp.setMarginStart(dp(64));
            outer.addView(divider, dlp);
        }
    }

    /** The GoldRush key that lets token discovery read chains with no free index (BNB Chain, Polygon, Base). */
    private void indexKey() {
        EditText key = new com.google.android.material.textfield.TextInputEditText(this);
        key.setHint("API key");
        key.setSingleLine(true);
        key.setText(TokenDiscovery.indexKey(this));
        com.harvana.app.p2p.ThemedDialogs.show(com.harvana.app.p2p.ThemedDialogs.alert(this)
                .setTitle("Token index key")
                .setView(key)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (d, w) -> TokenDiscovery.setIndexKey(this, key.getText() == null ? "" : key.getText().toString())));
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

    private TextView action(String text, View.OnClickListener l) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(14);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextColor(UiColors.accent());
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setMinHeight(dp(48));
        android.util.TypedValue tv = new android.util.TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackground, tv, true);
        t.setBackgroundResource(tv.resourceId);
        t.setOnClickListener(l);
        return t;
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
