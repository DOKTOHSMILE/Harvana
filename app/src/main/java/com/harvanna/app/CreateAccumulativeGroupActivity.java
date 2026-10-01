package com.harvanna.app;

import android.content.Intent;
import android.os.Bundle;

/** Accumulative savings only. Contribution, interest, withdrawal rule and the savings-book fields. */
public class CreateAccumulativeGroupActivity extends CreateNewGroupActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Intent i = getIntent();
		if (i == null) {
			i = new Intent();
			setIntent(i);
		}
		i.putExtra("groupType", "Accumulative Funds");
		i.putExtra("lockType", true);
		super.onCreate(savedInstanceState);
	}
}
