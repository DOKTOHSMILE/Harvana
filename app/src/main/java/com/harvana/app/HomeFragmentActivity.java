package com.harvana.app;

import android.animation.*;
import android.app.*;
import android.app.Activity;
import android.content.*;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.media.*;
import android.net.*;
import android.net.Uri;
import android.os.*;
import android.text.*;
import android.text.style.*;
import android.util.*;
import android.view.*;
import android.view.View;
import android.view.View.*;
import android.view.animation.*;
import android.widget.*;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.*;
import androidx.cardview.widget.CardView;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.budiyev.android.codescanner.*;
import com.bumptech.glide.*;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.card.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.*;
import java.io.*;
import java.text.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.regex.*;
import org.json.*;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.harvana.app.wallet.BalanceFormatter;
import com.harvana.app.wallet.WalletManager;
import com.harvana.app.wallet.WalletNodeBalanceHelper;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.shape.CornerFamily;
import com.google.android.material.bottomnavigation.BottomNavigationView.OnNavigationItemSelectedListener;

public class HomeFragmentActivity extends Fragment {

    private FirebaseDatabase _firebase = FirebaseDatabase.getInstance();

    private HashMap<String, Object> user_v_map = new HashMap<>();
    private String mySeedPhrase = "";

    private ArrayList<HashMap<String, Object>> lmap_custom_button = new ArrayList<>();
    private ArrayList<HashMap<String, Object>> new_test_map = new ArrayList<>();
    private ArrayList<HashMap<String, Object>> paymentsMap = new ArrayList<>();
    private ArrayList<HashMap<String, Object>> articlesMap = new ArrayList<>();
    private ArrayList<HashMap<String, Object>> trendingMap = new ArrayList<>();
    private ArrayList<HashMap<String, Object>> group_map_list = new ArrayList<>();

    private NestedScrollView vscroll1;
    private LinearLayout linear2;
    private LinearLayout linear194;
    private MaterialCardView cardview28;
    private LinearLayout linear36;
    private TextView textview2;
    private TextView textview3;
    private TextView textview4;
    private LinearLayout linear217;
    private LinearLayout linear200;
    private LinearLayout layout_metric_grid;
    private MaterialCardView cardview1;
    private MaterialCardView cardview4;
    private MaterialCardView cardview2;
    private MaterialCardView cardview3;
    private LinearLayout cardview29;
    private LinearLayout cardview30;
    private LinearLayout cardview31;
    private LinearLayout linear37;
    private LinearLayout linear40;
    private LinearLayout card_total_saved;
    private LinearLayout card_active_groups;
    private LinearLayout linear198;
    private LinearLayout linear38;
    private LinearLayout linear219;
    private ImageView imageview58;
    private TextView textview205;
    private TextView text_val_saved;
    private TextView text_sub_saved;
    private LinearLayout linear196;
    private LinearLayout linear39;
    private LinearLayout linear218;
    private ImageView imageview57;
    private TextView textview204;
    private TextView text_val_groups;
    private TextView text_sub_groups;
    private LinearLayout card_trust_score;
    private LinearLayout card_open_loans;
    private LinearLayout linear201;
    private FrameLayout linear202;
    private LinearLayout linear215;
    private ProgressBar ring_trust_score;
    private TextView text_val_trust;
    private TextView textview206;
    private TextView text_sub_trust;
    private LinearLayout linear203;
    private LinearLayout linear41;
    private LinearLayout linear220;
    private ImageView imageview59;
    private TextView textview207;
    private TextView text_val_loans;
    private TextView text_sub_loans;
    private LinearLayout linear209;
    private LinearLayout linear210;
    private LinearLayout linear_upcoming_payments;
    private LinearLayout linear7;
    private TextView textview212;
    private TextView textview6;
    private ImageView imageview54;
    private TextView textview213;
    private LinearLayout linear30;
    private LinearLayout linear42;
    private TextView textview5;
    private LinearLayout linear221;
    private LinearLayout linear206;
    private CardView cardview27;
    private LinearLayout linear207;
    private ImageView imageview53;
    private LinearLayout linear208;
    private LinearLayout linear225;
    private TextView tv_local_currency_eqv;
    private TextView tv_harv_balance;
    private ImageView imageview61;
    private LinearLayout linear223;
    private MaterialCardView cardview32;
    private LinearLayout linear222;
    private TextView textview220;
    private ImageView imageview60;
    private LinearLayout home_recent_tx;
    private LinearLayout home_savings_rows;
    private LinearLayout linear211;
    private LinearLayout linear212;
    private TextView textview215;
    private LinearLayout linear_learn_hub;
    private TextView textview10;
    private ImageView imageview55;
    private TextView textview214;
    private LinearLayout linear213;
    private LinearLayout linear214;
    private LinearLayout linear_trending_groups;
    private LinearLayout linear10;
    private TextView textview216;
    private TextView textview12;
    private ImageView imageview56;
    private TextView textview217;

