# Node reconciliation report (Tier 5, partial)

Reference: node repo `capital-oracle-projects` (read-only; nothing there was modified).
Scope of this pass: signed-action canonical messages and endpoint paths for what the app actually calls.

## Signed actions

| Action | App | Node | Result |
|---|---|---|---|
| Auth challenge (`HARVANNA_AUTH`) | uses node-supplied `canonical`; fallback fixed | `AuthChallengeStore.canonicalMessage` | MATCH (fixed fallback) |
| Native transfer | `from+to+amount+nonce+memo` | `TransactionPayload` | MATCH (T0-4) |
| `P2P_CLAIM_PAYMENT` | same fields | same | MATCH |
| `P2P_MARK_PAID` | same | same | MATCH |
| `P2P_CONFIRM_RECEIVED` | same | same | MATCH |
| `P2P_CANCEL_LISTING` | same | same | MATCH |
| `P2P_RAISE_DISPUTE` | same | same | MATCH |
| `P2P_LIST_MINE` | same | same | MATCH |
| `P2P_INSURANCE_DEPOSIT` | same | same | MATCH |
| `P2P_RESOLVE_DISPUTE` (guardian) | same | same | MATCH |
| `P2P_CREATE_LISTING` | now `chain/P2pCanonical.createListing` (golden-tested against the node class) | `addr:side:total:minOrder:maxOrder:durationHours:paymentWindowMinutes:unitPricePerHarvRaw:nonce` | MATCH (fixed 29 Sep 2026) |
| `P2P_GET_ORDER` | not implemented (app uses public GET `/order/{id}`) | GET still exists but returns a public view with bank/payout fields stripped; signed `POST /order/fetch` returns the full party view | GET works; app only gets the public view (payment details come from Firestore). Adopting the signed fetch is optional |

(Historical note, now resolved) `P2P_CREATE_LISTING` fix needed product input: the node now requires a max order amount, payment window and a
per-HARV price (`unitPricePerHarvRaw`, the mark/unit-of-account work the prompt says is in progress). The seller
UI has no fields for these. Listing creation will fail signature verification until they are added.
Not guessed.

## Node actions the app has no client for
The node defines 41 `getCanonicalMessage()` formats. The app implements none of the `HARVANNA_*_V1` group/Circle/
Accumulative/Fixed/GenLoan/staking/referral/identity-link actions (grep finds no such string). Group logic in the
app is Firestore-only (`GroupPayoutManager`, `GroupLoanManager`, ...), i.e. it is not wired to the on-chain group
protocol at all. That is a feature build, not a fix, and needs a decision on scope.

## Endpoints
App paths checked against node mappings: `/tx/send`, `/tx/status/*`, `/tx/estimate-fee`, `/wallet/balance|
transactions|pending`, `/node/status`, `/supply`, `/mining/challenge|auth`, and all `/p2p/*` paths listed in
the app exist on the node. **WebSocket auth: MISMATCH, fixed.** The node closes `/ws/wallet` if a token is in the query string; the app sent
both. The app now sends the token only in the `Authorization: Bearer` header. BALANCE_UPDATE / TX_CONFIRMED payload
shapes are compatible (app reads optional fields with defaults).
Not yet checked: request/response field names, `/blocks/latest`.

## Not done
Request/response shape comparison, tenure/participation gates, vouching, referral rewards.

## Update: request nonces (fixed)
The node's `/wallet/balance/{address}` returns `nextRequestNonce`, the nonce every signed group/P2P request must
carry. In today's legacy mode (`forbid-off-block-nonce=false`) it equals chain nonce + 1, but in strict mode it is
`requestNonce + 1` and diverges. The app computed `nonce + 1` for all seven P2P actions; they now use
`nextRequestNonce` (falling back to `nonce + 1` if absent). Native transfers still use the chain nonce.

## Update: native transfer request/response contract (matches)
`/tx/send` body fields (`from,to,amount,gas,nonce,memo,signature,publicKey`) match `TransactionPayload`;
`/tx/estimate-fee` returns `estimatedGas`, which the app reads.
