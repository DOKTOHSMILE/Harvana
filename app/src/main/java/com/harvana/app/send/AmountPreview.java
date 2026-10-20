package com.harvana.app.send;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.harvana.app.R;
import com.harvana.app.sync.HarvanaNodeClient;
import com.harvana.app.wallet.AssetPrices;
import com.harvana.app.wallet.FiatValueHelper;
import com.harvana.app.wallet.FxRates;

import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/**
 * The amount box on the Send screen (owner, 20 Oct 2026: typing by value must be plain). The amount is large with its
 * unit; beside it a two-way switch [coin | money] and MAX; under it the same amount in the other unit ("≈ $5.00" or
 * "≈ 0.0478 SOL"). The table under the box holds Available (in both units), the price and the tFare.
 *
 * Prices: HARV at the network's mark (GET /api/v1/p2p/mark, kobo per HARV), stable coins at their currency's
 * exchange rate, other coins at the public market price. The fee for HARV comes from GET /api/v1/tx/estimate-fee,
 * which uses the same calculation the node applies when it accepts the transfer.
 */
public final class AmountPreview {

    public interface Source {
        /** Asset key of the selected asset: HARV, a stable code (HUSD, HNGN, …) or a chain coin key (BTC, ETH, …). */
        String assetKey();
        String symbol();
        String from();
        String to();
    }

    private final Context c;
    private final EditText amount;
    private final Source src;
    /** The switch halves, the unit marks around the amount, the other-unit line and MAX (null when the layout lacks them). */
    private final TextView unitCoin, unitMoney, prefix, suffix, other, max;
    /** The table under the amount: Available / Value (or Price) / Network fee, label left and figure right. */
    private final TextView availLabel, availValue, valueLabel, valueValue, feeValue;
    private final LinearLayout availRow, valueRow, feeRow;
    private final TextView anchor;
    private final Handler main = new Handler(Looper.getMainLooper());
    private boolean moneyMode;
    /** Display money per one unit of the asset; NaN until known. */
    private double moneyPerUnit = Double.NaN;
    private String pricedKey = "";
    private int seq;

