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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.regex.*;
import org.json.*;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.harvana.app.wallet.BalanceFormatter;
import com.harvana.app.wallet.WalletManager;
import com.harvana.app.wallet.WalletNodeBalanceHelper;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.shape.CornerFamily;
import com.google.android.material.bottomnavigation.BottomNavigationView.OnNavigationItemSelectedListener;


public class HomeFragmentActivity extends Fragment {
	
	private FirebaseDatabase _firebase = FirebaseDatabase.getInstance();
	
	private HashMap<String, Object> user_v_map = new HashMap<>();
	private String mySeedPhrase = "";
	
	private ArrayList<HashMap<String, Object>> lmap_custom_button = new ArrayList<>();
	private ArrayList<HashMap<String, Object>> new_test_map = new ArrayList<>();
	private ArrayList<HashMap<String, Object>> paymentsMap = new ArrayList<>();
	private ArrayList<HashMap<String, Object>> articlesMap = new ArrayList<>();
	private ArrayList<HashMap<String, Object>> trendingMap = new ArrayList<>();
	private ArrayList<HashMap<String, Object>> group_map_list = new ArrayList<>();
	
	private NestedScrollView vscroll1;
	private LinearLayout linear2;
	private LinearLayout linear194;
	private MaterialCardView cardview28;
	private LinearLayout linear36;
	private TextView textview2;
	private TextView textview3;
	private TextView textview4;
	private LinearLayout linear217;
	private LinearLayout linear200;
	private LinearLayout layout_metric_grid;
	private MaterialCardView cardview1;
	private MaterialCardView cardview4;
	private MaterialCardView cardview2;
	private MaterialCardView cardview3;
	private LinearLayout cardview29;
	private LinearLayout cardview30;
	private LinearLayout cardview31;
	private LinearLayout linear37;
	private LinearLayout linear40;
	private MaterialCardView card_total_saved;
	private MaterialCardView card_active_groups;
	private LinearLayout linear198;
	private LinearLayout linear38;
	private LinearLayout linear219;
	private ImageView imageview58;
	private TextView textview205;
	private TextView text_val_saved;
	private TextView text_sub_saved;
	private LinearLayout linear196;
	private LinearLayout linear39;
	private LinearLayout linear218;
	private ImageView imageview57;
	private TextView textview204;
	private TextView text_val_groups;
	private TextView text_sub_groups;
	private MaterialCardView card_trust_score;
	private MaterialCardView card_open_loans;
	private LinearLayout linear201;
	private FrameLayout linear202;
	private LinearLayout linear215;
	private ProgressBar ring_trust_score;
	private TextView text_val_trust;
	private TextView textview206;
	private TextView text_sub_trust;
	private LinearLayout linear203;
	private LinearLayout linear41;
	private LinearLayout linear220;
	private ImageView imageview59;
	private TextView textview207;
	private TextView text_val_loans;
	private TextView text_sub_loans;
	private LinearLayout linear209;
	private LinearLayout linear210;
	private LinearLayout linear_upcoming_payments;
	private LinearLayout linear7;
	private TextView textview212;
	private TextView textview6;
	private ImageView imageview54;
	private TextView textview213;
	private LinearLayout linear30;
	private LinearLayout linear42;
	private TextView textview5;
	private LinearLayout linear221;
	private LinearLayout linear206;
	private CardView cardview27;
	private LinearLayout linear207;
	private ImageView imageview53;
	private LinearLayout linear208;
	private LinearLayout linear225;
	private TextView tv_local_currency_eqv;
	private TextView tv_harv_balance;
	private ImageView imageview61;
	private LinearLayout linear223;
	private MaterialCardView cardview32;
	private LinearLayout linear222;
	private TextView textview220;
	private ImageView imageview60;
	private LinearLayout linear211;
	private LinearLayout linear212;
	private TextView textview215;
	private LinearLayout linear_learn_hub;
	private TextView textview10;
	private ImageView imageview55;
	private TextView textview214;
	private LinearLayout linear213;
	private LinearLayout linear214;
	private LinearLayout linear_trending_groups;
	private LinearLayout linear10;
	private TextView textview216;
	private TextView textview12;
	private ImageView imageview56;
	private TextView textview217;
	
	private SharedPreferences authsp;
	private Calendar c = Calendar.getInstance();
	private SharedPreferences groupsp;
	private Intent intent = new Intent();
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
	private DatabaseReference userdb = _firebase.getReference("userdb");
	private ChildEventListener _userdb_child_listener;
	
	@NonNull
	@Override
	public View onCreateView(@NonNull LayoutInflater _inflater, @Nullable ViewGroup _container, @Nullable Bundle _savedInstanceState) {
		View _view = _inflater.inflate(R.layout.home_fragment, _container, false);
		initialize(_savedInstanceState, _view);
		FirebaseApp.initializeApp(getContext());
		initializeLogic();
		_addQuickActions(_view);
		if (card_trust_score != null) card_trust_score.setOnClickListener(v -> startActivity(new Intent(getContext(), TrustScoreActivity.class)));
		if (card_open_loans != null) card_open_loans.setOnClickListener(v -> startActivity(new Intent(getContext(), OpenLoansActivity.class)));
		return _view;
	}
	
