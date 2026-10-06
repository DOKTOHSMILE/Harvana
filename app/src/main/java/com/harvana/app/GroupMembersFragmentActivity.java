package com.harvana.app;

import android.animation.*;
import android.app.*;
import android.app.Activity;
import android.content.*;
import android.content.SharedPreferences;
import android.content.res.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.media.*;
import android.net.*;
import android.os.*;
import android.text.*;
import android.text.style.*;
import android.util.*;
import android.view.*;
import android.view.View.*;
import android.view.animation.*;
import android.widget.*;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.*;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.*;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import com.budiyev.android.codescanner.*;
import com.bumptech.glide.*;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.HashMap;
import java.util.regex.*;
import org.json.*;
import com.harvana.app.group.JoinRequestManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;


public class GroupMembersFragmentActivity extends Fragment {
	
	private FirebaseDatabase _firebase = FirebaseDatabase.getInstance();
	
	private LinearLayout linear121;
	private LinearLayout panel_pending;
	private TextView textview2;
	private RecyclerView recycler_members;
	private TextView tv_members_empty;
	private TextView tv_members_count;
	private TextView btn_members_invite;
	private TextView tv_pending_label;
	private RecyclerView recycler_pending_members;
	
	private SharedPreferences groupsp;
	private SharedPreferences authsp;
	private DatabaseReference userdb = _firebase.getReference("userdb");
	private ChildEventListener _userdb_child_listener;
	private DatabaseReference groupdb = _firebase.getReference("groupdb");
	private ChildEventListener _groupdb_child_listener;
	private FirebaseAuth fauth;
	private OnCompleteListener<AuthResult> _fauth_create_user_listener;
	private OnCompleteListener<AuthResult> _fauth_sign_in_listener;
	private OnCompleteListener<Void> _fauth_reset_password_listener;
	private OnCompleteListener<Void> fauth_updateEmailListener;
	private OnCompleteListener<Void> fauth_updatePasswordListener;
	private OnCompleteListener<Void> fauth_emailVerificationSentListener;
	private OnCompleteListener<Void> fauth_deleteUserListener;
	private OnCompleteListener<Void> fauth_updateProfileListener;
	private OnCompleteListener<AuthResult> fauth_phoneAuthListener;
	private OnCompleteListener<AuthResult> fauth_googleSignInListener;
	
	@NonNull
	@Override
	public View onCreateView(@NonNull LayoutInflater _inflater, @Nullable ViewGroup _container, @Nullable Bundle _savedInstanceState) {
		View _view = _inflater.inflate(R.layout.group_members_fragment, _container, false);
		initialize(_savedInstanceState, _view);
		FirebaseApp.initializeApp(getContext());
		initializeLogic();
		return _view;
	}
	
	private void initialize(Bundle _savedInstanceState, View _view) {
		linear121 = _view.findViewById(R.id.linear121);
		panel_pending = _view.findViewById(R.id.panel_pending);
		textview2 = _view.findViewById(R.id.textview2);
		recycler_members = _view.findViewById(R.id.recycler_members);
		com.harvana.app.utils.Shimmer.hold(recycler_members, com.harvana.app.utils.Shimmer.rows(recycler_members.getContext(), 5), 4000, "GroupMembersFragmentActivity");
		tv_members_empty = _view.findViewById(R.id.tv_members_empty);
		tv_members_count = _view.findViewById(R.id.tv_members_count);
		btn_members_invite = _view.findViewById(R.id.btn_members_invite);
		if (btn_members_invite != null) btn_members_invite.setOnClickListener(v -> {
			if (getActivity() != null) com.harvana.app.group.InviteMemberSheet.show(getActivity(), groupId, lastGroupData);
		});
		tv_pending_label = _view.findViewById(R.id.tv_pending_label);
		recycler_pending_members = _view.findViewById(R.id.recycler_pending_members);
		groupsp = getContext().getSharedPreferences("groupsp", Activity.MODE_PRIVATE);
		authsp = getContext().getSharedPreferences("authsp", Activity.MODE_PRIVATE);
		fauth = FirebaseAuth.getInstance();
		
		_userdb_child_listener = new ChildEventListener() {
			@Override
			public void onChildAdded(DataSnapshot _param1, String _param2) {
				GenericTypeIndicator<HashMap<String, Object>> _ind = new GenericTypeIndicator<HashMap<String, Object>>() {};
				final String _childKey = _param1.getKey();
				final HashMap<String, Object> _childValue = _param1.getValue(_ind);
				
			}
			
			@Override
			public void onChildChanged(DataSnapshot _param1, String _param2) {
				GenericTypeIndicator<HashMap<String, Object>> _ind = new GenericTypeIndicator<HashMap<String, Object>>() {};
				final String _childKey = _param1.getKey();
				final HashMap<String, Object> _childValue = _param1.getValue(_ind);
				
			}
			
			@Override
			public void onChildMoved(DataSnapshot _param1, String _param2) {
				
			}
			
			@Override
			public void onChildRemoved(DataSnapshot _param1) {
				GenericTypeIndicator<HashMap<String, Object>> _ind = new GenericTypeIndicator<HashMap<String, Object>>() {};
				final String _childKey = _param1.getKey();
				final HashMap<String, Object> _childValue = _param1.getValue(_ind);
				
			}
			
			@Override
			public void onCancelled(DatabaseError _param1) {
				final int _errorCode = _param1.getCode();
				final String _errorMessage = _param1.getMessage();
				
			}
		};
		userdb.addChildEventListener(_userdb_child_listener);
		
		_groupdb_child_listener = new ChildEventListener() {
			@Override
			public void onChildAdded(DataSnapshot _param1, String _param2) {
				GenericTypeIndicator<HashMap<String, Object>> _ind = new GenericTypeIndicator<HashMap<String, Object>>() {};
				final String _childKey = _param1.getKey();
				final HashMap<String, Object> _childValue = _param1.getValue(_ind);
				
			}
			
			@Override
			public void onChildChanged(DataSnapshot _param1, String _param2) {
				GenericTypeIndicator<HashMap<String, Object>> _ind = new GenericTypeIndicator<HashMap<String, Object>>() {};
				final String _childKey = _param1.getKey();
				final HashMap<String, Object> _childValue = _param1.getValue(_ind);
				
			}
			
			@Override
			public void onChildMoved(DataSnapshot _param1, String _param2) {
				
			}
			
			@Override
			public void onChildRemoved(DataSnapshot _param1) {
				GenericTypeIndicator<HashMap<String, Object>> _ind = new GenericTypeIndicator<HashMap<String, Object>>() {};
				final String _childKey = _param1.getKey();
				final HashMap<String, Object> _childValue = _param1.getValue(_ind);
				
			}
			
			@Override
			public void onCancelled(DatabaseError _param1) {
				final int _errorCode = _param1.getCode();
				final String _errorMessage = _param1.getMessage();
				
			}
		};
		groupdb.addChildEventListener(_groupdb_child_listener);
		
		fauth_updateEmailListener = _param1 -> {
			final boolean _success = _param1.isSuccessful();
			final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
			
		};
		
		fauth_updatePasswordListener = _param1 -> {
			final boolean _success = _param1.isSuccessful();
			final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
			
		};
		
		fauth_emailVerificationSentListener = _param1 -> {
			final boolean _success = _param1.isSuccessful();
			final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
			
		};
		
		fauth_deleteUserListener = _param1 -> {
			final boolean _success = _param1.isSuccessful();
			final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
			
		};
		
		fauth_phoneAuthListener = task -> {
			final boolean _success = task.isSuccessful();
			final String _errorMessage = task.getException() != null ? task.getException().getMessage() : "";
			
		};
		
		fauth_updateProfileListener = _param1 -> {
			final boolean _success = _param1.isSuccessful();
			final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
			
		};
		
		fauth_googleSignInListener = task -> {
			final boolean _success = task.isSuccessful();
			final String _errorMessage = task.getException() != null ? task.getException().getMessage() : "";
			
		};
		
		_fauth_create_user_listener = _param1 -> {
			final boolean _success = _param1.isSuccessful();
			final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
			
		};
		
		_fauth_sign_in_listener = _param1 -> {
			final boolean _success = _param1.isSuccessful();
			final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
			
		};
		
		_fauth_reset_password_listener = _param1 -> {
			final boolean _success = _param1.isSuccessful();
			
		};
	}
	
