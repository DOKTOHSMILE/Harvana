package com.harvana.app.group.create;

import android.widget.LinearLayout;

/** Rotating group, page 2: the coin, contribution, members, round length. */
public class RoundsStepFragment extends CreateGroupStepFragment {
    @Override protected void build(Host host, LinearLayout page) { host.buildRounds(page); }
}
