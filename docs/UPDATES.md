# Updates

**Standing rule (owner instruction, 1 Oct 2026).** Every change pushed to this repository that an operator has to apply by hand gets a dated entry in this file, in the same commit. That includes a rules publish, a config key, and an app build the phone must install. A code change that cannot go live by itself is unfinished without the entry. The node repository keeps its own steps in `docs/DEPLOYMENT.md`. Do not put those steps here.

## 5 Oct 2026: a separate create screen for each group type, and a friend picker

What the app does. Rotating, savings and goal groups now have their own create flow, in four steps (Basics, the type's rules, People, Review), and each asks for different things in its own words. Rotating: contribution, number of members, round length, with a live summary of the pot, the bond each member locks, and how long it runs. Savings: whether members can borrow and at what interest, and when savings can be taken out. Goal: what the group is for (one-off purchase, a cause the admin pays out, a project in stages, an emergency fund), the target, the rule for how money leaves and who votes, and who receives it, with a clear notice when the receiver is the admin. The old payout-order options (random, first-come, auction) are gone for rotating groups because the network pays in the order members join. The review shows the 1 HARV fee, that nothing is locked, and that the network makes the group wallet. Add friends opens a picker: search by name or @username, tap a friend to add just them, press and hold to tick several (a checkbox then appears on every friend and a tap ticks or unticks), Select all, and find someone by exact username who is not yet a friend.

What you do. Install an app build from this date. Nothing on the node or in Firestore changes for this.

What did not change. The creation calls to the node, the profile saved in Firestore and the invitations are the same as before. The old single create form is still in the code but nothing opens it.

## 5 Oct 2026: creating a group locks nothing; the group wallet is the network's

What the app does. No group creation makes a wallet on the phone. The group wallet is an address the network derives from the group id; no key exists for it, so only the node's rules can move its money. Creating a rotating group no longer locks the admin's double bond: the review sheet says "Locked now: Nothing", the admin pays only the 1 HARV creation fee, and then joins first, like everyone else, locking the ordinary bond. Until the admin has joined, other members see "Nobody has joined yet. The admin joins first" and the node refuses their join with a clear message. Savings and goal groups were already fee-only.

What you do, in this order:

1. Deploy the node first (node repository `docs/DEPLOYMENT.md`, "creating a rotating circle locks nothing"). The change is in the node: an old node still takes the double bond at creation.
2. Then install an app build from this date. An old app against the new node still works; the admin just has to tap Join afterwards.

What did not change. Circles created before the node swap keep their admin as member 1 with the double bond locked; nothing is refunded or moved.

## 5 Oct 2026: one build file, `app/module.toml`

What changed. CodeAssist builds from `app/module.toml`, and GitHub Actions used a separate `app/build.gradle`, so the two could disagree. CI now runs `tools/module_toml_to_gradle.py` first, which writes `app/build.gradle` from the toml: package name, SDK levels, version, build types and every dependency. Edit the toml, not the Gradle file; `python3 tools/module_toml_to_gradle.py --check` tells you whether they match. The toml keeps `core-1.13.0.aar` and `material-1.11.0.aar` as local files, as you build with them in CodeAssist, and now lists everything those two files need and do not bring themselves (a local `.aar` carries none of its own dependencies, which is why CodeAssist could not find `cardElevation`, `drawerLayoutStyle` or `contentPadding`): CardView, DrawerLayout, ConstraintLayout, CoordinatorLayout, RecyclerView, ViewPager2, Transition, VectorDrawable, Fragment, Lifecycle, Collection, Concurrent, Interpolator, VersionedParcelable and the annotation libraries. For Gradle the script swaps those two local files for the same versions from Maven, because a local `.aar` next to the same library arriving through AppCompat is a duplicate-class error.

What you do. Pull, and in CodeAssist reopen the project so it re-reads `module.toml`. The files in `app/libs` stay: CodeAssist uses them. The manifest warning about MPAndroidChart's version attributes is harmless.

## 5 Oct 2026: P2P trade chat, network actions for groups, Network control in group settings

What the app does. A P2P trade has a "Chat with the trader" link on the buyer's and the seller's trade screens: a small chat between the two people on that order, with a reminder to pay only into the escrow shown on the trade. Messages are kept for 30 days after the last one so a dispute can be shown what was said; the first of the two people to open the chat after that deletes it (there is no server job). The other person gets a notification when you write, with the other notification checks. For a group on the network the footer is now one button, "Actions & votes", which opens the network actions (the old Firestore payout and loan buttons stay only for legacy groups); an admin's payout or milestone request counts as the admin's yes vote. Group settings gains a Network control panel: the network id and wallet, whether the profile and the network agree, who decides and what starts a vote, and a screen for admin recovery.

What you do, in this order:

1. Publish the Firestore rules first: open `docs/firestore.console.rules`, select all, paste over the Firebase console editor and publish. It adds `P2P_CHATS` (and its messages). Do not copy rules out of a chat message.
2. The node does not need a new jar for this change.
3. Install an app build from this date or later.

What this does not do. The chat is not end-to-end encrypted: it is stored in Firestore for 30 days, readable by the two people and by whoever administers your Firebase project. There is no moderator view yet, so for a dispute a party shows the chat themselves. See `docs/GROUP_SYNC_AND_VOTING.md` for what the network and Firestore each hold for a group, the voting rules and the gaps that remain.

## 5 Oct 2026: fixed goals no longer say the money was sent when it was not

What was wrong. A fixed goal whose rule is "admin asks, the group votes" keeps the money in the group wallet after the goal is reached; nothing is paid until the admin requests a payout and the members vote. The app said the network had already sent the pot to the admin's wallet, hid the Request payout action once the goal was reached, and showed "Collected 0" because it read the Firestore payments list, which never holds deposits made on the network. Checked against the live node for group `FIXED-acbfe8c30807d5a4`: state ACTIVE, 40 of 40 raised, 40 HARV still in the group wallet, 0 paid out.

What the app does now. The wording follows what happened: "held by the group wallet, not paid out, the admin asks and members vote" (admin rule), "the network is sending" (all-at-once rule, until it confirms), "was sent to" only when the network marks the group completed. The admin keeps the Request payout, milestone and grant actions while money is left. Collected on a fixed goal is read from the network.

What did not change. The node does not need a new jar. A goal still closes to deposits once it is reached, and a group finishes only when the money is paid out.

What you do. Install an app build from this date or later. For the group above, the admin opens its Payments tab and uses Request payout; the other member then votes.

## 5 Oct 2026: friends list, pinch, and referrals by username

What the app does. The Friends screen shows each person with their photo, name, @username and trust score, with Follow / Unfollow and a Pinch button. A pinch is a nudge: the friend gets a notification "(name) pinched you. Go check what they want." One pinch per friend per hour, and only to someone you follow. Your username is your referral code: it is shown at the top of Friends with a Share button, and a new member can type a username under "Referred by (optional)" when creating their account. The person named sees the request under Friends, Referrals, and confirms it with their PIN only if they know the person. When both have agreed the referral is recorded on the network.

Why. Whoever refers someone answers for them. For each person you referred who later defaults in a group, your trust percentage drops by 10 points, up to 30. A default that is cured or forgiven stops costing you. This is worked out by the node and shown on your profile like the rest of the score.

What you do, in this order:

1. Publish the Firestore rules first. Open `docs/firestore.console.rules`, select all, paste it over the Firebase console editor and publish. It adds the rules for pinches (`PINCHES`), referral requests (`REFERRAL_ASKS`) and the "who invited me" fields on a profile (set once, cannot be changed). Do not copy rules out of a chat message.
2. Deploy the node. The referral record now needs the referrer's signature as well (`docs/TRUST_SCORE.md`, "The referrer must agree"), and the referrer's score is refreshed when a person they referred defaults. Use the steps in the node repository's `docs/DEPLOYMENT.md` (build, swap the jar, restart `harvanna`). An app build older than this one cannot record referrals; none did before.
3. Install an app build from this date or later.

What did not change. Following, followers and contact links work as before. Pinches use the phone's existing notification check (about every 15 minutes while the app is closed), not a push service, so a pinch can arrive a few minutes late.

## 5 Oct 2026: confirm your email before signing in; trust score screen rebuilt and shown everywhere

What the app does. An account made with an email and password cannot get into the app until the email is confirmed. Creating an account, signing in with an unconfirmed email, or opening the app with one all land on a "Confirm your email" screen with Resend (once a minute), "I have confirmed" and "Use a different account". Google sign-in is not affected. Forgot password now shows a tap-friendly link and a clear "Check your email" message. The Trust score screen is rebuilt around the node's percentage: the number and tier, how far the next tier is, the five parts with what raises each, and the Learn Hub activity. The percentage now appears on the Home card, the profile, P2P listings and trades, group members, group cards (the group's own percentage), join and payout requests, the people lists and Learn Hub posts. The older on-device score is no longer shown anywhere.

