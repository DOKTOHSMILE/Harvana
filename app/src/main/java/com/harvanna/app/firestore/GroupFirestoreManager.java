package com.harvanna.app.firestore;

import android.content.Context;
import android.util.Log;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
* REST-based Firestore access for everything group-related — the group
* document itself, plus four subcollections (members, notices, payments,
* loans). Matches the same REST-not-SDK, Bearer-token pattern already used
* for wallet data elsewhere in this app, rather than the Firebase SDK for
* Firestore itself — auth still goes through the real Firebase Auth SDK,
* since that's what's actually available in this project.
*
* Data model:
*   GROUPS/{groupId}                        — core group fields + groupRules array
*   GROUPS/{groupId}/members/{uid}           — one doc per member
*   GROUPS/{groupId}/notices/{noticeId}      — one doc per notice
*   GROUPS/{groupId}/payments/{paymentId}    — one doc per payment record
*   GROUPS/{groupId}/loans/{loanId}          — one doc per loan
*
* Subcollections exist specifically so editing one member's payment status
* doesn't re-sync the entire group document to every listening client, and
* so two members writing at the same time can't silently clobber each
* other's changes the way embedded arrays could.
*/
public class GroupFirestoreManager {
	
	private static final String TAG = "GroupFirestoreManager";
	private static final String PROJECT_ID = "capital-oracle-project";
	private static final String BASE_URL = "https://firestore.googleapis.com/v1/projects/" + PROJECT_ID + "/databases/(default)/documents";
	
	private static GroupFirestoreManager instance;
	private final OkHttpClient client;
	private final Context appContext;
	
	public static synchronized GroupFirestoreManager getInstance(Context context) {
		if (instance == null) {
			instance = new GroupFirestoreManager(context.getApplicationContext());
		}
		return instance;
	}
	
	private GroupFirestoreManager(Context context) {
		this.appContext = context;
		this.client = new OkHttpClient.Builder()
		.connectTimeout(15, TimeUnit.SECONDS)
		.readTimeout(15, TimeUnit.SECONDS)
		.build();
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
		_authorizedRequest("PATCH", "/GROUPS/" + groupId, _encodeFields(fields), new _RawCallback() {
			@Override public void onSuccess(String body) { callback.onSuccess(); }
			@Override public void onError(String error) { callback.onError(error); }
		});
	}
	
	/** Preferred over createGroup() for new groups — lets Firestore generate
*  a collision-free ID rather than the client inventing one, same idiom
*  already used for notices/payments/loans below. */	
	public void createGroupWithAutoId(Map<String, Object> fields, IdCallback callback) {
		_authorizedRequest("POST", "/GROUPS", _encodeFields(fields), new _RawCallback() {
			@Override
			public void onSuccess(String body) {
				try {
					JSONObject json = new JSONObject(body);
					String fullName = json.getString("name"); // .../GROUPS/{generatedId}
					String generatedId = fullName.substring(fullName.lastIndexOf('/') + 1);
					callback.onSuccess(generatedId);
				} catch (Exception e) {
					callback.onError("Failed to parse generated group id: " + e.getMessage());
				}
			}
			@Override public void onError(String error) { callback.onError(error); }
		});
	}
	
	public void getGroup(String groupId, DocCallback callback) {
		_authorizedRequest("GET", "/GROUPS/" + groupId, null, new _RawCallback() {
			@Override
			public void onSuccess(String body) {
				try {
					callback.onSuccess(_decodeDocument(new JSONObject(body)));
				} catch (Exception e) {
					callback.onError("Failed to parse group document: " + e.getMessage());
				}
			}
			@Override public void onError(String error) { callback.onError(error); }
		});
	}
	
	public void updateGroupFields(String groupId, Map<String, Object> fields, SimpleCallback callback) {
		// updateMask ensures only the given fields change — a bare PATCH
		// without it would silently overwrite the whole document.
		StringBuilder mask = new StringBuilder();
		for (String key : fields.keySet()) {
			if (mask.length() > 0) mask.append("&");
			mask.append("updateMask.fieldPaths=").append(key);
		}
		_authorizedRequest("PATCH", "/GROUPS/" + groupId + "?" + mask, _encodeFields(fields), new _RawCallback() {
			@Override public void onSuccess(String body) { callback.onSuccess(); }
			@Override public void onError(String error) { callback.onError(error); }
		});
	}
	
