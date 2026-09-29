package com.harvanna.wallet.utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.android.material.card.MaterialCardView;
import com.harvanna.wallet.R;

public class InAppNotificationHelper {

    public static void showTransactionNotification(final Activity activity, final String title, final String message, final String sender) {
        if (activity == null || activity.isFinishing()) return;

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    final ViewGroup contentRoot = activity.findViewById(android.R.id.content);
                    if (contentRoot == null) return;

                    final MaterialCardView cardView = new MaterialCardView(activity);
                    FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );
                    params.gravity = Gravity.TOP;
                    params.setMargins(32, 120, 32, 0); 
                    cardView.setLayoutParams(params);
                    cardView.setRadius(16f);
                    cardView.setCardElevation(12f);
                    cardView.setCardBackgroundColor(Color.parseColor("#0f1623"));

                    // Add 1dp solid border/stroke
                    int strokeWidthPx = (int) (1 * activity.getResources().getDisplayMetrics().density);
                    cardView.setStrokeWidth(strokeWidthPx);
                    cardView.setStrokeColor(Color.parseColor("#10b881"));

                    LinearLayout layout = new LinearLayout(activity);
                    layout.setOrientation(LinearLayout.HORIZONTAL);
                    layout.setPadding(24, 24, 24, 24);
                    layout.setGravity(Gravity.CENTER_VERTICAL);

                    ImageView icon = new ImageView(activity);
                    LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(72, 72);
                    icon.setLayoutParams(iconParams);
                    icon.setImageResource(R.drawable.icon_call_received_twotone);
                    icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

                    LinearLayout textLayout = new LinearLayout(activity);
                    textLayout.setOrientation(LinearLayout.VERTICAL);
                    textLayout.setPadding(24, 0, 0, 0);

                    TextView tvTitle = new TextView(activity);
                    String displaySender = (sender != null && !sender.isEmpty()) ? sender : "Unknown";
                    tvTitle.setText(title + " from: " + displaySender);
                    tvTitle.setTextSize(14f);
                    tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
                    tvTitle.setTextColor(Color.parseColor("#ffffff"));
                    tvTitle.setSingleLine(true);
                    tvTitle.setEllipsize(TextUtils.TruncateAt.END);

                    TextView tvMessage = new TextView(activity);
                    tvMessage.setText(message);
                    tvMessage.setTextSize(13f);
                    tvMessage.setTextColor(Color.WHITE);

                    textLayout.addView(tvTitle);
                    textLayout.addView(tvMessage);

                    layout.addView(icon);
                    layout.addView(textLayout);
                    cardView.addView(layout);

                    cardView.setTranslationY(-200f);
                    cardView.setAlpha(0f);

                    contentRoot.addView(cardView);

                    com.harvanna.wallet.utils.SoundUtils.playSound(activity, R.raw.drop_coin);

                    cardView.animate()
                            .translationY(0f)
                            .alpha(1f)
                            .setDuration(300)
                            .start();

                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                cardView.animate()
                                        .translationY(-200f)
                                        .alpha(0f)
                                        .setDuration(300)
                                        .setListener(new AnimatorListenerAdapter() {
                                            @Override
                                            public void onAnimationEnd(Animator animation) {
                                                try {
                                                    contentRoot.removeView(cardView);
                                                } catch (Exception ignored) {}
                                            }
                                        })
                                        .start();
                            } catch (Exception ignored) {}
                        }
                    }, 4000);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}