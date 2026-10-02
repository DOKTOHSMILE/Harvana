package com.harvanna.app;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.harvanna.app.earn.StakingPanel;

public class StakingDashboardActivity extends AppCompatActivity {

	private Toolbar _toolbar;
	private StakingPanel panel;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_staking_dashboard);
		_toolbar = findViewById(R.id._toolbar);
		setSupportActionBar(_toolbar);
		if (getSupportActionBar() != null) {
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
			getSupportActionBar().setHomeButtonEnabled(true);
			getSupportActionBar().setTitle("Staking Dashboard");
		}
		_toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onBackPressed();
			}
		});
		panel = new StakingPanel(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (panel != null) panel.reload();
	}
}
