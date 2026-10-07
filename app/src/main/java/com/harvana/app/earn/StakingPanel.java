package com.harvana.app.earn;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.harvana.app.CoinInfo;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public final class StakingPanel {
    private static final String PREFS = "harvana_stake_history";

    private final AppCompatActivity host;

    private final DecimalFormat harvFormatter = new DecimalFormat("0.####", DecimalFormatSymbols.getInstance(Locale.US));

    // Page 1 UI
    private final TextView overviewStaked, dailyPct, overviewUnclaimed;
    private final TextView tvPool, tvEstRewardInput;
    private final LineChart chart;
    private final EditText amount;
    private final MaterialButton stake;
    private final TextView available;
    private final TextView chip1d, chip3d, chip7d, chip1m, chip3m, chipAll;
    private final Spinner spAsset;

    // Lock Selection UI
    private final MaterialCardView cardLockFree, cardLock7, cardLock30, cardLock90;
    private int selectedLockDays = 0;

    // Page 2 UI
    private final TextView posTotal, posDailyPct, gridTotal, gridUnclaimed, gridApy;
    private final TextView unbondCount;
    private final MaterialButton claim;
    private final LinearLayout stakesList;
    private final TextView tabLive, tabClosed, liveEmpty;
    private final View liveSection, closedSection;
    private final TextView claimSub;
    private final LinearLayout unbondingList;

    private final View pageStake, pagePositions;
    private final TextView tabStake, tabPositions;

    private JSONObject info;
    private JSONObject pos;
    long observedPace;
    private long walletRaw;
    private int chartDays = 7;

    /** Colors come from the app theme, so the staking screens follow day and night like the rest of the app. */
    private int themeColor(int resId) { return ContextCompat.getColor(host, resId); }

    private static String dateTime(long ms) {
        return new java.text.SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()).format(new java.util.Date(ms));
    }

    /** When the next claim opens: 24 h after the last claim, or after the first stake if nothing was claimed yet. 0 = no position. */
    private long nextClaimAt() {
        if (pos == null) return 0;
        return StakingRules.nextClaimAt(pos.optLong("stakedRaw", 0), pos.optLong("lastClaimedTimestamp", 0),
                pos.optLong("firstStakedTimestamp", 0));
    }

    /** Unlock time of locked principal, or 0 when it is flexible or the lock has ended. */
    private long lockedUntil() {
        if (pos == null) return 0;
        return StakingRules.lockedUntil(pos.optLong("stakedRaw", 0), pos.optLong("unlockTimestamp", 0), System.currentTimeMillis());
    }

    /** Turns the node's refusal codes into sentences with the date the user needs. */
    private String friendlyStakingError(String raw) {
        String code = StakingRules.refusalCode(raw);
        if (code == null) return raw;
        if ("STAKE_LOCKED".equals(code)) {
            long until = pos == null ? 0 : pos.optLong("unlockTimestamp", 0);
            return until > 0 ? "Your principal is locked until " + dateTime(until) + ". Rewards can still be claimed."
                    : "Your principal is still locked. Rewards can still be claimed.";
        }
        if ("CLAIM_AVAILABLE_EVERY_24_HOURS".equals(code)) {
            long at = nextClaimAt();
            return at > 0 ? "You can claim once every 24 hours. Your next claim opens " + dateTime(at) + "."
                    : "You can claim once every 24 hours.";
        }
        if ("REMAINING_STAKE_BELOW_MINIMUM".equals(code)) return "Leave at least 1 HARV staked, or unstake everything.";
        if ("NOTHING_TO_CLAIM".equals(code)) return "Nothing to claim yet. Rewards drip into Claim through the day.";
        return "You cannot unstake more than you have staked.";
    }
    private boolean busy;

    private final ArrayList<HashMap<String, Object>> walletAssets = new ArrayList<>();
    private final ArrayList<String> assetDisplayNames = new ArrayList<>();
    private ArrayAdapter<String> assetAdapter;

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

        overviewStaked = activity.findViewById(R.id.tv_overview_staked);
        dailyPct = activity.findViewById(R.id.tv_daily_pct);
        overviewUnclaimed = activity.findViewById(R.id.tv_overview_unclaimed);
        tvPool = activity.findViewById(R.id.tv_pool);
        tvEstRewardInput = activity.findViewById(R.id.tv_est_reward_input);
        chart = activity.findViewById(R.id.reward_chart);
        amount = activity.findViewById(R.id.et_amount);
        stake = activity.findViewById(R.id.btn_stake);
        available = activity.findViewById(R.id.tv_available);
        spAsset = activity.findViewById(R.id.spAsset);

        chip1d = activity.findViewById(R.id.chip_1d);
        chip3d = activity.findViewById(R.id.chip_3d);
        chip7d = activity.findViewById(R.id.chip_7d);
        chip1m = activity.findViewById(R.id.chip_1m);
        chip3m = activity.findViewById(R.id.chip_3m);
        chipAll = activity.findViewById(R.id.chip_all);

        cardLockFree = activity.findViewById(R.id.card_lock_free);
        cardLock7 = activity.findViewById(R.id.card_lock_7);
        cardLock30 = activity.findViewById(R.id.card_lock_30);
        cardLock90 = activity.findViewById(R.id.card_lock_90);

        posTotal = activity.findViewById(R.id.tv_pos_total);
        posDailyPct = activity.findViewById(R.id.tv_pos_daily_pct);
        gridTotal = activity.findViewById(R.id.tv_grid_total);
        gridUnclaimed = activity.findViewById(R.id.tv_grid_unclaimed);
        gridApy = activity.findViewById(R.id.tv_grid_apy);
        unbondCount = activity.findViewById(R.id.tv_unbond_count);
        claim = activity.findViewById(R.id.btn_claim);
        claimSub = activity.findViewById(R.id.tv_claim_sub);
        unbondingList = activity.findViewById(R.id.unbonding_list);

        pageStake = activity.findViewById(R.id.page_stake);
        pagePositions = activity.findViewById(R.id.page_positions);
        stakesList = activity.findViewById(R.id.stakes_list);
        tabLive = activity.findViewById(R.id.tab_live);
        tabClosed = activity.findViewById(R.id.tab_closed);
        liveEmpty = activity.findViewById(R.id.tv_live_empty);
        liveSection = activity.findViewById(R.id.live_section);
        closedSection = activity.findViewById(R.id.closed_section);
        tabStake = activity.findViewById(R.id.tab_stake);
        tabPositions = activity.findViewById(R.id.tab_positions);

        setupAssetSpinner();
        setupLockCards();

        stake.setOnClickListener(v -> doStake());
        claim.setOnClickListener(v -> doClaim());
        tabLive.setOnClickListener(v -> showLive(true));
        tabClosed.setOnClickListener(v -> showLive(false));
        activity.findViewById(R.id.btn_max).setOnClickListener(v -> fillMax());

        amount.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { updateEstReward(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        tabStake.setOnClickListener(v -> showPage(true));
        tabPositions.setOnClickListener(v -> showPage(false));
        chip1d.setOnClickListener(v -> setRange(1));
        chip3d.setOnClickListener(v -> setRange(3));
        chip7d.setOnClickListener(v -> setRange(7));
        chip1m.setOnClickListener(v -> setRange(30));
        chip3m.setOnClickListener(v -> setRange(90));
        chipAll.setOnClickListener(v -> setRange(Integer.MAX_VALUE));

        styleChart();
        showLive(true);
        showPage(true);
        timerHandler.post(timerRunnable);
    }

    public void reload() { refresh(); }

    public static void fillHeadline(Activity activity, TextView apr) {
        if (activity == null || apr == null) return;
        NodeGroupClient.getStakingInfo(new NodeGroupClient.Callback() {
            @Override public void onSuccess(JSONObject json) {
                long pool = json == null ? 0 : json.optLong("rewardPoolBalanceRaw", 0);
                activity.runOnUiThread(() -> {
                    DecimalFormat fmt = new DecimalFormat("0.####", DecimalFormatSymbols.getInstance(Locale.US));
                    apr.setText(fmt.format(pool / (double) HarvanaNodeClient.HARV_UNITS_PER_TOKEN) + " HARV in the reward pool");
                });
            }
            @Override public void onError(String error) { }
        });
    }

    private void setupAssetSpinner() {
        assetAdapter = new ArrayAdapter<String>(
                host, com.harvana.app.R.layout.spinner_item, assetDisplayNames) {
            @NonNull
            @Override
            public View getView(int pos, View convertView, @NonNull ViewGroup parent) {
                TextView tv = (TextView) super.getView(pos, convertView, parent);
                tv.setTextColor(themeColor(R.color.colorTextPrimary));
                tv.setTextSize(18);
                return tv;
            }

            @Override
            public View getDropDownView(int pos, View convertView, @NonNull ViewGroup parent) {
                TextView tv = (TextView) super.getDropDownView(pos, convertView, parent);
                tv.setTextColor(themeColor(R.color.colorTextPrimary));
                tv.setTextSize(16);
                tv.setPadding(24, 24, 24, 24);
                tv.setBackgroundColor(themeColor(R.color.colorSurface));
                return tv;
            }
        };
        assetAdapter.setDropDownViewResource(com.harvana.app.R.layout.spinner_dropdown_item);
        spAsset.setAdapter(assetAdapter);
    }

    private void setupLockCards() {
        View.OnClickListener lockClick = v -> {
        int quiet = ContextCompat.getColor(host, R.color.colorStrokeBase);
        int accent = ContextCompat.getColor(host, R.color.colorHarvOrange);
        cardLockFree.setStrokeColor(quiet);
        cardLock7.setStrokeColor(quiet);
        cardLock30.setStrokeColor(quiet);
        cardLock90.setStrokeColor(quiet);
        ((MaterialCardView) v).setStrokeColor(accent);

            if (v == cardLockFree) selectedLockDays = 0;
            if (v == cardLock7) selectedLockDays = 7;
            if (v == cardLock30) selectedLockDays = 30;
            if (v == cardLock90) selectedLockDays = 90;

            updateEstReward();
        };

        cardLockFree.setOnClickListener(lockClick);
        cardLock7.setOnClickListener(lockClick);
        cardLock30.setOnClickListener(lockClick);
        cardLock90.setOnClickListener(lockClick);
    }

    private void updateEstReward() {
        double weight = StakingRules.weight(selectedLockDays);
        String holdText = selectedLockDays <= 0 ? "stays until you unstake" : selectedLockDays + "-day lock on the principal";
        double harvAmount = 0;
        try { harvAmount = Double.parseDouble(amount.getText().toString().trim()); } catch (Exception ignored) { }
        double[] r = rates();
        String text;
        if (harvAmount > 0 && r[0] > 0) {
            double apr = StakingRules.aprFor(harvAmount, weight, r[0], r[1]);
            double fullApr = StakingRules.aprFor(harvAmount, weight, StakingRules.FULL_POOL_DAILY_HARV * 365.0, r[1]);
            text = String.format(Locale.US, "About %.2f%% APR (%.2f%% APY if you restake daily) at %.2fx weight: %s HARV in 30 days, %s in a year. "
                            + "When the reward pool is full this becomes about %.2f%% APR. %s.",
                    apr * 100, StakingRules.apy(apr) * 100, weight,
                    fmt2(StakingRules.earned(harvAmount, apr, 30)), fmt2(StakingRules.earned(harvAmount, apr, 365)),
                    fullApr * 100, capitalise(holdText));
        } else {
            text = String.format(Locale.US, "%.2fx weight. Each day 0.05%% of the pool drips into Claim. %s.", weight, capitalise(holdText));
        }
        tvEstRewardInput.setText(text + " Estimates move with the pool and the total staked. Claims open once every 24 hours.");
    }

    /** {yearly drip in HARV, total weighted stake} from the node's /info and the observed block pace. */
    private double[] rates() {
        if (info == null) return new double[]{0, 0};
        double pool = info.optLong("rewardPoolBalanceRaw", 0) / (double) HarvanaNodeClient.HARV_UNITS_PER_TOKEN;
        double secs = info.optDouble("observedBlockSeconds", observedPace);
        double drip = StakingRules.yearlyDrip(pool, secs);
        double baseApr = info.optLong("estimatedBaseAprBps", 0) / 10_000.0;
        return new double[]{drip, StakingRules.totalWeight(drip, baseApr)};
    }

    private static String fmt2(double v) { return String.format(Locale.US, "%,.4f", v); }
    private static String capitalise(String t) { return t.isEmpty() ? t : Character.toUpperCase(t.charAt(0)) + t.substring(1); }

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
                @Override public void onSuccess(JSONObject json) { 
                    ui(() -> {
                        walletRaw = json.optLong("balance", 0);
                        double harvBalance = HarvanaNodeClient.toHarvDisplay(walletRaw);

                        walletAssets.clear();
                        assetDisplayNames.clear();

                        HashMap<String, Object> native_ = new HashMap<>();
                        native_.put("symbol", CoinInfo.ticker());
                        native_.put("balance", BigDecimal.valueOf(harvBalance));
                        walletAssets.add(native_);
                        assetDisplayNames.add((CoinInfo.ticker()));

                        assetAdapter.notifyDataSetChanged();
                        if (!walletAssets.isEmpty()) spAsset.setSelection(0);
                        paint();
                    });
                }
                @Override public void onError(String error) { }
            });
        }
        HarvanaNodeClient.getRecentBlocks(new HarvanaNodeClient.JsonCallback() {
            @Override public void onSuccess(JSONObject json) { observedPace = paceFromBlocks(json); paint(); }
            @Override public void onError(String error) { }
        });
    }

    /** Live stakes and closed (unstaked / withdrawn) ones sit on separate tabs so the page stays short. */
    private void showLive(boolean live) {
        liveSection.setVisibility(live ? View.VISIBLE : View.GONE);
        closedSection.setVisibility(live ? View.GONE : View.VISIBLE);
        int muted = ContextCompat.getColor(host, R.color.colorTextMuted);
        int onAccent = ContextCompat.getColor(host, R.color.colorTextPrimaryWhite);
        tabLive.setBackgroundResource(live ? R.drawable.bg_tab_active : 0);
        tabLive.setTextColor(live ? onAccent : muted);
        tabClosed.setBackgroundResource(live ? 0 : R.drawable.bg_tab_active);
        tabClosed.setTextColor(live ? muted : onAccent);
    }

    private void showPage(boolean staking) {
        pageStake.setVisibility(staking ? View.VISIBLE : View.GONE);
        pagePositions.setVisibility(staking ? View.GONE : View.VISIBLE);

        int muted = ContextCompat.getColor(host, R.color.colorTextMuted);
        int onAccent = ContextCompat.getColor(host, R.color.colorTextPrimaryWhite);
        tabStake.setBackgroundResource(staking ? R.drawable.bg_tab_active : 0);
        tabStake.setTextColor(staking ? onAccent : muted);
        tabPositions.setBackgroundResource(staking ? 0 : R.drawable.bg_tab_active);
        tabPositions.setTextColor(staking ? muted : onAccent);
    }

    private void paint() {
        ui(() -> {
            long staked = pos == null ? 0 : pos.optLong("stakedRaw", 0);
            long pending = pos == null ? 0 : pos.optLong("pendingRewardsRaw", 0);
            long claimed = pos == null ? 0 : pos.optLong("totalClaimedRaw", 0);
            long total = info == null ? 0 : info.optLong("totalStakedRaw", 0);
            long shareBps = pos != null && pos.has("shareBps") ? pos.optLong("shareBps", 0) : (total > 0 ? staked * 10_000L / total : 0);

            overviewStaked.setText(harv(staked));
            overviewUnclaimed.setText(harv(pending));
            available.setText("Available " + harv(walletRaw));
            if (info != null) tvPool.setText(String.format(Locale.US, "● Reward pool %s · %d stakers · 0.05%% drips into Claim each day",
                    harv(info.optLong("rewardPoolBalanceRaw", 0)), info.optInt("stakerCount", 0)));
            dailyPct.setText(staked <= 0 ? "Stake to start earning" : String.format(Locale.US, "You hold %.2f%% of the stake", shareBps / 100.0));

            long queued = unbondingTotal();
            posTotal.setText(harv(staked + queued));
            posDailyPct.setText(dailyPct.getText());
            gridTotal.setText(harv(staked));
            gridUnclaimed.setText(harv(pending));
            // Your rate: the flexible rate times your stake-weighted lock weight.
            double baseApr = info == null ? 0 : info.optLong("estimatedBaseAprBps", 0) / 10_000.0;
            double weighted = 0;
            for (Stake st : stakes()) weighted += st.amountRaw * ((st.multiplierBps <= 0 ? 10000 : st.multiplierBps) / 10_000.0);
            double myApr = staked > 0 ? baseApr * weighted / staked : baseApr;
            gridApy.setText(myApr <= 0 ? "--" : String.format(Locale.US, "%.2f%% · %.2f%%", myApr * 100, StakingRules.apy(myApr) * 100));
            updateEstReward();

            renderStakes();

            String address = WalletManager.getActiveWalletAddress(host);
            if (address != null && staked > 0) recordSnapshot(address, pending + claimed);
            drawChart(address);

            stake.setEnabled(!busy);

            setupUnbondingViews();
            updateLiveTimers();
        });
    }

    private void updateLiveTimers() {
        if (pos == null) return;
        long staked = pos.optLong("stakedRaw", 0);
        long pending = pos.optLong("pendingRewardsRaw", 0);

        if (staked == 0 && pending == 0) {
            claim.setEnabled(false);
            claim.setText("Claim all");
            claimSub.setText("Stake HARV to start earning. The stake stays until you unstake.");
        } else {
            long now = System.currentTimeMillis();
            long opensAt = nextClaimAt();
            boolean waiting = opensAt > now;
            String rule = "You can claim once every 24 hours.";
            if (waiting) {
                claim.setEnabled(false);
                claim.setText("Claim in " + formatTimerPrecise(opensAt - now));
                claimSub.setText((pending > 0 ? harv(pending) + " has dripped so far. " : "")
                        + "Next claim opens " + dateTime(opensAt) + ". " + rule);
            } else if (pending > 0) {
                claim.setEnabled(!busy);
                claim.setText("Claim");
                claimSub.setText(harv(pending) + " from all your stakes is ready. One claim pays them together and leaves the stakes in place. " + rule);
            } else {
                claim.setEnabled(false);
                claim.setText("Claim");
                claimSub.setText("Nothing has dripped into Claim yet. The pool adds to it through the day. " + rule);
            }
        }
    }

    private static String formatTimerPrecise(long ms) {
        if (ms <= 0) return "00h 00m";
        long totalSecs = ms / 1000;
        return String.format(Locale.US, "%02dh %02dm", totalSecs / 3600, (totalSecs % 3600) / 60);
    }

    private View createUnbondingRowView(JSONObject u) {
        ConstraintLayout cl = new ConstraintLayout(host);
        cl.setPadding(dp(12), dp(12), dp(12), dp(12));
        cl.setTag(u);

        cl.setClickable(true);
        cl.setFocusable(true);

        // Dynamically resolve theme attribute to prevent Resources$NotFoundException
        TypedValue outValue = new TypedValue();
        host.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
        cl.setBackgroundResource(outValue.resourceId);

        cl.setOnClickListener(v -> {
            JSONObject unbondData = (JSONObject) v.getTag();
            showUnbondDetailsSheet(unbondData);
        });

        long now = System.currentTimeMillis();
        long release = u.optLong("releaseTimestamp", 0);
        boolean ready = (release - now) <= 0 || "RELEASING".equals(u.optString("status"));

        TextView badge = new TextView(host);
        badge.setId(View.generateViewId());
        badge.setTextSize(11);
        badge.setTypeface(null, android.graphics.Typeface.BOLD);
        if (ready) {
            badge.setText("● Ready");
            badge.setTextColor(themeColor(R.color.colorSuccess));
        } else {
            badge.setText("● Unbonding");
            badge.setTextColor(themeColor(R.color.colorAccent));
        }

        TextView amountTv = new TextView(host);
        amountTv.setId(View.generateViewId());
        amountTv.setText(harv(u.optLong("amountRaw", 0)));
        amountTv.setTextColor(themeColor(R.color.colorTextPrimary));
        amountTv.setTextSize(15);
        amountTv.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView subTv = new TextView(host);
        subTv.setId(View.generateViewId());
        subTv.setTextColor(themeColor(R.color.colorTextTertiary));
        subTv.setTextSize(11);
        subTv.setText(ready ? "Cooldown Complete" : "Remaining: " + formatTimerPrecise(release - now));

        MaterialButton btn = new MaterialButton(host, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        btn.setId(View.generateViewId());
        btn.setText("Withdraw");
        btn.setAllCaps(false);
        btn.setTextSize(11);
        btn.setTextColor(themeColor(R.color.colorAccent));
        btn.setVisibility(ready ? View.VISIBLE : View.GONE);
        btn.setOnClickListener(v -> doManualSweep());

        cl.addView(badge);
        cl.addView(amountTv);
        cl.addView(subTv);
        cl.addView(btn);

        ConstraintSet cs = new ConstraintSet();
        cs.clone(cl);
        cs.connect(badge.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
        cs.connect(badge.getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
        cs.connect(amountTv.getId(), ConstraintSet.TOP, badge.getId(), ConstraintSet.BOTTOM, dp(2));
        cs.connect(amountTv.getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
        cs.connect(subTv.getId(), ConstraintSet.TOP, amountTv.getId(), ConstraintSet.BOTTOM, dp(2));
        cs.connect(subTv.getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
        cs.connect(btn.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
        cs.connect(btn.getId(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);
        cs.connect(btn.getId(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);
        cs.applyTo(cl);

        return cl;
    }

    private void setupUnbondingViews() {
        unbondingList.removeAllViews();
        if (pos == null) return;
        JSONArray rows = pos.optJSONArray("unbonding");
        if (rows == null || rows.length() == 0) {
            unbondCount.setText("● 0 In Progress");
            TextView none = new TextView(host);
            none.setText("Nothing closed yet. Stakes you unstake show here while they return and after they are withdrawn.");
            none.setTextColor(themeColor(R.color.colorTextMuted));
            none.setTextSize(13);
            unbondingList.addView(none);
            return;
        }

        List<JSONObject> activeRows = new ArrayList<>();
        for (int i = 0; i < rows.length(); i++) {
            JSONObject u = rows.optJSONObject(i);
            if (u != null && !"RELEASED".equals(u.optString("status"))) {
                activeRows.add(u);
            }
        }

        unbondCount.setText("● " + activeRows.size() + " In Progress");
        if (activeRows.isEmpty()) { addWithdrawnRows(rows); return; }

        MaterialCardView containerCard = new MaterialCardView(host);
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(0, dp(8), 0, dp(8));
        containerCard.setLayoutParams(cardLp);
        containerCard.setCardBackgroundColor(themeColor(R.color.colorSurface));
        containerCard.setRadius(dp(16));
        containerCard.setStrokeColor(themeColor(R.color.colorCardStroke));
        containerCard.setStrokeWidth(dp(1));

        LinearLayout cardInner = new LinearLayout(host);
        cardInner.setOrientation(LinearLayout.VERTICAL);
        cardInner.setPadding(dp(16), dp(16), dp(16), dp(16));

        int limit = Math.min(2, activeRows.size());
        for (int i = 0; i < limit; i++) {
            cardInner.addView(createUnbondingRowView(activeRows.get(i)));
            if (i < limit - 1) {
                View divider = new View(host);
                LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
                divLp.setMargins(0, dp(6), 0, dp(6));
                divider.setLayoutParams(divLp);
                divider.setBackgroundColor(themeColor(R.color.colorCardStroke));
                cardInner.addView(divider);
            }
        }

        if (activeRows.size() > 2) {
            TextView viewMoreBtn = new TextView(host);
            viewMoreBtn.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            viewMoreBtn.setPadding(0, dp(12), 0, dp(4));
            viewMoreBtn.setGravity(Gravity.CENTER);
            viewMoreBtn.setText("View All Withdrawals (" + activeRows.size() + ") >");
            viewMoreBtn.setTextColor(themeColor(R.color.colorAccent));
            viewMoreBtn.setTextSize(12);
            viewMoreBtn.setTypeface(null, android.graphics.Typeface.BOLD);
            viewMoreBtn.setOnClickListener(v -> showFullUnbondingBottomSheet(activeRows));
            cardInner.addView(viewMoreBtn);
        }

        containerCard.addView(cardInner);
        unbondingList.addView(containerCard);
        addWithdrawnRows(rows);
    }

    /** Completed withdrawals, newest first, under the ones still returning. */
    private void addWithdrawnRows(JSONArray rows) {
        List<JSONObject> done = new ArrayList<>();
        for (int i = 0; i < rows.length(); i++) {
            JSONObject u = rows.optJSONObject(i);
            if (u != null && "RELEASED".equals(u.optString("status"))) done.add(u);
        }
        if (done.isEmpty()) return;
        java.util.Collections.sort(done, (x, y) -> Long.compare(y.optLong("releaseTimestamp", 0), x.optLong("releaseTimestamp", 0)));
        TextView head = new TextView(host);
        head.setText("Withdrawn");
        head.setTextColor(themeColor(R.color.colorTextPrimary));
        head.setTextSize(14);
        head.setTypeface(null, android.graphics.Typeface.BOLD);
        head.setPadding(0, dp(8), 0, dp(4));
        unbondingList.addView(head);
        for (JSONObject u : done) {
            TextView row = new TextView(host);
            row.setText(harv(u.optLong("amountRaw", 0)) + " · returned " + dateTime(u.optLong("releaseTimestamp", 0)));
            row.setTextColor(themeColor(R.color.colorTextMuted));
            row.setTextSize(13);
            row.setPadding(0, dp(4), 0, dp(4));
            unbondingList.addView(row);
        }
    }

    private void applyCurvedBottomSheetBackground(LinearLayout root, BottomSheetDialog sheet) {
        GradientDrawable roundedBg = new GradientDrawable();
        roundedBg.setShape(GradientDrawable.RECTANGLE);
        roundedBg.setColor(themeColor(R.color.colorSurface));
        float radius = dp(20);
        roundedBg.setCornerRadii(new float[]{radius, radius, radius, radius, 0, 0, 0, 0});
        root.setBackground(roundedBg);

        sheet.setOnShowListener(dialog -> {
            BottomSheetDialog d = (BottomSheetDialog) dialog;
            View bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                bottomSheet.setBackgroundColor(Color.TRANSPARENT);
            }
        });
    }

    private void showFullUnbondingBottomSheet(List<JSONObject> allRows) {
        BottomSheetDialog sheet = new com.harvana.app.ui.AppSheet(host, com.google.android.material.R.style.Theme_Design_BottomSheetDialog);

        LinearLayout root = new LinearLayout(host);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(24), dp(24), dp(32));
        applyCurvedBottomSheetBackground(root, sheet);

        TextView title = new TextView(host);
        title.setText("All Unbonding & Withdrawals");
        title.setTextColor(themeColor(R.color.colorTextPrimary));
        title.setTextSize(20);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setPadding(0, 0, 0, dp(16));
        root.addView(title);

        ScrollView scrollView = new ScrollView(host);
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(400)));
        scrollView.setNestedScrollingEnabled(true);

        LinearLayout listContainer = new LinearLayout(host);
        listContainer.setOrientation(LinearLayout.VERTICAL);

        for (int i = 0; i < allRows.size(); i++) {
            View itemV = createUnbondingRowView(allRows.get(i));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, dp(4), 0, dp(4));
            itemV.setLayoutParams(lp);
            listContainer.addView(itemV);

            // Add lime/dark separator between rows in the bottom sheet
            if (i < allRows.size() - 1) {
                View divider = new View(host);
                LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
                divLp.setMargins(0, dp(6), 0, dp(6));
                divider.setLayoutParams(divLp);
                divider.setBackgroundColor(themeColor(R.color.colorCardStroke));
                listContainer.addView(divider);
            }
        }

        scrollView.addView(listContainer);
        root.addView(scrollView);

        sheet.setContentView(root);
        sheet.show();
    }

    private void showUnbondDetailsSheet(JSONObject u) {
        BottomSheetDialog sheet = new com.harvana.app.ui.AppSheet(host, com.google.android.material.R.style.Theme_Design_BottomSheetDialog);

        LinearLayout root = new LinearLayout(host);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(24), dp(24), dp(32));
        applyCurvedBottomSheetBackground(root, sheet);

        TextView title = new TextView(host);
        title.setText("Withdrawal Details");
        title.setTextColor(themeColor(R.color.colorTextPrimary));
        title.setTextSize(20);
        title.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView amountTv = new TextView(host);
        amountTv.setText(harv(u.optLong("amountRaw", 0)));
        amountTv.setTextColor(themeColor(R.color.colorTextPrimary));
        amountTv.setTextSize(32);
        amountTv.setTypeface(null, android.graphics.Typeface.BOLD);
        amountTv.setPadding(0, dp(16), 0, dp(4));

        long now = System.currentTimeMillis();
        long release = u.optLong("releaseTimestamp", 0);
        boolean ready = (release - now) <= 0 || "RELEASING".equals(u.optString("status"));

        TextView status = new TextView(host);
        status.setTextSize(12);
        status.setTypeface(null, android.graphics.Typeface.BOLD);
        if (ready) {
            status.setText("● Ready for Withdrawal");
            status.setTextColor(themeColor(R.color.colorSuccess));
        } else {
            status.setText("● Unbonding (Locked)");
            status.setTextColor(themeColor(R.color.colorAccent));
        }

        LinearLayout receiptBox = new LinearLayout(host);
        receiptBox.setOrientation(LinearLayout.VERTICAL);
        receiptBox.setBackgroundResource(R.drawable.bg_surface_light);
        receiptBox.setPadding(dp(16), dp(16), dp(16), dp(16));
        LinearLayout.LayoutParams boxParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        boxParams.setMargins(0, dp(24), 0, dp(16));
        receiptBox.setLayoutParams(boxParams);

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.US);
        String reqDate = sdf.format(new java.util.Date(u.optLong("requestedTimestamp", 0)));
        String relDate = sdf.format(new java.util.Date(release));

        receiptBox.addView(buildReceiptRow("Requested", reqDate));
        receiptBox.addView(buildReceiptRow("Unlocks", relDate));
        receiptBox.addView(buildReceiptRow("Unbond ID", u.optString("id", "UNKNOWN")));

        TextView note = new TextView(host);
        note.setText("For network security, unstaked funds undergo a strict 24-hour cooldown. Once the timer finishes, you can withdraw the HARV directly to your spendable wallet balance.");
        note.setTextColor(themeColor(R.color.colorTextTertiary));
        note.setTextSize(12);
        note.setLineSpacing(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, host.getResources().getDisplayMetrics()), 1.0f);

        MaterialButton actionBtn = new MaterialButton(host);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56));
        btnParams.setMargins(0, dp(24), 0, 0);
        actionBtn.setLayoutParams(btnParams);
        actionBtn.setAllCaps(false);
        actionBtn.setCornerRadius(dp(12));

        if (ready) {
            actionBtn.setText("Withdraw to Wallet");
            actionBtn.setTextColor(themeColor(R.color.colorTextOnAccent));
            actionBtn.setBackgroundColor(themeColor(R.color.colorAccent));
            actionBtn.setOnClickListener(v -> {
                sheet.dismiss();
                doManualSweep();
            });
        } else {
            actionBtn.setText("Got it");
            actionBtn.setTextColor(themeColor(R.color.colorTextPrimary));
            actionBtn.setBackgroundColor(themeColor(R.color.colorCardStroke));
            actionBtn.setOnClickListener(v -> sheet.dismiss());
        }

        root.addView(title);
        root.addView(amountTv);
        root.addView(status);
        root.addView(receiptBox);
        root.addView(note);
        root.addView(actionBtn);

        sheet.setContentView(root);
        sheet.show();
    }

    private LinearLayout buildReceiptRow(String label, String value) {
        LinearLayout row = new LinearLayout(host);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(6), 0, dp(6));

        TextView lblTv = new TextView(host);
        lblTv.setText(label);
        lblTv.setTextColor(themeColor(R.color.colorTextTertiary));
        lblTv.setTextSize(13);
        lblTv.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        TextView valTv = new TextView(host);
        valTv.setText(value);
        valTv.setTextColor(themeColor(R.color.colorTextPrimary));
        valTv.setTextSize(13);
        valTv.setTypeface(null, android.graphics.Typeface.BOLD);

        row.addView(lblTv);
        row.addView(valTv);
        return row;
    }

    private void doManualSweep() {
        busy = true; toast("Requesting withdrawal release...");
        HarvanaNodeClient.getRecentBlocks(new HarvanaNodeClient.JsonCallback() {
            @Override public void onSuccess(JSONObject json) { busy = false; refresh(); }
            @Override public void onError(String error) { busy = false; refresh(); }
        });
    }

    /** One stake as the node reports it (or the single combined position of an older node). */
    private static final class Stake {
        String id; long amountRaw; int lockDays; int multiplierBps; long unlockTimestamp; long pendingRaw;
    }

    private java.util.List<Stake> stakes() {
        java.util.List<Stake> out = new ArrayList<>();
        if (pos == null) return out;
        org.json.JSONArray arr = pos.optJSONArray("stakes");
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                Stake s = new Stake();
                s.id = o.optString("id", null);
                s.amountRaw = o.optLong("amountRaw", 0);
                s.lockDays = o.optInt("lockDays", 0);
                s.multiplierBps = o.optInt("multiplierBps", 10000);
                s.unlockTimestamp = o.optLong("unlockTimestamp", 0);
                s.pendingRaw = o.optLong("pendingRewardsRaw", 0);
                if (s.amountRaw > 0) out.add(s);
            }
        } else if (pos.optLong("stakedRaw", 0) > 0) {
            Stake s = new Stake();
            s.amountRaw = pos.optLong("stakedRaw", 0);
            s.lockDays = pos.optInt("lockDays", 0);
            s.multiplierBps = pos.optInt("multiplierBps", 10000);
            s.unlockTimestamp = pos.optLong("unlockTimestamp", 0);
            s.pendingRaw = pos.optLong("accruedRaw", 0);
            out.add(s);
        }
        return out;
    }

    /** One card per stake, each with its own lock, rewards and Unstake. Claim stays a single button for all of them. */
    private void renderStakes() {
        stakesList.removeAllViews();
        liveEmpty.setVisibility(stakes().isEmpty() ? View.VISIBLE : View.GONE);
        long now = System.currentTimeMillis();
        for (Stake s : stakes()) {
            int mult = s.multiplierBps <= 0 ? 10000 : s.multiplierBps;
            boolean locked = StakingRules.lockedUntil(s.amountRaw, s.unlockTimestamp, now) > 0;
            String type = s.lockDays <= 0 ? "Flexible · 1x · stays until you unstake"
                    : locked ? "Locked " + s.lockDays + " days · " + String.format(Locale.US, "%.2fx", mult / 10000.0) + " · unlocks " + dateTime(s.unlockTimestamp)
                    : "Lock ended · " + String.format(Locale.US, "%.2fx", mult / 10000.0) + " · you can unstake";

            com.google.android.material.card.MaterialCardView card = new com.google.android.material.card.MaterialCardView(host);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cp.topMargin = dp(12);
            card.setLayoutParams(cp);
            card.setRadius(dp(16));
            card.setCardBackgroundColor(themeColor(R.color.colorSurface));
            card.setStrokeColor(themeColor(R.color.colorCardStroke));
            card.setStrokeWidth(dp(1));

            LinearLayout col = new LinearLayout(host);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setPadding(dp(16), dp(16), dp(16), dp(16));

            LinearLayout top = new LinearLayout(host);
            top.setOrientation(LinearLayout.HORIZONTAL);
            top.setGravity(Gravity.CENTER_VERTICAL);
            TextView amountTv = new TextView(host);
            amountTv.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            amountTv.setText(harv(s.amountRaw));
            amountTv.setTextColor(themeColor(R.color.colorTextPrimary));
            amountTv.setTextSize(18);
            amountTv.setTypeface(null, android.graphics.Typeface.BOLD);
            TextView badge = new TextView(host);
            badge.setText(locked ? "● Locked" : "● Active (Earning)");
            badge.setTextColor(themeColor(locked ? R.color.colorAccent : R.color.colorSuccess));
            badge.setTextSize(12);
            badge.setTypeface(null, android.graphics.Typeface.BOLD);
            top.addView(amountTv);
            top.addView(badge);

            TextView typeTv = new TextView(host);
            typeTv.setText(type);
            typeTv.setTextColor(themeColor(R.color.colorTextPrimary));
            typeTv.setTextSize(13);
            typeTv.setPadding(0, dp(8), 0, 0);

            TextView earnedTv = new TextView(host);
            earnedTv.setText("Earned so far: " + harv(s.pendingRaw));
            earnedTv.setTextColor(themeColor(R.color.colorTextMuted));
            earnedTv.setTextSize(12);
            earnedTv.setPadding(0, dp(4), 0, 0);

            MaterialButton unstakeBtn = new MaterialButton(host, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            bp.topMargin = dp(14);
            unstakeBtn.setLayoutParams(bp);
            unstakeBtn.setText("Unstake");
            unstakeBtn.setAllCaps(false);
            unstakeBtn.setTextColor(themeColor(R.color.colorAccent));
            unstakeBtn.setStrokeColor(android.content.res.ColorStateList.valueOf(themeColor(R.color.colorCardStroke)));
            unstakeBtn.setEnabled(!busy);
            com.harvana.app.tx.PaymentTracker.mark(unstakeBtn);
            unstakeBtn.setOnClickListener(v -> askUnstake(s));

            col.addView(top);
            col.addView(typeTv);
            col.addView(earnedTv);
            col.addView(unstakeBtn);
            card.addView(col);
            stakesList.addView(card);
        }
    }

    private void askUnstake(Stake stake) {
        long lockedUntil = StakingRules.lockedUntil(stake.amountRaw, stake.unlockTimestamp, System.currentTimeMillis());
        if (lockedUntil > 0) {
            new com.harvana.app.ui.SheetDialog.Builder(host)
                    .setTitle("Principal locked")
                    .setMessage("This stake is locked until " + dateTime(lockedUntil)
                            + ". You cannot unstake it before then. Your other stakes are not affected, and you can still claim rewards once every 24 hours.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }
        final String lotId = stake.id;
        BottomSheetDialog sheet = new com.harvana.app.ui.AppSheet(host, com.google.android.material.R.style.Theme_Design_BottomSheetDialog);

        LinearLayout root = new LinearLayout(host);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(24), dp(24), dp(32));
        applyCurvedBottomSheetBackground(root, sheet);

        TextView title = new TextView(host);
        title.setText("Unstake this stake");
        title.setTextColor(themeColor(R.color.colorTextPrimary));
        title.setTextSize(20);
        title.setTypeface(null, android.graphics.Typeface.BOLD);

        long stakedRaw = stake.amountRaw;
        double stakedHarv = stakedRaw / (double) HarvanaNodeClient.HARV_UNITS_PER_TOKEN;

        TextView subTitle = new TextView(host);
        subTitle.setText("This stake: " + harv(stakedRaw) + (stake.lockDays > 0 ? " · " + stake.lockDays + "-day lock" : " · flexible"));
        subTitle.setTextColor(themeColor(R.color.colorTextTertiary));
        subTitle.setTextSize(13);
        subTitle.setPadding(0, dp(4), 0, dp(16));

        LinearLayout inputContainer = new LinearLayout(host);
        inputContainer.setOrientation(LinearLayout.HORIZONTAL);
        inputContainer.setBackgroundResource(R.drawable.bg_input_dark);
        inputContainer.setPadding(dp(16), dp(12), dp(16), dp(12));
        inputContainer.setGravity(Gravity.CENTER_VERTICAL);

        EditText inputEt = new com.google.android.material.textfield.TextInputEditText(host);
        inputEt.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        inputEt.setBackground(null);
        inputEt.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        inputEt.setHint("0.00");
        inputEt.setTextColor(themeColor(R.color.colorTextPrimary));
        inputEt.setTextSize(18);
        inputEt.setHintTextColor(themeColor(R.color.colorTextMuted));

        TextView currencyTv = new TextView(host);
        currencyTv.setText("HARV");
        currencyTv.setTextColor(themeColor(R.color.colorTextPrimary));
        currencyTv.setTypeface(null, android.graphics.Typeface.BOLD);

        inputContainer.addView(inputEt);
        inputContainer.addView(currencyTv);

        LinearLayout percentRow = new LinearLayout(host);
        percentRow.setOrientation(LinearLayout.HORIZONTAL);
        percentRow.setPadding(0, dp(12), 0, dp(16));
        percentRow.setWeightSum(4);

        String[] labels = {"10%", "25%", "50%", "Unstake All"};
        double[] multipliers = {0.10, 0.25, 0.50, 1.00};

        for (int i = 0; i < labels.length; i++) {
            final double mult = multipliers[i];
            MaterialButton btn = new MaterialButton(host, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(36), 1);
            if (i > 0) p.setMargins(dp(6), 0, 0, 0);
            btn.setLayoutParams(p);
            btn.setText(labels[i]);
            btn.setTextSize(11);
            btn.setAllCaps(false);
            btn.setTextColor(themeColor(R.color.colorAccent));
            btn.setStrokeColorResource(android.R.color.transparent);
            btn.setBackgroundColor(themeColor(R.color.colorCardStroke));
            btn.setOnClickListener(v -> {
                double val = stakedHarv * mult;
                inputEt.setText(harvFormatter.format(val));
                inputEt.setSelection(inputEt.getText().length());
            });
            percentRow.addView(btn);
        }

        MaterialButton confirmBtn = new MaterialButton(host);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56));
        confirmBtn.setLayoutParams(btnParams);
        confirmBtn.setText("Confirm Unstake");
        confirmBtn.setAllCaps(false);
        confirmBtn.setTextColor(themeColor(R.color.colorTextOnAccent));
        confirmBtn.setCornerRadius(dp(12));
        confirmBtn.setBackgroundColor(themeColor(R.color.colorAccent));

        confirmBtn.setOnClickListener(v -> {
            String valStr = inputEt.getText().toString().trim();
            if (valStr.isEmpty()) { toast("Enter an amount to unstake."); return; }
            try {
                double val = Double.parseDouble(valStr);
                if (val <= 0 || val > stakedHarv) { toast("Invalid unstake amount."); return; }
                sheet.dismiss();
                amount.setText(valStr);
                doUnstake(lotId);
            } catch (Exception e) { toast("Invalid amount."); }
        });

        root.addView(title);
        root.addView(subTitle);
        root.addView(inputContainer);
        root.addView(percentRow);
        root.addView(confirmBtn);

        sheet.setContentView(root);
        sheet.show();
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
        rows.add(new String[]{"Term", selectedLockDays == 0 ? "Free Stake (No Lock)" : selectedLockDays + " Days"});

        gate("Stake HARV", harvFormatter.format(harvAmount) + " HARV", rows, () -> {
            busy = true; paint();
            String memoLock = "STAKE_LOCK:" + selectedLockDays;
            TransactionRouter.routeTransaction(host, address, vault, BigDecimal.valueOf(harvAmount), null, memoLock,
                    new TransactionRouter.RouterCallback() {
                        @Override public void onSuccess(String txHash) { busy = false; amount.setText(""); toast("Stake submitted."); refresh(); }
                        @Override public void onError(String code, String msg) { busy = false; toast(msg); paint(); }
                    });
        });
    }

    private void doUnstake(String lotId) {
        String address = WalletManager.getActiveWalletAddress(host);
        double harvAmount;
        try { harvAmount = Double.parseDouble(amount.getText().toString().trim()); }
        catch (Exception e) { return; }

        ArrayList<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"To", "Your wallet, after 24 hours"});

        gate("Unstake HARV", harvFormatter.format(harvAmount) + " HARV", rows, () -> {
            busy = true;
            NodeGroupClient.unstake(host, address, lotId, BalanceFormatter.harvToRaw(harvAmount), new NodeGroupClient.Callback() {
                @Override public void onSuccess(JSONObject json) { busy = false; amount.setText(""); toast("Unstaking. Returns in 24 hours."); refresh(); }
                @Override public void onError(String error) { busy = false; toast(friendlyStakingError(error)); paint(); }
            });
        });
    }

    private void doClaim() {
        String address = WalletManager.getActiveWalletAddress(host);
        long pending = pos == null ? 0 : pos.optLong("pendingRewardsRaw", 0);
        if (pending <= 0) return;

        ArrayList<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"To", "Your wallet"});
        gate("Claim staking rewards", harv(pending), rows, () -> {
            busy = true;
            NodeGroupClient.claimStakingRewards(host, address, new NodeGroupClient.Callback() {
                @Override public void onSuccess(JSONObject json) { busy = false; toast("Reward claimed."); refresh(); }
                @Override public void onError(String error) { busy = false; toast(friendlyStakingError(error)); paint(); }
            });
        });
    }

    private void setRange(int days) {
        chartDays = days;
        chip1d.setTextColor(themeColor(R.color.colorTextTertiary)); chip1d.setBackgroundResource(0);
        chip3d.setTextColor(themeColor(R.color.colorTextTertiary)); chip3d.setBackgroundResource(0);
        chip7d.setTextColor(themeColor(R.color.colorTextTertiary)); chip7d.setBackgroundResource(0);
        chip1m.setTextColor(themeColor(R.color.colorTextTertiary)); chip1m.setBackgroundResource(0);
        chip3m.setTextColor(themeColor(R.color.colorTextTertiary)); chip3m.setBackgroundResource(0);
        chipAll.setTextColor(themeColor(R.color.colorTextTertiary)); chipAll.setBackgroundResource(0);

        TextView selected = days == 1 ? chip1d : days == 3 ? chip3d : days == 7 ? chip7d : days == 30 ? chip1m : days == 90 ? chip3m : chipAll;
        selected.setTextColor(themeColor(R.color.colorBackground));
        selected.setBackgroundResource(R.drawable.bg_pill_orange);

        drawChart(WalletManager.getActiveWalletAddress(host));
    }

    private void fillMax() { 
        if (walletRaw > 0) amount.setText(harvFormatter.format(walletRaw / (double) HarvanaNodeClient.HARV_UNITS_PER_TOKEN)); 
    }

    private void gate(String title, String amt, List<String[]> rows, Runnable w) { 
        PaymentConfirmSheet.reviewThenAuthorize(host, title, amt, rows, w); 
    }

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
            if (hit == null) { hit = new JSONObject(); hit.put("d", today); rows.put(hit); }
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

        int requiredPoints = chartDays == 1 ? 2 : chartDays;
        int start = requiredPoints >= rows.length() ? 0 : rows.length() - requiredPoints;
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
