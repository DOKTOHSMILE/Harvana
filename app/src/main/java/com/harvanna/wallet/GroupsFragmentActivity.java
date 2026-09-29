package com.harvanna.wallet;

import android.animation.*;
import android.app.*;
import android.app.Activity;
import android.content.*;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.media.*;
import android.net.*;
import android.net.Uri;
import android.os.*;
import android.text.*;
import android.text.style.*;
import android.util.*;
import android.view.*;
import android.view.View;
import android.view.View.*;
import android.view.animation.*;
import android.widget.*;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.*;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.budiyev.android.codescanner.*;
import com.bumptech.glide.*;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.card.*;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.*;
import org.json.*;

public class GroupsFragmentActivity extends Fragment {
	
	private FirebaseDatabase _firebase = FirebaseDatabase.getInstance();
	
	private String uid = "";
	
	private ArrayList<HashMap<String, Object>> listMap = new ArrayList<>();
	private ArrayList<HashMap<String, Object>> myGroupMap = new ArrayList<>();
	private ArrayList<HashMap<String, Object>> generalGroup = new ArrayList<>();
	private ArrayList<HashMap<String, Object>> myGroups = new ArrayList<>();
	
	private NestedScrollView vscroll4;
	private LinearLayout linear1;
	private LinearLayout linear2;
	private MaterialCardView cardview47;
	private LinearLayout linear104;
	private MaterialCardView cardview10;
	private LinearLayout linear3;
	private MaterialCardView cardview1;
	private TextView textview1;
	private TextView textview2;
	private TextView textview3;
	private LinearLayout linear96;
	private LinearLayout linear97;
	private View view98;
	private LinearLayout linear99;
	private View view100;
	private LinearLayout linear101;
	private View view102;
	private LinearLayout linear103;
	private TextView tv_stat_active_groups;
	private TextView textview68;
	private TextView tv_stat_total_members;
	private TextView textview69;
	private TextView tv_stat_total_saved;
	private TextView textview70;
	private TextView tv_stat_ongoing_cycles;
	private TextView textview71;
	private MaterialCardView cardview48;
	private TextView btn_filter_groups;
	private LinearLayout linear105;
	private EditText et_search_groups;
	private ListView listview1;
	
