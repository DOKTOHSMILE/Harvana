# Harvana Protocol — Group Subsystem & Multi-Coin Engine Diagnostic & Repair Report

## 1. Executive Summary
This document provides the technical audit, architectural refactoring, and resolution of the Harvana group savings circles, multi-coin engine, Web3 connectivity, anti-farm trust score system, and global regulatory framework.

---

## 2. Group Subsystem Diagnosis & Stuck Funds Elimination

### 2.1 The Vulnerability: Filtered Decision Actions
- **Previous Root Cause**: In `GroupPaymentFragmentActivity.java`, the method `isDecisionAction(a.id)` unconditionally suppressed vital lifecycle operations (`WITHDRAW_BOND`, `ACCUM_CLAIM`, `ACCUM_PACK`) on the Payments tab under the presumption that a separate Actions & Votes tab existed in `GroupDetailsActivity`. However, `GroupDetailsActivity` only contained six tabs (Overview, Members, Payments, Loans, Notice, History).
- **Impact**: When rotating savings circles completed or were aborted, members were unable to see or click the "Withdraw Bond" button. Similarly, in Accumulative groups, mature savings pots could not be claimed, leaving member funds mathematically locked in the escrow contract.
- **The Resolution**:
  1. Updated `GroupPaymentFragmentActivity.java` to explicitly permit and highlight actionable redemption buttons (`WITHDRAW_BOND`, `ACCUM_CLAIM`, `ACCUM_PACK`) with primary action buttons so funds can be withdrawn immediately upon maturity.
  2. Integrated a persistent `Actions, Votes & Governance` trigger that opens `GroupActionsActivity` directly from both the Overview and Payment views.
  3. Linked `FixedDissolve` and `EmergencyGrant` proposals so goal groups can dissolve with proportional member refunds if stalled.

### 2.2 Payment Processing & Node Escrow Sync
- Confirmed that `GroupPaymentHelper` records pending spend guards with automatic timeout releases (60s reserve, 3m broadcast) to eliminate double-spend locks while ensuring failed network transfers never prevent subsequent attempts.
- All payment confirmations trigger on-chain transaction receipt validation via `GroupOnChainGate` before mutating Firestore ledgers.

---

## 3. Grace Period & Default Forgiveness Implementation

### 3.1 On-Chain Grace Mechanism
- Node core `CircleEngine` and `ForgiveDefaultTest` enforce a grace window for members who miss scheduled round contributions.
- Members in default transition to `IN_GRACE` status, allowing them to cure their default with their scheduled payment plus gas before liquidation.
- Circle admins can propose `requestDefaultForgiveness` on the node for members experiencing extenuating circumstances.
- Eligible circle members cast weighted votes (`castDefaultForgivenessVote`). Defaulters are excluded from voting on their own cases.
- Passed forgiveness updates the ledger record to `FORGIVEN`, removes hard default flags, and updates the member's reputation label without fund loss.

---

## 4. Multi-Coin Engine & Rate-Limit Collective Caching

### 4.1 Distributed Firestore Shared Cache (`FirestoreSharedCache.java`)
- **Problem**: Individual clients directly querying public market APIs (CoinGecko, CoinMarketCap) suffer from severe HTTP 429 rate-limiting.
- **Solution**: Implemented single-leader distributed caching in `MARKET_CACHE` on Firebase Firestore:
  1. Any user requesting asset metadata or quotes first checks the shared Firestore cache.
  2. If fresh (`System.currentTimeMillis() - updatedAt < ttlMs`), the client uses the cached data with zero network API consumption.
  3. If expired, an atomic Firestore transaction elects a single leader client (`lockedUntil = now + 45s`). Only this leader queries external endpoints and commits fresh data to Firestore for all users globally.
  4. Non-leader clients read the previous snapshot (stale-while-revalidate), completely avoiding rate-limit lockout.

### 4.2 Multi-API Aggregation (`MultiCoinEngine.java`)
- Aggregates DexScreener, TrustWallet Assets repository, Binance 24h ticker, and Chainlist.
- When adding custom assets manually:
  - Fetches high-resolution logos, token symbols, names, and decimals via EVM contract calls and multi-API resolvers.
  - Automatically maps chain IDs and RPC endpoints.
  - Wires assets into `Web3Signer` and `DappBrowserActivity` so users can connect and swap them on decentralized exchanges (Uniswap, 1inch, PancakeSwap).

---

## 5. Anti-Farm Trust Score Engine (`AntiFarmTrustCollector.java`)

### 5.1 Protection Against Phone Farming Operations
- Phone farms operating racks of automated devices execute narrow, script-driven routines on single endpoints.
- `AntiFarmTrustCollector` monitors multi-subsystem feature breadth across:
  - Security PIN & Biometrics configuration
  - Mnemonic & Private Key backup verification
  - Web3 Browser exploration
  - Price chart and market analytics engagement
  - Group savings circle participation
  - P2P Escrow trade completion
- Incorporates device hardware entropy (sensor presence, battery variance, uptime continuity, and non-emulator hardware checks).
- Calculates a verified Human Trust Score (0–100%) and publishes verified ratings to `USERS/{uid}/TRUST_SIGNALS` and public `PROFILES`.

---

## 6. Multi-Jurisdictional Regulatory Protection & Legal Documentation
- Added comprehensive in-app legal terms (`legal_terms_of_service.txt`, `legal_privacy_policy.txt`) avoiding judicial complications across:
  - United States (FinCEN 2019 Unhosted Wallet Guidance & SEC software safe harbors)
  - European Union (MiCA Recital 22 Non-Custodial Protocol Exemption & GDPR)
  - United Kingdom (FCA Decentralized Finance Scope)
  - Nigeria & West Africa (CBN VASP Circular & Traditional ROSCA/Esusu protection)
  - United Arab Emirates (VARA Non-Custodial Exemption)
  - Singapore (MAS Payment Services Act Software Exemption)
- `DocumentViewerActivity.java` updated with a native, built-in reader sheet so users worldwide can inspect terms directly without requiring third-party software.

---

## 7. Multi-Language Usability
- Implemented localized resource directories (`values-es`, `values-fr`, `values-pt`, `values-ar`, `values-zh`, `values-sw`, `values-ha`) supporting global usability across the Americas, Europe, Africa, the Middle East, and Asia.
