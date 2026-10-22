package com.harvana.app;

import android.animation.*;
import android.app.*;
import android.content.*;
import android.content.Intent;
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
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.*;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.*;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import com.budiyev.android.codescanner.*;
import com.bumptech.glide.*;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.card.*;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.*;
import org.json.*;

public class LearnFragmentActivity extends Fragment {
	
	private FloatingActionButton _fab;
	
	private ArrayList<HashMap<String, Object>> listMap = new ArrayList<>();
	
	private NestedScrollView vscroll8;
	private LinearLayout linear1;
	private TextView textview1;
	private TextView textview2;
	private RecyclerView recyclerview1;
	private MaterialCardView cardview14;
	private RecyclerView recyclerview2;
	private EditText et_search_learn;
	private String learnQuery = "";
	private int learnShownCount;
	
	private Intent i = new Intent();
	private FirebaseAuth fauth;
	private OnCompleteListener<AuthResult> _fauth_create_user_listener;
	private OnCompleteListener<AuthResult> _fauth_sign_in_listener;
	private OnCompleteListener<Void> _fauth_reset_password_listener;
	private OnCompleteListener<Void> fauth_updateEmailListener;
	private OnCompleteListener<Void> fauth_updatePasswordListener;
	private OnCompleteListener<Void> fauth_emailVerificationSentListener;
	private OnCompleteListener<Void> fauth_deleteUserListener;
	private OnCompleteListener<Void> fauth_updateProfileListener;
	private OnCompleteListener<AuthResult> fauth_phoneAuthListener;
	private OnCompleteListener<AuthResult> fauth_googleSignInListener;
	
	@NonNull
	@Override
	public View onCreateView(@NonNull LayoutInflater _inflater, @Nullable ViewGroup _container, @Nullable Bundle _savedInstanceState) {
		View _view = _inflater.inflate(R.layout.learn_fragment, _container, false);
		initialize(_savedInstanceState, _view);
		FirebaseApp.initializeApp(getContext());
		initializeLogic();
		return _view;
	}
	
	private void initialize(Bundle _savedInstanceState, View _view) {
		_fab = _view.findViewById(R.id._fab);
		View _myActivity = _view.findViewById(R.id.tv_my_activity);
		// My Activity is no longer shown to members: the app counts usage in the background for the habit badges.
		if (_myActivity != null) _myActivity.setVisibility(View.GONE);
		vscroll8 = _view.findViewById(R.id.vscroll8);
		linear1 = _view.findViewById(R.id.linear1);
		textview1 = _view.findViewById(R.id.textview1);
		textview2 = _view.findViewById(R.id.textview2);
		recyclerview1 = _view.findViewById(R.id.recyclerview1);
		cardview14 = _view.findViewById(R.id.cardview14);
		recyclerview2 = _view.findViewById(R.id.recyclerview2);
		et_search_learn = _view.findViewById(R.id.et_search_learn);
		if (et_search_learn != null) {
			et_search_learn.setFocusable(false);
			et_search_learn.setCursorVisible(false);
			et_search_learn.setOnClickListener(_v -> _openLearnSearch());
		}
		com.harvana.app.utils.Shimmer.hold(recyclerview2, com.harvana.app.utils.Shimmer.cards(recyclerview2.getContext(), 3), 4000, "LearnFragmentActivity");
		fauth = FirebaseAuth.getInstance();
		
		_fab.setOnClickListener(_v -> {
			i.setClass(getContext().getApplicationContext(), CreateLearnhubContentActivity.class);
			startActivity(i);
		});
		
		fauth_updateEmailListener = _param1 -> {
			final boolean _success = _param1.isSuccessful();
			final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
			
		};
		
		fauth_updatePasswordListener = _param1 -> {
			final boolean _success = _param1.isSuccessful();
			final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
			
		};
		
		fauth_emailVerificationSentListener = _param1 -> {
			final boolean _success = _param1.isSuccessful();
			final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
			
		};
		
		fauth_deleteUserListener = _param1 -> {
			final boolean _success = _param1.isSuccessful();
			final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
			
		};
		
		fauth_phoneAuthListener = task -> {
			final boolean _success = task.isSuccessful();
			final String _errorMessage = task.getException() != null ? task.getException().getMessage() : "";
			
		};
		
		fauth_updateProfileListener = _param1 -> {
			final boolean _success = _param1.isSuccessful();
			final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
			
		};
		
		fauth_googleSignInListener = task -> {
			final boolean _success = task.isSuccessful();
			final String _errorMessage = task.getException() != null ? task.getException().getMessage() : "";
			
		};
		
		_fauth_create_user_listener = _param1 -> {
			final boolean _success = _param1.isSuccessful();
			final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
			
		};
		
		_fauth_sign_in_listener = _param1 -> {
			final boolean _success = _param1.isSuccessful();
			final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
			
		};
		
		_fauth_reset_password_listener = _param1 -> {
			final boolean _success = _param1.isSuccessful();
			
		};
	}
	
