package com.harvana.app.group;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.harvana.app.LocationHelper;
import com.harvana.app.ui.AppSheet;
import com.harvana.app.ui.Btn;
import com.harvana.app.utils.UiColors;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The location filter on Discover. Global is the default: every group, from every country. A person may narrow it to a country,
 * then (only once a country is chosen) to a state, then (only once a state is chosen) to a city. Each step is optional. The
 * choice is saved on the phone, so it is still there the next time the app opens; "Reset to global" is the only way back.
 */
public final class LocationFilter {
    private LocationFilter() {}

    public static final class Value {
        public String country = "", state = "", city = "";
        public boolean isGlobal() { return country.isEmpty(); }
        Value copy() { Value v = new Value(); v.country = country; v.state = state; v.city = city; return v; }

        /** Short text for the filter chip: Global, a country, "state, country" or "city, state". */
        public String label() {
            if (!city.isEmpty()) return city + ", " + state;
            if (!state.isEmpty()) return state + ", " + country;
            if (!country.isEmpty()) return country;
            return "Global";
        }
    }

    public interface OnApplied { void onApplied(Value value); }

    private static final String PREFS = "discover_location_filter";

    public static Value load(Context c) {
        SharedPreferences p = c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        Value v = new Value();
        v.country = p.getString("country", "");
        v.state = v.country.isEmpty() ? "" : p.getString("state", "");
        v.city = v.state.isEmpty() ? "" : p.getString("city", "");
        return v;
    }

    public static void save(Context c, Value v) {
        c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("country", v.country).putString("state", v.state).putString("city", v.city).apply();
    }

    /** True when the group is inside the chosen place. Global matches every group. Only the levels chosen are compared. */
    public static boolean matches(Value v, Map<String, Object> group) {
        if (v == null || v.isGlobal()) return true;
        if (!same(v.country, group.get("country"))) return false;
        if (!v.state.isEmpty() && !same(v.state, group.get("state"))) return false;
        if (!v.city.isEmpty() && !same(v.city, group.get("city"))) return false;
        return true;
    }

    private static boolean same(String wanted, Object actual) {
        return norm(wanted).equals(norm(actual == null ? "" : String.valueOf(actual)));
    }

    /** "Lagos State" and "Lagos" are the same place; so are different capitals and spacing. */
    static String norm(String s) {
        String t = s == null ? "" : s.trim().toLowerCase(Locale.US);
        for (String suffix : new String[]{" state", " province", " region"}) if (t.endsWith(suffix)) t = t.substring(0, t.length() - suffix.length());
        return t.trim();
    }

    // ── the picker ─────────────────────────────────────────────────────────

    private static volatile LocationHelper helper;
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();

    /** Opens the filter sheet. {@code onApplied} runs after Apply or Reset, with the value already saved. */
    public static void show(final Activity a, final OnApplied onApplied) {
        final Value draft = load(a).copy();
        final BottomSheetDialog dlg = new AppSheet(a);
        final float d = a.getResources().getDisplayMetrics().density;
        final LinearLayout root = new LinearLayout(a);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding((int) (20 * d), (int) (18 * d), (int) (20 * d), (int) (22 * d));
        root.setBackgroundColor(UiColors.surface());

        root.addView(text(a, "Location", 18, true, UiColors.textPrimary()));
        TextView hint = text(a, "", 12.5f, false, UiColors.textSecondary());
        hint.setPadding(0, (int) (4 * d), 0, (int) (14 * d));
        root.addView(hint);

        final MaterialButton country = Btn.outlined(a, "", null);
        final MaterialButton state = Btn.outlined(a, "", null);
        final MaterialButton city = Btn.outlined(a, "", null);
        for (MaterialButton b : new MaterialButton[]{country, state, city}) {
            b.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (52 * d));
            lp.bottomMargin = (int) (10 * d);
            root.addView(b, lp);
        }
        final MaterialButton reset = Btn.text(a, "Reset to global", null);
        final MaterialButton apply = Btn.filled(a, "Apply", null);
        LinearLayout actions = new LinearLayout(a);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.addView(reset, new LinearLayout.LayoutParams(0, (int) (50 * d), 1f));
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(0, (int) (50 * d), 1f);
        ap.setMarginStart((int) (8 * d));
        actions.addView(apply, ap);
        LinearLayout.LayoutParams actionsLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        actionsLp.topMargin = (int) (8 * d);
        root.addView(actions, actionsLp);

