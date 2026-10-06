package com.harvana.app.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.harvana.app.utils.UiColors;

import java.util.Calendar;

/** Pick a date in a bottom sheet instead of a pop-up: a spinner-style date wheel and one "Set date" button. */
public final class DateSheet {
    private DateSheet() {}

    public interface OnDate { void onDate(int year, int month0, int day); }

    /** {@code maxNow} stops a date in the future (a date of birth). */
    public static void show(Context context, String title, boolean maxNow, final OnDate cb) {
        final float dp = context.getResources().getDisplayMetrics().density;
        final AppSheet sheet = new AppSheet(context);
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (20 * dp);
        root.setPadding(pad, (int) (12 * dp), pad, pad);
        View handle = new View(context);
        android.graphics.drawable.GradientDrawable pill = new android.graphics.drawable.GradientDrawable();
        pill.setCornerRadius(4 * dp);
        pill.setColor((UiColors.textTertiary() & 0x00FFFFFF) | 0x66000000);
        handle.setBackground(pill);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams((int) (40 * dp), (int) (4 * dp));
        hp.gravity = android.view.Gravity.CENTER_HORIZONTAL;
        hp.bottomMargin = (int) (14 * dp);
        root.addView(handle, hp);
        TextView t = new TextView(context);
        t.setText(title);
        t.setTextSize(19);
        t.setTypeface(null, Typeface.BOLD);
        t.setTextColor(UiColors.textPrimary());
        root.addView(t);
        final DatePicker picker = new DatePicker(new android.view.ContextThemeWrapper(context, android.R.style.Theme_Material_Dialog));
        picker.setSpinnersShown(true);
        picker.setCalendarViewShown(false);
        if (maxNow) picker.setMaxDate(System.currentTimeMillis());
        Calendar c = Calendar.getInstance();
        if (maxNow) c.add(Calendar.YEAR, -18);                           // opens near a sensible adult birth year
        picker.updateDate(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        LinearLayout.LayoutParams pl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pl.gravity = android.view.Gravity.CENTER_HORIZONTAL;
        root.addView(picker, pl);
        MaterialButton set = new MaterialButton(context);
        set.setText("Set date");
        set.setAllCaps(false);
        set.setBackgroundTintList(android.content.res.ColorStateList.valueOf(UiColors.accent()));
        set.setTextColor(UiColors.onAccent());
        set.setOnClickListener(v -> {
            cb.onDate(picker.getYear(), picker.getMonth(), picker.getDayOfMonth());
            sheet.dismiss();
        });
        LinearLayout.LayoutParams sl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (50 * dp));
        sl.topMargin = (int) (12 * dp);
        root.addView(set, sl);
        sheet.setContentView(root);
        sheet.show();
    }
}
