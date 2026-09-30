package com.harvanna.app.firestore;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
* Firestore access for everything group-related - the group document itself,
* plus its subcollections (members, notices, payments, loans, joinRequests,
* payoutRequests, private) and a few top-level collections - via the native
* Firestore SDK (migrated from hand-built REST/OkHttp calls). The SDK attaches
* the signed-in user's ID token itself and provides offline persistence (see
* SketchApplication).
*
* The public API and the error-string contract are unchanged: a missing
* document is reported as "HTTP 404: ...", a permission failure as
* "HTTP 403: ...", a connectivity failure as "Network error: ..." (callers
* key on these). Callbacks are delivered on a background thread, as they were
* with the OkHttp version, so callers that switch to the UI thread themselves
* keep working.
*
* Offline writes: the SDK queues a write locally and completes its Task only
* once the server acknowledges it. To keep the old "fail fast when offline"
* contract, a write not acknowledged within WRITE_TIMEOUT_MS is reported as a
* "Network error" - the write stays queued in the SDK and syncs on reconnect.
* Writes with fixed ids (set / field-masked merge) are idempotent; auto-id
* adds (notices, payments, loans, ...) could in theory be applied later than
* reported, which is why callers treat them as best-effort or re-read state.
*
* Data model:
*   GROUPS/{groupId}                        - core group fields
*   GROUPS/{groupId}/members/{uid}           - one doc per member
*   GROUPS/{groupId}/notices/{noticeId}      - one doc per notice
*   GROUPS/{groupId}/payments/{paymentId}    - one doc per payment record
*   GROUPS/{groupId}/loans/{loanId}          - one doc per loan
*/
public class GroupFirestoreManager {
	
	private static final String TAG = "GroupFirestoreManager";
	private static final long WRITE_TIMEOUT_MS = 15_000L;
	private static final Executor BG = Executors.newCachedThreadPool();
	private static final ScheduledExecutorService TIMER = Executors.newSingleThreadScheduledExecutor();
	
	private static GroupFirestoreManager instance;
	private final Context appContext;
	
	public static synchronized GroupFirestoreManager getInstance(Context context) {
		if (instance == null) {
			instance = new GroupFirestoreManager(context.getApplicationContext());
		}
		return instance;
	}
	
	private GroupFirestoreManager(Context context) {
		this.appContext = context;
	}
	
	// ── Callback interfaces ─────────────────────────────────────────────
	
	public interface DocCallback {
		void onSuccess(Map<String, Object> data);
		void onError(String error);
	}
	
	public interface ListCallback {
		void onSuccess(List<Map<String, Object>> items);
		void onError(String error);
	}
	
	public interface IdCallback {
		void onSuccess(String generatedId);
		void onError(String error);
	}
	
	public interface SimpleCallback {
		void onSuccess();
		void onError(String error);
	}
	
	// ── Group document ──────────────────────────────────────────────────
	
	public void createGroup(String groupId, Map<String, Object> fields, SimpleCallback callback) {
		set(doc("GROUPS", groupId), fields, callback);
	}
	
	/** Lets Firestore generate a collision-free ID rather than the client inventing one. */
	public void createGroupWithAutoId(Map<String, Object> fields, IdCallback callback) {
		add(col("GROUPS"), fields, callback);
	}
	
	public void getGroup(String groupId, DocCallback callback) {
		get(doc("GROUPS", groupId), callback);
	}
	
	public void updateGroupFields(String groupId, Map<String, Object> fields, SimpleCallback callback) {
		merge(doc("GROUPS", groupId), fields, callback);
	}
	
	/** Equality query on one string field of the top-level GROUPS collection. */
	public void queryGroups(String fieldName, String fieldValue, int limit, ListCallback callback) {
		query(FirebaseFirestore.getInstance().collection("GROUPS").whereEqualTo(fieldName, fieldValue).limit(limit), false, callback);
	}
	
	/**
	 * Collection-group query across every GROUPS/{anyGroupId}/joinRequests
	 * subcollection for docs where requesterUid == the signed-in caller. Lets
	 * a member's own device discover "one of my join requests got approved
	 * somewhere" - each result doc already carries groupId and status.
	 */
	public void queryMyJoinRequests(String requesterUid, ListCallback callback) {
		query(FirebaseFirestore.getInstance().collectionGroup("joinRequests")
				.whereEqualTo("requesterUid", requesterUid).limit(50), false, callback);
	}

