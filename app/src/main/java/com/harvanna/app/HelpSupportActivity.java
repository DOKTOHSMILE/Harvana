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
import android.widget.ImageView;
import android.widget.LinearLayout;
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

public class HelpSupportActivity extends AppCompatActivity {
	
	private Toolbar _toolbar;
	private AppBarLayout _app_bar;
	private CoordinatorLayout _coordinator;
	
	private LinearLayout linear52;
	private TextView textview61;
	private TextView textview62;
	private TextView textview63;
	private MaterialCardView cardview12;
	private TextView textview72;
	private MaterialCardView cardview13;
	private LinearLayout linear53;
	private LinearLayout linear54;
	private View view55;
	private LinearLayout linear56;
	private View view57;
	private LinearLayout linear58;
	private View view59;
	private LinearLayout linear60;
	private TextView textview64;
	private TextView textview65;
	private TextView textview66;
	private TextView textview67;
	private TextView textview68;
	private TextView textview69;
	private TextView textview70;
	private TextView textview71;
	private LinearLayout rowContactEmail;
	private LinearLayout linear61;
	private ImageView imageview47;
	private TextView textview73;
	private TextView tvSupportEmail;
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.help_support);
		initialize(_savedInstanceState);
		FirebaseApp.initializeApp(this);
		initializeLogic();
		com.harvanna.app.utils.AppBar.header(this, "Help & support", "Answers, or reach us directly", R.id.textview61, R.id.textview62);
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
		linear52 = findViewById(R.id.linear52);
		textview61 = findViewById(R.id.textview61);
		textview62 = findViewById(R.id.textview62);
		textview63 = findViewById(R.id.textview63);
		cardview12 = findViewById(R.id.cardview12);
		textview72 = findViewById(R.id.textview72);
		cardview13 = findViewById(R.id.cardview13);
		linear53 = findViewById(R.id.linear53);
		linear54 = findViewById(R.id.linear54);
		view55 = findViewById(R.id.view55);
		linear56 = findViewById(R.id.linear56);
		view57 = findViewById(R.id.view57);
		linear58 = findViewById(R.id.linear58);
		view59 = findViewById(R.id.view59);
		linear60 = findViewById(R.id.linear60);
		textview64 = findViewById(R.id.textview64);
		textview65 = findViewById(R.id.textview65);
		textview66 = findViewById(R.id.textview66);
		textview67 = findViewById(R.id.textview67);
		textview68 = findViewById(R.id.textview68);
		textview69 = findViewById(R.id.textview69);
		textview70 = findViewById(R.id.textview70);
		textview71 = findViewById(R.id.textview71);
		rowContactEmail = findViewById(R.id.rowContactEmail);
		linear61 = findViewById(R.id.linear61);
		imageview47 = findViewById(R.id.imageview47);
		textview73 = findViewById(R.id.textview73);
		tvSupportEmail = findViewById(R.id.tvSupportEmail);
	}
	
	private void initializeLogic() {
		if (getSupportActionBar() != null) {
			getSupportActionBar().setTitle("Help & support");
			getSupportActionBar().setSubtitle("");
		}
		rowContactEmail.setOnClickListener(v -> {
			Intent intent = new Intent(Intent.ACTION_SENDTO);
			intent.setData(Uri.parse("mailto:support@harvanna.app"));
			intent.putExtra(Intent.EXTRA_SUBJECT, "Harvanna Support Request");
			try {
				startActivity(intent);
			} catch (Exception e) {
				android.widget.Toast.makeText(this,
				"No email app found. Reach us at support@harvanna.app", android.widget.Toast.LENGTH_LONG).show();
			}
		});
	}
	
}
