package com.harvana.app;

import com.harvana.app.utils.UiColors;
import com.harvana.app.SplashScreenActivity;
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
		if (_savedInstanceState == null) _walletOnboarding();
	}

	/** Right after a sign-in or a new account (logout clears every pref and wallet): create or restore a wallet first. */
	private void _walletOnboarding() {
		if (com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() == null) return;
		String addr = com.harvana.app.wallet.WalletManager.getActiveWalletAddress(this);
		if (addr != null && !addr.isEmpty()) return;
		android.content.SharedPreferences sp = getSharedPreferences("onboarding", MODE_PRIVATE);
		if (sp.getBoolean("wallet_prompted", false)) return;
		sp.edit().putBoolean("wallet_prompted", true).apply();
		startActivity(new Intent(this, WalletListActivity.class));
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
		com.harvana.app.utils.IncomingTxWatcher.start(getApplicationContext());
		setupNavigation();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuItem menu1 = menu.add(Menu.NONE, 0, Menu.NONE, "Scan");
		menu1.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		menu1.setActionView(_toolbarIcon(R.drawable.icon_qr_code_scanner_twotone, v -> {
			Intent scanIntent = new Intent(this, com.harvana.app.ScanQrCodeActivity.class);
			scanIntent.putExtra(com.harvana.app.ScanQrCodeActivity.EXTRA_TITLE, "Scan a Harvana code");
			scanIntent.putExtra(com.harvana.app.ScanQrCodeActivity.EXTRA_MODE, com.harvana.app.ScanQrCodeActivity.MODE_ANY);
			startActivityForResult(scanIntent, RC_SCAN_TO_SEND);
		}));

		MenuItem menu2 = menu.add(Menu.NONE, 1, Menu.NONE, "Notification");
		menu2.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		notifMenuItem = menu2;
		_updateNotifBadge();

		MenuItem menu3 = menu.add(Menu.NONE, 2, Menu.NONE, "More");
		menu3.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		menu3.setActionView(_toolbarIcon(R.drawable.icon_more_vert_round, v -> showRightDropdownMenu(v)));
		return super.onCreateOptionsMenu(menu);
	}

	/** Scan, bell and menu share one 24dp glyph inside a 48dp hit target. */
	private FrameLayout _toolbarIcon(int icon, View.OnClickListener click) {
		float d = getResources().getDisplayMetrics().density;
		int box = (int) (48 * d);
		int glyph = (int) (24 * d);
		FrameLayout f = new FrameLayout(this);
		ImageView iv = new ImageView(this);
		iv.setImageResource(icon);
		iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
		android.util.TypedValue tvOnBar = new android.util.TypedValue();
		getTheme().resolveAttribute(R.attr.onAppBarColor, tvOnBar, true);
		iv.setColorFilter(tvOnBar.data, android.graphics.PorterDuff.Mode.SRC_IN);
		f.addView(iv, new FrameLayout.LayoutParams(glyph, glyph, android.view.Gravity.CENTER));
		f.setLayoutParams(new ViewGroup.LayoutParams(box, box));
		f.setOnClickListener(click);
		return f;
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
		int n = com.harvana.app.notify.NotificationCenter.unreadCount(this);
		notifMenuItem.setTitle(n > 0 ? "Notification (" + n + " new)" : "Notification");
		android.widget.FrameLayout f = _toolbarIcon(R.drawable.icon_notifications_outline,
				v -> startActivity(new Intent(this, com.harvana.app.NotificationsActivity.class)));
		if (n > 0) {
			TextView b = new TextView(this);
			b.setText(n > 99 ? "99+" : String.valueOf(n));
			b.setTextSize(9);
			b.setTextColor(com.harvana.app.utils.UiColors.onAccent());
			b.setGravity(android.view.Gravity.CENTER);
			b.setBackgroundResource(R.drawable.dot_online);
			b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(this, R.color.colorError)));
			b.setTypeface(null, android.graphics.Typeface.BOLD);
			int sz = (int) (16 * getResources().getDisplayMetrics().density);
			android.widget.FrameLayout.LayoutParams lp = new android.widget.FrameLayout.LayoutParams(sz, sz, android.view.Gravity.TOP | android.view.Gravity.END);
			int inset = (int) (6 * getResources().getDisplayMetrics().density);
			lp.topMargin = inset; lp.rightMargin = inset;
			f.addView(b, lp);
		}
		notifMenuItem.setActionView(f);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		final int _id = item.getItemId();
		final String _title = (String) item.getTitle();
		if (_id == 0) {
			Intent scanIntent = new Intent(this, com.harvana.app.ScanQrCodeActivity.class);
			scanIntent.putExtra(com.harvana.app.ScanQrCodeActivity.EXTRA_TITLE, "Scan a Harvana code");
			scanIntent.putExtra(com.harvana.app.ScanQrCodeActivity.EXTRA_MODE, com.harvana.app.ScanQrCodeActivity.MODE_ANY);
			startActivityForResult(scanIntent, RC_SCAN_TO_SEND);
			
		}
		if (_id == 1) {
			startActivity(new Intent(this, com.harvana.app.NotificationsActivity.class));
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
		new android.content.IntentFilter("com.harvana.app.ACTION_WALLET_RECEIVED")
		);
	}
	
	@Override
	public void onPause() {
		super.onPause();
		com.harvana.app.utils.PresenceHeartbeat.stop();
		if (notifTick != null) notifHandler.removeCallbacks(notifTick);
		try { androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).unregisterReceiver(notifChanged); } catch (Exception ignored) { }
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		try {
			com.harvana.app.utils.IncomingTxWatcher.stop();
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
		if (toolbarLogo != null) loadUserProfile(toolbarLogo, toolbarLogoPx);       // a photo changed in Edit profile shows on return
		com.harvana.app.utils.PresenceHeartbeat.start(getApplicationContext());
		com.harvana.app.group.GroupSaver.retryAll(getApplicationContext(), null);
		com.harvana.app.notify.NotificationWorker.schedule(getApplicationContext());
		com.harvana.app.notify.WalletWatchService.start(getApplicationContext());
		androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this)
		.registerReceiver(notifChanged, new android.content.IntentFilter(com.harvana.app.notify.NotificationCenter.ACTION_CHANGED));
		_updateNotifBadge();
		notifTick = new Runnable() {
			@Override public void run() {
				com.harvana.app.notify.NotificationSync.run(getApplicationContext(), null);
				notifHandler.postDelayed(this, 90_000L);
			}
		};
		notifHandler.post(notifTick);
		// Keep the local offline database from growing unbounded with data
		// from groups the user's since left, stale discover snapshots, etc.
		// Cheap no-op most of the time (only deletes rows older than a week).
		com.harvana.app.offline.OfflineCacheManager.pruneStale(getApplicationContext());
		// Replay anything that was queued while offline. Each queued action
		// type maps to the same code that would've run on a successful tap;
		// adding a new offline-safe write later just means adding another
		// branch here.
		com.harvana.app.offline.OfflineActionQueue.flush(getApplicationContext(),
		new com.harvana.app.offline.OfflineActionQueue.HandlerRegistry() {
			@Override
			public com.harvana.app.offline.OfflineActionQueue.ActionHandler handlerFor(String type) {
				if ("CONTRIBUTION".equals(type)) {
					return com.harvana.app.group.GroupPaymentHelper::replayContributionFromQueue;
				}
				if ("LOAN_REPAY".equals(type)) {
					return com.harvana.app.group.GroupLoanManager::replayRepaymentFromQueue;
				}
				if ("NOTICE".equals(type)) {
					return com.harvana.app.group.GroupNoticeHelper::replayNoticeFromQueue;
				}
				if ("PENDING_GROUP_MARK".equals(type)) {
					return com.harvana.app.firestore.UserProfileManager::replayPendingGroupMarkFromQueue;
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
		android.util.TypedValue.COMPLEX_UNIT_DIP, 32, getResources().getDisplayMetrics());
		
		// 2. Avatar Setup
		com.google.android.material.card.MaterialCardView avatarCard = new com.google.android.material.card.MaterialCardView(this);
		avatarCard.setLayoutParams(new LinearLayout.LayoutParams(sizePx, sizePx));
		avatarCard.setRadius(sizePx / 2f);
		avatarCard.setCardElevation(0f);
		int resolvedPrimaryColor = androidx.core.content.ContextCompat.getColor(this, R.color.colorPrimary);
		avatarCard.setCardBackgroundColor(android.graphics.Color.TRANSPARENT);
		
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
			avatarCard.setClipToOutline(true);
		}
		
		final com.harvana.app.ui.AvatarView logoView = new com.harvana.app.ui.AvatarView(this);
		logoView.setAvatarSize(com.harvana.app.ui.AvatarView.Size.HEADER);
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
		titleView.setText("Harvana");
		titleView.setTextColor(onAppBarColor);
		titleView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 24);
		titleView.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
		textContainer.addView(titleView);
		toolbarContainer.addView(textContainer);
		
		// 4. Click Listeners & Styling
		View.OnClickListener openProfile = v -> {
			try {
				com.google.firebase.auth.FirebaseUser me = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
				if (me != null) startActivity(new Intent(getApplicationContext(), com.harvana.app.social.PublicProfileActivity.class)
						.putExtra(com.harvana.app.social.PublicProfileActivity.EXTRA_UID, me.getUid()));
				else {
					intent_page_opener.setClass(getApplicationContext(), ProfileActivity.class);
					startActivity(intent_page_opener);
				}
			} catch (Exception ignored) {}
		};
		avatarCard.setOnClickListener(openProfile);
		logoView.setOnClickListener(openProfile);
		
		ab.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(appBarColor));
		ab.setElevation(0f);                      // flat, same colour as the body
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
		
		toolbarLogo = logoView;
		toolbarLogoPx = sizePx;
		loadUserProfile(logoView, sizePx);
	}
	
	private ImageView toolbarLogo;
	private int toolbarLogoPx;
	
	private void loadUserProfile(final ImageView logoView, final int sizePx) {
		FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
		if (user == null || user.getPhotoUrl() == null) {
			_setToolbarInitialAvatar("", "", logoView);
			return;
		}
		String photoUrl = user.getPhotoUrl().toString().trim();
		// The photo chosen in Edit profile is saved on the profile; prefer it over the one from sign-in.
		try {
			java.util.Map<String, Object> mine = com.harvana.app.offline.OfflineCacheManager.load(this, "profile_" + user.getUid());
			Object ic = mine == null ? null : mine.get("icon");
			if (ic != null && String.valueOf(ic).startsWith("http")) photoUrl = String.valueOf(ic).trim();
		} catch (Exception ignored) { }
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
			_applyHomeChrome();
		});
	}

	/** Home draws its own header. Other tabs keep the app bar. The bottom bar is unchanged. */
	private void _applyHomeChrome() {
		if (_app_bar == null) return;
		_app_bar.setVisibility(_activeFragment == _home ? View.GONE : View.VISIBLE);
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
		if (_activeFragment == fragment) { _applyHomeChrome(); return; }
		
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
		_activeFragment = fragment;
		_applyHomeChrome();
	}
	
	private void _showVersionDialog() {
		String v = "unknown";
		try { v = getPackageManager().getPackageInfo(getPackageName(), 0).versionName; } catch (Exception ignored) { }
		new com.harvana.app.ui.SheetDialog.Builder(this)
			.setTitle("Harvana")
			.setMessage("You are on version " + v + ".\n\nThis build is installed directly, so new versions arrive as a new install from the team, not through an app store update.")
			.setPositiveButton("OK", null).show();
	}
	
	/** The side menu (built in MainDrawer). */
	public void showRightDropdownMenu(View anchorView) {
		if (anchorView == null) anchorView = findViewById(android.R.id.content);
		if (anchorView == null) return;
		new MainDrawer(this, new MainDrawer.Host() {
			@Override public void openGroupsTab() { bottomnavigation1.setSelectedItemId(R.id.menu_groups); }
			@Override public void showVersion() { _showVersionDialog(); }
			@Override public void logOut() {
				com.harvana.app.auth.LogoutManager.logout(getApplicationContext());
				intent_page_opener.setClass(getApplicationContext(), SplashScreenActivity.class);
				startActivity(intent_page_opener);
				finish();
			}
		}).show(anchorView);
	}
	
	private final BroadcastReceiver globalTxReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent != null && "com.harvana.app.ACTION_WALLET_RECEIVED".equals(intent.getAction())) {
				double amount = intent.getDoubleExtra("EXTRA_AMOUNT", intent.getDoubleExtra("amount", 0.0));
				String sender = intent.getStringExtra("EXTRA_SENDER");
				if (sender == null) sender = intent.getStringExtra("sender");
				
				// Pass 4 arguments: Activity, Title, Message, Sender
				com.harvana.app.utils.InAppNotificationHelper.showTransactionNotification(
				MainActivity.this, // Replace with your current Activity
				"Incoming Transfer", 
				"Received " + String.format(Locale.US, "%.5f", amount) + (" " + com.harvana.app.CoinInfo.ticker()),
				sender
				);
			}
		}
	};
	
	public void _setToolbarInitialAvatar(final String _first, final String _last, final ImageView _img) {
		if (_img instanceof com.harvana.app.ui.AvatarView) {
			// the shared avatar draws its own initials (white on the blue disc) until a photo loads
			_img.setImageDrawable(null);
			String full = ((_first == null ? "" : _first.trim()) + " " + (_last == null ? "" : _last.trim())).trim();
			if (full.isEmpty()) {
				try {
					com.google.firebase.auth.FirebaseUser u = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
					if (u != null) {
						java.util.Map<String, Object> mine = com.harvana.app.offline.OfflineCacheManager.load(this, "profile_" + u.getUid());
						if (mine != null) full = (String.valueOf(mine.get("first_name") == null ? "" : mine.get("first_name")) + " " + String.valueOf(mine.get("last_name") == null ? "" : mine.get("last_name"))).trim();
						if (full.isEmpty() && u.getDisplayName() != null) full = u.getDisplayName();
					}
				} catch (Exception ignored) { }
			}
			((com.harvana.app.ui.AvatarView) _img).setPerson(full);
			return;
		}
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
			// Any code the app makes: an address opens Send, an access card adds the wallet, a receipt is checked on the network.
			com.harvana.app.scan.QrRouter.route(this, data.getStringExtra(com.harvana.app.ScanQrCodeActivity.EXTRA_RESULT_TEXT));
		}
	}
	
	{
	}
	
}
