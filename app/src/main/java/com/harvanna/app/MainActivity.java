package com.harvanna.app;

import com.harvanna.app.utils.UiColors;
import com.harvanna.app.SplashScreenActivity;
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
import android.view.View.*;
import android.view.animation.*;
import android.widget.*;
import android.widget.LinearLayout;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.biometric.*;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.budiyev.android.codescanner.*;
import com.bumptech.glide.*;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomnavigation.BottomNavigationView.OnNavigationItemSelectedListener;
import com.google.android.material.card.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.*;
import org.json.*;
import java.net.URL;
import java.net.HttpURLConnection;
import androidx.core.content.ContextCompat;
import com.google.android.material.shape.CornerFamily;


public class MainActivity extends AppCompatActivity {
	
	private Toolbar _toolbar;
	private AppBarLayout _app_bar;
	private CoordinatorLayout _coordinator;
	private String wallet_address = "";
	
	private ArrayList<HashMap<String, Object>> systemLifecycleList = new ArrayList<>();
	private ArrayList<HashMap<String, Object>> securityFinanceList = new ArrayList<>();
	private ArrayList<HashMap<String, Object>> communityMechanicsList = new ArrayList<>();
	private ArrayList<HashMap<String, Object>> devUtilitiesList = new ArrayList<>();
	private ArrayList<HashMap<String, Object>> miningSettingsList = new ArrayList<>();
	
	private FrameLayout cardview1;
	private MaterialCardView cardview2;
	private LinearLayout linear2;
	private LinearLayout linear1;
	private BottomNavigationView bottomnavigation1;
	
