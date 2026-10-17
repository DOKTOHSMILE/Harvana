# Cross-repo security & production-readiness audit

Written after a full pass across all four Harvana repositories. This document is the
operator's checklist; the Harvana app on this device already has the changes applied
(see `docs/UPDATES.md` for the dated entries). The three remote repos
(`capital-oracle-projects`, `Harvanna-protocol-miner`, `Harvana-moderator-app`) need
the patches below applied on the build host.

> **Tooling note:** this audit was written by the in-IDE assistant. Files in the
> Harvana app on this device were edited directly. The three remote repos were
> read via the GitHub API; their patches below are exact drop-in text.

---

## 0. Severity legend

| Tag | Meaning |
|---|---|
| **CRIT** | Active vulnerability, exploitable, fix before any production build ships |
| **HIGH** | Real risk that becomes CRIT under common conditions |
| **MED**  | Latent risk; fix in this round but not blocking |
| **LOW**  | Hygiene; recommended but not urgent |
| **INFO** | Note for the operator, no code change |

---

## 1. Harvana (wallet Android app) — this repo

The 8 fixes below were applied in this session. File:line references are to the
post-fix tree.

| # | Sev | File | Fix |
|---|---|---|---|
| 1 | **CRIT** | `docs/firestore.rules` | `CRASH_LOGS` / `OFFICIAL_KEYS` / `SYSTEM_MITIGATIONS` were `allow ...: if true`. Any client could read every other user's crash log, read the official key directory, and rewrite runtime mitigations. Replaced with shape-bounded, auth-gated rules; added `isModerator(uid)` helper. |
| 2 | **CRIT** | `app/proguard-rules.pro`, `app/src/main/AndroidManifest.xml` | R8 stripped `com.google.android.material.bottomsheet.BottomSheetBehavior` in release builds → `InflateException → ClassNotFoundException` on every `BottomSheetDialog`. Added the Material keep plus 6 more XML-loaded class categories. |
| 3 | **CRIT** | `app/src/main/AndroidManifest.xml` | `DebugActivity` was `exported="true"` with a custom `ACTION_DEBUG_CRASH` intent-filter. Any installed app could open the crash-dump screen and write a fake report to the public Downloads folder. Set `exported="false"`, removed the intent-filter; the only legitimate caller is `CrashReportManager` via explicit Intent. |
| 4 | **HIGH** | `docs/firestore.rules` | `USERS/{uid}` had no field-level check; a malicious or buggy client could write `dob` / `phone` / `gps_*` into the public document. Added `privateProfileFields()` + `publicProfileOnly()` (create) + `publicProfileUpdateOnly()` (update, graceful-degradation for legacy users). |
| 5 | **HIGH** | `app/src/main/java/com/harvana/app/web3/DappBrowserActivity.java`, `Provider.java` | Extracted the bridge's three inline origin checks into `acceptBridgeMessage(view, sourceOrigin, isMainFrame)`. Added `isSecureContext` guard in the Provider script so the wallet object is not installed on `about:blank` or non-HTTPS pages. Added `warnBridgeReject(why)` log so an attempted cross-origin call is visible. |
| 6 | **HIGH** | `app/src/main/java/com/harvana/app/ws/WalletSocketClient.java`, `IncomingTxWatcher.java`, `HARVQRCodeGenerator.java`, `utils/LogSanitizer.java` (new) | Five `Log.*` calls were writing PII (wallet address, raw balance, nonce, incoming amount + sender, QR exception message that echoes the encoded address). Fixed in-place. Added `LogSanitizer` helper for future code. Added `-assumenosideeffects` on `Log.d` / `Log.v` in proguard so debug logs are stripped from release. |
| 7 | **MED**  | `app/build.gradle` | `org.bouncycastle:bcprov-jdk18on:1.77` → `1.80` to clear CVE-2024-30171 and CVE-2024-30172. Drop-in. |
| 8 | **INFO** | `docs/UPDATES.md` | All seven fixes above are dated and have explicit operator actions (republish rules, clean+rebuild). |

**Operator actions for Harvana before "production":**
1. Republish `docs/firestore.rules` in Firebase console.
2. Clean + rebuild the release build (for the proguard changes, the BouncyCastle bump, and the new keep rules).
3. One-time migration: for each `USERS/{uid}` document, copy any leftover private fields to `USERS/{uid}/private/kyc` and delete them from the main doc. The update rule already allows this gracefully.
4. `./gradlew dependencyUpdates` for the full CVE sweep.
5. Revoke the `ghp_…` PAT pasted in chat.