	private void initializeLogic() {
		groupId = getArguments() != null ? getArguments().getString("groupId", "") : "";
		if (recycler_members != null && recycler_members.getLayoutManager() == null) {
			recycler_members.setLayoutManager(new LinearLayoutManager(getContext()));
		}
		if (recycler_pending_members != null && recycler_pending_members.getLayoutManager() == null) {
			recycler_pending_members.setLayoutManager(new LinearLayoutManager(getContext()));
		}
		_loadCreatorAndMembers();
		
	}
	
	@Override
	public void onResume() {
		super.onResume();
		if (groupId != null && !groupId.isEmpty()) { 			
			_loadMembers(); 		
			_loadPending();
		}
		
	}
	

	public void _MORE() {
	}
	private String groupId = "";
	private HashMap<String, Object> user_v_map = new HashMap<>();
	private double length = 0;
	private double number = 0;
	private String creatorId = "";
	private HashMap<String, Object> user_data_v_map = new HashMap<>();
	private String uid = "";
	private boolean isAdmin = false;
	private boolean canInvite = false;
	private Map<String, Object> lastGroupData = new java.util.HashMap<>();
	private int nextSlot = 1;
	private int totalSlots = 0;
	private String joinApprovalMethod = "ADMIN";
	private final java.util.Set<String> activeMemberUids = new java.util.HashSet<>();
	private String groupType = "";
	private String linkedNodeId = "";
	private String groupWalletAddress = "";
	private double contributionAmount = 0;
	
	private ArrayList<HashMap<String, Object>> group_list_map = new ArrayList<>();
	private ArrayList<HashMap<String, Object>> group_members_map = new ArrayList<>();
	private ArrayList<HashMap<String, Object>> membersProfileList = new ArrayList<>();
	private ArrayList<HashMap<String, Object>> displayMembers = new ArrayList<>();
	private ArrayList<HashMap<String, Object>> pendingList = new ArrayList<>();
	
	
	private void _loadCreatorAndMembers() {
		if (groupId.isEmpty() || getContext() == null) return;
		com.harvana.app.firestore.GroupFirestoreManager.getInstance(getContext())
		.getGroup(groupId, new com.harvana.app.firestore.GroupFirestoreManager.DocCallback() {
			@Override
			public void onSuccess(Map<String, Object> data) {
				Object v = data.get("creatorId");
				creatorId = v != null ? v.toString() : "";
				Object gt = data.get("groupType");
				groupType = gt != null ? gt.toString() : "";
				Object ca = data.get("contributionAmount");
				if (ca instanceof Number) contributionAmount = ((Number) ca).doubleValue();
				else try { contributionAmount = Double.parseDouble(String.valueOf(ca)); } catch (Exception e) { contributionAmount = 0; }
				Object gw = data.get("groupWalletAddress");
				if (gw == null) gw = data.get("group_wallet");
				groupWalletAddress = gw != null ? gw.toString() : "";
				totalSlots = (int) num(data, "totalSlots");
				linkedNodeId = com.harvana.app.group.GroupKinds.nodeId(data);
				Object methodObj = data.get("joinApprovalMethod");
				joinApprovalMethod = methodObj != null ? methodObj.toString() : "ADMIN";
				String uid = FirebaseAuth.getInstance().getCurrentUser() != null
				? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
				isAdmin = !creatorId.isEmpty() && creatorId.equals(uid);
				lastGroupData = new java.util.HashMap<>(data);
				canInvite = isAdmin || Boolean.TRUE.equals(data.get("membersCanInvite"));
				if (getActivity() == null) return;
				getActivity().runOnUiThread(() -> {
					if (btn_members_invite != null)
					btn_members_invite.setVisibility(canInvite ? View.VISIBLE : View.GONE);
					_loadMembers();
				});
			}
			@Override
			public void onError(String error) {
				// THE FIX: this called _loadMembers() directly, on whatever
				// thread this callback fires on — which for a network
				// failure is OkHttp's background callback thread, not the
				// UI thread. _loadMembers()'s cache-first render then hit
				// recycler_members.setAdapter(...) straight from that
				// background thread, which Android does not allow (only
				// the thread that created a view hierarchy may touch it) —
				// this is exactly what crashed. The onSuccess case right
				// above already dispatches through runOnUiThread correctly;
				// this sibling path just never did.
				if (getActivity() == null) return;
				getActivity().runOnUiThread(() -> {
					if (btn_members_invite != null) {
						btn_members_invite.setVisibility(canInvite ? View.VISIBLE : View.GONE);
					}
					_loadMembers();
				});
			}
		});
	}
	
	/** Protocol roster wins. Firestore only supplies a name for a wallet the network already lists. */
	private void _replaceWithNodeRoster(List<Map<String, Object>> social) {
		if (getContext() == null || lastGroupData == null) return;
		com.harvana.app.group.GroupIdentity id = com.harvana.app.group.GroupIdentityResolver.resolve(groupId, lastGroupData);
		com.harvana.app.group.GroupRepository.loadMembers(id, new com.harvana.app.group.GroupRepository.Members() {
			@Override public void onError(String error) { }
			@Override public void onMembers(List<Map<String, Object>> rows) {
				if (getActivity() == null) return;
				getActivity().runOnUiThread(() -> {
					java.util.HashMap<String, Map<String, Object>> byWallet = new java.util.HashMap<>();
					if (social != null) {
						for (Map<String, Object> m : social) {
							String w = str(m, "walletAddress", str(m, "wallet", ""));
							if (!w.isEmpty()) byWallet.put(w.toLowerCase(java.util.Locale.US), m);
						}
					}
					displayMembers.clear();
					activeMemberUids.clear();
					for (Map<String, Object> row : rows) {
						String wallet = str(row, "walletAddress", "");
						Map<String, Object> known = byWallet.get(wallet.toLowerCase(java.util.Locale.US));
						HashMap<String, Object> show = new HashMap<>(row);
						String name = known == null ? "" : str(known, "displayName", str(known, "name", str(known, "memberName", "")));
						if (name.isEmpty()) name = wallet.length() > 10 ? wallet.substring(0, 6) + "…" + wallet.substring(wallet.length() - 4) : wallet;
						show.put("displayName", name);
						if (known != null) show.put("memberUid", str(known, "memberUid", ""));
						String state = str(row, "state", "");
						if (!state.isEmpty()) show.put("paymentStatus", state);
						displayMembers.add(show);
					}
					if (tv_members_count != null) tv_members_count.setText("Members · " + displayMembers.size() + " on the network");
					if (tv_members_empty != null) tv_members_empty.setVisibility(displayMembers.isEmpty() ? View.VISIBLE : View.GONE);
					if (recycler_members != null) recycler_members.setAdapter(new MembersAdapter(displayMembers));
				});
			}
		});
	}

