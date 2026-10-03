package com.harvana.app;

import android.animation.*;
import android.app.*;
import android.app.Activity;
import android.content.*;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.media.*;
import android.net.*;
import android.net.Uri;
import android.os.*;
import android.text.*;
import android.text.style.*;
import android.util.*;
import android.view.*;
import android.view.View;
import android.view.View.*;
import android.view.animation.*;
import android.widget.*;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.*;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.budiyev.android.codescanner.*;
import com.bumptech.glide.*;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
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
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.*;
import org.json.*;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


public class WelcomeActivity extends AppCompatActivity {
	
	public final int REQ_CD_GAUTH = 101;
	
	private Timer _timer = new Timer();
	private FirebaseDatabase _firebase = FirebaseDatabase.getInstance();
	
	private static final int CAMERA_PERMISSION_CODE = 100;
	
	private ArrayList<HashMap<String, Object>> user_map_data = new ArrayList<>();
	
	private CardView cardview7;
	private LinearLayout linear1;
	private LinearLayout linear10;
	private LinearLayout linear7;
	private LinearLayout linear6;
	private CardView cardview6;
	private TextView textview1;
	private TextView textview2;
	private LinearLayout linear2;
	private ImageView imageview1;
	private LinearLayout linear3;
	private LinearLayout linear4;
	private LinearLayout linear5;
	private ImageView imageview2;
	private TextView textview3;
	private ImageView imageview3;
	private TextView textview4;
	private ImageView imageview4;
	private TextView textview5;
	private CardView cardview5;
	private CardView cardview3;
	private LinearLayout linear9;
	private CardView cardview2;
	private CardView cardview4;
	private SignInButton signinbutton1;
	private Button button3;
	private Button button1;
	private Button button2;
	private TextView textview6;
	private ProgressBar progressbar1;
	
