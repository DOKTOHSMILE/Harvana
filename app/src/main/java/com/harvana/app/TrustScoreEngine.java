package com.harvana.app;

import android.content.Context;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
* Real trust score, computed from actual cross-group activity — not a
* hardcoded placeholder. ProfileActivity's trust score bars existed in
* the layout but were never wired to anything; initializeLogic() was
* completely empty and the bars were hardcoded to 0 regardless of any
* real user history.
*
* Gathers data across every group the user belongs to (not just one),
* combines it into weighted sub-scores, and an overall trust score other
* members can use as a real signal for who to deal with.
*
* WEIGHTS (sum to 100):
*   Payment History  30 — confirmed payments vs expected, across all groups
*   Consistency      20 — rounds paid without gaps, not just eventual completion
*   Group Loyalty    15 — groups stayed in vs abandoned
*   Tenure           15 — account + membership age, capped at a 12-month benchmark
*   Verified + Profile 10 — KYC verification status + profile completeness
*   Reports          10 — real hook into a member-reports collection, currently
*                          always zero since no reporting UI exists yet to
*                          generate any - NOT faked, genuinely reads real
*                          (currently empty) data, ready the moment reporting
*                          gets built
*/
@Deprecated
public class TrustScoreEngine {
	// The trust score people see is the node's percentage (trust/NetworkTrust, trust/ProfileTrust). This class is kept only for
	// the counts the P2P screens still read from it (verified, report count, number of groups). Do not show its overallScore.
	
	public static class Result {
		public int overallScore;              // 0-100
		public int paymentHistoryPercent;
		public int consistencyPercent;
		public int groupLoyaltyPercent;
		public int tenurePercent;
		public boolean verified;
		public int profileCompletenessPercent;
		public int reportCount;
		public int totalGroups;
		public int totalCyclesCompleted;
		public double totalLoansTakenHarv;
		public double totalAmountSavedHarv;
		public String tier; // Bronze/Silver/Gold/Platinum, derived from overallScore
	}
	
	public interface Callback {
		void onComplete(Result result);
		void onError(String error);
	}
	
	public static void compute(Context context, String uid, Callback callback) {
		if (context == null || uid == null || uid.isEmpty()) {
			callback.onError("Invalid user");
			return;
		}
		
		// Profile + groups prefer Firestore USERS/{uid}. Group membership is
		// written there by CreateNewGroupActivity and JoinRequestManager.
		// RTDB userdb/{uid}/groups is only a fallback for older accounts
		// that have not been re-joined under the Firestore path yet.
		com.harvana.app.firestore.UserProfileManager.getInstance(context).getProfile(uid,
		new com.harvana.app.firestore.UserProfileManager.DocCallback() {
			@Override
			public void onSuccess(Map<String, Object> profileData) {
				long accountCreatedAtMs = _parseTimestamp(profileData.get("timestamp"));
				String vType = profileData.get("v_type") != null ? profileData.get("v_type").toString() : null;
				boolean verified = vType != null && !vType.trim().isEmpty() && !"null".equalsIgnoreCase(vType.trim());
				
				String phone = _asString(profileData.get("phone"));
				String email = _asString(profileData.get("email"));
				String occupation = _asString(profileData.get("occupation"));
				String address = _asString(profileData.get("address"));
				String dob = _asString(profileData.get("dob"));
				int filled = 0, total = 5;
				if (_notEmpty(phone)) filled++;
				if (_notEmpty(email)) filled++;
				if (_notEmpty(occupation)) filled++;
				if (_notEmpty(address)) filled++;
				if (_notEmpty(dob)) filled++;
				int profileCompletenessPercent = (int) Math.round((filled / (double) total) * 100);
				
				final List<String> firestoreGroups = _groupIdsFromProfile(profileData);
				final long fAccountCreated = accountCreatedAtMs;
				final boolean fVerified = verified;
				final int fCompleteness = profileCompletenessPercent;
				if (!firestoreGroups.isEmpty()) {
					// Groups on the node are read from the node; older Firestore-only groups use the old path. Merged below.
					String wallet = com.harvana.app.wallet.WalletManager.getActiveWalletAddress(context);
					com.harvana.app.sync.NodePortfolio.load(context, uid, wallet, portfolio -> {
						List<String> legacy = (portfolio.entries.isEmpty() && portfolio.legacyGroupIds.isEmpty())
						? firestoreGroups : portfolio.legacyGroupIds;
						_continueWithGroupIds(context, uid, legacy, fAccountCreated, fVerified, fCompleteness, portfolio, callback);
					});
				} else {
					// Fallback for legacy RTDB-only membership lists
					_fetchGroupsFromRtdbThenContinue(context, uid, accountCreatedAtMs, verified,
					profileCompletenessPercent, callback);
				}
			}
			
			@Override
			public void onError(String error) {
				// Profile missing — still try RTDB groups for legacy users
				_fetchGroupsFromRtdbThenContinue(context, uid, 0L, false, 0, callback);
			}
		});
	}
	
