package com.harvana.app;

import android.content.Intent;
import android.os.Bundle;

/** Fixed contribution only. Daily custody amount, member count, goal and disbursement rule. */
public class CreateFixedGroupActivity extends CreateNewGroupActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Intent i = getIntent();
		if (i == null) {
			i = new Intent();
			setIntent(i);
		}
		i.putExtra("groupType", "Fixed Contribution");
		i.putExtra("lockType", true);
		super.onCreate(savedInstanceState);
	}
}
