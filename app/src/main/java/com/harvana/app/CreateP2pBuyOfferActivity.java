package com.harvana.app;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

/** Create a buy offer (res/layout/create_p2p_buy_offer.xml). Every control is wired by p2p/P2pOfferWizard. */
public class CreateP2pBuyOfferActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.create_p2p_buy_offer);
		com.harvana.app.utils.StatusBarUtil.matchToolbar(this);
		findViewById(R.id.iv_buy_offer_back).setOnClickListener(v -> finish());
		com.harvana.app.p2p.P2pOfferWizard.attachBuy(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		// Back from Payment methods: show the accounts just added.
		com.harvana.app.p2p.P2pOfferWizard.refreshPayments(this);
	}
}