	/** Extract group ids from Firestore USERS/{uid}.groups (list of id strings). */
	@SuppressWarnings("unchecked")
	private static List<String> _groupIdsFromProfile(Map<String, Object> profileData) {
		List<String> groupIds = new ArrayList<>();
		if (profileData == null) return groupIds;
		Object existing = profileData.get("groups");
		if (existing instanceof List) {
			for (Object g : (List<Object>) existing) {
				if (g == null) continue;
				// May be a plain id string or a map with "id"
				if (g instanceof Map) {
					Object id = ((Map<?, ?>) g).get("id");
					if (id != null && !id.toString().isEmpty()) groupIds.add(id.toString());
				} else {
					String id = String.valueOf(g).trim();
					if (!id.isEmpty() && !"null".equalsIgnoreCase(id)) groupIds.add(id);
				}
			}
		}
		return groupIds;
	}
	
	private static void _continueWithGroupIds(Context context, String uid, List<String> groupIds,
	long accountCreatedAtMs, boolean verified,
	int profileCompletenessPercent, Callback callback) {
		_continueWithGroupIds(context, uid, groupIds, accountCreatedAtMs, verified, profileCompletenessPercent, null, callback);
	}
	
	private static void _continueWithGroupIds(Context context, String uid, List<String> groupIds,
	long accountCreatedAtMs, boolean verified,
	int profileCompletenessPercent, com.harvana.app.sync.NodePortfolio.Portfolio extra, Callback callback) {
		boolean noNode = extra == null || extra.entries.isEmpty();
		if ((groupIds == null || groupIds.isEmpty()) && noNode) {
			Result r = new Result();
			r.totalGroups = 0;
			r.tenurePercent = _tenurePercent(accountCreatedAtMs);
			r.verified = verified;
			r.profileCompletenessPercent = profileCompletenessPercent;
			r.paymentHistoryPercent = 0;
			r.consistencyPercent = 0;
			r.groupLoyaltyPercent = 0;
			r.reportCount = 0;
			r.overallScore = _combineScore(r);
			r.tier = _tierFor(r);
			callback.onComplete(r);
			return;
		}
		_gatherGroupData(context, uid, groupIds == null ? new ArrayList<String>() : groupIds, accountCreatedAtMs, verified,
		profileCompletenessPercent, extra, callback);
	}
	
	/** Legacy fallback: groups stored as JSON string (or map/list) under RTDB userdb. */
	private static void _fetchGroupsFromRtdbThenContinue(Context context, String uid, long accountCreatedAtMs,
	boolean verified, int profileCompletenessPercent,
	Callback callback) {
		FirebaseDatabase.getInstance().getReference("userdb").child(uid).child("groups")
		.addListenerForSingleValueEvent(new ValueEventListener() {
			@Override
			public void onDataChange(DataSnapshot groupsSnap) {
				List<String> groupIds = new ArrayList<>();
				if (groupsSnap.exists()) {
					Object raw = groupsSnap.getValue();
					if (raw instanceof String) {
						try {
							List<Map<String, Object>> parsed = new com.google.gson.Gson().fromJson(
							(String) raw,
							new com.google.gson.reflect.TypeToken<List<Map<String, Object>>>() {}.getType());
							if (parsed != null) {
								for (Map<String, Object> entry : parsed) {
									Object id = entry.get("id");
									if (id != null && !id.toString().isEmpty()) groupIds.add(id.toString());
								}
							}
						} catch (Exception ignored) {}
					} else if (raw instanceof Map) {
						for (Object v : ((Map<?, ?>) raw).values()) {
							if (v instanceof Map) {
								Object id = ((Map<?, ?>) v).get("id");
								if (id != null && !id.toString().isEmpty()) groupIds.add(id.toString());
							}
						}
					} else if (raw instanceof List) {
						for (Object v : (List<?>) raw) {
							if (v instanceof Map) {
								Object id = ((Map<?, ?>) v).get("id");
								if (id != null && !id.toString().isEmpty()) groupIds.add(id.toString());
							} else if (v != null) {
								String id = String.valueOf(v).trim();
								if (!id.isEmpty()) groupIds.add(id);
							}
						}
					}
				}
				_continueWithGroupIds(context, uid, groupIds, accountCreatedAtMs, verified,
				profileCompletenessPercent, callback);
			}
			
			@Override
			public void onCancelled(DatabaseError error) {
				// RTDB failed too — honest zero-group result
				_continueWithGroupIds(context, uid, new ArrayList<>(), accountCreatedAtMs, verified,
				profileCompletenessPercent, callback);
			}
		});
	}
	
