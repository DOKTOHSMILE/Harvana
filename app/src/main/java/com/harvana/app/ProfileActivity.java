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
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.biometric.*;
import androidx.cardview.widget.CardView;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.budiyev.android.codescanner.*;
import com.bumptech.glide.*;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.card.*;
import com.google.android.material.progressindicator.LinearProgressIndicator;
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
import com.bumptech.glide.Glide;


public class ProfileActivity extends AppCompatActivity {
	
	private FirebaseDatabase _firebase = FirebaseDatabase.getInstance();
	
	private Toolbar _toolbar;
	private AppBarLayout _app_bar;
	private CoordinatorLayout _coordinator;
	private String uid = "";
	
	private LinearLayout linear1;
	private MaterialCardView cardview1;
	private MaterialCardView cardview5;
	private MaterialCardView cardview13;
	private MaterialCardView cardview9;
	private MaterialCardView cardview14;
	private MaterialCardView cardview8;
	private LinearLayout linear2;
	private LinearLayout linear117;
	private LinearLayout linear121;
	private LinearLayout linear123;
	private CardView cardview20;
	private LinearLayout linear118;
	private ImageView user_photo_imageview1;
	private LinearLayout linear119;
	private TextView user_state_country_textview3;
	private TextView name_textview1;
	private LinearLayout linear120;
	private ImageView imageview11;
	private TextView tier_indicator_textview2;
	private LinearLayout linear122;
	private LinearProgressIndicator trustScoreProgress5;
	private TextView textview154;
	private TextView user_trust_score_textview5;
	private MaterialCardView cardview21;
	private MaterialCardView cardview22;
	private MaterialCardView cardview23;
	private LinearLayout linear124;
	private TextView textview155;
	private TextView textview6;
	private LinearLayout linear125;
	private TextView textview156;
	private TextView textview9;
	private LinearLayout linear126;
	private TextView textview157;
	private TextView textview11;
	private LinearLayout linear7;
	private TextView textview4;
	private LinearLayout linear8;
	private MaterialCardView cardview6;
	private MaterialCardView cardview7;
	private LinearLayout linear9;
	private TextView textview5;
	private TextView tv_total_saved;
	private TextView tv_total_saved_fiat;
	private LinearLayout linear10;
	private TextView textview7;
	private TextView tv_total_loans;
	private TextView tv_total_loans_fiat;
	private LinearLayout linear127;
	private TextView textview158;
	private LinearLayout linear128;
	private LinearProgressIndicator trustScoreProgress1;
	private LinearLayout linear129;
	private LinearProgressIndicator trustScoreProgress2;
	private LinearLayout linear130;
	private LinearProgressIndicator trustScoreProgress3;
	private LinearLayout linear131;
	private LinearProgressIndicator trustScoreProgress4;
	private TextView textview159;
	private TextView textview40;
	private TextView textview160;
	private TextView textview42;
	private TextView textview161;
	private TextView textview44;
	private TextView textview162;
	private TextView textview46;
	private LinearLayout linear132;
	private LinearLayout linear133;
	private LinearLayout linear135;
	private CardView cardview24;
	private LinearLayout linear134;
	private ImageView imageview25;
	private ImageView imageview24;
	private TextView textview163;
	private TextView textview164;
	private LinearLayout linear136;
	private LinearLayout linear137;
	private LinearLayout linear138;
	private ImageView imageview26;
	private TextView textview165;
	private ImageView imageview27;
	private TextView textview166;
	private ImageView imageview28;
	private TextView textview167;
	private LinearLayout linear139;
	private TextView textview168;
	private LinearLayout linear140;
	private View view141;
	private LinearLayout linear142;
	private View view143;
	private LinearLayout linear144;
	private View view145;
	private LinearLayout linear146;
	private View view147;
	private LinearLayout linear151;
	private View linear152;
	private LinearLayout linear148;
	private TextView textview169;
	private TextView tv_phone;
	private ImageView imageview29;
	private TextView textview170;
	private TextView tv_email;
	private ImageView imageview30;
	private TextView textview171;
	private TextView tv_occupation;
	private TextView textview172;
	private TextView tv_address;
	private TextView textview176;
	private TextView tv_dob;
	private TextView textview173;
	private TextView tier_textview57;
	private LinearLayout linear149;
	private LinearLayout linear150;
	private ListView listview1;
	private LinearLayout linear14;
	private CardView cardview3;
	private TextView textview174;
	private ImageView imageview31;
	private ImageView imageview32;
	private TextView textview175;
	