	public void queryMyMemberships(String uid, ListCallback callback) {
		// groupId is taken from the document path GROUPS/{groupId}/members/{uid}.
		query(FirebaseFirestore.getInstance().collectionGroup("members")
				.whereEqualTo("memberUid", uid).limit(100), true, callback);
	}
	
	// ── Join requests subcollection ─────────────────────────────────────
	
	public void addJoinRequest(String groupId, String requesterUid, Map<String, Object> fields, IdCallback callback) {
		set(doc("GROUPS", groupId, "joinRequests", requesterUid), fields, new SimpleCallback() {
			@Override public void onSuccess() { callback.onSuccess(requesterUid); }
			@Override public void onError(String error) { callback.onError(error); }
		});
	}
	
	public void getJoinRequests(String groupId, ListCallback callback) {
		list(col("GROUPS", groupId, "joinRequests"), callback);
	}

	public void getJoinRequest(String groupId, String requesterUid, DocCallback callback) {
		get(doc("GROUPS", groupId, "joinRequests", requesterUid), callback);
	}

	public void updateJoinRequestFields(String groupId, String reqId, Map<String, Object> fields, SimpleCallback callback) {
		merge(doc("GROUPS", groupId, "joinRequests", reqId), fields, callback);
	}


	public interface VoteCallback {
		/** resolution is "PENDING", "APPROVED", or "REJECTED" — APPROVED
		 *  means this call already added the requester as a real member;
		 *  the caller doesn't need to do that separately. */
		void onVoteCast(String resolution, int yesVotes, int noVotes, int totalMembers);
		void onError(String error);
	}

	/**
	 * Casts one member's vote on a pending join request, then checks whether
	 * the outcome is now mathematically certain — approved the moment YES
	 * votes alone reach 60% of total membership, rejected the moment 60%
	 * becomes impossible even if every remaining member voted YES. No
	 * timeout; a request resolves exactly when its outcome stops being in
	 * doubt, not before and not later.
	 *
	 * Known limitation: this reads the current vote map, computes locally,
	 * then writes it back — not a real Firestore transaction. Two members
	 * voting in the same instant near the threshold could theoretically
	 * both read the same "before" state. Low-probability given how
	 * infrequently join votes happen, but a real fix would need Firestore's
	 * :commit transaction API, a heavier REST pattern than anything else
	 * in this class currently uses.
	 */
	public void castJoinVote(String groupId, String requesterUid, String voterUid, boolean approve, VoteCallback callback) {
		// Prefer Cloud Function (transactional threshold + admit). Fall back to local race-prone path.
		com.harvanna.app.group.GroupCloudFunctions.castJoinVote(groupId, requesterUid, approve,
				new com.harvanna.app.group.GroupCloudFunctions.Callback() {
					@Override
					public void onSuccess(org.json.JSONObject data) {
						if (callback == null) return;
						String resolution = data.optString("resolution", "PENDING");
						int yes = data.optInt("yes", 0);
						int no = data.optInt("no", 0);
						int total = data.optInt("totalMembers", 0);
						callback.onVoteCast(resolution, yes, no, total);
					}

					@Override
					public void onError(String error) {
						android.util.Log.w("GroupFirestoreManager",
								"castJoinVote cloud failed, local fallback: " + error);
						castJoinVoteLocal(groupId, requesterUid, voterUid, approve, callback);
					}
				});
	}

