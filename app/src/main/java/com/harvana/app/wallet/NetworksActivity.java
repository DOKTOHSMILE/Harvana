package com.harvana.app.wallet;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.harvana.app.R;

/**
 * Add a network, the way MetaMask does it: Popular (known networks from the chain list, each with Add) and Custom networks
 * (name, RPC URL, chain ID, symbol, block explorer; the RPC must answer with that chain ID). Each tab is its own fragment;
 * adding always goes through the approval sheet ({@link NetworkSheets#approve}).
 */
public class NetworksActivity extends AppCompatActivity {

    public static final String EXTRA_TAB = "tab";
    private static Runnable onAdded;
    private boolean added;

    /** Opens the Networks screen; {@code whenAdded} runs once when it closes after a network was added. */
    public static void open(Context c, Runnable whenAdded) {
        onAdded = whenAdded;
        c.startActivity(new Intent(c, NetworksActivity.class));
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        com.harvana.app.utils.AppBar.setContent(this, R.layout.activity_networks, "Networks", null);
        TabLayout tabs = findViewById(R.id.networks_tabs);
        com.harvana.app.utils.Toggles.pill(tabs);
        ViewPager2 pager = findViewById(R.id.networks_pager);
        pager.setAdapter(new FragmentStateAdapter(this) {
            @NonNull @Override public Fragment createFragment(int position) {
                return position == 0 ? new PopularNetworksFragment() : new CustomNetworkFragment();
            }
            @Override public int getItemCount() { return 2; }
        });
        new TabLayoutMediator(tabs, pager, (tab, i) -> tab.setText(i == 0 ? "Popular" : "Custom networks")).attach();
        pager.setCurrentItem(getIntent().getIntExtra(EXTRA_TAB, 0), false);
    }

    /** A fragment added a network: the Assets list redraws when this screen closes. */
    void networkAdded() { added = true; }

    @Override public boolean onSupportNavigateUp() { finish(); return true; }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!isFinishing()) return;
        Runnable r = onAdded;
        onAdded = null;
        if (added && r != null) r.run();
    }
}
