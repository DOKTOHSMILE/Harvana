package com.harvanna.wallet;

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
		vscroll8 = _view.findViewById(R.id.vscroll8);
		linear1 = _view.findViewById(R.id.linear1);
		textview1 = _view.findViewById(R.id.textview1);
		textview2 = _view.findViewById(R.id.textview2);
		recyclerview1 = _view.findViewById(R.id.recyclerview1);
		cardview14 = _view.findViewById(R.id.cardview14);
		recyclerview2 = _view.findViewById(R.id.recyclerview2);
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
		
		List<HashMap<String, Object>> cached = com.harvanna.wallet.offline.OfflineCacheManager.loadList(
		getContext(), "learn_content");
		if (cached != null && !cached.isEmpty()) {
			listMap.clear();
			listMap.addAll(cached);
			_renderFiltered();
		}
		
		com.harvanna.wallet.firestore.GroupFirestoreManager.getInstance(getContext())
		.getLearnContent(new com.harvanna.wallet.firestore.GroupFirestoreManager.ListCallback() {
			@Override
			public void onSuccess(List<Map<String, Object>> items) {
				listMap.clear();
				if (items != null) {
					for (Map<String, Object> item : items) {
						listMap.add(new HashMap<>(item));
					}
					Collections.sort(listMap, (a, b) -> {
						long ta = a.get("createdAt") instanceof Number ? ((Number) a.get("createdAt")).longValue() : 0L;
						long tb = b.get("createdAt") instanceof Number ? ((Number) b.get("createdAt")).longValue() : 0L;
						return Long.compare(tb, ta);
					});
				}
				com.harvanna.wallet.offline.OfflineCacheManager.saveList(getContext(), "learn_content", listMap);
				if (getActivity() == null) return;
				getActivity().runOnUiThread(() -> _renderFiltered());
			}
			
			@Override
			public void onError(String error) {
				if (com.harvanna.wallet.offline.NetworkStatus.isConnectivityError(error)
				&& !listMap.isEmpty()) {
					return;
				}
			}
		});
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
			View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.learn_hub_content_view, parent, false);
			return new ViewHolder(view);
		}
		
		@Override
		public void onBindViewHolder(ViewHolder holder, int position) {
			HashMap<String, Object> item = data.get(position);
			
			holder.layoutBadgeWrapper.setBackground(new GradientDrawable() { 
				public GradientDrawable getIns(int a, int b) {
					this.setCornerRadius(a);
					this.setColor(b); 
					return this;
				} 
			}.getIns((int)30, 0xFFDCFCE7));
			
			Object imageUrlObj = item.get("imageUrl");
			if (imageUrlObj != null && !imageUrlObj.toString().isEmpty()) {
				Glide.with(getContext()).load(Uri.parse(imageUrlObj.toString())).into(holder.imageview_cover);
			} else {
				holder.imageview_cover.setImageDrawable(null);
			}
			
			Object titleObj = item.get("title");
			holder.textview_title.setText(titleObj != null ? titleObj.toString() : "Untitled");
			
			Object descObj = item.get("description");
			holder.textview_desc.setText(descObj != null ? descObj.toString() : "");
			
			Object categoryObj = item.get("category");
			holder.textview_badge.setText(categoryObj != null ? categoryObj.toString() : "General");
			
			Object viewsObj = item.get("views");
			long views = viewsObj instanceof Number ? ((Number) viewsObj).longValue() : 0L;
			holder.textview_views.setText(views + (views == 1 ? " view" : " views"));
			
			Object authorUidObj = item.get("authorUid");
			String authorUid = authorUidObj != null ? authorUidObj.toString() : "";
			if (!authorUid.isEmpty()) {
				com.harvanna.wallet.group.MemberNameResolver.resolveInto(getContext(), authorUid, "Ajoo Community", holder.textview_author);
			} else {
				holder.textview_author.setText("Ajoo Community");
			}
		}
		
		@Override
		public int getItemCount() {
			return data.size();
		}
		
		public class ViewHolder extends RecyclerView.ViewHolder {
			androidx.cardview.widget.CardView card_view_container;
			LinearLayout linear58, layoutBadgeWrapper, linear60, linear62;
			FrameLayout frame_layout59;
			ImageView imageview_cover;
			TextView textview_badge, textview_title, textview_desc, textview_author, textview_views;
			View view61;
			
			public ViewHolder(View _view) {
				super(_view);
				card_view_container = _view.findViewById(R.id.card_view_container);
				linear58 = _view.findViewById(R.id.linear58);
				frame_layout59 = _view.findViewById(R.id.frame_layout59);
				linear60 = _view.findViewById(R.id.linear60);
				imageview_cover = _view.findViewById(R.id.imageview_cover);
				layoutBadgeWrapper = _view.findViewById(R.id.layoutBadgeWrapper);
				textview_badge = _view.findViewById(R.id.textview_badge);
				textview_title = _view.findViewById(R.id.textview_title);
				textview_desc = _view.findViewById(R.id.textview_desc);
				view61 = _view.findViewById(R.id.view61);
				linear62 = _view.findViewById(R.id.linear62);
				textview_author = _view.findViewById(R.id.textview_author);
				textview_views = _view.findViewById(R.id.textview_views);
			}
		}
	}
	
	{
	}
	
}