	private void _loadMembers() {
		if (groupId.isEmpty() || getContext() == null) return;
		// WhatsApp-style: show the last known member list instantly, before
		// this Firestore REST call completes.
		List<HashMap<String, Object>> cachedMembers =
		com.harvana.app.offline.OfflineCacheManager.loadList(getContext(), "members_" + groupId);
		if (cachedMembers != null && !cachedMembers.isEmpty()) {
			displayMembers.clear();
			activeMemberUids.clear();
			for (HashMap<String, Object> m : cachedMembers) {
				if (JoinRequestManager.isInactiveMemberStatus(str(m, "status", "ACTIVE"))) continue;
				displayMembers.add(m);
				String memberUid = str(m, "memberUid", "");
				if (!memberUid.isEmpty()) activeMemberUids.add(memberUid);
			}
			if (recycler_members != null) recycler_members.setAdapter(new MembersAdapter(displayMembers));
			
			if (tv_members_count != null) {
				String slots = totalSlots > 0
				? (displayMembers.size() + "/" + totalSlots + " slots")
				: (displayMembers.size() + " members");
				tv_members_count.setText("Members · " + slots);
			}
			if (tv_members_empty != null) {
				tv_members_empty.setVisibility(displayMembers.isEmpty() ? View.VISIBLE : View.GONE);
			}
		}
		
		com.harvana.app.firestore.GroupFirestoreManager.getInstance(getContext())
		.getMembers(groupId, new com.harvana.app.firestore.GroupFirestoreManager.ListCallback() {
			@Override
			public void onSuccess(List<Map<String, Object>> members) {
				if (getActivity() == null) return;
				getActivity().runOnUiThread(() -> {
					displayMembers.clear();
					activeMemberUids.clear();
					int maxSlot = 0;
					if (members != null) {
						for (Map<String, Object> m : members) {
							int s = (int) num(m, "slot");
							if (s > maxSlot) maxSlot = s;
							String memberUid = str(m, "memberUid", "");
							String memberStatus = str(m, "status", "ACTIVE");
							if (JoinRequestManager.isInactiveMemberStatus(memberStatus)) continue;
							HashMap<String, Object> row = new HashMap<>(m);
							displayMembers.add(row);
							if (!memberUid.isEmpty()) activeMemberUids.add(memberUid);
						}
					}
					nextSlot = maxSlot + 1;
					// Fresh data now on screen - update the cache so the NEXT
					// open shows this instead of today's snapshot.
					com.harvana.app.offline.OfflineCacheManager.saveList(getContext(), "members_" + groupId, displayMembers);
					if (tv_members_count != null) {
						String slots = totalSlots > 0
						? (displayMembers.size() + "/" + totalSlots + " slots")
						: (displayMembers.size() + " members");
						tv_members_count.setText("Members · " + slots);
					}
					if (tv_members_empty != null)
					tv_members_empty.setVisibility(displayMembers.isEmpty() ? View.VISIBLE : View.GONE);
					if (recycler_members != null)
					recycler_members.setAdapter(new MembersAdapter(displayMembers));
					if (!linkedNodeId.isEmpty()) _replaceWithNodeRoster(members);
					// Chained here rather than called separately by every
					// caller - guarantees activeMemberUids is populated
					// before the pending-list filter runs, closing the real
					// race where pending could be checked before members
					// finished loading.
					_loadPending();
				});
			}
			@Override
			public void onError(String error) {
				if (getActivity() != null)
				getActivity().runOnUiThread(() -> {
					SketchwareUtil.showMessage(getContext(), "Members: " + error);
					// Still attempt pending even if members failed - just
					// without the active-member exclusion filter benefiting
					// from a fresh list. Matches the original independent
					// behavior for this specific failure case.
					_loadPending();
				});
			}
		});
	}
	
	private void _loadPending() {
		if (groupId.isEmpty() || getContext() == null) return;
		// Admin-approval groups: admin-only, exactly as before. Member-vote
		// groups: every member needs to see this to actually cast a vote.
		boolean canSeePending = isAdmin || "MEMBER_VOTE".equals(joinApprovalMethod);
		if (!canSeePending) {
			if (panel_pending != null) panel_pending.setVisibility(View.GONE);
			return;
		}
		com.harvana.app.firestore.GroupFirestoreManager.getInstance(getContext())
		.getJoinRequests(groupId, new com.harvana.app.firestore.GroupFirestoreManager.ListCallback() {
			@Override
			public void onSuccess(List<Map<String, Object>> items) {
				if (getActivity() == null) return;
				getActivity().runOnUiThread(() -> {
					pendingList.clear();
					if (items != null) {
						for (Map<String, Object> r : items) {
							String st = str(r, "status", "PENDING");
							String requesterUid = str(r, "requesterUid", "");
							boolean statusSaysPending = "PENDING".equalsIgnoreCase(st) || st.isEmpty();
							// Defensive: regardless of what the raw status field
							// says, someone who's provably already an active
							// member should never also show as pending. Covers
							// this correctly even if the status write itself
							// has some other issue - the member list is the
							// more authoritative signal here.
							boolean alreadyActive = !requesterUid.isEmpty() && activeMemberUids.contains(requesterUid);
							if (statusSaysPending && !alreadyActive) {
								pendingList.add(new HashMap<>(r));
							}
						}
					}
					if (panel_pending != null)
					panel_pending.setVisibility(pendingList.isEmpty() ? View.GONE : View.VISIBLE);
					if (recycler_pending_members != null)
					recycler_pending_members.setAdapter(new PendingAdapter(pendingList));
				});
			}
			@Override
			public void onError(String error) {
				if (panel_pending != null && getActivity() != null)
				getActivity().runOnUiThread(() -> panel_pending.setVisibility(View.GONE));
			}
		});
	}
	
