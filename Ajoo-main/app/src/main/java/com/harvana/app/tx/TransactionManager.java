package com.harvana.app.tx;

import com.harvana.app.sync.FirestoreSyncManager;

/**
 * Transaction history lookups against the Firestore TRANSACTIONS mirror.
 *
 * Previously also had a full local sign-and-submit path (createTransaction,
 * signTransaction, verifySignature, submitSignedTransaction, send) built
 * around a model.Transaction object — that was only ever reachable from
 * TransactionRouter's Firestore-simulated token-transfer branch
 * (executeTransaction), which no UI screen actually exercises: every send
 * flow only ever offers native HARV. Native transfers sign and submit
 * directly in TransactionRouter.executeNativeTransfer() using Ed25519 and
 * HarvanaNodeClient - they never touched this class. Removed together with
 * the rest of that dead cluster (model.Transaction, gas.GasCalculator,
 * gas.GasFee, tx.CompleteFeeEngine) rather than leaving it half-wired.
 *
 * getTransaction()/getTransactions() are unrelated and still live -
 * WalletController (via IncomingTxWatcher) reads transaction history
 * through these.
 */
public final class TransactionManager {

    private TransactionManager() {}

    public interface TransactionListCallback {
        void onSuccess(java.util.List<java.util.Map<String, Object>> txs);
        void onError(String error);
    }

    public static void getTransaction(String txHash, FirestoreSyncManager.SyncCallback cb) {
        FirestoreSyncManager.getDocument("TRANSACTIONS", txHash, cb);
    }

    public static void getTransactions(String address, TransactionListCallback cb) {
        FirestoreSyncManager.queryTransactionsByAddress(address, cb);
    }

}
