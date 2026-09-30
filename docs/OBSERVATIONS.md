# Observations (out of scope / open)

## Done in this pass (Tier 0 / Tier 1 constants)
- T0-1: node base URL and WS base -> `https://harvannaprotocol.com` / `wss://harvannaprotocol.com`.
  Removed the self-signed IP pin from `network_security_config.xml` and deleted `harvanna_node_cert.pem`
  (the live host uses a CA cert). No stored-URL migration exists in this app (URL is a constant).
- T0-2: `AJ0x` -> `HV0x` everywhere (constants, regex validators, layouts hints).
  Left unchanged: `CreateNewGroupActivity.FEE_COLLECTOR_ADDRESS` (unused legacy hex; real HV0x address unknown).
- T0-3: read from node source (read-only): `AuthChallengeStore.canonicalMessage` =
  `HARVANNA_AUTH|address|challenge|clientId|app`. The app signs the node-supplied `canonical`;
  the local fallback now matches that format.
- T1-1/2/3: `ChainConstants` reward/halving/fee split corrected; hardcoded gas constants removed (were unused).
- T0-4: untouched, as instructed.

## Open, needs owner decisions (not guessed)
- T0-5: no `google-services.json` / plugin in repo.
- T2-1 keystore, T2-2 versionCode, T2-3 minify not changed.
- Existing wallets holding `AJ0x` addresses: HV0x is derived from the same key, so addresses
  should be re-derived from stored keys; stored `AJ0x` strings in prefs/Firestore are not migrated here.
- Tiers 3-5 need device/CodeAssist testing and a dedicated reconciliation pass.

## Email/password auth
- Added `EmailAuthFragment` (sign in, create account, forgot password, verification email on signup) using
  Firebase Auth; `LoginActivity` routes `eLogin`/`eCreate` (and `pLogin`, which falls back to email) to it.
  On success it hands off to `SplashScreenActivity`, which routes to Main or the profile form.
- Settings "Log Out" previously never called `FirebaseAuth.signOut()`; it now does, and returns to Splash.
- Phone/OTP login is still a stub (hardcoded test code) - not fixed here.
- Requires Firebase console: enable Email/Password provider, and T0-5 (`google-services.json`) still open;
  auth cannot work at runtime without it.
- Not compiled (no Android SDK here); needs a build check.

## Phone auth abandoned
- Phone/OTP sign-in was removed (`PhoneAuthFragment` deleted; the splash phone button was already hidden in the
  layout). Supported methods: email/password and Google. Both end at `SplashScreenActivity`, which routes existing
  profiles to MainActivity and new users to the complete-profile form.
- Complete-profile form requires a real signed-in Firebase uid (random-UUID fallback removed), prefills and locks
  the auth email, and prefills the name from the display name. Phone number is entered manually there.
- Email and Google sign-ins create separate Firebase accounts; account linking is not implemented.
- Firebase console: enable Email/Password and Google providers, register SHA fingerprints; T0-5 still open.

## Firebase setup (T0-5)
- Package renamed `com.harvanna.app` -> `com.harvanna.app` (applicationId, namespace, sources, manifest authorities). Register `com.harvanna.app` in Firebase.
- `com.harvanna.node.miner` (separate miner app) is still referenced by WalletEarnFragmentActivity; update if the miner is renamed.
- `com.google.gms.google-services` plugin (4.4.1) added to `app/build.gradle`. The build fails until
  `app/google-services.json` is added (download from Firebase console after registering the app + SHA fingerprints).
- App has never been on Play, so `versionCode 1` is fine (T2-2 resolved). Keystore (T2-1) still open.
- Testing is deferred to the final phase, done manually by the owner.

## Status log (29 Sep 2026, autonomous pass)

### Done (verification level noted)
- AJ0x -> HV0x migration (`wallet/AddressMigration`, called from `SketchApplication`): re-derives the address from
  stored key material, copies + verifies keys under the new address, repoints prefs / wallet list / cache, then
  removes old entries. Compiled by CI only; NOT tested on a device.
- `minifyEnabled true` + explicit R8 rules (blanket `-ignorewarnings/-dontwarn` removed). CI assembles release.
  A green build does NOT prove nothing reflection-dependent broke at runtime; needs real device testing before any
  release build is trusted.