	private void _showMemberSheet(HashMap<String, Object> member, boolean isPending) {
		if (getContext() == null) return;
		final BottomSheetDialog dialog = new BottomSheetDialog(getContext());
		View sheet = LayoutInflater.from(getContext()).inflate(R.layout.bottom_sheet_member_detail, null);
		dialog.setContentView(sheet);
		com.harvana.app.group.BottomSheetUtils.expandFullScreen(dialog, sheet);
		
		TextView initials = sheet.findViewById(R.id.tv_detail_initials);
		TextView name = sheet.findViewById(R.id.tv_detail_name);
		TextView role = sheet.findViewById(R.id.tv_detail_role);
		TextView slot = sheet.findViewById(R.id.tv_detail_slot);
		TextView status = sheet.findViewById(R.id.tv_detail_status);
		TextView wallet = sheet.findViewById(R.id.tv_detail_wallet);
		TextView balance = sheet.findViewById(R.id.tv_detail_balance);
		TextView trust = sheet.findViewById(getResources().getIdentifier("tv_detail_trust", "id", requireContext().getPackageName()));
		TextView occupation = sheet.findViewById(getResources().getIdentifier("tv_detail_occupation", "id", requireContext().getPackageName()));
		TextView location = sheet.findViewById(getResources().getIdentifier("tv_detail_location", "id", requireContext().getPackageName()));
		TextView phone = sheet.findViewById(getResources().getIdentifier("tv_detail_phone", "id", requireContext().getPackageName()));
		TextView email = sheet.findViewById(getResources().getIdentifier("tv_detail_email", "id", requireContext().getPackageName()));
		TextView close = sheet.findViewById(R.id.btn_detail_close);
		TextView reportBtn = sheet.findViewById(getResources().getIdentifier("btn_detail_report", "id", requireContext().getPackageName()));
		TextView kickBtn = sheet.findViewById(getResources().getIdentifier("btn_detail_kick", "id", requireContext().getPackageName()));
		
		String memberUid = str(member, "memberUid", str(member, "requesterUid", str(member, "_docId", "")));
		
		// Admin-only, never for a pending join request (nothing to report
		// yet), never for yourself.
		String currentUid = FirebaseAuth.getInstance().getCurrentUser() != null
		? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
		boolean canReport = isAdmin && !isPending && !memberUid.isEmpty() && !memberUid.equals(currentUid);
		if (reportBtn != null) {
			reportBtn.setVisibility(canReport ? View.VISIBLE : View.GONE);
			if (canReport) {
				reportBtn.setOnClickListener(v -> {
					dialog.dismiss();
					_showReportSheet(memberUid);
				});
			}
		}
		boolean canKick = canReport && !creatorId.equals(memberUid);
		if (kickBtn != null) {
			kickBtn.setVisibility(canKick ? View.VISIBLE : View.GONE);
			if (canKick) {
				kickBtn.setOnClickListener(v -> {
					dialog.dismiss();
					_confirmKickMember(memberUid);
				});
			}
		}
		
		// Fixed groups: admin can close the month when member has paid all 31 days.
		boolean isFixedGroup = groupType != null && groupType.toLowerCase(java.util.Locale.US).contains("fixed");
		if (isAdmin && !isPending && isFixedGroup && contributionAmount > 0
		&& groupWalletAddress.startsWith("HV0x") && !memberUid.isEmpty()) {
			String monthKey = com.harvana.app.group.FixedPaymentEngine.currentMonthKey();
			java.util.List<Integer> daysPaid = com.harvana.app.group.FixedPaymentEngine.daysPaidFromMember(member, monthKey);
			if (com.harvana.app.group.FixedPaymentEngine.monthComplete(daysPaid)) {
				// Offer close-month via a second action on the report button area,
				// or a simple dialog entry from a long-press alternative.
				// Use an explicit dialog option so we don't need a new layout id.
				TextView closeMonthHint = sheet.findViewById(
				getResources().getIdentifier("btn_detail_close_month", "id", requireContext().getPackageName()));
				if (closeMonthHint != null) {
					closeMonthHint.setVisibility(View.VISIBLE);
					closeMonthHint.setText("Close Fixed Month");
					closeMonthHint.setOnClickListener(v -> {
						dialog.dismiss();
						_startFixedMonthClose(memberUid, str(member, "walletAddress", str(member, "wallet_address", "")), daysPaid);
					});
				} else if (reportBtn != null) {
					// No dedicated button in layout — show an admin choice dialog instead
					// when they open this complete member.
					sheet.post(() -> {
						if (getActivity() == null || getActivity().isFinishing()) return;
						new android.app.AlertDialog.Builder(getActivity())
						.setTitle("Fixed month complete")
						.setMessage("This member paid 31/31 days. Close the month and return 30 days to them?")
						.setPositiveButton("Close month", (d, w) ->
						_startFixedMonthClose(memberUid,
						str(member, "walletAddress", str(member, "wallet_address", "")),
						daysPaid))
						.setNegativeButton("Later", null)
						.show();
					});
				}
			}
		}
		if (slot != null) {
			String s = str(member, "slot", "");
			slot.setText(s.isEmpty() ? "Slot —" : ("Slot " + s));
		}
		if (status != null) status.setText(isPending ? "PENDING join request" : str(member, "status", "ACTIVE"));
		if (role != null) {
			boolean admin = !creatorId.isEmpty() && creatorId.equals(memberUid);
			role.setText(isPending ? "Join request" : (admin ? "Admin" : "Member"));
		}
		if (name != null) com.harvana.app.utils.Shimmer.loading(name);
		if (close != null) close.setOnClickListener(v -> dialog.dismiss());
		
		// Offline-first: this member's profile may have been viewed before
		// (in this group or another) — render whatever's cached immediately
		// instead of leaving everything stuck on "Loading…"/"—" until a
		// network call resolves or fails.
		if (!memberUid.isEmpty()) {
			Map<String, Object> cachedProfile = com.harvana.app.offline.OfflineCacheManager.load(
			getContext(), "member_profile_" + memberUid);
			if (cachedProfile != null && !cachedProfile.isEmpty()) {
				_applyMemberDetailFields(cachedProfile, name, initials, sheet, trust, occupation, location, phone, email);
			}
		}
		
		// Wallet address lives on the group member doc, not the user profile —
		// written by GroupPaymentFragmentActivity.updateMemberFields() on first
		// confirmed contribution. Already available synchronously from `member`,
		// no fetch needed for this part.
		String memberWallet = str(member, "walletAddress", str(member, "wallet_address", ""));
		if (wallet != null) {
			wallet.setText(memberWallet.startsWith("HV0x") ? memberWallet : "No wallet linked");
		}
		if (memberWallet.startsWith("HV0x") && balance != null) {
			com.harvana.app.wallet.WalletNodeBalanceHelper.load(memberWallet,
			(bal, avail, pin, pout, pending, err) -> {
				if (getActivity() == null) return;
				double show = avail >= 0 ? avail : bal;
				getActivity().runOnUiThread(() ->
				balance.setText(com.harvana.app.wallet.BalanceFormatter.smart(show)));
			});
		} else if (balance != null) {
			balance.setText("Balance · —");
		}
		
		if (!memberUid.isEmpty()) {
			com.harvana.app.firestore.UserProfileManager.getInstance(getContext()).getProfile(memberUid,
			new com.harvana.app.firestore.UserProfileManager.DocCallback() {
				@Override
				public void onSuccess(Map<String, Object> data) {
					// THE FIX: this entire block touched views directly from
					// UserProfileManager's callback thread — which, like
					// GroupFirestoreManager, has no internal main-thread
					// dispatch of its own (confirmed: zero runOnUiThread/
					// Handler usage in UserProfileManager.java). Every one
					// of these setText/Glide/setVisibility calls was a real
					// crash risk, not just the onError fallback below.
					if (getActivity() == null) return;
					com.harvana.app.offline.OfflineCacheManager.save(getContext(), "member_profile_" + memberUid, data);
					getActivity().runOnUiThread(() ->
					_applyMemberDetailFields(data, name, initials, sheet, trust, occupation, location, phone, email));
				}
				@Override
				public void onError(String error) {
					if (getActivity() == null) return;
					// A genuine connectivity failure leaves whatever cached
					// render already happened above alone rather than
					// stomping it back down to "Member"/"—".
					if (com.harvana.app.offline.NetworkStatus.isConnectivityError(error)
					&& com.harvana.app.offline.OfflineCacheManager.has(getContext(), "member_profile_" + memberUid)) {
						return;
					}
					getActivity().runOnUiThread(() -> {
						if (name != null) name.setText("Member");
					});
				}
			});
		}
		dialog.show();
	}
	
