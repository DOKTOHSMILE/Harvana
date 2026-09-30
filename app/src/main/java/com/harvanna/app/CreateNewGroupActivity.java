package com.harvanna.app;

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
import android.util.Base64;
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
import com.harvanna.app.crypto.Ed25519;
import com.harvanna.app.tx.TransactionRouter;
import com.harvanna.app.wallet.SecureWalletStorage;
import com.harvanna.app.wallet.WalletGenerator;
import com.harvanna.app.wallet.WalletManager;
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
		// Opened from the "Create a group" picker with the type already chosen.
		String preselected = getIntent().getStringExtra("groupType");
		if (preselected != null && !preselected.isEmpty()) {
			_selectGroupType(preselected);
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
		
		card_type_rotating.setOnClickListener(_v -> _selectGroupType("Rotating Pool"));
		
		card_type_fixed.setOnClickListener(_v -> _selectGroupType("Fixed Contribution"));
		
		card_type_accumulative.setOnClickListener(_v -> _selectGroupType("Accumulative Funds"));
		
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
			public void onNothingSelected(AdapterView<?> _param1) {
				
			}
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
			public void onNothingSelected(AdapterView<?> _param1) {
				
			}
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
			public void onNothingSelected(AdapterView<?> _param1) {
				
			}
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
			public void onNothingSelected(AdapterView<?> _param1) {
				
			}
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
			public void onNothingSelected(AdapterView<?> _param1) {
				
			}
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
			public void onNothingSelected(AdapterView<?> _param1) {
				
			}
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
			public void onNothingSelected(AdapterView<?> _param1) {
				
			}
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
			public void onNothingSelected(AdapterView<?> _param1) {
				
			}
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
			public void onNothingSelected(AdapterView<?> _param1) {
				
			}
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
			public void onNothingSelected(AdapterView<?> _param1) {
				
			}
		});
		
		cancel_btn.setOnClickListener(_v -> finish());
		
		create_group_btn.setOnClickListener(_v -> {
			if (!_validateGroupForm()) return;
			_showCreateWalletBottomSheet();
		});
		
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
		androidx.appcompat.app.ActionBar ab = getSupportActionBar();
		int backgroundColor = androidx.core.content.ContextCompat.getColor(this, R.color.colorBackground);
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			final Window window = this.getWindow();
			window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
			window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
			com.harvanna.app.utils.StatusBarUtil.matchToolbar(this);
		}
		
		setTitle("CREATE NEW GROUP");
		// Load active wallet address
		SharedPreferences walletPrefs = getSharedPreferences("harv_wallet_prefs", MODE_PRIVATE);
		String savedSeed = walletPrefs.getString("seed_phrase", walletPrefs.getString("mnemonic", ""));
		
		if (savedSeed != null && !savedSeed.trim().isEmpty()) {
			try {
				activeAddress = com.harvanna.app.wallet.WalletManager.deriveAddressFromSeed(savedSeed);
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
		
		// Only relevant when approval is required at all — mirrors the
		// switch's own state, defaulting to shown since the switch defaults checked.
		fields_approval_method.setVisibility(switch_require_approval.isChecked() ? View.VISIBLE : View.GONE);
		switch_require_approval.setOnCheckedChangeListener((buttonView, isChecked) ->
		fields_approval_method.setVisibility(isChecked ? View.VISIBLE : View.GONE));
		
		spinner_approval_method.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				// Only "Member Vote" (position 1) needs the explanatory note
				textview_approval_method_note.setVisibility(position == 1 ? View.VISIBLE : View.GONE);
			}
			@Override
			public void onNothingSelected(AdapterView<?> parent) {}
		});
		
		drawTimingList = new ArrayList<>(Arrays.asList("SELECT DRAW TIMING", "Once at Cycle Start", "Re-draw Each Round"));
		spinner_draw_timing.setAdapter(new ArrayAdapter<String>(getBaseContext(), android.R.layout.simple_spinner_dropdown_item, drawTimingList));
		
		defaultConsequenceList = new ArrayList<>(Arrays.asList("SELECT CONSEQUENCE", "Warning", "Penalty Fee", "Skip Next Turn", "Remove from Group"));
		spinner_default_consequence.setAdapter(new ArrayAdapter<String>(getBaseContext(), android.R.layout.simple_spinner_dropdown_item, defaultConsequenceList));
		
		disbursementRuleList = new ArrayList<>(Arrays.asList("SELECT DISBURSEMENT", "All at Once at Goal", "Admin-Triggered"));
		spinner_disbursement_rule.setAdapter(new ArrayAdapter<String>(getBaseContext(), android.R.layout.simple_spinner_dropdown_item, disbursementRuleList));
		
		withdrawalRuleList = new ArrayList<>(Arrays.asList("SELECT WITHDRAWAL RULE", "Anytime", "Cycle-End Only"));
		spinner_withdrawal_rule.setAdapter(new ArrayAdapter<String>(getBaseContext(), android.R.layout.simple_spinner_dropdown_item, withdrawalRuleList));
		
		ArrayList<String> loanInterestPeriodList = new ArrayList<>(Arrays.asList("Monthly", "Daily", "Annually"));
		spinner_loan_interest_period.setAdapter(new ArrayAdapter<String>(getBaseContext(), android.R.layout.simple_spinner_dropdown_item, loanInterestPeriodList));
		// Monthly is the sensible default for micro-loan interest, matching
		// common Osusu/Ajo lending convention - not left unselected like the
		// other spinners, since there's a genuinely reasonable default here.
		spinner_loan_interest_period.setSelection(0);
		
		helper = new LocationHelper(this);
		
		FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
		if (currentUser != null) {
			uid = currentUser.getUid();
			com.harvanna.app.firestore.UserProfileManager.getInstance(this).getProfile(uid,
			new com.harvanna.app.firestore.UserProfileManager.DocCallback() {
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
		// Add to onStart
		androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).registerReceiver(
		globalTxReceiver, 
		new android.content.IntentFilter("com.harvanna.app.ACTION_WALLET_RECEIVED")
		);
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
	private String states = "";
	private HashMap<String, Object> newGroupMap = new HashMap<>();
	private String groupTypeString = "";
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
	private static final double GROUP_CREATION_FEE = 5;
	private String activeAddress = "";
	// TODO(observations): unused legacy AJ0x fee collector address; needs a real HV0x address if ever used.
	private static final String FEE_COLLECTOR_ADDRESS = "AJ0xc41061d9f595b3fbc0dc11a0f4811964eeda5bf1";
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
	
	private String pendingGroupMnemonic;
	private String pendingGroupWalletAddress;
	private String pendingCardQr, pendingCardDigits, pendingCardTemplate;
	private com.google.android.material.bottomsheet.BottomSheetDialog pendingGroupBottomSheetDialog;
	private static final int RC_MANDATORY_GROUP_CARD = 8803;
	
	public void _delete_rule_in(final double _pos, final ArrayList<HashMap<String, Object>> _lMap) {
		_lMap.remove((int)(_pos));
		listview1.setAdapter(new Listview1Adapter(_lMap));
		((BaseAdapter)listview1.getAdapter()).notifyDataSetChanged();
	}
	
	
	public void _showCreateWalletBottomSheet() {
		
		final com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog =
		new com.google.android.material.bottomsheet.BottomSheetDialog(this);
		
		View sheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_create_wallet, null);
		bottomSheetDialog.setContentView(sheetView);
		// Full-screen + rounded corners via the shared helper — the previous
		// inline version called getWindow().findViewById() BEFORE show(),
		// which is unreliable (window isn't guaranteed attached yet), and
		// never set STATE_EXPANDED/MATCH_PARENT at all, so this sheet was
		// opening as the default half-peek sheet instead of full screen.
		com.harvanna.app.group.BottomSheetUtils.expandFullScreen(bottomSheetDialog, sheetView);
		
		com.google.android.material.button.MaterialButton btnGenerate = sheetView.findViewById(R.id.btn_generate_wallet);
		TextView tvCancel = sheetView.findViewById(R.id.tv_cancel);
		TextView tvTitle = sheetView.findViewById(R.id.tv_title);
		if (tvTitle != null) {
			tvTitle.setText("New Group Wallet Seed Phrase");
		}
		
		btnGenerate.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				bottomSheetDialog.dismiss();
				_showRecoveryPhraseBottomSheet();
			}
		});
		
		tvCancel.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				bottomSheetDialog.dismiss();
			}
		});
		
		bottomSheetDialog.show();
		
	}
	
	
	public void _showRecoveryPhraseBottomSheet() {
		
		final Context context = this;
		if (context == null) return;
		
		final com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog =
		new com.google.android.material.bottomsheet.BottomSheetDialog(context);
		
		View sheetView = LayoutInflater.from(context)
		.inflate(R.layout.bottom_sheet_recovery_phrase, null);
		bottomSheetDialog.setContentView(sheetView);
		// Same fix as _showCreateWalletBottomSheet(): shared helper instead of
		// a pre-show findViewById() that could silently no-op, and this one
		// additionally never set full-screen state at all.
		com.harvanna.app.group.BottomSheetUtils.expandFullScreen(bottomSheetDialog, sheetView);
		
		try {
			final String mnemonic = com.harvanna.app.wallet.WalletGenerator.generateMnemonic(context);
			if (mnemonic == null || mnemonic.trim().isEmpty()) {
				SketchwareUtil.showMessage(context, "Failed to generate recovery phrase.");
				return;
			}
			
			final String groupWalletAddress = com.harvanna.app.wallet.WalletGenerator.deriveAddressFromMnemonic(mnemonic);
			if (groupWalletAddress == null || !groupWalletAddress.startsWith("HV0x") || groupWalletAddress.length() != 44) {
				SketchwareUtil.showMessage(context, "Failed to derive valid HV0x address.");
				return;
			}
			
			final String[] phraseWords = mnemonic.trim().split("\\s+");
			if (phraseWords.length != 12) {
				SketchwareUtil.showMessage(context, "Invalid phrase length.");
				return;
			}
			
			TextView tvTitle = sheetView.findViewById(R.id.tv_title);
			if (tvTitle != null) {
				tvTitle.setText("New Group Wallet Seed Phrase");
			}
			
			int[] tvNumIds = {
				R.id.tv_num_1, R.id.tv_num_2, R.id.tv_num_3, R.id.tv_num_4,
				R.id.tv_num_5, R.id.tv_num_6, R.id.tv_num_7, R.id.tv_num_8,
				R.id.tv_num_9, R.id.tv_num_10, R.id.tv_num_11, R.id.tv_num_12
			};
			int[] tvWordIds = {
				R.id.tv_word_1, R.id.tv_word_2, R.id.tv_word_3, R.id.tv_word_4,
				R.id.tv_word_5, R.id.tv_word_6, R.id.tv_word_7, R.id.tv_word_8,
				R.id.tv_word_9, R.id.tv_word_10, R.id.tv_word_11, R.id.tv_word_12
			};
			
			for (int i = 0; i < 12; i++) {
				TextView tvNum = sheetView.findViewById(tvNumIds[i]);
				TextView tvWord = sheetView.findViewById(tvWordIds[i]);
				if (tvNum != null) tvNum.setText((i + 1) + ".");
				if (tvWord != null) tvWord.setText(phraseWords[i]);
			}
			
			final CheckBox cbAgree = sheetView.findViewById(R.id.cb_agree);
			final com.google.android.material.button.MaterialButton btnFinish =
			sheetView.findViewById(R.id.btn_finish_setup);
			
			if (btnFinish == null) {
				SketchwareUtil.showMessage(context, "Finish button not found.");
				return;
			}
			
			btnFinish.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (cbAgree != null && !cbAgree.isChecked()) {
						SketchwareUtil.showMessage(context, "Please confirm you have backed up the phrase.");
						return;
					}
					
					pendingGroupMnemonic = mnemonic;
					pendingGroupWalletAddress = groupWalletAddress;
					pendingGroupBottomSheetDialog = bottomSheetDialog;
					Intent cardIntent = new Intent(context, com.harvanna.app.CreateHarvannaCardActivity.class);
					cardIntent.putExtra(com.harvanna.app.CreateHarvannaCardActivity.EXTRA_MANDATORY, true);
					cardIntent.putExtra(com.harvanna.app.CreateHarvannaCardActivity.EXTRA_RAW_MNEMONIC, mnemonic);
					cardIntent.putExtra(com.harvanna.app.CreateHarvannaCardActivity.EXTRA_WALLET_ADDRESS, groupWalletAddress);
					cardIntent.putExtra(com.harvanna.app.CreateHarvannaCardActivity.EXTRA_LABEL, group_name_edittext1.getText().toString().trim());
					startActivityForResult(cardIntent, RC_MANDATORY_GROUP_CARD);
				}
			});
			
			bottomSheetDialog.show();
			
		} catch (Exception e) {
			SketchwareUtil.showMessage(context, "Error: " + e.getMessage());
		}
		
		
	}
	
	/**
* After node is confirmed reachable: check personal balance, transfer 5 HARV
* creation fee to the new group wallet address, persist keys locally, then
* write the group doc. Wallet is created on-chain by the first credit —
* no Firestore WALLETS write.
*/
	private void _continueGroupCreationFee(final ProgressDialog progress,
	final Context context,
	final SharedPreferences prefs,
	final com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog,
	final String groupWalletAddress,
	final String mnemonic,
	final String finalPersonalAddress,
	final String finalPersonalSeed) {
		runOnUiThread(() -> {
			progress.setMessage(("Checking balance for 5 " + com.harvanna.app.CoinInfo.ticker() + " fee..."));
			com.harvanna.app.wallet.WalletNodeBridge.fetchLiveBalance(CreateNewGroupActivity.this, finalPersonalAddress,
			new com.harvanna.app.wallet.WalletNodeBridge.BalanceCallback() {
				@Override
				public void onSuccess(double harvBalance, long nonce, long blockHeight) {
					runOnUiThread(() -> {
						java.math.BigDecimal balance = java.math.BigDecimal.valueOf(harvBalance);
						java.math.BigDecimal requiredAmount = new java.math.BigDecimal("5.005");
						if (balance.compareTo(requiredAmount) < 0) {
							if (!isFinishing() && progress.isShowing()) progress.dismiss();
							SketchwareUtil.showMessage(context, ("Insufficient balance. You need at least 5.005 " + com.harvanna.app.CoinInfo.ticker() + "."));
							return;
						}
						
						try {
							String privateB64 = com.harvanna.app.wallet.WalletGenerator.derivePrivateKey(finalPersonalSeed);
							if (privateB64 != null) {
								byte[] seedBytes = android.util.Base64.decode(privateB64, android.util.Base64.NO_WRAP);
								byte[] pubKey = Ed25519.publicKeyFromSeed(seedBytes);
								String publicB64 = android.util.Base64.encodeToString(pubKey, android.util.Base64.NO_WRAP);
								com.harvanna.app.wallet.WalletManager.saveKeysLocally(
								CreateNewGroupActivity.this, finalPersonalAddress, privateB64, publicB64);
							}
						} catch (Exception e) {
							if (!isFinishing() && progress.isShowing()) progress.dismiss();
							SketchwareUtil.showMessage(context, "Key error: " + e.getMessage());
							return;
						}
						
						progress.setMessage(("Transferring 5 " + com.harvanna.app.CoinInfo.ticker() + " to group wallet..."));
						// THE FIX: this moved 5 HARV with zero PIN/biometric
						// confirmation — the only money-moving screen in the
						// app besides Send/P2P that had no gate at all.
						com.harvanna.app.wallet.PinPromptHelper.verifyPinThenProceed(CreateNewGroupActivity.this, () ->
						com.harvanna.app.tx.TransactionRouter.routeTransaction(
						CreateNewGroupActivity.this,
						finalPersonalAddress,
						groupWalletAddress,
						java.math.BigDecimal.valueOf(5.0),
						null,
						"Group creation fee",
						new com.harvanna.app.tx.TransactionRouter.RouterCallback() {
							@Override
							public void onSuccess(String txHash) {
								runOnUiThread(() -> {
									try {
										SharedPreferences.Editor editor = prefs.edit();
										java.util.Set<String> existing = prefs.getStringSet(
										"group_wallet_addresses", new java.util.HashSet<>());
										java.util.Set<String> updated = new java.util.HashSet<>(existing);
										updated.add(groupWalletAddress);
										editor.putStringSet("group_wallet_addresses", updated);
										editor.putString("last_group_wallet_address", groupWalletAddress);
										editor.putLong("last_group_wallet_created", System.currentTimeMillis());
										editor.apply();
										
										com.harvanna.app.wallet.SecureWalletStorage.saveMnemonic(
										CreateNewGroupActivity.this, groupWalletAddress, mnemonic);
										
										try {
											String groupPrivateB64 = com.harvanna.app.wallet.WalletGenerator.derivePrivateKey(mnemonic);
											if (groupPrivateB64 != null) {
												byte[] groupSeedBytes = android.util.Base64.decode(
												groupPrivateB64, android.util.Base64.NO_WRAP);
												byte[] groupPubKey = Ed25519.publicKeyFromSeed(groupSeedBytes);
												String groupPublicB64 = android.util.Base64.encodeToString(
												groupPubKey, android.util.Base64.NO_WRAP);
												com.harvanna.app.wallet.WalletManager.saveKeysLocally(
												CreateNewGroupActivity.this, groupWalletAddress,
												groupPrivateB64, groupPublicB64);
											}
										} catch (Exception e) {
											android.util.Log.w("CreateNewGroup",
											"Group wallet key derivation failed: " + e.getMessage());
										}
										
										if (!isFinishing() && progress.isShowing()) progress.dismiss();
										_submitNewGroupData(groupWalletAddress);
										bottomSheetDialog.dismiss();
										SketchwareUtil.showMessage(context,
										("Group Created — 5 " + com.harvanna.app.CoinInfo.ticker() + " transfer submitted!"));
										
										final String createdGroupName = group_name_edittext1.getText().toString().trim();
										new androidx.appcompat.app.AlertDialog.Builder(context)
										.setTitle("Back up this group's wallet?")
										.setMessage("As the group's admin, you're the one holding its wallet key. An Harvanna Access Card is a physical backup for it — recommended for any group holding real funds.")
										.setPositiveButton("Create Harvanna Card", (d, w) -> {
											Intent cardIntent = new Intent(context, com.harvanna.app.CreateHarvannaCardActivity.class);
											cardIntent.putExtra(com.harvanna.app.CreateHarvannaCardActivity.EXTRA_WALLET_ADDRESS, groupWalletAddress);
											cardIntent.putExtra(com.harvanna.app.CreateHarvannaCardActivity.EXTRA_LABEL, createdGroupName);
											context.startActivity(cardIntent);
										})
										.setNegativeButton("Not now", null)
										.show();
									} catch (Exception e) {
										if (!isFinishing() && progress.isShowing()) progress.dismiss();
										SketchwareUtil.showMessage(context,
										"Wallet save failed: " + e.getMessage());
									}
								});
							}
							
							@Override
							public void onError(String errorCode, String errorMessage) {
								runOnUiThread(() -> {
									if (!isFinishing() && progress.isShowing()) progress.dismiss();
									SketchwareUtil.showMessage(context,
									"Fee transfer failed: " + errorMessage);
								});
							}
						}));
					});
				}
				
				@Override
				public void onError(String error) {
					runOnUiThread(() -> {
						if (!isFinishing() && progress.isShowing()) progress.dismiss();
						SketchwareUtil.showMessage(context, "Failed to check personal balance: " + error);
					});
				}
			});
		});
	}
	
	private void _selectGroupType(String type) {
		groupTypeString = type;
		
		// Cross-fade the detail-card swap instead of a hard cut
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
		
		// Distribution method only applies to Rotating Pool — reset it
		// cleanly if the user switches away after having picked one.
		if (!"Rotating Pool".equals(type)) {
			distributionString = "";
			fields_random.setVisibility(View.GONE);
			fields_fcfs.setVisibility(View.GONE);
			fields_admin.setVisibility(View.GONE);
			fields_auction.setVisibility(View.GONE);
			fields_pool_universal.setVisibility(View.GONE);
		}
	}
	
	// ═══════════════════════════════════════════════════════════════════
	// DISTRIBUTION METHOD SELECTION — five cards, replaces the old spinner
	// listener. Plain CardView has no built-in "selected" stroke state
	// (that's Material-only), so selection is shown via background color
	// instead: lit accent tint when picked, back to normal surface when not.
	// ═══════════════════════════════════════════════════════════════════
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
		// Fixed Order needs no extra fields at all — passive, just join timestamp
		
		// Universal pool-risk fields appear once ANY method is picked
		fields_pool_universal.setVisibility(View.VISIBLE);
		
		_advanceStepper(3);
	}
	
	// ── Animators ──────────────────────────────────────────────────────
	
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
	
	/** Fills stepper dots up to and including the given step (1-4), with a
*  quick color-swap so progress feels alive rather than just correct. */
	private void _advanceStepper(int upToStep) {
		View[] dots = { step_dot_1, step_dot_2, step_dot_3, step_dot_4 };
		for (int i = 0; i < dots.length; i++) {
			int targetDrawable = (i < upToStep) ? R.drawable.step_dot_active : R.drawable.step_dot_inactive;
			dots[i].setBackgroundResource(targetDrawable);
		}
	}
	
	private String getFirestoreString(JSONObject fields, String fieldName) {
		try {
			if (!fields.has(fieldName)) {
				return "";
			}
			JSONObject field = fields.getJSONObject(fieldName);
			return field.optString("stringValue", "");
		} catch (Exception e) {
			return "";
		}
	}
	
	
	private boolean getFirestoreBoolean(JSONObject fields, String fieldName) {
		try {
			if (!fields.has(fieldName)) {
				return false;
			}
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
		if (frequencyString.trim().isEmpty()) {
			SketchwareUtil.showMessage(getApplicationContext(), "Please select a contribution frequency.");
			return false;
		}
		if (!_validatePositiveDecimal(contribution_amount_edittext3, "Please enter a valid contribution amount.")) return false;
		if (!_validateMinInt(no_of_slots_edittext4, 2, "A group needs at least 2 slots.")) return false;
		
		if (distributionString.trim().isEmpty()) {
			SketchwareUtil.showMessage(getApplicationContext(), "Please select a distribution method.");
			return false;
		}
		
		if ("Random Ballot".equals(distributionString) && drawTimingString.trim().isEmpty()) {
			SketchwareUtil.showMessage(getApplicationContext(), "Please select draw timing.");
			return false;
		}
		if ("First Come First Serve".equals(distributionString)) {
			if (!_validateMinInt(claim_window_edittext, 1, "Please enter a valid claim window.")) return false;
		}
		if ("Auction / Bidding".equals(distributionString)) {
			if (!_validateRangeInt(max_bid_cap_edittext, 1, 100, "Max bid cap must be between 1% and 100%.")) return false;
			if (!_validateRangeInt(commission_edittext, 0, 100, "Commission must be between 0% and 100%.")) return false;
			if (!_validateMinInt(bidding_window_edittext, 1, "Please enter a valid bidding window.")) return false;
		}
		
		if (!_validateMinInt(grace_period_edittext, 0, "Please enter a valid grace period.")) return false;
		if (!_validateRangeInt(min_trust_score_edittext, 0, 100, "Minimum trust score must be between 0 and 100.")) return false;
		if (defaultConsequenceString.trim().isEmpty()) {
			SketchwareUtil.showMessage(getApplicationContext(), "Please select what happens if a payment is missed.");
			return false;
		}
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
		if (!_validateRangeDecimal(loan_interest_rate_edittext, 0, 100, "Interest rate must be between 0% and 100%.")) return false;
		if (withdrawalRuleString.trim().isEmpty()) {
			SketchwareUtil.showMessage(getApplicationContext(), "Please select a withdrawal rule.");
			return false;
		}
		return true;
	}
	
	// ── Small shared validation helpers, used across all three group types ──
	
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
			if (intent != null && "com.harvanna.app.ACTION_WALLET_RECEIVED".equals(intent.getAction())) {
				double amount = intent.getDoubleExtra("amount", 0.0);
				String sender = intent.getStringExtra("sender");
				
				com.harvanna.app.utils.InAppNotificationHelper.showTransactionNotification(
				CreateNewGroupActivity.this,
				"Incoming Transfer",
				"Received " + String.format(Locale.US, "%.5f", amount) + (" " + com.harvanna.app.CoinInfo.ticker()),
				sender
				);
			}
		}
	};
	
	public void _submitNewGroupData(final String _realGroupWalletAddress) {
		// Client-generated id — Firestore doc path is fixed via createGroup's
		// PATCH rather than relying on auto-id, so members/notices/payments/
		// loans subcollections can all be addressed under it immediately.
		final String newGroupId = java.util.UUID.randomUUID().toString();
		final String creatorUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
		final String timestamp = String.valueOf(System.currentTimeMillis());
		
		newGroupMap = new HashMap<>();
		newGroupMap.put("groupId", newGroupId);
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
		
		// Permissions
		newGroupMap.put("requireApprovalToJoin", switch_require_approval.isChecked());
		newGroupMap.put("joinApprovalMethod", switch_require_approval.isChecked()
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
			newGroupMap.put("loanInterestRate", Double.parseDouble(loan_interest_rate_edittext.getText().toString()));
			newGroupMap.put("loanInterestPeriod",
			spinner_loan_interest_period.getSelectedItem() != null
			? spinner_loan_interest_period.getSelectedItem().toString().toUpperCase()
			: "MONTHLY");
			newGroupMap.put("withdrawalRule", withdrawalRuleString.trim());
		}
		
		// members/notice/payments/loans are NOT part of the group document —
		// they're separate subcollections now, written after the group
		// document itself succeeds.
		final com.harvanna.app.firestore.GroupFirestoreManager firestoreManager =
		com.harvanna.app.firestore.GroupFirestoreManager.getInstance(getApplicationContext());
		
		final ProgressDialog groupProgress = new ProgressDialog(CreateNewGroupActivity.this);
		groupProgress.setMessage("Creating your group...");
		groupProgress.setCancelable(false);
		groupProgress.show();
		
		firestoreManager.createGroup(newGroupId, newGroupMap, new com.harvanna.app.firestore.GroupFirestoreManager.SimpleCallback() {
			@Override
			public void onSuccess() {
				// Contract-compliant first member (creator = ADMIN, slot 1)
				Map<String, Object> memberFields = new HashMap<>();
				memberFields.put("memberUid", creatorUid);
				memberFields.put("slot", 1);
				memberFields.put("role", "ADMIN");
				memberFields.put("status", "ACTIVE");
				memberFields.put("joinedAt", System.currentTimeMillis());
				
				firestoreManager.addMember(newGroupId, creatorUid, memberFields, new com.harvanna.app.firestore.GroupFirestoreManager.SimpleCallback() {
					@Override
					public void onSuccess() {
						// THE FIX: this used to write the SAME "Group
						// launched" notice twice — once via postSystemNotice
						// (the standard path, correctly resolves to "By
						// System"), and once more via a manual addNotice()
						// call kept "for backward compatibility" that also
						// set creatorId to the real creator uid, which
						// resolves through the name-resolution fix applied
						// elsewhere this session to show as "By <creator
						// name>" instead. Two notices, two different
						// authors shown, same single event. The manual
						// write was pure duplication — postSystemNotice is
						// the one every other group event already uses
						// consistently. Notice-posting was already treated
						// as non-critical to navigation (the old onError
						// branch proceeded anyway) — same here, just
						// without a redundant second write to gate on.
						firestoreManager.postSystemNotice(newGroupId,
						"Group launched",
						"This group is now active. Welcome!");
						
						if (pendingCardQr != null && pendingCardDigits != null) {
							com.harvanna.app.group.GroupCardStore.publish(getApplicationContext(), newGroupId,
							_realGroupWalletAddress, group_name_edittext1.getText().toString().trim(),
							pendingCardTemplate == null ? "CLASSIC" : pendingCardTemplate,
							pendingCardQr, pendingCardDigits, creatorUid, null);
						}
						
						com.harvanna.app.firestore.UserProfileManager.getInstance(getApplicationContext())
						.addGroupToProfile(creatorUid, newGroupId, new com.harvanna.app.firestore.UserProfileManager.SimpleCallback() {
							@Override public void onSuccess() { /* best-effort, doesn't block navigation */ }
							@Override public void onError(String err) {
								android.util.Log.w("CreateNewGroup", "Failed to add group to Firestore profile: " + err);
							}
						});
						
						if (!CreateNewGroupActivity.this.isFinishing() && groupProgress.isShowing()) groupProgress.dismiss();
						intent.putExtra("groupId", newGroupId);
						intent.setClass(getApplicationContext(), GroupDetailsActivity.class);
						startActivity(intent);
						finish();
					}
					@Override
					public void onError(final String error) {
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								// Group document exists but has no members recorded —
								// a genuinely broken state, so surface this clearly
								// rather than silently proceeding into it.
								if (!CreateNewGroupActivity.this.isFinishing() && groupProgress.isShowing()) groupProgress.dismiss();
								SketchwareUtil.showMessage(getApplicationContext(), "Group created, but adding you as a member failed: " + error + ". Please contact support with this group ID: " + newGroupId);
							}
						});
					}
				});
			}
			@Override
			public void onError(final String error) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (!CreateNewGroupActivity.this.isFinishing() && groupProgress.isShowing()) groupProgress.dismiss();
						SketchwareUtil.showMessage(getApplicationContext(), "Failed to create group: " + error);
					}
				});
			}
		});
	}
	
	private void _proceedWithGroupWalletCreation() {
		final Context context = this;
		final String mnemonic = pendingGroupMnemonic;
		final String groupWalletAddress = pendingGroupWalletAddress;
		final com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog = pendingGroupBottomSheetDialog;
		
		final ProgressDialog progress = new ProgressDialog(CreateNewGroupActivity.this);
		progress.setMessage("Preparing wallets...");
		progress.setCancelable(false);
		progress.show();
		
		String personalSeed = com.harvanna.app.wallet.SecureWalletStorage.getActiveMnemonic(CreateNewGroupActivity.this);
		if (personalSeed == null) personalSeed = "";
		
		SharedPreferences prefs = getSharedPreferences("harv_wallet_prefs", Context.MODE_PRIVATE);
		String resolvedAddress = prefs.getString("active_wallet_address", "").trim();
		if (resolvedAddress.isEmpty()) {
			resolvedAddress = prefs.getString("wallet_address", "").trim();
		}
		
		if (resolvedAddress.isEmpty() && !personalSeed.isEmpty()) {
			try {
				resolvedAddress = com.harvanna.app.wallet.WalletManager.deriveAddressFromSeed(personalSeed).trim();
			} catch (Exception e) {
				resolvedAddress = "";
			}
		}
		
		if (resolvedAddress.isEmpty()) {
			if (!CreateNewGroupActivity.this.isFinishing() && progress.isShowing()) progress.dismiss();
			SketchwareUtil.showMessage(context, "Personal wallet not found. Cannot pay creation fee.");
			return;
		}
		
		final String finalPersonalAddress = resolvedAddress;
		final String finalPersonalSeed = personalSeed;
		
		progress.setMessage(("Connecting to " + com.harvanna.app.CoinInfo.ticker() + " node..."));
		com.harvanna.app.sync.HarvannaNodeClient.getWallet(groupWalletAddress,
		new com.harvanna.app.sync.HarvannaNodeClient.JsonCallback() {
			@Override
			public void onSuccess(org.json.JSONObject json) {
				_continueGroupCreationFee(progress, context, prefs, bottomSheetDialog,
				groupWalletAddress, mnemonic, finalPersonalAddress, finalPersonalSeed);
			}
			
			@Override
			public void onError(String error) {
				String err = error != null ? error : "";
				boolean nodeDown = err.contains("network error")
				|| err.contains("failed to connect")
				|| err.contains("Connection refused")
				|| err.contains("Unable to resolve")
				|| err.contains("timeout")
				|| err.contains("ETIMEDOUT");
				if (nodeDown) {
					CreateNewGroupActivity.this.runOnUiThread(() -> {
						if (!CreateNewGroupActivity.this.isFinishing() && progress.isShowing()) progress.dismiss();
						SketchwareUtil.showMessage(context, (com.harvanna.app.CoinInfo.ticker() + " node unreachable: ") + error);
					});
					return;
				}
				_continueGroupCreationFee(progress, context, prefs, bottomSheetDialog,
				groupWalletAddress, mnemonic, finalPersonalAddress, finalPersonalSeed);
			}
		});
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == RC_MANDATORY_GROUP_CARD && resultCode == Activity.RESULT_OK) {
			if (data != null) {
				pendingCardQr = data.getStringExtra(com.harvanna.app.CreateHarvannaCardActivity.EXTRA_RESULT_QR);
				pendingCardDigits = data.getStringExtra(com.harvanna.app.CreateHarvannaCardActivity.EXTRA_RESULT_DIGITS);
				pendingCardTemplate = data.getStringExtra(com.harvanna.app.CreateHarvannaCardActivity.EXTRA_RESULT_TEMPLATE);
			}
			_proceedWithGroupWalletCreation();
		}
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
			LayoutInflater _inflater = getLayoutInflater();
			View _view = _v;
			if (_view == null) {
				_view = _inflater.inflate(R.layout.rules_custom_card, null);
			}
			
			final com.google.android.material.card.MaterialCardView cardview33 = _view.findViewById(R.id.cardview33);
			final LinearLayout linear1 = _view.findViewById(R.id.linear1);
			final TextView rule_no_textview1 = _view.findViewById(R.id.rule_no_textview1);
			final View linear2 = _view.findViewById(R.id.linear2);
			final TextView rules_textview2 = _view.findViewById(R.id.rules_textview2);
			final ImageView imageview1 = _view.findViewById(R.id.imageview1);
			
			rule_no_textview1.setText(String.valueOf((long)(_position + 1)));
			rules_textview2.setText(_data.get((int) _position).get("rules").toString());
			imageview1.setOnClickListener(new View.OnClickListener(){
				@Override
				public void onClick(View _view){
					_delete_rule_in(_position, rules_l_map);
				}
			});
			
			return _view;
		}
	}
}
