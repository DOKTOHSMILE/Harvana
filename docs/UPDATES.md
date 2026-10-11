# Updates

**Standing rule (owner instruction, 1 Oct 2026).** Every change pushed to this repository that an operator has to apply by hand gets a dated entry in this file, in the same commit. That includes a rules publish, a config key, and an app build the phone must install. A code change that cannot go live by itself is unfinished without the entry. The node repository keeps its own steps in `docs/DEPLOYMENT.md`. Do not put those steps here.

## 11 Oct 2026: advert carousel on the Learn Hub

What the app does:

- **A strip of adverts sits at the top of the Learn Hub**, above the categories. Each advert:
  - is a rounded card with a picture, a "Sponsored" tag, a title, a short line and a button;
  - changes every 5 seconds and can be swiped by hand;
  - holds still while touched;
  - shows dots for its position.
- **When no advert is running, the strip is hidden.**
- **Tapping an advert opens its link:** an https page, or a screen in the app.
- **Each advert counts its views** (once per visit to the page) **and its taps**, so you can see how it performs.

**What you do:**

1. **Publish the rules:** copy `docs/firestore.rules` into Firebase console → Firestore → Rules → Publish. It adds the `LEARN_ADS` block. Only you, in the console, can add or change adverts; the app can only read them and add one view or tap.
2. **Add an advert:** Firebase console → Firestore → collection `LEARN_ADS` → Add document (automatic ID), with these fields:

| Field | Type | What it is |
|---|---|---|
| `active` | boolean | `true` to run it, `false` to stop it (required) |
| `title` | string | The headline, e.g. "Save weekly with Ajo groups" |
| `subtitle` | string | One short line under it (optional) |
| `imageUrl` | string | An `https://` picture, about 2:1 wide (optional; without one the card uses `color`) |
| `color` | string | Card colour when there is no picture, e.g. `#0B5F57` (optional) |
| `link` | string | `https://…` page, or `app:p2p`, `app:staking`, `app:my-trades`, `app:payment-methods`, `app:notifications` (optional; no link hides the button) |
| `cta` | string | Button text, e.g. "Start saving" (optional; defaults to "Learn more") |
| `sponsor` | string | Advertiser's name, shown after "Sponsored" (optional) |
| `order` | number | Lower numbers come first (optional) |
| `startsAt` / `endsAt` | number | Start and end time in milliseconds since 1970 (optional; leave out to run until switched off) |

`views` and `clicks` fill in by themselves. Up to 10 running adverts are shown. Only `https` pictures and links are accepted; anything else is ignored.

3. Install the build.

## 11 Oct 2026: P2P sell orders, Merchant Manager and Express tidied

What the app does:

- **Selling into a buy offer:**
  - The price now shows on older offers too. When an offer has no card in Firestore, the price signed into the offer on the network is used, the same fix the buy side got on 7 Oct.
  - The buyer who posted the offer is told at once, in the trade chat: "New order: I am selling you … HARV. It is held in escrow for you…".
- **Express:**
  - The trader whose offer was matched gets the same chat notice.
  - The confirm sheet shows their address shortened instead of the full raw address.
- **Merchant Manager:**
  - Each offer's Orders button says how many orders wait on you ("Orders (3) · 1 need you"), in amber.
  - The order list is in plain words, for example "buyer says paid: confirm and release". Orders that need you are marked ●.
  - Offer details read "You are selling HARV" and the state (Active, Matched, Closed) instead of "Side: SELL / Status: OPEN".
  - The empty "Paused" tab is gone; the network has no pause.
  - Buy offers are tagged green instead of the error red.

What you do: install the build.

**Build note:** every GitHub "Build Android APK" run since at least 7 Oct has stopped after 3 seconds, before any step ran, with no log. GitHub is refusing to start the build machine, which happens when the account's Actions are blocked (a billing problem or a spending limit). Check github.com → Settings → Billing and plans → Actions. Until then, GitHub builds no APK and nothing is compiled there; only CodeAssist on the phone builds the app.

## 11 Oct 2026: banks for international users

What the app does:

- **Payment methods start with the country of the account.** Pick from 44 choices: Nigeria, Ghana, Kenya, South Africa, Uganda, Tanzania, Rwanda, Cameroon, Senegal, Côte d'Ivoire, Egypt, the US, the UK, Canada, 15 European countries, the UAE, Saudi Arabia, Türkiye, Pakistan, India, the Philippines, Indonesia, Malaysia, Singapore, China, Japan, Australia, Mexico, Brazil, or "Other country". The app opens on the phone's own country, then on the last one used.
- **Each country lists its main banks, mobile money and payment apps, with logos.** For example: Chase, Barclays, Monzo, HDFC, N26, Equity, M-Pesa, MTN MoMo, Nubank. PayPal, Wise, Revolut, Payoneer and Skrill appear everywhere. 241 logos ship inside the app. A bank that isn't listed can still be used: type its name and pick "Use …".
- **The account boxes follow the country's own format, and each is checked the way that country's banks check it:**
  - Europe, the Gulf, Türkiye, Egypt and Pakistan: IBAN (checksum and length for that country), plus an optional BIC.
  - UK: sort code and an 8-digit account.
  - US: routing number (ABA checksum) and the account.
  - Canada: institution, transit and the account.
  - Australia: BSB and the account.
  - India: IFSC and the account.
  - Mexico: CLABE (check digit).
  - Brazil: Pix key.
  - Nigeria: the 10-digit NUBAN, as before.
  - Elsewhere: the account number with an optional branch code and SWIFT.
  - Mobile money asks for a phone number; payment apps ask for an e-mail, phone or @tag.
- **Bank filled in for you:**
  - A German, Austrian, Swiss, Dutch, Belgian, Luxembourg or Liechtenstein IBAN fills in the bank's name and BIC.
  - An Indian IFSC fills in the bank and branch.
  - Only the IBAN or IFSC is sent to the lookup, never the account holder's name.
- **On your P2P offer**, all the details go together in one line, for example "Routing 021000021 · Acct 123456789" or "DE89 3704 0044 0532 0130 00 · BIC COBADEFFXXX". The buyer sees exactly what to pay into.
- **Methods saved earlier** keep working and are treated as Nigerian.

What you do: pull the latest code into CodeAssist before building. The `PiClient.java:101` error comes from an old copy of that file; the file on GitHub has been fixed since 7 Oct. Then install the build.

## 8 Oct 2026: the send address matches the coin

What the app does:

- On Send, the address box shows the shape of the coin you picked. Pi shows G…, Bitcoin shows tb1… or bc1…, and HARV stays HV0x….
- The box holds 120 characters. It was stopping at 50, which cut off a Pi address.

What you do: install the build, open Send, and pick Pi.

## 8 Oct 2026: explorer page stops unexpected links

What the app does:

- The address placed in an explorer link is encoded, so a strange address cannot change the link into a different kind of address.
- The explorer page only follows http and https links. It does not open a file on the phone.

What you do: install the build.

## 8 Oct 2026: the official HARV coin mark

What the app does:

- **Every HARV coin picture is the round orange mark you supplied.** That includes the asset list, coin page, receive code and receipts. The black corners were removed so it sits on the page as a circle.

What you do: install the build.

## 8 Oct 2026: testnet or mainnet from the wallet list

What the app does:

- **My wallet list has a switch.** On is testnet, which is how the app starts. Off is mainnet. The choice is saved, and it asks before it changes.
- Bitcoin, Litecoin and Dogecoin show a different address on each side. Ethereum, BNB, Polygon, Base, Tron, Solana and Pi keep the same address and talk to the other network.
- **HARV stays on harvannaprotocol.com either way.** That is the only node. The switch says so. A mainnet node has not been added.

- **Pi follows the switch at once.** A payment and a balance check read the Pi server again each time, so turning the switch does not leave Pi on the old network.

What you do: install the build, open My wallet list, and use the switch. Then go back to Assets and pull down so the balances reload.

## 8 Oct 2026: the Gson warning on the Asset page

What the app does:

- **The Asset page no longer uses Gson to read the saved wallet.** It reads that JSON with the parser Android already has, so the build no longer warns that `TypeToken` is missing. The wallet it opens is the same.

What you do: build again. That warning should be gone.

## 8 Oct 2026: pull to refresh on the Asset page

What the app does:

- **Pull down on Assets, Activity or Earn.** The spinner shows, the numbers stay where they are, and the balances, the total and the activity list load again. Nothing is cleared first.

What you do: install the build and pull down on the coin list.

## 8 Oct 2026: a coin's about is about the coin

What the app does:

- **The About line describes the coin itself**: what it is, how its smallest unit works, and what pays the fee.
- It no longer says the address comes from the same recovery phrase. The Network page shows the network, the standard, the price, the decimals, the smallest unit and which coin pays the fee.

What you do: install the build and open a coin.

## 8 Oct 2026: coin and staking use a material tab

What the app does:

- **A coin's Overview, History and Network sit on a material tab** and you can still swipe between them.
- **Staking and My Positions do the same.** Each one is its own page. Live and Closed on My Positions use the same kind of tab.

What you do: install the build, open a coin, then open Staking.

## 8 Oct 2026: a receipt matches the coin

What the app does:

- **The receipt uses that coin's own words.** Bitcoin shows the transaction, the block, the confirmations, the size and how many inputs and outputs it has. Ethereum, BNB, Polygon and Base show the gas fee, the gas used, the gas price and the nonce. Tron shows energy and bandwidth. Solana shows the signature and the slot. Pi shows the ledger and the memo, when there is one.
- Harvana's burn line is only on a Harvana receipt.

What you do: install the build and open a transaction for each coin.

## 8 Oct 2026: a transaction stays on its receipt

What the app does:

- **Opening a transaction no longer leaves the app.** The receipt reads that coin's explorer and shows the record here: who sent it, who received it, the amount, the fee, the block and the explorer's own JSON.
- Tap the JSON to copy it. Nothing opens a website.

What you do: install the build, open a coin, then History, then a transaction.

## 8 Oct 2026: the other coins are on testnet

What the app does:

- **Every coin except HARV now uses a test network**, so a payment spends test coins, not real money. HARV stays on Harvana's own node.
- **Bitcoin, Litecoin and Dogecoin show a new testnet address** (it starts with tb1, tltc1 or n). The old live address is not used. Ethereum, BNB, Polygon, Base, Tron, Solana and Pi keep the same address, but the app talks to the test network.
- **A payment you send is kept in History** even before the explorer lists it.

What you do: install the build, open each coin, copy the address, and ask that network's public faucet for test coins.

| Coin | Network | Address looks like |
|---|---|---|
| Bitcoin | testnet4 | tb1… |
| Litecoin | testnet | tltc1… |
| Dogecoin | testnet | n… |
| Ethereum | Sepolia | 0x… (same as before) |
| BNB | BNB testnet | 0x… (same as before) |
| Polygon | Amoy | 0x… (same as before) |
| Base | Base Sepolia | 0x… (same as before) |
| Tron | Nile | T… (same as before) |
| Solana | devnet | the same address |
| Pi | Pi testnet | G… (same as before) |

Also fixed a build error on the Assets list where a balance refresh used the same name twice.

## 8 Oct 2026: a receipt reads that coin's explorer

What the app does:

- **History for every coin comes from that coin's own public explorer.** Bitcoin and Litecoin from mempool, Dogecoin from BlockCypher, Ethereum, BNB, Polygon and Base from Blockscout, Tron from Tronscan, Solana from the public network, Pi from Pi's own network. No paid key.
- **Opening a transaction fills the receipt from that explorer**: who sent it, who received it, the amount, the fee, the block and how many confirmations. The network line still opens the explorer page.

What you do: install the build, open a coin, then History, then a transaction.

## 8 Oct 2026: the scanner reads every coin's code

What the app does:

- **Scanning a code opens the right coin.** A Bitcoin, Litecoin, Dogecoin, Ethereum, BNB, Polygon, Base, Tron, Solana or Pi code fills the address and selects that coin. A Harvana code still opens HARV.
- **Each receive code carries that coin**, and the picture in the middle is that coin's logo. Copy and share still give the plain address.

What you do: install the build, open Receive for a coin, and scan that code from Send.

## 8 Oct 2026: the total adds every coin, in the currency you pick

What the app does:

- **The big number is everything you hold**, not only HARV. Each coin's row shows what that balance is worth. A coin you do not hold shows zero, not the price of one coin.
- **Tap the total** to choose naira, dollars, pounds, euro, cedi, shilling or rand. Each choice shows what one dollar is worth in that currency. The same choice in Settings still works.
- **The rates are fetched once and kept for twelve hours**, so the phone is not asking for a price on every row.

What you do: install the build, open Assets, and tap the total.

## 8 Oct 2026: send and receive checked before a real payment

What the app does:

- **Change comes back to your own address.** Bitcoin, Litecoin and Dogecoin were checked: the address the phrase opens, decoded again, is the same place the change is paid. A Bitcoin payment is a real SegWit transaction, and its signature matches the published BIP-143 test.
- **The fee is measured from the transaction that will actually be sent**, then the change is reduced so the network will accept it. An amount that is too small for that coin is refused before anything is signed.
- **Ethereum, BNB, Polygon and Base** add a little to the gas price so the payment is not left sitting. Solana refuses a payment that would not cover the network fee, and tells you when a brand-new Solana account needs at least 0.00089088 SOL.
- **Receive.** Copy and share always use the plain address. The QR for Bitcoin, Litecoin and Dogecoin also carries the coin name, so another wallet opens the right coin. Scanning such a code into Send strips that prefix. Pi still refuses to sign if the address on screen is not the one this recovery phrase opens.

What you do: install the build. Send a small amount first.

## 8 Oct 2026: send every coin, not only HARV and Pi

What the app does:

- **Send works for every coin in the wallet.** Bitcoin, Litecoin, Dogecoin, Ethereum, BNB, Polygon, Base, Tron and Solana, as well as HARV and Pi. The phone builds the payment, signs it with the recovery phrase, and hands it to that coin's own network. The key is not sent anywhere.
- **The fee is the network's, not Harvana's.** You review who it is going to and confirm with your PIN before anything is signed.
- **Checked on this machine, not on a live payment:** the Bitcoin signature matches the published test in BIP-143, and a Solana transfer signature verifies. A real payment still depends on the network accepting it.

What you do: install the build. Send a small amount first. The address you pay must be that coin's own kind of address (a Bitcoin address for BTC, an 0x address for ETH, BNB, Polygon and Base, and so on).

## 8 Oct 2026: one Pi address, and Pi can be sent

What the app does:

- **Pi has one address.** Receive was showing the Pi address saved for this wallet. The coin page was still opening with an older address it had kept from the first time the row was drawn. The coin page, Receive, the QR and Send now all read the saved address at the moment you open them.
- **The Receive QR matches the coin.** It no longer keeps the HARV code and the HARV logo while the text says Pi. The line under the code names the coin ("Scan to send PI to this address").
- **Sending Pi is on.** Send, choose Pi, enter a Pi address (it starts with G) and an amount. You confirm with your PIN, and the phone signs the payment and gives it to the Pi network. The other coins can still be received. Sending them from Harvana is not switched on, because each of those networks needs its own signed transaction.

What you do: install the build. Open Pi again. The address on the coin page and on Receive must be the same, the one Receive already showed.

## 8 Oct 2026: choosing a coin is its own screen

What the app does:

- **Send and Receive no longer open the coin list as a dropdown.** Tapping the coin opens a full screen, Choose asset, with the coin logo, name, network and ticker.
- **Search** matches the name, the ticker or the network (for example "doge", "pi" or "base").
- The coin that is already selected is marked. Picking one returns to Send or Receive with that coin's address and QR.

What you do: install the build.

## 8 Oct 2026: one page per coin, shared prices, Harvana explorer link

What the app does:

