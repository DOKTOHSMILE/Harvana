package com.harvana.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.widget.NestedScrollView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.harvana.app.learn.LearnPostBinder;
import com.harvana.app.learn.LearnRules;
import com.harvana.app.sync.NodeEngagementClient;
import com.harvana.app.wallet.WalletManager;

import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

/**
 * The full post. When the reader has reached the end and stayed long enough, the app sends the node a signed receipt;
 * the node decides whether it counts (once per post, a daily cap, no racing, no automated patterns) and a counted read adds to
 * the reader's trust score as real engagement. The time spent counts only while this screen is in front.
 */
public class LearnReadActivity extends AppCompatActivity {

    public static final String EXTRA_ID = "id", EXTRA_TITLE = "title", EXTRA_BODY = "body", EXTRA_CATEGORY = "category",
            EXTRA_IMAGE = "imageUrl", EXTRA_AUTHOR = "authorUid", EXTRA_CREATED = "createdAt", EXTRA_VIEWS = "views";

    private static final String PREFS = "learn_reads";
    private static final String KEY_DONE = "done_ids";

    private final Handler ticker = new Handler(Looper.getMainLooper());
    private LearnPostBinder.Post post;
    private NestedScrollView scroll;
    private TextView status;
    private long dwellMs;
    private long resumedAt;
    private boolean reachedEnd;
    private boolean submitting;
    private boolean finished;                       // counted, already counted, or not eligible: nothing more to send
    private int words;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_learn_read);
        Toolbar toolbar = findViewById(R.id._toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        post = new LearnPostBinder.Post();
        post.id = str(EXTRA_ID); post.title = str(EXTRA_TITLE); post.body = str(EXTRA_BODY); post.category = str(EXTRA_CATEGORY);
        post.imageUrl = str(EXTRA_IMAGE); post.authorUid = str(EXTRA_AUTHOR);
        post.createdAt = getIntent().getLongExtra(EXTRA_CREATED, 0L);
        post.views = getIntent().getLongExtra(EXTRA_VIEWS, 0L);
        words = LearnRules.wordCount(post.title, post.body);

        scroll = findViewById(R.id.scroll_read);
        status = findViewById(R.id.tv_read_status);
        LearnPostBinder.bind(this, findViewById(android.R.id.content), post, true, null);

        scroll.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) (v, x, y, ox, oy) -> checkEnd());
        scroll.post(this::checkEnd);

        countView();
        decideEligibility();
    }

    private String str(String key) { String v = getIntent().getStringExtra(key); return v == null ? "" : v; }

    /** One view per open. The rules allow a signed-in user to change only the views field. */
    private void countView() {
        if (post.id.isEmpty() || FirebaseAuth.getInstance().getCurrentUser() == null) return;
        FirebaseFirestore.getInstance().collection("LEARN_CONTENT").document(post.id).update("views", FieldValue.increment(1));
    }

    private void decideEligibility() {
        if (post.id.isEmpty()) { finished = true; return; }
        com.google.firebase.auth.FirebaseUser me = FirebaseAuth.getInstance().getCurrentUser();
        if (me != null && me.getUid().equals(post.authorUid)) { finished = true; setStatus("Your own post does not count as a read.", false); return; }
        if (doneIds().contains(post.id)) { finished = true; setStatus("You have read this one.", true); return; }
        String wallet = WalletManager.getActiveWalletAddress(this);
        if (wallet == null || wallet.isEmpty()) { finished = true; setStatus("Create a wallet to have your reading count toward your trust score.", false); return; }
        setStatus("Read to the end to count this as a read.", false);
    }

    private void checkEnd() {
        if (finished || scroll == null || scroll.getChildCount() == 0) return;
        View content = scroll.getChildAt(0);
        int slack = (int) (48 * getResources().getDisplayMetrics().density);
        if (scroll.getScrollY() + scroll.getHeight() >= content.getBottom() - slack) reachedEnd = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumedAt = System.currentTimeMillis();
        ticker.post(tick);
    }

    @Override
    protected void onPause() {
        super.onPause();
        ticker.removeCallbacks(tick);
        if (resumedAt > 0) { dwellMs += System.currentTimeMillis() - resumedAt; resumedAt = 0; }
        maybeSubmit();
    }

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            checkEnd();
            maybeSubmit();
            if (!finished && !submitting) {
                long spent = dwellMs + (resumedAt > 0 ? System.currentTimeMillis() - resumedAt : 0);
                long need = LearnRules.requiredDwellMs(words);
                if (reachedEnd && spent < need) setStatus("Almost there. Stay a little longer to count this read.", false);
                else if (!reachedEnd) setStatus("Read to the end to count this as a read.", false);
            }
            ticker.postDelayed(this, 1000);
        }
    };

    private void maybeSubmit() {
        if (finished || submitting || !reachedEnd) return;
        long spent = dwellMs + (resumedAt > 0 ? System.currentTimeMillis() - resumedAt : 0);
        if (spent < LearnRules.requiredDwellMs(words)) return;
        String wallet = WalletManager.getActiveWalletAddress(this);
        if (wallet == null || wallet.isEmpty()) return;
        submitting = true;
        final Context app = getApplicationContext();
        NodeEngagementClient.submitRead(app, wallet, post.id, words, spent, true, new NodeEngagementClient.Callback() {
            @Override public void onSuccess(JSONObject json) {
                runOnUiThread(() -> {
                    submitting = false;
                    if (json.optBoolean("counted", false)) {
                        finished = true; remember(post.id);
                        setStatus("Counted as a read. It adds to your trust score. Total verified reads: " + json.optInt("verifiedReads", 0) + ".", true);
                    } else {
                        handleRefusal(json.optString("reason", ""));
                    }
                });
            }
            @Override public void onError(String error) {
                runOnUiThread(() -> { submitting = false; setStatus("Could not record this read right now. It will be tried again.", false); });
            }
        });
    }

    private void handleRefusal(String reason) {
        switch (reason) {
            case "ALREADY_COUNTED": finished = true; remember(post.id); setStatus("You have read this one.", true); break;
            case "DAILY_CAP": finished = true; setStatus("You have reached today's limit of counted reads. Reads count again tomorrow.", false); break;
            case "TOO_SOON": setStatus("Give it a moment before the next read counts.", false); break;
            case "TOO_FAST": setStatus("That was quick. Stay a little longer on a post for it to count.", false); dwellMs = 0; reachedEnd = false; break;
            default: setStatus("This read could not be counted.", false);
        }
    }

    private void setStatus(String text, boolean done) {
        if (status == null) return;
        status.setText(text);
        status.setTextColor(androidx.core.content.ContextCompat.getColor(this, done ? R.color.colorSuccess : R.color.colorTextTertiary));
        status.setCompoundDrawablesWithIntrinsicBounds(done ? R.drawable.ic_learn_check : 0, 0, 0, 0);
        status.setCompoundDrawablePadding((int) (8 * getResources().getDisplayMetrics().density));
        if (done && status.getCompoundDrawables()[0] != null) {
            status.getCompoundDrawables()[0].mutate().setTint(androidx.core.content.ContextCompat.getColor(this, R.color.colorSuccess));
        }
    }

    private Set<String> doneIds() {
        SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
        return new HashSet<>(p.getStringSet(KEY_DONE, new HashSet<>()));
    }

    private void remember(String id) {
        Set<String> ids = doneIds();
        ids.add(id);
        if (ids.size() > 500) ids.clear();                 // the node still refuses a repeat; this only saves a request
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putStringSet(KEY_DONE, ids).apply();
    }
}
