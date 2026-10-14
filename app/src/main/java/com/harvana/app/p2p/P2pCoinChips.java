package com.harvana.app.p2p;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.harvana.app.R;

/** The coin chips (HARV and every stable coin the network has on), with logos; picking one sets P2pAsset. */
public final class P2pCoinChips {

	private P2pCoinChips() {}

	public static void build(ChipGroup group) {
		Context c = group.getContext();
		float d = c.getResources().getDisplayMetrics().density;
		group.setOnCheckedChangeListener(null);
		group.removeAllViews();
		String current = P2pAsset.get();
		ColorStateList bg = new ColorStateList(new int[][]{{android.R.attr.state_checked}, {}},
			new int[]{ContextCompat.getColor(c, R.color.colorAccentSoft), ContextCompat.getColor(c, R.color.colorSurface)});
		ColorStateList stroke = new ColorStateList(new int[][]{{android.R.attr.state_checked}, {}},
			new int[]{ContextCompat.getColor(c, R.color.colorAccent), ContextCompat.getColor(c, R.color.colorCardStroke)});
		for (String code : P2pAsset.available()) {
			Chip chip = new Chip(c);
			chip.setId(View.generateViewId());
			chip.setText(P2pAsset.ticker(code));
			chip.setTag(code);
			chip.setCheckable(true);
			chip.setCheckedIconVisible(false);
			chip.setChipIconVisible(true);
			chip.setChipIconSize(18 * d);
			chip.setChipIcon(P2pAsset.HARV.equals(code) ? ContextCompat.getDrawable(c, R.drawable.ic_coin_harv)
				: com.harvana.app.husd.StableIcon.drawable(c, code));
			chip.setChipBackgroundColor(bg);
			chip.setChipStrokeWidth(d);
			chip.setChipStrokeColor(stroke);
			chip.setTextColor(ContextCompat.getColor(c, R.color.colorTextPrimary));
			group.addView(chip);
			if (code.equals(current)) chip.setChecked(true);
		}
		group.setOnCheckedChangeListener((g, id) -> {
			Chip chip = g.findViewById(id);
			if (chip != null) P2pAsset.set(String.valueOf(chip.getTag()));
		});
	}
}