	private com.harvana.app.learn.AdCarousel ads;

	private void initializeLogic() {
		// Adverts sit above the category chips; hidden while none is running.
		if (getActivity() != null && linear1 != null) {
			View chips = linear1.findViewById(R.id.recyclerview1);
			ads = new com.harvana.app.learn.AdCarousel(getActivity(), linear1, chips == null ? 2 : linear1.indexOfChild(chips));
		}
		_initAdapters();
		_loadContent();
	}
	
	public void _MORE() {
	}
	private static final String FOR_YOU = "For you", LATEST = "Latest";
	private String selectedCategory = FOR_YOU;
	
	private static final String[] CATEGORIES = {
		FOR_YOU, LATEST, "Community Knowledge", "Finance Trends", "Investment Insights",
		"Group Savings", "Crypto & Blockchain"
	};
	
	// ── The feed ─────────────────────────────────────────────────────
	// The order is worked out once, when the feed loads or the reader changes the filter. After that Firestore may report changes
	// all day (every view of a post bumps its counter), but the list on screen keeps its order: posts that arrive later wait behind
	// a "new posts" button, and only a real change to a post's text or picture redraws that one card.
	private final HashMap<String, HashMap<String, Object>> byId = new HashMap<>();
	private final ArrayList<String> order = new ArrayList<>();
	private final ArrayList<String> waiting = new ArrayList<>();
	private final long seed = System.nanoTime();
	private LearnContentAdapter adapter;
	private TextView newPill;
	
	private static String idOf(Map<String, Object> m) { Object o = m.get("_docId"); return o == null ? "" : String.valueOf(o); }
	private static long timeOf(Map<String, Object> m) { Object o = m.get("createdAt"); return o instanceof Number ? ((Number) o).longValue() : 0L; }
	private static String strOf(Map<String, Object> m, String k) { Object o = m.get(k); return o == null ? "" : String.valueOf(o); }
	
	private List<HashMap<String, Object>> lastPosts;
	private java.util.Set<String> followingUids;
	private java.util.Set<String> blockedUids;
	
	/** Posts the author set to Followers or Only me are hidden from everyone else (the author always sees their own). */
	private List<HashMap<String, Object>> visibleTo(List<HashMap<String, Object>> posts) {
		if (followingUids == null) {
			followingUids = new java.util.HashSet<>();
			com.harvana.app.social.SocialStore.following((people, err) -> {
				if (err != null || people == null || getActivity() == null) { followingUids = null; return; }
				for (com.harvana.app.social.Person pr : people) followingUids.add(pr.uid);
				if (lastPosts != null) getActivity().runOnUiThread(() -> _onData(lastPosts, false));
			});
		}
		if (blockedUids == null) {
			blockedUids = new java.util.HashSet<>();
			com.harvana.app.social.SocialStore.blockedIds((ids, err) -> {
				if (err != null || ids == null || getActivity() == null) { blockedUids = null; return; }
				blockedUids = new java.util.HashSet<>(ids);
				if (!blockedUids.isEmpty() && lastPosts != null) getActivity().runOnUiThread(() -> _onData(lastPosts, false));
			});
		}
		String mine = me();
		ArrayList<HashMap<String, Object>> out = new ArrayList<>();
		for (HashMap<String, Object> m : posts) {
			String v = strOf(m, "visibility");
			String author = strOf(m, "authorUid");
			if (blockedUids != null && blockedUids.contains(author)) continue;          // people I have blocked
			boolean own = !mine.isEmpty() && mine.equals(author);
			if (own || v.isEmpty() || "public".equals(v)) { out.add(m); continue; }
			if ("followers".equals(v) && followingUids.contains(author)) out.add(m);
		}
		return out;
	}
	
