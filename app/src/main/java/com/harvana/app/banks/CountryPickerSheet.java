package com.harvana.app.banks;

import android.app.Activity;
import android.graphics.Typeface;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.harvana.app.utils.UiColors;

/** The countries the bank list covers, each with its flag and currency. */
public final class CountryPickerSheet {
    private CountryPickerSheet() {}

    public interface OnPicked { void picked(BankDirectory.Country country); }

    public static void show(Activity host, OnPicked cb) {
        BottomSheetDialog sheet = com.harvana.app.p2p.ThemedDialogs.sheet(host);
        LinearLayout root = new LinearLayout(host);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(UiColors.surface());
        int p = dp(host, 16);
        root.setPadding(p, p, p, p);
        TextView title = new TextView(host);
        title.setText("Where is the account?");
        title.setTextSize(18);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(UiColors.textPrimary());
        title.setPadding(0, 0, 0, dp(host, 8));
        root.addView(title);
        LinearLayout list = new LinearLayout(host);
        list.setOrientation(LinearLayout.VERTICAL);
        android.util.TypedValue tv = new android.util.TypedValue();
        host.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, tv, true);
        for (BankDirectory.Country k : BankDirectory.countries(host)) {
            TextView row = new TextView(host);
            row.setText(k.flag() + "   " + k.name + (k.currency.isEmpty() ? "" : "  ·  " + k.currency));
            row.setTextSize(15);
            row.setTextColor(UiColors.textPrimary());
            row.setPadding(dp(host, 4), dp(host, 12), dp(host, 4), dp(host, 12));
            row.setBackgroundResource(tv.resourceId);
            row.setOnClickListener(v -> { sheet.dismiss(); BankDirectory.rememberCountry(host, k.iso); cb.picked(k); });
            list.addView(row);
        }
        ScrollView sv = new ScrollView(host);
        sv.addView(list);
        root.addView(sv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                (int) (host.getResources().getDisplayMetrics().heightPixels * 0.6f)));
        sheet.setContentView(root);
        sheet.show();
    }

    private static int dp(Activity a, int v) { return Math.round(v * a.getResources().getDisplayMetrics().density); }
}
