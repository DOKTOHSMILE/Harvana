package com.harvanna.wallet;

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
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.*;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.budiyev.android.codescanner.*;
import com.bumptech.glide.*;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
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
import com.harvanna.wallet.firestore.UserProfileManager;
import androidx.core.content.ContextCompat;
import android.content.pm.PackageManager;


public class CompleteProfileFragmentActivity extends Fragment {
	
	private LocationHelper helper;
	private String idDocument = "bvn";
	private String longitudeString = "";
	private String latitudeString = "";
	
	private NestedScrollView vscroll1;
	private LinearLayout linear26;
	private MaterialCardView cardview45;
	private LinearLayout linear27;
	private TextView textview49;
	private TextView textview50;
	private TextView textview51;
	private TextView textview52;
	private MaterialCardView cardview46;
	private TextView textview53;
	private MaterialCardView cardview47;
	private TextView textview54;
	private MaterialCardView cardview48;
	private TextView textview55;
	private TextView textview56;
	private MaterialCardView cardview49;
	private TextView textview57;
	private LinearLayout linear28;
	private TextView textview58;
	private MaterialCardView cardview52;
	private LinearLayout linear29;
	private TextView textview61;
	private MaterialCardView cardview55;
	private TextView textview62;
	private MaterialCardView cardview56;
	private TextView textview63;
	private LinearLayout linear32;
	private TextView tv_id_number_label;
	private MaterialCardView cardview59;
	private TextView textview64;
	private TextView textview65;
	private MaterialCardView cardview60;
	private TextView textview66;
	private MaterialCardView cardview61;
	private TextView textview67;
	private MaterialCardView cardview62;
	private LinearLayout linear33;
	private TextView textview70;
	private MaterialCardView cardview65;
	private LinearLayout linear36;
	private LinearLayout linear37;
	private MaterialCardView cardview66;
	private EditText et_first_name;
	private EditText et_last_name;
	private TextView tv_dob;
	private EditText et_email;
	private MaterialCardView cardview50;
	private MaterialCardView cardview51;
	private EditText et_phone_code;
	private EditText et_phone_number;
	private Spinner spinner_country;
	private LinearLayout linear30;
	private LinearLayout linear31;
	private TextView textview59;
	private MaterialCardView cardview53;
	private Spinner spinner_state;
	private TextView textview60;
	private MaterialCardView cardview54;
	private Spinner spinner_city;
	private EditText et_address;
	private EditText et_occupation;
	private MaterialCardView cardview57;
	private MaterialCardView cardview58;
	private TextView btn_id_type_bvn;
	private TextView btn_id_type_nin;
	private EditText et_id_number;
	private EditText et_kin_name;
	private EditText et_kin_relationship;
	private EditText et_kin_phone;
	private LinearLayout linear34;
	private LinearLayout linear35;
	private TextView textview68;
	private MaterialCardView cardview63;
	private Spinner spinner_kin_state;
	private TextView textview69;
	private MaterialCardView cardview64;
	private Spinner spinner_kin_city;
	private EditText et_kin_address;
	private CheckBox checkbox_terms;
	private TextView textview71;
	private CheckBox checkbox_privacy;
	private TextView textview72;
	private TextView btn_submit_profile;
	
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
	
	@NonNull
	@Override
	public View onCreateView(@NonNull LayoutInflater _inflater, @Nullable ViewGroup _container, @Nullable Bundle _savedInstanceState) {
		View _view = _inflater.inflate(R.layout.complete_profile_fragment, _container, false);
		initialize(_savedInstanceState, _view);
		FirebaseApp.initializeApp(getContext());
		initializeLogic();
		return _view;
	}
	