	private void initialize(Bundle _savedInstanceState, View _view) {
		vscroll1 = _view.findViewById(R.id.vscroll1);
		linear2 = _view.findViewById(R.id.linear2);
		linear194 = _view.findViewById(R.id.linear194);
		cardview28 = _view.findViewById(R.id.cardview28);
		linear36 = _view.findViewById(R.id.linear36);
		textview2 = _view.findViewById(R.id.textview2);
		textview3 = _view.findViewById(R.id.textview3);
		textview4 = _view.findViewById(R.id.textview4);
		linear217 = _view.findViewById(R.id.linear217);
		linear200 = _view.findViewById(R.id.linear200);
		layout_metric_grid = _view.findViewById(R.id.layout_metric_grid);
		cardview1 = _view.findViewById(R.id.cardview1);
		cardview4 = _view.findViewById(R.id.cardview4);
		cardview2 = _view.findViewById(R.id.cardview2);
		cardview3 = _view.findViewById(R.id.cardview3);
		cardview29 = _view.findViewById(R.id.cardview29);
		cardview30 = _view.findViewById(R.id.cardview30);
		cardview31 = _view.findViewById(R.id.cardview31);
		linear37 = _view.findViewById(R.id.linear37);
		linear40 = _view.findViewById(R.id.linear40);
		card_total_saved = _view.findViewById(R.id.card_total_saved);
		card_active_groups = _view.findViewById(R.id.card_active_groups);
		linear198 = _view.findViewById(R.id.linear198);
		linear38 = _view.findViewById(R.id.linear38);
		linear219 = _view.findViewById(R.id.linear219);
		imageview58 = _view.findViewById(R.id.imageview58);
		textview205 = _view.findViewById(R.id.textview205);
		text_val_saved = _view.findViewById(R.id.text_val_saved);
		text_sub_saved = _view.findViewById(R.id.text_sub_saved);
		linear196 = _view.findViewById(R.id.linear196);
		linear39 = _view.findViewById(R.id.linear39);
		linear218 = _view.findViewById(R.id.linear218);
		imageview57 = _view.findViewById(R.id.imageview57);
		textview204 = _view.findViewById(R.id.textview204);
		text_val_groups = _view.findViewById(R.id.text_val_groups);
		text_sub_groups = _view.findViewById(R.id.text_sub_groups);
		card_trust_score = _view.findViewById(R.id.card_trust_score);
		card_open_loans = _view.findViewById(R.id.card_open_loans);
		linear201 = _view.findViewById(R.id.linear201);
		linear202 = _view.findViewById(R.id.linear202);
		linear215 = _view.findViewById(R.id.linear215);
		ring_trust_score = _view.findViewById(R.id.ring_trust_score);
		text_val_trust = _view.findViewById(R.id.text_val_trust);
		textview206 = _view.findViewById(R.id.textview206);
		text_sub_trust = _view.findViewById(R.id.text_sub_trust);
		linear203 = _view.findViewById(R.id.linear203);
		linear41 = _view.findViewById(R.id.linear41);
		linear220 = _view.findViewById(R.id.linear220);
		imageview59 = _view.findViewById(R.id.imageview59);
		textview207 = _view.findViewById(R.id.textview207);
		text_val_loans = _view.findViewById(R.id.text_val_loans);
		text_sub_loans = _view.findViewById(R.id.text_sub_loans);
		linear209 = _view.findViewById(R.id.linear209);
		linear210 = _view.findViewById(R.id.linear210);
		linear_upcoming_payments = _view.findViewById(R.id.linear_upcoming_payments);
		linear7 = _view.findViewById(R.id.linear7);
		textview212 = _view.findViewById(R.id.textview212);
		textview6 = _view.findViewById(R.id.textview6);
		imageview54 = _view.findViewById(R.id.imageview54);
		textview213 = _view.findViewById(R.id.textview213);
		linear30 = _view.findViewById(R.id.linear30);
		linear42 = _view.findViewById(R.id.linear42);
		textview5 = _view.findViewById(R.id.textview5);
		linear221 = _view.findViewById(R.id.linear221);
		linear206 = _view.findViewById(R.id.linear206);
		cardview27 = _view.findViewById(R.id.cardview27);
		linear207 = _view.findViewById(R.id.linear207);
		imageview53 = _view.findViewById(R.id.imageview53);
		linear208 = _view.findViewById(R.id.linear208);
		linear225 = _view.findViewById(R.id.linear225);
		tv_local_currency_eqv = _view.findViewById(R.id.tv_local_currency_eqv);
		tv_harv_balance = _view.findViewById(R.id.tv_harv_balance);
		imageview61 = _view.findViewById(R.id.imageview61);
		// The eye: one saved show/hide switch for every balance in the app (see BalancePrivacy).
		com.harvana.app.utils.BalancePrivacy.bindEye(imageview61);
		com.harvana.app.utils.BalancePrivacy.guard(tv_harv_balance);
		com.harvana.app.utils.BalancePrivacy.guard(tv_local_currency_eqv);
		com.harvana.app.utils.BalancePrivacy.guard(text_val_saved);
		com.harvana.app.utils.BalancePrivacy.guard(text_val_loans);
		linear223 = _view.findViewById(R.id.linear223);
		cardview32 = _view.findViewById(R.id.cardview32);
		linear222 = _view.findViewById(R.id.linear222);
		textview220 = _view.findViewById(R.id.textview220);
		imageview60 = _view.findViewById(R.id.imageview60);
		linear211 = _view.findViewById(R.id.linear211);
		linear212 = _view.findViewById(R.id.linear212);
		textview215 = _view.findViewById(R.id.textview215);
		linear_learn_hub = _view.findViewById(R.id.linear_learn_hub);
		textview10 = _view.findViewById(R.id.textview10);
		imageview55 = _view.findViewById(R.id.imageview55);
		textview214 = _view.findViewById(R.id.textview214);
		linear213 = _view.findViewById(R.id.linear213);
		linear214 = _view.findViewById(R.id.linear214);
		linear_trending_groups = _view.findViewById(R.id.linear_trending_groups);
		linear10 = _view.findViewById(R.id.linear10);
		textview216 = _view.findViewById(R.id.textview216);
		textview12 = _view.findViewById(R.id.textview12);
		imageview56 = _view.findViewById(R.id.imageview56);
		textview217 = _view.findViewById(R.id.textview217);
		authsp = getContext().getSharedPreferences("authsp", Activity.MODE_PRIVATE);
		groupsp = getContext().getSharedPreferences("groupsp", Activity.MODE_PRIVATE);
		fbauth = FirebaseAuth.getInstance();
		
		card_active_groups.setOnClickListener(_v -> {
			if (getActivity() != null) {
				com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = 
				getActivity().findViewById(R.id.bottomnavigation1);
				
				if (bottomNav != null) {
					bottomNav.setSelectedItemId(R.id.menu_groups);
				}
			}
		});
		
		textview6.setOnClickListener(_v -> {
			if (getActivity() != null) {
				com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = 
				getActivity().findViewById(R.id.bottomnavigation1);
				
				if (bottomNav != null) {
					bottomNav.setSelectedItemId(R.id.menu_groups);
				}
			}
		});
		
		cardview32.setOnClickListener(_v -> {
			if (getActivity() != null) {
				com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = 
				getActivity().findViewById(R.id.bottomnavigation1);
				
				if (bottomNav != null) {
					bottomNav.setSelectedItemId(R.id.menu_asset);
				}
			}
		});
		
		textview10.setOnClickListener(_v -> {
			if (getActivity() != null) {
				com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = 
				getActivity().findViewById(R.id.bottomnavigation1);
				
				if (bottomNav != null) {
					bottomNav.setSelectedItemId(R.id.menu_learn);
				}
			}
		});
		
		textview12.setOnClickListener(_v -> {
			if (getActivity() != null) {
				com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = 
				getActivity().findViewById(R.id.bottomnavigation1);
				
				if (bottomNav != null) {
					bottomNav.setSelectedItemId(R.id.menu_discover);
				}
			}
		});
		
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
		Calendar c = Calendar.getInstance();
		if (textview2 != null)
		textview2.setText(new SimpleDateFormat("EEEE dd MMMM", Locale.getDefault()).format(c.getTime()));
		if (textview4 != null)
		textview4.setText("Here's what's happening in your circles today.");
		_loadProfile();
		_loadWalletBalance();
		_loadLearnHubPreview();
		_loadTrendingGroups();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		_loadProfile();
		_loadWalletBalance();
		_loadLearnHubPreview();
		_loadTrendingGroups();
		if (getContext() != null) {
			com.harvana.app.sync.NodeLinkManager.ensureLinked(getContext(), null);
			com.harvana.app.utils.ActivityTracker.reportLiveness(getContext());
			com.harvana.app.group.InviteManager.checkAndNotify(getContext());
			final android.content.Context ic = getContext().getApplicationContext();
			com.harvana.app.group.InviteManager.listMine(ic, new com.harvana.app.group.InviteManager.ListCb() {
				@Override public void onList(java.util.List<java.util.Map<String, Object>> invites) {
					if (invites.isEmpty() || getActivity() == null || getView() == null) return;
					getActivity().runOnUiThread(() -> {
						if (getView() == null) return;
						com.google.android.material.snackbar.Snackbar.make(getView(),
							"You have " + invites.size() + " group invitation" + (invites.size() > 1 ? "s" : ""),
							com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
							.setAction("View", v -> startActivity(new Intent(getContext(), InvitesActivity.class))).show();
					});
				}
				@Override public void onError(String e) { }
			});
		}
	}
	public void _MORE() {
	}
	private void _openTab(Fragment fragment, int menuId) {
		if (getActivity() == null) return;
		int containerId = R.id.linear2;
		int alt = getResources().getIdentifier("fragment_container", "id", requireContext().getPackageName());
		if (alt != 0) containerId = alt;
		int mainFrame = getResources().getIdentifier("frame_main", "id", requireContext().getPackageName());
		if (mainFrame != 0) containerId = mainFrame;
		try {
			getParentFragmentManager().beginTransaction()
			.replace(containerId, fragment)
			.addToBackStack(null)
			.commit();
		} catch (Exception ignored) {}
		BottomNavigationView nav = getActivity().findViewById(R.id.bottomnavigation1);
		if (nav != null) {
			try { nav.getMenu().findItem(menuId).setChecked(true); } catch (Exception ignored) {}
		}
	}
	
	private void _loadProfile() {
		FirebaseAuth auth = FirebaseAuth.getInstance();
		if (auth.getCurrentUser() == null) {
			if (textview3 != null) textview3.setText(_greeting());
			_loadUpcomingPayments();
			return;
		}
		String uid = auth.getCurrentUser().getUid();
		Map<String, Object> cachedProfile = com.harvana.app.offline.OfflineCacheManager.load(
		getContext(), "profile_" + uid);
		if (cachedProfile != null && !cachedProfile.isEmpty()) {
			_bindProfileFields(cachedProfile);
		}
		
		com.harvana.app.firestore.UserProfileManager.getInstance(getContext())
		.getProfile(uid, new com.harvana.app.firestore.UserProfileManager.DocCallback() {
			@Override
			public void onSuccess(Map<String, Object> data) {
				if (!isAdded() || getActivity() == null) return;
				com.harvana.app.offline.OfflineCacheManager.save(getContext(), "profile_" + uid, data);
				getActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						_bindProfileFields(data);
					}
				});
				_selfHealGroupMemberships(uid);
			}
			
			@Override
			public void onError(String error) {
				if (!isAdded() || getActivity() == null) return;
				if (com.harvana.app.offline.NetworkStatus.isConnectivityError(error)
				&& cachedProfile != null && !cachedProfile.isEmpty()) {
					return;
				}
				getActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (textview3 != null) textview3.setText(_greeting());
						if (text_val_groups != null) text_val_groups.setText("0");
						_loadUpcomingPayments();
					}
				});
			}
		});
	}
	
	private void _selfHealGroupMemberships(String uid) {
		com.harvana.app.group.JoinRequestManager.selfHealApprovedMemberships(getContext(), uid,
		healedCount -> {
			if (healedCount <= 0) return;
			if (!isAdded() || getActivity() == null) return;
			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					_loadProfile();
				}
			});
		});
	}
	
	@SuppressWarnings("unchecked")
	private void _bindProfileFields(Map<String, Object> data) {
		if (!isAdded() || getContext() == null) return;
		if (data == null) data = new HashMap<>();
		
		Object firstObj = data.get("first_name");
		String first = firstObj != null ? firstObj.toString().trim() : "";
		if (textview3 != null)
		textview3.setText(_greeting() + (first.isEmpty() ? "" : ", " + first));
		
		if (text_val_trust != null) text_val_trust.setText("—");
		if (ring_trust_score != null) {
			ring_trust_score.setMax(com.harvana.app.trust.NetworkTrust.MAX);
			ring_trust_score.setProgress(0);
		}
		if (text_sub_trust != null) text_sub_trust.setText("of 26");
		
		if (text_val_loans != null) text_val_loans.setText("0");
		if (text_sub_loans != null) text_sub_loans.setText("No open loans");
		
		group_map_list.clear();
		Object groupsObj = data.get("groups");
		if (groupsObj instanceof List) {
			for (Object g : (List<Object>) groupsObj) {
				if (g == null) continue;
				String gid = g.toString().trim();
				if (gid.isEmpty()) continue;
				HashMap<String, Object> row = new HashMap<>();
				row.put("id", gid);
				group_map_list.add(row);
			}
		}
		int n = group_map_list.size();
		if (text_val_groups != null) text_val_groups.setText(String.valueOf(n));
		if (text_sub_groups != null)
		text_sub_groups.setText(n == 0 ? "Join your first circle"
		: (n == 1 ? "1 active circle" : n + " active circles"));
		
		_loadUpcomingPayments();
		_loadTotalSavedAcrossGroups();
		_loadOpenLoansAcrossGroups();
		_loadNodeTotals();
		_loadComputedTrust();
		_loadTrendingGroups();
	}
	
	private void _loadWalletBalance() {
		String addr = WalletManager.getActiveWalletAddress(getContext());
		SharedPreferences currencySp = getContext().getSharedPreferences("currencysp", Context.MODE_PRIVATE);
		final String currencySymbol = currencySp.getString("currency", "₦");
		final String currencyCode = com.harvana.app.wallet.FiatValueHelper.symbolToCurrencyCode(currencySymbol);
		
		if (addr == null || addr.isEmpty()) {
			if (tv_harv_balance != null) tv_harv_balance.setText("0.00");
			if (tv_local_currency_eqv != null) tv_local_currency_eqv.setText("≈ " + currencySymbol + "0.00");
			return;
		}
		
		Map<String, Object> cachedBalance = com.harvana.app.offline.OfflineCacheManager.load(
		getContext(), "wallet_balance_" + addr);
		if (cachedBalance != null && cachedBalance.get("show") instanceof Number) {
			double cachedShow = ((Number) cachedBalance.get("show")).doubleValue();
			if (tv_harv_balance != null) tv_harv_balance.setText(_fmtAmount(cachedShow));
			
			if (tv_local_currency_eqv != null) {
				com.harvana.app.wallet.FiatValueHelper.convertHarvToFiat(cachedShow, currencyCode, (fiatValue, rateAvailable) -> {
					if (!isAdded() || getActivity() == null) return;
					getActivity().runOnUiThread(() -> tv_local_currency_eqv.setText(rateAvailable
					? String.format(Locale.US, "≈ %s%,.2f", currencySymbol, fiatValue)
					: "≈ " + currencySymbol + "0.00"));
				});
			}
		}
		
		WalletNodeBalanceHelper.load(addr, (bal, avail, pin, pout, pending, err) -> {
			if (!isAdded() || getActivity() == null) return;
			if (err != null && com.harvana.app.offline.NetworkStatus.isConnectivityError(err)
			&& cachedBalance != null) {
				return;
			}
			double show = avail >= 0 ? avail : bal;
			if (err == null) {
				Map<String, Object> toCache = new HashMap<>();
				toCache.put("show", show);
				com.harvana.app.offline.OfflineCacheManager.save(getContext(), "wallet_balance_" + addr, toCache);
			}
			getActivity().runOnUiThread(() -> {
				if (tv_harv_balance != null)
				tv_harv_balance.setText(_fmtAmount(show));
				if (tv_local_currency_eqv != null) {
					com.harvana.app.wallet.FiatValueHelper.convertHarvToFiat(show, currencyCode, (fiatValue, rateAvailable) -> {
						if (!isAdded() || getActivity() == null) return;
						getActivity().runOnUiThread(() -> tv_local_currency_eqv.setText(rateAvailable
						? String.format(Locale.US, "≈ %s%,.2f", currencySymbol, fiatValue)
						: "≈ " + currencySymbol + "0.00"));
					});
				}
			});
		});
	}
	
	private void _loadUpcomingPayments() {
		if (linear_upcoming_payments == null || getContext() == null) return;
		
		final String myUid = FirebaseAuth.getInstance().getCurrentUser() != null
		? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
		List<HashMap<String, Object>> cachedRows = com.harvana.app.offline.OfflineCacheManager.loadList(
		getContext(), "upcoming_payments_v2_" + myUid);
		if (cachedRows != null && !cachedRows.isEmpty()) {
			_renderUpcoming(new ArrayList<>(cachedRows));
		} else if (group_map_list.isEmpty() || myUid.isEmpty()) {
			linear_upcoming_payments.removeAllViews();
			if (linear7 != null) linear7.setVisibility(View.VISIBLE);
		} else if (linear_upcoming_payments.getChildCount() == 0) {   // first load only
			linear_upcoming_payments.addView(com.harvana.app.utils.Shimmer.rows(getContext(), 2));
			if (linear7 != null) linear7.setVisibility(View.GONE);
		}
		
		if (group_map_list.isEmpty() || myUid.isEmpty()) return;
		
		final ArrayList<HashMap<String, Object>> rows = new ArrayList<>();
		final int[] left = {group_map_list.size()};
		final boolean[] hadConnectivityFailure = {false};
		final Runnable oneDone = () -> {
			left[0]--;
			if (left[0] <= 0) _finishUpcoming(myUid, rows, hadConnectivityFailure[0], cachedRows);
		};
		
		for (HashMap<String, Object> g : group_map_list) {
			final String gid = g.get("id") != null ? g.get("id").toString() : "";
			if (gid.isEmpty()) { oneDone.run(); continue; }
			final com.harvana.app.firestore.GroupFirestoreManager mgr =
					com.harvana.app.firestore.GroupFirestoreManager.getInstance(getContext());
			mgr.getGroup(gid, new com.harvana.app.firestore.GroupFirestoreManager.DocCallback() {
				@Override
				public void onSuccess(Map<String, Object> data) {
					if (com.harvana.app.group.GroupKinds.isLegacyRotating(data)) { oneDone.run(); return; }
					mgr.getMembers(gid, new com.harvana.app.firestore.GroupFirestoreManager.ListCallback() {
						@Override public void onSuccess(List<Map<String, Object>> members) {
							mgr.getPayments(gid, new com.harvana.app.firestore.GroupFirestoreManager.ListCallback() {
								@Override public void onSuccess(List<Map<String, Object>> payments) {
									_addUpcomingRow(rows, gid, data, members, payments, myUid);
									oneDone.run();
								}
								@Override public void onError(String error) {
									if (com.harvana.app.offline.NetworkStatus.isConnectivityError(error)) hadConnectivityFailure[0] = true;
									_addUpcomingRow(rows, gid, data, members, null, myUid);
									oneDone.run();
								}
							});
						}
						@Override public void onError(String error) {
							if (com.harvana.app.offline.NetworkStatus.isConnectivityError(error)) hadConnectivityFailure[0] = true;
							oneDone.run();
						}
					});
				}
				
				@Override
				public void onError(String error) {
					if (com.harvana.app.offline.NetworkStatus.isConnectivityError(error)) {
						hadConnectivityFailure[0] = true;
					}
					oneDone.run();
				}
			});
		}
	}
	
	private void _addUpcomingRow(ArrayList<HashMap<String, Object>> rows, String gid, Map<String, Object> data,
	List<Map<String, Object>> members, List<Map<String, Object>> payments, String myUid) {
		com.harvana.app.group.PaymentDueResolver.Result r = com.harvana.app.group.PaymentDueResolver.resolve(
		data, members, payments, myUid, System.currentTimeMillis(), java.util.TimeZone.getDefault());
		HashMap<String, Object> row = new HashMap<>();
		row.put("groupId", gid);
		row.put("groupName", str(data, "groupName", str(data, "name", "Group")));
		row.put("groupPhoto", str(data, "groupPhoto", ""));
		row.put("groupType", str(data, "groupType", ""));
		row.put("amount", num(data, "contributionAmount"));
		row.put("frequency", str(data, "frequency", ""));
		row.put("action", r.action.name());
		row.put("otherUid", r.otherMemberUid != null ? r.otherMemberUid : "");
		row.put("statusLine", r.statusLine);
		row.put("badge", r.badge);
		synchronized (rows) { rows.add(row); }
	}
	
	private void _finishUpcoming(String myUid, ArrayList<HashMap<String, Object>> rows,
	boolean hadConnectivityFailure, List<HashMap<String, Object>> cachedRows) {
		if (hadConnectivityFailure) {
			if (cachedRows == null) { java.util.Collections.sort(rows, UPCOMING_ORDER); _renderUpcoming(rows); }
			return;
		}
		java.util.Collections.sort(rows, UPCOMING_ORDER);
		com.harvana.app.offline.OfflineCacheManager.saveList(getContext(), "upcoming_payments_v2_" + myUid, rows);
		_renderUpcoming(rows);
	}
	
	/**
	 * Upcoming payments keep one fixed order, so the list does not shuffle on every refresh. The groups' answers arrive in whatever
	 * order the network happens to return them, so arrival order must never decide: first what needs doing (pay, remind, ...), then
	 * the group's name, then its id, which never changes.
	 */
	private static final java.util.Comparator<HashMap<String, Object>> UPCOMING_ORDER = (x, y) -> {
		int byAction = Integer.compare(rank(String.valueOf(x.get("action"))), rank(String.valueOf(y.get("action"))));
		if (byAction != 0) return byAction;
		int byName = String.valueOf(x.get("groupName")).compareToIgnoreCase(String.valueOf(y.get("groupName")));
		if (byName != 0) return byName;
		return String.valueOf(x.get("groupId")).compareTo(String.valueOf(y.get("groupId")));
	};
	
	private static int rank(String action) {
		return "PAY".equals(action) ? 0 : ("REMIND".equals(action) ? 1 : 2);
	}
	
	private void _renderUpcoming(ArrayList<HashMap<String, Object>> rows) {
		if (!isAdded() || linear_upcoming_payments == null || getActivity() == null) return;
		getActivity().runOnUiThread(() -> {
			linear_upcoming_payments.removeAllViews();
			int show = Math.min(2, rows.size());
			if (show == 0) {
				if (linear7 != null) linear7.setVisibility(View.VISIBLE);
				return;
			}
			if (linear7 != null) linear7.setVisibility(View.GONE);
			LayoutInflater inf = LayoutInflater.from(getContext());
			for (int i = 0; i < show; i++) {
				final HashMap<String, Object> row = rows.get(i);
				View item = inf.inflate(R.layout.home_upcoming_pay_custom_display,
				linear_upcoming_payments, false);
				TextView name = item.findViewById(R.id.textview2);
				TextView meta = item.findViewById(R.id.textview3);
				TextView statusLine = item.findViewById(R.id.textview5);
				TextView badge = item.findViewById(R.id.textview4);
				com.google.android.material.card.MaterialCardView badgeCard = item.findViewById(R.id.cardview3);
				com.google.android.material.button.MaterialButton button = item.findViewById(R.id.materialbutton1);
				final String gid = str(row, "groupId", "");
				final String action = str(row, "action", "NONE");
				final String otherUid = str(row, "otherUid", "");
				final String amountText = com.harvana.app.wallet.BalanceFormatter.smart(num(row, "amount"));
				
				ImageView face = item.findViewById(R.id.imageview1);
				if (face != null) com.harvana.app.utils.GroupRing.show(face, str(row, "groupType", ""), str(row, "groupPhoto", ""), R.drawable.ic_coin_harv);
				if (name != null) name.setText(str(row, "groupName", "Group"));
				if (meta != null) meta.setText(amountText + (str(row, "frequency", "").isEmpty() ? "" : " · " + str(row, "frequency", "")));
				if (statusLine != null) statusLine.setText(str(row, "statusLine", ""));
				
				int badgeColor = R.color.colorTextTertiary;
				if ("PAY".equals(action)) badgeColor = R.color.colorWarning;
				else if ("REMIND".equals(action)) badgeColor = R.color.colorInfo;
				else if ("Paid".equals(str(row, "badge", ""))) badgeColor = R.color.colorSuccess;
				int c = androidx.core.content.ContextCompat.getColor(requireContext(), badgeColor);
				if (badge != null) { badge.setText(str(row, "badge", "")); badge.setTextColor(c); }
				if (badgeCard != null) badgeCard.setStrokeColor(c);
				
				item.setOnClickListener(v -> _openGroup(gid));
				
				if ("PAY".equals(action) && getContext() != null) {
					String day = com.harvana.app.notify.DueCopy.dayKey(System.currentTimeMillis());
					String amountOnly = com.harvana.app.wallet.BalanceFormatter.smartDecimal(num(row, "amount"));
					com.harvana.app.notify.NotificationCenter.post(getContext(), com.harvana.app.notify.NotificationCenter.PAYMENT,
					com.harvana.app.notify.DueCopy.key(gid, day),
					com.harvana.app.notify.DueCopy.title(str(row, "groupName", "Group")),
					com.harvana.app.notify.DueCopy.body(amountOnly, com.harvana.app.CoinInfo.ticker()), gid, 2);
				}
				
				if (button == null) { linear_upcoming_payments.addView(item); continue; }
				if ("PAY".equals(action)) {
					button.setText("Pay");
					button.setEnabled(true);
					button.setAlpha(1f);
					button.setOnClickListener(v -> _openGroupPayments(gid));
				} else if ("REMIND".equals(action) && !otherUid.isEmpty()) {
					button.setText("Remind");
					button.setEnabled(true);
					button.setAlpha(1f);
					com.harvana.app.group.MemberNameResolver.resolve(getContext(), otherUid, "a member", who -> {
						if (isAdded() && statusLine != null) statusLine.setText("Waiting on " + who);
						button.setOnClickListener(v -> {
							button.setEnabled(false);
							com.harvana.app.group.GroupReminders.remind(getContext(), gid, otherUid, who, amountText,
							new com.harvana.app.group.GroupReminders.Callback() {
								@Override public void onSent() {
									if (!isAdded()) return;
									button.setText("Reminded");
									android.widget.Toast.makeText(getContext(), "Reminder sent to " + who, android.widget.Toast.LENGTH_SHORT).show();
								}
								@Override public void onError(String error) {
									if (!isAdded()) return;
									button.setEnabled(true);
									android.widget.Toast.makeText(getContext(), error, android.widget.Toast.LENGTH_LONG).show();
								}
							});
						});
					});
				} else {
					String why = str(row, "badge", "");
					if (why.isEmpty() || "Pay".equalsIgnoreCase(why)) why = "Not due";
					button.setText(why);
					button.setEnabled(false);
					button.setAlpha(0.55f);
					button.setOnClickListener(null);
				}
				linear_upcoming_payments.addView(item);
			}
		});
	}
	
	private void _openGroupPayments(String gid) {
		if (gid == null || gid.isEmpty()) return;
		intent.setClass(getContext().getApplicationContext(), GroupDetailsActivity.class);
		intent.putExtra("groupId", gid);
		intent.putExtra("tab", 2);
		startActivity(intent);
	}
	
	private void _openGroup(String gid) {
		if (gid == null || gid.isEmpty()) return;
		intent.setClass(getContext().getApplicationContext(), GroupDetailsActivity.class);
		intent.removeExtra("tab");
		intent.putExtra("groupId", gid);
		startActivity(intent);
	}
	
	private void _loadLearnHubPreview() {
		if (linear_learn_hub == null || getContext() == null) return;
		if (linear_learn_hub.getChildCount() == 0) linear_learn_hub.addView(com.harvana.app.utils.Shimmer.rows(getContext(), 2));   // first load only
		FirebaseDatabase.getInstance().getReference("learnhub")
		.limitToLast(2)
		.addListenerForSingleValueEvent(new ValueEventListener() {
			@Override
			public void onDataChange(@NonNull DataSnapshot snapshot) {
				if (!isAdded() || getActivity() == null) return;
				ArrayList<DataSnapshot> items = new ArrayList<>();
				for (DataSnapshot c : snapshot.getChildren()) items.add(c);
				Collections.reverse(items);
				getActivity().runOnUiThread(() -> {
					linear_learn_hub.removeAllViews();
					LayoutInflater inf = LayoutInflater.from(getContext());
					int n = 0;
					for (DataSnapshot c : items) {
						if (n >= 2) break;
						View item = inf.inflate(R.layout.home_learnhub_custom_display,
						linear_learn_hub, false);
						TextView title = item.findViewById(R.id.title_text);
						TextView cat = item.findViewById(R.id.category_text);
						String t = c.child("title").getValue(String.class);
						String category = c.child("category").getValue(String.class);
						Object views = c.child("views").getValue();
						if (title != null) {
							title.setText(t != null ? t : "Learn article");
							try {
								title.setTextColor(getResources().getColor(R.color.colorTextPrimary));
							} catch (Exception ignored) {}
						}
						if (cat != null)
						cat.setText((category != null ? category : "Finance")
						+ " · " + (views != null ? views : "0") + " views");
						item.setOnClickListener(v ->
						_openTab(new LearnFragmentActivity(), R.id.menu_learn));
						linear_learn_hub.addView(item);
						n++;
					}
					if (n == 0) {
						TextView empty = new TextView(getContext());
						empty.setText("No Learn Hub posts yet.");
						empty.setTextSize(13f);
						empty.setPadding(0, 16, 0, 8);
						linear_learn_hub.addView(empty);
					}
				});
			}
			
			@Override
			public void onCancelled(@NonNull DatabaseError error) {
				if (!isAdded() || getActivity() == null) return;
				getActivity().runOnUiThread(() -> {
					linear_learn_hub.removeAllViews();
					TextView empty = new TextView(getContext());
					empty.setText("Open Learn Hub for tips.");
					empty.setTextSize(13f);
					linear_learn_hub.addView(empty);
				});
			}
		});
	}
	
	private void _loadTrendingGroups() {
		if (linear_trending_groups == null || getContext() == null) return;
		if (linear_trending_groups.getChildCount() == 0) {          // first load only; later loads keep what is shown until new data arrives
			linear_trending_groups.addView(com.harvana.app.utils.Shimmer.rows(getContext(), 2));
			if (linear10 != null) linear10.setVisibility(View.GONE);
		}
		
		final String meUid = FirebaseAuth.getInstance().getCurrentUser() != null
		? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
		final java.util.Set<String> mine = new java.util.HashSet<>();
		for (HashMap<String, Object> g : group_map_list) {
			Object id = g.get("id");
			if (id != null) mine.add(String.valueOf(id));
		}
		com.harvana.app.firestore.GroupFirestoreManager.getInstance(getContext())
		.queryGroups("visibility", "Public", 30,
		new com.harvana.app.firestore.GroupFirestoreManager.ListCallback() {
			@Override
			public void onSuccess(List<Map<String, Object>> list) {
				if (!isAdded() || getActivity() == null) return;
				ArrayList<Map<String, Object>> sorted = new ArrayList<>();
				if (list != null) {
					for (Map<String, Object> g : list) {
						String gid0 = str(g, "_docId", str(g, "groupId", str(g, "id", "")));
						if (mine.contains(gid0) || (!meUid.isEmpty() && meUid.equals(str(g, "creatorId", "")))) continue;
						if (com.harvana.app.group.GroupKinds.isLegacyRotating(g)) continue;
						sorted.add(g);
					}
				}
				// Most members first; equal counts fall back to name then id, so the two shown never swap places between refreshes.
				Collections.sort(sorted, (a, b) -> {
					int byMembers = Double.compare(num(b, "membersCount"), num(a, "membersCount"));
					if (byMembers != 0) return byMembers;
					int byName = str(a, "groupName", "").compareToIgnoreCase(str(b, "groupName", ""));
					return byName != 0 ? byName : str(a, "_docId", "").compareTo(str(b, "_docId", ""));
				});
				getActivity().runOnUiThread(() -> {
					linear_trending_groups.removeAllViews();
					int show = Math.min(2, sorted.size());
					if (show == 0) {
						if (linear10 != null) linear10.setVisibility(View.VISIBLE);
						return;
					}
					if (linear10 != null) linear10.setVisibility(View.GONE);
					LayoutInflater inf = LayoutInflater.from(getContext());
					for (int i = 0; i < show; i++) {
						Map<String, Object> g = sorted.get(i);
						View item = inf.inflate(R.layout.home_trending_custom_display,
						linear_trending_groups, false);
						TextView name = item.findViewById(R.id.textview1);
						TextView verified = item.findViewById(R.id.textview2);
						TextView meta = item.findViewById(R.id.textview3);
						TextView trust = item.findViewById(R.id.textview4);
						TextView join = item.findViewById(R.id.btn_join);
						
						String gname = str(g, "groupName", str(g, "name", "Public group"));
						String loc = str(g, "location", str(g, "city", "Nigeria"));
						String freq = str(g, "frequency", "Weekly");
						int members = (int) num(g, "membersCount");
						if (members <= 0) members = (int) num(g, "totalSlots");
						String nodeId = com.harvana.app.trust.NetworkTrust.groupKey(g);
						String gid = str(g, "_docId", str(g, "groupId", str(g, "id", "")));
						
						if (name != null) name.setText(gname);
						ImageView face = item.findViewById(R.id.imageview1);
						if (face != null) com.harvana.app.utils.GroupRing.show(face, str(g, "groupType", ""), str(g, "groupPhoto", ""), R.drawable.ic_coin_harv);
						if (verified != null) verified.setVisibility(View.GONE);
						if (meta != null)
						meta.setText(loc + " · " + members + " members · " + freq);
						if (trust != null) {
							trust.setTag(nodeId);
							trust.setText(nodeId.isEmpty() ? "Not on the network" : "Trust …");
							if (!nodeId.isEmpty()) {
								com.harvana.app.trust.NetworkTrust.group(nodeId, score -> {
									if (!isAdded() || getActivity() == null) return;
									getActivity().runOnUiThread(() -> {
										if (!nodeId.equals(trust.getTag())) return;
										if (!score.published || score.summary.isEmpty()) {
											trust.setText("Not on the network");
											return;
										}
										trust.setText(score.summary);
										boolean strong = score.percent >= 65 || score.summary.startsWith("High trust");
										if (verified != null && strong) {
											verified.setText(score.percent >= 0 ? score.tier : "High trust");
											verified.setVisibility(View.VISIBLE);
										}
									});
								});
							}
						}
						if (join != null) {
							join.setText("View");
							final String joinGid = gid;
							join.setOnClickListener(v -> {
								if (joinGid.isEmpty()) { _openTab(new DiscoverFragmentActivity(), R.id.menu_discover); return; }
								Intent ji = new Intent(getContext(), GroupJoinRequestActivity.class);
								ji.putExtra("groupId", joinGid);
								startActivity(ji);
							});
						}
						final String openGid = gid;
						item.setOnClickListener(v -> {
							if (openGid.isEmpty()) return;
							Intent ji = new Intent(getContext(), GroupJoinRequestActivity.class);
							ji.putExtra("groupId", openGid);
							startActivity(ji);
						});
						linear_trending_groups.addView(item);
					}
				});
			}
			
			@Override
			public void onError(String error) {
				if (!isAdded() || getActivity() == null) return;
				getActivity().runOnUiThread(() -> {
					linear_trending_groups.removeAllViews();
					if (linear10 != null) linear10.setVisibility(View.VISIBLE);
				});
			}
		});
	}
	
	private void _loadTotalSavedAcrossGroups() {
		if (text_val_saved == null) return;
		final String myUid = FirebaseAuth.getInstance().getCurrentUser() != null
		? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
		Map<String, Object> cachedTotal = com.harvana.app.offline.OfflineCacheManager.load(
		getContext(), "total_saved_" + myUid);
		if (cachedTotal != null && cachedTotal.get("harv") instanceof Number) {
			_applyTotalSaved(((Number) cachedTotal.get("harv")).doubleValue());
		}
		
		if (group_map_list.isEmpty()) {
			if (cachedTotal == null && getActivity() != null) getActivity().runOnUiThread(() -> {
				text_val_saved.setText(("0 " + com.harvana.app.CoinInfo.ticker()));
				if (text_sub_saved != null) text_sub_saved.setText("No group savings yet");
			});
			return;
		}
		final double[] total = {0};
		final int[] left = {group_map_list.size()};
		final boolean[] hadConnectivityFailure = {false};
		
		for (HashMap<String, Object> g : group_map_list) {
			String gid = g.get("id") != null ? g.get("id").toString() : "";
			if (gid.isEmpty() || getContext() == null) {
				left[0]--;
				if (left[0] <= 0) _finishTotalSaved(myUid, total[0], hadConnectivityFailure[0], cachedTotal);
				continue;
			}
			com.harvana.app.firestore.GroupFirestoreManager.getInstance(getContext())
			.getPayments(gid, new com.harvana.app.firestore.GroupFirestoreManager.ListCallback() {
				@Override
				public void onSuccess(List<Map<String, Object>> items) {
					if (items != null && !myUid.isEmpty()) {
						for (Map<String, Object> pay : items) {
							String st = str(pay, "status", "CONFIRMED");
							boolean ok = "CONFIRMED".equalsIgnoreCase(st)
							|| "PAID".equalsIgnoreCase(st)
							|| st.isEmpty();
							String uid = str(pay, "memberUid", str(pay, "uid", ""));
							if (!ok) continue;
							if (!myUid.equals(uid)) continue;
							double amt = num(pay, "amountHarv");
							if (amt <= 0) amt = num(pay, "amount");
							total[0] += amt;
						}
					}
					left[0]--;
					if (left[0] <= 0) _finishTotalSaved(myUid, total[0], hadConnectivityFailure[0], cachedTotal);
				}
				@Override
				public void onError(String error) {
					if (com.harvana.app.offline.NetworkStatus.isConnectivityError(error)) {
						hadConnectivityFailure[0] = true;
					}
					left[0]--;
					if (left[0] <= 0) _finishTotalSaved(myUid, total[0], hadConnectivityFailure[0], cachedTotal);
				}
			});
		}
	}
	
	private void _finishTotalSaved(String myUid, double total, boolean hadConnectivityFailure,
	Map<String, Object> cachedTotal) {
		if (hadConnectivityFailure) {
			if (cachedTotal == null) _applyTotalSaved(total);
			return;
		}
		Map<String, Object> toCache = new HashMap<>();
		toCache.put("harv", total);
		com.harvana.app.offline.OfflineCacheManager.save(getContext(), "total_saved_" + myUid, toCache);
		_applyTotalSaved(total);
	}
	
	private void _loadOpenLoansAcrossGroups() {
		if (text_val_loans == null) return;
		final String myUid = FirebaseAuth.getInstance().getCurrentUser() != null
		? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
		
		Map<String, Object> cachedLoans = com.harvana.app.offline.OfflineCacheManager.load(
		getContext(), "open_loans_" + myUid);
		if (cachedLoans != null && cachedLoans.get("count") instanceof Number) {
			int cachedCount = ((Number) cachedLoans.get("count")).intValue();
			double cachedOutstanding = cachedLoans.get("outstanding") instanceof Number
			? ((Number) cachedLoans.get("outstanding")).doubleValue() : 0;
			_applyOpenLoans(cachedCount, cachedOutstanding);
		}
		
		if (group_map_list.isEmpty()) {
			if (cachedLoans == null && getActivity() != null) getActivity().runOnUiThread(() -> {
				text_val_loans.setText("0");
				if (text_sub_loans != null) text_sub_loans.setText("No open loans");
			});
			return;
		}
		final int[] openCount = {0};
		final double[] outstandingTotal = {0};
		final int[] left = {group_map_list.size()};
		final boolean[] hadConnectivityFailure = {false};
		
		for (HashMap<String, Object> g : group_map_list) {
			String gid = g.get("id") != null ? g.get("id").toString() : "";
			if (gid.isEmpty() || getContext() == null) {
				left[0]--;
				if (left[0] <= 0) {
					_finishOpenLoans(myUid, openCount[0], outstandingTotal[0], hadConnectivityFailure[0], cachedLoans);
				}
				continue;
			}
			com.harvana.app.firestore.GroupFirestoreManager.getInstance(getContext())
			.getLoans(gid, new com.harvana.app.firestore.GroupFirestoreManager.ListCallback() {
				@Override
				public void onSuccess(List<Map<String, Object>> items) {
					if (items != null && !myUid.isEmpty()) {
						for (Map<String, Object> loan : items) {
							String st = str(loan, "status", "");
							String borrower = str(loan, "borrowerUid", "");
							if (!"ACTIVE".equalsIgnoreCase(st)) continue;
							if (!myUid.equals(borrower)) continue;
							openCount[0]++;
							double principal = num(loan, "principalHarv");
							if (principal <= 0) principal = num(loan, "amountHarv");
							double repaid = num(loan, "repaidAmountHarv");
							outstandingTotal[0] += Math.max(0, principal - repaid);
						}
					}
					left[0]--;
					if (left[0] <= 0) {
						_finishOpenLoans(myUid, openCount[0], outstandingTotal[0], hadConnectivityFailure[0], cachedLoans);
					}
				}
				@Override
				public void onError(String error) {
					if (com.harvana.app.offline.NetworkStatus.isConnectivityError(error)) {
						hadConnectivityFailure[0] = true;
					}
					left[0]--;
					if (left[0] <= 0) {
						_finishOpenLoans(myUid, openCount[0], outstandingTotal[0], hadConnectivityFailure[0], cachedLoans);
					}
				}
			});
		}
	}
	
	private void _finishOpenLoans(String myUid, int count, double outstanding,
	boolean hadConnectivityFailure, Map<String, Object> cachedLoans) {
		if (hadConnectivityFailure) {
			if (cachedLoans == null) _applyOpenLoans(count, outstanding);
			return;
		}
		Map<String, Object> toCache = new HashMap<>();
		toCache.put("count", count);
		toCache.put("outstanding", outstanding);
		com.harvana.app.offline.OfflineCacheManager.save(getContext(), "open_loans_" + myUid, toCache);
		_applyOpenLoans(count, outstanding);
	}
	
	private double fsSaved = 0, nodeSaved = 0, fsLoanOut = 0, nodeLoanOut = 0;
	private int fsLoanCount = 0, nodeLoanCount = 0, nodeGroupCount = 0;
	
	private void _applyOpenLoans(int count, double outstandingHarv) {
		fsLoanCount = count;
		fsLoanOut = outstandingHarv;
		_renderLoans();
	}
	
	private void _renderLoans() {
		if (!isAdded() || getActivity() == null || text_val_loans == null) return;
		final int count = fsLoanCount + nodeLoanCount;
		final double outstandingHarv = fsLoanOut + nodeLoanOut;
		getActivity().runOnUiThread(() -> {
			text_val_loans.setText(String.valueOf(count));
			if (text_sub_loans != null) {
				text_sub_loans.setText(count == 0 ? "No open loans"
				: _fmtHarv(outstandingHarv) + " outstanding");
			}
		});
	}
	
	private void _loadComputedTrust() {
		if (getContext() == null) return;
		String address = com.harvana.app.wallet.WalletManager.getActiveWalletAddress(getContext());
		com.harvana.app.trust.NetworkTrust.wallet(address, score -> {
			if (!isAdded() || getActivity() == null) return;
			getActivity().runOnUiThread(() -> {
				boolean pct = score.percent >= 0;
				if (text_val_trust != null) text_val_trust.setText(pct ? score.percent + "%" : score.published ? String.valueOf(score.score) : "\u2014");
				if (ring_trust_score != null) {
					int max = pct ? 100 : com.harvana.app.trust.NetworkTrust.MAX;
					ring_trust_score.setMax(max);
					ring_trust_score.setProgress(Math.max(0, Math.min(max, pct ? score.percent : score.score)));
				}
				if (text_sub_trust != null) text_sub_trust.setText(pct ? (score.tier.isEmpty() ? "trust score" : score.tier) : (score.summary.isEmpty() ? "of 26" : score.summary));
			});
		});
	}
	
	private void _loadNodeTotals() {
		if (getContext() == null || FirebaseAuth.getInstance().getCurrentUser() == null) return;
		String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
		String wallet = WalletManager.getActiveWalletAddress(getContext());
		com.harvana.app.sync.NodePortfolio.load(getContext(), uid, wallet, p -> {
			nodeSaved = p.savedHarv();
			nodeLoanOut = p.outstandingHarv();
			nodeLoanCount = p.loansOpen();
			nodeGroupCount = p.entries.size();
			_applyTotalSaved(fsSaved);
			_renderLoans();
		});
	}
	
	private void _applyTotalSaved(double harv) {
		fsSaved = harv;
		if (!isAdded() || getActivity() == null || text_val_saved == null) return;
		getActivity().runOnUiThread(() -> {
			text_val_saved.setText(_fmtHarv(fsSaved + nodeSaved));
			if (text_sub_saved != null) {
				int n = group_map_list.size();
				text_sub_saved.setText(n <= 0 ? "No group savings yet"
				: ("Across " + n + (n == 1 ? " group" : " groups")));
			}
		});
	}
	
	private static String _fmtAmount(double v) {
		try {
			java.text.DecimalFormat df = new java.text.DecimalFormat("#,##0.00######", new java.text.DecimalFormatSymbols(java.util.Locale.US));
			return df.format(v);
		} catch (Exception e) {
			return String.valueOf(v);
		}
	}

	private static String _fmtHarv(double v) {
		try {
			java.text.DecimalFormat df = new java.text.DecimalFormat("0.########", new java.text.DecimalFormatSymbols(java.util.Locale.US));
			return df.format(v) + (" " + com.harvana.app.CoinInfo.ticker());
		} catch (Exception e) {
			return String.valueOf(v) + (" " + com.harvana.app.CoinInfo.ticker());
		}
	}
	
	private String _greeting() {
		int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
		if (h < 12) return "Good morning";
		if (h < 17) return "Good afternoon";
		return "Good evening";
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
	
	private void _addQuickActions(View root) {
		try {
			final MaterialCardView card = root.findViewById(R.id.cardview4);
			final View top = root.findViewById(R.id.linear30);
			if (card == null || top == null || getContext() == null) return;
			final float d = getResources().getDisplayMetrics().density;
			
			android.widget.LinearLayout row = new android.widget.LinearLayout(getContext());
			row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
			row.setPadding((int) (10 * d), 0, (int) (10 * d), (int) (10 * d));
			row.addView(_quickAction(R.drawable.ic_action_send, "Send", SendAssetActivity.class), _quickActionParams(d, false));
			row.addView(_quickAction(R.drawable.ic_action_receive, "Receive", ReceiveActivity.class), _quickActionParams(d, true));
			row.addView(_quickAction(R.drawable.ic_action_buy, "P2P", P2pMarketplaceActivity.class), _quickActionParams(d, true));
			
			card.removeView(top);
			android.widget.LinearLayout box = new android.widget.LinearLayout(getContext());
			box.setOrientation(android.widget.LinearLayout.VERTICAL);
			box.addView(top);
			box.addView(row);
			card.addView(box, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		} catch (Exception e) {
			android.util.Log.w("HomeFragment", "Quick actions not added", e);
		}
	}
	
	private android.widget.LinearLayout.LayoutParams _quickActionParams(float d, boolean leftGap) {
		android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
		if (leftGap) lp.leftMargin = (int) (10 * d);
		return lp;
	}
	
	private View _quickAction(int iconRes, String label, final Class<?> target) {
		final float d = getResources().getDisplayMetrics().density;
		android.widget.LinearLayout cell = new android.widget.LinearLayout(getContext());
		cell.setOrientation(android.widget.LinearLayout.VERTICAL);
		cell.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
		cell.setPadding(0, (int) (4 * d), 0, (int) (4 * d));
		cell.setBackgroundResource(R.drawable.bg_quick_action_hero);
		cell.setClickable(true);
		cell.setFocusable(true);
		cell.setMinimumHeight((int) (36 * d));
		
		android.widget.ImageView icon = new android.widget.ImageView(getContext());
		icon.setImageResource(iconRes);
		icon.setColorFilter(androidx.core.content.ContextCompat.getColor(getContext(), R.color.colorTextOnAccent));
		icon.setBackgroundResource(R.drawable.bg_quick_action_icon);
		int pad = (int) (4 * d);
		icon.setPadding(pad, pad, pad, pad);
		android.widget.LinearLayout.LayoutParams ilp = new android.widget.LinearLayout.LayoutParams((int) (20 * d), (int) (20 * d));
		ilp.gravity = android.view.Gravity.CENTER_HORIZONTAL;
		cell.addView(icon, ilp);
		
		TextView tv = new TextView(getContext());
		tv.setText(label);
		tv.setTextSize(10f);
		tv.setTypeface(null, android.graphics.Typeface.BOLD);
		tv.setTextColor(androidx.core.content.ContextCompat.getColor(getContext(), R.color.colorTextPrimary));
		tv.setGravity(android.view.Gravity.CENTER);
		tv.setPadding(0, (int) (4 * d), 0, 0);
		cell.addView(tv, new android.widget.LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		
		cell.setContentDescription(label);
		cell.setOnClickListener(v -> startActivity(new Intent(getContext(), target)));
		return cell;
	}
}
