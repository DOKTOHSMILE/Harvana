package com.harvana.app.ui;

import android.app.Activity;
import android.content.Context;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.harvana.app.R;
import com.harvana.app.utils.UiColors;

/**
 * The one full-screen search sheet. A single row, side by side: a close button, the search field, and a clear button.
 * Results follow the typing (the screen behind is filtered as you type); close keeps what was typed, clear empties the field.
 */
public final class SearchSheet {
    private SearchSheet() {}

    public interface Listener {
        /** Called on every change with the trimmed text (empty when cleared). */
        void onQuery(String query);
        /** One line under the field, e.g. "3 groups match". May return "". */
        String summary();
    }

    public static void show(final Activity a, String title, String hint, String current, final Listener l) {
        final BottomSheetDialog dlg = new AppSheet(a);
        final float d = a.getResources().getDisplayMetrics().density;
        final int pad = (int) (16 * d);

        LinearLayout root = new LinearLayout(a);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(UiColors.surface());
        root.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        TextView heading = new TextView(a);
        heading.setText(title);
        heading.setTextSize(18);
        heading.setTypeface(null, android.graphics.Typeface.BOLD);
        heading.setTextColor(UiColors.textPrimary());
        heading.setPadding(0, 0, 0, (int) (10 * d));
        root.addView(heading);

        LinearLayout row = new LinearLayout(a);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        final MaterialButton close = iconButton(a, R.drawable.ic_close, "Close search");
        final EditText input = new com.google.android.material.textfield.TextInputEditText(a);
        input.setHint(hint);
        input.setText(current == null ? "" : current);
        input.setSelection(input.getText().length());
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        final MaterialButton clear = iconButton(a, R.drawable.ic_clear_text, "Clear");

        row.addView(close, new LinearLayout.LayoutParams((int) (44 * d), (int) (44 * d)));
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        ip.setMargins((int) (6 * d), 0, (int) (6 * d), 0);
        row.addView(input, ip);
        row.addView(clear, new LinearLayout.LayoutParams((int) (44 * d), (int) (44 * d)));
        root.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final TextView summary = new TextView(a);
        summary.setTextSize(13);
        summary.setTextColor(UiColors.textSecondary());
        summary.setPadding((int) (6 * d), (int) (14 * d), 0, 0);
        root.addView(summary);

        final Runnable paintSummary = () -> {
            String s = l.summary();
            summary.setText(s == null ? "" : s);
            clear.setVisibility(input.getText().length() == 0 ? View.INVISIBLE : View.VISIBLE);
        };
        paintSummary.run();

        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int af) { }
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) { }
            @Override public void afterTextChanged(Editable s) {
                l.onQuery(s.toString().trim());
                paintSummary.run();
            }
        });
        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) { dlg.dismiss(); return true; }
            return false;
        });
        close.setOnClickListener(v -> dlg.dismiss());
        clear.setOnClickListener(v -> { input.setText(""); input.requestFocus(); });

        dlg.setContentView(root);
        dlg.setOnShowListener(x -> {
            FrameLayout sheet = dlg.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (sheet != null) {
                BottomSheetBehavior<View> b = BottomSheetBehavior.from(sheet);
                ViewGroup.LayoutParams lp = sheet.getLayoutParams();
                if (lp != null) { lp.height = a.getResources().getDisplayMetrics().heightPixels; sheet.setLayoutParams(lp); }
                b.setSkipCollapsed(true);
                b.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
            input.requestFocus();
            InputMethodManager imm = (InputMethodManager) a.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
        });
        dlg.show();
    }

    private static MaterialButton iconButton(Context c, int icon, String description) {
        MaterialButton b = new MaterialButton(c, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        b.setIconResource(icon);
        b.setIconTint(android.content.res.ColorStateList.valueOf(UiColors.textPrimary()));
        b.setIconSize((int) (20 * c.getResources().getDisplayMetrics().density));
        b.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
        b.setIconPadding(0);
        b.setText("");
        b.setInsetTop(0); b.setInsetBottom(0);
        b.setMinWidth(0); b.setMinimumWidth(0); b.setMinHeight(0); b.setMinimumHeight(0);
        b.setPadding(0, 0, 0, 0);
        b.setContentDescription(description);
        return b;
    }
}
