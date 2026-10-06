package com.harvana.app.receipt;

import com.harvana.app.ui.PaymentSuccessSheet;

/** Builds a {@link Receipt} from the places that have the facts. */
public final class Receipts {
    private Receipts() {}

    /** From the success sheet after a payment. */
    public static Receipt fromSuccess(PaymentSuccessSheet.Info info) {
        Receipt r = new Receipt();
        r.title = info.title == null || info.title.isEmpty() ? "Payment receipt" : info.title;
        r.status = info.confirmed ? "CONFIRMED" : "PENDING";
        String amount = info.amount == null ? "" : info.amount.trim();
        int sp = amount.lastIndexOf(' ');
        if (sp > 0) { r.symbol = amount.substring(sp + 1); r.amount = amount.substring(0, sp); } else r.amount = amount;
        r.direction = -1;
        r.timestamp = System.currentTimeMillis();
        r.reference = info.txHash == null ? "" : info.txHash;
        Receipt.Section pay = r.section("Payment");
        String hash = "";
        if (info.rows != null) for (String[] row : info.rows) {
            if (row == null || row.length < 2) continue;
            if ("Transaction".equalsIgnoreCase(row[0])) { hash = row[1]; continue; }
            pay.row(row[0], row[1]);
        }
        if (r.reference.isEmpty()) r.reference = hash;
        Receipt.Section net = r.section("Network");
        net.mono("Transaction hash", r.reference);
        return r;
    }

    /** From the transaction detail screen. */
    public static Receipt fromTransaction(String title, boolean incoming, String status, String amountText, String symbol, long timestamp,
                                          String from, String to, String memo, String fee, String burned,
                                          String txHash, long block, long confirmations, long nonce) {
        Receipt r = new Receipt();
        r.title = title == null || title.isEmpty() ? (incoming ? "Received" : "Sent") : title;
        r.status = status == null ? "" : status.toUpperCase(java.util.Locale.US);
        r.amount = amountText;
        r.symbol = symbol;
        r.direction = incoming ? 1 : -1;
        r.timestamp = timestamp;
        r.reference = txHash == null ? "" : txHash;
        r.section("Parties").row("From", from).row("To", to).row("Memo / note", memo);
        r.section("Fees").row("Network fee", fee).row("Burned", burned);
        r.section("Network").mono("Transaction hash", txHash).row("Block", block > 0 ? String.valueOf(block) : "Pending")
                .row("Confirmations", confirmations > 0 ? confirmations + "+" : "0 (pending)").row("Nonce", String.valueOf(nonce));
        return r;
    }
}
