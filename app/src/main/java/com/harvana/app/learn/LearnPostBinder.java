package com.harvana.app.learn;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.harvana.app.R;
import com.harvana.app.trust.ProfileTrust;

import java.util.Map;

/**
 * Fills one Learn Hub post, in the feed card and in the full reader (they share view ids). It shows the author with their
 * trust score, the engagement counts (fires, comments, views), and the actions: fire, comment, share, gift, more.
 * Every count comes from Firestore documents; none is a number the app can write.
 */
public final class LearnPostBinder {
    private LearnPostBinder() {}

    /** The post as plain values, so it can be passed to the reader in an Intent. */
    public static final class Post {
        public String id = "", title = "", body = "", category = "", imageUrl = "", authorUid = "", videoUrl = "";
        public long createdAt, views;

        public static Post from(Map<String, Object> m) {
            Post p = new Post();
            p.id = str(m.get("_docId"));
            p.title = str(m.get("title"));
            p.body = str(m.get("description"));
            p.category = str(m.get("category"));
            p.imageUrl = str(m.get("imageUrl"));
            p.authorUid = str(m.get("authorUid"));
            p.videoUrl = str(m.get("videoUrl"));
            p.createdAt = m.get("createdAt") instanceof Number ? ((Number) m.get("createdAt")).longValue() : 0L;
            p.views = m.get("views") instanceof Number ? ((Number) m.get("views")).longValue() : 0L;
            return p;
        }

        private static String str(Object o) { return o == null ? "" : String.valueOf(o); }
    }

    /** Fills the views under {@code root}. {@code fullText} shows the whole body; the feed shows the first lines. */
    public static void bind(Activity host, View root, Post post, boolean fullText, Runnable onCommentPosted) {
        final String id = post.id;
        root.setTag(R.id.tv_title, id);                     // a recycled card ignores answers meant for its previous post

        TextView title = root.findViewById(R.id.tv_title);
        TextView body = root.findViewById(R.id.tv_body);
        TextView category = root.findViewById(R.id.tv_category);
        TextView meta = root.findViewById(R.id.tv_meta);
        TextView name = root.findViewById(R.id.tv_name);
        TextView trust = root.findViewById(R.id.tv_trust);
        ImageView avatar = root.findViewById(R.id.iv_avatar);
        ImageView cover = root.findViewById(R.id.iv_cover);
        TextView viewsLine = root.findViewById(R.id.tv_views_line);

        title.setText(post.title.isEmpty() ? "Untitled" : post.title);
        body.setText(post.body);
        if (fullText) { body.setMaxLines(Integer.MAX_VALUE); body.setEllipsize(null); }
        else { body.setMaxLines(5); body.setEllipsize(android.text.TextUtils.TruncateAt.END); }
        final VideoLinks.Video video = VideoLinks.parse(post.videoUrl);
        category.setText((post.category.isEmpty() ? "General" : post.category) + (video != null ? "  \u00b7  \u25B6 Video" : ""));
        meta.setText(LearnRules.timeAgo(post.createdAt, System.currentTimeMillis()));
        viewsLine.setText(LearnRules.compactCount(post.views) + (post.views == 1 ? " view" : " views"));

        if (!post.imageUrl.isEmpty()) {
            cover.setVisibility(View.VISIBLE);
            Glide.with(host).load(Uri.parse(post.imageUrl)).into(cover);
        } else {
            cover.setVisibility(View.GONE);
            cover.setImageDrawable(null);
        }

        // A video post: in the full reader the player takes the cover's place. It is only a picture and a play button until pressed.
        View oldPlayer = root.findViewWithTag("learn_video_player");
        if (oldPlayer != null && oldPlayer.getParent() instanceof ViewGroup) ((ViewGroup) oldPlayer.getParent()).removeView(oldPlayer);
        if (fullText && video != null && cover.getParent() instanceof android.widget.LinearLayout) {
            android.widget.LinearLayout holder = (android.widget.LinearLayout) cover.getParent();
            VideoPlayerView player = new VideoPlayerView(host, video);
            player.setTag("learn_video_player");
            android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = (int) (12 * host.getResources().getDisplayMetrics().density);
            holder.addView(player, holder.indexOfChild(cover) + 1, lp);
            cover.setVisibility(View.GONE);
        }

        // The author, with the trust score the node wrote on their profile. Shown even before it is scored, and never hideable.
        name.setText("Harvana Community");
        ProfileTrust.bind(trust, null);
        final ProfileTrust.Card[] author = {null};
        ProfileTrust.resolve(post.authorUid, card -> {
            if (!id.equals(root.getTag(R.id.tv_title))) return;
            author[0] = card;
            if (!card.name.isEmpty()) name.setText(card.name);
            meta.setText((card.username.isEmpty() ? "" : "@" + card.username + " · ") + LearnRules.timeAgo(post.createdAt, System.currentTimeMillis()));
            ProfileTrust.bind(trust, card);
            if (!card.icon.isEmpty()) Glide.with(host).load(Uri.parse(card.icon)).placeholder(R.drawable.ic_coin_harv).into(avatar);
        });

        bindCounts(host, root, id);
        root.findViewById(R.id.ll_comment).setOnClickListener(v -> LearnCommentsSheet.show(host, id, () -> {
            bindCounts(host, root, id);
            if (onCommentPosted != null) onCommentPosted.run();
        }));
        root.findViewById(R.id.ll_share).setOnClickListener(v -> share(host, post));
        root.findViewById(R.id.ll_gift).setOnClickListener(v -> LearnGift.ask(host, id, author[0]));
        root.findViewById(R.id.iv_more).setOnClickListener(v -> more(host, v, post));
    }

