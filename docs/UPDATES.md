# Updates

**Standing rule (owner instruction, 1 Oct 2026).** Every change pushed to this repository that an operator has to apply by hand gets a dated entry in this file, in the same commit. That includes a rules publish, a config key, and an app build the phone must install. A code change that cannot go live by itself is unfinished without the entry. The node repository keeps its own steps in `docs/DEPLOYMENT.md`. Do not put those steps here.

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