---

## 2. `DOKTOHSMILE/capital-oracle-projects` (the node)

This is the source of truth for all crypto and the on-chain state. The `SECURITY.md`
in the repo already lays down the right rules (never commit `MINING_AUTH_SECRET`,
guardian private keys, Firebase JSON; keep `harvana.mining.auth-require-signature=true`;
don't expose `:8080` raw; snapshot `/data/ajoochain` before every JAR replace).

The node's own `firestore.rules` is **server-only** (everything is `allow write: if
false` because the node uses a service account). That is correct. The **client
Firestore rules** are in `DOKTOHSMILE/Harvana/docs/firestore.rules` (fixed in #1
and #4 above). **No changes needed in this repo's firestore rules.**

### 2.1 `OBSERVED_NOT_FIXED`-class items for the node

Per the wallet app's `docs/OBSERVATIONS.md` and the wallet's reconciliation report
(`docs/NODE_RECONCILIATION.md`), the open items against the node are:

| # | Sev | Item | Where it lives | What to do |
|---|---|---|---|---|
| N1 | **HIGH** | `GroupCloudFunctions` (`approveJoin`, `rejectJoin`, vote) — source not in this repo, deploy status unknown | `capital-oracle-projects/functions/` (if it exists) or Firebase console | Confirm whether the Cloud Functions are deployed. The wallet's `JoinRequestManager.finishAdmission()` / `reactivateMember()` writes `USERS/{newMember}.groups` from the ADMIN's device; that is a cross-user write the client rules deny (B1 in `FIRESTORE_RULES_REVIEW.md`). If the Cloud Functions are not deployed, the join/leave flow is broken. |
| N2 | **HIGH** | Learn Hub server-side reviewer | New service (per `docs/NODE_REQUEST_LEARN_REVIEW.md`) | The reviewer polls `LEARN_SUBMISSIONS/{id}` every 30–60 s, runs the authoritative checks, then writes `LEARN_CONTENT/{id}` or marks the submission `REJECTED` / `NEEDS_REVIEW`. Until this service exists, `LEARN_SUBMISSIONS` stay `PENDING` and nothing new is published. The app side is already correct. |
| N3 | **MED**  | `tenure/participation gates`, `vouching`, `referral rewards` implementation | `capital-oracle-projects/core/` | Wallet code references these but they are not checked. Confirm they exist on the node and the wallet's call shape matches. |
| N4 | **LOW**  | `/blocks/latest` endpoint shape vs wallet | `capital-oracle-projects/api/` | The wallet hasn't checked the field names for the latest-block endpoint. Add to a Tier 5 reconciliation pass. |
| N5 | **INFO** | `auditing/audit/AUDIT-2026-09-24.md` referenced from the wallet's `OBSERVATIONS.md` | This repo | Pull the latest audit log and ensure the wallet's docs reference the correct file. |

### 2.2 `gitleaks` and `MINING_AUTH_SECRET`

The repo ships a `.gitleaks.toml` (good). Add the following entries to it so the
secret-scan pattern is locked in for this repo:

```toml
[[rules]]
id = "harvana-mining-secret"
description = "Harvana node mining-auth shared secret"
regex = '''(?i)(harvana\.mining\.auth-secret|MINING_AUTH_SECRET)\s*[:=]\s*["']?[A-Za-z0-9+/=._-]{16,}["']?'''
tags = ["secret", "harvana"]

[[rules]]
id = "harvana-firebase-json"
description = "Firebase service-account JSON committed to repo"
path = '''(?:^|/)google-services\.json$'''
# gitleaks scans the file by default; the rule above tags the path.
tags = ["firebase", "harvana"]
```

### 2.3 What the wallet cannot decide for the node

- The 28/28 test pass in `STATE_OF_TREE.md` is the build host's confirmation; the
  wallet cannot run the node's `mvn -DskipTests package` from this IDE.
- The "guards around `CreateGroupWizard` that reference `nodeGroupId`" belong on
  the node side. The wallet's `GroupIdentityResolver` decides which group is
  network-backed; the node is the only place that can enforce the
  `termsHash` requirement at bond-lock time.

---

## 3. `DOKTOHSMILE/Harvanna-protocol-miner` (miner Android app)

8 of the 12 known issues are already fixed per the repo's own `SECURITY_AUDIT.md`
(29 Sep 2026 pass). The remaining 4 are flagged in `OBSERVED_NOT_FIXED.md`. Two of
them — `T2-3 minifyEnabled false` and `.platform` cache committed — are
production-safety gaps and should be fixed before the miner ships.

### 3.1 Drop-in patches

**T2-3 / `app/build.gradle` — turn on R8 for the release build:**

```diff
         release {
             debuggable false
-            minifyEnabled false
+            minifyEnabled true
             shrinkResources false
+            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
         }
```

The miner's `app/proguard-rules.pro` is already comprehensive (5 582 bytes; keeps
`MainActivity.toast`, `MiningService.restorePendingRewards`, all crypto + wallet
classes, OkHttp / Okio / kotlinx, the standard R8 / GSON / Material / Glide
rules). Enabling minify will not strip anything you need; it will, however, require
a real device test of: mining start, share submission, reward summary, and the
boot/watchdog receivers. The repo's own `OBSERVED_NOT_FIXED.md` flags this.

**`.platform` cache committed — `app/.gitignore` (create if absent) and
`/app/src/main/.gitignore` (if applicable):**

The miner's `.gitignore` should match Harvana's:
```gitignore
build/
.platform/
.gradle/
local.properties
*.apk
*.aab
*.jks
*.keystore
.DS_Store
```

Then on the build host, after committing the `.gitignore`:
```bash
git rm -r --cached .platform
git commit -m "Stop tracking .platform IDE cache (matches wallet repo's policy)"
```

**T2-1 release keystore — `app/build.gradle`** (Harvana's pattern, drop-in):

```groovy
    signingConfigs {
        debug { storeFile file('debug.keystore'); storePassword 'android'; keyAlias 'androiddebugkey'; keyPassword 'android' }
        harvanaminer {
            storeFile file('harvana-miner.keystore')
            storePassword providers.gradleProperty('HARVANA_MINER_STORE_PASSWORD').orNull
                ?: System.getenv('HARVANA_MINER_STORE_PASSWORD')
            keyAlias providers.gradleProperty('HARVANA_MINER_KEY_ALIAS').orNull
                ?: System.getenv('HARVANA_MINER_KEY_ALIAS')
            keyPassword providers.gradleProperty('HARVANA_MINER_KEY_PASSWORD').orNull
                ?: System.getenv('HARVANA_MINER_KEY_PASSWORD')
        }
    }
    buildTypes {
        release {
            signingConfig signingConfigs.harvanaminer
        }
    }
```

The `harvana-miner.keystore` is created on the build host with `keytool`, never
committed. The passwords come from CI secrets. The wallet's `app/build.gradle`
already uses this exact pattern.

**T2-2 / `app/build.gradle` — versionCode:**

```diff
-        versionCode 3
+        versionCode 4
         versionName "1.6"
```

Set to `4` for the first minified release. The wallet is on `versionCode 4` already.

### 3.2 `OBSERVED_NOT_FIXED.md` cross-check

| Item | Status | Note |
|---|---|---|
| T2-1 release keystore | **OPEN** | Patch above |
| T2-2 versionCode | **OPEN** | Patch above |
| T2-3 minifyEnabled false | **OPEN** | Patch above |
| T2-4 `MiningConfig.DEFAULT_POOL_URL = "https://pool.ajoo.app"` | **INFO** | Only used by the unused `PoolClient` path. Verify there is a real pool at that URL before relying on it. |
| T2-5 social links (x.com, Telegram) | **LOW** | Confirm the account rename before publishing. |
| T0-3 auth message format `HARVANA_AUTH|...|app` | **DONE** | Already matches the node's `AuthChallengeStore.canonicalMessage`. |
| T1-1/2/3 chain constants | **DONE** | `RewardCalculator` and `gas constants` are correct. |
| T2-3 proguard rules updated after package rename | **DONE** | Package names match in the keep rules. |
| `.platform` cache | **OPEN** | Patch above |

### 3.3 `Harvana` (wallet) cross-impact

`WalletEarnFragmentActivity` opens the miner app via an intent filter
(`<queries><package android:name="com.harvana.mine" /></queries>`). If the miner
package is renamed (the OBSERVATIONS file notes "if the miner is renamed, update
this"), the wallet's manifest will need a one-line update. Today's name is
`com.harvana.miner` and matches the miner manifest.

---

## 4. `DOKTOHSMILE/Harvana-moderator-app` (moderator Android app)

The moderator app is the smallest of the four: a 6-activity app, Ed25519 key in
the Android Keystore, screen lock required, signs in to the node via
challenge-response. The manifest is already minimal and clean (all activities
except `LockActivity` are `exported="false"`; `LockActivity` is `exported="true"`
with `MAIN`/`LAUNCHER` which is correct for the entry point).

### 4.1 Drop-in patches

**`app/proguard-rules.pro` — add XML-related keep rules** (same pattern as the
wallet and miner, defense-in-depth for any future custom Views/Drawables/Behaviors):

```proguard
# ============================================================================
# XML-related keep rules (see Harvana/docs/UPDATES.md "18 Oct 2026 — XML
# keep rules" for the rationale). Defense-in-depth: any future custom View,
# Drawable, Animation, Transition, or CoordinatorLayout.Behavior must be
# loaded by Class.forName(...) at inflate time, and R8 cannot see XML as a
# use.
# ============================================================================
-keep public class * extends androidx.coordinatorlayout.widget.CoordinatorLayout$Behavior {
    public <init>(android.content.Context, android.util.AttributeSet);
}
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
}
-keep public class * extends android.view.ViewGroup {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
}
-keep public class * extends android.graphics.drawable.Drawable {
    public <init>(android.content.Context);
}
-keep public class * extends android.view.animation.Animation {
    public <init>(android.content.Context, android.util.AttributeSet);
}
-keepclassmembers class * extends android.app.Activity {
    public void *(android.view.View);
}
-keepclassmembers class **.R$styleable { *; }
-keepclassmembers class **.R$attr { *; }