	/** Shared by _showMemberSheet's cache-first render and its live-fetch
*  onSuccess — one place doing this binding instead of two copies that
*  could drift apart. */
	private void _applyMemberDetailFields(Map<String, Object> data, TextView name, TextView initials,
	View sheet, TextView trust, TextView occupation, TextView location, TextView phone, TextView email) {
		Object fObj = data.get("first_name");
		Object lObj = data.get("last_name");
		String first = fObj != null ? fObj.toString() : null;
		String last = lObj != null ? lObj.toString() : null;
		String full = ((first != null ? first : "Member")
		+ (last != null ? " " + last : "")).trim();
		if (name != null) name.setText(full);
		if (initials != null && full.length() > 0)
		initials.setText(full.substring(0, 1).toUpperCase(Locale.US));
		
		ImageView avatar = sheet.findViewById(
		getResources().getIdentifier("iv_detail_avatar", "id", requireContext().getPackageName()));
		Object iconObj = data.get("icon");
		String iconUrl = iconObj != null ? iconObj.toString() : null;
		if (avatar != null && iconUrl != null && iconUrl.startsWith("http")) {
			avatar.setVisibility(View.VISIBLE);
			if (initials != null) initials.setVisibility(View.GONE);
			try {
				com.bumptech.glide.Glide.with(getContext().getApplicationContext())
				.load(android.net.Uri.parse(iconUrl))
				.circleCrop()
				.into(avatar);
			} catch (Exception ignored) {
				avatar.setVisibility(View.GONE);
				if (initials != null) initials.setVisibility(View.VISIBLE);
			}
		}
		
		if (trust != null) trust.setText("—");
		Object walletObj = data.get("walletAddress");
		if (walletObj == null) walletObj = data.get("wallet_address");
		String wallet = walletObj == null ? "" : String.valueOf(walletObj).trim();
		if (wallet.isEmpty()) {
			com.google.firebase.auth.FirebaseUser me = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
			Object uidObj = data.get("uid");
			if (me != null && uidObj != null && me.getUid().equals(String.valueOf(uidObj))) {
				wallet = com.harvana.app.wallet.WalletManager.getActiveWalletAddress(getContext());
			}
		}
		if (trust != null && wallet != null && wallet.startsWith("HV0x")) {
			final String addr = wallet;
			com.harvana.app.trust.NetworkTrust.wallet(addr, score -> {
				if (getActivity() == null) return;
				getActivity().runOnUiThread(() -> trust.setText(score.label()));
			});
		}
		
		Object occObj = data.get("occupation");
		String occ = occObj != null ? occObj.toString() : null;
		if (occupation != null)
		occupation.setText(occ != null && !occ.isEmpty() ? occ : "—");
		
		Object cityObj = data.get("city");
		Object stateObj = data.get("state");
		Object countryObj = data.get("country");
		String city = cityObj != null ? cityObj.toString() : null;
		String state = stateObj != null ? stateObj.toString() : null;
		String country = countryObj != null ? countryObj.toString() : null;
		String loc = "";
		if (city != null && !city.isEmpty()) loc = city;
		if (state != null && !state.isEmpty())
		loc = loc.isEmpty() ? state : loc + ", " + state;
		if (country != null && !country.isEmpty())
		loc = loc.isEmpty() ? country : loc + ", " + country;
		if (location != null)
		location.setText(loc.isEmpty() ? "—" : loc);
		
		Object phObj = data.get("phone");
		String ph = phObj != null ? phObj.toString() : null;
		if (phone != null)
		phone.setText(ph != null && !ph.isEmpty() ? ph : "—");
		
		Object emObj = data.get("email");
		String em = emObj != null ? emObj.toString() : null;
		if (email != null)
		email.setText(em != null && !em.isEmpty() ? em : "—");
	}
	
	/**
* Admin Fixed-month close for one member who paid 31/31 days.
* Transfers 30 × dailyAmount from group wallet → member, then records
* both ledger entries (member payout + admin fee) via FixedMonthCloseHelper.
*/
	private void _startFixedMonthClose(String memberUid, String memberWallet,
	java.util.List<Integer> daysPaid) {
		if (getContext() == null || getActivity() == null) return;
		if (memberWallet == null || !memberWallet.startsWith("HV0x")) {
			SketchwareUtil.showMessage(getContext(), ("Member has no linked " + com.harvana.app.CoinInfo.ticker() + " wallet."));
			return;
		}
		if (!groupWalletAddress.startsWith("HV0x")) {
			SketchwareUtil.showMessage(getContext(), "Group wallet not loaded.");
			return;
		}
		
		com.harvana.app.group.FixedMonthCloseHelper.evaluate(
		getContext(), groupId, memberUid, contributionAmount, daysPaid,
		new com.harvana.app.group.FixedMonthCloseHelper.Callback() {
			@Override
			public void onReady(double memberPayoutHarv, double adminFeeHarv, String message) {
				if (getActivity() == null) return;
				getActivity().runOnUiThread(() -> {
					java.util.ArrayList<String[]> rows = new java.util.ArrayList<>();
					rows.add(new String[]{"From", "Group wallet"});
					rows.add(new String[]{"To", memberWallet});
					rows.add(new String[]{"Member receives", com.harvana.app.wallet.BalanceFormatter.smart(memberPayoutHarv)});
					rows.add(new String[]{"Admin fee kept", com.harvana.app.wallet.BalanceFormatter.smart(adminFeeHarv)});
					rows.add(new String[]{"For", message});
					com.harvana.app.group.PaymentConfirmSheet.reviewThenAuthorize(getActivity(),
					"Close this fixed month",
					com.harvana.app.wallet.BalanceFormatter.smart(memberPayoutHarv),
					rows, () -> _executeFixedMonthTransfer(memberUid, memberWallet, memberPayoutHarv, adminFeeHarv));
				});
			}
			
			@Override
			public void onError(String error) {
				if (getContext() != null) {
					SketchwareUtil.showMessage(getContext(), error);
				}
			}
		});
	}
	
