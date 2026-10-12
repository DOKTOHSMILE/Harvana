package com.harvana.app.firestore;

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
	
	public void createGroup(String groupId, Map<String, Object> fields, SimpleCallback callback) {
		set(doc("GROUPS", groupId), fields, callback);
	}
	
	public void createGroupWithAutoId(Map<String, Object> fields, IdCallback callback) {
		add(col("GROUPS"), fields, callback);
	}
	
	public void getGroup(String groupId, DocCallback callback) {
		get(doc("GROUPS", groupId), callback);
	}
	
	public void updateGroupFields(String groupId, Map<String, Object> fields, SimpleCallback callback) {
		merge(doc("GROUPS", groupId), fields, callback);
	}
	
	public void queryGroups(String fieldName, String fieldValue, int limit, ListCallback callback) {
		query(FirebaseFirestore.getInstance().collection("GROUPS").whereEqualTo(fieldName, fieldValue).limit(limit), false, callback);
	}
	
	public void queryMyJoinRequests(String requesterUid, ListCallback callback) {
		query(FirebaseFirestore.getInstance().collectionGroup("joinRequests")
				.whereEqualTo("requesterUid", requesterUid).limit(50), false, callback);
	}

	public void queryMyMemberships(String uid, ListCallback callback) {
		query(FirebaseFirestore.getInstance().collectionGroup("members")
				.whereEqualTo("memberUid", uid).limit(100), true, callback);
	}
	
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
		void onVoteCast(String resolution, int yesVotes, int noVotes, int totalMembers);
		void onError(String error);
	}

	public void castJoinVote(String groupId, String requesterUid, String voterUid, boolean approve, VoteCallback callback) {
		com.harvana.app.group.GroupCloudFunctions.castJoinVote(groupId, requesterUid, approve,
				new com.harvana.app.group.GroupCloudFunctions.Callback() {
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
						android.util.Log.w("GroupFirestoreManager", "castJoinVote cloud failed, local fallback: " + error);
						castJoinVoteLocal(groupId, requesterUid, voterUid, approve, callback);
					}
				});
	}

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
							votes.putAll((Map<String, Object>) votesObj);
						}

						if (votes.containsKey(voterUid)) {
							callback.onError("You've already voted on this request.");
							return;
						}
						votes.put(voterUid, approve);

						int[] tally = new int[2];
						for (Object v : votes.values()) {
							if (Boolean.TRUE.equals(v)) tally[0]++; else tally[1]++;
						}
						final int finalYesVotes = tally[0];
						final int finalNoVotes = tally[1];
						int totalVotesCast = finalYesVotes + finalNoVotes;

						String resolution;
						if (totalMembers == 2 && !creatorId.isEmpty()) {
							double weightedYes = 0, weightedTotal = 0;
							for (Map.Entry<String, Object> e : votes.entrySet()) {
								double weight = creatorId.equals(e.getKey()) ? 60.0 : 40.0;
								weightedTotal += weight;
								if (Boolean.TRUE.equals(e.getValue())) weightedYes += weight;
							}
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

	private void _admitVotedMember(String groupId, String newMemberUid, int currentCount, Runnable onDone) {
		int slot = currentCount + 1;
		int newCount = currentCount + 1;
		com.harvana.app.group.JoinRequestManager.admitMember(
				appContext,
				this,
				groupId,
				newMemberUid,
				slot,
				newCount,
				new com.harvana.app.group.JoinRequestManager.Callback() {
					@Override
					public void onSuccess(String message) { onDone.run(); }
					@Override
					public void onError(String error) {
						android.util.Log.w(TAG, "Vote admission side-effects incomplete: " + error);
						onDone.run();
					}
				});
	}

	public void addMember(String groupId, String memberUid, Map<String, Object> fields, SimpleCallback callback) {
		set(doc("GROUPS", groupId, "members", memberUid), fields, callback);
	}

	public void updateMemberFields(String groupId, String memberUid, Map<String, Object> fields, SimpleCallback callback) {
		merge(doc("GROUPS", groupId, "members", memberUid), fields, callback);
	}
	
	public void getMembers(String groupId, ListCallback callback) {
		list(col("GROUPS", groupId, "members"), callback);
	}

	public void getMember(String groupId, String memberUid, DocCallback callback) {
		get(doc("GROUPS", groupId, "members", memberUid), callback);
	}
	
	public void setPrivateDoc(String groupId, String docId, Map<String, Object> fields, SimpleCallback callback) {
		set(doc("GROUPS", groupId, "private", docId), fields, callback);
	}

	public void getPrivateDoc(String groupId, String docId, DocCallback callback) {
		get(doc("GROUPS", groupId, "private", docId), callback);
	}

	public void setTopLevelDoc(String collection, String docId, Map<String, Object> fields, SimpleCallback callback) {
		set(doc(collection, docId), fields, callback);
	}

	public void updateTopLevelFields(String collection, String docId, Map<String, Object> fields, SimpleCallback callback) {
		merge(doc(collection, docId), fields, callback);
	}

	public void queryTopLevel(String collection, String field, String value, int limit, ListCallback callback) {
		query(FirebaseFirestore.getInstance().collection(collection).whereEqualTo(field, value).limit(limit), false, callback);
	}
	
	public void addNotice(String groupId, Map<String, Object> fields, IdCallback callback) {
		add(col("GROUPS", groupId, "notices"), fields, callback);
	}

	/** A notice with a fixed id, so writing it twice cannot make two. Members may create but not change a notice, so a repeat is refused. */
	public void setNotice(String groupId, String noticeId, Map<String, Object> fields, SimpleCallback callback) {
		set(doc("GROUPS", groupId, "notices", noticeId), fields, callback);
	}

	public void postSystemNotice(String groupId, String title, String message) {
		Map<String, Object> fields = com.harvana.app.group.GroupNoticeHelper.buildSystem(title, message);
		addNotice(groupId, fields, new IdCallback() {
			@Override public void onSuccess(String id) {}
			@Override public void onError(String error) {}
		});
	}

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

	/**
	 * Notices created after {@code sinceMs}, oldest first, at most {@code max}. The notification check uses this so each
	 * run reads only what is new, not every notice the group ever had (that cost grew with the group's age, on every
	 * phone, every 90 seconds). Notices without a numeric createdAt were never shown by the check, so nothing is lost.
	 */
	public void getNoticesSince(String groupId, long sinceMs, int max, ListCallback callback) {
		query(col("GROUPS", groupId, "notices").whereGreaterThan("createdAt", sinceMs)
			.orderBy("createdAt", Query.Direction.ASCENDING).limit(max), false, callback);
	}
	
	public void addPayment(String groupId, Map<String, Object> fields, IdCallback callback) {
		add(col("GROUPS", groupId, "payments"), fields, callback);
	}

	public void updatePaymentStatus(String groupId, String paymentDocId, Map<String, Object> fields, SimpleCallback callback) {
		merge(doc("GROUPS", groupId, "payments", paymentDocId), fields, callback);
	}
	
	public void getPayments(String groupId, ListCallback callback) {
		list(col("GROUPS", groupId, "payments"), callback);
	}
	
	public void addLoan(String groupId, Map<String, Object> fields, IdCallback callback) {
		add(col("GROUPS", groupId, "loans"), fields, callback);
	}
	
	public void getLoans(String groupId, ListCallback callback) {
		list(col("GROUPS", groupId, "loans"), callback);
	}

	public void getLoan(String groupId, String loanId, DocCallback callback) {
		get(doc("GROUPS", groupId, "loans", loanId), callback);
	}
	
	public void addPayoutRequest(String groupId, Map<String, Object> fields, IdCallback callback) {
		add(col("GROUPS", groupId, "payoutRequests"), fields, callback);
	}
	
	public void getPayoutRequests(String groupId, ListCallback callback) {
		list(col("GROUPS", groupId, "payoutRequests"), callback);
	}
	
	public void getPayoutRequest(String groupId, String requestId, DocCallback callback) {
		get(doc("GROUPS", groupId, "payoutRequests", requestId), callback);
	}
	
	public void updatePayoutRequest(String groupId, String requestId, Map<String, Object> fields, SimpleCallback callback) {
		merge(doc("GROUPS", groupId, "payoutRequests", requestId), fields, callback);
	}
	
	public void updateLoan(String groupId, String loanId, Map<String, Object> fields, SimpleCallback callback) {
		merge(doc("GROUPS", groupId, "loans", loanId), fields, callback);
	}

	public void addLearnSubmission(Map<String, Object> fields, IdCallback callback) {
		add(col("LEARN_SUBMISSIONS"), fields, callback);
	}

	/** Publishes a Learn Hub article straight away; there is no approval step. */
	public void publishLearn(Map<String, Object> fields, IdCallback callback) {
		add(col("LEARN_CONTENT"), fields, callback);
	}

	public void getMyLearnSubmissions(String uid, ListCallback callback) {
		queryTopLevel("LEARN_SUBMISSIONS", "authorUid", uid, 50, callback);
	}

	public void getLearnContent(ListCallback callback) {
		list(col("LEARN_CONTENT"), callback);
	}

	/** Deletes every document of a top-level collection whose field equals value (in batches). */
	public void deleteTopLevelWhere(String collection, String field, String value, SimpleCallback cb) {
		String err = precheck();
		if (err != null) { deliver(() -> cb.onError(err)); return; }
		BG.execute(() -> {
			try {
				FirebaseFirestore db = FirebaseFirestore.getInstance();
				while (true) {
					QuerySnapshot snap = com.google.android.gms.tasks.Tasks.await(
						db.collection(collection).whereEqualTo(field, value).limit(400).get(), 30, TimeUnit.SECONDS);
					if (snap.isEmpty()) break;
					// Two writes per document (archive copy + delete); a batch holds 500, so commit in groups of 200.
					com.google.firebase.firestore.WriteBatch batch = db.batch();
					int n = 0;
					for (DocumentSnapshot d : snap.getDocuments()) {
						SoftDelete.archiveAndDelete(batch, collection, d);
						if (++n == 200) { com.google.android.gms.tasks.Tasks.await(batch.commit(), 30, TimeUnit.SECONDS); batch = db.batch(); n = 0; }
					}
					if (n > 0) com.google.android.gms.tasks.Tasks.await(batch.commit(), 30, TimeUnit.SECONDS);
					if (snap.size() < 400) break;
				}
				cb.onSuccess();
			} catch (Exception e) {
				Throwable c = e.getCause() != null ? e.getCause() : e;
				cb.onError(c instanceof Exception ? describe((Exception) c) : String.valueOf(c.getMessage()));
			}
		});
	}

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
					// Each document is archived for a year (SoftDelete) and then deleted: two writes, so groups of 200.
					com.google.firebase.firestore.WriteBatch batch = db.batch();
					int n = 0;
					for (DocumentSnapshot d : snap.getDocuments()) {
						SoftDelete.archiveAndDelete(batch, "GROUPS/" + sub, d);
						if (++n == 200) { com.google.android.gms.tasks.Tasks.await(batch.commit(), 30, TimeUnit.SECONDS); batch = db.batch(); n = 0; }
					}
					if (n > 0) com.google.android.gms.tasks.Tasks.await(batch.commit(), 30, TimeUnit.SECONDS);
				}
				DocumentSnapshot group = com.google.android.gms.tasks.Tasks.await(db.collection("GROUPS").document(groupId).get(), 30, TimeUnit.SECONDS);
				com.google.firebase.firestore.WriteBatch last = db.batch();
				if (group.exists()) SoftDelete.archiveAndDelete(last, "GROUPS", group);
				else last.delete(db.collection("GROUPS").document(groupId));
				com.google.android.gms.tasks.Tasks.await(last.commit(), 30, TimeUnit.SECONDS);
				cb.onSuccess();
			} catch (Exception e) {
				Throwable c = e.getCause() != null ? e.getCause() : e;
				cb.onError(c instanceof Exception ? describe((Exception) c) : String.valueOf(c.getMessage()));
			}
		});
	}

	private static DocumentReference doc(String... path) {
		return FirebaseFirestore.getInstance().document(join(path));
	}

	private static CollectionReference col(String... path) {
		return FirebaseFirestore.getInstance().collection(join(path));
	}

	private static String join(String[] path) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < path.length; i++) {
			if (path[i] == null || path[i].isEmpty()) throw new IllegalArgumentException("Missing id in path.");
			if (i > 0) sb.append('/');
			sb.append(path[i]);
		}
		return sb.toString();
	}

	private static String precheck() {
		return FirebaseAuth.getInstance().getCurrentUser() == null ? "Not signed in — no current Firebase user." : null;
	}

	private void set(DocumentReference ref, Map<String, Object> fields, SimpleCallback cb) {
		String err = precheck();
		if (err != null) { deliver(() -> cb.onError(err)); return; }
		try {
			await(ref.set(clean(fields)), id -> cb.onSuccess(), cb::onError);
		} catch (Exception e) { deliver(() -> cb.onError(com.harvana.app.utils.ErrorText.fromHttp(400, ""))); }
	}

	private void merge(DocumentReference ref, Map<String, Object> fields, SimpleCallback cb) {
		String err = precheck();
		if (err != null) { deliver(() -> cb.onError(err)); return; }
		if (fields == null || fields.isEmpty()) { deliver(cb::onSuccess); return; }
		try {
			await(ref.set(clean(fields), SetOptions.mergeFields(new ArrayList<>(fields.keySet()))), id -> cb.onSuccess(), cb::onError);
		} catch (Exception e) { deliver(() -> cb.onError(com.harvana.app.utils.ErrorText.fromHttp(400, ""))); }
	}

	private void add(CollectionReference ref, Map<String, Object> fields, IdCallback cb) {
		String err = precheck();
		if (err != null) { deliver(() -> cb.onError(err)); return; }
		try {
			final DocumentReference newRef = ref.document();
			await(newRef.set(clean(fields)), id -> cb.onSuccess(newRef.getId()), cb::onError);
		} catch (Exception e) { deliver(() -> cb.onError(com.harvana.app.utils.ErrorText.fromHttp(400, ""))); }
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

	private <T> void await(Task<T> task, Ok ok, Fail fail) {
		final AtomicBoolean done = new AtomicBoolean(false);
		TIMER.schedule(() -> {
			if (done.compareAndSet(false, true)) {
				BG.execute(() -> fail.run("Network error: write not acknowledged yet"));
			}
		}, WRITE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
		task.addOnCompleteListener(BG, t -> {
			if (!done.compareAndSet(false, true)) return;
			if (t.isSuccessful()) ok.run(null);
			else fail.run(describe(t.getException()));
		});
	}

	private static void deliver(Runnable r) { BG.execute(r); }

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
				default:                  return "HTTP 500: " + msg;
			}
		}
		return "Network error: " + (e != null && e.getMessage() != null ? e.getMessage() : "unknown error");
	}

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

	public static Map<String, Object> cleanForWrite(Map<String, Object> fields) { return clean(fields); }
	public static Map<String, Object> readFromSdk(Map<String, Object> data) { return read(data); }

	static Map<String, Object> clean(Map<String, Object> fields) {
		Map<String, Object> out = new HashMap<>();
		if (fields != null) for (Map.Entry<String, Object> e : fields.entrySet()) out.put(e.getKey(), cleanValue(e.getValue()));
		return out;
	}

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
