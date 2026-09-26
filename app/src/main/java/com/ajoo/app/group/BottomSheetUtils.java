package com.ajoo.app.group;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.FrameLayout;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

/**
 * Forces a BottomSheetDialog to open fully expanded (not the default
 * peek-then-drag-up half sheet) with rounded top corners over a solid
 * background. Previously written inline once per sheet (the old admin
 * group-wallet transfer sheet had its own copy of this exact logic) -
 * pulled out here so every sheet that wants the full-screen treatment
 * gets it the same way instead of re-implementing it.
 */
public final class BottomSheetUtils {

	private BottomSheetUtils() {}

	/** cornerRadiusDp: top-corner radius in dp. backgroundColor: the sheet's
	 *  own background (its content should normally already paint this via
	 *  its root layout, so this is a fallback fill behind rounded corners). */
	public static void expandFullScreen(BottomSheetDialog dialog, View sheetView,
	                                    int cornerRadiusDp, int backgroundColor) {
		if (dialog == null || sheetView == null) return;

		dialog.setOnShowListener(d -> {
			BottomSheetDialog bsd = (BottomSheetDialog) d;
			FrameLayout bottomSheetInternal =
					bsd.findViewById(com.google.android.material.R.id.design_bottom_sheet);
			if (bottomSheetInternal == null) return;

			Context context = sheetView.getContext();
			bottomSheetInternal.setBackgroundColor(Color.TRANSPARENT);

			float radius = cornerRadiusDp * context.getResources().getDisplayMetrics().density;
			GradientDrawable background = new GradientDrawable();
			background.setShape(GradientDrawable.RECTANGLE);
			background.setColor(backgroundColor);
			background.setCornerRadii(new float[]{radius, radius, radius, radius, 0f, 0f, 0f, 0f});
			sheetView.setBackground(background);

			BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheetInternal);
			behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
			behavior.setSkipCollapsed(true);
			behavior.setFitToContents(false);
			behavior.setDraggable(true);

			android.view.ViewGroup.LayoutParams lp = bottomSheetInternal.getLayoutParams();
			lp.height = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
			bottomSheetInternal.setLayoutParams(lp);
		});
	}

	/** Convenience overload matching the dark fintech background (#0F1623)
	 *  used by every other full-screen sheet in the app. */
	public static void expandFullScreen(BottomSheetDialog dialog, View sheetView) {
		expandFullScreen(dialog, sheetView, 24, Color.parseColor("#0F1623"));
	}
}