	private String me() {
		com.google.firebase.auth.FirebaseUser u = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
		return u == null ? "" : u.getUid();
	}
	
	private void _loadContent() {
		if (getContext() == null) return;
		
		List<HashMap<String, Object>> cached = com.harvana.app.offline.OfflineCacheManager.loadList(
		getContext(), "learn_content");
		if (cached != null && !cached.isEmpty()) {
			_onData(cached, true);
		}
		
		// Live sync: the native Firestore listener delivers the published articles now and every time
		// the node's review service adds one (also served from the local cache when offline).
		if (learnListener != null) learnListener.remove();
		if (com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() == null) return;
		learnListener = com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("LEARN_CONTENT")
		.addSnapshotListener((snap, err) -> {
			if (err != null || snap == null || getActivity() == null) return;
			ArrayList<HashMap<String, Object>> fresh = new ArrayList<>();
			for (com.google.firebase.firestore.QueryDocumentSnapshot d : snap) {
				HashMap<String, Object> m = new HashMap<>(com.harvana.app.firestore.GroupFirestoreManager.readFromSdk(d.getData()));
				m.put("_docId", d.getId());
				fresh.add(m);
			}
			com.harvana.app.offline.OfflineCacheManager.saveList(getContext(), "learn_content", fresh);
			_onData(fresh, false);
		});
	}
	
	private com.google.firebase.firestore.ListenerRegistration learnListener;
	
	@Override
	public void onDestroyView() {
		if (learnListener != null) { learnListener.remove(); learnListener = null; }
		if (getContext() != null) com.harvana.app.learn.LearnInterest.get(getContext()).flush();
		super.onDestroyView();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		if (ads != null) { ads.load(); ads.start(); }
	}

	@Override
	public void onPause() {
		super.onPause();
		if (ads != null) ads.stop();
		if (getContext() != null) com.harvana.app.learn.LearnInterest.get(getContext()).flush();
	}
	
	/** Takes the posts from the cache or from Firestore. The first load orders the feed; later loads only merge. */
	private void _onData(List<HashMap<String, Object>> posts, boolean fromCache) {
		if (getActivity() == null) return;
		lastPosts = posts;
		posts = visibleTo(posts);
		HashMap<String, HashMap<String, Object>> incoming = new HashMap<>();
		for (HashMap<String, Object> m : posts) { String id = idOf(m); if (!id.isEmpty()) incoming.put(id, m); }
		
		boolean firstLoad = order.isEmpty();
		byId.clear();
		byId.putAll(incoming);
		listMap.clear();
		listMap.addAll(posts);
		
		if (firstLoad) {
			_rerank();
		} else {
			order.retainAll(incoming.keySet());                  // deleted posts leave
			waiting.retainAll(incoming.keySet());
			String mine = me();
			for (String id : incoming.keySet()) {
				if (order.contains(id) || waiting.contains(id)) continue;
				// Something you just published goes straight to the top; other people's new posts wait behind the button.
				if (!mine.isEmpty() && mine.equals(strOf(incoming.get(id), "authorUid"))) order.add(0, id);
				else waiting.add(id);
			}
		}
		_showWaiting();
		_show();
	}
	
	/** Works the order out again from everything we have: For you ranks by interest, Latest by time. */
	private void _rerank() {
		order.clear();
		waiting.clear();
		ArrayList<HashMap<String, Object>> all = new ArrayList<>(byId.values());
		if (LATEST.equals(selectedCategory)) {
			Collections.sort(all, (x, y) -> Long.compare(timeOf(y), timeOf(x)));
			for (HashMap<String, Object> m : all) order.add(idOf(m));
			return;
		}
		ArrayList<com.harvana.app.learn.LearnRanker.Item> items = new ArrayList<>();
		for (HashMap<String, Object> m : all) {
			long views = m.get("views") instanceof Number ? ((Number) m.get("views")).longValue() : 0L;
			items.add(new com.harvana.app.learn.LearnRanker.Item(idOf(m), strOf(m, "category"), strOf(m, "authorUid"), timeOf(m), views));
		}
		java.util.List<com.harvana.app.learn.LearnRanker.Item> ranked = com.harvana.app.learn.LearnRanker.rank(
			items, com.harvana.app.learn.LearnInterest.get(getContext()), System.currentTimeMillis(), me(), seed);
		for (com.harvana.app.learn.LearnRanker.Item it : ranked) order.add(it.id);
	}
	
