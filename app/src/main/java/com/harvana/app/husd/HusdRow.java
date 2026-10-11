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

import org.json.JSONObject;

/**
 * The hUSD line on the Assets tab. It appears only once the network has hUSD switched on, and reports its value
 * (1 hUSD = 1 US dollar) so the wallet total includes it.
 */
public final class HusdRow {
    private HusdRow() {}

    public interface Fiat { void onFiat(double fiat); }

    public static void render(Context c, LinearLayout box, String address, Fiat onFiat) {
        if (box == null || c == null || address == null || address.isEmpty()) return;
        HusdClient.info(new HusdClient.Cb() {
            @Override public void ok(JSONObject info) {
                if (!info.optBoolean("active")) { box.post(() -> { box.removeAllViews(); onFiat.onFiat(0); }); return; }
                HusdClient.balance(address, new HusdClient.Cb() {
                    @Override public void ok(JSONObject b) { box.post(() -> draw(c, box, b.optLong("balanceRaw"), onFiat)); }
                    @Override public void fail(String e) { }
                });
            }
            @Override public void fail(String e) { }
        });
    }

    private static void draw(Context c, LinearLayout box, long units, Fiat onFiat) {
        View item = box.getChildCount() > 0 ? box.getChildAt(0) : null;
        if (item == null) {
            item = LayoutInflater.from(c).inflate(R.layout.coin_list_card, box, false);
            box.addView(item);
            ImageView logo = item.findViewById(R.id.iv_asset_logo);
            TextView name = item.findViewById(R.id.tv_asset_name);
            TextView symbol = item.findViewById(R.id.tv_asset_symbol);
            if (logo != null) logo.setImageResource(R.drawable.ic_coin_husd);
            if (name != null) name.setText("hUSD");
            if (symbol != null) symbol.setText("HUSD");
            View card = item.findViewById(R.id.cardview1);
            View.OnClickListener open = v -> c.startActivity(new Intent(c, HusdActivity.class));
            if (card != null) card.setOnClickListener(open); else item.setOnClickListener(open);
        }
        double usd = units / (double) HusdClient.UNIT;
        TextView bal = item.findViewById(R.id.tv_asset_balance);
        TextView side = item.findViewById(R.id.tv_asset_amount);
        if (bal != null) { bal.setText(HusdClient.format(units)); com.harvana.app.utils.BalancePrivacy.guard(bal); }
        final View row = item;
        FxRates.load(c, () -> {
            if (side != null) { side.setText(FxRates.money(c, usd)); com.harvana.app.utils.BalancePrivacy.guard(side); }
            double fiat = FxRates.fiat(c, usd);
            onFiat.onFiat(Double.isNaN(fiat) ? 0 : fiat);
            row.setTag(R.id.tv_asset_amount, fiat);
        });
    }
}
