package com.harvana.app;

import android.animation.*;
import android.app.*;
import android.app.Activity;
import android.content.*;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.media.*;
import android.net.*;
import android.os.*;
import android.os.Vibrator;
import android.text.*;
import android.text.style.*;
import android.util.*;
import android.view.*;
import android.view.View;
import android.view.View.*;
import android.view.animation.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.biometric.*;
import androidx.cardview.widget.CardView;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.widget.NestedScrollView;
import androidx.slidingpanelayout.*;
import androidx.swiperefreshlayout.*;
import androidx.transition.AutoTransition;
import androidx.transition.TransitionManager;
import com.harvana.app.wallet.WalletManager;
import com.budiyev.android.codescanner.*;
import com.bumptech.glide.*;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.material.appbar.AppBarLayout;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import com.google.firebase.storage.*;
import java.io.*;
import java.math.BigDecimal;
import java.text.*;
import java.util.*;
import java.util.Calendar;
import java.util.HashMap;
import java.util.regex.*;
import org.json.*;

public class CreateNewGroupActivity extends AppCompatActivity {
	
	private FirebaseDatabase _firebase = FirebaseDatabase.getInstance();
	
	private Toolbar _toolbar;
	private AppBarLayout _app_bar;
	private CoordinatorLayout _coordinator;
	
	private NestedScrollView vscroll1;
	private LinearLayout linear1;
	private TextView textview1;
	private TextView textview11;
	private LinearLayout linear_stepper;
	private CardView cardview1;
	private CardView cardview11;
	private CardView cardview_type;
	private CardView card_rotating_details;
	private CardView card_fixed_details;
	private CardView card_accumulative_details;
	private boolean allowLoans = false;
	private final java.util.List<androidx.appcompat.widget.SwitchCompat> loanSwitches = new java.util.ArrayList<>();
	private CardView cardview13;
	private CardView card_permissions;
	private CardView cardview14;
	private LinearLayout linear58;
	private View step_dot_1;
	private View view61;
	private View step_dot_2;
	private View view62;
	private View step_dot_3;
	private View view63;
	private View step_dot_4;
	private LinearLayout linear2;
	private LinearLayout linear64;
	private TextView textview62;
	private EditText group_name_edittext1;
	private TextView textview63;
	private TextView textview64;
	private EditText description_edittext5;
	private ImageView imageview24;
	private TextView textview12;
	private LinearLayout linear17;
	private LinearLayout linear65;
	private TextView textview65;
	private TextView textview66;
	private CardView cardview2;
	private ImageView imageview25;
	private TextView textview13;
	private LinearLayout linear59;
	private ListView listview1;
	private LinearLayout linear48;
	private EditText rules_edittext2;
	private Button button1;
	private LinearLayout linear66;
	private LinearLayout linear67;
	private TextView textview71;
	private CardView card_type_rotating;
	private CardView card_type_fixed;
	private CardView card_type_accumulative;
	private ImageView imageview26;
	private TextView textview70;
	private LinearLayout linear_type_rotating_inner;
	private ImageView imageview27;
	private LinearLayout linear68;
	private ImageView check_type_rotating;
	private TextView textview72;
	private TextView textview73;
	private LinearLayout linear_type_fixed_inner;
	private ImageView imageview28;
	private LinearLayout linear69;
	private ImageView check_type_fixed;
	private TextView textview74;
	private TextView textview75;
	private LinearLayout linear_type_accumulative_inner;
	private ImageView imageview29;
	private LinearLayout linear70;
	private ImageView check_type_accumulative;
	private TextView textview76;
	private TextView textview77;
	private LinearLayout linear71;
	private LinearLayout linear72;
	private TextView textview79;
	private Spinner spinner_frequency;
	private LinearLayout linear73;
	private TextView textview82;
	private TextView textview83;
	private LinearLayout linear76;
	private LinearLayout linear79;
	private LinearLayout linear82;
	private LinearLayout fields_random;
	private LinearLayout fields_fcfs;
	private LinearLayout fields_admin;
	private LinearLayout fields_auction;
	private LinearLayout fields_pool_universal;
	private ImageView imageview30;
	private TextView textview78;
	private LinearLayout linear74;
	private LinearLayout linear75;
	private TextView textview80;
	private EditText contribution_amount_edittext3;
	private TextView textview81;
	private EditText no_of_slots_edittext4;
	private CardView dist_card_random;
	private CardView dist_card_fcfs;
	private LinearLayout linear77;
	private ImageView imageview31;
	private TextView textview84;
	private TextView textview85;
	private LinearLayout linear78;
	private ImageView imageview32;
	private TextView textview86;
	private TextView textview87;
	private CardView dist_card_admin;
	private CardView dist_card_fixed_order;
	private LinearLayout linear80;
	private ImageView imageview33;
	private TextView textview88;
	private TextView textview89;
	private LinearLayout linear81;
	private ImageView imageview34;
	private TextView textview90;
	private TextView textview91;
	private CardView dist_card_auction;
	private View view84;
	private LinearLayout linear83;
	private ImageView imageview35;
	private TextView textview92;
	private TextView textview93;
	private TextView textview94;
	private Spinner spinner_draw_timing;
	private TextView textview95;
	private TextView textview96;
	private EditText claim_window_edittext;
	private ImageView imageview36;
	private TextView textview97;
	private LinearLayout linear85;
	private TextView textview100;
	private EditText bidding_window_edittext;
	private LinearLayout linear88;
	private LinearLayout linear86;
	private LinearLayout linear87;
	private TextView textview98;
	private EditText max_bid_cap_edittext;
	private TextView textview99;
	private EditText commission_edittext;
	private ImageView imageview37;
	private TextView textview101;
	private View view89;
	private TextView textview102;
	private LinearLayout linear90;
	private TextView textview105;
	private Spinner spinner_default_consequence;
	private LinearLayout linear91;
	private LinearLayout linear92;
	private TextView textview103;
	private EditText grace_period_edittext;
	private TextView textview104;
	private EditText min_trust_score_edittext;
	private LinearLayout linear93;
	private LinearLayout linear94;
	private TextView textview107;
	private Spinner spinner_frequency_fixed;
	private TextView textview108;
	private EditText contribution_amount_fixed_edittext;
	private TextView textview109;
	private EditText no_of_slots_fixed_edittext;
	private TextView textview110;
	private TextView textview111;
	private EditText target_goal_edittext;
	private TextView textview112;
	private Spinner spinner_disbursement_rule;
	private ImageView imageview38;
	private TextView textview106;
	private LinearLayout linear95;
	private LinearLayout linear96;
	private TextView textview114;
	private Spinner spinner_frequency_accumulative;
	private TextView textview115;
	private EditText contribution_amount_accumulative_edittext;
	private TextView textview116;
	private EditText no_of_slots_accumulative_edittext;
	private LinearLayout linear97;
	private LinearLayout linear4;
	private ImageView imageview39;
	private TextView textview113;
	private LinearLayout linear98;
	private LinearLayout linear99;
	private TextView textview117;
	private EditText loan_interest_rate_edittext;
	private TextView textview118;
	private Spinner spinner_withdrawal_rule;
	private TextView textview5;
	private Spinner spinner_loan_interest_period;
	private TextView textview6;
	private LinearLayout linear19;
	private LinearLayout linear100;
	private LinearLayout linear55;
	private TextView textview121;
	private Spinner spinner_visibility;
	private ImageView imageview40;
	private TextView textview15;
	private LinearLayout linear57;
	private LinearLayout linear_city;
	private TextView textview119;
	private Spinner spinner_state;
	private TextView textview120;
	private Spinner spinner_city;
	private LinearLayout linear101;
	private LinearLayout linear102;
	private LinearLayout linear103;
	private LinearLayout fields_approval_method;
	private View view105;
	private LinearLayout linear106;
	private View view108;
	private LinearLayout linear109;
	private View view111;
	private LinearLayout linear112;
	private ImageView imageview41;
	private TextView textview122;
	private ImageView imageview42;
	private LinearLayout linear104;
	private SwitchCompat switch_require_approval;
	private TextView textview123;
	private TextView textview124;
	private TextView textview7;
	private Spinner spinner_approval_method;
	private TextView textview_approval_method_note;
	private ImageView imageview43;
	private LinearLayout linear107;
	private SwitchCompat switch_members_invite;
	private TextView textview125;
	private TextView textview126;
	private ImageView imageview44;
	private LinearLayout linear110;
	private SwitchCompat switch_members_edit;
	private TextView textview127;
	private TextView textview128;
	private ImageView imageview45;
	private LinearLayout linear113;
	private SwitchCompat switch_invite_link;
	private TextView textview129;
	private TextView textview130;
	private LinearLayout linear_fee_notice;
	private ImageView imageview3;
	private TextView textview131;
	private CardView cardview15;
	private CardView cardview16;
	private TextView cancel_btn;
	private TextView create_group_btn;
	