	/** Legacy client-side vote path — used only if Cloud Functions are unreachable. */
	private void castJoinVoteLocal(String groupId, String requesterUid, String voterUid, boolean approve, VoteCallback callback) {
		getGroup(groupId, new DocCallback() {
			@Override
			public void onSuccess(Map<String, Object> groupData) {
				Object countObj = groupData.get("membersCount");
				int totalMembers = countObj instanceof Number ? ((Number) countObj).intValue() : 0;
				if (totalMembers <= 0) {
					callback.onError("Could not determine current membership count.");
					return;
				}
				Object creatorObj = groupData.get("creatorId");
				String creatorId = creatorObj != null ? creatorObj.toString() : "";

				getJoinRequest(groupId, requesterUid, new DocCallback() {
					@Override
					public void onSuccess(Map<String, Object> request) {
						Object statusObj = request.get("status");
						String status = statusObj != null ? statusObj.toString() : "PENDING";
						if (!"PENDING".equalsIgnoreCase(status)) {
							callback.onError("This request has already been resolved.");
							return;
						}

						Map<String, Object> votes = new HashMap<>();
						Object votesObj = request.get("votes");
						if (votesObj instanceof Map) {
							//noinspection unchecked
							votes.putAll((Map<String, Object>) votesObj);
						}

						if (votes.containsKey(voterUid)) {
							callback.onError("You've already voted on this request.");
							return;
						}
						votes.put(voterUid, approve);

						int[] tally = new int[2]; // [0]=yes, [1]=no - counted here, never touched again after
						for (Object v : votes.values()) {
							if (Boolean.TRUE.equals(v)) tally[0]++; else tally[1]++;
						}
						final int finalYesVotes = tally[0];
						final int finalNoVotes = tally[1];
						int totalVotesCast = finalYesVotes + finalNoVotes;

						String resolution;
						if (totalMembers == 2 && !creatorId.isEmpty()) {
							// Special case: a strict equal-weight 60% threshold
							// in a 2-person group effectively requires unanimous
							// agreement, since one vote alone is only 50%. Give
							// the admin's vote real deciding weight instead -
							// 60/40 - so the admin alone can resolve a request
							// without needing the member's agreement, while the
							// member's vote alone can never reach the threshold.
							double weightedYes = 0, weightedTotal = 0;
							for (Map.Entry<String, Object> e : votes.entrySet()) {
								double weight = creatorId.equals(e.getKey()) ? 60.0 : 40.0;
								weightedTotal += weight;
								if (Boolean.TRUE.equals(e.getValue())) weightedYes += weight;
							}
							// Remaining weight (from whoever hasn't voted yet) -
							// same "could this still swing to yes" check as the
							// equal-weight path below, just in weighted terms.
							double remainingWeight = 100.0 - weightedTotal;
							double maxPossibleWeightedYes = weightedYes + remainingWeight;

							if (weightedYes >= 60.0) {
								resolution = "APPROVED";
							} else if (maxPossibleWeightedYes < 60.0) {
								resolution = "REJECTED";
							} else {
								resolution = "PENDING";
							}
						} else {
							int yesNeeded = (int) Math.ceil(0.60 * totalMembers);
							int maxPossibleYes = finalYesVotes + (totalMembers - totalVotesCast);
							if (finalYesVotes >= yesNeeded) {
								resolution = "APPROVED";
							} else if (maxPossibleYes < yesNeeded) {
								resolution = "REJECTED";
							} else {
								resolution = "PENDING";
							}
						}

						Map<String, Object> updateFields = new HashMap<>();
						updateFields.put("votes", votes);
						updateFields.put("status", resolution);
						if (!"PENDING".equals(resolution)) {
							updateFields.put("reviewedAt", System.currentTimeMillis());
						}

						addJoinRequest(groupId, requesterUid, updateFields, new IdCallback() {
							@Override
							public void onSuccess(String id) {
								if ("APPROVED".equals(resolution)) {
									_admitVotedMember(groupId, requesterUid, totalMembers, () ->
										callback.onVoteCast(resolution, finalYesVotes, finalNoVotes, totalMembers));
								} else {
									callback.onVoteCast(resolution, finalYesVotes, finalNoVotes, totalMembers);
								}
							}
							@Override public void onError(String error) { callback.onError(error); }
						});
					}
					@Override public void onError(String error) { callback.onError(error); }
				});
			}
			@Override public void onError(String error) { callback.onError(error); }
		});
	}

	/**
	 * Approval doesn't just flip a status flag — it actually adds the
	 * requester as a real member and runs the full contract side-effects
	 * (member doc + membersCount + profile + system notice) via the shared
	 * JoinRequestManager.admitMember helper so admin-approve and vote-approve
	 * always produce identical final state.
	 */
	private void _admitVotedMember(String groupId, String newMemberUid, int currentCount, Runnable onDone) {
		int slot = currentCount + 1;
		int newCount = currentCount + 1;
		com.harvanna.app.group.JoinRequestManager.admitMember(
				appContext,
				this,
				groupId,
				newMemberUid,
				slot,
				newCount,
				new com.harvanna.app.group.JoinRequestManager.Callback() {
					@Override
					public void onSuccess(String message) {
						onDone.run();
					}
					@Override
					public void onError(String error) {
						// Best-effort: still unblock the vote callback
						android.util.Log.w(TAG, "Vote admission side-effects incomplete: " + error);
						onDone.run();
					}
				});
	}

