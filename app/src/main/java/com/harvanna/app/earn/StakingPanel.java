package com.harvanna.app.earn;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.button.MaterialButton;
import com.harvanna.app.R;
import com.harvanna.app.group.PaymentConfirmSheet;
import com.harvanna.app.sync.HarvannaNodeClient;
import com.harvanna.app.sync.NodeGroupClient;
import com.harvanna.app.tx.TransactionRouter;
import com.harvanna.app.utils.UiColors;
import com.harvanna.app.wallet.BalanceFormatter;
import com.harvanna.app.wallet.WalletManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Staking dashboard. Numbers come from the node. A stake is a signed transfer
 * to the vault. The day line uses the recent block pace and this wallet's share.
 */
public final class StakingPanel {
    private static final String PREFS = "harvanna_stake_history";

    private final AppCompatActivity host;
    private final TextView stakedView, unclaimedView, dailyPct, pool, apy, monthly, available, status;
    private final TextView posTotal, posUnclaimed, activeAmount, activeMonthly;
    private final TextView chip7, chip1, chip3, chipAll;
    private final View pageStake, pagePositions, activeCard;
    private final MaterialButton tabStake, tabPositions;
    private final LineChart chart;
    private final LinearLayout unbonding;
    private final EditText amount;
    private final MaterialButton stake, unstake, claim;
    private JSONObject info;
    private JSONObject pos;
    private long observedPace;
    private long walletRaw;
    private int chartDays = 7;
    private boolean busy;

    public StakingPanel(AppCompatActivity activity) {
        this.host = activity;
        stakedView = activity.findViewById(R.id.tv_staked);
        unclaimedView = activity.findViewById(R.id.tv_unclaimed);
        dailyPct = activity.findViewById(R.id.tv_daily_pct);
        pool = activity.findViewById(R.id.tv_pool);
        apy = activity.findViewById(R.id.tv_apy);
        monthly = activity.findViewById(R.id.tv_monthly);
        available = activity.findViewById(R.id.tv_available);
        status = activity.findViewById(R.id.tv_status);
        posTotal = activity.findViewById(R.id.pos_total);
        posUnclaimed = activity.findViewById(R.id.pos_unclaimed);
        activeAmount = activity.findViewById(R.id.tv_active_amount);
        activeMonthly = activity.findViewById(R.id.tv_active_monthly);
        activeCard = activity.findViewById(R.id.active_card);
        pageStake = activity.findViewById(R.id.page_stake);
        pagePositions = activity.findViewById(R.id.page_positions);
        tabStake = activity.findViewById(R.id.tab_stake);
        tabPositions = activity.findViewById(R.id.tab_positions);
        chip7 = activity.findViewById(R.id.chip_7d);
        chip1 = activity.findViewById(R.id.chip_1m);
        chip3 = activity.findViewById(R.id.chip_3m);
        chipAll = activity.findViewById(R.id.chip_all);
        chart = activity.findViewById(R.id.reward_chart);
        unbonding = activity.findViewById(R.id.unbonding_list);
        amount = activity.findViewById(R.id.et_amount);
        stake = activity.findViewById(R.id.btn_stake);
        unstake = activity.findViewById(R.id.btn_unstake);
        claim = activity.findViewById(R.id.btn_claim);
        amount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        stake.setOnClickListener(v -> doStake());
        unstake.setOnClickListener(v -> doUnstake());
        claim.setOnClickListener(v -> doClaim());
        activity.findViewById(R.id.btn_active_unstake).setOnClickListener(v -> askUnstake());
        activity.findViewById(R.id.btn_max).setOnClickListener(v -> fillMax());
        tabStake.setOnClickListener(v -> showPage(true));
        tabPositions.setOnClickListener(v -> showPage(false));
        chip7.setOnClickListener(v -> setRange(7));
        chip1.setOnClickListener(v -> setRange(30));
        chip3.setOnClickListener(v -> setRange(90));
        chipAll.setOnClickListener(v -> setRange(Integer.MAX_VALUE));
        styleChart();
        showPage(true);
    }

