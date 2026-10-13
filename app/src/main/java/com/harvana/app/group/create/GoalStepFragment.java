package com.harvana.app.group.create;

import android.widget.LinearLayout;

/** Goal group, page 2: the coin, the target, how money leaves and who receives it. */
public class GoalStepFragment extends CreateGroupStepFragment {
    @Override protected int layout() { return com.harvana.app.R.layout.fragment_create_goal; }
    @Override protected void build(Host host, LinearLayout page) { host.buildGoal(page); }
}
