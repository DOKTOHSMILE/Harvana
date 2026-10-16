package com.harvana.app.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.harvana.app.utils.UiColors;

/**
 * A bottom sheet that does the job of an alert dialog, with the same builder calls: title, message, a custom view, a list of items,
 * and up to three buttons. It rises from the bottom with rounded corners and sits above the keyboard. Replace
 * {@code new AlertDialog.Builder(ctx)} with {@code new SheetDialog.Builder(ctx)}; nothing else in the call changes.
 */
public class SheetDialog extends AppSheet {

    public static final int BUTTON_POSITIVE = DialogInterface.BUTTON_POSITIVE;
    public static final int BUTTON_NEGATIVE = DialogInterface.BUTTON_NEGATIVE;
    public static final int BUTTON_NEUTRAL = DialogInterface.BUTTON_NEUTRAL;

    private final Button[] buttons = new Button[3];

    SheetDialog(Context context) { super(context); }

    /** The real button, so a caller can replace its click listener (to keep the sheet open while it checks something). */
    public Button getButton(int which) {
        switch (which) {
            case BUTTON_POSITIVE: return buttons[0];
            case BUTTON_NEGATIVE: return buttons[1];
            case BUTTON_NEUTRAL: return buttons[2];
            default: return null;
        }
    }

    /** The thin line between list rows in every sheet. */
    public static View divider(Context c) {
        View line = new View(c);
        line.setBackgroundColor((UiColors.textTertiary() & 0x00FFFFFF) | 0x33000000);
        line.setTag(com.harvana.app.R.id.sheet_divider_tag, "sheet-divider");
        return line;
    }

