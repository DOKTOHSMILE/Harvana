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
	private MaterialCardView card_total_saved;
	private MaterialCardView card_active_groups;
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
	private MaterialCardView card_trust_score;
	private MaterialCardView card_open_loans;
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
	public View onCreateView(@NonNull LayoutInflater _inflater, @Nullable ViewGroup _container, @Nullable Bundle _savedInstanceState) {
		View _view = _inflater.inflate(R.layout.home_fragment, _container, false);
		initialize(_savedInstanceState, _view);
		FirebaseApp.initializeApp(getContext());
		initializeLogic();
		_addQuickActions(_view);
		if (card_trust_score != null) card_trust_score.setOnClickListener(v -> startActivity(new Intent(getContext(), TrustScoreActivity.class)));
		if (card_open_loans != null) card_open_loans.setOnClickListener(v -> startActivity(new Intent(getContext(), OpenLoansActivity.class)));
		return _view;
	}
	
	private void initialize(Bundle _savedInstanceState, View _view) {
		vscroll1 = _view.findViewById(R.id.vscroll1);
		linear2 = _view.findViewById(R.id.linear2);
		linear194 = _view.findViewById(R.id.linear194);
		cardview28 = _view.findViewById(R.id.cardview28);
		linear36 = _view.findViewById(R.id.linear36);
		textview2 = _view.findViewById(R.id.textview2);
		textview3 = _view.findViewById(R.id.textview3);
		textview4 = _view.findViewById(R.id.textview4);
		linear217 = _view.findViewById(R.id.linear217);
		linear200 = _view.findViewById(R.id.linear200);
		layout_metric_grid = _view.findViewById(R.id.layout_metric_grid);
		cardview1 = _view.findViewById(R.id.cardview1);
		cardview4 = _view.findViewById(R.id.cardview4);
		cardview2 = _view.findViewById(R.id.cardview2);
		cardview3 = _view.findViewById(R.id.cardview3);
		cardview29 = _view.findViewById(R.id.cardview29);
		cardview30 = _view.findViewById(R.id.cardview30);
		cardview31 = _view.findViewById(R.id.cardview31);
		linear37 = _view.findViewById(R.id.linear37);
		linear40 = _view.findViewById(R.id.linear40);
		card_total_saved = _view.findViewById(R.id.card_total_saved);
		card_active_groups = _view.findViewById(R.id.card_active_groups);
		linear198 = _view.findViewById(R.id.linear198);
		linear38 = _view.findViewById(R.id.linear38);
		linear219 = _view.findViewById(R.id.linear219);
		imageview58 = _view.findViewById(R.id.imageview58);
		textview205 = _view.findViewById(R.id.textview205);
		text_val_saved = _view.findViewById(R.id.text_val_saved);
		text_sub_saved = _view.findViewById(R.id.text_sub_saved);
		linear196 = _view.findViewById(R.id.linear196);
		linear39 = _view.findViewById(R.id.linear39);
		linear218 = _view.findViewById(R.id.linear218);
		imageview57 = _view.findViewById(R.id.imageview57);
		textview204 = _view.findViewById(R.id.textview204);
		text_val_groups = _view.findViewById(R.id.text_val_groups);
		text_sub_groups = _view.findViewById(R.id.text_sub_groups);
		card_trust_score = _view.findViewById(R.id.card_trust_score);
		card_open_loans = _view.findViewById(R.id.card_open_loans);
		linear201 = _view.findViewById(R.id.linear201);
		linear202 = _view.findViewById(R.id.linear202);
		linear215 = _view.findViewById(R.id.linear215);
		ring_trust_score = _view.findViewById(R.id.ring_trust_score);
		text_val_trust = _view.findViewById(R.id.text_val_trust);
		textview206 = _view.findViewById(R.id.textview206);
		text_sub_trust = _view.findViewById(R.id.text_sub_trust);
		linear203 = _view.findViewById(R.id.linear203);
		linear41 = _view.findViewById(R.id.linear41);
		linear220 = _view.findViewById(R.id.linear220);
		imageview59 = _view.findViewById(R.id.imageview59);
		textview207 = _view.findViewById(R.id.textview207);
		text_val_loans = _view.findViewById(R.id.text_val_loans);
		text_sub_loans = _view.findViewById(R.id.text_sub_loans);
		linear209 = _view.findViewById(R.id.linear209);
		linear210 = _view.findViewById(R.id.linear210);
		linear_upcoming_payments = _view.findViewById(R.id.linear_upcoming_payments);
		linear7 = _view.findViewById(R.id.linear7);
		textview212 = _view.findViewById(R.id.textview212);
		textview6 = _view.findViewById(R.id.textview6);
		imageview54 = _view.findViewById(R.id.imageview54);
		textview213 = _view.findViewById(R.id.textview213);
		linear30 = _view.findViewById(R.id.linear30);
		linear42 = _view.findViewById(R.id.linear42);
		textview5 = _view.findViewById(R.id.textview5);
		linear221 = _view.findViewById(R.id.linear221);
		linear206 = _view.findViewById(R.id.linear206);
		cardview27 = _view.findViewById(R.id.cardview27);
		linear207 = _view.findViewById(R.id.linear207);
		imageview53 = _view.findViewById(R.id.imageview53);
		linear208 = _view.findViewById(R.id.linear208);
		linear225 = _view.findViewById(R.id.linear225);
		tv_local_currency_eqv = _view.findViewById(R.id.tv_local_currency_eqv);
		tv_harv_balance = _view.findViewById(R.id.tv_harv_balance);
		imageview61 = _view.findViewById(R.id.imageview61);
		// The eye: one saved show/hide switch for every balance in the app (see BalancePrivacy).
		com.harvana.app.utils.BalancePrivacy.bindEye(imageview61);
		com.harvana.app.utils.BalancePrivacy.guard(tv_harv_balance);
		com.harvana.app.utils.BalancePrivacy.guard(tv_local_currency_eqv);
		com.harvana.app.utils.BalancePrivacy.guard(text_val_saved);
		com.harvana.app.utils.BalancePrivacy.guard(text_val_loans);
		linear223 = _view.findViewById(R.id.linear223);
		cardview32 = _view.findViewById(R.id.cardview32);
		linear222 = _view.findViewById(R.id.linear222);
		textview220 = _view.findViewById(R.id.textview220);
		imageview60 = _view.findViewById(R.id.imageview60);
		home_recent_tx = _view.findViewById(R.id.home_recent_tx);
		linear211 = _view.findViewById(R.id.linear211);
		linear212 = _view.findViewById(R.id.linear212);
		textview215 = _view.findViewById(R.id.textview215);
		linear_learn_hub = _view.findViewById(R.id.linear_learn_hub);
		textview10 = _view.findViewById(R.id.textview10);
		imageview55 = _view.findViewById(R.id.imageview55);
		textview214 = _view.findViewById(R.id.textview214);
		linear213 = _view.findViewById(R.id.linear213);
		linear214 = _view.findViewById(R.id.linear214);
		linear_trending_groups = _view.findViewById(R.id.linear_trending_groups);
		linear10 = _view.findViewById(R.id.linear10);
		textview216 = _view.findViewById(R.id.textview216);
		textview12 = _view.findViewById(R.id.textview12);
		imageview56 = _view.findViewById(R.id.imageview56);
		textview217 = _view.findViewById(R.id.textview217);
		authsp = getContext().getSharedPreferences("authsp", Activity.MODE_PRIVATE);
		groupsp = getContext().getSharedPreferences("groupsp", Activity.MODE_PRIVATE);
		fbauth = FirebaseAuth.getInstance();
		
		card_active_groups.setOnClickListener(_v -> {
			if (getActivity() != null) {
				com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = 
				getActivity().findViewById(R.id.bottomnavigation1);
				
				if (bottomNav != null) {
					bottomNav.setSelectedItemId(R.id.menu_groups);
				}
			}
		});
		
		textview6.setOnClickListener(_v -> {
			if (getActivity() != null) {
				com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = 
				getActivity().findViewById(R.id.bottomnavigation1);
				
				if (bottomNav != null) {
					bottomNav.setSelectedItemId(R.id.menu_groups);
				}
			}
		});
		
		cardview32.setOnClickListener(_v -> {
			AssetFragmentActivity.pendingPage = 1;
			if (getActivity() != null) {
				com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = 
				getActivity().findViewById(R.id.bottomnavigation1);
				
				if (bottomNav != null) {
					bottomNav.setSelectedItemId(R.id.menu_asset);
				}
			}
		});
		
		textview10.setOnClickListener(_v -> {
			if (getActivity() != null) {
				com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = 
				getActivity().findViewById(R.id.bottomnavigation1);
				
				if (bottomNav != null) {
					bottomNav.setSelectedItemId(R.id.menu_learn);
				}
			}
		});
		
		textview12.setOnClickListener(_v -> {
			if (getActivity() != null) {
				com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = 
				getActivity().findViewById(R.id.bottomnavigation1);
				
				if (bottomNav != null) {
					bottomNav.setSelectedItemId(R.id.menu_discover);
				}
			}
		});
		
		
		fbauth_updateEmailListener = _param1 -> {
			final boolean _success = _param1.isSuccessful();
			final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
		};
		
		fbauth_updatePasswordListener = _param1 -> {
			final boolean _success = _param1.isSuccessful();
			final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
		};
		
		fbauth_emailVerificationSentListener = _param1 -> {
			final boolean _success = _param1.isSuccessful();
			final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
		};
		
		fbauth_deleteUserListener = _param1 -> {
			final boolean _success = _param1.isSuccessful();
			final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
		};
		
		fbauth_phoneAuthListener = task -> {
			final boolean _success = task.isSuccessful();
			final String _errorMessage = task.getException() != null ? task.getException().getMessage() : "";
		};
		
		fbauth_updateProfileListener = _param1 -> {
			final boolean _success = _param1.isSuccessful();
			final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
		};
		
		fbauth_googleSignInListener = task -> {
			final boolean _success = task.isSuccessful();
			final String _errorMessage = task.getException() != null ? task.getException().getMessage() : "";
		};
		
		_fbauth_create_user_listener = _param1 -> {
			final boolean _success = _param1.isSuccessful();
			final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
		};
		
		_fbauth_sign_in_listener = _param1 -> {
			final boolean _success = _param1.isSuccessful();
			final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
		};
		
		_fbauth_reset_password_listener = _param1 -> {
			final boolean _success = _param1.isSuccessful();
		};
	}
	
	private void initializeLogic() {
		Calendar c = Calendar.getInstance();
		if (textview2 != null)
		textview2.setText(new SimpleDateFormat("EEEE dd MMMM", Locale.getDefault()).format(c.getTime()));
		if (textview4 != null)
		textview4.setVisibility(View.GONE);                 // values only on Home: the greeting and the date
		// The last snapshot at once (no blank cards, no shimmer once Home has loaded before), then one refresh.
		com.harvana.app.home.HomeData.listen(homeListener);
		_render(com.harvana.app.home.HomeData.cached(getContext()));
		_loadWalletBalance();
	}
	
	/** MainActivity hides and shows its tabs: coming back to Home is a refresh too (one at most every few seconds). */
	@Override
	public void onHiddenChanged(boolean hidden) {
		super.onHiddenChanged(hidden);
		if (!hidden) com.harvana.app.home.HomeData.refresh(getContext(), false);
	}
	
	/** Invitations are announced once per count, not on every return to Home. */
	private static int invitesAnnounced = -1;
	
	@Override
	public void onResume() {
		super.onResume();
		com.harvana.app.home.HomeData.refresh(getContext(), false);
		_loadWalletBalance();
		_loadRecentTx();
		if (getContext() != null) {
			com.harvana.app.utils.NetworkStatusBanner.refresh(linear2, 1,
					com.harvana.app.wallet.WalletManager.getActiveWalletAddress(getContext()));
			com.harvana.app.sync.NodeLinkManager.ensureLinked(getContext(), null);
			com.harvana.app.utils.ActivityTracker.reportLiveness(getContext());
			com.harvana.app.group.InviteManager.checkAndNotify(getContext());
			final android.content.Context ic = getContext().getApplicationContext();
			com.harvana.app.group.InviteManager.listMine(ic, new com.harvana.app.group.InviteManager.ListCb() {
				@Override public void onList(java.util.List<java.util.Map<String, Object>> invites) {
					if (invites.isEmpty() || getActivity() == null || getView() == null) return;
					getActivity().runOnUiThread(() -> {
						if (getView() == null || invites.size() == invitesAnnounced) return;
						invitesAnnounced = invites.size();
						com.google.android.material.snackbar.Snackbar.make(getView(),
							"You have " + invites.size() + " group invitation" + (invites.size() > 1 ? "s" : ""),
							com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
							.setAction("View", v -> startActivity(new Intent(getContext(), InvitesActivity.class))).show();
					});
				}
				@Override public void onError(String e) { }
			});
		}
	}
	public void _MORE() {
	}
	/**
	 * Switches to one of the main tabs through the bottom bar, the same way tapping the bar does. It used to put a new
	 * copy of the tab's fragment into the container: MainActivity shows and hides its own tab fragments, so that copy
	 * was never hidden and stayed painted over every tab (Learn looked frozen while the bar still changed).
	 */
	private void _openTab(int menuId) {
		if (getActivity() == null) return;
		BottomNavigationView nav = getActivity().findViewById(R.id.bottomnavigation1);
		if (nav != null) nav.setSelectedItemId(menuId);
	}
	
	/**
	 * Home paints from one snapshot (HomeData): the last one at once when the tab opens, then the next one when every part
	 * of a refresh has answered. A list section is rebuilt only when its values changed, so nothing blanks or reshuffles.
	 */
	private final java.util.Map<String, String> shownSig = new HashMap<>();
	private final com.harvana.app.home.HomeData.Listener homeListener = s -> {
		if (isAdded() && getView() != null) _render(s);
	};
	
	private boolean _changed(String key, String sig) {
		if (sig.equals(shownSig.get(key))) return false;
		shownSig.put(key, sig);
		return true;
	}
	
	private void _render(org.json.JSONObject s) {
		if (getContext() == null) return;
		boolean signedIn = FirebaseAuth.getInstance().getCurrentUser() != null;
		String first = s.optString("name", "");
		if (textview3 != null) textview3.setText(_greeting() + (first.isEmpty() ? "" : ", " + first));
		
		org.json.JSONArray ids = s.optJSONArray("groupIds");
		org.json.JSONObject groups = s.optJSONObject("groups");
		int active = groups != null ? groups.optInt("active") : ids != null ? ids.length() : -1;
		if (text_val_groups != null) text_val_groups.setText(active < 0 ? "—" : String.valueOf(active));
		if (text_sub_groups != null) text_sub_groups.setText(active < 0 ? "" : active == 0 ? "Join your first circle" : active == 1 ? "1 active circle" : active + " active circles");
		
		org.json.JSONObject saved = s.optJSONObject("saved");
		if (text_val_saved != null) text_val_saved.setText(saved == null ? "—" : saved.optString("text", "0"));
		int across = saved == null ? 0 : saved.optInt("across");
		if (text_sub_saved != null) text_sub_saved.setText(saved == null ? "" : across <= 0 ? "No group savings yet" : "Across " + across + (across == 1 ? " group" : " groups"));
		_openTotalSavedOnTap();
		
		org.json.JSONObject trust = s.optJSONObject("trust");
		if (text_val_trust != null) text_val_trust.setText(trust == null ? "—" : trust.optString("value", "—"));
		if (ring_trust_score != null) {
			ring_trust_score.setMax(trust == null ? 100 : Math.max(1, trust.optInt("max", 100)));
			ring_trust_score.setProgress(trust == null ? 0 : trust.optInt("progress"));
		}
		if (text_sub_trust != null) text_sub_trust.setText(trust == null ? "" : trust.optString("sub", ""));
		
		org.json.JSONObject nl = s.optJSONObject("nodeLoans"), fl = s.optJSONObject("fsLoans");
		if (text_val_loans != null) {
			if (nl == null && fl == null) { text_val_loans.setText("—"); if (text_sub_loans != null) text_sub_loans.setText(""); }
			else {
				int count = (nl == null ? 0 : nl.optInt("count")) + (fl == null ? 0 : fl.optInt("count"));
				double out = (nl == null ? 0 : nl.optDouble("out", 0)) + (fl == null ? 0 : fl.optDouble("out", 0));
				text_val_loans.setText(String.valueOf(count));
				if (text_sub_loans != null) text_sub_loans.setText(count == 0 ? "No open loans" : _fmtHarv(out) + " outstanding");
			}
		}
		
		_renderUpcoming(s.optJSONObject("upcoming"), signedIn);
		org.json.JSONArray pending = s.optJSONArray("pending");
		trendingPending.clear();
		if (pending != null) for (int i = 0; i < pending.length(); i++) trendingPending.add(pending.optString(i));
		_renderTrending(s.optJSONObject("trending"), signedIn, String.valueOf(pending));
		_renderLearn(s.optJSONObject("learn"), signedIn);
	}
	
	/** A placeholder only while a section has never loaded; padded like the rows it stands for. */
	private void _placeholder(LinearLayout box, View empty) {
		if (box.getChildCount() > 0) return;
		View rows = com.harvana.app.utils.Shimmer.rows(getContext(), 2);
		int pad = (int) (20 * getResources().getDisplayMetrics().density);
		rows.setPadding(pad, 0, pad, pad / 2);
		box.addView(rows);
		if (empty != null) empty.setVisibility(View.GONE);
	}
	
	private void _renderUpcoming(org.json.JSONObject section, boolean signedIn) {
		if (linear_upcoming_payments == null) return;
		if (section == null && signedIn) { _placeholder(linear_upcoming_payments, linear7); return; }
		org.json.JSONArray rows = section == null ? new org.json.JSONArray() : section.optJSONArray("rows");
		if (rows == null) rows = new org.json.JSONArray();
		if (!_changed("upcoming", rows.toString())) return;
		linear_upcoming_payments.removeAllViews();
		if (linear7 != null) linear7.setVisibility(rows.length() == 0 ? View.VISIBLE : View.GONE);
		LayoutInflater inf = LayoutInflater.from(getContext());
		for (int i = 0; i < rows.length(); i++) {
			final org.json.JSONObject row = rows.optJSONObject(i);
			if (row == null) continue;
			View item = inf.inflate(R.layout.home_upcoming_pay_custom_display, linear_upcoming_payments, false);
			TextView name = item.findViewById(R.id.textview2);
			TextView meta = item.findViewById(R.id.textview3);
			TextView statusLine = item.findViewById(R.id.textview5);
			TextView badge = item.findViewById(R.id.textview4);
			com.google.android.material.card.MaterialCardView badgeCard = item.findViewById(R.id.cardview3);
			com.google.android.material.button.MaterialButton button = item.findViewById(R.id.materialbutton1);
			final String gid = row.optString("groupId");
			final String action = row.optString("action", "NONE");
			final String otherUid = row.optString("otherUid");
			final String who = row.optString("otherName", "a member");
			final String amountText = com.harvana.app.wallet.BalanceFormatter.smart(row.optDouble("amount", 0));
			String freq = row.optString("frequency");
			
			ImageView face = item.findViewById(R.id.imageview1);
			if (face != null) com.harvana.app.utils.GroupRing.show(face, row.optString("groupType"), row.optString("groupPhoto"), R.drawable.ic_coin_harv);
			if (name != null) name.setText(row.optString("groupName", "Group"));
			if (meta != null) meta.setText(amountText + (freq.isEmpty() ? "" : " · " + freq));
			if (statusLine != null) statusLine.setText(row.optString("statusLine"));
			
			int badgeColor = "PAY".equals(action) ? R.color.colorWarning : "REMIND".equals(action) ? R.color.colorInfo : R.color.colorTextTertiary;
			int c = androidx.core.content.ContextCompat.getColor(requireContext(), badgeColor);
			if (badge != null) { badge.setText(row.optString("badge")); badge.setTextColor(c); }
			if (badgeCard != null) badgeCard.setStrokeColor(c);
			item.setOnClickListener(v -> _openGroup(gid));
			
			if (button != null) {
				if ("PAY".equals(action)) {
					button.setText("Pay");
					button.setOnClickListener(v -> _openGroupPayments(gid));
				} else if ("REMIND".equals(action) && !otherUid.isEmpty()) {
					button.setText("Remind");
					button.setOnClickListener(v -> {
						button.setEnabled(false);
						com.harvana.app.group.GroupReminders.remind(getContext(), gid, otherUid, who, amountText,
						new com.harvana.app.group.GroupReminders.Callback() {
							@Override public void onSent() {
								if (!isAdded()) return;
								button.setText("Reminded");
								android.widget.Toast.makeText(getContext(), "Reminder sent to " + who, android.widget.Toast.LENGTH_SHORT).show();
							}
							@Override public void onError(String error) {
								if (!isAdded()) return;
								button.setEnabled(true);
								android.widget.Toast.makeText(getContext(), error, android.widget.Toast.LENGTH_LONG).show();
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
		if (linear_trending_groups == null) return;
		if (section == null && signedIn) { _placeholder(linear_trending_groups, linear10); return; }
		org.json.JSONArray rows = section == null ? new org.json.JSONArray() : section.optJSONArray("rows");
		if (rows == null) rows = new org.json.JSONArray();
		if (!_changed("trending", rows + "|" + pendingSig)) return;
		linear_trending_groups.removeAllViews();
		if (linear10 != null) linear10.setVisibility(rows.length() == 0 ? View.VISIBLE : View.GONE);
		LayoutInflater inf = LayoutInflater.from(getContext());
		for (int i = 0; i < rows.length(); i++) {
			org.json.JSONObject g = rows.optJSONObject(i);
			if (g == null) continue;
			View item = inf.inflate(R.layout.home_trending_custom_display, linear_trending_groups, false);
			TextView name = item.findViewById(R.id.textview1);
			TextView verified = item.findViewById(R.id.textview2);
			TextView meta = item.findViewById(R.id.textview3);
			TextView trust = item.findViewById(R.id.textview4);
			TextView join = item.findViewById(R.id.btn_join);
			final String gid = g.optString("groupId");
			if (name != null) name.setText(g.optString("name"));
			ImageView face = item.findViewById(R.id.imageview1);
			if (face != null) com.harvana.app.utils.GroupRing.show(face, g.optString("type"), g.optString("photo"), R.drawable.ic_coin_harv);
			if (meta != null) meta.setText(g.optString("meta"));
			if (trust != null) trust.setText(g.optString("trust"));
			if (verified != null) {
				String b = g.optString("badge");
				verified.setText(b);
				verified.setVisibility(b.isEmpty() ? View.GONE : View.VISIBLE);
			}
			View.OnClickListener open = v -> {
				if (gid.isEmpty()) { _openTab(R.id.menu_discover); return; }
				Intent ji = new Intent(getContext(), GroupJoinRequestActivity.class);
				ji.putExtra("groupId", gid);
				startActivity(ji);
			};
			if (join != null) { _styleTrendingButton(join, gid); join.setOnClickListener(open); }
			item.setOnClickListener(open);
			linear_trending_groups.addView(item);
		}
	}
	
	private void _renderLearn(org.json.JSONObject section, boolean signedIn) {
		if (linear_learn_hub == null) return;
		if (section == null && signedIn) { _placeholder(linear_learn_hub, null); return; }
		org.json.JSONArray rows = section == null ? new org.json.JSONArray() : section.optJSONArray("rows");
		if (rows == null) rows = new org.json.JSONArray();
		if (!_changed("learn", rows.toString())) return;
		linear_learn_hub.removeAllViews();
		if (rows.length() == 0) { _showLearnHubEmpty("No Learn Hub posts yet"); return; }
		LayoutInflater inf = LayoutInflater.from(getContext());
		for (int i = 0; i < rows.length(); i++) {
			org.json.JSONObject m = rows.optJSONObject(i);
			if (m == null) continue;
			View item = inf.inflate(R.layout.home_learnhub_custom_display, linear_learn_hub, false);
			TextView title = item.findViewById(R.id.title_text);
			TextView cat = item.findViewById(R.id.category_text);
			ImageView img = item.findViewById(R.id.imageview1);
			long views = m.optLong("views"), at = m.optLong("at");
			if (title != null) title.setText(m.optString("title"));
			if (cat != null) {
				String when = at > 0 ? " · " + android.text.format.DateUtils.getRelativeTimeSpanString(at, System.currentTimeMillis(), android.text.format.DateUtils.MINUTE_IN_MILLIS) : "";
				cat.setText(m.optString("category") + " · " + views + (views == 1 ? " view" : " views") + when);
			}
			String image = m.optString("image");
			if (img != null && !image.isEmpty()) com.bumptech.glide.Glide.with(this).load(image).centerCrop().into(img);
			item.setOnClickListener(v -> _openTab(R.id.menu_learn));
			linear_learn_hub.addView(item);
		}
	}
	
	private void _showLearnHubEmpty(String message) {
		if (linear_learn_hub == null || getContext() == null) return;
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
		empty.setTextColor(androidx.core.content.ContextCompat.getColor(getContext(), R.color.colorTextTertiary));
		LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		tp.topMargin = (int) (8 * d);
		box.addView(empty, tp);
		linear_learn_hub.addView(box);
	}

	/** The two newest movements on this wallet, under the balance. View more opens Asset → Activity. */
	private String recentSig = "";

	private void _loadRecentTx() {
		if (home_recent_tx == null || getContext() == null) return;
		String addr = WalletManager.getActiveWalletAddress(getContext());
		if (addr == null || addr.isEmpty()) { _paintRecent(java.util.Collections.emptyList(), ""); return; }
		final String wallet = addr;
		com.harvana.app.sync.HarvanaNodeClient.getHistory(wallet, 0, 2, new com.harvana.app.sync.HarvanaNodeClient.JsonCallback() {
			@Override public void onSuccess(org.json.JSONObject json) {
				java.util.List<java.util.HashMap<String, Object>> rows = com.harvana.app.wallet.ActivityFeed.parseNodePage(json);
				if (rows.size() > 2) rows = rows.subList(0, 2);
				final java.util.List<java.util.HashMap<String, Object>> show = rows;
				if (getActivity() == null) return;
				getActivity().runOnUiThread(() -> { if (isAdded()) _paintRecent(show, wallet); });
			}
			@Override public void onError(String error) {
				if (getActivity() == null) return;
				getActivity().runOnUiThread(() -> { if (isAdded()) _paintRecent(java.util.Collections.emptyList(), wallet); });
			}
		});
	}

	private void _paintRecent(java.util.List<java.util.HashMap<String, Object>> rows, String wallet) {
		if (home_recent_tx == null || getContext() == null) return;
		StringBuilder sig = new StringBuilder(wallet == null ? "" : wallet);
		for (java.util.HashMap<String, Object> m : rows) sig.append('|').append(m.get("txHash")).append(m.get("status")).append(m.get("value"));
		if (sig.toString().equals(recentSig) && home_recent_tx.getChildCount() > 0) return;
		recentSig = sig.toString();
		home_recent_tx.removeAllViews();
		if (rows.isEmpty()) {
			TextView empty = new TextView(getContext());
			empty.setText("No transactions yet");
			empty.setTextSize(13f);
			empty.setTextColor(androidx.core.content.ContextCompat.getColor(getContext(), R.color.colorTextTertiary));
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
			home_recent_tx.addView(row);
		}
	}

	private void _bindRecent(View row, java.util.HashMap<String, Object> item, String wallet, boolean last) {
		String sender = item.get("senderAddress") == null ? "" : String.valueOf(item.get("senderAddress"));
		String recipient = item.get("recipientAddress") == null ? "" : String.valueOf(item.get("recipientAddress"));
		String asset = item.get("tokenSymbol") == null ? com.harvana.app.CoinInfo.ticker() : String.valueOf(item.get("tokenSymbol")).toUpperCase(java.util.Locale.US);
		if (asset.isEmpty() || "NULL".equals(asset)) asset = com.harvana.app.CoinInfo.ticker();
		if (com.harvana.app.husd.Stable.isStable(asset)) asset = com.harvana.app.husd.Stable.symbol(asset);
		double amount = item.get("value") instanceof Number ? ((Number) item.get("value")).doubleValue() : 0;
		String status = item.get("status") == null ? "confirmed" : String.valueOf(item.get("status"));
		boolean pending = "pending".equalsIgnoreCase(status);
		boolean failed = "failed".equalsIgnoreCase(status);
		boolean incoming = recipient.equalsIgnoreCase(wallet);
		String title = failed ? "Failed " + asset : (pending ? (incoming ? "Receiving " : "Sending ") + asset + "…" : (incoming ? "Received " : "Sent ") + asset);
		String memo = item.get("memo") == null ? "" : String.valueOf(item.get("memo"));
		com.harvana.app.tx.TxKind kind = failed ? null : com.harvana.app.tx.TxKind.of(memo, incoming);
		if (kind != null) title = pending ? kind.title + "…" : kind.title;
		TextView type = row.findViewById(R.id.tv_tx_type);
		TextView addr = row.findViewById(R.id.tv_tx_address);
		TextView amt = row.findViewById(R.id.tv_tx_amount);
		TextView date = row.findViewById(R.id.tv_tx_date);
		ImageView icon = row.findViewById(R.id.iv_tx_icon);
		View divider = row.findViewById(R.id.tx_divider);
		if (type != null) type.setText(title);
		String who = incoming ? sender : recipient;
		String shortWho = who.length() <= 16 ? (who.isEmpty() ? "Unknown" : who) : who.substring(0, 10) + "…" + who.substring(who.length() - 4);
		if (addr != null) addr.setText((incoming ? "From: " : "To: ") + shortWho);
		int color = androidx.core.content.ContextCompat.getColor(getContext(),
			failed ? R.color.colorError : pending ? R.color.colorWarning : incoming ? R.color.colorAccent : R.color.colorError);
		if (amt != null) {
			amt.setText((failed ? "✕ " : incoming ? "+ " : "- ") + BalanceFormatter.smartDecimal(amount) + " " + asset);
			amt.setTextColor(color);
		}
		long ts = item.get("timestamp") instanceof Number ? ((Number) item.get("timestamp")).longValue() : 0L;
		long initiated = item.get("initiatedAt") instanceof Number ? ((Number) item.get("initiatedAt")).longValue() : 0L;
		if (date != null) date.setText(WalletActivityFragmentActivity._activityWhen(pending, initiated, ts));
		if (icon != null) {
			icon.setImageResource(failed || !incoming ? R.drawable.ic_action_send : R.drawable.ic_action_receive);
			if (kind != null && kind.icon != 0) icon.setImageResource(kind.icon);
			icon.setColorFilter(color);
		}
		if (divider != null) divider.setVisibility(last ? View.GONE : View.VISIBLE);
		View hit = row.findViewById(R.id.cardview1);
		if (hit != null) hit.setOnClickListener(v -> {
			Intent intent = new Intent(v.getContext(), ReceiptActivity.class);
			intent.putExtra("txHash", String.valueOf(item.get("txHash")));
			intent.putExtra("status", status);
			intent.putExtra("senderAddress", sender);
			intent.putExtra("recipientAddress", recipient);
			intent.putExtra("tokenSymbol", asset);
			intent.putExtra("value", amount);
			intent.putExtra("timestamp", ts);
			intent.putExtra("initiatedAt", initiated);
			intent.putExtra("memo", memo);
			if (item.get("harvFee") instanceof Number) intent.putExtra("harvFee", ((Number) item.get("harvFee")).doubleValue());
			if (item.get("feeSymbol") != null) intent.putExtra("feeSymbol", String.valueOf(item.get("feeSymbol")));
			if (item.get("blockNumber") instanceof Number) intent.putExtra("blockNumber", ((Number) item.get("blockNumber")).longValue());
			if (item.get("nonce") instanceof Number) intent.putExtra("nonce", ((Number) item.get("nonce")).longValue());
			v.getContext().startActivity(intent);
		});
	}
	
	/** Groups I have asked to join and am waiting on: their button says "Pending" instead of "Join". */
	private final java.util.Set<String> trendingPending = new java.util.HashSet<>();
	
	private void _styleTrendingButton(TextView join, String gid) {
		boolean pending = trendingPending.contains(gid);
		join.setTag(gid);
		join.setText(pending ? "Pending" : "Join");
		join.setSingleLine(true);
		join.setMaxLines(1);
		join.setAlpha(pending ? 0.65f : 1f);
	}
	
	/**
	 * The wallet card shows one number: everything the wallet holds, in the chosen currency, across every coin (WalletTotal).
	 */
	private final com.harvana.app.wallet.WalletTotal.Listener totalListener = (address, symbol, fiat) -> {
		if (!isAdded() || getContext() == null) return;
		String addr = WalletManager.getActiveWalletAddress(getContext());
		if (addr != null && addr.equals(address)) _showTotal(symbol, fiat);
	};
	
	private void _showTotal(String symbol, double fiat) {
		if (tv_harv_balance != null) {
			tv_harv_balance.setText(Double.isNaN(fiat) ? symbol + " —" : com.harvana.app.wallet.FxRates.format(symbol, fiat));
		}
		if (tv_local_currency_eqv != null) {
			String code = com.harvana.app.wallet.FiatValueHelper.symbolToCurrencyCode(symbol);
			tv_local_currency_eqv.setText("Total balance · " + code + " · all coins");
		}
	}
	
	private void _loadWalletBalance() {
		if (getContext() == null) return;
		String addr = WalletManager.getActiveWalletAddress(getContext());
		String symbol = com.harvana.app.wallet.FxRates.symbol(getContext());
		if (addr == null || addr.isEmpty()) { _showTotal(symbol, 0); return; }
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
		if (gid == null || gid.isEmpty()) return;
		intent.setClass(getContext().getApplicationContext(), GroupDetailsActivity.class);
		intent.putExtra("groupId", gid);
		intent.putExtra("tab", 2);
		startActivity(intent);
	}
	
	private void _openGroup(String gid) {
		if (gid == null || gid.isEmpty()) return;
		intent.setClass(getContext().getApplicationContext(), GroupDetailsActivity.class);
		intent.removeExtra("tab");
		intent.putExtra("groupId", gid);
		startActivity(intent);
	}
	
	/** The Total saved card opens the Total saved screen. */
	private void _openTotalSavedOnTap() {
		View v = text_val_saved;
		while (v != null && !(v instanceof com.google.android.material.card.MaterialCardView) && v.getParent() instanceof View) v = (View) v.getParent();
		View card = v instanceof com.google.android.material.card.MaterialCardView ? v : text_val_saved;
		if (card == null || card.hasOnClickListeners()) return;
		card.setOnClickListener(x -> startActivity(new android.content.Intent(getContext(), TotalSavedActivity.class)));
	}
	
	private static String _fmtAmount(double v) {
		try {
			java.text.DecimalFormat df = new java.text.DecimalFormat("#,##0.00######", new java.text.DecimalFormatSymbols(java.util.Locale.US));
			return df.format(v);
		} catch (Exception e) {
			return String.valueOf(v);
		}
	}

	private static String _fmtHarv(double v) {
		try {
			java.text.DecimalFormat df = new java.text.DecimalFormat("0.########", new java.text.DecimalFormatSymbols(java.util.Locale.US));
			return df.format(v) + (" " + com.harvana.app.CoinInfo.ticker());
		} catch (Exception e) {
			return String.valueOf(v) + (" " + com.harvana.app.CoinInfo.ticker());
		}
	}
	
	private String _greeting() {
		int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
		if (h < 12) return "Good morning";
		if (h < 17) return "Good afternoon";
		return "Good evening";
	}
	
	private static String str(Map<String, Object> m, String k, String d) {
		Object v = m.get(k);
		if (v == null) return d;
		String s = v.toString().trim();
		return s.isEmpty() || "null".equalsIgnoreCase(s) ? d : s;
	}
	
	private static double num(Map<String, Object> m, String k) {
		Object v = m.get(k);
		if (v instanceof Number) return ((Number) v).doubleValue();
		if (v != null) {
			try { return Double.parseDouble(v.toString()); } catch (Exception ignored) {}
		}
		return 0;
	}
	
	{
	}
	
	private void _addQuickActions(View root) {
		try {
			final MaterialCardView card = root.findViewById(R.id.cardview4);
			final View top = root.findViewById(R.id.linear30);
			if (card == null || top == null || getContext() == null) return;
			final float d = getResources().getDisplayMetrics().density;
			// Money first: the wallet card (balance with Send, Receive and P2P) sits right under the greeting, above the
			// group numbers, the way people expect a finance app to open.
			final View grid = root.findViewById(R.id.layout_metric_grid);
			if (grid != null && card.getParent() == grid.getParent() && card.getParent() instanceof android.view.ViewGroup) {
				android.view.ViewGroup parent = (android.view.ViewGroup) card.getParent();
				if (parent.indexOfChild(card) > parent.indexOfChild(grid)) {
					parent.removeView(card);
					parent.addView(card, parent.indexOfChild(grid));
					if (card.getLayoutParams() instanceof android.view.ViewGroup.MarginLayoutParams) {
						android.view.ViewGroup.MarginLayoutParams mlp = (android.view.ViewGroup.MarginLayoutParams) card.getLayoutParams();
						mlp.topMargin = (int) (8 * d);
						mlp.bottomMargin = (int) (14 * d);
						card.setLayoutParams(mlp);
					}
				}
			}
			
			android.widget.LinearLayout row = new android.widget.LinearLayout(getContext());
			row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
			int hPad = getResources().getConfiguration().screenWidthDp < 360 ? (int) (6 * d) : (int) (10 * d);
			row.setPadding(hPad, 0, hPad, (int) (10 * d));
			row.addView(_quickAction(R.drawable.ic_action_send, "Send", SendAssetActivity.class), _quickActionParams(d, false));
			row.addView(_quickAction(R.drawable.ic_action_receive, "Receive", ReceiveActivity.class), _quickActionParams(d, true));
			row.addView(_quickAction(R.drawable.ic_action_swap, "Swap", com.harvana.app.swap.SwapActivity.class), _quickActionParams(d, true));
			row.addView(_quickAction(R.drawable.ic_action_buy, "P2P", P2pMarketplaceActivity.class), _quickActionParams(d, true));
			
			card.removeView(top);
			android.widget.LinearLayout box = new android.widget.LinearLayout(getContext());
			box.setOrientation(android.widget.LinearLayout.VERTICAL);
			box.addView(top);
			box.addView(row);
			card.addView(box, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
			// A multi-coin wallet: no single coin's logo on the total.
			View coin = root.findViewById(R.id.cardview27);
			if (coin != null) coin.setVisibility(View.GONE);
			_orderByPriority(root);
		} catch (Exception e) {
			android.util.Log.w("HomeFragment", "Quick actions not added", e);
		}
	}
	
	/**
	 * Home in order of what people come for: money (the wallet card), what needs doing now (upcoming payments),
	 * how their savings stand (the numbers), then finding groups, then Learn Hub.
	 */
	private void _orderByPriority(View root) {
		int[] order = {R.id.cardview4, R.id.cardview1, R.id.layout_metric_grid, R.id.cardview3, R.id.cardview2};
		View first = root.findViewById(order[0]);
		if (first == null || !(first.getParent() instanceof ViewGroup)) return;
		ViewGroup parent = (ViewGroup) first.getParent();
		int at = parent.indexOfChild(first);
		for (int id : order) {
			View v = root.findViewById(id);
			if (v == null || v.getParent() != parent) continue;
			parent.removeView(v);
			parent.addView(v, Math.min(at, parent.getChildCount()));
			at = parent.indexOfChild(v) + 1;
		}
		_alignSections(root, order);
	}

	/**
	 * One spacing for every Home section: the same gap above each, and the four small cards' outer edges in line
	 * with the full-width cards (no extra inset), with an even gap between them.
	 */
	private void _alignSections(View root, int[] order) {
		float d = getResources().getDisplayMetrics().density;
		int gap = Math.round(16 * d), inner = Math.round(6 * d);
		View first = root.findViewById(order[0]);
		if (first != null && first.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
			ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) first.getLayoutParams();
			lp.bottomMargin = 0;                                              // the gap comes from the next section only
			first.setLayoutParams(lp);
		}
		for (int i = 1; i < order.length; i++) {
			View v = root.findViewById(order[i]);
			if (v == null || !(v.getLayoutParams() instanceof ViewGroup.MarginLayoutParams)) continue;
			ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
			lp.topMargin = gap;
			lp.leftMargin = 0;
			lp.rightMargin = 0;
			v.setLayoutParams(lp);
		}
		int[][] rows = {{R.id.card_total_saved, R.id.card_active_groups}, {R.id.card_trust_score, R.id.card_open_loans}};
		for (int r = 0; r < rows.length; r++) {
			for (int k = 0; k < 2; k++) {
				View card = root.findViewById(rows[r][k]);
				if (card == null || !(card.getLayoutParams() instanceof ViewGroup.MarginLayoutParams)) continue;
				ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) card.getLayoutParams();
				lp.setMargins(k == 0 ? 0 : inner, r == 0 ? 0 : inner * 2, k == 0 ? inner : 0, 0);
				lp.setMarginStart(k == 0 ? 0 : inner);
				lp.setMarginEnd(k == 0 ? inner : 0);
				card.setLayoutParams(lp);
			}
		}
	}

	private android.widget.LinearLayout.LayoutParams _quickActionParams(float d, boolean leftGap) {
		android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
		int screenWidthDp = getResources().getConfiguration().screenWidthDp;
		int margin = screenWidthDp < 360 ? (int) (4 * d) : (screenWidthDp < 400 ? (int) (6 * d) : (int) (8 * d));
		if (leftGap) lp.leftMargin = margin;
		return lp;
	}
	
	/** One of Home's money buttons: icon beside the name, 40dp, like the Assets screen's buttons. */
	private View _quickAction(int iconRes, String label, final Class<?> target) {
		final float d = getResources().getDisplayMetrics().density;
		android.widget.LinearLayout cell = new android.widget.LinearLayout(getContext());
		cell.setOrientation(android.widget.LinearLayout.HORIZONTAL);
		cell.setGravity(android.view.Gravity.CENTER);
		cell.setPadding((int) (4 * d), (int) (9 * d), (int) (4 * d), (int) (9 * d));
		cell.setBackgroundResource(R.drawable.bg_quick_action_card);
		cell.setClickable(true);
		cell.setFocusable(true);
		cell.setMinimumHeight((int) (40 * d));

		android.widget.ImageView icon = new android.widget.ImageView(getContext());
		icon.setImageResource(iconRes);
		icon.setColorFilter(androidx.core.content.ContextCompat.getColor(getContext(), R.color.colorAccent));
		cell.addView(icon, new android.widget.LinearLayout.LayoutParams((int) (17 * d), (int) (17 * d)));

		int screenWidthDp = getResources().getConfiguration().screenWidthDp;
		TextView tv = new TextView(getContext());
		tv.setText(label);
		tv.setTextSize(screenWidthDp < 360 ? 11f : 12f);
		tv.setMaxLines(1);
		tv.setEllipsize(android.text.TextUtils.TruncateAt.END);
		tv.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
		tv.setTextColor(androidx.core.content.ContextCompat.getColor(getContext(), R.color.colorTextPrimary));
		android.widget.LinearLayout.LayoutParams tlp = new android.widget.LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		tlp.leftMargin = screenWidthDp < 360 ? (int) (3 * d) : (int) (6 * d);
		cell.addView(tv, tlp);

		cell.setContentDescription(label);
		cell.setOnClickListener(v -> startActivity(new Intent(getContext(), target)));
		return cell;
	}
}
