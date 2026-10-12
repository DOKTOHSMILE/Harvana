package com.harvana.app.web3;

import android.os.Bundle;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;

import com.harvana.app.utils.AppBar;
import com.harvana.app.utils.UiColors;

/** Web3 (from My Asset): Harvana Swap, Harvana Staking, Harvana Explorer and other Web3 sites. */
public class DiscoverActivity extends AppCompatActivity {

    private Web3Home home;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        home = new Web3Home(this);
        NestedScrollView scroll = new NestedScrollView(this);
        scroll.setBackgroundColor(UiColors.background());
        scroll.setFillViewport(true);
        scroll.addView(home.view(), new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        AppBar.setContent(this, scroll, "Web3", "dApps").setNavigationOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (home != null) home.refreshRecent();
    }
}