	/**
* Structured query against the top-level GROUPS collection — a genuinely
* different REST shape than a plain document GET, since Firestore's REST
* API needs a runQuery request body for any kind of filtering.
* fieldValue is always treated as a string match (EQUAL) — sufficient
* for filtering on visibility, state, or groupType, which is all this
* app currently needs to filter groups by.
*/	
	public void queryGroups(String fieldName, String fieldValue, int limit, ListCallback callback) {
		try {
			JSONObject fieldPath = new JSONObject().put("fieldPath", fieldName);
			JSONObject value = new JSONObject().put("stringValue", fieldValue);
			JSONObject fieldFilter = new JSONObject()
			.put("field", fieldPath)
			.put("op", "EQUAL")
			.put("value", value);
			JSONObject where = new JSONObject().put("fieldFilter", fieldFilter);
			JSONObject from = new JSONObject().put("collectionId", "GROUPS");
			JSONObject structuredQuery = new JSONObject()
			.put("from", new JSONArray().put(from))
			.put("where", where)
			.put("limit", limit);
			JSONObject body = new JSONObject().put("structuredQuery", structuredQuery);
			
			_authorizedRequest("POST", ":runQuery", body, new _RawCallback() {
				@Override
				public void onSuccess(String responseBody) {
					try {
						List<Map<String, Object>> results = new ArrayList<>();
						JSONArray responseArray = new JSONArray(responseBody);
						for (int i = 0; i < responseArray.length(); i++) {
							JSONObject entry = responseArray.getJSONObject(i);
							// Entries with no "document" key are query-plan/progress
							// markers Firestore includes in runQuery responses —
							// not actual results, safe to skip.
							if (!entry.has("document")) continue;
							JSONObject doc = entry.getJSONObject("document");
							Map<String, Object> decoded = _decodeDocument(doc);
							String fullName = doc.optString("name", "");
							decoded.put("_docId", fullName.substring(fullName.lastIndexOf('/') + 1));
							results.add(decoded);
						}
						callback.onSuccess(results);
					} catch (Exception e) {
						callback.onError("Failed to parse group query results: " + e.getMessage());
					}
				}
				@Override
				public void onError(String error) { callback.onError(error); }
			});
		} catch (Exception e) {
			callback.onError("Failed to build group query: " + e.getMessage());
		}
	}
	
	/**
	 * Collection-group query across every GROUPS/{anyGroupId}/joinRequests
	 * subcollection for docs where requesterUid == the signed-in caller.
	 * Same runQuery REST shape as queryGroups() above, but with
	 * allDescendants:true on the "from" clause so it isn't scoped to one
	 * group's subcollection. Single equality filter only - Firestore
	 * auto-indexes that, no composite index to deploy.
	 *
	 * This is what lets a member's own device discover "one of my join
	 * requests got approved somewhere" without a top-level "my requests"
	 * collection - each result doc already carries groupId and status
	 * because GroupJoinRequestActivity writes both when the request is
	 * first submitted.
	 */
	public void queryMyJoinRequests(String requesterUid, ListCallback callback) {
		try {
			JSONObject fieldPath = new JSONObject().put("fieldPath", "requesterUid");
			JSONObject value = new JSONObject().put("stringValue", requesterUid);
			JSONObject fieldFilter = new JSONObject()
			.put("field", fieldPath)
			.put("op", "EQUAL")
			.put("value", value);
			JSONObject where = new JSONObject().put("fieldFilter", fieldFilter);
			JSONObject from = new JSONObject()
			.put("collectionId", "joinRequests")
			.put("allDescendants", true);
			JSONObject structuredQuery = new JSONObject()
			.put("from", new JSONArray().put(from))
			.put("where", where)
			.put("limit", 50);
			JSONObject body = new JSONObject().put("structuredQuery", structuredQuery);
			
			_authorizedRequest("POST", ":runQuery", body, new _RawCallback() {
				@Override
				public void onSuccess(String responseBody) {
					try {
						List<Map<String, Object>> results = new ArrayList<>();
						JSONArray responseArray = new JSONArray(responseBody);
						for (int i = 0; i < responseArray.length(); i++) {
							JSONObject entry = responseArray.getJSONObject(i);
							if (!entry.has("document")) continue;
							JSONObject doc = entry.getJSONObject("document");
							Map<String, Object> decoded = _decodeDocument(doc);
							String fullName = doc.optString("name", "");
							decoded.put("_docId", fullName.substring(fullName.lastIndexOf('/') + 1));
							results.add(decoded);
						}
						callback.onSuccess(results);
					} catch (Exception e) {
						callback.onError("Failed to parse join request query results: " + e.getMessage());
					}
				}
				@Override public void onError(String error) { callback.onError(error); }
			});
		} catch (Exception e) {
			callback.onError("Failed to build join request query: " + e.getMessage());
		}
	}

