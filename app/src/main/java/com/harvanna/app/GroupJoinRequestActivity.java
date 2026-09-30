package com.harvanna.app;

import com.harvanna.app.utils.UiColors;
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
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.biometric.*;
import androidx.cardview.widget.CardView;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.budiyev.android.codescanner.*;
import com.bumptech.glide.*;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.button.*;
import com.google.android.material.card.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;
import org.json.*;



public class GroupJoinRequestActivity extends AppCompatActivity {
	
	private Toolbar _toolbar;
	private AppBarLayout _app_bar;
	private CoordinatorLayout _coordinator;
	
	private NestedScrollView vscroll_join_request;
	private LinearLayout linear89;
	private LinearLayout linear65;
	private MaterialCardView cardview33;
	private TextView textview43;
	private MaterialCardView cardview36;
	private TextView textview44;
	private MaterialCardView cardview37;
	private TextView textview49;
	private MaterialCardView cardview38;
	private TextView textview54;
	private MaterialCardView cardview39;
	private TextView textview61;
	private MaterialCardView cardview40;
	private TextView tv_message_char_count;
	private MaterialCardView cardview41;
	private TextView textview63;
	private LinearLayout linear66;
	private LinearLayout linear67;
	private LinearLayout linear70;
	private CardView cardview34;
	private LinearLayout linear68;
	private ImageView img_group_avatar;
	private TextView tv_group_name;
	private LinearLayout linear69;
	private ImageView imageview17;
	private TextView tv_group_location;
	private MaterialCardView cardview35;
	private MaterialCardView card_health_badge;
	private TextView tv_group_type;
	private LinearLayout linear71;
	private View view72;
	private TextView tv_group_health;
	private TextView tv_group_description;
	private LinearLayout linear73;
	private LinearLayout linear74;
	private View view75;
	private LinearLayout linear76;
	private View view77;
	private LinearLayout linear78;
	private View view79;
	private LinearLayout linear80;
	private TextView textview45;
	private TextView tv_join_admin_value;
	private TextView textview46;
	private TextView tv_join_members_value;
	private TextView textview47;
	private TextView tv_join_created_value;
	private TextView textview48;
	private TextView tv_join_cycle_value;
	private LinearLayout layout_group_rules;
	private LinearLayout linear81;
	private LinearLayout linear82;
	private LinearLayout linear83;
	private LinearLayout linear84;
	private ImageView imageview18;
	private TextView textview50;
	private ImageView imageview19;
	private TextView textview51;
	private ImageView imageview20;
	private TextView textview52;
	private ImageView imageview21;
	private TextView textview53;
	private LinearLayout layout_group_history;
	private LinearLayout linear85;
	private LinearLayout linear86;
	private LinearLayout linear87;
	private ImageView imageview22;
	private TextView textview55;
	private TextView textview56;
	private ImageView imageview23;
	private TextView textview57;
	private TextView textview58;
	private ImageView imageview24;
	private TextView textview59;
	private TextView textview60;
	private EditText et_message_to_admin;
	private LinearLayout linear88;
	private CheckBox cb_agree_to_rules;
	private TextView textview62;
	private MaterialButton btn_cancel_join_request;
	private MaterialButton btn_send_join_request;
	
