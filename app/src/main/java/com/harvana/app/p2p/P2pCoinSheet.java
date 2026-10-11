package com.harvana.app.p2p;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.harvana.app.R;
import com.harvana.app.husd.StableIcon;

/** "Select Coin" for P2P: HARV and every stable asset the network has switched on. Picking one sets P2pAsset. */
public final class P2pCoinSheet {
    private P2pCoinSheet() {}

    public static void show(Context c, Runnable onPicked) {
        P2pAsset.loadActive(() -> new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> build(c, onPicked)));
    }

    private static void build(Context c, Runnable onPicked) {
        BottomSheetDialog dialog = ThemedDialogs.sheet(c);
        LinearLayout root = new LinearLayout(c);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(c, 20);
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(ThemedDialogs.surface(c));
        TextView title = text(c, "Select Coin", 18, true);
        title.setPadding(0, 0, 0, dp(c, 12));
        root.addView(title);
        for (String code : P2pAsset.available()) {
            LinearLayout row = new LinearLayout(c);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(c, 12), 0, dp(c, 12));
            ImageView icon = new ImageView(c);
            if (P2pAsset.isStable(code)) icon.setImageDrawable(StableIcon.drawable(c, code));
            else icon.setImageResource(R.drawable.ic_coin_harv);
            row.addView(icon, new LinearLayout.LayoutParams(dp(c, 28), dp(c, 28)));
            TextView label = text(c, P2pAsset.name(code), 15, code.equals(P2pAsset.get()));
            label.setPadding(dp(c, 12), 0, 0, 0);
            row.addView(label, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            if (code.equals(P2pAsset.get())) {
                TextView check = text(c, "", 18, true);
                com.harvana.app.utils.Icons.check(check, true);
                row.addView(check);
            }
            row.setOnClickListener(v -> {
                P2pAsset.set(code);
                dialog.dismiss();
                if (onPicked != null) onPicked.run();
            });
            root.addView(row);
        }
        if (P2pAsset.available().size() == 1) {
            TextView note = text(c, "Stable coins (hUSD, hNGN, …) appear here once the network switches them on.", 13, false);
            note.setAlpha(0.7f);
            note.setPadding(0, dp(c, 12), 0, 0);
            root.addView(note);
        }
        dialog.setContentView(root);
        dialog.show();
    }

    private static TextView text(Context c, String s, float sp, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(ThemedDialogs.onSurface(c));
        if (bold) t.setTypeface(null, Typeface.BOLD);
        return t;
    }

    private static int dp(Context c, int v) { return Math.round(v * c.getResources().getDisplayMetrics().density); }
}
