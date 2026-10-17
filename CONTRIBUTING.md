# Contributing

This is the Harvana wallet Android app. The protocol is Harvana; the node is
`DOKTOHSMILE/capital-oracle-projects`, the miner is
`DOKTOHSMILE/Harvanna-protocol-miner`, and the moderator app is
`DOKTOHSMILE/Harvana-moderator-app`. Cross-repo changes (group subsystem,
Learn reviewer, hUSD bridge, identity link) belong in the node repo; this
repo is the wallet client only.

## Build environment

- **JDK 17** (Temurin or any vendor build). The `compileOptions` block in
  `app/build.gradle` pins `sourceCompatibility = VERSION_17`.
- **Android SDK** with `compileSdk 34`, `targetSdk 34`, `minSdk 23`. The
  CI in `.github/workflows/ci.yml` installs the platform automatically;
  on your machine, install via `sdkmanager "platforms;android-34"
  "build-tools;34.0.0"`.
- **Gradle 8.x** via the wrapper in `gradlew`. Do not install Gradle
  globally; always use the wrapper.
- **Firebase CLI** is not required to build (the wallet ships a checked-in
  `app/google-services.json`); it is required to republish Firestore rules
  from the build host, see `tools/RELEASE_CHECKLIST.md`.

## Build flow

1. Clone with submodules if you add any (none today). `git clone` is enough.
2. `cd Harvana && ./gradlew assembleDebug` for a debug APK.
3. `./gradlew test` for the JVM unit tests under `app/src/test/`.
4. `./gradlew assembleRelease` for the release build. Requires
   `app/doktohsmile.keystore` and the matching env vars; see
   `app/build.gradle`.

The Gradle files are generated from `app/module.toml` by
`tools/module_toml_to_gradle.py`. Edit the toml, run the script, commit
both. Do not edit `app/build.gradle` by hand — CI will diff and fail.

## Code style

- **Java 17**, no Kotlin in this repo. Use `var` only in diamond-operator
  positions; the rest of the file style follows Android's default.
- **EditorConfig** is not committed; follow the existing
  indentation (4 spaces, tabs forbidden in Java, 2 spaces in Gradle /
  XML). The CI does not enforce this; reviewers do.
- **Line endings** are enforced by `.gitattributes`: LF on every
  hand-written source, CRLF on `gradlew.bat` and `*.bat`. Do not commit
  CRLF line endings in `.java` files.
- **Imports** are organised by Android Studio's default optimiser
  (`Cmd+Shift+O` on macOS, `Ctrl+Shift+O` on Linux/Windows).

## Commit messages

The Harvana repos use [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <short summary>

<body — what changed and why, not how>
```

Types used: `feat`, `fix`, `chore`, `docs`, `refactor`, `test`, `perf`,
`security`. Scopes: `wallet`, `group`, `learn`, `p2p`, `dapp`, `rules`,
`build`, `ci`, `proguard`, `manifest`, `firestore`, `node` (for the node
repo).

Examples from the 18 Oct 2026 batch:

```
fix(rules): tighten CRASH_LOGS / OFFICIAL_KEYS / SYSTEM_MITIGATIONS
fix(manifest): lock DebugActivity to in-process callers
fix(proguard): keep BottomSheetBehavior + add XML-loaded class section
chore(deps): bump BouncyCastle 1.77 -> 1.80 (CVE-2024-30171)
```

## Tests

- **Unit tests** live in `app/src/test/java/`. Pure Java on purpose so
  they run in the in-process VM without an Android emulator.
  `./gradlew test` runs them.
- **Integration tests** (when added) live in `app/src/androidTest/java/`
  and need a real device or emulator. CI does not run these; they are
  the contributor's responsibility on a local build.
- **Firestore rules tests** use the Firebase Emulator Suite. There is no
  Gradle task wired yet; see `tools/RELEASE_CHECKLIST.md` for the manual
  steps until one is added.

## Pull request flow

1. Branch off `main`. Branch name format: `<type>/<scope>-<short-desc>`,
   e.g. `fix/wallet-balance-log-leak`.
2. Make the change. Update `docs/UPDATES.md` with a dated entry under
   the standing rule: every change the operator has to apply by hand
   (rules publish, config key, app build) gets a dated entry in the
   same commit.
3. Run `./gradlew clean assembleDebug test lintDebug` locally. The CI
   runs the same set.
4. Open a PR against `main`. The CODEOWNERS file auto-assigns the
   protocol team. The CI runs `gitleaks` (secrets), the static-checks
   job (no `if true` rules, BouncyCastle ≥ 1.80, no non-LAUNCHER
   exported activity, etc.), and the build job.
5. One human approval from a CODEOWNER is required to merge.

## Security-sensitive changes

If your change touches any of the following, treat it as a security
change and add a `## <date> — <summary>` entry in `docs/UPDATES.md` with
a clear **Action:** line for the operator:

- anything in `docs/firestore.rules` (the rule will be republished);
- `app/proguard-rules.pro` (the build's R8 contract);
- `app/src/main/AndroidManifest.xml` (component exposure);
- `app/src/main/java/com/harvana/app/crypto/`, `…/identity/`,
  `…/wallet/`, `…/web3/`, `…/firestore/`, `…/telemetry/`;
- `SECURITY.md`, `.gitleaks.toml`, `.github/workflows/`, `CODEOWNERS`,
  `.gitattributes`.

If your change introduces a new permission, a new exported component,
a new `WebMessageListener` origin, a new Firestore collection, or a new
crypto algorithm, write a one-paragraph note at the bottom of the
`docs/CROSS_REPO_AUDIT.md` "What we did NOT do" section explaining what
is out of scope and why. The protocol team uses that section as a
checklist for the next audit pass.

## Reporting bugs

Non-security bugs: open a GitHub issue. Security bugs: see
`SECURITY.md`. Performance regressions: open an issue with a profile
trace and a `bugreport` zip.

## Code of conduct

Be specific, be technical, be kind. This is a testnet. Real money is not
on the line; the goal is to ship a wallet that, when the chain
graduates to mainnet, has every defensive layer reviewed.
