# Group subsystem repair

This is the first pass of the group-authority repair. It does not claim that every screen is migrated.

## 1. Baseline

Not re-run in this pass. The last known Android unit tests for group helpers were already in `app/src/test`. Node Maven tests were not part of this change. Remaining full Gradle and Maven runs are still required before calling the repair done.

## 2. Capability matrix

| Protocol | Node | Android caller now |
|---|---|---|
| Circle read | `GET /api/v1/circle/{id}` | `GroupRepository.loadProtocol` → `NodeGroupClient.getCircle` |
| Accum read | `GET /api/v1/accum/group/{id}` | `GroupRepository.loadProtocol` → `NodeGroupClient.getAccumGroup` |
| Fixed read | `GET /api/v1/fixed/group/{id}` | `GroupRepository.loadProtocol` → `NodeGroupClient.getFixedGroup` |
| Fixed disbursement | `POST /api/v1/fixed/request-disbursement` | `GroupPayoutManager.requestPayout` → `GroupRepository.requestPayout` → `NodeGroupClient.requestFixedDisbursement` |
| GenLoan request | `POST /api/v1/genloan/request` | `GroupLoanManager.applyForLoan` → `GroupRepository.requestLoan` → `NodeGroupClient.requestLoan` |
| Identity | Firestore document id is not a node id | `GroupIdentityResolver.resolve` |

Circle payout order and Accum claims are not given a fake Firestore payout. The payout button tells the member that.

## 3. Defects fixed

| Severity | Where | Cause | Fix |
|---|---|---|---|
| P0 | `CreateNewGroupActivity._submitNewGroupData` | A random UUID was stored as `groupId` beside the real `CIRC`/`ACCUM`/`FIXED` id | The record now also stores `firestoreDocId`, `nodeGroupId` and `family`. Protocol calls use the node id |
| P0 | `GroupPayoutManager.requestPayout` / `approvePayoutAndTransfer` | Firestore payout documents were treated as money | On-chain groups go to the node, or are refused. Legacy groups still use the app record |
| P0 | `GroupLoanManager.applyForLoan` | A second loan balance lived in Firestore | On-chain groups submit a signed GenLoan request. Duration is 24h or 720h, the node's own limits |
| P1 | `GroupDetailsLogic._applyGroupData` | Admin was `creatorId` Firebase uid | On-chain admin is the wallet that matches the node creator. Contribution, slots, escrow and round come from the node |

## 4. Tests

`GroupIdentityResolverTest` covers: a UUID is never the protocol id, the three prefixes, a legacy `groupId` field that is already a node id, a missing mapping, a broken explicit id, and a Firebase uid that cannot replace the node id.

## 5. Migration

Existing documents are not rewritten and not deleted. `GroupIdentityResolver` classifies them:

- `nodeGroupId`, `circleId` or `nodeId` with a real prefix → on-chain
- those fields set to something else → `BROKEN_MAPPING`, and a protocol-looking `groupId` is not used to paper over it
- no node id → `LEGACY_OFFCHAIN`

No new node identity is created for an old group.

## 6. Files

Added: `GroupIdentity`, `GroupIdentityResolver`, `GroupRepository`, `GroupCapability`, `GroupActionState`, `GroupIdentityResolverTest`, `Ajoo-main/NOT_THE_BUILD.txt`.

Changed: `GroupKinds`, `NodeGroupClient.familyOf`, `CreateNewGroupActivity`, `GroupDetailsLogic`, `GroupPayoutManager`, `GroupLoanManager`.

Deleted: none. `Ajoo-main` is not the Gradle module (`settings.gradle` includes only `:app`). It is marked, not removed.

## Continuation

- Circle join signs `POST /api/v1/circle/join` with the circle's `termsHash`. An admin approval no longer adds that person in Firestore.
- The members tab loads the node roster and uses Firestore only to put a name on a wallet the network already lists.
- A GenLoan request is mirrored as `source: node` under its `GENLOAN-` id. The list refreshes status, principal and repaid amount from the node. Repay calls `POST /api/v1/genloan/repay` and does not send a second transfer. The app does not disburse a network loan.
- Settings can still change the profile. On a network group it does not write a new contribution or cycle rule.

Still open: Accumulative and Fixed groups have no separate join endpoint, so a social request remains, and a full Gradle run was not executed in this pass.

## Votes and guardians

- Fixed votes load from `GET /api/v1/fixed/group/{id}/proposals`. Accumulative withdrawals load from `GET /api/v1/accum/group/{id}/pending-packs`. Notices are only a fallback if that read fails.
- Admin recovery is read from `GET /api/v1/admin-recovery/group/{id}`. Offering the seat and voting call the signed node routes.
- A network loan shows "Waiting for guardians" and cannot be paid out by a group admin. A guardian signs it in the guardian console, which posts `POST /api/v1/genloan/stage-guardian-signature`.

## 7. Still open

- Accumulative and Fixed groups have no separate join endpoint, so a social request remains.
- A full Gradle and node test run was not executed in this pass.

## 8. Path that is now real

Payout on a Fixed group: button → `GroupPayoutManager.requestPayout` → `GroupRepository.classify` → `GroupRepository.requestPayout` → `NodeGroupClient.requestFixedDisbursement` → `POST /api/v1/fixed/request-disbursement`.

Loan request: button → `GroupLoanManager.applyForLoan` → `GroupRepository.requestLoan` → `POST /api/v1/genloan/request`.

Loan repay: repay on a `GENLOAN-` id → `NodeGroupClient.repayLoan` → `POST /api/v1/genloan/repay`.

Circle join: join button → read `termsHash` → `NodeGroupClient.joinCircle` → `POST /api/v1/circle/join`.

Details: Firestore profile → `GroupIdentityResolver` → `GroupRepository.loadProtocol` → the family GET → creator, escrow, contribution and round replace the profile numbers.
