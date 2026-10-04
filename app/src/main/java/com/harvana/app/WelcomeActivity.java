package com.harvana.app;

import android.animation.*;
import android.app.*;
import android.app.Activity;
import android.content.*;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.*;
import android.os.*;
import android.util.TypedValue;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class WelcomeActivity extends AppCompatActivity {

    public final int REQ_CD_GAUTH = 101;

    private Timer _timer = new Timer();
    private FirebaseDatabase _firebase = FirebaseDatabase.getInstance();

    private MaterialButton btnGetStarted;
    private MaterialButton btnLogin;
    private LinearLayout linear10;
    private TextView textview6;
    private ProgressBar progressbar1;

    private Intent i = new Intent();
    private SharedPreferences authsp;
    private DatabaseReference dbauth = _firebase.getReference("dbauth");
    private ChildEventListener _dbauth_child_listener;
    private FirebaseAuth fbauth;
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
        btnGetStarted = findViewById(R.id.btn_get_started);
        btnLogin = findViewById(R.id.btn_login);
        linear10 = findViewById(R.id.linear10);
        textview6 = findViewById(R.id.textview6);
        progressbar1 = findViewById(R.id.progressbar1);
        authsp = getSharedPreferences("authsp", Activity.MODE_PRIVATE);
        fbauth = FirebaseAuth.getInstance();

        btnGetStarted.setOnClickListener(_v -> {
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

        btnLogin.setOnClickListener(_v -> showLoginBottomSheet());

        _dbauth_child_listener = new ChildEventListener() {
            @Override public void onChildAdded(DataSnapshot _param1, String _param2) {}
            @Override public void onChildChanged(DataSnapshot _param1, String _param2) {}
            @Override public void onChildMoved(DataSnapshot _param1, String _param2) {}
            @Override public void onChildRemoved(DataSnapshot _param1) {}
            @Override public void onCancelled(DatabaseError _param1) {}
        };
        dbauth.addChildEventListener(_dbauth_child_listener);

        _userdb_child_listener = new ChildEventListener() {
            @Override public void onChildAdded(DataSnapshot _param1, String _param2) {}
            @Override public void onChildChanged(DataSnapshot _param1, String _param2) {}
            @Override public void onChildMoved(DataSnapshot _param1, String _param2) {}
            @Override public void onChildRemoved(DataSnapshot _param1) {}
            @Override public void onCancelled(DatabaseError _param1) {}
        };
        userdb.addChildEventListener(_userdb_child_listener);
    }

    private void initializeLogic() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("92873857858-jgtl0u04g8m5fgdqkoc041n50a7tit96.apps.googleusercontent.com")
            .requestEmail()
            .build();

        gauth = GoogleSignIn.getClient(this, gso);

        linear10.setVisibility(View.GONE);

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
            final Window window = WelcomeActivity.this.getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.parseColor("#080A11"));
        }
    }

    private void showLoginBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this, com.google.android.material.R.style.Theme_Design_BottomSheetDialog);
        
        LinearLayout sheetLayout = new LinearLayout(this);
        sheetLayout.setOrientation(LinearLayout.VERTICAL);
        sheetLayout.setPadding(dp(24), dp(32), dp(24), dp(32));
        sheetLayout.setBackgroundColor(Color.parseColor("#121623")); // Dark surface

        TextView title = new TextView(this);
        title.setText("Welcome Back");
        title.setTextColor(Color.WHITE);
        title.setTextSize(22);
        title.setTypeface(null, Typeface.BOLD);
        title.setPadding(0, 0, 0, dp(8));
        
        TextView subtitle = new TextView(this);
        subtitle.setText("Select a method to log into your account.");
        subtitle.setTextColor(Color.parseColor("#8F9BB3"));
        subtitle.setTextSize(14);
        subtitle.setPadding(0, 0, 0, dp(24));

        MaterialButton btnGoogle = new MaterialButton(this);
        btnGoogle.setText("Continue with Google");
        btnGoogle.setAllCaps(false);
        btnGoogle.setTextColor(Color.BLACK);
        btnGoogle.setBackgroundColor(Color.WHITE);
        btnGoogle.setCornerRadius(dp(12));
        btnGoogle.setMinimumHeight(dp(56));
        // You can add a Google icon drawable here if you have one in your resources:
        // btnGoogle.setIconResource(R.drawable.ic_google); 
        btnGoogle.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            linear10.setVisibility(View.VISIBLE);
            Intent signInIntent = gauth.getSignInIntent();
            startActivityForResult(signInIntent, REQ_CD_GAUTH);
        });

        MaterialButton btnEmail = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(12), 0, 0);
        btnEmail.setLayoutParams(params);
        btnEmail.setText("Sign in with Email");
        btnEmail.setAllCaps(false);
        btnEmail.setTextColor(Color.WHITE);
        btnEmail.setStrokeColorResource(android.R.color.darker_gray);
        btnEmail.setCornerRadius(dp(12));
        btnEmail.setMinimumHeight(dp(56));
        btnEmail.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            i.putExtra("page", "eLogin");
            i.setClass(getApplicationContext(), LoginActivity.class);
            startActivity(i);
        });

        sheetLayout.addView(title);
        sheetLayout.addView(subtitle);
        sheetLayout.addView(btnGoogle);
        sheetLayout.addView(btnEmail);

        bottomSheetDialog.setContentView(sheetLayout);
        bottomSheetDialog.show();
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
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
                                    if (user == null) {
                                        linear10.setVisibility(View.GONE);
                                        return;
                                    }

                                    boolean isNewUser = task.getResult().getAdditionalUserInfo() != null
                                        && task.getResult().getAdditionalUserInfo().isNewUser();
                                    if (isNewUser) {
                                        authsp.edit().putString("first_name", firstName).commit();
                                        authsp.edit().putString("last_name", lastName).commit();
                                        _routeToProfileForm(user);
                                    } else {
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
                                                    linear10.setVisibility(View.GONE);
                                                }

                                                @Override
                                                public void onError(String error) {
                                                    if (com.harvana.app.offline.NetworkStatus.isConnectivityError(error)) {
                                                        Map<String, Object> cached = com.harvana.app.offline.OfflineCacheManager.load(
                                                            getApplicationContext(), "profile_" + user.getUid());
                                                        if (cached != null && !cached.isEmpty()) {
                                                            i.setClass(getApplicationContext(), MainActivity.class);
                                                            startActivity(i);
                                                            linear10.setVisibility(View.GONE);
                                                            return;
                                                        }
                                                    }
                                                    authsp.edit().putString("first_name", firstName).commit();
                                                    authsp.edit().putString("last_name", lastName).commit();
                                                    _routeToProfileForm(user);
                                                    linear10.setVisibility(View.GONE);
                                                }
                                            });
                                    }
                                } else {
                                    linear10.setVisibility(View.GONE);
                                    Exception e = task.getException();
                                    String errorMsg = (e != null) ? e.getMessage() : "Authentication failed";
                                    Toast.makeText(getApplicationContext(), "Auth Error: " + errorMsg, Toast.LENGTH_LONG).show();
                                }
                            }
                        });
                    } catch (Exception e) {
                        linear10.setVisibility(View.GONE);
                        Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    linear10.setVisibility(View.GONE);
                    try {
                        GoogleSignIn.getSignedInAccountFromIntent(_data).getResult(ApiException.class);
                    } catch (ApiException ae) {
                        int code = ae.getStatusCode();
                        if (code == 10) {
                            Toast.makeText(this, "Google sign-in isn't set up for this build yet (SHA-1 not registered in Firebase). Use email for now.", Toast.LENGTH_LONG).show();
                        } else if (code == 7) {
                            Toast.makeText(this, "No internet connection.", Toast.LENGTH_LONG).show();
                        } else if (code != 12501) { 
                            Toast.makeText(this, "Google sign-in failed (code " + code + ").", Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception ignored) { }
                }
                break;
            default:
                break;
        }
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
        finish();
    }
}
