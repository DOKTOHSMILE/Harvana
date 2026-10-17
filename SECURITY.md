# Security policy

This repository is the Harvana wallet Android app. The protocol is Harvana
(brand: Harvana; legacy spelling kept in the registered domain
`harvannaprotocol.com`). The wallet is the client of an on-chain protocol;
the node repo is `DOKTOHSMILE/capital-oracle-projects`, the miner is
`DOKTOHSMILE/Harvanna-protocol-miner`, and the moderator app is
`DOKTOHSMILE/Harvana-moderator-app`. Security issues that span all four
should be reported to the protocol team directly (see below), not via the
wallet's issue tracker.

## Supported versions

| Version | Supported |
| --- | --- |
| 1.3.x (testnet) | Yes — current build |
| < 1.3 | No |

This repository is the wallet. It is a testnet client. The HARV token on
this chain has no production value. Do not treat the on-chain balances as
real money.

## Reporting a vulnerability

**Email the operator privately.** Do not open a public GitHub issue for:

- key compromise, suspected key compromise, or wallet-key leakage;
- a remote-code-execution / arbitrary-write primitive reachable from the
  network or from the dapp browser;
- a by-pass of the moderator (`isModerator`) gate, the
  `publicProfileOnly` / `publicProfileUpdateOnly` rules, the
  `acceptBridgeMessage` origin check, or any other defensive layer listed
  in `docs/UPDATES.md`;
- an issue that lets a non-moderator user read or write a `CRASH_LOGS`,
  `OFFICIAL_KEYS` or `SYSTEM_MITIGATIONS` document in Firestore;
- a signer / key-handling bug in the wallet that would lead to loss of
  funds.

Include in the report:

- the version of the wallet (Settings → About, or the `versionName` /
  `versionCode` in the APK);
- the device model, Android version, and whether the build is debug or
  release;
- a clear repro: command, screen, transaction hash, or stack trace;
- the impact, as you understand it.

You will receive an acknowledgement within 7 days for reports on a
supported version. The protocol team triages by impact: signer / key bugs
first, dapp-bridge origin bypass next, rule-bypass third, then everything
else.

## Security model in one page

The wallet is a **client**. It does not hold custody. A compromised wallet
on a phone can sign transactions on behalf of that phone's owner; it
cannot, by itself, move funds the user has not authorised in the UI.

The five defensive layers the wallet is built on:

1. **Node is the source of truth** for balances, group state, and the
   signed group actions (`NodeGroupClient`, `HarvanaNodeClient`,
   `WalletSocketClient`). The wallet never moves a group's money by
   itself: it signs a request, the node carries it out.
2. **Dapp-bridge origin check** — `acceptBridgeMessage` in
   `DappBrowserActivity` is the only route from a web page to the wallet
   and rejects anything that is not the current page's main frame on
   `https://`. The Provider script additionally checks
   `window.isSecureContext` before installing `window.ethereum`.
3. **Firestore rules** (`docs/firestore.rules`) — the client cannot write
   a `CRASH_LOGS` shape that is not `kind=CRASH, severity in [...],
   status=OPEN, fixed=false`, cannot read or write `OFFICIAL_KEYS` or
   `SYSTEM_MITIGATIONS` unless the uid has a `MODERATORS/{uid}` doc with
   `active: true`, and cannot put a private field
   (`dob`, `v_type`, `bvn`, `nin`, `phone`, `email`, `address`,
   `occupation`, `gps_*`, `kin`, `city`) on a public `USERS/{uid}`
   document. The list is mirrored in `PrivateProfile.FIELDS` on the
   client; the CI in `.github/workflows/ci.yml` greps for any
   `allow.*if true` rule and fails the build.
4. **R8 + proguard** — `app/proguard-rules.pro` keeps every category of
   class Android inflates by `Class.forName(...)` from XML (Coordinator
   Behaviors, custom Views / Drawables / Animations / Transitions,
   `R$styleable`). Release builds strip `Log.d` / `Log.v`.
5. **Log hygiene** — every `Log.*` in production code paths goes through
   `com.harvana.app.utils.LogSanitizer` for any message that may contain
   a wallet address, balance, or incoming-tx detail.

If you find a way around any of these, that is in scope for private
reporting. If you find a way around the node's own checks, report to the
node repo, not this one.

## Out of scope

- The node's consensus, transport, or governance — see
  `DOKTOHSMILE/capital-oracle-projects`.
- The miner's reward math — see `DOKTOHSMILE/Harvanna-protocol-miner`.
- The moderator app's lock screen / key storage — see
  `DOKTOHSMILE/Harvana-moderator-app`.
- User error (lost seed phrase, shared PIN, sideloaded modified APK).
- Phishing outside the app: the wallet cannot prevent the user from
  pasting a seed into a fake website.

## Bounty

There is no public bounty program on this testnet. The protocol team may
acknowledge reporters in the post-fix changelog entry on a best-effort
basis.
