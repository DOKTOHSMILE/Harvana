package com.harvana.app;

import android.animation.*;
import android.app.*;
import android.content.*;
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
import android.widget.LinearLayout;
import com.google.android.material.switchmaterial.SwitchMaterial;
import android.widget.TextView;
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
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.card.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.storage.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;
import org.json.*;

public class PrivacySettingsActivity extends AppCompatActivity {
	
	private Toolbar _toolbar;
	private AppBarLayout _app_bar;
	private CoordinatorLayout _coordinator;
	
	private LinearLayout linear83;
	private TextView textview92;
	private TextView textview93;
	private MaterialCardView cardview20;
	private MaterialCardView cardview21;
	private MaterialCardView cardview22;
	private MaterialCardView cardview23;
	private TextView textview102;
	private LinearLayout linear84;
	private LinearLayout linear85;
	private SwitchMaterial switchShowTrustScore;
	private TextView textview94;
	private TextView textview95;
	private LinearLayout linear86;
	private LinearLayout linear87;
	private SwitchMaterial switchShowGroups;
	private TextView textview96;
	private TextView textview97;
	private LinearLayout linear88;
	private LinearLayout linear89;
	private SwitchMaterial switchShowActivity;
	private TextView textview98;
	private TextView textview99;
	private LinearLayout linear90;
	private LinearLayout linear91;
	private SwitchMaterial switchAllowInvites;
	private TextView textview100;
	private TextView textview101;
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.privacy_settings);
		initialize(_savedInstanceState);
		FirebaseApp.initializeApp(this);
		initializeLogic();
		com.harvana.app.utils.AppBar.header(this, "Privacy & visibility", "What other members can see about you", R.id.textview92, R.id.textview93);
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
		linear83 = findViewById(R.id.linear83);
		textview92 = findViewById(R.id.textview92);
		textview93 = findViewById(R.id.textview93);
		cardview20 = findViewById(R.id.cardview20);
		cardview21 = findViewById(R.id.cardview21);
		cardview22 = findViewById(R.id.cardview22);
		cardview23 = findViewById(R.id.cardview23);
		textview102 = findViewById(R.id.textview102);
		linear84 = findViewById(R.id.linear84);
		linear85 = findViewById(R.id.linear85);
		switchShowTrustScore = findViewById(R.id.switchShowTrustScore);
		textview94 = findViewById(R.id.textview94);
		textview95 = findViewById(R.id.textview95);
		linear86 = findViewById(R.id.linear86);
		linear87 = findViewById(R.id.linear87);
		switchShowGroups = findViewById(R.id.switchShowGroups);
		textview96 = findViewById(R.id.textview96);
		textview97 = findViewById(R.id.textview97);
		linear88 = findViewById(R.id.linear88);
		linear89 = findViewById(R.id.linear89);
		switchShowActivity = findViewById(R.id.switchShowActivity);
		textview98 = findViewById(R.id.textview98);
		textview99 = findViewById(R.id.textview99);
		linear90 = findViewById(R.id.linear90);
		linear91 = findViewById(R.id.linear91);
		switchAllowInvites = findViewById(R.id.switchAllowInvites);
		textview100 = findViewById(R.id.textview100);
		textview101 = findViewById(R.id.textview101);
	}
	
	private void initializeLogic() {
		if (getSupportActionBar() != null) {
			getSupportActionBar().setTitle("Privacy & Visibility");
			getSupportActionBar().setSubtitle("");
		}
		prefs = getSharedPreferences(PREFS_NAME, Activity.MODE_PRIVATE);
		
		// Defaults favor openness, matching how the app already behaves
		// today (trust score and groups are already shown on profiles
		// with no gating) — these toggles are additive restrictions a
		// person can opt into, not a new default-locked-down posture.
		switchShowTrustScore.setChecked(prefs.getBoolean("show_trust_score", true));
		switchShowGroups.setChecked(prefs.getBoolean("show_groups", true));
		switchShowActivity.setChecked(prefs.getBoolean("show_activity_status", true));
		switchAllowInvites.setChecked(prefs.getBoolean("allow_invites_from_anyone", true));
		
		_bindToggle(switchShowTrustScore, "show_trust_score");
		_bindToggle(switchShowGroups, "show_groups");
		_bindToggle(switchShowActivity, "show_activity_status");
		_bindToggle(switchAllowInvites, "allow_invites_from_anyone");
		
	}
	
	public void _MORE() {
	}
	public static final String PREFS_NAME = "privacy_prefs";
	private SharedPreferences prefs;
	private void _bindToggle(SwitchMaterial sw, String prefKey) {
		sw.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) ->
		prefs.edit().putBoolean(prefKey, isChecked).apply());
	}
	
	{
	}
	
}
