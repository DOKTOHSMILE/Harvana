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

## Phone / OTP auth and profile sync
- Added `PhoneAuthFragment`: real Firebase SMS OTP (send, verify, auto-verification, 60s resend, change number).
  Replaces the earlier hardcoded-test-code stub (which no longer exists in the code).
- Every sign-in method (email, phone, Google) ends at `SplashScreenActivity`, which looks up the profile by
  Firebase uid: exists -> MainActivity; missing -> `LoginActivity(page=complete)` (the sign-up form).
- Complete-profile form now prefills and locks the email and/or phone that Firebase already verified, prefills
  name from the display name, and requires a real signed-in Firebase uid (removed the random-UUID fallback).
  `LoginActivity` bounces to Splash if the form is opened with no signed-in user.
- Email, phone and Google sign-ins create separate Firebase accounts (separate uids/profiles); account linking
  is not implemented.
- Firebase console needs: Phone provider enabled, app SHA-1/SHA-256 registered, and Play Integrity/reCAPTCHA
  configured, plus T0-5 `google-services.json`. Not compiled or tested on device.
