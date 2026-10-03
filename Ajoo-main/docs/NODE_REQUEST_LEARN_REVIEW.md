# Request for the node team: automatic review of Learn Hub submissions

The app no longer publishes Learn Hub articles itself. It writes a submission, and a server-side reviewer (the node already holds Firestore service-account access through `FirestoreBridgeService`) decides what gets published. The app cannot write `LEARN_CONTENT` any more (see `docs/firestore.rules`).

## Flow
1. App: the author's phone runs a light screen (`learn/LearnContentScreen`: guaranteed returns, seed phrase / PIN / OTP requests, "send money to me", impersonation of the team, shortened or chat-invite links, shouting, non-https image) and, if it passes, creates `LEARN_SUBMISSIONS/{id}` with `status: "PENDING"`.
2. Node reviewer (new): picks up `PENDING` submissions (poll every 30-60 s, or a Firestore listener), runs the authoritative checks, then:
   - **APPROVED** -> create `LEARN_CONTENT/{sameId}` with `title, description, category, imageUrl, authorUid, createdAt, views: 0, approvedAt, submissionId`; then update the submission: `status: "APPROVED", reviewedAt, contentId`.
   - **REJECTED** -> update the submission: `status: "REJECTED", reviewedAt, reviewCode (short code), reviewMessage (one plain sentence the author can read)`. Nothing is written to `LEARN_CONTENT`.
   - **NEEDS_REVIEW** (unsure) -> `status: "NEEDS_REVIEW"`; shown to a human on the moderator dashboard, who then approves or rejects. The app shows this to the author as "still in review".
3. App: `LEARN_CONTENT` is read with a live listener (new articles appear without a refresh). `NotificationSync` turns `APPROVED` / `REJECTED` into an inbox notification for the author.

## Checks the reviewer should run (all server-side, the phone screen is only the first gate)
- Everything in the phone screen (same patterns, so a tampered app gains nothing).
- Links: allow-list or reputation check; reject shorteners, IP-address links, lookalike domains of harvannaprotocol.com, and wallet-drainer patterns; resolve redirects.
- Promotion: reject or hold posts that push a token, group, exchange or channel with a referral or contact call-to-action (phone numbers, handles, invite links).
- Impersonation: names or claims of the Harvana team, moderators or "support".
- Financial claims: guaranteed or fixed returns, "double your money", "insider" tips.
- Image: fetch the cover image (size and type limits) and hold it if it fails to load or is not an image.
- Duplicates: same or near-identical text from the same or other authors (flood/spam).
- Author: rate limit (for example 3 submissions per author per day) and hold authors with confirmed reports (`MEMBER_REPORTS` with status CONFIRMED).
- Idempotency: reviewing the same submission twice must not create two articles (use the submission id as the article id).

## Fields
`LEARN_SUBMISSIONS/{id}`: `title` (<=120), `description` (<=600), `category`, `imageUrl` (https), `authorUid`, `createdAt` (ms), `views` (0), `status`.
The reviewer adds: `reviewedAt`, `reviewCode`, `reviewMessage`, `contentId`.

## Not done in the app, needs the node
The reviewer service itself, its thresholds, and the moderator-dashboard tab for `NEEDS_REVIEW`. Until it exists, submissions stay `PENDING` and nothing new is published (existing `LEARN_CONTENT` articles stay visible).