	private void _executeFixedMonthTransfer(String memberUid, String memberWallet,
	double memberPayoutHarv, double adminFeeHarv) {
		if (getActivity() == null) return;
		final com.harvana.app.group.TransactionProgressSheet progress =
		com.harvana.app.group.TransactionProgressSheet.show(
		getActivity(), "Closing Fixed Month", memberPayoutHarv);
		progress.advanceTo(2);
		
		try {
				com.harvana.app.tx.TransactionRouter.routeTransaction(
				getActivity(),
				groupWalletAddress,
				memberWallet,
				java.math.BigDecimal.valueOf(memberPayoutHarv),
				null,
				"Fixed month close · return to member",
				new com.harvana.app.tx.TransactionRouter.RouterCallback() {
					@Override
					public void onSuccess(String txHash) {
						if (getActivity() == null) return;
						getActivity().runOnUiThread(() -> {
							progress.advanceTo(3);
							com.harvana.app.group.FixedMonthCloseHelper.recordClose(
							getContext(), groupId, memberUid,
							memberPayoutHarv, adminFeeHarv,
							groupWalletAddress, memberWallet, txHash,
							new com.harvana.app.group.FixedMonthCloseHelper.CloseCallback() {
								@Override
								public void onSuccess(String message) {
									if (getActivity() == null) return;
									getActivity().runOnUiThread(() -> {
										progress.showConfirmed();
										SketchwareUtil.showMessage(getContext(), message);
									});
								}
								
								@Override
								public void onError(String error) {
									if (getActivity() == null) return;
									getActivity().runOnUiThread(() -> {
										progress.showConfirmed();
										SketchwareUtil.showMessage(getContext(),
										"Transfer sent. Ledger note: " + error);
									});
								}
							});
						});
					}
					
					@Override
					public void onError(String code, String msg) {
						if (getActivity() == null) return;
						getActivity().runOnUiThread(() ->
						progress.showError("Transfer failed: " + msg));
					}
				});
			} catch (Exception e) {
				progress.showError(e.getMessage() != null ? e.getMessage() : "Transfer failed");
			}
	}
	
	private void _confirmKickMember(String targetUid) {
		if (getContext() == null || targetUid == null || targetUid.isEmpty()) return;
		com.harvana.app.p2p.ThemedDialogs.show(com.harvana.app.p2p.ThemedDialogs.alert(getContext())
		.setTitle("Remove this member?")
		.setMessage("They'll lose access to this group and won't be able to rejoin unless you unblock them in Settings.")
		.setPositiveButton("Remove", (d, w) -> {
			JoinRequestManager.kickMember(getContext(), groupId, targetUid, new JoinRequestManager.Callback() {
				@Override
				public void onSuccess(String message) {
					if (getActivity() == null) return;
					getActivity().runOnUiThread(() -> {
						SketchwareUtil.showMessage(getContext(),
						message != null ? message : "Member removed.");
						_loadMembers();
					});
				}
				@Override
				public void onError(String error) {
					if (getActivity() == null) return;
					getActivity().runOnUiThread(() ->
					SketchwareUtil.showMessage(getContext(), "Couldn't remove member: " + error));
				}
			});
		})
		.setNegativeButton("Cancel", null));
	}
	