	public void addMember(String groupId, String memberUid, Map<String, Object> fields, SimpleCallback callback) {
		set(doc("GROUPS", groupId, "members", memberUid), fields, callback);
	}

	/** Masked partial update to specific fields on a member doc - addMember()
	 *  replaces the whole document, which would wipe slot/joinedAt/status/role. */
	public void updateMemberFields(String groupId, String memberUid, Map<String, Object> fields, SimpleCallback callback) {
		merge(doc("GROUPS", groupId, "members", memberUid), fields, callback);
	}
	
	public void getMembers(String groupId, ListCallback callback) {
		list(col("GROUPS", groupId, "members"), callback);
	}

	/** Single member doc lookup - e.g. resolving the signed-in user's own slot. */
	public void getMember(String groupId, String memberUid, DocCallback callback) {
		get(doc("GROUPS", groupId, "members", memberUid), callback);
	}
	
	/**
	 * Members-only private documents (GROUPS/{gid}/private/{docId}), e.g. the
	 * encrypted wallet-card backup. Rules must restrict reads to members.
	 */
	public void setPrivateDoc(String groupId, String docId, Map<String, Object> fields, SimpleCallback callback) {
		set(doc("GROUPS", groupId, "private", docId), fields, callback);
	}

	public void getPrivateDoc(String groupId, String docId, DocCallback callback) {
		get(doc("GROUPS", groupId, "private", docId), callback);
	}

	// ── Generic top-level documents (e.g. INVITES) ─────────────────────

	public void setTopLevelDoc(String collection, String docId, Map<String, Object> fields, SimpleCallback callback) {
		set(doc(collection, docId), fields, callback);
	}

	public void updateTopLevelFields(String collection, String docId, Map<String, Object> fields, SimpleCallback callback) {
		merge(doc(collection, docId), fields, callback);
	}

	/** Equality query on one string field of a top-level collection. */
	public void queryTopLevel(String collection, String field, String value, int limit, ListCallback callback) {
		query(FirebaseFirestore.getInstance().collection(collection).whereEqualTo(field, value).limit(limit), false, callback);
	}

	// ── Notices subcollection ───────────────────────────────────────────
	
	public void addNotice(String groupId, Map<String, Object> fields, IdCallback callback) {
		add(col("GROUPS", groupId, "notices"), fields, callback);
	}

	/**
	 * Auto-posted activity notices - contributions, payouts, new members -
	 * broadcast through the same notices feed a manually-posted announcement
	 * uses. Deliberately fire-and-forget: a failed notice write shouldn't
	 * ever block or fail the real action that triggered it.
	 */
	public void postSystemNotice(String groupId, String title, String message) {
		Map<String, Object> fields = com.harvanna.app.group.GroupNoticeHelper.buildSystem(title, message);
		addNotice(groupId, fields, new IdCallback() {
			@Override public void onSuccess(String id) { /* best-effort */ }
			@Override public void onError(String error) { /* best-effort */ }
		});
	}

	// -- Member reports - user-side only, moderation is separate work ---
	// A top-level collection: reports are about a member's behavior across
	// every group. Every report is created PENDING and TrustScoreEngine only
	// ever reads CONFIRMED reports.

	public interface ReportCallback {
		void onSuccess(String reportId);
		void onError(String error);
	}

	public void submitReport(String reportedUid, String reporterUid, String groupId,
	                          String groupName, String reason, String details,
	                          ReportCallback callback) {
		Map<String, Object> fields = new HashMap<>();
		fields.put("reportedUid", reportedUid);
		fields.put("reporterUid", reporterUid);
		fields.put("groupId", groupId);
		fields.put("groupName", groupName != null ? groupName : "");
		fields.put("reason", reason);
		fields.put("details", details != null ? details : "");
		fields.put("status", "PENDING");
		fields.put("createdAt", System.currentTimeMillis());
		add(col("MEMBER_REPORTS"), fields, new IdCallback() {
			@Override public void onSuccess(String id) { callback.onSuccess(id); }
			@Override public void onError(String error) { callback.onError(error); }
		});
	}

