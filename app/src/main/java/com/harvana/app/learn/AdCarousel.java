package com.harvana.app.learn;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.harvana.app.utils.UiColors;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The advert carousel at the top of the Learn Hub. Adverts are documents in Firestore LEARN_ADS, added by the owner
 * in the Firebase console (the app can only read them, and count a view or a tap). See docs/UPDATES.md (11 Oct)
 * for the fields. Slides change every 5 seconds, stop while a finger is on them, and the whole strip hides when no
 * advert is running. The last list is kept on the phone so the strip appears at once.
 */
public final class AdCarousel {

    public static final String COLLECTION = "LEARN_ADS";
    private static final long SLIDE_MS = 5000;

    public static final class Ad {
        public String id = "", title = "", subtitle = "", imageUrl = "", link = "", cta = "", sponsor = "", color = "";
        public long order;
    }

    private final Activity host;
    private final LinearLayout root;
    private final ViewPager2 pager;
    private final LinearLayout dots;
    private final Adapter adapter;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<String> counted = new ArrayList<>();
    private boolean running;

    private final Runnable advance = new Runnable() {
        @Override public void run() {
            if (!running) return;
            int n = adapter.getItemCount();
            if (n > 1) pager.setCurrentItem((pager.getCurrentItem() + 1) % n, true);
            handler.postDelayed(this, SLIDE_MS);
        }
    };

