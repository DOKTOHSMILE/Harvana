package com.harvanna.wallet.tx;

import android.content.Context;

public class TransactionRequest {
    public transient Context context;
    public String from, to, tokenContractAddress, tokenSymbol, memo;
    public double amount;

    /** Matches the node's TransactionPayload.MAX_MEMO_LENGTH exactly - a
     *  memo signed under a different limit than the node enforces would
     *  either get silently truncated inconsistently or rejected outright. */
    public static final int MAX_MEMO_LENGTH = 200;

    public TransactionRequest() {}

    public TransactionRequest(String from, String to, String tokenContractAddress, String tokenSymbol, double amount) {
        setSender(from); setRecipient(to); setContractAddress(tokenContractAddress); setTokenSymbol(tokenSymbol); setAmount(amount);
    }

    public void setSender(String v) { if (v == null || v.trim().isEmpty()) throw new IllegalArgumentException("from is required"); from = v.trim(); }
    public void setRecipient(String v) { if (v == null || v.trim().isEmpty()) throw new IllegalArgumentException("to is required"); to = v.trim(); }
    public void setContractAddress(String v) { tokenContractAddress = v == null ? "" : v.trim(); }
    public void setTokenSymbol(String v) { tokenSymbol = v == null || v.trim().isEmpty() ? "AJC" : v.trim(); }
    public void setAmount(java.math.BigDecimal v) { setAmount(v == null ? 0.0 : v.doubleValue()); }
    public void setAmount(double v) { if (!Double.isFinite(v) || v <= 0.0) throw new IllegalArgumentException("amount must be finite and greater than zero"); amount = v; }
    public void setMemo(String v) {
        if (v == null) { memo = null; return; }
        String trimmed = v.trim();
        memo = trimmed.length() > MAX_MEMO_LENGTH ? trimmed.substring(0, MAX_MEMO_LENGTH) : trimmed;
    }
}