    private SharedPreferences authsp;
    private Calendar c = Calendar.getInstance();
    private SharedPreferences groupsp;
    private Intent intent = new Intent();
    private FirebaseAuth fbauth;
    private OnCompleteListener<AuthResult> _fbauth_create_user_listener;
    private OnCompleteListener<AuthResult> _fbauth_sign_in_listener;
    private OnCompleteListener<Void> _fbauth_reset_password_listener;
    private OnCompleteListener<Void> fbauth_updateEmailListener;
    private OnCompleteListener<Void> fbauth_updatePasswordListener;
    private OnCompleteListener<Void> fbauth_emailVerificationSentListener;
    private OnCompleteListener<Void> fbauth_deleteUserListener;
    private OnCompleteListener<Void> fbauth_updateProfileListener;
    private OnCompleteListener<AuthResult> fbauth_phoneAuthListener;
    private OnCompleteListener<AuthResult> fbauth_googleSignInListener;

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater _inflater, @Nullable ViewGroup _container,
            @Nullable Bundle _savedInstanceState) {
        View _view = _inflater.inflate(R.layout.home_fragment, _container, false);
        initialize(_savedInstanceState, _view);
        FirebaseApp.initializeApp(getContext());
        initializeLogic();
        _addQuickActions(_view);
        _wireHome(_view);
        if (card_trust_score != null)
            card_trust_score.setOnClickListener(
                    v -> startActivity(new Intent(getContext(), TrustScoreActivity.class)));
        if (card_open_loans != null)
            card_open_loans.setOnClickListener(
                    v -> startActivity(new Intent(getContext(), OpenLoansActivity.class)));
        return _view;
    }

    private void initialize(Bundle _savedInstanceState, View _view) {
        vscroll1 = _view.findViewById(R.id.vscroll1);
        linear2 = _view.findViewById(R.id.linear2);
        textview2 = _view.findViewById(R.id.textview2);
        textview3 = _view.findViewById(R.id.textview3);
        textview6 = _view.findViewById(R.id.textview6);
        textview10 = _view.findViewById(R.id.textview10);
        textview12 = _view.findViewById(R.id.textview12);

        card_total_saved = _view.findViewById(R.id.card_total_saved);
        card_active_groups = _view.findViewById(R.id.card_active_groups);
        card_trust_score = _view.findViewById(R.id.card_trust_score);
        card_open_loans = _view.findViewById(R.id.card_open_loans);

        text_val_saved = _view.findViewById(R.id.text_val_saved);
        text_val_groups = _view.findViewById(R.id.text_val_groups);
        text_val_trust = _view.findViewById(R.id.text_val_trust);
        text_val_loans = _view.findViewById(R.id.text_val_loans);

        linear7 = _view.findViewById(R.id.linear7);
        linear_upcoming_payments = _view.findViewById(R.id.linear_upcoming_payments);
        linear10 = _view.findViewById(R.id.linear10);
        linear_trending_groups = _view.findViewById(R.id.linear_trending_groups);
        linear_learn_hub = _view.findViewById(R.id.linear_learn_hub);

        tv_local_currency_eqv = _view.findViewById(R.id.tv_local_currency_eqv);
        tv_harv_balance = _view.findViewById(R.id.tv_harv_balance);
        imageview61 = _view.findViewById(R.id.imageview61);

        com.harvana.app.utils.BalancePrivacy.bindEye(imageview61);
        com.harvana.app.utils.BalancePrivacy.guard(tv_harv_balance);
        com.harvana.app.utils.BalancePrivacy.guard(tv_local_currency_eqv);
        com.harvana.app.utils.BalancePrivacy.guard(text_val_saved);
        com.harvana.app.utils.BalancePrivacy.guard(text_val_loans);

        home_recent_tx = _view.findViewById(R.id.home_recent_tx);
        home_savings_rows = _view.findViewById(R.id.home_savings_rows);
        authsp = getContext().getSharedPreferences("authsp", Activity.MODE_PRIVATE);
        groupsp = getContext().getSharedPreferences("groupsp", Activity.MODE_PRIVATE);
        fbauth = FirebaseAuth.getInstance();

        if (card_active_groups != null)
            card_active_groups.setOnClickListener(_v -> _openTab(R.id.menu_groups));
        if (textview6 != null)
            textview6.setOnClickListener(_v -> _openTab(R.id.menu_groups));
        if (textview10 != null)
            textview10.setOnClickListener(_v -> _openTab(R.id.menu_learn));
        if (textview12 != null)
            textview12.setOnClickListener(_v -> _openTab(R.id.menu_discover));

        fbauth_updateEmailListener = _param1 -> {
            final boolean _success = _param1.isSuccessful();
            final String _errorMessage = _param1.getException() != null
                    ? _param1.getException().getMessage()
                    : "";
        };

        fbauth_updatePasswordListener = _param1 -> {
            final boolean _success = _param1.isSuccessful();
            final String _errorMessage = _param1.getException() != null
                    ? _param1.getException().getMessage()
                    : "";
        };

        fbauth_emailVerificationSentListener = _param1 -> {
            final boolean _success = _param1.isSuccessful();
            final String _errorMessage = _param1.getException() != null
                    ? _param1.getException().getMessage()
                    : "";
        };

        fbauth_deleteUserListener = _param1 -> {
            final boolean _success = _param1.isSuccessful();
            final String _errorMessage = _param1.getException() != null
                    ? _param1.getException().getMessage()
                    : "";
        };

        fbauth_phoneAuthListener = task -> {
            final boolean _success = task.isSuccessful();
            final String _errorMessage = task.getException() != null
                    ? task.getException().getMessage()
                    : "";
        };

        fbauth_updateProfileListener = _param1 -> {
            final boolean _success = _param1.isSuccessful();
            final String _errorMessage = _param1.getException() != null
                    ? _param1.getException().getMessage()
                    : "";
        };

        fbauth_googleSignInListener = task -> {
            final boolean _success = task.isSuccessful();
            final String _errorMessage = task.getException() != null
                    ? task.getException().getMessage()
                    : "";
        };

        _fbauth_create_user_listener = _param1 -> {
            final boolean _success = _param1.isSuccessful();
            final String _errorMessage = _param1.getException() != null
                    ? _param1.getException().getMessage()
                    : "";
        };

        _fbauth_sign_in_listener = _param1 -> {
            final boolean _success = _param1.isSuccessful();
            final String _errorMessage = _param1.getException() != null
                    ? _param1.getException().getMessage()
                    : "";
        };

        _fbauth_reset_password_listener = _param1 -> {
            final boolean _success = _param1.isSuccessful();
        };
    }

    private void initializeLogic() {
        Calendar c = Calendar.getInstance();
        if (textview2 != null)
            textview2.setText(
                    new SimpleDateFormat("EEEE dd MMMM", Locale.getDefault()).format(c.getTime()));
        if (textview4 != null)
            textview4.setVisibility(View.GONE);
        com.harvana.app.home.HomeData.listen(homeListener);
        _render(com.harvana.app.home.HomeData.cached(getContext()));
        _loadWalletBalance();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden)
            return;
        com.harvana.app.home.HomeData.refresh(getContext(), false);
        _loadWalletBalance();
        _loadRecentTx();
        _loadTopCoins();
        _loadStake();
        _loadHomeInvites();
        _paintHideLabel();
    }

    private static int invitesAnnounced = -1;

    @Override
    public void onResume() {
        super.onResume();
        com.harvana.app.home.HomeData.refresh(getContext(), false);
        _loadWalletBalance();
        _loadRecentTx();
        _loadTopCoins();
        _loadStake();
        _loadHomeInvites();
        _paintHideLabel();
        if (getContext() != null) {
            com.harvana.app.utils.NetworkStatusBanner.refresh(linear2, 1,
                    com.harvana.app.wallet.WalletManager.getActiveWalletAddress(getContext()));
            com.harvana.app.sync.NodeLinkManager.ensureLinked(getContext(), null);
            com.harvana.app.utils.ActivityTracker.reportLiveness(getContext());
            com.harvana.app.group.InviteManager.checkAndNotify(getContext());
        }
    }

    public void _MORE() {
    }

    private void _openTab(int menuId) {
        if (getActivity() == null)
            return;
        BottomNavigationView nav = getActivity().findViewById(R.id.bottomnavigation1);
        if (nav != null)
            nav.setSelectedItemId(menuId);
    }

    private final java.util.Map<String, String> shownSig = new HashMap<>();
    private final com.harvana.app.home.HomeData.Listener homeListener = s -> {
        if (isAdded() && getView() != null)
            _render(s);
    };

    private boolean _changed(String key, String sig) {
        if (sig.equals(shownSig.get(key)))
            return false;
        shownSig.put(key, sig);
        return true;
    }

    private void _render(org.json.JSONObject s) {
        if (getContext() == null)
            return;
        boolean signedIn = FirebaseAuth.getInstance().getCurrentUser() != null;
        String first = s.optString("name", "");
        if (textview3 != null)
            textview3.setText(first.isEmpty() ? "Welcome back," : "Welcome back, " + first);
        homeSnap = s;

        org.json.JSONArray ids = s.optJSONArray("groupIds");
        org.json.JSONObject groups = s.optJSONObject("groups");
        int active = groups != null ? groups.optInt("active") : ids != null ? ids.length() : -1;
        if (text_val_groups != null)
            text_val_groups.setText(active < 0 ? "—" : String.valueOf(active));
        if (text_sub_groups != null)
            text_sub_groups.setText(active < 0 ? ""
                    : active == 0 ? "Join your first circle"
                            : active == 1 ? "1 active circle" : active + " active circles");

        org.json.JSONObject saved = s.optJSONObject("saved");
        if (text_val_saved != null)
            text_val_saved.setText(saved == null ? "—" : saved.optString("text", "0"));
        int across = saved == null ? 0 : saved.optInt("across");
        if (text_sub_saved != null)
            text_sub_saved.setText(saved == null ? ""
                    : across <= 0 ? "No group savings yet"
                            : "Across " + across + (across == 1 ? " group" : " groups"));
        _openTotalSavedOnTap();

        org.json.JSONObject trust = s.optJSONObject("trust");
        if (text_val_trust != null)
            text_val_trust.setText(trust == null ? "—" : trust.optString("value", "—"));
        if (ring_trust_score != null) {
            ring_trust_score.setMax(trust == null ? 100 : Math.max(1, trust.optInt("max", 100)));
            ring_trust_score.setProgress(trust == null ? 0 : trust.optInt("progress"));
        }
        if (text_sub_trust != null)
            text_sub_trust.setText(trust == null ? "" : trust.optString("sub", ""));

        org.json.JSONObject nl = s.optJSONObject("nodeLoans"), fl = s.optJSONObject("fsLoans");
        if (text_val_loans != null) {
            if (nl == null && fl == null) {
                text_val_loans.setText("—");
                if (text_sub_loans != null)
                    text_sub_loans.setText("");
            } else {
                int count = (nl == null ? 0 : nl.optInt("count"))
                        + (fl == null ? 0 : fl.optInt("count"));
                double out = (nl == null ? 0 : nl.optDouble("out", 0))
                        + (fl == null ? 0 : fl.optDouble("out", 0));
                text_val_loans.setText(String.valueOf(count));
                if (text_sub_loans != null)
                    text_sub_loans
                            .setText(count == 0 ? "No open loans" : _fmtHarv(out) + " outstanding");
            }
        }

        _renderUpcoming(s.optJSONObject("upcoming"), signedIn);
        _paintSavings();
        org.json.JSONArray pending = s.optJSONArray("pending");
        trendingPending.clear();
        if (pending != null)
            for (int i = 0; i < pending.length(); i++)
                trendingPending.add(pending.optString(i));
        _renderTrending(s.optJSONObject("trending"), signedIn, String.valueOf(pending));
        _renderLearn(s.optJSONObject("learn"), signedIn);
    }

    private void _placeholder(LinearLayout box, View empty) {
        if (box.getChildCount() > 0)
            return;
        View rows = com.harvana.app.utils.Shimmer.rows(getContext(), 2);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        rows.setPadding(pad, 0, pad, pad / 2);
        box.addView(rows);
        if (empty != null)
            empty.setVisibility(View.GONE);
    }

    private void _renderUpcoming(org.json.JSONObject section, boolean signedIn) {
        if (linear_upcoming_payments == null)
            return;
        if (section == null && signedIn) {
            _placeholder(linear_upcoming_payments, linear7);
            return;
        }
        org.json.JSONArray rows = section == null ? new org.json.JSONArray()
                : section.optJSONArray("rows");
        if (rows == null)
            rows = new org.json.JSONArray();
        if (!_changed("upcoming", rows.toString()))
            return;
        linear_upcoming_payments.removeAllViews();
        if (linear7 != null)
            linear7.setVisibility(rows.length() == 0 ? View.VISIBLE : View.GONE);
        LayoutInflater inf = LayoutInflater.from(getContext());
        for (int i = 0; i < rows.length(); i++) {
            final org.json.JSONObject row = rows.optJSONObject(i);
            if (row == null)
                continue;
            View item = inf.inflate(R.layout.home_upcoming_pay_custom_display,
                    linear_upcoming_payments, false);
            TextView name = item.findViewById(R.id.textview2);
            TextView meta = item.findViewById(R.id.textview3);
            TextView statusLine = item.findViewById(R.id.textview5);
            TextView badge = item.findViewById(R.id.textview4);
            com.google.android.material.card.MaterialCardView badgeCard = item
                    .findViewById(R.id.cardview3);
            com.google.android.material.button.MaterialButton button = item
                    .findViewById(R.id.materialbutton1);
            final String gid = row.optString("groupId");
            final String action = row.optString("action", "NONE");
            final String otherUid = row.optString("otherUid");
            final String who = row.optString("otherName", "a member");
            final String amountText = com.harvana.app.wallet.BalanceFormatter
                    .smart(row.optDouble("amount", 0));
            String freq = row.optString("frequency");

            ImageView face = item.findViewById(R.id.imageview1);
            if (face != null)
                com.harvana.app.utils.GroupRing.show(face, row.optString("groupType"),
                        row.optString("groupPhoto"), R.drawable.ic_coin_harv);
            if (name != null)
                name.setText(row.optString("groupName", "Group"));
            if (meta != null)
                meta.setText(amountText + (freq.isEmpty() ? "" : " · " + freq));
            if (statusLine != null)
                statusLine.setText(row.optString("statusLine"));

            int badgeColor = "PAY".equals(action) ? R.color.colorWarning
                    : "REMIND".equals(action) ? R.color.colorInfo : R.color.colorTextTertiary;
            int c = androidx.core.content.ContextCompat.getColor(requireContext(), badgeColor);
            if (badge != null) {
                badge.setText(row.optString("badge"));
                badge.setTextColor(c);
            }
            if (badgeCard != null)
                badgeCard.setStrokeColor(c);
            item.setOnClickListener(v -> _openGroup(gid));

            if (button != null) {
                if ("PAY".equals(action)) {
                    button.setText("Pay");
                    button.setOnClickListener(v -> _openGroupPayments(gid));
                } else if ("REMIND".equals(action) && !otherUid.isEmpty()) {
                    button.setText("Remind");
                    button.setOnClickListener(v -> {
                        button.setEnabled(false);
                        com.harvana.app.group.GroupReminders.remind(getContext(), gid, otherUid,
                                who, amountText,
                                new com.harvana.app.group.GroupReminders.Callback() {
                                    @Override
                                    public void onSent() {
                                        if (!isAdded())
                                            return;
                                        button.setText("Reminded");
                                        android.widget.Toast
                                                .makeText(getContext(), "Reminder sent to " + who,
                                                        android.widget.Toast.LENGTH_SHORT)
                                                .show();
                                    }

                                    @Override
                                    public void onError(String error) {
                                        if (!isAdded())
                                            return;
                                        button.setEnabled(true);
                                        android.widget.Toast.makeText(getContext(), error,
                                                android.widget.Toast.LENGTH_LONG).show();
                                    }
                                });
                    });
                } else {
                    button.setText("Open");
                    button.setOnClickListener(v -> _openGroup(gid));
                }
            }
            linear_upcoming_payments.addView(item);
        }
    }

    private void _renderTrending(org.json.JSONObject section, boolean signedIn, String pendingSig) {
        if (linear_trending_groups == null)
            return;
        if (section == null && signedIn) {
            _placeholder(linear_trending_groups, linear10);
            return;
        }
        org.json.JSONArray rows = section == null ? new org.json.JSONArray()
                : section.optJSONArray("rows");
        if (rows == null)
            rows = new org.json.JSONArray();
        if (!_changed("trending", rows + "|" + pendingSig))
            return;
        linear_trending_groups.removeAllViews();
        if (linear10 != null)
            linear10.setVisibility(rows.length() == 0 ? View.VISIBLE : View.GONE);
        LayoutInflater inf = LayoutInflater.from(getContext());
        for (int i = 0; i < rows.length(); i++) {
            org.json.JSONObject g = rows.optJSONObject(i);
            if (g == null)
                continue;
            View item = inf.inflate(R.layout.home_trending_custom_display, linear_trending_groups,
                    false);
            TextView name = item.findViewById(R.id.textview1);
            TextView verified = item.findViewById(R.id.textview2);
            TextView meta = item.findViewById(R.id.textview3);
            TextView trust = item.findViewById(R.id.textview4);
            TextView join = item.findViewById(R.id.btn_join);
            final String gid = g.optString("groupId");
            if (name != null)
                name.setText(g.optString("name"));
            ImageView face = item.findViewById(R.id.imageview1);
            if (face != null)
                com.harvana.app.utils.GroupRing.show(face, g.optString("type"),
                        g.optString("photo"), R.drawable.ic_coin_harv);
            if (meta != null)
                meta.setText(g.optString("meta"));
            if (trust != null)
                trust.setText(g.optString("trust"));
            if (verified != null) {
                String b = g.optString("badge");
                verified.setText(b);
                verified.setVisibility(b.isEmpty() ? View.GONE : View.VISIBLE);
            }
            View.OnClickListener open = v -> {
                if (gid.isEmpty()) {
                    _openTab(R.id.menu_discover);
                    return;
                }
                Intent ji = new Intent(getContext(), GroupJoinRequestActivity.class);
                ji.putExtra("groupId", gid);
                startActivity(ji);
            };
            if (join != null) {
                _styleTrendingButton(join, gid);
                join.setOnClickListener(open);
            }
            item.setOnClickListener(open);
            linear_trending_groups.addView(item);
        }
    }

    private void _renderLearn(org.json.JSONObject section, boolean signedIn) {
        if (linear_learn_hub == null)
            return;
        if (section == null && signedIn) {
            _placeholder(linear_learn_hub, null);
            return;
        }
        org.json.JSONArray rows = section == null ? new org.json.JSONArray()
                : section.optJSONArray("rows");
        if (rows == null)
            rows = new org.json.JSONArray();
        if (!_changed("learn", rows.toString()))
            return;
        linear_learn_hub.removeAllViews();
        if (rows.length() == 0) {
            _showLearnHubEmpty("No Learn Hub posts yet");
            return;
        }
        LayoutInflater inf = LayoutInflater.from(getContext());
        for (int i = 0; i < rows.length(); i++) {
            org.json.JSONObject m = rows.optJSONObject(i);
            if (m == null)
                continue;
            View item = inf.inflate(R.layout.home_learnhub_custom_display, linear_learn_hub, false);
            TextView title = item.findViewById(R.id.title_text);
            TextView cat = item.findViewById(R.id.category_text);
            ImageView img = item.findViewById(R.id.imageview1);
            long views = m.optLong("views"), at = m.optLong("at");
            if (title != null)
                title.setText(m.optString("title"));
            if (cat != null) {
                String when = at > 0
                        ? " · " + android.text.format.DateUtils.getRelativeTimeSpanString(at,
                                System.currentTimeMillis(),
                                android.text.format.DateUtils.MINUTE_IN_MILLIS)
                        : "";
                cat.setText(m.optString("category") + " · " + views
                        + (views == 1 ? " view" : " views") + when);
            }
            String image = m.optString("image");
            if (img != null && !image.isEmpty())
                com.bumptech.glide.Glide.with(this).load(image).centerCrop().into(img);
            item.setOnClickListener(v -> _openTab(R.id.menu_learn));
            linear_learn_hub.addView(item);
        }
    }

    private void _showLearnHubEmpty(String message) {
        if (linear_learn_hub == null || getContext() == null)
            return;
        linear_learn_hub.removeAllViews();
        float d = getResources().getDisplayMetrics().density;
        LinearLayout box = new LinearLayout(getContext());
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = (int) (10 * d);
        box.setPadding(pad, pad, pad, pad);
        ImageView art = new ImageView(getContext());
        art.setImageResource(R.drawable.illus_empty_learn);
        art.setAdjustViewBounds(true);
        box.addView(art, new LinearLayout.LayoutParams((int) (160 * d), (int) (120 * d)));
        TextView empty = new TextView(getContext());
        empty.setText(message);
        empty.setTextSize(13f);
        empty.setGravity(Gravity.CENTER);
        empty.setTextColor(androidx.core.content.ContextCompat.getColor(getContext(),
                R.color.colorTextTertiary));
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tp.topMargin = (int) (8 * d);
        box.addView(empty, tp);
        linear_learn_hub.addView(box);
    }

    private String recentSig = "";

    private void _loadRecentTx() {
        if (home_recent_tx == null || getContext() == null)
            return;
        String addr = WalletManager.getActiveWalletAddress(getContext());
        if (addr == null || addr.isEmpty()) {
            _paintRecent(java.util.Collections.emptyList(), "");
            return;
        }
        final String wallet = addr;
        com.harvana.app.sync.HarvanaNodeClient.getHistory(wallet, 0, 2,
                new com.harvana.app.sync.HarvanaNodeClient.JsonCallback() {
                    @Override
                    public void onSuccess(org.json.JSONObject json) {
                        java.util.List<java.util.HashMap<String, Object>> rows = com.harvana.app.wallet.ActivityFeed
                                .parseNodePage(json);
                        if (rows.size() > 2)
                            rows = rows.subList(0, 2);
                        final java.util.List<java.util.HashMap<String, Object>> show = rows;
                        if (getActivity() == null)
                            return;
                        getActivity().runOnUiThread(() -> {
                            if (isAdded())
                                _paintRecent(show, wallet);
                        });
                    }

                    @Override
                    public void onError(String error) {
                        if (getActivity() == null)
                            return;
                        getActivity().runOnUiThread(() -> {
                            if (isAdded())
                                _paintRecent(java.util.Collections.emptyList(), wallet);
                        });
                    }
                });
    }

    private void _paintRecent(java.util.List<java.util.HashMap<String, Object>> rows,
            String wallet) {
        if (home_recent_tx == null || getContext() == null)
            return;
        StringBuilder sig = new StringBuilder(wallet == null ? "" : wallet);
        for (java.util.HashMap<String, Object> m : rows)
            sig.append('|').append(m.get("txHash")).append(m.get("status")).append(m.get("value"));
        if (sig.toString().equals(recentSig) && home_recent_tx.getChildCount() > 0)
            return;
        recentSig = sig.toString();
        home_recent_tx.removeAllViews();
        if (rows.isEmpty()) {
            TextView empty = new TextView(getContext());
            empty.setText("No transactions yet");
            empty.setTextSize(13f);
            empty.setTextColor(androidx.core.content.ContextCompat.getColor(getContext(),
                    R.color.colorTextTertiary));
            int pad = (int) (8 * getResources().getDisplayMetrics().density);
            empty.setPadding(pad, pad, pad, pad / 2);
            home_recent_tx.addView(empty);
            return;
        }
        android.view.LayoutInflater inf = android.view.LayoutInflater.from(getContext());
        for (int i = 0; i < rows.size(); i++) {
            java.util.HashMap<String, Object> item = rows.get(i);
            View row = inf.inflate(R.layout.item_transaction, home_recent_tx, false);
            _bindRecent(row, item, wallet, i == rows.size() - 1);
            _shrinkHomeTx(row);
            home_recent_tx.addView(row);
        }
    }

    /** Home's copy of a transaction row: shorter than the full Activity list. */
    private void _shrinkHomeTx(View row) {
        float d = getResources().getDisplayMetrics().density;
        View line = row.findViewById(R.id.linear13);
        if (line != null) {
            line.setMinimumHeight((int) (48 * d));
            int h = (int) (8 * d);
            line.setPadding(h, (int) (2 * d), h, (int) (2 * d));
        }
        View icon = row.findViewById(R.id.cardview5);
        if (icon != null && icon.getLayoutParams() != null) {
            icon.getLayoutParams().width = (int) (28 * d);
            icon.getLayoutParams().height = (int) (28 * d);
            icon.setLayoutParams(icon.getLayoutParams());
        }
        TextView type = row.findViewById(R.id.tv_tx_type);
        TextView addr = row.findViewById(R.id.tv_tx_address);
        TextView amt = row.findViewById(R.id.tv_tx_amount);
        TextView date = row.findViewById(R.id.tv_tx_date);
        if (type != null)
            type.setTextSize(13f);
        if (addr != null)
            addr.setTextSize(11f);
        if (amt != null)
            amt.setTextSize(12f);
        if (date != null)
            date.setTextSize(10f);
        View divider = row.findViewById(R.id.tx_divider);
        if (divider != null && divider.getLayoutParams() instanceof ViewGroup.MarginLayoutParams)
            ((ViewGroup.MarginLayoutParams) divider.getLayoutParams())
                    .setMarginStart((int) (44 * d));
    }

    private void _bindRecent(View row, java.util.HashMap<String, Object> item, String wallet,
            boolean last) {
        String sender = item.get("senderAddress") == null ? ""
                : String.valueOf(item.get("senderAddress"));
        String recipient = item.get("recipientAddress") == null ? ""
                : String.valueOf(item.get("recipientAddress"));
        String asset = item.get("tokenSymbol") == null ? com.harvana.app.CoinInfo.ticker()
                : String.valueOf(item.get("tokenSymbol")).toUpperCase(java.util.Locale.US);
        if (asset.isEmpty() || "NULL".equals(asset))
            asset = com.harvana.app.CoinInfo.ticker();
        if (com.harvana.app.husd.Stable.isStable(asset))
            asset = com.harvana.app.husd.Stable.symbol(asset);
        final String finalAsset = asset;
        double amount = item.get("value") instanceof Number
                ? ((Number) item.get("value")).doubleValue()
                : 0;
        String status = item.get("status") == null ? "confirmed"
                : String.valueOf(item.get("status"));
        boolean pending = "pending".equalsIgnoreCase(status);
        boolean failed = "failed".equalsIgnoreCase(status);
        boolean incoming = recipient.equalsIgnoreCase(wallet);
        String title = failed ? "Failed " + finalAsset
                : (pending ? (incoming ? "Receiving " : "Sending ") + finalAsset + "…"
                        : (incoming ? "Received " : "Sent ") + finalAsset);
        String memo = item.get("memo") == null ? "" : String.valueOf(item.get("memo"));
        com.harvana.app.tx.TxKind kind = failed ? null
                : com.harvana.app.tx.TxKind.of(memo, incoming);
        if (kind != null)
            title = pending ? kind.title + "…" : kind.title;
        TextView type = row.findViewById(R.id.tv_tx_type);
        TextView addr = row.findViewById(R.id.tv_tx_address);
        TextView amt = row.findViewById(R.id.tv_tx_amount);
        TextView date = row.findViewById(R.id.tv_tx_date);
        ImageView icon = row.findViewById(R.id.iv_tx_icon);
        View divider = row.findViewById(R.id.tx_divider);
        if (type != null)
            type.setText(title);
        String who = incoming ? sender : recipient;
        String shortWho = who.length() <= 16 ? (who.isEmpty() ? "Unknown" : who)
                : who.substring(0, 10) + "…" + who.substring(who.length() - 4);
        if (addr != null)
            addr.setText((incoming ? "From: " : "To: ") + shortWho);
        int color = androidx.core.content.ContextCompat.getColor(getContext(),
                failed ? R.color.colorError
                        : pending ? R.color.colorWarning
                                : incoming ? R.color.colorAccent : R.color.colorError);
        if (amt != null) {
            amt.setText((failed ? "✕ " : incoming ? "+ " : "- ")
                    + BalanceFormatter.smartDecimal(amount) + " " + finalAsset);
            amt.setTextColor(color);
        }
        long ts = item.get("timestamp") instanceof Number
                ? ((Number) item.get("timestamp")).longValue()
                : 0L;
        long initiated = item.get("initiatedAt") instanceof Number
                ? ((Number) item.get("initiatedAt")).longValue()
                : 0L;
        if (date != null)
            date.setText(WalletActivityFragmentActivity._activityWhen(pending, initiated, ts));
        if (icon != null) {
            icon.setImageResource(
                    failed || !incoming ? R.drawable.ic_action_send : R.drawable.ic_action_receive);
            if (kind != null && kind.icon != 0)
                icon.setImageResource(kind.icon);
            icon.setColorFilter(color);
        }
        if (divider != null)
            divider.setVisibility(last ? View.GONE : View.VISIBLE);
        View hit = row.findViewById(R.id.cardview1);
        if (hit != null)
            hit.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), ReceiptActivity.class);
                intent.putExtra("txHash", String.valueOf(item.get("txHash")));
                intent.putExtra("status", status);
                intent.putExtra("senderAddress", sender);
                intent.putExtra("recipientAddress", recipient);
                intent.putExtra("tokenSymbol", finalAsset);
                intent.putExtra("value", amount);
                intent.putExtra("timestamp", ts);
                intent.putExtra("initiatedAt", initiated);
                intent.putExtra("memo", memo);
                if (item.get("harvFee") instanceof Number)
                    intent.putExtra("harvFee", ((Number) item.get("harvFee")).doubleValue());
                if (item.get("feeSymbol") != null)
                    intent.putExtra("feeSymbol", String.valueOf(item.get("feeSymbol")));
                if (item.get("blockNumber") instanceof Number)
                    intent.putExtra("blockNumber", ((Number) item.get("blockNumber")).longValue());
                if (item.get("nonce") instanceof Number)
                    intent.putExtra("nonce", ((Number) item.get("nonce")).longValue());
                v.getContext().startActivity(intent);
            });
    }

    private final java.util.Set<String> trendingPending = new java.util.HashSet<>();

    private void _styleTrendingButton(TextView join, String gid) {
        boolean pending = trendingPending.contains(gid);
        join.setTag(gid);
        join.setText(pending ? "Pending" : "Join");
        join.setSingleLine(true);
        join.setMaxLines(1);
        join.setAlpha(pending ? 0.65f : 1f);
    }

    private final com.harvana.app.wallet.WalletTotal.Listener totalListener = (address, symbol,
            fiat) -> {
        if (!isAdded() || getContext() == null)
            return;
        String addr = WalletManager.getActiveWalletAddress(getContext());
        if (addr != null && addr.equals(address))
            _showTotal(symbol, fiat);
    };

    private void _showTotal(String symbol, double fiat) {
        if (tv_harv_balance != null) {
            tv_harv_balance.setText(Double.isNaN(fiat) ? symbol + " —"
                    : com.harvana.app.wallet.FxRates.format(symbol, fiat));
        }
        if (tv_local_currency_eqv != null) {
            String code = com.harvana.app.wallet.FiatValueHelper.symbolToCurrencyCode(symbol);
            tv_local_currency_eqv.setText("Total balance · " + code + " · all coins");
        }
    }

    private void _loadWalletBalance() {
        if (getContext() == null)
            return;
        String addr = WalletManager.getActiveWalletAddress(getContext());
        String symbol = com.harvana.app.wallet.FxRates.symbol(getContext());
        if (addr == null || addr.isEmpty()) {
            _showTotal(symbol, 0);
            return;
        }
        _showTotal(symbol, com.harvana.app.wallet.WalletTotal.last(getContext(), addr, symbol,
                com.harvana.app.wallet.WalletTotal.HOME_MAX_AGE_MS));
        com.harvana.app.wallet.WalletTotal.listen(totalListener);
        com.harvana.app.wallet.WalletTotal.refresh(getContext(), addr);
    }

    @Override
    public void onDestroyView() {
        com.harvana.app.wallet.WalletTotal.unlisten(totalListener);
        com.harvana.app.home.HomeData.unlisten(homeListener);
        shownSig.clear();
        super.onDestroyView();
    }

    private void _openGroupPayments(String gid) {
        if (gid == null || gid.isEmpty())
            return;
        intent.setClass(getContext().getApplicationContext(), GroupDetailsActivity.class);
        intent.putExtra("groupId", gid);
        intent.putExtra("tab", 2);
        startActivity(intent);
    }

    private void _openGroup(String gid) {
        if (gid == null || gid.isEmpty())
            return;
        intent.setClass(getContext().getApplicationContext(), GroupDetailsActivity.class);
        intent.removeExtra("tab");
        intent.putExtra("groupId", gid);
        startActivity(intent);
    }

    private void _openTotalSavedOnTap() {
        View card = getView() == null ? null : getView().findViewById(R.id.card_total_saved);
        if (card == null)
            card = text_val_saved;
        if (card == null || card.hasOnClickListeners())
            return;
        card.setOnClickListener(x -> startActivity(
                new android.content.Intent(getContext(), TotalSavedActivity.class)));
    }

    private static String _fmtAmount(double v) {
        try {
            java.text.DecimalFormat df = new java.text.DecimalFormat("#,##0.00######",
                    new java.text.DecimalFormatSymbols(java.util.Locale.US));
            return df.format(v);
        } catch (Exception e) {
            return String.valueOf(v);
        }
    }

    private static String _fmtHarv(double v) {
        try {
            java.text.DecimalFormat df = new java.text.DecimalFormat("0.########",
                    new java.text.DecimalFormatSymbols(java.util.Locale.US));
            return df.format(v) + (" " + com.harvana.app.CoinInfo.ticker());
        } catch (Exception e) {
            return String.valueOf(v) + (" " + com.harvana.app.CoinInfo.ticker());
        }
    }

    private void _addQuickActions(View root) {
    }

    private org.json.JSONObject homeSnap;
    private java.util.List<java.util.Map<String, Object>> homeInvites = java.util.Collections
            .emptyList();
    private String savingsSig = "";

    private void _openActivityTab() {
        AssetFragmentActivity.pendingPage = 1;
        _openTab(R.id.menu_asset);
    }

    private void _wireHome(View root) {
        if (getContext() == null)
            return;
        View bell = root.findViewById(R.id.btn_home_bell);
        View more = root.findViewById(R.id.btn_home_more);
        View avatar = root.findViewById(R.id.iv_home_avatar);
        View hide = root.findViewById(R.id.tv_home_hide);
        if (bell != null)
            bell.setOnClickListener(
                    v -> startActivity(new Intent(getContext(), NotificationsActivity.class)));
        if (more != null)
            more.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity)
                    ((MainActivity) getActivity()).showRightDropdownMenu(v);
            });
        if (avatar != null)
            avatar.setOnClickListener(v -> {
                com.google.firebase.auth.FirebaseUser me = FirebaseAuth.getInstance()
                        .getCurrentUser();
                if (me == null)
                    startActivity(new Intent(getContext(), ProfileActivity.class));
                else
                    startActivity(new Intent(getContext(),
                            com.harvana.app.social.PublicProfileActivity.class)
                            .putExtra(com.harvana.app.social.PublicProfileActivity.EXTRA_UID,
                                    me.getUid()));
            });
        _loadAvatar(root.findViewById(R.id.iv_home_avatar));
        com.harvana.app.utils.BalancePrivacy.bindEye(imageview61);
        View.OnClickListener tog = v -> {
            com.harvana.app.utils.BalancePrivacy.toggle(v.getContext());
            _paintHideLabel();
        };
        if (imageview61 != null)
            imageview61.setOnClickListener(tog);
        if (hide != null)
            hide.setOnClickListener(tog);
        _paintHideLabel();
        for (int id : new int[] { R.id.tv_coin_harv, R.id.tv_coin_harv_fiat, R.id.tv_coin_btc,
                R.id.tv_coin_btc_fiat, R.id.tv_coin_eth, R.id.tv_coin_eth_fiat,
                R.id.tv_home_change }) {
            View viewToGuard = root.findViewById(id);
            if (viewToGuard instanceof TextView) {
                com.harvana.app.utils.BalancePrivacy.guard((TextView) viewToGuard);
            }
        }
        View add = root.findViewById(R.id.btn_home_add);
        View send = root.findViewById(R.id.btn_home_send);
        View transfer = root.findViewById(R.id.btn_home_transfer);
        View receive = root.findViewById(R.id.btn_home_receive);
        if (add != null)
            add.setOnClickListener(
                    v -> startActivity(new Intent(getContext(), P2pMarketplaceActivity.class)));
        if (send != null)
            send.setOnClickListener(
                    v -> startActivity(new Intent(getContext(), SendAssetActivity.class)));
        if (transfer != null)
            transfer.setOnClickListener(v -> _transferIntoCircle());
        if (receive != null)
            receive.setOnClickListener(
                    v -> startActivity(new Intent(getContext(), ReceiveActivity.class)));
        View savings = root.findViewById(R.id.qa_savings);
        View groups = root.findViewById(R.id.qa_groups);
        View stake = root.findViewById(R.id.qa_stake);
        View swap = root.findViewById(R.id.qa_swap);
        View activity = root.findViewById(R.id.btn_home_activity);
        if (savings != null)
            savings.setOnClickListener(
                    v -> startActivity(new Intent(getContext(), TotalSavedActivity.class)));
        if (groups != null)
            groups.setOnClickListener(v -> _openTab(R.id.menu_groups));
        if (stake != null)
            stake.setOnClickListener(
                    v -> startActivity(new Intent(getContext(), StakingDashboardActivity.class)));
        if (swap != null)
            swap.setOnClickListener(v -> startActivity(
                    new Intent(getContext(), com.harvana.app.swap.SwapActivity.class)));
        if (activity != null)
            activity.setOnClickListener(v -> _openActivityTab());
        View harv = root.findViewById(R.id.home_coin_harv);
        View btc = root.findViewById(R.id.home_coin_btc);
        View eth = root.findViewById(R.id.home_coin_eth);
        if (harv != null)
            harv.setOnClickListener(v -> _openCoin("HARV"));
        if (btc != null)
            btc.setOnClickListener(v -> _openCoin("BTC"));
        if (eth != null)
            eth.setOnClickListener(v -> _openCoin("ETH"));
    }

    private void _openCoin(String key) {
        if (getContext() == null)
            return;
        startActivity(new Intent(getContext(), CoinDetailActivity.class)
                .putExtra(com.harvana.app.wallet.CoinCatalog.EXTRA, key));
    }

    private void _transferIntoCircle() {
        org.json.JSONObject up = homeSnap == null ? null : homeSnap.optJSONObject("upcoming");
        org.json.JSONArray rows = up == null ? null : up.optJSONArray("rows");
        if (rows != null)
            for (int i = 0; i < rows.length(); i++) {
                org.json.JSONObject r = rows.optJSONObject(i);
                if (r != null && "PAY".equals(r.optString("action"))
                        && !r.optString("groupId").isEmpty()) {
                    _openGroupPayments(r.optString("groupId"));
                    return;
                }
            }
        _openTab(R.id.menu_groups);
    }

    private void _paintHideLabel() {
        TextView t = getView() == null ? null : getView().findViewById(R.id.tv_home_hide);
        if (t == null || getContext() == null)
            return;
        t.setText(com.harvana.app.utils.BalancePrivacy.isHidden(getContext()) ? "Show" : "Hide");
    }

    private void _loadAvatar(ImageView iv) {
        if (iv == null || getContext() == null)
            return;
        com.google.firebase.auth.FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null)
            return;
        String photo = user.getPhotoUrl() == null ? "" : user.getPhotoUrl().toString().trim();
        try {
            java.util.Map<String, Object> mine = com.harvana.app.offline.OfflineCacheManager
                    .load(getContext(), "profile_" + user.getUid());
            Object ic = mine == null ? null : mine.get("icon");
            if (ic != null && String.valueOf(ic).startsWith("http"))
                photo = String.valueOf(ic).trim();
        } catch (Exception ignored) {
        }
        if (photo.isEmpty())
            return;
        iv.setPadding(0, 0, 0, 0);
        iv.setImageTintList(null);
        try {
            com.bumptech.glide.Glide.with(this).load(photo).circleCrop().into(iv);
        } catch (Exception ignored) {
        }
    }

    private void _loadTopCoins() {
        if (getContext() == null)
            return;
        final android.content.Context ctx = getContext().getApplicationContext();
        final String harvAddr = WalletManager.getActiveWalletAddress(ctx);
        final String symbol = com.harvana.app.wallet.FxRates.symbol(ctx);
        com.harvana.app.wallet.HarvPrice.get(q -> {
            if (!isAdded() || q == null || Double.isNaN(q.changePct))
                return;
            TextView t = getView() == null ? null : getView().findViewById(R.id.tv_home_change);
            if (t == null)
                return;
            t.setText((q.changePct >= 0 ? "+" : "")
                    + String.format(java.util.Locale.US, "%.2f%% HARV today", q.changePct));
            t.setTextColor(androidx.core.content.ContextCompat.getColor(ctx,
                    q.changePct >= 0 ? R.color.colorSuccess : R.color.colorError));
            t.setVisibility(View.VISIBLE);
        });
        if (harvAddr == null || harvAddr.isEmpty()) {
            _setCoin(R.id.tv_coin_harv, R.id.tv_coin_harv_fiat, "HARV", null, null);
            _setCoin(R.id.tv_coin_btc, R.id.tv_coin_btc_fiat, "BTC", null, null);
            _setCoin(R.id.tv_coin_eth, R.id.tv_coin_eth_fiat, "ETH", null, null);
            return;
        }
        WalletNodeBalanceHelper.load(harvAddr, (bal, avail, pin, pout, pending, err) -> {
            if (getActivity() == null)
                return;
            double amount = avail >= 0 ? avail : bal;
            if (err != null && !err.isEmpty())
                amount = Double.NaN;
            final double harv = amount;
            com.harvana.app.wallet.FiatValueHelper.valueOfHarv(ctx,
                    Double.isNaN(harv) || harv < 0 ? 0 : harv, symbol, (fiat, ok) -> {
                        if (getActivity() == null)
                            return;
                        getActivity().runOnUiThread(() -> {
                            if (!isAdded())
                                return;
                            _setCoin(R.id.tv_coin_harv, R.id.tv_coin_harv_fiat, "HARV",
                                    Double.isNaN(harv) ? null : harv, ok ? fiat : null);
                        });
                    });
        });
        new Thread(() -> {
            java.util.Map<String, String> addrs;
            try {
                addrs = com.harvana.app.wallet.CoinCatalog.addresses(ctx, harvAddr);
            } catch (Exception e) {
                return;
            }
            final String btcLine = com.harvana.app.wallet.ChainBalances.assetBalance("BTC",
                    addrs.get("BTC"));
            final String ethLine = com.harvana.app.wallet.ChainBalances.assetBalance("ETH",
                    addrs.get("ETH"));
            com.harvana.app.wallet.AssetPrices.load(ctx, () -> {
                if (!isAdded())
                    return;
                _setPricedCoin(ctx, symbol, "BTC", R.id.tv_coin_btc, R.id.tv_coin_btc_fiat,
                        btcLine);
                _setPricedCoin(ctx, symbol, "ETH", R.id.tv_coin_eth, R.id.tv_coin_eth_fiat,
                        ethLine);
            });
        }, "home-coins").start();
    }

    private void _setPricedCoin(android.content.Context ctx, String symbol, String key,
            int amountId, int fiatId, String line) {
        Double held = com.harvana.app.wallet.AssetPrices.amount(line);
        com.harvana.app.wallet.AssetPrices.Quote q = com.harvana.app.wallet.AssetPrices.peek(ctx,
                key);
        Double fiat = null;
        if (held != null && q != null && q.usd > 0) {
            double v = com.harvana.app.wallet.FxRates.fiat(ctx, held * q.usd);
            if (!Double.isNaN(v))
                fiat = v;
        }
        _setCoin(amountId, fiatId, key, held, fiat);
    }

    private void _setCoin(int amountId, int fiatId, String ticker, Double amount, Double fiat) {
        if (getView() == null || getContext() == null)
            return;
        TextView a = getView().findViewById(amountId);
        TextView f = getView().findViewById(fiatId);
        String symbol = com.harvana.app.wallet.FxRates.symbol(getContext());
        if (a != null)
            a.setText(
                    ticker + " " + (amount == null ? "—" : BalanceFormatter.smartDecimal(amount)));
        if (f != null)
            f.setText(fiat == null ? "—" : com.harvana.app.wallet.FxRates.format(symbol, fiat));
    }

    private void _loadStake() {
        // Subtitle view tv_stake_sub was removed in the compact Quick Actions layout
    }

    private void _loadHomeInvites() {
        if (getContext() == null)
            return;
        final android.content.Context ic = getContext().getApplicationContext();
        com.harvana.app.group.InviteManager.listMine(ic,
                new com.harvana.app.group.InviteManager.ListCb() {
                    @Override
                    public void onList(java.util.List<java.util.Map<String, Object>> invites) {
                        if (getActivity() == null)
                            return;
                        getActivity().runOnUiThread(() -> {
                            if (!isAdded())
                                return;
                            homeInvites = invites == null ? java.util.Collections.emptyList()
                                    : invites;
                            savingsSig = "";
                            _paintSavings();
                            if (!homeInvites.isEmpty() && getView() != null
                                    && homeInvites.size() != invitesAnnounced) {
                                invitesAnnounced = homeInvites.size();
                                com.google.android.material.snackbar.Snackbar.make(getView(),
                                        "You have " + homeInvites.size() + " group invitation"
                                                + (homeInvites.size() > 1 ? "s" : ""),
                                        com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
                                        .setAction("View", v -> startActivity(
                                                new Intent(getContext(), InvitesActivity.class)))
                                        .show();
                            }
                        });
                    }

                    @Override
                    public void onError(String e) {
                    }
                });
    }

    /** Invitations only. Payments are the rows above, and the illustration is the empty state. */
    private void _paintSavings() {
        LinearLayout box = getView() == null ? null
                : getView().findViewById(R.id.home_savings_rows);
        if (box == null || getContext() == null)
            return;
        StringBuilder sig = new StringBuilder();
        for (java.util.Map<String, Object> m : homeInvites)
            sig.append('|').append(m.get("_docId"));
        if (sig.toString().equals(savingsSig) && (box.getChildCount() > 0 || homeInvites.isEmpty()))
            return;
        savingsSig = sig.toString();
        box.removeAllViews();
        int n = 0;
        for (java.util.Map<String, Object> inv : homeInvites) {
            if (n++ >= 3)
                break;
            box.addView(_savingsInvite(inv));
        }
        View caughtUp = getView().findViewById(R.id.linear7);
        if (caughtUp == null)
            return;
        boolean payments = linear_upcoming_payments != null
                && linear_upcoming_payments.getChildCount() > 0;
        caughtUp.setVisibility(!homeInvites.isEmpty() || payments ? View.GONE : View.VISIBLE);
    }

    private View _savingsDuty(org.json.JSONObject row) {
        float d = getResources().getDisplayMetrics().density;
        LinearLayout line = _savingsLine();
        ImageView icon = new ImageView(getContext());
        icon.setImageResource(R.drawable.ic_groups_line);
        icon.setColorFilter(
                androidx.core.content.ContextCompat.getColor(getContext(), R.color.colorSuccess));
        line.addView(icon, new LinearLayout.LayoutParams((int) (28 * d), (int) (28 * d)));
        LinearLayout mid = new LinearLayout(getContext());
        mid.setOrientation(LinearLayout.VERTICAL);
        mid.setPadding((int) (10 * d), 0, (int) (8 * d), 0);
        TextView name = _savingsText(row.optString("groupName", "Group"), 14, true,
                R.color.colorTextPrimary);
        TextView meta = _savingsText(row.optString("statusLine", row.optString("badge", "")), 12,
                false, R.color.colorTextSecondary);
        mid.addView(name);
        mid.addView(meta);
        line.addView(mid,
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        final String gid = row.optString("groupId");
        final String action = row.optString("action", "NONE");
        TextView go = _savingsText("PAY".equals(action) ? "Pay" : "Open", 13, true,
                R.color.colorSuccess);
        go.setPadding((int) (10 * d), (int) (6 * d), (int) (10 * d), (int) (6 * d));
        line.addView(go);
        View.OnClickListener open = v -> {
            if ("PAY".equals(action))
                _openGroupPayments(gid);
            else
                _openGroup(gid);
        };
        line.setOnClickListener(open);
        go.setOnClickListener(open);
        return line;
    }

    private View _savingsInvite(java.util.Map<String, Object> inv) {
        float d = getResources().getDisplayMetrics().density;
        LinearLayout line = _savingsLine();
        ImageView icon = new ImageView(getContext());
        icon.setImageResource(R.drawable.ic_tx_group_line);
        icon.setColorFilter(
                androidx.core.content.ContextCompat.getColor(getContext(), R.color.colorSuccess));
        line.addView(icon, new LinearLayout.LayoutParams((int) (28 * d), (int) (28 * d)));
        LinearLayout mid = new LinearLayout(getContext());
        mid.setOrientation(LinearLayout.VERTICAL);
        mid.setPadding((int) (10 * d), 0, (int) (8 * d), 0);
        mid.addView(
                _savingsText(str(inv, "groupName", "Group"), 14, true, R.color.colorTextPrimary));
        mid.addView(_savingsText("Pending invitation", 12, false, R.color.colorTextSecondary));
        line.addView(mid,
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        final String id = str(inv, "_docId", "");
        final String gid = str(inv, "groupId", "");
        TextView decline = _savingsText("Decline", 12, true, R.color.colorTextSecondary);
        decline.setPadding((int) (10 * d), (int) (6 * d), (int) (10 * d), (int) (6 * d));
        decline.setBackgroundResource(R.drawable.bg_home_quiet);
        TextView accept = _savingsText("Accept", 12, true, R.color.colorTextOnAccent);
        accept.setPadding((int) (12 * d), (int) (6 * d), (int) (12 * d), (int) (6 * d));
        accept.setBackgroundResource(R.drawable.bg_home_action);
        decline.setOnClickListener(
                v -> com.harvana.app.group.InviteManager.respond(getContext(), id, false, err -> {
                    if (getActivity() == null)
                        return;
                    getActivity().runOnUiThread(() -> {
                        if (err != null)
                            android.widget.Toast
                                    .makeText(getContext(), err, android.widget.Toast.LENGTH_LONG)
                                    .show();
                        else
                            _loadHomeInvites();
                    });
                }));
        accept.setOnClickListener(v -> {
            Intent i = new Intent(getContext(), GroupJoinRequestActivity.class);
            i.putExtra("groupId", gid);
            i.putExtra("inviteId", id);
            startActivity(i);
        });
        line.addView(decline);
        LinearLayout.LayoutParams gap = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        gap.setMarginStart((int) (6 * d));
        line.addView(accept, gap);
        return line;
    }

    private View _savingsRule() {
        float d = getResources().getDisplayMetrics().density;
        View rule = new View(getContext());
        rule.setBackgroundColor(androidx.core.content.ContextCompat.getColor(getContext(),
                R.color.colorCardStroke));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, Math.round(d)));
        int inset = (int) (14 * d);
        lp.setMarginStart(inset);
        lp.setMarginEnd(inset);
        rule.setLayoutParams(lp);
        return rule;
    }

    private LinearLayout _savingsLine() {
        float d = getResources().getDisplayMetrics().density;
        LinearLayout line = new LinearLayout(getContext());
        line.setGravity(Gravity.CENTER_VERTICAL);
        line.setPadding((int) (14 * d), (int) (10 * d), (int) (10 * d), (int) (10 * d));
        line.setClickable(true);
        android.util.TypedValue out = new android.util.TypedValue();
        getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, out,
                true);
        line.setBackgroundResource(out.resourceId);
        return line;
    }

    private TextView _savingsText(String s, float sp, boolean bold, int color) {
        TextView t = new TextView(getContext());
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(androidx.core.content.ContextCompat.getColor(getContext(), color));
        if (bold)
            t.setTypeface(null, android.graphics.Typeface.BOLD);
        return t;
    }

    private static String str(Map<String, Object> m, String k, String fb) {
        Object o = m == null ? null : m.get(k);
        return o == null || String.valueOf(o).trim().isEmpty() ? fb : String.valueOf(o).trim();
    }
}