What did not change. The node does not need a new jar. Firestore rules do not change for this.

What you do:

1. Install an app build from this date or later.
2. In the Firebase console, Authentication, Templates, check that "Email address verification" and "Password reset" are enabled and the sender is set. If the reset or confirmation email never arrives, check Authentication, Settings, Authorized domains and the spam folder. The app can only ask Firebase to send them.
3. Accounts that signed up with a password before this build and never confirmed will be asked to confirm on their next open. That is intended.

What this does not do. The check is in the app. A person using a modified app could skip it. To enforce it on the server too, a later change can require `request.auth.token.email_verified == true` for password accounts in `signedIn()` in the Firestore rules; that locks out every unconfirmed account at once, so it is not done here.

## 5 Oct 2026: trust percentage, Learn Hub reads and gifts, group trust

What the app does. A profile and a Learn Hub post show one trust score as a percentage with a tier (Bronze, Silver, Gold, Platinum), read from Firestore. The node computes it and writes it to the person's public card (`PROFILES/{pid}`), so nobody can edit or hide it. Learn Hub posts are social cards: fire, comments, share, gift, a reader that records a verified read once the post is read to the end, and a My Activity screen (30-day bars, popular days, peak hours). A group page shows the group's trust percentage and its five parts. The dynamic-animation library was added so the group list screen no longer crashes when it draws its progress bar. The root Gradle files were restored after CodeAssist removed them, so GitHub Actions can build again.

