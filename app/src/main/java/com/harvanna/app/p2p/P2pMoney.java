package com.harvanna.app.p2p;

import java.util.Locale;

/** Fiat formatting for P2P — never hard-code ₦ once the currency picker is live. */
public final class P2pMoney {

	private P2pMoney() {}

	public static String symbol(String code) {
		if (code == null || code.isEmpty()) code = P2pCurrencyFilterState.getCurrency();
		switch (code) {
			case "USD":
			case "USDT":
			case "USDC": return "$";
			case "EUR": return "€";
			case "GHS": return "₵";
			case "GBP": return "£";
			case "ZAR": return "R";
			case "BTC": return "₿";
			default: return "₦";
		}
	}

	public static String fiat(double amount, String code) {
		return symbol(code) + String.format(Locale.US, "%,.2f", amount);
	}

	public static String perHarv(double price, String code) {
		return fiat(price, code) + (" / " + com.harvanna.app.CoinInfo.ticker());
	}
}
