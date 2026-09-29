package com.harvanna.wallet;

public final class ChainConstants {
	
	private ChainConstants() {}
	
	// Address & Hash formatting
	public static final String ADDRESS_PREFIX = "HV0x";
	public static final int ADDRESS_HEX_LENGTH = 40;
	public static final int ADDRESS_TOTAL_LENGTH = 44;
	public static final int BLOCK_HASH_HEX_LENGTH = 64;
	public static final int BLOCK_HASH_TOTAL_LENGTH = 68;
	
	// Special Network Addresses
	public static final String GENESIS_ADDRESS = "HV0x0000000000000000000000000000000000000000";
	public static final String NULL_ADDRESS = "HV0x0000000000000000000000000000000000000000";
	public static final String GENESIS_MINER = "HV0xffffffffffffffffffffffffffffffffffffffff";
	
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
	
	// Fee distribution (reference; the node is authoritative)
	public static final double FEE_MINER_RATIO = 0.87;
	public static final double FEE_GOOD_BEHAVIOR_POOL_RATIO = 0.03;
	public static final double FEE_STAKING_POOL_RATIO = 0.10;
	
	// Fees must be fetched from GET /api/v1/tx/estimate-fee, never hardcoded client-side.
	
	// Mining & Halving Parameters
	public static final long HARV_INIT_REWARD_RAW = 125_000_000L; // 1.25 HARV
	public static final long HARV_HALVING_INTERVAL = 23_260_000L;
}
