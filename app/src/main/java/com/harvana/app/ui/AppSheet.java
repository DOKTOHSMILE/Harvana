package com.harvana.app.ui;

import android.content.Context;
import android.os.Bundle;
import android.graphics.Outline;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.harvana.app.utils.UiColors;

/**
 * The one bottom sheet every screen uses: Material, rounded top corners, opened fully, and resized above the keyboard so a text
 * field in it is never hidden and can always be typed into. Use it instead of a plain BottomSheetDialog, and use SheetDialog
 * where an alert dialog used to be.
 */
public class AppSheet extends BottomSheetDialog {
    private static final float CORNER_DP = 24f;

    public AppSheet(Context context) { super(context); }

    public AppSheet(Context context, int theme) { super(context, theme); }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getWindow() != null) {
            // The sheet shrinks above the keyboard instead of sliding under it.
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            paintNavigationBar();
        }
    }

    private GradientDrawable fill;

    /** The strip under the sheet (the gesture or button bar) takes the sheet's own colour, so the sheet reaches the bottom of the screen. */
    private void paintNavigationBar() {
        android.view.Window w = getWindow();
        if (w == null) return;
        int surface = com.google.android.material.color.MaterialColors.getColor(getContext(),
                com.google.android.material.R.attr.colorSurface, UiColors.surface());
        w.setNavigationBarColor(surface);
        boolean light = androidx.core.graphics.ColorUtils.calculateLuminance(surface) > 0.5;
        androidx.core.view.WindowCompat.getInsetsController(w, w.getDecorView()).setAppearanceLightNavigationBars(light);
    }

    @Override
    protected void onStart() {
        super.onStart();
        com.harvana.app.utils.SmartCaps.attach(getWindow());
        com.harvana.app.utils.SmartSpacing.attach(getWindow());
        final FrameLayout sheet = findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (sheet != null) {
            style(sheet);
            // Material and some screens replace the sheet's background after it is laid out (to square, transparent or an elevation-tinted
            // grey). Put the rounded surface back every time, so no sheet is ever left with flat edges.
            sheet.addOnLayoutChangeListener((v, l, t, r, bt, ol, ot, or, ob) -> style(sheet));
        }
        BottomSheetBehavior<FrameLayout> b = getBehavior();
        b.setSkipCollapsed(true);
        b.setState(BottomSheetBehavior.STATE_EXPANDED);
        View content = findViewById(android.R.id.content);
        if (content != null) {
            dividers(content);
            final int surface = surfaceColor();
            readable(content, surface);
            // Rows that arrive later (search results, loaded lists) are checked too.
            content.getViewTreeObserver().addOnGlobalLayoutListener(() -> readable(content, surface));
        }
        focusFirstField();
    }

    private static final String DIVIDER = "sheet-divider";

    /**
     * Every list in a sheet reads like a list: a thin line between rows that sit next to each other in a column and can be
     * tapped. Buttons, text fields, chips and switches on their own are not rows, and are left as they are.
     */
    static void dividers(View v) {
        if (!(v instanceof ViewGroup)) return;
        ViewGroup g = (ViewGroup) v;
        for (int i = 0; i < g.getChildCount(); i++) dividers(g.getChildAt(i));
        if (!(g instanceof android.widget.LinearLayout) || ((android.widget.LinearLayout) g).getOrientation() != android.widget.LinearLayout.VERTICAL) return;
        for (int i = g.getChildCount() - 1; i > 0; i--) {
            View a = g.getChildAt(i - 1), b = g.getChildAt(i);
            if (row(a) && row(b)) g.addView(SheetDialog.divider(g.getContext()), i, SheetDialog.dividerParams(g.getContext()));
        }
    }

    private int surfaceColor() {
        return com.google.android.material.color.MaterialColors.getColor(getContext(),
                com.google.android.material.R.attr.colorSurface, UiColors.surface());
    }

    /** Text on the sheet always reads: dark on a light sheet, light on a dark one. Text that would blend in takes the main text colour. */
    static void readable(View v, int surface) {
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) readable(g.getChildAt(i), surface);
            return;
        }
        if (!(v instanceof android.widget.TextView) || v instanceof EditText || v instanceof android.widget.Button) return;
        if (v.getBackground() instanceof android.graphics.drawable.ColorDrawable
                || v.getBackground() instanceof GradientDrawable) return;   // a chip or badge paints its own fill
        android.widget.TextView t = (android.widget.TextView) v;
        int fg = androidx.core.graphics.ColorUtils.compositeColors(t.getCurrentTextColor(), surface | 0xFF000000);
        if (androidx.core.graphics.ColorUtils.calculateContrast(fg, surface | 0xFF000000) < 3.0) t.setTextColor(UiColors.textPrimary());
    }

    private static boolean row(View v) {
        if (v == null || v.getVisibility() != View.VISIBLE || DIVIDER.equals(v.getTag(com.harvana.app.R.id.sheet_divider_tag))) return false;
        if (v instanceof android.widget.Button || v instanceof EditText || v instanceof android.widget.CompoundButton
                || v instanceof com.google.android.material.chip.ChipGroup) return false;
        if (v.getHeight() > 0 && v.getHeight() > (int) (120 * v.getResources().getDisplayMetrics().density)) return false;   // a card, not a row
        return v.isClickable() || v.hasOnClickListeners();
    }

    /** Rounded top corners on the sheet itself, and everything inside is clipped to the curve, whatever background it paints. */
    private void style(FrameLayout sheet) {
        final float r = CORNER_DP * getContext().getResources().getDisplayMetrics().density;
        if (fill == null) {
            fill = new GradientDrawable();
            fill.setShape(GradientDrawable.RECTANGLE);
            fill.setCornerRadii(new float[]{r, r, r, r, 0f, 0f, 0f, 0f});
            fill.setColor(com.google.android.material.color.MaterialColors.getColor(getContext(),
                    com.google.android.material.R.attr.colorSurface, UiColors.surface()));
        }
        if (sheet.getBackground() != fill) sheet.setBackground(fill);
        if (!sheet.getClipToOutline()) {
            sheet.setOutlineProvider(new ViewOutlineProvider() {
                @Override public void getOutline(View view, Outline outline) {
                    // a rounded rectangle taller than the sheet: only the top corners show
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight() + (int) r, r);
                }
            });
            sheet.setClipToOutline(true);
        }
    }

    /** A sheet with a text field opens with the cursor in the first one and the keyboard up. */
    private void focusFirstField() {
        View content = findViewById(android.R.id.content);
        final EditText first = firstEdit(content);
        if (first == null) return;
        first.post(() -> {
            if (!first.isEnabled()) return;
            first.requestFocus();
            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(first, InputMethodManager.SHOW_IMPLICIT);
        });
    }

    private static EditText firstEdit(View v) {
        if (v instanceof EditText) return (EditText) v;
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                EditText e = firstEdit(g.getChildAt(i));
                if (e != null) return e;
            }
        }
        return null;
    }
}