	private Intent i = new Intent();
	private TimerTask t;
	private SharedPreferences authsp;
	private DatabaseReference dbauth = _firebase.getReference("dbauth");
	private ChildEventListener _dbauth_child_listener;
	private FirebaseAuth fbauth;
	private OnCompleteListener<AuthResult> _fbauth_create_user_listener;
	private OnCompleteListener<AuthResult> _fbauth_sign_in_listener;
	private OnCompleteListener<Void> _fbauth_reset_password_listener;
	private OnCompleteListener<Void> fbauth_updateEmailListener;
	private OnCompleteListener<Void> fbauth_updatePasswordListener;
	private OnCompleteListener<Void> fbauth_emailVerificationSentListener;
	private OnCompleteListener<Void> fbauth_deleteUserListener;
	private OnCompleteListener<Void> fbauth_updateProfileListener;
	private OnCompleteListener<AuthResult> fbauth_phoneAuthListener;
	private OnCompleteListener<AuthResult> fbauth_googleSignInListener;
	private GoogleSignInClient gauth;
	private DatabaseReference userdb = _firebase.getReference("userdb");
	private ChildEventListener _userdb_child_listener;
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.welcome);
		initialize(_savedInstanceState);
		FirebaseApp.initializeApp(this);
		initializeLogic();
	}
	
	private void initialize(Bundle _savedInstanceState) {
		cardview7 = findViewById(R.id.cardview7);
		linear1 = findViewById(R.id.linear1);
		linear10 = findViewById(R.id.linear10);
		linear7 = findViewById(R.id.linear7);
		linear6 = findViewById(R.id.linear6);
		cardview6 = findViewById(R.id.cardview6);
		textview1 = findViewById(R.id.textview1);
		textview2 = findViewById(R.id.textview2);
		linear2 = findViewById(R.id.linear2);
		imageview1 = findViewById(R.id.imageview1);
		linear3 = findViewById(R.id.linear3);
		linear4 = findViewById(R.id.linear4);
		linear5 = findViewById(R.id.linear5);
		imageview2 = findViewById(R.id.imageview2);
		textview3 = findViewById(R.id.textview3);
		imageview3 = findViewById(R.id.imageview3);
		textview4 = findViewById(R.id.textview4);
		imageview4 = findViewById(R.id.imageview4);
		textview5 = findViewById(R.id.textview5);
		cardview5 = findViewById(R.id.cardview5);
		cardview3 = findViewById(R.id.cardview3);
		linear9 = findViewById(R.id.linear9);
		cardview2 = findViewById(R.id.cardview2);
		cardview4 = findViewById(R.id.cardview4);
		signinbutton1 = findViewById(R.id.signinbutton1);
		button3 = findViewById(R.id.button3);
		button1 = findViewById(R.id.button1);
		button2 = findViewById(R.id.button2);
		textview6 = findViewById(R.id.textview6);
		progressbar1 = findViewById(R.id.progressbar1);
		authsp = getSharedPreferences("authsp", Activity.MODE_PRIVATE);
		fbauth = FirebaseAuth.getInstance();
		
		signinbutton1.setOnClickListener(_v -> {
			Intent signInIntent = gauth.getSignInIntent();
			startActivityForResult(signInIntent, REQ_CD_GAUTH);
		});
		
		
		button1.setOnClickListener(_v -> {
			i.putExtra("page", "eLogin");
			i.setClass(getApplicationContext(), LoginActivity.class);
			startActivity(i);
		});
		
		button2.setOnClickListener(_v -> {
			new android.app.AlertDialog.Builder(this)
				.setTitle("Before you start")
				.setMessage("Harvana keeps your name, phone and wallet address so a circle can trust you. Contributions, loans and tips are in HARV, and every transfer shows the network fee before you confirm. Mining is optional and can heat the phone. The current admin of a circle sees member payment cards. Everyone else sees only their own.\n\nBy continuing you accept that.")
				.setPositiveButton("I agree", (d, w) -> {
					i.putExtra("page", "eCreate");
					i.setClass(getApplicationContext(), LoginActivity.class);
					startActivity(i);
				})
				.setNegativeButton("Not now", null)
				.show();
		});
		
		_dbauth_child_listener = new ChildEventListener() {
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
		dbauth.addChildEventListener(_dbauth_child_listener);
		
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
		
		fbauth_updateEmailListener = _param1 -> {
			final boolean _success = _param1.isSuccessful();
			final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
			
		};
		
		fbauth_updatePasswordListener = _param1 -> {
			final boolean _success = _param1.isSuccessful();
			final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
			
		};
		
		fbauth_emailVerificationSentListener = _param1 -> {
			final boolean _success = _param1.isSuccessful();
			final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
			
		};
		
		fbauth_deleteUserListener = _param1 -> {
			final boolean _success = _param1.isSuccessful();
			final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
			
		};
		
		fbauth_phoneAuthListener = task -> {
			final boolean _success = task.isSuccessful();
			final String _errorMessage = task.getException() != null ? task.getException().getMessage() : "";
			
		};
		
		fbauth_updateProfileListener = _param1 -> {
			final boolean _success = _param1.isSuccessful();
			final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
			
		};
		
		fbauth_googleSignInListener = task -> {
			final boolean _success = task.isSuccessful();
			final String _errorMessage = task.getException() != null ? task.getException().getMessage() : "";
			
		};
		
		_fbauth_create_user_listener = _param1 -> {
			final boolean _success = _param1.isSuccessful();
			final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
			
		};
		
		_fbauth_sign_in_listener = _param1 -> {
			final boolean _success = _param1.isSuccessful();
			final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
			
		};
		
		_fbauth_reset_password_listener = _param1 -> {
			final boolean _success = _param1.isSuccessful();
			
		};
	}
	
	private void initializeLogic() {
		GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
		.requestIdToken("92873857858-jgtl0u04g8m5fgdqkoc041n50a7tit96.apps.googleusercontent.com")
		.requestEmail()
		.build();
		
		gauth = GoogleSignIn.getClient(this, gso);
		
		// This screen only offers the sign-in choices. The logo screen before it decides who lands here.
		linear6.setVisibility(View.VISIBLE);
		progressbar1.setVisibility(View.GONE);
		if (textview6 != null) textview6.setText("");
		
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
			final Window window = WelcomeActivity.this.getWindow();
			window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
			window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
			window.setStatusBarColor(getResources().getColor(R.color.colorBackground));
		}
		
	}
	
	@Override
	protected void onActivityResult(int _requestCode, int _resultCode, Intent _data) {
		super.onActivityResult(_requestCode, _resultCode, _data);
		
		switch (_requestCode) {
			case REQ_CD_GAUTH:
			if (_resultCode == Activity.RESULT_OK) {
				Task<GoogleSignInAccount> _task = GoogleSignIn.getSignedInAccountFromIntent(_data);
				
				try {
					final GoogleSignInAccount account = _task.getResult(ApiException.class);
					
					final String firstName = (account.getGivenName() != null) ? account.getGivenName() : "";
					final String lastName = (account.getFamilyName() != null) ? account.getFamilyName() : "";
					
					AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
					fbauth.signInWithCredential(credential).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
						@Override
						public void onComplete(@NonNull Task<AuthResult> task) {
							if (task.isSuccessful()) {
								final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
								if (user == null) return;
								
								boolean isNewUser = task.getResult().getAdditionalUserInfo() != null
								&& task.getResult().getAdditionalUserInfo().isNewUser();
								if (isNewUser) {
									authsp.edit().putString("first_name", firstName).commit();
									authsp.edit().putString("last_name", lastName).commit();
									_routeToProfileForm(user);
								} else {
									// Same fix as the already-signed-in check above:
									// direct, scoped Firestore lookup instead of
									// scanning the entire userdb tree.
									com.harvana.app.firestore.UserProfileManager.getInstance(getApplicationContext())
									.getProfile(user.getUid(), new com.harvana.app.firestore.UserProfileManager.DocCallback() {
										@Override
										public void onSuccess(Map<String, Object> data) {
											boolean userExists = data != null && !data.isEmpty();
											if (userExists) {
												com.harvana.app.offline.OfflineCacheManager.save(
												getApplicationContext(), "profile_" + user.getUid(), data);
												i.setClass(getApplicationContext(), MainActivity.class);
												startActivity(i);
											} else {
												authsp.edit().putString("first_name", firstName).commit();
												authsp.edit().putString("last_name", lastName).commit();
												_routeToProfileForm(user);
											}
										}
										
										@Override
										public void onError(String error) {
											// Same fix as proceedAfterPermissions(): distinguish a
											// real connectivity failure from a genuine "no profile"
											// before deciding to route to account creation.
											if (com.harvana.app.offline.NetworkStatus.isConnectivityError(error)) {
												Map<String, Object> cached = com.harvana.app.offline.OfflineCacheManager.load(
												getApplicationContext(), "profile_" + user.getUid());
												if (cached != null && !cached.isEmpty()) {
													i.setClass(getApplicationContext(), MainActivity.class);
													startActivity(i);
													return;
												}
											}
											authsp.edit().putString("first_name", firstName).commit();
											authsp.edit().putString("last_name", lastName).commit();
											_routeToProfileForm(user);
										}
									});
								}
							} else {
								Exception e = task.getException();
								String errorMsg = (e != null) ? e.getMessage() : "Authentication failed";
								Toast.makeText(getApplicationContext(), "Auth Error: " + errorMsg, Toast.LENGTH_LONG).show();
							}
							
						}
					});
				} catch (Exception e) {
					SketchwareUtil.showMessage(getApplicationContext(), e.toString());
				}
				
			}
			else {
				// RESULT_CANCELED covers both "user backed out" and real failures. A failure
				// (e.g. DEVELOPER_ERROR / code 10 when this build's SHA-1 isn't registered in
				// Firebase) returns no account and makes no network call, so it looked like
				// the button did nothing. Surface the actual status code.
				try {
					GoogleSignIn.getSignedInAccountFromIntent(_data).getResult(ApiException.class);
				} catch (ApiException ae) {
					int code = ae.getStatusCode();
					if (code == 10) {
						Toast.makeText(this, "Google sign-in isn't set up for this build yet (SHA-1 not registered in Firebase). Use email for now.", Toast.LENGTH_LONG).show();
					} else if (code == 7) {
						Toast.makeText(this, "No internet connection.", Toast.LENGTH_LONG).show();
					} else if (code != 12501) { // 12501 = user cancelled
						Toast.makeText(this, "Google sign-in failed (code " + code + ").", Toast.LENGTH_LONG).show();
					}
				} catch (Exception ignored) { }
			}
			break;
			default:
			break;
		}
	}
	
	public void _injection() {
	}
	private void _routeToProfileForm(FirebaseUser user) {
		String photoUrl = (user.getPhotoUrl() != null) ? user.getPhotoUrl().toString() : "";
		String email = (user.getEmail() != null) ? user.getEmail() : "";
		
		authsp.edit()
		.putString("photoUrl", photoUrl)
		.putString("gmail", email)
		.commit();
		
		Intent i = new Intent();
		i.setClass(getApplicationContext(), LoginActivity.class);
		i.putExtra("page", "complete");
		startActivity(i);
		finish(); // ADD THIS
	}
	
	{
	}
	
}