    public void reload() {
        refresh();
    }

    /** Headline APR on the Earn intro. Uses the same pace and pool size as the dashboard. */
    public static void fillHeadline(Activity activity, TextView apr) {
        if (activity == null || apr == null) return;
        final long[] pace = new long[]{0};
        final JSONObject[] info = new JSONObject[]{null};
        Runnable paint = () -> {
            if (info[0] == null || pace[0] <= 0) return;
            double units = (double) HarvannaNodeClient.HARV_UNITS_PER_TOKEN;
            long total = info[0].optLong("totalStakedRaw", 0);
            long perBlock = info[0].optLong("stakingRewardPerBlockRaw", 0);
            double poolHarv = total / units;
            double emissionPerDay = (perBlock / units) * (86400.0 / pace[0]);
            double aprPct = poolHarv > 0 ? (emissionPerDay * 365.0 / poolHarv) * 100.0 : 0;
            activity.runOnUiThread(() -> apr.setText(aprPct >= 100
                    ? String.format(Locale.US, "%.0f%% APR", aprPct)
                    : String.format(Locale.US, "%.2f%% APR", aprPct)));
        };
        NodeGroupClient.getStakingInfo(new NodeGroupClient.Callback() {
            @Override public void onSuccess(JSONObject json) { info[0] = json; paint.run(); }
            @Override public void onError(String error) { }
        });
        HarvannaNodeClient.getRecentBlocks(new HarvannaNodeClient.JsonCallback() {
            @Override public void onSuccess(JSONObject json) { pace[0] = paceFromBlocks(json); paint.run(); }
            @Override public void onError(String error) { }
        });
    }

    private void refresh() {
        NodeGroupClient.getStakingInfo(new NodeGroupClient.Callback() {
            @Override public void onSuccess(JSONObject json) { info = json; paint(); }
            @Override public void onError(String error) { ui(() -> status.setText("Network: " + error)); }
        });
        String address = WalletManager.getActiveWalletAddress(host);
        if (address == null || address.isEmpty()) {
            ui(() -> status.setText("Create a wallet before you stake."));
        } else {
            NodeGroupClient.readStakingPosition(address, new NodeGroupClient.Callback() {
                @Override public void onSuccess(JSONObject json) { pos = json; paint(); }
                @Override public void onError(String error) { ui(() -> status.setText(error)); }
            });
        }
        HarvannaNodeClient.getRecentBlocks(new HarvannaNodeClient.JsonCallback() {
            @Override public void onSuccess(JSONObject json) { observedPace = paceFromBlocks(json); paint(); }
            @Override public void onError(String error) { }
        });
        if (address != null && !address.isEmpty()) {
            HarvannaNodeClient.getWallet(address, new HarvannaNodeClient.JsonCallback() {
                @Override public void onSuccess(JSONObject json) { walletRaw = json.optLong("balance", 0); paint(); }
                @Override public void onError(String error) { }
            });
        }
    }

    private void showPage(boolean staking) {
        pageStake.setVisibility(staking ? View.VISIBLE : View.GONE);
        pagePositions.setVisibility(staking ? View.GONE : View.VISIBLE);
        tabStake.setAlpha(staking ? 1f : 0.55f);
        tabPositions.setAlpha(staking ? 0.55f : 1f);
    }

    private void setRange(int days) {
        chartDays = days;
        chip7.setTextColor(days == 7 ? UiColors.accent() : UiColors.textSecondary());
        chip1.setTextColor(days == 30 ? UiColors.accent() : UiColors.textSecondary());
        chip3.setTextColor(days == 90 ? UiColors.accent() : UiColors.textSecondary());
        chipAll.setTextColor(days == Integer.MAX_VALUE ? UiColors.accent() : UiColors.textSecondary());
        drawChart(WalletManager.getActiveWalletAddress(host));
    }

    private void fillMax() {
        if (walletRaw <= 0) { toast("No spendable HARV in this wallet."); return; }
        amount.setText(String.format(Locale.US, "%.4f", walletRaw / (double) HarvannaNodeClient.HARV_UNITS_PER_TOKEN));
    }

