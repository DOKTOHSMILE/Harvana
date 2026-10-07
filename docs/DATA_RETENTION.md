# Deleted data: hidden at once, kept one year, then erased

When someone deletes something in Harvana, it disappears from every screen straight away. A private copy is kept for one year so a dispute, a fraud report or a lost-funds claim can still be looked into. After the year the copy is erased automatically.

## What is kept

Before the app deletes a document, it writes a copy to the Firestore collection `DELETED`, in the same batch as the delete. If the copy cannot be written, nothing is deleted. Each copy holds:

| Field | Meaning |
|---|---|
| `kind` | What it was, for example `GROUPS`, `GROUPS/members`, `LEARN_CONTENT`, `INVITES`, `P2P_CHATS/messages` |
| `path` | Where it lived, for example `GROUPS/abc/payments/xyz` |
| `data` | Every field of the document as it was |
| `deletedBy` | The uid of the person who deleted it |
| `deletedAt` | Server time of the delete |
| `purgeAt` | One year after the delete. The copy is erased after this time |

This covers:

- a deleted group: the group, its members, notices, payments, loans, join requests, payout requests, private records and its invitations;
- a deleted Learn post or profile article;
- an expired trade chat and its messages;
- anything removed through `FirestoreSyncManager.deleteDocument`.

What is not kept: undoing a follow, a block or a reaction, and removing your phone or email from contact discovery. These are switches, not content. Keeping them would defeat the reason you removed them.

The network's own records (balances, transactions, group escrow on the node) are never deleted. Nothing here touches them.

## Who can see the copies

Nobody, from the app. The rules (`docs/firestore.console.rules`, `match /DELETED/{id}`) allow:

- a signed-in user to create a copy, only under their own uid, with `deletedAt` set to server time and `purgeAt` set to one year from now;
- no read, change or delete of a copy, by anyone.

Only the project owner can open the copies, in the Firebase console or with the Admin SDK, when a dispute needs them.

## What the owner does once

1. Publish `docs/firestore.console.rules` (Firebase console → Firestore Database → Rules → paste → Publish).
2. Turn on automatic erasing:
   1. Go to Firebase console → Firestore Database → **Time-to-live** (or Google Cloud console → Firestore → Time-to-live).
   2. Choose **Create policy**.
   3. Set **Collection group** to `DELETED` and **Timestamp field** to `purgeAt`.
   4. Choose **Create**.

   With the gcloud CLI, the same step is one line:

   `gcloud firestore fields ttls update purgeAt --collection-group=DELETED --enable-ttl`

   Firestore usually erases an expired copy within a day of its `purgeAt` time.

Until step 2 is done, copies are kept but never erased. Until step 1 is done, deletes fail, because the rules refuse the copy and the batch fails with it. So publish the rules before installing the new build.

## Changing the keep time

`SoftDelete.KEEP_MS` in `app/src/main/java/com/harvana/app/firestore/SoftDelete.java` sets the keep time. The rules check that it is between 364 and 366 days. Change both together.
