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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.biometric.*;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.widget.NestedScrollView;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.bumptech.glide.Glide;
import com.harvana.app.firestore.UserProfileManager;
import com.harvana.app.utils.ThemeManager;
import java.util.Map;


public class SettingsActivity extends AppCompatActivity {
	
	private Toolbar _toolbar;
	private AppBarLayout _app_bar;
	private CoordinatorLayout _coordinator;
	
	private NestedScrollView vscroll1;
	private LinearLayout linear2;
	private MaterialCardView cardview1;
	private TextView textview2;
	private MaterialCardView cardview2;
	private TextView textview11;
	private MaterialCardView cardview3;
	private TextView textview20;
	private MaterialCardView cardview4;
	private TextView textview29;
	private MaterialCardView cardview5;
	private TextView textview36;
	private MaterialCardView cardview6;
	private TextView textview45;
	private LinearLayout rowEditProfile;
	private ImageView ivSettingsAvatar;
	private LinearLayout linear3;
	private TextView tvEditProfileLink;
	private TextView tvSettingsName;
	private TextView tvSettingsHandle;
	private TextView tvSettingsVerified;
	private LinearLayout linear4;
	private LinearLayout rowAccountInfo;
	private View view6;
	private LinearLayout rowSecurityPrivacy;
	private View view8;
	private LinearLayout rowWalletAssets;
	private View view10;
	private LinearLayout rowBackupRecovery;
	private ImageView imageview1;
	private LinearLayout linear5;
	private ImageView imageview2;
	private TextView textview3;
	private TextView textview4;
	private ImageView imageview3;
	private LinearLayout linear7;
	private ImageView imageview4;
	private TextView textview5;
	private TextView textview6;
	private ImageView imageview5;
	private LinearLayout linear9;
	private ImageView imageview6;
	private TextView textview7;
	private TextView textview8;
	private ImageView imageview7;
	private LinearLayout linear11;
	private ImageView imageview8;
	private TextView textview9;
	private TextView textview10;
	private LinearLayout linear12;
	private LinearLayout rowPaymentMethods;
	private View view14;
	private LinearLayout rowTransactionHistory;
	private View view16;
	private LinearLayout rowLimitsFees;
	private View view18;
	private LinearLayout rowTaxReports;
	private ImageView imageview9;
	private LinearLayout linear13;
	private ImageView imageview10;
	private TextView textview12;
	private TextView textview13;
	private ImageView imageview11;
	private LinearLayout linear15;
	private ImageView imageview12;
	private TextView textview14;
	private TextView textview15;
	private ImageView imageview13;
	private LinearLayout linear17;
	private ImageView imageview14;
	private TextView textview16;
	private TextView textview17;
	private ImageView imageview15;
	private LinearLayout linear19;
	private ImageView imageview16;
	private TextView textview18;
	private TextView textview19;
	private LinearLayout linear20;
	private LinearLayout rowGroupsCircles;
	private View view22;
	private LinearLayout rowPrivacyVisibility;
	private View view24;
	private LinearLayout rowNotifications;
	private View view26;
	private LinearLayout rowBlockedUsers;
	private ImageView imageview17;
	private LinearLayout linear21;
	private ImageView imageview18;
	private TextView textview21;
	private TextView textview22;
	private ImageView imageview19;
	private LinearLayout linear23;
	private ImageView imageview20;
	private TextView textview23;
	private TextView textview24;
	private ImageView imageview21;
	private LinearLayout linear25;
	private ImageView imageview22;
	private TextView textview25;
	private TextView textview26;
	private ImageView imageview23;
	private LinearLayout linear27;
	private ImageView imageview24;
	private TextView textview27;
	private TextView textview28;
	private LinearLayout linear28;
	private LinearLayout rowThemeAppearance;
	private View view30;
	private LinearLayout rowLanguage;
	private View view32;
	private LinearLayout rowCurrency;
	private View view34;
	private LinearLayout rowDataStorage;
	private ImageView imageview25;
	private LinearLayout linear29;
	private ImageView imageview26;
	private TextView textview30;
	private TextView tvThemeValue;
	private ImageView imageview27;
	private LinearLayout linear31;
	private ImageView imageview28;
	private TextView textview31;
	private TextView textview32;
	private ImageView imageview29;
	private LinearLayout linear33;
	private ImageView imageview30;
	private TextView textview33;
	private TextView tvCurrencyValue;
	private ImageView imageview31;
	private LinearLayout linear35;
	private ImageView imageview32;
	private TextView textview34;
	private TextView textview35;
	private LinearLayout linear36;
	private LinearLayout rowHelpSupport;
	private View view38;
	private LinearLayout rowAboutHarvana;
	private View view40;
	private LinearLayout rowTermsConditions;
	private View view42;
	private LinearLayout rowLogOut;
	private ImageView imageview33;
	private LinearLayout linear37;
	private ImageView imageview34;
	private TextView textview37;
	private TextView textview38;
	private ImageView imageview35;
	private LinearLayout linear39;
	private ImageView imageview36;
	private TextView textview39;
	private TextView textview40;
	private ImageView imageview37;
	private LinearLayout linear41;
	private ImageView imageview38;
	private TextView textview41;
	private TextView textview42;
	private ImageView imageview39;
	private LinearLayout linear43;
	private TextView textview43;
	private TextView textview44;
	
