package com.harvana.app;

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
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.*;
import androidx.cardview.widget.CardView;
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

public class DiscoverFragmentActivity extends Fragment {
	
	private FirebaseDatabase _firebase = FirebaseDatabase.getInstance();
	
	private double number = 0;
	private double length = 0;
	private String visibility = "";
	private String groupId = "";
	private String searchQuery = "";
	private final Set<String> myGroupIds = new HashSet<>();
	private final Set<String> pendingGroupIds = new HashSet<>();
	
	private ArrayList<HashMap<String, Object>> discover_group_map = new ArrayList<>();
	private ArrayList<String> publicGroupList = new ArrayList<>();
	private ArrayList<HashMap<String, Object>> publicGroupMap = new ArrayList<>();
	private ArrayList<String> privateGroupList = new ArrayList<>();
	private ArrayList<HashMap<String, Object>> privateGroupMap = new ArrayList<>();
	private ArrayList<HashMap<String, Object>> generalGroup = new ArrayList<>();
	private ArrayList<HashMap<String, Object>> publicGroups = new ArrayList<>();
	private ArrayList<HashMap<String, Object>> displayGroups = new ArrayList<>();
	
	private NestedScrollView vscroll6;
	private LinearLayout linear1;
	private TextView textview1;
	private TextView textview2;
	private MaterialCardView cardview5;
	private MaterialCardView cardview12;
	private LinearLayout linear3;
	private CardView cardview2;
	private LinearLayout linear50;
	private LinearLayout linear49;
	private ImageView imageview4;
	private TextView tv_search;
	private CardView cardview1;
	private CardView cardview4;
	private CardView cardview3;
	private LinearLayout lr_select_location;
	private ImageView imageview1;
	private TextView textview3;
	private CheckBox cd_verified_only;
	private Spinner sp_sort;
	private ListView listview1;
	
	private DatabaseReference grouodb = _firebase.getReference("groupdb");
	private ChildEventListener _grouodb_child_listener;
	private FirebaseAuth fbauth;
	private OnCompleteListener<AuthResult> _fbauth_create_user_listener;
	private OnCompleteListener<AuthResult> _fbauth_sign_in_listener;
	private OnCompleteListener<Void> _fbauth_reset_password_listener;
	private OnCompleteListener<Void> fbauth_updateEmailListener;
	private OnCompleteListener<Void> fbauth_updatePasswordListener;
	private OnCompleteListener<Void> fbauth_emailVerificationSentListener;
	private OnCompleteListener<Void> fbauth_deleteUserListener;
	private OnCompleteListener<Void> fbauth_updateProfileListener;
	private OnCompleteListener<AuthResult> fbauth_phoneAuthListener;
	private OnCompleteListener<AuthResult> fbauth_googleSignInListener;
	private SharedPreferences groupsp;
	private Intent intent = new Intent();
	
	@NonNull
	@Override
	public View onCreateView(@NonNull LayoutInflater _inflater, @Nullable ViewGroup _container, @Nullable Bundle _savedInstanceState) {
		View _view = _inflater.inflate(R.layout.discover_fragment, _container, false);
		initialize(_savedInstanceState, _view);
		FirebaseApp.initializeApp(getContext());
		initializeLogic();
		return _view;
	}
	
