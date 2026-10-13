package com.harvana.app.web3;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.harvana.app.utils.AppBar;

/** Web3 (from My Asset): Harvana Swap, Harvana Staking, Harvana Explorer and other Web3 sites. */
public class DiscoverActivity extends AppCompatActivity {

    private Web3Home home;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        home = new Web3Home(this);
        AppBar.setContent(this, com.harvana.app.R.layout.activity_discover, "Web3", "dApps").setNavigationOnClickListener(v -> finish());
        ((android.widget.FrameLayout) findViewById(com.harvana.app.R.id.discover_home)).addView(home.view());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (home != null) home.refreshRecent();
    }
}
