package com.harvanna.app.tx;

public class ExecutionResult {
    public final String txHash, status;
    public ExecutionResult(String txHash, String status) {
        this.txHash = txHash;
        this.status = status;
    }
}