	public void queryMyMemberships(String uid, ListCallback callback) {
		try {
			JSONObject fieldPath = new JSONObject().put("fieldPath", "memberUid");
			JSONObject value = new JSONObject().put("stringValue", uid);
			JSONObject fieldFilter = new JSONObject()
			.put("field", fieldPath)
			.put("op", "EQUAL")
			.put("value", value);
			JSONObject where = new JSONObject().put("fieldFilter", fieldFilter);
			JSONObject from = new JSONObject()
			.put("collectionId", "members")
			.put("allDescendants", true);
			JSONObject structuredQuery = new JSONObject()
			.put("from", new JSONArray().put(from))
			.put("where", where)
			.put("limit", 100);
			JSONObject body = new JSONObject().put("structuredQuery", structuredQuery);
			
			_authorizedRequest("POST", ":runQuery", body, new _RawCallback() {
				@Override
				public void onSuccess(String responseBody) {
					try {
						List<Map<String, Object>> results = new ArrayList<>();
						JSONArray responseArray = new JSONArray(responseBody);
						for (int i = 0; i < responseArray.length(); i++) {
							JSONObject entry = responseArray.getJSONObject(i);
							if (!entry.has("document")) continue;
							JSONObject doc = entry.getJSONObject("document");
							Map<String, Object> decoded = _decodeDocument(doc);
							
							String fullName = doc.optString("name", "");
							decoded.put("_docId", fullName.substring(fullName.lastIndexOf('/') + 1));
							
							// Extract the groupId from the REST path string
							// Path looks like: projects/.../documents/GROUPS/{groupId}/members/{uid}
							String[] pathParts = fullName.split("/");
							for (int p = 0; p < pathParts.length - 1; p++) {
								if ("GROUPS".equals(pathParts[p])) {
									decoded.put("groupId", pathParts[p + 1]);
									break;
								}
							}
							results.add(decoded);
						}
						callback.onSuccess(results);
					} catch (Exception e) {
						callback.onError("Failed to parse memberships query results: " + e.getMessage());
					}
				}
				@Override public void onError(String error) { callback.onError(error); }
			});
		} catch (Exception e) {
			callback.onError("Failed to build memberships query: " + e.getMessage());
		}
	}
	
	// ── Join requests subcollection ─────────────────────────────────────
	// "Request to join" in discover_group_custom_view.xml confirms this is
	// meant to be a pending request the admin reviews, not an instant add —
	// same pattern already established for loan applications.
	
	public void addJoinRequest(String groupId, String requesterUid, Map<String, Object> fields, IdCallback callback) {
		_authorizedRequest("PATCH", "/GROUPS/" + groupId + "/joinRequests/" + requesterUid, _encodeFields(fields), new _RawCallback() {
			@Override public void onSuccess(String body) { callback.onSuccess(requesterUid); }
			@Override public void onError(String error) { callback.onError(error); }
		});
	}
	
	public void getJoinRequests(String groupId, ListCallback callback) {
		_authorizedRequest("GET", "/GROUPS/" + groupId + "/joinRequests", null, new _RawCallback() {
			@Override
			public void onSuccess(String body) {
				try {
					List<Map<String, Object>> results = new ArrayList<>();
					JSONObject json = new JSONObject(body);
					if (json.has("documents")) {
						JSONArray docs = json.getJSONArray("documents");
						for (int i = 0; i < docs.length(); i++) {
							JSONObject doc = docs.getJSONObject(i);
							Map<String, Object> decoded = _decodeDocument(doc);
							String fullName = doc.optString("name", "");
							decoded.put("_docId", fullName.substring(fullName.lastIndexOf('/') + 1));
							results.add(decoded);
						}
					}
					callback.onSuccess(results);
				} catch (Exception e) {
					callback.onError("Failed to parse join requests: " + e.getMessage());
				}
			}
			@Override public void onError(String error) { callback.onError(error); }
		});
	}

