package com.harvana.app;

import com.harvana.app.utils.UiColors;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;

/**
 * Email + password authentication via Firebase Auth.
 * Modes: "eLogin" (sign in) and "eCreate" (register). Both support password reset.
 * On success the user is handed to SplashScreenActivity, which already routes to
 * MainActivity (profile exists) or the profile form (no profile yet).
 */
public class EmailAuthFragment extends Fragment {
	
	private static final String ARG_CREATE = "create";
	private static final int MIN_PASSWORD = 8;
	
	private boolean createMode;
	private FirebaseAuth auth;
	private TextInputLayout tilEmail, tilPassword, tilConfirm;
	private TextInputEditText etEmail, etPassword, etConfirm;
	private Button btnSubmit;
	
	public static EmailAuthFragment newInstance(boolean create) {
		EmailAuthFragment f = new EmailAuthFragment();
		Bundle b = new Bundle();
		b.putBoolean(ARG_CREATE, create);
		f.setArguments(b);
		return f;
	}
	
	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
		createMode = getArguments() != null && getArguments().getBoolean(ARG_CREATE);
		auth = FirebaseAuth.getInstance();
		
		int pad = dp(20);
		ScrollView scroll = new ScrollView(requireContext());
		scroll.setFillViewport(true);
		LinearLayout root = new LinearLayout(requireContext());
		root.setOrientation(LinearLayout.VERTICAL);
		root.setPadding(pad, pad, pad, pad);
		scroll.addView(root, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		
		TextView title = new TextView(requireContext());
		title.setText(createMode ? "Create your account" : "Welcome back");
		title.setTextSize(24f);
		title.setTextColor(UiColors.textPrimary());
		title.setPadding(0, dp(8), 0, dp(4));
		root.addView(title);
		
		TextView sub = new TextView(requireContext());
		sub.setText(createMode ? "Sign up with your email and a password." : "Sign in with your email and password.");
		sub.setPadding(0, 0, 0, dp(20));
		root.addView(sub);
		
		tilEmail = makeField("Email", false);
		etEmail = (TextInputEditText) tilEmail.getEditText();
		etEmail.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
		root.addView(tilEmail);
		
		tilPassword = makeField("Password", true);
		etPassword = (TextInputEditText) tilPassword.getEditText();
		root.addView(tilPassword);
		
		if (createMode) {
			tilConfirm = makeField("Confirm password", true);
			etConfirm = (TextInputEditText) tilConfirm.getEditText();
			root.addView(tilConfirm);
		}
		
		btnSubmit = new Button(requireContext());
		btnSubmit.setText(createMode ? "Create account" : "Sign in");
		btnSubmit.setAllCaps(false);
		btnSubmit.setTextColor(Color.WHITE);
		btnSubmit.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colorAccent));
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52));
		lp.topMargin = dp(16);
		root.addView(btnSubmit, lp);
		btnSubmit.setOnClickListener(v -> submit());
		
		if (!createMode) {
			TextView forgot = link("Forgot password?");
			forgot.setOnClickListener(v -> resetPassword());
			root.addView(forgot);
		}
		
		TextView switchMode = link(createMode ? "Already have an account? Sign in" : "New here? Create an account");
		switchMode.setOnClickListener(v -> requireActivity().getSupportFragmentManager().beginTransaction()
			.replace(R.id.linear2, newInstance(!createMode)).commit());
		root.addView(switchMode);
		
		return scroll;
	}
	
	private void submit() {
		clearErrors();
		String email = text(etEmail);
		String password = etPassword.getText() != null ? etPassword.getText().toString() : "";
		
		if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
			tilEmail.setError("Enter a valid email address");
			return;
		}
		if (password.isEmpty()) {
			tilPassword.setError("Enter your password");
			return;
		}
		if (createMode) {
			if (password.length() < MIN_PASSWORD) {
				tilPassword.setError("Use at least " + MIN_PASSWORD + " characters");
				return;
			}
			String confirm = etConfirm.getText() != null ? etConfirm.getText().toString() : "";
			if (!password.equals(confirm)) {
				tilConfirm.setError("Passwords do not match");
				return;
			}
		}
		
		setBusy(true);
		if (createMode) {
			auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
				if (!isAdded()) return;
				if (task.isSuccessful()) {
					FirebaseUser u = auth.getCurrentUser();
					if (u != null) u.sendEmailVerification();
					proceed(email);
				} else {
					setBusy(false);
					showError(task.getException());
				}
			});
		} else {
			auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
				if (!isAdded()) return;
				if (task.isSuccessful()) {
					proceed(email);
				} else {
					setBusy(false);
					showError(task.getException());
				}
			});
		}
	}
	
	private void resetPassword() {
		clearErrors();
		String email = text(etEmail);
		if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
			tilEmail.setError("Enter your email above, then tap Forgot password");
			return;
		}
		setBusy(true);
		auth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
			if (!isAdded()) return;
			setBusy(false);
			if (task.isSuccessful()) {
				toast("If an account exists for that email, a reset link has been sent.");
			} else {
				showError(task.getException());
			}
		});
	}
	
	/** Hand off to the splash router, which sends the user to Main or the profile form. */
	private void proceed(String email) {
		requireContext().getSharedPreferences("authsp", android.content.Context.MODE_PRIVATE)
			.edit().putString("gmail", email).apply();
		Intent i = new Intent(requireContext(), SplashScreenActivity.class);
		i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		startActivity(i);
		requireActivity().finish();
	}
	
	private void showError(Exception e) {
		String msg;
		if (e instanceof FirebaseAuthWeakPasswordException) {
			tilPassword.setError("Password is too weak");
			return;
		} else if (e instanceof FirebaseAuthUserCollisionException) {
			tilEmail.setError("An account with this email already exists");
			return;
		} else if (e instanceof FirebaseAuthInvalidUserException) {
			msg = "No account found for this email, or it has been disabled.";
		} else if (e instanceof FirebaseAuthInvalidCredentialsException) {
			msg = "Incorrect email or password.";
		} else if (e instanceof FirebaseTooManyRequestsException) {
			msg = "Too many attempts. Please wait a moment and try again.";
		} else if (e instanceof FirebaseNetworkException) {
			msg = "No internet connection.";
		} else if (e instanceof FirebaseAuthException) {
			msg = "Authentication failed (" + ((FirebaseAuthException) e).getErrorCode() + ").";
		} else {
			msg = e != null && e.getMessage() != null ? e.getMessage() : "Authentication failed.";
		}
		toast(msg);
	}
	
	private TextInputLayout makeField(String hint, boolean password) {
		TextInputLayout til = new TextInputLayout(requireContext(), null,
			com.google.android.material.R.attr.textInputOutlinedStyle);
		til.setHint(hint);
		if (password) til.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
		TextInputEditText et = new TextInputEditText(til.getContext());
		et.setSingleLine(true);
		if (password) {
			et.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
		}
		til.addView(et, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		lp.topMargin = dp(8);
		til.setLayoutParams(lp);
		return til;
	}
	
	private TextView link(String label) {
		TextView t = new TextView(requireContext());
		t.setText(label);
		t.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorAccent));
		t.setPadding(0, dp(16), 0, dp(8));
		t.setGravity(android.view.Gravity.CENTER);
		return t;
	}
	
	private void clearErrors() {
		tilEmail.setError(null);
		tilPassword.setError(null);
		if (tilConfirm != null) tilConfirm.setError(null);
	}
	
	private void setBusy(boolean busy) {
		btnSubmit.setEnabled(!busy);
		btnSubmit.setAlpha(busy ? 0.6f : 1f);
	}
	
	private String text(TextInputEditText et) {
		return et.getText() != null ? et.getText().toString().trim() : "";
	}
	
	private void toast(String m) {
		if (getContext() != null) Toast.makeText(getContext(), m, Toast.LENGTH_LONG).show();
	}
	
	private int dp(int v) {
		return Math.round(v * getResources().getDisplayMetrics().density);
	}
}
