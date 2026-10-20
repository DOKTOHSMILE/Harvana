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
    private final TextView availLabel, availValue, valueLabel, valueValue, feeValue, feeLabel;
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
        feeLabel = cell(c, "tFare", false); feeValue = cell(c, "", true);
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
    /**
     * MAX: the balance less the live fee when the fee comes out of the same coin (HARV's tFare, ETH's gas, SOL's 5,000
     * lamports, Pi's reserve, a UTXO coin's fee for spending everything…), so the send is never refused for the fee.
     * A token whose fee is paid in another coin gets its whole balance. The quote is read first when it is not fresh.
     */
    private void fillMax() {
        final String key = src.assetKey();
        Quote q = freshQuote(key);
        if (q == null) {
            fee("Reading the network fee…");
            loadQuote(key, () -> { Quote got = freshQuote(key); if (got != null) applyMax(got); else applyMax(null); });
            return;
        }
        applyMax(q);
    }

    private void applyMax(Quote q) {
        double all = q != null && q.maxSend != null ? q.maxSend.doubleValue() : availableCoins();
        if (q == null) fee("Fee not available: MAX is the whole balance");
        if (Double.isNaN(all) || all <= 0) { if (q != null) fee(feeText(q) + " · balance too low for the fee"); return; }
        if (moneyMode && moneyPerUnit > 0) {
            amount.setText(new BigDecimal(all * moneyPerUnit).setScale(2, RoundingMode.DOWN).toPlainString());
        } else {
            if (moneyMode) { moneyMode = false; paintUnits(); }
            amount.setText(q != null && q.maxSend != null ? q.maxSend.setScale(Math.min(8, Math.max(0, q.maxSend.scale())), RoundingMode.DOWN).stripTrailingZeros().toPlainString() : coinText(all));
        }
        amount.setSelection(amount.getText() == null ? 0 : amount.getText().length());
    }

    // ── live network fee ─────────────────────────────────────────────────────────────────────────────────────────

    /** A fee reading for one coin and receiver: what the fee is, in which coin, and the most that can be sent. */
    private static final class Quote {
        final String key, to;
        final long at;
        final java.math.BigDecimal fee, maxSend;
        final String symbol;
        final boolean sameCoin;

        Quote(String key, String to, java.math.BigDecimal fee, String symbol, java.math.BigDecimal maxSend, boolean sameCoin) {
            this.key = key; this.to = to; this.at = System.currentTimeMillis();
            this.fee = fee; this.symbol = symbol; this.maxSend = maxSend; this.sameCoin = sameCoin;
        }
    }

    private volatile Quote quote;
    private boolean quoting;

    /** API 23 has no java.util.function. */
    private interface QuoteDone { void accept(Quote q); }

    /** The last reading when it is for this coin and receiver and under 30 seconds old. */
    private Quote freshQuote(String key) {
        Quote q = quote;
        String to = src.to() == null ? "" : src.to().trim();
        return q != null && q.key.equals(key) && q.to.equals(to) && System.currentTimeMillis() - q.at < 30_000L ? q : null;
    }

    /** Reads the live fee off the main thread (the node for HARV and Harvana coins, the coin's own network otherwise). */
    private void loadQuote(String key, Runnable then) {
        if (quoting) return;
        quoting = true;
        final String from = src.from(), to = src.to() == null ? "" : src.to().trim();
        final double avail = availableCoins();
        if (from == null || from.isEmpty()) { quoting = false; return; }
        QuoteDone done = q -> main.post(() -> {
            quoting = false;
            if (q != null) quote = q;
            if (then != null) then.run();
            else refresh();
        });
        if ("HARV".equals(key)) {
            long raw = Double.isNaN(avail) ? 0 : Math.round(avail * 100_000_000d);
            HarvanaNodeClient.getFee(from, to.startsWith("HV0x") ? to : from, raw, new HarvanaNodeClient.JsonCallback() {
                @Override public void onSuccess(JSONObject j) {
                    long gas = j.optLong("estimatedGas", 0);
                    long reserve = gas + gas / 10;                                   // a little over: the fare follows HARV's price
                    done.accept(new Quote(key, to, java.math.BigDecimal.valueOf(gas).movePointLeft(8), "HARV",
                            java.math.BigDecimal.valueOf(Math.max(0, raw - reserve)).movePointLeft(8), true));
                }
                @Override public void onError(String e) { done.accept(null); }
            });
        } else if (com.harvana.app.husd.Stable.isStable(key)) {
            long units = Double.isNaN(avail) ? 0 : Math.round(avail * 1_000_000d);
            com.harvana.app.husd.HusdClient.fare(key, from, to.startsWith("HV0x") ? to : from, units, null, new com.harvana.app.husd.HusdClient.Cb() {
                @Override public void ok(JSONObject r) {
                    String payIn = r.optString("payIn", "HARV");
                    boolean inCoin = !"HARV".equals(payIn) && !"FREE".equals(payIn);
                    java.math.BigDecimal fee = "HARV".equals(payIn) ? java.math.BigDecimal.valueOf(r.optLong("harvGasRaw")).movePointLeft(8)
                            : java.math.BigDecimal.valueOf(r.optLong("coinFeeRaw")).movePointLeft(6);
                    long max = r.has("maxSendRaw") ? r.optLong("maxSendRaw") : units;
                    done.accept(new Quote(key, to, fee, "HARV".equals(payIn) ? "HARV" : src.symbol(),
                            java.math.BigDecimal.valueOf(inCoin ? max : units).movePointLeft(6), inCoin));
                }
                @Override public void fail(String e) { done.accept(null); }
            });
        } else {
            new Thread(() -> {
                Quote q = null;
                try {
                    com.harvana.app.wallet.ChainSend.Quote cq = "PI".equals(key) ? com.harvana.app.wallet.PiClient.quote(from)
                            : com.harvana.app.wallet.ChainSend.quote(key, from, to);
                    q = new Quote(key, to, cq.fee, cq.feeSymbol, cq.maxSend, cq.feeInSameCoin);
                } catch (Exception ignored) { }
                done.accept(q);
            }, "fee-quote").start();
        }
    }

    private String feeText(Quote q) {
        String f = q.fee.signum() == 0 ? "0" : q.fee.stripTrailingZeros().toPlainString();
        String money = q.sameCoin && moneyPerUnit > 0 ? "  (≈ " + FxRates.format(FxRates.symbol(c), q.fee.doubleValue() * moneyPerUnit) + ")" : "";
        return f + " " + q.symbol + money;
    }

    /** The coin figure on the screen's own "Available: 12.5 HARV" line. */
    private double availableCoins() {
        CharSequence t = anchor == null ? null : anchor.getText();
        if (t == null) return Double.NaN;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("([0-9][0-9,]*\\.?[0-9]*)").matcher(t.toString());
        return m.find() ? parse(m.group(1).replace(",", "")) : Double.NaN;
    }

    private void fee(String key, double coins, String worth) {
        boolean harvana = "HARV".equals(key) || com.harvana.app.husd.Stable.isStable(key);
        feeLabel.setText(harvana ? "tFare" : "Network fee");                 // other chains keep their own fee name
        Quote q = freshQuote(key);
        if (q == null) {
            fee("Reading…");
            loadQuote(key, null);
            return;
        }
        fee(feeText(q));                                                    // the fee alone; MAX already sends the most that fits
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
