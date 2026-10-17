# Release build checklist (operator pre-flight)

Use this on the build host **before** pushing a release build to the Play
Console / Firebase App Distribution / sideload channel. The Harvana app is at
the project root. Every step is either a build-host command, a Firebase console
click, or a one-time test on a real device.

> Convention: every step that is **must-do before this is "production"** is
> marked **[block]**. Steps marked **[verify]** are sanity checks the build
> host can do without shipping.

---

## A. Pre-build host checks (5 min)

- [ ] **[verify]** You are on a clean working tree on the `main` branch of the
  Harvana repo.
- [ ] **[verify]** `git log -1` shows a commit that mentions
  `Security & production-readiness pass: 7 fixes` (the 18 Oct 2026 batch).
- [ ] **[verify]** `grep -c 'allow.*if true' docs/firestore.rules` returns `0`.
- [ ] **[verify]** `grep -c 'bottomsheet' app/proguard-rules.pro` returns
  at least `2`.
- [ ] **[verify]** `grep 'bcprov-jdk18on' app/build.gradle` shows
  `:1.80`.
- [ ] **[verify]** `git grep -n 'android:exported="true"' app/src/main/AndroidManifest.xml`
  returns only the LAUNCHER activity of `SplashScreenActivity` and
  `LockActivity` (in the moderator), nothing else.

## B. Firebase console (10 min, one-time per deploy)

- [ ] **[block]** Open the Firebase console → Firestore Database → Rules.
  Paste the contents of `docs/firestore.rules` → Publish.
- [ ] **[verify]** Rules playground: simulate a signed-in client creating a
  `CRASH_LOGS` doc with `kind=CRASH, severity=CRITICAL, status=OPEN, fixed=false,
  timestamp>0, app="wallet"`. Expect: Allow.
- [ ] **[verify]** Rules playground: simulate the same client trying to read
  `OFFICIAL_KEYS` (any signed-in user). Expect: **Deny** unless the uid has a
  `MODERATORS/{uid}` document with `active: true`.
- [ ] **[verify]** Rules playground: simulate a signed-in client trying to
  update `USERS/{otherUid}.phone`. Expect: Deny (only `isSelf` may update; the
  `publicProfileUpdateOnly()` helper blocks it).
- [ ] **[block]** Open Firebase console → Authentication → Sign-in method.
  Enable: Email/Password, Google. Disable phone sign-in unless the operator
  has set up the SMS provider (out of scope for this pass; phone sign-in was
  removed in `OBSERVATIONS.md`).
- [ ] **[block]** Firebase console → Project settings → Your apps → Android
  app (`com.harvana.app`). Confirm SHA-1 and SHA-256 of the *release* signing
  key are registered. Google sign-in will fail in release builds otherwise.

## C. Build host (15 min)

- [ ] **[block]** `cd Harvana && ./gradlew clean assembleRelease`
- [ ] **[verify]** `./gradlew :app:lintRelease` is clean. The project's
  `app/build.gradle` sets `lint { abortOnError true }`, so a failure here is
  a hard build break.
- [ ] **[verify]** `./gradlew :app:test` (unit tests) passes.
- [ ] **[verify]** Inspect the release APK with
  `bundletool dump manifest --bundle=app/build/outputs/bundle/release/app-release.aab`
  and confirm:
  - `<application android:allowBackup="false">`
  - `<application android:usesCleartextTraffic="false">`
  - `<application android:networkSecurityConfig="@xml/network_security_config">`
  - `data_extraction_rules.xml` referenced on `<application>` (Android 12+)
  - `DebugActivity` is `exported="false"` and has **no** intent-filter
  - All non-launcher activities are `exported="false"`

## D. Real device smoke test (30 min)

Run on a freshly-installed APK signed with the release key on a real phone.

- [ ] **[block]** Cold start: app reaches Home in < 5 s; no crash dialog.
- [ ] **[block]** BottomSheetDialog flows: asset picker, coin detail sheet,
  sell-confirm sheet, buy-confirm sheet, transfer-confirm sheet. Each opens
  without `InflateException` (the BottomSheetBehavior keep rule).
- [ ] **[block]** Wallet socket connects, `BALANCE_UPDATE` arrives within 15 s.
- [ ] **[block]** Send a 0.001 HARV transfer to a known address. Confirm the
  receipt screen shows the full breakdown. Confirm
  `logcat -d | grep DappBridge` shows no "bridge message rejected" lines.
- [ ] **[block]** Open the dapp browser to a known dapp (e.g. a test Uniswap
  page on a testnet). Connect, sign a message, verify the signature. Confirm
  `isSecureContext` is true (the wallet object should install). Navigate to
  `about:blank` in a new tab; confirm `window.ethereum` is **not** installed.
- [ ] **[block]** Trigger a crash (any uncaught exception). Confirm the
  crash-dump screen opens, the report is saved to Downloads, and
  `CRASH_LOGS` has a new doc in Firestore within 30 s with `kind=CRASH,
  severity=CRITICAL, status=OPEN`.
- [ ] **[verify]** `adb logcat | grep -i harvana` shows no PII (wallet
  addresses, raw balances, incoming tx amounts). The 5 log calls fixed on
  18 Oct 2026 should now log only event names.

## E. One-time data migration (operator decision)

- [ ] **[verify, optional]** Run the USERS legacy migration:
  `USERSLegacyMigration.runAll(allUids, listener)`. The
  `publicProfileUpdateOnly()` rule is graceful-degradation, so this is the
  cleaner end-state, not a blocker.

## F. Other repos (3 repos, 45 min total)

The exact patches are in `docs/CROSS_REPO_AUDIT.md` for each repo.

- [ ] **[block]** `DOKTOHSMILE/capital-oracle-projects`: confirm Cloud Functions
  are deployed (N1) and Learn reviewer is in flight (N2).
- [ ] **[block]** `DOKTOHSMILE/Harvanna-protocol-miner`: apply §3.1 patches
  (minify, BouncyCastle 1.80, versionCode 4, release keystore), device-test
  mining start → share → reward.
- [ ] **[block]** `DOKTOHSMILE/Harvana-moderator-app`: apply §4.1–4.3
  patches (XML keep rules, minify, BouncyCastle 1.80, network security
  config, LockActivity `launchMode="singleTask"`), device-test
  challenge-response sign-in + the four post-lock screens.

## G. Token hygiene (do this once per build session)

- [ ] **[block]** The `ghp_…` PAT pasted in chat must be **revoked** at
  <https://github.com/settings/tokens>. The replacement goes in
  `~/.gradle/gradle.properties` on your machine (or CI secret), **never** in
  a build file or chat.
- [ ] **[verify]** Firebase service-account JSON, the `doktohsmile.keystore`,
  and the moderator's Ed25519 key are not in any repo, chat, or CI log.

---

## What "production ready" means in this checklist

A build is "production ready" when every **[block]** is ticked, every
**[verify]** is green, the six repos are at the same level, and the
`/api/v1/moderator/health` endpoint reports `OK: DEGRADED` or `OK` for the
node. The 18 Oct 2026 batch moves Harvana from 65% to 100% on the wallet side
once these 7 blocked items are checked off.
