package com.harvana.app.group.create;

import android.widget.LinearLayout;

/** Wizard page 3: who to invite. */
public class PeopleStepFragment extends CreateGroupStepFragment {
    @Override protected void build(Host host, LinearLayout page) { host.buildPeople(page); }
}
