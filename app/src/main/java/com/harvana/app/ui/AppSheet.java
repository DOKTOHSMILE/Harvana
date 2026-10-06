package com.harvana.app.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.ShapeAppearanceModel;
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
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        FrameLayout sheet = findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (sheet != null) {
            float r = CORNER_DP * getContext().getResources().getDisplayMetrics().density;
            ShapeAppearanceModel shape = ShapeAppearanceModel.builder().setTopLeftCornerSize(r).setTopRightCornerSize(r).build();
            MaterialShapeDrawable bg = new MaterialShapeDrawable(shape);
            bg.setFillColor(ColorStateList.valueOf(UiColors.surface()));
            sheet.setBackground(bg);
            sheet.setClipToOutline(true);                       // content with its own square background is clipped to the curve
        }
        BottomSheetBehavior<FrameLayout> b = getBehavior();
        b.setSkipCollapsed(true);
        b.setState(BottomSheetBehavior.STATE_EXPANDED);
        focusFirstField();
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
