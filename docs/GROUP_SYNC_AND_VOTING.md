# Groups: what the network holds, what Firestore holds, who decides

Written 5 Oct 2026 after a scan of the group screens. Read with `GROUP_ENGINE_MAP.md` (node rules) and `GROUP_INVENTORY.md`.

## The two halves of a group

| Half | Holds | Authority |
|---|---|---|
| The network (the node) | the group's wallet and every deposit, payout and bond; who is in the group; the rule that decides how money leaves; open votes | **Final for money and membership.** The app never moves a group's money by itself: it signs a request and the node carries it out |
| Firestore (`GROUPS/{id}` and its sub-collections) | name, photo, description, location, chat link, notices, invites, join requests, the profile's copy of counts | Presentation. Where it disagrees with the network, the network wins |

A group on the network has an id starting `CIRC-` (rotating), `ACCUM-` (accumulative) or `FIXED-` (goal). Its Firestore profile points at it (`nodeGroupId`). A group with no such id is a legacy Firestore-only group (its money is a wallet whose key is on the admin's phone). The screens decide which they are with `GroupIdentityResolver`.

## What each screen reads (scan, 5 Oct 2026)

| Screen | Rotating | Accumulative | Fixed goal | Notes |
|---|---|---|---|---|
| Header: Collected | Firestore payments | Firestore payments | **Network** (`raisedAmountRaw`) since this change | Fixed deposits never appear in Firestore payments |
| Overview | node + Firestore | node + Firestore | node + Firestore | Fixed wording now follows the real state (held, sending, sent) |
| Members | node roster merged with Firestore names | same | same | `_replaceWithNodeRoster` |
| Payments / actions | node | node | node | `GroupSnapshot` + `NodePayActions` |
| Loans | Firestore (legacy) + node repay | same | same | Network loans (`/genloan`) are only partly wired |
| Notices | Firestore | Firestore | Firestore | Presentation only, correct |
| Home totals and "upcoming payments" | Firestore | Firestore | Firestore | **Known gap**: wrong for network groups until they read the node |
| Footer buttons | **Actions & votes** | **Actions & votes** | **Actions & votes** | See below |
| Settings | profile + new Network control panel | same | same | New |

## Admin approval is honoured by the network

- Before: the footer showed the old Request payout / Approve payout / Apply for loan / Transfer funds buttons for every group. For a group on the network those only changed Firestore or tried to send from a phone wallet the group does not have, so an admin could "approve" and nothing happened on the network.
- Now: for a group on the network the footer is one button, **Actions & votes**, which opens the network screen. Every action there is a signed request the node verifies and carries out: request payout, request milestone, vote, approve an early withdrawal, claim. The old Firestore buttons remain only for legacy groups.
- The admin's request counts as the admin's yes vote. The app casts it straight after the request (the node's quorum counts yes votes, and a two-person group could not finish otherwise). Passing still needs a yes from someone other than the requester.

## What starts a vote, and how it is decided

Voters (fixed when the vote starts): the admin counts 40%; the other 60% is shared by the five members who have put in the most (everyone, if fewer than five have put anything in). A member votes only if they have paid in at least once and joined at least 48 hours ago. Pass: 51% of the weight says yes, at least two yes votes, at least one from someone other than the requester. A vote can be changed until it is decided.

| Group | Triggers | Who decides |
|---|---|---|
| Fixed, admin asks, the group votes | Admin requests a payout | Weighted vote |
| Fixed, milestones | Admin requests a milestone release | Weighted vote |
| Fixed, emergency grants | A member asks for a grant | Weighted vote |
| Fixed, all at once | Goal reached | Nobody: pays the wallet chosen at creation |
| Accumulative | A member asks to take savings out early | Admin alone |
| Rotating | Nothing about money is voted | The schedule; bonds cover missed payments |
| Any network group | A member asks to take over from a missing admin (admin recovery) | Weighted vote, 14 days |

Admin recovery now has screens: Group settings, Network control, Admin recovery. A member asks, the voters see who asked and the percentage so far, and vote.

## Settings: network control

Group settings has a Network control panel for groups on the network: the id and group wallet (copy), state, the rule for how money leaves, the admin, and whether the profile and the network agree on the member count. The admin can update the profile from the network in one tap. The panel also states who decides and what starts a vote.

## Known gaps (not done)

1. Home totals and upcoming payments still add up Firestore payments, so they are wrong for network groups.
2. Loans for network groups are not a full network flow.
3. No vote exists on the network for admitting a member (joining a rotating group is open and bond-locked; accumulative and fixed have no join step).
4. Nothing starts a new goal in a finished fixed group. That needs a node change.
5. The Firestore member count is updated by the admin from Settings, not automatically.