	/** Puts the visible, filtered posts into the list, redrawing only the cards that changed. */
	private void _show() {
		if (getActivity() == null || recyclerview2 == null) return;
		ArrayList<HashMap<String, Object>> shown = new ArrayList<>();
		boolean byCategory = !FOR_YOU.equals(selectedCategory) && !LATEST.equals(selectedCategory);
		String q = learnQuery == null ? "" : learnQuery.trim().toLowerCase(java.util.Locale.US);
		for (String id : order) {
			HashMap<String, Object> m = byId.get(id);
			if (m == null) continue;
			if (byCategory && !selectedCategory.equals(strOf(m, "category"))) continue;
			if (!q.isEmpty()) {
				String hay = (strOf(m, "title") + " " + strOf(m, "description") + " " + strOf(m, "category")
					+ " " + strOf(m, "authorName") + " " + strOf(m, "author")).toLowerCase(java.util.Locale.US);
				if (!hay.contains(q)) continue;
			}
			shown.add(m);
		}
		learnShownCount = shown.size();
		_paintLearnEmpty(shown.isEmpty(), !q.isEmpty());
		if (adapter == null) {
			adapter = new LearnContentAdapter(shown);
			recyclerview2.setAdapter(adapter);
			return;
		}
		final ArrayList<HashMap<String, Object>> before = adapter.data;
		androidx.recyclerview.widget.DiffUtil.DiffResult diff = androidx.recyclerview.widget.DiffUtil.calculateDiff(new androidx.recyclerview.widget.DiffUtil.Callback() {
			@Override public int getOldListSize() { return before.size(); }
			@Override public int getNewListSize() { return shown.size(); }
			@Override public boolean areItemsTheSame(int o, int n) { return idOf(before.get(o)).equals(idOf(shown.get(n))); }
			@Override public boolean areContentsTheSame(int o, int n) { return signature(before.get(o)).equals(signature(shown.get(n))); }
		});
		adapter.data = shown;
		diff.dispatchUpdatesTo(adapter);
	}

	private LinearLayout learnEmpty;

	/** Nothing in the feed (or nothing matching the search): the same kind of picture the empty groups screen uses. */
	private void _paintLearnEmpty(boolean empty, boolean filtered) {
		if (cardview14 == null || getContext() == null) return;
		if (empty) com.harvana.app.utils.Shimmer.release(recyclerview2);
		if (learnEmpty == null) {
			final float d = getResources().getDisplayMetrics().density;
			learnEmpty = new LinearLayout(getContext());
			learnEmpty.setOrientation(LinearLayout.VERTICAL);
			learnEmpty.setGravity(Gravity.CENTER);
			learnEmpty.setPadding((int) (24 * d), (int) (24 * d), (int) (24 * d), (int) (24 * d));
			ImageView icon = new ImageView(getContext());
			icon.setTag("empty_icon");
			icon.setAdjustViewBounds(true);
			learnEmpty.addView(icon, new LinearLayout.LayoutParams((int) (160 * d), (int) (120 * d)));
			TextView title = new TextView(getContext());
			title.setTag("empty_title");
			title.setTextSize(15);
			title.setTypeface(Typeface.DEFAULT_BOLD);
			title.setTextColor(com.harvana.app.utils.UiColors.textPrimary());
			title.setGravity(Gravity.CENTER);
			LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			tp.topMargin = (int) (12 * d);
			learnEmpty.addView(title, tp);
			TextView message = new TextView(getContext());
			message.setTag("empty_message");
			message.setTextSize(13);
			message.setTextColor(com.harvana.app.utils.UiColors.textSecondary());
			message.setGravity(Gravity.CENTER);
			LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			mp.topMargin = (int) (6 * d);
			learnEmpty.addView(message, mp);
			com.google.android.material.button.MaterialButton write = new com.google.android.material.button.MaterialButton(getContext());
			write.setTag("empty_button");
			write.setAllCaps(false);
			write.setOnClickListener(v -> {
				if (learnQuery != null && !learnQuery.isEmpty()) {
					learnQuery = "";
					if (et_search_learn != null) et_search_learn.setText("");
					_show();
					return;
				}
				startActivity(new Intent(getContext(), CreateLearnhubContentActivity.class));
			});
			LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, (int) (44 * d));
			bp.topMargin = (int) (16 * d);
			learnEmpty.addView(write, bp);
			cardview14.addView(learnEmpty, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		}
		((ImageView) learnEmpty.findViewWithTag("empty_icon")).setImageResource(filtered ? R.drawable.illus_empty_search : R.drawable.illus_empty_learn);
		((TextView) learnEmpty.findViewWithTag("empty_title")).setText(filtered ? "No matching posts" : "No posts yet");
		TextView message = (TextView) learnEmpty.findViewWithTag("empty_message");
		if (message != null) message.setText(filtered
				? "Nothing in Learn Hub matches that search."
				: "Lessons and posts show up here once they are published.");
		((TextView) learnEmpty.findViewWithTag("empty_button")).setText(filtered ? "Show all posts" : "Write a post");
		learnEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
		recyclerview2.setVisibility(empty ? View.GONE : View.VISIBLE);
	}