	private void initialize(Bundle _savedInstanceState, View _view) {
		helper = new LocationHelper(getContext());
		vscroll1 = _view.findViewById(R.id.vscroll1);
		linear26 = _view.findViewById(R.id.linear26);
		cardview45 = _view.findViewById(R.id.cardview45);
		linear27 = _view.findViewById(R.id.linear27);
		textview49 = _view.findViewById(R.id.textview49);
		textview50 = _view.findViewById(R.id.textview50);
		textview51 = _view.findViewById(R.id.textview51);
		textview52 = _view.findViewById(R.id.textview52);
		cardview46 = _view.findViewById(R.id.cardview46);
		textview53 = _view.findViewById(R.id.textview53);
		cardview47 = _view.findViewById(R.id.cardview47);
		textview54 = _view.findViewById(R.id.textview54);
		cardview48 = _view.findViewById(R.id.cardview48);
		textview55 = _view.findViewById(R.id.textview55);
		textview56 = _view.findViewById(R.id.textview56);
		cardview49 = _view.findViewById(R.id.cardview49);
		textview57 = _view.findViewById(R.id.textview57);
		linear28 = _view.findViewById(R.id.linear28);
		textview58 = _view.findViewById(R.id.textview58);
		cardview52 = _view.findViewById(R.id.cardview52);
		linear29 = _view.findViewById(R.id.linear29);
		textview61 = _view.findViewById(R.id.textview61);
		cardview55 = _view.findViewById(R.id.cardview55);
		textview62 = _view.findViewById(R.id.textview62);
		cardview56 = _view.findViewById(R.id.cardview56);
		textview63 = _view.findViewById(R.id.textview63);
		linear32 = _view.findViewById(R.id.linear32);
		tv_id_number_label = _view.findViewById(R.id.tv_id_number_label);
		cardview59 = _view.findViewById(R.id.cardview59);
		textview64 = _view.findViewById(R.id.textview64);
		textview65 = _view.findViewById(R.id.textview65);
		cardview60 = _view.findViewById(R.id.cardview60);
		textview66 = _view.findViewById(R.id.textview66);
		cardview61 = _view.findViewById(R.id.cardview61);
		textview67 = _view.findViewById(R.id.textview67);
		cardview62 = _view.findViewById(R.id.cardview62);
		linear33 = _view.findViewById(R.id.linear33);
		textview70 = _view.findViewById(R.id.textview70);
		cardview65 = _view.findViewById(R.id.cardview65);
		linear36 = _view.findViewById(R.id.linear36);
		linear37 = _view.findViewById(R.id.linear37);
		cardview66 = _view.findViewById(R.id.cardview66);
		et_first_name = _view.findViewById(R.id.et_first_name);
		et_last_name = _view.findViewById(R.id.et_last_name);
		tv_dob = _view.findViewById(R.id.tv_dob);
		et_email = _view.findViewById(R.id.et_email);
		cardview50 = _view.findViewById(R.id.cardview50);
		cardview51 = _view.findViewById(R.id.cardview51);
		et_phone_code = _view.findViewById(R.id.et_phone_code);
		et_phone_number = _view.findViewById(R.id.et_phone_number);
		spinner_country = _view.findViewById(R.id.spinner_country);
		linear30 = _view.findViewById(R.id.linear30);
		linear31 = _view.findViewById(R.id.linear31);
		textview59 = _view.findViewById(R.id.textview59);
		cardview53 = _view.findViewById(R.id.cardview53);
		spinner_state = _view.findViewById(R.id.spinner_state);
		textview60 = _view.findViewById(R.id.textview60);
		cardview54 = _view.findViewById(R.id.cardview54);
		spinner_city = _view.findViewById(R.id.spinner_city);
		et_address = _view.findViewById(R.id.et_address);
		et_occupation = _view.findViewById(R.id.et_occupation);
		cardview57 = _view.findViewById(R.id.cardview57);
		cardview58 = _view.findViewById(R.id.cardview58);
		btn_id_type_bvn = _view.findViewById(R.id.btn_id_type_bvn);
		btn_id_type_nin = _view.findViewById(R.id.btn_id_type_nin);
		et_id_number = _view.findViewById(R.id.et_id_number);
		et_kin_name = _view.findViewById(R.id.et_kin_name);
		et_kin_relationship = _view.findViewById(R.id.et_kin_relationship);
		et_kin_phone = _view.findViewById(R.id.et_kin_phone);
		linear34 = _view.findViewById(R.id.linear34);
		linear35 = _view.findViewById(R.id.linear35);
		textview68 = _view.findViewById(R.id.textview68);
		cardview63 = _view.findViewById(R.id.cardview63);
		spinner_kin_state = _view.findViewById(R.id.spinner_kin_state);
		textview69 = _view.findViewById(R.id.textview69);
		cardview64 = _view.findViewById(R.id.cardview64);
		spinner_kin_city = _view.findViewById(R.id.spinner_kin_city);
		et_kin_address = _view.findViewById(R.id.et_kin_address);
		checkbox_terms = _view.findViewById(R.id.checkbox_terms);
		textview71 = _view.findViewById(R.id.textview71);
		checkbox_privacy = _view.findViewById(R.id.checkbox_privacy);
		textview72 = _view.findViewById(R.id.textview72);
		btn_submit_profile = _view.findViewById(R.id.btn_submit_profile);
		authsp = getContext().getSharedPreferences("authsp", Activity.MODE_PRIVATE);
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
		// 1. Use the class variables already bound in initialize() instead of calling getView()
		MaterialCardView bvnCard = (MaterialCardView) btn_id_type_bvn.getParent();
		MaterialCardView ninCard = (MaterialCardView) btn_id_type_nin.getParent();
		
		bvnCard.setOnClickListener(v -> _selectIdType("bvn", bvnCard, ninCard, btn_id_type_bvn, btn_id_type_nin));
		ninCard.setOnClickListener(v -> _selectIdType("nin", bvnCard, ninCard, btn_id_type_bvn, btn_id_type_nin));
		
		// 2. Use getParentFragmentManager() instead of getActivity().getSupportFragmentManager()
		tv_dob.setOnClickListener(v -> {
			DialogFragment newFragment = new DatePickerFragment();
			newFragment.show(getParentFragmentManager(), "datePicker");
		});
		
		helper.setSpinnerData(spinner_country, helper.getCountryNames());
		spinner_country.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
				String country = spinner_country.getSelectedItem() != null ? spinner_country.getSelectedItem().toString() : "";
				helper.setSpinnerData(spinner_state, helper.getStateNames(country));
				helper.setSpinnerData(spinner_city, new ArrayList<>());
				helper.setSpinnerData(spinner_kin_state, helper.getStateNames(country));
				helper.setSpinnerData(spinner_kin_city, new ArrayList<>());
				String code = _phoneCodeFor(country);
				if (!code.isEmpty() && et_phone_code.getText().toString().trim().isEmpty()) {
					et_phone_code.setText(code);
				}
			}
			@Override public void onNothingSelected(AdapterView<?> parent) {}
		});
		
		spinner_state.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
				String country = spinner_country.getSelectedItem() != null ? spinner_country.getSelectedItem().toString() : "";
				String state = spinner_state.getSelectedItem() != null ? spinner_state.getSelectedItem().toString() : "";
				helper.setSpinnerData(spinner_city, helper.getCityNames(country, state));
			}
			@Override public void onNothingSelected(AdapterView<?> parent) {}
		});
		
		spinner_kin_state.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
				String country = spinner_country.getSelectedItem() != null ? spinner_country.getSelectedItem().toString() : "";
				String state = spinner_kin_state.getSelectedItem() != null ? spinner_kin_state.getSelectedItem().toString() : "";
				helper.setSpinnerData(spinner_kin_city, helper.getCityNames(country, state));
			}
			@Override public void onNothingSelected(AdapterView<?> parent) {}
		});
		
		if (!authsp.getString("gmail", "").isEmpty()) {
			et_email.setText(authsp.getString("gmail", ""));
		}
		
		// Sync with the sign-in account (email / Google): the auth email is pre-filled and locked.
		com.google.firebase.auth.FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
		if (fbUser != null) {
			String authEmail = fbUser.getEmail();
			if (authEmail != null && !authEmail.isEmpty()) {
				et_email.setText(authEmail);
				et_email.setEnabled(false);
			}
			String display = fbUser.getDisplayName();
			if (display != null && !display.trim().isEmpty() && et_first_name.getText().toString().isEmpty()) {
				String[] parts = display.trim().split("\\s+", 2);
				et_first_name.setText(parts[0]);
				if (parts.length > 1 && et_last_name.getText().toString().isEmpty()) et_last_name.setText(parts[1]);
			}
		}
		
		if (!authsp.getString("first_name", "").isEmpty()) {
			et_first_name.setText(authsp.getString("first_name", ""));
		}
		
		if (!authsp.getString("last_name", "").isEmpty()) {
			et_last_name.setText(authsp.getString("last_name", ""));
		}
		
		btn_submit_profile.setOnClickListener(v -> _validateAndSubmit());
		
		_captureGpsLocationIfPermitted();
		
	}
	
	public void _MORE() {
	}
	private void _selectIdType(String type, MaterialCardView bvnCard, MaterialCardView ninCard, TextView bvnLabel, TextView ninLabel) {
		idDocument = type;
		boolean isBvn = "bvn".equals(type);
		bvnCard.setCardBackgroundColor(getResources().getColor(isBvn ? R.color.colorAccentStrong : R.color.whiteFaint10));
		ninCard.setCardBackgroundColor(getResources().getColor(!isBvn ? R.color.colorAccentStrong : R.color.whiteFaint10));
		bvnLabel.setTextColor(getResources().getColor(isBvn ? R.color.colorSurface : R.color.colorTextPrimary));
		ninLabel.setTextColor(getResources().getColor(!isBvn ? R.color.colorSurface : R.color.colorTextPrimary));
		tv_id_number_label.setText((isBvn ? "BVN" : "NIN") + " Number");
	}
	
	/** Real dial codes for the countries LocationHelper actually lists -
*  a convenience prefill, not required (the field stays editable). */
	private String _phoneCodeFor(String country) {
		if (country == null) return "";
		switch (country) {
			case "Nigeria": return "+234";
			case "Ghana": return "+233";
			case "Kenya": return "+254";
			case "South Africa": return "+27";
			default: return "";
		}
	}
	
	private void _validateAndSubmit() {
		if (et_first_name.getText().toString().trim().isEmpty()) {
			et_first_name.setError("Please enter your first name");
			return;
		}
		if (et_last_name.getText().toString().trim().isEmpty()) {
			et_last_name.setError("Please enter your last name");
			return;
		}
		if (tv_dob.getText().toString().trim().isEmpty() || "Tap to select".equals(tv_dob.getText().toString().trim())) {
			SketchwareUtil.showMessage(getContext(), "Please select your date of birth");
			return;
		}
		if (et_email.getText().toString().trim().isEmpty()) {
			et_email.setError("Please input your email address");
			return;
		}
		if (et_phone_number.getText().toString().trim().isEmpty()) {
			et_phone_number.setError("Please input your phone number");
			return;
		}
		String country = spinner_country.getSelectedItem() != null ? spinner_country.getSelectedItem().toString() : "";
		if (country.trim().isEmpty()) {
			SketchwareUtil.showMessage(getContext(), "Please select your country");
			return;
		}
		String state = spinner_state.getSelectedItem() != null ? spinner_state.getSelectedItem().toString() : "";
		if (state.trim().isEmpty()) {
			SketchwareUtil.showMessage(getContext(), "Please select your state");
			return;
		}
		if (et_address.getText().toString().trim().isEmpty()) {
			et_address.setError("Please input your home address");
			return;
		}
		if (et_occupation.getText().toString().trim().isEmpty()) {
			et_occupation.setError("Please input your occupation");
			return;
		}
		if (et_id_number.getText().toString().trim().isEmpty()) {
			et_id_number.setError("Please input your " + idDocument.toUpperCase());
			return;
		}
		if (et_kin_name.getText().toString().trim().isEmpty()) {
			et_kin_name.setError("Please input your Next of Kin");
			return;
		}
		if (et_kin_relationship.getText().toString().trim().isEmpty()) {
			et_kin_relationship.setError("Please input your relationship");
			return;
		}
		String kinState = spinner_kin_state.getSelectedItem() != null ? spinner_kin_state.getSelectedItem().toString() : "";
		if (kinState.trim().isEmpty()) {
			SketchwareUtil.showMessage(getContext(), "Please select your Next of Kin's state");
			return;
		}
		if (et_kin_address.getText().toString().trim().isEmpty()) {
			et_kin_address.setError("Please enter your Next of Kin's address");
			return;
		}
		if (et_kin_phone.getText().toString().trim().isEmpty()) {
			et_kin_phone.setError("Please enter your Next of Kin's phone number");
			return;
		}
		if (!checkbox_terms.isChecked()) {
			SketchwareUtil.showMessage(getContext(), "Please agree to the Terms of Service");
			return;
		}
		if (!checkbox_privacy.isChecked()) {
			SketchwareUtil.showMessage(getContext(), "Please agree to the Privacy Policy");
			return;
		}
		// The profile is keyed to the real Firebase Auth uid; there is no fallback.
		com.google.firebase.auth.FirebaseUser signedIn = FirebaseAuth.getInstance().getCurrentUser();
		if (signedIn == null) {
			SketchwareUtil.showMessage(getContext(), "Session expired - please sign in again.");
			android.content.Intent back = new android.content.Intent(getActivity(), SplashScreenActivity.class);
			back.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
			startActivity(back);
			if (getActivity() != null) getActivity().finish();
			return;
		}
		String effectiveUid = signedIn.getUid();
		
		btn_submit_profile.setEnabled(false);
		btn_submit_profile.setText("Creating account…");
		_submitToFirebase(country, state, kinState, effectiveUid);
	}
	
	/** Moved directly from FinishSignupFragmentActivity's real write logic -
*  same userdb field names, so ProfileActivity, TrustScoreEngine, and
*  every member-detail sheet built earlier this session keep working
*  against this unchanged. */
	private void _submitToFirebase(String country, String state, String kinState, String effectiveUid) {
		Calendar authc = Calendar.getInstance();
		String cityStr = spinner_city.getSelectedItem() != null ? spinner_city.getSelectedItem().toString() : "";
		String kinCityStr = spinner_kin_city.getSelectedItem() != null ? spinner_kin_city.getSelectedItem().toString() : "";
		
		Map<String, Object> kinMap = new HashMap<>();
		kinMap.put("name", et_kin_name.getText().toString().trim());
		kinMap.put("phone", et_kin_phone.getText().toString().trim());
		kinMap.put("relationship", et_kin_relationship.getText().toString().trim());
		kinMap.put("address", et_kin_address.getText().toString().trim());
		kinMap.put("city", kinCityStr);
		kinMap.put("state", kinState);
		
		Map<String, Object> userVMap = new HashMap<>();
		userVMap.put("kin", kinMap);
		userVMap.put("gps_lat", latitudeString);
		userVMap.put("gps_long", longitudeString);
		userVMap.put("uid", effectiveUid);
		userVMap.put("dob", tv_dob.getText().toString().trim());
		userVMap.put("tier", "2");
		userVMap.put("city", cityStr);
		userVMap.put("state", state);
		userVMap.put("email", et_email.getText().toString().trim());
		userVMap.put("terms", "true");
		userVMap.put("phone", (et_phone_code.getText().toString() + et_phone_number.getText().toString()).trim());
		userVMap.put("v_type", idDocument);
		userVMap.put("icon", authsp.getString("photoUrl", ""));
		userVMap.put("privacy", "true");
		userVMap.put("country", country);
		userVMap.put("address", et_address.getText().toString().trim());
		userVMap.put("last_name", et_last_name.getText().toString().trim());
		userVMap.put("first_name", et_first_name.getText().toString().trim());
		userVMap.put("trust_score", "30");
		userVMap.put("occupation", et_occupation.getText().toString().trim());
		userVMap.put("timestamp", String.valueOf(authc.getTimeInMillis()));
		userVMap.put(idDocument, et_id_number.getText().toString().trim());
		
		UserProfileManager.getInstance(getContext()).saveProfile(effectiveUid, userVMap,
		new UserProfileManager.SimpleCallback() {
			@Override
			public void onSuccess() {
				if (getActivity() == null) return;
				getActivity().runOnUiThread(() -> {
					authsp.edit()
					.putString("privacy", "true")
					.putString("uid", effectiveUid)
					.commit();
					android.content.Intent intent = new android.content.Intent(getActivity(), MainActivity.class);
					intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
					startActivity(intent);
					getActivity().finish();
				});
			}
			
			@Override
			public void onError(String error) {
				if (getActivity() == null) return;
				getActivity().runOnUiThread(() -> {
					btn_submit_profile.setEnabled(true);
					btn_submit_profile.setText("Create Account");
					SketchwareUtil.showMessage(getContext(), "Failed to create account: " + error);
				});
			}
		});
	}
	
	/** Silent, best-effort - matches the original's non-blocking intent.
*  Only proceeds if location permission is already granted; doesn't
*  request it or hold up signup either way. gps_lat/gps_long aren't
*  read anywhere else in the app currently, so this stays supplementary,
*  not required. */
	private void _captureGpsLocationIfPermitted() {
		if (getContext() == null) return;
		if (ContextCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			return;
		}
		try {
			android.location.LocationManager locationManager =
			(android.location.LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
			if (locationManager == null) return;
			android.location.LocationListener locationListener = new android.location.LocationListener() {
				@Override
				public void onLocationChanged(android.location.Location location) {
					latitudeString = String.valueOf(location.getLatitude());
					longitudeString = String.valueOf(location.getLongitude());
				}
				@Override public void onStatusChanged(String provider, int status, Bundle extras) {}
				@Override public void onProviderEnabled(String provider) {}
				@Override public void onProviderDisabled(String provider) {}
			};
			if (locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)) {
				locationManager.requestLocationUpdates(android.location.LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
			}
		} catch (SecurityException ignored) {
			// Permission state changed between the check above and this call -
			// non-fatal, GPS fields just stay blank.
		}
	}
	
	public static class DatePickerFragment extends androidx.fragment.app.DialogFragment 
	implements android.app.DatePickerDialog.OnDateSetListener {
		
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			final Calendar c = Calendar.getInstance();
			int year = c.get(Calendar.YEAR);
			int month = c.get(Calendar.MONTH);
			int day = c.get(Calendar.DAY_OF_MONTH);
			
			return new android.app.DatePickerDialog(requireActivity(), this, year, month, day);
		}
		
		@Override
		public void onDateSet(android.widget.DatePicker view, int year, int month, int dayOfMonth) {
			if (getActivity() != null) {
				TextView tvDob = getActivity().findViewById(R.id.tv_dob);
				if (tvDob != null) {
					// Month is 0-indexed in Calendar
					String selectedDate = String.format(java.util.Locale.US, "%02d/%02d/%04d", dayOfMonth, month + 1, year);
					tvDob.setText(selectedDate);
				}
			}
		}
	}
	
	{
	}
	
}
