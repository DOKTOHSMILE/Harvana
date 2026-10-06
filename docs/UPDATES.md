# Updates

**Standing rule (owner instruction, 1 Oct 2026).** Every change pushed to this repository that an operator has to apply by hand gets a dated entry in this file, in the same commit. That includes a rules publish, a config key, and an app build the phone must install. A code change that cannot go live by itself is unfinished without the entry. The node repository keeps its own steps in `docs/DEPLOYMENT.md`. Do not put those steps here.

## 6 Oct 2026: bottom sheets instead of dialogs, everywhere

What the app does. The 78 places that opened a pop-up dialog (confirmations, "Deposit amount", PIN entry, pickers, "type CLOSE to confirm", and so on) now open a Material bottom sheet that rises from the bottom, with a small grab bar and rounded top corners, and keeps the same buttons. Every bottom sheet that was already in the app (more than a hundred) now goes through one shared base (`AppSheet`) so they all have the same 24dp rounded corners, open fully, and shrink above the keyboard. A sheet with a text field puts the cursor in the first field and brings the keyboard up, and the field and the buttons stay above the keyboard so you can see and edit what you type. Square-backed content is clipped to the curve. The little "Working..." progress pop-ups are not changed. What you do: install the new build.

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