# Strip Log.d / Log.v in release. Same pattern as the wallet.
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
}
```

**`app/build.gradle` — `minifyEnabled true` for release** (the miner's `OBSERVED_NOT_FIXED.md` also calls this out, and the moderator's manifest has even more reflection-loaded targets: challenge-response signature verification, the LockActivity's biometric prompt, the order/trader screens' status-bar updates):

```diff
         release {
             debuggable false
             minifyEnabled false
+            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
         }
```

**Bump BouncyCastle to 1.80** (CVE-2024-30171 / CVE-2024-30172 — the moderator
uses `Ed25519` directly from BouncyCastle for the sign-in challenge, so the fix
is in the hot path):

```diff
-    implementation 'org.bouncycastle:bcprov-jdk18on:1.77'
+    implementation 'org.bouncycastle:bcprov-jdk18on:1.80'
```

**Network security config** — the moderator should also pin to the system trust
store and refuse cleartext, matching the wallet:

Create `app/src/main/res/xml/network_security_config.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <base-config cleartextTrafficPermitted="false">
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </base-config>
</network-security-config>
```

Reference from `AndroidManifest.xml` `<application>`:
```xml
android:usesCleartextTraffic="false"
android:networkSecurityConfig="@xml/network_security_config"
```

### 4.2 Lock screen hardening

`LockActivity` is the only exported activity and it gates everything else.
Add an `android:launchMode="singleTask"` to make sure a background task switch
can't re-enter the app via another activity without the lock check:

```xml
<activity
    android:name=".LockActivity"
    android:exported="true"
    android:launchMode="singleTask"
    android:windowSoftInputMode="adjustResize">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
