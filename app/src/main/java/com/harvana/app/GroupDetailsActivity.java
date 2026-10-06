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
import android.view.View;
import android.view.View.*;
import android.view.animation.*;
import android.widget.*;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.*;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager.widget.ViewPager.OnAdapterChangeListener;
import androidx.viewpager.widget.ViewPager.OnPageChangeListener;
import com.budiyev.android.codescanner.*;
import com.bumptech.glide.*;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.button.*;
import com.google.android.material.card.*;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener;
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

public class GroupDetailsActivity extends AppCompatActivity {
	
	private FirebaseDatabase _firebase = FirebaseDatabase.getInstance();
	
	private AppBarLayout app_bar_layout;
	private MaterialCardView cardview16;
	private CollapsingToolbarLayout collapsing_toolbar;
	private LinearLayout linear_group_header;
	private LinearLayout linear7;
	private LinearLayout linear24;
	private LinearLayout linear26;
	private LinearLayout linear2;
	private ImageView iv_group_icon;
	private LinearLayout linear25;
	private TextView tv_admin_badge;
	private TextView tv_group_name;
	private TextView tv_group_meta;
	private MaterialCardView cardview13;
	private MaterialCardView cardview14;
	private MaterialCardView cardview15;
	private LinearLayout linear27;
	private ImageView imageview11;
	private LinearLayout linear30;
	private TextView textview10;
	private TextView tv_header_amount;
	private LinearLayout linear28;
	private ImageView imageview12;
	private LinearLayout linear31;
	private TextView textview11;
	private TextView tv_header_members;
	private LinearLayout linear29;
	private ImageView imageview13;
	private LinearLayout linear32;
	private TextView textview12;
	private TextView tv_header_next;
	private MaterialButton btn_footer_request_payout;
	private MaterialButton btn_footer_apply_loan;
	private ImageView imageview1;
	private View view8;
	private ImageView btn_group_overflow;
	private ImageView btn_group_chats;
	private LinearLayout linear9;
	private TabLayout tab_layout;
	private ViewPager view_pager;
	