	public void getNotices(String groupId, ListCallback callback) {
		list(col("GROUPS", groupId, "notices"), callback);
	}
	
	// ── Payments subcollection ──────────────────────────────────────────
	
	public void addPayment(String groupId, Map<String, Object> fields, IdCallback callback) {
		add(col("GROUPS", groupId, "payments"), fields, callback);
	}

	/** Updates a payment doc after creation - flips status from PENDING to
	 *  CONFIRMED once a real confirmation happens. */
	public void updatePaymentStatus(String groupId, String paymentDocId, Map<String, Object> fields, SimpleCallback callback) {
		merge(doc("GROUPS", groupId, "payments", paymentDocId), fields, callback);
	}
	
	public void getPayments(String groupId, ListCallback callback) {
		list(col("GROUPS", groupId, "payments"), callback);
	}
	
	// ── Loans subcollection ─────────────────────────────────────────────
	
	public void addLoan(String groupId, Map<String, Object> fields, IdCallback callback) {
		add(col("GROUPS", groupId, "loans"), fields, callback);
	}
	
	public void getLoans(String groupId, ListCallback callback) {
		list(col("GROUPS", groupId, "loans"), callback);
	}

	/** Single-loan fetch - re-validates a loan's real state before accepting a repayment. */
	public void getLoan(String groupId, String loanId, DocCallback callback) {
		get(doc("GROUPS", groupId, "loans", loanId), callback);
	}
	
	public void addPayoutRequest(String groupId, Map<String, Object> fields, IdCallback callback) {
		add(col("GROUPS", groupId, "payoutRequests"), fields, callback);
	}
	
	public void getPayoutRequests(String groupId, ListCallback callback) {
		list(col("GROUPS", groupId, "payoutRequests"), callback);
	}
	
	/** Single-request fetch - re-validates a payout request's real status before approving. */
	public void getPayoutRequest(String groupId, String requestId, DocCallback callback) {
		get(doc("GROUPS", groupId, "payoutRequests", requestId), callback);
	}
	
	public void updatePayoutRequest(String groupId, String requestId, Map<String, Object> fields, SimpleCallback callback) {
		merge(doc("GROUPS", groupId, "payoutRequests", requestId), fields, callback);
	}
	
	public void updateLoan(String groupId, String loanId, Map<String, Object> fields, SimpleCallback callback) {
		merge(doc("GROUPS", groupId, "loans", loanId), fields, callback);
	}

	// ── Learn Hub content (top-level collection) ───────────────────────

	/**
	 * Submits a Learn Hub article for automatic review (LEARN_SUBMISSIONS, status PENDING). Apps can no
	 * longer write LEARN_CONTENT directly: only the node's review service publishes approved items there.
	 */
	public void addLearnSubmission(Map<String, Object> fields, IdCallback callback) {
		add(col("LEARN_SUBMISSIONS"), fields, callback);
	}

	/** The signed-in user's own submissions with the review verdict (status, reason). */
	public void getMyLearnSubmissions(String uid, ListCallback callback) {
		queryTopLevel("LEARN_SUBMISSIONS", "authorUid", uid, 50, callback);
	}

	/** Lists every Learn Hub content doc - a global feed. */
	public void getLearnContent(ListCallback callback) {
		list(col("LEARN_CONTENT"), callback);
	}

 	/**
	 * Removes a group and everything under it (members, notices, payments, loans, joinRequests, payoutRequests,
	 * private). The group document goes LAST because the security rules that authorise the sub-deletes look at
	 * it. Only for app-only groups: a group that exists on the network is closed, never deleted.
	 */
	public void deleteGroupTree(String groupId, SimpleCallback cb) {
		String err = precheck();
		if (err != null) { deliver(() -> cb.onError(err)); return; }
		BG.execute(() -> {
			try {
				FirebaseFirestore db = FirebaseFirestore.getInstance();
				String[] subs = {"members", "notices", "payments", "loans", "joinRequests", "payoutRequests", "private"};
				for (String sub : subs) {
					QuerySnapshot snap = com.google.android.gms.tasks.Tasks.await(
						db.collection("GROUPS").document(groupId).collection(sub).get(), 30, TimeUnit.SECONDS);
					com.google.firebase.firestore.WriteBatch batch = db.batch();
					int n = 0;
					for (DocumentSnapshot d : snap.getDocuments()) {
						batch.delete(d.getReference());
						if (++n == 400) { com.google.android.gms.tasks.Tasks.await(batch.commit(), 30, TimeUnit.SECONDS); batch = db.batch(); n = 0; }
					}
					if (n > 0) com.google.android.gms.tasks.Tasks.await(batch.commit(), 30, TimeUnit.SECONDS);
				}
				com.google.android.gms.tasks.Tasks.await(db.collection("GROUPS").document(groupId).delete(), 30, TimeUnit.SECONDS);
				cb.onSuccess();
			} catch (Exception e) {
				Throwable c = e.getCause() != null ? e.getCause() : e;
				cb.onError(c instanceof Exception ? describe((Exception) c) : String.valueOf(c.getMessage()));
			}
		});
	}

