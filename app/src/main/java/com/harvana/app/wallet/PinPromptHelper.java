package com.harvana.app.wallet;

import android.content.Context;
import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import com.harvana.app.SecurityPinSettingsActivity;
import com.harvana.app.p2p.ThemedDialogs;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

/**
 * Shared PIN-entry prompt.
 *
 * verifyPinThenProceed() is for screens that are not payments (seed reveal,
 * restore). If no PIN is set, that call still continues.
 *
 * authorize() is the payment gate. Stake, unstake, claim, send, contribution,
 * and every other payment must call it. Nothing is broadcast until a
 * transaction PIN exists and the person has just verified it. The action
 * text is what they are authorizing.
 */
public final class PinPromptHelper {

    private PinPromptHelper() {}

    public interface OnVerified {
        void onVerified();
    }

    /**
     * Opt-in check. If no PIN is set, onVerified runs immediately.
     * Do not use this for anything that moves money — use {@link #authorize}.
     */
    public static void verifyPinThenProceed(Context context, OnVerified onVerified) {
        if (context == null || onVerified == null) return;
        if (!SecureWalletStorage.hasTransactionPin(context)) {
            onVerified.onVerified();
            return;
        }
        _challenge(context, "Enter your transaction PIN", onVerified);
    }

    /**
     * Required gate for a payment. Does not call onVerified until biometric
     * or PIN verification succeeds. If no PIN is set, opens PIN settings
     * and does not continue.
     */
    public static void authorize(Context context, String action, OnVerified onVerified) {
        if (context == null || onVerified == null) return;
        String what = (action == null || action.trim().isEmpty())
                ? "Authorize this before it is sent."
                : action.trim();
        if (!SecureWalletStorage.hasTransactionPin(context)) {
            _requirePin(context, what);
            return;
        }
        _challenge(context, what, onVerified);
    }

    private static void _requirePin(Context context, String action) {
        ThemedDialogs.show(ThemedDialogs.alert(context)
                .setTitle("Verification required")
                .setMessage("Set a transaction PIN before this can go through.\n\n" + action)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Set PIN", (d, w) -> {
                    Intent i = new Intent(context, SecurityPinSettingsActivity.class);
                    if (!(context instanceof android.app.Activity)) {
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    }
                    context.startActivity(i);
                }));
    }

    private static void _challenge(Context context, String action, OnVerified onVerified) {
        if (SecureWalletStorage.isBiometricEnabled(context) && context instanceof FragmentActivity) {
            FragmentActivity activity = (FragmentActivity) context;
            BiometricManager biometricManager = BiometricManager.from(activity);
            boolean biometricReady = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                    == BiometricManager.BIOMETRIC_SUCCESS;
            if (biometricReady) {
                _tryBiometric(activity, context, action, onVerified);
                return;
            }
        }
        _showPinDialog(context, action, onVerified);
    }

    private static void _tryBiometric(FragmentActivity activity, Context context, String action, OnVerified onVerified) {
        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
        .setTitle("Confirm it's you")
        .setSubtitle(action)
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
                _showPinDialog(context, action, onVerified);
            }

            @Override
            public void onAuthenticationFailed() {
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

        etInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { tvError.setVisibility(View.GONE); }
        });

        dialog.show();
    }
}