package com.harvana.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.harvana.app.utils.ThemeManager;
import com.harvana.app.utils.UiColors;

import java.util.Map;

/**
 * The side menu that slides in from the right of the main screen. Built in code from one list of sections, so adding or
 * removing a row is one line. Colours come from UiColors, so it follows the light and dark themes.
 */
final class MainDrawer {
    interface Host {
        void openGroupsTab();
        void showVersion();
        void logOut();
    }

    private final Activity a;
    private final Host host;
    private final float d;
    private PopupWindow window;

    MainDrawer(Activity a, Host host) {
        this.a = a;
        this.host = host;
        this.d = a.getResources().getDisplayMetrics().density;
    }

    private int dp(float v) { return Math.round(v * d); }

    void show(View anchor) {
        LinearLayout list = new LinearLayout(a);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(0, dp(6), 0, dp(6));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(androidx.core.content.ContextCompat.getColor(a, R.color.colorPopupSurface));     // not the page colour, so the menu stands out
        bg.setCornerRadius(dp(16));
        bg.setStroke(dp(1), androidx.core.content.ContextCompat.getColor(a, R.color.colorCardStroke));
        list.setBackground(bg);

        row(list, R.drawable.icon_account_balance_wallet_twotone, "Crypto wallets", null, () -> open(ChainWalletsActivity.class));
        row(list, R.drawable.icon_group_add_twotone, "Friends", null, () -> open(com.harvana.app.social.FriendsActivity.class));
        row(list, R.drawable.icon_mark_unread_chat_alt_round, "Invitations", null, () -> open(InvitesActivity.class));
        row(list, R.drawable.icon_alternate_email_twotone, "My contact card", null, () -> open(com.harvana.app.social.ContactCardActivity.class));
        row(list, R.drawable.icon_settings_outline, "Settings", null, () -> open(SettingsActivity.class));
        row(list, R.drawable.ic_help, "Help & support", null, () -> open(HelpSupportActivity.class));
        row(list, R.drawable.icon_info_round, "About", null, () -> open(AboutActivity.class));
        row(list, R.drawable.icon_share_round, "Share Harvana", null, this::shareApp);
        LinearLayout out = rowView(R.drawable.icon_power_settings_new_twotone, "Log out", null, UiColors.error(), UiColors.error());
        out.setOnClickListener(v -> { dismiss(); confirmLogOut(); });
        list.addView(out);

        window = new PopupWindow(list, dp(232), ViewGroup.LayoutParams.WRAP_CONTENT, true);
        window.setOutsideTouchable(true);
        window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        window.setElevation(dp(12));
        if (anchor == null) anchor = a.findViewById(android.R.id.content);
        window.showAsDropDown(anchor, 0, dp(4), Gravity.END);
    }

    private void dismiss() { if (window != null) window.dismiss(); }

    private void open(Class<?> cls) {
        dismiss();
        try { a.startActivity(new Intent(a, cls)); }
        catch (Exception e) { android.widget.Toast.makeText(a, "Couldn't open that screen", android.widget.Toast.LENGTH_SHORT).show(); }
    }

    // ── pieces ─────────────────────────────────────────────────────────