What did not change. PIN rules, staking, and the 24-hour claim rule.

What you do, in this order:

1. Publish the rules first. Open `docs/firestore.console.rules` in this repository, select all, paste it over the Firebase console editor and publish. Do not copy rules out of a chat message: a chat copy inserts `\` before `$(...)` and the console rejects it. That file now holds the `PIDS` and `PROFILES` rules (the node-only trust fields are read-only for the owner) and the Learn Hub reaction and comment rules. Without them the new app cannot save a profile card, react or comment.
2. The node is already on the matching jar (live since 5 Oct 2026, see the node repository's `docs/DEPLOYMENT.md`). Do not restart it for this change.
3. Install an app build from this date or later. Older builds keep working and show the old 0 to 26 score.
4. Clear the GitHub Actions artifact storage (Settings, Actions, Artifacts) so the APK upload step passes. The Gradle build itself passes.
5. Check one profile: open My Activity, read a post to the end, wait for the next node publish and look for the percentage on the profile.

What the app cannot do yet. Several P2P screens still read the older on-device score, and not every other-user profile screen shows the badge. Read receipts are effort evidence, not proof of a person, so engagement is 15% of the score and counts as zero when the reading pattern looks automated.

## 2 Oct 2026 — notices no longer repeat

What the app does. A notice is shown once. The tab already says Notice, so the page does not say it again. A card does not print the same sentence as both the title and the message. A contribution used to write two notices; it now writes one. Copies already saved (the same title and message, or two contribution notices a few minutes apart) are collapsed when the list opens.

What did not change. The node does not need a new jar.

What you do:

1. Install an app build from this commit.
2. Do not restart the live node for this change.

## 2 Oct 2026 — receipt layout for a payment

What the app does. The review before a payment, and the transaction screen after it, now read like a receipt. The amount is large. Under it is what the payment is for, then the date. Transaction details list the status, who it is from, who it is to, and the fee. See more opens the reference, block, and nonce. A timeline shows review, verify, and sent. Verify and continue is what asks for the PIN. On a finished transfer, Share transaction receipt is the button at the bottom.

What did not change. The PIN is still required. The node does not need a new jar.

What you do:

1. Install an app build from this commit.
2. Do not restart the live node for this change.

## 2 Oct 2026 — payment review sheet, then the PIN

What the app does. A payment button no longer opens the PIN by itself. Stake, unstake, claim, send, a group contribution, a deposit, joining, a loan disbursement or repayment, a group-wallet payout, creating a group, and P2P open, release, deposit, listing, and cancel first open a review sheet. The sheet states the amount, what the payment is for, who receives it, where it comes from, and the fee. Verify on that sheet is what asks for the PIN or fingerprint. Cancel on the sheet sends nothing.

What did not change. The PIN is still required, and a phone with no PIN is still sent to Settings. The node does not need a new jar.

What you do:

1. Install an app build from this commit. A build from `7fd6a51` asked for the PIN after a thinner confirm. This build is the receipt the person reads first.
2. Do not restart the live node for this change.

## 1 Oct 2026 — review the payment, then the PIN

What the app does. A payment no longer jumps from the button to the PIN. Stake, unstake, claim, send, a group contribution or deposit, joining a group, a loan disbursement or repayment, a group-wallet payout, creating a group, and P2P open, release, deposit, and listing first show a summary sheet: the amount, who it is for, and the fee. Continue is what asks for the PIN or fingerprint. Cancel on the sheet sends nothing. If no PIN is set, the action still stops and offers Settings.

What did not change. Group creation no longer generates a wallet or a 12-word phrase on the phone. The network assigns the group's wallet (the escrow address) when the group is created, and that address is what the app saves. The old "new group wallet seed" step is gone. Trust and activity are unchanged. The node does not need a new jar.

What you do:

1. Install an app build from this commit.
2. Set a transaction PIN in Settings before the first payment or stake.
3. Do not restart the live node for this change.

## 1 Oct 2026 — payment and staking require a PIN


What the app does. Stake, unstake, claim, send, a group contribution, a loan repayment or disbursement, a group-wallet transfer, creating a group (the fee is taken from the wallet), and P2P claim, release, deposit, and listing no longer go straight through. The screen states the action, then asks for the transaction PIN or fingerprint. If no PIN has been set, the action stops and offers Settings so a PIN can be created. Cancelling sends nothing. Showing the recovery phrase, restoring a wallet, and transferring admin still use the old optional check.

What did not change. The trust number stays 0–26. Activity rows are unchanged. The node does not need a new jar.

What you do:

1. Install an app build from this commit. An older build still lets stake, unstake, and claim through with no check, and still skips the check on other payments when no PIN is set.
2. Set a transaction PIN in Settings before the first payment or stake. Fingerprint works only after that PIN exists and biometric unlock is turned on.
3. Do not restart the live node for this change.

## 1 Oct 2026 — show the node's trust number, not a percent

What the app does. A person's score is the wallet figure the node saved, from 0 to 26 (10 if the device is bound, plus 2 for each active vouch, up to 8). A group's line is the node tier: New circle, Established, or High trust. Home, profile, Discover, your groups, a member sheet, and the P2P screens read that copy in Firestore and fall back to the node. They do not show the old 0–100 profile percent, and profile no longer writes `trust_score` back to `USERS`.

What did not change. Insurance still requires a verified profile, zero reports, and the deposit. Those checks are not the number on screen. The node does not need a new jar for this.

What you do:

1. Install an app build from this commit. A build from `5ce4531` still reads the copies, but several screens still paint the old percent until this build.
2. Do not restart the live node for this change.
3. A wallet or group with no copy yet shows "—" or "Not on the network" until the node publishes one. Opening the group, or a trust event on the wallet, writes the copy. Staking info still refreshes about once a minute.

## 1 Oct 2026 — read trust, groups and staking from the Firestore copy

Commit `5ce4531` on `main`.

What the app does. Group, roster and staking screens read the copy the node wrote. If that document is missing, or Firestore refuses the read, the same call uses the node. No setting changes when the Firebase plan changes. A signed unstake or claim still reads the live action nonce from the node.

What you do:

1. Do not copy rules out of a chat message. A chat copy inserts `\` in front of `$(...)`, and the Firebase console rejects that. Open `docs/firestore.console.rules` in this repository, select all, and paste that over the Firebase console editor. Publish. This works on the free plan. That file is the console rules you pasted on 1 Oct 2026, plus the seven node collections.
2. `docs/firestore.rules` also allows those reads, but its `USERS` rules are stricter than the console. Do not publish that file over the console until the two have been compared.
3. Install an app build from `5ce4531` or later.
4. The copies stay empty until the node is restarted from its own repository, using the steps in that repository's `docs/DEPLOYMENT.md` ("Putting a new jar on the live node"). Those steps keep the live config. Do not rebuild the jar from a fresh git checkout and start it on its own: the git properties file is placeholders, and that would blank the guardian keys and the fee wallet. This app update does not restart the node.

The owner published `docs/firestore.console.rules` in the Firebase console on 1 Oct 2026. The node is writing. An installed build from `5ce4531` or later reads the copies and falls back to the node when a copy is missing. An older installed build still calls the node for every screen.

## 5 Oct 2026 — P2P forms, sheets and toggles
- Add/Edit payment method: the four text fields were locked (`focusable=false`) and could not be typed in; fixed. "Set as default" now toggles from the whole row, not only the tiny checkbox. The sheet opens expanded and resizes above the keyboard.
- All P2P bottom sheets use the shared themed sheet (day/night correct). Filter/payment toggle rows are tappable across the full row.
- Hardcoded colours in P2P trade, chat and offer screens replaced by theme colours.

## 5 Oct 2026 — Named node badges
- New `trust/NodeBadges` reads the node's real badges (Circle veteran, Founding member, Zero default, Early payer, Vouched) from `/wallet/labels/{address}`, plus Insured trader from `/p2p/insurance-status`. `trust/BadgeChips` draws them as chips; tap a chip to read what it means.
- Shown on your profile (replacing the bare count) and under people on the Friends list and the friend picker. Badges are decoration only and are never stored by the app.
- Group trust before joining was already on Discover cards; group member rows do not show badges yet.

## 5 Oct 2026 — Docs brought in line
- `NODE_RECONCILIATION.md` now carries a status note: the group clients, referral consent, node trust percentages and badges exist, so the "no client for group actions" lines are historical.
- Node docs now write the brand as Harvana. The chain id, network id, signing strings, domain and package are unchanged. The Java class renames in the node are not done yet and wait for the owner's go-ahead.
- Node classes now read `HarvanaProtocolManager` and `HarvanaChainNodeApplication`; the node serves the coin name "Harvana". Comments and docs here follow. The staking dashboard layout still says "Harvanna" because staking is being changed separately.
