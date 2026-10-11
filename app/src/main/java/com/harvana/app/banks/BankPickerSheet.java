package com.harvana.app.banks;

import android.app.Activity;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.harvana.app.utils.UiColors;

import java.util.ArrayList;
import java.util.List;

/** A searchable list of banks with their logos. Picking one hands back the official bank. */
public final class BankPickerSheet {
    private BankPickerSheet() {}

    public interface OnPicked { void picked(BankDirectory.Bank bank); }

    public static void show(Activity host, String initialQuery, OnPicked cb) {
        show(host, "NG", initialQuery, cb);
    }

    /** The banks and payment apps of one country (ISO code, or "OTHER"). */
    public static void show(Activity host, String country, String initialQuery, OnPicked cb) {
        new Thread(() -> BankDirectory.refreshIfStale(host.getApplicationContext())).start();
        BottomSheetDialog sheet = com.harvana.app.p2p.ThemedDialogs.sheet(host);
        LinearLayout root = new LinearLayout(host);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(UiColors.surface());
        int p = dp(host, 16);
        root.setPadding(p, p, p, 0);

        TextView title = new TextView(host);
        BankDirectory.Country k = BankDirectory.country(host, country);
        title.setText(k.flag() + "  Banks in " + k.name);
        title.setTextSize(18);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(UiColors.textPrimary());
        root.addView(title);

        EditText search = new EditText(host);
        search.setHint("Search banks and payment apps");
        search.setSingleLine(true);
        search.setTextSize(15);
        search.setTextColor(UiColors.textPrimary());
        search.setHintTextColor(UiColors.textTertiary());
        android.graphics.drawable.GradientDrawable sb = new android.graphics.drawable.GradientDrawable();
        sb.setCornerRadius(dp(host, 12));
        sb.setColor(UiColors.surfaceAlt());
        search.setBackground(sb);
        search.setPadding(dp(host, 14), dp(host, 12), dp(host, 14), dp(host, 12));
        if (initialQuery != null) search.setText(initialQuery);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.topMargin = dp(host, 12);
        slp.bottomMargin = dp(host, 8);
        root.addView(search, slp);

        RecyclerView list = new RecyclerView(host);
        list.setLayoutManager(new LinearLayoutManager(host));
        Adapter adapter = new Adapter(host, b -> { sheet.dismiss(); cb.picked(b); });
        list.setAdapter(adapter);
        root.addView(list, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (host.getResources().getDisplayMetrics().heightPixels * 0.62f)));

        adapter.set(rows(host, country, search.getText().toString()));
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { adapter.set(rows(host, country, s.toString())); }
            @Override public void afterTextChanged(Editable s) {}
        });

        sheet.setContentView(root);
        sheet.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
        sheet.getBehavior().setSkipCollapsed(true);
        sheet.show();
    }

    /** The matches, plus a last row to use the typed name as it is, for a bank that isn't listed. */
    private static List<BankDirectory.Bank> rows(Activity host, String country, String typed) {
        List<BankDirectory.Bank> out = new ArrayList<>(BankDirectory.search(host, country, typed));
        String t = typed == null ? "" : typed.trim();
        if (t.length() >= 2) {
            boolean exact = false;
            for (BankDirectory.Bank b : out) if (b.name.equalsIgnoreCase(t) || b.display().equalsIgnoreCase(t)) { exact = true; break; }
            if (!exact) {
                BankDirectory.Bank own = new BankDirectory.Bank();
                own.name = t;
                own.country = country;
                own.kind = "custom";
                out.add(own);
            }
        }
        return out;
    }

    private static final class Adapter extends RecyclerView.Adapter<Adapter.Holder> {
        private final Activity host;
        private final OnPicked cb;
        private final List<BankDirectory.Bank> items = new ArrayList<>();

        Adapter(Activity host, OnPicked cb) { this.host = host; this.cb = cb; }

        void set(List<BankDirectory.Bank> banks) { items.clear(); items.addAll(banks); notifyDataSetChanged(); }

        @Override public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
            LinearLayout row = new LinearLayout(host);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(host, 4), dp(host, 10), dp(host, 4), dp(host, 10));
            row.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            android.util.TypedValue tv = new android.util.TypedValue();
            host.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, tv, true);
            row.setBackgroundResource(tv.resourceId);
            ImageView logo = new ImageView(host);
            row.addView(logo, new LinearLayout.LayoutParams(dp(host, 40), dp(host, 40)));
            LinearLayout col = new LinearLayout(host);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setPadding(dp(host, 12), 0, 0, 0);
            TextView name = new TextView(host);
            name.setTextSize(15);
            name.setTextColor(UiColors.textPrimary());
            TextView meta = new TextView(host);
            meta.setTextSize(12);
            meta.setTextColor(UiColors.textTertiary());
            col.addView(name);
            col.addView(meta);
            row.addView(col, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            return new Holder(row, logo, name, meta);
        }

        @Override public void onBindViewHolder(Holder h, int pos) {
            BankDirectory.Bank b = items.get(pos);
            if ("custom".equals(b.kind)) {
                h.name.setText("Use \"" + b.name + "\"");
                h.meta.setText("My bank isn't in the list");
                BankLogo.into(h.logo, b);
                h.itemView.setOnClickListener(v -> cb.picked(b));
                return;
            }
            h.name.setText(b.display());
            String meta = b.shortName.isEmpty() || b.shortName.equals(b.name) ? b.kindLabel() : b.name;
            if (!b.ussd.isEmpty()) meta += " · " + b.ussd;
            h.meta.setText(meta);
            BankLogo.into(h.logo, b);
            h.itemView.setOnClickListener(v -> cb.picked(b));
        }

        @Override public int getItemCount() { return items.size(); }

        static final class Holder extends RecyclerView.ViewHolder {
            final ImageView logo; final TextView name, meta;
            Holder(View v, ImageView logo, TextView name, TextView meta) { super(v); this.logo = logo; this.name = name; this.meta = meta; }
        }
    }

    private static int dp(Activity a, int v) { return Math.round(v * a.getResources().getDisplayMetrics().density); }
}