	private SharedPreferences groupsp;
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
		setContentView(R.layout.group_join_request);
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
		vscroll_join_request = findViewById(R.id.vscroll_join_request);
		linear89 = findViewById(R.id.linear89);
		linear65 = findViewById(R.id.linear65);
		cardview33 = findViewById(R.id.cardview33);
		textview43 = findViewById(R.id.textview43);
		cardview36 = findViewById(R.id.cardview36);
		textview44 = findViewById(R.id.textview44);
		cardview37 = findViewById(R.id.cardview37);
		textview49 = findViewById(R.id.textview49);
		cardview38 = findViewById(R.id.cardview38);
		textview54 = findViewById(R.id.textview54);
		cardview39 = findViewById(R.id.cardview39);
		textview61 = findViewById(R.id.textview61);
		cardview40 = findViewById(R.id.cardview40);
		tv_message_char_count = findViewById(R.id.tv_message_char_count);
		cardview41 = findViewById(R.id.cardview41);
		textview63 = findViewById(R.id.textview63);
		linear66 = findViewById(R.id.linear66);
		linear67 = findViewById(R.id.linear67);
		linear70 = findViewById(R.id.linear70);
		cardview34 = findViewById(R.id.cardview34);
		linear68 = findViewById(R.id.linear68);
		img_group_avatar = findViewById(R.id.img_group_avatar);
		tv_group_name = findViewById(R.id.tv_group_name);
		linear69 = findViewById(R.id.linear69);
		imageview17 = findViewById(R.id.imageview17);
		tv_group_location = findViewById(R.id.tv_group_location);
		cardview35 = findViewById(R.id.cardview35);
		card_health_badge = findViewById(R.id.card_health_badge);
		tv_group_type = findViewById(R.id.tv_group_type);
		linear71 = findViewById(R.id.linear71);
		view72 = findViewById(R.id.view72);
		tv_group_health = findViewById(R.id.tv_group_health);
		tv_group_description = findViewById(R.id.tv_group_description);
		linear73 = findViewById(R.id.linear73);
		linear74 = findViewById(R.id.linear74);
		view75 = findViewById(R.id.view75);
		linear76 = findViewById(R.id.linear76);
		view77 = findViewById(R.id.view77);
		linear78 = findViewById(R.id.linear78);
		view79 = findViewById(R.id.view79);
		linear80 = findViewById(R.id.linear80);
		textview45 = findViewById(R.id.textview45);
		tv_join_admin_value = findViewById(R.id.tv_join_admin_value);
		textview46 = findViewById(R.id.textview46);
		tv_join_members_value = findViewById(R.id.tv_join_members_value);
		textview47 = findViewById(R.id.textview47);
		tv_join_created_value = findViewById(R.id.tv_join_created_value);
		textview48 = findViewById(R.id.textview48);
		tv_join_cycle_value = findViewById(R.id.tv_join_cycle_value);
		layout_group_rules = findViewById(R.id.layout_group_rules);
		linear81 = findViewById(R.id.linear81);
		linear82 = findViewById(R.id.linear82);
		linear83 = findViewById(R.id.linear83);
		linear84 = findViewById(R.id.linear84);
		imageview18 = findViewById(R.id.imageview18);
		textview50 = findViewById(R.id.textview50);
		imageview19 = findViewById(R.id.imageview19);
		textview51 = findViewById(R.id.textview51);
		imageview20 = findViewById(R.id.imageview20);
		textview52 = findViewById(R.id.textview52);
		imageview21 = findViewById(R.id.imageview21);
		textview53 = findViewById(R.id.textview53);
		layout_group_history = findViewById(R.id.layout_group_history);
		linear85 = findViewById(R.id.linear85);
		linear86 = findViewById(R.id.linear86);
		linear87 = findViewById(R.id.linear87);
		imageview22 = findViewById(R.id.imageview22);
		textview55 = findViewById(R.id.textview55);
		textview56 = findViewById(R.id.textview56);
		imageview23 = findViewById(R.id.imageview23);
		textview57 = findViewById(R.id.textview57);
		textview58 = findViewById(R.id.textview58);
		imageview24 = findViewById(R.id.imageview24);
		textview59 = findViewById(R.id.textview59);
		textview60 = findViewById(R.id.textview60);
		et_message_to_admin = findViewById(R.id.et_message_to_admin);
		linear88 = findViewById(R.id.linear88);
		cb_agree_to_rules = findViewById(R.id.cb_agree_to_rules);
		textview62 = findViewById(R.id.textview62);
		btn_cancel_join_request = findViewById(R.id.btn_cancel_join_request);
		btn_send_join_request = findViewById(R.id.btn_send_join_request);
		groupsp = getSharedPreferences("groupsp", Activity.MODE_PRIVATE);
		fauth = FirebaseAuth.getInstance();
		
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
			com.harvanna.app.utils.StatusBarUtil.matchToolbar(this);
		}
		
		groupId = getIntent() != null ? getIntent().getStringExtra("groupId") : null;
		if (groupId == null || groupId.trim().isEmpty()) {
			groupId = groupsp != null ? groupsp.getString("preview_group_id", "") : "";
		}
		groupId = groupId != null ? groupId.trim() : "";
		
		if (groupId.isEmpty()) {
			SketchwareUtil.showMessage(getApplicationContext(), "Missing group id.");
			finish();
			return;
		}
		
		_loadGroup();
		_loadRecentActivity();
		
		
		if (btn_send_join_request != null) {
			btn_send_join_request.setEnabled(false);
			btn_send_join_request.setAlpha(0.45f);
			btn_send_join_request.setOnClickListener(v -> _requestToJoin());
		}
		if (btn_cancel_join_request != null) {
			btn_cancel_join_request.setOnClickListener(v -> finish());
		}
		if (cb_agree_to_rules != null) {
			cb_agree_to_rules.setOnCheckedChangeListener((buttonView, isChecked) -> _updateSendEnabled());
		}
		if (et_message_to_admin != null) {
			et_message_to_admin.addTextChangedListener(new TextWatcher() {
				@Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
				@Override public void onTextChanged(CharSequence s, int start, int before, int count) {
					if (tv_message_char_count != null) {
						tv_message_char_count.setText(s.length() + " / 300");
					}
				}
				@Override public void afterTextChanged(Editable s) {}
			});
		}
		
	}
	
	public void _MORE() {
	}
	private String groupId = "";
	private boolean sending = false;
	private String loadedCreatorId = "";
	private String loadedGroupName = "";
	private String loadedJoinApprovalMethod = "ADMIN";
	private int loadedMembersCount = 0;
	private int loadedTotalSlots = 0;
	private final java.util.Set<String> loadedBlockedUids = new java.util.HashSet<>();
	
	private void _updateSendEnabled() {
		boolean ok = cb_agree_to_rules != null && cb_agree_to_rules.isChecked() && !sending;
		if (btn_send_join_request != null) {
			btn_send_join_request.setEnabled(ok);
			btn_send_join_request.setAlpha(ok ? 1f : 0.45f);
		}
	}
	
	private void _loadGroup() {
		com.harvanna.app.firestore.GroupFirestoreManager.getInstance(getApplicationContext())
		.getGroup(groupId, new com.harvanna.app.firestore.GroupFirestoreManager.DocCallback() {
			@Override
			public void onSuccess(Map<String, Object> data) {
				runOnUiThread(() -> _bindGroup(data));
			}
			
			@Override
			public void onError(String error) {
				runOnUiThread(() ->
				SketchwareUtil.showMessage(getApplicationContext(),
				"Could not load group: " + error));
			}
		});
	}
	
	private void _bindGroup(Map<String, Object> data) {
		if (data == null) return;
		loadedGroupData = new HashMap<>(data);
		
		loadedGroupName = _str(data, "groupName", _str(data, "name", "Group"));
		loadedCreatorId = _str(data, "creatorId", "");
		loadedMembersCount = _asInt(data.get("membersCount"), 0);
		loadedTotalSlots = _asInt(data.get("totalSlots"), _asInt(data.get("slots"), 0));
		boolean requireApproval = false;
		Object reqObj = data.get("requireApprovalToJoin");
		if (reqObj instanceof Boolean) requireApproval = (Boolean) reqObj;
		else if (reqObj != null) requireApproval = "true".equalsIgnoreCase(String.valueOf(reqObj));
		loadedJoinApprovalMethod = _str(data, "joinApprovalMethod", requireApproval ? "ADMIN" : "NONE");
		loadedBlockedUids.clear();
		Object blocked = data.get("blockedMemberUids");
		if (blocked instanceof List) {
			for (Object o : (List<?>) blocked) {
				if (o != null && !String.valueOf(o).trim().isEmpty()) loadedBlockedUids.add(String.valueOf(o).trim());
			}
		}
		
		if (tv_group_name != null) tv_group_name.setText(loadedGroupName);
		
		String city = _str(data, "city", _str(data, "groupCity", ""));
		String state = _str(data, "state", _str(data, "groupState", ""));
		String loc = city;
		if (!state.isEmpty()) loc = city.isEmpty() ? state : city + ", " + state;
		if (loc.isEmpty()) loc = _str(data, "location", "—");
		if (tv_group_location != null) tv_group_location.setText(loc);
		
		String type = _str(data, "groupType", _str(data, "type", "Savings group"));
		if (tv_group_type != null) tv_group_type.setText(type);
		
		if (tv_group_description != null) {
			tv_group_description.setText(_str(data, "groupDescription",
			_str(data, "description", "No description provided.")));
		}
		
		int members = _asInt(data.get("membersCount"), 0);
		int slots = _asInt(data.get("totalSlots"), _asInt(data.get("slots"), 0));
		if (tv_join_members_value != null) {
			tv_join_members_value.setText(slots > 0 ? (members + " / " + slots) : String.valueOf(members));
		}
		
		int round = _asInt(data.get("currentRound"), _asInt(data.get("currentCycle"), 0));
		if (tv_join_cycle_value != null) {
			if (round > 0 && slots > 0) {
				tv_join_cycle_value.setText("Cycle " + round + " of " + slots);
			} else if (round > 0) {
				tv_join_cycle_value.setText("Cycle " + round);
			} else {
				tv_join_cycle_value.setText("—");
			}
		}
		
		long created = 0L;
		Object ts = data.get("createdAt");
		if (ts == null) ts = data.get("timestamp");
		if (ts instanceof Number) created = ((Number) ts).longValue();
		else if (ts != null) {
			try { created = Long.parseLong(String.valueOf(ts)); } catch (Exception ignored) {}
		}
		if (tv_join_created_value != null) {
			if (created > 0) {
				tv_join_created_value.setText(
				new SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(new Date(created)));
			} else {
				tv_join_created_value.setText("—");
			}
		}
		
		if (tv_group_health != null) {
			String status = _str(data, "status", "ACTIVE");
			tv_group_health.setText("ACTIVE".equalsIgnoreCase(status) ? "Healthy" : status);
		}
		
		if (tv_join_admin_value != null) {
			tv_join_admin_value.setText(loadedCreatorId.isEmpty() ? "—" : "Loading…");
		}
		if (!loadedCreatorId.isEmpty()) {
			com.harvanna.app.firestore.UserProfileManager.getInstance(getApplicationContext())
			.getProfile(loadedCreatorId, new com.harvanna.app.firestore.UserProfileManager.DocCallback() {
				@Override
				public void onSuccess(Map<String, Object> profile) {
					String name = _str(profile, "fullName",
					_str(profile, "name",
					_str(profile, "displayName", "Admin")));
					runOnUiThread(() -> {
						if (tv_join_admin_value != null) tv_join_admin_value.setText(name);
					});
				}
				
				@Override
				public void onError(String error) {
					runOnUiThread(() -> {
						if (tv_join_admin_value != null) {
							tv_join_admin_value.setText(_shortId(loadedCreatorId));
						}
					});
				}
			});
		}
		
		_bindRules(data.get("groupRules"));
	}
	
	@SuppressWarnings("unchecked")
	private void _bindRules(Object rawRules) {
		if (layout_group_rules == null) return;
		layout_group_rules.removeAllViews();
		
		List<String> rules = new ArrayList<>();
		if (rawRules instanceof List) {
			for (Object o : (List<?>) rawRules) {
				if (o == null) continue;
				if (o instanceof Map) {
					Object t = ((Map<?, ?>) o).get("text");
					if (t == null) t = ((Map<?, ?>) o).get("rule");
					if (t == null) t = ((Map<?, ?>) o).get("title");
					if (t != null) rules.add(String.valueOf(t));
				} else {
					String s = String.valueOf(o).trim();
					if (!s.isEmpty()) rules.add(s);
				}
			}
		} else if (rawRules instanceof String && !((String) rawRules).trim().isEmpty()) {
			rules.add(((String) rawRules).trim());
		}
		
		if (rules.isEmpty()) {
			rules.add("Follow the contribution schedule set by the group admin.");
			rules.add("Respect other members and keep payment confirmations honest.");
			rules.add("Leaving mid-cycle may forfeit pending payouts for that cycle.");
		}
		
		int accent = UiColors.accent();
		float density = getResources().getDisplayMetrics().density;
		for (int i = 0; i < rules.size(); i++) {
			LinearLayout row = new LinearLayout(this);
			row.setOrientation(LinearLayout.HORIZONTAL);
			LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			if (i < rules.size() - 1) rp.bottomMargin = (int) (12 * density);
			row.setLayoutParams(rp);
			
			TextView bullet = new TextView(this);
			bullet.setText("•");
			bullet.setTextColor(accent);
			bullet.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
			bullet.setPadding(0, 0, (int) (10 * density), 0);
			
			TextView tv = new TextView(this);
			tv.setText(rules.get(i));
			tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
			tv.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
			
			row.addView(bullet);
			row.addView(tv);
			layout_group_rules.addView(row);
		}
	}
	
	private void _loadRecentActivity() {
		if (layout_group_history == null) return;
		com.harvanna.app.group.GroupNoticeHelper.listNotices(getApplicationContext(), groupId,
		new com.harvanna.app.group.GroupNoticeHelper.ListCallback() {
			@Override
			public void onSuccess(List<Map<String, Object>> notices) {
				runOnUiThread(() -> _bindHistory(notices));
			}
			
			@Override
			public void onError(String error) {
				runOnUiThread(() -> _bindHistory(null));
			}
		});
	}
	
	private void _bindHistory(List<Map<String, Object>> notices) {
		if (layout_group_history == null) return;
		layout_group_history.removeAllViews();
		float density = getResources().getDisplayMetrics().density;
		
		List<Map<String, Object>> list = notices != null ? notices : new ArrayList<>();
		int max = Math.min(5, list.size());
		if (max == 0) {
			TextView empty = new TextView(this);
			empty.setText("No recent public activity yet.");
			empty.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
			layout_group_history.addView(empty);
			return;
		}
		
		for (int i = 0; i < max; i++) {
			Map<String, Object> n = list.get(i);
			String title = _str(n, "title", _str(n, "message", "Activity"));
			String when = _relativeTime(com.harvanna.app.group.GroupNoticeHelper.createdAtMillis(n));
			
			LinearLayout row = new LinearLayout(this);
			row.setOrientation(LinearLayout.HORIZONTAL);
			row.setGravity(Gravity.CENTER_VERTICAL);
			LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			if (i < max - 1) rp.bottomMargin = (int) (12 * density);
			row.setLayoutParams(rp);
			
			TextView tv = new TextView(this);
			tv.setText(title);
			tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
			tv.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
			
			TextView time = new TextView(this);
			time.setText(when);
			time.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
			
			row.addView(tv);
			row.addView(time);
			layout_group_history.addView(row);
		}
	}
	
	private Map<String, Object> loadedGroupData = new HashMap<>();
	private Map<String, Object> pendingAck;
	
	private void _requestToJoin() {
		if (sending) return;
		if (cb_agree_to_rules == null || !cb_agree_to_rules.isChecked()) {
			SketchwareUtil.showMessage(getApplicationContext(), "Please agree to the group rules first.");
			return;
		}
		String currentUid = FirebaseAuth.getInstance().getCurrentUser() != null
		? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
		if (currentUid.isEmpty()) {
			SketchwareUtil.showMessage(getApplicationContext(), "Sign in required to request to join.");
			return;
		}
		if (groupId == null || groupId.isEmpty()) {
			SketchwareUtil.showMessage(getApplicationContext(), "Missing group id.");
			return;
		}
		if (loadedBlockedUids.contains(currentUid)) {
			SketchwareUtil.showMessage(getApplicationContext(), "You can't join this group.");
			return;
		}
		if (loadedTotalSlots > 0 && loadedMembersCount >= loadedTotalSlots) {
			SketchwareUtil.showMessage(getApplicationContext(), "This group is full.");
			return;
		}
		
		// Gate: needs a wallet, then the member reads and signs the group contract.
		if (pendingAck == null) {
			com.harvanna.app.group.GroupWelcomeSheet.start(this, groupId, loadedGroupData, ack -> {
				pendingAck = ack;
				_requestToJoin();
			});
			return;
		}
		
		sending = true;
		_updateSendEnabled();
		if (btn_send_join_request != null) btn_send_join_request.setText("Sending…");
		
		com.harvanna.app.firestore.GroupFirestoreManager.getInstance(getApplicationContext())
		.getMember(groupId, currentUid, new com.harvanna.app.firestore.GroupFirestoreManager.DocCallback() {
			@Override
			public void onSuccess(Map<String, Object> member) {
				String status = member != null ? _str(member, "status", "") : "";
				if ("ACTIVE".equalsIgnoreCase(status)) {
					runOnUiThread(() -> {
						sending = false;
						_updateSendEnabled();
						SketchwareUtil.showMessage(getApplicationContext(), "You're already a member.");
						_openGroupDetails();
					});
					return;
				}
				if ("KICKED".equalsIgnoreCase(status) || "REMOVED".equalsIgnoreCase(status)) {
					runOnUiThread(() -> {
						sending = false;
						_updateSendEnabled();
						SketchwareUtil.showMessage(getApplicationContext(), "You can't join this group.");
					});
					return;
				}
				if ("LEFT".equalsIgnoreCase(status) && com.harvanna.app.group.JoinRequestManager.isOpenJoin(loadedJoinApprovalMethod)) {
					com.harvanna.app.group.JoinRequestManager.reactivateMember(getApplicationContext(), groupId, currentUid,
					new com.harvanna.app.group.JoinRequestManager.Callback() {
						@Override public void onSuccess(String message) { _joinSucceeded(message); }
						@Override public void onError(String error) { _joinFailed(error); }
					});
					return;
				}
				_submitJoinRequest(currentUid);
			}
			
			@Override
			public void onError(String error) {
				_submitJoinRequest(currentUid);
			}
		});
	}
	
	private void _submitJoinRequest(String currentUid) {
		boolean open = com.harvanna.app.group.JoinRequestManager.isOpenJoin(loadedJoinApprovalMethod);
		String message = et_message_to_admin != null
		? et_message_to_admin.getText().toString().trim() : "";
		
		if (open) {
			com.harvanna.app.firestore.GroupFirestoreManager mgr =
			com.harvanna.app.firestore.GroupFirestoreManager.getInstance(getApplicationContext());
			int nextSlot = loadedMembersCount + 1;
			com.harvanna.app.group.JoinRequestManager.admitMember(getApplicationContext(), mgr, groupId, currentUid,
			nextSlot, nextSlot, pendingAck, new com.harvanna.app.group.JoinRequestManager.Callback() {
				@Override public void onSuccess(String msg) { _joinSucceeded("You're in."); }
				@Override public void onError(String error) { _joinFailed(error); }
			});
			return;
		}
		
		com.harvanna.app.firestore.GroupFirestoreManager.getInstance(getApplicationContext())
		.getJoinRequest(groupId, currentUid, new com.harvanna.app.firestore.GroupFirestoreManager.DocCallback() {
			@Override
			public void onSuccess(Map<String, Object> existing) {
				String st = existing != null ? _str(existing, "status", "") : "";
				if ("PENDING".equalsIgnoreCase(st)) {
					runOnUiThread(() -> {
						sending = false;
						_updateSendEnabled();
						if (btn_send_join_request != null) {
							btn_send_join_request.setText("Send Join Request");
						}
						SketchwareUtil.showMessage(getApplicationContext(),
						"Your request is already waiting for review.");
					});
					return;
				}
				_writeJoinRequest(currentUid, message);
			}
			@Override
			public void onError(String error) {
				_writeJoinRequest(currentUid, message);
			}
		});
	}
	
	private void _writeJoinRequest(String currentUid, String message) {
		Map<String, Object> requestFields = new HashMap<>();
		requestFields.put("requesterUid", currentUid);
		requestFields.put("status", "PENDING");
		requestFields.put("votes", new HashMap<String, Object>());
		requestFields.put("createdAt", System.currentTimeMillis());
		requestFields.put("message", message);
		requestFields.put("groupId", groupId);
		if (pendingAck != null) requestFields.putAll(pendingAck);
		if (!loadedGroupName.isEmpty()) requestFields.put("groupName", loadedGroupName);
		
		com.harvanna.app.firestore.GroupFirestoreManager.getInstance(getApplicationContext())
		.addJoinRequest(groupId, currentUid, requestFields,
		new com.harvanna.app.firestore.GroupFirestoreManager.IdCallback() {
			@Override
			public void onSuccess(String generatedId) {
				_markInviteAccepted();
				com.harvanna.app.firestore.UserProfileManager.getInstance(getApplicationContext())
				.addPendingGroupToProfile(currentUid, groupId,
				new com.harvanna.app.firestore.UserProfileManager.SimpleCallback() {
					@Override public void onSuccess() { /* best-effort */ }
					@Override
					public void onError(String error) {
						if (com.harvanna.app.offline.NetworkStatus.isConnectivityError(error)) {
							java.util.Map<String, Object> payload = new java.util.HashMap<>();
							payload.put("uid", currentUid);
							payload.put("groupId", groupId);
							com.harvanna.app.offline.OfflineActionQueue.enqueue(
							getApplicationContext(), "PENDING_GROUP_MARK", payload);
						}
					}
				});
				runOnUiThread(() -> {
					sending = false;
					SketchwareUtil.showMessage(getApplicationContext(),
					"Request sent — the group admin will review it.");
					finish();
				});
			}
			
			@Override
			public void onError(String error) {
				_joinFailed(error);
			}
		});
	}
	
	private void _markInviteAccepted() {
		String inviteId = getIntent() != null ? getIntent().getStringExtra("inviteId") : null;
		if (inviteId != null && !inviteId.isEmpty()) {
			com.harvanna.app.group.InviteManager.respond(getApplicationContext(), inviteId, true, null);
		}
	}
	
	private void _joinSucceeded(String message) {
		_markInviteAccepted();
		final String circleId = loadedGroupData != null && loadedGroupData.get("circleId") != null
		? String.valueOf(loadedGroupData.get("circleId")).trim() : "";
		runOnUiThread(() -> {
			if (!circleId.isEmpty()) {
				// Rotating group: the last step is to lock the bond on the network.
				sending = false;
				new androidx.appcompat.app.AlertDialog.Builder(this)
				.setTitle("One more step")
				.setMessage("You're in the group. To secure your place in the payout order, lock your bond on the network now.")
				.setCancelable(false)
				.setPositiveButton("Continue", (dd, w) -> {
					Intent ci = new Intent(this, com.harvanna.app.circle.CircleActivity.class);
					ci.putExtra("circleId", circleId);
					startActivity(ci);
					finish();
				}).show();
				return;
			}
			sending = false;
			SketchwareUtil.showMessage(getApplicationContext(),
			message != null ? message : "You're in.");
			_openGroupDetails();
		});
	}
	
	private void _joinFailed(String error) {
		runOnUiThread(() -> {
			sending = false;
			_updateSendEnabled();
			if (btn_send_join_request != null) {
				btn_send_join_request.setText("Send Join Request");
			}
			SketchwareUtil.showMessage(getApplicationContext(),
			"Failed to join: " + error);
		});
	}
	
	private void _openGroupDetails() {
		Intent i = new Intent(GroupJoinRequestActivity.this, GroupDetailsActivity.class);
		i.putExtra("groupId", groupId);
		startActivity(i);
		finish();
	}
	
	private static String _str(Map<String, Object> m, String key, String def) {
		if (m == null || key == null) return def;
		Object v = m.get(key);
		if (v == null) return def;
		String s = String.valueOf(v).trim();
		return s.isEmpty() || "null".equalsIgnoreCase(s) ? def : s;
	}
	
	private static int _asInt(Object v, int def) {
		if (v instanceof Number) return ((Number) v).intValue();
		if (v != null) {
			try { return Integer.parseInt(String.valueOf(v).trim()); } catch (Exception ignored) {}
		}
		return def;
	}
	
	private static String _shortId(String id) {
		if (id == null || id.length() < 8) return id != null ? id : "—";
		return id.substring(0, 6) + "…";
	}
	
	private static String _relativeTime(long ms) {
		if (ms <= 0) return "";
		long diff = System.currentTimeMillis() - ms;
		if (diff < 60_000) return "just now";
		if (diff < 3_600_000) return (diff / 60_000) + "m ago";
		if (diff < 86_400_000) return (diff / 3_600_000) + "h ago";
		if (diff < 7L * 86_400_000) return (diff / 86_400_000) + "d ago";
		return (diff / (7L * 86_400_000)) + "w ago";
	}
	
	{
	}
	
}
