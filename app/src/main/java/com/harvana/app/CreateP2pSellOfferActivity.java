package com.harvana.app;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

/** Create a sell offer (res/layout/create_p2p_sell_offer.xml). Every control is wired by p2p/P2pOfferWizard. */
public class CreateP2pSellOfferActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.create_p2p_sell_offer);
		com.harvana.app.utils.StatusBarUtil.matchToolbar(this);
		findViewById(R.id.iv_sell_offer_back).setOnClickListener(v -> finish());
		com.harvana.app.p2p.P2pOfferWizard.attachSell(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		// Back from Payment methods: show the accounts just added.
		com.harvana.app.p2p.P2pOfferWizard.refreshPayments(this);
	}
}
