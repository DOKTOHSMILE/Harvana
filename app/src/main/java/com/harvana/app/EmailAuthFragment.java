package com.harvana.app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
		
		View root = inflater.inflate(R.layout.fragment_email_auth, container, false);
		((TextView) root.findViewById(R.id.tv_email_auth_title)).setText(createMode ? "Create your account" : "Sign in");
		tilEmail = root.findViewById(R.id.til_email_auth_email);
		etEmail = root.findViewById(R.id.et_email_auth_email);
		tilPassword = root.findViewById(R.id.til_email_auth_password);
		etPassword = root.findViewById(R.id.et_email_auth_password);
		if (createMode) {
			tilConfirm = root.findViewById(R.id.til_email_auth_confirm);
			etConfirm = root.findViewById(R.id.et_email_auth_confirm);
		} else {
			root.findViewById(R.id.til_email_auth_confirm).setVisibility(View.GONE);
		}
		btnSubmit = root.findViewById(R.id.btn_email_auth_submit);
		btnSubmit.setText(createMode ? "Create account" : "Sign in");
		btnSubmit.setOnClickListener(v -> submit());
		View forgot = root.findViewById(R.id.tv_email_auth_forgot);
		forgot.setVisibility(createMode ? View.GONE : View.VISIBLE);
		forgot.setOnClickListener(v -> resetPassword());
		TextView switchMode = root.findViewById(R.id.tv_email_auth_switch);
		switchMode.setText(createMode ? "Already have an account? Sign in" : "New here? Create an account");
		switchMode.setOnClickListener(v -> requireActivity().getSupportFragmentManager().beginTransaction()
			.replace(R.id.linear2, newInstance(!createMode)).commit());
		return root;
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
					saveEmail(email);
					openVerifyScreen();
				} else {
					setBusy(false);
					showError(task.getException());
				}
			});
		} else {
			auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
				if (!isAdded()) return;
				if (task.isSuccessful()) {
					FirebaseUser u = auth.getCurrentUser();
					if (u == null) { setBusy(false); return; }
					// An unconfirmed email does not get into the app: refresh the account first (they may have
					// confirmed in the browser), then either go on or send them to confirm.
					u.reload().addOnCompleteListener(r -> {
						if (!isAdded()) return;
						FirebaseUser fresh = auth.getCurrentUser();
						if (fresh != null && com.harvana.app.auth.EmailVerification.needsVerification(fresh)) {
							saveEmail(email);
							openVerifyScreen();
						} else {
							proceed(email);
						}
					});
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
			tilEmail.setError("Enter your email address here first, then tap Forgot password");
			etEmail.requestFocus();
			return;
		}
		setBusy(true);
		auth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
			if (!isAdded()) return;
			setBusy(false);
			if (task.isSuccessful()) {
				new com.harvana.app.ui.SheetDialog.Builder(requireContext())
					.setTitle("Check your email")
					.setMessage("If an account exists for " + email + ", we sent a link to choose a new password. "
						+ "It can take a few minutes. Check your spam folder too.")
					.setPositiveButton("OK", null)
					.show();
			} else if (task.getException() instanceof FirebaseAuthInvalidUserException) {
				// Firebase can answer this way when it hides whether an address is registered; say the same either way.
				toast("If an account exists for that email, a reset link has been sent.");
			} else {
				showError(task.getException());
			}
		});
	}
	
	private void saveEmail(String email) {
		requireContext().getSharedPreferences("authsp", android.content.Context.MODE_PRIVATE)
			.edit().putString("gmail", email).apply();
	}
	
	/** An account with an unconfirmed email goes to the confirm screen, not into the app. */
	private void openVerifyScreen() {
		Intent i = new Intent(requireContext(), VerifyEmailActivity.class);
		i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		startActivity(i);
		requireActivity().finish();
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
	
}
