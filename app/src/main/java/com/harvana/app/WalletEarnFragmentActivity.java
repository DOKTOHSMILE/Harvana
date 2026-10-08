package com.harvana.app;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.FirebaseApp;
import com.harvana.app.sync.HarvanaNodeClient;
import com.harvana.app.sync.NodeGroupClient;
import com.harvana.app.utils.UiColors;
import com.harvana.app.wallet.WalletManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WalletEarnFragmentActivity extends Fragment {

    private static final String PREF_MINING = "harvana_mining_rewards";
    private static final String KEY_TOTAL_MINED = "total_mined_harv";
    private String walletAddress = "";
    
    private final DecimalFormat harvFormatter = new DecimalFormat("0.####", DecimalFormatSymbols.getInstance(Locale.US));

    private NestedScrollView vscroll1;
    private LinearLayout linear1;
    private MaterialCardView cardview4, cardview5, cardStakeIntro;
    private LinearLayout linear7, linear8, linear13;
    private CardView cardview7;
    private ImageView imageview1;
    private TextView textview16, textview10, textview8;
    
    // New Staking Summary Views
    private TextView tvSummaryStaked;
    private TextView tvSummaryUnclaimed;
    private LineChart miniChart;
    private TextView tvLearnMore;
    private com.google.android.material.button.MaterialButton btnOpenStaking;

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater _inflater, @Nullable ViewGroup _container, @Nullable Bundle _savedInstanceState) {
        View _view = _inflater.inflate(R.layout.wallet_earn_fragment, _container, false);
        initialize(_savedInstanceState, _view);
        FirebaseApp.initializeApp(getContext());
        return _view;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        loadStakingSummary();
    }

    private void initialize(Bundle _savedInstanceState, View _view) {
        vscroll1 = _view.findViewById(R.id.vscroll1);
        linear1 = _view.findViewById(R.id.linear1);
        cardview4 = _view.findViewById(R.id.cardview4);
        cardview5 = _view.findViewById(R.id.cardview5);
        cardStakeIntro = _view.findViewById(R.id.card_stake_intro);
        linear7 = _view.findViewById(R.id.linear7);
        cardview7 = _view.findViewById(R.id.cardview7);
        linear13 = _view.findViewById(R.id.linear13);
        imageview1 = _view.findViewById(R.id.imageview1);
        textview16 = _view.findViewById(R.id.textview16);
        linear8 = _view.findViewById(R.id.linear8);
        textview10 = _view.findViewById(R.id.textview10);
        textview8 = _view.findViewById(R.id.textview8);
        
        tvSummaryStaked = _view.findViewById(R.id.tv_summary_staked);
        tvSummaryUnclaimed = _view.findViewById(R.id.tv_summary_unclaimed);
        miniChart = _view.findViewById(R.id.mini_reward_chart);
        tvLearnMore = _view.findViewById(R.id.tv_learn_more);
        btnOpenStaking = _view.findViewById(R.id.btn_open_staking);

        androidx.swiperefreshlayout.widget.SwipeRefreshLayout refresh = _view.findViewById(R.id.refresh_earn);
        if (refresh != null) {
            refresh.setColorSchemeResources(R.color.colorAccent);
            refresh.setProgressBackgroundColorSchemeResource(R.color.colorSurface);
            refresh.setOnRefreshListener(() -> {
                loadStakingSummary();
                refresh.postDelayed(() -> { if (isAdded()) refresh.setRefreshing(false); }, 1800);
            });
        }

        setupMiniChart();

        btnOpenStaking.setOnClickListener(_v -> {
            if (getContext() == null) return;
            startActivity(new Intent(getContext(), StakingDashboardActivity.class));
        });
        
        tvLearnMore.setOnClickListener(_v -> showHowItWorks());
        
        cardview7.setOnClickListener(_v -> {
            Context targetContext = getContext() != null ? getContext() : (getActivity() != null ? getActivity() : null);
            boolean isInstalled = false;
            
            if (targetContext != null) {
                try {
                    targetContext.getPackageManager().getPackageInfo("com.harvana.mine", 0);
                    isInstalled = true;
                } catch (Exception e) {
                    isInstalled = false;
                }
            }
            
            if (isInstalled && targetContext != null) {
                Intent launchIntent = targetContext.getPackageManager().getLaunchIntentForPackage("com.harvana.mine");
                if (launchIntent != null) {
                    try {
                        startActivity(launchIntent);
                    } catch (Exception e) {
                        Toast.makeText(targetContext, "Unable to launch Harvana Node Miner", Toast.LENGTH_LONG).show();
                    }
                }
            } else {
                if (targetContext != null) {
                    Toast.makeText(targetContext, "Harvana Node Miner app is not installed — opening download link", Toast.LENGTH_LONG).show();
                }
                String downloadUrl = "https://t.me/+yay9a20_6M4wZGZk";
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl));
                try {
                    startActivity(browserIntent);
                } catch (Exception e) {
                    // Fallback handled
                }
            }
        });
    }

    private void loadStakingSummary() {
        if (getContext() == null) return;
        String address = WalletManager.getActiveWalletAddress(getContext());
        
        if (address == null || address.isEmpty()) {
            tvSummaryStaked.setText("0 HARV");
            tvSummaryUnclaimed.setText("0 HARV");
            return;
        }

        NodeGroupClient.readStakingPosition(address, new NodeGroupClient.Callback() {
            @Override
            public void onSuccess(JSONObject json) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    long staked = json.optLong("stakedRaw", 0);
                    long pending = json.optLong("pendingRewardsRaw", 0);
                    
                    tvSummaryStaked.setText(harvFormatter.format(staked / (double) HarvanaNodeClient.HARV_UNITS_PER_TOKEN) + " HARV");
                    tvSummaryUnclaimed.setText(harvFormatter.format(pending / (double) HarvanaNodeClient.HARV_UNITS_PER_TOKEN) + " HARV");
                });
            }

            @Override
            public void onError(String error) {
                // Fail silently on the summary dashboard to avoid interrupting UX
            }
        });

        drawMiniChart(address);
    }

    private void setupMiniChart() {
        miniChart.setTouchEnabled(false);
        miniChart.getDescription().setEnabled(false);
        miniChart.getLegend().setEnabled(false);
        miniChart.getAxisLeft().setEnabled(false);
        miniChart.getAxisRight().setEnabled(false);
        miniChart.getXAxis().setEnabled(false);
        miniChart.setDrawGridBackground(false);
        miniChart.setDrawBorders(false);
        miniChart.setViewPortOffsets(0, 0, 0, 0); // Fills exactly to the edge
        miniChart.setBackgroundColor(Color.TRANSPARENT);
        miniChart.setNoDataText("Rewards show here as days pass.");
        miniChart.setNoDataTextColor(com.harvana.app.utils.UiColors.textSecondary());
    }

    private void drawMiniChart(String address) {
        if (getContext() == null) return;
        String raw = getContext().getSharedPreferences("harvana_stake_history", Context.MODE_PRIVATE).getString(address, "[]");
        try {
            JSONArray rows = new JSONArray(raw);
            if (rows.length() == 0) return;

            List<Entry> entries = new ArrayList<>();
            int chartDays = 7;
            int start = Math.max(0, rows.length() - chartDays);
            long previous = -1;

            if (start > 0) {
                JSONObject prior = rows.optJSONObject(start - 1);
                if (prior != null) previous = prior.optLong("earned", -1);
            }

            for (int i = start; i < rows.length(); i++) {
                JSONObject row = rows.optJSONObject(i);
                if (row == null) continue;
                long earned = row.optLong("earned", 0);
                double delta = previous < 0 ? 0 : Math.max(0, earned - previous) / (double) HarvanaNodeClient.HARV_UNITS_PER_TOKEN;
                previous = earned;
                entries.add(new Entry(entries.size(), (float) delta));
            }
            
            if (entries.isEmpty()) return;

            LineDataSet set = new LineDataSet(entries, "Rewards");
            set.setColor(com.harvana.app.utils.UiColors.accent()); // Harvana Orange
            set.setLineWidth(2f);
            set.setDrawCircles(false);
            set.setDrawValues(false);
            set.setDrawFilled(true);
            set.setFillColor(com.harvana.app.utils.UiColors.accent());
            set.setFillAlpha(40);
            set.setMode(LineDataSet.Mode.CUBIC_BEZIER);

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    miniChart.setData(new LineData(set));
                    miniChart.invalidate();
                });
            }

        } catch (Exception ignored) { }
    }

    private void showHowItWorks() {
        if (getContext() == null) return;
        com.google.android.material.bottomsheet.BottomSheetDialog dialog = com.harvana.app.p2p.ThemedDialogs.sheet(getContext());
        android.widget.ScrollView scroll = new android.widget.ScrollView(getContext());
        LinearLayout box = new LinearLayout(getContext());
        box.setOrientation(LinearLayout.VERTICAL);
        int p = (int) (20 * getResources().getDisplayMetrics().density);
        box.setPadding(p, p, p, p);
        box.addView(sheetTitle("HARV PoW Reward Program"));
        box.addView(sheetBody("The HARV Proof-of-Work reward program pays people who secure the chain. Solo miners take the full block when they find it. Pool miners split that block by the valid shares they submitted. A reward stays pending until the block has enough confirmations, then it is credited to the wallet.\n\nDifficulty, hashrate, battery protection and share checks are there so one phone cannot pretend to be a farm."));
        box.addView(sheetTitle("How to earn more"));
        box.addView(sheetBody("Stay online in longer sessions, keep a stable connection, and cut rejected shares by updating the app. A stronger CPU raises hashrate, but the red thread setting can overheat the phone. Staking is the other path: lock HARV and take your percentage of the reward pool that is already there. Unstaking stops earning immediately and returns after 24 hours."));
        scroll.addView(box);
        dialog.setContentView(scroll);
        dialog.show();
    }

    private TextView sheetTitle(String value) {
        TextView t = new TextView(getContext());
        t.setText(value);
        t.setTextSize(18);
        t.setTypeface(null, android.graphics.Typeface.BOLD);
        t.setTextColor(com.harvana.app.utils.UiColors.textPrimary());
        t.setPadding(0, (int) (12 * getResources().getDisplayMetrics().density), 0, 0);
        return t;
    }

    private TextView sheetBody(String value) {
        TextView t = new TextView(getContext());
        t.setText(value);
        t.setTextSize(14);
        t.setTextColor(com.harvana.app.utils.UiColors.textSecondary());
        t.setPadding(0, (int) (8 * getResources().getDisplayMetrics().density), 0, 0);
        return t;
    }
}
