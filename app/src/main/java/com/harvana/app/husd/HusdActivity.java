package com.harvana.app.husd;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

/**
 * A stable coin's page is the same coin page every other coin uses (CoinDetailActivity, with its Deposit, Withdraw,
 * Swap and fees for this coin). This name stays so older links ("asset" = "HEUR", …) still open the right page.
 */
public class HusdActivity extends AppCompatActivity {

    public static final String EXTRA_ASSET = "asset";

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        String asked = getIntent() == null ? null : getIntent().getStringExtra(EXTRA_ASSET);
        com.harvana.app.CoinDetailActivity.openStable(this, Stable.isStable(asked) ? asked : HusdClient.ASSET);
        finish();
        overridePendingTransition(0, 0);
    }

    public static String chainName(String c) {
        switch (c) {
            case "BASE": return "Base";
            case "POLYGON": return "Polygon";
            case "ARBITRUM": return "Arbitrum";
            case "ETHEREUM": return "Ethereum";
            case "BSC": return "BNB Chain";
            case "CELO": return "Celo";
            default: return c;
        }
    }
}
