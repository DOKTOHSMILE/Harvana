package com.harvana.app.group.create;

import android.widget.LinearLayout;

/** Wizard page 3: who to invite. */
public class PeopleStepFragment extends CreateGroupStepFragment {
    @Override protected int layout() { return com.harvana.app.R.layout.fragment_create_people; }
    @Override protected void build(Host host, LinearLayout page) { host.buildPeople(page); }
}
