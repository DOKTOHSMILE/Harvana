# Firestore rules review (deployed rules pasted by the owner, 29 Sep 2026)

Method: static read of the pasted rules against the app's actual Firestore calls (code search). NOT run against the
Firebase Emulator, and nothing was deployed. Treat as a review, not a proof. Severity is my judgement.

## A. Security problems (the rules allow more than they should)

| # | Severity | Finding |
|---|---|---|
| A1 | **High** | `GROUPS/{g}/members/{uid}` `create`: a user may create THEIR OWN member doc with `status == 'ACTIVE'` in ANY group (`request.auth.uid == memberUid`). That bypasses join approval entirely, and `isActiveMember()` then grants read access to that group's payments, loans, payout requests and notices. |
| A2 | **High** | `USERS/{uid}` `read: if signedIn()`. The profile holds DOB, ID number (`v_type` + the ID field), phone, home address, GPS, next-of-kin and trust score. Any signed-in account can read every user's full record. Firestore rules cannot hide individual fields. Needs a data split (public card vs private doc), which is an app change I can make. |
| A3 | Medium | `joinRequests` `update`: any ACTIVE member can change `status` (e.g. to APPROVED), not just cast a `votes` entry. One member can approve any applicant. |
| A4 | Medium | `payments` update: any member can mark a payment CONFIRMED with any `txRef`. The only check is client-side (`GroupOnChainGate`). The node, not the client, should confirm. |
| A5 | Medium | `TRANSACTIONS` `create: if signedIn()`: anyone can forge a "confirmed" transaction record. The app's only writer (`FirestoreSyncManager.recordTransaction`) has **no callers**, so client writes can be closed with no functional loss. |
| A6 | Low | `members` `create` shape check is `keys().hasAny([...])`, which is almost always true; role is not constrained, so a self-created member doc could set `role: 'ADMIN'`. |

## B. Functional breakage (the app needs writes the rules deny)

| # | Finding |
|---|---|
| B1 | `USERS/{uid}` `update: isSelf`. `JoinRequestManager.finishAdmission()` / `reactivateMember()` write `USERS/{newMember}.groups` from the ADMIN's (or voting member's) device. That is a cross-user write, so it is denied and the new member never gets the group in their profile. |
| B2 | `GROUPS/{g}` `update: isGroupCreator`. `reconcileMembersCount()` writes `membersCount` from whoever admits/leaves (a joining member, a voter). Denied for non-creators. |
| B3 | `members` `create` by an approver who is neither the creator nor the new member (vote-approval path, `admitMember`) is denied. |
| B4 | `GroupCloudFunctions` calls Cloud Functions (`approveJoin`, `rejectJoin`, vote) whose source is not in this repo; if they are not deployed, B1-B3 are the fallback path and they fail. Please confirm whether they are deployed. |
| B5 | Collections the app references that the catch-all denies: `WALLETS` (only via `WalletManager` helpers that have no callers except an internal one). Probably harmless, listed for completeness. |

Everything the app does in `USERS` for the signed-in user themself (sign-up `saveProfile`, `updateProfileFields`, presence) IS allowed
by `isSelf`, so **the new-user sign-up profile save should work** against these rules. The risks above are exposure of that data
(A2) and group flows (B1-B3), not the sign-up write itself.

## C. Proposed direction (owner decides; nothing applied)

1. Close A1/A6 now: members may self-create only if the group does not require approval (`requireApprovalToJoin == false`)
   or their join request is APPROVED, with `role == 'MEMBER'`. See `docs/firestore.rules.proposed`.
2. Close A5: `TRANSACTIONS` client writes off.
3. A3: only the creator may change `joinRequests.status`; members may only add their own vote.
4. Fix B1-B3 the durable way: **move admissions, votes and counts to the node** (it is the source of truth; the
   Circle/Accum/Fixed engines already do weighted voting and membership). Until then, either deploy the Cloud Functions or
   loosen rules narrowly (`membersCount` only). This is the group-rewire work already planned, starting with Circle.
5. A2: create `USERS/{uid}/private/kyc` (owner-only) for DOB/ID/address/GPS/kin and keep only name, icon, trust score,
   `lastActiveAt` in the readable doc. Requires an app change + a one-time data move; I can implement it on request.

Do not start real sign-ups until A1 and A2 are decided: A1 lets anyone join any group; A2 exposes all sign-up data.
