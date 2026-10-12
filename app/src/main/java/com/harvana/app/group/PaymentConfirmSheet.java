package com.harvana.app.group;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.harvana.app.R;
import com.harvana.app.wallet.PinPromptHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Receipt shown before any payment. Same shape as a completed transfer:
 * the amount, what it is for, the detail rows, and the steps still ahead.
 * Verify is what asks for the PIN. Cancel sends nothing.
 */
public final class PaymentConfirmSheet {

	public interface OnConfirm {
		void onConfirmed();
	}

	private PaymentConfirmSheet() {}

	public static void show(Context context, String title, double amountHarv,
	                         String recipientAddress, String memo, String feeDisplay,
	                         OnConfirm onConfirm) {
		if (context == null) return;
		List<String[]> rows = new ArrayList<>();
		if (recipientAddress != null && !recipientAddress.trim().isEmpty()) {
			rows.add(new String[]{"To", recipientAddress.trim()});
		}
		if (memo != null && !memo.trim().isEmpty()) {
			rows.add(new String[]{"For", memo.trim()});
		}
		rows.add(new String[]{"Transport fare", feeDisplay != null && !feeDisplay.trim().isEmpty()
				? feeDisplay.trim() : "Taken when this is sent"});
		present(context, title, com.harvana.app.wallet.BalanceFormatter.smart(amountHarv), rows, false,
				() -> { if (onConfirm != null) onConfirm.onConfirmed(); });
	}

	public static void reviewThenAuthorize(Context context, String title, String amountText,
	                                        List<String[]> rows, Runnable onAuthorized) {
		present(context, title, amountText, rows, true, onAuthorized);
	}

