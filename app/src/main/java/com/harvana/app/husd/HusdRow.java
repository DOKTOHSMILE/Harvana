package com.harvana.app.husd;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.harvana.app.R;
import com.harvana.app.wallet.FxRates;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The stable-asset lines on the Assets tab: hUSD, hEUR, hNGN and the rest. A line appears only once the network has
 * that asset switched on. Each reports its value (1 hNGN = 1 naira, converted to the currency this person shows) so
 * the wallet total includes it; {@code onFiat} gets the sum of all of them.
 */
public final class HusdRow {
    private HusdRow() {}

    public interface Fiat { void onFiat(double fiat); }

    public static void render(Context c, LinearLayout box, String address, Fiat onFiat) {
        if (box == null || c == null || address == null || address.isEmpty()) return;
        HusdClient.list(new HusdClient.Cb() {
            @Override public void ok(JSONObject r) {
                JSONArray arr = r.optJSONArray("assets");
                List<String> on = new ArrayList<>();
                if (arr != null) for (int i = 0; i < arr.length(); i++) {
                    JSONObject a = arr.optJSONObject(i);
                    if (a != null && a.optBoolean("active") && Stable.isStable(a.optString("asset"))) on.add(a.optString("asset"));
                }
                box.post(() -> {
                    // Drop lines for assets that are no longer on.
                    for (int i = box.getChildCount() - 1; i >= 0; i--) {
                        Object tag = box.getChildAt(i).getTag();
                        if (!(tag instanceof String) || !on.contains(tag)) box.removeViewAt(i);
                    }
                    if (on.isEmpty()) { onFiat.onFiat(0); return; }
                    Map<String, Double> values = new HashMap<>();
                    for (String code : on) {
                        HusdClient.balance(code, address, new HusdClient.Cb() {
                            @Override public void ok(JSONObject b) {
                                box.post(() -> draw(c, box, code, b.optLong("balanceRaw"), fiat -> {
                                    values.put(code, fiat);
                                    double sum = 0;
                                    for (double v : values.values()) sum += v;
                                    onFiat.onFiat(sum);
                                }));
                            }
                            @Override public void fail(String e) { }
                        });
                    }
                });
            }
            @Override public void fail(String e) { }
        });
    }

    private static View lineFor(LinearLayout box, String code) {
        for (int i = 0; i < box.getChildCount(); i++) if (code.equals(box.getChildAt(i).getTag())) return box.getChildAt(i);
        return null;
    }

    private static void draw(Context c, LinearLayout box, String code, long units, Fiat onFiat) {
        View item = lineFor(box, code);
        if (item == null) {
            item = LayoutInflater.from(c).inflate(R.layout.coin_list_card, box, false);
            item.setTag(code);
            // Keep the order the node lists them in (hUSD first), whichever balance answers first.
            int at = 0;
            for (String k : Stable.CODES) {
                if (k.equals(code)) break;
                if (lineFor(box, k) != null) at++;
            }
            box.addView(item, Math.min(at, box.getChildCount()));
            ImageView logo = item.findViewById(R.id.iv_asset_logo);
            TextView name = item.findViewById(R.id.tv_asset_name);
            TextView symbol = item.findViewById(R.id.tv_asset_symbol);
            if (logo != null) logo.setImageDrawable(StableIcon.drawable(c, code));
            if (name != null) name.setText(Stable.symbol(code));
            if (symbol != null) symbol.setText(code);
            View card = item.findViewById(R.id.cardview1);
            View.OnClickListener open = v -> c.startActivity(new Intent(c, HusdActivity.class).putExtra(HusdActivity.EXTRA_ASSET, code));
            if (card != null) card.setOnClickListener(open); else item.setOnClickListener(open);
        }
        double amount = units / (double) HusdClient.UNIT;
        TextView bal = item.findViewById(R.id.tv_asset_balance);
        TextView side = item.findViewById(R.id.tv_asset_amount);
        if (bal != null) { bal.setText(HusdClient.format(units)); com.harvana.app.utils.BalancePrivacy.guard(bal); }
        final View row = item;
        FxRates.load(c, () -> {
            // amount is in the asset's own currency: turn it into dollars first, then into what this person shows.
            double perUsd = FxRates.perUsd(c, Stable.currency(code));
            double usd = Double.isNaN(perUsd) || perUsd <= 0 ? Double.NaN : amount / perUsd;
            if (side != null) { side.setText(Double.isNaN(usd) ? "…" : FxRates.money(c, usd)); com.harvana.app.utils.BalancePrivacy.guard(side); }
            double fiat = Double.isNaN(usd) ? Double.NaN : FxRates.fiat(c, usd);
            onFiat.onFiat(Double.isNaN(fiat) ? 0 : fiat);
            row.setTag(R.id.tv_asset_amount, fiat);
        });
    }
}