	private Intent i = new Intent();
	private SharedPreferences groupsp;
	private SharedPreferences authsp;
	private DatabaseReference groupdb = _firebase.getReference("groupdb");
	private ChildEventListener _groupdb_child_listener;
	private DatabaseReference userdb = _firebase.getReference("userdb");
	private ChildEventListener _userdb_child_listener;
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
		View _view = _inflater.inflate(R.layout.groups_fragment, _container, false);
		initialize(_savedInstanceState, _view);
		FirebaseApp.initializeApp(getContext());
		initializeLogic();
		return _view;
	}
	
	private void initialize(Bundle _savedInstanceState, View _view) {
		vscroll4 = _view.findViewById(R.id.vscroll4);
		linear1 = _view.findViewById(R.id.linear1);
		linear2 = _view.findViewById(R.id.linear2);
		cardview47 = _view.findViewById(R.id.cardview47);
		linear104 = _view.findViewById(R.id.linear104);
		cardview10 = _view.findViewById(R.id.cardview10);
		linear3 = _view.findViewById(R.id.linear3);
		cardview1 = _view.findViewById(R.id.cardview1);
		textview1 = _view.findViewById(R.id.textview1);
		textview2 = _view.findViewById(R.id.textview2);
		textview3 = _view.findViewById(R.id.textview3);
		linear96 = _view.findViewById(R.id.linear96);
		linear97 = _view.findViewById(R.id.linear97);
		view98 = _view.findViewById(R.id.view98);
		linear99 = _view.findViewById(R.id.linear99);
		view100 = _view.findViewById(R.id.view100);
		linear101 = _view.findViewById(R.id.linear101);
		view102 = _view.findViewById(R.id.view102);
		linear103 = _view.findViewById(R.id.linear103);
		tv_stat_active_groups = _view.findViewById(R.id.tv_stat_active_groups);
		textview68 = _view.findViewById(R.id.textview68);
		tv_stat_total_members = _view.findViewById(R.id.tv_stat_total_members);
		textview69 = _view.findViewById(R.id.textview69);
		tv_stat_total_saved = _view.findViewById(R.id.tv_stat_total_saved);
		textview70 = _view.findViewById(R.id.textview70);
		tv_stat_ongoing_cycles = _view.findViewById(R.id.tv_stat_ongoing_cycles);
		textview71 = _view.findViewById(R.id.textview71);
		cardview48 = _view.findViewById(R.id.cardview48);
		btn_filter_groups = _view.findViewById(R.id.btn_filter_groups);
		linear105 = _view.findViewById(R.id.linear105);
		et_search_groups = _view.findViewById(R.id.et_search_groups);
		listview1 = _view.findViewById(R.id.listview1);
		groupsp = getContext().getSharedPreferences("groupsp", Activity.MODE_PRIVATE);
		authsp = getContext().getSharedPreferences("authsp", Activity.MODE_PRIVATE);
		fauth = FirebaseAuth.getInstance();
		
		cardview1.setOnClickListener(_v -> {
			i.setClass(getContext().getApplicationContext(), CreateNewGroupActivity.class);
			startActivity(i);
		});
		
		listview1.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> _param1, View _param2, int _param3, long _param4) {
				final int _position = _param3;
				if (_position < 0 || _position >= myGroups.size()) return;
				Object gid = myGroups.get(_position).get("groupId");
				if (gid == null || gid.toString().isEmpty()) return;
				i.putExtra("groupId", gid.toString());
				i.setClass(getContext().getApplicationContext(), GroupDetailsActivity.class);
				startActivity(i);
			}
		});
		
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
	}
	
	@Override
	public void onResume() {
		super.onResume();
		uid = FirebaseAuth.getInstance().getCurrentUser() != null
		? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
		if (uid.isEmpty()) return;
		_loadMyGroups();
		
	}
	public void _MORE() {
	}
	private void _loadMyGroups() {
		List<HashMap<String, Object>> cachedGroups =
		com.harvanna.wallet.offline.OfflineCacheManager.loadList(getContext(), "my_groups_" + uid);
		if (cachedGroups != null && !cachedGroups.isEmpty() && getActivity() != null) {
			myGroups.clear();
			myGroups.addAll(cachedGroups);
			listview1.setAdapter(new Listview1Adapter(myGroups));
		}
		
		com.harvanna.wallet.firestore.UserProfileManager.getInstance(getContext()).getProfile(uid,
		new com.harvanna.wallet.firestore.UserProfileManager.DocCallback() {
			@Override
			public void onSuccess(Map<String, Object> data) {
				List<String> groupIds = new ArrayList<>();
				Object rawGroups = data.get("groups");
				if (rawGroups instanceof List) {
					for (Object g : (List<?>) rawGroups) {
						if (g != null && !g.toString().isEmpty()) groupIds.add(g.toString());
					}
				}
				if (groupIds.isEmpty()) {
					if (getActivity() == null) return;
					getActivity().runOnUiThread(() -> {
						myGroups.clear();
						listview1.setAdapter(new Listview1Adapter(myGroups));
					});
					return;
				}
				_fetchGroupDetails(groupIds);
			}
			
			@Override
			public void onError(String error) {
				// 404 = no profile doc / no groups field yet — treat as empty, not an error
				if (error != null && error.contains("404")) {
					if (getActivity() == null) return;
					getActivity().runOnUiThread(() -> {
						myGroups.clear();
						listview1.setAdapter(new Listview1Adapter(myGroups));
					});
					return;
				}
				// A genuine connectivity failure leaves whatever cached list
				// was already rendered above alone instead of covering it
				// with an error toast.
				if (com.harvanna.wallet.offline.NetworkStatus.isConnectivityError(error)
				&& cachedGroups != null && !cachedGroups.isEmpty()) {
					return;
				}
				if (getActivity() == null) return;
				getActivity().runOnUiThread(() ->
				SketchwareUtil.showMessage(getContext(), "Failed to load your groups: " + error));
			}
		});
	}
	
	private void _fetchGroupDetails(List<String> groupIds) {
		// FIX: was a plain append-on-arrival list - N independent parallel
		// network calls complete in whatever order network latency happens
		// to produce, which is different every single load. That's exactly
		// the "shuffling" symptom. Using an index-preserving array instead:
		// every group lands in the same position regardless of which
		// network call happens to finish first, keeping a stable order
		// matching the user's real join order from their groups list.
		final HashMap<String, Object>[] resultsByIndex = new HashMap[groupIds.size()];
		final int[] remaining = {groupIds.size()};
		final boolean[] hadConnectivityFailure = {false};
		
		for (int i = 0; i < groupIds.size(); i++) {
			final String groupId = groupIds.get(i);
			final int index = i;
			com.harvanna.wallet.firestore.GroupFirestoreManager.getInstance(getContext())
			.getGroup(groupId, new com.harvanna.wallet.firestore.GroupFirestoreManager.DocCallback() {
				@Override
				public void onSuccess(Map<String, Object> data) {
					HashMap<String, Object> row = new HashMap<>();
					if (data != null) row.putAll(data);
					row.put("groupId", groupId);
					resultsByIndex[index] = row;
					_checkComplete();
				}
				
				@Override
				public void onError(String error) {
					// A group that failed to load still occupies its slot as
					// null rather than being silently dropped or shifting
					// everything after it - filtered out below, not left as
					// a gap that would itself shuffle remaining positions.
					if (com.harvanna.wallet.offline.NetworkStatus.isConnectivityError(error)) {
						hadConnectivityFailure[0] = true;
					}
					_checkComplete();
				}
				
				private void _checkComplete() {
					remaining[0]--;
					if (remaining[0] <= 0 && getActivity() != null) {
						// THE FIX: this used to unconditionally rebuild and
						// re-render myGroups here regardless of outcome — so
						// a dropped connection meant every single fetch
						// failed, resultsByIndex stayed all-null, and this
						// wiped the perfectly good cache-rendered list from
						// _loadMyGroups() down to empty a few seconds after
						// it first showed correctly. That's the exact
						// "shows data then blanks out" symptom. Now: only
						// replace what's on screen if at least one group
						// genuinely came back fresh, or there was no
						// connectivity problem at all (a real, non-network
						// failure like a deleted group should still update
						// the list).
						boolean gotAnyFreshData = false;
						for (HashMap<String, Object> row : resultsByIndex) {
							if (row != null) { gotAnyFreshData = true; break; }
						}
						if (!gotAnyFreshData && hadConnectivityFailure[0]) {
							return;
						}
						getActivity().runOnUiThread(() -> {
							myGroups.clear();
							for (HashMap<String, Object> row : resultsByIndex) {
								if (row != null) myGroups.add(row);
							}
							listview1.setAdapter(new Listview1Adapter(myGroups));
							// Update the cache with what's now genuinely fresh,
							// so the NEXT open shows this instead of today's data.
							com.harvanna.wallet.offline.OfflineCacheManager.saveList(getContext(), "my_groups_" + uid, myGroups);
						});
					}
				}
			});
		}
	}
	
	private double _safeDouble(Map<String, Object> data, String key) {
		Object v = data.get(key);
		if (v == null) return 0;
		if (v instanceof Number) return ((Number) v).doubleValue();
		try {
			return Double.parseDouble(v.toString());
		} catch (Exception e) {
			return 0;
		}
	}
	
	private String _safeString(Map<String, Object> data, String key, String fallback) {
		Object v = data.get(key);
		if (v == null) return fallback;
		String s = v.toString().trim();
		return s.isEmpty() || "null".equalsIgnoreCase(s) ? fallback : s;
	}
	
	private void setupCardExpansion(HashMap<String, Object> group, LinearLayout lin, ImageView img_hide, androidx.cardview.widget.CardView cad) {
		cad.setClipToOutline(true);
		
		boolean isExpanded = group.containsKey("isExpanded") && (boolean) group.get("isExpanded");
		lin.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
		img_hide.setRotation(isExpanded ? 180f : 0f);
		
		img_hide.setOnClickListener(v -> {
			boolean currentExpanded = group.containsKey("isExpanded") && (boolean) group.get("isExpanded");
			boolean newState = !currentExpanded;
			group.put("isExpanded", newState);
			
			lin.setVisibility(newState ? View.VISIBLE : View.GONE);
			img_hide.setRotation(newState ? 180f : 0f);
		});
	}
	
	{
	}
	
	public class Listview1Adapter extends BaseAdapter {
		
		ArrayList<HashMap<String, Object>> _data;
		
		public Listview1Adapter(ArrayList<HashMap<String, Object>> _arr) {
			_data = _arr;
		}
		
		@Override
		public int getCount() {
			return _data.size();
		}
		
		@Override
		public HashMap<String, Object> getItem(int _index) {
			return _data.get(_index);
		}
		
		@Override
		public long getItemId(int _index) {
			return _index;
		}
		
		@Override
		public View getView(final int _position, View _v, ViewGroup _container) {
			LayoutInflater _inflater = getActivity().getLayoutInflater();
			View _view = _v;
			if (_view == null) {
				_view = _inflater.inflate(R.layout.group_custom_view, null);
			}
			
			final com.google.android.material.card.MaterialCardView cardview1 = _view.findViewById(R.id.cardview1);
			final LinearLayout linear1 = _view.findViewById(R.id.linear1);
			final LinearLayout linear2 = _view.findViewById(R.id.linear2);
			final LinearLayout linear85 = _view.findViewById(R.id.linear85);
			final androidx.cardview.widget.CardView cardview2 = _view.findViewById(R.id.cardview2);
			final LinearLayout linear78 = _view.findViewById(R.id.linear78);
			final TextView position_textview2 = _view.findViewById(R.id.position_textview2);
			final ImageView img_hide = _view.findViewById(R.id.img_hide);
			final ImageView iv_group_card_icon = _view.findViewById(R.id.iv_group_card_icon);
			final LinearLayout linear86 = _view.findViewById(R.id.linear86);
			final TextView type_freq_textview3 = _view.findViewById(R.id.type_freq_textview3);
			final TextView name_textview1 = _view.findViewById(R.id.name_textview1);
			final ImageView imageview1 = _view.findViewById(R.id.imageview1);
			final View view79 = _view.findViewById(R.id.view79);
			final LinearLayout linear5 = _view.findViewById(R.id.linear5);
			final com.google.android.material.progressindicator.LinearProgressIndicator current_cycle_progressbar1 = _view.findViewById(R.id.current_cycle_progressbar1);
			final LinearLayout members_progress_indicator_linear3 = _view.findViewById(R.id.members_progress_indicator_linear3);
			final LinearLayout linear4 = _view.findViewById(R.id.linear4);
			final TextView current_cycle_textview4 = _view.findViewById(R.id.current_cycle_textview4);
			final TextView current_cycle_percent_textview5 = _view.findViewById(R.id.current_cycle_percent_textview5);
			final LinearLayout linear80 = _view.findViewById(R.id.linear80);
			final LinearLayout linear81 = _view.findViewById(R.id.linear81);
			final LinearLayout linear82 = _view.findViewById(R.id.linear82);
			final TextView textview58 = _view.findViewById(R.id.textview58);
			final TextView amount_per_slot_textview6 = _view.findViewById(R.id.amount_per_slot_textview6);
			final TextView textview59 = _view.findViewById(R.id.textview59);
			final TextView number_of_members_textview7 = _view.findViewById(R.id.number_of_members_textview7);
			final TextView textview60 = _view.findViewById(R.id.textview60);
			final TextView group_trust_score_perc_textview8 = _view.findViewById(R.id.group_trust_score_perc_textview8);
			
			HashMap<String, Object> group = _data.get(_position);
			
			// Handle expandable toggle behavior cleanly via separate method
			setupCardExpansion(group, linear85, img_hide, cardview1);
			
			if (name_textview1 != null) {
				name_textview1.setText(_safeString(group, "groupName", "Untitled Group"));
			}
			
			boolean isAdmin = uid.equals(_safeString(group, "creatorId", ""));
			if (position_textview2 != null) {
				if (isAdmin) {
					position_textview2.setBackground(new GradientDrawable() {
						{
							setCornerRadius(30);
							setStroke(2, 0xFF10B881);
							setColor(getResources().getColor(R.color.darkFaint10));
						}
					});
					position_textview2.setText("ADMIN");
				} else {
					position_textview2.setBackground(new GradientDrawable() {
						{
							setCornerRadius(30);
							setStroke(2, 0xFF13456D);
							setColor(getResources().getColor(R.color.darkFaint10));
						}
					});
					position_textview2.setText("MEMBER");
				}
			}
			
			String state = _safeString(group, "state", "");
			String type = _safeString(group, "groupType", "—");
			String freq = _safeString(group, "frequency", "—");
			if (type_freq_textview3 != null) {
				type_freq_textview3.setText((state.isEmpty() ? "" : state + " · ") + type + " · " + freq);
			}
			
			long round = (long) _safeDouble(group, "currentRound");
			long slots = (long) _safeDouble(group, "totalSlots");
			if (slots <= 0) slots = 1;
			if (round <= 0) round = 1;
			int progress = (int) Math.min(100, (round * 100.0) / slots);
			
			if (current_cycle_textview4 != null) {
				current_cycle_textview4.setText("Cycle " + round + " of " + slots);
			}
			if (current_cycle_percent_textview5 != null) {
				current_cycle_percent_textview5.setText(progress + "%");
			}
			if (current_cycle_progressbar1 != null) {
				current_cycle_progressbar1.setProgress(progress);
			}
			
			if (amount_per_slot_textview6 != null) {
				amount_per_slot_textview6.setText(
				com.harvanna.wallet.wallet.BalanceFormatter.smart(_safeDouble(group, "contributionAmount")) + "/slot");
			}
			if (number_of_members_textview7 != null) {
				number_of_members_textview7.setText((long) _safeDouble(group, "membersCount") + " members");
			}
			
			double trustScore = _safeDouble(group, "trustScore");
			if (group_trust_score_perc_textview8 != null) {
				group_trust_score_perc_textview8.setText("Trust " + (long) trustScore + "%");
				if (trustScore < 50) group_trust_score_perc_textview8.setTextColor(0xFFF44336);
				else if (trustScore < 75) group_trust_score_perc_textview8.setTextColor(0xFFFF9800);
				else group_trust_score_perc_textview8.setTextColor(0xFF4CAF50);
			}
			
			return _view;
		}
	}
	
	{
	}
	
}