- Tier 4, manager 1 of 5: `UserProfileManager` now uses the native Firestore SDK; persistence enabled explicitly in
  `SketchApplication`. Not yet verified against real data or with a real network drop.
- Tier 5: WS auth header fixed; P2P requests now use `nextRequestNonce`.

### What CI does and does not prove
Proves: Java compiles, Gradle build completes, resources resolve (and, for release, R8 completes).
Does not prove: CodeAssist compatibility (Tier 3, needs an on-device attempt), Google Sign-In completing, R8 not
breaking reflection at runtime, Firestore offline persistence syncing after a real network drop.

### Blocked / open - not decided on your behalf
- T2-1 release keystore existence (needs owner answer).
- T2-2 Play upload history: owner said the app is not on Play; treated as resolved (versionCode 1).
- `CreateNewGroupActivity.FEE_COLLECTOR_ADDRESS` (`AJ0x...`, unused): needs owner to say what it should point to.
- **Firestore security rules on the live project (HIGH severity).** The app has no access to the Firebase console
  from this container, so the deployed rules are unverified. Every profile save/read, group and P2P Firestore call
  depends on them. If rules are locked down (or not matching the `USERS/{uid}` per-user access these managers
  assume), sign-up will fail at the profile-save step for every new user; if they are wide open, user data
  (KYC-style fields: DOB, ID numbers, next of kin) is exposed. Check the Rules tab before real sign-ups.
  Note the node repo has a `firestore.rules` file; not compared against the deployed rules.
- Google Sign-In SHA-1: needs a real build's signing fingerprint, which needs CodeAssist/Android Studio access this
  container doesn't have (CI's debug APK is signed with a throwaway runner key, not the key you will install with,
  unless you install the CI-built APK itself - then its fingerprint could be read from that APK).
- P2P listing creation: node now signs maxOrder/paymentWindow/unitPrice; app UI has none of these (owner decision).
- On-chain group/staking/referral/identity-link actions: not implemented in the app (scope decision).

## Update: splash / "cannot log in" diagnosis and rebrand (29 Sep 2026)
- Root cause found statically (device not available): `splash_screen.xml` had the **Email Login and Create Account
  cards `visibility="gone"` and their buttons `enabled="false"`**, so only the Google button was usable. Google
  sign-in cannot succeed until a SHA-1 is registered; that failure returns RESULT_CANCELED with no message and no
  network traffic - i.e. "nothing happens" and 0 B data usage, exactly as reported. Fixed: email buttons are
  visible and enabled; Google failures now show a message (code 10 = SHA-1 not registered); a 12 s spinner safety net.
  This is inferred from the code and screenshots, not reproduced on a device. An older installed build (pre-fix
  node URL / no google-services.json) could also contribute; reinstall from the latest build.