	private static void present(Context context, String title, String amountText,
	                            List<String[]> rows, boolean thenPin, Runnable after) {
		if (context == null) return;
		String heading = title == null || title.trim().isEmpty() ? "Review this payment" : title.trim();
		String amount = amountText == null ? "" : amountText.trim();
		boolean hideAmount = amount.isEmpty() || "—".equals(amount);

		String purpose = null;
		List<String[]> facts = new ArrayList<>();
		if (rows != null) {
			for (String[] row : rows) {
				if (row == null || row.length < 2 || row[0] == null) continue;
				String label = row[0].trim();
				String value = row[1] == null ? "" : row[1].trim();
				if (value.isEmpty()) value = "—";
				if (purpose == null && isPurpose(label)) purpose = value;
				else facts.add(new String[]{label, _present(value)});
			}
		}
		if (purpose == null) purpose = hideAmount ? heading : heading;

		int surface = color(context, R.color.colorSurface);
		int primary = color(context, R.color.colorTextPrimary);
		int secondary = color(context, R.color.colorTextSecondary);
		int tertiary = color(context, R.color.colorTextTertiary);
		int accent = color(context, R.color.colorAccent);
		int onAccent = color(context, R.color.colorTextOnAccent);
		int divider = color(context, R.color.colorDivider);
		int success = color(context, R.color.colorSuccess);
		int warning = color(context, R.color.colorWarning);
		float d = context.getResources().getDisplayMetrics().density;

		LinearLayout root = new LinearLayout(context);
		root.setOrientation(LinearLayout.VERTICAL);
		root.setPadding((int) (20 * d), (int) (8 * d), (int) (20 * d), (int) (16 * d));

		View handle = new View(context);
		GradientDrawable handleBg = new GradientDrawable();
		handleBg.setCornerRadius(4 * d);
		handleBg.setColor(divider);
		handle.setBackground(handleBg);
		LinearLayout.LayoutParams handleLp = new LinearLayout.LayoutParams((int) (40 * d), (int) (4 * d));
		handleLp.gravity = Gravity.CENTER_HORIZONTAL;
		handleLp.bottomMargin = (int) (18 * d);
		root.addView(handle, handleLp);

		// The app's own send icon (it used to be an arrow character).
		android.widget.ImageView icon = new android.widget.ImageView(context);
		icon.setImageResource(R.drawable.ic_action_send_line);
		icon.setScaleType(android.widget.ImageView.ScaleType.CENTER_INSIDE);
		icon.setPadding((int) (18 * d), (int) (18 * d), (int) (18 * d), (int) (18 * d));
		GradientDrawable circle = new GradientDrawable();
		circle.setShape(GradientDrawable.OVAL);
		circle.setColor(color(context, R.color.colorSurfaceRaised));
		icon.setBackground(circle);
		int iconSize = (int) (64 * d);
		LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(iconSize, iconSize);
		iconLp.gravity = Gravity.CENTER_HORIZONTAL;
		root.addView(icon, iconLp);

		if (!hideAmount) {
			TextView tvAmount = label(context, amount, 32, primary, true);
			tvAmount.setGravity(Gravity.CENTER);
			tvAmount.setPadding(0, (int) (14 * d), 0, 0);
			root.addView(tvAmount);
		}

		TextView tvPurpose = label(context, purpose, 15, secondary, false);
		tvPurpose.setGravity(Gravity.CENTER);
		tvPurpose.setPadding((int) (12 * d), (int) (6 * d), (int) (12 * d), 0);
		root.addView(tvPurpose);

		SimpleDateFormat fmt = new SimpleDateFormat("MMM d, yyyy | h:mma", Locale.US);
		fmt.setTimeZone(TimeZone.getTimeZone("Africa/Lagos"));
		TextView when = label(context, fmt.format(new Date()) + "  ·  Not sent yet", 13, tertiary, false);
		when.setGravity(Gravity.CENTER);
		when.setPadding(0, (int) (4 * d), 0, (int) (16 * d));
		root.addView(when);

		root.addView(dash(context, divider, d));

		TextView section = label(context, "Transaction details", 16, primary, true);
		section.setPadding(0, (int) (18 * d), 0, (int) (8 * d));
		root.addView(section);

		root.addView(statusRow(context, "Waiting for you", warning, d, secondary));

		int preview = Math.min(3, facts.size());
		for (int i = 0; i < preview; i++) root.addView(fact(context, facts.get(i)[0], facts.get(i)[1], secondary, primary, d));

		LinearLayout extra = new LinearLayout(context);
		extra.setOrientation(LinearLayout.VERTICAL);
		extra.setVisibility(View.GONE);
		for (int i = preview; i < facts.size(); i++) extra.addView(fact(context, facts.get(i)[0], facts.get(i)[1], secondary, primary, d));
		if (facts.size() > preview) root.addView(extra);

		if (facts.size() > preview) {
			TextView see = label(context, "See more", 14, accent, true);
			see.setGravity(Gravity.CENTER);
			see.setPadding(0, (int) (12 * d), 0, (int) (4 * d));
			see.setOnClickListener(v -> {
				boolean open = extra.getVisibility() == View.VISIBLE;
				extra.setVisibility(open ? View.GONE : View.VISIBLE);
				see.setText(open ? "See more" : "See less");
			});
			root.addView(see);
		}

		View rule = new View(context);
		rule.setBackgroundColor(divider);
		LinearLayout.LayoutParams ruleLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, (int) d));
		ruleLp.topMargin = (int) (14 * d);
		root.addView(rule, ruleLp);

		TextView timeTitle = label(context, "Timeline", 16, primary, true);
		timeTitle.setPadding(0, (int) (18 * d), 0, (int) (10 * d));
		root.addView(timeTitle);
		root.addView(step(context, true, accent, "Review", "You are reading what this payment is for", d, primary, secondary));
		root.addView(step(context, false, accent, "Verify", thenPin ? "PIN or fingerprint, next" : "You confirm on this screen", d, primary, secondary));
		root.addView(step(context, false, success, "Sent", "Nothing moves until you verify", d, primary, secondary));

		Button go = actionButton(context, thenPin ? "Verify and continue" : "Continue", true, accent, onAccent, d);
		LinearLayout.LayoutParams goLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (52 * d));
		goLp.topMargin = (int) (18 * d);
		root.addView(go, goLp);

		TextView cancel = label(context, "Cancel", 15, secondary, true);
		cancel.setGravity(Gravity.CENTER);
		cancel.setPadding(0, (int) (14 * d), 0, (int) (6 * d));
		root.addView(cancel);

		ScrollView scroll = new ScrollView(context);
		scroll.setFillViewport(true);
		scroll.addView(root, new ScrollView.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		GradientDrawable sheetBg = new GradientDrawable();
		sheetBg.setColor(surface);
		float r = 24 * d;
		sheetBg.setCornerRadii(new float[]{r, r, r, r, 0, 0, 0, 0});
		scroll.setBackground(sheetBg);

		BottomSheetDialog dialog = new com.harvana.app.ui.AppSheet(context);
		dialog.setContentView(scroll);
		dialog.setOnShowListener(shown -> {
			FrameLayout sheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
			if (sheet == null) return;
			sheet.setBackgroundColor(android.graphics.Color.TRANSPARENT);
			BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(sheet);
			behavior.setFitToContents(true);
			behavior.setSkipCollapsed(true);
			behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
			int max = (int) (context.getResources().getDisplayMetrics().heightPixels * 0.92f);
			scroll.post(() -> {
				if (scroll.getHeight() > max) {
					ViewGroup.LayoutParams lp = scroll.getLayoutParams();
					lp.height = max;
					scroll.setLayoutParams(lp);
				}
			});
		});

		cancel.setOnClickListener(v -> dialog.dismiss());
		go.setOnClickListener(v -> {
			dialog.dismiss();
			if (thenPin) {
				PinPromptHelper.authorize(context, _action(heading, hideAmount ? "" : amount), () -> {
					com.harvana.app.tx.PaymentTracker.noteReview(hideAmount ? "" : amount, rows);   // the success sheet and receipt use what the person just approved
					if (after != null) after.run();
				});
			} else if (after != null) {
				after.run();
			}
		});
		dialog.show();
	}

	private static View dash(Context ctx, int color, float d) {
		View v = new View(ctx);
		GradientDrawable g = new GradientDrawable();
		g.setColor(color);
		v.setBackground(g);
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, (int) d));
		v.setLayoutParams(lp);
		return v;
	}

	private static View statusRow(Context ctx, String text, int tone, float d, int labelColor) {
		LinearLayout row = new LinearLayout(ctx);
		row.setOrientation(LinearLayout.HORIZONTAL);
		row.setGravity(Gravity.CENTER_VERTICAL);
		row.setPadding(0, (int) (10 * d), 0, (int) (10 * d));
		TextView left = label(ctx, "Status", 14.5f, labelColor, false);
		TextView pill = label(ctx, "●  " + text, 12.5f, tone, true);
		pill.setPadding((int) (10 * d), (int) (5 * d), (int) (10 * d), (int) (5 * d));
		GradientDrawable bg = new GradientDrawable();
		bg.setCornerRadius(20 * d);
		bg.setColor((tone & 0x00FFFFFF) | 0x26000000);
		pill.setBackground(bg);
		row.addView(left, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
		row.addView(pill);
		return row;
	}

	private static View step(Context ctx, boolean current, int tone, String title, String body, float d, int primary, int secondary) {
		LinearLayout row = new LinearLayout(ctx);
		row.setOrientation(LinearLayout.HORIZONTAL);
		row.setPadding(0, (int) (6 * d), 0, (int) (6 * d));
		TextView dot = label(ctx, current ? "●" : "○", 14, tone, true);
		dot.setGravity(Gravity.CENTER);
		LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams((int) (22 * d), ViewGroup.LayoutParams.WRAP_CONTENT);
		dotLp.rightMargin = (int) (10 * d);
		LinearLayout col = new LinearLayout(ctx);
		col.setOrientation(LinearLayout.VERTICAL);
		col.addView(label(ctx, title, 14.5f, primary, true));
		TextView sub = label(ctx, body, 12.5f, secondary, false);
		sub.setPadding(0, (int) (2 * d), 0, 0);
		col.addView(sub);
		row.addView(dot, dotLp);
		row.addView(col, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
		return row;
	}

	private static boolean isPurpose(String label) {
		String l = label.toLowerCase(Locale.US);
		return l.equals("for") || l.equals("what this pays for") || l.equals("what this is for")
				|| l.equals("you are authorizing") || l.equals("this does") || l.equals("note");
	}

	private static View fact(Context ctx, String name, String value, int labelColor, int valueColor, float d) {
		LinearLayout row = new LinearLayout(ctx);
		row.setOrientation(LinearLayout.HORIZONTAL);
		row.setGravity(Gravity.CENTER_VERTICAL);
		row.setPadding(0, (int) (11 * d), 0, (int) (11 * d));
		TextView left = label(ctx, name, 14.5f, labelColor, false);
		TextView right = label(ctx, value, 14.5f, valueColor, false);
		right.setGravity(Gravity.END);
		row.addView(left, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.9f));
		row.addView(right, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.2f));
		return row;
	}

	private static Button actionButton(Context ctx, String text, boolean primary, int accent, int onAccent, float d) {
		com.google.android.material.button.MaterialButton b = primary ? com.harvana.app.ui.Btn.filled(ctx, text, null) : com.harvana.app.ui.Btn.outlined(ctx, text, null);
		b.setTextSize(15);
		b.setTypeface(Typeface.DEFAULT_BOLD);
		return b;
	}

	private static TextView label(Context ctx, String text, float sp, int color, boolean bold) {
		TextView t = new TextView(ctx);
		t.setText(text);
		t.setTextSize(sp);
		t.setTextColor(color);
		if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
		return t;
	}

	private static int color(Context ctx, int res) {
		return ContextCompat.getColor(ctx, res);
	}

	private static String _action(String title, String amountText) {
		String t = title == null || title.trim().isEmpty() ? "Authorize this payment" : title.trim();
		if (amountText == null || amountText.trim().isEmpty() || "—".equals(amountText.trim())) return t;
		return t + " — " + amountText.trim();
	}

	private static String _present(String value) {
		if ((value.startsWith("HV0x") || value.startsWith("AJ0x")) && value.length() > 16) {
			return value.substring(0, 8) + "…" + value.substring(value.length() - 4);
		}
		return value;
	}
}
