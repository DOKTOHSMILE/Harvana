package com.harvanna.app.earn;

import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
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
import java.util.Date;
import java.util.Locale;

/**
 * Stake HARV on the Earn tab. Numbers come from GET /api/v1/staking/info and
 * /position. A stake is a signed transfer to the vault. Claim and unstake use
 * the staking action nonce. The APR is the node's estimate, not a promise.
 */
public final class StakingPanel {
    private static final int DAYS = 7;

    private final Fragment host;
    private final TextView apr, tvl, position, daily, status;
    private final LinearLayout chart, unbonding;
    private final EditText amount;
    private final MaterialButton stake, unstake, claim;
    private JSONObject info;
    private JSONObject pos;
    private boolean busy;

    private StakingPanel(Fragment host, LinearLayout root) {
        this.host = host;
        apr = text(22, true, UiColors.accent());
        tvl = text(13, false, UiColors.textSecondary());
        position = text(14, false, UiColors.textPrimary());
        daily = text(14, true, UiColors.textPrimary());
        status = text(12, false, UiColors.textSecondary());
        chart = row();
        unbonding = col();
        amount = new EditText(host.requireContext());
        amount.setHint("Amount in HARV");
        amount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        amount.setTextColor(UiColors.textPrimary());
        amount.setHintTextColor(UiColors.textTertiary());
        amount.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) {}
            public void afterTextChanged(android.text.Editable s) {
                if (pos == null || pos.optLong("stakedRaw", 0) == 0) paint();
            }
        });
        stake = button("Stake", false);
        unstake = button("Unstake", true);
        claim = button("Claim rewards", false);
        stake.setOnClickListener(v -> doStake());
        unstake.setOnClickListener(v -> doUnstake());
        claim.setOnClickListener(v -> doClaim());

        root.setOrientation(LinearLayout.VERTICAL);
        int p = dp(16);
        root.setPadding(p, p, p, p);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(UiColors.surface());
        bg.setCornerRadius(dp(20));
        bg.setStroke(dp(1), (UiColors.textTertiary() & 0x00FFFFFF) | 0x33000000);
        root.setBackground(bg);
        root.addView(text("Stake HARV", 18, true, UiColors.textPrimary()));
        root.addView(text("Earn a share of every block, in proportion to what you stake.", 13, false, UiColors.textSecondary()));
        root.addView(apr);
        root.addView(tvl);
        root.addView(position);
        root.addView(daily);
        root.addView(gap(text("Estimated earnings, next 7 days", 12, true, UiColors.textTertiary())));
        root.addView(chart);
        root.addView(amount);
        LinearLayout actions = row();
        actions.addView(stake, weight());
        actions.addView(unstake, weight());
        root.addView(actions);
        root.addView(claim);
        root.addView(unbonding);
        root.addView(status);
        root.addView(text("Minimum first stake is 1 HARV. Unstaking returns after 7 days and stops earning immediately. The rate is an estimate: it moves with total stake and block time.", 12, false, UiColors.textTertiary()));
    }

    public static void attach(Fragment fragment, LinearLayout parent) {
        if (fragment.getContext() == null || parent == null) return;
        View existing = null;
        for (int i = 0; i < parent.getChildCount(); i++) {
            if (parent.getChildAt(i).getTag() instanceof StakingPanel) { existing = parent.getChildAt(i); break; }
        }
        if (existing != null) {
            ((StakingPanel) existing.getTag()).refresh();
            return;
        }
        LinearLayout card = new LinearLayout(fragment.requireContext());
        card.setTag("staking-panel");
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int m = (int) (16 * fragment.getResources().getDisplayMetrics().density);
        lp.setMargins(m, m, m, 0);
        card.setLayoutParams(lp);
        StakingPanel panel = new StakingPanel(fragment, card);
        card.setTag(panel);
        parent.addView(card, 0);
        panel.refresh();
    }

    private void refresh() {
        if (host.getContext() == null) return;
        String address = WalletManager.getActiveWalletAddress(host.requireContext());
        NodeGroupClient.getStakingInfo(new NodeGroupClient.Callback() {
            @Override public void onSuccess(JSONObject json) { info = json; paint(); }
            @Override public void onError(String error) { ui(() -> status.setText("Network: " + error)); }
        });
        if (address == null || address.isEmpty()) {
            ui(() -> position.setText("Create a wallet before you stake."));
            return;
        }
        NodeGroupClient.readStakingPosition(address, new NodeGroupClient.Callback() {
            @Override public void onSuccess(JSONObject json) { pos = json; paint(); }
            @Override public void onError(String error) { ui(() -> status.setText(error)); }
        });
    }

    private void paint() {
        ui(() -> {
            if (info != null) {
                long aprBps = info.optLong("estimatedAprBps", 0);
                double aprPct = aprBps / 100.0;
                apr.setText(String.format(Locale.US, "%.2f%% estimated APR", aprPct));
                tvl.setText(String.format(Locale.US, "%s staked · %d stakers · %.4f HARV per block to stakers",
                        harv(info.optLong("totalStakedRaw", 0)),
                        info.optInt("stakerCount", 0),
                        info.optLong("stakingRewardPerBlockRaw", 0) / (double) HarvannaNodeClient.HARV_UNITS_PER_TOKEN));
            }
            long staked = pos == null ? 0 : pos.optLong("stakedRaw", 0);
            long pending = pos == null ? 0 : pos.optLong("pendingRewardsRaw", 0);
            long claimed = pos == null ? 0 : pos.optLong("totalClaimedRaw", 0);
            position.setText("Your stake  " + harv(staked) + "\nUnclaimed  " + harv(pending) + "    Claimed  " + harv(claimed));
            double aprFrac = info == null ? 0 : info.optLong("estimatedAprBps", 0) / 10000.0;
            double stakedHarv = staked / (double) HarvannaNodeClient.HARV_UNITS_PER_TOKEN;
            double perDay = stakedHarv * aprFrac / 365.0;
            daily.setText(staked > 0
                    ? String.format(Locale.US, "About %.6f HARV a day at the current rate", perDay)
                    : "Stake to start earning. The chart uses the amount you type.");
            drawChart(staked > 0 ? perDay : projectedFromField(aprFrac));
            drawUnbonding();
            claim.setEnabled(!busy && pending > 0);
            stake.setEnabled(!busy);
            unstake.setEnabled(!busy && staked > 0);
        });
    }

    /** Cumulative estimate: day n is n times one day's share. */
    private void drawChart(double perDay) {
        chart.removeAllViews();
        chart.setGravity(Gravity.BOTTOM);
        chart.setMinimumHeight(dp(88));
        double max = Math.max(perDay * DAYS, 0.0000001);
        for (int d = 1; d <= DAYS; d++) {
            LinearLayout col = col();
            col.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            View bar = new View(host.requireContext());
            GradientDrawable g = new GradientDrawable();
            g.setColor(UiColors.accent());
            g.setCornerRadius(dp(4));
            bar.setBackground(g);
            int h = (int) Math.max(dp(6), dp(72) * ((perDay * d) / max));
            col.addView(bar, new LinearLayout.LayoutParams(dp(18), h));
            TextView lab = text(10, false, UiColors.textTertiary());
            lab.setText(String.valueOf(d));
            lab.setGravity(Gravity.CENTER);
            col.addView(lab);
            chart.addView(col, weight());
        }
    }

    private double projectedFromField(double aprFrac) {
        try {
            double typed = Double.parseDouble(amount.getText().toString().trim());
            if (typed > 0) return typed * aprFrac / 365.0;
        } catch (Exception ignored) { }
        return 0;
    }

    private void drawUnbonding() {
        unbonding.removeAllViews();
        if (pos == null) return;
        JSONArray rows = pos.optJSONArray("unbonding");
        if (rows == null || rows.length() == 0) return;
        unbonding.addView(text("Unbonding", 12, true, UiColors.textTertiary()));
        SimpleDateFormat fmt = new SimpleDateFormat("d MMM", Locale.US);
        for (int i = 0; i < rows.length(); i++) {
            JSONObject u = rows.optJSONObject(i);
            if (u == null) continue;
            String when = fmt.format(new Date(u.optLong("releaseTimestamp", 0)));
            unbonding.addView(text(harv(u.optLong("amountRaw", 0)) + " · " + u.optString("status", "") + " · " + when, 12, false, UiColors.textSecondary()));
        }
    }

    private void doStake() {
        if (host.getContext() == null || info == null) return;
        String address = WalletManager.getActiveWalletAddress(host.requireContext());
        String vault = info.optString("vaultAddress", "");
        double harv;
        try { harv = Double.parseDouble(amount.getText().toString().trim()); }
        catch (Exception e) { toast("Enter an amount in HARV."); return; }
        long already = pos == null ? 0 : pos.optLong("stakedRaw", 0);
        long min = info.optLong("minStakeRaw", HarvannaNodeClient.HARV_UNITS_PER_TOKEN);
        if (vault.isEmpty() || address == null || address.isEmpty()) { toast("Wallet or vault is not ready."); return; }
        if (already < min && BalanceFormatter.harvToRaw(harv) < min) { toast("The first stake must be at least 1 HARV."); return; }
        busy = true;
        paint();
        TransactionRouter.routeTransaction(host.requireContext(), address, vault, BigDecimal.valueOf(harv), null, "STAKE",
                new TransactionRouter.RouterCallback() {
                    @Override public void onSuccess(String txHash) { busy = false; toast("Stake submitted."); refresh(); }
                    @Override public void onError(String code, String message) { busy = false; toast(message); paint(); }
                });
    }

    private void doUnstake() {
        if (host.getContext() == null) return;
        String address = WalletManager.getActiveWalletAddress(host.requireContext());
        double harv;
        try { harv = Double.parseDouble(amount.getText().toString().trim()); }
        catch (Exception e) { toast("Enter how much HARV to unstake."); return; }
        if (harv <= 0 || address == null) return;
        busy = true;
        NodeGroupClient.unstake(host.requireContext(), address, BalanceFormatter.harvToRaw(harv), new NodeGroupClient.Callback() {
            @Override public void onSuccess(JSONObject json) { busy = false; toast("Unstaking. It returns after 7 days."); refresh(); }
            @Override public void onError(String error) { busy = false; toast(error); paint(); }
        });
    }

    private void doClaim() {
        if (host.getContext() == null) return;
        String address = WalletManager.getActiveWalletAddress(host.requireContext());
        if (address == null || address.isEmpty()) return;
        busy = true;
        NodeGroupClient.claimStakingRewards(host.requireContext(), address, new NodeGroupClient.Callback() {
            @Override public void onSuccess(JSONObject json) { busy = false; toast("Reward claimed."); refresh(); }
            @Override public void onError(String error) { busy = false; toast(error); paint(); }
        });
    }

    private String harv(long raw) {
        return String.format(Locale.US, "%.4f HARV", raw / (double) HarvannaNodeClient.HARV_UNITS_PER_TOKEN);
    }

    private void toast(String s) { ui(() -> { if (host.getContext() != null) Toast.makeText(host.requireContext(), s, Toast.LENGTH_LONG).show(); }); }
    private void ui(Runnable r) { if (host.getActivity() != null) host.requireActivity().runOnUiThread(r); }
    private int dp(int v) { return Math.round(v * host.getResources().getDisplayMetrics().density); }

    private TextView text(int sp, boolean bold, int color) {
        return text("", sp, bold, color);
    }

    private TextView text(String value, int sp, boolean bold, int color) {
        TextView t = new TextView(host.requireContext());
        t.setText(value);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(null, Typeface.BOLD);
        t.setPadding(0, dp(4), 0, 0);
        return t;
    }

    private TextView gap(TextView t) { t.setPadding(0, dp(12), 0, dp(4)); return t; }

    private MaterialButton button(String label, boolean outlined) {
        MaterialButton b = outlined
                ? new MaterialButton(host.requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle)
                : new MaterialButton(host.requireContext());
        b.setText(label);
        b.setAllCaps(false);
        return b;
    }

    private LinearLayout row() {
        LinearLayout l = new LinearLayout(host.requireContext());
        l.setOrientation(LinearLayout.HORIZONTAL);
        return l;
    }

    private LinearLayout col() {
        LinearLayout l = new LinearLayout(host.requireContext());
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private LinearLayout.LayoutParams weight() {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
    }
}
