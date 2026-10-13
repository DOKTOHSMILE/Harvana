package com.harvana.app.group.create;

import android.widget.LinearLayout;

/** Wizard page 1: name, description, picture, place, rules, chats. */
public class BasicsStepFragment extends CreateGroupStepFragment {
    @Override protected void build(Host host, LinearLayout page) { host.buildBasics(page); }
}