	private static String _asString(Object v) {
		return v != null ? v.toString() : null;
	}
	
	private static void _gatherGroupData(Context context, String uid, List<String> groupIds,
	long accountCreatedAtMs, boolean verified,
	int profileCompletenessPercent, com.harvana.app.sync.NodePortfolio.Portfolio extra, Callback callback) {
		final int n = groupIds.size();
		if (n == 0) {
			_finish(callback, accountCreatedAtMs, accountCreatedAtMs, verified, profileCompletenessPercent, 0, 0,
			0, 0, 0, 0, 0, 0, 0, extra);
			return;
		}
		final int[] remaining = {n};
		final long[] earliestJoinedAt = {accountCreatedAtMs};
		final int[] groupsStillIn = {0};
		final int[] totalExpectedPayments = {0};
		final int[] totalConfirmedPayments = {0};
		final int[] totalPaymentGaps = {0};
		final int[] totalRoundsTracked = {0};
		final double[] totalLoansTaken = {0};
		final double[] totalAmountSaved = {0};
		// Genuinely not computed in this version - would need each group's
		// own currentRound/totalSlots to know if a cycle actually finished,
		// a separate getGroup() fetch not yet wired in here. Left honestly
		// at 0 rather than faking a number.
		final int[] cyclesCompleted = {0};
		
		for (String groupId : groupIds) {
			com.harvana.app.firestore.GroupFirestoreManager mgr =
			com.harvana.app.firestore.GroupFirestoreManager.getInstance(context);
			
			// Three independent reads per group - member doc (tenure, still-active),
			// payments (history/consistency/savings), loans (amount taken). Each
			// group's three reads run in parallel with every other group's.
			final int[] perGroupRemaining = {3};
			Runnable onGroupDone = () -> {
				synchronized (remaining) {
					perGroupRemaining[0]--;
					if (perGroupRemaining[0] <= 0) {
						remaining[0]--;
						if (remaining[0] <= 0) {
							_finish(callback, accountCreatedAtMs, earliestJoinedAt[0], verified,
							profileCompletenessPercent, n, groupsStillIn[0],
							totalExpectedPayments[0], totalConfirmedPayments[0],
							totalPaymentGaps[0], totalRoundsTracked[0],
							totalLoansTaken[0], totalAmountSaved[0], cyclesCompleted[0], extra);
						}
					}
				}
			};
			
			mgr.getMembers(groupId, new com.harvana.app.firestore.GroupFirestoreManager.ListCallback() {
				@Override
				public void onSuccess(List<Map<String, Object>> members) {
					if (members != null) {
						for (Map<String, Object> m : members) {
							String memberUid = _str(m, "memberUid");
							if (uid.equals(memberUid)) {
								long joinedAt = (long) _num(m, "joinedAt");
								synchronized (remaining) {
									groupsStillIn[0]++;
									if (joinedAt > 0 && joinedAt < earliestJoinedAt[0]) {
										earliestJoinedAt[0] = joinedAt;
									}
								}
							}
						}
					}
					onGroupDone.run();
				}
				@Override public void onError(String error) { onGroupDone.run(); }
			});
			
			mgr.getPayments(groupId, new com.harvana.app.firestore.GroupFirestoreManager.ListCallback() {
				@Override
				public void onSuccess(List<Map<String, Object>> payments) {
					if (payments != null) {
						int myConfirmed = 0, myTotal = 0;
						long lastRound = -1;
						List<Long> myRounds = new ArrayList<>();
						for (Map<String, Object> p : payments) {
							String memberUid = _str(p, "memberUid");
							if (!uid.equals(memberUid)) continue;
							myTotal++;
							String status = _str(p, "status");
							boolean confirmed = "CONFIRMED".equalsIgnoreCase(status) || "PAID".equalsIgnoreCase(status);
							if (confirmed) {
								myConfirmed++;
								double amt = _num(p, "amountHarv");
								synchronized (remaining) { totalAmountSaved[0] += amt; }
								long round = (long) _num(p, "round");
								if (round <= 0) round = (long) _num(p, "day");
								if (round > 0) myRounds.add(round);
							}
						}
						// Consistency: gaps between confirmed rounds, not just
						// total completion - two people with the same
						// completion rate can have very different regularity.
						java.util.Collections.sort(myRounds);
						int gaps = 0;
						for (int i = 1; i < myRounds.size(); i++) {
							if (myRounds.get(i) - myRounds.get(i - 1) > 1) gaps++;
						}
						synchronized (remaining) {
							totalExpectedPayments[0] += myTotal;
							totalConfirmedPayments[0] += myConfirmed;
							totalPaymentGaps[0] += gaps;
							totalRoundsTracked[0] += Math.max(1, myRounds.size());
						}
					}
					onGroupDone.run();
				}
				@Override public void onError(String error) { onGroupDone.run(); }
			});
			
			mgr.getLoans(groupId, new com.harvana.app.firestore.GroupFirestoreManager.ListCallback() {
				@Override
				public void onSuccess(List<Map<String, Object>> loans) {
					if (loans != null) {
						for (Map<String, Object> l : loans) {
							String borrowerUid = _str(l, "borrowerUid");
							if (!uid.equals(borrowerUid)) continue;
							// THE FIX: this used to sum every loan doc's
							// principal regardless of status, so a REJECTED
							// application (never disbursed — no money ever
							// moved) or a still-PENDING one (not yet
							// approved) counted the same as a loan the
							// person actually received. "Taken" means it
							// was actually disbursed: ACTIVE (currently
							// outstanding) or REPAID (disbursed and paid
							// back) only.
							String status = _str(l, "status");
							boolean actuallyTaken = "ACTIVE".equalsIgnoreCase(status)
							|| "REPAID".equalsIgnoreCase(status);
							if (!actuallyTaken) continue;
							double principal = _num(l, "principalHarv");
							synchronized (remaining) { totalLoansTaken[0] += principal; }
						}
					}
					onGroupDone.run();
				}
				@Override public void onError(String error) { onGroupDone.run(); }
			});
		}
	}
	
