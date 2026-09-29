package com.harvanna.app;

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
import android.widget.Switch;
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


public class NotificationSettingsActivity extends AppCompatActivity {
	
	private Toolbar _toolbar;
	private AppBarLayout _app_bar;
	private CoordinatorLayout _coordinator;
	
	private LinearLayout linear62;
	private TextView textview74;
	private TextView textview75;
	private MaterialCardView cardview14;
	private MaterialCardView cardview15;
	private MaterialCardView cardview16;
	private MaterialCardView cardview17;
	private TextView textview84;
	private MaterialCardView cardview18;
	private TextView textview87;
	private LinearLayout linear63;
	private LinearLayout linear64;
	private Switch switchTransactionAlerts;
	private TextView textview76;
	private TextView textview77;
	private LinearLayout linear65;
	private LinearLayout linear66;
	private Switch switchPaymentReminders;
	private TextView textview78;
	private TextView textview79;
	private LinearLayout linear67;
	private LinearLayout linear68;
	private Switch switchLoanReminders;
	private TextView textview80;
	private TextView textview81;
	private LinearLayout linear69;
	private LinearLayout linear70;
	private Switch switchGroupNotices;
	private TextView textview82;
	private TextView textview83;
	private LinearLayout linear71;
	private LinearLayout linear72;
	private View view73;
	private LinearLayout linear74;
	private TextView textview85;
	private Switch switchSound;
	private TextView textview86;
	private Switch switchVibrate;
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.notification_settings);
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
		linear62 = findViewById(R.id.linear62);
		textview74 = findViewById(R.id.textview74);
		textview75 = findViewById(R.id.textview75);
		cardview14 = findViewById(R.id.cardview14);
		cardview15 = findViewById(R.id.cardview15);
		cardview16 = findViewById(R.id.cardview16);
		cardview17 = findViewById(R.id.cardview17);
		textview84 = findViewById(R.id.textview84);
		cardview18 = findViewById(R.id.cardview18);
		textview87 = findViewById(R.id.textview87);
		linear63 = findViewById(R.id.linear63);
		linear64 = findViewById(R.id.linear64);
		switchTransactionAlerts = findViewById(R.id.switchTransactionAlerts);
		textview76 = findViewById(R.id.textview76);
		textview77 = findViewById(R.id.textview77);
		linear65 = findViewById(R.id.linear65);
		linear66 = findViewById(R.id.linear66);
		switchPaymentReminders = findViewById(R.id.switchPaymentReminders);
		textview78 = findViewById(R.id.textview78);
		textview79 = findViewById(R.id.textview79);
		linear67 = findViewById(R.id.linear67);
		linear68 = findViewById(R.id.linear68);
		switchLoanReminders = findViewById(R.id.switchLoanReminders);
		textview80 = findViewById(R.id.textview80);
		textview81 = findViewById(R.id.textview81);
		linear69 = findViewById(R.id.linear69);
		linear70 = findViewById(R.id.linear70);
		switchGroupNotices = findViewById(R.id.switchGroupNotices);
		textview82 = findViewById(R.id.textview82);
		textview83 = findViewById(R.id.textview83);
		linear71 = findViewById(R.id.linear71);
		linear72 = findViewById(R.id.linear72);
		view73 = findViewById(R.id.view73);
		linear74 = findViewById(R.id.linear74);
		textview85 = findViewById(R.id.textview85);
		switchSound = findViewById(R.id.switchSound);
		textview86 = findViewById(R.id.textview86);
		switchVibrate = findViewById(R.id.switchVibrate);
	}
	
	private void initializeLogic() {
		if (getSupportActionBar() != null) {
			getSupportActionBar().setTitle("Notification settings");
			getSupportActionBar().setSubtitle("");
		}
		prefs = getSharedPreferences(PREFS_NAME, Activity.MODE_PRIVATE);
		
		// All default to true — an opt-out model, so someone who never
		// opens this screen still gets the notifications an app like this
		// is actually useful for (a missed contribution, a due loan).
		switchTransactionAlerts.setChecked(prefs.getBoolean("transaction_alerts", true));
		switchPaymentReminders.setChecked(prefs.getBoolean("payment_reminders", true));
		switchLoanReminders.setChecked(prefs.getBoolean("loan_reminders", true));
		switchGroupNotices.setChecked(prefs.getBoolean("group_notices", true));
		switchSound.setChecked(prefs.getBoolean("sound", true));
		switchVibrate.setChecked(prefs.getBoolean("vibrate", true));
		
		_bindToggle(switchTransactionAlerts, "transaction_alerts");
		_bindToggle(switchPaymentReminders, "payment_reminders");
		_bindToggle(switchLoanReminders, "loan_reminders");
		_bindToggle(switchGroupNotices, "group_notices");
		_bindToggle(switchSound, "sound");
		_bindToggle(switchVibrate, "vibrate");
		
	}
	
	public void _MORE() {
	}
	public static final String PREFS_NAME = "notification_prefs";
	private SharedPreferences prefs;
	
	private void _bindToggle(Switch sw, String prefKey) {
		sw.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) ->
		prefs.edit().putBoolean(prefKey, isChecked).apply());
	}
	{
	}
	
}