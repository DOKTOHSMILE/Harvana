package com.ajoo.app;

public final class ChainConstants {
	
	private ChainConstants() {}
	
	// Address & Hash formatting
	public static final String ADDRESS_PREFIX = "AJ0x";
	public static final int ADDRESS_HEX_LENGTH = 40;
	public static final int ADDRESS_TOTAL_LENGTH = 44;
	public static final int BLOCK_HASH_HEX_LENGTH = 64;
	public static final int BLOCK_HASH_TOTAL_LENGTH = 68;
	
	// Special Network Addresses
	public static final String GENESIS_ADDRESS = "AJ0x0000000000000000000000000000000000000000";
	public static final String NULL_ADDRESS = "AJ0x0000000000000000000000000000000000000000";
	public static final String GENESIS_MINER = "AJ0xffffffffffffffffffffffffffffffffffffffff";
	
	// Firestore Collection Names
	public static final String COL_WALLETS = "WALLETS";
	public static final String COL_TRANSACTIONS = "TRANSACTIONS";
	public static final String COL_BURNS = "BURNS";
	public static final String COL_FEE_HISTORY = "FEE_HISTORY";
	public static final String COL_TOKENS = "TOKENS";
	public static final String COL_LIQUIDITY_POOLS = "LIQUIDITY_POOLS";
	public static final String COL_CHAIN_STATE = "CHAIN_STATE";
	public static final String COL_GENESIS = "GENESIS";
	public static final String COL_BLOCKS = "BLOCKS";
	
	// Firestore Document Keys
	public static final String DOC_CHAIN_CURRENT = "current";
	public static final String DOC_GENESIS_BLOCK = "block_0";
	
	// Treasury & Fee Distribution Ratios (Reference)
	public static final double FEE_BURN_RATIO = 0.50;
	public static final double FEE_TREASURY_RATIO = 0.20;
	public static final double FEE_MINER_RATIO = 0.30;
	
	// Gas Constants (in AJC)
	public static final double GAS_BASE_AJC = 0.00021;
	public static final double GAS_LISTED_TOKEN = 0.00035;
	public static final double GAS_UNLISTED_TOKEN = 0.00050;
	
	// Mining & Halving Parameters
	public static final double AJC_INIT_REWARD = 50.0;
	public static final long AJC_HALVING_INTERVAL = 210000L;
}