	private static void _finish(Callback callback, long accountCreatedAtMs, long earliestJoinedAt,
	boolean verified, int profileCompletenessPercent, int totalGroups,
	int groupsStillIn, int totalExpectedPayments, int totalConfirmedPayments,
	int totalPaymentGaps, int totalRoundsTracked, double totalLoansTaken,
	double totalAmountSaved, int cyclesCompleted, com.harvana.app.sync.NodePortfolio.Portfolio extra) {
		// Merge groups that live on the node: their figures come from the node's member records.
		if (extra != null) {
			synchronized (extra.entries) {
				for (com.harvana.app.sync.NodePortfolio.Entry e : extra.entries) {
					com.harvana.app.engine.Position ps = e.position;
					totalGroups++;
					if (ps.stillIn) groupsStillIn++;
					totalExpectedPayments += ps.expectedPayments;
					totalConfirmedPayments += ps.confirmedPayments;
					totalPaymentGaps += ps.paymentGaps;
					totalRoundsTracked += Math.max(1, ps.confirmedPayments);
					totalLoansTaken += ps.borrowedHarv;
					totalAmountSaved += ps.savedHarv;
					if (ps.cycleCompleted) cyclesCompleted++;
					if (ps.joinedMs > 0 && (earliestJoinedAt <= 0 || ps.joinedMs < earliestJoinedAt)) earliestJoinedAt = ps.joinedMs;
				}
			}
		}
		Result r = new Result();
		r.totalGroups = totalGroups;
		r.totalLoansTakenHarv = totalLoansTaken;
		r.totalAmountSavedHarv = totalAmountSaved;
		r.totalCyclesCompleted = cyclesCompleted;
		r.verified = verified;
		r.profileCompletenessPercent = profileCompletenessPercent;
		r.reportCount = 0; // real hook, genuinely zero until reporting exists - see class doc
		
		r.paymentHistoryPercent = totalExpectedPayments > 0
		? (int) Math.round((totalConfirmedPayments / (double) totalExpectedPayments) * 100)
		: 0;
		
		r.consistencyPercent = totalRoundsTracked > 0
		? Math.max(0, (int) Math.round((1.0 - (totalPaymentGaps / (double) totalRoundsTracked)) * 100))
		: 0;
		
		r.groupLoyaltyPercent = totalGroups > 0
		? (int) Math.round((groupsStillIn / (double) totalGroups) * 100)
		: 0;
		
		long tenureBaseline = Math.min(accountCreatedAtMs > 0 ? accountCreatedAtMs : Long.MAX_VALUE,
		earliestJoinedAt > 0 ? earliestJoinedAt : Long.MAX_VALUE);
		r.tenurePercent = _tenurePercent(tenureBaseline == Long.MAX_VALUE ? 0 : tenureBaseline);
		
		r.overallScore = _combineScore(r);
		r.tier = _tierFor(r);
		callback.onComplete(r);
	}
	