	private void _openLearnSearch() {
		if (getActivity() == null) return;
		com.harvana.app.ui.SearchSheet.show(getActivity(), "Search Learn Hub", "Title, topic or author", learnQuery,
			new com.harvana.app.ui.SearchSheet.Listener() {
				@Override public void onQuery(String q) {
					learnQuery = q == null ? "" : q.trim().toLowerCase(java.util.Locale.US);
					if (et_search_learn != null) et_search_learn.setText(q);
					_show();
				}
				@Override public String summary() {
					int n = learnShownCount;
					return n == 1 ? "1 post matches" : n + " posts match";
				}
			});
	}
	
	/** What a card shows. The view counter is left out on purpose: it changes all the time and is not worth a redraw. */
	private static String signature(Map<String, Object> m) {
		return strOf(m, "title") + "\u0001" + strOf(m, "description") + "\u0001" + strOf(m, "imageUrl") + "\u0001" + strOf(m, "videoUrl") + "\u0001" + strOf(m, "category");
	}
	
	private void _showWaiting() {
		if (getContext() == null) return;
		if (newPill == null) {
			newPill = new TextView(getContext());
			newPill.setTextSize(13);
			newPill.setTypeface(null, Typeface.BOLD);
			newPill.setTextColor(com.harvana.app.utils.UiColors.onAccent());
			newPill.setGravity(Gravity.CENTER);
			float d = getResources().getDisplayMetrics().density;
			newPill.setPadding((int) (16 * d), (int) (8 * d), (int) (16 * d), (int) (8 * d));
			GradientDrawable g = new GradientDrawable();
			g.setCornerRadius(40 * d);
			g.setColor(androidx.core.content.ContextCompat.getColor(getContext(), R.color.colorAccent));
			newPill.setBackground(g);
			LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			lp.gravity = Gravity.CENTER_HORIZONTAL;
			lp.topMargin = (int) (4 * d);
			int at = linear1.indexOfChild(cardview14);
			linear1.addView(newPill, Math.max(0, at), lp);
			newPill.setOnClickListener(v -> {
				_rerank();
				_showWaiting();
				_show();
				recyclerview2.scrollToPosition(0);
			});
		}
		if (waiting.isEmpty()) newPill.setVisibility(View.GONE);
		else {
			newPill.setText(waiting.size() + (waiting.size() == 1 ? " new post" : " new posts"));
			com.harvana.app.utils.Icons.start(newPill, com.harvana.app.R.drawable.ic_arrow_up_line, 15);
			if (newPill.getCompoundDrawablesRelative()[0] != null) newPill.getCompoundDrawablesRelative()[0].setTint(com.harvana.app.utils.UiColors.onAccent());
			newPill.setVisibility(View.VISIBLE);
		}
	}
	
	private void _initAdapters() {
		recyclerview1.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
		recyclerview1.setItemAnimator(null);                       // chips must not animate or change width when pressed
		recyclerview1.setHasFixedSize(true);
		recyclerview1.setAdapter(new CategoryAdapter(CATEGORIES));
		
		recyclerview2.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
	}
	
