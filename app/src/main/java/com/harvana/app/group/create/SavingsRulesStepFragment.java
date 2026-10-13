package com.harvana.app.group.create;

import android.widget.LinearLayout;

/** Savings group, page 2: the coin, loans and interest, withdrawals. */
public class SavingsRulesStepFragment extends CreateGroupStepFragment {
    @Override protected void build(Host host, LinearLayout page) { host.buildSavingsRules(page); }
}
