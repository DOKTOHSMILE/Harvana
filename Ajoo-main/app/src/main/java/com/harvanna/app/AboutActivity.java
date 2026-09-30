package com.harvanna.app;

import android.animation.*;
import android.app.*;
import android.content.*;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
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
import androidx.appcompat.widget.Toolbar;
import androidx.biometric.*;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
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

public class AboutActivity extends AppCompatActivity {
	
	private Toolbar _toolbar;
	private AppBarLayout _app_bar;
	private CoordinatorLayout _coordinator;
	
	private LinearLayout linear44;
	private LinearLayout linear45;
	private MaterialCardView cardview7;
	private TextView textview51;
	private ImageView imageview40;
	private TextView textview46;
	private TextView tvAppVersion;
	private TextView textview47;
	private LinearLayout linear46;
	private LinearLayout rowPrivacyPolicy;
	private View view47;
	private LinearLayout rowTermsOfService;
	private View view48;
	private LinearLayout rowOpenSourceLicenses;
	private TextView textview48;
	private ImageView imageview41;
	private TextView textview49;
	private ImageView imageview42;
	private TextView textview50;
	private ImageView imageview43;
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.about);
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
		linear44 = findViewById(R.id.linear44);
		linear45 = findViewById(R.id.linear45);
		cardview7 = findViewById(R.id.cardview7);
		textview51 = findViewById(R.id.textview51);
		imageview40 = findViewById(R.id.imageview40);
		textview46 = findViewById(R.id.textview46);
		tvAppVersion = findViewById(R.id.tvAppVersion);
		textview47 = findViewById(R.id.textview47);
		linear46 = findViewById(R.id.linear46);
		rowPrivacyPolicy = findViewById(R.id.rowPrivacyPolicy);
		view47 = findViewById(R.id.view47);
		rowTermsOfService = findViewById(R.id.rowTermsOfService);
		view48 = findViewById(R.id.view48);
		rowOpenSourceLicenses = findViewById(R.id.rowOpenSourceLicenses);
		textview48 = findViewById(R.id.textview48);
		imageview41 = findViewById(R.id.imageview41);
		textview49 = findViewById(R.id.textview49);
		imageview42 = findViewById(R.id.imageview42);
		textview50 = findViewById(R.id.textview50);
		imageview43 = findViewById(R.id.imageview43);
	}
	
	private void initializeLogic() {
		if (getSupportActionBar() != null) {
			getSupportActionBar().setTitle("About");
			getSupportActionBar().setSubtitle("");
		}
		_bindRealVersion();
		
		rowPrivacyPolicy.setOnClickListener(v -> {
			Intent intent = new Intent(this, DocumentViewerActivity.class);
			intent.putExtra("doc_type", "privacy");
			startActivity(intent);
		});
		
		rowTermsOfService.setOnClickListener(v -> {
			Intent intent = new Intent(this, DocumentViewerActivity.class);
			intent.putExtra("doc_type", "terms");
			startActivity(intent);
		});
		
		// No open-source license list has been compiled for this project
		// yet — being honest about that rather than showing an empty or
		// fake screen.
		rowOpenSourceLicenses.setOnClickListener(v ->
		Toast.makeText(this, "License list coming soon", Toast.LENGTH_SHORT).show());
		
	}
	
	public void _MORE() {
	}
	private void _bindRealVersion() {
		try {
			PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
			tvAppVersion.setText("Version " + info.versionName);
		} catch (PackageManager.NameNotFoundException e) {
			tvAppVersion.setText("Version 1.0");
		}
	}
	
	{
	}
	
}

// Force update