	public void getJoinRequest(String groupId, String requesterUid, DocCallback callback) {
		_authorizedRequest("GET", "/GROUPS/" + groupId + "/joinRequests/" + requesterUid, null, new _RawCallback() {
			@Override
			public void onSuccess(String body) {
				try {
					callback.onSuccess(_decodeDocument(new JSONObject(body)));
				} catch (Exception e) {
					callback.onError("Failed to parse join request: " + e.getMessage());
				}
			}
			@Override public void onError(String error) { callback.onError(error); }
		});
	}

	public void updateJoinRequestFields(String groupId, String reqId, Map<String, Object> fields, SimpleCallback callback) {
		StringBuilder mask = new StringBuilder();
		for (String key : fields.keySet()) {
			if (mask.length() > 0) mask.append("&");
			mask.append("updateMask.fieldPaths=").append(key);
		}
		_authorizedRequest("PATCH",
		"/GROUPS/" + groupId + "/joinRequests/" + reqId + "?" + mask,
		_encodeFields(fields),
		new _RawCallback() {
			@Override public void onSuccess(String body) { callback.onSuccess(); }
			@Override public void onError(String error) { callback.onError(error); }
		});
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
		_authorizedRequest("PATCH", "/GROUPS/" + groupId + "/members/" + memberUid, _encodeFields(fields), new _RawCallback() {
			@Override public void onSuccess(String body) { callback.onSuccess(); }
			@Override public void onError(String error) { callback.onError(error); }
		});
	}

	/** Masked partial update to specific fields on an existing member doc -
	 *  addMember() above is an unmasked PATCH that replaces the whole
	 *  document, which would wipe slot/joinedAt/status/role if used for a
	 *  "just update one field" case like registering a wallet address. */
	public void updateMemberFields(String groupId, String memberUid, Map<String, Object> fields, SimpleCallback callback) {
		StringBuilder mask = new StringBuilder();
		for (String key : fields.keySet()) {
			if (mask.length() > 0) mask.append("&");
			mask.append("updateMask.fieldPaths=").append(key);
		}
		_authorizedRequest("PATCH", "/GROUPS/" + groupId + "/members/" + memberUid + "?" + mask,
			_encodeFields(fields), new _RawCallback() {
				@Override public void onSuccess(String body) { callback.onSuccess(); }
				@Override public void onError(String error) { callback.onError(error); }
			});
	}
	
	public void getMembers(String groupId, ListCallback callback) {
		_listSubcollection(groupId, "members", callback);
	}

	/** Single member doc lookup — e.g. resolving the signed-in user's own slot. */
	public void getMember(String groupId, String memberUid, DocCallback callback) {
		_authorizedRequest("GET", "/GROUPS/" + groupId + "/members/" + memberUid, null, new _RawCallback() {
			@Override
			public void onSuccess(String body) {
				try {
					callback.onSuccess(_decodeDocument(new JSONObject(body)));
				} catch (Exception e) {
					callback.onError("Failed to parse member document: " + e.getMessage());
				}
			}
			@Override public void onError(String error) { callback.onError(error); }
		});
	}
	
	// ── Notices subcollection ───────────────────────────────────────────
	
	public void addNotice(String groupId, Map<String, Object> fields, IdCallback callback) {
		_createWithAutoId(groupId, "notices", fields, callback);
	}

	/**
	 * Auto-posted activity notices - contributions, payouts, new members -
	 * broadcast through the same notices feed a manually-posted announcement
	 * uses, so every real group event shows up in one place every member
	 * can see. Deliberately fire-and-forget: a failed notice write shouldn't
	 * ever block or fail the real action (the payment, the payout, the
	 * membership) that triggered it - this is a side effect, not the
	 * primary operation.
	 */
	public void postSystemNotice(String groupId, String title, String message) {
		// Canonical schema: type SYSTEM, createdAt long, system=true
		Map<String, Object> fields = com.harvanna.app.group.GroupNoticeHelper.buildSystem(title, message);
		addNotice(groupId, fields, new IdCallback() {
			@Override public void onSuccess(String id) { /* best-effort */ }
			@Override public void onError(String error) { /* best-effort */ }
		});
	}
	