    private static void bindCounts(Activity host, View root, String id) {
        TextView fireCount = root.findViewById(R.id.tv_fire);
        TextView commentCount = root.findViewById(R.id.tv_comment);
        View fire = root.findViewById(R.id.ll_fire);
        ImageView fireIcon = root.findViewById(R.id.iv_fire);
        final long[] fires = {0};
        final boolean[] mine = {false};

        Runnable paint = () -> {
            fireCount.setText(LearnRules.compactCount(fires[0]));
            fire.setBackgroundResource(mine[0] ? R.drawable.bg_learn_action_on : R.drawable.bg_learn_action);
            int tint = ContextCompat.getColor(host, mine[0] ? R.color.colorAccent : R.color.colorTextTertiary);
            fireIcon.setColorFilter(tint);
            fireCount.setTextColor(ContextCompat.getColor(host, mine[0] ? R.color.colorAccent : R.color.colorTextSecondary));
        };
        paint.run();

        LearnSocial.fireCount(id, n -> host.runOnUiThread(() -> { if (!id.equals(root.getTag(R.id.tv_title))) return; fires[0] = n; paint.run(); }));
        LearnSocial.hasFired(id, on -> host.runOnUiThread(() -> { if (!id.equals(root.getTag(R.id.tv_title))) return; mine[0] = on; paint.run(); }));
        LearnSocial.commentCount(id, n -> host.runOnUiThread(() -> {
            if (!id.equals(root.getTag(R.id.tv_title))) return;
            commentCount.setText(LearnRules.compactCount(n));
        }));

        fire.setOnClickListener(v -> {
            boolean next = !mine[0];
            mine[0] = next;
            fires[0] = Math.max(0, fires[0] + (next ? 1 : -1));
            paint.run();                                            // optimistic; undone below if the write fails
            LearnSocial.setFire(id, next, (ok, error) -> host.runOnUiThread(() -> {
                if (ok) return;
                mine[0] = !next;
                fires[0] = Math.max(0, fires[0] + (next ? -1 : 1));
                paint.run();
                Toast.makeText(host, error, Toast.LENGTH_SHORT).show();
            }));
        });
    }

    private static void share(Activity host, Post post) {
        String snippet = post.body.length() > 280 ? post.body.substring(0, 277) + "..." : post.body;
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, post.title + "\n\n" + snippet + "\n\nShared from Harvana");
        host.startActivity(Intent.createChooser(send, "Share"));
    }

    private static void more(Activity host, View anchor, Post post) {
        PopupMenu menu = new PopupMenu(host, anchor);
        menu.getMenu().add(0, 1, 0, "Copy text");
        menu.setOnMenuItemClickListener(item -> {
            ClipboardManager cm = (ClipboardManager) host.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("Learn Hub", post.title + "\n\n" + post.body));
            Toast.makeText(host, "Copied", Toast.LENGTH_SHORT).show();
            return true;
        });
        menu.show();
    }
}
