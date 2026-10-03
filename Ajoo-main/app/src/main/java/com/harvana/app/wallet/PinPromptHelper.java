package com.harvana.app.wallet;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import com.harvana.app.p2p.ThemedDialogs;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

/**
 * Shared PIN-entry prompt, used by any screen that needs to gate a real
 * action (sending funds, releasing P2P escrow, etc.) behind the
 * transaction PIN set up in SecurityPinSettingsActivity.
 *
 * If no PIN has ever been set, verifyPinThenProceed() lets the action
 * through unchallenged — this is an opt-in security feature, not a forced
 * one; someone who hasn't set a PIN yet shouldn't be locked out of sending
 * money because of it.
 */
public final class PinPromptHelper {

    private PinPromptHelper() {}

    public interface OnVerified {
        void onVerified();
    }

    /**
     * The single call most screens actually want: does nothing but invoke
     * onVerified immediately if no PIN is set. If a PIN is set AND
     * biometric unlock is enabled, tries fingerprint/face first (matching
     * SecurityPinSettingsActivity's own real BiometricPrompt integration —
     * previously this was PIN-only and ignored the biometric toggle
     * entirely, which meant enabling biometric there had no actual effect
     * on any real send flow). Falls back to the PIN dialog if biometric
     * fails, is cancelled, or isn't available on this specific call site's
     * Context. A wrong PIN or a cancelled dialog simply does not proceed —
     * no error thrown, the caller's action just doesn't happen.
     */
    public static void verifyPinThenProceed(Context context, OnVerified onVerified) {
        if (!SecureWalletStorage.hasTransactionPin(context)) {
            onVerified.onVerified();
            return;
        }

        if (SecureWalletStorage.isBiometricEnabled(context) && context instanceof FragmentActivity) {
            FragmentActivity activity = (FragmentActivity) context;
            BiometricManager biometricManager = BiometricManager.from(activity);
            boolean biometricReady = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                    == BiometricManager.BIOMETRIC_SUCCESS;
            if (biometricReady) {
                _tryBiometric(activity, context, onVerified);
                return;
            }
            // Enrolled/available state can change after the toggle was
            // turned on (e.g. all fingerprints removed in phone Settings) —
            // fall through to the PIN dialog instead of silently blocking
            // the action or crashing on an unusable prompt.
        }

        _showPinDialog(context, "Enter your transaction PIN", onVerified);
    }

    private static void _tryBiometric(FragmentActivity activity, Context context, OnVerified onVerified) {
        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
        .setTitle("Confirm it's you")
        .setSubtitle("Verify to continue")
        .setNegativeButtonText("Use PIN instead")
        .build();

        BiometricPrompt biometricPrompt = new BiometricPrompt(activity, ContextCompat.getMainExecutor(activity),
        new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                onVerified.onVerified();
            }

            @Override
            public void onAuthenticationError(int errorCode, CharSequence errString) {
                // Covers both "Use PIN instead" and a genuine biometric
                // failure/timeout — either way, fall back to the PIN
                // dialog rather than just silently doing nothing, since
                // the person still has a legitimate way to proceed.
                _showPinDialog(context, "Enter your transaction PIN", onVerified);
            }

            @Override
            public void onAuthenticationFailed() {
                // A single failed match doesn't cancel the prompt itself —
                // the system dialog stays open for another attempt.
            }
        });

        biometricPrompt.authenticate(promptInfo);
    }

    private static void _showPinDialog(Context context, String message, OnVerified onVerified) {
        View dialogView = LayoutInflater.from(context).inflate(
        context.getResources().getIdentifier("dialog_pin_entry", "layout", context.getPackageName()), null);
        TextView tvMessage = dialogView.findViewById(
        context.getResources().getIdentifier("tv_pin_dialog_message", "id", context.getPackageName()));
        EditText etInput = dialogView.findViewById(
        context.getResources().getIdentifier("et_pin_dialog_input", "id", context.getPackageName()));
        TextView tvError = dialogView.findViewById(
        context.getResources().getIdentifier("tv_pin_dialog_error", "id", context.getPackageName()));
        tvMessage.setText(message);

        AlertDialog dialog = ThemedDialogs.alert(context)
        .setView(dialogView)
        .setPositiveButton("Confirm", null)
        .setNegativeButton("Cancel", null)
        .create();

        dialog.setOnShowListener(d -> {
            ThemedDialogs.paintButtons(dialog);
            Button confirmButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            confirmButton.setOnClickListener(v -> {
                String entered = etInput.getText().toString().trim();
                long locked = SecureWalletStorage.pinLockRemainingMs(context);
                if (locked > 0) {
                    tvError.setText("Too many wrong tries. Try again in " + ((locked + 59_999) / 60_000) + " min.");
                    tvError.setVisibility(View.VISIBLE);
                    etInput.setText("");
                    return;
                }
                if (SecureWalletStorage.verifyTransactionPin(context, entered)) {
                    dialog.dismiss();
                    onVerified.onVerified();
                } else {
                    tvError.setText("Incorrect PIN. Please try again.");
                    tvError.setVisibility(View.VISIBLE);
                    etInput.setText("");
                }
            });
        });

        // Clears the inline error as soon as the person starts typing again,
        // rather than leaving a stale "Incorrect PIN" message visible while
        // they're mid-way through entering a fresh attempt.
        etInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { tvError.setVisibility(View.GONE); }
        });

        dialog.show();
    }
}