	// ── SDK plumbing ────────────────────────────────────────────────────

	private static DocumentReference doc(String... path) {
		return FirebaseFirestore.getInstance().document(join(path));
	}

	private static CollectionReference col(String... path) {
		return FirebaseFirestore.getInstance().collection(join(path));
	}

	private static String join(String[] path) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < path.length; i++) {
			if (path[i] == null || path[i].isEmpty()) throw new IllegalArgumentException("Missing id in " + path[0] + " path.");
			if (i > 0) sb.append('/');
			sb.append(path[i]);
		}
		return sb.toString();
	}

	/** Signed-in check shared by every call (rules need an authenticated user). */
	private static String precheck() {
		return FirebaseAuth.getInstance().getCurrentUser() == null ? "Not signed in \u2014 no current Firebase user." : null;
	}

	private void set(DocumentReference ref, Map<String, Object> fields, SimpleCallback cb) {
		String err = precheck();
		if (err != null) { deliver(() -> cb.onError(err)); return; }
		try {
			await(ref.set(clean(fields)), id -> cb.onSuccess(), cb::onError);
		} catch (Exception e) { deliver(() -> cb.onError("HTTP 400: " + e.getMessage())); }
	}

	/** Field-masked merge: only the given top-level fields change; the doc is created if absent. */
	private void merge(DocumentReference ref, Map<String, Object> fields, SimpleCallback cb) {
		String err = precheck();
		if (err != null) { deliver(() -> cb.onError(err)); return; }
		if (fields == null || fields.isEmpty()) { deliver(cb::onSuccess); return; }
		try {
			await(ref.set(clean(fields), SetOptions.mergeFields(new ArrayList<>(fields.keySet()))), id -> cb.onSuccess(), cb::onError);
		} catch (Exception e) { deliver(() -> cb.onError("HTTP 400: " + e.getMessage())); }
	}

	private void add(CollectionReference ref, Map<String, Object> fields, IdCallback cb) {
		String err = precheck();
		if (err != null) { deliver(() -> cb.onError(err)); return; }
		try {
			final DocumentReference newRef = ref.document(); // id is known before the write completes
			await(newRef.set(clean(fields)), id -> cb.onSuccess(newRef.getId()), cb::onError);
		} catch (Exception e) { deliver(() -> cb.onError("HTTP 400: " + e.getMessage())); }
	}

	private void get(DocumentReference ref, DocCallback cb) {
		String err = precheck();
		if (err != null) { deliver(() -> cb.onError(err)); return; }
		ref.get().addOnCompleteListener(BG, task -> {
			if (!task.isSuccessful()) { cb.onError(describe(task.getException())); return; }
			DocumentSnapshot snap = task.getResult();
			if (snap == null || !snap.exists()) { cb.onError("HTTP 404: document " + ref.getPath() + " not found"); return; }
			cb.onSuccess(read(snap.getData()));
		});
	}

	private void list(CollectionReference ref, ListCallback cb) { query(ref, false, cb); }

	private void query(Query q, boolean addGroupId, ListCallback cb) {
		String err = precheck();
		if (err != null) { deliver(() -> cb.onError(err)); return; }
		q.get().addOnCompleteListener(BG, task -> {
			if (!task.isSuccessful()) { cb.onError(describe(task.getException())); return; }
			List<Map<String, Object>> out = new ArrayList<>();
			QuerySnapshot snap = task.getResult();
			if (snap != null) {
				for (QueryDocumentSnapshot d : snap) {
					Map<String, Object> m = read(d.getData());
					m.put("_docId", d.getId());
					if (addGroupId) {
						DocumentReference group = d.getReference().getParent().getParent();
						if (group != null) m.put("groupId", group.getId());
					}

  // ── SDK plumbing ────────────────────────────────────────────────────

	private static DocumentReference doc(String... path) {
		return FirebaseFirestore.getInstance().document(join(path));
	}

	private static CollectionReference col(String... path) {
		return FirebaseFirestore.getInstance().collection(join(path));
	}

	private static String join(String[] path) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < path.length; i++) {
			if (path[i] == null || path[i].isEmpty()) throw new IllegalArgumentException("Missing id in " + path[0] + " path.");
			if (i > 0) sb.append('/');
			sb.append(path[i]);
		}
		return sb.toString();
	}

	/** Signed-in check shared by every call (rules need an authenticated user). */
	private static String precheck() {
		return FirebaseAuth.getInstance().getCurrentUser() == null ? "Not signed in \u2014 no current Firebase user." : null;
	}

	private void set(DocumentReference ref, Map<String, Object> fields, SimpleCallback cb) {
		String err = precheck();
		if (err != null) { deliver(() -> cb.onError(err)); return; }
		try {
			await(ref.set(clean(fields)), id -> cb.onSuccess(), cb::onError);
		} catch (Exception e) { deliver(() -> cb.onError("HTTP 400: " + e.getMessage())); }
	}

	/** Field-masked merge: only the given top-level fields change; the doc is created if absent. */
	private void merge(DocumentReference ref, Map<String, Object> fields, SimpleCallback cb) {
		String err = precheck();
		if (err != null) { deliver(() -> cb.onError(err)); return; }
		if (fields == null || fields.isEmpty()) { deliver(cb::onSuccess); return; }
		try {
			await(ref.set(clean(fields), SetOptions.mergeFields(new ArrayList<>(fields.keySet()))), id -> cb.onSuccess(), cb::onError);
		} catch (Exception e) { deliver(() -> cb.onError("HTTP 400: " + e.getMessage())); }
	}

	private void add(CollectionReference ref, Map<String, Object> fields, IdCallback cb) {
		String err = precheck();
		if (err != null) { deliver(() -> cb.onError(err)); return; }
		try {
			final DocumentReference newRef = ref.document(); // id is known before the write completes
			await(newRef.set(clean(fields)), id -> cb.onSuccess(newRef.getId()), cb::onError);
		} catch (Exception e) { deliver(() -> cb.onError("HTTP 400: " + e.getMessage())); }
	}

	private void get(DocumentReference ref, DocCallback cb) {
		String err = precheck();
		if (err != null) { deliver(() -> cb.onError(err)); return; }
		ref.get().addOnCompleteListener(BG, task -> {
			if (!task.isSuccessful()) { cb.onError(describe(task.getException())); return; }
			DocumentSnapshot snap = task.getResult();
			if (snap == null || !snap.exists()) { cb.onError("HTTP 404: document " + ref.getPath() + " not found"); return; }
			cb.onSuccess(read(snap.getData()));
		});
	}

	private void list(CollectionReference ref, ListCallback cb) { query(ref, false, cb); }

	private void query(Query q, boolean addGroupId, ListCallback cb) {
		String err = precheck();
		if (err != null) { deliver(() -> cb.onError(err)); return; }
		q.get().addOnCompleteListener(BG, task -> {
			if (!task.isSuccessful()) { cb.onError(describe(task.getException())); return; }
			List<Map<String, Object>> out = new ArrayList<>();
			QuerySnapshot snap = task.getResult();
			if (snap != null) {
				for (QueryDocumentSnapshot d : snap) {
					Map<String, Object> m = read(d.getData());
					m.put("_docId", d.getId());
					if (addGroupId) {
						DocumentReference group = d.getReference().getParent().getParent();
						if (group != null) m.put("groupId", group.getId());
					}
					out.add(m);
				}
			}
			cb.onSuccess(out);
		});
	}

	private interface Ok { void run(String id); }
	private interface Fail { void run(String error); }

	/** Reports exactly one result: the SDK's ack, or a network error after WRITE_TIMEOUT_MS. */
	private <T> void await(Task<T> task, Ok ok, Fail fail) {
		final AtomicBoolean done = new AtomicBoolean(false);
		TIMER.schedule(() -> {
			if (done.compareAndSet(false, true)) {
				BG.execute(() -> fail.run("Network error: write not acknowledged yet (queued; will sync when back online)"));
			}
		}, WRITE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
		task.addOnCompleteListener(BG, t -> {
			if (!done.compareAndSet(false, true)) return;
			if (t.isSuccessful()) ok.run(null);
			else fail.run(describe(t.getException()));
		});
	}

	private static void deliver(Runnable r) { BG.execute(r); }

	/** Maps SDK failures onto the app's existing "HTTP <code>: " / "Network error: " strings. */
	private static String describe(Exception e) {
		if (e instanceof FirebaseFirestoreException) {
			FirebaseFirestoreException fe = (FirebaseFirestoreException) e;
			String msg = fe.getMessage() != null ? fe.getMessage() : fe.getCode().name();
			switch (fe.getCode()) {
				case NOT_FOUND:           return "HTTP 404: " + msg;
				case PERMISSION_DENIED:   return "HTTP 403: " + msg;
				case UNAUTHENTICATED:     return "HTTP 401: " + msg;
				case UNAVAILABLE:
				case DEADLINE_EXCEEDED:
				case CANCELLED:           return "Network error: " + msg;
				case INVALID_ARGUMENT:
				case FAILED_PRECONDITION:
				case ALREADY_EXISTS:      return "HTTP 400: " + msg;
				default:                  return "HTTP 500: " + msg;
			}
		}
		return "Network error: " + (e != null && e.getMessage() != null ? e.getMessage() : "unknown error");
	}

	// ── Value conversion (same types the REST encoder/decoder used) ─────

	/** Writes: null/String/Integer/Long/Double/Boolean/List/Map only; anything else is stringified. */
	@SuppressWarnings("unchecked")
	static Object cleanValue(Object v) {
		if (v == null || v instanceof String || v instanceof Boolean) return v;
		if (v instanceof Integer || v instanceof Long) return v;
		if (v instanceof Double) return v;
		if (v instanceof Float) return ((Float) v).doubleValue();
		if (v instanceof Short || v instanceof Byte) return ((Number) v).longValue();
		if (v instanceof List) {
			List<Object> out = new ArrayList<>();
			for (Object o : (List<Object>) v) out.add(cleanValue(o));
			return out;
		}
		if (v instanceof Map) {
			Map<String, Object> out = new HashMap<>();
			for (Map.Entry<String, Object> e : ((Map<String, Object>) v).entrySet()) out.put(String.valueOf(e.getKey()), cleanValue(e.getValue()));
			return out;
		}
		return String.valueOf(v);
	}

	/** Package/shared helper: normalises a map for a native write. */
	public static Map<String, Object> cleanForWrite(Map<String, Object> fields) { return clean(fields); }

	/** Shared helper: normalises SDK data for callers (timestamps to millis, etc.). */
	public static Map<String, Object> readFromSdk(Map<String, Object> data) { return read(data); }

	static Map<String, Object> clean(Map<String, Object> fields) {
		Map<String, Object> out = new HashMap<>();
		if (fields != null) for (Map.Entry<String, Object> e : fields.entrySet()) out.put(e.getKey(), cleanValue(e.getValue()));
		return out;
	}

	/** Reads: timestamps become epoch millis, other SDK types become strings; maps and lists stay maps and lists. */
	@SuppressWarnings("unchecked")
	static Object readValue(Object v) {
		if (v == null || v instanceof String || v instanceof Boolean || v instanceof Long || v instanceof Double) return v;
		if (v instanceof Number) return ((Number) v).longValue();
		if (v instanceof Timestamp) return ((Timestamp) v).toDate().getTime();
		if (v instanceof List) {
			List<Object> out = new ArrayList<>();
			for (Object o : (List<Object>) v) out.add(readValue(o));
			return out;
		}
		if (v instanceof Map) {
			Map<String, Object> out = new HashMap<>();
			for (Map.Entry<String, Object> e : ((Map<String, Object>) v).entrySet()) out.put(String.valueOf(e.getKey()), readValue(e.getValue()));
			return out;
		}
		return String.valueOf(v);
	}

	static Map<String, Object> read(Map<String, Object> data) {
		Map<String, Object> out = new HashMap<>();
		if (data != null) for (Map.Entry<String, Object> e : data.entrySet()) out.put(e.getKey(), readValue(e.getValue()));
		return out;
	}
}
