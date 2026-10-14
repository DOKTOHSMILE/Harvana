package com.harvana.app;

/** Sell into a BUY offer: the same screen as buying (P2pBuyOrderActivity), from the seller's side. */
public class P2pSellOrderActivity extends P2pBuyOrderActivity {
	@Override
	protected boolean selling() { return true; }
}