	private SharedPreferences authsp;
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.settings);
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
		vscroll1 = findViewById(R.id.vscroll1);
		linear2 = findViewById(R.id.linear2);
		cardview1 = findViewById(R.id.cardview1);
		textview2 = findViewById(R.id.textview2);
		cardview2 = findViewById(R.id.cardview2);
		textview11 = findViewById(R.id.textview11);
		cardview3 = findViewById(R.id.cardview3);
		textview20 = findViewById(R.id.textview20);
		cardview4 = findViewById(R.id.cardview4);
		textview29 = findViewById(R.id.textview29);
		cardview5 = findViewById(R.id.cardview5);
		textview36 = findViewById(R.id.textview36);
		cardview6 = findViewById(R.id.cardview6);
		textview45 = findViewById(R.id.textview45);
		rowEditProfile = findViewById(R.id.rowEditProfile);
		ivSettingsAvatar = findViewById(R.id.ivSettingsAvatar);
		linear3 = findViewById(R.id.linear3);
		tvEditProfileLink = findViewById(R.id.tvEditProfileLink);
		tvSettingsName = findViewById(R.id.tvSettingsName);
		tvSettingsHandle = findViewById(R.id.tvSettingsHandle);
		tvSettingsVerified = findViewById(R.id.tvSettingsVerified);
		linear4 = findViewById(R.id.linear4);
		rowAccountInfo = findViewById(R.id.rowAccountInfo);
		view6 = findViewById(R.id.view6);
		rowSecurityPrivacy = findViewById(R.id.rowSecurityPrivacy);
		view8 = findViewById(R.id.view8);
		rowWalletAssets = findViewById(R.id.rowWalletAssets);
		view10 = findViewById(R.id.view10);
		rowBackupRecovery = findViewById(R.id.rowBackupRecovery);
		imageview1 = findViewById(R.id.imageview1);
		linear5 = findViewById(R.id.linear5);
		imageview2 = findViewById(R.id.imageview2);
		textview3 = findViewById(R.id.textview3);
		textview4 = findViewById(R.id.textview4);
		imageview3 = findViewById(R.id.imageview3);
		linear7 = findViewById(R.id.linear7);
		imageview4 = findViewById(R.id.imageview4);
		textview5 = findViewById(R.id.textview5);
		textview6 = findViewById(R.id.textview6);
		imageview5 = findViewById(R.id.imageview5);
		linear9 = findViewById(R.id.linear9);
		imageview6 = findViewById(R.id.imageview6);
		textview7 = findViewById(R.id.textview7);
		textview8 = findViewById(R.id.textview8);
		imageview7 = findViewById(R.id.imageview7);
		linear11 = findViewById(R.id.linear11);
		imageview8 = findViewById(R.id.imageview8);
		textview9 = findViewById(R.id.textview9);
		textview10 = findViewById(R.id.textview10);
		linear12 = findViewById(R.id.linear12);
		rowPaymentMethods = findViewById(R.id.rowPaymentMethods);
		view14 = findViewById(R.id.view14);
		rowTransactionHistory = findViewById(R.id.rowTransactionHistory);
		view16 = findViewById(R.id.view16);
		rowLimitsFees = findViewById(R.id.rowLimitsFees);
		view18 = findViewById(R.id.view18);
		rowTaxReports = findViewById(R.id.rowTaxReports);
		imageview9 = findViewById(R.id.imageview9);
		linear13 = findViewById(R.id.linear13);
		imageview10 = findViewById(R.id.imageview10);
		textview12 = findViewById(R.id.textview12);
		textview13 = findViewById(R.id.textview13);
		imageview11 = findViewById(R.id.imageview11);
		linear15 = findViewById(R.id.linear15);
		imageview12 = findViewById(R.id.imageview12);
		textview14 = findViewById(R.id.textview14);
		textview15 = findViewById(R.id.textview15);
		imageview13 = findViewById(R.id.imageview13);
		linear17 = findViewById(R.id.linear17);
		imageview14 = findViewById(R.id.imageview14);
		textview16 = findViewById(R.id.textview16);
		textview17 = findViewById(R.id.textview17);
		imageview15 = findViewById(R.id.imageview15);
		linear19 = findViewById(R.id.linear19);
		imageview16 = findViewById(R.id.imageview16);
		textview18 = findViewById(R.id.textview18);
		textview19 = findViewById(R.id.textview19);
		linear20 = findViewById(R.id.linear20);
		rowGroupsCircles = findViewById(R.id.rowGroupsCircles);
		view22 = findViewById(R.id.view22);
		rowPrivacyVisibility = findViewById(R.id.rowPrivacyVisibility);
		view24 = findViewById(R.id.view24);
		rowNotifications = findViewById(R.id.rowNotifications);
		view26 = findViewById(R.id.view26);
		rowBlockedUsers = findViewById(R.id.rowBlockedUsers);
		imageview17 = findViewById(R.id.imageview17);
		linear21 = findViewById(R.id.linear21);
		imageview18 = findViewById(R.id.imageview18);
		textview21 = findViewById(R.id.textview21);
		textview22 = findViewById(R.id.textview22);
		imageview19 = findViewById(R.id.imageview19);
		linear23 = findViewById(R.id.linear23);
		imageview20 = findViewById(R.id.imageview20);
		textview23 = findViewById(R.id.textview23);
		textview24 = findViewById(R.id.textview24);
		imageview21 = findViewById(R.id.imageview21);
		linear25 = findViewById(R.id.linear25);
		imageview22 = findViewById(R.id.imageview22);
		textview25 = findViewById(R.id.textview25);
		textview26 = findViewById(R.id.textview26);
		imageview23 = findViewById(R.id.imageview23);
		linear27 = findViewById(R.id.linear27);
		imageview24 = findViewById(R.id.imageview24);
		textview27 = findViewById(R.id.textview27);
		textview28 = findViewById(R.id.textview28);
		linear28 = findViewById(R.id.linear28);
		rowThemeAppearance = findViewById(R.id.rowThemeAppearance);
		view30 = findViewById(R.id.view30);
		rowLanguage = findViewById(R.id.rowLanguage);
		view32 = findViewById(R.id.view32);
		rowCurrency = findViewById(R.id.rowCurrency);
		view34 = findViewById(R.id.view34);
		rowDataStorage = findViewById(R.id.rowDataStorage);
		imageview25 = findViewById(R.id.imageview25);
		linear29 = findViewById(R.id.linear29);
		imageview26 = findViewById(R.id.imageview26);
		textview30 = findViewById(R.id.textview30);
		tvThemeValue = findViewById(R.id.tvThemeValue);
		imageview27 = findViewById(R.id.imageview27);
		linear31 = findViewById(R.id.linear31);
		imageview28 = findViewById(R.id.imageview28);
		textview31 = findViewById(R.id.textview31);
		textview32 = findViewById(R.id.textview32);
		imageview29 = findViewById(R.id.imageview29);
		linear33 = findViewById(R.id.linear33);
		imageview30 = findViewById(R.id.imageview30);
		textview33 = findViewById(R.id.textview33);
		tvCurrencyValue = findViewById(R.id.tvCurrencyValue);
		imageview31 = findViewById(R.id.imageview31);
		linear35 = findViewById(R.id.linear35);
		imageview32 = findViewById(R.id.imageview32);
		textview34 = findViewById(R.id.textview34);
		textview35 = findViewById(R.id.textview35);
		linear36 = findViewById(R.id.linear36);
		rowHelpSupport = findViewById(R.id.rowHelpSupport);
		view38 = findViewById(R.id.view38);
		rowAboutHarvana = findViewById(R.id.rowAboutHarvana);
		view40 = findViewById(R.id.view40);
		rowTermsConditions = findViewById(R.id.rowTermsConditions);
		view42 = findViewById(R.id.view42);
		rowLogOut = findViewById(R.id.rowLogOut);
		imageview33 = findViewById(R.id.imageview33);
		linear37 = findViewById(R.id.linear37);
		imageview34 = findViewById(R.id.imageview34);
		textview37 = findViewById(R.id.textview37);
		textview38 = findViewById(R.id.textview38);
		imageview35 = findViewById(R.id.imageview35);
		linear39 = findViewById(R.id.linear39);
		imageview36 = findViewById(R.id.imageview36);
		textview39 = findViewById(R.id.textview39);
		textview40 = findViewById(R.id.textview40);
		imageview37 = findViewById(R.id.imageview37);
		linear41 = findViewById(R.id.linear41);
		imageview38 = findViewById(R.id.imageview38);
		textview41 = findViewById(R.id.textview41);
		textview42 = findViewById(R.id.textview42);
		imageview39 = findViewById(R.id.imageview39);
		linear43 = findViewById(R.id.linear43);
		textview43 = findViewById(R.id.textview43);
		textview44 = findViewById(R.id.textview44);
		authsp = getSharedPreferences("authsp", Activity.MODE_PRIVATE);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		// Back from Edit profile or another screen: the header and the values show what is saved now.
		if (tvSettingsName != null) {
			_loadProfileHeader();
			_bindCurrentValues();
		}
	}
	
	private void initializeLogic() {
		_compactList();
		if (getSupportActionBar() != null) {
			getSupportActionBar().setTitle("Settings");
			getSupportActionBar().setSubtitle(null);
		}
		_applyCategoryTints();
		_loadProfileHeader();
		_bindCurrentValues();
		_wireNavigation();
	}
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		final int _id = item.getItemId();
		final String _title = (String) item.getTitle();
		
		return super.onOptionsItemSelected(item);
	}
	public void _MORE() {
	}
	private void _applyCategoryTints() {
		int green = ContextCompat.getColor(this, R.color.colorAccent);
		int amber = ContextCompat.getColor(this, R.color.colorAmber);
		int blue = ContextCompat.getColor(this, R.color.colorInfo);
		int violet = ContextCompat.getColor(this, R.color.colorSecondaryAccent);
		int muted = ContextCompat.getColor(this, R.color.colorTextTertiary);
		int red = ContextCompat.getColor(this, R.color.colorError);
		
		_tintRowIcon(rowAccountInfo, green);
		_tintRowIcon(rowSecurityPrivacy, green);
		_tintRowIcon(rowWalletAssets, green);
		_tintRowIcon(rowBackupRecovery, green);
		
		_tintRowIcon(rowPaymentMethods, amber);
		_tintRowIcon(rowTransactionHistory, amber);
		_tintRowIcon(rowLimitsFees, amber);
		_tintRowIcon(rowTaxReports, amber);
		
		_tintRowIcon(rowGroupsCircles, blue);
		_tintRowIcon(rowPrivacyVisibility, blue);
		_tintRowIcon(rowNotifications, blue);
		_tintRowIcon(rowBlockedUsers, blue);
		
		_tintRowIcon(rowThemeAppearance, violet);
		_tintRowIcon(rowLanguage, violet);
		_tintRowIcon(rowCurrency, violet);
		_tintRowIcon(rowDataStorage, violet);
		
		_tintRowIcon(rowHelpSupport, muted);
		_tintRowIcon(rowAboutHarvana, muted);
		_tintRowIcon(rowTermsConditions, muted);
		
		_tintRowIcon(rowLogOut, red);
		
		if (ivSettingsAvatar != null) {
			ivSettingsAvatar.setBackgroundTintList(ColorStateList.valueOf(green));
		}
		if (tvSettingsVerified != null) {
			tvSettingsVerified.setBackgroundTintList(ColorStateList.valueOf(green));
		}
	}
	
	private void _tintRowIcon(LinearLayout row, int color) {
		if (row == null || row.getChildCount() == 0) return;
		View icon = row.getChildAt(0);
		if (icon instanceof ImageView) {
			// The icon alone in the category colour, centred, no badge behind it.
			icon.setBackground(null);
			icon.setBackgroundTintList(null);
			((ImageView) icon).setScaleType(ImageView.ScaleType.FIT_CENTER);
			((ImageView) icon).setImageTintList(ColorStateList.valueOf(color));
		}
	}
	
	private void _loadProfileHeader() {
		// The signed-in account (the old saved uid can be empty, which showed "Guest").
		com.google.firebase.auth.FirebaseUser fu = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
		String uid = fu != null ? fu.getUid() : authsp.getString("uid", "");
		if (fu != null && fu.getDisplayName() != null && !fu.getDisplayName().isEmpty()) tvSettingsName.setText(fu.getDisplayName());
		if (uid.isEmpty()) {
			tvSettingsName.setText("Guest");
			return;
		}
		
		Map<String, Object> cached = com.harvana.app.offline.OfflineCacheManager.load(getApplicationContext(), "profile_" + uid);
		if (cached != null) {
			_bindProfileHeader(cached);
		}
		
		UserProfileManager.getInstance(this).getProfile(uid, new UserProfileManager.DocCallback() {
			@Override
			public void onSuccess(Map<String, Object> data) {
				runOnUiThread(() -> _bindProfileHeader(data));
			}
			
			@Override
			public void onError(String error) {}
		});
	}
	
	private void _bindProfileHeader(Map<String, Object> data) {
		String iconStr = _str(data, "icon");
		String firstName = _str(data, "first_name");
		String lastName = _str(data, "last_name");
		String vType = _str(data, "v_type");
		String fullName = (firstName + " " + lastName).trim();
		tvSettingsName.setText(fullName.isEmpty() ? "Harvana Member" : fullName);
		String username = _str(data, "username");
		if (!username.isEmpty() && !"null".equalsIgnoreCase(username)) {
			tvSettingsHandle.setText("@" + username);
			tvSettingsHandle.setVisibility(View.VISIBLE);
		} else {
			tvSettingsHandle.setVisibility(View.GONE);
		}
		// No ID check exists (NIN/BVN were removed), so no "verified" badge is shown.
		tvSettingsVerified.setVisibility(View.GONE);
		
		if (!iconStr.isEmpty() && !"null".equalsIgnoreCase(iconStr)) {
			Glide.with(getApplicationContext())
			.load(Uri.parse(iconStr))
			.placeholder(R.drawable.ic_person)
			.into(ivSettingsAvatar);
			ivSettingsAvatar.setBackgroundTintList(null);
			ivSettingsAvatar.setPadding(0, 0, 0, 0);
		}
	}
	
	private String _str(Map<String, Object> data, String key) {
		Object v = data.get(key);
		return v == null ? "" : String.valueOf(v);
	}
	
	private double _num(Map<String, Object> data, String key) {
		Object v = data.get(key);
		if (v instanceof Number) return ((Number) v).doubleValue();
		try { return Double.parseDouble(String.valueOf(v)); } catch (Exception e) { return 0; }
	}
	
	private void _bindCurrentValues() {
		tvThemeValue.setText(ThemeManager.label(this));
		
		SharedPreferences currencySp = getSharedPreferences("currencysp", Activity.MODE_PRIVATE);
		String currency = currencySp.getString("currency", "₦");
		tvCurrencyValue.setText(_currencyCode(currency));
	}
	
	private static final String[][] SUPPORTED_CURRENCIES = {
		{"₦", "NGN — Nigerian Naira"},
		{"$", "USD — US Dollar"},
		{"£", "GBP — British Pound"},
		{"€", "EUR — Euro"},
		{"GH₵", "GHS — Ghanaian Cedi"},
		{"KSh", "KES — Kenyan Shilling"},
		{"R", "ZAR — South African Rand"},
	};
	
	private String _currencyCode(String symbol) {
		String label = _currencyLabel(symbol);
		int dash = label.indexOf(" — ");
		return dash > 0 ? label.substring(0, dash) : label;
	}
	
	private String _currencyLabel(String symbol) {
		for (String[] entry : SUPPORTED_CURRENCIES) {
			if (entry[0].equals(symbol)) return entry[1];
		}
		return symbol;
	}
	
	private void _wireNavigation() {
		rowEditProfile.setOnClickListener(v -> _openActivity(EditProfileActivity.class));
		rowAccountInfo.setOnClickListener(v -> _openActivity(EditProfileActivity.class));
		rowSecurityPrivacy.setOnClickListener(v -> _openActivity(SecurityPinSettingsActivity.class));
		rowAboutHarvana.setOnClickListener(v -> _openActivity(AboutActivity.class));
		rowTermsConditions.setOnClickListener(v -> _openActivity(DocumentViewerActivity.class));
		
		rowGroupsCircles.setOnClickListener(v -> {
			Intent intent = new Intent(this, MainActivity.class);
			intent.putExtra("open_tab", "groups");
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
			startActivity(intent);
		});
		
		rowWalletAssets.setOnClickListener(v -> {
			Intent intent = new Intent(this, MainActivity.class);
			intent.putExtra("open_fragment", "asset");
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
			startActivity(intent);
		});
		
		rowThemeAppearance.setOnClickListener(v -> _showThemePicker());
		rowCurrency.setOnClickListener(v -> _showCurrencyPicker());
		rowDataStorage.setOnClickListener(v -> _showDataStorageDialog());
		rowNotifications.setOnClickListener(v -> _openActivity(NotificationSettingsActivity.class));
		rowPrivacyVisibility.setOnClickListener(v -> _openActivity(PrivacySettingsActivity.class));
		
		View.OnClickListener comingSoon = v -> Toast.makeText(this,
		"This section is coming soon", Toast.LENGTH_SHORT).show();
		rowBackupRecovery.setOnClickListener(v -> _openActivity(BackupRecoveryActivity.class));
		rowPaymentMethods.setOnClickListener(v -> _openActivity(PaymentMethodsActivity.class));
		rowTransactionHistory.setOnClickListener(v -> {
			Intent intent = new Intent(this, MainActivity.class);
			intent.putExtra("open_fragment", "asset");
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
			startActivity(intent);
			Toast.makeText(this, "Open the Activity tab to view your transaction history", Toast.LENGTH_LONG).show();
		});
		rowLimitsFees.setOnClickListener(comingSoon);
		rowTaxReports.setOnClickListener(comingSoon);
		rowBlockedUsers.setOnClickListener(v -> _openActivity(BlockedUsersActivity.class));
		rowLanguage.setOnClickListener(v -> _showLanguagePicker());
		rowHelpSupport.setOnClickListener(v -> _openActivity(HelpSupportActivity.class));
		
		rowLogOut.setOnClickListener(v -> _confirmLogOut());
	}
	
	private void _showCurrencyPicker() {
		SharedPreferences currencySp = getSharedPreferences("currencysp", Activity.MODE_PRIVATE);
		String current = currencySp.getString("currency", "₦");
		
		String[] labels = new String[SUPPORTED_CURRENCIES.length];
		int selectedIndex = 0;
		for (int i = 0; i < SUPPORTED_CURRENCIES.length; i++) {
			labels[i] = SUPPORTED_CURRENCIES[i][1];
			if (SUPPORTED_CURRENCIES[i][0].equals(current)) selectedIndex = i;
		}
		
		new com.harvana.app.ui.SheetDialog.Builder(this)
		.setTitle("Currency")
		.setSingleChoiceItems(labels, selectedIndex, (dialog, which) -> {
			String chosenSymbol = SUPPORTED_CURRENCIES[which][0];
			com.harvana.app.wallet.FxRates.save(this, chosenSymbol);
			com.harvana.app.wallet.FxRates.load(this, null);
			tvCurrencyValue.setText(_currencyCode(chosenSymbol));
			dialog.dismiss();
		})
		.setNegativeButton("Cancel", null)
		.show();
	}
	
	private void _showDataStorageDialog() {
		new com.harvana.app.ui.SheetDialog.Builder(this)
		.setTitle("Clear cache")
		.setPositiveButton("Clear", (dialog, which) -> {
			com.harvana.app.offline.OfflineCacheManager.clearAll(getApplicationContext());
			com.harvana.app.sync.NodeReadCache.clear();
			Toast.makeText(this, "Cache cleared", Toast.LENGTH_SHORT).show();
		})
		.setNegativeButton("Cancel", null)
		.show();
	}
	
	private void _showLanguagePicker() {
		new com.harvana.app.ui.SheetDialog.Builder(this)
		.setTitle("Language")
		.setSingleChoiceItems(new String[]{"English"}, 0, (dialog, which) -> dialog.dismiss())
		.show();
	}
	
	/**
	 * Keeps Settings short: rows that repeat another screen or are not built yet are hidden (Account information is
	 * Edit profile; groups, history and terms live on their own screens or in About; limits, tax reports and other
	 * languages are not ready), each row shows its title only, and rows are tighter.
	 */
	private void _compactList() {
		View[] hide = {rowAccountInfo, rowLimitsFees, rowTaxReports, rowGroupsCircles, rowTransactionHistory, rowTermsConditions, rowLanguage};
		for (View row : hide) {
			if (row == null) continue;
			row.setVisibility(View.GONE);
			android.view.ViewGroup parent = (android.view.ViewGroup) row.getParent();
			int at = parent.indexOfChild(row);
			// Its divider: the one after it, or the one before it when it was the last row in the card.
			View after = at + 1 < parent.getChildCount() ? parent.getChildAt(at + 1) : null;
			View before = at > 0 ? parent.getChildAt(at - 1) : null;
			if (after instanceof com.google.android.material.divider.MaterialDivider) after.setVisibility(View.GONE);
			else if (before instanceof com.google.android.material.divider.MaterialDivider) before.setVisibility(View.GONE);
		}
		final float d = getResources().getDisplayMetrics().density;
		View[] rows = {rowSecurityPrivacy, rowWalletAssets, rowBackupRecovery, rowPaymentMethods, rowPrivacyVisibility, rowNotifications,
			rowBlockedUsers, rowThemeAppearance, rowCurrency, rowDataStorage, rowHelpSupport, rowAboutHarvana, rowLogOut};
		for (View r : rows) {
			if (!(r instanceof android.view.ViewGroup)) continue;
			android.view.ViewGroup row = (android.view.ViewGroup) r;
			row.setPadding((int) (14 * d), (int) (9 * d), (int) (14 * d), (int) (9 * d));
			for (int c = 0; c < row.getChildCount(); c++) {
				View child = row.getChildAt(c);
				if (c == 0 && child.getLayoutParams() != null) {           // the icon
					child.getLayoutParams().width = (int) (24 * d);
					child.getLayoutParams().height = (int) (24 * d);
					child.setPadding(0, 0, 0, 0);
					if (child instanceof ImageView) ((ImageView) child).setScaleType(ImageView.ScaleType.FIT_CENTER);
				}
				if (child instanceof LinearLayout && ((LinearLayout) child).getChildCount() > 1) {
					((LinearLayout) child).getChildAt(1).setVisibility(View.GONE);   // the grey line under the title
				}
			}
		}
	}

	private void _openActivity(Class<?> activityClass) {
		startActivity(new Intent(this, activityClass));
	}
	
	private void _showThemePicker() {
		String mode = ThemeManager.getMode(this);
		int checked = "light".equals(mode) ? 1 : "dark".equals(mode) ? 2 : 0;
		new com.harvana.app.ui.SheetDialog.Builder(this)
		.setTitle("Appearance")
		.setSingleChoiceItems(new String[]{ThemeManager.systemLabel(this), "Light", "Dark"}, checked, (dialog, which) -> {
			// The whole app re-themes immediately: AppCompat restarts every open screen in the new mode.
			if (which == 1) ThemeManager.setLightMode(this);
			else if (which == 2) ThemeManager.setDarkMode(this);
			else ThemeManager.setSystemMode(this);
			dialog.dismiss();
		})
		.setNegativeButton("Cancel", null)
		.show();
	}
	
	private void _confirmLogOut() {
		new com.harvana.app.ui.SheetDialog.Builder(this)
		.setTitle("Log out")
		.setPositiveButton("Log out", (dialog, which) -> _performLogOut())
		.setNegativeButton("Cancel", null)
		.show();
	}
	
	private void _performLogOut() {
		// The same sign-out as the side menu: every store this app writes, the wallet keys included.
		com.harvana.app.auth.LogoutManager.logout(getApplicationContext());
		Intent intent = new Intent(this, SplashScreenActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		startActivity(intent);
		finish();
	}
	
	{
	}
	
}
