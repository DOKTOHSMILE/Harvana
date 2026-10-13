package com.harvana.app.group.create;

import android.widget.LinearLayout;

/** Wizard page 4: everything chosen, before the group is created. */
public class ReviewStepFragment extends CreateGroupStepFragment {
    @Override protected int layout() { return com.harvana.app.R.layout.fragment_create_review; }
    @Override protected void build(Host host, LinearLayout page) { host.buildReview(page); }
}
