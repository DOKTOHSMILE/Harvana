package com.harvana.app.receipt;

import android.os.Handler;
import android.os.Looper;

import com.harvana.app.sync.HarvanaNodeClient;
import com.harvana.app.ui.PaymentSuccessSheet;

import org.json.JSONObject;

import java.util.Locale;

/**
 * Makes every receipt carry the same facts. The success sheet only knows a few rows, so before sharing we read the
 * transaction from the node by hash and build the same full receipt the transaction detail screen shows: parties, memo,
 * fees, burned, hash, block, confirmations and nonce. If the node cannot be reached the short receipt is used.
 */
public final class ReceiptLoader {
    private ReceiptLoader() {}

    public interface Done { void onReady(Receipt receipt); }

    public static void forSuccess(final PaymentSuccessSheet.Info info, final Done done) {
        final Receipt fallback = Receipts.fromSuccess(info);
        final Handler main = new Handler(Looper.getMainLooper());
        if (info.txHash == null || info.txHash.isEmpty()) { done.onReady(fallback); return; }
        HarvanaNodeClient.nodeGet("/api/v1/tx/" + info.txHash, new HarvanaNodeClient.JsonCallback() {
            @Override public void onSuccess(JSONObject tx) {
                Receipt full;
                try { full = build(info, fallback, tx); } catch (Exception e) { full = fallback; }
                final Receipt out = full;
                main.post(() -> done.onReady(out));
            }
            @Override public void onError(String error) { main.post(() -> done.onReady(fallback)); }
        });
    }

    private static Receipt build(PaymentSuccessSheet.Info info, Receipt fallback, JSONObject tx) {
        String symbol = fallback.symbol == null || fallback.symbol.isEmpty() ? com.harvana.app.CoinInfo.ticker() : fallback.symbol;
        String from = tx.optString("from", "");
        String to = tx.optString("to", "");
        String memo = tx.optString("memo", "");
        long block = tx.optLong("blockHeight", tx.optLong("block", 0L));
        long conf = tx.optLong("confirmations", 0L);
        long nonce = tx.optLong("nonce", 0L);
        long ts = tx.optLong("timestamp", fallback.timestamp);
        double fee = HarvanaNodeClient.toHarvDisplay(tx.optLong("gas", tx.optLong("fee", 0L)));
        double burned = tx.optDouble("burnAmount", 0.0);
        String status = conf > 0 || block > 0 ? "CONFIRMED" : (info.confirmed ? "CONFIRMED" : "PENDING");
        String amount = fallback.amount;
        if (amount == null || amount.isEmpty()) amount = money(HarvanaNodeClient.toHarvDisplay(tx.optLong("amount", 0L)));
        return Receipts.fromTransaction(info.title, false, status, amount, symbol, ts > 0 ? ts : fallback.timestamp,
                from, to, memo, money(fee) + " " + symbol, money(burned) + " " + symbol, info.txHash, block, conf, nonce);
    }

    private static String money(double v) {
        String s = String.format(Locale.US, "%.6f", v);
        s = s.indexOf('.') >= 0 ? s.replaceAll("0+$", "").replaceAll("\\.$", "") : s;
        return s.isEmpty() ? "0" : s;
    }
}