    public static LinearLayout.LayoutParams dividerParams(Context c) {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, Math.round(c.getResources().getDisplayMetrics().density * 0.8f)));
    }

    /** Adds rows to a sheet's column as a list: a divider between each, like every sheet list. */
    public static void list(LinearLayout column, View... rows) {
        for (View r : rows) {
            if (column.getChildCount() > 0) column.addView(divider(column.getContext()), dividerParams(column.getContext()));
            column.addView(r);
        }
    }

    public static class Builder {
        private final Context context;
        private CharSequence title, message;
        private View view;
        private CharSequence[] items;
        private int checked = -1;
        private DialogInterface.OnClickListener itemListener;
        private boolean singleChoice;
        private boolean[] multiChecked;
        private DialogInterface.OnMultiChoiceClickListener multiListener;
        private CharSequence positive, negative, neutral;
        private DialogInterface.OnClickListener positiveL, negativeL, neutralL;
        private boolean cancelable = true;
        private DialogInterface.OnDismissListener dismissL;
        private DialogInterface.OnCancelListener cancelL;

        public Builder(Context context) { this.context = context; }
        public Builder(Context context, int themeResId) { this.context = context; }

        public Context getContext() { return context; }

        public Builder setTitle(CharSequence t) { title = t; return this; }
        public Builder setTitle(int res) { title = context.getText(res); return this; }
        public Builder setMessage(CharSequence m) { message = m; return this; }
        public Builder setMessage(int res) { message = context.getText(res); return this; }
        public Builder setView(View v) { view = v; return this; }
        public Builder setView(int layoutRes) { view = android.view.LayoutInflater.from(context).inflate(layoutRes, null); return this; }
        public Builder setIcon(int res) { return this; }
        public Builder setIcon(Drawable d) { return this; }
        public Builder setCancelable(boolean c) { cancelable = c; return this; }
        public Builder setOnDismissListener(DialogInterface.OnDismissListener l) { dismissL = l; return this; }
        public Builder setOnCancelListener(DialogInterface.OnCancelListener l) { cancelL = l; return this; }

        public Builder setItems(CharSequence[] list, DialogInterface.OnClickListener l) { items = list; itemListener = l; singleChoice = false; return this; }
        public Builder setItems(int arrayRes, DialogInterface.OnClickListener l) { return setItems(context.getResources().getTextArray(arrayRes), l); }
        public Builder setSingleChoiceItems(CharSequence[] list, int checkedItem, DialogInterface.OnClickListener l) {
            items = list; checked = checkedItem; itemListener = l; singleChoice = true; return this;
        }
        public Builder setSingleChoiceItems(int arrayRes, int checkedItem, DialogInterface.OnClickListener l) {
            return setSingleChoiceItems(context.getResources().getTextArray(arrayRes), checkedItem, l);
        }

        public Builder setMultiChoiceItems(CharSequence[] list, boolean[] checkedItems, DialogInterface.OnMultiChoiceClickListener l) {
            items = list; multiChecked = checkedItems == null ? new boolean[list.length] : checkedItems; multiListener = l; singleChoice = false; return this;
        }

        public Builder setPositiveButton(CharSequence t, DialogInterface.OnClickListener l) { positive = t; positiveL = l; return this; }
        public Builder setPositiveButton(int res, DialogInterface.OnClickListener l) { return setPositiveButton(context.getText(res), l); }
        public Builder setNegativeButton(CharSequence t, DialogInterface.OnClickListener l) { negative = t; negativeL = l; return this; }
        public Builder setNegativeButton(int res, DialogInterface.OnClickListener l) { return setNegativeButton(context.getText(res), l); }
        public Builder setNeutralButton(CharSequence t, DialogInterface.OnClickListener l) { neutral = t; neutralL = l; return this; }
        public Builder setNeutralButton(int res, DialogInterface.OnClickListener l) { return setNeutralButton(context.getText(res), l); }

        public SheetDialog create() {
            final SheetDialog d = new SheetDialog(context);
            final float dp = context.getResources().getDisplayMetrics().density;
            final int pad = (int) (20 * dp);

            LinearLayout root = new LinearLayout(context);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setPadding(pad, (int) (10 * dp), pad, pad);

            View handle = new View(context);                                  // the small grab bar at the top of every sheet
            GradientDrawable pill = new GradientDrawable();
            pill.setCornerRadius(4 * dp);
            pill.setColor((UiColors.textTertiary() & 0x00FFFFFF) | 0x66000000);
            handle.setBackground(pill);
            LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams((int) (40 * dp), (int) (4 * dp));
            hp.gravity = Gravity.CENTER_HORIZONTAL;
            hp.bottomMargin = (int) (14 * dp);
            root.addView(handle, hp);

            if (title != null && title.length() > 0) {
                TextView t = new TextView(context);
                t.setText(title);
                t.setTextSize(19);
                t.setTypeface(null, Typeface.BOLD);
                t.setTextColor(UiColors.textPrimary());
                t.setPadding(0, 0, 0, (int) (8 * dp));
                root.addView(t);
            }
            if (message != null && message.length() > 0) {
                TextView m = new TextView(context);
                m.setText(message);
                m.setTextSize(14.5f);
                m.setTextColor(UiColors.textSecondary());
                m.setLineSpacing(0, 1.12f);
                m.setPadding(0, 0, 0, (int) (12 * dp));
                root.addView(m);
            }
            if (view != null) {
                if (view.getParent() instanceof ViewGroup) ((ViewGroup) view.getParent()).removeView(view);
                root.addView(view, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            }
            if (items != null) {
                final CheckedTextView[] rows = new CheckedTextView[items.length];
                for (int i = 0; i < items.length; i++) {
                    final int at = i;
                    CheckedTextView row = new CheckedTextView(context);
                    row.setText(items[i]);
                    row.setTextSize(16);
                    row.setTextColor(UiColors.textPrimary());
                    row.setMinHeight((int) (54 * dp));
                    row.setGravity(Gravity.CENTER_VERTICAL);
                    row.setPadding(0, (int) (6 * dp), 0, (int) (6 * dp));
                    android.util.TypedValue ripple = new android.util.TypedValue();
                    context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, ripple, true);
                    row.setBackgroundResource(ripple.resourceId);
                    if (singleChoice || multiChecked != null) {
                        row.setCheckMarkDrawable(com.harvana.app.R.drawable.sheet_checkmark);
                        if (row.getCheckMarkDrawable() != null) row.getCheckMarkDrawable().mutate().setTint(UiColors.accent());
                        row.setChecked(singleChoice ? i == checked : multiChecked[i]);
                        if (singleChoice) rows[i] = row;
                    }
                    row.setOnClickListener(v -> {
                        if (multiChecked != null) {
                            multiChecked[at] = !multiChecked[at];
                            ((CheckedTextView) v).setChecked(multiChecked[at]);
                            if (multiListener != null) multiListener.onClick(d, at, multiChecked[at]);
                            return;                                              // checkboxes stay open until a button is pressed
                        }
                        if (singleChoice) {
                            for (CheckedTextView r : rows) if (r != null) r.setChecked(r == v);
                        }
                        if (itemListener != null) itemListener.onClick(d, at);
                        if (!singleChoice) d.dismiss();                         // a plain list closes when a row is chosen
                    });
                    if (i > 0) root.addView(divider(context), dividerParams(context));   // a list, one line between rows
                    root.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (54 * dp)));
                }
            }
            if (positive != null || negative != null || neutral != null) {
                LinearLayout bar = new LinearLayout(context);
                bar.setOrientation(LinearLayout.HORIZONTAL);
                bar.setGravity(Gravity.CENTER_VERTICAL);
                LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                bl.topMargin = (int) (16 * dp);
                int gap = (int) (4 * dp);
                if (neutral != null) {
                    d.buttons[2] = button(context, d, neutral, neutralL, DialogInterface.BUTTON_NEUTRAL, false, dp);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                    lp.setMargins(gap, 0, gap, 0);
                    bar.addView(d.buttons[2], lp);
                }
                if (negative != null) {
                    d.buttons[1] = button(context, d, negative, negativeL, DialogInterface.BUTTON_NEGATIVE, false, dp);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                    lp.setMargins(gap, 0, gap, 0);
                    bar.addView(d.buttons[1], lp);
                }
                if (positive != null) {
                    d.buttons[0] = button(context, d, positive, positiveL, DialogInterface.BUTTON_POSITIVE, true, dp);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, (negative != null || neutral != null) ? 1.4f : 1f);
                    lp.setMargins(gap, 0, gap, 0);
                    bar.addView(d.buttons[0], lp);
                }
                root.addView(bar, bl);
            }

            ScrollView scroll = new ScrollView(context);                       // tall content scrolls; the sheet shrinks above the keyboard
            scroll.setFillViewport(false);
            scroll.addView(root, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            d.setContentView(scroll);
            d.setCancelable(cancelable);
            d.setCanceledOnTouchOutside(cancelable);
            if (dismissL != null) d.setOnDismissListener(dismissL);
            if (cancelL != null) d.setOnCancelListener(cancelL);
            return d;
        }

        public SheetDialog show() {
            SheetDialog d = create();
            d.show();
            return d;
        }

        /** The same two button looks the rest of the app uses: filled accent for the main action, outlined accent for the others. */
        private static Button button(Context c, final SheetDialog d, CharSequence text, final DialogInterface.OnClickListener l,
                                     final int which, boolean main, float dp) {
            MaterialButton b = main
                    ? new MaterialButton(c)
                    : new MaterialButton(c, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
            b.setText(text);
            b.setAllCaps(false);
            b.setTextSize(15);
            b.setTypeface(null, Typeface.BOLD);
            b.setSingleLine(false);
            b.setMaxLines(2);
            b.setGravity(Gravity.CENTER);
            b.setInsetTop(0);
            b.setInsetBottom(0);
            b.setMinHeight((int) (48 * dp));
            b.setMinimumHeight((int) (48 * dp));
            b.setCornerRadius((int) (12 * dp));
            b.setPadding((int) (14 * dp), 0, (int) (14 * dp), 0);
            if (main) {
                b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(UiColors.accent()));
                b.setTextColor(UiColors.onAccent());
            } else {
                b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT));
                b.setTextColor(UiColors.accent());
                b.setStrokeColor(android.content.res.ColorStateList.valueOf(UiColors.accent()));
                b.setStrokeWidth((int) (1.5f * dp));
            }
            b.setOnClickListener(v -> {
                if (l != null) l.onClick(d, which);
                d.dismiss();                                                  // like an alert dialog, a button closes it
            });
            return b;
        }
    }
}