	/** Report submission - the user side only. Every report is created
*  PENDING and stays invisible to everyone but a future moderator until
*  reviewed; nothing here confirms or acts on anything. */
	private void _showReportSheet(String reportedUid) {
		if (getContext() == null) return;
		final BottomSheetDialog dialog = new BottomSheetDialog(getContext());
		View sheet = LayoutInflater.from(getContext()).inflate(R.layout.bottom_sheet_submit_report, null);
		dialog.setContentView(sheet);
		com.harvana.app.group.BottomSheetUtils.expandFullScreen(dialog, sheet);
		
		TextView target = sheet.findViewById(R.id.tv_report_target);
		Spinner reasonSpinner = sheet.findViewById(R.id.spinner_report_reason);
		EditText details = sheet.findViewById(R.id.et_report_details);
		View btnCancel = sheet.findViewById(R.id.btn_report_cancel);
		View btnSubmit = sheet.findViewById(R.id.btn_report_submit);
		
		if (target != null) target.setText("Reporting: Loading…");
		// Same fix as the loan borrower lookup: Firestore is the real profile
		// source post-migration, not legacy RTDB userdb/{uid}.
		com.harvana.app.group.MemberNameResolver.resolve(getContext(), reportedUid, "Member", resolvedName -> {
			if (target != null) target.setText("Reporting: " + resolvedName);
		});
		
		ArrayList<String> reasons = new ArrayList<>(Arrays.asList(
		"Select a reason", "Scam", "Fraud", "Misconduct", "Bad behavior", "Other"));
		if (reasonSpinner != null) {
			reasonSpinner.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, reasons));
		}
		
		if (btnCancel != null) btnCancel.setOnClickListener(v -> dialog.dismiss());
		if (btnSubmit != null) {
			btnSubmit.setOnClickListener(v -> {
				int reasonPos = reasonSpinner != null ? reasonSpinner.getSelectedItemPosition() : 0;
				if (reasonPos <= 0) {
					SketchwareUtil.showMessage(getContext(), "Select a reason first.");
					return;
				}
				String reasonText = reasons.get(reasonPos).toUpperCase(Locale.US).replace(" ", "_");
				String detailsText = details != null ? details.getText().toString().trim() : "";
				if (detailsText.isEmpty()) {
					SketchwareUtil.showMessage(getContext(), "Add some detail - dates, amounts, what happened.");
					return;
				}
				
				String currentUid = FirebaseAuth.getInstance().getCurrentUser() != null
				? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
				btnSubmit.setEnabled(false);
				
				com.harvana.app.firestore.GroupFirestoreManager.getInstance(getContext())
				.submitReport(reportedUid, currentUid, groupId, "", reasonText, detailsText,
				new com.harvana.app.firestore.GroupFirestoreManager.ReportCallback() {
					@Override
					public void onSuccess(String reportId) {
						if (getActivity() == null) return;
						getActivity().runOnUiThread(() -> {
							dialog.dismiss();
							SketchwareUtil.showMessage(getContext(), "Report submitted for review.");
						});
					}
					@Override
					public void onError(String error) {
						if (getActivity() == null) return;
						getActivity().runOnUiThread(() -> {
							btnSubmit.setEnabled(true);
							SketchwareUtil.showMessage(getContext(), "Failed to submit: " + error);
						});
					}
				});
			});
		}
		
		dialog.show();
	}
	
	// ── Active members adapter ────────────────────────────────────────
	
	public class MembersAdapter extends RecyclerView.Adapter<MembersAdapter.VH> {
		private final ArrayList<HashMap<String, Object>> data;
		MembersAdapter(ArrayList<HashMap<String, Object>> data) { this.data = data; }
		
		@NonNull
		@Override
		public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			View v = LayoutInflater.from(parent.getContext())
			.inflate(R.layout.member_user_card, parent, false);
			return new VH(v);
		}
		
		@Override
		public void onBindViewHolder(@NonNull VH h, int position) {
			HashMap<String, Object> member = data.get(position);
			final String memberUid = str(member, "memberUid", "");
			Object slotObj = member.get("slot");
			if (h.slot != null) h.slot.setText("Slot " + (slotObj != null ? slotObj.toString() : "—"));
			boolean memberIsAdmin = !creatorId.isEmpty() && creatorId.equals(memberUid);
			if (h.role != null) {
				h.role.setVisibility(memberIsAdmin ? View.VISIBLE : View.GONE);
				h.role.setText("Admin");
			}
			if (h.name != null) com.harvana.app.utils.Shimmer.loading(h.name);
			if (h.wallet != null) h.wallet.setText("—");
			if (h.status != null) h.status.setText(str(member, "status", "Active"));
			if (h.balance != null) h.balance.setText("");
			if (h.initials != null) h.initials.setText("?");
			h.itemView.setTag(memberUid);
			h.itemView.setOnClickListener(v -> _showMemberSheet(member, false));
			
			if (h.wallet != null) {
				String memberWallet = str(member, "walletAddress", str(member, "wallet_address", ""));
				if (memberWallet.startsWith("HV0x")) {
					h.wallet.setText(_shortAddr(memberWallet));
					_fetchBalance(memberWallet, h.balance);
				} else {
					h.wallet.setText("No wallet");
				}
			}
			
			if (!memberUid.isEmpty()) {
				// 1. Instantly load from cache to prevent "Loading..." or "Member" flashes
				Map<String, Object> cachedProfile = com.harvana.app.offline.OfflineCacheManager.load(getContext(), "member_profile_" + memberUid);
				if (cachedProfile != null && !cachedProfile.isEmpty()) {
					Object fObj = cachedProfile.get("first_name");
					Object lObj = cachedProfile.get("last_name");
					String first = fObj != null ? fObj.toString() : null;
					String last = lObj != null ? lObj.toString() : null;
					String full = ((first != null ? first : "Member") + (last != null ? " " + last : "")).trim();
					if (h.name != null) h.name.setText(full);
					if (h.initials != null && full.length() > 0)
					h.initials.setText(full.substring(0, 1).toUpperCase(Locale.US));
				}
				
				// 2. Fetch live data
				com.harvana.app.firestore.UserProfileManager.getInstance(getContext()).getProfile(memberUid,
				new com.harvana.app.firestore.UserProfileManager.DocCallback() {
					@Override
					public void onSuccess(Map<String, Object> data) {
						if (getActivity() == null) return;
						
						// Save fresh profile to cache for offline use
						com.harvana.app.offline.OfflineCacheManager.save(getContext(), "member_profile_" + memberUid, data);
						
						getActivity().runOnUiThread(() -> {
							if (h.itemView.getTag() == null || !h.itemView.getTag().equals(memberUid)) return;
							Object fObj = data.get("first_name");
							Object lObj = data.get("last_name");
							String first = fObj != null ? fObj.toString() : null;
							String last = lObj != null ? lObj.toString() : null;
							String full = ((first != null ? first : "Member")
							+ (last != null ? " " + last : "")).trim();
							if (h.name != null) h.name.setText(full);
							if (h.initials != null && full.length() > 0)
							h.initials.setText(full.substring(0, 1).toUpperCase(Locale.US));
							_applyPresence(h, data);
						});
					}
					@Override
					public void onError(String error) {
						if (getActivity() == null) return;
						
						// Prevent stomping existing cached name with "Member" during network error
						if (com.harvana.app.offline.NetworkStatus.isConnectivityError(error) 
						&& com.harvana.app.offline.OfflineCacheManager.has(getContext(), "member_profile_" + memberUid)) {
							return;
						}
						
						getActivity().runOnUiThread(() -> {
							if (h.itemView.getTag() == null || !h.itemView.getTag().equals(memberUid)) return;
							if (h.name != null) h.name.setText("Member");
						});
					}
				});
			}
		}
		
		@Override
		public int getItemCount() { return data.size(); }
		
		/** Circular ring around the avatar: green = online now, amber = away, thin grey = offline. */
		private void _applyPresence(VH h, Map<String, Object> profile) {
			long now = System.currentTimeMillis();
			com.harvana.app.group.PresenceState st = com.harvana.app.group.PresenceState.of(profile, now);
			View ring = h.itemView.findViewById(R.id.view116);
			View dot = h.itemView.findViewById(R.id.dot_member_online);
			if (ring != null) {
				int res = st == com.harvana.app.group.PresenceState.ONLINE ? R.drawable.presence_ring_online
				: st == com.harvana.app.group.PresenceState.AWAY ? R.drawable.presence_ring_away
				: R.drawable.presence_ring_offline;
				ring.setBackgroundResource(res);
			}
			if (dot != null) dot.setVisibility(st == com.harvana.app.group.PresenceState.ONLINE ? View.VISIBLE : View.GONE);
			String label = com.harvana.app.group.PresenceState.label(profile, now);
			if (h.status != null && !label.isEmpty()) h.status.setText(label);
		}
		
		private void _fetchBalance(String address, TextView target) {
			if (target == null) return;
			com.harvana.app.wallet.WalletNodeBalanceHelper.load(address,
			(bal, avail, pin, pout, pending, err) -> {
				if (getActivity() == null) return;
				getActivity().runOnUiThread(() ->
				target.setText(String.format(Locale.US, ("%.2f " + com.harvana.app.CoinInfo.ticker()), avail >= 0 ? avail : bal)));
			});
		}
		
		private String _shortAddr(String a) {
			if (a == null || a.length() < 14) return a != null ? a : "";
			return a.substring(0, 10) + "…" + a.substring(a.length() - 4);
		}
		
		class VH extends RecyclerView.ViewHolder {
			TextView name, wallet, slot, status, role, initials, balance;
			VH(View v) {
				super(v);
				name = _id(v, "tv_member_name");
				wallet = _id(v, "tv_member_wallet");
				slot = _id(v, "tv_member_slot");
				status = _id(v, "tv_member_status");
				role = _id(v, "tv_member_role");
				initials = _id(v, "tv_member_initials");
				balance = _id(v, "tv_member_balance");
			}
			TextView _id(View v, String n) {
				int r = v.getResources().getIdentifier(n, "id", v.getContext().getPackageName());
				return r != 0 ? v.findViewById(r) : null;
			}
		}
	}
	
	// ── Pending requests adapter ──────────────────────────────────────
	
	public class PendingAdapter extends RecyclerView.Adapter<PendingAdapter.VH> {
		private final ArrayList<HashMap<String, Object>> data;
		PendingAdapter(ArrayList<HashMap<String, Object>> data) { this.data = data; }
		
		@NonNull
		@Override
		public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			View v = LayoutInflater.from(parent.getContext())
			.inflate(R.layout.member_pending_card, parent, false);
			return new VH(v);
		}
		
		@Override
		public void onBindViewHolder(@NonNull VH h, int position) {
			HashMap<String, Object> req = data.get(position);
			final String reqUid = str(req, "memberUid", str(req, "requesterUid",
			str(req, "_docId", str(req, "id", ""))));
			final String docId = str(req, "_docId", reqUid);
			
			if (h.name != null) com.harvana.app.utils.Shimmer.loading(h.name);
			if (h.meta != null) h.meta.setText("Wants to join");
			if (h.initials != null) h.initials.setText("?");
			
			h.itemView.setOnClickListener(v -> _showMemberSheet(req, true));
			
			boolean isVoteMode = "MEMBER_VOTE".equals(joinApprovalMethod);
			String currentUid = FirebaseAuth.getInstance().getCurrentUser() != null
			? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
			
			if (isVoteMode) {
				// Can't vote on your own request, and can't vote twice —
				// both real conditions, not just UI polish, since the
				// underlying castJoinVote() call would reject a repeat
				// vote anyway. Checking here avoids a pointless round-trip
				// and gives the person a clear reason up front.
				boolean isOwnRequest = !currentUid.isEmpty() && currentUid.equals(reqUid);
				boolean alreadyVoted = false;
				Object votesObj = req.get("votes");
				if (votesObj instanceof Map) {
					alreadyVoted = ((Map<?, ?>) votesObj).containsKey(currentUid);
				}
				
				if (isOwnRequest) {
					if (h.accept != null) h.accept.setVisibility(View.GONE);
					if (h.reject != null) h.reject.setVisibility(View.GONE);
					if (h.meta != null) h.meta.setText("Your request — waiting on other members' votes");
				} else if (alreadyVoted) {
					if (h.accept != null) h.accept.setVisibility(View.GONE);
					if (h.reject != null) h.reject.setVisibility(View.GONE);
					if (h.meta != null) h.meta.setText("You already voted on this request");
				} else {
					if (h.accept != null) {
						h.accept.setVisibility(View.VISIBLE);
						h.accept.setText("Vote Yes");
						h.accept.setEnabled(true);
						h.accept.setOnClickListener(v -> _castVote(h, req, docId, reqUid, currentUid, true));
					}
					if (h.reject != null) {
						h.reject.setVisibility(View.VISIBLE);
						h.reject.setText("Vote No");
						h.reject.setEnabled(true);
						h.reject.setOnClickListener(v -> _castVote(h, req, docId, reqUid, currentUid, false));
					}
				}
			} else {
				if (h.accept != null) {
					h.accept.setVisibility(View.VISIBLE);
					h.accept.setText("Accept");
					h.accept.setOnClickListener(v -> {
						h.accept.setEnabled(false);
						h.accept.setText("…");
						JoinRequestManager.approve(getContext(), groupId, docId, reqUid, nextSlot,
						new JoinRequestManager.Callback() {
							@Override
							public void onSuccess(String message) {
								if (getActivity() == null) return;
								getActivity().runOnUiThread(() -> {
									SketchwareUtil.showMessage(getContext(), message);
									nextSlot++;
									_loadMembers();
								});
							}
							@Override
							public void onError(String error) {
								if (getActivity() == null) return;
								getActivity().runOnUiThread(() -> {
									h.accept.setEnabled(true);
									h.accept.setText("Accept");
									SketchwareUtil.showMessage(getContext(), error);
								});
							}
						});
					});
				}
				if (h.reject != null) {
					h.reject.setVisibility(View.VISIBLE);
					h.reject.setText("Reject");
					h.reject.setOnClickListener(v ->
					JoinRequestManager.reject(getContext(), groupId, docId,
					new JoinRequestManager.Callback() {
						@Override
						public void onSuccess(String message) {
							if (getActivity() == null) return;
							getActivity().runOnUiThread(() -> {
								SketchwareUtil.showMessage(getContext(), message);
								_loadPending();
							});
						}
						@Override
						public void onError(String error) {
							if (getActivity() != null)
							getActivity().runOnUiThread(() ->
							SketchwareUtil.showMessage(getContext(), error));
						}
					}));
				}
			}
			
			if (!reqUid.isEmpty()) {
				// Same fix as everywhere else this session: Firestore is the
				// real profile source post-migration, not legacy RTDB
				// userdb/{uid}. Manual tag-guard here (rather than relying
				// only on resolveInto()'s internal one) since this row also
				// needs the initials avatar updated, not just the name.
				if (h.initials != null) h.initials.setTag(reqUid);
				com.harvana.app.group.MemberNameResolver.resolve(getContext(), reqUid, "User", full -> {
					if (h.initials != null && !reqUid.equals(h.initials.getTag())) return;
					if (h.name != null) h.name.setText(full);
					if (h.initials != null && full.length() > 0)
					h.initials.setText(full.substring(0, 1).toUpperCase(Locale.US));
				});
			}
		}
		
		@Override
		public int getItemCount() { return data.size(); }
		
		class VH extends RecyclerView.ViewHolder {
			TextView name, meta, initials, accept, reject;
			VH(View v) {
				super(v);
				name = v.findViewById(R.id.tv_pending_name);
				meta = v.findViewById(R.id.tv_pending_meta);
				initials = v.findViewById(R.id.tv_pending_initials);
				accept = v.findViewById(R.id.btn_accept_request);
				reject = v.findViewById(R.id.btn_reject_request);
			}
		}
	}
	
	private void _castVote(PendingAdapter.VH h, HashMap<String, Object> req, String docId,
	String reqUid, String currentUid, boolean approve) {
		if (h.accept != null) h.accept.setEnabled(false);
		if (h.reject != null) h.reject.setEnabled(false);
		
		com.harvana.app.firestore.GroupFirestoreManager.getInstance(getContext())
		.castJoinVote(groupId, reqUid, currentUid, approve,
		new com.harvana.app.firestore.GroupFirestoreManager.VoteCallback() {
			@Override
			public void onVoteCast(String resolution, int yesVotes, int noVotes, int totalMembers) {
				if (getActivity() == null) return;
				getActivity().runOnUiThread(() -> {
					String message;
					if ("APPROVED".equals(resolution)) {
						message = "Request approved by vote — member added.";
					} else if ("REJECTED".equals(resolution)) {
						message = "Request rejected by vote.";
					} else {
						message = "Vote recorded (" + yesVotes + " yes, " + noVotes + " no, need "
						+ (int) Math.ceil(0.60 * totalMembers) + " of " + totalMembers + ").";
					}
					SketchwareUtil.showMessage(getContext(), message);
					_loadMembers();
				});
			}
			@Override
			public void onError(String error) {
				if (getActivity() == null) return;
				getActivity().runOnUiThread(() -> {
					if (h.accept != null) h.accept.setEnabled(true);
					if (h.reject != null) h.reject.setEnabled(true);
					SketchwareUtil.showMessage(getContext(), error);
				});
			}
		});
	}
	
	private static String str(Map<String, Object> m, String k, String d) {
		Object v = m.get(k);
		if (v == null) return d;
		String s = v.toString().trim();
		return s.isEmpty() || "null".equalsIgnoreCase(s) ? d : s;
	}
	
	private static double num(Map<String, Object> m, String k) {
		Object v = m.get(k);
		if (v instanceof Number) return ((Number) v).doubleValue();
		if (v != null) {
			try { return Double.parseDouble(v.toString()); } catch (Exception ignored) {}
		}
		return 0;
	}
	
	{
	}
	
}