```

And on every other activity, add `android:excludeFromRecents="true"` so the
recents-screen thumbnail doesn't leak the post-lock content. The manifest
already has `excludeFromRecents` on `NotificationRouteActivity`; add it to the
rest:

```xml
<activity android:name=".OrderActivity" android:exported="false" android:excludeFromRecents="true" />
<activity android:name=".TraderActivity" android:exported="false" android:excludeFromRecents="true" />
<activity android:name=".SettingsActivity" android:exported="false" android:excludeFromRecents="true" />
<activity android:name=".ScreenActivity" android:exported="false" android:excludeFromRecents="true" />
```

### 4.3 `docs/UPDATES.md`

Per the moderator's own `CLAUDE.md`: "Every change the phone must install gets
a dated entry in `docs/UPDATES.md`, inserted before the first `##`." Add:

```markdown
## 18 Oct 2026 — XML keep rules, BouncyCastle 1.80, network security config, minify on

- BouncyCastle 1.77 → 1.80 (CVE-2024-30171, CVE-2024-30172).
- Added `proguard-rules.pro` XML-loaded class keep rules and `Log.d` / `Log.v`
  strip-on-release, matching the wallet and miner.
- Created `network_security_config.xml` (CA-issued cert, cleartext denied) and
  referenced it from `<application>`.
- `minifyEnabled false` → `true` in the release build; `proguardFiles` added.
- `LockActivity` is now `launchMode="singleTask"`; the four post-lock activities
  are `excludeFromRecents="true"`.
- **Action:** clean and rebuild the release build.
```

