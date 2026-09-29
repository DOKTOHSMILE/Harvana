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