	private SharedPreferences groupsp;
	private SharedPreferences authsp;
	private Calendar c = Calendar.getInstance();
	private Vibrator v;
	private Intent intent = new Intent();
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
	private DatabaseReference userdb = _firebase.getReference("userdb");
	private ChildEventListener _userdb_child_listener;
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.create_new_group);
		initialize(_savedInstanceState);
		FirebaseApp.initializeApp(this);
		initializeLogic();
		boolean lock = getIntent() != null && getIntent().getBooleanExtra("lockType", false);
		String preselected = getIntent() != null ? getIntent().getStringExtra("groupType") : null;
		if (lock && preselected != null && !preselected.isEmpty()) {
			_selectGroupType(preselected);
			_lockToType(preselected);
		} else {
			_showTypeChooserOnly();
		}
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
		vscroll1 = findViewById(R.id.vscroll1);
		linear1 = findViewById(R.id.linear1);
		textview1 = findViewById(R.id.textview1);
		textview11 = findViewById(R.id.textview11);
		linear_stepper = findViewById(R.id.linear_stepper);
		cardview1 = findViewById(R.id.cardview1);
		cardview11 = findViewById(R.id.cardview11);
		cardview_type = findViewById(R.id.cardview_type);
		card_rotating_details = findViewById(R.id.card_rotating_details);
		card_fixed_details = findViewById(R.id.card_fixed_details);
		card_accumulative_details = findViewById(R.id.card_accumulative_details);
		_addLoanToggles();
		cardview13 = findViewById(R.id.cardview13);
		card_permissions = findViewById(R.id.card_permissions);
		cardview14 = findViewById(R.id.cardview14);
		linear58 = findViewById(R.id.linear58);
		step_dot_1 = findViewById(R.id.step_dot_1);
		view61 = findViewById(R.id.view61);
		step_dot_2 = findViewById(R.id.step_dot_2);
		view62 = findViewById(R.id.view62);
		step_dot_3 = findViewById(R.id.step_dot_3);
		view63 = findViewById(R.id.view63);
		step_dot_4 = findViewById(R.id.step_dot_4);
		linear2 = findViewById(R.id.linear2);
		linear64 = findViewById(R.id.linear64);
		textview62 = findViewById(R.id.textview62);
		group_name_edittext1 = findViewById(R.id.group_name_edittext1);
		textview63 = findViewById(R.id.textview63);
		textview64 = findViewById(R.id.textview64);
		description_edittext5 = findViewById(R.id.description_edittext5);
		imageview24 = findViewById(R.id.imageview24);
		textview12 = findViewById(R.id.textview12);
		linear17 = findViewById(R.id.linear17);
		linear65 = findViewById(R.id.linear65);
		textview65 = findViewById(R.id.textview65);
		textview66 = findViewById(R.id.textview66);
		cardview2 = findViewById(R.id.cardview2);
		imageview25 = findViewById(R.id.imageview25);
		textview13 = findViewById(R.id.textview13);
		linear59 = findViewById(R.id.linear59);
		listview1 = findViewById(R.id.listview1);
		linear48 = findViewById(R.id.linear48);
		rules_edittext2 = findViewById(R.id.rules_edittext2);
		button1 = findViewById(R.id.button1);
		linear66 = findViewById(R.id.linear66);
		linear67 = findViewById(R.id.linear67);
		textview71 = findViewById(R.id.textview71);
		card_type_rotating = findViewById(R.id.card_type_rotating);
		card_type_fixed = findViewById(R.id.card_type_fixed);
		card_type_accumulative = findViewById(R.id.card_type_accumulative);
		imageview26 = findViewById(R.id.imageview26);
		textview70 = findViewById(R.id.textview70);
		linear_type_rotating_inner = findViewById(R.id.linear_type_rotating_inner);
		imageview27 = findViewById(R.id.imageview27);
		linear68 = findViewById(R.id.linear68);
		check_type_rotating = findViewById(R.id.check_type_rotating);
		textview72 = findViewById(R.id.textview72);
		textview73 = findViewById(R.id.textview73);
		linear_type_fixed_inner = findViewById(R.id.linear_type_fixed_inner);
		imageview28 = findViewById(R.id.imageview28);
		linear69 = findViewById(R.id.linear69);
		check_type_fixed = findViewById(R.id.check_type_fixed);
		textview74 = findViewById(R.id.textview74);
		textview75 = findViewById(R.id.textview75);
		linear_type_accumulative_inner = findViewById(R.id.linear_type_accumulative_inner);
		imageview29 = findViewById(R.id.imageview29);
		linear70 = findViewById(R.id.linear70);
		check_type_accumulative = findViewById(R.id.check_type_accumulative);
		textview76 = findViewById(R.id.textview76);
		textview77 = findViewById(R.id.textview77);
		linear71 = findViewById(R.id.linear71);
		linear72 = findViewById(R.id.linear72);
		textview79 = findViewById(R.id.textview79);
		spinner_frequency = findViewById(R.id.spinner_frequency);
		linear73 = findViewById(R.id.linear73);
		textview82 = findViewById(R.id.textview82);
		textview83 = findViewById(R.id.textview83);
		linear76 = findViewById(R.id.linear76);
		linear79 = findViewById(R.id.linear79);
		linear82 = findViewById(R.id.linear82);
		fields_random = findViewById(R.id.fields_random);
		fields_fcfs = findViewById(R.id.fields_fcfs);
		fields_admin = findViewById(R.id.fields_admin);
		fields_auction = findViewById(R.id.fields_auction);
		fields_pool_universal = findViewById(R.id.fields_pool_universal);
		imageview30 = findViewById(R.id.imageview30);
		textview78 = findViewById(R.id.textview78);
		linear74 = findViewById(R.id.linear74);
		linear75 = findViewById(R.id.linear75);
		textview80 = findViewById(R.id.textview80);
		contribution_amount_edittext3 = findViewById(R.id.contribution_amount_edittext3);
		textview81 = findViewById(R.id.textview81);
		no_of_slots_edittext4 = findViewById(R.id.no_of_slots_edittext4);
		dist_card_random = findViewById(R.id.dist_card_random);
		dist_card_fcfs = findViewById(R.id.dist_card_fcfs);
		linear77 = findViewById(R.id.linear77);
		imageview31 = findViewById(R.id.imageview31);
		textview84 = findViewById(R.id.textview84);
		textview85 = findViewById(R.id.textview85);
		linear78 = findViewById(R.id.linear78);
		imageview32 = findViewById(R.id.imageview32);
		textview86 = findViewById(R.id.textview86);
		textview87 = findViewById(R.id.textview87);
		dist_card_admin = findViewById(R.id.dist_card_admin);
		dist_card_fixed_order = findViewById(R.id.dist_card_fixed_order);
		linear80 = findViewById(R.id.linear80);
		imageview33 = findViewById(R.id.imageview33);
		textview88 = findViewById(R.id.textview88);
		textview89 = findViewById(R.id.textview89);
		linear81 = findViewById(R.id.linear81);
		imageview34 = findViewById(R.id.imageview34);
		textview90 = findViewById(R.id.textview90);
		textview91 = findViewById(R.id.textview91);
		dist_card_auction = findViewById(R.id.dist_card_auction);
		view84 = findViewById(R.id.view84);
		linear83 = findViewById(R.id.linear83);
		imageview35 = findViewById(R.id.imageview35);
		textview92 = findViewById(R.id.textview92);
		textview93 = findViewById(R.id.textview93);
		textview94 = findViewById(R.id.textview94);
		spinner_draw_timing = findViewById(R.id.spinner_draw_timing);
		textview95 = findViewById(R.id.textview95);
		textview96 = findViewById(R.id.textview96);
		claim_window_edittext = findViewById(R.id.claim_window_edittext);
		imageview36 = findViewById(R.id.imageview36);
		textview97 = findViewById(R.id.textview97);
		linear85 = findViewById(R.id.linear85);
		textview100 = findViewById(R.id.textview100);
		bidding_window_edittext = findViewById(R.id.bidding_window_edittext);
		linear88 = findViewById(R.id.linear88);
		linear86 = findViewById(R.id.linear86);
		linear87 = findViewById(R.id.linear87);
		textview98 = findViewById(R.id.textview98);
		max_bid_cap_edittext = findViewById(R.id.max_bid_cap_edittext);
		textview99 = findViewById(R.id.textview99);
		commission_edittext = findViewById(R.id.commission_edittext);
		imageview37 = findViewById(R.id.imageview37);
		textview101 = findViewById(R.id.textview101);
		view89 = findViewById(R.id.view89);
		textview102 = findViewById(R.id.textview102);
		linear90 = findViewById(R.id.linear90);
		textview105 = findViewById(R.id.textview105);
		spinner_default_consequence = findViewById(R.id.spinner_default_consequence);
		linear91 = findViewById(R.id.linear91);
		linear92 = findViewById(R.id.linear92);
		textview103 = findViewById(R.id.textview103);
		grace_period_edittext = findViewById(R.id.grace_period_edittext);
		textview104 = findViewById(R.id.textview104);
		min_trust_score_edittext = findViewById(R.id.min_trust_score_edittext);
		linear93 = findViewById(R.id.linear93);
		linear94 = findViewById(R.id.linear94);
		textview107 = findViewById(R.id.textview107);
		spinner_frequency_fixed = findViewById(R.id.spinner_frequency_fixed);
		textview108 = findViewById(R.id.textview108);
		contribution_amount_fixed_edittext = findViewById(R.id.contribution_amount_fixed_edittext);
		textview109 = findViewById(R.id.textview109);
		no_of_slots_fixed_edittext = findViewById(R.id.no_of_slots_fixed_edittext);
		textview110 = findViewById(R.id.textview110);
		textview111 = findViewById(R.id.textview111);
		target_goal_edittext = findViewById(R.id.target_goal_edittext);
		textview112 = findViewById(R.id.textview112);
		spinner_disbursement_rule = findViewById(R.id.spinner_disbursement_rule);
		imageview38 = findViewById(R.id.imageview38);
		textview106 = findViewById(R.id.textview106);
		linear95 = findViewById(R.id.linear95);
		linear96 = findViewById(R.id.linear96);
		textview114 = findViewById(R.id.textview114);
		spinner_frequency_accumulative = findViewById(R.id.spinner_frequency_accumulative);
		textview115 = findViewById(R.id.textview115);
		contribution_amount_accumulative_edittext = findViewById(R.id.contribution_amount_accumulative_edittext);
		textview116 = findViewById(R.id.textview116);
		no_of_slots_accumulative_edittext = findViewById(R.id.no_of_slots_accumulative_edittext);
		linear97 = findViewById(R.id.linear97);
		linear4 = findViewById(R.id.linear4);
		imageview39 = findViewById(R.id.imageview39);
		textview113 = findViewById(R.id.textview113);
		linear98 = findViewById(R.id.linear98);
		linear99 = findViewById(R.id.linear99);
		textview117 = findViewById(R.id.textview117);
		loan_interest_rate_edittext = findViewById(R.id.loan_interest_rate_edittext);
		textview118 = findViewById(R.id.textview118);
		spinner_withdrawal_rule = findViewById(R.id.spinner_withdrawal_rule);
		textview5 = findViewById(R.id.textview5);
		spinner_loan_interest_period = findViewById(R.id.spinner_loan_interest_period);
		textview6 = findViewById(R.id.textview6);
		linear19 = findViewById(R.id.linear19);
		linear100 = findViewById(R.id.linear100);
		linear55 = findViewById(R.id.linear55);
		textview121 = findViewById(R.id.textview121);
		spinner_visibility = findViewById(R.id.spinner_visibility);
		imageview40 = findViewById(R.id.imageview40);
		textview15 = findViewById(R.id.textview15);
		linear57 = findViewById(R.id.linear57);
		linear_city = findViewById(R.id.linear_city);
		textview119 = findViewById(R.id.textview119);
		spinner_state = findViewById(R.id.spinner_state);
		textview120 = findViewById(R.id.textview120);
		spinner_city = findViewById(R.id.spinner_city);
		linear101 = findViewById(R.id.linear101);
		linear102 = findViewById(R.id.linear102);
		linear103 = findViewById(R.id.linear103);
		fields_approval_method = findViewById(R.id.fields_approval_method);
		view105 = findViewById(R.id.view105);
		linear106 = findViewById(R.id.linear106);
		view108 = findViewById(R.id.view108);
		linear109 = findViewById(R.id.linear109);
		view111 = findViewById(R.id.view111);
		linear112 = findViewById(R.id.linear112);
		imageview41 = findViewById(R.id.imageview41);
		textview122 = findViewById(R.id.textview122);
		imageview42 = findViewById(R.id.imageview42);
		linear104 = findViewById(R.id.linear104);
		switch_require_approval = findViewById(R.id.switch_require_approval);
		textview123 = findViewById(R.id.textview123);
		textview124 = findViewById(R.id.textview124);
		textview7 = findViewById(R.id.textview7);
		spinner_approval_method = findViewById(R.id.spinner_approval_method);
		textview_approval_method_note = findViewById(R.id.textview_approval_method_note);
		imageview43 = findViewById(R.id.imageview43);
		linear107 = findViewById(R.id.linear107);
		switch_members_invite = findViewById(R.id.switch_members_invite);
		textview125 = findViewById(R.id.textview125);
		textview126 = findViewById(R.id.textview126);
		imageview44 = findViewById(R.id.imageview44);
		linear110 = findViewById(R.id.linear110);
		switch_members_edit = findViewById(R.id.switch_members_edit);
		textview127 = findViewById(R.id.textview127);
		textview128 = findViewById(R.id.textview128);
		imageview45 = findViewById(R.id.imageview45);
		linear113 = findViewById(R.id.linear113);
		switch_invite_link = findViewById(R.id.switch_invite_link);
		textview129 = findViewById(R.id.textview129);
		textview130 = findViewById(R.id.textview130);
		linear_fee_notice = findViewById(R.id.linear_fee_notice);
		imageview3 = findViewById(R.id.imageview3);
		textview131 = findViewById(R.id.textview131);
		cardview15 = findViewById(R.id.cardview15);
		cardview16 = findViewById(R.id.cardview16);
		cancel_btn = findViewById(R.id.cancel_btn);
		create_group_btn = findViewById(R.id.create_group_btn);
		groupsp = getSharedPreferences("groupsp", Activity.MODE_PRIVATE);
		authsp = getSharedPreferences("authsp", Activity.MODE_PRIVATE);
		v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		fauth = FirebaseAuth.getInstance();
		
		button1.setOnClickListener(_v -> {
			if (!(rules_edittext2.getText().toString().trim().equals(""))) {
				{
					HashMap<String, Object> _item = new HashMap<>();
					_item.put("rules", rules_edittext2.getText().toString().trim());
					rules_l_map.add(_item);
				}
				listview1.setAdapter(new Listview1Adapter(rules_l_map));
				((BaseAdapter)listview1.getAdapter()).notifyDataSetChanged();
				rules_edittext2.setText("");
			}
		});
		
		card_type_rotating.setOnClickListener(_v -> _openType(CreateRotatingGroupActivity.class));
		card_type_fixed.setOnClickListener(_v -> _openType(CreateFixedGroupActivity.class));
		card_type_accumulative.setOnClickListener(_v -> _openType(CreateAccumulativeGroupActivity.class));
		
		spinner_frequency.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> _param1, View _param2, int _param3, long _param4) {
				final int _position = _param3;
				if (_param2 != null) {
					((TextView)_param2).setTextColor(getResources().getColor(R.color.colorTextPrimary));
				}
				frequencyString = _position == 0 ? "" : frequencyList.get(_position);
			}
			@Override
			public void onNothingSelected(AdapterView<?> _param1) {}
		});
		
		dist_card_random.setOnClickListener(_v -> _selectDistributionMethod("Random Ballot"));
		dist_card_fcfs.setOnClickListener(_v -> _selectDistributionMethod("First Come First Serve"));
		dist_card_admin.setOnClickListener(_v -> _selectDistributionMethod("Admin Assigned"));
		dist_card_fixed_order.setOnClickListener(_v -> _selectDistributionMethod("Fixed Order (Join Date)"));
		dist_card_auction.setOnClickListener(_v -> _selectDistributionMethod("Auction / Bidding"));
		
		spinner_draw_timing.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> _param1, View _param2, int _param3, long _param4) {
				final int _position = _param3;
				if (_param2 != null) {
					((TextView)_param2).setTextColor(getResources().getColor(R.color.colorTextPrimary));
				}
				drawTimingString = _position == 0 ? "" : drawTimingList.get(_position);
			}
			@Override
			public void onNothingSelected(AdapterView<?> _param1) {}
		});
		
		spinner_default_consequence.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> _param1, View _param2, int _param3, long _param4) {
				final int _position = _param3;
				if (_param2 != null) {
					((TextView)_param2).setTextColor(getResources().getColor(R.color.colorTextPrimary));
				}
				defaultConsequenceString = _position == 0 ? "" : defaultConsequenceList.get(_position);
			}
			@Override
			public void onNothingSelected(AdapterView<?> _param1) {}
		});
		
		spinner_frequency_fixed.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> _param1, View _param2, int _param3, long _param4) {
				final int _position = _param3;
				if (_param2 != null) {
					((TextView)_param2).setTextColor(getResources().getColor(R.color.colorTextPrimary));
				}
				frequencyString = _position == 0 ? "" : frequencyList.get(_position);
			}
			@Override
			public void onNothingSelected(AdapterView<?> _param1) {}
		});
		
		spinner_disbursement_rule.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> _param1, View _param2, int _param3, long _param4) {
				final int _position = _param3;
				if (_param2 != null) {
					((TextView)_param2).setTextColor(getResources().getColor(R.color.colorTextPrimary));
				}
				disbursementRuleString = _position == 0 ? "" : disbursementRuleList.get(_position);
			}
			@Override
			public void onNothingSelected(AdapterView<?> _param1) {}
		});
		
		spinner_frequency_accumulative.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> _param1, View _param2, int _param3, long _param4) {
				final int _position = _param3;
				if (_param2 != null) {
					((TextView)_param2).setTextColor(getResources().getColor(R.color.colorTextPrimary));
				}
				frequencyString = _position == 0 ? "" : frequencyList.get(_position);
			}
			@Override
			public void onNothingSelected(AdapterView<?> _param1) {}
		});
		
		spinner_withdrawal_rule.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> _param1, View _param2, int _param3, long _param4) {
				final int _position = _param3;
				if (_param2 != null) {
					((TextView)_param2).setTextColor(getResources().getColor(R.color.colorTextPrimary));
				}
				withdrawalRuleString = _position == 0 ? "" : withdrawalRuleList.get(_position);
			}
			@Override
			public void onNothingSelected(AdapterView<?> _param1) {}
		});
		
		spinner_visibility.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> _param1, View _param2, int _param3, long _param4) {
				final int _position = _param3;
				if (_param2 != null) {
					((TextView)_param2).setTextColor(getResources().getColor(R.color.colorTextPrimary));
				}
				visibilityString = _position == 0 ? "" : visibilityList.get(_position);
			}
			@Override
			public void onNothingSelected(AdapterView<?> _param1) {}
		});
		
		spinner_state.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> _param1, View _param2, int _param3, long _param4) {
				final int _position = _param3;
				if (_param2 != null) {
					((TextView)_param2).setTextColor(getResources().getColor(R.color.colorTextPrimary));
				}
				stateString = _param1.getItemAtPosition(_position).toString();
				helper.setSpinnerData(spinner_city, helper.getCityNames(countryString, stateString));
			}
			@Override
			public void onNothingSelected(AdapterView<?> _param1) {}
		});
		
		spinner_city.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> _param1, View _param2, int _param3, long _param4) {
				final int _position = _param3;
				if (_param2 != null) {
					((TextView)_param2).setTextColor(getResources().getColor(R.color.colorTextPrimary));
				}
				cityString = _param1.getItemAtPosition(_position).toString();
			}
			@Override
			public void onNothingSelected(AdapterView<?> _param1) {}
		});
		
		cancel_btn.setOnClickListener(_v -> finish());
		
		create_group_btn.setOnClickListener(_v -> {
			if (!_validateGroupForm()) return;
			if ("Rotating Pool".equals(groupTypeString)) {
				_createRotatingOnChain();
			} else if ("Accumulative Funds".equals(groupTypeString)) {
				_createAccumOnChain();
			} else if ("Fixed Contribution".equals(groupTypeString)) {
				_createFixedOnChain();
			} else {
				SketchwareUtil.showMessage(getApplicationContext(), "Choose rotating, savings, or goal. The network assigns the group wallet — this phone does not create one.");
			}
		});
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
		
		ViewGroup formHost = findViewById(R.id.linear1);
		if (formHost != null) formHost.addView(com.harvana.app.social.PeoplePicker.section(this, peopleToInvite));
		if (textview1 != null) textview1.setVisibility(View.GONE);
		if (textview11 != null) textview11.setVisibility(View.GONE);
		_updateBar();
		
		SharedPreferences walletPrefs = getSharedPreferences("harv_wallet_prefs", MODE_PRIVATE);
		String savedSeed = walletPrefs.getString("seed_phrase", walletPrefs.getString("mnemonic", ""));
		
		if (savedSeed != null && !savedSeed.trim().isEmpty()) {
			try {
				activeAddress = com.harvana.app.wallet.WalletManager.deriveAddressFromSeed(savedSeed);
			} catch (Exception e) {
				activeAddress = "";
			}
		}
		
		frequencyList = new ArrayList<>(Arrays.asList("SELECT FREQUENCY", "Daily", "Weekly", "Bi-weekly", "Monthly"));
		spinner_frequency.setAdapter(new ArrayAdapter<String>(getBaseContext(), android.R.layout.simple_spinner_dropdown_item, frequencyList));
		spinner_frequency_fixed.setAdapter(new ArrayAdapter<String>(getBaseContext(), android.R.layout.simple_spinner_dropdown_item, frequencyList));
		spinner_frequency_accumulative.setAdapter(new ArrayAdapter<String>(getBaseContext(), android.R.layout.simple_spinner_dropdown_item, frequencyList));
		
		visibilityList = new ArrayList<>(Arrays.asList("SELECT VISIBILITY", "Public", "Private"));
		spinner_visibility.setAdapter(new ArrayAdapter<String>(getBaseContext(), android.R.layout.simple_spinner_dropdown_item, visibilityList));
		
		ArrayList<String> approvalMethodList = new ArrayList<>(Arrays.asList("Group Admin", "Member Vote (60% required)"));
		spinner_approval_method.setAdapter(new ArrayAdapter<String>(getBaseContext(), android.R.layout.simple_spinner_dropdown_item, approvalMethodList));
		
		fields_approval_method.setVisibility(switch_require_approval.isChecked() ? View.VISIBLE : View.GONE);
		switch_require_approval.setOnCheckedChangeListener((buttonView, isChecked) ->
		fields_approval_method.setVisibility(isChecked ? View.VISIBLE : View.GONE));
		
		spinner_approval_method.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				textview_approval_method_note.setVisibility(position == 1 ? View.VISIBLE : View.GONE);
			}
			@Override
			public void onNothingSelected(AdapterView<?> parent) {}
		});
		
		drawTimingList = new ArrayList<>(Arrays.asList("SELECT DRAW TIMING", "Once at Cycle Start", "Re-draw Each Round"));
		spinner_draw_timing.setAdapter(new ArrayAdapter<String>(getBaseContext(), android.R.layout.simple_spinner_dropdown_item, drawTimingList));
		
		defaultConsequenceList = new ArrayList<>(Arrays.asList("SELECT CONSEQUENCE", "Warning", "Penalty Fee", "Skip Next Turn", "Remove from Group"));
		spinner_default_consequence.setAdapter(new ArrayAdapter<String>(getBaseContext(), android.R.layout.simple_spinner_dropdown_item, defaultConsequenceList));
		
		disbursementRuleList = new ArrayList<>(Arrays.asList("SELECT DISBURSEMENT", "All at Once at Goal", "Admin-Triggered", "Released in Milestones", "Emergency Grants"));
		spinner_disbursement_rule.setAdapter(new ArrayAdapter<String>(getBaseContext(), android.R.layout.simple_spinner_dropdown_item, disbursementRuleList));
		
		withdrawalRuleList = new ArrayList<>(Arrays.asList("SELECT WITHDRAWAL RULE", "Anytime", "Cycle-End Only"));
		spinner_withdrawal_rule.setAdapter(new ArrayAdapter<String>(getBaseContext(), android.R.layout.simple_spinner_dropdown_item, withdrawalRuleList));
		
		ArrayList<String> loanInterestPeriodList = new ArrayList<>(Arrays.asList("Monthly", "Daily", "Annually"));
		spinner_loan_interest_period.setAdapter(new ArrayAdapter<String>(getBaseContext(), android.R.layout.simple_spinner_dropdown_item, loanInterestPeriodList));
		spinner_loan_interest_period.setSelection(0);
		
		helper = new LocationHelper(this);
		
		FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
		if (currentUser != null) {
			uid = currentUser.getUid();
			com.harvana.app.firestore.UserProfileManager.getInstance(this).getProfile(uid,
			new com.harvana.app.firestore.UserProfileManager.DocCallback() {
				@Override
				public void onSuccess(Map<String, Object> data) {
					runOnUiThread(() -> {
						Object countryVal = data.get("country");
						countryString = countryVal != null ? String.valueOf(countryVal) : "";
						if (!countryString.isEmpty()) {
							helper.setSpinnerData(spinner_state, helper.getStateNames(countryString));
							helper.setSpinnerData(spinner_city, new ArrayList<String>());
						}
					});
				}
				@Override
				public void onError(String error) {
					android.util.Log.w("CreateNewGroup", "Failed to load profile from Firestore: " + error);
				}
			});
		}
	}
	
	@Override
	public void onStart() {
		super.onStart();
		androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).registerReceiver(
		globalTxReceiver, 
		new android.content.IntentFilter("com.harvana.app.ACTION_WALLET_RECEIVED")
		);
	}
	
	@Override
	public void onStop() {
		super.onStop();
		try {
			androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).unregisterReceiver(globalTxReceiver);
		} catch (Exception ignored) {}
	}
	
	public void _MORE() {}
	
	private String states = "";
	private HashMap<String, Object> newGroupMap = new HashMap<>();
	private String groupTypeString = "";
	private final com.harvana.app.social.PeoplePicker.Selection peopleToInvite = new com.harvana.app.social.PeoplePicker.Selection();
	private String frequencyString = "";
	private String visibilityString = "";
	private String locationString = "";
	private HashMap<String, Object> group_member_map = new HashMap<>();
	private String uid = "";
	private HashMap<String, Object> user_v_map = new HashMap<>();
	private HashMap<String, Object> notice_v_map = new HashMap<>();
	private HashMap<String, Object> rules_v_map = new HashMap<>();
	private HashMap<String, Object> p_tracker_map = new HashMap<>();
	private String distributionString = "";
	private String cityString = "";
	private double harv_balance = 0;
	private String activeAddress = "";
	private String stateString = "";
	private String countryString = "";
	LocationHelper helper;
	private String groupAddress = "";
	private String wallet_address = "";
	private String my_group_list = "";
	private String drawTimingString = "";
	private String withdrawalRuleString = "";
	private String defaultConsequenceString = "";
	private String disbursementRuleString = "";
	
	private ArrayList<String> statesList = new ArrayList<>();
	private ArrayList<String> groupTypeList = new ArrayList<>();
	private ArrayList<String> frequencyList = new ArrayList<>();
	private ArrayList<String> visibilityList = new ArrayList<>();
	private ArrayList<HashMap<String, Object>> group_member_map_list = new ArrayList<>();
	private ArrayList<HashMap<String, Object>> group_map_list = new ArrayList<>();
	private ArrayList<HashMap<String, Object>> notice_list_map = new ArrayList<>();
	private ArrayList<HashMap<String, Object>> rules_l_map = new ArrayList<>();
	private ArrayList<HashMap<String, Object>> p_tracker_l_map = new ArrayList<>();
	private ArrayList<String> distributionList = new ArrayList<>();
	private ArrayList<String> cityList = new ArrayList<>();
	private ArrayList<HashMap<String, Object>> my_group_map = new ArrayList<>();
	private ArrayList<String> drawTimingList = new ArrayList<>();
	private ArrayList<String> defaultConsequenceList = new ArrayList<>();
	private ArrayList<String> disbursementRuleList = new ArrayList<>();
	private ArrayList<String> withdrawalRuleList = new ArrayList<>();
	
	
	public void _delete_rule_in(final double _pos, final ArrayList<HashMap<String, Object>> _lMap) {
		_lMap.remove((int)(_pos));
		listview1.setAdapter(new Listview1Adapter(_lMap));
		((BaseAdapter)listview1.getAdapter()).notifyDataSetChanged();
	}
	
	
	
	
	private void _addLoanToggles() {
		CardView[] cards = {card_fixed_details, card_accumulative_details};
		for (CardView c : cards) {
			if (c == null || c.getChildCount() == 0 || !(c.getChildAt(0) instanceof LinearLayout)) continue;
			LinearLayout host = (LinearLayout) c.getChildAt(0);
			androidx.appcompat.widget.SwitchCompat sw = new androidx.appcompat.widget.SwitchCompat(this);
			sw.setText("Allow member loans (adds a Loans tab)");
			sw.setTextColor(com.harvana.app.utils.UiColors.textPrimary());
			sw.setPadding(0, (int) (14 * getResources().getDisplayMetrics().density), 0, 0);
			sw.setOnCheckedChangeListener((b, on) -> {
				allowLoans = on;
				for (androidx.appcompat.widget.SwitchCompat o : loanSwitches) if (o != b && o.isChecked() != on) o.setChecked(on);
				_applyLoanFieldVisibility();
			});
			loanSwitches.add(sw);
			host.addView(sw);
		}
		if (card_fixed_details != null && card_fixed_details.getChildCount() > 0 && card_fixed_details.getChildAt(0) instanceof LinearLayout) {
			LinearLayout host = (LinearLayout) card_fixed_details.getChildAt(0);
			beneficiaryInput = new EditText(this);
			beneficiaryInput.setHint("Who receives the money? Wallet address (empty = your wallet)");
			beneficiaryInput.setSingleLine(true);
			beneficiaryInput.setTextSize(13);
			host.addView(beneficiaryInput);
		}
	}
	
	private void _applyLoanFieldVisibility() {
		View row = findViewById(R.id.linear97);
		if (row != null) row.setVisibility(allowLoans ? View.VISIBLE : View.GONE);
	}
	
	private void _openType(Class<?> cls) {
		Intent next = new Intent(this, cls);
		startActivity(next);
		finish();
	}

	/** No type chosen yet: this screen is only the three choices, never the combined form. */
	private void _showTypeChooserOnly() {
		_updateBar();
		if (card_rotating_details != null) card_rotating_details.setVisibility(View.GONE);
		if (card_fixed_details != null) card_fixed_details.setVisibility(View.GONE);
		if (card_accumulative_details != null) card_accumulative_details.setVisibility(View.GONE);
		if (linear1 != null) {
			for (int i = 0; i < linear1.getChildCount(); i++) {
				View child = linear1.getChildAt(i);
				int id = child.getId();
				if (id != R.id.cardview_type && id != R.id.textview1 && id != R.id.textview11) {
					child.setVisibility(View.GONE);
				}
			}
		}
		if (cardview_type != null) cardview_type.setVisibility(View.VISIBLE);
	}

	/** A dedicated create screen: only the fields that belong to this type stay on the page. */
	private void _lockToType(String type) {
		if (cardview_type != null) cardview_type.setVisibility(View.GONE);
		if (linear_stepper != null) linear_stepper.setVisibility(View.GONE);
		if (card_rotating_details != null) {
			card_rotating_details.setVisibility("Rotating Pool".equals(type) ? View.VISIBLE : View.GONE);
		}
		if (card_fixed_details != null) {
			card_fixed_details.setVisibility("Fixed Contribution".equals(type) ? View.VISIBLE : View.GONE);
		}
		if (card_accumulative_details != null) {
			card_accumulative_details.setVisibility("Accumulative Funds".equals(type) ? View.VISIBLE : View.GONE);
		}
		androidx.appcompat.app.ActionBar bar = getSupportActionBar();
		String title = "Create group";
		String blurb = "";
		if ("Rotating Pool".equals(type)) {
			title = "New rotating group";
			blurb = "Everyone pays the same amount each round. One member receives the pot each round until every slot has had a turn. 3 to 12 members. This group runs on the Harvana network.";
		} else if ("Fixed Contribution".equals(type)) {
			title = "New goal group";
			blurb = "The group saves toward one target. Each day of the month is its own box, and a payment marks that date. Money leaves only by the disbursement rule you set here.";
		} else if ("Accumulative Funds".equals(type)) {
			title = "New savings group";
			blurb = "Each member keeps their own savings. Every payment is recorded as a small box with the date written inside. Set the interest and when members may withdraw.";
		}
		if (bar != null) {
			bar.setTitle(title);
			bar.setSubtitle("You'll be the admin");
		}
		if (linear1 != null && !blurb.isEmpty()) {
			TextView intro = new TextView(this);
			intro.setText(blurb);
			intro.setTextSize(13.5f);
			intro.setTextColor(com.harvana.app.utils.UiColors.textSecondary());
			int pad = (int) (8 * getResources().getDisplayMetrics().density);
			intro.setPadding(0, 0, 0, pad);
			linear1.addView(intro, 0);
		}
	}

	private void _updateBar() {
		androidx.appcompat.app.ActionBar bar = getSupportActionBar();
		if (bar == null) return;
		bar.setTitle("Create new group");
		bar.setSubtitle(groupTypeString == null || groupTypeString.isEmpty()
			? "Choose a group type · you'll become the admin"
			: groupTypeString + " · you'll become the admin");
	}
	
	private void _selectGroupType(String type) {
		groupTypeString = type;
		_updateBar();
		boolean rot = true;
		switch_require_approval.setEnabled(!rot);
		if (rot) { switch_require_approval.setChecked(false); }
		boolean defaultLoans = "Accumulative Funds".equals(type);
		for (androidx.appcompat.widget.SwitchCompat sw : loanSwitches) sw.setChecked(defaultLoans);
		allowLoans = defaultLoans;
		_applyLoanFieldVisibility();
		
		TransitionManager.beginDelayedTransition(linear1, new AutoTransition().setDuration(220));
		
		card_rotating_details.setVisibility("Rotating Pool".equals(type) ? View.VISIBLE : View.GONE);
		card_fixed_details.setVisibility("Fixed Contribution".equals(type) ? View.VISIBLE : View.GONE);
		card_accumulative_details.setVisibility("Accumulative Funds".equals(type) ? View.VISIBLE : View.GONE);
		
		check_type_rotating.setVisibility("Rotating Pool".equals(type) ? View.VISIBLE : View.INVISIBLE);
		check_type_fixed.setVisibility("Fixed Contribution".equals(type) ? View.VISIBLE : View.INVISIBLE);
		check_type_accumulative.setVisibility("Accumulative Funds".equals(type) ? View.VISIBLE : View.INVISIBLE);
		
		CardView selectedCard = "Rotating Pool".equals(type) ? card_type_rotating
		: "Fixed Contribution".equals(type) ? card_type_fixed
		: card_type_accumulative;
		ImageView selectedCheck = "Rotating Pool".equals(type) ? check_type_rotating
		: "Fixed Contribution".equals(type) ? check_type_fixed
		: check_type_accumulative;
		
		_animateCardPress(selectedCard);
		_animateCheckFadeIn(selectedCheck);
		_advanceStepper(2);
		
		if (!"Rotating Pool".equals(type)) {
			distributionString = "";
			fields_random.setVisibility(View.GONE);
			fields_fcfs.setVisibility(View.GONE);
			fields_admin.setVisibility(View.GONE);
			fields_auction.setVisibility(View.GONE);
			fields_pool_universal.setVisibility(View.GONE);
		}
	}
	
	private void _selectDistributionMethod(String method) {
		distributionString = method;
		
		int normalBg = androidx.core.content.ContextCompat.getColor(this, R.color.colorSurfaceAlt);
		int selectedBg = androidx.core.content.ContextCompat.getColor(this, R.color.colorAccentStroke);
		
		dist_card_random.setCardBackgroundColor("Random Ballot".equals(method) ? selectedBg : normalBg);
		dist_card_fcfs.setCardBackgroundColor("First Come First Serve".equals(method) ? selectedBg : normalBg);
		dist_card_admin.setCardBackgroundColor("Admin Assigned".equals(method) ? selectedBg : normalBg);
		dist_card_fixed_order.setCardBackgroundColor("Fixed Order (Join Date)".equals(method) ? selectedBg : normalBg);
		dist_card_auction.setCardBackgroundColor("Auction / Bidding".equals(method) ? selectedBg : normalBg);
		
		CardView tappedCard = "Random Ballot".equals(method) ? dist_card_random
		: "First Come First Serve".equals(method) ? dist_card_fcfs
		: "Admin Assigned".equals(method) ? dist_card_admin
		: "Fixed Order (Join Date)".equals(method) ? dist_card_fixed_order
		: dist_card_auction;
		_animateCardPress(tappedCard);
		
		TransitionManager.beginDelayedTransition(linear1, new AutoTransition().setDuration(220));
		
		fields_random.setVisibility("Random Ballot".equals(method) ? View.VISIBLE : View.GONE);
		fields_fcfs.setVisibility("First Come First Serve".equals(method) ? View.VISIBLE : View.GONE);
		fields_admin.setVisibility("Admin Assigned".equals(method) ? View.VISIBLE : View.GONE);
		fields_auction.setVisibility("Auction / Bidding".equals(method) ? View.VISIBLE : View.GONE);
		
		fields_pool_universal.setVisibility(View.VISIBLE);
		_advanceStepper(3);
	}
	
	private void _animateCardPress(CardView card) {
		card.animate()
		.scaleX(0.97f).scaleY(0.97f)
		.setDuration(90)
		.withEndAction(() -> card.animate().scaleX(1f).scaleY(1f).setDuration(120).start())
		.start();
	}
	
	private void _animateCheckFadeIn(ImageView check) {
		check.setAlpha(0f);
		check.animate().alpha(1f).setDuration(180).start();
	}
	
	private void _advanceStepper(int upToStep) {
		View[] dots = { step_dot_1, step_dot_2, step_dot_3, step_dot_4 };
		for (int i = 0; i < dots.length; i++) {
			int targetDrawable = (i < upToStep) ? R.drawable.step_dot_active : R.drawable.step_dot_inactive;
			dots[i].setBackgroundResource(targetDrawable);
		}
	}
	
	private String getFirestoreString(JSONObject fields, String fieldName) {
		try {
			if (!fields.has(fieldName)) return "";
			JSONObject field = fields.getJSONObject(fieldName);
			return field.optString("stringValue", "");
		} catch (Exception e) {
			return "";
		}
	}
	
	private boolean getFirestoreBoolean(JSONObject fields, String fieldName) {
		try {
			if (!fields.has(fieldName)) return false;
			JSONObject field = fields.getJSONObject(fieldName);
			return field.optBoolean("booleanValue", false);
		} catch (Exception e) {
			return false;
		}
	}
	
	private boolean _validateGroupForm() {
		if (group_name_edittext1.getText().toString().trim().isEmpty()) {
			SketchwareUtil.showMessage(getApplicationContext(), "Please enter a group name.");
			return false;
		}
		if (description_edittext5.getText().toString().trim().isEmpty()) {
			SketchwareUtil.showMessage(getApplicationContext(), "Please add a description.");
			return false;
		}
		if (groupTypeString.trim().isEmpty()) {
			SketchwareUtil.showMessage(getApplicationContext(), "Please choose a group type.");
			return false;
		}
		
		if ("Rotating Pool".equals(groupTypeString)) {
			if (!_validateRotatingPoolFields()) return false;
		} else if ("Fixed Contribution".equals(groupTypeString)) {
			if (!_validateFixedContributionFields()) return false;
		} else if ("Accumulative Funds".equals(groupTypeString)) {
			if (!_validateAccumulativeFundsFields()) return false;
		}
		
		if (visibilityString.trim().isEmpty()) {
			SketchwareUtil.showMessage(getApplicationContext(), "Please select group visibility.");
			return false;
		}
		if (stateString.trim().isEmpty()) {
			SketchwareUtil.showMessage(getApplicationContext(), "Please select a state.");
			return false;
		}
		if (cityString.trim().isEmpty()) {
			SketchwareUtil.showMessage(getApplicationContext(), "Please select a city.");
			return false;
		}
		return true;
	}
	
	private boolean _validateRotatingPoolFields() {
		if (!("Daily".equals(frequencyString) || "Weekly".equals(frequencyString) || "Monthly".equals(frequencyString))) {
			SketchwareUtil.showMessage(getApplicationContext(), "Rotating groups run daily, weekly or monthly. Please pick one of those.");
			return false;
		}
		if (!_validatePositiveDecimal(contribution_amount_edittext3, "Please enter a valid contribution amount.")) return false;
		if (!_validateRangeInt(no_of_slots_edittext4, 3, 12, "A rotating group needs between 3 and 12 members.")) return false;
		return true;
	}
	
	private boolean _validateFixedContributionFields() {
		if (frequencyString.trim().isEmpty()) {
			SketchwareUtil.showMessage(getApplicationContext(), "Please select a contribution frequency.");
			return false;
		}
		if (!_validatePositiveDecimal(contribution_amount_fixed_edittext, "Please enter a valid contribution amount.")) return false;
		if (!_validateMinInt(no_of_slots_fixed_edittext, 2, "A group needs at least 2 members.")) return false;
		if (!_validatePositiveDecimal(target_goal_edittext, "Please enter a valid target goal amount.")) return false;
		if (disbursementRuleString.trim().isEmpty()) {
			SketchwareUtil.showMessage(getApplicationContext(), "Please select a disbursement rule.");
			return false;
		}
		return true;
	}
	
	private boolean _validateAccumulativeFundsFields() {
		if (frequencyString.trim().isEmpty()) {
			SketchwareUtil.showMessage(getApplicationContext(), "Please select a contribution frequency.");
			return false;
		}
		if (!_validatePositiveDecimal(contribution_amount_accumulative_edittext, "Please enter a valid contribution amount.")) return false;
		if (!_validateMinInt(no_of_slots_accumulative_edittext, 2, "A group needs at least 2 members.")) return false;
		if (allowLoans && !_validateRangeDecimal(loan_interest_rate_edittext, 0, 100, "Interest rate must be between 0% and 100%.")) return false;
		if (withdrawalRuleString.trim().isEmpty()) {
			SketchwareUtil.showMessage(getApplicationContext(), "Please select a withdrawal rule.");
			return false;
		}
		return true;
	}
	
	private boolean _validatePositiveDecimal(EditText field, String errorMsg) {
		String s = field.getText().toString().trim();
		if (s.isEmpty()) {
			SketchwareUtil.showMessage(getApplicationContext(), errorMsg);
			return false;
		}
		try {
			if (Double.parseDouble(s) <= 0) {
				SketchwareUtil.showMessage(getApplicationContext(), errorMsg);
				return false;
			}
		} catch (NumberFormatException e) {
			SketchwareUtil.showMessage(getApplicationContext(), errorMsg);
			return false;
		}
		return true;
	}
	
	private boolean _validateMinInt(EditText field, int min, String errorMsg) {
		String s = field.getText().toString().trim();
		if (s.isEmpty()) {
			SketchwareUtil.showMessage(getApplicationContext(), errorMsg);
			return false;
		}
		try {
			if (Integer.parseInt(s) < min) {
				SketchwareUtil.showMessage(getApplicationContext(), errorMsg);
				return false;
			}
		} catch (NumberFormatException e) {
			SketchwareUtil.showMessage(getApplicationContext(), errorMsg);
			return false;
		}
		return true;
	}
	
	private boolean _validateRangeInt(EditText field, int min, int max, String errorMsg) {
		String s = field.getText().toString().trim();
		if (s.isEmpty()) {
			SketchwareUtil.showMessage(getApplicationContext(), errorMsg);
			return false;
		}
		try {
			int val = Integer.parseInt(s);
			if (val < min || val > max) {
				SketchwareUtil.showMessage(getApplicationContext(), errorMsg);
				return false;
			}
		} catch (NumberFormatException e) {
			SketchwareUtil.showMessage(getApplicationContext(), errorMsg);
			return false;
		}
		return true;
	}
	
	private boolean _validateRangeDecimal(EditText field, double min, double max, String errorMsg) {
		String s = field.getText().toString().trim();
		if (s.isEmpty()) {
			SketchwareUtil.showMessage(getApplicationContext(), errorMsg);
			return false;
		}
		try {
			double val = Double.parseDouble(s);
			if (val < min || val > max) {
				SketchwareUtil.showMessage(getApplicationContext(), errorMsg);
				return false;
			}
		} catch (NumberFormatException e) {
			SketchwareUtil.showMessage(getApplicationContext(), errorMsg);
			return false;
		}
		return true;
	}
	
	private final BroadcastReceiver globalTxReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent != null && "com.harvana.app.ACTION_WALLET_RECEIVED".equals(intent.getAction())) {
				double amount = intent.getDoubleExtra("amount", 0.0);
				String sender = intent.getStringExtra("sender");
				
				com.harvana.app.utils.InAppNotificationHelper.showTransactionNotification(
				CreateNewGroupActivity.this,
				"Incoming Transfer",
				"Received " + String.format(Locale.US, "%.5f", amount) + (" " + com.harvana.app.CoinInfo.ticker()),
				sender
				);
			}
		}
	};
	
	private String pendingCircleId = null;
	
	private void _createRotatingOnChain() {
		final String wallet = com.harvana.app.wallet.WalletManager.getActiveWalletAddress(this);
		if (wallet == null || wallet.isEmpty() || com.harvana.app.wallet.WalletManager.loadPrivateKey(this, wallet) == null) {
			new androidx.appcompat.app.AlertDialog.Builder(this).setTitle("You need a wallet first")
			.setMessage("A rotating group runs on the network and is paid from your wallet. Create or restore a wallet, then try again.")
			.setPositiveButton("OK", null).show();
			return;
		}
		final long contributionRaw = com.harvana.app.circle.CircleUi.toRaw(contribution_amount_edittext3.getText().toString());
		final int members = Integer.parseInt(no_of_slots_edittext4.getText().toString().trim());
		if (members < 3 || members > 12) {
			SketchwareUtil.showMessage(getApplicationContext(), "A rotating group needs 3 to 12 members.");
			return;
		}
		final long cycleHours = "Daily".equals(frequencyString) ? 24L : "Weekly".equals(frequencyString) ? 168L : 720L;
		final long perMember = contributionRaw * (members - 1);
		final long creatorBond = perMember * 2L;
		final long fee = 100_000_000L;
		java.util.ArrayList<String[]> rows = new java.util.ArrayList<>();
		rows.add(new String[]{"Each round", com.harvana.app.circle.CircleUi.harv(contributionRaw) + " from every member"});
		rows.add(new String[]{"Pot each round", com.harvana.app.circle.CircleUi.harv(contributionRaw * (members - 1))});
		rows.add(new String[]{"Other members lock", com.harvana.app.circle.CircleUi.harv(perMember)});
		rows.add(new String[]{"Your bond", com.harvana.app.circle.CircleUi.harv(creatorBond)});
		rows.add(new String[]{"Creation fee", com.harvana.app.circle.CircleUi.harv(fee)});
		rows.add(new String[]{"Group wallet", "Assigned by the network. This phone does not create one."});
		rows.add(new String[]{"Payout", "Only what the other members pay. Bonds stay locked."});
		com.harvana.app.group.PaymentConfirmSheet.reviewThenAuthorize(this,
		"Create this rotating group", com.harvana.app.circle.CircleUi.harv(fee), rows, () -> {
			final ProgressDialog pd = ProgressDialog.show(this, null, "Creating on the network…", true, false);
			com.harvana.app.sync.NodeGroupClient.createCircle(getApplicationContext(), wallet, contributionRaw, members, cycleHours,
			new com.harvana.app.sync.NodeGroupClient.Callback() {
				@Override public void onSuccess(org.json.JSONObject json) {
					final String id = json.optString("circleId", "");
					if (id.isEmpty()) {
						runOnUiThread(() -> { pd.dismiss(); SketchwareUtil.showMessage(getApplicationContext(), "The network accepted it but returned no id."); });
						return;
					}
					pendingCircleId = id;
					com.harvana.app.sync.NodeGroupClient.getCircle(id, new com.harvana.app.sync.NodeGroupClient.Callback() {
						@Override public void onSuccess(org.json.JSONObject circle) {
							runOnUiThread(() -> { pd.dismiss(); _submitNewGroupData(circle.optString("escrowAddress", "")); });
						}
						@Override public void onError(String e) {
							runOnUiThread(() -> { pd.dismiss(); _submitNewGroupData(""); });
						}
					});
				}
				@Override public void onError(String error) {
					runOnUiThread(() -> { pd.dismiss(); SketchwareUtil.showMessage(getApplicationContext(), com.harvana.app.circle.CircleUi.friendly(error)); });
				}
			});
		});
	}
	
	private EditText beneficiaryInput;
	
	private static String _ruleCode(String label) {
		if ("All at Once at Goal".equals(label)) return "ALL_AT_ONCE_AT_GOAL";
		if ("Admin-Triggered".equals(label)) return "ADMIN_TRIGGERED";
		if ("Released in Milestones".equals(label)) return "MILESTONE_BASED";
		if ("Emergency Grants".equals(label)) return "EMERGENCY_GRANT";
		return label;
	}
	
	private String _requireWalletOrExplain() {
		final String wallet = com.harvana.app.wallet.WalletManager.getActiveWalletAddress(this);
		if (wallet == null || wallet.isEmpty() || com.harvana.app.wallet.WalletManager.loadPrivateKey(this, wallet) == null) {
			new androidx.appcompat.app.AlertDialog.Builder(this).setTitle("You need a wallet first")
			.setMessage("This group runs on the network and is paid from your wallet.")
			.setPositiveButton("OK", null).show();
			return null;
		}
		return wallet;
	}
	
	private void _createAccumOnChain() {
		final String wallet = _requireWalletOrExplain();
		if (wallet == null) return;
		double ratePct = 0.0;
		if (allowLoans) {
			try { ratePct = Double.parseDouble(loan_interest_rate_edittext.getText().toString().trim()); }
			catch (Exception e) {
				SketchwareUtil.showMessage(getApplicationContext(), "Enter an interest rate from 0 to 50.");
				return;
			}
		}
		if (ratePct < 0 || ratePct > 50) {
			SketchwareUtil.showMessage(getApplicationContext(), "Interest on a savings group is 0 to 50%.");
			return;
		}
		final long bps = Math.round(ratePct * 100.0);
		final String rule = "Cycle-End Only".equals(withdrawalRuleString) ? "CYCLE_END_ONLY" : "ANYTIME";
		String msg = "• Creation fee: 1 HARV\n• Interest rate: " + ratePct + "%\n• Withdrawals: " + rule;
		_confirmAndCreateOnChain("Create this savings group?", msg, cb ->
		com.harvana.app.sync.NodeGroupClient.createAccumGroup(getApplicationContext(), wallet, bps, rule, cb), "groupId", "ACCUM");
	}
	
	private void _createFixedOnChain() {
		final String wallet = _requireWalletOrExplain();
		if (wallet == null) return;
		final String rule = _ruleCode(disbursementRuleString);
		String ben = beneficiaryInput != null ? beneficiaryInput.getText().toString().trim() : "";
		if (ben.isEmpty()) ben = wallet;
		if (!"EMERGENCY_GRANT".equals(rule) && !(ben.startsWith("HV0x") && ben.length() == 44)) {
			SketchwareUtil.showMessage(getApplicationContext(), "Enter a valid wallet address for who receives the money.");
			return;
		}
		final String beneficiary = "EMERGENCY_GRANT".equals(rule) ? "" : ben;
		final long target = com.harvana.app.circle.CircleUi.toRaw(target_goal_edittext.getText().toString());
		final String name = group_name_edittext1.getText().toString().trim();
		final String desc = description_edittext5.getText().toString().trim();
		String msg = "• Goal: " + com.harvana.app.circle.CircleUi.harv(target)
				+ "\n• Paid to: " + (beneficiary.isEmpty() ? "the group, by vote" : beneficiary)
				+ "\n• When the goal is reached, the whole pot is sent to that wallet automatically. No vote."
				+ "\n• Creation fee: 1 HARV";
		_confirmAndCreateOnChain("Create this goal group?", msg, cb ->
		com.harvana.app.sync.NodeGroupClient.createFixedGroup(getApplicationContext(), wallet, target, rule, beneficiary, 0L, name, desc, cb), "groupId", "FIXED");
	}
	
	private interface NodeCreate { void run(com.harvana.app.sync.NodeGroupClient.Callback cb); }
	
	private void _confirmAndCreateOnChain(String title, String msg, NodeCreate create, String idKey, String family) {
		java.util.ArrayList<String[]> rows = new java.util.ArrayList<>();
		if (msg != null) {
			for (String line : msg.split("\n")) {
				String t = line.trim();
				if (t.startsWith("•")) t = t.substring(1).trim();
				int colon = t.indexOf(':');
				if (colon > 0) rows.add(new String[]{t.substring(0, colon).trim(), t.substring(colon + 1).trim()});
				else if (!t.isEmpty()) rows.add(new String[]{"Note", t});
			}
		}
		rows.add(new String[]{"Group wallet", "Assigned by the network. This phone does not create one."});
		com.harvana.app.group.PaymentConfirmSheet.reviewThenAuthorize(this, title, "1 HARV", rows, () -> {
			final ProgressDialog pd = ProgressDialog.show(this, null, "Creating on the network…", true, false);
			create.run(new com.harvana.app.sync.NodeGroupClient.Callback() {
				@Override public void onSuccess(org.json.JSONObject json) {
					final String id = json.optString(idKey, json.optString("circleId", ""));
					if (id.isEmpty()) {
						runOnUiThread(() -> { pd.dismiss(); SketchwareUtil.showMessage(getApplicationContext(), "The network accepted it but returned no id."); });
						return;
					}
					pendingCircleId = id;
					com.harvana.app.sync.NodeGroupClient.Callback got = new com.harvana.app.sync.NodeGroupClient.Callback() {
						@Override public void onSuccess(org.json.JSONObject g) { runOnUiThread(() -> { pd.dismiss(); _submitNewGroupData(g.optString("escrowAddress", "")); }); }
						@Override public void onError(String e) { runOnUiThread(() -> { pd.dismiss(); _submitNewGroupData(""); }); }
					};
					if ("ACCUM".equals(family)) com.harvana.app.sync.NodeGroupClient.getAccumGroup(id, got);
					else com.harvana.app.sync.NodeGroupClient.getFixedGroup(id, got);
				}
				@Override public void onError(String error) {
					runOnUiThread(() -> { pd.dismiss(); SketchwareUtil.showMessage(getApplicationContext(), com.harvana.app.circle.CircleUi.friendly(error)); });
				}
			});
		});
	}
	
	public void _submitNewGroupData(final String _realGroupWalletAddress) {
		com.harvana.app.trust.AppActivity.note(this, "GROUP", "Created a group");
		final String newGroupId = java.util.UUID.randomUUID().toString();
		final String creatorUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
		final String timestamp = String.valueOf(System.currentTimeMillis());
		
		newGroupMap = new HashMap<>();
		newGroupMap.put("groupId", newGroupId);
		newGroupMap.put("firestoreDocId", newGroupId);
		newGroupMap.put("timestamp", timestamp);
		newGroupMap.put("groupName", group_name_edittext1.getText().toString().trim());
		newGroupMap.put("groupType", groupTypeString.trim());
		newGroupMap.put("groupDescription", description_edittext5.getText().toString().trim());
		newGroupMap.put("frequency", frequencyString.trim());
		newGroupMap.put("visibility", visibilityString.trim());
		newGroupMap.put("country", countryString.trim());
		newGroupMap.put("state", stateString.trim());
		newGroupMap.put("city", cityString.trim());
		newGroupMap.put("distributionMethod", distributionString.trim());
		newGroupMap.put("creatorId", creatorUid);
		newGroupMap.put("status", "Active");
		newGroupMap.put("currentRound", (int)(1));
		newGroupMap.put("membersCount", (int)(1));
		newGroupMap.put("totalCollected", (double)(0));
		newGroupMap.put("trustScore", (int)(0));
		newGroupMap.put("groupRules", rules_l_map);
		newGroupMap.put("groupWalletAddress", _realGroupWalletAddress);
		
		final boolean rotatingOnChain = pendingCircleId != null;
		newGroupMap.put("requireApprovalToJoin", !rotatingOnChain && switch_require_approval.isChecked());
		newGroupMap.put("joinApprovalMethod", !rotatingOnChain && switch_require_approval.isChecked()
		? (spinner_approval_method.getSelectedItemPosition() == 1 ? "MEMBER_VOTE" : "ADMIN")
		: "NONE");
		newGroupMap.put("membersCanInvite", switch_members_invite.isChecked());
		newGroupMap.put("membersCanEdit", switch_members_edit.isChecked());
		newGroupMap.put("inviteViaLinkEnabled", switch_invite_link.isChecked());
		
		if ("Rotating Pool".equals(groupTypeString)) {
			newGroupMap.put("contributionAmount", Double.parseDouble(contribution_amount_edittext3.getText().toString()));
			newGroupMap.put("totalSlots", Integer.parseInt(no_of_slots_edittext4.getText().toString()));
			newGroupMap.put("drawTiming", drawTimingString.trim());
			newGroupMap.put("claimWindowHours", claim_window_edittext.getText().toString().trim());
			newGroupMap.put("maxBidCapPercent", max_bid_cap_edittext.getText().toString().trim());
			newGroupMap.put("commissionPercent", commission_edittext.getText().toString().trim());
			newGroupMap.put("biddingWindowMinutes", bidding_window_edittext.getText().toString().trim());
			newGroupMap.put("gracePeriodDays", grace_period_edittext.getText().toString().trim());
			newGroupMap.put("minTrustScoreEarlySlots", min_trust_score_edittext.getText().toString().trim());
			newGroupMap.put("defaultConsequence", defaultConsequenceString.trim());
		} else if ("Fixed Contribution".equals(groupTypeString)) {
			newGroupMap.put("contributionAmount", Double.parseDouble(contribution_amount_fixed_edittext.getText().toString()));
			newGroupMap.put("totalSlots", Integer.parseInt(no_of_slots_fixed_edittext.getText().toString()));
			newGroupMap.put("targetGoalAmount", Double.parseDouble(target_goal_edittext.getText().toString()));
			newGroupMap.put("disbursementRule", disbursementRuleString.trim());
		} else if ("Accumulative Funds".equals(groupTypeString)) {
			newGroupMap.put("contributionAmount", Double.parseDouble(contribution_amount_accumulative_edittext.getText().toString()));
			newGroupMap.put("totalSlots", Integer.parseInt(no_of_slots_accumulative_edittext.getText().toString()));
			newGroupMap.put("loanInterestRate", allowLoans ? Double.parseDouble(loan_interest_rate_edittext.getText().toString()) : 0.0);
			newGroupMap.put("loanInterestPeriod",
			spinner_loan_interest_period.getSelectedItem() != null
			? spinner_loan_interest_period.getSelectedItem().toString().toUpperCase()
			: "MONTHLY");
			newGroupMap.put("withdrawalRule", withdrawalRuleString.trim());
		}
		
		if (pendingCircleId != null) {
			newGroupMap.put("nodeGroupId", pendingCircleId);
			newGroupMap.put("family", com.harvana.app.group.GroupKinds.familyOf(pendingCircleId));
			if (pendingCircleId.startsWith("CIRC-")) newGroupMap.put("circleId", pendingCircleId);
			newGroupMap.put("onChain", true);
		}
		newGroupMap.put("allowLoans", allowLoans);
		if (allowLoans && !newGroupMap.containsKey("loanInterestRate")) {
			newGroupMap.put("loanInterestRate", 0.0);
			newGroupMap.put("loanInterestPeriod", "MONTHLY");
		}
		
		final ProgressDialog groupProgress = new ProgressDialog(CreateNewGroupActivity.this);
		groupProgress.setMessage("Saving your group...");
		groupProgress.setCancelable(false);
		groupProgress.show();
		
		final boolean onNode = pendingCircleId != null;
		if (onNode) com.harvana.app.group.GroupSaver.queue(getApplicationContext(), creatorUid, newGroupId, newGroupMap);
		
		com.harvana.app.group.GroupSaver.save(getApplicationContext(), creatorUid, newGroupId, newGroupMap, error -> runOnUiThread(() -> {
			if (!CreateNewGroupActivity.this.isFinishing() && groupProgress.isShowing()) groupProgress.dismiss();
			if (error != null) {
				if (onNode) {
					SketchwareUtil.showMessage(getApplicationContext(), "Your group exists on the network, but saving its details failed. Group ID: " + pendingCircleId);
					finish();
				} else {
					SketchwareUtil.showMessage(getApplicationContext(), error);
				}
				return;
			}
			if (onNode) com.harvana.app.group.GroupSaver.unqueue(getApplicationContext(), newGroupId);
			
			com.harvana.app.social.PeoplePicker.sendAll(CreateNewGroupActivity.this, newGroupId, newGroupMap, peopleToInvite, "", (sent, failed) -> { });
			
			intent.putExtra("groupId", newGroupId);
			intent.setClass(getApplicationContext(), GroupDetailsActivity.class);
			startActivity(intent);
			finish();
		}));
	}
	
	public class Listview1Adapter extends BaseAdapter {
		ArrayList<HashMap<String, Object>> _data;
		
		public Listview1Adapter(ArrayList<HashMap<String, Object>> _arr) {
			_data = _arr;
		}
		
		@Override
		public int getCount() { return _data.size(); }
		
		@Override
		public HashMap<String, Object> getItem(int _index) { return _data.get(_index); }
		
		@Override
		public long getItemId(int _index) { return _index; }
		
		@Override
		public View getView(final int _position, View _v, ViewGroup _container) {
			LayoutInflater _inflater = getLayoutInflater();
			View _view = _v;
			if (_view == null) {
				_view = _inflater.inflate(R.layout.rules_custom_card, null);
			}
			
			final TextView rule_no_textview1 = _view.findViewById(R.id.rule_no_textview1);
			final TextView rules_textview2 = _view.findViewById(R.id.rules_textview2);
			final ImageView imageview1 = _view.findViewById(R.id.imageview1);
			
			rule_no_textview1.setText(String.valueOf((long)(_position + 1)));
			rules_textview2.setText(_data.get((int) _position).get("rules").toString());
			imageview1.setOnClickListener(_view1 -> _delete_rule_in(_position, rules_l_map));
			
			return _view;
		}
	}
}