	// -- Member reports - user-side only, moderation is separate work ---
	//
	// A top-level collection, not a group subcollection: reports are about
	// a member's behavior, which matters across every group they're in, not
	// just the one where an incident happened - the trust score engine
	// already reads cross-group, so reports need to be queryable the same
	// way, not scattered under each group individually.
	//
	// Written here (not a separate ReportManager class) because the REST
	// infrastructure this needs (_authorizedRequest, _encodeFields,
	// _decodeDocument) is all private to this class already - reusing it
	// directly avoids duplicating that plumbing in a second file.
	//
	// Deliberately scoped to submission only. Every report is created with
	// status PENDING and stays invisible to everyone but moderators until
	// a review step (not built yet - explicitly separate work) confirms or
	// dismisses it. TrustScoreEngine only ever reads CONFIRMED reports, so
	// an unreviewed accusation can't affect anyone's score.

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

		_authorizedRequest("POST", "/MEMBER_REPORTS", _encodeFields(fields), new _RawCallback() {
			@Override
			public void onSuccess(String body) {
				try {
					JSONObject json = new JSONObject(body);
					String fullName = json.getString("name");
					String generatedId = fullName.substring(fullName.lastIndexOf('/') + 1);
					callback.onSuccess(generatedId);
				} catch (Exception e) {
					callback.onError("Failed to parse generated report id: " + e.getMessage());
				}
			}
			@Override public void onError(String error) { callback.onError(error); }
		});
	}

	public void getNotices(String groupId, ListCallback callback) {
		_listSubcollection(groupId, "notices", callback);
	}
	
	// ── Payments subcollection ──────────────────────────────────────────
	
	public void addPayment(String groupId, Map<String, Object> fields, IdCallback callback) {
		_createWithAutoId(groupId, "payments", fields, callback);
	}

	/** Updates a specific payment doc after creation — specifically for
	 *  flipping status from PENDING to CONFIRMED once a real on-chain
	 *  confirmation actually happens, rather than writing CONFIRMED
	 *  prematurely at submission time. */
	public void updatePaymentStatus(String groupId, String paymentDocId, Map<String, Object> fields, SimpleCallback callback) {
		StringBuilder mask = new StringBuilder();
		for (String key : fields.keySet()) {
			if (mask.length() > 0) mask.append("&");
			mask.append("updateMask.fieldPaths=").append(key);
		}
		_authorizedRequest("PATCH", "/GROUPS/" + groupId + "/payments/" + paymentDocId + "?" + mask,
			_encodeFields(fields), new _RawCallback() {
				@Override public void onSuccess(String body) { callback.onSuccess(); }
				@Override public void onError(String error) { callback.onError(error); }
			});
	}
	
	public void getPayments(String groupId, ListCallback callback) {
		_listSubcollection(groupId, "payments", callback);
	}
	
	// ── Loans subcollection ─────────────────────────────────────────────
	
	public void addLoan(String groupId, Map<String, Object> fields, IdCallback callback) {
		_createWithAutoId(groupId, "loans", fields, callback);
	}
	
	public void getLoans(String groupId, ListCallback callback) {
		_listSubcollection(groupId, "loans", callback);
	}

	/** Single-loan fetch — used to re-validate a loan's real state (principal,
	 * interest, disbursedAt, status) server-side-equivalent before accepting
	 * a repayment, instead of trusting whatever the UI last rendered. */
	public void getLoan(String groupId, String loanId, DocCallback callback) {
		_authorizedRequest("GET", "/GROUPS/" + groupId + "/loans/" + loanId, null, new _RawCallback() {
			@Override
			public void onSuccess(String body) {
				try {
					callback.onSuccess(_decodeDocument(new JSONObject(body)));
				} catch (Exception e) {
					callback.onError("Failed to parse loan document: " + e.getMessage());
				}
			}
			@Override public void onError(String error) { callback.onError(error); }
		});
	}
	
	// ── Shared subcollection helpers ────────────────────────────────────
	
	private void _createWithAutoId(String groupId, String subcollection, Map<String, Object> fields, IdCallback callback) {
		_authorizedRequest("POST", "/GROUPS/" + groupId + "/" + subcollection, _encodeFields(fields), new _RawCallback() {
			@Override
			public void onSuccess(String body) {
				try {
					JSONObject json = new JSONObject(body);
					String fullName = json.getString("name"); // .../subcollection/{generatedId}
					String generatedId = fullName.substring(fullName.lastIndexOf('/') + 1);
					callback.onSuccess(generatedId);
				} catch (Exception e) {
					callback.onError("Failed to parse generated document id: " + e.getMessage());
				}
			}
			@Override public void onError(String error) { callback.onError(error); }
		});
	}
	
	private void _listSubcollection(String groupId, String subcollection, ListCallback callback) {
		_authorizedRequest("GET", "/GROUPS/" + groupId + "/" + subcollection, null, new _RawCallback() {
			@Override
			public void onSuccess(String body) {
				try {
					List<Map<String, Object>> results = new ArrayList<>();
					JSONObject json = new JSONObject(body);
					if (json.has("documents")) {
						JSONArray docs = json.getJSONArray("documents");
						for (int i = 0; i < docs.length(); i++) {
							JSONObject doc = docs.getJSONObject(i);
							Map<String, Object> decoded = _decodeDocument(doc);
							// Stash the doc id (last path segment of "name") so callers
							// can reference this specific record later if needed.
							String fullName = doc.optString("name", "");
							decoded.put("_docId", fullName.substring(fullName.lastIndexOf('/') + 1));
							results.add(decoded);
						}
					}
					callback.onSuccess(results);
				} catch (Exception e) {
					callback.onError("Failed to parse " + subcollection + " list: " + e.getMessage());
				}
			}
			@Override public void onError(String error) { callback.onError(error); }
		});
	}
	
	// ── Firestore typed-value encoding ──────────────────────────────────
	
	private JSONObject _encodeFields(Map<String, Object> fields) {
		try {
			JSONObject root = new JSONObject();
			JSONObject fieldsObj = new JSONObject();
			for (Map.Entry<String, Object> entry : fields.entrySet()) {
				fieldsObj.put(entry.getKey(), _encodeValue(entry.getValue()));
			}
			root.put("fields", fieldsObj);
			return root;
		} catch (Exception e) {
			Log.e(TAG, "Failed to encode fields: " + e.getMessage());
			return new JSONObject();
		}
	}
	
	@SuppressWarnings("unchecked")
	private JSONObject _encodeValue(Object value) throws Exception {
		JSONObject wrapper = new JSONObject();
		if (value == null) {
			wrapper.put("nullValue", JSONObject.NULL);
		} else if (value instanceof String) {
			wrapper.put("stringValue", value);
		} else if (value instanceof Integer || value instanceof Long) {
			wrapper.put("integerValue", String.valueOf(value));
		} else if (value instanceof Double || value instanceof Float) {
			wrapper.put("doubleValue", value);
		} else if (value instanceof Boolean) {
			wrapper.put("booleanValue", value);
		} else if (value instanceof List) {
			JSONArray valuesArray = new JSONArray();
			for (Object item : (List<Object>) value) {
				valuesArray.put(_encodeValue(item));
			}
			JSONObject arrayWrapper = new JSONObject();
			arrayWrapper.put("values", valuesArray);
			wrapper.put("arrayValue", arrayWrapper);
		} else if (value instanceof Map) {
			JSONObject mapFields = new JSONObject();
			for (Map.Entry<String, Object> entry : ((Map<String, Object>) value).entrySet()) {
				mapFields.put(entry.getKey(), _encodeValue(entry.getValue()));
			}
			JSONObject mapWrapper = new JSONObject();
			mapWrapper.put("fields", mapFields);
			wrapper.put("mapValue", mapWrapper);
		} else {
			// Fallback — stringify anything unexpected rather than throw and
			// abort the whole write over one unusual field.
			wrapper.put("stringValue", String.valueOf(value));
		}
		return wrapper;
	}
	
	// ── Firestore typed-value decoding ──────────────────────────────────
	
	private Map<String, Object> _decodeDocument(JSONObject doc) throws Exception {
		Map<String, Object> result = new HashMap<>();
		if (!doc.has("fields")) return result;
		JSONObject fields = doc.getJSONObject("fields");
		Iterator<String> keys = fields.keys();
		while (keys.hasNext()) {
			String key = keys.next();
			result.put(key, _decodeValue(fields.getJSONObject(key)));
		}
		return result;
	}
	
	private Object _decodeValue(JSONObject wrapper) throws Exception {
		if (wrapper.has("stringValue")) return wrapper.getString("stringValue");
		if (wrapper.has("integerValue")) return Long.parseLong(wrapper.getString("integerValue"));
		if (wrapper.has("doubleValue")) return wrapper.getDouble("doubleValue");
		if (wrapper.has("booleanValue")) return wrapper.getBoolean("booleanValue");
		if (wrapper.has("nullValue")) return null;
		if (wrapper.has("arrayValue")) {
			List<Object> list = new ArrayList<>();
			JSONObject arrayValue = wrapper.getJSONObject("arrayValue");
			if (arrayValue.has("values")) {
				JSONArray values = arrayValue.getJSONArray("values");
				for (int i = 0; i < values.length(); i++) {
					list.add(_decodeValue(values.getJSONObject(i)));
				}
			}
			return list;
		}
		if (wrapper.has("mapValue")) {
			Map<String, Object> map = new HashMap<>();
			JSONObject mapValue = wrapper.getJSONObject("mapValue");
			if (mapValue.has("fields")) {
				JSONObject mapFields = mapValue.getJSONObject("fields");
				Iterator<String> keys = mapFields.keys();
				while (keys.hasNext()) {
					String key = keys.next();
					map.put(key, _decodeValue(mapFields.getJSONObject(key)));
				}
			}
			return map;
		}
		return null;
	}
	
	// ── HTTP + auth plumbing ────────────────────────────────────────────
	
	private interface _RawCallback {
		void onSuccess(String body);
		void onError(String error);
	}
	
	private void _authorizedRequest(String method, String path, JSONObject body, _RawCallback callback) {
		FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
		if (user == null) {
			callback.onError("Not signed in — no current Firebase user.");
			return;
		}
		// false = use a cached token if it's still valid, matching the
		// standard Firebase SDK pattern already used elsewhere in this app.
		user.getIdToken(false).addOnCompleteListener(new OnCompleteListener<GetTokenResult>() {
			@Override
			public void onComplete(Task<GetTokenResult> task) {
				if (task.isSuccessful() && task.getResult() != null) {
					_executeRequest(method, path, body, task.getResult().getToken(), callback, true);
				} else {
					String msg = task.getException() != null ? task.getException().getMessage() : "unknown error";
					callback.onError("Failed to obtain ID token: " + msg);
				}
			}
		});
	}
	
	private void _executeRequest(String method, String path, JSONObject body, String idToken, _RawCallback callback, boolean allowRetryOn401) {
		Request.Builder builder = new Request.Builder()
		.url(BASE_URL + path)
		.header("Authorization", "Bearer " + idToken);
		
		RequestBody requestBody = null;
		if (body != null) {
			requestBody = RequestBody.create(body.toString(), MediaType.parse("application/json"));
		}
		
		switch (method) {
			case "GET": builder.get(); break;
			case "POST": builder.post(requestBody != null ? requestBody : RequestBody.create("", MediaType.parse("application/json"))); break;
			case "PATCH": builder.patch(requestBody != null ? requestBody : RequestBody.create("", MediaType.parse("application/json"))); break;
			default: callback.onError("Unsupported HTTP method: " + method); return;
		}
		
		client.newCall(builder.build()).enqueue(new Callback() {
			@Override
			public void onFailure(Call call, IOException e) {
				callback.onError("Network error: " + e.getMessage());
			}
			
			@Override
			public void onResponse(Call call, Response response) throws IOException {
				String responseBody = response.body() != null ? response.body().string() : "";
				if (response.code() == 401 && allowRetryOn401) {
					// Force-refresh once, then retry — true forces Firebase to
					// fetch a genuinely fresh token instead of a cached one.
					FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
					if (user == null) {
						callback.onError("Not signed in — can't refresh token after 401.");
						return;
					}
					user.getIdToken(true).addOnCompleteListener(new OnCompleteListener<GetTokenResult>() {
						@Override
						public void onComplete(Task<GetTokenResult> task) {
							if (task.isSuccessful() && task.getResult() != null) {
								_executeRequest(method, path, body, task.getResult().getToken(), callback, false);
							} else {
								String msg = task.getException() != null ? task.getException().getMessage() : "unknown error";
								callback.onError("Token refresh failed after 401: " + msg);
							}
						}
					});
					return;
				}
				if (!response.isSuccessful()) {
					callback.onError("HTTP " + response.code() + ": " + responseBody);
					return;
				}
				callback.onSuccess(responseBody);
			}
		});
	}
	
	public void addPayoutRequest(String groupId, Map<String, Object> fields, IdCallback callback) {
		_createWithAutoId(groupId, "payoutRequests", fields, callback);
	}
	
	public void getPayoutRequests(String groupId, ListCallback callback) {
		_listSubcollection(groupId, "payoutRequests", callback);
	}
	
	/** Single-request fetch — used to re-validate a payout request's real
	 * current status server-side-equivalent before approving it, instead of
	 * trusting whatever the admin's list screen last rendered. Same pattern
	 * as getLoan(). */
	public void getPayoutRequest(String groupId, String requestId, DocCallback callback) {
		_authorizedRequest("GET", "/GROUPS/" + groupId + "/payoutRequests/" + requestId, null, new _RawCallback() {
			@Override
			public void onSuccess(String body) {
				try {
					callback.onSuccess(_decodeDocument(new JSONObject(body)));
				} catch (Exception e) {
					callback.onError("Failed to parse payout request document: " + e.getMessage());
				}
			}
			@Override public void onError(String error) { callback.onError(error); }
		});
	}
	
	public void updatePayoutRequest(String groupId, String requestId, Map<String, Object> fields, SimpleCallback callback) {
		StringBuilder mask = new StringBuilder();
		for (String key : fields.keySet()) {
			if (mask.length() > 0) mask.append("&");
			mask.append("updateMask.fieldPaths=").append(key);
		}
		_authorizedRequest("PATCH",
		"/GROUPS/" + groupId + "/payoutRequests/" + requestId + "?" + mask,
		_encodeFields(fields),
		new _RawCallback() {
			@Override public void onSuccess(String body) { callback.onSuccess(); }
			@Override public void onError(String error) { callback.onError(error); }
		});
	}
	
	public void updateLoan(String groupId, String loanId, Map<String, Object> fields, SimpleCallback callback) {
		StringBuilder mask = new StringBuilder();
		for (String key : fields.keySet()) {
			if (mask.length() > 0) mask.append("&");
			mask.append("updateMask.fieldPaths=").append(key);
		}
		_authorizedRequest("PATCH",
		"/GROUPS/" + groupId + "/loans/" + loanId + "?" + mask,
		_encodeFields(fields),
		new _RawCallback() {
			@Override public void onSuccess(String body) { callback.onSuccess(); }
			@Override public void onError(String error) { callback.onError(error); }
		});
	}

	// ── Learn Hub content (top-level collection, same precedent as
	// MEMBER_REPORTS above — not nested under a specific group) ──────────

	/** Posts a new Learn Hub article/guide. LearnFragmentActivity previously
	 *  had no data source at all — this is the write half of actually
	 *  making that tab work. */
	public void addLearnContent(Map<String, Object> fields, IdCallback callback) {
		_authorizedRequest("POST", "/LEARN_CONTENT", _encodeFields(fields), new _RawCallback() {
			@Override
			public void onSuccess(String body) {
				try {
					JSONObject json = new JSONObject(body);
					String fullName = json.getString("name");
					String generatedId = fullName.substring(fullName.lastIndexOf('/') + 1);
					callback.onSuccess(generatedId);
				} catch (Exception e) {
					callback.onError("Failed to parse generated content id: " + e.getMessage());
				}
			}
			@Override public void onError(String error) { callback.onError(error); }
		});
	}

	/** Lists every Learn Hub content doc. No group scoping needed — this is
	 *  a global feed, same shape as MEMBER_REPORTS' top-level collection. */
	public void getLearnContent(ListCallback callback) {
		_authorizedRequest("GET", "/LEARN_CONTENT", null, new _RawCallback() {
			@Override
			public void onSuccess(String body) {
				try {
					List<Map<String, Object>> results = new ArrayList<>();
					JSONObject json = new JSONObject(body);
					if (json.has("documents")) {
						JSONArray docs = json.getJSONArray("documents");
						for (int i = 0; i < docs.length(); i++) {
							JSONObject doc = docs.getJSONObject(i);
							Map<String, Object> decoded = _decodeDocument(doc);
							String fullName = doc.optString("name", "");
							decoded.put("_docId", fullName.substring(fullName.lastIndexOf('/') + 1));
							results.add(decoded);
						}
					}
					callback.onSuccess(results);
				} catch (Exception e) {
					callback.onError("Failed to parse Learn Hub content list: " + e.getMessage());
				}
			}
			@Override public void onError(String error) { callback.onError(error); }
		});
	}
}
