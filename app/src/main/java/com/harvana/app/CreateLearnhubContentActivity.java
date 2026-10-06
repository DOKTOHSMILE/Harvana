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
	private EditText et_learn_video_url;
	private TextView tv_learn_video_status;
	
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
		_setupFormatting();
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
		et_learn_video_url = findViewById(R.id.et_learn_video_url);
		tv_learn_video_status = findViewById(R.id.tv_learn_video_status);
		final String videoHint = tv_learn_video_status.getText().toString();
		et_learn_video_url.addTextChangedListener(new android.text.TextWatcher() {
			@Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
			@Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
			@Override public void afterTextChanged(android.text.Editable e) {
				String raw = e.toString().trim();
				com.harvana.app.learn.VideoLinks.Video v = com.harvana.app.learn.VideoLinks.parse(raw);
				if (raw.isEmpty()) { tv_learn_video_status.setText(videoHint); tv_learn_video_status.setTextColor(getResources().getColor(R.color.colorTextTertiary)); }
				else if (v != null) {
					tv_learn_video_status.setText("\u2713 " + v.platform.label + " video found. Readers can play it inside your article.");
					tv_learn_video_status.setTextColor(getResources().getColor(R.color.colorSuccess));
				} else {
					tv_learn_video_status.setText(com.harvana.app.learn.VideoLinks.problem(raw));
					tv_learn_video_status.setTextColor(getResources().getColor(R.color.colorError));
				}
			}
		});
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
	
	// ── formatting ───────────────────────────────────────────────

	private TextView formatPreview;
	
	/** A row of format buttons above the description (bold, italic, heading, list, quote) and a Preview that shows the post as readers will see it. */
	private void _setupFormatting() {
		final EditText field = et_learn_description;
		final View card = (View) field.getParent();
		if (!(card.getParent() instanceof LinearLayout)) return;
		final LinearLayout parent = (LinearLayout) card.getParent();
		float d = getResources().getDisplayMetrics().density;
		
		android.widget.HorizontalScrollView scroller = new android.widget.HorizontalScrollView(this);
		scroller.setHorizontalScrollBarEnabled(false);
		LinearLayout bar = new LinearLayout(this);
		bar.setOrientation(LinearLayout.HORIZONTAL);
		scroller.addView(bar);
		
		final String[] labels = {"B", "I", "H", "\u2022 List", "1. List", "\u201D Quote", "Link", "Preview"};
		for (final String label : labels) {
			TextView t = new TextView(this);
			t.setText(label);
			t.setTextSize(13);
			t.setGravity(android.view.Gravity.CENTER);
			t.setTextColor(getResources().getColor("Preview".equals(label) ? R.color.colorAccent : R.color.colorTextPrimary));
			if ("B".equals(label)) t.setTypeface(null, android.graphics.Typeface.BOLD);
			if ("I".equals(label)) t.setTypeface(null, android.graphics.Typeface.ITALIC);
			t.setPadding((int) (14 * d), 0, (int) (14 * d), 0);
			android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
			bg.setCornerRadius(20 * d);
			bg.setColor(getResources().getColor(R.color.colorSurfaceRaised));
			t.setBackground(bg);
			LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, (int) (36 * d));
			lp.setMarginEnd((int) (8 * d));
			bar.addView(t, lp);
			t.setOnClickListener(v -> {
				switch (label) {
					case "B": _wrap(field, "**", "**"); break;
					case "I": _wrap(field, "*", "*"); break;
					case "H": _linePrefix(field, "## "); break;
					case "\u2022 List": _linePrefix(field, "- "); break;
					case "1. List": _linePrefix(field, "1. "); break;
					case "\u201D Quote": _linePrefix(field, "> "); break;
					case "Link": _wrap(field, "[", "](https://)"); break;
					default: _togglePreview(field); break;
				}
			});
		}
		LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		slp.topMargin = (int) (6 * d);
		parent.addView(scroller, parent.indexOfChild(card), slp);
		
		formatPreview = new TextView(this);
		formatPreview.setTextSize(14.5f);
		formatPreview.setLineSpacing(0, 1.2f);
		formatPreview.setTextColor(getResources().getColor(R.color.colorTextSecondary));
		formatPreview.setPadding((int) (14 * d), (int) (12 * d), (int) (14 * d), (int) (12 * d));
		android.graphics.drawable.GradientDrawable pbg = new android.graphics.drawable.GradientDrawable();
		pbg.setCornerRadius(14 * d);
		pbg.setColor(getResources().getColor(R.color.colorSurfaceRaised));
		formatPreview.setBackground(pbg);
		formatPreview.setVisibility(View.GONE);
		LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		plp.topMargin = (int) (6 * d);
		plp.bottomMargin = (int) (16 * d);
		parent.addView(formatPreview, parent.indexOfChild(card) + 1, plp);
		
		// The limit is visible, never hidden: 8,000 characters, counted as you type.
		final TextView counter = new TextView(this);
		counter.setTextSize(11.5f);
		counter.setGravity(android.view.Gravity.END);
		LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		clp.topMargin = (int) (-10 * d);
		clp.bottomMargin = (int) (10 * d);
		parent.addView(counter, parent.indexOfChild(card) + 1, clp);
		final int max = DESCRIPTION_MAX;
		final Runnable paint = () -> {
			int n = field.getText().length();
			counter.setText(String.format(java.util.Locale.US, "%,d / %,d characters", n, max));
			counter.setTextColor(getResources().getColor(n >= max * 0.9 ? R.color.colorWarning : R.color.colorTextTertiary));
		};
		paint.run();
		field.addTextChangedListener(new android.text.TextWatcher() {
			@Override public void beforeTextChanged(CharSequence x, int a, int b, int c) { }
			@Override public void onTextChanged(CharSequence x, int a, int b, int c) { }
			@Override public void afterTextChanged(android.text.Editable e) { paint.run(); }
		});
	}
	
	/** The longest article, in characters. Matches android:maxLength on the field and the 8000 limit in the Firestore rules. */
	private static final int DESCRIPTION_MAX = 8000;
	
	/** Wraps the selected text (or puts the cursor between the two markers when nothing is selected). */
	private void _wrap(EditText f, String left, String right) {
		int a = Math.max(0, Math.min(f.getSelectionStart(), f.getSelectionEnd()));
		int b = Math.max(0, Math.max(f.getSelectionStart(), f.getSelectionEnd()));
		android.text.Editable e = f.getText();
		if (b > a) { e.insert(b, right); e.insert(a, left); f.setSelection(b + left.length() + right.length()); }
		else { e.insert(a, left + right); f.setSelection(a + left.length()); }
		f.requestFocus();
	}
	
	/** Puts a marker at the start of the current line, or takes it off if it is already there. */
	private void _linePrefix(EditText f, String prefix) {
		android.text.Editable e = f.getText();
		int at = Math.max(0, f.getSelectionStart());
		int start = e.toString().lastIndexOf('\n', at - 1) + 1;
		if (e.toString().startsWith(prefix, start)) e.delete(start, start + prefix.length());
		else e.insert(start, prefix);
		f.requestFocus();
	}
	
	private void _togglePreview(EditText f) {
		if (formatPreview.getVisibility() == View.VISIBLE) { formatPreview.setVisibility(View.GONE); return; }
		String text = f.getText().toString();
		if (text.trim().isEmpty()) { Toast.makeText(this, "Write something first.", Toast.LENGTH_SHORT).show(); return; }
		com.harvana.app.learn.LearnMarkdownView.bind(formatPreview, text, false);
		formatPreview.setVisibility(View.VISIBLE);
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
	private static final int RC_PICK_IMAGE = 502, RC_CROP_IMAGE = 503;
	private String uploadedImageUrl = null;
	private boolean uploadInProgress = false;
	
	// ── Image picking ────────────────────────────────────────────────────
	
	private void _checkPermissionAndPickImage() {
		// The system picture chooser (ACTION_GET_CONTENT) needs no storage permission: the person picks the file and the app is
		// handed just that one. Asking for photo-library access first only blocked people who said no.
		_launchImagePicker();
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
			// Crop first, with a free box: an article cover can be any shape (aspect 0 means flexible).
			startActivityForResult(com.harvana.app.utils.CropImageActivity.intent(this, data.getData(), 0f, 1280, "Crop cover", false), RC_CROP_IMAGE);
			return;
		}
		if (requestCode == RC_CROP_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {
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
		
		// Shrunk to 1280 px on the phone, then sent to the picture host set in res/values/image_host.xml (Firebase Storage if none).
		com.harvana.app.utils.ImageHost.upload(this, localUri, "learn_hub/" + uid, 1280, new com.harvana.app.utils.ImageHost.Callback() {
			@Override public void onSuccess(String url) {
				uploadedImageUrl = url;
				uploadInProgress = false;
				progress_image_upload.setVisibility(View.GONE);
				btn_learn_post.setEnabled(true);
			}
			@Override public void onError(String message) { _onUploadFailed(new Exception(message)); }
		});
	}
	
	private void _onUploadFailed(Exception e) {
		uploadInProgress = false;
		progress_image_upload.setVisibility(View.GONE);
		btn_learn_post.setEnabled(true);
		String m = e != null && e.getMessage() != null ? e.getMessage().toLowerCase(java.util.Locale.US) : "";
		boolean network = m.contains("connect") || m.contains("timeout") || m.contains("timed out") || m.contains("unable to resolve") || m.contains("network");
		_showError(network ? "Couldn't reach the picture server. Check your internet connection and try again, or post without a picture."
				: "Couldn't upload the picture" + (e != null && e.getMessage() != null ? ": " + e.getMessage() : "") + ". Try again, or post without a picture.");
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
		String imageUrl = uploadedImageUrl != null ? uploadedImageUrl : "";    // the picture is uploaded from the phone; there is no typed address
		String category = (String) spinner_learn_category.getSelectedItem();
		
		if (title.isEmpty()) {
			_showError("Please enter a title.");
			return;
		}
		if (description.isEmpty()) {
			_showError("Please enter a description.");
			return;
		}
		// A video link can stand in for the cover image. YouTube gives its own thumbnail, which becomes the cover.
		String videoRaw = et_learn_video_url.getText().toString().trim();
		com.harvana.app.learn.VideoLinks.Video video = com.harvana.app.learn.VideoLinks.parse(videoRaw);
		if (!videoRaw.isEmpty() && video == null) {
			_showError(com.harvana.app.learn.VideoLinks.problem(videoRaw));
			return;
		}
		if (video != null && imageUrl.isEmpty()) imageUrl = video.thumbnail;
		// A picture and a video are both optional: a post can be words only. A typed picture address must still be a web address.
		if (video == null && !imageUrl.isEmpty() && !imageUrl.startsWith("https://")) {
			_showError("A picture address must start with https://  Or leave the picture out.");
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
		if (video != null) {
			fields.put("videoUrl", video.link);
			fields.put("videoType", video.platform.label.toLowerCase());
		}
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
