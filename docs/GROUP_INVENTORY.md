# Group restructure - Phase 1 inventory (30 Sep 2026)

Method: static search of every call to `GroupFirestoreManager` (39 files, ~40 distinct operations). Classes: (a) money/membership meaningful, must become a signed node call; (b) Firestore-native, may stay; (c) the node has no equivalent endpoint, a node-side decision for the owner. Node repo was only read.

## Table

| Operation (Firestore) | Where used | Class | Node equivalent | State today |
|---|---|---|---|---|
| createGroup, addMember (creator) | CreateNewGroupActivity | (a) money, (b) profile | Circle: `POST /circle/create`. Accum: `/accum/create-group`. Fixed: `/fixed/create-group` | **Rotating: done** (creates the circle, then the Firestore group linked by `circleId`). Accum, Fixed: still Firestore only |
| join requests: addJoinRequest, getJoinRequest(s), updateJoinRequestFields, castJoinVote, queryMyJoinRequests | GroupJoinRequestActivity, JoinRequestManager, GroupMembersFragment, NotificationSync | **(c)** | None. Circle joins are open on the node (bond-locked, no admin approval). Accum/Fixed have no join call at all (a member exists after the first line/deposit) | Stays Firestore. Approval is an app-level gate only. **Owner decision needed** (see below) |
| membership: getMember(s), updateMemberFields, addMember (join), queryMyMemberships | 9 screens | (a) | Circle: `GET /circle/{id}/member/{addr}`; Accum/Fixed: `GET .../member/{addr}` | Firestore is still what screens read. Not yet node-derived |
| payments: addPayment, updatePaymentStatus, getPayments | GroupPaymentFragment, GroupPaymentHelper, FixedMonthCloseHelper, FixedPaymentEngine, Home, TrustScore*, Overview | (a) | Circle `/circle/contribute`; Accum `/accum/contribute*`; Fixed `/fixed/deposit` | **Rotating: routed** (Payments tab opens the circle screen; contribute is a signed node call). Legacy Firestore payments remain for old groups and for Accum/Fixed |
| loans: addLoan, updateLoan, getLoan(s) | GroupLoanManager, GroupLoansFragment, OpenLoans, Home, TrustScore* | (a), out of scope | GenLoan engine (`/genloan/...`) | Firestore only. Plan says handle GenLoan last |
| payoutRequests: add/get/updatePayoutRequest | GroupPayoutManager | (a) | Circle: automatic payout by the node, no request. Accum: pack/claim/approve. Fixed: request + weighted vote | Firestore only. For Rotating groups the request flow is obsolete |
| votes (join votes) | GroupMembersFragment | **(c)** | Node votes exist only for Fixed (disbursement, milestone, grant) and admin recovery | App-level vote stays Firestore |
| notices: addNotice, getNotices, postSystemNotice, reminders | 8 files | (b) | none, not needed | Stay Firestore-native (as the plan suggests) |
| getGroup, queryGroups | 18 uses | (b) for text/metadata | `GET /circle|accum|fixed/...` for money fields | Text from Firestore. Money fields (saved total, round, roster) should come from the node for linked groups |
| private docs: walletCard, chatLink; INVITES; profile `groups` list | Group*Store, InviteManager | (b) | none | Firestore. `walletCard` applies only to non-on-chain groups |
| submitReport (member reports) | GroupMembersFragment | (b) | none | Firestore |
| Learn hub content | Learn* | unrelated | none | Firestore |
| Totals and scores derived from Firestore payments/loans | Home, TrustScoreEngine, TrustScoreActivity, OpenLoans | (a) | derive from node member records | Still Firestore-derived. Wrong for on-chain groups until switched |

## What is done for Circle (Phase 2) and what is not
Done: one create flow that creates the circle on the node with a disclosed cost; the group is stored with `circleId`; the join step sends people to the node's bond-locking join; contribute, bond withdrawal, roster and round are shown from the node on the circle screen.

Not done for Circle, before the plan's Phase 2 can be called finished:
1. Group Overview/Members/History still read Firestore; they should read the node's circle record for round, roster and payouts.
2. Home "Upcoming payments", Total saved, Trust score and Open loans are computed from Firestore, so on-chain groups show wrong figures.
3. The Firestore member list and the on-chain roster can differ.
4. Old rotating groups (Firestore-only) still exist with the old wallet flow.
5. The whole flow has never been run against the real node from a device.

## Node-side gaps for the owner (class c)
1. No admission control or approval step for Circle, Accum or Fixed. Decide: keep an app-level approval only (current), or add a node "membership request" concept.
2. No node "member vote" for admitting someone; node votes are only for Fixed spends and admin recovery.
3. Accum and Fixed have no explicit join transaction, so "signed the contract and joined" cannot be recorded on the node before the first deposit.
4. No node endpoint returns a user's list of groups, so the app still needs its own mapping (profile `groups`) from a user to group ids.

## STOP - owner decision before Accum/Fixed or any wipe
Has any real person, other than the developer, used the group feature with real value or real expectations (the screenshots show a second member in "Eguonor special group 1")? If yes, existing Firestore groups have no on-chain backing. Options: archive and start clean, keep them read-only as "legacy", or reconcile by hand. Nothing has been deleted or migrated by this work.

## Owner decisions received (30 Sep) and applied
1. Existing Firestore-only rotating groups are test data: they are hidden from Discover, My Groups, Home (trending and upcoming payments). Nothing was deleted; wipe them in the Firebase console when you like. (`GroupKinds.isLegacyRotating`.)
2. Joining is open on the node for on-chain groups: new Rotating groups are saved with `requireApprovalToJoin=false`, the approval switch is disabled for Rotating, members join in the app, then lock their bond on the network.
Accumulative and Fixed are unchanged until their turn in the agreed order.
