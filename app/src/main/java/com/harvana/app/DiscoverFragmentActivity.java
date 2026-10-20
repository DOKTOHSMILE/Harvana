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
	private com.harvana.app.group.LocationFilter.Value locationFilter = new com.harvana.app.group.LocationFilter.Value();
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
	
	private FirebaseAuth fbauth;
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
		com.harvana.app.utils.Shimmer.hold(listview1, com.harvana.app.utils.Shimmer.cards(listview1.getContext(), 3), 4000, "DiscoverFragmentActivity");
		fbauth = FirebaseAuth.getInstance();
		groupsp = getContext().getSharedPreferences("groupsp", Activity.MODE_PRIVATE);
		
		tv_search.setOnClickListener(_v -> _promptSearch());
		
		// Location filter: Global until the person picks a country (then optionally a state and a city). It is saved on the phone.
		locationFilter = com.harvana.app.group.LocationFilter.load(getContext());
		if (textview3 != null) textview3.setText(locationFilter.label());
		if (lr_select_location != null) lr_select_location.setOnClickListener(_v -> {
			if (getActivity() == null) return;
			com.harvana.app.group.LocationFilter.show(getActivity(), picked -> {
				locationFilter = picked;
				if (textview3 != null) textview3.setText(picked.label());
				_applyFilterAndSort();
			});
		});
		
		cd_verified_only.setOnCheckedChangeListener((_buttonView, _isChecked) -> {
			_applyFilterAndSort();
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
		
	}
	
	private void initializeLogic() {
		_loadPublicGroups();
		ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(
		getContext(),
		R.layout.spinner_selected_compact,
		new String[]{"Most members", "On network", "Newest"});
		sortAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
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
		com.harvana.app.ui.SearchSheet.show(getActivity(), "Search groups", "Group name, state or type", searchQuery,
		new com.harvana.app.ui.SearchSheet.Listener() {
			@Override public void onQuery(String q) {
				searchQuery = q.toLowerCase(java.util.Locale.US);
				if (tv_search != null) tv_search.setText(searchQuery.isEmpty() ? "Search group" : searchQuery);
				_applyFilterAndSort();
			}
			@Override public String summary() {
				int n = displayGroups.size();
				return n == 1 ? "1 group matches" : n + " groups match";
			}
		});
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
			loaded = true;
			_applyFilterAndSort();
		}
		
		com.harvana.app.firestore.GroupFirestoreManager.getInstance(getContext())
		.queryGroups("visibility", "Public", 300, new com.harvana.app.firestore.GroupFirestoreManager.ListCallback() {
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
								if (!myGroupIds.contains(resolvedId) && !com.harvana.app.group.GroupKinds.isLegacyRotating(g)
										&& !com.harvana.app.group.GroupCloser.isClosed(g) && !com.harvana.app.group.GroupCloser.isDeleted(g)) {
									publicGroups.add(row);
								}
							}
						}
						loaded = true;
						_applyFilterAndSort();
						com.harvana.app.offline.OfflineCacheManager.saveList(getContext(), "discover_groups", publicGroups);
					});
				};
				
				if (currentUid.isEmpty()) {
					myGroupIds.clear();
					applyGroups.run();
					return;
				}
				
				// Clear stale "Pending" marks first, so the buttons only say Pending for requests that are really waiting.
				com.harvana.app.group.JoinRequestManager.reconcilePending(getContext(), currentUid, removedMarks -> {
				if (getActivity() == null) return;
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
				});
			}
			
			@Override
			public void onError(String error) {
				if (getActivity() == null) return;
				getActivity().runOnUiThread(() -> {
					// SAFETY CHECK: Ensure context is still available for the Toast
					if (getContext() == null) return;
					if (!loaded) { loaded = true; failed = true; _applyFilterAndSort(); }
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
			if (!com.harvana.app.group.LocationFilter.matches(locationFilter, g)) continue;
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
		_paintEmpty();
	}
	
	/** True once the list has an answer (cache, network or a failure); until then the shimmer shows. */
	private boolean loaded, failed;
	private LinearLayout emptyBox;
	
	/** Nothing to show: an icon, one line and a button that fits the case (retry, clear filters or create). */
	private void _paintEmpty() {
		if (cardview12 == null || getContext() == null) return;
		if (emptyBox == null) {
			final float d = getResources().getDisplayMetrics().density;
			emptyBox = new LinearLayout(getContext());
			emptyBox.setOrientation(LinearLayout.VERTICAL);
			emptyBox.setGravity(Gravity.CENTER);
			emptyBox.setPadding((int) (24 * d), (int) (24 * d), (int) (24 * d), (int) (24 * d));
			ImageView icon = new ImageView(getContext());
			icon.setTag("empty_icon");
			icon.setAdjustViewBounds(true);
			emptyBox.addView(icon, new LinearLayout.LayoutParams((int) (180 * d), (int) (135 * d)));
			TextView title = new TextView(getContext());
			title.setTag("empty_title");
			title.setTextSize(15);
			title.setTypeface(Typeface.DEFAULT_BOLD);
			title.setTextColor(com.harvana.app.utils.UiColors.textPrimary());
			title.setGravity(Gravity.CENTER);
			LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			tp.topMargin = (int) (12 * d);
			emptyBox.addView(title, tp);
			com.google.android.material.button.MaterialButton act = new com.google.android.material.button.MaterialButton(getContext());
			act.setTag("empty_button");
			act.setAllCaps(false);
			act.setOnClickListener(v -> {
				if (failed) { failed = false; _loadPublicGroups(); return; }
				if (_filtered()) {
					searchQuery = "";
					if (tv_search != null) tv_search.setText("Search group");
					if (cd_verified_only != null) cd_verified_only.setChecked(false);
					locationFilter = new com.harvana.app.group.LocationFilter.Value();
					com.harvana.app.group.LocationFilter.save(getContext(), locationFilter);
					if (textview3 != null) textview3.setText(locationFilter.label());
					_applyFilterAndSort();
					return;
				}
				com.harvana.app.group.CreateGroupSheet.show(getContext());
			});
			LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, (int) (44 * d));
			bp.topMargin = (int) (16 * d);
			emptyBox.addView(act, bp);
			cardview12.addView(emptyBox, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		}
		boolean filtered = _filtered();
		((ImageView) emptyBox.findViewWithTag("empty_icon")).setImageResource(failed || filtered ? R.drawable.illus_empty_search : R.drawable.illus_empty_groups);
		((TextView) emptyBox.findViewWithTag("empty_title")).setText(failed ? "Groups did not load" : filtered ? "No matching groups" : "No public groups");
		((TextView) emptyBox.findViewWithTag("empty_button")).setText(failed ? "Retry" : filtered ? "Clear filters" : "+ New Group");
		boolean empty = displayGroups.isEmpty() && loaded;
		emptyBox.setVisibility(empty ? View.VISIBLE : View.GONE);
		if (listview1 != null) listview1.setVisibility(empty ? View.GONE : View.VISIBLE);
	}
	
	private boolean _filtered() {
		return !searchQuery.isEmpty() || (cd_verified_only != null && cd_verified_only.isChecked())
			|| (locationFilter != null && !locationFilter.isGlobal());
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
			ImageView iv_group_photo = _view.findViewById(R.id.iv_group_photo);
			if (iv_group_photo != null) com.harvana.app.utils.GroupRing.show(iv_group_photo, _safeString(group, "groupType", ""), _safeString(group, "groupPhoto", ""), R.drawable.ic_coin_harv);
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
				String coin0 = _safeString(group, "asset", "");
				tv_slotAmount.setText(com.harvana.app.wallet.BalanceFormatter.smart(_safeDouble(group, "contributionAmount")) + " "
						+ (com.harvana.app.husd.Stable.isStable(coin0) ? com.harvana.app.husd.Stable.symbol(coin0) : com.harvana.app.CoinInfo.ticker()));
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
				btn_join.setText(isPending ? "Request pending" : "Request to join");
				btn_join.setTextColor(androidx.core.content.ContextCompat.getColor(getContext(), R.color.colorTextOnAccent));
				if (cardview5 != null) cardview5.setAlpha(isPending ? 0.6f : 1f);
				btn_join.setSingleLine(true);
				btn_join.setMaxLines(1);
				final long fMembers = membersCount, fSlots = totalSlots;
				if (imageview4 != null) imageview4.setOnClickListener(v -> {
					String nodeKey = com.harvana.app.trust.NetworkTrust.groupKey(group);
					String base = "Type: " + (tv_groupType != null ? tv_groupType.getText() : "")
						+ "\nPays: " + (tv_frequency != null ? tv_frequency.getText() : "")
						+ "\nPer slot: " + (tv_slotAmount != null ? tv_slotAmount.getText() : "")
						+ "\nSlots filled: " + fMembers + " / " + fSlots
						+ "\n\nA vote in this group passes at 51% of the vote weight, with enough people voting."
						+ "\nAsk to join and the group's admin reviews your request. You pay nothing until you are accepted and, for a circle, your bond is locked.";
					if (nodeKey.isEmpty()) {
						new com.harvana.app.ui.SheetDialog.Builder(v.getContext())
							.setTitle(_safeString(group, "groupName", "Group"))
							.setMessage(base)
							.setPositiveButton("Close", null)
							.show();
						return;
					}
					com.harvana.app.trust.NetworkTrust.group(nodeKey, score -> {
						if (getActivity() == null) return;
						String trust = score.summary == null || score.summary.isEmpty() ? "Not published yet" : score.summary;
						getActivity().runOnUiThread(() -> new com.harvana.app.ui.SheetDialog.Builder(v.getContext())
							.setTitle(_safeString(group, "groupName", "Group"))
							.setMessage("Trust: " + trust + "\n\n" + base)
							.setPositiveButton("Close", null)
							.show());
					});
				});
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