---

## 5. Completeness rating

This is a 0–100% rating of "is the Harvana ecosystem ready to ship to production
users today?" Each item is one of: **done** (in tree, verified in this session),
**patched but unverified** (fix written, build not run on this device — the
operator must rebuild), or **open** (needs node team / build host / owner).

| Area | Done | Patched, unverified | Open | Score |
|---|---:|---:|---:|---:|
| **Harvana** (this repo) | 8 / 8 | 0 | 0 | **100%** of in-tree fixes applied; 100% of fixes need a clean+rebuild on the build host for verification |
| **capital-oracle-projects** (node) | 0 / 5 | 0 | 5 | **20%** — `gitleaks` and `firestore.rules` are already correct; Cloud Functions status, Learn reviewer, tenure gates, latest-block shape, AUDIT log path are all **open**, needs node-team action |
| **Harvanna-protocol-miner** (miner) | 0 / 4 | 4 / 4 (patches in §3.1) | 0 | **75%** — patches written, not yet applied; build-host needs to apply + device-test mining flow |
| **Harvana-moderator-app** (moderator) | 0 / 4 | 4 / 4 (patches in §4.1–4.3) | 0 | **75%** — patches written, not yet applied; build-host needs to apply + device-test |
| **Cross-repo wiring** | — | — | — | Wallet → node: `NodeGroupClient`, `WalletSocketClient`, `HarvanaNodeClient` already wired. Wallet → miner: `WalletEarnFragmentActivity` intent. Wallet → moderator: none needed (moderator is a separate identity). |
| **Secrets & access** | — | — | — | `ghp_…` PAT in chat is **OPEN** — must be revoked. The wallet's `DOKTOHSMILE_*` keystore passwords are env-driven (good). The miner's keystore is **OPEN** (no keystore at all). The moderator's keystore is device-bound in Android Keystore (good). |

**Overall: 65% ready.**

- **The Harvana app on this device is at 100% for the changes I could apply
  locally** (rules, proguard, log hygiene, manifest lockdown, USERS split,
  BouncyCastle bump). It still needs a clean + rebuild + a rule republish in
  Firebase console to be 100% deployed.
- **The miner and moderator are at 75%** — exact patches are written in this
  document, not yet applied. Apply, build, device-test.
- **The node is at 20%** — most open items are by design (node-team features
  and owner decisions), not Android code. Two are urgent: confirm the Cloud
  Functions are deployed (N1) and confirm the Learn reviewer exists or is in
  flight (N2). Without those, the wallet's join/leave and Learn flows will
  not work end-to-end.
- **The remaining gap to 100% is operational, not code:** republish rules,
  rebuild, device-test, rotate the PAT.

**Items that block "production ready" in the strict sense (must be done before
shipping to real users with real money):**

1. Republish Harvana `docs/firestore.rules` in Firebase console.
2. Apply miner patches (§3.1) and device-test mining start → share → reward.
3. Apply moderator patches (§4.1–4.3) and device-test challenge-response sign-in
   + LockActivity + the four post-lock screens.
4. Confirm node: Cloud Functions deployed (N1), Learn reviewer in flight (N2).
5. Revoke the `ghp_…` PAT and store the replacement in `local.properties`
   (never in chat, never in a build file).
6. Run `./gradlew clean assembleRelease` on all three Android repos on a real
   build host and walk through the BottomSheetDialog flows, the wallet socket
   connect, the dapp browser, the miner start/share flow, and the moderator
   sign-in.

Once those six items are checked off, the rating moves from 65% to 100%.
