package com.harvana.app;

import android.animation.*;
import android.app.*;
import android.app.Activity;
import android.content.*;
import android.content.SharedPreferences;
import android.content.res.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.media.*;
import android.net.*;
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
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;
import org.json.*;

public class GroupNoticeFragmentActivity extends Fragment {
	
	private LinearLayout linear133;
	private RecyclerView recycler_notices;
	private TextView tv_notices_empty;
	private TextView textview78;
	private TextView btn_new_notice;
	
	private SharedPreferences groupsp;
	private SharedPreferences authsp;
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
		View _view = _inflater.inflate(R.layout.group_notice_fragment, _container, false);
		initialize(_savedInstanceState, _view);
		FirebaseApp.initializeApp(getContext());
		initializeLogic();
		return _view;
	}
	
	private void initialize(Bundle _savedInstanceState, View _view) {
		linear133 = _view.findViewById(R.id.linear133);
		recycler_notices = _view.findViewById(R.id.recycler_notices);
		tv_notices_empty = _view.findViewById(R.id.tv_notices_empty);
		textview78 = _view.findViewById(R.id.textview78);
		btn_new_notice = _view.findViewById(R.id.btn_new_notice);
		groupsp = getContext().getSharedPreferences("groupsp", Activity.MODE_PRIVATE);
		authsp = getContext().getSharedPreferences("authsp", Activity.MODE_PRIVATE);
		fauth = FirebaseAuth.getInstance();
		
		btn_new_notice.setOnClickListener(_v -> _showPostDialog());
		
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
		groupId = getArguments() != null ? getArguments().getString("groupId", "") : "";
		_resolveAdminState();
		_loadNotices();
		
	}
	
	@Override
	public void onResume() {
		super.onResume();
		if (groupId != null && !groupId.isEmpty()) { 	
			_resolveAdminState(); 		
			_loadNotices(); 		
		}
		
	}
	public void _MORE() {
	}
	private String groupId = "";
	private HashMap<String, Object> user_v_map = new HashMap<>();
	private double length = 0;
	private double number = 0;
	private boolean isAdmin = false;
	
	private ArrayList<HashMap<String, Object>> group_list_map = new ArrayList<>();
	private ArrayList<HashMap<String, Object>> notice_l_map = new ArrayList<>();
	
	private void _resolveAdminState() {
		if (groupId == null || groupId.isEmpty() || getContext() == null) return;
		
		Map<String, Object> cachedGroup = com.harvana.app.offline.OfflineCacheManager.load(
		getContext(), "group_" + groupId);
		if (cachedGroup != null && !cachedGroup.isEmpty()) {
			_applyAdminState(cachedGroup);
		}
		
		com.harvana.app.firestore.GroupFirestoreManager.getInstance(getContext())
		.getGroup(groupId, new com.harvana.app.firestore.GroupFirestoreManager.DocCallback() {
			@Override
			public void onSuccess(Map<String, Object> data) {
				if (data == null || data.isEmpty()) return;
				com.harvana.app.offline.OfflineCacheManager.save(getContext(), "group_" + groupId, data);
				if (getActivity() == null) return;
				getActivity().runOnUiThread(() -> _applyAdminState(data));
			}
			@Override
			public void onError(String error) {}
		});
	}
	
	private void _applyAdminState(Map<String, Object> data) {
		String creatorId = _str(data, "creatorId", _str(data, "adminUid", ""));
		String currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null
		? com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
		isAdmin = !creatorId.isEmpty() && creatorId.equals(currentUid);
		if (linear133 != null) linear133.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
		if (btn_new_notice != null) {
			btn_new_notice.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
			btn_new_notice.setOnClickListener(v -> _showPostDialog());
		}
	}
	
	private static String _str(Map<String, Object> m, String key, String fallback) {
		Object v = m.get(key);
		return v != null ? v.toString() : fallback;
	}
	
	private void _loadNotices() {
		if (groupId.isEmpty() || getContext() == null) return;
		
		// Offline-first: render last-known notices immediately.
		List<HashMap<String, Object>> cached = com.harvana.app.offline.OfflineCacheManager.loadList(
		getContext(), "notices_" + groupId);
		if (cached != null && !cached.isEmpty()) {
			_bindNoticeList(new ArrayList<>(cached));
		}
		
		com.harvana.app.group.GroupNoticeHelper.listNotices(getContext(), groupId,
		new com.harvana.app.group.GroupNoticeHelper.ListCallback() {
			@Override
			public void onSuccess(List<Map<String, Object>> items) {
				List<Map<String, Object>> list = items != null ? items : new ArrayList<>();
				com.harvana.app.offline.OfflineCacheManager.saveList(getContext(), "notices_" + groupId, list);
				_bindNoticeList(list);
			}
			
			@Override
			public void onError(String error) {
				if (getActivity() == null) return;
				getActivity().runOnUiThread(() -> {
					if (com.harvana.app.offline.NetworkStatus.isConnectivityError(error) && cached != null && !cached.isEmpty()) {
						return; 
					}
					if (tv_notices_empty != null && (cached == null || cached.isEmpty())) {
						tv_notices_empty.setVisibility(View.VISIBLE);
					}
				});
			}
		});
	}
	
	private void _bindNoticeList(List<Map<String, Object>> list) {
		if (getActivity() == null) return;
		final List<Map<String, Object>> shown = com.harvana.app.group.GroupNoticeHelper.unique(list);
		getActivity().runOnUiThread(() -> {
			if (tv_notices_empty != null) {
				tv_notices_empty.setVisibility(shown.isEmpty() ? View.VISIBLE : View.GONE);
			}
			if (recycler_notices != null) {
				if (recycler_notices.getLayoutManager() == null) {
					recycler_notices.setLayoutManager(new LinearLayoutManager(getContext()));
				}
				recycler_notices.setAdapter(new NoticeAdapter(shown));
			}
		});
	}
	
	private void _showPostDialog() {
		if (getActivity() == null) return;
		
		com.google.android.material.bottomsheet.BottomSheetDialog dialog =
		new com.google.android.material.bottomsheet.BottomSheetDialog(getActivity());
		int layoutId = getResources().getIdentifier("bottom_sheet_post_notice", "layout", getContext().getPackageName());
		if (layoutId == 0) {
			SketchwareUtil.showMessage(getContext(), "Error: bottom_sheet_post_notice.xml layout not found!");
			return;
		}
		View sheetView = getActivity().getLayoutInflater().inflate(layoutId, null);
		dialog.setContentView(sheetView);
		com.harvana.app.group.BottomSheetUtils.expandFullScreen(dialog, sheetView);
		
		EditText title = sheetView.findViewById(getResources().getIdentifier("et_notice_title", "id", getContext().getPackageName()));
		EditText body = sheetView.findViewById(getResources().getIdentifier("et_notice_body", "id", getContext().getPackageName()));
		View btnCancel = sheetView.findViewById(getResources().getIdentifier("btn_notice_cancel", "id", getContext().getPackageName()));
		View btnPost = sheetView.findViewById(getResources().getIdentifier("btn_notice_post", "id", getContext().getPackageName()));
		
		if (btnCancel != null) btnCancel.setOnClickListener(v -> dialog.dismiss());
		if (btnPost != null) {
			btnPost.setOnClickListener(v -> {
				String titleText = title != null ? title.getText().toString().trim() : "";
				String bodyText = body != null ? body.getText().toString().trim() : "";
				com.harvana.app.group.GroupNoticeHelper.postAnnouncement(
				getContext(), groupId, titleText, bodyText,
				new com.harvana.app.group.GroupNoticeHelper.Callback() {
					@Override
					public void onSuccess(String id) {
						if (getActivity() == null) return;
						getActivity().runOnUiThread(() -> {
							dialog.dismiss();
							SketchwareUtil.showMessage(getContext(), "Notice posted");
							_loadNotices();
						});
					}
					
					@Override
					public void onError(String error) {
						if (getActivity() == null) return;
						if (com.harvana.app.offline.NetworkStatus.isConnectivityError(error)) {
							// Queue action for sync
							java.util.Map<String, Object> payload = new java.util.HashMap<>();
							payload.put("groupId", groupId);
							payload.put("title", titleText);
							payload.put("message", bodyText);
							com.harvana.app.offline.OfflineActionQueue.enqueue(getContext(), "NOTICE", payload);
							
							// FIX: Add offline notice to cache immediately so UI feels responsive
							Map<String, Object> offlineNotice = new HashMap<>();
							offlineNotice.put("title", titleText);
							offlineNotice.put("message", bodyText);
							offlineNotice.put("timestamp", System.currentTimeMillis());
							offlineNotice.put("system", false);
							offlineNotice.put("creatorId", FirebaseAuth.getInstance().getCurrentUser().getUid());
							
							List<HashMap<String, Object>> currentCache = com.harvana.app.offline.OfflineCacheManager.loadList(getContext(), "notices_" + groupId);
							if (currentCache == null) currentCache = new ArrayList<>();
							currentCache.add(0, new HashMap<>(offlineNotice));
							com.harvana.app.offline.OfflineCacheManager.saveList(getContext(), "notices_" + groupId, currentCache);
							
							getActivity().runOnUiThread(() -> {
								dialog.dismiss();
								SketchwareUtil.showMessage(getContext(), "You're offline — this will post automatically once you're back online.");
								_loadNotices();
							});
							return;
						}
						getActivity().runOnUiThread(() ->
						SketchwareUtil.showMessage(getContext(), error));
					}
				});
			});
		}
		
		dialog.show();
	}
	
	private class NoticeAdapter extends RecyclerView.Adapter<NoticeAdapter.VH> {
		private final List<Map<String, Object>> items;
		
		NoticeAdapter(List<Map<String, Object>> items) {
			this.items = items;
		}
		
		@NonNull
		@Override
		public VH onCreateViewHolder(@NonNull ViewGroup parent, int vt) {
			View v = LayoutInflater.from(parent.getContext())
			.inflate(R.layout.notice_custom_card, parent, false);
			return new VH(v);
		}
		
		@Override
		public void onBindViewHolder(@NonNull VH h, int pos) {
			Map<String, Object> n = items.get(pos);
			
			// FIX: Use a temporary variable for the fallback assignment so the 
			// final variables used in the lambda remain "effectively final".
			String tempTitle = str(n, "title", "");
			if (tempTitle.isEmpty()) tempTitle = str(n, "sender", "Notice");
			
			final String rawTitle = tempTitle;
			final String rawBody = str(n, "message", str(n, "body", ""));
			
			_showCopy(h, rawTitle, rawBody);
			
			boolean isSystem = Boolean.TRUE.equals(n.get("system")) || "true".equalsIgnoreCase(str(n, "system", ""));
			String creatorId = str(n, "creatorId", "");
			
			if (isSystem) {
				h.author.setText("By System");
				if (!creatorId.isEmpty()) {
					String uniqueTag = "system_" + creatorId + "_" + pos;
					h.author.setTag(uniqueTag);
					com.harvana.app.group.MemberNameResolver.resolve(getContext(), creatorId, "Member", resolvedName -> {
						if (h.author.getTag() != null && h.author.getTag().equals(uniqueTag)) {
							String newTitle = rawTitle;
							String newBody = rawBody;
							
							if (rawTitle.equalsIgnoreCase("New member joined")) {
								newTitle = resolvedName + " joined the group";
								newBody = "";
							} else if (rawTitle.equalsIgnoreCase("Contribution received")) {
								newBody = rawBody.replace("A member", resolvedName);
							} else if (rawTitle.equalsIgnoreCase("Loan repaid") || rawBody.contains("fully repaid")) {
								newTitle = resolvedName + " repaid a loan";
								newBody = "";
							} else if (rawTitle.equalsIgnoreCase("Loan applied")) {
								newBody = rawBody.replace("A member", resolvedName);
							} else if (rawTitle.equalsIgnoreCase("Loan disbursed")) {
								newBody = "Loan was approved and disbursed to " + resolvedName + ".";
							}
							
							_showCopy(h, newTitle, newBody);
						}
					});
				}
			} else if (creatorId.isEmpty()) {
				h.author.setText("By " + com.harvana.app.group.GroupNoticeHelper.displayAuthor(n));
			} else {
				h.author.setTag(creatorId + "_" + pos);
				h.author.setText("By …");
				com.harvana.app.group.MemberNameResolver.resolve(getContext(), creatorId, "Admin", resolvedName -> {
					if (h.author.getTag() != null && h.author.getTag().equals(creatorId + "_" + pos)) {
						h.author.setText("By " + resolvedName);
					}
				});
			}
			
			if (h.date != null) {
				String formatted = com.harvana.app.group.GroupNoticeHelper.formatDate(n);
				h.date.setText(formatted.isEmpty() ? str(n, "timestamp", "") : formatted);
			}
		}
		
		@Override
		public int getItemCount() {
			return items.size();
		}
		
		class VH extends RecyclerView.ViewHolder {
			TextView title, body, author, date;
			
			VH(View v) {
				super(v);
				title = v.findViewById(R.id.tv_notice_title);
				body = v.findViewById(R.id.tv_notice_body);
				author = v.findViewById(R.id.tv_notice_author);
				date = v.findViewById(R.id.tv_notice_date);
			}
		}
		
		String str(Map<String, Object> m, String k, String d) {
			Object v = m.get(k);
			return v != null ? String.valueOf(v) : d;
		}
	}

	/** Title is the notice. The body is hidden when it only repeats the title. */
	private static void _showCopy(NoticeAdapter.VH h, String title, String body) {
		if (h.title != null) h.title.setText(title == null ? "" : title);
		if (h.body == null) return;
		String t = title == null ? "" : title.trim();
		String b = body == null ? "" : body.trim();
		String nt = t.toLowerCase(java.util.Locale.US).replaceAll("[^a-z0-9]+", " ").replace(" has ", " ").trim();
		String nb = b.toLowerCase(java.util.Locale.US).replaceAll("[^a-z0-9]+", " ").replace(" has ", " ").trim();
		boolean repeat = b.isEmpty() || nt.equals(nb) || (!nt.isEmpty() && (nb.startsWith(nt) || nt.startsWith(nb)));
		h.body.setVisibility(repeat ? View.GONE : View.VISIBLE);
		if (!repeat) h.body.setText(b);
	}
	
	{
	}
	
}
