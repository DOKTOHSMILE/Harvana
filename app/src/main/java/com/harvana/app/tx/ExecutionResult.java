package com.harvana.app.tx;

public class ExecutionResult {
    public final String txHash, status;
    public ExecutionResult(String txHash, String status) {
        this.txHash = txHash;
        this.status = status;
    }
}