        final Runnable paint = () -> {
            country.setText("Country   ·   " + (draft.country.isEmpty() ? "All countries" : draft.country));
            state.setText("State   ·   " + (draft.state.isEmpty() ? "All states" : draft.state));
            city.setText("City   ·   " + (draft.city.isEmpty() ? "All cities" : draft.city));
            state.setEnabled(!draft.country.isEmpty());
            state.setAlpha(draft.country.isEmpty() ? 0.45f : 1f);
            city.setEnabled(!draft.state.isEmpty());
            city.setAlpha(draft.state.isEmpty() ? 0.45f : 1f);
            hint.setText(draft.isGlobal()
                ? "Global: groups from every country are shown. Pick a country to narrow it down. State and city are optional."
                : "Showing groups in " + draft.label() + ".");
        };
        paint.run();

        country.setOnClickListener(v -> withHelper(a, h -> pick(a, "Country", h.getCountryNames(), draft.country, "All countries", chosen -> {
            if (!chosen.equals(draft.country)) { draft.country = chosen; draft.state = ""; draft.city = ""; }
            paint.run();
        })));
        state.setOnClickListener(v -> withHelper(a, h -> pick(a, "State in " + draft.country, h.getStateNames(draft.country), draft.state, "All states", chosen -> {
            if (!chosen.equals(draft.state)) { draft.state = chosen; draft.city = ""; }
            paint.run();
        })));
        city.setOnClickListener(v -> withHelper(a, h -> pick(a, "City in " + draft.state, h.getCityNames(draft.country, draft.state), draft.city, "All cities", chosen -> {
            draft.city = chosen;
            paint.run();
        })));
        reset.setOnClickListener(v -> {
            Value global = new Value();
            save(a, global);
            dlg.dismiss();
            if (onApplied != null) onApplied.onApplied(global);
        });
        apply.setOnClickListener(v -> {
            save(a, draft);
            dlg.dismiss();
            if (onApplied != null) onApplied.onApplied(draft);
        });

        dlg.setContentView(root);
        dlg.show();
    }

    private interface HelperReady { void run(LocationHelper h); }
    private interface Chosen { void on(String name); }

    /** The place list is a large file: read it once, off the screen thread. */
    private static void withHelper(final Activity a, final HelperReady ready) {
        LocationHelper h = helper;
        if (h != null) { ready.run(h); return; }
        IO.execute(() -> {
            LocationHelper loaded = new LocationHelper(a);
            helper = loaded;
            a.runOnUiThread(() -> ready.run(loaded));
        });
    }

    /** A searchable list in its own sheet. The first row clears this level ("All ..."). */
    private static void pick(final Activity a, String title, final List<String> items, final String selected, final String anyLabel, final Chosen chosen) {
        final BottomSheetDialog dlg = new AppSheet(a);
        final float d = a.getResources().getDisplayMetrics().density;
        LinearLayout root = new LinearLayout(a);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding((int) (20 * d), (int) (18 * d), (int) (20 * d), (int) (12 * d));
        root.setBackgroundColor(UiColors.surface());
        root.addView(text(a, title, 18, true, UiColors.textPrimary()));
        final EditText search = new com.google.android.material.textfield.TextInputEditText(a);
        search.setHint("Search");
        search.setSingleLine(true);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sp.topMargin = (int) (10 * d);
        sp.bottomMargin = (int) (6 * d);
        root.addView(search, sp);
        final ListView list = new ListView(a);
        list.setDivider(null);
        root.addView(list, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (a.getResources().getDisplayMetrics().heightPixels * 0.55f)));

        final List<String> shown = new ArrayList<>();
        final android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<String>(a, android.R.layout.simple_list_item_1, shown) {
            @Override public View getView(int position, View convert, ViewGroup parent) {
                TextView t = (TextView) super.getView(position, convert, parent);
                String row = getItem(position);
                boolean any = position == 0;
                boolean on = any ? selected.isEmpty() : row.equals(selected);
                t.setTextColor(on ? UiColors.accent() : UiColors.textPrimary());
                t.setTypeface(null, on || any ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
                t.setPadding((int) (4 * d), (int) (12 * d), (int) (4 * d), (int) (12 * d));
                return t;
            }
        };
        list.setAdapter(adapter);
        final Runnable refill = () -> {
            String q = search.getText().toString().trim().toLowerCase(Locale.US);
            shown.clear();
            shown.add(anyLabel);
            for (String s : items) if (q.isEmpty() || s.toLowerCase(Locale.US).contains(q)) shown.add(s);
            adapter.notifyDataSetChanged();
        };
        refill.run();
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int af) { }
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) { }
            @Override public void afterTextChanged(Editable s) { refill.run(); }
        });
        list.setOnItemClickListener((p, v, position, id) -> {
            chosen.on(position == 0 ? "" : shown.get(position));
            dlg.dismiss();
        });
        dlg.setContentView(root);
        dlg.show();
    }

    private static TextView text(Context c, String s, float sp, boolean bold, int color) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(null, android.graphics.Typeface.BOLD);
        return t;
    }
}
