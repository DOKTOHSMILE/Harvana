package com.harvana.app.ui;

import android.content.Context;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.harvana.app.utils.UiColors;

/**
 * "Working..." as a bottom sheet: a spinner and one line of text, nothing to tap. Replaces ProgressDialog with the same calls
 * (show, setMessage, setCancelable, dismiss, isShowing). It cannot be swiped away unless it is made cancelable.
 */
public class ProgressSheet extends AppSheet {
    private final TextView text;

    public ProgressSheet(Context context) {
        super(context);
        float dp = context.getResources().getDisplayMetrics().density;
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setGravity(Gravity.CENTER_VERTICAL);
        int pad = (int) (24 * dp);
        root.setPadding(pad, (int) (28 * dp), pad, (int) (32 * dp));
        CircularProgressIndicator spin = new CircularProgressIndicator(context);
        spin.setIndeterminate(true);
        spin.setIndicatorColor(UiColors.accent());
        spin.setIndicatorSize((int) (32 * dp));
        spin.setTrackThickness((int) (3 * dp));
        root.addView(spin, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        text = new TextView(context);
        text.setTextSize(15.5f);
        text.setTextColor(UiColors.textPrimary());
        text.setPadding((int) (18 * dp), 0, 0, 0);
        root.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        setContentView(root);
        setCancelable(false);
        setCanceledOnTouchOutside(false);
    }

    /** Same shape as ProgressDialog.show(context, title, message, indeterminate, cancelable). */
    public static ProgressSheet show(Context context, CharSequence title, CharSequence message, boolean indeterminate, boolean cancelable) {
        ProgressSheet p = new ProgressSheet(context);
        p.setMessage(message);
        p.setCancelable(cancelable);
        p.setCanceledOnTouchOutside(cancelable);
        p.show();
        return p;
    }

    public void setMessage(CharSequence message) { text.setText(message == null ? "" : message); }
}
