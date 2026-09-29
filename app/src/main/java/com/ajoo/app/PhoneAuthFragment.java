package com.ajoo.app;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Gravity;
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
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

/**
 * Phone number sign-in with a real SMS OTP via Firebase Auth.
 * Step 1: enter phone in international format (+234...). Step 2: enter the 6-digit code.
 * Supports instant/auto verification, resend (with cooldown), and changing the number.
 * On success hands off to SplashScreenActivity, which routes existing users to MainActivity
 * and new users (no profile yet) to the complete-profile form.
 */
public class PhoneAuthFragment extends Fragment {
	
	private static final long RESEND_SECONDS = 60;
	
	private FirebaseAuth auth;
	private String verificationId;
	private PhoneAuthProvider.ForceResendingToken resendToken;
	private String phone = "";
	private CountDownTimer timer;
	
	private LinearLayout root;
	private TextInputLayout tilPhone, tilCode;
	private TextInputEditText etPhone, etCode;
	private Button btnAction;
	private TextView tvResend, tvChange, tvSub;
	private boolean codeStep = false;
	private boolean autoVerified = false;
	
	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
		auth = FirebaseAuth.getInstance();
		int pad = dp(20);
		ScrollView scroll = new ScrollView(requireContext());
		scroll.setFillViewport(true);
		root = new LinearLayout(requireContext());
		root.setOrientation(LinearLayout.VERTICAL);
		root.setPadding(pad, pad, pad, pad);
		scroll.addView(root, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		
		TextView title = new TextView(requireContext());
		title.setText("Sign in with phone");
		title.setTextSize(24f);
		title.setTextColor(Color.parseColor("#222222"));
		title.setPadding(0, dp(8), 0, dp(4));
		root.addView(title);
		
		tvSub = new TextView(requireContext());
		tvSub.setPadding(0, 0, 0, dp(20));
		root.addView(tvSub);
		
		tilPhone = makeField("Phone number (e.g. +2348012345678)");
		etPhone = (TextInputEditText) tilPhone.getEditText();
		etPhone.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
		root.addView(tilPhone);
		
		tilCode = makeField("6-digit code");
		etCode = (TextInputEditText) tilCode.getEditText();
		etCode.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
		etCode.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(6)});
		root.addView(tilCode);
		
		btnAction = new Button(requireContext());
		btnAction.setAllCaps(false);
		btnAction.setTextColor(Color.WHITE);
		btnAction.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colorAccent));
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52));
		lp.topMargin = dp(16);
		root.addView(btnAction, lp);
		btnAction.setOnClickListener(v -> { if (codeStep) verifyCode(); else sendCode(false); });
		
		tvResend = link("");
		tvResend.setOnClickListener(v -> { if (tvResend.isEnabled()) sendCode(true); });
		root.addView(tvResend);
		
		tvChange = link("Use a different number");
		tvChange.setOnClickListener(v -> showStep(false));
		root.addView(tvChange);
		
		TextView email = link("Use email instead");
		email.setOnClickListener(v -> requireActivity().getSupportFragmentManager().beginTransaction()
			.replace(R.id.linear2, EmailAuthFragment.newInstance(false)).commit());
		root.addView(email);
		
		showStep(false);
		return scroll;
	}
	
	private void showStep(boolean code) {
		codeStep = code;
		tilPhone.setVisibility(code ? View.GONE : View.VISIBLE);
		tilCode.setVisibility(code ? View.VISIBLE : View.GONE);
		tvResend.setVisibility(code ? View.VISIBLE : View.GONE);
		tvChange.setVisibility(code ? View.VISIBLE : View.GONE);
		btnAction.setText(code ? "Verify" : "Send code");
		tvSub.setText(code ? "Enter the code we sent to " + phone : "We'll text you a verification code.");
		if (!code && timer != null) timer.cancel();
		setBusy(false);
	}
	
	private void sendCode(boolean resend) {
		tilPhone.setError(null);
		if (!resend) {
			String p = etPhone.getText() != null ? etPhone.getText().toString().replaceAll("[\\s\\-()]", "") : "";
			if (!p.matches("^\\+[1-9]\\d{7,14}$")) {
				tilPhone.setError("Use international format, e.g. +2348012345678");
				return;
			}
			phone = p;
		}
		setBusy(true);
		PhoneAuthOptions.Builder b = PhoneAuthOptions.newBuilder(auth)
			.setPhoneNumber(phone)
			.setTimeout(RESEND_SECONDS, TimeUnit.SECONDS)
			.setActivity(requireActivity())
			.setCallbacks(callbacks);
		if (resend && resendToken != null) b.setForceResendingToken(resendToken);
		PhoneAuthProvider.verifyPhoneNumber(b.build());
	}
	
	private final PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks =
		new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
			@Override
			public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
				// Instant verification / SMS auto-retrieval: no code entry needed.
				if (!isAdded()) return;
				autoVerified = true;
				signIn(credential);
			}
			
			@Override
			public void onVerificationFailed(@NonNull FirebaseException e) {
				if (!isAdded()) return;
				setBusy(false);
				String msg;
				if (e instanceof FirebaseAuthInvalidCredentialsException) msg = "That phone number is not valid.";
				else if (e instanceof FirebaseTooManyRequestsException) msg = "Too many attempts. Please try again later.";
				else if (e instanceof FirebaseNetworkException) msg = "No internet connection.";
				else msg = e.getMessage() != null ? e.getMessage() : "Could not send the code.";
				toast(msg);
			}
			
			@Override
			public void onCodeSent(@NonNull String id, @NonNull PhoneAuthProvider.ForceResendingToken token) {
				if (!isAdded()) return;
				verificationId = id;
				resendToken = token;
				showStep(true);
				startCooldown();
				toast("Code sent");
			}
			
			@Override
			public void onCodeAutoRetrievalTimeOut(@NonNull String id) {
				verificationId = id;
			}
		};
	
	private void verifyCode() {
		tilCode.setError(null);
		String code = etCode.getText() != null ? etCode.getText().toString().trim() : "";
		if (code.length() != 6) {
			tilCode.setError("Enter the 6-digit code");
			return;
		}
		if (verificationId == null) {
			toast("Please request a new code.");
			showStep(false);
			return;
		}
		signIn(PhoneAuthProvider.getCredential(verificationId, code));
	}
	
	private void signIn(PhoneAuthCredential credential) {
		setBusy(true);
		auth.signInWithCredential(credential).addOnCompleteListener(task -> {
			if (!isAdded()) return;
			if (task.isSuccessful()) {
				proceed();
			} else {
				setBusy(false);
				Exception e = task.getException();
				if (e instanceof FirebaseAuthInvalidCredentialsException) {
					if (codeStep) tilCode.setError("Incorrect or expired code");
					else toast("Verification failed.");
				} else {
					toast(e != null && e.getMessage() != null ? e.getMessage() : "Sign-in failed.");
				}
			}
		});
	}
	
	private void proceed() {
		if (timer != null) timer.cancel();
		requireContext().getSharedPreferences("authsp", Context.MODE_PRIVATE)
			.edit().putString("phone_auth", phone).apply();
		Intent i = new Intent(requireContext(), SplashScreenActivity.class);
		i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		startActivity(i);
		requireActivity().finish();
	}
	
	private void startCooldown() {
		if (timer != null) timer.cancel();
		tvResend.setEnabled(false);
		timer = new CountDownTimer(RESEND_SECONDS * 1000, 1000) {
			@Override public void onTick(long ms) {
				tvResend.setText("Resend code in " + (ms / 1000) + "s");
			}
			@Override public void onFinish() {
				tvResend.setEnabled(true);
				tvResend.setText("Resend code");
			}
		}.start();
	}
	
	@Override
	public void onDestroyView() {
		if (timer != null) timer.cancel();
		super.onDestroyView();
	}
	
	private TextInputLayout makeField(String hint) {
		TextInputLayout til = new TextInputLayout(requireContext(), null,
			com.google.android.material.R.attr.textInputOutlinedStyle);
		til.setHint(hint);
		TextInputEditText et = new TextInputEditText(til.getContext());
		et.setSingleLine(true);
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
		t.setGravity(Gravity.CENTER);
		return t;
	}
	
	private void setBusy(boolean busy) {
		btnAction.setEnabled(!busy);
		btnAction.setAlpha(busy ? 0.6f : 1f);
	}
	
	private void toast(String m) {
		if (getContext() != null) Toast.makeText(getContext(), m, Toast.LENGTH_LONG).show();
	}
	
	private int dp(int v) {
		return Math.round(v * getResources().getDisplayMetrics().density);
	}
}
