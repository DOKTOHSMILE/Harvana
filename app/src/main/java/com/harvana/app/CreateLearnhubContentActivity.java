package com.harvana.app;

import android.Manifest;
import android.animation.*;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
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
import android.view.View.*;
import android.view.animation.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.biometric.*;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.budiyev.android.codescanner.*;
import com.bumptech.glide.*;
import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.button.*;
import com.google.android.material.card.*;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.*;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;
import org.json.*;

public class CreateLearnhubContentActivity extends AppCompatActivity {
	
	private Toolbar _toolbar;
	private AppBarLayout _app_bar;
	private CoordinatorLayout _coordinator;
	
	private LinearLayout linear10;
	private TextView textview24;
	private TextView textview25;
	private TextView textview26;
	private MaterialCardView cardview13;
	private TextView textview27;
	private MaterialCardView cardview14;
	private TextView textview28;
	private MaterialCardView cardview7;
	private TextView textview29;
	private MaterialCardView card_image_picker;
	private TextView textview31;
	private MaterialCardView cardview15;
	private TextView textview32;
	private TextView tv_learn_error;
	private MaterialButton btn_learn_post;
	private EditText et_learn_title;
	private EditText et_learn_description;
	private Spinner spinner_learn_category;
	private FrameLayout frame_layout2;
	private ImageView iv_learn_image_preview;
	private LinearLayout layout_image_placeholder;
	private LinearProgressIndicator progress_image_upload;
	private ImageView imageview2;
	private TextView textview30;
	private EditText et_learn_image_url;
	
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
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.create_learnhub_content);
		initialize(_savedInstanceState);
		FirebaseApp.initializeApp(this);
		initializeLogic();
		com.harvana.app.utils.AppBar.header(this, "New Learn Hub article", "Share knowledge with members", R.id.textview24, R.id.textview25);
	}
	
	private void initialize(Bundle _savedInstanceState) {
		_app_bar = findViewById(R.id._app_bar);
		_coordinator = findViewById(R.id._coordinator);
		_toolbar = findViewById(R.id._toolbar);
		setSupportActionBar(_toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);
		_toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _v) {
				onBackPressed();
			}
		});
		linear10 = findViewById(R.id.linear10);
		textview24 = findViewById(R.id.textview24);
		textview25 = findViewById(R.id.textview25);
		textview26 = findViewById(R.id.textview26);
		cardview13 = findViewById(R.id.cardview13);
		textview27 = findViewById(R.id.textview27);
		cardview14 = findViewById(R.id.cardview14);
		textview28 = findViewById(R.id.textview28);
		cardview7 = findViewById(R.id.cardview7);
		textview29 = findViewById(R.id.textview29);
		card_image_picker = findViewById(R.id.card_image_picker);
		textview31 = findViewById(R.id.textview31);
		cardview15 = findViewById(R.id.cardview15);
		textview32 = findViewById(R.id.textview32);
		tv_learn_error = findViewById(R.id.tv_learn_error);
		btn_learn_post = findViewById(R.id.btn_learn_post);
		et_learn_title = findViewById(R.id.et_learn_title);
		et_learn_description = findViewById(R.id.et_learn_description);
		spinner_learn_category = findViewById(R.id.spinner_learn_category);
		frame_layout2 = findViewById(R.id.frame_layout2);
		iv_learn_image_preview = findViewById(R.id.iv_learn_image_preview);
		layout_image_placeholder = findViewById(R.id.layout_image_placeholder);
		progress_image_upload = findViewById(R.id.progress_image_upload);
		imageview2 = findViewById(R.id.imageview2);
		textview30 = findViewById(R.id.textview30);
		et_learn_image_url = findViewById(R.id.et_learn_image_url);
		fauth = FirebaseAuth.getInstance();
		
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
		androidx.appcompat.app.ActionBar ab = getSupportActionBar();
		int backgroundColor = androidx.core.content.ContextCompat.getColor(this, R.color.colorBackground);
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			final Window window = this.getWindow();
			window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
			window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
			com.harvana.app.utils.StatusBarUtil.matchToolbar(this);
		}
		
		ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
		this, android.R.layout.simple_spinner_dropdown_item, CATEGORIES);
		spinner_learn_category.setAdapter(categoryAdapter);
		
		btn_learn_post.setOnClickListener(v -> _submit());
		card_image_picker.setOnClickListener(v -> _checkPermissionAndPickImage());
		
	}
	
	public void _MORE() {
	}
	private static final String[] CATEGORIES = {
		"Community Knowledge", "Finance Trends", "Investment Insights",
		"Group Savings", "Crypto & Blockchain"
	};
	private static final int RC_READ_IMAGES = 501;
	private static final int RC_PICK_IMAGE = 502;
	private String uploadedImageUrl = null;
	private boolean uploadInProgress = false;
	
	// ── Image picking ────────────────────────────────────────────────────
	
	private void _checkPermissionAndPickImage() {
		String permission = Build.VERSION.SDK_INT >= 33
		? Manifest.permission.READ_MEDIA_IMAGES
		: Manifest.permission.READ_EXTERNAL_STORAGE;
		
		if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
			_launchImagePicker();
		} else {
			ActivityCompat.requestPermissions(this, new String[]{permission}, RC_READ_IMAGES);
		}
	}
	
	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == RC_READ_IMAGES) {
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				_launchImagePicker();
			} else {
				SketchwareUtil.showMessage(getApplicationContext(), "Photo library access is needed to pick a cover image.");
			}
		}
	}
	
	private void _launchImagePicker() {
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("image/*");
		startActivityForResult(Intent.createChooser(intent, "Select cover image"), RC_PICK_IMAGE);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == RC_PICK_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {
			Uri pickedUri = data.getData();
			iv_learn_image_preview.setVisibility(View.VISIBLE);
			layout_image_placeholder.setVisibility(View.GONE);
			Glide.with(this).load(pickedUri).centerCrop().into(iv_learn_image_preview);
			_uploadImage(pickedUri);
		}
	}
	
	private void _uploadImage(Uri localUri) {
		String uid = FirebaseAuth.getInstance().getCurrentUser() != null
		? FirebaseAuth.getInstance().getCurrentUser().getUid() : "anon";
		
		uploadInProgress = true;
		uploadedImageUrl = null;
		progress_image_upload.setVisibility(View.VISIBLE);
		btn_learn_post.setEnabled(false);
		tv_learn_error.setVisibility(View.GONE);
		
		String path = "learn_hub/" + uid + "/" + System.currentTimeMillis() + ".jpg";
		StorageReference ref = FirebaseStorage.getInstance().getReference().child(path);
		
		ref.putFile(localUri)
		.addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl()
		.addOnSuccessListener(downloadUri -> {
			uploadedImageUrl = downloadUri.toString();
			uploadInProgress = false;
			progress_image_upload.setVisibility(View.GONE);
			btn_learn_post.setEnabled(true);
		})
		.addOnFailureListener(this::_onUploadFailed))
		.addOnFailureListener(this::_onUploadFailed);
	}
	
	private void _onUploadFailed(Exception e) {
		uploadInProgress = false;
		progress_image_upload.setVisibility(View.GONE);
		btn_learn_post.setEnabled(true);
		_showError("Image upload failed: " + (e != null ? e.getMessage() : "unknown error")
		+ ". You can paste an image URL below instead.");
	}
	
	// ── Submit ───────────────────────────────────────────────────────────
	
	private void _submit() {
		String wallet = com.harvana.app.wallet.WalletManager.getActiveWalletAddress(this);
		if (wallet == null || wallet.trim().isEmpty()) {
			_showError("Add a wallet before you post. Tips for this article are paid to that address.");
			return;
		}
		if (uploadInProgress) {
			_showError("Please wait for the image to finish uploading.");
			return;
		}
		
		String title = et_learn_title.getText().toString().trim();
		String description = et_learn_description.getText().toString().trim();
		String typedUrl = et_learn_image_url.getText().toString().trim();
		// A device-uploaded image always wins over a manually-typed URL if
		// both are somehow present — the upload is the more deliberate,
		// more recent action.
		String imageUrl = uploadedImageUrl != null ? uploadedImageUrl : typedUrl;
		String category = (String) spinner_learn_category.getSelectedItem();
		
		if (title.isEmpty()) {
			_showError("Please enter a title.");
			return;
		}
		if (description.isEmpty()) {
			_showError("Please enter a description.");
			return;
		}
		if (imageUrl.isEmpty() || !(imageUrl.startsWith("http://") || imageUrl.startsWith("https://"))) {
			_showError("Please add a cover image, or paste a valid image URL starting with http:// or https://");
			return;
		}
		if (category == null) category = CATEGORIES[0];
		
		String uid = FirebaseAuth.getInstance().getCurrentUser() != null
		? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
		if (uid.isEmpty()) {
			_showError("Not signed in.");
			return;
		}
		
		com.harvana.app.learn.LearnContentScreen.Result screen =
			com.harvana.app.learn.LearnContentScreen.check(title, description, imageUrl);
		if (screen.blocked()) {
			_showError(screen.message());
			return;
		}
		
		tv_learn_error.setVisibility(View.GONE);
		btn_learn_post.setEnabled(false);
		final ProgressDialog progress = new ProgressDialog(this);
		progress.setMessage("Publishing…");
		progress.setCancelable(false);
		progress.show();
		
		Map<String, Object> fields = new HashMap<>();
		fields.put("title", title);
		fields.put("description", description);
		fields.put("category", category);
		fields.put("imageUrl", imageUrl);
		fields.put("authorUid", uid);
		fields.put("createdAt", System.currentTimeMillis());
		fields.put("views", 0);
				
		com.harvana.app.firestore.GroupFirestoreManager.getInstance(getApplicationContext())
		.publishLearn(fields, new com.harvana.app.firestore.GroupFirestoreManager.IdCallback() {
			@Override
			public void onSuccess(String generatedId) {
				runOnUiThread(() -> {
					if (!isFinishing() && progress.isShowing()) progress.dismiss();
					SketchwareUtil.showMessage(getApplicationContext(), "Published. Your article is live in Learn Hub.");
					finish();
				});
			}
			
			@Override
			public void onError(String error) {
				runOnUiThread(() -> {
					if (!isFinishing() && progress.isShowing()) progress.dismiss();
					btn_learn_post.setEnabled(true);
					_showError("Failed to publish: " + error);
				});
			}
		});
	}
	
	private void _showError(String message) {
		tv_learn_error.setText(message);
		tv_learn_error.setVisibility(View.VISIBLE);
	}
	
	{
	}
	
}