    private void askUnstake() {
        EditText input = new EditText(host);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint("Amount in HARV");
        new androidx.appcompat.app.AlertDialog.Builder(host)
                .setTitle("Unstake")
                .setMessage("This amount stops earning now and returns after 7 days.")
                .setView(input)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Unstake", (d, w) -> {
                    amount.setText(input.getText());
                    doUnstake();
                })
                .show();
    }

    private void paint() {
        ui(() -> {
            long staked = pos == null ? 0 : pos.optLong("stakedRaw", 0);
            long pending = pos == null ? 0 : pos.optLong("pendingRewardsRaw", 0);
            long claimed = pos == null ? 0 : pos.optLong("totalClaimedRaw", 0);
            double units = (double) HarvannaNodeClient.HARV_UNITS_PER_TOKEN;
            long total = info == null ? 0 : info.optLong("totalStakedRaw", 0);
            long perBlock = info == null ? 0 : info.optLong("stakingRewardPerBlockRaw", 0);
            long pace = observedPace > 0 ? observedPace : (info == null ? 0 : info.optLong("observedBlockSeconds", 0));
            double poolHarv = total / units;
            double stakedHarv = staked / units;
            stakedView.setText(harv(staked));
            unclaimedView.setText("Unclaimed  " + harv(pending));
            posUnclaimed.setText(harv(pending));
            activeAmount.setText(harv(staked));
            activeCard.setVisibility(staked > 0 ? View.VISIBLE : View.GONE);
            available.setText("Available  " + harv(walletRaw));
            if (info != null) {
                pool.setText(String.format(Locale.US, "%.4f HARV per block  ·  %d active stakers",
                        perBlock / units, info.optInt("stakerCount", 0)));
            }
            double pct = dailyPercent(staked, pending, claimed, pace, perBlock, poolHarv, stakedHarv);
            double emissionPerDay = pace > 0 ? (perBlock / units) * (86400.0 / pace) : 0;
            double perDay = (pace > 0 && poolHarv > 0) ? stakedHarv * (emissionPerDay / poolHarv) : 0;
            double aprPct = poolHarv > 0 ? (emissionPerDay * 365.0 / poolHarv) * 100.0 : 0;
            apy.setText(aprPct >= 100
                    ? String.format(Locale.US, "Current est. pool APY  %.0f%%", aprPct)
                    : String.format(Locale.US, "Current est. pool APY  %.2f%%", aprPct));
            // Create a formatter that limits to 4 decimal places and dynamically drops trailing zeros
java.text.DecimalFormat df = new java.text.DecimalFormat("#.####");

monthly.setText(String.format(Locale.US, "~ %s monthly, at your current share", df.format(perDay * 30)));
activeMonthly.setText(String.format(Locale.US, "Est. monthly reward ~ %s", df.format(perDay * 30)));
            long queued = unbondingTotal();
            posTotal.setText(harv(staked + queued));
            if (pace <= 0 || staked <= 0) {
                dailyPct.setText(staked <= 0 ? "Stake to start earning" : "Checking the block pace…");
                dailyPct.setTextColor(UiColors.textSecondary());
            } else {
                dailyPct.setText(String.format(Locale.US, "%+.2f%% today", pct));
                dailyPct.setTextColor(pct >= 0 ? UiColors.success() : UiColors.error());
            }
            String address = WalletManager.getActiveWalletAddress(host);
            if (address != null && staked > 0) recordSnapshot(address, pending + claimed);
            drawChart(address);
            drawUnbonding();
            claim.setEnabled(!busy && pending > 0);
            stake.setEnabled(!busy);
            unstake.setEnabled(!busy && staked > 0);
        });
    }

    /** ((rewards now − rewards yesterday) / stake) × 100. Falls back to today's pace when yesterday is not stored yet. */
    private double dailyPercent(long staked, long pending, long claimed, long pace, long perBlock, double poolHarv, double stakedHarv) {
        String address = WalletManager.getActiveWalletAddress(host);
        long yesterday = address == null ? -1 : earnedOn(address, dayKey(-1));
        long now = pending + claimed;
        if (yesterday >= 0 && staked > 0) return (now - yesterday) * 100.0 / staked;
        if (pace > 0 && poolHarv > 0 && stakedHarv > 0) {
            double emissionPerDay = (perBlock / (double) HarvannaNodeClient.HARV_UNITS_PER_TOKEN) * (86400.0 / pace);
            double perDay = stakedHarv * (emissionPerDay / poolHarv);
            return perDay / stakedHarv * 100.0;
        }
        return 0;
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

    private long earnedOn(String address, String day) {
        JSONArray rows = history(address);
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.optJSONObject(i);
            if (row != null && day.equals(row.optString("d"))) return row.optLong("earned", -1);
        }
        return -1;
    }

    private static String dayKey(int offsetDays) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.add(java.util.Calendar.DAY_OF_YEAR, offsetDays);
        return String.format(Locale.US, "%04d-%02d-%02d", c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH) + 1, c.get(java.util.Calendar.DAY_OF_MONTH));
    }

    private void styleChart() {
        chart.setNoDataText("Rewards show here as days pass on this phone.");
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
            double delta = previous < 0 ? 0 : Math.max(0, earned - previous) / (double) HarvannaNodeClient.HARV_UNITS_PER_TOKEN;
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

    private void drawUnbonding() {
        unbonding.removeAllViews();
        if (pos == null) return;
        JSONArray rows = pos.optJSONArray("unbonding");
        if (rows == null || rows.length() == 0) {
            TextView empty = new TextView(host);
            empty.setText("Nothing is unbonding.");
            empty.setTextColor(UiColors.textSecondary());
            empty.setPadding(0, dp(8), 0, 0);
            unbonding.addView(empty);
            return;
        }
        long now = System.currentTimeMillis();
        for (int i = 0; i < rows.length(); i++) {
            JSONObject u = rows.optJSONObject(i);
            if (u == null || "RELEASED".equals(u.optString("status"))) continue;
            long release = u.optLong("releaseTimestamp", 0);
            long remain = release - now;
            boolean ready = remain <= 0 || "RELEASING".equals(u.optString("status"));
            TextView row = new TextView(host);
            row.setPadding(0, dp(10), 0, dp(4));
            if (ready) {
                row.setText(harv(u.optLong("amountRaw", 0)) + "\nReady. The network is returning this to your wallet. Not earning.");
                row.setTextColor(UiColors.accent());
            } else {
                row.setText(harv(u.optLong("amountRaw", 0)) + "\nUnbonding  ·  " + remainText(remain) + "\nNot earning rewards.");
                row.setTextColor(UiColors.textPrimary());
            }
            unbonding.addView(row);
        }
    }

    private long unbondingTotal() {
        if (pos == null) return 0;
        JSONArray rows = pos.optJSONArray("unbonding");
        if (rows == null) return 0;
        long sum = 0;
        for (int i = 0; i < rows.length(); i++) {
            JSONObject u = rows.optJSONObject(i);
            if (u == null || "RELEASED".equals(u.optString("status"))) continue;
            sum += u.optLong("amountRaw", 0);
        }
        return sum;
    }

    private static String remainText(long ms) {
        long hours = Math.max(1, ms / 3_600_000L);
        long days = hours / 24;
        long remHours = hours % 24;
        if (days > 0) return days + (days == 1 ? " day, " : " days, ") + remHours + " hours remaining";
        return hours + " hours remaining";
    }

    private void doStake() {
        if (info == null) return;
        String address = WalletManager.getActiveWalletAddress(host);
        String vault = info.optString("vaultAddress", "");
        double harvAmount;
        try { harvAmount = Double.parseDouble(amount.getText().toString().trim()); }
        catch (Exception e) { toast("Enter an amount in HARV."); return; }
        long already = pos == null ? 0 : pos.optLong("stakedRaw", 0);
        long min = info.optLong("minStakeRaw", HarvannaNodeClient.HARV_UNITS_PER_TOKEN);
        if (vault.isEmpty() || address == null || address.isEmpty()) { toast("Wallet or vault is not ready."); return; }
        if (already < min && BalanceFormatter.harvToRaw(harvAmount) < min) { toast("The first stake must be at least 1 HARV."); return; }
        ArrayList<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"To", vault});
        rows.add(new String[]{"For", "It starts earning now. Unstaking later returns after 7 days."});
        rows.add(new String[]{"From", "Your wallet"});
        rows.add(new String[]{"Network fee", "Taken with the stake"});
        gate("Stake HARV", String.format(Locale.US, "%.4f HARV", harvAmount), rows, () -> {
            busy = true;
            paint();
            TransactionRouter.routeTransaction(host, address, vault, BigDecimal.valueOf(harvAmount), null, "STAKE",
                    new TransactionRouter.RouterCallback() {
                        @Override public void onSuccess(String txHash) {
                            com.harvanna.app.trust.AppActivity.note(host, "STAKE", "Staked HARV");
                            busy = false; toast("Stake submitted."); refresh();
                        }
                        @Override public void onError(String code, String message) { busy = false; toast(message); paint(); }
                    });
        });
    }

    private void doUnstake() {
        String address = WalletManager.getActiveWalletAddress(host);
        double harvAmount;
        try { harvAmount = Double.parseDouble(amount.getText().toString().trim()); }
        catch (Exception e) { toast("Enter how much HARV to unstake."); return; }
        if (harvAmount <= 0 || address == null) return;
        ArrayList<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"To", "Your wallet, after 7 days"});
        rows.add(new String[]{"For", "It stops earning now and returns to your wallet after 7 days."});
        rows.add(new String[]{"Network fee", "Taken when the unstake is sent"});
        gate("Unstake HARV", String.format(Locale.US, "%.4f HARV", harvAmount), rows, () -> {
            busy = true;
            NodeGroupClient.unstake(host, address, BalanceFormatter.harvToRaw(harvAmount), new NodeGroupClient.Callback() {
                @Override public void onSuccess(JSONObject json) {
                    com.harvanna.app.trust.AppActivity.note(host, "UNSTAKE", "Unstaked HARV");
                    busy = false; toast("Unstaking. It returns after 7 days."); refresh();
                }
                @Override public void onError(String error) { busy = false; toast(error); paint(); }
            });
        });
    }

    private void doClaim() {
        String address = WalletManager.getActiveWalletAddress(host);
        if (address == null || address.isEmpty()) return;
        long pending = pos == null ? 0 : pos.optLong("pendingRewardsRaw", 0);
        if (pending <= 0) { toast("Nothing to claim yet."); return; }
        ArrayList<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"To", "Your wallet"});
        rows.add(new String[]{"For", "Staking rewards already earned"});
        rows.add(new String[]{"Network fee", "Taken when the claim is sent"});
        gate("Claim staking rewards", harv(pending), rows, () -> {
            busy = true;
            NodeGroupClient.claimStakingRewards(host, address, new NodeGroupClient.Callback() {
                @Override public void onSuccess(JSONObject json) {
                    com.harvanna.app.trust.AppActivity.note(host, "CLAIM", "Claimed staking rewards");
                    busy = false; toast("Reward claimed."); refresh();
                }
                @Override public void onError(String error) { busy = false; toast(error); paint(); }
            });
        });
    }

    private void gate(String title, String amountText, List<String[]> rows, Runnable work) {
        PaymentConfirmSheet.reviewThenAuthorize(host, title, amountText, rows, work);
    }

    private String harv(long raw) {
        return String.format(Locale.US, "%.4f HARV", raw / (double) HarvannaNodeClient.HARV_UNITS_PER_TOKEN);
    }

    private void toast(String s) { ui(() -> Toast.makeText(host, s, Toast.LENGTH_LONG).show()); }
    private void ui(Runnable r) { host.runOnUiThread(r); }
    private int dp(int v) { return Math.round(v * host.getResources().getDisplayMetrics().density); }
}
