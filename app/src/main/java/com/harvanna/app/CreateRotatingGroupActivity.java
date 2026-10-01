package com.harvanna.app;

import android.content.Intent;
import android.os.Bundle;

/** Rotating pool only. Slots, contribution, frequency and payout order — nothing from the other group types. */
public class CreateRotatingGroupActivity extends CreateNewGroupActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Intent i = getIntent();
		if (i == null) {
			i = new Intent();
			setIntent(i);
		}
		i.putExtra("groupType", "Rotating Pool");
		i.putExtra("lockType", true);
		super.onCreate(savedInstanceState);
	}
}