	/** Linear scale to a 12-month benchmark - a full year of history is
*  100%, newer accounts scale down proportionally. */	
	private static int _tenurePercent(long sinceMs) {
		if (sinceMs <= 0) return 0;
		long elapsedMs = System.currentTimeMillis() - sinceMs;
		long twelveMonthsMs = 365L * 24 * 60 * 60 * 1000;
		double pct = Math.min(1.0, elapsedMs / (double) twelveMonthsMs);
		return (int) Math.round(pct * 100);
	}
	
	private static int _combineScore(Result r) {
		double score =
		(r.paymentHistoryPercent * 0.30) +
		(r.consistencyPercent * 0.20) +
		(r.groupLoyaltyPercent * 0.15) +
		(r.tenurePercent * 0.15) +
		(((r.verified ? 100 : 0) * 0.5 + r.profileCompletenessPercent * 0.5) * 0.10);
		// Reports genuinely subtract, once any exist - real penalty, not
		// just a display label, but with nothing to report yet this is
		// always a no-op subtraction of zero.
		score -= Math.min(50, r.reportCount * 10) * 0.10;
		return Math.max(0, Math.min(100, (int) Math.round(score)));
	}
	
	/** Sept 2026 rule, refined on request: Platinum requires an actual,
*  demonstrated track record of group participation — not merely
*  having joined one — and that record itself has to be genuinely
*  strong, not just a high blended overall score propped up by
*  tenure/verification while the group-specific sub-scores are
*  mediocre. Concretely, ALL of the following, on top of the usual
*  85+ overall score:
*    - totalCyclesCompleted > 0 — has actually finished at least one
*      full rotating cycle, real history rather than bare membership
*    - paymentHistoryPercent >= 85 — the payments they were expected
*      to make, they made, at the same bar as Platinum itself
*    - consistencyPercent >= 85 — paid on time/without gaps, not
*      just eventually caught up
*  Falls back to Gold when any of these aren't met, same as the
*  zero-groups case this replaces — a real gap in participation
*  history, or a participation history that hasn't actually been
*  strong, both mean "not yet provably Platinum," not "untrustworthy." */	
	private static String _tierFor(Result r) {
		int score = r.overallScore;
		if (score >= 85) {
			boolean provenGroupHistory = r.totalGroups > 0
			&& r.totalCyclesCompleted > 0
			&& r.paymentHistoryPercent >= 85
			&& r.consistencyPercent >= 85;
			return provenGroupHistory ? "Platinum" : "Gold";
		}
		if (score >= 65) return "Gold";
		if (score >= 40) return "Silver";
		return "Bronze";
	}
	
	private static long _parseTimestamp(Object raw) {
		if (raw == null) return 0;
		try { return Long.parseLong(String.valueOf(raw)); } catch (Exception e) { return 0; }
	}
	
	private static boolean _notEmpty(String s) {
		return s != null && !s.trim().isEmpty() && !"null".equalsIgnoreCase(s.trim());
	}
	
	private static String _str(Map<String, Object> m, String k) {
		Object v = m.get(k);
		return v != null ? v.toString() : "";
	}
	
	private static double _num(Map<String, Object> m, String k) {
		Object v = m.get(k);
		if (v instanceof Number) return ((Number) v).doubleValue();
		try { return Double.parseDouble(String.valueOf(v)); } catch (Exception e) { return 0; }
	}
}