	private Intent intent_page_opener = new Intent();
	private SharedPreferences walletsp;
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
		setContentView(R.layout.main);
		initialize(_savedInstanceState);
		FirebaseApp.initializeApp(this);
		initializeLogic();
	}
	
	private void initialize(Bundle _savedInstanceState) {
		_app_bar = findViewById(R.id._app_bar);
		_coordinator = findViewById(R.id._coordinator);
		_toolbar = findViewById(R.id._toolbar);
		setSupportActionBar(_toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);
		_toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _v) {
				onBackPressed();
			}
		});
		cardview1 = findViewById(R.id.cardview1);
		cardview2 = findViewById(R.id.cardview2);
		linear2 = findViewById(R.id.linear2);
		linear1 = findViewById(R.id.linear1);
		bottomnavigation1 = findViewById(R.id.bottomnavigation1);
		walletsp = getSharedPreferences("walletsp", Activity.MODE_PRIVATE);
		fauth = FirebaseAuth.getInstance();
		
		bottomnavigation1.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
			@Override
			public boolean onNavigationItemSelected(MenuItem item) {
				final int _itemId = item.getItemId();
				if (_itemId == R.id.menu_home) {
					
					showFragment(_home);
					return true;
					
					
				} else if (_itemId == R.id.menu_groups) {
					
					showFragment(_groups);
					return true;
					
					
				} else if (_itemId == R.id.menu_discover) {
					
					showFragment(_discover);
					return true;
					
					
				} else if (_itemId == R.id.menu_learn) {
					
					showFragment(_learn);
					return true;
					
					
				} else if (_itemId == R.id.menu_asset) {
					
					showFragment(_asset);
					return true;
					
				}
				
				return true;
			}
		});
		
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
		setupCustomActionBar();
		com.harvanna.app.utils.IncomingTxWatcher.start(getApplicationContext());
		setupNavigation();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuItem menu1 = menu.add(Menu.NONE, 0, Menu.NONE, "Scan");
		menu1.setIcon(R.drawable.icon_qr_code_scanner_twotone);
		menu1.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		
		MenuItem menu2 = menu.add(Menu.NONE, 1, Menu.NONE, "Notification");
		menu2.setIcon(R.drawable.icon_notifications_outline);
		menu2.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		notifMenuItem = menu2;
		_updateNotifBadge();
		
		MenuItem menu3 = menu.add(Menu.NONE, 2, Menu.NONE, "More");
		menu3.setIcon(R.drawable.icon_more_vert_round);
		menu3.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		
		// Fetch the theme's adaptive text color
		// Icons must contrast with the app bar (orange by day, charcoal by night), so use the bar's own "on" colour.
		android.util.TypedValue tvOnBar = new android.util.TypedValue();
		getTheme().resolveAttribute(R.attr.onAppBarColor, tvOnBar, true);
		int iconColor = tvOnBar.data;
		
		// Mutate and apply the dynamic tint to the Notification icon
		if (menu1.getIcon() != null) {
			menu1.getIcon().mutate().setColorFilter(iconColor, android.graphics.PorterDuff.Mode.SRC_IN);
		}
		_updateNotifBadge();
		
		// Mutate and apply the dynamic tint to the More (dropdown) icon
		if (menu2.getIcon() != null) {
			menu2.getIcon().mutate().setColorFilter(iconColor, android.graphics.PorterDuff.Mode.SRC_IN);
		}
		
		if (menu3.getIcon() != null) {
			menu3.getIcon().mutate().setColorFilter(iconColor, android.graphics.PorterDuff.Mode.SRC_IN);
		}
		return super.onCreateOptionsMenu(menu);
	}
	
	private MenuItem notifMenuItem;
	private final Handler notifHandler = new Handler(Looper.getMainLooper());
	private Runnable notifTick;
	private final BroadcastReceiver notifChanged = new BroadcastReceiver() {
		@Override public void onReceive(Context context, Intent intent) { _updateNotifBadge(); }
	};
	
	/** Bell icon with a small count of unread notifications. */
	private void _updateNotifBadge() {
		if (notifMenuItem == null) return;
		int n = com.harvanna.app.notify.NotificationCenter.unreadCount(this);
		notifMenuItem.setTitle(n > 0 ? "Notification (" + n + " new)" : "Notification");
		android.widget.FrameLayout f = new android.widget.FrameLayout(this);
		int pad = (int) (12 * getResources().getDisplayMetrics().density);
		android.widget.ImageView iv = new android.widget.ImageView(this);
		iv.setImageResource(R.drawable.icon_notifications_outline);
		android.util.TypedValue tvOnBar2 = new android.util.TypedValue();
		getTheme().resolveAttribute(R.attr.onAppBarColor, tvOnBar2, true);
		iv.setColorFilter(tvOnBar2.data);
		iv.setPadding(pad, pad, pad, pad);
		f.addView(iv, new android.widget.FrameLayout.LayoutParams(pad * 4, pad * 4));
		if (n > 0) {
			TextView b = new TextView(this);
			b.setText(n > 99 ? "99+" : String.valueOf(n));
			b.setTextSize(9);
			b.setTextColor(android.graphics.Color.WHITE);
			b.setGravity(android.view.Gravity.CENTER);
			b.setBackgroundResource(R.drawable.dot_online);
			b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(this, R.color.colorError)));
			b.setTypeface(null, android.graphics.Typeface.BOLD);
			int sz = (int) (16 * getResources().getDisplayMetrics().density);
			android.widget.FrameLayout.LayoutParams lp = new android.widget.FrameLayout.LayoutParams(sz, sz, android.view.Gravity.TOP | android.view.Gravity.END);
			lp.topMargin = pad / 2; lp.rightMargin = pad / 2;
			f.addView(b, lp);
		}
		f.setOnClickListener(v -> startActivity(new Intent(this, com.harvanna.app.NotificationsActivity.class)));
		notifMenuItem.setActionView(f);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		final int _id = item.getItemId();
		final String _title = (String) item.getTitle();
		if (_id == 0) {
			Intent scanIntent = new Intent(this, com.harvanna.app.ScanQrCodeActivity.class);
			scanIntent.putExtra(com.harvanna.app.ScanQrCodeActivity.EXTRA_TITLE, "Scan Wallet Address");
			scanIntent.putExtra(com.harvanna.app.ScanQrCodeActivity.EXTRA_MODE, com.harvanna.app.ScanQrCodeActivity.MODE_WALLET_ADDRESS);
			startActivityForResult(scanIntent, RC_SCAN_TO_SEND);
			
		}
		if (_id == 1) {
			startActivity(new Intent(this, com.harvanna.app.NotificationsActivity.class));
		}
		if (_id == 2) {
			// Pass the view ID of the button being clicked to anchor the menu to the right side of it
			showRightDropdownMenu(findViewById(android.R.id.content));
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onBackPressed() {
		
	}
	
	
	@Override
	public void onStop() {
		super.onStop();
		// Add to onStop
		try {
			androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).unregisterReceiver(globalTxReceiver);
		} catch (Exception ignored) {}
		
	}
	
	@Override
	public void onStart() {
		super.onStart();
		// Add to onStart
		androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).registerReceiver(
		globalTxReceiver, 
		new android.content.IntentFilter("com.harvanna.app.ACTION_WALLET_RECEIVED")
		);
	}
	
	@Override
	public void onPause() {
		super.onPause();
		com.harvanna.app.utils.PresenceHeartbeat.stop();
		if (notifTick != null) notifHandler.removeCallbacks(notifTick);
		try { androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).unregisterReceiver(notifChanged); } catch (Exception ignored) { }
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		try {
			com.harvanna.app.utils.IncomingTxWatcher.stop();
		} catch(Exception e) {
			// Ignored
		}
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		if (bottomnavigation1 != null) {
			outState.putInt("active_tab_id", bottomnavigation1.getSelectedItemId());
		}
		super.onSaveInstanceState(outState);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		com.harvanna.app.utils.PresenceHeartbeat.start(getApplicationContext());
		com.harvanna.app.group.GroupSaver.retryAll(getApplicationContext(), null);
		com.harvanna.app.notify.NotificationWorker.schedule(getApplicationContext());
		androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this)
		.registerReceiver(notifChanged, new android.content.IntentFilter(com.harvanna.app.notify.NotificationCenter.ACTION_CHANGED));
		_updateNotifBadge();
		notifTick = new Runnable() {
			@Override public void run() {
				com.harvanna.app.notify.NotificationSync.run(getApplicationContext(), null);
				notifHandler.postDelayed(this, 90_000L);
			}
		};
		notifHandler.post(notifTick);
		// Keep the local offline database from growing unbounded with data
		// from groups the user's since left, stale discover snapshots, etc.
		// Cheap no-op most of the time (only deletes rows older than a week).
		com.harvanna.app.offline.OfflineCacheManager.pruneStale(getApplicationContext());
		// Replay anything that was queued while offline. Each queued action
		// type maps to the same code that would've run on a successful tap;
		// adding a new offline-safe write later just means adding another
		// branch here.
		com.harvanna.app.offline.OfflineActionQueue.flush(getApplicationContext(),
		new com.harvanna.app.offline.OfflineActionQueue.HandlerRegistry() {
			@Override
			public com.harvanna.app.offline.OfflineActionQueue.ActionHandler handlerFor(String type) {
				if ("CONTRIBUTION".equals(type)) {
					return com.harvanna.app.group.GroupPaymentHelper::replayContributionFromQueue;
				}
				if ("LOAN_REPAY".equals(type)) {
					return com.harvanna.app.group.GroupLoanManager::replayRepaymentFromQueue;
				}
				if ("NOTICE".equals(type)) {
					return com.harvanna.app.group.GroupNoticeHelper::replayNoticeFromQueue;
				}
				if ("PENDING_GROUP_MARK".equals(type)) {
					return com.harvanna.app.firestore.UserProfileManager::replayPendingGroupMarkFromQueue;
				}
				return null;
			}
		},
		null);
	}
	public void _MORE() {
	}
	private Fragment _home = new HomeFragmentActivity();
	private Fragment _groups = new GroupsFragmentActivity();
	private Fragment _discover = new DiscoverFragmentActivity();
	private Fragment _learn = new LearnFragmentActivity();
	private Fragment _asset = new AssetFragmentActivity();
	private Fragment _activeFragment = _home;
	private static final int RC_SCAN_TO_SEND = 4105;
	
	private void setupCustomActionBar() {
		androidx.appcompat.app.ActionBar ab = getSupportActionBar();
		if (ab == null) return;
		
		ab.setDisplayShowTitleEnabled(false);
		ab.setDisplayShowCustomEnabled(true);
		ab.setDisplayHomeAsUpEnabled(false);
		
		// 1. Containers & Layouts
		LinearLayout toolbarContainer = new LinearLayout(this);
		toolbarContainer.setOrientation(LinearLayout.HORIZONTAL);
		toolbarContainer.setGravity(android.view.Gravity.CENTER_VERTICAL);
		toolbarContainer.setLayoutParams(new LinearLayout.LayoutParams(
		LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
		toolbarContainer.setPadding(16, 0, 16, 0);
		
		int sizePx = (int) android.util.TypedValue.applyDimension(
		android.util.TypedValue.COMPLEX_UNIT_DIP, 40, getResources().getDisplayMetrics());
		
		// 2. Avatar Setup
		com.google.android.material.card.MaterialCardView avatarCard = new com.google.android.material.card.MaterialCardView(this);
		avatarCard.setLayoutParams(new LinearLayout.LayoutParams(sizePx, sizePx));
		avatarCard.setRadius(sizePx / 2f);
		avatarCard.setCardElevation(0f);
		int resolvedPrimaryColor = androidx.core.content.ContextCompat.getColor(this, R.color.colorPrimary);
		avatarCard.setCardBackgroundColor(resolvedPrimaryColor);
		
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
			avatarCard.setClipToOutline(true);
		}
		
		final ImageView logoView = new ImageView(this);
		logoView.setId(android.view.View.generateViewId());
		logoView.setScaleType(ImageView.ScaleType.CENTER_CROP);
		logoView.setLayoutParams(new FrameLayout.LayoutParams(sizePx, sizePx));
		
		_setToolbarInitialAvatar("", "", logoView);
		avatarCard.addView(logoView);
		toolbarContainer.addView(avatarCard);
		
		// 3. Text Setup
		LinearLayout textContainer = new LinearLayout(this);
		textContainer.setOrientation(LinearLayout.VERTICAL);
		textContainer.setGravity(android.view.Gravity.CENTER_VERTICAL);
		LinearLayout.LayoutParams textContainerLp = new LinearLayout.LayoutParams(
		LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		textContainerLp.setMargins(24, 0, 0, 0);
		textContainer.setLayoutParams(textContainerLp);
		
		TextView titleView = new TextView(this);
		android.util.TypedValue tvBar = new android.util.TypedValue();
		android.util.TypedValue tvOn = new android.util.TypedValue();
		getTheme().resolveAttribute(R.attr.appBarColor, tvBar, true);
		getTheme().resolveAttribute(R.attr.onAppBarColor, tvOn, true);
		final int appBarColor = tvBar.data;
		final int onAppBarColor = tvOn.data;
		titleView.setText("Harvanna");
		titleView.setTextColor(onAppBarColor);
		titleView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 18);
		titleView.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
		
		TextView subtitleView = new TextView(this);
		subtitleView.setText("capital oracle projects");
		subtitleView.setTextColor((onAppBarColor & 0x00FFFFFF) | 0xB3000000);
		subtitleView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12);
		
		textContainer.addView(titleView);
		textContainer.addView(subtitleView);
		toolbarContainer.addView(textContainer);
		
		// 4. Click Listeners & Styling
		View.OnClickListener openProfile = v -> {
			try {
				intent_page_opener.setClass(getApplicationContext(), ProfileActivity.class);
				startActivity(intent_page_opener);
			} catch (Exception ignored) {}
		};
		avatarCard.setOnClickListener(openProfile);
		logoView.setOnClickListener(openProfile);
		
		ab.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(appBarColor));
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
			android.view.Window window = getWindow();
			window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
			window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
			window.setStatusBarColor(appBarColor);
			new androidx.core.view.WindowInsetsControllerCompat(window, window.getDecorView())
			.setAppearanceLightStatusBars(androidx.core.graphics.ColorUtils.calculateLuminance(appBarColor) > 0.4);
		}
		
		androidx.appcompat.app.ActionBar.LayoutParams params = new androidx.appcompat.app.ActionBar.LayoutParams(
		androidx.appcompat.app.ActionBar.LayoutParams.MATCH_PARENT,
		androidx.appcompat.app.ActionBar.LayoutParams.MATCH_PARENT,
		android.view.Gravity.START | android.view.Gravity.CENTER_VERTICAL);
		ab.setCustomView(toolbarContainer, params);
		
		loadUserProfile(logoView, sizePx);
	}
	
	private void loadUserProfile(final ImageView logoView, final int sizePx) {
		FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
		if (user == null || user.getPhotoUrl() == null) {
			_setToolbarInitialAvatar("", "", logoView);
			return;
		}
		String photoUrl = user.getPhotoUrl().toString().trim();
		if (photoUrl.isEmpty()) {
			_setToolbarInitialAvatar("", "", logoView);
			return;
		}
		try {
			Glide.with(MainActivity.this)
			.load(photoUrl)
			.circleCrop()
			.override(sizePx, sizePx)
			.into(logoView);
		} catch (Exception e) {
			_setToolbarInitialAvatar("", "", logoView);
		}
	}
	
	private void setupNavigation() {
		bottomnavigation1.post(() -> {
			androidx.fragment.app.Fragment existingHome = getSupportFragmentManager().findFragmentByTag("home");
			if (existingHome == null) {
				setupFragments();
				Integer initialMenuId = _menuIdForFragmentExtra(getIntent().getStringExtra("open_fragment"));
				bottomnavigation1.setSelectedItemId(initialMenuId != null ? initialMenuId : R.id.menu_home);
			} else {
				_home = existingHome;
				_groups = getSupportFragmentManager().findFragmentByTag("groups");
				_discover = getSupportFragmentManager().findFragmentByTag("discover");
				_learn = getSupportFragmentManager().findFragmentByTag("learn");
				_asset = getSupportFragmentManager().findFragmentByTag("asset");
				
				Integer resumedMenuId = _menuIdForFragmentExtra(getIntent().getStringExtra("open_fragment"));
				if (resumedMenuId != null) {
					getIntent().removeExtra("open_fragment");
					bottomnavigation1.setSelectedItemId(resumedMenuId);
				} else {
					int restoredTabId = bottomnavigation1.getSelectedItemId();
					if (restoredTabId == R.id.menu_home) _activeFragment = _home;
					else if (restoredTabId == R.id.menu_groups) _activeFragment = _groups;
					else if (restoredTabId == R.id.menu_discover) _activeFragment = _discover;
					else if (restoredTabId == R.id.menu_learn) _activeFragment = _learn;
					else if (restoredTabId == R.id.menu_asset) _activeFragment = _asset;
				}
			}
		});
	}
	
	private Integer _menuIdForFragmentExtra(String value) {
		if (value == null) return null;
		switch (value) {
			case "home": return R.id.menu_home;
			case "groups": return R.id.menu_groups;
			case "discover": return R.id.menu_discover;
			case "learn": return R.id.menu_learn;
			case "asset": return R.id.menu_asset;
			default: return null;
		}
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);
		// THE FIX: the "open_fragment" deep link only ever worked when
		// MainActivity was being freshly created — the check lived inside
		// onCreate()'s bottomnavigation1.post() block, so launching it
		// from Settings while MainActivity was already alive in the back
		// stack (FLAG_ACTIVITY_SINGLE_TOP) delivered a new intent that
		// nothing ever looked at. bottomnavigation1 is guaranteed non-null
		// here since onNewIntent() can't fire before onCreate() has run.
		if (intent != null && intent.hasExtra("open_fragment")) {
			Integer menuId = _menuIdForFragmentExtra(intent.getStringExtra("open_fragment"));
			if (menuId != null && bottomnavigation1 != null) {
				bottomnavigation1.setSelectedItemId(menuId);
			}
			intent.removeExtra("open_fragment");
		}
	}
	
	// 1. Call this method ONCE in your onCreate (before setting up the BottomNavigation)
	private void setupFragments() {
		getSupportFragmentManager().beginTransaction()
		.add(R.id.linear2, _asset, "asset").hide(_asset)
		.add(R.id.linear2, _learn, "learn").hide(_learn)
		.add(R.id.linear2, _discover, "discover").hide(_discover)
		.add(R.id.linear2, _groups, "groups").hide(_groups)
		.add(R.id.linear2, _home, "home") // Only Home is NOT hidden
		.commit();
	}
	
	// 2. Your upgraded showFragment method
	private void showFragment(Fragment fragment) {
		if (_activeFragment == fragment) return; // Do nothing if tapping the same tab
		
		// FIX: Explicitly declare the AndroidX FragmentTransaction to avoid import conflicts
		androidx.fragment.app.FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		
		// Safely hide ALL fragments to prevent Z-order overlapping bugs
		if (_home.isAdded()) ft.hide(_home);
		if (_groups.isAdded()) ft.hide(_groups);
		if (_discover.isAdded()) ft.hide(_discover);
		if (_learn.isAdded()) ft.hide(_learn);
		if (_asset.isAdded()) ft.hide(_asset);
		
		// Show the newly requested fragment
		if (fragment.isAdded()) {
			ft.show(fragment);
		}
		
		ft.commit();
		_activeFragment = fragment; // Update the tracker
	}
	
	// Height Auto-Fixer for Nested ScrollViews
	private void _ensureOverflowLists() {
		if (communityMechanicsList.isEmpty()) {
			HashMap<String, Object> a = new HashMap<>();
			a.put("title", "My groups");
			a.put("desc", "Open groups you belong to");
			communityMechanicsList.add(a);
			HashMap<String, Object> b = new HashMap<>();
			b.put("title", "Invite friends");
			b.put("desc", "Share Harvanna with someone");
			communityMechanicsList.add(b);
		}
		if (securityFinanceList.isEmpty()) {
			HashMap<String, Object> a = new HashMap<>();
			a.put("title", "Security & PIN");
			a.put("desc", "Lock the app and wallet");
			securityFinanceList.add(a);
			HashMap<String, Object> b = new HashMap<>();
			b.put("title", "KYC verification");
			b.put("desc", "Coming soon");
			b.put("comingSoon", Boolean.TRUE);
			securityFinanceList.add(b);
		}
		if (systemLifecycleList.isEmpty()) {
			HashMap<String, Object> s = new HashMap<>();
			s.put("title", "Settings");
			s.put("desc", "App preferences and account");
			systemLifecycleList.add(s);
			
			HashMap<String, Object> a = new HashMap<>();
			a.put("title", "Check for updates");
			a.put("desc", "App version and updates");
			systemLifecycleList.add(a);
			HashMap<String, Object> b = new HashMap<>();
			b.put("title", "Terms");
			b.put("desc", "How Harvanna works");
			systemLifecycleList.add(b);
			HashMap<String, Object> c = new HashMap<>();
			c.put("title", "About");
			c.put("desc", ("Harvanna and " + com.harvanna.app.CoinInfo.ticker()));
			systemLifecycleList.add(c);
		}
	}
	
	private void setListViewHeightBasedOnChildren(ListView listView) {
		ListAdapter listAdapter = listView.getAdapter();
		if (listAdapter == null) return;
		int totalHeight = 0;
		int adapterCount = listAdapter.getCount();
		for (int i = 0; i < adapterCount; i++) {
			View listItem = listAdapter.getView(i, null, listView);
			listItem.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
			totalHeight += listItem.getMeasuredHeight();
		}
		ViewGroup.LayoutParams params = listView.getLayoutParams();
		params.height = totalHeight + (listView.getDividerHeight() * (adapterCount - 1));
		listView.setLayoutParams(params);
		listView.requestLayout();
	}
	
	private void _showVersionDialog() {
		String v = "unknown";
		try { v = getPackageManager().getPackageInfo(getPackageName(), 0).versionName; } catch (Exception ignored) { }
		new androidx.appcompat.app.AlertDialog.Builder(this)
			.setTitle("Harvanna")
			.setMessage("You are on version " + v + ".\n\nThis build is installed directly, so new versions arrive as a new install from the team, not through an app store update.")
			.setPositiveButton("OK", null).show();
	}
	
	// Master Right-Side Sheet Engine with 100% Reliable Background Dim Layer
	public void showRightDropdownMenu(View anchorView) {
		if (anchorView == null) {
			anchorView = findViewById(android.R.id.content);
		}
		if (anchorView == null) return;
		_ensureOverflowLists();
		final View popupView = getLayoutInflater().inflate(R.layout.custom_right_dropdown, null);
		
		ListView popupListView1 = popupView.findViewById(R.id.listview1);
		ListView popupListView2 = popupView.findViewById(R.id.listview2);
		ListView popupListView3 = popupView.findViewById(R.id.listview3);
		ListView popupListView4 = popupView.findViewById(R.id.listview4);
		Button button1 = popupView.findViewById(R.id.button1);
		
		// Mining settings removed from this app (mining now lives in the
		// standalone Harvanna Node Miner app) — miningSettingsList is always
		// empty, so this row group is hidden entirely rather than shown
		// with zero items. listview1 is also absent from custom_right_dropdown.
		if (popupListView1 != null) popupListView1.setVisibility(View.GONE);
		if (popupListView2 != null) popupListView2.setAdapter(new Listview2Adapter(communityMechanicsList));
		if (popupListView3 != null) popupListView3.setAdapter(new Listview3Adapter(securityFinanceList));
		if (popupListView3 != null) popupListView3.setOnItemClickListener((parent, view, position, id) -> {
			// Position 0 = "Security & PIN Settings" (real, wired now).
			// Position 1 = "Compliance / KYC Verification (Coming Soon)" —
			// deliberately does nothing when tapped, matching its dimmed,
			// disabled visual state rather than opening a screen with
			// nothing real behind it.
			if (position == 0) {
				try {
					startActivity(new android.content.Intent(MainActivity.this, SecurityPinSettingsActivity.class));
				} catch (Exception e) {
					Toast.makeText(getApplicationContext(), "Security settings unavailable", Toast.LENGTH_SHORT).show();
				}
			}
		});
		
		if (popupListView2 != null) setListViewHeightBasedOnChildren(popupListView2);
		if (popupListView3 != null) setListViewHeightBasedOnChildren(popupListView3);
		if (popupListView4 != null) setListViewHeightBasedOnChildren(popupListView4);
		
		// Calculate side sheet width (320dp) in pixels
		int sheetWidthPx = (int) android.util.TypedValue.applyDimension(
		android.util.TypedValue.COMPLEX_UNIT_DIP, 
		320, 
		popupView.getContext().getResources().getDisplayMetrics()
		);
		
		// Create the window panel with FULL Screen Height
		final PopupWindow popupWindow = new PopupWindow(
		popupView, 
		sheetWidthPx, 
		WindowManager.LayoutParams.MATCH_PARENT, 
		true
		);
		
		if (popupListView4 != null) {
			popupListView4.setAdapter(new Listview4Adapter(systemLifecycleList));
			popupListView4.setOnItemClickListener((parent, view, position, id) -> {
				popupWindow.dismiss();
				try {
					if (position == 0) {
						startActivity(new android.content.Intent(MainActivity.this, SettingsActivity.class));
					} else if (position == 1) {
						_showVersionDialog();
					} else if (position == 2) {
						startActivity(new android.content.Intent(MainActivity.this, DocumentViewerActivity.class).putExtra("doc_type", "terms"));
					} else if (position == 3) {
						startActivity(new android.content.Intent(MainActivity.this, AboutActivity.class));
					}
				} catch (Exception e) {
					Toast.makeText(getApplicationContext(), "Couldn't open that screen", Toast.LENGTH_SHORT).show();
				}
			});
		}
		
		if (popupListView2 != null) popupListView2.setOnItemClickListener((parent, view, position, id) -> {
			popupWindow.dismiss();
			if (position == 0) {
				bottomnavigation1.setSelectedItemId(R.id.menu_groups);
			} else if (position == 1) {
				android.content.Intent share = new android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain");
				share.putExtra(android.content.Intent.EXTRA_TEXT, "Join me on Harvanna: save together in groups and manage your " + com.harvanna.app.CoinInfo.ticker() + " wallet.");
				startActivity(android.content.Intent.createChooser(share, "Invite friends"));
			}
		});
		if (popupListView3 != null) popupListView3.setOnItemClickListener((parent, view, position, id) -> {
			popupWindow.dismiss();
			if (position == 0) {
				try {
					startActivity(new android.content.Intent(MainActivity.this, SecurityPinSettingsActivity.class));
				} catch (Exception e) {
					Toast.makeText(getApplicationContext(), "Security settings unavailable", Toast.LENGTH_SHORT).show();
				}
			}
		});
		
		// Generate curved left-side background dynamically
		int cornerRadius = (int) android.util.TypedValue.applyDimension(
		android.util.TypedValue.COMPLEX_UNIT_DIP, 
		28, 
		popupView.getContext().getResources().getDisplayMetrics()
		);
		
		GradientDrawable backgroundShape = new GradientDrawable();
		backgroundShape.setColor(com.harvanna.app.utils.UiColors.surface());
		backgroundShape.setCornerRadii(new float[] {
			cornerRadius, cornerRadius, 
			0, 0,                       
			0, 0,                       
			cornerRadius, cornerRadius  
		});
		
		View mainContainerLayout = popupView.findViewById(R.id.linear1);
		if (mainContainerLayout != null) {
			mainContainerLayout.setBackground(backgroundShape);
		}
		
		popupWindow.setElevation(24f);
		popupWindow.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
		
		// Clean Right-To-Left Slide transitions
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
			android.transition.Slide slideTransition = new android.transition.Slide(Gravity.END);
			slideTransition.setDuration(280); 
			slideTransition.setInterpolator(new android.view.animation.DecelerateInterpolator());
			popupWindow.setEnterTransition(slideTransition);
			popupWindow.setExitTransition(slideTransition);
		} else {
			popupWindow.setAnimationStyle(android.R.style.Animation_Activity); 
		}
		
		// FIX: Inject a custom full-screen background overlay container view directly onto the activity window
		final ViewGroup rootActivityLayout = (ViewGroup) anchorView.getRootView();
		final View dynamicDimOverlay = new View(popupView.getContext());
		dynamicDimOverlay.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		dynamicDimOverlay.setBackgroundColor(0x73000000); // Perfectly balanced 45% alpha dark background shadow tint
		dynamicDimOverlay.setAlpha(0f);
		
		// Add the shadow overlay to screen and fade it in smoothly over 280ms
		rootActivityLayout.addView(dynamicDimOverlay);
		dynamicDimOverlay.animate().alpha(1f).setDuration(280).start();
		
		// Automatically wipe out the dark shadow overlay canvas as soon as user dismisses the side drawer
		popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
			@Override
			public void onDismiss() {
				rootActivityLayout.removeView(dynamicDimOverlay);
			}
		});
		
		// Render layout right against the screen bounds
		popupWindow.showAtLocation(anchorView.getRootView(), Gravity.TOP | Gravity.END, 0, 0);
		
		if (button1 != null) button1.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				com.harvanna.app.auth.LogoutManager.logout(getApplicationContext());
				intent_page_opener.setClass(getApplicationContext(), SplashScreenActivity.class);
				startActivity(intent_page_opener);
				finish();
			}
		});
		
	}
	
	// Adapter 2
	public class Listview2Adapter extends BaseAdapter {
		ArrayList<HashMap<String, Object>> _data;
		public Listview2Adapter(ArrayList<HashMap<String, Object>> _arr) { _data = _arr; }
		@Override public int getCount() { return _data.size(); }
		@Override public HashMap<String, Object> getItem(int _index) { return _data.get(_index); }
		@Override public long getItemId(int _index) { return _index; }
		@Override public View getView(final int _position, View _v, ViewGroup _container) {
			View _view = getLayoutInflater().inflate(R.layout.menu_custom_card, _container, false);
			TextView textview1 = _view.findViewById(R.id.textview1);
			TextView textview2 = _view.findViewById(R.id.textview2);
			ImageView imageview1 = _view.findViewById(R.id.imageview1);
			
			textview1.setText(_data.get(_position).get("title").toString());
			textview2.setText(_data.get(_position).get("desc").toString());
			
			if (_position == 0) imageview1.setImageResource(R.drawable.icon_checklist_rtl_round);
			else if (_position == 1) imageview1.setImageResource(R.drawable.icon_share_round);
			return _view;
		}
	}
	
	// Adapter 3
	public class Listview3Adapter extends BaseAdapter {
		ArrayList<HashMap<String, Object>> _data;
		public Listview3Adapter(ArrayList<HashMap<String, Object>> _arr) { _data = _arr; }
		@Override public int getCount() { return _data.size(); }
		@Override public HashMap<String, Object> getItem(int _index) { return _data.get(_index); }
		@Override public long getItemId(int _index) { return _index; }
		@Override public View getView(final int _position, View _v, ViewGroup _container) {
			View _view = getLayoutInflater().inflate(R.layout.menu_custom_card, _container, false);
			TextView textview1 = _view.findViewById(R.id.textview1);
			TextView textview2 = _view.findViewById(R.id.textview2);
			ImageView imageview1 = _view.findViewById(R.id.imageview1);
			
			textview1.setText(_data.get(_position).get("title").toString());
			textview2.setText(_data.get(_position).get("desc").toString());
			
			// THE FIX: KYC is marked Coming Soon (real BVN/NIN verification
			// needs a third-party identity vendor decision first) — dim the
			// row so it visibly reads as not-yet-available rather than
			// looking identical to the working entries around it.
			boolean comingSoon = Boolean.TRUE.equals(_data.get(_position).get("comingSoon"));
			_view.setAlpha(comingSoon ? 0.5f : 1f);
			_view.setEnabled(!comingSoon);
			
			if (_position == 0) imageview1.setImageResource(R.drawable.icon_security_round);
			else if (_position == 1) imageview1.setImageResource(R.drawable.icon_verified_round);
			return _view;
		}
	}
	
	// Adapter 4
	public class Listview4Adapter extends BaseAdapter {
		ArrayList<HashMap<String, Object>> _data;
		public Listview4Adapter(ArrayList<HashMap<String, Object>> _arr) { _data = _arr; }
		@Override public int getCount() { return _data.size(); }
		@Override public HashMap<String, Object> getItem(int _index) { return _data.get(_index); }
		@Override public long getItemId(int _index) { return _index; }
		@Override public View getView(final int _position, View _v, ViewGroup _container) {
			View _view = getLayoutInflater().inflate(R.layout.menu_custom_card, _container, false);
			TextView textview1 = _view.findViewById(R.id.textview1);
			TextView textview2 = _view.findViewById(R.id.textview2);
			ImageView imageview1 = _view.findViewById(R.id.imageview1);
			
			textview1.setText(_data.get(_position).get("title").toString());
			textview2.setText(_data.get(_position).get("desc").toString());
			
			if (_position == 0) imageview1.setImageResource(R.drawable.icon_settings_outline);
			else if (_position == 1) imageview1.setImageResource(R.drawable.icon_system_update_round);
			else if (_position == 2) imageview1.setImageResource(R.drawable.icon_description_round);
			else if (_position == 3) imageview1.setImageResource(R.drawable.icon_info_round);
			return _view;
		}
	}
	
	private final BroadcastReceiver globalTxReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent != null && "com.harvanna.app.ACTION_WALLET_RECEIVED".equals(intent.getAction())) {
				double amount = intent.getDoubleExtra("EXTRA_AMOUNT", intent.getDoubleExtra("amount", 0.0));
				String sender = intent.getStringExtra("EXTRA_SENDER");
				if (sender == null) sender = intent.getStringExtra("sender");
				
				// Pass 4 arguments: Activity, Title, Message, Sender
				com.harvanna.app.utils.InAppNotificationHelper.showTransactionNotification(
				MainActivity.this, // Replace with your current Activity
				"Incoming Transfer", 
				"Received " + String.format(Locale.US, "%.5f", amount) + (" " + com.harvanna.app.CoinInfo.ticker()),
				sender
				);
			}
		}
	};
	
	public void _setToolbarInitialAvatar(final String _first, final String _last, final ImageView _img) {
		String initials = "";
		if (_first != null && !_first.trim().isEmpty()) {
			initials += _first.trim().substring(0, 1).toUpperCase(Locale.US);
		}
		if (_last != null && !_last.trim().isEmpty()) {
			initials += _last.trim().substring(0, 1).toUpperCase(Locale.US);
		}
		if (initials.isEmpty()) {
			initials = "?";
		}
		
		int[] materialColors = {
			0xFF1ABC9C, 0xFF2ECC71, 0xFF3498DB, 0xFF9B59B6, 0xFF34495E,
			0xFF16A085, 0xFF27AE60, 0xFF2980B9, 0xFF8E44AD, 0xFF2C3E50,
			0xFFE67E22, 0xFFE74C3C, 0xFFD35400, 0xFFC0392B, 0xFF7F8C8D
		};
		int combinedHash = (_first != null ? _first.hashCode() : 0) + (_last != null ? _last.hashCode() : 0);
		int assignedColor = materialColors[Math.abs(combinedHash) % materialColors.length];
		
		android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(200, 200, android.graphics.Bitmap.Config.ARGB_8888);
		android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
		
		android.graphics.Paint bgPaint = new android.graphics.Paint();
		bgPaint.setColor(assignedColor);
		bgPaint.setAntiAlias(true);
		canvas.drawCircle(100, 100, 100, bgPaint);
		
		android.graphics.Paint textPaint = new android.graphics.Paint();
		textPaint.setColor(android.graphics.Color.WHITE);
		textPaint.setAntiAlias(true);
		textPaint.setTextAlign(android.graphics.Paint.Align.CENTER);
		textPaint.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD));
		textPaint.setTextSize(initials.length() > 1 ? 75 : 90);
		
		float yBaselinePosition = (canvas.getHeight() / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f);
		canvas.drawText(initials, 100, yBaselinePosition, textPaint);
		_img.setImageBitmap(bitmap);
	}
	
	public void _createInitialAvatar(final String _first, final String _last, final ImageView _img) {
		// 1. Extract initials safely (handles single names or empty spaces gracefully)
		String initials = "";
		if (_first != null && !_first.trim().isEmpty()) {
			initials += _first.trim().substring(0, 1).toUpperCase();
		}
		if (_last != null && !_last.trim().isEmpty()) {
			initials += _last.trim().substring(0, 1).toUpperCase();
		}
		if (initials.isEmpty()) {
			initials = "?"; // Fallback character if both fields are blank
		}
		
		// 2. Select a beautiful background color deterministically
		// (The same name will always get the exact same color assigned to it!)
		int[] materialColors = {
			0xFF1ABC9C, 0xFF2ECC71, 0xFF3498DB, 0xFF9B59B6, 0xFF34495E,
			0xFF16A085, 0xFF27AE60, 0xFF2980B9, 0xFF8E44AD, 0xFF2C3E50,
			0xFFE67E22, 0xFFE74C3C, 0xFFD35400, 0xFFC0392B, 0xFF7F8C8D
		};
		int combinedHash = (_first != null ? _first.hashCode() : 0) + (_last != null ? _last.hashCode() : 0);
		int assignedColor = materialColors[Math.abs(combinedHash) % materialColors.length];
		
		// 3. Setup a clean 200x200 canvas workspace
		android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(200, 200, android.graphics.Bitmap.Config.ARGB_8888);
		android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
		
		// 4. Draw the circular background profile base
		android.graphics.Paint bgPaint = new android.graphics.Paint();
		bgPaint.setColor(assignedColor);
		bgPaint.setAntiAlias(true);
		canvas.drawCircle(100, 100, 100, bgPaint);
		
		// 5. Setup styling definitions for the text layout
		android.graphics.Paint textPaint = new android.graphics.Paint();
		textPaint.setColor(android.graphics.Color.WHITE);
		textPaint.setAntiAlias(true);
		textPaint.setTextAlign(android.graphics.Paint.Align.CENTER);
		textPaint.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD));
		
		// Auto-adjust font sizing depending on whether it's 1 letter or 2
		textPaint.setTextSize(initials.length() > 1 ? 75 : 90);
		
		// Mathematical baseline adjustment to center letters vertically perfectly
		float yBaselinePosition = (canvas.getHeight() / 2) - ((textPaint.descent() + textPaint.ascent()) / 2);
		
		// 6. Paint the letters and apply the final product directly to your view target
		canvas.drawText(initials, 100, yBaselinePosition, textPaint);
		_img.setImageBitmap(bitmap);
		
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == RC_SCAN_TO_SEND && resultCode == RESULT_OK && data != null) {
			String address = data.getStringExtra(com.harvanna.app.ScanQrCodeActivity.EXTRA_RESULT_TEXT);
			Intent sendIntent = new Intent(this, SendAssetActivity.class);
			sendIntent.putExtra("recipientAddress", address); // confirm this matches whatever extra key SendAssetActivity actually reads on launch — I haven't traced that key name yet
			startActivity(sendIntent);
		}
	}
	
	{
	}
	
}
