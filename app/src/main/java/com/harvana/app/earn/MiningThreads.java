package com.harvana.app.earn;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.harvana.app.utils.UiColors;

/**
 * How many cores a phone may hash. The top count is red and asks before it turns on.
 * The saved count never starts above one less than the phone's cores, unless the person accepts the warning.
 */
public final class MiningThreads {
    public static final String PREFS = "harvana_mining_prefs";
    public static final String KEY = "thread_count";

    private MiningThreads() {}

    public static int cores() {
        int n = Runtime.getRuntime().availableProcessors();
        return n < 1 ? 1 : n;
    }

    public static int safeCap(int cores) {
        return cores <= 1 ? 1 : cores - 1;
    }

    public static boolean dangerous(int threads, int cores) {
        return threads >= cores && cores > 1;
    }

    public static int heat(int threads, int cores) {
        if (dangerous(threads, cores)) return 2;
        if (threads >= Math.max(2, cores - 2)) return 1;
        return 0;
    }

    public static int saved(Context c) {
        int saved = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY, 1);
        if (saved < 1) saved = 1;
        if (saved > cores()) saved = cores();
        return saved;
    }

    public static void attach(Context c, LinearLayout parent) {
        if (c == null || parent == null) return;
        if (parent.findViewWithTag("mining-threads") != null) return;
        int cores = cores();
        float d = c.getResources().getDisplayMetrics().density;
        int pad = (int) (14 * d);
        LinearLayout card = new LinearLayout(c);
        card.setTag("mining-threads");
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(pad, pad, pad, pad);
        GradientDrawable surface = new GradientDrawable();
        surface.setColor(UiColors.surface());
        surface.setCornerRadius(14 * d);
        card.setBackground(surface);
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardLp.bottomMargin = (int) (12 * d);
        card.setLayoutParams(cardLp);
        card.addView(line(c, "Phone hashing", 16, true, UiColors.textPrimary()));
        card.addView(line(c, "Each step is one core. The red one can overheat the phone.", 12, false, UiColors.textSecondary()));
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        TextView status = line(c, label(saved(c), cores), 13, false, UiColors.textSecondary());
        int max = Math.min(8, cores);
        for (int n = 1; n <= max; n++) {
            final int threads = n;
            TextView chip = line(c, String.valueOf(n), 14, true, 0xFF1A1208);
            chip.setGravity(Gravity.CENTER);
            int color = heat(n, cores) == 2 ? 0xFFD32F2F : heat(n, cores) == 1 ? 0xFFF08A24 : 0xFF3DDC97;
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(10 * d);
            bg.setColor(color);
            if (heat(n, cores) == 2) bg.setStroke((int) (2 * d), 0xFFFFCDD2);
            chip.setBackground(bg);
            int chipPad = (int) (10 * d);
            chip.setPadding(chipPad, chipPad, chipPad, chipPad);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            lp.rightMargin = (int) (6 * d);
            chip.setOnClickListener(v -> choose(c, threads, cores, status));
            row.addView(chip, lp);
        }
        card.addView(row);
        card.addView(status);
        parent.addView(card, 0);
    }

    private static TextView line(Context c, String text, float sp, boolean bold, int color) {
        TextView t = new TextView(c);
        t.setText(text);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(null, Typeface.BOLD);
        return t;
    }

    private static void choose(Context c, int threads, int cores, TextView status) {
        if (!dangerous(threads, cores)) {
            save(c, threads);
            status.setText(label(threads, cores));
            return;
        }
        new com.harvana.app.ui.SheetDialog.Builder(c)
            .setTitle("This can overheat the phone")
            .setMessage("Using every core keeps the CPU at the top of its range. The phone can get hot, slow down, and shut itself off. Leave one core free unless you accept that.")
            .setPositiveButton("Use " + threads + " anyway", (d, w) -> {
                save(c, threads);
                status.setText(label(threads, cores));
            })
            .setNegativeButton("Keep it safer", (d, w) -> {
                save(c, safeCap(cores));
                status.setText(label(safeCap(cores), cores));
            })
            .show();
    }

    private static void save(Context c, int threads) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(KEY, threads).apply();
    }

    private static String label(int threads, int cores) {
        if (dangerous(threads, cores)) return threads + " cores selected. The phone may overheat.";
        return threads + " of " + cores + " cores. One is left free so the phone can breathe.";
    }
}