	private void initialize(Bundle _savedInstanceState, View _view) {
		vscroll6 = _view.findViewById(R.id.vscroll6);
		linear1 = _view.findViewById(R.id.linear1);
		textview1 = _view.findViewById(R.id.textview1);
		textview2 = _view.findViewById(R.id.textview2);
		cardview5 = _view.findViewById(R.id.cardview5);
		cardview12 = _view.findViewById(R.id.cardview12);
		linear3 = _view.findViewById(R.id.linear3);
		cardview2 = _view.findViewById(R.id.cardview2);
		linear50 = _view.findViewById(R.id.linear50);
		linear49 = _view.findViewById(R.id.linear49);
		imageview4 = _view.findViewById(R.id.imageview4);
		tv_search = _view.findViewById(R.id.tv_search);
		cardview1 = _view.findViewById(R.id.cardview1);
		cardview4 = _view.findViewById(R.id.cardview4);
		cardview3 = _view.findViewById(R.id.cardview3);
		lr_select_location = _view.findViewById(R.id.lr_select_location);
		imageview1 = _view.findViewById(R.id.imageview1);
		textview3 = _view.findViewById(R.id.textview3);
		cd_verified_only = _view.findViewById(R.id.cd_verified_only);
		sp_sort = _view.findViewById(R.id.sp_sort);
		listview1 = _view.findViewById(R.id.listview1);
		com.harvana.app.utils.Shimmer.hold(listview1, com.harvana.app.utils.Shimmer.cards(listview1.getContext(), 3), 4000);
		fbauth = FirebaseAuth.getInstance();
		groupsp = getContext().getSharedPreferences("groupsp", Activity.MODE_PRIVATE);
		
		tv_search.setOnClickListener(_v -> _promptSearch());
		
		cd_verified_only.setOnCheckedChangeListener((_buttonView, _isChecked) -> {
			if (_isChecked) _applyFilterAndSort();
		});
		
		sp_sort.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> _param1, View _param2, int _param3, long _param4) {
				final int _position = _param3;
				_applyFilterAndSort();
			}
			
