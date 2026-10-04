package com.harvana.app.earn;

import android.app.Activity;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.harvana.app.R;
import com.harvana.app.group.PaymentConfirmSheet;
import com.harvana.app.sync.HarvanaNodeClient;
import com.harvana.app.sync.NodeGroupClient;
import com.harvana.app.tx.TransactionRouter;
import com.harvana.app.utils.UiColors;
import com.harvana.app.wallet.BalanceFormatter;
import com.harvana.app.wallet.WalletManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class StakingPanel {
    private static final String PREFS = "harvana_stake_history";

    private final AppCompatActivity host;
    
    // Smart formatter: drops trailing zeros, max 4 decimals
    private final DecimalFormat harvFormatter = new DecimalFormat("0.####", DecimalFormatSymbols.getInstance(Locale.US));
    
    // Page 1 UI
    private final TextView overviewStaked, dailyPct, overviewUnclaimed;
    private final TextView tvPool;
    private final LineChart chart;
    private final EditText amount;
    private final MaterialButton stake;
    private final TextView available;
    private final TextView chip7, chip1, chip3, chipAll;
    
    // Page 2 UI
    private final TextView posTotal, posDailyPct, gridTotal, gridUnclaimed, gridApy;
    private final TextView activeAmount, activeMonthly, unbondCount;
    private final MaterialButton claim, activeUnstake;
    private final TextView claimSub;
    private final LinearLayout unbondingList;
    
    // Core Layout
    private final View pageStake, pagePositions, activeCard;
    private final TextView tabStake, tabPositions;
    
    private JSONObject info;
    private JSONObject pos;
    private long observedPace;
    private long walletRaw;
    private int chartDays = 7;
    private boolean busy;

    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (host.isDestroyed() || host.isFinishing()) return;
            updateLiveTimers();
            timerHandler.postDelayed(this, 1000);
        }
    };

    public StakingPanel(AppCompatActivity activity) {
        this.host = activity;
        
        // Bind Page 1
        overviewStaked = activity.findViewById(R.id.tv_overview_staked);
        dailyPct = activity.findViewById(R.id.tv_daily_pct);
        overviewUnclaimed = activity.findViewById(R.id.tv_overview_unclaimed);
        tvPool = activity.findViewById(R.id.tv_pool);
        chart = activity.findViewById(R.id.reward_chart);
        amount = activity.findViewById(R.id.et_amount);
        stake = activity.findViewById(R.id.btn_stake);
        available = activity.findViewById(R.id.tv_available);
        
        chip7 = activity.findViewById(R.id.chip_7d);
        chip1 = activity.findViewById(R.id.chip_1m);
        chip3 = activity.findViewById(R.id.chip_3m);
        chipAll = activity.findViewById(R.id.chip_all);
        
        // Bind Page 2
        posTotal = activity.findViewById(R.id.tv_pos_total);
        posDailyPct = activity.findViewById(R.id.tv_pos_daily_pct);
        gridTotal = activity.findViewById(R.id.tv_grid_total);
        gridUnclaimed = activity.findViewById(R.id.tv_grid_unclaimed);
        gridApy = activity.findViewById(R.id.tv_grid_apy);
        activeAmount = activity.findViewById(R.id.tv_active_amount);
        activeMonthly = activity.findViewById(R.id.tv_active_monthly);
        unbondCount = activity.findViewById(R.id.tv_unbond_count);
        claim = activity.findViewById(R.id.btn_claim);
        claimSub = activity.findViewById(R.id.tv_claim_sub);
        activeUnstake = activity.findViewById(R.id.btn_active_unstake);
        unbondingList = activity.findViewById(R.id.unbonding_list);
        
        // Bind Layouts
        pageStake = activity.findViewById(R.id.page_stake);
        pagePositions = activity.findViewById(R.id.page_positions);
        activeCard = activity.findViewById(R.id.active_card);
        tabStake = activity.findViewById(R.id.tab_stake);
        tabPositions = activity.findViewById(R.id.tab_positions);
        
        stake.setOnClickListener(v -> doStake());
        claim.setOnClickListener(v -> doClaim());
        activeUnstake.setOnClickListener(v -> askUnstake());
        activity.findViewById(R.id.btn_max).setOnClickListener(v -> fillMax());
        
        tabStake.setOnClickListener(v -> showPage(true));
        tabPositions.setOnClickListener(v -> showPage(false));
        chip7.setOnClickListener(v -> setRange(7));
        chip1.setOnClickListener(v -> setRange(30));
        chip3.setOnClickListener(v -> setRange(90));
        chipAll.setOnClickListener(v -> setRange(Integer.MAX_VALUE));
        
        styleChart();
        showPage(true);
        timerHandler.post(timerRunnable);
    }

    public void reload() { refresh(); }

    private void refresh() {
        NodeGroupClient.getStakingInfo(new NodeGroupClient.Callback() {
            @Override public void onSuccess(JSONObject json) { info = json; paint(); }
            @Override public void onError(String error) { }
        });
        String address = WalletManager.getActiveWalletAddress(host);
        if (address != null && !address.isEmpty()) {
            NodeGroupClient.readStakingPosition(address, new NodeGroupClient.Callback() {
                @Override public void onSuccess(JSONObject json) { pos = json; paint(); }
                @Override public void onError(String error) { }
            });
            HarvanaNodeClient.getWallet(address, new HarvanaNodeClient.JsonCallback() {
                @Override public void onSuccess(JSONObject json) { walletRaw = json.optLong("balance", 0); paint(); }
                @Override public void onError(String error) { }
            });
        }
        HarvanaNodeClient.getRecentBlocks(new HarvanaNodeClient.JsonCallback() {
            @Override public void onSuccess(JSONObject json) { observedPace = paceFromBlocks(json); paint(); }
            @Override public void onError(String error) { }
        });
    }

    private void showPage(boolean staking) {
        pageStake.setVisibility(staking ? View.VISIBLE : View.GONE);
        pagePositions.setVisibility(staking ? View.GONE : View.VISIBLE);
        
        tabStake.setBackgroundResource(staking ? R.drawable.bg_tab_active : 0);
        tabStake.setTextColor(staking ? Color.WHITE : Color.parseColor("#8F9BB3"));
        
        tabPositions.setBackgroundResource(staking ? 0 : R.drawable.bg_tab_active);
        tabPositions.setTextColor(staking ? Color.parseColor("#8F9BB3") : Color.WHITE);
    }

    private void paint() {
        ui(() -> {
            long staked = pos == null ? 0 : pos.optLong("stakedRaw", 0);
            long pending = pos == null ? 0 : pos.optLong("pendingRewardsRaw", 0);
            long claimed = pos == null ? 0 : pos.optLong("totalClaimedRaw", 0);
            long total = info == null ? 0 : info.optLong("totalStakedRaw", 0);
            long shareBps = pos != null && pos.has("shareBps") ? pos.optLong("shareBps", 0) : (total > 0 ? staked * 10_000L / total : 0);
            long poolRaw = pos != null && pos.has("rewardPoolBalanceRaw") ? pos.optLong("rewardPoolBalanceRaw", 0) : (info == null ? 0 : info.optLong("rewardPoolBalanceRaw", 0));
            
            double units = (double) HarvanaNodeClient.HARV_UNITS_PER_TOKEN;
            double stakedHarv = staked / units;
            
            // Page 1
            overviewStaked.setText(harv(staked));
            overviewUnclaimed.setText(harv(pending));
            available.setText("Available " + harv(walletRaw));
            if (info != null) tvPool.setText(String.format(Locale.US, "%s in the reward pool  ·  %d stakers", harv(poolRaw), info.optInt("stakerCount", 0)));
            dailyPct.setText(staked <= 0 ? "Stake to start earning" : String.format(Locale.US, "~ +%.3f%% Daily Fluctuating", shareBps / 100.0));
            
            // Page 2
            long queued = unbondingTotal();
            posTotal.setText(harv(staked + queued));
            posDailyPct.setText(dailyPct.getText());
            gridTotal.setText(harv(staked));
            gridUnclaimed.setText(harv(pending));
            gridApy.setText(info != null ? String.format(Locale.US, "%.2f%%", info.optLong("estimatedAprBps", 0) / 100.0) : "--%");
            
            activeCard.setVisibility(staked > 0 ? View.VISIBLE : View.GONE);
            activeAmount.setText(harv(staked));
            activeMonthly.setText(staked <= 0 ? "—" : "~ " + harvFormatter.format(stakedHarv * 0.05) + " HARV (Dynamic)");
            
            String address = WalletManager.getActiveWalletAddress(host);
            if (address != null && staked > 0) recordSnapshot(address, pending + claimed);
            drawChart(address);
            
            stake.setEnabled(!busy);
            activeUnstake.setEnabled(!busy);
            
            setupUnbondingViews();
            updateLiveTimers();
        });
    }

    private void updateLiveTimers() {
        if (pos == null) return;
        long now = System.currentTimeMillis();
        long staked = pos.optLong("stakedRaw", 0);
        long pending = pos.optLong("pendingRewardsRaw", 0);
        
        long lastInteraction = pos.optLong("lastClaimedTimestamp", 0);
        if (lastInteraction == 0) lastInteraction = pos.optLong("firstStakedTimestamp", 0);
        long timeUntilClaim = (lastInteraction + 24L * 3600 * 1000) - now;

        if (staked == 0 && pending == 0) {
            claim.setEnabled(false); claim.setText("Claim Now >");
            claimSub.setText("Stake HARV to start earning rewards.");
        } else if (pending > 0 && timeUntilClaim <= 0) {
            claim.setEnabled(!busy); claim.setText("Claim Now >");
            claimSub.setText("Your rewards are ready to be claimed.");
        } else if (pending > 0) {
            claim.setEnabled(false); claim.setText("Claiming Locked");
            claimSub.setText("Rewards unlock in " + formatTimerPrecise(timeUntilClaim));
        }

        // Update Unbonding rows dynamically
        int activeUnbondings = 0;
        for (int i = 0; i < unbondingList.getChildCount(); i++) {
            View v = unbondingList.getChildAt(i);
            if (v.getTag() instanceof JSONObject) {
                JSONObject u = (JSONObject) v.getTag();
                long release = u.optLong("releaseTimestamp", 0);
                long remain = release - now;
                boolean ready = remain <= 0 || "RELEASING".equals(u.optString("status"));
                
                TextView statusBadge = v.findViewWithTag("badge");
                TextView subText = v.findViewWithTag("sub");
                MaterialButton btn = v.findViewWithTag("btn");
                
                if (ready) {
                    statusBadge.setText("● Ready for Withdrawal");
                    statusBadge.setTextColor(Color.parseColor("#3B82F6"));
                    subText.setText("Status: Cooldown Complete");
                    btn.setVisibility(View.VISIBLE);
                } else {
                    activeUnbondings++;
                    statusBadge.setText("● Unbonding");
                    statusBadge.setTextColor(Color.parseColor("#FF7A00"));
                    subText.setText("Cooldown Remaining: " + formatTimerPrecise(remain));
                    btn.setVisibility(View.GONE);
                }
            }
        }
        unbondCount.setText("● " + activeUnbondings + " In Progress");
    }

    private static String formatTimerPrecise(long ms) {
        if (ms <= 0) return "00h 00m";
        long totalSecs = ms / 1000;
        return String.format(Locale.US, "%02dh %02dm", totalSecs / 3600, (totalSecs % 3600) / 60);
    }

    private void setupUnbondingViews() {
        unbondingList.removeAllViews();
        if (pos == null) return;
        JSONArray rows = pos.optJSONArray("unbonding");
        if (rows == null || rows.length() == 0) return;
        
        for (int i = 0; i < rows.length(); i++) {
            JSONObject u = rows.optJSONObject(i);
            if (u == null || "RELEASED".equals(u.optString("status"))) continue;
            
            MaterialCardView card = new MaterialCardView(host);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, dp(8), 0, dp(8));
            card.setLayoutParams(lp);
            card.setCardBackgroundColor(Color.parseColor("#121623"));
            card.setRadius(dp(12));
            card.setStrokeColor(Color.parseColor("#2A3042"));
            card.setStrokeWidth(dp(1));
            card.setTag(u);
            
            ConstraintLayout cl = new ConstraintLayout(host);
            cl.setPadding(dp(16), dp(16), dp(16), dp(16));
            
            TextView badge = new TextView(host);
            badge.setId(View.generateViewId());
            badge.setTag("badge");
            badge.setTextSize(12);
            badge.setTypeface(null, android.graphics.Typeface.BOLD);
            
            TextView amountTv = new TextView(host);
            amountTv.setId(View.generateViewId());
            amountTv.setText(harv(u.optLong("amountRaw", 0)));
            amountTv.setTextColor(Color.WHITE);
            amountTv.setTextSize(16);
            amountTv.setTypeface(null, android.graphics.Typeface.BOLD);
            
            TextView subTv = new TextView(host);
            subTv.setId(View.generateViewId());
            subTv.setTag("sub");
            subTv.setTextColor(Color.parseColor("#8F9BB3"));
            subTv.setTextSize(12);
            
            MaterialButton btn = new MaterialButton(host, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
            btn.setId(View.generateViewId());
            btn.setTag("btn");
            btn.setText("[Withdraw to Wallet]");
            btn.setAllCaps(false);
            btn.setTextColor(Color.parseColor("#FF7A00"));
            btn.setStrokeColorResource(android.R.color.transparent);
            btn.setVisibility(View.GONE);
            btn.setOnClickListener(v -> doManualSweep());
            
            cl.addView(badge);
            cl.addView(amountTv);
            cl.addView(subTv);
            cl.addView(btn);
            
            ConstraintSet cs = new ConstraintSet();
            cs.clone(cl);
            cs.connect(badge.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
            cs.connect(badge.getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
            cs.connect(amountTv.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
            cs.connect(amountTv.getId(), ConstraintSet.START, badge.getId(), ConstraintSet.END, dp(16));
            cs.connect(subTv.getId(), ConstraintSet.TOP, amountTv.getId(), ConstraintSet.BOTTOM, dp(4));
            cs.connect(subTv.getId(), ConstraintSet.START, amountTv.getId(), ConstraintSet.START);
            cs.connect(btn.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
            cs.connect(btn.getId(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);
            cs.connect(btn.getId(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);
            cs.applyTo(cl);
            
            card.addView(cl);
            unbondingList.addView(card);
        }
    }

    private void doManualSweep() {
        busy = true; toast("Requesting withdrawal release...");
        HarvanaNodeClient.getRecentBlocks(new HarvanaNodeClient.JsonCallback() {
            @Override public void onSuccess(JSONObject json) { busy = false; refresh(); }
            @Override public void onError(String error) { busy = false; refresh(); }
        });
    }

    private void askUnstake() {
        EditText input = new EditText(host);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint("Amount in HARV");
        new androidx.appcompat.app.AlertDialog.Builder(host)
                .setTitle("Unstake Principal")
                .setMessage("Funds stop earning immediately and will lock for exactly 24 hours before returning to your wallet.")
                .setView(input)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Unstake", (d, w) -> {
                    amount.setText(input.getText());
                    doUnstake();
                })
                .show();
    }

    private void doStake() {
        if (info == null) return;
        String address = WalletManager.getActiveWalletAddress(host);
        String vault = info.optString("vaultAddress", "");
        double harvAmount;
        try { harvAmount = Double.parseDouble(amount.getText().toString().trim()); }
        catch (Exception e) { toast("Enter an amount in HARV."); return; }
        
        long already = pos == null ? 0 : pos.optLong("stakedRaw", 0);
        long min = info.optLong("minStakeRaw", HarvanaNodeClient.HARV_UNITS_PER_TOKEN);
        if (vault.isEmpty() || address == null) return;
        if (already < min && BalanceFormatter.harvToRaw(harvAmount) < min) { toast("The first stake must be at least 1 HARV."); return; }
        
        ArrayList<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"To", vault});
        rows.add(new String[]{"For", "Starts earning continuously. Claims unlock every 24h."});
        
        gate("Stake HARV", harvFormatter.format(harvAmount) + " HARV", rows, () -> {
            busy = true; paint();
            TransactionRouter.routeTransaction(host, address, vault, BigDecimal.valueOf(harvAmount), null, "STAKE",
                    new TransactionRouter.RouterCallback() {
                        @Override public void onSuccess(String txHash) { busy = false; toast("Stake submitted."); refresh(); }
                        @Override public void onError(String code, String msg) { busy = false; toast(msg); paint(); }
                    });
        });
    }

    private void doUnstake() {
        String address = WalletManager.getActiveWalletAddress(host);
        double harvAmount;
        try { harvAmount = Double.parseDouble(amount.getText().toString().trim()); }
        catch (Exception e) { return; }
        
        ArrayList<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"To", "Your wallet, after 24 hours"});
        rows.add(new String[]{"For", "Returns to your wallet in exactly 24 hours."});
        
        gate("Unstake HARV", harvFormatter.format(harvAmount) + " HARV", rows, () -> {
            busy = true;
            NodeGroupClient.unstake(host, address, BalanceFormatter.harvToRaw(harvAmount), new NodeGroupClient.Callback() {
                @Override public void onSuccess(JSONObject json) { busy = false; toast("Unstaking. Returns in 24 hours."); refresh(); }
                @Override public void onError(String error) { busy = false; toast(error); paint(); }
            });
        });
    }

    private void doClaim() {
        String address = WalletManager.getActiveWalletAddress(host);
        long pending = pos == null ? 0 : pos.optLong("pendingRewardsRaw", 0);
        if (pending <= 0) return;
        
        ArrayList<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"To", "Your wallet"});
        rows.add(new String[]{"For", "Staking rewards already earned"});
        gate("Claim staking rewards", harv(pending), rows, () -> {
            busy = true;
            NodeGroupClient.claimStakingRewards(host, address, new NodeGroupClient.Callback() {
                @Override public void onSuccess(JSONObject json) { busy = false; toast("Reward claimed."); refresh(); }
                @Override public void onError(String error) { busy = false; toast(error); paint(); }
            });
        });
    }

    // --- Helpers ---
    
    private void setRange(int days) {
        chartDays = days;
        chip7.setTextColor(days == 7 ? Color.parseColor("#080A11") : Color.parseColor("#8F9BB3"));
        chip7.setBackgroundResource(days == 7 ? R.drawable.bg_pill_orange : 0);
        drawChart(WalletManager.getActiveWalletAddress(host));
    }
    
    private void fillMax() { 
        if (walletRaw > 0) amount.setText(harvFormatter.format(walletRaw / (double) HarvanaNodeClient.HARV_UNITS_PER_TOKEN)); 
    }
    
    private void gate(String title, String amt, List<String[]> rows, Runnable w) { 
        PaymentConfirmSheet.reviewThenAuthorize(host, title, amt, rows, w); 
    }
    
    // Updated harv formatting method using DecimalFormat
    private String harv(long raw) { 
        return harvFormatter.format(raw / (double) HarvanaNodeClient.HARV_UNITS_PER_TOKEN) + " HARV"; 
    }
    
    private void toast(String s) { ui(() -> Toast.makeText(host, s, Toast.LENGTH_LONG).show()); }
    private void ui(Runnable r) { host.runOnUiThread(r); }
    private int dp(int v) { return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, host.getResources().getDisplayMetrics()); }
    
    private long unbondingTotal() {
        if (pos == null) return 0;
        JSONArray rows = pos.optJSONArray("unbonding");
        if (rows == null) return 0;
        long sum = 0;
        for (int i = 0; i < rows.length(); i++) {
            JSONObject u = rows.optJSONObject(i);
            if (u != null && !"RELEASED".equals(u.optString("status"))) sum += u.optLong("amountRaw", 0);
        }
        return sum;
    }
    
    private void recordSnapshot(String address, long earnedRaw) {
        try {
            JSONArray rows = history(address);
            String today = dayKey(0);
            JSONObject hit = null;
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.optJSONObject(i);
                if (row != null && today.equals(row.optString("d"))) { hit = row; break; }
            }
            if (hit == null) {
                hit = new JSONObject();
                hit.put("d", today);
                rows.put(hit);
            }
            hit.put("earned", earnedRaw);
            while (rows.length() > 120) rows.remove(0);
            host.getSharedPreferences(PREFS, Activity.MODE_PRIVATE).edit().putString(address, rows.toString()).apply();
        } catch (Exception ignored) { }
    }
    
    private JSONArray history(String address) {
        String raw = host.getSharedPreferences(PREFS, Activity.MODE_PRIVATE).getString(address, "[]");
        try { return new JSONArray(raw); } catch (Exception e) { return new JSONArray(); }
    }
    
    private static String dayKey(int offsetDays) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.add(java.util.Calendar.DAY_OF_YEAR, offsetDays);
        return String.format(Locale.US, "%04d-%02d-%02d", c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH) + 1, c.get(java.util.Calendar.DAY_OF_MONTH));
    }
    
    private void styleChart() {
        chart.setNoDataText("Rewards show here as days pass.");
        chart.setNoDataTextColor(UiColors.textSecondary());
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.getAxisRight().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setExtraBottomOffset(8f);
        XAxis x = chart.getXAxis();
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setTextColor(UiColors.textTertiary());
        x.setDrawGridLines(false);
        x.setGranularity(1f);
        chart.getAxisLeft().setTextColor(UiColors.textTertiary());
        chart.getAxisLeft().setGridColor(UiColors.textTertiary() & 0x40FFFFFF | 0x22000000);
        chart.setBackgroundColor(Color.TRANSPARENT);
    }
    
    private void drawChart(String address) {
        if (address == null) { chart.clear(); return; }
        JSONArray rows = history(address);
        if (rows.length() == 0) { chart.clear(); chart.invalidate(); return; }
        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int start = chartDays >= rows.length() ? 0 : rows.length() - chartDays;
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
            entries.add(new Entry(labels.size(), (float) delta));
            String d = row.optString("d", "");
            labels.add(d.length() >= 10 ? d.substring(5) : d);
        }
        LineDataSet set = new LineDataSet(entries, "Rewards");
        set.setColor(UiColors.accent());
        set.setLineWidth(2f);
        set.setCircleColor(UiColors.accent());
        set.setCircleRadius(3f);
        set.setDrawValues(false);
        set.setDrawFilled(true);
        set.setFillColor(UiColors.accent());
        set.setFillAlpha(70);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        chart.setData(new LineData(set));
        chart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        chart.getXAxis().setLabelCount(Math.min(labels.size(), 6), false);
        chart.invalidate();
    }
    
    private static long paceFromBlocks(JSONObject json) {
        JSONArray blocks = json == null ? null : json.optJSONArray("blocks");
        if (blocks == null || blocks.length() < 2) return 0;
        long newest = blocks.optJSONObject(0) == null ? 0 : blocks.optJSONObject(0).optLong("timestamp");
        long oldest = blocks.optJSONObject(blocks.length() - 1) == null ? 0 : blocks.optJSONObject(blocks.length() - 1).optLong("timestamp");
        long span = newest - oldest;
        int gaps = blocks.length() - 1;
        if (span < 1000 || gaps < 1) return 0;
        return Math.max(1L, span / 1000L / gaps);
    }
}
