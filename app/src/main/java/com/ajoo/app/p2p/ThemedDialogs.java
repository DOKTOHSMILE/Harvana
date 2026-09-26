package com.ajoo.app.p2p;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.ajoo.app.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.textview.MaterialTextView;

/**
 * Every AlertDialog / BottomSheetDialog in P2P (and anywhere else that
 * used the raw builders) must go through here. Night AppTheme used to
 * map colorPrimary to colorSurface, so Material dialog BUTTON text was
 * the same colour as the sheet — Confirm / Cancel / I've Paid vanished.
 *
 * Background and on-surface text are resolved from the same overlay
 * theme so they cannot drift apart when the user flips day/night.
 */
public final class ThemedDialogs {

	private ThemedDialogs() {}

	public static AlertDialog.Builder alert(Context context) {
		TypedValue tv = new TypedValue();
		int theme = R.style.AjooAlertDialog;
		if (context.getTheme().resolveAttribute(androidx.appcompat.R.attr.alertDialogTheme, tv, true) && tv.resourceId != 0) {
			theme = tv.resourceId;
		}
		return new AlertDialog.Builder(context, theme) {
			@Override
			public AlertDialog show() {
				AlertDialog dialog = create();
				dialog.setOnShowListener(d -> paintButtons(dialog));
				dialog.show();
				return dialog;
			}
		};
	}

	public static BottomSheetDialog sheet(Context context) {
		TypedValue tv = new TypedValue();
		int theme = R.style.AjooBottomSheetDialog;
		if (context.getTheme().resolveAttribute(com.google.android.material.R.attr.bottomSheetDialogTheme, tv, true)
			&& tv.resourceId != 0) {
			theme = tv.resourceId;
		}
		return new BottomSheetDialog(context, theme);
	}

	public static int surface(Context context) {
		return MaterialColors.getColor(context, com.google.android.material.R.attr.colorSurface,
			context.getResources().getColor(R.color.colorSurface));
	}

	public static int onSurface(Context context) {
		return MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurface,
			context.getResources().getColor(R.color.colorTextPrimary));
	}

	public static int onSurfaceVariant(Context context) {
		return MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurfaceVariant,
			context.getResources().getColor(R.color.colorTextSecondary));
	}

	public static int accent(Context context) {
		return MaterialColors.getColor(context, androidx.appcompat.R.attr.colorAccent,
			context.getResources().getColor(R.color.colorP2pAccent));
	}

	public static int onAccent(Context context) {
		return MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnPrimary,
			context.getResources().getColor(R.color.colorP2pOnAccent));
	}

	public static LinearLayout sheetRoot(Context context) {
		LinearLayout root = new LinearLayout(context);
		root.setOrientation(LinearLayout.VERTICAL);
		int pad = dp(context, 20);
		root.setPadding(pad, pad, pad, pad);
		float radius = dp(context, 20);
		GradientDrawable bg = new GradientDrawable();
		bg.setShape(GradientDrawable.RECTANGLE);
		bg.setColor(surface(context));
		bg.setCornerRadii(new float[]{radius, radius, radius, radius, 0f, 0f, 0f, 0f});
		root.setBackground(bg);
		root.setLayoutParams(new LinearLayout.LayoutParams(
			LinearLayout.LayoutParams.MATCH_PARENT,
			LinearLayout.LayoutParams.WRAP_CONTENT));
		return root;
	}

	public static MaterialTextView sheetTitle(Context context, String text) {
		MaterialTextView tv = new MaterialTextView(context);
		tv.setText(text);
		tv.setTextColor(onSurface(context));
		tv.setTextSize(18f);
		tv.setTypeface(tv.getTypeface(), Typeface.BOLD);
		tv.setPadding(0, 0, 0, dp(context, 16));
		return tv;
	}

	public static MaterialTextView sheetLabel(Context context, String text) {
		MaterialTextView tv = new MaterialTextView(context);
		tv.setText(text);
		tv.setTextColor(onSurfaceVariant(context));
		tv.setTextSize(13f);
		tv.setPadding(0, dp(context, 8), 0, dp(context, 8));
		return tv;
	}

	public static LinearLayout sheetRow(Context context) {
		LinearLayout row = new LinearLayout(context);
		row.setOrientation(LinearLayout.HORIZONTAL);
		row.setGravity(Gravity.CENTER_VERTICAL);
		row.setPadding(0, dp(context, 12), 0, dp(context, 12));
		row.setClickable(true);
		row.setFocusable(true);
		row.setMinimumHeight(dp(context, 48));
		return row;
	}

	public static LinearLayout sheetButtonRow(Context context) {
		LinearLayout row = new LinearLayout(context);
		row.setOrientation(LinearLayout.HORIZONTAL);
		row.setPadding(0, dp(context, 20), 0, 0);
		row.setWeightSum(2f);
		return row;
	}

	public static MaterialButton outlineButton(Context context, String text) {
		MaterialButton b = new MaterialButton(context, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
		b.setText(text);
		b.setTextColor(accent(context));
		b.setStrokeColor(ColorStateList.valueOf(accent(context)));
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
		lp.setMarginEnd(dp(context, 8));
		b.setLayoutParams(lp);
		b.setMinHeight(dp(context, 48));
		return b;
	}

	public static MaterialButton filledButton(Context context, String text) {
		MaterialButton b = new MaterialButton(context, null, com.google.android.material.R.attr.materialButtonStyle);
		b.setText(text);
		b.setBackgroundTintList(ColorStateList.valueOf(accent(context)));
		b.setTextColor(onAccent(context));
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
		lp.setMarginStart(dp(context, 8));
		b.setLayoutParams(lp);
		b.setMinHeight(dp(context, 48));
		return b;
	}

	public static View spacer(Context context) {
		View v = new View(context);
		v.setLayoutParams(new LinearLayout.LayoutParams(0, 0, 1f));
		return v;
	}

	public static int dp(Context context, int dp) {
		return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
			context.getResources().getDisplayMetrics()));
	}

	public static void paintInput(EditText input) {
		if (input == null) return;
		Context context = input.getContext();
		input.setTextColor(onSurface(context));
		input.setHintTextColor(onSurfaceVariant(context));
	}

	/** Force AlertDialog action buttons to the accent colour after show() —
	 *  some OEM overlays still ignore buttonBarPositiveButtonStyle. */
	public static void paintButtons(AlertDialog dialog) {
		if (dialog == null) return;
		int accent = accent(dialog.getContext());
		if (dialog.getButton(AlertDialog.BUTTON_POSITIVE) != null) {
			dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(accent);
		}
		if (dialog.getButton(AlertDialog.BUTTON_NEGATIVE) != null) {
			dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(accent);
		}
		if (dialog.getButton(AlertDialog.BUTTON_NEUTRAL) != null) {
			dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(accent);
		}
	}

	public static AlertDialog show(AlertDialog.Builder builder) {
		AlertDialog dialog = builder.create();
		dialog.setOnShowListener(d -> paintButtons(dialog));
		dialog.show();
		return dialog;
	}
}