	private SharedPreferences authsp;
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
	private DatabaseReference userdb = _firebase.getReference("userdb");
	private ChildEventListener _userdb_child_listener;
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.profile);
		initialize(_savedInstanceState);
		FirebaseApp.initializeApp(this);
		initializeLogic();
		addSocialShortcuts();
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
		linear1 = findViewById(R.id.linear1);
		cardview1 = findViewById(R.id.cardview1);
		cardview5 = findViewById(R.id.cardview5);
		cardview13 = findViewById(R.id.cardview13);
		cardview9 = findViewById(R.id.cardview9);
		cardview14 = findViewById(R.id.cardview14);
		cardview8 = findViewById(R.id.cardview8);
		linear2 = findViewById(R.id.linear2);
		linear117 = findViewById(R.id.linear117);
		linear121 = findViewById(R.id.linear121);
		linear123 = findViewById(R.id.linear123);
		cardview20 = findViewById(R.id.cardview20);
		linear118 = findViewById(R.id.linear118);
		user_photo_imageview1 = findViewById(R.id.user_photo_imageview1);
		linear119 = findViewById(R.id.linear119);
		user_state_country_textview3 = findViewById(R.id.user_state_country_textview3);
		name_textview1 = findViewById(R.id.name_textview1);
		linear120 = findViewById(R.id.linear120);
		imageview11 = findViewById(R.id.imageview11);
		tier_indicator_textview2 = findViewById(R.id.tier_indicator_textview2);
		linear122 = findViewById(R.id.linear122);
		trustScoreProgress5 = findViewById(R.id.trustScoreProgress5);
		textview154 = findViewById(R.id.textview154);
		user_trust_score_textview5 = findViewById(R.id.user_trust_score_textview5);
		cardview21 = findViewById(R.id.cardview21);
		cardview22 = findViewById(R.id.cardview22);
		cardview23 = findViewById(R.id.cardview23);
		linear124 = findViewById(R.id.linear124);
		textview155 = findViewById(R.id.textview155);
		textview6 = findViewById(R.id.textview6);
		linear125 = findViewById(R.id.linear125);
		textview156 = findViewById(R.id.textview156);
		textview9 = findViewById(R.id.textview9);
		linear126 = findViewById(R.id.linear126);
		textview157 = findViewById(R.id.textview157);
		textview11 = findViewById(R.id.textview11);
		linear7 = findViewById(R.id.linear7);
		textview4 = findViewById(R.id.textview4);
		linear8 = findViewById(R.id.linear8);
		cardview6 = findViewById(R.id.cardview6);
		cardview7 = findViewById(R.id.cardview7);
		linear9 = findViewById(R.id.linear9);
		textview5 = findViewById(R.id.textview5);
		tv_total_saved = findViewById(R.id.tv_total_saved);
		tv_total_saved_fiat = findViewById(R.id.tv_total_saved_fiat);
		linear10 = findViewById(R.id.linear10);
		textview7 = findViewById(R.id.textview7);
		tv_total_loans = findViewById(R.id.tv_total_loans);
		tv_total_loans_fiat = findViewById(R.id.tv_total_loans_fiat);
		linear127 = findViewById(R.id.linear127);
		textview158 = findViewById(R.id.textview158);
		linear128 = findViewById(R.id.linear128);
		trustScoreProgress1 = findViewById(R.id.trustScoreProgress1);
		linear129 = findViewById(R.id.linear129);
		trustScoreProgress2 = findViewById(R.id.trustScoreProgress2);
		linear130 = findViewById(R.id.linear130);
		trustScoreProgress3 = findViewById(R.id.trustScoreProgress3);
		linear131 = findViewById(R.id.linear131);
		trustScoreProgress4 = findViewById(R.id.trustScoreProgress4);
		textview159 = findViewById(R.id.textview159);
		textview40 = findViewById(R.id.textview40);
		textview160 = findViewById(R.id.textview160);
		textview42 = findViewById(R.id.textview42);
		textview161 = findViewById(R.id.textview161);
		textview44 = findViewById(R.id.textview44);
		textview162 = findViewById(R.id.textview162);
		textview46 = findViewById(R.id.textview46);
		linear132 = findViewById(R.id.linear132);
		linear133 = findViewById(R.id.linear133);
		linear135 = findViewById(R.id.linear135);
		cardview24 = findViewById(R.id.cardview24);
		linear134 = findViewById(R.id.linear134);
		imageview25 = findViewById(R.id.imageview25);
		imageview24 = findViewById(R.id.imageview24);
		textview163 = findViewById(R.id.textview163);
		textview164 = findViewById(R.id.textview164);
		linear136 = findViewById(R.id.linear136);
		linear137 = findViewById(R.id.linear137);
		linear138 = findViewById(R.id.linear138);
		imageview26 = findViewById(R.id.imageview26);
		textview165 = findViewById(R.id.textview165);
		imageview27 = findViewById(R.id.imageview27);
		textview166 = findViewById(R.id.textview166);
		imageview28 = findViewById(R.id.imageview28);
		textview167 = findViewById(R.id.textview167);
		linear139 = findViewById(R.id.linear139);
		textview168 = findViewById(R.id.textview168);
		linear140 = findViewById(R.id.linear140);
		view141 = findViewById(R.id.view141);
		linear142 = findViewById(R.id.linear142);
		view143 = findViewById(R.id.view143);
		linear144 = findViewById(R.id.linear144);
		view145 = findViewById(R.id.view145);
		linear146 = findViewById(R.id.linear146);
		view147 = findViewById(R.id.view147);
		linear151 = findViewById(R.id.linear151);
		linear152 = findViewById(R.id.linear152);
		linear148 = findViewById(R.id.linear148);
		textview169 = findViewById(R.id.textview169);
		tv_phone = findViewById(R.id.tv_phone);
		imageview29 = findViewById(R.id.imageview29);
		textview170 = findViewById(R.id.textview170);
		tv_email = findViewById(R.id.tv_email);
		imageview30 = findViewById(R.id.imageview30);
		textview171 = findViewById(R.id.textview171);
		tv_occupation = findViewById(R.id.tv_occupation);
		textview172 = findViewById(R.id.textview172);
		tv_address = findViewById(R.id.tv_address);
		textview176 = findViewById(R.id.textview176);
		tv_dob = findViewById(R.id.tv_dob);
		textview173 = findViewById(R.id.textview173);
		tier_textview57 = findViewById(R.id.tier_textview57);
		linear149 = findViewById(R.id.linear149);
		linear150 = findViewById(R.id.linear150);
		listview1 = findViewById(R.id.listview1);
		linear14 = findViewById(R.id.linear14);
		cardview3 = findViewById(R.id.cardview3);
		textview174 = findViewById(R.id.textview174);
		imageview31 = findViewById(R.id.imageview31);
		imageview32 = findViewById(R.id.imageview32);
		textview175 = findViewById(R.id.textview175);
		authsp = getSharedPreferences("authsp", Activity.MODE_PRIVATE);
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
		androidx.appcompat.app.ActionBar ab = getSupportActionBar();
		int backgroundColor = androidx.core.content.ContextCompat.getColor(this, R.color.colorBackground);
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			final Window window = this.getWindow();
			window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
			window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
			com.harvana.app.utils.StatusBarUtil.matchToolbar(this);
		}
		
		uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
		
		// WhatsApp-style: show the last known profile instantly, before
		// either network call below completes.
		Map<String, Object> cached = com.harvana.app.offline.OfflineCacheManager.load(getApplicationContext(), "profile_" + uid);
		if (cached != null) _bindProfileFields(cached);
		
		_loadProfile();
		_loadTrustScore();
		
	}
	
	public void _MORE() {
	}
	private void _loadProfile() {
		com.harvana.app.firestore.UserProfileManager.getInstance(getApplicationContext())
		.getProfile(uid, new com.harvana.app.firestore.UserProfileManager.DocCallback() {
			@Override
			public void onSuccess(Map<String, Object> data) {
				if (data == null || data.isEmpty()) return;
				com.harvana.app.offline.OfflineCacheManager.save(getApplicationContext(), "profile_" + uid, data);
				com.harvana.app.identity.IdentityStore.ensure(uid, data, (pid, error) -> {
					if (pid != null && (data.get("pid") == null || !pid.equals(String.valueOf(data.get("pid"))))) {
						data.put("pid", pid);
						com.harvana.app.offline.OfflineCacheManager.save(getApplicationContext(), "profile_" + uid, data);
						runOnUiThread(() -> _bindProfileFields(data));
					}
				});
				runOnUiThread(() -> _bindProfileFields(data));
			}
			
			@Override
			public void onError(String error) {
				// Cache-first render already happened in initializeLogic() —
				// on a genuine connectivity failure there's already
				// something correct on screen, so don't cover it with an
				// error toast. Only surface a real (non-connectivity)
				// failure.
				if (com.harvana.app.offline.NetworkStatus.isConnectivityError(error)) return;
				runOnUiThread(() -> SketchwareUtil.showMessage(getApplicationContext(), "Failed to load profile: " + error));
			}
		});
	}
	
	private void _bindProfileFields(Map<String, Object> data) {
		String iconStr = _str(data, "icon");
		String firstNameStr = _str(data, "first_name");
		String lastNameStr = _str(data, "last_name");
		
		if (iconStr.isEmpty() || "null".equalsIgnoreCase(iconStr)) {
			_createInitialAvatar(firstNameStr, lastNameStr, user_photo_imageview1);
		} else {
			Glide.with(getApplicationContext()).load(Uri.parse(iconStr)).into(user_photo_imageview1);
		}
		
		name_textview1.setText((firstNameStr + " " + lastNameStr).trim());
		
		user_trust_score_textview5.setText("— / 26");
		trustScoreProgress5.setMax(26);
		trustScoreProgress5.setProgress(0);
		
		long timestamp = (long) _num(data, "timestamp");
		String joined = "";
		if (timestamp > 0) {
			Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(timestamp);
			joined = new SimpleDateFormat("MMM dd yyyy", Locale.US).format(cal.getTime());
		}
		
		String stateStr = _str(data, "state");
		String countryStr = _str(data, "country");
		String pid = _str(data, "pid");
		String place = stateStr + ", " + countryStr + (joined.isEmpty() ? "" : " • Joined " + joined);
		user_state_country_textview3.setText(pid.isEmpty() ? place : "ID " + pid + " • " + place);
		
		String tierStr = _str(data, "tier");
		tier_indicator_textview2.setText("Tier " + tierStr);
		imageview11.setColorFilter(0xFF2196F3, PorterDuff.Mode.MULTIPLY);
		
		String vTypeStr = _str(data, "v_type").toUpperCase(Locale.US);
		tier_textview57.setText("Tier " + tierStr + " • " + vTypeStr + " verified");
		
		tv_phone.setText(_str(data, "phone"));
		tv_email.setText(_str(data, "email"));
		tv_occupation.setText(_str(data, "occupation"));
		
		String addressStr = _str(data, "address");
		String cityStr = _str(data, "city");
		tv_address.setText(addressStr + ", " + cityStr + ", " + stateStr + ", " + countryStr);
		
		tv_dob.setText(_str(data, "dob"));
	}
	
	private TextView networkTrustView;
	private com.google.firebase.firestore.ListenerRegistration reputationReg;
	
	/** The score on screen is the document the node wrote. This activity does not calculate it. */
	private void _attachNetworkTrust() {
		try {
			final String addr = com.harvana.app.wallet.WalletManager.getActiveWalletAddress(this);
			if (trustScoreProgress5 == null || !(trustScoreProgress5.getParent() instanceof ViewGroup)) return;
			ViewGroup host = (ViewGroup) trustScoreProgress5.getParent();
			if (networkTrustView == null) {
				networkTrustView = new TextView(this);
				networkTrustView.setTextSize(12);
				networkTrustView.setTextColor(com.harvana.app.utils.UiColors.textSecondary());
				networkTrustView.setPadding(0, (int) (10 * getResources().getDisplayMetrics().density), 0, 0);
				host.addView(networkTrustView);
			}
			if (reputationReg != null) { reputationReg.remove(); reputationReg = null; }
			if (addr == null || addr.isEmpty()) { networkTrustView.setText("Network trust: create a wallet to build a trust record on the network."); return; }
			networkTrustView.setText("Network trust: waiting for Firestore…");
			com.harvana.app.sync.NodeLinkManager.ensureLinked(this, status -> { });
			String status = com.harvana.app.sync.NodeLinkManager.cachedStatus(this, addr);
			reputationReg = com.harvana.app.firestore.NodeMirrors.wallet(addr, doc -> runOnUiThread(() -> {
				if (networkTrustView == null) return;
				if (doc == null) {
					networkTrustView.setText("Network trust: not published yet." + (status == null || status.isEmpty() ? "" : "\n" + status));
					return;
				}
				Object summary = doc.get("summary");
				String line = summary == null ? "Network trust published" : "Network trust: " + summary;
				Object actions = doc.get("activityCount");
				if (actions instanceof Number) {
					int n = ((Number) actions).intValue();
					line += "\nActivity: " + n + (n == 1 ? " action confirmed on the network" : " actions confirmed on the network");
				}
				if (status != null && !status.isEmpty()) line += "\n" + status;
				final String networkLine = line;
				com.harvana.app.identity.CombinedIdentity.load(account -> runOnUiThread(() -> {
					if (networkTrustView == null) return;
					String who = account.name.isEmpty() ? "Signed-in account" : account.name;
					networkTrustView.setText(who + "\n" + networkLine);
				}));
			}));
		} catch (Exception ignored) { }
	}
	
	private void _loadTrustScore() {
		_attachNetworkTrust();
		// Offline-first: render the last computed trust score/tier/totals
		// immediately — this whole card (ring, 4 sub-metrics, group/cycle
		// counts, Total Saved, Total Loans) previously had zero fallback
		// and just stayed blank on a cold offline open.
		Map<String, Object> cachedScore = com.harvana.app.offline.OfflineCacheManager.load(
		getApplicationContext(), "trust_score_" + uid);
		if (cachedScore != null) _bindTrustScoreFields(cachedScore);
		
		com.harvana.app.trust.NetworkTrust.wallet(
		com.harvana.app.wallet.WalletManager.getActiveWalletAddress(this), score -> runOnUiThread(() -> {
			Map<String, Object> toCache = new HashMap<>();
			toCache.put("network", true);
			toCache.put("overallScore", score.score);
			toCache.put("published", score.published);
			toCache.put("percent", score.percent);
			toCache.put("tier", score.tier);
			toCache.put("bound", score.bound);
			toCache.put("vouches", score.vouches);
			toCache.put("badges", score.badges);
			toCache.put("established", score.established);
			toCache.put("summary", score.summary);
			com.harvana.app.offline.OfflineCacheManager.save(getApplicationContext(), "trust_score_" + uid, toCache);
			_bindTrustScoreFields(toCache);
		}));
	}
	
	private void _bindTrustScoreFields(Map<String, Object> r) {
		if (!Boolean.TRUE.equals(r.get("network"))) return;
		int overallScore = r.get("overallScore") instanceof Number ? ((Number) r.get("overallScore")).intValue() : 0;
		boolean published = Boolean.TRUE.equals(r.get("published"));
		int percent = r.get("percent") instanceof Number ? ((Number) r.get("percent")).intValue() : -1;
		if (published && percent >= 0) {
			// The node's single trust score, as a percentage (the same number every profile shows).
			Object tier = r.get("tier");
			user_trust_score_textview5.setText(percent + "%" + (tier == null || String.valueOf(tier).isEmpty() ? "" : " · " + tier));
			trustScoreProgress5.setMax(100);
			trustScoreProgress5.setProgress(Math.max(0, Math.min(100, percent)));
		} else {
			user_trust_score_textview5.setText(published ? overallScore + " / 26" : "— / 26");
			trustScoreProgress5.setMax(26);
			trustScoreProgress5.setProgress(published ? Math.max(0, Math.min(26, overallScore)) : 0);
		}

		boolean bound = Boolean.TRUE.equals(r.get("bound"));
		int vouches = r.get("vouches") instanceof Number ? ((Number) r.get("vouches")).intValue() : 0;
		int vouchPoints = Math.min(vouches, 8) * 2;
		int badges = r.get("badges") instanceof Number ? ((Number) r.get("badges")).intValue() : 0;
		boolean established = Boolean.TRUE.equals(r.get("established"));
		TextView device = findViewById(R.id.textview159);
		TextView vouchLabel = findViewById(R.id.textview160);
		TextView badgeLabel = findViewById(R.id.textview161);
		TextView establishedLabel = findViewById(R.id.textview162);
		if (device != null) device.setText("Device");
		if (vouchLabel != null) vouchLabel.setText("Vouches");
		if (badgeLabel != null) badgeLabel.setText("Badges");
		if (establishedLabel != null) establishedLabel.setText("Established");
		textview40.setText(bound ? "10" : "0");
		trustScoreProgress1.setMax(10);
		trustScoreProgress1.setProgress(bound ? 10 : 0);
		textview42.setText(String.valueOf(vouchPoints));
		trustScoreProgress2.setMax(16);
		trustScoreProgress2.setProgress(vouchPoints);
		textview44.setText(String.valueOf(badges));
		final android.widget.LinearLayout badgeRow = findViewById(R.id.profileBadgeRow);
		if (badgeRow != null) {
			String me = com.harvana.app.wallet.WalletManager.getActiveWalletAddress(this);
			com.harvana.app.trust.NodeBadges.load(me, list -> {
				com.harvana.app.trust.BadgeChips.fill(badgeRow, list, false);
				if (!list.isEmpty()) textview44.setText(String.valueOf(list.size()));
			});
		}
		trustScoreProgress3.setMax(5);
		trustScoreProgress3.setProgress(Math.min(5, badges));
		textview46.setText(established ? "Yes" : "No");
		trustScoreProgress4.setMax(1);
		trustScoreProgress4.setProgress(established ? 1 : 0);
	}

	public void _createInitialAvatar(final String _first, final String _last, final ImageView _img) {
		String initials = "";
		if (_first != null && !_first.trim().isEmpty()) {
			initials += _first.trim().substring(0, 1).toUpperCase(Locale.US);
		}
		if (_last != null && !_last.trim().isEmpty()) {
			initials += _last.trim().substring(0, 1).toUpperCase(Locale.US);
		}
		if (initials.isEmpty()) initials = "?";
		
		int[] materialColors = {
			0xFF1ABC9C, 0xFF2ECC71, 0xFF3498DB, 0xFF9B59B6, 0xFF34495E,
			0xFF16A085, 0xFF27AE60, 0xFF2980B9, 0xFF8E44AD, 0xFF2C3E50,
			0xFFE67E22, 0xFFE74C3C, 0xFFD35400, 0xFFC0392B, 0xFF7F8C8D
		};
		int combinedHash = (_first != null ? _first.hashCode() : 0) + (_last != null ? _last.hashCode() : 0);
		int assignedColor = materialColors[Math.abs(combinedHash) % materialColors.length];
		
		Bitmap bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		
		Paint bgPaint = new Paint();
		bgPaint.setColor(assignedColor);
		bgPaint.setAntiAlias(true);
		canvas.drawCircle(100, 100, 100, bgPaint);
		
		Paint textPaint = new Paint();
		textPaint.setColor(Color.WHITE);
		textPaint.setAntiAlias(true);
		textPaint.setTextAlign(Paint.Align.CENTER);
		textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
		textPaint.setTextSize(initials.length() > 1 ? 75 : 90);
		
		float yBaselinePosition = (canvas.getHeight() / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f);
		canvas.drawText(initials, 100, yBaselinePosition, textPaint);
		_img.setImageBitmap(bitmap);
	}
	
	private static String _str(Map<String, Object> m, String k) {
		Object v = m.get(k);
		if (v == null) return "";
		String s = v.toString().trim();
		return "null".equalsIgnoreCase(s) ? "" : s;
	}
	
	private static double _num(Map<String, Object> m, String k) {
		Object v = m.get(k);
		if (v instanceof Number) return ((Number) v).doubleValue();
		try { return Double.parseDouble(String.valueOf(v)); } catch (Exception e) { return 0; }
	}
	{
	}
	

	/** Friends sit under the name, not at the bottom of the profile. */
	private void addSocialShortcuts() {
		if (linear2 == null) return;
		float d = getResources().getDisplayMetrics().density;
		android.widget.LinearLayout row = new android.widget.LinearLayout(this);
		row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
		row.setGravity(android.view.Gravity.CENTER_VERTICAL);
		row.setPadding(0, (int) (16 * d), 0, 0);
		android.widget.LinearLayout text = new android.widget.LinearLayout(this);
		text.setOrientation(android.widget.LinearLayout.VERTICAL);
		android.widget.TextView title = new android.widget.TextView(this);
		title.setText("Friends");
		title.setTextSize(15);
		title.setTypeface(null, android.graphics.Typeface.BOLD);
		title.setTextColor(com.harvana.app.utils.UiColors.textPrimary());
		final android.widget.TextView sub = new android.widget.TextView(this);
		sub.setText("People you follow each other with");
		sub.setTextSize(12);
		sub.setTextColor(com.harvana.app.utils.UiColors.textSecondary());
		text.addView(title);
		text.addView(sub);
		row.addView(text, new android.widget.LinearLayout.LayoutParams(0, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
		android.widget.TextView open = new android.widget.TextView(this);
		open.setText("See all");
		open.setTextSize(13);
		open.setTypeface(null, android.graphics.Typeface.BOLD);
		open.setTextColor(com.harvana.app.utils.UiColors.accent());
		row.addView(open);
		row.setOnClickListener(v -> startActivity(new android.content.Intent(this, com.harvana.app.social.FriendsActivity.class)));
		linear2.addView(row, 1);
		com.harvana.app.social.SocialStore.following((following, e1) ->
			com.harvana.app.social.SocialStore.followers((followers, e2) -> runOnUiThread(() -> {
				java.util.Set<String> back = new java.util.HashSet<>();
				if (followers != null) for (com.harvana.app.social.Person p : followers) back.add(p.uid);
				java.util.List<String> names = new java.util.ArrayList<>();
				if (following != null) for (com.harvana.app.social.Person p : following) {
					if (back.contains(p.uid)) names.add(p.name);
				}
				if (names.isEmpty()) sub.setText("No friends yet. Find someone by username.");
				else {
					String shown = names.size() == 1 ? names.get(0)
						: names.get(0) + (names.size() == 2 ? " and " + names.get(1) : " and " + (names.size() - 1) + " others");
					sub.setText(names.size() + (names.size() == 1 ? " friend · " : " friends · ") + shown);
				}
			})));
	}
}