	public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {
		private String[] categories;
		
		public CategoryAdapter(String[] categories) {
			this.categories = categories;
		}
		
		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.learn_hub_custom_category_picker, parent, false);
			return new ViewHolder(view);
		}
		
		@Override
		public void onBindViewHolder(ViewHolder holder, int position) {
			String category = categories[position];
			if (holder.label != null) holder.label.setText(category);
			
			boolean isSelected = category.equals(selectedCategory);
			if (holder.card != null) {
				holder.card.setCardBackgroundColor(isSelected
				? androidx.core.content.ContextCompat.getColor(getContext(), R.color.colorAccent)
				: androidx.core.content.ContextCompat.getColor(getContext(), R.color.colorSurfaceRaised));
			}
			if (holder.label != null) {
				holder.label.setTextColor(isSelected ? com.harvana.app.utils.UiColors.onAccent()
					: androidx.core.content.ContextCompat.getColor(getContext(), R.color.colorTextPrimary));
			}
			
			holder.itemView.setOnClickListener(v -> {
				String before = selectedCategory;
				selectedCategory = category;
				for (int i = 0; i < categories.length; i++) if (categories[i].equals(before) || categories[i].equals(category)) notifyItemChanged(i);
				if (FOR_YOU.equals(category) || LATEST.equals(category)) _rerank();
				_showWaiting();
				_show();
				if (recyclerview2 != null) recyclerview2.scrollToPosition(0);
			});
		}
		
		@Override
		public int getItemCount() {
			return categories.length;
		}
		
		public class ViewHolder extends RecyclerView.ViewHolder {
			androidx.cardview.widget.CardView card;
			TextView label;
			
			public ViewHolder(View itemView) {
				super(itemView);
				card = itemView.findViewById(R.id.card_category_chip);
				label = itemView.findViewById(R.id.tv_category_chip_label);
			}
		}
	}
	
	public class LearnContentAdapter extends RecyclerView.Adapter<LearnContentAdapter.ViewHolder> {
		ArrayList<HashMap<String, Object>> data;
		
		public LearnContentAdapter(ArrayList<HashMap<String, Object>> data) {
			this.data = data;
		}
		
		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.learn_post_card, parent, false);
			return new ViewHolder(view);
		}
		
		@Override
		public void onBindViewHolder(ViewHolder holder, int position) {
			final com.harvana.app.learn.LearnPostBinder.Post post = com.harvana.app.learn.LearnPostBinder.Post.from(data.get(position));
			holder.postId = post.id;
			com.harvana.app.learn.LearnPostBinder.bind(getActivity(), holder.itemView, post, false, null);
			View.OnClickListener open = v -> {
				Intent i = new Intent(getContext(), LearnReadActivity.class);
				i.putExtra(LearnReadActivity.EXTRA_ID, post.id);
				i.putExtra(LearnReadActivity.EXTRA_TITLE, post.title);
				i.putExtra(LearnReadActivity.EXTRA_BODY, post.body);
				i.putExtra(LearnReadActivity.EXTRA_CATEGORY, post.category);
				i.putExtra(LearnReadActivity.EXTRA_IMAGE, post.imageUrl);
				i.putExtra(LearnReadActivity.EXTRA_VIDEO, post.videoUrl);
				i.putExtra(LearnReadActivity.EXTRA_AUTHOR, post.authorUid);
				i.putExtra(LearnReadActivity.EXTRA_CREATED, post.createdAt);
				i.putExtra(LearnReadActivity.EXTRA_VIEWS, post.views);
				startActivity(i);
			};
			View title = holder.itemView.findViewById(R.id.tv_title);
			View body = holder.itemView.findViewById(R.id.tv_body);
			View cover = holder.itemView.findViewById(R.id.fl_cover);
			if (cover != null) cover.setOnClickListener(open);          // in the feed, the picture opens the post
			if (title != null) title.setOnClickListener(open);
			if (body != null) body.setOnClickListener(open);
		}
		
		@Override
		public void onViewAttachedToWindow(ViewHolder holder) {
			super.onViewAttachedToWindow(holder);
			// A post that came on screen counts as shown, so the next feed does not lead with it again.
			if (getContext() != null && holder.postId != null) com.harvana.app.learn.LearnInterest.get(getContext()).shown(holder.postId);
		}
		
		@Override
		public int getItemCount() {
			return data.size();
		}
		
		public class ViewHolder extends RecyclerView.ViewHolder {
			String postId;
			public ViewHolder(View _view) {
				super(_view);
			}
		}
	}
	
	{
	}
	
}
