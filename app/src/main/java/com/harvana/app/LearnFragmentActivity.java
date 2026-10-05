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
		if (_myActivity != null) _myActivity.setOnClickListener(_v -> startActivity(new Intent(getContext(), ActivityStatsActivity.class)));
		vscroll8 = _view.findViewById(R.id.vscroll8);
		linear1 = _view.findViewById(R.id.linear1);
		textview1 = _view.findViewById(R.id.textview1);
		textview2 = _view.findViewById(R.id.textview2);
		recyclerview1 = _view.findViewById(R.id.recyclerview1);
		cardview14 = _view.findViewById(R.id.cardview14);
		recyclerview2 = _view.findViewById(R.id.recyclerview2);
		com.harvana.app.utils.Shimmer.hold(recyclerview2, com.harvana.app.utils.Shimmer.cards(recyclerview2.getContext(), 3), 4000);
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
	
	private void initializeLogic() {
		_initAdapters();
		_loadContent();
	}
	
	public void _MORE() {
	}
	private String selectedCategory = "All";
	
	private static final String[] CATEGORIES = {
		"All", "Community Knowledge", "Finance Trends", "Investment Insights",
		"Group Savings", "Crypto & Blockchain"
	};
	
	private void _loadContent() {
		if (getContext() == null) return;
		
		List<HashMap<String, Object>> cached = com.harvana.app.offline.OfflineCacheManager.loadList(
		getContext(), "learn_content");
		if (cached != null && !cached.isEmpty()) {
			listMap.clear();
			listMap.addAll(cached);
			_renderFiltered();
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
			Collections.sort(fresh, (x, y) -> {
				long tx = x.get("createdAt") instanceof Number ? ((Number) x.get("createdAt")).longValue() : 0L;
				long ty = y.get("createdAt") instanceof Number ? ((Number) y.get("createdAt")).longValue() : 0L;
				return Long.compare(ty, tx);
			});
			listMap.clear();
			listMap.addAll(fresh);
			com.harvana.app.offline.OfflineCacheManager.saveList(getContext(), "learn_content", listMap);
			_renderFiltered();
		});
	}
	
	private com.google.firebase.firestore.ListenerRegistration learnListener;
	
	@Override
	public void onDestroyView() {
		if (learnListener != null) { learnListener.remove(); learnListener = null; }
		super.onDestroyView();
	}
	
	private void _renderFiltered() {
		if (getActivity() == null) return;
		ArrayList<HashMap<String, Object>> filtered = new ArrayList<>();
		for (HashMap<String, Object> item : listMap) {
			Object cat = item.get("category");
			if ("All".equals(selectedCategory) || (cat != null && selectedCategory.equals(cat.toString()))) {
				filtered.add(item);
			}
		}
		
		recyclerview2.setAdapter(new LearnContentAdapter(filtered));
	}
	
	private void _initAdapters() {
		recyclerview1.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
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
				holder.label.setTextColor(0xFFFFFFFF);
				holder.label.setAlpha(isSelected ? 1f : 0.8f);
			}
			
			holder.itemView.setOnClickListener(v -> {
				selectedCategory = category;
				notifyDataSetChanged();
				_renderFiltered();
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
		private ArrayList<HashMap<String, Object>> data;
		
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
			com.harvana.app.learn.LearnPostBinder.bind(getActivity(), holder.itemView, post, false, null);
			View.OnClickListener open = v -> {
				Intent i = new Intent(getContext(), LearnReadActivity.class);
				i.putExtra(LearnReadActivity.EXTRA_ID, post.id);
				i.putExtra(LearnReadActivity.EXTRA_TITLE, post.title);
				i.putExtra(LearnReadActivity.EXTRA_BODY, post.body);
				i.putExtra(LearnReadActivity.EXTRA_CATEGORY, post.category);
				i.putExtra(LearnReadActivity.EXTRA_IMAGE, post.imageUrl);
				i.putExtra(LearnReadActivity.EXTRA_AUTHOR, post.authorUid);
				i.putExtra(LearnReadActivity.EXTRA_CREATED, post.createdAt);
				i.putExtra(LearnReadActivity.EXTRA_VIEWS, post.views);
				startActivity(i);
			};
			View title = holder.itemView.findViewById(R.id.tv_title);
			View body = holder.itemView.findViewById(R.id.tv_body);
			if (title != null) title.setOnClickListener(open);
			if (body != null) body.setOnClickListener(open);
		}
		
		@Override
		public int getItemCount() {
			return data.size();
		}
		
		public class ViewHolder extends RecyclerView.ViewHolder {
			public ViewHolder(View _view) {
				super(_view);
			}
		}
	}
	
	{
	}
	
}