			@Override
			public void onNothingSelected(AdapterView<?> _param1) {
				
			}
		});
		
		_grouodb_child_listener = new ChildEventListener() {
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
		grouodb.addChildEventListener(_grouodb_child_listener);
		
		fbauth_updateEmailListener = _param1 -> {
			final boolean _success = _param1.isSuccessful();
			final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
			
		};
		
		fbauth_updatePasswordListener = _param1 -> {
			final boolean _success = _param1.isSuccessful();
			final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
			
		};
		
		fbauth_emailVerificationSentListener = _param1 -> {
			final boolean _success = _param1.isSuccessful();
			final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
			
		};
		
		fbauth_deleteUserListener = _param1 -> {
			final boolean _success = _param1.isSuccessful();
			final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
			
		};
		
		fbauth_phoneAuthListener = task -> {
			final boolean _success = task.isSuccessful();
			final String _errorMessage = task.getException() != null ? task.getException().getMessage() : "";
			
		};
		
		fbauth_updateProfileListener = _param1 -> {
			final boolean _success = _param1.isSuccessful();
			final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
			
		};
		
		fbauth_googleSignInListener = task -> {
			final boolean _success = task.isSuccessful();
			final String _errorMessage = task.getException() != null ? task.getException().getMessage() : "";
			
		};
		
		_fbauth_create_user_listener = _param1 -> {
			final boolean _success = _param1.isSuccessful();
			final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
			
		};
		
		_fbauth_sign_in_listener = _param1 -> {
			final boolean _success = _param1.isSuccessful();
			final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
			
		};
		
		_fbauth_reset_password_listener = _param1 -> {
			final boolean _success = _param1.isSuccessful();
			
		};
	}
	
	private void initializeLogic() {
		_loadPublicGroups();
		ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(
		getContext(),
		android.R.layout.simple_spinner_dropdown_item,
		new String[]{"Most Members", "On network", "Newest"});
		sp_sort.setAdapter(sortAdapter);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		_loadPublicGroups();
	}
	public void _MORE() {
	}
	private void _promptSearch() {
		if (getActivity() == null) return;
		
		com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog = 
		new com.google.android.material.bottomsheet.BottomSheetDialog(getActivity());
		
		// Root layout for the bottom sheet configured to match parent height
		android.widget.LinearLayout layout = new android.widget.LinearLayout(getActivity());
		layout.setOrientation(android.widget.LinearLayout.VERTICAL);
		layout.setLayoutParams(new android.view.ViewGroup.LayoutParams(
		android.view.ViewGroup.LayoutParams.MATCH_PARENT, 
		android.view.ViewGroup.LayoutParams.MATCH_PARENT));
		
		int padding = (int) (16 * getResources().getDisplayMetrics().density);
		layout.setPadding(padding, padding, padding, padding);
		
		// Title
		android.widget.TextView title = new android.widget.TextView(getActivity());
		title.setText("Search groups");
		title.setTextSize(18);
		title.setTypeface(null, android.graphics.Typeface.BOLD);
		title.setPadding(0, 0, 0, padding);
		layout.addView(title);
		
		// Input EditText
		final android.widget.EditText input = new android.widget.EditText(getActivity());
		input.setHint("Group name…");
		input.setText(searchQuery);
		input.setSingleLine(true);
		layout.addView(input);
		
		// Buttons Container (Horizontal Layout)
		android.widget.LinearLayout buttonLayout = new android.widget.LinearLayout(getActivity());
		buttonLayout.setOrientation(android.widget.LinearLayout.HORIZONTAL);
		buttonLayout.setGravity(android.view.Gravity.END);
		buttonLayout.setPadding(0, padding, 0, 0);
		
		// Clear Button (Neutral)
		android.widget.Button btnClear = new android.widget.Button(getActivity(), null, android.R.attr.buttonStyleSmall);
		btnClear.setText("Clear");
		btnClear.setOnClickListener(v -> {
			searchQuery = "";
			if (tv_search != null) tv_search.setText("Search group");
			_applyFilterAndSort();
			bottomSheetDialog.dismiss();
		});
		
		// Search Button (Positive)
		android.widget.Button btnSearch = new android.widget.Button(getActivity());
		btnSearch.setText("Search");
		btnSearch.setOnClickListener(v -> {
			searchQuery = input.getText().toString().trim().toLowerCase(java.util.Locale.US);
			if (tv_search != null) {
				tv_search.setText(searchQuery.isEmpty() ? "Search group" : searchQuery);
			}
			_applyFilterAndSort();
			bottomSheetDialog.dismiss();
		});
		
		buttonLayout.addView(btnClear);
		buttonLayout.addView(btnSearch);
		layout.addView(buttonLayout);
		
		bottomSheetDialog.setContentView(layout);
		
		// Force Full-Screen State and Expand on Show
		bottomSheetDialog.setOnShowListener(dialog -> {
			com.google.android.material.bottomsheet.BottomSheetDialog d = 
			(com.google.android.material.bottomsheet.BottomSheetDialog) dialog;
			
			android.widget.FrameLayout bottomSheet = d.findViewById(
			com.google.android.material.R.id.design_bottom_sheet);
			
			if (bottomSheet != null) {
				com.google.android.material.bottomsheet.BottomSheetBehavior<android.view.View> behavior = 
				com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheet);
				
				// Force the layout height to match the full screen height
				android.view.ViewGroup.LayoutParams params = bottomSheet.getLayoutParams();
				int screenHeight = getResources().getDisplayMetrics().heightPixels;
				if (params != null) {
					params.height = screenHeight;
					bottomSheet.setLayoutParams(params);
				}
				
				// Skip the collapsed state and expand completely
				behavior.setSkipCollapsed(true);
				behavior.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
			}
			
			// Trigger keyboard focus
			input.requestFocus();
			android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) 
			getActivity().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
			if (imm != null) {
				imm.showSoftInput(input, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
			}
		});
		
		bottomSheetDialog.show();
	}
	
	private void _loadPublicGroups() {
		if (getContext() == null) return;
		
		String currentUid = FirebaseAuth.getInstance().getCurrentUser() != null
		? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
		
		List<HashMap<String, Object>> cachedGroups =
		com.harvana.app.offline.OfflineCacheManager.loadList(getContext(), "discover_groups");
		if (cachedGroups != null && !cachedGroups.isEmpty()) {
			publicGroups.clear();
			publicGroups.addAll(cachedGroups);
			_applyFilterAndSort();
		}
		
		com.harvana.app.firestore.GroupFirestoreManager.getInstance(getContext())
		.queryGroups("visibility", "Public", 50, new com.harvana.app.firestore.GroupFirestoreManager.ListCallback() {
			@Override
			public void onSuccess(List<Map<String, Object>> groups) {
				if (getActivity() == null) return;
				
				Runnable applyGroups = () -> {
					if (getActivity() == null) return;
					getActivity().runOnUiThread(() -> {
						// SAFETY CHECK: Ensure fragment is still attached
						if (getActivity() == null || getContext() == null) return;
						
						publicGroups.clear();
						if (groups != null) {
							for (Map<String, Object> g : groups) {
								HashMap<String, Object> row = new HashMap<>();
								row.putAll(g);
								Object docId = g.get("_docId");
								Object gid = g.get("groupId");
								String resolvedId = docId != null ? docId.toString() : (gid != null ? gid.toString() : "");
								row.put("groupId", resolvedId);
								if (!myGroupIds.contains(resolvedId) && !com.harvana.app.group.GroupKinds.isLegacyRotating(g)) {
									publicGroups.add(row);
								}
							}
						}
						_applyFilterAndSort();
						com.harvana.app.offline.OfflineCacheManager.saveList(getContext(), "discover_groups", publicGroups);
					});
				};
				
				if (currentUid.isEmpty()) {
					myGroupIds.clear();
					applyGroups.run();
					return;
				}
				
				com.harvana.app.firestore.UserProfileManager.getInstance(getContext()).getProfile(currentUid,
				new com.harvana.app.firestore.UserProfileManager.DocCallback() {
					@Override
					public void onSuccess(Map<String, Object> data) {
						if (getActivity() == null) return;
						myGroupIds.clear();
						Object rawGroups = data.get("groups");
						if (rawGroups instanceof List) {
							for (Object gid : (List<?>) rawGroups) {
								if (gid != null) myGroupIds.add(gid.toString());
							}
						}
						pendingGroupIds.clear();
						Object rawPending = data.get("pendingGroupIds");
						if (rawPending instanceof List) {
							for (Object gid : (List<?>) rawPending) {
								if (gid != null) pendingGroupIds.add(gid.toString());
							}
						}
						applyGroups.run();
					}
					@Override
					public void onError(String error) {
						if (getActivity() == null) return;
						myGroupIds.clear(); 
						pendingGroupIds.clear();
						applyGroups.run();
					}
				});
			}
			
			@Override
			public void onError(String error) {
				if (getActivity() == null) return;
				getActivity().runOnUiThread(() -> {
					// SAFETY CHECK: Ensure context is still available for the Toast
					if (getContext() == null) return;
					SketchwareUtil.showMessage(getContext(), "Failed to load groups: " + error);
				});
			}
		});
	}
	
	
	private void _applyFilterAndSort() {
		ArrayList<HashMap<String, Object>> filtered = new ArrayList<>();
		boolean verifiedOnly = cd_verified_only != null && cd_verified_only.isChecked();
		
		for (HashMap<String, Object> g : publicGroups) {
			if (verifiedOnly && com.harvana.app.trust.NetworkTrust.groupKey(g).isEmpty()) continue;
			if (!searchQuery.isEmpty()) {
				String name = _safeString(g, "groupName", "").toLowerCase(Locale.US);
				String state = _safeString(g, "state", "").toLowerCase(Locale.US);
				String type = _safeString(g, "groupType", "").toLowerCase(Locale.US);
				if (!name.contains(searchQuery) && !state.contains(searchQuery) && !type.contains(searchQuery)) {
					continue;
				}
			}
			filtered.add(g);
		}
		
		int sortIndex = sp_sort != null ? sp_sort.getSelectedItemPosition() : 0;
		if (sortIndex == 0) {
			java.util.Collections.sort(filtered, (a, b) -> Double.compare(_safeDouble(b, "membersCount"), _safeDouble(a, "membersCount")));
		} else if (sortIndex == 1) {
			java.util.Collections.sort(filtered, (a, b) -> {
				int an = com.harvana.app.trust.NetworkTrust.groupKey(a).isEmpty() ? 0 : 1;
				int bn = com.harvana.app.trust.NetworkTrust.groupKey(b).isEmpty() ? 0 : 1;
				if (an != bn) return Integer.compare(bn, an);
				return Double.compare(_safeDouble(b, "membersCount"), _safeDouble(a, "membersCount"));
			});
		} else if (sortIndex == 2) {
			java.util.Collections.sort(filtered, (a, b) -> Double.compare(
			_safeDouble(b, "timestamp", _safeDouble(b, "createdTimestamp")),
			_safeDouble(a, "timestamp", _safeDouble(a, "createdTimestamp"))));
		}
		
		displayGroups.clear();
		displayGroups.addAll(filtered);
		if (listview1 != null) {
			listview1.setAdapter(new Listview1Adapter(displayGroups));
		}
	}
	
	private double _safeDouble(Map<String, Object> data, String key) {
		return _safeDouble(data, key, 0);
	}
	
	private double _safeDouble(Map<String, Object> data, String key, double fallback) {
		Object v = data.get(key);
		if (v == null) return fallback;
		if (v instanceof Number) return ((Number) v).doubleValue();
		try {
			return Double.parseDouble(v.toString());
		} catch (Exception e) {
			return fallback;
		}
	}
	
	private String _safeString(Map<String, Object> data, String key, String fallback) {
		Object v = data.get(key);
		if (v == null) return fallback;
		String s = v.toString().trim();
		return s.isEmpty() || "null".equalsIgnoreCase(s) ? fallback : s;
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
				_view = _inflater.inflate(R.layout.discover_group_custom_view, null);
			}
			
			final com.google.android.material.card.MaterialCardView cardview1 = _view.findViewById(R.id.cardview1);
			final LinearLayout linear1 = _view.findViewById(R.id.linear1);
			final LinearLayout linear2 = _view.findViewById(R.id.linear2);
			final LinearLayout linear3 = _view.findViewById(R.id.linear3);
			final LinearLayout linear4 = _view.findViewById(R.id.linear4);
			final LinearLayout linear_slots_row = _view.findViewById(R.id.linear_slots_row);
			final com.google.android.material.progressindicator.LinearProgressIndicator slots_filled_progressbar = _view.findViewById(R.id.slots_filled_progressbar);
			final com.google.android.material.card.MaterialCardView cardview5 = _view.findViewById(R.id.cardview5);
			final TextView tv_groupName = _view.findViewById(R.id.tv_groupName);
			final TextView tv_status = _view.findViewById(R.id.tv_status);
			final ImageView imageview1 = _view.findViewById(R.id.imageview1);
			final TextView tv_state = _view.findViewById(R.id.tv_state);
			final LinearLayout linear8 = _view.findViewById(R.id.linear8);
			final ImageView imageview3 = _view.findViewById(R.id.imageview3);
			final TextView tv_frequency = _view.findViewById(R.id.tv_frequency);
			final ImageView imageview2 = _view.findViewById(R.id.imageview2);
			final TextView tv_groupType = _view.findViewById(R.id.tv_groupType);
			final androidx.cardview.widget.CardView cardview2 = _view.findViewById(R.id.cardview2);
			final androidx.cardview.widget.CardView cardview3 = _view.findViewById(R.id.cardview3);
			final androidx.cardview.widget.CardView cardview4 = _view.findViewById(R.id.cardview4);
			final LinearLayout linear5 = _view.findViewById(R.id.linear5);
			final TextView textview4 = _view.findViewById(R.id.textview4);
			final TextView tv_members = _view.findViewById(R.id.tv_members);
			final LinearLayout linear6 = _view.findViewById(R.id.linear6);
			final TextView textview6 = _view.findViewById(R.id.textview6);
			final TextView tv_trustscore = _view.findViewById(R.id.tv_trustscore);
			final LinearLayout linear7 = _view.findViewById(R.id.linear7);
			final TextView textview8 = _view.findViewById(R.id.textview8);
			final TextView tv_slotAmount = _view.findViewById(R.id.tv_slotAmount);
			final TextView tv_slots_label = _view.findViewById(R.id.tv_slots_label);
			final TextView tv_slots_filled = _view.findViewById(R.id.tv_slots_filled);
			final LinearLayout linear9 = _view.findViewById(R.id.linear9);
			final Button btn_join = _view.findViewById(R.id.btn_join);
			final LinearLayout linear10 = _view.findViewById(R.id.linear10);
			final ImageView imageview4 = _view.findViewById(R.id.imageview4);
			
			if (imageview2 != null) {
				imageview2.setColorFilter(0xFF10B881, PorterDuff.Mode.MULTIPLY);
			}
			
			HashMap<String, Object> group = _data.get(_position);
			if (tv_groupName != null) tv_groupName.setText(_safeString(group, "groupName", "Untitled Group"));
			if (tv_status != null) {
				tv_status.setText(_safeString(group, "status", "Active"));
			}
			String city = _safeString(group, "city", "");
			String state = _safeString(group, "state", "");
			if (tv_state != null) {
				tv_state.setText(!city.isEmpty() ? city + ", " + state : (state.isEmpty() ? "—" : state));
			}
			if (tv_frequency != null) tv_frequency.setText(_safeString(group, "frequency", "—"));
			if (tv_groupType != null) tv_groupType.setText(_safeString(group, "groupType", "—"));
			if (tv_members != null) {
				tv_members.setText(String.valueOf((long) _safeDouble(group, "membersCount")));
			}
			if (tv_slotAmount != null) {
				tv_slotAmount.setText(String.format(Locale.US, ("%.4f " + com.harvana.app.CoinInfo.ticker()), _safeDouble(group, "contributionAmount")));
			}
			
			String nodeId = com.harvana.app.trust.NetworkTrust.groupKey(group);
			if (tv_trustscore != null) {
				tv_trustscore.setTag(nodeId);
				tv_trustscore.setText(nodeId.isEmpty() ? "—" : "…");
				tv_trustscore.setTextColor(0xFF9E9E9E);
				if (!nodeId.isEmpty()) {
					final TextView cell = tv_trustscore;
					com.harvana.app.trust.NetworkTrust.group(nodeId, score -> {
						if (getActivity() == null) return;
						getActivity().runOnUiThread(() -> {
							if (!nodeId.equals(cell.getTag())) return;
							if (!score.published || score.summary.isEmpty()) {
								cell.setText("—");
								return;
							}
							String label = score.summary;
							int cut = label.indexOf(" · ");
							if (cut > 0) label = label.substring(0, cut);
							cell.setText(label);
							if (score.percent >= 0) { cell.setTextColor(com.harvana.app.trust.ProfileTrust.tierColor(score.tier)); return; }
							if (score.summary.startsWith("High trust")) cell.setTextColor(0xFF4CAF50);
							else if (score.summary.startsWith("Established")) cell.setTextColor(0xFFFF9800);
						});
					});
				}
			}
			
			// Slots-filled progress — the pre-join equivalent of the "current
			// cycle" progress shown on your own groups: how full this group
			// is, since cycle position isn't meaningful before you've joined.
			long membersCount = (long) _safeDouble(group, "membersCount");
			long totalSlots = (long) _safeDouble(group, "totalSlots");
			if (totalSlots <= 0) totalSlots = 1;
			int slotsProgress = (int) Math.min(100, (membersCount * 100.0) / totalSlots);
			if (tv_slots_filled != null) {
				tv_slots_filled.setText(membersCount + " / " + totalSlots);
			}
			if (slots_filled_progressbar != null) {
				slots_filled_progressbar.setProgress(slotsProgress);
			}
			
			final String gid = _safeString(group, "groupId", "");
			if (btn_join != null) {
				// Same fix already applied everywhere else this session:
				// disable + relabel instead of always allowing a tap, so a
				// pending request can't be sent twice.
				boolean isPending = pendingGroupIds.contains(gid);
				btn_join.setEnabled(!isPending);
				btn_join.setText(isPending ? "Pending approval" : "Join");
				btn_join.setOnClickListener(isPending ? null : v -> {
					if (gid.isEmpty()) return;
					Intent i = new Intent();
					i.setClass(getContext().getApplicationContext(), GroupJoinRequestActivity.class);
					i.putExtra("groupId", gid);
					startActivity(i);
				});
			}
			
			
			return _view;
		}
	}
}
