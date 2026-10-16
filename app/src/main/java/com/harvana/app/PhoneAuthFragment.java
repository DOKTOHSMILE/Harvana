package com.harvana.app;

import android.content.Intent;
import android.os.Bundle;
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
 * Phone sign-in through Firebase Auth. Firebase sends the SMS itself, so this does not wait on a Cloud Function.
 */
public class PhoneAuthFragment extends Fragment {

	private FirebaseAuth auth;
	private TextInputLayout tilNumber, tilCode;
	private TextInputEditText etNumber, etCode;
	private Button submit;
	private String verificationId;
	private PhoneAuthProvider.ForceResendingToken resendToken;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
		auth = FirebaseAuth.getInstance();
		View root = inflater.inflate(R.layout.fragment_phone_auth, container, false);
		tilNumber = root.findViewById(R.id.til_phone_auth_number);
		etNumber = root.findViewById(R.id.et_phone_auth_number);
		tilCode = root.findViewById(R.id.til_phone_auth_code);
		etCode = root.findViewById(R.id.et_phone_auth_code);
		submit = root.findViewById(R.id.btn_phone_auth_submit);
		etNumber.setText("+234");
		submit.setOnClickListener(v -> {
			if (verificationId == null) sendCode(false);
			else confirmCode();
		});
		TextView email = root.findViewById(R.id.tv_phone_auth_email);
		email.setOnClickListener(v -> requireActivity().getSupportFragmentManager().beginTransaction()
			.replace(R.id.linear2, EmailAuthFragment.newInstance(false)).commit());
		return root;
	}

	private void sendCode(boolean force) {
		tilNumber.setError(null);
		String phone = text(etNumber).replace(" ", "").replace("-", "");
		if (!phone.startsWith("+") || phone.length() < 8 || phone.length() > 16) {
			tilNumber.setError("Use the full number, starting with + and the country code");
			return;
		}
		setBusy(true);
		PhoneAuthOptions.Builder b = PhoneAuthOptions.newBuilder(auth)
			.setPhoneNumber(phone)
			.setTimeout(60L, TimeUnit.SECONDS)
			.setActivity(requireActivity())
			.setCallbacks(callbacks);
		if (force && resendToken != null) b.setForceResendingToken(resendToken);
		PhoneAuthProvider.verifyPhoneNumber(b.build());
	}

	private final PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks =
		new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
			@Override
			public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
				signIn(credential);
			}

			@Override
			public void onVerificationFailed(@NonNull FirebaseException e) {
				if (!isAdded()) return;
				setBusy(false);
				String msg;
				if (e instanceof FirebaseAuthInvalidCredentialsException) msg = "That phone number is not valid.";
				else if (e instanceof FirebaseTooManyRequestsException) msg = "Too many texts. Wait and try again.";
				else if (e instanceof FirebaseNetworkException) msg = "No internet connection.";
				else msg = e.getMessage() != null ? e.getMessage() : "Could not send the code.";
				toast(msg);
			}

			@Override
			public void onCodeSent(@NonNull String id, @NonNull PhoneAuthProvider.ForceResendingToken token) {
				if (!isAdded()) return;
				verificationId = id;
				resendToken = token;
				setBusy(false);
				tilCode.setVisibility(View.VISIBLE);
				submit.setText("Confirm code");
				((TextView) requireView().findViewById(R.id.tv_phone_auth_hint))
					.setText("Enter the code from the text. It can take a minute.");
			}
		};

	private void confirmCode() {
		tilCode.setError(null);
		String code = text(etCode);
		if (verificationId == null) { sendCode(false); return; }
		if (code.length() < 4) {
			tilCode.setError("Enter the code from the text");
			return;
		}
		setBusy(true);
		signIn(PhoneAuthProvider.getCredential(verificationId, code));
	}

	private void signIn(PhoneAuthCredential credential) {
		auth.signInWithCredential(credential).addOnCompleteListener(task -> {
			if (!isAdded()) return;
			if (!task.isSuccessful() || auth.getCurrentUser() == null) {
				setBusy(false);
				toast(task.getException() != null && task.getException().getMessage() != null
					? task.getException().getMessage() : "That code did not work.");
				return;
			}
			String phone = auth.getCurrentUser().getPhoneNumber();
			if (phone == null) phone = text(etNumber);
			requireContext().getSharedPreferences("authsp", android.content.Context.MODE_PRIVATE)
				.edit().putString("phone", phone).apply();
			Intent i = new Intent(requireContext(), SplashScreenActivity.class);
			i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
			startActivity(i);
			requireActivity().finish();
		});
	}

	private void setBusy(boolean busy) {
		submit.setEnabled(!busy);
		submit.setText(busy ? "Please wait" : (verificationId == null ? "Send code" : "Confirm code"));
	}

	private static String text(TextInputEditText e) {
		return e.getText() == null ? "" : e.getText().toString().trim();
	}

	private void toast(String msg) {
		if (isAdded()) Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
	}
}
