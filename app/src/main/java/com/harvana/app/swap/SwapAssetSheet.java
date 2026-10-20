package com.harvana.app.swap;

import android.app.Activity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.harvana.app.R;
import com.harvana.app.husd.Stable;
import com.harvana.app.husd.StableIcon;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Asset selector: search, then HARV and every Harvana coin with the wallet's balance. A coin whose pool is closed is
 * shown as unavailable and cannot be picked; a coin the node has not switched on is not listed.
 */
final class SwapAssetSheet {

    interface Pick { void picked(String code); }

    private SwapAssetSheet() {}

    static void show(SwapActivity host, String title, List<String> assets, JSONArray pools, Map<String, Long> balances,
                     String selected, Pick pick) {
        BottomSheetDialog d = new BottomSheetDialog(host);
        View root = host.getLayoutInflater().inflate(R.layout.sheet_swap_assets, null);
        ((TextView) root.findViewById(R.id.tv_swap_assets_title)).setText(title);
        LinearLayout list = root.findViewById(R.id.swap_assets_list);
        EditText search = root.findViewById(R.id.et_swap_assets_search);
        Runnable fill = () -> {
            list.removeAllViews();
            String q = search.getText() == null ? "" : search.getText().toString().trim().toLowerCase(Locale.US);
            for (String code : assets) {
                String sym = SwapText.sym(code), name = SwapText.name(code);
                if (!q.isEmpty() && !sym.toLowerCase(Locale.US).contains(q) && !name.toLowerCase(Locale.US).contains(q)
                        && !code.toLowerCase(Locale.US).contains(q)) continue;
                boolean open = SwapMath.HARV.equals(code) || openPool(pools, code);
                View row = host.getLayoutInflater().inflate(R.layout.item_swap_asset, list, false);
                ((ImageView) row.findViewById(R.id.iv_swap_asset)).setImageDrawable(host.icon(code));
                ((TextView) row.findViewById(R.id.tv_swap_asset_symbol)).setText(sym);
                ((TextView) row.findViewById(R.id.tv_swap_asset_name)).setText(name);
                Long bal = balances.get(code);
                ((TextView) row.findViewById(R.id.tv_swap_asset_balance)).setText(bal == null ? "…" : SwapText.number(code, bal));
                TextView state = row.findViewById(R.id.tv_swap_asset_state);
                if (code.equals(selected)) {
                    state.setText("Selected ✓");
                    state.setTextColor(host.tone(1));
                } else if (!open) {
                    state.setText("No liquidity");
                    state.setTextColor(host.tone(-1));
                } else {
                    state.setText("");
                }
                row.setAlpha(open ? 1f : 0.5f);
                row.setContentDescription(sym + ", " + name + (open ? "" : ", unavailable: no liquidity"));
                if (open) row.setOnClickListener(v -> { d.dismiss(); pick.picked(code); });
                list.addView(row);
            }
            if (list.getChildCount() == 0) list.addView(com.harvana.app.utils.EmptyState.illustrate(SwapText.line(host, "No asset matches."), com.harvana.app.R.drawable.illus_empty_search));
        };
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) { fill.run(); }
        });
        fill.run();
        d.setContentView(root);
        d.show();
    }

    private static boolean openPool(JSONArray pools, String code) {
        for (int i = 0; pools != null && i < pools.length(); i++) {
            JSONObject p = pools.optJSONObject(i);
            if (p != null && code.equals(p.optString("asset"))) return p.optBoolean("open");
        }
        return false;
    }
}