    public AmountPreview(Context c, EditText amount, TextView anchor, Source src) {
        this.c = c;
        this.amount = amount;
        this.src = src;
        this.anchor = anchor;
        float d = c.getResources().getDisplayMetrics().density;
        LinearLayout table = new LinearLayout(c);
        table.setOrientation(LinearLayout.VERTICAL);
        table.setPadding(Math.round(14 * d), Math.round(8 * d), Math.round(14 * d), Math.round(8 * d));
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setCornerRadius(12 * d);
        bg.setColor(c.getResources().getColor(R.color.colorSurface));
        table.setBackground(bg);
        availLabel = cell(c, "Available", false); availValue = cell(c, "…", true);
        availRow = row(c, availLabel, availValue);
        valueLabel = cell(c, "Value", false); valueValue = cell(c, "", true);
        valueRow = row(c, valueLabel, valueValue);
        valueLabel.setText("Price");
        TextView feeLabel = cell(c, "tFare", false); feeValue = cell(c, "", true);
        feeRow = row(c, feeLabel, feeValue);
        table.addView(availRow);
        table.addView(valueRow);
        table.addView(feeRow);
        View root = amount.getRootView();
        unitCoin = root.findViewById(R.id.tv_unit_coin);
        unitMoney = root.findViewById(R.id.tv_unit_money);
        prefix = root.findViewById(R.id.tv_amount_prefix);
        suffix = root.findViewById(R.id.tv_amount_suffix);
        other = root.findViewById(R.id.tv_amount_other);
        max = root.findViewById(R.id.tv_amount_max);
        if (unitCoin != null) unitCoin.setOnClickListener(v -> { if (moneyMode) switchMode(); });
        if (unitMoney != null) unitMoney.setOnClickListener(v -> { if (!moneyMode) switchMode(); });
        if (max != null) max.setOnClickListener(v -> fillMax());
        // Into the nearest vertical column under the amount (the "Available" line may sit in a horizontal row, which
        // squeezed it to one letter per line), and the old line itself is replaced by the table's first row.
        View branch = anchor;
        ViewGroup parent = anchor == null ? null : (ViewGroup) anchor.getParent();
        while (parent instanceof LinearLayout && ((LinearLayout) parent).getOrientation() != LinearLayout.VERTICAL && parent.getParent() instanceof ViewGroup) {
            branch = parent;
            parent = (ViewGroup) parent.getParent();
        }
        if (parent != null) {
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            tlp.topMargin = Math.round(6 * d);
            parent.addView(table, parent.indexOfChild(branch) + 1, tlp);
        }
        if (anchor != null) {
            mirrorAvailable();
            anchor.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int a, int b, int e) { }
                @Override public void onTextChanged(CharSequence s, int a, int b, int e) { }
                @Override public void afterTextChanged(Editable s) { mirrorAvailable(); }
            });
            anchor.setVisibility(View.GONE);
        }
        valueRow.setVisibility(View.GONE);
        feeRow.setVisibility(View.GONE);
        amount.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int d) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int d) { }
            @Override public void afterTextChanged(Editable s) { refreshSoon(); }
        });
        assetChanged();
    }

    /** Call when another asset is picked: back to typing the coin, and the price is looked up again. */
    public void assetChanged() {
        if (moneyMode) { moneyMode = false; amount.setText(""); }
        moneyPerUnit = Double.NaN;
        pricedKey = "";
        paintUnits();
        refreshSoon();
    }

    public boolean inMoney() { return moneyMode; }

    /**
     * Before sending: when the person typed money, puts the coin amount into the field and leaves money mode, so the
     * rest of the send reads a coin amount as before. False (with a message on the line) when there is no price yet.
     */
    public boolean normalise() {
        if (!moneyMode) return true;
        double typed = parse(amount.getText().toString());
        if (Double.isNaN(moneyPerUnit) || moneyPerUnit <= 0) { value("Value", "No price yet: enter " + src.symbol()); return false; }
        moneyMode = false;
        paintUnits();
        amount.setText(coinText(typed / moneyPerUnit));
        return true;
    }

    private void switchMode() {
        double typed = parse(amount.getText().toString());
        if (Double.isNaN(moneyPerUnit) || moneyPerUnit <= 0) {
            price(() -> { if (moneyPerUnit > 0) switchMode(); else otherLine("No price for " + src.symbol()); });
            return;
        }
        moneyMode = !moneyMode;
        paintUnits();
        if (!Double.isNaN(typed) && typed > 0) {
            amount.setText(moneyMode ? new BigDecimal(typed * moneyPerUnit).setScale(2, RoundingMode.HALF_UP).toPlainString()
                    : coinText(typed / moneyPerUnit));
        }
        refreshSoon();
    }

    private void refreshSoon() {
        final int mine = ++seq;
        main.postDelayed(() -> { if (mine == seq) price(this::refresh); }, 350);
    }

    private void refresh() {
        double typed = parse(amount.getText().toString());
        String key = src.assetKey();
        boolean known = !Double.isNaN(moneyPerUnit) && moneyPerUnit > 0;
        String money = FxRates.symbol(c);
        value("Price", known ? "1 " + src.symbol() + " ≈ " + FxRates.format(money, moneyPerUnit) : "");
        mirrorAvailable();
        if (Double.isNaN(typed) || typed <= 0) {
            otherLine(moneyMode ? "≈ 0 " + src.symbol() : known ? "≈ " + FxRates.format(money, 0) : "");
            feeRow.setVisibility(View.GONE);
            return;
        }
        double coins = moneyMode ? (known ? typed / moneyPerUnit : Double.NaN) : typed;
        otherLine(!known ? "" : moneyMode ? "≈ " + coinText(coins) + " " + src.symbol() : "≈ " + FxRates.format(money, typed * moneyPerUnit));
        fee(key, coins, "");
    }

    /** The switch shows which unit is being typed; the amount carries that unit's mark. */
    private void paintUnits() {
        String money = FxRates.symbol(c);
        String code = FiatValueHelper.symbolToCurrencyCode(money);
        if (unitCoin != null) chip(unitCoin, src.symbol(), !moneyMode);
        if (unitMoney != null) chip(unitMoney, code == null || code.isEmpty() ? money : code, moneyMode);
        if (prefix != null) { prefix.setText(money); prefix.setVisibility(moneyMode ? View.VISIBLE : View.GONE); }
        if (suffix != null) suffix.setText(moneyMode ? "" : src.symbol());
        amount.setHint(moneyMode ? "0.00" : "0");
    }

    private void chip(TextView t, String text, boolean on) {
        t.setText(text);
        t.setBackgroundResource(on ? R.drawable.bg_chip_on : 0);
        t.setTextColor(c.getResources().getColor(on ? R.color.colorAccent : R.color.colorTextSecondary));
    }

    private void otherLine(String text) { if (other != null) other.setText(text); }

    /** MAX: the whole available balance, in the unit being typed. */
    private void fillMax() {
        double all = availableCoins();
        if (Double.isNaN(all) || all <= 0) return;
        if (moneyMode && moneyPerUnit > 0) {
            amount.setText(new BigDecimal(all * moneyPerUnit).setScale(2, RoundingMode.DOWN).toPlainString());
        } else {
            if (moneyMode) { moneyMode = false; paintUnits(); }
            amount.setText(coinText(all));
        }
        amount.setSelection(amount.getText() == null ? 0 : amount.getText().length());
    }

    /** The coin figure on the screen's own "Available: 12.5 HARV" line. */
    private double availableCoins() {
        CharSequence t = anchor == null ? null : anchor.getText();
        if (t == null) return Double.NaN;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("([0-9][0-9,]*\\.?[0-9]*)").matcher(t.toString());
        return m.find() ? parse(m.group(1).replace(",", "")) : Double.NaN;
    }

    private void fee(String key, double coins, String worth) {
        final int mine = seq;
        if ("HARV".equals(key)) {
            String from = src.from(), to = src.to();
            if (from == null || from.isEmpty()) return;
            if (to == null || !to.startsWith("HV0x")) to = from;
            long raw = Double.isNaN(coins) ? 0 : Math.round(coins * 100_000_000d);
            HarvanaNodeClient.getFee(from, to, raw, new HarvanaNodeClient.JsonCallback() {
                @Override public void onSuccess(JSONObject j) {
                    long gas = j.optLong("estimatedGas", 0);
                    long kobo = j.isNull("estimatedKobo") ? 0 : j.optLong("estimatedKobo", 0);
                    String f = "tFare " + coinText(gas / 1e8) + " HARV" + (kobo > 0 ? " (≈ ₦" + trim(kobo / 100d) + ")" : "");
                    main.post(() -> { if (mine == seq) fee(f.replace("tFare ", "")); });
                }
                @Override public void onError(String e) { }
            });
        } else if (com.harvana.app.husd.Stable.isStable(key)) {
            fee("HARV");
        } else {
            fee("Set by that network when sent");
        }
    }

    /** Looks up the money value of one unit of the selected asset, then runs {@code then} on the main thread. */
    private void price(Runnable then) {
        final String key = src.assetKey();
        if (key.equals(pricedKey) && !Double.isNaN(moneyPerUnit)) { then.run(); return; }
        FxRates.load(c, () -> {
            String code = FiatValueHelper.symbolToCurrencyCode(FxRates.symbol(c));
            double displayPerUsd = FxRates.perUsd(c, code);
            if ("HARV".equals(key)) {
                com.harvana.app.wallet.HarvPrice.get(q -> {
                    set(key, com.harvana.app.wallet.HarvPrice.usd(c, q), displayPerUsd);
                    then.run();
                });
            } else if (com.harvana.app.husd.Stable.isStable(key)) {
                double per = FxRates.perUsd(c, com.harvana.app.husd.Stable.currency(key));
                set(key, Double.isNaN(per) || per <= 0 ? Double.NaN : 1d / per, displayPerUsd);
                then.run();
            } else {
                AssetPrices.load(c, () -> {
                    AssetPrices.Quote q = AssetPrices.peek(c, key);
                    set(key, q == null ? Double.NaN : q.usd, displayPerUsd);
                    then.run();
                });
            }
        });
    }

    private void set(String key, double usdPerUnit, double displayPerUsd) {
        pricedKey = key;
        moneyPerUnit = Double.isNaN(usdPerUnit) || Double.isNaN(displayPerUsd) ? Double.NaN : usdPerUnit * displayPerUsd;
    }

    private void value(String label, String text) {
        valueLabel.setText(label);
        valueValue.setText(text);
        valueRow.setVisibility(text == null || text.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void fee(String text) {
        feeValue.setText(text);
        feeRow.setVisibility(text == null || text.isEmpty() ? View.GONE : View.VISIBLE);
    }

    /** The screen still writes "Available: 12.5 HARV" into its own line; the table shows the figure. */
    private void mirrorAvailable() {
        CharSequence t = anchor == null ? null : anchor.getText();
        String s = t == null ? "" : t.toString().replaceFirst("^\\s*Available:?\\s*", "");
        double all = availableCoins();
        if (!s.isEmpty() && moneyPerUnit > 0 && !Double.isNaN(all)) s = s + "  (≈ " + FxRates.format(FxRates.symbol(c), all * moneyPerUnit) + ")";
        availValue.setText(s.isEmpty() ? "…" : s);
        availRow.setVisibility(s.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private static LinearLayout row(Context c, TextView label, TextView value) {
        LinearLayout r = new LinearLayout(c);
        r.setGravity(Gravity.CENTER_VERTICAL);
        int p = Math.round(4 * c.getResources().getDisplayMetrics().density);
        r.setPadding(0, p, 0, p);
        r.addView(label, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        value.setGravity(Gravity.END);
        r.addView(value, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.6f));
        return r;
    }

    private static TextView cell(Context c, String text, boolean figure) {
        TextView t = new TextView(c);
        t.setText(text);
        t.setTextSize(13f);
        t.setTextColor(c.getResources().getColor(figure ? R.color.colorTextPrimary : R.color.colorTextSecondary));
        if (figure) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private static String join(String a, String b) { return a == null || a.isEmpty() ? b : a + "  ·  " + b; }

    private static double parse(String s) {
        try { return s == null || s.trim().isEmpty() ? Double.NaN : Double.parseDouble(s.trim()); } catch (Exception e) { return Double.NaN; }
    }

    private static String coinText(double v) {
        if (Double.isNaN(v) || v <= 0) return "0";
        return new BigDecimal(v).setScale(8, RoundingMode.DOWN).stripTrailingZeros().toPlainString();
    }

    private static String trim(double v) { return String.format(Locale.US, v < 10 ? "%.2f" : "%,.0f", v); }
}
