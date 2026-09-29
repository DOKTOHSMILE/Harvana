# Observations (out of scope / open)

## Done in this pass (Tier 0 / Tier 1 constants)
- T0-1: node base URL and WS base -> `https://harvannaprotocol.com` / `wss://harvannaprotocol.com`.
  Removed the self-signed IP pin from `network_security_config.xml` and deleted `ajoo_node_cert.pem`
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
- Package renamed `com.ajoo.app` -> `com.harvanna.wallet` (applicationId, namespace, sources, manifest authorities). Register `com.harvanna.wallet` in Firebase.
- `com.ajoo.node.miner` (separate miner app) is still referenced by WalletEarnFragmentActivity; update if the miner is renamed.
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