    /** Builds the strip and adds it to {@code parent} at {@code index}. Hidden until an advert loads. */
    public AdCarousel(Activity host, ViewGroup parent, int index) {
        this.host = host;
        root = new LinearLayout(host);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setVisibility(View.GONE);
        pager = new ViewPager2(host);
        adapter = new Adapter();
        pager.setAdapter(adapter);
        pager.setOffscreenPageLimit(1);
        // A little of the next slide shows at the edge, so people see it swipes.
        View inner = pager.getChildAt(0);
        if (inner instanceof RecyclerView) {
            inner.setPadding(dp(16), 0, dp(28), 0);
            ((RecyclerView) inner).setClipToPadding(false);
        }
        pager.setPageTransformer((page, pos) -> page.setTranslationX(-dp(6) * pos));
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(160));
        plp.topMargin = dp(14);
        root.addView(pager, plp);
        dots = new LinearLayout(host);
        dots.setGravity(Gravity.CENTER);
        dots.setPadding(0, dp(8), 0, 0);
        root.addView(dots);
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override public void onPageSelected(int position) { paintDots(position); countView(position); }
            @Override public void onPageScrollStateChanged(int state) {
                // A finger on the strip holds it still; it carries on 5 seconds after letting go.
                handler.removeCallbacks(advance);
                if (state == ViewPager2.SCROLL_STATE_IDLE && running) handler.postDelayed(advance, SLIDE_MS);
            }
        });
        parent.addView(root, Math.max(0, Math.min(index, parent.getChildCount())),
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        show(fromCache());
        load();
    }

    public void start() {
        running = true;
        handler.removeCallbacks(advance);
        handler.postDelayed(advance, SLIDE_MS);
    }

    public void stop() {
        running = false;
        handler.removeCallbacks(advance);
    }

    /** Adverts that are switched on and inside their dates, in their set order. */
    public void load() {
        FirebaseFirestore.getInstance().collection(COLLECTION).whereEqualTo("active", true).limit(10).get()
                .addOnSuccessListener(q -> {
                    long now = System.currentTimeMillis();
                    List<Ad> ads = new ArrayList<>();
                    for (DocumentSnapshot d : q.getDocuments()) {
                        Ad a = new Ad();
                        a.id = d.getId();
                        a.title = str(d.get("title"));
                        a.subtitle = str(d.get("subtitle"));
                        a.imageUrl = str(d.get("imageUrl"));
                        a.link = str(d.get("link"));
                        a.cta = str(d.get("cta"));
                        a.sponsor = str(d.get("sponsor"));
                        a.color = str(d.get("color"));
                        a.order = num(d.get("order"));
                        long starts = num(d.get("startsAt")), ends = num(d.get("endsAt"));
                        if (starts > 0 && now < starts) continue;
                        if (ends > 0 && now > ends) continue;
                        if (a.title.isEmpty() && a.imageUrl.isEmpty()) continue;
                        if (!a.imageUrl.isEmpty() && !a.imageUrl.startsWith("https://")) a.imageUrl = "";   // https pictures only
                        ads.add(a);
                    }
                    Collections.sort(ads, (x, y) -> Long.compare(x.order, y.order));
                    toCache(ads);
                    show(ads);
                });
    }

    private void show(List<Ad> ads) {
        if (host.isFinishing()) return;
        adapter.set(ads);
        root.setVisibility(ads.isEmpty() ? View.GONE : View.VISIBLE);
        dots.removeAllViews();
        if (ads.size() > 1) for (int i = 0; i < ads.size(); i++) {
            View d = new View(host);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(6), dp(6));
            lp.setMargins(dp(3), 0, dp(3), 0);
            dots.addView(d, lp);
        }
        paintDots(pager.getCurrentItem());
    }

    private void paintDots(int sel) {
        for (int i = 0; i < dots.getChildCount(); i++) {
            View d = dots.getChildAt(i);
            GradientDrawable g = new GradientDrawable();
            g.setCornerRadius(dp(3));
            g.setColor(i == sel ? UiColors.accent() : (UiColors.textTertiary() & 0x00FFFFFF) | 0x55000000);
            d.setBackground(g);
            ViewGroup.LayoutParams lp = d.getLayoutParams();
            lp.width = dp(i == sel ? 16 : 6);
            d.setLayoutParams(lp);
        }
    }

    /** One view per advert per screen visit, so the owner can see how often each is seen. */
    private void countView(int position) {
        Ad a = adapter.at(position);
        if (a == null || counted.contains(a.id)) return;
        counted.add(a.id);
        bump(a.id, "views");
    }

    private static void bump(String id, String field) {
        if (com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() == null) return;
        FirebaseFirestore.getInstance().collection(COLLECTION).document(id).update(field, FieldValue.increment(1));
    }

    /** Opens an advert: an https page in the browser, or a screen in the app ("app:p2p", "app:staking"…). */
    private void open(Ad a) {
        bump(a.id, "clicks");
        String link = a.link == null ? "" : a.link.trim();
        if (link.isEmpty()) return;
        Intent i = null;
        switch (link) {
            case "app:p2p": i = new Intent(host, com.harvana.app.P2pMarketplaceActivity.class); break;
            case "app:staking": i = new Intent(host, com.harvana.app.StakingDashboardActivity.class); break;
            case "app:my-trades": i = new Intent(host, com.harvana.app.P2pMyTradesActivity.class); break;
            case "app:payment-methods": i = new Intent(host, com.harvana.app.PaymentMethodsActivity.class); break;
            case "app:notifications": i = new Intent(host, com.harvana.app.NotificationsActivity.class); break;
            default:
                if (link.startsWith("https://")) i = new Intent(Intent.ACTION_VIEW, Uri.parse(link));   // never http, file or script links
        }
        if (i == null) return;
        try { host.startActivity(i); } catch (Exception ignored) { }
    }

    private final class Adapter extends RecyclerView.Adapter<Adapter.Holder> {
        private final List<Ad> items = new ArrayList<>();

        void set(List<Ad> ads) { items.clear(); items.addAll(ads); notifyDataSetChanged(); }
        Ad at(int i) { return i >= 0 && i < items.size() ? items.get(i) : null; }

        @Override public Holder onCreateViewHolder(ViewGroup parent, int type) {
            MaterialCardView card = new MaterialCardView(host);
            card.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            card.setRadius(dp(18));
            card.setCardElevation(0);
            card.setUseCompatPadding(false);
            FrameLayout frame = new FrameLayout(host);
            ImageView image = new ImageView(host);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            frame.addView(image, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            View shade = new View(host);
            shade.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{0xCC000000, 0x66000000, 0x00000000}));
            frame.addView(shade, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            LinearLayout text = new LinearLayout(host);
            text.setOrientation(LinearLayout.VERTICAL);
            text.setGravity(Gravity.CENTER_VERTICAL);
            text.setPadding(dp(18), dp(14), dp(18), dp(14));
            TextView badge = new TextView(host);
            badge.setTextSize(10);
            badge.setTypeface(null, Typeface.BOLD);
            badge.setLetterSpacing(0.08f);
            badge.setTextColor(Color.WHITE);
            GradientDrawable bb = new GradientDrawable();
            bb.setCornerRadius(dp(8));
            bb.setColor(0x55FFFFFF);
            badge.setBackground(bb);
            badge.setPadding(dp(8), dp(2), dp(8), dp(2));
            TextView title = new TextView(host);
            title.setTextSize(18);
            title.setTypeface(null, Typeface.BOLD);
            title.setTextColor(Color.WHITE);
            title.setMaxLines(2);
            title.setPadding(0, dp(8), dp(40), 0);
            TextView sub = new TextView(host);
            sub.setTextSize(13);
            sub.setTextColor(0xE6FFFFFF);
            sub.setMaxLines(2);
            sub.setPadding(0, dp(4), dp(40), 0);
            TextView cta = new TextView(host);
            cta.setTextSize(13);
            cta.setTypeface(null, Typeface.BOLD);
            cta.setTextColor(Color.BLACK);
            GradientDrawable cb = new GradientDrawable();
            cb.setCornerRadius(dp(16));
            cb.setColor(Color.WHITE);
            cta.setBackground(cb);
            cta.setPadding(dp(14), dp(6), dp(14), dp(6));
            LinearLayout.LayoutParams wrap = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            text.addView(badge, wrap);
            text.addView(title);
            text.addView(sub);
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            clp.topMargin = dp(10);
            text.addView(cta, clp);
            frame.addView(text, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            card.addView(frame);
            return new Holder(card, image, badge, title, sub, cta);
        }

        @Override public void onBindViewHolder(Holder h, int pos) {
            Ad a = items.get(pos);
            int base;
            try { base = a.color.isEmpty() ? UiColors.accent() : Color.parseColor(a.color); } catch (Exception e) { base = UiColors.accent(); }
            h.card.setCardBackgroundColor(base);
            if (a.imageUrl.isEmpty()) h.image.setImageDrawable(null);
            else Glide.with(host).load(a.imageUrl).centerCrop().into(h.image);
            h.badge.setText(a.sponsor.isEmpty() ? "SPONSORED" : "SPONSORED · " + a.sponsor.toUpperCase(java.util.Locale.US));
            h.title.setText(a.title);
            h.title.setVisibility(a.title.isEmpty() ? View.GONE : View.VISIBLE);
            h.sub.setText(a.subtitle);
            h.sub.setVisibility(a.subtitle.isEmpty() ? View.GONE : View.VISIBLE);
            boolean hasLink = !a.link.isEmpty();
            h.cta.setText(a.cta.isEmpty() ? "Learn more" : a.cta);
            h.cta.setVisibility(hasLink ? View.VISIBLE : View.GONE);
            h.card.setOnClickListener(hasLink ? v -> open(a) : null);
        }

        @Override public int getItemCount() { return items.size(); }

        final class Holder extends RecyclerView.ViewHolder {
            final MaterialCardView card; final ImageView image; final TextView badge, title, sub, cta;
            Holder(MaterialCardView card, ImageView image, TextView badge, TextView title, TextView sub, TextView cta) {
                super(card); this.card = card; this.image = image; this.badge = badge; this.title = title; this.sub = sub; this.cta = cta;
            }
        }
    }

    // ── Kept on the phone so the strip shows at once next time ──

    private SharedPreferences prefs() { return host.getSharedPreferences("learn_ads", Context.MODE_PRIVATE); }

    private void toCache(List<Ad> ads) {
        try {
            JSONArray arr = new JSONArray();
            for (Ad a : ads) arr.put(new JSONObject().put("id", a.id).put("title", a.title).put("subtitle", a.subtitle)
                    .put("imageUrl", a.imageUrl).put("link", a.link).put("cta", a.cta).put("sponsor", a.sponsor).put("color", a.color));
            prefs().edit().putString("ads", arr.toString()).apply();
        } catch (Exception ignored) { }
    }

    private List<Ad> fromCache() {
        List<Ad> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(prefs().getString("ads", "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                Ad a = new Ad();
                a.id = o.optString("id"); a.title = o.optString("title"); a.subtitle = o.optString("subtitle");
                a.imageUrl = o.optString("imageUrl"); a.link = o.optString("link"); a.cta = o.optString("cta");
                a.sponsor = o.optString("sponsor"); a.color = o.optString("color");
                out.add(a);
            }
        } catch (Exception ignored) { }
        return out;
    }

    private static String str(Object o) { return o == null ? "" : String.valueOf(o).trim(); }
    private static long num(Object o) { return o instanceof Number ? ((Number) o).longValue() : 0; }
    private int dp(int v) { return Math.round(v * host.getResources().getDisplayMetrics().density); }
}