	private FragmentAdapterComponentFragmentAdapter fragmentAdapterComponent;
	private SharedPreferences authsp;
	private SharedPreferences groupsp;
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
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.group_details);
		initialize(_savedInstanceState);
		FirebaseApp.initializeApp(this);
		initializeLogic();
	}
	
	private void initialize(Bundle _savedInstanceState) {
		app_bar_layout = findViewById(R.id.app_bar_layout);
		cardview16 = findViewById(R.id.cardview16);
		collapsing_toolbar = findViewById(R.id.collapsing_toolbar);
		linear_group_header = findViewById(R.id.linear_group_header);
		linear7 = findViewById(R.id.linear7);
		linear24 = findViewById(R.id.linear24);
		linear26 = findViewById(R.id.linear26);
		linear2 = findViewById(R.id.linear2);
		iv_group_icon = findViewById(R.id.iv_group_icon);
		linear25 = findViewById(R.id.linear25);
		tv_admin_badge = findViewById(R.id.tv_admin_badge);
		tv_group_name = findViewById(R.id.tv_group_name);
		tv_group_meta = findViewById(R.id.tv_group_meta);
		cardview13 = findViewById(R.id.cardview13);
		cardview14 = findViewById(R.id.cardview14);
		cardview15 = findViewById(R.id.cardview15);
		linear27 = findViewById(R.id.linear27);
		imageview11 = findViewById(R.id.imageview11);
		linear30 = findViewById(R.id.linear30);
		textview10 = findViewById(R.id.textview10);
		tv_header_amount = findViewById(R.id.tv_header_amount);
		linear28 = findViewById(R.id.linear28);
		imageview12 = findViewById(R.id.imageview12);
		linear31 = findViewById(R.id.linear31);
		textview11 = findViewById(R.id.textview11);
		tv_header_members = findViewById(R.id.tv_header_members);
		linear29 = findViewById(R.id.linear29);
		imageview13 = findViewById(R.id.imageview13);
		linear32 = findViewById(R.id.linear32);
		textview12 = findViewById(R.id.textview12);
		tv_header_next = findViewById(R.id.tv_header_next);
		btn_footer_request_payout = findViewById(R.id.btn_footer_request_payout);
		btn_footer_apply_loan = findViewById(R.id.btn_footer_apply_loan);
		imageview1 = findViewById(R.id.imageview1);
		view8 = findViewById(R.id.view8);
		btn_group_overflow = findViewById(R.id.btn_group_overflow);
		btn_group_chats = findViewById(R.id.btn_group_chats);
		linear9 = findViewById(R.id.linear9);
		tab_layout = findViewById(R.id.tab_layout);
		view_pager = findViewById(R.id.view_pager);
		fragmentAdapterComponent = new FragmentAdapterComponentFragmentAdapter(getApplicationContext(), getSupportFragmentManager());
		authsp = getSharedPreferences("authsp", Activity.MODE_PRIVATE);
		groupsp = getSharedPreferences("groupsp", Activity.MODE_PRIVATE);
		fauth = FirebaseAuth.getInstance();
		
		imageview1.setOnClickListener(_v -> finish());
		
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
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
			final Window window = this.getWindow();
			window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
			window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
			com.harvana.app.utils.StatusBarUtil.matchToolbar(this);
		}
		
		groupId = getIntent().getStringExtra("groupId");
		if (groupId == null) groupId = "";
		
		_setupTabs();
		_wireGroupLogic();
		int startTab = getIntent().getIntExtra("tab", -1);
		if (startTab >= 0 && view_pager != null) view_pager.setCurrentItem(startTab);
		if (btn_group_overflow != null) {
			btn_group_overflow.setOnClickListener(v -> {
				Intent i = new Intent(getApplicationContext(), GroupSettingsActivity.class);
				i.putExtra("groupId", groupId);
				startActivity(i);
			});
		}
		if (btn_group_chats != null) {
			btn_group_chats.setOnClickListener(v ->
				com.harvana.app.group.GroupChatSheet.show(this, groupId));
		}
		
	}
	
	public class FragmentAdapterComponentFragmentAdapter extends FragmentStatePagerAdapter {
		// This class is deprecated, you should migrate to ViewPager2:
		// https://developer.android.com/reference/androidx/viewpager2/widget/ViewPager2
		Context context;
		int tabCount;
		
		public FragmentAdapterComponentFragmentAdapter(Context context, FragmentManager manager) {
			super(manager);
			this.context = context;
		}
		
		public void setTabCount(int tabCount) {
			this.tabCount = tabCount;
			pages.clear();
			for (int i = 0; i < tabCount; i++) pages.add(i);
		}
		
		final java.util.List<Integer> pages = new java.util.ArrayList<>();
		
		/** Groups created with loans switched off don't get a Loans tab at all. */
		void setLoansEnabled(boolean enabled) {
			boolean has = pages.contains(3);
			if (enabled == has) return;
			if (enabled) pages.add(3, 3); else pages.remove(Integer.valueOf(3));
			notifyDataSetChanged();
		}
		
		int positionOfPage(int page) { return pages.indexOf(page); }
		
		@Override
		public int getItemPosition(Object object) { return POSITION_NONE; }
		
		@Override
		public int getCount() {
			return pages.size();
		}
		
		@Override
		public CharSequence getPageTitle(int _index) {
			int _position = pages.get(_index);
			if (_position == 0) return "Overview";
			if (_position == 1) return "Members";
			if (_position == 2) return "Payments";
			if (_position == 3) return "Loans";
			if (_position == 4) return "Notice";
			if (_position == 5) return "History";
			return "";
		}
		
		
		@Override
		public Fragment getItem(int _index) {
			int _position = pages.get(_index);
			Bundle args = new Bundle();
			args.putString("groupId", groupId);
			
			Fragment fragment;
			if (_position == 0) {
				fragment = new GroupOverviewFragmentActivity();
			} else if (_position == 1) {
				fragment = new GroupMembersFragmentActivity();
			} else if (_position == 2) {
				fragment = new GroupPaymentFragmentActivity();
			} else if (_position == 3) {
				fragment = new GroupLoansFragmentActivity();
			} else if (_position == 4) {
				fragment = new GroupNoticeFragmentActivity();
			} else if (_position == 5) {
				fragment = new HistoryFragmentActivity();
			} else {
				fragment = new Fragment();
			}
			fragment.setArguments(args);
			return fragment;
		}
		
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuItem menu1 = menu.add(Menu.NONE, 0, Menu.NONE, "Chat");
		menu1.setIcon(R.drawable.icon_mark_unread_chat_alt_round);
		menu1.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		MenuItem menu2 = menu.add(Menu.NONE, 1, Menu.NONE, "Options");
		menu2.setIcon(R.drawable.icon_more_vert_round);
		menu2.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		final int _id = item.getItemId();
		final String _title = (String) item.getTitle();
		
		if (_id == 0) {
			String uid = FirebaseAuth.getInstance().getCurrentUser() != null
			? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
			com.harvana.app.group.GroupChatConnector.open(this, groupId, !uid.isEmpty() && uid.equals(groupCreatorId));
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onBackPressed() {
		finish();
	}
	
	@Override
	public void onStart() {
		super.onStart();
		androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this)
		.registerReceiver(globalTxReceiver,
		new android.content.IntentFilter("com.harvana.app.ACTION_WALLET_RECEIVED"));
	}
	
	@Override
	public void onStop() {
		super.onStop();
		// Add to onStop
		try {
			androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).unregisterReceiver(globalTxReceiver);
		} catch (Exception ignored) {}
		
	}
	public void _MORE() {
	}
	private HashMap<String, Object> user_v_map = new HashMap<>();
	private String groupId = "";
	private double number = 0;
	private double length = 0;
	private String fName = "";
	private String lName = "";
	private boolean isCurrentUserAdmin = false;
	private String groupCreatorId = "";
	private String groupWalletAddress = "";
	private com.harvana.app.group.GroupDetailsLogic groupLogic;
	private int myResolvedSlot = 0;
	
	private ArrayList<HashMap<String, Object>> group_list_map = new ArrayList<>();
	
	private final BroadcastReceiver globalTxReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent != null && "com.harvana.app.ACTION_WALLET_RECEIVED".equals(intent.getAction())) {
				double amount = intent.getDoubleExtra("amount", 0.0);
				String sender = intent.getStringExtra("sender");
				
				com.harvana.app.utils.InAppNotificationHelper.showTransactionNotification(
				GroupDetailsActivity.this,
				"Incoming Transfer", 
				"Received " + String.format(Locale.US, "%.5f", amount) + (" " + com.harvana.app.CoinInfo.ticker()),
				sender
				);
			}
		}
	};
	
	private void _setupTabs() {
		FragmentAdapterComponentFragmentAdapter adapter =
		new FragmentAdapterComponentFragmentAdapter(this, getSupportFragmentManager());
		adapter.setTabCount(6);
		pagerAdapter = adapter;
		view_pager.setAdapter(adapter);
		tab_layout.setupWithViewPager(view_pager);
		tab_layout.setTabMode(TabLayout.MODE_SCROLLABLE);
		tab_layout.setTabTextColors(0xFF9AA3B2, getResources().getColor(R.color.colorAccent));
		tab_layout.setSelectedTabIndicatorColor(getResources().getColor(R.color.colorAccent));
		view_pager.setCurrentItem(0);
	}
	
	private void _wireGroupLogic() {
		groupLogic = new com.harvana.app.group.GroupDetailsLogic(this,
		new com.harvana.app.group.GroupDetailsLogic.UiBridge() {
			@Override
			public void runOnUi(Runnable r) {
				runOnUiThread(r);
			}
			
			@Override
			public void showMessage(String msg) {
				SketchwareUtil.showMessage(getApplicationContext(), msg);
			}
			
			@Override
			public void openLoansTab() {
				if (view_pager == null) return;
				int idx = pagerAdapter != null ? pagerAdapter.positionOfPage(3) : 3;
				if (idx >= 0) view_pager.setCurrentItem(idx);
				else SketchwareUtil.showMessage(getApplicationContext(), "Loans are turned off for this group.");
			}
			
			@Override
			public void showAdminTransferSheet() {
				_showAdminTransferSheet();
			}
			
			@Override
			public void transferFromGroupWallet(String from, String to, double amountHarv,
			com.harvana.app.group.GroupPayoutManager.Callback cb) {
				_executeGroupWalletTransfer(from, to, amountHarv, cb);
			}
			
			@Override
			public String getMemberWalletAddress() {
				SharedPreferences prefs = getSharedPreferences("wallet", MODE_PRIVATE);
				String addr = prefs.getString("wallet_address", "").trim();
				if (addr.isEmpty()) {
					prefs = getSharedPreferences("harv_wallet_prefs", MODE_PRIVATE);
					addr = prefs.getString("active_wallet_address", "").trim();
				}
				return addr;
			}
			
			@Override
			public int getMySlot() {
				return myResolvedSlot;
			}
		});
		
		groupLogic.setGroupId(groupId);
		
		if (btn_footer_request_payout != null) {
			btn_footer_request_payout.setVisibility(View.GONE);
			btn_footer_request_payout.setEnabled(false);
		}
		if (btn_footer_apply_loan != null) {
			btn_footer_apply_loan.setVisibility(View.GONE);
			btn_footer_apply_loan.setEnabled(false);
		}
		
		if (btn_footer_request_payout != null && btn_footer_apply_loan != null) {
			groupLogic.loadGroup(
			btn_footer_request_payout,
			btn_footer_apply_loan,
			(data, admin) -> {
				isCurrentUserAdmin = admin;
				if (btn_footer_request_payout != null) {
					btn_footer_request_payout.setVisibility(View.VISIBLE);
					btn_footer_request_payout.setEnabled(true);
				}
				if (btn_footer_apply_loan != null) {
					// A group on the network has no Firestore loan or payout buttons: the footer opens its network actions.
					btn_footer_apply_loan.setVisibility(groupLogic.isOnChain() ? View.GONE : View.VISIBLE);
					btn_footer_apply_loan.setEnabled(true);
				}
				groupCreatorId = _safeString(data, "creatorId", "");
				groupWalletAddress = groupLogic.getGroupWalletAddress();
				groupsp.edit().putBoolean("isAdmin", admin).apply();
				_bindHeader(data, admin);
				_resolveMySlot();
			});
		}
	}
	
	private void _resolveMySlot() {
		String uid = FirebaseAuth.getInstance().getCurrentUser() != null
		? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
		if (groupId.isEmpty() || uid.isEmpty()) return;
		
		com.harvana.app.firestore.GroupFirestoreManager.getInstance(getApplicationContext())
		.getMember(groupId, uid, new com.harvana.app.firestore.GroupFirestoreManager.DocCallback() {
			@Override
			public void onSuccess(Map<String, Object> data) {
				int slot = data != null ? (int) _asDouble(data.get("slot")) : 0;
				runOnUiThread(() -> myResolvedSlot = slot);
				
				String status = data != null ? _safeString(data, "status", "") : "";
				if ("ACTIVE".equalsIgnoreCase(status) || status.isEmpty()) {
					com.harvana.app.firestore.UserProfileManager.getInstance(getApplicationContext())
					.addGroupToProfile(uid, groupId, new com.harvana.app.firestore.UserProfileManager.SimpleCallback() {
						@Override public void onSuccess() {}
						@Override public void onError(String err) {
							android.util.Log.w("GroupDetails", "Self-heal profile groups failed: " + err);
						}
					});
				} else if (com.harvana.app.group.JoinRequestManager.isInactiveMemberStatus(status)) {
					com.harvana.app.firestore.UserProfileManager.getInstance(getApplicationContext())
					.removeGroupFromProfile(uid, groupId, new com.harvana.app.firestore.UserProfileManager.SimpleCallback() {
						@Override public void onSuccess() {}
						@Override public void onError(String err) {}
					});
					runOnUiThread(() -> {
						SketchwareUtil.showMessage(getApplicationContext(),
						"You're no longer a member of this group.");
						Intent home = new Intent(GroupDetailsActivity.this, MainActivity.class);
						home.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
						startActivity(home);
						finish();
					});
				}
			}
			
			@Override
			public void onError(String error) {
			}
		});
	}
	
	private String _extractLocation(Map<String, Object> data) {
		String city = _safeString(data, "city", "");
		String state = _safeString(data, "state", "");
		String country = _safeString(data, "country", "");
		String loc = "";
		
		if (!city.isEmpty()) loc += city;
		if (!state.isEmpty()) loc += (loc.isEmpty() ? "" : ", ") + state;
		if (!country.isEmpty()) loc += (loc.isEmpty() ? "" : ", ") + country;
		
		if (loc.isEmpty()) {
			loc = _safeString(data, "location", _safeString(data, "groupLocation", ""));
		}
		return loc;
	}
	
	private FragmentAdapterComponentFragmentAdapter pagerAdapter;
	
	/** Five tabs (a group without loans) share the full width; six tabs scroll sideways as before. */
	private void _fitTabs() {
		if (tab_layout == null || pagerAdapter == null) return;
		boolean fill = pagerAdapter.getCount() <= 5;
		tab_layout.setTabMode(fill ? TabLayout.MODE_FIXED : TabLayout.MODE_SCROLLABLE);
		tab_layout.setTabGravity(fill ? TabLayout.GRAVITY_FILL : TabLayout.GRAVITY_START);
	}
	
	private void _bindHeader(Map<String, Object> data, boolean admin) {
		if (data == null) return;
		if (pagerAdapter != null) {
			Object al = data.get("allowLoans");
			pagerAdapter.setLoansEnabled(!(al instanceof Boolean) || (Boolean) al);
			_fitTabs();
		}
		
		String name = _safeString(data, "groupName", "Group");
		String type = _safeString(data, "groupType", "");
		String freq = _safeString(data, "frequency", "—");
		String vis = _safeString(data, "visibility", "");
		String slots = data.get("totalSlots") != null
		? String.valueOf((long) _asDouble(data.get("totalSlots"))) : "0";
		double amount = _asDouble(data.get("contributionAmount"));
		long members = data.get("membersCount") != null
		? (long) _asDouble(data.get("membersCount")) : 0;
		String round = data.get("currentRound") != null
		? String.valueOf((long) _asDouble(data.get("currentRound"))) : "—";
		
		String location = _extractLocation(data);
		
		if (tv_group_name != null) tv_group_name.setText(name);
		if (iv_group_icon != null) {
			String photo = _safeString(data, "groupPhoto", "");
			com.harvana.app.utils.GroupRing.apply(iv_group_icon, type, 1f, 0x00000000);      // ring colour = group type
			iv_group_icon.setImageTintList(null);
			iv_group_icon.clearColorFilter();
			if (!photo.startsWith("http")) { int p = (int) (10 * getResources().getDisplayMetrics().density); iv_group_icon.setPadding(p, p, p, p); iv_group_icon.setImageResource(R.drawable.ic_coin_harv); }
			if (photo.startsWith("http")) {
				iv_group_icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
				try { com.bumptech.glide.Glide.with(this).load(photo).circleCrop().into(iv_group_icon); } catch (Exception ignored) { }
				iv_group_icon.setOnClickListener(v -> com.harvana.app.utils.ImageViewerActivity.open(this, photo));
			}
		}
		if (tv_group_meta != null) {
			String metaText = freq + " · " + slots + " slots · " + vis;
			if (!location.isEmpty() && !"Unknown".equalsIgnoreCase(location)) {
				metaText += " · " + location;
			}
			tv_group_meta.setText(metaText);
		}
		if (tv_header_amount != null) {
			com.harvana.app.utils.Shimmer.loading(tv_header_amount);
		}
		_loadTotalGroupContributions();
		
		if (tv_header_members != null) {
			tv_header_members.setText(members + " / " + slots);
		}
		if (tv_header_next != null) {
			tv_header_next.setText("Round " + round);
		}
		if (tv_admin_badge != null) {
			tv_admin_badge.setVisibility(admin ? View.VISIBLE : View.GONE);
		}
	}
	
	private void _executeGroupWalletTransfer(String from, String to, double amountHarv,
	com.harvana.app.group.GroupPayoutManager.Callback cb) {
		java.util.ArrayList<String[]> rows = new java.util.ArrayList<>();
		rows.add(new String[]{"From", from});
		rows.add(new String[]{"To", to});
		rows.add(new String[]{"For", "A payout from the group wallet"});
		rows.add(new String[]{"Network fee", "Taken from the group wallet"});
		com.harvana.app.group.PaymentConfirmSheet.reviewThenAuthorize(GroupDetailsActivity.this,
		"Send from the group wallet", com.harvana.app.wallet.BalanceFormatter.smart(amountHarv), rows, () -> {
			try {
				java.math.BigDecimal amount = java.math.BigDecimal.valueOf(amountHarv);
				com.harvana.app.tx.TransactionRouter.routeTransaction(
				GroupDetailsActivity.this,
				from,
				to,
				amount,
				null,
				new com.harvana.app.tx.TransactionRouter.RouterCallback() {
					@Override
					public void onSuccess(String txHash) {
						runOnUiThread(() -> cb.onSuccess(txHash != null ? txHash : "ok"));
					}
					
					@Override
					public void onError(String errorCode, String errorMessage) {
						runOnUiThread(() -> cb.onError(errorMessage != null ? errorMessage : errorCode));
					}
				});
			} catch (Exception e) {
				cb.onError(e.getMessage());
			}
		});
	}
	
	private static final int GROUP_SEND_REQUEST_CODE = 2101;
	
	public void _showAdminTransferSheet() {
		if (groupWalletAddress == null || groupWalletAddress.isEmpty()) {
			SketchwareUtil.showMessage(getApplicationContext(), "Group wallet address not loaded yet.");
			return;
		}
		Intent intent = new Intent(GroupDetailsActivity.this, SendAssetActivity.class);
		intent.putExtra("fromWalletAddress", groupWalletAddress);
		startActivityForResult(intent, GROUP_SEND_REQUEST_CODE);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == GROUP_SEND_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
			SketchwareUtil.showMessage(getApplicationContext(), "Transfer submitted from group wallet!");
			if (groupLogic != null) groupLogic.refreshGroupWalletBalance(tv_header_amount);
			_loadTotalGroupContributions();
		}
	}
	
	private String _safeString(Map<String, Object> data, String key, String fallback) {
		Object v = data.get(key);
		return v != null ? v.toString() : fallback;
	}
	
	private double _asDouble(Object value) {
		if (value instanceof Number) return ((Number) value).doubleValue();
		try {
			return Double.parseDouble(value.toString());
		} catch (Exception e) {
			return 0;
		}
	}
	
	private void _loadTotalGroupContributions() {
		if (groupId == null || groupId.isEmpty()) return;
		
		// A fixed goal lives on the network: deposits are on the chain, not in the Firestore payments list below, so that list
		// reads 0 for it. Collected is what the network says was raised, whether or not it has been paid out yet.
		if ("FIXED".equals(com.harvana.app.sync.NodeGroupClient.familyOf(groupId))) {
			com.harvana.app.sync.NodeGroupClient.getFixedGroup(groupId, new com.harvana.app.sync.NodeGroupClient.Callback() {
				@Override public void onSuccess(org.json.JSONObject json) {
					final double raised = json.optLong("raisedAmountRaw", 0L) / 100_000_000.0;
					runOnUiThread(() -> { if (tv_header_amount != null) tv_header_amount.setText(com.harvana.app.wallet.BalanceFormatter.smart(raised)); });
				}
				@Override public void onError(String error) { /* keep what is shown */ }
			});
			return;
		}
		
		Map<String, Object> cachedTotal = com.harvana.app.offline.OfflineCacheManager.load(
		getApplicationContext(), "group_total_contributions_" + groupId);
		if (cachedTotal != null && cachedTotal.get("total") instanceof Number) {
			double cachedGrandTotal = ((Number) cachedTotal.get("total")).doubleValue();
			if (tv_header_amount != null) {
				tv_header_amount.setText(com.harvana.app.wallet.BalanceFormatter.smart(cachedGrandTotal));
			}
		}
		
		com.harvana.app.firestore.GroupFirestoreManager.getInstance(getApplicationContext())
		.getPayments(groupId, new com.harvana.app.firestore.GroupFirestoreManager.ListCallback() {
			@Override
			public void onSuccess(List<Map<String, Object>> items) {
				double total = 0;
				if (items != null) {
					for (Map<String, Object> pay : items) {
						String st = _safeString(pay, "status", "CONFIRMED");
						boolean ok = "CONFIRMED".equalsIgnoreCase(st)
						|| "PAID".equalsIgnoreCase(st);
						if (!ok) continue;
						
						String type = _safeString(pay, "type", "").toUpperCase(Locale.US);
						if (type.contains("LOAN") || type.contains("PAYOUT")
						|| type.contains("REJECT") || type.contains("FEE")
						|| type.contains("DISBURSE") || type.contains("MONTH_CLOSE")
						|| type.contains("ADMIN")) {
							continue;
						}
						boolean contribution = type.isEmpty()
						|| type.contains("CONTRIBUTION")
						|| type.contains("FIXED")
						|| "PAYMENT".equals(type);
						if (!contribution) continue;
						
						double amt = _asDouble(pay.get("amountHarv"));
						if (amt <= 0) amt = _asDouble(pay.get("amount"));
						if (amt > 0) total += amt;
					}
				}
				final double grandTotal = total;
				Map<String, Object> toCache = new HashMap<>();
				toCache.put("total", grandTotal);
				com.harvana.app.offline.OfflineCacheManager.save(
				getApplicationContext(), "group_total_contributions_" + groupId, toCache);
				runOnUiThread(() -> {
					if (tv_header_amount != null) {
						tv_header_amount.setText(
						com.harvana.app.wallet.BalanceFormatter.smart(grandTotal));
					}
				});
			}
			@Override
			public void onError(String error) {
				if (com.harvana.app.offline.NetworkStatus.isConnectivityError(error)
				&& cachedTotal != null) {
					return;
				}
				runOnUiThread(() -> {
					if (tv_header_amount != null) tv_header_amount.setText("—");
				});
			}
		});
	}
	
	{
	}
	
}
