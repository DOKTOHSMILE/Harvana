package com.harvana.app;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.harvana.app.auth.EmailVerification;
import com.harvana.app.utils.UiColors;

/**
 * A signed-in account whose email is not confirmed lands here and goes no further. They can resend the confirmation email
 * (once a minute), check that they have confirmed, or sign out to use another account.
 */
public class VerifyEmailActivity extends AppCompatActivity {

    private TextView state;
    private Button resend, confirmed;
    private CountDownTimer cooldown;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { goTo(WelcomeActivity.class); return; }

        int pad = dp(24);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(pad, dp(72), pad, pad);
        root.setBackgroundColor(ContextCompat.getColor(this, R.color.colorBackground));

        TextView title = text("Confirm your email", 24f, UiColors.textPrimary());
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        root.addView(title);

        String email = user.getEmail() == null ? "your email" : user.getEmail();
        TextView body = text("We sent a confirmation link to " + email + ". Open it, then come back and tap the button below. "
                + "You cannot use Harvana until the email is confirmed. Check your spam folder if you do not see it.", 14f,
                ContextCompat.getColor(this, R.color.colorTextTertiary));
        body.setPadding(0, dp(12), 0, dp(24));
        root.addView(body);

        state = text("", 13f, ContextCompat.getColor(this, R.color.colorTextTertiary));
        state.setPadding(0, 0, 0, dp(12));
        root.addView(state);

        confirmed = button("I have confirmed my email", true);
        confirmed.setOnClickListener(v -> checkNow());
        root.addView(confirmed, buttonParams());

        resend = button("Resend confirmation email", false);
        resend.setOnClickListener(v -> resendEmail());
        root.addView(resend, buttonParams());

        TextView other = text("Use a different account", 14f, ContextCompat.getColor(this, R.color.colorAccent));
        other.setPadding(0, dp(20), 0, dp(8));
        other.setOnClickListener(v -> {
            com.harvana.app.auth.LogoutManager.logout(this);
            FirebaseAuth.getInstance().signOut();
            goTo(WelcomeActivity.class);
        });
        root.addView(other);
        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // The person may have confirmed in their mail app and switched back.
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) check(false);
    }

    @Override
    protected void onDestroy() {
        if (cooldown != null) cooldown.cancel();
        super.onDestroy();
    }

    private void checkNow() { check(true); }

    private void check(boolean userAsked) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { goTo(WelcomeActivity.class); return; }
        confirmed.setEnabled(false);
        user.reload().addOnCompleteListener(t -> {
            confirmed.setEnabled(true);
            FirebaseUser fresh = FirebaseAuth.getInstance().getCurrentUser();
            if (fresh != null && !EmailVerification.needsVerification(fresh)) {
                // Refresh the sign-in token too, so the server sees the confirmed email.
                fresh.getIdToken(true).addOnCompleteListener(x -> goTo(SplashScreenActivity.class));
            } else if (userAsked) {
                state.setText(t.isSuccessful()
                        ? "Not confirmed yet. Open the link in the email, then try again."
                        : "Could not check right now. Check your connection and try again.");
            }
        });
    }

    private void resendEmail() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        resend.setEnabled(false);
        user.sendEmailVerification().addOnCompleteListener(t -> {
            if (t.isSuccessful()) {
                state.setText("Sent. It can take a minute to arrive.");
                startCooldown();
            } else {
                resend.setEnabled(true);
                state.setText(t.getException() instanceof FirebaseTooManyRequestsException
                        ? "Too many requests. Wait a few minutes and try again."
                        : "Could not send the email. Check your connection and try again.");
            }
        });
    }

    private void startCooldown() {
        if (cooldown != null) cooldown.cancel();
        resend.setEnabled(false);
        cooldown = new CountDownTimer(EmailVerification.RESEND_COOLDOWN_SECONDS * 1000L, 1000L) {
            @Override public void onTick(long ms) { resend.setText("Resend in " + (ms / 1000 + 1) + "s"); }
            @Override public void onFinish() { resend.setText("Resend confirmation email"); resend.setEnabled(true); }
        }.start();
    }

    private void goTo(Class<?> screen) {
        Intent i = new Intent(this, screen);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    private TextView text(String s, float size, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);
        return t;
    }

    private Button button(String label, boolean primary) {
        Button bt = new Button(this);
        bt.setText(label);
        bt.setAllCaps(false);
        bt.setTextColor(primary ? Color.WHITE : ContextCompat.getColor(this, R.color.colorAccent));
        bt.setBackgroundColor(primary ? ContextCompat.getColor(this, R.color.colorAccent) : ContextCompat.getColor(this, R.color.colorSurfaceAlt));
        return bt;
    }

    private LinearLayout.LayoutParams buttonParams() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52));
        lp.topMargin = dp(10);
        return lp;
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
