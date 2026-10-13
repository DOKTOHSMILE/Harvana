package com.harvana.app.group;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.harvana.app.R;
import com.harvana.app.trust.ProfileTrust;

/**
 * Shows a member's profile photo in the round initials badge of a member row. The badge starts as initials; when the person has
 * a photo it fills the circle (the initials stay in the text but are made clear so a name that arrives later cannot draw over it).
 */
public final class MemberPhoto {
    private MemberPhoto() {}

    /** For a member the network knows only by wallet: the photo of the person who owns it. */
    public static void applyWallet(final TextView badge, final String wallet) {
        if (badge == null) return;
        badge.setBackgroundResource(R.drawable.bg_avatar);
        badge.setTextColor(Color.WHITE);
        badge.setTag(R.id.member_photo_uid, "w:" + wallet);
        if (wallet == null || wallet.isEmpty()) return;
        com.harvana.app.trust.WalletProfiles.resolve(wallet, card -> {
            if (!("w:" + wallet).equals(badge.getTag(R.id.member_photo_uid))) return;
            if (card == null) return;
            if (!card.name.isEmpty()) badge.setText(card.name.substring(0, 1).toUpperCase(java.util.Locale.US));
            if (!card.uid.isEmpty()) { apply(badge, card.uid); return; }      // the account's own photo, newest first
            if (card.icon.isEmpty()) return;
            try {
                Glide.with(badge.getContext().getApplicationContext()).load(card.icon).circleCrop().into(new CustomTarget<Drawable>() {
                    @Override public void onResourceReady(@NonNull Drawable d, @Nullable Transition<? super Drawable> t) {
                        if (!("w:" + wallet).equals(badge.getTag(R.id.member_photo_uid))) return;
                        badge.setBackground(d);
                        badge.setTextColor(Color.TRANSPARENT);
                    }
                    @Override public void onLoadCleared(@Nullable Drawable placeholder) { }
                });
            } catch (Exception ignored) { }
        });
    }

    public static void apply(final TextView badge, final String uid) {
        if (badge == null) return;
        badge.setBackgroundResource(R.drawable.bg_avatar);
        badge.setTextColor(Color.WHITE);
        badge.setTag(R.id.member_photo_uid, uid);
        if (uid == null || uid.isEmpty()) return;
        ProfileTrust.resolve(uid, card -> {
            if (!uid.equals(badge.getTag(R.id.member_photo_uid))) return;          // the row was reused for someone else
            if (card == null || card.icon == null || card.icon.isEmpty()) return;
            try {
                Glide.with(badge.getContext().getApplicationContext()).load(card.icon).circleCrop().into(new CustomTarget<Drawable>() {
                    @Override public void onResourceReady(@NonNull Drawable d, @Nullable Transition<? super Drawable> t) {
                        if (!uid.equals(badge.getTag(R.id.member_photo_uid))) return;
                        badge.setBackground(d);
                        badge.setTextColor(Color.TRANSPARENT);
                    }
                    @Override public void onLoadCleared(@Nullable Drawable placeholder) { }
                });
            } catch (Exception ignored) { }
        });
    }
}
