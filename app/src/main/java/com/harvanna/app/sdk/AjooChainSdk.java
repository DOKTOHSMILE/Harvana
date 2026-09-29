package com.harvanna.app.sdk;

import android.content.Context;
import com.harvanna.app.AddressUtil;
import com.harvanna.app.sync.AjooNodeClient;
import com.harvanna.app.tx.TransactionRouter;
import com.harvanna.app.wallet.WalletGenerator;
import com.harvanna.app.wallet.WalletManager;

import org.json.JSONObject;

public class AjooChainSdk {
	
	// ==================== CALLBACKS ====================
	
	public interface WalletCreationCallback {
		void onSuccess(String walletAddress, String mnemonic);
		void onError(String error);
	}
	
	public interface WalletCallback {
		void onSuccess(String walletAddress);
		void onError(String error);
	}
	
	public interface NodeDataCallback {
		void onSuccess(JSONObject data);
		void onError(String error);
	}
	
	// ==================== WALLET METHODS ====================
	
	/**
* Creates a new wallet cryptographically and saves keys locally.
* No server or database registration is required for a new keypair.
*/	
	public static void createNewWallet(Context context, final WalletCreationCallback callback) {
		try {
			final String mnemonic = WalletGenerator.generateMnemonic(context);
			final String newAddress = WalletGenerator.deriveAddressFromMnemonic(mnemonic);
			final String privateB64 = WalletGenerator.derivePrivateKey(mnemonic);
			final byte[] seed = android.util.Base64.decode(privateB64, android.util.Base64.NO_WRAP);
			final byte[] pubKey = com.harvanna.app.crypto.Ed25519.publicKeyFromSeed(seed);
			final String publicB64 = android.util.Base64.encodeToString(pubKey, android.util.Base64.NO_WRAP);
			
			WalletManager.saveKeysLocally(context, newAddress, privateB64, publicB64);
			
			// Wallet exists locally. Node will recognize it upon first transaction.
			callback.onSuccess(newAddress, mnemonic);
			
		} catch (Exception e) {
			callback.onError("Failed to generate wallet: " + e.getMessage());
		}
	}
	
	/**
* Restores a wallet from a 12-word BIP-39 mnemonic.
* Simply derives the keys; does not fail if the node hasn't seen the address yet.
*/	
	public static void restoreWalletFromMnemonic(String mnemonic, final WalletCallback callback) {
		final String walletAddress = WalletGenerator.deriveAddressFromMnemonic(mnemonic);
		if (walletAddress == null || walletAddress.isEmpty()) {
			callback.onError("Invalid seed phrase format.");
			return;
		}
		
		// Return success immediately since the address is mathematically valid.
		// Balances can be fetched separately via the UI using fetchWalletData.
		callback.onSuccess(walletAddress);
	}
	
	/**
* Fetches current wallet balance and nonce directly from the Node.
*/	
	public static void fetchWalletData(String walletAddress, final NodeDataCallback callback) {
		if (!AddressUtil.isValidAddress(walletAddress)) {
			callback.onError("Invalid HV0x address format.");
			return;
		}
		
		AjooNodeClient.getWallet(walletAddress, new AjooNodeClient.JsonCallback() {
			@Override
			public void onSuccess(JSONObject json) {
				callback.onSuccess(json);
			}
			
			@Override
			public void onError(String error) {
				callback.onError("Node failed to fetch wallet: " + error);
			}
		});
	}
	
	// ==================== TRANSFER METHOD ====================
	
	/**
	 * Main transfer method for native Ajoocoin (AJC).
	 * Secondary token parameters have been removed to enforce native-only transactions.
	 *
	 * Previously bridged the node's wallet JSON into sender/recipient Maps
	 * and called TransactionRouter.executeTransaction() - that was the
	 * Firestore-simulated TOKEN path (now removed as dead code), wired in
	 * here by mistake even though this method's own doc comment says
	 * "native-only". Fixed to call routeTransaction() instead, the same
	 * node-authoritative native path every other send screen uses - it
	 * fetches nonce/fee from the node and signs itself, so the sender-doc
	 * bridging above was unnecessary work for the wrong destination anyway.
	 */
	public static void sendTransfer(final Context context, 
	final String senderAddress,
	final String recipientAddress,
	final double amount,
	final TransactionRouter.ExecutionCallback callback) {
		
		if (!AddressUtil.isValidAddress(recipientAddress)) {
			callback.onError("Invalid recipient HV0x address format.");
			return;
		}
		
		TransactionRouter.routeTransaction(
		context, senderAddress, recipientAddress,
		java.math.BigDecimal.valueOf(amount), null,
		new TransactionRouter.RouterCallback() {
			@Override
			public void onSuccess(String txHash) {
				callback.onSuccess(new com.harvanna.app.tx.ExecutionResult(txHash, "PENDING"));
			}
			@Override
			public void onError(String errorCode, String errorMessage) {
				callback.onError(errorMessage != null ? errorMessage : errorCode);
			}
		});
	}
	
	@Deprecated
	public static void sendTransfer(final String senderAddress, final String recipientAddress,
	final double amount,
	final TransactionRouter.ExecutionCallback callback) {
		if (callback != null) {
			callback.onError("Context is required for local transaction signing");
		}
	}
}