- **Every chain is its own asset.** Bitcoin, Litecoin, Dogecoin, Ethereum, BNB, Polygon, Base, Tron, Solana and Pi each have a row on Assets, the same shape as HARV, with that coin's logo. The old Crypto Wallets screen is gone. Tapping a row opens the coin page with that coin's name, logo, address and description. Send and Receive follow the coin that was opened. HARV uses the same coin logo everywhere, not a letter in a circle.
- **The coin page is three pages you can swipe:** Overview, History, Network. Tapping a tab does the same thing.
  - Overview names that coin. Pi says "About Pi Network", not Harvana.
  - If the account is not active yet, the big number is `0` plus the ticker, and the reason ("Not active yet") sits underneath. It no longer replaces the balance.
  - History lists only that coin. Pi no longer shows HARV transfers. Pi, Bitcoin and Litecoin load their own public history. The others say there are no transactions yet, because their explorers need a paid key (the 7 Oct note that said the coin page's History tab stays HARV only is out of date).
  - Network shows that coin's own facts. A long address sits on its own line under Address, so the label is not crushed into a vertical stack.
- **A free market price** shows for every coin except HARV (no HARV price is invented). The phone reads one shared copy at `MARKET_PRICES/spot`. If that copy is under 10 minutes old, it is used and the price site is not called. If it is older, one signed-in phone fetches the new prices and writes them for the others. A phone cannot overwrite a fresh copy.
- **Explorer.** On a HARV coin, Explorer opens `https://harvanna.com/explorer/#/address/{address}`. The other coins still open their own public explorers. The page itself is served by the node (`/explorer/`). That jar is not on the live server yet, and `harvanna.com` does not resolve yet. Both are in the node repository's `docs/DEPLOYMENT.md` for 8 Oct 2026.
- **Learn Hub.** A new article is signed by the wallet that is signed in, not only the wallet bound to the account.
- **Receipt.** The transaction screen is `ReceiptActivity`, the bar says Receipt, a pending transaction uses the clock icon, and a confirmed one uses the same icon as the wallet's Activity list.
- **Deleted groups no longer raise the count.** A group the node answers 404 for is taken off the profile instead of being counted as a group that still exists.
- **Learn on Home** shows at most two rows, in the same card style. The "counted as a read / trust score" line is gone from a Learn post.
- **Fixed:** the build failed in Pi history (`ChainHistory.java`), which still read a field that had been removed.

What you do:

1. Install this build.
2. Publish `docs/firestore.rules` from the Firebase console. Until `MARKET_PRICES` is allowed, the shared price write is rejected and each phone fetches its own price. Nothing else in the app waits on that.
3. The explorer button stays dark until the node jar from 8 Oct is deployed and `harvanna.com` points at the same server. Those steps are in the node repository, not here.

## 7 Oct 2026: wallets on Android 6 and 7; pasted phrases open the right wallet

These are fixes the miner app already had, now in Harvana too.

What the app does:

- **Android 6 and 7 can create and restore wallets.** The app used the phone's "PBKDF2WithHmacSHA512" function, which only exists from Android 8. On older phones, wallet creation, restore, the backup card, the transaction PIN and the Pi and other-coin wallets failed. These now use the app's own implementation, built on HmacSHA512. It gives exactly the same bytes, checked against the platform function, so every existing wallet, PIN and backup card stays valid.
- **A phrase pasted with a double space, a line break or capitals opens the right wallet.** Before, the words passed the check, but the wallet was worked out from the raw text and a different, empty wallet opened. The phrase is now reduced to single spaces and lower case before use, and saved that way.

What you do: install the build. Anyone who restored a wallet from a pasted phrase and found it empty should restore it again; the right wallet now opens.

## 7 Oct 2026: bank directory with logos; Pi build error fixed

What the app does:

- **Choosing a bank.** Adding a bank account opens a searchable list of every Nigerian bank, microfinance bank and payment app (284 of them) with logos. OPay, Moniepoint, GTBank, Access, Zenith, First Bank, UBA and the other common ones come first. Each row shows the official name, the type of institution and the bank's USSD code.
- **Typed names are recognised.** "gtb", "GTBank" or "058" all become Guaranty Trust Bank, and the logo appears in the box as you type. The official name is what gets saved and sent with your P2P offer, so buyers see the correct bank.
- **Account number checks.**
  - The number must have the bank's digit count (10).
  - For commercial banks, the CBN NUBAN check digit is tested. A number that fails gets a warning; tap Save again to keep it anyway.
  - Payment apps such as OPay, whose account numbers are phone numbers, are not held to the check digit.
- **Logos everywhere the account shows:** on your saved payment methods, and beside the bank name on the buyer's trade screen.
- **Offline first.** The list and the logos of the 43 most used banks ship inside the app. Other logos load from the public directory, and a coloured badge with the bank's initials stands in when there is no network. The list refreshes itself from nigerianbanks.xyz at most once a week, which picks up newly licensed banks.
- **Fixed:** the build failed in `wallet/PiClient.java` (an unhandled `JSONException` on line 101).

The app cannot look up the account holder's name automatically yet. That needs a payment provider's secret API key (Paystack or Flutterwave), which must sit on the server, never in the app.

What you do: install the build.

## 7 Oct 2026: P2P chat and My trades rebuilt

What the app does:

- **Trade chat:**
  - The chat shows the other trader's name.
  - The long warning is now one short safety card.
  - Messages are grouped under day labels and show the time only.
  - Quick replies sit above the box ("I've sent the payment", "Payment received", "Please check your bank app" and more).
  - The box is a rounded field with a round send button.
- **My trades:**
  - Trades that wait on you come first, with a clear action: "Pay now" when you are the buyer, "Confirm & release" when you are the seller.
  - The whole card opens the trade.
  - The status lines say plainly who has to do what. For example, the buyer reads "The seller's HARV is held in escrow for you. Pay, then tap 'I have paid'".

What you do: install the build.

## 7 Oct 2026: one Activity list for every coin; other-chain transaction details

What the app does:

- **The wallet's Activity tab lists every coin together.** Besides HARV, it shows recent Pi, Bitcoin and Litecoin transactions for the coins turned on under Add coins, newest first. Each of those rows names its network ("Bitcoin · From: …").
- **The coin page's History tab stays HARV only.**
- **Transaction detail for another network** shows the network, the amount, sent or received, the addresses, the transaction hash, the block and the fee where the network reports them, and opens the network's public explorer on tap. HARV-only fields (burn, nonce) are hidden for those.
- Ethereum, Tron, Solana and Dogecoin balances show on the Assets tab; their transaction lists need a paid explorer key, so they are not listed yet.

What you do: install the build.

## 7 Oct 2026: social backup card restyled; 24-word backup code fixed

What the app does:

- **Fixed: new cards printed no backup code.** A wallet with a 24-word phrase has a 145-digit code, but the card only printed a 106-digit one, so the back of every new card had no code. Both lengths now print, in groups of five.
- **Fixed: restoring by typing the code refused 145-digit codes.** Both lengths are accepted.
- **Card back restyled:** "Social backup" title with three plain lines (safe to give to someone you trust, can't be opened without your password, keep the password apart), a larger framed QR with "Scan in Harvana to restore" under it, and the backup code beside it.
- **Card front:** "Valid through: Infinity" is replaced by the month the card was made ("Issued MM/YY"), and the address label is tidied.

What you do: install the build. Anyone who made a card from a 24-word wallet before this build should make a new one, so it carries the printed code.

## 7 Oct 2026: staking dashboard shows APR and APY

What the app does:

- **"APR · APY" replaces "Your share" on the staking dashboard.** Your APR is the network's flexible rate times your stake-weighted lock weight; APY is what that becomes if you claim and restake every day. Your share of the stake still shows above.
- **The stake form shows a live estimate** while you type an amount and pick a lock: the APR and APY you would get, what you would earn in 30 days and in a year, and the APR once the reward pool is full.

The formula (it mirrors the node's daily drip, `docs/STAKING.md` in the node repository):

- yearly drip D = reward pool x 0.05% x 365, scaled by the observed block pace;
- total weight W = the sum of every stake x its lock weight (worked back from the node's `estimatedBaseAprBps`);
- APR of a new stake A at lock weight m = D x m / (W + A x m); your own stake is counted in the pool it shares;
- APY = (1 + APR / 365)^365 - 1;
- full pool: the drip equals the daily staking mint, 1,080 HARV a day.

These are estimates: the rate moves with the pool and with the total staked. No node change is needed.

What you do: install the build.

## 7 Oct 2026: P2P open-trades badge, seller's receiving account

What the app does:

- **The My trades icon on the P2P page shows a red count** of your trades that still need something: waiting for payment, waiting for your confirmation, or in dispute. It refreshes each time you come back to the page.
- **The seller's trade page no longer says "Couldn't load your payment account".** It reads the receiving account from the seller's own offer and shows "Buyer is paying into your … account ending ….".

What you do: install the build.

## 7 Oct 2026: Learn Hub reader shows comments under the post

What the app does:

- **Comments sit under the post** in the full reader, with the count, a comment box and the list. The comment button scrolls down to them. The feed still opens comments in a sheet.
- **The "more" button** shows its two-tone dots instead of a flat grey blob. Its menu now has Copy text, Share, View author's profile, Show less like this, Report post (opens an email to support) and, for your own post, Delete post.

What you do: install the build.

## 7 Oct 2026: notifications in tabs, Discover join button

What the app does:

- **Notifications are split into tabs:** All, Transactions (transfers, payments, loans), Groups (group notices, invitations, membership) and Others (account and social). Each tab shows its unread count. Items are cards with a type icon, grouped under Today, Yesterday and a date, and unread items carry a dot.
- **Discover group cards.** The join bar showed as an empty coloured block because its text was the same colour as the bar. It now reads "Request to join", or "Request pending" when a request is waiting. The info icon opens a short summary of the group.

What you do: install the build.

## 7 Oct 2026: other coins on the Assets tab, P2P icon matches Home

What the app does:

- **Add coins on the Assets tab.** Under HARV there is an "Add coins" button. It opens a list of Pi, Bitcoin, Litecoin, Dogecoin, Ethereum and EVM chains, Tron and Solana, each with an on/off switch. A coin that is on shows under HARV with its address and live balance; tapping it opens the chain wallets page. All coins come from the same recovery phrase. The choice is saved on the phone; all coins are off until the user turns them on.
- **The P2P button on the Assets page uses the same icon as Home's P2P action.**
- The Assets list now scrolls as one page, so the extra coins fit below HARV.

What you do: install the build.

## 7 Oct 2026: coin page in tabs, plain "For" on transactions, trust page and settings tidy-up

What the app does:

- **The coin page has three tabs under the balance:**
  - Overview: the wallet address and what the coin is;
  - History: this wallet's transactions, the same list as the wallet's Activity tab;
  - Network: supply, holders, block height, reward and halving.
- **Transaction detail "For" is in plain words,** such as "Savings contribution · ref 5b2cde66" or "P2P escrow · ref 16667299", instead of the raw network memo. Tapping it still copies the full memo.
- **Trust score.** Only the tier card swipes, and every page is the same height. The Account card, the "You are at Bronze" bar and the "Next badge" card are removed.
- **My Activity is no longer shown.** The app still counts usage in the background for the habit badges.
- **Settings header.** It shows the signed-in name and the real @username (it showed "Guest @@handle"). The menu icons sit on soft category-coloured badges, drawn in that colour, instead of solid blobs.

What you do: install the build.

## 7 Oct 2026: P2P trade notices, chat fix, clearer buyer steps

What the app does:

- **Every step of a trade notifies the other person.** Each step writes a short line into the trade chat, and the other person's phone is notified by the usual chat check:
  - the buyer opens an order ("New order: … HARV");
  - the buyer marks payment sent;
  - the seller confirms and releases;
  - either side opens a dispute.

  The lines also stay in the chat as a record for disputes.
- **Trade chat "Could not send" is fixed.** The first message tried to update a chat that had not been created yet; the chat is now created first.
- **The buyer no longer sees "Escrow funded"** when opening an order (the seller's HARV is what sits in escrow). The step reads "Order opened".
- **"Become an Insured Trader" is gone** from the empty Buy and Sell lists. The button now reads "Post a buy offer" or "Post a sell offer", which is what it always did.

What you do: install the build.

## 7 Oct 2026: version 1.2.0 (code 3): P2P repaired, Pi sending, real check icons

What the app does:

- **P2P listings show their seller, price and trust to every buyer.** These lived only in the private listing record, which the rules let only the seller (or a buyer who had already claimed) read. So everyone else saw "Unavailable", "Trader" and "—", and the trade chat could not start.
  - A new public listing card (`P2P_LISTINGS_PUBLIC`) holds the seller, price, limits, terms and the payment-method names, never account numbers. The bank details stay private.
  - The price also falls back to the one the node signs into each listing.
  - Listings created before this build still show the node's price, but not the seller's name. Relist them to show it.
- **Sellers can finish a trade.** After the buyer marks "paid", the seller now sees "I have independently verified the exact payment" and "Confirm Payment Received". Both had been hidden in the layout and never shown.
- **Trade progress** keeps each step's label, with a real check icon (filled when done, an open circle when not), on the Buy and Sell trade screens.
- **Real check icons everywhere.** Text ticks ("✓") across P2P rows, payment lists, milestones, the Learn video note and transaction details are replaced by the app's check icon.
- **The Buy order screen:**
  - the 25% / 50% / 75% / Max buttons are readable (their text was the colour of their background);
  - "How much HARV do you want?" lost its stray backslash, along with 13 other layouts.
- **Pi sending.** Crypto wallets → Pi now has Send Pi (address, amount, optional note; reviewed, then PIN or fingerprint) and History. The transaction is built and signed on the phone. It matches the official Stellar SDK byte for byte (`tools/pi`), and is sent to Pi's own Horizon server.

What you do:

1. Publish `docs/firestore.console.rules`. It adds `P2P_LISTINGS_PUBLIC`.
2. Install the build.
3. Relist your open P2P offers, so their public card exists.

## 7 Oct 2026: Crypto wallets — Bitcoin, Ethereum and more from the same words; the phrase as one text

What the app does:

- **Crypto wallets** (side menu) lists the wallets your recovery phrase also opens, each with its address, Copy, a QR code to receive, and its balance:
  - Pi Network (24-word wallets);
  - Bitcoin (bc1...);
  - Ethereum, which is the same address on BNB Chain, Base and Polygon;
  - Tron (TRX and USDT);
  - Solana;
  - Litecoin;
  - Dogecoin.

  They use the paths standard wallets use, so the same words open them in MetaMask, Trust Wallet, Phantom, TronLink and Electrum.
- **Only public addresses are stored.** Keys are worked out from the phrase on the phone when needed.
- **Balances are read from free public services:** mempool.space, litecoinspace.org, BlockCypher, publicnode.com RPCs, TronGrid, Solana's public RPC and Pi's Horizon. Nothing runs on a Harvana server, so it costs nothing to run.
- **Receiving works now. Sending from these chains comes next.**
- **The recovery phrase is shown as one block of text,** words separated by spaces, on Create wallet and Backup & recovery. Restore, including the group-wallet restore, takes it in one box.

Checked off the phone: every chain's address for a 12-word and a 24-word phrase matched independent reference wallets (eth-account, embit and PyNaCl), including the published test addresses (`tools/chainkeys`).

What you do: install the new build, open Crypto wallets, and compare one address with MetaMask or Trust Wallet loaded with the same test words.

## 7 Oct 2026: 24-word recovery phrase; the same words open your Pi wallet

What the app does:

- **New wallets get 24 words** (256-bit, standard BIP-39) instead of 12. Create wallet shows them three to a row. So does Backup & recovery, which now also shows the Pi address the words open, with a copy button.
- **The same 24 words also give a Pi Network wallet.** It is derived the way Pi's own wallet does it: Stellar's SEP-0005 method on path m/44'/314159'/0'. So typing these words into Pi's app opens the same Pi wallet, and a Pi passphrase restored into Harvana shows that Pi wallet and gives a HARV wallet as well. Only the Pi address ("G...") is stored; the Pi key is worked out from the phrase when needed.
- **Your HARV address does not change.** HARV keys come from the phrase exactly as before, so every existing wallet keeps its address.
- **Older 12-word wallets keep working:** restore, cards and sending all work as before. They have no Pi wallet, because Pi's own app uses 24 words.
- **Restore accepts any standard length** (12, 15, 18, 21 or 24 words), checked against the BIP-39 checksum.
- **The wallet access card holds 24-word wallets too.** The QR stays one line, and the printed digit code is 145 digits instead of 106. Old 106-digit cards still restore.

Checked off the phone:

- the word logic against the official BIP-39 test vectors (12 and 24 words, including a wrong checksum);
- the Pi address method against the three published SEP-0005 vectors, all matching (`tools/PiKeysCheck.java`).

What you do: install the new build, create a new wallet, and copy its 24 words into Pi's app to check that the Pi address matches the one Backup & recovery shows. Please do this with a test wallet first.

## 7 Oct 2026: share a profile as a picture and a message

What the app does. Share on a profile, and Share on the My QR code screen, now send two things together:

- **A profile card picture:** the name, @username, trust tier and the profile QR code (scanning it in Harvana offers "Add friend").
- **A message:** for example "Valentine (@doktohsmile) on Harvana, trust 19% Bronze. Scan the QR code in the Harvana app to add me, or search @doktohsmile."

Apps that take only one of the two still get something useful. Without a username there is no QR, so only the message is sent. What you do: install the new build.

## 7 Oct 2026: tier medals

What the app does. Each trust tier has a 3D medal:

- New: a green sprout;
- Bronze: one star;
- Silver: two stars;
- Gold: three stars;
- Platinum: a diamond.

The tier chip on a profile ("19% · Bronze") and the small trust pills elsewhere show the medal. Tapping one opens a dialog with every tier. The tiers you have reached are in colour; the rest are grey and faded, with what each one means and the score it starts at. The top of the dialog shows how many points to the next tier.

The medal sources are in `tools/illustrations/tier_*.svg`. What you do: install the new build.

## 7 Oct 2026: no "BVN/NIN verified" anywhere

What the app does. The profile's personal details no longer show a "Verification" row ("BVN verified" or "NIN verified"). Settings no longer shows a "Verified Member" badge. Both came from an ID choice made at sign-up that was never checked, and sign-up no longer saves it. What you do: install the new build.

## 7 Oct 2026: Home opens with your money

What the app does. On Home, the wallet card (balance with Send, Receive and P2P) now sits right under the greeting. The group numbers, upcoming payments, Learn Hub and trending groups follow. Before, the balance was below the stats and the payments list. What you do: install the new build.

## 7 Oct 2026: one scale for corners and text

What the app does. Across 90 screens:

- **Card corners** use three sizes: 12dp for small cards, 16dp for normal cards and 20dp for large panels. Pills and circles are unchanged. Before, there were 20 different radii.
- **Button corners** are 12dp.
- **Text** follows one scale. Half sizes such as 13.5sp moved by half a point. Text below 10sp, which was hard to read, is now 10sp. Odd headings snapped to 18, 22 or 28sp.

What you do: install the new build. Tell me any screen where a line now wraps.

## 7 Oct 2026: plain words

What the app does. Messages people see no longer use engine words:

- "node" now reads "network";
- "on-chain" and "blockchain" now read "on the network";
- nonce errors now read "Couldn't reach the network";
- "waiting for Firestore" now reads "loading".

The phrasing changed in 34 places. "Escrow" stays, because it is the usual word in P2P trading. What you do: install the new build.

## 7 Oct 2026: one switch, one segmented choice, one toolbar

What the app does:

- **Switches** all look the same: an accent knob on a soft accent track when on, a white (light grey at night) knob on a grey track when off. Four switches in Create group had their knob painted green, so they looked on even when off; that is fixed.
- **Segmented choices** (Fixed or Floating price on the P2P offer screens) fill the chosen side with the accent colour. Before, the selected side barely stood out.
- **Toolbars** all use the standard header's look: the same bold 20sp title, 13sp subtitle and spacing.

What you do: install the new build.

## 7 Oct 2026: P2P payment proof and the network price

What the app does:

- **Payment proof.** On a buy trade, picking a picture of the bank transfer now uploads it and sends it to the seller as a picture in the trade chat. Until now it was only previewed on the buyer's phone.
- **The seller sees it.** The seller's trade screen shows the latest proof; tap it to open it full size. It stays with the chat for the dispute window. A picture is not the payment, so the seller still checks their own account before releasing.
- **Market price.** The P2P price reference now uses the network's own price of HARV (new node endpoint `/api/v1/p2p/mark`) once it is live. Until then it uses the median of open listings, as before. The screen says which one it is.

What you do, in this order:

1. Deploy the node jar.
2. Publish `docs/firestore.console.rules`. Chat messages may now carry an `image` address. Without the new rules, a proof is refused.
3. Install the new build.

## 7 Oct 2026: illustrated group contract

What the app does. Each page of the group contract opens with a picture of what the page is about:

- the group together;
- members around the pot;
- coins going into a shared jar;
- one member lending to another;
- hands raised beside a ballot box;
- a shield over the money;
- chat bubbles;
- a member waving at an open door;
- an open book of words;
- a goal flag;
- coins passing round a circle;
- savings jars growing;
- a notebook and calculator;
- a pen signing the contract.

They share one warm, textured style and a palette taken from the app. The source that draws them is in `tools/illustrations`, so new pictures will match. What you do: install the new build.

## 7 Oct 2026: one QR design across the app

What the app does. Every QR code the app makes now looks the same: deep-teal rounded squares on white, teal corner eyes, and the HARV coin in the middle. That covers the receive address, the coin page, profile codes, receipts and the wallet access card. Error correction stays at the highest level, and the coin covers well under the share it can recover.

Checked off the phone: a faithful copy of the design decodes with a standard QR reader for addresses, profile links, receipt links and a long access-card payload, at 300, 512 and 720px.

The receipt QR is larger (96 points instead of 72) and drawn sharp. The old one was shrunk to about 1.5px per square, too small for a camera.

What you do: install the new build, then scan one of each code with the app and with another phone's camera.

## 7 Oct 2026: audit fixes in the rules (publish the rules)

What was wrong. The app writes to three places the published rules had no entry for, so Firestore refused those writes:

1. **Changing a username failed** for anyone who already had one. The change also writes a 90-day hold on the old name to `USERNAME_HOLDS`, and that refusal sank the whole transaction.
2. **Your activity log never saved** (`USERS/{uid}/ACTIVITY`).
3. **The IDENTITY address-to-account card never saved.** Nothing reads it, and it would have shown publicly which wallets belong to whom.

What changed:

- `USERNAME_HOLDS` has a rule. Only the name's owner can place the hold, at server time, and the hold is checked when anyone claims a name. A released name stays reserved for its old owner for 90 days.
- `USERS/{uid}/ACTIVITY` has a rule: only the owner can read or add to it.
- The app no longer writes `IDENTITY`.

What you do:

1. Publish `docs/firestore.console.rules`.
2. Install the new build.

## 7 Oct 2026: shorter Settings, real icons in place of placeholders

What the app does:

- **Settings is about half as long.** Rows are tighter and show their title only.
- **Seven Settings rows are hidden:**
  - Account information (same as Edit profile);
  - Groups & circles (it is the Groups tab);
  - Transaction history (it is on the wallet);
  - Terms (it is inside About);
  - Language (English only for now);
  - Limits & fees and Tax reports (not built yet).
- **Group settings** used a placeholder picture for each setting. Each setting now has its own icon: approval, invite, contribution, pool, grace period and commission. The group type badge uses the circle, goal or savings icon.
- **P2P avatars** show the person icon instead of the placeholder picture.

What you do: install the new build.

## 7 Oct 2026: 3D HARV coin

What the app does. The HARV coin (`ic_harv_coin`) is now drawn in 3D. It keeps the same coral-to-gold colours and the white H, and adds a raised rim, a visible coin edge, an embossed H and a soft gloss. It is a 512px PNG in `drawable-nodpi`. The earlier 2142px image sat in `drawable`, where Android scales it up by screen density; on large phones that could use a lot of memory. What you do: install the new build.

## 7 Oct 2026: back arrows, password eyes, friend codes, no NIN/BVN

What the app does:

- **Back arrows.** Every screen that uses the shared top bar shows its back arrow again. The arrow had been captured as an image before its fade-in, so it was invisible.
- **Password eyes.** Password fields use Harvana's own eye: open while the text shows, crossed while it is hidden. These fields now have a show/hide eye too:
  - the card password;
  - the 12-word recovery phrase on Restore;
  - the Access Card passphrase in group settings.
- **Missing icons.** The eye icons and about 90 other icons were stored only for one screen density. They now live in the shared folder, so every phone finds them.
- **Friend codes.** Scanning someone's username QR offers "Add friend" (or "View profile"). After adding, it opens their profile.
- **No NIN/BVN.** Sign-up no longer asks for NIN or BVN.
- **Debug button gone.** The debug label and the Guardian Console button are gone from Security settings.

What you do: install the new build.

## 7 Oct 2026: the app's own icons where emoji and symbols stood

What the app does. Seven new duotone icons in the house style, built from Fluent UI System Icons (MIT) like the rest: calendar, verified, star, warning, arrow up, cross in a circle, and an open circle. They now appear in these places:

- the group settings frequency chip (was 📅);
- the "What this person can do" list (was ✅ and ❌);
- "Verified Member" in Settings and "Verified" on Home (was ✓);
- the P2P rating (was ★);
- the loan estimate warning (was ⚠);
- the Learn "new posts" pill (was ↑);
- the payment confirmation sheet, which shows the send icon (was ↗).

Decorative sparkles and step dots stay as they are. What you do: install the new build.

## 7 Oct 2026: shorter profile and trust pages, a "My data" tab

What the app does:

- Profiles show only the milestones a member has reached, in one row that scrolls sideways. Tap a badge to flick through them full size. On your own profile and on the trust score page, "See all" opens the full shelf, with what is still to earn.
- Your own profile has a new **My data** tab with:
  - your network facts (device, vouches, established);
  - your linked wallets;
  - links to My activity, Devices and the full trust score;
  - a note on how deleted data is kept.

  The Trust and Activity tabs are shorter as a result.
- On the trust score page, badges show as one line of names, with "What these mean" for the explanations.

What you do: install the new build.

## 7 Oct 2026: one person, many wallets

What the app does. Signing in with another wallet no longer moves your account onto it. The new wallet is added to you, signed by its own key, and your first wallet stays your main one, which keeps your profile, history and gifts. The network counts every wallet's activity toward you: trust and badges are worked out across all your wallets, and a default on a new wallet lowers you, not just that wallet. A wallet can belong to one person, and a phone to one person; each of your own wallets can be bound on your phone. Your own profile lists your wallets ("Main wallet", "Also yours"). Nobody else can see that list.

What you do:

1. Deploy the node jar first (node repository, `docs/DEPLOYMENT.md`, "one person, many wallets").
2. Install the new build.

Without the new node, a second wallet shows "The network is still on the one-wallet rule".

## 7 Oct 2026: deleted data is kept for a year, then erased

What the app does. Deleting a group, a Learn post, a profile article or an expired trade chat still removes it from every screen at once. Before the delete, the app now writes a private copy to `DELETED`, with `purgeAt` set one year later. Nobody can read these copies from the app. The delete prompts now say this. The full design is in `docs/DATA_RETENTION.md`.

What you do, in this order:

1. Publish `docs/firestore.console.rules`, which adds the new `DELETED` block. Without it, every delete fails.
2. In the Firestore console, under Time-to-live, create a policy with collection group `DELETED` and timestamp field `purgeAt`. With gcloud: `gcloud firestore fields ttls update purgeAt --collection-group=DELETED --enable-ttl`.
3. Install the new build.

## 7 Oct 2026: one scanner for every Harvana QR code

What the app does. The scan button in the top bar now reads every code the app makes and opens it in the right place: a wallet address opens Send with the recipient filled in (scanning an address used to open Send empty), a wallet access card opens Restore and asks for the card password to add the wallet, a profile code opens that member's profile, and a receipt is checked against the network ("Genuine: confirmed on the network" with block, amount, sender and recipient; "Sent, not confirmed yet"; or "Not found on the network", which means the receipt may not be genuine). Anything else is shown as text you can copy. Codes now scan reliably: the app used to draw most of its codes as emerald on dark navy (a light-on-dark code most camera scanners cannot read); every code is now dark on a light ground with a wider quiet border. New receipts carry harvana://tx/<hash>; older receipts with a bare hash still scan. Picking a QR image from the gallery also tries harder (inverted and unevenly lit photos). What you do: install the new build.

## 7 Oct 2026: milestone awards shelf

What the app does. The activity list on Trust score is replaced by a shelf of milestone awards: 33 badges in six families (Saving; Groups and leadership; Trading, loans and giving; Learning and writing; Standing; Your habits), each a colourful 3D badge on a light card coloured by its tier (bronze, silver, gold; mint for one-off awards; grey while locked). Tap any badge to open the full award and flick left or right through the whole shelf. Where the counts come from: network badges (Heavy Saver, Team Leader, Group Governor, Trusted Borrower and others) are counted by the node from confirmed activity; writer badges (Master Writer for posts with 5 or more handshakes, Storyteller, Crowd Favourite, Conversation Starter, Widely Read) are counted from the Learn Hub documents themselves; "Your habits" (Regular, Explorer) are counted on the phone and shown only to you, never in the trust score. The shelf is on Trust score, on your profile, on My activity, and on other members' profiles (earned badges only). What you do: deploy the node jar (milestones endpoint), publish `docs/firestore.console.rules` (the profile `milestones` field is node-only), then install the new build. Until the node is updated the network badges show as not yet earned.

## 6 Oct 2026: deleting a group removes it from Firestore completely

What the app does. Deleting a group (from group settings, after a goal group is closed and refunded, or when the app clears a group that no longer exists on the network) now removes, in order: every invitation to the group (so invitees no longer see it), the group and all of its sub-collections (members, notices, payments, loans, join and payout requests, private), the admin's profile entry, and the phone's offline copies of the group, its members and loans, and the Discover list. Records the node keeps for network groups (trust and mirrors) are the node's and stay. What you do: publish `docs/firestore.console.rules` in the Firebase console (the group's admin may now read and delete its invitations), then install the new build. Until the rules are published, invitations sent by other members to a deleted group are left in place; everything else is removed.

## 6 Oct 2026: group wallet icon without a background

What the app does. On a group's Overview, the "Group wallet" icon no longer sits on a filled square: it is the plain duotone wallet icon in the accent colour, like the icons in the cards above it. What you do: install the new build.

## 6 Oct 2026: payment sheet shows pending honestly, and no more "HARV HARV"

What the app does. The payment sheet no longer shows a success tick while a payment is still waiting for the network. Pending: an amber clock that gently pulses, with "Confirming on the network…". Confirmed: it turns into a green check in a circle that pops in. Both are in the app's own duotone icon style with no background (the old tick was a text character on a green disc). Amounts no longer read "10 HARV HARV" on the payment sheet, the members list, the express marketplace and loan reminders (the amount formatter already adds HARV). One payment can no longer open a second success sheet. What you do: install the new build.

## 6 Oct 2026: notification checks read only what is new

What the app does. The notification check (every 90 seconds while the app is open, every 15 minutes in the background) used to read every notice ever posted in each of your groups. It now reads only notices newer than the last one seen, at most 100 per group, and pinch checks only read pinches since the last look. Same notifications, a small fraction of the Firestore reads (about 320 to about 20 per check for a member in 3 busy groups). Firestore creates the single-field index this needs on its own. What you do: install the new build. Details: node repository `docs/PLATFORM_SCALE_TEST.md`.

## 6 Oct 2026: icons fill in 3D when tapped

What the app does. Every icon now has two looks: the duotone line at rest, and a filled version shaded for depth (lit from the top, soft shadow) in one single colour, the icon's own colour or the colour the screen gives it. The icon fades into the filled look while it is pressed, and stays filled while it is selected or checked (the current bottom-bar tab, toggles, selected chips). Icons inside a tappable row fill when the row is pressed. Icons that have their own tap action keep their own pressed state. What you do: install the new build.

## 6 Oct 2026: new icons across the whole app

What the app does. Every icon in the app (163 of them: bottom bar, buttons, menus, transaction types, profile, wallet, groups, Learn Hub, notifications) is redrawn in one Harvana duotone style: a clean line over a soft gold fill. Where a screen colours an icon, both tones take that colour, so the two-tone look stays. Android's built-in system icons that were still used (info, alert, search, gallery, camera, send, more, and the notification icon) are replaced by the same set, and the text characters used as icons (✕ close, ⋮ menu, › chevrons) are now real icons. Kept as they are: the HARV coin and logo, the launcher icon and the 3D handshake reaction. What you do: install the new build.

## 6 Oct 2026: handshake reaction on Learn posts

What the app does. The fire reaction on Learn posts is now a handshake: the 3D 🤝 handshake (yellow, as in Fluent Emoji) with a transparent background (Microsoft Fluent Emoji, MIT, see docs/THIRD_PARTY_NOTICES.md), meaning you agree with the author. Before you react it shows in grey (same 3D shape, no colour); when you tap it, it pops and turns yellow. Tap again to take it back. Counts and who reacted are unchanged (same records as before). What you do: install the new build.

## 6 Oct 2026: close a goal group nobody else joined and get your money back

What the app does. In group settings, "Check if I can close this group" on a goal group where you are the admin and nobody else has joined now offers "Refund and delete": the network closes the group, sends everything in its wallet back to your wallet (small network fee), and the group is removed from the app. If anyone else has put money in, the network refuses. What you do: deploy the new node jar first (capital-oracle-projects `docs/DEPLOYMENT.md`, "close a goal group nobody else joined"), then install the new build.

## 6 Oct 2026: a new goal stays in the same group

What the app does. "Start a new goal" no longer creates a new group. The admin of a goal group that has reached its goal (and, for pay-out-in-one rules, been paid out) enters a name and target, and the same group starts a new goal with its own ledger: same members, rule and wallet, nobody joins again. Progress, the Overview and the board show the current goal; the running total and past goals are kept. A new Goals screen (the "Goals" button at the foot of a goal group, or "See all goals" on Actions & votes) shows the current goal with its progress, then every finished goal newest first: raised against target, paid out or kept, start and close dates, and what you put into each goal, plus totals across all goals. What you do: the node needs the new jar first (capital-oracle-projects `docs/DEPLOYMENT.md`, "next goal inside the same group"), then install the new build. Until the node is updated the button will report an error from the network.

## 6 Oct 2026: bigger app name in the top bar

What the app does. The "Harvana" name next to your photo in the top bar is larger (24sp, was 18sp). What you do: install the new build.

## 6 Oct 2026: bottom sheets reach the bottom of the screen

What the app does. Every bottom sheet (all of them go through the shared sheet base) now paints the system navigation strip under it in the sheet's own colour, with light or dark navigation icons to match, so a sheet no longer ends above a darker band at the bottom of the screen. What you do: install the new build.

## 6 Oct 2026: one button look across the app

What the app does. Every Material button now defaults to the same look, in bottom sheets and on screens alike: 12dp corners, 48dp tall, bold sentence-case text, filled accent for the main action and an outlined accent edge for the others. Buttons that set their own colours or shape in their layout keep those; the rest, including the cramped "Log out" and "Confirm" buttons on the sheets, now match. Install the build from the latest push: the "Log out" sheet in your screenshot is from the build before the sheet buttons were restyled. What you do: install the new build.

## 6 Oct 2026: trending groups button shows Pending approval

What the app does. On Home, "Trending Public Groups": the button on each group says "Pending approval" (dimmed) when you have already sent a request to join it, and "Join" when you have not (it used to say "View" for both). Tapping the card still opens the group's page. What you do: install the new build.

## 6 Oct 2026: bottom sheets instead of dialogs, everywhere

What the app does. The 78 places that opened a pop-up dialog (confirmations, "Deposit amount", PIN entry, pickers, "type CLOSE to confirm", and so on) now open a Material bottom sheet that rises from the bottom, with a small grab bar and rounded top corners, and keeps the same buttons. Every bottom sheet that was already in the app (more than a hundred) now goes through one shared base (`AppSheet`) so they all have the same 24dp rounded corners, open fully, and shrink above the keyboard. Every sheet's own surface now gets its rounded top corners put back after each layout, and everything inside it is clipped to the curve, so no sheet is left with flat edges whatever it paints or whatever Material does to its background. The buttons on the alert-style sheets (Confirm, Cancel and the rest) now match the rest of the app: filled accent for the main action, outlined accent for the others, equal rows. A sheet with a text field puts the cursor in the first field and brings the keyboard up, and the field and the buttons stay above the keyboard so you can see and edit what you type. Square-backed content is clipped to the curve. The "Working..." progress pop-ups are now sheets too (`ProgressSheet`): a spinner and one line of text that cannot be swiped away. The date of birth picker on Complete profile is a sheet as well (`DateSheet`, a date wheel and a Set date button), and no pop-up dialog is left in the app. What you do: install the new build.

## 6 Oct 2026: group tabs fill the width when there is no Loans tab

What the app does. On a group's page, a group without loans has five tabs (Overview, Members, Payments, Notice, History) and they now share the full width evenly instead of sitting at the left with History half off screen. A group with loans keeps the six scrolling tabs. What you do: install the new build.

## 6 Oct 2026: spinner text and lists follow the theme everywhere

What the app does. Every drop-down picker in the app (Location country, state and city on group creation and profile, the Learn category, asset pickers, sort, frequency, visibility and the rest) now shows its selected value and its list in the palette's text colour on the surface colour, so it is readable in both the light and dark theme. Before, the value could come out dark grey on the dark page, which made the Location pickers on "New goal group" look empty. Two layouts (`spinner_item`, `spinner_dropdown_item`) now replace Android's stock ones in all 27 places an adapter was built, and a theme style gives every list a surface background and an accent highlight. What you do: install the new build.

## 6 Oct 2026: contract pages no longer repeat each other

What the app does. The group contract pages say each thing once. The "money rules" page is gone for goal and savings groups (its contribution, payout rule and withdrawal lines were already on the "In short" and "where the money goes" pages) and now only appears for a circle that has a grace period, penalty or commission. The "About" page no longer repeats who the admin is, the "How Harvana keeps it safe" page no longer repeats that you sign with your wallet or the circle bond, the worked example is one or two lines of numbers (a savings group has none), the treasurer line no longer repeats the voting weights, and "Words explained" lists only words no earlier page explains. What you do: install the new build. Contracts people already signed stay valid.

## 6 Oct 2026: block members (friends and followers)

What the app does. You can now block a member from three places: the ⋯ menu on a row in Friends, Following or Followers ("Block"), and the "Block this member" link on their profile. Blocking removes them from your followers and friends, makes you stop following them, hides their Learn posts from your feed, and stops them following you again (they just get "You can't follow this member." and are not told they are blocked). A new "Blocked" tab on the Friends screen lists everyone you blocked with an Unblock button; the profile link also turns into "Unblock this member". Unblocking does not restore the old follows: you both start fresh. Because blocking removes the follow, a blocked member can no longer read your contact links or pinch you.

What you do. Publish `docs/firestore.console.rules` again. It adds a private `BLOCKS/{you}/list/{them}` list that only you can read or write, and makes creating a follow fail when the person being followed has blocked the follower. Without it, Block shows an error. Then install the new build. A blocked member's own phone may keep showing you in their Following list until they open it and unfollow; they cannot follow again.

## 6 Oct 2026: loan interest is per day, for the days held

What the app does. Loan interest is now set, shown and charged per day in every group, never per month. When a group is created the rate is "Interest per day" (0 to 5%). When a member asks for a loan they choose how many days they plan to keep it (1 to 30, the network's range) and see the interest for the full term; the loan screen says that repaying earlier costs interest only for the days held. Example: 0.5% a day on 100 HARV, taken for 30 days and repaid on day 15, costs 7.5 HARV, not 15. The network already worked this way (simple daily interest by the elapsed time, fractions of a day counted, worked out when the loan is repaid), so no node change was needed; what was wrong was the app, which labelled the same number "per month" in the create screens, the contract and the loan screens. A group on the network now shows its stored number as a per-day rate, and an older app-only group with a monthly or yearly rate has it spread over 30 or 365 days so nobody's existing loan gets dearer. The contract's Loans section says it in plain words with the early-repayment example. The group settings screen saves the rate as per day (0 to 5%).

What you do. Install the new build. Groups already on the network were created with the number typed as "monthly" but the network has always charged it per day: check any group whose creator typed a monthly-sounding rate (for example 5), because that group is charging 5% a day. Tell me which groups and I can suggest what to do; the rate of a group on the network cannot be changed after creation.

## 6 Oct 2026: more members vote as a group grows

What the app does. Text only. The members' 60% of a group vote is now shared by 20% of the qualified members, at least 5 and at most 15 (it was always the top 5). The contract says so. A treasurer never takes one of those member places. What you do: deploy the node jar first (node `docs/DEPLOYMENT.md`, "Group treasurer role"), then install the build. Votes already open keep their voters and weights.

## 6 Oct 2026: vote weights with a treasurer: officers share the 40% in order

What the app does. Text only. When a group has a treasurer the 40% that was the admin's alone is shared, calculated from how many members vote: the admin holds the most, then the treasurer, then each member, in equal steps (about 23% and 17% in a group with five voting members, 22% and 18% with four, 25% and 15% with ten). With three voting members or fewer the officers split the 40% evenly. Members still share 60%. Without a treasurer the admin keeps 40%. The contract, the network panel and the admin's permissions page say so. What you do: deploy the node jar first (node `docs/DEPLOYMENT.md`, "Group treasurer role"), then install the build. Votes already open keep the weights they started with.

## 6 Oct 2026: treasurer role (needs the new node jar)

What the app does. In Group Settings, Members, tapping a member of a goal or savings group on the network (the admin only) now offers Make treasurer / Remove as treasurer. It asks for confirmation and the PIN, then the admin's wallet signs the change on the network. The member list shows a TREASURER badge, and the member's permissions page says what the role allows: in a savings group, approving members' withdrawal requests (the money still goes only to the member it belongs to); in a goal group, asking for payouts and milestone releases (the group still votes first). The treasurer sees the same Approve withdrawal buttons and request actions the admin sees. The treasurer gets no extra vote and cannot change settings, the admin seat or who is in the group. Circles and groups kept only in the app have no treasurer.

Admin co-sign: a payout or milestone request filed by the treasurer is paid only after the group votes and the admin has voted yes as well (the node records the admin as the required co-signer when the request is filed). The admin's own requests work as before, and savings withdrawal approvals need no co-sign because they pay only the member who owns the savings. The admin votes yes in the normal vote list.

Optional, and two ways to decide. A group has no treasurer unless one is appointed. The admin can name or remove one alone, or any member can open "Propose as treasurer (the group votes)" on another member; the group, including the admin, then votes from Group Settings, Members, "Treasurer (optional)" card (Vote yes / Vote no). Adding someone by vote also needs the admin's yes; removing the treasurer needs only the vote. One proposal can be open at a time and it expires after 14 days. The contract of goal and savings groups now says the treasurer is optional and how they are appointed.

What you do. Deploy the node jar first (node `docs/DEPLOYMENT.md`, "Group treasurer role"), then install the new app build. With an old node, a treasurer's approval is refused as "only the admin".

## 6 Oct 2026: member list with roles and permissions in Group Settings

What the app does. Group Settings, Members section, has a new "Roles and permissions" card: every active member with an initial, name, a role badge (ADMIN or MEMBER, admin first) and the slot for a rotating circle. Tapping a member opens what that person can do under the group's rules: pay in, invite (only if the admin allows members to invite), vote on big decisions in goal and savings groups (needs one payment and 48 hours in the group; the screen says whether the 48 hours have passed), and for the admin: accept, remove and block members, change settings, ask for payouts and approve withdrawals, hand over the admin role, close the group. For a member it also says what they cannot do (remove members, change settings, close; take a goal deposit out alone). The admin sees Make admin (the existing transfer, with its confirmation) and Remove (the existing removal, which also blocks them from requesting again) on other members. The permissions are read from the group's rules and are not switches: the app does not let an admin give one member special powers, because the network and the contract do not have per-member permissions. What you do: install the new build. No node or rules change.

## 6 Oct 2026: group settings page in standard sections

What the app does. Group Settings is now organised under a chip bar, one section at a time: **General** (type, picture, description), **Access** (visibility, approval to join, members can invite, plus a new Invite people card with Invite someone and Share group ID), **Members** (new summary card with the member count and Transfer admin role, then the blocked-members list), **Money & rules** (contribution and the type's own rules, the rule notes and the group wallet with backup and restore), **Network** (the network control panel: who decides, admin recovery), and **More** (mute reminders and notices, a new About this group card with type, status, where it is kept, created date, group ID with Copy, and Read the contract, the admin's close or reopen section, and a new Leave this group card). The cards keep their existing behaviour and the Save Settings button still saves everything; only where each card sits changed. Leave and Transfer admin are still in the top menu as well. What you do: install the new build. No node or rules change.

## 6 Oct 2026: contract wording checked against the app code

What the app does. After reading the app code: for a group that exists on the network the app's Group Settings already leaves the contribution, goal, payout rule, interest and withdrawal rule alone ("stay as the network set them"; it only saves the profile), so the contract's money-flow sections are accurate for those. But an older group that exists only in the app can still have those settings changed by its admin and has no network wallet, so the "network made a wallet with no private key" wording was wrong for it. Those groups now get a shorter section that says plainly: kept in the app only, no network wallet or enforced payouts, the admin looks after the money and can change settings, so read again if the rules change. What you do: install the new build.

## 6 Oct 2026: every group type's contract says where the money goes

What the app does. Rotating circles and accumulative savings groups now have the same kind of contract section that goal groups got: "The circle and where the money goes" and "The savings group and where the money goes". Each says what the group is for, who started it (the admin), that the network made a group wallet with no private key, that every payment or saving goes from the member's own wallet into that group wallet and never through a Harvana company wallet, and who is paid from it and how (circle: the member whose turn it is, in the order the network sets when the circle fills, with the bond covering a missed payment; savings: back to the member's own wallet when claimed, after the admin approves a withdrawal). All three types are read page by page with the tick-to-continue rule from the entry below. The node was read to check the "who set it" claims: the contribution, goal, beneficiary, payout rule, interest and withdrawal rule are written only when the group is created and no endpoint changes them afterwards; the only thing that moves is the admin seat (admin recovery vote). The circle order is not pure joining order: members who already finished a circle take the first 30% of places. What you do: install the new build.

## 6 Oct 2026: group contract is read page by page and says where the goal money goes

What the app does. (1) The contract of a goal group has a new section, "The goal and where the money goes": the purpose (members gather money for one goal), who set it (the admin who created the group), the group wallet the network makes when the group is created (no private key, nobody can spend from it alone), that every deposit goes from the member's wallet into that group wallet (never through a Harvana company wallet), who receives the money when the goal is reached (the beneficiary wallet chosen at creation) and when (all at once, after a vote, in milestones or as grants), and what happens if the goal is never reached. New goal groups now save the beneficiary wallet in their profile so the contract can show it; older ones say "the wallet chosen when the group was created". (2) Every contract is now read one page per topic, in bold, and cannot be skipped: no swiping ahead, the "I understand and agree" box becomes tickable only after the page is read to its end, and Next works only once it is ticked. The last page shows the fingerprint and signs with the wallet and PIN, as before.

What you do. Install the new build. No node or rules change. Contracts already signed stay valid; they were signed over the older text.

## 6 Oct 2026: new goal from a goal group (app only)

What the app does. The board of a goal group now ends with a "+ New goal" box. Tapping it asks for a name and a target (the current target is filled in), shows the review screen with the network's 1 HARV creation fee, and asks for the PIN. It then creates a new goal group on the network with the same payout rule and beneficiary, copies the group's profile (picture, place, description, rules) into a new Firestore group with the new name and target, and opens it. Each goal is its own group with its own wallet and its own money; the old goal is left exactly as it was. Nothing changes on the node: it already lets anyone create goal groups.

What you do. Install the new build. No rules change (it writes the new group the same way the create wizard does).

## 6 Oct 2026: savings calendar pays by tapping the day

What the app does. On a savings line (Payments tab) the separate "Save today" button is gone. Tap today's blue box to save today; tap a red (missed) box to pay that one day back; tap a grey upcoming box to pay ahead from today up to that day. Long-press any payable box to start picking several, then tap more and press Pay in the bar that appears. Days ahead are paid in order from today (picking one picks every day up to it); missed days are paid by their exact dates; a mix is two payments behind one review and PIN, back-pay first. Every payment still shows the review screen and asks for the PIN first. "Save several days" stays for paying ahead. What you do: install the new build. No node change. Rotating (circle) groups pay one contribution per round and goal groups take free-amount deposits, so they have no day boxes to pick; the node only accepts those as before.

## 6 Oct 2026: Learn filter chips

What the app does. The Learn filter chips (For you, Latest, categories) now show their text in the page's normal text colour when not selected (it was white on cream in the light theme, so it could not be read), keep one fixed height and bold weight, and no longer animate or change width when pressed. What you do: install the new build.

## 6 Oct 2026: Devices list

What the app does. Profile, Activity tab, new "Devices" button: a list of the phones the account has been signed in on. This phone is on top ("Signed in now"); under "Other phones" each earlier phone shows its make and model, when it signed in, and when it was signed out (or when it was last seen). The list is kept in the account's own `USERS/{uid}/private/devices` document (one entry per phone, newest twelve), updated whenever a phone opens the app or signs in; a fresh sign-in marks the other phones signed out. Because an account is signed in on one phone at a time, there is no "sign out other sessions" button.

What you do. Install the new build. No rules change (the existing owner-only rule for `USERS/{uid}/private/{doc}` covers it). Phones on an older build do not appear until they update.

## 6 Oct 2026: app activity feeds the trust score (liveness check)

What the app does. The activity the app counts on the phone now doubles as a liveness check on the account. On each day the account was really used (at least 8 taps and screens, counted in UTC days) the app tells the node, signed by the wallet, which days those were: only the day numbers, never what was done. The node adds them to the "days active in the last 30" part of the trust score (the engagement component: up to 12 days is full marks for that part). A day that also had a counted read still counts once. Sent at most every 6 hours, when the app is closed or the Home screen opens. Together with one phone per account this makes an idle or abandoned account earn nothing from it.

What you do. Deploy the node jar with the new `POST /api/v1/engagement/liveness` (node `docs/DEPLOYMENT.md`), then install the new app build. Until the node is updated the app's report is refused and quietly retried.

What did not change. Reads, the daily cap and the automated-pattern check use real reads only. This is evidence of use, not proof of a person: it is capped (about 4.5 of 100 points at most), and personhood binding and vouching stay the strongest signals.

## 6 Oct 2026: one phone per account

What the app does. An account can be signed in on one phone at a time. When you sign in on a new phone, that phone stores a session id in your own `USERS/{uid}/private/session` document, and any other phone signed in to the account notices within seconds, signs out and shows "Signed out: this account was signed in on <phone>". Only the sign-in ends: the wallet and its keys stay on the old phone, so nothing is lost. The document also records the phone's make and model and the time. Phones already signed in before this build claim the session the first time they open the new build (the last one to open it keeps it).

What you do. Install the new build on every phone. No rules change: the existing owner-only rule for `USERS/{uid}/private/{doc}` already covers it. Not a security lock: it is checked by the app, so a phone running an old build is not signed out.

## 6 Oct 2026: My Activity now tracks the whole app

What the app does. From this build the app counts how it is used, on the phone only: screens opened, taps, visits, time in the app, by day, weekday and hour. Only counts are kept (no content, no text, nothing sent to the node or Firestore). My Activity shows the daily chart, busiest days, peak hours (in the phone's time), time today and over 30 days, visits, taps and the most opened screens. The verified-reads tiles still come from the node. Counting starts when the new build is installed, so charts fill up over the following days.

What you do. Install the new build. Nothing on the server.

## 6 Oct 2026: one-time wallet link, article options, activity fixes

What the app does. (1) Posting in Learn no longer links or binds anything. It only checks that the profile already shows your active wallet; if not, it says to open Profile and tap "Bind this device" once. That button now binds the phone and then links the wallet to the account, shows the real answer from the network (no old "not accepting links" text), and stays visible until both are done. (2) The Activity tiles on a profile now have real margins (the spacing was being overwritten). (3) "My Learn contributions" rows have a More menu: Edit (title and text), Visibility (Public, Followers, Only me), Copy text, Delete. Followers-only and Only-me articles are hidden from other people in the Learn feed. (4) My Activity now explains errors (clock off, no answer) with tap to retry, shows axes on empty charts, and says that your own posts do not count.

What you do. Publish `docs/firestore.console.rules` again: it lets the author change `title`, `description`, `visibility` and `editedAt` of their own article. Without it, Edit and Visibility say "The new rules may not be published yet". Then install the new build.

What did not change. The node. Visibility is hidden in the app feed, not enforced by Firestore, so it is not a security boundary.

## 6 Oct 2026: version 1.1.0 (build 2)

The first version bump since the app was created (it was 1.0, build 1). It collects everything in the entries below and above this one that changed on the phone: one profile screen with cover, friends and groups redesign, group pictures with type rings, Learn Hub "For you" feed with formatted text, video links, optional pictures and delete, crop screens and face focus, one back arrow, bars that follow each page's colour, the show/hide balance eye, device binding, plain-English crash screen, and the shimmer loading. Install this build on every phone; older builds cannot show the new rows and will keep the old look. Build 2 is what the Play listing or a side-loaded copy must be above.

Release checklist for whoever builds it: (1) publish `docs/firestore.console.rules`; (2) set Cloudinary preset Overwrite on (see "Pictures upload on Save"); (3) deploy the node jar with the media-delete endpoint and its three Cloudinary settings (node `docs/DEPLOYMENT.md`); (4) build and install.

## 6 Oct 2026: video links in Learn Hub articles

What the app does. When writing an article, an author can paste a video link instead of (or as well as) an image: YouTube (watch, youtu.be, Shorts, live), Facebook (video, watch, reel, fb.watch), TikTok (the full `/@name/video/123...` link; short `vm.tiktok.com` links are refused with an explanation) or Vimeo. The field says "YouTube video found" (or why the link cannot be used) as they type. The article is saved with `videoUrl` (a clean https link) and `videoType`; a YouTube link also supplies its thumbnail as the cover, so a video article needs no image. In the reader, the video shows as a picture with a play button and the platform name; nothing loads until the reader presses play, then the platform's own player streams the video inside the article, with full screen, and "Can't play it here? Open it in YouTube" under it for videos whose owner has switched embedding off. Nothing is uploaded, stored or re-hosted by Harvana. Feed cards show "▶ Video" beside the category. Articles without a video are unchanged and no player is created for them.

What you do. Publish `docs/firestore.console.rules` again: it lets a new article have no image when it has a video link, and limits `videoUrl` to an https address of at most 400 characters. Then install an app build from this date.

What did not change. The node, the scam screen on titles and descriptions, and how reading, reactions and comments work.

## 6 Oct 2026: cover photo on profiles

What the app does. Every profile now has a wide cover picture across the top, with the round profile picture overlapping its lower edge. Members add or change theirs in Edit profile (Change cover, and Remove if there is one); on your own profile the cover also has an Add cover / Change cover button. Other members see it on your profile. With no cover, a soft colour band is shown instead. The picture is shrunk to 1280 px and uploaded the same way as the profile picture (Cloudinary if set up, else Firebase Storage); the address is saved as `cover` on the member's own `USERS/{uid}` document.

What you do. Install an app build from this date. No rules change: members can already write to their own `USERS` document and any signed-in member can read it. If you use Firebase Storage rather than Cloudinary, the Storage rules need to allow writing under `profile_covers/{uid}/` (the same pattern as `profile_icons/{uid}/`).

What did not change. The public profile card (`PROFILES`), the node.

## 6 Oct 2026: profile and Learn pictures can go to Cloudinary's free plan instead of Firebase Storage

What the app does. Photos picked for a profile picture or a Learn Hub cover are first shrunk on the phone (profile 512 px, cover 1280 px, JPEG, turned upright), usually to 40 to 250 KB, then uploaded to one place and the returned https address is stored exactly as before, so nothing that shows a picture changes. If `app/src/main/res/values/image_host.xml` has a Cloudinary cloud name and preset, the upload goes to Cloudinary; if both are empty (the default) it goes to Firebase Storage as it did.

What you do (only if you want Cloudinary).

1. Create a free account at cloudinary.com. Copy the **Cloud name** from the dashboard.
2. Settings > Upload > Add upload preset: Signing mode **Unsigned**, allowed formats `jpg, png, webp`, a maximum file size (for example 2 MB), and set **Asset folder** to `harvana` (the app does not send a folder for Cloudinary).
3. Put the cloud name and the preset name into `cloudinary_cloud_name` and `cloudinary_upload_preset` in `image_host.xml`, then build the app.

Neither value is a secret, but an unsigned preset lets anyone who knows it upload to your account, so keep the size and format limits tight and watch the dashboard's usage. Pictures already in Firebase Storage keep working; their addresses are unchanged.

What did not change. Firestore rules (the picture address is still an https string), the Storage rules for the Firebase route, and the node.

## 5 Oct 2026: one profile screen, Edit profile and Profile settings, Find friends, Learn publishes at once, shimmer loading

What the app does.

- **Back arrow.** Every screen now shows the same arrow. Screens built in code had picked up a thin chevron from a leftover `drawable-xhdpi/ic_arrow_back.xml`; that file is deleted, so they use the standard arrow the P2P screens always had.
- **One profile.** Tapping your photo or "Profile" opens the same page other members see, with your own extras. The old personal profile screen is gone (it only forwards now). Your phone, email, occupation, address, birthday, ID and verification sit in a folded "Personal details" card that only you can open; the trust score's five parts are folded under "Show breakdown".
- **Edit profile** now works: change photo, first and last name (locked once an identity check has passed), occupation, address and city. Country, state, email, date of birth and ID are shown but fixed. Saving also refreshes your public card.
- **Profile settings** (the Settings tile on your profile): edit profile, contact card, QR code, profile privacy, privacy and visibility, blocked users, PIN and security, notifications, and a last row to the full app settings.
- **Find friends** is its own screen: contacts already on Harvana first with Follow, everyone else below with Invite (opens a text message to that number), and a search box. Reading contacts is still opt-in and nothing is uploaded.
- **Learn Hub** articles publish the moment they are posted. No approval step. The on-phone check for scams (guaranteed returns, asking for keys, shortened links) still runs. The author can delete their own article.
- **Shimmer loading** on screens that wait for the node or Firestore: profile, trust score, group pages, friends, invitations, open loans, home sections, and the main lists (groups, Learn, history, assets, P2P, members, loans, notices). A text that says "Loading…" now shimmers too.

What you do.

1. Publish `docs/firestore.console.rules` in the Firebase console (Firestore, Rules). It adds: the author may create and delete their own `LEARN_CONTENT` article.
2. Check the Firebase **Storage** rules allow a signed-in member to write under `profile_icons/{their uid}/` (and read it). The Learn cover uploads already use `learn_hub/{uid}/`. Example: `match /profile_icons/{uid}/{file} { allow read: if request.auth != null; allow write: if request.auth != null && request.auth.uid == uid; }`. Without it, "Change photo" shows an upload error and the rest of Edit profile still works.
3. Install an app build from this date.

What did not change. Nothing on the node. Old Learn submissions stay in `LEARN_SUBMISSIONS` and are not published.

## 5 Oct 2026: a separate create screen for each group type, and a friend picker

What the app does. Rotating, savings and goal groups now have their own create flow, in four steps (Basics, the type's rules, People, Review), and each asks for different things in its own words. Rotating: contribution, number of members, round length, with a live summary of the pot, the bond each member locks, and how long it runs. Savings: whether members can borrow and at what interest, and when savings can be taken out. Goal: what the group is for (one-off purchase, a cause the admin pays out, a project in stages, an emergency fund), the target, the rule for how money leaves and who votes, and who receives it, with a clear notice when the receiver is the admin. The old payout-order options (random, first-come, auction) are gone for rotating groups because the network pays in the order members join. The review shows the 1 HARV fee, that nothing is locked, and that the network makes the group wallet. Add friends opens a picker: search by name or @username, tap a friend to add just them, press and hold to tick several (a checkbox then appears on every friend and a tap ticks or unticks), Select all, and find someone by exact username who is not yet a friend.

What you do. Install an app build from this date. Nothing on the node or in Firestore changes for this.

What did not change. The creation calls to the node, the profile saved in Firestore and the invitations are the same as before. The old single create form is still in the code but nothing opens it.

## 5 Oct 2026: creating a group locks nothing; the group wallet is the network's

What the app does. No group creation makes a wallet on the phone. The group wallet is an address the network derives from the group id; no key exists for it, so only the node's rules can move its money. Creating a rotating group no longer locks the admin's double bond: the review sheet says "Locked now: Nothing", the admin pays only the 1 HARV creation fee, and then joins first, like everyone else, locking the ordinary bond. Until the admin has joined, other members see "Nobody has joined yet. The admin joins first" and the node refuses their join with a clear message. Savings and goal groups were already fee-only.

What you do, in this order:

1. Deploy the node first (node repository `docs/DEPLOYMENT.md`, "creating a rotating circle locks nothing"). The change is in the node: an old node still takes the double bond at creation.
2. Then install an app build from this date. An old app against the new node still works; the admin just has to tap Join afterwards.

What did not change. Circles created before the node swap keep their admin as member 1 with the double bond locked; nothing is refunded or moved.

## 5 Oct 2026: one build file, `app/module.toml`

What changed. CodeAssist builds from `app/module.toml`, and GitHub Actions used a separate `app/build.gradle`, so the two could disagree. CI now runs `tools/module_toml_to_gradle.py` first, which writes `app/build.gradle` from the toml: package name, SDK levels, version, build types and every dependency. Edit the toml, not the Gradle file; `python3 tools/module_toml_to_gradle.py --check` tells you whether they match. The toml keeps `core-1.13.0.aar` and `material-1.11.0.aar` as local files, as you build with them in CodeAssist, and now lists everything those two files need and do not bring themselves (a local `.aar` carries none of its own dependencies, which is why CodeAssist could not find `cardElevation`, `drawerLayoutStyle` or `contentPadding`): CardView, DrawerLayout, ConstraintLayout, CoordinatorLayout, RecyclerView, ViewPager2, Transition, VectorDrawable, Fragment, Lifecycle, Collection, Concurrent, Interpolator, VersionedParcelable and the annotation libraries. For Gradle the script swaps those two local files for the same versions from Maven, because a local `.aar` next to the same library arriving through AppCompat is a duplicate-class error.

What you do. Pull, and in CodeAssist reopen the project so it re-reads `module.toml`. The files in `app/libs` stay: CodeAssist uses them. The manifest warning about MPAndroidChart's version attributes is harmless.

## 5 Oct 2026: P2P trade chat, network actions for groups, Network control in group settings

What the app does. A P2P trade has a "Chat with the trader" link on the buyer's and the seller's trade screens: a small chat between the two people on that order, with a reminder to pay only into the escrow shown on the trade. Messages are kept for 30 days after the last one so a dispute can be shown what was said; the first of the two people to open the chat after that deletes it (there is no server job). The other person gets a notification when you write, with the other notification checks. For a group on the network the footer is now one button, "Actions & votes", which opens the network actions (the old Firestore payout and loan buttons stay only for legacy groups); an admin's payout or milestone request counts as the admin's yes vote. Group settings gains a Network control panel: the network id and wallet, whether the profile and the network agree, who decides and what starts a vote, and a screen for admin recovery.

What you do, in this order:

1. Publish the Firestore rules first: open `docs/firestore.console.rules`, select all, paste over the Firebase console editor and publish. It adds `P2P_CHATS` (and its messages). Do not copy rules out of a chat message.
2. The node does not need a new jar for this change.
3. Install an app build from this date or later.

What this does not do. The chat is not end-to-end encrypted: it is stored in Firestore for 30 days, readable by the two people and by whoever administers your Firebase project. There is no moderator view yet, so for a dispute a party shows the chat themselves. See `docs/GROUP_SYNC_AND_VOTING.md` for what the network and Firestore each hold for a group, the voting rules and the gaps that remain.

## 5 Oct 2026: fixed goals no longer say the money was sent when it was not

What was wrong. A fixed goal whose rule is "admin asks, the group votes" keeps the money in the group wallet after the goal is reached; nothing is paid until the admin requests a payout and the members vote. The app said the network had already sent the pot to the admin's wallet, hid the Request payout action once the goal was reached, and showed "Collected 0" because it read the Firestore payments list, which never holds deposits made on the network. Checked against the live node for group `FIXED-acbfe8c30807d5a4`: state ACTIVE, 40 of 40 raised, 40 HARV still in the group wallet, 0 paid out.

What the app does now. The wording follows what happened: "held by the group wallet, not paid out, the admin asks and members vote" (admin rule), "the network is sending" (all-at-once rule, until it confirms), "was sent to" only when the network marks the group completed. The admin keeps the Request payout, milestone and grant actions while money is left. Collected on a fixed goal is read from the network.

What did not change. The node does not need a new jar. A goal still closes to deposits once it is reached, and a group finishes only when the money is paid out.

What you do. Install an app build from this date or later. For the group above, the admin opens its Payments tab and uses Request payout; the other member then votes.

## 5 Oct 2026: friends list, pinch, and referrals by username

What the app does. The Friends screen shows each person with their photo, name, @username and trust score, with Follow / Unfollow and a Pinch button. A pinch is a nudge: the friend gets a notification "(name) pinched you. Go check what they want." One pinch per friend per hour, and only to someone you follow. Your username is your referral code: it is shown at the top of Friends with a Share button, and a new member can type a username under "Referred by (optional)" when creating their account. The person named sees the request under Friends, Referrals, and confirms it with their PIN only if they know the person. When both have agreed the referral is recorded on the network.

Why. Whoever refers someone answers for them. For each person you referred who later defaults in a group, your trust percentage drops by 10 points, up to 30. A default that is cured or forgiven stops costing you. This is worked out by the node and shown on your profile like the rest of the score.

What you do, in this order:

1. Publish the Firestore rules first. Open `docs/firestore.console.rules`, select all, paste it over the Firebase console editor and publish. It adds the rules for pinches (`PINCHES`), referral requests (`REFERRAL_ASKS`) and the "who invited me" fields on a profile (set once, cannot be changed). Do not copy rules out of a chat message.
2. Deploy the node. The referral record now needs the referrer's signature as well (`docs/TRUST_SCORE.md`, "The referrer must agree"), and the referrer's score is refreshed when a person they referred defaults. Use the steps in the node repository's `docs/DEPLOYMENT.md` (build, swap the jar, restart `harvana`). An app build older than this one cannot record referrals; none did before.
3. Install an app build from this date or later.

What did not change. Following, followers and contact links work as before. Pinches use the phone's existing notification check (about every 15 minutes while the app is closed), not a push service, so a pinch can arrive a few minutes late.

## 5 Oct 2026: confirm your email before signing in; trust score screen rebuilt and shown everywhere

What the app does. An account made with an email and password cannot get into the app until the email is confirmed. Creating an account, signing in with an unconfirmed email, or opening the app with one all land on a "Confirm your email" screen with Resend (once a minute), "I have confirmed" and "Use a different account". Google sign-in is not affected. Forgot password now shows a tap-friendly link and a clear "Check your email" message. The Trust score screen is rebuilt around the node's percentage: the number and tier, how far the next tier is, the five parts with what raises each, and the Learn Hub activity. The percentage now appears on the Home card, the profile, P2P listings and trades, group members, group cards (the group's own percentage), join and payout requests, the people lists and Learn Hub posts. The older on-device score is no longer shown anywhere.

What did not change. The node does not need a new jar. Firestore rules do not change for this.

What you do:

1. Install an app build from this date or later.
2. In the Firebase console, Authentication, Templates, check that "Email address verification" and "Password reset" are enabled and the sender is set. If the reset or confirmation email never arrives, check Authentication, Settings, Authorized domains and the spam folder. The app can only ask Firebase to send them.
3. Accounts that signed up with a password before this build and never confirmed will be asked to confirm on their next open. That is intended.

What this does not do. The check is in the app. A person using a modified app could skip it. To enforce it on the server too, a later change can require `request.auth.token.email_verified == true` for password accounts in `signedIn()` in the Firestore rules; that locks out every unconfirmed account at once, so it is not done here.

## 5 Oct 2026: trust percentage, Learn Hub reads and gifts, group trust

What the app does. A profile and a Learn Hub post show one trust score as a percentage with a tier (Bronze, Silver, Gold, Platinum), read from Firestore. The node computes it and writes it to the person's public card (`PROFILES/{pid}`), so nobody can edit or hide it. Learn Hub posts are social cards: fire, comments, share, gift, a reader that records a verified read once the post is read to the end, and a My Activity screen (30-day bars, popular days, peak hours). A group page shows the group's trust percentage and its five parts. The dynamic-animation library was added so the group list screen no longer crashes when it draws its progress bar. The root Gradle files were restored after CodeAssist removed them, so GitHub Actions can build again.

What did not change. PIN rules, staking, and the 24-hour claim rule.

What you do, in this order:

1. Publish the rules first. Open `docs/firestore.console.rules` in this repository, select all, paste it over the Firebase console editor and publish. Do not copy rules out of a chat message: a chat copy inserts `\` before `$(...)` and the console rejects it. That file now holds the `PIDS` and `PROFILES` rules (the node-only trust fields are read-only for the owner) and the Learn Hub reaction and comment rules. Without them the new app cannot save a profile card, react or comment.
2. The node is already on the matching jar (live since 5 Oct 2026, see the node repository's `docs/DEPLOYMENT.md`). Do not restart it for this change.
3. Install an app build from this date or later. Older builds keep working and show the old 0 to 26 score.
4. Clear the GitHub Actions artifact storage (Settings, Actions, Artifacts) so the APK upload step passes. The Gradle build itself passes.
5. Check one profile: open My Activity, read a post to the end, wait for the next node publish and look for the percentage on the profile.

What the app cannot do yet. Several P2P screens still read the older on-device score, and not every other-user profile screen shows the badge. Read receipts are effort evidence, not proof of a person, so engagement is 15% of the score and counts as zero when the reading pattern looks automated.

## 2 Oct 2026 — notices no longer repeat

What the app does. A notice is shown once. The tab already says Notice, so the page does not say it again. A card does not print the same sentence as both the title and the message. A contribution used to write two notices; it now writes one. Copies already saved (the same title and message, or two contribution notices a few minutes apart) are collapsed when the list opens.

What did not change. The node does not need a new jar.

What you do:

1. Install an app build from this commit.
2. Do not restart the live node for this change.

## 2 Oct 2026 — receipt layout for a payment

What the app does. The review before a payment, and the transaction screen after it, now read like a receipt. The amount is large. Under it is what the payment is for, then the date. Transaction details list the status, who it is from, who it is to, and the fee. See more opens the reference, block, and nonce. A timeline shows review, verify, and sent. Verify and continue is what asks for the PIN. On a finished transfer, Share transaction receipt is the button at the bottom.

What did not change. The PIN is still required. The node does not need a new jar.

What you do:

1. Install an app build from this commit.
2. Do not restart the live node for this change.

## 2 Oct 2026 — payment review sheet, then the PIN

What the app does. A payment button no longer opens the PIN by itself. Stake, unstake, claim, send, a group contribution, a deposit, joining, a loan disbursement or repayment, a group-wallet payout, creating a group, and P2P open, release, deposit, listing, and cancel first open a review sheet. The sheet states the amount, what the payment is for, who receives it, where it comes from, and the fee. Verify on that sheet is what asks for the PIN or fingerprint. Cancel on the sheet sends nothing.

What did not change. The PIN is still required, and a phone with no PIN is still sent to Settings. The node does not need a new jar.

What you do:

1. Install an app build from this commit. A build from `7fd6a51` asked for the PIN after a thinner confirm. This build is the receipt the person reads first.
2. Do not restart the live node for this change.

## 1 Oct 2026 — review the payment, then the PIN

What the app does. A payment no longer jumps from the button to the PIN. Stake, unstake, claim, send, a group contribution or deposit, joining a group, a loan disbursement or repayment, a group-wallet payout, creating a group, and P2P open, release, deposit, and listing first show a summary sheet: the amount, who it is for, and the fee. Continue is what asks for the PIN or fingerprint. Cancel on the sheet sends nothing. If no PIN is set, the action still stops and offers Settings.

What did not change. Group creation no longer generates a wallet or a 12-word phrase on the phone. The network assigns the group's wallet (the escrow address) when the group is created, and that address is what the app saves. The old "new group wallet seed" step is gone. Trust and activity are unchanged. The node does not need a new jar.

What you do:

1. Install an app build from this commit.
2. Set a transaction PIN in Settings before the first payment or stake.
3. Do not restart the live node for this change.

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

## 5 Oct 2026 — P2P forms, sheets and toggles
- Add/Edit payment method: the four text fields were locked (`focusable=false`) and could not be typed in; fixed. "Set as default" now toggles from the whole row, not only the tiny checkbox. The sheet opens expanded and resizes above the keyboard.
- All P2P bottom sheets use the shared themed sheet (day/night correct). Filter/payment toggle rows are tappable across the full row.
- Hardcoded colours in P2P trade, chat and offer screens replaced by theme colours.

## 5 Oct 2026 — Named node badges
- New `trust/NodeBadges` reads the node's real badges (Circle veteran, Founding member, Zero default, Early payer, Vouched) from `/wallet/labels/{address}`, plus Insured trader from `/p2p/insurance-status`. `trust/BadgeChips` draws them as chips; tap a chip to read what it means.
- Shown on your profile (replacing the bare count) and under people on the Friends list and the friend picker. Badges are decoration only and are never stored by the app.
- Group trust before joining was already on Discover cards; group member rows do not show badges yet.

## 5 Oct 2026 — Docs brought in line
- `NODE_RECONCILIATION.md` now carries a status note: the group clients, referral consent, node trust percentages and badges exist, so the "no client for group actions" lines are historical.
- Node docs now write the brand as Harvana. The chain id, network id, signing strings, domain and package are unchanged. The Java class renames in the node are not done yet and wait for the owner's go-ahead.
- Node classes now read `HarvanaProtocolManager` and `HarvanaChainNodeApplication`; the node serves the coin name "Harvana". Comments and docs here follow. The staking dashboard layout still says "Harvana" because staking is being changed separately.

## 5 Oct 2026 — Profile screen: real tier, real data, no placeholders
- The header check mark and label now show the node's tier (Bronze, Silver, Gold, Platinum) in the tier colour, and stay hidden until the node has scored the person. The old "Tier 2" came from the profile record, not the network.
- The top score line shows the node percentage whenever the node has one (it showed "— / 26" while the card below said 16% Bronze).
- Sample text in the layout (name, "Lagos, Nigeria", phone, DOB, "Tier 2 · BVN/NIN verified") is gone; empty fields read "Not provided"; the address skips empty parts.
- Phone: a stored number made of two joined numbers is shown as the first one, and saving no longer joins a code onto a number that already starts with it.

## 5 Oct 2026 — Renamed to Harvana for the fresh genesis
- Signing prefixes (`HARVANA_*`), the auth message and the miner package name now use the new spelling, matching the node. This build only works against a node built from the matching node commit. The domain is unchanged until harvana is registered.

## 5 Oct 2026 — Trust score screen redesigned as a level screen
- Swipe the whole level screen (tier card, road to that tier, next badge) through the four tiers, opening on your current tier; the screen background changes colour with the tier; each says Surpassed, Current or Yet to unlock, with the points still needed. Page colours blend the tier colour into the theme surface, so day and night both stay in style.
- "Level up to <next tier>" shows the whole road (0, 40, 65, 85%) with a knob and a bubble at your percentage that fills in when the screen opens, the points left, and which part of the score has the most room (`trust/TierJourneyView`).
- "Next badge" shows the next badge the node really awards (Vouched, Circle veteran, Zero default, Early payer) with a bar that fills toward it. Insured trader is not included: its progress needs the P2P deposit status.
- The tier shown is the node's own `trustTier`, never worked out from the percentage (Platinum also needs reliability and conduct at 85%).
- Not built: tier perks. The node says badges and tiers unlock nothing, so the pages show requirements, not invented benefits.

## 5 Oct 2026 — New tier, and the Merchant manager screen
- The node now calls a wallet "New" until it has a met obligation, a completed circle or 5 confirmed actions; a group is New until it has 3 members and a finished cycle. The app shows "New · 16%" on profiles, a New page first in the trust level screen (with the three ways out and a bar for confirmed actions), and a neutral New colour. Needs the matching node build.
- Merchant manager: the Create Offer button text was the same colour as the button (now readable); the five filter tabs now show which one is selected and ripple on tap; Create Offer opens a curved bottom sheet with Sell and Buy side by side instead of a white dialog.
- Friends screen: the referral code and a Share button now sit side by side; Add a friend and Contacts come first and the tabs and list are at the bottom; rows are shorter with Follow/Unfollow and Pinch side by side in small buttons; the tabs now show the selected one (filled) and update on every tap.
- Spacing pass on the screens added in the last few days: more padding inside trust score cards and between them, looser note and row spacing, taller road view, wider gaps between badge chips, more room around the Friends tabs and buttons, and more space around the Merchant manager button, tabs and the Sell/Buy sheet.
- Build files: GitHub now builds only from `app/module.toml`. No Gradle file is stored in the repo any more (`build.gradle`, `settings.gradle`, `gradle.properties`, the wrapper and `app/build.gradle` are git-ignored); the workflow generates them from the toml each run. A phone commit can no longer add, delete or revert them.
- CI now follows the miner app's workflow: unit tests, then a debug APK (named with the commit, with a SHA256 file), manual runs allowed, older runs cancelled, and tags like v1.0 attach the APK to a release. The old `gradle build` (all variants plus lint) is gone, so lint warnings no longer fail the build and release minification is not part of CI.

## 5 Oct 2026 — Public profiles and privacy
- Tapping a person in Friends now opens their public profile (`social/PublicProfileActivity`): photo, name, @username, trust score and tier, the five trust parts, badges, bio, location, member-since, counts of groups, cycles completed, loans and pending loans, and the groups you both belong to (tap one to open it). Actions: Follow/Unfollow/Follow back, Pinch, Send HARV (opens Send with their wallet filled in), Contact (mutual friends only), Share.
- What is public: trust score and parts (the node writes them; nobody can hide them), country and state, counts. What is optional and off until the member turns it on: age (years only, never the date of birth) and city. Never shown: amounts, group names (only groups both people belong to), date of birth, phone, email, address.
- Privacy settings (`social/ProfilePrivacyActivity`, from Profile > Public profile > Privacy): two switches, a 140-character bio, a list of what is always public or never shown, and a preview of your own public profile. A switch turned off removes the value from the public card, not just from the screen.
- The city moved to the owner-only part of the profile (`PrivateProfile`), so it is no longer readable from `USERS/{uid}` by other members. The group members list therefore no longer shows other people's city.
- Your phone refreshes the public card (counts, location, age) whenever you open Profile. The counts are written by your own phone from the node and group records, so they are self-reported; making the node publish them like the trust score is the stronger version.
- Rules: `PROFILES/{pid}` now allows `country, state, city, age, show_age, show_city, bio, member_since, groups_count, cycles_completed, loans_count, loans_pending, stats_updated` (both rules files). Republish `docs/firestore.console.rules`.
- Not built: report or block a member, mutual friends (a person's follower list is private to them), a profile link that opens from outside the app.

## 5 Oct 2026 — Grace: forgiving a default
- Circle screens show a Defaults card for members who defaulted. The admin can ask the group to forgive one; the voters (admin 40%, others share 60%, 51% to pass) vote yes or no with their PIN. The defaulter cannot vote on their own case. Forgiving moves no money; it takes the default off the member's record (trust conduct, vouching, referrals).
- New badge names: Forgiven default, Cured default (nothing can set it yet), Rehabilitated (3 clean circles since the last default). Needs the matching node build.
- Signed messages added to `GroupCanonical` with tests.

## 5 Oct 2026 — New profile layout, QR code, and paying back missed days
- **Profile layout** (`social/PublicProfileActivity`, the same screen for you and for everyone else): centered photo, name, presence (online / last seen), the trust tier, a row of action tiles, an info card (value over caption), and three tabs: Trust, Activity, Groups. Others get Follow/Unfollow, Pinch, Send and Share; you get Edit info, QR code and Settings. The three-dot menu holds Share profile, Copy username, Contact details and Unfollow for others, and Change username, Privacy settings, Personal details and My QR code for you. Your own view also shows your phone, birthday (with age), email and city, which nobody else sees. The drawer header and the top-bar avatar now open this screen; the old profile (verification, business profile, Learn contributions) is under "Personal details".
- **QR code** (`social/ProfileQrActivity`): a card with your photo and @username that opens your profile when scanned, Share as a picture, and Scan to open someone else's. The code holds only `harvana://user/<username>`.
- **Pay back missed days**: on a savings line, red (missed) boxes can be tapped to pick them, or long-pressed to start picking several. A bar shows how many days and the cost; Pay asks for your PIN and sends the signed back-pay. The calendar is now drawn from the days the node says are paid, so a gap shows as red instead of the first N boxes turning green. Needs the matching node build. Fixed-custody calendars from older Firestore-only groups are not covered yet.
- Presence is the existing self-reported `lastActiveAt`, public to signed-in members.

## 5 Oct 2026 — Cure a default
- On a circle's Defaults card, a member who defaulted sees what it would cost to put it right (the part of their bond the default used, not the penalty), the date it must be done by, and how many cures they have used (2 allowed for life). One tap, review and PIN. It goes back into their own bond and the default stops counting against them. When the node says it is not allowed (window passed, bond did not cover the shortfall, limit reached) the card says why and points to the group's forgiveness vote.
- Needs the matching node build. The signed message is in `GroupCanonical` with a test.

## Profile layout and flexible Learn crop
- Profile now reads like a social profile: cover, picture on the left over the cover, name with the trust chip, @username, bio, a facts line (where, age, joined), and Following / Followers counts (tap to open Friends). On your own profile there are two wide buttons, Share and Edit profile; Settings, QR code and privacy are in the top-right menu.
- Learn cover crop is free: drag the corners or edges to pick any shape.

## Learn: For you feed, slimmer cards, optional pictures
- The Learn feed now suggests posts for you. "For you" ranks by what you open, read, fire, comment on, share and gift (category and author, fading over about 10 days), freshness and popularity, pushes down posts you have already seen or opened, and mixes topics. "Latest" is plain newest first. Your interest data stays on your phone.
- The order is set when the feed loads. New posts from others wait behind an "N new posts" button instead of shuffling the list; view counters no longer redraw cards.
- Cards: the picture is at the top, cut by the card's curve, fading into the card; text sits under it. Body is 3 lines.
- A picture or video link is optional: text-only posts are allowed. Owner: publish the updated docs/firestore.console.rules (image is now optional).

## Friends screen redesign
- Friends now uses pill tabs (Friends, Following, Followers, Referrals), a search box that filters as you type and can look a username up on Harvana, and a count line ("12 Friends").
- Rows are plain list rows: photo, name, @username, trust score, a "Follows you" label on the Following tab, one pill button (Following to unfollow, Follow back / Follow to follow) and a ⋯ menu (View profile, Pinch, Contact links, Unfollow). Followers you have not followed back come first.
- Username, Contacts and Invite are three quick buttons under the tabs. Your referral code and the Confirm / Decline requests live in the Referrals tab.

## Group pictures, and the profile photo that did not update
- Create group has an optional Group picture: pick, crop 1:1 (round guide), upload. Group settings has the same for the admin (Add / Change / Remove, saved straight away); members see it. The picture is stored as `groupPhoto` on the group, shown on the group page header, in My groups, and on shared groups on a profile. Firestore rules already let the creator edit it; no rules change.
- Fixed: a new profile photo showed the old one on the profile and Home. Edit profile saved the new photo on your private profile, but other screens read the public card (`PROFILES/{pid}`), which kept the old photo, and the Home header read the sign-in photo. Saving now copies the photo and name onto the public card, updates the sign-in photo, your own profile prefers your edited photo, and Home refreshes when you return. If a photo still looks old, open your own profile once: it republishes the card.

## Group type rings
- A coloured ring around the group picture shows the kind of group: blue = Rotating Pool, green = Accumulative Funds, purple = Fixed Contribution, grey = other. It shows with or without a picture, in My groups, the group page header, Group settings, Create group (preview) and shared groups on a profile. Colours live in `utils/GroupRing.java`.

## Learn: formatted text (bold, italic, headings, lists)
- Learn posts now understand light formatting: `# / ## / ###` headings, `**bold**`, `*italic*`, `***both***`, `~~strike~~`, `` `code` ``, `- item` / `1. item` lists (indent two spaces to nest), `> quote`, `---` divider, `[text](https://link)` and bare https links (tappable in the reader). An unclosed marker is shown as typed. `\*` shows a literal `*`.
- Write screen: format buttons (B, I, H, List, 1. List, Quote, Link) wrap the selection or the current line, and Preview shows the post as readers will. The description box is taller and now takes up to 8000 characters (was 600). Feed cards, the reader, Share and Copy all handle it (Share and Copy send plain text).
- Owner: publish the updated docs/firestore.console.rules (description limit 600 to 8000), or long posts will be refused.

## Group pictures everywhere
- The group picture and its type ring now also show on: the Discover list, Home (Upcoming payments and Trending public groups), the join request screen, the welcome sheet shown before joining, group invites (the picture loads with the group details) and Open loans. Together with My groups, the group page, Group settings, Create group and shared groups on a profile, every place a group appears uses `GroupRing.show`. A group without a picture shows the coin with its ring. Open loans rows for groups on the network show the ring without a picture for now.

## Goal group: "FIXED_GROUP_NOT_FOUND" on Request payout
- Cause: the group page can be drawn from a saved copy in Firestore (`GROUP_STATE`). The node answered the payout request with `FIXED_GROUP_NOT_FOUND`, meaning the node has no goal group with that id (the network state was reset, or the group never finished being created) while the saved copy still shows 40 of 40 HARV.
- The page now asks the node directly and, when it says "not found", shows a "Not on the network" card at the top explaining the numbers are an old copy. Errors of this kind are shown in plain words, not as raw JSON.
- Owner: to confirm on the server, `curl -s https://harvannaprotocol.com/api/v1/fixed/group/<the full FIXED-... id>`; a 404 confirms it. The stale `GROUP_STATE/<id>` documents in Firestore can be deleted. HARV that went into that group's escrow cannot be paid out from a group the node no longer has; if the chain was reset those balances were reset with it.

## No tint on photos or the group wallet icon
- Uploaded user and group photos are never tinted or coloured behind: the group header, group settings, Create group, join request and group lists now clear any tint and use a see-through fill behind the ring. The placeholder for a group without a picture is the real HARV coin, untinted.
- The Group wallet icon (group Overview) was an accent-coloured icon on an accent-coloured badge, so it showed as a solid orange square. The tint is removed, so the white wallet shows on the badge.

## Groups the network no longer has are cleared automatically
- `group/StaleGroupSweeper` runs in the background when My groups loads. It asks the node directly about each group that lives on the network and removes the ones the node reports as not found: from your list and local copies, and, if you are the group's admin, its Firestore profile is deleted too. Other members' lists clear when they next open the app.
- Safeguards: the node must answer a health check with a chain height first; only a clear "not found" counts (never a timeout); a group must be missing on two checks at least ten minutes apart; groups under an hour old are never touched; one pass at most every 30 minutes. A toast says how many were removed.
- Not cleared by the app: the node-written copies in Firestore (`GROUP_STATE`, `GROUP_MEMBERS`), because the rules let only the node write them. They are harmless once the group is gone from the lists; delete them in the Firebase console if you want them gone.

## Pictures upload on Save, and a change replaces the old one
- Picking a picture (profile photo, cover, group picture, Learn cover) now only crops it and previews it on the phone. It uploads when you press the save button (Save changes, Save Settings, Create group, Post Article). If the upload fails, nothing else is saved and the picture stays chosen so you can retry; if the upload works, the rest is saved right after. So a failed save no longer leaves an uploaded picture behind. Create group uploads the picture before the group is made, so a failed upload costs no fee.
- One picture per person and per group: uploads use a fixed name (`profile_icons/<uid>`, `profile_covers/<uid>`, `group_photos/<groupId>`), so a new picture replaces the old one. Owner, one-time: in Cloudinary open the upload preset `harvana_unsigned` (Settings, Upload, Upload presets) and switch **Overwrite** on, and **Use filename or externally defined Public ID** on with **Unique filename** off. If Overwrite is off the app still works (it falls back to a new name) but old pictures stay in Cloudinary.
- Not covered: removing a picture (the Remove buttons) and Learn covers (each article has its own) cannot delete the file from Cloudinary, because deleting needs the account's secret key, which must never be in the app. Deleting from the app would need a small node endpoint that holds the secret; say if you want that.

## True deletion of pictures, stable Upcoming payments, tier on the profile
- Removing a cover or a group picture, and deleting a group, now deletes the file from Cloudinary for good, through a new node endpoint (`POST /api/v1/media/delete`, see the node repository's `docs/DEPLOYMENT.md`). The phone sends its Firebase sign-in proof and the kind of picture; the node works out the file and lets only the owner (or the group's creator) delete it. Owner: add the Cloudinary API key and secret to the node's live settings file (three lines, steps in that file); until then removing a picture still works in the app but the file stays in Cloudinary.
- Home, Upcoming payments no longer reshuffle on refresh: order is what needs doing first, then group name, then group id (it used to follow whichever answer came back first). The two Trending public groups are tie-broken the same way.
- Profile: the tier is always shown. A member the network has not scored yet shows "New · not scored yet" (it used to say only "Not scored yet", and the tier fell back to Bronze); the Trust tab shows "Tier: New".

## Wallet device binding
- Why a profile said "Device: Not bound": the app kept a saved "bound" flag and only tried to bind right after a successful account link, so (a) when the link was refused (links are off by default on the node, or the wallet was already linked) binding was never attempted, and (b) after the node's fresh start, which forgot every binding, the saved flag hid it for good.
- Now binding is separate from linking and is checked against the node itself (`GET /api/v1/personhood/<address>`, at most every 30 minutes): a wallet that is not bound is bound with this phone's device hash, and the result is remembered with a plain-English status. A link the network refuses no longer blocks binding.
- The "On the network" card on your own profile shows a **Bind this device** button when the wallet is not bound (it also lives on the Trust score screen). One phone can be bound to one wallet; if this phone is already bound to a different wallet the status says so.

## One back arrow everywhere (audit of every screen and app bar)
- Audited all 66 screens in the manifest and every layout with a toolbar. The one back arrow is `ic_arrow_back` (Material arrow, 24dp). New `ic_nav_back` is the same shape in the toolbar's icon colour and is the theme's `homeAsUpIndicator`, so every toolbar up arrow, from layouts or code, now resolves to it (day and night themes).
- Found and fixed: Group details, P2P marketplace and Confirm sell offer used a different, rounded arrow (`icon_arrow_back_round`, with inconsistent sizes and colours): now `ic_arrow_back`, 24dp, with a Back description; both rounded variants are deleted so they cannot come back. Create wallet had a toolbar that was never installed, so it had no arrow and no title: fixed. My Activity, Learn read and Staking dashboard had a different, transparent bar without the app-bar colours: they now use the standard bar and arrow colour.
- Left as they are on purpose: the sign-in and welcome start screens, the splash, the email check, the crash screen, and the full-screen picture viewer and crop screen (the crop screen has Cancel).
- Rule: do not add another back icon. Use `@drawable/ic_arrow_back` in a custom header or let the toolbar use the theme's.

## App bar, toolbar and status bar match the screen (WhatsApp look)
- The bar colour is now the screen's own background in every theme: `appBarColor` is `colorBackground` and `onAppBarColor` is `colorTextPrimary` (light and night), so every toolbar, the Home bar, bars built in code, and the system status bar are the same colour as the body, with dark icons by day and light icons at night. P2P screens follow the same rule (their background matches the app's in each mode).
- Elevation 0 and no lift shadow on all 42 app-bar layouts and on the Home action bar. The collapsing Group page header scrim is the background colour too.
- To restore a coloured bar later, change the two attributes in `res/values/styles.xml` and `res/values-night/styles.xml`; nothing else needs touching.

- The bottom navigation bar is now the page colour too, flat, without its outline, so the whole frame (status bar, top bar, bottom bar) merges with the screen. The miner app got the same treatment (status bar, every top bar, the Home top bar and its bottom button row). The splash (brand colour), welcome and picture viewer/crop screens keep their own colours on purpose.

- Menus and popups (the Home three-dot menu, overflow menus, row menus) now use their own shade, a step darker than the page by day and a step lighter at night (`colorPopupSurface`), with a hairline edge, rounded corners and a stronger shadow, so they stand out from the screen behind them. One style (`HarvanaPopupMenu`) covers every system popup menu; the Home menu sets the same colours in code.

## Bars follow each page's own colour (BarMatcher)
- Pages are not all the same colour (Home is white, others cream or dark), so one theme colour could not match them all. `utils/BarMatcher` reads each page: the solid-colour view that starts right under the bar and fills the width is the page body, and its colour is applied to the toolbar and its app-bar layout, a collapsing header's scrim, the action bar, the status bar (icons dark on light, light on dark) and the bottom navigation bar on Home (tagged `bar_bottom`). It looks again whenever the screen is laid out, so changing tabs or loading a fragment keeps the bar in step. It is installed once from `SketchApplication`, so it covers every screen without per-screen code.
- Custom headers join in by tagging the view `android:tag="bar_top"` (done for the standard header and the P2P marketplace bar). Left alone on purpose: splash, welcome, the crash screen, and the picture viewer/crop/picker screens.
- The theme default (`appBarColor` = `colorBackground`) stays as the fallback until the first look.

## Activity grid, "Your groups" admin list, and the show/hide balance eye
- Profile, Activity tab: the four counts are now compact tiles (about 60 dp instead of a tall block), with space between tiles and from the card edge, the number in the accent colour on top and the label under it.
- Profile, Groups tab, "Your groups": lists the groups you are the admin (creator) of, each row like the Groups list (round picture with the type ring, name, type, members, round progress, trust); tap opens the group. Others still see only the groups they share with you. Old test groups stay hidden.
- Show/hide balance (the eye): before, the eye icons were pictures only. Now there is one saved switch for the whole app (`utils/BalancePrivacy`). Tapping the eye on Home, on the Assets page (both eyes) hides or shows every balance on every open screen at once, and the choice is kept after the app closes. While hidden, numbers become dots ("600.90755" shows "••••", "≈ ₦1,200.50" shows "≈ ₦••••", "20 HARV" shows "•••• HARV"). The icon is an open eye when balances show and a crossed eye when hidden. Covered: Home balance, local-currency value, Total saved, Open loans; Assets total value and every coin row; the coin page balance. A new balance on another screen joins by calling `BalancePrivacy.guard(textView)` once.

## Learn: compact reaction row, More at the end, Delete for the poster
- The reaction row is one compact line that always fits: Fire (with its count), Comments (with its count), Share and Gift share the width equally at 34 dp (icons 16 dp, count text 12 sp; Share and Gift are icons only), and the More button sits at the far end. It no longer scrolls sideways, so small phones show every reaction. Same row in the feed card and the full reader.
- More now has **Delete post** for the poster only (the item is not in anyone else's menu, and the Firestore rules refuse anyone else). It asks first, then removes the post for everyone; in the feed the card disappears by itself, in the reader the screen closes.
- A cover picture that cannot load no longer leaves an empty gap at the top of the card; the card is drawn as a text post.

## Face focus (cover pictures, crop screen, picture viewer)
- A new `FaceFocus` finds the main face in a picture on the phone (Android's built-in face detector; no network, no extra library, nothing uploaded). It is used in three places, and with no clear face every one of them behaves as before:
  - Learn cover pictures (feed card, reader, and the preview on the write screen) are cropped around the face, with a little head room, instead of the dead centre (the picture in your screenshot showed a chest with no face).
  - The crop screen opens already framed on the face: zoomed so the face fills a good part of the frame and centred in it for the square and cover shapes, and a starting box around the face for the free shape of Learn covers. You can still move and resize.
  - The full-screen picture viewer: double-tap goes straight to the face (zoomed to about half the screen width) when there is one; double-tap again returns to the full picture. Pinch and drag work as before.
- The detector sees upright, front-facing faces; side-on faces, small faces and drawings may not be found, and then the middle of the picture is used.

## Learn Hub: posting needs a linked wallet
- Readers gift HARV to the wallet the network has confirmed for the author's profile (`PROFILES/{pid}.wallet`, written by the node when the wallet is linked to the account). So Post Article now requires it: the app checks that the author's profile has a confirmed wallet and that it is the wallet active on the phone. If it is missing, the app links the wallet to the account right then ("Linking your wallet to your account…"), waits for the network to confirm (a few seconds, up to three checks), and then carries on posting by itself. If it still cannot be confirmed, nothing is posted and the message says why (for example the network is not accepting account links yet, or the account is linked to a different wallet).
- Owner: this depends on the node's account links. `harvana.identity.links-enabled` is `false` unless switched on in the live settings file; with it off, no wallet can be confirmed and nobody can post. Set it to `true` (it also needs `harvana.identity.pepper` to be a real secret), restart, and check with `curl -s https://harvannaprotocol.com/api/v1/personhood/<address>` that the node answers. See node `docs/DEPLOYMENT.md`.

- Learn Hub: in the full post view, tapping the picture opens it full screen (pinch, drag, double-tap; the double-tap goes to a face when there is one). In the feed, tapping a post's picture opens the post, like its title and text do.

- The Gift button's icon is now an unfilled line drawing of the Harvana coin (outer ring, thin inner ring and an outlined "H"), instead of the generic gift box. It is one colour and follows the button's accent tint. File: `res/drawable/ic_learn_gift.xml`.

## Group tabs fill the width without Loans (follow-up)
- The tab bar re-applies its fixed/fill mode after the pager rebuilds its tabs, so a group with loans switched off now stretches its five tabs across the full width.

## Staking: every stake is its own row (needs the new node jar first)
- Each time you stake, it now shows as its own card in My Positions with its own amount, lock (7/30/90 days or flexible), multiplier, unlock date and "Earned so far". Staking again no longer merges into an earlier stake.
- Each card has its own Unstake. A locked stake stays locked while your other stakes can be unstaked. The sheet shows which stake you are unstaking.
- There is still one **Claim all** button: one claim pays the rewards of every stake together (once every 24 hours).
- Your existing combined stake appears as one card with the same amount, lock and rewards.
- Install this after the node jar is updated (node docs/DEPLOYMENT.md, "Separate stakes"). With an older jar a single stake still unstakes, several stakes do not.

## Payments: pending buttons, full-screen success sheet, history names and icons
- **Pending buttons, app-wide.** From the moment a payment starts until the network confirms it (or it fails), every payment button shows "Pending…" with a rolling ring and cannot be tapped again. This covers the wallet Send screen, group contributions, savings and goal deposits, loan repayments, staking (Stake, Claim all, Unstake), P2P escrow and insurance, gifts and group-wallet payouts. The button that started the payment is locked automatically, and the main payment buttons are tagged `payment_button` in their layouts (code: `tx/PaymentTracker.java`; buttons built in code call `PaymentTracker.mark(view)`).
- **Full-screen success sheet after every payment.** Shows the amount, who it went to, the fee and transaction, a "Confirming on the network…" line that turns to "Confirmed", Share receipt and Done. It replaces the older per-screen success and progress sheets (code: `ui/PaymentSuccessSheet.java`). Claimed rewards and started unstakes use the same sheet.
- **History names and icons by what the transaction is.** Staked HARV (lock icon, shows the lock length), Staking payout, Group created, Group contribution, Savings contribution, Goal deposit, Circle bond, Loan repayment / disbursement, Gift sent / received, P2P escrow and Group payout each have their own title and icon instead of "Sent HARV" (code: `tx/TxKind.java`, `res/drawable/ic_tx_*.xml`). Plain transfers stay "Sent / Received". The detail page uses the same names.
- **Staking: Live and Closed tabs.** My Positions now has Live (one card per stake) and Closed (unstakes returning, and withdrawn ones) so the page stays short.
- Needs the new node jar for separate stakes (node docs/DEPLOYMENT.md, "Separate stakes").

## Gas on every action (needs the node jar with the fee rule)
- Claiming staking rewards, unstaking, claiming a savings payout, withdrawing a circle bond and requesting a goal payout now each charge a network fee from your own wallet, like a send does. You need a little HARV in the wallet or the action is refused with "Network fee not paid".
- The app sends a tiny fee transfer first (history shows it as "Network fee · For claiming staking rewards" with a receipt icon), then the action. The fee success sheet is not shown; the action's own sheet is.
- Stakes already paid gas.

## One fee rule everywhere (needs the node jar with the fee policy)
- Loan requests, goal-group requests and votes that can release money, and cancelling a P2P listing now also pay the network fee from your wallet first (history shows "Network fee"), the same as claims and unstakes. Creating a savings or goal group also charges gas on its creation fee, like circles.

## New look: Ink Teal and Warm Sand, and one avatar scale
- **Theme.** The whole app moves from orange on charcoal to **Ink Teal `#053D3A`** (trust, growth, calm) with **Warm Sand `#FFE2B8`** (warm, approachable). It is all in `res/values/colors.xml` and `res/values-night/colors.xml`, so every screen follows.
  - Day: warm cream pages (`#FFF8EE`), white cards, Ink Teal text, teal `#0B5F57` buttons with white text.
  - Night: deep teal pages (`#031F1D`), teal cards, warm white text, **Warm Sand buttons with Ink Teal text**.
  - The wallet card is Ink Teal in both. Success is a calmer green, errors a softer red. The HARV coin is now gold.
  - Text and button contrast checked against WCAG AA (body text 4.7 to 15:1, buttons 7.5 to 9.7:1).
- **Avatars: three sizes, then stop.** One `AvatarView` and styles: 24dp in lists, 32dp in headers, 40dp on profiles; initials in white on a teal gradient disc when there is no photo; optional online dot (`ui/AvatarView.java`, `Avatar.Initials.*` styles). Converted: member, pending, payment and blocked rows, P2P rows and trades, Learn posts, the drawer and top bar, friends lists, profile and QR, the member sheet, group cards and settings. Left alone on purpose: the photo pickers in Edit profile and Create group (large editing previews) and the group-join icon badge.

## One look across the whole app
- **One set of colours, four roles.** Page (`colorBackground`), card (`colorSurface`), inset inside a card (`colorSurfaceAlt`), and one border (`colorCardStroke`). Every older name (the P2P set, "raised", "deep", "main", "dark", "light", stroke base, popup) is now an alias of one of those, so no screen can drift to its own shade. To retune the look, change only `values/colors.xml` and `values-night/colors.xml`.
- **Cards.** All 194 plain cards in 59 layouts now use the card colour, and a card inside a card uses the inset colour. Cards that are meant to be coloured (warnings, accent banners, status chips) were left as designed. Card corners are on one scale: 8, 12, 16 and 24dp (circles and pills untouched).
- **Pages.** The Welcome screen (it was a separate dark navy page), the group Members and Overview tabs (a different background from the other tabs) and the P2P screens now share the same page colour. The remaining hard-coded navy and charcoal colours in Welcome, the rewards tables and the debug screen use the theme.
- Receipts and exported statements keep their own fixed colours on purpose (they are images).

## Collected total, one receipt design, and unreadable text fixed everywhere
- **Collected on a group.** A rotating circle and a savings group live on the network, so their payments never reached the list the header added up, and it showed 0. The header now reads what the network has: a circle's is the sum of what its members have paid in, a savings group's is what everyone has saved (goal groups already did this). The Contributions column on each member card shows that member's paid-in total for circles.
- **One receipt, shared one way.** A new receipt design (Ink Teal header with the Harvana mark, large amount, status pill, details card, network details, receipt number and a QR of the transaction hash) is drawn by one renderer (`receipt/ReceiptRenderer.java`) and shared by one function (`receipt/ReceiptShare.java`, through the FileProvider, so no storage permission and no leftover files). The success sheet's Share receipt (image or PDF), the transaction page's Image and PDF exports, and the group statement (PDF and image, multi-page, summary and zebra rows) all use it. CSV and DOCX exports now share through the same function.
- **Text that could not be read.** The Sign in button had white text on the Warm Sand button at night. I searched every layout and screen for the same problem: 36 layout texts that were white on a light fill or on a surface (the insurance deposit screen, staking sheets and cards, rewards tables, Add payment method, Send funds, Restore, Unlock card and more) and 30 in code (Sign in and Create account, Verify email buttons, group slots, Learn filters, staking and send sheets, notification messages, status badges, the debug page) now use the theme's on-accent, ink or text colours. Failed transactions used a dark ink on dark; they now use the error colour. White text over photos, video and the camera was left alone.

## Material widgets everywhere
- **Buttons.** Every button is now a Material button: the 12 plain buttons and 17 text views that acted as buttons in layouts, and the 10 built in code (new helper `ui/Btn.java` with filled, tonal, outlined and text styles, used by Sign in, Create account, Verify email, payment confirm, group pay and more). One style in `styles.xml` (rounded 12dp, bold, 48dp tall, ripple) drives them all.
- **Other widgets.** Text fields are Material `TextInputEditText` (83 in layouts, 51 in code), switches `SwitchMaterial` (20 plus 4 in code), checkboxes `MaterialCheckBox` (10 plus 10 in code), 84 hairline lines are `MaterialDivider`, the trade, loan and welcome progress bars are Material progress indicators. Cards and text-field boxes have one theme style each (`Harvana.Card`, `Harvana.TextInputLayout`) so new screens match without extra attributes.
- Left as they were: icon-only buttons (images), notification layouts (the system draws them and cannot use Material widgets), and the stage dots on the payment progress sheet.

## Group members show the names they chose in the app
- A member the network knows only by wallet (no member record linking an app account) was shown as a bare address. The app now looks the wallet up on the public profile cards (the node writes a verified person's wallet onto their card when they link their account) and shows their app name on the Members tab, in the "This round's recipient" line on Overview, and for counterparties in group History (`trust/WalletProfiles.java`, used by `MemberNameResolver`). A member with no linked account still shows "Member · Slot N".
- Together with the Collected total and per-member paid-in amounts from the earlier change, the group page now draws its figures and names from the network and the app profiles.

## Capital letters for typing, and the last Material swaps
- **Capitals in text fields.** Plain text fields now start each sentence with a capital, and names, cities, states, locations and similar fields capitalise every word (`utils/SmartCaps.java`, covering every screen, sheet, dialog and field built in code). Fields that must stay exactly as typed are left alone: email, passwords, user names, wallet addresses and codes, URLs, search boxes, recovery phrases, amounts, phone and ID numbers. If someone types a whole entry in lower case, the first letter of each sentence (or word) is capitalised when they leave the field; text that already has any capital is never changed.
- **Material.** All 40 toolbars are now `MaterialToolbar` and all 52 plain cards are `MaterialCardView` (no border added). Text views become Material text views through the Material theme, and every button, text field, switch, checkbox, divider and progress bar was converted earlier. Not changed: spinners (27), icon images, the stage dots on the payment progress sheet, and notification layouts.

## Even buttons and spacing in the invite and contract sheets
- Join-group contract: Cancel, Back and the sign button are the same width and 50dp high, on one line; the last page's button now reads "Sign contract".
- Invite sheet: Add friends, Username and Contacts are three equal 50dp buttons with gaps; the card, email field, message field and the two bottom buttons have proper margins and side padding.

## More Material
- All 258 icon images in the layouts are now `ShapeableImageView` (notification layouts excluded: they cannot use it).
- The back button in the shared header, the wallet options button and the QR gallery button are Material icon buttons; the loading spinner on the reward table is a Material circular indicator.
- Still not Material: 27 spinners (dropdowns; each needs its own code change to become Material dropdown menus), the stage dots and trust ring (custom drawn), and notification layouts.

## Spacing safety net
- `utils/SmartSpacing.java` runs on every screen, sheet and dialog: buttons and fields that sit directly against a neighbour in a vertical list get an 8dp gap (12dp after a field); two buttons side by side with no gap get 8dp between them. Layouts that already have spacing, toggle groups, divided lists and views tagged `keep_tight` are left alone.

## Material dropdowns
- All 27 spinners (plus two built in code) are now `ui/MaterialSpinner`: the Material outlined-field look with rounded outline, surface fill, 52dp touch target and rounded popup. It keeps the normal Spinner behaviour, so choosing and reading values did not change. Inside a card it stays borderless so there is no double outline.

## Home: Learn Hub card
- The Learn Hub card on Home now reads the same posts as the Learn tab (it was reading an old list, so it always said "No Learn Hub posts yet"). It shows the newest 3 public posts with cover image, title, category, views and how long ago, and tapping opens Learn. The empty and offline messages are padded and use the soft text colour.
- The "Pending approval" button on trending/discover groups now reads "Pending" on one line so it no longer wraps.

## Every receipt has the same facts
- Sharing a receipt from the payment success sheet now reads the transaction from the network by its hash first, so it shows the same full data as the transaction detail receipt: from, to, memo, network fee, burned, hash, block, confirmations and nonce (it used to show only a few rows). If the network cannot be reached it falls back to the short receipt.
- Fixed the duplicated unit in the headline ("0.1 HARV HARV").

## Gifts are recorded like reactions
- When a reader sends a gift on a Learn Hub post, it is now recorded on the post (`LEARN_CONTENT/{post}/gifts/{transfer hash}`: who, to which wallet, amount, time), once per transfer, the same way reactions are recorded. The gift button shows how many gifts the post has received, counted from the records, and updates right after you send one. The author sees the count on their own post.
- **Owner step:** publish the updated `docs/firestore.console.rules` (new `gifts` rule: signed-in users can read; a user can create only their own record; no edits or deletes).

## Join requests no longer stay "Pending" for ages
- The "Pending" mark on a group came from a list on your profile that was only cleaned up if one particular query worked. A request that was approved, rejected, withdrawn or never saved could leave the mark behind forever. Home and Discover now check each marked group's own join request directly and clear every mark that is not a real waiting request, then show Pending only for requests that are still waiting (`JoinRequestManager.reconcilePending`).
- A request that really is waiting stays Pending until the group's admin (or the member vote) answers it. That is up to the group.

## Join requests from "Actions & votes"
- "Actions & votes" opens the network group screen (grants, payouts and their votes). Join requests are not decided there, they are accepted or voted on in the Members tab, so people looking for them found an unrelated screen. That screen now shows a "N people want to join" card with a "Review join requests" button that opens the group's Members tab.

## Join requests: where and how to vote
- The Members tab now always shows a "JOIN REQUESTS" block at the top for admins and, in vote groups, for every member ("JOIN REQUESTS · VOTE HERE"). With a plain line of instructions: tap Vote Yes or Vote No on each request, it passes at 60% yes (with one member, your Yes is enough). When nobody is waiting it says so, instead of the block being invisible.

## Member photos in the Members tab
- Members, people waiting to join and the member detail sheet now show the person's profile photo in the round badge (initials until the photo loads, or when they have none). Member and request rows use the 40dp avatar size so photos are readable (`group/MemberPhoto.java`).

## Members tab no longer reloads on every resume
- Coming back to the Members tab only reloads the list when it is more than a minute old. Accepting, voting, removing and pull-to-refresh still reload straight away.

## Group picture rings
- The coloured ring around group pictures was cut up since the images became Material image views (they erase anything outside their own shape, which included the ring). Group pictures now use the Material round shape with a native border in the group-type colour (blue rotating, green accumulative, purple fixed, grey other), so the ring is complete everywhere (Home, Discover, Groups, invites, profiles, settings).

## Group rings, second fix
- The group pictures that carry the type ring in layouts (Home upcoming and trending, Discover, Groups, group page, join request) are plain image views again, which is what the ring drawing was built for, so the ring is whole. Any group picture that is still a Material image view builds its ring with the view's own border, applied in the right order.

## Home: no Pay button for a goal that no longer takes deposits
- Home's Upcoming payments offered "Pay" for a fixed goal group that had already reached its goal. For a goal group on the network Home now asks the network for the goal's real state before offering Pay: if the goal is reached or the group is completed, the row says "Goal reached - deposits are closed", shows "Closed" and the button is disabled. The resolver also treats a group saved as completed or closed the same way.

## Home Pay button follows the same rules as the Payments tab, for every network group
- Before Home offers "Pay" for a rotating circle or a goal group on the network, it now reads the group and your membership from the network and applies the same rules the Payments tab uses. The button is disabled, with the reason and a short badge (Closed, Paid, Waiting, Not due), when: the goal is reached or the group is completed; the circle is still forming; you already paid this round; it is your turn to be paid; your membership defaulted; or the group is paused. It stays "Pay" only when the group would accept a payment from you.

## Smaller Pay button on Home
- The Pay / Remind / status button on Home's Upcoming payments rows is now a compact 32dp pill (it was drawing at about 56dp tall because of the button style's minimum height and insets).

## Shorter rows on Home
- Upcoming payments and Trending public groups rows are shorter: less vertical padding, a 36dp group picture (was 45dp), slightly smaller title and detail text, a tighter badge, and the Join button is the same compact 32dp pill as Pay.

## Notifications open the page they are about
- Tapping a notification, in the Notifications list or in the phone's notification shade, now goes to that notification's own page through one shared router (`notify/NotificationRouter.java`): a payment or transfer opens that transaction's details (read from the network by its hash), a group notice opens that group on the right tab (a new member opens Members, a loan Loans, a payment reminder Payments, other notices Notice), an invitation opens Invitations, an approved join request opens the group, a declined one opens Discover, a trade message opens My trades, a friend request opens Friends, and a Learn review result opens Learn. Before, the shade always opened the inbox list, and a payment notification only went to the wallet tab.
- Group pages now take the tab as a page (Overview, Members, Payments, Loans, Notice, History), so a group without a Loans tab still opens the right one (Notice used to land on History).

## Transaction notifications show the transaction with its confirmations
- A received, sent or signed-payment notification opens the details of that exact transaction. The transaction lookup on the network does not say how many blocks confirm it, so the router now also reads the transaction status for the confirmation count (it used to open showing zero confirmations).

## Every notification opens its own item
- An invitation notification opens that invitation (the group's details and the Join choice) instead of the invitations list; a published Learn article opens the article; a trade message opens the chat with that trader; a pinch opens the person's profile. Together with the earlier routes (transactions, group notices on the right tab, join results, friend requests) every notification type now lands on the specific thing it is about. Invitations received before this update have no group saved with them and still open the invitations list.

## Discover: location filter (global, country, state, city)
- The location chip on Discover now works. Tapping it opens a Location sheet: **Global** is the default (every group from every country). Optionally pick a **country**; once a country is picked, optionally a **state**; once a state is picked, optionally a **city**. Each list is searchable and starts with an "All ..." row. Groups are filtered to the most specific place chosen (a country shows all of that country's groups, a state only that state's, a city only that city's). "State" and "Lagos State" count as the same place.
- **Apply** saves the choice on the phone, so closing and reopening the app keeps it; the chip shows what is applied (Global, a country, "state, country" or "city, state"). **Reset to global** clears it.
- Discover now loads up to 300 public groups (was 50) so the filter has enough to work with, and the "On network" checkbox re-filters when it is switched off as well as on.
- Groups created without a country, state or city only appear under Global.

## One full-screen search sheet (Groups and Discover)
- Searching on the Groups tab now opens the same full-screen sheet as Discover (before, the search box on Groups did nothing). New shared sheet `ui/SearchSheet.java`: one row, side by side, with a **close** button, the **search field** and a **clear** button. The list behind filters as you type, a line shows how many groups match, the keyboard's search key closes the sheet, close keeps what you typed and clear empties the field. Discover uses the same sheet instead of its old Clear/Search buttons.
- Groups search matches name, type, city, state, country and frequency. The Filter button on Groups now works too: All my groups, Rotating, Accumulative, Fixed contribution, or Groups I run.

## New Group button readable
- The "+ New Group" button on the Groups tab had no text colour, so its label was pale on the sand button and could not be read. It now uses the on-accent ink colour in day and night themes.

## Group notices and notifications: wired up properly
- **Found wrong:** system notices (a member joined, a loan was applied for, a contribution received...) were written with the person who did it as the "target", and the notification code only notified that target. So the one person who caused the event was told, under "Payment reminders", and every other member heard nothing. Votes were worse: the notice pointing at a new vote had no owner at all, so nobody was notified.
- **Now:** every notice says who it is for. A notice about something that happened goes to every member except whoever did it, under Group notices. A payment reminder goes only to the member it names. A new vote (a payout request, milestone, emergency grant, savings pack, or someone asking to join a group that votes) is added to the group's Notice tab with a clear message and notifies every member; tapping it opens Actions & votes for a network group, or the Members tab to vote on a join request. For groups where the admin decides, the join-request notice goes to the admin only. Older notices without this information are treated the same way.
- Notifications reach a phone when the app next syncs (it checks regularly in the background). There is no instant push without a server.
- **Owner step:** publish the updated `docs/firestore.console.rules` (one added rule lets a person who has just asked to join post the single "Join request" notice).

## Duplicates removed from notices and notifications
- **Notice tab:** the old duplicate filter hid any two notices with the same words, even days apart or about different proposals, so a second "New member joined" or a second vote with the same title disappeared. Two notices now count as the same only when they are the same stored notice, the same proposal, or the same words from the same person within two minutes (a retry or double write).
- **Written once:** a vote notice has a fixed id built from the proposal, and a join-request notice one built from the person and the hour, so a retry or a second screen cannot write a second copy.
- **Notifications:** a vote is one notification however many notice documents exist for it; the confirmation of a transfer replaces its "on its way" notification (and copies already in the inbox are tidied when the list opens).
- Separate transfers of the same amount to the same person are different transactions and stay separate.

## Home rows stay one line
- The text in Home's Upcoming payments, Learn Hub and Trending groups rows is now a single line, cut with "…" when it is too long, so every row keeps the same height (the goal-reached message and long article titles used to wrap onto a second line).

## New Actions & votes page; one payment page
- The old full-screen network group page (`NodeGroupActivity`) is deleted. It showed the group's payments, so "Actions & votes" opened what looked like the payment page. The group's **Payments tab** (`GroupPaymentFragmentActivity`) is now the only payment page, and it only offers payments (contribute, deposit, save, start a savings line, join a circle's bond). Requests, withdrawals and votes no longer appear there.
- **Actions & votes** is a new page (`group/GroupActionsActivity`). A summary at the top counts what is waiting. **Needs your attention:** open votes with the amount, the reason, who asked and when, a bar showing the yes share against the percentage needed, how many voters said yes, your own vote and Vote yes / Vote no (you can change a vote until it closes); join requests with the person's photo and name, voted on by members or accepted or rejected by the admin depending on the group; withdrawal requests for the admin or treasurer to approve; defaulted members of a circle with the vote to forgive and the way to put your own default right. **What you can do:** ask for a payout, a milestone release or an emergency grant, request a withdrawal or claim savings, withdraw a bond, start a new goal once a goal group has reached its target. **Recent decisions:** closed votes and how they ended. Pull down to refresh.
- A new request now also posts its notice, so every member is told a vote is waiting.
- Places that opened the old page now go where they belong: join, loan repayment and "continue" after joining open the group's Payments or Loans tab; "Open a circle by ID" finds the group by its network id and opens its page.

## Home lists only groups with something to do
- Upcoming payments on Home now lists only groups where you have a payment to make or a member to remind. Groups with nothing due (idle, goal reached, already paid, not your turn) are no longer shown there; when none is left the card shows its usual "no upcoming payments" message. The groups are all still under the Groups tab.

## Success sheet and receipt show the amount and the details
- A deposit, contribution or savings payment on a group showed no amount, only "From", on the success sheet and on the shared receipt (the network's answer to these requests carries no amount). The success sheet and receipt now use what you reviewed and approved: the amount, the group, where it goes and what it is for, with your wallet as "From". The network fee stays its own line.
