package com.harvana.app.wallet;

import android.app.Activity;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.harvana.app.R;

/**
 * The two network sheets, as MetaMask shows them: "Add this network?" (its name, RPC URL, chain ID, coin symbol, and the
 * rest under View all details, Cancel / Approve) and "Allow this site to switch the network?" (from → to, Cancel / Switch
 * network). Both name the site when a dApp asked. Layouts: res/layout/sheet_approve_network.xml and sheet_switch_network.xml.
 */
public final class NetworkSheets {
    private NetworkSheets() {}

    /**
     * Asks before adding {@code n}. {@code origin} is the site that asked (null when the person typed it in);
     * {@code listed} is true when the chain list knows this chain ID, which hides the "not in the chain list" warning.
     */
    public static void approve(Activity a, CustomAssets.Asset n, String origin, boolean listed, Runnable onApprove, Runnable onCancel) {
        com.harvana.app.ui.AppSheet sheet = new com.harvana.app.ui.AppSheet(a);
        View v = a.getLayoutInflater().inflate(R.layout.sheet_approve_network, null, false);
        origin(v, origin);
        icon(v.findViewById(R.id.iv_network_sheet_icon), "C:" + n.chainId, n.icon);
        ((TextView) v.findViewById(R.id.tv_network_sheet_name)).setText(n.chainName);
        ((TextView) v.findViewById(R.id.tv_network_sheet_url)).setText(host(n.rpc.split(",")[0].trim()));
        ((TextView) v.findViewById(R.id.tv_network_sheet_chain_id)).setText(n.chainId + "  (0x" + Long.toHexString(n.chainId) + ")");
        ((TextView) v.findViewById(R.id.tv_network_sheet_symbol)).setText(n.symbol);
        ((TextView) v.findViewById(R.id.tv_network_sheet_explorer)).setText(n.explorer.isEmpty() ? "—" : n.explorer);
        ((TextView) v.findViewById(R.id.tv_network_sheet_decimals)).setText(String.valueOf(n.decimals));
        TextView more = v.findViewById(R.id.tv_network_sheet_more);
        more.setOnClickListener(x -> {
            ((TextView) v.findViewById(R.id.tv_network_sheet_url)).setText(n.rpc.split(",")[0].trim());
            v.findViewById(R.id.tv_network_sheet_explorer_row).setVisibility(View.VISIBLE);
            v.findViewById(R.id.tv_network_sheet_decimals_row).setVisibility(View.VISIBLE);
            more.setVisibility(View.GONE);
        });
        v.findViewById(R.id.network_sheet_warning).setVisibility(listed ? View.GONE : View.VISIBLE);
        final boolean[] answered = {false};
        v.findViewById(R.id.btn_network_sheet_cancel).setOnClickListener(x -> sheet.dismiss());
        v.findViewById(R.id.btn_network_sheet_approve).setOnClickListener(x -> {
            answered[0] = true;
            sheet.dismiss();
            if (onApprove != null) onApprove.run();
        });
        sheet.setOnDismissListener(d -> { if (!answered[0] && onCancel != null) onCancel.run(); });
        sheet.setContentView(v);
        sheet.show();
    }

    /** Asks before a site moves the browser from one network to another. Keys: "ETH", "BNB", "POL", "BASE" or "C:<chain id>". */
    public static void switchNetwork(Activity a, String origin, String fromKey, String fromName, String toKey, String toName,
                                     Runnable onSwitch, Runnable onCancel) {
        com.harvana.app.ui.AppSheet sheet = new com.harvana.app.ui.AppSheet(a);
        View v = a.getLayoutInflater().inflate(R.layout.sheet_switch_network, null, false);
        origin(v, origin);
        icon(v.findViewById(R.id.iv_switch_from), fromKey, null);
        icon(v.findViewById(R.id.iv_switch_to), toKey, null);
        ((TextView) v.findViewById(R.id.tv_switch_from)).setText(fromName);
        ((TextView) v.findViewById(R.id.tv_switch_to)).setText(toName);
        final boolean[] answered = {false};
        v.findViewById(R.id.btn_network_sheet_cancel).setOnClickListener(x -> sheet.dismiss());
        v.findViewById(R.id.btn_network_sheet_switch).setOnClickListener(x -> {
            answered[0] = true;
            sheet.dismiss();
            if (onSwitch != null) onSwitch.run();
        });
        sheet.setOnDismissListener(d -> { if (!answered[0] && onCancel != null) onCancel.run(); });
        sheet.setContentView(v);
        sheet.show();
    }

    /** A network's logo: the built-in chains' own, an added network's icon from the chain list, else a plain coin. */
    public static void icon(ImageView iv, String key, String url) {
        if (iv == null) return;
        int res = builtInIcon(key);
        if (res != 0) { iv.setImageResource(res); return; }
        String u = url;
        if ((u == null || u.isEmpty()) && key != null) {
            CustomAssets.Asset added = CustomAssets.find(key);
            if (added != null) u = added.icon;
        }
        if (u != null && u.startsWith("http")) {
            com.bumptech.glide.Glide.with(iv).load(u).placeholder(R.drawable.ic_coin_generic).error(R.drawable.ic_coin_generic).circleCrop().into(iv);
        } else {
            iv.setImageResource(R.drawable.ic_coin_generic);
        }
    }

    private static int builtInIcon(String key) {
        if (key == null) return 0;
        switch (key) {
            case "HARV": return R.drawable.ic_harv_coin;
            case "ETH": return R.drawable.ic_coin_eth;
            case "BNB": return R.drawable.ic_coin_bnb;
            case "POL": return R.drawable.ic_coin_pol;
            case "BASE": return R.drawable.ic_coin_base;
            default: return 0;
        }
    }

    private static void origin(View v, String origin) {
        TextView t = v.findViewById(R.id.tv_network_sheet_origin);
        if (origin == null || origin.isEmpty()) { t.setVisibility(View.GONE); return; }
        t.setText(origin);
        t.setVisibility(View.VISIBLE);
    }

    static String host(String url) {
        try { String h = Uri.parse(url).getHost(); return h == null || h.isEmpty() ? url : h; } catch (Exception e) { return url; }
    }
}