    private View header() {
        LinearLayout h = new LinearLayout(a);
        h.setOrientation(LinearLayout.HORIZONTAL);
        h.setGravity(Gravity.CENTER_VERTICAL);
        h.setPadding(dp(20), dp(20), dp(20), dp(16));
        GradientDrawable g = new GradientDrawable();
        g.setColor(UiColors.background());
        h.setBackground(g);

        final com.harvana.app.ui.AvatarView avatar = new com.harvana.app.ui.AvatarView(a);
        avatar.setAvatarSize(com.harvana.app.ui.AvatarView.Size.HEADER);
        avatar.setPerson("");
        h.addView(avatar, new LinearLayout.LayoutParams(dp(32), dp(32)));

        LinearLayout text = new LinearLayout(a);
        text.setOrientation(LinearLayout.VERTICAL);
        text.setPadding(dp(14), 0, 0, 0);
        final TextView name = label("Your account", 17, true, UiColors.textPrimary());
        final TextView sub = label("", 13, false, UiColors.textSecondary());
        text.addView(name);
        text.addView(sub);
        h.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        com.google.firebase.auth.FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u != null) {
            sub.setText(u.getEmail() == null ? "" : u.getEmail());
            Map<String, Object> cached = null;
            try { cached = com.harvana.app.offline.OfflineCacheManager.load(a, "profile_" + u.getUid()); } catch (Exception ignored) { }
            if (cached != null) {
                String first = str(cached.get("first_name")), last = str(cached.get("last_name")), user = str(cached.get("username"));
                String full = (first + " " + last).trim();
                if (!full.isEmpty()) name.setText(full);
                avatar.setPerson(!full.isEmpty() ? full : user);
                if (!user.isEmpty()) sub.setText("@" + user);
                String icon = str(cached.get("icon"));
                if (!icon.isEmpty()) {
                    try { Glide.with(a.getApplicationContext()).load(Uri.parse(icon)).circleCrop().into(avatar); } catch (Exception ignored) { }
                }
            }
        }
        h.setOnClickListener(v -> {
            com.google.firebase.auth.FirebaseUser me = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
            if (me == null) { open(ProfileActivity.class); return; }
            a.startActivity(new android.content.Intent(a, com.harvana.app.social.PublicProfileActivity.class)
                    .putExtra(com.harvana.app.social.PublicProfileActivity.EXTRA_UID, me.getUid()));
        });
        return h;
    }

    private void section(LinearLayout list, String title) {
        TextView t = label(title.toUpperCase(java.util.Locale.US), 11, true, UiColors.textTertiary());
        t.setLetterSpacing(0.08f);
        t.setPadding(dp(20), dp(18), dp(20), dp(4));
        list.addView(t);
    }

    private void divider(LinearLayout list) {
        View v = new View(a);
        v.setBackgroundColor(UiColors.textTertiary() & 0x33FFFFFF | 0x22000000);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        lp.setMargins(dp(20), dp(12), dp(20), dp(4));
        list.addView(v, lp);
    }

    private void row(LinearLayout list, int icon, String title, String subtitle, Runnable action) {
        LinearLayout r = rowView(icon, title, subtitle, UiColors.textPrimary(), UiColors.accent());
        r.setOnClickListener(v -> action.run());
        list.addView(r);
    }

    private LinearLayout rowView(int icon, String title, String subtitle, int titleColor, int iconColor) {
        LinearLayout r = new LinearLayout(a);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(dp(16), dp(11), dp(16), dp(11));
        r.setMinimumHeight(dp(44));
        TypedValue tv = new TypedValue();
        a.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, tv, true);
        r.setBackgroundResource(tv.resourceId);

        ImageView i = new ImageView(a);
        i.setImageResource(icon);
        i.setColorFilter(iconColor);
        r.addView(i, new LinearLayout.LayoutParams(dp(22), dp(22)));

        LinearLayout t = new LinearLayout(a);
        t.setOrientation(LinearLayout.VERTICAL);
        t.setPadding(dp(16), 0, 0, 0);
        t.addView(label(title, 15, false, titleColor));
        if (subtitle != null && !subtitle.isEmpty()) t.addView(label(subtitle, 12, false, UiColors.textSecondary()));
        r.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return r;
    }

    private TextView label(String s, int sp, boolean bold, int color) {
        TextView t = new TextView(a);
        t.setText(s); t.setTextSize(sp); t.setTextColor(color);
        if (bold) t.setTypeface(null, Typeface.BOLD);
        return t;
    }

    // ── actions ────────────────────────────────────────────────────────

    private void pickTheme() {
        dismiss();
        String mode = ThemeManager.getMode(a);
        int checked = "light".equals(mode) ? 1 : "dark".equals(mode) ? 2 : 0;
        new com.harvana.app.ui.SheetDialog.Builder(a).setTitle("Appearance")
            .setSingleChoiceItems(new String[]{ThemeManager.systemLabel(a), "Light", "Dark"}, checked, (dlg, which) -> {
                if (which == 1) ThemeManager.setLightMode(a); else if (which == 2) ThemeManager.setDarkMode(a); else ThemeManager.setSystemMode(a);
                dlg.dismiss();
            }).setNegativeButton("Cancel", null).show();
    }

    private void shareApp() {
        dismiss();
        Intent share = new Intent(Intent.ACTION_SEND).setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, "Join me on Harvana: save together in groups and manage your " + CoinInfo.ticker() + " wallet.");
        a.startActivity(Intent.createChooser(share, "Share Harvana"));
    }

    private void confirmLogOut() {
        new com.harvana.app.ui.SheetDialog.Builder(a).setTitle("Log out?")
            .setMessage("You will need to sign in again. Your wallet stays safe on this phone as long as you keep your recovery phrase.")
            .setPositiveButton("Log out", (x, y) -> host.logOut())
            .setNegativeButton("Cancel", null).show();
    }

    private String versionName() {
        try { return a.getPackageManager().getPackageInfo(a.getPackageName(), 0).versionName; } catch (Exception e) { return "?"; }
    }

    private int statusBarHeight() {
        int id = a.getResources().getIdentifier("status_bar_height", "dimen", "android");
        return id > 0 ? a.getResources().getDimensionPixelSize(id) : dp(24);
    }

    private static String str(Object o) { return o == null ? "" : String.valueOf(o).trim(); }
}
