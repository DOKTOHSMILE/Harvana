package com.harvanna.wallet;

import com.harvanna.wallet.utils.UiColors;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.*;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.*;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
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

public class GroupOverviewFragmentActivity extends Fragment {
	
	private FirebaseDatabase _firebase = FirebaseDatabase.getInstance();
	
	private String groupId = "";
	private HashMap<String, Object> user_v_map = new HashMap<>();
	private double length = 0;
	private double number = 0;
	private boolean isAdmin = false;
	
	private ArrayList<HashMap<String, Object>> group_list_map = new ArrayList<>();
	private ArrayList<HashMap<String, Object>> rules_l_map = new ArrayList<>();
	private ArrayList<HashMap<String, Object>> fixed_savings_slot_map = new ArrayList<>();
	
	private LinearLayout linear60;
	private MaterialCardView card_group_wallet;
	private TextView textview52;
	private MaterialCardView cardview18;
	private LinearLayout linear75;
	private TextView tv_rules_header;
	private MaterialCardView card_code_of_conduct;
	private TextView textview21;
	private MaterialCardView cardview10;
	private MaterialCardView panel_rotating;
	private MaterialCardView panel_fixed;
	private MaterialCardView panel_accumulative;
	private TextView textview61;
	private MaterialCardView cardview21;
	private MaterialCardView panel_payout_requests;
	private LinearLayout linear76;
	private LinearLayout linear17;
	private MaterialCardView cardview22;
	private View view18;
	private LinearLayout linear19;
	private FrameLayout frame_layout86;
	private TextView tv_cycle_hint;
	private TextView tv_group_wallet_hint;
	private ImageView imageview15;
	private TextView textview50;
	private TextView tv_group_wallet_balance;
	private LinearLayout linear96;
	private TextView tv_group_wallet_address;
	private ImageView imageview16;
	private TextView tv_cycle_status;
	private TextView tv_cycle_percent;
	private View view_cycle_progress;
	private LinearLayout linear20;
	private LinearLayout linear78;
	private View view16;
	private LinearLayout linear18;
	private View view24;
	private LinearLayout linear25;
	private LinearLayout linear79;
	private View view14;
	private LinearLayout linear15;
	private TextView textview7;
	private TextView tv_rule_type;
	private TextView textview8;
	private TextView tv_rule_contribution;
	private LinearLayout linear21;
	private View view22;
	private LinearLayout linear23;
	private TextView textview9;
	private TextView tv_rule_location;
	private TextView tv_label_admin_or_interest;
	private TextView tv_rule_admin_fee;
	private LinearLayout linear26;
	private View view27;
	private LinearLayout linear28;
	private TextView textview11;
	private TextView tv_rule_grace_period;
	private TextView textview12;
	private TextView tv_rule_distribution;
	private TextView textview49;
	private TextView tv_overview_description;
	private LinearLayout layout_rules_container;
	private LinearLayout linear29;
	private RecyclerView recycler_payment_slots;
	private LinearLayout linear30;
	private LinearLayout linear2;
	private TextView tv_slot_grid_helper_hint;
	private View view31;
	private TextView textview22;
	private View view32;
	private TextView textview23;
	private LinearLayout linear87;
	private TextView textview58;
	private TextView tv_rotating_current;
	private TextView tv_rotating_next;
	private TextView tv_rotating_method;
	private LinearLayout linear88;
	private TextView textview59;
	private TextView tv_fixed_goal;
	private TextView tv_fixed_progress;
	private TextView tv_fixed_rule;
	private LinearLayout linear89;
	private TextView textview60;
	private TextView tv_loan_rate;
	private TextView tv_loan_withdrawal;
	private TextView tv_loan_active_count;
	private LinearLayout linear90;
	private LinearLayout linear91;
	private LinearLayout linear92;
	private LinearLayout linear93;
	private TextView textview62;
	private TextView tv_pool_contribution;
	private TextView textview63;
	private TextView tv_pool_total;
	private TextView textview64;
	private TextView tv_pool_paid;
	private LinearLayout linear94;
	private TextView textview65;
	private RecyclerView recycler_payout_requests;
	private TextView tv_payout_requests_empty;
	
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
		View _view = _inflater.inflate(R.layout.group_overview_fragment, _container, false);
		initialize(_savedInstanceState, _view);
		FirebaseApp.initializeApp(getContext());
		initializeLogic();
		return _view;
	}
	
	private void initialize(Bundle _savedInstanceState, View _view) {
		linear60 = _view.findViewById(R.id.linear60);
		card_group_wallet = _view.findViewById(R.id.card_group_wallet);
		textview52 = _view.findViewById(R.id.textview52);
		cardview18 = _view.findViewById(R.id.cardview18);
		linear75 = _view.findViewById(R.id.linear75);
		tv_rules_header = _view.findViewById(R.id.tv_rules_header);
		card_code_of_conduct = _view.findViewById(R.id.card_code_of_conduct);
		textview21 = _view.findViewById(R.id.textview21);
		cardview10 = _view.findViewById(R.id.cardview10);
		panel_rotating = _view.findViewById(R.id.panel_rotating);
		panel_fixed = _view.findViewById(R.id.panel_fixed);
		panel_accumulative = _view.findViewById(R.id.panel_accumulative);
		textview61 = _view.findViewById(R.id.textview61);
		cardview21 = _view.findViewById(R.id.cardview21);
		panel_payout_requests = _view.findViewById(R.id.panel_payout_requests);
		linear76 = _view.findViewById(R.id.linear76);
		linear17 = _view.findViewById(R.id.linear17);
		cardview22 = _view.findViewById(R.id.cardview22);
		view18 = _view.findViewById(R.id.view18);
		linear19 = _view.findViewById(R.id.linear19);
		frame_layout86 = _view.findViewById(R.id.frame_layout86);
		tv_cycle_hint = _view.findViewById(R.id.tv_cycle_hint);
		tv_group_wallet_hint = _view.findViewById(R.id.tv_group_wallet_hint);
		imageview15 = _view.findViewById(R.id.imageview15);
		textview50 = _view.findViewById(R.id.textview50);
		tv_group_wallet_balance = _view.findViewById(R.id.tv_group_wallet_balance);
		linear96 = _view.findViewById(R.id.linear96);
		tv_group_wallet_address = _view.findViewById(R.id.tv_group_wallet_address);
		imageview16 = _view.findViewById(R.id.imageview16);
		tv_cycle_status = _view.findViewById(R.id.tv_cycle_status);
		tv_cycle_percent = _view.findViewById(R.id.tv_cycle_percent);
		view_cycle_progress = _view.findViewById(R.id.view_cycle_progress);
		linear20 = _view.findViewById(R.id.linear20);
		linear78 = _view.findViewById(R.id.linear78);
		view16 = _view.findViewById(R.id.view16);
		linear18 = _view.findViewById(R.id.linear18);
		view24 = _view.findViewById(R.id.view24);
		linear25 = _view.findViewById(R.id.linear25);
		linear79 = _view.findViewById(R.id.linear79);
		view14 = _view.findViewById(R.id.view14);
		linear15 = _view.findViewById(R.id.linear15);
		textview7 = _view.findViewById(R.id.textview7);
		tv_rule_type = _view.findViewById(R.id.tv_rule_type);
		textview8 = _view.findViewById(R.id.textview8);
		tv_rule_contribution = _view.findViewById(R.id.tv_rule_contribution);
		linear21 = _view.findViewById(R.id.linear21);
		view22 = _view.findViewById(R.id.view22);
		linear23 = _view.findViewById(R.id.linear23);
		textview9 = _view.findViewById(R.id.textview9);
		tv_rule_location = _view.findViewById(R.id.tv_rule_location);
		tv_label_admin_or_interest = _view.findViewById(R.id.tv_label_admin_or_interest);
		tv_rule_admin_fee = _view.findViewById(R.id.tv_rule_admin_fee);
		linear26 = _view.findViewById(R.id.linear26);
		view27 = _view.findViewById(R.id.view27);
		linear28 = _view.findViewById(R.id.linear28);
		textview11 = _view.findViewById(R.id.textview11);
		tv_rule_grace_period = _view.findViewById(R.id.tv_rule_grace_period);
		textview12 = _view.findViewById(R.id.textview12);
		tv_rule_distribution = _view.findViewById(R.id.tv_rule_distribution);
		textview49 = _view.findViewById(R.id.textview49);
		tv_overview_description = _view.findViewById(R.id.tv_overview_description);
		layout_rules_container = _view.findViewById(R.id.layout_rules_container);
		linear29 = _view.findViewById(R.id.linear29);
		recycler_payment_slots = _view.findViewById(R.id.recycler_payment_slots);
		linear30 = _view.findViewById(R.id.linear30);
		linear2 = _view.findViewById(R.id.linear2);
		tv_slot_grid_helper_hint = _view.findViewById(R.id.tv_slot_grid_helper_hint);
		view31 = _view.findViewById(R.id.view31);
		textview22 = _view.findViewById(R.id.textview22);
		view32 = _view.findViewById(R.id.view32);
		textview23 = _view.findViewById(R.id.textview23);
		linear87 = _view.findViewById(R.id.linear87);
		textview58 = _view.findViewById(R.id.textview58);
		tv_rotating_current = _view.findViewById(R.id.tv_rotating_current);
		tv_rotating_next = _view.findViewById(R.id.tv_rotating_next);
		tv_rotating_method = _view.findViewById(R.id.tv_rotating_method);
		linear88 = _view.findViewById(R.id.linear88);
		textview59 = _view.findViewById(R.id.textview59);
		tv_fixed_goal = _view.findViewById(R.id.tv_fixed_goal);
		tv_fixed_progress = _view.findViewById(R.id.tv_fixed_progress);
		tv_fixed_rule = _view.findViewById(R.id.tv_fixed_rule);
		linear89 = _view.findViewById(R.id.linear89);
		textview60 = _view.findViewById(R.id.textview60);
		tv_loan_rate = _view.findViewById(R.id.tv_loan_rate);
		tv_loan_withdrawal = _view.findViewById(R.id.tv_loan_withdrawal);
		tv_loan_active_count = _view.findViewById(R.id.tv_loan_active_count);
		linear90 = _view.findViewById(R.id.linear90);
		linear91 = _view.findViewById(R.id.linear91);
		linear92 = _view.findViewById(R.id.linear92);
		linear93 = _view.findViewById(R.id.linear93);
		textview62 = _view.findViewById(R.id.textview62);
		tv_pool_contribution = _view.findViewById(R.id.tv_pool_contribution);
		textview63 = _view.findViewById(R.id.textview63);
		tv_pool_total = _view.findViewById(R.id.tv_pool_total);
		textview64 = _view.findViewById(R.id.textview64);
		tv_pool_paid = _view.findViewById(R.id.tv_pool_paid);
		linear94 = _view.findViewById(R.id.linear94);
		textview65 = _view.findViewById(R.id.textview65);
		recycler_payout_requests = _view.findViewById(R.id.recycler_payout_requests);
		tv_payout_requests_empty = _view.findViewById(R.id.tv_payout_requests_empty);
		groupsp = getContext().getSharedPreferences("groupsp", Activity.MODE_PRIVATE);
		authsp = getContext().getSharedPreferences("authsp", Activity.MODE_PRIVATE);
		fauth = FirebaseAuth.getInstance();
		
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
		groupId = getArguments() != null ? getArguments().getString("groupId", "") : "";
		_loadGroupData();
		
	}
	
	@Override
	public void onResume() {
		super.onResume();
		if (groupId != null && !groupId.isEmpty()) {
			_loadGroupData();
		}
		
	}
	public void _MORE() {
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
			loc = _safeString(data, "location", _safeString(data, "groupLocation", "Unknown"));
		}
		return loc.isEmpty() ? "Unknown" : loc;
	}
	
	private void _loadGroupData() {
		if (groupId == null || groupId.isEmpty()) {
			groupId = getArguments() != null ? getArguments().getString("groupId", "") : "";
		}
		if (groupId == null || groupId.isEmpty()) return;
		if (getContext() == null) return;
		
		final String cacheKey = "group_" + groupId;
		Map<String, Object> cached = com.harvanna.wallet.offline.OfflineCacheManager.load(getContext(), cacheKey);
		if (cached != null) {
			_applyGroupData(cached);
		}
		
		com.harvanna.wallet.firestore.GroupFirestoreManager.getInstance(getContext())
		.getGroup(groupId, new com.harvanna.wallet.firestore.GroupFirestoreManager.DocCallback() {
			@Override
			public void onSuccess(Map<String, Object> data) {
				if (getActivity() == null) return;
				com.harvanna.wallet.offline.OfflineCacheManager.save(getContext(), cacheKey, data);
				getActivity().runOnUiThread(() -> _applyGroupData(data));
			}
			
			@Override
			public void onError(String error) {
				if (getActivity() == null) return;
				if (cached != null) return;
				getActivity().runOnUiThread(() ->
				SketchwareUtil.showMessage(getContext(),
				"Failed to load group overview: " + error));
			}
		});
	}
	
	private void _applyGroupData(Map<String, Object> data) {
		if (data == null) return;
		
		// Admin?
		String creatorId = _safeString(data, "creatorId", "");
		String uid = FirebaseAuth.getInstance().getCurrentUser() != null
		? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
		isAdmin = !creatorId.isEmpty() && creatorId.equals(uid);
		
		String groupType = _safeString(data, "groupType", "");
		String description = _safeString(data, "groupDescription",
		_safeString(data, "description", "No description yet."));
		double contribution = _safeDouble(data, "contributionAmount");
		long totalSlots = (long) _safeDouble(data, "totalSlots");
		long currentRound = (long) _safeDouble(data, "currentRound");
		if (currentRound <= 0) currentRound = 1;
		if (totalSlots <= 0) totalSlots = 1;
		
		String groupWallet = _safeString(data, "groupWalletAddress",
		_safeString(data, "group_wallet", "—"));
		double goal = _safeDouble(data, "targetGoalAmount");
		
		// About
		if (tv_overview_description != null) {
			tv_overview_description.setText(description);
		}
		
		// Group wallet
		if (tv_group_wallet_address != null) {
			tv_group_wallet_address.setText(groupWallet);
		}
		if (card_group_wallet != null) {
			card_group_wallet.setVisibility(View.VISIBLE);
		}
		_loadGroupWalletBalance(groupWallet);
		
		// Financial Terms Extractions
		String location = _extractLocation(data);
		String gracePeriod = _safeString(data, "gracePeriodDays", "0");
		String distribution = _safeString(data, "distributionMethod", "—");
		
		// Dynamically handle Admin Fee vs Interest Rate based on Group Type
		boolean isAccumulative = groupType.contains("Accumulative");
		if (tv_label_admin_or_interest != null) {
			tv_label_admin_or_interest.setText(isAccumulative ? "LOAN INTEREST" : "ADMIN FEE");
		}
		
		String secondaryFinancialVal = "0%";
		if (isAccumulative) {
			double rate = _safeDouble(data, "loanInterestRate");
			secondaryFinancialVal = String.format(Locale.US, "%.2f%%", rate);
		} else {
			String commission = _safeString(data, "commissionPercent", "0");
			if (!commission.contains("%")) commission += "%";
			secondaryFinancialVal = commission;
		}
		
		// Apply to Financial Terms Grid
		if (tv_rule_type != null) tv_rule_type.setText(groupType.isEmpty() ? "—" : groupType);
		// APPLIED FIX
		if (tv_rule_contribution != null) tv_rule_contribution.setText(_formatUIBalance(contribution));
		if (tv_rule_location != null) tv_rule_location.setText(location);
		if (tv_rule_admin_fee != null) tv_rule_admin_fee.setText(secondaryFinancialVal);
		if (tv_rule_grace_period != null) tv_rule_grace_period.setText(gracePeriod + " Days");
		if (tv_rule_distribution != null) tv_rule_distribution.setText(distribution);
		
		// Robust Custom Rules Array Parser (Code of Conduct)
		if (layout_rules_container != null) {
			layout_rules_container.removeAllViews();
			List<String> parsedRules = new ArrayList<>();
			
			Object rulesObj = data.get("groupRules");
			if (rulesObj instanceof List) {
				List<?> rawList = (List<?>) rulesObj;
				for (Object item : rawList) {
					if (item instanceof Map) {
						Map<?, ?> mapItem = (Map<?, ?>) item;
						String r = "";
						if (mapItem.containsKey("rules")) r = String.valueOf(mapItem.get("rules"));
						else if (mapItem.containsKey("rule")) r = String.valueOf(mapItem.get("rule"));
						else if (mapItem.containsKey("text")) r = String.valueOf(mapItem.get("text"));
						
						if (r != null && !r.trim().isEmpty() && !"null".equalsIgnoreCase(r.trim())) {
							parsedRules.add(r.trim());
						}
					} else if (item instanceof String) {
						String r = (String) item;
						if (r != null && !r.trim().isEmpty()) {
							parsedRules.add(r.trim());
						}
					}
				}
			}
			
			boolean hasRules = !parsedRules.isEmpty();
			
			if (hasRules) {
				if (tv_rules_header != null) tv_rules_header.setVisibility(View.VISIBLE);
				if (card_code_of_conduct != null) card_code_of_conduct.setVisibility(View.VISIBLE);
				
				for (String ruleText : parsedRules) {
					TextView tvRule = new TextView(getContext());
					tvRule.setText("•  " + ruleText);
					tvRule.setTextColor(UiColors.textSecondary()); 
					tvRule.setTextSize(13.5f);
					tvRule.setPadding(0, 8, 0, 8);
					layout_rules_container.addView(tvRule);
				}
				
				String consequence = _safeString(data, "defaultConsequence", "");
				if (!consequence.isEmpty()) {
					TextView tvConsequence = new TextView(getContext());
					tvConsequence.setText("Penalty for violation: " + consequence);
					tvConsequence.setTextColor(UiColors.error()); 
					tvConsequence.setPadding(0, 16, 0, 0);
					tvConsequence.setTypeface(null, Typeface.BOLD);
					tvConsequence.setTextSize(13f);
					layout_rules_container.addView(tvConsequence);
				}
			} else {
				if (tv_rules_header != null) tv_rules_header.setVisibility(View.GONE);
				if (card_code_of_conduct != null) card_code_of_conduct.setVisibility(View.GONE);
			}
		}
		
		// Cycle progress
		int percent = (int) Math.min(100, (currentRound * 100.0) / totalSlots);
		if (tv_cycle_status != null) {
			tv_cycle_status.setText("Slot " + currentRound + " of " + totalSlots);
		}
		if (tv_cycle_percent != null) {
			tv_cycle_percent.setText(percent + "%");
		}
		if (tv_slot_grid_helper_hint != null) {
			tv_slot_grid_helper_hint.setText("Tracks unique member slots participating in Round " + currentRound);
		}
		_updateProgressBar(percent);
		_loadPaymentSlots(groupId, currentRound, totalSlots, goal);
		
		// Type panels — only one visible
		boolean rotating = groupType.contains("Rotating");
		boolean fixed = groupType.contains("Fixed");
		boolean accum = groupType.contains("Accumulative");
		
		if (panel_rotating != null) panel_rotating.setVisibility(rotating ? View.VISIBLE : View.GONE);
		if (panel_fixed != null) panel_fixed.setVisibility(fixed ? View.VISIBLE : View.GONE);
		if (panel_accumulative != null) panel_accumulative.setVisibility(accum ? View.VISIBLE : View.GONE);
		
		if (rotating) {
			if (tv_rotating_current != null) {
				tv_rotating_current.setText("Current slot: " + currentRound + " / " + totalSlots);
			}
			if (tv_rotating_next != null) {
				tv_rotating_next.setText("Next recipient: —");
			}
			if (tv_rotating_method != null) {
				tv_rotating_method.setText("Order: " + distribution);
			}
		}
		
		if (fixed) {
			// APPLIED FIX
			if (tv_fixed_goal != null) {
				tv_fixed_goal.setText("Target: " + _formatUIBalance(goal));
			}
			if (tv_fixed_rule != null) {
				tv_fixed_rule.setText("Disbursement: " + _safeString(data, "disbursementRule", "—"));
			}
		}
		
		if (accum) {
			double rate = _safeDouble(data, "loanInterestRate");
			if (tv_loan_rate != null) {
				tv_loan_rate.setText(String.format(Locale.US, "Interest: %.2f%%", rate));
			}
			if (tv_loan_withdrawal != null) {
				tv_loan_withdrawal.setText("Withdrawal rule: " + _safeString(data, "withdrawalRule", "—"));
			}
			if (tv_loan_active_count != null) {
				tv_loan_active_count.setText("Active loans: 0");
			}
		}
		
		// Pool summary
		// APPLIED FIX
		if (tv_pool_contribution != null) {
			tv_pool_contribution.setText(_formatUIBalance(contribution));
		}
		if (tv_pool_total != null) {
			tv_pool_total.setText(_formatUIBalance(contribution * totalSlots));
		}
	}
	
	private void _updateProgressBar(int percent) {
		if (view_cycle_progress == null) return;
		view_cycle_progress.post(() -> {
			View parent = (View) view_cycle_progress.getParent();
			if (parent == null) return;
			int w = parent.getWidth();
			if (w <= 0) return;
			ViewGroup.LayoutParams lp = view_cycle_progress.getLayoutParams();
			lp.width = Math.max(8, (w * percent) / 100);
			view_cycle_progress.setLayoutParams(lp);
		});
	}
	
	private void _loadGroupWalletBalance(String address) {
		if (address == null || !address.startsWith("HV0x")) {
			if (tv_group_wallet_balance != null) tv_group_wallet_balance.setText("0 AJC");
			return;
		}
		final String cacheKey = "wallet_balance_" + address;
		Map<String, Object> cached = com.harvanna.wallet.offline.OfflineCacheManager.load(getContext(), cacheKey);
		if (cached != null && tv_group_wallet_balance != null) {
			Object show = cached.get("show");
			if (show instanceof Number) {
				// APPLIED FIX
				tv_group_wallet_balance.setText(_formatUIBalance(((Number) show).doubleValue()));
			}
		}
		com.harvanna.wallet.sync.AjooNodeClient.getWallet(address, new com.harvanna.wallet.sync.AjooNodeClient.JsonCallback() {
			@Override
			public void onSuccess(org.json.JSONObject json) {
				long raw = json.optLong("balance", 0L);
				double ajc = com.harvanna.wallet.sync.AjooNodeClient.toAjcDisplay(raw);
				Map<String, Object> toCache = new HashMap<>();
				toCache.put("show", ajc); // Saves safely as a raw double to the cache
				if (getContext() != null) {
					com.harvanna.wallet.offline.OfflineCacheManager.save(getContext(), cacheKey, toCache);
				}
				if (getActivity() == null) return;
				getActivity().runOnUiThread(() -> {
					if (tv_group_wallet_balance != null) {
						// APPLIED FIX
						tv_group_wallet_balance.setText(_formatUIBalance(ajc));
					}
				});
			}
			
			@Override
			public void onError(String error) {
				if (getActivity() == null) return;
				if (cached != null) return;
				getActivity().runOnUiThread(() -> {
					if (tv_group_wallet_balance != null) {
						tv_group_wallet_balance.setText("—");
					}
				});
			}
		});
	}
	
	private void _loadPaymentSlots(String groupId, long currentRound, long totalSlots, double targetGoal) {
		if (groupId == null || groupId.isEmpty() || recycler_payment_slots == null) return;
		
		List<HashMap<String, Object>> cachedMembers =
		com.harvanna.wallet.offline.OfflineCacheManager.loadList(getContext(), "members_" + groupId);
		List<HashMap<String, Object>> cachedPayments =
		com.harvanna.wallet.offline.OfflineCacheManager.loadList(getContext(), "payments_" + groupId);
		if (cachedMembers != null || cachedPayments != null) {
			_paintPaymentSlots(cachedMembers, cachedPayments, currentRound, totalSlots, targetGoal);
		}
		
		com.harvanna.wallet.firestore.GroupFirestoreManager.getInstance(getContext())
		.getMembers(groupId, new com.harvanna.wallet.firestore.GroupFirestoreManager.ListCallback() {
			@Override
			public void onSuccess(java.util.List<Map<String, Object>> members) {
				com.harvanna.wallet.firestore.GroupFirestoreManager.getInstance(getContext())
				.getPayments(groupId, new com.harvanna.wallet.firestore.GroupFirestoreManager.ListCallback() {
					@Override
					public void onSuccess(java.util.List<Map<String, Object>> payments) {
						if (members != null) {
							List<HashMap<String, Object>> mCache = new ArrayList<>();
							for (Map<String, Object> m : members) {
								mCache.add(m instanceof HashMap
								? (HashMap<String, Object>) m
								: new HashMap<>(m));
							}
							com.harvanna.wallet.offline.OfflineCacheManager.saveList(
							getContext(), "members_" + groupId, mCache);
						}
						if (payments != null) {
							List<HashMap<String, Object>> pCache = new ArrayList<>();
							for (Map<String, Object> p : payments) {
								pCache.add(p instanceof HashMap
								? (HashMap<String, Object>) p
								: new HashMap<>(p));
							}
							com.harvanna.wallet.offline.OfflineCacheManager.saveList(
							getContext(), "payments_" + groupId, pCache);
						}
						_paintPaymentSlots(members, payments, currentRound, totalSlots, targetGoal);
					}
					@Override public void onError(String error) {}
				});
			}
			@Override public void onError(String error) {}
		});
	}
	
	private void _paintPaymentSlots(java.util.List<? extends Map<String, Object>> members,
	java.util.List<? extends Map<String, Object>> payments,
	long currentRound, long totalSlots, double targetGoal) {
		if (totalSlots <= 0) totalSlots = 1;
		
		final Map<String, Integer> uidToSlot = new HashMap<>();
		final Map<Integer, String> slotToUid = new HashMap<>();
		
		if (members != null) {
			for (Map<String, Object> m : members) {
				if (m == null) continue;
				int slot = (int) _safeDouble(m, "slot");
				String uid = _safeString(m, "memberUid", _safeString(m, "uid", ""));
				if (slot > 0 && !uid.isEmpty()) {
					uidToSlot.put(uid, slot);
					slotToUid.put(slot, uid);
				}
			}
		}
		
		java.util.Set<String> paidUidsThisRound = new java.util.HashSet<>();
		java.util.Set<Integer> paidSlotsThisRound = new java.util.HashSet<>();
		double totalRaisedAllTime = 0;
		
		if (payments != null) {
			for (Map<String, Object> p : payments) {
				if (p == null) continue;
				String st = _safeString(p, "status", "");
				boolean confirmed = "CONFIRMED".equalsIgnoreCase(st) || "PAID".equalsIgnoreCase(st);
				if (!confirmed) continue;
				
				double amt = _safeDouble(p, "amountAjc");
				if (amt <= 0) amt = _safeDouble(p, "amount");
				totalRaisedAllTime += amt;
				
				long round = (long) _safeDouble(p, "round");
				if (round <= 0) round = currentRound;
				
				if (round == currentRound) {
					String pUid = _safeString(p, "memberUid", _safeString(p, "uid", ""));
					int pSlot = (int) _safeDouble(p, "slot");
					
					if (!pUid.isEmpty()) {
						paidUidsThisRound.add(pUid);
						if (uidToSlot.containsKey(pUid)) {
							paidSlotsThisRound.add(uidToSlot.get(pUid));
						}
					}
					if (pSlot > 0) {
						paidSlotsThisRound.add(pSlot);
					}
				}
			}
		}
		
		boolean[] slotPaid = new boolean[(int) totalSlots + 1];
		for (int s : paidSlotsThisRound) {
			if (s < slotPaid.length) slotPaid[s] = true;
		}
		for (Map.Entry<Integer, String> e : slotToUid.entrySet()) {
			if (e.getKey() < slotPaid.length && paidUidsThisRound.contains(e.getValue())) {
				slotPaid[e.getKey()] = true;
			}
		}
		
		final double finalRaised = totalRaisedAllTime;
		final int paidCount = Math.max(paidUidsThisRound.size(), paidSlotsThisRound.size());
		final int slots = (int) totalSlots;
		
		Runnable paint = () -> {
			if (recycler_payment_slots == null) return;
			if (recycler_payment_slots.getLayoutManager() == null) {
				recycler_payment_slots.setLayoutManager(
				new androidx.recyclerview.widget.GridLayoutManager(getContext(), 8));
			}
			recycler_payment_slots.setAdapter(new SlotGridAdapter(slots, slotPaid));
			if (tv_pool_paid != null) {
				tv_pool_paid.setText(paidCount + " / " + slots);
			}
			if (tv_fixed_progress != null) {
				// APPLIED FIX
				tv_fixed_progress.setText("Raised: "
				+ _formatUIBalance(finalRaised)
				+ " / " + _formatUIBalance(targetGoal));
			}
		};
		if (getActivity() != null && isAdded()) {
			getActivity().runOnUiThread(paint);
		} else {
			paint.run();
		}
	}
	
	private class SlotGridAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<SlotGridAdapter.VH> {
		private final int totalSlots;
		private final boolean[] slotPaid;
		SlotGridAdapter(int totalSlots, boolean[] slotPaid) {
			this.totalSlots = totalSlots;
			this.slotPaid = slotPaid;
		}
		
		@Override
		public VH onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
			View v = LayoutInflater.from(parent.getContext())
			.inflate(parent.getResources().getIdentifier(
			"item_payment_slot_cell", "layout", parent.getContext().getPackageName()),
			parent, false);
			return new VH(v);
		}
		
		@Override
		public void onBindViewHolder(VH h, int position) {
			int slot = position + 1;
			boolean paid = slot < slotPaid.length && slotPaid[slot];
			if (h.label != null) h.label.setText(String.valueOf(slot));
			if (h.card != null) {
				h.card.setCardBackgroundColor(paid
				? getResources().getColor(R.color.colorAccent)
				: getResources().getColor(R.color.colorSurfaceAlt));
			}
			if (h.label != null) {
				h.label.setTextColor(paid ? 0xFFFFFFFF : getResources().getColor(R.color.colorTextPrimary));
			}
		}
		
		@Override
		public int getItemCount() { return totalSlots; }
		
		class VH extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
			TextView label;
			androidx.cardview.widget.CardView card;
			VH(View v) {
				super(v);
				if (v instanceof androidx.cardview.widget.CardView) card = (androidx.cardview.widget.CardView) v;
				int labelId = v.getResources().getIdentifier("tv_slot_number", "id", v.getContext().getPackageName());
				if (labelId != 0) label = v.findViewById(labelId);
			}
		}
	}
	
	private String _safeString(Map<String, Object> data, String key, String fallback) {
		Object v = data.get(key);
		if (v == null) return fallback;
		String s = v.toString().trim();
		return s.isEmpty() || "null".equalsIgnoreCase(s) ? fallback : s;
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
	
	// NEW UI FORMATTER HELPER
	private String _formatUIBalance(double v) {
		try {
			java.text.DecimalFormat uiDf = new java.text.DecimalFormat("#,##0.########", new java.text.DecimalFormatSymbols(java.util.Locale.US));
			return uiDf.format(v);
		} catch (Exception e) {
			return String.valueOf(v);
		}
	}
	{
	}
	
}
