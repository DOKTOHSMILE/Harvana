package com.harvana.app.utils;

import android.content.res.ColorStateList;
import android.view.ViewGroup;

import com.google.android.material.tabs.TabLayout;
import com.harvana.app.R;

/**
 * One look for every toggle in the app: a rounded track with the selected option as an inset pill, 38dp tall
 * (owner: 35 to 40dp; R.dimen.toggle_height). The pill is inset inside the drawable, never by padding on the
 * TabLayout, because TabLayout ignores its end padding when it draws the indicator and the pill ran past the track.
 */
public final class Toggles {
    private Toggles() {}

    public static void pill(TabLayout t) {
        if (t == null) return;
        int h = t.getResources().getDimensionPixelSize(R.dimen.toggle_height);
        t.setPadding(0, 0, 0, 0);
        t.setBackgroundResource(R.drawable.bg_tab_container);
        t.setSelectedTabIndicator(R.drawable.bg_toggle_indicator);
        t.setSelectedTabIndicatorGravity(TabLayout.INDICATOR_GRAVITY_STRETCH);
        t.setTabIndicatorFullWidth(true);
        t.setSelectedTabIndicatorColor(UiColors.accent());
        t.setTabTextColors(UiColors.textSecondary(), UiColors.onAccent());
        t.setTabRippleColor(ColorStateList.valueOf(0x00000000));
        t.setInlineLabel(true);
        ViewGroup.LayoutParams lp = t.getLayoutParams();
        if (lp == null) lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, h);
        lp.height = h;
        t.setLayoutParams(lp);
        t.setMinimumHeight(h);
    }
}
