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
	
	
	private String uid = "";
	
	private ArrayList<HashMap<String, Object>> listMap = new ArrayList<>();
	private ArrayList<HashMap<String, Object>> myGroupMap = new ArrayList<>();
	private ArrayList<HashMap<String, Object>> generalGroup = new ArrayList<>();
	private ArrayList<HashMap<String, Object>> myGroups = new ArrayList<>();
	private final ArrayList<HashMap<String, Object>> shownGroups = new ArrayList<>();   // myGroups after the search and the type filter
	private String groupQuery = "";
	private String typeFilter = "";
	
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
	private FirebaseAuth fauth;
	
	@NonNull
	@Override
	public View onCreateView(@NonNull LayoutInflater _inflater, @Nullable ViewGroup _container, @Nullable Bundle _savedInstanceState) {
		View _view = _inflater.inflate(R.layout.groups_fragment, _container, false);
		initialize(_savedInstanceState, _view);
		FirebaseApp.initializeApp(getContext());
		initializeLogic();
		_addCircleEntry(_view);
		return _view;
	}
	
	// One create entry (the existing button) opens the type picker; circles can also be opened by ID.
	// Guarded so a layout change can never crash the tab.
	private void _addCircleEntry(View root) {
		try {
			cardview1.setOnClickListener(v -> com.harvana.app.group.CreateGroupSheet.show(getContext()));
			
			View header = root.findViewById(R.id.linear2);
			if (header == null || !(header.getParent() instanceof LinearLayout)) return;
			LinearLayout parent = (LinearLayout) header.getParent();
			if (parent.getOrientation() != LinearLayout.VERTICAL) return;
			final float d = getResources().getDisplayMetrics().density;
			
			TextView link = new TextView(getContext());
			link.setText("Have a circle ID? Open it");
			link.setTextSize(13);
			link.setTextColor(androidx.core.content.ContextCompat.getColor(getContext(), R.color.colorAccentStrong));
			link.setPadding((int) (22 * d), 0, (int) (22 * d), (int) (10 * d));
			link.setOnClickListener(v -> _promptCircleId());
			parent.addView(link, parent.indexOfChild(header) + 1);
		} catch (Exception e) {
			android.util.Log.w("GroupsFragment", "Create entry not wired", e);
		}
	}
	
	private void _openGroupSearch() {
		if (getActivity() == null) return;
		com.harvana.app.ui.SearchSheet.show(getActivity(), "Search my groups", "Name, type, city or state", groupQuery,
		new com.harvana.app.ui.SearchSheet.Listener() {
			@Override public void onQuery(String q) {
				groupQuery = q.toLowerCase(java.util.Locale.US);
				if (et_search_groups != null) et_search_groups.setText(q);
				_showGroups();
			}
			@Override public String summary() {
				int n = shownGroups.size();
				return n == 1 ? "1 group matches" : n + " groups match";
			}
		});
	}
	
	private void _openTypeFilter() {
		if (getContext() == null) return;
		final String[] labels = {"All my groups", "Rotating", "Accumulative", "Fixed contribution", "Groups I run", "Closed groups"};
		final String[] keys = {"", "rotat", "accum", "fixed", "admin", "closed"};
		new com.harvana.app.ui.SheetDialog.Builder(getContext()).setTitle("Show")
			.setItems(labels, (dlg, which) -> {
				typeFilter = keys[which];
				if (btn_filter_groups != null) btn_filter_groups.setText(typeFilter.isEmpty() ? "Filter" : labels[which]);
				_showGroups();
			}).setNegativeButton("Cancel", null).show();
	}
	
	/** Puts my groups on screen after the search words and the type filter. Every place that refreshes the list comes through here. */
	private void _showGroups() {
		shownGroups.clear();
		for (HashMap<String, Object> g : myGroups) {
			// A closed group leaves the list; it is under "Closed groups" until it is deleted for good.
			boolean closed = com.harvana.app.group.GroupCloser.isClosed(g);
			if ("closed".equals(typeFilter) != closed) continue;
			if (!typeFilter.isEmpty() && !"closed".equals(typeFilter)) {
				if ("admin".equals(typeFilter)) {
					String mine = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() == null ? ""
						: com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
					if (mine.isEmpty() || !mine.equals(String.valueOf(g.get("creatorId")))) continue;
				} else if (!String.valueOf(g.get("groupType")).toLowerCase(java.util.Locale.US).contains(typeFilter)) continue;
			}
			if (!groupQuery.isEmpty()) {
				StringBuilder hay = new StringBuilder();
				for (String k : new String[]{"groupName", "groupType", "city", "state", "country", "frequency"}) {
					Object v = g.get(k);
					if (v != null) hay.append(v).append(' ');
				}
				if (!hay.toString().toLowerCase(java.util.Locale.US).contains(groupQuery)) continue;
			}
			shownGroups.add(g);
		}
		if (listview1 != null) listview1.setAdapter(new Listview1Adapter(shownGroups));
		_paintStats();
		_paintEmpty();
	}
	
	/** The four figures at the top, from the groups on the list (closed groups left out) and the node's savings. */
	private void _paintStats() {
		int active = 0, ongoing = 0;
		long members = 0;
		for (HashMap<String, Object> g : myGroups) {
			if (com.harvana.app.group.GroupCloser.isClosed(g)) continue;
			active++;
			members += (long) _safeDouble(g, "membersCount");
			long round = (long) _safeDouble(g, "currentRound"), slots = (long) _safeDouble(g, "totalSlots");
			if (round > 0 && (slots <= 0 || round <= slots)) ongoing++;
		}
		if (tv_stat_active_groups != null) tv_stat_active_groups.setText(String.valueOf(active));
		if (tv_stat_total_members != null) tv_stat_total_members.setText(String.valueOf(members));
		if (tv_stat_ongoing_cycles != null) tv_stat_ongoing_cycles.setText(String.valueOf(ongoing));
		if (tv_stat_total_saved != null) tv_stat_total_saved.setText(savedText.isEmpty() ? "0" : savedText);
	}
	
	private String savedText = "";
	
	/** Total saved as on Home: the node's savings in each group's coin, one figure in the chosen currency. */
	private void _loadSaved() {
		if (getContext() == null || uid.isEmpty()) return;
		final Context ctx = getContext().getApplicationContext();
		String wallet = com.harvana.app.wallet.WalletManager.getActiveWalletAddress(ctx);
		com.harvana.app.sync.NodePortfolio.load(ctx, uid, wallet, p -> com.harvana.app.wallet.FxRates.load(ctx,
			() -> com.harvana.app.wallet.HarvPrice.get(q -> {
				double usd = p.savedUsd(ctx);
				String t = Double.isNaN(usd) ? p.savedInGroupCoins() : com.harvana.app.wallet.FxRates.money(ctx, usd);
				if (getActivity() == null) return;
				getActivity().runOnUiThread(() -> { savedText = t == null ? "" : t; _paintStats(); });
			})));
	}
	
	private LinearLayout emptyBox;
	
	/** No group on the list: an icon, a count line and the create button, in the list's card. */
	private void _paintEmpty() {
		if (cardview10 == null || getContext() == null) return;
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
			com.google.android.material.button.MaterialButton create = new com.google.android.material.button.MaterialButton(getContext());
			create.setTag("empty_button");
			create.setAllCaps(false);
			create.setOnClickListener(v -> {
				if (typeFilter.isEmpty() && groupQuery.isEmpty()) { com.harvana.app.group.CreateGroupSheet.show(getContext()); return; }
				typeFilter = ""; groupQuery = "";
				if (btn_filter_groups != null) btn_filter_groups.setText("Filter");
				if (et_search_groups != null) et_search_groups.setText("");
				_showGroups();
			});
			LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, (int) (44 * d));
			bp.topMargin = (int) (16 * d);
			emptyBox.addView(create, bp);
			cardview10.addView(emptyBox, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		}
		boolean filtered = !typeFilter.isEmpty() || !groupQuery.isEmpty();
		((ImageView) emptyBox.findViewWithTag("empty_icon")).setImageResource(filtered ? R.drawable.illus_empty_search : R.drawable.illus_empty_groups);
		((TextView) emptyBox.findViewWithTag("empty_title")).setText(filtered ? "No matching groups" : "No groups");
		((TextView) emptyBox.findViewWithTag("empty_button")).setText(filtered ? "Show all groups" : "+ New Group");
		boolean empty = shownGroups.isEmpty() && loaded;
		emptyBox.setVisibility(empty ? View.VISIBLE : View.GONE);
		if (listview1 != null) listview1.setVisibility(empty ? View.GONE : View.VISIBLE);
	}
	
	/** True once the list has an answer (cache or network); until then the shimmer shows, not the empty state. */
	private boolean loaded;
	
	private void _promptCircleId() {
		final EditText input = new com.google.android.material.textfield.TextInputEditText(getContext());
		input.setHint("CIRC-…");
		input.setSingleLine(true);
		new com.harvana.app.ui.SheetDialog.Builder(getContext())
				.setTitle("Open a circle")
				.setMessage("Paste the circle ID someone shared with you.")
				.setView(input)
				.setNegativeButton("Cancel", null)
				.setPositiveButton("Open", (dlg, w) -> {
					String id = input.getText().toString().trim();
					if (!(id.startsWith("CIRC-") || id.startsWith("ACCUM-") || id.startsWith("FIXED-"))) {
						Toast.makeText(getContext(), "A group ID starts with CIRC-, ACCUM- or FIXED-", Toast.LENGTH_SHORT).show();
						return;
					}
					// The group's page is found by its network id; a group nobody has saved in the app has no page to open.
					com.harvana.app.firestore.GroupFirestoreManager.getInstance(getContext()).queryGroups("nodeGroupId", id, 1,
						new com.harvana.app.firestore.GroupFirestoreManager.ListCallback() {
							@Override public void onSuccess(java.util.List<java.util.Map<String, Object>> list) {
								String docId = "";
								if (list != null && !list.isEmpty()) {
									Object d = list.get(0).get("_docId");
									docId = d == null ? "" : String.valueOf(d);
								}
								final String open = docId;
								if (getActivity() == null) return;
								getActivity().runOnUiThread(() -> {
									if (open.isEmpty()) { Toast.makeText(getContext(), "That group has no page in the app yet.", Toast.LENGTH_LONG).show(); return; }
									Intent it = new Intent(getContext(), GroupDetailsActivity.class);
									it.putExtra("groupId", open);
									startActivity(it);
								});
							}
							@Override public void onError(String error) {
								if (getActivity() != null) getActivity().runOnUiThread(() ->
									Toast.makeText(getContext(), "Couldn't look that group up. Try again.", Toast.LENGTH_LONG).show());
							}
						});
				})
				.show();
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
		com.harvana.app.utils.Shimmer.hold(listview1, com.harvana.app.utils.Shimmer.rows(listview1.getContext(), 5), 4000, "GroupsFragmentActivity");
		groupsp = getContext().getSharedPreferences("groupsp", Activity.MODE_PRIVATE);
		authsp = getContext().getSharedPreferences("authsp", Activity.MODE_PRIVATE);
		fauth = FirebaseAuth.getInstance();
		
		cardview1.setOnClickListener(_v -> com.harvana.app.group.CreateGroupSheet.show(getContext()));
		
		// Search opens the same full-screen sheet as Discover; the box on this screen only shows what was typed.
		if (et_search_groups != null) {
			et_search_groups.setFocusable(false);
			et_search_groups.setCursorVisible(false);
			et_search_groups.setOnClickListener(_v -> _openGroupSearch());
		}
		if (btn_filter_groups != null) {
			btn_filter_groups.setTextColor(com.harvana.app.utils.UiColors.textPrimary());
			btn_filter_groups.setOnClickListener(_v -> _openTypeFilter());
		}
		
		listview1.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> _param1, View _param2, int _param3, long _param4) {
				final int _position = _param3;
				if (_position < 0 || _position >= shownGroups.size()) return;
				Object gid = shownGroups.get(_position).get("groupId");
				if (gid == null || gid.toString().isEmpty()) return;
				i.putExtra("groupId", gid.toString());
				i.setClass(getContext().getApplicationContext(), GroupDetailsActivity.class);
				startActivity(i);
			}
		});
		
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
		_loadSaved();
		
	}
	public void _MORE() {
	}
	private void _loadMyGroups() {
		List<HashMap<String, Object>> cachedGroups =
		com.harvana.app.offline.OfflineCacheManager.loadList(getContext(), "my_groups_" + uid);
		if (cachedGroups != null && !cachedGroups.isEmpty() && getActivity() != null) {
			myGroups.clear();
			myGroups.addAll(cachedGroups);
			loaded = true;
			_showGroups();
		}
		
		com.harvana.app.firestore.UserProfileManager.getInstance(getContext()).getProfile(uid,
		new com.harvana.app.firestore.UserProfileManager.DocCallback() {
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
						loaded = true;
						_showGroups();
						com.harvana.app.utils.Shimmer.release(listview1);
					});
					return;
				}
				_fetchGroupDetails(groupIds);
			}
			
			@Override
			public void onError(String error) {
				// 404 = no profile doc / no groups field yet — treat as empty, not an error
				if (error != null && com.harvana.app.utils.ErrorText.is(error, "404")) {
					if (getActivity() == null) return;
					getActivity().runOnUiThread(() -> {
						myGroups.clear();
						loaded = true;
						_showGroups();
						com.harvana.app.utils.Shimmer.release(listview1);
					});
					return;
				}
				// A genuine connectivity failure leaves whatever cached list
				// was already rendered above alone instead of covering it
				// with an error toast.
				if (com.harvana.app.offline.NetworkStatus.isConnectivityError(error)
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
			com.harvana.app.firestore.GroupFirestoreManager.getInstance(getContext())
			.getGroup(groupId, new com.harvana.app.firestore.GroupFirestoreManager.DocCallback() {
				@Override
				public void onSuccess(Map<String, Object> data) {
					HashMap<String, Object> row = new HashMap<>();
					if (data != null) row.putAll(data);
					row.put("groupId", groupId);
					// Old Firestore-only rotating groups were test data: hidden, not deleted.
					if (!com.harvana.app.group.GroupKinds.isLegacyRotating(data) && !com.harvana.app.group.GroupCloser.isDeleted(data)) resultsByIndex[index] = row;
					// A closed group past its time is deleted for good by its admin's app.
					if (getContext() != null) com.harvana.app.group.GroupCloser.purgeIfDue(getContext(), groupId, data);
					_checkComplete();
				}
				
				@Override
				public void onError(String error) {
					// A group that failed to load still occupies its slot as
					// null rather than being silently dropped or shifting
					// everything after it - filtered out below, not left as
					// a gap that would itself shuffle remaining positions.
					if (com.harvana.app.offline.NetworkStatus.isConnectivityError(error)) {
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
							loaded = true;
							_showGroups();
							// Update the cache with what's now genuinely fresh,
							// so the NEXT open shows this instead of today's data.
							com.harvana.app.offline.OfflineCacheManager.saveList(getContext(), "my_groups_" + uid, myGroups);
							// Groups the network no longer has are removed here, carefully and in the background (see StaleGroupSweeper).
							com.harvana.app.group.StaleGroupSweeper.sweep(getContext(), uid, new ArrayList<>(myGroups), cleared -> {
								if (getActivity() == null) return;
								getActivity().runOnUiThread(() -> {
									for (java.util.Iterator<HashMap<String, Object>> it = myGroups.iterator(); it.hasNext(); ) { if (cleared.contains(String.valueOf(it.next().get("groupId")))) it.remove(); }
									_showGroups();
									com.harvana.app.offline.OfflineCacheManager.saveList(getContext(), "my_groups_" + uid, myGroups);
									Toast.makeText(getContext(), cleared.size() == 1 ? "Removed a group that no longer exists on the network."
										: "Removed " + cleared.size() + " groups that no longer exist on the network.", Toast.LENGTH_LONG).show();
								});
							});
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
			
			// The same card as on a profile's Groups tab (GroupCard).
			com.harvana.app.group.GroupCard.bind(getActivity(), _view, _data.get(_position), uid);
			
			return _view;
		}
	}
	
	{
	}
	
}