- Palette: Neon Orange #FF9030 + Charcoal #111827, light and dark (values/ and values-night/), drawable hardcoded
  greens/navys moved to tokens. Text on orange is charcoal (white on #FF9030 is only ~2.2:1). Visual result not
  reviewed on a device.
- App renamed "Harvanna Wallet"; layout text rebranded (Harvanna -> Harvanna, HARV/Harvannacoin -> HARV). Java string
  literals still say "HARV"/"Harvanna" in places (not changed; user-visible follow-up). The launcher icon is a generated
  placeholder (orange square, charcoal H).
- "Home activity" UX/UI redesign, and per-group-type screens: NOT done yet. See docs/GROUP_ENGINE_MAP.md.
- R8: first release build ran out of memory (512 MB default heap) - raised via gradle.properties. CI evidence:
  run 9 (commit 3833e05) passed the full pipeline: 58 canonical unit tests, debug build, and minified release build
  (R8) with the Firestore migration, rebrand/palette, splash fixes and AJ0x migration included. Later commits
  (group client, quick actions, palette-everywhere) are verified by their own runs; see the Actions tab.

## P2P seller screen (owner decisions of 29 Sep 2026) - what was done and where the node differs
Done: the listing is now signed and sent with `maxOrderAmount`, `paymentWindowMinutes` and `unitPricePerHarvRaw`
(kobo per HARV) using the wallet's `nextRequestNonce`; the canonical string is a pure builder checked against the node's
own class (2 golden cases). Seller sees a market reference and a non-blocking flag when their price is >15% away.
Max order: must be >= min(500 HARV, total), between min and total.

Differences from the decisions as stated (found while reading the node; NOT changed on the node):
1. **Payment window**: the node accepts 15-60 minutes (default 45) and REJECTS anything above 60 (`INVALID_PAYMENT_WINDOW`).
   `MINIMUM_P2P_ESCROW_TIME_MS` does not exist anywhere in the node repo. 1h/2h/4h presets would be rejected for 2h and 4h.
   The app keeps the existing 15/30/45/60 presets (which the node's own comment says match the UI). If a 1h minimum is really
   wanted, that is a node change (owner's decision) and the presets change with it.
2. **The mark is not reachable from the app**: `HarvannaProtocolManager.getMarkStatus()` exists but no controller exposes it.
   So the screen shows the MEDIAN of open-listing prices as a clearly-labelled reference, not the accepted mark. Needs a node
   endpoint (e.g. `GET /api/v1/mark`) - node-side follow-up. When it exists, swap it into `P2pPriceReference.fetch`.
3. **Currency**: the node prices in kobo (Naira). Non-NGN offers are refused in the app with a clear message.
4. **Margin pricing** is resolved to a fixed signed price against the same reference; if no reference exists it asks for a fixed price.
5. Whether `/listings/open` includes `unitPricePerHarvRaw` per listing was not verified against a running node; if it is
   redacted the reference shows "No market reference yet".

## Package renamed again (29 Sep 2026, urgent): com.harvanna.wallet -> com.harvanna.app
All sources, tests, manifest authorities, Gradle `applicationId`/`namespace` and docs updated (history: com.harvanna.app -> com.harvanna.wallet -> com.harvanna.app).
**Firebase action required:** `app/google-services.json` was registered for `com.harvanna.wallet`. I only edited its `package_name` field so
the Gradle plugin accepts the build; the `mobilesdk_app_id` and OAuth entries still belong to the OLD Firebase app. Register a new Android app
`com.harvanna.app` in the Firebase console (with SHA-1/SHA-256 from README/this file's debug key), download the new `google-services.json`, and
replace this one. Until then email/password may work but Google sign-in will not.

## App name and Firebase file (29 Sep 2026)
- App name is now just "Harvanna" (launcher label, main header, about screen).
- `app/google-services.json` replaced with the owner's file that has a client for `com.harvanna.app`
  (`1:92873857858:android:7edea0b3ad5cf38d65c6db`). It has NO Android OAuth client (`client_type: 1`) for `com.harvanna.app` yet:
  the debug SHA-1 (bff2f88297ff53dd1f21bca9e5a36e49011fc55a) was registered on the OLD `com.harvanna.wallet` app entry.
  Register SHA-1 + SHA-256 on the `com.harvanna.app` entry, re-download, replace this file. Google sign-in stays broken until then.
  The old `com.harvanna.wallet` entry can be deleted in the console afterwards.

## Firebase file finalised (29 Sep 2026)
`app/google-services.json` now contains only `com.harvanna.app` (`1:92873857858:android:7edea0b3ad5cf38d65c6db`) with the Android OAuth client
for the debug SHA-1 (bff2f88297ff53dd1f21bca9e5a36e49011fc55a) and the web client used by `requestIdToken`. This is the config Google sign-in needs.
Whether the SHA-256 is also registered is not visible in this file. Still to verify on a device: Google sign-in, and that Email/Password + Google
providers are enabled in Authentication.

## Coin ticker and name from the node (29 Sep 2026)
- New `CoinInfo` (ticker/name), loaded from cache at startup and refreshed from `GET /api/v1/chain/info` (`ticker`). Defaults: HARV / Harvanna.
  The node publishes NO coin name and NO decimals; `CoinInfo` will pick up `coinName`/`name` if the node adds it. Node-side suggestion:
  add `coinName` and `decimals` to `/chain/info`.
- 194 user-visible string literals in 51 Java files no longer hardcode HARV/Harvanna/Harvannacoin/HarvannaChain: they use `CoinInfo.ticker()`/`name()` or "Harvanna" / "Harvanna Protocol".
  Left on purpose: class/identifier names, log tags, file-system folder names (`Harvanna`, `Harvanna/media/documents`, so nothing saved by older code is orphaned),
  and "Harvanna Node Miner" (a separate app with its own package/name).
- Static text in XML layouts says HARV (set earlier); it is not fetched dynamically. If the node ever changes the ticker, Java-built text follows, layout text does not.
- **Found, not fixed:** `HomeFragmentActivity` and `GroupPaymentFragmentActivity` use a hardcoded `HARV_TO_NGN = 150.0` to show a Naira value. That is a made-up
  rate, so the "NGN" totals (e.g. the asset screen) are not real. The node's mark is the right source but no endpoint exposes it yet; until then these should show no
  Naira value (or the open-listing median labelled as approximate). Needs an owner decision.
- **Found, not fixed:** the transaction detail screen shows an "Amount Burned" row; the node's fee model (87% miner / 3% good-behaviour / 10% staking) has no burn.

## The old brand and ticker were removed everywhere (29 Sep 2026)
Every class, file, resource id/style/layout, string, log tag, preference/database/keystore name, comment, doc, CI artifact name and the Gradle project
name now use Harvanna / HARV. The two legal documents in `assets` (Privacy Policy, Terms of Service) were renamed and their text rewritten - **the owner
or counsel should review them**, since a mechanical brand swap changes legal wording. The countries data file was deliberately left alone: it contains the letters
"ajc" inside real place names.

Consequences to know:
- **Local storage names changed** (preferences files, keystore alias, offline database, etc.). A device that already has the previous build installed keeps its old
  data on disk but the new build cannot see it: the wallet must be restored from its recovery phrase / Access Card. Do not uninstall or clear data on a device holding
  a wallet you have not backed up.
- Mechanical name results that need the owner to confirm they exist: the support address is now `support@harvanna.app`; the separate miner app is now expected at package
  `com.harvanna.node.miner` (the Earn screen launches it); saved-file folder is `Harvanna`.
- The repository is still called "Ajoo" on GitHub and git history contains the old names; neither is part of the app.

## Group wallet card sharing
- The encrypted Access Card (ciphertext + digit code + address, never the passphrase) is published to `GROUPS/{gid}/private/walletCard` after group creation (or when the admin regenerates it from Group settings). Members view/print/save it in `GroupWalletBackupActivity`.
- Needs the `private` rule in `docs/firestore.rules` deployed; until then publish 403s (kept locally and retried when the admin opens the backup screen) and members see an error.
- Security note: members hold ciphertext, so the passphrase strength is the only protection (min 20 chars enforced by the card crypto). Admin should use a long random passphrase and never share it.
- Node-derived on-chain Circle escrow has no private key, so no card exists for it; only personal-wallet backup applies.
- Not implemented: per-type settings for GenLoan; a fuller rebuild of the settings screen (existing type panels for Rotating/Fixed/Accumulative kept, backup section added).

## Owner decisions applied (30 Sep)
- Removed the fake HARV_TO_NGN=150 rate everywhere; the group wallet's "≈ ₦" line is hidden (no real rate source).
- Access Card passphrase minimum is now 24 characters (new cards only; existing cards still decrypt).
- Loans: the node has loan machinery for Accumulative and Fixed groups only (Circle/Rotating has none), so the loan toggle exists only for those two types; Rotating is always loans-off.
- Miner package: com.harvanna.mine (+ manifest <queries> so Android 11+ can detect it).
- Full Firestore rules replacement: docs/firestore.rules. Note the rebrand renamed data fields (amountAjc->amountHarv, repaidAmountAjc->repaidAmountHarv); the rules use the new names, so any data written by an older build under old names would not match.

## Trust score and wallet ownership on the node
- Node has two mechanisms: `POST /api/v1/identity/link` (wallet signature + Firebase ID token; node stores only HMAC(pepper, uid); disabled unless `harvanna.identity.links-enabled=true`) and `POST /api/v1/personhood/bind` + `/vouch`, `GET /api/v1/personhood/{address}`.
- The node's trust score is only 0..26: bound = 10, +2 per effective vouch up to 8. It is NOT the app's 0..100 score (TrustScoreEngine, computed client-side from Firestore). They are different measures; the profile now shows both, labelled separately. There is no node endpoint that takes the app score.
- **One wallet per account and one wallet per device hash**: the node rejects a second wallet for the same user (`USER_ALREADY_LINKED`) and a second wallet on the same device (`DEVICE_ALREADY_BOUND`). "Various wallets per user" is therefore not possible without a node change; the app links the active wallet only and shows the node's message otherwise.
- The device attestation hash is client-asserted (SHA-256 of ANDROID_ID + package); real attestation (Play Integrity) is future node work. Node deploy must set `harvanna.identity.links-enabled=true` and a real pepper.

## Invites
- Invites live in `INVITES/{groupId}_{emailKey}` (email-keyed so people without an account can be invited). Rules are in docs/firestore.rules and must be deployed. Invitees must have a VERIFIED email to see or answer invites (otherwise anyone could register a victim's email and read their invites).
- The invitee is notified when the app is opened (Android notification + a Snackbar on Home). There is NO background push: that needs FCM plus a Cloud Function, which this project doesn't have.
- The invite screen summarises the group and links to the join screen, which shows the rules, requires a wallet and a signed contract.

## Group chat connector and presence
- Chat is external by design: the admin saves an https WhatsApp/Telegram/Signal/Discord/Facebook link (validated by ChatLink) in `GROUPS/{gid}/private/chatLink` (members-only read via the existing private rule). The toolbar Chat icon opens it after a safety warning.
- Presence bug found: `markSelfActiveNow` had no callers, so nobody ever appeared online. `PresenceHeartbeat` now writes `lastActiveAt` every 2 min while the app is in the foreground (respecting the privacy switch, which also writes `showActivityStatus=false`). Member avatars get a ring: green online (<5 min), amber away (<1 h), thin grey offline. Presence is self-reported, so it is approximate.
- Profiles are readable by any signed-in user (open item A2), so presence is too.

## Notification center
- `NotificationsActivity` + `NotificationCenter` (per-user inbox, 200 cap, de-duplicated) is the single place for notifications. Sources: incoming transfers (WalletController), group notices addressed to you or posted by others, payment reminders targeted at you, payment-due (Home), invitations, join-request answers. Each also raises an Android notification if the user's Notification settings allow that category.
- Bugs fixed: the toolbar bell (menu id 1) had no handler; MainActivity's receiver read `amount`/`sender` extras that the broadcaster never sets (`EXTRA_AMOUNT`/`EXTRA_SENDER`), so it always showed 0.00000.
- Delivery is by polling (every ~90 s in the foreground, every ~15 min in the background via WorkManager, only while online). True instant push needs FCM + a Cloud Function or the node pushing; not built. Android may delay background work on battery saver / restricted phones.
- Not yet connected: loan-due reminders and admin-side alerts (new join request, loan application) - the category switches exist; sources still to be added.

## Rotating group = one group (merged on-chain + Firestore)
- The separate "Rotating circle" entry is gone. Picking Rotating uses the normal create form; on confirm the app creates the circle on the node (creator bond 2x + 1 HARV fee, disclosed first) and saves the Firestore group with `circleId` and `onChain=true`. Firestore holds the human data (name, description, location, visibility, joining, invites, notices, chat, trust); the node holds the money, bonds, round and payout order.
- The off-chain-only rotating controls (ballot / first-come / admin-assigned / fixed-order / auction, commission, grace period, default consequence, minimum trust) are hidden: the node decides payout order (join order with early slots for track-record members) and covers missed payments from bonds. Frequency is limited to Daily/Weekly/Monthly (node cycle 24-720 h), members 3-12.
- Group wallet + Access Card are skipped for on-chain groups (the escrow has no key). Settings hides Restore/Backup for them. The Payments tab shows a card that opens the circle screen (join + bond, contribute, order, bond withdrawal). After a Firestore join is approved, the member is sent to lock the bond.
- Still separate: Firestore membership and on-chain roster can differ (a member may be approved but not yet have locked a bond). Circle members' joins are not admin-approved on the node. Old rotating groups created before this change keep their group-wallet flow.

## Node integration guide (30 Sep) - reconciliation
- Nonces now come from `GET /api/v1/wallet/nonce/{address}` (`nextRequestNonce` for action requests, `nextNonce` for locks), as the guide says.
- **v2 member locks are now supported** (`chain/LockV2`, `sync/LockSigner`, hooked into every signed POST): when the node returns `locksToSign` the app signs and submits each template to `/api/v1/locks/submit` (chain nonce = nextNonce, +1 per template, expiry now+1h, memo/type/amount/maxFee verbatim). Without `locksToSign` behaviour is unchanged. The byte layout is unit-tested against the node's own encoder. `CircleActivity.onResume` also picks up templates left pending after a lost response. Not yet shown: an explicit "you pay amount + fee" confirmation for the lock (the earlier disclosure dialogs already show the amounts); to add before the activation height is set.
- **Open node issue (guide 2.1, OBSERVED_NOT_FIXED #18):** the circle escrow appears to be short by (contribution x 1) per round (each round collects members-1 contributions but pays contribution x members). Do not deploy Rotating groups with real balances until the owner confirms the intended payout. The create screen currently tells users "the pot each round is contribution x members", which matches the node's payout target.
- **Unit of account is not built**; group creation messages will change (kobo amounts, bootstrap mark) and ship with a reset. Do not treat the current creation fields as long-term.
- Guide 6.6 items not yet in the UI: group trust tier and member labels before joining; vote weights/quorum on the vote screens; `escrowTxHash` for P2P in v2 mode should be tracked by the hash returned from `/locks/submit`.

## Owner decision: circle payout = Option A (30 Sep)
The recipient receives contribution x (members - 1), the amount actually collected that round (the recipient does not pay in their own round). All app text was changed to match (create confirmation, welcome contract example and summary, circle terms row). **The node still pays contribution x members** (per its guide, entry #18): the node team must change the payout target to contribution x (members - 1) (and check the completion/entitlement figures) before real balances. I did not touch the node repo.

## Accumulative and Fixed now created on the network (stage 2)
- Create form: Accumulative calls `/accum/create-group` (interest = the loan interest %, withdrawal rule mapped to ANYTIME / CYCLE_END_ONLY, 1 HARV fee). Fixed calls `/fixed/create-group` (goal, rule, beneficiary, 1 HARV fee); the rule list gained Milestones and Emergency Grants; a receiver-address field was added (empty = your wallet; not needed for Emergency Grants). Confirmation dialogs state the cost and the irreversible terms first.
- The Firestore group stores `nodeGroupId` (and `circleId` for circles) + `onChain=true`. `GroupKinds.nodeId` is the single way to read the link.
- The node has no member cap, contribution schedule or slots for these families; "members" and "contribution" on the form are display/app-level only. Admission approval is disabled for all new groups (the node has none).
- Old Firestore-only Accumulative/Fixed groups still work through the old screens (only old rotating groups are hidden).
- Still on Firestore: loans tab and payout requests for Accumulative/Fixed; Home totals, Trust score and Open loans.

## Totals and trust from the node, merged with Firestore (stage 3)
- `sync/NodePortfolio` splits the user's groups into on-node groups (read from the node: your member record, and your line for Accumulative) and older Firestore-only ones. `engine/NodePositions` (unit-tested) turns node records into saved amount, borrowed/repaid, payments expected vs made, late/default gaps and completion.
- Home: Total saved and Open loans = Firestore totals (older groups) + node totals. The trust ring now shows the computed score (TrustScoreEngine), not the stored profile number. TrustScoreEngine merges node positions into payment history, consistency, loyalty, tenure and savings. Trust Score screen lists a "On the network" line per group. Open Loans lists node debts (borrowed minus repaid from your member record) with a button into the group.
- Gaps: the node has no call to list a member's loans, so node loans show as an outstanding total per group (not per loan), and per-loan repay needs the loan id (the loan API reads by id only). Circle "saved" counts the locked bond only. Accumulative "expected payments" assumes a daily schedule up to 30 days. A node loans-list endpoint should be added to the request to the node team.

## Retry for groups missing their Firestore record
A group created on the node whose Firestore save fails is queued on the phone (`group/GroupSaver`, SharedPreferences `pending_group_saves`). A background job (`group/GroupSaveWorker`, WorkManager, network required, exponential backoff from 30 s) retries automatically as soon as a connection exists, and the app also retries on every open. Every step uses fixed ids and PATCH writes, so retrying is safe. Node data is never touched by the retry.
