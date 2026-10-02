package com.harvanna.app.group;

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
import com.harvanna.app.R;
import com.harvanna.app.wallet.PinPromptHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Review sheet shown before any payment. The person sees what they are
 * paying, who it is for, and the fee. Confirming that sheet is what asks
 * for the PIN or fingerprint. Cancel on either step sends nothing.
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
		rows.add(new String[]{"Network fee", feeDisplay != null && !feeDisplay.trim().isEmpty()
				? feeDisplay.trim() : "Taken when this is sent"});
		present(context, title, com.harvanna.app.wallet.BalanceFormatter.smart(amountHarv), rows, false,
				() -> { if (onConfirm != null) onConfirm.onConfirmed(); });
	}

	/**
	 * Payment summary, then the PIN or fingerprint. Cancel on either step
	 * sends nothing. rows are label/value lines (who it is for, the fee, what it pays).
	 */
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
				if (purpose == null && isPurpose(label)) {
					purpose = value;
				} else {
					facts.add(new String[]{label, _present(value)});
				}
			}
		}
		if (purpose == null) {
			purpose = hideAmount ? heading : ("You are about to send " + amount + ".");
		}

		int surface = color(context, R.color.colorSurface);
		int raised = color(context, R.color.colorSurfaceRaised);
		int primary = color(context, R.color.colorTextPrimary);
		int secondary = color(context, R.color.colorTextSecondary);
		int tertiary = color(context, R.color.colorTextTertiary);
		int accent = color(context, R.color.colorAccent);
		int onAccent = color(context, R.color.colorTextOnAccent);
		int divider = color(context, R.color.colorDivider);
		int stroke = color(context, R.color.colorCardStroke);
		float d = context.getResources().getDisplayMetrics().density;

		LinearLayout root = new LinearLayout(context);
		root.setOrientation(LinearLayout.VERTICAL);
		int pad = (int) (22 * d);
		root.setPadding(pad, (int) (10 * d), pad, (int) (20 * d));

		View handle = new View(context);
		GradientDrawable handleBg = new GradientDrawable();
		handleBg.setCornerRadius(4 * d);
		handleBg.setColor(stroke);
		handle.setBackground(handleBg);
		LinearLayout.LayoutParams handleLp = new LinearLayout.LayoutParams((int) (40 * d), (int) (4 * d));
		handleLp.gravity = Gravity.CENTER_HORIZONTAL;
		handleLp.bottomMargin = (int) (16 * d);
		root.addView(handle, handleLp);

		TextView eyebrow = label(context, "REVIEW BEFORE YOU PAY", 11, accent, true);
		eyebrow.setLetterSpacing(0.08f);
		root.addView(eyebrow);

		TextView tvTitle = label(context, heading, 20, primary, true);
		tvTitle.setPadding(0, (int) (6 * d), 0, 0);
		root.addView(tvTitle);

		if (!hideAmount) {
			LinearLayout amountCard = card(context, raised, stroke, d);
			LinearLayout.LayoutParams amountLp = new LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			amountLp.topMargin = (int) (16 * d);
			TextView amountLabel = label(context, "AMOUNT", 11, tertiary, true);
			amountLabel.setLetterSpacing(0.08f);
			amountLabel.setGravity(Gravity.CENTER);
			amountCard.addView(amountLabel);
			TextView tvAmount = label(context, amount, 28, primary, true);
			tvAmount.setGravity(Gravity.CENTER);
			tvAmount.setPadding(0, (int) (4 * d), 0, 0);
			amountCard.addView(tvAmount);
			root.addView(amountCard, amountLp);
		}

		LinearLayout card = card(context, raised, stroke, d);
		LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		cardLp.topMargin = (int) (12 * d);

		TextView forLabel = label(context, "WHAT THIS PAYS FOR", 11, tertiary, true);
		forLabel.setLetterSpacing(0.06f);
		card.addView(forLabel);
		TextView forBody = label(context, purpose, 15, primary, false);
		forBody.setPadding(0, (int) (6 * d), 0, (int) (4 * d));
		forBody.setLineSpacing(2 * d, 1f);
		card.addView(forBody);

		for (int i = 0; i < facts.size(); i++) {
			View line = new View(context);
			line.setBackgroundColor(divider);
			LinearLayout.LayoutParams lineLp = new LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, (int) d));
			lineLp.topMargin = (int) (10 * d);
			lineLp.bottomMargin = (int) (10 * d);
			card.addView(line, lineLp);
			card.addView(fact(context, facts.get(i)[0], facts.get(i)[1], secondary, primary, d));
		}
		root.addView(card, cardLp);

		TextView note = label(context,
				thenPin
						? "Nothing is sent until you verify with your PIN or fingerprint."
						: "Check this carefully. Continue only if it is what you meant to pay.",
				12.5f, tertiary, false);
		note.setPadding(0, (int) (14 * d), 0, (int) (16 * d));
		root.addView(note);

		LinearLayout buttons = new LinearLayout(context);
		buttons.setOrientation(LinearLayout.HORIZONTAL);

		Button cancel = actionButton(context, "Cancel", false, accent, onAccent, d);
		Button go = actionButton(context, thenPin ? "Verify" : "Continue", true, accent, onAccent, d);
		LinearLayout.LayoutParams half = new LinearLayout.LayoutParams(0, (int) (52 * d), 1f);
		LinearLayout.LayoutParams halfEnd = new LinearLayout.LayoutParams(0, (int) (52 * d), 1f);
		halfEnd.leftMargin = (int) (10 * d);
		buttons.addView(cancel, half);
		buttons.addView(go, halfEnd);
		root.addView(buttons);

		ScrollView scroll = new ScrollView(context);
		scroll.setFillViewport(true);
		scroll.addView(root, new ScrollView.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		BottomSheetDialog dialog = new BottomSheetDialog(context);
		dialog.setContentView(scroll);
		dialog.setOnShowListener(shown -> {
			FrameLayout sheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
			if (sheet == null) return;
			sheet.setBackgroundColor(android.graphics.Color.TRANSPARENT);
			GradientDrawable bg = new GradientDrawable();
			bg.setColor(surface);
			float r = 24 * d;
			bg.setCornerRadii(new float[]{r, r, r, r, 0f, 0f, 0f, 0f});
			scroll.setBackground(bg);
			BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(sheet);
			behavior.setFitToContents(true);
			behavior.setSkipCollapsed(true);
			behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
			int max = (int) (context.getResources().getDisplayMetrics().heightPixels * 0.9f);
			sheet.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
			scroll.setLayoutParams(new FrameLayout.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
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
					if (after != null) after.run();
				});
			} else if (after != null) {
				after.run();
			}
		});
		dialog.show();
	}

	private static boolean isPurpose(String label) {
		String l = label.toLowerCase(Locale.US);
		return l.equals("for") || l.equals("what this pays for") || l.equals("what this is for")
				|| l.equals("you are authorizing") || l.equals("this does") || l.equals("note");
	}

	private static LinearLayout card(Context ctx, int fill, int stroke, float d) {
		LinearLayout card = new LinearLayout(ctx);
		card.setOrientation(LinearLayout.VERTICAL);
		int p = (int) (16 * d);
		card.setPadding(p, p, p, p);
		GradientDrawable bg = new GradientDrawable();
		bg.setColor(fill);
		bg.setCornerRadius(16 * d);
		bg.setStroke(Math.max(1, (int) d), stroke);
		card.setBackground(bg);
		return card;
	}

	private static View fact(Context ctx, String name, String value, int labelColor, int valueColor, float d) {
		boolean block = value.length() > 36;
		LinearLayout row = new LinearLayout(ctx);
		row.setOrientation(block ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
		if (!block) row.setGravity(Gravity.CENTER_VERTICAL);
		TextView left = label(ctx, name, 13.5f, labelColor, false);
		TextView right = label(ctx, value, 13.5f, valueColor, true);
		if (block) {
			right.setPadding(0, (int) (4 * d), 0, 0);
			row.addView(left);
			row.addView(right);
		} else {
			right.setGravity(Gravity.END);
			row.addView(left, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.9f));
			row.addView(right, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.3f));
		}
		return row;
	}

	private static Button actionButton(Context ctx, String text, boolean primary, int accent, int onAccent, float d) {
		Button b = new Button(ctx);
		b.setText(text);
		b.setAllCaps(false);
		b.setTextSize(15);
		b.setTypeface(Typeface.DEFAULT_BOLD);
		b.setTextColor(primary ? onAccent : accent);
		b.setBackgroundResource(primary ? R.drawable.bg_btn_primary : R.drawable.bg_btn_outline_accent);
		b.setStateListAnimator(null);
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
