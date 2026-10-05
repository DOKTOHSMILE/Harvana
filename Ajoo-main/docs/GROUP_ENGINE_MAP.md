# Group engine map: node (source of truth) vs. the app

Read from the node source (`core/*Engine.java`, `api/*Controller.java`, `model/*Request.java`). Read-only reference.
Amounts are raw units (1 HARV = 100,000,000). Every mutation is an Ed25519-signed request whose canonical message
is built by `chain/GroupCanonical` (verified against the node's own classes: 58/58 golden cases) and sent through
`sync/NodeGroupClient`. Request nonce = the wallet's `nextRequestNonce`.

## The four node group families

| Node family | Id prefix | App's current type | What it is |
|---|---|---|---|
| Circle | `CIRC-` | "Rotating Pool" | Rotating savings (Ajo/Osusu). 3-12 members, cycle 24-720 h. Bonds: every member locks `contribution x (members-1)`; the creator locks 2x that plus a 1 HARV creation fee (non-refundable). States: circle FORMING -> ACTIVE -> completed/aborted; member ACTIVE -> LATE -> DEFAULTED_PRE_PAYOUT / COMPLETED. 24 h grace; default penalty 7% (cap 10%). Joining needs the circle's `termsHash` (all members sign the same terms). Clean completion pays small HARV rewards (0.5 per clean member, 1.5 admin). |
| Accumulative (Accum) | `ACCUM-` | "Accumulative Funds" | Personal savings *lines* inside a group. Group params: `interestRateBps` (max 5000 = 50%), `withdrawalRule` = `ANYTIME` or `CYCLE_END_ONLY`; 1 HARV creation fee. A member starts a line: `dailyAmountRaw`, `autoCloseEnabled`, `lineType` FLEXIBLE or LOCKED (+`lockedUntilTimestamp`), `depositMode` ONGOING / SINGLE_LUMP_SUM / VARIABLE_AMOUNT (+`maxContributionRaw`). Lines last 30 days. Actions: contribute, contribute-multiple (catch-up), contribute-variable, pack (early close request), approve-pack (admin), claim, wallet-recovery. |
| Fixed | `FIXED-` | "Fixed Contribution" | Goal / project / emergency pool. Params: `targetGoalAmountRaw`, `beneficiaryAddress`, `interestRateBps`, name + description, `disbursementRule` = `ALL_AT_ONCE_AT_GOAL`, `ADMIN_TRIGGERED`, `MILESTONE_BASED` or `EMERGENCY_GRANT`. Members deposit; money leaves only by rule: request-disbursement (admin-triggered) + vote; milestone request + vote; grant request + vote. Group state ACTIVE -> COMPLETED. |
| Loans (GenLoan) | (inside a group) | app "loans" tab | Borrow from a group pool: `principalRaw`, `durationHours` 24-720, max pool utilisation 75%, 24 h grace; guardian-signature staging; repay. |

Cross-cutting: group-weighted voting (admin weight 40%, pass at 51% of total weight, quorum 15%), admin recovery
(request + vote, 14-day expiry), referral rewards (1 HARV; referred member must have paid >= 50 HARV and 30 days tenure),
vouching, staking (unstake/claim signed; stake is a plain transfer to the vault).

## Where the app diverges today (gaps)

1. **Group state lives in Firestore + Cloud Functions, not on the node.** `GroupCloudFunctions` (functions not in this
   repo), `GroupPayoutManager`, `GroupLoanManager`, `JoinRequestManager` write Firestore; `GroupOnChainGate` only checks
   that a tx hash exists. The node never learns the group exists. Until creation/contribution go through the node, the
   node's bonds, penalties, votes and payouts are not applied to app groups.
2. **Group ids and wallets are generated locally** (`groupWalletAddress`); the node derives ids (`CIRC-…`) and escrow
   addresses itself and returns the id from the create call.
3. **Create-group forms lack node parameters**: circle bond/fee disclosure; accum `withdrawalRule`, line types and
   deposit modes; fixed `disbursementRule` (4 values), beneficiary, milestones/grants. The app's Fixed type (day-grid
   monthly payments, `FixedPaymentEngine`) does not exist on the node.
4. **Voting**: node uses weighted votes with quorum; app uses its own thresholds in Firestore.
5. **Units/naming**: app labels still say HARV in Java strings (layouts were rebranded to HARV).

## Delivered so far
- `chain/GroupCanonical` + `GroupCanonicalTest` (58 golden cases, run in CI).
- `sync/NodeGroupClient`: all reads (`/circle`, `/accum`, `/fixed`, `/genloan`, admin-recovery, referral, staking) and
  signed actions for every family. Not yet called from any screen.

## Proposed UX per type (next step, needs your go-ahead on scope)
- **Circle**: create form shows *total to lock up front* (bond + 1 HARV fee) before signing; detail screen = round
  progress ring, "whose turn", your bond, next due date + grace countdown, late/defaulted badges; join flow shows terms
  (contribution, members, cycle, bond) and requires confirming the `termsHash`.
- **Accumulative**: detail = list of the member's lines (type chip, progress to 30 days, lock date), "Contribute",
  "Pack early" (admin approval status), "Claim" when eligible; create form exposes withdrawal rule + interest.
- **Fixed**: detail = goal thermometer, rule chip, open requests (disbursement/milestone/grant) each with vote bar and
  Yes/No buttons; create form exposes rule + beneficiary.
- **Loans**: request (principal, duration) with utilisation cap hint; repay with remaining due and grace countdown.
Migration approach: run node calls behind the existing screens one action at a time (create -> join -> contribute),
keeping Firestore only for presentation metadata (names, photos, chat/notices) that the node does not hold.

## Voting rules confirmed from node source (GroupGovernance.java, HarvanaProtocolManager constants)
- Applies to Accumulative and Fixed groups only: Fixed disbursement / milestone / emergency-grant votes and admin recovery. Circle has no votes. Accumulative has no vote endpoint (loan pools are admin/rule driven).
- Voter set is snapshotted when the proposal is created: admin 40% (4000 bps, fixed); the other 60% split evenly among up to the top 5 members by achievement (Accum: totalLinesCompleted, Fixed: contributedRaw). If fewer than 5 members have non-zero history, every eligible member votes. The admin never counts twice.
- Eligible members must have contributed at least once and have 48h tenure (Circle exempt). Migrated (recovered) wallets do not vote.
- Pass: yes weight >= 51% of snapshot total weight AND yes voters >= quorum AND at least one yes voter other than the requester. Quorum = min(eligible voters, max(2, ceil(15% of total members))), stored on the proposal (`minVoters`).
- Votes can be changed until the proposal resolves (`alreadyVoted` flag returned). Admin recovery proposals expire after 14 days; a proposal needs at least 2 eligible voters.
- App consequence: vote UI must show weight bps, yes/total weight, quorum (minVoters), and disable voting for non-eligible members; the Firestore group votes in the Loans/Payments tabs are app-only and do not reflect the node's rules.
